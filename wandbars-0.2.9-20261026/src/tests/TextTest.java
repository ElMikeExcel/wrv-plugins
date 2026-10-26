import com.winkreyes.excelwandbars.core.*;
import java.util.List;

public final class TextTest {
    public static void run() {
        System.out.println("TextTest");
        Check.eq("Bar 2/3: Boon, Feed, Extinguish", BarText.line(1, 3, List.of("Boon", "Feed", "Extinguish")), "bar line");
        Check.eq("Bar 3/3: (empty)", BarText.line(2, 3, List.of()), "empty bar");
        String longLine = BarText.line(0, 3, List.of("Aaaaaaaaaaaa", "Bbbbbbbbbbbb", "Cccccccccccc", "Dddddddddddd", "Eeeeeeeeeeee", "Ffff"));
        Check.ok(longLine.length() <= BarText.MAX_LEN + 4 && longLine.contains("+"), "long bar line truncated: " + longLine);
        Check.ok(!BarText.line(0, 3, List.of("A")).contains("\u2014"), "no em dash");
        Check.eq("1 sec", Durations.ms(1000), "1 s");
        Check.eq("1.5 sec", Durations.ms(1500), "1.5 s");
        Check.eq("6 sec", Durations.ms(6000), "6 s");
        Check.eq("2 min", Durations.ms(120000), "2 min");
        Check.eq("none", Durations.ms(0), "0");
        Check.eq("16", Durations.number(16.0), "16 blocks");
        Check.eq("Mend", SpellInfoText.strip("\u00a7aMend"), "strip colour");

        SpellFacts mend = new SpellFacts();
        mend.name = "Mend"; mend.description = "A small heal"; mend.costs.add("2 Mana"); mend.cooldownMs = 1000; mend.range = 16; mend.rank = 1; mend.maxRank = 3; mend.nextRankCasts = 20; mend.castCount = 5; mend.nextRankPath = "student"; mend.bar = 1;
        Check.eq("Mend: 2 Mana | 1 sec cooldown | 16 blocks", SpellInfoText.summary(mend), "summary");
        List<String> l = SpellInfoText.lines(mend);
        Check.eq("Mend  (Rank 1 of 3)", l.get(0), "header with rank");
        Check.ok(l.contains("Cost: 2 Mana"), "cost line");
        Check.ok(l.contains("Cooldown: 1 sec"), "cooldown line");
        Check.ok(l.contains("Range: 16 blocks"), "range line");
        Check.ok(l.contains("On bar 1"), "bar line");
        mend.pathOk = false;
        Check.ok(SpellInfoText.lines(mend).contains("Next rank: 5 of 20 casts, opens at Student rank"), "0.2.2 next rank line, rank locked: " + SpellInfoText.lines(mend));
        mend.pathOk = true;
        Check.ok(SpellInfoText.lines(mend).contains("Next rank: 5 of 20 casts"), "0.2.2 next rank line, rank open (no rank note)");
        mend.castCount = 0;
        Check.ok(SpellInfoText.lines(mend).contains("Next rank: 0 of 20 casts"), "0.2.2 zero casts still shows 0 of 20");
        mend.castCount = 99;
        Check.ok(SpellInfoText.lines(mend).contains("Next rank: 20 of 20 casts"), "count capped at the need");
        mend.castCount = 5;
        mend.pathOk = false; mend.rankByWand = true;
        Check.ok(SpellInfoText.lines(mend).contains("Next rank: comes with Student rank"), "0.2.2 pathless wand: next rank comes with the wand rank, no cast count: " + SpellInfoText.lines(mend));
        Check.ok(!SpellInfoText.lines(mend).stream().anyMatch(x -> x.contains("casts")), "pathless: no cast count shown");
        mend.pathOk = true; mend.rankByWand = false;
        Check.eq("Mend (Rank 1 of 3)", SpellInfoText.button(mend), "button");

        SpellFacts boon = new SpellFacts();
        boon.name = "Boon"; boon.costs.add("20 Mana"); boon.activeCosts.add("1 Mana"); boon.toggle = true; boon.stopsManaRegen = true; boon.selfOnly = true; boon.range = 32; boon.cooldownMs = 6000;
        Check.eq("20 Mana to start, then 1 Mana per sec", SpellInfoText.costText(boon), "toggle cost");
        List<String> bl = SpellInfoText.lines(boon);
        Check.ok(bl.contains("Range: Self"), "self spell shows Self, not a range");
        Check.ok(bl.stream().anyMatch(x -> x.startsWith("Toggle")), "toggle line");
        Check.ok(bl.stream().anyMatch(x -> x.contains("does not refill")), "no-regen line");
        Check.ok(!bl.stream().anyMatch(x -> x.startsWith("Next rank")), "single rank: no next-rank line");
        SpellFacts top = new SpellFacts(); top.name = "X"; top.rank = 3; top.maxRank = 3;
        Check.ok(SpellInfoText.lines(top).contains("Top rank reached"), "top rank line");
        SpellFacts free = new SpellFacts(); free.name = "Y";
        Check.eq("Free", SpellInfoText.costText(free), "free spell");
        for (String s : bl) Check.ok(!s.contains("\u2014"), "no em dash in: " + s);
    }
}
