import excelemfixes.tracker.Expertise;

import java.util.List;

/**
 * The expertise scale (D187): EM quality colors in ladder order,
 * clean thresholds, the count itself colored, and a compact legend
 * carrying every tier's color, range and label.
 */
public final class ExpertiseTest {

    public static void run() {
        System.out.println("ExpertiseTest");
        Check.ok(Expertise.tierFor(0) == null, "no tier at zero");
        Check.eq("Novice", Expertise.tierFor(1).label(), "×1 is Novice");
        Check.eq("Familiar", Expertise.tierFor(2).label(), "×2 is Familiar");
        Check.eq("Familiar", Expertise.tierFor(4).label(), "×4 is Familiar");
        Check.eq("Experienced", Expertise.tierFor(5).label(),
                "×5 is Experienced");
        Check.eq("Experienced", Expertise.tierFor(9).label(),
                "×9 is Experienced");
        Check.eq("Veteran", Expertise.tierFor(10).label(), "×10 is Veteran");
        Check.eq("Veteran", Expertise.tierFor(19).label(), "×19 is Veteran");
        Check.eq("Expert", Expertise.tierFor(20).label(), "×20 is Expert");
        Check.eq("Expert", Expertise.tierFor(39).label(), "×39 is Expert");
        Check.eq("Master", Expertise.tierFor(40).label(), "×40 is Master");
        Check.eq("Master", Expertise.tierFor(74).label(), "×74 is Master");
        Check.eq("Mastered", Expertise.tierFor(75).label(),
                "×75 is Mastered");
        Check.eq("Mastered", Expertise.tierFor(5000).label(),
                "huge counts stay Mastered");

        // Colors follow the EM ladder order.
        Check.eq("&7", Expertise.tierFor(1).color(), "Novice is gray");
        Check.eq("&f", Expertise.tierFor(3).color(), "Familiar is white");
        Check.eq("&a", Expertise.tierFor(7).color(), "Experienced is green");
        Check.eq("&9", Expertise.tierFor(15).color(), "Veteran is blue");
        Check.eq("&5", Expertise.tierFor(30).color(), "Expert is purple");
        Check.eq("&6", Expertise.tierFor(50).color(), "Master is gold");
        Check.eq("&b", Expertise.tierFor(90).color(),
                "Mastered is mythic light-blue");

        // The count NUMBER carries the tier color.
        Check.eq("&7Kills: &5×21", Expertise.countLine("Kills", 21),
                "×21 renders purple (Expert)");
        Check.eq("&7Clears: &f×2", Expertise.countLine("Clears", 2),
                "×2 renders white (Familiar) — same scale for dungeons");
        Check.eq("&7Kills: &8—", Expertise.countLine("Kills", 0),
                "zero renders a plain dash, no fake tier");

        // Legend: header + 7 tier lines + 2 footer lines, every
        // tier present with its range, name in its color.
        List<String> legend = Expertise.legendLines();
        Check.eq(10, legend.size(), "legend is compact (10 lines)");
        String joined = String.join("\n", legend);
        for (String needle : new String[]{"×1", "×2–4", "×5–9", "×10–19",
                "×20–39", "×40–74", "×75+", "Novice", "Familiar",
                "Experienced", "Veteran", "Expert", "Master", "Mastered"}) {
            Check.ok(joined.contains(needle), "legend covers " + needle);
        }
        Check.ok(Expertise.legendButton().detail() != null
                        && Expertise.legendButton().detail().lines()
                                .equals(legend),
                "legend button carries the same lines as its detail");
    }
}
