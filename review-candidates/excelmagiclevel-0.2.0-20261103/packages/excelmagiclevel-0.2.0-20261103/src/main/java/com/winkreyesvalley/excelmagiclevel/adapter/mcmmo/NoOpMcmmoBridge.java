package com.winkreyesvalley.excelmagiclevel.adapter.mcmmo;

/**
 * ExcelMagicLevel — the v1 mcMMO bridge: deliberately a no-op (SPEC §9.2).
 * mcMMO absent, mid-update, or API-moved is indistinguishable from this
 * bridge, and that is the point — nothing in v1's function can break,
 * because no mcMMO code path is in the loop. Progress is nevertheless
 * kept mcMMO-expressible (see core.ExportRows) so a future migration is
 * an import, not a reset.
 */
public final class NoOpMcmmoBridge implements McmmoBridge {
    @Override
    public boolean isConnected() {
        return false;
    }
}
