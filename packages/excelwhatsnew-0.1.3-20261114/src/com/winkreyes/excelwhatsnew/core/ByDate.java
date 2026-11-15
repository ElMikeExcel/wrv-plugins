package com.winkreyes.excelwhatsnew.core;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * "By date" grouping (0.1.2). Groups the loaded changelog entries (newest first)
 * into months, newest first, then days, newest first, from each entry's posted
 * date. Pure logic so the tests can check the grouping without a server.
 * 0.1.3 (Mike, Nov 14, 2026): entries INSIDE a day are oldest first, so a day
 * tap opens the day's oldest entry and the day walk runs oldest to newest.
 * Months and days keep their newest-first order; the flat list is untouched.
 *
 * Date rule (design D2/D8): an entry's date is its posted date. If posted is
 * missing or unparseable, the YYYY-MM-DD prefix of its id is used instead.
 * If neither parses, the entry is left out of the by-date views only (it stays
 * in the flat list) and a warning is recorded. If posted disagrees with the id
 * prefix, posted wins and a warning is recorded.
 */
public final class ByDate {
    private ByDate() { }

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    /** One day that has entries. Entries are OLDEST first (0.1.3 day walk order). */
    public static final class Day {
        public final LocalDate date;
        public final String label; // e.g. "Fri, Nov 13"
        public final List<Entry> entries;
        Day(LocalDate d, List<Entry> e) { date = d; entries = List.copyOf(e); label = dayLabel(d); }
        public int count() { return entries.size(); }
        /** "1 change" or "N changes". */
        public String countLabel() { return ByDate.countLabel(entries.size()); }
    }

    /** One month that has entries. Days are newest first. */
    public static final class Month {
        public final YearMonth key;
        public final String label; // e.g. "November 2026"
        public final List<Day> days;
        Month(YearMonth k, List<Day> d) { key = k; days = List.copyOf(d); label = monthLabel(k); }
        public int count() { int n = 0; for (Day d : days) n += d.count(); return n; }
        /** "1 change" or "N changes". */
        public String countLabel() { return ByDate.countLabel(count()); }
        public Day day(LocalDate d) { for (Day x : days) if (x.date.equals(d)) return x; return null; }
    }

    /** The whole grouping: months newest first, plus load warnings. */
    public static final class Grouping {
        public final List<Month> months;
        public final List<String> warnings;
        Grouping(List<Month> m, List<String> w) { months = List.copyOf(m); warnings = List.copyOf(w); }
        public Month month(YearMonth k) { for (Month m : months) if (m.key.equals(k)) return m; return null; }
        public Day day(LocalDate d) {
            Month m = month(YearMonth.from(d));
            return m == null ? null : m.day(d);
        }
        public boolean isEmpty() { return months.isEmpty(); }
    }

    /** "Fri, Nov 13" — weekday first (supervisor decision D41-5). */
    public static String dayLabel(LocalDate d) { return d.format(DAY_FMT); }

    /** "November 2026". */
    public static String monthLabel(YearMonth m) { return m.format(MONTH_FMT); }

    /** "1 change" or "N changes". */
    public static String countLabel(int n) { return n + (n == 1 ? " change" : " changes"); }

    /** The date an entry groups under: posted, else its id's YYYY-MM-DD prefix, else null. */
    public static LocalDate dateOf(Entry e) {
        if (e.posted != null) return e.posted;
        return idPrefixDate(e.id);
    }

    /** Parse a leading YYYY-MM-DD from an entry id, or null. */
    public static LocalDate idPrefixDate(String id) {
        if (id == null || id.length() < 10) return null;
        try { return LocalDate.parse(id.substring(0, 10)); } catch (Exception ex) { return null; }
    }

    public static Grouping group(List<Entry> entries) {
        List<String> warn = new ArrayList<>();
        Map<YearMonth, Map<LocalDate, List<Entry>>> byMonth = new LinkedHashMap<>();
        for (Entry e : entries) {
            LocalDate d = e.posted;
            if (d != null) {
                LocalDate prefix = idPrefixDate(e.id);
                if (prefix != null && !prefix.equals(d))
                    warn.add(e.id + ": posted " + d + " disagrees with its id date " + prefix + " (posted wins)");
            } else {
                d = idPrefixDate(e.id);
                if (d == null) {
                    warn.add(e.id + " has no usable date; left out of By date");
                    continue;
                }
            }
            byMonth.computeIfAbsent(YearMonth.from(d), k -> new LinkedHashMap<>())
                    .computeIfAbsent(d, k -> new ArrayList<>()).add(e);
        }
        List<YearMonth> keys = new ArrayList<>(byMonth.keySet());
        keys.sort((a, b) -> b.compareTo(a)); // newest month first
        List<Month> months = new ArrayList<>();
        for (YearMonth k : keys) {
            Map<LocalDate, List<Entry>> days = byMonth.get(k);
            List<LocalDate> dkeys = new ArrayList<>(days.keySet());
            dkeys.sort((a, b) -> b.compareTo(a)); // newest day first
            List<Day> dl = new ArrayList<>();
            for (LocalDate d : dkeys) {
                // Entries arrive newest first (file order); the day walk is
                // oldest first (0.1.3), so the day's list is reversed here.
                List<Entry> walk = new ArrayList<>(days.get(d));
                Collections.reverse(walk);
                dl.add(new Day(d, walk));
            }
            months.add(new Month(k, dl));
        }
        return new Grouping(months, warn);
    }
}
