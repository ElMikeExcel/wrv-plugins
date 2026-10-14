// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

/**
 * Source-reviewed DeluxeMenus 1.14.1 adapter; exact-binary/runtime acceptance is still required.
 * Cancel the inventory open, not a click: DM 1.14.1's click listener ignores cancellation.
 */
public final class MobileMenuBridge implements Listener {
    static final Set<String> MENUS = Set.of("spawnshop", "spawnshop_food", "spawnshop_building",
        "spawnshop_travel_animals", "spawnshop_enchanting", "spawnshop_potions_protection");
    record Target(Object holder, String menu, Plugin owner, Object contract) {}
    interface Access {
        Target identify(Player player, Inventory inventory) throws ReflectiveOperationException;
        /** Removes only this exact current registered holder, with close=false and executeCloseActions=false. */
        boolean cleanup(Player player, Target target) throws ReflectiveOperationException;
    }
    private record Pending(Player player, Inventory previous, Object uiToken, Target target) {}
    private final WRVShops plugin;
    private final BedrockForms forms;
    private final Access access;
    private final Map<UUID, Pending> pending = new HashMap<>();
    public MobileMenuBridge(WRVShops plugin) { this(plugin, new BedrockForms(plugin), new ReflectiveAccess(plugin)); }
    MobileMenuBridge(WRVShops plugin, BedrockForms forms, Access access) {
        this.plugin=Objects.requireNonNull(plugin);this.forms=Objects.requireNonNull(forms);this.access=Objects.requireNonNull(access);
    }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void opened(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        pending.remove(player.getUniqueId()); // Any later inventory request supersedes the earlier handoff.
        if (!forms.isBedrock(player)) return; // Positive detection only: ordinary Java openings are untouched.
        final Target target;
        try { target=access.identify(player,event.getInventory()); }
        catch (ReflectiveOperationException|RuntimeException|LinkageError failure) {
            plugin.getLogger().warning("Mobile DeluxeMenus interface unavailable: "+failure.getClass().getSimpleName());return;
        }
        if(target==null||!MENUS.contains(target.menu()))return;
        // Paper closes the previous container before this event. Cancellation prevents its open-screen packet.
        Inventory previous=player.getOpenInventory().getTopInventory();
        event.setCancelled(true);
        Pending request=new Pending(player,previous,plugin.getUi()==null?null:plugin.getUi().sessionToken(player.getUniqueId()),target);pending.put(player.getUniqueId(),request);
        plugin.getServer().getScheduler().runTask(plugin,()->finish(request));
    }
    private void finish(Pending request) {
        Player player=request.player();UUID id=player.getUniqueId();
        final boolean cleaned;
        try { cleaned=access.cleanup(player,request.target()); }
        catch(ReflectiveOperationException|RuntimeException|LinkageError failure){
            pending.remove(id,request);
            plugin.getLogger().warning("Cancelled mobile menu holder could not be safely cleaned: "+failure.getClass().getSimpleName());
            if(player.isOnline())player.sendMessage(ChatColor.RED+"The mobile shop could not open safely. Please try /shop later.");return;
        }
        boolean current=pending.remove(id,request);
        if(!current||!plugin.isEnabled()||!player.isOnline()||!request.target().owner().isEnabled()
            ||player.getOpenInventory().getTopInventory()!=request.previous()
            ||(plugin.getUi()!=null&&plugin.getUi().sessionToken(id)!=request.uiToken()))return;
        if(!cleaned){player.sendMessage(ChatColor.RED+"The mobile shop handoff was cancelled safely. Please reopen /shop.");return;}
        if(plugin.getUi()==null){player.sendMessage(ChatColor.RED+"Mobile shops are unavailable. No purchase was started.");return;}
        plugin.getUi().showMobileMenu(player,request.target().menu());
    }
    @EventHandler public void closed(InventoryCloseEvent event){
        Pending request=pending.get(event.getPlayer().getUniqueId());
        if(request!=null&&event.getInventory()==request.previous())pending.remove(event.getPlayer().getUniqueId(),request);
    }
    @EventHandler public void quit(PlayerQuitEvent event){pending.remove(event.getPlayer().getUniqueId());}
    @EventHandler public void changedWorld(PlayerChangedWorldEvent event){pending.remove(event.getPlayer().getUniqueId());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void teleport(PlayerTeleportEvent event){pending.remove(event.getPlayer().getUniqueId());}
    public void closeAll(){
        for(Pending request:List.copyOf(pending.values())){
            try{access.cleanup(request.player(),request.target());}
            catch(ReflectiveOperationException|RuntimeException|LinkageError failure){plugin.getLogger().warning("Mobile holder shutdown cleanup unavailable.");}
        }
        pending.clear();
    }
    private static final class ReflectiveAccess implements Access {
        private record Contract(Class<?> holder,Method viewer,Method name,Method inventory,Method registered,Method close){}
        private final WRVShops plugin;
        private ReflectiveAccess(WRVShops plugin){this.plugin=plugin;}
        public Target identify(Player player,Inventory inventory)throws ReflectiveOperationException{
            Plugin dm=plugin.getServer().getPluginManager().getPlugin("DeluxeMenus");
            if(dm==null||!dm.isEnabled()||inventory.getHolder()==null)return null;
            ClassLoader loader=dm.getClass().getClassLoader();
            Class<?> holder=Class.forName("com.extendedclip.deluxemenus.menu.MenuHolder",false,loader);
            // Never trust an inventory title, a matching class name in another plugin, or a player-supplied route.
            if(holder.getClassLoader()!=loader||inventory.getHolder().getClass()!=holder)return null;
            Object object=inventory.getHolder();
            Method viewer=holder.getMethod("getViewer"),name=holder.getMethod("getMenuName"),owned=holder.getMethod("getInventory");
            if(viewer.invoke(object)!=player||owned.invoke(object)!=inventory)return null;
            Object value=name.invoke(object);
            if(!(value instanceof String text)||!MENUS.contains(text))return null;
            try{
                Class<?> menu=Class.forName("com.extendedclip.deluxemenus.menu.Menu",false,loader);
                Class<?> owner=Class.forName("com.extendedclip.deluxemenus.DeluxeMenus",false,loader);
                if(!owner.isInstance(dm)||menu.getClassLoader()!=loader)throw new NoSuchMethodException("Untrusted DeluxeMenus owner");
                Contract contract=new Contract(holder,viewer,name,owned,menu.getMethod("getMenuHolder",Player.class),
                    menu.getMethod("closeMenu",owner,Player.class,boolean.class,boolean.class));
                if(!Modifier.isStatic(contract.registered().getModifiers())||!Modifier.isStatic(contract.close().getModifiers()))
                    throw new NoSuchMethodException("Expected DeluxeMenus static public API");
                return new Target(object,text,dm,contract);
            }catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){
                // The trusted shop has already been identified: cancel its opening even if cleanup API probing fails.
                return new Target(object,text,dm,unsupported);
            }
        }
        public boolean cleanup(Player player,Target target)throws ReflectiveOperationException{
            if(!(target.contract() instanceof Contract contract))throw new ReflectiveOperationException("Verified shop cleanup interface is unavailable");
            // A changed/reloaded plugin instance cannot authorize cleanup in another instance's registry.
            if(plugin.getServer().getPluginManager().getPlugin("DeluxeMenus")!=target.owner())return false;
            Object registered=contract.registered().invoke(null,player);
            if(!(registered instanceof Optional<?> current)||current.isEmpty()||current.get()!=target.holder())return false;
            contract.close().invoke(null,target.owner(),player,false,false);
            Object after=contract.registered().invoke(null,player);
            return after instanceof Optional<?> remaining&&(remaining.isEmpty()||remaining.get()!=target.holder());
        }
    }
}
