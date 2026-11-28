import excelemfixes.questdest.FetchDerivation;
import excelemfixes.questdest.QuestDestinations;

import java.util.List;
import java.util.Map;

/**
 * The fetch-destination derivation policy (v26, FIX-LIST B1):
 * a no-table-row quest whose CURRENT objective is FETCH_ITEM
 * derives its destination from the item's source in EliteMobs'
 * data — open-world dropper centroid first, instance entry
 * for instance-only droppers, treasure chest last — and from
 * nothing when EliteMobs' data names no source.
 */
public class FetchDerivationTest {

    private static final String QUEST = "xmas_quest_0.yml";
    private static final String ITEM = "xmas_lost_present.yml";

    private static FetchDerivation.Point p(String w, double x, double y, double z) {
        return new FetchDerivation.Point(w, x, y, z);
    }

    public static void run() {
        System.out.println("== FetchDerivation ==");
        openWorldDropperCentroid();
        b5NorthPoleShape();
        instanceOnlyDropperUsesWormholeEntry();
        instanceOnlyDropperUsesTeleporterNpc();
        instanceWithoutEntryIsNothing();
        chestFallback();
        dropperBeatsChest();
        determinismAcrossOrderings();
        noSourceIsNothing();
        centroidMath();
    }

    /** (i) A boss dropping the item in an open world: the
     *  destination is the centroid of its spawns there. */
    private static void openWorldDropperCentroid() {
        FetchDerivation.Resolution r = FetchDerivation.resolve(
                QUEST, "Find the lost gifts!", ITEM, "Lost Present",
                List.of(new FetchDerivation.BossSource(
                        "nasty_worm.yml", "Nasty Worm",
                        List.of(p("em_primis", 1160, 23, 386),
                                p("em_primis", 1166, 23, 392)))),
                List.of(), w -> null);
        Check.eq(FetchDerivation.Kind.BOSS, r.kind(),
                "open-world dropper: kind BOSS");
        Check.ok(r.destination() != null, "open-world dropper: destination");
        Check.close(1163, r.destination().x(), "centroid x");
        Check.close(23, r.destination().y(), "centroid y");
        Check.close(389, r.destination().z(), "centroid z");
        Check.eq("em_primis", r.destination().world(), "centroid world");
        Check.eq("Lost Present", r.destination().target(),
                "target is the item");
        Check.ok(r.destination().directions().get(0)
                        .contains("Nasty Worm"),
                "directions name the dropper");
    }

    /** The B5 specimen shape (Nov 26, 2026): xmas_lost_present
     *  drops from the North Pole bosses, all spawning in the
     *  shared em_north_pole world — sorted-filename-first
     *  dropper wins and its single spawn is the destination. */
    private static void b5NorthPoleShape() {
        List<FetchDerivation.BossSource> droppers = List.of(
                new FetchDerivation.BossSource(
                        "northpole_santa_claus.yml", "Santa Claus",
                        List.of(p("em_north_pole", -323.5, 59.2, -447.4))),
                new FetchDerivation.BossSource(
                        "northpole_elf.yml", "Workshop Elf",
                        List.of(p("em_north_pole", -304, 65.2, -415.3),
                                p("em_north_pole", -310, 65.2, -431.3))));
        FetchDerivation.Resolution r = FetchDerivation.resolve(
                QUEST, "Find the lost gifts!", ITEM, "Lost Present",
                droppers, List.of(), w -> null);
        Check.eq(FetchDerivation.Kind.BOSS, r.kind(), "B5 shape: kind BOSS");
        Check.eq("em_north_pole", r.destination().world(),
                "B5 shape: north pole world");
        Check.close(-307, r.destination().x(),
                "B5 shape: elf centroid x (elf sorts first)");
        Check.close(-423.3, r.destination().z(),
                "B5 shape: elf centroid z");
    }

    /** (ii) Droppers only inside em_id_* worlds: the wormhole
     *  entry in the parent world is the destination. */
    private static void instanceOnlyDropperUsesWormholeEntry() {
        Map<String, FetchDerivation.EntrySource> entries = Map.of(
                "em_id_primis_gladius", new FetchDerivation.EntrySource(
                        "Instance wormhole",
                        p("em_primis", 512.5, 42, 724.5), false));
        FetchDerivation.Resolution r = FetchDerivation.resolve(
                "primis_caeruleaum_fletching.yml", "Arrow Hunter",
                "grappling_arrow.yml", "Grappling Arrow",
                List.of(new FetchDerivation.BossSource(
                        "invading_assassinator.yml", "Invading Assassinator",
                        List.of(p("em_id_primis_gladius", 10, 64, 10)))),
                List.of(), entries::get);
        Check.eq(FetchDerivation.Kind.INSTANCE, r.kind(),
                "instance-only: kind INSTANCE");
        Check.close(512.5, r.destination().x(), "instance entry x");
        Check.eq("em_primis", r.destination().world(),
                "instance entry is in the parent world");
        Check.ok(!r.destination().glowIsEntity(),
                "wormhole entry glows as a marker");
    }

    /** (ii) Teleporter-NPC entry: the destination glows the
     *  NPC itself (entity mode), like the dialog derivation. */
    private static void instanceOnlyDropperUsesTeleporterNpc() {
        Map<String, FetchDerivation.EntrySource> entries = Map.of(
                "em_id_primis_gladius", new FetchDerivation.EntrySource(
                        "Pontis Soldier",
                        p("em_primis", 666.5, 22, 766.5), true));
        FetchDerivation.Resolution r = FetchDerivation.resolve(
                "primis_caeruleaum_fletching.yml", "Arrow Hunter",
                "grappling_arrow.yml", "Grappling Arrow",
                List.of(new FetchDerivation.BossSource(
                        "invading_assassinator.yml", "Invading Assassinator",
                        List.of(p("em_id_primis_gladius", 10, 64, 10)))),
                List.of(), entries::get);
        Check.eq(FetchDerivation.Kind.INSTANCE, r.kind(),
                "teleporter entry: kind INSTANCE");
        Check.ok(r.destination().glowIsEntity(),
                "teleporter entry glows the NPC entity");
        Check.eq("Pontis Soldier", r.destination().glowEntityName(),
                "teleporter entry names the NPC");
    }

    /** Instance-only dropper with NO known entry: no
     *  destination — the entry is never guessed. */
    private static void instanceWithoutEntryIsNothing() {
        FetchDerivation.Resolution r = FetchDerivation.resolve(
                QUEST, "q", ITEM, "Lost Present",
                List.of(new FetchDerivation.BossSource(
                        "boss.yml", "Boss",
                        List.of(p("em_id_nowhere", 1, 2, 3)))),
                List.of(), w -> null);
        Check.eq(FetchDerivation.Kind.NONE, r.kind(),
                "instance without entry: NONE");
        Check.ok(r.destination() == null,
                "instance without entry: no destination");
    }

    /** (iii) No dropper at all: a chest holding the item. */
    private static void chestFallback() {
        FetchDerivation.Resolution r = FetchDerivation.resolve(
                "primis_pontis_mille_late_delivery.yml", "Late delivery",
                "fish_nets.yml", "Fish Nets",
                List.of(),
                List.of(new FetchDerivation.ChestSource(
                        "fish_nets_chest.yml",
                        p("em_primis", 805, 15, 904))),
                w -> null);
        Check.eq(FetchDerivation.Kind.CHEST, r.kind(), "chest: kind CHEST");
        Check.close(805, r.destination().x(), "chest x");
        Check.close(904, r.destination().z(), "chest z");
    }

    /** Source precedence: a dropper beats a chest (the census
     *  order — the kill channel is the primary source). */
    private static void dropperBeatsChest() {
        FetchDerivation.Resolution r = FetchDerivation.resolve(
                QUEST, "q", ITEM, "Lost Present",
                List.of(new FetchDerivation.BossSource(
                        "boss.yml", "Boss",
                        List.of(p("em_primis", 1, 2, 3)))),
                List.of(new FetchDerivation.ChestSource(
                        "chest.yml", p("em_primis", 9, 9, 9))),
                w -> null);
        Check.eq(FetchDerivation.Kind.BOSS, r.kind(),
                "dropper beats chest");
    }

    /** Registry order must not matter: the same sources in a
     *  different order resolve to the same destination. */
    private static void determinismAcrossOrderings() {
        FetchDerivation.BossSource a = new FetchDerivation.BossSource(
                "a_boss.yml", "A", List.of(p("em_primis", 0, 64, 0)));
        FetchDerivation.BossSource b = new FetchDerivation.BossSource(
                "b_boss.yml", "B", List.of(p("em_primis", 100, 64, 100)));
        QuestDestinations.Destination d1 = FetchDerivation.resolve(
                QUEST, "q", ITEM, "i", List.of(a, b), List.of(), w -> null)
                .destination();
        QuestDestinations.Destination d2 = FetchDerivation.resolve(
                QUEST, "q", ITEM, "i", List.of(b, a), List.of(), w -> null)
                .destination();
        Check.eq(d1.coords(), d2.coords(),
                "sorted-filename winner is order-independent");
        Check.close(0, d1.x(), "a_boss (sorted first) wins");
    }

    /** No source in EliteMobs' data: NONE, never a guess. */
    private static void noSourceIsNothing() {
        FetchDerivation.Resolution r = FetchDerivation.resolve(
                QUEST, "q", ITEM, "Lost Present",
                List.of(), List.of(), w -> null);
        Check.eq(FetchDerivation.Kind.NONE, r.kind(), "no source: NONE");
        Check.ok(r.destination() == null, "no source: no destination");
    }

    private static void centroidMath() {
        FetchDerivation.Point c = FetchDerivation.centroid(
                List.of(p("w", 0, 10, 0), p("w", 10, 20, 30),
                        p("other", 999, 999, 999)), "w");
        Check.close(5, c.x(), "centroid x averages same-world only");
        Check.close(15, c.z(), "centroid z averages same-world only");
        Check.ok(FetchDerivation.centroid(
                List.of(p("w", 1, 2, 3)), "nope") == null,
                "centroid of an absent world is null");
        Check.ok(FetchDerivation.isInstanceWorld("em_id_primis_gladius"),
                "em_id_ worlds are instanced");
        Check.ok(!FetchDerivation.isInstanceWorld("em_north_pole"),
                "shared dungeon worlds are not em_id_ instances");
    }
}
