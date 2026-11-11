package com.winkreyes.buildmode.core;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import static java.nio.file.StandardOpenOption.*;

/** Independent, checksummed latest-session index. Never contains or deletes inventory snapshots. */
public final class ActiveSessionIndex {
    private final Path root;
    public record Entry(UUID session, boolean active) { }
    public ActiveSessionIndex(Path root) throws IOException { this.root = root; Files.createDirectories(root); force(root); }
    public Set<UUID> players() throws IOException {
        Set<UUID> ids = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, "*.idx")) {
            for (Path p : stream) {
                try { ids.add(UUID.fromString(p.getFileName().toString().replaceFirst("\\.idx$", ""))); }
                catch (IllegalArgumentException e) { throw new IOException("Unknown index identity", e); }
            }
        }
        return Set.copyOf(ids);
    }
    public Optional<Entry> read(UUID player) throws IOException {
        Path path = root.resolve(player + ".idx");
        byte[] bytes;
        try {
            var a = Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!a.isRegularFile() || a.isSymbolicLink() || a.size() != 69) throw new IOException("Invalid index file");
            bytes = Files.readAllBytes(path);
        } catch (NoSuchFileException absent) { return Optional.empty(); }
        if (bytes.length != 69) throw new IOException("Invalid index length");
        byte[] body = Arrays.copyOf(bytes, 37), hash = Arrays.copyOfRange(bytes, 37, 69);
        if (!MessageDigest.isEqual(hash, digest(body))) throw new IOException("Invalid index checksum");
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(body))) {
            if (in.readInt() != 0x45424931 || !new UUID(in.readLong(), in.readLong()).equals(player)) throw new IOException("Invalid index identity/version");
            return Optional.of(new Entry(new UUID(in.readLong(), in.readLong()), in.readBoolean()));
        }
    }
    public void begin(RecoveryJournal.Session session) throws IOException { write(session.player(), new Entry(session.id(), true)); }
    public void validate(RecoveryJournal.Session session) throws IOException {
        var entry = read(session.player());
        if (entry.isPresent() && entry.get().active() && !entry.get().session().equals(session.id())) throw new IOException("Journal and active index disagree");
    }
    public void complete(RecoveryJournal.Session session) throws IOException { validate(session); write(session.player(), new Entry(session.id(), false)); }
    private void write(UUID player, Entry entry) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(buffer)) {
            out.writeInt(0x45424931); out.writeLong(player.getMostSignificantBits()); out.writeLong(player.getLeastSignificantBits());
            out.writeLong(entry.session().getMostSignificantBits()); out.writeLong(entry.session().getLeastSignificantBits()); out.writeBoolean(entry.active());
        }
        byte[] body = buffer.toByteArray(); buffer.write(digest(body));
        Path temp = root.resolve(player + "-" + UUID.randomUUID() + ".tmp");
        try (FileChannel channel = FileChannel.open(temp, CREATE_NEW, WRITE)) {
            ByteBuffer bytes = ByteBuffer.wrap(buffer.toByteArray()); while (bytes.hasRemaining()) channel.write(bytes); channel.force(true);
        }
        Files.move(temp, root.resolve(player + ".idx"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); force(root);
    }
    private static byte[] digest(byte[] bytes) throws IOException {
        try { return MessageDigest.getInstance("SHA-256").digest(bytes); } catch (NoSuchAlgorithmException e) { throw new IOException(e); }
    }
    private static void force(Path path) throws IOException { try (FileChannel channel = FileChannel.open(path, READ)) { channel.force(true); } }
}
