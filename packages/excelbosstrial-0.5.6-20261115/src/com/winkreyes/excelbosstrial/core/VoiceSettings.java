/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;
import java.util.Optional;

/**
 * One chapter's boss-voice settings (ExcelBossTrial 0.5.2):
 * the clip slugs for every voice seam of the chapter, read
 * adapter-side from the chapter's config keys like the
 * milestone and taunt leaf settings (no core verdict rule
 * consumes them, so BossTrialConfig's canonical record stays
 * untouched).
 *
 * Config shape (bundled config.yml carries the settled slate):
 *   &lt;chapter&gt;.taunt-sounds   - index-aligned with &lt;chapter&gt;.taunts
 *   &lt;chapter&gt;.voice.open / .flip / .beat-75 / .beat-50 / .beat-25 /
 *            .beat-50-phase2 / .addwave / .relay / .lastdoor /
 *            .boom / .boom-alt / .verdict
 *
 * Every value is a clip SLUG (the pack file stem); the event
 * id is BossVoice's contract. An absent key keeps the chapter
 * default below; a present-but-empty (or malformed) value is
 * an honored silence at that seam - the milestone-line
 * precedent. The defaults ARE the DESIGN section 5 line sets
 * as rendered into the frozen packs: 32 wired clips (wither
 * 10 taunts, dragon 8, elder 7, warden 7). The 6 raid clips
 * are deliberately absent - no BossKind names the raid, and
 * their playback rides a later GuildGate packet.
 */
public record VoiceSettings(String open, String flip,
                            String beat75, String beat50, String beat25,
                            String beat50Phase2, String addwave,
                            String relay, String lastdoor,
                            String boom, String boomAlt, String verdict,
                            List<String> tauntSounds) {

    /** The slug for a milestone stamp: threshold index 0/1/2
     *  is the 75/50/25% stamp. The dragon's phase-2 50% stamp
     *  has its own clip (beat50p2, DESIGN section 5.2 - the
     *  crystals line); it falls back to the plain 50% slug
     *  when the phase-2 entry is silenced. Every other stamp
     *  reads the same slug in either phase. */
    public String beatSlug(int thresholdIndex, boolean phase2) {
        return switch (thresholdIndex) {
            case 0 -> beat75;
            case 1 -> phase2 && !beat50Phase2.isEmpty() ? beat50Phase2 : beat50;
            case 2 -> beat25;
            default -> "";
        };
    }

    /** The event for a milestone stamp, or empty when that
     *  stamp is silent for this chapter (the Wither ships no
     *  beat clips at all - its voice is the eats). */
    public Optional<String> beatEvent(BossKind kind, int thresholdIndex, boolean phase2) {
        return BossVoice.eventId(kind, beatSlug(thresholdIndex, phase2));
    }

    /** How many clips this chapter can actually speak: the
     *  non-empty seam slugs plus the non-empty taunt slots.
     *  The boot summary's "armed" count. */
    public int clipCount() {
        int n = 0;
        for (String s : new String[] {open, flip, beat75, beat50, beat25,
                beat50Phase2, addwave, relay, lastdoor, boom, boomAlt, verdict}) {
            if (s != null && !s.isEmpty()) {
                n++;
            }
        }
        for (String s : tauntSounds) {
            if (s != null && !s.isEmpty()) {
                n++;
            }
        }
        return n;
    }

    /** Armed = at least one clip can sound for this chapter. */
    public boolean armed() {
        return clipCount() > 0;
    }

    /**
     * Sanitized construction: every seam slug passes
     * BossVoice.sanitizeSlug (junk reads as silence at that
     * seam, never as a guessed clip), and the taunt-sounds
     * list passes the position-preserving alignment rule.
     * Null seam values read as the fallback's (the key was
     * absent); a present empty string stays empty.
     */
    public static VoiceSettings sanitized(String open, String flip,
                                          String beat75, String beat50, String beat25,
                                          String beat50Phase2, String addwave,
                                          String relay, String lastdoor,
                                          String boom, String boomAlt, String verdict,
                                          List<String> tauntSounds,
                                          VoiceSettings fallback) {
        return new VoiceSettings(
                pick(open, fallback.open), pick(flip, fallback.flip),
                pick(beat75, fallback.beat75), pick(beat50, fallback.beat50),
                pick(beat25, fallback.beat25),
                pick(beat50Phase2, fallback.beat50Phase2),
                pick(addwave, fallback.addwave), pick(relay, fallback.relay),
                pick(lastdoor, fallback.lastdoor), pick(boom, fallback.boom),
                pick(boomAlt, fallback.boomAlt), pick(verdict, fallback.verdict),
                BossVoice.sanitizeAligned(tauntSounds, fallback.tauntSounds));
    }

    private static String pick(String raw, String fallback) {
        return raw == null ? BossVoice.sanitizeSlug(fallback)
                : BossVoice.sanitizeSlug(raw);
    }

    /** The settled defaults per chapter - the DESIGN section 5
     *  line sets as rendered into the frozen packs. Chapters
     *  carry empty slugs for seams that are not theirs (the
     *  Wither has no open/flip/beat clips; only the dragon
     *  flips; only the elder relays; only the warden booms). */
    public static VoiceSettings defaults(BossKind kind) {
        return switch (kind) {
            case WITHER -> new VoiceSettings("", "", "", "", "", "", "",
                    "", "", "", "", "", BossVoice.DEFAULT_WITHER_TAUNT_SOUNDS);
            case ENDER_DRAGON -> new VoiceSettings("open", "flip",
                    "beat75", "beat50", "beat25", "beat50p2", "addwave",
                    "", "", "", "", "verdict", List.of());
            case ELDER_GUARDIAN -> new VoiceSettings("open", "",
                    "beat75", "beat50", "beat25", "", "",
                    "relay", "lastdoor", "", "", "verdict", List.of());
            case WARDEN -> new VoiceSettings("open", "",
                    "beat75", "beat50", "beat25", "", "",
                    "", "", "boom", "boom_alt", "verdict", List.of());
        };
    }
}
