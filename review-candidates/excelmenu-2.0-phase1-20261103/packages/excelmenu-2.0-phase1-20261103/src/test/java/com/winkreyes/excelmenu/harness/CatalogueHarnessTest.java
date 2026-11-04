package com.winkreyes.excelmenu.harness;

import com.winkreyes.excelmenu.core.platform.Platform;
import com.winkreyes.excelmenu.core.registry.EntryDef;
import com.winkreyes.excelmenu.core.registry.EntryRegistry;
import com.winkreyes.excelmenu.core.registry.EntryResolver;
import com.winkreyes.excelmenu.core.registry.RegistryConfig;
import com.winkreyes.excelmenu.core.registry.ResolvedEntry;
import com.winkreyes.excelmenu.core.registry.ViewerContext;
import org.junit.Test;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.fail;

/**
 * THE CATALOGUE GOLDEN GATE (DESIGN-2.0 §4 Phase 1 + census
 * Amendment B): old Java catalogue vs old Bedrock catalogue vs the
 * 2.0 registry rendering of commands_menu. Unlike the Server Menu,
 * the catalogue's two old columns ALREADY disagree — that drift is
 * the disease the registry cures — so the gate's shape is: every
 * shared row's dispatch intent and wording must match its old
 * column exactly, EXCEPT differences classified into the named
 * acceptance table below; every acceptance must actually occur.
 *
 *   C1 Presence, new on Java: the three Bedrock-only rows
 *      (/childproofmenu, /unchildproof, /whatsnew) render on Java
 *      now — parity by construction, the D3 class.
 *   C2 Presence, new on Bedrock: the six Java-only rows and the
 *      whole Fixes & Help group render on Bedrock now.
 *   C3 Description drift resolved to the Java text: where the two
 *      columns worded a row differently, the entry keeps the Java
 *      lore (the column carrying the Oct 23 buy-path amendments);
 *      the Bedrock form body line changes wording. Rows: /shop,
 *      /deathprotection status, /mobveil status, /lostfound,
 *      /trash, /mark. (For /shop the kept text is "Open the Spawn
 *      Shop"; the Bedrock "Open the Excel Shop" wording is a
 *      one-line data edit away if Mike prefers it — flagged in the
 *      Phase 1 report.)
 *   C4 Teleport dispatch convergence: the catalogue /ag row loses
 *      BedrockMenu's lock-dependent branch (confirm when unlocked,
 *      /seals when sealed) and dispatches the shared action — the
 *      guild teleport confirm menu, as Java always did. (/spawn's
 *      intents already matched: confirm-then-teleport both sides.)
 *   C5 Screen-row convergence: /ask and /mark lose BedrockMenu's
 *      bespoke screens (Ask text box, Maker's Mark form) FROM THE
 *      CATALOGUE and speak the entry's [message] hint — the D7/D6
 *      classes, same as the compass doors. Their guard nuance goes
 *      with them: the five formerly guarded rows (cosmetics,
 *      lostfound, trash, mark, ask) now dispatch the plain shared
 *      action, as the compass doors already did.
 *   C6 Hint buttons: Bedrock's eleven display-only rows (run=null)
 *      gain buttons whose tap speaks the Java type-it-in-chat hint,
 *      instead of being body text with no button.
 *   C7 Within-group order (Java): Shops & Trading's /cosmetics sits
 *      second in the union order (the Bedrock + compass order)
 *      instead of last-but-one as the old Java two-page arrangement
 *      had it. Every other group's common-row relative order matches
 *      BOTH old columns exactly (asserted, no acceptance).
 */
public class CatalogueHarnessTest {

    private static final Set<String> REQUIRED =
            Set.of("C1", "C2", "C3", "C4", "C5", "C6", "C7");

    private final Set<String> occurred = new HashSet<>();
    private final List<String> failures = new ArrayList<>();
    private int rowsCompared = 0;

    @Test
    public void catalogueThreeWay() throws Exception {
        Path fixtures = Path.of(System.getProperty("harness.fixtures", "fixtures"));
        Path entriesDir = Path.of(System.getProperty(
                "harness.entries", "src/main/resources/entries"));

        Map<String, CatalogueModels.JavaRow> javaRows =
                CatalogueModels.byDisplay(CatalogueModels.oldJava(fixtures));
        Map<String, CatalogueModels.BedrockRow> bedrockRows =
                CatalogueModels.bedrockByDisplay(CatalogueModels.oldBedrock(fixtures));

        EntryRegistry registry = new EntryRegistry();
        EntryRegistry.LoadOutcome outcome = registry.loadDirectory(entriesDir);
        if (!outcome.failed().isEmpty()) {
            fail("entries must all load: " + outcome.failed());
        }
        RegistryConfig config = new RegistryConfig(RegistryConfig.SERVER_ZONE,
                "&eThat isn't available right now.",
                Map.of("commands_menu", List.of("getting_around", "money_jobs",
                        "skills_levels", "shops_trading", "claims_land",
                        "guild_quests", "server_info", "fixes_help",
                        "your_items_help")),
                Map.of());
        ViewerContext viewer = new ViewerContext() {
            @Override public Platform platform() { return Platform.BEDROCK; }
            @Override public boolean hasPermission(String p) { return true; }
            @Override public boolean pluginPresent(String n) { return true; }
            @Override public String resolvePlaceholder(String t) { return null; }
        };
        List<ResolvedEntry> resolved = EntryResolver.resolve("commands_menu",
                outcome.loaded().values(), viewer,
                Clock.fixed(Instant.parse("2026-11-01T18:00:00Z"), ZoneOffset.UTC),
                config);
        Map<String, ResolvedEntry> newRows = new LinkedHashMap<>();
        for (ResolvedEntry r : resolved) {
            newRows.put(EntryResolver.stripLegacy(r.label()), r);
        }

        Set<String> displays = new HashSet<>();
        displays.addAll(javaRows.keySet());
        displays.addAll(bedrockRows.keySet());
        for (String display : displays) {
            compareRow(display, javaRows.get(display), bedrockRows.get(display),
                    newRows.get(display));
        }
        compareSequences(javaRows, bedrockRows, resolved);

        for (String c : REQUIRED) {
            if (!occurred.contains(c)) {
                failures.add("acceptance " + c + " never occurred — stale acceptance");
            }
        }
        System.out.println("CATALOGUE HARNESS: " + rowsCompared + " rows compared,"
                + " acceptances occurred: " + occurred);
        if (!failures.isEmpty()) {
            fail("catalogue gate FAILED (" + failures.size() + "):\n - "
                    + String.join("\n - ", failures));
        }
    }

    private void compareRow(String display, CatalogueModels.JavaRow j,
                            CatalogueModels.BedrockRow b, ResolvedEntry n) {
        if (n == null) {
            failures.add("row '" + display + "' missing from the 2.0 registry");
            return;
        }
        rowsCompared++;
        String newKey = CatalogueModels.keyOf(n.action());

        if (j == null) occurred.add("C1");           // Bedrock-only row
        if (b == null) occurred.add("C2");           // Java-only row

        if (j != null) {
            // Java column: wording + dispatch must match EXACTLY (the
            // conversion keeps the Java dialect verbatim; the
            // completeness suite proves it field-by-field).
            String javaKey = CatalogueModels.keyOf(j.action());
            if (!javaKey.equals(newKey)) {
                failures.add("row '" + display + "': dispatch vs Java differs"
                        + " (old " + javaKey + ", new " + newKey + ")");
            }
        }
        if (b != null) {
            String bedrockKey = CatalogueModels.bedrockKeyOf(b);
            if (!bedrockKey.equals(newKey)) {
                String c = classifyKey(display, b, newKey);
                if (c == null) {
                    failures.add("row '" + display + "': dispatch vs Bedrock differs"
                            + " (old " + bedrockKey + ", new " + newKey
                            + ") — unclassified");
                } else {
                    occurred.add(c);
                }
            }
            String newDesc = n.lore().isEmpty() ? ""
                    : EntryResolver.stripLegacy(n.lore().get(0));
            if (!newDesc.equals(b.description())) {
                occurred.add("C3");
                if (!Set.of("/shop", "/deathprotection status", "/mobveil status",
                        "/lostfound", "/trash", "/mark").contains(display)) {
                    failures.add("row '" + display + "': description drift not in"
                            + " the C3 row list (old '" + b.description()
                            + "', new '" + newDesc + "')");
                }
            }
        }
    }

    private String classifyKey(String display, CatalogueModels.BedrockRow b,
                               String newKey) {
        if (b.run() == null && newKey.equals("message")) return "C6";
        if (display.equals("/ag")) return "C4";
        if (display.equals("/ask") || display.equals("/mark")) return "C5";
        return null;
    }

    /** Per-group relative order of the rows two columns share:
     *  exact vs Bedrock (the union follows it), exact vs Java
     *  except C7. (A whole-list comparison would be meaningless:
     *  the old Java order interleaves two chest pages, so page-2
     *  rows sat after every page-1 row regardless of group.) */
    private void compareSequences(Map<String, CatalogueModels.JavaRow> javaRows,
                                  Map<String, CatalogueModels.BedrockRow> bedrockRows,
                                  List<ResolvedEntry> resolved) {
        Map<String, List<String>> newByGroup = new LinkedHashMap<>();
        for (ResolvedEntry r : resolved) {
            newByGroup.computeIfAbsent(r.entry().group(), k -> new ArrayList<>())
                    .add(EntryResolver.stripLegacy(r.label()));
        }
        Map<String, List<String>> javaByGroup = new LinkedHashMap<>();
        for (CatalogueModels.JavaRow r : javaRows.values()) {
            javaByGroup.computeIfAbsent(groupKey(r.group()), k -> new ArrayList<>())
                    .add(r.display());
        }
        Map<String, List<String>> bedrockByGroup = new LinkedHashMap<>();
        for (CatalogueModels.BedrockRow r : bedrockRows.values()) {
            bedrockByGroup.computeIfAbsent(groupKey(r.group()), k -> new ArrayList<>())
                    .add(r.display());
        }
        for (Map.Entry<String, List<String>> e : newByGroup.entrySet()) {
            String group = e.getKey();
            List<String> newSeq = e.getValue();
            List<String> bedrockSeq = bedrockByGroup.getOrDefault(group, List.of());
            List<String> newVsBedrock = newSeq.stream()
                    .filter(bedrockSeq::contains).toList();
            if (!newVsBedrock.equals(bedrockSeq)) {
                failures.add("group " + group + ": Bedrock relative order not"
                        + " preserved:\n   old: " + bedrockSeq
                        + "\n   new: " + newVsBedrock);
            }
            List<String> javaSeq = javaByGroup.getOrDefault(group, List.of());
            List<String> newVsJava = newSeq.stream().filter(javaSeq::contains).toList();
            if (!newVsJava.equals(javaSeq)) {
                // Only the C7 shape is acceptable: /cosmetics moved
                // to second in Shops & Trading; removing it from
                // both must make the sequences identical.
                List<String> a = newVsJava.stream()
                        .filter(d -> !d.equals("/cosmetics")).toList();
                List<String> c = javaSeq.stream()
                        .filter(d -> !d.equals("/cosmetics")).toList();
                if (a.equals(c) && group.equals("shops_trading")) {
                    occurred.add("C7");
                } else {
                    failures.add("group " + group + ": Java relative order differs"
                            + " beyond C7:\n   old: " + javaSeq
                            + "\n   new: " + newVsJava);
                }
            }
        }
    }

    private static String groupKey(String displayName) {
        return switch (displayName) {
            case "Getting Around" -> "getting_around";
            case "Money & Jobs" -> "money_jobs";
            case "Skills & Levels" -> "skills_levels";
            case "Shops & Trading" -> "shops_trading";
            case "Claims & Land" -> "claims_land";
            case "Guild & Quests" -> "guild_quests";
            case "Server Info" -> "server_info";
            case "Fixes & Help" -> "fixes_help";
            case "Your Items & Help" -> "your_items_help";
            default -> throw new IllegalStateException("unknown group " + displayName);
        };
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
