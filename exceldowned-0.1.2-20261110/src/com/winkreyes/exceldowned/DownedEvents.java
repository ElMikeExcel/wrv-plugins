package com.winkreyes.exceldowned;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Bukkit events ExcelDowned fires so Protect / BossTrial can consume the
 * downed lifecycle live (DESIGN §7). None are cancellable — the state
 * machine has already decided when these fire.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class DownedEvents {

    private DownedEvents() {}

    public static final class DownedEnterEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();
        private final Player player;
        private final String cause;
        public DownedEnterEvent(Player player, String cause) { this.player = player; this.cause = cause; }
        public Player getPlayer() { return player; }
        public String getCause() { return cause; }
        @Override public HandlerList getHandlers() { return HANDLERS; }
        public static HandlerList getHandlerList() { return HANDLERS; }
    }

    public static final class DownedReviveEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();
        private final Player healer;
        private final Player target;
        private final double amountRestored;
        public DownedReviveEvent(Player healer, Player target, double amountRestored) {
            this.healer = healer; this.target = target; this.amountRestored = amountRestored;
        }
        public Player getHealer() { return healer; }
        public Player getTarget() { return target; }
        public double getAmountRestored() { return amountRestored; }
        @Override public HandlerList getHandlers() { return HANDLERS; }
        public static HandlerList getHandlerList() { return HANDLERS; }
    }

    public static final class DownedDeathEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();
        private final Player player;
        private final DownedSession.Exit exit;
        public DownedDeathEvent(Player player, DownedSession.Exit exit) { this.player = player; this.exit = exit; }
        public Player getPlayer() { return player; }
        public DownedSession.Exit getExit() { return exit; }
        @Override public HandlerList getHandlers() { return HANDLERS; }
        public static HandlerList getHandlerList() { return HANDLERS; }
    }
}
