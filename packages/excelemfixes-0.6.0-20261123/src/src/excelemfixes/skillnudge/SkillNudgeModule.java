package excelemfixes.skillnudge;

import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import excelemfixes.shared.GeyserForms;
import excelemfixes.shared.TextUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * Skill Bonus Nudge module: tells a player, once per session,
 * when their EliteMobs skill levels have earned skill bonuses
 * they never claimed.
 *
 * <p>The defect: EliteMobs' skill system grants real power —
 * the Armor line alone holds Battle Hardened, Second Wind and
 * Last Stand — but nothing anywhere tells a player the picks
 * exist or that theirs are sitting unclaimed. Observed live
 * (Nov 22, 2026): a player with ARMOR level 14 and WANDS
 * level 17 had selected ZERO bonuses in every skill, and was
 * dying to burst damage the unclaimed Armor picks exist to
 * answer. The menu to claim them is three screens deep inside
 * EliteMobs' status screen, and EM never points at it.
 *
 * <p>The fix: on join (delayed a few seconds so EM has loaded
 * the player's data and the login burst has settled), read the
 * player's skill levels and selections through EliteMobs' own
 * public API (see {@link EmSkillData}), compare them against
 * EM's own bonus catalog (see {@link BonusCatalog}) with the
 * pure rules in {@link SkillNudgeMath}, and — only when a pick
 * is genuinely claimable — send the configured message naming
 * the count and the claim path. At most once per session
 * (one join-to-quit connection); a player who claims their
 * bonuses simply stops qualifying.
 *
 * <p>On Java the message is clickable: clicking any line runs
 * the configured {@code click-command} (default
 * {@code /elitemobs}, the menu the message names), so the
 * claim path is one tap from the nudge. Bedrock players get
 * no click events through Geyser, so they receive the plain
 * message with the command spelled out in the text.
 *
 * <p>Scope: deliberately NOT world-gated. The check is about
 * the player's account-wide EM progression, evaluated once at
 * join; the join world is the overworld for everyone, so a
 * world gate would silence the nudge for exactly the players
 * it exists for. The module changes no gameplay anywhere — it
 * reads EM data and sends chat.
 *
 * <p>EliteMobs is a softdepend: without it there is nothing
 * to check, and the module idles without loading any EM class
 * (all EM access lives behind {@link EmSkillData}, first
 * touched only after the presence check passes).
 */
public final class SkillNudgeModule implements FixModule, Listener {

    private ModuleContext ctx;
    private volatile boolean debug;
    private volatile long delayTicks = 100L;
    private volatile int[] tierLevels = SkillNudgeMath.DEFAULT_TIER_LEVELS.clone();
    private volatile int maxActive = SkillNudgeMath.DEFAULT_MAX_ACTIVE;
    private volatile List<String> messageLines = List.of();
    private volatile String clickCommand = "/elitemobs";
    private volatile Map<String, int[]> catalog = Map.of();
    private final Set<UUID> checkedThisSession = new HashSet<>();

    @Override
    public String id() {
        return "skill-bonus-nudge";
    }

    @Override
    public String displayName() {
        return "Skill Bonus Nudge";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        load();
        if (Bukkit.getPluginManager().getPlugin("EliteMobs") == null) {
            ctx.logger().info("Skill Bonus Nudge: EliteMobs plugin not found — module idles "
                    + "(no skill data to check). It starts checking the moment "
                    + "EliteMobs is installed.");
            return;
        }
        refreshCatalog();
        Bukkit.getPluginManager().registerEvents(this, ctx.plugin());
        ctx.logger().info("Skill Bonus Nudge ready — join check after "
                + delayTicks + " ticks, tiers at levels "
                + java.util.Arrays.toString(tierLevels)
                + ", max " + maxActive + " active per skill.");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        checkedThisSession.clear();
    }

    @Override
    public void reload() {
        load();
        if (Bukkit.getPluginManager().getPlugin("EliteMobs") != null) {
            refreshCatalog();
        }
    }

    private void load() {
        ConfigurationSection cfg = ctx.moduleConfig();
        debug = cfg != null && cfg.getBoolean("debug", false);
        delayTicks = cfg == null ? 100L : Math.max(0L, cfg.getLong("delay-ticks", 100L));
        maxActive = cfg == null ? SkillNudgeMath.DEFAULT_MAX_ACTIVE
                : Math.max(1, cfg.getInt("max-active-per-skill",
                        SkillNudgeMath.DEFAULT_MAX_ACTIVE));
        List<Integer> tiers = cfg == null ? List.of()
                : cfg.getIntegerList("tier-unlock-levels");
        if (tiers.size() == 4) {
            int[] t = new int[4];
            for (int i = 0; i < 4; i++) t[i] = Math.max(1, tiers.get(i));
            tierLevels = t;
        } else {
            tierLevels = SkillNudgeMath.DEFAULT_TIER_LEVELS.clone();
        }
        List<String> lines = cfg == null ? List.of() : cfg.getStringList("message");
        messageLines = lines.isEmpty() ? defaultMessage() : List.copyOf(lines);
        String cc = cfg == null ? "/elitemobs" : cfg.getString("click-command", "/elitemobs");
        clickCommand = cc == null ? "" : cc.strip();
    }

    private static List<String> defaultMessage() {
        return List.of(
                "&6&lSkill bonuses waiting! &eYou have &f%count% &eunclaimed "
                        + "EliteMobs skill bonus(es) — your &f%topskill% &elevels earned them.",
                "&7Open &f/elitemobs&7, choose &fSkills&7, and activate your picks. "
                        + "Armor picks like Battle Hardened and Last Stand make elites hit softer.");
    }

    private void refreshCatalog() {
        Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
        if (em == null) return;
        File folder = new File(em.getDataFolder(), "skillbonuses");
        catalog = BonusCatalog.read(folder, ctx.logger());
    }

    // ------------------------------------------------------------ join check

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(ctx.plugin(), () -> {
            if (!player.isOnline()) return;
            if (!checkedThisSession.add(uuid)) return; // once per session
            check(player);
        }, delayTicks);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        checkedThisSession.remove(event.getPlayer().getUniqueId());
    }

    private void check(Player player) {
        try {
            Map<String, Integer> levels =
                    EmSkillData.readLevels(player.getUniqueId(), ctx.logger());
            if (levels.isEmpty()) return; // EM has no data — nothing to judge
            Map<String, Integer> selected =
                    EmSkillData.readSelectedCounts(player.getUniqueId(), ctx.logger());
            if (selected == null) return; // unknown — never cry wolf
            Map<String, int[]> cat = catalog.isEmpty() ? null : catalog;
            SkillNudgeMath.Evaluation eval = SkillNudgeMath.evaluate(
                    levels, selected, cat, tierLevels, maxActive);
            if (debug) {
                ctx.logger().info("Skill Bonus Nudge: " + player.getName()
                        + " levels=" + levels + " selected=" + selected
                        + " claimable=" + eval.totalClaimable()
                        + " perSkill=" + eval.perSkill());
            }
            if (eval.totalClaimable() <= 0) return;
            String top = eval.topSkill() == null ? "" : prettySkill(eval.topSkill());
            // Java: the whole nudge is clickable — one tap runs the
            // claim-path command. Bedrock (Geyser) has no chat click
            // events, so those players keep the plain spelled-out text.
            boolean clickable = !clickCommand.isEmpty()
                    && !GeyserForms.isBedrockPlayer(player, ctx.logger());
            for (String line : messageLines) {
                Component msg = TextUtil.component(line
                        .replace("%count%", String.valueOf(eval.totalClaimable()))
                        .replace("%topskill%", top)
                        .replace("%player%", player.getName()));
                if (clickable) {
                    msg = msg.clickEvent(ClickEvent.runCommand(clickCommand))
                            .hoverEvent(HoverEvent.showText(
                                    Component.text("Click to open " + clickCommand)));
                }
                player.sendMessage(msg);
            }
            ctx.logger().info("Skill Bonus Nudge: told " + player.getName()
                    + " about " + eval.totalClaimable()
                    + " unclaimed skill bonus(es) " + eval.perSkill());
        } catch (Throwable t) {
            ctx.logger().warning("Skill Bonus Nudge: check failed for "
                    + player.getName() + ": " + t);
        }
    }

    /** ARMOR -> Armor, WANDS -> Wands (display only). */
    static String prettySkill(String type) {
        if (type == null || type.isEmpty()) return "";
        return type.charAt(0) + type.substring(1).toLowerCase();
    }

    // Test hook: the session set, so rig drivers can observe state size.
    int sessionsChecked() {
        return checkedThisSession.size();
    }

    // Kept for parity with other modules' config sections.
    List<String> messageSnapshot() {
        return new ArrayList<>(messageLines);
    }
}
