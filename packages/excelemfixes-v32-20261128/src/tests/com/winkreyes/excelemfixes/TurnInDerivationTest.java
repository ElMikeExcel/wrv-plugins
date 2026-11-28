import excelemfixes.questdest.DialogDerivation;
import excelemfixes.questdest.QuestDestinations;
import excelemfixes.questdest.TurnInDerivation;

/**
 * The turn-in-phase destination switch (v27, FIX-LIST B2):
 * when a carried quest's objectives are ALL complete, the
 * resolved destination switches to the turn-in NPC's spawn
 * (turnInNPC from the quest file, else the giver — that
 * choice is the EmTurnInData bridge's; this suite pins the
 * pure policy: the switch fires only on all-complete, the
 * toggle restores pre-v27 behavior, an unresolvable NPC
 * never produces a guessed destination, and the derived row
 * carries the NPC's spawn exactly).
 *
 * The specimen is the re-audit's hole: "Storm's end"
 * (primis_main_quest_14) turns in to Navita
 * (primis_pontis_mille_shogun.yml), whose EliteMobs NPC
 * registry spawn is em_primis 595.5, 72, 771.7 — the census
 * rounds that to 596, 72, 772.
 */
public class TurnInDerivationTest {

    private static final String QUEST = "primis_main_quest_14.yml";
    private static final DialogDerivation.NpcSpawn NAVITA =
            new DialogDerivation.NpcSpawn(
                    "primis_pontis_mille_shogun.yml", "Navita",
                    "em_primis", 595.5, 72, 771.7);

    public static void run() {
        System.out.println("== TurnInDerivation ==");
        specimenSwitchesToNavita();
        objectivesOutstandingStaysSilent();
        toggleOffIsOldBehavior();
        unresolvableNpcIsNothing();
        derivedRowShape();
    }

    /** The headline case: all objectives complete -> the
     *  destination IS the turn-in NPC's spawn. */
    private static void specimenSwitchesToNavita() {
        TurnInDerivation.Resolution r = TurnInDerivation.resolve(
                true, true, QUEST, "Storm's end", NAVITA);
        Check.eq(TurnInDerivation.Source.TURN_IN, r.source(),
                "all complete: source TURN_IN");
        Check.ok(r.destination() != null,
                "all complete: destination present");
        Check.close(595.5, r.destination().x(),
                "specimen x is Navita's spawn x");
        Check.close(72, r.destination().y(),
                "specimen y is Navita's spawn y");
        Check.close(771.7, r.destination().z(),
                "specimen z is Navita's spawn z");
        Check.eq("em_primis", r.destination().world(),
                "specimen world is Navita's spawn world");
        Check.eq(QUEST, r.destination().questFile(),
                "derived row keeps the quest file");
        // The census rounds the spawn to 596, 72, 772 — the
        // FIX-LIST B2 specimen coordinates.
        Check.eq(596L, Math.round(r.destination().x()),
                "specimen x rounds to 596");
        Check.eq(72L, Math.round(r.destination().y()),
                "specimen y rounds to 72");
        Check.eq(772L, Math.round(r.destination().z()),
                "specimen z rounds to 772");
    }

    /** Any objective outstanding: no switch — the objective
     *  phase's own channels own the destination. */
    private static void objectivesOutstandingStaysSilent() {
        TurnInDerivation.Resolution r = TurnInDerivation.resolve(
                true, false, QUEST, "Storm's end", NAVITA);
        Check.eq(TurnInDerivation.Source.NONE, r.source(),
                "not all complete: source NONE");
        Check.ok(r.destination() == null,
                "not all complete: no destination");
    }

    /** Toggle off: the exact pre-v27 resolution stands. */
    private static void toggleOffIsOldBehavior() {
        TurnInDerivation.Resolution r = TurnInDerivation.resolve(
                false, true, QUEST, "Storm's end", NAVITA);
        Check.eq(TurnInDerivation.Source.NONE, r.source(),
                "toggle off: source NONE");
        Check.ok(r.destination() == null,
                "toggle off: no destination");
    }

    /** All complete but the NPC cannot be resolved (unknown,
     *  disabled, anchorless): never guessed at. */
    private static void unresolvableNpcIsNothing() {
        TurnInDerivation.Resolution r = TurnInDerivation.resolve(
                true, true, QUEST, "Storm's end", null);
        Check.eq(TurnInDerivation.Source.NONE, r.source(),
                "no spawn: source NONE");
        Check.ok(r.destination() == null,
                "no spawn: no destination");
    }

    /** The derived row's shape: it IS a person, so guidance
     *  lights the NPC itself, and the text says turn in. */
    private static void derivedRowShape() {
        QuestDestinations.Destination d =
                TurnInDerivation.derivedDestination(
                        QUEST, "Storm's end", NAVITA);
        Check.ok(d.glowIsEntity(),
                "derived row glows the entity, not a marker");
        Check.eq("Navita", d.glowEntityName(),
                "derived row's glow entity is the turn-in NPC");
        Check.eq("Turn in to Navita", d.target(),
                "derived row's target names the turn-in");
        Check.eq("Navita", d.place(),
                "derived row's place is the turn-in NPC");
        Check.ok(d.directions().stream()
                        .anyMatch(s -> s.contains("Navita")),
                "derived directions name the turn-in NPC");
    }
}
