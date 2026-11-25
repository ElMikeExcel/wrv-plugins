package excelemfixes.mcmmo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The mcMMO XP Connector's engine: hit provenance for the
 * per-damage veto, kill-award computation and delivery, the
 * hourly ceiling ledger, and the deferred-award queue for
 * awards earned inside no-XP worlds.
 *
 * <p>Delivery rule that shapes everything here: an award is
 * granted through mcMMO's normal gain pipeline (via
 * {@link XpPort}) so sharing and multipliers compose — but
 * that pipeline silently pays 0 to a player standing in a
 * no-XP world (the mcMMO build's world gate eats the gain
 * before any event fires). So an award earned in such a world
 * is QUEUED, persisted, and granted the moment the earner is
 * in an ordinary world. The award is never granted by writing
 * mcMMO's profile directly: that path skips the pipeline's
 * events, sharing, multipliers and level-up processing all at
 * once.
 *
 * <p>This class touches only Bukkit types plus the module's
 * pure logic; EliteMobs and mcMMO are reached through the
 * {@link ElitePort} / {@link XpPort} bridges.
 */
public final class KseaEngine implements Listener, XpPort.Hooks {

    /** Module settings, re-read on reload. */
    public record Settings(boolean vetoEnabled, boolean awardsEnabled,
                           AwardRates.Rates rates, int ceiling,
                           int shareBudget,
                           long tokenTtlTicks, boolean debug,
                           boolean watchdogEnabled, int watchdogFlagMinuteXp,
                           int watchdogFlagHourXp, int watchdogCapHourXp) {

        static Settings from(ConfigurationSection section) {
            AwardRates.Rates defaults = AwardRates.Rates.defaults();
            if (section == null) {
                return new Settings(true, true, defaults, 60_000,
                        8, 6L, false, true, 25_000, 90_000, 120_000);
            }
            AwardRates.Rates rates = new AwardRates.Rates(
                    section.getInt("rates.boss", defaults.boss()),
                    section.getInt("rates.miniboss", defaults.miniboss()),
                    section.getInt("rates.natural-elite",
                            defaults.natural()),
                    section.getInt("rates.trash", defaults.trash()));
            return new Settings(
                    section.getBoolean("per-damage-veto", true),
                    section.getBoolean("kill-awards", true),
                    rates,
                    Math.max(1, section.getInt("hourly-ceiling", 60_000)),
                    Math.max(0, section.getInt("share-veto-budget", 8)),
                    Math.max(1L, section.getLong("hit-token-ttl-ticks", 6L)),
                    section.getBoolean("debug", false),
                    section.getBoolean("watchdog.enabled", true),
                    Math.max(1, section.getInt(
                            "watchdog.flag-minute-xp", 25_000)),
                    Math.max(1, section.getInt(
                            "watchdog.flag-hour-xp", 90_000)),
                    Math.max(1, section.getInt(
                            "watchdog.cap-hour-xp", 120_000)));
        }
    }

    /** One queued award awaiting an ordinary-world delivery. */
    private record PendingAward(UUID player, String skill, int amount,
                                long queuedAtMillis) {
    }

    /** The last player-side hit on an entity, for kill attribution. */
    private record LastHit(UUID earner, String skill, boolean petHit,
                           boolean castSummon, long tick) {
    }

    private final JavaPlugin plugin;
    private final Logger logger;
    private final ElitePort elitePort;
    private volatile XpPort xpPort;
    private final NoXpRecognition recognition;
    private final File ledgerFile;
    private final File pendingFile;
    private final File watchdogFile;
    private final File flagsFile;

    private volatile Settings settings;
    private final HitTokens tokens = new HitTokens();
    private final Map<UUID, LastHit> lastHits = new HashMap<>();
    private final List<PendingAward> pending = new ArrayList<>();
    private CeilingLedger ledger = new CeilingLedger();
    private WatchdogLedger watchdog = new WatchdogLedger();

    /**
     * Watchdog cap state: key (uuid|skill) -> the millisecond
     * at which the blocked state may be re-checked against the
     * ledger. Absent = not blocked. The veto path reads this
     * map only (never scans the ledger per event); the entry
     * is refreshed by the MONITOR observer when a gain pushes
     * the rolling hour to the cap, and re-derived lazily once
     * the recheck moment passes.
     */
    private final Map<String, Long> capRecheckAt = new HashMap<>();
    private long lastWatchdogSaveMillis = 0L;

    /** Final amount of the in-flight grant, set by the MONITOR hook. */
    private final ThreadLocal<Float> grantObserved = new ThreadLocal<>();

    public KseaEngine(JavaPlugin plugin, ElitePort elitePort, XpPort xpPort,
                      NoXpRecognition recognition, Settings settings) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.elitePort = elitePort;
        this.xpPort = xpPort;
        this.recognition = recognition;
        this.settings = settings;
        File dir = plugin.getDataFolder();
        this.ledgerFile = new File(dir, "mcmmo-connector-ledger.txt");
        this.pendingFile = new File(dir, "mcmmo-connector-pending.txt");
        this.watchdogFile = new File(dir, "mcmmo-watchdog-ledger.txt");
        this.flagsFile = new File(dir, "mcmmo-watchdog-flags.txt");
        loadState();
    }

    /**
     * Wires the XP bridge after construction (the bridge's
     * listeners consult this engine as their hooks, and this
     * engine grants through the bridge — the module class
     * builds the engine first, then the bridge, then sets the
     * port here, before either is registered).
     */
    public void setXpPort(XpPort xpPort) {
        this.xpPort = xpPort;
    }

    public void reload(ConfigurationSection section) {
        this.settings = Settings.from(section);
    }

    private void debug(String message) {
        if (settings.debug) logger.info("mcMMO XP Connector: " + message);
    }

    private long nowTick() {
        return Bukkit.getCurrentTick();
    }

    // ------------------------------------------------------------ hooks

    @Override
    public boolean vetoGain(UUID player, String skill) {
        boolean veto = tokens.consumeForGain(player, skill, nowTick(),
                settings.tokenTtlTicks);
        if (veto) {
            debug("vetoed a " + skill + " per-damage gain for " + player
                    + " (EliteMobs target)");
        }
        return veto;
    }

    @Override
    public boolean vetoShare(UUID receiver, String skill) {
        List<HitTokens.Token> candidates = tokens.shareCandidates(skill,
                nowTick(), settings.tokenTtlTicks);
        // Other earners' tokens first: a member's share of someone
        // else's EliteMobs hit.
        for (HitTokens.Token token : candidates) {
            if (token.earner().equals(receiver)) continue;
            if (xpPort.sameParty(token.earner(), receiver)) {
                tokens.spendShareBudget(token);
                debug("vetoed a shared " + skill + " gain for " + receiver
                        + " (share of an EliteMobs hit)");
                return true;
            }
        }
        // The earner's OWN share copy last: when the earner is in
        // a party, mcMMO's ShareHandler replaces the earner's
        // primary gain with a PARTY_MEMBERS redistribution, so
        // the earner's copy of their own vetoed hit arrives as a
        // SHARED_PVE event addressed to the earner themself.
        // Skipping it (the original shape) let exactly that copy
        // pay out — rig-proven leak, run 4 of the KSEA fixtures.
        for (HitTokens.Token token : candidates) {
            if (!token.earner().equals(receiver)) continue;
            tokens.spendShareBudget(token);
            debug("vetoed a shared " + skill + " gain for " + receiver
                    + " (earner's own share of an EliteMobs hit)");
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------ watchdog

    /**
     * The watchdog's ear: every gain the pipeline finally
     * wrote, observed at MONITOR. Records the gain, raises a
     * flag record on a threshold crossing (flagging never
     * blocks anything), and arms the cap state when the
     * rolling hour reaches the cap.
     */
    @Override
    public void observedGain(UUID player, String skill, float amount,
                             String worldName) {
        if (!settings.watchdogEnabled || amount <= 0) return;
        long now = System.currentTimeMillis();
        watchdog.record(player, skill, now, amount);
        double minuteTotal = watchdog.totalLast(player, skill, now,
                WatchdogLedger.MINUTE_MS);
        double hourTotal = watchdog.totalLast(player, skill, now,
                WatchdogLedger.HOUR_MS);
        if (minuteTotal >= settings.watchdogFlagMinuteXp
                && minuteTotal - amount < settings.watchdogFlagMinuteXp) {
            writeFlag(player, skill, "MINUTE", minuteTotal,
                    settings.watchdogFlagMinuteXp, worldName, now);
        }
        if (hourTotal >= settings.watchdogFlagHourXp
                && hourTotal - amount < settings.watchdogFlagHourXp) {
            writeFlag(player, skill, "HOUR", hourTotal,
                    settings.watchdogFlagHourXp, worldName, now);
        }
        if (hourTotal >= settings.watchdogCapHourXp) {
            capRecheckAt.put(player + "|" + skill, now + 1_000L);
        }
        if (now - lastWatchdogSaveMillis > 30_000L) {
            lastWatchdogSaveMillis = now;
            saveWatchdog();
        }
    }

    /**
     * The watchdog's teeth, consulted by the veto listener:
     * true while this player+skill's rolling hour is at or
     * over the cap. Reads only the precomputed recheck map;
     * the ledger is re-derived at most once per second per
     * key, when the recheck moment passes.
     */
    @Override
    public boolean watchdogCapped(UUID player, String skill) {
        if (!settings.watchdogEnabled) return false;
        String k = player + "|" + skill;
        Long recheck = capRecheckAt.get(k);
        if (recheck == null) return false;
        long now = System.currentTimeMillis();
        if (now < recheck) return true;
        double hourTotal = watchdog.totalLast(player, skill, now,
                WatchdogLedger.HOUR_MS);
        if (hourTotal >= settings.watchdogCapHourXp) {
            capRecheckAt.put(k, now + 1_000L);
            return true;
        }
        capRecheckAt.remove(k);
        return false;
    }

    /**
     * One flag record: appended to the flags file in the
     * module data folder and mirrored as one warning log
     * line. Names the player, the skill, the window totals,
     * the world the gains landed in, and how many EliteMobs
     * hit tokens were live for that player+skill — the
     * context a human needs to judge whether the rate came
     * from EliteMobs combat or from somewhere else entirely.
     */
    private void writeFlag(UUID player, String skill, String window,
                           double windowTotal, int threshold,
                           String worldName, long nowMillis) {
        Player bukkit = Bukkit.getPlayer(player);
        String name = bukkit == null ? "?" : bukkit.getName();
        int liveTokens = tokens.liveCountFor(player, skill, nowTick(),
                settings.tokenTtlTicks);
        String line = java.time.Instant.ofEpochMilli(nowMillis).toString()
                + "\t" + player + "\t" + name + "\t" + skill
                + "\t" + window + "\t" + (long) windowTotal
                + "\t" + threshold + "\t" + worldName
                + "\t" + liveTokens;
        try {
            File dir = flagsFile.getParentFile();
            if (dir != null) dir.mkdirs();
            Files.writeString(flagsFile.toPath(), line + "\n",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            logger.warning("mcMMO XP Connector: could not write the "
                    + "watchdog flags file (" + e + ").");
        }
        logger.warning("mcMMO XP Connector: watchdog flag: " + name
                + " (" + player + ") gained " + (long) windowTotal
                + " " + skill + " XP in one " + window.toLowerCase()
                + " (flag threshold " + threshold + "), world "
                + worldName + ", live EliteMobs hit tokens "
                + liveTokens + " — recorded in "
                + flagsFile.getName() + "; gains are NOT blocked "
                + "by a flag.");
    }

    @Override
    public void grantedFinal(UUID player, String skill, float amount) {
        grantObserved.set(amount);
        if (amount > 0) {
            ledger.debit(player, skill, amount, System.currentTimeMillis());
            saveLedger();
        }
    }

    // ------------------------------------------------------------- hits

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        Entity damager = event.getDamager();

        UUID earner = null;
        String skill = null;
        boolean petHit = false;
        boolean castSummon = false;
        if (damager instanceof Player player) {
            earner = player.getUniqueId();
            skill = WeaponSkills.forMelee(mainHandName(player));
        } else if (damager instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player player) {
                earner = player.getUniqueId();
                skill = WeaponSkills.forShot(mainHandName(player),
                        projectile.getType().name());
                if (skill == null) skill = WeaponSkills.UNARMED;
            }
        } else if (damager instanceof Wolf wolf) {
            if (wolf.getOwner() instanceof Player owner) {
                earner = owner.getUniqueId();
                skill = WeaponSkills.TAMING;
                petHit = true;
                castSummon = recognition.isCastSummon(wolf);
            }
        }
        if (earner == null || skill == null) return;

        ElitePort.EliteInfo info = elitePort.info(target);
        if (!info.tracked()) return;

        long tick = nowTick();
        lastHits.put(target.getUniqueId(),
                new LastHit(earner, skill, petHit, castSummon, tick));
        pruneLastHits(tick);

        if (settings.vetoEnabled
                && !recognition.isNoXpEntity(target, info)) {
            tokens.plant(earner, skill, tick, target.getUniqueId(),
                    settings.shareBudget);
        }
    }

    private void pruneLastHits(long nowTick) {
        lastHits.values().removeIf(h -> nowTick - h.tick() > 400);
    }

    private static String mainHandName(Player player) {
        return player.getInventory().getItemInMainHand().getType().name();
    }

    // ------------------------------------------------------------ deaths

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        if (!settings.awardsEnabled) return;
        LivingEntity entity = event.getEntity();
        ElitePort.EliteInfo info = elitePort.info(entity);
        LastHit lastHit = lastHits.remove(entity.getUniqueId());
        if (!info.tracked()) return;
        if (recognition.isNoXpEntity(entity, info)) {
            debug("no award: recognised no-XP entity ("
                    + info.filename() + ")");
            return;
        }
        if (info.passivePracticeTarget()) {
            debug("no award: passive practice target ("
                    + info.filename() + ")");
            return;
        }

        UUID earner = null;
        String skill = null;
        Player killer = entity.getKiller();
        // Pet kills resolve FIRST when the module's own last-hit
        // record says the last hit was a pet's: Bukkit credits a
        // tamed pet's kill to its owner, so getKiller() alone
        // cannot tell a pet kill from the owner's own melee kill
        // (rig-proven: an adopted wolf's kill was priced as a
        // SWORDS kill with the owner's held sword). The record
        // always holds the LATEST hit, so a petHit record means
        // the pet's hit really was the last one before death.
        if (lastHit != null && lastHit.petHit()
                && nowTick() - lastHit.tick() <= 200
                && (killer == null
                    || killer.getUniqueId().equals(lastHit.earner()))) {
            if (!lastHit.castSummon()) {
                earner = lastHit.earner();
                skill = WeaponSkills.TAMING;
            } else {
                debug("no award: killing pet was a cast summon");
            }
        } else if (killer != null) {
            earner = killer.getUniqueId();
            skill = killingBlowSkill(entity, killer);
        } else if (lastHit != null
                && nowTick() - lastHit.tick() <= 200) {
            earner = lastHit.earner();
            skill = lastHit.skill();
        }
        if (earner == null || skill == null) {
            debug("no award: no player killing blow for "
                    + info.filename());
            return;
        }

        int award = AwardRates.award(info.rankClass(), info.level(),
                settings.rates);
        if (award <= 0) return;
        debug("kill award: " + award + " " + skill + " XP for " + earner
                + " (" + info.rankClass() + " x level " + info.level()
                + ", " + info.filename() + ")");
        offerAward(earner, skill, award);
    }

    private String killingBlowSkill(LivingEntity entity, Player killer) {
        if (entity.getLastDamageCause()
                instanceof EntityDamageByEntityEvent byEntity
                && byEntity.getDamager() instanceof Projectile projectile) {
            String shot = WeaponSkills.forShot(mainHandName(killer),
                    projectile.getType().name());
            if (shot != null) return shot;
        }
        return WeaponSkills.forMelee(mainHandName(killer));
    }

    // ----------------------------------------------------------- awards

    /**
     * Routes one computed award: grant now, or queue it.
     *
     * <p>The grant is always attempted, even in a no-XP world:
     * with the module's event-layer world gate the marked grant
     * passes the module's own veto and pays instantly in-world.
     * When the pipeline produces no earner event at all — the
     * gated fork build still drops in-world grants before any
     * event exists, or the player is in Creative — grantNow's
     * no-event path queues the award, and the queue pays it on
     * world exit exactly as the original deferral did. Deferral
     * is therefore reactive (a property of what the pipeline
     * did) rather than pre-decided from the world name.
     */
    private void offerAward(UUID earner, String skill, int amount) {
        Player player = Bukkit.getPlayer(earner);
        if (player == null) {
            enqueue(earner, skill, amount);
            return;
        }
        grantNow(earner, skill, amount);
    }

    /**
     * Grants through the pipeline, fitted to the ceiling. The
     * ledger is debited by the MONITOR hook with the final
     * post-pipeline amount. If the pipeline produces no earner
     * event at all (the player is in Creative, or another
     * plugin zeroed the grant), the award is queued instead of
     * vanishing.
     */
    private void grantNow(UUID earner, String skill, int amount) {
        long now = System.currentTimeMillis();
        int fit = ledger.fit(earner, skill, amount, settings.ceiling, now);
        if (fit <= 0) {
            debug("award held by the hourly ceiling for " + earner
                    + " (" + skill + ")");
            return;
        }
        if (fit < amount) {
            debug("award partially fitted to the ceiling: " + fit
                    + " of " + amount + " (" + skill + ")");
        }
        grantObserved.remove();
        boolean handed = xpPort.grant(earner, skill, fit);
        Float observed = grantObserved.get();
        grantObserved.remove();
        if (!handed || observed == null) {
            enqueue(earner, skill, amount);
            debug("award queued: the pipeline produced no grant for "
                    + earner + " (" + skill + ")");
        }
    }

    private void enqueue(UUID earner, String skill, int amount) {
        pending.add(new PendingAward(earner, skill, amount,
                System.currentTimeMillis()));
        savePending();
    }

    /** Pays out a player's queued awards when delivery is possible. */
    public void deliverPending(Player player) {
        if (player == null || !player.isOnline()) return;
        UUID id = player.getUniqueId();
        boolean changed = false;
        Iterator<PendingAward> it = pending.iterator();
        while (it.hasNext()) {
            PendingAward award = it.next();
            if (!award.player().equals(id)) continue;
            long now = System.currentTimeMillis();
            int fit = ledger.fit(id, award.skill(), award.amount(),
                    settings.ceiling, now);
            if (fit <= 0) continue; // ceiling full: stays queued
            grantObserved.remove();
            boolean handed = xpPort.grant(id, award.skill(), fit);
            Float observed = grantObserved.get();
            grantObserved.remove();
            if (handed && observed != null) {
                it.remove();
                changed = true;
                debug("delivered deferred award: " + fit + " "
                        + award.skill() + " XP for " + id);
            }
        }
        if (changed) savePending();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // mcMMO loads profiles asynchronously; give it time.
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> deliverPending(event.getPlayer()), 40L);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> deliverPending(event.getPlayer()), 5L);
    }

    /** Startup sweep for players already online (reload case). */
    public void sweepOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            deliverPending(player);
        }
    }

    // -------------------------------------------------------- persistence

    private void loadState() {
        try {
            if (ledgerFile.isFile()) {
                ledger = CeilingLedger.deserialize(Files.readString(
                        ledgerFile.toPath(), StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            logger.warning("mcMMO XP Connector: could not read the "
                    + "ceiling ledger (" + e + "); starting a fresh one.");
            ledger = new CeilingLedger();
        }
        try {
            if (watchdogFile.isFile()) {
                watchdog = WatchdogLedger.deserialize(Files.readString(
                        watchdogFile.toPath(), StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            logger.warning("mcMMO XP Connector: could not read the "
                    + "watchdog ledger (" + e + "); starting a fresh one.");
            watchdog = new WatchdogLedger();
        }
        try {
            if (pendingFile.isFile()) {
                for (String line : Files.readAllLines(pendingFile.toPath(),
                        StandardCharsets.UTF_8)) {
                    String[] parts = line.trim().split("\t");
                    if (parts.length != 4) continue;
                    try {
                        pending.add(new PendingAward(
                                UUID.fromString(parts[0].trim()),
                                parts[1].trim(),
                                Integer.parseInt(parts[2].trim()),
                                Long.parseLong(parts[3].trim())));
                    } catch (IllegalArgumentException ignored) {
                        // Skip the bad line.
                    }
                }
            }
        } catch (IOException e) {
            logger.warning("mcMMO XP Connector: could not read the "
                    + "pending-award queue (" + e + ").");
        }
    }

    /** Saves all state files; called on shutdown too. */
    public void saveState() {
        saveLedger();
        savePending();
        saveWatchdog();
    }

    private void saveWatchdog() {
        writeAtomically(watchdogFile,
                watchdog.serialize(System.currentTimeMillis()));
    }

    private void saveLedger() {
        writeAtomically(ledgerFile,
                ledger.serialize(System.currentTimeMillis()));
    }

    private void savePending() {
        StringBuilder sb = new StringBuilder();
        for (PendingAward award : pending) {
            sb.append(award.player()).append('\t').append(award.skill())
                    .append('\t').append(award.amount()).append('\t')
                    .append(award.queuedAtMillis()).append('\n');
        }
        writeAtomically(pendingFile, sb.toString());
    }

    private void writeAtomically(File file, String content) {
        try {
            File dir = file.getParentFile();
            if (dir != null) dir.mkdirs();
            File tmp = new File(dir, file.getName() + ".tmp");
            Files.writeString(tmp.toPath(), content,
                    StandardCharsets.UTF_8);
            Files.move(tmp.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.warning("mcMMO XP Connector: could not save "
                    + file.getName() + " (" + e + ").");
        }
    }

    // ------------------------------------------------------------ test hooks

    /** Pending awards for a player (rig/tests). */
    public int pendingCount(UUID player) {
        int count = 0;
        for (PendingAward award : pending) {
            if (award.player().equals(player)) count++;
        }
        return count;
    }

    /** The ceiling ledger (rig/tests). */
    public CeilingLedger ledger() {
        return ledger;
    }
}
