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
 * the top 10, the viewer's own rank card, and a back button. Since 1.2
 * this is the fallback presentation only (Bedrock players get native
 * forms, Java players the clickable chat menu); it is still reachable
 * whenever a form cannot be sent. The main grid uses the spaced
 * BoardPresentation.MAIN_SLOTS layout in a 54-slot chest. */
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
        List<Category> available = BoardPresentation.availableBoards(snapshot.boards());
        Inventory inv = Bukkit.createInventory(new Holder(null), 54, "Leaderboards");
        int shown = Math.min(available.size(), BoardPresentation.MAIN_SLOTS.length);
        for (int i = 0; i < shown; i++) {
            Category category = available.get(i);
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
            inv.setItem(BoardPresentation.slotForCategoryIndex(i), item);
        }
        if (available.isEmpty()) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§7Boards are warming up…");
            meta.setLore(List.of("§7Try again in a few seconds."));
            item.setItemMeta(meta);
            inv.setItem(22, item);
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
            // Main menu: the shared spaced layout maps the slot back to
            // the category index in available order.
            List<Category> available = BoardPresentation.availableBoards(plugin.snapshot().boards());
            int index = BoardPresentation.categoryIndexForSlot(event.getRawSlot());
            if (index >= 0 && index < available.size()) {
                openBoard(player, available.get(index));
            }
        } else if (event.getRawSlot() == 26) {
            openMain(player);
        }
    }
}
