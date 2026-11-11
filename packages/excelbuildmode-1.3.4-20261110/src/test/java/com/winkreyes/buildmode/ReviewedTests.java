package com.winkreyes.buildmode;

import com.winkreyes.buildmode.core.*;
import com.winkreyes.buildmode.paper.BuildPolicy;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * ExcelBuildMode 1.3.4: regression pins for the reviewed-builder
 * exemption recovered from the October 1.3.3 "reviewed" build (the live
 * jar, SHA-256 3662dcb7…; recovery record: REVIEWED-DELTA-RECOVERY.md
 * in the 1.3.4 build folder). The recovered behavior, pinned here at
 * the pure-policy layer exactly as the preserved jar's bytecode
 * implements it:
 *
 *   - entry: BuildPolicy.refusesOutsideWorldEdit(worldEdit, reviewed,
 *     hasOutsideWorldEdit) == worldEdit && !reviewed && hasOutside.
 *     A reviewed holder is NEVER refused for standing WorldEdit; a
 *     non-reviewed holder with standing WorldEdit still is.
 *   - exit: BuildPolicy.probesResidualWorldEdit(worldEdit, reviewed)
 *     == worldEdit && !reviewed. The residual-permission probe after
 *     attachment removal runs only for non-reviewed builders.
 *
 * The Bukkit call sites (enter / removeAttachment in ExcelBuildMode)
 * pass worldEdit = the plugin's temporary-worldedit state and
 * reviewed = p.hasPermission(BuildPolicy.REVIEWED_PERMISSION); their
 * wiring is bytecode-verified against the preserved jar (normalized
 * javap match, see the recovery record) and exercised live by the
 * 1.3.4 rig boot. Run via build.sh.
 */
public final class ReviewedTests {
    private static int assertions;

    public static void main(String[] args) throws Exception {
        entryTruthTable(); exitTruthTable(); pluginYmlPins(); probeSetPins();
        System.out.println("PASS " + assertions + " assertions (reviewed-builder exemption: entry refusal + exit probe, recovered 1.3.3 behavior)");
        Files.writeString(Path.of("target/reviewed-test-result.txt"), "PASS " + assertions + " assertions\n");
    }

    private static void entryTruthTable() {
        // The refusal case that must survive the recovery: a NON-reviewed
        // builder who already holds WorldEdit outside Build Mode is refused.
        check(BuildPolicy.refusesOutsideWorldEdit(true, false, true),
            "non-reviewed + standing WorldEdit + temporary WorldEdit on: entry refused");
        // The recovered exemption: a reviewed holder is never refused,
        // whatever WorldEdit they hold.
        check(!BuildPolicy.refusesOutsideWorldEdit(true, true, true),
            "reviewed + standing WorldEdit: entry NOT refused (the recovered exemption)");
        check(!BuildPolicy.refusesOutsideWorldEdit(true, true, false),
            "reviewed + no standing WorldEdit: entry not refused");
        // Everyone else: no standing WorldEdit, no refusal — reviewed or not.
        check(!BuildPolicy.refusesOutsideWorldEdit(true, false, false),
            "non-reviewed + no standing WorldEdit: entry not refused");
        // Temporary WorldEdit off: the refusal never applies at all.
        check(!BuildPolicy.refusesOutsideWorldEdit(false, false, true),
            "temporary WorldEdit off: entry never refused for standing WorldEdit");
        check(!BuildPolicy.refusesOutsideWorldEdit(false, true, true),
            "temporary WorldEdit off + reviewed: entry not refused");
        check(!BuildPolicy.refusesOutsideWorldEdit(false, false, false),
            "temporary WorldEdit off + nothing held: entry not refused");
        check(!BuildPolicy.refusesOutsideWorldEdit(false, true, false),
            "temporary WorldEdit off + reviewed + nothing held: entry not refused");
    }

    private static void exitTruthTable() {
        // The probe that must survive: a NON-reviewed builder's residual
        // WorldEdit after attachment removal is still probed (and fails
        // the exit in the paper class).
        check(BuildPolicy.probesResidualWorldEdit(true, false),
            "non-reviewed + temporary WorldEdit on: exit residual probe RUNS");
        // The recovered exemption: a reviewed builder's standing WorldEdit
        // is expected to remain, so the probe is skipped.
        check(!BuildPolicy.probesResidualWorldEdit(true, true),
            "reviewed + temporary WorldEdit on: exit residual probe SKIPPED (the recovered exemption)");
        check(!BuildPolicy.probesResidualWorldEdit(false, false),
            "temporary WorldEdit off: exit probe does not run");
        check(!BuildPolicy.probesResidualWorldEdit(false, true),
            "temporary WorldEdit off + reviewed: exit probe does not run");
    }

    private static void pluginYmlPins() throws Exception {
        check(BuildPolicy.REVIEWED_PERMISSION.equals("excelbuildmode.reviewed"),
            "reviewed node constant is excelbuildmode.reviewed (the preserved jar's value)");
        check(!BuildPolicy.REVIEWED_PERMISSION.equals(AdminPolicy.PERMISSION),
            "reviewed node is distinct from the admin node");
        check(!BuildPolicy.REVIEWED_PERMISSION.equals(BigPastePolicy.PERMISSION),
            "reviewed node is distinct from the bigpaste node");
        String yml;
        try (InputStream in = ReviewedTests.class.getResourceAsStream("/plugin.yml")) {
            check(in != null, "plugin.yml is on the test classpath (build copies resources)");
            yml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        check(yml.contains("version: '1.3.4'"), "plugin.yml declares version 1.3.4");
        check(yml.contains("excelbuildmode.reviewed:"), "plugin.yml registers the reviewed node");
        int reviewedAt = yml.indexOf("excelbuildmode.reviewed:");
        String reviewedBlock = yml.substring(reviewedAt, Math.min(yml.length(), reviewedAt + 400));
        check(reviewedBlock.contains("default: false"),
            "excelbuildmode.reviewed defaults to false (the preserved jar's declaration)");
        check(reviewedBlock.contains("exit residual-permission probe"),
            "the reviewed node's description names the exit-probe skip (preserved wording)");
        // BOTH 1.3.3 deltas ride in the one 1.3.4 jar.
        check(yml.contains("excelbuildmode.admin:"), "plugin.yml still registers the admin node (combined build)");
        int adminAt = yml.indexOf("excelbuildmode.admin:");
        check(yml.substring(adminAt, Math.min(yml.length(), adminAt + 400)).contains("default: op"),
            "excelbuildmode.admin still defaults to op (combined build)");
    }

    private static void probeSetPins() {
        // The reviewed node is ours, not a WorldEdit node: holding it
        // permanently must trip neither probe itself.
        check(!BuildPolicy.WORLD_EDIT_PERMISSIONS.contains(BuildPolicy.REVIEWED_PERMISSION),
            "the reviewed node is not in the WorldEdit probe set");
        check(!BuildPolicy.sessionPermissions(true).containsKey(BuildPolicy.REVIEWED_PERMISSION)
            && !BuildPolicy.sessionPermissions(false).containsKey(BuildPolicy.REVIEWED_PERMISSION),
            "the reviewed node is never granted by a session attachment");
        // The session grant shape is unchanged by the recovery: reviewed
        // builders get the same session WorldEdit grant as everyone else.
        check(BuildPolicy.sessionPermissions(true).containsKey(BuildPolicy.WORLD_EDIT_GRANT),
            "the session WorldEdit grant is unchanged");
    }

    private static void check(boolean condition, String label) {
        assertions++;
        if (!condition) throw new AssertionError("FAILED: " + label);
    }
}
