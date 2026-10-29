// Excel signature - WRV maintained source; original authorship and license notices preserved.
// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;

/**
 * 1.3.3 regression suite for the retired DeluxeMenus mobile bridge.
 *
 * The bridge intercepted InventoryOpenEvent for Bedrock players, reflectively
 * identified the old menu plugin's MenuHolder, cancelled the open, and handed
 * off to ShopUi.showMobileMenu. That menu plugin is gone from the server: the
 * replacement (ExcelMenu) renders Bedrock forms natively from the same menu
 * definitions and its buttons dispatch "excelshop browse", so no inventory
 * interception is needed — or wanted. These tests pin the removal: the class
 * is absent, nothing in WRVShops wires it, no production source references the
 * retired plugin, and a retired-menu-shaped inventory open proceeds untouched
 * with no warning. The MobileCatalog flow the bridge used to feed is pinned
 * as SURVIVING (last test) — removal must not tear at it.
 */
class MobileBridgeRemovalTest {

    @Test void bridgeClassAndItsSeamsAreAbsent() {
        assertThrows(ClassNotFoundException.class,
            () -> Class.forName("com.winkreyes.shops.MobileMenuBridge"),
            "MobileMenuBridge must not ship in 1.3.3");
        assertThrows(ClassNotFoundException.class,
            () -> Class.forName("com.winkreyes.shops.MobileMenuBridge$Access"));
        assertThrows(ClassNotFoundException.class,
            () -> Class.forName("com.winkreyes.shops.MobileMenuBridge$Target"));
    }

    @Test void wrvShopsDeclaresNoBridgeField() {
        for (var field : WRVShops.class.getDeclaredFields()) {
            assertFalse(field.getType().getName().contains("MobileMenuBridge"),
                "WRVShops field still references the bridge: " + field.getName());
            assertNotEquals("mobileBridge", field.getName());
        }
    }

    @Test void productionSourcesCarryNoRetiredMenuReference() throws Exception {
        Path root = Files.isDirectory(Path.of("src")) ? Path.of("")
            : Files.isDirectory(Path.of("project", "src")) ? Path.of("project")
            : null;
        assertNotNull(root, "test must run from the project dir (fleet harness does)");
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root.resolve("src"))) {
            walk.filter(p -> p.toString().endsWith(".java")).forEach(files::add);
        }
        files.add(root.resolve("plugin.yml"));
        assertFalse(files.isEmpty());
        for (Path file : files) {
            String text = Files.readString(file);
            assertFalse(text.contains("DeluxeMenus"), file + " still references the retired menu plugin");
            assertFalse(text.contains("MobileMenuBridge"), file + " still references the retired bridge");
            assertFalse(text.contains("Mobile DeluxeMenus interface unavailable"),
                file + " still carries the bridge warning");
        }
    }

    @Test void retiredMenuShapedOpenIsNotInterceptedAndLogsNoWarning() {
        WRVShops plugin = mock(WRVShops.class);
        Server server = mock(Server.class);
        PluginManager plugins = mock(PluginManager.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        Logger logger = Logger.getLogger("MobileBridgeRemovalTest");
        List<LogRecord> records = new ArrayList<>();
        Handler capture = new Handler() {
            @Override public void publish(LogRecord record) { records.add(record); }
            @Override public void flush() { }
            @Override public void close() { }
        };
        capture.setLevel(Level.ALL);
        logger.addHandler(capture);
        logger.setLevel(Level.ALL);
        try {
            when(plugin.getServer()).thenReturn(server);
            when(server.getPluginManager()).thenReturn(plugins);
            when(server.getScheduler()).thenReturn(scheduler);
            when(plugin.getLogger()).thenReturn(logger);

            ShopUi ui = new ShopUi(plugin);
            Player player = mock(Player.class);
            UUID id = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(id);
            when(player.isOnline()).thenReturn(true);
            // A holder shaped like the retired plugin's menu holder: the test
            // cannot (and need not) name its class — with the bridge gone, no
            // ExcelShops listener inspects inventory holders on open at all.
            InventoryHolder holder = mock(InventoryHolder.class);
            Inventory inventory = mock(Inventory.class);
            when(inventory.getHolder()).thenReturn(holder);
            InventoryOpenEvent event = mock(InventoryOpenEvent.class);
            when(event.getPlayer()).thenReturn(player);
            when(event.getInventory()).thenReturn(inventory);

            ui.opened(event);

            verify(event, never()).setCancelled(anyBoolean());
            assertNull(ui.currentKind(id), "the open must not start any shop page");
            assertTrue(records.stream().noneMatch(r -> r.getMessage() != null
                    && r.getMessage().contains("Mobile")),
                "no bridge warning may be logged, got: " + records);
        } finally {
            logger.removeHandler(capture);
        }
    }

    @Test void mobileCatalogEntrySurvivesBridgeRemoval() throws Exception {
        // The flow the bridge used to feed is preserved exactly: MobileCatalog
        // still loads through WRVShops and ShopUi keeps its native entry.
        assertNotNull(Class.forName("com.winkreyes.shops.MobileCatalog"));
        assertNotNull(ShopUi.class.getMethod("showMobileMenu", Player.class, String.class));
        assertNotNull(WRVShops.class.getMethod("getMobileCatalog"));
    }
}
