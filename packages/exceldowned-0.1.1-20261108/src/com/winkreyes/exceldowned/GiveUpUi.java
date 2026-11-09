package com.winkreyes.exceldowned;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The give-up confirm UI (0.1.1) — buttons are the house way: a Java
 * player gets an inventory menu, a Bedrock player gets the Cumulus
 * modal form, and if neither can be presented the caller falls back
 * to the 0.1.0 chat arm/confirm.
 *
 * One state, three doors: opening the UI ARMS the session's give-up
 * (DownedSession.armGiveUp — done by the command before calling
 * openConfirm); "Let go" confirms through the session's own
 * confirmGiveUp, so a stale UI can never kill; closing the UI or
 * choosing "Keep fighting" disarms. The lifecycle mirrors the
 * ExcelBedrockMenu TeleportConfirmationGate ticket pattern: a form
 * answer is honored only while its ticket is still the player's
 * current ticket, and every session exit (revive / death / external
 * death / quit) invalidates the UI through invalidate().
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class GiveUpUi implements Listener {

    /** Identity token for one presented Bedrock form (answer must match). */
    private static final class Ticket {
        final UUID playerId;
        Ticket(UUID playerId) { this.playerId = playerId; }
    }

    private final ExcelDowned plugin;
    private final Map<UUID, Ticket> formTickets = new ConcurrentHashMap<>();
    private final Set<UUID> javaOpen = ConcurrentHashMap.newKeySet();

    public GiveUpUi(ExcelDowned plugin) { this.plugin = plugin; }

    // ---------- presentation ----------

    /**
     * Present the confirm UI for an already-armed downed player.
     * Returns true when a UI was presented; false means the caller
     * should use the chat arm/confirm prompt instead.
     */
    public boolean openConfirm(Player player) {
        DownedSession s = plugin.sessionOf(player.getUniqueId());
        if (s == null) return false;
        boolean bedrock = BedrockForms.isBedrockPlayer(player, plugin.getLogger());
        switch (GiveUpRoute.route(bedrock, BedrockForms.isFormApiAvailable())) {
            case BEDROCK_FORM: {
                Ticket ticket = new Ticket(player.getUniqueId());
                formTickets.put(player.getUniqueId(), ticket);
                String body = "Let go and die where you lie — your items drop, "
                        + "no rescue, no undo.\n\nOr keep fighting and hold on: "
                        + "a party member can still revive you.\n\nBleed out in "
                        + ExcelDowned.formatClock(s.remainingMs(System.currentTimeMillis()))
                        + " · you have " + (plugin.giveUpWindowMs / 1000) + "s to choose";
                boolean sent = BedrockForms.sendModalForm(player,
                        "✝ You are DOWN", body,
                        "✝ Let go", "Keep fighting",
                        first -> Bukkit.getScheduler().runTask(plugin,
                                () -> onFormAnswer(player, ticket, first)),
                        () -> Bukkit.getScheduler().runTask(plugin,
                                () -> onFormClosed(player, ticket)),
                        plugin.getLogger());
                if (!sent) {
                    formTickets.remove(player.getUniqueId(), ticket);
                    return false;
                }
                return true;
            }
            case JAVA_MENU: {
                Inventory inv = GiveUpMenu.build();
                javaOpen.add(player.getUniqueId());
                player.openInventory(inv);
                return true;
            }
            default:
                return false;
        }
    }

    // ---------- Bedrock form answers ----------

    private void onFormAnswer(Player player, Ticket ticket, boolean letGo) {
        if (formTickets.get(player.getUniqueId()) != ticket) return; // stale form
        formTickets.remove(player.getUniqueId(), ticket);
        if (!player.isOnline()) return;
        if (letGo) confirmLetGo(player);
        else cancelGiveUp(player, false);
    }

    private void onFormClosed(Player player, Ticket ticket) {
        if (formTickets.get(player.getUniqueId()) != ticket) return;
        formTickets.remove(player.getUniqueId(), ticket);
        cancelGiveUp(player, false); // dismissed without choosing = cancel
    }

    // ---------- Java menu events ----------

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GiveUpHolder)) return;
        event.setCancelled(true); // a menu is not a container
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getClickedInventory() != event.getInventory()) return; // bottom-inventory click
        GiveUpHolder holder = (GiveUpHolder) event.getInventory().getHolder();
        String action = holder.actionAt(event.getSlot());
        if (action == null) return;
        if (GiveUpHolder.ACTION_LET_GO.equals(action)) confirmLetGo(player);
        else if (GiveUpHolder.ACTION_KEEP_FIGHTING.equals(action)) cancelGiveUp(player, true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof GiveUpHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof GiveUpHolder)) return;
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        javaOpen.remove(player.getUniqueId());
        // Closed without a confirmed choice = cancel/disarm. After a
        // "Let go" the session is already consumed and after "Keep
        // fighting" the arm is already cleared, so this is a no-op on
        // those paths and the cancel on the ESC path — exactly the
        // discipline the spec asks for, with no flag to get wrong.
        DownedSession s = plugin.sessionOf(player.getUniqueId());
        if (s != null) s.disarmGiveUp();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        invalidate(event.getPlayer().getUniqueId());
    }

    // ---------- outcomes ----------

    /**
     * "Let go": confirm through the session state machine. A stale UI
     * (revived, bled out, window lapsed) does NOTHING — no death, no
     * error — because confirmGiveUp refuses a lapsed arm and a missing
     * session means the down already resolved another way.
     */
    public void confirmLetGo(Player player) {
        DownedSession s = plugin.sessionOf(player.getUniqueId());
        if (s == null) return; // resolved already (revive/death): silent no-op
        if (s.confirmGiveUp(System.currentTimeMillis(), plugin.giveUpWindowMs)) {
            player.sendMessage(ChatColor.DARK_RED + "✝ You let go.");
            plugin.finalizeDeath(player, DownedSession.Exit.GIVE_UP);
        } else {
            // the arm window lapsed while the UI sat open
            closeJavaMenu(player);
            player.sendMessage(ChatColor.GRAY + "✝ That moment passed — you're still down. "
                    + "/giveup again if you're sure.");
        }
    }

    /** "Keep fighting" / UI dismissed: disarm and stand down. */
    public void cancelGiveUp(Player player, boolean closeMenu) {
        DownedSession s = plugin.sessionOf(player.getUniqueId());
        if (s != null) s.disarmGiveUp();
        if (closeMenu) closeJavaMenu(player);
        if (s != null && player.isOnline()) {
            player.sendMessage(ChatColor.GREEN + "✚ You keep fighting — hold on for rescue.");
        }
    }

    // ---------- lifecycle ----------

    /**
     * Invalidate any open confirm UI for a player whose session just
     * exited (revive, death, external death, quit): drop the form
     * ticket so a late answer is ignored, and close the Java menu.
     */
    public void invalidate(UUID playerId) {
        formTickets.remove(playerId);
        javaOpen.remove(playerId);
        Player p = Bukkit.getPlayer(playerId);
        if (p != null) closeJavaMenu(p);
    }

    /** Per-second hook (state task): a Java menu whose arm lapsed closes. */
    public void tickExpire() {
        if (javaOpen.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (UUID id : Set.copyOf(javaOpen)) {
            Player p = Bukkit.getPlayer(id);
            DownedSession s = plugin.sessionOf(id);
            if (p == null || s == null) { javaOpen.remove(id); continue; }
            if (!s.isGiveUpArmed(now, plugin.giveUpWindowMs) && !s.isGiveUpConfirmed()) {
                closeJavaMenu(p); // the close handler disarms (already lapsed)
                p.sendMessage(ChatColor.GRAY + "✝ That moment passed — you're still down. "
                        + "/giveup again if you're sure.");
            }
        }
    }

    public void shutdown() {
        for (UUID id : Set.copyOf(javaOpen)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) closeJavaMenu(p);
        }
        javaOpen.clear();
        formTickets.clear();
    }

    private void closeJavaMenu(Player player) {
        if (player.getOpenInventory() != null
                && player.getOpenInventory().getTopInventory().getHolder() instanceof GiveUpHolder) {
            player.closeInventory();
        }
        javaOpen.remove(player.getUniqueId());
    }
}
