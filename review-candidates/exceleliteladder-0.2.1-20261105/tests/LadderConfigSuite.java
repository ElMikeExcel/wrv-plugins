import com.winkreyes.exceleliteladder.core.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * ExcelEliteLadder suites for the config table (0.2.0; retune values 0.2.1):
 *
 *   1. SHIPPED-TABLE EQUALITY — the shipped default ladder.yml, parsed by the
 *      same YAML engine family the server uses, must yield a table equal to
 *      the built-in default table (LadderTable.builtin(); since 0.2.1 the
 *      Nov 5, 2026 retune) value-for-value across every step and every one
 *      of the six MobCombat keys, and must resolve every staged fixture to
 *      the same step.
 *   2. MALFORMED BATTERY — every defect class is refused with the defect
 *      named, and a holder following the plugin's policy (refuse -> keep
 *      last-good) provably keeps the previous table instance.
 *   3. RELOAD FLIP — a valid edited table takes over without restart; a
 *      broken edit afterwards leaves the flipped table in force.
 */
public final class LadderConfigSuite {

    /** The plugin's load policy in miniature: refused loads never touch the live table. */
    static final class Holder {
        LadderTable current = LadderTable.builtin();
        String defect = null;

        boolean tryLoad(Map<String, ?> root) {
            try {
                current = LadderConfig.parseRoot(root);
                defect = null;
                return true;
            } catch (LadderConfigException ex) {
                defect = ex.getMessage();
                return false;
            }
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> loadYaml(Path p) throws IOException {
        Object root = new Yaml().load(Files.readString(p, StandardCharsets.UTF_8));
        return (Map<String, Object>) root;
    }

    static Map<String, Object> goodRoot() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("ladder", LadderConfig.defaultSectionMap());
        return root;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> section(Map<String, Object> root) {
        return (Map<String, Object>) root.get("ladder");
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> row(Map<String, Object> root, int n) {
        return (Map<String, Object>) section(root).get("step-" + n);
    }

    static void assertTablesEqual(LadderTable expected, LadderTable actual, String label) {
        for (int n = LadderTable.MIN_STEP; n <= LadderTable.MAX_STEP; n++) {
            for (String key : MobCombatFile.KEYS) {
                Check.close(expected.step(n).values().get(key), actual.step(n).values().get(key),
                        label + " S" + n + " " + key);
            }
        }
    }

    static void run(String ladderYmlPath) throws Exception {
        System.out.println("0.2.1 shipped-table equality: shipped ladder.yml == the built-in default (Nov 5, 2026 retune)");
        LadderTable builtin = LadderTable.builtin();
        Map<String, Object> shipped = loadYaml(Paths.get(ladderYmlPath));
        LadderTable fromConfig = LadderConfig.parseRoot(shipped);
        assertTablesEqual(builtin, fromConfig, "ladder.yml");
        LadderTable fromMap = LadderConfig.parse(LadderConfig.defaultSectionMap());
        assertTablesEqual(builtin, fromMap, "defaultSectionMap");
        for (String name : List.of("S1-norm", "S2-norm", "S3-norm", "S4-norm", "S5-norm", "old-S0", "old-S1")) {
            Map<String, Double> v = MobCombatFile.read(TestMain.fx(name + "-MobCombatSettings.yml"));
            Check.eq(builtin.matchStep(v), fromConfig.matchStep(v), "matchStep parity on " + name);
        }

        System.out.println("0.2.0 malformed battery: every defect refused, last-good kept");
        Holder h = new Holder();
        Check.ok(h.tryLoad(goodRoot()), "holder loads the good table");
        LadderTable good = h.current;

        refuse(h, good, "missing step-4", r -> section(r).remove("step-4"), "ladder.step-4 is missing");
        refuse(h, good, "extra step-6", r -> section(r).put("step-6", row(goodRoot(), 5)), "unknown step key 'step-6'");
        refuse(h, good, "misnamed step-02", r -> { Object v = section(r).remove("step-2"); section(r).put("step-02", v); },
                "unknown step key 'step-02'");
        refuse(h, good, "missing field scaled-to-player", r -> row(r, 2).remove("scaled-to-player"),
                "ladder.step-2 is missing 'scaled-to-player'");
        refuse(h, good, "typo key spwan", r -> { row(r, 1).remove("spawn"); row(r, 1).put("spwan", 0.07); },
                "ladder.step-1 has unknown key 'spwan'");
        refuse(h, good, "spawn as text", r -> row(r, 3).put("spawn", "lots"), "ladder.step-3.spawn must be a number");
        refuse(h, good, "cap as text", r -> row(r, 3).put("cap", "many"), "ladder.step-3.cap must be a number");
        refuse(h, good, "cap not whole", r -> row(r, 2).put("cap", 120.5), "ladder.step-2.cap must be a whole number");
        refuse(h, good, "spawn zero", r -> row(r, 1).put("spawn", 0), "ladder.step-1.spawn must be > 0 and <= 1");
        refuse(h, good, "spawn above 1", r -> row(r, 1).put("spawn", 1.5), "ladder.step-1.spawn must be > 0 and <= 1");
        refuse(h, good, "cap zero", r -> row(r, 4).put("cap", 0), "ladder.step-4.cap must be between 1 and 1000");
        refuse(h, good, "negative multiplier", r -> row(r, 2).put("scaled-to-player", -1),
                "ladder.step-2.scaled-to-player must be > 0 and <= 10");
        refuse(h, good, "multiplier above 10", r -> row(r, 2).put("normalized-to-player", 99),
                "ladder.step-2.normalized-to-player must be > 0 and <= 10");
        refuse(h, good, "NaN spawn", r -> row(r, 5).put("spawn", Double.NaN), "ladder.step-5.spawn must be a finite number");
        refuse(h, good, "infinite multiplier", r -> row(r, 5).put("scaled-to-elite", Double.POSITIVE_INFINITY),
                "ladder.step-5.scaled-to-elite must be a finite number");
        refuse(h, good, "boolean spawn", r -> row(r, 1).put("spawn", Boolean.TRUE), "ladder.step-1.spawn must be a number");
        refuse(h, good, "blank cap", r -> row(r, 1).put("cap", null), "ladder.step-1.cap is blank");
        refuse(h, good, "spawn decreases at step-3", r -> row(r, 3).put("spawn", 0.05), "ladder.spawn must not decrease");
        refuse(h, good, "cap decreases at step-4", r -> row(r, 4).put("cap", 100), "ladder.cap must not decrease");
        refuse(h, good, "dealt decreases at step-4", r -> row(r, 4).put("scaled-to-player", 1.5),
                "ladder.scaled-to-player must not decrease");
        refuse(h, good, "taken increases at step-2", r -> row(r, 2).put("scaled-to-elite", 0.60),
                "ladder.scaled-to-elite must not increase");
        refuse(h, good, "step row not a mapping", r -> section(r).put("step-3", 42), "ladder.step-3 must be a mapping");
        refuse(h, good, "ladder section not a mapping", r -> r.put("ladder", 42), "'ladder:' must be a mapping");
        refuse(h, good, "no ladder section at all", r -> r.remove("ladder"), "no 'ladder:' section");
        refuse(h, good, "empty file (null root)", r -> { }, "no 'ladder:' section found", true);

        // YAML-text level: garbage never reaches the table, and a .nan in the
        // shipped file's own text is refused by the finite check.
        boolean yamlThrew = false;
        try {
            new Yaml().load("ladder: [unclosed");
        } catch (YAMLException ex) {
            yamlThrew = true;
        }
        Check.ok(yamlThrew, "broken YAML text throws at parse (plugin converts this to a refusal)");
        String nanText = Files.readString(Paths.get(ladderYmlPath), StandardCharsets.UTF_8)
                .replaceFirst("spawn: 0\\.10", "spawn: .nan");
        Map<String, Object> nanRoot = (Map<String, Object>) new Yaml().load(nanText);
        Holder hNan = new Holder();
        Check.ok(!hNan.tryLoad(nanRoot) && hNan.defect != null && hNan.defect.contains("finite"),
                "shipped text with spawn .nan refused (" + hNan.defect + ")");
        Check.ok(tablesEqual(hNan.current, builtin),
                "first-boot refusal keeps the built-in default");

        System.out.println("0.2.0 reload flip: valid edit applies without restart, broken edit keeps it");
        Holder flip = new Holder();
        Check.ok(flip.tryLoad(goodRoot()), "flip holder starts on the shipped table");
        Map<String, Object> edited = goodRoot();
        // The peak plateau is S4==S5: retuning the peak spawn means editing both
        // rows (editing S4 alone is refused by monotonicity — spawn may never
        // decrease, and 0.20 -> 0.17 would, at S5).
        row(edited, 4).put("spawn", 0.20);
        row(edited, 5).put("spawn", 0.20);
        Check.ok(flip.tryLoad(edited), "edited table (peak spawn 0.20 on S4+S5) accepted");
        Check.close(0.20, flip.current.step(4).spawn(), "live table now has S4 spawn 0.20");
        LadderTable flipped = flip.current;
        String s4edited = MobCombatFile.patch(TestMain.fx("S3-norm-MobCombatSettings.yml"),
                new LadderStep(4, 0.20, 150, 0.5, 2.0, 0.35, 1.94));
        Map<String, Double> editedValues = MobCombatFile.read(s4edited);
        Check.eq(4, flip.current.matchStep(editedValues), "edited S4 file matches the flipped table at S4");
        Check.eq(0, builtin.matchStep(editedValues), "edited S4 file is off the shipped table (the flip is real)");
        Map<String, Object> broken = goodRoot();
        section(broken).remove("step-5");
        Check.ok(!flip.tryLoad(broken), "broken edit after the flip refused");
        Check.ok(flip.current == flipped, "last-good kept: still the flipped table instance");
        Check.close(0.20, flip.current.step(4).spawn(), "S4 spawn still 0.20 after the refused reload");
    }

    static boolean tablesEqual(LadderTable a, LadderTable b) {
        for (int n = LadderTable.MIN_STEP; n <= LadderTable.MAX_STEP; n++) {
            for (String key : MobCombatFile.KEYS) {
                if (Math.abs(a.step(n).values().get(key) - b.step(n).values().get(key)) > 1e-9) return false;
            }
        }
        return true;
    }

    interface Mutator {
        void apply(Map<String, Object> root);
    }

    static void refuse(Holder h, LadderTable good, String name, Mutator m, String fragment) {
        refuse(h, good, name, m, fragment, false);
    }

    static void refuse(Holder h, LadderTable good, String name, Mutator m, String fragment, boolean nullRoot) {
        Map<String, Object> root = goodRoot();
        m.apply(root);
        boolean accepted = h.tryLoad(nullRoot ? null : root);
        Check.ok(!accepted, name + " refused");
        Check.ok(h.defect != null && h.defect.contains(fragment),
                name + " names the defect (" + h.defect + ")");
        Check.ok(h.current == good, name + ": last-good table instance kept");
    }
}
