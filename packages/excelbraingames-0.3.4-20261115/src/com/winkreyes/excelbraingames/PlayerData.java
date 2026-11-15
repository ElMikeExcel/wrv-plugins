package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.core.Ledger;
import com.winkreyes.excelbraingames.games.memory.MemoryRound;
import com.winkreyes.excelbraingames.games.sudoku.SudokuRound;
import com.winkreyes.excelbraingames.games.text.QuickMathRound;
import com.winkreyes.excelbraingames.games.text.ScrambleRound;
import com.winkreyes.excelbraingames.games.wordsearch.WordSearchRound;

import java.util.UUID;

/**
 * One player's Brain Games state: the earnings ledger plus at most one
 * active round per game (SPEC §9.5). Persisted by DataStore as YAML.
 */
public final class PlayerData {
    public final UUID uuid;
    public String name;
    public final Ledger ledger = new Ledger();
    public WordSearchRound wordSearch;
    public MemoryRound memory;
    public SudokuRound sudoku;
    public QuickMathRound quickMath;
    public ScrambleRound scramble;
    // Riddle of the Day, per player per day (SPEC §9.5: attempts + hints,
    // resets at the day boundary — the date field is the reset mechanism).
    public String riddleDate = "";
    public int riddleAttempts;
    public int riddleHints;
    public boolean riddleSolved;
    public boolean riddlePaid;

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name == null ? "" : name;
    }

    /** Roll the riddle state over when the stored date isn't today. */
    public void riddleRoll(String today) {
        if (!today.equals(riddleDate)) {
            riddleDate = today;
            riddleAttempts = 0;
            riddleHints = 0;
            riddleSolved = false;
            riddlePaid = false;
        }
    }

    public boolean hasAnyRound() {
        return wordSearch != null || memory != null || sudoku != null || quickMath != null || scramble != null;
    }
}
