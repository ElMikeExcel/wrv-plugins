// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.platform.Detection;
import com.winkreyes.excelmenu.core.screens.WebsiteCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * /serverwebsite (DESIGN-2.0 §2.4), ported from ExcelBedrockMenu
 * 1.5.28's showWebsite: Bedrock players get a form spelling the
 * addresses out (Bedrock cannot open a browser from in game) and
 * both addresses as a plain chat copy; Java players get both
 * addresses as clickable chat links.
 */
final class WebsiteScreen {

    private final JavaPlugin plugin;
    private final PlatformBridge bridge;
    /** Opens the Server Menu (3.0.2, BB-01 FIX-2): the Bedrock
     *  form's Back lands there. */
    private final java.util.function.Consumer<Player> serverMenuOpener;

    WebsiteScreen(JavaPlugin plugin, PlatformBridge bridge) {
        this(plugin, bridge, p -> p.performCommand("menu"));
    }

    WebsiteScreen(JavaPlugin plugin, PlatformBridge bridge,
                  java.util.function.Consumer<Player> serverMenuOpener) {
        this.plugin = plugin;
        this.bridge = bridge;
        this.serverMenuOpener = serverMenuOpener;
    }

    void show(Player player) {
        if (bridge.detect(player) == Detection.State.BEDROCK) {
            bridge.sendSimpleForm(player, WebsiteCore.FORM_TITLE,
                    WebsiteCore.websiteFormContent(),
                    List.of(WebsiteCore.FORM_BUTTONS),
                    new PlatformBridge.FormHandler() {
                        @Override
                        public void onSelect(int index) {
                            if (index == WebsiteCore.FORM_BACK && player.isOnline()) {
                                // Back (3.0.2, BB-01 FIX-2): return
                                // to the Server Menu.
                                serverMenuOpener.accept(player);
                            }
                            // Close: nothing to do
                        }

                        @Override
                        public void onClosedOrInvalid() {
                            // Closed: nothing to do.
                        }
                    });
            for (String line : WebsiteCore.websiteChatLines()) {
                player.sendMessage(line);
            }
            // Sent or not, the chat copy above is in hand either way.
            return;
        }
        player.sendMessage(Component.text("» Server Website: ", NamedTextColor.AQUA)
                .append(Component.text(WebsiteCore.WEBSITE_DISPLAY, NamedTextColor.AQUA,
                                TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(WebsiteCore.WEBSITE_URL))
                        .hoverEvent(HoverEvent.showText(
                                Component.text(WebsiteCore.LINK_HOVER)))));
        player.sendMessage(Component.text("» Short link: ", NamedTextColor.AQUA)
                .append(Component.text(WebsiteCore.WEBSITE_SHORT_DISPLAY,
                                NamedTextColor.AQUA, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(WebsiteCore.WEBSITE_SHORT_URL))
                        .hoverEvent(HoverEvent.showText(
                                Component.text(WebsiteCore.LINK_HOVER)))));
        player.sendMessage(WebsiteCore.JAVA_FOOTER);
    }
}
