package com.winkreyes.excelprotect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * ExcelProtect — COMBAT ANALYTICS family (SPEC §3.10): the shared fight-window damage
 * ledger (ExcelTelemetry's model, proven 2026-11-01), per-event combat damage/healing
 * records for player-involved combat, fight aggregation with a config gap window, and
 * Magic CastEvent heal-spell claims via a fail-soft reflective adapter. Boss/seal
 * fight tables arrive via api.FightTableEvent.
 *
 * HEAL ATTRIBUTION (㉚.10 find-and-fix, 2026-11-08 — see HEAL-ATTRIBUTION-FINDINGS.md):
 * the 0.1.7 pairing could never fire, for stacked reasons: (1) the reflective cast
 * chain called Mage.getController().getPlayer(), a method MagicController does not
 * have (Mage.getPlayer() is the real one), so every cast threw NoSuchMethodException
 * into a silent catch and the claims map stayed permanently empty; (2) CastEvent is
 * fired by Magic from BaseSpell.finish(), AFTER HealAction has already fired the
 * CUSTOM regain inside the cast, so forward-only pairing misses by construction —
 * matching here is bidirectional and deferred, BossTrial 0.3.1's actual model
 * (FightTracker resolves claims against regains at snapshot with |Δt| ≤ window);
 * (3) the 1 s single-consume claim window cannot cover the WRV heal-over-time spell
 * (500 ms delay, 10 ticks over 5 s) — claims are no longer consumed and the window
 * is combat.heal-claim-window-ms (default 6500); (4) credited heals now join the
 * fight the target is actually in (boss-anchored) instead of a separate area-cell
 * fight whose zero damage total got it dropped by fight-min-damage; (5) splash
 * potions are captured at PotionSplashEvent (their regains are MAGIC / MAGIC_REGEN,
 * never CUSTOM, so the old reason gate made them unattributable). Every heal record
 * carries the effective/overheal split (effective capped at health actually missing);
 * fight rows accumulate EFFECTIVE healing, as BossTrial's tracker does.
 *
 * DAMAGE ATTRIBUTION (§S1–S2, SPELL-ATTRIBUTION-GAP-20261108): the same claims now
 * label DAMAGE. Magic's DamageAction delivers through a cached fake ThrownPotion
 * entity (shooter = caster), so the Bukkit event wears a potion costume: cause
 * MAGIC, damager ThrownPotion. A claim whose target set contains the victim is
 * the discriminator — claim-matched hits record spell + delivery="spell" and the
 * costume comes off; a thrown-potion hit with NO matching claim is a real potion
 * (delivery="potion"); plain projectile hits keep delivery="projectile"; melee
 * causes are never spell-labelled (delivery="hand"). Because CastEvent fires
 * AFTER the damage (post-cast ordering, same as heals), a spell-shaped hit with
 * no claim yet is buffered briefly and resolved when the claim lands; the dealt
 * incident layer re-checks claims again when an incident closes.
 *
 * RE-BASE NOTE (0.1.8 rebuild, 2026-11-08): this file is the ㉚.10 heal-attribution
 * build merged onto the LIVE 0.1.7 tree — it keeps 0.1.7's kill-parity elite note
 * (EliteProbe at damage time) and restores peekClaim for DealtWatch/HealingWatch;
 * the 1 s consume-claim model is retired (claims are a list, never consumed).
 * Excel — Wink Reyes Valley
 */
public final class CombatCore implements Listener {
    private final Ledger ledger;
    private final Settings cfg;
    private final Plugin plugin;
    final DamageLedger kills = new DamageLedger(5000);
    private final List<HealAttribution.Claim> claims = new ArrayList<>();
    private final List<PendingHeal> pending = new ArrayList<>();
    private final List<SplashRegen> splashRegens = new ArrayList<>();
    private final List<SplashHit> splashHits = new ArrayList<>();
    private final List<PendingDamage> pendingDamage = new ArrayList<>();
    private long castSeq = 0;
    private boolean magicHooked = false;
    private final Map<String, Fight> fights = new HashMap<>();
    private long fightSeq = 0;

    /** A regain held briefly for a CastEvent that has not arrived yet (post-cast event order). */
    private record PendingHeal(UUID target, String name, Location loc, double raw,
                               double eff, double over, String reason, long ms) { }

    /** A splash REGENERATION attribution: ticks on this target credit the thrower until expiry. */
    private record SplashRegen(UUID target, UUID thrower, String throwerName, long expiryMs, String castId) { }

    /** A spell-shaped damage record held briefly for its CastEvent (post-cast ordering). */
    private record PendingDamage(Json.Obj obj, Location loc, UUID caster, UUID victim, String cause,
                                 String projectile, String fallbackDelivery, long ms) { }

    /** A damage label decision: the spell (nullable), how the hit was delivered, and
     *  whether a claim could still arrive for it (buffer instead of emitting bare). */
    public record DamageLabel(String spell, String delivery, boolean claimMayArrive) { }

    /** A splash instant-health credit already written; the vanilla MAGIC regain that follows is the same heal. */
    private record SplashHit(UUID target, long ms) { }

    private static final class Fight {
        final String id; long lastMs; double total;
        final Map<String, double[]> players = new TreeMap<>(); // name -> [dealt, taken, healedDone, healedReceived]
        final Map<String, Map<String, Double>> healSpells = new HashMap<>(); // healer -> spell -> effective (§S2 split)
        Fight(String id, long now) { this.id = id; this.lastMs = now; }
        double[] row(String name) { return players.computeIfAbsent(name, k -> new double[4]); }
    }

    public CombatCore(Plugin plugin, Ledger ledger, Settings cfg) {
        this.plugin = plugin; this.ledger = ledger; this.cfg = cfg;
    }

    /** Fail-soft Magic adapter: register for CastEvent by name; absence is silent. */
    public void hookMagic() {
        Plugin magic = Bukkit.getPluginManager().getPlugin("Magic");
        if (magic == null || !magic.isEnabled()) { plugin.getLogger().info("[ExcelProtect] combat: Magic not present — spell attribution off (fail-soft)"); return; }
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Event> cls = (Class<? extends Event>)
                    Class.forName("com.elmakers.mine.bukkit.api.event.CastEvent");
            Bukkit.getPluginManager().registerEvent(cls, this, EventPriority.MONITOR,
                    (EventExecutor) (l, ev) -> onCast(ev), plugin, true);
            magicHooked = true;
            plugin.getLogger().info("[ExcelProtect] combat: Magic CastEvent feed hooked (spell attribution on)");
        } catch (Throwable t) {
            plugin.getLogger().info("[ExcelProtect] combat: Magic CastEvent unavailable — spell attribution off (fail-soft)");
        }
    }

    /**
     * CastEvent handler (reflective, fail-soft per cast). 0.1.8 fixes vs 0.1.7:
     * the caster comes from Mage.getPlayer() directly (the old chain
     * getController().getPlayer() does not exist on MagicController and threw on
     * every cast); the spell result is filtered to completed casts (BossTrial's
     * CAST / CAST_TARGET / CAST_SELF rule); the cast context's target entity and
     * targeted-entity set become target-specific claims, not a targetless guess;
     * and after the claim lands, buffered regains are re-matched against it,
     * because the regain usually fired BEFORE this event (post-cast ordering).
     */
    private void onCast(Event ev) {
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
            long now = System.currentTimeMillis();
            // Targets from the cast context first (BossTrial's order), spell second.
            List<UUID> targets = new ArrayList<>();
            Object ctx = invokeOrNull(ev, "getContext");
            if (ctx != null) {
                Object te = invokeOrNull(ctx, "getTargetEntity");
                if (te instanceof Entity e && !targets.contains(e.getUniqueId())) targets.add(e.getUniqueId());
                Object coll = invokeOrNull(ctx, "getTargetedEntities");
                if (coll instanceof Iterable<?> it)
                    for (Object o : it)
                        if (o instanceof Entity e && targets.size() < 16 && !targets.contains(e.getUniqueId()))
                            targets.add(e.getUniqueId());
            }
            if (targets.isEmpty()) {
                Object te = invokeOrNull(spell, "getTargetEntity");
                if (te instanceof Player tp) targets.add(tp.getUniqueId());
            }
            Location cl = caster.getLocation();
            if (targets.isEmpty()) {
                addClaim(caster, null, key, now, cl);
            } else {
                for (UUID t : targets) addClaim(caster, t, key, now, cl);
            }
            resolvePending();
            resolvePendingDamage();
        } catch (Throwable ignored) { }
    }

    private static Object invokeOrNull(Object target, String method) {
        try { return target.getClass().getMethod(method).invoke(target); }
        catch (Throwable t) { return null; }
    }

    private void addClaim(Player caster, UUID target, String spell, long now, Location loc) {
        claims.add(new HealAttribution.Claim(caster.getUniqueId(), caster.getName(), target, spell,
                now, loc.getWorld() == null ? null : loc.getWorld().getName(), loc.getX(), loc.getZ(),
                ++castSeq));
        pruneClaims(now);
    }

    private void pruneClaims(long now) {
        long keep = cfg.combatHealClaimWindowMs() + 5_000L;
        claims.removeIf(c -> now - c.ms() > keep);
    }

    /**
     * Damage-path spell label (unchanged role from 0.1.7): the caster's freshest
     * cast within 1 s names the damage record's spell. Heal-spell claims are NOT
     * consumed here — a heal cast's claim belongs to the heal ticks that follow it,
     * and 0.1.7's unconditional consume let any attack within 1 s steal it.
     */
    /**
     * Read a caster's freshest claim WITHOUT consuming it (restored for the 0.1.8
     * merged model — 0.1.7's contract, which DealtWatch/HealingWatch were built
     * against; the staged ㉚.10 tree dropped it and broke both watches). Claims
     * are a shared list, never consumed: one cast legitimately explains its heal
     * ticks, its damage ticks and both incident layers. Freshness = the claim
     * window (combat.heal-claim-window-ms), not 0.1.7's 1 s guess.
     */
    public String peekClaim(UUID caster) {
        long now = System.currentTimeMillis();
        HealAttribution.Claim best = null;
        for (HealAttribution.Claim c : claims)
            if (c.caster().equals(caster) && now - c.ms() <= cfg.combatHealClaimWindowMs()
                    && (best == null || c.ms() > best.ms())) best = c;
        return best == null ? null : best.spell();
    }

    /** The claim that labels a damage hit, or null. Targetless claims require clean air. */
    public HealAttribution.Claim matchDamageClaim(UUID caster, UUID victim, long hitMs,
                                                  boolean allowTargetless) {
        return HealAttribution.bestDamageClaim(claims, caster, victim, hitMs,
                cfg.combatHealClaimWindowMs(), allowTargetless, cfg.magicHealSpells());
    }

    /**
     * Late label lookup for the dealt incident layer: an incident closes at least
     * an incident-gap (2 s) after its last tick, by which time the cast's claim
     * has landed. EXACT-target claims only (never a targetless guess), anywhere
     * in [start − window, end + window].
     */
    public HealAttribution.Claim lateDamageClaim(UUID caster, UUID victim, long startMs, long endMs) {
        HealAttribution.Claim best = null;
        for (HealAttribution.Claim c : claims) {
            if (!c.caster().equals(caster) || victim == null || !victim.equals(c.target())) continue;
            if (HealAttribution.isHealSpell(c.spell(), cfg.magicHealSpells())) continue;
            if (c.ms() < startMs - cfg.combatHealClaimWindowMs()
                    || c.ms() > endMs + cfg.combatHealClaimWindowMs()) continue;
            if (best == null || c.ms() > best.ms()) best = c;
        }
        return best;
    }

    /**
     * §S1 damage labeling — the thrown-potion costume test. Inputs are the RAW
     * event shape: cause name, and the projectile item key BEFORE shooter
     * resolution (null when the damager was not a projectile).
     *   melee causes (ENTITY_ATTACK / SWEEP_ATTACK) → "hand", never spell-labelled;
     *   a claim whose target set contains the victim → that spell, delivery "spell";
     *   thrown potion + no matching claim → a REAL potion, delivery "potion";
     *   other projectile → delivery "projectile"; MAGIC without a projectile →
     *   targetless claims may label; anything else → "hand".
     * claimMayArrive is set when the shape is spell-shaped (MAGIC cause or the
     * potion costume) but no claim exists YET — CastEvent is post-cast.
     */
    public DamageLabel labelDamage(UUID caster, UUID victim, String cause, String projectileKey, long hitMs) {
        if ("ENTITY_ATTACK".equals(cause) || "SWEEP_ATTACK".equals(cause))
            return new DamageLabel(null, "hand", false);
        boolean potionCostume = HealAttribution.isPotionProjectile(projectileKey);
        boolean magicCause = "MAGIC".equals(cause);
        if (projectileKey == null && !magicCause) return new DamageLabel(null, "hand", false);
        boolean allowTargetless = projectileKey == null;
        HealAttribution.Claim c = matchDamageClaim(caster, victim, hitMs, allowTargetless);
        if (c != null) return new DamageLabel(c.spell(), "spell", false);
        String fallback = potionCostume ? "potion" : projectileKey != null ? "projectile" : "hand";
        return new DamageLabel(null, fallback, magicHooked && (potionCostume || magicCause));
    }

    /** Resolve buffered spell-shaped damage records against claims that just landed. */
    private void resolvePendingDamage() {
        if (pendingDamage.isEmpty()) return;
        long now = System.currentTimeMillis();
        Iterator<PendingDamage> it = pendingDamage.iterator();
        while (it.hasNext()) {
            PendingDamage pd = it.next();
            boolean allowTargetless = pd.projectile() == null;
            HealAttribution.Claim c = matchDamageClaim(pd.caster(), pd.victim(), pd.ms(), allowTargetless);
            if (c != null) {
                it.remove();
                pd.obj().put("spell", c.spell()).put("delivery", "spell");
                ledger.emit("combat", pd.loc(), pd.obj());
            } else if (now - pd.ms() > 2_500L) {
                it.remove();
                applyDelivery(pd.obj(), pd.fallbackDelivery(), pd.projectile());
                ledger.emit("combat", pd.loc(), pd.obj());
            }
        }
    }

    /** Flush any buffered damage records at their fallback labels (shutdown/timer sweep). */
    public void flushPendingDamage() { resolvePendingDamage(); }

    private static void applyDelivery(Json.Obj o, String delivery, String projectileKey) {
        o.put("delivery", delivery);
        if (projectileKey != null && !"spell".equals(delivery)) o.put("projectile", projectileKey);
    }

    private HealAttribution.Claim matchClaim(Player target, String reason, long now) {
        Location l = target.getLocation();
        double r = cfg.combatHealProximityBlocks();
        return HealAttribution.bestClaim(claims, target.getUniqueId(), reason,
                l.getWorld() == null ? null : l.getWorld().getName(), l.getX(), l.getZ(), now,
                cfg.combatHealClaimWindowMs(), r * r, cfg.magicHealSpells());
    }

    static String attackerKey(Entity damager, EntityDamageEvent.DamageCause cause) {
        if (damager == null) return "env:" + cause.name().toLowerCase();
        if (damager instanceof Player p) return "player:" + p.getUniqueId();
        return "entity:" + damager.getType().getKey() + ":" + damager.getUniqueId();
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity victim)) return;
        Entity damager = null;
        if (e instanceof EntityDamageByEntityEvent by) {
            damager = by.getDamager();
            if (damager instanceof Projectile proj && proj.getShooter() instanceof Entity shooter) damager = shooter;
        }
        double dmg = e.getFinalDamage();
        if (dmg <= 0) return;
        // §S1: the RAW projectile identity, captured before shooter resolution —
        // Magic's DamageAction delivers spell damage through a cached fake
        // ThrownPotion whose shooter is the caster, so this key is the costume.
        String rawProjectile = null;
        if (e instanceof EntityDamageByEntityEvent by0 && by0.getDamager() instanceof Projectile proj0)
            rawProjectile = proj0.getType().getKey().toString();
        String kind = damager instanceof Player ? "player" : damager != null ? "entity" : "environment";
        String label = damager instanceof Player p ? p.getName()
                : damager != null ? damager.getType().getKey().toString() : null;
        kills.record(victim.getUniqueId(), System.currentTimeMillis(), attackerKey(damager, e.getCause()),
                kind, damager == null ? null : damager.getUniqueId().toString(), label,
                e.getCause().name(), dmg);
        // 0.1.7 kill parity: cache the victim's elite level on the book at
        // damage time (Telemetry's pattern — EliteMobs unregisters the mob
        // during its own death handling, so an at-death probe is too late).
        DamageLedger.Book kb = kills.book(victim.getUniqueId());
        if (kb != null) kb.noteElite(EliteProbe.levelOf(victim));
        // combat family: only player-involved events (SPEC §3.10)
        boolean playerInvolved = victim instanceof Player || damager instanceof Player;
        if (!playerInvolved) return;
        if (cfg.combatDamage()) {
            long now = System.currentTimeMillis();
            Json.Obj o = new Json.Obj().put("type", "combat_damage")
                    .put("victim", victim.getType().getKey().toString())
                    .put("victim_uuid", victim.getUniqueId().toString())
                    .put("amount", round2(dmg)).put("cause", e.getCause().name());
            if (victim.getCustomName() != null) o.put("victim_name", victim.getCustomName());
            if (damager instanceof Player dp) {
                o.put("attacker", dp.getName()).put("attacker_uuid", dp.getUniqueId().toString());
                ItemFact w = ItemFact.of(dp.getInventory().getItemInMainHand(), false);
                if (w != null) o.put("weapon", w.toJson());
                DamageLabel lab = labelDamage(dp.getUniqueId(), victim.getUniqueId(),
                        e.getCause().name(), rawProjectile, now);
                if (lab.spell() != null) {
                    o.put("spell", lab.spell()).put("delivery", "spell");
                    ledger.emit("combat", victim.getLocation(), o);
                } else if (lab.claimMayArrive()) {
                    // CastEvent fires after the damage: hold the record briefly
                    // for its claim instead of writing it in the potion costume.
                    pendingDamage.add(new PendingDamage(o, victim.getLocation(),
                            dp.getUniqueId(), victim.getUniqueId(), e.getCause().name(),
                            rawProjectile, lab.delivery(), now));
                } else {
                    applyDelivery(o, lab.delivery(), rawProjectile);
                    ledger.emit("combat", victim.getLocation(), o);
                }
            } else if (damager != null) {
                o.put("attacker_entity", damager.getType().getKey().toString())
                 .put("attacker_uuid", damager.getUniqueId().toString());
                ledger.emit("combat", victim.getLocation(), o);
            } else {
                ledger.emit("combat", victim.getLocation(), o);
            }
        }
        if (cfg.combatFights()) {
            Fight f = currentFight(victim, damager);
            if (f != null) {
                f.lastMs = System.currentTimeMillis(); f.total += dmg;
                if (damager instanceof Player dp) f.row(dp.getName())[0] += dmg;
                if (victim instanceof Player vp) f.row(vp.getName())[1] += dmg;
            }
        }
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeal(EntityRegainHealthEvent e) {
        if (!cfg.combatHealing() || !(e.getEntity() instanceof Player target)) return;
        double amt = e.getAmount();
        if (amt <= 0 || Double.isNaN(amt)) return;
        long now = System.currentTimeMillis();
        flushExpiredPending(now);
        HealAttribution.Split sp = HealAttribution.split(amt, target.getHealth(), maxHealth(target));
        String reason = e.getRegainReason().name();
        // A splash instant-health heal was already credited at PotionSplashEvent
        // (thrown potions apply their effect right after the splash event returns);
        // the MAGIC regain that follows is the same heal — never count it twice.
        if (reason.equals("MAGIC") && consumeSplashHit(target.getUniqueId(), now)) return;
        // Splash regeneration ticks attribute to the thrower while the splash lives.
        if (reason.equals("MAGIC_REGEN")) {
            SplashRegen sr = splashRegenFor(target.getUniqueId(), now);
            if (sr != null) {
                creditResolved(sr.throwerName(), sr.thrower(), target.getName(), target.getUniqueId(),
                        target, sp, "POTION", null, null, sr.castId());
                return;
            }
        }
        // A claim already on file explains this regain (heal-over-time ticks after
        // the first, and any cast whose CastEvent landed before its effect).
        HealAttribution.Claim c = matchClaim(target, reason, now);
        if (c != null) {
            creditResolved(c.casterName(), c.caster(), target.getName(), target.getUniqueId(),
                    target, sp, "SPELL", c.spell(), null, "c" + c.id());
            return;
        }
        // CastEvent is fired after the cast's effects, so a spell heal usually
        // arrives BEFORE its claim: hold attributable regains for the claim window
        // and let onCast resolve them. Unattributable reasons record immediately.
        if (magicHooked && (reason.equals("CUSTOM") || reason.equals("MAGIC_REGEN"))) {
            pending.add(new PendingHeal(target.getUniqueId(), target.getName(), target.getLocation().clone(),
                    amt, sp.effective(), sp.overheal(), reason, now));
            // A held heal is still fight activity: keep the target's fight open
            // until the claim resolves, or a short fight-gap closes the fight
            // first and the credit lands in a successor fight (observed on the
            // heal rig with a 5 s gap: HoT resolved 0.3 s after its fight closed).
            if (cfg.combatFights()) {
                Fight f = fightForHeal(target, null, target.getName(), null, target.getLocation());
                if (f != null) f.lastMs = now;
            }
            return;
        }
        genericHeal(target.getName(), target.getUniqueId(), target, sp, reason);
    }

    /**
     * Splash potions (㉚.10): attribute at the splash, per affected player.
     * Instant health credits immediately — raw amount = the whole-HP figure
     * vanilla applies for potion healing (4 × 2^amplifier) × the event's
     * intensity for that entity (see HealAttribution.splashInstantAmount),
     * split against the health missing at splash time (the effect has not applied yet at
     * MONITOR). Regeneration registers a thrower attribution that the later
     * MAGIC_REGEN regains consume tick by tick. Non-player throwers (witches,
     * dispensers) and non-player targets are outside the combat family.
     */
    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSplash(PotionSplashEvent e) {
        if (!cfg.combatHealing()) return;
        if (!(e.getPotion().getShooter() instanceof Player thrower)) return;
        long now = System.currentTimeMillis();
        // One cast id per throw: every entity this splash credits shares it (§S3).
        String splashCastId = "p" + (++castSeq);
        String potionId = null;
        try {
            if (e.getPotion().getItem() != null) potionId = e.getPotion().getItem().getType().getKey().toString();
        } catch (Throwable ignored) { }
        for (PotionEffect eff : e.getPotion().getEffects()) {
            boolean instant = eff.getType().equals(PotionEffectType.INSTANT_HEALTH);
            boolean regen = eff.getType().equals(PotionEffectType.REGENERATION);
            if (!instant && !regen) continue;
            for (LivingEntity affected : e.getAffectedEntities()) {
                if (!(affected instanceof Player target)) continue;
                double intensity = e.getIntensity(target);
                if (intensity <= 0) continue;
                if (instant) {
                    double raw = HealAttribution.splashInstantAmount(eff.getAmplifier(), intensity);
                    HealAttribution.Split sp = HealAttribution.split(raw, target.getHealth(), maxHealth(target));
                    creditResolved(thrower.getName(), thrower.getUniqueId(), target.getName(),
                            target.getUniqueId(), target, sp, "POTION", null, potionId, splashCastId);
                    splashHits.add(new SplashHit(target.getUniqueId(), now));
                } else {
                    long expiry = now + eff.getDuration() * 50L + 3_000L;
                    splashRegens.removeIf(s -> s.target().equals(target.getUniqueId()));
                    splashRegens.add(new SplashRegen(target.getUniqueId(), thrower.getUniqueId(),
                            thrower.getName(), expiry, splashCastId));
                }
            }
        }
    }

    private boolean consumeSplashHit(UUID target, long now) {
        Iterator<SplashHit> it = splashHits.iterator();
        boolean found = false;
        while (it.hasNext()) {
            SplashHit h = it.next();
            if (now - h.ms() > 2_500L) { it.remove(); continue; }
            if (!found && h.target().equals(target)) { it.remove(); found = true; }
        }
        return found;
    }

    private SplashRegen splashRegenFor(UUID target, long now) {
        SplashRegen found = null;
        Iterator<SplashRegen> it = splashRegens.iterator();
        while (it.hasNext()) {
            SplashRegen s = it.next();
            if (now > s.expiryMs()) { it.remove(); continue; }
            if (s.target().equals(target)) found = s;
        }
        return found;
    }

    /** Credit buffered regains whose cast claim has now arrived (bidirectional matching). */
    private void resolvePending() {
        if (pending.isEmpty()) return;
        long now = System.currentTimeMillis();
        Iterator<PendingHeal> it = pending.iterator();
        while (it.hasNext()) {
            PendingHeal p = it.next();
            Location l = p.loc();
            double r = cfg.combatHealProximityBlocks();
            HealAttribution.Claim c = HealAttribution.bestClaim(claims, p.target(), p.reason(),
                    l.getWorld() == null ? null : l.getWorld().getName(), l.getX(), l.getZ(), p.ms(),
                    cfg.combatHealClaimWindowMs(), r * r, cfg.magicHealSpells());
            if (c != null) {
                it.remove();
                creditResolved(c.casterName(), c.caster(), p.name(), p.target(), null,
                        new HealAttribution.Split(p.eff(), p.over()), "SPELL", c.spell(), null, "c" + c.id(), l);
            } else if (now - p.ms() > cfg.combatHealClaimWindowMs()) {
                it.remove();
                genericHeal(p.name(), p.target(), null,
                        new HealAttribution.Split(p.eff(), p.over()), p.reason(), l);
            }
        }
    }

    private void flushExpiredPending(long now) {
        // resolvePending also flushes expired entries as generic heals.
        if (!pending.isEmpty()) resolvePending();
    }

    /** An unattributed heal: target + amount + source reason (0.1.7's generic shape, plus the split). */
    private void genericHeal(String targetName, UUID targetUuid, Player target,
                             HealAttribution.Split sp, String reason) {
        genericHeal(targetName, targetUuid, target, sp, reason, target == null ? null : target.getLocation());
    }

    private void genericHeal(String targetName, UUID targetUuid, Player target,
                             HealAttribution.Split sp, String reason, Location loc) {
        Json.Obj o = new Json.Obj().put("type", "combat_heal")
                .put("target", targetName).put("target_uuid", targetUuid.toString())
                .put("amount", round2(sp.effective() + sp.overheal()))
                .put("effective", round2(sp.effective())).put("overheal", round2(sp.overheal()))
                .put("source", reason);
        ledger.emit("combat", loc, o);
        if (cfg.combatFights()) {
            Fight f = fightForHeal(target, null, targetName, null, loc);
            if (f != null) { f.lastMs = System.currentTimeMillis(); f.row(targetName)[3] += sp.effective(); }
        }
    }

    /** Credit a heal to a caster (Magic heal claims resolved by BossTrial-style pairing: healer casts, target regains). */
    public void creditHeal(Player healer, Player target, double amount, String spell) {
        if (!cfg.combatHealing()) return;
        HealAttribution.Split sp = HealAttribution.split(amount, target.getHealth(), maxHealth(target));
        creditResolved(healer == null ? null : healer.getName(), healer == null ? null : healer.getUniqueId(),
                target.getName(), target.getUniqueId(), target, sp, "SPELL", spell, null, null);
    }

    /**
     * The one credited-heal write: combat_heal with healer, spell/potion identity and
     * the effective/overheal split (amount keeps its 0.1.7 meaning = raw amount), and
     * the fight rows — healing_done for the healer, healing_received for the target,
     * EFFECTIVE amounts only, in the fight the target is actually in.
     */
    private void creditResolved(String healerName, UUID healerUuid, String targetName, UUID targetUuid,
                                Player target, HealAttribution.Split sp, String source, String spell,
                                String potionId, String castId) {
        creditResolved(healerName, healerUuid, targetName, targetUuid, target, sp, source, spell, potionId,
                castId, target == null ? null : target.getLocation());
    }

    private void creditResolved(String healerName, UUID healerUuid, String targetName, UUID targetUuid,
                                Player target, HealAttribution.Split sp, String source, String spell,
                                String potionId, String castId, Location loc) {
        if (!cfg.combatHealing()) return;
        Json.Obj o = new Json.Obj().put("type", "combat_heal")
                .put("target", targetName).put("target_uuid", targetUuid.toString())
                .put("amount", round2(sp.effective() + sp.overheal()))
                .put("effective", round2(sp.effective())).put("overheal", round2(sp.overheal()))
                .put("source", source);
        if (spell != null) o.put("spell", spell);
        if (potionId != null) o.put("potion", potionId);
        // cast_id (§S3): every tick of one cast shares it, so reports aggregate
        // per cast instead of per tick. SPELL credits carry the claim's id ("c…"),
        // splash credits the throw's id ("p…"); unattributed heals carry none.
        if (castId != null) o.put("cast_id", castId);
        if (healerName != null) o.put("healer", healerName);
        if (healerUuid != null) o.put("healer_uuid", healerUuid.toString());
        ledger.emit("combat", loc, o);
        if (cfg.combatFights()) {
            Entity healerEnt = healerUuid == null ? null : Bukkit.getPlayer(healerUuid);
            Fight f = fightForHeal(target, healerEnt, targetName, healerName, loc);
            if (f != null) {
                f.lastMs = System.currentTimeMillis();
                f.row(targetName)[3] += sp.effective();
                if (healerName != null) {
                    f.row(healerName)[2] += sp.effective();
                    // §S2: the per-spell split of that healing (effective only)
                    if ("SPELL".equals(source) && spell != null)
                        f.healSpells.computeIfAbsent(healerName, k -> new TreeMap<>())
                                .merge(spell, sp.effective(), Double::sum);
                }
            }
        }
    }

    private static double maxHealth(Player p) {
        try {
            var a = p.getAttribute(Attribute.MAX_HEALTH);
            if (a != null) return a.getValue();
        } catch (Throwable ignored) { }
        return 20.0;
    }

    /**
     * The fight a heal belongs to: an open fight that already holds the target (or
     * the healer) — boss/mob-anchored fights first — so healing lands in the SAME
     * per-player table as that fight's damage. 0.1.7 always opened the target's
     * area-cell fight, whose damage total stayed 0 and whose summary was then
     * dropped by fight-min-damage: credited healing could never surface in a
     * boss fight's table even when the pairing worked. Falls back to the
     * currentFight keying when no open fight knows either player.
     */
    private Fight fightForHeal(Entity target, Entity healer, String targetName, String healerName, Location loc) {
        long now = System.currentTimeMillis();
        long gap = cfg.fightGapSeconds() * 1000L;
        Fight best = null; boolean bestEnt = false, bestHasTarget = false;
        for (Map.Entry<String, Fight> en : fights.entrySet()) {
            Fight f = en.getValue();
            if (now - f.lastMs > gap) continue;
            boolean hasT = targetName != null && f.players.containsKey(targetName);
            boolean hasH = healerName != null && f.players.containsKey(healerName);
            if (!hasT && !hasH) continue;
            boolean ent = en.getKey().startsWith("ent:");
            if (best == null || (hasT && !bestHasTarget) || (hasT == bestHasTarget && ent && !bestEnt)) {
                best = f; bestEnt = ent; bestHasTarget = hasT;
            }
        }
        if (best != null) return best;
        if (target != null) return currentFight(target, healer);
        if (loc != null) return areaFight(loc, now);
        return null;
    }

    private Fight areaFight(Location l, long now) {
        String key = "area:" + (l.getWorld() == null ? "" : l.getWorld().getName())
                + ":" + (l.getBlockX() >> 5) + ":" + (l.getBlockZ() >> 5);
        Fight f = fights.get(key);
        if (f == null || now - f.lastMs > cfg.fightGapSeconds() * 1000L) {
            if (f != null) closeFight(f);
            f = new Fight("fight-" + (++fightSeq) + "-" + (now / 1000), now);
            fights.put(key, f);
        }
        return f;
    }

    private Fight currentFight(Entity a, Entity b) {
        long now = System.currentTimeMillis();
        String key = fightKey(a, b);
        Fight f = fights.get(key);
        if (f == null || now - f.lastMs > cfg.fightGapSeconds() * 1000L) {
            if (f != null) closeFight(f);
            f = new Fight("fight-" + (++fightSeq) + "-" + (now / 1000), now);
            fights.put(key, f);
        }
        return f;
    }

    private String fightKey(Entity a, Entity b) {
        // general fights group by the non-player party when there is one (a boss/mob), else by world area cell
        Entity anchor = !(a instanceof Player) ? a : (b != null && !(b instanceof Player)) ? b : a;
        if (anchor != null && !(anchor instanceof Player))
            return "ent:" + anchor.getUniqueId();
        org.bukkit.Location l = a.getLocation();
        return "area:" + l.getWorld().getName() + ":" + (l.getBlockX() >> 5) + ":" + (l.getBlockZ() >> 5);
    }

    private void closeFight(Fight f) {
        if (f.total < cfg.fightMinDamage()) return;
        Json.Arr rows = new Json.Arr();
        f.players.forEach((name, v) -> {
            Json.Obj row = new Json.Obj().put("player", name)
                    .put("damage_dealt", round2(v[0])).put("damage_taken", round2(v[1]))
                    .put("healing_done", round2(v[2])).put("healing_received", round2(v[3]));
            // §S2: optional per-spell split of healing_done (effective amounts)
            Map<String, Double> split = f.healSpells.get(name);
            if (split != null && !split.isEmpty()) {
                Json.Obj sp = new Json.Obj();
                split.forEach((k, val) -> sp.put(k, round2(val)));
                row.put("healing_spells", sp);
            }
            rows.add(row);
        });
        ledger.emit("combat", null, new Json.Obj().put("type", "fight_summary")
                .put("encounter", f.id).put("source", "general")
                .put("total_damage", round2(f.total)).put("players", rows));
    }

    /** Called by the flush timer: close fights whose gap window expired; flush held heals. */
    public void tick() {
        long now = System.currentTimeMillis();
        flushExpiredPending(now);
        resolvePendingDamage();
        pruneClaims(now);
        splashRegens.removeIf(s -> now > s.expiryMs());
        splashHits.removeIf(h -> now - h.ms() > 2_500L);
        if (!cfg.combatFights()) return;
        fights.values().removeIf(f -> {
            if (now - f.lastMs > cfg.fightGapSeconds() * 1000L) { closeFight(f); return true; }
            return false;
        });
        kills.prune(now - 10 * 60_000L);
    }

    static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
