import excelemfixes.jobsdummy.DummyBossList;
import java.util.List;

/** Jobs Dummy Gate boss-list parsing, normalization, and scope. */
public final class DummyBossListTest {

    public static void run() {
        System.out.println("DummyBossListTest");
        // The shipped default list (EliteMobs' five guild dummies).
        DummyBossList shipped = DummyBossList.parse(List.of(
                "training_dummy_lv1", "training_dummy_lv25",
                "training_dummy_lv50", "training_dummy_lv75",
                "training_dummy_lv100"));
        Check.eq(5, shipped.size(), "shipped default list has five bosses");
        Check.ok(shipped.isVetoed("training_dummy_lv1.yml"),
                "lv1 vetoed (filename with .yml)");
        Check.ok(shipped.isVetoed("training_dummy_lv25"),
                "lv25 vetoed (bare id)");
        Check.ok(shipped.isVetoed("training_dummy_lv50.yml"),
                "lv50 vetoed");
        Check.ok(shipped.isVetoed("training_dummy_lv75.yml"),
                "lv75 vetoed");
        Check.ok(shipped.isVetoed("training_dummy_lv100.yml"),
                "lv100 vetoed");

        // Normalization: case and whitespace on either side.
        Check.ok(shipped.isVetoed("Training_Dummy_LV1.YML"),
                "matching ignores case and extension case");
        Check.ok(shipped.isVetoed("  training_dummy_lv1.yml  "),
                "matching trims whitespace");
        DummyBossList messy = DummyBossList.parse(List.of(
                " Training_Dummy_LV25.yml ", "training_dummy_lv25",
                "training_dummy_lv25.yml"));
        Check.eq(1, messy.size(), "duplicates collapse after normalization");

        // Scope: ONLY the five guild dummies. Everything else pays.
        Check.ok(!shipped.isVetoed("class_trial_training_construct.yml"),
                "class-trial construct NOT vetoed (keeps paying by design)");
        Check.ok(!shipped.isVetoed("class_trial_training_wolf.yml"),
                "class-trial wolf NOT vetoed (keeps paying by design)");
        Check.ok(!shipped.isVetoed("training_dummy_lv2.yml"),
                "unlisted dummy level NOT vetoed");
        Check.ok(!shipped.isVetoed("test_boss.yml"),
                "unrelated boss NOT vetoed");
        Check.ok(!shipped.isVetoed("primis_bloodtemple_climb_miniboss_1_p4.yml"),
                "real boss NOT vetoed");
        Check.ok(!shipped.isVetoed("husk"),
                "entity type is never a key");

        // Degenerate inputs.
        Check.eq(0, DummyBossList.parse(null).size(), "null list parses empty");
        Check.eq(0, DummyBossList.parse("junk").size(), "non-list parses empty");
        Check.eq(0, DummyBossList.parse(List.of("", "  ", ".yml")).size(),
                "blank entries are dropped");
        Check.ok(!DummyBossList.parse(null).isVetoed("training_dummy_lv1.yml"),
                "empty list vetoes nothing");
        Check.ok(!shipped.isVetoed(null), "null filename is not vetoed");
        Check.ok(!shipped.isVetoed(""), "empty filename is not vetoed");

        // normalize() itself.
        Check.eq("training_dummy_lv1",
                DummyBossList.normalize("Training_Dummy_LV1.yml"),
                "normalize strips .yml and lower-cases");
        Check.eq("training_dummy_lv1",
                DummyBossList.normalize("training_dummy_lv1"),
                "normalize leaves a bare id alone");
        Check.eq("", DummyBossList.normalize(null), "normalize null -> empty");
    }
}
