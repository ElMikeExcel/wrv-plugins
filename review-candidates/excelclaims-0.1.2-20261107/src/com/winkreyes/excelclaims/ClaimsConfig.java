package com.winkreyes.excelclaims;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * ExcelClaims — every behavioral knob from config.yml, in one place.
 * Field defaults ARE the live Wink Reyes Valley values, so a missing
 * or partial config degrades to the live behavior, never to a
 * stranger's defaults. Excel — Iris / Mike.
 */
public final class ClaimsConfig {

    public enum WorldMode { SURVIVAL, DISABLED }

    public final Map<String, WorldMode> worldModes = new HashMap<>();

    // claim blocks
    public int initialBlocks = 100;
    public int accruedPerHour = 100;
    public int maxAccrued = 80000;
    public int idleThresholdMinutes = 0;
    public int idlePercent = 0;
    public double abandonReturnRatio = 1.0;
    public int maxClaimsPerPlayer = 0;
    public double purchaseCost = 0.0;
    public double sellValue = 0.0;
    public int maxBonus = 0;

    // creation
    public int autoClaimRadius = 4;
    public int autoClaimRadiusMinimum = 0;
    public int minimumWidth = 5;
    public int minimumArea = 100;
    public int extendIntoGround = 5;
    public int claimDefaultRadius = 4;
    public Material modificationTool = Material.GOLDEN_SHOVEL;
    public Material investigationTool = Material.STICK;

    // expiration (0.1.2: per-player — Mike's ruling, Nov 7, 2026)
    // ONE inactivity threshold, settable per player. Days are
    // counted from the owner's last-seen basis; EXPIRATION_NEVER
    // (-1) means infinite. A per-player override wins over the
    // default. The code default for the default is NEVER: a config
    // that says nothing about expiration expires nothing — the
    // safe direction, and the live state Mike chose ("infinite
    // for now, with the option per player whenever we want it").
    public static final int EXPIRATION_NEVER = -1;
    public int expirationCheckMinutes = 60;
    public int expirationDefaultDays = EXPIRATION_NEVER;
    public final Map<UUID, Integer> expirationPlayerDays = new HashMap<>();

    // protection
    public boolean preventTheft = true;
    public boolean protectCreatures = true;
    public boolean preventButtonsSwitches = true;
    public boolean lockWoodenDoors = false;
    public boolean lockTrapDoors = false;
    public boolean lockFenceGates = true;
    public boolean enderPearlsRequireAccess = true;
    public boolean refundDeniedEnderPearls = true;
    public boolean raidTriggersRequireBuild = true;
    public boolean villagerTradingRequiresBuild = true;
    public boolean hoppersRequireBuild = false;
    public boolean lecternReadingRequiresAccess = true;
    public final Set<String> commandsRequiringAccess = new HashSet<>();

    // pvp
    public boolean pvpProtectPlayerClaims = true;
    public boolean pvpProtectAdminClaims = true;
    public boolean pvpProtectAdminSubdivisions = true;
    public int combatTimeoutSeconds = 15;
    public boolean punishLogout = true;

    // environment
    public boolean blockLandClaimExplosions = true;
    public boolean blockSurfaceCreeperExplosions = true;
    public boolean blockSurfaceOtherExplosions = true;
    public boolean fireSpreadsInClaims = false;
    public boolean fireDamagesInClaims = false;
    public boolean fireSpreads = false;
    public boolean fireDestroys = false;
    public String pistonMovement = "CLAIMS_ONLY";
    public boolean endermenMoveBlocks = false;
    public boolean silverfishBreakBlocks = false;
    public boolean creaturesTrampleCrops = false;
    public boolean rabbitsEatCrops = true;

    // audit
    public boolean publishToProtect = true;

    private final Map<String, String> messages = new HashMap<>();

    public WorldMode modeFor(World world) {
        if (world == null) return WorldMode.DISABLED;
        return worldModes.getOrDefault(world.getName(), WorldMode.DISABLED);
    }

    public boolean claimsEnabled(String worldName) {
        return worldModes.getOrDefault(worldName, WorldMode.DISABLED) != WorldMode.DISABLED;
    }

    // ------------------------------------------------------------ expiration (0.1.2)

    /**
     * Parse one expiration-days value from config: a number, a
     * numeric string, or "never" (any case). Any negative number
     * normalizes to EXPIRATION_NEVER. Returns null when the value
     * is absent or unparseable — callers treat null as "not set"
     * (a junk per-player value means no override; a junk default
     * leaves the NEVER field default standing).
     */
    public static Integer tryParseExpirationDays(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Number n) {
            int v = n.intValue();
            return v < 0 ? EXPIRATION_NEVER : v;
        }
        String s = raw.toString().trim();
        if (s.equalsIgnoreCase("never")) return EXPIRATION_NEVER;
        try {
            int v = Integer.parseInt(s);
            return v < 0 ? EXPIRATION_NEVER : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Config-file spelling of a days value: "never" or the count. */
    public static String formatExpirationDays(int days) {
        return days < 0 ? "never" : Integer.toString(days);
    }

    /** The threshold that governs this owner: their override if one exists, else the default. */
    public int effectiveExpirationDays(UUID owner) {
        if (owner != null) {
            Integer override = expirationPlayerDays.get(owner);
            if (override != null) return override;
        }
        return expirationDefaultDays;
    }

    public boolean hasExpirationOverride(UUID owner) {
        return owner != null && expirationPlayerDays.containsKey(owner);
    }

    /** Set (or replace) a player's override in memory — live from this moment. */
    public void setExpirationOverride(UUID owner, int days) {
        expirationPlayerDays.put(owner, days < 0 ? EXPIRATION_NEVER : days);
    }

    /**
     * The /excelclaims expire write path: record the override in
     * memory AND in the given live configuration, so the caller
     * only has to save it. The value is written in the config's
     * own spelling ("never" or a day count) under
     * expiration.players.<uuid>.
     */
    public void applyExpirationOverride(FileConfiguration cfg, UUID owner, int days) {
        setExpirationOverride(owner, days);
        cfg.set("expiration.players." + owner, formatExpirationDays(
                expirationPlayerDays.get(owner)));
    }

    public String msg(String key) {
        return messages.getOrDefault(key, key);
    }

    public String msg(String key, Map<String, String> vars) {
        String out = msg(key);
        for (Map.Entry<String, String> e : vars.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue());
        }
        return out;
    }

    public void load(FileConfiguration cfg) {
        worldModes.clear();
        if (cfg.isConfigurationSection("worlds")) {
            for (String w : cfg.getConfigurationSection("worlds").getKeys(false)) {
                String v = cfg.getString("worlds." + w, "Disabled");
                worldModes.put(w, "Survival".equalsIgnoreCase(v) ? WorldMode.SURVIVAL : WorldMode.DISABLED);
            }
        }
        initialBlocks = cfg.getInt("claim-blocks.initial", initialBlocks);
        accruedPerHour = cfg.getInt("claim-blocks.accrued-per-hour", accruedPerHour);
        maxAccrued = cfg.getInt("claim-blocks.max-accrued", maxAccrued);
        idleThresholdMinutes = cfg.getInt("claim-blocks.idle-threshold-minutes", idleThresholdMinutes);
        idlePercent = cfg.getInt("claim-blocks.idle-percent", idlePercent);
        abandonReturnRatio = cfg.getDouble("claim-blocks.abandon-return-ratio", abandonReturnRatio);
        maxClaimsPerPlayer = cfg.getInt("claim-blocks.max-claims-per-player", maxClaimsPerPlayer);
        purchaseCost = cfg.getDouble("claim-blocks.purchase-cost", purchaseCost);
        sellValue = cfg.getDouble("claim-blocks.sell-value", sellValue);
        maxBonus = cfg.getInt("claim-blocks.max-bonus", maxBonus);

        autoClaimRadius = cfg.getInt("creation.automatic-new-player-claims-radius", autoClaimRadius);
        autoClaimRadiusMinimum = cfg.getInt("creation.automatic-new-player-claims-radius-minimum", autoClaimRadiusMinimum);
        minimumWidth = cfg.getInt("creation.minimum-width", minimumWidth);
        minimumArea = cfg.getInt("creation.minimum-area", minimumArea);
        extendIntoGround = cfg.getInt("creation.extend-into-ground-distance", extendIntoGround);
        claimDefaultRadius = cfg.getInt("creation.claim-default-radius", claimDefaultRadius);
        Material mt = Material.matchMaterial(cfg.getString("creation.modification-tool", "GOLDEN_SHOVEL"));
        if (mt != null) modificationTool = mt;
        Material it = Material.matchMaterial(cfg.getString("creation.investigation-tool", "STICK"));
        if (it != null) investigationTool = it;

        expirationCheckMinutes = cfg.getInt("expiration.check-interval-minutes", expirationCheckMinutes);
        // 0.1.2 per-player expiration. Retired 0.1.0/0.1.1 keys
        // (chest-claim-days, unused-claim-days, all-claims-days-
        // inactive, except-when-owner-has-*) are simply never read
        // — an old config (including the 99999 recovery freeze)
        // loads clean and its retired values are inert.
        Integer defDays = tryParseExpirationDays(cfg.get("expiration.default-days"));
        if (defDays != null) expirationDefaultDays = defDays;
        expirationPlayerDays.clear();
        if (cfg.isConfigurationSection("expiration.players")) {
            for (String key : cfg.getConfigurationSection("expiration.players").getKeys(false)) {
                UUID id;
                try {
                    id = UUID.fromString(key.trim());
                } catch (IllegalArgumentException ignored) {
                    continue; // not a UUID — not a player entry
                }
                Integer d = tryParseExpirationDays(cfg.get("expiration.players." + key));
                if (d != null) expirationPlayerDays.put(id, d); // junk value = no override
            }
        }

        preventTheft = cfg.getBoolean("protection.prevent-theft", preventTheft);
        protectCreatures = cfg.getBoolean("protection.protect-creatures", protectCreatures);
        preventButtonsSwitches = cfg.getBoolean("protection.prevent-buttons-switches", preventButtonsSwitches);
        lockWoodenDoors = cfg.getBoolean("protection.lock-wooden-doors", lockWoodenDoors);
        lockTrapDoors = cfg.getBoolean("protection.lock-trap-doors", lockTrapDoors);
        lockFenceGates = cfg.getBoolean("protection.lock-fence-gates", lockFenceGates);
        enderPearlsRequireAccess = cfg.getBoolean("protection.ender-pearls-require-access-trust", enderPearlsRequireAccess);
        refundDeniedEnderPearls = cfg.getBoolean("protection.refund-denied-ender-pearls", refundDeniedEnderPearls);
        raidTriggersRequireBuild = cfg.getBoolean("protection.raid-triggers-require-build-trust", raidTriggersRequireBuild);
        villagerTradingRequiresBuild = cfg.getBoolean("protection.villager-trading-requires-build-trust", villagerTradingRequiresBuild);
        hoppersRequireBuild = cfg.getBoolean("protection.hoppers-require-build-trust", hoppersRequireBuild);
        lecternReadingRequiresAccess = cfg.getBoolean("protection.lectern-reading-requires-access-trust", lecternReadingRequiresAccess);
        commandsRequiringAccess.clear();
        for (String c : cfg.getStringList("protection.commands-requiring-access-trust")) {
            commandsRequiringAccess.add(c.toLowerCase());
        }
        if (commandsRequiringAccess.isEmpty()) commandsRequiringAccess.add("/sethome");

        pvpProtectPlayerClaims = cfg.getBoolean("pvp.protect-players-in-player-claims", pvpProtectPlayerClaims);
        pvpProtectAdminClaims = cfg.getBoolean("pvp.protect-players-in-admin-claims", pvpProtectAdminClaims);
        pvpProtectAdminSubdivisions = cfg.getBoolean("pvp.protect-players-in-admin-subdivisions", pvpProtectAdminSubdivisions);
        combatTimeoutSeconds = cfg.getInt("pvp.combat-timeout-seconds", combatTimeoutSeconds);
        punishLogout = cfg.getBoolean("pvp.punish-logout", punishLogout);

        blockLandClaimExplosions = cfg.getBoolean("environment.block-land-claim-explosions", blockLandClaimExplosions);
        blockSurfaceCreeperExplosions = cfg.getBoolean("environment.block-surface-creeper-explosions", blockSurfaceCreeperExplosions);
        blockSurfaceOtherExplosions = cfg.getBoolean("environment.block-surface-other-explosions", blockSurfaceOtherExplosions);
        fireSpreadsInClaims = cfg.getBoolean("environment.fire-spreads-in-claims", fireSpreadsInClaims);
        fireDamagesInClaims = cfg.getBoolean("environment.fire-damages-in-claims", fireDamagesInClaims);
        fireSpreads = cfg.getBoolean("environment.fire-spreads", fireSpreads);
        fireDestroys = cfg.getBoolean("environment.fire-destroys", fireDestroys);
        pistonMovement = cfg.getString("environment.piston-movement", pistonMovement);
        endermenMoveBlocks = cfg.getBoolean("environment.endermen-move-blocks", endermenMoveBlocks);
        silverfishBreakBlocks = cfg.getBoolean("environment.silverfish-break-blocks", silverfishBreakBlocks);
        creaturesTrampleCrops = cfg.getBoolean("environment.creatures-trample-crops", creaturesTrampleCrops);
        rabbitsEatCrops = cfg.getBoolean("environment.rabbits-eat-crops", rabbitsEatCrops);

        publishToProtect = cfg.getBoolean("audit.publish-to-excelprotect", publishToProtect);

        messages.clear();
        if (cfg.isConfigurationSection("messages")) {
            for (String k : cfg.getConfigurationSection("messages").getKeys(false)) {
                messages.put(k, cfg.getString("messages." + k, k));
            }
        }
    }
}

// ExcelClaims — Iris / Mike, for Wink Reyes Valley
