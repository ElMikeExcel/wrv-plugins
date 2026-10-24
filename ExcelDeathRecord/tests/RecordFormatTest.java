import com.winkreyes.exceldeathrecord.core.DeathRecord;
import com.winkreyes.exceldeathrecord.core.DeathSnapshot;
import com.winkreyes.exceldeathrecord.core.RecordFormat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * RecordFormat round-trips + the owner's hard-requirement test
 * (Oct 22, 2026 amendment): a death whose drops are destroyed —
 * LAVA, VOID — must produce a record identical in content to a
 * normal-cause death with the same inventory, because the
 * snapshot is built from the event's data, not from item
 * entities in the world.
 */
public class RecordFormatTest {

    private static DeathSnapshot.RawItem raw(String slot, String id, int count, int damage,
                                             Map<String, Integer> enchants, String name, String bytesText) {
        DeathSnapshot.RawItem ri = new DeathSnapshot.RawItem();
        ri.slot = slot; ri.id = id; ri.count = count; ri.damage = damage;
        ri.enchants = enchants; ri.customName = name;
        ri.bytes = bytesText.getBytes(StandardCharsets.UTF_8);
        return ri;
    }

    /** One fixed kit, reused across causes: a god sword, a plain
     *  stack, an enchanted book, armor, offhand shield. */
    private static List<DeathSnapshot.RawItem> kit(String slotPrefix, int dropOffset) {
        return List.of(
            raw(slotPrefix + dropOffset, "minecraft:diamond_sword", 1, 394,
                Map.of("minecraft:sharpness", 4, "minecraft:fire_aspect", 2, "minecraft:looting", 3),
                "", "SWORD-BYTES"),
            raw(slotPrefix + (dropOffset + 1), "minecraft:cobblestone", 64, -1,
                Map.of(), "", "COBBLE-BYTES"),
            raw(slotPrefix + (dropOffset + 2), "minecraft:enchanted_book", 1, -1,
                Map.of("minecraft:mending", 1), "", "BOOK-BYTES"));
    }

    static DeathRecord deathWithCause(String cause, String deathMessage, String killer) {
        List<DeathSnapshot.RawItem> inv = List.of(
            raw("storage-0", "minecraft:diamond_sword", 1, 394,
                Map.of("minecraft:sharpness", 4, "minecraft:fire_aspect", 2, "minecraft:looting", 3),
                "", "SWORD-BYTES"),
            raw("storage-1", "minecraft:cobblestone", 64, -1, Map.of(), "", "COBBLE-BYTES"),
            raw("helmet", "minecraft:iron_helmet", 1, 14, Map.of(), "", "HELM-BYTES"),
            raw("chestplate", "minecraft:diamond_chestplate", 1, 1,
                Map.of("minecraft:protection", 4), "", "CHEST-BYTES"),
            raw("offhand", "minecraft:shield", 1, 0, Map.of(), "", "SHIELD-BYTES"));
        return DeathSnapshot.build(
            ".BedrockAlex", "00000000-0000-0000-0009-000000000001",
            1792724644000L, "2026-10-22 21:24:04",
            "minecraft:the_nether", 100.5, 32.0, -40.25, "SURVIVAL",
            deathMessage, cause, killer,
            20, 1395, 87, false,
            kit("drop-", 0), inv);
    }

    public static void run() {
        System.out.println("RecordFormatTest");

        // Full round-trip of a rich record.
        DeathRecord r = deathWithCause("ENTITY_ATTACK", ".BedrockAlex was slain by Zombie", "Zombie (minecraft:zombie)");
        String text = RecordFormat.write(r);
        DeathRecord back = RecordFormat.read(text);
        Check.eq(r, back, "rich record round-trips field-for-field");
        Check.eq(text, RecordFormat.write(back), "write is stable across a read (write-read-write)");

        // Hostile strings: quotes, backslash, colon, unicode, newline in a name.
        DeathRecord hostile = deathWithCause("ENTITY_ATTACK", "x: \"quoted\" \\ path\nline2", "none");
        hostile.playerName = ".bedrock alex:né";
        hostile.drops.get(0).customName = "Sword \"of\" \\Truth\\ — é";
        DeathRecord hostileBack = RecordFormat.read(RecordFormat.write(hostile));
        Check.eq(hostile, hostileBack, "hostile strings round-trip (quotes, backslash, colon, unicode, newline)");

        // Empty sections both ways.
        DeathRecord empty = DeathSnapshot.build(
            "RigDead", "22222222-2222-2222-2222-222222222222", 1L, "2026-10-23 00:00:01",
            "minecraft:overworld", 0.5, 101.0, 0.5, "SURVIVAL",
            "RigDead died", "GENERIC", "none", 0, 0, 0, true,
            List.of(), List.of());
        DeathRecord emptyBack = RecordFormat.read(RecordFormat.write(empty));
        Check.eq(empty, emptyBack, "empty drops + empty inventory round-trip");
        Check.ok(RecordFormat.write(empty).contains("drops: []"), "empty drops written as drops: []");
        Check.ok(RecordFormat.write(empty).contains("keep-inventory: true"), "keepInventory true recorded");

        // THE AMENDMENT TEST: same kit, three causes. The item
        // content must be identical across all three and complete
        // (byte payloads intact) — destruction of the drops in the
        // world cannot touch a record built from the event.
        DeathRecord normal = deathWithCause("ENTITY_ATTACK", ".BedrockAlex was slain by Zombie", "Zombie (minecraft:zombie)");
        DeathRecord lava = deathWithCause("LAVA", ".BedrockAlex tried to swim in lava", "none");
        DeathRecord voidDeath = deathWithCause("VOID", ".BedrockAlex fell out of the world", "none");

        DeathRecord lavaBack = RecordFormat.read(RecordFormat.write(lava));
        DeathRecord voidBack = RecordFormat.read(RecordFormat.write(voidDeath));
        Check.eq(lava, lavaBack, "LAVA death record round-trips complete");
        Check.eq(voidDeath, voidBack, "VOID death record round-trips complete");
        Check.eq("LAVA", lavaBack.damageCause, "LAVA death is labeled LAVA");
        Check.eq("VOID", voidBack.damageCause, "VOID death is labeled VOID");
        Check.eq(normal.drops, lavaBack.drops, "LAVA record drops identical to normal-cause drops");
        Check.eq(normal.drops, voidBack.drops, "VOID record drops identical to normal-cause drops");
        Check.eq(normal.inventory, lavaBack.inventory, "LAVA record inventory identical to normal-cause inventory");
        Check.eq(normal.inventory, voidBack.inventory, "VOID record inventory identical to normal-cause inventory");
        Check.eq(3, lavaBack.drops.size(), "LAVA record holds the full drop list (3 stacks)");
        Check.eq(5, voidBack.inventory.size(), "VOID record holds the full inventory (5 stacks)");
        Check.ok(!lavaBack.drops.get(0).bytesB64.isEmpty(), "LAVA record sword bytes present (byte-exact payload)");
        Check.eq(normal.drops.get(0).bytesB64, voidBack.drops.get(0).bytesB64,
            "VOID record sword bytes identical to normal-cause bytes");

        // Null bytes from a failed serialization still record the item.
        DeathSnapshot.RawItem noBytes = raw("storage-3", "minecraft:torch", 12, -1, Map.of(), "", "X");
        noBytes.bytes = null;
        DeathRecord nb = DeathSnapshot.build("P", "u", 5L, "t", "w", 0, 0, 0, "SURVIVAL",
            "m", "FALL", "none", 0, 0, 0, false, List.of(), List.of(noBytes));
        Check.eq("", nb.inventory.get(0).bytesB64, "null bytes recorded as empty payload, item still listed");
        Check.eq(nb, RecordFormat.read(RecordFormat.write(nb)), "null-bytes record round-trips");
    }
}
