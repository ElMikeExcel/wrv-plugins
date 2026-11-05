package com.winkreyes.excellandmarks.menu;

import com.winkreyes.excellandmarks.core.SpotMenuModel;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

/**
 * Native Floodgate form renderer (SPEC §6) — the Cumulus SimpleForm
 * stack ExcelMenu's Bedrock renderer uses. ONLY loaded when the
 * floodgate plugin is present (guarded in the main class); no other
 * class references it except through {@link FormPort}.
 */
public final class FloodgateForms implements FormPort {

    private final Logger log;

    public FloodgateForms(Logger log) {
        this.log = log;
    }

    private FloodgateApi api() {
        try {
            return FloodgateApi.getInstance();
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public boolean available() {
        return api() != null;
    }

    @Override
    public boolean isBedrock(UUID player) {
        FloodgateApi api = api();
        return api != null && player != null && api.isFloodgatePlayer(player);
    }

    @Override
    public boolean send(UUID player, SpotMenuModel.Screen screen, IntConsumer onButton) {
        FloodgateApi api = api();
        if (api == null || player == null) return false;
        try {
            SimpleForm.Builder b = SimpleForm.builder()
                    .title(screen.title())
                    .content(String.join("\n", screen.lines()));
            for (SpotMenuModel.Button btn : screen.buttons()) {
                b.button(btn.label());
            }
            b.validResultHandler((form, response) -> {
                int idx;
                try {
                    idx = response.clickedButtonId();
                } catch (Throwable t) {
                    idx = -1;
                }
                onButton.accept(idx);
            });
            b.closedOrInvalidResultHandler((form, response) -> onButton.accept(-1));
            return api.sendForm(player, b.build());
        } catch (Throwable t) {
            log.warning("ExcelLandmarks: Floodgate form send failed (" + t + ") — inventory fallback");
            return false;
        }
    }
}
