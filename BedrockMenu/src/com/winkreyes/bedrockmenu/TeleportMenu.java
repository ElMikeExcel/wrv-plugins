package com.winkreyes.bedrockmenu;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * "Teleport to a player" - no typing needed.
 * 1) Player picks a name from a list (Bedrock: form buttons, Java: chest of heads).
 * 2) We run EssentialsX "/tpa <name>" AS that player, so Essentials' cooldowns,
 *    safe-teleport checks, warmups and timeouts all still apply.
 * 3) The target gets a tap-to-answer prompt (Bedrock: Accept/Decline popup,
 *    Java: clickable [Accept] [Decline] in chat) which runs /tpaccept or /tpdeny.
 *
 * Ported from Steve's 1.3.0 TeleportMenu; Geyser calls go through the
 * reflection-based GeyserForms helper so there is no compile-time Geyser
 * dependency. Behavior is identical from the player's perspective.
 */
public class TeleportMenu implements CommandExecutor, Listener {

    private final BedrockMenu plugin;

    public TeleportMenu(BedrockMenu plugin) {
        this.plugin = plugin;
    }

    /** Marks our chest GUI so we only react to clicks in it. Keeps the player list in slot order. */
    private static final class PickHolder implements InventoryHolder {
        final List<UUID> slots = new ArrayList<>();
        Inventory inv;
        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
        open(plugin, (Player) sender);
        return true;
    }

    // ---------- step 1: the player list ----------

    static void open(BedrockMenu plugin, Player player) {
        List<Player> others = new ArrayList<>();
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (o.equals(player)) {
                continue;
            }
            // Hide vanished players
            if (!player.canSee(o)) {
                continue;
            }
            others.add(o);
        }
        others.sort(Comparator.comparing(o -> displayName(o).toLowerCase()));

        if (others.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Nobody else is online right now. Try again when family joins!");
            return;
        }
        if (GeyserForms.isBedrockPlayer(player, plugin.getLogger())) {
            try {
                if (bedrockList(plugin, player, others)) {
                    return;
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Teleport form failed for " + player.getName() + ": " + t);
            }
        }
        javaList(player, others);
    }

    private static boolean bedrockList(BedrockMenu plugin, Player player, List<Player> others) {
        List<UUID> ids = new ArrayList<>();
        String[] buttons = new String[others.size()];
        for (int i = 0; i < others.size(); i++) {
            Player o = others.get(i);
            buttons[i] = stripColor(displayName(o));
            ids.add(o.getUniqueId());
        }
        return GeyserForms.sendSimpleForm(player, "Teleport to a player",
                "Tap who you want to visit. They'll get a request to accept.",
                buttons,
                clicked -> {
                    if (clicked < 0 || clicked >= ids.size()) {
                        return;
                    }
                    UUID target = ids.get(clicked);
                    Bukkit.getScheduler().runTask(plugin,
                            () -> sendRequest(plugin, player, target));
                },
                plugin.getLogger());
    }

    private static void javaList(Player player, List<Player> others) {
        PickHolder h = new PickHolder();
        int size = Math.min(54, Math.max(9, ((others.size() + 8) / 9) * 9));
        h.inv = Bukkit.createInventory(h, size, "Teleport to a player");
        for (Player o : others) {
            if (h.slots.size() >= size) {
                break;
            }
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta m = (SkullMeta) head.getItemMeta();
            if (m != null) {
                m.setOwningPlayer(o);
                m.setDisplayName(ChatColor.GREEN + stripColor(displayName(o)));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Click to ask to teleport");
                m.setLore(lore);
                head.setItemMeta(m);
            }
            h.inv.setItem(h.slots.size(), head);
            h.slots.add(o.getUniqueId());
        }
        player.openInventory(h.inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof PickHolder h)) {
            return;
        }
        e.setCancelled(true); // never let heads be taken out
        if (!(e.getWhoClicked() instanceof Player p)) {
            return;
        }
        if (e.getClickedInventory() != e.getInventory()) {
            return;
        }
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= h.slots.size()) {
            return;
        }
        UUID target = h.slots.get(slot);
        Bukkit.getScheduler().runTask(plugin, () -> {
            p.closeInventory();
            sendRequest(plugin, p, target);
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof PickHolder) {
            e.setCancelled(true);
        }
    }

    // ---------- step 2: send the request through EssentialsX ----------

    private static void sendRequest(BedrockMenu plugin, Player from, UUID targetId) {
        Player to = Bukkit.getPlayer(targetId);
        if (!from.isOnline()) {
            return;
        }
        if (to == null || !to.isOnline()) {
            from.sendMessage(ChatColor.YELLOW + "Looks like they just left the game. Try someone else!");
            return;
        }
        Plugin ess = Bukkit.getPluginManager().getPlugin("Essentials");
        if (ess == null || !ess.isEnabled()) {
            from.sendMessage(ChatColor.RED + "Teleport requests aren't working right now. Ask an adult to check the server.");
            return;
        }
        // Real username (not nickname) so it always matches. Essentials prints its own
        // messages (incl. cooldown). If the command is rejected we skip the popup.
        boolean ok = from.performCommand("tpa " + to.getName());
        if (!ok) {
            return;
        }
        // Give Essentials a tick to register the request, then show the answer prompt.
        final String fromName = from.getName();
        final UUID fromId = from.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> askTarget(plugin, fromId, fromName, targetId), 2L);
    }

    // ---------- step 3: tap-to-answer prompt for the target ----------

    private static void askTarget(BedrockMenu plugin, UUID fromId, String fromName, UUID targetId) {
        Player to = Bukkit.getPlayer(targetId);
        if (to == null) {
            return;
        }
        Player from = Bukkit.getPlayer(fromId);
        String who = from != null ? stripColor(displayName(from)) : fromName;

        if (GeyserForms.isBedrockPlayer(to, plugin.getLogger())) {
            try {
                boolean sent = GeyserForms.sendModalForm(to, "Teleport request",
                        who + " wants to teleport to you.",
                        "Accept", "Decline",
                        accept -> Bukkit.getScheduler().runTask(plugin,
                                () -> answer(to, fromId, fromName, accept)),
                        plugin.getLogger());
                if (sent) {
                    return;
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Teleport popup failed for " + to.getName() + ": " + t);
            }
        }
        // Java (or fallback): clickable chat buttons
        TextComponent msg = new TextComponent(who + " wants to teleport to you. ");
        msg.setColor(ChatColor.GOLD);
        TextComponent accept = new TextComponent("[Accept]");
        accept.setColor(ChatColor.GREEN);
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept " + fromName));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Let them come to you")));
        TextComponent space = new TextComponent(" ");
        TextComponent decline = new TextComponent("[Decline]");
        decline.setColor(ChatColor.RED);
        decline.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny " + fromName));
        decline.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Say no thanks")));
        msg.addExtra(accept);
        msg.addExtra(space);
        msg.addExtra(decline);
        to.spigot().sendMessage(msg);
    }

    private static void answer(Player to, UUID fromId, String fromName, boolean accept) {
        if (!to.isOnline()) {
            return;
        }
        if (Bukkit.getPlayer(fromId) == null) {
            to.sendMessage(ChatColor.YELLOW + fromName + " left the game, so there's nothing to answer.");
            return;
        }
        to.performCommand((accept ? "tpaccept " : "tpdeny ") + fromName);
    }

    // ---------- helpers ----------

    /**
     * Shows EssentialsX nicknames when set (Essentials syncs them to the
     * Bukkit display name). Falls back to the plain username.
     */
    private static String displayName(Player p) {
        String s = p.getDisplayName();
        return (s == null || s.isEmpty()) ? p.getName() : s;
    }

    private static String stripColor(String s) {
        return s == null ? "" : ChatColor.stripColor(s);
    }
}
