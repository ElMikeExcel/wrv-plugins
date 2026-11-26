package excelemfixes.mcmmo;

import java.util.Locale;

/**
 * The EliteMobs training-dummy boss-file matcher, mirrored from
 * the mcMMO fork's recognition set (the fork's Layer-2 fix of
 * Nov 15, 2026): the Adventurers Guild dummy files
 * ({@code training_dummy_lv*.yml}) plus EliteMobs'
 * {@code damage_test_dummy.yml}. Directory prefixes and case
 * are ignored.
 *
 * <p>These bosses are excluded from kill awards before pricing,
 * on top of the structural passive-target rule (a dummy is
 * {@code ai: false} with {@code damageMultiplier: 0.0}, so the
 * passive rule already catches the class; this filename set is
 * the same belt-and-braces the fork itself keeps).
 */
public final class DummyFiles {

    private DummyFiles() {
    }

    public static boolean isTrainingDummyFilename(String filename) {
        if (filename == null) return false;
        String base = filename.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) base = base.substring(slash + 1);
        String lower = base.toLowerCase(Locale.ROOT);
        return (lower.startsWith("training_dummy_lv") && lower.endsWith(".yml"))
                || lower.equals("damage_test_dummy.yml");
    }
}
