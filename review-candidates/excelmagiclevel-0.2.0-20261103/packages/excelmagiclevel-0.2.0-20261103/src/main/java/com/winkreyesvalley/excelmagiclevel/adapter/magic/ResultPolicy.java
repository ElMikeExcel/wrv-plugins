package com.winkreyesvalley.excelmagiclevel.adapter.magic;

import com.elmakers.mine.bukkit.api.spell.SpellResult;

/**
 * ExcelMagicLevel — which cast results pay XP (SPEC §4.1 rule 2). Pays:
 * CAST, CAST_SELF, CAST_TARGET, and every ALTERNATE* variant. Explicitly
 * excluded even though the enum calls some of them "success":
 * DEACTIVATE (turning a toggle OFF is not casting), REACTIVATE (reload
 * bookkeeping), STOP / PENDING (not completed casts), CANCELLED (refunded,
 * not a cast). Everything else is a failure or neutral and pays nothing —
 * which {@code isSuccess()} alone would NOT guarantee, hence this explicit
 * policy, pinned by a unit test over every enum value.
 */
public final class ResultPolicy {
    private ResultPolicy() {
    }

    public static boolean pays(SpellResult result) {
        if (result == null) return false;
        return result == SpellResult.CAST
                || result == SpellResult.CAST_SELF
                || result == SpellResult.CAST_TARGET
                || result.isAlternate();
    }
}
