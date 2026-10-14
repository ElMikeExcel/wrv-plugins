// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Authored API seam tests, not a substitute for the exact DeluxeMenus/Paper runtime. */
class MobileMenuBridgeTest {
    private WRVShops plugin;private ShopUi ui;private Player player;private Plugin dm;private BedrockForms forms;
    private MobileMenuBridge.Access access;private MobileMenuBridge bridge;
    private Inventory previous,source;private MobileMenuBridge.Target target;
    private final Deque<Runnable> tasks=new ArrayDeque<>();
    @BeforeEach void setup() throws Exception {
        plugin=mock(WRVShops.class);ui=mock(ShopUi.class);player=mock(Player.class);dm=mock(Plugin.class);
        forms=mock(BedrockForms.class);access=mock(MobileMenuBridge.Access.class);
        Server server=mock(Server.class);BukkitScheduler scheduler=mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);when(plugin.getUi()).thenReturn(ui);when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("MobileMenuBridgeTest"));when(server.getScheduler()).thenReturn(scheduler);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());when(player.isOnline()).thenReturn(true);when(dm.isEnabled()).thenReturn(true);
        previous=mock(Inventory.class);source=mock(Inventory.class);InventoryView view=mock(InventoryView.class);
        when(player.getOpenInventory()).thenReturn(view);when(view.getTopInventory()).thenReturn(previous);
        when(forms.isBedrock(player)).thenReturn(true);target=new MobileMenuBridge.Target(new Object(),"spawnshop",dm,new Object());
        when(access.identify(player,source)).thenReturn(target);when(access.cleanup(player,target)).thenReturn(true);
        when(scheduler.runTask(eq(plugin),any(Runnable.class))).thenAnswer(c->{tasks.add(c.getArgument(1));return mock(BukkitTask.class);});
        bridge=new MobileMenuBridge(plugin,forms,access);
    }
    private InventoryOpenEvent open(Inventory inventory){
        InventoryOpenEvent event=mock(InventoryOpenEvent.class);when(event.getPlayer()).thenReturn(player);when(event.getInventory()).thenReturn(inventory);
        bridge.opened(event);return event;
    }
    private void flush(){while(!tasks.isEmpty())tasks.removeFirst().run();}
    @Test void cancelsGridBeforeAnyFormThenCleansOnlyBoundHolder(){
        InventoryOpenEvent event=open(source);verify(event).setCancelled(true);verify(ui,never()).showMobileMenu(any(),anyString());
        flush();verify(ui).showMobileMenu(player,"spawnshop");
    }
    @Test void javaInventoryAndOtherMenusAreUntouched() throws Exception {
        when(forms.isBedrock(player)).thenReturn(false);InventoryOpenEvent java=open(source);flush();verify(java,never()).setCancelled(anyBoolean());
        when(forms.isBedrock(player)).thenReturn(true);when(access.identify(player,source)).thenReturn(null);
        InventoryOpenEvent other=open(source);flush();verify(other,never()).setCancelled(anyBoolean());verify(ui,never()).showMobileMenu(any(),anyString());
    }
    @ParameterizedTest @ValueSource(strings={"quit","world","teleport","close","offline","disable","view","ui_session"})
    void staleHandoffCleansItsHolderButNeverReopens(String reason) throws Exception {
        open(source);
        switch(reason){
            case "quit"->{PlayerQuitEvent e=mock(PlayerQuitEvent.class);when(e.getPlayer()).thenReturn(player);bridge.quit(e);}
            case "world"->{PlayerChangedWorldEvent e=mock(PlayerChangedWorldEvent.class);when(e.getPlayer()).thenReturn(player);bridge.changedWorld(e);}
            case "teleport"->{PlayerTeleportEvent e=mock(PlayerTeleportEvent.class);when(e.getPlayer()).thenReturn(player);bridge.teleport(e);}
            case "close"->{InventoryCloseEvent e=mock(InventoryCloseEvent.class);when(e.getPlayer()).thenReturn(player);when(e.getInventory()).thenReturn(previous);bridge.closed(e);}
            case "offline"->when(player.isOnline()).thenReturn(false);
            case "disable"->when(plugin.isEnabled()).thenReturn(false);
            case "view"->when(player.getOpenInventory().getTopInventory()).thenReturn(mock(Inventory.class));
            case "ui_session"->when(ui.sessionToken(player.getUniqueId())).thenReturn(new Object());
        }
        flush();verify(access).cleanup(player,target);verify(ui,never()).showMobileMenu(any(),anyString());
    }
    @Test void anotherRegisteredHolderIsNeverRemovedOrFollowed() throws Exception {
        when(access.cleanup(player,target)).thenReturn(false);open(source);flush();verify(ui,never()).showMobileMenu(any(),anyString());
    }
    @Test void cleanupFailureClosesTheAttemptRatherThanOpeningUnreadGrid() throws Exception {
        when(access.cleanup(player,target)).thenThrow(new NoSuchMethodException("fixture"));InventoryOpenEvent event=open(source);flush();
        verify(event).setCancelled(true);verify(ui,never()).showMobileMenu(any(),anyString());verify(player).sendMessage(contains("could not open safely"));
    }
    @Test void supersededPendingEntryCannotReplaceLatestMenu() throws Exception {
        Inventory second=mock(Inventory.class);MobileMenuBridge.Target replacement=new MobileMenuBridge.Target(new Object(),"spawnshop_food",dm,new Object());
        when(access.identify(player,second)).thenReturn(replacement);when(access.cleanup(player,replacement)).thenReturn(true);
        open(source);open(second);flush();verify(ui,never()).showMobileMenu(player,"spawnshop");verify(ui).showMobileMenu(player,"spawnshop_food");
    }
}
