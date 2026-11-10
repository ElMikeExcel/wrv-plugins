package com.winkreyes.excelautosort;

import com.winkreyes.excelautosort.core.ActivesParse;
import com.winkreyes.excelautosort.core.BufferCore;
import com.winkreyes.excelautosort.core.ChestKeys;
import com.winkreyes.excelautosort.core.FilterCore;
import com.winkreyes.excelautosort.core.MaxOrgCore;
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

    // ------------------------------------------------- Max Organization (§15, 0.2.0)

    private static MaxOrgCore.SlotStack ss(String mat, int count, int max) {
        return new MaxOrgCore.SlotStack(mat, "{}", count, max);
    }

    @Test
    void entitlementAccessStates() {
        long now = 1_800_000_000_000L;
        assertEquals(MaxOrgCore.Access.NONE,
                MaxOrgCore.access(MaxOrgCore.Entitlement.NONE, now));
        assertEquals(MaxOrgCore.Access.RENTED,
                MaxOrgCore.access(new MaxOrgCore.Entitlement(false, now + 1000), now));
        assertEquals(MaxOrgCore.Access.NONE,
                MaxOrgCore.access(new MaxOrgCore.Entitlement(false, now - 1), now),
                "an expiry in the past is lapsed, not rented");
        assertEquals(MaxOrgCore.Access.OWNED,
                MaxOrgCore.access(new MaxOrgCore.Entitlement(true, 0), now));
        assertEquals(MaxOrgCore.Access.OWNED,
                MaxOrgCore.access(new MaxOrgCore.Entitlement(true, now - 5000), now),
                "owned beats any rent timestamp");
        assertTrue(MaxOrgCore.lapsed(new MaxOrgCore.Entitlement(false, now - 1), now));
        assertFalse(MaxOrgCore.lapsed(new MaxOrgCore.Entitlement(false, now + 1), now));
        assertFalse(MaxOrgCore.lapsed(MaxOrgCore.Entitlement.NONE, now),
                "never rented is not 'lapsed' — the unlock screen words differ");
    }

    @Test
    void rentRenewalAndBuyMath() {
        long now = 1_800_000_000_000L;
        // Fresh rent: 30 days from now (§15.2 settlement values).
        assertEquals(now + 30 * MaxOrgCore.DAY_MS, MaxOrgCore.rentExpiry(now, 30));
        // Renewing an ACTIVE rental extends from the current expiry —
        // the player keeps the days already paid for.
        MaxOrgCore.Entitlement active = new MaxOrgCore.Entitlement(false, now + 10 * MaxOrgCore.DAY_MS);
        assertEquals(now + 40 * MaxOrgCore.DAY_MS, MaxOrgCore.renewalExpiry(active, now, 30));
        assertEquals(new MaxOrgCore.Entitlement(false, now + 40 * MaxOrgCore.DAY_MS),
                MaxOrgCore.afterRent(active, now, 30));
        // Renewing a LAPSED rental starts from now — re-rent restores instantly.
        MaxOrgCore.Entitlement lapsed = new MaxOrgCore.Entitlement(false, now - 5 * MaxOrgCore.DAY_MS);
        assertEquals(now + 30 * MaxOrgCore.DAY_MS, MaxOrgCore.renewalExpiry(lapsed, now, 30));
        // Buy: permanent, rent timestamp spent — and no proration anywhere
        // in the math (afterBuy discards the remaining rent entirely).
        assertEquals(new MaxOrgCore.Entitlement(true, 0), MaxOrgCore.afterBuy(active));
        // Renting while owned is a no-op at the core (refused upstream too).
        assertEquals(new MaxOrgCore.Entitlement(true, 0),
                MaxOrgCore.afterRent(new MaxOrgCore.Entitlement(true, 0), now, 30));
    }

    @Test
    void radiusBoundaryIsExact() {
        // Player standing at a block center; radius 16 (§15.3/§15.7):
        // a chest whose CENTER is exactly 16 away is in; 17 is out.
        assertTrue(MaxOrgCore.withinRadius(0.5, 64.5, 0.5, 16, 64, 0, 16));
        assertFalse(MaxOrgCore.withinRadius(0.5, 64.5, 0.5, 17, 64, 0, 16));
        // Vertical counts too (3D distance, not a cylinder).
        assertTrue(MaxOrgCore.withinRadius(0.5, 64.5, 0.5, 0, 64 + 16, 0, 16));
        assertFalse(MaxOrgCore.withinRadius(0.5, 64.5, 0.5, 0, 64 + 17, 0, 16));
        // Diagonal: 11-11-11 is ~19.05 — out; 9-9-9 is ~15.6 — in.
        assertFalse(MaxOrgCore.withinRadius(0.5, 64.5, 0.5, 11, 64 + 11, 11, 16));
        assertTrue(MaxOrgCore.withinRadius(0.5, 64.5, 0.5, 9, 64 + 9, 9, 16));
        // Same block is always in, even at radius 0.
        assertTrue(MaxOrgCore.withinRadius(0.5, 64.5, 0.5, 0, 64, 0, 0));
    }

    @Test
    void viewFlattensRealSlotsAndSkipsEmpties() {
        MaxOrgCore.ChestView a = new MaxOrgCore.ChestView("world:1:64:1", "world", 1, 64, 1,
                List.of(ss("IRON_INGOT", 34, 64), ss(null, 0, 0), ss("DIRT", 5, 64)));
        MaxOrgCore.ChestView b = new MaxOrgCore.ChestView("world:2:64:1", "world", 2, 64, 1,
                List.of(ss("GOLD_INGOT", 3, 64)));
        List<MaxOrgCore.ViewEntry> flat = MaxOrgCore.flatten(List.of(a, b));
        assertEquals(3, flat.size());
        assertEquals(0, flat.get(0).slot());
        assertEquals(2, flat.get(1).slot(), "slot indices stay the REAL chest slots");
        assertEquals("world:2:64:1", flat.get(2).chestKey());
        // Fit math: partial similar stack + one empty slot.
        assertEquals(30 + 64, MaxOrgCore.fitsCount(
                List.of(ss("IRON_INGOT", 34, 64), ss(null, 0, 0), ss("DIRT", 5, 64)),
                ss("IRON_INGOT", 64, 64)));
        assertEquals(0, MaxOrgCore.fitsCount(List.of(ss("DIRT", 64, 64)), ss("IRON_INGOT", 1, 64)));
    }

    @Test
    void depositPrefersWhitelistThenOpenThenBlacklist() {
        // §15.7: a listed item always prefers the whitelist chest that
        // lists it — even when an open chest comes first in the network.
        MaxOrgCore.DepositChest open = new MaxOrgCore.DepositChest("open", Mode.OFF, Set.of(),
                List.of(ss(null, 0, 0)));
        MaxOrgCore.DepositChest white = new MaxOrgCore.DepositChest("white", Mode.WHITELIST,
                Set.of("COBBLESTONE"), List.of(ss(null, 0, 0)));
        MaxOrgCore.DepositChest black = new MaxOrgCore.DepositChest("black", Mode.BLACKLIST,
                Set.of("DIRT"), List.of(ss(null, 0, 0), ss(null, 0, 0)));
        List<MaxOrgCore.PlayerStack> hand = List.of(
                new MaxOrgCore.PlayerStack(0, ss("COBBLESTONE", 10, 64)),
                new MaxOrgCore.PlayerStack(1, ss("DIRT", 10, 64)),
                new MaxOrgCore.PlayerStack(2, ss("STONE", 10, 64)));
        MaxOrgCore.DepositPlan plan = MaxOrgCore.planDeposit(hand, List.of(open, white, black));
        // Cobble -> whitelist (tier 0) despite the open chest being first.
        assertTrue(plan.moves().contains(new MaxOrgCore.DepositMove(0, "white", 10)),
                "cobble prefers the whitelist chest: " + plan.moves());
        // Dirt is refused by the blacklist chest (it lists dirt); it
        // lands in the open chest (tier 1 beats tier 2 black-not-listed).
        assertTrue(plan.moves().contains(new MaxOrgCore.DepositMove(1, "open", 10)),
                "dirt goes to the open chest, never the refusing blacklist chest: " + plan.moves());
        // Stone (listed nowhere): open chest is full of dirt (10 < 64,
        // partial similar does not help stone) — wait: the open chest
        // has ONE slot, topped to 10 dirt; stone cannot share it.
        // The blacklist chest doesn't list stone, so it accepts (tier 2).
        assertTrue(plan.moves().contains(new MaxOrgCore.DepositMove(2, "black", 10)),
                "stone falls through to the blacklist chest that doesn't list it: " + plan.moves());
        assertTrue(plan.unplaced().isEmpty(), "everything placed: " + plan.unplaced());
    }

    @Test
    void depositUnplacedStaysWithPlayer() {
        // §15.5: an item no network chest will take is reported, never voided.
        MaxOrgCore.DepositChest white = new MaxOrgCore.DepositChest("white", Mode.WHITELIST,
                Set.of("COBBLESTONE"), List.of(ss(null, 0, 0)));
        List<MaxOrgCore.PlayerStack> hand = List.of(
                new MaxOrgCore.PlayerStack(0, ss("DIAMOND", 5, 64)));
        MaxOrgCore.DepositPlan plan = MaxOrgCore.planDeposit(hand, List.of(white));
        assertTrue(plan.moves().isEmpty());
        assertEquals(List.of(new MaxOrgCore.Unplaced(0, "DIAMOND", 5)), plan.unplaced());
        // Full destination: a whitelist chest with no room takes nothing.
        MaxOrgCore.DepositChest full = new MaxOrgCore.DepositChest("full", Mode.WHITELIST,
                Set.of("COBBLESTONE"), List.of(ss("COBBLESTONE", 64, 64)));
        MaxOrgCore.DepositPlan plan2 = MaxOrgCore.planDeposit(
                List.of(new MaxOrgCore.PlayerStack(0, ss("COBBLESTONE", 10, 64))), List.of(full));
        assertTrue(plan2.moves().isEmpty());
        assertEquals(10, plan2.unplaced().get(0).count());
    }

    @Test
    void depositTopsPartialsAcrossStacks() {
        // Two player stacks of the same material fill one partial chest
        // stack exactly; the planner's capacity accounting is cumulative.
        MaxOrgCore.DepositChest chest = new MaxOrgCore.DepositChest("c", Mode.OFF, Set.of(),
                List.of(ss("IRON_INGOT", 60, 64)));
        List<MaxOrgCore.PlayerStack> hand = List.of(
                new MaxOrgCore.PlayerStack(0, ss("IRON_INGOT", 3, 64)),
                new MaxOrgCore.PlayerStack(1, ss("IRON_INGOT", 3, 64)));
        MaxOrgCore.DepositPlan plan = MaxOrgCore.planDeposit(hand, List.of(chest));
        assertEquals(List.of(new MaxOrgCore.DepositMove(0, "c", 3),
                new MaxOrgCore.DepositMove(1, "c", 1)), plan.moves());
        assertEquals(List.of(new MaxOrgCore.Unplaced(1, "IRON_INGOT", 2)), plan.unplaced());
        // Different meta never tops up (similarity is material + meta).
        MaxOrgCore.SlotStack named = new MaxOrgCore.SlotStack("IRON_INGOT", "{\"name\":\"x\"}", 60, 64);
        MaxOrgCore.DepositChest c2 = new MaxOrgCore.DepositChest("c2", Mode.OFF, Set.of(), List.of(named));
        MaxOrgCore.DepositPlan plan3 = MaxOrgCore.planDeposit(
                List.of(new MaxOrgCore.PlayerStack(0, ss("IRON_INGOT", 3, 64))), List.of(c2));
        assertTrue(plan3.moves().isEmpty(), "a named stack's slot is not room for a plain one");
    }

    @Test
    void maxOrgProtectShapesCarryTheirFields() {
        Map<String, Object> buy = ProtectEvents.maxOrgPurchase(
                "uuid-1", "Mike", ProtectEvents.PURCHASE_BUY, 15000, true, 0);
        assertEquals("autosort_maxorg_purchase", ProtectEvents.MAXORG_PURCHASE);
        assertEquals("buy", buy.get("kind"));
        assertEquals(15000.0, buy.get("amount"));
        assertEquals(Boolean.TRUE, buy.get("owned_after"));
        assertEquals(0L, buy.get("rent_until_ms"));
        assertEquals("max-org", buy.get("via"));
        Map<String, Object> rent = ProtectEvents.maxOrgPurchase(
                "uuid-1", "Mike", ProtectEvents.PURCHASE_RENT, 1000, false, 123456789L);
        assertEquals(123456789L, rent.get("rent_until_ms"));
        Map<String, Object> take = ProtectEvents.networkTake(
                "world", 1, 64, 2, "world:1:64:2", "uuid-1", "Mike", "uuid-2",
                "minecraft:iron_ingot", 34);
        assertEquals("network-view", take.get("via"), "the network cause is distinguishable (§15.5)");
        assertEquals("world:1:64:2", take.get("chest_key"));
        assertEquals(34, take.get("count"));
        Map<String, Object> dep = ProtectEvents.networkDeposit(
                "world", 1, 64, 2, "world:1:64:2", "uuid-1", "Mike", "uuid-2",
                "minecraft:cobblestone", 10);
        assertEquals("network-view", dep.get("via"));
        assertEquals("autosort_network_deposit", ProtectEvents.NETWORK_DEPOSIT);
    }

    @Test
    void moneyReadsPlainly() {
        assertEquals("$1,000", MaxOrgCore.money(1000));
        assertEquals("$15,000", MaxOrgCore.money(15000));
        assertEquals("$0", MaxOrgCore.money(0));
        assertEquals("$9.99", MaxOrgCore.money(9.99));
        assertEquals("$1,234.50", MaxOrgCore.money(1234.5));
    }

    @Test
    void maxOrgConfigDefaults() throws Exception {
        // §15.7 config-first: the settlement values ship as defaults,
        // and every max-org message key the code speaks lives in the file.
        java.io.File f = new java.io.File(System.getProperty("autosort.config", "config.yml"));
        org.bukkit.configuration.file.YamlConfiguration shipped =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
        org.bukkit.configuration.file.YamlConfiguration user =
                new org.bukkit.configuration.file.YamlConfiguration();
        user.setDefaults(shipped);
        assertTrue(user.getBoolean("max-org.enabled"), "max-org ships on");
        assertEquals(1000.0, user.getDouble("max-org.rent-price"), "rent $1,000 (Mike, Nov 7, 12:01 AM)");
        assertEquals(30, user.getInt("max-org.rent-days"), "rent 30 days");
        assertEquals(15000.0, user.getDouble("max-org.buy-price"), "buy $15,000");
        assertEquals(16, user.getInt("max-org.radius"), "radius 16");
        assertEquals(45, user.getInt("max-org.page-size"), "network page 45");
        for (String key : List.of("status-maxorg", "gui-maxorg-title", "gui-maxorg-confirm-title",
                "gui-network-title", "maxorg-state-owned", "maxorg-state-rented", "maxorg-state-lapsed",
                "maxorg-state-none", "maxorg-rent-button", "maxorg-buy-button", "maxorg-owned-note",
                "maxorg-open-network", "maxorg-confirm-charge", "maxorg-confirm-how", "maxorg-rented",
                "maxorg-bought", "maxorg-already-owned", "maxorg-insufficient", "maxorg-economy-down",
                "maxorg-disabled", "maxorg-network-empty-title", "maxorg-network-info",
                "maxorg-deposit-button", "maxorg-deposited", "maxorg-deposited-none", "maxorg-unplaced",
                "maxorg-chest-gone", "maxorg-no-room")) {
            assertTrue(shipped.isString("messages." + key) || shipped.isList("messages." + key),
                    "message key must live in config.yml: messages." + key);
        }
        assertTrue(shipped.isList("messages.maxorg-pitch"), "the pitch is a config list (§15.2)");
        assertTrue(shipped.isList("messages.maxorg-confirm-rent"));
        assertTrue(shipped.isList("messages.maxorg-confirm-buy"));
        assertTrue(shipped.isList("messages.maxorg-network-empty"));
    }

    // ------------------------------------------------- admin surface (0.2.1)

    @Test
    void adminRevokeClearsOwnedAndRentAtOnce() {
        long now = 1_800_000_000_000L;
        // Owned: revoke removes the permanent flag entirely.
        MaxOrgCore.Entitlement owned = new MaxOrgCore.Entitlement(true, 0);
        assertTrue(MaxOrgCore.holdsAnything(owned));
        assertEquals(MaxOrgCore.Entitlement.NONE, MaxOrgCore.afterRevoke(owned));
        assertEquals(MaxOrgCore.Access.NONE,
                MaxOrgCore.access(MaxOrgCore.afterRevoke(owned), now));
        // Rented: revoke cancels the rent — the expiry is gone, not aged.
        MaxOrgCore.Entitlement rented = new MaxOrgCore.Entitlement(false, now + 10 * MaxOrgCore.DAY_MS);
        assertTrue(MaxOrgCore.holdsAnything(rented));
        MaxOrgCore.Entitlement revoked = MaxOrgCore.afterRevoke(rented);
        assertEquals(0L, revoked.rentUntilMs(), "revoke zeroes the rent, it does not expire it");
        assertFalse(revoked.owned());
        assertEquals(MaxOrgCore.Access.NONE, MaxOrgCore.access(revoked, now));
        assertFalse(MaxOrgCore.lapsed(revoked, now),
                "a revoked player reads NONE afterwards, not 'lapsed'");
    }

    @Test
    void adminRevokeOfNothingIsACleanNoOp() {
        assertFalse(MaxOrgCore.holdsAnything(MaxOrgCore.Entitlement.NONE));
        assertFalse(MaxOrgCore.holdsAnything(null));
        assertEquals(MaxOrgCore.Entitlement.NONE,
                MaxOrgCore.afterRevoke(MaxOrgCore.Entitlement.NONE));
        // Idempotent: revoking a revoke changes nothing further.
        MaxOrgCore.Entitlement once = MaxOrgCore.afterRevoke(
                new MaxOrgCore.Entitlement(true, 123L));
        assertEquals(once, MaxOrgCore.afterRevoke(once));
    }

    @Test
    void adminRevokeClearsALapsedRecordToo() {
        long now = 1_800_000_000_000L;
        // A lapsed rental is still a record on the books: the admin
        // status reads LAPSED until a revoke clears the stale expiry.
        MaxOrgCore.Entitlement lapsed = new MaxOrgCore.Entitlement(false, now - 5 * MaxOrgCore.DAY_MS);
        assertTrue(MaxOrgCore.lapsed(lapsed, now));
        assertTrue(MaxOrgCore.holdsAnything(lapsed),
                "a lapsed record still holds something for a revoke to remove");
        assertEquals(MaxOrgCore.Entitlement.NONE, MaxOrgCore.afterRevoke(lapsed));
    }

    @Test
    void adminGrantIsPermanentAndMatchesTheBuyShape() {
        long now = 1_800_000_000_000L;
        // The no-charge grant lands in exactly the state a buy lands in.
        assertEquals(MaxOrgCore.afterBuy(MaxOrgCore.Entitlement.NONE),
                MaxOrgCore.afterAdminGrant(MaxOrgCore.Entitlement.NONE));
        MaxOrgCore.Entitlement rented = new MaxOrgCore.Entitlement(false, now + 10 * MaxOrgCore.DAY_MS);
        assertEquals(MaxOrgCore.afterBuy(rented), MaxOrgCore.afterAdminGrant(rented),
                "granting over a live rent spends the rent timestamp, like a buy");
        MaxOrgCore.Entitlement granted = MaxOrgCore.afterAdminGrant(rented);
        assertEquals(MaxOrgCore.Access.OWNED,
                MaxOrgCore.access(granted, now + 520L * 7 * MaxOrgCore.DAY_MS),
                "a granted entitlement is still OWNED ten years on");
        // Granting an owner is a no-op in shape (the service reports it).
        assertEquals(granted, MaxOrgCore.afterAdminGrant(granted));
    }

    @Test
    void adminProtectShapesCarryTheirFields() {
        Map<String, Object> revoke = ProtectEvents.maxOrgAdmin(
                "console", "uuid-target", "Kay", true, 0L, false, 0L);
        assertEquals("autosort_maxorg_admin_revoke", ProtectEvents.MAXORG_ADMIN_REVOKE);
        assertEquals("autosort_maxorg_admin_grant", ProtectEvents.MAXORG_ADMIN_GRANT);
        assertEquals("console", revoke.get("actor"));
        assertEquals("uuid-target", revoke.get("target"));
        assertEquals("Kay", revoke.get("target_name"));
        assertEquals(Boolean.TRUE, revoke.get("owned_before"));
        assertEquals(Boolean.FALSE, revoke.get("owned_after"));
        assertEquals(0L, revoke.get("rent_until_after_ms"));
        assertEquals("admin", revoke.get("via"),
                "an admin correction never wears the purchase cause");
        Map<String, Object> grant = ProtectEvents.maxOrgAdmin(
                "uuid-admin", "uuid-target", "Kay", false, 123L, true, 0L);
        assertEquals(123L, grant.get("rent_until_before_ms"),
                "the grant record keeps the rent it replaced");
        assertEquals(Boolean.TRUE, grant.get("owned_after"));
    }

    @Test
    void adminSurfaceIsDeclaredInPluginAndConfig() throws Exception {
        // The 0.2.1 admin surface is declared, not implied: plugin.yml
        // carries the version bump and the admin permission, and the
        // shipped usage line names the per-player subcommands.
        java.io.File pf = new java.io.File(System.getProperty("autosort.plugin", "plugin.yml"));
        assertTrue(pf.isFile(), "shipped plugin.yml must be readable from the project root");
        org.bukkit.configuration.file.YamlConfiguration pluginYml =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(pf);
        assertEquals("0.2.1", pluginYml.getString("version"));
        assertTrue(pluginYml.isConfigurationSection("permissions.excelautosort.admin"),
                "the admin permission node must be declared");
        assertEquals("op", pluginYml.getString("permissions.excelautosort.admin.default"),
                "admin defaults to op — console and ops only");
        java.io.File cf = new java.io.File(System.getProperty("autosort.config", "config.yml"));
        org.bukkit.configuration.file.YamlConfiguration shipped =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(cf);
        boolean usageNamesAdmin = shipped.getStringList("messages.usage").stream()
                .anyMatch(l -> l.contains("admin") && l.contains("status")
                        && l.contains("revoke") && l.contains("grant"));
        assertTrue(usageNamesAdmin,
                "the usage line must name admin status|revoke|grant: "
                        + shipped.getStringList("messages.usage"));
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
