import java.nio.file.*;
public final class PartyDropsModuleTest {
  public static void run() { try { runInner(); } catch (Exception e) { throw new RuntimeException(e); } }
  private static void runInner() throws Exception {
    MultiplierMathTest.run(); PartyRangeTest.run(); ParticipantCounterTest.run();
    OriginGateTest.run(); HintGateTest.run();
    // plugin.yml guard: pack must NOT gain a hard depend; EliteMobs stays softdepend-only
    String yml = Files.readString(Path.of("src/plugin.yml"));
    Check.ok(yml.lines().noneMatch(l -> l.startsWith("depend:")), "plugin.yml: no hard depend line at all");
    Check.ok(yml.contains("softdepend: [EliteMobs"), "plugin.yml: EliteMobs remains a softdepend");
    Check.ok(yml.contains("excelpartydrops.admin"), "plugin.yml: frozen permission node declared");
    Check.ok(yml.lines().noneMatch(l -> l.contains("- ExcelTogether") || l.startsWith("softdepend") && l.contains("ExcelTogether")), "plugin.yml: no Together dependency (range math is a copy)");
    String cfg = Files.readString(Path.of("src/config.yml"));
    Check.ok(cfg.contains("party-drops:"), "config.yml: party-drops section present");
    Check.ok(cfg.contains("party-radius: 50.0"), "config.yml: shipped default radius 50.0");
    Check.ok(cfg.contains("step-per-extra: 0.5"), "config.yml: shipped default step 0.5");
    Check.ok(cfg.contains("multiplier-cap: 3.0"), "config.yml: shipped default multiplier cap 3.0");
    Check.ok(cfg.contains("participant-cap: 5"), "config.yml: shipped default participant cap 5");
  }
}
