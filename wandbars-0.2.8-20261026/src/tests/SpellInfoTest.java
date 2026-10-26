import com.winkreyes.excelwandbars.core.*;
import java.util.*;

/**
 * Spell info 2026-10-26: golden checks against the live Magic spells.yml copy
 * (tests/data/spells.yml, sha256 c505e510...). Every expected number here was
 * checked by hand against that file. If spells.yml changes, these fail on
 * purpose: re-check the numbers, then update the expectations.
 */
public final class SpellInfoTest {
    static final Map<String, List<String>> WANDS = new LinkedHashMap<>();
    static {
        WANDS.put("beginner", List.of("heal", "boon", "feed", "extinguish", "aqualung", "mend"));
        WANDS.put("student", List.of("heal", "boon", "feed", "extinguish", "aqualung", "cure", "shell", "gills", "stormshackle", "mend", "flashheal", "ward"));
        WANDS.put("apprentice", List.of("heal", "boon", "feed", "extinguish", "aqualung", "cure", "shell", "gills", "stormshackle", "superheal", "recall", "mend", "flashheal", "ward", "greaterheal"));
        WANDS.put("master", List.of("heal", "boon", "feed", "extinguish", "aqualung", "cure", "shell", "gills", "stormshackle", "superheal", "recall", "blessing", "recall_death", "mend", "flashheal", "ward", "greaterheal"));
    }
    static final Map<String, List<String>> HOLDS = Map.of(
            "beginner", List.of("beginner"), "student", List.of("student", "beginner"),
            "apprentice", List.of("apprentice", "student", "beginner"), "master", List.of("master", "apprentice", "student", "beginner"));

    static int level(SpellsYml y, String spell, String rank) {
        return SpellEffects.levelAt(y.maxLevel(spell), l -> y.requiredPath(spell, l), p -> HOLDS.get(rank).contains(p));
    }

    static List<String> fx(SpellsYml y, String key) { return SpellEffects.lines(y.get(key)); }

    public static void run(String path) throws Exception {
        System.out.println("SpellInfoTest");
        Check.eq("0.75 heart", SpellEffects.hearts(1.5), "hearts 1.5 HP");
        Check.eq("1 heart", SpellEffects.hearts(2), "hearts 2 HP");
        Check.eq("1.25 hearts", SpellEffects.hearts(2.5), "hearts 2.5 HP");
        Check.eq("5 hearts", SpellEffects.hearts(10), "hearts 10 HP");
        Check.eq("3 drumsticks", SpellEffects.drumsticks(6), "food 6");
        Check.eq(List.of("Cures Nausea, Weakness, Poison, Slowness,", "Mining Fatigue and Wither"),
                SpellEffects.wrap("Cures Nausea, Weakness, Poison, Slowness, Mining Fatigue and Wither", 44), "wrap at a space");
        if (path == null || path.isBlank() || !new java.io.File(path).exists()) { Check.ok(false, "spells.yml copy missing: " + path); return; }
        SpellsYml y = new SpellsYml(path);

        // Spell rank each wand rank gives (Magic Wand.levelSpells with level_spells_to_path).
        String[][] want = {
            {"beginner", "heal=2 boon=2 feed=1 extinguish=1 aqualung=1 mend=2"},
            {"student", "heal=2 boon=3 feed=1 extinguish=1 aqualung=1 cure=2 shell=2 gills=1 stormshackle=1 mend=3 flashheal=2 ward=1"},
            {"apprentice", "heal=3 boon=3 feed=1 extinguish=1 aqualung=1 cure=3 shell=3 gills=1 stormshackle=1 superheal=1 recall=4 mend=4 flashheal=3 ward=1 greaterheal=2"},
            {"master", "heal=3 boon=4 feed=1 extinguish=1 aqualung=1 cure=5 shell=4 gills=1 stormshackle=1 superheal=1 recall=5 blessing=1 recall_death=1 mend=5 flashheal=4 ward=1 greaterheal=3"},
        };
        for (String[] w : want) {
            StringBuilder sb = new StringBuilder();
            for (String sp : WANDS.get(w[0])) sb.append(sb.length() > 0 ? " " : "").append(sp).append('=').append(level(y, sp, w[0]));
            Check.eq(w[1], sb.toString(), "spell ranks on a " + w[0] + " wand");
        }

        // Golden lines (hand-checked against spells.yml).
        Check.eq(List.of("Who: a friendly player or mob you aim at", "Aim at nothing to use it on yourself",
                "Heals 0.75 heart", "Your mana refills slower for 5 sec after"), fx(y, "mend|2"), "Mend rank 2 (Beginner)");
        Check.ok(fx(y, "mend|3").contains("Heals 1 heart"), "Mend rank 3 heals 2 HP");
        Check.ok(fx(y, "mend|4").contains("Heals 1.25 hearts"), "Mend rank 4 heals 2.5 HP");
        Check.ok(fx(y, "mend|5").contains("Heals 1.5 hearts"), "Mend rank 5 heals 3 HP");
        Check.eq(List.of("Who: a friendly player or mob you aim at", "Aim at nothing to use it on yourself",
                "Heals 0.5 heart every 1 sec for 10 sec", "5 hearts in all", "Same target can get it again after 10 sec",
                "Your mana refills slower for 5 sec after"), fx(y, "heal|2"), "Heal rank 2 (Beginner)");
        Check.ok(fx(y, "heal|3").containsAll(List.of("Heals 1 heart right away", "Then 0.5 heart every 1 sec for 10 sec", "6 hearts in all")), "Heal rank 3");
        Check.eq(List.of("Who: you", "Gives Speed II while on (40% faster)", "Gives Jump Boost III while on"), fx(y, "boon|2"), "Boon rank 2 (Beginner)");
        Check.ok(fx(y, "boon|3").contains("Gives Speed III while on (60% faster)"), "Boon rank 3 Speed III");
        Check.ok(fx(y, "boon|4").contains("Gives Night Vision while on"), "Boon rank 4 night vision");
        Check.eq(List.of("Who: a friendly player you aim at", "Aim at nothing to use it on yourself", "Heals 2 hearts",
                "Cures Poison and Wither", "Fills 3 drumsticks of food", "Your mana refills slower for 5 sec after"), fx(y, "feed"), "Feed");
        Check.eq(List.of("Works where you aim", "Puts out fire blocks within 10 blocks", "Puts out burning players and mobs nearby"), fx(y, "extinguish"), "Extinguish");
        Check.eq(List.of("Works by itself when you start to drown", "Fills your air bar all the way"), fx(y, "aqualung"), "Aqualung");
        Check.ok(fx(y, "cure|2").contains("Cures Nausea, Weakness, Poison, Slowness, Mining Fatigue and Wither"), "Cure rank 2 cures");
        Check.ok(fx(y, "cure|2").contains("within 5 blocks of you"), "Cure rank 2 radius 5");
        Check.ok(fx(y, "cure|3").contains("within 8 blocks of you"), "Cure rank 3 radius 8");
        Check.ok(fx(y, "cure|4").contains("Fills 2 drumsticks of food over 2 sec"), "Cure rank 4 food");
        Check.ok(fx(y, "cure|5").contains("Turns zombie villagers back into villagers"), "Cure rank 5 villagers");
        Check.ok(fx(y, "shell|2").containsAll(List.of("The dome lasts 5 sec", "Spells that hit it bounce back 50% of the time", "Dome size: 3 blocks from middle to edge")), "Shell rank 2");
        Check.ok(fx(y, "gills").containsAll(List.of("Gives Water Breathing for 30 sec", "Puts a glass helmet on you for 30 sec", "Helmet has Respiration III and Aqua Affinity")), "Gills");
        Check.ok(fx(y, "stormshackle").containsAll(List.of("Each hit: 0.25 heart damage", "Each hit: Slowness I for 4 sec", "First target is hit 3 times",
                "Then jumps to up to 3 more within 8 blocks", "Each of those is hit 3 times", "Each then jumps to 1 more within 8 blocks", "That one is hit 1 time")), "Storm Shackle");
        Check.ok(fx(y, "flashheal|2").contains("Heals 2.5 hearts") && fx(y, "flashheal|3").contains("Heals 3 hearts") && fx(y, "flashheal|4").contains("Heals 3.5 hearts"), "Flash Heal ranks");
        Check.ok(fx(y, "ward").containsAll(List.of("Gives Absorption I for 15 sec (2 extra hearts)", "Same target can get it again after 12 sec")), "Ward");
        Check.ok(fx(y, "superheal").containsAll(List.of("Heals 4 hearts each", "Gives Regeneration I for 5 sec (about 1 heart)", "Fills 2 drumsticks of food each")), "Superheal");
        Check.ok(fx(y, "greaterheal|2").containsAll(List.of("Takes 2 sec to cast", "You get Slowness II while casting", "A hit over 2 hearts stops it", "Casting another spell stops it", "Then heals 3 hearts")), "Greater Heal rank 2");
        Check.ok(fx(y, "greaterheal|3").contains("Then heals 4 hearts"), "Greater Heal rank 3");
        Check.ok(fx(y, "recall|4").containsAll(List.of("You wait 5 sec, then you go", "A hit over 0.5 heart stops it", "You can keep 2 markers")), "Recall rank 4");
        Check.ok(fx(y, "blessing").containsAll(List.of("Heals 8 hearts at once (Instant Health III)", "Gives Resistance II for 10 sec (40% less damage)", "Gives Absorption II for 10 sec (4 extra hearts)")), "Blessing");

        // Every template players can get: something to say, no em dash, no seals, short after wrapping.
        Set<String> seen = new TreeSet<>();
        for (Map.Entry<String, List<String>> w : WANDS.entrySet()) for (String sp : w.getValue()) {
            for (int l = 1; l <= y.maxLevel(sp); l++) seen.add(l == 1 ? sp : sp + "|" + l);
        }
        for (String k : seen) {
            List<String> lines = fx(y, k);
            Check.ok(!lines.isEmpty(), k + " has effect lines");
            for (String s : lines) {
                Check.ok(!s.contains("\u2014") && !s.toLowerCase(Locale.ROOT).contains("seal"), k + " clean: " + s);
                for (String part : SpellEffects.wrap(s, SpellInfoText.LINE)) Check.ok(part.length() <= SpellInfoText.LINE, k + " short: " + part);
            }
        }

        // Full screen for Mend on a Beginner wand, as a player sees it.
        SpellFacts m = new SpellFacts();
        m.key = "mend"; m.name = "Mend"; m.description = "A quick, cheap touch of healing."; m.rank = 2; m.maxRank = 5; m.bar = 1;
        m.costs.add("4 Mana"); m.mageCooldownMs = 1000; m.range = 16; m.effects.addAll(fx(y, "mend|2"));
        m.rankByWand = true; m.pathOk = false; m.nextRankPath = "student";
        SpellFacts m3 = new SpellFacts(); m3.costs.add("8 Mana"); m3.mageCooldownMs = 1000; m3.range = 16; m3.effects.addAll(fx(y, "mend|3"));
        m.nextWandRank = "Student"; m.nextWandRankLevel = 3;
        m.nextChanges.addAll(SpellEffects.changes(SpellInfoText.rankLines(m), SpellInfoText.rankLines(m3)));
        m.rankUpHow = "Student rank is not open yet. Watch What's New.";
        List<String> screen = SpellInfoText.lines(m);
        System.out.println("    " + String.join("\n    ", screen));
        Check.ok(screen.contains("Cooldown: none, just a 1 sec pause"), "Mend cooldown shows the 1 sec pause");
        Check.ok(screen.contains("Next rank: Rank 3 at Student rank"), "Mend next rank line");
        Check.ok(screen.contains("- Heals 1 heart") && screen.contains("- Cost: 8 Mana"), "Mend rank 3 changes");
        Check.ok(screen.contains("How to reach Student rank:"), "how-to line");
        Check.ok(screen.stream().anyMatch(x -> x.startsWith("Next rank")), "lore keeps a Next rank line (Magic-style lore)");
    }
}
