package com.winkreyes.exceleliteladder.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Function;

/**
 * Writes one ladder step into MobCombatSettings.yml:
 * dated .bak (never overwritten) -> temp file -> atomic move -> read back + verify ->
 * restore the .bak on any failure. Pure java.nio so the whole path is unit tested.
 */
public final class FileApplier {

    /** Test seam: lets the suite and the rig force a failure at a named point. */
    public interface Faults {
        default void beforeMove(Path temp) throws IOException {
        }

        default void afterMove(Path target) throws IOException {
        }
    }

    public static final Faults NONE = new Faults() {
    };

    public record Result(boolean ok, boolean changed, String error, Path backup, String shaBefore,
                         String shaAfter, boolean restored) {
    }

    private FileApplier() {
    }

    /**
     * @param yamlCheck returns null when the text parses as YAML with the same top-level key
     *                  set as the original, else a reason
     * @param stamp     dated stamp for the backup name, e.g. 20261025-141500
     */
    public static Result apply(Path target, Path backupDir, int applied, LadderStep to, String stamp,
                               Function<String[], String> yamlCheck, Faults faults) {
        byte[] original;
        try {
            original = Files.readAllBytes(target);
        } catch (IOException ex) {
            return new Result(false, false, "cannot read " + target.getFileName() + ": " + ex.getMessage(), null, null, null, false);
        }
        String shaBefore = sha256(original);
        String before = new String(original, StandardCharsets.UTF_8);
        Map<String, Double> live;
        try {
            live = MobCombatFile.read(before);
        } catch (LadderException ex) {
            return new Result(false, false, ex.getMessage(), null, shaBefore, shaBefore, false);
        }
        String drift = StepPlan.driftCheck(live, applied, to.step());
        if (drift != null) {
            return new Result(false, false, "REFUSED: " + drift, null, shaBefore, shaBefore, false);
        }
        String after = MobCombatFile.patch(before, to);
        if (after.equals(before)) {
            return new Result(true, false, null, null, shaBefore, shaBefore, false);
        }

        Path backup;
        try {
            Files.createDirectories(backupDir);
            backup = uniqueBackup(backupDir, stamp, applied);
            Files.write(backup, original, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            if (!Arrays.equals(Files.readAllBytes(backup), original)) {
                return new Result(false, false, "backup read-back differs; nothing changed", backup, shaBefore, shaBefore, false);
            }
        } catch (IOException ex) {
            return new Result(false, false, "cannot write backup: " + ex.getMessage() + "; nothing changed", null, shaBefore, shaBefore, false);
        }

        Path temp = target.resolveSibling(target.getFileName() + ".eliteladder-tmp");
        try {
            Files.write(temp, after.getBytes(StandardCharsets.UTF_8));
            faults.beforeMove(temp);
            move(temp, target);
            faults.afterMove(target);
            String written = new String(Files.readAllBytes(target), StandardCharsets.UTF_8);
            String bad = MobCombatFile.verify(before, written, to);
            if (bad == null) {
                bad = yamlCheck.apply(new String[] {before, written});
            }
            if (bad != null) {
                throw new IOException("verify failed: " + bad);
            }
            return new Result(true, true, null, backup, shaBefore, sha256(Files.readAllBytes(target)), false);
        } catch (IOException | RuntimeException ex) {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // best effort
            }
            boolean restored = restore(target, original);
            String now = shaOf(target);
            return new Result(false, false, ex.getMessage() + (restored ? "; original restored from " + backup.getFileName()
                    : "; RESTORE FAILED, copy " + backup.getFileName() + " back by hand"), backup, shaBefore, now, restored);
        }
    }

    /** Puts the original bytes back (temp + move), true when the file then equals them. */
    static boolean restore(Path target, byte[] original) {
        Path temp = target.resolveSibling(target.getFileName() + ".eliteladder-restore");
        try {
            Files.write(temp, original);
            move(temp, target);
            return Arrays.equals(Files.readAllBytes(target), original);
        } catch (IOException ex) {
            try {
                Files.write(target, original);
                return Arrays.equals(Files.readAllBytes(target), original);
            } catch (IOException ex2) {
                return false;
            }
        }
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static Path uniqueBackup(Path dir, String stamp, int applied) {
        String base = "MobCombatSettings.yml." + stamp + "-from-S" + applied;
        Path p = dir.resolve(base + ".bak");
        int n = 2;
        while (Files.exists(p)) {
            p = dir.resolve(base + "-" + n++ + ".bak");
        }
        return p;
    }

    public static String shaOf(Path p) {
        try {
            return sha256(Files.readAllBytes(p));
        } catch (IOException ex) {
            return "unreadable";
        }
    }

    public static String sha256(byte[] b) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
