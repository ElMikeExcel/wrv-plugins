package com.winkreyes.excelbraingames.gui;

import com.winkreyes.excelbraingames.ExcelBrainGames;
import com.winkreyes.excelbraingames.GameConfig;
import com.winkreyes.excelbraingames.GameService;
import com.winkreyes.excelbraingames.PlayerData;
import com.winkreyes.excelbraingames.games.memory.MemoryRound;
import com.winkreyes.excelbraingames.games.sudoku.SudokuRound;
import com.winkreyes.excelbraingames.games.sudoku.SudokuTextGrid;
import com.winkreyes.excelbraingames.games.wordsearch.WordSearchRound;
import com.winkreyes.excelbraingames.core.TileKeys;
import com.winkreyes.excelbraingames.core.WsGeometry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
 *
 * Word Search boards (0.3.2) render through the ROUND's recorded geometry
 * (core/WsGeometry), never the viewer's platform: a Bedrock-dealt round is
 * a 5-wide stream on every client, a Java-dealt round keeps the 9-wide
 * map — a round always looks the way it was dealt. Every other game keeps
 * the 9-wide map (Sudoku's chat-grid print is its Bedrock mitigation).
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

    /**
     * Letter Tiles (0.3.1): stamp the item_model component that the letter
     * tile packs resolve to a printed letter/digit (Java pack directly,
     * Bedrock through Geyser's v2 mappings). PRESENTATION ONLY — the base
     * item, name and lore are exactly the 0.3.0 rendering, so a client
     * without the pack sees and plays the identical card/board.
     */
    static ItemStack stamp(ItemStack it, String modelKey) {
        ItemMeta meta = it.getItemMeta();
        meta.setItemModel(new NamespacedKey(TileKeys.NAMESPACE, modelKey));
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
        String qmLore = pd.quickMath != null ? "&eRound in progress — click to resume"
                : "&710 quick questions, real money";
        inv.setItem(20, item(Material.REDSTONE, cfg.msg("picker-quickmath", "&c&lQuick Math"), qmLore));
        h.actions.put(20, "game:quickmath");
        String scLore = pd.scramble != null ? "&eRound in progress — click to resume"
                : "&7Unscramble 5 words, bank every word";
        inv.setItem(22, item(Material.STRING, cfg.msg("picker-scramble", "&e&lWord Scramble"), scLore));
        h.actions.put(22, "game:scramble");
        String rdLore;
        if (pd.riddleSolved) rdLore = "&aSolved today — nice work!";
        else if (pd.riddleAttempts >= com.winkreyes.excelbraingames.games.text.RiddleText.MAX_ATTEMPTS)
            rdLore = "&7No attempts left today — new riddle tomorrow";
        else rdLore = "&7One riddle a day — pays " + com.winkreyes.excelbraingames.core.Money.fmt(
                com.winkreyes.excelbraingames.games.text.RiddleText.payoutCents(pd.riddleHints));
        inv.setItem(24, item(Material.WRITTEN_BOOK, cfg.msg("picker-riddle", "&6&lRiddle of the Day"), rdLore));
        h.actions.put(24, "game:riddle");
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
            case "quickmath" -> "&c&lQuick Math &7— pick a level";
            case "scramble" -> "&e&lWord Scramble &7— pick a level";
            default -> "&b&lMemory Match &7— pick a size";
        };
        Inventory inv = inv(h, 27, title);
        for (int i = 0; i < 27; i++) inv.setItem(i, filler());
        boolean kid = plugin.gameConfig().isKid(p.getUniqueId());
        int slot = 10;
        if (game.equals("wordsearch")) {
            for (GameConfig.WsTier tier : cfg().wsTiers.values()) {
                if (slot > 16) break;
                // 0.3.2: name the board THIS player will be dealt — a Bedrock
                // player's tiers wear their 5-wide shapes (same tier terms:
                // words, lengths, payout, par are the tier's own either way).
                GameConfig.WsTier shown = cfg().wsTierFor(tier.id(), plugin.platformOf(p));
                inv.setItem(slot, tierItem(shown != null ? shown.label() : tier.label(), com.winkreyes.excelbraingames.core.Money.fmt(tier.payoutCents()),
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
        } else if (game.equals("quickmath")) {
            for (GameConfig.QmTier tier : cfg().qmTiers.values()) {
                if (slot > 16) break;
                String lore = tier.choices() ? "&7Tap answers — no typing" : "&7Type answers in chat";
                inv.setItem(slot, tierItemLore(tier.label(), com.winkreyes.excelbraingames.core.Money.fmt(tier.payoutCents()),
                        tier.kid(), kid, lore));
                if (tierAvailableText(tier.kid())) h.actions.put(slot, "tier:quickmath:" + tier.id());
                slot++;
            }
        } else if (game.equals("scramble")) {
            for (GameConfig.ScrTier tier : cfg().scrambleTiers.values()) {
                if (slot > 16) break;
                String lore = tier.kid() ? "&7Tap the right word — retries allowed"
                        : "&7Type the word — " + com.winkreyes.excelbraingames.core.Money.fmt(tier.perWordCents()) + " per word";
                inv.setItem(slot, tierItemLore(tier.label(), com.winkreyes.excelbraingames.core.Money.fmt(tier.payoutCents()),
                        tier.kid(), kid, lore));
                if (tierAvailableText(tier.kid())) h.actions.put(slot, "tier:scramble:" + tier.id());
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

    private ItemStack tierItemLore(String label, String payout, boolean kidTier, boolean kidFlagged, String extraLore) {
        GameConfig cfg = cfg();
        if (kidTier && !cfg.kidTiersEnabled) {
            return item(Material.GRAY_DYE, "&8&l" + label,
                    "&8Kid tier — opens after the", "&8word lists are hand-checked");
        }
        if (kidTier && !kidFlagged) {
            return item(Material.PAPER, "&e&l" + label,
                    "&7Pays up to " + payout, "&6Kid tier: grown-ups earn half", extraLore);
        }
        return item(Material.PAPER, "&f&l" + label, "&7Pays up to " + payout, extraLore);
    }

    private boolean tierAvailableText(boolean kidTier) {
        return !(kidTier && !cfg().kidTiersEnabled);
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
            case "quickmath" -> "Quick Math";
            case "scramble" -> "Word Scramble";
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
        // 0.3.2: the slot map is the ROUND's recorded geometry. Under JAVA
        // this loop visits exactly the slots 0.3.1 visited (cellAt inverts
        // r*9+c); under BEDROCK the cells stream contiguously from slot 0,
        // so the phone's ~5-wide Geyser wrap displays the board as dealt.
        WsGeometry geo = round.geometry();
        for (int slot = 0; slot < 54; slot++) {
            if (slot == SLOT_WORDS || slot == SLOT_NEW || slot == SLOT_CLOSE) continue;
            int idx = geo.cellAt(slot, cols, rows);
            if (idx >= 0) {
                if (round.isVoid(idx)) { inv.setItem(slot, filler()); continue; }
                char letter = round.letterAt(idx);
                boolean foundCell = round.cellFound(idx);
                boolean pendingCell = round.pending() == idx;
                if (foundCell) {
                    inv.setItem(slot, stamp(item(Material.LIME_STAINED_GLASS_PANE, "&a&l" + letter),
                            TileKeys.wsModel(letter, TileKeys.WsState.FOUND)));
                } else if (pendingCell) {
                    inv.setItem(slot, stamp(item(Material.YELLOW_STAINED_GLASS_PANE, "&e&l" + letter,
                            "&7First letter picked —", "&7now tap the last letter"),
                            TileKeys.wsModel(letter, TileKeys.WsState.SELECTED)));
                } else {
                    inv.setItem(slot, stamp(item(Material.PAPER, "&f&l" + letter),
                            TileKeys.wsModel(letter, TileKeys.WsState.NORMAL)));
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
        printSudokuGrid(p, round); // 0.3.1: the square, in chat, at every board opening
        return inv;
    }

    /**
     * 0.3.1 (Extension 2): the Sudoku board as a bordered chat square. The
     * Bedrock chest GUI reflows to ~5 columns and wraps the Java layout, so
     * the grid itself is unreadable there; chat always renders as a square.
     * Givens are [bracketed], placed digits plain, blanks dotted; picture
     * rounds print their palette numbers plus a legend.
     */
    public void printSudokuGrid(Player p, SudokuRound round) {
        p.sendMessage(GameService.text("&d&lSudoku &7— your board: &f[ ]&7 = given, plain = yours, &f.&7 = empty"));
        for (String line : SudokuTextGrid.lines(round)) {
            p.sendMessage(GameService.text("&f" + line));
        }
        if (round.picture()) {
            List<Material> pics = cfg().sudokuPictureSymbols;
            StringBuilder legend = new StringBuilder("&7Pictures: ");
            for (int d = 1; d <= round.size() && d <= pics.size(); d++) {
                if (d > 1) legend.append("   ");
                legend.append("&f").append(d).append(" &7= ").append(pretty(pics.get(d - 1)));
            }
            p.sendMessage(GameService.text(legend.toString()));
        }
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
        // 0.3.1 (Extension 2): the chat-grid control rides every board size —
        // slot 44 is filler in all three layouts.
        inv.setItem(44, item(Material.BOOK, "&f&lShow board in chat",
                "&7Prints the whole square to chat —", "&7read it there, tap cells here"));
        h.actions.put(44, "spal:show");
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
            ItemStack it = item(Material.RED_STAINED_GLASS_PANE, "&c&l" + symbolName(round, v), "&cThat one isn't right");
            // 0.3.1: digit tile on the red pane in BOTH modes — in picture mode
            // the digit is the picture's number (see the chat-grid legend).
            return v >= 1 && v <= 9 ? stamp(it, TileKeys.sudokuModel(v, TileKeys.SudokuState.WRONG)) : it;
        }
        if (v == 0) {
            ItemStack it = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = it.getItemMeta();
            meta.displayName(GameService.text(" "));
            it.setItemMeta(meta);
            return it;
        }
        if (round.isGiven(idx)) {
            return stamp(item(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "&9&l" + symbolName(round, v),
                    "&7Given — locked"), TileKeys.sudokuModel(v, TileKeys.SudokuState.GIVEN));
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
        // 0.3.1: digit tile on board cells and palette alike; the palette ALSO
        // keeps its stack-count digit (readable without any pack).
        return stamp(it, TileKeys.sudokuModel(digit, TileKeys.SudokuState.PLACED));
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

    // ------------------------------------------------------------ text-game cards (Phase 3)

    /**
     * The shared question card: Quick Math and Word Scramble render
     * through this one view, fed by the shared question loop
     * (TextLoop.nextPrompt). Choice rounds get tappable answers; typed
     * rounds point at chat (the chat listener feeds the same loop).
     */
    public Inventory questionCard(Player p, String game) {
        BrainHolder h = new BrainHolder(BrainHolder.View.QUESTION);
        h.game = game;
        String title = game.equals("quickmath") ? "&c&lQuick Math" : "&e&lWord Scramble";
        Inventory inv = inv(h, 27, title);
        fillQuestion(inv, h, p);
        return inv;
    }

    public void fillQuestion(Inventory inv, BrainHolder h, Player p) {
        PlayerData pd = plugin.service().data(p);
        for (int i = 0; i < 27; i++) inv.setItem(i, filler());
        h.actions.clear();
        var prompt = plugin.textLoop().nextPrompt(p.getUniqueId().toString(), h.game, "");
        boolean qm = h.game.equals("quickmath");
        String progress;
        List<String> choices = prompt.choices();
        if (qm) {
            var r = pd.quickMath;
            progress = r == null ? "" : "&7Question " + Math.min(r.index() + 1, 10) + " of 10 · " + r.correct() + " right so far";
        } else {
            var r = pd.scramble;
            progress = r == null ? "" : "&7Word " + Math.min(r.index() + 1, 5) + " of 5 · banked "
                    + com.winkreyes.excelbraingames.core.Money.fmt(r.earnedCents());
        }
        inv.setItem(4, item(Material.PAPER, "&f&l" + prompt.question(), progress,
                qm ? "&7One try per question" : "&7Unscramble this word"));
        if (!choices.isEmpty()) {
            int[] slots = {11, 13, 15};
            for (int i = 0; i < choices.size() && i < 3; i++) {
                ItemStack choiceItem = item(Material.EMERALD, "&a&l" + choices.get(i), "&7Tap to answer");
                if (qm) {
                    // 0.3.1 (Extension 1): the kid card's numbers live in item
                    // names — invisible pre-tap on mobile. The number tile
                    // prints the value on the emerald itself; a no-pack client
                    // still sees the name exactly as 0.3.0.
                    String key = TileKeys.numberModelFor(choices.get(i));
                    if (key != null) choiceItem = stamp(choiceItem, key);
                }
                inv.setItem(slots[i], choiceItem);
                h.actions.put(slots[i], "qchoice:" + choices.get(i));
            }
            if (qm && pd.quickMath != null && pd.quickMath.choices()) {
                // 0.3.1 add-on (§8 residual, closed): the option tiles fixed
                // the ANSWERS, but the kid QUESTION itself lives only in the
                // prompt paper's name — still invisible pre-tap on mobile.
                // Print it ONCE per question, mirroring the Scramble kid
                // print below (index in the key: an identical question later
                // in the round is a NEW question and prints again; re-renders
                // of the same question stay silent). Typed rounds never reach
                // this branch (no choices) — their questions already arrive
                // via TextInputListener.promptInChat at open and per answer.
                String key = "qm|" + pd.quickMath.index() + "|" + prompt.question()
                        + "|" + String.join(",", choices);
                if (h.choicePrint.markIfNew(key)) {
                    p.sendMessage(GameService.text("&c&lQuick Math &7— question "
                            + Math.min(pd.quickMath.index() + 1, 10) + " of 10: &f&l"
                            + prompt.question()));
                }
            }
            if (!qm && pd.scramble != null && pd.scramble.kid()) {
                // 0.3.1 (Extension 1): whole-word choices can't be tiles. Print
                // them ONCE per word prompt, numbered in card order, so a
                // mobile player reads before tapping.
                String key = prompt.question() + "|" + String.join(",", choices);
                if (h.choicePrint.markIfNew(key)) {
                    StringBuilder sb = new StringBuilder("&e&lWord Scramble &7— unscramble &f&l")
                            .append(prompt.question()).append("&7 — tap: ");
                    for (int i = 0; i < choices.size(); i++) {
                        if (i > 0) sb.append("&7  ·  ");
                        sb.append("&f&l").append(i + 1).append(") ").append(choices.get(i));
                    }
                    p.sendMessage(GameService.text(sb.toString()));
                }
            }
        } else {
            inv.setItem(13, item(Material.OAK_SIGN, "&e&lType your answer in chat",
                    "&7Your next chat message is your answer"));
        }
        if (!qm && pd.scramble != null && !pd.scramble.kid()) {
            var r = pd.scramble;
            inv.setItem(20, item(Material.TORCH, "&e&lHint",
                    r.hintUsed() ? "&7This word starts with: &f&l" + r.hintLetter()
                            : "&7First letter — halves this word's pay"));
            h.actions.put(20, "qhint");
        }
        if (!qm) {
            var r = pd.scramble;
            inv.setItem(22, item(Material.GOLD_INGOT, "&6&lFinish now",
                    "&7Bank what you've earned: " + com.winkreyes.excelbraingames.core.Money.fmt(r == null ? 0 : r.earnedCents())));
            h.actions.put(22, "qfinish");
        } else {
            inv.setItem(22, item(Material.CLOCK, "&7&lSave & stop", "&7Your round waits right here"));
            h.actions.put(22, "qstop");
        }
        inv.setItem(26, item(Material.BARRIER, "&c&lClose"));
        h.actions.put(26, "qclose");
    }

    /** The Riddle of the Day card: today's riddle, hint and answer doors. */
    public Inventory riddleCard(Player p, PlayerData pd) {
        BrainHolder h = new BrainHolder(BrainHolder.View.RIDDLE);
        h.game = "riddle";
        Inventory inv = inv(h, 27, "&6&lRiddle of the Day");
        fillRiddle(inv, h, p, pd);
        return inv;
    }

    public void fillRiddle(Inventory inv, BrainHolder h, Player p, PlayerData pd) {
        for (int i = 0; i < 27; i++) inv.setItem(i, filler());
        h.actions.clear();
        var prompt = plugin.textLoop().nextPrompt(p.getUniqueId().toString(), "riddle", "");
        java.util.List<String> lore = new java.util.ArrayList<>();
        for (String line : wrap(prompt.question(), 34)) lore.add("&f" + line);
        lore.add("");
        if (pd.riddleSolved) lore.add("&aYou solved it — paid for today!");
        else if (pd.riddleAttempts >= com.winkreyes.excelbraingames.games.text.RiddleText.MAX_ATTEMPTS)
            lore.add("&7No attempts left — new riddle tomorrow");
        else lore.add("&7Attempts left today: "
                + (com.winkreyes.excelbraingames.games.text.RiddleText.MAX_ATTEMPTS - pd.riddleAttempts));
        inv.setItem(4, item(Material.WRITTEN_BOOK, "&6&lToday's riddle", lore.toArray(new String[0])));
        var riddle = plugin.wordLists().riddles().get(plugin.todaysRiddleIndex());
        if (!pd.riddleSolved && pd.riddleHints > 0) {
            inv.setItem(11, item(Material.TORCH, "&e&lHint", "&f" + riddle.hint()));
        } else if (!pd.riddleSolved) {
            inv.setItem(11, item(Material.TORCH, "&e&lHint",
                    "&7Costs $25 of the prize", "&7Pays " + com.winkreyes.excelbraingames.core.Money.fmt(
                            com.winkreyes.excelbraingames.games.text.RiddleText.payoutCents(pd.riddleHints + 1)) + " if you solve it"));
            h.actions.put(11, "rhint");
        }
        if (!pd.riddleSolved) {
            inv.setItem(15, item(Material.NAME_TAG, "&a&lAnswer in chat",
                    "&7Tap, then type your answer in chat"));
            h.actions.put(15, "ranswer");
        }
        inv.setItem(22, item(Material.ARROW, "&7Back"));
        h.actions.put(22, "back:picker");
        inv.setItem(26, item(Material.BARRIER, "&c&lClose"));
        h.actions.put(26, "qclose");
    }

    static java.util.List<String> wrap(String text, int width) {
        java.util.List<String> out = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() + word.length() + 1 > width && line.length() > 0) {
                out.add(line.toString());
                line = new StringBuilder();
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    static String pretty(Material m) {
        String s = m.name().toLowerCase().replace('_', ' ');
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
