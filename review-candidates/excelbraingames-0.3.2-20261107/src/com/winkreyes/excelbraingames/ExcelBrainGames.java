package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.bedrock.BedrockHook;
import com.winkreyes.excelbraingames.core.BuildInfo;
import com.winkreyes.excelbraingames.core.Money;
import com.winkreyes.excelbraingames.core.WsGeometry;
import com.winkreyes.excelbraingames.gui.GuiListener;
import com.winkreyes.excelbraingames.gui.Views;
import org.bukkit.entity.Player;
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
 * Phase 3 (0.3.0) adds the text trio on ONE shared question-loop module
 * (games/text/TextLoop.java): Quick Math, Word Scramble, Riddle of the
 * Day — completing the six-game set from SPEC §1.
 *
 * Kid tiers ship ENABLED: Mike hand-checked the kid word lists and
 * approved them Nov 4, 2026, clearing the SPEC §9.4 pre-release gate;
 * the checked lists live in wordlists/ with the adult lists.
 *
 * 0.3.2 adds Word Search board geometry (core/WsGeometry): Bedrock
 * starters are dealt a 5-wide stream board shaped for the phone's
 * Geyser wrap; the geometry is recorded on the round, and platform
 * detection is a Floodgate softdep — absent or unknown means the Java
 * geometry, exactly as 0.3.1 dealt everyone.
 */
public final class ExcelBrainGames extends JavaPlugin {

    /** Bundled word lists extracted to the data folder on first enable. */
    private static final String[] BUNDLED_WORDLISTS = {
            "wordlists/animals.yml", "wordlists/food.yml", "wordlists/minecraft.yml",
            "wordlists/space.yml", "wordlists/ocean.yml", "wordlists/school.yml",
            "wordlists/kid-animals.yml", "wordlists/kid-food.yml",
            "wordlists/kid-minecraft.yml", "wordlists/riddles.yml",
    };

    private GameConfig gameConfig;
    private WordLists wordLists;
    private DataStore dataStore;
    private GameService service;
    private Views views;
    private Economy economy;
    private com.winkreyes.excelbraingames.games.text.RiddleService riddleService;
    private com.winkreyes.excelbraingames.games.text.TextLoop textLoop;
    private TextInputListener textInput;
    private BedrockHook bedrock;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        extractWordlists();
        reloadAll();
        bindBedrock();
        this.dataStore = new DataStore(getDataFolder(), getLogger());
        this.service = new GameService(this);
        this.views = new Views(this);
        this.riddleService = new com.winkreyes.excelbraingames.games.text.RiddleService(getDataFolder(), getLogger());
        this.textLoop = new com.winkreyes.excelbraingames.games.text.TextLoop(
                uuid -> dataStore.get(uuid, null, gameConfig.zone),
                () -> wordLists.riddles(),
                this::todaysRiddleIndex);
        hookEconomy();

        BrainGamesCommand cmd = new BrainGamesCommand(this);
        if (getCommand("games") != null) getCommand("games").setExecutor(cmd);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        this.textInput = new TextInputListener(this);
        getServer().getPluginManager().registerEvents(textInput, this);

        getLogger().info("ExcelBrainGames " + getDescription().getVersion() + " enabled: "
                + "games=[word-search, memory-match, sudoku, quick-math, word-scramble, riddle], kid-tiers=" + (gameConfig.kidTiersEnabled ? "ENABLED" : "DISABLED (pre-release gate)")
                + ", daily-cap=" + Money.fmt(gameConfig.dailyCapCents)
                + ", wordlists=" + wordLists.fileCount() + " files / " + wordLists.themeCount() + " themes"
                + ", riddles=" + wordLists.riddles().size()
                + ", economy=" + (economy == null ? "NONE (ledger-only payouts!)" : economy.getName())
                + ", platform-detection=" + (bedrock != null ? "floodgate" : "unavailable (Java geometry for all)")
                + ", build=" + BuildInfo.BUILD_ID);
    }

    /**
     * Bind the Floodgate hook if (and only if) floodgate is present and
     * enabled — the fleet pattern: BedrockHook is the only class that
     * references Floodgate, and it is only loaded after this check, so
     * the plugin runs unchanged on a server without floodgate.
     */
    private void bindBedrock() {
        org.bukkit.plugin.Plugin fg = getServer().getPluginManager().getPlugin("floodgate");
        if (fg == null || !fg.isEnabled()) {
            getLogger().info("ExcelBrainGames: floodgate not present — Word Search deals the Java geometry to everyone.");
            return;
        }
        try {
            this.bedrock = new BedrockHook();
            getLogger().info("ExcelBrainGames: floodgate detected — Bedrock players get the 5-wide Word Search board.");
        } catch (Throwable t) {
            this.bedrock = null;
            getLogger().warning("ExcelBrainGames: floodgate hook failed to bind (" + t.getMessage()
                    + ") — Word Search deals the Java geometry to everyone.");
        }
    }

    /**
     * The Word Search geometry a NEW round for this player is dealt in:
     * BEDROCK when Floodgate knows them, JAVA otherwise — including when
     * the platform cannot be determined at all (safe default).
     */
    public WsGeometry platformOf(Player p) {
        return bedrock != null && bedrock.isBedrock(p) ? WsGeometry.BEDROCK : WsGeometry.JAVA;
    }

    /** Today's riddle index (rolls the day over on first touch); -1 when the list is empty. */
    public int todaysRiddleIndex() {
        java.util.List<com.winkreyes.excelbraingames.games.text.RiddleText.Riddle> list = wordLists.riddles();
        return riddleService.todayIndex(list.size(), gameConfig.zone, gameConfig.riddlePreferKidWeekends,
                i -> list.get(i).kid());
    }

    public com.winkreyes.excelbraingames.games.text.RiddleService riddleService() { return riddleService; }
    public com.winkreyes.excelbraingames.games.text.TextLoop textLoop() { return textLoop; }
    public TextInputListener textInput() { return textInput; }

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
