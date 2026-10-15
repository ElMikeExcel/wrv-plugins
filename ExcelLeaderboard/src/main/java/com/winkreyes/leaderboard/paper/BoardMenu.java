package com.winkreyes.leaderboard.paper;

import com.winkreyes.leaderboard.ExcelLeaderboard;
import com.winkreyes.leaderboard.core.BoardMath;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

/** WealthBoard-style chest menus: a category grid, then a board view with
 * the top 10, the viewer's own rank card, and a back button. Geyser renders
 * these as standard chest screens on Bedrock. */
public final class BoardMenu implements Listener {

    private static final class Holder implements InventoryHolder {
        final Category category; // null = main menu
        Holder(Category category) { this.category = category; }
        @Override public Inventory getInventory() { return null; }
    }

    private final ExcelLeaderboard plugin;

    public BoardMenu(ExcelLeaderboard plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player player) {
        ExcelLeaderboard.Snapshot snapshot = plugin.snapshot();
        List<Category> available = List.of(Category.values()).stream()
                .filter(c -> snapshot.boards().containsKey(c)).toList();
        Inventory inv = Bukkit.createInventory(new Holder(null), 27, "Leaderboards");
        int slot = 10;
        for (Category category : available) {
            if (slot >= 26) {
                break;
            }
            ItemStack item = new ItemStack(category.icon());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + category.title());
            List<BoardMath.Entry> board = snapshot.boards().get(category);
            if (!board.isEmpty()) {
                BoardMath.Entry leader = board.get(0);
                meta.setLore(List.of("§7Leader: §f" + leader.name(),
                        "§7" + category.formatValue(leader.value())));
            }
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            slot += (slot == 16) ? 3 : 1; // two tidy rows: 10-16, then 19-25
        }
        if (available.isEmpty()) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§7Boards are warming up…");
            meta.setLore(List.of("§7Try again in a few seconds."));
            item.setItemMeta(meta);
            inv.setItem(13, item);
        }
        player.openInventory(inv);
    }

    public void openBoard(Player player, Category category) {
        ExcelLeaderboard.Snapshot snapshot = plugin.snapshot();
        List<BoardMath.Entry> board = snapshot.boards().getOrDefault(category, List.of());
        Inventory inv = Bukkit.createInventory(new Holder(category), 27, category.title());
        List<BoardMath.Entry> top = BoardMath.top(board, 10);
        for (int i = 0; i < top.size(); i++) {
            BoardMath.Entry entry = top.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.player()));
            int rank = BoardMath.rankOf(board, entry.player());
            meta.setDisplayName("§e#" + rank + " §f" + entry.name());
            meta.setLore(List.of("§7" + category.formatValue(entry.value())));
            head.setItemMeta(meta);
            inv.setItem(i, head);
        }
        int myRank = BoardMath.rankOf(board, player.getUniqueId());
        ItemStack mine = new ItemStack(Material.NAME_TAG);
        ItemMeta mineMeta = mine.getItemMeta();
        if (myRank > 0) {
            BoardMath.Entry me = board.stream()
                    .filter(e -> e.player().equals(player.getUniqueId())).findFirst().orElseThrow();
            mineMeta.setDisplayName("§aYour rank: #" + myRank);
            mineMeta.setLore(List.of("§7" + category.formatValue(me.value())));
        } else {
            mineMeta.setDisplayName("§7Your rank: not on this board yet");
        }
        mine.setItemMeta(mineMeta);
        inv.setItem(18, mine);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§7← All boards");
        back.setItemMeta(backMeta);
        inv.setItem(26, back);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        if (holder.category == null) {
            // Main menu: identify the category by slot order from the snapshot.
            List<Category> available = List.of(Category.values()).stream()
                    .filter(c -> plugin.snapshot().boards().containsKey(c)).toList();
            int index = slotIndex(event.getRawSlot());
            if (index >= 0 && index < available.size()) {
                openBoard(player, available.get(index));
            }
        } else if (event.getRawSlot() == 26) {
            openMain(player);
        }
    }

    /** Map a raw slot in the main menu back to the category index laid out
     * by openMain (slots 10-16 = 0-6, slots 19-25 = 7-13). */
    private static int slotIndex(int rawSlot) {
        if (rawSlot >= 10 && rawSlot <= 16) {
            return rawSlot - 10;
        }
        if (rawSlot >= 19 && rawSlot <= 25) {
            return 7 + (rawSlot - 19);
        }
        return -1;
    }
}
