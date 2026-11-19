package com.winkreyes.excelmobveil;

import com.destroystokyo.paper.event.entity.EndermanAttackPlayerEvent;
import com.destroystokyo.paper.event.entity.SlimeTargetLivingEntityEvent;
import io.papermc.paper.event.entity.WardenAngerChangeEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import io.papermc.paper.registry.RegistryAccess;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.*;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.LongSupplier;
import java.util.logging.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Reconstructed event-level checks. Native Paper consumption and AI need separate runtime checks. */
class VeilGameplayTest {
    static MockedStatic<RegistryAccess> registryAccess;
    static final Map<NamespacedKey,PotionEffectType> effects=new HashMap<>();
    MockedStatic<Bukkit> bukkit;
    ExcelMobVeil plugin;
    Player player;
    PlayerInventory inventory;
    BukkitScheduler scheduler;
    final UUID id=UUID.randomUUID();
    final AtomicLong now=new AtomicLong(1_000);
    final Map<NamespacedKey,Object> state=new HashMap<>();
    final AtomicReference<PotionEffect> invis=new AtomicReference<>();
    final Deque<Runnable> tasks=new ArrayDeque<>();
    final Map<Integer,ItemStack> inventorySlots=new HashMap<>();
    final Map<UUID,PlayerItemConsumeEvent> pending=new HashMap<>();
    final NamespacedKey started=new NamespacedKey("excelmobveil","started_at");
    final NamespacedKey expires=new NamespacedKey("excelmobveil","expires_at");
    final NamespacedKey potionKey=new NamespacedKey("excelmobveil","potion");

    @BeforeAll static void bootstrapEffectRegistry() {
        registryAccess=mockStatic(RegistryAccess.class);
        RegistryAccess access=mock(RegistryAccess.class,invocation->{
            if(invocation.getMethod().getName().equals("getRegistry")) {
                return mock(Registry.class,call->{
                    String name=call.getMethod().getName();
                    if(name.equals("get") || name.equals("getOrThrow")) {
                        NamespacedKey key=call.getArgument(0);
                        return effects.computeIfAbsent(key,k->{
                            PotionEffectType type=mock(PotionEffectType.class);
                            when(type.getKey()).thenReturn(k); when(type.getName()).thenReturn(k.getKey());
                            return type;
                        });
                    }
                    return RETURNS_DEFAULTS.answer(call);
                });
            }
            return RETURNS_DEFAULTS.answer(invocation);
        });
        registryAccess.when(RegistryAccess::registryAccess).thenReturn(access);
        assertNotNull(PotionEffectType.INVISIBILITY);
    }
    @AfterAll static void closeRegistry() { if(registryAccess!=null) registryAccess.close(); }

    @BeforeEach void setup() throws Exception {
        plugin=mock(ExcelMobVeil.class,CALLS_REAL_METHODS);
        inject("clock",(LongSupplier)now::get);
        inject("potionKey",potionKey); inject("startedKey",started); inject("expiresKey",expires);
        inject("purchasePlayers",new HashMap<>()); inject("pendingConsumes",pending); inject("ownEffectChanges",new HashSet<>());
        Logger logger=Logger.getAnonymousLogger(); logger.setLevel(Level.OFF); doReturn(logger).when(plugin).getLogger();
        player=mock(Player.class); when(player.getUniqueId()).thenReturn(id); when(player.isOnline()).thenReturn(true);
        inventory=mock(PlayerInventory.class); when(player.getInventory()).thenReturn(inventory);
        when(inventory.getHeldItemSlot()).thenReturn(0);
        when(inventory.getItem(anyInt())).thenAnswer(i->inventorySlots.get(i.getArgument(0)));
        doAnswer(i->{inventorySlots.put(i.getArgument(0),i.getArgument(1));return null;})
                .when(inventory).setItem(anyInt(),any(ItemStack.class));
        PersistentDataContainer data=mock(PersistentDataContainer.class);
        when(player.getPersistentDataContainer()).thenReturn(data);
        when(data.has(any(NamespacedKey.class))).thenAnswer(i->state.containsKey(i.getArgument(0)));
        when(data.has(any(NamespacedKey.class),eq(PersistentDataType.LONG)))
                .thenAnswer(i->state.get(i.getArgument(0)) instanceof Long);
        when(data.get(any(NamespacedKey.class),eq(PersistentDataType.LONG))).thenAnswer(i->{
            Object value=state.get(i.getArgument(0));
            if(value!=null && !(value instanceof Long)) throw new IllegalArgumentException("Wrong PDC primitive type");
            return value;
        });
        doAnswer(i->{state.put(i.getArgument(0),i.getArgument(2));return null;})
                .when(data).set(any(NamespacedKey.class),eq(PersistentDataType.LONG),anyLong());
        doAnswer(i->{state.remove(i.getArgument(0));return null;}).when(data).remove(any(NamespacedKey.class));
        when(player.getPotionEffect(PotionEffectType.INVISIBILITY)).thenAnswer(i->invis.get());
        when(player.hasPotionEffect(PotionEffectType.INVISIBILITY)).thenAnswer(i->invis.get()!=null);
        when(player.addPotionEffect(any(PotionEffect.class))).thenAnswer(i->{invis.set(i.getArgument(0));return true;});
        doAnswer(i->{invis.set(null);return null;}).when(player).removePotionEffect(PotionEffectType.INVISIBILITY);
        scheduler=mock(BukkitScheduler.class);
        when(scheduler.runTask(eq(plugin),any(Runnable.class))).thenAnswer(i->{tasks.add(i.getArgument(1));return null;});
        bukkit=mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(Bukkit::getWorlds).thenReturn(List.of());
        bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(player));
    }
    @AfterEach void teardown() { if(bukkit!=null) bukkit.close(); }
    void inject(String name,Object value) throws Exception {
        Field field=ExcelMobVeil.class.getDeclaredField(name); field.setAccessible(true); field.set(plugin,value);
    }
    void drain() {
        while(!tasks.isEmpty()) {
            PlayerItemConsumeEvent drink=pending.get(id);
            if(drink!=null && !drink.isCancelled()) {
                int slot=drink.getHand()==EquipmentSlot.OFF_HAND?40:0;
                if(inventorySlots.get(slot)==drink.getItem()) inventorySlots.put(slot,item(Material.GLASS_BOTTLE,false));
            }
            tasks.remove().run();
        }
    }
    void active() {
        state.put(started,now.get()); state.put(expires,now.get()+480_000L);
        invis.set(new PotionEffect(PotionEffectType.INVISIBILITY,9_600,0,false,false,true));
    }
    void refresh(boolean restore) throws Exception {
        Method method=ExcelMobVeil.class.getDeclaredMethod("refreshPlayer",Player.class,boolean.class);
        method.setAccessible(true); method.invoke(plugin,player,restore);
    }
    ItemStack item(Material material,boolean marked) {
        ItemStack result=mock(ItemStack.class);
        when(result.getType()).thenReturn(material); when(result.clone()).thenReturn(result); when(result.getAmount()).thenReturn(1);
        when(result.isSimilar(any(ItemStack.class))).thenAnswer(i->result==i.getArgument(0));
        when(result.hasItemMeta()).thenReturn(true);
        ItemMeta meta=mock(ItemMeta.class); when(result.getItemMeta()).thenReturn(meta);
        PersistentDataContainer data=mock(PersistentDataContainer.class); when(meta.getPersistentDataContainer()).thenReturn(data);
        when(data.has(potionKey,PersistentDataType.BYTE)).thenReturn(marked);
        when(data.get(potionKey,PersistentDataType.BYTE)).thenReturn(marked?(byte)1:null);
        return result;
    }
    PlayerItemConsumeEvent consume(ItemStack item) {
        if(!pending.containsKey(id)) inventorySlots.put(0,item);
        return new PlayerItemConsumeEvent(player,item,EquipmentSlot.HAND);
    }
    void drink() { plugin.consume(consume(item(Material.POTION,true))); drain(); }
    void melee() {
        PrePlayerAttackEntityEvent event=mock(PrePlayerAttackEntityEvent.class);
        when(event.getPlayer()).thenReturn(player); plugin.melee(event);
    }
    void quit() {
        PlayerQuitEvent event=mock(PlayerQuitEvent.class); when(event.getPlayer()).thenReturn(player); plugin.quit(event);
    }
    void changeWorld() {
        PlayerChangedWorldEvent event=mock(PlayerChangedWorldEvent.class); when(event.getPlayer()).thenReturn(player); plugin.changedWorld(event);
    }
    <E extends Event> void dispatch(String methodName,E event) throws Exception {
        Method method=ExcelMobVeil.class.getMethod(methodName,event.getClass());
        EventHandler handler=method.getAnnotation(EventHandler.class);
        if(handler.ignoreCancelled() && event instanceof Cancellable cancelled && cancelled.isCancelled()) return;
        method.invoke(plugin,event);
    }
    EntityPotionEffectEvent external(EntityPotionEffectEvent.Action action,boolean override) {
        PotionEffect replacement=action==EntityPotionEffectEvent.Action.CHANGED?
                new PotionEffect(PotionEffectType.INVISIBILITY,12_000,1):null;
        return new EntityPotionEffectEvent(player,invis.get(),replacement,EntityPotionEffectEvent.Cause.PLUGIN,action,override);
    }
    EntityDamageByEntityEvent damage(Entity source) {
        EntityDamageByEntityEvent event=mock(EntityDamageByEntityEvent.class); when(event.getDamager()).thenReturn(source); return event;
    }

    @Test void storedMarkerAloneNeverActivates() {
        assertTrue(plugin.isVeilPotion(item(Material.POTION,true))); assertFalse(plugin.isVeiled(player));
        verify(player,never()).addPotionEffect(any());
    }
    @Test void onlyDrinkableMarkedPotionsAreRecognized() {
        assertFalse(plugin.isVeilPotion(null)); assertFalse(plugin.isVeilPotion(item(Material.POTION,false)));
        assertFalse(plugin.isVeilPotion(item(Material.SPLASH_POTION,true)));
        assertFalse(plugin.isVeilPotion(item(Material.LINGERING_POTION,true)));
    }
    @Test void activationWaitsUntilNextTickThenUsesAbsoluteDeadline() {
        plugin.consume(consume(item(Material.POTION,true))); assertFalse(plugin.isVeiled(player)); drain();
        assertTrue(plugin.isVeiled(player)); assertEquals(481_000L,state.get(expires)); assertEquals(9_600,invis.get().getDuration());
        verify(player,times(2)).saveData();
    }
    @Test void cancellationBeforeDispatchConsumesNothingAndSchedulesNothing() throws Exception {
        PlayerItemConsumeEvent event=consume(item(Material.POTION,true)); event.setCancelled(true); dispatch("consume",event);
        assertTrue(tasks.isEmpty()); assertFalse(plugin.isVeiled(player));
    }
    @Test void laterMonitorCancellationNeverActivates() {
        PlayerItemConsumeEvent event=consume(item(Material.POTION,true)); plugin.consume(event); event.setCancelled(true); drain();
        assertFalse(plugin.isVeiled(player)); verify(player,times(1)).saveData();
    }
    @Test void lateItemReplacementNeverActivates() {
        PlayerItemConsumeEvent event=consume(item(Material.POTION,true)); plugin.consume(event);
        event.setItem(item(Material.POTION,false)); drain(); assertFalse(plugin.isVeiled(player));
    }
    @Test void duplicatePendingDrinkIsCancelledWithoutExtendingFirst() {
        PlayerItemConsumeEvent first=consume(item(Material.POTION,true)),second=consume(item(Material.POTION,true));
        plugin.consume(first); plugin.consume(second); assertTrue(second.isCancelled()); drain();
        assertEquals(481_000L,state.get(expires)); verify(player,times(1)).addPotionEffect(any());
    }
    @Test void activeVeilAllowsAnotherDrinkAndAddsExactlyEightMinutesToRemainingTime() {
        active(); now.addAndGet(5_000); PlayerItemConsumeEvent event=consume(item(Material.POTION,true)); plugin.consume(event);
        assertFalse(event.isCancelled()); assertEquals(481_000L,state.get(expires)); drain();
        assertEquals(961_000L,state.get(expires)); assertEquals(955_000L,plugin.remainingMillis(player));
        assertEquals(1_000L,state.get(started)); assertEquals(19_100,invis.get().getDuration());
    }
    @Test void ordinaryInvisibilityRejectsVeilDrink() {
        invis.set(new PotionEffect(PotionEffectType.INVISIBILITY,100,0)); PlayerItemConsumeEvent event=consume(item(Material.POTION,true));
        plugin.consume(event); assertTrue(event.isCancelled()); assertFalse(plugin.isVeiled(player));
    }
    @Test void quitBeforeActivationInvalidatesPendingDrink() {
        plugin.consume(consume(item(Material.POTION,true))); quit(); drain(); assertFalse(plugin.isVeiled(player));
    }
    @Test void deathBeforeActivationInvalidatesPendingDrink() {
        plugin.consume(consume(item(Material.POTION,true))); when(player.isDead()).thenReturn(true); drain(); assertFalse(plugin.isVeiled(player));
    }
    @Test void offlineBeforeActivationNeverGrantsVeil() {
        plugin.consume(consume(item(Material.POTION,true))); when(player.isOnline()).thenReturn(false); drain(); assertFalse(plugin.isVeiled(player));
    }
    @Test void milkEndsVeilAfterFinalCancellationCheck() {
        active(); plugin.consume(consume(item(Material.MILK_BUCKET,false))); assertTrue(plugin.isVeiled(player)); drain();
        assertFalse(plugin.isVeiled(player)); assertNull(invis.get());
    }
    @Test void cancelledMilkDoesNotEndVeil() {
        active(); PlayerItemConsumeEvent event=consume(item(Material.MILK_BUCKET,false)); plugin.consume(event); event.setCancelled(true); drain();
        assertTrue(plugin.isVeiled(player));
    }
    @Test void deathRemovesRecordAndEffectWithoutSavingStaleDeathInventory() {
        active(); PlayerDeathEvent event=mock(PlayerDeathEvent.class); when(event.getEntity()).thenReturn(player); plugin.died(event);
        assertFalse(plugin.isVeiled(player)); assertNull(invis.get()); verify(player,never()).saveData();
    }
    @Test void expiryCleansRecordAndVisibleEffect() throws Exception {
        active(); now.addAndGet(480_000); assertFalse(plugin.isVeiled(player)); refresh(false);
        assertTrue(state.isEmpty()); assertNull(invis.get()); verify(player).saveData();
    }
    @Test void missingEffectOnPulseEndsRecordAndDoesNotResurrectIt() throws Exception {
        active(); invis.set(null); refresh(false); assertFalse(plugin.isVeiled(player)); verify(player,never()).addPotionEffect(any());
    }
    @Test void reloadPersistsDeadlineAndRestoresOnlyRemainingTime() throws Exception {
        active(); plugin.onDisable(); assertNull(invis.get()); assertEquals(481_000L,state.get(expires)); now.addAndGet(120_000); refresh(true);
        assertEquals(7_200,invis.get().getDuration()); assertEquals(481_000L,state.get(expires));
    }
    @Test void worldTransitionNeverRefreshesEightMinuteDeadline() {
        active(); now.addAndGet(60_000); changeWorld(); assertEquals(481_000L,state.get(expires)); assertEquals(420_000L,plugin.remainingMillis(player));
    }
    @Test void targetSuppressionDoesNotInspectOrRemoveArmor() {
        active(); EntityTargetLivingEntityEvent event=mock(EntityTargetLivingEntityEvent.class); when(event.getTarget()).thenReturn(player);
        plugin.target(event); verify(event).setCancelled(true); verify(player,never()).getInventory();
    }
    @Test void inactiveOrExpiredPlayerCanBeTargetedNormally() {
        EntityTargetLivingEntityEvent event=mock(EntityTargetLivingEntityEvent.class); when(event.getTarget()).thenReturn(player);
        plugin.target(event); active(); now.addAndGet(480_000); plugin.target(event); verify(event,never()).setCancelled(anyBoolean());
    }
    @Test void endermanSlimeAndWardenSpecialTargetEventsAreSuppressed() {
        active(); EndermanAttackPlayerEvent enderman=mock(EndermanAttackPlayerEvent.class); when(enderman.getPlayer()).thenReturn(player);
        plugin.enderman(enderman); verify(enderman).setCancelled(true);
        SlimeTargetLivingEntityEvent slime=mock(SlimeTargetLivingEntityEvent.class); when(slime.getTarget()).thenReturn(player);
        plugin.slime(slime); verify(slime).setCancelled(true);
        WardenAngerChangeEvent warden=mock(WardenAngerChangeEvent.class); when(warden.getTarget()).thenReturn(player);
        plugin.anger(warden); verify(warden).setNewAnger(0);
    }
    @Test void meleeAttemptEndsVeilEvenIfDamageWillBeCancelled() {
        active(); melee(); assertFalse(plugin.isVeiled(player)); assertNull(invis.get());
    }
    @Test void incomingMobDamageIsNeverCancelledOrReduced() {
        active(); EntityDamageByEntityEvent event=damage(mock(Zombie.class)); when(event.getEntity()).thenReturn(player);
        plugin.outgoing(event); assertTrue(plugin.isVeiled(player)); verify(event,never()).setCancelled(anyBoolean());
        verify(event,never()).setDamage(anyDouble()); verify(player,never()).setHealth(anyDouble());
    }
    @Test void outgoingPlayerDamageEndsVeilWithoutChangingDamage() {
        active(); EntityDamageByEntityEvent event=damage(player); plugin.outgoing(event); assertFalse(plugin.isVeiled(player));
        verify(event,never()).setCancelled(anyBoolean()); verify(event,never()).setDamage(anyDouble());
    }
    @Test void arrowLaunchEndsVeil() {
        active(); Arrow arrow=mock(Arrow.class); when(arrow.getShooter()).thenReturn(player);
        ProjectileLaunchEvent event=mock(ProjectileLaunchEvent.class); when(event.getEntity()).thenReturn(arrow); plugin.projectile(event);
        assertFalse(plugin.isVeiled(player));
    }
    @Test void harmlessUtilityProjectilesDoNotEndVeil() {
        active(); for(Class<? extends Projectile> type:List.of(EnderPearl.class,ThrownExpBottle.class,FishHook.class)) {
            Projectile shot=mock(type); when(shot.getShooter()).thenReturn(player);
            ProjectileLaunchEvent event=mock(ProjectileLaunchEvent.class); when(event.getEntity()).thenReturn(shot);
            plugin.projectile(event); assertTrue(plugin.isVeiled(player));
        }
    }
    @Test void replacingInvisibilityEndsMobIgnoreAndPreservesReplacement() {
        active(); EntityPotionEffectEvent event=external(EntityPotionEffectEvent.Action.CHANGED,true);
        plugin.effectChanged(event); invis.set(event.getNewEffect()); drain(); assertFalse(plugin.isVeiled(player)); assertSame(event.getNewEffect(),invis.get());
    }
    @Test void nonOverridingEffectChangeDoesNotEndVeil() {
        active(); plugin.effectChanged(external(EntityPotionEffectEvent.Action.CHANGED,false)); drain(); assertTrue(plugin.isVeiled(player));
    }
    @Test void externalReplacementRetiresRecordSynchronouslyBeforeAttackCanStripIt() {
        active(); EntityPotionEffectEvent event=external(EntityPotionEffectEvent.Action.CHANGED,true);
        plugin.effectChanged(event); assertFalse(plugin.isVeiled(player)); invis.set(event.getNewEffect()); melee();
        assertSame(event.getNewEffect(),invis.get()); drain(); assertSame(event.getNewEffect(),invis.get());
    }
    @Test void cancelledExternalChangeIsIgnoredAtDispatch() throws Exception {
        active(); EntityPotionEffectEvent event=external(EntityPotionEffectEvent.Action.CHANGED,true); event.setCancelled(true);
        dispatch("effectChanged",event); drain(); assertTrue(plugin.isVeiled(player));
    }
    @Test void lateMonitorCancellationConservativelyRetiresVeilWithoutRemovingEffect() {
        active(); PotionEffect before=invis.get(); EntityPotionEffectEvent event=external(EntityPotionEffectEvent.Action.CHANGED,true);
        plugin.effectChanged(event); event.setCancelled(true); drain(); assertFalse(plugin.isVeiled(player)); assertSame(before,invis.get());
    }
    @Test void externalRemovalEndsMobIgnoreImmediatelyAndDoesNotReapply() {
        active(); plugin.effectChanged(external(EntityPotionEffectEvent.Action.REMOVED,false)); assertFalse(plugin.isVeiled(player));
        invis.set(null); drain(); assertNull(invis.get()); verify(player,never()).addPotionEffect(any());
    }
    @Test void failedRestoreCannotKeepMobIgnoreUnderAnExistingEffect() throws Exception {
        active(); when(player.addPotionEffect(any(PotionEffect.class))).thenReturn(false); refresh(true);
        assertFalse(plugin.isVeiled(player)); assertTrue(state.isEmpty());
    }
    @Test void restorePreservesUnrelatedInvisibilityAndRetiresVeil() throws Exception {
        active(); PotionEffect unrelated=new PotionEffect(PotionEffectType.INVISIBILITY,20_000,1); invis.set(unrelated); refresh(true);
        assertFalse(plugin.isVeiled(player)); assertSame(unrelated,invis.get());
        verify(player,never()).addPotionEffect(any()); verify(player,never()).removePotionEffect(any());
    }
    @Test void expiryDoesNotRemoveUnrelatedInvisibility() throws Exception {
        active(); PotionEffect unrelated=new PotionEffect(PotionEffectType.INVISIBILITY,20_000,1); invis.set(unrelated); now.addAndGet(480_000); refresh(false);
        assertFalse(plugin.isVeiled(player)); assertSame(unrelated,invis.get());
    }
    @Test void lagPulseReplacesOwnedEffectWithRemainingRealTime() throws Exception {
        active(); now.addAndGet(120_000); refresh(false); assertEquals(7_200,invis.get().getDuration()); assertEquals(481_000L,state.get(expires));
        verify(player).removePotionEffect(PotionEffectType.INVISIBILITY);
    }
    @Test void effectApplicationFailureDoesNotGrantMobIgnore() {
        when(player.addPotionEffect(any(PotionEffect.class))).thenReturn(false); drink();
        assertFalse(plugin.isVeiled(player)); assertTrue(state.isEmpty());
    }
    @Test void ownedAddAndRemoveEventsDoNotRetireVeil() {
        when(player.addPotionEffect(any(PotionEffect.class))).thenAnswer(i->{
            PotionEffect next=i.getArgument(0);
            plugin.effectChanged(new EntityPotionEffectEvent(player,invis.get(),next,EntityPotionEffectEvent.Cause.PLUGIN,
                    invis.get()==null?EntityPotionEffectEvent.Action.ADDED:EntityPotionEffectEvent.Action.CHANGED,true));
            invis.set(next); return true;
        });
        doAnswer(i->{
            plugin.effectChanged(new EntityPotionEffectEvent(player,invis.get(),null,EntityPotionEffectEvent.Cause.PLUGIN,
                    EntityPotionEffectEvent.Action.REMOVED,false)); invis.set(null); return null;
        }).when(player).removePotionEffect(PotionEffectType.INVISIBILITY);
        drink(); assertTrue(plugin.isVeiled(player)); changeWorld(); drain(); assertTrue(plugin.isVeiled(player));
    }
    @Test void rangedLingeringExplosiveAndFangDamageRevealResponsiblePlayer() {
        Arrow arrow=mock(Arrow.class); when(arrow.getShooter()).thenReturn(player);
        AreaEffectCloud cloud=mock(AreaEffectCloud.class); when(cloud.getSource()).thenReturn(player);
        TNTPrimed tnt=mock(TNTPrimed.class); when(tnt.getSource()).thenReturn(player);
        EvokerFangs fangs=mock(EvokerFangs.class); when(fangs.getOwner()).thenReturn(player);
        for(Entity source:List.of(arrow,cloud,tnt,fangs)) { active(); plugin.outgoing(damage(source)); assertFalse(plugin.isVeiled(player)); assertNull(invis.get()); }
    }
    @Test void bowShotEndsVeil() {
        active(); EntityShootBowEvent event=mock(EntityShootBowEvent.class); when(event.getEntity()).thenReturn(player);
        plugin.bow(event); assertFalse(plugin.isVeiled(player)); assertNull(invis.get());
    }
    @Test void farmingMiningPlantingHarvestingAndAirSwingsRetainVeil() throws Exception {
        active(); int handlers=0;
        for(Class<? extends Event> type:List.of(BlockBreakEvent.class,BlockPlaceEvent.class,PlayerHarvestBlockEvent.class,
                PlayerInteractEvent.class,PlayerAnimationEvent.class,BlockDamageEvent.class)) {
            Event event=mock(type);
            for(Method method:ExcelMobVeil.class.getMethods()) {
                if(method.getAnnotation(EventHandler.class)!=null && method.getParameterCount()==1 && method.getParameterTypes()[0].isInstance(event)) {
                    handlers++; method.invoke(plugin,event);
                }
            }
            assertTrue(plugin.isVeiled(player),type.getSimpleName()+" must not reveal the player");
        }
        assertEquals(0,handlers,"Broad interaction/swing listeners must not treat farming as an entity attack");
        verify(player,never()).removePotionEffect(any());
    }
    @Test void existingTargetsPathsAngerAndWitherHeadsAreClearedOnActivation() {
        World world=mock(World.class); Mob mob=mock(Mob.class); Warden warden=mock(Warden.class); Wither wither=mock(Wither.class);
        com.destroystokyo.paper.entity.Pathfinder path=mock(com.destroystokyo.paper.entity.Pathfinder.class);
        when(mob.getTarget()).thenReturn(player); when(mob.getPathfinder()).thenReturn(path); when(mob.getMemory(MemoryKey.ANGRY_AT)).thenReturn(id);
        when(wither.getTarget(Wither.Head.LEFT)).thenReturn(player); when(world.getLivingEntities()).thenReturn(List.of(mob,warden,wither));
        bukkit.when(Bukkit::getWorlds).thenReturn(List.of(world)); drink();
        verify(mob).setTarget(null); verify(path).stopPathfinding(); verify(mob).setMemory(MemoryKey.ANGRY_AT,null);
        verify(warden).clearAnger(player); verify(wither).setTarget(Wither.Head.LEFT,null);
    }
    @Test void passiveThornsRetaliationDoesNotCountAsAttacking() {
        active(); EntityDamageByEntityEvent event=damage(player); when(event.getCause()).thenReturn(EntityDamageEvent.DamageCause.THORNS);
        plugin.outgoing(event); assertTrue(plugin.isVeiled(player)); verify(event,never()).setCancelled(anyBoolean()); verify(event,never()).setDamage(anyDouble());
    }
    @Test void incorrectlyTypedPersistentDeadlineFailsClosedAndIsCleanedUp() throws Exception {
        active(); state.put(expires,"481000"); assertEquals(0,plugin.remainingMillis(player)); assertFalse(plugin.isVeiled(player));
        refresh(false); assertTrue(state.isEmpty()); assertNull(invis.get());
    }
    @Test void incompletePersistentDeadlineIsCleanedUp() throws Exception {
        state.put(started,now.get()); refresh(false); assertTrue(state.isEmpty()); assertFalse(plugin.isVeiled(player));
    }
    @Test void incorrectlyTypedPotionMarkerIsRejectedWithoutWrongPrimitiveRead() {
        ItemStack potion=item(Material.POTION,true); PersistentDataContainer data=potion.getItemMeta().getPersistentDataContainer();
        when(data.has(potionKey,PersistentDataType.BYTE)).thenReturn(false);
        when(data.get(potionKey,PersistentDataType.BYTE)).thenThrow(new IllegalArgumentException("Wrong primitive type"));
        assertFalse(plugin.isVeilPotion(potion));
    }
    @Test void unexpectedPotionMarkerValueIsRejected() {
        ItemStack potion=item(Material.POTION,true); when(potion.getItemMeta().getPersistentDataContainer().get(potionKey,PersistentDataType.BYTE)).thenReturn((byte)2);
        assertFalse(plugin.isVeilPotion(potion));
    }
    @Test void offHandConsumptionUsesSameActivationChecks() {
        PlayerItemConsumeEvent event=new PlayerItemConsumeEvent(player,item(Material.POTION,true),EquipmentSlot.OFF_HAND);
        plugin.consume(event); assertFalse(plugin.isVeiled(player)); drain(); assertTrue(plugin.isVeiled(player)); assertEquals(481_000L,state.get(expires));
    }
    @Test void activeQuitKeepsAbsoluteDeadlineRatherThanPausingIt() {
        active(); quit(); now.addAndGet(120_000); assertEquals(481_000L,state.get(expires)); assertEquals(360_000L,plugin.remainingMillis(player));
    }
    @Test void twoFreshSequentialDrinksGiveSixteenMinutes() {
        drink(); PlayerItemConsumeEvent second=consume(item(Material.POTION,true)); plugin.consume(second); assertFalse(second.isCancelled()); drain();
        assertEquals(960_000L,plugin.remainingMillis(player)); assertEquals(961_000L,state.get(expires));
        assertEquals(19_200,invis.get().getDuration()); verify(player,times(4)).saveData();
    }
    @Test void manySequentialDrinksRemainAdditiveWithoutArbitraryCap() {
        for(int i=0;i<8;i++) drink(); assertEquals(3_840_000L,plugin.remainingMillis(player));
        assertEquals(3_841_000L,state.get(expires)); assertEquals(76_800,invis.get().getDuration());
    }
    @Test void cancelledExtensionPreservesPriorDeadlineAndEffect() {
        active(); PlayerItemConsumeEvent extra=consume(item(Material.POTION,true)); plugin.consume(extra); extra.setCancelled(true); drain();
        assertEquals(481_000L,state.get(expires)); assertTrue(plugin.isVeiled(player)); verify(player,times(1)).saveData();
    }
    @Test void duplicatePendingExtensionCancelsOnlyDuplicate() {
        active(); PlayerItemConsumeEvent first=consume(item(Material.POTION,true)),second=consume(item(Material.POTION,true));
        plugin.consume(first); plugin.consume(second); assertFalse(first.isCancelled()); assertTrue(second.isCancelled()); drain(); assertEquals(961_000L,state.get(expires));
    }
    @Test void attackingEndsEntireStackedTimer() {
        active(); drink(); melee(); assertFalse(plugin.isVeiled(player)); assertTrue(state.isEmpty()); assertNull(invis.get());
    }
    @Test void attackingWhileExtensionPendingPreventsReactivation() {
        active(); plugin.consume(consume(item(Material.POTION,true))); melee(); drain();
        assertFalse(plugin.isVeiled(player)); assertTrue(state.isEmpty()); assertNull(invis.get());
    }
    @Test void stackedDeadlineSurvivesDisableOfflineTimeAndRestore() throws Exception {
        active(); drink(); plugin.onDisable(); assertEquals(961_000L,state.get(expires)); now.addAndGet(240_000); refresh(true);
        assertEquals(720_000L,plugin.remainingMillis(player)); assertEquals(14_400,invis.get().getDuration()); assertEquals(961_000L,state.get(expires));
    }
    @Test void stackedDeadlineSurvivesWorldChangeWithoutAddingTime() {
        active(); drink(); now.addAndGet(240_000); changeWorld(); assertEquals(961_000L,state.get(expires)); assertEquals(720_000L,plugin.remainingMillis(player));
    }
    @Test void extendingExpiredVeilStartsFreshDuration() {
        active(); now.addAndGet(600_000); drink(); assertEquals(480_000L,plugin.remainingMillis(player)); assertEquals(1_081_000L,state.get(expires));
    }
    @Test void unrepresentableExtensionCancelledBeforeNativeConsumption() {
        active(); state.put(expires,Long.MAX_VALUE); invis.set(new PotionEffect(PotionEffectType.INVISIBILITY,Integer.MAX_VALUE,0,false,false,true));
        PlayerItemConsumeEvent extra=consume(item(Material.POTION,true)); plugin.consume(extra);
        assertTrue(extra.isCancelled()); assertTrue(tasks.isEmpty()); assertEquals(Long.MAX_VALUE,state.get(expires));
        assertTrue(plugin.isVeiled(player)); verify(player,never()).saveData();
    }
    @Test void unrepresentableFreshDeadlineCancelledBeforeConsumption() {
        now.set(Long.MAX_VALUE); PlayerItemConsumeEvent event=consume(item(Material.POTION,true)); plugin.consume(event);
        assertTrue(event.isCancelled()); assertTrue(tasks.isEmpty()); assertTrue(state.isEmpty());
    }
    @Test void veryLargeTickDurationDoesNotOverflowLagComparison() throws Exception {
        active(); state.put(expires,Long.MAX_VALUE); invis.set(new PotionEffect(PotionEffectType.INVISIBILITY,Integer.MAX_VALUE,0,false,false,true));
        refresh(false); assertTrue(plugin.isVeiled(player)); verify(player,never()).addPotionEffect(any()); verify(player,never()).removePotionEffect(any());
    }
    @Test void reportedSaveFailureRestoresPriorTimerAndProvenBottleOnly() {
        doNothing().doThrow(new IllegalStateException("simulated save failure")).when(player).saveData();
        active(); ItemStack potion=item(Material.POTION,true); plugin.consume(consume(potion)); assertDoesNotThrow(this::drain);
        assertEquals(481_000L,state.get(expires)); assertTrue(plugin.isVeiled(player)); assertSame(potion,inventorySlots.get(0));
        verify(inventory).setItem(0,potion); verify(player,atLeastOnce()).sendMessage(contains("Saving Veil Potion state failed"));
    }
    @Test void reportedFailureWithUncertainHandNeverBlindlyGrantsBottle() {
        doNothing().doThrow(new IllegalStateException("simulated save failure")).when(player).saveData();
        active(); plugin.consume(consume(item(Material.POTION,true))); ItemStack changed=item(Material.DIAMOND,false); inventorySlots.put(0,changed);
        assertDoesNotThrow(this::drain); assertEquals(481_000L,state.get(expires)); assertSame(changed,inventorySlots.get(0));
        verify(inventory,never()).setItem(anyInt(),any(ItemStack.class)); verify(player).sendMessage(contains("Ask an admin to check the bottle and timer"));
    }
    @Test void failedExtensionAndFailedPriorEffectRestoreCannotKeepMobIgnore() {
        active(); when(player.addPotionEffect(any(PotionEffect.class))).thenReturn(false); ItemStack potion=item(Material.POTION,true);
        plugin.consume(consume(potion)); drain(); assertFalse(plugin.isVeiled(player)); assertTrue(state.isEmpty()); assertNull(invis.get());
        assertSame(potion,inventorySlots.get(0));
    }
    @Test void noDecrementRollbackStillRequestsSaveOfRestoredTimer() {
        active(); when(player.addPotionEffect(any(PotionEffect.class))).thenReturn(false,true);
        ItemStack potion=item(Material.POTION,true); plugin.consume(consume(potion));
        while(!tasks.isEmpty()) tasks.remove().run();
        assertEquals(481_000L,state.get(expires)); assertSame(potion,inventorySlots.get(0)); verify(player,atLeastOnce()).saveData();
    }
    @Test void statusCeilingDoesNotOverflowLargeStackedDeadline() {
        active(); state.put(expires,Long.MAX_VALUE); invis.set(new PotionEffect(PotionEffectType.INVISIBILITY,Integer.MAX_VALUE,0,false,false,true));
        bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
        plugin.onCommand(player,mock(org.bukkit.command.Command.class),"veil",new String[]{"status"});
        verify(player).sendMessage(contains("Veil active: 9223372036854775 seconds left"));
    }
    @Test void knownPreflightSaveFailureCancelsBeforeBottleIsUsed() {
        doThrow(new IllegalStateException("simulated preflight save failure")).when(player).saveData();
        active(); ItemStack potion=item(Material.POTION,true); PlayerItemConsumeEvent event=consume(potion); plugin.consume(event);
        assertTrue(event.isCancelled()); assertTrue(tasks.isEmpty()); assertEquals(481_000L,state.get(expires)); assertSame(potion,inventorySlots.get(0));
        verify(player,never()).addPotionEffect(any());
    }
    @Test void hugeDeadlineRefreshesEffectChunkBeforeItRunsOut() throws Exception {
        active(); state.put(expires,Long.MAX_VALUE); invis.set(new PotionEffect(PotionEffectType.INVISIBILITY,Integer.MAX_VALUE-1_201,0,false,false,true));
        refresh(false); assertEquals(Integer.MAX_VALUE,invis.get().getDuration()); assertEquals(Long.MAX_VALUE,state.get(expires));
        verify(player).addPotionEffect(any());
    }
    @Test void offHandFailureRestoresOnlyProvenOffHandBottle() {
        active(); when(player.addPotionEffect(any(PotionEffect.class))).thenReturn(false,true);
        ItemStack potion=item(Material.POTION,true); inventorySlots.put(40,potion); ItemStack mainHand=item(Material.DIAMOND,false); inventorySlots.put(0,mainHand);
        plugin.consume(new PlayerItemConsumeEvent(player,potion,EquipmentSlot.OFF_HAND)); drain(); assertEquals(481_000L,state.get(expires));
        assertSame(potion,inventorySlots.get(40)); assertSame(mainHand,inventorySlots.get(0));
        verify(inventory).setItem(40,potion); verify(inventory,never()).setItem(eq(0),any(ItemStack.class));
    }
    @Test void clockOverflowAfterConsumptionRestoresBottleWithoutGrantingTime() {
        ItemStack potion=item(Material.POTION,true); plugin.consume(consume(potion)); now.set(Long.MAX_VALUE); drain();
        assertTrue(state.isEmpty()); assertSame(potion,inventorySlots.get(0)); assertFalse(plugin.isVeiled(player));
    }
}
