package com.winkreyes.bedrockmenu;

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
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.geyser.api.GeyserApi;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * "Teleport to a player" - no typing needed.
 * 1) Player picks a name from a list (Bedrock: form buttons, Java: chest of heads).
 * 2) We run EssentialsX "/tpa <name>" AS that player, so Essentials' cooldowns,
 *    safe-teleport checks, warmups and timeouts all still apply.
 * 3) The target gets a tap-to-answer prompt (Bedrock: Accept/Decline popup,
 *    Java: clickable [Accept] [Decline] in chat) which runs /tpaccept or /tpdeny.
 */
public class TeleportMenu implements CommandExecutor, Listener {

    private final BedrockMenu plugin;

    public TeleportMenu(BedrockMenu plugin) { this.plugin = plugin; }

    /** Marks our chest GUI so we only react to clicks in it. Keeps the player list in slot order. */
    private static final class PickHolder implements InventoryHolder {
        final List<UUID> slots = new ArrayList<>();
        Inventory inv;
        @Override public Inventory getInventory() { return inv; }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Only players can use this."); return true; }
        open(plugin, p);
        return true;
    }

    // ---------- step 1: the player list ----------

    static void open(BedrockMenu plugin, Player player) {
        List<Player> others = new ArrayList<>();
        for (Player o : Bukkit.getOnlinePlayers()) if (!o.equals(player)) others.add(o);
        others.sort((a, b) -> displayName(a).compareToIgnoreCase(displayName(b)));

        if (others.isEmpty()) {
            player.sendMessage(Component.text("Nobody else is online right now. Try again when family joins!", NamedTextColor.YELLOW));
            return;
        }
        if (isBedrock(player)) {
            try { if (bedrockList(plugin, player, others)) return; }
            catch (Throwable t) { plugin.getLogger().warning("Teleport form failed for " + player.getName() + ": " + t); }
        }
        javaList(player, others);
    }

    private static boolean bedrockList(BedrockMenu plugin, Player player, List<Player> others) {
        List<UUID> ids = new ArrayList<>();
        SimpleForm.Builder b = SimpleForm.builder().title("Teleport to a player")
                .content("Tap who you want to visit. They'll get a request to accept.");
        for (Player o : others) { b.button(displayName(o)); ids.add(o.getUniqueId()); }
        b.validResultHandler(r -> {
            int i = r.clickedButtonId();
            if (i < 0 || i >= ids.size()) return;
            UUID target = ids.get(i);
            Bukkit.getScheduler().runTask(plugin, () -> sendRequest(plugin, player, target));
        });
        return GeyserApi.api().sendForm(player.getUniqueId(), b.build());
    }

    private static void javaList(Player player, List<Player> others) {
        PickHolder h = new PickHolder();
        int size = Math.min(54, Math.max(9, ((others.size() + 8) / 9) * 9));
        h.inv = Bukkit.createInventory(h, size, Component.text("Teleport to a player"));
        for (Player o : others) {
            if (h.slots.size() >= size) break;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta m = (SkullMeta) head.getItemMeta();
            m.setOwningPlayer(o);
            m.displayName(Component.text(displayName(o), NamedTextColor.GREEN));
            m.lore(List.of(Component.text("Click to ask to teleport", NamedTextColor.GRAY)));
            head.setItemMeta(m);
            h.inv.setItem(h.slots.size(), head);
            h.slots.add(o.getUniqueId());
        }
        player.openInventory(h.inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof PickHolder h)) return;
        e.setCancelled(true); // never let heads be taken out
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() != e.getInventory()) return;
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= h.slots.size()) return;
        UUID target = h.slots.get(slot);
        Bukkit.getScheduler().runTask(plugin, () -> { p.closeInventory(); sendRequest(plugin, p, target); });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof PickHolder) e.setCancelled(true);
    }

    // ---------- step 2: send the request through EssentialsX ----------

    private static void sendRequest(BedrockMenu plugin, Player from, UUID targetId) {
        Player to = Bukkit.getPlayer(targetId);
        if (!from.isOnline()) return;
        if (to == null || !to.isOnline()) {
            from.sendMessage(Component.text("Looks like they just left the game. Try someone else!", NamedTextColor.YELLOW));
            return;
        }
        Plugin ess = Bukkit.getPluginManager().getPlugin("Essentials");
        if (ess == null || !ess.isEnabled()) {
            from.sendMessage(Component.text("Teleport requests aren't working right now. Ask an adult to check the server.", NamedTextColor.RED));
            return;
        }
        // Real username (not nickname) so it always matches. Essentials prints its own
        // messages (incl. cooldown). If the command is rejected we skip the popup.
        boolean ok = from.performCommand("tpa " + to.getName());
        if (!ok) return;
        // Give Essentials a tick to register the request, then show the answer prompt.
        Bukkit.getScheduler().runTaskLater(plugin, () -> askTarget(plugin, from.getUniqueId(), from.getName(), targetId), 2L);
    }

    // ---------- step 3: tap-to-answer prompt for the target ----------

    private static void askTarget(BedrockMenu plugin, UUID fromId, String fromName, UUID targetId) {
        Player to = Bukkit.getPlayer(targetId);
        if (to == null) return;
        Player from = Bukkit.getPlayer(fromId);
        String who = from != null ? displayName(from) : fromName;

        if (isBedrock(to)) {
            try {
                ModalForm form = ModalForm.builder()
                        .title("Teleport request")
                        .content(who + " wants to teleport to you.")
                        .button1("Accept").button2("Decline")
                        .validResultHandler(r -> Bukkit.getScheduler().runTask(plugin,
                                () -> answer(to, fromId, fromName, r.clickedFirst())))
                        .build();
                if (GeyserApi.api().sendForm(targetId, form)) return;
            } catch (Throwable t) {
                plugin.getLogger().warning("Teleport popup failed for " + to.getName() + ": " + t);
            }
        }
        // Java (or fallback): clickable chat buttons
        to.sendMessage(Component.text(who + " wants to teleport to you. ", NamedTextColor.GOLD)
                .append(Component.text("[Accept]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/tpaccept " + fromName))
                        .hoverEvent(HoverEvent.showText(Component.text("Let them come to you"))))
                .append(Component.text(" "))
                .append(Component.text("[Decline]", NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/tpdeny " + fromName))
                        .hoverEvent(HoverEvent.showText(Component.text("Say no thanks")))));
    }

    private static void answer(Player to, UUID fromId, String fromName, boolean accept) {
        if (!to.isOnline()) return;
        if (Bukkit.getPlayer(fromId) == null) {
            to.sendMessage(Component.text(fromName + " left the game, so there's nothing to answer.", NamedTextColor.YELLOW));
            return;
        }
        to.performCommand((accept ? "tpaccept " : "tpdeny ") + fromName);
    }

    // ---------- helpers ----------

    /** Shows EssentialsX nicknames (e.g. "Grandma") when set; Paper's display name carries them. */
    private static String displayName(Player p) {
        String s = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(p.displayName());
        return (s == null || s.isBlank()) ? p.getName() : s;
    }

    private static boolean isBedrock(Player p) {
        Plugin g = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
        if (g == null || !g.isEnabled()) return false;
        try { return GeyserApi.api().isBedrockPlayer(p.getUniqueId()); } catch (Throwable t) { return false; }
    }
}
