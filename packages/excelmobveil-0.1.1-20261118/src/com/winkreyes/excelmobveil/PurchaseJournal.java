package com.winkreyes.excelmobveil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.CRC32;

/**
 * Append-only, force-to-disk purchase intent log. This is NOT a transaction
 * spanning Vault and Minecraft playerdata. Any unfinished transaction on load
 * requires an administrator to reconcile money and delivery before unlocking.
 * Corrupt/truncated records fail closed; no recovery ever replays money calls.
 */
public class PurchaseJournal {
    public enum Phase {
        WITHDRAW_INTENT, WITHDRAWN, REFUND_INTENT, RECONCILE,
        COMPLETED, REFUNDED, DECLINED;

        public boolean terminal() {
            return this == COMPLETED || this == REFUNDED || this == DECLINED;
        }
    }

    public record Entry(UUID transactionId, UUID playerId, PurchaseCoordinator.Product product,
                        double price, Phase phase, long timestamp) {}

    private final Path file;
    private final Map<UUID, Entry> latest = new LinkedHashMap<>();
    private boolean healthy = true;

    public PurchaseJournal(Path file) throws IOException {
        this.file = file.toAbsolutePath();
        Path parent = this.file.getParent();
        Files.createDirectories(parent);
        if (!Files.exists(this.file)) {
            try (FileChannel channel = FileChannel.open(this.file, StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            // Persist the newly created directory entry before any money operation.
            try (FileChannel directory = FileChannel.open(parent, StandardOpenOption.READ)) {
                directory.force(true);
            }
        }
        byte[] bytes = Files.readAllBytes(this.file);
        if (bytes.length > 0 && bytes[bytes.length - 1] != '\n') {
            throw new IOException("Purchase journal has a truncated tail; manual reconciliation required");
        }
        String content = new String(bytes, StandardCharsets.UTF_8);
        if (!content.isEmpty()) {
            for (String line : content.split("\n")) {
                Entry entry = decode(line);
                validateTransition(entry);
                latest.put(entry.transactionId(), entry);
            }
        }
    }

    public synchronized boolean healthy() { return healthy; }

    public synchronized Map<UUID, Entry> unresolved() {
        Map<UUID, Entry> result = new LinkedHashMap<>();
        latest.forEach((id, entry) -> { if (!entry.phase().terminal()) result.put(id, entry); });
        return Collections.unmodifiableMap(result);
    }

    public synchronized Map<UUID, Entry> entries() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(latest));
    }

    /** An append failure poisons this instance, even if no bytes appeared written. */
    public synchronized void append(Entry entry) throws IOException {
        if (!healthy) throw new IOException("Purchase journal is unavailable");
        try {
            validateTransition(entry);
            byte[] bytes = encode(entry).getBytes(StandardCharsets.UTF_8);
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            latest.put(entry.transactionId(), entry);
        } catch (IOException | RuntimeException failure) {
            healthy = false;
            throw new IOException("Could not durably append purchase journal", failure);
        }
    }

    private void validateTransition(Entry next) throws IOException {
        if (next == null || next.transactionId() == null || next.playerId() == null
                || next.product() == null || next.phase() == null
                || !Double.isFinite(next.price()) || next.price() <= 0) {
            throw new IOException("Invalid purchase journal record");
        }
        Entry previous = latest.get(next.transactionId());
        if (previous == null) {
            if (next.phase() != Phase.WITHDRAW_INTENT) {
                throw new IOException("Purchase journal transaction does not begin with intent");
            }
            return;
        }
        if (!previous.playerId().equals(next.playerId()) || previous.product() != next.product()
                || Double.compare(previous.price(), next.price()) != 0 || previous.phase().terminal()) {
            throw new IOException("Purchase journal transaction identity/state mismatch");
        }
        boolean legal = next.phase() == Phase.RECONCILE
                || previous.phase() == Phase.WITHDRAW_INTENT
                    && (next.phase() == Phase.WITHDRAWN || next.phase() == Phase.DECLINED)
                || previous.phase() == Phase.WITHDRAWN
                    && (next.phase() == Phase.COMPLETED || next.phase() == Phase.REFUND_INTENT)
                || previous.phase() == Phase.REFUND_INTENT && next.phase() == Phase.REFUNDED;
        if (!legal) throw new IOException("Invalid purchase journal phase transition");
    }

    private static String encode(Entry e) {
        String body = "1\t" + e.transactionId() + "\t" + e.playerId() + "\t" + e.product()
                + "\t" + Double.toHexString(e.price()) + "\t" + e.phase() + "\t" + e.timestamp();
        return body + "\t" + checksum(body) + "\n";
    }

    private static Entry decode(String line) throws IOException {
        try {
            String[] parts = line.split("\t", -1);
            if (parts.length != 8 || !parts[0].equals("1")) throw new IllegalArgumentException();
            String body = line.substring(0, line.lastIndexOf('\t'));
            if (!checksum(body).equals(parts[7])) throw new IllegalArgumentException();
            return new Entry(UUID.fromString(parts[1]), UUID.fromString(parts[2]),
                    PurchaseCoordinator.Product.valueOf(parts[3]), Double.valueOf(parts[4]),
                    Phase.valueOf(parts[5]), Long.parseLong(parts[6]));
        } catch (RuntimeException invalid) {
            throw new IOException("Purchase journal is corrupt; manual reconciliation required", invalid);
        }
    }

    private static String checksum(String body) {
        CRC32 checksum = new CRC32();
        checksum.update(body.getBytes(StandardCharsets.UTF_8));
        return Long.toHexString(checksum.getValue());
    }
}
