/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * AtomicStateFile durability contract (0.5.6 fsync rebuild;
 * P-FSYNC-1, Nov 14, 2026). The mirror diagnosis
 * (~/workspace/mirror-storage-diagnosis-20261114/DIAGNOSIS.md)
 * proved the PC test rig degrades BossTrial because Windows
 * NIO refuses FileChannel.open on a directory with
 * AccessDeniedException, so every state save failed its
 * post-move directory force and the host locked writes.
 * The fix: the directory force is best-effort ONLY for that
 * platform refusal; the mandatory temp-file fsync carries
 * the write. Every other failure still fails the write.
 * The Windows refusal cannot occur naturally on this Linux
 * build host, so it is injected through the package-private
 * DirectoryForce seam of the production write path.
 */
public final class AtomicStateFileTest {

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

    private static boolean noTempLeft(Path dir) throws IOException {
        try (var entries = Files.list(dir)) {
            return entries.noneMatch(p -> p.getFileName().toString().startsWith(".recovery-state-"));
        }
    }

    public static void main(String[] args) throws IOException {
        Path dir = Files.createTempDirectory("atomic-state-file-test");
        try {
            // 1. Ordinary write through the public API: the real
            // directory force runs on this platform and succeeds.
            Path target = dir.resolve("state.yml");
            List<String> phases = new ArrayList<>();
            AtomicStateFile.write(target, "first", phases::add);
            check(Files.readString(target).equals("first"), "public write lands the content");
            check(phases.equals(List.of("before-write", "before-move", "after-move", "after-force")),
                    "public write walks all four probe phases in order");
            check(noTempLeft(dir), "public write leaves no temp file");

            // 2. The Windows failure mode, injected: the directory
            // force throws AccessDeniedException (exactly what
            // Windows NIO throws opening a directory channel).
            boolean[] forceCalled = {false};
            AtomicStateFile.DirectoryForce windowsRefusal = d -> {
                forceCalled[0] = true;
                throw new AccessDeniedException(d.toString());
            };
            List<String> phases2 = new ArrayList<>();
            AtomicStateFile.write(target, "second", phases2::add, windowsRefusal);
            check(forceCalled[0], "directory force is actually attempted before it is excused");
            check(Files.readString(target).equals("second"),
                    "write succeeds and lands the content when the platform refuses the directory force");
            check(phases2.equals(List.of("before-write", "before-move", "after-move", "after-force")),
                    "excused directory force still reaches the after-force phase");
            check(noTempLeft(dir), "excused write leaves no temp file");

            // 2b. Repeated saves keep working under the refusal —
            // the mirror symptom was every save failing, not one.
            AtomicStateFile.write(target, "third", p -> {}, windowsRefusal);
            check(Files.readString(target).equals("third"),
                    "a later save also succeeds under the platform refusal");

            // 3. Control: any OTHER directory-force failure still
            // fails the write (Linux semantics unchanged — the
            // plugin's writes-locked self-protection keeps its
            // trigger for genuine durability failures).
            AtomicStateFile.DirectoryForce genuineFailure = d -> {
                throw new IOException("injected genuine directory-force failure");
            };
            boolean propagated = false;
            try {
                AtomicStateFile.write(target, "fourth", p -> {}, genuineFailure);
            } catch (IOException e) {
                propagated = e.getMessage() != null
                        && e.getMessage().contains("injected genuine directory-force failure");
            }
            check(propagated, "a non-platform directory-force failure still propagates as IOException");
            check(noTempLeft(dir), "failed write still cleans up its temp file");
        } finally {
            try (var entries = Files.list(dir)) {
                entries.forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
            }
            Files.deleteIfExists(dir);
        }

        System.out.println("AtomicStateFileTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
