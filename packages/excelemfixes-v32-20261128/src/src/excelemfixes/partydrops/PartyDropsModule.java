package excelemfixes.partydrops;

import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * The party-drops module (module id party-drops): when a natural
 * EliteMobs elite dies, the players present at the death scale its
 * vanilla drop list — the fold of the standalone ExcelPartyDrops
 * 0.1.0 (renamed ExcelEMPartyDrops on live) into this pack
 * (consolidation programme Wave 3, ruling D245, Nov 25, 2026).
 * Behavior is 0.1.0's, ported class for class; the /partydrops
 * command name, its status|reload shape and its
 * excelpartydrops.admin permission node are exactly the
 * standalone's (frozen per programme gate 8).
 *
 * WHY THIS MODULE EXISTS (the REASON): Mike-approved spec,
 * Oct 20, 2026 — party play should make natural elites drop more
 * (1x-3x by party presence); solo stays exactly vanilla. Players
 * present at the death (alive, same world, within the 50-block
 * party radius) scale the elite's VANILLA drop list:
 * 1 + 0.5 x (participants - 1), capped at 3x, participant cap 5,
 * with per-stack probabilistic rounding that is exact in
 * expectation. EliteMobs' generated gear never passes through
 * the drop list and is untouched (EM already pays every >=10%
 * damager their own roll); the coin shower (0.25 dial, FINAL)
 * never enters the list either; dropped XP, Jobs and mcMMO are
 * untouched. A quiet, cooldown-gated action-bar hint sometimes
 * tells the group it paid off (Mike's decided visibility,
 * Oct 20, 2026 — the hint teaches the social loop).
 *
 * THE RANGE MATH IS A COPY, NOT A CALL: PartyRange.within()
 * re-implements ExcelTogether's PartyBonus.within() semantics
 * (squared distance, inclusive boundary) exactly, anchored on
 * the elite's death location. ExcelTogether exposes no callable
 * API and this module has NO Together dependency (Together
 * folds in programme wave 13; the copy stays a copy).
 *
 * OWNER-USABILITY (D246): the module is toggleable
 * (modules.party-drops.enabled) and config-driven (party-radius,
 * step-per-extra, multiplier-cap, participant-cap,
 * respect-farming-cap, hint-cooldown-seconds, debug). Shipped
 * defaults are the spec values — generic balance choices any
 * owner can change; no server-specific names, paths or content
 * IDs appear in code or shipped defaults. WRV runs the shipped
 * defaults unchanged (its live standalone config was diffed at
 * the fold gate: enabled true, radius 50.0, step 0.5, cap 3.0,
 * participant cap 5, hint cooldown 600, debug off).
 *
 * DEPENDENCY (programme gate 7 — self-contained pack): no new
 * hard depend. EliteMobs is reached through the pack's existing
 * softdepend: the EM listener class is loaded and registered
 * only when EliteMobs is present and enabled; absent, the
 * module logs one line, stays inert, and the pack boots clean.
 * No other cross-system leg exists (the Together range math is
 * a copy, not a seam).
 *
 * STATE: none. The module writes no files; with the module off,
 * /partydrops does not exist and elite drops are exactly
 * vanilla, solo and in a party alike.
 */
public final class PartyDropsModule implements FixModule, CommandExecutor, TabCompleter {

    public static final String PERM_ADMIN = "excelpartydrops.admin";

    private ModuleContext ctx;
    private volatile PartyDropsSettings settings = PartyDropsSettings.defaults();
    private volatile HintGate hintGate = new HintGate(
            PartyDropsSettings.DEFAULT_HINT_COOLDOWN_SECONDS * 1000L);
    private final Random random = new Random();
    private PartyDropsListener listener;
    private boolean emHooked = false;

    @Override
    public String id() {
        return "party-drops";
    }

    @Override
    public String displayName() {
        return "Party Drops";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        settings = loadSettings();
        Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
        if (em != null && em.isEnabled()) {
            // Only now is PartyDropsListener loaded — it is the
            // single class that references EliteMobs API types.
            listener = new PartyDropsListener(this);
            Bukkit.getPluginManager().registerEvents(listener, ctx.plugin());
            emHooked = true;
        }
        ctx.registerCommand("partydrops", List.of(),
                "/partydrops <status|reload>", this);
        ctx.logger().info("Party Drops ready: " + describe()
                + (emHooked ? ", EliteMobs hooked"
                            : ", EliteMobs NOT found - inert (no elite drops will be multiplied)"));
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        emHooked = false;
    }

    @Override
    public void reload() {
        settings = loadSettings();
    }

    /** The live settings snapshot (swapped whole on reload). */
    public PartyDropsSettings settingsSnapshot() {
        return settings;
    }

    HintGate hintGate() {
        return hintGate;
    }

    Random random() {
        return random;
    }

    Logger logger() {
        return ctx.logger();
    }

    boolean emHooked() {
        return emHooked;
    }

    private String describe() {
        PartyDropsSettings s = settings;
        return (s.enabled() ? "ON" : "OFF (master switch)")
                + ", radius " + s.partyRadius() + " blocks, step " + s.stepPerExtra()
                + ", multiplier cap " + s.multiplierCap()
                + ", participant cap " + s.participantCap();
    }

    private PartyDropsSettings loadSettings() {
        ConfigurationSection sec = ctx.moduleConfig();
        PartyDropsSettings s;
        if (sec == null) {
            s = PartyDropsSettings.defaults();
        } else {
            s = PartyDropsSettings.of(
                    sec.contains("enabled") ? sec.getBoolean("enabled") : null,
                    sec.contains("party-radius") ? sec.getDouble("party-radius") : null,
                    sec.contains("step-per-extra") ? sec.getDouble("step-per-extra") : null,
                    sec.contains("multiplier-cap") ? sec.getDouble("multiplier-cap") : null,
                    sec.contains("participant-cap") ? sec.getInt("participant-cap") : null,
                    sec.contains("respect-farming-cap") ? sec.getBoolean("respect-farming-cap") : null,
                    sec.contains("hint-cooldown-seconds") ? sec.getLong("hint-cooldown-seconds") : null,
                    sec.contains("debug") ? sec.getBoolean("debug") : null);
        }
        // The standalone rebuilt its hint gate on every settings
        // load (a reload restarts hint cooldowns); kept verbatim.
        hintGate = new HintGate(s.hintCooldownSeconds() * 1000L);
        if (s.respectFarmingCap()) {
            // Spec section 5: the key is reserved. In 0.1.0 it had
            // no effect — say so in the log rather than let a
            // config file lie (the standalone's warning, kept).
            ctx.logger().warning("party-drops: respect-farming-cap=true is reserved and NOT implemented"
                    + " - the multiplier behaves as if it were false (see config comments).");
        }
        return s;
    }

    // ---------------------------------------------------- /partydrops

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) {
            sender.sendMessage("You need the excelpartydrops.admin permission for that.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sender.sendMessage("ExcelPartyDrops: " + describe()
                    + ", EliteMobs " + (emHooked ? "hooked" : "not hooked (inert)"));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            ctx.plugin().reloadConfig();
            settings = loadSettings();
            sender.sendMessage("ExcelPartyDrops config reloaded: " + describe());
            return true;
        }
        sender.sendMessage("Usage: /partydrops status|reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission(PERM_ADMIN)) {
            return List.of("status", "reload").stream()
                    .filter(x -> x.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
