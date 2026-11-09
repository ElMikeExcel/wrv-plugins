/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Boss voice resolution (0.5.2): the pack contract (event-id
 * form, per the GuildGate narrator precedent), slug
 * sanitization, the index-alignment rule for taunt-sounds,
 * the missing-clip fail-soft, the per-phase beat resolution,
 * the settled defaults (32 wired clips), config sanitization,
 * and the TauntBook pick integration the taunt seam rides.
 */
public final class BossVoiceTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    public static void main(String[] args) {
        // --- The pack contract: boss segments + event-id form.
        check(BossVoice.bossSlug(BossKind.WITHER).equals("wither"), "slug: wither");
        check(BossVoice.bossSlug(BossKind.ENDER_DRAGON).equals("dragon"), "slug: dragon");
        check(BossVoice.bossSlug(BossKind.ELDER_GUARDIAN).equals("elder"),
                "slug: the elder chapter's pack segment is elder, not elder-guardian");
        check(BossVoice.bossSlug(BossKind.WARDEN).equals("warden"), "slug: warden");
        check(BossVoice.bossSlug(null).isEmpty(), "slug: null kind is empty");
        check(BossVoice.eventId(BossKind.WITHER, "eat_01")
                        .equals(Optional.of("wrv:bosstrial.wither.eat_01")),
                "event: wither eat_01 carries the wrv:bosstrial. form of the packs");
        check(BossVoice.eventId(BossKind.ENDER_DRAGON, "flip")
                        .equals(Optional.of("wrv:bosstrial.dragon.flip")),
                "event: dragon flip");
        check(BossVoice.eventId(BossKind.ELDER_GUARDIAN, "relay")
                        .equals(Optional.of("wrv:bosstrial.elder.relay")),
                "event: elder relay");
        check(BossVoice.eventId(BossKind.WARDEN, "boom_alt")
                        .equals(Optional.of("wrv:bosstrial.warden.boom_alt")),
                "event: warden boom_alt (underscore slug, as the pack names it)");
        check(BossVoice.eventId(BossKind.WARDEN, "").isEmpty(),
                "event: a blank slug resolves to NO event (chat-only seam)");
        check(BossVoice.eventId(BossKind.WARDEN, "not a slug!").isEmpty(),
                "event: a malformed slug resolves to NO event, never a guessed id");
        check(BossVoice.eventId(null, "open").isEmpty(), "event: null kind is empty");

        // --- Slug sanitization.
        check(BossVoice.sanitizeSlug("  Beat50P2 ").equals("beat50p2"),
                "sanitize: trim + lowercase");
        check(BossVoice.sanitizeSlug("boom-alt").isEmpty(),
                "sanitize: a hyphen can never name a pack clip - silence");
        check(BossVoice.sanitizeSlug("").isEmpty(), "sanitize: empty stays empty");
        check(BossVoice.sanitizeSlug(null).isEmpty(), "sanitize: null reads as silence");
        check(BossVoice.isSlug("eat_10") && !BossVoice.isSlug("eat 10"),
                "isSlug: underscores yes, spaces no");

        // --- Index alignment: sanitization preserves positions.
        List<String> aligned = BossVoice.sanitizeAligned(
                List.of("eat_01", "junk slug!", "eat_03"), List.of("x"));
        check(aligned.size() == 3, "align: the sounds list never changes length");
        check(aligned.get(0).equals("eat_01") && aligned.get(2).equals("eat_03"),
                "align: good entries keep their own index");
        check(aligned.get(1).isEmpty(),
                "align: a junk entry becomes an EMPTY SLOT at its index, not a shift");
        check(BossVoice.sanitizeAligned(null, List.of("eat_01")).equals(List.of("eat_01")),
                "align: an absent list keeps the fallback");
        check(BossVoice.sanitizeAligned(null, null).isEmpty(),
                "align: absent list + no fallback is empty, not an exception");

        // --- The settled defaults: 32 wired clips (10+8+7+7).
        VoiceSettings wither = VoiceSettings.defaults(BossKind.WITHER);
        VoiceSettings dragon = VoiceSettings.defaults(BossKind.ENDER_DRAGON);
        VoiceSettings elder = VoiceSettings.defaults(BossKind.ELDER_GUARDIAN);
        VoiceSettings warden = VoiceSettings.defaults(BossKind.WARDEN);
        check(wither.clipCount() == 10, "defaults: wither wires its 10 taunt clips");
        check(dragon.clipCount() == 8, "defaults: dragon wires 8 clips");
        check(elder.clipCount() == 7, "defaults: elder wires 7 clips");
        check(warden.clipCount() == 7, "defaults: warden wires 7 clips");
        check(wither.clipCount() + dragon.clipCount() + elder.clipCount()
                + warden.clipCount() == 32,
                "defaults: 32 wired clips total - the 6 raid clips are NOT among them");
        check(wither.armed() && dragon.armed() && elder.armed() && warden.armed(),
                "defaults: every chapter is voice-armed");

        // --- Taunt resolution by index (the subtitle's own index).
        check(BossVoice.tauntEvent(BossKind.WITHER, wither, 0)
                        .equals(Optional.of("wrv:bosstrial.wither.eat_01")),
                "taunt: line 1 (MORE.) speaks eat_01");
        check(BossVoice.tauntEvent(BossKind.WITHER, wither, 4)
                        .equals(Optional.of("wrv:bosstrial.wither.eat_05")),
                "taunt: line 5 (Feed me, champions.) speaks eat_05");
        check(BossVoice.tauntEvent(BossKind.WITHER, wither, 9)
                        .equals(Optional.of("wrv:bosstrial.wither.eat_10")),
                "taunt: line 10 speaks eat_10");
        check(BossVoice.tauntEvent(BossKind.WITHER, wither, 10).isEmpty(),
                "taunt: an index past the sounds list is a missing clip - empty");
        check(!BossVoice.tauntSlotExists(wither, 10),
                "taunt: slot existence is judged against the sounds list");
        VoiceSettings shortSounds = VoiceSettings.sanitized(null, null, null, null,
                null, null, null, null, null, null, null, null,
                List.of("eat_01", "", "eat_03"), wither);
        check(BossVoice.tauntEvent(BossKind.WITHER, shortSounds, 2)
                        .equals(Optional.of("wrv:bosstrial.wither.eat_03")),
                "taunt: a shorter sounds list still aligns by index");
        check(BossVoice.tauntEvent(BossKind.WITHER, shortSounds, 1).isEmpty()
                        && BossVoice.tauntSlotExists(shortSounds, 1),
                "taunt: an empty slot is a missing clip that EXISTS as a slot (log once)");
        check(BossVoice.tauntEvent(BossKind.WITHER, shortSounds, 5).isEmpty()
                        && !BossVoice.tauntSlotExists(shortSounds, 5),
                "taunt: beyond a short list there is no slot at all");

        // --- Beats: per stamp, per phase.
        check(elder.beatEvent(BossKind.ELDER_GUARDIAN, 0, false)
                        .equals(Optional.of("wrv:bosstrial.elder.beat75")),
                "beat: elder 75% stamp speaks beat75");
        check(elder.beatEvent(BossKind.ELDER_GUARDIAN, 2, false)
                        .equals(Optional.of("wrv:bosstrial.elder.beat25")),
                "beat: elder 25% stamp speaks beat25");
        check(dragon.beatEvent(BossKind.ENDER_DRAGON, 1, false)
                        .equals(Optional.of("wrv:bosstrial.dragon.beat50")),
                "beat: dragon phase-1 50% speaks beat50");
        check(dragon.beatEvent(BossKind.ENDER_DRAGON, 1, true)
                        .equals(Optional.of("wrv:bosstrial.dragon.beat50p2")),
                "beat: dragon phase-2 50% speaks its own clip, beat50p2");
        check(dragon.beatEvent(BossKind.ENDER_DRAGON, 0, true)
                        .equals(Optional.of("wrv:bosstrial.dragon.beat75")),
                "beat: dragon phase-2 75% reuses beat75 (the pack ships no p2 variant)");
        VoiceSettings dragonNoP2 = VoiceSettings.sanitized(null, null, null, null,
                null, "", null, null, null, null, null, null, null, dragon);
        check(dragonNoP2.beatEvent(BossKind.ENDER_DRAGON, 1, true)
                        .equals(Optional.of("wrv:bosstrial.dragon.beat50")),
                "beat: a silenced beat-50-phase2 falls back to beat50, never to nothing-by-accident");
        check(dragon.beatEvent(BossKind.ENDER_DRAGON, 3, false).isEmpty(),
                "beat: an out-of-range stamp index is silent");
        check(wither.beatEvent(BossKind.WITHER, 0, false).isEmpty()
                        && wither.beatEvent(BossKind.WITHER, 1, false).isEmpty()
                        && wither.beatEvent(BossKind.WITHER, 2, false).isEmpty(),
                "beat: the Wither ships NO beat clips - its voice is the eats alone");

        // --- Config sanitization of the voice keys.
        VoiceSettings honored = VoiceSettings.sanitized("", null, null, null, null,
                null, null, null, null, null, null, null, null, dragon);
        check(honored.open().isEmpty() && honored.flip().equals("flip"),
                "config: a present-but-empty value is an honored silence; absent keeps the default");
        VoiceSettings shouted = VoiceSettings.sanitized(null, " FLIP ", null, null,
                null, null, null, null, null, null, null, null, null, dragon);
        check(shouted.flip().equals("flip"),
                "config: a slug is normalized (trim/lowercase) rather than failing the seam");
        VoiceSettings junkVerdict = VoiceSettings.sanitized(null, null, null, null,
                null, null, null, null, null, null, null, "not a slug!", null, dragon);
        check(BossVoice.eventId(BossKind.ENDER_DRAGON, junkVerdict.verdict()).isEmpty(),
                "config: a malformed verdict slug leaves the verdict seam chat-only");

        // --- The TauntBook pick integration the seam rides.
        TauntBook book = new TauntBook();
        UUID boss = UUID.randomUUID();
        Optional<TauntBook.Pick> pick = book.onEatPick(boss, 1_000L,
                TauntSettings.DEFAULT_WITHER_LINES, 25_000L, () -> 4);
        check(pick.isPresent() && pick.get().index() == 4
                        && pick.get().line().equals("Feed me, champions."),
                "pick: the book's pick carries its line AND its index");
        check(BossVoice.tauntEvent(BossKind.WITHER, wither, pick.get().index())
                        .equals(Optional.of("wrv:bosstrial.wither.eat_05")),
                "pick: the picked line's index resolves its own clip");
        Optional<TauntBook.Pick> swallowed = book.onEatPick(boss, 2_000L,
                TauntSettings.DEFAULT_WITHER_LINES, 25_000L, () -> 0);
        check(swallowed.isEmpty(),
                "pick: a second eat inside the cooldown speaks in NEITHER track");
        TauntBook book2 = new TauntBook();
        UUID boss2 = UUID.randomUUID();
        Optional<TauntBook.Pick> first = book2.onEatPick(boss2, 0L,
                TauntSettings.DEFAULT_WITHER_LINES, 0L, () -> 4);
        Optional<TauntBook.Pick> second = book2.onEatPick(boss2, 1L,
                TauntSettings.DEFAULT_WITHER_LINES, 0L, () -> 4);
        check(first.isPresent() && second.isPresent()
                        && second.get().index() != first.get().index(),
                "pick: the no-repeat advance moves the index, so the clip varies with the line");
        check(book.onEat(boss, 30_000L, TauntSettings.DEFAULT_WITHER_LINES,
                        25_000L, () -> 1).isPresent(),
                "pick: the legacy onEat still speaks (it delegates to the pick)");

        System.out.println("BossVoiceTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
