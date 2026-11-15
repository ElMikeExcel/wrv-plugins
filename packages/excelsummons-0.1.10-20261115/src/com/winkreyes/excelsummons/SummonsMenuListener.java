package com.winkreyes.excelsummons;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Menu click wiring for the /summons menu (0.1.7): the menus are
 * control panels, never item sources — every click in one of our
 * views is cancelled, and clicks inside the view itself delegate
 * to SummonsMenus.handleClick (the DummyListener pattern).
 *
 * 0.1.9 adds the Java rename CHAT PROMPT state. SummonsMenus
 * arms a prompt when a pet page's Rename button is tapped; the
 * player's NEXT chat message (inside the timeout) is the new
 * name. That message is cancelled as chat — a pet's new name is
 * never broadcast to the server — and "cancel" is the way out.
 * The prompt is one-shot: use, timeout, or quit clears it, and
 * the timeout speaks up so a late name is never mistaken for a
 * sent one. (AsyncPlayerChatEvent is the seam: deprecated on
 * modern Paper but still fired; the prompt consumes the message
 * before anything else can treat it as chat.)
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class SummonsMenuListener implements Listener {

    /** How long a rename prompt waits for its answer. The
     *  player-facing message says 30 seconds — keep them in step. */
    static final long RENAME_PROMPT_TIMEOUT_MS = 30_000L;

    private final ExcelSummons plugin;

    /** One armed rename prompt: the pet it will rename, the name
     *  it showed when armed (for the cancel message), and the
     *  adopted page to return to. */
    private record PendingRename(UUID entityId, String displayName, int fromPage) {}

    private final Map<UUID, PendingRename> renamePrompts = new ConcurrentHashMap<>();

    public SummonsMenuListener(ExcelSummons plugin) {
        this.plugin = plugin;
    }

    /** Arm a rename prompt for a player (SummonsMenus, on the
     *  pet page's Rename tap). One prompt per player: arming a
     *  second replaces the first, and the first's timeout task
     *  will find a different prompt in the map and leave it. */
    public void beginRenamePrompt(UUID playerId, UUID entityId, String displayName,
                                  int fromPage) {
        PendingRename pending = new PendingRename(entityId, displayName, fromPage);
        renamePrompts.put(playerId, pending);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (renamePrompts.remove(playerId, pending)) {
                Player p = Bukkit.getPlayer(playerId);
                if (p != null && p.isOnline()) {
                    p.sendMessage("That rename prompt expired — open the pet's page "
                            + "to rename it.");
                }
            }
        }, RENAME_PROMPT_TIMEOUT_MS / 50L);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (event.getInventory().getHolder() instanceof SummonsMenus.MainMenu
                || event.getInventory().getHolder() instanceof SummonsMenus.AdoptedMenu
                || event.getInventory().getHolder() instanceof SummonsMenus.ConfirmDismissMenu
                || event.getInventory().getHolder() instanceof SummonsMenus.PetMenu) {
            event.setCancelled(true);
            if (event.getClickedInventory() == event.getInventory()) {
                SummonsMenus.handleClick(plugin, p, event.getInventory().getHolder(),
                        event.getSlot());
            }
        }
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SummonsMenus.MainMenu
                || event.getInventory().getHolder() instanceof SummonsMenus.AdoptedMenu
                || event.getInventory().getHolder() instanceof SummonsMenus.ConfirmDismissMenu
                || event.getInventory().getHolder() instanceof SummonsMenus.PetMenu) {
            event.setCancelled(true);
        }
    }

    /** The rename prompt's answer channel. Fires on the async
     *  chat thread: the state change (consume the prompt, cancel
     *  the message) happens here; everything that touches the
     *  world hops to the main thread. A player with no armed
     *  prompt is untouched — their chat is their chat. */
    @EventHandler
    public void onRenameChat(AsyncPlayerChatEvent event) {
        PendingRename pending = renamePrompts.remove(event.getPlayer().getUniqueId());
        if (pending == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        String text = event.getMessage() == null ? "" : event.getMessage();
        if (text.trim().equalsIgnoreCase("cancel")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                player.sendMessage("Rename cancelled — your " + pending.displayName()
                        + " keeps its name.");
                SummonsMenus.openAdopted(plugin, player, pending.fromPage());
            });
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                SummonsMenus.finishRename(plugin, player, pending.entityId(), text,
                        pending.fromPage());
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        renamePrompts.remove(event.getPlayer().getUniqueId());
    }
}
