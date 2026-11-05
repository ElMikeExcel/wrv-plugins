package com.winkreyes.excellandmarks;

import com.winkreyes.excellandmarks.core.Attribution;
import com.winkreyes.excellandmarks.core.Spot;
import com.winkreyes.excellandmarks.hook.GpAdapter;
import com.winkreyes.excellandmarks.hook.ProtectAttribution;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * ExcelProtect attribution: the blocks-*.jsonl parser (field order,
 * malformed lines), latest-per-position resolution, the renter-only
 * region tally, the file-level ProtectAttribution read, and the GP
 * adapter's fail-soft contract with GriefPrevention absent.
 */
public final class AttributionTest {

    static final UUID RENTER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID OTHER = UUID.fromString("22222222-2222-2222-2222-222222222222");

    public static void main(String[] args) throws Exception {
        Check ck = new Check("AttributionTest");
        parser(ck);
        latestResolution(ck);
        tallyRules(ck);
        fileLevelRead(ck);
        gpFailSoft(ck);
        ck.finish();
    }

    static String line(String action, String block, UUID who, int x, int y, int z, long ts) {
        return "{\"ts\":\"2026-11-05T10:00:00Z\",\"ts_ms\":" + ts + ",\"type\":\"block_change\",\"action\":\"" + action
                + "\",\"world\":\"world\",\"x\":" + x + ",\"y\":" + y + ",\"z\":" + z
                + ",\"block\":\"" + block + "\",\"player\":\"P\",\"player_uuid\":\"" + who + "\"}";
    }

    static void parser(Check ck) {
        Attribution.Entry e = Attribution.parseLine(line("place", "minecraft:glowstone", RENTER, 1, 64, -3, 12345));
        ck.that(e != null, "well-formed line parses");
        ck.eq(1, e.x(), "x parsed");
        ck.eq(-3, e.z(), "negative z parsed");
        ck.eq(12345, e.tsMs(), "ts_ms parsed");
        ck.eqStr("minecraft:glowstone", e.block(), "block parsed");
        ck.that(e.player().equals(RENTER), "player uuid parsed");
        // Field order must not matter.
        String shuffled = "{\"player_uuid\":\"" + RENTER + "\",\"block\":\"minecraft:stone\",\"z\":5,\"y\":6,\"x\":7,"
                + "\"world\":\"world\",\"action\":\"break\",\"ts_ms\":99}";
        Attribution.Entry s = Attribution.parseLine(shuffled);
        ck.that(s != null && s.x() == 7 && s.action().equals("break") && s.tsMs() == 99, "field order independent");
        // Malformed / irrelevant lines are dropped, never thrown.
        ck.that(Attribution.parseLine("not json at all") == null, "garbage -> null");
        ck.that(Attribution.parseLine("") == null, "empty -> null");
        ck.that(Attribution.parseLine("{\"type\":\"chat\",\"x\":1}") == null, "missing fields -> null");
        ck.that(Attribution.parseLine(line("place", "minecraft:stone", RENTER, 1, 2, 3, 1).replace("\"x\":1", "\"x\":\"a\"")) == null,
                "non-numeric coord -> null");
    }

    static Spot plot() {
        Spot s = new Spot("lm-9", "world", 100, 64, 100, "P", Spot.Type.PLOT, Spot.Tier.LANDMARK,
                150_000, 2_500_000, 5, 0L);
        s.ownerId = RENTER;
        s.ownerName = "R";
        s.mode = Spot.Mode.RENTED;
        return s;
    }

    static Map<String, Attribution.Entry> resolve(String... lines) {
        return Attribution.latestPerPosition(List.of(lines).stream().map(Attribution::parseLine).toList());
    }

    static void latestResolution(Check ck) {
        // Placed then broken: the break is the latest word -> nothing attributable.
        Map<String, Attribution.Entry> m = resolve(
                line("place", "minecraft:stone", RENTER, 100, 64, 100, 1000),
                line("break", "minecraft:air", RENTER, 100, 64, 100, 2000));
        ck.eqStr("break", m.get("world@100,64,100").action(), "later break wins over earlier place");
        // Broken then re-placed (by someone else): the place stands, attributed to them.
        m = resolve(
                line("break", "minecraft:air", RENTER, 100, 64, 100, 1000),
                line("place", "minecraft:dirt", OTHER, 100, 64, 100, 2000));
        ck.that(m.get("world@100,64,100").player().equals(OTHER), "later place by other player wins");
        // Out-of-order file lines: the newer timestamp still wins.
        m = resolve(
                line("place", "minecraft:stone", RENTER, 100, 64, 100, 3000),
                line("break", "minecraft:air", RENTER, 100, 64, 100, 2000));
        ck.eqStr("place", m.get("world@100,64,100").action(), "newer ts wins even when listed first");
    }

    static void tallyRules(Check ck) {
        Spot s = plot();
        Map<String, Attribution.Entry> latest = new TreeMap<>();
        // Renter placed 3 glowstone + 2 stone inside; 1 stone outside the region.
        latest.putAll(resolve(
                line("place", "minecraft:glowstone", RENTER, 100, 64, 100, 1),
                line("place", "minecraft:glowstone", RENTER, 101, 64, 100, 2),
                line("place", "minecraft:glowstone", RENTER, 102, 65, 100, 3),
                line("place", "minecraft:stone", RENTER, 100, 61, 100, 4),
                line("place", "minecraft:stone", RENTER, 100, 76, 100, 5),
                line("place", "minecraft:stone", RENTER, 120, 64, 100, 6)));
        // Another player's block inside the region is NOT the renter's.
        latest.putAll(resolve(line("place", "minecraft:diamond_block", OTHER, 100, 64, 101, 7)));
        // A renter place that was later broken is excluded.
        latest.putAll(resolve(
                line("place", "minecraft:glass", RENTER, 103, 64, 100, 8),
                line("break", "minecraft:air", RENTER, 103, 64, 100, 9)));
        // An air "place" is not a material.
        latest.putAll(resolve(line("place", "minecraft:air", RENTER, 104, 64, 100, 10)));

        Attribution.Tally t = Attribution.tally(latest, s, RENTER, 3, 12);
        ck.eq(3, t.materials.getOrDefault("minecraft:glowstone", 0), "glowstone tallied");
        // stone at y=61 (in range: 64-3) and y=76 (64+12) count; y=120-x one is out of region
        ck.eq(2, t.materials.getOrDefault("minecraft:stone", 0), "stone tallied (region edges included)");
        ck.that(!t.materials.containsKey("minecraft:diamond_block"), "other player's block excluded");
        ck.that(!t.materials.containsKey("minecraft:glass"), "broken-after-place excluded");
        ck.eq(5, t.totalBlocks(), "total attributed blocks");
        ck.eq(5, t.positions.size(), "positions to clear == tallied blocks");
    }

    static void fileLevelRead(Check ck) throws Exception {
        Path tmp = Files.createTempDirectory("lm-attr");
        Path plugins = tmp.resolve("plugins");
        Path protectDir = tmp.resolve("ExcelProtect");
        Files.createDirectories(plugins);
        Files.createDirectories(protectDir);
        String body = String.join("\n",
                line("place", "minecraft:glowstone", RENTER, 100, 64, 100, 1000),
                line("place", "minecraft:glowstone", RENTER, 100, 64, 100, 1000), // duplicate identical
                line("break", "minecraft:air", RENTER, 101, 64, 100, 500),
                line("place", "minecraft:oak_planks", RENTER, 101, 64, 100, 1500),
                "garbage line that must be skipped") + "\n";
        Files.writeString(protectDir.resolve("blocks-2026-11-05.jsonl"), body);

        ProtectAttribution pa = new ProtectAttribution(plugins.toFile(), "../ExcelProtect", Logger.getLogger("t"));
        ck.that(pa.available(), "protect dir present -> available");
        Attribution.Tally t = pa.tallyFor(plot(), RENTER, 3, 12);
        ck.eq(1, t.materials.getOrDefault("minecraft:glowstone", 0), "file read: glowstone once (dup collapsed)");
        ck.eq(1, t.materials.getOrDefault("minecraft:oak_planks", 0), "file read: re-place after break counts");
        ck.eq(2, t.totalBlocks(), "file read: total");

        ProtectAttribution missing = new ProtectAttribution(plugins.toFile(), "../NoSuchPlugin", Logger.getLogger("t"));
        ck.that(!missing.available(), "missing dir -> unavailable");
        ck.eq(0, missing.tallyFor(plot(), RENTER, 3, 12).totalBlocks(), "missing dir -> empty tally, no throw");
    }

    static void gpFailSoft(Check ck) {
        GpAdapter none = GpAdapter.NONE;
        ck.that(!none.available(), "absent GP: adapter reports unavailable");
        ck.that(none.createSubdivision("world", 0, 0, 0, 1, 1, 1, RENTER) == null,
                "absent GP: create returns null (naming-only), never throws");
        ck.that(none.deleteSubdivision(42L), "absent GP: delete is a satisfied no-op");
    }
}
