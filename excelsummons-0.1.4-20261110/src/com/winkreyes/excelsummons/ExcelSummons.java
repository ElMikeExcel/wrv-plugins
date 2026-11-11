package com.winkreyes.excelsummons;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelSummons — the summoning tree's bridge (DESIGN §4.3). One job:
 * know which entities are summons and whose they are, and carry the
 * four jobs that hang off that knowledge — attribution (via
 * ExcelSummonsAPI), allegiance (target engine + scrubs in
 * SummonsListener), explosion safety, and bridge-enforced lifetime.
 *
 * It owns no combat content of its own: the spells live in Magic, the
 * boss ledger in ExcelBossTrial, the records in ExcelProtect. This
 * plugin is the registry they all read.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class ExcelSummons extends JavaPlugin {

    private final SummonRegistry registry = new SummonRegistry();
    private final Map<String, CapPolicy.SpellRule> rules = new LinkedHashMap<>();
    private CapPolicy capPolicy;
    private PartyProvider partyProvider = PartyProvider.NONE;
    private SummonsListener listener;

    private boolean partySharing = true;
    private double engageRadius = 32.0;
    private double leashDistance = 32.0;
    private long aggressorMemoryMs = 30_000L;
    private long targetTicks = 40L;
    private long sweepTicks = 100L;
    // 0.1.2: Guardian taunt + tank skin, and the mana-reserve upkeep.
    private double tauntRadius = 16.0;
    private double guardianDamageReduction = 0.40;
    private final Map<String, Integer> reservePercents = new LinkedHashMap<>();
    private int reserveCeilingPct = 75;
    private int reserveFloorPct = 20;

    // ------------------------------------------------------------ lifecycle

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        loadRegistry();
        ExcelSummonsAPI.install(registry);
        partyProvider = new TogetherPartyProvider(getLogger());
        listener = new SummonsListener(this);
        getServer().getPluginManager().registerEvents(listener, this);
        Bukkit.getScheduler().runTaskTimer(this, listener::targetTick, 60L, targetTicks);
        Bukkit.getScheduler().runTaskTimer(this, listener::sweepTick, 100L, sweepTicks);
        getLogger().info("ExcelSummons " + BuildInfo.VERSION + " enabled (build "
                + BuildInfo.BUILD_ID + "); registry holds " + registry.size() + " summon(s).");
    }

    @Override
    public void onDisable() {
        // Server stop: combat summons are removed (they were temporary
        // by design); pets and mounts are real entities that persist in
        // the world and stay registered for the next start.
        for (SummonRecord rec : new ArrayList<>(registry.all())) {
            if (LifetimePolicy.decide(rec, System.currentTimeMillis(), false, true)
                    == LifetimePolicy.Decision.REMOVE) {
                Entity e = Bukkit.getEntity(rec.entityId());
                if (e != null) e.remove();
                registry.unregister(rec.entityId());
            }
        }
        saveRegistry();
        ExcelSummonsAPI.uninstall();
        getLogger().info("ExcelSummons disabled; registry saved with "
                + registry.size() + " persistent summon(s).");
    }

    private void loadSettings() {
        rules.clear();
        ConfigurationSection spells = getConfig().getConfigurationSection("summon-spells");
        if (spells != null) {
            for (String key : spells.getKeys(false)) {
                ConfigurationSection s = spells.getConfigurationSection(key);
                if (s == null) continue;
                rules.put(key, new CapPolicy.SpellRule(
                        SummonRecord.Branch.fromConfig(s.getString("branch", "combat")),
                        s.getInt("cap", 0),
                        s.getLong("duration-ms", 0L),
                        s.getBoolean("enforce-cap", true)));
            }
        }
        // 0.1.4 (§3 as amended 10:35 AM): the unified per-caster
        // TOTAL cap is rank-scaled — the summon-cap-by-rank map
        // replaces the retired per-caster-combat-cap key
        // (subsumed by the total — see CapPolicy). A missing or
        // empty section falls back to the locked ladder.
        Map<String, Integer> capMap = new LinkedHashMap<>();
        ConfigurationSection capSection = getConfig()
                .getConfigurationSection("summon-cap-by-rank");
        if (capSection != null) {
            for (String key : capSection.getKeys(false)) {
                capMap.put(key.toLowerCase(), Math.max(1, capSection.getInt(key, 1)));
            }
        }
        if (capMap.isEmpty()) capMap.putAll(RankCap.lockedLadder());
        capPolicy = new CapPolicy(capMap,
                getConfig().getInt("server-combat-ceiling", 12));
        partySharing = getConfig().getBoolean("party-target-sharing", true);
        aggressorMemoryMs = Math.max(1000L,
                getConfig().getLong("aggressor-memory-ms", 30_000L));
        engageRadius = getConfig().getDouble("engage-radius", 32.0);
        leashDistance = getConfig().getDouble("leash-distance", 32.0);
        targetTicks = Math.max(10L, getConfig().getLong("target-tick-ticks", 40L));
        sweepTicks = Math.max(20L, getConfig().getLong("sweep-ticks", 100L));
        tauntRadius = Math.max(1.0, getConfig().getDouble("guardian-taunt-radius", 16.0));
        guardianDamageReduction = Math.max(0.0, Math.min(0.95,
                getConfig().getDouble("guardian-damage-reduction", 0.40)));
        reserveCeilingPct = Math.max(1,
                getConfig().getInt("reserve-ceiling-percent", 75));
        reserveFloorPct = Math.max(0,
                getConfig().getInt("reserve-floor-percent", 20));
        reservePercents.clear();
        ConfigurationSection reserve = getConfig()
                .getConfigurationSection("summon-reserve-percent");
        if (reserve != null) {
            for (String key : reserve.getKeys(false)) {
                reservePercents.put(key, Math.max(0, reserve.getInt(key, 0)));
            }
        }
    }

    // ------------------------------------------------------------ accessors

    public SummonRegistry registry() { return registry; }
    public CapPolicy capPolicy() { return capPolicy; }
    public PartyProvider partyProvider() { return partyProvider; }
    public boolean partySharing() { return partySharing; }
    public long aggressorMemoryMs() { return aggressorMemoryMs; }
    public double engageRadius() { return engageRadius; }
    public double leashDistance() { return leashDistance; }
    public double tauntRadius() { return tauntRadius; }
    public double guardianDamageReduction() { return guardianDamageReduction; }
    public Map<String, Integer> reservePercents() { return reservePercents; }
    public int reserveCeilingPct() { return reserveCeilingPct; }
    public int reserveFloorPct() { return reserveFloorPct; }

    /** Per-family caps for the reserve-bearing families —
     *  ReservePolicy.allKeys needs them to know which count-tiered
     *  modifier keys exist (archer2, wolf5, familiar3, ...) and
     *  must be swept. 0.1.4 (§2.1e): ANY branch with a configured
     *  percent > 0 — the wolf's cap of 5 is what makes the
     *  grandfathered wolf4/wolf5 tier keys exist (§3.3). */
    public Map<String, Integer> reserveFamilyCaps() {
        Map<String, Integer> caps = new LinkedHashMap<>();
        for (Map.Entry<String, CapPolicy.SpellRule> e : rules.entrySet()) {
            if (ReservePolicy.percentFor(reservePercents, e.getKey()) > 0) {
                caps.merge(SummonRecord.baseKey(e.getKey()),
                        Math.max(1, e.getValue().cap), Math::max);
            }
        }
        return caps;
    }

    /** Rule for a cast spell key: exact (tier) key first, family base second. */
    public CapPolicy.SpellRule ruleFor(String spellKey) {
        CapPolicy.SpellRule exact = rules.get(spellKey);
        if (exact != null) return exact;
        return rules.get(SummonRecord.baseKey(spellKey));
    }

    public boolean isManaged(String spellKey) {
        return spellKey != null && ruleFor(spellKey) != null;
    }

    // ------------------------------------------- 0.1.4: earned rank
    // The rank-scaled cap reads the caster's EARNED rank from the
    // same record the wand reconcile reads: ExcelMagicGate's
    // earned.yml (players.<uuid>.credited_total), derived through
    // the locked thresholds (RankCap). The file is re-read only
    // when its size/mtime stamp changes (the verified-stamps
    // discipline); any failure — file missing, player absent,
    // total unreadable — yields null, and CapPolicy fails the cap
    // to the LOWEST rung. MagicGate is the only writer; this is a
    // read-only seam.

    private Map<UUID, Long> earnedTotals = Map.of();
    private long earnedStampSize = -1L;
    private long earnedStampMtime = -1L;

    private File earnedFile() {
        File plugins = getDataFolder().getParentFile();
        return plugins == null ? null
                : new File(plugins, "ExcelMagicGate" + File.separator + "earned.yml");
    }

    private synchronized void refreshEarnedTotals() {
        File f = earnedFile();
        if (f == null || !f.exists()) {
            earnedTotals = Map.of();
            earnedStampSize = -1L;
            earnedStampMtime = -1L;
            return;
        }
        long size = f.length();
        long mtime = f.lastModified();
        if (size == earnedStampSize && mtime == earnedStampMtime) return;
        try {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
            ConfigurationSection players = yml.getConfigurationSection("players");
            Map<UUID, Long> out = new LinkedHashMap<>();
            if (players != null) {
                for (String key : players.getKeys(false)) {
                    try {
                        UUID id = UUID.fromString(key);
                        out.put(id, players.getLong(key + ".credited_total", 0L));
                    } catch (IllegalArgumentException ignored) {
                        // a non-UUID row is skipped, never fatal
                    }
                }
            }
            earnedTotals = out;
            earnedStampSize = size;
            earnedStampMtime = mtime;
        } catch (Throwable t) {
            getLogger().warning("ExcelSummons: earned.yml read failed (caps fail "
                    + "to the lowest rung): " + t.getMessage());
            earnedTotals = Map.of();
            earnedStampSize = -1L;
            earnedStampMtime = -1L;
        }
    }

    /** The caster's earned rank key (beginner/student/apprentice/
     *  master) from MagicGate's earned.yml, or null when it cannot
     *  be read. */
    public String earnedRankOf(UUID playerId) {
        if (playerId == null) return null;
        refreshEarnedTotals();
        Long credited = earnedTotals.get(playerId);
        return credited == null ? null : RankCap.rankForCredited(credited);
    }

    // ------------------------------------------------------------ persistence

    private File dataFile() {
        return new File(getDataFolder(), "summons-data.yml");
    }

    private void loadRegistry() {
        File f = dataFile();
        if (!f.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
        List<Map<String, Object>> rows = new ArrayList<>();
        List<?> raw = yml.getList("summons");
        if (raw != null) {
            for (Object o : raw) {
                if (o instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> row = (Map<String, Object>) o;
                    rows.add(row);
                }
            }
        }
        registry.restore(rows);
    }

    /** Write the registry to disk. Called on every mutation: the file is
     *  tiny and the Wolf lesson says a registry that only lives in
     *  memory is a registry that lies after the first relog. */
    public synchronized void saveRegistry() {
        File f = dataFile();
        try {
            if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
            YamlConfiguration yml = new YamlConfiguration();
            yml.set("summons", registry.snapshot());
            String body = yml.saveToString();
            java.nio.file.Files.writeString(f.toPath(),
                    "# ExcelSummons registry — build " + BuildInfo.BUILD_ID + "\n" + body);
        } catch (IOException e) {
            getLogger().warning("ExcelSummons: registry save failed: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------ command

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("ExcelSummons " + BuildInfo.VERSION
                    + " — /summons list | dismiss [all|pets] | admin reload");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "list": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("The registry holds " + registry.size() + " summon(s).");
                    return true;
                }
                Player p = (Player) sender;
                List<SummonRecord> mine = registry.ofOwner(p.getUniqueId());
                if (mine.isEmpty()) {
                    p.sendMessage("You have no summons registered.");
                    return true;
                }
                for (SummonRecord r : mine) {
                    p.sendMessage(listLineFor(r));
                }
                int heldTotal = ReservePolicy.totalPercent(registry, p.getUniqueId(),
                        reservePercents);
                if (heldTotal > 0) {
                    p.sendMessage("Mana held by your summons: " + heldTotal
                            + "% of your pool.");
                }
                return true;
            }
            case "dismiss": {
                if (!(sender instanceof Player)) return true;
                Player p = (Player) sender;
                // 0.1.4 (§2.4): `dismiss pets` is the player's own
                // release for cast companion/pack pets. Bare and
                // `all` keep their exact 0.1.3 meanings.
                AdminPolicy.DismissScope scope = AdminPolicy.DismissScope.MOUNT_ONLY;
                if (args.length > 1 && args[1].equalsIgnoreCase("all")) {
                    scope = AdminPolicy.DismissScope.ALL;
                } else if (args.length > 1 && args[1].equalsIgnoreCase("pets")) {
                    scope = AdminPolicy.DismissScope.PETS;
                }
                int n = listener.dismiss(p.getUniqueId(), scope);
                p.sendMessage(n == 0 ? "Nothing to dismiss."
                        : "Dismissed " + n + " summon(s).");
                return true;
            }
            case "admin": {
                // 0.1.3 console admin (audit §4): admin list/dismiss
                // are routed and gated by AdminPolicy BEFORE the
                // legacy admin body below; the gate (console
                // explicit, otherwise the EXISTING excelsummons.admin)
                // precedes all target handling. admin reload and the
                // bare admin usage keep the pre-0.1.3 behavior.
                AdminPolicy.Parsed parsed = AdminPolicy.route(args,
                        sender instanceof ConsoleCommandSender,
                        sender.hasPermission(AdminPolicy.PERMISSION));
                switch (parsed.route()) {
                    case ADMIN_DENIED -> {
                        sender.sendMessage("No permission.");
                        return true;
                    }
                    case ADMIN_USAGE -> {
                        sender.sendMessage("Usage: /summons admin list <player>"
                                + " | /summons admin dismiss <player> [all|pets]");
                        return true;
                    }
                    case ADMIN_LIST -> { adminList(sender, parsed.target()); return true; }
                    case ADMIN_DISMISS -> {
                        adminDismiss(sender, parsed.target(), parsed.scope());
                        return true;
                    }
                    default -> { /* LEGACY: fall through to the pre-0.1.3 admin body */ }
                }
                if (!sender.hasPermission("excelsummons.admin")) {
                    sender.sendMessage("No permission.");
                    return true;
                }
                if (args.length > 1 && args[1].equalsIgnoreCase("reload")) {
                    reloadConfig();
                    loadSettings();
                    sender.sendMessage("ExcelSummons settings reloaded; registry holds "
                            + registry.size() + " summon(s).");
                    return true;
                }
                sender.sendMessage("Usage: /summons admin reload");
                return true;
            }
            default:
                sender.sendMessage("Usage: /summons list | dismiss [all|pets] | admin reload");
                return true;
        }
    }

    // ------------------------------------------------- 0.1.3 console admin
    // Thin Bukkit mechanics for AdminPolicy's decisions: online-player
    // lookup, the shared list-line render, and the EXISTING dismiss
    // path. Every routing/gate/selection decision is AdminPolicy's.

    /** Render one summon's list line — the format the self list has
     *  always used (Nameplate.listLine), shared with the admin list
     *  so an operator sees exactly what the player sees. */
    private String listLineFor(SummonRecord r) {
        String left = r.expiresAtMillis() == null ? "persistent"
                : Math.max(0, (r.expiresAtMillis() - System.currentTimeMillis()) / 1000)
                  + "s left";
        Entity ent = Bukkit.getEntity(r.entityId());
        String hp;
        if (ent instanceof org.bukkit.entity.LivingEntity) {
            org.bukkit.entity.LivingEntity le =
                    (org.bukkit.entity.LivingEntity) ent;
            hp = "HP " + Nameplate.formatHp(le.getHealth()) + "/"
                    + Nameplate.formatHp(SummonsListener.maxHealth(le));
        } else {
            hp = "not loaded";
        }
        // A summon's mana-reserve hold is part of its list line —
        // the same percentage the mage's modifiers show
        // (ReservePolicy). 0.1.4 (§2.1f): every branch prints its
        // hold, not just combat — companions and the mount hold
        // reserve now too.
        int hold = ReservePolicy.percentFor(reservePercents, r.spellKey());
        return Nameplate.listLine(r.spellKey(), r.branch().name().toLowerCase(),
                left, hp, hold);
    }

    /** The admin target: an ONLINE player by name, exact and
     *  case-insensitive (AdminPolicy.nameMatches). Null when no
     *  online player carries that name — unknown and offline are
     *  the same clean no-op to the operator. */
    private Player onlineByName(String target) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (AdminPolicy.nameMatches(p.getName(), target)) return p;
        }
        return null;
    }

    private static String actorOf(CommandSender sender) {
        return sender instanceof ConsoleCommandSender ? "CONSOLE" : sender.getName();
    }

    /** /summons admin list <player>: the target's summons in the
     *  self-list format, plus the held-total line. Read-only. */
    private void adminList(CommandSender sender, String targetName) {
        Player target = onlineByName(targetName);
        if (target == null) {
            sender.sendMessage("Summons admin: no player named '" + targetName
                    + "' is online. Nothing was changed.");
            return;
        }
        List<SummonRecord> theirs = registry.ofOwner(target.getUniqueId());
        if (theirs.isEmpty()) {
            sender.sendMessage(target.getName() + " has no summons registered.");
            return;
        }
        sender.sendMessage("Summons for " + target.getName() + ":");
        for (SummonRecord r : theirs) {
            sender.sendMessage(listLineFor(r));
        }
        int heldTotal = ReservePolicy.totalPercent(registry, target.getUniqueId(),
                reservePercents);
        if (heldTotal > 0) {
            sender.sendMessage("Mana held by " + target.getName() + "'s summons: "
                    + heldTotal + "% of their pool.");
        }
    }

    /** /summons admin dismiss <player> [all|pets]: the EXISTING
     *  dismiss path on the target, in the routed scope (mount only,
     *  mount + all combat, or cast pets only — exactly the
     *  self-dismiss semantics). The count is reported to the
     *  operator and an INFO audit line names the operator actor —
     *  never a fake player action. */
    private void adminDismiss(CommandSender sender, String targetName,
                              AdminPolicy.DismissScope scope) {
        Player target = onlineByName(targetName);
        if (target == null) {
            sender.sendMessage("Summons admin: no player named '" + targetName
                    + "' is online. Nothing was changed.");
            return;
        }
        String actor = actorOf(sender);
        int n = listener.dismiss(target.getUniqueId(), scope);
        getLogger().info(AdminPolicy.auditLine(actor, target.getName(),
                target.getUniqueId(), n, scope));
        sender.sendMessage(n == 0 ? "Nothing to dismiss for " + target.getName() + "."
                : "Dismissed " + n + " summon(s) for " + target.getName() + ".");
    }
}
