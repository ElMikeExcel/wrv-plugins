package com.winkreyes.buildmode.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Two private, installation-provisioned cohort replicas, stored outside the snapshot directory. */
public final class CohortStore {
    private static final int MAX_BYTES = 1024 * 1024;
    public record Result(Set<UUID> known, boolean verified, String problem) {
        public Result { known = Set.copyOf(known); }
    }
    private CohortStore() { }
    public static Result inspect(Path primary, Path backup) {
        Set<UUID> a = null, b = null, known = new HashSet<>();
        try { a = read(primary); known.addAll(a); } catch (IOException | RuntimeException ignored) { }
        try { b = read(backup); known.addAll(b); } catch (IOException | RuntimeException ignored) { }
        boolean good = a != null && b != null && a.equals(b) && !a.isEmpty();
        return new Result(known, good, good ? null : "Private recovery-cohort replicas are missing, unreadable or inconsistent. New Build entry is disabled.");
    }
    public static byte[] encode(Set<UUID> cohort) {
        if (cohort.isEmpty()) throw new IllegalArgumentException("Empty cohort");
        String body = "ExcelBuildMode recovery cohort v1\n" + String.join("\n", cohort.stream().map(UUID::toString).sorted().toList()) + "\n";
        return (body + "sha256=" + hash(body.getBytes(StandardCharsets.UTF_8)) + "\n").getBytes(StandardCharsets.UTF_8);
    }
    private static Set<UUID> read(Path file) throws IOException {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Missing regular cohort file");
        long size = Files.size(file);
        if (size < 100 || size > MAX_BYTES) throw new IOException("Invalid cohort size");
        String text = Files.readString(file, StandardCharsets.UTF_8);
        int at = text.lastIndexOf("sha256=");
        if (at < 0 || !text.endsWith("\n")) throw new IOException("Missing cohort checksum");
        String body = text.substring(0, at), expected = text.substring(at + 7).strip();
        if (!hash(body.getBytes(StandardCharsets.UTF_8)).equals(expected)) throw new IOException("Cohort checksum mismatch");
        String[] lines = body.split("\n");
        if (lines.length < 2 || !lines[0].equals("ExcelBuildMode recovery cohort v1")) throw new IOException("Invalid cohort version");
        Set<UUID> ids = new HashSet<>();
        for (int i = 1; i < lines.length; i++) {
            UUID id = UUID.fromString(lines[i]);
            if (!id.toString().equals(lines[i]) || !ids.add(id)) throw new IOException("Noncanonical or duplicate cohort identity");
        }
        return Set.copyOf(ids);
    }
    private static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
