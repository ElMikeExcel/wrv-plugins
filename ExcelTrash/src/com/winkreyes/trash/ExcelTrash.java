package com.winkreyes.trash;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.command.*;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ExcelTrash 0.2.0. Same plugin, same /trash and /bin.
 *  - /trash: inventory mode (4-row "Trash" screen, red trash side on top, your inventory below).
 *  - /trash chest, or the chest button in the Trash screen: chest mode for the chest you look at (6 rows:
 *    live chest on top, buttons, red trash side at the bottom). Tap a chest item to move it to the red side.
 *  - OK deletes everything on the red side. Recover (button) or /trash undo brings that whole batch back once,
 *    within 5 minutes (0.1.0 rule: only the last batch). Closing without OK gives everything back.
 * Dupe rules: the chest rows are a picture; every tap there is cancelled and done by the server on the real chest.
 * The real chest is locked (other viewers, hoppers, breaking, explosions) while its Trash screen is open.
 */
public final class ExcelTrash extends JavaPlugin implements Listener, TabExecutor {
    static final String VERSION = "0.2.0";
    private TrashStore store;
    private TrashLog log;
    private final Map<UUID, TrashHolder> sessions = new HashMap<>();
    private boolean closeDeletes;

    record Pos(UUID world, int x, int y, int z) {
        static Pos of(Location l) { return new Pos(l.getWorld().getUID(), l.getBlockX(), l.getBlockY(), l.getBlockZ()); }
        static Pos of(Block b) { return new Pos(b.getWorld().getUID(), b.getX(), b.getY(), b.getZ()); }
    }

    /** The Trash screen. Identity is checked by holder, never by title. */
    static final class TrashHolder implements InventoryHolder {
        final UUID owner; final boolean chest; Inventory inv;
        Block block; Material type; Inventory real; Set<Pos> keys = Set.of(); int page; String source = TrashStore.INV;
        long batchId; long lastButton; boolean done;
        final Map<Integer, ItemStack> origin = new HashMap<>(); // red-side slot -> exact stack we moved there from the chest
        TrashHolder(UUID owner, boolean chest) { this.owner = owner; this.chest = chest; }
        @Override public Inventory getInventory() { return inv; }
        boolean bag(int raw) { return chest ? Layout.bBag(raw) : Layout.aBag(raw); }
        int bagStart() { return chest ? Layout.B_BAG_START : 0; }
        int bagEnd() { return chest ? Layout.B_SIZE : Layout.A_BAG_END; }
    }

    @Override public void onEnable() {
        saveDefaultConfig();
        closeDeletes = "delete".equalsIgnoreCase(getConfig().getString("close-action", "return"));
        store = new TrashStore(getDataFolder().toPath().resolve("batches.dat"), System::currentTimeMillis);
        store.load();
        log = new TrashLog(getDataFolder().toPath().resolve("trash.log"));
        PluginCommand c = Objects.requireNonNull(getCommand("trash"));
        c.setExecutor(this); c.setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, () -> store.purgeExpired(), 1200L, 1200L);
        Bukkit.getScheduler().runTaskTimer(this, this::tickSessions, 10L, 10L);
        getLogger().info("ExcelTrash " + VERSION + " enabled; undo window 5 minutes; chest mode on; close-action="
                + (closeDeletes ? "delete" : "return"));
    }

    @Override public void onDisable() {
        // Server stop with a Trash screen open: give everything back first (chest items to the chest), then close.
        for (TrashHolder s : new ArrayList<>(sessions.values())) {
            Player p = Bukkit.getPlayer(s.owner);
            finish(s, p, "stop");
            if (p != null && p.getOpenInventory().getTopInventory().getHolder() == s) p.closeInventory();
        }
        sessions.clear();
        if (store != null) store.save();
    }

    // ------------------------------------------------------------------ commands
    @Override public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        String sub = args.length >= 1 ? args[0].toLowerCase(Locale.ROOT) : "";
        if (sub.equals("peek")) {
            if (!s.hasPermission("exceltrash.admin")) { s.sendMessage("No permission."); return true; }
            if (args.length < 2) { s.sendMessage("/trash peek <player>"); return true; }
            OfflinePlayer t = Bukkit.getOfflinePlayer(args[1]);
            Optional<TrashStore.Batch> b = store.peek(t.getUniqueId());
            TrashHolder open = sessions.get(t.getUniqueId());
            s.sendMessage("[ExcelTrash] peek " + args[1] + ": " + b.map(x -> "batch id=" + x.id() + " stacks=" + x.entries().size()
                    + " items=" + count(x.entries()) + " chest=" + x.entries().stream().filter(TrashStore::isChest).count()
                    + " source=" + x.source() + " left=" + store.secondsLeft(t.getUniqueId()) + "s").orElse("none")
                    + (open == null ? " screen=closed" : " screen=" + (open.chest ? "chest" : "inventory") + " redside=" + bagItems(open)));
            return true;
        }
        if (!(s instanceof Player p)) { s.sendMessage("Players only."); return true; }
        if (sub.equals("undo")) { undo(p, null); return true; }
        if (sub.equals("chest")) { openChest(p); return true; }
        openInv(p);
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (a.length != 1) return List.of();
        List<String> o = new ArrayList<>(List.of("undo", "chest"));
        if (s.hasPermission("exceltrash.admin")) o.add("peek");
        return o.stream().filter(x -> x.startsWith(a[0].toLowerCase(Locale.ROOT))).toList();
    }

    // ------------------------------------------------------------------ opening
    void openInv(Player p) {
        TrashHolder s = new TrashHolder(p.getUniqueId(), false);
        s.inv = Bukkit.createInventory(s, Layout.A_SIZE, Component.text("Trash: put items here, press OK"));
        drawButtons(s, p);
        sessions.put(p.getUniqueId(), s);
        p.openInventory(s.inv);
        Bukkit.getPluginManager().callEvent(new TrashOpenEvent(p, "inventory"));
    }

    static boolean supported(InventoryType t) {
        return t == InventoryType.CHEST || t == InventoryType.BARREL || t == InventoryType.SHULKER_BOX
                || t == InventoryType.HOPPER || t == InventoryType.DISPENSER || t == InventoryType.DROPPER;
    }

    boolean openChest(Player p) {
        Block b = p.getTargetBlockExact(5);
        Inventory real = b == null ? null : live(b);
        if (real == null || !supported(real.getType())) {
            say(p, "Look at a chest, barrel, shulker box or hopper (up to 5 blocks away), then try again.", NamedTextColor.YELLOW);
            return false;
        }
        if (b.getState(false) instanceof Lockable lk && lk.isLocked()) { say(p, "That chest is locked.", NamedTextColor.RED); return false; }
        Set<Pos> keys = keys(real);
        for (TrashHolder o : sessions.values()) if (!o.owner.equals(p.getUniqueId()) && !Collections.disjoint(o.keys, keys)) {
            say(p, "Someone else is using Trash on that chest right now.", NamedTextColor.RED); return false;
        }
        for (HumanEntity v : real.getViewers()) if (!v.getUniqueId().equals(p.getUniqueId())) {
            say(p, "Someone else has that chest open. Try again when they close it.", NamedTextColor.RED); return false;
        }
        InventoryView before = p.getOpenInventory();
        if (!access(p, b)) { say(p, "You can't use that chest here.", NamedTextColor.RED); return false; }
        InventoryView after = p.getOpenInventory();
        if (after.getTopInventory() != before.getTopInventory()) return false; // another plugin (a shop) answered the tap instead
        real = live(b);
        if (real == null) return false;
        TrashHolder s = new TrashHolder(p.getUniqueId(), true);
        s.block = b; s.type = b.getType(); s.real = real; s.keys = keys(real);
        s.source = b.getWorld().getName() + ";" + b.getX() + ";" + b.getY() + ";" + b.getZ();
        s.inv = Bukkit.createInventory(s, Layout.B_SIZE, Component.text("Trash from chest: tap items to trash"));
        render(s); drawButtons(s, p);
        sessions.put(p.getUniqueId(), s);
        p.openInventory(s.inv);
        Bukkit.getPluginManager().callEvent(new TrashOpenEvent(p, "chest"));
        return true;
    }

    /** GriefPrevention (and any other protection) answers a real right-click on the block. */
    static boolean access(Player p, Block b) {
        PlayerInteractEvent ev = new PlayerInteractEvent(p, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, null, b, BlockFace.UP, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(ev);
        return ev.useInteractedBlock() != Event.Result.DENY;
    }

    static Inventory live(Block b) {
        if (b == null) return null;
        BlockState st = b.getState(false);
        return st instanceof Container c ? c.getInventory() : null;
    }

    static Set<Pos> keys(Inventory inv) {
        if (inv == null) return Set.of();
        if (inv instanceof DoubleChestInventory d) {
            Set<Pos> s = new HashSet<>();
            Location l = d.getLeftSide().getLocation(), r = d.getRightSide().getLocation();
            if (l != null) s.add(Pos.of(l)); if (r != null) s.add(Pos.of(r));
            return s;
        }
        if (inv.getHolder(false) instanceof TrashHolder) return Set.of();
        Location l = inv.getLocation();
        return l == null || l.getWorld() == null ? Set.of() : Set.of(Pos.of(l));
    }

    // ------------------------------------------------------------------ drawing
    private static ItemStack button(Material m, String name, NamedTextColor col, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(name, col));
        if (lore.length > 0) meta.lore(Arrays.stream(lore).map(x -> (Component) Component.text(x, NamedTextColor.GRAY)).toList());
        it.setItemMeta(meta);
        return it;
    }

    void drawButtons(TrashHolder s, Player p) {
        ItemStack red = button(Material.RED_STAINED_GLASS_PANE, " ", NamedTextColor.RED);
        long left = store.secondsLeft(p.getUniqueId());
        ItemStack undo = button(Material.LIME_DYE, "Recover last trash", NamedTextColor.GREEN,
                store.has(p.getUniqueId()) ? "Gives back what you last deleted (" + left / 60 + "m " + left % 60 + "s left)" : "Nothing to recover");
        ItemStack ok = button(Material.RED_CONCRETE, "OK, delete the red side", NamedTextColor.RED,
                "Deletes everything on the red side.", "Changed your mind? Recover within 5 min.");
        ItemStack cancel = button(Material.BARRIER, "Cancel", NamedTextColor.YELLOW, "Close and give everything back");
        if (!s.chest) {
            for (int i = Layout.A_BAG_END; i < Layout.A_SIZE; i++) s.inv.setItem(i, red);
            s.inv.setItem(Layout.A_CHEST, button(Material.CHEST, "Trash from a chest", NamedTextColor.GOLD,
                    "Look at a chest first, then tap here.", "Shows that chest next to a trash bag."));
            s.inv.setItem(Layout.A_UNDO, undo); s.inv.setItem(Layout.A_OK, ok); s.inv.setItem(Layout.A_CANCEL, cancel);
            return;
        }
        for (int i = Layout.B_MIRROR; i < Layout.B_BAG_START; i++) s.inv.setItem(i, red);
        int pages = Layout.pages(s.real == null ? 27 : s.real.getSize());
        if (pages > 1) {
            s.inv.setItem(Layout.B_PREV, button(Material.ARROW, "Page " + Math.max(1, s.page) + " of " + pages, NamedTextColor.AQUA, "Previous page"));
            s.inv.setItem(Layout.B_NEXT, button(Material.ARROW, "Page " + Math.min(pages, s.page + 2) + " of " + pages, NamedTextColor.AQUA, "Next page"));
        }
        s.inv.setItem(Layout.B_INFO, button(s.type == null ? Material.CHEST : s.type, "Chest above, page " + (s.page + 1) + " of " + pages, NamedTextColor.GOLD,
                "Tap a chest item to move it to the red side.", "Tap a red side item, then a chest slot, to put it back."));
        s.inv.setItem(Layout.B_INV, button(Material.PLAYER_HEAD, "Trash from my inventory", NamedTextColor.GOLD, "Switch to the inventory trash"));
        s.inv.setItem(Layout.B_UNDO, undo); s.inv.setItem(Layout.B_OK, ok); s.inv.setItem(Layout.B_CANCEL, cancel);
    }

    /** Refresh the chest picture from the real chest. */
    void render(TrashHolder s) {
        if (!s.chest || s.real == null) return;
        ItemStack unused = button(Material.BLACK_STAINED_GLASS_PANE, " ", NamedTextColor.DARK_GRAY);
        int size = s.real.getSize();
        for (int i = 0; i < Layout.B_MIRROR; i++) {
            int ri = Layout.real(s.page, i, size);
            ItemStack want = ri < 0 ? unused : s.real.getItem(ri);
            ItemStack have = s.inv.getItem(i);
            if (Objects.equals(empty(want) ? null : want, empty(have) ? null : have)) continue;
            s.inv.setItem(i, empty(want) ? null : want.clone());
        }
    }

    static boolean empty(ItemStack it) { return it == null || it.getType().isAir() || it.getAmount() <= 0; }

    // ------------------------------------------------------------------ session upkeep
    void tickSessions() {
        for (TrashHolder s : new ArrayList<>(sessions.values())) {
            Player p = Bukkit.getPlayer(s.owner);
            if (p == null || p.getOpenInventory().getTopInventory().getHolder() != s) {
                if (!s.done) finish(s, p, "lost");
                continue;
            }
            if (s.chest && !validate(s, p)) { p.closeInventory(); continue; }
            if (s.chest) render(s);
        }
    }

    /** Chest still there, same kind, same size, loaded, and the player is near it. Re-fetches the live inventory. */
    boolean validate(TrashHolder s, Player p) {
        Block b = s.block;
        if (b == null || !b.getWorld().isChunkLoaded(b.getX() >> 4, b.getZ() >> 4)) return false;
        if (b.getType() != s.type) return false;
        Inventory real = live(b);
        if (real == null || s.real == null || real.getSize() != s.real.getSize()) return false;
        s.real = real;
        return p != null && p.getWorld().equals(b.getWorld()) && p.getLocation().distanceSquared(b.getLocation().add(0.5, 0.5, 0.5)) <= 64;
    }

    // ------------------------------------------------------------------ clicks
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder(false) instanceof TrashHolder s)) return;
        if (!(e.getWhoClicked() instanceof Player p)) { e.setCancelled(true); return; }
        int raw = e.getRawSlot();
        int top = s.inv.getSize();
        if (s.done) { e.setCancelled(true); return; }
        if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR) { e.setCancelled(true); return; } // never gather from the chest picture
        if (raw < 0 || raw >= top) { // your own inventory, or outside the window
            if (raw >= top && e.isShiftClick()) { e.setCancelled(true); e.setCurrentItem(intoBag(s, e.getCurrentItem())); }
            return;
        }
        if (s.bag(raw)) {
            if (e.getClick() == ClickType.DOUBLE_CLICK) { e.setCancelled(true); return; }
            s.origin.remove(raw); // an item you pick up or swap here is in your hands now; it is no longer marked as the chest's
            return; // red side behaves like a normal chest
        }
        e.setCancelled(true);
        if (s.chest ? Layout.bButtons(raw) : Layout.aButtons(raw)) { button(s, p, raw); return; }
        if (s.chest) chestClick(s, p, raw, e);
    }

    private ItemStack intoBag(TrashHolder s, ItemStack cur) {
        if (empty(cur)) return cur;
        ItemStack left = cur.clone();
        for (int pass = 0; pass < 2 && left.getAmount() > 0; pass++) // merge first, then empty slots
            for (int i = s.bagStart(); i < s.bagEnd() && left.getAmount() > 0; i++) {
                ItemStack slot = s.inv.getItem(i);
                if (pass == 1 && empty(slot)) { s.inv.setItem(i, left.clone()); left.setAmount(0); }
                else if (pass == 0 && !empty(slot) && slot.isSimilar(left) && slot.getAmount() < slot.getMaxStackSize() && !s.origin.containsKey(i)) {
                    int mv = Math.min(slot.getMaxStackSize() - slot.getAmount(), left.getAmount());
                    slot.setAmount(slot.getAmount() + mv); left.setAmount(left.getAmount() - mv);
                }
            }
        return left.getAmount() > 0 ? left : null;
    }

    void chestClick(TrashHolder s, Player p, int raw, InventoryClickEvent e) {
        ClickType ct = e.getClick();
        if (ct != ClickType.LEFT && ct != ClickType.RIGHT && ct != ClickType.SHIFT_LEFT && ct != ClickType.SHIFT_RIGHT) return;
        if (!validate(s, p)) { Bukkit.getScheduler().runTask(this, () -> p.closeInventory()); return; }
        int ri = Layout.real(s.page, raw, s.real.getSize());
        if (ri < 0) return;
        ItemStack cursor = e.getView().getCursor();
        ItemStack realItem = s.real.getItem(ri);
        if (!empty(cursor)) { // put a red side item back into the chest
            if (s.real.getType() == InventoryType.SHULKER_BOX && Tag.SHULKER_BOXES.isTagged(cursor.getType())) {
                say(p, "A shulker box can't go inside a shulker box.", NamedTextColor.RED); return;
            }
            if (empty(realItem)) { s.real.setItem(ri, cursor.clone()); e.getView().setCursor(null); }
            else if (realItem.isSimilar(cursor) && realItem.getAmount() < realItem.getMaxStackSize()) {
                int mv = Math.min(realItem.getMaxStackSize() - realItem.getAmount(), cursor.getAmount());
                ItemStack merged = realItem.clone(); merged.setAmount(realItem.getAmount() + mv);
                s.real.setItem(ri, merged);
                ItemStack rest = cursor.clone(); rest.setAmount(cursor.getAmount() - mv);
                e.getView().setCursor(rest.getAmount() > 0 ? rest : null);
            } else say(p, "That chest slot is taken. Tap an empty one.", NamedTextColor.YELLOW);
            render(s);
            return;
        }
        if (empty(realItem)) return;
        int free = -1;
        for (int i = Layout.B_BAG_START; i < Layout.B_SIZE; i++) if (empty(s.inv.getItem(i))) { free = i; break; }
        if (free < 0) { say(p, "The red side is full. Press OK to delete it, then keep going.", NamedTextColor.YELLOW); return; }
        ItemStack moving = realItem.clone();
        s.real.setItem(ri, null);           // out of the real chest ...
        s.inv.setItem(free, moving);        // ... and onto the red side, in the same server step
        s.origin.put(free, moving.clone());
        render(s);
    }

    void button(TrashHolder s, Player p, int raw) {
        long now = System.currentTimeMillis();
        if (now - s.lastButton < 400) return; // fast double taps count once
        s.lastButton = now;
        if (s.chest) {
            switch (raw) {
                case Layout.B_OK -> commit(s, p);
                case Layout.B_UNDO -> { undo(p, s); drawButtons(s, p); }
                case Layout.B_CANCEL -> Bukkit.getScheduler().runTask(this, () -> p.closeInventory());
                case Layout.B_PREV, Layout.B_NEXT -> {
                    int pages = Layout.pages(s.real.getSize());
                    s.page = Math.floorMod(s.page + (raw == Layout.B_NEXT ? 1 : -1), pages);
                    render(s); drawButtons(s, p);
                }
                case Layout.B_INV -> switchMode(s, p, false);
                default -> { }
            }
        } else {
            switch (raw) {
                case Layout.A_OK -> commit(s, p);
                case Layout.A_UNDO -> { undo(p, s); drawButtons(s, p); }
                case Layout.A_CANCEL -> Bukkit.getScheduler().runTask(this, () -> p.closeInventory());
                case Layout.A_CHEST -> switchMode(s, p, true);
                default -> { }
            }
        }
    }

    void switchMode(TrashHolder s, Player p, boolean toChest) {
        if (bagItems(s) > 0) { say(p, "Press OK or take your items off the red side first.", NamedTextColor.YELLOW); return; }
        Bukkit.getScheduler().runTask(this, () -> {
            if (!p.isOnline() || p.getOpenInventory().getTopInventory().getHolder() != s) return;
            if (toChest) {
                Block b = p.getTargetBlockExact(5);
                Inventory r = b == null ? null : live(b);
                if (r == null || !supported(r.getType())) { say(p, "Look at a chest first (up to 5 blocks away), then tap the chest button.", NamedTextColor.YELLOW); return; }
                finish(s, p, "switch"); if (!openChest(p)) p.closeInventory();
            } else { finish(s, p, "switch"); openInv(p); }
        });
    }

    // ------------------------------------------------------------------ drags, hoppers, protection
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getView().getTopInventory().getHolder(false) instanceof TrashHolder s)) return;
        int top = s.inv.getSize();
        for (int raw : e.getRawSlots()) if (raw < top && !s.bag(raw)) { e.setCancelled(true); return; }
        for (int raw : e.getRawSlots()) s.origin.remove(raw);
    }

    private TrashHolder lockOf(Set<Pos> k, UUID except) {
        for (TrashHolder s : sessions.values()) if (!s.done && s.chest && (except == null || !s.owner.equals(except)) && !Collections.disjoint(s.keys, k)) return s;
        return null;
    }
    private boolean anyLock() { for (TrashHolder s : sessions.values()) if (s.chest && !s.done) return true; return false; }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent e) {
        if (!anyLock() || e.getInventory().getHolder(false) instanceof TrashHolder) return;
        if (lockOf(keys(e.getInventory()), e.getPlayer().getUniqueId()) != null) {
            e.setCancelled(true);
            if (e.getPlayer() instanceof Player p) say(p, "Someone is using Trash on this chest. Try again in a moment.", NamedTextColor.YELLOW);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHopper(InventoryMoveItemEvent e) {
        if (!anyLock()) return;
        if (lockOf(keys(e.getSource()), null) != null || lockOf(keys(e.getDestination()), null) != null) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHopperPickup(InventoryPickupItemEvent e) {
        if (anyLock() && lockOf(keys(e.getInventory()), null) != null) e.setCancelled(true);
    }

    private boolean lockedBlock(Block b) { return anyLock() && lockOf(Set.of(Pos.of(b)), null) != null; }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (lockedBlock(e.getBlock())) { e.setCancelled(true); say(e.getPlayer(), "Someone is using Trash on this chest.", NamedTextColor.YELLOW); }
    }
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) { if (anyLock()) e.blockList().removeIf(this::lockedBlock); }
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) { if (anyLock()) e.blockList().removeIf(this::lockedBlock); }
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityChange(EntityChangeBlockEvent e) { if (lockedBlock(e.getBlock())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) { if (lockedBlock(e.getBlock())) e.setCancelled(true); }

    // ------------------------------------------------------------------ close, death, OK, undo
    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder(false) instanceof TrashHolder s)) return;
        if (e.getPlayer() instanceof Player p) finish(s, p, "close");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent e) {
        TrashHolder s = sessions.get(e.getPlayer().getUniqueId());
        if (s == null || s.done) return;
        // Red side items are still the player's: chest items go home, the rest join the death drops (or stay, with keepInventory).
        List<ItemStack> mine = takeBag(s, true);
        if (e.getKeepInventory()) for (ItemStack it : mine) giveOrDrop(e.getPlayer(), it);
        else e.getDrops().addAll(mine);
        s.done = true; sessions.remove(s.owner, s);
    }

    /** End of a screen: closing without OK gives everything back (or deletes, if close-action: delete). */
    void finish(TrashHolder s, Player p, String why) {
        if (s.done) return;
        s.done = true;
        sessions.remove(s.owner, s);
        if (p == null) return;
        if (closeDeletes && !why.equals("stop") && bagItems(s) > 0) { commit(s, p); return; }
        int n = 0;
        for (ItemStack it : takeBag(s, true)) { n += it.getAmount(); giveOrDrop(p, it); }
        if (n > 0 && !why.equals("switch")) say(p, "Nothing was deleted. Your " + n + " items went back. Press OK to delete.", NamedTextColor.GRAY);
    }

    /** Empties the red side. Chest-origin stacks go back to the chest when possible (returnChest). Returns what is left for the player. */
    List<ItemStack> takeBag(TrashHolder s, boolean returnChest) {
        List<ItemStack> out = new ArrayList<>();
        Player p = Bukkit.getPlayer(s.owner);
        boolean chestOk = returnChest && s.chest && validateLoose(s);
        for (int i = s.bagStart(); i < s.bagEnd(); i++) {
            ItemStack it = s.inv.getItem(i);
            s.inv.setItem(i, null);
            if (empty(it)) continue;
            it = it.clone();
            if (chestOk && fromChest(s, i, it)) {
                for (ItemStack over : s.real.addItem(it).values()) out.add(over);
            } else out.add(it);
        }
        s.origin.clear();
        return out;
    }

    private boolean validateLoose(TrashHolder s) {
        if (s.block == null || !s.block.getWorld().isChunkLoaded(s.block.getX() >> 4, s.block.getZ() >> 4) || s.block.getType() != s.type) return false;
        Inventory r = live(s.block); if (r == null) return false; s.real = r; return true;
    }

    private static boolean fromChest(TrashHolder s, int slot, ItemStack it) {
        ItemStack o = s.origin.get(slot);
        return o != null && o.isSimilar(it) && o.getAmount() == it.getAmount();
    }

    int bagItems(TrashHolder s) {
        int n = 0;
        for (int i = s.bagStart(); i < s.bagEnd(); i++) { ItemStack it = s.inv.getItem(i); if (!empty(it)) n += it.getAmount(); }
        return n;
    }

    /** OK: delete everything on the red side. It joins this screen's batch, which Recover or /trash undo returns once. */
    void commit(TrashHolder s, Player p) {
        List<String> entries = new ArrayList<>();
        Map<Material, Integer> counts = new TreeMap<>();
        int items = 0, fromChest = 0;
        for (int i = s.bagStart(); i < s.bagEnd(); i++) {
            ItemStack it = s.inv.getItem(i);
            if (empty(it)) continue;
            String b64 = Base64.getEncoder().encodeToString(it.serializeAsBytes());
            boolean c = s.chest && fromChest(s, i, it);
            entries.add(c ? TrashStore.chest(b64) : TrashStore.player(b64));
            if (c) fromChest += it.getAmount();
            items += it.getAmount();
            counts.merge(it.getType(), it.getAmount(), Integer::sum);
        }
        if (entries.isEmpty()) { if (!s.done) say(p, "The red side is empty. Put items there first.", NamedTextColor.YELLOW); return; }
        for (int i = s.bagStart(); i < s.bagEnd(); i++) s.inv.setItem(i, null);
        s.origin.clear();
        s.batchId = store.add(p.getUniqueId(), s.batchId, s.source, entries);
        log.write("TRASH", p.getName(), p.getUniqueId(), entries.size(), items,
                summary(counts) + " source=" + (s.chest ? "chest:" + s.source.replace(';', ',') + " fromchest=" + fromChest : "inventory") + " batch=" + s.batchId);
        say(p, "Deleted " + items + " items. Changed your mind? Tap Recover or type /trash undo within 5 minutes.", NamedTextColor.GRAY);
        if (!s.done) drawButtons(s, p);
    }

    /** One-shot recover of the last deleted batch. Chest items go back to their chest when it still exists, has room and you may use it. */
    void undo(Player p, TrashHolder open) {
        Optional<TrashStore.Batch> got = store.take(p.getUniqueId());
        if (got.isEmpty()) { say(p, "Nothing to recover (only your last trash, for 5 minutes).", NamedTextColor.RED); return; }
        TrashStore.Batch b = got.get();
        if (open != null && open.batchId == b.id()) open.batchId = 0; // next OK starts a new batch
        Inventory chest = b.fromChest() ? chestFor(p, b.source(), open) : null;
        int items = 0, dropped = 0, toChest = 0, bad = 0;
        Map<Material, Integer> counts = new TreeMap<>();
        for (String e : b.entries()) {
            ItemStack it;
            try { it = ItemStack.deserializeBytes(Base64.getDecoder().decode(TrashStore.data(e))); } catch (RuntimeException ex) { bad++; continue; }
            items += it.getAmount();
            counts.merge(it.getType(), it.getAmount(), Integer::sum);
            Collection<ItemStack> rest = List.of(it);
            if (chest != null && TrashStore.isChest(e)) {
                int before = it.getAmount();
                rest = chest.addItem(it.clone()).values();
                toChest += before - rest.stream().mapToInt(ItemStack::getAmount).sum();
            }
            for (ItemStack r : rest) dropped += giveOrDrop(p, r);
        }
        log.write("UNDO", p.getName(), p.getUniqueId(), b.entries().size(), items,
                "dropped=" + dropped + " tochest=" + toChest + (bad > 0 ? " unreadable=" + bad : "") + " " + summary(counts) + " batch=" + b.id());
        say(p, "Recovered " + items + " items" + (toChest > 0 ? " (" + toChest + " back in the chest)" : "")
                + (dropped > 0 ? " (" + dropped + " dropped at your feet)." : "."), NamedTextColor.GREEN);
        if (open != null && open.chest) render(open);
    }

    private Inventory chestFor(Player p, String source, TrashHolder open) {
        try {
            String[] s = source.split(";");
            World w = Bukkit.getWorld(s[0]);
            if (w == null) return null;
            int x = Integer.parseInt(s[1]), y = Integer.parseInt(s[2]), z = Integer.parseInt(s[3]);
            if (!w.isChunkLoaded(x >> 4, z >> 4)) return null;
            Block b = w.getBlockAt(x, y, z);
            Inventory inv = live(b);
            if (inv == null || !supported(inv.getType())) return null;
            Set<Pos> k = keys(inv);
            if (open != null && open.chest && !Collections.disjoint(open.keys, k)) return inv; // your own open screen
            if (lockOf(k, null) != null) return null;
            InventoryView before = p.getOpenInventory();
            if (!access(p, b)) return null;
            if (p.getOpenInventory().getTopInventory() != before.getTopInventory()) return null;
            return inv;
        } catch (RuntimeException e) { return null; }
    }

    /** Into the inventory; what does not fit drops at the feet. Returns how many dropped. */
    static int giveOrDrop(Player p, ItemStack it) {
        int d = 0;
        for (ItemStack over : p.getInventory().addItem(it).values()) { d += over.getAmount(); p.getWorld().dropItemNaturally(p.getLocation(), over); }
        return d;
    }

    static int count(List<String> entries) {
        int n = 0;
        for (String e : entries) try { n += ItemStack.deserializeBytes(Base64.getDecoder().decode(TrashStore.data(e))).getAmount(); } catch (RuntimeException ignored) { }
        return n;
    }

    static void say(Player p, String m, NamedTextColor c) { if (p != null) p.sendMessage(Component.text(m, c)); }

    static String summary(Map<Material, Integer> c) {
        return c.entrySet().stream().map(e -> e.getKey().name() + "x" + e.getValue()).collect(Collectors.joining(",", "[", "]"));
    }
}
