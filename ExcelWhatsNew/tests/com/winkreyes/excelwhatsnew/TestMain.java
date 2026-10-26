package com.winkreyes.excelwhatsnew;

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
import java.util.*;
import java.util.zip.ZipFile;

public class TestMain {
    static int pass = 0, fail = 0;
    static void ok(boolean c, String m) { if (c) { pass++; System.out.println("ok   " + m); } else { fail++; System.out.println("FAIL " + m); } }

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

        // Every command and alias named in the seed exists in the jar that is LIVE (hash-checked copies on the box)
        Map<String, String> jars = new LinkedHashMap<>();
        jars.put("lostfound", "/workspace/batch-next/deathrecord-0.3.0/ExcelDeathRecord-0.3.0.jar");
        jars.put("recover", "/workspace/batch-next/deathrecord-0.3.0/ExcelDeathRecord-0.3.0.jar");
        jars.put("mark", "/workspace/batch-next/makersmark/ExcelMakersMark-0.1.0.jar");
        jars.put("tips", "/workspace/batch-next/2am-batch/files/tips/ExcelTips-0.1.0.jar");
        jars.put("autosort", "/workspace/batch-next/2am-batch/files/spellbook/autosort-0.1.17+26.3-paper.jar");
        jars.put("sort", "/workspace/batch-next/2am-batch/files/spellbook/autosort-0.1.17+26.3-paper.jar");
        jars.put("wandstyle", "/workspace/batch-next/2am-batch/files/spellbook/ExcelWandBars-0.2.1.jar");
        jars.put("trash", "/workspace/batch-next/2am-batch/files/trash/ExcelTrash-0.2.0.jar");
        for (var j : jars.entrySet()) ok(jarCommands(j.getValue()).contains(j.getKey()), "/" + j.getKey() + " is registered by " + Path.of(j.getValue()).getFileName());
        String py = Files.readString(Path.of("plugin.yml"));
        ok(py.contains("  whatsnew:") && py.contains("aliases: [news]"), "/whatsnew and /news registered by this plugin");
        ok(py.contains("excelwhatsnew.use:\n    default: true"), "/whatsnew is for everyone (default true)");

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

        // 0.1.1 Newer news paging
        ok(!Nav.hasNewer(0) && Nav.hasNewer(1) && Nav.hasNewer(9), "Newer news only when idx > 0");
        ok(Nav.newerNumber(1) == 1 && Nav.newerNumber(4) == 4, "Newer news opens /whatsnew idx (the next newer one)");
        ok(Nav.olderNumber(0) == 2 && Nav.olderNumber(3) == 5, "Older news opens /whatsnew idx+2 (unchanged)");
        ok(Nav.hasOlder(0, 10) && !Nav.hasOlder(9, 10) && !Nav.hasOlder(0, 1), "Older news stops at the last shown entry");
        // Bedrock form: order OK, Newer, Older, Close; OK and Close never together (same as 0.1.0)
        ok(Nav.formButtons(true, 0, 10).equals(List.of(Act.OK, Act.OLDER)), "form newest + pending: OK, Older");
        ok(Nav.formButtons(false, 0, 10).equals(List.of(Act.OLDER, Act.CLOSE)), "form newest, already OK: Older, Close");
        ok(Nav.formButtons(false, 3, 10).equals(List.of(Act.NEWER, Act.OLDER, Act.CLOSE)), "form middle: Newer, Older, Close");
        ok(Nav.formButtons(false, 9, 10).equals(List.of(Act.NEWER, Act.CLOSE)), "form oldest shown: Newer, Close");
        ok(Nav.formButtons(true, 2, 10).equals(List.of(Act.OK, Act.NEWER, Act.OLDER)), "form pending middle: OK, Newer, Older");
        ok(Nav.formButtons(false, 0, 1).equals(List.of(Act.CLOSE)) && Nav.formButtons(true, 0, 1).equals(List.of(Act.OK)), "form single entry: one button");
        // Callback mapping through the same list
        List<Act> mid = Nav.formButtons(false, 3, 10);
        ok(Nav.clicked(mid, 0) == Act.NEWER && Nav.clicked(mid, 1) == Act.OLDER && Nav.clicked(mid, 2) == Act.CLOSE, "middle form: id 0 Newer, 1 Older, 2 Close");
        List<Act> pend = Nav.formButtons(true, 0, 10);
        ok(Nav.clicked(pend, 0) == Act.OK && Nav.clicked(pend, 1) == Act.OLDER, "pending newest: id 0 OK, id 1 Older (same ids as 0.1.0)");
        List<Act> last = Nav.formButtons(false, 9, 10);
        ok(Nav.clicked(last, 0) == Act.NEWER && Nav.clicked(last, 1) == Act.CLOSE, "oldest form: id 0 Newer, id 1 Close (never Older)");
        ok(Nav.clicked(mid, -1) == null && Nav.clicked(mid, 3) == null && Nav.clicked(mid, 99) == null, "closed or out-of-range id does nothing");
        // Java row: same order, no Close
        ok(Nav.javaButtons(true, 0, 10).equals(List.of(Act.OK, Act.OLDER)), "java newest pending: [OK] [Older news]");
        ok(Nav.javaButtons(false, 3, 10).equals(List.of(Act.NEWER, Act.OLDER)), "java middle: [Newer news] [Older news]");
        ok(Nav.javaButtons(false, 9, 10).equals(List.of(Act.NEWER)), "java oldest: [Newer news]");
        ok(Nav.javaButtons(false, 0, 1).isEmpty(), "java single entry, already OK: no row");
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
        Path live = Path.of("/workspace/batch-next/fixes-20261026-am/readback/ExcelWhatsNew_config.yml");
        if (Files.exists(live)) {
            Map<String, Object> lm = new Yaml().load(Files.readString(live));
            Map<?, ?> tx = (Map<?, ?>) lm.get("text");
            ok(tx != null && !tx.containsKey("newer-button"), "live config 62b95a5e has no newer-button (code default 'Newer news' applies)");
            ok(Integer.valueOf(10).equals(lm.get("show-count")), "live config show-count is 10");
        }
        String src = Files.readString(Path.of("src/com/winkreyes/excelwhatsnew/ExcelWhatsNew.java"));
        ok(src.contains("c.getString(\"text.newer-button\", \"Newer news\")"), "code reads text.newer-button with default 'Newer news'");
        ok(!src.contains("(or 3)"), "hardcoded (or 3) gone from source");

        System.out.println("RESULT " + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
