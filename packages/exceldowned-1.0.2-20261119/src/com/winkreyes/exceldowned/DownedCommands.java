package com.winkreyes.exceldowned;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 * /giveup (two-step, DESIGN §4 — 0.1.1: the first step opens the
 * button confirm UI; the typed two-step remains as the fallback and
 * shares the same arm state) and /downed (status + staff reload;
 * 0.1.2: console-admin revive/clear — see AdminPolicy).
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class DownedCommands implements CommandExecutor {

    private final ExcelDowned plugin;

    public DownedCommands(ExcelDowned plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("raiseally")) {
            // Cleric "Raise Ally" (1.0.0, design B sections 3.1/8):
            // the typed door — one attemptRaise path behind every
            // door, so a Bedrock player whose form cannot be sent
            // can still fire it inside the bleed-out window. All
            // gates, costs, and messages are ClericRevive's.
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only a player can raise an ally.");
                return true;
            }
            if (plugin.clericRevive() != null) {
                plugin.clericRevive().attemptRaise((Player) sender);
            } else {
                sender.sendMessage(ChatColor.GRAY
                        + "✝ Raise Ally is unavailable right now.");
            }
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("giveup")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only a player can give up.");
                return true;
            }
            Player p = (Player) sender;
            DownedSession s = plugin.sessionOf(p.getUniqueId());
            if (s == null) {
                p.sendMessage(ChatColor.GRAY + "You are not downed.");
                return true;
            }
            long now = System.currentTimeMillis();
            // One arm state, two doors (0.1.1): a typed /giveup while
            // the give-up stands armed — by an open menu, an open form,
            // or a chat arm — CONFIRMS, exactly as the second typed
            // /giveup did in 0.1.0.
            if (s.isGiveUpArmed(now, plugin.giveUpWindowMs) || s.isGiveUpConfirmed()) {
                DownedSession.GiveUpResult r = s.giveUp(now, plugin.giveUpWindowMs);
                if (r == DownedSession.GiveUpResult.DIED) {
                    p.sendMessage(ChatColor.DARK_RED + "✝ You let go.");
                    plugin.finalizeDeath(p, DownedSession.Exit.GIVE_UP);
                }
                return true;
            }
            // First step: arm, then present the button confirm (the
            // house way). If no UI can be presented, the 0.1.0 chat
            // prompt is the fallback and the arm stands on its own.
            s.armGiveUp(now);
            if (plugin.giveUpUi().openConfirm(p)) {
                p.sendMessage(ChatColor.RED + "✝ Let go, or keep fighting? You have "
                        + (plugin.giveUpWindowMs / 1000) + "s.");
            } else {
                p.sendMessage(ChatColor.RED + "✝ Are you sure? Type " + ChatColor.BOLD + "/giveup"
                        + ChatColor.RESET + ChatColor.RED + " again within "
                        + (plugin.giveUpWindowMs / 1000) + "s to let go and die where you lie.");
            }
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("downed")) {
            // 1.0.0 shop charge leg (W6 section 3.2): the paid buy
            // verb is claimed FIRST, before the give/admin surface —
            // console-only; a player sender is refused with the shop
            // pointer; the review opens for the named buyer and
            // charges nothing until its own Confirm.
            RevivePotionPolicy.BuyParsed buy = RevivePotionPolicy.routeBuy(args,
                    sender instanceof ConsoleCommandSender);
            switch (buy.route()) {
                case BUY -> { revivePotionBuy(sender, buy.target()); return true; }
                case DENIED -> {
                    sender.sendMessage(ChatColor.GOLD + "✝ Revive Potions are sold at the "
                            + "Excel Shop — use the shop menu.");
                    return true;
                }
                case USAGE -> {
                    sender.sendMessage("Usage: /downed revivepotion buy <player>");
                    return true;
                }
                default -> { /* NOT_OURS: fall through to the give surface */ }
            }
            // 1.0.0 Revive Potion seam (spec A sections 7/8): routed
            // and gated by RevivePotionPolicy BEFORE the admin surface
            // — the give gate (console || exceldowned.admin) precedes
            // all target handling, exactly like the 0.1.2 verbs.
            RevivePotionPolicy.GiveParsed rp = RevivePotionPolicy.routeGive(args,
                    sender instanceof ConsoleCommandSender,
                    sender.hasPermission(AdminPolicy.PERMISSION));
            switch (rp.route()) {
                case GIVE -> { revivePotionGive(sender, rp.target(), rp.amount()); return true; }
                case CONFIRM -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("Only a player can confirm a revive.");
                        return true;
                    }
                    Player carrier = (Player) sender;
                    if (plugin.revivePotionUi() == null
                            || !plugin.revivePotionUi().confirmFromChat(carrier)) {
                        carrier.sendMessage(ChatColor.GRAY
                                + "✝ No revive is waiting on you — walk up to a downed"
                                + " player and click them.");
                    }
                    return true;
                }
                case DENIED -> {
                    sender.sendMessage("ExcelDowned: /downed revivepotion give needs the "
                            + AdminPolicy.PERMISSION + " permission.");
                    return true;
                }
                case USAGE -> {
                    sender.sendMessage("Usage: /downed revivepotion give <player> [amount]"
                            + " | /downed revivepotion confirm");
                    return true;
                }
                default -> { /* NOT_OURS: fall through to the 0.1.2 surface */ }
            }
            // The retired free-buy alias is deliberately NOT claimed
            // here (W6 section 3.2): "/downed revive buy <player>"
            // routes NOT_OURS in RevivePotionPolicy.routeBuy and is
            // not a revivepotion verb for routeGive, so it falls
            // through to the generic surfaces below and can never
            // reach the mint.
            // 0.1.2 console admin (audit §4): revive/clear are routed
            // and gated by AdminPolicy BEFORE the legacy surface; the
            // gate (console explicit, otherwise exceldowned.admin)
            // precedes all target handling, and the legacy branches
            // below are the pre-0.1.2 behavior unchanged.
            AdminPolicy.Parsed parsed = AdminPolicy.route(args,
                    sender instanceof ConsoleCommandSender,
                    sender.hasPermission(AdminPolicy.PERMISSION));
            switch (parsed.route()) {
                case ADMIN_DENIED -> {
                    sender.sendMessage("ExcelDowned: /downed " + args[0].toLowerCase()
                            + " needs the " + AdminPolicy.PERMISSION + " permission.");
                    return true;
                }
                case ADMIN_USAGE -> {
                    sender.sendMessage("Usage: /downed revive <player> | /downed clear <player>");
                    return true;
                }
                case ADMIN_REVIVE -> { adminRevive(sender, parsed.target()); return true; }
                case ADMIN_CLEAR -> { adminClear(sender, parsed.target()); return true; }
                default -> { /* LEGACY: fall through to the pre-0.1.2 surface */ }
            }
            if (args.length >= 1 && args[0].equalsIgnoreCase("reload")
                    && sender.hasPermission("exceldowned.admin")) {
                plugin.reloadConfig();
                plugin.loadSettings();
                sender.sendMessage("ExcelDowned settings reloaded.");
                return true;
            }
            if (args.length >= 1 && sender.hasPermission("exceldowned.admin")) {
                Player t = Bukkit.getPlayer(args[0]);
                if (t == null) { sender.sendMessage("No such player online."); return true; }
                DownedSession s = plugin.sessionOf(t.getUniqueId());
                sender.sendMessage(t.getName() + ": " + (s == null ? "not downed"
                        : "DOWNED, bleed out in "
                        + ExcelDowned.formatClock(s.remainingMs(System.currentTimeMillis()))
                        + " (cause " + s.cause() + ")"));
                sender.sendMessage("Revive Potions: "
                        + RevivePotionItem.countPotions(plugin, t.getInventory()));
                return true;
            }
            if (sender instanceof Player) {
                DownedSession s = plugin.sessionOf(((Player) sender).getUniqueId());
                sender.sendMessage(s == null ? "You are not downed."
                        : "DOWNED — bleed out in "
                        + ExcelDowned.formatClock(s.remainingMs(System.currentTimeMillis())));
            }
            return true;
        }
        return false;
    }

    // ---------- 1.0.0 Revive Potion seam handlers (thin mechanics;
    // ---------- routing/gate decisions are RevivePotionPolicy's)

    /**
     * The paid buy (W6 section 3.2): console dispatch only (the
     * route already refused everyone else). Resolves the named
     * online buyer and opens their purchase review — no charge at
     * dispatch, ever; only the review's Confirm can debit.
     */
    private void revivePotionBuy(CommandSender sender, String target) {
        Player buyer = Bukkit.getPlayerExact(target);
        if (buyer == null) {
            sender.sendMessage("Downed revivepotion buy: no player named '" + target
                    + "' is online; no review was opened and nothing was charged.");
            return;
        }
        RevivePurchaseCoordinator.Result result = plugin.openRevivePurchase(buyer);
        if (result != null && result.status() == RevivePurchaseCoordinator.Status.ARMED) {
            sender.sendMessage("Downed revivepotion buy: opened the Revive Potion "
                    + "purchase review for " + buyer.getName() + " (no charge yet).");
        } else {
            sender.sendMessage("Downed revivepotion buy: no review opened for "
                    + buyer.getName() + " ("
                    + (result == null ? "UNAVAILABLE" : result.status().name())
                    + "); nothing was charged.");
        }
    }

    private void revivePotionGive(CommandSender sender, String target, int amount) {
        Player t = Bukkit.getPlayerExact(target);
        if (t == null) {
            sender.sendMessage("Downed revivepotion: no player named '" + target
                    + "' is online; nothing was minted.");
            return;
        }
        int minted = plugin.giveRevivePotions(actorOf(sender), t, amount);
        if (minted > 0) {
            sender.sendMessage("Downed revivepotion: minted " + minted
                    + " Revive Potion(s) for " + t.getName() + ".");
            t.sendMessage(ChatColor.GREEN + "✝ You received " + minted
                    + " Revive Potion(s). Carry one to a downed player — don't drink it.");
        } else {
            sender.sendMessage("Downed revivepotion: nothing was minted"
                    + " (the feature may be disabled in config).");
        }
    }

    // ---------- 0.1.2 console admin handlers (thin Bukkit mechanics;
    // ---------- every decision is AdminPolicy's) ----------

    private static String actorOf(CommandSender sender) {
        return sender instanceof ConsoleCommandSender ? "CONSOLE" : sender.getName();
    }

    /** Classify a named target: online player, its live session, and
     *  any durable (offline) session recorded under that name. */
    private AdminPolicy.TargetKind classify(String target, Player[] onlineOut,
                                            DownedSession[] sessionOut) {
        Player online = Bukkit.getPlayerExact(target);
        DownedSession live = online == null ? null : plugin.sessionOf(online.getUniqueId());
        DownedSession durable = live == null ? plugin.sessionByName(target) : null;
        onlineOut[0] = online;
        sessionOut[0] = live != null ? live : durable;
        return AdminPolicy.targetKind(online != null, live != null, durable != null);
    }

    private void adminRevive(CommandSender sender, String target) {
        Player[] online = new Player[1];
        DownedSession[] session = new DownedSession[1];
        AdminPolicy.TargetKind kind = classify(target, online, session);
        switch (AdminPolicy.decideRevive(kind)) {
            case PROCEED -> {
                if (plugin.adminRevive(actorOf(sender), online[0])) {
                    sender.sendMessage("Downed admin: " + online[0].getName()
                            + " revived — downed state resolved at 50% health, no death.");
                } else {
                    sender.sendMessage("Downed admin: " + target + " is no longer downed; nothing was changed.");
                }
            }
            case NOT_DOWNED -> sender.sendMessage("Downed admin: " + online[0].getName()
                    + " is not downed; nothing to revive.");
            case OFFLINE_ONLY -> sender.sendMessage("Downed admin: " + session[0].playerName()
                    + " is DOWNED but offline (bleed out in "
                    + ExcelDowned.formatClock(session[0].remainingMs(System.currentTimeMillis()))
                    + ") — revive restores live health and needs the player online, so nothing was changed. "
                    + "Use /downed clear " + session[0].playerName() + " to remove the offline session instead.");
            case UNKNOWN -> sender.sendMessage("Downed admin: no player named '" + target
                    + "' is online and no downed session exists for that name. Nothing was changed.");
        }
    }

    private void adminClear(CommandSender sender, String target) {
        Player[] online = new Player[1];
        DownedSession[] session = new DownedSession[1];
        AdminPolicy.TargetKind kind = classify(target, online, session);
        switch (AdminPolicy.decideClear(kind)) {
            case PROCEED -> {
                boolean wasOnline = kind == AdminPolicy.TargetKind.ONLINE_DOWNED;
                String name = session[0].playerName();
                if (plugin.adminClear(actorOf(sender), session[0].playerId())) {
                    sender.sendMessage("Downed admin: " + name + "'s downed session cleared ("
                            + (wasOnline ? "online" : "offline record")
                            + "). No health was restored and no death was run.");
                } else {
                    sender.sendMessage("Downed admin: " + target + " is no longer downed; nothing was changed.");
                }
            }
            case NOT_DOWNED -> sender.sendMessage("Downed admin: " + online[0].getName()
                    + " is not downed; nothing to clear.");
            case UNKNOWN -> sender.sendMessage("Downed admin: no player named '" + target
                    + "' is online and no downed session exists for that name. Nothing was changed.");
        }
    }
}
