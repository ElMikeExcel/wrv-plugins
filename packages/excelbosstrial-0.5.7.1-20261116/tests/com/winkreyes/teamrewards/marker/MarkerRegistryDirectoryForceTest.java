/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.teamrewards.marker;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * MarkerRegistry directory-force contract (0.5.6 fsync
 * follow-up rebuild; P-FSYNC-1, Nov 14, 2026). Same idiom
 * and same fix as AtomicStateFile: the post-move directory
 * force in mutate() is best-effort ONLY for the platform
 * refusal (Windows NIO throws AccessDeniedException opening
 * a directory channel); the mandatory temp-file fsync
 * carries the write. Any other directory-force failure
 * still marks persistence uncertain and fails the mutation.
 * The Windows refusal cannot occur naturally on this Linux
 * build host, so it is injected through the package-private
 * DirectoryForce seam of the production mutate path.
 */
public final class MarkerRegistryDirectoryForceTest {

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

    private static MarkerRegistry.Marker marker() {
        return new MarkerRegistry.Marker(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, 64, -3);
    }

    public static void main(String[] args) throws IOException {
        Path dir = Files.createTempDirectory("marker-registry-dirforce-test");
        try {
            // 1. Ordinary mutations through the public API: the
            // real directory force runs on this platform.
            Path file1 = dir.resolve("one/markers.bin");
            MarkerRegistry.Marker m0 = marker();
            try (MarkerRegistry registry = new MarkerRegistry(file1)) {
                registry.add(m0);
                check(m0.equals(registry.get(m0.id())), "public add lands the marker");
            }
            try (MarkerRegistry reopened = new MarkerRegistry(file1)) {
                check(reopened.all().size() == 1 && m0.equals(reopened.get(m0.id())),
                        "marker survives close and reopen with the real directory force");
            }

            // 2. The Windows failure mode, injected: the
            // directory force throws AccessDeniedException.
            Path file2 = dir.resolve("two/markers.bin");
            MarkerRegistry.Marker m1 = marker();
            MarkerRegistry.Marker m2 = marker();
            int[] forceCalls = {0};
            try (MarkerRegistry registry = new MarkerRegistry(file2)) {
                registry.directoryForce = d -> {
                    forceCalls[0]++;
                    throw new AccessDeniedException(d.toString());
                };
                registry.add(m1);
                check(forceCalls[0] > 0, "directory force is actually attempted before it is excused");
                registry.add(m2);
                check(registry.writable(), "registry stays writable under the platform refusal");
                registry.remove(m1.id());
                check(registry.writable(), "remove also succeeds under the platform refusal");
            }
            try (MarkerRegistry reopened = new MarkerRegistry(file2)) {
                check(reopened.all().size() == 1 && m2.equals(reopened.get(m2.id())),
                        "mutations under the refusal are durable on disk (add/add/remove leaves the second marker)");
            }

            // 3. Control: any OTHER directory-force failure
            // still fails the mutation and marks persistence
            // uncertain (Linux semantics unchanged).
            Path file3 = dir.resolve("three/markers.bin");
            try (MarkerRegistry registry = new MarkerRegistry(file3)) {
                registry.directoryForce = d -> {
                    throw new IOException("injected genuine directory-force failure");
                };
                boolean uncertain = false;
                try {
                    registry.add(marker());
                } catch (IOException e) {
                    uncertain = "marker persistence uncertain".equals(e.getMessage())
                            && e.getCause() != null
                            && e.getCause().getMessage() != null
                            && e.getCause().getMessage().contains("injected genuine directory-force failure");
                }
                check(uncertain, "a non-platform directory-force failure still fails the mutation as persistence uncertain");
                check(!registry.writable(), "registry is no longer writable after a genuine directory-force failure");
            }
        } finally {
            try (var walk = Files.walk(dir)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
            }
        }

        System.out.println("MarkerRegistryDirectoryForceTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
