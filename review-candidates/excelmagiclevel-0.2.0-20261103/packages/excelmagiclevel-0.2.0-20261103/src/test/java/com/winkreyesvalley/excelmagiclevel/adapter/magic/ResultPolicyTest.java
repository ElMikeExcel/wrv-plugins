package com.winkreyesvalley.excelmagiclevel.adapter.magic;

import com.elmakers.mine.bukkit.api.spell.SpellResult;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * ExcelMagicLevel — the paying-result policy, pinned over EVERY SpellResult
 * value (SPEC §4.1 rule 2). If a future Magic version adds a result, this
 * test forces an explicit decision about it instead of a silent default.
 */
public class ResultPolicyTest {
    @Test
    public void payingSetIsExactlyTheSpecSet() {
        Set<SpellResult> expected = EnumSet.of(
                SpellResult.CAST, SpellResult.CAST_SELF, SpellResult.CAST_TARGET,
                SpellResult.ALTERNATE, SpellResult.ALTERNATE_UP, SpellResult.ALTERNATE_DOWN,
                SpellResult.ALTERNATE_SNEAK, SpellResult.ALTERNATE_JUMPING,
                SpellResult.ALTERNATE_SNEAK_UP, SpellResult.ALTERNATE_SNEAK_DOWN,
                SpellResult.ALTERNATE_JUMPING_UP, SpellResult.ALTERNATE_JUMPING_DOWN);
        Set<SpellResult> actual = EnumSet.noneOf(SpellResult.class);
        for (SpellResult result : SpellResult.values()) {
            if (ResultPolicy.pays(result)) actual.add(result);
        }
        assertEquals(expected, actual);
    }

    @Test
    public void namedExclusionsPayNothing() {
        // The traps: all four are isSuccess() == true in Magic's enum.
        assertFalse(ResultPolicy.pays(SpellResult.DEACTIVATE));
        assertFalse(ResultPolicy.pays(SpellResult.REACTIVATE));
        assertFalse(ResultPolicy.pays(SpellResult.STOP));
        assertFalse(ResultPolicy.pays(SpellResult.PENDING));
        assertFalse(ResultPolicy.pays(SpellResult.CANCELLED));
        assertFalse(ResultPolicy.pays(null));
    }
}
