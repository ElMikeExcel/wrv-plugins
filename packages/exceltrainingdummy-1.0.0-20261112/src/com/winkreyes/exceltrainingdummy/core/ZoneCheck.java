package com.winkreyes.exceltrainingdummy.core;

/**
 * Spawn-zone verdict (DESIGN §1): a dummy may be spawned in the
 * wilderness, on the spawner's OWN claim, or in an admin training
 * zone. Conservative by construction:
 *  - no claim at the spot            -> wilderness, allowed;
 *  - the spawner owns the claim      -> allowed (subclaims resolve
 *    to their parent's owner before this check runs);
 *  - an admin claim                  -> allowed ONLY for a player
 *    holding the admin permission (staff-run training zones); a
 *    plain player may not camp a dummy in an admin claim;
 *  - anyone else's claim             -> denied, even with build
 *    trust: trust to build is not a training ground grant.
 * The Bukkit layer resolves the claim facts (via the claims plugin,
 * reflection — see ClaimGate) and feeds them here.
 */
public final class ZoneCheck {
    private ZoneCheck() {}

    public enum Verdict {
        WILDERNESS,
        OWN_CLAIM,
        ADMIN_ZONE,
        DENIED_FOREIGN_CLAIM,
        DENIED_ADMIN_CLAIM;

        public boolean allowed() {
            return this == WILDERNESS || this == OWN_CLAIM || this == ADMIN_ZONE;
        }
    }

    public static Verdict evaluate(boolean claimPresent, boolean adminClaim,
                                   boolean spawnerOwns, boolean spawnerHasAdminPermission) {
        if (!claimPresent) return Verdict.WILDERNESS;
        if (adminClaim) {
            return spawnerHasAdminPermission ? Verdict.ADMIN_ZONE : Verdict.DENIED_ADMIN_CLAIM;
        }
        if (spawnerOwns) return Verdict.OWN_CLAIM;
        return Verdict.DENIED_FOREIGN_CLAIM;
    }
}
