// Excel signature - WRV maintained source; original authorship and license notices preserved.
// SPDX-License-Identifier: MIT
package com.winkreyes.shops.checkout;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Shared journal/migration locking. Reserve before opening any descriptor: on POSIX,
 * closing even an unsuccessful duplicate descriptor can release this JVM's existing lock.
 * Callers must read through channel(), never open another descriptor for the locked file.
 * Symbolic and hard links are refused so alternate names cannot silently drop that lock.
 */
public final class DataFileLock implements AutoCloseable {
    private static final Set<Path> OPEN_PATHS = new HashSet<>();
    private static final LinkOption NOFOLLOW = LinkOption.NOFOLLOW_LINKS;
    private final Path path;
    private final FileChannel channel;
    private final FileLock lock;
    private final BasicFileAttributes identity;
    private boolean closed;

    private DataFileLock(Path path, FileChannel channel, FileLock lock, BasicFileAttributes identity) {
        this.path = path; this.channel = channel; this.lock = lock; this.identity = identity;
    }

    public static synchronized DataFileLock open(Path requested, boolean create) throws IOException {
        Path path = requested.toAbsolutePath().normalize();
        rejectSymlinkAncestors(path);
        if (!OPEN_PATHS.add(path)) throw new IOException("Shop data is already open: " + path);
        FileChannel channel = null;
        FileLock lock = null;
        try {
            BasicFileAttributes before;
            try { before = checkedAttributes(path); }
            catch (NoSuchFileException absent) { before = null; }
            if (before != null && !before.isRegularFile()) throw new IOException("Lock is not a regular file: " + path);
            channel = create
                    ? FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE, NOFOLLOW)
                    : FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE, NOFOLLOW);
            try { lock = channel.tryLock(); }
            catch (OverlappingFileLockException failure) { throw new IOException("Shop data is already open: " + path, failure); }
            if (lock == null) throw new IOException("Another process has shop data open: " + path);
            BasicFileAttributes after = checkedAttributes(path);
            if (before != null && !sameIdentity(before, after)) throw new IOException("Lock file changed while opening: " + path);
            return new DataFileLock(path, channel, lock, after);
        } catch (IOException | RuntimeException | Error failure) {
            if (lock != null) try { lock.release(); } catch (IOException secondary) { failure.addSuppressed(secondary); }
            if (channel != null) try { channel.close(); } catch (IOException secondary) { failure.addSuppressed(secondary); }
            OPEN_PATHS.remove(path);
            throw failure;
        }
    }

    /** A non-locking reader still reserves its descriptor against same-JVM lock owners. */
    public static synchronized ReadAccess openReadOnly(Path requested) throws IOException {
        Path path = requested.toAbsolutePath().normalize();
        rejectSymlinkAncestors(path);
        if (!OPEN_PATHS.add(path)) throw new IOException("Shop data is already open: " + path);
        try {
            if (!checkedAttributes(path).isRegularFile()) throw new IOException("Not a regular data file: " + path);
            return new ReadAccess(path, FileChannel.open(path, StandardOpenOption.READ, NOFOLLOW));
        } catch (IOException | RuntimeException | Error failure) {
            OPEN_PATHS.remove(path);
            throw failure;
        }
    }

    public static final class ReadAccess implements AutoCloseable {
        private final Path path;
        private final FileChannel channel;
        private boolean closed;
        private ReadAccess(Path path, FileChannel channel) { this.path = path; this.channel = channel; }
        public FileChannel channel() { return channel; }
        @Override public void close() throws IOException {
            synchronized (DataFileLock.class) {
                if (closed) return;
                closed = true;
                try { channel.close(); } finally { OPEN_PATHS.remove(path); }
            }
        }
    }

    public Path path() { return path; }
    public FileChannel channel() { return channel; }

    /** Migration expects its source to remain byte- and metadata-stable. */
    public void verifyIdentity() throws IOException {
        if (!lock.isValid() || !sameIdentity(identity, checkedAttributes(path))) {
            throw new IOException("Locked source changed: " + path);
        }
    }

    public static BasicFileAttributes checkedAttributes(Path path) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class, NOFOLLOW);
        if (attrs.isSymbolicLink()) throw new IOException("Shop data refuses symbolic links: " + path);
        if (attrs.isRegularFile() && path.getFileSystem().supportedFileAttributeViews().contains("unix")) {
            Object count = Files.getAttribute(path, "unix:nlink", NOFOLLOW);
            if (!(count instanceof Number links) || links.longValue() != 1) {
                throw new IOException("Shop data refuses hard-linked files: " + path);
            }
        }
        return attrs;
    }

    private static boolean sameIdentity(BasicFileAttributes a, BasicFileAttributes b) {
        return a.isRegularFile() == b.isRegularFile() && a.size() == b.size()
                && a.lastModifiedTime().equals(b.lastModifiedTime()) && Objects.equals(a.fileKey(), b.fileKey());
    }

    private static void rejectSymlinkAncestors(Path path) throws IOException {
        Path cursor = path.getRoot();
        for (Path component : path) {
            cursor = cursor.resolve(component);
            if (Files.isSymbolicLink(cursor)) throw new IOException("Shop data refuses symbolic links: " + cursor);
        }
    }

    @Override public void close() throws IOException {
        synchronized (DataFileLock.class) {
            if (closed) return;
            closed = true;
            try { try { lock.release(); } finally { channel.close(); } }
            finally { OPEN_PATHS.remove(path); }
        }
    }
}
