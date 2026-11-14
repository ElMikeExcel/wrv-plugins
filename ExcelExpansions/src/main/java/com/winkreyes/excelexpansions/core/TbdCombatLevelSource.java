package com.winkreyes.excelexpansions.core;
import java.util.OptionalInt; import java.util.UUID;
/** Placeholder plumbing for #16: no source identified yet, so every lookup is unknown -> player counts as NOT in band, gap logged. */
public final class TbdCombatLevelSource implements CombatLevelSource {
 public OptionalInt combatLevel(UUID p){return OptionalInt.empty();}
 public String name(){return "TBD at build: /em profile (rig) or guild rank - DB has no combat-level column (proven)";}
}
