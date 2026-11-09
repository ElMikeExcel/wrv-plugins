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
    /** 0.2.11 (B-2): how a view was opened. AUTO is the plugin presenting
     * the menu itself (verdict / join); MANUAL is every player-initiated
     * open (command, chest marker, compass). Immunity frames bind to
     * AUTO only — see ReviewFramesPolicy. */
    public enum Origin { AUTO, MANUAL }
    /** Session lifecycle notices for the frames layer. The token identifies
     * one viewing session: paging/refresh reopens inside a session keep it,
     * so a close notice for a replaced view is recognisably stale. */
    public interface SessionListener {
        void opened(UUID owner, Origin origin, long token);
        void closed(UUID owner, Origin origin, long token);
    }
    private final Plugin plugin;
    private final ClaimGateway gateway;
    private final Map<UUID, View> open = new HashMap<>();
    private SessionListener sessionListener;
    private java.util.function.BiConsumer<UUID, List<String>> openObserver;
    private long tokenSequence;
    private static final class View implements InventoryHolder {
        final ClaimSession session;
        final Origin origin;
        final long token;
        Inventory inventory;
        View(ClaimSession session, Origin origin, long token) { this.session=session; this.origin=origin; this.token=token; }
        @Override public Inventory getInventory() { return inventory; }
    }
    public PersonalClaimsUi(Plugin plugin, ClaimGateway gateway) {
        this.plugin=Objects.requireNonNull(plugin); this.gateway=Objects.requireNonNull(gateway);
    }
    public void setSessionListener(SessionListener listener) { this.sessionListener=listener; }
    /** Called after every successful open with the claim IDs that were
     * pending at that open (0.2.11 B-2: any open marks them presented). */
    public void setOpenObserver(java.util.function.BiConsumer<UUID, List<String>> observer) { this.openObserver=observer; }
    /** True while the player's top inventory is the view of a live AUTO session. */
    public boolean isAutoViewOpen(Player player) {
        View view=open.get(player.getUniqueId());
        return view!=null && view.origin==Origin.AUTO && player.getOpenInventory().getTopInventory()==view.inventory;
    }
    /** True while the player's top inventory is one of this UI's own
     * views (either origin). A fresh batch re-presents over our own
     * view - it is not a foreign menu (rig find, boot B2: a player
     * who left the dragon verdict view open was skipped by the
     * wither verdict's present, because the holder guard treated
     * our own view as another menu). */
    public boolean isClaimsViewOpen(Player player) {
        View view=open.get(player.getUniqueId());
        return view!=null && player.getOpenInventory().getTopInventory()==view.inventory;
    }
    private void notifyOpened(View view) { if(sessionListener!=null) sessionListener.opened(view.session.owner(),view.origin,view.token); }
    private void notifyClosed(View view) { if(sessionListener!=null) sessionListener.closed(view.session.owner(),view.origin,view.token); }
    public boolean open(Player player) { return open(player,0,Origin.MANUAL,false); }
    /** 0.2.11 (B-2): the plugin presents the menu itself. Behaviour is the
     * manual open's, byte for byte, apart from the AUTO origin it carries. */
    public boolean openAuto(Player player) { return open(player,0,Origin.AUTO,false); }
    private boolean open(Player player,int page,Origin origin,boolean continuation) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Main thread required");
        if (!player.isOnline()) return false;
        List<ClaimGateway.Pending> pending;
        try { pending=gateway.pending(player.getUniqueId()); }
        catch (RuntimeException unavailable) { player.sendMessage("Your rewards are unavailable right now; claims are retained."); return false; }
        View previous=open.get(player.getUniqueId());
        long token=continuation && previous!=null ? previous.token : ++tokenSequence;
        View view;
        try { view=new View(new ClaimSession(player.getUniqueId(),pending,page),origin,token); }
        catch (RuntimeException malformed) { player.sendMessage("Reward records need review; nothing was claimed."); return false; }
        view.inventory=Bukkit.createInventory(view,54,"Your earned rewards");
        int slot=0;
        for (var entry:view.session.visible()) view.inventory.setItem(slot++,icon(Material.CHEST,entry.label(),"Left-click to claim. Inventory space required."));
        view.inventory.setItem(45,icon(Material.ARROW,"Previous page",""));
        view.inventory.setItem(49,icon(Material.PAPER,"Refresh rewards","Only your own rewards appear here."));
        view.inventory.setItem(53,icon(Material.ARROW,"Next page",""));
        if(!continuation && previous!=null) notifyClosed(previous);
        View old=open.put(player.getUniqueId(),view);
        if(old!=null) old.session.close();
        player.openInventory(view.inventory);
        if(!continuation) notifyOpened(view);
        if(openObserver!=null) openObserver.accept(player.getUniqueId(),pending.stream().map(ClaimGateway.Pending::id).toList());
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
            Bukkit.getScheduler().runTask(plugin,()->{ if(player.isOnline() && open.get(player.getUniqueId())==view) open(player,page,view.origin,true); });
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
            open(player,view.session.page(),view.origin,true);
        });
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void drag(InventoryDragEvent event) {
        if(event.getView().getTopInventory().getHolder() instanceof View) event.setCancelled(true);
    }
    @EventHandler public void close(InventoryCloseEvent event) {
        if(event.getInventory().getHolder() instanceof View view) {
            view.session.close();
            // Notify only when this view was still the live one: a view
            // replaced by a reopen fires its close event during the new
            // open, and that stale close must not end the new session.
            if(open.remove(event.getPlayer().getUniqueId(),view)) notifyClosed(view);
        }
    }
    @EventHandler public void quit(PlayerQuitEvent event) {
        View view=open.remove(event.getPlayer().getUniqueId()); if(view!=null){view.session.close(); notifyClosed(view);}
    }
    public void shutdown() {
        for(var entry:List.copyOf(open.entrySet())) {
            entry.getValue().session.close(); notifyClosed(entry.getValue()); Player player=Bukkit.getPlayer(entry.getKey());
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
