import com.winkreyes.exceltrainingwheels.core.ProtectedRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProtectedRegistryTest {
    static final UUID P1 = UUID.fromString("00000000-0000-0000-0009-000000000001");
    static final UUID P2 = UUID.fromString("00000000-0000-0000-0009-000000000002");

    public static void run() {
        System.out.println("ProtectedRegistryTest");

        Map<String, String[]> raw = new HashMap<>();
        raw.put(P1.toString(), new String[]{"0.5", ".ExamplePlayer"});
        raw.put(P2.toString(), new String[]{"0.75", ".ExamplePlayer2"});
        ProtectedRegistry.ParseResult parsed = ProtectedRegistry.fromRaw(raw);
        Check.eq(0, parsed.errors().size(), "clean parse has no errors");
        Check.eq(2, parsed.registry().size(), "clean parse loads both entries");
        Check.ok(parsed.registry().isProtected(P1), "P1 is protected");
        Check.close(0.5, parsed.registry().factor(P1).orElse(-1), "P1 factor 0.5");
        Check.close(0.75, parsed.registry().factor(P2).orElse(-1), "P2 factor 0.75");
        Check.eq(".ExamplePlayer", parsed.registry().entry(P1).orElseThrow().name(), "name survives parse");
        Check.ok(!parsed.registry().isProtected(UUID.randomUUID()), "stranger is not protected");
        Check.ok(parsed.registry().factor(UUID.randomUUID()).isEmpty(), "stranger has no factor");

        // Bad rows are skipped one by one, good rows still load.
        Map<String, String[]> messy = new HashMap<>();
        messy.put("not-a-uuid", new String[]{"0.5", "Ghost"});
        messy.put(P1.toString(), new String[]{"banana", ".ExamplePlayer"});
        messy.put(P2.toString(), new String[]{"1.5", ".ExamplePlayer2"});
        messy.put("00000000-0000-0000-0009-0000000000ab", new String[]{"-0.25", ".AlphaPlayer"});
        ProtectedRegistry.ParseResult messyParsed = ProtectedRegistry.fromRaw(messy);
        Check.eq(4, messyParsed.errors().size(), "each bad row reported (uuid, nan-factor, >1, <0)");
        Check.eq(0, messyParsed.registry().size(), "no bad row loaded");

        // Boundaries are valid factors.
        Map<String, String[]> edges = new HashMap<>();
        edges.put(P1.toString(), new String[]{"0.0", "Zero"});
        edges.put(P2.toString(), new String[]{"1.0", "One"});
        ProtectedRegistry.ParseResult edgeParsed = ProtectedRegistry.fromRaw(edges);
        Check.eq(0, edgeParsed.errors().size(), "boundary factors 0.0 and 1.0 parse clean");
        Check.close(0.0, edgeParsed.registry().factor(P1).orElse(-1), "factor 0.0 kept");
        Check.close(1.0, edgeParsed.registry().factor(P2).orElse(-1), "factor 1.0 kept (listed = protected for door guard)");

        // Missing name is fine; missing factor is an error row.
        Map<String, String[]> sparse = new HashMap<>();
        sparse.put(P1.toString(), new String[]{"0.5", null});
        ProtectedRegistry.ParseResult sparseParsed = ProtectedRegistry.fromRaw(sparse);
        Check.eq(0, sparseParsed.errors().size(), "null name parses");
        Check.ok(sparseParsed.registry().entry(P1).orElseThrow().name() == null, "null name kept as null");

        // Mutations + round-trip persistence.
        ProtectedRegistry reg = new ProtectedRegistry();
        reg.put(P1, 0.5, ".ExamplePlayer");
        Check.ok(reg.isProtected(P1), "put protects");
        reg.put(P1, 0.75, ".ExamplePlayer");
        Check.close(0.75, reg.factor(P1).orElse(-1), "put overwrites factor (graduation step)");
        Check.eq(1, reg.size(), "overwrite does not duplicate");
        Map<String, String[]> out = reg.toRaw();
        ProtectedRegistry.ParseResult roundTrip = ProtectedRegistry.fromRaw(out);
        Check.eq(0, roundTrip.errors().size(), "round-trip parses clean");
        Check.close(0.75, roundTrip.registry().factor(P1).orElse(-1), "round-trip preserves factor");
        Check.eq(".ExamplePlayer", roundTrip.registry().entry(P1).orElseThrow().name(), "round-trip preserves name");
        Check.ok(reg.remove(P1), "remove works");
        Check.ok(!reg.remove(P1), "second remove is a no-op false");
        Check.ok(!reg.isProtected(P1), "removed player unprotected");

        boolean threw = false;
        try { reg.put(P1, 2.0, "x"); } catch (IllegalArgumentException e) { threw = true; }
        Check.ok(threw, "put rejects factor > 1");
        Check.ok(ProtectedRegistry.validFactor(0.0) && ProtectedRegistry.validFactor(1.0)
                && !ProtectedRegistry.validFactor(Double.POSITIVE_INFINITY)
                && !ProtectedRegistry.validFactor(Double.NaN), "validFactor boundaries");
    }
}
