package com.winkreyes.excelcalmbars;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * Per-connection outbound gate, injected into each player's Netty
 * pipeline just ahead of the server's packet handler so it sees
 * boss-bar packets as objects, before encoding — and before any
 * protocol translation, so the same gate covers Java clients and,
 * through Geyser's translation of the same packets, Bedrock.
 *
 * The handler is a thin shell: all decisions live in
 * {@link ExcelCalmBars#tryHold(PlayerBars, Object)} so the fail-open
 * rule is enforced in exactly one place. Any exception anywhere in
 * the decision path passes the packet through untouched.
 */
public final class GateHandler extends ChannelDuplexHandler {

    private final ExcelCalmBars plugin;
    private final PlayerBars bars;

    public GateHandler(ExcelCalmBars plugin, PlayerBars bars) {
        this.plugin = plugin;
        this.bars = bars;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg,
                      ChannelPromise promise) throws Exception {
        if (BossBridge.isBossPacket(msg)
                && !BossBridge.consumeBypass(msg)) {
            boolean held;
            try {
                held = plugin.tryHold(bars, msg);
            } catch (Throwable t) {
                held = false; // fail open, always
            }
            if (held) {
                // Swallowed: complete the promise so the sender's
                // future resolves exactly as if the packet had
                // been written.
                promise.setSuccess();
                return;
            }
        }
        super.write(ctx, msg, promise);
    }
}
