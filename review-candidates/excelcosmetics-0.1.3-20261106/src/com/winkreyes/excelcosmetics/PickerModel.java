package com.winkreyes.excelcosmetics;

import com.winkreyes.excelcosmetics.core.Catalog;
import com.winkreyes.excelcosmetics.core.Grant;
import com.winkreyes.excelcosmetics.core.Kind;
import com.winkreyes.excelcosmetics.core.PlayerState;
import com.winkreyes.excelcosmetics.core.TimeText;

import java.util.ArrayList;
import java.util.List;

/** The picker's content, shared by both doors (Java inventory GUI
 *  and Bedrock Cumulus form) so the two can never drift apart:
 *  every ACTIVE grant, its remaining time, and what is equipped.
 *  This is the one place a player can see remaining time — they
 *  came looking for it; nothing is ever pushed at them. */
final class PickerModel {

    record Entry(Kind kind, String id, String title, String detail, Integer rgb,
                 boolean equipped, boolean equippable) {}

    private PickerModel() {}

    static List<Entry> entries(Catalog catalog, PlayerState state, long now) {
        List<Entry> out = new ArrayList<>();
        if (state == null) return out;
        for (Grant g : state.grants()) {
            if (!g.isActiveAt(now)) continue;
            String title = catalog.displayName(g.kind(), g.id());
            String detail = TimeText.remaining(g, now);
            switch (g.kind()) {
                case COLOR -> {
                    Catalog.ColorItem c = catalog.colors().get(g.id());
                    out.add(new Entry(g.kind(), g.id(), title, detail,
                            c != null ? c.rgb() : null,
                            g.id().equals(state.equippedColorId()), true));
                }
                case EFFECT -> out.add(new Entry(g.kind(), g.id(), title, detail, null,
                        g.id().equals(state.equippedEffectId()), true));
                case SET -> {
                    Catalog.SetItem s = catalog.sets().get(g.id());
                    out.add(new Entry(g.kind(), g.id(), title, detail,
                            s != null ? s.rgb() : null,
                            g.id().equals(state.equippedSetId()), true));
                }
                case EMOTES -> out.add(new Entry(g.kind(), g.id(), title,
                        detail + " — always on while you hold it", null, false, false));
            }
        }
        return out;
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
