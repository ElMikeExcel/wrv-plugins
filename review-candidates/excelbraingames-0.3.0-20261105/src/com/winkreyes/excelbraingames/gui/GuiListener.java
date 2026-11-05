package com.winkreyes.excelbraingames.gui;

import com.winkreyes.excelbraingames.ExcelBrainGames;
import com.winkreyes.excelbraingames.GameConfig;
import com.winkreyes.excelbraingames.GameService;
import com.winkreyes.excelbraingames.PlayerData;
import com.winkreyes.excelbraingames.games.memory.MemoryRound;
import com.winkreyes.excelbraingames.games.sudoku.SudokuRound;
import com.winkreyes.excelbraingames.games.wordsearch.WordSearchRound;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * All Brain Games clicks flow through here. Every click inside a Brain Games
 * view is cancelled (nothing is a real item), then dispatched on the action
 * string the view builder stored in the holder. Round state auto-saves on
 * every mutation (SPEC §9.5).
 */
public final class GuiListener implements Listener {

    private final ExcelBrainGames plugin;
    private final Views views;

    public GuiListener(ExcelBrainGames plugin) {
        this.plugin = plugin;
        this.views = plugin.views();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof BrainHolder h)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() != top) return;
        String action = h.actions.get(e.getSlot());
        if (action == null) return;
        GameService svc = plugin.service();
        PlayerData pd = svc.data(p);
        handle(p, pd, h, top, action);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof BrainHolder) {
            e.setCancelled(true);
        }
    }

    private void handle(Player p, PlayerData pd, BrainHolder h, Inventory inv, String action) {
        GameService svc = plugin.service();
        String[] parts = action.split(":");
        switch (parts[0]) {
            case "soon" -> p.sendMessage(GameService.text("&8That game isn't out yet — it's on the way."));
            case "game" -> {
                String game = parts[1];
                if (game.equals("riddle")) {
                    pd.riddleRoll(svc.today());
                    p.openInventory(views.riddleCard(p, pd));
                    return;
                }
                boolean active = switch (game) {
                    case "wordsearch" -> pd.wordSearch != null;
                    case "sudoku" -> pd.sudoku != null;
                    case "quickmath" -> pd.quickMath != null;
                    case "scramble" -> pd.scramble != null;
                    default -> pd.memory != null;
                };
                if (active) p.openInventory(views.resume(p, game, false));
                else if (game.equals("wordsearch")) p.openInventory(views.themePick(p));
                else p.openInventory(views.tierPick(p, game, ""));
            }
            case "theme" -> p.openInventory(views.tierPick(p, "wordsearch", parts[1]));
            case "tier" -> startRound(p, pd, parts);
            case "resume" -> {
                if (parts[1].equals("wordsearch") && pd.wordSearch != null) {
                    p.openInventory(views.wsBoard(p, pd.wordSearch));
                } else if (parts[1].equals("memory") && pd.memory != null) {
                    p.openInventory(views.memBoard(p, pd.memory));
                } else if (parts[1].equals("sudoku") && pd.sudoku != null) {
                    p.openInventory(views.sudokuBoard(p, pd.sudoku));
                } else if (parts[1].equals("quickmath") && pd.quickMath != null) {
                    openQuestion(p, "quickmath");
                } else if (parts[1].equals("scramble") && pd.scramble != null) {
                    openQuestion(p, "scramble");
                } else {
                    p.openInventory(views.picker(p, pd));
                }
            }
            case "discard" -> {
                if (!h.armed) {
                    p.openInventory(views.resume(p, parts[1], true));
                } else {
                    if (parts[1].equals("wordsearch")) { pd.wordSearch = null; svc.store().save(pd); p.openInventory(views.themePick(p)); }
                    else if (parts[1].equals("sudoku")) { pd.sudoku = null; svc.store().save(pd); p.openInventory(views.tierPick(p, "sudoku", "")); }
                    else if (parts[1].equals("quickmath")) { pd.quickMath = null; svc.clearInput(p.getUniqueId()); svc.store().save(pd); p.openInventory(views.tierPick(p, "quickmath", "")); }
                    else if (parts[1].equals("scramble")) { pd.scramble = null; svc.clearInput(p.getUniqueId()); svc.store().save(pd); p.openInventory(views.tierPick(p, "scramble", "")); }
                    else { pd.memory = null; svc.store().save(pd); p.openInventory(views.tierPick(p, "memory", "")); }
                }
            }
            case "back" -> {
                if (parts[1].equals("picker")) p.openInventory(views.picker(p, pd));
                else if (parts[1].equals("board") && pd.wordSearch != null) p.openInventory(views.wsBoard(p, pd.wordSearch));
            }
            case "cell" -> wsTap(p, pd, h, inv, Integer.parseInt(parts[1]));
            case "card" -> memFlip(p, pd, h, inv, Integer.parseInt(parts[1]));
            case "scell" -> sudokuCellTap(p, pd, h, inv, Integer.parseInt(parts[1]));
            case "spal" -> sudokuPalette(p, pd, h, inv, parts[1]);
            case "qchoice" -> textAnswer(p, pd, h, inv, parts[1]);
            case "qhint" -> {
                if (pd.scramble != null && pd.scramble.useHint()) {
                    svc.store().save(pd);
                    p.sendMessage(GameService.text("&eHint: this word starts with &f&l" + pd.scramble.hintLetter()));
                    click(p);
                    views.fillQuestion(inv, h, p);
                }
            }
            case "qfinish" -> {
                if (pd.scramble != null) {
                    svc.completeScramble(p, pd, pd.scramble);
                    h.done = true;
                    p.closeInventory();
                }
            }
            case "qstop" -> { svc.clearInput(p.getUniqueId()); p.closeInventory(); }
            case "qclose" -> { svc.clearInput(p.getUniqueId()); p.closeInventory(); }
            case "rhint" -> {
                pd.riddleRoll(svc.today());
                if (!pd.riddleSolved && pd.riddleHints < com.winkreyes.excelbraingames.games.text.RiddleText.MAX_HINTS) {
                    pd.riddleHints++;
                    svc.store().save(pd);
                    var riddle = plugin.wordLists().riddles().get(plugin.todaysRiddleIndex());
                    p.sendMessage(GameService.text("&eHint: &f" + riddle.hint()));
                    click(p);
                    views.fillRiddle(inv, h, p, pd);
                }
            }
            case "ranswer" -> {
                pd.riddleRoll(svc.today());
                if (!pd.riddleSolved && pd.riddleAttempts < com.winkreyes.excelbraingames.games.text.RiddleText.MAX_ATTEMPTS) {
                    svc.expectInput(p, "riddle");
                    p.closeInventory();
                    p.sendMessage(GameService.text("&6&lRiddle of the Day: &r&f"
                            + plugin.wordLists().riddles().get(plugin.todaysRiddleIndex()).question()));
                    p.sendMessage(GameService.text("&7Type your answer in chat — it won't be shown to everyone."));
                }
            }
            case "ctl" -> {
                switch (parts[1]) {
                    case "words" -> { if (pd.wordSearch != null) p.openInventory(views.wsWords(p, pd.wordSearch)); }
                    case "new" -> p.openInventory(views.resume(p, h.game, false));
                    case "close" -> p.closeInventory();
                }
            }
        }
    }

    private void startRound(Player p, PlayerData pd, String[] parts) {
        GameService svc = plugin.service();
        if (parts[1].equals("wordsearch")) {
            GameConfig.WsTier tier = plugin.gameConfig().wsTiers.get(parts[2]);
            GameConfig.Theme theme = plugin.gameConfig().themes.get(parts[3]);
            if (tier == null || theme == null) return;
            WordSearchRound round = svc.startWordSearch(p, theme, tier);
            click(p);
            p.openInventory(views.wsBoard(p, round));
        } else if (parts[1].equals("sudoku")) {
            GameConfig.SudTier tier = plugin.gameConfig().sudokuTiers.get(parts[2]);
            if (tier == null) return;
            SudokuRound round = svc.startSudoku(p, tier, parts[3].equals("pic"));
            click(p);
            p.openInventory(views.sudokuBoard(p, round));
        } else if (parts[1].equals("quickmath")) {
            GameConfig.QmTier tier = plugin.gameConfig().qmTiers.get(parts[2]);
            if (tier == null) return;
            svc.startQuickMath(p, tier);
            click(p);
            openQuestion(p, "quickmath");
        } else if (parts[1].equals("scramble")) {
            GameConfig.ScrTier tier = plugin.gameConfig().scrambleTiers.get(parts[2]);
            if (tier == null) return;
            if (svc.startScramble(p, tier) == null) {
                p.sendMessage(GameService.text("&cThat word pool is too small right now — try the other level."));
                return;
            }
            click(p);
            openQuestion(p, "scramble");
        } else {
            GameConfig.MemTier tier = plugin.gameConfig().memTiers.get(parts[2]);
            if (tier == null) return;
            MemoryRound round = svc.startMemory(p, tier);
            click(p);
            p.openInventory(views.memBoard(p, round));
        }
    }

    // ------------------------------------------------------------ text games

    /** Open the shared question card; typed rounds also enter chat-answer mode. */
    private void openQuestion(Player p, String game) {
        GameService svc = plugin.service();
        PlayerData pd = svc.data(p);
        p.openInventory(views.questionCard(p, game));
        boolean typed = game.equals("quickmath")
                ? pd.quickMath != null && !pd.quickMath.choices()
                : pd.scramble != null && !pd.scramble.kid();
        if (typed) {
            svc.expectInput(p, game);
            plugin.textInput().promptInChat(p, game);
        }
    }

    /** A tapped choice on the question card — same loop path as a chat answer. */
    private void textAnswer(Player p, PlayerData pd, BrainHolder h, Inventory inv, String value) {
        if (h.done) return;
        click(p);
        plugin.textInput().processAnswer(p, h.game, value);
    }

    // ------------------------------------------------------------ Sudoku

    private void sudokuPalette(Player p, PlayerData pd, BrainHolder h, Inventory inv, String what) {
        SudokuRound round = pd.sudoku;
        if (round == null || h.done) return;
        GameService svc = plugin.service();
        GameConfig.SudTier tier = plugin.gameConfig().sudokuTiers.get(round.tierId());
        switch (what) {
            case "erase" -> { h.sudokuSel = -1; click(p); views.renderSudoku(inv, h, round); }
            case "bank" -> { h.sudokuBankHigh = !h.sudokuBankHigh; click(p); views.renderSudoku(inv, h, round); }
            case "page" -> { h.sudokuPage = h.sudokuPage == 0 ? 1 : 0; click(p); views.renderSudoku(inv, h, round); }
            case "check" -> {
                int max = tier != null && tier.kid() ? Integer.MAX_VALUE : plugin.gameConfig().sudokuMaxChecks;
                if (!round.useCheck(max)) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f);
                    p.sendMessage(GameService.text("&eYou've used all your Checks on this puzzle — the rest is on you!"));
                    return;
                }
                svc.store().save(pd);
                java.util.List<Integer> wrong = round.wrongCells();
                h.sudokuWrong = new java.util.HashSet<>(wrong);
                h.sudokuWrongUntil = System.currentTimeMillis() + 4000;
                if (wrong.isEmpty()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.4f);
                    p.sendMessage(GameService.text("&aCheck: everything you've placed so far is right!"));
                } else {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
                    p.sendMessage(GameService.text("&cCheck: " + wrong.size() + " square(s) aren't right — they're flashing red."));
                }
                views.renderSudoku(inv, h, round);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    h.sudokuWrong = java.util.Set.of();
                    if (p.getOpenInventory().getTopInventory().getHolder() == h && pd.sudoku == round) {
                        views.renderSudoku(inv, h, round);
                    }
                }, 80L);
            }
            default -> { // a digit
                h.sudokuSel = Integer.parseInt(what);
                click(p);
                views.renderSudoku(inv, h, round);
            }
        }
    }

    private void sudokuCellTap(Player p, PlayerData pd, BrainHolder h, Inventory inv, int idx) {
        SudokuRound round = pd.sudoku;
        if (round == null || h.done) return;
        GameService svc = plugin.service();
        if (round.isGiven(idx)) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
            return;
        }
        if (h.sudokuSel == 0) {
            p.sendMessage(GameService.text("&7Pick a number from the palette first, then tap squares to place it."));
            return;
        }
        if (h.sudokuSel == -1) {
            if (round.erase(idx)) {
                click(p);
                svc.store().save(pd);
                views.renderSudoku(inv, h, round);
            }
            return;
        }
        SudokuRound.PlaceResult result = round.place(idx, h.sudokuSel);
        if (result != SudokuRound.PlaceResult.PLACED) return;
        click(p);
        if (round.isSolved()) {
            svc.completeSudoku(p, pd, round);
            h.done = true;
        } else {
            svc.store().save(pd);
        }
        views.renderSudoku(inv, h, round);
    }

    private void wsTap(Player p, PlayerData pd, BrainHolder h, Inventory inv, int idx) {
        WordSearchRound round = pd.wordSearch;
        if (round == null || h.done) return;
        WordSearchRound.TapResult result = round.tap(idx);
        GameService svc = plugin.service();
        switch (result) {
            case PENDING_SET -> { click(p); views.renderWs(inv, h, round); }
            case CANCELLED -> { click(p); views.renderWs(inv, h, round); }
            case FOUND -> {
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.4f);
                if (round.isComplete()) {
                    svc.completeWordSearch(p, pd, round);
                    h.done = true;
                } else {
                    svc.store().save(pd);
                }
                views.renderWs(inv, h, round);
            }
            case ALREADY_FOUND -> { click(p); views.renderWs(inv, h, round); }
            case NO_MATCH, NOT_A_LINE -> {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f);
                views.renderWs(inv, h, round);
            }
            case IGNORED -> { /* void / out of board */ }
        }
    }

    private void memFlip(Player p, PlayerData pd, BrainHolder h, Inventory inv, int card) {
        MemoryRound round = pd.memory;
        if (round == null || h.done) return;
        MemoryRound.FlipResult result = round.flip(card);
        GameService svc = plugin.service();
        switch (result) {
            case FIRST -> { click(p); svc.store().save(pd); views.renderMem(inv, h, round); }
            case MATCH -> {
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.4f);
                if (round.isComplete()) {
                    svc.completeMemory(p, pd, round);
                    h.done = true;
                } else {
                    svc.store().save(pd);
                }
                views.renderMem(inv, h, round);
            }
            case MISS -> {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.9f);
                svc.store().save(pd);
                views.renderMem(inv, h, round);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (round.resolveMiss()) {
                        svc.store().save(pd);
                        if (p.getOpenInventory().getTopInventory().getHolder() == h) {
                            views.renderMem(inv, h, round);
                        }
                    }
                }, 20L);
            }
            case IGNORED -> { /* face-up / matched / miss showing */ }
        }
    }

    private void click(Player p) {
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
    }
}
