/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.teamrewards.ui;

import java.util.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

/** Register this listener and an owner-only command; compass integrations call open(player).
 * Virtual chest only. No physical blocks, reward ItemStacks, or unsafe inventory transfer paths. */
public final class PersonalClaimsUi implements Listener, CommandExecutor {
    private final Plugin plugin;
    private final ClaimGateway gateway;
    private final Map<UUID, View> open = new HashMap<>();
    private static final class View implements InventoryHolder {
        final ClaimSession session;
        Inventory inventory;
        View(ClaimSession session) { this.session=session; }
        @Override public Inventory getInventory() { return inventory; }
    }
    public PersonalClaimsUi(Plugin plugin, ClaimGateway gateway) {
        this.plugin=Objects.requireNonNull(plugin); this.gateway=Objects.requireNonNull(gateway);
    }
    public boolean open(Player player) { return open(player,0); }
    private boolean open(Player player,int page) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Main thread required");
        if (!player.isOnline()) return false;
        List<ClaimGateway.Pending> pending;
        try { pending=gateway.pending(player.getUniqueId()); }
        catch (RuntimeException unavailable) { player.sendMessage("Your rewards are unavailable right now; claims are retained."); return false; }
        View view;
        try { view=new View(new ClaimSession(player.getUniqueId(),pending,page)); }
        catch (RuntimeException malformed) { player.sendMessage("Reward records need review; nothing was claimed."); return false; }
        view.inventory=Bukkit.createInventory(view,54,"Your earned rewards");
        int slot=0;
        for (var entry:view.session.visible()) view.inventory.setItem(slot++,icon(Material.CHEST,entry.label(),"Left-click to claim. Inventory space required."));
        view.inventory.setItem(45,icon(Material.ARROW,"Previous page",""));
        view.inventory.setItem(49,icon(Material.PAPER,"Refresh rewards","Only your own rewards appear here."));
        view.inventory.setItem(53,icon(Material.ARROW,"Next page",""));
        View old=open.put(player.getUniqueId(),view);
        if(old!=null) old.session.close();
        player.openInventory(view.inventory);
        return true;
    }
    private static ItemStack icon(Material material,String label,String hint) {
        ItemStack item=new ItemStack(material); ItemMeta meta=item.getItemMeta();
        // Plain text; no legacy formatting or MiniMessage parsing of reward labels.
        meta.displayName(net.kyori.adventure.text.Component.text(label));
        meta.lore(List.of(net.kyori.adventure.text.Component.text(hint)));
        item.setItemMeta(meta); return item;
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof View view)) return;
        boolean previouslyCancelled=event.isCancelled();
        event.setCancelled(true); // Including bottom inventory, shift, hotbar, drop, double-click, outside.
        if(previouslyCancelled || !(event.getWhoClicked() instanceof Player player)) return;
        if(open.get(player.getUniqueId())!=view || !view.session.owner().equals(player.getUniqueId()) || !view.session.isLive()) return;
        if(event.getClick()!=ClickType.LEFT || event.getAction()!=InventoryAction.PICKUP_ALL) return;
        int slot=event.getRawSlot();
        if(slot==45 || slot==49 || slot==53) {
            int page=view.session.page()+(slot==45?-1:slot==53?1:0); view.session.close();
            Bukkit.getScheduler().runTask(plugin,()->{ if(player.isOnline() && open.get(player.getUniqueId())==view) open(player,page); });
            return;
        }
        var request=view.session.request(player.getUniqueId(),slot,true);
        if(request.isEmpty()) return;
        // Inventory API mutations/delivery occur next tick, never inside the click transaction.
        Bukkit.getScheduler().runTask(plugin,()->{
            if(!player.isOnline() || open.get(player.getUniqueId())!=view) return;
            ClaimGateway.Result result;
            try { result=gateway.claim(player.getUniqueId(),request.get()); }
            catch(RuntimeException failure) { result=ClaimGateway.Result.UNAVAILABLE; }
            player.sendMessage(switch(result) {
                case DELIVERED -> "Reward claimed.";
                case FULL_INVENTORY -> "Make room first. Your reward is still pending.";
                case UNSAFE_INVENTORY -> "Close your wand or temporary inventory before claiming. Your reward remains pending.";
                case OFFLINE -> "Your reward remains pending until you are online.";
                case ALREADY_CLAIMED -> "That reward was already claimed.";
                case NOT_OWNER -> "That reward does not belong to you.";
                case PENDING_REVIEW -> "That claim needs review; it will not be sent again automatically.";
                case UNAVAILABLE -> "Claim status is unavailable. No automatic retry was made.";
            });
            open(player,view.session.page());
        });
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void drag(InventoryDragEvent event) {
        if(event.getView().getTopInventory().getHolder() instanceof View) event.setCancelled(true);
    }
    @EventHandler public void close(InventoryCloseEvent event) {
        if(event.getInventory().getHolder() instanceof View view) {
            view.session.close(); open.remove(event.getPlayer().getUniqueId(),view);
        }
    }
    @EventHandler public void quit(PlayerQuitEvent event) {
        View view=open.remove(event.getPlayer().getUniqueId()); if(view!=null)view.session.close();
    }
    public void shutdown() {
        for(var entry:List.copyOf(open.entrySet())) {
            entry.getValue().session.close(); Player player=Bukkit.getPlayer(entry.getKey());
            if(player!=null && player.getOpenInventory().getTopInventory()==entry.getValue().inventory)player.closeInventory();
        }
        open.clear();
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!(sender instanceof Player player)) { sender.sendMessage("Open rewards in game."); return true; }
        if(args.length!=0) { player.sendMessage("This command opens only your own rewards; use /"+label); return true; }
        open(player); return true;
    }
}
