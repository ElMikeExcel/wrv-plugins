package com.winkreyes.excelbedrockmenu;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * v1.5.17: the example player name in the help text, usage and hints is
 * the neutral "Alex" (1.5.16 used a real family member's first name).
 *
 * v1.5.16: /childproofmenu - tap-to-pick front end for the server's
 * /childproof alias (commands.yml: untrust then accesstrust, run as the
 * claim owner). Nothing here changes trust itself: after the player
 * picks a name and taps "Yes, childproof", we run "/childproof <name>"
 * AS the player, so GriefPrevention does all permission checks and
 * prints its own confirmations.
 *
 * Extra guard the typed alias cannot have: we only offer it while the
 * player stands inside a claim (GriefPrevention, read by reflection, no
 * compile-time dependency), because GP applies untrust/accesstrust to
 * ALL of the player's claims when they stand outside one.
 */
public class ChildproofMenu implements CommandExecutor {

    static final String TITLE = "Childproof a player";
    static final String YES = "Yes, childproof";
    static final String NO = "Cancel";

    private final ExcelBedrockMenu plugin;

    public ChildproofMenu(ExcelBedrockMenu plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
        Player p = (Player) sender;
        if (args.length >= 1) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("help")) { for (String l : helpLines()) p.sendMessage(l); return true; }
            if ((sub.equals("done") || sub.equals("undone")) && args.length >= 2) {
                p.sendMessage(resultLine(sub.equals("done"), args[1], verify(p, args[1])));
                return true;
            }
        }
        open(plugin, p);
        return true;
    }

    // ---------- v1.5.16 confirmations (called as the last line of the commands.yml aliases) ----------

    /** Help text, always with a concrete example. */
    static String[] helpLines() {
        return new String[]{
            "\u00a76Childproof \u00a77- let little ones use doors, buttons and beds in your claim without breaking stuff.",
            "\u00a7eStand in your claim and type \u00a7a/childproof Alex \u00a77(example - type the player's name, not a nickname)",
            "\u00a7eUndo it: \u00a7a/unchildproof Alex\u00a77. Full trust again: \u00a7a/trust Alex",
            "\u00a7eOn a phone? Tap \u00a7a/childproofmenu\u00a7e and pick the player from a list.",
        };
    }

    /** Verified state after the alias ran: "access", "none", "other:<level>", "outside", "unknown". */
    static String verify(Player owner, String name) {
        try {
            Plugin gp = Bukkit.getPluginManager().getPlugin("GriefPrevention");
            if (gp == null || !gp.isEnabled()) return "unknown";
            org.bukkit.OfflinePlayer t = Bukkit.getPlayerExact(name);
            if (t == null) t = Bukkit.getOfflinePlayerIfCached(name);
            if (t == null) return "unknown";
            Object store = gp.getClass().getField("dataStore").get(gp);
            Object claim = null;
            for (Method m : store.getClass().getMethods()) {
                Class<?>[] pt = m.getParameterTypes();
                if (m.getName().equals("getClaimAt") && pt.length == 3 && pt[0] == Location.class && pt[1] == boolean.class) {
                    claim = m.invoke(store, owner.getLocation(), true, null); break;
                }
            }
            if (claim == null) return "outside";
            @SuppressWarnings({"unchecked", "rawtypes"})
            Class<Enum> perm = (Class<Enum>) Class.forName("me.ryanhamshire.GriefPrevention.ClaimPermission", true, gp.getClass().getClassLoader());
            Method check = claim.getClass().getMethod("checkPermission", UUID.class, perm, org.bukkit.event.Event.class);
            boolean build = check.invoke(claim, t.getUniqueId(), Enum.valueOf(perm, "Build"), null) == null;
            boolean cont = check.invoke(claim, t.getUniqueId(), Enum.valueOf(perm, "Container"), null) == null;
            boolean access = check.invoke(claim, t.getUniqueId(), Enum.valueOf(perm, "Access"), null) == null;
            if (build) return "other:build";
            if (cont) return "other:containers";
            return access ? "access" : "none";
        } catch (Throwable e) {
            return "unknown";
        }
    }

    /** The one confirmation line, with the real name in the example commands. */
    static String resultLine(boolean childproof, String name, String state) {
        if (state.equals("outside"))
            return "\u00a7eYou weren't standing in a claim, so this changed ALL your claims. To change just one, stand in it and type \u00a7a/" + (childproof ? "childproof " : "unchildproof ") + name;
        if (childproof && state.equals("access"))
            return "\u00a7aDone! " + name + " can open doors, press buttons and use beds in this claim, but can't build, break things or open chests. Undo: \u00a7e/unchildproof " + name;
        if (!childproof && state.equals("none"))
            return "\u00a7aDone! " + name + " has no access to this claim now. Childproof again: \u00a7e/childproof " + name;
        if (state.startsWith("other:"))
            return "\u00a7e" + name + " still has " + state.substring(6) + " access here (maybe from public or a group trust). Check \u00a7a/trustlist\u00a7e.";
        return "\u00a7eCheck the messages above. Type the player's exact name, like \u00a7a/" + (childproof ? "childproof " : "unchildproof ") + "Alex\u00a7e, while standing in your claim.";
    }

    /** Body text of the confirm form for a picked name. */
    static String confirmContent(String name) {
        return name + " will only be able to use doors, buttons, levers and beds in this claim. "
                + "They can't build, break things or open chests here. "
                + "Undo: /unchildproof " + name + ". Full trust again: /trust " + name + ".";
    }

    /** The exact command run as the player for a picked name. */
    static String childproofCommand(String name) {
        return "childproof " + name;
    }

    /** Tri-state: TRUE in a claim, FALSE not in one, null when GriefPrevention can't be read. */
    static Boolean inClaim(Player player) {
        try {
            Plugin gp = Bukkit.getPluginManager().getPlugin("GriefPrevention");
            if (gp == null || !gp.isEnabled()) return null;
            Field ds = gp.getClass().getField("dataStore");
            Object store = ds.get(gp);
            Method at = null;
            for (Method m : store.getClass().getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (m.getName().equals("getClaimAt") && p.length == 3 && p[0] == Location.class && p[1] == boolean.class) { at = m; break; }
            }
            if (at == null) return null;
            return at.invoke(store, player.getLocation(), true, null) != null;
        } catch (Throwable t) {
            return null;
        }
    }

    static void open(ExcelBedrockMenu plugin, Player player) {
        if (Bukkit.getCommandMap().getCommand("childproof") == null) {
            player.sendMessage(ChatColor.YELLOW + "Childproof isn't set up on this server yet. Ask an adult to check the server.");
            return;
        }
        Boolean here = inClaim(player);
        if (Boolean.FALSE.equals(here)) {
            player.sendMessage(ChatColor.YELLOW + "Stand inside your claim first, then try again (or type it, like /childproof Alex). It only changes the claim you're standing in.");
            return;
        }
        List<Player> others = new ArrayList<>();
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (!o.equals(player) && player.canSee(o)) others.add(o);
        }
        others.sort(Comparator.comparing(o -> ChatColor.stripColor(o.getDisplayName()).toLowerCase(Locale.ROOT)));
        if (others.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Nobody else is online right now. You can also type the name, like /childproof Alex (example).");
            return;
        }
        if (GeyserForms.isBedrockPlayer(player, plugin.getLogger())) {
            try {
                if (bedrockList(plugin, player, others)) return;
            } catch (Throwable t) {
                plugin.getLogger().warning("Childproof form failed for " + player.getName() + ": " + t);
            }
        }
        javaList(player, others);
    }

    /** Button label: nickname plus the real name when they differ (".KidPlayer" shows as "Alex (.KidPlayer)"). */
    static String label(String display, String name) {
        String d = display == null ? "" : ChatColor.stripColor(display).trim();
        String core = d.startsWith("~") ? d.substring(1) : d; // Essentials nick prefix
        if (core.isEmpty() || core.equalsIgnoreCase(name)) return name;
        return core + " (" + name + ")";
    }

    private static boolean bedrockList(ExcelBedrockMenu plugin, Player player, List<Player> others) {
        List<UUID> ids = new ArrayList<>();
        String[] buttons = new String[others.size()];
        for (int i = 0; i < others.size(); i++) {
            buttons[i] = label(others.get(i).getDisplayName(), others.get(i).getName());
            ids.add(others.get(i).getUniqueId());
        }
        return GeyserForms.sendSimpleForm(player, TITLE,
                "Tap who should only use doors and buttons in this claim (no building, breaking or chests).",
                buttons,
                clicked -> {
                    if (clicked < 0 || clicked >= ids.size()) return;
                    UUID target = ids.get(clicked);
                    Bukkit.getScheduler().runTask(plugin, () -> confirm(plugin, player, target));
                },
                plugin.getLogger());
    }

    private static void confirm(ExcelBedrockMenu plugin, Player player, UUID targetId) {
        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !player.isOnline()) {
            if (player.isOnline()) player.sendMessage(ChatColor.YELLOW + "That player left. Nothing was changed.");
            return;
        }
        String name = target.getName();
        boolean sent = GeyserForms.sendModalForm(player, "Childproof " + name + "?", confirmContent(name), YES, NO,
                first -> {
                    if (Boolean.TRUE.equals(first) && player.isOnline()) {
                        Bukkit.getScheduler().runTask(plugin, () -> player.performCommand(childproofCommand(name)));
                    }
                }, plugin.getLogger());
        if (!sent) {
            player.sendMessage(ChatColor.YELLOW + "Type /childproof " + name + " to do it.");
        }
    }

    /** Java (or form failure): clickable names in chat that fill in the command; the player presses Enter to run it. */
    private static void javaList(Player player, List<Player> others) {
        player.sendMessage(ChatColor.GOLD + "Childproof: click a name, then press Enter. Doors and buttons only, in this claim.");
        for (Player o : others) {
            TextComponent line = new TextComponent(ChatColor.AQUA + " \u00bb " + ChatColor.GREEN + label(o.getDisplayName(), o.getName()));
            line.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + childproofCommand(o.getName())));
            line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("/childproof " + o.getName())));
            player.spigot().sendMessage(line);
        }
    }
}
