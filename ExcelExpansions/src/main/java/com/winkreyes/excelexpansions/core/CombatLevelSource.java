package com.winkreyes.excelexpansions.core;
import java.util.OptionalInt; import java.util.UUID;
/** LOCK-PREP #16 carried exactly: source TBD at build: /em profile (rig) or guild rank - DB has no combat-level column (proven).
 *  Implementations must return empty when the level cannot be read; the watcher never estimates a level (DESIGN 2.2). */
public interface CombatLevelSource { OptionalInt combatLevel(UUID player); String name(); }
