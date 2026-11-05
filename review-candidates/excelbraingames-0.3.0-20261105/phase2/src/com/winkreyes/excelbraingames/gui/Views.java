package com.winkreyes.excelbraingames.gui;

import com.winkreyes.excelbraingames.ExcelBrainGames;
import com.winkreyes.excelbraingames.GameConfig;
import com.winkreyes.excelbraingames.GameService;
import com.winkreyes.excelbraingames.PlayerData;
import com.winkreyes.excelbraingames.games.memory.MemoryRound;
import com.winkreyes.excelbraingames.games.sudoku.SudokuRound;
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
        String sdLore = pd.sudoku != null ? "&eRound in progress — click to resume"
                : "&7The number puzzle — fresh every round";
        inv.setItem(15, item(Material.WRITABLE_BOOK, cfg.msg("picker-sudoku", "&d&lSudoku"), sdLore));
        h.actions.put(15, "game:sudoku");
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
        String title = switch (game) {
            case "wordsearch" -> "&a&lWord Search &7— pick a size";
            case "sudoku" -> "&d&lSudoku &7— pick a size";
            default -> "&b&lMemory Match &7— pick a size";
        };
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
        } else if (game.equals("sudoku")) {
            for (GameConfig.SudTier tier : cfg().sudokuTiers.values()) {
                if (slot > 17) break;
                inv.setItem(slot, tierItem(tier.label(), com.winkreyes.excelbraingames.core.Money.fmt(tier.payoutCents()),
                        tier.kid(), kid));
                if (tierAvailableSudoku(tier)) {
                    h.actions.put(slot, "tier:sudoku:" + tier.id() + ":num");
                    if (tier.pictureCapable() && cfg().sudokuPictureSymbols.size() >= tier.size() && slot + 1 <= 17) {
                        slot++;
                        inv.setItem(slot, tierItem(tier.label() + " — Pictures",
                                com.winkreyes.excelbraingames.core.Money.fmt(tier.payoutCents()),
                                tier.kid(), kid));
                        h.actions.put(slot, "tier:sudoku:" + tier.id() + ":pic");
                    }
                }
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

    private boolean tierAvailableSudoku(GameConfig.SudTier tier) {
        return !(tier.kid() && !cfg().kidTiersEnabled);
    }

    // ------------------------------------------------------------ resume prompt

    public Inventory resume(Player p, String game, boolean armed) {
        BrainHolder h = new BrainHolder(BrainHolder.View.RESUME);
        h.game = game;
        h.armed = armed;
        String name = switch (game) {
            case "wordsearch" -> "Word Search";
            case "sudoku" -> "Sudoku";
            default -> "Memory Match";
        };
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

    // ------------------------------------------------------------ Sudoku board

    /**
     * Sudoku rendering (SPEC §4.4). Palette-first: the palette is part of
     * every board view; the player picks a digit (or the eraser), then taps
     * cells. Layouts per size, all inside the 54-slot GUI:
     *  - 4x4: board rows 0-3 at the top, palette row beneath it (digits,
     *    eraser, check), controls in the standard 51/52/53 slots.
     *  - 6x6: board takes all six rows on the left; palette rides the
     *    right rail (digits, eraser, check), controls at 51/52/53.
     *  - 9x9: two views (rows 1-5 / rows 5-9, row 5 the shared seam). The
     *    bottom row is the palette: five digit slots + a bank switch
     *    (digits 1-5 / 6-9), eraser, check, and the view toggle — 57
     *    tappables (45 cells + 9 digits + eraser + check + page) cannot
     *    fit 54 slots alongside the standard New/Close controls, so on
     *    9x9 Close is the native GUI close (the round auto-saves, same as
     *    the Close button) and New Game is reached through the picker /
     *    resume view. Deviation recorded in TEST-REPORT.md.
     */
    public Inventory sudokuBoard(Player p, SudokuRound round) {
        BrainHolder h = new BrainHolder(BrainHolder.View.SUDOKU_BOARD);
        h.game = "sudoku";
        GameConfig.SudTier tier = cfg().sudokuTiers.get(round.tierId());
        String title = "&d&lSudoku &7— " + (tier == null ? round.tierId() : tier.label())
                + (round.picture() ? " &7(Pictures)" : "");
        Inventory inv = inv(h, 54, title);
        renderSudoku(inv, h, round);
        return inv;
    }

    public void renderSudoku(Inventory inv, BrainHolder h, SudokuRound round) {
        h.actions.clear();
        for (int slot = 0; slot < 54; slot++) inv.setItem(slot, filler());
        int n = round.size();
        boolean wrongActive = System.currentTimeMillis() < h.sudokuWrongUntil;
        int rowFrom = n == 9 && h.sudokuPage == 1 ? 4 : 0;
        int rowTo = n == 9 ? rowFrom + 4 : n - 1;
        for (int r = rowFrom; r <= rowTo; r++) {
            for (int c = 0; c < n; c++) {
                int idx = r * n + c;
                int slot = (r - rowFrom) * 9 + c;
                inv.setItem(slot, sudokuCell(round, idx, wrongActive && h.sudokuWrong.contains(idx)));
                h.actions.put(slot, "scell:" + idx);
            }
        }
        if (n == 9) {
            renderSudokuPalette9(inv, h, round);
        } else if (n == 6) {
            int[] digitSlots = {6, 7, 8, 15, 16, 17};
            for (int d = 1; d <= 6; d++) {
                inv.setItem(digitSlots[d - 1], paletteDigit(round, d, h.sudokuSel == d));
                h.actions.put(digitSlots[d - 1], "spal:" + d);
            }
            inv.setItem(24, eraserItem(h.sudokuSel == -1));
            h.actions.put(24, "spal:erase");
            inv.setItem(25, checkItem(round));
            h.actions.put(25, "spal:check");
            renderSudokuControls(inv, h, round);
        } else { // 4x4: palette row directly under the board
            for (int d = 1; d <= 4; d++) {
                int slot = 36 + (d - 1);
                inv.setItem(slot, paletteDigit(round, d, h.sudokuSel == d));
                h.actions.put(slot, "spal:" + d);
            }
            inv.setItem(40, eraserItem(h.sudokuSel == -1));
            h.actions.put(40, "spal:erase");
            inv.setItem(41, checkItem(round));
            h.actions.put(41, "spal:check");
            renderSudokuControls(inv, h, round);
        }
    }

    private void renderSudokuControls(Inventory inv, BrainHolder h, SudokuRound round) {
        inv.setItem(SLOT_WORDS, item(Material.PAPER, "&f&lFilled: " + round.filledCount() + " / " + (round.size() * round.size()),
                "&7Checks used: " + round.checksUsed()));
        inv.setItem(SLOT_NEW, item(Material.COMPASS, "&e&lNew game", "&7Leave this round and start over"));
        h.actions.put(SLOT_NEW, "ctl:new");
        inv.setItem(SLOT_CLOSE, item(Material.BARRIER, "&c&lClose", "&7Your round is saved — resume any time"));
        h.actions.put(SLOT_CLOSE, "ctl:close");
    }

    private void renderSudokuPalette9(Inventory inv, BrainHolder h, SudokuRound round) {
        int first = h.sudokuBankHigh ? 6 : 1;
        int count = h.sudokuBankHigh ? 4 : 5;
        for (int i = 0; i < count; i++) {
            int d = first + i;
            inv.setItem(45 + i, paletteDigit(round, d, h.sudokuSel == d));
            h.actions.put(45 + i, "spal:" + d);
        }
        inv.setItem(50, item(Material.ARROW, h.sudokuBankHigh ? "&f&lNumbers 1–5" : "&f&lNumbers 6–9",
                "&7Switch the palette numbers"));
        h.actions.put(50, "spal:bank");
        inv.setItem(51, eraserItem(h.sudokuSel == -1));
        h.actions.put(51, "spal:erase");
        inv.setItem(52, checkItem(round));
        h.actions.put(52, "spal:check");
        inv.setItem(53, item(Material.COMPASS, h.sudokuPage == 0 ? "&f&lRows 5–9 ▼" : "&f&lRows 1–5 ▲",
                "&7Flip the board view (row 5 is on both)"));
        h.actions.put(53, "spal:page");
    }

    private ItemStack sudokuCell(SudokuRound round, int idx, boolean wrong) {
        int v = round.valueAt(idx);
        if (wrong) {
            return item(Material.RED_STAINED_GLASS_PANE, "&c&l" + symbolName(round, v), "&cThat one isn't right");
        }
        if (v == 0) {
            ItemStack it = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = it.getItemMeta();
            meta.displayName(GameService.text(" "));
            it.setItemMeta(meta);
            return it;
        }
        if (round.isGiven(idx)) {
            return item(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "&9&l" + symbolName(round, v),
                    "&7Given — locked");
        }
        return digitItem(round, v, false);
    }

    private ItemStack paletteDigit(SudokuRound round, int digit, boolean selected) {
        ItemStack it = digitItem(round, digit, true);
        return selected ? glint(it) : it;
    }

    private ItemStack digitItem(SudokuRound round, int digit, boolean palette) {
        if (round.picture()) {
            List<Material> pics = cfg().sudokuPictureSymbols;
            Material m = digit - 1 < pics.size() ? pics.get(digit - 1) : Material.PAPER;
            return item(m, "&f&l" + pretty(m), palette ? "&7Tap, then tap squares to place" : "");
        }
        ItemStack it = item(Material.PAPER, "&f&l" + digit, palette ? "&7Tap, then tap squares to place" : "");
        it.setAmount(digit);
        return it;
    }

    private String symbolName(SudokuRound round, int v) {
        if (v == 0) return " ";
        if (round.picture()) {
            List<Material> pics = cfg().sudokuPictureSymbols;
            return v - 1 < pics.size() ? pretty(pics.get(v - 1)) : String.valueOf(v);
        }
        return String.valueOf(v);
    }

    private ItemStack eraserItem(boolean selected) {
        ItemStack it = item(Material.BRICK, "&e&lEraser", "&7Pick this, then tap a square", "&7to clear what you placed");
        return selected ? glint(it) : it;
    }

    private ItemStack checkItem(SudokuRound round) {
        GameConfig.SudTier tier = cfg().sudokuTiers.get(round.tierId());
        boolean kid = tier != null && tier.kid();
        String cost = kid ? "&7Free for the kid puzzle — no limit"
                : "&7Costs " + cfg().sudokuCheckCostPercent + "% of the prize per use"
                + " &7(" + round.checksUsed() + "/" + cfg().sudokuMaxChecks + " used)";
        return item(Material.COMPASS, "&f&lCheck my squares", cost, "&7Wrong squares flash red");
    }

    static String pretty(Material m) {
        String s = m.name().toLowerCase().replace('_', ' ');
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
