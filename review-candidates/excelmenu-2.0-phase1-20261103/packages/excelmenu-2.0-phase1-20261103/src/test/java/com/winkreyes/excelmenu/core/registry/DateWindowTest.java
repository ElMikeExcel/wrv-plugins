package com.winkreyes.excelmenu.core.registry;

import com.winkreyes.excelmenu.core.platform.Platform;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The dated-entry proof (DESIGN-2.0 §4, last bullet): visibility is a
 * RENDER-TIME decision. The same loaded entry object is resolved at
 * several instants with no reload and no file edit — present inside
 * its window, gone outside it — including the midnight
 * America/Chicago boundary, where the entry's last day ends at
 * 06:00 UTC, not at 00:00 UTC.
 */
public class DateWindowTest {

    private static final RegistryConfig CONFIG = RegistryConfig.defaults();

    private static EntryDef dated(String id, String from, String until) {
        return new EntryDef(id, "server_menu", "help", 10, null,
                "COMPASS", "Dated", List.of(), List.of("[close]"),
                Set.of(Platform.JAVA, Platform.BEDROCK), null, null, null,
                java.util.Map.of(),
                from == null ? null : java.time.LocalDate.parse(from),
                until == null ? null : java.time.LocalDate.parse(until),
                EntryDef.Origin.FILE);
    }

    private static boolean visible(EntryDef e, String instantUtc) {
        Clock clock = Clock.fixed(Instant.parse(instantUtc), ZoneOffset.UTC);
        return !EntryResolver.resolve("server_menu", List.of(e),
                new FakeViewer(), clock, CONFIG).isEmpty();
    }

    @Test
    public void windowEdgesAreInclusive() {
        EntryDef e = dated("w", "2026-10-31", "2026-11-05");
        // Chicago is UTC-5 (CDT) on Oct 30/31 morning, UTC-6 (CST) after Nov 1 2am.
        assertEquals(false, visible(e, "2026-10-30T12:00:00Z")); // Oct 30 in Chicago
        assertEquals(true, visible(e, "2026-10-31T12:00:00Z"));  // from-day
        assertEquals(true, visible(e, "2026-11-03T12:00:00Z"));  // inside
        assertEquals(true, visible(e, "2026-11-05T12:00:00Z"));  // until-day (inclusive)
        assertEquals(false, visible(e, "2026-11-06T12:00:00Z")); // day after
    }

    @Test
    public void midnightChicagoBoundary() {
        EntryDef e = dated("w", null, "2026-11-05");
        // Nov 5 23:59:59 CST = Nov 6 05:59:59 UTC — still the last day in Chicago.
        assertTrue(visible(e, "2026-11-06T05:59:59Z"));
        // Nov 6 00:00:00 CST = Nov 6 06:00:00 UTC — the entry retires exactly here.
        assertEquals(false, visible(e, "2026-11-06T06:00:00Z"));
        // And the from-side: Nov 5 begins at 06:00 UTC, not 00:00 UTC.
        EntryDef f = dated("f", "2026-11-05", null);
        assertEquals(false, visible(f, "2026-11-05T05:59:59Z"));
        assertTrue(visible(f, "2026-11-05T06:00:00Z"));
    }

    @Test
    public void noWindowMeansAlwaysVisible() {
        EntryDef e = dated("w", null, null);
        assertTrue(visible(e, "2026-01-01T00:00:00Z"));
        assertTrue(visible(e, "2030-01-01T00:00:00Z"));
    }

    /**
     * The throwaway dated test entry, loaded from YAML through the
     * real loader, then resolved at four instants WITHOUT reloading:
     * the exact mechanism the Halloween entry's Nov 5 retirement
     * depends on.
     */
    @Test
    public void datedTestEntryFromYamlResolvesAtRenderTime() {
        String yaml = """
                id: dated_test
                menu: server_menu
                group: help
                order: 99
                icon: CLOCK
                label: '&e&lDated test'
                action: '[close]'
                visible-until: 2026-11-05
                """;
        EntryLoader.Result r = EntryLoader.load("dated_test",
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        assertTrue(r.errors().toString(), r.ok());
        EntryDef e = r.def();
        assertTrue(visible(e, "2026-11-04T18:00:00Z"));   // in window
        assertTrue(visible(e, "2026-11-05T18:00:00Z"));   // last day
        assertEquals(false, visible(e, "2026-11-06T06:00:00Z")); // retired at Chicago midnight
        assertEquals(false, visible(e, "2026-11-07T18:00:00Z")); // stays retired
    }

    /** The REAL Halloween fixture entry behaves the same way. */
    @Test
    public void halloweenFixtureRetiresAfterNov5() throws Exception {
        EntryDef e;
        try (InputStream in = getClass().getResourceAsStream("/entries/halloween.yml")) {
            EntryLoader.Result r = EntryLoader.load("halloween", in);
            assertTrue(r.errors().toString(), r.ok());
            e = r.def();
        }
        assertEquals("2026-11-05", e.visibleUntil().toString());
        assertTrue(visible(e, "2026-11-01T18:00:00Z"));   // in season
        assertTrue(visible(e, "2026-11-05T23:00:00Z"));   // last evening (CST 17:00)
        assertEquals(false, visible(e, "2026-11-06T06:00:00Z")); // gone, no reload
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
