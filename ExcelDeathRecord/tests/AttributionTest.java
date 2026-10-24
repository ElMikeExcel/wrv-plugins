import com.winkreyes.exceldeathrecord.core.Attribution;
import com.winkreyes.exceldeathrecord.core.DeathRecord;

public class AttributionTest {

    private static Attribution.DropInfo tracked(String uuid, String name, long droppedAt) {
        Attribution.DropInfo d = new Attribution.DropInfo();
        d.ownerUuid = uuid; d.ownerName = name; d.droppedEpochMillis = droppedAt;
        return d;
    }

    public static void run() {
        System.out.println("AttributionTest");

        // 1) The entity's own owner wins over tracking.
        Attribution.Owner o1 = Attribution.resolve("uuid-entity", "EntityName",
            tracked("uuid-tracked", "TrackedName", 1000));
        Check.eq("uuid-entity", o1.uuid, "entity owner wins: uuid");
        Check.eq("EntityName", o1.name, "entity owner wins: name");

        // 2) No entity owner: the drop tracking resolves the person.
        Attribution.Owner o2 = Attribution.resolve(null, null, tracked("uuid-tracked", "TrackedName", 1000));
        Check.eq("uuid-tracked", o2.uuid, "tracked fallback: uuid");
        Check.eq("TrackedName", o2.name, "tracked fallback: name");
        Attribution.Owner o2b = Attribution.resolve("", "", tracked("uuid-tracked", "TrackedName", 1000));
        Check.eq("uuid-tracked", o2b.uuid, "empty entity owner also falls back to tracking");

        // 3) Neither knows: unknown, never a guess.
        Attribution.Owner o3 = Attribution.resolve(null, null, null);
        Check.eq("", o3.uuid, "unknown owner: empty uuid");
        Check.eq("", o3.name, "unknown owner: empty name");
        Attribution.Owner o3b = Attribution.resolve(null, null, tracked("", "", 0));
        Check.eq("", o3b.uuid, "empty tracked owner stays unknown");

        // Entity owner with no resolvable name: uuid kept, name empty.
        Attribution.Owner o4 = Attribution.resolve("uuid-entity", null, null);
        Check.eq("uuid-entity", o4.uuid, "uuid-only owner keeps the uuid");
        Check.eq("", o4.name, "uuid-only owner has empty name");

        // Ages.
        Check.eq(42L, Attribution.ageSeconds(tracked("u", "n", 10_000), 52_000, -1),
            "tracked drop ages by the clock (42s)");
        Check.eq(7L, Attribution.ageSeconds(null, 52_000, 7), "untracked falls back to lived seconds");
        Check.eq(-1L, Attribution.ageSeconds(null, 52_000, -1), "no knowledge at all: age unknown (-1)");
        Check.eq(9L, Attribution.ageSeconds(tracked("u", "n", 60_000), 52_000, 9),
            "clock skew (drop after destroy) falls back rather than going negative");

        // The despawn valuable-filter.
        DeathRecord.ItemEntry plain = new DeathRecord.ItemEntry();
        plain.id = "minecraft:cobblestone";
        Check.ok(!Attribution.isValuable(plain), "plain cobblestone is not valuable (despawn skips it)");
        DeathRecord.ItemEntry enchanted = new DeathRecord.ItemEntry();
        enchanted.id = "minecraft:diamond_sword";
        enchanted.enchants.put("minecraft:sharpness", 4);
        Check.ok(Attribution.isValuable(enchanted), "an enchanted item is valuable");
        DeathRecord.ItemEntry named = new DeathRecord.ItemEntry();
        named.id = "minecraft:stick";
        named.customName = "The Beater";
        Check.ok(Attribution.isValuable(named), "a custom-named item is valuable");
        Check.ok(!Attribution.isValuable(null), "null item is not valuable");
    }
}
