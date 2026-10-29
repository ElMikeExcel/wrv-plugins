/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.time.LocalDate;
import java.util.List;

/**
 * Provenance lore (0.2.5, spec 4.6): each named-gear copy is
 * stamped with its own earner's name and the date. Pins the
 * wording, the two-line shape, the per-earner line, and the
 * date format the adapter renders into item lore.
 */
public final class ProvenanceLoreTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    public static void main(String[] args) {
        LocalDate day = LocalDate.of(2026, 10, 23);

        // Single champion: two lines, earner first, date second.
        List<String> solo = ProvenanceLore.lines("PlayerA", day);
        check(solo.size() == 2, "provenance is exactly two lines");
        check(solo.get(0).equals("Earned by PlayerA"),
                "earner line names the champion");
        check(solo.get(1).equals("October 23, 2026"),
                "date line is the long US date");

        // Per-earner: another champion's copy names THEM, never
        // the party and never the first champion.
        List<String> other = ProvenanceLore.lines("PlayerB", day);
        check(other.get(0).equals("Earned by PlayerB"),
                "each copy names its own earner");
        check(!other.get(0).equals(solo.get(0)),
                "two champions' copies carry different earner lines");
        check(other.get(1).equals(solo.get(1)),
                "same kill, same date line on both copies");

        // Date format across the calendar (no zero-padding on
        // the day, full month name, four-digit year).
        check(ProvenanceLore.formatDate(LocalDate.of(2027, 1, 5))
                        .equals("January 5, 2027"),
                "date format: January 5, 2027");
        check(ProvenanceLore.formatDate(LocalDate.of(2026, 12, 25))
                        .equals("December 25, 2026"),
                "date format: December 25, 2026");

        // Player words only: no internal vocabulary leaks into
        // the stamp (no "seal", "credit", "verdict", "UUID").
        String joined = String.join(" ", solo).toLowerCase(java.util.Locale.US);
        check(!joined.contains("seal") && !joined.contains("credit")
                        && !joined.contains("verdict") && !joined.contains("uuid"),
                "provenance lines are player words, no internal terms");

        // Degenerate names degrade to a generic earner line,
        // never a blank or "null" stamp.
        check(ProvenanceLore.lines(null, day).get(0).equals("Earned by a champion"),
                "null name degrades to 'a champion'");
        check(ProvenanceLore.lines("  ", day).get(0).equals("Earned by a champion"),
                "blank name degrades to 'a champion'");

        System.out.println("ProvenanceLoreTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

