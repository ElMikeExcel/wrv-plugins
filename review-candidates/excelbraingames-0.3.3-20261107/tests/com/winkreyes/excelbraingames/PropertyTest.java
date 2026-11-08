package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.core.Dir;
import com.winkreyes.excelbraingames.games.wordsearch.WordSearchRound;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Generator property tests over the SHIPPED content (build dir passed as
 * -Dbuild=<path>): wordlists/*.yml + config.yml tier shapes.
 *
 * For every tier and theme, 500 seeded rounds:
 *  - the full tier word count is placed;
 *  - every placed word reads back off the grid at its stored placement
 *    (findable by construction) and is actually FOUND by tapping its
 *    endpoints through the real checker — a full solve completes the round;
 *  - every placement is in-bounds, direction-legal for the tier, and never
 *    crosses a GUI control void;
 *  - accidental words (listed pool words the filler happens to form at
 *    non-placement positions) are REJECTED by the checker — it honors stored
 *    placements only (SPEC §3.1 accepted behavior), so filler can never
 *    break or fake a solve.
 */
public final class PropertyTest {

    static boolean[] voids(int cols, int rows) {
        boolean[] v = new boolean[cols * rows];
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            int slot = r * 9 + c;
            if (slot == 51 || slot == 52 || slot == 53) v[r * cols + c] = true;
        }
        return v;
    }

    public static void main(String[] args) throws Exception {
        Check ck = new Check("PropertyTest");
        File build = new File(System.getProperty("build", "."));
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(build, "config.yml"));

        WordLists lists = new WordLists();
        lists.load(new File(build, "wordlists"), java.util.logging.Logger.getLogger("t"));

        List<String> themes = List.of("animals", "food", "minecraft", "space", "ocean", "school");
        runTier(ck, cfg, lists, "standard", themes);
        runTier(ck, cfg, lists, "challenge", themes);
        // Kid pools load from wordlists/ too since Nov 4, 2026: Mike approved
        // the lists and they were promoted out of wordlists/draft/.
        runTier(ck, cfg, lists, "kid", List.of("animals", "food", "minecraft"));
        ck.finish();
    }

    static void runTier(Check ck, YamlConfiguration cfg, WordLists lists, String tierId, List<String> themes) {
        String base = "games.wordsearch.tiers." + tierId + ".";
        int cols = cfg.getInt(base + "cols"), rows = cfg.getInt(base + "rows");
        int words = cfg.getInt(base + "words");
        int minLen = cfg.getInt(base + "min-length"), maxLen = cfg.getInt(base + "max-length");
        Set<Dir> dirs = EnumSet.noneOf(Dir.class);
        for (String d : cfg.getStringList(base + "directions")) dirs.add(Dir.parse(d));
        WordSearchRound.Spec spec = new WordSearchRound.Spec(cols, rows, words, minLen, maxLen, dirs);
        boolean[] voids = voids(cols, rows);
        long accidentalProbes = 0;

        for (String theme : themes) {
            List<String> pool = lists.pool(theme, tierId);
            ck.that(pool.size() >= words, tierId + "/" + theme + ": shipped pool covers the tier word count");
            Set<String> poolSet = new HashSet<>();
            for (String w : pool) {
                String word = w.trim().toUpperCase();
                if (word.length() >= minLen && word.length() <= maxLen) poolSet.add(word);
            }
            for (int seed = 0; seed < 500; seed++) {
                // SplitMix-mixed seeds: consecutive java.util.Random seeds are
                // LCG-correlated and produced systematically bad deals for some
                // pools (a harness artifact — the same pools pass 100/100 under
                // plain and under mixed seeds; production uses a time-seeded
                // shared Random).
                long mixed = (seed + 0x9E3779B97F4A7C15L) * 0xBF58476D1CE4E5B9L;
                mixed ^= (tierId.hashCode() * 31L + theme.hashCode()) * 0x94D049BB133111EBL;
                WordSearchRound r = WordSearchRound.generate(tierId, theme, spec, pool, voids,
                        new Random(mixed), 0L);
                String tag = tierId + "/" + theme + " seed " + seed;
                ck.eq(words, r.placements().size(), tag + ": full word count");
                Set<Long> endpointPairs = new HashSet<>();
                for (int i = 0; i < r.placements().size(); i++) {
                    WordSearchRound.Placement p = r.placements().get(i);
                    Dir d = Dir.ofDelta(p.dr, p.dc);
                    ck.that(d != null && dirs.contains(d), tag + ": direction legal");
                    ck.that(r.readPlacement(i).equals(p.word), tag + ": word readable at placement");
                    for (int k = 0; k < p.len(); k++) {
                        ck.that(!r.isVoid(p.idxAt(cols, k)), tag + ": no void crossing");
                    }
                    endpointPairs.add(pairKey(p.startIdx(cols), p.endIdx(cols)));
                    endpointPairs.add(pairKey(p.endIdx(cols), p.startIdx(cols)));
                }
                // Full solve through the real two-tap checker.
                WordSearchRound solve = WordSearchRound.fromMap(r.toMap());
                for (WordSearchRound.Placement p : solve.placements()) {
                    solve.tap(p.startIdx(cols));
                    ck.that(solve.tap(p.endIdx(cols)) == WordSearchRound.TapResult.FOUND,
                            tag + ": solve tap finds " + p.word);
                }
                ck.that(solve.isComplete(), tag + ": full solve completes the round");
                // Accidental-word probe: any pool word formed by cells that are NOT
                // a stored placement must be rejected by the checker.
                boolean probed = false;
                outer:
                for (int a = 0; a < cols * rows && !probed; a++) {
                    if (r.isVoid(a) || r.letterAt(a) == 0) continue;
                    for (Dir d : Dir.values()) {
                        for (int len = minLen; len <= maxLen; len++) {
                            int er = a / cols + d.dr * (len - 1), ec = a % cols + d.dc * (len - 1);
                            if (er < 0 || er >= rows || ec < 0 || ec >= cols) continue;
                            int b = er * cols + ec;
                            if (endpointPairs.contains(pairKey(a, b))) continue;
                            StringBuilder sb = new StringBuilder();
                            boolean voidInLine = false;
                            for (int k = 0; k < len; k++) {
                                int idx = (a / cols + d.dr * k) * cols + (a % cols + d.dc * k);
                                if (r.isVoid(idx)) { voidInLine = true; break; }
                                sb.append(r.letterAt(idx));
                            }
                            if (voidInLine) continue;
                            if (poolSet.contains(sb.toString())) {
                                WordSearchRound probe = WordSearchRound.fromMap(r.toMap());
                                probe.tap(a);
                                WordSearchRound.TapResult res = probe.tap(b);
                                ck.that(res == WordSearchRound.TapResult.NO_MATCH,
                                        tag + ": accidental word '" + sb + "' rejected by checker");
                                accidentalProbes++;
                                probed = true;
                                break outer;
                            }
                        }
                    }
                }
            }
        }
        System.out.println("  " + tierId + ": accidental-word probes executed: " + accidentalProbes);
    }

    static long pairKey(int a, int b) { return ((long) a << 32) | (b & 0xffffffffL); }
}
