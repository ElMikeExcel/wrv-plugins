import com.winkreyes.excelwandbars.core.ProgressGate;
import com.winkreyes.excelwandbars.core.ProgressText;
import com.winkreyes.excelwandbars.core.SpellFacts;
import com.winkreyes.excelwandbars.core.SpellTip;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 0.2.2: progress line text, rank header, throttle, Spell Book tip gate. */
public final class ProgressTest {
    static SpellFacts sp(String name, int rank, int max, long count, long need, String path, boolean ok) {
        SpellFacts f = new SpellFacts(); f.name = name; f.key = name.toLowerCase(); f.rank = rank; f.maxRank = max;
        f.castCount = count; f.nextRankCasts = need; f.nextRankPath = path; f.pathOk = ok; return f;
    }

    public static void run() {
        System.out.println("ProgressTest");
        // ---- line
        Check.eq("Boon: 12/40 casts to Rank 2", ProgressText.line(sp("Boon", 1, 4, 12, 40, "beginner", true)), "line, rank open");
        Check.eq("Heal: 12/50 casts to Rank 2 (opens at Student rank)", ProgressText.line(sp("Heal", 1, 3, 12, 50, "student", false)), "line, rank locked");
        Check.eq("Heal: 50/50 casts. Rank 2 opens at Student rank", ProgressText.line(sp("Heal", 1, 3, 73, 50, "student", false)), "line, count full but rank locked (capped)");
        Check.eq(null, ProgressText.line(sp("Feed", 1, 1, 3, 0, "", true)), "single-rank spell: no line");
        Check.eq(null, ProgressText.line(sp("Heal", 3, 3, 3, 50, "", true)), "top rank: no line");
        Check.eq(null, ProgressText.line(null), "null safe");
        Check.eq("Boon reached Rank 2!", ProgressText.rankedUp("Boon", 2), "rank-up line");
        SpellFacts bw = sp("Boon", 2, 4, 37, 50, "student", false); bw.rankByWand = true;
        Check.eq(null, ProgressText.line(bw), "pathless wand (WRV rank wands): casts never rank up, so no cast line");
        Check.eq(0, ProgressText.candidates(java.util.List.of(bw)).size(), "pathless: not a candidate");
        List<String> hw = ProgressText.header("Beginner", "Student", false, true, ProgressText.candidates(java.util.List.of(bw)));
        Check.eq(2, hw.size(), "pathless header: 2 lines");
        Check.eq("Your rank: Beginner. Next rank: Student.", hw.get(0), "pathless header line 1");
        Check.eq("Your spells move up a rank when you reach Student rank.", hw.get(1), "pathless header line 2 (no casts, no seals)");
        Check.eq("Your rank: Master (top rank).", ProgressText.header("Master", null, false, true, null).get(0), "pathless top rank");
        // ---- header
        List<SpellFacts> book = new ArrayList<>();
        book.add(sp("Heal", 1, 3, 30, 50, "student", false));
        book.add(sp("Boon", 1, 4, 12, 40, "beginner", true));
        book.add(sp("Feed", 1, 1, 0, 0, "", true));
        List<ProgressText.Candidate> c = ProgressText.candidates(book);
        Check.eq(1, c.size(), "only Boon can rank up at Beginner (Heal waits for Student, Feed has no ranks)");
        List<String> h = ProgressText.header("Beginner", "Student", true, c);
        Check.eq("Your rank: Beginner. Next rank: Student.", h.get(0), "header line 1");
        Check.eq("To become a Student, get Boon to Rank 2: 12/40 casts.", h.get(1), "header line 2 (one candidate)");
        book.add(sp("Recall", 1, 2, 20, 25, "beginner", true));
        h = ProgressText.header("Beginner", "Student", true, ProgressText.candidates(book));
        Check.eq("To become a Student, get any spell to its next rank. Closest: Recall, 20/25 casts.", h.get(1), "header picks the closest of several");
        h = ProgressText.header("Student", "Apprentice", false, ProgressText.candidates(book));
        Check.ok(h.get(1).startsWith("Keep casting"), "not by casts: neutral line");
        Check.eq("To become an Apprentice, get Boon to Rank 2: 12/40 casts.", ProgressText.header("Student", "Apprentice", true, ProgressText.candidates(book.subList(0, 2))).get(1), "an Apprentice");
        Check.eq("Your rank: Master (top rank).", ProgressText.header("Master", null, false, c).get(0), "top rank header");
        Check.eq(0, ProgressText.header(null, "Student", true, c).size(), "no path: no header");
        String all = String.join(" ", h) + String.join(" ", ProgressText.header("Beginner", "Student", true, c));
        Check.ok(!all.toLowerCase().contains("seal"), "header never mentions seals");
        for (String s : new String[] {all, ProgressText.line(book.get(0)), ProgressText.line(book.get(1))})
            Check.ok(s.indexOf('\u2014') < 0 && s.indexOf('\u2013') < 0, "no long dashes: " + s);
        // ---- gate
        UUID a = UUID.randomUUID();
        ProgressGate g = new ProgressGate();
        Check.eq(ProgressGate.Result.SHOW, g.decide(a, "boon", 1, 1, 40, false, 10_000), "first counted cast shows");
        Check.eq(ProgressGate.Result.THROTTLED, g.decide(a, "boon", 1, 2, 40, false, 12_000), "2 s later: throttled");
        Check.eq(ProgressGate.Result.SAME, g.decide(a, "boon", 1, 2, 40, false, 20_000), "count did not change: nothing");
        Check.eq(ProgressGate.Result.SHOW, g.decide(a, "boon", 1, 3, 40, false, 20_000), "after the gap: shows");
        Check.eq(ProgressGate.Result.SHOW, g.decide(a, "boon", 1, 40, 40, false, 21_000), "the cast that fills the count always shows");
        Check.eq(ProgressGate.Result.RANKED_UP, g.decide(a, "boon", 2, 0, 50, false, 21_500), "rank-up always shows");
        Check.eq(ProgressGate.Result.STANCE, g.decide(a, "boon", 2, 1, 50, true, 40_000), "stance on: skipped");
        g.skipDuringStance = false;
        Check.eq(ProgressGate.Result.SHOW, g.decide(a, "boon", 2, 2, 50, true, 50_000), "skip-during-stance false: shows");
        g.forget(a);
        Check.eq(ProgressGate.Result.SHOW, g.decide(a, "boon", 2, 2, 50, false, 50_100), "after quit: fresh");
        g.enabled = false;
        Check.eq(ProgressGate.Result.DISABLED, g.decide(a, "boon", 2, 3, 50, false, 90_000), "disabled");
        // ---- spell tip gate (repeats until OK)
        SpellTip t = new SpellTip();
        UUID b = UUID.randomUUID();
        int shows = 0;
        for (int k = 0; k < 40; k++) if (t.onTrigger(b, 100_000L * k) == SpellTip.Show.SHOW) shows++;
        Check.eq(40, shows, "until OK: no cap (40 of 40 spaced triggers showed)");
        Check.eq(SpellTip.Show.GAP, t.onTrigger(b, 100_000L * 39 + 10_000), "within the 60 s gap: no repeat");
        Check.ok(t.pending(b), "pending before OK");
        Check.ok(t.ack(b), "OK");
        Check.ok(!t.ack(b), "second OK is not new");
        Check.eq(SpellTip.Show.ACKED, t.onTrigger(b, 9_999_999), "after OK: never again");
        Check.ok(!t.pending(b), "not pending after OK");
        t.reset(b);
        Check.eq(SpellTip.Show.SHOW, t.onTrigger(b, 10_000_000), "admin reset: shows again");
        SpellTip capped = new SpellTip(); capped.maxShows = 2;
        capped.onTrigger(b, 0); capped.onTrigger(b, 100_000);
        Check.eq(SpellTip.Show.DONE, capped.onTrigger(b, 200_000), "max-shows 2: stops");
        SpellTip off = new SpellTip(); off.enabled = false;
        Check.eq(SpellTip.Show.DISABLED, off.onTrigger(b, 0), "disabled");
        List<String> bl = SpellTip.lines(true), jl = SpellTip.lines(false);
        String bt = String.join(" ", bl).toLowerCase(), jt = String.join(" ", jl);
        Check.ok(!bt.contains("right-click") && !bt.contains("click"), "Bedrock tip: no click words");
        Check.ok(bt.contains("tap") && bt.contains("press and hold"), "Bedrock tip: tap and press and hold");
        Check.ok(bt.contains("/spellbook ok"), "Bedrock tip: /spellbook ok");
        Check.ok(jt.contains("[OK]") && jt.contains("/spellbook"), "Java tip: [OK] and /spellbook");
        Check.eq(bl.size() - 1, SpellTip.body(true).size(), "body drops the OK line");
        for (String s : bl) Check.ok(s.length() <= 120 && s.indexOf('\u2014') < 0, "Bedrock tip line short, no long dash: " + s.length());
        Check.ok(!(bt + jt.toLowerCase()).contains("seal"), "tip never mentions seals");
        Check.ok(!(bt + jt.toLowerCase()).contains("how close"), "0.2.2 tip does not promise a cast count (pathless wands show none)");
    }
}

