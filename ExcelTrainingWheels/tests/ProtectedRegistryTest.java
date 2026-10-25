import com.winkreyes.exceltrainingwheels.core.ProtectedRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProtectedRegistryTest {
    static final UUID KID = UUID.fromString("00000000-0000-0000-0009-000000000001");
    static final UUID KID2 = UUID.fromString("00000000-0000-0000-0009-000000000002");

    public static void run() {
        System.out.println("ProtectedRegistryTest");

        Map<String, String[]> raw = new HashMap<>();
        raw.put(KID.toString(), new String[]{"0.5", ".ExampleKid"});
        raw.put(KID2.toString(), new String[]{"0.75", ".ExampleKid2"});
        ProtectedRegistry.ParseResult parsed = ProtectedRegistry.fromRaw(raw);
        Check.eq(0, parsed.errors().size(), "clean parse has no errors");
        Check.eq(2, parsed.registry().size(), "clean parse loads both entries");
        Check.ok(parsed.registry().isProtected(KID), "Kid is protected");
        Check.close(0.5, parsed.registry().factor(KID).orElse(-1), "Kid factor 0.5");
        Check.close(0.75, parsed.registry().factor(KID2).orElse(-1), "Kid2 factor 0.75");
        Check.eq(".ExampleKid", parsed.registry().entry(KID).orElseThrow().name(), "name survives parse");
        Check.ok(!parsed.registry().isProtected(UUID.randomUUID()), "stranger is not protected");
        Check.ok(parsed.registry().factor(UUID.randomUUID()).isEmpty(), "stranger has no factor");

        // Bad rows are skipped one by one, good rows still load.
        Map<String, String[]> messy = new HashMap<>();
        messy.put("not-a-uuid", new String[]{"0.5", "Ghost"});
        messy.put(KID.toString(), new String[]{"banana", ".ExampleKid"});
        messy.put(KID2.toString(), new String[]{"1.5", ".ExampleKid2"});
        messy.put("00000000-0000-0000-0009-0000000000ab", new String[]{"-0.25", ".AlphaPlayer"});
        ProtectedRegistry.ParseResult messyParsed = ProtectedRegistry.fromRaw(messy);
        Check.eq(4, messyParsed.errors().size(), "each bad row reported (uuid, nan-factor, >1, <0)");
        Check.eq(0, messyParsed.registry().size(), "no bad row loaded");

        // Boundaries are valid factors.
        Map<String, String[]> edges = new HashMap<>();
        edges.put(KID.toString(), new String[]{"0.0", "Zero"});
        edges.put(KID2.toString(), new String[]{"1.0", "One"});
        ProtectedRegistry.ParseResult edgeParsed = ProtectedRegistry.fromRaw(edges);
        Check.eq(0, edgeParsed.errors().size(), "boundary factors 0.0 and 1.0 parse clean");
        Check.close(0.0, edgeParsed.registry().factor(KID).orElse(-1), "factor 0.0 kept");
        Check.close(1.0, edgeParsed.registry().factor(KID2).orElse(-1), "factor 1.0 kept (listed = protected for door guard)");

        // Missing name is fine; missing factor is an error row.
        Map<String, String[]> sparse = new HashMap<>();
        sparse.put(KID.toString(), new String[]{"0.5", null});
        ProtectedRegistry.ParseResult sparseParsed = ProtectedRegistry.fromRaw(sparse);
        Check.eq(0, sparseParsed.errors().size(), "null name parses");
        Check.ok(sparseParsed.registry().entry(KID).orElseThrow().name() == null, "null name kept as null");

        // Mutations + round-trip persistence.
        ProtectedRegistry reg = new ProtectedRegistry();
        reg.put(KID, 0.5, ".ExampleKid");
        Check.ok(reg.isProtected(KID), "put protects");
        reg.put(KID, 0.75, ".ExampleKid");
        Check.close(0.75, reg.factor(KID).orElse(-1), "put overwrites factor (graduation step)");
        Check.eq(1, reg.size(), "overwrite does not duplicate");
        Map<String, String[]> out = reg.toRaw();
        ProtectedRegistry.ParseResult roundTrip = ProtectedRegistry.fromRaw(out);
        Check.eq(0, roundTrip.errors().size(), "round-trip parses clean");
        Check.close(0.75, roundTrip.registry().factor(KID).orElse(-1), "round-trip preserves factor");
        Check.eq(".ExampleKid", roundTrip.registry().entry(KID).orElseThrow().name(), "round-trip preserves name");
        Check.ok(reg.remove(KID), "remove works");
        Check.ok(!reg.remove(KID), "second remove is a no-op false");
        Check.ok(!reg.isProtected(KID), "removed player unprotected");

        boolean threw = false;
        try { reg.put(KID, 2.0, "x"); } catch (IllegalArgumentException e) { threw = true; }
        Check.ok(threw, "put rejects factor > 1");
        Check.ok(ProtectedRegistry.validFactor(0.0) && ProtectedRegistry.validFactor(1.0)
                && !ProtectedRegistry.validFactor(Double.POSITIVE_INFINITY)
                && !ProtectedRegistry.validFactor(Double.NaN), "validFactor boundaries");
    }
}
