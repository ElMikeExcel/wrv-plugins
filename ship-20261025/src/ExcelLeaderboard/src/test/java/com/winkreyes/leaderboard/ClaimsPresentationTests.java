package com.winkreyes.leaderboard;

import com.winkreyes.leaderboard.core.BoardMath;
import com.winkreyes.leaderboard.paper.BoardPresentation;
import com.winkreyes.leaderboard.paper.Category;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The Land Claimed board's rows carry two numbers: blocks claimed
 * (the ranked value) and blocks still claimable (the detail suffix).
 * These checks pin the exact strings every presentation shows, and
 * that boards without details render exactly as before. */
public final class ClaimsPresentationTests {

    private static int checks = 0;

    private static void eq(Object actual, Object expected, String label) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static UUID id(int n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    public static void main(String[] args) {
        List<BoardMath.Entry> board = BoardMath.rank(List.of(
                new BoardMath.Entry(id(1), "Avery", 1_234),
                new BoardMath.Entry(id(2), "Mike", 500),
                new BoardMath.Entry(id(3), "Rowan", 0)));
        Map<UUID, String> details = Map.of(
                id(1), "876 left",
                id(2), "1,600 left",
                id(3), "100 left");

        // Ranked rows show claimed + still-claimable.
        List<String> lines = BoardPresentation.rankedLines(Category.CLAIMS, board, 10, details);
        eq(lines.get(0), "#1 Avery · 1,234 · 876 left", "claims row 1");
        eq(lines.get(1), "#2 Mike · 500 · 1,600 left", "claims row 2");
        eq(lines.get(2), "#3 Rowan · 0 · 100 left", "claims row 3");

        // The viewer's own line carries both numbers too.
        eq(BoardPresentation.ownRankLine(Category.CLAIMS, board, id(2), details),
                "Your rank: #2 · 500 · 1,600 left", "claims own rank");
        eq(BoardPresentation.ownRankLine(Category.CLAIMS, board, id(99), details),
                "Your rank: not on this board yet", "claims own rank absent");

        // Form content uses the same lines.
        List<String> content = BoardPresentation.boardContentLines(
                Category.CLAIMS, board, id(1), details);
        eq(content.get(0), "#1 Avery · 1,234 · 876 left", "claims form row 1");
        eq(content.get(content.size() - 1), "Your rank: #1 · 1,234 · 876 left", "claims form own rank");

        // Chat lines use the same lines.
        Map<Category, List<BoardMath.Entry>> boards = Map.of(Category.CLAIMS, board);
        List<BoardPresentation.ChatLine> chat = BoardPresentation.boardChatLines(
                boards, Category.CLAIMS, id(3), details);
        eq(chat.get(1).text(), "#1 Avery · 1,234 · 876 left", "claims chat row 1");
        eq(chat.get(chat.size() - 2).text(), "Your rank: #3 · 0 · 100 left", "claims chat own rank");

        // A missing detail falls back to the bare value (no dangling
        // separator), and boards that never had details are unchanged.
        List<String> partial = BoardPresentation.rankedLines(
                Category.CLAIMS, board, 10, Map.of(id(1), "876 left"));
        eq(partial.get(1), "#2 Mike · 500", "missing detail falls back");
        List<String> plain = BoardPresentation.rankedLines(Category.CLAIMS, board, 10);
        eq(plain.get(0), "#1 Avery · 1,234", "no details renders as before");

        // The board is reachable under its own key, like every board.
        eq(BoardPresentation.boardCommand(Category.CLAIMS),
                "/leaderboard board claims", "claims command key");
        eq(Category.byId("claims"), Category.CLAIMS, "claims by id");

        System.out.println("ClaimsPresentationTests: " + checks + " assertions passed");
    }
}
