package com.winkreyes.buildmode.core;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import static java.nio.file.StandardOpenOption.*;

/** Main-thread journal. No Bukkit dependencies. Files are private recovery data, not an admin API. */
public final class RecoveryJournal {
    public static final int MAX_BYTES = 16 * 1024 * 1024;
    private final Path root;
    private final Faults faults;
    private final StorageAdmission storage;
    public interface Faults { void at(String stage) throws IOException; }
    public record Session(UUID player, UUID id, byte[] snapshot) {
        public Session { snapshot = snapshot.clone(); }
        @Override public byte[] snapshot() { return snapshot.clone(); }
    }
    private record State(UUID session, boolean active) { }
    public RecoveryJournal(Path root) throws IOException { this(root, stage -> {}); }
    public RecoveryJournal(Path root, Faults faults) throws IOException {
        this(root, faults, null);
    }
    public RecoveryJournal(Path root, Faults faults, StorageAdmission storage) throws IOException {
        this.root = root; this.faults = faults; this.storage = storage;
        Files.createDirectories(root); forceDirectory(root);
    }
    public Set<UUID> players() throws IOException {
        Set<UUID> result = new HashSet<>();
        try (var stream = Files.list(root)) {
            for (Path p : stream.toList()) {
                if (!Files.isDirectory(p)) continue;
                try { result.add(UUID.fromString(p.getFileName().toString())); }
                catch (IllegalArgumentException ignored) { }
            }
        }
        return Set.copyOf(result);
    }
    public Session begin(UUID player, byte[] snapshot) throws IOException {
        Objects.requireNonNull(snapshot);
        if (snapshot.length == 0 || snapshot.length > MAX_BYTES) throw new IOException("Invalid snapshot size");
        if (active(player).isPresent()) throw new IOException("Recovery already pending");
        if (storage != null) storage.beforeEntry(root, snapshot.length);
        UUID id = UUID.randomUUID();
        Path dir = root.resolve(player.toString());
        Files.createDirectories(dir); forceDirectory(root);
        Path archive = dir.resolve(id.toString());
        Files.createDirectory(archive); forceDirectory(dir);
        byte[] checked = envelope(snapshot);
        writeImmutable(archive.resolve("survival.bin"), checked); faults.at("primary");
        writeImmutable(archive.resolve("survival.backup.bin"), checked); faults.at("backup");
        forceDirectory(archive);
        // Verify bytes before committing the record that authorizes destructive changes.
        if (!Arrays.equals(readChecked(archive.resolve("survival.bin")), snapshot)
                || !Arrays.equals(readChecked(archive.resolve("survival.backup.bin")), snapshot))
            throw new IOException("Snapshot verification mismatch");
        faults.at("verified");
        writeState(dir, new State(id, true));
        faults.at("committed");
        return new Session(player, id, snapshot);
    }
    public Optional<Session> active(UUID player) throws IOException {
        Path dir = root.resolve(player.toString());
        Optional<State> state = readState(dir);
        if (state.isEmpty() || !state.get().active()) return Optional.empty();
        UUID id = state.get().session();
        Path archive = dir.resolve(id.toString());
        byte[] snapshot;
        try { snapshot = readChecked(archive.resolve("survival.bin")); }
        catch (IOException primary) {
            try { snapshot = readChecked(archive.resolve("survival.backup.bin")); }
            catch (IOException backup) { primary.addSuppressed(backup); throw primary; }
        }
        return Optional.of(new Session(player, id, snapshot));
    }
    public void complete(Session expected) throws IOException {
        Path dir = root.resolve(expected.player().toString());
        State state = readState(dir).orElseThrow(() -> new IOException("Missing session record"));
        if (!state.active() || !state.session().equals(expected.id())) throw new IOException("Session changed");
        faults.at("before_complete");
        // Tombstone, not deletion. All session snapshots are retained unchanged for manual recovery.
        writeState(dir, new State(expected.id(), false));
        faults.at("completed");
    }
    private Optional<State> readState(Path dir) throws IOException {
        Path file = dir.resolve("state.bin");
        if (!Files.exists(file)) return Optional.empty();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(readChecked(file)))) {
            if (in.readInt() != 1) throw new IOException("Unknown state version");
            State result = new State(new UUID(in.readLong(), in.readLong()), in.readBoolean());
            if (in.read() != -1) throw new IOException("Trailing state bytes");
            return Optional.of(result);
        } catch (RuntimeException e) { throw new IOException("Invalid state", e); }
    }
    private void writeState(Path dir, State state) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(1); out.writeLong(state.session().getMostSignificantBits());
            out.writeLong(state.session().getLeastSignificantBits()); out.writeBoolean(state.active());
        }
        Path temp = dir.resolve("state-" + UUID.randomUUID() + ".tmp");
        writeImmutable(temp, envelope(bytes.toByteArray()));
        faults.at("state_temp");
        // No non-atomic fallback. An unsupported filesystem refuses entry/recovery completion.
        Files.move(temp, dir.resolve("state.bin"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        forceDirectory(dir);
    }
    private static void writeImmutable(Path path, byte[] data) throws IOException {
        try (FileChannel channel = FileChannel.open(path, CREATE_NEW, WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
    }
    private static byte[] envelope(byte[] data) throws IOException {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                out.writeInt(0x45424D31); out.writeInt(data.length);
                out.write(MessageDigest.getInstance("SHA-256").digest(data)); out.write(data);
            }
            return bytes.toByteArray();
        } catch (NoSuchAlgorithmException e) { throw new IOException(e); }
    }
    private static byte[] readChecked(Path file) throws IOException {
        long size = Files.size(file);
        if (size < 41 || size > MAX_BYTES + 40L) throw new IOException("Invalid file size: " + file);
        try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
            if (in.readInt() != 0x45424D31) throw new IOException("Invalid snapshot header");
            int count = in.readInt();
            if (count < 1 || count > MAX_BYTES || size != count + 40L) throw new IOException("Invalid byte count");
            byte[] hash = in.readNBytes(32), data = in.readNBytes(count);
            if (data.length != count || !MessageDigest.isEqual(hash, MessageDigest.getInstance("SHA-256").digest(data)))
                throw new IOException("Recovery checksum mismatch: " + file);
            return data;
        } catch (NoSuchAlgorithmException e) { throw new IOException(e); }
    }
    private static void forceDirectory(Path directory) throws IOException {
        try (FileChannel channel = FileChannel.open(directory, READ)) { channel.force(true); }
    }
}
