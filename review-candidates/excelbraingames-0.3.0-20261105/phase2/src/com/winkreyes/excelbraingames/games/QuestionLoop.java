package com.winkreyes.excelbraingames.games;

/**
 * PHASE 3 STUB — the shared question-loop module the text games will share
 * (SPEC §11): Quick Math, Word Scramble and Riddle of the Day all run the
 * same loop — present a prompt, take a form/chat answer, score it, advance.
 * Phase 1 builds only the framework + the two grid games, so this interface
 * is the seam they will plug into; there is deliberately NO game logic here.
 */
public interface QuestionLoop {

    /** One prompt in a text-game round. */
    record Prompt(String question, java.util.List<String> choices) {}

    /** Score an answer: correct or not, and the cents it earned toward the round. */
    record Verdict(boolean correct, long earnedCents) {}

    Prompt nextPrompt(String playerKey, String gameId, String tierId);

    Verdict answer(String playerKey, String gameId, String response);
}
