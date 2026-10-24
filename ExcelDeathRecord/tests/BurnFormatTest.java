import com.winkreyes.exceldeathrecord.core.BurnFormat;
import com.winkreyes.exceldeathrecord.core.DeathRecord;
import com.winkreyes.exceldeathrecord.core.DestructionRecord;
import com.winkreyes.exceldeathrecord.core.BurnIndex;

public class BurnFormatTest {
    public static void run() {
        System.out.println("BurnFormatTest");

        DestructionRecord r = new DestructionRecord();
        r.ownerName = ".BedrockAlex";
        r.ownerUuid = "00000000-0000-0000-0009-000000000001";
        r.epochMillis = 1792724700000L;
        r.timeText = "2026-10-22 21:25:00";
        r.cause = "LAVA";
        r.world = "minecraft:the_nether";
        r.x = 100.5; r.y = 32.0; r.z = -40.5;
        r.ageSeconds = 42;
        r.item.slot = "destroyed";
        r.item.id = "minecraft:diamond_sword";
        r.item.count = 1;
        r.item.damage = 394;
        r.item.enchants.put("minecraft:sharpness", 4);
        r.item.enchants.put("minecraft:fire_aspect", 2);
        r.item.customName = "";
        r.item.bytesB64 = "U1dPUkQtQllURVM=";

        String text = BurnFormat.write(r);
        DestructionRecord back = BurnFormat.read(text);
        Check.eq(r, back, "burn record round-trips field-for-field");
        Check.eq(text, BurnFormat.write(back), "burn write is stable across a read");

        // Unknown owner + unknown age.
        DestructionRecord unknown = new DestructionRecord();
        unknown.cause = "VOID";
        unknown.world = "minecraft:overworld";
        unknown.item.id = "minecraft:torch";
        unknown.item.count = 12;
        unknown.item.slot = "destroyed";
        DestructionRecord unknownBack = BurnFormat.read(BurnFormat.write(unknown));
        Check.eq(unknown, unknownBack, "unknown-owner record round-trips (owner stays empty, never guessed)");
        Check.ok(!BurnFormat.write(unknown).contains("age-seconds"), "unknown age is omitted from the file");

        // The listing line.
        Check.eq("2026-10-22 21:25:00 — minecraft:diamond_sword x1 damage=394 enchants: minecraft:sharpness=4, minecraft:fire_aspect=2 destroyed by LAVA at minecraft:the_nether 100.5, 32.0, -40.5 (owner: .BedrockAlex, after 42s in the world)",
            BurnIndex.summaryLine(r), "burn summary line exact");
        DestructionRecord u2 = new DestructionRecord();
        u2.timeText = "2026-10-22 21:26:00";
        u2.cause = "VOID";
        u2.world = "minecraft:overworld";
        u2.x = 1.0; u2.y = -70.0; u2.z = 2.0;
        u2.item.id = "minecraft:torch"; u2.item.count = 12;
        Check.eq("2026-10-22 21:26:00 — minecraft:torch x12 destroyed by VOID at minecraft:overworld 1.0, -70.0, 2.0 (owner: unknown)",
            BurnIndex.summaryLine(u2), "burn summary line: unknown owner, unknown age");

        // Ordering.
        BurnIndex.Entry older = new BurnIndex.Entry("2026-10-22_212400.yml", unknown);
        DestructionRecord newerRec = new DestructionRecord();
        newerRec.epochMillis = 1792724700000L;
        BurnIndex.Entry newer = new BurnIndex.Entry("2026-10-22_212500.yml", newerRec);
        java.util.List<BurnIndex.Entry> sorted = BurnIndex.newestFirst(java.util.List.of(older, newer));
        Check.eq("2026-10-22_212500.yml", sorted.get(0).fileName, "burn index sorts newest first");
    }
}
