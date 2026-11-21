package com.winkreyes.excelquestxp;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pure grant policy (no Bukkit calls): weapon-skill target selection,
 * Armor XP at the 1/3 combat rate, and the exactly-once claim ledger.
 *
 * <p>Skill target (D-W3-1, amended by D159 in 1.1.0): quest XP goes to
 * the player's chosen target — a pinned weapon skill when the player
 * pinned one, otherwise their current highest weapon skill — plus Armor
 * XP at the standard 1/3 combat rate, the same split a kill pays. The
 * default (no pin) is exactly the pre-1.1.0 behavior. Ties in the
 * highest-skill rule pick the first skill in EliteMobs' weapon-skill
 * order, so the choice is deterministic for a fresh player (all 0).
 */
public final class GrantPolicy {

    private GrantPolicy() {}

    /** Armor XP for a weapon-XP grant: xp / 3, integer division (EM's own rule). */
    public static long armorXp(long weaponXp) {
        return weaponXp / 3;
    }

    /**
     * The D159 target resolution, used verbatim by {@link SkillGrant}:
     * a pinned weapon skill (index into EliteMobs' weapon-skill order)
     * wins even when another skill is higher; {@code pinnedIndex < 0}
     * (or out of range, e.g. a stale pin) means HIGHEST mode and defers
     * to {@link #highestWeaponIndex} — the pre-1.1.0 rule, unchanged.
     */
    public static int resolveTargetIndex(long[] weaponXp, int pinnedIndex) {
        if (pinnedIndex >= 0 && pinnedIndex < weaponXp.length) {
            return pinnedIndex;
        }
        return highestWeaponIndex(weaponXp);
    }

    /**
     * Index of the highest value in {@code weaponXp}; ties resolve to the
     * lowest index (first in EliteMobs' weapon-skill order). Returns -1 for
     * an empty array.
     */
    public static int highestWeaponIndex(long[] weaponXp) {
        int best = -1;
        long bestXp = Long.MIN_VALUE;
        for (int i = 0; i < weaponXp.length; i++) {
            if (weaponXp[i] > bestXp) {
                bestXp = weaponXp[i];
                best = i;
            }
        }
        return best;
    }

    /**
     * Exactly-once ledger for completion grants. A quest completion event
     * must grant at most once per player per quest, even if EliteMobs fires
     * the event twice for one turn-in. The once-only quest marker is the
     * durable guard; this ledger is the in-session guard against double
     * fire. {@link #tryClaim} returns false when the key was already
     * claimed. Claims are intentionally never expired: a once-only quest
     * cannot legitimately complete twice for the same player.
     */
    public static final class ClaimLedger {
        private final java.util.Set<String> claimed = ConcurrentHashMap.newKeySet();

        public static String key(UUID player, String questKey) {
            return player.toString() + "|" + questKey;
        }

        public boolean tryClaim(UUID player, String questKey) {
            return claimed.add(key(player, questKey));
        }

        public void release(UUID player, String questKey) {
            claimed.remove(key(player, questKey));
        }

        public boolean isClaimed(UUID player, String questKey) {
            return claimed.contains(key(player, questKey));
        }

        public int size() {
            return claimed.size();
        }
    }
}
