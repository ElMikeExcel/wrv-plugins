package com.winkreyes.leaderboard;

import com.winkreyes.leaderboard.core.BoardMath;
import com.winkreyes.leaderboard.paper.BoardPresentation;
import com.winkreyes.leaderboard.paper.Category;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Chat-menu suite for 1.2's Java presentation: the exact lines
 * /leaderboard prints, their click commands and kinds. Same fixture
 * shape as FormContentTests so both presentations are pinned to the
 * same snapshot. */
public final class ChatMenuTests {

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

        // Main panel.
        List<BoardPresentation.ChatLine> main = BoardPresentation.mainChatLines(boards);
        eq(main.size(), 4, "main lines: header + 3 boards");
        eq(main.get(0).text(), "━━━━ Leaderboards ━━━━", "main header text");
        eq(main.get(0).kind(), BoardPresentation.Kind.HEADER, "main header kind");
        check(main.get(0).clickCommand() == null, "header not clickable");
        eq(main.get(1).text(), "» Top Miner — P1 · 12,000", "entry line with leader");
        eq(main.get(1).clickCommand(), "/leaderboard board miner", "entry click command");
        eq(main.get(1).hoverText(), "Click to view", "entry hover");
        eq(main.get(1).kind(), BoardPresentation.Kind.ENTRY, "entry kind");
        eq(main.get(2).clickCommand(), "/leaderboard board builder", "builder click command");
        eq(main.get(3).text(), "» Richest", "empty board entry shows bare title");
        eq(main.get(3).clickCommand(), "/leaderboard board richest", "richest click command");

        // Category keys are the enum names in lowercase.
        eq(BoardPresentation.boardCommand(Category.MCMMO), "/leaderboard board mcmmo", "mcmmo key");
        eq(BoardPresentation.boardCommand(Category.JOBS), "/leaderboard board jobs", "jobs key");

        // Empty snapshot: header + warming-up note.
        List<BoardPresentation.ChatLine> none = BoardPresentation.mainChatLines(Map.of());
        eq(none.size(), 2, "empty main lines");
        eq(none.get(1).text(), BoardPresentation.WARMING_UP_TEXT, "empty main note text");
        eq(none.get(1).kind(), BoardPresentation.Kind.NOTE, "empty main note kind");

        // Board view.
        List<BoardPresentation.ChatLine> board = BoardPresentation.boardChatLines(boards, Category.MINER, id(3));
        eq(board.size(), 13, "board lines: header + 10 + own + back");
        eq(board.get(0).text(), "━━ Top Miner ━━", "board header text");
        eq(board.get(1).text(), "#1 P1 · 12,000", "board first rank");
        eq(board.get(1).kind(), BoardPresentation.Kind.RANK, "rank kind");
        eq(board.get(10).text(), "#10 P10 · 3,000", "board tenth rank (cap)");
        check(board.stream().noneMatch(l -> l.text().contains("P11")), "top-10 cap excludes 11th");
        eq(board.get(11).text(), "Your rank: #3 · 10,000", "board own rank");
        eq(board.get(11).kind(), BoardPresentation.Kind.OWN_RANK, "own rank kind");
        eq(board.get(12).text(), "« Back to boards", "back line text");
        eq(board.get(12).clickCommand(), "/leaderboard", "back line command");
        eq(board.get(12).kind(), BoardPresentation.Kind.BACK, "back line kind");

        // Viewer not on the board.
        List<BoardPresentation.ChatLine> absent = BoardPresentation.boardChatLines(boards, Category.MINER, id(99));
        eq(absent.get(11).text(), "Your rank: not on this board yet", "board own rank absent");

        // Small board: every entry shown.
        List<BoardPresentation.ChatLine> small = BoardPresentation.boardChatLines(boards, Category.BUILDER, id(22));
        eq(small.size(), 6, "small board lines: header + 3 + own + back");

        // Category with no board in the snapshot (hidden board typed by hand).
        List<BoardPresentation.ChatLine> hidden = BoardPresentation.boardChatLines(boards, Category.ELITE, id(3));
        eq(hidden.size(), 3, "hidden board lines: header + own + back");
        eq(hidden.get(0).text(), "━━ Elite Coins ━━", "hidden board header");

        System.out.println("ChatMenuTests: " + checks + " assertions passed");
    }
}
