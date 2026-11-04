package com.winkreyesvalley.excelmagiclevel.core;

import org.junit.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

/** ExcelMagicLevel — the §9.3 export shape, exercised against a fixture. */
public class ExportRowsTest {
    @Test
    public void rowsAreMcMmoShapedAndSorted() {
        Curve curve = new Curve(20, 100.0, 1.5, 10, Map.of());
        UUID low = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID high = UUID.fromString("00000000-0000-0000-0000-000000000002");
        String yaml = ExportRows.toYaml(Map.of(low, 150L, high, 1700L), curve, Instant.parse("2026-10-31T00:00:00Z"));
        assertTrue(yaml.contains("skill: magic"));
        assertTrue(yaml.contains("total-xp: 1700"));
        assertTrue(yaml.contains("level: 5"));  // 1700 total = exactly level 5 on the Standard curve
        assertTrue(yaml.contains("level: 2"));  // 150 total = level 2
        assertTrue("highest XP first", yaml.indexOf("1700") < yaml.indexOf("150"));
    }

    @Test
    public void emptyStoreExportsHeaderOnly() {
        Curve curve = new Curve(20, 100.0, 1.5, 10, Map.of());
        String yaml = ExportRows.toYaml(Map.of(), curve, Instant.EPOCH);
        assertTrue(yaml.contains("rows:"));
    }
}
