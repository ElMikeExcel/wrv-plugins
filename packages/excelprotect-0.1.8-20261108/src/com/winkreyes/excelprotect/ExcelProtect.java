package com.winkreyes.excelprotect;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * ExcelProtect 0.1.3 — the all-in-one record + recovery system (SPEC v2). One async
 * writer, daily JSONL per stream, byte-exact payloads, rollback + restores + Lost &
 * Found. Every adapter (GriefPrevention / Vault / mcMMO / Magic) is fail-soft.
 * 0.1.2 adds DAMAGE INCIDENTS: coalesced damage-taken records on the damage
 * stream (DamageWatch / DamageIncidents), with EliteMobs attribution.
 * 0.1.3 adds the other two directions: DAMAGE DEALT incidents on the dealt
 * stream (DealtWatch / DealtIncidents — player → mob, elite victims, kill
 * attribution) and HEALING incidents on the healing stream (HealingWatch /
 * HealingIncidents — self vs player-to-player, event + effective amounts).
 * 0.1.4 "Where" adds the LOCATION TIMELINE (LocationWatch — position samples
 * + typed anchors on the locations stream, sampling not move tracking), the
 * playtime + where queries (PlaytimeReport / WhereReport over records that
 * already existed), and inspect player extended to ALL streams in sections.
 * 0.1.5 "Clarity" adds the readability layer the Nov 5 mending-book
 * investigation proved missing: DELIVERY OUTCOMES on acquisitions
 * (item_delivery records), CHAIN LINKING across trade → drop → pickup →
 * container (Chains), the item chain-of-custody query (ItemChainReport),
 * drop PAIRING + UNRECOVERED flags and a LIVE inventory read in inspect,
 * and PLAYER-FIRST TIMELINES (Timelines — a derived per-player
 * projection of the canonical streams, which stay the source of truth).
 * 0.1.8 "Attribution" fixes the combat recorder end-to-end (㉚.10 + §S1–S3):
 * heal cast claims carry target sets and effective/overheal splits, splash
 * potions credit once at the splash, spell DAMAGE reads its spell name
 * through Magic's thrown-potion delivery costume (explicit `delivery` field
 * on every damage record), item_break captures worn-out gear, and the
 * healing report answers spell used / who was healed / by how much, per cast.
 * Excel — Wink Reyes Valley
 */
public final class ExcelProtect extends JavaPlugin implements Listener {
    private Settings cfg;
    private Ledger ledger;
    private CombatCore combat;
    private Blocks blocks;
    private EconomyWatch economy;
    private Recovery recovery;
    private Rollback rollback;
    private Importer importer;
    private McmmoAdapter mcmmo;
    private Watch watch;
    private DamageWatch damageWatch;
    private DealtWatch dealtWatch;
    private HealingWatch healingWatch;
    private LocationWatch locationWatch;
    private Chains chains;
    private Timelines timelines;
    private boolean gpOn, mcmmoOn, weOn;

    /** Admin self-logging (Amendment 3.9): every staff action is itself a record. */
    private void admin(CommandSender s, String action, String detail) {
        Json.Obj o = new Json.Obj().put("type", "admin").put("action", action).put("detail", detail);
        if (s instanceof Player p) o.put("player", p.getName()).put("player_uuid", p.getUniqueId().toString());
        else o.put("player", "console");
        ledger.emit("admin", s instanceof Player p ? p.getLocation() : null, o);
    }
    private final Set<UUID> inspecting = new HashSet<>();

    @Override public void onEnable() {
        saveDefaultConfig();
        cfg = new Settings(getConfig());
        ItemFact.init(this);
        com.winkreyes.excelprotect.api.ExcelProtectAPI.init(this);
        ledger = new Ledger(getDataFolder(), cfg);
        combat = new CombatCore(this, ledger, cfg);
        blocks = new Blocks(ledger, cfg);
        economy = new EconomyWatch(ledger, cfg);
        recovery = new Recovery(ledger, cfg, getDataFolder());
        rollback = new Rollback(ledger, cfg, recovery);
        importer = new Importer(ledger, recovery, getDataFolder());
        // 0.1.5 Clarity: the chain tracker (state only) + the player-first
        // timeline projection (derived; failure-isolated from the ledger).
        chains = new Chains(cfg);
        if (cfg.playerTimelines()) {
            timelines = new Timelines(getDataFolder());
            for (org.bukkit.OfflinePlayer op : Bukkit.getOfflinePlayers())
                timelines.notePlayer(op.getUniqueId(), op.getName());
            timelines.setMarkerSink(o -> ledger.emit("system", null, o));
            ledger.setTimelines(timelines);
        }

        var pm = getServer().getPluginManager();
        // 0.1.3: the dealt/healing watches are constructed and registered BEFORE
        // CombatCore on purpose — their claim PEEKs must run before CombatCore's
        // per-event listeners consume the same Magic cast claims (peek never
        // consumes, so both layers attribute the same event; see CombatCore).
        if (cfg.damageDealt()) {
            dealtWatch = new DealtWatch(this, ledger, cfg, combat);
            pm.registerEvents(dealtWatch, this);
        }
        if (cfg.healingIncidents()) {
            healingWatch = new HealingWatch(this, ledger, cfg, combat);
            pm.registerEvents(healingWatch, this);
        }
        if (dealtWatch != null || healingWatch != null)
            getServer().getScheduler().runTaskTimer(this, () -> {
                if (dealtWatch != null) dealtWatch.tick();
                if (healingWatch != null) healingWatch.tick();
                combat.flushPendingDamage(); // §S1: spell-shaped records awaiting claims expire here
            }, 20, 20);
        pm.registerEvents(combat, this);
        pm.registerEvents(blocks, this);
        Containers containers = new Containers(ledger, cfg);
        containers.setChains(chains);
        pm.registerEvents(containers, this);
        watch = new Watch(this, ledger, cfg, getDataFolder());
        blocks.setWatch(watch);
        containers.setWatch(watch);
        recovery.setWatch(watch);
        pm.registerEvents(new Entities(ledger, cfg, combat), this);
        ItemEvents itemEvents = new ItemEvents(this, ledger, cfg, recovery);
        itemEvents.setChains(chains);
        pm.registerEvents(itemEvents, this);
        pm.registerEvents(new Players(ledger, cfg, combat), this);
        // 0.1.4: location timeline — anchors via events, samples via the timer.
        // The sampler is stateless (reads the live player list each tick), so
        // quits need no cleanup and offline players leave no records.
        if (cfg.locations()) {
            locationWatch = new LocationWatch(ledger, cfg);
            pm.registerEvents(locationWatch, this);
            long period = LocationWatch.periodTicks(cfg.locationsSampleSeconds());
            getServer().getScheduler().runTaskTimer(this, locationWatch::sample, period, period);
        }
        pm.registerEvents(economy, this);
        if (cfg.damageIncidents()) {
            damageWatch = new DamageWatch(this, ledger, cfg);
            pm.registerEvents(damageWatch, this);
            getServer().getScheduler().runTaskTimer(this, damageWatch::tick, 20, 20);
        }
        Adapters adapters = new Adapters(this, ledger, cfg, chains);
        pm.registerEvents(adapters, this);
        adapters.hookJobs(this);
        pm.registerEvents(this, this);

        // Fail-soft adapters
        Plugin gp = pm.getPlugin("GriefPrevention");
        if (gp != null && gp.isEnabled()) {
            try {
                pm.registerEvents(new GpAdapter(ledger, cfg), this);
                ledger.setClaimLookup(GpAdapter::ownerAt);
                gpOn = true;
            } catch (Throwable t) { getLogger().info("[ExcelProtect] GriefPrevention adapter failed — running without claim stamping"); }
        }
        economy.hook();
        combat.hookMagic();
        Plugin mm = pm.getPlugin("mcMMO");
        if (mm != null && mm.isEnabled()) {
            try { mcmmo = new McmmoAdapter(ledger, cfg); pm.registerEvents(mcmmo, this); mcmmoOn = true; }
            catch (Throwable t) { getLogger().info("[ExcelProtect] mcMMO adapter failed — progression off (fail-soft)"); }
        }
        // WorldEdit logging (0.1.6): WE edits bypass Bukkit block events, so
        // without the hook they were never recorded. Fail-soft like every
        // other adapter; the hook class loads only when WE is present.
        Plugin we = pm.getPlugin("WorldEdit");
        if (we != null && we.isEnabled() && cfg.worldEdit()) {
            try { new WorldEditHook(ledger).register(); weOn = true; }
            catch (Throwable t) { getLogger().info("[ExcelProtect] WorldEdit hook failed — running without WorldEdit logging (fail-soft)"); }
        }

        long flushTicks = Math.max(20, cfg.flushMs() / 50);
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            ledger.flushAll();
            try { economy.poll(); } catch (Throwable ignored) { }
            try { ledger.shedMarkers(); } catch (Throwable ignored) { }
            ledger.tickRollover();
        }, flushTicks, flushTicks);
        getServer().getScheduler().runTaskTimer(this, combat::tick, 200, 200);
        if (mcmmoOn) getServer().getScheduler().runTaskTimer(this,
                () -> mcmmo.snapshotAll("hourly"), 72_000, 72_000);
        getServer().getScheduler().runTaskAsynchronously(this, ledger::housekeeping);

        getLogger().info("[ExcelProtect] damage incidents: " + (damageWatch == null
                ? "OFF (config log.combat.damage-incidents=false)"
                : "ON gap=" + cfg.damageIncidentGapMs() + "ms near-death<="
                  + Math.round(cfg.nearDeathFraction() * 100) + "% elitemobs=" + damageWatch.eliteState()));
        getLogger().info("[ExcelProtect] damage dealt: " + (dealtWatch == null
                ? "OFF (config log.combat.damage-dealt=false)"
                : "ON gap=" + cfg.damageIncidentGapMs() + "ms elitemobs=" + dealtWatch.eliteState()));
        getLogger().info("[ExcelProtect] healing incidents: " + (healingWatch == null
                ? "OFF (config log.combat.healing-incidents=false)"
                : "ON gap=" + cfg.damageIncidentGapMs() + "ms"));
        getLogger().info("[ExcelProtect] locations: " + (locationWatch == null
                ? "OFF (config log.locations=false)"
                : "ON sample=" + cfg.locationsSampleSeconds() + "s anchors=join,quit,teleport,world,respawn,gamemode"));
        getLogger().info("[ExcelProtect] clarity: ON delivery=next-tick chains=expiry "
                + cfg.chainExpirySeconds() + "s ground-radius=" + cfg.clarityGroundRadius());
        getLogger().info("[ExcelProtect] player timelines: " + (timelines == null
                ? "OFF (config log.player-timelines=false)"
                : "ON (projection of the streams; streams stay canonical)"));
        getLogger().info("[ExcelProtect] worldedit logging: " + (weOn
                ? "ON (WorldEdit edits recorded into the blocks stream, actor-attributed)"
                : "OFF (WorldEdit absent, disabled, or log.blocks.worldedit=false)"));
        getLogger().info("[ExcelProtect] recovery: ON rollback=blocks+containers+undo(incl. containers) restore-entity restore-inventory (0.1.6)");
        getLogger().info("[ExcelProtect] importer: deathrecord honors restored marks + lost&found state migration (0.1.7)");
        getLogger().info("[ExcelProtect] kill parity: elite/weapon/origin-class/damage-shares on entity_death (0.1.7)");
        getLogger().info("[ExcelProtect] READY version=0.1.8 dir=" + getDataFolder()
                + " gp=" + (gpOn ? "on" : "off") + " vault=" + (economy.on() ? "on" : "off")
                + " mcmmo=" + (mcmmoOn ? "on" : "off") + " we=" + (weOn ? "on" : "off")
                + " retention-days=" + cfg.retentionDays());
    }

    @Override public void onDisable() {
        if (damageWatch != null) damageWatch.closeAll("shutdown");
        if (dealtWatch != null) dealtWatch.closeAll("shutdown");
        if (healingWatch != null) healingWatch.closeAll("shutdown");
        if (ledger != null) ledger.flushAll();
    }

    // mcMMO snapshots on join/quit (samples stream)
    @EventHandler public void onJoinSnap(PlayerJoinEvent e) {
        if (mcmmo != null) mcmmo.snapshot(e.getPlayer(), "join");
        if (timelines != null) timelines.notePlayer(e.getPlayer().getUniqueId(), e.getPlayer().getName());
    }
    @EventHandler public void onQuitSnap(PlayerQuitEvent e) { if (mcmmo != null) mcmmo.snapshot(e.getPlayer(), "quit"); }

    // inspect: click a block, get its latest event in chat (one block = one short answer)
    @EventHandler public void onInspectClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_BLOCK && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!inspecting.contains(e.getPlayer().getUniqueId()) || e.getClickedBlock() == null) return;
        String s = blocks.inspect(e.getClickedBlock().getLocation());
        e.getPlayer().sendMessage("§e[ExcelProtect] §f" + (s == null ? "no recorded event for that block yet" : s));
    }

    // ---------------------------------------------------------------- commands
    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (cmd.getName()) {
            case "excelprotect" -> { return protect(sender, args); }
            case "lostfound" -> { return lostfound(sender, args); }
            case "deathrecord" -> { return deathrecord(sender, args); }
            default -> { return false; }
        }
    }

    private boolean protect(CommandSender s, String[] args) {
        if (args.length == 0) { s.sendMessage("§e/excelprotect §fexport|inspect [player <name>|at <world> <x> <y> <z>]|damage <player> [hours]|dealt <player> [hours]|healing <player> [hours]|where <player> [hours]|playtime <player> [days]|item <player> <item> [hours]|timelines rebuild [day]|rollback|restore-stand|restore-entity|restore-inventory|deaths|import|status"); return true; }
        switch (args[0]) {
            case "status" -> {
                s.sendMessage("§e[ExcelProtect] §fversion=0.1.8 gp=" + (gpOn ? "on" : "off")
                        + " vault=" + (economy.on() ? "on" : "off") + " mcmmo=" + (mcmmoOn ? "on" : "off")
                        + " we=" + (weOn ? "on" : "off")
                        + " dropped-total=" + ledger.totalDropped());
                for (String line : ledger.flushAll().split("\n"))
                    if (!line.isBlank()) s.sendMessage("§7  " + line);
                return true;
            }
            case "inspect" -> {
                // 0.1.1 query forms route FIRST, for any sender — the console can always
                // help a player (Mike's standing rule, Nov 3, 2026). Bare `inspect` keeps
                // its 0.1.0 meaning: the click-to-inspect toggle, which needs a player.
                if (args.length >= 2 && args[1].equals("player")) return inspectPlayerCmd(s, args);
                if (args.length >= 2 && args[1].equals("at")) return inspectAtCmd(s, args);
                if (!(s instanceof Player p)) {
                    s.sendMessage("§e[ExcelProtect] §fconsole inspect: /excelprotect inspect player <name> [n] | inspect at <world> <x> <y> <z> [n] (bare inspect toggles click mode — players only)");
                    return true;
                }
                if (inspecting.remove(p.getUniqueId())) { s.sendMessage("§e[ExcelProtect] §finspect OFF"); admin(s, "inspect", "off"); }
                else { inspecting.add(p.getUniqueId()); s.sendMessage("§e[ExcelProtect] §finspect ON — click any block"); admin(s, "inspect", "on"); }
                return true;
            }
            case "export" -> {
                String[] tokens = java.util.Arrays.copyOfRange(args, 1, args.length);
                Exporter.Query q = Exporter.Query.parse(tokens);
                if (q.error != null) {
                    s.sendMessage("§c[ExcelProtect] " + q.error + " — export aborted. "
                            + "Times look like 2026-11-06T17:41:56 (or a bare date).");
                    admin(s, "export_error", q.error);
                    return true;
                }
                if (s instanceof Player p && q.radius >= 0 && !hasAt(tokens)) {
                    q.atX = p.getLocation().getX(); q.atY = p.getLocation().getY(); q.atZ = p.getLocation().getZ();
                    if (q.world == null) q.world = p.getWorld().getName();
                }
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    ledger.flushAll();
                    List<String> lines = Exporter.scan(ledger, q);
                    try {
                        File dir = new File(getDataFolder(), "exports");
                        dir.mkdirs();
                        String name = "export-" + Ledger.isoOf(System.currentTimeMillis())
                                .replaceAll("[^0-9A-Za-z]+", "") + ".jsonl";
                        File out = new File(dir, name);
                        List<String> all = new ArrayList<>();
                        all.add(new Json.Obj().put("type", "export_meta")
                                .put("generated", Ledger.isoOf(System.currentTimeMillis()))
                                .put("count", lines.size()).render());
                        all.addAll(lines);
                        Files.write(out.toPath(), all, StandardCharsets.UTF_8);
                        String msg = "[ExcelProtect] export: " + lines.size()
                                + " events -> plugins/ExcelProtect/exports/" + name;
                        getLogger().info(msg);
                        admin(s, "export", lines.size() + " events -> " + name
                                + " filters=" + String.join(" ", tokens));
                        Bukkit.getScheduler().runTask(this, () -> s.sendMessage("§e" + msg));
                    } catch (Exception e) {
                        Bukkit.getScheduler().runTask(this, () -> s.sendMessage("§c[ExcelProtect] export failed: " + e));
                    }
                });
                return true;
            }
            case "rollback" -> {
                java.util.List<String> tokList = new java.util.ArrayList<>(java.util.List.of(args).subList(1, args.length));
                boolean preview = tokList.remove("preview");
                String[] tokens = tokList.toArray(new String[0]);
                Exporter.Query q = Exporter.Query.parse(tokens);
                if (q.error != null) {
                    s.sendMessage("§c[ExcelProtect] " + q.error + " — rollback aborted (nothing was changed). "
                            + "Times look like 2026-11-06T17:41:56 (or a bare date).");
                    admin(s, "rollback_error", q.error);
                    return true;
                }
                long target = q.toMs >= 0 ? q.toMs : System.currentTimeMillis();
                Player actor = s instanceof Player p ? p : null;
                String opId = "op-" + System.currentTimeMillis();
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    ledger.flushAll();
                    Exporter.Query qb = Exporter.Query.parse(tokens); qb.stream = "blocks"; qb.types = "block_change";
                    List<String> blockLines = Exporter.scan(ledger, qb);
                    Exporter.Query qc = Exporter.Query.parse(tokens); qc.stream = "containers"; qc.types = "container_transaction";
                    List<String> contLines = Exporter.scan(ledger, qc);
                    java.util.Map<String, List<String>> byPos = new java.util.HashMap<>();
                    for (String l : contLines) {
                        String key = Json.strField(l, "world") + "@" + (long) Json.numField(l, "x", 0)
                                + "," + (long) Json.numField(l, "y", 0) + "," + (long) Json.numField(l, "z", 0);
                        byPos.computeIfAbsent(key, k -> new ArrayList<>()).add(l);
                    }
                    Bukkit.getScheduler().runTask(this, () -> {
                        java.util.Map<String, String> chosen = Rollback.chooseBlocks(blockLines, target);
                        if (preview) {
                            admin(s, "rollback_preview", chosen.size() + " blocks, " + byPos.size()
                                    + " containers, target=" + Ledger.isoOf(target));
                            s.sendMessage("§e[ExcelProtect] §fPREVIEW: would restore " + chosen.size()
                                    + " block position(s) and " + byPos.size() + " container(s) to "
                                    + Ledger.isoOf(target) + " — rerun without 'preview' to commit");
                            return;
                        }
                        // capture the pre-rollback world state as this op's undo data
                        // (0.1.6: containers are captured too — pre-0.1.6 undo covered blocks only)
                        List<String> undoLines = rollback.captureUndo(chosen);
                        List<String> undoContainers = rollback.captureUndoContainers(byPos);
                        undoLines.addAll(undoContainers);
                        try {
                            File uf = new File(getDataFolder(), "undo-" + opId + ".jsonl");
                            Files.write(uf.toPath(), undoLines, StandardCharsets.UTF_8);
                        } catch (Exception ignored) { }
                        int n = rollback.rollbackBlocks(blockLines, target, actor);
                        int c = 0;
                        for (List<String> group : byPos.values()) if (rollback.rollbackContainer(group, target, actor)) c++;
                        ledger.emit("system", actor == null ? null : actor.getLocation(),
                                new Json.Obj().put("type", "rollback_op").put("op", opId)
                                        .put("blocks", n).put("containers", c)
                                        .put("undo_containers", undoContainers.size())
                                        .put("undo_file", "undo-" + opId + ".jsonl"));
                        admin(s, "rollback", opId + ": " + n + " blocks, " + c + " containers");
                        s.sendMessage("§e[ExcelProtect] §frollback done: " + n + " blocks, " + c
                                + " containers (op " + opId + " — undo with /excelprotect undo " + opId + ")");
                    });
                });
                return true;
            }
            case "undo" -> {
                if (args.length < 2) { s.sendMessage("§eUsage: §f/excelprotect undo <op-id>"); return true; }
                String opId = args[1];
                Player actor = s instanceof Player p ? p : null;
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    File uf = new File(getDataFolder(), "undo-" + opId + ".jsonl");
                    if (!uf.exists()) {
                        Bukkit.getScheduler().runTask(this, () -> s.sendMessage("§c[ExcelProtect] no undo data for " + opId));
                        return;
                    }
                    List<String> lines;
                    try { lines = Files.readAllLines(uf.toPath(), StandardCharsets.UTF_8); }
                    catch (Exception e) { lines = List.of(); }
                    List<String> undoLines = lines;
                    Bukkit.getScheduler().runTask(this, () -> {
                        // 0.1.6: undo files may also carry container_transaction
                        // lines (the op's pre-rollback container contents) —
                        // restore those too. Pre-0.1.6 files hold blocks only.
                        List<String> blockLines = new ArrayList<>();
                        java.util.Map<String, List<String>> undoCont = new java.util.HashMap<>();
                        for (String l : undoLines) {
                            if ("container_transaction".equals(Json.strField(l, "type"))) {
                                String key = Json.strField(l, "world") + "@" + (long) Json.numField(l, "x", 0)
                                        + "," + (long) Json.numField(l, "y", 0) + "," + (long) Json.numField(l, "z", 0);
                                undoCont.computeIfAbsent(key, k -> new ArrayList<>()).add(l);
                            } else blockLines.add(l);
                        }
                        int n = rollback.rollbackBlocks(blockLines, Long.MAX_VALUE, actor);
                        int c = 0;
                        for (List<String> group : undoCont.values())
                            if (rollback.rollbackContainer(group, Long.MAX_VALUE, actor)) c++;
                        uf.renameTo(new File(getDataFolder(), "undo-" + opId + ".jsonl.done"));
                        admin(s, "rollback_undo", opId + ": " + n + " positions, " + c + " containers");
                        s.sendMessage("§e[ExcelProtect] §fundid " + opId + ": " + n + " position(s)"
                                + (c > 0 ? ", " + c + " container(s)" : "") + " restored");
                    });
                });
                return true;
            }
            case "restore-stand" -> {
                if (!cfg.entityRestore()) { s.sendMessage("§c[ExcelProtect] entity restore disabled in config"); return true; }
                String match = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "armor_stand";
                Player actor = s instanceof Player p ? p : null;
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    ledger.flushAll();
                    Exporter.Query q = Exporter.Query.parse(new String[]{"stream=entities", "type=entity_death", "match=" + match});
                    List<String> lines = Exporter.scan(ledger, q);
                    Bukkit.getScheduler().runTask(this, () -> {
                        if (lines.isEmpty()) { s.sendMessage("§c[ExcelProtect] no matching entity death"); return; }
                        admin(s, "restore_stand", "match=" + match);
                        s.sendMessage("§e[ExcelProtect] §f" + rollback.restoreStand(lines.get(lines.size() - 1), actor));
                    });
                });
                return true;
            }
            case "restore-entity" -> { return restoreEntityCmd(s, args); }
            case "restore-inventory" -> { return restoreInventoryCmd(s, args); }
            case "damage" -> { return damageCmd(s, args); }
            case "dealt" -> { return dealtCmd(s, args); }
            case "healing" -> { return healingCmd(s, args); }
            case "where" -> { return whereCmd(s, args); }
            case "playtime" -> { return playtimeCmd(s, args); }
            case "item" -> { return itemCmd(s, args); }
            case "timelines" -> { return timelinesCmd(s, args); }
            case "deaths" -> { return deathsCmd(s, args); }
            case "import" -> {
                if (args.length < 3 || !args[1].equals("deathrecord")) {
                    s.sendMessage("§eUsage: §f/excelprotect import deathrecord <folder-of-ExcelDeathRecord-data>");
                    return true;
                }
                File root = new File(args[2]);
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    Importer.Result r = importer.importDeathRecord(root);
                    admin(s, "import", "deathrecord " + root + " -> " + r.deaths() + " deaths, " + r.burns() + " burns"
                            + ", " + r.restoredBurns() + " restored burns, " + r.restoredItems() + " restored death items"
                            + ", " + r.lfState() + " lf-state files");
                    Bukkit.getScheduler().runTask(this, () -> s.sendMessage("§e[ExcelProtect] §fimport: "
                            + r.deaths() + " deaths, " + r.burns() + " burns, " + r.skipped() + " skipped, " + r.errors() + " errors"
                            + " (restored-marked: " + r.restoredBurns() + " burns, " + r.restoredItems() + " death items; lf-state: "
                            + r.lfState() + " player files)"));
                });
                return true;
            }
            default -> { s.sendMessage("§e/excelprotect §fexport|inspect [player <name>|at <world> <x> <y> <z>]|damage <player> [hours]|dealt <player> [hours]|healing <player> [hours]|where <player> [hours]|playtime <player> [days]|item <player> <item> [hours]|timelines rebuild [day]|rollback|restore-stand|restore-entity|restore-inventory|deaths|import|status"); return true; }
        }
    }

    private boolean hasAt(String[] tokens) {
        for (String t : tokens) if (t.startsWith("at=")) return true;
        return false;
    }

    // ------------------------------------------------- inspect query forms (0.1.1; all-stream sections 0.1.4)
    /**
     * /excelprotect inspect player <name> [n] — that player's recorded activity
     * across ALL streams, one section per stream (0.1.4; 0.1.1 scanned only
     * containers/items/deaths/blocks, which made "what has X been doing?"
     * unanswerable for fights, movement, commands and money — coverage map,
     * query-surface gap). [n] now caps EACH section (default 10, 1–25);
     * every capped section ends with its "…and N more" tail. Any sender.
     */
    private boolean inspectPlayerCmd(CommandSender s, String[] args) {
        if (args.length < 3) { s.sendMessage("§eUsage: §f/excelprotect inspect player <name> [n]"); return true; }
        String who = args[2];
        int limit = InspectReport.parseLimit(args.length >= 4 ? args[3] : null);
        // 0.1.5 LIVE read (SPEC §5): an ONLINE player's inventory is read
        // in real time HERE, on the main thread, before the async scan —
        // "how many does she have right now" is answerable mid-session,
        // not only at her next join/leave snapshot.
        Player online = Bukkit.getPlayerExact(who);
        LiveInventory.Summary live = null;
        if (online != null) {
            List<ItemFact> inv = new ArrayList<>(), ender = new ArrayList<>();
            for (ItemStack st : online.getInventory().getContents()) {
                ItemFact f = ItemFact.of(st, false);
                if (f != null) inv.add(f);
            }
            for (ItemStack st : online.getEnderChest().getContents()) {
                ItemFact f = ItemFact.of(st, false);
                if (f != null) ender.add(f);
            }
            live = LiveInventory.summarize(inv, ender);
        }
        LiveInventory.Summary liveF = live;
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            ledger.flushAll();
            java.util.Map<String, List<String>> byStream = collectSections(who);
            List<InspectReport.Section> sections = InspectReport.buildSections(
                    byStream, who, limit, System.currentTimeMillis());
            // latest inventory snapshot (timestamp + reason) — the LIVE
            // contrast line for online players, the as-of line for offline
            String snapInfo = null;
            long bestTs = -1;
            for (String l : byStream.getOrDefault("snapshots", List.of())) {
                if (!"inventory_snapshot".equals(Json.strField(l, "type"))) continue;
                long ts = (long) Json.numField(l, "ts_ms", 0);
                if (ts > bestTs) {
                    bestTs = ts;
                    String reason = Json.strField(l, "reason");
                    snapInfo = Ledger.isoOf(ts).substring(0, 19).replace('T', ' ')
                            + (reason == null ? "" : " (" + reason + ")");
                }
            }
            String snapF = snapInfo;
            int total = 0;
            for (InspectReport.Section sec : sections) total += sec.total();
            int totalF = total;
            admin(s, "inspect_player", who + " (" + totalF + " events, " + sections.size() + " streams)");
            Bukkit.getScheduler().runTask(this, () -> {
                if (sections.isEmpty() && liveF == null) { s.sendMessage("§c[ExcelProtect] no recorded activity for " + who); return; }
                s.sendMessage("§e[ExcelProtect] §f" + who + " — recorded activity by stream ("
                        + totalF + " event(s)):");
                if (liveF != null) {
                    s.sendMessage("§e[ExcelProtect] §f[LIVE] " + who + " is ONLINE — inventory right now: "
                            + liveF.invStacks() + " stack(s) (" + liveF.invItems() + " item(s)), ender chest "
                            + liveF.enderStacks() + " stack(s) (" + liveF.enderItems() + " item(s)):");
                    int shown = 0;
                    for (String l : liveF.lines()) {
                        if (shown++ >= 15) {
                            s.sendMessage("§7  …and " + (liveF.lines().size() - 15)
                                    + " more signature(s) — full contents via export stream=snapshots");
                            break;
                        }
                        s.sendMessage("§7  " + l);
                    }
                    s.sendMessage("§7  (last snapshot: " + (snapF == null ? "none recorded" : snapF)
                            + " — LIVE supersedes it)");
                } else {
                    s.sendMessage("§7  " + who + " is offline — counts are as of the last snapshot ("
                            + (snapF == null ? "none recorded" : snapF) + ")");
                }
                for (InspectReport.Section sec : sections) {
                    if (sec.stream().equals("locations") && sec.total() == 0) {
                        s.sendMessage("§e[ExcelProtect] §f[" + sec.stream() + "] position samples only:");
                    } else {
                        s.sendMessage("§e[ExcelProtect] §f[" + sec.stream() + "] latest " + sec.shown()
                                + " of " + sec.total() + ":");
                    }
                    for (String line : sec.lines()) s.sendMessage("§7  " + line);
                    if (sec.more() > 0)
                        s.sendMessage("§7  …and " + sec.more() + " more — full detail via /excelprotect export stream="
                                + sec.stream() + " player=" + who);
                }
            });
        });
        return true;
    }

    // ------------------------------------------------- 0.1.5 player-first collection
    /** The LAST occurrence of a string field — projected timeline copies carry
     *  their origin in a trailing "stream" field, and a canonical line can
     *  legitimately contain an earlier one (overload_shed names its stream). */
    private static String lastStrField(String line, String key) {
        int i = line.lastIndexOf("\"" + key + "\"");
        if (i < 0) return null;
        int colon = line.indexOf(':', i + key.length() + 2);
        if (colon < 0) return null;
        int p = colon + 1;
        while (p < line.length() && line.charAt(p) == ' ') p++;
        if (p >= line.length() || line.charAt(p) != '"') return null;
        int q = line.indexOf('"', p + 1);
        return q < 0 ? null : line.substring(p + 1, q);
    }

    /**
     * Per-stream lines for one player, player-first (SPEC §7): a day with
     * a timeline file for the player is read from that ONE file; a day
     * without one (pre-0.1.5 history, toggle-off gaps) is read from the
     * streams. No day is read from both, so the merge cannot
     * double-count. With no timeline files at all, this is exactly the
     * 0.1.4 per-stream scan.
     */
    private java.util.Map<String, List<String>> collectSections(String who) {
        Timelines tl = ledger.timelines();
        java.util.Map<String, File> timelineDays = new java.util.HashMap<>();
        if (tl != null)
            for (File f : tl.filesFor(Timelines.sanitize(who)))
                timelineDays.put(Timelines.dayOfFile(f.getName()), f);
        java.util.Map<String, List<String>> byStream = new java.util.LinkedHashMap<>();
        for (String stream : Exporter.STREAMS) byStream.put(stream, new ArrayList<>());
        if (timelineDays.isEmpty()) {
            for (String stream : Exporter.STREAMS) {
                Exporter.Query q = Exporter.Query.parse(new String[]{"player=" + who});
                q.stream = stream;
                byStream.put(stream, Exporter.scan(ledger, q));
            }
            return byStream;
        }
        java.util.Set<String> days = new java.util.TreeSet<>(timelineDays.keySet());
        for (String stream : Exporter.STREAMS)
            for (File f : ledger.streamFiles(stream)) days.add(Timelines.dayOfFile(f.getName()));
        for (String day : days) {
            File tf = timelineDays.get(day);
            if (tf != null) {
                try (var r = Ledger.open(tf)) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        String st = lastStrField(line, "stream");
                        if (st != null && byStream.containsKey(st)) byStream.get(st).add(line);
                    }
                } catch (Exception ignored) { }
            } else {
                long from = java.time.LocalDate.parse(day).atStartOfDay(Ledger.ZONE).toInstant().toEpochMilli();
                long to = from + 86_400_000L - 1;
                for (String stream : Exporter.STREAMS) {
                    Exporter.Query q = Exporter.Query.parse(new String[]{"player=" + who});
                    q.stream = stream; q.fromMs = from; q.toMs = to;
                    byStream.get(stream).addAll(Exporter.scan(ledger, q));
                }
            }
        }
        return byStream;
    }

    private record CollectedLines(List<String> lines, String source) {}

    /** Chain-relevant lines (economy/items/containers/deaths) for one
     *  player over a window, with the same per-day player-first rule. */
    private CollectedLines collectChainLines(String who, long fromMs, long toMs) {
        List<String> wanted = List.of("economy", "items", "containers", "deaths");
        Timelines tl = ledger.timelines();
        List<String> out = new ArrayList<>();
        if (tl == null || tl.filesFor(Timelines.sanitize(who)).length == 0) {
            for (String stream : wanted) {
                Exporter.Query q = Exporter.Query.parse(new String[]{"player=" + who});
                q.stream = stream; q.fromMs = fromMs; q.toMs = toMs;
                out.addAll(Exporter.scan(ledger, q));
            }
            return new CollectedLines(out, "streams");
        }
        String key = Timelines.sanitize(who);
        boolean usedT = false, usedS = false;
        java.time.LocalDate d0 = java.time.Instant.ofEpochMilli(fromMs).atZone(Ledger.ZONE).toLocalDate();
        java.time.LocalDate d1 = java.time.Instant.ofEpochMilli(toMs).atZone(Ledger.ZONE).toLocalDate();
        for (java.time.LocalDate d = d0; !d.isAfter(d1); d = d.plusDays(1)) {
            String day = d.toString();
            File tf = tl.fileFor(key, day);
            if (tf != null) {
                usedT = true;
                try (var r = Ledger.open(tf)) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        String st = lastStrField(line, "stream");
                        if (st != null && wanted.contains(st)) out.add(line);
                    }
                } catch (Exception ignored) { }
            } else {
                usedS = true;
                long from = Math.max(fromMs, d.atStartOfDay(Ledger.ZONE).toInstant().toEpochMilli());
                long to = Math.min(toMs, d.plusDays(1).atStartOfDay(Ledger.ZONE).toInstant().toEpochMilli() - 1);
                for (String stream : wanted) {
                    Exporter.Query q = Exporter.Query.parse(new String[]{"player=" + who});
                    q.stream = stream; q.fromMs = from; q.toMs = to;
                    out.addAll(Exporter.scan(ledger, q));
                }
            }
        }
        return new CollectedLines(out, usedT && usedS ? "timeline+streams" : usedT ? "timeline" : "streams");
    }

    // ------------------------------------------------- item chain query (0.1.5)
    /**
     * /excelprotect item <player> <item> [hours] — the chain of custody
     * for one item in one player's hands (SPEC §3): acquisitions with
     * their delivery outcomes, drops, pickups, container moves, losses
     * and recoveries, oldest first. The Nov 5 mending-book question —
     * "she bought 2, where are they" — is this command's one line.
     * Any sender; self-logged like the other queries.
     */
    private boolean itemCmd(CommandSender s, String[] args) {
        if (args.length < 3) { s.sendMessage("§eUsage: §f/excelprotect item <player> <item> [last-hours]"); return true; }
        String who = args[1];
        String item = args[2];
        double hours = DamageReport.parseHours(args.length >= 4 ? args[3] : null);
        long toMs = System.currentTimeMillis();
        long fromMs = toMs - (long) (hours * 3_600_000);
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            ledger.flushAll();
            CollectedLines c = collectChainLines(who, fromMs, toMs);
            ItemChainReport.Report r = ItemChainReport.build(c.lines(), who, item, fromMs, toMs);
            admin(s, "item_query", who + " / " + item + " last " + DamageReport.fmtHours(hours)
                    + " (" + r.total() + " events, source " + c.source() + ")");
            Bukkit.getScheduler().runTask(this, () -> {
                if (r.total() == 0) {
                    s.sendMessage("§c[ExcelProtect] no records for " + who + " / " + item
                            + " in the last " + DamageReport.fmtHours(hours));
                    return;
                }
                s.sendMessage("§e[ExcelProtect] §f" + who + " — chain of custody for " + item
                        + ", last " + DamageReport.fmtHours(hours) + ": " + r.total() + " event(s), "
                        + r.chains() + " chain(s) (source: " + c.source() + ")");
                for (String line : r.lines()) s.sendMessage("§7  " + line);
            });
        });
        return true;
    }

    // ------------------------------------------------- timelines admin (0.1.5)
    /**
     * /excelprotect timelines rebuild [yyyy-MM-dd] — regenerate one
     * day's player-timeline files from that day's canonical streams
     * (SPEC §7: the projection is derived and rebuildable). Default
     * day: yesterday (Chicago). Any sender; self-logged; async.
     */
    private boolean timelinesCmd(CommandSender s, String[] args) {
        Timelines tl = ledger.timelines();
        if (tl == null) { s.sendMessage("§c[ExcelProtect] player timelines are OFF in config (log.player-timelines)"); return true; }
        if (args.length < 2 || !args[1].equals("rebuild")) {
            s.sendMessage("§eUsage: §f/excelprotect timelines rebuild [yyyy-MM-dd]");
            return true;
        }
        String day = args.length >= 3 ? args[2]
                : java.time.LocalDate.now(Ledger.ZONE).minusDays(1).toString();
        try { java.time.LocalDate.parse(day); }
        catch (Exception e) { s.sendMessage("§eUsage: §f/excelprotect timelines rebuild [yyyy-MM-dd]"); return true; }
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            ledger.flushAll();
            List<String[]> pairs = new ArrayList<>();
            for (String stream : Exporter.STREAMS)
                for (File f : ledger.streamFiles(stream)) {
                    if (!f.getName().contains("-" + day + ".jsonl")) continue;
                    try (var r = Ledger.open(f)) {
                        String line;
                        while ((line = r.readLine()) != null) pairs.add(new String[]{stream, line});
                    } catch (Exception ignored) { }
                }
            try {
                int n = tl.rebuildDay(day, pairs);
                admin(s, "timelines_rebuild", day + " -> " + n + " copies from " + pairs.size() + " canonical lines");
                Bukkit.getScheduler().runTask(this, () -> s.sendMessage("§e[ExcelProtect] §ftimelines rebuilt for "
                        + day + ": " + n + " record copies from " + pairs.size() + " canonical line(s)"));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(this, () -> s.sendMessage("§c[ExcelProtect] timelines rebuild failed: " + e));
            }
        });
        return true;
    }

    /** /excelprotect inspect at <world> <x> <y> <z> [n] — recorded history at one block/container position. Any sender. */
    private boolean inspectAtCmd(CommandSender s, String[] args) {
        if (args.length < 6) { s.sendMessage("§eUsage: §f/excelprotect inspect at <world> <x> <y> <z> [n]"); return true; }
        String world = args[2];
        long x, y, z;
        try {
            x = Long.parseLong(args[3]); y = Long.parseLong(args[4]); z = Long.parseLong(args[5]);
        } catch (NumberFormatException e) {
            s.sendMessage("§eUsage: §f/excelprotect inspect at <world> <x> <y> <z> [n]"); return true;
        }
        int limit = InspectReport.parseLimit(args.length >= 7 ? args[6] : null);
        // The click-inspect answer for this exact block when the world is loaded —
        // the same one-line summary a clicking player would see, as the first line.
        String latest = null;
        org.bukkit.World w = Bukkit.getWorld(world);
        if (w != null) latest = blocks.inspect(new org.bukkit.Location(w, x, y, z));
        String latestF = latest;
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            ledger.flushAll();
            List<String> scanned = new ArrayList<>();
            for (String stream : new String[]{"blocks", "containers"}) {
                Exporter.Query q = Exporter.Query.parse(new String[]{
                        "world=" + world, "at=" + x + "," + y + "," + z, "radius=0"});
                q.stream = stream;
                scanned.addAll(Exporter.scan(ledger, q));
            }
            InspectReport.Report r = InspectReport.build(scanned, limit);
            admin(s, "inspect_at", world + " " + x + "," + y + "," + z + " (" + r.total() + " events)");
            Bukkit.getScheduler().runTask(this, () -> {
                if (r.total() == 0 && latestF == null) {
                    s.sendMessage("§c[ExcelProtect] no recorded events at " + world + " " + x + "," + y + "," + z);
                    return;
                }
                s.sendMessage("§e[ExcelProtect] §fhistory at " + world + " " + x + "," + y + "," + z + ":");
                if (latestF != null) s.sendMessage("§7  latest (click-inspect): " + latestF);
                for (String line : r.lines()) s.sendMessage("§7  " + line);
                if (r.total() > r.lines().size())
                    s.sendMessage("§7  … " + (r.total() - r.lines().size())
                            + " earlier event(s) — full history via /excelprotect export");
            });
        });
        return true;
    }

    // ------------------------------------------------- damage query form (0.1.2)
    /** /excelprotect damage <player> [last-hours] — that player's recent damage incidents (damage stream). Any sender. */
    private boolean damageCmd(CommandSender s, String[] args) {
        if (args.length < 2) { s.sendMessage("§eUsage: §f/excelprotect damage <player> [last-hours]"); return true; }
        if (damageWatch == null) { s.sendMessage("§c[ExcelProtect] damage incidents are OFF in config (log.combat.damage-incidents)"); return true; }
        String who = args[1];
        double hours = DamageReport.parseHours(args.length >= 3 ? args[2] : null);
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            ledger.flushAll();
            Exporter.Query q = Exporter.Query.parse(new String[]{
                    "stream=damage", "player=" + who, "hours=" + hours});
            List<String> lines = Exporter.scan(ledger, q);
            DamageReport.Report r = DamageReport.build(lines, 10);
            admin(s, "damage_query", who + " last " + DamageReport.fmtHours(hours) + " (" + r.total() + " incidents)");
            Bukkit.getScheduler().runTask(this, () -> {
                if (r.total() == 0) {
                    s.sendMessage("§c[ExcelProtect] no damage incidents recorded for " + who
                            + " in the last " + DamageReport.fmtHours(hours));
                    return;
                }
                s.sendMessage("§e[ExcelProtect] §f" + who + " — latest " + r.lines().size()
                        + " of " + r.total() + " damage incident(s), last " + DamageReport.fmtHours(hours) + ":");
                for (String line : r.lines()) s.sendMessage("§7  " + line);
            });
        });
        return true;
    }

    // ------------------------------------------------- dealt + healing query forms (0.1.3)
    /** /excelprotect dealt <player> [last-hours] — damage that player DEALT to mobs (dealt stream): totals, elite share, per-victim breakdown, latest incidents. Any sender. */
    private boolean dealtCmd(CommandSender s, String[] args) {
        if (args.length < 2) { s.sendMessage("§eUsage: §f/excelprotect dealt <player> [last-hours]"); return true; }
        if (dealtWatch == null) { s.sendMessage("§c[ExcelProtect] damage dealt is OFF in config (log.combat.damage-dealt)"); return true; }
        String who = args[1];
        double hours = DamageReport.parseHours(args.length >= 3 ? args[2] : null);
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            ledger.flushAll();
            Exporter.Query q = Exporter.Query.parse(new String[]{
                    "stream=dealt", "player=" + who, "hours=" + hours});
            List<String> lines = Exporter.scan(ledger, q);
            DealtReport.Report r = DealtReport.build(lines, 10);
            admin(s, "dealt_query", who + " last " + DamageReport.fmtHours(hours)
                    + " (" + r.total() + " incidents, " + r.totalDamage() + " dmg)");
            Bukkit.getScheduler().runTask(this, () -> {
                if (r.total() == 0) {
                    s.sendMessage("§c[ExcelProtect] no damage dealt recorded for " + who
                            + " in the last " + DamageReport.fmtHours(hours));
                    return;
                }
                s.sendMessage("§e[ExcelProtect] §f" + who + " — damage dealt, last "
                        + DamageReport.fmtHours(hours) + ": §f" + Json.num(r.totalDamage())
                        + " total (" + Json.num(r.eliteDamage()) + " to elites), "
                        + r.total() + " incident(s)");
                for (String line : r.byVictim()) s.sendMessage("§7  " + line);
                s.sendMessage("§e[ExcelProtect] §flatest incidents:");
                for (String line : r.lines()) s.sendMessage("§7  " + line);
            });
        });
        return true;
    }

    /**
     * /excelprotect healing <player> [last-hours] — that player's healing, two
     * sections. Any sender. (1) The 0.1.3 incident form over the healing stream:
     * self / done to others / received. (2) The 0.1.8 §S3 SPELL form over the
     * combat stream's persisted combat_heal records: per spell (or potion) and
     * target — casts, raw, effective, overheal — aggregated PER CAST, with the
     * unattributed residue as its own line.
     */
    private boolean healingCmd(CommandSender s, String[] args) {
        if (args.length < 2) { s.sendMessage("§eUsage: §f/excelprotect healing <player> [last-hours]"); return true; }
        String who = args[1];
        double hours = DamageReport.parseHours(args.length >= 3 ? args[2] : null);
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            ledger.flushAll();
            HealingReport.Report r = null;
            if (healingWatch != null) {
                Exporter.Query q = Exporter.Query.parse(new String[]{
                        "stream=healing", "player=" + who, "hours=" + hours});
                r = HealingReport.build(Exporter.scan(ledger, q), who, 10);
            }
            Exporter.Query cq = Exporter.Query.parse(new String[]{
                    "stream=combat", "type=combat_heal", "player=" + who, "hours=" + hours});
            HealingSpellReport.Report sr = HealingSpellReport.build(Exporter.scan(ledger, cq), who, 12);
            HealingReport.Report fr = r;
            admin(s, "healing_query", who + " last " + DamageReport.fmtHours(hours)
                    + " (" + (fr == null ? 0 : fr.total()) + " incidents, "
                    + sr.casts() + " spell/potion casts)");
            Bukkit.getScheduler().runTask(this, () -> {
                boolean any = false;
                if (fr != null && fr.total() > 0) {
                    any = true;
                    s.sendMessage("§e[ExcelProtect] §f" + who + " — healing, last "
                            + DamageReport.fmtHours(hours) + ":");
                    s.sendMessage("§7  self: " + Json.num(fr.selfHealed()) + " healed ("
                            + Json.num(fr.selfEffective()) + " effective)");
                    s.sendMessage("§7  to others: " + Json.num(fr.doneHealed()) + " healed ("
                            + Json.num(fr.doneEffective()) + " effective)");
                    s.sendMessage("§7  received from others: " + Json.num(fr.receivedHealed())
                            + " (+ " + Json.num(fr.receivedUnknown()) + " unattributed)");
                    for (String line : fr.lines()) s.sendMessage("§7  " + line);
                }
                if (sr.records() > 0 || sr.residueRaw() > 0) {
                    any = true;
                    s.sendMessage("§e[ExcelProtect] §f" + who + " — by spell (combat records), last "
                            + DamageReport.fmtHours(hours) + ":");
                    for (String line : sr.lines()) s.sendMessage("§7  " + line);
                }
                if (!any) {
                    if (healingWatch == null && cfg.combatHealing())
                        s.sendMessage("§c[ExcelProtect] no healing recorded for " + who
                                + " in the last " + DamageReport.fmtHours(hours));
                    else if (healingWatch == null)
                        s.sendMessage("§c[ExcelProtect] healing incidents are OFF in config (log.combat.healing-incidents)");
                    else
                        s.sendMessage("§c[ExcelProtect] no healing recorded for " + who
                                + " in the last " + DamageReport.fmtHours(hours));
                }
            });
        });
        return true;
    }

    // ------------------------------------------------- where + playtime query forms (0.1.4)
    /** /excelprotect where <player> [last-hours] — the player's location route over the window (locations stream): start → anchors → latest. Any sender. */
    private boolean whereCmd(CommandSender s, String[] args) {
        if (args.length < 2) { s.sendMessage("§eUsage: §f/excelprotect where <player> [last-hours]"); return true; }
        if (locationWatch == null) { s.sendMessage("§c[ExcelProtect] locations are OFF in config (log.locations)"); return true; }
        String who = args[1];
        double hours = DamageReport.parseHours(args.length >= 3 ? args[2] : null);
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            ledger.flushAll();
            Exporter.Query q = Exporter.Query.parse(new String[]{
                    "stream=locations", "player=" + who, "hours=" + hours});
            List<String> lines = Exporter.scan(ledger, q);
            WhereReport.Report r = WhereReport.build(lines, 15);
            admin(s, "where_query", who + " last " + DamageReport.fmtHours(hours)
                    + " (" + r.records() + " records, " + r.steps() + " steps)");
            Bukkit.getScheduler().runTask(this, () -> {
                if (r.records() == 0) {
                    s.sendMessage("§c[ExcelProtect] no location records for " + who
                            + " in the last " + DamageReport.fmtHours(hours));
                    return;
                }
                s.sendMessage("§e[ExcelProtect] §f" + who + " — where, last " + DamageReport.fmtHours(hours)
                        + ": " + r.records() + " record(s) (" + r.anchors() + " anchors, "
                        + r.samples() + " samples), " + r.steps() + " step(s):");
                for (String line : r.lines()) s.sendMessage("§7  " + line);
                if (r.hiddenSteps() > 0)
                    s.sendMessage("§7  … " + r.hiddenSteps()
                            + " intermediate step(s) omitted — full route via /excelprotect export stream=locations player=" + who);
            });
        });
        return true;
    }

    /**
     * /excelprotect playtime <player> [days] — total time online over the last
     * N Chicago calendar days (default 1 = today), derived from the players
     * stream's join/leave arcs Protect already keeps (NO new capture — the
     * gap was the query, coverage map row 30). An arc still open counts to
     * now only when the player is actually online at query time. Any sender.
     */
    private boolean playtimeCmd(CommandSender s, String[] args) {
        if (args.length < 2) { s.sendMessage("§eUsage: §f/excelprotect playtime <player> [days]"); return true; }
        String who = args[1];
        int days = PlaytimeReport.parseDays(args.length >= 3 ? args[2] : null);
        long now = System.currentTimeMillis();
        long fromMs = java.time.LocalDate.now(Ledger.ZONE).minusDays(days - 1L)
                .atStartOfDay(Ledger.ZONE).toInstant().toEpochMilli();
        // Decide the open-arc rule on the main thread, before going async.
        Player online = Bukkit.getPlayerExact(who);
        long closeOpenAt = online != null ? now : -1;
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            ledger.flushAll();
            // No time filter on the scan: an arc that STARTED before the window
            // still contributes its in-window overlap, so pairing needs the join.
            Exporter.Query q = Exporter.Query.parse(new String[]{
                    "stream=players", "type=player_join,player_leave", "player=" + who});
            List<String> lines = Exporter.scan(ledger, q);
            PlaytimeReport.Report r = PlaytimeReport.build(lines, who, fromMs, now, closeOpenAt);
            admin(s, "playtime_query", who + " last " + days + "d (" + r.sessions()
                    + " sessions, " + PlaytimeReport.fmtDuration(r.totalMs()) + ")");
            Bukkit.getScheduler().runTask(this, () -> {
                if (r.sessions() == 0) {
                    s.sendMessage("§c[ExcelProtect] no recorded sessions for " + who
                            + " in the last " + days + " day(s)");
                    return;
                }
                s.sendMessage("§e[ExcelProtect] §f" + who + " — playtime, last " + days + " day(s): §f"
                        + PlaytimeReport.fmtDuration(r.totalMs()) + "§f total over "
                        + r.sessions() + " session(s), longest "
                        + PlaytimeReport.fmtDuration(r.longestMs()));
                if (r.openNow())
                    s.sendMessage("§7  online now — current session "
                            + PlaytimeReport.fmtDuration(r.currentMs()) + " (since "
                            + Ledger.isoOf(r.openSinceMs()).substring(11, 19) + ")");
            });
        });
        return true;
    }

    private boolean deathsCmd(CommandSender s, String[] args) {
        // /excelprotect deaths <player> [restore <n> [force]]
        if (args.length < 2) { s.sendMessage("§eUsage: §f/excelprotect deaths <player> [restore <n> [force]]"); return true; }
        String who = args[1];
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            ledger.flushAll();
            Exporter.Query q = Exporter.Query.parse(new String[]{"stream=deaths", "type=death_snapshot", "player=" + who});
            List<String> lines = Exporter.scan(ledger, q);
            Bukkit.getScheduler().runTask(this, () -> {
                if (lines.isEmpty()) { s.sendMessage("§c[ExcelProtect] no death snapshots for " + who); return; }
                if (args.length >= 4 && args[2].equals("restore")) {
                    int n;
                    try { n = Integer.parseInt(args[3]); } catch (Exception e) { n = 0; }
                    int idx = lines.size() - 1 - n;
                    if (idx < 0 || idx >= lines.size()) { s.sendMessage("§c[ExcelProtect] no snapshot #" + n); return; }
                    Player target = Bukkit.getPlayerExact(who);
                    if (target == null) { s.sendMessage("§c[ExcelProtect] " + who + " is not online"); return; }
                    boolean force = args.length >= 5 && args[4].equals("force");
                    admin(s, "death_restore", who + " snapshot #" + n + (force ? " FORCE" : ""));
                    s.sendMessage("§e[ExcelProtect] §f" + rollback.restoreDeath(lines.get(idx), target,
                            s instanceof Player p ? p : null, force));
                    return;
                }
                admin(s, "death_lookup", who + " (" + lines.size() + " snapshots)");
                s.sendMessage("§e[ExcelProtect] §f" + lines.size() + " death snapshot(s) for " + who
                        + " (restore with: deaths " + who + " restore <n>, 0 = latest)");
                for (int i = 0; i < Math.min(5, lines.size()); i++) {
                    String l = lines.get(lines.size() - 1 - i);
                    s.sendMessage("§7  #" + i + " " + Json.strField(l, "ts") + " — " + Json.strField(l, "death_message"));
                }
            });
        });
        return true;
    }

    private boolean restoreEntityCmd(CommandSender s, String[] args) {
        // /excelprotect restore-entity [preview] [force] [filters: hours=|from=|to=|match=|world=]
        java.util.List<String> tokList = new java.util.ArrayList<>(java.util.List.of(args).subList(1, args.length));
        boolean preview = tokList.remove("preview");
        boolean force = tokList.remove("force");
        String[] tokens = tokList.toArray(new String[0]);
        Exporter.Query probe = Exporter.Query.parse(tokens);
        if (probe.error != null) {
            s.sendMessage("§c[ExcelProtect] " + probe.error + " — restore-entity aborted (nothing was spawned).");
            admin(s, "restore_entity_error", probe.error);
            return true;
        }
        long target = probe.toMs >= 0 ? probe.toMs : System.currentTimeMillis();
        Player actor = s instanceof Player p ? p : null;
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            ledger.flushAll();
            Exporter.Query q = Exporter.Query.parse(tokens); q.stream = "entities"; q.types = "entity_death";
            List<String> lines = Exporter.scan(ledger, q);
            Bukkit.getScheduler().runTask(this, () -> {
                String line = Rollback.chooseEntityLine(lines, target);
                if (line == null) { s.sendMessage("§c[ExcelProtect] no entity death records match"); return; }
                String label = Json.strField(line, "entity") + (Json.strField(line, "name") != null
                        ? " '" + Json.strField(line, "name") + "'" : "") + " died " + Json.strField(line, "ts");
                if (preview) {
                    boolean used = recovery.isRestored(Rollback.entityMark(line));
                    admin(s, "restore_entity_preview", label);
                    s.sendMessage("§e[ExcelProtect] §fPREVIEW: would restore " + label
                            + (used ? " — ALREADY RESTORED once (commit needs force)" : "")
                            + " — rerun without 'preview' to commit");
                    return;
                }
                admin(s, "restore_entity", label + (force ? " FORCE" : ""));
                s.sendMessage("§e[ExcelProtect] §f" + rollback.restoreEntity(line, actor, force));
            });
        });
        return true;
    }

    private boolean restoreInventoryCmd(CommandSender s, String[] args) {
        // /excelprotect restore-inventory <player> [preview] [force] [filters: hours=|from=|to=]
        if (args.length < 2) {
            s.sendMessage("§eUsage: §f/excelprotect restore-inventory <player> [preview] [force] [hours=N|from=|to=]");
            return true;
        }
        String who = args[1];
        java.util.List<String> tokList = new java.util.ArrayList<>(java.util.List.of(args).subList(2, args.length));
        boolean preview = tokList.remove("preview");
        boolean force = tokList.remove("force");
        String[] tokens = tokList.toArray(new String[0]);
        Exporter.Query probe = Exporter.Query.parse(tokens);
        if (probe.error != null) {
            s.sendMessage("§c[ExcelProtect] " + probe.error + " — restore-inventory aborted (nothing was changed).");
            admin(s, "restore_inventory_error", probe.error);
            return true;
        }
        long target = probe.toMs >= 0 ? probe.toMs : System.currentTimeMillis();
        Player actor = s instanceof Player p ? p : null;
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            ledger.flushAll();
            Exporter.Query q = Exporter.Query.parse(tokens);
            q.stream = "deaths"; q.types = "death_snapshot"; q.player = who;
            List<String> lines = Exporter.scan(ledger, q);
            Bukkit.getScheduler().runTask(this, () -> {
                String line = Rollback.chooseSnapshot(lines, target);
                if (line == null) {
                    s.sendMessage("§c[ExcelProtect] no death snapshots for " + who + " at/before the target");
                    return;
                }
                int[] counts = Rollback.snapshotCounts(line);
                String deathId = Json.strField(line, "death_id");
                if (preview) {
                    boolean used = deathId != null && recovery.isRestored("death:" + deathId);
                    admin(s, "restore_inventory_preview", who + " snapshot " + Json.strField(line, "ts"));
                    s.sendMessage("§e[ExcelProtect] §fPREVIEW: would REPLACE " + who + "'s inventory with the death snapshot of "
                            + Json.strField(line, "ts") + " — " + counts[0] + " inventory stacks, " + counts[1]
                            + " ender-chest stacks" + (used ? " — ALREADY RESTORED once (commit needs force)" : "")
                            + " — rerun without 'preview' to commit");
                    return;
                }
                Player targetP = Bukkit.getPlayerExact(who);
                if (targetP == null) { s.sendMessage("§c[ExcelProtect] " + who + " is not online"); return; }
                admin(s, "restore_inventory", who + " snapshot " + Json.strField(line, "ts") + (force ? " FORCE" : ""));
                s.sendMessage("§e[ExcelProtect] §f" + rollback.restoreInventory(line, targetP, actor, force));
            });
        });
        return true;
    }

    private boolean lostfound(CommandSender s, String[] args) {
        if (!(s instanceof Player p)) { s.sendMessage("players only"); return true; }
        List<Recovery.LedgerEntry> entries = recovery.recoverable(p.getUniqueId());
        if (args.length >= 2 && args[0].equals("claim")) {
            int n;
            try { n = Integer.parseInt(args[1]); } catch (Exception e) { n = -1; }
            if (n < 0 || n >= entries.size()) { p.sendMessage("§c[Lost & Found] no entry #" + n); return true; }
            Recovery.LedgerEntry en = entries.get(n);
            if (recovery.cooldownActive(p.getUniqueId())) { p.sendMessage("§c[Lost & Found] recovery cooldown is active"); return true; }
            double bal = economy.balance(p);
            boolean free = recovery.freeApplies(p.getUniqueId(), bal);
            if (!free && economy.on() && bal < cfg.lfPrice()) {
                p.sendMessage("§c[Lost & Found] you need $" + cfg.lfPrice() + " (balance $" + (long) bal + ")");
                return true;
            }
            ItemStack stack = en.item().restore();
            if (stack == null) { p.sendMessage("§c[Lost & Found] that record has no restorable payload"); return true; }
            if (!free && economy.on() && !economy.charge(p, cfg.lfPrice(), "lostfound")) {
                p.sendMessage("§c[Lost & Found] charge failed"); return true;
            }
            p.getInventory().addItem(stack);
            recovery.markRestored(en.id());
            recovery.noteRecovery(p.getUniqueId(), free);
            ledger.emit("deaths", p.getLocation(), new Json.Obj().put("type", "lostfound_recovery")
                    .put("player", p.getName()).put("player_uuid", p.getUniqueId().toString())
                    .put("item", en.item().toJson()).put("price", free ? 0 : cfg.lfPrice()).put("free", free));
            p.sendMessage("§a[Lost & Found] §fRecovered " + en.item().fingerprint()
                    + (free ? " (free first recovery)" : " for $" + cfg.lfPrice()));
            return true;
        }
        if (entries.isEmpty()) { p.sendMessage("§e[Lost & Found] §fnothing recoverable right now."); return true; }
        p.sendMessage("§e[Lost & Found] §fyour destroyed items (recover with /lostfound claim <n>):");
        for (int i = 0; i < Math.min(10, entries.size()); i++) {
            Recovery.LedgerEntry en = entries.get(i);
            p.sendMessage("§7  #" + i + " " + en.item().fingerprint() + " — " + en.cause()
                    + " (" + Ledger.isoOf(en.tsMs()) + ")");
        }
        p.sendMessage("§7Price: $" + cfg.lfPrice() + " per item"
                + (recovery.freeApplies(p.getUniqueId(), economy.balance(p)) ? " — your first recovery is FREE" : ""));
        return true;
    }

    private boolean deathrecord(CommandSender s, String[] args) {
        if (args.length < 1) { s.sendMessage("§eUsage: §f/deathrecord <player>"); return true; }
        String who = args[0];
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            ledger.flushAll();
            Exporter.Query qd = Exporter.Query.parse(new String[]{"stream=deaths", "type=death_snapshot", "player=" + who});
            List<String> deaths = Exporter.scan(ledger, qd);
            Exporter.Query qb = Exporter.Query.parse(new String[]{"stream=deaths", "type=item_destroyed", "player=" + who});
            List<String> burns = Exporter.scan(ledger, qb);
            Bukkit.getScheduler().runTask(this, () -> {
                admin(s, "deathrecord_lookup", who);
                s.sendMessage("§e[DeathRecord] §f" + who + ": " + deaths.size() + " death snapshot(s), "
                        + burns.size() + " destroyed item(s) — detail via /excelprotect export stream=deaths player=" + who);
            });
        });
        return true;
    }
}
