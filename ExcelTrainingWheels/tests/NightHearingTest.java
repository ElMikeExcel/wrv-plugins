import com.winkreyes.exceltrainingwheels.core.NightHearing;
import com.winkreyes.exceltrainingwheels.core.NightHearing.Hunters;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** 0.1.7: Night Hearing (Seal 2+, night, loud actions pull idle zombies). */
public class NightHearingTest {
    static void run() {
        System.out.println("NightHearing:");
        // night window
        Check.ok(!NightHearing.isNight(12999L), "12999 is day");
        Check.ok(NightHearing.isNight(13000L), "13000 is night");
        Check.ok(NightHearing.isNight(18000L), "midnight is night");
        Check.ok(NightHearing.isNight(23000L), "23000 is night");
        Check.ok(!NightHearing.isNight(23001L), "23001 is dawn (off)");
        Check.ok(!NightHearing.isNight(1000L), "morning is day");
        Check.ok(NightHearing.isNight(24000L * 5 + 18000L), "full-time wraps to midnight");

        // gate
        Check.ok(NightHearing.active(true, true, true, 2, 2, true, false, 18000L), "seal 2 night overworld = ON");
        Check.ok(NightHearing.active(true, true, true, 5, 2, true, false, 18000L), "seal 5 = ON");
        Check.ok(!NightHearing.active(true, true, true, 1, 2, true, false, 18000L), "seal 1 = OFF");
        Check.ok(!NightHearing.active(true, true, true, 0, 2, true, false, 18000L), "seal 0 = OFF");
        Check.ok(!NightHearing.active(true, true, false, 5, 2, true, false, 18000L), "unknown seal count = OFF (fail closed)");
        Check.ok(!NightHearing.active(true, true, true, 2, 2, true, false, 6000L), "day = OFF");
        Check.ok(!NightHearing.active(true, true, true, 2, 2, false, false, 18000L), "nether/end = OFF");
        Check.ok(!NightHearing.active(true, true, true, 2, 2, true, true, 18000L), "excluded world = OFF");
        Check.ok(!NightHearing.active(false, true, true, 2, 2, true, false, 18000L), "feature switch off = OFF");
        Check.ok(!NightHearing.active(true, false, true, 2, 2, true, false, 18000L), "master switch off = OFF");

        // excluded worlds
        List<String> pre = List.of("em_");
        Check.ok(NightHearing.excludedWorld("em_adventurers_guild", Set.of(), pre), "em_ prefix excluded");
        Check.ok(NightHearing.excludedWorld("EM_Dungeon", Set.of(), pre), "prefix is case-insensitive");
        Check.ok(!NightHearing.excludedWorld("world", Set.of(), pre), "main world not excluded");
        Check.ok(NightHearing.excludedWorld("arena", Set.of("arena"), pre), "listed name excluded");
        Check.ok(NightHearing.excludedWorld(null, Set.of(), pre), "null world excluded");
        Check.ok(!NightHearing.excludedWorld("world", null, null), "null lists safe");

        // noises
        for (String k : List.of("block_open", "block_close", "block_place", "block_destroy", "entity_damage",
                "projectile_shoot", "container_open", "container_close", "note_block_play", "jukebox_play")) {
            Check.ok(NightHearing.isNoise(k, false), k + " is loud");
        }
        Check.ok(NightHearing.isNoise("minecraft:block_open", false), "namespaced key accepted");
        Check.ok(NightHearing.isNoise("BLOCK_OPEN", false), "case-insensitive key");
        Check.ok(!NightHearing.isNoise("step", false), "footsteps silent by default");
        Check.ok(NightHearing.isNoise("step", true), "footsteps heard when enabled");
        for (String k : List.of("swim", "splash", "eat", "drink", "hit_ground", "flap", "item_interact_finish",
                "entity_place", "teleport", "sculk_sensor_tendrils_clicking", "elytra_glide", "resonate_1")) {
            Check.ok(!NightHearing.isNoise(k, false), k + " not a noise");
        }
        Check.ok(!NightHearing.isNoise(null, true), "null key not a noise");

        // range
        Check.close(16.0, NightHearing.range(16.0, false), "range 16");
        Check.close(8.0, NightHearing.range(16.0, true), "sneaking halves to 8");
        Check.close(0.0, NightHearing.range(-3.0, false), "negative range clamps 0");
        Check.close(1.0, NightHearing.sanitizeRange(0.0), "range clamps up to 1");
        Check.close(48.0, NightHearing.sanitizeRange(500.0), "range clamps down to 48");
        Check.close(1.0, NightHearing.sanitizeRange(Double.NaN), "NaN range -> 1");

        // who is heard
        Check.ok(NightHearing.playerHearable("SURVIVAL", false, false, false, false, false), "survival heard");
        Check.ok(NightHearing.playerHearable("ADVENTURE", false, false, false, false, false), "adventure heard");
        Check.ok(!NightHearing.playerHearable("SURVIVAL", true, false, false, false, false), "protected NEVER heard");
        Check.ok(!NightHearing.playerHearable("CREATIVE", false, false, false, false, false), "creative not heard");
        Check.ok(!NightHearing.playerHearable("SPECTATOR", false, false, false, false, false), "spectator not heard");
        Check.ok(!NightHearing.playerHearable("SURVIVAL", false, true, false, false, false), "bedrock grace not heard");
        Check.ok(!NightHearing.playerHearable("SURVIVAL", false, false, true, false, false), "veiled not heard");
        Check.ok(!NightHearing.playerHearable("SURVIVAL", false, false, false, true, false), "sleeper silent");
        Check.ok(!NightHearing.playerHearable("SURVIVAL", false, false, false, false, true), "dead not heard");
        Check.ok(!NightHearing.playerHearable(null, false, false, false, false, false), "null mode not heard");

        // which zombies
        Set<String> types = NightHearing.parseTypes(null);
        Check.eq(Set.of("ZOMBIE", "HUSK", "ZOMBIE_VILLAGER"), types, "default types");
        Check.ok(NightHearing.zombieEligible(types, "ZOMBIE", false, false), "idle zombie hears");
        Check.ok(NightHearing.zombieEligible(types, "HUSK", false, false), "idle husk hears");
        Check.ok(NightHearing.zombieEligible(types, "ZOMBIE_VILLAGER", false, false), "idle zombie villager hears");
        Check.ok(!NightHearing.zombieEligible(types, "ZOMBIE", true, false), "busy zombie ignores");
        Check.ok(!NightHearing.zombieEligible(types, "ZOMBIE", false, true), "elite never hears");
        Check.ok(!NightHearing.zombieEligible(types, "DROWNED", false, false), "drowned not listed");
        Check.ok(!NightHearing.zombieEligible(types, "ZOMBIFIED_PIGLIN", false, false), "piglin not listed");
        Check.ok(!NightHearing.parseTypes(List.of("drowned", "zombie")).contains("DROWNED"), "drowned stripped");
        Check.eq(Set.of("ZOMBIE"), NightHearing.parseTypes(List.of(" zombie ")), "types trimmed and upper-cased");
        Check.eq(types, NightHearing.parseTypes(List.of("DROWNED")), "all-invalid list falls back to defaults");

        // pull caps
        Check.eq(3, NightHearing.pullCount(10, 0, 3, 6), "max 3 per noise");
        Check.eq(2, NightHearing.pullCount(2, 0, 3, 6), "only 2 candidates");
        Check.eq(1, NightHearing.pullCount(10, 5, 3, 6), "only room for 1 under 6 per player");
        Check.eq(0, NightHearing.pullCount(10, 6, 3, 6), "player cap reached");
        Check.eq(0, NightHearing.pullCount(10, 9, 3, 6), "over cap -> 0");
        Check.eq(0, NightHearing.pullCount(0, 0, 3, 6), "no candidates");
        Check.eq(0, NightHearing.pullCount(5, 0, -1, 6), "negative cap -> 0");

        // cooldown
        UUID a = new UUID(1L, 1L), b = new UUID(2L, 2L);
        NightHearing.Cooldown cd = new NightHearing.Cooldown(20_000L);
        Check.ok(cd.ready(a, 0L), "first noise ready");
        cd.start(a, 1_000L);
        Check.ok(!cd.ready(a, 20_999L), "cooling at 19.999 s");
        Check.ok(cd.ready(a, 21_000L), "ready at 20 s");
        Check.ok(cd.ready(b, 5_000L), "cooldown is per player");
        cd.forget(a);
        Check.ok(cd.ready(a, 2_000L), "forget clears");

        // hunters: give up after 60 s without progress; a door counts as progress
        UUID z1 = new UUID(9L, 1L), z2 = new UUID(9L, 2L), z3 = new UUID(9L, 3L);
        Hunters h = new Hunters(60_000L);
        h.pull(z1, a, 0L);
        Check.eq(Hunters.Verdict.KEEP, h.check(z1, true, false, false, 30_000L), "hunting at 30 s");
        Check.eq(Hunters.Verdict.KEEP, h.check(z1, true, false, false, 59_999L), "still hunting at 59.999 s");
        Check.eq(Hunters.Verdict.GIVE_UP, h.check(z1, true, false, false, 60_000L), "gives up at 60 s");
        Check.ok(!h.contains(z1), "given-up zombie dropped");
        h.pull(z2, a, 0L);
        Check.eq(Hunters.Verdict.KEEP, h.check(z2, true, false, true, 50_000L), "at a door at 50 s");
        Check.eq(Hunters.Verdict.KEEP, h.check(z2, true, false, true, 105_000L), "iron siege at 105 s keeps going");
        Check.eq(Hunters.Verdict.KEEP, h.check(z2, true, false, false, 164_999L), "door gone, clock from last door work");
        Check.eq(Hunters.Verdict.GIVE_UP, h.check(z2, true, false, false, 165_000L), "gives up 60 s after last door work");
        h.pull(z3, b, 0L);
        Check.eq(Hunters.Verdict.REACHED, h.check(z3, true, true, false, 10_000L), "reached the player");
        Check.ok(!h.contains(z3), "reached zombie handed to vanilla");
        h.pull(z3, b, 0L);
        Check.eq(Hunters.Verdict.LOST, h.check(z3, false, false, false, 1_000L), "lost target (other plugin / death)");
        Check.eq(Hunters.Verdict.LOST, h.check(new UUID(7L, 7L), true, false, false, 0L), "unknown zombie = LOST");
        h.pull(z1, a, 0L); h.pull(z2, a, 0L); h.pull(z3, b, 0L);
        Check.eq(2, h.releasePlayer(a).size(), "bed releases that player's 2 hunters");
        Check.eq(1, h.size(), "other player's hunter kept");
        Check.eq(1, h.releaseAll().size(), "dawn releases all");
        Check.eq(0, h.size(), "empty after dawn");

        // bed
        Check.ok(NightHearing.forceBed(true, true, "NOT_SAFE"), "monsters nearby -> bed opens");
        Check.ok(!NightHearing.forceBed(true, true, "NOT_POSSIBLE_NOW"), "daytime bed never forced");
        Check.ok(!NightHearing.forceBed(true, true, "TOO_FAR_AWAY"), "too far never forced");
        Check.ok(!NightHearing.forceBed(true, true, "OBSTRUCTED"), "obstructed never forced");
        Check.ok(!NightHearing.forceBed(true, true, "NOT_POSSIBLE_HERE"), "nether bed never forced");
        Check.ok(!NightHearing.forceBed(true, false, "NOT_SAFE"), "hearing off -> vanilla bed rule");
        Check.ok(!NightHearing.forceBed(false, true, "NOT_SAFE"), "option off -> vanilla bed rule");

        // wood fallback
        Check.ok(!NightHearing.woodFallbackRuns(true, "HARD"), "Hard: vanilla breaks wood, fallback idle");
        Check.ok(NightHearing.woodFallbackRuns(true, "NORMAL"), "Normal: fallback runs");
        Check.ok(NightHearing.woodFallbackRuns(true, "EASY"), "Easy: fallback runs");
        Check.ok(!NightHearing.woodFallbackRuns(true, "PEACEFUL"), "Peaceful: nothing");
        Check.ok(!NightHearing.woodFallbackRuns(false, "NORMAL"), "option off");

        // door steering
        Check.eq(NightHearing.DoorKind.IRON, NightHearing.steerKind("IRON_DOOR", true), "iron door -> iron siege");
        Check.eq(NightHearing.DoorKind.WOOD, NightHearing.steerKind("OAK_DOOR", true), "oak door -> wood siege");
        Check.eq(NightHearing.DoorKind.WOOD, NightHearing.steerKind("CRIMSON_DOOR", false), "crimson door -> wood siege");
        Check.eq(NightHearing.DoorKind.COPPER, NightHearing.steerKind("WAXED_OXIDIZED_COPPER_DOOR", true), "waxed copper -> copper siege");
        Check.eq(NightHearing.DoorKind.COPPER, NightHearing.steerKind("COPPER_DOOR", true), "copper -> copper siege");
        Check.ok(NightHearing.steerKind("COPPER_DOOR", false) == null, "copper tier off -> never steered to copper");
        Check.ok(NightHearing.steerKind("OAK_TRAPDOOR", true) == null, "trapdoor is not a door");
        Check.ok(NightHearing.steerKind("IRON_TRAPDOOR", true) == null, "iron trapdoor is not a door");
        Check.ok(NightHearing.steerKind("STONE", true) == null, "stone is not a door");
        Check.ok(NightHearing.steerKind(null, true) == null, "null material");
        Check.ok(NightHearing.steerScore(5, 1.0) < NightHearing.steerScore(5, 6.0), "same walk: door nearer the player wins");
        Check.ok(NightHearing.steerScore(4, 2.0) < NightHearing.steerScore(20, 2.0), "same door distance: shorter walk wins");
        Check.ok(NightHearing.steerScore(-3, -1.0) == 0.0, "score floors at 0");
        Check.ok(NightHearing.outsideFace(9.0, 1.0), "face farther from the player is the outside");
        Check.ok(!NightHearing.outsideFace(1.0, 9.0), "face nearer the player is the inside");
        Check.ok(!NightHearing.outsideFace(4.0, 4.0), "tie is not an outside face");
        Check.ok(NightHearing.onSpot(0.5, 0.0, 1.0), "0.7 blocks off = on the spot");
        Check.ok(!NightHearing.onSpot(1.5, 0.0, 1.0), "1.2 blocks off = not on the spot (radius 1)");
        Check.ok(NightHearing.onSpot(1.5, 0.0, 1.5), "1.2 blocks off = on the spot (radius 1.5)");
        Check.ok(!NightHearing.onSpot(0.1, 2.0, 1.0), "a floor up = not on the spot");
        Check.eq(12.0, NightHearing.sanitizeSteerRadius(Double.NaN), "steer radius NaN -> 12");
        Check.eq(24.0, NightHearing.sanitizeSteerRadius(99.0), "steer radius clamps high");
        Check.eq(4.0, NightHearing.sanitizeSteerRadius(1.0), "steer radius clamps low");

        // sanitize
        Check.eq(10, NightHearing.sanitizeCount(99, 10), "count clamps high");
        Check.eq(0, NightHearing.sanitizeCount(-4, 10), "count clamps low");
        Check.eq(600L, NightHearing.sanitizeSeconds(9999L, 600L), "seconds clamp high");
        Check.eq(0L, NightHearing.sanitizeSeconds(-1L, 600L), "seconds clamp low");
    }
}
