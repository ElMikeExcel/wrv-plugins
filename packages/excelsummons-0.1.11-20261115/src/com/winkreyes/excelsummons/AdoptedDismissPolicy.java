package com.winkreyes.excelsummons;

import java.util.UUID;

/**
 * The adopted-dismiss gate (ExcelSummons 0.1.7, Mike's ruling,
 * Nov 12, 2026, ~6:05 PM CST). Adopted pets are the player's own
 * tamed/bred animals; through 0.1.6 NO dismiss path could take
 * them at all. 0.1.7 adds exactly one door: the Summons menu's
 * per-pet, confirm-gated dismiss. This class is its pure
 * decision layer — eligibility and the audit line — so the
 * harness proves the gate the live path runs.
 *
 * The gate is deliberately narrow: the record must exist, must
 * belong to the acting owner, and must be ADOPTED origin (the
 * flow's origin check — a CAST record is refused here because
 * cast dismissal already has its scopes; this path exists only
 * for adopted pets). There is no bulk form: eligibility is
 * decided per single record, and no "dismiss all adopted"
 * selection exists anywhere.
 *
 * The command scopes are untouched: AdminPolicy.dismissTargets
 * still never selects an adopted record for MOUNT_ONLY / PETS /
 * ALL, so /summons dismiss in every form behaves exactly as
 * 0.1.6. This menu flow is the only adopted-dismiss door.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class AdoptedDismissPolicy {
    private AdoptedDismissPolicy() {}

    /** May this single record be dismissed through the adopted
     *  menu flow by this owner? */
    public static boolean eligible(SummonRecord rec, UUID owner) {
        return rec != null && owner != null
                && owner.equals(rec.ownerId())
                && rec.origin() == SummonRecord.Origin.ADOPTED;
    }

    /** The INFO audit line for one adopted dismissal. It names
     *  the acting player and the pet's spell/type key — an
     *  adopted dismissal is a permanent removal of a real animal
     *  and is recorded as what it is. */
    public static String auditLine(String actorName, UUID actorId, SummonRecord rec) {
        String who = actorName == null || actorName.isBlank() ? "UNKNOWN" : actorName;
        return "Summons adopted dismiss by " + who + " (" + actorId + "): dismissed adopted "
                + rec.spellKey() + " (" + rec.branch().name().toLowerCase() + "), entity "
                + rec.entityId() + " — permanent removal of a tamed animal, "
                + "confirm-gated menu flow.";
    }
}
