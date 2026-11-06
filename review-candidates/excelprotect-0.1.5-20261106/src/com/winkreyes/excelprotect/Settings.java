package com.winkreyes.excelprotect;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * ExcelProtect — every toggle and threshold from config.yml (house adjustability law).
 * Defaults implement SPEC §2: everything ON except players.chat, blocks.form-fade-grow,
 * blocks.liquid-flow and blocks.pistons. Excel — Wink Reyes Valley
 */
public final class Settings {
    private final FileConfiguration c;

    public Settings(FileConfiguration c) { this.c = c; }

    private boolean on(String key, boolean def) { return c.getBoolean("log." + key, def); }

    // blocks
    public boolean blocksPlayer() { return on("blocks.player", true); }
    public boolean blocksNatural() { return on("blocks.natural", true); }
    public boolean blocksBurn() { return on("blocks.burn", true); }
    public boolean blocksExplosions() { return on("blocks.explosions", true); }
    public boolean blocksFormFadeGrow() { return on("blocks.form-fade-grow", false); }
    public boolean blocksLiquid() { return on("blocks.liquid-flow", false); }
    public boolean blocksPistons() { return on("blocks.pistons", false); }
    // containers / entities / items
    public boolean containers() { return on("containers.transactions", true); }
    public boolean entityDeaths() { return on("entities.deaths", true); }
    public boolean entityChanges() { return on("entities.equipment-changes", true); }
    public boolean entitySpawns() { return on("entities.spawns", true); }
    public boolean entityTames() { return on("entities.tames", true); }
    public boolean itemDrops() { return on("items.drops", true); }
    public boolean itemPickups() { return on("items.pickups", true); }
    public boolean itemDespawns() { return on("items.despawns", true); }
    // players
    public boolean joins() { return on("players.joins", true); }
    public boolean leaves() { return on("players.leaves", true); }
    public boolean teleports() { return on("players.teleports", true); }
    public boolean commands() { return on("players.commands", true); }
    public boolean chat() { return on("players.chat", false); }
    public boolean playerDeaths() { return on("players.deaths", true); }
    public boolean progression() { return on("players.progression", true); }
    // economy / audit / combat / deaths / claims
    public boolean ecoShop() { return on("economy.shop-purchases", true); }
    public boolean ecoMoney() { return on("economy.money-deltas", true); }
    public boolean ecoRewards() { return on("economy.reward-adjustments", true); }
    public boolean ecoCharges() { return on("economy.service-charges", true); }
    public boolean ecoCasts() { return on("economy.service-casts", true); }
    public boolean auditXp() { return on("audit.xp-levels", true); }
    public boolean auditEnchants() { return on("audit.enchants", true); }
    public boolean auditEnderchest() { return on("audit.enderchest", true); }
    public boolean combatDamage() { return on("combat.damage", true); }
    public boolean combatHealing() { return on("combat.healing", true); }
    public boolean combatFights() { return on("combat.fight-summaries", true); }
    public boolean damageIncidents() { return on("combat.damage-incidents", true); }
    public boolean damageDealt() { return on("combat.damage-dealt", true); }
    public boolean healingIncidents() { return on("combat.healing-incidents", true); }
    public boolean deathSnapshots() { return on("deaths.snapshots", true); }
    public boolean burnLedger() { return on("deaths.burn-ledger", true); }
    public boolean claimEvents() { return on("claims.events", true); }
    // locations (0.1.4): the timeline stream — samples + anchors
    public boolean locations() { return on("locations", true); }
    public boolean inventorySnapshots() { return on("players.inventory-snapshots", true); }
    public boolean publish() { return on("publish", true); }
    // player-first timelines (0.1.5): the per-player projection of the streams
    public boolean playerTimelines() { return on("player-timelines", true); }

    // clarity (0.1.5): chain lifetime + delivery ground check
    public int chainExpirySeconds() {
        return Math.max(60, Math.min(86_400, c.getInt("clarity.chain-expiry-seconds", 1800)));
    }
    public int clarityGroundRadius() {
        return Math.max(1, Math.min(16, c.getInt("clarity.ground-radius", 5)));
    }

    // watch / alerts (Amendment 3)
    public boolean watchOn() { return c.getBoolean("watch.enabled", true); }
    public int watchBurstCount() { return Math.max(2, c.getInt("watch.burst-breaks.count", 20)); }
    public int watchBurstWindowSec() { return Math.max(5, c.getInt("watch.burst-breaks.window-seconds", 60)); }
    public boolean watchValuableDestruction() { return c.getBoolean("watch.valuable-destruction", true); }
    public boolean watchContainerAccess() { return c.getBoolean("watch.non-owner-container-access", true); }
    public String webhookUrl() { return c.getString("watch.webhook-url", ""); }

    // overload shed order (Amendment 3): earliest = shed first = smallest effective cap
    public java.util.List<String> shedOrder() {
        return c.getStringList("writer.shed-order");
    }

    // writer / retention
    public int flushMs() { return Math.max(100, c.getInt("writer.flush-interval-ms", 1000)); }
    public int queueCap() { return Math.max(100, c.getInt("writer.queue-cap-per-stream", 20000)); }
    public int retentionDays() { return Math.max(0, c.getInt("retention-days", 30)); }

    // detail shaping (never existence — SPEC §2)
    public boolean payloadFor(String itemId) {
        List<String> globs = c.getStringList("detail.payload-types");
        for (String g : globs) if (glob(g, itemId)) return true;
        return false;
    }
    public boolean payloadAll() { return c.getBoolean("detail.payload-all-ledger-items", true); }
    public int ghostMinAgeTicks() { return Math.max(0, c.getInt("detail.ghost-min-age-ticks", 100)); }

    // combat grouping
    public int fightGapSeconds() { return Math.max(5, c.getInt("combat.fight-gap-seconds", 30)); }
    public double fightMinDamage() { return Math.max(0, c.getDouble("combat.fight-min-damage", 1.0)); }

    // damage incidents (0.1.2): coalesce window + near-death line
    public long damageIncidentGapMs() {
        return Math.max(250, Math.min(30_000, c.getLong("combat.damage-incident-gap-ms", 2000)));
    }
    public double nearDeathFraction() {
        double f = c.getDouble("combat.near-death-health-fraction", 0.30);
        return f < 0 ? 0 : Math.min(1, f);
    }

    // location sampling (0.1.4): one position dot per online player per interval.
    // Clamp 5s–1h: below 5s the volume stops being noise (coverage map math);
    // the default 60s is the settled reconciliation with the Nov 1 no-per-step
    // movement decision — sampling, never move-event tracking.
    public int locationsSampleSeconds() {
        return Math.max(5, Math.min(3600, c.getInt("locations.sample-seconds", 60)));
    }

    // rollback
    public boolean entityRestore() { return c.getBoolean("rollback.entity-restore", true); }
    public int inspectIndexCap() { return Math.max(100, c.getInt("inspect.index-cap", 5000)); }

    // lost & found (defaults = settled WRV values; config comments say change them)
    public long lfPrice() { return Math.max(0, c.getLong("lostfound.price", 10_000)); }
    public boolean lfFreeFirst() { return c.getBoolean("lostfound.free-first-recovery", true); }
    public long lfFreeThreshold() { return Math.max(0, c.getLong("lostfound.free-threshold", 10_000)); }
    public boolean lfFreeOnlyIfNoEarlier() { return c.getBoolean("lostfound.free-only-if-no-earlier-recovery", true); }
    public int lfWindowDays() { return Math.max(1, c.getInt("lostfound.window-days", 14)); }
    public int lfCooldownHours() { return Math.max(0, c.getInt("lostfound.cooldown-hours", 0)); }
    public boolean lfTellOwner() { return c.getBoolean("lostfound.tell-owner", true); }

    public static boolean glob(String pattern, String value) {
        StringBuilder rx = new StringBuilder();
        for (char ch : pattern.toCharArray()) {
            if (ch == '*') rx.append(".*");
            else if ("\\.[]{}()+-^$|?".indexOf(ch) >= 0) rx.append('\\').append(ch);
            else rx.append(ch);
        }
        return value.matches(rx.toString());
    }
}
