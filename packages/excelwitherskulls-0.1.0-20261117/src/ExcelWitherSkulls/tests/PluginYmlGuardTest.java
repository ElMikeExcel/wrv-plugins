import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * plugin.yml guards (house law, mirrored from ExcelVillageExits):
 * EliteMobs is a SOFT depend only — the plugin must boot and stay
 * inert-but-harmless without it (the seam fails safe), never fail
 * to load. Also pins the identity fields.
 */
public final class PluginYmlGuardTest {

    public static void run() throws Exception {
        System.out.println("PluginYmlGuardTest");
        Path yml = Path.of("plugin.yml");
        Check.ok(Files.exists(yml), "plugin.yml present in project root");
        String text = Files.readString(yml, StandardCharsets.UTF_8);

        Check.ok(text.contains("softdepend: [EliteMobs]"), "EliteMobs is softdepend");
        boolean hardDepend = text.lines().anyMatch(l -> l.startsWith("depend:"));
        Check.ok(!hardDepend, "no hard depend line at all");
        Check.ok(text.contains("name: ExcelWitherSkulls"), "plugin name pinned");
        Check.ok(text.contains("version: 0.1.0"), "version 0.1.0 pinned");
        Check.ok(text.contains("excelwitherskulls.admin"), "admin permission declared");
    }
}
