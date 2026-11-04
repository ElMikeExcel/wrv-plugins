package com.winkreyesvalley.excelmagiclevel.adapter.magic;

import com.elmakers.mine.bukkit.api.magic.Mage;
import com.winkreyesvalley.excelmagiclevel.core.Curve;
import com.winkreyesvalley.excelmagiclevel.core.ModifierTemplates;

import java.util.ArrayList;
import java.util.List;

/**
 * ExcelMagicLevel — applies and repairs a mage's Magic Level modifier
 * (SPEC §0.2, §7.2). Boosts are Magic-native transient modifiers with
 * duration 0: Magic saves them into the mage's own save file and restores
 * them on load, so a boost survives relog and restart with no companion
 * code running. This class's whole job is keeping exactly one invariant:
 * <b>at most one {@code wrv_magiclevel_*} key on a mage, and it is the
 * current level's</b> (none at level 1).
 *
 * <p>Reconcile only ever touches keys with our prefix — Boon/Cloak/Force
 * reserves and every other house modifier compose additively and are
 * never ours to remove (SPEC §7.4).</p>
 */
public final class BoostApplier {
    /** What a reconcile did — reported verbatim in logs and rig evidence. */
    public record ReconcileOutcome(List<String> removed, String added, boolean alreadyCorrect) {
    }

    private final String prefix;
    private final Curve curve;

    public BoostApplier(String prefix, Curve curve) {
        this.prefix = prefix;
        this.curve = curve;
    }

    /** The modifier key a level should carry, or null at level 1. */
    public String expectedKey(int level) {
        return level >= 2 ? ModifierTemplates.keyFor(prefix, level, curve.maxLevel()) : null;
    }

    /**
     * Enforce the invariant on this mage for this level. Idempotent; safe
     * to call on every MageLoadEvent and every level change, in either
     * direction (a curve retune can move a level down as well as up).
     */
    public ReconcileOutcome reconcile(Mage mage, int level) {
        String expected = expectedKey(level);
        List<String> removed = new ArrayList<>();
        for (String key : new ArrayList<>(mage.getModifierKeys())) {
            if (ModifierTemplates.isOurs(prefix, key) && !key.equals(expected)) {
                mage.removeModifier(key);
                removed.add(key);
            }
        }
        String added = null;
        boolean alreadyCorrect = removed.isEmpty() && (expected == null || mage.hasModifier(expected));
        if (expected != null && !mage.hasModifier(expected)) {
            if (mage.addModifier(expected)) {
                added = expected;
            }
        }
        return new ReconcileOutcome(removed, added, alreadyCorrect && added == null);
    }

    /**
     * Remove EVERY ExcelMagicLevel modifier from this mage (SPEC §7.3 —
     * the uninstall edge: boosts live in Magic's saves, so stripping is a
     * tool, not documentation). Returns how many were removed.
     */
    public int strip(Mage mage) {
        int count = 0;
        for (String key : new ArrayList<>(mage.getModifierKeys())) {
            if (ModifierTemplates.isOurs(prefix, key)) {
                mage.removeModifier(key);
                count++;
            }
        }
        return count;
    }
}
