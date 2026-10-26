package com.winkreyes.excelguide.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The peer-learning gates (0.3.0). Pure string work, no state, no I/O.
 *
 * 0.2.3 learned 65 "answers" from family chat and most were chatter
 * ("<name> come back", "my brain is fried"). The causes were loose
 * gates: any question-shaped line opened a capture window, the next
 * line from anyone else became the answer, one line could answer
 * several questions at once, a lone "yeah" counted as corroboration,
 * and an answer nobody objected to was learned on its own. 0.3.0
 * tightens every gate, and nothing a player says is ever served
 * until Mike approves it.
 *
 * The gates:
 * - QUESTION gate (may a window open?): question-shaped, at least
 *   three words with one meaningful one, names a game term, is not
 *   social chat aimed at another player ("do you wanna...", "have
 *   you seen..."), carries no profanity, no player name, and is not
 *   about seal-fight mechanics.
 * - ANSWER gate (may a line become a candidate?): long enough
 *   (default 20 characters and 4 words, one of them meaningful),
 *   not a question, not chit-chat or a hedge ("lol", "idk", "maybe",
 *   "come back", lines that open with "i"/"my"/"btw"...), shares a
 *   meaningful word with the question or names two game words, and
 *   passes the profanity, player-name, spoiler and 0.2.3 privacy
 *   screens (PeerGuard).
 * - SERVE gate: the spoiler, profanity and name screens run again
 *   at serve time, so a word added to the config later also
 *   blocks answers that were approved before it.
 *
 * Every list has built-in defaults; the config lists ADD to them
 * (the live data-folder config.yml is never replaced by a jar swap,
 * so the defaults must stand on their own).
 */
public final class PeerFilter {

    /** Words too generic to count as shared topic between a question and an answer. */
    static final Set<String> GENERIC = Set.of(
            "get", "make", "go", "going", "gonna", "thing", "things", "stuff", "one", "like",
            "need", "want", "know", "think", "really", "also", "still", "more", "some", "any",
            "all", "way", "time", "now", "yes", "yeah", "no", "not", "use", "work", "works",
            "good", "bad", "much", "many", "lot", "lots", "u", "ur", "ya", "yall", "out",
            "up", "down", "off", "over", "only", "even", "too", "very", "well", "back",
            "again", "wait", "said", "say", "see", "look", "put", "give", "take", "come",
            "anymore", "ever", "already", "why", "which", "who", "whats", "something",
            "anything", "everything", "guys", "guy", "oh", "ok", "okay", "lol", "dont",
            "doesnt", "didnt", "isnt", "arent", "wasnt", "cant", "wont", "aint", "theres",
            "thats", "youre", "were", "hes", "shes", "ill", "id", "wanna", "gotta");

    /**
     * Built-in game vocabulary: Minecraft items, blocks, mobs and
     * mechanics plus the valley's own systems. A question must name
     * one to open a window; an answer that shares no word with its
     * question must name one to be considered at all.
     */
    static final Set<String> DEFAULT_GAME_TERMS = Set.of(
            // mechanics
            "craft", "crafting", "recipe", "recipes", "smelt", "smelting", "furnace", "brew",
            "brewing", "potion", "potions", "enchant", "enchanting", "enchantment", "enchantments",
            "anvil", "grindstone", "smithing", "template", "templates", "upgrade", "repair",
            "mending", "unbreaking", "fortune", "silk", "looting", "sharpness", "efficiency",
            "protection", "feather", "falling", "infinity", "power", "punch", "flame", "loyalty",
            "riptide", "channeling", "trade", "trading", "trades", "villager", "villagers",
            "emerald", "emeralds", "xp", "experience", "level", "levels", "spawn", "spawner",
            "spawners", "respawn", "bed", "beds", "sleep", "nether", "end", "overworld", "portal",
            "portals", "stronghold", "fortress", "bastion", "bastions", "monument", "village",
            "mansion", "temple", "ruins", "ancient", "city", "deep", "dark", "biome", "biomes",
            "mine", "mining", "farm", "farming", "breed", "breeding", "tame", "taming", "fish",
            "fishing", "hunger", "food", "eat", "health", "hearts", "armor", "armour", "shield",
            "elytra", "fly", "flying", "glide", "rocket", "rockets", "firework", "fireworks",
            "redstone", "piston", "pistons", "hopper", "hoppers", "chest", "chests", "barrel",
            "shulker", "inventory", "stack", "stacks", "block", "blocks", "ore", "ores",
            // materials and items
            "diamond", "diamonds", "netherite", "iron", "gold", "golden", "copper", "coal",
            "lapis", "quartz", "obsidian", "glowstone", "amethyst", "wood", "log", "logs",
            "plank", "planks", "stone", "cobblestone", "deepslate", "sand", "gravel", "clay",
            "wool", "string", "leather", "book", "books", "bookshelf", "bookshelves", "paper",
            "sugar", "cane", "wheat", "seeds", "carrot", "carrots", "potato", "potatoes",
            "berries", "bread", "cake", "sword", "swords", "axe", "axes", "pickaxe", "pickaxes",
            "shovel", "hoe", "bow", "bows", "arrow", "arrows", "crossbow", "crossbows", "trident",
            "mace", "maces", "helmet", "chestplate", "leggings", "boots", "tool", "tools",
            "weapon", "weapons", "compass", "map", "maps", "clock", "saddle", "lead", "nametag",
            "torch", "torches", "lantern", "bucket", "water", "lava", "boat", "minecart", "rail",
            "rails", "totem", "pearl", "pearls", "eye", "eyes", "skull", "skulls", "egg", "eggs",
            "horse", "horses", "wolf", "dog", "cat", "fox", "parrot", "axolotl", "turtle",
            "bee", "bees", "sheep", "cow", "cows", "pig", "chicken", "llama",
            "build", "buildmode", "ender", "beacon", "conduit", "membrane", "honey", "honeycomb", "scute", "sniffer",
            "camel", "armadillo", "breeze", "lodestone", "spyglass", "bundle", "crafter",
            "berry", "netherrack", "basalt", "soul", "warped", "crimson", "mushroom",
            // mobs
            "mob", "mobs", "zombie", "zombies", "skeleton", "skeletons", "creeper", "creepers",
            "spider", "spiders", "enderman", "endermen", "witch", "slime", "phantom", "phantoms",
            "blaze", "blazes", "ghast", "piglin", "piglins", "hoglin", "pillager", "pillagers",
            "illager", "illagers", "ravager", "evoker", "vex", "drowned", "guardian", "guardians",
            "dragon", "wither", "warden", "elder", "raid", "raids", "omen", "boss", "bosses",
            "elite", "elites", "elitemobs",
            // the valley's own systems
            "claim", "claims", "claimblocks", "griefprevention", "trust", "shop", "shops",
            "store", "sell", "buy", "money", "coins", "balance", "pay", "jobs", "job", "mcmmo",
            "skill", "skills", "ability", "abilities", "acrobatics", "alchemy", "archery",
            "excavation", "herbalism", "unarmed", "woodcutting", "magic", "wand",
            "wands", "spell", "spells", "spellbook", "mana", "cast", "seal", "seals", "guild",
            "quartermaster", "menu", "warp", "warps", "tpa", "teleport", "home", "homes",
            "party", "parties", "leaderboard", "website", "discord", "server", "bedrock",
            "java", "rank", "ranks", "kit", "kits", "trash", "waypoint", "waypoints",
            "cosmetics", "guide", "command", "commands", "plugin", "plugins", "report");

    /**
     * Spoiler phrases (Mike's learn-by-dying rule, Oct 24, 2026): the
     * GO-CHECKLIST spoiler grep terms, plus the fight-detail phrases the
     * Oct 24 site spoiler cleanup removed. Normalized form.
     */
    static final List<String> DEFAULT_SPOILER_PHRASES = List.of(
            "seal resists", "sealbound", "health pool", "health pools", "gravewhisper",
            "bosstrial status", "second phase", "sealed near", "first phase", "two phase",
            "two phases", "rises again", "rise again", "fair fight", "fair fights",
            "seal fight rules", "unearned", "6 hour cooldown", "six hour cooldown",
            "arena lock", "movement spells are sealed", "half the boss", "boss health",
            "180 seconds", "raid timer", "lock timer", "winner check");

    /** Seal-boss context words ... */
    static final Set<String> DEFAULT_SPOILER_BOSS_WORDS = Set.of(
            "dragon", "wither", "warden", "guardian", "guardians", "elder", "raid", "raids",
            "raider", "raiders", "seal", "seals", "boss", "bosses", "bosstrial", "elitemobs",
            "omen", "crystal", "crystals", "endermen");

    /** ... that make a spoiler only alongside one of these fight-mechanic words. */
    static final Set<String> DEFAULT_SPOILER_MECHANIC_WORDS = Set.of(
            "phase", "phases", "hp", "health", "heal", "heals", "healing", "rally", "rallies",
            "resist", "resists", "rises", "rise", "lock", "locks", "locked", "timer", "timers",
            "cooldown", "cooldowns", "immune", "immunity", "sealed", "glow", "glowing", "wave",
            "waves", "enrage", "enraged", "ability", "abilities", "attack", "attacks",
            "damage", "minion", "minions", "summons", "stronger", "tougher", "weakness",
            "weak", "weakspot", "percent", "mechanic", "mechanics", "trick", "cheese",
            "strategy", "stage", "stages", "invulnerable", "regen", "regenerates");

    /** Social or personal questions aimed at another player, not at how the game works. */
    static final List<String> DEFAULT_SOCIAL_PHRASES = List.of(
            "do you wanna", "do u wanna", "you wanna", "u wanna", "wanna come", "wanna see",
            "wanna play", "do you want", "do u want", "you want", "want it", "want some",
            "have you seen", "did you see", "do you see", "can you see", "you been",
            "are you", "r u", "are u", "where are you", "where r u", "where you",
            "why do you keep", "can you come", "can u come", "come here", "can you send",
            "can you help me", "can you give me", "can i have", "can i come", "what are you doing",
            "what r u doing", "did you", "did u", "you there", "anyone there", "anyone on",
            "who is on", "whos on", "who wants", "how are you", "you mean", "u mean",
            "want to play", "want to come", "your friends", "any of your", "you guys want",
            "u guys want", "yall want");

    /** Chit-chat, hedges and narration: never an answer. Matched as padded phrases. */
    static final List<String> DEFAULT_CHATTER_PHRASES = List.of(
            "lol", "lmao", "lmfao", "rofl", "haha", "hahaha", "hehe", "xd", "brb", "afk",
            "gg", "bruh", "bro", "omg", "omw", "idk", "idc", "ikr", "nvm", "jk", "smh",
            "come back", "my brain", "brain is fried", "i fell", "fell down", "this place",
            "creepy", "im going", "i am going", "going back", "be right back", "one sec",
            "hold on", "i dont know", "i dont think", "not sure", "no idea", "maybe",
            "i guess", "probably", "i think so", "i found", "i got", "i was", "i have",
            "i added", "i made", "wait for me", "follow me", "look at this", "check this",
            "guess what", "never mind", "good night", "goodnight", "gtg", "got to go",
            "see you", "see ya", "cya", "bye", "hello", "hi guys", "hey guys", "glitch",
            "lag", "laggy", "lagging", "who cares");

    /** First words that open chatter or narration rather than an answer. */
    static final Set<String> CHATTER_OPENERS = Set.of(
            "i", "im", "ive", "id", "ill", "me", "my", "mine", "we", "were", "and", "but",
            "btw", "lol", "ok", "okay", "oh", "bro", "bruh", "yo", "hey", "hi", "wait",
            "omg", "wow", "dude", "so", "um", "uh", "uhh", "umm", "hmm", "yeah",
            "nah", "thanks", "thank", "ty", "thx", "ooh", "oooh", "ohh", "welp", "dang",
            "damn", "sorry", "lmao", "haha");

    /** Strict agreement: a short line that only backs what was said. */
    static final Set<String> AGREE_OPENERS = Set.of(
            "yes", "yeah", "yep", "yup", "correct", "exactly", "true", "agreed", "agree",
            "confirmed", "confirm", "same", "ya", "yea");
    static final List<String> AGREE_PHRASES = List.of(
            "thats right", "that is right", "thats true", "thats correct", "that is correct",
            "can confirm", "it does", "that works", "they do", "you do", "it is", "he is right",
            "she is right", "theyre right", "yes it does", "yes you do", "100 percent");

    /** Strict asker confirmation: thanks, or it worked. */
    static final Set<String> THANKS_TOKENS = Set.of(
            "thanks", "thank", "thx", "ty", "tysm", "thankyou", "tyvm");
    static final List<String> WORKED_PHRASES = List.of(
            "that worked", "it worked", "it works", "works now", "got it", "that helped",
            "found it", "that did it", "it did work", "it does work", "that fixed it");

    /** Configurable lists, already merged with the defaults above. */
    public record Lists(Set<String> gameTerms, List<String> spoilerPhrases,
                        Set<String> spoilerBossWords, Set<String> spoilerMechanicWords,
                        Set<String> blockedWords, Set<String> blockedNames,
                        List<String> socialPhrases, List<String> chatterPhrases) {}

    /** Defaults plus the config additions (each config list ADDS to its default). */
    public static Lists lists(Collection<String> extraGameTerms, Collection<String> extraSpoilerTerms,
                              Collection<String> extraBlockedWords, Collection<String> extraNames,
                              Collection<String> extraSocial, Collection<String> extraChatter) {
        Set<String> game = new LinkedHashSet<>(DEFAULT_GAME_TERMS);
        addNorm(game, extraGameTerms);
        List<String> spoil = new ArrayList<>(DEFAULT_SPOILER_PHRASES);
        addNorm(spoil, extraSpoilerTerms);
        Set<String> blocked = new LinkedHashSet<>();
        addNorm(blocked, extraBlockedWords);
        Set<String> names = new LinkedHashSet<>();
        if (extraNames != null) {
            for (String n : extraNames) {
                names.addAll(nameTokens(n));
            }
        }
        List<String> social = new ArrayList<>(DEFAULT_SOCIAL_PHRASES);
        addNorm(social, extraSocial);
        List<String> chatter = new ArrayList<>(DEFAULT_CHATTER_PHRASES);
        addNorm(chatter, extraChatter);
        return new Lists(Set.copyOf(game), List.copyOf(spoil), DEFAULT_SPOILER_BOSS_WORDS,
                DEFAULT_SPOILER_MECHANIC_WORDS, Set.copyOf(blocked), Set.copyOf(names),
                List.copyOf(social), List.copyOf(chatter));
    }

    public static Lists defaults() {
        return lists(null, null, null, null, null, null);
    }

    private static void addNorm(Collection<String> into, Collection<String> extra) {
        if (extra == null) {
            return;
        }
        for (String s : extra) {
            String n = TextNorm.normalize(s);
            if (!n.isBlank()) {
                into.add(n);
            }
        }
    }

    private final Lists lists;
    private final int minAnswerChars;
    private final int minAnswerWords;

    public PeerFilter(Lists lists, int minAnswerChars, int minAnswerWords) {
        this.lists = lists == null ? defaults() : lists;
        this.minAnswerChars = Math.max(1, minAnswerChars);
        this.minAnswerWords = Math.max(1, minAnswerWords);
    }

    public Lists lists() {
        return lists;
    }

    // ---------- topic words ----------

    /** Meaningful words: query terms (no stopwords) minus generic words, light-stemmed. */
    public static Set<String> meaningful(String norm) {
        Set<String> out = new LinkedHashSet<>();
        for (String t : TextNorm.queryTerms(norm)) {
            if (GENERIC.contains(t) || t.chars().allMatch(Character::isDigit)) {
                continue;
            }
            out.add(stem(t));
        }
        return out;
    }

    /** TextNorm's light stem plus "-es" after x/ch/sh/ss ("foxes" -> "fox", "torches" -> "torch"). */
    public static String stem(String t) {
        if (t.length() > 4 && (t.endsWith("xes") || t.endsWith("ches") || t.endsWith("shes")
                || t.endsWith("sses"))) {
            return t.substring(0, t.length() - 2);
        }
        return TextNorm.stem(t);
    }

    /** Filler that never belongs in a topic key ("u", "guys", "really"...). */
    static final Set<String> KEY_FILLER = Set.of(
            "u", "ur", "ya", "yall", "guys", "guy", "like", "really", "even", "anyone",
            "anybody", "someone", "somebody", "ever", "still", "also", "now", "again",
            "please", "pls", "plz", "ok", "okay", "oh", "lol", "wait", "hey", "yo", "bro");

    /**
     * The serve/re-ask match key: the question's query words (no
     * stopwords, no filler), light-stemmed and sorted. "how do i get
     * a saddle" and "how do you get saddles" share the key "saddle".
     * Blank when nothing is left.
     */
    public static String topicKey(String norm) {
        java.util.TreeSet<String> terms = new java.util.TreeSet<>();
        for (String t : TextNorm.queryTerms(norm)) {
            if (!KEY_FILLER.contains(t)) {
                terms.add(stem(t));
            }
        }
        return String.join(" ", terms);
    }

    public static int overlap(Set<String> a, Set<String> b) {
        int n = 0;
        for (String s : a) {
            if (b.contains(s)) {
                n++;
            }
        }
        return n;
    }

    public boolean namesGameTerm(Set<String> tokens) {
        return gameTermCount(tokens) > 0;
    }

    /** Distinct game words in a line (a word and its plural count once). */
    public int gameTermCount(Set<String> tokens) {
        Set<String> hit = new LinkedHashSet<>();
        for (String t : tokens) {
            if (lists.gameTerms().contains(t) || lists.gameTerms().contains(stem(t))) {
                hit.add(stem(t));
            }
        }
        return hit.size();
    }

    // ---------- screens ----------

    /** Seal-fight mechanics (learn-by-dying rule): a spoiler phrase, or a boss word with a mechanic word. */
    public boolean isSpoiler(String norm) {
        if (norm == null || norm.isBlank()) {
            return false;
        }
        String padded = " " + norm + " ";
        for (String p : lists.spoilerPhrases()) {
            if (padded.contains(" " + p + " ")) {
                return true;
            }
        }
        Set<String> tokens = TextNorm.tokenSet(norm);
        boolean boss = false;
        boolean mech = false;
        for (String t : tokens) {
            String s = TextNorm.stem(t);
            if (lists.spoilerBossWords().contains(t) || lists.spoilerBossWords().contains(s)) {
                boss = true;
            }
            if (lists.spoilerMechanicWords().contains(t) || lists.spoilerMechanicWords().contains(s)) {
                mech = true;
            }
        }
        return boss && mech;
    }

    public boolean hasProfanity(Set<String> tokens) {
        for (String t : tokens) {
            if (PeerGuard.isProfane(t) || lists.blockedWords().contains(t)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The name tokens of a display name: the whole normalized name
     * squashed ("Quiet_Otter" -> "quietotter"), and each part of three
     * or more letters that is not itself a game word ("quiet" counts,
     * "otter" would not if it were a mob). Bedrock's leading dot is
     * dropped by normalization.
     */
    public static Set<String> nameTokens(String name) {
        Set<String> out = new LinkedHashSet<>();
        String n = TextNorm.normalize(name);
        if (n.isBlank()) {
            return out;
        }
        out.add(n.replace(" ", ""));
        for (String part : n.split(" ")) {
            if (part.length() >= 3 && !DEFAULT_GAME_TERMS.contains(part)
                    && !TextNorm.STOPWORDS.contains(part) && !GENERIC.contains(part)) {
                out.add(part);
            }
        }
        // 0.3.4: also short forms (camelCase / digit parts, runs of
        // parts, leading letters of a one-part name). Added on top of
        // the 0.3.3 words, so nothing that matched before stops matching.
        out.addAll(NameMatch.tokens(name));
        return out;
    }

    /**
     * Chat nicknames by lower-case player name (0.3.4). The engine owns
     * the map and keeps it current from the server glue; may be empty.
     */
    private java.util.Map<String, String> nicknames = java.util.Map.of();

    public void setNicknames(java.util.Map<String, String> nicknames) {
        this.nicknames = nicknames == null ? java.util.Map.of() : nicknames;
    }

    /** True when the line names any known player (online, seen in chat, or a configured name). */
    public boolean namesPlayer(Set<String> tokens, Collection<String> knownNames) {
        for (String t : tokens) {
            if (lists.blockedNames().contains(t)
                    || (t.endsWith("s") && lists.blockedNames().contains(t.substring(0, t.length() - 1)))) {
                return true;
            }
        }
        if (knownNames == null) {
            return false;
        }
        for (String name : knownNames) {
            if (name == null) {
                continue;
            }
            for (String nt : nameTokens(name)) {
                if (tokens.contains(nt) || tokens.contains(nt + "s")) { // "owl's" -> "owls"
                    return true;
                }
            }
            String nick = nicknames.get(NameMatch.key(name));
            if (nick != null && NameMatch.matches(tokens, NameMatch.tokens(name, nick))) {
                return true; // 0.3.4: the player's chat nickname
            }
        }
        return false;
    }

    private static boolean containsPhrase(String norm, List<String> phrases) {
        String padded = " " + norm + " ";
        for (String p : phrases) {
            if (padded.contains(" " + p + " ")) {
                return true;
            }
        }
        return false;
    }

    // ---------- gates ----------

    /**
     * May this question open a capture window? Null = yes; otherwise
     * the reason slug for the peer log.
     */
    public String questionReject(String raw, String norm, Collection<String> knownNames) {
        if (norm == null || norm.isBlank()) {
            return "q-empty";
        }
        if (!TextNorm.questionShaped(raw, norm)) {
            return "q-not-a-question";
        }
        Set<String> tokens = TextNorm.tokenSet(norm);
        if (tokens.size() < 3 || meaningful(norm).isEmpty() || topicKey(norm).isBlank()) {
            return "q-too-short";
        }
        if (hasProfanity(tokens)) {
            return "q-profanity";
        }
        if (containsPhrase(norm, lists.socialPhrases())) {
            return "q-social";
        }
        if (namesPlayer(tokens, knownNames)) {
            return "q-names-player";
        }
        if (isSpoiler(norm)) {
            return "q-spoiler";
        }
        if (!namesGameTerm(tokens)) {
            return "q-no-game-term";
        }
        return null;
    }

    /**
     * May this chat line become a candidate answer to the question
     * whose meaningful words are {@code questionTerms}? Null = yes.
     */
    public String answerReject(String raw, String cleaned, Set<String> questionTerms,
                               int maxChars, Collection<String> knownNames, String speakerName) {
        if (cleaned == null || cleaned.isBlank()) {
            return "too-short";
        }
        String norm = TextNorm.normalize(cleaned);
        Set<String> tokens = TextNorm.tokenSet(norm);
        if (cleaned.trim().length() < minAnswerChars || tokens.size() < minAnswerWords
                || meaningful(norm).isEmpty()) {
            return "too-short";
        }
        if (TextNorm.questionShaped(raw, norm)) {
            return "question";
        }
        if (cleaned.trim().length() > Math.max(maxChars, 1)) {
            return "too-long";
        }
        String first = norm.split(" ", 2)[0];
        if (CHATTER_OPENERS.contains(first) || containsPhrase(norm, lists.chatterPhrases())) {
            return "chatter";
        }
        if (hasProfanity(tokens)) {
            return "profanity";
        }
        if (namesPlayer(tokens, knownNames)) {
            return "names-player";
        }
        if (isSpoiler(norm)) {
            return "spoiler";
        }
        // Relevance: shares a meaningful word with the question, or
        // (for answers in other words) names at least two game words.
        Set<String> answerTerms = meaningful(norm);
        if (overlap(answerTerms, questionTerms) == 0 && gameTermCount(tokens) < 2) {
            return "off-topic";
        }
        // The 0.2.3 shape + privacy screens still apply (personal stats,
        // stat content, another player's numbers, over-long rants).
        PeerGuard.Reject old = PeerGuard.check(raw, cleaned, maxChars, knownNames, speakerName);
        if (old != null) {
            return old.slug();
        }
        return null;
    }

    /** Serve-time re-check of an approved answer. Null = may be served. */
    public String serveReject(String answer, Collection<String> knownNames) {
        String norm = TextNorm.normalize(answer);
        Set<String> tokens = TextNorm.tokenSet(norm);
        if (norm.isBlank()) {
            return "empty";
        }
        if (hasProfanity(tokens)) {
            return "profanity";
        }
        if (namesPlayer(tokens, knownNames)) {
            return "names-player";
        }
        if (isSpoiler(norm)) {
            return "spoiler";
        }
        return null;
    }

    /** A short line that only backs the standing answer ("yeah thats right", "yep"). */
    public static boolean isAgreement(String raw, String norm) {
        if (norm == null || norm.isBlank() || TextNorm.questionShaped(raw, norm)) {
            return false;
        }
        Set<String> tokens = TextNorm.tokenSet(norm);
        if (tokens.size() > 6) {
            return false;
        }
        if (PeerGuard.isDisagreement(norm, tokens)) {
            return false;
        }
        String first = norm.split(" ", 2)[0];
        if (AGREE_OPENERS.contains(first)) {
            return true;
        }
        return containsPhrase(norm, AGREE_PHRASES);
    }

    /** The asker's thanks or "that worked" - nothing looser ("yeah" no longer counts). */
    public static boolean isAskerConfirmation(String raw, String norm) {
        if (norm == null || norm.isBlank() || TextNorm.questionShaped(raw, norm)) {
            return false;
        }
        Set<String> tokens = TextNorm.tokenSet(norm);
        if (tokens.size() > 10 || PeerGuard.isDisagreement(norm, tokens)) {
            return false;
        }
        for (String t : tokens) {
            if (THANKS_TOKENS.contains(t)) {
                return true;
            }
        }
        return containsPhrase(norm, WORKED_PHRASES);
    }

    public static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}
