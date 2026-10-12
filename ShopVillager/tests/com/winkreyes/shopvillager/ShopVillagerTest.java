package com.winkreyes.shopvillager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class ShopVillagerTest {
    private ShopVillager plugin;
    private Player player;
    private Villager villager;
    private BukkitScheduler scheduler;
    private ConsoleCommandSender console;
    private YamlConfiguration config;
    private Map<UUID, Long> cooldowns;
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() throws Exception {
        bukkit = mockStatic(Bukkit.class);
        scheduler = mock(BukkitScheduler.class);
        console = mock(ConsoleCommandSender.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(Bukkit::getConsoleSender).thenReturn(console);
        plugin = mock(ShopVillager.class, CALLS_REAL_METHODS);
        config = new YamlConfiguration();
        doReturn(config).when(plugin).getConfig();
        // Mockito bypasses JavaPlugin's server-only constructor and field initializers.
        cooldowns = new HashMap<>();
        Field field = ShopVillager.class.getDeclaredField("last");
        field.setAccessible(true);
        field.set(plugin, cooldowns);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Buyer");
        when(player.isOnline()).thenReturn(true);
        villager = mock(Villager.class);
        when(villager.getScoreboardTags()).thenReturn(Set.of(ShopVillager.TAG));
    }

    @AfterEach
    void tearDown() {
        if (bukkit != null) bukkit.close();
    }

    private PlayerInteractEntityEvent interaction(Entity target, EquipmentSlot hand) {
        PlayerInteractEntityEvent event = mock(PlayerInteractEntityEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getRightClicked()).thenReturn(target);
        when(event.getHand()).thenReturn(hand);
        return event;
    }

    @Test
    void taggedMainHandOpensOriginalMenuAndSuppressesDuplicateEvents() {
        PlayerInteractEntityEvent first = interaction(villager, EquipmentSlot.HAND);
        PlayerInteractEntityEvent duplicate = interaction(villager, EquipmentSlot.HAND);
        plugin.onInteract(first);
        plugin.onInteract(duplicate);
        verify(first).setCancelled(true);
        verify(duplicate).setCancelled(true);
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler, times(1)).runTask(eq(plugin), task.capture());
        task.getValue().run();
        bukkit.verify(() -> Bukkit.dispatchCommand(console, "dm open spawnshop Buyer"));
    }

    @Test
    void offHandCannotOpenOrConsumeCooldown() {
        PlayerInteractEntityEvent event = interaction(villager, EquipmentSlot.OFF_HAND);
        plugin.onInteract(event);
        verify(event).setCancelled(true);
        assertTrue(cooldowns.isEmpty());
        verifyNoInteractions(scheduler);
    }

    @Test
    void ordinaryVillagersKeepNormalInteraction() {
        Villager ordinary = mock(Villager.class);
        when(ordinary.getScoreboardTags()).thenReturn(Set.of());
        PlayerInteractEntityEvent event = interaction(ordinary, EquipmentSlot.HAND);
        plugin.onInteract(event);
        verify(event, never()).setCancelled(anyBoolean());
        verifyNoInteractions(scheduler);
    }

    @Test
    void customOpenCommandStillSubstitutesPlayerName() {
        config.set("open-command", "dm open special_shop %player%");
        plugin.onInteract(interaction(villager, EquipmentSlot.HAND));
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTask(eq(plugin), task.capture());
        task.getValue().run();
        bukkit.verify(() -> Bukkit.dispatchCommand(console, "dm open special_shop Buyer"));
    }

    @Test
    void disconnectBeforeScheduledOpenDoesNotDispatchCommand() {
        plugin.onInteract(interaction(villager, EquipmentSlot.HAND));
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTask(eq(plugin), task.capture());
        when(player.isOnline()).thenReturn(false);
        task.getValue().run();
        bukkit.verify(() -> Bukkit.dispatchCommand(any(), anyString()), never());
    }

    @Test
    void quitClearsOnlyTheDepartingPlayersCooldown() {
        UUID other = UUID.randomUUID();
        cooldowns.put(player.getUniqueId(), 1L);
        cooldowns.put(other, 2L);
        PlayerQuitEvent event = mock(PlayerQuitEvent.class);
        when(event.getPlayer()).thenReturn(player);
        plugin.onQuit(event);
        assertEquals(Map.of(other, 2L), cooldowns);
    }

    @Test
    void disableClearsAllCooldowns() {
        cooldowns.put(player.getUniqueId(), 1L);
        plugin.onDisable();
        assertTrue(cooldowns.isEmpty());
    }

    @Test
    void taggingExistingVillagerAddsInvulnerabilityWithoutChangingAppearance() {
        when(player.getTargetEntity(5)).thenReturn(villager);
        assertTrue(plugin.onCommand(player, mock(Command.class), "shopvillager", new String[]{"tag"}));
        verify(villager).setInvulnerable(true);
        verify(villager).addScoreboardTag(ShopVillager.TAG);
        verify(villager, never()).setProfession(any());
        verify(villager, never()).setAI(anyBoolean());
        verify(villager, never()).setCustomName(anyString());
    }

    @Test
    void preciseInteractionAndMerchantSafetyNetCancelTrades() {
        PlayerInteractAtEntityEvent interact = mock(PlayerInteractAtEntityEvent.class);
        when(interact.getRightClicked()).thenReturn(villager);
        plugin.onInteractAt(interact);
        verify(interact).setCancelled(true);
        MerchantInventory inventory = mock(MerchantInventory.class);
        when(inventory.getHolder()).thenReturn(villager);
        InventoryOpenEvent open = mock(InventoryOpenEvent.class);
        when(open.getInventory()).thenReturn(inventory);
        plugin.onOpen(open);
        verify(open).setCancelled(true);
    }

    @Test
    void administrativePermissionIsPreservedInPluginDescriptor() throws Exception {
        YamlConfiguration descriptor = new YamlConfiguration();
        descriptor.loadFromString(Files.readString(Path.of("plugin.yml")));
        assertEquals("shopvillager.admin", descriptor.getString("commands.shopvillager.permission"));
        assertEquals("op", descriptor.getString("permissions.shopvillager.admin.default"));
        assertEquals("1.0.1", descriptor.getString("version"));
    }
}
