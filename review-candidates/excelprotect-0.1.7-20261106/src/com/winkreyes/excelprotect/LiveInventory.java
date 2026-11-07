package com.winkreyes.excelprotect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ExcelProtect 0.1.5 "Clarity" — the LIVE inventory read for
 * `inspect player` (SPEC §5): the online player's current inventory +
 * ender chest, aggregated by item signature in the snapshot
 * fingerprint style. Pure aggregation over ItemFacts — the command
 * layer does the Bukkit read on the main thread and this class does
 * the counting, so the whole shape is unit-testable. Fingerprints
 * only: no payloads are read, ever — same rule as every inspect form.
 * Excel — Wink Reyes Valley
 */
public final class LiveInventory {
    private LiveInventory() {}

    /** Aggregated view: stack/item totals per holder + one line per distinct signature. */
    public record Summary(int invStacks, int invItems, int enderStacks, int enderItems, List<String> lines) {}

    public static Summary summarize(List<ItemFact> inventory, List<ItemFact> enderChest) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, String> labels = new LinkedHashMap<>();
        int invItems = 0, enderItems = 0;
        for (ItemFact f : inventory) {
            invItems += f.count();
            Chains.Sig sig = Chains.of(f);
            counts.merge(sig.key(), f.count(), Integer::sum);
            labels.putIfAbsent(sig.key(), sig.label());
        }
        for (ItemFact f : enderChest) {
            enderItems += f.count();
            Chains.Sig sig = Chains.of(f);
            counts.merge(sig.key(), f.count(), Integer::sum);
            labels.putIfAbsent(sig.key(), sig.label());
        }
        List<String> keys = new ArrayList<>(counts.keySet());
        keys.sort((a, b) -> labels.get(a).compareTo(labels.get(b)));
        List<String> lines = new ArrayList<>();
        for (String k : keys) lines.add(labels.get(k) + " x" + counts.get(k));
        return new Summary(inventory.size(), invItems, enderChest.size(), enderItems, lines);
    }
}
