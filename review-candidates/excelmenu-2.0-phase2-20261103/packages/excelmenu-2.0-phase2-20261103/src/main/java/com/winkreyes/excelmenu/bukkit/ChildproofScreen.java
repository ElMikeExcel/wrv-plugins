// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.platform.Detection;
import com.winkreyes.excelmenu.core.screens.ChildproofCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * /childproofmenu — the tap-to-pick front end for the server's
 * /childproof alias, ported from ExcelBedrockMenu 1.5.28's
 * ChildproofMenu (DESIGN-2.0 §2.4). Nothing here changes trust
 * itself: after the player picks a name and taps "Yes, childproof",
 * the screen runs "/childproof <name>" AS the player, so
 * GriefPrevention does all permission checks and prints its own
 * confirmations. The commands.yml aliases keep calling
 * "/childproofmenu done|undone <name>" as their last line — that
 * argument contract is preserved verbatim.
 *
 * Extra guard the typed alias cannot have: the picker is only
 * offered while the player stands inside a claim (GriefPrevention,
 * read by reflection, no compile-time dependency), because GP
 * applies untrust/accesstrust to ALL of the player's claims when
 * they stand outside one.
 */
final class ChildproofScreen implements CommandExecutor {

    private final JavaPlugin plugin;
    private final PlatformBridge bridge;

    ChildproofScreen(JavaPlugin plugin, PlatformBridge bridge) {
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
        Player p = (Player) sender;
        if (args.length >= 1) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("help")) {
                for (String l : ChildproofCore.helpLines()) p.sendMessage(l);
                return true;
            }
            if ((sub.equals("done") || sub.equals("undone")) && args.length >= 2) {
                p.sendMessage(ChildproofCore.resultLine(
                        sub.equals("done"), args[1], verify(p, args[1])));
                return true;
            }
        }
        open(p);
        return true;
    }

    // ---------- the picker ----------

    void open(Player player) {
        if (Bukkit.getCommandMap().getCommand("childproof") == null) {
            player.sendMessage(ChildproofCore.NOT_SET_UP_MESSAGE);
            return;
        }
        Boolean here = inClaim(player);
        if (Boolean.FALSE.equals(here)) {
            player.sendMessage(ChildproofCore.OUTSIDE_CLAIM_MESSAGE);
            return;
        }
        List<Player> others = new ArrayList<>();
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (!o.equals(player) && player.canSee(o)) others.add(o);
        }
        others.sort(Comparator.comparing(
                o -> ScreenItems.stripColor(ScreenItems.displayName(o))
                        .toLowerCase(Locale.ROOT)));
        if (others.isEmpty()) {
            player.sendMessage(ChildproofCore.NOBODY_MESSAGE);
            return;
        }
        if (bridge.detect(player) == Detection.State.BEDROCK) {
            try {
                if (bedrockList(player, others)) return;
            } catch (Throwable t) {
                plugin.getLogger().warning("ExcelMenu: childproof form failed for "
                        + player.getName() + ": " + t);
            }
        }
        javaList(player, others);
    }

    private boolean bedrockList(Player player, List<Player> others) {
        List<UUID> ids = new ArrayList<>();
        List<String> buttons = new ArrayList<>();
        for (Player o : others) {
            buttons.add(ChildproofCore.label(
                    ScreenItems.stripColor(ScreenItems.displayName(o)), o.getName()));
            ids.add(o.getUniqueId());
        }
        return bridge.sendSimpleForm(player, ChildproofCore.TITLE,
                ChildproofCore.LIST_BODY, buttons,
                new PlatformBridge.FormHandler() {
                    @Override
                    public void onSelect(int index) {
                        if (index < 0 || index >= ids.size()) return;
                        confirm(player, ids.get(index));
                    }

                    @Override
                    public void onClosedOrInvalid() {
                        // Closed = cancelled: nothing is changed.
                    }
                });
    }

    private void confirm(Player player, UUID targetId) {
        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !player.isOnline()) {
            if (player.isOnline()) {
                player.sendMessage(ChildproofCore.TARGET_LEFT_MESSAGE);
            }
            return;
        }
        String name = target.getName();
        boolean sent = bridge.sendModalForm(player, ChildproofCore.confirmTitle(name),
                ChildproofCore.confirmContent(name),
                ChildproofCore.YES, ChildproofCore.NO,
                new PlatformBridge.ModalHandler() {
                    @Override
                    public void onResult(boolean first) {
                        if (first && player.isOnline()) {
                            player.performCommand(
                                    ChildproofCore.childproofCommand(name));
                        }
                    }

                    @Override
                    public void onClosedOrInvalid() {
                        // Closed = cancelled: nothing is changed.
                    }
                });
        if (!sent) {
            player.sendMessage(ChildproofCore.typeItMessage(name));
        }
    }

    /** Java (or form failure): clickable names in chat that fill in
     *  the command; the player presses Enter to run it. */
    private void javaList(Player player, List<Player> others) {
        player.sendMessage(ChildproofCore.JAVA_LIST_HEADER);
        for (Player o : others) {
            String label = ChildproofCore.label(
                    ScreenItems.stripColor(ScreenItems.displayName(o)), o.getName());
            Component line = Component.text(" » ", NamedTextColor.AQUA)
                    .append(Component.text(label, NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.suggestCommand(
                                    "/" + ChildproofCore.childproofCommand(o.getName())))
                            .hoverEvent(HoverEvent.showText(
                                    Component.text("/childproof " + o.getName()))));
            player.sendMessage(line);
        }
    }

    // ---------- GriefPrevention reads (reflection, fail-soft) ----------

    /** Tri-state: TRUE in a claim, FALSE not in one, null when
     *  GriefPrevention can't be read. */
    static Boolean inClaim(Player player) {
        try {
            Plugin gp = Bukkit.getPluginManager().getPlugin("GriefPrevention");
            if (gp == null || !gp.isEnabled()) return null;
            Field ds = gp.getClass().getField("dataStore");
            Object store = ds.get(gp);
            Method at = null;
            for (Method m : store.getClass().getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (m.getName().equals("getClaimAt") && p.length == 3
                        && p[0] == Location.class && p[1] == boolean.class) {
                    at = m;
                    break;
                }
            }
            if (at == null) return null;
            return at.invoke(store, player.getLocation(), true, null) != null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Verified state after the alias ran: "access", "none",
     *  "other:<level>", "outside", "unknown". */
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
                if (m.getName().equals("getClaimAt") && pt.length == 3
                        && pt[0] == Location.class && pt[1] == boolean.class) {
                    claim = m.invoke(store, owner.getLocation(), true, null);
                    break;
                }
            }
            if (claim == null) return "outside";
            @SuppressWarnings({"unchecked", "rawtypes"})
            Class<Enum> perm = (Class<Enum>) Class.forName(
                    "me.ryanhamshire.GriefPrevention.ClaimPermission", true,
                    gp.getClass().getClassLoader());
            Method check = claim.getClass().getMethod("checkPermission",
                    UUID.class, perm, org.bukkit.event.Event.class);
            boolean build = check.invoke(claim, t.getUniqueId(),
                    Enum.valueOf(perm, "Build"), null) == null;
            boolean cont = check.invoke(claim, t.getUniqueId(),
                    Enum.valueOf(perm, "Container"), null) == null;
            boolean access = check.invoke(claim, t.getUniqueId(),
                    Enum.valueOf(perm, "Access"), null) == null;
            if (build) return "other:build";
            if (cont) return "other:containers";
            return access ? "access" : "none";
        } catch (Throwable e) {
            return "unknown";
        }
    }
}
