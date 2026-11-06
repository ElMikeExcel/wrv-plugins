package com.winkreyes.excelcosmetics;

import com.winkreyes.excelcosmetics.core.Kind;
import com.winkreyes.excelcosmetics.core.PlayerState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/** /cosmetics — the picker. One command, two doors: Bedrock
 *  players get a Cumulus form, Java players the inventory GUI,
 *  and the list/equip/off subcommands work as plain text for
 *  everyone (also the fallback if a form ever fails to send). */
final class CosmeticsCommand implements CommandExecutor {

    private static final TextColor GRAY = TextColor.color(0xBBBBBB);
    private static final TextColor GREEN = TextColor.color(0x7DFF9E);
    private static final TextColor RED = TextColor.color(0xFF6B6B);

    private final ExcelCosmetics plugin;

    CosmeticsCommand(ExcelCosmetics plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cosmetics are worn by players — run this in game.");
            return true;
        }
        if (!plugin.masterEnabled()) {
            player.sendMessage(Component.text("Cosmetics are switched off right now.").color(RED));
            return true;
        }
        if (args.length == 0) {
            openPicker(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "list" -> list(player);
            case "equip" -> equip(player, args);
            case "off" -> off(player, args);
            default -> player.sendMessage(Component.text(
                    "Usage: /cosmetics [list | equip <color|effect|set> <id> | off]").color(GRAY));
        }
        return true;
    }

    private void openPicker(Player player) {
        List<PickerModel.Entry> entries = plugin.pickerEntries(player.getUniqueId());
        Boolean bedrock = BedrockPicker.bedrockOrNull(player);
        if (Boolean.TRUE.equals(bedrock) && !entries.isEmpty()) {
            List<String> labels = new ArrayList<>();
            for (PickerModel.Entry e : entries) {
                labels.add(e.title() + (e.equipped() ? " (equipped)" : "") + " — " + e.detail());
            }
            boolean sent = BedrockPicker.sendPicker(player, "Cosmetics",
                    "Your cosmetics. Tap one to equip it.", labels,
                    index -> new BukkitRunnable() {
                        @Override public void run() {
                            if (index >= 0 && index < entries.size()) {
                                PickerModel.Entry e = entries.get(index);
                                if (e.equippable()) {
                                    plugin.equipAndSave(player, e.kind(), e.id());
                                } else {
                                    player.sendMessage(Component.text(
                                            "The Emote Pack is always on while you hold it.").color(GRAY));
                                }
                            }
                        }
                    }.runTask(plugin),
                    plugin.getLogger());
            if (sent) return;
            // fall through to the GUI door
        }
        if (entries.isEmpty() && Boolean.TRUE.equals(bedrock)) {
            player.sendMessage(Component.text(
                    "You don't have any cosmetics yet — they are sold at the spawn shop.").color(GRAY));
            return;
        }
        PickerGui.open(plugin, player, entries);
    }

    private void list(Player player) {
        List<PickerModel.Entry> entries = plugin.pickerEntries(player.getUniqueId());
        if (entries.isEmpty()) {
            player.sendMessage(Component.text(
                    "You don't have any cosmetics yet — they are sold at the spawn shop.").color(GRAY));
            return;
        }
        player.sendMessage(Component.text("Your cosmetics:").color(GREEN));
        for (PickerModel.Entry e : entries) {
            player.sendMessage(Component.text(" • " + e.title() + " — " + e.detail()
                    + (e.equipped() ? " (equipped)" : "")).color(GRAY));
        }
    }

    private void equip(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /cosmetics equip <color|effect|set> <id>").color(GRAY));
            return;
        }
        Kind kind = Kind.parse(args[1]);
        if (kind == null || kind == Kind.EMOTES) {
            player.sendMessage(Component.text("You can equip a color, an effect, or a set.").color(RED));
            return;
        }
        String id = args[2].toLowerCase();
        PlayerState state = plugin.store().peek(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (state == null || !state.hasActive(kind, id, now)) {
            player.sendMessage(Component.text("You don't hold that right now.").color(RED));
            return;
        }
        plugin.equipAndSave(player, kind, id);
    }

    private void off(Player player, String[] args) {
        PlayerState state = plugin.store().state(player.getUniqueId());
        if (args.length >= 2) {
            Kind kind = Kind.parse(args[1]);
            if (kind != null) state.setEquipped(kind, null);
        } else {
            state.setEquipped(Kind.COLOR, null);
            state.setEquipped(Kind.EFFECT, null);
            state.setEquipped(Kind.SET, null);
        }
        plugin.stateStore().save();
        player.sendMessage(Component.text("Cosmetics unequipped. Your rentals keep their time.").color(GRAY));
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
