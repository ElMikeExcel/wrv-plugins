package com.winkreyes.leaderboard.paper;

import com.winkreyes.leaderboard.core.BoardMath;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Pure builders for every /leaderboard presentation: the Bedrock form
 * labels and content, the Java clickable-chat lines, and the chest
 * fallback's slot layout. Everything here is a static function of the
 * snapshot map, so the exact strings a player sees are unit-testable
 * off-server; the reflective form sender, the Adventure chat renderer,
 * and the chest menu are thin shells over these builders. */
public final class BoardPresentation {

    private BoardPresentation() {}

    // ---- Shared ---------------------------------------------------------

    public static final String MAIN_TITLE = "Leaderboards";
    public static final String MAIN_FORM_CONTENT = "Pick a board.";
    public static final String WARMING_UP_TEXT =
            "Boards are warming up… Try again in a few seconds.";

    /** Boards present in the snapshot, in menu (enum) order. Mirrors the
     * availability filter every presentation applies. */
    public static List<Category> availableBoards(Map<Category, List<BoardMath.Entry>> boards) {
        List<Category> available = new ArrayList<>();
        for (Category category : Category.values()) {
            if (boards.containsKey(category)) {
                available.add(category);
            }
        }
        return available;
    }

    /** "Name · value" for the board's leader, or null when the board has
     * no entries (callers then show the bare board title). */
    public static String leaderLine(Category category, List<BoardMath.Entry> board) {
        if (board == null || board.isEmpty()) {
            return null;
        }
        BoardMath.Entry leader = board.get(0);
        return leader.name() + " · " + category.formatValue(leader.value());
    }

    /** "#N · value" rank line for the viewer, shared by the form content
     * and the chat board view. */
    public static String ownRankLine(Category category, List<BoardMath.Entry> board, UUID viewer) {
        int rank = BoardMath.rankOf(board, viewer);
        if (rank <= 0) {
            return "Your rank: not on this board yet";
        }
        for (BoardMath.Entry entry : board) {
            if (entry.player().equals(viewer)) {
                return "Your rank: #" + rank + " · " + category.formatValue(entry.value());
            }
        }
        return "Your rank: not on this board yet";
    }

    /** Ranked lines for the top entries: "#1 Name · value", ranks shared
     * on ties exactly like the chest board view's head cards. */
    public static List<String> rankedLines(Category category, List<BoardMath.Entry> board, int limit) {
        List<String> lines = new ArrayList<>();
        for (BoardMath.Entry entry : BoardMath.top(board, limit)) {
            int rank = BoardMath.rankOf(board, entry.player());
            lines.add("#" + rank + " " + entry.name() + " · " + category.formatValue(entry.value()));
        }
        return lines;
    }

    // ---- Bedrock forms ----------------------------------------------------

    public static final String[] BOARD_FORM_BUTTONS = {"← All boards", "Close"};

    /** One compact single-line button per available board:
     * "Top Miner — Name · 12,340"; a board with no entries yet shows its
     * bare title. */
    public static List<String> mainFormButtons(Map<Category, List<BoardMath.Entry>> boards) {
        List<String> labels = new ArrayList<>();
        for (Category category : availableBoards(boards)) {
            String leader = leaderLine(category, boards.get(category));
            labels.add(leader == null ? category.title() : category.title() + " — " + leader);
        }
        return labels;
    }

    /** Board form content: the full top 10, a blank line, then the
     * viewer's own rank line. */
    public static List<String> boardContentLines(Category category, List<BoardMath.Entry> board,
                                                 UUID viewer) {
        List<String> lines = new ArrayList<>(rankedLines(category, board, 10));
        lines.add("");
        lines.add(ownRankLine(category, board, viewer));
        return lines;
    }

    public static String boardContent(Category category, List<BoardMath.Entry> board, UUID viewer) {
        return String.join("\n", boardContentLines(category, board, viewer));
    }

    // ---- Chest fallback layout ---------------------------------------------

    /** Spaced main-menu grid in a 54-slot chest (Mike's rule, Oct 15,
     * 2026: a blank slot between every icon so nobody fat-fingers a
     * neighbour). Category index i (in available order) sits at
     * MAIN_SLOTS[i]; the 9th board is centered on the fourth row. The
     * chest fallback shows at most these nine boards; forms and chat
     * list every available board. */
    public static final int[] MAIN_SLOTS = {10, 12, 14, 16, 19, 21, 23, 25, 31};

    public static int slotForCategoryIndex(int index) {
        return index >= 0 && index < MAIN_SLOTS.length ? MAIN_SLOTS[index] : -1;
    }

    /** Inverse of the layout: which category index a clicked raw slot
     * means in the main menu, or -1 for any other slot. */
    public static int categoryIndexForSlot(int rawSlot) {
        for (int i = 0; i < MAIN_SLOTS.length; i++) {
            if (MAIN_SLOTS[i] == rawSlot) {
                return i;
            }
        }
        return -1;
    }

    // ---- Java clickable chat ------------------------------------------------

    public enum Kind { HEADER, ENTRY, RANK, OWN_RANK, BACK, NOTE }

    /** One chat line: its exact text, the command a click runs (null =
     * not clickable), the hover text (null = none), and its render kind. */
    public record ChatLine(String text, String clickCommand, String hoverText, Kind kind) {}

    public static final String MAIN_HEADER = "━━━━ Leaderboards ━━━━";
    public static final String BOARD_COMMAND_PREFIX = "/leaderboard board ";
    public static final String BACK_TEXT = "« Back to boards";

    /** The command a board's chat line runs; the key is the Category enum
     * name in lowercase (miner, builder, ..., mcmmo, jobs, elite). */
    public static String boardCommand(Category category) {
        return BOARD_COMMAND_PREFIX + category.name().toLowerCase(Locale.ROOT);
    }

    public static List<ChatLine> mainChatLines(Map<Category, List<BoardMath.Entry>> boards) {
        List<ChatLine> lines = new ArrayList<>();
        lines.add(new ChatLine(MAIN_HEADER, null, null, Kind.HEADER));
        List<Category> available = availableBoards(boards);
        if (available.isEmpty()) {
            lines.add(new ChatLine(WARMING_UP_TEXT, null, null, Kind.NOTE));
            return lines;
        }
        for (Category category : available) {
            String leader = leaderLine(category, boards.get(category));
            String text = "» " + category.title() + (leader == null ? "" : " — " + leader);
            lines.add(new ChatLine(text, boardCommand(category), "Click to view", Kind.ENTRY));
        }
        return lines;
    }

    public static List<ChatLine> boardChatLines(Map<Category, List<BoardMath.Entry>> boards,
                                                 Category category, UUID viewer) {
        List<BoardMath.Entry> board = boards.getOrDefault(category, List.of());
        List<ChatLine> lines = new ArrayList<>();
        lines.add(new ChatLine("━━ " + category.title() + " ━━", null, null, Kind.HEADER));
        for (String line : rankedLines(category, board, 10)) {
            lines.add(new ChatLine(line, null, null, Kind.RANK));
        }
        lines.add(new ChatLine(ownRankLine(category, board, viewer), null, null, Kind.OWN_RANK));
        lines.add(new ChatLine(BACK_TEXT, "/leaderboard", null, Kind.BACK));
        return lines;
    }
}
