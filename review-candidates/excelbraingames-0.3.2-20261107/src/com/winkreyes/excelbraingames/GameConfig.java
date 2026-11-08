package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.core.Dir;
import com.winkreyes.excelbraingames.core.Money;
import com.winkreyes.excelbraingames.core.WsGeometry;
import com.winkreyes.excelbraingames.games.sudoku.SudokuRound;
import com.winkreyes.excelbraingames.games.wordsearch.WordSearchRound;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Everything tunable lives in config.yml (fleet config-first pattern):
 * payouts, tier shapes, the daily cap, symbols, themes, kid flags, messages.
 * This class is the parsed view; reload rebuilds it wholesale.
 */
public final class GameConfig {

    public record WsTier(String id, String label, boolean kid, WordSearchRound.Spec spec,
                         long payoutCents, int parSeconds, int speedBonusPercent) {}
    public record MemTier(String id, String label, boolean kid, int pairs,
                          long payoutCents, int parFlips, int parBonusPercent) {}
    public record SudTier(String id, String label, boolean kid, int size, int boxRows, int boxCols,
                          int givensMin, int givensMax, long payoutCents,
                          int parSeconds, int speedBonusPercent, boolean pictureCapable) {}
    public record QmTier(String id, String label, boolean kid, long payoutCents, boolean choices) {}
    public record ScrTier(String id, String label, boolean kid, long payoutCents, long perWordCents) {}
    public record Theme(String id, String label, Material icon) {}
    public record KidEntry(UUID uuid, String name) {}

    public final long dailyCapCents;
    public final ZoneId zone;
    public final long streakBonusPerDayCents;
    public final long streakBonusMaxCents;
    public final boolean kidTiersEnabled;
    public final Map<String, WsTier> wsTiers;
    /** Bedrock board shapes per Word Search tier (0.3.2): same tier, 5-wide stream shape. */
    public final Map<String, WsTier> wsBedrockTiers;
    public final Map<String, MemTier> memTiers;
    public final Map<String, SudTier> sudokuTiers;
    public final int sudokuCheckCostPercent;
    public final int sudokuMaxChecks;
    public final List<Material> sudokuPictureSymbols;
    public final Map<String, QmTier> qmTiers;
    public final Map<String, ScrTier> scrambleTiers;
    public final boolean riddlePreferKidWeekends;
    public final List<Material> memorySymbols;
    public final Map<String, Theme> themes;
    public final List<KidEntry> kids;
    private final FileConfiguration cfg;

    public GameConfig(FileConfiguration cfg) {
        this.cfg = cfg;
        this.dailyCapCents = Money.dollarsToCents(cfg.getDouble("economy.daily-cap", 1500.00));
        this.zone = ZoneId.of(cfg.getString("economy.zone", "America/Chicago"));
        this.streakBonusPerDayCents = Money.dollarsToCents(cfg.getDouble("economy.streak-bonus-per-day", 10.00));
        this.streakBonusMaxCents = Money.dollarsToCents(cfg.getDouble("economy.streak-bonus-max", 50.00));
        this.kidTiersEnabled = cfg.getBoolean("games.kid-tiers-enabled", false);

        Map<String, WsTier> ws = new LinkedHashMap<>();
        ConfigurationSection wsSec = cfg.getConfigurationSection("games.wordsearch.tiers");
        if (wsSec != null) {
            for (String id : wsSec.getKeys(false)) {
                ConfigurationSection t = wsSec.getConfigurationSection(id);
                Set<Dir> dirs = EnumSet.noneOf(Dir.class);
                for (String d : t.getStringList("directions")) {
                    Dir dir = Dir.parse(d);
                    if (dir != null) dirs.add(dir);
                }
                WordSearchRound.Spec spec = new WordSearchRound.Spec(
                        t.getInt("cols", 9), t.getInt("rows", 6), t.getInt("words", 8),
                        t.getInt("min-length", 3), t.getInt("max-length", 8), dirs);
                ws.put(id, new WsTier(id, t.getString("label", id), t.getBoolean("kid", false), spec,
                        Money.dollarsToCents(t.getDouble("payout", 0)),
                        t.getInt("par-seconds", 0), t.getInt("speed-bonus-percent", 0)));
            }
        }
        this.wsTiers = ws;

        // 0.3.2 Bedrock shapes (config games.wordsearch.bedrock-tiers): a
        // shape entry overrides ONLY label + cols + rows of its tier — word
        // count, length band, directions, payout, par and kid flag are the
        // tier's own (Mike's ruling, Nov 7, 2026: same lists, same tiers,
        // both platforms). A shape for a tier that does not exist is inert.
        Map<String, WsTier> wsBedrock = new LinkedHashMap<>();
        ConfigurationSection btSec = cfg.getConfigurationSection("games.wordsearch.bedrock-tiers");
        if (btSec != null) {
            for (String id : btSec.getKeys(false)) {
                ConfigurationSection t = btSec.getConfigurationSection(id);
                WsTier base = ws.get(id);
                if (t == null || base == null) continue;
                WordSearchRound.Spec bSpec = new WordSearchRound.Spec(
                        t.getInt("cols", base.spec().cols()), t.getInt("rows", base.spec().rows()),
                        base.spec().wordCount(), base.spec().minLen(), base.spec().maxLen(),
                        base.spec().directions());
                wsBedrock.put(id, new WsTier(id, t.getString("label", base.label()), base.kid(), bSpec,
                        base.payoutCents(), base.parSeconds(), base.speedBonusPercent()));
            }
        }
        this.wsBedrockTiers = wsBedrock;

        Map<String, MemTier> mem = new LinkedHashMap<>();
        ConfigurationSection memSec = cfg.getConfigurationSection("games.memory.tiers");
        if (memSec != null) {
            for (String id : memSec.getKeys(false)) {
                ConfigurationSection t = memSec.getConfigurationSection(id);
                mem.put(id, new MemTier(id, t.getString("label", id), t.getBoolean("kid", false),
                        t.getInt("pairs", 6), Money.dollarsToCents(t.getDouble("payout", 0)),
                        t.getInt("par-flips", 0), t.getInt("par-bonus-percent", 20)));
            }
        }
        this.memTiers = mem;

        Map<String, SudTier> sud = new LinkedHashMap<>();
        ConfigurationSection sudSec = cfg.getConfigurationSection("games.sudoku.tiers");
        if (sudSec != null) {
            for (String id : sudSec.getKeys(false)) {
                ConfigurationSection t = sudSec.getConfigurationSection(id);
                sud.put(id, new SudTier(id, t.getString("label", id), t.getBoolean("kid", false),
                        t.getInt("size", 9), t.getInt("box-rows", 3), t.getInt("box-cols", 3),
                        t.getInt("givens-min", 24), t.getInt("givens-max", 30),
                        Money.dollarsToCents(t.getDouble("payout", 0)),
                        t.getInt("par-seconds", 0), t.getInt("speed-bonus-percent", 0),
                        t.getBoolean("picture", false)));
            }
        }
        this.sudokuTiers = sud;
        this.sudokuCheckCostPercent = cfg.getInt("games.sudoku.check-cost-percent", 10);
        this.sudokuMaxChecks = cfg.getInt("games.sudoku.max-checks", 3);
        List<Material> pic = new ArrayList<>();
        for (String s : cfg.getStringList("games.sudoku.picture-symbols")) {
            Material m = Material.matchMaterial(s);
            if (m != null && m.isItem()) pic.add(m);
        }
        this.sudokuPictureSymbols = pic;

        Map<String, QmTier> qm = new LinkedHashMap<>();
        ConfigurationSection qmSec = cfg.getConfigurationSection("games.quickmath.tiers");
        if (qmSec != null) {
            for (String id : qmSec.getKeys(false)) {
                ConfigurationSection t = qmSec.getConfigurationSection(id);
                qm.put(id, new QmTier(id, t.getString("label", id), t.getBoolean("kid", false),
                        Money.dollarsToCents(t.getDouble("payout", 0)),
                        t.getBoolean("choices", false)));
            }
        }
        this.qmTiers = qm;

        Map<String, ScrTier> scr = new LinkedHashMap<>();
        ConfigurationSection scrSec = cfg.getConfigurationSection("games.scramble.tiers");
        if (scrSec != null) {
            for (String id : scrSec.getKeys(false)) {
                ConfigurationSection t = scrSec.getConfigurationSection(id);
                long payout = Money.dollarsToCents(t.getDouble("payout", 0));
                scr.put(id, new ScrTier(id, t.getString("label", id), t.getBoolean("kid", false),
                        payout, payout / 5));
            }
        }
        this.scrambleTiers = scr;
        this.riddlePreferKidWeekends = cfg.getBoolean("games.riddle.prefer-kid-weekends", true);

        List<Material> symbols = new ArrayList<>();
        for (String s : cfg.getStringList("games.memory.symbols")) {
            Material m = Material.matchMaterial(s);
            if (m != null && m.isItem()) symbols.add(m);
        }
        this.memorySymbols = symbols;

        Map<String, Theme> th = new LinkedHashMap<>();
        ConfigurationSection thSec = cfg.getConfigurationSection("themes");
        if (thSec != null) {
            for (String id : thSec.getKeys(false)) {
                ConfigurationSection t = thSec.getConfigurationSection(id);
                Material icon = Material.matchMaterial(t.getString("icon", "PAPER"));
                th.put(id, new Theme(id, t.getString("label", id), icon == null ? Material.PAPER : icon));
            }
        }
        this.themes = th;

        List<KidEntry> kidList = new ArrayList<>();
        List<Map<?, ?>> rawKids = cfg.getMapList("kids");
        for (Map<?, ?> e : rawKids) {
            Object u = e.get("uuid");
            if (u == null) continue;
            try {
                kidList.add(new KidEntry(UUID.fromString(String.valueOf(u)),
                        e.get("name") == null ? "" : String.valueOf(e.get("name"))));
            } catch (IllegalArgumentException ignored) { /* malformed entry skipped */ }
        }
        this.kids = kidList;
    }

    /**
     * The tier a new Word Search round of {@code tierId} is dealt from in
     * geometry {@code geo} (0.3.2): the Bedrock shape when the round is a
     * Bedrock round and a shape is configured, else the tier itself.
     * Returns null only when the tier id itself is unknown.
     */
    public WsTier wsTierFor(String tierId, WsGeometry geo) {
        if (geo == WsGeometry.BEDROCK) {
            WsTier t = wsBedrockTiers.get(tierId);
            if (t != null) return t;
        }
        return wsTiers.get(tierId);
    }

    public boolean isKid(UUID uuid) {
        for (KidEntry k : kids) if (k.uuid().equals(uuid)) return true;
        return false;
    }

    public String msg(String key, String fallback) {
        return cfg.getString("messages." + key, fallback);
    }
}
