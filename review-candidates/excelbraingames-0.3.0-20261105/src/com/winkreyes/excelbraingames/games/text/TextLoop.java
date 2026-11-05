package com.winkreyes.excelbraingames.games.text;

import com.winkreyes.excelbraingames.PlayerData;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * The ONE shared question-loop module for the text games (SPEC §11,
 * implemented in Phase 3): Quick Math, Word Scramble and Riddle of the
 * Day all run through this loop — present a prompt, take a form/chat
 * answer, score it, advance. The GUI card and the chat listener are two
 * front doors onto this same loop; neither carries game rules.
 *
 * State lives in the player's saved rounds (PlayerData), so the loop is
 * stateless itself. Money is NOT moved here: a Verdict reports what an
 * answer earned toward its round, and GameService settles finished
 * rounds through the shared ledger (cap, streak, kid-tier rule) exactly
 * like the grid games.
 *
 * Game ids: "quickmath", "scramble", "riddle".
 */
public final class TextLoop implements com.winkreyes.excelbraingames.games.QuestionLoop {

    private final Function<UUID, PlayerData> dataFor;
    private final java.util.function.Supplier<List<RiddleText.Riddle>> riddles;
    private final java.util.function.IntSupplier todayRiddleIndex;

    public TextLoop(Function<UUID, PlayerData> dataFor,
                    java.util.function.Supplier<List<RiddleText.Riddle>> riddles,
                    java.util.function.IntSupplier todayRiddleIndex) {
        this.dataFor = dataFor;
        this.riddles = riddles;
        this.todayRiddleIndex = todayRiddleIndex;
    }

    private PlayerData data(String playerKey) {
        try { return dataFor.apply(UUID.fromString(playerKey)); }
        catch (IllegalArgumentException e) { return null; }
    }

    @Override
    public Prompt nextPrompt(String playerKey, String gameId, String tierId) {
        PlayerData pd = data(playerKey);
        if (pd == null) return new Prompt("", List.of());
        return switch (gameId) {
            case "quickmath" -> {
                QuickMathRound r = pd.quickMath;
                if (r == null || r.finished()) yield new Prompt("", List.of());
                List<String> choices = r.currentChoices().stream().map(String::valueOf).toList();
                yield new Prompt(r.currentPrompt(), choices);
            }
            case "scramble" -> {
                ScrambleRound r = pd.scramble;
                if (r == null || r.finished()) yield new Prompt("", List.of());
                yield new Prompt(r.currentScrambled(), r.currentChoices());
            }
            case "riddle" -> {
                List<RiddleText.Riddle> list = riddles.get();
                int idx = todayRiddleIndex.getAsInt();
                if (idx < 0 || idx >= list.size()) yield new Prompt("", List.of());
                yield new Prompt(list.get(idx).question(), List.of());
            }
            default -> new Prompt("", List.of());
        };
    }

    @Override
    public Verdict answer(String playerKey, String gameId, String response) {
        PlayerData pd = data(playerKey);
        if (pd == null || response == null) return new Verdict(false, 0);
        return switch (gameId) {
            case "quickmath" -> {
                QuickMathRound r = pd.quickMath;
                if (r == null || r.finished()) yield new Verdict(false, 0);
                int value;
                try { value = Integer.parseInt(response.trim()); }
                catch (NumberFormatException e) { yield new Verdict(false, 0); } // not a number: no attempt spent
                yield new Verdict(r.answer(value), 0); // QM earnings crystallize at round end (SPEC §6.3 rounding)
            }
            case "scramble" -> {
                ScrambleRound r = pd.scramble;
                if (r == null || r.finished()) yield new Verdict(false, 0);
                long before = r.earnedCents();
                int scored = r.answer(response);
                yield new Verdict(scored == 1, r.earnedCents() - before);
            }
            case "riddle" -> {
                List<RiddleText.Riddle> list = riddles.get();
                int idx = todayRiddleIndex.getAsInt();
                if (idx < 0 || idx >= list.size() || pd.riddleSolved
                        || pd.riddleAttempts >= RiddleText.MAX_ATTEMPTS) yield new Verdict(false, 0);
                pd.riddleAttempts++;
                boolean ok = RiddleText.matches(response, list.get(idx));
                if (ok) {
                    pd.riddleSolved = true;
                    yield new Verdict(true, RiddleText.payoutCents(pd.riddleHints));
                }
                yield new Verdict(false, 0);
            }
            default -> new Verdict(false, 0);
        };
    }
}
