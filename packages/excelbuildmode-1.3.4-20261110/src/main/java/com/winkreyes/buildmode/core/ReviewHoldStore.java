package com.winkreyes.buildmode.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static java.nio.file.StandardOpenOption.*;

/** Sticky per-player review holds, independent of native saves and inventory snapshots. No automatic clearing. */
public final class ReviewHoldStore {
    private final Path root;
    public ReviewHoldStore(Path root) throws IOException {
        this.root = root;
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Review-hold directory is unavailable");
    }
    public Set<UUID> players() throws IOException {
        Set<UUID> ids = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, "*.hold")) {
            for (Path file : stream) {
                if (ids.size() >= 10_000) throw new IOException("Review-hold scan budget exceeded");
                try { ids.add(UUID.fromString(file.getFileName().toString().replaceFirst("\\.hold$", ""))); }
                catch (IllegalArgumentException e) { throw new IOException("Malformed review-hold identity", e); }
            }
        }
        return Set.copyOf(ids);
    }
    public void hold(UUID player) throws IOException {
        Path file = root.resolve(Objects.requireNonNull(player) + ".hold");
        // Presence is deliberately conservative even if an existing hold's content became corrupt.
        if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return;
        byte[] bytes = ("ExcelBuildMode manual review v1\n" + player + "\nDo not clear without verified recovery.\n").getBytes(StandardCharsets.UTF_8);
        Path temp = root.resolve(player + "-" + UUID.randomUUID() + ".tmp");
        try (FileChannel channel = FileChannel.open(temp, CREATE_NEW, WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) channel.write(buffer); channel.force(true);
        }
        Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE);
        try (FileChannel directory = FileChannel.open(root, READ)) { directory.force(true); }
    }
}
