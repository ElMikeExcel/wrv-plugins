import java.nio.file.Files;
import java.nio.file.Path;

/** plugin.yml / config.yml identity guards (house pattern). */
public final class PluginYmlGuardTest {
    public static void run() throws Exception {
        System.out.println("PluginYmlGuardTest:");
        String yml = Files.readString(Path.of("plugin.yml"));
        Check.ok(yml.contains("name: ExcelItems"), "plugin name is ExcelItems");
        Check.ok(yml.contains("main: com.winkreyes.excelitems.ExcelItems"), "main class pin");
        Check.ok(yml.contains("version: 0.1.0"), "version pin 0.1.0");
        Check.ok(yml.contains("excelitems:"), "admin command declared");
        Check.ok(yml.contains("excelitems.admin:"), "admin permission declared");
        Check.ok(!yml.lines().anyMatch(l -> l.startsWith("depend:")),
                "no hard depend (ExcelItems IS the dependency others declare)");

        String cfg = Files.readString(Path.of("config.yml"));
        Check.ok(cfg.contains("1: veil_potion"), "config assignment id 1");
        Check.ok(cfg.contains("2: deathprotection_basic"), "config assignment id 2");
        Check.ok(cfg.contains("3: deathpotion"), "config assignment id 3");
        Check.ok(cfg.contains("4: revive_potion"), "config assignment id 4");
    }
}
