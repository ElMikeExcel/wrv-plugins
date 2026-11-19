package com.winkreyes.excelmobveil;

import com.winkreyes.excelitems.api.ExcelItemsAPI;
import com.winkreyes.excelitems.api.ItemBehavior;
import com.winkreyes.excelitems.core.BuiltinItems;
import com.winkreyes.excelitems.core.ItemDefinition;
import com.winkreyes.excelitems.core.ItemRegistry;
import io.papermc.paper.registry.RegistryAccess;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.*;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.LongSupplier;
import java.util.logging.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Migration M3 checks (ExcelMobVeil -> ExcelItems, ID 1 veil potion).
 *
 * The fake API below is backed by the REAL registry core
 * (BuiltinItems.registry()) and replicates the ItemFactory read
 * rule — primary mark first, legacy fallback only when no primary
 * mark is present — because ItemFactory itself needs a live
 * ExcelItems plugin instance to run. The dual-stamp mint rule the
 * fake applies in create() is ItemFactory's documented rule, driven
 * by the real ID 1 definition. What these tests pin is MobVeil's
 * side of the contract: full mint delegation to create(1), identify
 * through the API on the drink path, behavior registration for ID 1,
 * and — the M3 gate — veil stacking/time identical for
 * registry-minted and legacy-held potions.
 */
class RegistryMigrationTest {
    static MockedStatic<RegistryAccess> registryAccess;
    static final Map<NamespacedKey, PotionEffectType> effects = new HashMap<>();
    static final NamespacedKey PRIMARY = new NamespacedKey("excelitems", "id");
    static final NamespacedKey LEGACY = new NamespacedKey("excelmobveil", "potion");
    static final NamespacedKey STARTED = new NamespacedKey("excelmobveil", "started_at");
    static final NamespacedKey EXPIRES = new NamespacedKey("excelmobveil", "expires_at");

    /** ExcelItemsAPI test double over the real registry core. */
    static final class FakeApi implements ExcelItemsAPI {
        final ItemRegistry registry = BuiltinItems.registry();
        final List<int[]> createCalls = new ArrayList<>();
        final Map<Integer, ItemBehavior> behaviors = new HashMap<>();
        final Map<ItemStack, Map<NamespacedKey, Object>> marksByStack = new IdentityHashMap<>();

        ItemStack stack(Material material, Map<NamespacedKey, Object> marks, int amount) {
            ItemStack stack = mock(ItemStack.class);
            when(stack.getType()).thenReturn(material);
            when(stack.getAmount()).thenReturn(amount);
            when(stack.clone()).thenReturn(stack);
            when(stack.isSimilar(any(ItemStack.class))).thenAnswer(i -> stack == i.getArgument(0));
            when(stack.hasItemMeta()).thenReturn(true);
            ItemMeta meta = mock(ItemMeta.class);
            when(stack.getItemMeta()).thenReturn(meta);
            PersistentDataContainer data = mock(PersistentDataContainer.class);
            when(meta.getPersistentDataContainer()).thenReturn(data);
            when(data.has(any(NamespacedKey.class), any())).thenAnswer(i -> marks.containsKey(i.getArgument(0)));
            when(data.get(any(NamespacedKey.class), any())).thenAnswer(i -> marks.get(i.getArgument(0)));
            marksByStack.put(stack, marks);
            return stack;
        }

        @Override public ItemStack create(int id) { return create(id, 1); }
        @Override public ItemStack create(int id, int amount) {
            createCalls.add(new int[]{id, amount});
            ItemDefinition def = registry.requireId(id);
            Map<NamespacedKey, Object> marks = new HashMap<>();
            marks.put(PRIMARY, id);
            if (def.hasLegacyMark()) {
                marks.put(new NamespacedKey(def.legacyNamespace(), def.legacyKey()), (byte) 1);
                if (def.legacyLoreKey() != null)
                    marks.put(new NamespacedKey(def.legacyNamespace(), def.legacyLoreKey()), def.loreVersion());
            }
            return stack(Material.valueOf(def.material()), marks, amount);
        }
        @Override public ItemStack create(String key) { return create(registry.requireKey(key).id(), 1); }
        @Override public OptionalInt identify(ItemStack stack) {
            Optional<ItemDefinition> def = identifyDefinition(stack);
            return def.isPresent() ? OptionalInt.of(def.get().id()) : OptionalInt.empty();
        }
        @Override public Optional<ItemDefinition> identifyDefinition(ItemStack stack) {
            if (stack == null || !stack.hasItemMeta()) return Optional.empty();
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) return Optional.empty();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            Integer primary = pdc.has(PRIMARY, PersistentDataType.INTEGER)
                    ? (Integer) pdc.get(PRIMARY, PersistentDataType.INTEGER) : null;
            if (primary != null) return registry.identify(primary, null, null);
            for (ItemDefinition def : registry.all()) {
                if (!def.hasLegacyMark()) continue;
                NamespacedKey legacy = new NamespacedKey(def.legacyNamespace(), def.legacyKey());
                if (pdc.has(legacy, PersistentDataType.BYTE)) return Optional.of(def);
            }
            return Optional.empty();
        }
        @Override public boolean isCustomItem(ItemStack stack) { return identify(stack).isPresent(); }
        @Override public boolean isItem(ItemStack stack, int id) {
            OptionalInt found = identify(stack);
            return found.isPresent() && found.getAsInt() == id;
        }
        @Override public Optional<ItemDefinition> definition(int id) { return registry.byId(id); }
        @Override public Optional<ItemDefinition> definition(String key) { return registry.byKey(key); }
        @Override public Collection<ItemDefinition> definitions() { return registry.all(); }
        @Override public ItemDefinition register(ItemDefinition definition) { return registry.register(definition); }
        @Override public void registerBehavior(int id, ItemBehavior behavior) {
            registry.requireId(id);
            behaviors.put(id, behavior);
        }
        @Override public Optional<ItemBehavior> behavior(int id) { return Optional.ofNullable(behaviors.get(id)); }
        @Override public int nextId() { return registry.nextId(); }
    }

    MockedStatic<Bukkit> bukkit;
    ExcelMobVeil plugin;
    FakeApi api;
    Player player;
    PlayerInventory inventory;
    final UUID id = UUID.randomUUID();
    final AtomicLong now = new AtomicLong(1_000);
    final Map<NamespacedKey, Object> state = new HashMap<>();
    final AtomicReference<PotionEffect> invis = new AtomicReference<>();
    final Deque<Runnable> tasks = new ArrayDeque<>();
    final Map<Integer, ItemStack> inventorySlots = new HashMap<>();
    final Map<UUID, PlayerItemConsumeEvent> pending = new HashMap<>();

    @BeforeAll static void bootstrapEffectRegistry() {
        registryAccess = mockStatic(RegistryAccess.class);
        RegistryAccess access = mock(RegistryAccess.class, invocation -> {
            if (invocation.getMethod().getName().equals("getRegistry")) {
                return mock(Registry.class, call -> {
                    String name = call.getMethod().getName();
                    if (name.equals("get") || name.equals("getOrThrow")) {
                        NamespacedKey key = call.getArgument(0);
                        return effects.computeIfAbsent(key, k -> {
                            PotionEffectType type = mock(PotionEffectType.class);
                            when(type.getKey()).thenReturn(k);
                            when(type.getName()).thenReturn(k.getKey());
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
    @AfterAll static void closeRegistry() { if (registryAccess != null) registryAccess.close(); }

    @BeforeEach void setup() throws Exception {
        api = new FakeApi();
        plugin = mock(ExcelMobVeil.class, CALLS_REAL_METHODS);
        inject("clock", (LongSupplier) now::get);
        inject("potionKey", LEGACY);
        inject("startedKey", STARTED);
        inject("expiresKey", EXPIRES);
        inject("itemsApi", api);
        inject("purchasePlayers", new HashMap<>());
        inject("pendingConsumes", pending);
        inject("ownEffectChanges", new HashSet<>());
        Logger logger = Logger.getAnonymousLogger();
        logger.setLevel(Level.OFF);
        doReturn(logger).when(plugin).getLogger();
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        when(player.isOnline()).thenReturn(true);
        inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getHeldItemSlot()).thenReturn(0);
        when(inventory.getItem(anyInt())).thenAnswer(i -> inventorySlots.get(i.getArgument(0)));
        doAnswer(i -> { inventorySlots.put(i.getArgument(0), i.getArgument(1)); return null; })
                .when(inventory).setItem(anyInt(), any(ItemStack.class));
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        when(player.getPersistentDataContainer()).thenReturn(data);
        when(data.has(any(NamespacedKey.class))).thenAnswer(i -> state.containsKey(i.getArgument(0)));
        when(data.has(any(NamespacedKey.class), eq(PersistentDataType.LONG)))
                .thenAnswer(i -> state.get(i.getArgument(0)) instanceof Long);
        when(data.get(any(NamespacedKey.class), eq(PersistentDataType.LONG))).thenAnswer(i -> {
            Object value = state.get(i.getArgument(0));
            if (value != null && !(value instanceof Long)) throw new IllegalArgumentException("Wrong PDC primitive type");
            return value;
        });
        doAnswer(i -> { state.put(i.getArgument(0), i.getArgument(2)); return null; })
                .when(data).set(any(NamespacedKey.class), eq(PersistentDataType.LONG), anyLong());
        doAnswer(i -> { state.remove(i.getArgument(0)); return null; }).when(data).remove(any(NamespacedKey.class));
        when(player.getPotionEffect(PotionEffectType.INVISIBILITY)).thenAnswer(i -> invis.get());
        when(player.hasPotionEffect(PotionEffectType.INVISIBILITY)).thenAnswer(i -> invis.get() != null);
        when(player.addPotionEffect(any(PotionEffect.class))).thenAnswer(i -> { invis.set(i.getArgument(0)); return true; });
        doAnswer(i -> { invis.set(null); return null; }).when(player).removePotionEffect(PotionEffectType.INVISIBILITY);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(i -> { tasks.add(i.getArgument(1)); return null; });
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(Bukkit::getWorlds).thenReturn(List.of());
        bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(player));
    }
    @AfterEach void teardown() { if (bukkit != null) bukkit.close(); }

    void inject(String name, Object value) throws Exception {
        Field field = ExcelMobVeil.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(plugin, value);
    }
    void drain() {
        while (!tasks.isEmpty()) {
            PlayerItemConsumeEvent drink = pending.get(id);
            if (drink != null && !drink.isCancelled()) {
                int slot = drink.getHand() == EquipmentSlot.OFF_HAND ? 40 : 0;
                if (inventorySlots.get(slot) == drink.getItem())
                    inventorySlots.put(slot, api.stack(Material.GLASS_BOTTLE, new HashMap<>(), 1));
            }
            tasks.remove().run();
        }
    }
    ItemStack marked(Integer primary, boolean legacy) {
        Map<NamespacedKey, Object> marks = new HashMap<>();
        if (primary != null) marks.put(PRIMARY, primary);
        if (legacy) marks.put(LEGACY, (byte) 1);
        return api.stack(Material.POTION, marks, 1);
    }
    ItemStack legacyPotion() { return marked(null, true); }
    void drink(ItemStack item) {
        inventorySlots.put(0, item);
        plugin.consume(new PlayerItemConsumeEvent(player, item, EquipmentSlot.HAND));
        drain();
    }

    // ------------------------------------------------------------ identify

    @Test void identifyPrimaryOnly() {
        assertTrue(plugin.isVeilPotion(marked(1, false)));
    }
    @Test void identifyLegacyOnly() {
        assertTrue(plugin.isVeilPotion(marked(null, true)));
    }
    @Test void identifyDualStamped() {
        assertTrue(plugin.isVeilPotion(marked(1, true)));
    }
    @Test void identifyPlainPotionIsNegative() {
        assertFalse(plugin.isVeilPotion(marked(null, false)));
        assertFalse(plugin.isVeilPotion(null));
    }
    @Test void forgedPrimaryNeverFallsThroughToLegacy() {
        // Primary says deathpotion (3); the legacy veil mark must NOT
        // give the item a second identity.
        assertFalse(plugin.isVeilPotion(marked(3, true)));
    }
    @Test void unknownPrimaryNeverFallsThroughToLegacy() {
        assertFalse(plugin.isVeilPotion(marked(99, true)));
    }
    @Test void otherRegistryItemIsNotVeil() {
        assertFalse(plugin.isVeilPotion(marked(4, false)));
    }
    @Test void splashShapeIsNeverVeilEvenWithPrimaryMark() {
        Map<NamespacedKey, Object> marks = new HashMap<>();
        marks.put(PRIMARY, 1);
        assertFalse(plugin.isVeilPotion(api.stack(Material.SPLASH_POTION, marks, 1)));
    }

    // ---------------------------------------------------------------- mint

    @Test void mintFullyDelegatesToRegistryCreate() {
        ItemStack minted = plugin.createPotion(3);
        assertEquals(1, api.createCalls.size());
        assertArrayEquals(new int[]{1, 3}, api.createCalls.get(0));
        // Dual-stamp preserved: the delegated mint carries BOTH the
        // primary mark and this plugin's legacy mark.
        Map<NamespacedKey, Object> marks = api.marksByStack.get(minted);
        assertEquals(1, marks.get(PRIMARY));
        assertEquals((byte) 1, marks.get(LEGACY));
        assertTrue(plugin.isVeilPotion(minted));
    }
    @Test void mintAmountValidationPrecedesDelegation() {
        assertThrows(IllegalArgumentException.class, () -> plugin.createPotion(0));
        assertThrows(IllegalArgumentException.class, () -> plugin.createPotion(65));
        assertTrue(api.createCalls.isEmpty());
    }

    // ------------------------------------------------------------ behavior

    @Test void veilBehaviorRegisteredForId1() {
        plugin.registerVeilBehavior();
        assertTrue(api.behavior(1).isPresent());
        assertEquals("excelmobveil:veil-drink", api.behavior(1).get().handlerId());
        assertEquals(api.definition(1).orElseThrow().handlerId(), api.behavior(1).get().handlerId());
    }

    // ------------------------------------------- definition of record (ID 1)

    @Test void registryDefinitionMatchesTheLegacyMint() {
        ItemDefinition def = BuiltinItems.VEIL_POTION;
        assertEquals(1, def.id());
        assertEquals("veil_potion", def.key());
        assertEquals("POTION", def.material());
        assertEquals(0x63A8AA, def.potionColorRgb());
        assertEquals("WATER", def.basePotionType());
        assertTrue(def.glint());
        assertEquals("excelmobveil", def.legacyNamespace());
        assertEquals("potion", def.legacyKey());
        assertEquals("excelmobveil:veil-drink", def.handlerId());
        // The construction the registry mints must read exactly like
        // the potion the legacy createPotion built by hand.
        assertEquals(ChatColor.AQUA + "" + ChatColor.BOLD + "Veil Potion",
                ChatColor.translateAlternateColorCodes('&', def.displayName()));
        List<String> legacyLore = List.of(
                ChatColor.GRAY + "Drink: invisible body + standard mobs ignore you.",
                ChatColor.GRAY + "Adds 8 real-time minutes, even while offline.",
                ChatColor.AQUA + "Drink more to stack time; attacking ends it all.",
                ChatColor.YELLOW + "Works in armor. Armor stays visible to players.",
                ChatColor.GRAY + "Farming is safe. Milk and death end all time.",
                ChatColor.DARK_GRAY + "Boss scripts and area attacks can still hurt you.");
        List<String> translated = new ArrayList<>();
        for (String line : def.lore()) translated.add(ChatColor.translateAlternateColorCodes('&', line));
        assertEquals(legacyLore, translated);
    }

    // ------------------------------------------------- M3 gate: stacking

    @Test void legacyDrinkAddsEightMinutes() {
        drink(legacyPotion());
        assertEquals(481_000L, state.get(EXPIRES));
        assertEquals(1_000L, state.get(STARTED));
        assertEquals(480_000L, plugin.remainingMillis(player));
    }
    @Test void registryMintedDrinkAddsEightMinutesIdentically() {
        drink(api.create(1));
        assertEquals(481_000L, state.get(EXPIRES));
        assertEquals(1_000L, state.get(STARTED));
        assertEquals(480_000L, plugin.remainingMillis(player));
    }
    @Test void stackingIsIdenticalAcrossItemSources() {
        // Legacy, then legacy again.
        drink(legacyPotion());
        drink(legacyPotion());
        assertEquals(961_000L, state.get(EXPIRES));
        long legacyStacked = plugin.remainingMillis(player);
        // Reset, then registry-minted twice.
        state.clear();
        invis.set(null);
        drink(api.create(1));
        drink(api.create(1));
        assertEquals(961_000L, state.get(EXPIRES));
        assertEquals(legacyStacked, plugin.remainingMillis(player));
        // Reset, then mixed sources in both orders.
        state.clear();
        invis.set(null);
        drink(legacyPotion());
        drink(api.create(1));
        assertEquals(961_000L, state.get(EXPIRES));
        state.clear();
        invis.set(null);
        drink(api.create(1));
        drink(legacyPotion());
        assertEquals(961_000L, state.get(EXPIRES));
        assertEquals(960_000L, plugin.remainingMillis(player));
    }
}
