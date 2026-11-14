package com.winkreyes.exceleitemobsabilitybedrockbridge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 0.2.0 console-admin status checks: the gate arithmetic
 * (console allowed, node-holder allowed, non-admin player
 * denied), the subcommand matcher, the exact status contents
 * for every link state (EliteMobs UP / STANDING BY / DOWN,
 * Geyser complete / incomplete), and the shipped plugin.yml
 * gate (version 0.2.0, the new admin node defaulting to op, no
 * command-level permission locking players out of /abilities).
 * Bukkit-free: runs in both run-tests.sh modes.
 */
public final class StatusCommandTestMain {

    static int checks = 0;
    static int failures = 0;

    static void check(String name, boolean ok, String detail) {
        checks++;
        if (ok) {
            System.out.println("PASS " + name);
        } else {
            failures++;
            System.out.println("FAIL " + name + " :: " + detail);
        }
    }

    static void eq(String name, Object expected, Object actual) {
        check(name, expected == null ? actual == null : expected.equals(actual),
                "expected <" + expected + "> but was <" + actual + ">");
    }

    public static void main(String[] args) throws Exception {
        // Gate arithmetic.
        check("gate console allowed", StatusCommand.isAdmin(true, false), "console denied");
        check("gate node-holder allowed", StatusCommand.isAdmin(false, true), "holder denied");
        check("gate non-admin denied", !StatusCommand.isAdmin(false, false), "non-admin allowed");
        eq("admin node name", "exceleitemobsabilitybedrockbridge.admin", StatusCommand.PERMISSION);
        check("denial names the node", StatusCommand.DENIED.contains("exceleitemobsabilitybedrockbridge.admin"),
                StatusCommand.DENIED);

        // Matcher.
        check("matcher status", StatusCommand.isStatus("status"), "no match");
        check("matcher STATUS", StatusCommand.isStatus("STATUS"), "no match");
        check("matcher rejects stat", !StatusCommand.isStatus("stat"), "matched");
        check("matcher rejects null", !StatusCommand.isStatus(null), "matched");

        // UP state: every fact spelled out.
        List<String> up = StatusCommand.statusLines("0.2.0", true, "10.9.14", true, true, true, false);
        eq("status line count", 4, up.size());
        eq("status line 1", "ExcelEliteMobsAbilityBedrockBridge status: enabled (v0.2.0)", up.get(0));
        check("status UP names EM version + READY",
                up.get(1).contains("UP") && up.get(1).contains("10.9.14") && up.get(1).contains("READY"),
                up.get(1));
        check("status geyser complete",
                up.get(2).contains("Geyser-Spigot enabled") && up.get(2).contains("floodgate enabled")
                        && up.get(2).contains("available"), up.get(2));
        check("status config line", up.get(3).contains("debug-log-fires OFF"), up.get(3));

        // STANDING BY state (EM enabled, ACS not ready).
        List<String> by = StatusCommand.statusLines("0.2.0", true, "10.9.14", false, true, true, true);
        check("status STANDING BY", by.get(1).contains("STANDING BY") && by.get(1).contains("not ready"),
                by.get(1));
        check("status config ON follows input", by.get(3).contains("debug-log-fires ON"), by.get(3));

        // DOWN state (EM absent) + incomplete Geyser.
        List<String> down = StatusCommand.statusLines("0.2.0", false, null, false, false, true, false);
        check("status DOWN", down.get(1).contains("DOWN") && down.get(1).contains("not found"),
                down.get(1));
        check("status geyser incomplete warns",
                down.get(2).contains("NOT enabled") && down.get(2).contains("incomplete"), down.get(2));

        // Unknown EM version is reported as unknown, never invented.
        List<String> unk = StatusCommand.statusLines("0.2.0", true, null, true, true, false, false);
        check("status unknown EM version", unk.get(1).contains("(unknown version)"), unk.get(1));

        // The shipped plugin.yml gate.
        String yml = Files.readString(Path.of("plugin.yml"));
        check("plugin.yml version 0.2.0", yml.contains("version: 0.2.0"), yml.lines().findFirst().orElse(""));
        check("plugin.yml admin node", yml.contains("exceleitemobsabilitybedrockbridge.admin:"), "node missing");
        check("plugin.yml admin default op", yml.contains("default: op"), "default missing");
        check("plugin.yml usage names status", yml.contains("/abilities status"), "usage missing");
        check("plugin.yml NO command-level permission",
                !yml.contains("    permission:"), "a command-level permission would lock players out");

        System.out.println("----");
        System.out.println(checks + " checks, " + failures + " failures");
        System.exit(failures == 0 ? 0 : 1);
    }
}
