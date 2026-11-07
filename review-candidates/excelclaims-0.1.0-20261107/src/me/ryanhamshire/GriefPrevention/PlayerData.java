package me.ryanhamshire.GriefPrevention;

import com.winkreyes.excelclaims.Core;
import com.winkreyes.excelclaims.PlayerRecord;

import java.util.UUID;
import java.util.Vector;

/**
 * ExcelClaims facade — a player's claim-block account, API-shaped
 * like the public GriefPrevention PlayerData type it stands in for.
 * Part of ExcelClaims (Iris / Mike).
 */
public class PlayerData {

    public UUID playerID;

    final PlayerRecord record;
    private final Core core;

    public PlayerData(PlayerRecord record, Core core) {
        this.record = record;
        this.core = core;
        this.playerID = record.id;
    }

    public synchronized int getAccruedClaimBlocks() { return record.accrued; }

    public void setAccruedClaimBlocks(Integer blocks) {
        if (blocks == null) return;
        record.accrued = Math.max(0, blocks);
        core.store().savePlayer(record);
    }

    public int getBonusClaimBlocks() { return record.bonus; }

    public void setBonusClaimBlocks(Integer blocks) {
        if (blocks == null) return;
        record.bonus = Math.max(0, blocks);
        core.store().savePlayer(record);
    }

    public int getRemainingClaimBlocks() {
        return core.store().remainingBlocks(record.id);
    }

    /** This player's claims (top-level and subdivisions). */
    public Vector<Claim> getClaims() {
        Vector<Claim> out = new Vector<>();
        for (com.winkreyes.excelclaims.ClaimRecord r : core.store().claimsOwnedBy(record.id)) {
            out.add(core.facadeFor(r));
        }
        return out;
    }

    public int getAccruedClaimBlocksLimit() {
        return record.accruedLimitOverride >= 0 ? record.accruedLimitOverride : core.config().maxAccrued;
    }

    public void setAccruedClaimBlocksLimit(int limit) {
        record.accruedLimitOverride = limit;
        core.store().savePlayer(record);
    }
}
