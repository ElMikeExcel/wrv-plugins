package com.winkreyes.excelmenu.harness;

import com.winkreyes.excelmenu.core.registry.EntryDef;
import com.winkreyes.excelmenu.core.registry.EntryRegistry;
import com.winkreyes.excelmenu.core.registry.EntryResolver;
import org.junit.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Conversion COMPLETENESS for the commands catalogue (Amendment B):
 * every row of both old columns is accounted for in the registry —
 * counted, not eyeballed. Old Java: 53 rows across two definition
 * pages. Old Bedrock: 50 rows in 8 groups. Union: 56 entries in 9
 * groups (6 rows were Java-only, 3 were Bedrock-only).
 *
 * Verbatim means verbatim — with exactly two NAMED exceptions, the
 * settled text fixes of INSTALL-PACKET §7 (Mike's decisions, Oct 31,
 * 2026; baked into the conversion at the Nov 1, 2026 re-base):
 * st_shop's lore carries "Open the Excel Shop" (Bedrock's wording,
 * adopted) and yi_lostfound's lore carries "last 14 days" (the live
 * Lost & Found rule). Each exception must actually fire — if the old
 * text stops matching, the exception fails loudly instead of going
 * stale (the harness's no-stale-acceptances rule).
 */
public class CatalogueConversionTest {

    /** Entry id -> (stale Java lore text, settled replacement). */
    private static final Map<String, Map.Entry<String, String>> SETTLED_LORE_FIXES =
            Map.of(
                    "st_shop", Map.entry("&7Open the Spawn Shop",
                            "&7Open the Excel Shop"),
                    "yi_lostfound", Map.entry(
                            "&7the void or a despawn in the last 7 days",
                            "&7the void or a despawn in the last 14 days"));

    @Test
    public void everyOldRowIsAnEntry() throws Exception {
        Path fixtures = Path.of(System.getProperty("harness.fixtures", "fixtures"));
        Path entriesDir = Path.of(System.getProperty(
                "harness.entries", "src/main/resources/entries"));

        List<CatalogueModels.JavaRow> javaRows = CatalogueModels.oldJava(fixtures);
        List<CatalogueModels.BedrockRow> bedrockRows =
                CatalogueModels.oldBedrock(fixtures);
        assertEquals("old Java catalogue rows", 53, javaRows.size());
        assertEquals("old Bedrock catalogue rows", 50, bedrockRows.size());

        EntryRegistry registry = new EntryRegistry();
        EntryRegistry.LoadOutcome outcome = registry.loadDirectory(entriesDir);
        assertTrue("entries must all load: " + outcome.failed(),
                outcome.failed().isEmpty());
        Map<String, EntryDef> catalogue = new HashMap<>();
        for (EntryDef e : outcome.loaded().values()) {
            if (e.menu().equals("commands_menu")) catalogue.put(e.id(), e);
        }
        assertEquals("union catalogue entries", 56, catalogue.size());

        Map<String, EntryDef> byDisplay = new HashMap<>();
        for (EntryDef e : catalogue.values()) {
            String display = EntryResolver.stripLegacy(e.label());
            assertTrue("duplicate display " + display,
                    byDisplay.put(display, e) == null);
        }
        for (CatalogueModels.JavaRow r : javaRows) {
            assertTrue("Java row missing from registry: " + r.display(),
                    byDisplay.containsKey(r.display()));
            EntryDef e = byDisplay.get(r.display());
            assertEquals("entry id must keep the Java item id (stats continuity): "
                    + r.display(), r.id(), e.id());
            assertEquals("label must be the Java name verbatim: " + r.display(),
                    r.label(), e.label());
            List<String> expectedLore = r.lore();
            Map.Entry<String, String> fix = SETTLED_LORE_FIXES.get(r.id());
            if (fix != null) {
                assertTrue("settled lore fix must fire for " + r.id()
                                + " (stale text not in the old lore — the"
                                + " exception is stale, remove or update it)",
                        expectedLore.stream().anyMatch(l -> l.contains(fix.getKey())));
                expectedLore = expectedLore.stream()
                        .map(l -> l.replace(fix.getKey(), fix.getValue())).toList();
            }
            assertEquals("lore must be the Java lore verbatim"
                            + (fix != null ? " + the settled text fix" : "")
                            + ": " + r.display(),
                    expectedLore, e.lore());
            assertEquals("action must be the Java click lines verbatim: "
                    + r.display(), r.action(), e.action());
        }
        for (CatalogueModels.BedrockRow r : bedrockRows) {
            assertTrue("Bedrock row missing from registry: " + r.display(),
                    byDisplay.containsKey(r.display()));
        }

        Map<String, Integer> perGroup = new HashMap<>();
        for (EntryDef e : catalogue.values()) {
            perGroup.merge(e.group(), 1, Integer::sum);
            // The group trailer lore line names the group, Java-style.
            String trailer = e.lore().get(e.lore().size() - 1);
            assertTrue("trailer lore for " + e.id() + ": " + trailer,
                    trailer.startsWith("&8"));
        }
        assertEquals(Map.of(
                "getting_around", 6, "money_jobs", 6, "skills_levels", 5,
                "shops_trading", 7, "claims_land", 9, "guild_quests", 4,
                "server_info", 11, "fixes_help", 4, "your_items_help", 4),
                perGroup);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
