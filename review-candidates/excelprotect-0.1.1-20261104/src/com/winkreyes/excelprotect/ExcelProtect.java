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
 * ExcelProtect 0.1.1 — the all-in-one record + recovery system (SPEC v2). One async
 * writer, daily JSONL per stream, byte-exact payloads, rollback + restores + Lost &
 * Found. Every adapter (GriefPrevention / Vault / mcMMO / Magic) is fail-soft.
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
    private boolean gpOn, mcmmoOn;

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
        importer = new Importer(ledger, getDataFolder());

        var pm = getServer().getPluginManager();
        pm.registerEvents(combat, this);
        pm.registerEvents(blocks, this);
        Containers containers = new Containers(ledger, cfg);
        pm.registerEvents(containers, this);
        watch = new Watch(this, ledger, cfg, getDataFolder());
        blocks.setWatch(watch);
        containers.setWatch(watch);
        recovery.setWatch(watch);
        pm.registerEvents(new Entities(ledger, cfg, combat), this);
        pm.registerEvents(new ItemEvents(this, ledger, cfg, recovery), this);
        pm.registerEvents(new Players(ledger, cfg, combat), this);
        pm.registerEvents(economy, this);
        Adapters adapters = new Adapters(this, ledger, cfg);
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

        getLogger().info("[ExcelProtect] READY version=0.1.1 dir=" + getDataFolder()
                + " gp=" + (gpOn ? "on" : "off") + " vault=" + (economy.on() ? "on" : "off")
                + " mcmmo=" + (mcmmoOn ? "on" : "off") + " retention-days=" + cfg.retentionDays());
    }

    @Override public void onDisable() {
        if (ledger != null) ledger.flushAll();
    }

    // mcMMO snapshots on join/quit (samples stream)
    @EventHandler public void onJoinSnap(PlayerJoinEvent e) { if (mcmmo != null) mcmmo.snapshot(e.getPlayer(), "join"); }
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
        if (args.length == 0) { s.sendMessage("§e/excelprotect §fexport|inspect [player <name>|at <world> <x> <y> <z>]|rollback|restore-stand|deaths|import|status"); return true; }
        switch (args[0]) {
            case "status" -> {
                s.sendMessage("§e[ExcelProtect] §fversion=0.1.1 gp=" + (gpOn ? "on" : "off")
                        + " vault=" + (economy.on() ? "on" : "off") + " mcmmo=" + (mcmmoOn ? "on" : "off")
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
                        List<String> undoLines = rollback.captureUndo(chosen);
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
                        int n = rollback.rollbackBlocks(undoLines, Long.MAX_VALUE, actor);
                        uf.renameTo(new File(getDataFolder(), "undo-" + opId + ".jsonl.done"));
                        admin(s, "rollback_undo", opId + ": " + n + " positions");
                        s.sendMessage("§e[ExcelProtect] §fundid " + opId + ": " + n + " position(s) restored");
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
            case "deaths" -> { return deathsCmd(s, args); }
            case "import" -> {
                if (args.length < 3 || !args[1].equals("deathrecord")) {
                    s.sendMessage("§eUsage: §f/excelprotect import deathrecord <folder-of-ExcelDeathRecord-data>");
                    return true;
                }
                File root = new File(args[2]);
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    Importer.Result r = importer.importDeathRecord(root);
                    admin(s, "import", "deathrecord " + root + " -> " + r.deaths() + " deaths, " + r.burns() + " burns");
                    Bukkit.getScheduler().runTask(this, () -> s.sendMessage("§e[ExcelProtect] §fimport: "
                            + r.deaths() + " deaths, " + r.burns() + " burns, " + r.skipped() + " skipped, " + r.errors() + " errors"));
                });
                return true;
            }
            default -> { s.sendMessage("§e/excelprotect §fexport|inspect [player <name>|at <world> <x> <y> <z>]|rollback|restore-stand|deaths|import|status"); return true; }
        }
    }

    private boolean hasAt(String[] tokens) {
        for (String t : tokens) if (t.startsWith("at=")) return true;
        return false;
    }

    // ------------------------------------------------- inspect query forms (0.1.1)
    /** /excelprotect inspect player <name> [n] — that player's latest recorded activity (containers, items, deaths, blocks). Any sender. */
    private boolean inspectPlayerCmd(CommandSender s, String[] args) {
        if (args.length < 3) { s.sendMessage("§eUsage: §f/excelprotect inspect player <name> [n]"); return true; }
        String who = args[2];
        int limit = InspectReport.parseLimit(args.length >= 4 ? args[3] : null);
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            ledger.flushAll();
            List<String> scanned = new ArrayList<>();
            for (String stream : new String[]{"containers", "items", "deaths", "blocks"}) {
                Exporter.Query q = Exporter.Query.parse(new String[]{"player=" + who});
                q.stream = stream;
                scanned.addAll(Exporter.scan(ledger, q));
            }
            InspectReport.Report r = InspectReport.build(scanned, limit);
            admin(s, "inspect_player", who + " (" + r.total() + " events)");
            Bukkit.getScheduler().runTask(this, () -> {
                if (r.total() == 0) { s.sendMessage("§c[ExcelProtect] no recorded activity for " + who); return; }
                s.sendMessage("§e[ExcelProtect] §f" + who + " — latest " + r.lines().size()
                        + " of " + r.total() + " recorded event(s):");
                for (String line : r.lines()) s.sendMessage("§7  " + line);
            });
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
