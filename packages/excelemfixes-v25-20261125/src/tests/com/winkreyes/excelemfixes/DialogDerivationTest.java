import excelemfixes.questdest.DialogDerivation;
import excelemfixes.questdest.QuestDestinations;

import java.util.List;

/**
 * The dialog-destination derivation policy (0.19.0): a tracked
 * quest with no table row derives its destination from its
 * current incomplete DIALOG objective's target NPC; a table
 * row keeps precedence whenever one exists; the toggle off
 * restores the exact pre-0.19.0 table-only behavior.
 */
public class DialogDerivationTest {

    private static final String QUEST =
            "primis_pontis_mille_the_groves_call.yml";
    private static final DialogDerivation.NpcSpawn NEMIAN =
            new DialogDerivation.NpcSpawn(
                    "primis_groves_nemus_leader.yml", "King Nemian",
                    "em_primis", 100.5, 64, -200.5);

    private static QuestDestinations.Destination row() {
        return new QuestDestinations.Destination(
                "primis_pontis_mille_serious_sewage.yml",
                "Serious sewage", "Sewer mutants", "The Sewers",
                "em_primis", "Primis", 1, 2, 3,
                "the sewer grate", "COMPASS",
                List.of("Drop into the grate."), "marker", null);
    }

    public static void run() {
        System.out.println("== DialogDerivation ==");
        noRowDialogDerives();
        tableRowWinsWithoutDialog();
        beamFollowsCurrentObjective();
        tableRowWinsOverDialog();
        toggleOffIsOldBehavior();
        nothingAnywhereIsNothing();
        derivedRowShape();
    }

    /** The headline case: no table row + dialog outstanding
     *  derives the NPC's spawn (Jester / King Nemian shape). */
    private static void noRowDialogDerives() {
        DialogDerivation.Resolution r = DialogDerivation.resolve(
                true, null, QUEST, "The groves call!", NEMIAN);
        Check.eq(DialogDerivation.Source.DIALOG, r.source(),
                "no row + dialog: source DIALOG");
        Check.ok(r.destination() != null,
                "no row + dialog: destination present");
        Check.close(100.5, r.destination().x(), "derived x is NPC spawn x");
        Check.close(64, r.destination().y(), "derived y is NPC spawn y");
        Check.close(-200.5, r.destination().z(), "derived z is NPC spawn z");
        Check.eq("em_primis", r.destination().world(),
                "derived world is NPC spawn world");
        Check.eq(QUEST, r.destination().questFile(),
                "derived row keeps the quest file");
    }

    /** No dialog outstanding: the hand-written row stands
     *  (instance entries EM data cannot express). */
    private static void tableRowWinsWithoutDialog() {
        QuestDestinations.Destination row = row();
        DialogDerivation.Resolution r = DialogDerivation.resolve(
                true, row, row.questFile(), "Serious sewage", null);
        Check.eq(DialogDerivation.Source.TABLE, r.source(),
                "row + no dialog: source TABLE");
        Check.ok(r.destination() == row,
                "row + no dialog: the row itself is returned");
    }

    /** Both present: the table row keeps precedence (rows
     *  encode instance entries EM data cannot express); the
     *  derivation only fills the no-row gap. */
    private static void tableRowWinsOverDialog() {
        QuestDestinations.Destination row = row();
        DialogDerivation.Resolution r = DialogDerivation.resolve(
                true, row, row.questFile(), "Serious sewage", NEMIAN);
        Check.eq(DialogDerivation.Source.TABLE, r.source(),
                "row + dialog: source TABLE (row precedence)");
        Check.ok(r.destination() == row,
                "row + dialog: the row itself is returned");
    }

    /** Beam follows the current objective: a quest with two
     *  dialog objectives and no row derives the FIRST
     *  incomplete one; once it completes, the bridge passes
     *  the next objective's NPC and the destination moves. */
    private static void beamFollowsCurrentObjective() {
        DialogDerivation.NpcSpawn second =
                new DialogDerivation.NpcSpawn(
                        "primis_other_npc.yml", "Second Speaker",
                        "em_primis", 7, 70, 9);
        DialogDerivation.Resolution first = DialogDerivation.resolve(
                true, null, QUEST, "The groves call!", NEMIAN);
        Check.eq("King Nemian", first.destination().place(),
                "first incomplete dialog: beam at first NPC");
        DialogDerivation.Resolution next = DialogDerivation.resolve(
                true, null, QUEST, "The groves call!", second);
        Check.eq("Second Speaker", next.destination().place(),
                "after first dialog completes: beam moves to next NPC");
        Check.close(7, next.destination().x(),
                "moved destination x is next NPC spawn x");
    }

    /** Toggle off = old behavior exactly: derivation never
     *  fires; a row still stands; no row means no destination. */
    private static void toggleOffIsOldBehavior() {
        DialogDerivation.Resolution noRow = DialogDerivation.resolve(
                false, null, QUEST, "The groves call!", NEMIAN);
        Check.eq(DialogDerivation.Source.NONE, noRow.source(),
                "toggle off + dialog + no row: NONE (old behavior)");
        Check.ok(noRow.destination() == null,
                "toggle off + dialog + no row: no destination");
        QuestDestinations.Destination row = row();
        DialogDerivation.Resolution withRow = DialogDerivation.resolve(
                false, row, row.questFile(), "Serious sewage", NEMIAN);
        Check.eq(DialogDerivation.Source.TABLE, withRow.source(),
                "toggle off + dialog + row: TABLE (old behavior)");
        Check.ok(withRow.destination() == row,
                "toggle off + dialog + row: the row itself");
    }

    /** No row, no dialog (or an unresolvable dialog target —
     *  the bridge passes a null spawn): no destination, never
     *  a guess. */
    private static void nothingAnywhereIsNothing() {
        DialogDerivation.Resolution r = DialogDerivation.resolve(
                true, null, QUEST, "The groves call!", null);
        Check.eq(DialogDerivation.Source.NONE, r.source(),
                "no row + no spawn: NONE");
        Check.ok(r.destination() == null,
                "no row + no spawn: no destination");
    }

    /** The derived row is shaped like a table row so the beam,
     *  /questwhere and the glow guide consume it unchanged —
     *  and its glow mode lights the NPC itself. */
    private static void derivedRowShape() {
        QuestDestinations.Destination d =
                DialogDerivation.derivedDestination(
                        QUEST, "The groves call!", NEMIAN);
        Check.eq("Talk to King Nemian", d.target(),
                "derived target names the talk");
        Check.eq("King Nemian", d.place(),
                "derived place is the NPC");
        Check.ok(d.glowIsEntity(),
                "derived glow mode is entity (lights the NPC)");
        Check.eq("King Nemian", d.glowEntityName(),
                "derived glow entity is the NPC name");
        Check.ok(!d.directions().isEmpty(),
                "derived row carries directions text");
        // A blank quest name falls back to the file, never blank.
        QuestDestinations.Destination unnamed =
                DialogDerivation.derivedDestination(QUEST, null, NEMIAN);
        Check.eq(QUEST, unnamed.questName(),
                "derived quest name falls back to the file");
    }
}
