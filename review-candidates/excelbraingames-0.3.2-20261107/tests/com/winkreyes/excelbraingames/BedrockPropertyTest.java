package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.core.Dir;
import com.winkreyes.excelbraingames.core.WsGeometry;
import com.winkreyes.excelbraingames.games.wordsearch.WordSearchRound;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Word Search Bedrock property run (0.3.2) — the saturation proof and
 * the stream-findability gate over the SHIPPED word lists and the
 * SHIPPED config's bedrock-tiers, at the project's standard proof count
 * (500 SplitMix-mixed seeds per tier × theme, same convention and seed
 * mixing as PropertyTest).
 *
 * Per round, all asserted:
 *   - the full tier word count is placed (the generator saturates under
 *     Mike's length ruling on the 5-wide shapes);
 *   - the round records BEDROCK geometry;
 *   - no placement runs horizontally (E/W) with length &gt; 5;
 *   - every placement is readable off the grid AND traceable in the
 *     5-wide stream display in its placed direction (the property whose
 *     absence Mike hit on Nov 7, 2026);
 *   - the round solves end-to-end through the real two-tap checker;
 *   - every 50th round also round-trips through the save format with
 *     its geometry and board intact.
 */
public class BedrockPropertyTest {

    public static void main(String[] args) throws Exception {
        Check ck = new Check("BedrockPropertyTest");
        File build = new File(System.getProperty("build", "."));
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(build, "config.yml"));

        WordLists lists = new WordLists();
        lists.load(new File(build, "wordlists"), java.util.logging.Logger.getLogger("t"));

        runTier(ck, cfg, lists, "standard", List.of("animals", "food", "minecraft", "space", "ocean", "school"));
        runTier(ck, cfg, lists, "challenge", List.of("animals", "food", "minecraft", "space", "ocean", "school"));
        // Kid pools exist only for these themes (same as PropertyTest).
        runTier(ck, cfg, lists, "kid", List.of("animals", "food", "minecraft"));
        ck.finish();
    }

    static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    static void runTier(Check ck, YamlConfiguration cfg, WordLists lists, String tierId, List<String> themes) {
        String base = "games.wordsearch.tiers." + tierId + ".";
        String bed = "games.wordsearch.bedrock-tiers." + tierId + ".";
        ck.that(cfg.isSet(bed + "cols") && cfg.isSet(bed + "rows"),
                tierId + ": shipped config carries a bedrock shape");
        int cols = cfg.getInt(bed + "cols"), rows = cfg.getInt(bed + "rows");
        int words = cfg.getInt(base + "words");
        int minLen = cfg.getInt(base + "min-length"), maxLen = cfg.getInt(base + "max-length");
        Set<Dir> dirs = EnumSet.noneOf(Dir.class);
        for (String d : cfg.getStringList(base + "directions")) dirs.add(Dir.parse(d));
        WordSearchRound.Spec spec = new WordSearchRound.Spec(cols, rows, words, minLen, maxLen, dirs);
        boolean[] voids = WsGeometry.BEDROCK.controlVoids(cols, rows);
        ck.that(cols == WsGeometry.BEDROCK.guiCols,
                tierId + ": bedrock shape is exactly the stream width (" + cols + ")");
        ck.that(cols * rows <= 51, tierId + ": bedrock board ends before the control slots");

        for (String theme : themes) {
            List<String> pool = lists.pool(theme, tierId);
            ck.that(pool.size() >= words, tierId + "/" + theme + ": shipped pool covers the tier word count");
            Set<String> poolSet = new HashSet<>();
            for (String w : pool) {
                String word = w.trim().toUpperCase();
                if (word.length() >= minLen && word.length() <= maxLen) poolSet.add(word);
            }
            int full = 0;
            for (int seed = 0; seed < 500; seed++) {
                // SplitMix-mixed seeds (PropertyTest convention; a distinct
                // base so this run's seeds are not PropertyTest's).
                long mixed = mix(0xBED20C5L + tierId.hashCode() * 1_000_003L + theme.hashCode() * 131L + seed);
                WordSearchRound r = WordSearchRound.generate(tierId, theme, spec, pool, voids,
                        WsGeometry.BEDROCK, new Random(mixed), mixed);
                String ctx = tierId + "/" + theme + " seed " + seed;
                ck.that(r.geometry() == WsGeometry.BEDROCK, ctx + ": geometry recorded");
                ck.eq(words, r.placements().size(), ctx + ": full word count placed (saturation)");
                if (r.placements().size() == words) full++;
                for (int i = 0; i < r.placements().size(); i++) {
                    WordSearchRound.Placement p = r.placements().get(i);
                    ck.that(poolSet.contains(p.word), ctx + ": placed word is a pool word");
                    ck.that(p.word.equals(r.readPlacement(i)), ctx + ": placement readable");
                    ck.that(!(p.dr == 0 && p.len() > 5),
                            ctx + ": length ruling holds (" + p.word + " runs " + p.dr + "," + p.dc + ")");
                    for (int k = 0; k < p.len(); k++)
                        ck.that(!r.isVoid(p.idxAt(cols, k)), ctx + ": no void crossing");
                    // Stream traceability: consecutive letters step by the
                    // placement's own direction in the 5-wide display.
                    int slot0 = WsGeometry.BEDROCK.slotOf(p.startIdx(cols), cols);
                    int dr0 = slot0 / 5, dc0 = slot0 % 5;
                    for (int k = 0; k < p.len(); k++) {
                        int slot = WsGeometry.BEDROCK.slotOf(p.idxAt(cols, k), cols);
                        ck.that(slot / 5 == dr0 + p.dr * k && slot % 5 == dc0 + p.dc * k,
                                ctx + ": stream step " + k + " of " + p.word + " follows its direction");
                        ck.eq(p.word.charAt(k), r.letterAt(p.idxAt(cols, k)),
                                ctx + ": stream letter " + k + " of " + p.word);
                    }
                }
                // Solve the round end-to-end through the real checker.
                for (int i = 0; i < r.placements().size(); i++) {
                    WordSearchRound.Placement p = r.placements().get(i);
                    ck.that(r.tap(p.startIdx(cols)) == WordSearchRound.TapResult.PENDING_SET,
                            ctx + ": solve tap 1 " + p.word);
                    WordSearchRound.TapResult res = r.tap(p.endIdx(cols));
                    ck.that(res == WordSearchRound.TapResult.FOUND
                                    || res == WordSearchRound.TapResult.ALREADY_FOUND,
                            ctx + ": solve tap 2 " + p.word + " -> " + res);
                }
                ck.that(r.isComplete(), ctx + ": round completes through the checker");
                if (seed % 50 == 0) {
                    WordSearchRound back = WordSearchRound.fromMap(r.toMap());
                    ck.that(back.geometry() == WsGeometry.BEDROCK, ctx + ": save round-trip keeps geometry");
                    ck.eq(r.placements().size(), back.placements().size(),
                            ctx + ": save round-trip keeps the board");
                    for (int i = 0; i < r.placements().size(); i++)
                        ck.that(r.placements().get(i).word.equals(back.readPlacement(i)),
                                ctx + ": round-tripped placement readable");
                }
            }
            System.out.println("  bedrock " + tierId + "/" + theme + ": " + full + "/500 rounds at full count");
            ck.eq(500, full, tierId + "/" + theme + ": saturation 500/500");
        }
    }
}
