import com.winkreyes.exceltrainingwheels.core.BedrockGrace;

import java.util.UUID;

/** 0.1.6: the Bedrock loading-screen grace window. */
public class BedrockGraceTest {
    static void run() {
        System.out.println("BedrockGrace:");
        UUID bedrock = new UUID(0L, 0x0009_01F2_3456_789AL);
        UUID java = UUID.fromString("123e4567-e89b-42d3-a456-426614174000");
        Check.ok(BedrockGrace.looksLikeFloodgateUuid(bedrock), "Floodgate-style UUID detected");
        Check.ok(!BedrockGrace.looksLikeFloodgateUuid(java), "Java UUID not Bedrock");
        Check.ok(!BedrockGrace.looksLikeFloodgateUuid(null), "null UUID not Bedrock");

        BedrockGrace g = new BedrockGrace(4_000L);
        Check.ok(g.start(bedrock, 1_000L), "window starts");
        Check.ok(g.active(bedrock, 1_000L), "active at start");
        Check.ok(g.active(bedrock, 4_999L), "active just before 4 s");
        Check.ok(!g.active(bedrock, 5_000L), "over at 4 s");
        Check.eq(0, g.size(), "expired entry dropped");
        g.start(bedrock, 10_000L);
        g.start(bedrock, 12_000L);
        Check.ok(g.active(bedrock, 15_500L), "restart (join then world change) extends from the latest trigger");
        Check.ok(g.end(bedrock), "attack/break ends it early");
        Check.ok(!g.active(bedrock, 15_600L), "inactive after early end");
        Check.ok(!g.end(bedrock), "ending twice is a no-op");
        Check.ok(!g.active(java, 15_600L), "never started -> inactive");

        BedrockGrace off = new BedrockGrace(0L);
        Check.ok(!off.start(bedrock, 1L), "seconds 0 = off");
        Check.ok(!off.active(bedrock, 1L), "off -> never active");

        Check.eq(4L, BedrockGrace.sanitizeSeconds(4L), "seconds 4 kept");
        Check.eq(0L, BedrockGrace.sanitizeSeconds(-3L), "negative -> 0");
        Check.eq(30L, BedrockGrace.sanitizeSeconds(600L), "clamped to 30");

        Check.ok(BedrockGrace.causeStillApplies("VOID", true), "VOID still applies");
        Check.ok(BedrockGrace.causeStillApplies("KILL", true), "KILL still applies");
        Check.ok(BedrockGrace.causeStillApplies("SUICIDE", false), "SUICIDE still applies");
        Check.ok(BedrockGrace.causeStillApplies("LAVA", true), "LAVA applies when allow-lava true");
        Check.ok(!BedrockGrace.causeStillApplies("LAVA", false), "LAVA blocked when allow-lava false");
        Check.ok(!BedrockGrace.causeStillApplies("ENTITY_ATTACK", true), "mob melee blocked");
        Check.ok(!BedrockGrace.causeStillApplies("PROJECTILE", true), "arrows blocked");
        Check.ok(!BedrockGrace.causeStillApplies("FALL", true), "fall blocked");
        Check.ok(!BedrockGrace.causeStillApplies(null, true), "null cause blocked");
    }
}
