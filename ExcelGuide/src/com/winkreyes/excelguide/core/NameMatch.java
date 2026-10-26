package com.winkreyes.excelguide.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Player-name matching for the "is this line talking to a player?" gate
 * (0.3.4). Before 0.3.4 a name only counted when chat used the WHOLE
 * gamertag as one word, so a short form ("hey Quiet, ...") or a chat
 * nickname slipped through, reached the AI and could get a public
 * Guide answer after the delayed-answer wait.
 *
 * <p>Rules (all case-insensitive, whole words only - a name never
 * matches inside another word):
 * <ul>
 *   <li>The whole name, squashed ("Quiet_Otter" -&gt; "quietotter"),
 *       always counts (unchanged from 0.3.3).</li>
 *   <li>The name is cut into parts at underscores, dots, spaces,
 *       digits and camelCase humps ("QuietOtter42" -&gt; quiet, otter).
 *       Each part of {@link #MIN_PART} or more letters counts, unless
 *       it is a game word, a stopword or a common word
 *       ({@link #COMMON}).</li>
 *   <li>Runs of neighbouring parts count too ("BigQuietOtter" -&gt;
 *       bigquiet, quietotter), so "BigQuiet" works even though "big"
 *       alone is too common.</li>
 *   <li>A one-part lowercase name ("quietotter") cannot be cut, so its
 *       leading letters count from {@link #MIN_PREFIX} letters
 *       ("quie", "quiet", ...) with the same word screens.</li>
 *   <li>Nicknames (the Essentials nickname or display name) go through
 *       the same rules after colours, bracket tags and the leading "~"
 *       are stripped.</li>
 *   <li>A trailing "s" is allowed for the possessive ("quiets").</li>
 * </ul>
 * Tradeoffs: a name part that is also a common word or a game word
 * (cool, king, fox, creeper) never counts on its own, so "is this cool?"
 * still reaches the Guide; a two-letter short form is too short to
 * count. A short real-name form (for example the owner's first name)
 * DOES count while that player is online or was seen in chat, so
 * "yo [name], what do you think?" is treated as talking to that player.
 */
public final class NameMatch {

    private NameMatch() {}

    /** Shortest name part that counts on its own. */
    public static final int MIN_PART = 3;
    /** Shortest leading slice of an uncuttable name that counts. */
    public static final int MIN_PREFIX = 4;

    /**
     * Common words that often appear inside gamertags or nicknames but
     * are ordinary chat words; never a name on their own.
     */
    static final Set<String> COMMON = Set.of(
            "the", "and", "for", "you", "your", "not", "but", "all", "any", "can", "has",
            "her", "him", "his", "its", "our", "out", "who", "why", "how", "what", "yes",
            "hey", "lol", "omg", "bro", "sis", "mom", "dad", "man", "men", "boy", "boys",
            "girl", "girls", "kid", "kids", "baby", "dude", "guy", "lady", "king", "queen",
            "lord", "sir", "pro", "noob", "gamer", "gamers", "game", "games", "gaming", "play",
            "player", "players", "plays", "real", "official", "big", "lil", "little",
            "tiny", "mini", "super", "mega", "ultra", "best", "good", "bad", "epic", "cool",
            "nice", "fun", "funny", "happy", "sad", "mad", "love", "life", "live", "world",
            "dark", "light", "red", "blue", "green", "black", "white", "pink", "purple",
            "yellow", "orange", "grey", "gray", "fire", "ice", "star", "stars", "sun", "moon",
            "night", "day", "time", "here", "now", "new", "old", "one", "two", "three",
            "test", "help", "please", "thanks", "thank", "okay", "hello", "bye", "yeah",
            "yep", "nope", "maybe", "mrs", "miss", "master", "boss", "team", "clan", "crew",
            "yt", "ttv", "tv", "xx", "xxx", "mc", "craft", "pvp", "god", "hero", "legend",
            "ninja", "shadow", "storm", "wolf", "cat", "dog", "bear", "lion", "tiger",
            "dragon", "rose", "flower", "candy", "sweet", "honey", "angel", "devil", "ghost",
            "house", "home", "town", "land", "sky", "sea", "ocean", "rain", "snow", "cloud");

    /** True when a word may never be a name on its own. */
    static boolean screened(String w) {
        return w.length() < MIN_PART
                || COMMON.contains(w)
                || TextNorm.STOPWORDS.contains(w)
                || PeerFilter.GENERIC.contains(w)
                || PeerFilter.DEFAULT_GAME_TERMS.contains(w)
                || PeerFilter.THANKS_TOKENS.contains(w)
                || w.chars().allMatch(Character::isDigit);
    }

    /**
     * Clean a display name or nickname: drop section-sign and ampersand
     * colour codes, "[tag]" / "(tag)" prefixes, and leading marks such
     * as Essentials' "~" or Floodgate's ".".
     */
    public static String cleanDisplay(String display) {
        if (display == null) {
            return "";
        }
        String s = display.replaceAll("(?i)[\u00a7&]#[0-9a-f]{6}", "")
                .replaceAll("(?i)[\u00a7&][0-9a-fk-orx]", "");
        s = s.replaceAll("\\[[^\\]]*\\]", " ").replaceAll("\\([^)]*\\)", " ");
        s = s.trim();
        while (!s.isEmpty() && !Character.isLetterOrDigit(s.charAt(0))) {
            s = s.substring(1);
        }
        return s.trim();
    }

    /** Split a raw name at separators, digits and camelCase humps (case kept). */
    static List<String> parts(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        String spaced = raw
                .replaceAll("(?<=[a-z])(?=[A-Z])", " ")
                .replaceAll("(?<=[A-Z])(?=[A-Z][a-z])", " ")
                .replaceAll("(?<=[A-Za-z])(?=[0-9])", " ")
                .replaceAll("(?<=[0-9])(?=[A-Za-z])", " ");
        for (String p : TextNorm.normalize(spaced).split(" ")) {
            if (!p.isBlank()) {
                out.add(p);
            }
        }
        return out;
    }

    /**
     * Every chat word that should count as naming this player. The
     * whole squashed name always counts; other forms go through the
     * word screens. {@code nickname} may be null.
     */
    public static Set<String> tokens(String name, String nickname) {
        String ck = (name == null ? "" : name) + "\n" + (nickname == null ? "" : nickname);
        Set<String> hit = CACHE.get(ck);
        if (hit != null) {
            return hit;
        }
        Set<String> out = new LinkedHashSet<>();
        addName(out, name, true);
        String nick = cleanDisplay(nickname);
        if (!nick.isBlank()) {
            addName(out, nick, false);
        }
        Set<String> frozen = java.util.Collections.unmodifiableSet(out);
        if (CACHE.size() > 2000) {
            CACHE.clear();
        }
        CACHE.put(ck, frozen);
        return frozen;
    }

    private static final java.util.concurrent.ConcurrentHashMap<String, Set<String>> CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** Gamertag only (no nickname). */
    public static Set<String> tokens(String name) {
        return tokens(name, null);
    }

    private static void addName(Set<String> out, String raw, boolean wholeAlways) {
        String whole = TextNorm.normalize(raw).replace(" ", "");
        if (whole.isBlank()) {
            return;
        }
        if (wholeAlways || !screened(whole)) {
            out.add(whole);
        }
        List<String> parts = parts(raw);
        // single parts and runs of neighbouring parts
        for (int i = 0; i < parts.size(); i++) {
            StringBuilder run = new StringBuilder();
            for (int j = i; j < parts.size(); j++) {
                run.append(parts.get(j));
                String r = run.toString();
                if (!screened(r)) {
                    out.add(r);
                }
            }
        }
        // an uncuttable name: its leading letters
        if (parts.size() == 1) {
            String p = parts.get(0);
            for (int k = MIN_PREFIX; k < p.length(); k++) {
                String pre = p.substring(0, k);
                if (!screened(pre)) {
                    out.add(pre);
                }
            }
        }
    }

    /** True when any token (or its possessive "s" form) is one of the name words. */
    public static boolean matches(Set<String> chatTokens, Collection<String> nameWords) {
        if (chatTokens == null || nameWords == null) {
            return false;
        }
        for (String nw : nameWords) {
            if (chatTokens.contains(nw) || chatTokens.contains(nw + "s")) {
                return true;
            }
        }
        return false;
    }

    /** Lower-case key for name maps. */
    static String key(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }
}
