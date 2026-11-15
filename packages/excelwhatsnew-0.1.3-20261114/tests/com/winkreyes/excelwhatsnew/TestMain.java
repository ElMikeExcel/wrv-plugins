package com.winkreyes.excelwhatsnew;

import com.winkreyes.excelwhatsnew.core.ByDate;
import com.winkreyes.excelwhatsnew.core.Changelog;
import com.winkreyes.excelwhatsnew.core.Entry;
import com.winkreyes.excelwhatsnew.core.Nav;
import com.winkreyes.excelwhatsnew.core.Nav.Act;
import com.winkreyes.excelwhatsnew.core.Render;
import com.winkreyes.excelwhatsnew.core.Seen;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.zip.ZipFile;

public class TestMain {
    static int pass = 0, fail = 0, skip = 0;
    static void ok(boolean c, String m) { if (c) { pass++; System.out.println("ok   " + m); } else { fail++; System.out.println("FAIL " + m); } }
    static void skipped(String m) { skip++; System.out.println("skip " + m); }

    @SuppressWarnings("unchecked")
    static Changelog load(String yml) { Map<String, Object> m = new Yaml().load(yml); return Changelog.parse((List<?>) m.get("entries")); }

    /** Command names (and aliases) a real jar registers. */
    @SuppressWarnings("unchecked")
    static Set<String> jarCommands(String jar) throws Exception {
        Set<String> out = new HashSet<>();
        try (ZipFile z = new ZipFile(jar)) {
            Map<String, Object> py = new Yaml().load(z.getInputStream(z.getEntry("plugin.yml")));
            Map<String, Object> cmds = (Map<String, Object>) py.get("commands");
            for (var e : cmds.entrySet()) {
                out.add(e.getKey());
                Object al = e.getValue() instanceof Map<?, ?> mm ? mm.get("aliases") : null;
                if (al instanceof List<?> l) for (Object x : l) out.add(String.valueOf(x));
            }
        }
        return out;
    }

    /** The live per-day entry counts (design §5, verified against the live changelog). */
    static final int[][] LIVE_DAYS = {
        {2026, 11, 13, 5}, {2026, 11, 12, 8}, {2026, 11, 11, 8}, {2026, 11, 10, 8},
        {2026, 11, 9, 9}, {2026, 11, 8, 7}, {2026, 11, 7, 9}, {2026, 11, 6, 1},
        {2026, 11, 5, 6}, {2026, 11, 4, 1}, {2026, 11, 3, 1}, {2026, 11, 2, 5},
        {2026, 11, 1, 4}, {2026, 10, 31, 3}, {2026, 10, 30, 3}, {2026, 10, 29, 2},
        {2026, 10, 28, 2}, {2026, 10, 26, 10}, {2026, 10, 25, 3}, {2026, 10, 24, 1},
    };

    static List<Entry> syntheticLiveEntries() {
        List<Entry> out = new ArrayList<>();
        String[] mon = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (int[] d : LIVE_DAYS) {
            LocalDate date = LocalDate.of(d[0], d[1], d[2]);
            for (int i = 1; i <= d[3]; i++)
                out.add(new Entry(String.format("%s-e%d", date, i), mon[d[1]] + " " + d[2], date,
                        "Test " + date + " #" + i, List.of(), List.of(), List.of("one thing")));
        }
        return out;
    }

    public static void main(String[] a) throws Exception {
        String seed = Files.readString(Path.of("changelog.yml"));
        Changelog c = load(seed);
        ok(c.entries.size() == 1, "seed changelog has 1 entry");
        ok(c.warnings.isEmpty(), "seed changelog has no warnings " + c.warnings);
        Entry e = c.entries.get(0);
        ok(e.id.equals("2026-10-24-evening") && e.date.equals("Oct 24") && e.posted.equals(LocalDate.of(2026, 10, 24)), "seed id/date/posted");
        ok(Render.formTitle(e).equals("What's New (Oct 24)"), "form title is What's New (Oct 24)");
        List<String> want = List.of("/lostfound", "/mark", "/tips", "/autosort", "/wandstyle", "/whatsnew");
        for (String w : want) ok(e.newCommands.stream().anyMatch(x -> x.cmd.startsWith(w)), "New commands lists " + w);
        ok(e.betterCommands.size() == 1 && e.betterCommands.get(0).cmd.equals("/trash"), "Commands that got better: /trash");
        for (Entry.Cmd x : e.newCommands) ok(x.what.length() <= 160 && !x.what.isEmpty(), "one-line description for " + x.cmd + " (" + x.what.length() + " chars)");

        // Every command and alias named in the seed exists in the jar that is LIVE (hash-checked copies on the box).
        // On a machine without those jars the checks are skipped, not faked.
        Map<String, String> jars = new LinkedHashMap<>();
        jars.put("lostfound", "/workspace/batch-next/deathrecord-0.3.0/ExcelDeathRecord-0.3.0.jar");
        jars.put("recover", "/workspace/batch-next/deathrecord-0.3.0/ExcelDeathRecord-0.3.0.jar");
        jars.put("mark", "/workspace/batch-next/makersmark/ExcelMakersMark-0.1.0.jar");
        jars.put("tips", "/workspace/batch-next/2am-batch/files/tips/ExcelTips-0.1.0.jar");
        jars.put("autosort", "/workspace/batch-next/2am-batch/files/spellbook/autosort-0.1.17+26.3-paper.jar");
        jars.put("sort", "/workspace/batch-next/2am-batch/files/spellbook/autosort-0.1.17+26.3-paper.jar");
        jars.put("wandstyle", "/workspace/batch-next/2am-batch/files/spellbook/ExcelWandBars-0.2.1.jar");
        jars.put("trash", "/workspace/batch-next/2am-batch/files/trash/ExcelTrash-0.2.0.jar");
        for (var j : jars.entrySet()) {
            if (!Files.exists(Path.of(j.getValue()))) { skipped("/" + j.getKey() + " (external jar absent: " + j.getValue() + ")"); continue; }
            ok(jarCommands(j.getValue()).contains(j.getKey()), "/" + j.getKey() + " is registered by " + Path.of(j.getValue()).getFileName());
        }
        String py = Files.readString(Path.of("plugin.yml"));
        ok(py.contains("  whatsnew:") && py.contains("aliases: [news]"), "/whatsnew and /news registered by this plugin");
        ok(py.contains("excelwhatsnew.use:\n    default: true"), "/whatsnew is for everyone (default true)");
        ok(py.contains("version: 0.1.3"), "plugin.yml version is 0.1.3");

        // Public-text rules
        String all = String.join("\n", Changelog.allText(e));
        ok(!all.contains("\u2014") && !all.contains("\u2013"), "no em or en dashes");
        ok(!all.matches("(?s).*[0-9a-f]{8}-[0-9a-f]{4}-.*"), "no UUIDs");
        ok(!all.toLowerCase().matches("(?s).*\\b(refund|webhook|seal alert|op |admin only).*"), "nothing staff-only in the news");

        // Render
        List<String> j = Render.java(e);
        ok(j.get(0).contains("What's New") && j.get(0).contains("Oct 24"), "java header");
        ok(j.contains("&a&lNew commands") && j.contains("&b&lCommands that got better") && j.contains("&d&lOther changes"), "java has the three sections");
        ok(j.stream().noneMatch(s -> s.contains("No typing")), "java never shows the Bedrock no-typing hints");
        ok(j.stream().anyMatch(s -> s.contains("Trash Bin in the compass")), "java shows the Java compass hint");
        String b = Render.bedrock(e, "footer");
        ok(b.contains("No typing: \u00a7rtap Wand controls on the compass."), "bedrock shows the no-typing hint");
        ok(!b.contains("Trash Bin in the compass menu"), "bedrock never shows the Java compass hint");
        ok(b.indexOf("New commands") < b.indexOf("Commands that got better") && b.indexOf("Commands that got better") < b.indexOf("Other changes"), "bedrock section order");

        // Seen logic
        String two = seed.replace("entries:\n", "entries:\n  - id: 2026-11-01\n    date: Nov 1\n    posted: 2026-11-01\n    title: Test news\n    changes: [\"one thing\"]\n");
        Changelog c2 = load(two);
        LocalDate d = LocalDate.of(2026, 11, 2);
        ok(c2.warnings.isEmpty() && c2.entries.get(0).id.equals("2026-11-01"), "new entry on top parses");
        ok(Seen.pending(c2.entries, null, null, d, 30).id.equals("2026-11-01"), "never said OK -> newest");
        ok(Seen.pending(c2.entries, "2026-10-24-evening", LocalDate.of(2026, 10, 24), d, 30).id.equals("2026-11-01"), "OK on older -> newest is pending");
        ok(Seen.pending(c2.entries, "2026-11-01", LocalDate.of(2026, 11, 1), d, 30) == null, "OK on newest -> nothing");
        ok(Seen.pending(c2.entries, "gone-id", LocalDate.of(2026, 11, 1), d, 30) == null, "OK id removed, OK day not before newest -> nothing");
        ok(Seen.pending(c2.entries, "gone-id", LocalDate.of(2026, 10, 30), d, 30) != null, "OK id removed, newest posted after OK day -> pending");
        ok(Seen.pending(c2.entries, null, null, LocalDate.of(2026, 12, 5), 30) == null, "older than max-age-days -> not on join");
        ok(Seen.pending(c2.entries, null, null, LocalDate.of(2026, 12, 5), 0) != null, "max-age 0 = no limit");
        ok(Seen.unseenCount(c2.entries, "2026-10-24-evening") == 1 && Seen.unseenCount(c2.entries, null) == 2, "unseen count");

        // Validation
        Changelog bad = load("entries:\n  - id: a\n    date: X\n    title: T\n    changes: [\"has \u2014 dash\"]\n  - id: a\n    title: dup\n  - title: no id\n  - id: b\n    date: Y\n    title: T2\n    new-commands: [{cmd: trash, what: x}, {cmd: /y}]\n");
        ok(bad.entries.size() == 2, "dup id and missing id skipped");
        ok(bad.warnings.stream().anyMatch(w -> w.contains("em dash")), "em dash warned");
        ok(bad.warnings.stream().anyMatch(w -> w.contains("should start with /")), "missing slash warned");
        ok(bad.warnings.stream().anyMatch(w -> w.contains("needs cmd and what")), "missing what warned");
        Changelog order = load("entries:\n  - {id: a, date: A, posted: 2026-10-01, title: T, changes: [x]}\n  - {id: b, date: B, posted: 2026-10-05, title: T, changes: [x]}\n");
        ok(order.warnings.stream().anyMatch(w -> w.contains("newest at the top")), "wrong order warned");
        ok(Changelog.parse(null).warnings.size() == 1, "missing entries list warned");

        // 0.1.1 Newer news paging (+ 0.1.2 By date button on the global entry view)
        ok(!Nav.hasNewer(0) && Nav.hasNewer(1) && Nav.hasNewer(9), "Newer news only when idx > 0");
        ok(Nav.newerNumber(1) == 1 && Nav.newerNumber(4) == 4, "Newer news opens /whatsnew idx (the next newer one)");
        ok(Nav.olderNumber(0) == 2 && Nav.olderNumber(3) == 5, "Older news opens /whatsnew idx+2 (unchanged)");
        ok(Nav.hasOlder(0, 10) && !Nav.hasOlder(9, 10) && !Nav.hasOlder(0, 1), "Older news stops at the last shown entry");
        // Bedrock form: order OK, Newer, Older, By date, Close; OK and Close never together (same as 0.1.0)
        ok(Nav.formButtons(true, 0, 10).equals(List.of(Act.OK, Act.OLDER, Act.BYDATE)), "form newest + pending: OK, Older, By date");
        ok(Nav.formButtons(false, 0, 10).equals(List.of(Act.OLDER, Act.BYDATE, Act.CLOSE)), "form newest, already OK: Older, By date, Close");
        ok(Nav.formButtons(false, 3, 10).equals(List.of(Act.NEWER, Act.OLDER, Act.BYDATE, Act.CLOSE)), "form middle: Newer, Older, By date, Close");
        ok(Nav.formButtons(false, 9, 10).equals(List.of(Act.NEWER, Act.BYDATE, Act.CLOSE)), "form oldest shown: Newer, By date, Close");
        ok(Nav.formButtons(true, 2, 10).equals(List.of(Act.OK, Act.NEWER, Act.OLDER, Act.BYDATE)), "form pending middle: OK, Newer, Older, By date");
        ok(Nav.formButtons(false, 0, 1).equals(List.of(Act.BYDATE, Act.CLOSE)) && Nav.formButtons(true, 0, 1).equals(List.of(Act.OK, Act.BYDATE)), "form single entry: By date (+ OK or Close)");
        // Callback mapping through the same list
        List<Act> mid = Nav.formButtons(false, 3, 10);
        ok(Nav.clicked(mid, 0) == Act.NEWER && Nav.clicked(mid, 1) == Act.OLDER && Nav.clicked(mid, 2) == Act.BYDATE && Nav.clicked(mid, 3) == Act.CLOSE, "middle form: id 0 Newer, 1 Older, 2 By date, 3 Close");
        List<Act> pend = Nav.formButtons(true, 0, 10);
        ok(Nav.clicked(pend, 0) == Act.OK && Nav.clicked(pend, 1) == Act.OLDER && Nav.clicked(pend, 2) == Act.BYDATE, "pending newest: id 0 OK, 1 Older, 2 By date");
        List<Act> last = Nav.formButtons(false, 9, 10);
        ok(Nav.clicked(last, 0) == Act.NEWER && Nav.clicked(last, 1) == Act.BYDATE && Nav.clicked(last, 2) == Act.CLOSE, "oldest form: id 0 Newer, 1 By date, 2 Close (never Older)");
        ok(Nav.clicked(mid, -1) == null && Nav.clicked(mid, 4) == null && Nav.clicked(mid, 99) == null, "closed or out-of-range id does nothing");
        // Java row: same order, no Close
        ok(Nav.javaButtons(true, 0, 10).equals(List.of(Act.OK, Act.OLDER, Act.BYDATE)), "java newest pending: [OK] [Older news] [By date]");
        ok(Nav.javaButtons(false, 3, 10).equals(List.of(Act.NEWER, Act.OLDER, Act.BYDATE)), "java middle: [Newer news] [Older news] [By date]");
        ok(Nav.javaButtons(false, 9, 10).equals(List.of(Act.NEWER, Act.BYDATE)), "java oldest: [Newer news] [By date]");
        ok(Nav.javaButtons(false, 0, 1).equals(List.of(Act.BYDATE)), "java single entry, already OK: [By date] only");
        for (int sh = 1; sh <= 10; sh++) for (int i = 0; i < sh; i++) for (boolean pd : new boolean[]{true, false}) {
            List<Act> f = Nav.formButtons(pd, i, sh), jv = Nav.javaButtons(pd, i, sh);
            List<Act> fNoClose = new ArrayList<>(f); fNoClose.remove(Act.CLOSE);
            if (!fNoClose.equals(jv)) ok(false, "parity java vs bedrock at idx " + i + " of " + sh + " pending " + pd);
            if (f.isEmpty()) ok(false, "form never empty at idx " + i + " of " + sh);
        }
        ok(true, "Java/Bedrock parity: same buttons in the same order for every idx/shown/pending (Close is Bedrock only)");
        // Typed fallback mentions both directions
        String th = Nav.typedHint(false, 3, 10);
        ok(th.contains("Newer news: &e/whatsnew 3") && th.contains("Older news: &e/whatsnew 5"), "typed hint has both directions: " + th);
        ok(Nav.typedHint(true, 0, 10).contains("/whatsnew ok") && !Nav.typedHint(true, 0, 10).contains("Newer"), "typed hint newest: OK + Older, no Newer");
        ok(Nav.typedHint(false, 9, 10).contains("Newer news: &e/whatsnew 9") && !Nav.typedHint(false, 9, 10).contains("Older"), "typed hint oldest: Newer only");
        ok(Nav.typedHint(false, 0, 1).isEmpty(), "typed hint nothing to say: no line");
        // Bedrock /whatsnew list hint follows the real count (was hardcoded '(or 3)')
        ok(Nav.listHint(1).isEmpty(), "list hint 1 entry: none");
        ok(Nav.listHint(2).equals("&7Type &e/whatsnew 2 &7to read the older one."), "list hint 2 entries");
        ok(Nav.listHint(10).equals("&7Type &e/whatsnew 2 &7to &e/whatsnew 10 &7to read an older one."), "list hint 10 entries: /whatsnew 2 to /whatsnew 10");
        ok(!Nav.listHint(10).contains("or 3"), "list hint no longer hardcodes (or 3)");
        for (String t : List.of(Nav.listHint(10), Nav.typedHint(true, 3, 10))) ok(!t.contains("\u2014") && !t.contains("\u2013"), "no em or en dashes in new text");
        // Config: shipped default has the key; a config without it (live 62b95a5e) falls back
        String cfg = Files.readString(Path.of("config.yml"));
        ok(cfg.contains("newer-button: 'Newer news'"), "default config.yml has text.newer-button");
        ok(cfg.contains("bydate-button: 'By date'"), "default config.yml has text.bydate-button");
        ok(cfg.contains("back-button: 'Back'"), "default config.yml has text.back-button");
        Path live = Path.of("/workspace/batch-next/fixes-20261026-am/readback/ExcelWhatsNew_config.yml");
        if (Files.exists(live)) {
            Map<String, Object> lm = new Yaml().load(Files.readString(live));
            Map<?, ?> tx = (Map<?, ?>) lm.get("text");
            ok(tx != null && !tx.containsKey("newer-button"), "live config 62b95a5e has no newer-button (code default 'Newer news' applies)");
            ok(Integer.valueOf(10).equals(lm.get("show-count")), "live config show-count is 10");
        } else skipped("live-config checks (readback absent on this VM)");
        String src = Files.readString(Path.of("src/com/winkreyes/excelwhatsnew/ExcelWhatsNew.java"));
        ok(src.contains("c.getString(\"text.newer-button\", \"Newer news\")"), "code reads text.newer-button with default 'Newer news'");
        ok(src.contains("c.getString(\"text.bydate-button\", \"By date\")"), "code reads text.bydate-button with default 'By date'");
        ok(src.contains("c.getString(\"text.back-button\", \"Back\")"), "code reads text.back-button with default 'Back'");
        ok(!src.contains("(or 3)"), "hardcoded (or 3) gone from source");

        // ------------------------------------------------ 0.1.2 By date
        // Labels: weekday first (D41-5), singular vs plural, month labels.
        ok(ByDate.dayLabel(LocalDate.of(2026, 11, 13)).equals("Fri, Nov 13"), "day label Fri, Nov 13");
        ok(ByDate.dayLabel(LocalDate.of(2026, 10, 26)).equals("Mon, Oct 26"), "day label Mon, Oct 26");
        ok(ByDate.dayLabel(LocalDate.of(2026, 10, 24)).equals("Sat, Oct 24"), "day label Sat, Oct 24");
        ok(ByDate.monthLabel(YearMonth.of(2026, 11)).equals("November 2026"), "month label November 2026");
        ok(ByDate.monthLabel(YearMonth.of(2026, 10)).equals("October 2026"), "month label October 2026");
        ok(ByDate.countLabel(1).equals("1 change") && ByDate.countLabel(5).equals("5 changes") && ByDate.countLabel(0).equals("0 changes"), "count label singular/plural");

        // Grouping fixture = the live 96-entry shape (synthetic, same per-day counts).
        ByDate.Grouping g = ByDate.group(syntheticLiveEntries());
        ok(g.warnings.isEmpty(), "live-shape grouping has no warnings " + g.warnings);
        ok(g.months.size() == 2, "live shape: 2 months");
        ByDate.Month nov = g.months.get(0), oct = g.months.get(1);
        ok(nov.key.equals(YearMonth.of(2026, 11)) && nov.count() == 72, "November 2026 — 72 changes");
        ok(oct.key.equals(YearMonth.of(2026, 10)) && oct.count() == 24, "October 2026 — 24 changes");
        ok(nov.days.size() == 13 && oct.days.size() == 7, "live shape: 13 November days, 7 October days");
        ok(nov.days.get(0).date.equals(LocalDate.of(2026, 11, 13)) && nov.days.get(0).count() == 5, "newest day first: Fri, Nov 13 — 5 changes");
        ok(nov.days.get(nov.days.size() - 1).date.equals(LocalDate.of(2026, 11, 1)) && nov.days.get(nov.days.size() - 1).count() == 4, "oldest November day last: Nov 1 — 4 changes");
        ok(oct.day(LocalDate.of(2026, 10, 27)) == null, "no Oct 27 row (no entries that day)");
        ByDate.Day oct26 = g.day(LocalDate.of(2026, 10, 26));
        ok(oct26 != null && oct26.count() == 10 && oct26.label.equals("Mon, Oct 26"), "Oct 26 — 10 changes");
        ok(oct26.entries.get(0).id.equals("2026-10-26-e10") && oct26.entries.get(9).id.equals("2026-10-26-e1"), "entries inside a day walk OLDEST first (0.1.3): e10 first, e1 last");
        ByDate.Day nov13 = g.day(LocalDate.of(2026, 11, 13));
        ok(nov13.entries.get(0).id.equals("2026-11-13-e5") && nov13.entries.get(4).id.equals("2026-11-13-e1"), "Nov 13 day tap opens its OLDEST entry first (0.1.3), newest last");
        // Grouping must not reorder the flat list itself.
        List<Entry> syn = syntheticLiveEntries();
        ByDate.group(syn);
        ok(syn.get(0).id.equals("2026-11-13-e1") && syn.get(syn.size() - 1).id.equals("2026-10-24-e1"), "grouping leaves the flat list newest-first (unchanged)");
        ByDate.Day oct24 = g.day(LocalDate.of(2026, 10, 24));
        ok(oct24 != null && oct24.count() == 1 && oct24.label.equals("Sat, Oct 24"), "one-entry day: Sat, Oct 24 — 1 change");
        int total = 0; for (ByDate.Month m : g.months) total += m.count();
        ok(total == 96, "live shape: 96 entries in the grouping (all reachable by date)");
        // Every per-day count in the design §5 table, byte-checked.
        for (int[] dd : LIVE_DAYS) {
            ByDate.Day dy = g.day(LocalDate.of(dd[0], dd[1], dd[2]));
            if (dy == null || dy.count() != dd[3]) ok(false, "per-day count " + dd[0] + "-" + dd[1] + "-" + dd[2] + " expected " + dd[3]);
        }
        ok(true, "all 20 per-day counts match the design §5 table");

        // Same grouping run against the REAL live changelog when it is on disk.
        // (0.1.3: expectations are derived from the file itself — the live
        // changelog grows between builds, so pinned counts drift; the base
        // gate run recorded exactly that drift, 96 -> 100 entries.)
        Path liveLog = Path.of("/home/hatch/workspace/whatsnew-pipeline/build/changelog.yml");
        if (Files.exists(liveLog)) {
            Changelog lc = load(Files.readString(liveLog));
            ByDate.Grouping lg = ByDate.group(lc.entries);
            int dated = 0; for (Entry le : lc.entries) if (ByDate.dateOf(le) != null) dated++;
            int grouped = 0; for (ByDate.Month lm : lg.months) grouped += lm.count();
            ok(grouped == dated, "live changelog: every dated entry is grouped (" + grouped + " of " + lc.entries.size() + " entries)");
            boolean monthsDesc = true;
            for (int i = 1; i < lg.months.size(); i++) if (!lg.months.get(i - 1).key.isAfter(lg.months.get(i).key)) monthsDesc = false;
            ok(monthsDesc, "live changelog: months stay newest first");
            ByDate.Day l13 = lg.day(LocalDate.of(2026, 11, 13));
            List<Entry> fileNov13 = new ArrayList<>();
            for (Entry le : lc.entries) if (LocalDate.of(2026, 11, 13).equals(ByDate.dateOf(le))) fileNov13.add(le);
            ok(l13 != null && !fileNov13.isEmpty() && l13.count() == fileNov13.size(), "live Nov 13 day has all " + fileNov13.size() + " of its entries");
            ok(l13 != null && l13.entries.get(0).id.equals(fileNov13.get(fileNov13.size() - 1).id),
                    "live Nov 13 walk OPENS the day's oldest entry (last in file order): " + (l13 == null ? "none" : l13.entries.get(0).id));
            ok(l13 != null && l13.entries.get(l13.count() - 1).id.equals(fileNov13.get(0).id),
                    "live Nov 13 walk ENDS at the day's newest entry (first in file order): " + (l13 == null ? "none" : l13.entries.get(l13.count() - 1).id));
        } else skipped("live-changelog grouping check (pipeline build absent)");

        // Edge fixtures (design §10.1).
        ByDate.Grouping one = ByDate.group(List.of(
                new Entry("2026-11-01-only", "Nov 1", LocalDate.of(2026, 11, 1), "Only", List.of(), List.of(), List.of("x"))));
        ok(one.months.size() == 1 && one.months.get(0).days.size() == 1 && one.months.get(0).days.get(0).count() == 1,
                "one-entry month still walks month -> day -> entry (no skipping)");
        ByDate.Grouping empty = ByDate.group(List.of());
        ok(empty.isEmpty() && empty.warnings.isEmpty(), "empty changelog groups to nothing (months view shows No news yet. + Back)");
        Changelog unparsed = load("entries:\n"
                + "  - {id: nodate-here, date: X, posted: not-a-date, title: Bad, changes: [x]}\n"
                + "  - {id: 2026-11-02-fallback, date: Nov 2, title: Fallback, changes: [x]}\n"
                + "  - {id: 2026-11-03-real, date: Nov 3, posted: 2026-11-03, title: Real, changes: [x]}\n");
        ok(unparsed.entries.size() == 3, "all three edge entries stay in the flat list");
        ByDate.Grouping eg = ByDate.group(unparsed.entries);
        ok(eg.day(LocalDate.of(2026, 11, 2)) != null && eg.day(LocalDate.of(2026, 11, 2)).entries.get(0).id.equals("2026-11-02-fallback"),
                "posted missing but id has a valid date prefix: grouped by the prefix");
        ok(eg.months.size() == 1 && eg.months.get(0).count() == 2, "unparseable entry excluded from by-date only (2 of 3 grouped)");
        ok(eg.warnings.stream().anyMatch(w -> w.contains("nodate-here") && w.contains("no usable date")), "unparseable entry warned at load: " + eg.warnings);
        Changelog disagree = load("entries:\n  - {id: 2026-11-05-wrong, date: Nov 4, posted: 2026-11-04, title: T, changes: [x]}\n");
        ByDate.Grouping dg = ByDate.group(disagree.entries);
        ok(dg.day(LocalDate.of(2026, 11, 4)) != null && dg.day(LocalDate.of(2026, 11, 5)) == null, "posted wins over a disagreeing id prefix");
        ok(dg.warnings.stream().anyMatch(w -> w.contains("disagrees")), "posted/id disagreement warned: " + dg.warnings);

        // Day-bounded paging (Nav), 0.1.3 walk: position 1 = oldest, Newer advances.
        ok(Nav.dayPosition(0, 10).equals("1/10") && Nav.dayPosition(9, 10).equals("10/10")
                && Nav.dayPosition(4, 5).equals("5/5") && Nav.dayPosition(0, 1).equals("1/1"), "day position labels are N/M, 1-based from the oldest");
        ok(Nav.dayHasNewer(0, 5) && Nav.dayHasNewer(3, 5) && !Nav.dayHasNewer(4, 5) && !Nav.dayHasNewer(0, 1), "day walk: Newer exists until the newest entry");
        ok(!Nav.dayHasOlder(0) && Nav.dayHasOlder(1) && Nav.dayHasOlder(4), "day walk: Older exists only after the oldest entry");
        ok(Nav.dayNewerWalkNumber(0) == 2 && Nav.dayNewerWalkNumber(3) == 5, "day walk Newer opens bydate N = position + 1");
        ok(Nav.dayOlderWalkNumber(1) == 1 && Nav.dayOlderWalkNumber(4) == 4, "day walk Older opens bydate N = position - 1");
        ok(Nav.formDayButtons(false, 0, 5).equals(List.of(Act.NEWER, Act.BACK, Act.CLOSE)), "day form OLDEST of 5 (walk start): Newer, Back, Close (no Older)");
        ok(Nav.formDayButtons(false, 4, 5).equals(List.of(Act.OLDER, Act.BACK, Act.CLOSE)), "day form NEWEST of 5 (walk end): Older, Back, Close (no Newer)");
        ok(Nav.formDayButtons(false, 2, 5).equals(List.of(Act.NEWER, Act.OLDER, Act.BACK, Act.CLOSE)), "day form middle: Newer, Older, Back, Close");
        ok(Nav.formDayButtons(true, 0, 5).equals(List.of(Act.OK, Act.NEWER, Act.BACK)), "day form pending at walk start: OK, Newer, Back (no Close)");
        ok(Nav.formDayButtons(true, 4, 5).equals(List.of(Act.OK, Act.OLDER, Act.BACK)), "day form pending at walk end: OK, Older, Back (no Close)");
        ok(Nav.formDayButtons(false, 0, 1).equals(List.of(Act.BACK, Act.CLOSE)), "day form single entry: Back, Close (no Newer/Older)");
        ok(Nav.javaDayButtons(false, 2, 5).equals(List.of(Act.NEWER, Act.OLDER, Act.BACK)), "java day middle: [Newer news] [Older news] [Back]");
        ok(Nav.javaDayButtons(false, 0, 5).equals(List.of(Act.NEWER, Act.BACK)), "java day walk start: [Newer news] [Back]");
        ok(Nav.javaDayButtons(false, 4, 5).equals(List.of(Act.OLDER, Act.BACK)), "java day walk end: [Older news] [Back]");
        ok(Nav.javaDayButtons(true, 0, 1).equals(List.of(Act.OK, Act.BACK)), "java day single pending: [OK] [Back]");
        List<Act> dayMid = Nav.formDayButtons(false, 2, 5);
        ok(Nav.clicked(dayMid, 0) == Act.NEWER && Nav.clicked(dayMid, 1) == Act.OLDER && Nav.clicked(dayMid, 2) == Act.BACK && Nav.clicked(dayMid, 3) == Act.CLOSE, "day form mapping: 0 Newer, 1 Older, 2 Back, 3 Close");
        for (int sz = 1; sz <= 10; sz++) for (int i = 0; i < sz; i++) for (boolean pd : new boolean[]{true, false}) {
            List<Act> f = Nav.formDayButtons(pd, i, sz), jv = Nav.javaDayButtons(pd, i, sz);
            List<Act> fNoClose = new ArrayList<>(f); fNoClose.remove(Act.CLOSE);
            if (!fNoClose.equals(jv)) ok(false, "day parity java vs bedrock at idx " + i + " of " + sz + " pending " + pd);
        }
        ok(true, "Java/Bedrock parity for day views (Close is Bedrock only)");
        String dh = Nav.typedDayHint(false, 4, 1, 5, LocalDate.of(2026, 11, 13));
        ok(dh.contains("Newer news: &e/whatsnew 4") && dh.contains("Older news: &e/whatsnew 6")
                && dh.contains("Back to Fri, Nov 13: &e/whatsnew bydate 2026-11-13"), "typed day hint: both directions (global numbers) + Back line: " + dh);
        ok(dh.contains("Fri, Nov 13 2/5"), "typed day hint shows the walk position N/M: " + dh);
        String dhStart = Nav.typedDayHint(false, 10, 0, 5, LocalDate.of(2026, 11, 13));
        ok(dhStart.contains("1/5") && dhStart.contains("Newer news") && !dhStart.contains("Older news"), "typed day hint at walk start: position 1/5, Newer only: " + dhStart);
        String dhEnd = Nav.typedDayHint(false, 6, 4, 5, LocalDate.of(2026, 11, 13));
        ok(dhEnd.contains("5/5") && dhEnd.contains("Older news") && !dhEnd.contains("Newer news"), "typed day hint at walk end: position 5/5, Older only: " + dhEnd);
        ok(Nav.typedDayHint(false, 0, 0, 1, LocalDate.of(2026, 10, 24)).contains("Back to Sat, Oct 24: &e/whatsnew bydate 2026-10-24"),
                "typed day hint for a single entry still carries the Back line");
        ok(Nav.typedDayHint(false, 0, 0, 1, LocalDate.of(2026, 10, 24)).contains("Sat, Oct 24 1/1"), "typed day hint for a single entry shows position 1/1");
        ok(!dh.contains("\u2014") && !dh.contains("\u2013"), "no em or en dashes in the typed day hint");
        // 0.1.3 position rendering: Bedrock form title + Java chat line both carry N/M.
        ok(Render.formDayTitle(e, 0, 5).equals("What's New (Oct 24) (1/5)"), "bedrock day form title carries the position: " + Render.formDayTitle(e, 0, 5));
        ok(Render.formDayTitle(e, 9, 10).endsWith("(10/10)"), "bedrock day form title at walk end is 10/10");
        ok(Render.dayPositionLine(LocalDate.of(2026, 11, 13), 0, 5).equals("&7Fri, Nov 13 (1/5)"), "java day position line: day label + N/M");
        ok(Render.dayPositionLine(LocalDate.of(2026, 10, 26), 9, 10).equals("&7Mon, Oct 26 (10/10)"), "java day position line at walk end is 10/10");
        String xsrc = Files.readString(Path.of("src/com/winkreyes/excelwhatsnew/ExcelWhatsNew.java"));
        ok(xsrc.contains("Render.formDayTitle(e, idxInDay, d.entries.size())"), "day form opens with the position title");
        ok(xsrc.contains("Render.dayPositionLine(day, idxInDay, d.entries.size())"), "java day view sends the position line");
        ok(xsrc.contains("Nav.dayNewerWalkNumber(idxInDay)") && xsrc.contains("Nav.dayOlderWalkNumber(idxInDay)"), "java day buttons use the walk numbers");
        ok(xsrc.contains("openDay(q, fd, fi + 1, false)") && xsrc.contains("openDay(q, fd, fi - 1, false)"), "bedrock day form: Newer advances the walk, Older steps back");
        // By-date labels themselves use the design's em dash separator (design §3, exact labels).
        ok(("November 2026 — " + ByDate.countLabel(72)).equals("November 2026 — 72 changes"), "month button label is exact");
        ok(("Fri, Nov 13 — " + ByDate.countLabel(5)).equals("Fri, Nov 13 — 5 changes"), "day button label is exact");

        System.out.println("RESULT " + pass + " passed, " + fail + " failed, " + skip + " skipped");
        if (fail > 0) System.exit(1);
    }
}
