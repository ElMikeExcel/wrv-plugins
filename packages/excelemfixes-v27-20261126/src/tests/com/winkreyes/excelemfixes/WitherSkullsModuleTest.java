import excelemfixes.witherskulls.SkullMath;
import excelemfixes.witherskulls.SkullSettings;
import excelemfixes.witherskulls.TopUpDecider;
import java.nio.file.*;
public final class WitherSkullsModuleTest {
  public static void run() { try { runInner(); } catch (Exception e) { throw new RuntimeException(e); } }
  private static void runInner() throws Exception {
    SkullMathTest.run(); SkullSettingsTest.run(); TopUpDeciderTest.run();
    // plugin.yml guard: pack must NOT gain a hard depend; EliteMobs stays softdepend-only
    String yml = Files.readString(Path.of("src/plugin.yml"));
    Check.ok(yml.lines().noneMatch(l -> l.startsWith("depend:")), "plugin.yml: no hard depend line at all");
    Check.ok(yml.contains("softdepend: [EliteMobs"), "plugin.yml: EliteMobs remains a softdepend");
    Check.ok(yml.contains("excelwitherskulls.admin"), "plugin.yml: frozen permission node declared");
    String cfg = Files.readString(Path.of("src/config.yml"));
    Check.ok(cfg.contains("wither-skulls:"), "config.yml: wither-skulls section present");
    Check.ok(cfg.contains("target-chance-percent: 25.0"), "config.yml: shipped default target 25.0");
  }
}
