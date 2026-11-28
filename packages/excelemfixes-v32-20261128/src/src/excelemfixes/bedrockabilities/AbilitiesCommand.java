package excelemfixes.bedrockabilities;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * /abilities (alias /classabilities) - ported verbatim in
 * behavior from the standalone bridge 0.2.0.
 *
 * Bedrock players get the Cumulus form built from their live
 * EliteMobs class state. Java players keep EliteMobs' native
 * F-key flow untouched and just get a one-line pointer to it.
 * If the form cannot be sent (no Geyser connection), the
 * player still sees their ability list in chat - read-only,
 * since the bridge fires only from the form, exactly as
 * specced.
 *
 * /abilities status - a global, read-only status (EliteMobs /
 * Geyser link state) routed BEFORE the player-only gate,
 * runnable from the console and gated by
 * exceleitemobsabilitybedrockbridge.admin (default op).
 * Per-player ability state belongs to EliteMobs, not the
 * bridge, so a global status is the whole console-admin
 * surface.
 */
public final class AbilitiesCommand implements CommandExecutor {

    private final BedrockAbilitiesModule module;

    AbilitiesCommand(BedrockAbilitiesModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // The console-admin status is routed BEFORE the
        // player-only gate - it is the one form a non-player
        // sender (the console) must be able to run.
        if (args.length >= 1 && StatusCommand.isStatus(args[0])) {
            handleStatus(sender);
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("The ability bridge is for players in game.");
            return true;
        }
        AcsGateway gateway = module.gateway();
        if (gateway == null || !gateway.moduleReady()) {
            player.sendMessage("Class abilities are unavailable right now.");
            return true;
        }
        if (!BridgeForms.isBedrockPlayer(player)) {
            player.sendMessage("Your class abilities are on your keyboard: press F, then "
                    + "F for Mobility, left-click for Signature, right-click for Utility. "
                    + "This menu is the Bedrock bridge.");
            return true;
        }

        ClassView view = gateway.classView(player);
        FormModel model = FormModelBuilder.build(view);
        boolean sent = BridgeForms.sendForm(player, model,
                index -> {
                    BridgeSlot slot = model.slotForButton(index);
                    if (slot != null) {
                        module.fireAbility(player, slot);
                    }
                },
                module.logger());
        if (!sent) {
            // Fail-soft: show the same content as chat lines.
            // The player can read their abilities; firing
            // stays form-only.
            for (String line : model.content().split("\n")) {
                if (!line.isBlank()) {
                    player.sendMessage(line);
                }
            }
            player.sendMessage("(The ability menu could not open - try again in a moment.)");
        }
        return true;
    }

    /**
     * /abilities status: the global console-admin surface. Gate
     * first (console always; anyone else needs the admin node),
     * then the read-only link-state lines. The EliteMobs facts
     * come from the plugin manager; the ACS fact comes through
     * the bridge's own gateway seam (moduleReady), the same
     * probe the player path trusts. Fires nothing.
     */
    private void handleStatus(CommandSender sender) {
        boolean console = !(sender instanceof Player);
        if (!StatusCommand.isAdmin(console, sender.hasPermission(StatusCommand.PERMISSION))) {
            sender.sendMessage(StatusCommand.DENIED);
            return;
        }
        Plugin eliteMobs = module.plugin().getServer().getPluginManager().getPlugin("EliteMobs");
        boolean emEnabled = eliteMobs != null && eliteMobs.isEnabled();
        String emVersion = eliteMobs == null ? null : eliteMobs.getDescription().getVersion();
        AcsGateway gateway = module.gateway();
        boolean acsReady = gateway != null && gateway.moduleReady();
        Plugin geyser = module.plugin().getServer().getPluginManager().getPlugin("Geyser-Spigot");
        Plugin floodgate = module.plugin().getServer().getPluginManager().getPlugin("floodgate");
        for (String line : StatusCommand.statusLines(
                module.version(),
                emEnabled, emVersion, acsReady,
                geyser != null && geyser.isEnabled(),
                floodgate != null && floodgate.isEnabled(),
                module.debugLogFires())) {
            sender.sendMessage(line);
        }
    }
}
