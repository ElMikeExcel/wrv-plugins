import excelemfixes.timedevents.CompassText;
import excelemfixes.timedevents.EventNames;
import excelemfixes.timedevents.RelocationPolicy;
import excelemfixes.timedevents.TimedEventSettings;
import excelemfixes.timedevents.TrackedEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * The timed-event-guide suites (v28): settings clamps, name
 * cleaning, compass math, relocation policy, the tracked
 * event lifecycle, and the file guards (plugin.yml gains no
 * hard depend; config.yml carries the section with the
 * shipped defaults).
 */
public final class TimedEventGuideTest {

    public static void run() {
        try {
            runInner();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void runInner() throws Exception {
        System.out.println("TimedEventGuideTest");
        settings();
        names();
        compass();
        relocation();
        tracking();
        files();
    }

    // ---------------------------------------------------------- settings

    private static void settings() {
        TimedEventSettings d = TimedEventSettings.defaults();
        Check.ok(d.enabled() && d.relocate() && d.preventFarDespawn()
                && d.broadcast() && d.beacon() && !d.broadcastServerWide()
                && !d.debug(), "settings: defaults on, server-wide + debug off");
        Check.close(20.0, d.relocationMinDistance(), "settings: default min 20");
        Check.close(48.0, d.relocationMaxDistance(), "settings: default max 48");
        Check.eq(24, d.safeSpotAttempts(), "settings: default attempts 24");
        Check.close(256.0, d.beaconViewDistance(), "settings: default view 256");
        Check.eq(40, d.columnHeight(), "settings: default height 40");
        Check.eq(2, d.columnStep(), "settings: default step 2");
        Check.eq(10, d.refreshTicks(), "settings: default refresh 10");
        Check.eq(300, d.pendingTimeoutSeconds(), "settings: default pending 300");
        Check.eq(40, d.maxTrackMinutes(), "settings: default track 40");

        TimedEventSettings n = TimedEventSettings.of(null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null);
        Check.eq(d, n, "settings: all-null config = defaults");

        TimedEventSettings c = TimedEventSettings.of(false, false, -5.0, 999.0, 0,
                false, false, true, false, 1.0, 1, 0, 1, 1, 0, true);
        Check.ok(!c.enabled() && !c.relocate() && !c.preventFarDespawn()
                && !c.broadcast() && !c.beacon() && c.broadcastServerWide()
                && c.debug(), "settings: explicit booleans honored");
        Check.close(20.0, c.relocationMinDistance(), "settings: min below range -> default");
        Check.close(48.0, c.relocationMaxDistance(), "settings: max above range -> default");
        Check.eq(24, c.safeSpotAttempts(), "settings: attempts 0 -> default");
        Check.close(256.0, c.beaconViewDistance(), "settings: view below range -> default");
        Check.eq(40, c.columnHeight(), "settings: height below range -> default");
        Check.eq(2, c.columnStep(), "settings: step 0 -> default");
        Check.eq(10, c.refreshTicks(), "settings: refresh 1 -> default");
        Check.eq(300, c.pendingTimeoutSeconds(), "settings: pending below range -> default");
        Check.eq(40, c.maxTrackMinutes(), "settings: track 0 -> default");

        TimedEventSettings band = TimedEventSettings.of(null, null, 60.0, 30.0, null,
                null, null, null, null, null, null, null, null, null, null, null);
        Check.close(60.0, band.relocationMinDistance(), "settings: min 60 honored");
        Check.close(60.0, band.relocationMaxDistance(), "settings: max below min raised to min");

        TimedEventSettings nan = TimedEventSettings.of(null, null, Double.NaN,
                Double.POSITIVE_INFINITY, null, null, null, null, null, Double.NaN,
                null, null, null, null, null, null);
        Check.close(20.0, nan.relocationMinDistance(), "settings: NaN min -> default");
        Check.close(48.0, nan.relocationMaxDistance(), "settings: infinite max -> default");
        Check.close(256.0, nan.beaconViewDistance(), "settings: NaN view -> default");
    }

    // ---------------------------------------------------------- names

    private static void names() {
        Check.eq("Bone Seeker Goblin",
                EventNames.cleanDisplayName(
                        "$eventBossLevel <gradient:#76EFC4:#00C96E:#0A7A44>Bone Seeker Goblin</gradient>"),
                "names: gradient + level placeholder cleaned");
        Check.eq("Trophy Collector Goblin",
                EventNames.cleanDisplayName("§6Trophy Collector Goblin"),
                "names: legacy code cleaned");
        Check.eq("", EventNames.cleanDisplayName(null), "names: null -> empty");
        Check.eq("", EventNames.cleanDisplayName("<gradient:#000:#fff></gradient>"),
                "names: markup only -> empty");
        Check.eq("Free Goblins Event Pack Bone Seeker Goblin",
                EventNames.prettifyFilename("em_free_goblins_event_pack_bone_seeker_goblin.yml"),
                "names: boss filename prettified");
        Check.eq("Free Goblins Event Pack Bone Seeker Goblin",
                EventNames.prettifyFilename(
                        "customevents/em_events_goblins_free/em_free_goblins_event_pack_bone_seeker_goblin_event.yml"),
                "names: event filename prettified (path + _event stripped)");
        Check.eq("Bone Seeker Goblin",
                EventNames.displayName(
                        "$eventBossLevel <gradient:#76EFC4:#00C96E:#0A7A44>Bone Seeker Goblin</gradient>",
                        "「☠」 Bone Seeker Gobl", "x.yml", "y.yml"),
                "names: configured name wins over the truncated live name");
        Check.eq("Meat Hunter Goblin",
                EventNames.displayName(null, "§eMeat Hunter Goblin",
                        "em_meat_hunter.yml", "em_meat_event.yml"),
                "names: live name is the fallback");
        Check.eq("A timed event",
                EventNames.displayName(null, null, null, null),
                "names: total fallback");
    }

    // ---------------------------------------------------------- compass

    private static void compass() {
        Check.eq("north", CompassText.direction8(0, -10), "compass: -Z is north");
        Check.eq("south", CompassText.direction8(0, 10), "compass: +Z is south");
        Check.eq("east", CompassText.direction8(10, 0), "compass: +X is east");
        Check.eq("west", CompassText.direction8(-10, 0), "compass: -X is west");
        Check.eq("north-east", CompassText.direction8(10, -10), "compass: diagonal NE");
        Check.eq("south-west", CompassText.direction8(-10, 10), "compass: diagonal SW");
        Check.eq("north-west", CompassText.direction8(-10, -10), "compass: diagonal NW");
        Check.eq("south-east", CompassText.direction8(10, 10), "compass: diagonal SE");
        Check.eq("north", CompassText.direction8(1, -10), "compass: near-axis rounds to north");
        Check.eq(87L, CompassText.blocks(60, 63), "compass: distance rounds (87.0)");
        Check.eq("87 blocks north-east of you", CompassText.guidance(61, -62),
                "compass: guidance line shape");
    }

    // ---------------------------------------------------------- relocation

    private static void relocation() {
        Check.eq(RelocationPolicy.Decision.STAY,
                RelocationPolicy.decide(-1, 48), "relocation: no player -> stay");
        Check.eq(RelocationPolicy.Decision.STAY,
                RelocationPolicy.decide(48, 48), "relocation: at max -> stay");
        Check.eq(RelocationPolicy.Decision.STAY,
                RelocationPolicy.decide(30, 48), "relocation: inside band -> stay");
        Check.eq(RelocationPolicy.Decision.MOVE,
                RelocationPolicy.decide(48.01, 48), "relocation: past max -> move");
        Check.eq(RelocationPolicy.Decision.MOVE,
                RelocationPolicy.decide(181, 48), "relocation: EM scatter corner -> move");
        Check.close(20.0, RelocationPolicy.targetDistance(0.0, 20, 48),
                "relocation: roll 0 lands at min");
        Check.close(48.0, RelocationPolicy.targetDistance(1.0, 20, 48),
                "relocation: roll 1 lands at max");
        Check.close(34.0, RelocationPolicy.targetDistance(0.5, 20, 48),
                "relocation: roll 0.5 lands mid-band");
        Check.close(48.0, RelocationPolicy.targetDistance(7.5, 20, 48),
                "relocation: roll above 1 clamps to max");
        Check.close(20.0, RelocationPolicy.targetDistance(-2.0, 20, 48),
                "relocation: roll below 0 clamps to min");
    }

    // ---------------------------------------------------------- tracking

    private static void tracking() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        TrackedEvent event = new TrackedEvent("em_pack_bone_seeker_event.yml",
                List.of("em_pack_bone_seeker_goblin.yml", "em_pack_helper.yml"), 1_000L);
        Check.ok(event.matchesBoss("em_pack_bone_seeker_goblin.yml"),
                "tracking: boss filename matches");
        Check.ok(event.matchesBoss("EM_PACK_BONE_SEEKER_GOBLIN.YML"),
                "tracking: match is case-insensitive");
        Check.ok(!event.matchesBoss("em_pack_other.yml"),
                "tracking: other boss does not match");
        Check.ok(!event.hasBound() && !event.allBoundGone(),
                "tracking: pending event is not 'all gone'");
        Check.ok(event.pendingExpired(1_000L + 301_000L, 300_000L),
                "tracking: pending expires after timeout");
        Check.ok(!event.pendingExpired(1_000L + 299_000L, 300_000L),
                "tracking: pending alive inside timeout");

        event.bind(a);
        Check.ok(event.hasBound() && event.isBound(a), "tracking: bind records the boss");
        Check.ok(!event.pendingExpired(1_000L + 999_000L, 300_000L),
                "tracking: bound event never pending-expires");
        event.bind(b);
        event.markGone(a);
        Check.ok(!event.allBoundGone(), "tracking: one of two gone is not closed");
        event.markGone(UUID.randomUUID());
        Check.ok(!event.allBoundGone(), "tracking: foreign uuid changes nothing");
        event.markGone(b);
        Check.ok(event.allBoundGone(), "tracking: all bound gone closes");
        Check.ok(!event.broadcastSent(), "tracking: broadcast starts unsent");
        event.markBroadcastSent();
        Check.ok(event.broadcastSent(), "tracking: broadcast marked sent");
        Check.ok(event.expired(1_000L + 41L * 60_000L, 40L * 60_000L),
                "tracking: failsafe age expires");
        Check.ok(!event.expired(1_000L + 39L * 60_000L, 40L * 60_000L),
                "tracking: inside failsafe age alive");
    }

    // ---------------------------------------------------------- files

    private static void files() throws Exception {
        String yml = Files.readString(Path.of("src/plugin.yml"));
        Check.ok(yml.lines().noneMatch(l -> l.startsWith("depend:")),
                "plugin.yml: no hard depend line at all");
        Check.ok(yml.contains("softdepend: [EliteMobs"),
                "plugin.yml: EliteMobs remains a softdepend");
        Check.ok(yml.contains("version: v32"), "plugin.yml: version v32");
        Check.ok(yml.contains("excelemfixes.eventguide.admin"),
                "plugin.yml: eventguide permission node declared");
        Check.ok(yml.contains("timed-event-guide"),
                "plugin.yml: module described");
        String cfg = Files.readString(Path.of("src/config.yml"));
        Check.ok(cfg.contains("timed-event-guide:"), "config.yml: section present");
        Check.ok(cfg.contains("relocation-min-distance: 20.0"),
                "config.yml: shipped default min 20.0");
        Check.ok(cfg.contains("relocation-max-distance: 48.0"),
                "config.yml: shipped default max 48.0");
        Check.ok(cfg.contains("broadcast-server-wide: false"),
                "config.yml: shipped default world-scope broadcast");
        Check.ok(cfg.contains("max-track-minutes: 40"),
                "config.yml: shipped default track 40");
    }
}
