package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.core.Money;
import com.winkreyes.excelbraingames.gui.GuiListener;
import com.winkreyes.excelbraingames.gui.Views;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * ExcelBrainGames — Phase 1 (SPEC §11): the framework (/games menu + compass
 * door, sessions with save/resume, the shared $1,500/day cap + streak
 * ledger, Vault payouts, console admin) plus Word Search and Memory Match.
 * Phase 2 (0.2.0) adds Sudoku: uniqueness-proving generator, palette-first
 * placement, picture mode on the kid 4x4, 9x9 two-view paging.
 * Quick Math / Word Scramble / Riddle are Phase 3 (the shared question-loop
 * seam ships as a stub, games/QuestionLoop.java).
 *
 * Kid tiers ship ENABLED: Mike hand-checked the kid word lists and
 * approved them Nov 4, 2026, clearing the SPEC §9.4 pre-release gate;
 * the checked lists live in wordlists/ with the adult lists.
 */
public final class ExcelBrainGames extends JavaPlugin {

    /** Bundled word lists extracted to the data folder on first enable. */
    private static final String[] BUNDLED_WORDLISTS = {
            "wordlists/animals.yml", "wordlists/food.yml", "wordlists/minecraft.yml",
            "wordlists/space.yml", "wordlists/ocean.yml", "wordlists/school.yml",
            "wordlists/kid-animals.yml", "wordlists/kid-food.yml",
            "wordlists/kid-minecraft.yml",
    };

    private GameConfig gameConfig;
    private WordLists wordLists;
    private DataStore dataStore;
    private GameService service;
    private Views views;
    private Economy economy;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        extractWordlists();
        reloadAll();
        this.dataStore = new DataStore(getDataFolder(), getLogger());
        this.service = new GameService(this);
        this.views = new Views(this);
        hookEconomy();

        BrainGamesCommand cmd = new BrainGamesCommand(this);
        if (getCommand("games") != null) getCommand("games").setExecutor(cmd);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);

        getLogger().info("ExcelBrainGames " + getDescription().getVersion() + " enabled: "
                + "games=[word-search, memory-match, sudoku], kid-tiers=" + (gameConfig.kidTiersEnabled ? "ENABLED" : "DISABLED (pre-release gate)")
                + ", daily-cap=" + Money.fmt(gameConfig.dailyCapCents)
                + ", wordlists=" + wordLists.fileCount() + " files / " + wordLists.themeCount() + " themes"
                + ", economy=" + (economy == null ? "NONE (ledger-only payouts!)" : economy.getName()));
    }

    @Override
    public void onDisable() {
        if (dataStore != null) dataStore.saveAll();
    }

    /** Reload config + tiers + word lists from disk (admin reload). */
    public void reloadAll() {
        reloadConfig();
        this.gameConfig = new GameConfig(getConfig());
        if (this.wordLists == null) this.wordLists = new WordLists();
        this.wordLists.load(new File(getDataFolder(), "wordlists"), getLogger());
    }

    private void extractWordlists() {
        for (String path : BUNDLED_WORDLISTS) {
            File out = new File(getDataFolder(), path);
            if (!out.exists()) {
                saveResource(path, false);
            }
        }
    }

    private void hookEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        this.economy = rsp == null ? null : rsp.getProvider();
        if (this.economy == null) {
            getLogger().warning("ExcelBrainGames: no Vault economy provider found — payouts will be ledger-only until one is present.");
        }
    }

    public GameConfig gameConfig() { return gameConfig; }
    public WordLists wordLists() { return wordLists; }
    public DataStore dataStore() { return dataStore; }
    public GameService service() { return service; }
    public Views views() { return views; }
    public Economy economy() { return economy; }
}
