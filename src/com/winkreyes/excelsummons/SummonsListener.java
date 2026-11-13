package com.winkreyes.excelsummons;

import com.elmakers.mine.bukkit.api.block.UndoList;
import com.elmakers.mine.bukkit.api.event.CastEvent;
import com.elmakers.mine.bukkit.api.event.PreCastEvent;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.elmakers.mine.bukkit.api.spell.SpellResult;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The bridge's ears and hands: capture registrations from Magic casts,
 * refuse over-cap casts before they happen, scrub targets and damage
 * under Mike's party rules (0.1.1: the owner's party is untouchable
 * and damage-proof; a known stranger who hits a summon becomes its
 * one retaliation target; unknown party status protects), scrub ghast
 * explosions so a summon can never remodel the village, and run the
 * target engine and lifetime sweep on their ticks.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class SummonsListener implements Listener {

    private static final long RECENT_WINDOW_MS = 15_000L;
    private static final long EXPIRED_RECORD_GRACE_MS = 30_000L;

    private final ExcelSummons plugin;
    private final Map<UUID, Map.Entry<UUID, Long>> recentTargetByPlayer = new HashMap<>();
    private final Map<UUID, Map.Entry<UUID, Long>> recentAttackerByPlayer = new HashMap<>();
    private final Map<UUID, Long> lastRefusalMsgAt = new HashMap<>();
    private final AggressorMemory aggressors;
    private final Map<UUID, String> baseNames = new HashMap<>();
    private final java.util.Set<String> warnedReserveKeys = new java.util.HashSet<>();

    public SummonsListener(ExcelSummons plugin) {
        this.plugin = plugin;
        this.aggressors = new AggressorMemory(plugin.aggressorMemoryMs());
    }

    private SummonRegistry registry() { return plugin.registry(); }

    /** Live party relation of a player to a summon's owner: party
     *  membership is re-read at every decision, so a player who joins
     *  the party mid-fight is protected from that moment on (and a
     *  stale aggressor entry can never override a fresh PARTY read). */
    private TargetRules.Relation relation(UUID ownerId, UUID playerId) {
        return TargetRules.relationFor(ownerId != null && ownerId.equals(playerId),
                plugin.partyProvider().membershipOf(ownerId, playerId));
    }

    /** The ONLY door through which a summon may hold a player target:
     *  the player is a known stranger AND this summon's recorded
     *  aggressor (Mike's retaliation rule, 0.1.1). */
    private boolean mayRetaliateAgainst(SummonRecord rec, Player target, long now) {
        return relation(rec.ownerId(), target.getUniqueId()) == TargetRules.Relation.STRANGER
                && aggressors.isAggressor(rec.entityId(), target.getUniqueId(), now);
    }

    // ------------------------------------------------------------ capture

    /**
     * Pre-cast gate: caps are enforced HERE, before mana is spent, so a
     * refused cast is a refusal and not a half-cast (DESIGN §6.1). The
     * mount's recast toggle also lives here: casting Mount while yours
     * is out dismisses it instead of summoning a second one.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPreCast(PreCastEvent event) {
        String key = event.getSpell() == null ? null : event.getSpell().getKey();
        if (!plugin.isManaged(key)) return;
        Mage mage = event.getMage();
        Player caster = mage == null ? null : mage.getPlayer();
        if (caster == null) return;
        CapPolicy.SpellRule rule = plugin.ruleFor(key);
        UUID owner = caster.getUniqueId();

        if (rule.branch == SummonRecord.Branch.MOUNT) {
            SummonRecord existing = firstOfBranch(owner, SummonRecord.Branch.MOUNT);
            if (existing != null) {
                event.setCancelled(true);
                if (despawn(existing, false)) {
                    caster.sendMessage("Your mount drifts home.");
                } else {
                    caster.sendMessage("Your mount cannot leave while it is being ridden.");
                }
                return;
            }
        }
        // The total cap is rank-scaled (§3 as amended): the
        // caster's earned rank comes from MagicGate's earned.yml
        // seam; unreadable fails to the lowest rung in CapPolicy.
        String rank = plugin.earnedRankOf(owner);
        CapPolicy.Refusal refusal = plugin.capPolicy()
                .check(registry(), owner, SummonRecord.baseKey(key), rule, rank);
        if (refusal != CapPolicy.Refusal.NONE) {
            event.setCancelled(true);
            long now = System.currentTimeMillis();
            Long last = lastRefusalMsgAt.get(owner);
            if (last == null || now - last > 1500L) {
                lastRefusalMsgAt.put(owner, now);
                // §4: the message names the actual reason — for the
                // total cap, the rank, its limit, and what is
                // currently summoned.
                caster.sendMessage(plugin.capPolicy().refusalMessage(refusal,
                        registry(), owner, SummonRecord.baseKey(key), rule, rank));
            }
            return;
        }
        // Mana-reserve gate (0.1.2, Mike's anti-spam ruling; widened
        // 0.1.4 §2.1c): a summon whose family holds reserve holds a
        // percentage of the caster's pool while it lives
        // (ReservePolicy). The gate keys on the family's configured
        // percent > 0 — NOT on the combat branch — so companions
        // and the mount are gated by the same ceiling/floor law.
        // Refused HERE — before mana is spent — when the summons'
        // own total would pass the ceiling, or when the mage's live
        // usable fraction (toggles included) would fall below the
        // floor. Same refusal shape as the caps.
        int spellPct = ReservePolicy.percentFor(plugin.reservePercents(),
                SummonRecord.baseKey(key));
        if (spellPct > 0) {
            // 0.1.6: "already held" is the APPLIED total — adopted
            // records hold nothing (Mike's ruling) and the sync
            // clamp caps cast holds at the ceiling/floor — so the
            // gate's number is the number the mage actually
            // carries, never the raw registry sum.
            int currentPct = ReservePolicy.appliedPercent(registry(), owner,
                    plugin.reservePercents(), plugin.reserveCeilingPct(),
                    plugin.reserveFloorPct());
            ReservePolicy.Refusal r = ReservePolicy.check(currentPct, spellPct,
                    plugin.reserveCeilingPct(), mage.getEffectiveManaMax(),
                    mage.getManaMax(), plugin.reserveFloorPct());
            if (r != ReservePolicy.Refusal.NONE) {
                event.setCancelled(true);
                long now = System.currentTimeMillis();
                Long last = lastRefusalMsgAt.get(owner);
                if (last == null || now - last > 1500L) {
                    lastRefusalMsgAt.put(owner, now);
                    caster.sendMessage(ReservePolicy.refusalMessage(r,
                            SummonRecord.baseKey(key), spellPct, currentPct,
                            plugin.reserveCeilingPct(), plugin.reserveFloorPct()));
                }
            }
        }
    }

    /**
     * Capture: on a completed cast of a managed summon spell, the
     * mage's last undo list names exactly the entities that cast made
     * (Magic tracks them for its own undo machinery). Those UUIDs are
     * registered to the caster — deterministic, no radius heuristics
     * (DESIGN §4.3). Capture-time cap enforcement is the backstop for
     * casts that never passed PreCast.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCast(CastEvent event) {
        String key = event.getSpell() == null ? null : event.getSpell().getKey();
        if (!plugin.isManaged(key)) return;
        if (event.getSpellResult() == SpellResult.FAIL) return;
        Mage mage = event.getMage();
        Player caster = mage == null ? null : mage.getPlayer();
        if (caster == null) return;
        UndoList undo = mage.getLastUndoList();
        if (undo == null) return;
        CapPolicy.SpellRule rule = plugin.ruleFor(key);
        String base = SummonRecord.baseKey(key);
        UUID owner = caster.getUniqueId();
        boolean changed = false;
        for (Entity e : undo.getAllEntities()) {
            if (e == null || e.getUniqueId().equals(owner)) continue;
            if (registry().isRegistered(e.getUniqueId())) continue;
            if (!(e instanceof LivingEntity)) continue;
            String rank = plugin.earnedRankOf(owner);
            CapPolicy.Refusal refusal = plugin.capPolicy().check(registry(), owner, base, rule, rank);
            if (refusal != CapPolicy.Refusal.NONE) {
                e.remove(); // backstop: the excess summon never takes the field
                caster.sendMessage(plugin.capPolicy().refusalMessage(refusal,
                        registry(), owner, base, rule, rank));
                continue;
            }
            long now = System.currentTimeMillis();
            // §2.4: a cast-captured summon is CAST-origin — the
            // only kind `dismiss pets` may ever remove.
            SummonRecord rec = new SummonRecord(
                    e.getUniqueId(), owner, base, rule.branch,
                    LifetimePolicy.expiresAt(now, rule.durationMillis),
                    e.getWorld().getName(), now, SummonRecord.Origin.CAST);
            registry().register(rec);
            if ("HAPPY_GHAST".equals(e.getType().name())) applyHarness(e);
            if (ArcherEquipment.guarantees(key)) applyArcherEquipment(e, key);
            refreshNameplate(rec); // health plate from the first moment
            changed = true;
        }
        if (changed) {
            plugin.saveRegistry();
            syncReserve(owner); // a new summon starts holding its reserve now
        }
    }

    /** The happy ghast's harness rides the vanilla body slot. The
     *  material is resolved BY NAME: the house compile surface (the
     *  shared legacy paper-api) predates the happy ghast, so the
     *  RED_HARNESS constant does not exist at compile time — on the
     *  live 26.3 server the name resolves and the harness applies.
     *  Any failure is soft: the mount is simply unharnessed until the
     *  rig pass settles the slot (DESIGN §5.4's verification item). */
    private void applyHarness(Entity ghast) {
        try {
            if (ghast instanceof LivingEntity) {
                org.bukkit.inventory.EntityEquipment eq =
                        ((LivingEntity) ghast).getEquipment();
                if (eq != null) {
                    eq.setItem(EquipmentSlot.BODY,
                            new ItemStack(Material.valueOf("RED_HARNESS")));
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().info("ExcelSummons: harness application skipped ("
                    + t.getClass().getSimpleName() + "); mount ships unharnessed.");
        }
    }

    /** The archer equipment guarantee (0.1.5): the spells specify
     *  the archer's bow + tier helmet at every tier, but Magic's
     *  SpawnEntity layer does not apply item_in_hand on spawn (the
     *  archer arrived bowless — diagnosis of record,
     *  archer-bow-diagnosis-20261110.md), so the bridge sets the
     *  gear itself when it registers a CAST archer, in the same
     *  soft-fail shape as applyHarness: any failure leaves the
     *  registration and the cast untouched, with one INFO line.
     *  The spec (family, tier, material names) is ArcherEquipment's;
     *  the TIER comes from the cast-time spell key in hand here
     *  (archer|2), because the registry record folds tiers into the
     *  family base. Materials resolve BY NAME, like the harness:
     *  BOW / IRON_HELMET / CHAINMAIL_HELMET are ancient names, and
     *  the by-name shape keeps the guarantee as fail-soft as the
     *  harness path it mirrors. After setting, the gear is READ
     *  BACK and verified against the spec (ArcherEquipment.matches);
     *  a mismatch logs the same soft note — verification never
     *  fails the cast either. Public so the desk rig exercises this
     *  exact method on real spawned entities (the bridge path),
     *  not a copy of it. The guard lives inside: a non-archer key
     *  or a non-living entity is a clean no-op. */
    public void applyArcherEquipment(Entity entity, String spellKey) {
        try {
            if (!ArcherEquipment.guarantees(spellKey)) return;
            if (!(entity instanceof LivingEntity)) return;
            org.bukkit.inventory.EntityEquipment eq =
                    ((LivingEntity) entity).getEquipment();
            if (eq == null) return;
            eq.setItem(EquipmentSlot.HAND,
                    new ItemStack(Material.valueOf(
                            ArcherEquipment.handMaterialName(spellKey))));
            eq.setItem(EquipmentSlot.HEAD,
                    new ItemStack(Material.valueOf(
                            ArcherEquipment.helmetMaterialName(spellKey))));
            ItemStack hand = eq.getItem(EquipmentSlot.HAND);
            ItemStack head = eq.getItem(EquipmentSlot.HEAD);
            boolean verified = ArcherEquipment.matches(
                    hand == null ? null : hand.getType().name(),
                    head == null ? null : head.getType().name(),
                    spellKey);
            if (!verified) {
                plugin.getLogger().info("ExcelSummons: archer equipment for "
                        + spellKey + " did not verify on read-back; "
                        + "the summon fights on regardless.");
            }
        } catch (Throwable t) {
            plugin.getLogger().info("ExcelSummons: archer equipment skipped ("
                    + t.getClass().getSimpleName()
                    + "); the summon fights on regardless.");
        }
    }

    // ------------------------------------------------------------ scrubs

    /** A registered summon may hold a player as a target through
     *  exactly one door: retaliation against its recorded aggressor
     *  (a known non-party player who hit it). Every other player
     *  target — owner, party, bystander, unknown-status — is
     *  cancelled and cleared (DESIGN §3 as revised in 0.1.1). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onTarget(EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player)) return;
        SummonRecord rec = registry().get(event.getEntity().getUniqueId());
        if (rec == null) return;
        if (mayRetaliateAgainst(rec, (Player) event.getTarget(),
                System.currentTimeMillis())) {
            return; // retaliation against the recorded aggressor stands
        }
        event.setCancelled(true);
        event.setTarget(null);
    }

    /**
     * The damage rules + the target engine's memory, in one listener
     * (0.1.1 party rules):
     * - A player hitting a registered summon: owner / party / unknown
     *   party status — the hit is CANCELLED outright and nothing is
     *   recorded (the summon takes no damage and can never aggro on
     *   them). A known stranger — the hit lands and the stranger
     *   becomes that summon's recorded aggressor.
     * - A registered summon hitting out: its damage lands on a player
     *   ONLY when that player is its recorded aggressor (and still a
     *   known stranger); on passive animals and owned pets it never
     *   lands; hostile mobs are fair game.
     * - Damage BY players and TO players is also remembered so the
     *   target engine knows who is fighting what (mob fights only —
     *   player targets come from the aggressor memory, never here).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity source = ExcelSummonsAPI.unwrapSource(event.getDamager());
        Entity victim = event.getEntity();
        long now = System.currentTimeMillis();

        SummonRecord victimRec = registry().get(victim.getUniqueId());
        // 0.1.2 Guardian tank skin: a guardian record takes reduced
        // damage from EVERY source (TauntRules.guardianDamageTaken) —
        // applied before the scrubs so a protected player's cancelled
        // hit stays cancelled and everything else lands softer.
        if (victimRec != null && "guardian".equals(victimRec.spellKey())
                && !event.isCancelled()) {
            event.setDamage(TauntRules.guardianDamageTaken(event.getDamage(),
                    plugin.guardianDamageReduction()));
        }
        if (victimRec != null && source instanceof Player) {
            Player hitter = (Player) source;
            if (TargetRules.incomingPlayerHit(relation(victimRec.ownerId(),
                    hitter.getUniqueId())) == TargetRules.IncomingHit.CANCEL_PROTECTED) {
                event.setCancelled(true);
                return; // protected hit: no damage, no memory, no aggro
            }
            aggressors.record(victim.getUniqueId(), hitter.getUniqueId(), now);
        }

        if (source != null && registry().isRegistered(source.getUniqueId())) {
            SummonRecord srcRec = registry().get(source.getUniqueId());
            if (victim instanceof Player) {
                Player vp = (Player) victim;
                boolean allowed = srcRec != null && TargetRules.playerDamageAllowed(
                        relation(srcRec.ownerId(), vp.getUniqueId()),
                        aggressors.isAggressor(source.getUniqueId(),
                                vp.getUniqueId(), now));
                if (!allowed) {
                    event.setCancelled(true);
                    return;
                }
            } else {
                boolean victimOwnedPet = victim instanceof Tameable
                        && ((Tameable) victim).isTamed()
                        && ((Tameable) victim).getOwner() != null;
                boolean victimPassive = victim instanceof Animals && !(victim instanceof Enemy);
                if (!TargetRules.damageAllowed(false, victimPassive, victimOwnedPet)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        if (source instanceof Player && victim instanceof Enemy) {
            recentTargetByPlayer.put(source.getUniqueId(),
                    Map.entry(victim.getUniqueId(), now));
        }
        if (victim instanceof Player && source instanceof Enemy) {
            recentAttackerByPlayer.put(victim.getUniqueId(),
                    Map.entry(source.getUniqueId(), now));
        }
    }

    /** The tank skin also covers non-entity damage (fire, falls,
     *  contact): EntityDamageByEntityEvent is handled in onDamage
     *  above and skipped here so no hit is ever reduced twice. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGuardianAmbientDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) return;
        SummonRecord rec = registry().get(event.getEntity().getUniqueId());
        if (rec != null && "guardian".equals(rec.spellKey())) {
            event.setDamage(TauntRules.guardianDamageTaken(event.getDamage(),
                    plugin.guardianDamageReduction()));
        }
    }

    // ------------------------------------------------------------ nameplate

    /**
     * Health visibility (0.1.1, Mike: "make sure that we can see how
     * much health your mob has"): every registered summon wears its
     * health on its nameplate — "Archer — 18/20" (Nameplate holds
     * the format). Refreshed at registration, on both bridge ticks,
     * and one tick after any damage or regain event involving the
     * summon (scheduled: damage and regains APPLY after their event
     * returns, so an in-event read would show the old number).
     * Composition, never clobber: the base is the name the entity
     * already carries — Magic's configured name if a spell sets one
     * (none of the tree's spells do today; CastEvent fires after
     * the spell performs, so a configured name would already be in
     * place at capture), a player's name tag on a pet — with our
     * own previous suffix stripped; unnamed summons fall back to
     * their spell's display name. A rename by anyone is adopted as
     * the new base at the next refresh. NOT a boss bar — the bar
     * is the boss (Mike's law); /summons list carries the same
     * numbers in text for anyone who wants them exact.
     */
    public void refreshNameplate(SummonRecord rec) {
        Entity e = Bukkit.getEntity(rec.entityId());
        if (!(e instanceof LivingEntity)) return;
        LivingEntity le = (LivingEntity) e;
        String current = le.getCustomName();
        String base;
        if (current != null) {
            base = Nameplate.baseName(current, rec.spellKey());
            baseNames.put(rec.entityId(), base);
        } else {
            base = baseNames.getOrDefault(rec.entityId(),
                    Nameplate.spellDisplayName(rec.spellKey()));
        }
        String rendered = Nameplate.display(base, le.getHealth(), maxHealth(le));
        if (!rendered.equals(current)) {
            le.setCustomName(rendered);
            le.setCustomNameVisible(true);
        }
    }

    static double maxHealth(LivingEntity le) {
        org.bukkit.attribute.AttributeInstance attr =
                le.getAttribute(Attribute.MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onSummonDamaged(EntityDamageEvent event) {
        scheduleNameRefresh(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onSummonRegained(EntityRegainHealthEvent event) {
        scheduleNameRefresh(event.getEntity().getUniqueId());
    }

    private void scheduleNameRefresh(UUID entityId) {
        if (!registry().isRegistered(entityId)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            SummonRecord rec = registry().get(entityId);
            if (rec != null) refreshNameplate(rec);
        });
    }

    /** Ghast block safety: when a registered summon's fireball goes
     *  off, the explosion keeps its teeth (entity damage) and loses
     *  its landscaping (the block list is emptied) — DESIGN §5.2. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onExplode(EntityExplodeEvent event) {
        Entity boom = event.getEntity();
        if (boom instanceof Projectile) {
            Entity source = ExcelSummonsAPI.unwrapSource(boom);
            if (source != null && registry().isRegistered(source.getUniqueId())) {
                event.blockList().clear();
            }
        }
    }

    // ------------------------------------------------------------ lifetime

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        UUID dead = event.getEntity().getUniqueId();
        SummonRecord gone = registry().unregister(dead);
        if (gone != null) {
            aggressors.clear(dead); // a dead summon holds no grudge
            baseNames.remove(dead);
            plugin.saveRegistry();
            syncReserve(gone.ownerId()); // a dead summon's reserve is released
        }
        aggressors.clearAggressor(dead); // a dead aggressor is nobody's target
    }

    /** Reserve re-sync on join (§2.3): combat summons despawn on
     *  logout (a release path), but companions and the mount
     *  PERSIST across relog — for them join is a RE-APPLY path:
     *  their CAST holds are re-derived from the persisted registry
     *  and put back on the mage. 0.1.6 amends the §3.3 note: the
     *  SUMMONS are still never trimmed, but their HOLDS are
     *  clamped at the ceiling + floor and ADOPTED records hold
     *  nothing (Mike's ruling, Nov 12, 2026) — an over-ceiling
     *  grandfathered cast set keeps every animal and holds only
     *  up to the cap, and an adopted menagerie's stale modifiers
     *  clear at this sync. The sync also sweeps any modifier
     *  state that outlived its summons. Delayed a few seconds so
     *  Magic has loaded the mage. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> syncReserve(id), 60L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        aggressors.clearAggressor(event.getPlayer().getUniqueId());
        despawnCasterCombat(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        despawnCasterCombat(event.getEntity().getUniqueId());
    }

    /** A combat summon that outlived its duration inside an unloaded
     *  chunk dies on return, not whenever someone notices (DESIGN §6.2). */
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (Entity e : event.getEntities()) {
            SummonRecord rec = registry().get(e.getUniqueId());
            if (rec != null && rec.branch() == SummonRecord.Branch.COMBAT
                    && rec.expiryDue(now)) {
                e.remove();
                registry().unregister(rec.entityId());
                aggressors.clear(rec.entityId());
                baseNames.remove(rec.entityId());
                syncReserve(rec.ownerId()); // chunk re-check release path
                changed = true;
            }
        }
        if (changed) plugin.saveRegistry();
    }

    private void despawnCasterCombat(UUID owner) {
        boolean changed = false;
        for (SummonRecord rec : registry().ofOwner(owner)) {
            if (LifetimePolicy.leavesWithCaster(rec)) {
                despawn(rec, true);
                changed = true;
            }
        }
        if (changed) plugin.saveRegistry();
    }

    /** Remove a summon's entity + record. Returns false when the
     *  lifetime policy defers (ridden). {@code force} skips the expiry
     *  question but never the ridden rule. */
    public boolean despawn(SummonRecord rec, boolean force) {
        Entity e = Bukkit.getEntity(rec.entityId());
        boolean ridden = e != null && !e.getPassengers().isEmpty();
        if (!force) {
            LifetimePolicy.Decision d = LifetimePolicy.decide(
                    rec, System.currentTimeMillis(), ridden, false);
            if (d == LifetimePolicy.Decision.DEFER_RIDDEN) return false;
            if (d == LifetimePolicy.Decision.KEEP) return false;
        } else if (ridden) {
            return false; // the ridden rule has no force override
        }
        if (e != null) {
            e.getWorld().spawnParticle(Particle.POOF, e.getLocation().add(0, 0.5, 0), 12);
            e.remove();
        }
        registry().unregister(rec.entityId());
        aggressors.clear(rec.entityId());
        baseNames.remove(rec.entityId());
        plugin.saveRegistry();
        syncReserve(rec.ownerId()); // expiry / dismissal / logout / death funnel
        return true;
    }

    /** Dismiss path (command + mount toggle + console admin), by
     *  scope (0.1.4 §2.4): MOUNT_ONLY takes the mount; ALL takes
     *  mount + combat; PETS takes CAST-origin companion + pack
     *  pets only — the player-controlled release for companion
     *  reserve. Adopted pets are never selected by any scope. The
     *  target selection is AdminPolicy's — the same pure selection
     *  the harness tests exercise — so the self dismiss and the
     *  admin dismiss can never drift apart. */
    public int dismiss(UUID owner, AdminPolicy.DismissScope scope) {
        int n = 0;
        for (SummonRecord rec : AdminPolicy.dismissTargets(registry().ofOwner(owner), scope)) {
            if (despawn(rec, true)) n++;
        }
        return n;
    }

    /** The 0.1.3 boolean form — delegates to the scope form. */
    public int dismiss(UUID owner, boolean all) {
        return dismiss(owner, all ? AdminPolicy.DismissScope.ALL
                : AdminPolicy.DismissScope.MOUNT_ONLY);
    }

    /** 0.1.7 adopted single-record dismiss (Mike's ruling, Nov 12,
     *  2026, ~6:05 PM): the Summons menu's confirm-gated flow is
     *  the ONLY door to adopted dismissal — the player confirms
     *  one named pet in the menu, and this path takes exactly that
     *  one record. Eligibility is AdoptedDismissPolicy's: the
     *  record must exist, belong to the owner, and be ADOPTED
     *  origin; anything else is a clean no-op (CAST records keep
     *  their scopes — this path refuses them). The command scopes
     *  never call this. A successful dismissal writes the INFO
     *  audit line (player + pet spell/type key). */
    public boolean dismissOneAdopted(UUID owner, UUID entityId, String actorName) {
        SummonRecord rec = registry().get(entityId);
        if (!AdoptedDismissPolicy.eligible(rec, owner)) return false;
        if (!despawn(rec, true)) return false;
        plugin.getLogger().info(AdoptedDismissPolicy.auditLine(actorName, owner, rec));
        return true;
    }

    private SummonRecord firstOfBranch(UUID owner, SummonRecord.Branch branch) {
        for (SummonRecord r : registry().ofOwner(owner)) {
            if (r.branch() == branch) return r;
        }
        return null;
    }

    // ------------------------------------------------- 0.1.2: taunt + reserve

    /**
     * Guardian taunt (0.1.2, Mike's tank ruling): on the target tick,
     * each living Guardian pulls party-engaged hostiles off their
     * players. The facts are gathered here; the decision is
     * TauntRules' — a hostile qualifies only when it is a living Mob
     * in the Guardian's world, inside the taunt radius, and its
     * CURRENT target is the Guardian's owner or a member of the
     * owner's party (that target IS the engagement evidence: taunt
     * steals fights, it never starts them, never pulls an unengaged
     * pack, never touches a hostile chasing a non-party player, and
     * never points anything at a player — the Guardian itself is the
     * only target a taunt ever sets). Governed bosses get no
     * special case: BossTrial manages damage intake, never boss
     * targets, so boss attention is vanilla AI and the same
     * re-point applies (effectiveness per boss class is analyzed in
     * the build record).
     */
    private void tauntTick() {
        double radius = plugin.tauntRadius();
        for (SummonRecord rec : registry().all()) {
            if (rec.branch() != SummonRecord.Branch.COMBAT) continue;
            if (!"guardian".equals(rec.spellKey())) continue;
            Entity e = Bukkit.getEntity(rec.entityId());
            if (!(e instanceof Mob) || e.isDead()) continue;
            Mob guardian = (Mob) e;
            // Party truth for taunt is Together's membership itself,
            // not the target-sharing toggle (that toggle governs
            // whether summons JOIN party fights, not who is party).
            java.util.Set<UUID> party =
                    plugin.partyProvider().partyMembersOf(rec.ownerId());
            for (Entity near : guardian.getNearbyEntities(radius, radius, radius)) {
                if (!(near instanceof Mob) || !(near instanceof Enemy)) continue;
                Mob hostile = (Mob) near;
                if (hostile.isDead()) continue;
                LivingEntity current = hostile.getTarget();
                boolean targetIsPlayer = current instanceof Player;
                boolean ownerOrParty = targetIsPlayer
                        && (current.getUniqueId().equals(rec.ownerId())
                            || party.contains(current.getUniqueId()));
                boolean ok = TauntRules.shouldTaunt(true, true, true,
                        guardian.getLocation().distance(hostile.getLocation()), radius,
                        targetIsPlayer, ownerOrParty,
                        current != null && current.getUniqueId().equals(rec.entityId()));
                if (ok) hostile.setTarget(guardian);
            }
        }
    }

    /** The caster's Magic mage, or null. MagicPlugin implements the
     *  MagicAPI interface, and plugin.yml declares depend: [Magic],
     *  so the cast is safe whenever this plugin is enabled. */
    private Mage mageOf(Player player) {
        try {
            org.bukkit.plugin.Plugin magic =
                    Bukkit.getPluginManager().getPlugin("Magic");
            if (magic instanceof MagicAPI) {
                return ((MagicAPI) magic).getMage(player);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("ExcelSummons: Magic API lookup failed: "
                    + t.getMessage());
        }
        return null;
    }

    /**
     * Mana-reserve upkeep (0.1.2): re-derive one caster's held
     * reserve from the registry and diff it onto their mage as Magic
     * modifiers (ReservePolicy). Desired keys are added when absent;
     * stale keys (a summon left, a count changed) are removed. This
     * runs after every registry mutation that can change the answer
     * — cast capture, despawn, dismissal, summon death, expiry,
     * chunk re-check — plus a sweep-tick drift pass and a join
     * re-sync, so the hold tracks the registry no matter which path
     * a summon took out. Fail-soft throughout: if Magic or the
     * template is missing, the summons still work; a missing
     * template logs ONE warning per key per boot (the modifiers
     * addition in the summons packet is an install item — see the
     * build record's ship gates). 0.1.6: the desired state itself
     * is clamped + adopted-exempt in ReservePolicy, so this diff
     * is also the enforcement — a stale over-ceiling or adopted
     * modifier is removed here even when nothing was cast.
     */
    public void syncReserve(UUID owner) {
        Player player = Bukkit.getPlayer(owner);
        if (player == null || !player.isOnline()) return;
        Mage mage = mageOf(player);
        if (mage == null) return;
        try {
            // 0.1.6: the re-derivation is clamped at the live
            // ceiling + floor and excludes adopted records
            // (ReservePolicy) — the same law PreCast enforces, so
            // a login/sync can never re-apply an over-ceiling sum
            // (Jester's 85% menagerie, Nov 12, 2026) and an adopted
            // pet's stale modifier is swept away by this diff.
            Map<String, Integer> desired = ReservePolicy.desiredModifiers(
                    registry(), owner, plugin.reservePercents(),
                    plugin.reserveCeilingPct(), plugin.reserveFloorPct());
            for (String key : ReservePolicy.allKeys(plugin.reservePercents(),
                    plugin.reserveFamilyCaps())) {
                boolean want = desired.containsKey(key);
                boolean have = mage.hasModifier(key);
                if (want && !have) {
                    if (!mage.addModifier(key) && warnedReserveKeys.add(key)) {
                        plugin.getLogger().warning("ExcelSummons: reserve modifier '"
                                + key + "' could not be applied — is the summons "
                                + "packet's modifiers addition installed in Magic's "
                                + "modifiers.yml?");
                    }
                } else if (!want && have) {
                    mage.removeModifier(key);
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("ExcelSummons: reserve sync failed for "
                    + player.getName() + ": " + t.getMessage());
        }
    }

    // ------------------------------------------------------------ ticks

    /**
     * Lifetime sweep (every sweep-ticks): enforce expiries that are
     * due, adopt unregistered player-owned wolves and cats found in
     * the loaded world (the tameable capture path — it catches casts
     * whose undo list did not name the pet, pre-bridge pets, and
     * hand-tamed wolves alike), and retire records of combat summons
     * whose entities vanished past expiry (e.g. removed by Magic's own
     * undo, which fires no death event).
     *
     * 0.1.4 §3.4 NOTE — registry reconciliation is deliberately NOT
     * wired here: SummonRegistry.sweepMissing stays uncalled from
     * this path. A record carries a home WORLD but no position, and
     * Bukkit.getEntity(...) == null is also the unloaded-chunk
     * answer, so no test available to this packet can prove
     * "chunk loaded, entity absent" for a persistent pet — wiring
     * the sweep on that evidence would unregister live pets in
     * unloaded chunks and desync their reserve. Per the design's
     * own gate (§3.4/§5.2), the stale-record behavior (cosmetic in
     * /summons list) is kept rather than risk live pets. See the
     * 0.1.4 build record.
     */
    public void sweepTick() {
        long now = System.currentTimeMillis();
        boolean changed = false;

        // Nameplates for every branch (the target tick only walks
        // combat summons, and only while their caster is online);
        // the base-name cache is pruned to live registrations here.
        baseNames.keySet().removeIf(id -> !registry().isRegistered(id));
        for (SummonRecord rec : registry().all()) {
            if (Bukkit.getEntity(rec.entityId()) instanceof LivingEntity) {
                refreshNameplate(rec);
            }
        }

        for (SummonRecord rec : registry().expiryDue(now)) {
            Entity e = Bukkit.getEntity(rec.entityId());
            if (e == null) {
                if (now > (rec.expiresAtMillis() == null ? now : rec.expiresAtMillis())
                        + EXPIRED_RECORD_GRACE_MS) {
                    registry().unregister(rec.entityId());
                    aggressors.clear(rec.entityId());
                baseNames.remove(rec.entityId());
                    syncReserve(rec.ownerId());
                    changed = true;
                }
                continue; // in an unloaded chunk: EntitiesLoadEvent owns it
            }
            boolean ridden = !e.getPassengers().isEmpty();
            if (LifetimePolicy.decide(rec, now, ridden, false)
                    == LifetimePolicy.Decision.REMOVE) {
                e.getWorld().spawnParticle(Particle.POOF, e.getLocation().add(0, 0.5, 0), 12);
                e.remove();
                registry().unregister(rec.entityId());
                aggressors.clear(rec.entityId());
                baseNames.remove(rec.entityId());
                syncReserve(rec.ownerId());
                changed = true;
            }
        }

        // Reserve drift repair (0.1.2; widened 0.1.4 §2.1d):
        // whatever path a summon left by — including Magic's own
        // undo, which unregisters nothing here and is only noticed
        // by the expiry sweep above — the reserve is re-derived
        // from the registry for every owner still fielding a
        // reserve-bearing summon (any branch with percent > 0),
        // every sweep. 0.1.6: owners whose records are ALL adopted
        // stay in this set on purpose — their desired state is
        // empty, so the sweep is what clears any stale adopted
        // modifiers a pre-0.1.6 sync left on the mage.
        java.util.Set<UUID> reserveOwners = new java.util.HashSet<>();
        for (SummonRecord rec : registry().all()) {
            if (ReservePolicy.percentFor(plugin.reservePercents(), rec.spellKey()) > 0) {
                reserveOwners.add(rec.ownerId());
            }
        }
        for (UUID ownerId : reserveOwners) syncReserve(ownerId);

        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (LivingEntity le : world.getLivingEntities()) {
                if (registry().isRegistered(le.getUniqueId())) continue;
                if (!(le instanceof Tameable)) continue;
                Tameable t = (Tameable) le;
                if (!t.isTamed() || t.getOwner() == null) continue;
                UUID ownerId = t.getOwner().getUniqueId();
                if (ownerId == null || Bukkit.getOfflinePlayer(ownerId).getName() == null) {
                    continue; // not a player's pet (or not provably one)
                }
                SummonRecord.Branch branch;
                String spell;
                if (le.getType() == EntityType.WOLF) {
                    branch = SummonRecord.Branch.PACK;
                    spell = "wolf";
                } else if (le.getType() == EntityType.CAT) {
                    branch = SummonRecord.Branch.COMPANION;
                    spell = "cat";
                } else {
                    continue; // horses and parrots are not tree summons
                }
                // §2.4: a sweep-registered pet is ADOPTED-origin —
                // it counts toward the §3 cap (one registry, one
                // truth), but `dismiss pets` can never remove it:
                // it is the player's own hand-tamed animal, not a
                // conjured spell pet. 0.1.6 (Mike's ruling,
                // Nov 12, 2026): an adopted pet holds NO reserve —
                // ReservePolicy excludes its origin on every path.
                SummonRecord petRec = new SummonRecord(le.getUniqueId(), ownerId, spell,
                        branch, null, world.getName(), now, SummonRecord.Origin.ADOPTED);
                registry().register(petRec);
                refreshNameplate(petRec); // adopted pets get their plate too
                syncReserve(ownerId); // re-derive (an adopted pet itself holds nothing)
                changed = true;
            }
        }
        if (changed) plugin.saveRegistry();
    }

    /**
     * Target engine (every target-ticks, ~2 s by default): point each
     * combat summon at its first engageable fight — its recorded
     * aggressor first (0.1.1 retaliation, validated live every tick:
     * still a known stranger, alive, online, in this world and inside
     * the engage radius, else the memory is dropped), then the mob
     * priorities (the caster's attacker, the caster's own target, a
     * party member's fight) — or clear its target and leash it home.
     * Player targets come ONLY from the aggressor memory: the
     * recent-fight maps below feed mob candidates, and a player id in
     * one of them can never be chosen (TargetRules.choose requires a
     * hostile non-player). The rules are TargetRules'; this method
     * only gathers facts.
     */
    public void targetTick() {
        tauntTick(); // 0.1.2: Guardians pull party-engaged hostiles first
        long now = System.currentTimeMillis();
        for (SummonRecord rec : registry().all()) {
            if (rec.branch() != SummonRecord.Branch.COMBAT) continue;
            Entity e = Bukkit.getEntity(rec.entityId());
            if (!(e instanceof Mob) || e.isDead()) continue;
            Mob mob = (Mob) e;
            Player caster = Bukkit.getPlayer(rec.ownerId());
            if (caster == null || !caster.isOnline()) continue;
            refreshNameplate(rec);

            TargetRules.Candidate aggressor = aggressorCandidate(rec, mob, now);
            TargetRules.Candidate attacker = candidate(
                    recent(recentAttackerByPlayer, rec.ownerId(), now), mob);
            TargetRules.Candidate target = candidate(
                    recent(recentTargetByPlayer, rec.ownerId(), now), mob);
            TargetRules.Candidate party = null;
            if (plugin.partySharing()) {
                for (UUID member : plugin.partyProvider().partyMembersOf(rec.ownerId())) {
                    UUID fight = recent(recentTargetByPlayer, member, now);
                    if (fight == null) fight = recent(recentAttackerByPlayer, member, now);
                    if (fight != null) {
                        party = candidate(fight, mob);
                        if (party != null) break;
                    }
                }
            }
            UUID chosen = TargetRules.chooseWithRetaliation(
                    aggressor, attacker, target, party, plugin.engageRadius());
            if (chosen != null) {
                Entity chosenEntity = Bukkit.getEntity(chosen);
                if (chosenEntity instanceof LivingEntity) {
                    LivingEntity current = mob.getTarget();
                    if (current == null || !current.getUniqueId().equals(chosen)) {
                        mob.setTarget((LivingEntity) chosenEntity);
                    }
                }
            } else {
                if (mob.getTarget() != null) mob.setTarget(null);
                double dist = sameWorld(mob, caster)
                        ? mob.getLocation().distance(caster.getLocation()) : 0;
                if (TargetRules.shouldLeashHome(dist, plugin.leashDistance(), false)) {
                    mob.teleport(caster.getLocation());
                }
            }
        }
    }

    /** The summon's retaliation candidate for this tick, or null.
     *  The memory entry is validated against the live world and
     *  DROPPED when it no longer holds: aggressor offline or dead,
     *  no longer a known stranger (joined the party / status now
     *  unknown — protection wins), in another world, or beyond the
     *  engage radius (the memory's leash-range clear). The clock half
     *  of the lifetime lives in AggressorMemory itself. */
    private TargetRules.Candidate aggressorCandidate(SummonRecord rec, Mob mob, long now) {
        UUID aggId = aggressors.aggressorOf(rec.entityId(), now);
        if (aggId == null) return null;
        Player agg = Bukkit.getPlayer(aggId);
        boolean valid = agg != null && agg.isOnline() && !agg.isDead()
                && relation(rec.ownerId(), aggId) == TargetRules.Relation.STRANGER
                && agg.getWorld().equals(mob.getWorld())
                && agg.getLocation().distance(mob.getLocation()) <= plugin.engageRadius();
        if (!valid) {
            aggressors.clear(rec.entityId());
                baseNames.remove(rec.entityId());
            return null;
        }
        return candidate(aggId, mob);
    }

    private UUID recent(Map<UUID, Map.Entry<UUID, Long>> map, UUID player, long now) {
        Map.Entry<UUID, Long> e = map.get(player);
        if (e == null) return null;
        if (now - e.getValue() > RECENT_WINDOW_MS) {
            map.remove(player);
            return null;
        }
        return e.getKey();
    }

    private TargetRules.Candidate candidate(UUID id, Mob summon) {
        if (id == null) return null;
        Entity e = Bukkit.getEntity(id);
        if (e == null) return null;
        boolean sameWorld = e.getWorld().equals(summon.getWorld());
        double dist = sameWorld ? e.getLocation().distance(summon.getLocation()) : 0;
        return new TargetRules.Candidate(id, e instanceof Enemy, e instanceof Player,
                !e.isDead(), sameWorld, dist);
    }

    private boolean sameWorld(Entity a, Entity b) {
        return a.getWorld().equals(b.getWorld());
    }
}
