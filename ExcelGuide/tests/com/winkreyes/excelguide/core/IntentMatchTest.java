package com.winkreyes.excelguide.core;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Intent matching over the REAL intents.yml (SPEC 10.1): every intent
 * gets at least five positive phrasings (kid phrasings, no-caps
 * Bedrock style, typos-lite) that must match it at or above the
 * shipped intent threshold, and negatives - a shared pool of family
 * chatter that must match nothing, plus per-intent forbidden cases.
 * Also covers the ambient door: only the marked tight groups may fire.
 */
public final class IntentMatchTest {

    private static final double BAR = 0.72; // shipped intent-min-score

    private static boolean hasSkill(String norm) {
        for (String tok : norm.split(" ")) {
            if (GuideEngine.SKILLS.contains(tok)) {
                return true;
            }
        }
        return false;
    }

    private static IntentSet.Match match(IntentSet intents, String line, boolean ambientDoor) {
        String norm = TextNorm.normalize(line);
        return intents.match(norm, TextNorm.tokenSet(norm), ambientDoor, hasSkill(norm));
    }

    private static void expect(TestSupport.T t, IntentSet intents, String line, String id) {
        IntentSet.Match m = match(intents, line, false);
        t.check("match [" + line + "] -> " + id,
                m != null && m.intent().id().equals(id) && m.score() >= BAR);
    }

    private static void expectAmbient(TestSupport.T t, IntentSet intents, String line, String id) {
        IntentSet.Match m = match(intents, line, true);
        t.check("ambient [" + line + "] -> " + id,
                m != null && m.intent().id().equals(id) && m.score() >= BAR);
    }

    private static void expectNone(TestSupport.T t, IntentSet intents, String line, boolean ambientDoor) {
        IntentSet.Match m = match(intents, line, ambientDoor);
        t.check("no match [" + line + "]" + (ambientDoor ? " (ambient)" : ""),
                m == null || m.score() < BAR);
    }

    public static void main(String[] args) {
        TestSupport.T t = new TestSupport.T();
        IntentSet intents = TestSupport.realIntents();
        t.eq("intent count (28 + my-claim-blocks)", 29, intents.intents().size());

        Map<String, List<String>> positives = Map.ofEntries(
            Map.entry("lost-wand", List.of(
                "i lost my wand", "I LOST MY WAND", "my wand is gone",
                "i cant find my wand", "help my wand disappeared", "how do i restore my wand")),
            Map.entry("magic-standing", List.of(
                "what is my magic level", "my magic level", "whats my mage level",
                "show my magic", "magic level please")),
            Map.entry("get-spells", List.of(
                "how do i get new spells", "how do i learn spells", "teach me a spell",
                "how do i learn magic", "i want to learn spells", "how can i get spells",
                "how do i unlock spells",
                "when can i learn spells", "when do i get spells", "when can i learn magic")),
            Map.entry("mcmmo-skill-level", List.of(
                "what is my mining level", "whats my fishing level", "my mining level",
                "what level is my mining", "show my mining level", "mining level")),
            Map.entry("jobs-level", List.of(
                "what is my job level", "my jobs level", "what level is my job",
                "show my jobs", "whats my miner level", "my job level please")),
            Map.entry("playtime", List.of(
                "how long have i played", "whats my playtime", "how many hours have i played",
                "how long have i played on this server", "check my play time", "hours played")),
            Map.entry("my-money", List.of(
                "what is my balance", "how much money do i have", "my balance",
                "how much money have i got", "show my money", "my cash")),
            Map.entry("my-level", List.of(
                "whats my level", "what level am i", "show my level",
                "check my level", "my level", "what is my level")),
            Map.entry("seals-progress", List.of(
                "how many seals are left", "how many seals", "seal progress",
                "how many seals are broken", "how many seals remaining", "how many seals do we need")),
            Map.entry("guild-locked", List.of(
                "why is the guild locked", "how do i enter the guild", "where is the guild",
                "when will the guild open", "i cant enter the guild", "how do i get into the guild")),
            // 0.2.2: first-person amount questions route to the
            // my-claim-blocks STAT (Mike's live test: "I was hoping it
            // would actually tell me the amount of blocks"); the
            // generic claim-amounts keeps the impersonal shapes below.
            // ClaimBlocksTest covers the answer-level checks.
            Map.entry("my-claim-blocks", List.of(
                "how much land can i claim", "how many claim blocks do i get",
                "how much can i claim", "how many blocks do i get",
                "how much land do i get", "how many claim blocks can i have",
                "how do i get more land", "how much land do i have left",
                "how many claim blocks do i have", "what are my claim blocks",
                "how many blocks do i have left", "claim blocks i have left",
                "how much more land can i claim")),
            Map.entry("claim-amounts", List.of(
                "what is the claim limit",
                "how much land can a player claim", "how many claim blocks do you get",
                "how much land can you claim")),
            Map.entry("land-leaderboard", List.of(
                "who has the most land", "who claimed the most land",
                "who has claimed the most land", "show me the land leaderboard",
                "who has the biggest claim", "land rankings",
                "who claimed the most", "the most land claimed")),
            Map.entry("claim-land", List.of(
                "how do i claim land", "claim my land", "how do i protect my house",
                "how can i claim my stuff area", "what does the golden shovel do", "how do i claim")),
            Map.entry("griefed", List.of(
                "someone griefed me", "someone broke my house", "who broke my fence",
                "someone stole my diamonds", "my stuff is gone someone took it", "can you undo the damage")),
            Map.entry("shops-how", List.of(
                "how do shops work", "how do i buy from a shop", "how do i buy stuff",
                "how do i use the shop", "how shops work", "how do i buy")),
            Map.entry("make-shop", List.of(
                "how do i make a shop", "i want to make my own shop", "how do i open a shop",
                "i want to sell my stuff", "how do i start a shop", "how do i sell items")),
            Map.entry("commands-list", List.of(
                "what commands are there", "command list", "list the commands",
                "what command do i use", "what commands can i use", "show me the command list")),
            Map.entry("home-teleport", List.of(
                "how do i get home", "how do i go home", "how do i set home",
                "what is sethome", "how do i teleport home", "how do i get back home")),
            Map.entry("places-visit", List.of(
                "what is places", "how do i visit my friend", "how do i visit a base",
                "how do i visit her house", "how do i save a place", "tell me about places")),
            Map.entry("party", List.of(
                "how do parties work", "what is a party", "how do i play together",
                "what is the party bonus", "how do i join a party", "party")),
            Map.entry("xp-bonuses", List.of(
                "is there an xp bonus", "what is the weekend bonus", "weekend xp",
                "how do i get more xp", "is there a weekend xp bonus", "any xp bonus today")),
            Map.entry("death", List.of(
                "do i lose my stuff if i die", "keep inventory", "what happens when i die",
                "i died and lost my stuff", "do i keep my stuff when i die", "if i die do i lose my stuff")),
            Map.entry("phantoms", List.of(
                "do phantoms spawn here", "what is a phantom", "there are phantoms",
                "i cant sleep because of the noise", "phantoms")),
            Map.entry("skill-unlocks", List.of(
                "what do i unlock", "what do i get next", "what is the next perk",
                "show me the skill tree", "when do i unlock the next thing", "what do i unlock with mining",
                "how do i get a new ability", "how do abilities work", "how do i unlock abilities",
                "what abilities does mining have", "how do i get abilities in mcmmo",
                "how do i earn new abilities",
                "when do i get a new ability ?", "when do i get new abilities",
                "when do abilities unlock")),
            Map.entry("level-a-skill", List.of(
                "how do i level up mining", "how to level fishing", "how do i train fishing",
                "how do i get better at mining", "train mining", "how do i level mining")),
            Map.entry("elites", List.of(
                "what are elite mobs", "what is an elite", "what are boss mobs",
                "tell me about the adventure mobs", "that was a strong monster", "elite")),
            Map.entry("restart", List.of(
                "when does the server restart", "server restart", "does the server restart",
                "what time is the restart", "restart", "will it reboot soon")),
            Map.entry("builder-mode", List.of(
                "what is build mode", "what is builder mode", "builder mode",
                "tell me about build mode", "how does builder mode work", "build mode info"))
        );
        for (Map.Entry<String, List<String>> e : positives.entrySet()) {
            for (String line : e.getValue()) {
                expect(t, intents, line, e.getKey());
            }
        }

        // Family chatter that must match NOTHING, on any door.
        List<String> chatter = List.of(
            "dinner is ready", "i made a cake", "look at my house its cool",
            "the dragon is dead lol", "i need more wood", "anyone have iron",
            "mom said bedtime soon", "i found diamonds", "lets go mining tomorrow",
            "my dog is cute", "school was fun today", "i built a tower",
            "who wants cake", "the farm is done", "i am going to the nether");
        for (String line : chatter) {
            expectNone(t, intents, line, false);
            expectNone(t, intents, line, true);
        }

        // Per-intent forbidden / confusion cases.
        expectNone(t, intents, "i found my wand", false);              // wand, found
        expectNone(t, intents, "how do i craft a wand", false);        // wand, craft
        expectNone(t, intents, "i died laughing", false);              // death, forbidden phrase
        expectNone(t, intents, "its my birthday party", false);        // party, birthday
        expectNone(t, intents, "the elite four are cool", false);      // elites, four
        expectNone(t, intents, "bob the builder is on tv", false);     // builder below bar
        expectNone(t, intents, "the water level is rising", false);    // level, not a stats question
        expectNone(t, intents, "what level is the mine", false);       // no clarify trigger
        expectNone(t, intents, "i love my job", false);                // jobs, no level
        expectNone(t, intents, "how much money does it cost", false);  // money, cost veto
        expectNone(t, intents, "the shop balance is weird", false);    // money, shop veto
        expectNone(t, intents, "the seal at the zoo was cute", false);  // seals, singular zoo
        expectNone(t, intents, "restart your game its glitched", false); // restart, game veto
        expectNone(t, intents, "unlock my phone please", false);       // unlocks, phone veto
        expectNone(t, intents, "the quartermaster tells guild tales", false); // guild, tales veto
        expectNone(t, intents, "magic tricks are fun", false);         // magic, no claim
        expectNone(t, intents, "how do i make a golem", false);        // vanilla crafting, no intent (0.1.1 baseline)
        expectNone(t, intents, "what level do i need for spells", false); // spells, level veto keeps it unclaimed
        expectNone(t, intents, "i claim this seat", false);            // claim, no land/my
        expectNone(t, intents, "story time was fun", false);           // playtime, no played
        IntentSet.Match mm = match(intents, "what is my magic level", false);
        t.check("magic question routes to magic-standing, not clarify",
                mm != null && mm.intent().id().equals("magic-standing"));

        // Skill-name disambiguation: clarify must not swallow skill levels.
        IntentSet.Match skill = match(intents, "whats my mining level", false);
        t.check("skill level beats clarify", skill != null && skill.intent().id().equals("mcmmo-skill-level"));

        // Ambient door: the marked tight set fires...
        expectAmbient(t, intents, "i lost my wand", "lost-wand");
        expectAmbient(t, intents, "my wand is gone", "lost-wand");
        expectAmbient(t, intents, "how many seals are left", "seals-progress");
        expectAmbient(t, intents, "the guild is locked", "guild-locked");
        expectAmbient(t, intents, "why cant i enter the guild", "guild-locked");
        expectAmbient(t, intents, "how do i claim land", "claim-land");
        expectAmbient(t, intents, "what commands are there", "commands-list");
        expectAmbient(t, intents, "do i lose my stuff when i die", "death");
        expectAmbient(t, intents, "keep inventory on", "death");
        // Hot-fix v2 (Oct 18, 2026): the two live failures happened in
        // plain chat, so the new shapes must fire through the ambient
        // door too - and ONLY those shapes (the rest of skill-unlocks
        // and get-spells stays command/summon-only, as at launch).
        // 0.2.2: the first-person shapes fire ambient as the
        // my-claim-blocks STAT (the answer is whispered to the asker,
        // never public); the impersonal shape still fires the public
        // generic claim-amounts.
        expectAmbient(t, intents, "how much land can i claim", "my-claim-blocks");
        expectAmbient(t, intents, "how many claim blocks do i get", "my-claim-blocks");
        expectAmbient(t, intents, "how do i get more land", "my-claim-blocks");
        expectAmbient(t, intents, "how much land can a player claim", "claim-amounts");
        // Land leaderboard (0.2.0): the pointer intent fires in plain
        // chat on its tight shapes only - bragging without an anchor
        // word stays chatter.
        expectAmbient(t, intents, "who has the most land", "land-leaderboard");
        expectAmbient(t, intents, "who claimed the most land", "land-leaderboard");
        expectAmbient(t, intents, "show me the land leaderboard", "land-leaderboard");
        expectNone(t, intents, "i have the most land", true);
        expectNone(t, intents, "i have the most land", false);
        expectNone(t, intents, "my claim is the biggest one here", true);
        expectAmbient(t, intents, "when do i get a new ability ?", "skill-unlocks");
        expectAmbient(t, intents, "when can i learn spells", "get-spells");
        expectNone(t, intents, "what do i unlock", true);
        expectNone(t, intents, "how do i get new spells", true);
        expectNone(t, intents, "how do i get a new ability", true);
        // ...and everything else stays silent in plain chat.
        expectNone(t, intents, "whats my level", true);
        expectNone(t, intents, "how much money do i have", true);
        expectNone(t, intents, "what is my balance", true);
        expectNone(t, intents, "how do shops work", true);
        expectNone(t, intents, "what are elite mobs", true);
        expectNone(t, intents, "how long have i played", true);
        expectNone(t, intents, "do phantoms spawn", true);
        expectNone(t, intents, "what is my mining level", true);

        t.finish("IntentMatchTest");
    }
}
