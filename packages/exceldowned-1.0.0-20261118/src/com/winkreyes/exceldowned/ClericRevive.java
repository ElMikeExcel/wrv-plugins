package com.winkreyes.exceldowned;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Cleric "Raise Ally" cast path (ExcelDowned 1.0.0, design B
 * sections 3 + 5; GRACE route D107; baseline D108) — thin Bukkit
 * mechanics over ClericRevivePolicy (every decision) and the
 * ClericGate seam (every EliteMobs read/spend). One entry point,
 * attemptRaise, behind every door (the /raiseally command today;
 * menu/form buttons ride the same call when their surfaces land).
 *
 * Discipline (design 5.1): all gates re-read LIVE state at attempt
 * time; nothing is charged unless ExcelDowned.reviveByCleric lands
 * the revive; only then is GRACE spent through the gate and the
 * per-caster cooldown stamped. The cooldown is success-only, 600 s
 * by default (config cleric-revive.cooldown-seconds — Mike, Nov 17,
 * 2026, NOT the design's 120 s), SEPARATE from the Revive Potion's
 * cooldown, and persisted (cleric-revive.yml) so a restart mid-
 * cooldown preserves the remaining time (design 5.4 / proof R6).
 *
 * If the GRACE spend returns false after a landed revive (session
 * closed / balance raced below cost in the one-tick window the
 * spike names), nothing further is charged, the cooldown still
 * starts, and the miss is logged — the revive already landed
 * exactly-once and is never rolled back for a billing race.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class ClericRevive {

    private final ExcelDowned plugin;
    private final ClericGate gate; // may be null: construction failed -> UNAVAILABLE
    private final Map<UUID, Long> lastSuccessMs = new ConcurrentHashMap<>();
    private final File cooldownFile;

    public ClericRevive(ExcelDowned plugin, ClericGate gate) {
        this.plugin = plugin;
        this.gate = gate;
        this.cooldownFile = new File(plugin.getDataFolder(), "cleric-revive.yml");
        loadCooldowns();
    }

    // ---------- cooldown (per caster, success-only, persisted) ----------

    public boolean onCooldown(UUID casterId, long nowMs) {
        Long last = lastSuccessMs.get(casterId);
        return last != null && ClericRevivePolicy.cooldownActive(
                nowMs, last, plugin.clericReviveCooldownMs);
    }

    public long cooldownRemainingMs(UUID casterId, long nowMs) {
        Long last = lastSuccessMs.get(casterId);
        if (last == null) return 0;
        return ClericRevivePolicy.cooldownRemainingMs(
                nowMs, last, plugin.clericReviveCooldownMs);
    }

    private void stampSuccess(UUID casterId, long nowMs) {
        lastSuccessMs.put(casterId, nowMs);
        saveCooldowns();
    }

    // ---------- the cast (design 5.1 gate order) ----------

    /**
     * One Raise Ally attempt. Returns true only when a revive landed
     * through this call. Every refusal sends exactly one plain-words
     * line naming the reason and charges nothing.
     */
    public boolean attemptRaise(Player caster) {
        if (caster == null) return false;
        long now = System.currentTimeMillis();
        ClericGate.Probe probe = gate == null
                ? ClericGate.Probe.UNAVAILABLE : gate.probe();
        ClericGate.ClericView view = gate == null
                ? ClericGate.ClericView.noClass() : gate.view(caster);

        // Target enumeration (read-only): downed allies, same world.
        List<Player> candidates = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(caster) || !plugin.isDowned(p.getUniqueId())) continue;
            if (!p.getWorld().equals(caster.getWorld())) continue;
            candidates.add(p);
        }
        int n = candidates.size();
        double[] distSq = new double[n];
        boolean[] inRange = new boolean[n];
        boolean anyInRange = false;
        for (int i = 0; i < n; i++) {
            distSq[i] = candidates.get(i).getLocation()
                    .distanceSquared(caster.getLocation());
            inRange[i] = FixPolicy.withinReviveRange(true, distSq[i],
                    plugin.clericReviveRange);
            anyInRange |= inRange[i];
        }
        // Eligibility for the picker applies the party rule when the
        // config requires it (D-B3: default off — any downed ally).
        boolean requireParty = plugin.clericReviveRequireParty;
        boolean[] eligible = new boolean[n];
        for (int i = 0; i < n; i++) {
            eligible[i] = inRange[i] && (!requireParty
                    || plugin.party().membershipOf(caster.getUniqueId(),
                            candidates.get(i).getUniqueId())
                        == PartyProvider.Membership.PARTY);
        }
        int picked = ClericRevivePolicy.pickTarget(
                aimedIndex(caster, candidates, inRange), eligible, distSq);
        // With require-party on, "a target is party" is exactly "the
        // picker found one" — that is what lets the verdict tell
        // NOT_PARTY (allies down in range, none in party) apart from
        // NO_TARGET_IN_RANGE (nobody down in range at all).
        boolean targetIsParty = picked >= 0;

        ClericRevivePolicy.Verdict verdict = ClericRevivePolicy.verdict(
                plugin.clericReviveEnabled, probe,
                view.hasActiveClass(),
                ClericRevivePolicy.isClericRoot(view.rootId()),
                ClericRevivePolicy.isGrace(view.resourceType()),
                ClericRevivePolicy.levelUnlocked(view.emLevel(),
                        plugin.clericReviveUnlockLevel),
                plugin.isDowned(caster.getUniqueId()), caster.isDead(),
                onCooldown(caster.getUniqueId(), now),
                anyInRange, requireParty, targetIsParty,
                ClericRevivePolicy.graceAffordable(view.resourceAmount(),
                        plugin.clericReviveGraceCost));
        if (verdict != ClericRevivePolicy.Verdict.OK) {
            caster.sendMessage(refusalLine(verdict, caster, now));
            return false;
        }

        Player target = candidates.get(picked);
        // Step 7: the revive itself. Exactly-once lives inside
        // ExcelDowned — a false here means another door consumed the
        // session in the same window; charge nothing (design 5.1).
        if (!plugin.reviveByCleric(caster, target)) {
            caster.sendMessage(ChatColor.GRAY + "✝ " + target.getName()
                    + " was already raised — nothing was spent.");
            return false;
        }
        // The revive landed: only now charge + cool down (D107/D108).
        if (ClericRevivePolicy.shouldCharge(true)) {
            boolean spent = gate != null
                    && gate.trySpend(caster, plugin.clericReviveGraceCost);
            if (!spent) {
                plugin.getLogger().warning("ExcelDowned: Raise Ally revived "
                        + target.getName() + " for " + caster.getName()
                        + " but the GRACE spend of " + plugin.clericReviveGraceCost
                        + " did not deduct (session closed or balance raced) — "
                        + "nothing further charged; cooldown still applies.");
            }
            stampSuccess(caster.getUniqueId(), System.currentTimeMillis());
        }
        caster.sendMessage(ChatColor.GREEN + "✝ You raised " + target.getName()
                + " — Raise Ally recharges in "
                + ExcelDowned.formatClock(plugin.clericReviveCooldownMs) + ".");
        return true;
    }

    /** The aimed-at candidate wins the pick (design section 3): the
     *  in-range downed ally closest to the caster's aim inside a
     *  ~60-degree cone (the MagicBridge picker shape), else -1. */
    private int aimedIndex(Player caster, List<Player> candidates, boolean[] inRange) {
        Vector aim = caster.getEyeLocation().getDirection();
        int best = -1;
        double bestDot = 0.5;
        for (int i = 0; i < candidates.size(); i++) {
            if (!inRange[i]) continue;
            Vector to = candidates.get(i).getLocation().add(0, 1, 0).toVector()
                    .subtract(caster.getEyeLocation().toVector());
            if (to.lengthSquared() < 0.01) return i;
            double dot = to.normalize().dot(aim);
            if (dot > bestDot) { bestDot = dot; best = i; }
        }
        return best;
    }

    private String refusalLine(ClericRevivePolicy.Verdict verdict,
                               Player caster, long now) {
        return switch (verdict) {
            case FEATURE_DISABLED -> ChatColor.GRAY
                    + "✝ Raise Ally is turned off right now.";
            case MODULE_UNAVAILABLE -> ChatColor.RED
                    + "✝ Raise Ally is unavailable — the EliteMobs class system can't be reached.";
            case MODULE_NOT_READY -> ChatColor.GRAY
                    + "✝ Raise Ally is still warming up — try again in a moment.";
            case NO_ACTIVE_CLASS -> ChatColor.RED
                    + "✝ Raise Ally needs an active class — choose Cleric first.";
            case NOT_CLERIC -> ChatColor.RED
                    + "✝ Raise Ally is a Cleric ability — your active class can't use it.";
            case LEVEL_TOO_LOW -> ChatColor.RED
                    + "✝ Raise Ally unlocks for Clerics at EM level "
                    + plugin.clericReviveUnlockLevel + ".";
            case CASTER_DOWNED -> ChatColor.RED
                    + "✝ You are down — you can't raise anyone.";
            case CASTER_DEAD -> ChatColor.RED
                    + "✝ You can't raise anyone while dead.";
            case ON_COOLDOWN -> ChatColor.GRAY
                    + "✝ Raise Ally is recharging — "
                    + ExcelDowned.formatClock(cooldownRemainingMs(
                            caster.getUniqueId(), now)) + " left.";
            case NO_TARGET_IN_RANGE -> ChatColor.RED
                    + "✝ No downed ally within "
                    + trimRange(plugin.clericReviveRange)
                    + " blocks — nothing was spent.";
            case NOT_PARTY -> ChatColor.RED
                    + "✝ Raise Ally can only raise your party members right now.";
            case GRACE_SHORT -> ChatColor.RED
                    + "✝ Not enough GRACE — Raise Ally costs "
                    + trimCost(plugin.clericReviveGraceCost) + " GRACE.";
            case OK -> "";
        };
    }

    private static String trimRange(double range) {
        return range == Math.rint(range)
                ? Long.toString(Math.round(range)) : Double.toString(range);
    }

    private static String trimCost(double cost) {
        return cost == Math.rint(cost)
                ? Long.toString(Math.round(cost)) : Double.toString(cost);
    }

    // ---------- persistence (design 5.4: cooldown survives restart) ----------

    private synchronized void saveCooldowns() {
        try {
            YamlConfiguration y = new YamlConfiguration();
            for (Map.Entry<UUID, Long> e : lastSuccessMs.entrySet()) {
                y.set("cooldowns." + e.getKey(), e.getValue());
            }
            cooldownFile.getParentFile().mkdirs();
            y.save(cooldownFile);
        } catch (IOException e) {
            plugin.getLogger().warning("ExcelDowned: cleric cooldown save failed: "
                    + e.getMessage());
        }
    }

    private void loadCooldowns() {
        if (!cooldownFile.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(cooldownFile);
        if (y.getConfigurationSection("cooldowns") == null) return;
        for (String key : y.getConfigurationSection("cooldowns").getKeys(false)) {
            try {
                lastSuccessMs.put(UUID.fromString(key),
                        y.getLong("cooldowns." + key));
            } catch (Throwable t) {
                plugin.getLogger().warning("ExcelDowned: skipped bad cleric "
                        + "cooldown entry " + key);
            }
        }
    }

    public void shutdown() { saveCooldowns(); }
}
