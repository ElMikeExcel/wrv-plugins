package excelemfixes.clarity;

import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import excelemfixes.shared.TextUtil;
import excelemfixes.shared.Views;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The clarity-menus module: /emguide opens "EliteMobs, Made
 * Clear", our own plain-language front door for EliteMobs
 * itself — Skills &amp; skill bonuses (with the click-the-skill
 * claiming step EliteMobs never states), Quests (including the
 * safe way to check a quest), Shops &amp; Elite Coins, and the
 * gear tools. Route buttons close our menu and run the matching
 * EliteMobs command as the player (the tracker's performCommand
 * pattern), so each explanation sits one tap away from the real
 * screen — except routes with an em-menu target, which open an
 * EliteMobs screen directly through EliteMobs' own opener (the
 * skill-bonus claim screens have no command route: EliteMobs'
 * normal /em Skills page is a view-only text page). All wording
 * lives in clarity-menus.yml (bundled; a copy in the plugin data
 * folder overrides it).
 */
public final class ClarityMenusModule implements FixModule, CommandExecutor {

    private ModuleContext ctx;
    private ClarityMenus menus;

    @Override
    public String id() {
        return "clarity-menus";
    }

    @Override
    public String displayName() {
        return "Clarity Menus";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        if (!loadText()) {
            throw new IllegalStateException("clarity-menus text failed to load");
        }
        ctx.renderer().registerHandler("clarity", this::handleAction);
        ctx.registerCommand("emguide", List.of(), "/emguide [page]", this);
        ctx.logger().info("Clarity Menus ready — " + menus.text().pages().size()
                + " pages, " + menus.text().routes().size() + " routes.");
    }

    @Override
    public void onDisable() {
        if (ctx != null) ctx.renderer().unregisterHandler("clarity");
    }

    @Override
    public void reload() {
        loadText();
    }

    private boolean loadText() {
        File external = new File(ctx.plugin().getDataFolder(),
                "clarity-menus.yml");
        try {
            ClarityText text;
            if (external.isFile()) {
                try (InputStream in = new FileInputStream(external)) {
                    text = ClarityText.load(new InputStreamReader(in,
                            StandardCharsets.UTF_8));
                }
                ctx.logger().info("Clarity Menus: data-folder clarity-menus.yml in use");
            } else {
                try (InputStream in =
                             ctx.plugin().getResource("clarity-menus.yml")) {
                    if (in == null) {
                        ctx.logger().severe("Clarity Menus: bundled clarity-menus.yml missing");
                        return false;
                    }
                    text = ClarityText.load(new InputStreamReader(in,
                            StandardCharsets.UTF_8));
                }
            }
            menus = new ClarityMenus(text);
            return true;
        } catch (Exception e) {
            ctx.logger().severe("Clarity Menus: text load failed: " + e);
            return false;
        }
    }

    // ------------------------------------------------------------- command

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("The /emguide menus are for players.");
            return true;
        }
        String pageId = args.length > 0 ? args[0].toLowerCase() : "home";
        Views.View view = menus.pageView(pageId);
        if (view == null) view = menus.pageView("home");
        ctx.renderer().openRoot(player, view);
        return true;
    }

    // ---------------------------------------------------------- navigation

    /** Actions: clarity:page:<id> | clarity:route:<id>. */
    private void handleAction(Player player, String action) {
        String[] parts = action.split(":", 3);
        if (parts.length < 3) return;
        switch (parts[1]) {
            case "page" -> {
                Views.View view = menus.pageView(parts[2]);
                if (view != null) ctx.renderer().open(player, view);
            }
            case "route" -> {
                ClarityText.Route route = menus.text().route(parts[2]);
                if (route == null) return;
                if (route.hint() != null && !route.hint().isBlank()) {
                    player.sendMessage(TextUtil.component(route.hint()));
                }
                player.closeInventory();
                if (route.emMenu() != null) {
                    openEmMenu(player, route.emMenu());
                } else {
                    player.performCommand(route.command());
                }
            }
            default -> { /* unknown sub-action: ignore */ }
        }
    }

    /**
     * Opens one of EliteMobs' own screens directly (the route's
     * em-menu target). The open runs next tick — EliteMobs' own
     * menu code does the same after a click, so the inventory we
     * just closed is fully settled first. If EliteMobs is absent
     * or a future EliteMobs moved the opener, the player gets
     * the manual alt-menu path instead of a dead button.
     */
    private void openEmMenu(Player player, String target) {
        Bukkit.getScheduler().runTask(ctx.plugin(), () -> {
            Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
            if (em == null || !em.isEnabled()) {
                sendEmMenuFallback(player);
                return;
            }
            try {
                switch (target) {
                    case "skill-bonus" -> EmMenuBridge.openSkillBonusMenu(player);
                    default -> sendEmMenuFallback(player);
                }
            } catch (Throwable t) {
                ctx.logger().warning("Clarity Menus: EliteMobs menu '"
                        + target + "' failed to open: " + t);
                sendEmMenuFallback(player);
            }
        });
    }

    private void sendEmMenuFallback(Player player) {
        player.sendMessage(TextUtil.component("&eThat screen would not open."
                + " The manual path: run &f/elitemobs alt&e once, then &f/em&e,"
                + " choose &fSkills&e, and click &fthe skill itself&e."));
    }

    /** Test/rig hook. */
    public ClarityMenus menus() {
        return menus;
    }
}
