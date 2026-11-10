package com.winkreyes.excelmagicgate;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The cast-observation side of the earning system (0.3.0; DESIGN.md
 * §4). Everything Magic-facing is REFLECTIVE, the house pattern for
 * this plugin: Magic's PreCastEvent / CastEvent / PathUpgradeEvent
 * are registered dynamically by class name from Magic's own
 * classloader (PluginManager.registerEvent), and their payloads are
 * read reflectively. Bukkit-native events (health regain, food
 * change, damage) are ordinary compile-time listeners.
 *
 * The observation design mirrors the shipped ExcelWandBars RankUpHook
 * pipeline (the house precedent the design adopts wholesale):
 * - PreCastEvent timestamps the caster.
 * - A heal counts only when a CUSTOM EntityRegainHealthEvent landed
 *   for the caster or a context target after the pre-cast timestamp
 *   (Magic's HealAction never fires it for a full-health target, so
 *   the event PROVES the heal did work). feed additionally accepts
 *   an observed food-level increase (DESIGN.md §11 Q5 — the rig
 *   decides whether FeedAction produces one; both signals are wired).
 * - Damage counts on the context's targeted entities; projectile
 *   families whose hits resolve through child contexts may instead
 *   credit from a correlated EntityDamageByEntityEvent landing in a
 *   short post-cast window (deferred decision, DESIGN.md §4).
 * - CastEvent fires BEFORE Magic increments its own cast_count and
 *   can fire for sub-contexts of one cast; only the base-context
 *   event is ever considered, and MagicGate's own ledger (never
 *   Magic's counter) is the credited authority.
 * - PathUpgradeEvent is Magic's own auto-upgrade announcing itself;
 *   it is not Cancellable, so enforcement is reactive: an upgrade
 *   past the player's target schedules a corrective reconcile (the
 *   60 s sweep is the backstop for any route that skips the event).
 */
public final class MagicCreditHook implements Listener {

    /** A projectile cast awaiting correlated-hit evidence. */
    private record PendingCorrelated(UUID caster, String family, long expiresAtMs,
                                     boolean playerTargetKnown) {}

    private final ExcelMagicGate plugin;
    private final Map<UUID, Long> preCastAt = new ConcurrentHashMap<>();
    private final Map<UUID, Long> regainAt = new ConcurrentHashMap<>();
    private final Map<UUID, Long> foodUpAt = new ConcurrentHashMap<>();
    private final Map<UUID, PendingCorrelated> pending = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> pairCredits = new HashMap<>();
    private final Map<String, Integer> counters = new ConcurrentHashMap<>();
    private volatile boolean registered;

    public MagicCreditHook(ExcelMagicGate plugin) {
        this.plugin = plugin;
    }

    public String counterLine() {
        if (counters.isEmpty()) {
            return "none yet";
        }
        java.util.List<String> parts = new java.util.ArrayList<>();
        counters.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(e -> parts.add(e.getKey() + "=" + e.getValue()));
        return String.join(", ", parts);
    }

    private void bump(String key) {
        counters.merge(key, 1, Integer::sum);
    }

    // ------------------------------------------------------------------
    // Registration (dynamic, reflective — no compile-time Magic dep)
    // ------------------------------------------------------------------

    public boolean register() {
        Plugin magic = Bukkit.getPluginManager().getPlugin("Magic");
        if (magic == null || !magic.isEnabled()) {
            plugin.getLogger().warning("Credit hook NOT registered: Magic is absent — "
                    + "no casts will credit until Magic is present and the server restarts.");
            return false;
        }
        try {
            ClassLoader cl = magic.getClass().getClassLoader();
            registerMagicEvent(cl, "com.elmakers.mine.bukkit.api.event.PreCastEvent",
                    e -> onPreCast(e));
            registerMagicEvent(cl, "com.elmakers.mine.bukkit.api.event.CastEvent",
                    e -> onCast(e));
            registerMagicEvent(cl, "com.elmakers.mine.bukkit.api.event.PathUpgradeEvent",
                    e -> onPathUpgrade(e));
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
            plugin.getLogger().info("Credit hook registered (PreCast/Cast/PathUpgrade "
                    + "observed reflectively from Magic's classloader).");
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Credit hook registration failed: " + e);
            return false;
        }
    }

    private interface EventSink {
        void accept(Event event);
    }

    private void registerMagicEvent(ClassLoader cl, String className, EventSink sink)
            throws Exception {
        @SuppressWarnings("unchecked")
        Class<? extends Event> eventClass =
                (Class<? extends Event>) Class.forName(className, true, cl);
        Bukkit.getPluginManager().registerEvent(eventClass, this, EventPriority.MONITOR,
                (listener, event) -> {
                    try {
                        sink.accept(event);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Credit hook event handling failed ("
                                + className + "): " + e);
                    }
                }, plugin);
    }

    public boolean registered() {
        return registered;
    }

    // ------------------------------------------------------------------
    // Bukkit-native observation (compile-time listeners)
    // ------------------------------------------------------------------

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent e) {
        if (e.getRegainReason() == EntityRegainHealthEvent.RegainReason.CUSTOM
                && e.getAmount() > 0) {
            regainAt.put(e.getEntity().getUniqueId(), System.currentTimeMillis());
        }
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFoodChange(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p && e.getFoodLevel() > p.getFoodLevel()) {
            foodUpAt.put(p.getUniqueId(), System.currentTimeMillis());
        }
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        Player caster = null;
        if (e.getDamager() instanceof Player p) {
            caster = p;
        } else if (e.getDamager() instanceof Projectile proj
                && proj.getShooter() instanceof Player p) {
            caster = p;
        }
        if (caster == null) {
            return;
        }
        PendingCorrelated pc = pending.remove(caster.getUniqueId());
        if (pc == null || System.currentTimeMillis() > pc.expiresAtMs()) {
            return;
        }
        // Correlated hit landed: re-run the decision with the
        // correlation flag set (the daily cap and pair window are
        // re-read at credit time, so the bound still holds).
        boolean playerTarget = e.getEntity() instanceof Player
                && !e.getEntity().getUniqueId().equals(caster.getUniqueId());
        CreditPolicy policy = plugin.creditPolicy();
        CreditPolicy.Observation o = new CreditPolicy.Observation(
                pc.family(), true, true, false, false, true, playerTarget,
                plugin.earnedDailyCount(caster.getUniqueId(), pc.family()),
                playerTarget ? pairCount(caster.getUniqueId(),
                        ((Player) e.getEntity()).getUniqueId()) : 0);
        if (policy.decide(o) == CreditPolicy.Decision.CREDIT) {
            bump("credited-correlated");
            if (playerTarget) {
                recordPair(caster.getUniqueId(), ((Player) e.getEntity()).getUniqueId());
            }
            plugin.applyCredit(caster, pc.family());
        } else {
            bump("denied-correlated");
        }
    }

    // ------------------------------------------------------------------
    // Magic events (reflective payloads)
    // ------------------------------------------------------------------

    private void onPreCast(Event event) {
        try {
            Object mage = invoke(event, "getMage");
            Player p = playerOf(mage);
            if (p != null) {
                preCastAt.put(p.getUniqueId(), System.currentTimeMillis());
            }
        } catch (Exception ignored) {
            // observation only; a failed read must never disturb a cast
        }
    }

    private void onCast(Event event) {
        try {
            Object mage = invoke(event, "getMage");
            Player caster = playerOf(mage);
            if (caster == null) {
                return;
            }
            Object spell = invoke(event, "getSpell");
            String family = CreditPolicy.familyOf(spellKeyOf(spell));
            CreditPolicy policy = plugin.creditPolicy();
            CreditPolicy.GateMode mode = policy.modeFor(family);
            if (mode == CreditPolicy.GateMode.NO_CREDIT) {
                bump("denied-excluded");
                return;
            }

            Object result = invoke(event, "getSpellResult");
            boolean success = result != null
                    && Boolean.TRUE.equals(invoke(result, "isSuccess"));
            Object context = invoke(event, "getContext");
            boolean baseContext = context != null
                    && context == invoke(context, "getBaseContext");

            // Targeted-entity evidence (context undo list + direct target).
            boolean targetedNonCaster = false;
            boolean playerTarget = false;
            UUID firstPlayerTarget = null;
            if (context != null) {
                Object targeted = invoke(context, "getTargetedEntities");
                if (targeted instanceof Collection<?> c) {
                    for (Object o : c) {
                        if (o instanceof Entity ent) {
                            if (!ent.getUniqueId().equals(caster.getUniqueId())) {
                                targetedNonCaster = true;
                                if (ent instanceof Player tp) {
                                    playerTarget = true;
                                    if (firstPlayerTarget == null) {
                                        firstPlayerTarget = tp.getUniqueId();
                                    }
                                }
                            }
                        }
                    }
                }
                Object direct = invoke(context, "getTargetEntity");
                if (direct instanceof Entity ent
                        && !ent.getUniqueId().equals(caster.getUniqueId())) {
                    targetedNonCaster = true;
                    if (ent instanceof Player tp) {
                        playerTarget = true;
                        if (firstPlayerTarget == null) {
                            firstPlayerTarget = tp.getUniqueId();
                        }
                    }
                }
            }

            long since = preCastAt.getOrDefault(caster.getUniqueId(),
                    System.currentTimeMillis() - 2000) - 50;
            boolean healObserved = observedSince(regainAt, caster.getUniqueId(), since);
            if (!healObserved && context != null) {
                Object direct = invoke(context, "getTargetEntity");
                if (direct instanceof Entity ent) {
                    healObserved = observedSince(regainAt, ent.getUniqueId(), since);
                }
            }
            if (!healObserved && family.equals("feed")) {
                // feed's work signal may be hunger, not health (Q5).
                healObserved = observedSince(foodUpAt, caster.getUniqueId(), since)
                        || (context != null && invoke(context, "getTargetEntity")
                                instanceof Entity ent
                                && observedSince(foodUpAt, ent.getUniqueId(), since));
            }

            CreditPolicy.Observation o = new CreditPolicy.Observation(
                    family, success, baseContext, healObserved, targetedNonCaster,
                    false, playerTarget,
                    plugin.earnedDailyCount(caster.getUniqueId(), family),
                    firstPlayerTarget == null ? 0
                            : pairCount(caster.getUniqueId(), firstPlayerTarget));
            CreditPolicy.Decision decision = policy.decide(o);
            switch (decision) {
                case CREDIT -> {
                    bump("credited");
                    if (playerTarget && firstPlayerTarget != null
                            && (mode == CreditPolicy.GateMode.DAMAGE_STRICT
                                    || mode == CreditPolicy.GateMode.DAMAGE_CORRELATED)) {
                        recordPair(caster.getUniqueId(), firstPlayerTarget);
                    }
                    plugin.applyCredit(caster, family);
                }
                case DENIED_NO_TARGET -> {
                    bump("denied-no-target");
                    // Projectile families get the correlated second
                    // chance: a hit may land just after finish().
                    if (mode == CreditPolicy.GateMode.DAMAGE_CORRELATED && success
                            && baseContext) {
                        pending.put(caster.getUniqueId(), new PendingCorrelated(
                                caster.getUniqueId(), family,
                                System.currentTimeMillis() + 3000, playerTarget));
                        bump("pending-correlated");
                    }
                }
                case DENIED_HEAL_NOT_OBSERVED -> bump("denied-heal-not-observed");
                case DENIED_DAILY_CAP -> bump("denied-daily-cap");
                case DENIED_PAIR_WINDOW -> bump("denied-pair-window");
                case DENIED_NOT_SUCCESS -> bump("denied-not-success");
                case DENIED_SUBCONTEXT -> bump("denied-subcontext");
                default -> bump("denied-other");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Credit hook cast handling failed: " + e);
        }
    }

    /** Magic promoted a mage on its own: if the new rank is above the
     *  player's earned target, correct it on the next tick (reactive
     *  enforcement; the watch sweep is the backstop). */
    private void onPathUpgrade(Event event) {
        try {
            Object mage = invoke(event, "getMage");
            Player p = playerOf(mage);
            Object newPath = invoke(event, "getNewPath");
            if (p == null || newPath == null) {
                return;
            }
            String newKey = String.valueOf(invoke(newPath, "getKey"));
            RankFloor.Rank newRank = RankFloor.fromKey(newKey);
            if (newRank == null) {
                return;
            }
            RankFloor.Rank target = plugin.targetRankFor(p.getUniqueId());
            if (newRank.ordinal() > target.ordinal()) {
                bump("auto-upgrade-corrected");
                plugin.getLogger().info("Credit hook: Magic auto-upgraded " + p.getName()
                        + " to " + newKey + ", above their earned target " + target.key()
                        + " — corrective reconcile scheduled.");
                Bukkit.getScheduler().runTask(plugin,
                        () -> plugin.reconcileNow(p, "auto-upgrade guard"));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Credit hook PathUpgrade handling failed: " + e);
        }
    }

    // ------------------------------------------------------------------
    // Pair window (hit-trade bound)
    // ------------------------------------------------------------------

    private int pairCount(UUID caster, UUID target) {
        Deque<Long> q = pairCredits.get(caster + "->" + target);
        if (q == null) {
            return 0;
        }
        prunePair(q);
        return q.size();
    }

    private void recordPair(UUID caster, UUID target) {
        Deque<Long> q = pairCredits.computeIfAbsent(
                caster + "->" + target, k -> new ArrayDeque<>());
        prunePair(q);
        q.addLast(System.currentTimeMillis());
    }

    private void prunePair(Deque<Long> q) {
        long cutoff = System.currentTimeMillis()
                - plugin.pairWindowMinutes() * 60_000L;
        while (!q.isEmpty() && q.peekFirst() < cutoff) {
            q.pollFirst();
        }
    }

    // ------------------------------------------------------------------
    // Reflective helpers
    // ------------------------------------------------------------------

    private static boolean observedSince(Map<UUID, Long> map, UUID id, long since) {
        Long at = map.get(id);
        return at != null && at >= since;
    }

    private static Player playerOf(Object mage) throws Exception {
        if (mage == null) {
            return null;
        }
        Object isPlayer = invoke(mage, "isPlayer");
        if (!Boolean.TRUE.equals(isPlayer)) {
            return null;
        }
        Object p = invoke(mage, "getPlayer");
        return p instanceof Player player ? player : null;
    }

    private static String spellKeyOf(Object spell) throws Exception {
        if (spell == null) {
            return "";
        }
        Object key = invoke(spell, "getSpellKey");
        if (key != null) {
            Object base = invoke(key, "getBaseKey");
            if (base != null) {
                return String.valueOf(base);
            }
        }
        Object k = invoke(spell, "getKey");
        return k == null ? "" : String.valueOf(k);
    }

    private static Object invoke(Object target, String name) throws Exception {
        if (target == null) {
            return null;
        }
        Method m = target.getClass().getMethod(name);
        m.setAccessible(true);
        return m.invoke(target);
    }
}
