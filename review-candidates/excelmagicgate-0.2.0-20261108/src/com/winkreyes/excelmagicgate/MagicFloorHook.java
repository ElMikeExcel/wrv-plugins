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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Bukkit side of stage-floor reconciliation (0.2.0, Item ㉝-b).
 *
 * Everything Magic-facing is REFLECTIVE, the house pattern for this
 * plugin (no compile-time Magic dependency): the controller comes
 * from the Magic plugin's own getController(), mages and wands are
 * the objects Magic returns, and each step is performed by Magic's
 * OWN ProgressionPath.upgrade(mage, wand) — the same in-place
 * mutation Magic's automatic progression uses, which preserves the
 * wand item, its bound owner, its spell inventory and its identity
 * while setting mana_max / mana_regeneration from the next rank.
 *
 * Wand discovery order: active wand -> offhand wand -> a bound wand
 * whose live item is physically in the player's inventory or ender
 * chest. A bound wand that exists only as serialized bytes inside
 * the Utility Pouch is DETECTED through ExcelPouch's PouchQuery
 * service (Oct 18 spec; query-only by contract, looked up
 * reflectively, fail-open when absent) and DEFERRED explicitly to
 * wand activation — this class never reads or writes pouch bytes.
 * The join leg, the held-item activation leg and the watch-tick
 * sweep complete a deferred step as soon as the wand is live again.
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
    public record ReconcileResult(String player, FloorPlan.Outcome outcome,
                                  String fromRank, String toRank, int steps,
                                  int manaMax, int manaRegeneration) {}

    private final ExcelMagicGate plugin;
    private final Set<String> deferredLogged = ConcurrentHashMap.newKeySet();
    private volatile String lastSummary = "no reconcile run yet";
    private final java.util.Map<String, Integer> counters = new ConcurrentHashMap<>();

    public MagicFloorHook(ExcelMagicGate plugin) {
        this.plugin = plugin;
    }

    public String lastSummary() {
        return lastSummary;
    }

    public String counterLine() {
        if (counters.isEmpty()) {
            return "none yet";
        }
        List<String> parts = new ArrayList<>();
        counters.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                .forEach(e -> parts.add(e.getKey() + "=" + e.getValue()));
        return String.join(", ", parts);
    }

    private void bump(String key) {
        counters.merge(key, 1, Integer::sum);
    }

    // ------------------------------------------------------------------
    // Entry points
    // ------------------------------------------------------------------

    /** Reconcile every online mage to the stage's floor (apply + sweep legs). */
    public List<ReconcileResult> reconcileAllOnline(int stage, String source) {
        List<ReconcileResult> out = new ArrayList<>();
        RankFloor.Rank floor = RankFloor.floorForStage(stage);
        int census = mageDataCensus();
        for (Player p : Bukkit.getOnlinePlayers()) {
            ReconcileResult r = reconcilePlayer(p, stage, source);
            if (r != null) {
                out.add(r);
            }
        }
        long stepped = out.stream().filter(r -> r.outcome() == FloorPlan.Outcome.STEP).count();
        lastSummary = source + ": stage " + stage + " floor " + floor.key()
                + ", online checked " + out.size() + ", stepped " + stepped
                + ", mage-data census " + census + " files";
        plugin.getLogger().info("Floor reconcile (" + lastSummary + ")");
        return out;
    }

    /** Reconcile one player; null when the player has no Magic mage. */
    public ReconcileResult reconcilePlayer(Player player, int stage, String source) {
        RankFloor.Rank floor = RankFloor.floorForStage(stage);
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

            FloorPlan.Decision decision = FloorPlan.decide(access, currentKey, floor);
            switch (decision.outcome()) {
                case ALREADY_AT_FLOOR -> {
                    bump("already");
                    return result(player, decision.outcome(), currentKey, currentKey, 0, wand);
                }
                case DEFERRED_PARKED_POUCH, DEFERRED_NO_LIVE_WAND -> {
                    bump(decision.outcome() == FloorPlan.Outcome.DEFERRED_PARKED_POUCH
                            ? "deferred-pouch" : "deferred-no-live-wand");
                    String onceKey = player.getUniqueId() + ":" + decision.outcome();
                    if (deferredLogged.add(onceKey)) {
                        plugin.getLogger().info("Floor reconcile DEFERRED for " + player.getName()
                                + ": bound wand is "
                                + (decision.outcome() == FloorPlan.Outcome.DEFERRED_PARKED_POUCH
                                        ? "parked in the Utility Pouch (serialized; PouchQuery is query-only)"
                                        : "not a live carried item")
                                + " — the step to the stage " + stage + " floor (" + floor.key()
                                + ") completes on wand activation (held-item / join / sweep legs).");
                    }
                    return result(player, decision.outcome(), currentKey, currentKey, 0, wand);
                }
                case SKIP_UNKNOWN_RANK -> {
                    bump("skip-unknown-rank");
                    plugin.getLogger().warning("Floor reconcile SKIPPED for " + player.getName()
                            + ": wand path key '" + currentKey + "' is not a WRV rank — never guessed at.");
                    return result(player, decision.outcome(), currentKey, currentKey, 0, wand);
                }
                case STEP -> {
                    // fall through to the stepping loop below
                }
            }

            // --- step up via Magic's own upgrade(), re-reading the rank ---
            // Proven shape (rig, Magic 11.2.5): a Quartermaster wand's
            // own path field is UNSET until its first upgrade, so the
            // rank is resolved from the agreeing sources (wand path ->
            // active properties -> mage class path -> wand template ->
            // exact frame match), declared on the wand with setPath
            // (the set lands even though Magic's change notification
            // NPEs on the null old path — caught and ignored), and each
            // step calls the CURRENT path object's upgrade(mage, wand)
            // fetched from the controller by key. Advancement is always
            // judged by re-reading the wand, never by exception state.
            String fromKey = currentKey;
            int stepsDone = 0;
            // Magic's upgrade() lands on the mage's ACTIVE properties
            // (doUpgrade -> upgradeTo(activeProperties), bytecode- and
            // rig-verified): stepping a wand that is not active moves
            // the class-side properties and leaves the wand item's own
            // frame behind. So activate the wand first — Magic's own
            // activation, the same thing taking it in hand does — then
            // re-discover and step the ACTIVE wand object.
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
                    plugin.getLogger().warning("Floor reconcile STALLED for " + player.getName()
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
                    plugin.getLogger().info("Floor reconcile: upgrade() for " + player.getName()
                            + " threw after/while stepping from '" + currentKey + "' ("
                            + stepError.getCause() + ") — re-reading the wand to judge.");
                }
                // Re-discover FRESH objects before judging: for a
                // wand that is carried but not active, upgrade() lands
                // on the item's stored data, and the Wand object this
                // loop started with can be a stale materialization —
                // the first rig stall was exactly that (the step had
                // landed; the stale object still read the old rank).
                Discovered fresh = discoverWand(controller(), mage, player);
                Object freshWand = fresh.wand() != null ? fresh.wand() : wand;
                String afterKey = resolveRankKey(mage, freshWand);
                if (afterKey == null || afterKey.equals(currentKey)) {
                    plugin.getLogger().warning("Floor reconcile STALLED for " + player.getName()
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
            boolean verified = now != null && now.ordinal() >= floor.ordinal()
                    && RankFloor.frameMatches(now, manaMax, manaRegen);
            if (stepsDone > 0 && verified) {
                bump("stepped");
                deferredLogged.remove(player.getUniqueId() + ":" + FloorPlan.Outcome.DEFERRED_PARKED_POUCH);
                deferredLogged.remove(player.getUniqueId() + ":" + FloorPlan.Outcome.DEFERRED_NO_LIVE_WAND);
                plugin.getLogger().info("Floor reconcile: " + player.getName() + " stepped "
                        + fromKey + " -> " + currentKey + " (" + stepsDone + " upgrade(s), "
                        + source + ", stage " + stage + " floor " + floor.key()
                        + ") — pool " + manaMax + ", regen " + manaRegen + "/s, verified on the wand.");
                player.sendMessage(ExcelMagicGate.colorText(
                        "&6[MagicGate] &eYour wand rises to &f" + title(currentKey)
                                + "&e — mana pool &f" + manaMax + "&e, regen &f" + manaRegen
                                + "/s&e (stage " + stage + " floor)."));
                return result(player, FloorPlan.Outcome.STEP, fromKey, currentKey, stepsDone, wand);
            }
            if (stepsDone > 0) {
                bump("step-unverified");
                plugin.getLogger().warning("Floor reconcile UNVERIFIED for " + player.getName()
                        + ": stepped " + stepsDone + " time(s) to '" + currentKey
                        + "' but the wand reads pool " + manaMax + " / regen " + manaRegen
                        + " — expected the " + floor.key() + " frame ("
                        + floor.manaMax() + " / " + floor.manaRegeneration() + ").");
                return result(player, FloorPlan.Outcome.STEP, fromKey, currentKey, stepsDone, wand);
            }
            bump("no-progress");
            return result(player, FloorPlan.Outcome.ALREADY_AT_FLOOR, fromKey, currentKey, 0, wand);
        } catch (Exception e) {
            bump("errors");
            plugin.getLogger().warning("Floor reconcile failed for " + player.getName()
                    + " (" + source + "): " + e);
            return null;
        }
    }

    /** Wand discovery result: the wand object (or null), whether a
     *  live item backs it, and the mage's bound-wand list. */
    private record Discovered(Object wand, boolean liveItem, List<Object> bound) {}

    /**
     * Discover the player's wand, in order: active wand -> offhand
     * wand -> a bound wand whose live item is carried -> the Wand
     * object for a carried wand ITEM (a freshly pouch-extracted wand
     * whose object Magic has not registered yet — the controller
     * materializes it, so the activation leg never races that
     * registration).
     */
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

    /** Read-only rank/frame view of a player for /magicgate rank. */
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
            RankFloor.Rank floor = RankFloor.floorForStage(appliedStage);
            if (wand == null) {
                return "Rank: no wand found. Stage " + appliedStage + " floor is "
                        + floor.key() + " (" + floor.manaMax() + " mana, "
                        + floor.manaRegeneration() + "/s regen).";
            }
            String key = resolveRankKey(mage, wand);
            if (key == null) {
                key = "?";
            }
            int manaMax = ((Number) invoke(wand, "getManaMax", NOARGS)).intValue();
            int manaRegen = ((Number) invoke(wand, "getManaRegeneration", NOARGS)).intValue();
            RankFloor.Rank rank = RankFloor.fromKey(key);
            StringBuilder sb = new StringBuilder();
            sb.append("Rank: ").append(title(key))
                    .append(" — mana pool ").append(manaMax)
                    .append(", regen ").append(manaRegen).append("/s.");
            sb.append(" Stage ").append(appliedStage).append(" floor: ").append(title(floor.key()))
                    .append(" (").append(floor.manaMax()).append(" mana, ")
                    .append(floor.manaRegeneration()).append("/s).");
            if (rank != null && rank.ordinal() < floor.ordinal()) {
                sb.append(" Your floor step is owed — hold your wand and it applies automatically.");
            } else if (rank != null) {
                sb.append(" You are at or above the floor.");
            }
            if (rank != null && rank.next() != null) {
                sb.append(" Learning every ").append(key).append(" spell still earns ")
                        .append(title(rank.next().key())).append(" the classic way.");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Could not read your mage record: " + e.getMessage();
        }
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
     *  (rig-caught: a phantom with no template, no spells, pool 0
     *  was nearly stepped). getIfWand returns an object only when
     *  the item really is a wand, materializing it from the item's
     *  stored data — exactly the pouch-extracted case. A candidate
     *  must also look like a real wand (bound, templated, or holding
     *  spells) before it is trusted. */
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

    private ReconcileResult result(Player player, FloorPlan.Outcome outcome,
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
