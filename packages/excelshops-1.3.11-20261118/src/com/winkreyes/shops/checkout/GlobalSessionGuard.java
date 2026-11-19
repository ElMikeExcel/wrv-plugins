// SPDX-License-Identifier: MIT
package com.winkreyes.shops.checkout;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Global session proof (ExcelShops 1.3.7, reconstructed from the live
 * 1.3.7-analytics-OFFLINE jar — see the 1.3.8 build record).
 *
 * When enabled (config session-proof-enabled, default OFF), the guard
 * proves at startup that BOTH checkout journals ended their previous
 * session in a fully validated terminal state: it fingerprints the
 * journal files and their active-session markers (FileJournal.proof),
 * compares them against the READY envelope written by recordReady,
 * and only then may a no-activity recovery be accepted. The first
 * mutation of a session writes a durable fence file; any uncertainty
 * writes an UNCERTAIN fence. A clean shutdown (closeClean) removes
 * both files, which is what makes the next start a CLEAN_START.
 * Every file the guard writes carries a trailing SHA-256 checksum
 * line and is forced to storage. When disabled, every operation is
 * a no-op and no lock or file is created.
 */
public final class GlobalSessionGuard implements AutoCloseable {
    private static final Set<String> KEYS = Set.of("checkout", "legacy-checkout");
    private static final String HEADER = "ExcelShops global session 2";
    private final Path root;
    private final Path envelope;
    private final Path fence;
    private final DataFileLock lock;
    private final boolean enabled;
    private String session;
    private Map<String, Fingerprint> assessed;
    private String assessedEnvelopeBody;
    private Decision decision;
    private boolean ready;
    private boolean fenced;
    private boolean uncertain;
    private boolean closed;

    public static GlobalSessionGuard open(Path root, boolean enabled) throws IOException {
        return new GlobalSessionGuard(root, enabled);
    }

    private GlobalSessionGuard(Path root, boolean enabled) throws IOException {
        this.enabled = enabled;
        this.root = root.toAbsolutePath().normalize();
        this.envelope = this.root.resolve("global-session-v2");
        this.fence = this.root.resolve("global-session-v2.fence");
        if (!enabled) {
            this.lock = null;
        } else {
            Files.createDirectories(this.root);
            if (this.root.getFileName() != null && this.root.getParent() != null) {
                Path lockPath = this.root.resolveSibling("." + this.root.getFileName() + "-global-session-v2.lock");
                this.lock = DataFileLock.open(lockPath, true);
            } else {
                throw new IOException("Plugin root must have a parent");
            }
        }
    }

    public synchronized Decision assess(Map<String, Fingerprint> proofs, boolean anyMarkerPresent) throws IOException {
        requireOpen();
        if (!enabled) return Decision.BLOCKED;
        requireProofs(proofs);
        assessed = Map.copyOf(proofs);
        assessedEnvelopeBody = null;
        decision = Decision.BLOCKED;
        if (ready || uncertain || present(fence)) return decision;
        if (!present(envelope)) {
            if (!anyMarkerPresent) decision = Decision.CLEAN_START;
            return decision;
        }
        try {
            String body = readChecked(envelope);
            String[] lines = body.split("\n", -1);
            if (lines.length != 6 || !HEADER.equals(lines[0]) || !"READY".equals(lines[2])
                    || !rootBinding().equals(lines[3])) {
                return decision;
            }
            UUID.fromString(lines[1]);
            if (proofs.values().stream().allMatch(proof -> !proof.markerSha256().isEmpty())
                    && line("checkout", proofs.get("checkout")).equals(lines[4])
                    && line("legacy-checkout", proofs.get("legacy-checkout")).equals(lines[5])) {
                decision = Decision.NO_OP_RECOVERY_ELIGIBLE;
                assessedEnvelopeBody = body;
            }
        } catch (IOException | IllegalArgumentException failure) {
            decision = Decision.BLOCKED;
        }
        return decision;
    }

    public synchronized void recordReady(Map<String, Fingerprint> proofs) throws IOException {
        recordReady(proofs, false);
    }

    public synchronized void recordReady(Map<String, Fingerprint> proofs, boolean allowRecovery) throws IOException {
        requireOpen();
        if (!enabled) return;
        requireProofs(proofs);
        if (ready || uncertain || assessed == null || !assessed.equals(proofs)
                || decision == null || decision == Decision.BLOCKED
                || (decision == Decision.NO_OP_RECOVERY_ELIGIBLE && !allowRecovery)
                || present(fence)) {
            throw new IOException("Session proof not eligible");
        }
        session = UUID.randomUUID().toString();
        String body = HEADER + "\n" + session + "\nREADY\n" + rootBinding() + "\n"
                + line("checkout", proofs.get("checkout")) + "\n"
                + line("legacy-checkout", proofs.get("legacy-checkout"));
        try {
            if (decision == Decision.NO_OP_RECOVERY_ELIGIBLE) {
                String current = readChecked(envelope);
                if (!current.equals(assessedEnvelopeBody)) {
                    throw new IOException("Session evidence changed after assessment");
                }
                archiveRecoveryEvidence(current);
            } else if (present(envelope)) {
                throw new IOException("Unexpected session envelope appeared");
            }
            writeAtomic(envelope, body);
            ready = true;
        } catch (IOException failure) {
            uncertain = true;
            throw failure;
        }
    }

    public synchronized void beforeMutation(String reason) throws IOException {
        requireOpen();
        if (!enabled) return;
        if (!ready) {
            markUncertain("Mutation attempted before READY: " + reason);
            throw new IOException("Global session is not ready");
        }
        if (uncertain) throw new IOException("Global session is uncertain");
        if (!fenced) {
            try {
                if (present(fence)) throw new IOException("Unexpected existing mutation fence");
                writeNew(fence, HEADER + "\n" + session + "\nMUTATION\n" + safeReason(reason));
                fenced = true;
            } catch (IOException failure) {
                uncertain = true;
                throw failure;
            }
        }
    }

    public synchronized void markUncertain(String reason) throws IOException {
        requireOpen();
        if (!enabled) return;
        uncertain = true;
        if (!present(fence)) {
            writeNew(fence, HEADER + "\n" + (session == null ? "UNSTARTED" : session) + "\nUNCERTAIN\n"
                    + safeReason(reason != null && !reason.isBlank()
                            ? reason.substring(0, Math.min(reason.length(), 2048)) : "unspecified uncertainty"));
        }
        fenced = true;
    }

    public synchronized void closeClean(boolean cleanBoundary) throws IOException {
        requireOpen();
        if (!enabled) {
            close();
            return;
        }
        if (cleanBoundary && ready && !uncertain) {
            try {
                Files.deleteIfExists(envelope);
                syncDirectory();
                Files.deleteIfExists(fence);
                syncDirectory();
            } finally {
                close();
            }
        } else {
            throw new IOException("Clean boundary not established");
        }
    }

    public synchronized void closeUnclean() throws IOException {
        close();
    }

    @Override public synchronized void close() throws IOException {
        if (closed) return;
        closed = true;
        if (lock != null) lock.close();
    }

    private static boolean present(Path path) throws IOException {
        try {
            Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            return true;
        } catch (NoSuchFileException missing) {
            return false;
        }
    }

    private void requireOpen() throws IOException {
        if (closed) throw new IOException("Session guard closed");
    }

    private static void requireProofs(Map<String, Fingerprint> proofs) throws IOException {
        if (proofs == null || !proofs.keySet().equals(KEYS)
                || proofs.values().stream().anyMatch(proof -> proof == null || !proof.validTerminal())) {
            throw new IOException("Both fully validated terminal journal proofs required");
        }
    }

    private String rootBinding() {
        return "root\t" + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(root.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String line(String key, Fingerprint proof) {
        return key + "\t" + proof.length() + "\t" + proof.sha256() + "\t" + proof.markerSha256();
    }

    private static String safeReason(String reason) throws IOException {
        if (reason == null || reason.isBlank() || reason.length() > 2048) {
            throw new IOException("Invalid mutation reason");
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(reason.getBytes(StandardCharsets.UTF_8));
    }

    private static String digest(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private String readChecked(Path path) throws IOException {
        BasicFileAttributes attributes = DataFileLock.checkedAttributes(path);
        if (!attributes.isRegularFile() || attributes.size() > 16384L) {
            throw new IOException("Invalid envelope");
        }
        byte[] bytes = Files.readAllBytes(path);
        String text = new String(bytes, StandardCharsets.UTF_8);
        int lastNewline = text.lastIndexOf('\n', text.length() - 2);
        if (!text.endsWith("\n") || lastNewline < 0) throw new IOException("Truncated envelope");
        String body = text.substring(0, lastNewline);
        String checksum = text.substring(lastNewline + 1, text.length() - 1);
        if (!digest(body.getBytes(StandardCharsets.UTF_8)).equals(checksum)) {
            throw new IOException("Envelope checksum mismatch");
        }
        return body;
    }

    private void archiveRecoveryEvidence(String body) throws IOException {
        Path audit = root.resolve("global-session-v2-audit");
        if (!present(audit)) {
            Files.createDirectory(audit);
            syncDirectory();
        }
        if (!DataFileLock.checkedAttributes(audit).isDirectory()) {
            throw new IOException("Recovery audit path is not a directory");
        }
        Path archived = audit.resolve("no-op-" + UUID.randomUUID() + ".proof");
        writeNew(archived, body);
        syncDirectory(audit);
    }

    private void writeAtomic(Path target, String body) throws IOException {
        Path temporary = root.resolve("global-session-v2.tmp-" + UUID.randomUUID());
        writeNew(temporary, body);
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            syncDirectory();
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void writeNew(Path target, String body) throws IOException {
        byte[] bytes = (body + "\n" + digest(body.getBytes(StandardCharsets.UTF_8)) + "\n")
                .getBytes(StandardCharsets.UTF_8);
        try (FileChannel channel = FileChannel.open(target, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
        syncDirectory();
    }

    private void syncDirectory() throws IOException {
        syncDirectory(root);
    }

    private void syncDirectory(Path target) throws IOException {
        // Directory fsync is best-effort: Windows refuses directory channels
        // (AccessDeniedException on open), so a refusal must not fail the save;
        // the file itself is always forced. Other failures propagate.
        try (FileChannel channel = FileChannel.open(target, StandardOpenOption.READ)) {
            channel.force(true);
        } catch (AccessDeniedException | UnsupportedOperationException refused) {
            // Platform cannot fsync directories; file-level fsync already done.
        }
    }

    public enum Decision { CLEAN_START, NO_OP_RECOVERY_ELIGIBLE, BLOCKED }

    public record Fingerprint(long length, String sha256, boolean validTerminal, String markerSha256) {
        public Fingerprint(long length, String sha256, boolean validTerminal) {
            this(length, sha256, validTerminal, "");
        }

        public Fingerprint {
            if (length < 0 || sha256 == null || !sha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("Invalid journal fingerprint");
            }
            if (markerSha256 == null || !(markerSha256.isEmpty() || markerSha256.matches("[0-9a-f]{64}"))) {
                throw new IllegalArgumentException("Invalid marker fingerprint");
            }
        }
    }
}
