package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.ActionParser;
import com.winkreyes.excelmenu.core.Text;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Executes click-action lines for a viewer. Every line was parsed
 * and validated at load, so dispatch never meets an unknown tag;
 * runtime failures (a vanished command, a bad JSON payload) log a
 * warning naming the menu and item rather than throwing at a player.
 */
final class ActionDispatcher {

    /** What "the current view" means for [close] / [refresh]: a real
     *  inventory on Java, a form to resend on Bedrock. */
    interface Session {
        void close(Player player);
        void refresh(Player player);
    }

    private final JavaPlugin plugin;
    private final MenuService service;

    ActionDispatcher(JavaPlugin plugin, MenuService service) {
        this.plugin = plugin;
        this.service = service;
    }

    void runAll(Player player, String where, List<String> lines, Session session) {
        if (lines == null) return;
        for (String line : lines) {
            ActionParser.Action action;
            try {
                action = ActionParser.parse(line);
            } catch (ActionParser.ActionParseException e) {
                plugin.getLogger().warning("ExcelMenu: " + where + ": " + e.getMessage());
                continue;
            }
            if (action.delayTicks() > 0) {
                ActionParser.Action a = action;
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> runNow(player, where, a, session), a.delayTicks());
            } else {
                runNow(player, where, action, session);
            }
        }
    }

    private void runNow(Player player, String where, ActionParser.Action a, Session session) {
        if (!player.isOnline()) return;
        String body = Placeholders.resolve(player, a.body());
        try {
            switch (a.kind()) {
                case CONSOLE -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), body);
                case PLAYER -> player.performCommand(stripSlash(body));
                case COMMANDEVENT -> {
                    PlayerCommandPreprocessEvent event =
                            new PlayerCommandPreprocessEvent(player, "/" + stripSlash(body));
                    Bukkit.getPluginManager().callEvent(event);
                    if (!event.isCancelled()) {
                        Bukkit.dispatchCommand(player, stripSlash(event.getMessage()));
                    }
                }
                case MESSAGE -> player.sendMessage(LegacyComponentSerializer.legacySection()
                        .deserialize(Text.colorize(body)));
                case JSON -> {
                    try {
                        player.sendMessage(GsonComponentSerializer.gson().deserialize(body));
                    } catch (RuntimeException e) {
                        plugin.getLogger().warning("ExcelMenu: " + where
                                + ": bad [json] payload — " + e.getMessage());
                    }
                }
                case OPEN_MENU -> service.openMenu(player, body);
                case CLOSE -> session.close(player);
                case REFRESH -> session.refresh(player);
                case SOUND -> playSound(player, body, false);
                case BROADCAST_SOUND -> {
                    for (Player p : Bukkit.getOnlinePlayers()) playSound(p, body, true);
                }
                case CONNECT -> connect(player, body);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("ExcelMenu: " + where + ": action [" + a.kind()
                    + "] '" + a.body() + "' failed — " + t.getMessage());
        }
    }

    private void playSound(Player player, String body, boolean broadcast) {
        // Format: SOUND_NAME[;volume[;pitch]] (commas/spaces also accepted).
        String[] parts = body.trim().split("[;,\\s]+");
        Sound sound;
        try {
            sound = Sound.valueOf(parts[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("ExcelMenu: unknown sound '" + parts[0] + "'");
            return;
        }
        float volume = parts.length > 1 ? parseFloat(parts[1], 1f) : 1f;
        float pitch = parts.length > 2 ? parseFloat(parts[2], 1f) : 1f;
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private void connect(Player player, String server) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF("Connect");
            out.writeUTF(server);
            player.sendPluginMessage(plugin, "BungeeCord", bytes.toByteArray());
        } catch (IOException | RuntimeException e) {
            plugin.getLogger().warning("ExcelMenu: [connect] to '" + server
                    + "' failed — " + e.getMessage());
        }
    }

    private static float parseFloat(String s, float dflt) {
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            return dflt;
        }
    }

    private static String stripSlash(String cmd) {
        return cmd.startsWith("/") ? cmd.substring(1) : cmd;
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
