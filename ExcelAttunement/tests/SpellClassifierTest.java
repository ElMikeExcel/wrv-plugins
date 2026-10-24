import com.winkreyes.excelattunement.core.SpellClass;
import com.winkreyes.excelattunement.core.SpellClassifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pins the classification table against the REAL MagicGate stage
 * configs: every granted spell in every stage must be classified, the
 * class split must match the recorded counts, and the signal rules
 * re-derived from the spell definitions (tests/spell-signals.txt,
 * produced by tools/gen_spell_table.py) must hold.
 */
public class SpellClassifierTest {
    // The SUPPORT judgment calls whose base-level definitions carry
    // their support signal as PotionEffect rather than a Heal/Feed
    // action: beneficial buffs (blessing, boon, gills, invincible,
    // cloak) and cure — negative-effect removal; Feed joins cure's own
    // action list at its higher levels. Pinned by name so a definition
    // change forces a conscious re-decision.
    static final Set<String> BUFF_SUPPORT = Set.of(
            "blessing", "boon", "gills", "invincible", "cloak", "cure");

    static void run(Path stagesDir, Path tableFile, Path signalsFile) throws IOException {
        System.out.println("== SpellClassifierTest ==");
        SpellClassifier c = SpellClassifier.fromLines(Files.readAllLines(tableFile));

        Check.eq(100, c.size(), "table covers exactly the 100 issuable spells");
        Map<SpellClass, Integer> counts = new EnumMap<>(SpellClass.class);
        for (SpellClass cls : c.table().values()) counts.merge(cls, 1, Integer::sum);
        Check.eq(36, counts.getOrDefault(SpellClass.COMBAT, 0), "36 COMBAT");
        Check.eq(12, counts.getOrDefault(SpellClass.SUPPORT, 0), "12 SUPPORT");
        Check.eq(52, counts.getOrDefault(SpellClass.UTILITY, 0), "52 UTILITY");

        // Every granted spell in every stage is classified.
        Pattern grant = Pattern.compile("permission set magic\\.cast\\.([a-z_0-9]+)");
        Set<String> allGrants = new HashSet<>();
        for (int s = 0; s <= 5; s++) {
            String txt = Files.readString(stagesDir.resolve("stage" + s + "/grants.txt"));
            Matcher m = grant.matcher(txt);
            while (m.find()) allGrants.add(m.group(1));
        }
        Check.eq(100, allGrants.size(), "grant universe across stages 0-5 is 100 spells");
        for (String g : allGrants) {
            Check.ok(c.isKnown(g), "granted spell classified: " + g);
        }
        Check.eq(allGrants, c.table().keySet(), "table keys == grant universe (no more, no less)");

        // Signal cross-checks from the real definitions.
        for (String line : Files.readAllLines(signalsFile)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] p = line.trim().split("\\s+");
            String key = p[0];
            boolean hasDamage = p[1].equals("1");
            boolean hasHealFeed = p[2].equals("1");
            SpellClass cls = c.classify(key);
            if (hasDamage) Check.eq(SpellClass.COMBAT, cls, key + ": Damage action -> COMBAT");
            if (hasHealFeed && !hasDamage) Check.eq(SpellClass.SUPPORT, cls, key + ": heal/feed, no damage -> SUPPORT");
            if (cls == SpellClass.SUPPORT) {
                Check.ok(hasHealFeed || BUFF_SUPPORT.contains(key),
                        key + ": SUPPORT carries a heal/feed signal or is a pinned buff call");
            }
        }

        // Key normalization + the fail-closed default.
        Check.eq(SpellClass.SUPPORT, c.classify("heal"), "heal is SUPPORT");
        Check.eq(SpellClass.SUPPORT, c.classify("heal|3"), "leveled key heal|3 classifies as heal");
        Check.eq(SpellClass.COMBAT, c.classify("CHAINLIGHTNING"), "keys are case-insensitive");
        Check.eq(SpellClass.COMBAT, c.classify("stormshackle"), "our chain-lightning variant is COMBAT");
        Check.eq(SpellClass.UTILITY, c.classify("recall"), "recall is UTILITY");
        Check.ok(!c.isKnown("fireball"), "a spell from the locked shelf is not in the table");
        Check.eq(SpellClass.COMBAT, c.classify("fireball"), "unknown key fails closed as COMBAT");

        // Parser discipline.
        try {
            SpellClassifier.fromLines(List.of("heal = SUPPORT", "heal = COMBAT"));
            Check.ok(false, "duplicate key rejected");
        } catch (IllegalArgumentException e) {
            Check.ok(true, "duplicate key rejected");
        }
        try {
            SpellClassifier.fromLines(List.of("heal = WRONG"));
            Check.ok(false, "bad class rejected");
        } catch (IllegalArgumentException e) {
            Check.ok(true, "bad class rejected");
        }
    }
}
