package com.winkreyes.exceltogether;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import com.winkreyesvalley.excelmagiclevel.adapter.together.PartyLookup;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * ExcelTogether 0.3.0: the Jobs party bonus (+25% money and XP) plus the party itself: invite, accept, leave.
 * 0.3.0 adds the WRV-068 party range indicator (action bar line + transition cues), display only.
 * 0.4.0 adds party friendly-fire blocking and bonus mob drops (PartyCombatDrops); 0.4.1 shows the range
 * line only when it changes (BarPolicy); 0.4.2 escapes % in Bedrock form text; 0.4.3 routes mcMMO XP
 * share grants through ExcelRewardGuard's observed path when it is present.
 * 0.5.0 is the party mesh: /party, /excelparty and /em party are one party (EmSync + EmLink, with the
 * EliteMobs party as its face), and the PartyLookup service is published for party-share consumers.
 * The command is /excelparty (aliases /eparty, /together) because mcMMO owns /party on this server.
 */
public final class ExcelTogether extends JavaPlugin implements Listener, Env {
    private PartyManager mgr;
    private Controller ctl;
    private PartyMemberFinder partyFinder;
    private RangeIndicator indicator;
    private BarPolicy barPolicy;
    private BarPolicy.Gap gainGap;
    private boolean compassClick;
    private long idleMs; private double bonusMult = PartyBonus.DEFAULT_MULTIPLIER;
    private final Map<UUID, Long> lastActive = new HashMap<>();
    long rangeLines = 0L;
    long gainLines = 0L;
    /** Anti-leech: a player who has not moved a block, clicked or broken a block for share-idle-seconds gets no shared gains. 0 turns the check off. */
    private boolean isActive(UUID id) {
        if (idleMs <= 0) return true;
        Long t = lastActive.get(id);
        return t == null || System.currentTimeMillis() - t <= idleMs;
    }
    private void touch(UUID id) { lastActive.put(id, System.currentTimeMillis()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(org.bukkit.event.player.PlayerMoveEvent e) {
        if (e.getTo() == null || (e.getFrom().getBlockX() == e.getTo().getBlockX() && e.getFrom().getBlockY() == e.getTo().getBlockY() && e.getFrom().getBlockZ() == e.getTo().getBlockZ())) return;
        touch(e.getPlayer().getUniqueId());
    }
    @EventHandler(priority = EventPriority.MONITOR) public void onClick(org.bukkit.event.player.PlayerInteractEvent e) { touch(e.getPlayer().getUniqueId()); }
    @EventHandler(priority = EventPriority.MONITOR) public void onBreak(org.bukkit.event.block.BlockBreakEvent e) { touch(e.getPlayer().getUniqueId()); }

    @Override public void onEnable() {
        saveDefaultConfig();
        PartyBonus bonus = new PartyBonus(getConfig().getDouble("multiplier", PartyBonus.DEFAULT_MULTIPLIER),
                getConfig().getDouble("radius", PartyBonus.DEFAULT_RADIUS));
        bonusMult = bonus.multiplier();
        BonusMode mode = BonusMode.parse(getConfig().getString("bonus-mode", "party"));
        mgr = new PartyManager(getConfig().getInt("max-party-size", PartyManager.DEFAULT_SIZE),
                getConfig().getLong("invite-expiry-seconds", 60) * 1000L,
                getConfig().getLong("offline-grace-seconds", 300) * 1000L,
                id -> Bukkit.getPlayer(id) != null);
        // 0.5.0: publish the party roster to any plugin holding the PartyLookup contract
        // (ExcelMagicLevel's share is the first consumer). Read-only; unregistered in onDisable.
        publishPartyLookup();
        compassClick = getConfig().getBoolean("compass-click-invite", true);
        int pct = (int) Math.round((bonus.multiplier() - 1.0) * 100.0);
        double sharePct = "off".equalsIgnoreCase(getConfig().getString("share-mode", "full")) ? 0 : getConfig().getDouble("share-percent", 100);
        idleMs = getConfig().getLong("share-idle-seconds", 120) * 1000L;
        PartyMemberFinder finder = new PartyMemberFinder(bonus, mgr, mode, this::isActive);
        partyFinder = finder;
        indicator = new RangeIndicator();
        BarPolicy.Mode barMode = BarPolicy.Mode.parse(getConfig().getString("range-indicator.action-bar", "changes"));
        barPolicy = new BarPolicy(barMode, Math.round(getConfig().getDouble("range-indicator.change-hold-seconds", 2.0) * 1000.0),
                Math.round(getConfig().getDouble("range-indicator.repeat-seconds", 30.0) * 1000.0));
        gainGap = new BarPolicy.Gap(Math.round(getConfig().getDouble("range-indicator.gain-line-gap-seconds", 5.0) * 1000.0));
        GeyserForms.setEscapePercent(getConfig().getBoolean("bedrock-forms-escape-percent", true));
        Forms forms = new SyncForms(new GeyserForms(getLogger()), r -> Bukkit.getScheduler().runTask(this, r));
        String xpNote = setupMcmmoXpShare(finder, sharePct, pct);
        String note = PartyText.bonusNote(pct, (int) bonus.radius(), (int) sharePct, xpNote.startsWith("ON"));
        org.bukkit.plugin.Plugin emPlugin = Bukkit.getPluginManager().getPlugin("EliteMobs");
        if (getConfig().getBoolean("mesh.em.enabled", true) && emPlugin != null && emPlugin.isEnabled())
            note += " One party everywhere: /party, /excelparty and /em party are the same party.";
        ctl = new Controller(mgr, this, forms, note);
        ctl.setIndicator(indicator);
        ctl.setLogSink(getLogger()::info);
        String linkNote = setupMcmmoLink();
        String emNote = setupEmLink();
        ctl.setStatus(v -> { Player pl = Bukkit.getPlayer(v.id()); return pl == null ? List.of() : finder.status(pl); });
        ShareSink sink = sharePct > 0 ? new JobsShareSink() : null;
        getServer().getPluginManager().registerEvents(new PartyListener(bonus, finder, sink, sharePct,
                gainBar(),
                (op, t) -> { Player pl = op.getPlayer(); if (pl != null) new BukkitPlayer(pl).say(t); },
                System::currentTimeMillis), this);
        getServer().getPluginManager().registerEvents(this, this);
        PartyCombatDrops combatDrops = new PartyCombatDrops(this, mgr, bonus.radius());
        getServer().getPluginManager().registerEvents(combatDrops, this);
        for (Player pl : Bukkit.getOnlinePlayers()) combatDrops.refreshHp(pl.getUniqueId());
        Bukkit.getScheduler().runTaskTimer(this, () -> ctl.tick(), 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(this, this::tickIndicator, 60L, 50L);   // WRV-068: range line every 2.5s
        getLogger().info("ExcelTogether 0.5.0 enabled: x" + bonus.multiplier() + " money and XP, bonus-mode " + mode
                + ", radius " + bonus.radius() + " blocks, parties up to " + mgr.maxSize() + ", invites last "
                + (mgr.ttlMs() / 1000) + "s, compass click invite " + (compassClick ? "ON" : "off") + ". Bedrock forms: "
                + (Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null ? "Geyser found" : "no Geyser, chat menu only")
                + ". Jobs sharing: " + (sharePct > 0 ? (int) sharePct + "%" : "OFF") + ", idle limit " + (idleMs / 1000) + "s. mcMMO XP sharing: " + xpNote + ". mcMMO party link: " + linkNote
                + ". Bedrock form % escape " + (GeyserForms.escapePercent ? "ON" : "off")
                + ". Range indicator: action bar " + barPolicy.mode().name().toLowerCase()
                + (barPolicy.mode() == BarPolicy.Mode.CHANGES ? " (hold " + barPolicy.holdMs() + " ms, reminder " + (barPolicy.repeatMs() > 0 ? barPolicy.repeatMs() / 1000 + "s" : "off") + ")" : "")
                + ", gain lines at most every " + (gainGap.gapMs() / 1000) + "s, cue hysteresis 3s. " + combatDrops.summary()
                + ". EliteMobs mesh: " + emNote + ". PartyLookup service: published.");
    }

    /**
     * Publishes the PartyLookup service (TOGETHER-API-NOTE §3c). Two registrations, one provider:
     * Bukkit's ServicesManager keys registrations by Class OBJECT, and every plugin loads its own
     * copy of the interface - so a consumer that bundles the contract (ExcelMagicLevel does) can
     * only ever find a registration made under ITS classloader's Class. Alongside the plain
     * registration under our own copy, we therefore register a small reflective bridge under
     * ExcelMagicLevel's copy when that plugin is present. The bridge is a proxy: every call lands
     * on the same MagicLevelPartyLookup. Proven necessary on the 0.5.0 rig: without it,
     * ExcelMagicLevel 0.2.0 - enabling after us, softdepend satisfied - still reported no service.
     * Fail-soft: if the bridge cannot be built, the plain registration stands and a warning says
     * MagicLevel will keep its proximity fallback.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void publishPartyLookup() {
        PartyLookup provider = new MagicLevelPartyLookup(mgr);
        getServer().getServicesManager().register(PartyLookup.class, provider, this, ServicePriority.Normal);
        org.bukkit.plugin.Plugin eml = Bukkit.getPluginManager().getPlugin("ExcelMagicLevel");
        if (eml != null) {
            try {
                ClassLoader cl = eml.getClass().getClassLoader();
                Class<?> emlLookup = Class.forName("com.winkreyesvalley.excelmagiclevel.adapter.together.PartyLookup", false, cl);
                if (emlLookup != PartyLookup.class) {
                    Object bridge = java.lang.reflect.Proxy.newProxyInstance(cl, new Class<?>[]{emlLookup}, (proxy, method, a) -> {
                        if (method.getName().equals("partyMembers") && a != null && a.length == 1 && a[0] instanceof UUID u) return provider.partyMembers(u);
                        if (method.getName().equals("toString")) return "ExcelTogether PartyLookup (bridge to ExcelMagicLevel)";
                        if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                        if (method.getName().equals("equals")) return proxy == a[0];
                        throw new UnsupportedOperationException(method.getName());
                    });
                    getServer().getServicesManager().register((Class) emlLookup, bridge, this, ServicePriority.Normal);
                }
            } catch (Throwable t) {
                getLogger().warning("ExcelTogether: PartyLookup bridge to ExcelMagicLevel failed (" + t.getClass().getSimpleName()
                        + ") - ExcelMagicLevel will keep its proximity fallback.");
            }
        }
        getLogger().info("ExcelTogether: PartyLookup service published (party share consumers can connect).");
    }

    /** 0.5.0: the only job of onDisable is dropping our service registrations (0.4.x had no onDisable). */
    @Override public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
    }

    /** Gain lines (Jobs + mcMMO share) share one throttle: the BarPolicy gap, and nothing at all when the bar is off. */
    private BiConsumer<OfflinePlayer, String> gainBar() {
        return (op, t) -> {
            Player pl = op.getPlayer();
            if (pl == null || gainGap == null) return;
            if (barPolicy != null && barPolicy.mode() == BarPolicy.Mode.OFF) return;
            long now = System.currentTimeMillis();
            if (!gainGap.due(pl.getUniqueId(), now)) return;
            new BukkitPlayer(pl).actionBar(t);
            if (barPolicy != null) barPolicy.touch(pl.getUniqueId(), now);
            gainLines++;
        };
    }

    BarPolicy barPolicy() { return barPolicy; }

    /**
     * WRV-068 range indicator ticker (every 2.5s). Display only: nothing here touches payouts.
     * Partied players get the action bar line from the same range test that drives the bonus
     * (PartyMemberFinder.partyMembersInRange shares near() with hasPartner), plus the debounced
     * title/sound cues from RangeIndicator. Players not in a party see nothing.
     */
    private void tickIndicator() {
        if (mgr == null || partyFinder == null || indicator == null) return;
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            if (!mgr.inParty(id)) { indicator.sample(id, false, false, now); barPolicy.forget(id); continue; }
            BukkitPlayer view = new BukkitPlayer(p);
            int n = partyFinder.partyMembersInRange(p);
            if (barPolicy.show(id, n, now)) {
                if (n > 0) view.actionBar(RangeIndicator.activeLine(n, bonusMult));
                else view.actionBarDim(RangeIndicator.OUT_LINE);
                rangeLines++;
            }
            RangeIndicator.Cue cue = indicator.sample(id, true, n > 0, now);
            if (cue == RangeIndicator.Cue.NONE || !indicator.cuesEnabled(id)) continue;
            if (cue == RangeIndicator.Cue.TURNED_ON) { view.titleFlash("Party bonus ON", true); view.cueSound(); }
            else view.titleFlash("Party bonus OFF", false);
        }
    }

    /** mcMMO skill XP copy to nearby party members (reflection; off with a plain reason if anything is missing). */
    @SuppressWarnings("unchecked")
    private String setupMcmmoXpShare(PartyFinder finder, double sharePct, int pct) {
        if (sharePct <= 0) return "OFF (share-mode off)";
        if (!getConfig().getBoolean("share-mcmmo-xp", true)) return "OFF (config)";
        org.bukkit.plugin.Plugin mm = Bukkit.getPluginManager().getPlugin("mcMMO");
        if (mm == null || !mm.isEnabled()) return "OFF (mcMMO not installed)";
        McmmoXpShare x = new McmmoXpShare(mm.getClass().getClassLoader(), finder, sharePct, Bukkit::getPlayer,
                gainBar(), "+" + pct + "% party bonus: shared skill XP", bonusMult);
        if (!x.available()) { getLogger().warning("mcMMO XP sharing OFF, missing: " + x.missing); return "OFF (mcMMO API differs)"; }
        getServer().getPluginManager().registerEvent(x.eventClass, this, EventPriority.MONITOR, (l, e) -> x.onGain(e), this, true);
        return "ON (" + (int) sharePct + "%, skipped for parties where mcMMO XP share is EQUAL)";
    }

    /** Soft link to mcMMO parties. Any problem just leaves it off; ExcelTogether works alone. */
    private String setupMcmmoLink() {
        if (!getConfig().getBoolean("mcmmo-party-link", true)) return "OFF (config)";
        org.bukkit.plugin.Plugin mm = Bukkit.getPluginManager().getPlugin("mcMMO");
        if (mm == null || !mm.isEnabled()) return "OFF (mcMMO not installed)";
        try {
            ReflectMcmmoLink l = new ReflectMcmmoLink(mm.getClass().getClassLoader(), Bukkit::getPlayer, Bukkit::getOfflinePlayer, getLogger());
            if (!l.available()) { getLogger().warning("mcMMO found but these parts are missing, party link OFF: " + l.missing()); return "OFF (mcMMO API differs: " + l.missing().size() + " missing)"; }
            ctl.setLink(l);
            String ev = registerMcmmoEvents(mm);
            List<UUID> here = new ArrayList<>(); for (Player op : Bukkit.getOnlinePlayers()) here.add(op.getUniqueId());
            if (!here.isEmpty()) Bukkit.getScheduler().runTask(this, () -> ctl.mcmmoWatch(here));      // reload: adopt players already online
            return "ON (mcMMO " + mm.getDescription().getVersion() + ", hand joins mirrored by " + ev + ")";
        } catch (Throwable t) { getLogger().warning("mcMMO party link OFF: " + t); return "OFF (error)"; }
    }

    /** Soft mesh leg to EliteMobs parties (0.5.0). Any problem just leaves it off; ExcelTogether works as before. */
    private String setupEmLink() {
        if (!getConfig().getBoolean("mesh.em.enabled", true)) return "OFF (config)";
        org.bukkit.plugin.Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
        if (em == null || !em.isEnabled()) return "OFF (EliteMobs not installed)";
        try {
            ReflectEmLink l = new ReflectEmLink(em.getClass().getClassLoader(), Bukkit::getPlayer, getLogger());
            if (!l.available()) { getLogger().warning("EliteMobs found but these parts are missing, mesh OFF: " + l.missing()); return "OFF (EliteMobs API differs: " + l.missing().size() + " missing)"; }
            // The parties-enabled probe is NOT trusted at enable time: EliteMobs 10.9.12 loads its
            // configs seconds after its onEnable returns (proven on the 0.5.0 rig - the probe read
            // the static default false while Party.yml said enabled: true). A false here therefore
            // never refuses the mesh; a recheck 30 s later, when EM has settled, delivers the
            // verdict and stands the link down if parties really are disabled.
            int pollEvery = Math.max(1, getConfig().getInt("mesh.em.poll-every-ticks", 2));
            boolean loginMat = getConfig().getBoolean("mesh.em.login-materialize", true);
            boolean mirrorInvites = getConfig().getBoolean("mesh.em.mirror-invites", true);
            boolean reform = getConfig().getBoolean("mesh.em.leader-reform", false);
            ctl.setEmLink(l);
            ctl.setEmPollEvery(pollEvery);
            ctl.setEmMirrorInvites(mirrorInvites);
            ctl.emSync().setLoginMaterialize(loginMat);
            ctl.emSync().setLeaderReform(reform);
            ctl.emSync().setQuietSeconds(getConfig().getLong("mesh.em.ready-check-quiet-seconds", 45));
            ctl.emSync().setDeferMaxSeconds(getConfig().getLong("mesh.em.defer-max-seconds", 300));
            ctl.emSync().setDebug(getConfig().getBoolean("mesh.debug", false));
            registerEmDungeonEvents(em, l);
            if (Boolean.FALSE.equals(l.partiesEnabled())) {
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (Boolean.FALSE.equals(l.partiesEnabled())) l.disableSession("EliteMobs parties are disabled in Party.yml");
                }, 600L);
            }
            List<UUID> here = new ArrayList<>(); for (Player op : Bukkit.getOnlinePlayers()) here.add(op.getUniqueId());
            if (!here.isEmpty()) Bukkit.getScheduler().runTask(this, () -> ctl.emWatch(here));      // reload: reconcile players already online
            return "ON (poll ~" + pollEvery + "s, login materialize " + (loginMat ? "ON" : "off") + ", invites " + (mirrorInvites ? "mirrored" : "not mirrored")
                    + (reform ? ", leader reform ON" : "") + ")"
                    + (mgr.maxSize() > 5 ? " (party size capped at 5 by EliteMobs)" : "");
        } catch (Throwable t) { getLogger().warning("EliteMobs mesh OFF: " + t); return "OFF (error)"; }
    }

    /** Freeze F1: track who is inside an EliteMobs dungeon/arena, by reflection through EM's classloader. */
    @SuppressWarnings("unchecked")
    private void registerEmDungeonEvents(org.bukkit.plugin.Plugin em, ReflectEmLink l) {
        Map<Class<?>, Boolean> evs = l.dungeonEvents();
        if (evs.isEmpty()) {
            getLogger().warning("ExcelTogether mesh: EliteMobs dungeon events not available; the dungeon freeze relies on refused-write deferral only.");
            return;
        }
        for (Map.Entry<Class<?>, Boolean> e : evs.entrySet()) {
            Class<? extends org.bukkit.event.Event> ec = (Class<? extends org.bukkit.event.Event>) e.getKey();
            boolean joined = e.getValue();
            getServer().getPluginManager().registerEvent(ec, this, EventPriority.MONITOR, (li, ev) -> l.trackInstanceEvent(ev, joined), this, true);
        }
    }

    /** Listens for mcMMO party changes by reflection (no hard dependency). The 2-second poll covers anything the event misses. */
    @SuppressWarnings("unchecked")
    private String registerMcmmoEvents(org.bukkit.plugin.Plugin mm) {
        try {
            Class<? extends org.bukkit.event.Event> ev = (Class<? extends org.bukkit.event.Event>) Class.forName("com.gmail.nossr50.events.party.McMMOPartyChangeEvent", false, mm.getClass().getClassLoader());
            java.lang.reflect.Method gOld = ev.getMethod("getOldParty"), gNew = ev.getMethod("getNewParty"), gPlayer = ev.getMethod("getPlayer");
            getServer().getPluginManager().registerEvent(ev, this, EventPriority.MONITOR, (l, e) -> {
                try {
                    if (!ev.isInstance(e)) return;
                    Player p = (Player) gPlayer.invoke(e);
                    String oldP = (String) gOld.invoke(e), newP = (String) gNew.invoke(e);
                    UUID id = p == null ? null : p.getUniqueId();
                    List<UUID> ids = ctl.mcmmoEventIds(id, oldP, newP);
                    List<UUID> joined = (newP != null && id != null) ? List.of(id) : List.of();
                    Bukkit.getScheduler().runTask(this, () -> ctl.mcmmoChanged(ids, joined));
                } catch (Throwable t) { getLogger().warning("mcMMO party event ignored: " + t); }
            }, this, true);
            return "event + poll";
        } catch (Throwable t) {
            getLogger().warning("mcMMO party event not available, using poll only: " + t);
            return "poll only";
        }
    }

    // ---- Env ----
    @Override public PlayerView online(UUID id) { Player p = Bukkit.getPlayer(id); return p == null ? null : new BukkitPlayer(p); }
    @Override public List<PlayerView> visibleOthers(PlayerView viewer) {
        List<PlayerView> out = new ArrayList<>();
        Player v = Bukkit.getPlayer(viewer.id());
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (v != null && (o.getUniqueId().equals(v.getUniqueId()) || !v.canSee(o))) continue;
            out.add(new BukkitPlayer(o));
        }
        return out;
    }
    @Override public long now() { return System.currentTimeMillis(); }

    // ---- events ----
    @EventHandler public void onQuit(PlayerQuitEvent e) {
        lastActive.remove(e.getPlayer().getUniqueId());
        if (indicator != null) indicator.forget(e.getPlayer().getUniqueId());
        if (barPolicy != null) barPolicy.forget(e.getPlayer().getUniqueId());
        if (gainGap != null) gainGap.forget(e.getPlayer().getUniqueId());
        ctl.playerQuit(e.getPlayer().getUniqueId());
    }
    @EventHandler public void onJoin(PlayerJoinEvent e) { touch(e.getPlayer().getUniqueId()); ctl.playerJoin(new BukkitPlayer(e.getPlayer())); }

    /** Right-click another player while holding a compass: ask to invite them (Yes/Cancel first). They must still accept. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCompassClick(PlayerInteractEntityEvent e) {
        if (!compassClick || e.getHand() != EquipmentSlot.HAND) return;
        if (!(e.getRightClicked() instanceof Player target)) return;
        Player me = e.getPlayer();
        if (me.getInventory().getItemInMainHand().getType() != Material.COMPASS) return;
        if (!me.hasPermission("excelparty.use") || target.hasMetadata("NPC")) return;
        ctl.compassClick(new BukkitPlayer(me), new BukkitPlayer(target));
    }

    // ---- command ----
    @Override public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (!(s instanceof Player pl)) { s.sendMessage("Players only."); return true; }
        if (!pl.hasPermission("excelparty.use")) { pl.sendMessage("You can't use parties."); return true; }
        ctl.command(new BukkitPlayer(pl), a);
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 1) {
            List<String> o = new ArrayList<>(List.of("invite", "accept", "decline", "confirm", "cancel", "leave", "list", "status", "kick", "invites", "cue", "help"));
            o.removeIf(x -> !x.startsWith(a[0].toLowerCase(Locale.ROOT)));
            return o;
        }
        if (a.length == 2 && a[0].equalsIgnoreCase("cue")) {
            List<String> o = new ArrayList<>(List.of("on", "off"));
            o.removeIf(x -> !x.startsWith(a[1].toLowerCase(Locale.ROOT)));
            return o;
        }
        if (a.length == 2 && a[0].equalsIgnoreCase("invite")) {
            List<String> o = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase(Locale.ROOT).startsWith(a[1].toLowerCase(Locale.ROOT))) o.add(p.getName());
            return o;
        }
        return List.of();
    }
}
