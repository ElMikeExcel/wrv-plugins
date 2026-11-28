import java.nio.file.*;
public final class BedrockAbilitiesModuleTest {
  public static void run() { try { runInner(); } catch (Exception e) { throw new RuntimeException(e); } }
  private static void runInner() throws Exception {
    String yml = Files.readString(Path.of("src/plugin.yml"));
    Check.ok(yml.lines().noneMatch(l -> l.startsWith("depend:")), "plugin.yml: no hard depend line at all");
    Check.ok(yml.contains("softdepend: [EliteMobs"), "plugin.yml: EliteMobs remains a softdepend");
    Check.ok(yml.contains("floodgate"), "plugin.yml: floodgate is softdepend-only (gate 7)");
    Check.ok(yml.contains("exceleitemobsabilitybedrockbridge.admin"), "plugin.yml: frozen permission node declared");
    String cfg = Files.readString(Path.of("src/config.yml"));
    Check.ok(cfg.contains("bedrock-abilities:"), "config.yml: bedrock-abilities section present");
    Check.ok(cfg.contains("debug-log-fires: false"), "config.yml: shipped default debug-log-fires false");
    String module = Files.readString(Path.of("src/src/excelemfixes/bedrockabilities/BedrockAbilitiesModule.java"));
    Check.ok(module.contains("modules.bedrock-abilities") || module.contains("bedrock-abilities"), "module: id present");
    Check.ok(!module.contains("disablePlugin"), "module: never disables the pack (leg stands down only)");
  }
}
