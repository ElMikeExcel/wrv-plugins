/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.io.File;
import java.nio.file.Files;

/**
 * 0.5.7.1: the three-home BuildInfo stamp guard. The 0.5.7 build
 * updated plugin.yml and excel-build.txt but missed BuildInfo.java,
 * so the shipped jar booted as v0.5.7 while its Build: line printed
 * the stale 0.5.6 build ID - and no harness could see the split.
 * This harness pins the three fingerprint homes to each other on
 * every build: BuildInfo.VERSION must equal the plugin.yml version,
 * BuildInfo.BUILD_ID must equal the excel-build.txt build-id, the
 * excel-build.txt version must equal the plugin.yml version, and
 * BUILD_ID must carry the EXC-BOSSTRIAL-<VERSION>-WRV- prefix
 * (prefix agreement, in the fleet shape of ExcelMagicGate's
 * BUILD_ID prefix assertion). Any mismatch fails the build before
 * a jar exists. Plain-JVM, file reads only from the tree root
 * (build-test.sh runs from there, as PluginYmlParseTest relies on).
 */
public final class BuildInfoStampTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    private static String field(File file, String key) throws Exception {
        for (String line : Files.readAllLines(file.toPath())) {
            String trimmed = line.trim();
            if (trimmed.startsWith(key)) {
                return trimmed.substring(key.length()).trim();
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        File pluginYml = new File("plugin.yml");
        File excelBuild = new File("excel-build.txt");
        check(pluginYml.isFile(), "plugin.yml present at tree root");
        check(excelBuild.isFile(), "excel-build.txt present at tree root");

        String pluginVersion = field(pluginYml, "version:");
        String buildId = field(excelBuild, "build-id:");
        String buildVersion = field(excelBuild, "version:");

        check(pluginVersion != null && BuildInfo.VERSION.equals(pluginVersion),
                "BuildInfo.VERSION equals plugin.yml version (BuildInfo="
                        + BuildInfo.VERSION + ", plugin.yml=" + pluginVersion + ")");
        check(buildVersion != null && BuildInfo.VERSION.equals(buildVersion),
                "BuildInfo.VERSION equals excel-build.txt version (BuildInfo="
                        + BuildInfo.VERSION + ", excel-build.txt=" + buildVersion + ")");
        check(pluginVersion != null && pluginVersion.equals(buildVersion),
                "excel-build.txt version equals plugin.yml version");
        check(buildId != null && BuildInfo.BUILD_ID.equals(buildId),
                "BuildInfo.BUILD_ID equals excel-build.txt build-id (BuildInfo="
                        + BuildInfo.BUILD_ID + ", excel-build.txt=" + buildId + ")");
        check(BuildInfo.BUILD_ID.startsWith("EXC-BOSSTRIAL-" + BuildInfo.VERSION + "-WRV-"),
                "BuildInfo.BUILD_ID starts with EXC-BOSSTRIAL-<VERSION>-WRV-");
        check(pluginVersion != null
                        && BuildInfo.BUILD_ID.startsWith("EXC-BOSSTRIAL-" + pluginVersion + "-WRV-"),
                "BuildInfo.BUILD_ID prefix agrees with plugin.yml version");

        System.out.println("BuildInfoStampTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
