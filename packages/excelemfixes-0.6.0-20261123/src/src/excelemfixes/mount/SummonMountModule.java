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
 * <p>Not a defect workaround — a product riding the pack's module
 * framework, with the pack's posture: individually toggleable,
 * config-driven, and removable without touching anything else.
 * Retirement is the module toggle (modules.summon-mount.enabled)
 * plus the in-config {@code retired} flag for the soft landing
 * (horses called in, sales stopped, summons answered with the
 * retirement message) — see the design note.
 *
 * <p>ExcelItems is a softdepend of the pack and a hard
 * requirement of THIS module (the charm lives in its registry):
 * when ExcelItems is absent the module stands down with one clear
 * log line and registers nothing — no listeners, no command, no
 * registry claim — leaving every other module untouched. Vault is
 * required only for purchases; without an economy the charm still
 * summons and admins can still give it.
 */
public final class SummonMountModule implements FixModule {

    private ModuleContext ctx;
    private boolean active;
    private ExcelItemsAPI items;
    private Economy economy;
    private MountSettings settings;
    private MountService service;
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

        service = new MountService(ctx.plugin(), settings,
                MountRegistrar.charmCheck(items));
        Bukkit.getPluginManager().registerEvents(service, ctx.plugin());
        service.start();
        if (economy != null) {
            buyFlow = new BuyFlow(items, economy, settings,
                    ctx.logger());
        }
        ctx.registerCommand("mountcharm", java.util.List.of("mount"),
                "/mountcharm buy|confirm|summon|dismiss|status",
                new MountCommand(this));

        // Orphan sweep once worlds/chunks have settled.
        Bukkit.getScheduler().runTaskLater(ctx.plugin(),
                () -> {
                    if (service != null) service.sweepOrphans();
                }, 100L);

        active = true;
        ctx.logger().info("Summon Mount enabled: Mount Charm"
                + " (registry id " + MountItem.ID + ") at $"
                + settings.price() + " shop / $" + settings.npcPrice()
                + " vendor NPC; retired=" + settings.retired() + ".");
    }

    @Override
    public void onDisable() {
        if (service != null) {
            service.stop();
            HandlerList.unregisterAll(service);
            service = null;
        }
        buyFlow = null;
        active = false;
    }

    @Override
    public void reload() {
        if (!active) return;
        settings = MountSettings.from(ctx.moduleConfig());
        if (service != null) {
            service.updateSettings(settings);
            if (settings.retired()) service.despawnAll();
        }
        if (buyFlow != null) buyFlow.updateSettings(settings);
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

    /** Mint a charm (admin give). Only valid while active. */
    public ItemStack createCharm() {
        return MountRegistrar.createCharm(items);
    }
}
