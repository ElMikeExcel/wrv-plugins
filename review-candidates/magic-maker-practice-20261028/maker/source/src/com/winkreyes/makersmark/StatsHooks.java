package com.winkreyes.makersmark;

import com.winkreyes.makersmark.core.Gear;
import com.winkreyes.makersmark.core.ProjectileIdentity;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Raider;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Kills (weapons), hits blocked (armor) and boss trophies. Counters go into the item tag right away;
 * armor lore is redrawn at most every stats.redraw-seconds per piece (or when a screen opens), so a long
 * fight is not rewriting lore on every hit.
 */
final class StatsHooks implements Listener {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final ExcelMakersMark plugin;
    private final Marks marks;
    private final FarmCredit farmCredit;
    private final java.util.function.Function<UUID, Player> bossPlayers;
    private final Map<EntityDamageByEntityEvent, Credit> hitSources = new java.util.WeakHashMap<>();
    private final Map<EntityDeathEvent, Boolean> creditSnapshots = new java.util.WeakHashMap<>();
    /** Last armor lore redraw per player per slot, and pieces with a newer count than their lore. */
    private final Map<UUID, EnumMap<EquipmentSlot, Long>> lastDraw = new HashMap<>();
    private final Map<UUID, EnumSet<EquipmentSlot>> dirty = new HashMap<>();
    /** Boss entity -> (weapon id -> player) for every tracked weapon that hurt it during the fight. */
    private final Map<UUID, Map<String, UUID>> bossDamagers = new HashMap<>();
    private final Map<UUID, Long> bossSeen = new HashMap<>();
    /** Named bosses (trophies.named) remembered by entity, because EliteMobs swaps a boss's name tag for its
     *  hit messages ("I've been hit!") while it is being fought, so the name can't be read again mid-fight. */
    private final Map<UUID, String> namedBoss = new HashMap<>();
    long redraws; // for /mark inspect style checks in tests (debug log)

    StatsHooks(ExcelMakersMark plugin) { this(plugin, Bukkit::getPlayer); }

    /** Narrow resolver seam for the existing boss-participation loop; production uses Bukkit. */
    StatsHooks(ExcelMakersMark plugin, java.util.function.Function<UUID, Player> bossPlayers) {
        this.plugin = plugin; this.marks = plugin.marks(); this.farmCredit = new FarmCredit(plugin);
        this.bossPlayers = java.util.Objects.requireNonNull(bossPlayers);
    }

    void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private long redrawMs() { return Math.max(0, plugin.getConfig().getLong("stats.redraw-seconds", 5)) * 1000L; }
    private boolean debug() { return plugin.getConfig().getBoolean("debug", false); }

    // ---------- helpers ----------
    /** Edit an item in place (mirror stack), reading and writing our data in one meta update. */
    private void bump(ItemStack it, Player viewer, boolean redraw, Consumer<Marks.Data> change) {
        it.editMeta(m -> {
            Marks.Data d = marks.read(m);
            change.accept(d);
            marks.write(m, d);
            if (redraw) marks.render(m, it.getType().name(), d, plugin.showsStats(viewer));
        });
    }

    private String ensureId(ItemStack it) {
        Marks.Data d = marks.read(it);
        if (d.id != null) return d.id;
        String id = UUID.randomUUID().toString();
        it.editMeta(m -> m.getPersistentDataContainer().set(marks.kId, PersistentDataType.STRING, id));
        return id;
    }

    /** Find a weapon by its id in a player's inventory (main, hotbar, armor, offhand). Returns the live stack. */
    private ItemStack findById(Player p, String id) {
        if (id == null) return null;
        PlayerInventory inv = p.getInventory();
        java.util.List<String> identities = new java.util.ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) identities.add(marks.read(inv.getItem(i)).id);
        int unique = ProjectileIdentity.uniqueIndex(identities, id);
        return unique < 0 ? null : inv.getItem(unique);
    }

    private boolean statsOn() { return plugin.statsEnabled(); }
    private boolean trackable(ItemStack it) { return it != null && !it.getType().isAir() && plugin.eligible(it); }

    /** Which player and weapon get credit for a hit: melee main hand, or the bow / crossbow / trident that fired. */
    private record Credit(Player player, ItemStack melee, String weaponId, Entity projectile) {}

    private Credit credit(EntityDamageByEntityEvent e) {
        if (e.isCancelled() || e.getFinalDamage() <= 0) return null;
        return hitSources.get(e); // Never re-read a different held item when the victim later dies.
    }

    private Credit captureHitSource(EntityDamageByEntityEvent e) {
        Entity dmg = e.getDamager();
        EntityDamageEvent.DamageCause c = e.getCause();
        if (c == EntityDamageEvent.DamageCause.THORNS) return null;
        if (dmg instanceof Player p) {
            if (c != EntityDamageEvent.DamageCause.ENTITY_ATTACK && c != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return null;
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (!trackable(hand) || !Gear.weapon(hand.getType().name())) return null;
            String established = ProjectileIdentity.capture(marks.read(hand).id).orElse(null);
            return established == null ? null : new Credit(p, null, established, null);
        }
        PersistentDataContainer pdc = dmg.getPersistentDataContainer();
        String wid = pdc.get(marks.kWeapon, PersistentDataType.STRING);
        String owner = pdc.get(marks.kOwner, PersistentDataType.STRING);
        if (wid == null || owner == null) return null;
        Player p;
        try { p = Bukkit.getPlayer(UUID.fromString(owner)); } catch (IllegalArgumentException ex) { return null; }
        if (p == null) return null;
        return new Credit(p, null, wid, dmg);
    }

    /** Apply a change to the credited weapon, wherever it is now (hand, inventory, or a flying trident). */
    private boolean applyToWeapon(Credit cr, Consumer<Marks.Data> change) {
        if (cr.melee() != null) { bump(cr.melee(), cr.player(), true, change); return true; }
        long observedMatches = java.util.Arrays.stream(cr.player().getInventory().getContents())
                .filter(it -> cr.weaponId() != null && cr.weaponId().equals(marks.read(it).id)).count();
        if (observedMatches > 1) return false; // A duplicate must not fall through to the flying-trident fallback.
        ItemStack inInv = findById(cr.player(), cr.weaponId());
        if (inInv != null) { bump(inInv, cr.player(), true, change); return true; }
        if (cr.projectile() instanceof Trident t && !t.isDead()) {
            ItemStack ti = t.getItemStack();
            String have = marks.read(ti).id;
            if (have == null && plugin.eligible(ti)) {
                // The thrown copy can lose the id set at launch; the entity tag says which trident this is.
                ti.editMeta(m -> m.getPersistentDataContainer().set(marks.kId, PersistentDataType.STRING, cr.weaponId()));
                have = cr.weaponId();
            }
            if (cr.weaponId().equals(have)) {
                bump(ti, cr.player(), true, change);
                t.setItemStack(ti);
                return true;
            }
        }
        return false;
    }

    // ---------- projectiles: remember which weapon fired them ----------
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent e) {
        if (!statsOn() || !(e.getEntity() instanceof Player p) || !plugin.counts(p)) return;
        ItemStack bow = e.getBow();
        if (!trackable(bow) || !Gear.weapon(bow.getType().name())) return;
        String id = ProjectileIdentity.capture(marks.read(bow).id).orElse(null);
        if (id == null) {
            // An unmarked launch cannot be linked safely to a later same-material held item.
            // Provision identity through a proven acquisition/use adapter BEFORE firing instead.
            if (debug()) plugin.getLogger().info("[stats] projectile history skipped: source lacks established item identity");
            return;
        }
        Entity proj = e.getProjectile();
        proj.getPersistentDataContainer().set(marks.kWeapon, PersistentDataType.STRING, id);
        proj.getPersistentDataContainer().set(marks.kOwner, PersistentDataType.STRING, p.getUniqueId().toString());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent e) {
        if (!statsOn() || !(e.getEntity() instanceof Trident t) || !(t.getShooter() instanceof Player p) || !plugin.counts(p)) return;
        ItemStack it = t.getItemStack();
        if (!trackable(it)) return;
        String id = ensureId(it);
        t.setItemStack(it);
        t.getPersistentDataContainer().set(marks.kWeapon, PersistentDataType.STRING, id);
        t.getPersistentDataContainer().set(marks.kOwner, PersistentDataType.STRING, p.getUniqueId().toString());
        // Vanilla can replace the thrown copy after this event, so set the id again one tick later.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!t.isValid()) return;
            ItemStack now = t.getItemStack();
            if (marks.read(now).id == null && plugin.eligible(now)) {
                now.editMeta(m -> m.getPersistentDataContainer().set(marks.kId, PersistentDataType.STRING, id));
                t.setItemStack(now);
            }
        });
    }

    // ---------- named bosses: read the name before other plugins change it ----------
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamagePre(EntityDamageEvent e) {
        if (e instanceof EntityDamageByEntityEvent hit) hitSources.put(hit, captureHitSource(hit));
        if (e.getEntity() instanceof LivingEntity le && !(le instanceof Player)) rememberNamed(le);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(org.bukkit.event.entity.CreatureSpawnEvent e) {
        if (!plugin.getConfig().getBoolean("trophies.named.enabled", false)) return;
        LivingEntity le = e.getEntity();
        // EliteMobs names its bosses right after the spawn, so look one and five ticks later.
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (le.isValid()) rememberNamed(le); }, 1L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (le.isValid()) rememberNamed(le); }, 5L);
    }

    private void rememberNamed(LivingEntity le) {
        if (namedBoss.containsKey(le.getUniqueId())) return;
        String id = nameMatch(le);
        if (id != null) {
            namedBoss.put(le.getUniqueId(), id);
            if (debug()) plugin.getLogger().info("[stats] named boss seen: " + id);
        }
    }

    /** Called by /mark reload: forget remembered names so a changed list applies to new sightings. */
    void clearNamed() { namedBoss.clear(); }

    // ---------- hits blocked + boss damagers ----------
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!statsOn()) return;
        if (e.getEntity() instanceof Player victim && plugin.counts(victim) && plugin.getConfig().getBoolean("stats.hits", true)
                && e.getCause() != EntityDamageEvent.DamageCause.THORNS) {
            armorHit(victim);
            shieldBlock(victim, e);
        }
        if (plugin.getConfig().getBoolean("stats.trophies", true) && e.getEntity() instanceof LivingEntity le) {
            String boss = bossId(le);
            if (boss != null) {
                Credit cr = credit(e);
                if (cr != null && plugin.counts(cr.player())) {
                    String id = cr.melee() != null ? ensureId(cr.melee()) : cr.weaponId();
                    bossDamagers.computeIfAbsent(le.getUniqueId(), k -> new LinkedHashMap<>()).put(id, cr.player().getUniqueId());
                    bossSeen.put(le.getUniqueId(), System.currentTimeMillis());
                    if (debug()) plugin.getLogger().info("[stats] " + cr.player().getName() + " hurt boss " + boss + " (" + bossDamagers.get(le.getUniqueId()).size() + " weapons)");
                }
            }
        }
    }

    /** Count a proven shield reduction, not merely an equipped or raised shield. */
    private void shieldBlock(Player p, EntityDamageByEntityEvent e) {
        if (!e.isApplicable(EntityDamageEvent.DamageModifier.BLOCKING)
                || !Double.isFinite(e.getDamage(EntityDamageEvent.DamageModifier.BLOCKING))
                || !(e.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) < 0)
                || !p.hasActiveItem()) return;
        EquipmentSlot slot = p.getActiveItemHand();
        if (slot != EquipmentSlot.HAND && slot != EquipmentSlot.OFF_HAND) return;
        ItemStack shield = p.getInventory().getItem(slot);
        if (shield.getType() != org.bukkit.Material.SHIELD || !trackable(shield)
                || p.getActiveItem().getType() != org.bukkit.Material.SHIELD) return;
        bump(shield, p, true, d -> { if (d.hits < Integer.MAX_VALUE) d.hits++; });
    }

    private void armorHit(Player p) {
        PlayerInventory inv = p.getInventory();
        long now = System.currentTimeMillis();
        EnumMap<EquipmentSlot, Long> last = lastDraw.computeIfAbsent(p.getUniqueId(), k -> new EnumMap<>(EquipmentSlot.class));
        for (EquipmentSlot s : ARMOR) {
            ItemStack it = inv.getItem(s);
            if (it == null || !Gear.armor(it.getType().name()) || !plugin.eligible(it)) continue;
            boolean draw = now - last.getOrDefault(s, 0L) >= redrawMs();
            bump(it, p, draw, d -> { if (d.hits < Integer.MAX_VALUE) d.hits++; });
            if (draw) { last.put(s, now); redraws++; EnumSet<EquipmentSlot> ds = dirty.get(p.getUniqueId()); if (ds != null) ds.remove(s); }
            else dirty.computeIfAbsent(p.getUniqueId(), k -> EnumSet.noneOf(EquipmentSlot.class)).add(s);
        }
    }

    /** Redraw pieces whose count changed since their last redraw, once the wait is over. */
    private void tick() {
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<UUID, EnumSet<EquipmentSlot>>> itr = dirty.entrySet().iterator(); itr.hasNext(); ) {
            Map.Entry<UUID, EnumSet<EquipmentSlot>> en = itr.next();
            Player p = Bukkit.getPlayer(en.getKey());
            if (p == null) { itr.remove(); continue; }
            EnumMap<EquipmentSlot, Long> last = lastDraw.computeIfAbsent(p.getUniqueId(), k -> new EnumMap<>(EquipmentSlot.class));
            en.getValue().removeIf(s -> {
                if (now - last.getOrDefault(s, 0L) < redrawMs()) return false;
                redrawSlot(p, s); last.put(s, now);
                return true;
            });
            if (en.getValue().isEmpty()) itr.remove();
        }
        // forget bosses that were never killed (left, despawned, or unloaded long ago)
        bossSeen.entrySet().removeIf(b -> {
            Entity ent = Bukkit.getEntity(b.getKey());
            if (ent != null && ent.isValid()) return false;
            if (now - b.getValue() < 30 * 60_000L) return false;
            bossDamagers.remove(b.getKey());
            return true;
        });
        if (namedBoss.size() > 256) namedBoss.keySet().removeIf(u -> { Entity ent = Bukkit.getEntity(u); return ent == null || !ent.isValid(); });
    }

    private void redrawSlot(Player p, EquipmentSlot s) {
        ItemStack it = p.getInventory().getItem(s);
        if (it == null || !plugin.eligible(it)) return;
        Marks.Data d = marks.read(it);
        it.editMeta(m -> marks.render(m, it.getType().name(), d, plugin.showsStats(p)));
        redraws++;
        if (debug()) plugin.getLogger().info("[stats] redraw " + p.getName() + " " + s + " hits=" + d.hits);
    }

    private void flush(Player p) {
        EnumSet<EquipmentSlot> ds = dirty.remove(p.getUniqueId());
        if (ds == null) return;
        EnumMap<EquipmentSlot, Long> last = lastDraw.computeIfAbsent(p.getUniqueId(), k -> new EnumMap<>(EquipmentSlot.class));
        for (EquipmentSlot s : ds) { redrawSlot(p, s); last.put(s, System.currentTimeMillis()); }
    }

    void flushAll() { for (Player p : Bukkit.getOnlinePlayers()) flush(p); }

    /** After /mark stats on|off: show or hide the counter lines on everything the player carries. */
    void redrawInventory(Player p) {
        dirty.remove(p.getUniqueId());
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || !plugin.eligible(it)) continue;
            Marks.Data d = marks.read(it);
            if (!d.hasStats()) continue;
            it.editMeta(m -> marks.render(m, it.getType().name(), d, plugin.showsStats(p)));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onOpen(InventoryOpenEvent e) { if (e.getPlayer() instanceof Player p) flush(p); }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        flush(e.getPlayer());
        lastDraw.remove(e.getPlayer().getUniqueId());
        plugin.tips().quit(e.getPlayer().getUniqueId());
    }

    // ---------- kills + boss trophies ----------
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeathCreditSnapshot(EntityDeathEvent e) {
        if (!statsOn() || !plugin.getConfig().getBoolean("stats.kills", true)
                || e.getEntity() instanceof Player || e.getEntity() instanceof ArmorStand) return;
        creditSnapshots.put(e, farmCredit.reduced(e.getEntity()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent e) {
        LivingEntity dead = e.getEntity();
        boolean reduced = Boolean.TRUE.equals(creditSnapshots.remove(e));
        if (e.isCancelled()) return;
        Map<String, UUID> damagers = bossDamagers.remove(dead.getUniqueId());
        bossSeen.remove(dead.getUniqueId());
        if (!statsOn() || dead instanceof ArmorStand) { namedBoss.remove(dead.getUniqueId()); return; }

        if (plugin.getConfig().getBoolean("stats.kills", true) && dead.getLastDamageCause() instanceof EntityDamageByEntityEvent last) {
            Credit cr = credit(last);
            if (cr != null && cr.player() != dead && plugin.counts(cr.player())) {
                boolean pvp = dead instanceof Player;
                boolean ok = applyToWeapon(cr, d -> { if (pvp) d.pvp++; else d.mobKill(reduced); });
                if(ok && !pvp && plugin.historyRecorder()!=null && cr.weaponId()!=null){
                    com.winkreyes.makersmark.history.HistoryLedger.Key accomplishment=ordinaryAccomplishment(dead);
                    if(accomplishment!=null)try{plugin.historyRecorder().record(UUID.fromString(cr.weaponId()),dead.getUniqueId(),cr.player().getUniqueId(),accomplishment);}catch(IllegalArgumentException invalid){plugin.getLogger().warning("History refused malformed verified weapon identity");}
                }
                if (debug()) plugin.getLogger().info("[stats] kill " + dead.getType() + " by " + cr.player().getName() + (cr.melee() != null ? " melee" : " projectile") + " credited=" + ok);
            }
        }

        String boss = bossId(dead);
        namedBoss.remove(dead.getUniqueId());
        if (boss != null && damagers != null && plugin.getConfig().getBoolean("stats.trophies", true)) {
            for (Map.Entry<String, UUID> en : damagers.entrySet()) {
                Player p = bossPlayers.apply(en.getValue());
                if (p == null || !plugin.counts(p)) continue;
                // Count observable inventory AND flying copies before choosing.
                // Ambiguity is not absence and must never select the first trident.
                long copies = java.util.Arrays.stream(p.getInventory().getContents())
                        .filter(it -> en.getKey().equals(marks.read(it).id)).count();
                java.util.List<Trident> flying = new java.util.ArrayList<>();
                for (Trident t : dead.getWorld().getEntitiesByClass(Trident.class)) {
                    ItemStack ti = t.getItemStack();
                    String tid = marks.read(ti).id;
                    if (tid == null) tid = t.getPersistentDataContainer().get(marks.kWeapon, PersistentDataType.STRING);
                    if (en.getKey().equals(tid)) flying.add(t);
                }
                if (copies + flying.size() != 1) {
                    plugin.getLogger().warning("Boss trophy/history refused unresolved physical weapon identity "
                            + en.getKey() + ": inventory=" + copies + ", flying=" + flying.size());
                    continue;
                }
                ItemStack w = findById(p, en.getKey());
                if (w != null && !trackable(w)) continue;
                if (w == null) {
                    Trident t = flying.getFirst();
                    ItemStack ti = t.getItemStack();
                    if (!trackable(ti)) continue;
                    if (marks.read(ti).id == null) ti.editMeta(m -> m.getPersistentDataContainer()
                            .set(marks.kId, PersistentDataType.STRING, en.getKey()));
                    bump(ti, p, true, d -> d.slain.merge(boss, 1, Integer::sum));
                    t.setItemStack(ti);
                    recordBossParticipation(dead, en.getKey(), p);
                    if (debug()) plugin.getLogger().info("[stats] trophy " + boss + " for " + p.getName() + " (unique trident in flight)");
                    continue;
                }
                bump(w, p, true, d -> d.slain.merge(boss, 1, Integer::sum));
                recordBossParticipation(dead, en.getKey(), p);
                if (debug()) plugin.getLogger().info("[stats] trophy " + boss + " for " + p.getName() + " " + w.getType());
            }
        }
    }

    /** Archive the same victim receipt as the final-blow path; never add assist kills/credit. */
    private void recordBossParticipation(LivingEntity dead, String weaponId, Player participant) {
        if (plugin.historyRecorder() == null) return;
        var accomplishment = ordinaryAccomplishment(dead);
        if (accomplishment == null) return;
        try {
            plugin.historyRecorder().record(UUID.fromString(weaponId), dead.getUniqueId(),
                    participant.getUniqueId(), accomplishment);
        } catch (IllegalArgumentException invalid) {
            plugin.getLogger().warning("Boss history refused malformed tracked weapon identity");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemove(EntityRemoveEvent e) {
        if (e.getCause() == EntityRemoveEvent.Cause.UNLOAD || e.getCause() == EntityRemoveEvent.Cause.PLAYER_QUIT) return;
        namedBoss.remove(e.getEntity().getUniqueId());
        Map<String, UUID> gone = bossDamagers.remove(e.getEntity().getUniqueId());
        if (gone != null) {
            bossSeen.remove(e.getEntity().getUniqueId());
            if (debug()) plugin.getLogger().info("[stats] boss " + e.getEntity().getType() + " removed (" + e.getCause() + "), cleared " + gone.size() + " damager(s)");
        }
    }

    /** Observed vanilla names only. Foreign entity PDC is held until its integration is verified. */
    private com.winkreyes.makersmark.history.HistoryLedger.Key ordinaryAccomplishment(LivingEntity entity){
        return Accomplishments.observed(entity);
    }

    /** Boss id from the configured list: entity type, optional raid captain, optional name match. */
    String bossId(LivingEntity le) {
        Map<String, String> labels = plugin.bossLabels();
        String type = le.getType().name();
        if (labels.containsKey(type)) return type;
        if (labels.containsKey("RAID_CAPTAIN") && le instanceof Raider r && r.isPatrolLeader()) return "RAID_CAPTAIN";
        if (!plugin.getConfig().getBoolean("trophies.named.enabled", false)) return null;
        String known = namedBoss.get(le.getUniqueId());
        if (known != null) return known;
        String id = nameMatch(le);
        if (id != null) namedBoss.put(le.getUniqueId(), id);
        return id;
    }

    private String nameMatch(LivingEntity le) {
        if (!plugin.getConfig().getBoolean("trophies.named.enabled", false) || le.customName() == null) return null;
        String name = PlainTextComponentSerializer.plainText().serialize(le.customName()).toLowerCase(Locale.ROOT);
        for (Map<?, ?> m : plugin.getConfig().getMapList("trophies.named.list")) {
            Object id = m.get("id"), match = m.get("match");
            if (id != null && match != null && name.contains(match.toString().toLowerCase(Locale.ROOT))) return "NAME:" + id.toString().toUpperCase(Locale.ROOT);
        }
        return null;
    }
}
