// Excel signature - WRV maintained source; original authorship and license notices preserved.
// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import com.winkreyes.shops.checkout.DataFileLock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * One-time, whole-directory WRVShops -> ExcelShops migration. Invoke before creating
 * defaults, a registry, or a checkout journal; retain the returned session until shutdown.
 * No legacy bytes or paths are modified. Migration failures retain evidence and block boot.
 * Receipts bind absolute source and target paths: relocating the server requires offline
 * provenance review rather than silently adopting a receipt from another location.
 *
 * The legacy journal lock excludes cooperating lock holders only. WRVShops 0.1.0 has a
 * POSIX descriptor-close locking flaw, so independently verifying that its server is fully
 * stopped is a prerequisite; acquiring this lock does not establish that fact. Repeated
 * content/identity snapshots detect source changes, and dirty markers are preserved.
 * Unrelated software need not honor advisory locks. An atomic directory move and directory
 * fsync are mandatory; unsupported filesystems fail closed.
 */
public final class LegacyDataMigration {
    static final String MANIFEST = ".excelshops-legacy-manifest-v1";
    static final String COMPLETE = ".excelshops-legacy-complete-v1";
    static final String FORMAT = "ExcelShops legacy migration 1";
    private static final int MAX_MANIFEST_BYTES = 64 * 1024 * 1024;
    private static final int MAX_ENTRIES = 1_000_000;
    private static final LinkOption NOFOLLOW = LinkOption.NOFOLLOW_LINKS;

    public enum Outcome { FRESH, EXISTING_TARGET, MIGRATED, ALREADY_MIGRATED }

    /** Owns both the Excel migration lock and, when present, the untouched legacy log lock. */
    public static final class Session implements AutoCloseable {
        private final DataFileLock processLock;
        private DataFileLock legacyLock;
        private Outcome outcome;
        private boolean closed;

        private Session(DataFileLock processLock) { this.processLock = processLock; }
        public Outcome outcome() { return outcome; }

        @Override public synchronized void close() throws IOException {
            if (closed) return;
            closed = true;
            IOException failure = null;
            if (legacyLock != null) {
                try { legacyLock.close(); } catch (IOException ex) { failure = ex; }
            }
            try { processLock.close(); } catch (IOException ex) {
                if (failure == null) failure = ex; else failure.addSuppressed(ex);
            }
            if (failure != null) throw failure;
        }
    }

    /* Deterministic failure/race injection, package-private and unused in production. */
    @FunctionalInterface interface CopyHook {
        void afterCopy(Path source, Path staged) throws IOException;
    }

    private LegacyDataMigration() { }

    public static Session open(Path legacyData, Path excelData, boolean legacyPluginLoaded)
            throws IOException {
        return open(legacyData, excelData, legacyPluginLoaded, (source, staged) -> { });
    }

    static Session open(Path legacyData, Path excelData, boolean legacyPluginLoaded, CopyHook hook)
            throws IOException {
        if (legacyPluginLoaded) {
            throw new IOException("WRVShops is loaded. Remove the old plugin and fully stop its server before starting ExcelShops.");
        }
        Path source = Objects.requireNonNull(legacyData).toAbsolutePath().normalize();
        Path target = Objects.requireNonNull(excelData).toAbsolutePath().normalize();
        Path parent = target.getParent();
        if (parent == null || !parent.equals(source.getParent()) || source.equals(target)) {
            throw new IOException("Legacy and ExcelShops folders must be distinct siblings.");
        }
        rejectSymlinkAncestors(parent);
        requireDirectory(parent);
        rejectSymlinkAncestors(source);
        rejectSymlinkAncestors(target);
        Path lockPath = parent.resolve("." + target.getFileName() + ".migration.lock");
        Session session = new Session(DataFileLock.open(lockPath, true));
        try {
            Path stage = parent.resolve("." + target.getFileName() + ".migration-staging");
            Path intent = parent.resolve("." + target.getFileName() + ".migration-in-progress");
            if (exists(stage) || exists(intent)) {
                throw new IOException("Incomplete ExcelShops migration evidence exists. Preserve both folders and reconcile offline: " + parent);
            }
            if (exists(source)) requireDirectory(source);
            if (exists(target)) requireDirectory(target);
            Path legacyLog = source.resolve("checkout/transactions.log");
            rejectSymlinkAncestors(legacyLog);
            if (exists(legacyLog)) session.legacyLock = DataFileLock.open(legacyLog, false);
            Map<String, Entry> original = exists(source) ? snapshot(source, session.legacyLock, false) : null;
            requireJournalEvidence(original, "Legacy WRVShops");
            Map<String, Entry> existing = exists(target) ? snapshot(target, null, false) : null;
            requireLegacyJournalEvidence(existing, "ExcelShops");
            boolean manifest = exists(target.resolve(MANIFEST));
            boolean complete = exists(target.resolve(COMPLETE));
            if (manifest || complete) {
                if (!manifest || !complete) throw new IOException("ExcelShops migration receipt is incomplete.");
                Map<String, Entry> recorded = readReceipt(source, target);
                requireMigratedCoreFiles(target, recorded);
                if (original != null && !sameContents(original, recorded)) {
                    throw new IOException("Legacy WRVShops data changed after migration. Refusing to overwrite or combine either data folder.");
                }
                session.outcome = Outcome.ALREADY_MIGRATED;
                return session;
            }
            if (original == null || original.size() == 1) {
                session.outcome = existing == null || existing.size() == 1 ? Outcome.FRESH : Outcome.EXISTING_TARGET;
                return session;
            }
            if (original.containsKey(MANIFEST) || original.containsKey(COMPLETE)) {
                throw new IOException("Legacy data contains reserved ExcelShops migration receipt names.");
            }
            if (existing != null && existing.size() > 1) {
                throw new IOException("Both WRVShops and ExcelShops contain data without valid migration provenance. Preserve both and reconcile offline.");
            }
            migrate(source, target, stage, intent, original, existing, session.legacyLock, hook);
            session.outcome = Outcome.MIGRATED;
            return session;
        } catch (IOException | RuntimeException | Error failure) {
            try { session.close(); } catch (IOException closeFailure) { failure.addSuppressed(closeFailure); }
            throw failure;
        }
    }

    private static void migrate(Path source, Path target, Path stage, Path intent,
            Map<String, Entry> original, Map<String, Entry> existing, DataFileLock legacyLock,
            CopyHook hook) throws IOException {
        Path parent = target.getParent();
        writeDurable(intent, (FORMAT + "\n" + source + "\n" + target + "\n").getBytes(StandardCharsets.UTF_8));
        syncDirectory(parent);
        createPrivateDirectory(stage);
        syncDirectory(parent);
        for (Map.Entry<String, Entry> item : original.entrySet()) {
            if (item.getKey().isEmpty()) continue;
            Path from = source.resolve(item.getKey());
            Path to = stage.resolve(item.getKey());
            if (item.getValue().directory()) createPrivateDirectory(to);
            else {
                copyDurable(from, to, legacyLock, item.getValue().permissions());
                hook.afterCopy(from, to);
            }
        }
        if (!sameContents(original, snapshot(stage, null, false))) {
            throw new IOException("Copied legacy data failed checksum verification. Staging evidence was preserved.");
        }
        requireUnchanged(source, original, legacyLock);
        byte[] manifest = manifestBytes(source, target, original);
        writeDurable(stage.resolve(MANIFEST), manifest);
        writeDurable(stage.resolve(COMPLETE), (FORMAT + "\n" + hash(manifest) + "\n").getBytes(StandardCharsets.UTF_8));
        List<Path> directories = new ArrayList<>();
        for (Map.Entry<String, Entry> item : original.entrySet()) {
            if (item.getValue().directory()) directories.add(stage.resolve(item.getKey()));
        }
        directories.sort(Comparator.comparingInt(Path::getNameCount).reversed());
        for (Path directory : directories) {
            String relative = stage.relativize(directory).toString();
            applyPermissions(directory, original.get(relative).permissions());
            syncDirectory(directory);
        }
        rejectSymlinkAncestors(source);
        rejectSymlinkAncestors(target);
        requireUnchanged(source, original, legacyLock);
        if (existing == null ? exists(target) : !existing.equals(snapshot(target, null, false))) {
            throw new IOException("ExcelShops target changed during migration. Staging evidence was preserved.");
        }
        // There is deliberately no non-atomic fallback, merge, deletion, or overwrite of data.
        Files.move(stage, target, StandardCopyOption.ATOMIC_MOVE);
        syncDirectory(parent);
        requireUnchanged(source, original, legacyLock);
        if (!sameContents(original, snapshot(target, null, true))) {
            throw new IOException("Published ExcelShops data failed checksum verification. Migration remains blocked.");
        }
        readReceipt(source, target);
        Files.delete(intent);
        try { syncDirectory(parent); }
        catch (IOException failure) {
            // A failed final durability barrier must not look like a clean migration on retry.
            try { writeDurable(intent, (FORMAT + "\nFinal directory sync failed.\n").getBytes(StandardCharsets.UTF_8)); }
            catch (IOException restoreFailure) { failure.addSuppressed(restoreFailure); }
            throw failure;
        }
    }

    private record Entry(boolean directory, long size, String digest, String modified, String key, String permissions) { }

    private static Map<String, Entry> snapshot(Path root, DataFileLock legacyLock, boolean omitReceipt)
            throws IOException {
        rejectSymlinkAncestors(root);
        Map<String, Entry> result = new TreeMap<>();
        scan(root, root, result, legacyLock, omitReceipt, new HashSet<>());
        return result;
    }

    private static void scan(Path root, Path path, Map<String, Entry> result, DataFileLock legacyLock,
            boolean omitReceipt, Set<Object> fileKeys) throws IOException {
        String relative = root.relativize(path).toString();
        if (omitReceipt && (relative.equals(MANIFEST) || relative.equals(COMPLETE))) return;
        BasicFileAttributes before = attributes(path);
        if (!before.isDirectory() && !before.isRegularFile()) {
            throw new IOException("Migration refuses symlinks and special files: " + path);
        }
        if (before.isRegularFile() && before.fileKey() != null && !fileKeys.add(before.fileKey())) {
            throw new IOException("Migration refuses duplicate file identities/hard links: " + path);
        }
        String permissions = readPermissions(path);
        String digest = before.isDirectory() ? "" : hashFile(path, legacyLock);
        BasicFileAttributes after = attributes(path);
        if (!sameIdentity(before, after) || !permissions.equals(readPermissions(path))) {
            throw new IOException("File changed while reading: " + path);
        }
        result.put(relative, new Entry(before.isDirectory(), before.isDirectory() ? 0 : before.size(),
                digest, before.lastModifiedTime().toString(), String.valueOf(before.fileKey()), permissions));
        if (result.size() > MAX_ENTRIES) throw new IOException("Too many migration entries; manual review required.");
        if (before.isDirectory()) {
            try (DirectoryStream<Path> children = Files.newDirectoryStream(path)) {
                for (Path child : children) scan(root, child, result, legacyLock, omitReceipt, fileKeys);
            }
            if (!sameIdentity(before, attributes(path))) {
                throw new IOException("Directory changed while reading: " + path);
            }
        }
    }

    private static void requireUnchanged(Path source, Map<String, Entry> original, DataFileLock lock)
            throws IOException {
        if (!original.equals(snapshot(source, lock, false))) {
            throw new IOException("Legacy WRVShops data changed during migration. Stop all writers and reconcile the preserved evidence offline.");
        }
    }

    private static boolean sameContents(Map<String, Entry> first, Map<String, Entry> second) {
        if (!first.keySet().equals(second.keySet())) return false;
        for (String key : first.keySet()) {
            Entry a = first.get(key), b = second.get(key);
            if (a.directory() != b.directory() || a.size() != b.size() || !a.digest().equals(b.digest())) return false;
        }
        return true;
    }

    private static void requireJournalEvidence(Map<String, Entry> entries, String folder) throws IOException {
        if (entries == null) return;
        requireLegacyJournalEvidence(entries, folder);
        String journal = Path.of("checkout", "transactions.log").toString();
        String activeSession = Path.of("checkout", "active-session").toString();
        // Saving a registry or creating a session follows journal initialization. Their
        // presence without its log cannot establish that no pending debit needs recovery.
        // Config/catalog-only and an empty checkout directory are valid earlier starts.
        if ((entries.containsKey("shops.dat") || entries.containsKey(activeSession))
                && (!entries.containsKey(journal) || entries.get(journal).directory())) {
            throw new IOException(folder + " has registry/session evidence but its checkout/transactions.log is missing or not a regular file. "
                    + "Preserve all data and reconcile offline; a fresh journal will not be created.");
        }
    }

    private static void requireLegacyJournalEvidence(Map<String, Entry> entries, String folder) throws IOException {
        if (entries == null) return;
        String journal = Path.of("legacy-checkout", "transactions.log").toString();
        String activeSession = Path.of("legacy-checkout", "active-session").toString();
        if (entries.containsKey(activeSession)
                && (!entries.containsKey(journal) || entries.get(journal).directory())) {
            throw new IOException(folder + " has legacy checkout session evidence but its legacy-checkout/transactions.log is missing or not a regular file. "
                    + "Preserve all data and reconcile offline; a fresh journal will not be created.");
        }
    }

    private static void requireMigratedCoreFiles(Path target, Map<String, Entry> recorded) throws IOException {
        // Legitimate operation changes these bytes, but never removes these files. In
        // particular, recreating a missing old journal could discard pending recovery.
        for (String name : List.of("config.yml", "spawn-catalog.yml", "shops.dat", "checkout/transactions.log", "legacy-checkout/transactions.log")) {
            if (recorded.containsKey(name) && !recorded.get(name).directory()
                    && (!exists(target.resolve(name)) || !attributes(target.resolve(name)).isRegularFile())) {
                throw new IOException("A migrated core data file is missing or replaced: " + name
                        + ". Preserve both folders and reconcile offline; defaults will not be created.");
            }
        }
    }

    private static byte[] manifestBytes(Path source, Path target, Map<String, Entry> entries)
            throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(FORMAT);
            out.writeUTF(source.toString());
            out.writeUTF(target.toString());
            out.writeInt(entries.size());
            for (Map.Entry<String, Entry> item : entries.entrySet()) {
                out.writeUTF(item.getKey());
                out.writeBoolean(item.getValue().directory());
                out.writeLong(item.getValue().size());
                out.writeUTF(item.getValue().digest());
            }
        }
        if (bytes.size() > MAX_MANIFEST_BYTES) throw new IOException("Migration manifest is too large.");
        return bytes.toByteArray();
    }

    private static Map<String, Entry> readReceipt(Path source, Path target) throws IOException {
        Path manifest = target.resolve(MANIFEST), complete = target.resolve(COMPLETE);
        if (!attributes(manifest).isRegularFile() || !attributes(complete).isRegularFile()
                || Files.size(manifest) > MAX_MANIFEST_BYTES || Files.size(complete) > 256) {
            throw new IOException("Invalid ExcelShops migration receipt.");
        }
        byte[] bytes = Files.readAllBytes(manifest);
        String marker = Files.readString(complete, StandardCharsets.UTF_8);
        if (!marker.equals(FORMAT + "\n" + hash(bytes) + "\n")) {
            throw new IOException("ExcelShops migration receipt checksum mismatch.");
        }
        Map<String, Entry> entries = new TreeMap<>();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (!FORMAT.equals(in.readUTF()) || !source.toString().equals(in.readUTF())
                    || !target.toString().equals(in.readUTF())) throw new IOException("Migration provenance does not match these folders.");
            int count = in.readInt();
            if (count < 1 || count > MAX_ENTRIES) throw new IOException("Invalid migration manifest count.");
            for (int i = 0; i < count; i++) {
                String name = in.readUTF();
                Path relative = Path.of(name);
                if (relative.isAbsolute() || !relative.normalize().equals(relative) || name.equals(".")
                        || relative.startsWith("..") || name.equals(MANIFEST) || name.equals(COMPLETE)) {
                    throw new IOException("Invalid migration manifest path.");
                }
                boolean directory = in.readBoolean();
                long size = in.readLong();
                String digest = in.readUTF();
                if (size < 0 || (directory ? size != 0 || !digest.isEmpty() : !digest.matches("[a-f0-9]{64}"))) {
                    throw new IOException("Invalid migration manifest entry.");
                }
                if (entries.put(name, new Entry(directory, size, digest, "", "", "")) != null) {
                    throw new IOException("Duplicate migration manifest path.");
                }
            }
            if (in.read() != -1 || !entries.containsKey("") || !entries.get("").directory()) {
                throw new IOException("Incomplete migration manifest.");
            }
        } catch (IllegalArgumentException failure) {
            throw new IOException("Invalid migration manifest.", failure);
        }
        return entries;
    }

    private static void copyDurable(Path source, Path target, DataFileLock legacyLock, String permissions) throws IOException {
        if (legacyLock != null && source.equals(legacyLock.path())) {
            legacyLock.verifyIdentity();
            copyChannel(legacyLock.channel(), target, permissions);
            legacyLock.verifyIdentity();
        } else {
            try (DataFileLock.ReadAccess in = DataFileLock.openReadOnly(source)) {
                copyChannel(in.channel(), target, permissions);
            }
        }
    }

    private static void copyChannel(FileChannel source, Path target, String permissions) throws IOException {
        try (FileChannel out = FileChannel.open(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, NOFOLLOW)) {
            ByteBuffer buffer = ByteBuffer.allocate(64 * 1024);
            long offset = 0;
            int read;
            while ((read = source.read(buffer, offset)) != -1) {
                if (read == 0) continue;
                offset += read;
                buffer.flip();
                while (buffer.hasRemaining()) out.write(buffer);
                buffer.clear();
            }
            applyPermissions(target, permissions);
            out.force(true);
        }
    }

    private static String hashFile(Path path, DataFileLock legacyLock) throws IOException {
        if (legacyLock != null && path.equals(legacyLock.path())) {
            legacyLock.verifyIdentity();
            String result = hashChannel(legacyLock.channel());
            legacyLock.verifyIdentity();
            return result;
        }
        try (DataFileLock.ReadAccess access = DataFileLock.openReadOnly(path)) {
            return hashChannel(access.channel());
        }
    }

    private static String hashChannel(FileChannel channel) throws IOException {
        MessageDigest digest = sha256();
        ByteBuffer buffer = ByteBuffer.allocate(64 * 1024);
        long offset = 0;
        int read;
        while ((read = channel.read(buffer, offset)) != -1) {
            if (read == 0) continue;
            offset += read;
            buffer.flip();
            digest.update(buffer);
            buffer.clear();
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String hash(byte[] bytes) { return HexFormat.of().formatHex(sha256().digest(bytes)); }
    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private static void writeDurable(Path path, byte[] bytes) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, NOFOLLOW)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
    }

    private static void createPrivateDirectory(Path directory) throws IOException {
        if (Files.getFileAttributeView(directory.getParent(), PosixFileAttributeView.class, NOFOLLOW) != null) {
            Files.createDirectory(directory, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        } else Files.createDirectory(directory);
    }

    private static String readPermissions(Path path) throws IOException {
        return Files.getFileAttributeView(path, PosixFileAttributeView.class, NOFOLLOW) == null ? ""
                : PosixFilePermissions.toString(Files.getPosixFilePermissions(path, NOFOLLOW));
    }

    private static void applyPermissions(Path path, String permissions) throws IOException {
        if (!permissions.isEmpty()) Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(permissions));
    }

    private static void syncDirectory(Path directory) throws IOException {
        try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ, NOFOLLOW)) { channel.force(true); }
    }

    private static BasicFileAttributes attributes(Path path) throws IOException {
        return DataFileLock.checkedAttributes(path);
    }
    private static boolean sameIdentity(BasicFileAttributes a, BasicFileAttributes b) {
        return a.isDirectory() == b.isDirectory() && a.isRegularFile() == b.isRegularFile()
                && a.isSymbolicLink() == b.isSymbolicLink() && a.size() == b.size()
                && a.lastModifiedTime().equals(b.lastModifiedTime()) && Objects.equals(a.fileKey(), b.fileKey());
    }
    private static boolean exists(Path path) throws IOException {
        try { attributes(path); return true; }
        catch (java.nio.file.NoSuchFileException absent) { return false; }
    }
    private static void requireDirectory(Path path) throws IOException {
        if (!attributes(path).isDirectory()) throw new IOException("Expected a real directory: " + path);
    }
    private static void rejectSymlinkAncestors(Path path) throws IOException {
        Path cursor = path.getRoot();
        for (Path component : path) {
            cursor = cursor.resolve(component);
            if (Files.isSymbolicLink(cursor)) throw new IOException("Migration refuses symlink paths: " + cursor);
        }
    }
}
