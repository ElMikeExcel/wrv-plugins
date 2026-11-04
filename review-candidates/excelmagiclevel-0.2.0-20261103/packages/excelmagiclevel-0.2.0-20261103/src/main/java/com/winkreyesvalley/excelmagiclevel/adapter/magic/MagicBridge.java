package com.winkreyesvalley.excelmagiclevel.adapter.magic;

import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * ExcelMagicLevel — the single door to Magic's API (house build style:
 * every Magic touchpoint lives in adapter.magic, behind this bridge, so a
 * Magic API shift is absorbed here and nowhere else). Magic is the
 * substrate, so a missing controller is the one correct hard edge
 * (SPEC §9.2) — the plugin disables rather than half-running.
 */
public final class MagicBridge {
    private MageController controller;

    /** @return true if the Magic controller is reachable. */
    public boolean connect() {
        try {
            RegisteredServiceProvider<MagicAPI> rsp =
                    Bukkit.getServicesManager().getRegistration(MagicAPI.class);
            if (rsp != null && rsp.getProvider() != null) {
                controller = rsp.getProvider().getController();
            }
        } catch (Throwable ignored) {
            // fall through to the plugin-cast route
        }
        if (controller == null) {
            try {
                Plugin magic = Bukkit.getPluginManager().getPlugin("Magic");
                if (magic instanceof MagicAPI api) {
                    controller = api.getController();
                }
            } catch (Throwable ignored) {
                // controller stays null; caller treats it as the hard edge
            }
        }
        return controller != null;
    }

    public MageController controller() {
        return controller;
    }

    public Mage mage(Player player) {
        return controller == null ? null : controller.getMage(player);
    }
}
