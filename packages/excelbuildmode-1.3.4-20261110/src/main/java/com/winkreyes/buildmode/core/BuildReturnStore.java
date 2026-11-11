package com.winkreyes.buildmode.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static java.nio.file.StandardOpenOption.*;

/**
 * Durable "return to Build Mode" records (ExcelBuildMode 1.3), one small
 * file per player in a provisioned directory, written in the same
 * atomic style as ReviewHoldStore. Presence of a record means: this
 * player logged out (or the server stopped) while in Build Mode, and
 * on their next join the session is restored INTO Build Mode instead
 * of running the survival recovery — the logout/login desync fix.
 *
 * A record is only an intent, never an authority: it is honored only
 * while it names the player's currently active recovery-journal
 * session (see BuildReturnPolicy). The survival snapshot stays in the
 * RecoveryJournal exactly as before; this store holds no items, no
 * balances and no permissions.
 */
public final class BuildReturnStore {
    /** read() outcome: a usable record, no record, or an unreadable one. */
    public sealed interface Record {
        record Present(UUID session) implements Record { }
        record Absent() implements Record { }
        record Corrupt(String reason) implements Record { }
    }

    private static final String HEADER = "ExcelBuildMode build-return v1";
    private final Path root;

    public BuildReturnStore(Path root) throws IOException {
        this.root = root;
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Build-return directory is unavailable");
    }

    /** Bind the player to the journal session they entered Build Mode with. Overwrites any prior record. */
    public void record(UUID player, UUID session) throws IOException {
        Objects.requireNonNull(player); Objects.requireNonNull(session);
        Path file = file(player);
        byte[] bytes = (HEADER + "\n" + player + "\n" + session + "\n").getBytes(StandardCharsets.UTF_8);
        Path temp = root.resolve(player + "-" + UUID.randomUUID() + ".tmp");
        try (FileChannel channel = FileChannel.open(temp, CREATE_NEW, WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) channel.write(buffer); channel.force(true);
        }
        Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        try (FileChannel directory = FileChannel.open(root, READ)) { directory.force(true); }
    }

    /** Read the record without mutating anything. A malformed file is Corrupt, never a guess. */
    public Record read(UUID player) {
        Path file = file(player);
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return new Record.Absent();
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (lines.size() != 3 || !lines.get(0).equals(HEADER)) return new Record.Corrupt("Unexpected build-return file shape");
            if (!UUID.fromString(lines.get(1)).equals(player)) return new Record.Corrupt("Build-return file names another player");
            return new Record.Present(UUID.fromString(lines.get(2)));
        } catch (Exception e) {
            return new Record.Corrupt("Unreadable build-return file: " + e.getMessage());
        }
    }

    /** Remove the record (session exited Build Mode for good). Absence is not an error. */
    public void clear(UUID player) throws IOException {
        Files.deleteIfExists(file(player));
    }

    private Path file(UUID player) { return root.resolve(Objects.requireNonNull(player) + ".return"); }
}
