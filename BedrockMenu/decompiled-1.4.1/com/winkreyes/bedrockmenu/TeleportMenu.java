/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.ChatColor
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.md_5.bungee.api.chat.ClickEvent
 *  net.md_5.bungee.api.chat.ClickEvent$Action
 *  net.md_5.bungee.api.chat.HoverEvent
 *  net.md_5.bungee.api.chat.HoverEvent$Action
 *  net.md_5.bungee.api.chat.TextComponent
 *  net.md_5.bungee.api.chat.hover.content.Content
 *  net.md_5.bungee.api.chat.hover.content.Text
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 *  org.bukkit.plugin.Plugin
 */
package com.winkreyes.bedrockmenu;

import com.winkreyes.bedrockmenu.BedrockMenu;
import com.winkreyes.bedrockmenu.GeyserForms;
import java.lang.invoke.CallSite;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Content;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

public class TeleportMenu
implements CommandExecutor,
Listener {
    private final BedrockMenu plugin;

    public TeleportMenu(BedrockMenu bedrockMenu) {
        this.plugin = bedrockMenu;
    }

    public boolean onCommand(CommandSender commandSender, Command command, String string, String[] stringArray) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("Only players can use this.");
            return true;
        }
        TeleportMenu.open(this.plugin, (Player)commandSender);
        return true;
    }

    static void open(BedrockMenu bedrockMenu, Player player2) {
        ArrayList<Player> arrayList = new ArrayList<Player>();
        for (Player player3 : Bukkit.getOnlinePlayers()) {
            if (player3.equals((Object)player2) || !player2.canSee(player3)) continue;
            arrayList.add(player3);
        }
        arrayList.sort(Comparator.comparing(player -> TeleportMenu.displayName(player).toLowerCase()));
        if (arrayList.isEmpty()) {
            player2.sendMessage(String.valueOf(ChatColor.YELLOW) + "Nobody else is online right now. Try again when family joins!");
            return;
        }
        if (GeyserForms.isBedrockPlayer(player2, bedrockMenu.getLogger())) {
            try {
                if (TeleportMenu.bedrockList(bedrockMenu, player2, arrayList)) {
                    return;
                }
            }
            catch (Throwable throwable) {
                bedrockMenu.getLogger().warning("Teleport form failed for " + player2.getName() + ": " + String.valueOf(throwable));
            }
        }
        TeleportMenu.javaList(player2, arrayList);
    }

    private static boolean bedrockList(BedrockMenu bedrockMenu, Player player, List<Player> list) {
        ArrayList<UUID> arrayList = new ArrayList<UUID>();
        String[] stringArray = new String[list.size()];
        for (int i = 0; i < list.size(); ++i) {
            Player player2 = list.get(i);
            stringArray[i] = TeleportMenu.stripColor(TeleportMenu.displayName(player2));
            arrayList.add(player2.getUniqueId());
        }
        return GeyserForms.sendSimpleForm(player, "Teleport to a player", "Tap who you want to visit. They'll get a request to accept.", stringArray, n -> {
            if (n < 0 || n >= arrayList.size()) {
                return;
            }
            UUID uUID = (UUID)arrayList.get(n);
            Bukkit.getScheduler().runTask((Plugin)bedrockMenu, () -> TeleportMenu.sendRequest(bedrockMenu, player, uUID));
        }, bedrockMenu.getLogger());
    }

    private static void javaList(Player player, List<Player> list) {
        PickHolder pickHolder = new PickHolder();
        int n = Math.min(54, Math.max(9, (list.size() + 8) / 9 * 9));
        pickHolder.inv = Bukkit.createInventory((InventoryHolder)pickHolder, (int)n, (String)"Teleport to a player");
        for (Player player2 : list) {
            if (pickHolder.slots.size() >= n) break;
            ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta)itemStack.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer((OfflinePlayer)player2);
                skullMeta.setDisplayName(String.valueOf(ChatColor.GREEN) + TeleportMenu.stripColor(TeleportMenu.displayName(player2)));
                ArrayList<CallSite> arrayList = new ArrayList<CallSite>();
                arrayList.add((CallSite)((Object)(String.valueOf(ChatColor.GRAY) + "Click to ask to teleport")));
                skullMeta.setLore(arrayList);
                itemStack.setItemMeta((ItemMeta)skullMeta);
            }
            pickHolder.inv.setItem(pickHolder.slots.size(), itemStack);
            pickHolder.slots.add(player2.getUniqueId());
        }
        player.openInventory(pickHolder.inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent inventoryClickEvent) {
        InventoryHolder inventoryHolder = inventoryClickEvent.getInventory().getHolder();
        if (!(inventoryHolder instanceof PickHolder)) {
            return;
        }
        PickHolder pickHolder = (PickHolder)inventoryHolder;
        inventoryClickEvent.setCancelled(true);
        HumanEntity humanEntity = inventoryClickEvent.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        inventoryHolder = (Player)humanEntity;
        if (inventoryClickEvent.getClickedInventory() != inventoryClickEvent.getInventory()) {
            return;
        }
        int n = inventoryClickEvent.getRawSlot();
        if (n < 0 || n >= pickHolder.slots.size()) {
            return;
        }
        UUID uUID = pickHolder.slots.get(n);
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.lambda$onClick$0((Player)inventoryHolder, uUID));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent inventoryDragEvent) {
        if (inventoryDragEvent.getInventory().getHolder() instanceof PickHolder) {
            inventoryDragEvent.setCancelled(true);
        }
    }

    private static void sendRequest(BedrockMenu bedrockMenu, Player player, UUID uUID) {
        Player player2 = Bukkit.getPlayer((UUID)uUID);
        if (!player.isOnline()) {
            return;
        }
        if (player2 == null || !player2.isOnline()) {
            player.sendMessage(String.valueOf(ChatColor.YELLOW) + "Looks like they just left the game. Try someone else!");
            return;
        }
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Essentials");
        if (plugin == null || !plugin.isEnabled()) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Teleport requests aren't working right now. Ask an adult to check the server.");
            return;
        }
        boolean bl = player.performCommand("tpa " + player2.getName());
        if (!bl) {
            return;
        }
        String string = player.getName();
        UUID uUID2 = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater((Plugin)bedrockMenu, () -> TeleportMenu.askTarget(bedrockMenu, uUID2, string, uUID), 2L);
    }

    private static void askTarget(BedrockMenu bedrockMenu, UUID uUID, String string, UUID uUID2) {
        String string2;
        Player player = Bukkit.getPlayer((UUID)uUID2);
        if (player == null) {
            return;
        }
        Player player2 = Bukkit.getPlayer((UUID)uUID);
        String string3 = string2 = player2 != null ? TeleportMenu.stripColor(TeleportMenu.displayName(player2)) : string;
        if (GeyserForms.isBedrockPlayer(player, bedrockMenu.getLogger())) {
            try {
                boolean bl2 = GeyserForms.sendModalForm(player, "Teleport request", string2 + " wants to teleport to you.", "Accept", "Decline", bl -> Bukkit.getScheduler().runTask((Plugin)bedrockMenu, () -> TeleportMenu.answer(player, uUID, string, bl)), bedrockMenu.getLogger());
                if (bl2) {
                    return;
                }
            }
            catch (Throwable throwable) {
                bedrockMenu.getLogger().warning("Teleport popup failed for " + player.getName() + ": " + String.valueOf(throwable));
            }
        }
        TextComponent textComponent = new TextComponent(string2 + " wants to teleport to you. ");
        textComponent.setColor(ChatColor.GOLD);
        TextComponent textComponent2 = new TextComponent("[Accept]");
        textComponent2.setColor(ChatColor.GREEN);
        textComponent2.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept " + string));
        textComponent2.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Content[]{new Text("Let them come to you")}));
        TextComponent textComponent3 = new TextComponent(" ");
        TextComponent textComponent4 = new TextComponent("[Decline]");
        textComponent4.setColor(ChatColor.RED);
        textComponent4.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny " + string));
        textComponent4.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Content[]{new Text("Say no thanks")}));
        textComponent.addExtra((BaseComponent)textComponent2);
        textComponent.addExtra((BaseComponent)textComponent3);
        textComponent.addExtra((BaseComponent)textComponent4);
        player.spigot().sendMessage((BaseComponent)textComponent);
    }

    private static void answer(Player player, UUID uUID, String string, boolean bl) {
        if (!player.isOnline()) {
            return;
        }
        if (Bukkit.getPlayer((UUID)uUID) == null) {
            player.sendMessage(String.valueOf(ChatColor.YELLOW) + string + " left the game, so there's nothing to answer.");
            return;
        }
        player.performCommand((bl ? "tpaccept " : "tpdeny ") + string);
    }

    private static String displayName(Player player) {
        String string = player.getDisplayName();
        return string == null || string.isEmpty() ? player.getName() : string;
    }

    private static String stripColor(String string) {
        return string == null ? "" : ChatColor.stripColor((String)string);
    }

    private /* synthetic */ void lambda$onClick$0(Player player, UUID uUID) {
        player.closeInventory();
        TeleportMenu.sendRequest(this.plugin, player, uUID);
    }

    private static final class PickHolder
    implements InventoryHolder {
        final List<UUID> slots = new ArrayList<UUID>();
        Inventory inv;

        private PickHolder() {
        }

        public Inventory getInventory() {
            return this.inv;
        }
    }
}

