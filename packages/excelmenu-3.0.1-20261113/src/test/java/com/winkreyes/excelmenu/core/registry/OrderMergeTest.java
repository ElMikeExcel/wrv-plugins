package com.winkreyes.excelmenu.core.registry;

import com.winkreyes.excelmenu.core.platform.Platform;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/** 3.0 OrderMerge properties (DESIGN §3.3/§7). */
public class OrderMergeTest {

    private static ResolvedEntry entry(String id, String group, int order) {
        EntryDef def = new EntryDef(id, "server_menu", group, order, null,
                "STONE", id, List.of(), List.of(),
                Set.of(Platform.JAVA, Platform.BEDROCK), null, null, null,
                Map.of(), null, null, EntryDef.Origin.FILE);
        return new ResolvedEntry(def, "STONE", id, List.of(), List.of(), null, null);
    }

    private static List<ResolvedEntry> base() {
        return List.of(
                entry("a", "travel", 1), entry("b", "travel", 2), entry("c", "travel", 3),
                entry("x", "me", 1), entry("y", "me", 2));
    }

    private static List<String> ids(List<ResolvedEntry> list) {
        return list.stream().map(ResolvedEntry::id).toList();
    }

    private static PlayerMenuPrefs prefs(Map<String, List<String>> groups,
                                         List<String> groupOrder) {
        return new PlayerMenuPrefs(1, "server_menu", groupOrder, groups);
    }

    @Test
    public void untouchedPlayerKeepsDefault() {
        assertEquals(List.of("a", "b", "c", "x", "y"),
                ids(OrderMerge.merge(base(), null, List.of("travel", "me"), true)));
        assertEquals(List.of("a", "b", "c", "x", "y"),
                ids(OrderMerge.merge(base(), PlayerMenuPrefs.empty("server_menu"),
                        List.of("travel", "me"), true)));
    }

    @Test
    public void storedOrderHonouredWithinGroupOnly() {
        PlayerMenuPrefs p = prefs(Map.of("travel", List.of("c", "a", "b")), List.of());
        assertEquals(List.of("c", "a", "b", "x", "y"),
                ids(OrderMerge.merge(base(), p, List.of("travel", "me"), true)));
    }

    @Test
    public void newEntryAppendsAtGroupEnd() {
        PlayerMenuPrefs p = prefs(Map.of("travel", List.of("c", "a")), List.of());
        // 'b' is not in the stored list: it appends after the stored
        // ids, in default position relative to other new ids.
        assertEquals(List.of("c", "a", "b", "x", "y"),
                ids(OrderMerge.merge(base(), p, List.of("travel", "me"), true)));
    }

    @Test
    public void vanishedIdDropsSilently() {
        PlayerMenuPrefs p = prefs(Map.of("travel", List.of("gone", "c", "a", "b")), List.of());
        assertEquals(List.of("c", "a", "b", "x", "y"),
                ids(OrderMerge.merge(base(), p, List.of("travel", "me"), true)));
    }

    @Test
    public void unknownGroupIgnored() {
        PlayerMenuPrefs p = prefs(Map.of("nope", List.of("y", "x")), List.of());
        assertEquals(List.of("a", "b", "c", "x", "y"),
                ids(OrderMerge.merge(base(), p, List.of("travel", "me"), true)));
    }

    @Test
    public void groupOrderAppliesOnBedrockPathOnly() {
        PlayerMenuPrefs p = prefs(Map.of(), List.of("me", "travel"));
        assertEquals(List.of("x", "y", "a", "b", "c"),
                ids(OrderMerge.merge(base(), p, List.of("travel", "me"), true)));
        // Java: group sequence stays the config order (§3.3).
        assertEquals(List.of("a", "b", "c", "x", "y"),
                ids(OrderMerge.merge(base(), p, List.of("travel", "me"), false)));
    }

    @Test
    public void mergeIsIdempotent() {
        PlayerMenuPrefs p = prefs(Map.of("travel", List.of("c", "a", "b")),
                List.of("me", "travel"));
        List<ResolvedEntry> once = OrderMerge.merge(base(), p, List.of("travel", "me"), true);
        List<ResolvedEntry> twice = OrderMerge.merge(once, p, List.of("travel", "me"), true);
        assertEquals(ids(once), ids(twice));
    }

    @Test
    public void mergeIdsAlgebra() {
        assertEquals(List.of("c", "a", "b"),
                OrderMerge.mergeIds(List.of("a", "b", "c"), List.of("c", "a")));
        assertEquals(List.of("b", "a"),
                OrderMerge.mergeIds(List.of("a", "b"), List.of("gone", "b", "a")));
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
