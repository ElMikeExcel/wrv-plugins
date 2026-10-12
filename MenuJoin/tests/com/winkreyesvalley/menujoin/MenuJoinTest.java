package com.winkreyesvalley.menujoin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

class MenuJoinTest {
    private static final NamespacedKey COMPASS_KEY = new NamespacedKey("menujoin", "menu_compass");
    private static final NamespacedKey DISABLED_KEY = new NamespacedKey("menujoin", "autojoin_disabled");
    private MenuJoin plugin;
    private Player player;
    private PlayerInventory inventory;
    private PersistentDataContainer playerData;
    private BukkitScheduler scheduler;
    private PluginCommand compassCommand;
    private PluginCommand toggleCommand;
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        PluginManager pluginManager = mock(PluginManager.class);
        scheduler = mock(BukkitScheduler.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

        plugin = mock(MenuJoin.class, CALLS_REAL_METHODS);
        doReturn("MenuJoin").when(plugin).getName();
        doReturn(Logger.getAnonymousLogger()).when(plugin).getLogger();
        doReturn(new PluginDescriptionFile("MenuJoin", "1.2.1", MenuJoin.class.getName()))
                .when(plugin).getDescription();
        compassCommand = mock(PluginCommand.class);
        toggleCommand = mock(PluginCommand.class);
        doReturn(compassCommand).when(plugin).getCommand("compass");
        doReturn(toggleCommand).when(plugin).getCommand("menujoin");
        plugin.onEnable();

        player = mock(Player.class);
        inventory = mock(PlayerInventory.class);
        playerData = mock(PersistentDataContainer.class);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getPersistentDataContainer()).thenReturn(playerData);
        when(player.isOnline()).thenReturn(true);
        when(inventory.getContents()).thenReturn(new ItemStack[41]);
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>());
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void givesOneCompassAndPreservesItsPersistentIdentity() {
        try (MockedConstruction<ItemStack> created = mockCompassCreation()) {
            assertTrue(plugin.giveMenuCompass(player));
            assertEquals(1, created.constructed().size());
            ItemStack compass = created.constructed().getFirst();
            verify(inventory).addItem(compass);
            ItemMeta meta = compass.getItemMeta();
            verify(meta).setDisplayName("§6Server Menu");
            verify(meta).setLore(List.of("§7Tap to open the server menu"));
            verify(meta.getPersistentDataContainer()).set(COMPASS_KEY, PersistentDataType.BOOLEAN, true);
            verify(compass).setItemMeta(meta);
            verify(player, never()).getWorld();
        }
    }

    @Test
    void fullInventoryDoesNotCreateGroundDropsEvenWhenRepeated() {
        ItemStack leftover = mock(ItemStack.class);
        HashMap<Integer, ItemStack> leftovers = new HashMap<>();
        leftovers.put(0, leftover);
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(leftovers);
        try (MockedConstruction<ItemStack> ignored = mockCompassCreation()) {
            for (int attempt = 0; attempt < 3; attempt++) {
                assertFalse(plugin.giveMenuCompass(player));
            }
        }
        verify(player, never()).getWorld();
        verify(player, never()).getLocation();
        verify(player, times(3)).sendMessage(contains("inventory is full"));
        verify(inventory, never()).setItem(anyInt(), any(ItemStack.class));
    }

    @Test
    void fullInventoryCommandReportsRecoveryInsteadOfSuccessOrExistingCompass() {
        HashMap<Integer, ItemStack> leftovers = new HashMap<>();
        leftovers.put(0, mock(ItemStack.class));
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(leftovers);
        try (MockedConstruction<ItemStack> ignored = mockCompassCreation()) {
            assertTrue(executor(compassCommand).onCommand(player, compassCommand, "compass", new String[0]));
        }
        verify(player).sendMessage(contains("inventory is full"));
        verify(player, never()).sendMessage(contains("Here's a fresh"));
        verify(player, never()).sendMessage(contains("already have"));
        verify(player, never()).getWorld();
    }

    @Test
    void compassCanBeRecoveredAfterSpaceIsFreed() {
        HashMap<Integer, ItemStack> leftovers = new HashMap<>();
        leftovers.put(0, mock(ItemStack.class));
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(leftovers, new HashMap<>());
        try (MockedConstruction<ItemStack> ignored = mockCompassCreation()) {
            assertFalse(plugin.giveMenuCompass(player));
            assertTrue(plugin.giveMenuCompass(player));
        }
        verify(player, never()).getWorld();
    }

    @Test
    void existingCompassInOffHandPreventsAnotherDelivery() {
        ItemStack[] contents = new ItemStack[41];
        contents[40] = item(Material.COMPASS, true);
        when(inventory.getContents()).thenReturn(contents);
        assertFalse(plugin.giveMenuCompass(player));
        verify(inventory, never()).addItem(any(ItemStack[].class));
    }

    @Test
    void existingCompassOnCursorPreventsAnotherDelivery() {
        ItemStack compass = item(Material.COMPASS, true);
        when(player.getItemOnCursor()).thenReturn(compass);
        assertFalse(plugin.giveMenuCompass(player));
        verify(inventory, never()).addItem(any(ItemStack[].class));
    }

    @Test
    void ordinaryCompassAndUnrelatedMarkedItemAreNotMenuCompasses() {
        ItemStack ordinaryCompass = item(Material.COMPASS, false);
        ItemStack markedStone = item(Material.STONE, true);
        when(inventory.getContents()).thenReturn(new ItemStack[] {null, ordinaryCompass, markedStone});
        assertFalse(plugin.hasMenuCompass(player));
    }

    @Test
    void existingCompassCommandReportsExistingWithoutAdding() {
        ItemStack compass = item(Material.COMPASS, true);
        when(player.getItemOnCursor()).thenReturn(compass);
        assertTrue(executor(compassCommand).onCommand(player, compassCommand, "compass", new String[0]));
        verify(player).sendMessage(contains("already have"));
        verify(inventory, never()).addItem(any(ItemStack[].class));
    }

    @Test
    void deathRemovesOnlyMarkedCompassesFromDropsAndItemsToKeep() {
        ItemStack compass = item(Material.COMPASS, true);
        ItemStack ordinaryCompass = item(Material.COMPASS, false);
        ItemStack stone = item(Material.STONE, false);
        List<ItemStack> drops = new ArrayList<>(List.of(compass, ordinaryCompass, stone));
        List<ItemStack> keep = new ArrayList<>(List.of(compass, stone));
        PlayerDeathEvent event = mock(PlayerDeathEvent.class);
        when(event.getDrops()).thenReturn(drops);
        when(event.getItemsToKeep()).thenReturn(keep);

        plugin.onDeath(event);

        assertEquals(List.of(ordinaryCompass, stone), drops);
        assertEquals(List.of(stone), keep);
    }

    @Test
    void manualMenuCompassDropIsCancelled() {
        PlayerDropItemEvent event = dropEvent(item(Material.COMPASS, true));
        plugin.onDrop(event);
        verify(event).setCancelled(true);
        verify(player).sendMessage(contains("stays with you"));
    }

    @Test
    void ordinaryCompassDropIsUnchanged() {
        PlayerDropItemEvent event = dropEvent(item(Material.COMPASS, false));
        plugin.onDrop(event);
        verify(event, never()).setCancelled(anyBoolean());
    }

    @Test
    void rightClickMenuCompassOpensMenu() {
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        ItemStack compass = item(Material.COMPASS, true);
        when(event.getItem()).thenReturn(compass);
        when(event.getPlayer()).thenReturn(player);
        plugin.onCompassUse(event);
        verify(event).setCancelled(true);
        verify(player).performCommand("menu");
    }

    @Test
    void leftClickDoesNotOpenMenu() {
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_AIR);
        plugin.onCompassUse(event);
        verify(player, never()).performCommand(anyString());
        verify(event, never()).setCancelled(anyBoolean());
    }

    @Test
    void joinRechecksToggleWhenDelayedTaskActuallyRuns() {
        doReturn(false).when(plugin).isDisabled(player);
        PlayerJoinEvent event = mock(PlayerJoinEvent.class);
        when(event.getPlayer()).thenReturn(player);
        plugin.onJoin(event);
        Runnable delayed = scheduledTask(60L);

        doReturn(true).when(plugin).isDisabled(player);
        doReturn(false).when(plugin).giveMenuCompass(player);
        delayed.run();

        verify(plugin).giveMenuCompass(player);
        verify(player, never()).performCommand(anyString());
    }

    @Test
    void enabledJoinStillOpensMenuAfterDelay() {
        doReturn(false).when(plugin).isDisabled(player);
        doReturn(true).when(plugin).giveMenuCompass(player);
        PlayerJoinEvent event = mock(PlayerJoinEvent.class);
        when(event.getPlayer()).thenReturn(player);
        plugin.onJoin(event);
        scheduledTask(60L).run();
        verify(player).performCommand("menu");
    }

    @Test
    void disconnectedPlayerGetsNoDelayedJoinWork() {
        PlayerJoinEvent event = mock(PlayerJoinEvent.class);
        when(event.getPlayer()).thenReturn(player);
        plugin.onJoin(event);
        when(player.isOnline()).thenReturn(false);
        scheduledTask(60L).run();
        verify(plugin, never()).giveMenuCompass(player);
        verify(player, never()).performCommand(anyString());
    }

    @Test
    void respawnWithRetainedCompassDoesNotDuplicateIt() {
        ItemStack compass = item(Material.COMPASS, true);
        when(inventory.getContents()).thenReturn(new ItemStack[] {compass});
        PlayerRespawnEvent event = mock(PlayerRespawnEvent.class);
        when(event.getPlayer()).thenReturn(player);
        plugin.onRespawn(event);
        scheduledTask(1L).run();
        verify(inventory, never()).addItem(any(ItemStack[].class));
    }

    @Test
    void respawnReplacesLostCompassAfterOneTick() {
        PlayerRespawnEvent event = mock(PlayerRespawnEvent.class);
        when(event.getPlayer()).thenReturn(player);
        plugin.onRespawn(event);
        try (MockedConstruction<ItemStack> ignored = mockCompassCreation()) {
            scheduledTask(1L).run();
        }
        verify(inventory).addItem(any(ItemStack[].class));
    }

    @Test
    void toggleWritesAndRemovesExistingPersistentKey() {
        when(playerData.getOrDefault(DISABLED_KEY, PersistentDataType.BOOLEAN, false)).thenReturn(false);
        assertTrue(executor(toggleCommand).onCommand(player, toggleCommand, "menujoin", new String[0]));
        verify(playerData).set(DISABLED_KEY, PersistentDataType.BOOLEAN, true);

        when(playerData.getOrDefault(DISABLED_KEY, PersistentDataType.BOOLEAN, false)).thenReturn(true);
        assertTrue(executor(toggleCommand).onCommand(player, toggleCommand, "menujoin", new String[0]));
        verify(playerData).remove(DISABLED_KEY);
    }

    private CommandExecutor executor(PluginCommand command) {
        ArgumentCaptor<CommandExecutor> executor = ArgumentCaptor.forClass(CommandExecutor.class);
        verify(command).setExecutor(executor.capture());
        return executor.getValue();
    }

    private Runnable scheduledTask(long ticks) {
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTaskLater(eq(plugin), task.capture(), eq(ticks));
        return task.getValue();
    }

    private PlayerDropItemEvent dropEvent(ItemStack stack) {
        Item entity = mock(Item.class);
        when(entity.getItemStack()).thenReturn(stack);
        PlayerDropItemEvent event = mock(PlayerDropItemEvent.class);
        when(event.getItemDrop()).thenReturn(entity);
        when(event.getPlayer()).thenReturn(player);
        return event;
    }

    private ItemStack item(Material material, boolean marked) {
        ItemStack item = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        when(item.getType()).thenReturn(material);
        when(item.hasItemMeta()).thenReturn(true);
        when(item.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(data);
        when(data.getOrDefault(COMPASS_KEY, PersistentDataType.BOOLEAN, false)).thenReturn(marked);
        return item;
    }

    private MockedConstruction<ItemStack> mockCompassCreation() {
        return mockConstruction(ItemStack.class, (item, context) -> {
            assertEquals(List.of(Material.COMPASS), context.arguments());
            ItemMeta meta = mock(ItemMeta.class);
            when(item.getItemMeta()).thenReturn(meta);
            when(meta.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        });
    }
}
