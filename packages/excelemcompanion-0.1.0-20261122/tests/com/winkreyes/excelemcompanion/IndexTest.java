import com.winkreyes.excelemcompanion.dropguide.DropIndex;
import com.winkreyes.excelemcompanion.dropguide.DropGuideMenus;
import org.bukkit.Material;

import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * The real generated index (src/drops-index.yml, from the live
 * EliteMobs configs) must load, be complete, and obey Mike's rules:
 * no drop chances anywhere, class-loot drops state their stats roll,
 * quest items are present, every icon is a real Material.
 */
public final class IndexTest {

    private static final Pattern DROP_CHANCE_TEXT = Pattern.compile(
            "(?i)(drop chance|drop rate|loot chance|chance to drop"
                    + "|chance of dropping|%\\s*(drop|loot|chance to drop)"
                    + "|(drop|loot)\\s*(chance|rate))");

    private static DropIndex load() throws Exception {
        try (FileReader r = new FileReader("src/drops-index.yml",
                StandardCharsets.UTF_8)) {
            return DropIndex.load(r);
        }
    }

    public static void run() {
        System.out.println("IndexTest");
        DropIndex idx;
        try {
            idx = load();
        } catch (Exception e) {
            Check.ok(false, "index loads (" + e + ")");
            return;
        }
        Check.ok(idx.places().size() >= 30,
                "places >= 30 (actual " + idx.places().size() + ")");
        Check.ok(idx.mobCount() >= 400,
                "mobs >= 400 (actual " + idx.mobCount() + ")");
        Check.ok(idx.dropCount() >= 4000,
                "drops >= 4000 (actual " + idx.dropCount() + ")");

        // The Sewers / Shredder specimen (Mike's acceptance case).
        DropIndex.Place sewers = idx.placeById("primis_sewers");
        Check.ok(sewers != null, "Sewers place exists");
        DropIndex.Mob shredder = sewers == null ? null
                : sewers.mobs().stream()
                        .filter(m -> m.name().contains("Shredder"))
                        .findFirst().orElse(null);
        Check.ok(shredder != null, "Shredder mob exists in Sewers");
        DropIndex.Drop keys = shredder == null ? null
                : shredder.drops().stream()
                        .filter(d -> d.name().contains("Sewage Keys"))
                        .findFirst().orElse(null);
        Check.ok(keys != null, "Shredder drops the Sewage Keys");
        if (keys != null) {
            Check.eq("quest", keys.kind(), "Sewage Keys is a quest drop");
            Check.eq("BONE_MEAL", keys.material(),
                    "Sewage Keys icon is BONE_MEAL");
        }
        DropIndex.Drop wand = shredder == null ? null
                : shredder.drops().stream()
                        .filter(d -> d.name().contains("Caravan Pointer"))
                        .findFirst().orElse(null);
        Check.ok(wand != null && "classloot".equals(wand.kind()),
                "Shredder drops the Caravan Pointer as class loot");

        // Whole-index invariants.
        boolean materialsOk = true;
        boolean classlootWordingOk = true;
        boolean noChanceText = true;
        boolean everyMobHasDrops = true;
        boolean namesOk = true;
        for (DropIndex.Place p : idx.places()) {
            if (p.id().isBlank() || p.name().isBlank()) namesOk = false;
            for (DropIndex.Mob m : p.mobs()) {
                if (m.id().isBlank() || m.name().isBlank()) namesOk = false;
                if (m.drops().isEmpty()) everyMobHasDrops = false;
                if (DROP_CHANCE_TEXT.matcher(m.name()).find()) {
                    noChanceText = false;
                }
                for (String line : DropGuideMenus.mobLore(m)) {
                    if (DROP_CHANCE_TEXT.matcher(line).find()) {
                        noChanceText = false;
                    }
                }
                for (DropIndex.Drop d : m.drops()) {
                    if (d.name().isBlank()) namesOk = false;
                    try {
                        Material.valueOf(d.material());
                    } catch (IllegalArgumentException e) {
                        materialsOk = false;
                        System.out.println("    bad material: " + d.material()
                                + " on " + d.name());
                    }
                    if (DROP_CHANCE_TEXT.matcher(d.name()).find()) {
                        noChanceText = false;
                    }
                    for (String line : DropGuideMenus.dropLore(d)) {
                        if (DROP_CHANCE_TEXT.matcher(line).find()) {
                            noChanceText = false;
                            System.out.println("    chance text: " + line);
                        }
                    }
                    if (("classloot".equals(d.kind())
                            || "classloot-generic".equals(d.kind())
                            || "random".equals(d.kind()))
                            && !DropGuideMenus.dropLore(d).toString()
                                    .contains("Stats are rolled when this drops.")) {
                        classlootWordingOk = false;
                        System.out.println("    missing rolled wording: "
                                + d.name());
                    }
                }
            }
        }
        Check.ok(materialsOk, "every drop icon is a valid Material");
        Check.ok(classlootWordingOk,
                "every generated-loot drop states its stats roll at drop");
        Check.ok(noChanceText, "no drop-chance text anywhere in the guide");
        Check.ok(everyMobHasDrops, "every listed mob has at least one drop");
        Check.ok(namesOk, "every place/mob/drop has a non-blank name");

        // The raw index file must not carry chance data at all.
        try {
            String raw = Files.readString(Path.of("src/drops-index.yml"));
            Check.ok(!raw.contains("chance=") && !raw.contains("dropChance"),
                    "raw index carries no chance fields");
        } catch (Exception e) {
            Check.ok(false, "raw index readable (" + e + ")");
        }
    }
}
