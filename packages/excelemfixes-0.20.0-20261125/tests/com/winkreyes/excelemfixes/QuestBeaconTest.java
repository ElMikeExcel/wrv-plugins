import excelemfixes.questbeacon.BeamGeometry;
import excelemfixes.questbeacon.BeamPolicy;

/**
 * The quest-beacon module's pure core: the edge-riding anchor
 * geometry (Mike's Nov 22, 2026, 11:44 PM shape — the beam rides
 * at the edge of the player's view toward the destination and
 * settles onto it in range) and the show/hide policy (the beam
 * lives only while a quest is tracked; it ends on untrack,
 * completion, world change, or arrival).
 */
public class QuestBeaconTest {

    public static void run() {
        System.out.println("== QuestBeacon ==");
        geometry();
        policy();
    }

    private static void geometry() {
        // Far away due east: anchor rides exactly edgeDistance out,
        // on the straight line to the destination, not settled.
        BeamGeometry.Anchor a = BeamGeometry.anchor(0, 0, 480, 0, 48);
        Check.close(48, a.x(), "edge anchor x due east");
        Check.close(0, a.z(), "edge anchor z due east");
        Check.ok(!a.settled(), "far beam rides the edge");
        Check.close(480, a.distance(), "far distance reported");

        // Diagonal: anchor stays on the line and at edge distance.
        BeamGeometry.Anchor d = BeamGeometry.anchor(0, 0, 300, 400, 48);
        Check.close(500, d.distance(), "diagonal distance 3-4-5");
        Check.close(48, Math.hypot(d.x(), d.z()),
                "diagonal anchor at edge distance");
        Check.close(300.0 / 400.0, d.x() / d.z(),
                "diagonal anchor on the destination line");
        Check.ok(!d.settled(), "diagonal beam rides the edge");

        // Negative direction: the line is followed both ways.
        BeamGeometry.Anchor w = BeamGeometry.anchor(100, 100, -380, 100, 48);
        Check.close(52, w.x(), "westward anchor x");
        Check.close(100, w.z(), "westward anchor z");

        // Inside edge distance: settled exactly on the destination.
        BeamGeometry.Anchor s = BeamGeometry.anchor(10, 10, 30, 25, 48);
        Check.ok(s.settled(), "near beam settles");
        Check.close(30, s.x(), "settled anchor is the destination x");
        Check.close(25, s.z(), "settled anchor is the destination z");

        // Exactly at edge distance: settled (boundary inclusive).
        BeamGeometry.Anchor b = BeamGeometry.anchor(0, 0, 48, 0, 48);
        Check.ok(b.settled(), "at edge distance the beam settles");

        // Standing on the destination: settled, distance zero, no
        // division blow-up.
        BeamGeometry.Anchor z = BeamGeometry.anchor(5, 5, 5, 5, 48);
        Check.ok(z.settled(), "on the destination settles");
        Check.close(0, z.distance(), "on the destination distance 0");

        // Moving player: the anchor tracks the player's position.
        BeamGeometry.Anchor m1 = BeamGeometry.anchor(0, 0, 480, 0, 48);
        BeamGeometry.Anchor m2 = BeamGeometry.anchor(100, 0, 480, 0, 48);
        Check.close(148, m2.x(), "anchor repositions as the player moves");
        Check.ok(m2.x() > m1.x(), "anchor advances with the player");

        Check.close(5, BeamGeometry.horizontalDistance(0, 0, 3, 4),
                "horizontal distance 3-4-5");
    }

    private static void policy() {
        double arrival = 8, edge = 48;
        // The happy paths.
        Check.eq(BeamPolicy.Mode.SHOW_EDGE, BeamPolicy.decide(
                false, "q.yml", true, true, 200, arrival, edge),
                "far tracked covered quest shows the edge beam");
        Check.eq(BeamPolicy.Mode.SHOW_SETTLED, BeamPolicy.decide(
                false, "q.yml", true, true, 30, arrival, edge),
                "near tracked covered quest shows the settled beam");
        Check.eq(BeamPolicy.Mode.SHOW_SETTLED, BeamPolicy.decide(
                false, "q.yml", true, true, 48, arrival, edge),
                "at edge distance the beam is settled");

        // End conditions from the settled design.
        Check.eq(BeamPolicy.Mode.HIDE_ARRIVED, BeamPolicy.decide(
                false, "q.yml", true, true, 8, arrival, edge),
                "arrival radius ends the beam");
        Check.eq(BeamPolicy.Mode.HIDE_ARRIVED, BeamPolicy.decide(
                false, "q.yml", true, true, 0, arrival, edge),
                "standing on the destination ends the beam");
        Check.eq(BeamPolicy.Mode.HIDE_NO_TRACK, BeamPolicy.decide(
                false, null, true, true, 200, arrival, edge),
                "untracked (or completed) quest ends the beam");
        Check.eq(BeamPolicy.Mode.HIDE_NO_TRACK, BeamPolicy.decide(
                false, "  ", true, true, 200, arrival, edge),
                "blank tracked file reads as no track");
        Check.eq(BeamPolicy.Mode.HIDE_UNCOVERED, BeamPolicy.decide(
                false, "q.yml", false, true, 200, arrival, edge),
                "a quest with no table row gets no beam");
        Check.eq(BeamPolicy.Mode.HIDE_WRONG_WORLD, BeamPolicy.decide(
                false, "q.yml", true, false, 200, arrival, edge),
                "another world gets no beam");
        Check.eq(BeamPolicy.Mode.HIDE_PLAYER_OFF, BeamPolicy.decide(
                true, "q.yml", true, true, 200, arrival, edge),
                "the player's own off switch wins");

        // Precedence: off beats everything; no-track beats uncovered.
        Check.eq(BeamPolicy.Mode.HIDE_PLAYER_OFF, BeamPolicy.decide(
                true, null, false, false, 0, arrival, edge),
                "off beats every other condition");
        Check.eq(BeamPolicy.Mode.HIDE_NO_TRACK, BeamPolicy.decide(
                false, null, false, false, 200, arrival, edge),
                "no-track beats uncovered and wrong-world");

        Check.ok(BeamPolicy.shows(BeamPolicy.Mode.SHOW_EDGE)
                && BeamPolicy.shows(BeamPolicy.Mode.SHOW_SETTLED)
                && !BeamPolicy.shows(BeamPolicy.Mode.HIDE_ARRIVED),
                "shows() covers exactly the two drawing modes");
    }
}
