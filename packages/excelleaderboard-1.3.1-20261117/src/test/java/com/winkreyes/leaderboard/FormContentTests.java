package com.winkreyes.leaderboard;

import com.winkreyes.leaderboard.core.BoardMath;
import com.winkreyes.leaderboard.paper.BoardPresentation;
import com.winkreyes.leaderboard.paper.Category;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Form-content suite for 1.2's Bedrock presentations: the exact button
 * labels and board content a Bedrock player is sent, built from a
 * fixture snapshot. Covers the compact one-line label, the top-10 cap,
 * own-rank present/absent, a board with fewer than 10 entries, a board
 * present with no entries (no leader line), and the empty snapshot's
 * warming-up case. */
public final class FormContentTests {

    private static int checks = 0;

    private static void eq(Object actual, Object expected, String label) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    private static UUID id(int n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    /** MINER: 12 ranked entries P1..P12 (12,000 down to 1,000).
     * BUILDER: 3 entries. RICHEST: present but empty. */
    private static Map<Category, List<BoardMath.Entry>> fixture() {
        Map<Category, List<BoardMath.Entry>> boards = new EnumMap<>(Category.class);
        List<BoardMath.Entry> miner = new ArrayList<>();
        for (int n = 1; n <= 12; n++) {
            miner.add(new BoardMath.Entry(id(n), "P" + n, 13_000L - 1_000L * n));
        }
        boards.put(Category.MINER, miner);
        boards.put(Category.BUILDER, List.of(
                new BoardMath.Entry(id(21), "Ada", 300),
                new BoardMath.Entry(id(22), "Ben", 200),
                new BoardMath.Entry(id(23), "Cid", 100)));
        boards.put(Category.RICHEST, List.of());
        return boards;
    }

    public static void main(String[] args) {
        Map<Category, List<BoardMath.Entry>> boards = fixture();
        List<BoardMath.Entry> miner = boards.get(Category.MINER);

        // Availability and button labels.
        List<Category> available = BoardPresentation.availableBoards(boards);
        eq(available.size(), 3, "available boards");
        eq(available.get(0), Category.MINER, "available order first");
        eq(available.get(2), Category.RICHEST, "available order last");
        List<String> buttons = BoardPresentation.mainFormButtons(boards);
        eq(buttons.size(), 3, "one button per available board");
        eq(buttons.get(0), "Top Miner — P1 · 12,000", "compact label with leader");
        eq(buttons.get(1), "Top Builder — Ada · 300", "compact label builder");
        eq(buttons.get(2), "Richest", "empty board shows bare title");
        eq(BoardPresentation.leaderLine(Category.MINER, miner), "P1 · 12,000", "leader line");
        check(BoardPresentation.leaderLine(Category.RICHEST, boards.get(Category.RICHEST)) == null,
                "empty board has no leader line");

        // Board content: top 10 cap, blank separator, own rank.
        List<String> lines = BoardPresentation.boardContentLines(Category.MINER, miner, id(3));
        eq(lines.size(), 12, "content lines: 10 ranked + blank + own");
        eq(lines.get(0), "#1 P1 · 12,000", "content first line");
        eq(lines.get(9), "#10 P10 · 3,000", "content tenth line (cap)");
        eq(lines.get(10), "", "blank separator before own rank");
        eq(lines.get(11), "Your rank: #3 · 10,000", "own rank line");
        check(BoardPresentation.boardContent(Category.MINER, miner, id(3))
                .startsWith("#1 P1 · 12,000\n#2 P2 · 11,000"), "joined content shape");
        List<String> absent = BoardPresentation.boardContentLines(Category.MINER, miner, id(99));
        eq(absent.get(11), "Your rank: not on this board yet", "own rank absent case");

        // Board with fewer than 10 entries.
        List<String> small = BoardPresentation.boardContentLines(
                Category.BUILDER, boards.get(Category.BUILDER), id(22));
        eq(small.size(), 5, "small board lines: 3 ranked + blank + own");
        eq(small.get(4), "Your rank: #2 · 200", "small board own rank");

        // Board present with no entries at all.
        List<String> empty = BoardPresentation.boardContentLines(
                Category.RICHEST, boards.get(Category.RICHEST), id(3));
        eq(empty.size(), 2, "empty board lines: blank + own");
        eq(empty.get(0), "", "empty board first line blank");
        eq(empty.get(1), "Your rank: not on this board yet", "empty board own line");

        // Empty snapshot: no buttons, warming-up text.
        Map<Category, List<BoardMath.Entry>> none = Map.of();
        eq(BoardPresentation.mainFormButtons(none).size(), 0, "no boards, no buttons");
        eq(BoardPresentation.availableBoards(none).size(), 0, "no boards available");
        eq(BoardPresentation.WARMING_UP_TEXT, "Boards are warming up… Try again in a few seconds.",
                "warming-up text");

        // Form constants.
        eq(BoardPresentation.BOARD_FORM_BUTTONS[0], "← All boards", "board form back button");
        eq(BoardPresentation.BOARD_FORM_BUTTONS[1], "Close", "board form close button");
        eq(BoardPresentation.MAIN_FORM_CONTENT, "Pick a board.", "main form content");
        eq(BoardPresentation.MAIN_TITLE, "Leaderboards", "main form title");

        System.out.println("FormContentTests: " + checks + " assertions passed");
    }
}
