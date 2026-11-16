package com.winkreyes.excelprotect;

import com.winkreyes.excelprotect.api.GameplayEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.server.PluginEnableEvent;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ExcelProtect 0.1.10 (P-PROTECT-WATCH-1) — the AUDIT WATCH glue: the feeds
 * that fill the per-player audit log (AuditLog). Every feed is fail-soft in
 * the house adapter style — a missing neighbor (Magic, ExcelSummons) turns
 * its feed off quietly; the log core and the emitter door keep working.
 *
 * Feeds, and what each can honestly claim:
 *  1. SPELL CASTS — Protect's own reflective Magic CastEvent registration
 *     (the CombatCore pattern). Completed casts only, with the spell's mana
 *     cost (Spell.getRequiredCost().getMana()) and the caster's mana /
 *     mana_max / effective_mana_max read off the Mage at cast time.
 *  2. SUMMON SPAWN / DESPAWN — observed through the ExcelSummonsAPI
 *     attribution facade, reflectively (no compile-time dependency): a
 *     spawn-event fast path with delayed attribution checks, a 5 s sweep of
 *     entities near online players (adoptions register long after the
 *     entity spawned, so the sweep is what sees them), EntityDeathEvent
 *     (reason=died) and EntityRemoveEvent (reason mapped from its Cause).
 *     Despawn reasons are the OBSERVED removal causes — the watch never
 *     claims "expired" it did not see; the bridge's own reason arrives via
 *     feed 4 when ExcelSummons emits it.
 *  3. MANA-RESERVE RE-DERIVATIONS — after every observed spawn/despawn the
 *     watch replays the derivation (AuditReserve: CAST-only holds, the
 *     ceiling/floor clamp walk) over its observed registry for that owner,
 *     priced from the LIVE ExcelSummons config percent table and stamped
 *     with the mage's live pool numbers when Magic answers. These lines
 *     carry source=derived — they are Protect's replay of Summons' law,
 *     labeled as such, never passed off as Summons' own bookkeeping.
 *  4. THE EMITTER DOOR — GameplayEvent (api package): ExcelSummons (and any
 *     gameplay plugin) fires its own facts — above all summon_refusal with
 *     the exact reason and the same numbers shown to the player, which only
 *     the refusing plugin knows. Door records keep the emitter's numbers
 *     verbatim and carry its source name. Emitter-driven spawn/despawn
 *     lines also sync the observed registry, but do NOT trigger a derived
 *     reserve line (the emitter owns its reserve records).
 *  5. OBSERVED REFUSALS — a reflective Magic PreCastEvent registration at
 *     MONITOR: when a summon-family cast ends the PreCast phase cancelled,
 *     the watch records cast_refused with source=observed_precast and the
 *     derived held percent at that moment. The refusing plugin and its
 *     reason are NOT visible to Protect and the record says so — it is a
 *     sighting, not the refusal itself.
 *
 * Every record is written twice by design: to the player's own audit file
 * (the watch's readable log) and onto the canonical audit stream (so
 * inspect / export / player timelines keep the whole story in one pull).
 * Excel — Wink Reyes Valley
 */
public final class AuditWatch implements Listener {

    private static final Set<String> DOOR_KINDS = Set.of(
            "spell_cast", "summon_spawn", "summon_despawn", "mana_reserve", "summon_refusal");
    private static final Map<String, Integer> DEFAULT_PERCENTS = Map.of(
            "archer", 10, "bruiser", 15, "guardian", 25, "revenant", 20, "ghast", 25,
            "wolf", 10, "familiar", 5, "cat", 5, "mount", 10);

    private final Plugin plugin;
    private final Ledger ledger;
    private final Settings cfg;
    private final AuditLog log;

    private record Observed(UUID entityId, UUID owner, String ownerName, String spell,
                            String branch, String origin, String entityType, long seq) { }
    private final Map<UUID, Observed> observed = new ConcurrentHashMap<>();
    private long seqCounter = 0;

    private volatile boolean magicHooked;
    private volatile boolean summonsState; // last resolved bridge state, for the READY line

    // live ExcelSummons config cache (percents + ceiling + floor + families)
    private volatile Map<String, Integer> percents = DEFAULT_PERCENTS;
    private volatile Set<String> families = DEFAULT_PERCENTS.keySet();
    private volatile int ceiling = 75, floorPct = 20;
    private volatile long percentsMtime = -1;

    public AuditWatch(Plugin plugin, Ledger ledger, Settings cfg, File dataDir) {
        this.plugin = plugin;
        this.ledger = ledger;
        this.cfg = cfg;
        this.log = new AuditLog(dataDir);
        refreshPercents(true);
    }

    public AuditLog log() { return log; }
    public String flushAll() { return log.flushAll(); }
    public boolean magicHooked() { return magicHooked; }
    public boolean summonsOn() { resolveSummons(); return summonsState; }

    // ------------------------------------------------------------ recording
    /** Write one record to the player's audit file AND the canonical audit stream. */
    private void emit(Json.Obj o, Location loc) {
        log.record(o, loc == null || loc.getWorld() == null ? null : loc.getWorld().getName(),
                loc == null ? null : loc.getX(), loc == null ? null : loc.getY(),
                loc == null ? null : loc.getZ());
        ledger.emit("audit", loc, o);
    }

    private static Json.Obj base(String type, String source, Player p) {
        Json.Obj o = new Json.Obj().put("type", type).put("source", source);
        if (p != null) o.put("player", p.getName()).put("player_uuid", p.getUniqueId().toString());
        return o;
    }

    private static Json.Obj base(String type, String source, String name, UUID uuid) {
        Json.Obj o = new Json.Obj().put("type", type).put("source", source);
        if (name != null) o.put("player", name);
        if (uuid != null) o.put("player_uuid", uuid.toString());
        return o;
    }

    // ------------------------------------------------------------ Magic feed
    /** Fail-soft Magic adapter: CastEvent + PreCastEvent by name (CombatCore pattern). */
    public void hookMagic() {
        Plugin magic = Bukkit.getPluginManager().getPlugin("Magic");
        if (magic == null || !magic.isEnabled()) {
            plugin.getLogger().info("[ExcelProtect] audit watch: Magic not present — cast feed off (fail-soft)");
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Class<? extends org.bukkit.event.Event> castCls = (Class<? extends org.bukkit.event.Event>)
                    Class.forName("com.elmakers.mine.bukkit.api.event.CastEvent");
            Bukkit.getPluginManager().registerEvent(castCls, this, EventPriority.MONITOR,
                    (l, ev) -> onCast(ev), plugin, true);
            @SuppressWarnings("unchecked")
            Class<? extends org.bukkit.event.Event> preCls = (Class<? extends org.bukkit.event.Event>)
                    Class.forName("com.elmakers.mine.bukkit.api.event.PreCastEvent");
            Bukkit.getPluginManager().registerEvent(preCls, this, EventPriority.MONITOR,
                    (l, ev) -> onPreCast(ev), plugin, true);
            magicHooked = true;
            plugin.getLogger().info("[ExcelProtect] audit watch: Magic CastEvent + PreCastEvent feeds hooked");
        } catch (Throwable t) {
            plugin.getLogger().info("[ExcelProtect] audit watch: Magic events unavailable — cast feed off (fail-soft)");
        }
    }

    private void onCast(org.bukkit.event.Event ev) {
        if (!cfg.auditCasts()) return;
        try {
            Object result = invokeOrNull(ev, "getSpellResult");
            if (result instanceof Enum<?> en) {
                String rn = en.name();
                if (!rn.equals("CAST") && !rn.equals("CAST_TARGET") && !rn.equals("CAST_SELF")) return;
            }
            Object mage = ev.getClass().getMethod("getMage").invoke(ev);
            if (mage == null) return;
            Object p = invokeOrNull(mage, "getPlayer");
            if (!(p instanceof Player) ) {
                Object ent = invokeOrNull(mage, "getEntity");
                if (ent instanceof Player) p = ent;
            }
            if (!(p instanceof Player caster)) return;
            Object spell = ev.getClass().getMethod("getSpell").invoke(ev);
            if (spell == null) return;
            Object keyObj = invokeOrNull(spell, "getKey");
            if (!(keyObj instanceof String key)) return;
            Json.Obj o = base("spell_cast", "magic", caster);
            o.put("spell", key).put("spell_base", AuditReserve.baseKey(key));
            if (result != null) o.put("result", String.valueOf(result));
            Object cost = invokeOrNull(spell, "getRequiredCost");
            Object mana = cost == null ? null : invokeOrNull(cost, "getMana");
            if (mana instanceof Number n) o.put("mana_cost", n.intValue());
            putMageNumbers(o, mage);
            emit(o, caster.getLocation());
        } catch (Throwable ignored) { }
    }

    private void onPreCast(org.bukkit.event.Event ev) {
        if (!cfg.auditRefusals()) return;
        try {
            if (!(ev instanceof org.bukkit.event.Cancellable c) || !c.isCancelled()) return;
            Object mage = ev.getClass().getMethod("getMage").invoke(ev);
            if (mage == null) return;
            Object p = invokeOrNull(mage, "getPlayer");
            if (!(p instanceof Player) ) {
                Object ent = invokeOrNull(mage, "getEntity");
                if (ent instanceof Player) p = ent;
            }
            if (!(p instanceof Player caster)) return;
            Object spell = ev.getClass().getMethod("getSpell").invoke(ev);
            if (spell == null) return;
            Object keyObj = invokeOrNull(spell, "getKey");
            if (!(keyObj instanceof String key)) return;
            refreshPercents(false);
            if (!families.contains(AuditReserve.baseKey(key))) return; // summon-family only
            Json.Obj o = base("cast_refused", "observed_precast", caster);
            o.put("spell", key).put("spell_base", AuditReserve.baseKey(key));
            AuditReserve.Derivation d = deriveFor(caster.getUniqueId());
            o.put("held_total_percent", d.heldTotalPercent());
            putMageNumbers(o, mage);
            o.put("note", "cancellation observed at MONITOR; the refusing plugin and its "
                    + "reason are not visible to Protect — an emitter-reported summon_refusal "
                    + "record carries the exact reason and numbers");
            emit(o, caster.getLocation());
        } catch (Throwable ignored) { }
    }

    /** mana / mana_max / effective_mana_max off a Mage, reflectively, when they answer. */
    private static void putMageNumbers(Json.Obj o, Object mage) {
        Object mana = invokeOrNull(mage, "getMana");
        if (mana instanceof Number n) o.put("mana_after", Ledger.round(n.doubleValue()));
        Object max = invokeOrNull(mage, "getManaMax");
        if (max instanceof Number n) o.put("mana_max", n.intValue());
        Object eff = invokeOrNull(mage, "getEffectiveManaMax");
        if (eff instanceof Number n) o.put("effective_mana_max", n.intValue());
    }

    /** The live Mage for an online player via the Magic plugin's API, or null. */
    private Object mageFor(Player p) {
        try {
            Plugin magic = Bukkit.getPluginManager().getPlugin("Magic");
            if (magic == null || !magic.isEnabled()) return null;
            for (Method m : magic.getClass().getMethods()) {
                if (m.getName().equals("getMage") && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].isAssignableFrom(Player.class)) {
                    return m.invoke(magic, p);
                }
            }
        } catch (Throwable ignored) { }
        return null;
    }

    private static Object invokeOrNull(Object target, String method) {
        try { return target.getClass().getMethod(method).invoke(target); }
        catch (Throwable t) { return null; }
    }

    // ------------------------------------------------------------ Summons bridge (reflective)
    private volatile Class<?> summonsApi;
    private volatile boolean summonsResolved;

    private Class<?> resolveSummons() {
        if (!summonsResolved) {
            summonsResolved = true;
            try {
                summonsApi = Class.forName("com.winkreyes.excelsummons.ExcelSummonsAPI");
                summonsState = true;
            } catch (Throwable t) {
                summonsApi = null;
                summonsState = false;
            }
        }
        return summonsApi;
    }

    @EventHandler public void onPluginEnable(PluginEnableEvent e) {
        // ExcelSummons may enable after Protect: forget the cached miss once.
        if (e.getPlugin().getName().equals("ExcelSummons")) { summonsResolved = false; resolveSummons(); }
    }

    private Object summonsRecord(Entity ent) {
        Class<?> api = resolveSummons();
        if (api == null || ent == null) return null;
        try {
            Method is = api.getMethod("isSummon", Entity.class);
            if (!Boolean.TRUE.equals(is.invoke(null, ent))) return null;
            return api.getMethod("getRecord", Entity.class).invoke(null, ent);
        } catch (Throwable t) { return null; }
    }

    private static String recStr(Object rec, String method) {
        Object v = invokeOrNull(rec, method);
        return v == null ? null : String.valueOf(v);
    }

    // ------------------------------------------------------------ summon observation
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(EntitySpawnEvent e) {
        if (!cfg.auditSummons()) return;
        if (!(e.getEntity() instanceof LivingEntity)) return;
        Entity ent = e.getEntity();
        scheduleAttribution(ent, 3L);
        scheduleAttribution(ent, 20L);
    }

    private void scheduleAttribution(Entity ent, long delayTicks) {
        try {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try { tryAttribute(ent, "observed"); } catch (Throwable ignored) { }
            }, delayTicks);
        } catch (Throwable ignored) { }
    }

    /** The 5 s sweep: adoptions register long after spawn, so proximity to a player is the net. */
    public void sweep() {
        if (!cfg.auditSummons()) return;
        try {
            refreshPercents(false);
            for (Player p : Bukkit.getOnlinePlayers()) {
                for (Entity ent : p.getNearbyEntities(48, 32, 48)) {
                    if (ent instanceof LivingEntity) tryAttribute(ent, "observed_sweep");
                }
            }
            // Reconciliation: a still-valid, still-present entity the bridge no
            // longer registers has left the registry under our feet (a despawn
            // path with no removal event) — record it as unregistered.
            for (Observed ob : new ArrayList<>(observed.values())) {
                Entity ent = Bukkit.getEntity(ob.entityId());
                if (ent == null || !ent.isValid()) continue; // removal events own those
                if (summonsRecord(ent) == null) recordDespawn(ob, "unregistered", "observed_sweep", ent.getLocation());
            }
        } catch (Throwable ignored) { }
    }

    private void tryAttribute(Entity ent, String source) {
        if (ent == null || observed.containsKey(ent.getUniqueId())) return;
        Object rec = summonsRecord(ent);
        if (rec == null) return;
        Object ownerObj = invokeOrNull(rec, "ownerId");
        if (!(ownerObj instanceof UUID owner)) return;
        String spell = recStr(rec, "spellKey");
        String ownerName = ownerName(owner);
        Observed ob = new Observed(ent.getUniqueId(), owner, ownerName,
                spell == null ? "" : AuditReserve.baseKey(spell.toLowerCase()),
                lower(recStr(rec, "branch")), lower(recStr(rec, "origin")),
                ent.getType().name().toLowerCase(), ++seqCounter);
        if (observed.putIfAbsent(ent.getUniqueId(), ob) != null) return;
        Json.Obj o = base("summon_spawn", source, ownerName, owner);
        o.put("summon", ob.spell()).put("entity_uuid", ob.entityId().toString())
         .put("entity_type", ob.entityType());
        if (ob.branch() != null) o.put("branch", ob.branch());
        if (ob.origin() != null) o.put("origin", ob.origin());
        Object exp = invokeOrNull(rec, "expiresAtMillis");
        if (exp instanceof Number n) o.put("expires_ms", n.longValue());
        emit(o, ent.getLocation());
        deriveAndEmit(ob, "summon_spawn");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent e) {
        if (!cfg.auditSummons()) return;
        Observed ob = observed.get(e.getEntity().getUniqueId());
        if (ob == null) {
            Object rec = summonsRecord(e.getEntity());
            if (rec == null) return;
            tryAttribute(e.getEntity(), "observed");
            ob = observed.get(e.getEntity().getUniqueId());
            if (ob == null) return;
        }
        recordDespawn(ob, "died", "observed", e.getEntity().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemove(EntityRemoveEvent e) {
        if (!cfg.auditSummons()) return;
        Observed ob = observed.get(e.getEntity().getUniqueId());
        if (ob == null) return;
        String reason = reasonFor(e.getCause());
        if (reason == null) return; // DEATH is the death handler's line
        recordDespawn(ob, reason, "observed", e.getEntity().getLocation());
    }

    /** EntityRemoveEvent.Cause -> the watch's despawn reason vocabulary. Pure. */
    public static String reasonFor(EntityRemoveEvent.Cause cause) {
        if (cause == null) return "removed";
        return switch (cause) {
            case DEATH -> null; // EntityDeathEvent records reason=died
            case UNLOAD -> "unloaded";
            case PLUGIN -> "removed_plugin";
            case DESPAWN -> "despawned";
            case DISCARD -> "discarded";
            case OUT_OF_WORLD -> "out_of_world";
            case PLAYER_QUIT -> "owner_quit";
            default -> "removed_" + cause.name().toLowerCase();
        };
    }

    private void recordDespawn(Observed ob, String reason, String source, Location loc) {
        if (observed.remove(ob.entityId()) == null) return; // first path wins; never double-record
        Json.Obj o = base("summon_despawn", source, ob.ownerName(), ob.owner());
        o.put("summon", ob.spell()).put("entity_uuid", ob.entityId().toString())
         .put("entity_type", ob.entityType()).put("reason", reason);
        if (ob.origin() != null) o.put("origin", ob.origin());
        emit(o, loc);
        deriveAndEmit(ob, "summon_despawn");
    }

    private static String ownerName(UUID owner) {
        org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(owner);
        String n = op == null ? null : op.getName();
        return n == null ? owner.toString() : n;
    }

    private static String lower(String s) { return s == null ? null : s.toLowerCase(); }

    // ------------------------------------------------------------ reserve derivation
    private AuditReserve.Derivation deriveFor(UUID owner) {
        List<Observed> mine = new ArrayList<>();
        for (Observed ob : observed.values()) if (ob.owner().equals(owner)) mine.add(ob);
        mine.sort((a, b) -> Long.compare(a.seq(), b.seq()));
        List<AuditReserve.Summon> in = new ArrayList<>();
        for (Observed ob : mine) in.add(new AuditReserve.Summon(ob.spell(), "cast".equals(ob.origin())));
        return AuditReserve.derive(in, percents, ceiling, floorPct);
    }

    private void deriveAndEmit(Observed trigger, String triggerType) {
        if (!cfg.auditManaReserve()) return;
        try {
            refreshPercents(false);
            List<Observed> mine = new ArrayList<>();
            for (Observed ob : observed.values()) if (ob.owner().equals(trigger.owner())) mine.add(ob);
            mine.sort((a, b) -> Long.compare(a.seq(), b.seq()));
            List<AuditReserve.Summon> in = new ArrayList<>();
            int triggerIdx = -1;
            for (int i = 0; i < mine.size(); i++) {
                if (mine.get(i).entityId().equals(trigger.entityId())) triggerIdx = i;
                in.add(new AuditReserve.Summon(mine.get(i).spell(), "cast".equals(mine.get(i).origin())));
            }
            // A despawn trigger is already out of the registry: replay the walk
            // as it stands AFTER the removal; its own hold is 0 by construction.
            AuditReserve.Derivation d = AuditReserve.derive(in, percents, ceiling, floorPct);
            Json.Obj o = base("mana_reserve", "derived", trigger.ownerName(), trigger.owner());
            o.put("trigger", triggerType).put("summon", trigger.spell());
            int trigHold = 0;
            if (triggerType.equals("summon_spawn") && triggerIdx >= 0
                    && Boolean.TRUE.equals(d.included().get(triggerIdx))) {
                trigHold = AuditReserve.percentFor(percents, trigger.spell());
            }
            o.put("hold_percent", trigHold);
            o.put("held_total_percent", d.heldTotalPercent());
            o.put("ceiling_percent", ceiling).put("floor_percent", floorPct);
            if (!d.appliedCounts().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                d.appliedCounts().forEach((k, v) -> {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(k).append(" x").append(v);
                });
                o.put("holding", sb.toString());
            }
            Player owner = Bukkit.getPlayer(trigger.owner());
            if (owner != null) {
                Object mage = mageFor(owner);
                if (mage != null) {
                    Object max = invokeOrNull(mage, "getManaMax");
                    Object eff = invokeOrNull(mage, "getEffectiveManaMax");
                    if (max instanceof Number n) {
                        o.put("mana_max", n.intValue());
                        o.put("held_total", AuditReserve.heldUnits(d.heldTotalPercent(), n.doubleValue()));
                    }
                    if (eff instanceof Number n) o.put("usable_mana", n.intValue())
                            .put("effective_mana_max", n.intValue());
                }
            }
            emit(o, owner == null ? null : owner.getLocation());
        } catch (Throwable ignored) { }
    }

    // ------------------------------------------------------------ live Summons config
    /** Percent table / ceiling / floor from the LIVE ExcelSummons config (mtime-cached). */
    private void refreshPercents(boolean force) {
        try {
            File f = new File(plugin.getDataFolder().getParentFile(), "ExcelSummons/config.yml");
            if (!f.exists()) return;
            long mt = f.lastModified();
            if (!force && mt == percentsMtime) return;
            percentsMtime = mt;
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            Map<String, Integer> table = new LinkedHashMap<>();
            org.bukkit.configuration.ConfigurationSection sec = y.getConfigurationSection("summon-reserve-percent");
            if (sec != null) for (String k : sec.getKeys(false)) table.put(k.toLowerCase(), sec.getInt(k));
            if (!table.isEmpty()) percents = Map.copyOf(table);
            ceiling = y.getInt("reserve-ceiling-percent", ceiling);
            floorPct = y.getInt("reserve-floor-percent", floorPct);
            org.bukkit.configuration.ConfigurationSection spells = y.getConfigurationSection("summon-spells");
            if (spells != null && !spells.getKeys(false).isEmpty()) {
                java.util.Set<String> fam = new java.util.HashSet<>();
                for (String k : spells.getKeys(false)) fam.add(AuditReserve.baseKey(k.toLowerCase()));
                fam.addAll(percents.keySet());
                families = Set.copyOf(fam);
            }
        } catch (Throwable ignored) { }
    }

    // ------------------------------------------------------------ emitter door
    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameplay(GameplayEvent e) {
        if (!cfg.auditWatch()) return;
        try {
            String kind = e.kind();
            if (kind == null || !DOOR_KINDS.contains(kind)) return;
            Map<String, Object> f = e.fields();
            if (f == null) return;
            Object name = f.get("player"), uuid = f.get("player_uuid");
            if (!(name instanceof String) || !(uuid instanceof String)) {
                // No per-player home: count it through the log core's drop path.
                log.record(new Json.Obj().put("type", kind), null, null, null, null);
                return;
            }
            Json.Obj o = new Json.Obj().put("type", kind);
            f.forEach((k, v) -> {
                if (v instanceof String || v instanceof Number || v instanceof Boolean) o.put(k, v);
            });
            if (!o.has("source")) o.put("source", "emitter");
            // Registry sync for emitter-driven spawn/despawn (no derived reserve:
            // the emitter owns its reserve records).
            if (kind.equals("summon_spawn") || kind.equals("summon_despawn")) {
                syncFromDoor(kind, f);
            }
            Player p = Bukkit.getPlayer(UUID.fromString((String) uuid));
            emit(o, p == null ? null : p.getLocation());
        } catch (Throwable ignored) { }
    }

    private void syncFromDoor(String kind, Map<String, Object> f) {
        try {
            if (!(f.get("entity_uuid") instanceof String eu) || !(f.get("player_uuid") instanceof String pu)) return;
            UUID entityId = UUID.fromString(eu);
            UUID owner = UUID.fromString(pu);
            if (kind.equals("summon_despawn")) { observed.remove(entityId); return; }
            String summon = f.get("summon") instanceof String s ? AuditReserve.baseKey(s.toLowerCase()) : "";
            String origin = f.get("origin") instanceof String s ? s.toLowerCase() : null;
            String branch = f.get("branch") instanceof String s ? s.toLowerCase() : null;
            String type = f.get("entity_type") instanceof String s ? s : "";
            observed.putIfAbsent(entityId, new Observed(entityId, owner,
                    String.valueOf(f.get("player")), summon, branch, origin, type, ++seqCounter));
        } catch (Throwable ignored) { }
    }
}
