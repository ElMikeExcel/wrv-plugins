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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * The class-xp-sharing module (module id caster-xp-sharing,
 * the id it shipped under in 0.20.0): a per-player toggle
 * for how the two foundation skills of a player's class
 * form share XP, plus a one-time catch-up per class tree
 * that closes a player's existing gap between them.
 *
 * WHY THIS MODULE EXISTS (player feedback + Mike's
 * rulings): on Nov 25, 2026 a player (Jester) reported his
 * Spellcaster class "paused at spellcaster 12 because
 * staves is the limiting requirement — the staff and wand
 * exp dont share nor do they level equally", after having
 * to fight a level-25 elite at an effective level of 10.
 * Verified against EliteMobs' own data: his WANDS XP was
 * 240,760 (level 18) against STAVES 41,791 (level 12), and
 * EliteMobs' Advanced Combat System sets a class's level
 * to the LOWER of its two foundation skills and discards —
 * does not bank — class XP earned past that cap. There is
 * no EliteMobs setting for any of it (the system is alpha
 * with a fixed baseline). Mike's ruling D242 the same day:
 * let the PLAYER choose how the two tracks share XP — a
 * toggle — and close the existing gap once for the players
 * already caught by it. That shipped in 0.20.0 for the
 * Spellcaster pair (WANDS + STAVES).
 *
 * Mike then asked what else of the same shape had been
 * missed. The hidden-mechanics audit that followed (Nov
 * 25, 2026; EliteMobs jar bytecode + live data) found the
 * min() + discard rule is UNIVERSAL: all 76 class forms in
 * EliteMobs' built-in catalog advance on the lower of two
 * foundation skills, the pairs CHANGE from form to form
 * in every tree except Spellcaster (Berserker's root pair
 * is AXES + MACES, Cleric's is MACES + TRIDENTS, Paladin's
 * is ARMOR + SWORDS...), and a second player was already
 * capped by it — Marvin's Paladin could not pass level 10
 * because his ARMOR (level 10) trailed his SWORDS (level
 * 13). Mike's ruling D243: extend the toggle to ALL
 * classes. That is this version: the pair is resolved
 * from the player's selected form against the audit's
 * probed catalog (ClassFormTable), and the one-time
 * catch-up fires once per class TREE.
 *
 * WHAT IT DOES: each player picks STOCK (EliteMobs
 * default, no intervention), SHARED (each gain on either
 * of their form's two foundation skills is split across
 * both tracks) or CARRY (gains on the leading track also
 * grant the same XP into the lagging track, so the class
 * minimum rises with the main weapon), from a "Class XP
 * Sharing" row on EVERY page of /skillmenu, or with
 * /classxp (alias /casterxp). The mode is ONE global
 * per-player setting; which pair it applies to follows
 * the player's selected form. EliteMobs' class formula
 * itself is never edited — the module grants offsetting
 * skill XP through EliteMobs' own public PlayerData calls
 * (see EmCasterBridge), driven by EliteMobs' own
 * skill-XP-gain event (see CasterXpListener). The first
 * time a player picks a non-STOCK mode with a form of a
 * tree selected, a one-time catch-up for that tree raises
 * the lagging track of their current form's pair to the
 * leading track's XP, recorded in the module's data file
 * so it can never fire twice for that tree. Mode changes
 * apply immediately, no relog. With the module off, or a
 * player in STOCK, EliteMobs behaves exactly as shipped.
 *
 * MIGRATION: 0.20.0's data file holds per-player modes
 * and Spellcaster catch-up records in a flat shape; the
 * state store converts them on load (see CasterXpState),
 * so a catch-up that fired under 0.20.0 never refires
 * and one still owed stays owed.
 */
public final class CasterXpModule implements FixModule, CommandExecutor {

    private ModuleContext ctx;
    private CasterXpState state = new CasterXpState();
    private CasterXpListener listener;
    private boolean active; // EliteMobs present + bridge probes
    private volatile boolean catchUpEnabled = true;
    private volatile boolean debug;
    private volatile String modeSetMessage =
            "&dClass XP sharing set to &f%mode%&d. &7%blurb%";
    private volatile String catchUpMessage =
            "&dClass catch-up: &eyour %track% XP was raised from"
                    + " %from% to %to% to match your %other% — a"
                    + " one-time fix for this class tree, so your"
                    + " class is no longer held back by the old gap.";

    @Override
    public String id() {
        return "caster-xp-sharing";
    }

    @Override
    public String displayName() {
        return "Class XP Sharing";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        loadConfig();
        loadState();
        ctx.renderer().registerHandler("casterxp", this::handleAction);
        ctx.registerCommand("classxp", List.of("casterxp"),
                "/classxp [stock|shared|carry|status]", this);
        active = emPresent() && bridgeProbes();
        if (active) {
            listener = new CasterXpListener(state, ctx.logger());
            listener.setDebug(debug);
            Bukkit.getPluginManager().registerEvents(listener, ctx.plugin());
            SkillsMenuModel.setCasterSharingRowVisible(true);
            ctx.logger().info("Class XP Sharing ready — per-player"
                    + " foundation-skill sharing toggle for every"
                    + " EliteMobs class form (default mode "
                    + state.defaultMode() + ", catch-up "
                    + (catchUpEnabled ? "on" : "off") + ", "
                    + state.size() + " stored player record(s)).");
        } else {
            ctx.logger().warning("Class XP Sharing: EliteMobs skill"
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
                    map.put(key, deepMap(row));
                }
            }
            state = CasterXpState.fromMap(map);
            state.setDefaultMode(configuredDefault);
        } catch (Throwable t) {
            ctx.logger().log(Level.WARNING,
                    "Class XP Sharing: state file unreadable, starting"
                            + " empty (stored modes lost, catch-ups may"
                            + " re-fire): " + t, t);
            state = new CasterXpState();
            state.setDefaultMode(configuredDefault);
        }
    }

    /** Converts a YAML section into plain nested maps (the
     *  state store's shape), recursing into sub-sections
     *  such as the per-tree catch-up records. */
    private static Map<String, Object> deepMap(ConfigurationSection section) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection sub) {
                out.put(key, deepMap(sub));
            } else {
                out.put(key, value);
            }
        }
        return out;
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
                writeDeep(players.createSection(e.getKey()), row);
            }
            yaml.save(file);
        } catch (IOException | RuntimeException e) {
            ctx.logger().log(Level.WARNING,
                    "Class XP Sharing: state save failed: " + e, e);
        }
    }

    /** Writes plain nested maps into a YAML section,
     *  recursing into sub-maps. */
    private static void writeDeep(ConfigurationSection section,
                                  Map<String, Object> map) {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            Object value = e.getValue();
            if (value instanceof Map<?, ?> sub) {
                Map<String, Object> child = new LinkedHashMap<>();
                for (Map.Entry<?, ?> ce : sub.entrySet()) {
                    child.put(String.valueOf(ce.getKey()), ce.getValue());
                }
                writeDeep(section.createSection(e.getKey()), child);
            } else {
                section.set(e.getKey(), value);
            }
        }
    }

    // ------------------------------------------------------------- command

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Class XP Sharing: " + state.size()
                    + " stored player record(s); default mode "
                    + state.defaultMode() + ". Players set their own"
                    + " mode in game.");
            return true;
        }
        if (!active) {
            player.sendMessage(TextUtil.component(
                    "&eClass XP sharing is unavailable right now"
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
                    "&eUsage: /classxp [stock|shared|carry|status]"));
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
     * time), fires the one-time catch-up for the tree of
     * the player's currently selected form on the first
     * non-STOCK choice for that tree when it is enabled and
     * a grant is owed AND lands, and tells the player what
     * happened. With no form selected there is no pair to
     * close, so the catch-up simply stays owed until a
     * non-STOCK choice is made with a form selected.
     */
    private void applyMode(Player player, CasterXpMath.Mode mode) {
        UUID uuid = player.getUniqueId();
        state.choose(uuid, mode);
        String catchUpNote = "";
        if (mode != CasterXpMath.Mode.STOCK && catchUpEnabled) {
            String formId = EmCasterBridge.activeFormId(uuid);
            String tree = formId == null
                    ? null : ClassFormTable.treeOf(formId);
            if (tree != null && !state.catchUpDone(uuid, tree)) {
                catchUpNote = runCatchUp(player, formId, tree);
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
            ctx.logger().info("Class XP Sharing: " + player.getName()
                    + " set mode " + mode);
        }
    }

    /**
     * One tree's one-time catch-up: raises the lagging
     * track of the player's CURRENT FORM's pair to the
     * leading track's total through EliteMobs' own add
     * call. The done-flag is recorded ONLY when the grant
     * verifiably landed (the lagging total re-reads at the
     * leading total) or no grant was owed — if EliteMobs
     * has no loaded data for the player, nothing is
     * recorded and the catch-up stays owed rather than
     * being spent on a grant that never happened. Returns
     * the player-facing note ("" when no grant was owed).
     */
    private String runCatchUp(Player player, String formId, String tree) {
        UUID uuid = player.getUniqueId();
        ClassFormTable.Form form = ClassFormTable.form(formId);
        if (form == null) return "";
        String skillA = form.skillA(), skillB = form.skillB();
        long aXp = EmCasterBridge.readXp(uuid, skillA);
        long bXp = EmCasterBridge.readXp(uuid, skillB);
        CasterXpMath.CatchUp plan =
                CasterXpMath.catchUp(aXp, bXp, skillA, skillB);
        CasterXpState.CatchUpRecord record = new CasterXpState.CatchUpRecord();
        record.at = Instant.now().toString();
        record.form = formId;
        record.skillA = skillA;
        record.skillB = skillB;
        record.beforeA = aXp;
        record.beforeB = bXp;
        if (plan.amount() <= 0 || plan.lagging() == null) {
            record.track = "";
            record.amount = 0;
            state.recordCatchUp(uuid, tree, record);
            return "";
        }
        String leading = CasterXpMath.otherInPair(skillA, skillB,
                plan.lagging());
        long leadingTotal = Math.max(aXp, bXp);
        EmCasterBridge.addXp(uuid, plan.lagging(), plan.amount());
        long after = EmCasterBridge.readXp(uuid, plan.lagging());
        if (after < leadingTotal) {
            ctx.logger().warning("Class XP Sharing: catch-up for "
                    + player.getName() + " (" + tree + ") did not land ("
                    + plan.lagging() + " reads " + after + ", expected "
                    + leadingTotal + "); leaving it owed, not recorded.");
            return "";
        }
        record.track = plan.lagging();
        record.amount = plan.amount();
        state.recordCatchUp(uuid, tree, record);
        ctx.logger().info("Class XP Sharing: one-time catch-up for "
                + player.getName() + " — " + tree + " tree, "
                + plan.lagging() + " +" + plan.amount() + " XP ("
                + skillA + " " + aXp + ", " + skillB + " " + bXp
                + " before).");
        long from = plan.lagging().equals(skillA) ? aXp : bXp;
        return catchUpMessage
                .replace("%track%", CasterXpView.pretty(plan.lagging()))
                .replace("%other%", CasterXpView.pretty(leading))
                .replace("%from%", CasterXpView.formatXp(from))
                .replace("%to%", CasterXpView.formatXp(after))
                .replace("%amount%", CasterXpView.formatXp(plan.amount()));
    }

    private CasterXpView.Snapshot snapshot(Player player) {
        UUID uuid = player.getUniqueId();
        CasterXpMath.Mode mode = state.modeOf(uuid);
        String formId = EmCasterBridge.activeFormId(uuid);
        ClassFormTable.Form form = formId == null
                ? null : ClassFormTable.form(formId);
        if (form == null) {
            return new CasterXpView.Snapshot(mode, null, "", 0, 0,
                    "", 0, 0, false);
        }
        long aXp = EmCasterBridge.readXp(uuid, form.skillA());
        long bXp = EmCasterBridge.readXp(uuid, form.skillB());
        String tree = ClassFormTable.treeOf(formId);
        return new CasterXpView.Snapshot(mode, form.displayName(),
                form.skillA(), aXp, EmCasterBridge.levelOf(aXp),
                form.skillB(), bXp, EmCasterBridge.levelOf(bXp),
                tree != null && state.catchUpDone(uuid, tree));
    }

    private void sendStatus(Player player) {
        UUID uuid = player.getUniqueId();
        CasterXpMath.Mode mode = state.modeOf(uuid);
        player.sendMessage(TextUtil.component(
                "&dClass XP sharing: &f" + CasterXpView.modeName(mode)));
        String formId = EmCasterBridge.activeFormId(uuid);
        ClassFormTable.Form form = formId == null
                ? null : ClassFormTable.form(formId);
        if (form == null) {
            player.sendMessage(TextUtil.component(
                    "&7No class selected — this setting applies to"
                            + " the two foundation skills of whichever"
                            + " class you select."));
        } else {
            long aXp = EmCasterBridge.readXp(uuid, form.skillA());
            long bXp = EmCasterBridge.readXp(uuid, form.skillB());
            player.sendMessage(TextUtil.component(
                    "&7" + form.displayName() + " — "
                            + CasterXpView.pretty(form.skillA()) + " Lv "
                            + EmCasterBridge.levelOf(aXp) + " — "
                            + CasterXpView.formatXp(aXp) + " XP · "
                            + CasterXpView.pretty(form.skillB()) + " Lv "
                            + EmCasterBridge.levelOf(bXp) + " — "
                            + CasterXpView.formatXp(bXp) + " XP"));
        }
        player.sendMessage(TextUtil.component(
                "&7Change it with &f/classxp shared&7, &f/classxp carry"
                        + "&7 or &f/classxp stock&7 — or from the Class"
                        + " XP Sharing row on any page of &f/skillmenu&7."));
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
