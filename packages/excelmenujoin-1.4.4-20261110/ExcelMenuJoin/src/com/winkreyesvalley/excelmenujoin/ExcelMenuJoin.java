package com.winkreyesvalley.excelmenujoin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ExcelMenuJoin 1.3.0 - full rename of MenuJoin 1.2.0 (same features: menu
 * compass on join/respawn, compass never drops on death and cannot be thrown
 * away, /compass to replace a lost one, /excelmenujoin toggle, %menujoin_status%).
 *
 * Rename compatibility (nothing player-facing breaks):
 *  - PlaceholderAPI expansion identifier stays "menujoin" (%menujoin_status%).
 *  - /menujoin keeps working as an alias of /excelmenujoin.
 *  - The auto-open toggle is stored in the player PDC. Old data lives under
 *    menujoin:autojoin_disabled; the new key is excelmenujoin:autojoin_disabled.
 *    Reads check the new key first and fall back to the legacy key; writes set
 *    BOTH keys so the state stays consistent for old and new readers.
 *  - Menu compasses: new compasses are stamped with the new key
 *    excelmenujoin:menu_compass AND the legacy key menujoin:menu_compass.
 *    Compasses carrying only the legacy stamp are still recognized and get the
 *    new stamp added on join (restamp), keeping the legacy stamp so other
 *    plugins that check menujoin:menu_compass (e.g. ExcelShops) keep working.
 *  - On first enable, any files in the old plugins/MenuJoin data folder are
 *    copied to plugins/ExcelMenuJoin.
 *
 * 1.4.0 (2026-10-19): the single give path (giveMenuCompass, used by join,
 * respawn AND /compass) now consults ExcelPouch's PouchQuery service first:
 * if the player's Utility Pouch already holds a menu compass, the give is
 * skipped — a parked compass must never duplicate. The lookup FAILS OPEN:
 * ExcelPouch absent, no service, or any error, and the give proceeds exactly
 * as in 1.3.0 (self-healing if the pouch is ever removed). Nothing else
 * changes. See PouchCompassCheck.
 *
 * 1.4.1 (2026-10-20): plugin.yml drops the DeluxeMenus hard dependency.
 * This plugin never called the DeluxeMenus API — the menu opens through
 * the player's own "/menu" command (ExcelBedrockMenu's command, whose
 * Java dispatch ExcelMenu's /dm compatibility alias now serves). The
 * hard depend was load-order scaffolding only, and after the ExcelMenu
 * swap retired DeluxeMenus it kept this plugin from loading at all,
 * killing the compass (give-on-join/respawn, /compass, click-to-open).
 * No code changes; the dependency line was the whole defect.
 *
 * 1.4.2 (2026-10-24): tapping the compass in the AIR opens the menu again
 * (see onCompassUse and CompassTap). Nothing else changes.
 *
 * 1.4.4 (2026-11-10): console admin (Mike's Nov 3, 2026 console-admin
 * rule; the Nov 9, 2026 coverage audit found this plugin a FAIL —
 * both commands were self-only and player-gated, in 1.4.1 and
 * still in 1.4.3). /excelmenujoin gains an admin branch routed
 * BEFORE the player-only gate: admin status <player>,
 * admin toggle <player> and admin compass <player>, gated by
 * the new excelmenujoin.admin permission (default op; the
 * console is explicitly allowed). The gate decision and reply
 * texts live in AdminCommands. The auto-open flag lives on the
 * player (PDC), so all three forms take an ONLINE player by
 * exact name — there is no offline form and no name-to-UUID
 * record is invented. The self-service toggle and /compass are
 * unchanged, as are the 1.4.2/1.4.3 tap and join-session fixes.
 */
public class ExcelMenuJoin extends JavaPlugin implements Listener {
    // Excel: bind delayed automatic menu opening to its exact join session.
    private record JoinTicket(Player player) {}
    private final Map<UUID, JoinTicket> pendingJoins = new HashMap<>();
    private NamespacedKey disabledKey;
    private NamespacedKey legacyDisabledKey; // menujoin:autojoin_disabled (pre-rename data)
    private NamespacedKey compassKey;
    private NamespacedKey legacyCompassKey; // menujoin:menu_compass - same as 1.1.0/1.2.0, existing compasses keep working
    private ExcelMenuJoinExpansion expansion;

    @Override
    public void onEnable() {
        migrateLegacyDataFolder("MenuJoin");
        disabledKey = new NamespacedKey(this, "autojoin_disabled");
        legacyDisabledKey = new NamespacedKey("menujoin", "autojoin_disabled");
        compassKey = new NamespacedKey(this, "menu_compass");
        legacyCompassKey = new NamespacedKey("menujoin", "menu_compass");
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("excelmenujoin").setExecutor(this::toggleCommand);
        getCommand("compass").setExecutor(this::compassCommand);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            expansion = new ExcelMenuJoinExpansion(this);
            expansion.register();
            getLogger().info("Hooked into PlaceholderAPI (%menujoin_status%).");
        }
        getLogger().info("ExcelMenuJoin enabled.");
    }

    @Override
    public void onDisable() {
        pendingJoins.clear();
        if (expansion != null) {
            try { expansion.unregister(); } catch (Exception ignored) { }
        }
    }

    /**
     * One-time rename migration: if our data folder has no files yet and the
     * legacy plugin's data folder (plugins/<legacyName>) exists, copy its
     * contents across so no config or data is lost in the rename.
     */
    private void migrateLegacyDataFolder(String legacyName) {
        try {
            File newDir = getDataFolder();
            File legacyDir = new File(newDir.getParentFile(), legacyName);
            if (!legacyDir.isDirectory() || legacyDir.equals(newDir)) {
                return;
            }
            if (newDir.isDirectory()) {
                try (var stream = Files.walk(newDir.toPath())) {
                    if (stream.anyMatch(p -> !p.equals(newDir.toPath()))) {
                        return; // new folder already has content; never overwrite
                    }
                }
            }
            final Path from = legacyDir.toPath();
            final Path to = newDir.toPath();
            try (var stream = Files.walk(from)) {
                stream.forEach(src -> {
                    try {
                        Path dst = to.resolve(from.relativize(src));
                        if (Files.isDirectory(src)) {
                            Files.createDirectories(dst);
                        } else {
                            Files.createDirectories(dst.getParent());
                            Files.copy(src, dst);
                        }
                    } catch (IOException e) {
                        getLogger().warning("Data migration copy failed for " + src + ": " + e.getMessage());
                    }
                });
            }
            getLogger().info("Migrated legacy data folder plugins/" + legacyName + " -> plugins/" + newDir.getName());
        } catch (Exception e) {
            getLogger().warning("Legacy data migration (" + legacyName + ") failed: " + e.getMessage());
        }
    }

    public boolean isDisabled(Player p) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        Boolean current = pdc.get(disabledKey, PersistentDataType.BOOLEAN);
        if (current != null) {
            return current;
        }
        // Pre-rename data: written by MenuJoin under the menujoin namespace.
        return pdc.getOrDefault(legacyDisabledKey, PersistentDataType.BOOLEAN, false);
    }

    public void setDisabled(Player p, boolean disabled) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        if (disabled) {
            // Write BOTH keys so a rollback to MenuJoin (or any legacy reader)
            // still sees the same toggle state.
            pdc.set(disabledKey, PersistentDataType.BOOLEAN, true);
            pdc.set(legacyDisabledKey, PersistentDataType.BOOLEAN, true);
        } else {
            pdc.remove(disabledKey);
            pdc.remove(legacyDisabledKey);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        boolean open = !isDisabled(p);
        JoinTicket ticket = new JoinTicket(p);
        pendingJoins.put(p.getUniqueId(), ticket);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (pendingJoins.get(p.getUniqueId()) != ticket) return;
            pendingJoins.remove(p.getUniqueId());
            if (!isEnabled() || !p.isOnline()) return;
            restampLegacyCompasses(p);
            giveMenuCompass(p);
            // Excel: a toggle switched OFF during the delay takes effect immediately.
            if (open && !isDisabled(p)) openMenuThroughCommandPolicy(p);
        }, 60L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        JoinTicket ticket = pendingJoins.get(e.getPlayer().getUniqueId());
        if (ticket != null && ticket.player() == e.getPlayer()) {
            pendingJoins.remove(e.getPlayer().getUniqueId());
        }
    }

    /** Excel: programmatic menu opening must respect the same command guards as /menu. */
    private void openMenuThroughCommandPolicy(Player p) {
        if (!isEnabled() || !p.isOnline()) return;
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(p, "/menu");
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || !isEnabled() || !p.isOnline()) return;
        String command = event.getMessage();
        // Respect a valid rewritten command, never dispatch an empty/malformed one.
        if (command == null || command.length() < 2 || command.charAt(0) != '/'
                || Character.isWhitespace(command.charAt(1))
                || command.indexOf('\n') >= 0 || command.indexOf('\r') >= 0
                || command.indexOf('\0') >= 0) return;
        p.performCommand(command.substring(1));
    }

    @EventHandler
    public void onCompassUse(PlayerInteractEvent e) {
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        if (!isMenuCompass(e.getItem())) return;
        // Respect an earlier plugin's cancellation (e.g. ExcelShops handling a
        // shop sign tap with the menu compass).
        // 1.4.2: check useItemInHand, NOT isCancelled(). isCancelled() reports
        // useInteractedBlock == DENY, and Bukkit fires every RIGHT_CLICK_AIR
        // with the block result already DENY (there is no block). So 1.3.0 -
        // 1.4.1 ignored every tap in the air: the compass only opened the menu
        // when it was tapped against a block. A plugin that cancels the event
        // with setCancelled(true) sets useItemInHand to DENY too, so the
        // shop-sign case is still respected.
        if (!CompassTap.shouldOpen(e.useItemInHand() == Event.Result.DENY)) return;
        e.setCancelled(true);
        openMenuThroughCommandPolicy(e.getPlayer());
    }

    // Give a compass back after respawn (1 tick later, once the player is in the world)
    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (p.isOnline()) giveMenuCompass(p);
        }, 1L);
    }

    // Menu compass never drops on death (other drops untouched)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        e.getDrops().removeIf(this::isMenuCompass);
        e.getItemsToKeep().removeIf(this::isMenuCompass); // keepInventory: avoid a doubled compass
    }

    // Menu compass can't be thrown out of the inventory
    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (isMenuCompass(e.getItemDrop().getItemStack())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§7Your §6Server Menu §7compass stays with you. Tap it to open the menu!");
        }
    }

    public boolean hasMenuCompass(Player p) {
        for (ItemStack it : p.getInventory().getContents()) if (isMenuCompass(it)) return true;
        return isMenuCompass(p.getItemOnCursor());
    }

    /** Returns true if a new compass was given. */
    public boolean giveMenuCompass(Player p) {
        if (hasMenuCompass(p)) return false;
        // 1.4.0: a compass parked in the Utility Pouch counts as held —
        // never duplicate it. Fail-open: no answer means give (see
        // PouchCompassCheck). ExcelPouch registers its query service
        // under its own plugin, so look the plugin up by name.
        Boolean pouchHolds = PouchCompassCheck.pouchHoldsMenuCompass(
                Bukkit.getServicesManager(),
                Bukkit.getPluginManager().getPlugin("ExcelPouch"),
                p.getUniqueId());
        if (!PouchCompassCheck.shouldGive(false, pouchHolds)) return false;
        p.getInventory().addItem(createMenuCompass()).values()
                .forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(), left));
        return true;
    }

    private ItemStack createMenuCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Server Menu");
        meta.setLore(List.of("§7Tap to open the server menu"));
        // New stamp under our own namespace, plus the legacy stamp so plugins
        // written against MenuJoin (e.g. ExcelShops' shop-sign check for
        // menujoin:menu_compass) keep recognizing the compass.
        meta.getPersistentDataContainer().set(compassKey, PersistentDataType.BOOLEAN, true);
        meta.getPersistentDataContainer().set(legacyCompassKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Restamp: any carried compass that only has the legacy menujoin stamp
     * gets the new excelmenujoin stamp added. The legacy stamp is kept (see
     * the class javadoc), so recognition never regresses for any reader.
     */
    private void restampLegacyCompasses(Player p) {
        for (ItemStack it : p.getInventory().getContents()) {
            if (it == null || it.getType() != Material.COMPASS || !it.hasItemMeta()) continue;
            ItemMeta meta = it.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            boolean legacy = pdc.getOrDefault(legacyCompassKey, PersistentDataType.BOOLEAN, false);
            boolean current = pdc.getOrDefault(compassKey, PersistentDataType.BOOLEAN, false);
            if (legacy && !current) {
                pdc.set(compassKey, PersistentDataType.BOOLEAN, true);
                it.setItemMeta(meta);
            }
        }
    }

    private boolean isMenuCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.getOrDefault(compassKey, PersistentDataType.BOOLEAN, false)
                || pdc.getOrDefault(legacyCompassKey, PersistentDataType.BOOLEAN, false);
    }

    private boolean toggleCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // 1.4.4: the admin branch routes BEFORE the player-only
        // gate, so the console (and any sender holding
        // excelmenujoin.admin) can inspect and fix another
        // player's state. The self-service form below is
        // byte-for-byte the 1.4.3 behaviour.
        if (args.length >= 1 && args[0].equalsIgnoreCase("admin")) {
            return handleAdmin(sender, args);
        }
        if (!(sender instanceof Player p)) { sender.sendMessage("Only players can use this command."); return true; }
        boolean disable = !isDisabled(p);
        setDisabled(p, disable);
        if (disable) p.sendMessage("§7The server menu will §cno longer §7open automatically when you join. Type §6/menu §7anytime to open it.");
        else p.sendMessage("§7The server menu will §aopen §7automatically when you join.");
        return true;
    }

    // ------------------------------------------------------------ admin (1.4.4)

    /**
     * /excelmenujoin admin status|toggle|compass <player> — the
     * console-admin surface (Mike's Nov 3, 2026 rule). The gate
     * is checked in code (AdminCommands.allowed: console
     * explicitly allowed, anyone else needs excelmenujoin.admin,
     * default op) because /excelmenujoin itself must stay open
     * to every player for the self-service toggle form.
     *
     * Targets resolve to ONLINE players by exact name only: the
     * state all three forms touch lives on the player — the
     * auto-open flag in their PersistentDataContainer (read and
     * written through the same isDisabled/setDisabled the self
     * toggle uses) and the compass in their live inventory,
     * given through the single giveMenuCompass path, so the
     * 1.4.0 Utility Pouch rule applies to an admin give too.
     * This plugin keeps no name-to-UUID record, so an offline
     * player cannot be addressed and none is invented. Toggle
     * and compass outcomes are logged, so the console exercise
     * leaves its own record in the server log.
     */
    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!AdminCommands.allowed(sender instanceof ConsoleCommandSender,
                sender.hasPermission(AdminCommands.PERMISSION))) {
            sender.sendMessage(AdminCommands.DENIED);
            return true;
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("status")) {
            Player target = resolveAdminTarget(sender, args[2], AdminCommands.notFoundStatus(args[2]));
            if (target == null) return true;
            sender.sendMessage(AdminCommands.statusMessage(target.getName(), target.getUniqueId(),
                    !isDisabled(target), hasMenuCompass(target)));
            return true;
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("toggle")) {
            Player target = resolveAdminTarget(sender, args[2], AdminCommands.notFoundToggle(args[2]));
            if (target == null) return true;
            boolean wasAutoOpen = !isDisabled(target);
            setDisabled(target, wasAutoOpen); // the flip: auto-open ON -> disabled, OFF -> enabled
            String reply = AdminCommands.toggleMessage(target.getName(), target.getUniqueId(), wasAutoOpen);
            sender.sendMessage(reply);
            getLogger().info("MenuJoin admin toggle by " + sender.getName() + ": " + reply);
            return true;
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("compass")) {
            Player target = resolveAdminTarget(sender, args[2], AdminCommands.notFoundCompass(args[2]));
            if (target == null) return true;
            boolean given = giveMenuCompass(target);
            String reply = given
                    ? AdminCommands.compassGivenMessage(target.getName(), target.getUniqueId())
                    : AdminCommands.compassNotGivenMessage(target.getName(), target.getUniqueId());
            sender.sendMessage(reply);
            getLogger().info("MenuJoin admin compass by " + sender.getName() + ": " + reply);
            return true;
        }
        sender.sendMessage(AdminCommands.USAGE_STATUS);
        sender.sendMessage(AdminCommands.USAGE_TOGGLE);
        sender.sendMessage(AdminCommands.USAGE_COMPASS);
        return true;
    }

    /**
     * Resolve an admin target to an ONLINE player by exact name.
     * On failure the caller-supplied rejection line is sent and
     * null returned. There is deliberately no UUID fallback:
     * every state these forms touch (PDC flag, live inventory)
     * requires the player to be online anyway.
     */
    private Player resolveAdminTarget(CommandSender sender, String target, String rejection) {
        Player online = Bukkit.getPlayerExact(target);
        if (online != null) return online;
        sender.sendMessage(rejection);
        return null;
    }

    private boolean compassCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Only players can use this command."); return true; }
        if (giveMenuCompass(p)) p.sendMessage("§aHere's a fresh §6Server Menu §acompass! Tap it to open the menu.");
        else p.sendMessage("§7You already have your §6Server Menu §7compass. Check your inventory!");
        return true;
    }
}
