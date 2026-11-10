package com.winkreyes.exceldetails;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic proofs: segment lifecycle, budget self-cap, line
 * shedding, formatting. Excel — Wink Reyes Valley (Iris / Mike).
 */
public class DetailsTest {

    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();
    private static final long T0 = 1_000_000L;

    // ---------- segment lifecycle ----------

    @Test
    void segmentOpensOnFirstHitAndAccumulates() {
        SegmentTracker t = new SegmentTracker();
        assertFalse(t.isActive());
        t.recordDamageDone(A, "Mike", 12.5, "Warden", T0);
        assertTrue(t.isActive());
        t.recordDamageDone(A, "Mike", 7.5, "Warden", T0 + 1000);
        t.recordDamageTaken(A, "Mike", 18.75, "Warden", T0 + 1000);
        assertEquals(20.0, t.currentStats(A).damageDone(), 1e-9);
        assertEquals(18.75, t.currentStats(A).damageTaken(), 1e-9);
        assertEquals(20.0, t.currentStats(A).doneByTarget().get("Warden"), 1e-9);
        assertEquals(1000, t.elapsedMs(T0 + 1000));
    }

    @Test
    void segmentClosesAfterQuietTimeoutAndMergesOverall() {
        SegmentTracker t = new SegmentTracker();
        t.recordDamageDone(A, "Mike", 100, "Zombie", T0);
        assertFalse(t.tick(T0 + 7_999, 8_000));
        assertTrue(t.isActive());
        assertTrue(t.tick(T0 + 8_000, 8_000));
        assertFalse(t.isActive());
        assertEquals(100.0, t.overallStats(A).damageDone(), 1e-9);
        // last segment stays readable while idle
        assertEquals(1, t.ranked(CombatStats.Window.DAMAGE_DONE).size());
        assertEquals(100.0, t.ranked(CombatStats.Window.DAMAGE_DONE).get(0).amount(), 1e-9);
        // a fresh hit opens a NEW segment with clean numbers
        t.recordDamageDone(A, "Mike", 5, "Zombie", T0 + 20_000);
        assertEquals(5.0, t.currentStats(A).damageDone(), 1e-9);
        assertEquals(100.0, t.overallStats(A).damageDone(), 1e-9);
    }

    @Test
    void healingIsTrackedSeparately() {
        SegmentTracker t = new SegmentTracker();
        t.recordHealing(B, "Kay", 6.0, T0);
        t.recordDamageDone(B, "Kay", 3.0, "Skeleton", T0);
        assertEquals(6.0, t.currentStats(B).healingDone(), 1e-9);
        assertEquals(3.0, t.currentStats(B).damageDone(), 1e-9);
        assertEquals(6.0, t.windowTotal(CombatStats.Window.HEALING_DONE), 1e-9);
    }

    @Test
    void manualClearDiscardsAndRestartsClock() {
        SegmentTracker t = new SegmentTracker();
        t.recordDamageDone(A, "Mike", 50, "Zombie", T0);
        t.clearCurrent(T0 + 5_000);
        assertTrue(t.isActive());
        assertEquals(0.0, t.currentStats(A).damageDone(), 1e-9);
        assertEquals(0, t.elapsedMs(T0 + 5_000));
        // cleared numbers never reach the overall totals
        assertTrue(t.tick(T0 + 13_000, 8_000));
        assertEquals(0.0, t.overallStats(A).damageDone(), 1e-9);
    }

    @Test
    void rankingIsDescendingAndZeroesExcluded() {
        SegmentTracker t = new SegmentTracker();
        t.recordDamageDone(A, "Mike", 30, "Zombie", T0);
        t.recordDamageDone(B, "Kay", 90, "Zombie", T0);
        List<SegmentTracker.Row> rows = t.ranked(CombatStats.Window.DAMAGE_DONE);
        assertEquals(2, rows.size());
        assertEquals("Kay", rows.get(0).name());
        assertEquals(120.0, t.windowTotal(CombatStats.Window.DAMAGE_DONE), 1e-9);
        assertTrue(t.ranked(CombatStats.Window.DAMAGE_TAKEN).isEmpty());
    }

    @Test
    void nonPositiveAmountsNeverOpenASegment() {
        SegmentTracker t = new SegmentTracker();
        t.recordDamageDone(A, "Mike", 0, "Zombie", T0);
        t.recordDamageTaken(A, "Mike", -3, "Fall", T0);
        assertFalse(t.isActive());
    }

    // ---------- budget / placement math ----------

    @Test
    void piggybackBudgetIsFifteenMinusForeignLines() {
        assertEquals(15, MeterMath.meterBudget(0));
        assertEquals(9, MeterMath.meterBudget(6));   // typical EM party box
        assertEquals(0, MeterMath.meterBudget(15));  // quest view owns the box
        assertEquals(0, MeterMath.meterBudget(20));
    }

    @Test
    void linesShedFooterThenLowestRowsHeaderLast() {
        SegmentTracker t = new SegmentTracker();
        t.recordDamageDone(A, "Mike", 100, "Warden", T0);
        t.recordDamageDone(B, "Kay", 50, "Warden", T0);
        List<SegmentTracker.Row> rows = t.ranked(CombatStats.Window.DAMAGE_DONE);
        // budget 2: header + top row only, footer shed
        List<String> two = LineBuilder.build(CombatStats.Window.DAMAGE_DONE, rows, A, 150, 30_000, 6, 2);
        assertEquals(2, two.size());
        assertTrue(two.get(0).contains("Details!"));
        assertTrue(two.get(1).contains("Mike"));
        // budget 1: header alone survives
        List<String> one = LineBuilder.build(CombatStats.Window.DAMAGE_DONE, rows, A, 150, 30_000, 6, 1);
        assertEquals(1, one.size());
        // budget 0: nothing written at all
        assertTrue(LineBuilder.build(CombatStats.Window.DAMAGE_DONE, rows, A, 150, 30_000, 6, 0).isEmpty());
    }

    @Test
    void ownRowSurvivesTheCut() {
        SegmentTracker t = new SegmentTracker();
        UUID c = UUID.randomUUID();
        t.recordDamageDone(A, "Mike", 300, "Warden", T0);
        t.recordDamageDone(B, "Kay", 200, "Warden", T0);
        t.recordDamageDone(c, "Rylie", 10, "Warden", T0);
        List<SegmentTracker.Row> rows = t.ranked(CombatStats.Window.DAMAGE_DONE);
        // budget 4 = header + 1 row + footer; viewer is rank 3 but keeps a row
        List<String> lines = LineBuilder.build(CombatStats.Window.DAMAGE_DONE, rows, c, 510, 60_000, 6, 4);
        assertEquals(4, lines.size());
        assertTrue(lines.stream().anyMatch(l -> l.contains("Rylie")), String.join(" | ", lines));
    }

    // ---------- formatting ----------

    @Test
    void compactAmountsReadLikeDetails() {
        assertEquals("950", MeterMath.compact(950));
        assertEquals("12.5", MeterMath.compact(12.5));
        assertEquals("12.5k", MeterMath.compact(12_500));
        assertEquals("3.4M", MeterMath.compact(3_400_000));
        assertEquals("0", MeterMath.compact(0));
    }

    @Test
    void percentAndPerSecondAndClock() {
        assertEquals(45, MeterMath.percent(45, 100));
        assertEquals(0, MeterMath.percent(5, 0));
        assertEquals(10.0, MeterMath.perSecond(20, 2_000), 1e-9);
        assertEquals(20.0, MeterMath.perSecond(20, 500), 1e-9); // 1 s floor
        assertEquals("0:42", MeterMath.clock(42_000));
        assertEquals("12:05", MeterMath.clock(725_000));
    }

    @Test
    void windowParsingAndCycling() {
        assertEquals(CombatStats.Window.DAMAGE_TAKEN, CombatStats.Window.parse("taken"));
        assertEquals(CombatStats.Window.HEALING_DONE, CombatStats.Window.parse("healing"));
        assertEquals(CombatStats.Window.DAMAGE_DONE, CombatStats.Window.parse("damage"));
        assertNull(CombatStats.Window.parse("bogus"));
        assertEquals(CombatStats.Window.DAMAGE_TAKEN, CombatStats.Window.DAMAGE_DONE.next());
        assertEquals(CombatStats.Window.DAMAGE_DONE, CombatStats.Window.HEALING_DONE.next());
    }

    @Test
    void prefsDefaultOnAndToggleFlips() {
        PlayerPrefs p = new PlayerPrefs();
        assertTrue(p.isEnabled(A));
        assertFalse(p.toggle(A));
        assertTrue(p.toggle(A));
        assertEquals(CombatStats.Window.DAMAGE_DONE, p.window(A));
    }

    // ---------- 0.2.0: the mix (per-metric checkboxes) ----------

    @Test
    void mixDefaultsToDamageDoneOnly() {
        PlayerPrefs p = new PlayerPrefs();
        assertEquals(EnumSet.of(CombatStats.Window.DAMAGE_DONE), p.shownWindows(A));
        assertTrue(p.shows(A, CombatStats.Window.DAMAGE_DONE));
        assertFalse(p.shows(A, CombatStats.Window.DAMAGE_TAKEN));
        assertFalse(p.shows(A, CombatStats.Window.HEALING_DONE));
    }

    @Test
    void mixTogglesAreIndependentAndAllOffIsLegal() {
        PlayerPrefs p = new PlayerPrefs();
        assertTrue(p.toggleShown(A, CombatStats.Window.DAMAGE_TAKEN));
        assertTrue(p.toggleShown(A, CombatStats.Window.HEALING_DONE));
        assertEquals(EnumSet.allOf(CombatStats.Window.class), p.shownWindows(A));
        // untick everything: a legal state (meter tracks, shows no box)
        assertFalse(p.toggleShown(A, CombatStats.Window.DAMAGE_DONE));
        assertFalse(p.toggleShown(A, CombatStats.Window.DAMAGE_TAKEN));
        assertFalse(p.toggleShown(A, CombatStats.Window.HEALING_DONE));
        assertTrue(p.shownWindows(A).isEmpty());
    }

    @Test
    void showOnlyIsTheExclusiveQuickPath() {
        PlayerPrefs p = new PlayerPrefs();
        p.toggleShown(A, CombatStats.Window.HEALING_DONE);
        p.showOnly(A, CombatStats.Window.DAMAGE_TAKEN); // /details window + next semantics
        assertEquals(EnumSet.of(CombatStats.Window.DAMAGE_TAKEN), p.shownWindows(A));
        assertEquals(CombatStats.Window.DAMAGE_TAKEN, p.window(A)); // focus follows
    }

    private static LineBuilder.Section section(SegmentTracker t, CombatStats.Window w) {
        List<SegmentTracker.Row> rows = t.ranked(w);
        double total = rows.stream().mapToDouble(SegmentTracker.Row::amount).sum();
        return new LineBuilder.Section(w, rows, total);
    }

    private static SegmentTracker threeWindowTracker(int actorsPerWindow) {
        SegmentTracker t = new SegmentTracker();
        for (int i = 0; i < actorsPerWindow; i++) {
            UUID id = UUID.randomUUID();
            t.recordDamageDone(id, "Done" + i, 100 - i, "Zombie", T0);
            t.recordDamageTaken(id, "Done" + i, 90 - i, "Zombie", T0);
            t.recordHealing(id, "Done" + i, 80 - i, T0);
        }
        return t;
    }

    @Test
    void sectionsComposeOneTwoThree() {
        SegmentTracker t = threeWindowTracker(3);
        // ONE section: header + rows + footer, 0.1.0 shape
        List<String> one = LineBuilder.buildSections(
                List.of(section(t, CombatStats.Window.DAMAGE_DONE)), A, 8_400, 6, 15);
        assertEquals(5, one.size());
        assertTrue(one.get(0).contains("Damage Done"));
        assertTrue(one.get(4).contains("0:08"));
        // TWO sections: both headers, canonical order, footer kept
        List<String> two = LineBuilder.buildSections(List.of(
                section(t, CombatStats.Window.DAMAGE_DONE),
                section(t, CombatStats.Window.HEALING_DONE)), A, 8_400, 6, 15);
        assertEquals(9, two.size());
        assertTrue(two.get(0).contains("Damage Done"));
        assertTrue(two.get(4).contains("Healing Done"));
        assertTrue(two.get(8).contains("0:08"));
        // THREE sections: three separated headers in canonical order
        List<String> three = LineBuilder.buildSections(List.of(
                section(t, CombatStats.Window.DAMAGE_DONE),
                section(t, CombatStats.Window.DAMAGE_TAKEN),
                section(t, CombatStats.Window.HEALING_DONE)), A, 8_400, 6, 15);
        int dmg = -1, tak = -1, heal = -1;
        for (int i = 0; i < three.size(); i++) {
            if (three.get(i).contains("Damage Done")) dmg = i;
            if (three.get(i).contains("Damage Taken")) tak = i;
            if (three.get(i).contains("Healing Done")) heal = i;
        }
        assertTrue(dmg >= 0 && tak > dmg && heal > tak, String.join(" | ", three));
        assertTrue(three.size() <= 15);
    }

    @Test
    void sectionsTruncateToTheFifteenCeiling() {
        SegmentTracker t = threeWindowTracker(10);
        List<String> lines = LineBuilder.buildSections(List.of(
                section(t, CombatStats.Window.DAMAGE_DONE),
                section(t, CombatStats.Window.DAMAGE_TAKEN),
                section(t, CombatStats.Window.HEALING_DONE)), A, 60_000, 6, 15);
        // worked case (DESIGN §2): 3 headers + 4 rows each, footer shed
        assertEquals(15, lines.size());
        assertEquals(3, lines.stream().filter(l -> l.contains("Details!")).count());
        assertTrue(lines.stream().noneMatch(l -> l.contains("• you")), String.join(" | ", lines));
    }

    @Test
    void sectionsFitUnderAFatEmPartyBox() {
        SegmentTracker t = threeWindowTracker(10);
        int budget = MeterMath.meterBudget(7); // EM party box at 7 lines -> 8 left
        List<String> lines = LineBuilder.buildSections(List.of(
                section(t, CombatStats.Window.DAMAGE_DONE),
                section(t, CombatStats.Window.DAMAGE_TAKEN),
                section(t, CombatStats.Window.HEALING_DONE)), A, 60_000, 6, budget);
        // worked case (DESIGN §2): 3 headers + rows 2/2/1, no footer
        assertEquals(8, lines.size());
        assertEquals(3, lines.stream().filter(l -> l.contains("Details!")).count());
        // EM owns the whole box: nothing renders at all
        assertTrue(LineBuilder.buildSections(List.of(
                section(t, CombatStats.Window.DAMAGE_DONE)), A, 60_000, 6,
                MeterMath.meterBudget(15)).isEmpty());
    }

    @Test
    void noDataNoSectionsNoBox() {
        SegmentTracker t = new SegmentTracker(); // nothing recorded
        assertTrue(LineBuilder.buildSections(List.of(
                section(t, CombatStats.Window.DAMAGE_DONE),
                section(t, CombatStats.Window.HEALING_DONE)), A, 0, 6, 15).isEmpty());
        assertTrue(LineBuilder.buildSections(List.of(), A, 0, 6, 15).isEmpty());
        SegmentTracker live = threeWindowTracker(2);
        assertTrue(LineBuilder.buildSections(List.of(
                section(live, CombatStats.Window.DAMAGE_DONE)), A, 0, 6, 0).isEmpty());
    }

    @Test
    void ownRowRetainedPerSection() {
        SegmentTracker t = new SegmentTracker();
        UUID low = UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            t.recordHealing(UUID.randomUUID(), "Healer" + i, 100 - i * 10, T0);
        }
        t.recordHealing(low, "Rylie", 5, T0); // rank 6 in healing
        List<String> lines = LineBuilder.buildSections(List.of(
                section(t, CombatStats.Window.HEALING_DONE)), low, 60_000, 2, 15);
        assertTrue(lines.stream().anyMatch(l -> l.contains("Rylie")), String.join(" | ", lines));
    }

    // ---------- 0.2.0: churn fix (micro segments never announce) ----------

    @Test
    void zeroEventSegmentNeverClosesOrAnnounces() {
        SegmentTracker t = new SegmentTracker();
        assertFalse(t.tick(T0 + 999_999, 8_000)); // idle: nothing to close
        assertFalse(t.lastCloseAnnounceable());
    }

    @Test
    void microSegmentMergesSilently() {
        SegmentTracker t = new SegmentTracker();
        t.recordHealing(A, "Mike", 1.0, T0); // one lone regen tick, then quiet
        assertEquals(1, t.segmentEventCount());
        assertTrue(t.tick(T0 + 8_000, 8_000));
        assertFalse(t.lastCloseAnnounceable());              // no console noise
        assertEquals(1.0, t.overallStats(A).healingDone(), 1e-9); // still counted
        assertEquals(1, t.ranked(CombatStats.Window.HEALING_DONE).size()); // still archived
    }

    @Test
    void realFightAnnounces() {
        SegmentTracker t = new SegmentTracker();
        t.recordDamageDone(A, "Mike", 10, "Zombie", T0);
        t.recordDamageDone(A, "Mike", 10, "Zombie", T0 + 2_000);
        assertTrue(t.tick(T0 + 10_000, 8_000));
        assertTrue(t.lastCloseAnnounceable());
    }

    @Test
    void postClearEmptyCloseIsSilent() {
        SegmentTracker t = new SegmentTracker();
        t.recordDamageDone(A, "Mike", 10, "Zombie", T0);
        t.recordDamageDone(A, "Mike", 10, "Zombie", T0 + 1_000);
        t.clearCurrent(T0 + 2_000); // restarts an EMPTY segment
        assertEquals(0, t.segmentEventCount());
        assertTrue(t.tick(T0 + 10_000, 8_000)); // the empty segment closes...
        assertFalse(t.lastCloseAnnounceable()); // ...and never announces
        assertEquals(0.0, t.overallStats(A).damageDone(), 1e-9);
    }

    // ---------- 0.2.0: persistence (prefs + mix + overall totals) ----------

    @Test
    void persistenceRoundTripSurvivesARestart(@TempDir Path dir) throws Exception {
        PlayerPrefs prefs = new PlayerPrefs();
        SegmentTracker tracker = new SegmentTracker();
        prefs.setEnabled(A, false);
        prefs.showOnly(A, CombatStats.Window.DAMAGE_TAKEN);
        prefs.setShown(B, CombatStats.Window.HEALING_DONE, true); // B: damage + healing
        tracker.recordDamageDone(A, "Mike", 120.5, "Warden", T0);
        tracker.recordDamageTaken(A, "Mike", 33.25, "Warden", T0 + 500);
        tracker.recordHealing(B, "Kay", 42.0, T0 + 700);
        assertTrue(tracker.tick(T0 + 9_000, 8_000)); // close -> merge into overall

        File file = dir.resolve("data.yml").toFile();
        DataFile.save(file, prefs, tracker);

        PlayerPrefs prefs2 = new PlayerPrefs();
        SegmentTracker tracker2 = new SegmentTracker();
        DataFile.load(file, prefs2, tracker2);

        assertFalse(prefs2.isEnabled(A));
        assertEquals(EnumSet.of(CombatStats.Window.DAMAGE_TAKEN), prefs2.shownWindows(A));
        assertEquals(CombatStats.Window.DAMAGE_TAKEN, prefs2.window(A));
        assertEquals(EnumSet.of(CombatStats.Window.DAMAGE_DONE, CombatStats.Window.HEALING_DONE),
                prefs2.shownWindows(B));
        CombatStats a = tracker2.overallStats(A);
        assertEquals(120.5, a.damageDone(), 1e-9);
        assertEquals(33.25, a.damageTaken(), 1e-9);
        assertEquals(42.0, tracker2.overallStats(B).healingDone(), 1e-9);
        assertEquals("Mike", tracker2.nameOf(A));
        assertFalse(tracker2.isActive()); // totals restored, no phantom segment
    }

    @Test
    void legacyWindowKeyMigratesToTheMix(@TempDir Path dir) throws Exception {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("exceldetails-build", "EXC-DETAILS-0.1.0-WRV-20261109-01-a47f0c");
        yml.set("players." + A + ".enabled", true);
        yml.set("players." + A + ".window", "DAMAGE_TAKEN");
        File file = dir.resolve("data.yml").toFile();
        yml.save(file);

        PlayerPrefs prefs = new PlayerPrefs();
        DataFile.load(file, prefs, new SegmentTracker());
        assertEquals(EnumSet.of(CombatStats.Window.DAMAGE_TAKEN), prefs.shownWindows(A));
        assertEquals(CombatStats.Window.DAMAGE_TAKEN, prefs.window(A));
        assertTrue(prefs.isEnabled(A));
    }

    // ---------- 0.2.1: party scoping (display-only filter) ----------

    private static final UUID C = UUID.randomUUID(); // the stranger

    /** Mike 100 done, Kay (party) 50 done, stranger 900 done; all three also heal. */
    private static SegmentTracker scopedTracker() {
        SegmentTracker t = new SegmentTracker();
        t.recordDamageDone(A, "Mike", 100, "Zombie", T0);
        t.recordDamageDone(B, "Kay", 50, "Zombie", T0);
        t.recordDamageDone(C, "Stranger", 900, "Zombie", T0);
        t.recordHealing(A, "Mike", 30, T0);
        t.recordHealing(B, "Kay", 70, T0);
        t.recordHealing(C, "Stranger", 900, T0);
        return t;
    }

    private static LineBuilder.Section scopedSection(SegmentTracker t, CombatStats.Window w,
                                                     UUID viewer, java.util.Set<UUID> scope) {
        List<SegmentTracker.Row> rows = PartyScope.visibleRows(t.ranked(w), viewer, scope);
        return new LineBuilder.Section(w, rows, PartyScope.totalOf(rows));
    }

    @Test
    void scopeSoloShowsOnlyOwnRows() {
        SegmentTracker t = scopedTracker();
        // Solo: scope is the viewer alone (both seams empty).
        java.util.Set<UUID> solo = PartyScope.scopeOf(A, Set.of(), Set.of());
        List<SegmentTracker.Row> rows = PartyScope.visibleRows(
                t.ranked(CombatStats.Window.DAMAGE_DONE), A, solo);
        assertEquals(1, rows.size());
        assertEquals("Mike", rows.get(0).name());
        assertEquals(100.0, PartyScope.totalOf(rows), 1e-9);
    }

    @Test
    void scopePartyMemberVisibleNonPartyFiltered() {
        SegmentTracker t = scopedTracker();
        java.util.Set<UUID> scope = PartyScope.scopeOf(A, Set.of(A, B), Set.of());
        List<SegmentTracker.Row> rows = PartyScope.visibleRows(
                t.ranked(CombatStats.Window.DAMAGE_DONE), A, scope);
        assertEquals(2, rows.size());
        assertEquals("Mike", rows.get(0).name()); // ranked order preserved
        assertEquals("Kay", rows.get(1).name());
        assertTrue(rows.stream().noneMatch(r -> r.id().equals(C)));
    }

    @Test
    void scopeIsTheUnionOfBothPartySystems() {
        // Together says {A,B}; EliteMobs says {A,C-as-member}: union wins,
        // and null member sets (a seam down) never shrink below the viewer.
        UUID d = UUID.randomUUID();
        java.util.Set<UUID> scope = PartyScope.scopeOf(A, Set.of(A, B), Set.of(A, d));
        assertEquals(Set.of(A, B, d), scope);
        assertEquals(Set.of(A), PartyScope.scopeOf(A, null, null));
        assertEquals(Set.of(A, B), PartyScope.scopeOf(A, Set.of(B), null));
    }

    @Test
    void scopeFloorIsSelfOnlyWhenScopeMissing() {
        SegmentTracker t = scopedTracker();
        // Fail-soft floor: a null scope (or one missing the viewer)
        // still shows the viewer's own row and nobody else's.
        List<SegmentTracker.Row> nullScope = PartyScope.visibleRows(
                t.ranked(CombatStats.Window.DAMAGE_DONE), A, null);
        assertEquals(1, nullScope.size());
        assertEquals(A, nullScope.get(0).id());
        // A scope set is additive visibility ON TOP of the own-row
        // guarantee: {B} shows Mike (own) + Kay (in scope), never the
        // stranger — and a scope missing the viewer never hides him.
        List<SegmentTracker.Row> foreignScope = PartyScope.visibleRows(
                t.ranked(CombatStats.Window.DAMAGE_DONE), A, Set.of(B));
        assertEquals(2, foreignScope.size());
        assertTrue(foreignScope.stream().anyMatch(r -> r.id().equals(A)));
        assertTrue(foreignScope.stream().noneMatch(r -> r.id().equals(C)));
    }

    @Test
    void sectionTotalRecomputedOnFilteredRows() {
        SegmentTracker t = scopedTracker();
        java.util.Set<UUID> scope = PartyScope.scopeOf(A, Set.of(A, B), Set.of());
        // Healing: Mike 30, Kay 70, stranger 900. Filtered total is 100,
        // so Mike's share renders 30% — within-group, not 3% of server.
        LineBuilder.Section s = scopedSection(t, CombatStats.Window.HEALING_DONE, A, scope);
        assertEquals(100.0, s.total(), 1e-9);
        List<String> lines = LineBuilder.buildSections(List.of(s), A, 10_000, 6, 15);
        assertTrue(lines.stream().anyMatch(l -> l.contains("Mike") && l.contains("(30%)")),
                String.join(" | ", lines));
        assertTrue(lines.stream().noneMatch(l -> l.contains("Stranger")),
                String.join(" | ", lines));
    }

    @Test
    void scopedViewerWithNoInScopeRowsGetsNoBox() {
        SegmentTracker t = new SegmentTracker();
        t.recordDamageDone(C, "Stranger", 500, "Zombie", T0); // only the stranger fought
        java.util.Set<UUID> solo = PartyScope.scopeOf(A, Set.of(), Set.of());
        List<String> lines = LineBuilder.buildSections(List.of(
                scopedSection(t, CombatStats.Window.DAMAGE_DONE, A, solo)), A, 5_000, 6, 15);
        assertTrue(lines.isEmpty()); // existing no-data behavior, now per-viewer
    }

    @Test
    void scopedOwnRowStillPinnedUnderCut() {
        SegmentTracker t = new SegmentTracker();
        java.util.Set<UUID> party = new java.util.HashSet<>();
        party.add(A);
        for (int i = 0; i < 5; i++) {
            UUID id = UUID.randomUUID();
            party.add(id);
            t.recordHealing(id, "Mate" + i, 100 - i * 10, T0);
        }
        t.recordHealing(A, "Mike", 5, T0); // viewer is rank 6 inside his own party
        t.recordHealing(C, "Stranger", 999, T0); // out-of-party: must not appear or displace
        List<String> lines = LineBuilder.buildSections(List.of(
                scopedSection(t, CombatStats.Window.HEALING_DONE, A,
                        PartyScope.scopeOf(A, party, Set.of()))), A, 60_000, 2, 15);
        assertTrue(lines.stream().anyMatch(l -> l.contains("Mike")), String.join(" | ", lines));
        assertTrue(lines.stream().noneMatch(l -> l.contains("Stranger")), String.join(" | ", lines));
    }

    @Test
    void overallSnapshotAndRestoreAreIsolated() {
        SegmentTracker t = new SegmentTracker();
        t.recordDamageDone(A, "Mike", 10, "Zombie", T0);
        t.tick(T0 + 8_000, 8_000);
        var snap = t.overallSnapshot();
        snap.get(A).addDamageDone(999, "Zombie"); // mutating the snapshot...
        assertEquals(10.0, t.overallStats(A).damageDone(), 1e-9); // ...never leaks back
        SegmentTracker t2 = new SegmentTracker();
        t2.restoreOverall(t.overallSnapshot(), t.overallNames());
        assertEquals(10.0, t2.overallStats(A).damageDone(), 1e-9);
        assertEquals("Mike", t2.nameOf(A));
    }
}
