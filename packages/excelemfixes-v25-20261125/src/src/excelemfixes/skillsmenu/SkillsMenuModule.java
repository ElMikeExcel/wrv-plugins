package excelemfixes.skillsmenu;

import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import excelemfixes.clarity.EmMenuBridge;
import excelemfixes.shared.TextUtil;
import excelemfixes.shared.Views;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * The skills-menu module: /skillmenu (alias /skills) opens
 * "My Skills &amp; Skill Bonuses" — OUR native screen for
 * EliteMobs' skill bonuses, one menu definition rendered as
 * a Java chest GUI or a Bedrock form by the pack's shared
 * renderer, the way every Excel menu works.
 *
 * Why this exists (player feedback): on Nov 24, 2026 Jester
 * reported that EliteMobs' normal /em menu shows skills as a
 * view-only text list — no skill passives, no way to switch
 * a bonus on; the only working screen was EliteMobs' alt
 * chest menu, which Bedrock players get as a clunky
 * container. Mike's direction the same day: build the
 * skills screen ourselves with the appropriate format per
 * platform, like our other menus. The toggles are not ours —
 * they are performed through EliteMobs' own selection calls
 * and activation hooks (see EmSkillsBridge), so the state is
 * EliteMobs' own and its other screens agree with ours.
 *
 * Fallback: if EliteMobs is absent, or a future EliteMobs
 * moves the classes this module bridges to, opening the
 * menu degrades to EliteMobs' own chest claim screens (the
 * clarity module's route), and failing that to the printed
 * manual path — never to an error in a player's face.
 */
public final class SkillsMenuModule implements FixModule, CommandExecutor {

    private ModuleContext ctx;

    @Override
    public String id() {
        return "skills-menu";
    }

    @Override
    public String displayName() {
        return "Skills Menu";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        ctx.renderer().registerHandler("skillsmenu", this::handleAction);
        ctx.registerCommand("skillmenu", List.of("skills"),
                "/skillmenu", this);
        if (emPresent() && bridgeProbes()) {
            ctx.logger().info("Skills Menu ready — native skill-bonus"
                    + " screens (Java GUI + Bedrock form).");
        } else {
            ctx.logger().warning("Skills Menu: EliteMobs skill-bonus"
                    + " machinery did not answer; /skillmenu will fall"
                    + " back to EliteMobs' own claim screens.");
        }
    }

    @Override
    public void onDisable() {
        if (ctx != null) ctx.renderer().unregisterHandler("skillsmenu");
    }

    // ------------------------------------------------------------- command

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("The /skillmenu menus are for players.");
            return true;
        }
        openOverview(player, true);
        return true;
    }

    // ------------------------------------------------------------ actions

    /** Actions: skillsmenu:open:<TYPE> | skillsmenu:toggle:<TYPE>:<id>. */
    private void handleAction(Player player, String action) {
        String openType = SkillsMenuModel.parseOpen(action);
        if (openType != null) {
            SkillsMenuModel.SkillState skill = readSkill(player, openType);
            if (skill == null) {
                openOverview(player, false);
            } else {
                ctx.renderer().open(player,
                        SkillsMenuModel.skillView(skill));
            }
            return;
        }
        String[] toggle = SkillsMenuModel.parseToggle(action);
        if (toggle != null) {
            if (!emPresent()) {
                fallback(player);
                return;
            }
            try {
                EmSkillsBridge.toggle(player, toggle[0], toggle[1]);
            } catch (Throwable t) {
                ctx.logger().warning("Skills Menu: toggle failed for "
                        + player.getName() + ": " + t);
                fallback(player);
                return;
            }
            SkillsMenuModel.SkillState skill = readSkill(player, toggle[0]);
            if (skill == null) {
                openOverview(player, false);
            } else {
                // Refresh the screen in place (the renderer stack
                // keeps Back pointed at the overview) — EliteMobs'
                // own screen reopens itself after a toggle too.
                // On Bedrock this also keeps the form alive: a
                // form closes on selection, so not re-rendering
                // would strand the player with no menu at all.
                ctx.renderer().openReplace(player,
                        SkillsMenuModel.skillView(skill));
            }
        }
    }

    // -------------------------------------------------------------- reads

    private void openOverview(Player player, boolean root) {
        if (!emPresent()) {
            fallback(player);
            return;
        }
        List<SkillsMenuModel.SkillState> skills;
        try {
            skills = EmSkillsBridge.readAll(player);
        } catch (Throwable t) {
            ctx.logger().warning("Skills Menu: state read failed for "
                    + player.getName() + ": " + t);
            fallback(player);
            return;
        }
        Views.View view = SkillsMenuModel.overviewView(skills);
        if (root) {
            ctx.renderer().openRoot(player, view);
        } else {
            ctx.renderer().openReplace(player, view);
        }
    }

    /** Fresh state for one skill; null when EliteMobs cannot
     *  answer (the caller falls back instead of erroring). */
    private SkillsMenuModel.SkillState readSkill(Player player,
                                                 String typeName) {
        if (!emPresent()) return null;
        try {
            return EmSkillsBridge.readSkill(player, typeName);
        } catch (Throwable t) {
            ctx.logger().warning("Skills Menu: state read failed for "
                    + player.getName() + ": " + t);
            return null;
        }
    }

    // ------------------------------------------------------------ fallback

    private boolean emPresent() {
        Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
        return em != null && em.isEnabled();
    }

    private boolean bridgeProbes() {
        try {
            return EmSkillsBridge.probe();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Degrade to EliteMobs' own chest claim screens (the
     * clarity module's 0.13.0 route); if even that opener is
     * gone, print the manual path. The open runs next tick so
     * any menu of ours the player just clicked is fully
     * settled first.
     */
    private void fallback(Player player) {
        if (!emPresent()) {
            sendManualPath(player);
            return;
        }
        player.closeInventory();
        Bukkit.getScheduler().runTask(ctx.plugin(), () -> {
            try {
                EmMenuBridge.openSkillBonusMenu(player);
            } catch (Throwable t) {
                ctx.logger().warning("Skills Menu: fallback EliteMobs"
                        + " screen failed to open: " + t);
                sendManualPath(player);
            }
        });
    }

    private void sendManualPath(Player player) {
        player.sendMessage(TextUtil.component("&eThat screen would not open."
                + " The manual path: run &f/elitemobs alt&e once, then &f/em&e,"
                + " choose &fSkills&e, and click &fthe skill itself&e."));
    }
}
