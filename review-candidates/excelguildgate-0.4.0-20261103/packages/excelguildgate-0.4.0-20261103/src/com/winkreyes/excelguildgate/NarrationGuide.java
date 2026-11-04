package com.winkreyes.excelguildgate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Pure narration plan behind the seal-fall narrator (v0.3.0).
 *
 * No Bukkit dependencies, so the whole rule set is unit-testable on a plain
 * JVM (see tests/com/winkreyes/excelguildgate/NarrationGuideTest.java).
 * The plugin parses config.yml into this object once at enable; every
 * decision the fall path and the join path need is answered here, and the
 * plugin only performs the side effects (playsound, chat) the plan calls
 * for. The plan itself never mutates after construction.
 *
 * What the narrator does, in campaign terms: when the Nth seal falls, a
 * recorded voice line (a custom sound from the server resource pack,
 * {@code wrv:guildgate.seal<N>}) plays for everyone online, followed by a
 * short staggered guide for that stage: what opened, the practical need
 * (a wand, a rank, a handout), and the pointer to the website's Skill
 * Trees page. A player who was away hears the voice line for their
 * HIGHEST missed stage once, on the join that delivers their text
 * catch-up; the once-ness is guaranteed by CatchUpTracker advancing the
 * seen-stage before this plan is consulted, never by state kept here.
 *
 * Semantics pinned by the tests:
 * - Stages are 1..5, keyed by the fallen-seal count like the catch-up
 *   beats. Out-of-range stages have no sound and no guide.
 * - A guide table entry that is PRESENT for a stage wins wholesale: its
 *   lines are that stage's guide (an empty line list means the stage
 *   intentionally has no guide). A stage with NO entry falls back to the
 *   built-in default guide.
 * - A sound override that is present but blank silences that stage's
 *   voice line (the guide text still runs). No override means the
 *   default sound {@code wrv:guildgate.seal<stage>}.
 * - Master toggle off: no moment sound, no guide, no replay. Catch-up
 *   replay toggle off: the moment still plays; only the join replay is
 *   silenced.
 */
public final class NarrationGuide {

    public static final int STAGE_COUNT = 5;
    public static final String SOUND_PREFIX = "wrv:guildgate.seal";

    /**
     * Built-in guide wording (FINAL, Oct 18 2026): one entry per
     * stage, each a short staggered sequence - what opened, the practical
     * need, the Skill Trees pointer. Wording mirrors the Magic packets'
     * LADDER.md placement decisions; the Seal 1 wand line is the decided
     * village-first flow (Mike, Oct 17 2026): the old quartermaster who
     * left the Guild long ago lives in the village and issues a bound
     * apprentice wand there when Seal 1 falls. The guild world stays
     * sealed until Seal 5.
     */
    private static final String[][] GUIDE_FALLBACKS = {
            {
                    "&6— What fell open: &fThe Healer &6—",
                    "&7Healing magic is back: mend wounds, cure poison and curses,"
                            + " and bless your party before a fight.",
                    "&7To cast any of it, &fyou'll need a wand &7— seek the &fold"
                            + " quartermaster in the village&7: he left the Guild long"
                            + " ago, and when a seal falls he places a &fbound"
                            + " apprentice wand &7in your hand.",
                    "&7Spells are learned with spell points (SP) earned by casting;"
                            + " ranks run Beginner to Master.",
                    "&7The full ladder lives on the &fSkill Trees &7page of the server website.",
            },
            {
                    "&6— What fell open: &fThe Pathfinder &6—",
                    "&7Light for dark places, keen sight, and the getting-around arts"
                            + " return; the road itself feels shorter.",
                    "&7New spells join your wand's ladder at every rank; the short hop,"
                            + " &fBlink&7, waits at Master.",
                    "&7Keep casting to earn SP — each rank's spells run about two days"
                            + " of family play.",
                    "&7The full ladder lives on the &fSkill Trees &7page of the server website.",
            },
            {
                    "&6— What fell open: &fThe Wonder &6—",
                    "&7The wild elements stir: sparks, storms, and sky-fire answer the"
                            + " call again.",
                    "&7Its headline pair, &fLightning &7and &fChain Lightning&7, sit at"
                            + " Master rank — worth the climb.",
                    "&7Keep casting to earn SP — each rank's spells run about two days"
                            + " of family play.",
                    "&7The full ladder lives on the &fSkill Trees &7page of the server website.",
            },
            {
                    "&6— What fell open: &fThe Masons &6—",
                    "&7Building magic wakes: spells that place, move, and shape stone"
                            + " and timber.",
                    "&7The Masons do not sit on the wand ladder yet — their craft comes"
                            + " by the &fbuilder's handout &7(the masons wand and the"
                            + " builder group). Ask Mike.",
                    "&7When the last seal falls, the Masons take their place at Master"
                            + " rank like everyone else.",
                    "&7The full ladder lives on the &fSkill Trees &7page of the server website.",
            },
            {
                    "&6— What fell open: &fThe Guild Opens &6—",
                    "&6The Adventurers Guild is OPEN. &7Type &f/ag &7and walk in:"
                            + " Hermes and Casus are waiting.",
                    "&7The last schools of magic return with it, and the top rank of"
                            + " every ladder stands open.",
                    "&7The Masons join the Master rank; every seal's spells now exist"
                            + " in the valley.",
                    "&7The full ladders live on the &fSkill Trees &7page of the server website.",
            },
    };

    private final boolean enabled;
    private final boolean catchupReplay;
    private final long guideLineDelaySeconds;
    private final List<String> sounds;      // index 0 = stage 1; null = silenced
    private final List<List<String>> guides; // index 0 = stage 1; never null entries

    private NarrationGuide(boolean enabled, boolean catchupReplay, long guideLineDelaySeconds,
                             List<String> sounds, List<List<String>> guides) {
        this.enabled = enabled;
        this.catchupReplay = catchupReplay;
        this.guideLineDelaySeconds = Math.max(0L, guideLineDelaySeconds);
        this.sounds = sounds;
        this.guides = guides;
    }

    /** Built-in defaults with the given toggles (the no-config fallback). */
    public static NarrationGuide defaults(boolean enabled, boolean catchupReplay,
                                          long guideLineDelaySeconds) {
        return build(enabled, catchupReplay, guideLineDelaySeconds, null, null);
    }

    /**
     * Build a plan from raw stage tables (either may be null or short).
     *
     * @param soundOverrides index 0 = stage 1; a present entry REPLACES the
     *                       default sound, and a blank entry silences it
     * @param guideOverrides index 0 = stage 1; a present entry REPLACES the
     *                       default guide wholesale (empty list = no guide)
     */
    public static NarrationGuide build(boolean enabled, boolean catchupReplay,
                                       long guideLineDelaySeconds,
                                       List<String> soundOverrides,
                                       List<List<String>> guideOverrides) {
        List<String> sounds = new ArrayList<>(STAGE_COUNT);
        List<List<String>> guides = new ArrayList<>(STAGE_COUNT);
        for (int i = 0; i < STAGE_COUNT; i++) {
            int stage = i + 1;
            String sound = SOUND_PREFIX + stage;
            if (soundOverrides != null && i < soundOverrides.size()
                    && soundOverrides.get(i) != null) {
                String override = soundOverrides.get(i).trim();
                sound = override.isEmpty() ? null : override;
            }
            sounds.add(sound);
            List<String> guide = List.of(GUIDE_FALLBACKS[i]);
            if (guideOverrides != null && i < guideOverrides.size()
                    && guideOverrides.get(i) != null) {
                List<String> cleaned = new ArrayList<>();
                for (String line : guideOverrides.get(i)) {
                    if (line != null && !line.isBlank()) {
                        cleaned.add(line);
                    }
                }
                guide = Collections.unmodifiableList(cleaned);
            }
            guides.add(guide);
        }
        return new NarrationGuide(enabled, catchupReplay, guideLineDelaySeconds,
                Collections.unmodifiableList(sounds),
                Collections.unmodifiableList(guides));
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean catchupReplay() {
        return catchupReplay;
    }

    /** Seconds between staggered guide lines (0 = all at once). */
    public long guideLineDelaySeconds() {
        return guideLineDelaySeconds;
    }

    /**
     * The voice line to play when the given stage falls, or empty when
     * narration is off, the stage is out of range, or the stage's sound
     * was silenced in config.
     */
    public Optional<String> momentSound(int stage) {
        if (!enabled || stage < 1 || stage > STAGE_COUNT) {
            return Optional.empty();
        }
        return Optional.ofNullable(sounds.get(stage - 1));
    }

    /**
     * The guide lines for a stage (in order, possibly empty). Empty when
     * narration is off or the stage is out of range.
     */
    public List<String> guideLines(int stage) {
        if (!enabled || stage < 1 || stage > STAGE_COUNT) {
            return List.of();
        }
        return guides.get(stage - 1);
    }

    /**
     * The voice line a returning player hears alongside their text
     * catch-up: the sound of their HIGHEST missed beat. Empty when
     * narration or the replay toggle is off, nothing was missed, or that
     * stage's sound was silenced. Pure and stateless: the caller runs
     * this exactly once per catch-up delivery, and CatchUpTracker's
     * seen-stage advance is what makes it once-only.
     */
    public Optional<String> replaySound(List<Integer> missedBeats) {
        if (!enabled || !catchupReplay || missedBeats == null || missedBeats.isEmpty()) {
            return Optional.empty();
        }
        int highest = 0;
        for (Integer beat : missedBeats) {
            if (beat != null && beat > highest) {
                highest = beat;
            }
        }
        if (highest < 1 || highest > STAGE_COUNT) {
            return Optional.empty();
        }
        return Optional.ofNullable(sounds.get(highest - 1));
    }
}
