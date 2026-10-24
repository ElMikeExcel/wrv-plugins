import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 1.4.1 regression guard: the DeluxeMenus hard dependency must never come
 * back. It was load-order scaffolding for an API this plugin never called,
 * and after the ExcelMenu swap retired DeluxeMenus it kept the whole
 * plugin (and with it the server-menu compass) from loading at all.
 * Reads the shipped plugin.yml from the project directory.
 */
public final class PluginYmlGuardTest {
    public static void run() {
        System.out.println("PluginYmlGuardTest:");
        String text;
        List<String> lines;
        try {
            Path yml = Path.of("plugin.yml");
            text = Files.readString(yml);
            lines = Files.readAllLines(yml);
        } catch (Exception e) {
            Check.ok(false, "plugin.yml readable from project dir (" + e.getMessage() + ")");
            return;
        }
        Check.ok(lines.stream().anyMatch(l -> l.trim().equals("version: 1.4.2")),
                "plugin.yml version is 1.4.2");
        boolean hardDependLine = lines.stream()
                .map(String::trim)
                .anyMatch(l -> l.startsWith("depend:") || l.startsWith("depend :"));
        Check.ok(!hardDependLine, "plugin.yml declares no hard 'depend:' line at all");
        boolean dmInSoftdepend = lines.stream()
                .map(String::trim)
                .anyMatch(l -> l.startsWith("softdepend:") && l.contains("DeluxeMenus"));
        Check.ok(!dmInSoftdepend, "DeluxeMenus is not in softdepend either (nothing to order against)");
        Check.ok(lines.stream().anyMatch(l -> l.trim().startsWith("softdepend: [PlaceholderAPI, ExcelPouch]")),
                "softdepend unchanged: [PlaceholderAPI, ExcelPouch]");
        Check.ok(text.contains("main: com.winkreyesvalley.excelmenujoin.ExcelMenuJoin"),
                "main class unchanged");
    }
}
