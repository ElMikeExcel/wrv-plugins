// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.platform.Detection;
import com.winkreyes.excelmenu.core.screens.TeleportCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * "Teleport to a player" — no typing needed (DESIGN-2.0 §2.4: the
 * /tpmenu screen, ported from ExcelBedrockMenu 1.5.29's TeleportMenu
 * as a shared core + thin renderers):
 *
 *   1) Pick a name from a list (Bedrock: form buttons, Java: chest
 *      of heads).
 *   2) Pick a direction: "Teleport to player" (we go to them) or
 *      "Ask player to come to me" (they come to us) — Mike's labels,
 *      Nov 2, 2026. Bedrock: a two-button form titled with the
 *      picked player's name. Java: a small choice chest. Closing
 *      either one sends nothing.
 *   3) The request runs through EssentialsX ("/tpa <name>" or
 *      "/tpahere <name>") AS the picker, so Essentials' cooldowns,
 *      safe-teleport checks, warmups and timeouts all still apply.
 *   4) The target gets a tap-to-answer prompt worded for the
 *      direction (Bedrock: Accept/Decline popup, Java: clickable
 *      [Accept] [Decline] in chat) which runs /tpaccept or /tpdeny.
 *      Essentials answers both request directions with the same pair.
 *
 * The wording lives in TeleportCore (pinned by tests); this class
 * is transport and Bukkit reads only.
 */
final class TeleportScreen implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    private final PlatformBridge bridge;

    TeleportScreen(JavaPlugin plugin, PlatformBridge bridge) {
        this.plugin = plugin;
        this.bridge = bridge;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label,
                             String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
        open((Player) sender);
        return true;
    }

    // ---------- step 1: the player list ----------

    void open(Player player) {
        List<Player> others = new ArrayList<>();
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (o.equals(player)) continue;
            // Hide vanished players
            if (!player.canSee(o)) continue;
            others.add(o);
        }
        others.sort(Comparator.comparing(
                o -> ScreenItems.displayName(o).toLowerCase()));

        if (others.isEmpty()) {
            player.sendMessage("§e" + TeleportCore.NOBODY_MESSAGE);
            return;
        }
        if (bridge.detect(player) == Detection.State.BEDROCK) {
            try {
                if (bedrockList(player, others)) return;
            } catch (Throwable t) {
                plugin.getLogger().warning("ExcelMenu: teleport form failed for "
                        + player.getName() + ": " + t);
            }
        }
        javaList(player, others);
    }

    private boolean bedrockList(Player player, List<Player> others) {
        List<UUID> ids = new ArrayList<>();
        List<String> buttons = new ArrayList<>();
        for (Player o : others) {
            buttons.add(ScreenItems.stripColor(ScreenItems.displayName(o)));
            ids.add(o.getUniqueId());
        }
        return bridge.sendSimpleForm(player, TeleportCore.LIST_TITLE,
                TeleportCore.LIST_BODY, buttons,
                new PlatformBridge.FormHandler() {
                    @Override
                    public void onSelect(int index) {
                        if (index < 0 || index >= ids.size()) return;
                        UUID target = ids.get(index);
                        String targetLabel = buttons.get(index);
                        bedrockChoice(player, target, targetLabel);
                    }

                    @Override
                    public void onClosedOrInvalid() {
                        // Closed = cancelled: nothing is sent.
                    }
                });
    }

    /** Marks our chest GUI so we only react to clicks in it. Keeps
     *  the player list in slot order. */
    private static final class PickHolder implements InventoryHolder {
        final List<UUID> slots = new ArrayList<>();
        Inventory inv;

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    /** Marks our direction-choice chest so we only react to clicks in it. */
    private static final class ChoiceHolder implements InventoryHolder {
        final UUID target;
        Inventory inv;

        ChoiceHolder(UUID target) {
            this.target = target;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    private static final int CHOICE_SLOT_TELEPORT_TO = 2;
    private static final int CHOICE_SLOT_COME_TO_ME = 6;

    private void javaList(Player player, List<Player> others) {
        PickHolder h = new PickHolder();
        int size = Math.min(54, Math.max(9, ((others.size() + 8) / 9) * 9));
        h.inv = Bukkit.createInventory(h, size, TeleportCore.LIST_TITLE);
        for (Player o : others) {
            if (h.slots.size() >= size) break;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta m = (SkullMeta) head.getItemMeta();
            if (m != null) {
                m.setOwningPlayer(o);
                m.displayName(ScreenItems.legacy("&a"
                        + ScreenItems.stripColor(ScreenItems.displayName(o))));
                m.lore(List.of(ScreenItems.legacy("&7" + TeleportCore.HEAD_LORE)));
                head.setItemMeta(m);
            }
            h.inv.setItem(h.slots.size(), head);
            h.slots.add(o.getUniqueId());
        }
        player.openInventory(h.inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof ChoiceHolder h) {
            e.setCancelled(true); // never let the choice items be taken out
            if (!(e.getWhoClicked() instanceof Player p)) return;
            if (e.getClickedInventory() != e.getInventory()) return;
            TeleportCore.Direction direction;
            if (e.getRawSlot() == CHOICE_SLOT_TELEPORT_TO) {
                direction = TeleportCore.Direction.TELEPORT_TO;
            } else if (e.getRawSlot() == CHOICE_SLOT_COME_TO_ME) {
                direction = TeleportCore.Direction.COME_TO_ME;
            } else {
                return;
            }
            UUID target = h.target;
            Bukkit.getScheduler().runTask(plugin, () -> {
                p.closeInventory();
                sendRequest(p, target, direction);
            });
            return;
        }
        if (!(e.getInventory().getHolder() instanceof PickHolder h)) return;
        e.setCancelled(true); // never let heads be taken out
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() != e.getInventory()) return;
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= h.slots.size()) return;
        UUID target = h.slots.get(slot);
        Bukkit.getScheduler().runTask(plugin, () -> {
            p.closeInventory();
            Player to = Bukkit.getPlayer(target);
            if (to == null || !to.isOnline()) {
                // sendRequest owns the "just left the game" message.
                sendRequest(p, target, TeleportCore.Direction.TELEPORT_TO);
                return;
            }
            javaChoice(p, to);
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        Object holder = e.getInventory().getHolder();
        if (holder instanceof PickHolder || holder instanceof ChoiceHolder) {
            e.setCancelled(true);
        }
    }

    // ---------- step 2: pick a direction ----------

    private void bedrockChoice(Player player, UUID target, String targetLabel) {
        if (!player.isOnline()) return;
        boolean sent = false;
        try {
            sent = bridge.sendModalForm(player, targetLabel, TeleportCore.CHOICE_BODY,
                    TeleportCore.LABEL_TELEPORT_TO, TeleportCore.LABEL_COME_TO_ME,
                    new PlatformBridge.ModalHandler() {
                        @Override
                        public void onResult(boolean first) {
                            sendRequest(player, target, first
                                    ? TeleportCore.Direction.TELEPORT_TO
                                    : TeleportCore.Direction.COME_TO_ME);
                        }

                        @Override
                        public void onClosedOrInvalid() {
                            // Closed = cancelled: nothing is sent.
                        }
                    });
        } catch (Throwable t) {
            plugin.getLogger().warning("ExcelMenu: teleport choice form failed for "
                    + player.getName() + ": " + t);
        }
        if (!sent) {
            // Fail soft to the pre-1.5.29 behavior: a plain /tpa request.
            // (A closed form never reaches here — closing calls nothing.)
            sendRequest(player, target, TeleportCore.Direction.TELEPORT_TO);
        }
    }

    private void javaChoice(Player player, Player target) {
        String name = ScreenItems.stripColor(ScreenItems.displayName(target));
        ChoiceHolder h = new ChoiceHolder(target.getUniqueId());
        h.inv = Bukkit.createInventory(h, 9, name);
        h.inv.setItem(CHOICE_SLOT_TELEPORT_TO, ScreenItems.item(Material.ENDER_PEARL,
                "&a" + TeleportCore.LABEL_TELEPORT_TO,
                "&7" + TeleportCore.choiceLoreTo(name)));
        h.inv.setItem(CHOICE_SLOT_COME_TO_ME, ScreenItems.item(Material.ENDER_EYE,
                "&b" + TeleportCore.LABEL_COME_TO_ME,
                "&7" + TeleportCore.choiceLoreHere(name)));
        player.openInventory(h.inv);
    }

    // ---------- step 3: send the request through EssentialsX ----------

    private void sendRequest(Player from, UUID targetId,
                             TeleportCore.Direction direction) {
        Player to = Bukkit.getPlayer(targetId);
        if (!from.isOnline()) return;
        if (to == null || !to.isOnline()) {
            from.sendMessage("§e" + TeleportCore.LEFT_MESSAGE);
            return;
        }
        Plugin ess = Bukkit.getPluginManager().getPlugin("Essentials");
        if (ess == null || !ess.isEnabled()) {
            from.sendMessage("§c" + TeleportCore.ESSENTIALS_MISSING_MESSAGE);
            return;
        }
        // Real username (not nickname) so it always matches. Essentials
        // prints its own messages (incl. cooldown). If the command is
        // rejected we skip the popup.
        boolean ok = from.performCommand(TeleportCore.requestCommand(direction, to.getName()));
        if (!ok) return;
        // Give Essentials a tick to register the request, then show
        // the answer prompt.
        final String fromName = from.getName();
        final UUID fromId = from.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> askTarget(fromId, fromName, targetId, direction), 2L);
    }

    // ---------- step 4: tap-to-answer prompt for the target ----------

    private void askTarget(UUID fromId, String fromName, UUID targetId,
                           TeleportCore.Direction direction) {
        Player to = Bukkit.getPlayer(targetId);
        if (to == null) return;
        Player from = Bukkit.getPlayer(fromId);
        String who = from != null
                ? ScreenItems.stripColor(ScreenItems.displayName(from)) : fromName;
        String prompt = TeleportCore.promptText(direction, who);

        if (bridge.detect(to) == Detection.State.BEDROCK) {
            try {
                boolean sent = bridge.sendModalForm(to, TeleportCore.PROMPT_TITLE,
                        prompt, TeleportCore.ACCEPT_LABEL, TeleportCore.DECLINE_LABEL,
                        new PlatformBridge.ModalHandler() {
                            @Override
                            public void onResult(boolean first) {
                                answer(to, fromId, fromName, first);
                            }

                            @Override
                            public void onClosedOrInvalid() {
                                // Closed = no answer; the request times
                                // out in Essentials, as before.
                            }
                        });
                if (sent) return;
            } catch (Throwable t) {
                plugin.getLogger().warning("ExcelMenu: teleport popup failed for "
                        + to.getName() + ": " + t);
            }
        }
        // Java (or fallback): clickable chat buttons
        Component msg = Component.text(prompt + " ", NamedTextColor.GOLD)
                .append(Component.text("[Accept]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/tpaccept " + fromName))
                        .hoverEvent(HoverEvent.showText(
                                Component.text(TeleportCore.acceptHover(direction)))))
                .append(Component.text(" "))
                .append(Component.text("[Decline]", NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/tpdeny " + fromName))
                        .hoverEvent(HoverEvent.showText(
                                Component.text(TeleportCore.DECLINE_HOVER))));
        to.sendMessage(msg);
    }

    private void answer(Player to, UUID fromId, String fromName, boolean accept) {
        if (!to.isOnline()) return;
        if (Bukkit.getPlayer(fromId) == null) {
            to.sendMessage("§e" + TeleportCore.answererLeftMessage(fromName));
            return;
        }
        to.performCommand((accept ? "tpaccept " : "tpdeny ") + fromName);
    }
}
