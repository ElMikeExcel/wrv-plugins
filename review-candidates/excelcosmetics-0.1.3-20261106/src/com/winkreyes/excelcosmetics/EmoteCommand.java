package com.winkreyes.excelcosmetics;

import com.winkreyes.excelcosmetics.core.EmoteDefs;
import com.winkreyes.excelcosmetics.core.Kind;
import com.winkreyes.excelcosmetics.core.PlayerState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** The emote command pack (/shrug, /hug, ...): ordinary server
 *  chat lines, identical on every client — parity-safe by
 *  construction. Requires the pack to be held (rented or owned);
 *  admins bypass for testing. Small per-player cooldown so the
 *  pack can never become a spam cannon. */
final class EmoteCommand implements CommandExecutor {

    private static final long COOLDOWN_MILLIS = 3000L;
    private final ExcelCosmetics plugin;
    private final Map<UUID, Long> lastUsed = new HashMap<>();

    EmoteCommand(ExcelCosmetics plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Emotes are player actions — run this in game.");
            return true;
        }
        EmoteDefs.Emote emote = EmoteDefs.COMMANDS.get(command.getName().toLowerCase());
        if (emote == null) return false;

        long now = System.currentTimeMillis();
        PlayerState state = plugin.store().peek(player.getUniqueId());
        boolean holds = state != null && state.hasAnyActive(Kind.EMOTES, now);
        if (!holds && !player.hasPermission("excelcosmetics.admin")) {
            player.sendMessage(Component.text(
                    "You don't have the Emote Pack right now — it is sold at the spawn shop.")
                    .color(TextColor.color(0xFF6B6B)));
            return true;
        }
        Long last = lastUsed.get(player.getUniqueId());
        if (last != null && now - last < COOLDOWN_MILLIS && !player.hasPermission("excelcosmetics.admin")) {
            return true; // quiet cooldown — no message, no spam
        }

        String targetName = null;
        if (args.length > 0) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                player.sendMessage(Component.text("Can't find '" + args[0] + "' online.")
                        .color(TextColor.color(0xFF6B6B)));
                return true;
            }
            targetName = target.getName();
        } else if (emote.requiresTarget()) {
            targetName = null; // solo line
        }

        lastUsed.put(player.getUniqueId(), now);
        String line = EmoteDefs.render(emote, player.getName(), targetName);
        plugin.getServer().broadcast(Component.text("* " + line)
                .color(TextColor.color(0xAAAAAA)).decorate(TextDecoration.ITALIC));
        return true;
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
