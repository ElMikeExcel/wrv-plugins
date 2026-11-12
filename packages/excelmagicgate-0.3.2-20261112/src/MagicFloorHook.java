package com.winkreyes.excelmagicgate;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Bukkit side of rank reconciliation (0.2.0 floor; 0.3.0 ceiling
 * + earned enforcement).
 *
 * 0.3.0 retarget (DESIGN.md §1): the stage table is now a CEILING,
 * and each mage's reconcile target is
 *   target = min(earnedRank(creditedTotal), ceilingRank(appliedStage))
 * The step-UP leg is the 0.2.0 machinery unchanged — Magic's OWN
 * ProgressionPath.upgrade(mage, wand), reflective, verified from the
 * wand itself — with only the destination rank changed. The step-DOWN
 * leg is new (DESIGN.md §1 Option A, rig-gated): Magic has no
 * downgrade, so a demotion is performed through the property seam —
 * reflective setPath(lowerKey) on the wand and on the mage's active
 * properties, then the lower frame set explicitly via
 * setManaMax/setManaRegeneration (Magic's upgradeTo only ever RAISES
 * the frame, so re-applying a lower path would not lower it) — and
 * verified from the wand exactly like a step-up. A demotion never
 * removes learned spells or spell levels: what a demoted player
 * loses is the frame — pool, regen, hotbar — until they re-earn it.
 * Demotion defers identically to step-up when the wand is not a
 * live item: a serialized (pouch / chest) wand is NEVER edited.
 *
 * AMENDMENT (0.3.0, Nov 10, 2026 — decide-and-log D2): the same
 * reconcile also runs the wand spell-inventory sync
 * (WandInventorySync). The original build stepped ranks without
 * granting the ranks' spells, which is exactly how the Nov 8
 * summoning tree stayed invisible on every wand (defined in the
 * book, listed in no player's inventory). The sync is standing,
 * not one-time: on every leg that reaches a live wand, any spell
 * in the wand's CURRENT-rank path lists (plus the config-carried
 * supplemental grants) that the wand lacks is granted through
 * Magic's own API; spells are only ever ADDED, never removed, so
 * a demotion still costs only the frame.
 *
 * When earning is disabled in config, the reconciler keeps the exact
 * 0.2.x behaviour (FloorPlan against the stage floor, up only).
 *
 * Wand discovery order: active wand -> offhand wand -> a bound wand
 * whose live item is physically in the player's inventory. A bound
 * wand that exists only as serialized bytes inside the Utility Pouch
 * is DETECTED through ExcelPouch's PouchQuery service (reflective,
 * fail-open) and DEFERRED explicitly to wand activation — this class
 * never reads or writes pouch bytes. The join leg, the held-item
 * activation leg and the watch-tick sweep complete a deferred move
 * as soon as the wand is live again.
 *
 * Enumeration: online players come from Bukkit's online-player
 * collection (Bedrock dotted-name players are ordinary Player
 * objects and are included — there is deliberately no name-shape
 * filter anywhere in this class), and the owed census is read from
 * Magic's OWN data folder (plugins/Magic/data/players/, one .dat
 * per mage in Magic 11.x), never from a Java player list.
 */
public final class MagicFloorHook {

    /** One player's reconciliation outcome, for logging and status. */
    public record ReconcileResult(String player, CeilingPlan.Outcome outcome,
                                  String fromRank, String toRank, int steps,
                                  int manaMax, int manaRegeneration) {}

    private final ExcelMagicGate plugin;
    private final Set<String> deferredLogged = ConcurrentHashMap.newKeySet();
    private final Set<UUID> pendingDemotions = ConcurrentHashMap.newKeySet();
    private volatile String lastSummary = "no reconcile run yet";
    private volatile String lastInventorySummary = "no inventory sync run yet";
    private final Map<String, Integer> counters = new ConcurrentHashMap<>();

    public MagicFloorHook(ExcelMagicGate plugin) {
        this.plugin = plugin;
    }

    public String lastSummary() {
        return lastSummary;
    }

    /** Most recent wand-inventory sync outcome, for /magicgate status. */
    public String lastInventorySummary() {
        return lastInventorySummary;
    }

    /** Players currently known to sit above their earned target. */
    public int demotionsPendingCount() {
        return pendingDemotions.size();
    }

    public String counterLine() {
        if (counters.isEmpty()) {
            return "none yet";
        }
        List<String> parts = new ArrayList<>();
        counters.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(e -> parts.add(e.getKey() + "=" + e.getValue()));
        return String.join(", ", parts);
    }

    private void bump(String key) {
        counters.merge(key, 1, Integer::sum);
    }

    // ------------------------------------------------------------------
    // Entry points
    // ------------------------------------------------------------------

    /** Reconcile every online mage to their earned target (apply + sweep legs). */
    public List<ReconcileResult> reconcileAllOnline(int stage, String source) {
        List<ReconcileResult> out = new ArrayList<>();
        RankFloor.Rank ceiling = RankFloor.floorForStage(stage);
        int census = mageDataCensus();
        for (Player p : Bukkit.getOnlinePlayers()) {
            ReconcileResult r = reconcilePlayer(p, stage, source);
            if (r != null) {
                out.add(r);
            }
        }
        long stepped = out.stream().filter(r -> r.outcome() == CeilingPlan.Outcome.STEP_UP).count();
        long demoted = out.stream().filter(r -> r.outcome() == CeilingPlan.Outcome.DEMOTE).count();
        lastSummary = source + ": stage " + stage + " ceiling " + ceiling.key()
                + ", online checked " + out.size() + ", stepped up " + stepped
                + ", demoted " + demoted
                + ", demotions pending " + pendingDemotions.size()
                + ", mage-data census " + census + " files";
        plugin.getLogger().info("Rank reconcile (" + lastSummary + ")");
        return out;
    }

    /** Reconcile one player; null when the player has no Magic mage. */
    public ReconcileResult reconcilePlayer(Player player, int stage, String source) {
        RankFloor.Rank ceiling = RankFloor.floorForStage(stage);
        try {
            Object controller = controller();
            if (controller == null) {
                return null; // Magic absent: nothing to reconcile against
            }
            Object mage = invoke(controller, "getMage",
                    new Class<?>[]{Player.class}, player);
            if (mage == null) {
                return null; // not a mage (never took a class): skip silently
            }

            // --- discover the wand and where it physically is ---
            Discovered found = discoverWand(controller, mage, player);
            Object wand = found.wand();
            boolean liveItem = found.liveItem();
            List<Object> bound = found.bound();

            FloorPlan.WandAccess access;
            String currentKey = null;
            if (wand != null && liveItem) {
                access = FloorPlan.WandAccess.ACTIVE_OR_INVENTORY;
                currentKey = resolveRankKey(mage, wand);
            } else if (!bound.isEmpty() || wand != null) {
                // A bound wand exists but no live item is carried:
                // pouch (serialized) or elsewhere. PouchQuery decides
                // which deferral this is; it cannot do more by design.
                access = pouchHoldsWand(player.getUniqueId())
                        ? FloorPlan.WandAccess.PARKED_IN_POUCH
                        : FloorPlan.WandAccess.NO_LIVE_WAND;
                Object anyWand = wand != null ? wand : bound.get(0);
                currentKey = resolveRankKey(mage, anyWand);
            } else {
                access = FloorPlan.WandAccess.NO_LIVE_WAND;
            }

            // --- the decision ---
            CeilingPlan.Decision decision;
            if (plugin.earningEnabled()) {
                long credited = creditedTotal(player, mage);
                RankFloor.Rank earned = EarnedRank.rankFor(credited, plugin.thresholds());
                decision = CeilingPlan.decide(access, currentKey, earned, ceiling,
                        plugin.demotionEnabled());
            } else {
                // Earning off: exact 0.2.x floor behaviour (up only).
                FloorPlan.Decision legacy = FloorPlan.decide(access, currentKey, ceiling);
                decision = switch (legacy.outcome()) {
                    case STEP -> new CeilingPlan.Decision(CeilingPlan.Outcome.STEP_UP,
                            legacy.steps(), ceiling);
                    case ALREADY_AT_FLOOR -> new CeilingPlan.Decision(
                            CeilingPlan.Outcome.AT_TARGET, 0,
                            RankFloor.fromKey(currentKey) == null ? ceiling
                                    : RankFloor.fromKey(currentKey));
                    case DEFERRED_PARKED_POUCH -> new CeilingPlan.Decision(
                            CeilingPlan.Outcome.DEFERRED_PARKED_POUCH, 0, ceiling);
                    case DEFERRED_NO_LIVE_WAND -> new CeilingPlan.Decision(
                            CeilingPlan.Outcome.DEFERRED_NO_LIVE_WAND, 0, ceiling);
                    case SKIP_UNKNOWN_RANK -> new CeilingPlan.Decision(
                            CeilingPlan.Outcome.SKIP_UNKNOWN_RANK, 0, ceiling);
                };
            }

            // Track above-target players for /magicgate status.
            RankFloor.Rank currentRank = RankFloor.fromKey(currentKey);
            if (currentRank != null && decision.target() != null
                    && currentRank.ordinal() > decision.target().ordinal()) {
                pendingDemotions.add(player.getUniqueId());
            } else if (decision.outcome() == CeilingPlan.Outcome.AT_TARGET
                    || decision.outcome() == CeilingPlan.Outcome.STEP_UP) {
                pendingDemotions.remove(player.getUniqueId());
            }

            switch (decision.outcome()) {
                case AT_TARGET -> {
                    bump("already");
                    syncInventory(player, mage, currentKey);
                    refreshLore(player);
                    return result(player, decision.outcome(), currentKey, currentKey, 0, wand);
                }
                case DEMOTE_HELD -> {
                    bump("demote-held");
                    String onceKey = player.getUniqueId() + ":DEMOTE_HELD";
                    if (deferredLogged.add(onceKey)) {
                        plugin.getLogger().info("Rank reconcile DEMOTE HELD for "
                                + player.getName() + ": wand reads " + currentKey
                                + " but their earned target is " + decision.target().key()
                                + " and earning.demotion-enabled is false — rank left "
                                + "untouched (named state, per DESIGN.md S11 Q2).");
                    }
                    return result(player, decision.outcome(), currentKey, currentKey, 0, wand);
                }
                case DEFERRED_PARKED_POUCH, DEFERRED_NO_LIVE_WAND -> {
                    bump(decision.outcome() == CeilingPlan.Outcome.DEFERRED_PARKED_POUCH
                            ? "deferred-pouch" : "deferred-no-live-wand");
                    String onceKey = player.getUniqueId() + ":" + decision.outcome();
                    if (deferredLogged.add(onceKey)) {
                        plugin.getLogger().info("Rank reconcile DEFERRED for " + player.getName()
                                + ": bound wand is "
                                + (decision.outcome() == CeilingPlan.Outcome.DEFERRED_PARKED_POUCH
                                        ? "parked in the Utility Pouch (serialized; PouchQuery is query-only)"
                                        : "not a live carried item")
                                + " — the move to their earned target ("
                                + (decision.target() == null ? "?" : decision.target().key())
                                + ") completes on wand activation (held-item / join / sweep legs).");
                    }
                    return result(player, decision.outcome(), currentKey, currentKey, 0, wand);
                }
                case SKIP_UNKNOWN_RANK -> {
                    bump("skip-unknown-rank");
                    plugin.getLogger().warning("Rank reconcile SKIPPED for " + player.getName()
                            + ": wand path key '" + currentKey + "' is not a WRV rank — never guessed at.");
                    return result(player, decision.outcome(), currentKey, currentKey, 0, wand);
                }
                case STEP_UP, DEMOTE -> {
                    // fall through to the move legs below
                }
            }

            // --- activate the wand first (both directions) ---
            // Magic's upgrade() lands on the mage's ACTIVE properties,
            // and the demotion seam writes the active properties too:
            // activate the wand (Magic's own activation, the same thing
            // taking it in hand does), then re-discover and move the
            // ACTIVE wand object.
            try {
                Method activate = wand.getClass().getMethod("activate",
                        Class.forName("com.elmakers.mine.bukkit.api.magic.Mage",
                                true, wand.getClass().getClassLoader()));
                activate.invoke(wand, mage);
            } catch (Exception ignored) {
                // Activation is best-effort; the re-discovery + the
                // wand-frame verification below judge the outcome.
            }
            Discovered activated = discoverWand(controller, mage, player);
            if (activated.wand() != null && activated.liveItem()) {
                wand = activated.wand();
            }

            ReconcileResult moved = decision.outcome() == CeilingPlan.Outcome.DEMOTE
                    ? demote(player, mage, wand, currentKey, decision, source, stage)
                    : stepUp(player, mage, wand, currentKey, decision, source, stage);
            // The inventory sync follows the wand's RESULTING rank
            // (never the intended target): a rank-up fills the book
            // to the new rank in the same leg; a stalled partial
            // move syncs only to the rank the wand actually reached.
            if (moved != null && moved.toRank() != null) {
                syncInventory(player, mage, moved.toRank());
            }
            return moved;
        } catch (Exception e) {
            bump("errors");
            plugin.getLogger().warning("Rank reconcile failed for " + player.getName()
                    + " (" + source + "): " + e);
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Step-up leg (0.2.0 machinery, retargeted to min(earned, ceiling))
    // ------------------------------------------------------------------

    private ReconcileResult stepUp(Player player, Object mage, Object wand, String currentKey,
                                   CeilingPlan.Decision decision, String source, int stage) {
        try {
            RankFloor.Rank target = decision.target();
            String fromKey = currentKey;
            int stepsDone = 0;
            // A Quartermaster wand's own path field is UNSET until its
            // first upgrade: declare the resolved rank on the wand
            // with setPath first (the set lands even though Magic's
            // change notification NPEs on the null old path — caught
            // and ignored), then step via the CURRENT path object's
            // upgrade(mage, wand). Advancement is always judged by
            // re-reading the wand, never by exception state.
            Object wandPathObj = invoke(wand, "getPath", NOARGS);
            if (wandPathObj == null) {
                try {
                    Method setPath = wand.getClass().getMethod("setPath", String.class);
                    setPath.invoke(wand, currentKey);
                } catch (Exception ignored) {
                    // Magic's setPath notification NPEs when the old
                    // path was null; the set itself lands (rig-proven).
                }
            }
            for (int i = 0; i < decision.steps(); i++) {
                Object controllerNow = controller();
                ClassLoader magicCl = controllerNow.getClass().getClassLoader();
                Class<?> wandPathApi = Class.forName(
                        "com.elmakers.mine.bukkit.api.wand.WandUpgradePath", true, magicCl);
                Method getPath = controllerNow.getClass().getMethod("getPath", String.class);
                Object path = getPath.invoke(controllerNow, currentKey);
                if (path == null || !wandPathApi.isInstance(path)) {
                    plugin.getLogger().warning("Rank reconcile STALLED for " + player.getName()
                            + ": controller has no progression path for '" + currentKey + "'.");
                    break;
                }
                Object next = invoke(path, "getNextPath", NOARGS);
                if (next == null) {
                    break;
                }
                Class<?> mageApi = Class.forName(
                        "com.elmakers.mine.bukkit.api.magic.Mage", true, magicCl);
                Class<?> wandApi = Class.forName(
                        "com.elmakers.mine.bukkit.api.wand.Wand", true, magicCl);
                Method upgrade = path.getClass().getMethod("upgrade", mageApi, wandApi);
                try {
                    upgrade.invoke(path, mage, wand);
                } catch (Exception stepError) {
                    // An upgrade can throw inside Magic AFTER applying
                    // (the null-old-path notification). Judge by the
                    // re-read, not by the exception.
                    plugin.getLogger().info("Rank reconcile: upgrade() for " + player.getName()
                            + " threw after/while stepping from '" + currentKey + "' ("
                            + stepError.getCause() + ") — re-reading the wand to judge.");
                }
                // Re-discover FRESH objects before judging: for a
                // wand that is carried but not active, upgrade() lands
                // on the item's stored data, and the Wand object this
                // loop started with can be a stale materialization.
                Discovered fresh = discoverWand(controller(), mage, player);
                Object freshWand = fresh.wand() != null ? fresh.wand() : wand;
                String afterKey = resolveRankKey(mage, freshWand);
                if (afterKey == null || afterKey.equals(currentKey)) {
                    plugin.getLogger().warning("Rank reconcile STALLED for " + player.getName()
                            + ": upgrade() did not advance the wand rank from '" + currentKey
                            + "' — stopping (no force, no retry loop).");
                    break;
                }
                stepsDone++;
                currentKey = afterKey;
                wand = freshWand;
            }

            // --- verify from the wand itself ---
            int manaMax = ((Number) invoke(wand, "getManaMax", NOARGS)).intValue();
            int manaRegen = ((Number) invoke(wand, "getManaRegeneration", NOARGS)).intValue();
            RankFloor.Rank now = RankFloor.fromKey(currentKey);
            boolean verified = now != null && now.ordinal() >= target.ordinal()
                    && RankFloor.frameMatches(now, manaMax, manaRegen);
            if (stepsDone > 0 && verified) {
                bump("stepped");
                deferredLogged.remove(player.getUniqueId() + ":" + CeilingPlan.Outcome.DEFERRED_PARKED_POUCH);
                deferredLogged.remove(player.getUniqueId() + ":" + CeilingPlan.Outcome.DEFERRED_NO_LIVE_WAND);
                plugin.getLogger().info("Rank reconcile: " + player.getName() + " stepped "
                        + fromKey + " -> " + currentKey + " (" + stepsDone + " upgrade(s), "
                        + source + ", target " + target.key() + " at stage " + stage
                        + ") — pool " + manaMax + ", regen " + manaRegen + "/s, verified on the wand.");
                player.sendMessage(ExcelMagicGate.colorText(
                        "&6[MagicGate] &eYour wand rises to &f" + title(currentKey)
                                + "&e — mana pool &f" + manaMax + "&e, regen &f" + manaRegen
                                + "/s&e (earned rank)."));
                refreshLore(player);
                return result(player, CeilingPlan.Outcome.STEP_UP, fromKey, currentKey, stepsDone, wand);
            }
            if (stepsDone > 0) {
                bump("step-unverified");
                plugin.getLogger().warning("Rank reconcile UNVERIFIED for " + player.getName()
                        + ": stepped " + stepsDone + " time(s) to '" + currentKey
                        + "' but the wand reads pool " + manaMax + " / regen " + manaRegen
                        + " — expected the " + target.key() + " frame ("
                        + target.manaMax() + " / " + target.manaRegeneration() + ").");
                return result(player, CeilingPlan.Outcome.STEP_UP, fromKey, currentKey, stepsDone, wand);
            }
            bump("no-progress");
            return result(player, CeilingPlan.Outcome.AT_TARGET, fromKey, currentKey, 0, wand);
        } catch (Exception e) {
            bump("errors");
            plugin.getLogger().warning("Rank step-up failed for " + player.getName()
                    + " (" + source + "): " + e);
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Demotion leg (0.3.0, DESIGN.md §1 Option A — the property seam)
    // ------------------------------------------------------------------

    /**
     * Bring a wand DOWN to its earned target, one rank at a time.
     * Magic has no downgrade (verified three ways in DESIGN.md §1):
     * each step is setPath(lowerKey) on the wand AND on the mage's
     * active properties (and class properties when writable), then
     * the lower frame written explicitly via setManaMax /
     * setManaRegeneration on the wand and the active properties,
     * then re-discovery + verification from the wand itself — the
     * same judge-by-re-read discipline as the step-up leg. Learned
     * spells are never removed. Any step that does not verify stops
     * the demotion cold (no force, no retry loop); the partial state
     * is logged with the exact step that failed.
     */
    private ReconcileResult demote(Player player, Object mage, Object wand, String currentKey,
                                   CeilingPlan.Decision decision, String source, int stage) {
        RankFloor.Rank target = decision.target();
        String fromKey = currentKey;
        RankFloor.Rank fromRank = RankFloor.fromKey(fromKey);
        int stepsDone = 0;
        try {
            for (int i = 0; i < decision.steps(); i++) {
                RankFloor.Rank current = RankFloor.fromKey(currentKey);
                RankFloor.Rank lower = CeilingPlan.oneDown(current);
                if (lower == null) {
                    break;
                }
                // 1) path: wand + active properties (+ class when writable)
                setPathQuietly(wand, lower.key(), player, "wand");
                Object active = activeProperties(mage);
                if (active != null) {
                    setPathQuietly(active, lower.key(), player, "active properties");
                }
                Object mageClass = activeClass(mage);
                if (mageClass != null) {
                    setPathQuietly(mageClass, lower.key(), player, "mage class");
                }
                // 2) frame: Magic's own upgrade path only ever RAISES
                //    the frame, so the lower frame is written directly.
                setFrameQuietly(wand, lower, player, "wand");
                if (active != null) {
                    setFrameQuietly(active, lower, player, "active properties");
                }
                // Clamp a now-overflowing mana pool to the new frame.
                try {
                    Object mana = invoke(wand, "getMana", NOARGS);
                    if (mana instanceof Number n && n.doubleValue() > lower.manaMax()) {
                        Method setMana = wand.getClass().getMethod("setMana", float.class);
                        setMana.setAccessible(true);
                        setMana.invoke(wand, (float) lower.manaMax());
                    }
                } catch (Exception ignored) {
                    // clamping is cosmetic; the frame verification judges
                }
                // 3) judge by the re-read, never by exception state
                Discovered fresh = discoverWand(controller(), mage, player);
                Object freshWand = fresh.wand() != null ? fresh.wand() : wand;
                String afterKey = resolveRankKey(mage, freshWand);
                int manaMax = ((Number) invoke(freshWand, "getManaMax", NOARGS)).intValue();
                int manaRegen = ((Number) invoke(freshWand, "getManaRegeneration", NOARGS)).intValue();
                if (!lower.key().equals(afterKey)
                        || !RankFloor.frameMatches(lower, manaMax, manaRegen)) {
                    bump("demote-stalled");
                    plugin.getLogger().warning("Rank reconcile DEMOTE STALLED for "
                            + player.getName() + ": after setPath('" + lower.key()
                            + "') the wand re-reads path '" + afterKey + "' pool " + manaMax
                            + " / regen " + manaRegen + " — expected " + lower.key()
                            + " frame (" + lower.manaMax() + " / " + lower.manaRegeneration()
                            + "). Stopping (no force, no retry loop); " + stepsDone
                            + " demotion step(s) had landed. This is the DESIGN.md S1 "
                            + "seam failing — record it, do not work around it silently.");
                    return result(player, CeilingPlan.Outcome.DEMOTE, fromKey,
                            currentKey, stepsDone, freshWand);
                }
                stepsDone++;
                currentKey = afterKey;
                wand = freshWand;
            }

            int manaMax = ((Number) invoke(wand, "getManaMax", NOARGS)).intValue();
            int manaRegen = ((Number) invoke(wand, "getManaRegeneration", NOARGS)).intValue();
            RankFloor.Rank now = RankFloor.fromKey(currentKey);
            boolean verified = now == target
                    && RankFloor.frameMatches(target, manaMax, manaRegen);
            if (stepsDone > 0 && verified) {
                bump("demoted");
                pendingDemotions.remove(player.getUniqueId());
                deferredLogged.remove(player.getUniqueId() + ":" + CeilingPlan.Outcome.DEFERRED_PARKED_POUCH);
                deferredLogged.remove(player.getUniqueId() + ":" + CeilingPlan.Outcome.DEFERRED_NO_LIVE_WAND);
                long credited = plugin.earnedStore().creditedTotal(player.getUniqueId().toString());
                long backTo = fromRank == null ? 0
                        : Math.max(0, plugin.thresholds().at(fromRank) - credited);
                plugin.getLogger().info("Rank reconcile: " + player.getName() + " demoted "
                        + fromKey + " -> " + currentKey + " (" + stepsDone + " step(s), "
                        + source + ", earned target at stage " + stage + ") — pool " + manaMax
                        + ", regen " + manaRegen + "/s, verified on the wand. Credited casts "
                        + credited + "; spells never removed.");
                player.sendMessage(ExcelMagicGate.colorText(
                        "&6[MagicGate] &eRanks are earned now: your wand settles to &f"
                                + title(currentKey) + "&e — mana pool &f" + manaMax
                                + "&e, regen &f" + manaRegen + "/s&e. Credited casts: &f"
                                + credited + "&e"
                                + (backTo > 0 ? " — &f" + backTo + "&e more credited casts "
                                        + "re-earn " + title(fromKey) + "."
                                        : ".")));
                refreshLore(player);
                return result(player, CeilingPlan.Outcome.DEMOTE, fromKey, currentKey, stepsDone, wand);
            }
            bump("demote-unverified");
            plugin.getLogger().warning("Rank reconcile DEMOTE UNVERIFIED for " + player.getName()
                    + ": moved " + stepsDone + " step(s) to '" + currentKey + "' reading pool "
                    + manaMax + " / regen " + manaRegen + " — expected the " + target.key()
                    + " frame (" + target.manaMax() + " / " + target.manaRegeneration() + ").");
            return result(player, CeilingPlan.Outcome.DEMOTE, fromKey, currentKey, stepsDone, wand);
        } catch (Exception e) {
            bump("errors");
            plugin.getLogger().warning("Rank demotion failed for " + player.getName()
                    + " (" + source + "): " + e);
            return null;
        }
    }

    /** setPath with the floor hook's standing rule: Magic's change
     *  notification can throw AFTER the set lands — the re-read judges. */
    private void setPathQuietly(Object target, String key, Player player, String what) {
        try {
            Method setPath = target.getClass().getMethod("setPath", String.class);
            setPath.setAccessible(true);
            setPath.invoke(target, key);
        } catch (Exception e) {
            plugin.getLogger().info("Rank reconcile: setPath('" + key + "') on " + what
                    + " for " + player.getName() + " threw (" + e.getCause()
                    + ") — re-reading to judge.");
        }
    }

    /** Write a rank's frame (pool + regen) onto a properties holder. */
    private void setFrameQuietly(Object target, RankFloor.Rank rank, Player player, String what) {
        try {
            Method setMax = target.getClass().getMethod("setManaMax", int.class);
            setMax.setAccessible(true);
            setMax.invoke(target, rank.manaMax());
        } catch (Exception e) {
            plugin.getLogger().info("Rank reconcile: setManaMax on " + what + " for "
                    + player.getName() + " unavailable (" + e.getCause() + ").");
        }
        try {
            Method setRegen = target.getClass().getMethod("setManaRegeneration", int.class);
            setRegen.setAccessible(true);
            setRegen.invoke(target, rank.manaRegeneration());
        } catch (Exception e) {
            plugin.getLogger().info("Rank reconcile: setManaRegeneration on " + what
                    + " for " + player.getName() + " unavailable (" + e.getCause() + ").");
        }
    }

    private Object activeProperties(Object mage) {
        try {
            return invoke(mage, "getActiveProperties", NOARGS);
        } catch (Exception e) {
            return null;
        }
    }

    private Object activeClass(Object mage) {
        try {
            return invoke(mage, "getActiveClass", NOARGS);
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Wand spell-inventory sync (0.3.0 amendment — WandInventorySync)
    // ------------------------------------------------------------------

    /**
     * Grant every spell in the wand's current-rank path lists that
     * the wand is missing. Standing sync, called from every
     * reconcile leg that reached a live wand (AT_TARGET, and after
     * a step-up / demote, keyed to the RESULTING rank). Only ever
     * adds; a serialized wand never reaches this method (callers
     * defer those legs whole). Writes go through Magic's own API —
     * addSpell (forceAddSpell as fallback) registers the spell, and
     * updateSpellInventory places it at the next free slot when the
     * wand's inventory map does not already carry it; together they
     * are the runtime form of the two mage-file representations
     * (parsed spell_inventory + item custom_data) the tame-fix
     * packet's grant pass edits offline. Every grant is verified by
     * re-discovering the wand and re-reading its spells + inventory,
     * never by the write's return value alone.
     *
     * 0.3.2 dead-key gate: before any grant, each missing key is
     * resolved against Magic's spell registry (SpellTemplateGate);
     * a key with no template is skipped and named at WARNING rather
     * than fed to addSpell, whose inventory rebuild NPEs on it
     * (diagnosis §1). Rank/reconcile behaviour is unchanged.
     */
    private void syncInventory(Player player, Object mage, String resultingRankKey) {
        if (!plugin.inventorySyncEnabled()) {
            return;
        }
        RankFloor.Rank rank = RankFloor.fromKey(resultingRankKey);
        if (rank == null) {
            return;
        }
        try {
            Object controller = controller();
            if (controller == null) {
                return;
            }
            Object path = invoke(controller, "getPath",
                    new Class<?>[]{String.class}, rank.key());
            if (path == null) {
                plugin.getLogger().warning("Wand inventory sync SKIPPED for "
                        + player.getName() + ": Magic has no progression path for '"
                        + rank.key() + "' — the expected spell set cannot be read, "
                        + "and an unreadable set is never guessed at.");
                bump("inv-no-path");
                return;
            }
            java.util.Set<String> expected = WandInventorySync.expectedFor(rank,
                    stringCollection(invoke(path, "getSpells", NOARGS)),
                    stringCollection(invoke(path, "getExtraSpells", NOARGS)),
                    plugin.supplementalGrants());

            Discovered found = discoverWand(controller, mage, player);
            if (found.wand() == null || !found.liveItem()) {
                return; // not live after all: the rank leg's deferral stands
            }
            Object wand = found.wand();
            // Writes land on the mage's ACTIVE properties, like the
            // rank moves: activate first, then re-discover.
            try {
                Method activate = wand.getClass().getMethod("activate",
                        Class.forName("com.elmakers.mine.bukkit.api.magic.Mage",
                                true, wand.getClass().getClassLoader()));
                activate.invoke(wand, mage);
            } catch (Exception ignored) {
                // best-effort; verification below judges the outcome
            }
            Discovered activated = discoverWand(controller, mage, player);
            if (activated.wand() != null && activated.liveItem()) {
                wand = activated.wand();
            }

            Map<String, Integer> inventory = spellInventoryOf(wand);
            java.util.Set<String> held = new java.util.LinkedHashSet<>(inventory.keySet());
            held.addAll(stringCollection(invoke(wand, "getSpells", NOARGS)));
            List<String> missing = WandInventorySync.missingFor(expected, held);
            if (missing.isEmpty()) {
                bump("inv-in-sync");
                lastInventorySummary = player.getName() + " @" + rank.key()
                        + ": in sync (" + expected.size() + " path spells held)";
                return;
            }

            // Dead-key gate (0.3.2, P0 defensive fix — diagnosis §5
            // Fix 1): a missing key that resolves to NO SpellTemplate
            // in Magic must never reach addSpell — Magic's
            // Wand.buildInventory() dereferences the null template
            // and NPEs, poisoning the whole grant batch. Skip those
            // keys, name each one at WARNING, and sync the rest.
            // Skipped keys are accounted as skipped below, never as
            // granted. The resolver is fail-open (SpellTemplateGate):
            // a probe failure leaves the key grantable.
            SpellTemplateGate.Partition gate = SpellTemplateGate.partition(missing,
                    key -> spellTemplateResolves(controller, key));
            List<String> skippedDead = gate.skipped();
            for (String dead : skippedDead) {
                plugin.getLogger().warning(
                        SpellTemplateGate.skippedWarning(dead, player.getName()));
            }
            if (!skippedDead.isEmpty()) {
                bump("inv-dead-key-skipped");
            }
            missing = gate.grantable();
            if (missing.isEmpty()) {
                lastInventorySummary = player.getName() + " @" + rank.key()
                        + ": " + SpellTemplateGate.accounting(0, 0, skippedDead)
                        + " (nothing grantable was missing)";
                return;
            }

            Map<String, Integer> slots = WandInventorySync.slotsFor(missing, inventory);
            Map<String, Integer> placements = new java.util.LinkedHashMap<>();
            for (String spell : missing) {
                addSpellQuietly(wand, spell, player);
                if (!spellInventoryOf(wand).containsKey(spell)) {
                    placements.put(spell, slots.get(spell));
                }
            }
            if (!placements.isEmpty()) {
                try {
                    Method update = wand.getClass().getMethod("updateSpellInventory", Map.class);
                    update.setAccessible(true);
                    update.invoke(wand, placements);
                } catch (Exception e) {
                    plugin.getLogger().info("Wand inventory sync: updateSpellInventory for "
                            + player.getName() + " unavailable (" + e.getCause()
                            + ") — judging by the re-read.");
                }
            }

            // Judge by the re-read, never by the writes.
            Discovered verify = discoverWand(controller(), mage, player);
            Object vwand = verify.wand() != null ? verify.wand() : wand;
            java.util.Set<String> vheld = new java.util.LinkedHashSet<>(
                    spellInventoryOf(vwand).keySet());
            vheld.addAll(stringCollection(invoke(vwand, "getSpells", NOARGS)));
            java.util.Set<String> vheldBase = WandInventorySync.baseKeys(vheld);
            List<String> granted = new ArrayList<>();
            List<String> failed = new ArrayList<>();
            for (String spell : missing) {
                (vheldBase.contains(spell) ? granted : failed).add(spell);
            }
            if (!granted.isEmpty()) {
                bump("inv-granted");
                plugin.getLogger().info("Wand inventory sync: " + player.getName()
                        + " granted " + granted.size() + " spell(s) at " + rank.key()
                        + " — " + String.join(", ", granted)
                        + " — verified on the wand. (Standing sync: the rank's "
                        + "path lists fill the wand; spells are only ever added.)");
                player.sendMessage(ExcelMagicGate.colorText(
                        "&6[MagicGate] &eYour spell book fills in: &f"
                                + granted.size() + " spell(s)&e now on your wand — &f"
                                + String.join(", ", granted) + "&e."));
            }
            if (!failed.isEmpty()) {
                bump("inv-grant-failed");
                plugin.getLogger().warning("Wand inventory sync INCOMPLETE for "
                        + player.getName() + " at " + rank.key() + ": "
                        + failed.size() + " spell(s) did not verify on the wand "
                        + "after granting — " + String.join(", ", failed)
                        + ". The next sweep retries them (the sync is standing).");
            }
            if (!skippedDead.isEmpty()) {
                // Completion accounting: skipped dead keys are
                // reported as skipped — never counted as granted.
                plugin.getLogger().warning("Wand inventory sync accounting for "
                        + player.getName() + " at " + rank.key() + ": "
                        + SpellTemplateGate.accounting(
                                granted.size(), failed.size(), skippedDead)
                        + " — skipped keys were not attempted and are not "
                        + "counted as granted.");
            }
            lastInventorySummary = player.getName() + " @" + rank.key()
                    + ": granted " + granted.size() + " spell(s)"
                    + (failed.isEmpty() ? "" : ", " + failed.size() + " unverified")
                    + (skippedDead.isEmpty() ? "" : ", " + skippedDead.size()
                            + " skipped (no template: "
                            + String.join(", ", skippedDead) + ")");
        } catch (Exception e) {
            bump("inv-errors");
            plugin.getLogger().warning("Wand inventory sync failed for "
                    + player.getName() + ": " + e);
        }
    }

    /**
     * True when Magic's controller resolves the key to a live
     * SpellTemplate (reflective getSpellTemplate, the diagnosis §5
     * Fix 1 probe). FAIL-OPEN: any probe failure (no controller, no
     * such method, a reflection error) returns true — the probe is a
     * guard against a KNOWN-null template, never a new way to block
     * a grant; when the probe cannot answer, addSpell runs exactly
     * as it did before 0.3.2 and the re-read judges.
     */
    private boolean spellTemplateResolves(Object controller, String spell) {
        if (controller == null) {
            return true;
        }
        try {
            Method m = controller.getClass().getMethod("getSpellTemplate", String.class);
            m.setAccessible(true);
            return m.invoke(controller, spell) != null;
        } catch (Exception e) {
            return true;
        }
    }

    /** addSpell with Magic's own grant fallback (forceAddSpell);
     *  the re-read in syncInventory judges, so this only reports
     *  whether either write claimed success. The 0.3.2 dead-key gate
     *  sits on this seam too: a key with no SpellTemplate in Magic is
     *  refused here (named at WARNING) before addSpell can feed it
     *  to Wand.buildInventory(). syncInventory partitions its batch
     *  up front, so in the sync path this guard is a second net for
     *  any other caller, and the WARNING fires once per key there. */
    private boolean addSpellQuietly(Object wand, String spell, Player player) {
        try {
            Object controller = controller();
            if (controller != null && !spellTemplateResolves(controller, spell)) {
                plugin.getLogger().warning(
                        SpellTemplateGate.skippedWarning(spell, player.getName()));
                return false;
            }
        } catch (Exception ignored) {
            // probe unavailable: proceed exactly as before 0.3.2
        }
        try {
            Method add = wand.getClass().getMethod("addSpell", String.class);
            add.setAccessible(true);
            if (Boolean.TRUE.equals(add.invoke(wand, spell))) {
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().info("Wand inventory sync: addSpell('" + spell + "') for "
                    + player.getName() + " threw (" + e.getCause() + ") — trying the grant fallback.");
        }
        try {
            Method force = wand.getClass().getMethod("forceAddSpell", String.class);
            force.setAccessible(true);
            return Boolean.TRUE.equals(force.invoke(wand, spell));
        } catch (Exception e) {
            plugin.getLogger().info("Wand inventory sync: forceAddSpell('" + spell + "') for "
                    + player.getName() + " unavailable (" + e.getCause() + ").");
            return false;
        }
    }

    /** The wand's spell_inventory map (spell key -> slot), copied. */
    private Map<String, Integer> spellInventoryOf(Object wand) {
        Map<String, Integer> out = new java.util.LinkedHashMap<>();
        try {
            Object raw = invoke(wand, "getSpellInventory", NOARGS);
            if (raw instanceof Map<?, ?> m) {
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    if (e.getKey() != null && e.getValue() instanceof Number n) {
                        out.put(String.valueOf(e.getKey()), n.intValue());
                    }
                }
            }
        } catch (Exception ignored) {
            // an unreadable inventory reads as empty; verification judges
        }
        return out;
    }

    private static List<String> stringCollection(Object raw) {
        List<String> out = new ArrayList<>();
        if (raw instanceof Collection<?> c) {
            for (Object o : c) {
                if (o != null) {
                    out.add(String.valueOf(o));
                }
            }
        }
        return out;
    }

    private void refreshLore(Player player) {
        if (plugin.loreHook() != null) {
            plugin.loreHook().refresh(player);
        }
    }

    /**
     * The player's credited total: the ledger row when one exists;
     * otherwise the DESIGN.md §6 fallback — seed the row from the
     * live Mage API sum under the same classification (covers a
     * player who joined between the placement pull and the install,
     * and future new mages, whose sum is 0 anyway). Placement rows
     * are never overwritten by the fallback.
     */
    private long creditedTotal(Player player, Object mage) {
        EarnedStore store = plugin.earnedStore();
        String uuid = player.getUniqueId().toString();
        EarnedStore.Row row = store.row(uuid);
        if (row != null) {
            return row.creditedTotal();
        }
        long sum = 0;
        try {
            Object spells = invoke(mage, "getSpells", NOARGS);
            if (spells instanceof Collection<?> c) {
                java.util.Map<String, Long> counts = new java.util.HashMap<>();
                for (Object spell : c) {
                    try {
                        Object data = invoke(spell, "getSpellData", NOARGS);
                        Object key = invoke(spell, "getSpellKey", NOARGS);
                        if (data == null || key == null) {
                            continue;
                        }
                        long count = ((Number) invoke(data, "getCastCount", NOARGS)).longValue();
                        counts.put(String.valueOf(invoke(key, "getKey", NOARGS)), count);
                    } catch (Exception ignored) {
                        // one unreadable spell must not sink the sum
                    }
                }
                sum = PlacementCalculator.creditedTotalFromCounts(counts, plugin.creditPolicy());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Fallback credit seed failed for " + player.getName()
                    + ": " + e + " — treating credited total as 0 for this reconcile.");
            return 0;
        }
        if (store.seedFallback(uuid, sum)) {
            plugin.markEarnedDirty();
            plugin.getLogger().info("Rank reconcile: seeded " + player.getName()
                    + "'s credited total from the live Mage API (" + sum
                    + " credited casts, no placement row existed).");
        }
        return sum;
    }

    // ------------------------------------------------------------------
    // Wand discovery (unchanged from 0.2.0)
    // ------------------------------------------------------------------

    /** Wand discovery result: the wand object (or null), whether a
     *  live item backs it, and the mage's bound-wand list. */
    private record Discovered(Object wand, boolean liveItem, List<Object> bound) {}

    private Discovered discoverWand(Object controller, Object mage, Player player) throws Exception {
        Object wand = invoke(mage, "getActiveWand", NOARGS);
        boolean liveItem = wand != null;
        if (wand == null) {
            wand = invoke(mage, "getOffhandWand", NOARGS);
            liveItem = wand != null;
        }
        List<Object> bound = new ArrayList<>();
        Object boundObj = invoke(mage, "getBoundWands", NOARGS);
        if (boundObj instanceof Collection<?> c) {
            bound.addAll(c);
        }
        if (wand == null && !bound.isEmpty()) {
            for (Object candidate : bound) {
                if (itemIsCarried(player, candidate)) {
                    wand = candidate;
                    liveItem = true;
                    break;
                }
            }
        }
        if (wand == null) {
            wand = wandFromCarriedItem(controller, player);
            liveItem = wand != null;
        }
        return new Discovered(wand, liveItem, bound);
    }

    /** Read-only rank/credit view of a player for /magicgate rank. */
    public String describePlayer(Player player, int appliedStage) {
        try {
            Object controller = controller();
            if (controller == null) {
                return "Magic is not available right now.";
            }
            Object mage = invoke(controller, "getMage", new Class<?>[]{Player.class}, player);
            if (mage == null) {
                return "You have no mage class yet — take a wand from the Quartermaster first.";
            }
            Object wand = invoke(mage, "getActiveWand", NOARGS);
            if (wand == null) {
                wand = invoke(mage, "getOffhandWand", NOARGS);
            }
            if (wand == null) {
                Object boundObj = invoke(mage, "getBoundWands", NOARGS);
                if (boundObj instanceof Collection<?> c && !c.isEmpty()) {
                    wand = c.iterator().next();
                }
            }
            RankFloor.Rank ceiling = RankFloor.floorForStage(appliedStage);
            StringBuilder sb = new StringBuilder();
            if (wand == null) {
                sb.append("Rank: no wand found.");
            } else {
                String key = resolveRankKey(mage, wand);
                if (key == null) {
                    key = "?";
                }
                int manaMax = ((Number) invoke(wand, "getManaMax", NOARGS)).intValue();
                int manaRegen = ((Number) invoke(wand, "getManaRegeneration", NOARGS)).intValue();
                sb.append("Rank: ").append(title(key))
                        .append(" — mana pool ").append(manaMax)
                        .append(", regen ").append(manaRegen).append("/s.");
            }
            sb.append(" Stage ").append(appliedStage).append(" ceiling: ")
                    .append(title(ceiling.key()))
                    .append(" (").append(ceiling.manaMax()).append(" mana, ")
                    .append(ceiling.manaRegeneration()).append("/s).");
            if (plugin.earningEnabled()) {
                long credited = creditedTotal(player, mage);
                EarnedRank.Thresholds t = plugin.thresholds();
                RankFloor.Rank earned = EarnedRank.rankFor(credited, t);
                RankFloor.Rank target = EarnedRank.targetFor(credited, ceiling, t);
                sb.append(" Credited casts: ").append(credited)
                        .append(" — earned ").append(title(earned.key()))
                        .append(", your rank target is ").append(title(target.key())).append(".");
                if (earned.next() != null) {
                    sb.append(" Next rank ").append(title(earned.next().key()))
                            .append(" at ").append(t.at(earned.next()))
                            .append(" credited casts (")
                            .append(EarnedRank.remainingToNext(credited, t))
                            .append(" to go).");
                } else {
                    sb.append(" You have earned the top rank.");
                }
                if (earned.ordinal() > ceiling.ordinal()) {
                    sb.append(" The seal ceiling holds you at ")
                            .append(title(ceiling.key()))
                            .append(" until the next seal falls.");
                }
            } else {
                sb.append(" Rank earning is currently OFF — the stage floor rules apply.");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Could not read your mage record: " + e.getMessage();
        }
    }

    /** Package-private rank read for WandLoreHook. */
    public String rankKeyOf(Object mage, Object wand) {
        return resolveRankKey(mage, wand);
    }

    /** Number of mage records in Magic's OWN data folder (the census). */
    public int mageDataCensus() {
        try {
            Plugin magic = Bukkit.getPluginManager().getPlugin("Magic");
            if (magic == null) {
                return -1;
            }
            File dir = new File(new File(magic.getDataFolder(), "data"), "players");
            // Magic 11.x stores one .dat per mage (older layouts used
            // .yml); backups (.dat.bak) are not mages and are excluded.
            File[] files = dir.listFiles((d, name) ->
                    name.endsWith(".dat") || name.endsWith(".yml"));
            return files == null ? 0 : files.length;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Resolve a wand's current rank key from the sources that agree
     * on the live server (rig-verified against Magic 11.2.5):
     *   1. the wand's own path (set after its first upgrade),
     *   2. the mage's active properties path (the wand, when active),
     *   3. the mage class path (a fresh mage's class starts at
     *      beginner and is always present),
     *   4. the wand's template key when it is itself a rank key
     *      (a never-upgraded Quartermaster wand: beginner template
     *      IS the beginner rank),
     *   5. an exact frame match (manaMax + regen equal a rank's
     *      frame — the QM frames are exact by construction).
     * Returns null only when NO source yields a rank: a genuinely
     * foreign wand, which is never guessed at.
     */
    private String resolveRankKey(Object mage, Object wand) {
        try {
            if (wand != null) {
                Object path = invoke(wand, "getPath", NOARGS);
                if (path != null) {
                    String key = (String) invoke(path, "getKey", NOARGS);
                    if (RankFloor.fromKey(key) != null) {
                        return key;
                    }
                }
            }
            if (mage != null) {
                Object active = null;
                try {
                    active = invoke(mage, "getActiveProperties", NOARGS);
                } catch (Exception ignored) {
                    // older/other Mage impls: fall through to the class
                }
                if (active != null) {
                    Object path = invoke(active, "getPath", NOARGS);
                    if (path != null) {
                        String key = (String) invoke(path, "getKey", NOARGS);
                        if (RankFloor.fromKey(key) != null) {
                            return key;
                        }
                    }
                }
                Object mageClass = null;
                try {
                    mageClass = invoke(mage, "getActiveClass", NOARGS);
                } catch (Exception ignored) {
                    // no class accessor: fall through to template/frame
                }
                if (mageClass != null) {
                    Object path = invoke(mageClass, "getPath", NOARGS);
                    if (path != null) {
                        String key = (String) invoke(path, "getKey", NOARGS);
                        if (RankFloor.fromKey(key) != null) {
                            return key;
                        }
                    }
                }
            }
            if (wand != null) {
                String template = (String) invoke(wand, "getTemplateKey", NOARGS);
                if (RankFloor.fromKey(template) != null) {
                    return template;
                }
                int manaMax = ((Number) invoke(wand, "getManaMax", NOARGS)).intValue();
                int manaRegen = ((Number) invoke(wand, "getManaRegeneration", NOARGS)).intValue();
                for (RankFloor.Rank r : RankFloor.Rank.values()) {
                    if (RankFloor.frameMatches(r, manaMax, manaRegen)) {
                        return r.key();
                    }
                }
            }
        } catch (Exception ignored) {
            // resolution is best-effort across sources; null = unknown
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Reflection helpers
    // ------------------------------------------------------------------

    private static final Class<?>[] NOARGS = new Class<?>[0];

    private Object controller() throws Exception {
        Plugin magic = Bukkit.getPluginManager().getPlugin("Magic");
        if (magic == null || !magic.isEnabled()) {
            return null;
        }
        Method m = magic.getClass().getMethod("getController");
        return m.invoke(magic);
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... args)
            throws Exception {
        Method m = target.getClass().getMethod(name, types);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    /** The Wand object for a wand item physically carried by the
     *  player (inventory or ender chest), or null. Uses ONLY the
     *  controller's getIfWand — NEVER getWand(item): getWand wraps
     *  ANY item as a new wand (it is the wand-ify call), so scanning
     *  with it manufactures phantom wands out of ordinary items
     *  (rig-caught in 0.2.0). getIfWand returns an object only when
     *  the item really is a wand. A candidate must also look like a
     *  real wand (bound, templated, or holding spells). */
    private Object wandFromCarriedItem(Object controller, Player player) {
        try {
            Method getIfWand = controller.getClass().getMethod("getIfWand", ItemStack.class);
            java.util.List<ItemStack> stacks = new ArrayList<>();
            for (ItemStack s : player.getInventory().getContents()) {
                if (s != null) stacks.add(s);
            }
            for (ItemStack s : stacks) {
                Object w = getIfWand.invoke(controller, s);
                if (w != null && looksLikeRealWand(w)) {
                    return w;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // NOTE on scope: the carried-item scan and the bound-carried
    // check cover the player's INVENTORY only (see itemIsCarried).
    // A wand sitting in the ender chest is deliberately treated as
    // not-live and deferred to activation: Magic's upgrade() lands
    // on the mage's ACTIVE properties, and an ender-chest wand
    // cannot be activated where it sits.

    /** A real wand is bound, templated, or holding spells; a phantom
     *  materialization is none of the three. */
    private boolean looksLikeRealWand(Object wand) {
        try {
            Object bound = invoke(wand, "isBound", NOARGS);
            if (Boolean.TRUE.equals(bound)) {
                return true;
            }
            Object template = invoke(wand, "getTemplateKey", NOARGS);
            if (template != null) {
                return true;
            }
            Object spells = invoke(wand, "getSpells", NOARGS);
            return spells instanceof Collection<?> c && !c.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /** True when the wand's live item is in the player's inventory.
     *  The ender chest is deliberately NOT "carried" for stepping:
     *  Magic's upgrade() lands on the mage's active properties, and
     *  a wand in the ender chest cannot be activated where it sits —
     *  it defers to activation like the pouch case. */
    private boolean itemIsCarried(Player player, Object wand) {
        try {
            Object itemObj = invoke(wand, "getItem", NOARGS);
            if (!(itemObj instanceof ItemStack item) || item.getType().isAir()) {
                return false;
            }
            for (ItemStack s : player.getInventory().getContents()) {
                if (s != null && (s == item || s.equals(item))) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /** PouchQuery (ExcelPouch, Oct 18 spec) — reflective, fail-open. */
    private boolean pouchHoldsWand(UUID playerId) {
        try {
            Plugin pouch = Bukkit.getPluginManager().getPlugin("ExcelPouch");
            if (pouch == null || !pouch.isEnabled()) {
                return false;
            }
            Class<?> queryClass = Class.forName(
                    "com.winkreyes.excelpouch.api.PouchQuery", true, pouch.getClass().getClassLoader());
            RegisteredServiceProvider<?> rsp =
                    Bukkit.getServicesManager().getRegistration(queryClass);
            if (rsp == null || rsp.getProvider() == null) {
                return false;
            }
            Object result = queryClass.getMethod("holdsKind", UUID.class, String.class)
                    .invoke(rsp.getProvider(), playerId, "magic_wand");
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false; // fail open: treat "no answer" as "holds nothing"
        }
    }

    private ReconcileResult result(Player player, CeilingPlan.Outcome outcome,
                                   String from, String to, int steps, Object wand) {
        int manaMax = -1;
        int manaRegen = -1;
        if (wand != null) {
            try {
                manaMax = ((Number) invoke(wand, "getManaMax", NOARGS)).intValue();
                manaRegen = ((Number) invoke(wand, "getManaRegeneration", NOARGS)).intValue();
            } catch (Exception ignored) {
                // result object only; the wand read is best-effort here
            }
        }
        return new ReconcileResult(player.getName(), outcome, from, to, steps, manaMax, manaRegen);
    }

    static String title(String rankKey) {
        if (rankKey == null || rankKey.isEmpty()) {
            return "Unknown";
        }
        return rankKey.substring(0, 1).toUpperCase(Locale.ROOT) + rankKey.substring(1);
    }
}
