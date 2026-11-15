package com.winkreyes.excelbraingames.games;

/**
 * The shared question-loop contract the text games share (SPEC §11):
 * Quick Math, Word Scramble and Riddle of the Day all run the same
 * loop — present a prompt, take a form/chat answer, score it, advance.
 * Phase 1 shipped this interface as the seam; Phase 3 implements it in
 * games/text/TextLoop.java, backed by the saved rounds in PlayerData.
 * The GUI question card and the chat listener are two front doors onto
 * that one implementation; neither carries game rules.
 */
public interface QuestionLoop {

    /** One prompt in a text-game round. */
    record Prompt(String question, java.util.List<String> choices) {}

    /** Score an answer: correct or not, and the cents it earned toward the round. */
    record Verdict(boolean correct, long earnedCents) {}

    Prompt nextPrompt(String playerKey, String gameId, String tierId);

    Verdict answer(String playerKey, String gameId, String response);
}
