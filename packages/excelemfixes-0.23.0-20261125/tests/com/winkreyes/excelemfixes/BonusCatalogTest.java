import excelemfixes.skillnudge.BonusCatalog;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

/**
 * BonusCatalog against fixture files shaped like EliteMobs'
 * real skillbonuses folder: enabled counts per tier, disabled
 * files excluded, malformed files skipped, missing folder =
 * empty catalog (the module's fallback then applies).
 */
public final class BonusCatalogTest {

    public static void run() {
        System.out.println("BonusCatalogTest");
        try {
            File dir = Files.createTempDirectory("bonus-catalog-test").toFile();
            write(dir, "armor_a.yml",
                    "isEnabled: true\nskillType: ARMOR\nunlockTier: 1\n");
            write(dir, "armor_b.yml",
                    "isEnabled: true\nskillType: ARMOR\nunlockTier: 3\n");
            write(dir, "armor_c.yml",
                    "isEnabled: true\nskillType: ARMOR\nunlockTier: 3\n");
            write(dir, "swords_off.yml",
                    "isEnabled: false\nskillType: SWORDS\nunlockTier: 2\n");
            write(dir, "broken.yml", "this is: [not, valid: yaml");
            write(dir, "notayml.txt",
                    "isEnabled: true\nskillType: BOWS\nunlockTier: 1\n");

            Map<String, int[]> cat = BonusCatalog.read(dir, null);
            Check.ok(cat.containsKey("ARMOR"), "catalog has ARMOR");
            Check.eq(1, cat.get("ARMOR")[0], "armor tier 1 count = 1");
            Check.eq(0, cat.get("ARMOR")[1], "armor tier 2 count = 0");
            Check.eq(2, cat.get("ARMOR")[2], "armor tier 3 count = 2");
            Check.ok(!cat.containsKey("SWORDS"), "disabled bonus excluded (no SWORDS)");
            Check.ok(!cat.containsKey("BOWS"), "non-yml file ignored (no BOWS)");

            Map<String, int[]> missing = BonusCatalog.read(
                    new File(dir, "does-not-exist"), null);
            Check.ok(missing.isEmpty(), "missing folder reads as empty catalog");
        } catch (Exception e) {
            Check.ok(false, "BonusCatalogTest threw: " + e);
        }
    }

    private static void write(File dir, String name, String body) throws Exception {
        Files.writeString(new File(dir, name).toPath(), body);
    }
}
