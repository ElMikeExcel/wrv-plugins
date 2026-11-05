package com.winkreyes.excellandmarks.menu;

import com.winkreyes.excellandmarks.core.SpotMenuModel;

import java.util.UUID;
import java.util.function.IntConsumer;

/**
 * Bedrock form port (SPEC §6): the implementation that touches
 * Floodgate/Cumulus classes lives in FloodgateForms and is only loaded
 * when Floodgate is present. Menus code talks only to this interface,
 * so the plugin enables cleanly with no form stack at all — Bedrock
 * players then get the inventory renderer through Geyser, exactly the
 * Phase-1 BrainGames fallback.
 */
public interface FormPort {

    FormPort NONE = new FormPort() {
        @Override public boolean available() { return false; }
        @Override public boolean isBedrock(UUID player) { return false; }
        @Override public boolean send(UUID player, SpotMenuModel.Screen screen, IntConsumer onButton) { return false; }
    };

    boolean available();

    boolean isBedrock(UUID player);

    /**
     * Send one screen as a native form. The callback receives the model
     * button INDEX, or -1 when the form was closed without a choice.
     * Returns false when the form could not be sent (caller falls back
     * to the inventory renderer).
     */
    boolean send(UUID player, SpotMenuModel.Screen screen, IntConsumer onButton);
}
