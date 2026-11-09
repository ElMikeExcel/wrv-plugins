package com.winkreyes.exceldowned;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ExcelDowned — lethal damage becomes a DOWNED state instead of death,
 * and the Resurrection spell (Magic) brings a downed party member back
 * at 50% health. See DESIGN.md (~/workspace/downed-revive/).
 *
 * The registry is keyed by UUID and persisted on every transition, so
 * logout and server restart neither extend nor shorten a bleed-out —
 * the deadline is a wall-clock timestamp.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class ExcelDowned extends JavaPlugin {

    // settings (config.yml)
    long bleedOutMs;
    double downedDamageFactor;
    double damageSecondsPerPoint;
    double downedHealth;
    double reviveHealthFraction;
    double gateRange;
    double reviveRange;
    long giveUpWindowMs;
    double scrubRadius;
    int slownessAmplifier;

    private final Map<UUID, DownedSession> sessions = new ConcurrentHashMap<>();
    private final Set<UUID> bypassDeath = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String[]> savedNames = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastReviveByCaster = new ConcurrentHashMap<>();
    private TogetherPartyProvider party;
    private GiveUpUi giveUpUi;
    private File stateFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        party = new TogetherPartyProvider(getLogger(),
                getConfig().getBoolean("party.fallback-all-online", false));
        stateFile = new File(getDataFolder(), "downed.yml");
        loadState();
        getServer().getPluginManager().registerEvents(new DownedListener(this), this);
        giveUpUi = new GiveUpUi(this);
        getServer().getPluginManager().registerEvents(giveUpUi, this);
        MagicBridge bridge = new MagicBridge(this);
        getServer().getPluginManager().registerEvents(bridge, this);
        bridge.tryRegisterAction();
        DownedCommands cmds = new DownedCommands(this);
        if (getCommand("giveup") != null) getCommand("giveup").setExecutor(cmds);
        if (getCommand("downed") != null) getCommand("downed").setExecutor(cmds);
        // state task: slowness refresh, action bar, bleed-out clock (1 s)
        Bukkit.getScheduler().runTaskTimer(this, this::stateTick, 20L, 20L);
        // deaggro scrub (2 s): clear any mob found targeting a downed player
        Bukkit.getScheduler().runTaskTimer(this, this::scrubTick, 40L, 40L);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isDowned(p.getUniqueId())) applyDownedState(p);
        }
        getLogger().info("ExcelDowned " + getDescription().getVersion() + " ready — build "
                + BuildInfo.BUILD_ID + "; sessions restored: " + sessions.size()
                + "; bleed-out " + (bleedOutMs / 1000) + "s; downed factor " + downedDamageFactor);
    }

    @Override
    public void onDisable() {
        if (giveUpUi != null) giveUpUi.shutdown();
        saveState();
    }

    void loadSettings() {
        bleedOutMs = getConfig().getLong("bleed-out-seconds", 300) * 1000L;
        downedDamageFactor = getConfig().getDouble("downed-damage-factor", 0.25);
        damageSecondsPerPoint = getConfig().getDouble("damage-seconds-per-point", 1.0);
        downedHealth = getConfig().getDouble("downed-health", 1.0);
        reviveHealthFraction = getConfig().getDouble("revive-health-fraction", 0.5);
        gateRange = getConfig().getDouble("gate-range", 24.0);
        reviveRange = getConfig().getDouble("revive-range", 20.0);
        giveUpWindowMs = getConfig().getLong("giveup-window-seconds", 10) * 1000L;
        scrubRadius = getConfig().getDouble("scrub-radius", 32.0);
        slownessAmplifier = getConfig().getInt("slowness-amplifier", 4);
    }

    // ---------- registry ----------

    public boolean isDowned(UUID id) { return id != null && sessions.containsKey(id); }
    public DownedSession sessionOf(UUID id) { return id == null ? null : sessions.get(id); }
    public PartyProvider party() { return party; }
    public GiveUpUi giveUpUi() { return giveUpUi; }
    Set<UUID> bypassDeath() { return bypassDeath; }

    // ---------- state transitions ----------

    /** Enter DOWNED. Returns false if already downed (exactly-once entry). */
    public boolean enterDowned(Player player, String cause) {
        UUID id = player.getUniqueId();
        if (sessions.containsKey(id)) return false;
        DownedSession s = new DownedSession(id, player.getName(), System.currentTimeMillis(),
                bleedOutMs, cause,
                player.getWorld().getName(),
                player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
        sessions.put(id, s);
        saveState();
        applyDownedState(player);
        clearAggroOn(player);
        record("downed_enter", baseFields(player)
                + ",\"cause\":\"" + esc(cause) + "\""
                + ",\"world\":\"" + esc(player.getWorld().getName()) + "\"");
        Bukkit.getPluginManager().callEvent(new DownedEvents.DownedEnterEvent(player, cause));
        alertParty(player, ChatColor.RED + "✝ " + player.getName() + " is DOWN — "
                + "Resurrection can bring them back (" + formatClock(s.remainingMs(now())) + ")");
        player.sendMessage(ChatColor.RED + "✝ You are DOWN. You will bleed out in "
                + formatClock(bleedOutMs) + " unless a party member revives you. /giveup to let go.");
        return true;
    }

    /** Apply the visible/practical downed state to an online player. */
    public void applyDownedState(Player player) {
        if (!isDowned(player.getUniqueId())) return;
        try { player.setHealth(downedHealth); } catch (Throwable ignored) {}
        player.setSprinting(false);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60,
                slownessAmplifier, false, false, true));
        savedNames.putIfAbsent(player.getUniqueId(),
                new String[]{player.getDisplayName(), player.getPlayerListName()});
        String marked = ChatColor.RED + "✝ DOWNED " + ChatColor.RESET + player.getName();
        try { player.setDisplayName(marked); player.setPlayerListName(marked); } catch (Throwable ignored) {}
    }

    private void clearDownedPresentation(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        String[] orig = savedNames.remove(player.getUniqueId());
        if (orig != null) {
            try { player.setDisplayName(orig[0]); player.setPlayerListName(orig[1]); }
            catch (Throwable ignored) {}
        }
    }

    /** Revive a downed player. Returns true only if this call did it. */
    public boolean revive(Player healer, Player target) {
        if (target == null) return false;
        DownedSession s = sessions.remove(target.getUniqueId());
        if (s == null) return false;
        if (giveUpUi != null) giveUpUi.invalidate(target.getUniqueId()); // an open confirm dies with the down
        saveState();
        double max = target.getAttribute(Attribute.MAX_HEALTH) != null
                ? target.getAttribute(Attribute.MAX_HEALTH).getValue() : 20.0;
        double after = max * reviveHealthFraction;
        double given = after - downedHealth;
        clearDownedPresentation(target);
        try { target.setHealth(after); } catch (Throwable ignored) {}
        if (healer != null) lastReviveByCaster.put(healer.getUniqueId(), System.currentTimeMillis());
        record("revive", "\"spell\":\"resurrection\""
                + ",\"healer\":\"" + esc(healer == null ? "" : healer.getName()) + "\""
                + ",\"healer_uuid\":\"" + (healer == null ? "" : healer.getUniqueId()) + "\""
                + ",\"target\":\"" + esc(target.getName()) + "\""
                + ",\"target_uuid\":\"" + target.getUniqueId() + "\""
                + ",\"amount_restored\":" + round2(given)
                + ",\"effective\":" + round2(given)
                + ",\"health_after\":" + round2(after));
        Bukkit.getPluginManager().callEvent(new DownedEvents.DownedReviveEvent(healer, target, given));
        alertParty(target, ChatColor.GREEN + "✝ " + target.getName() + " was revived by "
                + (healer == null ? "Resurrection" : healer.getName()) + "!");
        target.sendMessage(ChatColor.GREEN + "✝ You were revived at 50% health.");
        return true;
    }

    /**
     * Run the one real death. Consumes the session + the death latch
     * first, so no path can ever produce a second death for one down.
     */
    public boolean finalizeDeath(Player player, DownedSession.Exit exit) {
        UUID id = player.getUniqueId();
        DownedSession s = sessions.get(id);
        if (s == null || !s.consumeDeath()) return false;
        sessions.remove(id);
        if (giveUpUi != null) giveUpUi.invalidate(id); // an open confirm dies with the down
        saveState();
        bypassDeath.add(id);
        clearDownedPresentation(player);
        String type = exit == DownedSession.Exit.GIVE_UP ? "give_up"
                : exit == DownedSession.Exit.BLEED_OUT ? "bleed_out" : "downed_death_external";
        record(type, baseFields(player));
        Bukkit.getPluginManager().callEvent(new DownedEvents.DownedDeathEvent(player, exit));
        if (player.isOnline()) {
            try { player.setHealth(0.0); } catch (Throwable ignored) {}
        }
        return true;
    }

    /** External death (e.g. /kill) while downed: clear state, no second path. */
    public void noteExternalDeath(Player player) {
        if (sessions.remove(player.getUniqueId()) != null) {
            if (giveUpUi != null) giveUpUi.invalidate(player.getUniqueId());
            saveState();
            clearDownedPresentation(player);
            record("downed_death_external", baseFields(player));
        }
    }

    public boolean wasRevivedByRecently(UUID casterId, long withinMs) {
        Long t = lastReviveByCaster.get(casterId);
        return t != null && System.currentTimeMillis() - t <= withinMs;
    }

    public void consumeReviveMark(UUID casterId) { lastReviveByCaster.remove(casterId); }

    // ---------- tasks ----------

    private void stateTick() {
        long now = System.currentTimeMillis();
        for (DownedSession s : sessions.values()) {
            Player p = Bukkit.getPlayer(s.playerId());
            if (p == null) {
                // offline: wall clock rules; an expired offline session is
                // finalized at login (PlayerJoin handler), not here.
                continue;
            }
            if (s.isExpired(now)) {
                p.sendMessage(ChatColor.DARK_RED + "✝ You bled out.");
                finalizeDeath(p, DownedSession.Exit.BLEED_OUT);
                continue;
            }
            // keep the freeze exact + the slowness on (Cure strips slow)
            if (Math.abs(p.getHealth() - downedHealth) > 0.01) {
                try { p.setHealth(downedHealth); } catch (Throwable ignored) {}
            }
            if (!p.hasPotionEffect(PotionEffectType.SLOWNESS)) applyDownedState(p);
            p.sendActionBar(ChatColor.RED + "✝ DOWNED — bleed out in "
                    + formatClock(s.remainingMs(now)) + " · /giveup to let go");
        }
        if (giveUpUi != null) giveUpUi.tickExpire(); // a lapsed arm closes its menu
    }

    private void scrubTick() {
        for (DownedSession s : sessions.values()) {
            Player p = Bukkit.getPlayer(s.playerId());
            if (p != null) clearAggroOn(p);
        }
    }

    /** Clear current mob targets on a downed player (entry + scrub). */
    public void clearAggroOn(Player player) {
        for (org.bukkit.entity.Entity e : player.getNearbyEntities(scrubRadius, scrubRadius, scrubRadius)) {
            if (e instanceof org.bukkit.entity.Mob) {
                org.bukkit.entity.Mob mob = (org.bukkit.entity.Mob) e;
                if (mob.getTarget() == player) mob.setTarget(null);
            }
        }
    }

    void alertParty(Player subject, String message) {
        Set<UUID> members = party.partyMembersOf(subject.getUniqueId());
        for (UUID id : members) {
            Player m = Bukkit.getPlayer(id);
            if (m != null) m.sendMessage(message);
        }
    }

    // ---------- persistence ----------

    private synchronized void saveState() {
        try {
            YamlConfiguration y = new YamlConfiguration();
            for (DownedSession s : sessions.values()) {
                y.set("sessions." + s.playerId(), s.toMap());
            }
            y.save(stateFile);
        } catch (IOException e) {
            getLogger().warning("ExcelDowned: state save failed: " + e.getMessage());
        }
    }

    private void loadState() {
        if (!stateFile.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(stateFile);
        if (y.getConfigurationSection("sessions") == null) return;
        for (String key : y.getConfigurationSection("sessions").getKeys(false)) {
            Map<String, Object> m = new HashMap<>();
            for (String k : y.getConfigurationSection("sessions." + key).getKeys(false)) {
                m.put(k, y.get("sessions." + key + "." + k));
            }
            try { sessions.put(UUID.fromString(key), DownedSession.fromMap(m)); }
            catch (Throwable t) { getLogger().warning("ExcelDowned: skipped bad session " + key); }
        }
    }

    // ---------- records (Protect-shaped JSONL, DESIGN §7) ----------

    private String baseFields(Player p) {
        return "\"player\":\"" + esc(p.getName()) + "\",\"uuid\":\"" + p.getUniqueId() + "\"";
    }

    public void record(String type, String fields) {
        String day = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        File dir = new File(getDataFolder(), "records");
        dir.mkdirs();
        File f = new File(dir, "records-" + day + ".jsonl");
        String line = "{\"ts\":" + System.currentTimeMillis() + ",\"type\":\"" + type + "\","
                + fields + "}";
        try (PrintWriter w = new PrintWriter(new FileWriter(f, true))) {
            w.println(line);
        } catch (IOException e) {
            getLogger().warning("ExcelDowned: record write failed: " + e.getMessage());
        }
    }

    static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    static long now() { return System.currentTimeMillis(); }

    static String formatClock(long ms) {
        long sec = Math.max(0, (ms + 999) / 1000);
        return (sec / 60) + ":" + String.format("%02d", sec % 60);
    }
}
