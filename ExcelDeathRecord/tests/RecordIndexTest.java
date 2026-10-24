import com.winkreyes.exceldeathrecord.core.DeathRecord;
import com.winkreyes.exceldeathrecord.core.RecordIndex;

import java.util.List;

public class RecordIndexTest {

    private static DeathRecord rec(long epoch, String timeText, int dropCount, int invCount) {
        DeathRecord r = new DeathRecord();
        r.epochMillis = epoch;
        r.timeText = timeText;
        r.damageCause = "ENTITY_ATTACK";
        r.killer = "Zombie (minecraft:zombie)";
        r.world = "minecraft:overworld";
        r.x = -1280.5; r.y = 71.0; r.z = -300.25;
        for (int i = 0; i < dropCount; i++) r.drops.add(new DeathRecord.ItemEntry());
        for (int i = 0; i < invCount; i++) r.inventory.add(new DeathRecord.ItemEntry());
        return r;
    }

    public static void run() {
        System.out.println("RecordIndexTest");

        RecordIndex.Entry a = new RecordIndex.Entry("2026-10-22_212404.yml", rec(3000, "t3", 0, 0));
        RecordIndex.Entry b = new RecordIndex.Entry("2026-10-22_212405.yml", rec(5000, "t5", 0, 0));
        RecordIndex.Entry c = new RecordIndex.Entry("2026-10-22_212406.yml", rec(1000, "t1", 0, 0));
        List<RecordIndex.Entry> sorted = RecordIndex.newestFirst(List.of(a, b, c));
        Check.eq("2026-10-22_212405.yml", sorted.get(0).fileName, "newest first: highest epoch leads");
        Check.eq("2026-10-22_212406.yml", sorted.get(2).fileName, "newest first: lowest epoch trails");

        // Same-second tie: the "-2" file (later write) lists first.
        RecordIndex.Entry d1 = new RecordIndex.Entry("2026-10-22_212404.yml", rec(7000, "t7", 0, 0));
        RecordIndex.Entry d2 = new RecordIndex.Entry("2026-10-22_212404-2.yml", rec(7000, "t7", 0, 0));
        List<RecordIndex.Entry> tie = RecordIndex.newestFirst(List.of(d1, d2));
        Check.eq("2026-10-22_212404-2.yml", tie.get(0).fileName, "same-second tie: -2 write lists above the plain name");

        Check.eq(b, RecordIndex.pick(sorted, 1), "pick 1 = most recent");
        Check.eq(c, RecordIndex.pick(sorted, 3), "pick 3 = oldest of three");
        Check.eq(null, RecordIndex.pick(sorted, 4), "pick past the end = null");
        Check.eq(null, RecordIndex.pick(sorted, 0), "pick 0 = null (1-based)");

        DeathRecord r = rec(3000, "2026-10-22 21:24:04", 8, 8);
        Check.eq("2026-10-22 21:24:04 — ENTITY_ATTACK by Zombie (minecraft:zombie) at minecraft:overworld -1280.5, 71.0, -300.3 (8 dropped, 8 carried)",
            RecordIndex.summaryLine(r), "summary line exact");

        DeathRecord noKiller = rec(3000, "2026-10-22 21:24:04", 3, 5);
        noKiller.killer = "none";
        noKiller.damageCause = "LAVA";
        Check.eq("2026-10-22 21:24:04 — LAVA at minecraft:overworld -1280.5, 71.0, -300.3 (3 dropped, 5 carried)",
            RecordIndex.summaryLine(noKiller), "summary line omits a none killer");

        DeathRecord.ItemEntry sword = new DeathRecord.ItemEntry();
        sword.slot = "storage-0"; sword.id = "minecraft:diamond_sword"; sword.count = 1; sword.damage = 394;
        sword.enchants.put("minecraft:sharpness", 4);
        sword.enchants.put("minecraft:fire_aspect", 2);
        Check.eq("storage-0: minecraft:diamond_sword x1 damage=394 enchants: minecraft:sharpness=4, minecraft:fire_aspect=2",
            RecordIndex.itemLine(sword), "item line: enchanted + damaged");
        sword.customName = "Rig Blade";
        Check.eq("storage-0: minecraft:diamond_sword x1 damage=394 enchants: minecraft:sharpness=4, minecraft:fire_aspect=2 name=\"Rig Blade\"",
            RecordIndex.itemLine(sword), "item line: custom name appended");

        DeathRecord.ItemEntry plain = new DeathRecord.ItemEntry();
        plain.slot = "drop-1"; plain.id = "minecraft:cobblestone"; plain.count = 64;
        Check.eq("drop-1: minecraft:cobblestone x64", RecordIndex.itemLine(plain), "item line: plain stack stays bare");
    }
}
