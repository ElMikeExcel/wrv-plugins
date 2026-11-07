package me.ryanhamshire.GriefPrevention;

/**
 * ExcelClaims facade — the claim-permission ladder, with the same
 * constants and implication shape as the public GriefPrevention API
 * (Edit &gt; Build &gt; Container &gt; Access; Manage apart;
 * Inventory = view-only container trust). Clean-room: written from
 * the API's observable behavior. Part of ExcelClaims (Iris / Mike).
 */
public enum ClaimPermission {
    Edit,
    Build,
    Container,
    Access,
    Manage,
    Inventory;

    /** True when holding {@code other} also grants this permission. */
    public boolean isGrantedBy(ClaimPermission other) {
        if (other == null) return false;
        switch (this) {
            case Edit:
                return other == Edit;
            case Build:
                return other == Edit || other == Build;
            case Container:
                return other == Edit || other == Build || other == Container;
            case Access:
                return other == Edit || other == Build || other == Container
                        || other == Access || other == Inventory;
            case Manage:
                return other == Manage;
            case Inventory:
                return other == Edit || other == Build || other == Container || other == Inventory;
            default:
                return false;
        }
    }
}
