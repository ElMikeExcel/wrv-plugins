package com.winkreyes.excelbraingames.gui;

import com.winkreyes.excelbraingames.ExcelBrainGames;
import com.winkreyes.excelbraingames.GameConfig;
import com.winkreyes.excelbraingames.GameService;
import com.winkreyes.excelbraingames.PlayerData;
import com.winkreyes.excelbraingames.games.memory.MemoryRound;
import com.winkreyes.excelbraingames.games.wordsearch.WordSearchRound;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Inventory rendering for every Brain Games screen. Grid games render in an
 * inventory GUI on BOTH platforms (SPEC §2): Bedrock sees the Geyser-
 * translated grid, which is a true 2D tap surface; native Bedrock forms
 * cannot do a 2D grid. Board controls always sit in slots 51/52/53 so the
 * layout is identical in every game.
 */
public final class Views {

    public static final int SLOT_WORDS = 51, SLOT_NEW = 52, SLOT_CLOSE = 53;

    private final ExcelBrainGames plugin;

    public Views(ExcelBrainGames plugin) { this.plugin = plugin; }

    private GameConfig cfg() { return plugin.gameConfig(); }

    // ------------------------------------------------------------ items

    static ItemStack item(Material mat, String name, String... loreLines) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(GameService.text(name));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String l : loreLines) lore.add(GameService.text(l));
        if (!lore.isEmpty()) meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        it.setItemMeta(meta);
        return it;
    }

    static ItemStack glint(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        it.setItemMeta(meta);
        return it;
    }

    static ItemStack filler() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(GameService.text(" "));
        it.setItemMeta(meta);
        return it;
    }

    private Inventory inv(BrainHolder holder, int size, String title) {
        Inventory inv = Bukkit.createInventory(holder, size, GameService.text(title));
        holder.bind(inv);
        return inv;
    }

    // ------------------------------------------------------------ picker

    public Inventory picker(Player p, PlayerData pd) {
        BrainHolder h = new BrainHolder(BrainHolder.View.PICKER);
        Inventory inv = inv(h, 27, "&5&lBrain Games");
        for (int i = 0; i < 27; i++) inv.setItem(i, filler());
        GameConfig cfg = cfg();
        String wsLore = pd.wordSearch != null ? "&eRound in progress — click to resume"
                : "&7Find the hidden words in the grid";
        inv.setItem(11, item(Material.BOOK, cfg.msg("picker-wordsearch", "&a&lWord Search"), wsLore));
        h.actions.put(11, "game:wordsearch");
        String mmLore = pd.memory != null ? "&eRound in progress — click to resume"
                : "&7Flip the cards and match the pairs";
        inv.setItem(13, item(Material.ITEM_FRAME, cfg.msg("picker-memory", "&b&lMemory Match"), mmLore));
        h.actions.put(13, "game:memory");
        inv.setItem(15, item(Material.GRAY_DYE, "&8&lSudoku", "&8Coming in Phase 2"));
        h.actions.put(15, "soon");
        inv.setItem(22, item(Material.NAME_TAG, "&8&lQuick Math · Word Scramble · Riddle", "&8Coming in Phase 3"));
        h.actions.put(22, "soon");
        return inv;
    }

    // ------------------------------------------------------------ theme / tier pickers

    public Inventory themePick(Player p) {
        BrainHolder h = new BrainHolder(BrainHolder.View.THEME_PICK);
        h.game = "wordsearch";
        Inventory inv = inv(h, 27, "&a&lWord Search &7— pick a theme");
        for (int i = 0; i < 27; i++) inv.setItem(i, filler());
        int slot = 10;
        for (GameConfig.Theme theme : cfg().themes.values()) {
            if (slot > 16) break;
            inv.setItem(slot, item(theme.icon(), "&f&l" + theme.label()));
            h.actions.put(slot, "theme:" + theme.id());
            slot++;
            if ((slot - 10) % 7 == 0) slot += 2;
        }
        inv.setItem(22, item(Material.ARROW, "&7Back"));
        h.actions.put(22, "back:picker");
        return inv;
    }

    public Inventory tierPick(Player p, String game, String themeId) {
        BrainHolder h = new BrainHolder(BrainHolder.View.TIER_PICK);
        h.game = game;
        String title = game.equals("wordsearch") ? "&a&lWord Search &7— pick a size" : "&b&lMemory Match &7— pick a size";
        Inventory inv = inv(h, 27, title);
        for (int i = 0; i < 27; i++) inv.setItem(i, filler());
        boolean kid = plugin.gameConfig().isKid(p.getUniqueId());
        int slot = 10;
        if (game.equals("wordsearch")) {
            for (GameConfig.WsTier tier : cfg().wsTiers.values()) {
                if (slot > 16) break;
                inv.setItem(slot, tierItem(tier.label(), com.winkreyes.excelbraingames.core.Money.fmt(tier.payoutCents()),
                        tier.kid(), kid));
                if (tierAvailableWs(tier, themeId)) h.actions.put(slot, "tier:wordsearch:" + tier.id() + ":" + themeId);
                slot++;
            }
        } else {
            for (GameConfig.MemTier tier : cfg().memTiers.values()) {
                if (slot > 16) break;
                inv.setItem(slot, tierItem(tier.label(), com.winkreyes.excelbraingames.core.Money.fmt(tier.payoutCents()),
                        tier.kid(), kid));
                if (tierAvailableMem(tier)) h.actions.put(slot, "tier:memory:" + tier.id());
                slot++;
            }
        }
        inv.setItem(22, item(Material.ARROW, "&7Back"));
        h.actions.put(22, "back:picker");
        return inv;
    }

    private ItemStack tierItem(String label, String payout, boolean kidTier, boolean kidFlagged) {
        GameConfig cfg = cfg();
        if (kidTier && !cfg.kidTiersEnabled) {
            return item(Material.GRAY_DYE, "&8&l" + label,
                    "&8Kid tier — opens after the", "&8word lists are hand-checked");
        }
        if (kidTier && !kidFlagged) {
            return item(Material.PAPER, "&e&l" + label,
                    "&7Pays " + payout, "&6Kid tier: grown-ups earn half");
        }
        return item(Material.PAPER, "&f&l" + label, "&7Pays " + payout);
    }

    private boolean tierAvailableWs(GameConfig.WsTier tier, String themeId) {
        if (tier.kid() && !cfg().kidTiersEnabled) return false;
        return plugin.wordLists().hasPool(themeId, tier.id());
    }

    private boolean tierAvailableMem(GameConfig.MemTier tier) {
        return !(tier.kid() && !cfg().kidTiersEnabled);
    }

    // ------------------------------------------------------------ resume prompt

    public Inventory resume(Player p, String game, boolean armed) {
        BrainHolder h = new BrainHolder(BrainHolder.View.RESUME);
        h.game = game;
        h.armed = armed;
        String name = game.equals("wordsearch") ? "Word Search" : "Memory Match";
        Inventory inv = inv(h, 27, "&e&l" + name + " &7— round in progress");
        for (int i = 0; i < 27; i++) inv.setItem(i, filler());
        inv.setItem(11, item(Material.LIME_DYE, "&a&lResume my round", "&7Pick up right where you left off"));
        h.actions.put(11, "resume:" + game);
        if (armed) {
            inv.setItem(15, glint(item(Material.RED_DYE, "&c&lClick again to discard",
                    "&cThe unfinished round pays nothing")));
        } else {
            inv.setItem(15, item(Material.YELLOW_DYE, "&e&lStart a new round",
                    "&7Discards the unfinished round", "&7(it pays nothing)"));
        }
        h.actions.put(15, "discard:" + game);
        inv.setItem(22, item(Material.ARROW, "&7Back"));
        h.actions.put(22, "back:picker");
        return inv;
    }

    // ------------------------------------------------------------ Word Search board

    public Inventory wsBoard(Player p, WordSearchRound round) {
        BrainHolder h = new BrainHolder(BrainHolder.View.WS_BOARD);
        h.game = "wordsearch";
        GameConfig.Theme theme = cfg().themes.get(round.themeId());
        String title = "&a&lWord Search &7— " + (theme == null ? round.themeId() : theme.label());
        Inventory inv = inv(h, 54, title);
        renderWs(inv, h, round);
        return inv;
    }

    public void renderWs(Inventory inv, BrainHolder h, WordSearchRound round) {
        h.actions.clear();
        int cols = round.cols(), rows = round.rows();
        for (int slot = 0; slot < 54; slot++) {
            if (slot == SLOT_WORDS || slot == SLOT_NEW || slot == SLOT_CLOSE) continue;
            int r = slot / 9, c = slot % 9;
            if (r < rows && c < cols) {
                int idx = r * cols + c;
                if (round.isVoid(idx)) { inv.setItem(slot, filler()); continue; }
                char letter = round.letterAt(idx);
                boolean foundCell = round.cellFound(idx);
                boolean pendingCell = round.pending() == idx;
                if (foundCell) {
                    inv.setItem(slot, item(Material.LIME_STAINED_GLASS_PANE, "&a&l" + letter));
                } else if (pendingCell) {
                    inv.setItem(slot, item(Material.YELLOW_STAINED_GLASS_PANE, "&e&l" + letter,
                            "&7First letter picked —", "&7now tap the last letter"));
                } else {
                    inv.setItem(slot, item(Material.PAPER, "&f&l" + letter));
                }
                h.actions.put(slot, "cell:" + idx);
            } else {
                inv.setItem(slot, filler());
            }
        }
        inv.setItem(SLOT_WORDS, item(Material.BOOK, "&f&lWords: " + round.found().size() + " / " + round.placements().size(),
                "&7Click to see the word list"));
        h.actions.put(SLOT_WORDS, "ctl:words");
        inv.setItem(SLOT_NEW, item(Material.COMPASS, "&e&lNew game", "&7Leave this round and start over"));
        h.actions.put(SLOT_NEW, "ctl:new");
        inv.setItem(SLOT_CLOSE, item(Material.BARRIER, "&c&lClose", "&7Your round is saved — resume any time"));
        h.actions.put(SLOT_CLOSE, "ctl:close");
    }

    public Inventory wsWords(Player p, WordSearchRound round) {
        BrainHolder h = new BrainHolder(BrainHolder.View.WS_WORDS);
        h.game = "wordsearch";
        Inventory inv = inv(h, 54, "&a&lWord Search &7— the words");
        for (int i = 0; i < 54; i++) inv.setItem(i, filler());
        int slot = 0;
        for (int i = 0; i < round.placements().size() && slot < 45; i++) {
            WordSearchRound.Placement pl = round.placements().get(i);
            boolean foundIt = round.found().contains(i);
            inv.setItem(slot, foundIt
                    ? glint(item(Material.LIME_DYE, "&a&m" + pl.word, "&aFound!"))
                    : item(Material.PAPER, "&f&l" + pl.word, "&7Still hiding in the grid"));
            slot++;
        }
        inv.setItem(49, item(Material.ARROW, "&f&lBack to the grid"));
        h.actions.put(49, "back:board");
        return inv;
    }

    // ------------------------------------------------------------ Memory board

    public Inventory memBoard(Player p, MemoryRound round) {
        BrainHolder h = new BrainHolder(BrainHolder.View.MEMORY_BOARD);
        h.game = "memory";
        Inventory inv = inv(h, 54, "&b&lMemory Match");
        renderMem(inv, h, round);
        return inv;
    }

    public void renderMem(Inventory inv, BrainHolder h, MemoryRound round) {
        h.actions.clear();
        List<Material> symbols = cfg().memorySymbols;
        for (int slot = 0; slot < 54; slot++) {
            if (slot == SLOT_WORDS || slot == SLOT_NEW || slot == SLOT_CLOSE) continue;
            if (slot < round.cardCount()) {
                byte state = round.stateAt(slot);
                if (state == MemoryRound.DOWN) {
                    inv.setItem(slot, item(Material.BLUE_STAINED_GLASS_PANE, "&7&l?", "&7Tap to flip"));
                } else {
                    Material sym = symbols.get(round.symbolAt(slot));
                    ItemStack card = item(sym, "&f&l" + pretty(sym));
                    if (state == MemoryRound.MATCHED) card = glint(card);
                    inv.setItem(slot, card);
                }
                h.actions.put(slot, "card:" + slot);
            } else {
                inv.setItem(slot, filler());
            }
        }
        inv.setItem(SLOT_WORDS, item(Material.PAPER, "&f&lFlips: " + round.flips()
                + " · Pairs: " + round.matchedPairs() + " / " + round.pairs()));
        inv.setItem(SLOT_NEW, item(Material.COMPASS, "&e&lNew game", "&7Leave this round and start over"));
        h.actions.put(SLOT_NEW, "ctl:new");
        inv.setItem(SLOT_CLOSE, item(Material.BARRIER, "&c&lClose", "&7Your round is saved — resume any time"));
        h.actions.put(SLOT_CLOSE, "ctl:close");
    }

    static String pretty(Material m) {
        String s = m.name().toLowerCase().replace('_', ' ');
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
