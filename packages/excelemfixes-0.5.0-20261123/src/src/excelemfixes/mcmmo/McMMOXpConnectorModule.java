package excelemfixes.mcmmo;

import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

/**
 * mcMMO XP Connector module: rebalances mcMMO XP for EliteMobs
 * content — per-damage XP from EliteMobs entities is replaced
 * by a fixed kill award priced by the entity's own EliteMobs
 * level and class-loot rank, under a hard hourly ceiling.
 *
 * <p>The defect: mcMMO prices combat XP per point of damage,
 * settled per hit, with no per-kill cap. Against vanilla-sized
 * health pools that is sane; against EliteMobs pools (a level
 * 100 elite carries millions of HP) the pool IS the payout —
 * a target that never dies pays indefinitely, which is how a
 * guild training dummy became an XP farm. The blunt fix
 * (zeroing all mcMMO XP in EliteMobs worlds) stops the farm
 * but also pays nothing for real EliteMobs play.
 *
 * <p>The fix, in three parts (details in the config section
 * and the README): (1) a veto — every player/pet hit on an
 * EliteMobs entity plants a provenance token, and mcMMO gains
 * matching a token (the earner's PVE gains and the party
 * shares derived from them) are zeroed on mcMMO's gain event;
 * (2) a kill award — when an EliteMobs entity dies to a
 * player (or a player's adopted pet), the killer earns
 * rate x level in the killing weapon's skill, granted through
 * mcMMO's normal gain pipeline; (3) a ceiling — award XP is
 * capped per player per skill per rolling hour, counted on
 * the final granted (post-multiplier) amount and persisted
 * across restarts. Recognised no-XP entities (training
 * dummies, managed summons, denied entities) and passive
 * practice targets earn no award, ever.
 *
 * <p>Both partner plugins are soft dependencies: without
 * EliteMobs there is nothing to rebalance, and without mcMMO
 * there is nothing to pay — if either is missing the module
 * stands down with one log line and loads none of the bridge
 * classes.
 */
public final class McMMOXpConnectorModule implements FixModule {

    private ModuleContext ctx;
    private KseaEngine engine;
    private McMMOBridge bridge;

    @Override
    public String id() {
        return "mcmmo-xp-connector";
    }

    @Override
    public String displayName() {
        return "mcMMO XP Connector";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
        Plugin mcmmo = Bukkit.getPluginManager().getPlugin("mcMMO");
        boolean emOk = em != null && em.isEnabled();
        boolean mcmmoOk = mcmmo != null && mcmmo.isEnabled();
        if (!emOk || !mcmmoOk) {
            String missing = !emOk && !mcmmoOk ? "EliteMobs and mcMMO"
                    : !emOk ? "EliteMobs" : "mcMMO";
            ctx.logger().info("mcMMO XP Connector: standing down — "
                    + missing + " not found. This module rebalances "
                    + "mcMMO XP for EliteMobs content and needs both "
                    + "plugins; with either missing it does nothing, "
                    + "and no other module is affected.");
            return;
        }
        KseaEngine.Settings settings =
                KseaEngine.Settings.from(ctx.moduleConfig());
        EmIdentity identity = new EmIdentity(ctx.logger());
        NoXpRecognition recognition = new NoXpRecognition(ctx.logger());
        // The bridge's listeners consult the engine as their
        // hooks, and the engine grants through the bridge as
        // its XP port: build the engine, then the bridge, then
        // wire the port — all before registering either.
        engine = new KseaEngine(ctx.plugin(), identity, null, recognition,
                settings);
        bridge = new McMMOBridge(ctx.logger(), engine);
        engine.setXpPort(bridge);
        Bukkit.getPluginManager().registerEvents(engine, ctx.plugin());
        Bukkit.getPluginManager().registerEvents(bridge, ctx.plugin());
        engine.sweepOnline();
        ctx.logger().info("mcMMO XP Connector ready — kill awards "
                + "BOSS " + settings.rates().boss() + " / MINIBOSS "
                + settings.rates().miniboss() + " / NATURAL "
                + settings.rates().natural() + " / TRASH "
                + settings.rates().trash() + " XP per level, ceiling "
                + settings.ceiling() + " per skill per hour, per-damage "
                + "veto " + (settings.vetoEnabled() ? "on" : "off") + ".");
    }

    @Override
    public void onDisable() {
        if (engine != null) {
            HandlerList.unregisterAll(engine);
            engine.saveState();
            engine = null;
        }
        if (bridge != null) {
            HandlerList.unregisterAll(bridge);
            bridge = null;
        }
    }

    @Override
    public void reload() {
        if (engine != null && ctx != null) {
            engine.reload(ctx.moduleConfig());
        }
    }

    /** Test/rig hook: the engine, or null when stood down. */
    public KseaEngine engine() {
        return engine;
    }
}
