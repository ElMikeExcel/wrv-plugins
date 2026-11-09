package me.ryanhamshire.GriefPrevention;

import com.winkreyes.excelclaims.Core;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ExcelClaims — registered under the GriefPrevention name and package
 * ON PURPOSE: every Wink Reyes Valley plugin that links, probes, or
 * looks up the claims API (ExcelLandmarks, ExcelAutoSort, ExcelProtect,
 * ExcelAuditLog, ExcelGuide, ExcelLeaderboard, ExcelShops,
 * ExcelBossTrial, the menus — and Magic) finds the exact surface it
 * was built against, so the swap needs zero rebuilds anywhere else.
 * The engine is ours (com.winkreyes.excelclaims), clean-room; every
 * player-facing string says ExcelClaims. (Iris / Mike)
 */
public class GriefPrevention extends JavaPlugin {

    public static GriefPrevention instance;
    public DataStore dataStore;

    // Read directly by Magic's protection integration.
    public boolean config_pvp_noCombatInPlayerLandClaims = true;
    public boolean config_pvp_noCombatInAdminLandClaims = true;
    public boolean config_pvp_noCombatInAdminSubdivisions = true;

    private Core core;

    @Override
    public void onEnable() {
        instance = this;
        core = new Core(this);
        core.enable();
        this.dataStore = new DataStore(core);
        this.config_pvp_noCombatInPlayerLandClaims = core.config().pvpProtectPlayerClaims;
        this.config_pvp_noCombatInAdminLandClaims = core.config().pvpProtectAdminClaims;
        this.config_pvp_noCombatInAdminSubdivisions = core.config().pvpProtectAdminSubdivisions;
    }

    @Override
    public void onDisable() {
        if (core != null) core.disable();
        instance = null;
    }

    /** The ExcelClaims engine behind this facade (our own plugins may use it directly). */
    public Core excelClaimsCore() { return core; }

    public boolean claimsEnabledForWorld(World world) {
        return core != null && core.config().modeFor(world) != com.winkreyes.excelclaims.ClaimsConfig.WorldMode.DISABLED;
    }
}
