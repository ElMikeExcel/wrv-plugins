package com.winkreyes.excelexpansions.core;
import java.util.OptionalInt; import java.util.UUID;
/** Watcher band source backed by ExcelExpansions' own persistent level store
 *  (Mike, Nov 14 2026). The band signal is the EM-sourced HighestLevelKilled
 *  persisted in the store - a NAMED EliteMobs field, not a computed combat
 *  level: EM stores no canonical combat level (em_player_data.db has Score /
 *  HighestLevelKilled / Kills, no level column - proven, LOCK-PREP #16).
 *  A player with no store entry, or an entry whose signal was never read,
 *  counts as NOT in band; the watcher never estimates (DESIGN 2.2).
 *  Which signal is canonical for IN-BAND (this store signal vs the in-game
 *  /em profile value vs guild rank) is the rig-owed #16 decision. */
public final class StoreCombatLevelSource implements CombatLevelSource {
 private final LevelStore store;
 public StoreCombatLevelSource(LevelStore store){ this.store=store; }
 public OptionalInt combatLevel(UUID player){
  return store.get(player)
    .filter(e -> e.highestLevelKilled()!=null)
    .map(e -> OptionalInt.of(e.highestLevelKilled()))
    .orElse(OptionalInt.empty());
 }
 public String name(){ return "ExcelExpansions level store (band signal: EliteMobs PlayerData.HighestLevelKilled; canonical-source decision rig-owed, #16)"; }
}
