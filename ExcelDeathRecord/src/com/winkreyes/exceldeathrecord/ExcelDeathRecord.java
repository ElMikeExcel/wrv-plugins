package com.winkreyes.exceldeathrecord;

import com.winkreyes.exceldeathrecord.core.BurnIndex;
import com.winkreyes.exceldeathrecord.core.DeathRecord;
import com.winkreyes.exceldeathrecord.core.DeathSnapshot;
import com.winkreyes.exceldeathrecord.core.RecordFormat;
import com.winkreyes.exceldeathrecord.core.RecordIndex;
import com.winkreyes.exceldeathrecord.core.RecordName;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ExcelDeathRecord 0.2.0 — the death ledger for Wink Reyes
 * Valley: "logs what people have at the point of death before
 * it hits the ground." Every PlayerDeathEvent is snapshotted
 * from the event itself — its final drop list plus the player's
 * inventory/armor/offhand read AT the event — so the record is
 * complete even when the death destroys the drops (lava, fire,
 * void, explosion): nothing here ever reads item entities back
 * from the world. Items are stored byte-exact (serializeAsBytes
 * as Base64) beside a human-readable summary, one file per death
 * under deaths/<player-uuid>/.
 *
 * The same plugin keeps the Burn Ledger (class BurnWatch): a
 * recoverable record of item stacks destroyed in the world by
 * lava, fire, or the void, attributed to their owner as far as
 * the platform knows.
 *
 * 0.2.0 adds /deathrecord restore (typed + tap menu): gives back
 * the byte-exact copies, all-or-nothing, marks each restored item
 * in its record, and logs every restore to restores.csv.
 */
public final class ExcelDeathRecord extends JavaPlugin implements Listener {

    static final String VERSION = "0.2.0";
    private static ExcelDeathRecord INSTANCE;
    static ExcelDeathRecord instance() { return INSTANCE; }

    private RecordStore store;
    private BurnWatch burnWatch;
    private RestoreService restoreService;
    private RestoreMenu menu;
    private java.time.ZoneId displayZone;

    @Override
    public void onEnable() {
        INSTANCE = this;
        saveDefaultConfig();
        try {
            displayZone = java.time.ZoneId.of(getConfig().getString("display-zone", "America/Chicago"));
        } catch (Exception e) {
            displayZone = java.time.ZoneId.of("America/Chicago");
        }
        store = new RecordStore(getDataFolder());
        burnWatch = new BurnWatch(this, store);
        restoreService = new RestoreService(this, store);
        menu = new RestoreMenu(this, store, restoreService, displayZone);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(burnWatch, this);
        getServer().getPluginManager().registerEvents(menu, this);
        burnWatch.startVoidSweep();
        getLogger().info("ExcelDeathRecord " + VERSION + " enabled — death ledger + Burn Ledger watching; restore ready (menu + typed), bedrock-forms="
                + GeyserForms.probe() + ", display-zone=" + displayZone.getId() + ", log=restores.csv");
    }

    /** Async file write with a synchronous fallback when the
     *  scheduler refuses (plugin disabling): the record matters
     *  more than the thread it lands from. */
    void writeAsync(Runnable write) {
        try {
            Bukkit.getScheduler().runTaskAsynchronously(this, write);
        } catch (RuntimeException e) {
            write.run();
        }
    }

    // ---------- the death snapshot ----------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        try {
            Player player = event.getEntity();

            List<DeathSnapshot.RawItem> drops = new ArrayList<>();
            List<ItemStack> dropStacks = event.getDrops();
            for (int i = 0; i < dropStacks.size(); i++) {
                ItemStack s = dropStacks.get(i);
                if (s != null && !s.getType().isAir() && s.getAmount() > 0) {
                    drops.add(ItemCapture.of("drop-" + i, s));
                }
            }

            List<DeathSnapshot.RawItem> carried = new ArrayList<>();
            PlayerInventory inv = player.getInventory();
            ItemStack[] storage = inv.getStorageContents();
            for (int i = 0; i < storage.length; i++) {
                ItemStack s = storage[i];
                if (s != null && !s.getType().isAir() && s.getAmount() > 0) {
                    carried.add(ItemCapture.of("storage-" + i, s));
                }
            }
            addCarried(carried, "helmet", inv.getHelmet());
            addCarried(carried, "chestplate", inv.getChestplate());
            addCarried(carried, "leggings", inv.getLeggings());
            addCarried(carried, "boots", inv.getBoots());
            addCarried(carried, "offhand", inv.getItemInOffHand());

            Location loc = player.getLocation();
            long now = System.currentTimeMillis();

            DeathRecord record = DeathSnapshot.build(
                    player.getName(),
                    player.getUniqueId().toString(),
                    now,
                    RecordName.timeText(now, store.zone()),
                    loc.getWorld() == null ? "" : loc.getWorld().getKey().toString(),
                    loc.getX(), loc.getY(), loc.getZ(),
                    player.getGameMode().name(),
                    deathMessageText(event),
                    damageCauseName(player),
                    killerText(player),
                    player.getLevel(),
                    player.getTotalExperience(),
                    event.getDroppedExp(),
                    event.getKeepInventory(),
                    drops, carried);

            String text = RecordFormat.write(record);
            writeAsync(() -> {
                try {
                    store.writeDeath(record, text);
                } catch (Exception e) {
                    getLogger().warning("ExcelDeathRecord: could not write death record for "
                            + record.playerName + ": " + e.getMessage());
                }
            });
            getLogger().info("ExcelDeathRecord: death recorded — " + record.playerName + " — "
                    + RecordIndex.summaryLine(record));
        } catch (Throwable t) {
            // The snapshot must never break the death itself.
            getLogger().warning("ExcelDeathRecord: death snapshot failed (death proceeds normally): " + t);
        }
    }

    private static void addCarried(List<DeathSnapshot.RawItem> out, String slot, ItemStack s) {
        if (s != null && !s.getType().isAir() && s.getAmount() > 0) {
            out.add(ItemCapture.of(slot, s));
        }
    }

    private static String deathMessageText(PlayerDeathEvent event) {
        try {
            Component msg = event.deathMessage();
            if (msg != null) return PlainTextComponentSerializer.plainText().serialize(msg);
        } catch (Throwable ignored) { }
        try {
            String legacy = event.getDeathMessage();
            return legacy == null ? "" : legacy;
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String damageCauseName(Player player) {
        EntityDamageEvent last = player.getLastDamageCause();
        return last == null ? "" : last.getCause().name();
    }

    /** "Kay (player)" / "Zombie (minecraft:zombie)" / "none". */
    private static String killerText(Player player) {
        Player killer = player.getKiller();
        if (killer != null) return killer.getName() + " (player)";
        EntityDamageEvent last = player.getLastDamageCause();
        if (last instanceof EntityDamageByEntityEvent byEntity) {
            Entity damager = byEntity.getDamager();
            if (damager instanceof Projectile proj && proj.getShooter() instanceof Entity shooter) {
                damager = shooter;
            }
            if (damager instanceof Player p) return p.getName() + " (player)";
            if (damager instanceof LivingEntity living) {
                return living.getName() + " (" + living.getType().getKey() + ")";
            }
            return damager.getType().getKey().toString();
        }
        return "none";
    }

    // ---------- /deathrecord ----------

    static final String[] HELP = {
        "/deathrecord <player> - list deaths, newest first (10 a page)",
        "/deathrecord <player> page 2 - older deaths",
        "/deathrecord <player> <n> - open death n with item numbers (1 = newest)",
        "/deathrecord burned [player] [page 2] - Burn Ledger (lava, fire, void, despawned valuables)",
        "/deathrecord restore <player> - tap menu: pick death, items, who gets them, confirm",
        "/deathrecord restore <player> <n> <items> [to <player>] [xp] [force]",
        "  <items>: all | 3 | 2,3,7 | 1-5 (numbers from the death view)",
        "  Examples:",
        "  /deathrecord restore Alex 1 all   (everything from Alex's newest death, back to Alex)",
        "  /deathrecord restore Alex 1 all xp   (same, plus the XP Alex had)",
        "  /deathrecord restore .BedrockAlex 1 3 to Sam   (item 3 goes to Sam instead)",
        "  /deathrecord restore Alex 1 2,3,7 to Sam   |   /deathrecord restore Alex 2 1-5",
        "  /deathrecord restore Alex 2026-10-23_221412 3 to Sam   (record id instead of n: never shifts)",
        "  /deathrecord restore burned 4 to Sam   (Burn Ledger entry 4)",
        "Use the real player name (Bedrock names start with a dot). An Essentials nickname works too when it is unique.",
        "Already-restored items are skipped; add force to give them again. If it won't all fit, nothing is given."
    };

    private void help(CommandSender sender) {
        for (String line : HELP) sender.sendMessage(line);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("deathrecord")) return false;
        if (!sender.hasPermission("exceldeathrecord.admin")) {
            sender.sendMessage("ExcelDeathRecord: that command is for ops.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            help(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("restore")) {
            if (!sender.hasPermission(RestoreMenu.PERM)) {
                sender.sendMessage("ExcelDeathRecord: restore is for ops.");
                return true;
            }
            return restoreCommand(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
        }
        if (args[0].equalsIgnoreCase("burned")) {
            return burnedCommand(sender, args);
        }
        return deathsCommand(sender, args);
    }

    /** Trailing "page N": returns N and strips it, or 1. */
    private static int pageArg(List<String> words) {
        if (words.size() >= 2 && words.get(words.size() - 2).equalsIgnoreCase("page")) {
            try {
                int p = Integer.parseInt(words.get(words.size() - 1));
                words.remove(words.size() - 1);
                words.remove(words.size() - 1);
                return Math.max(1, p);
            } catch (NumberFormatException ignored) { }
        }
        return 1;
    }

    /** Typed name -> deaths folder UUID: stored name, Bukkit name, then Essentials nickname. */
    UUID resolveSource(String name) {
        UUID uuid = store.resolveDeathsPlayer(name);
        if (uuid != null) return uuid;
        Player online = Names.online(name);
        if (online != null && !store.readDeaths(online.getUniqueId()).isEmpty()) return online.getUniqueId();
        UUID nick = Names.offlineNick(name);
        if (nick != null && !store.readDeaths(nick).isEmpty()) return nick;
        return null;
    }

    private String when(long epochMillis) { return menu.when(epochMillis); }

    private boolean deathsCommand(CommandSender sender, String[] args) {
        List<String> words = new ArrayList<>(java.util.Arrays.asList(args));
        int page = pageArg(words);
        // A trailing positive integer or record id picks a record; the
        // rest of the line is the player name (Bedrock names may contain spaces).
        int n = -1;
        String id = "";
        if (words.size() >= 2) {
            String last = words.get(words.size() - 1);
            if (com.winkreyes.exceldeathrecord.core.RestoreArgs.RECORD_ID.matcher(last.replace(".yml", "")).matches()) {
                id = last.replace(".yml", ""); words.remove(words.size() - 1);
            } else {
                try {
                    int parsed = Integer.parseInt(last);
                    if (parsed > 0) { n = parsed; words.remove(words.size() - 1); }
                } catch (NumberFormatException ignored) { }
            }
        }
        String name = String.join(" ", words).trim();

        UUID uuid = resolveSource(name);
        if (uuid == null) {
            sender.sendMessage("ExcelDeathRecord: no death records on file for " + name
                    + ". Use the real player name (Bedrock names start with a dot).");
            return true;
        }
        List<RecordIndex.Entry> entries = RecordIndex.newestFirst(store.readDeaths(uuid));
        if (entries.isEmpty()) {
            sender.sendMessage("ExcelDeathRecord: no death records on file for " + name + ".");
            return true;
        }
        String storedName = entries.get(0).record.playerName;

        if (n < 0 && id.isEmpty()) {
            int pages = RecordIndex.pageCount(entries.size(), 10);
            List<RecordIndex.Entry> shown = RecordIndex.page(entries, page, 10);
            sender.sendMessage("ExcelDeathRecord: " + entries.size() + " death(s) on file for " + storedName
                    + " (page " + Math.min(page, pages) + "/" + pages + ", times " + displayZone.getId() + "):");
            if (shown.isEmpty()) { sender.sendMessage("No page " + page + ". Last page is " + pages + "."); return true; }
            for (int i = 0; i < shown.size(); i++) {
                RecordIndex.Entry e = shown.get(i);
                int number = (page - 1) * 10 + i + 1;
                List<DeathRecord.ItemEntry> list = RecordIndex.restorable(e.record);
                int rc = 0; for (DeathRecord.ItemEntry it : list) if (it.isRestored()) rc++;
                sender.sendMessage(number + ". " + when(e.record.epochMillis) + " | " + RecordIndex.summaryLine(e.record)
                        + (rc > 0 ? " [" + rc + "/" + list.size() + " restored]" : ""));
            }
            if (page < pages) sender.sendMessage("Older: /deathrecord " + storedName + " page " + (page + 1));
            sender.sendMessage("Open one: /deathrecord " + storedName + " <n>   Restore menu: /deathrecord restore " + storedName);
            return true;
        }

        RecordIndex.Entry picked = null;
        if (!id.isEmpty()) {
            for (int i = 0; i < entries.size(); i++) if (entries.get(i).fileName.equals(id + ".yml")) { picked = entries.get(i); n = i + 1; }
        } else {
            picked = RecordIndex.pick(entries, n);
        }
        if (picked == null) {
            sender.sendMessage("ExcelDeathRecord: " + storedName + " has " + entries.size()
                    + " death(s) on file - there is no " + (id.isEmpty() ? "#" + n : "record " + id) + ".");
            return true;
        }
        DeathRecord r = picked.record;
        sender.sendMessage("Death #" + n + " (record " + picked.fileName.replace(".yml", "") + ") - "
                + when(r.epochMillis) + " | " + RecordIndex.summaryLine(r));
        sender.sendMessage("Death message: " + (r.deathMessage.isEmpty() ? "(none)" : r.deathMessage));
        sender.sendMessage("XP level " + r.xpLevel + ", total XP " + r.totalExperience
                + ", dropped XP " + r.droppedExp + "; keepInventory: " + r.keepInventory
                + "; game mode: " + r.gamemode
                + (r.xpRestoredTimes > 0 ? "; XP restored to " + r.xpRestoredTo + " " + r.xpRestoredAt : ""));
        List<DeathRecord.ItemEntry> list = RecordIndex.restorable(r);
        if (RecordIndex.restorableIsCarried(r)) {
            sender.sendMessage("Nothing dropped. Carried at death (" + list.size() + "), restore needs force:");
        } else {
            sender.sendMessage("Dropped (" + list.size() + "), restore numbers:");
        }
        for (int i = 0; i < list.size(); i++) sender.sendMessage("  " + RecordIndex.numberedItemLine(i + 1, list.get(i)));
        if (!RecordIndex.restorableIsCarried(r) && !r.inventory.isEmpty()) {
            sender.sendMessage("Carried at death: " + r.inventory.size() + " stack(s) (same items, by slot; restore uses the numbers above).");
        }
        sender.sendMessage("Restore: /deathrecord restore " + storedName + " " + n + " <all|3|2,3,7|1-5> [to <player>] [xp]");
        return true;
    }

    private List<BurnIndex.Entry> burnsNewestFirst() {
        return BurnIndex.newestFirst(store.readBurns(null));
    }

    private boolean burnedCommand(CommandSender sender, String[] args) {
        List<String> words = new ArrayList<>(java.util.Arrays.asList(args).subList(1, args.length));
        int page = pageArg(words);
        String name = words.isEmpty() ? null : String.join(" ", words).trim();
        if (name != null && name.isEmpty()) name = null;
        // Numbers are positions in the UNFILTERED newest-first ledger, so
        // "restore burned <n>" means the same entry whatever filter was used.
        List<BurnIndex.Entry> all = burnsNewestFirst();
        List<Integer> numbers = new ArrayList<>();
        List<BurnIndex.Entry> entries = new ArrayList<>();
        java.util.Set<String> wanted = null;
        if (name != null) {
            wanted = new java.util.HashSet<>();
            for (BurnIndex.Entry e : store.readBurns(name)) wanted.add(e.folder + "/" + e.fileName);
        }
        for (int i = 0; i < all.size(); i++) {
            BurnIndex.Entry e = all.get(i);
            if (wanted == null || wanted.contains(e.folder + "/" + e.fileName)) { entries.add(e); numbers.add(i + 1); }
        }
        if (entries.isEmpty()) {
            sender.sendMessage("ExcelDeathRecord [Burn Ledger]: no destructions on file"
                    + (name == null ? "." : " for " + name + "."));
            return true;
        }
        int pages = RecordIndex.pageCount(entries.size(), 10);
        sender.sendMessage("ExcelDeathRecord [Burn Ledger]: " + entries.size() + " destruction(s) on file"
                + (name == null ? "" : " for " + name) + " (page " + Math.min(page, pages) + "/" + pages + "):");
        List<BurnIndex.Entry> shown = RecordIndex.page(entries, page, 10);
        List<Integer> shownNumbers = RecordIndex.page(numbers, page, 10);
        if (shown.isEmpty()) { sender.sendMessage("No page " + page + ". Last page is " + pages + "."); return true; }
        for (int i = 0; i < shown.size(); i++) {
            sender.sendMessage(shownNumbers.get(i) + ". " + when(shown.get(i).record.epochMillis) + " | "
                    + BurnIndex.summaryLine(shown.get(i).record));
        }
        if (page < pages) sender.sendMessage("Older: /deathrecord burned " + (name == null ? "" : name + " ") + "page " + (page + 1));
        sender.sendMessage("Restore one: /deathrecord restore burned <n> to <player>");
        return true;
    }

    private boolean restoreCommand(CommandSender sender, String[] rest) {
        com.winkreyes.exceldeathrecord.core.RestoreArgs a = com.winkreyes.exceldeathrecord.core.RestoreArgs.parse(rest);
        if (a.mode == com.winkreyes.exceldeathrecord.core.RestoreArgs.Mode.ERROR) {
            sender.sendMessage("ExcelDeathRecord: " + a.error);
            sender.sendMessage("Examples: /deathrecord help");
            return true;
        }
        String staff = sender instanceof Player p ? p.getName() : "CONSOLE";

        if (a.mode == com.winkreyes.exceldeathrecord.core.RestoreArgs.Mode.BURNED) {
            List<BurnIndex.Entry> all = burnsNewestFirst();
            BurnIndex.Entry picked = null;
            if (a.n > 0 && a.n <= all.size()) picked = all.get(a.n - 1);
            if (!a.recordId.isEmpty()) for (BurnIndex.Entry e : all) if (e.fileName.equals(a.recordId + ".yml")) { picked = e; break; }
            if (picked == null) { sender.sendMessage("ExcelDeathRecord: no Burn Ledger entry " + (a.n > 0 ? "#" + a.n : a.recordId) + ". See /deathrecord burned"); return true; }
            Player target = Names.online(a.target);
            if (target == null) { sender.sendMessage("ExcelDeathRecord: " + a.target + " is not online (use their real name or nickname)."); return true; }
            RestoreService.Source src = new RestoreService.Source();
            src.burned = true;
            src.file = store.burnFile(picked.folder, picked.fileName);
            src.sourceName = picked.record.ownerName;
            for (String line : restoreService.restore(staff, src, "1", null, target, a.force, false)) sender.sendMessage(line);
            return true;
        }

        UUID uuid = resolveSource(a.player);
        if (uuid == null) {
            sender.sendMessage("ExcelDeathRecord: no death records on file for " + a.player
                    + ". Use the real player name (Bedrock names start with a dot); a nickname only works if Essentials knows it.");
            return true;
        }
        List<RecordIndex.Entry> entries = RecordIndex.newestFirst(store.readDeaths(uuid));
        if (entries.isEmpty()) { sender.sendMessage("ExcelDeathRecord: no death records on file for " + a.player + "."); return true; }
        String storedName = entries.get(0).record.playerName;
        int n = a.n;
        if (!a.recordId.isEmpty()) {
            n = -1;
            for (int i = 0; i < entries.size(); i++) if (entries.get(i).fileName.equals(a.recordId + ".yml")) n = i + 1;
            if (n < 0) { sender.sendMessage("ExcelDeathRecord: " + storedName + " has no record " + a.recordId + "."); return true; }
        }
        if (a.hasRecord() && (n < 1 || n > entries.size())) {
            sender.sendMessage("ExcelDeathRecord: " + storedName + " has " + entries.size() + " death(s) on file - there is no #" + n + ".");
            return true;
        }

        if (a.mode == com.winkreyes.exceldeathrecord.core.RestoreArgs.Mode.MENU) {
            if (!(sender instanceof Player staffPlayer)) {
                sender.sendMessage("The tap menu needs a player. From console, type it out:");
                help(sender);
                return true;
            }
            menu.open(staffPlayer, uuid, entries, n);
            return true;
        }

        RecordIndex.Entry picked = entries.get(n - 1);
        Player target = a.target.isEmpty() ? Bukkit.getPlayer(uuid) : Names.online(a.target);
        if (target == null) {
            sender.sendMessage("ExcelDeathRecord: " + (a.target.isEmpty() ? storedName : a.target)
                    + " is not online. Items can only go to an online player (add: to <player>).");
            return true;
        }
        RestoreService.Source src = new RestoreService.Source();
        src.file = store.deathFile(uuid, picked.fileName);
        src.sourceName = storedName;
        src.sourceUuid = uuid;
        for (String line : restoreService.restore(staff, src, a.items, null, target, a.force, a.xp)) sender.sendMessage(line);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : new String[]{"restore", "burned", "help"}) if (s.startsWith(args[0].toLowerCase(java.util.Locale.ROOT))) out.add(s);
            for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase(java.util.Locale.ROOT).startsWith(args[0].toLowerCase(java.util.Locale.ROOT))) out.add(p.getName());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("restore")) {
            if ("burned".startsWith(args[1].toLowerCase(java.util.Locale.ROOT))) out.add("burned");
            for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase(java.util.Locale.ROOT).startsWith(args[1].toLowerCase(java.util.Locale.ROOT))) out.add(p.getName());
        }
        return out;
    }
}
