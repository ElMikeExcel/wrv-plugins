package me.ryanhamshire.GriefPrevention;

/**
 * ExcelClaims facade — result of a DataStore.createClaim call, same
 * public shape as the GriefPrevention API type it stands in for.
 * Part of ExcelClaims (Iris / Mike).
 */
public class CreateClaimResult {
    public boolean succeeded;
    public Claim claim;

    public CreateClaimResult() { }
}
