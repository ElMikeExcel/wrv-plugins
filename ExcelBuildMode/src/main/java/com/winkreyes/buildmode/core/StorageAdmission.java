package com.winkreyes.buildmode.core;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/** Entry-only storage budget. Never prunes files and never blocks an existing recovery. */
public final class StorageAdmission {
    public static final class EntryRefused extends IOException { public EntryRefused(String message) { super(message); } }
    public static final long DEFAULT_BYTES = 128L * 1024 * 1024;
    public static final long DEFAULT_RESERVE = 512L * 1024 * 1024;
    public static final long DEFAULT_SESSIONS = 2048;
    private static final long WRITE_MARGIN = 64L * 1024;
    private static final long MAX_SCAN_ENTRIES = 100_000;
    @FunctionalInterface public interface FreeSpace { long available(Path root) throws IOException; }
    public record Usage(long bytes, long archives) { }
    private final long maxBytes, reserve, maxArchives;
    private final FreeSpace free;
    public StorageAdmission(long maxBytes, long reserve, long maxArchives) {
        this(maxBytes, reserve, maxArchives, root -> Files.getFileStore(root).getUsableSpace());
    }
    public StorageAdmission(long maxBytes, long reserve, long maxArchives, FreeSpace free) {
        if (maxBytes < 1 || reserve < 0 || maxArchives < 1) throw new IllegalArgumentException("Invalid storage budget");
        this.maxBytes = maxBytes; this.reserve = reserve; this.maxArchives = maxArchives;
        this.free = Objects.requireNonNull(free);
    }
    public Usage inspect(Path root) throws IOException {
        long bytes = 0, archives = 0, entries = 0;
        Deque<Path> queue = new ArrayDeque<>(); queue.add(root);
        while (!queue.isEmpty()) {
            Path dir = queue.removeFirst();
            BasicFileAttributes da = Files.readAttributes(dir, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!da.isDirectory() || da.isSymbolicLink()) throw new IOException("Unsafe journal directory: " + dir);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path p : stream) {
                    if (++entries > MAX_SCAN_ENTRIES) throw new IOException("Journal scan budget exceeded; administrator review required");
                    BasicFileAttributes a = Files.readAttributes(p, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                    if (a.isSymbolicLink()) throw new IOException("Journal symlink is not supported: " + p);
                    if (a.isDirectory()) {
                        queue.addLast(p);
                        if (root.relativize(p).getNameCount() == 2) archives++;
                    } else if (a.isRegularFile()) {
                        try { bytes = Math.addExact(bytes, a.size()); }
                        catch (ArithmeticException e) { throw new IOException("Journal size overflow", e); }
                    } else throw new IOException("Unsupported journal file: " + p);
                }
            }
        }
        return new Usage(bytes, archives);
    }
    public void beforeEntry(Path root, int snapshotBytes) throws IOException {
        Usage used = inspect(root);
        try {
            // Two envelopes plus state/index/metadata headroom. Logical-byte budget is not a host-quota API.
            long addition = Math.addExact(Math.multiplyExact(2L, Math.addExact(snapshotBytes, 40L)), WRITE_MARGIN);
            if (used.archives >= maxArchives || Math.addExact(used.bytes, addition) > maxBytes)
                throw new EntryRefused("Saved Build Mode recovery data reached its entry budget. Existing recovery is retained; ask an administrator to archive it safely.");
            long available = free.available(root);
            if (available < 0 || available < Math.addExact(reserve, addition))
                throw new EntryRefused("Not enough verified free space to start Build Mode. Existing recovery remains available.");
        } catch (ArithmeticException e) { throw new IOException("Storage budget overflow", e); }
    }
}
