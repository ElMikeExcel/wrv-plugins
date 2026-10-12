// SPDX-License-Identifier: MIT
package com.winkreyes.shops.checkout;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.winkreyes.shops.checkout.CheckoutService.Journal;
import com.winkreyes.shops.checkout.CheckoutService.Quote;
import com.winkreyes.shops.checkout.CheckoutService.Stage;

/**
 * Forced, append-only evidence journal, not an atomic Vault transaction log. Each UTF-8 line
 * has tab-separated version, transaction UUID, buyer UUID, seller UUID or '-', URL-base64
 * offer ID, revision, quantity, amount in minor units, URL-base64 fingerprint, quote expiry,
 * stage, write time, URL-base64 detail, and SHA-256 of the preceding fields.
 *
 * The active-session marker is forced before this instance is usable. An existing marker,
 * any unfinished/uncertain transaction, or invalid journal prevents all new transactions.
 * A clean close removes the marker ONLY after callers have saved player/world state.
 * Reconciliation is deliberately an offline administrator operation, never automatic.
 */
public final class FileJournal implements Journal {
    public static final String LOG_FILE = "transactions.log";
    public static final String SESSION_FILE = "active-session";
    public static final long MAX_JOURNAL_BYTES = 256L * 1024 * 1024;
    private static final int MAX_LINE_LENGTH = 2 * 1024 * 1024;
    private static final int MAX_DETAIL_BYTES = CheckoutService.MAX_EVIDENCE_BYTES + 1024;
    private final Path directory;
    private final Path sessionFile;
    private final FileChannel channel;
    private final FileLock fileLock;
    private final Map<UUID, Stage> states = new HashMap<>();
    private boolean recovery;
    private String reason = "";
    private boolean closed;

    public FileJournal(Path directory) throws IOException {
        this.directory = Objects.requireNonNull(directory).toAbsolutePath().normalize();
        Files.createDirectories(this.directory);
        this.sessionFile = this.directory.resolve(SESSION_FILE);
        Path log = this.directory.resolve(LOG_FILE);
        FileChannel opened = FileChannel.open(log, StandardOpenOption.CREATE,
                StandardOpenOption.READ, StandardOpenOption.WRITE);
        FileLock lock = null;
        try {
            try { lock = opened.tryLock(); }
            catch (OverlappingFileLockException ex) { throw new IOException("Checkout journal is already open.", ex); }
            if (lock == null) throw new IOException("Checkout journal is locked by another process.");
            channel = opened;
            fileLock = lock;
            if (Files.exists(sessionFile)) requireRecovery("Previous session did not shut down cleanly.");
            if (Files.size(log) > MAX_JOURNAL_BYTES) {
                requireRecovery("Journal exceeded its safe size limit; reconcile and archive it offline.");
            } else {
                readJournal(log);
            }
            if (states.values().stream().anyMatch(stage -> !stage.terminal())) {
                requireRecovery("Journal contains an incomplete or uncertain transaction.");
            }
            createSessionMarker();
            syncDirectory();
            channel.position(channel.size());
        } catch (IOException | RuntimeException | Error ex) {
            if (lock != null) try { lock.release(); } catch (IOException ignored) { }
            try { opened.close(); } catch (IOException ignored) { }
            throw ex;
        }
    }

    @Override public synchronized boolean requiresRecovery() { return recovery; }
    @Override public synchronized String recoveryReason() { return reason; }
    public Path directory() { return directory; }

    @Override public synchronized void record(Quote quote, Stage stage, String detail) throws IOException {
        if (closed) throw new IOException("Checkout journal is closed.");
        if (recovery && stage != Stage.RECOVERY_REQUIRED) {
            throw new IOException("Checkout journal requires administrator reconciliation.");
        }
        try {
            Objects.requireNonNull(quote);
            Objects.requireNonNull(stage);
            Objects.requireNonNull(detail);
            if (detail.getBytes(StandardCharsets.UTF_8).length > MAX_DETAIL_BYTES) throw new IOException("Journal detail is too long.");
            Stage previous = states.get(quote.id());
            if (!allowed(previous, stage)) throw new IOException("Invalid checkout journal transition.");
            String body = String.join("\t", "1", quote.id().toString(), quote.buyerId().toString(),
                    quote.sellerId() == null ? "-" : quote.sellerId().toString(), encode(quote.offerId()),
                    Long.toString(quote.revision()), Integer.toString(quote.quantity()),
                    Long.toString(quote.amountMinorUnits()), encode(quote.fingerprint()),
                    quote.expiresAt().toString(), stage.name(), Instant.now().toString(), encode(detail));
            byte[] line = (body + "\t" + checksum(body) + "\n").getBytes(StandardCharsets.UTF_8);
            if (line.length > MAX_LINE_LENGTH || channel.size() + line.length > MAX_JOURNAL_BYTES) {
                throw new IOException("Checkout journal storage limit reached.");
            }
            ByteBuffer bytes = ByteBuffer.wrap(line);
            while (bytes.hasRemaining()) channel.write(bytes);
            channel.force(true); // An intent must reach storage before its associated side effect.
            states.put(quote.id(), stage);
            if (stage == Stage.RECOVERY_REQUIRED) requireRecovery(detail);
        } catch (IOException | RuntimeException ex) {
            requireRecovery("Journal append or force failed; do not repeat any economic action.");
            if (ex instanceof IOException io) throw io;
            throw new IOException("Invalid journal data.", ex);
        }
    }

    private void readJournal(Path log) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(log, StandardCharsets.UTF_8)) {
            String line;
            while ((line = readBoundedLine(reader)) != null) {
                if (line.length() > MAX_LINE_LENGTH) throw new IllegalArgumentException("Oversized record.");
                String[] fields = line.split("\t", -1);
                if (fields.length != 14 || !fields[0].equals("1")) throw new IllegalArgumentException("Invalid record.");
                int lastTab = line.lastIndexOf('\t');
                if (!checksum(line.substring(0, lastTab)).equals(fields[13])) throw new IllegalArgumentException("Invalid checksum.");
                UUID id = UUID.fromString(fields[1]);
                UUID.fromString(fields[2]);
                if (!fields[3].equals("-")) UUID.fromString(fields[3]);
                if (decode(fields[4]).isBlank() || Long.parseLong(fields[5]) < 0) throw new IllegalArgumentException("Invalid offer.");
                Money.requireQuantity(Integer.parseInt(fields[6]));
                Money.requirePositive(Long.parseLong(fields[7]));
                if (decode(fields[8]).isBlank()) throw new IllegalArgumentException("Missing fingerprint.");
                Instant.parse(fields[9]);
                Stage stage = Stage.valueOf(fields[10]);
                Instant.parse(fields[11]);
                String detail = decode(fields[12]);
                if (!allowed(states.get(id), stage)) throw new IllegalArgumentException("Invalid transition.");
                states.put(id, stage);
                if (stage == Stage.RECOVERY_REQUIRED) requireRecovery(detail);
            }
        } catch (IllegalArgumentException ex) {
            requireRecovery("Journal is incomplete or corrupt; preserve it for reconciliation.");
        }
    }

    /** Reject a truncated final record even if its complete prefix looks parseable. */
    private static String readBoundedLine(BufferedReader reader) throws IOException {
        StringBuilder line = new StringBuilder();
        int value;
        while ((value = reader.read()) != -1) {
            if (value == '\n') return line.toString();
            line.append((char) value);
            if (line.length() > MAX_LINE_LENGTH) throw new IllegalArgumentException("Oversized record.");
        }
        if (!line.isEmpty()) throw new IllegalArgumentException("Truncated record.");
        return null;
    }

    private static boolean allowed(Stage previous, Stage next) {
        if (next == Stage.RECOVERY_REQUIRED) return true;
        if (previous == null) return next == Stage.WITHDRAW_INTENT;
        return switch (previous) {
            case WITHDRAW_INTENT -> next == Stage.WITHDRAWN || next == Stage.NO_CHARGE;
            case WITHDRAWN -> next == Stage.TRANSFER_INTENT;
            case TRANSFER_INTENT -> next == Stage.TRANSFER_APPLIED || next == Stage.REFUND_INTENT;
            case TRANSFER_APPLIED -> next == Stage.PAYOUT_INTENT || next == Stage.COMMITTED;
            case PAYOUT_INTENT -> next == Stage.PAYOUT_DECLINED || next == Stage.PAID;
            case PAYOUT_DECLINED -> next == Stage.ROLLBACK_INTENT;
            case PAID -> next == Stage.COMMITTED;
            case ROLLBACK_INTENT -> next == Stage.ROLLED_BACK;
            case ROLLED_BACK -> next == Stage.REFUND_INTENT;
            case REFUND_INTENT -> next == Stage.REFUNDED;
            case REFUNDED, NO_CHARGE, COMMITTED, RECOVERY_REQUIRED -> false;
        };
    }

    private void createSessionMarker() throws IOException {
        if (Files.exists(sessionFile)) return;
        try (FileChannel marker = FileChannel.open(sessionFile, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            ByteBuffer content = StandardCharsets.UTF_8.encode("WRVShops active session " + UUID.randomUUID() + " " + Instant.now() + "\n");
            while (content.hasRemaining()) marker.write(content);
            marker.force(true);
        }
    }

    private void syncDirectory() throws IOException {
        // Refuse to start when the filesystem cannot provide required directory durability.
        try (FileChannel directoryChannel = FileChannel.open(directory, StandardOpenOption.READ)) {
            directoryChannel.force(true);
        }
    }

    private void requireRecovery(String detail) {
        recovery = true;
        if (reason.isEmpty()) reason = detail;
    }

    private static String encode(String text) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String text) {
        return new String(Base64.getUrlDecoder().decode(text), StandardCharsets.UTF_8);
    }

    private static String checksum(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Required SHA-256 implementation is missing.", impossible);
        }
    }

    @Override public synchronized void close() throws IOException { closeInternal(true); }
    @Override public synchronized void closeUnclean() throws IOException { closeInternal(false); }

    private void closeInternal(boolean clean) throws IOException {
        if (closed) return;
        try {
            channel.force(true);
            if (clean && !recovery && states.values().stream().allMatch(Stage::terminal)) {
                try {
                    Files.delete(sessionFile);
                    syncDirectory();
                } catch (IOException ex) {
                    requireRecovery("Could not durably complete a clean shutdown.");
                    try { createSessionMarker(); syncDirectory(); }
                    catch (IOException secondary) { ex.addSuppressed(secondary); }
                    throw ex;
                }
            }
        } finally {
            closed = true;
            try { fileLock.release(); }
            finally { channel.close(); }
        }
    }
}
