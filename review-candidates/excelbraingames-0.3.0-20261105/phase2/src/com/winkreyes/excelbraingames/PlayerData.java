package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.core.Ledger;
import com.winkreyes.excelbraingames.games.memory.MemoryRound;
import com.winkreyes.excelbraingames.games.sudoku.SudokuRound;
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

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name == null ? "" : name;
    }

    public boolean hasAnyRound() {
        return wordSearch != null || memory != null || sudoku != null;
    }
}
