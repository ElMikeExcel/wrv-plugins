import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 1.4.1 regression guard: the DeluxeMenus hard dependency must never come
 * back. It was load-order scaffolding for an API this plugin never called,
 * and after the ExcelMenu swap retired DeluxeMenus it kept the whole
 * plugin (and with it the server-menu compass) from loading at all.
 * 1.4.4 extends the guard to the console-admin wiring: the version,
 * the new excelmenujoin.admin node defaulting to op, the usage naming
 * all three admin forms, and the self-service wiring (command-level
 * permission, both player nodes) unchanged.
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
        Check.ok(lines.stream().anyMatch(l -> l.trim().equals("version: 1.4.4")),
                "plugin.yml version is 1.4.4");
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
        // 1.4.2 console-admin wiring
        Check.ok(text.contains("  excelmenujoin.admin:"),
                "plugin.yml declares the excelmenujoin.admin permission");
        Check.ok(lines.stream().anyMatch(l -> l.trim().equals("default: op")),
                "plugin.yml: the admin permission defaults to op (console satisfies op)");
        Check.ok(text.contains("/excelmenujoin admin status <player>"),
                "plugin.yml: usage names the admin status form");
        Check.ok(text.contains("/excelmenujoin admin toggle <player>"),
                "plugin.yml: usage names the admin toggle form");
        Check.ok(text.contains("/excelmenujoin admin compass <player>"),
                "plugin.yml: usage names the admin compass form");
        Check.ok(text.contains("    permission: excelmenujoin.toggle"),
                "plugin.yml: command-level permission unchanged (excelmenujoin.toggle; the admin gate lives in code)");
        Check.ok(text.contains("  excelmenujoin.compass:"),
                "plugin.yml: the compass permission node is still declared");
    }
}
