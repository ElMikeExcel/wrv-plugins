package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.core.Ledger;
import com.winkreyes.excelbraingames.core.Money;
import com.winkreyes.excelbraingames.core.Payouts;
import com.winkreyes.excelbraingames.games.memory.MemoryRound;
import com.winkreyes.excelbraingames.games.sudoku.SudokuRound;
import com.winkreyes.excelbraingames.games.text.QuickMathRound;
import com.winkreyes.excelbraingames.games.text.RiddleText;
import com.winkreyes.excelbraingames.games.text.ScrambleRound;
import com.winkreyes.excelbraingames.games.wordsearch.WordSearchRound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/**
 * Round lifecycle: start, complete-and-pay, discard. Owns the money path —
 * tier payout -> speed/par adjustment -> kid-tier rule -> ledger settlement
 * (cap + streak) -> Vault deposit. Pays ONCE per generated round: the
 * round's paid flag is set before the deposit and the round is cleared from
 * the player's active slot in the same step, so a completion can never be
 * settled twice.
 */
public final class GameService {

    private final ExcelBrainGames plugin;
    private final Random rng = new Random();
    static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    public GameService(ExcelBrainGames plugin) {
        this.plugin = plugin;
    }

    public GameConfig config() { return plugin.gameConfig(); }
    public DataStore store() { return plugin.dataStore(); }

    public PlayerData data(Player p) {
        return store().get(p.getUniqueId(), p.getName(), config().zone);
    }

    public String today() {
        return LocalDate.now(config().zone).toString();
    }

    // --------------------------------------------------------- Word Search

    /** Void mask for the shared grid GUI: the three control slots (51-53) of a 54-slot inventory. */
    public static boolean[] controlVoids(int cols, int rows) {
        boolean[] voids = new boolean[cols * rows];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int slot = r * 9 + c; // GUI is 9 slots wide regardless of grid width
                if (slot == 51 || slot == 52 || slot == 53) voids[r * cols + c] = true;
            }
        }
        return voids;
    }

    public WordSearchRound startWordSearch(Player p, GameConfig.Theme theme, GameConfig.WsTier tier) {
        String band = tier.id();
        List<String> pool = plugin.wordLists().pool(theme.id(), band);
        WordSearchRound.Spec spec = tier.spec();
        WordSearchRound round = WordSearchRound.generate(tier.id(), theme.id(), spec, pool,
                controlVoids(spec.cols(), spec.rows()), rng, System.currentTimeMillis());
        PlayerData pd = data(p);
        pd.wordSearch = round;
        store().save(pd);
        plugin.getLogger().info("ExcelBrainGames: " + p.getName() + " started Word Search "
                + theme.id() + "/" + tier.id() + " (" + round.placements().size() + " words)");
        return round;
    }

    /** Settle a completed Word Search round. Returns the settlement, or null if it was already paid. */
    public Ledger.Settlement completeWordSearch(Player p, PlayerData pd, WordSearchRound round) {
        if (round.paid()) return null;
        GameConfig.WsTier tier = config().wsTiers.get(round.tierId());
        if (tier == null) return null;
        int bonusPct = 0;
        if (tier.parSeconds() > 0) {
            long elapsedSec = (System.currentTimeMillis() - round.startedMillis()) / 1000;
            if (elapsedSec <= tier.parSeconds()) bonusPct = tier.speedBonusPercent();
        }
        long base = Payouts.adjust(tier.payoutCents(), bonusPct, tier.kid(), config().isKid(p.getUniqueId()));
        Ledger.Settlement s = settle(p, pd, base);
        round.markPaid();
        pd.wordSearch = null;
        store().save(pd);
        announceCompletion(p, "Word Search", s, bonusPct > 0);
        return s;
    }

    // --------------------------------------------------------- Memory Match

    public MemoryRound startMemory(Player p, GameConfig.MemTier tier) {
        MemoryRound round = MemoryRound.deal(tier.id(), tier.pairs(), config().memorySymbols.size(),
                rng, System.currentTimeMillis());
        PlayerData pd = data(p);
        pd.memory = round;
        store().save(pd);
        return round;
    }

    public Ledger.Settlement completeMemory(Player p, PlayerData pd, MemoryRound round) {
        if (round.paid()) return null;
        GameConfig.MemTier tier = config().memTiers.get(round.tierId());
        if (tier == null) return null;
        int bonusPct = (tier.parFlips() > 0 && round.flips() <= tier.parFlips()) ? tier.parBonusPercent() : 0;
        long base = Payouts.adjust(tier.payoutCents(), bonusPct, tier.kid(), config().isKid(p.getUniqueId()));
        Ledger.Settlement s = settle(p, pd, base);
        round.markPaid();
        pd.memory = null;
        store().save(pd);
        announceCompletion(p, "Memory Match", s, bonusPct > 0);
        return s;
    }

    // --------------------------------------------------------- Sudoku

    public SudokuRound startSudoku(Player p, GameConfig.SudTier tier, boolean picture) {
        SudokuRound round = SudokuRound.generate(tier.id(), tier.size(), tier.boxRows(), tier.boxCols(),
                tier.givensMin(), tier.givensMax(), picture, rng, System.currentTimeMillis());
        PlayerData pd = data(p);
        pd.sudoku = round;
        store().save(pd);
        plugin.getLogger().info("ExcelBrainGames: " + p.getName() + " started Sudoku "
                + tier.id() + (picture ? " (picture)" : "") + " (" + round.givensCount() + " givens)");
        return round;
    }

    /**
     * Settle a solved Sudoku board. Money order (SPEC §4.4/§4.5): tier
     * payout, minus check-button deductions (adult tiers; the kid tier's
     * checks are free), then the par-time speed bonus on Medium/Hard,
     * then the kid-tier rule, then the shared ledger (cap + streak).
     */
    public Ledger.Settlement completeSudoku(Player p, PlayerData pd, SudokuRound round) {
        if (round.paid()) return null;
        GameConfig.SudTier tier = config().sudokuTiers.get(round.tierId());
        if (tier == null) return null;
        long afterChecks = tier.kid()
                ? tier.payoutCents()
                : SudokuRound.afterCheckDeduction(tier.payoutCents(), round.checksUsed(),
                        config().sudokuCheckCostPercent);
        int bonusPct = 0;
        if (tier.parSeconds() > 0) {
            long elapsedSec = (System.currentTimeMillis() - round.startedMillis()) / 1000;
            if (elapsedSec <= tier.parSeconds()) bonusPct = tier.speedBonusPercent();
        }
        long base = Payouts.adjust(afterChecks, bonusPct, tier.kid(), config().isKid(p.getUniqueId()));
        Ledger.Settlement s = settle(p, pd, base);
        round.markPaid();
        pd.sudoku = null;
        store().save(pd);
        announceCompletion(p, "Sudoku", s, bonusPct > 0);
        return s;
    }

    // --------------------------------------------------------- text games (Phase 3)

    /** Players currently in chat-answer mode, and for which game. */
    private final java.util.Map<java.util.UUID, String> pendingInput = new java.util.HashMap<>();

    public void expectInput(Player p, String gameId) { pendingInput.put(p.getUniqueId(), gameId); }
    public String pendingGame(java.util.UUID uuid) { return pendingInput.get(uuid); }
    public void clearInput(java.util.UUID uuid) { pendingInput.remove(uuid); }

    public QuickMathRound startQuickMath(Player p, GameConfig.QmTier tier) {
        QuickMathRound round = QuickMathRound.generate(tier.id(), tier.kid(), tier.choices(),
                rng, System.currentTimeMillis());
        PlayerData pd = data(p);
        pd.quickMath = round;
        store().save(pd);
        plugin.getLogger().info("ExcelBrainGames: " + p.getName() + " started Quick Math " + tier.id());
        return round;
    }

    /** Settle a finished Quick Math round (SPEC §6.3): tier x correct/10, floored to whole dollars, kid rule on kid tiers. */
    public Ledger.Settlement completeQuickMath(Player p, PlayerData pd, QuickMathRound round) {
        if (round.paid() || !round.finished()) return null;
        GameConfig.QmTier tier = config().qmTiers.get(round.tierId());
        if (tier == null) return null;
        long base = Payouts.adjust(round.payoutCents(tier.payoutCents()), 0, tier.kid(),
                config().isKid(p.getUniqueId()));
        Ledger.Settlement s = settle(p, pd, base);
        round.markPaid();
        pd.quickMath = null;
        clearInput(p.getUniqueId());
        store().save(pd);
        announceCompletion(p, "Quick Math", s, false);
        return s;
    }

    /** Start a Scramble round; null when the tier's word pool can't fill one. */
    public ScrambleRound startScramble(Player p, GameConfig.ScrTier tier) {
        List<String> pool = plugin.wordLists().poolAll(tier.kid() ? "kid" : "standard");
        ScrambleRound round = ScrambleRound.generate(tier.id(), tier.kid(), tier.perWordCents(),
                pool, rng, System.currentTimeMillis());
        if (round == null) return null;
        PlayerData pd = data(p);
        pd.scramble = round;
        store().save(pd);
        plugin.getLogger().info("ExcelBrainGames: " + p.getName() + " started Word Scramble " + tier.id());
        return round;
    }

    /**
     * Settle a Scramble round — on completion OR on the player's
     * "finish now" (SPEC §7.3: partial credit banks what was earned).
     * The kid-tier half rule applies to the kid tier's earnings.
     */
    public Ledger.Settlement completeScramble(Player p, PlayerData pd, ScrambleRound round) {
        if (round.paid()) return null;
        GameConfig.ScrTier tier = config().scrambleTiers.get(round.tierId());
        if (tier == null) return null;
        long base = Payouts.adjust(round.earnedCents(), 0, tier.kid(), config().isKid(p.getUniqueId()));
        Ledger.Settlement s = settle(p, pd, base);
        round.markPaid();
        pd.scramble = null;
        clearInput(p.getUniqueId());
        store().save(pd);
        announceCompletion(p, "Word Scramble", s, false);
        return s;
    }

    /** Settle today's Riddle solve (SPEC §8.2): $100 − $25/hint, floor $25, once per day. */
    public Ledger.Settlement completeRiddle(Player p, PlayerData pd) {
        if (!pd.riddleSolved || pd.riddlePaid) return null;
        long base = RiddleText.payoutCents(pd.riddleHints);
        Ledger.Settlement s = settle(p, pd, base);
        pd.riddlePaid = true;
        clearInput(p.getUniqueId());
        store().save(pd);
        announceCompletion(p, "Riddle of the Day", s, false);
        return s;
    }

    // --------------------------------------------------------- shared money path

    private Ledger.Settlement settle(Player p, PlayerData pd, long baseCents) {
        Ledger.Settlement s = pd.ledger.settle(today(), baseCents, config().dailyCapCents,
                config().streakBonusPerDayCents, config().streakBonusMaxCents);
        if (s.totalPaid() > 0) {
            Economy econ = plugin.economy();
            if (econ != null) {
                econ.depositPlayer(p, Money.centsToDollars(s.totalPaid()));
            } else {
                plugin.getLogger().warning("ExcelBrainGames: no economy provider — payout of "
                        + Money.fmt(s.totalPaid()) + " to " + p.getName() + " recorded in ledger only");
            }
        }
        return s;
    }

    private void announceCompletion(Player p, String game, Ledger.Settlement s, boolean bonusEarned) {
        GameConfig cfg = config();
        String line = cfg.msg("completed", "&a&l{game} complete! &r&e+{amount}")
                .replace("{game}", game)
                .replace("{amount}", Money.fmt(s.totalPaid()));
        p.sendMessage(LEGACY.deserialize(line));
        if (bonusEarned && s.basePaid() > 0) {
            p.sendMessage(LEGACY.deserialize(cfg.msg("bonus-earned", "&7Bonus for a fast / sharp finish included.")));
        }
        if (s.bonusPaid() > 0) {
            p.sendMessage(LEGACY.deserialize(cfg.msg("streak-bonus", "&dStreak bonus: +{amount} (day {streak})")
                    .replace("{amount}", Money.fmt(s.bonusPaid()))
                    .replace("{streak}", String.valueOf(s.streakAfter()))));
        }
        if (s.capped()) {
            p.sendMessage(LEGACY.deserialize(cfg.msg("cap-hit",
                    "&6You've hit today's Brain Games limit of {cap}. More tomorrow!")
                    .replace("{cap}", Money.fmt(config().dailyCapCents))));
        }
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
    }

    public static Component text(String legacy) {
        return LEGACY.deserialize(legacy);
    }
}
