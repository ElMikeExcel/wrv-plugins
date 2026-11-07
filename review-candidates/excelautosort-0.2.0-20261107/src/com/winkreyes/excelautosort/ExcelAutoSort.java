package com.winkreyes.excelautosort;

import com.winkreyes.excelautosort.bedrock.BedrockFormsHook;
import com.winkreyes.excelautosort.cmd.AutoSortCommand;
import com.winkreyes.excelautosort.core.ActivesParse;
import com.winkreyes.excelautosort.engine.BufferService;
import com.winkreyes.excelautosort.engine.ChestListener;
import com.winkreyes.excelautosort.engine.FilterEngine;
import com.winkreyes.excelautosort.engine.MaxOrgService;
import com.winkreyes.excelautosort.engine.SelectorFlow;
import com.winkreyes.excelautosort.engine.SortEngine;
import com.winkreyes.excelautosort.gui.MaxOrgGui;
import com.winkreyes.excelautosort.gui.SelectorGui;
import com.winkreyes.excelautosort.hook.AutoSortExpansion;
import com.winkreyes.excelautosort.hook.EconomyPort;
import com.winkreyes.excelautosort.hook.GpRights;
import com.winkreyes.excelautosort.hook.ProtectSink;
import com.winkreyes.excelautosort.store.DataStore;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * ExcelAutoSort — our own auto-sort + chest filters for Wink
 * Reyes Valley (SPEC: ~/workspace/autosort-plugin/SPEC.md). Replaces
 * BOTH the third-party AutoSort 0.1.17 engine (by nothub) and our
 * ExcelAutoSortBridge status adapter in one swap: the sort parity
 * contract is reimplemented in-house (§4), the placeholder is served
 * from our own state (§8), and the filter-delete system (§5–§7) is
 * new: per-chest whitelist/blacklist with an itemized confirm, a
 * 7-day recovery buffer, and every removal written to ExcelProtect.
 */
public final class ExcelAutoSort extends JavaPlugin {

    private DataStore store;
    private Messages messages;
    private SortEngine sorter;
    private FilterEngine filter;
    private BufferService buffers;
    private SelectorFlow flow;
    private SelectorGui gui;
    private MaxOrgService maxOrg;
    private MaxOrgGui maxOrgGui;
    private GpRights rights = GpRights.UNAVAILABLE;
    private ProtectSink sink = ProtectSink.NOOP;
    private EconomyPort economy = EconomyPort.UNAVAILABLE;
    private BedrockFormsHook bedrock;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.messages = new Messages(getConfig());
        this.store = new DataStore(getDataFolder(), getLogger());
        store.loadAll();
        importIncumbentActives();

        this.sorter = new SortEngine(this);
        this.buffers = new BufferService(this);
        this.filter = new FilterEngine(this);
        this.flow = new SelectorFlow(this);
        this.gui = new SelectorGui(this);
        this.maxOrg = new MaxOrgService(this);
        this.maxOrgGui = new MaxOrgGui(this);

        bindRights();
        bindProtect();
        bindEconomy();
        bindBedrock();

        AutoSortCommand cmd = new AutoSortCommand(this);
        PluginCommand pc = getCommand("autosort");
        if (pc != null) {
            pc.setExecutor(cmd);
            pc.setTabCompleter(cmd);
        }

        // Filter listener registers BEFORE the GUI's own handlers are
        // irrelevant — but the §4 order (filter, then sort, on close) is
        // inside ChestListener by construction.
        getServer().getPluginManager().registerEvents(new ChestListener(this), this);
        getServer().getPluginManager().registerEvents(gui, this);
        getServer().getPluginManager().registerEvents(maxOrgGui, this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new AutoSortExpansion(this).register();
        }

        // The only scheduled work in the plugin (§3): an hourly sweep
        // over the in-memory buffer index. It touches no blocks.
        Bukkit.getScheduler().runTaskTimer(this, () -> buffers.sweepAll(),
                72_000L, 72_000L);

        int waiting = 0;
        for (List<com.winkreyes.excelautosort.store.BufferEntry> l : store.buffers.values()) waiting += l.size();
        getLogger().info("ExcelAutoSort " + BuildInfo.VERSION + " awake: sort parity engine on ("
                + sorter.typeListForMessage() + "), filter chests " + store.chests.size()
                + ", recovery entries " + waiting
                + ", protect sink " + (sink.bound() ? "BOUND" : "UNBOUND")
                + ", gp rights " + (rights.available() ? "BOUND" : "UNBOUND")
                + ", max-org " + (maxOrg.enabled() ? "on" : "off")
                + " (economy " + (economy.available() ? "BOUND" : "UNBOUND")
                + ", entitlements " + store.maxOrg.size() + ")"
                + ", build " + BuildInfo.BUILD_ID);
    }

    @Override
    public void onDisable() {
        if (store != null) store.saveAll();
    }

    // ------------------------------------------------------------ binds (isolation pattern)

    private void bindRights() {
        Plugin gp = Bukkit.getPluginManager().getPlugin("GriefPrevention");
        if (gp != null && gp.isEnabled()) {
            try {
                rights = new com.winkreyes.excelautosort.hook.GpRightsHook(this, getLogger());
                rights.setClaimChangeHandler((world, minX, maxX, minZ, maxZ, reason) ->
                        Bukkit.getScheduler().runTask(this,
                                () -> filter.suspendInClaim(world, minX, maxX, minZ, maxZ, reason)));
            } catch (Throwable t) {
                getLogger().warning("ExcelAutoSort: GriefPrevention hook failed to bind (" + t
                        + ") — chest setup is DENIED until this is fixed (fail closed)");
                rights = GpRights.UNAVAILABLE;
            }
        } else {
            getLogger().warning("ExcelAutoSort: GriefPrevention not present — chest setup is DENIED (fail closed, SPEC §6.4)");
        }
    }

    private void bindProtect() {
        Plugin protect = Bukkit.getPluginManager().getPlugin("ExcelProtect");
        if (protect != null && protect.isEnabled()) {
            try {
                sink = new com.winkreyes.excelautosort.protect.ProtectGateway(getLogger());
                getLogger().info("ExcelAutoSort: ExcelProtect audit sink BOUND (stream: items)");
                return;
            } catch (Throwable t) {
                getLogger().severe("ExcelAutoSort: ExcelProtect is present but its audit sink FAILED TO BIND ("
                        + t + ") — deletions will be recorded in the buffer and console only");
                sink = ProtectSink.NOOP;
                return;
            }
        }
        getLogger().warning("ExcelAutoSort: ExcelProtect not present — the audit sink is UNBOUND; "
                + "filter removals are recorded in the recovery buffer and console only");
        sink = ProtectSink.NOOP;
    }

    private void bindEconomy() {
        Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");
        if (vault != null && vault.isEnabled()) {
            try {
                economy = new com.winkreyes.excelautosort.economy.VaultGateway();
                if (!economy.available()) {
                    getLogger().warning("ExcelAutoSort: Vault is present but no economy provider is "
                            + "registered — Max Organization purchases will refuse (nothing charged)");
                }
                return;
            } catch (Throwable t) {
                getLogger().warning("ExcelAutoSort: Vault economy failed to bind (" + t
                        + ") — Max Organization purchases will refuse (nothing charged)");
                economy = EconomyPort.UNAVAILABLE;
                return;
            }
        }
        getLogger().warning("ExcelAutoSort: Vault not present — Max Organization purchases "
                + "will refuse (nothing charged); the network view still works for existing owners");
    }

    private void bindBedrock() {
        Plugin floodgate = Bukkit.getPluginManager().getPlugin("floodgate");
        if (floodgate != null && floodgate.isEnabled()) {
            try {
                bedrock = new BedrockFormsHook(this);
            } catch (Throwable t) {
                getLogger().warning("ExcelAutoSort: Floodgate forms failed to bind (" + t
                        + ") — Bedrock players get the Java-style flow via Geyser inventory");
                bedrock = null;
            }
        }
    }

    // ------------------------------------------------------------ migration (§10)

    /** One-time import of the incumbent's active.txt ON set; nobody's toggle resets. */
    private void importIncumbentActives() {
        File playersFile = new File(getDataFolder(), "data/players.yml");
        if (playersFile.exists()) return;
        Path legacy = Path.of("plugins", "autosort", "active.txt");
        if (!Files.exists(legacy)) return;
        try {
            Set<UUID> imported = ActivesParse.parse(Files.readAllLines(legacy));
            if (!imported.isEmpty()) {
                store.sortOn.addAll(imported);
                store.savePlayers();
            }
            getLogger().info("ExcelAutoSort: imported " + imported.size()
                    + " auto-sort states from AutoSort (plugins/autosort/active.txt)");
        } catch (Exception e) {
            getLogger().warning("ExcelAutoSort: could not import AutoSort's active.txt (" + e.getMessage() + ")");
        }
    }

    // ------------------------------------------------------------ platform routing

    /** Bedrock players get Cumulus forms; everyone else the inventory GUI (§7.5). */
    public void openSelector(Player player, List<String> targetKeys) {
        if (bedrock != null && bedrock.isBedrock(player)) {
            bedrock.openSelector(player, targetKeys);
        } else {
            gui.openSelector(player, targetKeys);
        }
    }

    public void openRecovery(Player player) {
        if (bedrock != null && bedrock.isBedrock(player)) {
            bedrock.openRecovery(player);
        } else {
            gui.openRecovery(player);
        }
    }

    /** Max Organization (§15): entitled players land in the network view; others on the unlock screen. */
    public void openMaxOrg(Player player) {
        if (bedrock != null && bedrock.isBedrock(player)) {
            bedrock.openMaxOrg(player);
        } else if (maxOrg.entitled(player.getUniqueId())) {
            maxOrgGui.openNetwork(player);
        } else {
            maxOrgGui.openUnlock(player);
        }
    }

    public void reloadAll() {
        reloadConfig();
        messages.reload(getConfig());
        store.loadAll();
    }

    // ------------------------------------------------------------ accessors

    public DataStore store() { return store; }
    public Messages messages() { return messages; }
    public SortEngine sorter() { return sorter; }
    public FilterEngine filter() { return filter; }
    public BufferService buffers() { return buffers; }
    public SelectorFlow flow() { return flow; }
    public SelectorGui gui() { return gui; }
    public MaxOrgService maxOrg() { return maxOrg; }
    public MaxOrgGui maxOrgGui() { return maxOrgGui; }
    public GpRights rights() { return rights; }
    public ProtectSink sink() { return sink; }
    public EconomyPort economy() { return economy; }
    public BedrockFormsHook bedrock() { return bedrock; }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
