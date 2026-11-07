package com.winkreyes.excelautosort.cmd;

import com.winkreyes.excelautosort.ExcelAutoSort;
import com.winkreyes.excelautosort.Names;
import com.winkreyes.excelautosort.core.FilterCore;
import com.winkreyes.excelautosort.core.Mode;
import com.winkreyes.excelautosort.engine.FilterEngine;
import com.winkreyes.excelautosort.engine.SelectorFlow;
import com.winkreyes.excelautosort.gui.SelectorSession;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * /autosort (alias /sort) — SPEC §8. Bare use is the parity toggle;
 * subcommands reach everything else, and every one of them is also in
 * the menu (menu standard). Non-players get the refusal line for
 * player commands and may run the admin subcommands; bare console use
 * prints the usage (house console-help rule).
 *
 * The help listing is in WORKFLOW order (chest, recover, status,
 * admin), not alphabetical — a deliberate house habit.
 */
public final class AutoSortCommand implements CommandExecutor, TabCompleter {

    private final ExcelAutoSort plugin;

    public AutoSortCommand(ExcelAutoSort plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player p) {
                if (!p.hasPermission("excelautosort.toggle")) return deny(sender);
                boolean now = plugin.sorter().toggle(p);
                if (now) {
                    plugin.messages().send(p, "sort-enabled", "&aSorting enabled for: {types}",
                            Map.of("types", plugin.sorter().typeListForMessage()));
                } else {
                    plugin.messages().send(p, "sort-disabled", "&cSorting disabled");
                }
                return true;
            }
            usage(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "status" -> { return status(sender); }
            case "max" -> { return max(sender, args); }
            case "chest" -> { return chest(sender, args); }
            case "recover" -> {
                if (!playerOnly(sender)) return true;
                Player p = (Player) sender;
                if (!p.hasPermission("excelautosort.recover")) return deny(sender);
                plugin.openRecovery(p);
                return true;
            }
            case "admin" -> { return admin(sender, args); }
            case "help" -> { usage(sender); return true; }
            default -> { usage(sender); return true; }
        }
    }

    private boolean deny(CommandSender sender) {
        sender.sendMessage(plugin.messages().of(
                plugin.messages().raw("no-permission", "&cYou don't have permission for that.")));
        return true;
    }

    private boolean playerOnly(CommandSender sender) {
        if (sender instanceof Player) return true;
        sender.sendMessage(plugin.messages().raw("not-for-console",
                "This command is not available for non-players."));
        return false;
    }

    private void usage(CommandSender sender) {
        for (String line : plugin.messages().rawList("usage", List.of(
                "&eExcelAutoSort — /autosort toggles sorting of your chests",
                "&e/autosort chest — set up the filter on the chest you are looking at",
                "&e/autosort recover — open your filter recovery box",
                "&e/autosort max — Max Organization: all your chests around you at once",
                "&e/autosort status — your auto-sort state",
                "&e/autosort admin prune|reload — staff tools"), Map.of())) {
            sender.sendMessage(plugin.messages().of(line));
        }
    }

    private boolean status(CommandSender sender) {
        if (sender instanceof Player p) {
            int mine = 0;
            for (var r : plugin.store().chests.values()) {
                if (p.getUniqueId().equals(r.owner)) mine++;
            }
            plugin.messages().send(p, "status-sort", "&eAuto-sort: &f{state}",
                    Map.of("state", plugin.sorter().statusOf(p.getUniqueId())));
            plugin.messages().send(p, "status-chests", "&eFilter chests you set up: &f{count}",
                    Map.of("count", String.valueOf(mine)));
            plugin.messages().send(p, "status-buffer", "&eRecovery box: &f{count} stacks waiting",
                    Map.of("count", String.valueOf(plugin.buffers().countOf(p.getUniqueId()))));
            plugin.messages().send(p, "status-maxorg", "&eMax Organization: &f{state}",
                    Map.of("state", plugin.messages().raw(plugin.maxOrg().stateKey(p.getUniqueId()),
                            "not unlocked", plugin.maxOrg().stateVars(p.getUniqueId()))));
        } else {
            sender.sendMessage("ExcelAutoSort " + com.winkreyes.excelautosort.BuildInfo.VERSION
                    + " — build " + com.winkreyes.excelautosort.BuildInfo.BUILD_ID
                    + " — filter chests: " + plugin.store().chests.size()
                    + ", players with sorting on: " + plugin.store().sortOn.size()
                    + ", protect sink: " + (plugin.sink().bound() ? "BOUND" : "UNBOUND"));
        }
        return true;
    }

    private boolean chest(CommandSender sender, String[] args) {
        if (!playerOnly(sender)) return true;
        Player p = (Player) sender;
        if (!p.hasPermission("excelautosort.chest")) return deny(sender);
        Block target = p.getTargetBlockExact(6);
        if (target == null || (target.getType() != Material.CHEST
                && target.getType() != Material.TRAPPED_CHEST
                && target.getType() != Material.BARREL)) {
            plugin.messages().send(p, "look-at-chest", "&eLook at a chest or barrel and try again.");
            return true;
        }
        List<String> keys = FilterEngine.keysOfBlock(target);
        if (keys.isEmpty()) {
            plugin.messages().send(p, "look-at-chest", "&eLook at a chest or barrel and try again.");
            return true;
        }
        if (args.length >= 3 && (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove"))) {
            return chestEdit(p, keys, args);
        }
        plugin.openSelector(p, keys);
        return true;
    }

    /**
     * The staff text path (§8): the same arm/confirm rules as the GUI.
     * An edit that would create new rejects prints the itemized sweep
     * and changes NOTHING; re-running with --confirm applies it and
     * sweeps (§6.1 — no silent arm, ever).
     */
    private boolean chestEdit(Player p, List<String> keys, String[] args) {
        boolean add = args[1].equalsIgnoreCase("add");
        boolean confirm = false;
        String matName = null;
        for (int i = 2; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--confirm")) confirm = true;
            else matName = args[i];
        }
        Material mat = matName == null ? null : Material.matchMaterial(matName.toUpperCase());
        if (mat == null) {
            plugin.messages().send(p, "bad-material", "&cThat is not an item name I know: {name}",
                    Map.of("name", matName == null ? "" : matName));
            return true;
        }
        SelectorSession s = plugin.flow().open(p, keys);
        if (s == null) return deny(p); // §6.4: plain refusal, nothing leaks.
        SelectorFlow.Result r = add
                ? plugin.flow().addItem(s, mat.name())
                : plugin.flow().removeItem(s, mat.name());
        switch (r) {
            case DENIED -> plugin.messages().send(p, "no-permission",
                    "&cYou don't have permission for that.");
            case NOOP -> plugin.messages().send(p, "edit-noop",
                    "&e{item} is already {state} the list.",
                    Map.of("item", Names.friendly(mat.name()), "state", add ? "on" : "off"));
            case APPLIED -> plugin.messages().send(p, "edit-applied",
                    "&a{item}: {state} the list.",
                    Map.of("item", Names.friendly(mat.name()), "state", add ? "added to" : "removed from"));
            case NEEDS_CONFIRM -> {
                List<FilterCore.RejectLine> rejects = plugin.flow().rejectsOf(s, s.pendingMode, s.pendingItems);
                if (!confirm) {
                    plugin.messages().send(p, "confirm-needed",
                            "&eThat change would remove these from the chest right now:");
                    for (FilterCore.RejectLine l : rejects) {
                        p.sendMessage(plugin.messages().of("&7- " + Names.friendly(l.material()) + " ×" + l.totalCount()));
                    }
                    plugin.messages().send(p, "confirm-how",
                            "&eNothing changed. Run it again with &f--confirm&e to move those to your recovery box.");
                    plugin.flow().cancel(s);
                } else {
                    int[] swept = plugin.flow().confirm(p, s);
                    plugin.messages().send(p, "edit-applied",
                            "&a{item}: {state} the list.",
                            Map.of("item", Names.friendly(mat.name()), "state", add ? "added to" : "removed from"));
                    if (swept[0] > 0) {
                        plugin.messages().send(p, "swept", "&eMoved {count} items to your recovery box.",
                                Map.of("count", String.valueOf(swept[1])));
                    }
                }
            }
        }
        return true;
    }

    /**
     * /autosort max (§15): bare opens the Max Organization screens
     * (network view when entitled, the unlock screen otherwise).
     * rent|buy is the text path — the same confirm-before-charge rule
     * as the screens: the first run prints the full confirm wording
     * and charges nothing; only the --confirm re-run charges, through
     * the service's single charge path.
     */
    private boolean max(CommandSender sender, String[] args) {
        if (!playerOnly(sender)) return true;
        Player p = (Player) sender;
        if (!p.hasPermission("excelautosort.maxorg")) return deny(sender);
        if (args.length < 2) {
            plugin.openMaxOrg(p);
            return true;
        }
        switch (args[1].toLowerCase()) {
            case "status" -> {
                plugin.messages().send(p, "status-maxorg", "&eMax Organization: &f{state}",
                        Map.of("state", plugin.messages().raw(plugin.maxOrg().stateKey(p.getUniqueId()),
                                "not unlocked", plugin.maxOrg().stateVars(p.getUniqueId()))));
                return true;
            }
            case "rent", "buy" -> {
                boolean buy = args[1].equalsIgnoreCase("buy");
                boolean confirm = false;
                for (int i = 2; i < args.length; i++) {
                    if (args[i].equalsIgnoreCase("--confirm")) confirm = true;
                }
                if (!confirm) {
                    for (String line : plugin.maxOrg().confirmLines(buy)) {
                        p.sendMessage(plugin.messages().of("&e" + line));
                    }
                    plugin.messages().send(p, "maxorg-confirm-how",
                            "&eNothing charged. Run &f/autosort max {kind} --confirm&e to go ahead.",
                            Map.of("kind", buy ? "buy" : "rent"));
                    return true;
                }
                com.winkreyes.excelautosort.engine.MaxOrgService.PurchaseOutcome outcome =
                        plugin.maxOrg().purchase(p, buy);
                plugin.maxOrg().tellOutcome(p, outcome, buy);
                return true;
            }
            default -> {
                plugin.openMaxOrg(p);
                return true;
            }
        }
    }

    private boolean admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("excelautosort.admin")) return deny(sender);
        if (args.length >= 2 && args[1].equalsIgnoreCase("prune")) {
            int n = plugin.filter().adminPrune();
            sender.sendMessage(plugin.messages().of("&aPruned " + n + " filter record(s) whose chest is gone."));
            return true;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            sender.sendMessage(plugin.messages().of("&aExcelAutoSort reloaded (config + data)."));
            return true;
        }
        sender.sendMessage(plugin.messages().of("&eUsage: /autosort admin prune | reload"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("chest", "recover", "max", "status", "admin", "help")) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("max")) {
            for (String s : List.of("rent", "buy", "status")) if (s.startsWith(args[1].toLowerCase())) out.add(s);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("max")) {
            if ("--confirm".startsWith(args[2].toLowerCase())) out.add("--confirm");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("chest")) {
            for (String s : List.of("add", "remove")) if (s.startsWith(args[1].toLowerCase())) out.add(s);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("chest")) {
            String prefix = args[2].toUpperCase();
            for (Material m : Material.values()) {
                if (m.name().startsWith(prefix)) { out.add(m.name().toLowerCase()); if (out.size() > 40) break; }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            for (String s : List.of("prune", "reload")) if (s.startsWith(args[1].toLowerCase())) out.add(s);
        }
        return out;
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
