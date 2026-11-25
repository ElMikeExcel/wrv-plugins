package excelemfixes.skillnudge;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.yaml.snakeyaml.Yaml;

/**
 * Reads EliteMobs' own skill-bonus catalog: the
 * plugins/EliteMobs/skillbonuses folder, one yml per bonus,
 * each carrying skillType (ARMOR, SWORDS, ...), unlockTier
 * (1..4) and isEnabled. The result is, per skill type, the
 * number of enabled bonuses in each tier — exactly the counts
 * {@link SkillNudgeMath} needs, taken from EM's own files so a
 * server that edits its bonus set gets correct nudges with no
 * code change (the pack's data-driven rule).
 *
 * Read-only. A missing/unreadable folder yields an empty map
 * and the module falls back to SkillNudgeMath's one-per-tier
 * floor; a malformed file is skipped, never fatal.
 */
public final class BonusCatalog {

    private BonusCatalog() {}

    /** Per skill type: int[4] of enabled bonus counts by tier (index 0 = tier 1). */
    public static Map<String, int[]> read(File folder, Logger log) {
        Map<String, int[]> out = new LinkedHashMap<>();
        if (folder == null || !folder.isDirectory()) {
            if (log != null) {
                log.warning("Skill Bonus Nudge: EliteMobs skillbonuses folder not found at "
                        + (folder == null ? "?" : folder.getPath())
                        + " — nudges use the one-bonus-per-tier floor until it is readable.");
            }
            return out;
        }
        File[] files = folder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return out;
        Yaml yaml = new Yaml();
        int parsed = 0;
        for (File f : files) {
            try (InputStream in = new FileInputStream(f)) {
                Object doc = yaml.load(in);
                if (!(doc instanceof Map<?, ?> map)) continue;
                if (!Boolean.TRUE.equals(map.get("isEnabled"))) continue;
                Object type = map.get("skillType");
                Object tier = map.get("unlockTier");
                if (!(type instanceof String skill) || !(tier instanceof Number t)) continue;
                int idx = t.intValue() - 1;
                if (idx < 0 || idx > 3) continue;
                out.computeIfAbsent(skill.toUpperCase(), k -> new int[4])[idx]++;
                parsed++;
            } catch (Throwable ignored) {
                // One bad file never blocks the catalog.
            }
        }
        if (log != null) {
            log.info("Skill Bonus Nudge: bonus catalog read — " + parsed
                    + " enabled bonuses across " + out.size() + " skill types.");
        }
        return out;
    }
}
