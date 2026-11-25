package excelemfixes.mount;

import com.winkreyes.excelitems.api.ExcelItemsAPI;
import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Summon Mount module — the interim mount product (D196, Mike,
 * Nov 22, 2026; shipped as an ExcelEMFixes module per D199): a
 * buyable Mount Charm (ExcelItems registry ID 5) that calls and
 * dismisses a player's own horse, built for travel in EliteMobs
 * worlds until EliteMobs ships native mounts (watch
 * P-EM-MOUNT-WATCH-20261122).
 *
 * <p>Since 0.8.0 the module carries a second, premium tier
 * (D204): the Ghast Charm (registry ID 9), a carry-and-toggle
 * charm for a flying happy-ghast mount, sold under a quest
 * gate — the buyer must be halfway through the quests of
 * Primis or of any single expansion. The ghast is its own
 * service, its own settings block and its own command
 * (/ghastcharm), sharing the module's policy (combat gate,
 * world gate, retirement) and — through one shared
 * {@link CombatMarks} — one combat state per player, so the
 * second charm opens no hole in the first charm's gates. The
 * ghast tier has its own config switch (ghast.enabled) and
 * stands down alone if registry ID 9 is ever claimed by
 * someone else; the horse is unaffected either way.
 *
 * <p>Not a defect workaround — a product riding the pack's module
 * framework, with the pack's posture: individually toggleable,
 * config-driven, and removable without touching anything else.
 * Retirement is the module toggle (modules.summon-mount.enabled)
 * plus the in-config {@code retired} flag for the soft landing
 * (mounts called in, sales stopped, summons answered with the
 * retirement message) — see the design note.
 *
 * <p>ExcelItems is a softdepend of the pack and a hard
 * requirement of THIS module (the charms live in its registry):
 * when ExcelItems is absent the module stands down with one clear
 * log line and registers nothing — no listeners, no command, no
 * registry claim — leaving every other module untouched. Vault is
 * required only for purchases; without an economy the charms still
 * summon and admins can still give them. The quest gate reads
 * EliteMobs data through {@link EmQuestGate}; without EliteMobs
 * the gate cannot be read and Ghast Charm purchases refuse
 * (fail closed) while everything else works.
 */
public final class SummonMountModule implements FixModule {

    private ModuleContext ctx;
    private boolean active;
    private ExcelItemsAPI items;
    private Economy economy;
    private MountSettings settings;
    private GhastSettings ghastSettings;
    private CombatMarks combatMarks;
    private MountService service;
    private GhastMountService ghastService;
    private EmQuestGate gate;
    private BuyFlow buyFlow;

    @Override
    public String id() {
        return "summon-mount";
    }

    @Override
    public String displayName() {
        return "Summon Mount";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        this.settings = MountSettings.from(ctx.moduleConfig());
        this.ghastSettings = GhastSettings.from(ctx.moduleConfig() == null
                ? null : ctx.moduleConfig().getConfigurationSection("ghast"));

        if (Bukkit.getPluginManager().getPlugin("ExcelItems") == null) {
            ctx.logger().warning("summon-mount: ExcelItems is not"
                    + " installed — the Mount Charm lives in the"
                    + " ExcelItems registry, so this module is"
                    + " standing down. Install ExcelItems to enable"
                    + " it; every other ExcelEMFixes module is"
                    + " unaffected.");
            active = false;
            return;
        }
        try {
            items = MountRegistrar.api();
        } catch (Throwable t) {
            ctx.logger().severe("summon-mount: ExcelItems API"
                    + " unavailable (" + t + ") — module standing"
                    + " down.");
            active = false;
            return;
        }
        if (MountRegistrar.ensureRegistered(items, ctx.logger())
                == MountRegistrar.Outcome.CLASH) {
            active = false;
            return;
        }

        economy = hookEconomy();
        if (economy == null) {
            ctx.logger().warning("summon-mount: no Vault economy"
                    + " found — charm purchases are disabled (give"
                    + " and summon still work).");
        }

        combatMarks = new CombatMarks();
        service = new MountService(ctx.plugin(), settings,
                MountRegistrar.charmCheck(items), combatMarks);
        Bukkit.getPluginManager().registerEvents(service, ctx.plugin());
        service.start();

        enableGhastTier(ctx);

        if (economy != null) {
            buyFlow = new BuyFlow(items, economy, settings, ghastSettings,
                    gate, ctx.logger());
        }
        ctx.registerCommand("mountcharm", java.util.List.of("mount"),
                "/mountcharm buy|confirm|summon|dismiss|status",
                new MountCommand(this));
        if (ghastService != null) {
            ctx.registerCommand("ghastcharm", java.util.List.of(),
                    "/ghastcharm buy|confirm|summon|dismiss|status",
                    new GhastCommand(this));
        }

        // Orphan sweep once worlds/chunks have settled.
        Bukkit.getScheduler().runTaskLater(ctx.plugin(),
                () -> {
                    if (service != null) service.sweepOrphans();
                    if (ghastService != null) ghastService.sweepOrphans();
                }, 100L);

        active = true;
        ctx.logger().info("Summon Mount enabled: Mount Charm"
                + " (registry id " + MountItem.ID + ") at $"
                + settings.price() + " shop / $" + settings.npcPrice()
                + " vendor NPC; retired=" + settings.retired() + ".");
        if (ghastService != null) {
            ctx.logger().info("Summon Mount ghast tier enabled: Ghast"
                    + " Charm (registry id " + GhastItem.ID + ") at $"
                    + ghastSettings.price() + " shop / $"
                    + ghastSettings.npcPrice() + " vendor NPC; quest"
                    + " gate " + (ghastSettings.gateEnabled()
                            ? "on (fraction "
                                    + ghastSettings.gateFraction() + ")"
                            : "off") + ".");
        }
    }

    /** The ghast tier (D204): registration, service and gate,
     *  each able to stand down without touching the horse. */
    private void enableGhastTier(ModuleContext ctx) {
        if (!ghastSettings.enabled()) {
            ctx.logger().info("summon-mount: ghast tier disabled in"
                    + " config (ghast.enabled: false) — Mount Charm"
                    + " only.");
            return;
        }
        if (MountRegistrar.ensureGhastRegistered(items, ctx.logger())
                == MountRegistrar.Outcome.CLASH) {
            return;
        }
        ghastService = new GhastMountService(ctx.plugin(), settings,
                ghastSettings, MountRegistrar.ghastCharmCheck(items),
                combatMarks);
        Bukkit.getPluginManager().registerEvents(ghastService,
                ctx.plugin());
        ghastService.start();

        if (Bukkit.getPluginManager().getPlugin("EliteMobs") != null) {
            gate = new EmQuestGate(ctx.plugin(), ctx.logger(),
                    ghastSettings.gatePackages());
            gate.updateFraction(ghastSettings.gateFraction());
        } else if (ghastSettings.gateEnabled()) {
            ctx.logger().warning("summon-mount: the ghast quest gate"
                    + " is enabled but EliteMobs is not installed —"
                    + " the gate cannot be read, so Ghast Charm"
                    + " purchases will refuse (fail closed). Set"
                    + " ghast.gate.enabled: false to sell ungated.");
        }
        if (ghastSettings.gateEnabled()
                && ghastSettings.gatePackages().isEmpty()) {
            ctx.logger().warning("summon-mount: the ghast quest gate"
                    + " is enabled but no quest packages are"
                    + " configured (ghast.gate.packages) — the"
                    + " Ghast Charm cannot be bought until at least"
                    + " one package is defined.");
        }
    }

    @Override
    public void onDisable() {
        if (service != null) {
            service.stop();
            HandlerList.unregisterAll(service);
            service = null;
        }
        if (ghastService != null) {
            ghastService.stop();
            HandlerList.unregisterAll(ghastService);
            ghastService = null;
        }
        buyFlow = null;
        gate = null;
        active = false;
    }

    @Override
    public void reload() {
        if (!active) return;
        settings = MountSettings.from(ctx.moduleConfig());
        ghastSettings = GhastSettings.from(ctx.moduleConfig() == null
                ? null : ctx.moduleConfig().getConfigurationSection("ghast"));
        if (service != null) {
            service.updateSettings(settings);
            if (settings.retired()) service.despawnAll();
        }
        if (ghastService != null) {
            ghastService.updateSettings(settings, ghastSettings);
            if (settings.retired()) ghastService.despawnAll();
        }
        if (gate != null) {
            gate.updateRules(ghastSettings.gatePackages());
            gate.updateFraction(ghastSettings.gateFraction());
        }
        if (buyFlow != null) buyFlow.updateSettings(settings, ghastSettings);
    }

    /** Re-read settings on /mountcharm reload (admin). */
    void reloadSettings() {
        reload();
    }

    private Economy hookEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return null;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit
                .getServicesManager().getRegistration(Economy.class);
        return rsp == null ? null : rsp.getProvider();
    }

    // ------------------------------------------------------------ access

    /** True when the module fully enabled (ExcelItems present, ID
     *  5 claimed). False = standing down (see the enable log). */
    public boolean isActive() {
        return active;
    }

    public MountService service() {
        return service;
    }

    /** Null when the ghast tier is off (config), stood down
     *  (registry clash), or the module is down. */
    public GhastMountService ghastService() {
        return ghastService;
    }

    /** Null when no Vault economy is available (purchases off). */
    public BuyFlow buyFlow() {
        return buyFlow;
    }

    public Economy economy() {
        return economy;
    }

    public MountSettings settings() {
        return settings;
    }

    public GhastSettings ghastSettings() {
        return ghastSettings;
    }

    /** Mint a charm (admin give). Only valid while active. */
    public ItemStack createCharm() {
        return MountRegistrar.createCharm(items);
    }

    /** Mint a Ghast Charm (admin give). Only valid while the
     *  ghast tier is up. */
    public ItemStack createGhastCharm() {
        return MountRegistrar.createGhastCharm(items);
    }
}
