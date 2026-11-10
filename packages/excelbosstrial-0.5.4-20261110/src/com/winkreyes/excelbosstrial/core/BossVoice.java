/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Boss voice playback resolution (ExcelBossTrial 0.5.2, the
 * combined Item 35 build: the boss-voice lane of record -
 * ~/workspace/boss-voice-20261108/DESIGN.md sections 2.2/4/5,
 * decisions ruled Nov 9, 2026 - rebased onto Packet B 0.5.1 per
 * ~/workspace/seal-fight-packet-b-20261109/RECONCILIATION.md).
 *
 * This class is the pure contract between the fight seams and
 * the two resource packs. A line of text becomes a sound EVENT
 * ID of the exact form both packs were built to:
 *
 *     wrv:bosstrial.&lt;boss&gt;.&lt;slug&gt;
 *
 * That form is the GuildGate narrator precedent, verified in
 * that plugin's shipped source (ExcelGuildGate plays the plain
 * string "wrv:guildgate.sealN" through Player.playSound): on
 * Java the id names the event registered under assets/wrv/
 * sounds.json in the merged pack (43 events, frozen hash in the
 * 0.5.2 packet), and on Bedrock the same string is the key in
 * the mcpack's sound_definitions.json, whose boss definitions
 * carry is3D: true - so Geyser forwards the position and the
 * clip attenuates from the boss (Mike's ruling: playback AT
 * THE BOSS, positional; the raid herald clips ride the packs
 * inert and are NOT wired here - their playback home is a
 * later GuildGate packet).
 *
 * Fail-soft is the law of this unit, mirroring the taunt
 * design: a slug that is blank or malformed resolves to NO
 * event (the seam stays chat-only), never to a guessed id.
 * Slug sanitization PRESERVES list positions - the wither's
 * taunt-sounds list is index-aligned with its taunts list, so
 * a junk entry becomes an empty slot at the same index (that
 * line is chat-only) instead of shifting every later line
 * onto the wrong clip.
 *
 * Nothing in here touches Bukkit, timers, or state: voice
 * fires only where a seam calls it, on the seam's real event.
 */
public final class BossVoice {

    /** The event-id prefix every boss clip shares. */
    public static final String EVENT_PREFIX = "wrv:bosstrial.";

    /** The shipped Wither taunt clips, index-aligned with
     *  TauntSettings.DEFAULT_WITHER_LINES (DESIGN section 5.1:
     *  the voice slate is the shipped chat slate, verbatim, in
     *  the same order - eat_01 is "MORE.", eat_05 is "Feed me,
     *  champions.", and so on). */
    public static final List<String> DEFAULT_WITHER_TAUNT_SOUNDS = List.of(
            "eat_01", "eat_02", "eat_03", "eat_04", "eat_05",
            "eat_06", "eat_07", "eat_08", "eat_09", "eat_10");

    private BossVoice() {
    }

    /** The pack's boss segment for a chapter: the elder
     *  chapter's segment is "elder" (the packs' folder and
     *  event names), not the config section name. */
    public static String bossSlug(BossKind kind) {
        if (kind == null) {
            return "";
        }
        return switch (kind) {
            case WITHER -> "wither";
            case ENDER_DRAGON -> "dragon";
            case ELDER_GUARDIAN -> "elder";
            case WARDEN -> "warden";
        };
    }

    /** A clip slug is a lowercase pack file stem: letters,
     *  digits and underscores only (eat_01, beat50p2,
     *  boom_alt). Anything else can never name a clip in
     *  either pack. */
    public static boolean isSlug(String raw) {
        if (raw == null || raw.isEmpty()) {
            return false;
        }
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_')) {
                return false;
            }
        }
        return true;
    }

    /** Sanitize one raw config slug: trim and lowercase, then
     *  keep it only if it can name a pack clip; anything else
     *  reads as "" (silent at that seam). */
    public static String sanitizeSlug(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        return isSlug(s) ? s : "";
    }

    /**
     * Sanitize an index-aligned slug list (the wither's
     * taunt-sounds): position is meaning, so the output has
     * EXACTLY the input's length - a blank or malformed entry
     * becomes an empty slot at its own index (that line is
     * chat-only) and never shifts a later line's clip. A null
     * list (the key was absent) keeps the fallback's entries.
     */
    public static List<String> sanitizeAligned(List<String> raw, List<String> fallback) {
        if (raw == null) {
            return fallback == null ? List.of() : List.copyOf(fallback);
        }
        List<String> out = new ArrayList<>(raw.size());
        for (String s : raw) {
            out.add(sanitizeSlug(s));
        }
        return List.copyOf(out);
    }

    /** The full event id for one clip, or empty when the slug
     *  cannot name a clip (the seam stays chat-only). */
    public static Optional<String> eventId(BossKind kind, String slug) {
        String clean = sanitizeSlug(slug);
        if (kind == null || clean.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(EVENT_PREFIX + bossSlug(kind) + "." + clean);
    }

    /**
     * The event for the taunt line at this index of the
     * chapter's taunts list - the index alignment rule: line i
     * speaks taunt-sounds entry i. An index outside the sounds
     * list, or an empty slot at that index, is a MISSING clip:
     * empty here, chat-only at the seam, logged once by the
     * host. The line list itself is never consulted for length
     * games - alignment is judged against the sounds list the
     * config actually carries.
     */
    public static Optional<String> tauntEvent(BossKind kind, VoiceSettings settings,
                                              int lineIndex) {
        if (settings == null || lineIndex < 0
                || lineIndex >= settings.tauntSounds().size()) {
            return Optional.empty();
        }
        return eventId(kind, settings.tauntSounds().get(lineIndex));
    }

    /** True when this taunt index HAS a sounds slot (present
     *  but empty still counts as a slot - it is a missing clip
     *  the host logs once, not a config absence). */
    public static boolean tauntSlotExists(VoiceSettings settings, int lineIndex) {
        return settings != null && lineIndex >= 0
                && lineIndex < settings.tauntSounds().size();
    }
}
