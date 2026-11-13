package com.winkreyes.excelskillspanel;

import com.winkreyes.excelskillspanel.bukkit.PanelMenuListener;
import com.winkreyes.excelskillspanel.bukkit.PanelMenus;
import com.winkreyes.excelskillspanel.core.PanelModel;
import com.winkreyes.excelskillspanel.provider.SkillsProvider;
import com.winkreyes.excelskillspanel.provider.mcmmo.McMMoProvider;
import com.winkreyes.excelskillspanel.provider.wrv.MagicHeaderProvider;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * ExcelSkillsPanel — the unified Skills panel (menu programme
 * item 2, DESIGN §7.3): one panel with a character header and
 * per-skill rows with levels and XP progress bars, MMO-style, on
 * both platforms (Java chest / Bedrock Cumulus form), built on
 * the ExcelSummons one-model-two-presentations pattern.
 *
 * The panel is its OWN plugin, an mcMMO companion built on
 * mcMMO's public API through a provider seam (softdepends — no
 * mcMMO code copied, so mcMMO updates carry the panel):
 * - provider-mcmmo supplies the 19 skill rows + Power Level.
 * - provider-magic (WRV-only) supplies the FULL Magic header.
 * A provider that is absent simply contributes nothing; the
 * panel still opens with whatever answered.
 *
 * /skillpanel opens the panel. The typed /magiclevel output
 * remains ExcelMagicLevel's chat surface and this panel's
 * conceptual fallback, unchanged.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class ExcelSkillsPanel extends JavaPlugin implements CommandExecutor {

    private final List<SkillsProvider> providers = new ArrayList<>();

    @Override
    public void onEnable() {
        if (enabled("mcMMO")) {
            providers.add(new McMMoProvider(getLogger()));
        }
        if (enabled("Magic") || enabled("ExcelMagicLevel") || enabled("ExcelGuildGate")) {
            providers.add(new MagicHeaderProvider(getLogger()));
        }
        getServer().getPluginManager().registerEvents(new PanelMenuListener(this), this);
        if (getCommand("skillpanel") != null) {
            getCommand("skillpanel").setExecutor(this);
        }
        List<String> ids = new ArrayList<>();
        for (SkillsProvider p : providers) ids.add(p.id());
        getLogger().info("ExcelSkillsPanel " + BuildInfo.VERSION + " awake (build "
                + BuildInfo.BUILD_ID + "); providers: "
                + (ids.isEmpty() ? "none — the panel will open empty" : String.join(", ", ids)));
    }

    private boolean enabled(String pluginName) {
        Plugin p = getServer().getPluginManager().getPlugin(pluginName);
        return p != null && p.isEnabled();
    }

    /** The panel model for one player: every registered
     *  provider's contribution, in registration order (mcMMO
     *  first, then the WRV Magic module). */
    public PanelModel.Panel panelFor(Player player) {
        List<PanelModel.HeaderBlock> headers = new ArrayList<>();
        List<PanelModel.SkillRow> rows = new ArrayList<>();
        List<String> magicDetail = List.of();
        for (SkillsProvider provider : providers) {
            SkillsProvider.ProviderData data = provider.collect(player);
            headers.addAll(data.headers());
            rows.addAll(data.rows());
            if (!data.magicDetailLines().isEmpty()) magicDetail = data.magicDetailLines();
        }
        return new PanelModel.Panel(headers, rows, magicDetail);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("The skills panel is a player screen — run /skillpanel in game.");
            return true;
        }
        PanelMenus.open(this, player);
        return true;
    }
}
