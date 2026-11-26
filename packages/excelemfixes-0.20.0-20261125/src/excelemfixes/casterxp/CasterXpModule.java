package excelemfixes.casterxp;

import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import excelemfixes.shared.TextUtil;
import excelemfixes.skillsmenu.SkillsMenuModel;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * The caster-xp-sharing module: a per-player toggle for how
 * EliteMobs' two caster foundation skills (WANDS and
 * STAVES) share XP, plus a one-time catch-up that closes a
 * player's existing gap between them.
 *
 * WHY THIS MODULE EXISTS (player feedback + Mike's ruling):
 * on Nov 25, 2026 a player (Jester) reported his Spellcaster
 * class "paused at spellcaster 12 because staves is the
 * limiting requirement — the staff and wand exp dont share
 * nor do they level equally", after having to fight a
 * level-25 elite at an effective level of 10. Verified
 * against EliteMobs' own data: his WANDS XP was 240,760
 * (level 18) against STAVES 41,791 (level 12), and
 * EliteMobs' Advanced Combat System sets a class's level to
 * the LOWER of its two foundation skills and discards —
 * does not bank — class XP earned past that cap. There is
 * no EliteMobs setting for any of it (the system is alpha
 * with a fixed baseline). Mike's ruling the same day: let
 * the PLAYER choose how caster XP is shared — a toggle —
 * and close the existing gap once for the players already
 * caught by it.
 *
 * WHAT IT DOES: each player picks STOCK (EliteMobs
 * default, no intervention), SHARED (each Wands/Staves gain
 * is split across both tracks) or CARRY (gains on the
 * leading track also grant the same XP into the lagging
 * track, so the class minimum rises with the main weapon),
 * from a "Caster XP Sharing" row on the Wands and Staves
 * pages of /skillmenu, or with /casterxp. EliteMobs' class
 * formula itself is never edited — the module grants
 * offsetting skill XP through EliteMobs' own public
 * PlayerData calls (see EmCasterBridge), driven by
 * EliteMobs' own skill-XP-gain event (see CasterXpListener).
 * The first time a player picks a non-STOCK mode, a
 * one-time catch-up raises their lagging track to their
 * leading track's XP, recorded in the module's data file
 * so it can never fire twice. Mode changes apply
 * immediately, no relog. With the module off, or a player
 * in STOCK, EliteMobs behaves exactly as shipped.
 *
 * Scope: the WANDS/STAVES pair only — the reported case,
 * and the pair every Spellcaster-tree form in EliteMobs'
 * built-in catalog uses. EliteMobs gives OTHER classes
 * other foundation pairs (Paladin ARMOR+SWORDS and more);
 * those classes carry the same min() structure but are
 * deliberately NOT covered here (see the build record).
 */
public final class CasterXpModule implements FixModule, CommandExecutor {

    private ModuleContext ctx;
    private CasterXpState state = new CasterXpState();
    private CasterXpListener listener;
    private boolean active; // EliteMobs present + bridge probes
    private volatile boolean catchUpEnabled = true;
    private volatile boolean debug;
    private volatile String modeSetMessage =
            "&dCaster XP sharing set to &f%mode%&d. &7%blurb%";
    private volatile String catchUpMessage =
            "&dCaster catch-up: &eyour %track% XP was raised from"
                    + " %from% to %to% to match your %other% — a"
                    + " one-time fix, so your class is no longer held"
                    + " back by the old gap.";

    @Override
    public String id() {
        return "caster-xp-sharing";
    }

    @Override
    public String displayName() {
        return "Caster XP Sharing";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        loadConfig();
        loadState();
        ctx.renderer().registerHandler("casterxp", this::handleAction);
        ctx.registerCommand("casterxp", List.of(),
                "/casterxp [stock|shared|carry|status]", this);
        active = emPresent() && bridgeProbes();
        if (active) {
            listener = new CasterXpListener(state, ctx.logger());
            listener.setDebug(debug);
            Bukkit.getPluginManager().registerEvents(listener, ctx.plugin());
            SkillsMenuModel.setCasterSharingRowVisible(true);
            ctx.logger().info("Caster XP Sharing ready — per-player"
                    + " WANDS/STAVES sharing toggle (default mode "
                    + state.defaultMode() + ", catch-up "
                    + (catchUpEnabled ? "on" : "off") + ", "
                    + state.size() + " stored player record(s)).");
        } else {
            ctx.logger().warning("Caster XP Sharing: EliteMobs skill"
                    + " data did not answer; the module idles (no"
                    + " listener, no menu row) until EliteMobs is"
                    + " present and its skill data responds.");
        }
    }

    @Override
    public void onDisable() {
        SkillsMenuModel.setCasterSharingRowVisible(false);
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        if (ctx != null) ctx.renderer().unregisterHandler("casterxp");
        saveState();
        active = false;
    }

    @Override
    public void reload() {
        loadConfig();
        loadState();
        if (listener != null) listener.setDebug(debug);
    }

    // -------------------------------------------------------------- config

    private void loadConfig() {
        ConfigurationSection cfg = ctx.moduleConfig();
        catchUpEnabled = cfg == null || cfg.getBoolean("catch-up-enabled", true);
        debug = cfg != null && cfg.getBoolean("debug", false);
        state.setDefaultMode(CasterXpMath.Mode.parse(
                cfg == null ? null : cfg.getString("default-mode", "STOCK")));
        String msm = cfg == null ? null : cfg.getString("mode-set-message");
        if (msm != null && !msm.isBlank()) modeSetMessage = msm;
        String cum = cfg == null ? null : cfg.getString("catch-up-message");
        if (cum != null && !cum.isBlank()) catchUpMessage = cum;
    }

    // --------------------------------------------------------------- state

    private File stateFile() {
        return new File(ctx.plugin().getDataFolder(), "caster-xp-sharing.yml");
    }

    private void loadState() {
        // loadConfig ran first and parked the configured
        // default on the current store; carry it across the
        // swap so a reload never loses it.
        CasterXpMath.Mode configuredDefault = state.defaultMode();
        File file = stateFile();
        if (!file.isFile()) {
            state = new CasterXpState();
            state.setDefaultMode(configuredDefault);
            return;
        }
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection players = yaml.getConfigurationSection("players");
            Map<String, Object> map = new HashMap<>();
            if (players != null) {
                for (String key : players.getKeys(false)) {
                    ConfigurationSection row = players.getConfigurationSection(key);
                    if (row == null) continue;
                    Map<String, Object> values = new HashMap<>();
                    for (String field : row.getKeys(false)) {
                        values.put(field, row.get(field));
                    }
                    map.put(key, values);
                }
            }
            state = CasterXpState.fromMap(map);
            state.setDefaultMode(configuredDefault);
        } catch (Throwable t) {
            ctx.logger().log(Level.WARNING,
                    "Caster XP Sharing: state file unreadable, starting"
                            + " empty (stored modes lost, catch-ups may"
                            + " re-fire): " + t, t);
            state = new CasterXpState();
            state.setDefaultMode(configuredDefault);
        }
    }

    private void saveState() {
        if (ctx == null) return;
        try {
            File file = stateFile();
            file.getParentFile().mkdirs();
            YamlConfiguration yaml = new YamlConfiguration();
            ConfigurationSection players = yaml.createSection("players");
            for (Map.Entry<String, Object> e : state.toMap().entrySet()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> row = (Map<String, Object>) e.getValue();
                ConfigurationSection section =
                        players.createSection(e.getKey());
                for (Map.Entry<String, Object> field : row.entrySet()) {
                    section.set(field.getKey(), field.getValue());
                }
            }
            yaml.save(file);
        } catch (IOException | RuntimeException e) {
            ctx.logger().log(Level.WARNING,
                    "Caster XP Sharing: state save failed: " + e, e);
        }
    }

    // ------------------------------------------------------------- command

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Caster XP Sharing: " + state.size()
                    + " stored player record(s); default mode "
                    + state.defaultMode() + ". Players set their own"
                    + " mode in game.");
            return true;
        }
        if (!active) {
            player.sendMessage(TextUtil.component(
                    "&eCaster XP sharing is unavailable right now"
                            + " (EliteMobs skill data did not answer)."));
            return true;
        }
        if (args.length == 0) {
            ctx.renderer().openRoot(player, CasterXpView.view(snapshot(player)));
            return true;
        }
        String arg = args[0].toLowerCase();
        if (arg.equals("status")) {
            sendStatus(player);
            return true;
        }
        CasterXpMath.Mode mode = parseModeArg(arg);
        if (mode == null) {
            player.sendMessage(TextUtil.component(
                    "&eUsage: /casterxp [stock|shared|carry|status]"));
            return true;
        }
        applyMode(player, mode);
        return true;
    }

    private static CasterXpMath.Mode parseModeArg(String arg) {
        return switch (arg) {
            case "stock", "separate", "off" -> CasterXpMath.Mode.STOCK;
            case "shared", "pool", "pooled" -> CasterXpMath.Mode.SHARED;
            case "carry", "main" -> CasterXpMath.Mode.CARRY;
            default -> null;
        };
    }

    // -------------------------------------------------------------- actions

    /** Actions: casterxp:open | casterxp:set:<MODE>. */
    private void handleAction(Player player, String action) {
        if (!active) return;
        if (CasterXpView.isOpen(action)) {
            ctx.renderer().open(player, CasterXpView.view(snapshot(player)));
            return;
        }
        CasterXpMath.Mode mode = CasterXpView.parseSet(action);
        if (mode != null) {
            applyMode(player, mode);
            // Refresh the sharing screen in place (on Bedrock a
            // form closes on selection, so re-rendering is also
            // what keeps the player in the flow).
            ctx.renderer().openReplace(player,
                    CasterXpView.view(snapshot(player)));
        }
    }

    // ------------------------------------------------------------ behavior

    /**
     * Applies a player's mode choice: stores it (effective
     * immediately — the listener reads the store at event
     * time), fires the one-time catch-up on the first
     * non-STOCK choice when it is enabled and a grant is
     * owed AND lands, and tells the player what happened.
     */
    private void applyMode(Player player, CasterXpMath.Mode mode) {
        UUID uuid = player.getUniqueId();
        state.choose(uuid, mode);
        String catchUpNote = "";
        if (mode != CasterXpMath.Mode.STOCK && catchUpEnabled) {
            CasterXpState.PlayerState record = state.record(uuid);
            if (record != null && !record.catchUpDone) {
                catchUpNote = runCatchUp(player);
            }
        }
        saveState();
        player.sendMessage(TextUtil.component(modeSetMessage
                .replace("%mode%", CasterXpView.modeName(mode))
                .replace("%blurb%", CasterXpView.modeBlurb(mode))));
        if (!catchUpNote.isEmpty()) {
            player.sendMessage(TextUtil.component(catchUpNote));
        }
        if (debug) {
            ctx.logger().info("Caster XP Sharing: " + player.getName()
                    + " set mode " + mode);
        }
    }

    /**
     * The one-time catch-up: raises the lagging caster track
     * to the leading track's total through EliteMobs' own add
     * call. The done-flag is recorded ONLY when the grant
     * verifiably landed (the lagging total re-reads at the
     * leading total) or no grant was owed — if EliteMobs has
     * no loaded data for the player, nothing is recorded and
     * the catch-up stays owed rather than being spent on a
     * grant that never happened. Returns the player-facing
     * note ("" when no grant was owed).
     */
    private String runCatchUp(Player player) {
        UUID uuid = player.getUniqueId();
        long[] xp = EmCasterBridge.readCasterXp(uuid);
        long wands = xp[0], staves = xp[1];
        CasterXpMath.CatchUp plan = CasterXpMath.catchUp(wands, staves);
        if (plan.amount() <= 0 || plan.lagging() == null) {
            state.recordCatchUp(uuid, Instant.now().toString(), "", 0,
                    wands, staves);
            return "";
        }
        String leading = CasterXpMath.otherSkill(plan.lagging());
        long leadingTotal = Math.max(wands, staves);
        EmCasterBridge.addXp(uuid, plan.lagging(), plan.amount());
        long after = EmCasterBridge.readXp(uuid, plan.lagging());
        if (after < leadingTotal) {
            ctx.logger().warning("Caster XP Sharing: catch-up for "
                    + player.getName() + " did not land (" + plan.lagging()
                    + " reads " + after + ", expected " + leadingTotal
                    + "); leaving it owed, not recorded.");
            return "";
        }
        state.recordCatchUp(uuid, Instant.now().toString(), plan.lagging(),
                plan.amount(), wands, staves);
        ctx.logger().info("Caster XP Sharing: one-time catch-up for "
                + player.getName() + " — " + plan.lagging() + " +"
                + plan.amount() + " XP (WANDS " + wands + ", STAVES "
                + staves + " before).");
        long from = CasterXpMath.WANDS.equals(plan.lagging()) ? wands : staves;
        return catchUpMessage
                .replace("%track%", pretty(plan.lagging()))
                .replace("%other%", pretty(leading))
                .replace("%from%", CasterXpView.formatXp(from))
                .replace("%to%", CasterXpView.formatXp(after))
                .replace("%amount%", CasterXpView.formatXp(plan.amount()));
    }

    private CasterXpView.Snapshot snapshot(Player player) {
        UUID uuid = player.getUniqueId();
        long[] xp = EmCasterBridge.readCasterXp(uuid);
        CasterXpState.PlayerState record = state.record(uuid);
        return new CasterXpView.Snapshot(state.modeOf(uuid), xp[0],
                EmCasterBridge.levelOf(xp[0]), xp[1],
                EmCasterBridge.levelOf(xp[1]),
                record != null && record.catchUpDone);
    }

    private void sendStatus(Player player) {
        UUID uuid = player.getUniqueId();
        long[] xp = EmCasterBridge.readCasterXp(uuid);
        CasterXpMath.Mode mode = state.modeOf(uuid);
        player.sendMessage(TextUtil.component(
                "&dCaster XP sharing: &f" + CasterXpView.modeName(mode)));
        player.sendMessage(TextUtil.component(
                "&7Wands Lv " + EmCasterBridge.levelOf(xp[0]) + " — "
                        + CasterXpView.formatXp(xp[0]) + " XP · Staves Lv "
                        + EmCasterBridge.levelOf(xp[1]) + " — "
                        + CasterXpView.formatXp(xp[1]) + " XP"));
        player.sendMessage(TextUtil.component(
                "&7Change it with &f/casterxp shared&7, &f/casterxp carry"
                        + "&7 or &f/casterxp stock&7 — or from the Wands"
                        + " and Staves pages of &f/skillmenu&7."));
    }

    /** WANDS -> Wands (display only). */
    private static String pretty(String type) {
        if (type == null || type.isEmpty()) return "";
        return type.charAt(0) + type.substring(1).toLowerCase();
    }

    // -------------------------------------------------------------- helpers

    private boolean emPresent() {
        Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
        return em != null && em.isEnabled();
    }

    private boolean bridgeProbes() {
        try {
            return EmCasterBridge.probe();
        } catch (Throwable t) {
            return false;
        }
    }

    // Test/rig hooks.
    CasterXpState state() {
        return state;
    }

    boolean active() {
        return active;
    }
}
