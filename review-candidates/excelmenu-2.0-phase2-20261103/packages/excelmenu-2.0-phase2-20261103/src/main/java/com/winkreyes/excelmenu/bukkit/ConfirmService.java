// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.platform.Detection;
import com.winkreyes.excelmenu.core.screens.ConfirmTargets;
import com.winkreyes.excelmenu.core.screens.TeleportConfirmationGate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The teleport-confirmation gate as a core service (DESIGN-2.0
 * §2.4 forced change 2): the four retired teleport_confirm_*
 * definition ids route here from MenuService.openMenu — the single
 * choke point every renderer passes through — and render as the
 * platform's own prompt: Bedrock gets the modal form (the
 * ExcelBedrockMenu gate screen, wording unchanged), Java gets a
 * chest built from the retired definition's own items.
 *
 * The discipline is the gate's: a prompt is single-use, bound to
 * the player instance, the world and a 60-second TTL; opening any
 * menu invalidates it (MenuService calls invalidate on open, as
 * BedrockMenu did); a closed modal cancels it; a failed modal send
 * cancels it and tells the player they have NOT been teleported —
 * a failed confirmation transport never performs an unconfirmed
 * teleport.
 */
final class ConfirmService implements Listener {

    static final String SEND_FAILED_MESSAGE =
            "§eThe teleport confirmation could not open. You have not been teleported. Please try again.";
    static final String EXPIRED_MESSAGE =
            "§eThat teleport confirmation has expired. Nothing happened.";

    private final JavaPlugin plugin;
    private final PlatformBridge bridge;
    private final SpawnTeleporter spawnTeleporter;
    private final TeleportConfirmationGate gate = new TeleportConfirmationGate();

    ConfirmService(JavaPlugin plugin, PlatformBridge bridge,
                          SpawnTeleporter spawnTeleporter) {
        this.plugin = plugin;
        this.bridge = bridge;
        this.spawnTeleporter = spawnTeleporter;
    }

    /** True when a menu id is one of the retired confirm definitions. */
    boolean handles(String menuId) {
        return ConfirmTargets.isConfirmation(menuId);
    }

    /** Open the confirmation for a retired definition id. */
    void open(Player player, String menuId) {
        ConfirmTargets.Target target = ConfirmTargets.byDefinitionId(menuId);
        if (target == null) return;
        if (bridge.detect(player) == Detection.State.BEDROCK) {
            openModal(player, target);
        } else {
            openChest(player, target);
        }
    }

    /** Drop the player's prompt because the menu session advanced. */
    void invalidate(Player player) {
        gate.invalidate(player.getUniqueId());
    }

    void retire() {
        gate.clear();
    }

    // ---------- Bedrock: the modal gate ----------

    private void openModal(Player player, ConfirmTargets.Target target) {
        if (!player.isOnline() || !plugin.isEnabled()) return;
        TeleportConfirmationGate.Ticket ticket = gate.begin(player.getUniqueId(),
                player, player.getWorld().getUID(), System.nanoTime());
        boolean sent;
        try {
            sent = bridge.sendModalForm(player, target.modalTitle(), target.modalContent(),
                    ConfirmTargets.YES_LABEL, ConfirmTargets.NO_LABEL,
                    new PlatformBridge.ModalHandler() {
                        @Override
                        public void onResult(boolean first) {
                            boolean accepted = gate.answer(ticket, player,
                                    player.getWorld().getUID(), System.nanoTime(),
                                    player.isOnline() && plugin.isEnabled(),
                                    TeleportConfirmationGate.confirmChoiceTeleports(first));
                            if (accepted) run(player, target);
                        }

                        @Override
                        public void onClosedOrInvalid() {
                            gate.cancel(ticket);
                        }
                    });
        } catch (Throwable t) {
            plugin.getLogger().warning("ExcelMenu: teleport confirm form failed for "
                    + player.getName() + ": " + t);
            sent = false;
        }
        if (!sent) {
            gate.cancel(ticket);
            if (player.isOnline()) player.sendMessage(SEND_FAILED_MESSAGE);
        }
    }

    // ---------- Java: the definition's own chest ----------

    /** Marks our confirmation chest so clicks route here. */
    private static final class ConfirmHolder implements InventoryHolder {
        final TeleportConfirmationGate.Ticket ticket;
        final ConfirmTargets.Target target;
        Inventory inv;

        ConfirmHolder(TeleportConfirmationGate.Ticket ticket,
                      ConfirmTargets.Target target) {
            this.ticket = ticket;
            this.target = target;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    private void openChest(Player player, ConfirmTargets.Target target) {
        TeleportConfirmationGate.Ticket ticket = gate.begin(player.getUniqueId(),
                player, player.getWorld().getUID(), System.nanoTime());
        ConfirmHolder holder = new ConfirmHolder(ticket, target);
        Inventory inv = Bukkit.createInventory(holder, 9,
                ScreenItems.legacy(target.chestTitle()));
        holder.inv = inv;
        inv.setItem(3, ScreenItems.item(Material.LIME_DYE,
                "&a&l" + ConfirmTargets.YES_LABEL, target.yesLore()));
        inv.setItem(4, ScreenItems.item(Material.valueOf(target.icon()),
                target.infoName(), target.infoLore().toArray(new String[0])));
        inv.setItem(5, ScreenItems.item(Material.RED_DYE,
                "&c&l" + ConfirmTargets.NO_LABEL, ConfirmTargets.CANCEL_LORE));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof ConfirmHolder h)) return;
        e.setCancelled(true); // never let the prompt items be taken out
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() != e.getInventory()) return;
        if (e.getRawSlot() == 3) {
            boolean accepted = gate.answer(h.ticket, p, p.getWorld().getUID(),
                    System.nanoTime(), p.isOnline() && plugin.isEnabled(), true);
            Bukkit.getScheduler().runTask(plugin, () -> {
                p.closeInventory();
                if (accepted) {
                    run(p, h.target);
                } else {
                    p.sendMessage(EXPIRED_MESSAGE);
                }
            });
        } else if (e.getRawSlot() == 5) {
            gate.cancel(h.ticket);
            Bukkit.getScheduler().runTask(plugin, () -> p.closeInventory());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof ConfirmHolder) {
            e.setCancelled(true);
        }
    }

    // ---------- the confirmed action ----------

    private void run(Player player, ConfirmTargets.Target target) {
        switch (target.kind()) {
            case SPAWN_FALLBACK -> spawnTeleporter.teleportToSpawn(player);
            case PLAYER_COMMAND -> player.performCommand(target.command());
        }
    }

    // ---------- gate invalidation (§2.5 discipline) ----------

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        gate.invalidate(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        gate.invalidate(event.getPlayer().getUniqueId());
    }
}
