package com.winkreyes.excelwandbars;

import com.elmakers.mine.bukkit.api.event.CastEvent;
import com.elmakers.mine.bukkit.api.event.PreCastEvent;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.elmakers.mine.bukkit.api.spell.Spell;
import com.elmakers.mine.bukkit.api.wand.Wand;
import com.elmakers.mine.bukkit.api.wand.WandTemplate;
import com.winkreyes.excelwandbars.core.EarnMath;
import wrv.magic.evidence.EffectObserver;
import wrv.sp.route.NativeCastRouteAdapter;
import wrv.sp.route.SingleSpRoute;
import com.winkreyes.excelwandbars.core.EarnedBarGate;
import com.winkreyes.excelwandbars.core.RankLadder;
import com.winkreyes.excelwandbars.core.RankText;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * Rank-up by casting (2026-10-26). Successful casts with a rank wand (beginner, student,
 * apprentice, master) earn Magic SP, using each spell's own "earns" value from spells.yml,
 * Magic's own earn-cooldown throttle, the server's existing XP bonuses (mcMMO /xprate =
 * ExcelWeekendXP weekend 1.2x; ExcelTogether party 1.25x), capped. When the player has
 * enough SP for the next rank, their held, owned wand is upgraded IN PLACE (same item,
 * no new wand, spells only ever added). The rank wand templates stay pathless with
 * earn_multiplier 0, so Magic never pays SP for the same cast a second time.
 *
 * Off unless config "rank-up-casting.enabled: true" (reloadable).
 */
final class RankUpHook implements Listener {
    private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");
    private final ExcelWandBars plugin;
    private final MagicAPI api;

    // settings
    boolean enabled = false;
    private RankLadder ladder = RankLadder.wrvDefault();
    private long earnCooldownMs = 60_000;
    private final Map<String, Long> earnCooldownBySpell = new HashMap<>();
    private final Set<String> healSpells = new HashSet<>();
    private double dailyFull = 800, dailyMax = 1600, dailySlow = 0.25;
    private double cap = 1.5;
    private boolean weekendOn = true, groupOn = true;
    private double weekendFallback = 1.2;
    private boolean announceTitle = true, announceChat = true, announceSound = true, broadcast = true;
    private boolean actionBar = true;
    private long actionBarDelayTicks = 12;
    private String titleText = "{rank} rank!";
    private String subtitleText = "Your wand learned {spells} new spells";
    private String chatText = "Your wand is now {rank} rank. It learned {spells} new spells. Open /spellbook to read them.";
    private String broadcastText = "{player} reached {rank} rank!";
    private String howText = "Cast spells to earn SP.";

    // state
    private final Map<UUID, Long> preCast = new HashMap<>();
    private final Map<UUID, Long> regainAt = new HashMap<>();
    private final Map<String, Long> lastEarn = new HashMap<>();     // uuid|spell -> ms
    private final Map<UUID, Double> carry = new HashMap<>();
    private final Map<UUID, Double> today = new HashMap<>();
    private LocalDate todayDate = LocalDate.now(CHICAGO);
    private final Map<UUID, Long> upgradeFailedAt = new HashMap<>();
    private final File dailyFile;
    long paid = 0, vetoedHeal = 0, throttledZero = 0, rankUps = 0, failedUps = 0;
    String lastLog = "";
    String lastBar = "";

    RankUpHook(ExcelWandBars plugin, MagicAPI api) {
        this.plugin = plugin;
        this.api = api;
        this.dailyFile = new File(plugin.getDataFolder(), "rankup-daily.yml");
    }

    void loadSettings(ConfigurationSection c) {
        // Parse before changing settings; malformed explicit fence prevents startup registration.
        Set<String> configuredFence = parseStartupFence(c);
        previouslyManagedFence.addAll(configuredFence);
        startupConfiguredFence = configuredFence;
        settingsGeneration++;
        detachManagedEvidence();
        earnedBarGate.clear();
        enabled = c != null && c.getBoolean("enabled", false);
        if (c == null) return;
        RankLadder l = new RankLadder();
        ConfigurationSection sp = c.getConfigurationSection("sp-to-reach");
        l.add("beginner", 0);
        l.add("student", sp == null ? 800 : sp.getLong("student", 800));
        l.add("apprentice", sp == null ? 1600 : sp.getLong("apprentice", 1600));
        l.add("master", sp == null ? 1800 : sp.getLong("master", 1800));
        ConfigurationSection ms = c.getConfigurationSection("max-rank-by-stage");
        if (ms != null) for (String k : ms.getKeys(false)) l.minStage(k, ms.getInt(k, 0));
        ladder = l;
        earnCooldownMs = Math.max(0, c.getLong("earn-cooldown-seconds", 60)) * 1000L;
        earnCooldownBySpell.clear();
        ConfigurationSection ec = c.getConfigurationSection("earn-cooldown-by-spell");
        if (ec != null) for (String k : ec.getKeys(false)) earnCooldownBySpell.put(k.toLowerCase(Locale.ROOT), Math.max(0, ec.getLong(k)) * 1000L);
        healSpells.clear();
        for (String s : c.getStringList("heal-only-when-it-heals")) healSpells.add(s.toLowerCase(Locale.ROOT));
        if (!c.isList("heal-only-when-it-heals")) healSpells.addAll(List.of("heal", "mend", "flashheal", "greaterheal"));
        dailyFull = c.getDouble("daily.full-rate-sp", 800);
        dailyMax = c.getDouble("daily.max-sp", 1600);
        dailySlow = c.getDouble("daily.slow-rate", 0.25);
        cap = c.getDouble("bonus.cap", 1.5);
        weekendOn = c.getBoolean("bonus.weekend", true);
        groupOn = c.getBoolean("bonus.group", true);
        weekendFallback = c.getDouble("bonus.weekend-if-no-mcmmo", 1.2);
        announceTitle = c.getBoolean("announce.title", true);
        announceChat = c.getBoolean("announce.chat", true);
        announceSound = c.getBoolean("announce.sound", true);
        broadcast = c.getBoolean("announce.broadcast", true);
        actionBar = c.getBoolean("action-bar", true);
        partyBarHoldMs = Math.max(0, c.getLong("party-bar-wait-ms", 2500));
        titleText = c.getString("text.title", titleText);
        subtitleText = c.getString("text.subtitle", subtitleText);
        chatText = c.getString("text.chat", chatText);
        broadcastText = c.getString("text.broadcast", broadcastText);
        howText = c.getString("text.how", howText);
    }


    // OFFLINE OPT-IN ONLY: never configured or registered by normal plugin startup.
    private NativeCastRouteAdapter managedRoute;
    /** Explicit persisted configuration, loaded before this listener is registered. No default spell families. */
    static Set<String> parseStartupFence(ConfigurationSection settings) {
        String key = "managed-evidence-fenced-spells";
        if (settings == null || !settings.contains(key)) return Set.of();
        Object raw = settings.get(key);
        if (!(raw instanceof List<?> values)) throw new IllegalArgumentException("explicit managed fence must be a list");
        Set<String> result = new LinkedHashSet<>();
        for (Object value : values) {
            if (!(value instanceof String spell) || !spell.matches("[a-z0-9_.-]{1,200}") || !result.add(spell))
                throw new IllegalArgumentException("managed fence requires unique canonical base spell keys");
        }
        return Set.copyOf(result);
    }
    private Set<String> startupConfiguredFence = Set.of();
    private Set<String> managedKeys = Set.of();
    private final Set<String> previouslyManagedFence = new HashSet<>(); // survives settings reload within this hook instance
    private final Map<UUID, Long> finishRevision = new HashMap<>();
    private final Map<UUID, ManagedAward> managedSnapshots = new HashMap<>();
    private long settingsGeneration;
    private int managedCapacity;
    private record SourceFollowup(Wand original, String id, String template, Mage mage, UUID owner, UUID capturedOwner, boolean capturedBound) {
        boolean matches(Player player, Mage currentMage, Wand current) {
            try { return current == original && currentMage == mage && owner.equals(player.getUniqueId())
                    && id.equals(current.getId()) && template.equals(current.getTemplateKey())
                    && current.canUse(player) && strictOwned(current, player) && sameOwnership(current, capturedOwner, capturedBound); }
            catch (Throwable unavailable) { return false; }
        }
    }
    private record ManagedAward(UUID cast, Player player, Mage mage, String spell, int earns, long at,
            LocalDate day, long cooldown, EarnMath.Bonus bonus, long revision, long generation,
            SourceFollowup followup, SingleSpRoute.Event event) {}
    private static boolean strictOwned(Wand wand, Player p) {
        try { Object id = wand.getClass().getMethod("getOwnerId").invoke(wand);
            if (id == null || id.toString().isBlank()) return !wand.isBound();
            return id.toString().equalsIgnoreCase(p.getUniqueId().toString());
        } catch (Throwable unknown) { return false; }
    }
    private static boolean sameOwnership(Wand wand, UUID expectedOwner, boolean expectedBound) {
        try { Object raw = wand.getClass().getMethod("getOwnerId").invoke(wand);
            UUID owner = raw == null || raw.toString().isBlank() ? null : UUID.fromString(raw.toString());
            return java.util.Objects.equals(owner, expectedOwner) && wand.isBound() == expectedBound;
        } catch (Throwable unknown) { return false; }
    }
    private void markFinish(CastEvent event) {
        Mage mage = event.getMage(); Player player = mage == null || !mage.isPlayer() ? null : mage.getPlayer();
        if (player != null) finishRevision.merge(player.getUniqueId(), 1L, Math::addExact);
    }
    /** Must be explicitly called by a separately reviewed integration; no config default or startup call. */
    EffectObserver.Sink configureManagedEvidence(Set<String> keys, SingleSpRoute.LateProof late, int pendingCapacity, java.util.function.Predicate<EffectObserver.Receipt> reviewedEligibility) {
        if (!Bukkit.isPrimaryThread() || managedRoute != null || pendingCapacity < 1) throw new IllegalStateException("owner/registration/capacity");
        if (!startupConfiguredFence.containsAll(keys)) throw new IllegalStateException("managed route keys must be explicitly fenced in startup configuration");
        java.util.Objects.requireNonNull(reviewedEligibility, "explicit reviewed usefulness eligibility");
        NativeCastRouteAdapter next = new NativeCastRouteAdapter(keys, late, pendingCapacity, this::captureManagedFinish,
                reviewedEligibility, this::acceptManagedDecision, t -> plugin.getLogger().warning("managed SP route: " + t));
        managedKeys = Set.copyOf(keys); managedCapacity = pendingCapacity; previouslyManagedFence.addAll(keys); managedRoute = next;
        return new EffectObserver.Sink() {
            final NativeCastRouteAdapter generation = managedRoute;
            public void castStarted(EffectObserver.RootEvidence r) { if (generation == managedRoute) generation.castStarted(r); }
            public void observe(EffectObserver.Receipt r) { if (generation == managedRoute) generation.observe(r); }
            public void rootFinished(EffectObserver.RootEvidence r) {
                if (generation != managedRoute) return;
                try { generation.rootFinished(r); } finally { managedSnapshots.remove(r.cast()); }
            }
        };
    }
    record ManagedRegistration(EffectObserver.Sink sink, AutoCloseable handle) {}
    ManagedRegistration configureManagedEvidenceOwned(Set<String> keys, SingleSpRoute.LateProof late,
            int capacity, java.util.function.Predicate<EffectObserver.Receipt> eligibility) {
        EffectObserver.Sink sink = configureManagedEvidence(keys, late, capacity, eligibility);
        NativeCastRouteAdapter mine = managedRoute;
        return new ManagedRegistration(sink, () -> {
            if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("owner thread required");
            if (managedRoute == mine) detachManagedEvidence();
        });
    }
    void detachManagedEvidence() {
        if (managedRoute != null) managedRoute.detach();
        managedRoute = null; managedKeys = Set.of(); managedSnapshots.clear(); finishRevision.clear();
    }
    private SingleSpRoute.Event captureManagedFinish(CastEvent event) {
        EffectObserver.RootEvidence root = EffectObserver.originalRootEvidence(event.getContext()).orElseThrow(() -> new IllegalStateException("original root unavailable"));
        if (managedRoute == null || !managedRoute.hasOriginalRoot(root)) throw new IllegalStateException("root not admitted in current route generation");
        Mage mage = event.getMage(); Player p = mage == null || !mage.isPlayer() ? null : mage.getPlayer();
        Wand original = event.getContext().getBaseContext().getWand();
        EffectObserver.SourceSnapshot source = root.source();
        if (p == null || !p.getUniqueId().equals(root.caster()) || plugin.skipWorld(p.getWorld()) || !isLadderWand(original)
                || source == null || source.wandId() == null || source.templateKey() == null || !source.ownerKnown()
                || source.bound() == null || source.earns() == null || !Double.isFinite(source.earns())
                || !source.wandId().equals(original.getId()) || !source.templateKey().equals(original.getTemplateKey())
                || !original.canUse(p) || !strictOwned(original, p) || !sameOwnership(original, source.ownerId(), source.bound())
                || (source.ownerId() == null ? source.bound() : !source.ownerId().equals(p.getUniqueId())))
            throw new IllegalStateException("unverified original owned rank wand");
        int earns = (int)Math.round(source.earns()); if (earns <= 0) throw new IllegalStateException("no original earns");
        long at = System.currentTimeMillis(); LocalDate day = java.time.Instant.ofEpochMilli(at).atZone(CHICAGO).toLocalDate();
        EarnMath.Bonus b = bonus(p); long cd = earnCooldownBySpell.getOrDefault(root.spell(), earnCooldownMs);
        SingleSpRoute.Event snapshot = new SingleSpRoute.Event(p.getUniqueId(), root.spell(), event.getSpellResult().isSuccess(), at, day.toString(), earns, cd, b.multiplier, "rankup-managed-generation-" + settingsGeneration);
        if (!managedSnapshots.containsKey(root.cast()) && managedSnapshots.size() >= managedCapacity) throw new IllegalStateException("managed snapshot capacity");
        ManagedAward award = new ManagedAward(root.cast(), p, mage, root.spell(), earns, at, day, cd, b,
                finishRevision.getOrDefault(p.getUniqueId(), 0L), settingsGeneration,
                new SourceFollowup(original, source.wandId(), source.templateKey(), mage, p.getUniqueId(), source.ownerId(), source.bound()), snapshot);
        if (managedSnapshots.putIfAbsent(root.cast(), award) != null) throw new IllegalStateException("snapshot already captured");
        return snapshot;
    }
    private void acceptManagedDecision(SingleSpRoute.Decision decision) {
        if (decision.route() != SingleSpRoute.Route.VERIFIED || decision.root() == null) return;
        ManagedAward saved = managedSnapshots.remove(decision.root().cast());
        if (saved == null || saved.event != decision.event()) throw new IllegalStateException("missing exact finish snapshot");
        UUID id = saved.player.getUniqueId();
        if (!enabled || saved.generation != settingsGeneration || finishRevision.getOrDefault(id, 0L) != saved.revision
                || System.currentTimeMillis() < saved.at
                || !saved.day.equals(LocalDate.now(CHICAGO)) || todayDate.isAfter(saved.day)
                || lastEarn.getOrDefault(id + "|" + saved.spell, Long.MIN_VALUE) > saved.at
                || !saved.player.isOnline() || api.getController().getRegisteredMage(saved.player) != saved.mage) {
            log(id + " managed SP: obsolete finish/account snapshot skipped; no backpay"); return;
        }
        awardExisting(saved.player, saved.mage, saved.spell, saved.earns, saved.at, saved.cooldown, saved.bonus, saved.followup);
    }

    // ------------------------------------------------------------------ helpers

    static String rankOf(Wand w) {
        if (w == null) return null;
        String k = w.getTemplateKey();
        return k == null ? null : k.toLowerCase(Locale.ROOT);
    }

    boolean isLadderWand(Wand w) { return w != null && ladder.has(rankOf(w)); }
    /** 0.3.0: rank keys in ladder order (beginner, student, apprentice, master). */
    List<String> ladderOrder() { return new java.util.ArrayList<>(ladder.order()); }

    /** The rank's player-facing name: config rank-names first (0.2.8: master shows as "Master"), else Magic's path name. */
    String rankName(String key) {
        String magic = null;
        try {
            com.elmakers.mine.bukkit.api.wand.WandUpgradePath path = key == null ? null : api.getController().getPath(key);
            if (path != null && path.getName() != null && !path.getName().isBlank()) magic = com.winkreyes.excelwandbars.core.SpellInfoText.strip(path.getName()).trim();
        } catch (Throwable ignored) { }
        if (magic == null) magic = key == null || key.isEmpty() ? "" : Character.toUpperCase(key.charAt(0)) + key.substring(1);
        return plugin.rankDisplay(key, magic);
    }

    private int stageCache = -1;
    private long stageReadAt = 0;
    /** ExcelMagicGate appliedStage, -1 when unknown. Read from its state file at most once a minute. */
    int stage() {
        long now = System.currentTimeMillis();
        if (now - stageReadAt < 60_000) return stageCache;
        stageReadAt = now;
        try {
            File f = new File(plugin.getDataFolder().getParentFile(), "ExcelMagicGate/state.yml");
            stageCache = f.isFile() ? YamlConfiguration.loadConfiguration(f).getInt("appliedStage", -1) : -1;
        } catch (Throwable t) { stageCache = -1; }
        return stageCache;
    }

    // ------------------------------------------------------------------ bonuses (one source of truth: the plugins that run them)

    String weekendSource = "?";
    /** The weekend / event XP rate the server runs right now (mcMMO /xprate, set by ExcelWeekendXP). */
    double weekend() {
        if (!weekendOn) return 1.0;
        try {
            Plugin m = Bukkit.getPluginManager().getPlugin("mcMMO");
            if (m != null && m.isEnabled()) {
                Class<?> main = m.getClass();
                Object p = main.getField("p").get(null);
                boolean on = (Boolean) main.getMethod("isXPEventEnabled").invoke(p);
                if (!on) { weekendSource = "mcMMO off"; return 1.0; }
                Class<?> ec = Class.forName("com.gmail.nossr50.config.experience.ExperienceConfig", true, main.getClassLoader());
                Object cfg = ec.getMethod("getInstance").invoke(null);
                double r = ((Number) ec.getMethod("getExperienceGainsGlobalMultiplier").invoke(cfg)).doubleValue();
                weekendSource = "mcMMO xprate " + r;
                return Math.max(1.0, r);
            }
        } catch (Throwable t) { weekendSource = "mcMMO read failed " + t.getClass().getSimpleName(); }
        // No mcMMO: the same Fri-Sun window ExcelWeekendXP uses.
        DayOfWeek d = ZonedDateTime.now(CHICAGO).getDayOfWeek();
        boolean wk = d == DayOfWeek.FRIDAY || d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
        weekendSource = "calendar " + (wk ? "weekend" : "weekday");
        return wk ? weekendFallback : 1.0;
    }

    private Plugin togetherPlugin;
    private Object togetherMgr;
    private Method sameParty;
    String groupSource = "?";
    /** ExcelTogether's party bonus when a party member is near (its own multiplier and radius). */
    double group(Player p) {
        if (!groupOn) return 1.0;
        try {
            Plugin t = Bukkit.getPluginManager().getPlugin("ExcelTogether");
            if (t == null || !t.isEnabled()) { groupSource = "ExcelTogether absent"; return 1.0; }
            double mult = t.getConfig().getDouble("multiplier", 1.25);
            double radius = t.getConfig().getDouble("radius", 50);
            String mode = t.getConfig().getString("bonus-mode", "party");
            if (t != togetherPlugin) {
                togetherPlugin = t; togetherMgr = null; sameParty = null;
                for (Field f : t.getClass().getDeclaredFields()) {
                    if (f.getType().getSimpleName().equals("PartyManager")) {
                        f.setAccessible(true);
                        togetherMgr = f.get(t);
                        sameParty = f.getType().getMethod("sameParty", UUID.class, UUID.class);
                        break;
                    }
                }
            }
            double r2 = radius * radius;
            for (Player o : p.getWorld().getPlayers()) {
                if (o == p || !o.isOnline() || o.isDead()) continue;
                if (o.getLocation().distanceSquared(p.getLocation()) > r2) continue;
                boolean together;
                if (!"party".equalsIgnoreCase(mode)) together = true;
                else if (togetherMgr != null && sameParty != null) together = (Boolean) sameParty.invoke(togetherMgr, p.getUniqueId(), o.getUniqueId());
                else together = mcmmoSameParty(p, o);
                if (together) { groupSource = "ExcelTogether " + mode + " x" + mult; return Math.max(1.0, mult); }
            }
            groupSource = "ExcelTogether nobody near";
            return 1.0;
        } catch (Throwable e) {
            groupSource = "ExcelTogether read failed " + e.getClass().getSimpleName();
            return 1.0;
        }
    }

    private static boolean mcmmoSameParty(Player a, Player b) {
        try {
            Plugin m = Bukkit.getPluginManager().getPlugin("mcMMO");
            if (m == null) return false;
            Class<?> pa = Class.forName("com.gmail.nossr50.api.PartyAPI", true, m.getClass().getClassLoader());
            return (Boolean) pa.getMethod("inSameParty", Player.class, Player.class).invoke(null, a, b);
        } catch (Throwable t) { return false; }
    }

    EarnMath.Bonus bonus(Player p) { return EarnMath.bonus(weekend(), group(p), cap); }

    // ------------------------------------------------------------------ events

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPreCast(PreCastEvent e) {
        if (!enabled) return;
        Mage m = e.getMage();
        if (m == null || !m.isPlayer()) return;
        preCast.put(m.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    /** Magic heals fire RegainReason.CUSTOM just before the health changes (HealAction), never at full health. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent e) {
        if (!enabled || e.getRegainReason() != EntityRegainHealthEvent.RegainReason.CUSTOM || e.getAmount() <= 0) return;
        regainAt.put(e.getEntity().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCast(CastEvent e) {
        if (!enabled) return;
        try {
            if (managedRoute != null || !previouslyManagedFence.isEmpty()) {
                Spell routedSpell = e.getSpell();
                String routedKey = routedSpell == null || routedSpell.getSpellKey() == null ? null : routedSpell.getSpellKey().getBaseKey();
                if (routedKey != null && previouslyManagedFence.contains(routedKey)) {
                    if (managedRoute == null || !managedKeys.contains(routedKey)) return; // Reload/detach never silently restores legacy for managed spells.
                    if (e.getContext() == null || e.getContext() != e.getContext().getBaseContext()) return;
                    markFinish(e);
                    managedRoute.onCast(e);
                    return; // Managed failure never falls through into legacy qualification.
                }
                if (managedRoute != null) markFinish(e); // A newer unmanaged finish also invalidates stale managed snapshots.
            }
            if (e.getSpellResult() == null || !e.getSpellResult().isSuccess()) return;
            Mage mage = e.getMage();
            Player p = mage == null || !mage.isPlayer() ? null : mage.getPlayer();
            Spell spell = e.getSpell();
            if (p == null || spell == null || plugin.skipWorld(p.getWorld())) return;
            Wand wand = mage.getActiveWand();
            if (!isLadderWand(wand)) return;
            String key = spell.getSpellKey() == null ? spell.getKey() : spell.getSpellKey().getBaseKey();
            key = key.toLowerCase(Locale.ROOT);
            int earns = (int) Math.round(spell.getEarns());
            if (earns <= 0) return;
            long now = System.currentTimeMillis();
            if (healSpells.contains(key) && !healedSomeone(p, e, now)) {
                vetoedHeal++;
                log(p.getName() + " " + key + ": no one was hurt, no SP");
                return;
            }
            awardExisting(p, mage, key, earns, now, earnCooldownBySpell.getOrDefault(key, earnCooldownMs), null, null);

        } catch (Throwable t) {
            if (plugin.debug()) plugin.getLogger().info("rank-up earn: " + t);
        }
    }

    /** Single existing accounting body: same Mage SP, throttle/day/carry maps, caps and best-effort persistence. */
    private void awardExisting(Player p, Mage mage, String key, int earns, long now, long cd, EarnMath.Bonus b, SourceFollowup expectedSource) {
            String lk = p.getUniqueId() + "|" + key;
            Long last = lastEarn.get(lk);
            int base = EarnMath.throttled(earns, last == null ? Long.MAX_VALUE : now - last, cd);
            if (base <= 0) { throttledZero++; log(p.getName() + " " + key + ": too soon, 0 SP"); return; }
            lastEarn.put(lk, now);
            final EarnMath.Bonus appliedBonus = b == null ? bonus(p) : b;
            rollDay();
            com.winkreyes.excelwandbars.core.SharedSpAward.Result award =
                    com.winkreyes.excelwandbars.core.SharedSpAward.calculate(base, appliedBonus.multiplier,
                            today.getOrDefault(p.getUniqueId(), 0.0), carry.getOrDefault(p.getUniqueId(), 0.0),
                            dailyFull, dailyMax, dailySlow);
            double got = award.qualifiedSp();
            today.merge(p.getUniqueId(), got, Double::sum);
            int whole = award.currencyWhole();
            carry.put(p.getUniqueId(), award.carryAfter());
            if (whole > 0) { mage.addCurrency("sp", whole, true); paid += whole; }
            log(p.getName() + " " + key + ": earns " + earns + " -> " + base + " x" + appliedBonus.multiplier + " (" + weekendSource + "; " + groupSource + ") daily " + String.format(Locale.ROOT, "%.2f", got) + " -> +" + whole + " SP");
            final int gained = whole;
            Bukkit.getScheduler().runTask(plugin, () -> afterEarn(p, gained, appliedBonus, expectedSource));
    }

    /** A heal spell counts only if Magic really healed someone (the target or the caster) during this cast. */
    private boolean healedSomeone(Player p, CastEvent e, long now) {
        long since = preCast.getOrDefault(p.getUniqueId(), now - 2000) - 50;
        Long me = regainAt.get(p.getUniqueId());
        if (me != null && me >= since) return true;
        try {
            Entity t = e.getContext() == null ? null : e.getContext().getTargetEntity();
            if (t != null) { Long at = regainAt.get(t.getUniqueId()); if (at != null && at >= since) return true; }
        } catch (Throwable ignored) { }
        return false;
    }

    private void rollDay() {
        LocalDate d = LocalDate.now(CHICAGO);
        if (!d.equals(todayDate)) { todayDate = d; today.clear(); }
    }

    private void afterEarn(Player p, int gained, EarnMath.Bonus b, SourceFollowup expectedSource) {
        if (!p.isOnline()) return;
        Mage mage = api.getController().getRegisteredMage(p);
        if (mage == null) { log(p.getName() + " after earn: no mage"); return; }
        Wand wand = mage.getActiveWand();
        if (expectedSource != null && !expectedSource.matches(p, mage, wand)) { log(p.getName() + " after earn: original source no longer active/owned; rank followup skipped"); return; }
        if (!isLadderWand(wand)) { log(p.getName() + " after earn: not holding a rank wand"); return; }
        String rank = rankOf(wand);
        long sp = (long) Math.floor(mage.getCurrency("sp"));
        RankLadder.Step st = ladder.step(rank, sp, stage());
        if (st == RankLadder.Step.READY && ownedBy(wand, p)) {
            Long failed = upgradeFailedAt.get(p.getUniqueId());
            if (failed == null || System.currentTimeMillis() - failed > 600_000) {
                String result = rankUp(p, mage, wand);
                log(p.getName() + " rank up: " + result);
                if (result.startsWith("OK")) { earnedBarGate.forget(p.getUniqueId()); return; }
            }
        }
        if (actionBar && gained > 0) {
            String next = ladder.next(rank);
            String line = RankText.earned(gained, b.label, b.multiplier, rankName(rank), sp, next == null ? 0 : ladder.cost(next), next == null);
            lastBar = line;
            sendBar(p, line, rank, actionBarDelayTicks, earnedBarGate.offer(p.getUniqueId(), System.currentTimeMillis()));
        }
    }

    /** Only the player's own wand ranks up (a borrowed or someone else's bound wand never does). */
    static boolean ownedBy(Wand wand, Player p) {
        try {
            Method m = wand.getClass().getMethod("getOwnerId");
            Object id = m.invoke(wand);
            if (id == null || id.toString().isEmpty()) return !wand.isBound();
            return id.toString().equalsIgnoreCase(p.getUniqueId().toString());
        } catch (Throwable t) {
            return true;
        }
    }

    /** Every spell a rank template (and the templates it inherits) lists. */
    /** Newly unlocked managed spells begin at native base; learned practice controls higher effective levels. */
    static String newSpellProvisionKey(String key) {
        return wrv.magic.level.LearnedHooks.managesKey(key)
                ? new com.elmakers.mine.bukkit.api.spell.SpellKey(key).getBaseKey() : key;
    }

    Set<String> templateSpells(String key) {
        Set<String> out = new LinkedHashSet<>();
        WandTemplate t = api.getController().getWandTemplate(key);
        for (int i = 0; t != null && i < 8; i++, t = t.getParent()) {
            ConfigurationSection c = t.getConfiguration();
            if (c == null) continue;
            for (String s : c.getStringList("spells")) out.add(s);
            if (c.isConfigurationSection("spells")) out.addAll(c.getConfigurationSection("spells").getKeys(false));
        }
        return out;
    }

    /**
     * Upgrade the held wand in place to the next rank. SP is only taken when the wand really
     * changed and kept every spell it had. Returns "OK ..." or the reason it did not happen.
     */
    String rankUp(Player p, Mage mage, Wand wand) {
        String from = rankOf(wand);
        String to = ladder.next(from);
        if (to == null) return "top";
        if (api.getController().getWandTemplate(to) == null) return "no template " + to;
        long need = ladder.cost(to);
        if (mage.getCurrency("sp") < need) return "not enough SP";
        if (!MigrationTouchGuard.allow(mage, wand)) return "WAITING saved wand progression reconciliation";
        Set<String> before = new HashSet<>(wand.getSpells());
        Set<String> want = templateSpells(to);
        try {
            boolean replaced = false;
            try {
                Method rt = findMethod(wand.getClass(), "replaceTemplate", String.class);
                if (rt != null) { rt.setAccessible(true); rt.invoke(wand, to); replaced = true; }
            } catch (Throwable ignored) { }
            if (!replaced) {
                Method st = wand.getClass().getMethod("setTemplate", String.class);
                st.invoke(wand, to);
            }
            // Numbers only go up (Magic's own upgrade rule); the template's are the new rank's.
            WandTemplate t = api.getController().getWandTemplate(to);
            ConfigurationSection tc = t == null ? null : t.getConfiguration();
            if (tc != null) {
                for (String k : new String[] {"mana_max", "mana_regeneration", "hotbar_count"}) {
                    if (tc.contains(k)) wand.upgrade(k, tc.get(k));
                }
                if (tc.contains("level_spells_to_path")) wand.configure("level_spells_to_path", tc.getString("level_spells_to_path"));
            }
            int added = 0;
            for (String s : want) {
                String base = s.contains("|") ? s.substring(0, s.indexOf('|')) : s;
                if (!hasSpell(wand, base)) { if (wand.addSpell(newSpellProvisionKey(s))) added++; }
            }
            wand.saveState();
            boolean changed = to.equalsIgnoreCase(rankOf(wand));
            if (!changed) {
                failedUps++;
                upgradeFailedAt.put(p.getUniqueId(), System.currentTimeMillis());
                plugin.getLogger().warning("Rank up " + p.getName() + " " + from + " -> " + to + " did not stick (template still " + rankOf(wand) + "). No SP taken.");
                return "FAILED template=" + rankOf(wand);
            }
            // Spells are kept by base key (a rank step may raise "mend|2" to "mend|3", never lower it).
            Map<String, Integer> beforeLv = levels(before);
            Map<String, Integer> afterLv = levels(wand.getSpells());
            List<String> lost = new ArrayList<>();
            for (Map.Entry<String, Integer> en : beforeLv.entrySet()) {
                Integer now2 = afterLv.get(en.getKey());
                if (now2 == null || now2 < en.getValue()) lost.add(en.getKey() + (en.getValue() > 1 ? "|" + en.getValue() : ""));
            }
            if (!lost.isEmpty()) {
                // Put back anything that went missing, then check again. The wand already changed rank, so SP is spent either way.
                for (String s : lost) wand.addSpell(s);
                wand.saveState();
                afterLv = levels(wand.getSpells());
                List<String> still = new ArrayList<>();
                for (Map.Entry<String, Integer> en : beforeLv.entrySet()) {
                    Integer now2 = afterLv.get(en.getKey());
                    if (now2 == null || now2 < en.getValue()) still.add(en.getKey());
                }
                plugin.getLogger().warning("Rank up " + p.getName() + " " + from + " -> " + to + ": re-added " + lost + (still.isEmpty() ? "" : ", STILL MISSING " + still));
            }
            mage.removeCurrency("sp", need, true);
            rankUps++;
            int newSpells = 0;
            for (String k : afterLv.keySet()) if (!beforeLv.containsKey(k)) newSpells++;
            announce(p, to, newSpells);
            // 0.2.9: put the new spells into the player's own bar (empty slots first, else the Spellbook), after the rank lines.
            StyleHook sh = plugin.styleHook();
            if (sh != null) sh.applyLater(p, "rank up", 20L);
            return "OK " + from + " -> " + to + " (-" + need + " SP, +" + newSpells + " spells, added " + added + ", replaceTemplate " + replaced + ", bound " + wand.isBound() + (lost.isEmpty() ? "" : ", re-added " + lost) + ")";
        } catch (Throwable t) {
            failedUps++;
            upgradeFailedAt.put(p.getUniqueId(), System.currentTimeMillis());
            plugin.getLogger().warning("Rank up " + p.getName() + " " + from + " -> " + to + " failed: " + t + ". No SP taken.");
            return "FAILED " + t;
        }
    }

    /** "mend|3" -> mend=3, "feed" -> feed=1. */
    static Map<String, Integer> levels(java.util.Collection<String> spells) {
        Map<String, Integer> out = new HashMap<>();
        for (String s : spells) {
            int i = s.indexOf('|');
            String b = (i < 0 ? s : s.substring(0, i)).toLowerCase(Locale.ROOT);
            int lv = 1;
            if (i >= 0) { try { lv = Integer.parseInt(s.substring(i + 1)); } catch (NumberFormatException ignored) { } }
            out.merge(b, lv, Math::max);
        }
        return out;
    }

    private static boolean hasSpell(Wand w, String base) {
        for (String s : w.getSpells()) {
            String b = s.contains("|") ? s.substring(0, s.indexOf('|')) : s;
            if (b.equalsIgnoreCase(base)) return true;
        }
        return false;
    }

    private static Method findMethod(Class<?> c, String name, Class<?>... args) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try { return k.getDeclaredMethod(name, args); } catch (NoSuchMethodException ignored) { }
        }
        return null;
    }

    private void announce(Player p, String to, int spells) {
        String rn = rankName(to);
        if (announceTitle) {
            p.showTitle(Title.title(Component.text(RankText.fill(titleText, rn, spells), NamedTextColor.GOLD),
                    Component.text(RankText.fill(subtitleText, rn, spells), NamedTextColor.AQUA),
                    Title.Times.times(java.time.Duration.ofMillis(500), java.time.Duration.ofMillis(3500), java.time.Duration.ofMillis(1000))));
        }
        if (announceChat) p.sendMessage(ChatColor.GOLD + RankText.fill(chatText, rn, spells));
        if (announceSound) {
            try { p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f); } catch (Throwable ignored) { }
        }
        if (broadcast) {
            String msg = RankText.fill(broadcastText, rn, spells).replace("{player}", displayName(p));
            for (Player o : Bukkit.getOnlinePlayers()) if (o != p) o.sendMessage(ChatColor.AQUA + msg);
        }
    }

    /** Nickname if the server set one (Essentials etc.), colours and formatting removed; else the account name. */
    static String displayName(Player p) {
        String d = null;
        try { d = ChatColor.stripColor(p.getDisplayName()); } catch (Throwable ignored) { }
        if (d != null) d = d.replaceAll("[\\u00a7&][0-9a-fk-orA-FK-OR]", "").trim();
        if (d != null && d.startsWith("~")) d = d.substring(1).trim();   // Essentials nickname-prefix
        return d == null || d.isEmpty() ? p.getName() : d;
    }

    // 0.2.8: ExcelTogether shares the action bar ("Party: nobody in range", "Party bonus ACTIVE", gain lines).
    // If its line went up less than partyBarHoldMs ago, wait until it has been readable, then show ours.
    // If ExcelTogether shows another line meanwhile, wait again (at most 3 tries, then skip ours).
    // Offline candidate: Attunement stances and their clear message take priority as well.
    // The queued line is a latest-paid-cast snapshot, not a continuous SP balance ticker.
    // Durable current SP remains at the top of the Spell Book.
    long partyBarHoldMs = 2500;
    long partyBarWaits = 0, partyBarSkips = 0;
    private final EarnedBarGate earnedBarGate = new EarnedBarGate();
    private void sendBar(Player p, String line, String expectedRank, long delayTicks, long token) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            UUID id = p.getUniqueId();
            if (!earnedBarGate.isCurrent(id, token)) return;
            if (!p.isOnline() || !enabled || !actionBar) { earnedBarGate.forget(id); return; }
            Mage mage = api.getController().getRegisteredMage(p);
            if (mage == null || !isLadderWand(mage.getActiveWand()) || !expectedRank.equals(rankOf(mage.getActiveWand())) || plugin.skipWorld(p.getWorld())) {
                earnedBarGate.forget(id); return;
            }
            long now = System.currentTimeMillis();
            long at = partyBarAt(p);
            long wait = at <= 0 ? 0 : at + partyBarHoldMs - now;
            EarnedBarGate.Result r = earnedBarGate.decide(id, token, now, stanceBusyOrUnknown(p), wait > 0);
            switch (r) {
                case WAIT_STANCE, WAIT_CLEAR -> sendBar(p, line, expectedRank, 10, token);
                case WAIT_PARTY -> {
                    partyBarWaits++;
                    sendBar(p, line, expectedRank, Math.max(1, (wait + 49) / 50), token);
                }
                case PARTY_SKIP -> partyBarSkips++;
                case SEND -> p.sendActionBar(Component.text(line, NamedTextColor.LIGHT_PURPLE));
                default -> { } // stale, expired or invalidated: Spell Book remains authoritative
            }
        }, delayTicks);
    }

    /** Fail closed for an installed but incompatible stance API: never cover its warnings. */
    private boolean stanceBusyOrUnknown(Player p) {
        Plugin a = Bukkit.getPluginManager().getPlugin("ExcelAttunement");
        if (a == null || !a.isEnabled()) return false;
        try {
            Object stance = a.getClass().getMethod("activeStance", Player.class).invoke(a, p);
            return !(stance instanceof Enum<?> e) || !e.name().equals("NONE");
        } catch (ReflectiveOperationException | RuntimeException ex) { return true; }
    }

    private Object barPolicy; private Plugin barPolicyOf; private Field barSt, barLastAt; String partyBarSource = "?";
    /** When ExcelTogether last put a party line on this player's action bar (ms), 0 if never/unknown. Read only. */
    long partyBarAt(Player p) {
        try {
            Plugin t = Bukkit.getPluginManager().getPlugin("ExcelTogether");
            if (t == null || !t.isEnabled()) { partyBarSource = "ExcelTogether absent"; return 0; }
            if (t != barPolicyOf) {
                barPolicyOf = t; barPolicy = null; barSt = null; barLastAt = null;
                for (Field f : t.getClass().getDeclaredFields()) {
                    if (!f.getType().getSimpleName().equals("BarPolicy")) continue;
                    f.setAccessible(true);
                    barPolicy = f.get(t);
                    barSt = f.getType().getDeclaredField("st"); barSt.setAccessible(true);
                    break;
                }
            }
            if (barPolicy == null || barSt == null) { partyBarSource = "ExcelTogether bar unknown"; return 0; }
            Object s;
            synchronized (barPolicy) { s = ((Map<?, ?>) barSt.get(barPolicy)).get(p.getUniqueId()); }
            if (s == null) { partyBarSource = "ExcelTogether no party line"; return 0; }
            if (barLastAt == null) { barLastAt = s.getClass().getDeclaredField("lastAt"); barLastAt.setAccessible(true); }
            partyBarSource = "ExcelTogether bar ok";
            return barLastAt.getLong(s);
        } catch (Throwable e) {
            partyBarSource = "ExcelTogether bar read failed " + e.getClass().getSimpleName();
            return 0;
        }
    }

    // ------------------------------------------------------------------ spell book lines

    /** Header lines for the Spell Book (both editions). Empty when off or not a rank wand. */
    List<String> header(Player p, Wand wand) {
        List<String> out = new ArrayList<>();
        if (!enabled || p == null || !isLadderWand(wand)) return out;
        try {
            Mage mage = api.getController().getRegisteredMage(p);
            long sp = mage == null ? 0 : (long) Math.floor(mage.getCurrency("sp"));
            String rank = rankOf(wand);
            String next = ladder.next(rank);
            RankLadder.Step st = ladder.step(rank, sp, stage());
            return RankText.header(rankName(rank), next == null ? null : rankName(next), sp, next == null ? 0 : ladder.cost(next), st, bonus(p).tag(), howText);
        } catch (Throwable t) {
            return out;
        }
    }

    // ------------------------------------------------------------------ admin + persistence

    List<String> status(Player p) {
        List<String> out = new ArrayList<>();
        out.add("rank-up-casting enabled=" + enabled + " stage=" + stage() + " paid=" + paid + " vetoedHeal=" + vetoedHeal + " throttled0=" + throttledZero + " rankUps=" + rankUps + " failed=" + failedUps);
        if (p != null) {
            Mage mage = api.getController().getRegisteredMage(p);
            Wand w = mage == null ? null : mage.getActiveWand();
            EarnMath.Bonus b = bonus(p);
            out.add(p.getName() + ": wand=" + rankOf(w) + " bound=" + (w != null && w.isBound()) + " owned=" + (w != null && ownedBy(w, p)) + " sp=" + (mage == null ? "?" : mage.getCurrency("sp"))
                    + " today=" + String.format(Locale.ROOT, "%.1f", today.getOrDefault(p.getUniqueId(), 0.0)) + " bonus=x" + b.multiplier + " [" + weekendSource + "; " + groupSource + "]");
            if (w != null) out.add("spells: " + new java.util.TreeSet<>(w.getSpells()));
            for (String h : header(p, w)) out.add("header: " + h);
        }
        out.add("last: " + lastLog);
        out.add("last bar: " + lastBar);
        out.add("party bar: " + partyBarSource + " (wait " + partyBarHoldMs + " ms; waited " + partyBarWaits + ", skipped " + partyBarSkips + ")");
        return out;
    }

    String forceCheck(Player p) {
        Mage mage = api.getController().getRegisteredMage(p);
        Wand w = mage == null ? null : mage.getActiveWand();
        if (!isLadderWand(w)) return "not holding a rank wand";
        RankLadder.Step st = ladder.step(rankOf(w), (long) mage.getCurrency("sp"), stage());
        if (st != RankLadder.Step.READY) return "step " + st;
        if (!ownedBy(w, p)) return "not their wand";
        return rankUp(p, mage, w);
    }

    void load() {
        try {
            if (!dailyFile.isFile()) return;
            YamlConfiguration y = YamlConfiguration.loadConfiguration(dailyFile);
            if (!LocalDate.now(CHICAGO).toString().equals(y.getString("date"))) return;
            ConfigurationSection s = y.getConfigurationSection("earned");
            if (s != null) for (String k : s.getKeys(false)) today.put(UUID.fromString(k), s.getDouble(k));
        } catch (Throwable t) { plugin.getLogger().warning("rankup-daily.yml: " + t); }
    }

    void save() {
        try {
            rollDay();
            YamlConfiguration y = new YamlConfiguration();
            y.set("date", todayDate.toString());
            for (Map.Entry<UUID, Double> e : today.entrySet()) y.set("earned." + e.getKey(), Math.round(e.getValue() * 100) / 100.0);
            y.save(dailyFile);
        } catch (Throwable t) { plugin.getLogger().warning("rankup-daily.yml: " + t); }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        earnedBarGate.forget(id);
        managedSnapshots.values().removeIf(saved -> saved.player.getUniqueId().equals(id));
        finishRevision.remove(id);
        preCast.remove(id);
        regainAt.remove(id);
        upgradeFailedAt.remove(id);
        lastEarn.keySet().removeIf(k -> k.startsWith(id.toString()));
    }

    private void log(String s) {
        lastLog = s;
        if (plugin.debug()) plugin.getLogger().info("rank-up: " + s);
    }
}

