package com.winkreyes.excelautosort;

import com.winkreyes.excelautosort.core.ActivesParse;
import com.winkreyes.excelautosort.core.BufferCore;
import com.winkreyes.excelautosort.core.ChestKeys;
import com.winkreyes.excelautosort.core.FilterCore;
import com.winkreyes.excelautosort.core.Mode;
import com.winkreyes.excelautosort.core.ProtectEvents;
import com.winkreyes.excelautosort.core.SelectorModel;
import com.winkreyes.excelautosort.core.SortCore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ExcelAutoSort unit suite — the pure core (SPEC §11): the sort parity
 * ordering (upstream SortTest cases ported as data), filter matching
 * and reject itemization, buffer expiry/cap math, key handling, the
 * active.txt import tolerance, the Protect event field tables, and
 * the shared Java/Bedrock selector model.
 */
public class AutoSortTest {

    // Synthetic registry indices mirroring Registry.MATERIAL's relative
    // order for the four materials the upstream test uses ("diamond and
    // stick come before cobblestone and dirt" — upstream SortTest).
    private static SortCore.StackView v(String key, int idx, int amount, int max) {
        return new SortCore.StackView(idx, key, "{}", amount, max);
    }

    // ---------------------------------------------------------- SortCore

    @Test
    void upstreamPlacedItemsSortAndMerge() {
        // Upstream SortTest PLACED -> SORTED, ported verbatim as data.
        List<SortCore.StackView> in = List.of(
                v("dirt", 40, 10, 64),
                v("cobblestone", 30, 5, 64),
                v("stick", 20, 4, 64),
                v("diamond", 10, 2, 64),
                v("dirt", 40, 20, 64));
        List<SortCore.StackView> out = SortCore.sortAndMerge(in);
        assertEquals(List.of(
                v("diamond", 10, 2, 64),
                v("stick", 20, 4, 64),
                v("cobblestone", 30, 5, 64),
                v("dirt", 40, 30, 64)), out);
    }

    @Test
    void upstreamSwordsOrderByMetaText() {
        // Upstream SortTest SWORDS -> SWORDS_SORTED: meta compared as text,
        // "{}" (plain) sorts after any {"..."} because '}' > '"'.
        SortCore.StackView d3 = new SortCore.StackView(15, "iron_sword", "{\"minecraft:damage\":3}", 1, 1);
        SortCore.StackView d2 = new SortCore.StackView(15, "iron_sword", "{\"minecraft:damage\":2}", 1, 1);
        SortCore.StackView d0 = new SortCore.StackView(15, "iron_sword", "{}", 1, 1);
        SortCore.StackView d1 = new SortCore.StackView(15, "iron_sword", "{\"minecraft:damage\":1}", 1, 1);
        assertEquals(List.of(d1, d2, d3, d0), SortCore.sortAndMerge(List.of(d3, d2, d0, d1)));
    }

    @Test
    void mergeSpillsIntoMaxSizeChunks() {
        List<SortCore.StackView> out = SortCore.sortAndMerge(List.of(
                v("dirt", 40, 40, 64), v("dirt", 40, 40, 64)));
        assertEquals(List.of(v("dirt", 40, 64, 64), v("dirt", 40, 16, 64)), out);
    }

    @Test
    void differentMetaNeverMerges() {
        SortCore.StackView plain = new SortCore.StackView(40, "dirt", "{}", 10, 64);
        SortCore.StackView named = new SortCore.StackView(40, "dirt", "{\"minecraft:custom_name\":\"x\"}", 10, 64);
        assertEquals(2, SortCore.sortAndMerge(List.of(plain, named)).size());
    }

    @Test
    void sortDoesNotMutateInputAndDropsEmpties() {
        List<SortCore.StackView> in = new java.util.ArrayList<>(List.of(
                v("dirt", 40, 0, 64), v("diamond", 10, 1, 64)));
        List<SortCore.StackView> out = SortCore.sortAndMerge(in);
        assertEquals(2, in.size());
        assertEquals(List.of(v("diamond", 10, 1, 64)), out);
        assertEquals(List.of(), SortCore.sortAndMerge(List.of()));
    }

    // ---------------------------------------------------------- FilterCore

    @Test
    void whitelistBlacklistOffMatching() {
        Set<String> list = Set.of("COBBLESTONE");
        assertTrue(FilterCore.isReject(Mode.WHITELIST, list, "DIRT"));
        assertFalse(FilterCore.isReject(Mode.WHITELIST, list, "COBBLESTONE"));
        assertTrue(FilterCore.isReject(Mode.BLACKLIST, list, "COBBLESTONE"));
        assertFalse(FilterCore.isReject(Mode.BLACKLIST, list, "DIRT"));
        assertFalse(FilterCore.isReject(Mode.OFF, list, "COBBLESTONE"));
        assertFalse(FilterCore.isReject(Mode.OFF, list, "DIRT"));
    }

    @Test
    void rejectsAggregatePerMaterialInFirstSeenOrder() {
        List<FilterCore.ContentStack> contents = List.of(
                new FilterCore.ContentStack("COBBLESTONE", 10),
                new FilterCore.ContentStack("DIAMOND", 1),
                new FilterCore.ContentStack("COBBLESTONE", 5),
                new FilterCore.ContentStack("DIRT", 3));
        List<FilterCore.RejectLine> lines = FilterCore.rejects(
                Mode.BLACKLIST, Set.of("COBBLESTONE", "DIRT"), contents);
        assertEquals(List.of(
                new FilterCore.RejectLine("COBBLESTONE", 15, 2),
                new FilterCore.RejectLine("DIRT", 3, 1)), lines);
        assertEquals(3, FilterCore.totalStacks(lines));
        assertEquals(18, FilterCore.totalItems(lines));
    }

    // ---------------------------------------------------------- BufferCore

    @Test
    void purgeTakesExpiredFirstEarliestExpiryFirst() {
        List<BufferCore.TimedEntry> in = List.of(
                new BufferCore.TimedEntry("a", 100, 500),
                new BufferCore.TimedEntry("b", 200, 300),
                new BufferCore.TimedEntry("c", 300, 10_000));
        List<BufferCore.Purge> plan = BufferCore.purgePlan(in, 1_000, 162);
        assertEquals(List.of(
                new BufferCore.Purge("b", BufferCore.PurgeReason.EXPIRED),
                new BufferCore.Purge("a", BufferCore.PurgeReason.EXPIRED)), plan);
    }

    @Test
    void purgeOverflowTakesOldestAndExpiredDoNotCountTowardCap() {
        List<BufferCore.TimedEntry> in = List.of(
                new BufferCore.TimedEntry("dead", 50, 10),
                new BufferCore.TimedEntry("old", 100, 99_000),
                new BufferCore.TimedEntry("mid", 200, 99_000),
                new BufferCore.TimedEntry("new", 300, 99_000),
                new BufferCore.TimedEntry("newest", 400, 99_000));
        List<BufferCore.Purge> plan = BufferCore.purgePlan(in, 1_000, 3);
        assertEquals(List.of(
                new BufferCore.Purge("dead", BufferCore.PurgeReason.EXPIRED),
                new BufferCore.Purge("old", BufferCore.PurgeReason.OVERFLOW)), plan);
    }

    @Test
    void displayOrderIsNewestFirstAndTimeLeftReadsPlainly() {
        List<BufferCore.TimedEntry> in = List.of(
                new BufferCore.TimedEntry("a", 100, 0),
                new BufferCore.TimedEntry("b", 300, 0),
                new BufferCore.TimedEntry("c", 200, 0));
        assertEquals(List.of("b", "c", "a"),
                BufferCore.displayOrder(in).stream().map(BufferCore.TimedEntry::id).toList());
        assertEquals("7 days", BufferCore.timeLeft(7 * 86_400_000L, 0));
        assertEquals("1 day", BufferCore.timeLeft(86_400_000L, 0));
        assertEquals("1 hour", BufferCore.timeLeft(3_600_000L, 0));
        assertEquals("30 minutes", BufferCore.timeLeft(30 * 60_000L, 0));
        assertEquals("under a minute", BufferCore.timeLeft(30_000L, 0));
        assertEquals("under a minute", BufferCore.timeLeft(0L, 5_000L));
    }

    // ---------------------------------------------------------- ChestKeys

    @Test
    void keysRoundTripAndRejectMalformed() {
        assertEquals("world:1:64:-30", ChestKeys.key("world", 1, 64, -30));
        ChestKeys.BlockKey bk = ChestKeys.parse("world:1:64:-30");
        assertEquals("world", bk.world());
        assertEquals(-30, bk.z());
        assertEquals("world:1:64:-30", bk.format());
        assertNull(ChestKeys.parse("world:1:64"));
        assertNull(ChestKeys.parse("world:a:b:c"));
        assertNull(ChestKeys.parse(null));
        assertEquals(List.of("world:0:0:0", "world:1:0:0"),
                ChestKeys.canonical(List.of("world:1:0:0", "world:0:0:0")));
    }

    // ---------------------------------------------------------- ActivesParse

    @Test
    void activesParseToleratesBlanksAndBadLines() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        Set<UUID> out = ActivesParse.parse(List.of(
                "", "  ", a.toString(), "not-a-uuid", " " + b + " ", a.toString()));
        assertEquals(Set.of(a, b), out);
        assertEquals(Set.of(), ActivesParse.parse(null));
    }

    // ---------------------------------------------------------- ProtectEvents

    @Test
    void filterDeleteCarriesTheFullSpecFieldTable() {
        Map<String, Object> m = ProtectEvents.filterDelete(
                "world", 1, 64, 2, "world:1:64:2",
                "uuid-actor", "Mike", "player",
                "uuid-owner", "Kay", "whitelist",
                "minecraft:netherite_sword", "B64DATA", 1,
                "buf-1", "uuid-actor", 123_456L);
        assertEquals(Set.of("world", "x", "y", "z", "chest_key",
                "actor", "actor_name", "actor_kind",
                "chest_owner", "chest_owner_name", "mode",
                "item", "item_data", "count",
                "buffer_id", "buffer_owner", "expires_ms"), m.keySet());
        assertEquals("minecraft:netherite_sword", m.get("item"));
        assertEquals("player", m.get("actor_kind"));
        assertEquals(123_456L, m.get("expires_ms"));
        assertEquals("items", ProtectEvents.STREAM);
    }

    @Test
    void automationDeleteHasNullActor() {
        Map<String, Object> m = ProtectEvents.filterDelete(
                "world", 1, 64, 2, "world:1:64:2",
                null, null, "hopper",
                "uuid-owner", "Kay", "blacklist",
                "minecraft:rotten_flesh", "B64", 4,
                "buf-2", "uuid-owner", 999L);
        assertNull(m.get("actor"));
        assertEquals("hopper", m.get("actor_kind"));
        assertEquals("uuid-owner", m.get("buffer_owner"));
    }

    @Test
    void otherEventShapesCarryTheirFields() {
        Map<String, Object> arm = ProtectEvents.filterArm("world", 0, 0, 0, "world:0:0:0",
                "u", "Mike", "u", "whitelist", 6, 2, 74, false);
        assertEquals(6, arm.get("list_size"));
        assertEquals(2, arm.get("swept_stacks"));
        assertEquals(74, arm.get("swept_items"));
        assertEquals("block-break", ProtectEvents.unmark("world", 0, 0, 0, "k", null, "o", "block-break").get("cause"));
        assertEquals("expired", ProtectEvents.bufferPurge("o", "b", "minecraft:dirt", 3, "expired", "k").get("reason"));
        assertEquals("b", ProtectEvents.recoverClaim("u", "Mike", "o", "b", "minecraft:dirt", 3, "k").get("buffer_id"));
        assertEquals("claim-deleted", ProtectEvents.suspend("world", 0, 0, 0, "k", "o", "claim-deleted").get("reason"));
        assertEquals("half-broken", ProtectEvents.chestReshaped("world", 0, 0, 0, "k", "o", "half-broken", 1).get("detail"));
    }

    // ---------------------------------------------------------- SelectorModel

    @Test
    void entriesAreContentsSeededThenAbsentListEntries() {
        List<FilterCore.ContentStack> contents = List.of(
                new FilterCore.ContentStack("IRON_INGOT", 20),
                new FilterCore.ContentStack("COBBLESTONE", 64),
                new FilterCore.ContentStack("IRON_INGOT", 14));
        Set<String> list = new java.util.LinkedHashSet<>(List.of("COBBLESTONE", "GOLD_INGOT"));
        List<SelectorModel.Entry> entries = SelectorModel.entries(contents, list);
        assertEquals(List.of(
                new SelectorModel.Entry("IRON_INGOT", false, 34),
                new SelectorModel.Entry("COBBLESTONE", true, 64),
                new SelectorModel.Entry("GOLD_INGOT", true, 0)), entries);
    }

    @Test
    void pagingSlicesAtPageSize() {
        List<SelectorModel.Entry> all = new java.util.ArrayList<>();
        for (int i = 0; i < 30; i++) all.add(new SelectorModel.Entry("M" + i, false, 1));
        assertEquals(28, SelectorModel.page(all, 0, 28).size());
        assertEquals(2, SelectorModel.page(all, 1, 28).size());
        assertEquals(2, SelectorModel.pageCount(30, 28));
        assertEquals(1, SelectorModel.pageCount(0, 28));
    }

    @Test
    void bedrockModelMatchesJavaModel() {
        // §7.5 parity: the Bedrock form is built from the SAME entries
        // (order + initial toggle states) and the mode dropdown's
        // pre-selected index is the current mode. One model, two skins.
        List<FilterCore.ContentStack> contents = List.of(
                new FilterCore.ContentStack("DIRT", 5),
                new FilterCore.ContentStack("STONE", 9));
        Set<String> list = Set.of("STONE");
        List<SelectorModel.Entry> entries = SelectorModel.entries(contents, list);
        assertEquals(List.of(false, true), entries.stream().map(SelectorModel.Entry::onList).toList());
        assertEquals(0, SelectorModel.modeIndex(Mode.OFF));
        assertEquals(1, SelectorModel.modeIndex(Mode.WHITELIST));
        assertEquals(2, SelectorModel.modeIndex(Mode.BLACKLIST));
        for (Mode m : Mode.values()) assertEquals(m, SelectorModel.modeAt(SelectorModel.modeIndex(m)));
    }

    // ------------------------------------------------- config posture (§9)

    /**
     * Mike's config posture (Nov 6, 2026, 11:51 PM CST): the shipped
     * config.yml IS the defaults source — a server config with keys
     * absent must fall back to the decided values (buffer window
     * 168h, hopper filtering ON, cap 162, page 28), and every message
     * key the code speaks must exist in the file (config-first means
     * the words live in the file, not only in code fallbacks).
     */
    @Test
    void configDefaultsWhenKeysAbsent() throws Exception {
        java.io.File f = new java.io.File(System.getProperty("autosort.config", "config.yml"));
        assertTrue(f.isFile(), "shipped config.yml must be readable from the project root: " + f.getAbsolutePath());
        org.bukkit.configuration.file.YamlConfiguration shipped =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
        org.bukkit.configuration.file.YamlConfiguration user =
                new org.bukkit.configuration.file.YamlConfiguration();
        user.setDefaults(shipped); // an empty live config: every read falls back
        assertEquals(168L, user.getLong("recovery.hours"), "buffer window default = 7 days (D2)");
        assertTrue(user.getBoolean("filter.hoppers"), "hopper filtering default = ON (D3)");
        assertEquals(162, user.getInt("recovery.max-stacks"), "buffer cap default");
        assertEquals(28, user.getInt("selector.page-size"), "selector page-size default");
        assertEquals(List.of("CHEST", "BARREL", "ENDER_CHEST", "SHULKER_BOX"),
                user.getStringList("sort.types"), "parity sort-type set (P4)");
        for (String key : List.of("no-permission", "look-at-chest", "sort-enabled", "sort-disabled",
                "armed", "swept", "claimed", "confirm-needed", "confirm-how", "edit-applied",
                "gui-selector-title", "gui-confirm-title", "gui-recovery-title", "mode-off",
                "mode-whitelist", "mode-blacklist", "recovery-empty", "claim-all")) {
            assertTrue(shipped.isString("messages." + key) || shipped.isList("messages." + key),
                    "message key must live in config.yml: messages." + key);
        }
        assertFalse(shipped.isString("messages.read-only"),
                "the read-only message died with the read-only view (§6.4, Nov 6, 2026)");
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
