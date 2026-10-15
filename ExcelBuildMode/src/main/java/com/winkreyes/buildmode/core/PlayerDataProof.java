package com.winkreyes.buildmode.core;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import static java.nio.file.StandardOpenOption.*;

/** Bounded, read-only proof of the exact saved native player record. Does not deserialize or mutate inventories/PDC. */
public final class PlayerDataProof {
    public static final String MARKER_KEY = "excelbuildmode:recovery";
    public static final int MAX_COMPRESSED_BYTES = 16 * 1024 * 1024;
    public static final int MAX_DECOMPRESSED_BYTES = 32 * 1024 * 1024;
    public static final int MAX_DEPTH = 64, MAX_ELEMENTS = 1_000_000;
    public enum Status { VERIFIED_UNMARKED_SURVIVAL, MARKED, UNKNOWN }
    /** marker is null both for absence and wrong-type markers; status/reason distinguish these cases. */
    public record Inspection(Status status, UUID player, int gameMode, String marker, String reason) { }
    private record Parsed(UUID player, int gameMode, boolean markerPresent, boolean markerString, String marker) {
        Inspection inspection() {
            if (markerPresent) return new Inspection(Status.MARKED, player, gameMode, marker,
                markerString && validMarker(marker) ? "Native recovery marker is present" : "Malformed or unknown native recovery marker is present");
            if (gameMode == 0) return new Inspection(Status.VERIFIED_UNMARKED_SURVIVAL, player, gameMode, null, "Verified UUID, Survival mode and absent recovery marker");
            return new Inspection(Status.UNKNOWN, player, gameMode, null, "Unmarked native playerdata is not Survival");
        }
    }
    @FunctionalInterface interface VerificationHook { void at(String point) throws IOException; }
    private PlayerDataProof() { }

    /** Invalid, missing, changing, non-regular or unsupported data is UNKNOWN, never a clean-player inference. */
    public static Inspection inspect(Path dat, UUID expectedPlayer, String markerKey) {
        try {
            validateArguments(dat, expectedPlayer, markerKey);
            BasicFileAttributes before = stamp(dat);
            byte[] bytes;
            try (FileChannel channel = FileChannel.open(dat, READ, LinkOption.NOFOLLOW_LINKS)) { bytes = readBounded(channel); }
            Parsed parsed = parse(bytes, expectedPlayer, markerKey);
            requireSame(before, stamp(dat));
            return parsed.inspection();
        } catch (IOException | RuntimeException failure) {
            return new Inspection(Status.UNKNOWN, null, -1, null,
                "Native playerdata could not be verified: " + failure.getClass().getSimpleName() + ": " + Objects.toString(failure.getMessage(), "verification failed"));
        }
    }

    /**
     * Verify EXACT current bytes and marker, fsync that same inode and parent directory, and reject detectable
     * replacement/change. Call after native saveData on the server thread, with no competing native save.
     * This method never writes marker values or repairs a record. A return is a point-in-time local-fs proof,
     * not an atomic transaction with the plugin journal, a guarantee against external writes, or disk hardware.
     */
    public static Inspection forceVerifiedMarker(Path dat, UUID expectedPlayer, String markerKey,
                                                 String expectedMarkerOrNull, boolean requireSurvival) throws IOException {
        return forceVerifiedMarker(dat, expectedPlayer, markerKey, expectedMarkerOrNull, requireSurvival, point -> {});
    }
    static Inspection forceVerifiedMarker(Path dat, UUID expectedPlayer, String markerKey,
                                          String expectedMarkerOrNull, boolean requireSurvival, VerificationHook hook) throws IOException {
        validateArguments(dat, expectedPlayer, markerKey);
        if (expectedMarkerOrNull != null && !validMarker(expectedMarkerOrNull)) throw new IOException("Expected marker is not a recognized v1 state/session");
        BasicFileAttributes before = stamp(dat);
        // Opening read/write makes force meaningful even though this verifier never writes data itself.
        try (FileChannel channel = FileChannel.open(dat, READ, WRITE, LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = readBounded(channel);
            Parsed parsed = parse(bytes, expectedPlayer, markerKey);
            requireExpected(parsed, expectedMarkerOrNull, requireSurvival);
            requireSame(before, stamp(dat));
            hook.at("verified");
            channel.force(true);
            Path parent = dat.toAbsolutePath().normalize().getParent();
            if (parent == null) throw new IOException("Playerdata parent directory is unavailable");
            try (FileChannel directory = FileChannel.open(parent, READ)) { directory.force(true); }
            hook.at("forced");
            requireSame(before, stamp(dat));
            if (!Arrays.equals(bytes, readBounded(channel))) throw new IOException("Native playerdata bytes changed during verification");
            // Reopen the path as well as rereading the original descriptor: an atomic replacement is unsafe.
            try (FileChannel current = FileChannel.open(dat, READ, LinkOption.NOFOLLOW_LINKS)) {
                if (!Arrays.equals(bytes, readBounded(current))) throw new IOException("Native playerdata path changed during verification");
            }
            requireSame(before, stamp(dat));
            return parsed.inspection();
        }
    }
    private static void validateArguments(Path dat, UUID expectedPlayer, String markerKey) throws IOException {
        if (dat == null || expectedPlayer == null) throw new IOException("Playerdata path and expected UUID are required");
        if (!MARKER_KEY.equals(markerKey)) throw new IOException("Only the helper's own recovery marker may be inspected");
    }
    private static BasicFileAttributes stamp(Path dat) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(dat, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile() || attributes.isSymbolicLink()) throw new IOException("Native playerdata is not a regular non-symlink file");
        if (attributes.size() < 18 || attributes.size() > MAX_COMPRESSED_BYTES) throw new IOException("Native playerdata compressed size is outside the safety bound");
        if (attributes.fileKey() == null) throw new IOException("Filesystem does not expose a stable native playerdata identity");
        return attributes;
    }
    private static void requireSame(BasicFileAttributes before, BasicFileAttributes after) throws IOException {
        if (!Objects.equals(before.fileKey(), after.fileKey()) || before.size() != after.size()
            || !before.lastModifiedTime().equals(after.lastModifiedTime())) throw new IOException("Native playerdata file changed during verification");
    }
    private static byte[] readBounded(FileChannel channel) throws IOException {
        if (channel.size() < 18 || channel.size() > MAX_COMPRESSED_BYTES) throw new IOException("Native playerdata compressed size is outside the safety bound");
        channel.position(0);
        ByteArrayOutputStream output = new ByteArrayOutputStream((int)Math.min(channel.size(), 8192));
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        while (channel.read(buffer) != -1) {
            buffer.flip();
            if (output.size() + buffer.remaining() > MAX_COMPRESSED_BYTES) throw new IOException("Native playerdata grew past its compressed bound");
            output.write(buffer.array(), 0, buffer.remaining()); buffer.clear();
        }
        return output.toByteArray();
    }
    private static void requireExpected(Parsed parsed, String expectedMarker, boolean requireSurvival) throws IOException {
        if (requireSurvival && parsed.gameMode != 0) throw new IOException("Saved native playerdata is not Survival");
        if (expectedMarker == null) {
            if (parsed.markerPresent) throw new IOException("Saved recovery marker has not been removed");
        } else if (!parsed.markerPresent || !parsed.markerString || !expectedMarker.equals(parsed.marker))
            throw new IOException("Saved recovery marker does not match the expected session and state");
    }
    private static boolean validMarker(String marker) {
        if (marker == null) return false;
        String[] pieces = marker.split(":", -1);
        if (pieces.length != 3 || !pieces[0].equals("v1") || !(pieces[1].equals("active") || pieces[1].equals("restored") || pieces[1].equals("review"))) return false;
        try { return UUID.fromString(pieces[2]).toString().equals(pieces[2]); }
        catch (IllegalArgumentException invalid) { return false; }
    }
    private static Parsed parse(byte[] compressed, UUID expectedPlayer, String markerKey) throws IOException {
        byte[] decoded = inflateOneMember(compressed);
        Parser parser = new Parser(decoded, markerKey);
        if (parser.in.readUnsignedByte() != 10) throw new IOException("Native playerdata root is not an NBT compound");
        parser.in.readUTF(); // NBT root name is not player identity.
        parser.compound(0, Scope.ROOT);
        if (parser.in.read() != -1) throw new IOException("Trailing bytes after native playerdata NBT");
        if (parser.uuid == null || !parser.uuid.equals(expectedPlayer)) throw new IOException("Native playerdata UUID is missing or mismatched");
        if (parser.gameMode == null || parser.gameMode < 0 || parser.gameMode > 3) throw new IOException("Native playerdata game mode is missing or invalid");
        return new Parsed(parser.uuid, parser.gameMode, parser.markerPresent, parser.markerString, parser.marker);
    }
    /** Strictly one complete gzip member: reject CRC/ISIZE errors, concatenation and ignored trailing junk. */
    private static byte[] inflateOneMember(byte[] bytes) throws IOException {
        if (bytes.length < 18 || bytes.length > MAX_COMPRESSED_BYTES) throw new IOException("Invalid gzip length");
        if (u8(bytes, 0) != 31 || u8(bytes, 1) != 139 || u8(bytes, 2) != 8) throw new IOException("Native playerdata is not gzip/deflate");
        int flags = u8(bytes, 3), offset = 10;
        if ((flags & 0xe0) != 0) throw new IOException("Reserved gzip flags");
        if ((flags & 4) != 0) { int size = u8(bytes, offset) | u8(bytes, offset + 1) << 8; offset += 2; offset = advance(bytes, offset, size); }
        if ((flags & 8) != 0) while (u8(bytes, offset++) != 0) { }
        if ((flags & 16) != 0) while (u8(bytes, offset++) != 0) { }
        if ((flags & 2) != 0) {
            CRC32 header = new CRC32(); header.update(bytes, 0, offset);
            int expected = u8(bytes, offset) | u8(bytes, offset + 1) << 8;
            if (((int)header.getValue() & 0xffff) != expected) throw new IOException("Invalid gzip header checksum"); offset += 2;
        }
        if (bytes.length - offset < 8) throw new IOException("Truncated gzip payload");
        Inflater inflater = new Inflater(true); CRC32 crc = new CRC32();
        try {
            inflater.setInput(bytes, offset, bytes.length - offset);
            ByteArrayOutputStream output = new ByteArrayOutputStream(); byte[] buffer = new byte[8192];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count == 0 && !inflater.finished()) throw new IOException("Truncated or unsupported gzip deflate payload");
                if (output.size() + count > MAX_DECOMPRESSED_BYTES) throw new IOException("Native playerdata exceeds decompressed safety bound");
                output.write(buffer, 0, count); crc.update(buffer, 0, count);
            }
            int trailer = bytes.length - inflater.getRemaining();
            if (bytes.length - trailer != 8) throw new IOException("Extra gzip members or trailing compressed data");
            if (little32(bytes, trailer) != crc.getValue() || little32(bytes, trailer + 4) != output.size()) throw new IOException("Invalid gzip payload checksum or size");
            return output.toByteArray();
        } catch (DataFormatException invalid) { throw new IOException("Invalid gzip deflate payload", invalid); }
        finally { inflater.end(); }
    }
    private static int u8(byte[] bytes, int index) throws IOException { if (index < 0 || index >= bytes.length) throw new EOFException("Truncated gzip header/trailer"); return bytes[index] & 255; }
    private static int advance(byte[] bytes, int offset, int count) throws IOException { if (offset < 0 || count < 0 || count > bytes.length - offset) throw new EOFException("Truncated gzip header"); return offset + count; }
    private static long little32(byte[] bytes, int offset) throws IOException { return (long)u8(bytes, offset) | (long)u8(bytes, offset + 1) << 8 | (long)u8(bytes, offset + 2) << 16 | (long)u8(bytes, offset + 3) << 24; }
    private enum Scope { ROOT, PDC, IGNORE }
    private static final class Parser {
        final DataInputStream in; final String markerKey; int elements;
        UUID uuid; Integer gameMode; boolean markerPresent, markerString; String marker;
        Parser(byte[] bytes, String markerKey) { in = new DataInputStream(new ByteArrayInputStream(bytes)); this.markerKey = markerKey; }
        void compound(int depth, Scope scope) throws IOException {
            if (depth > MAX_DEPTH) throw new IOException("NBT nesting exceeds safety bound");
            Set<String> names = new HashSet<>();
            for (int type; (type = in.readUnsignedByte()) != 0;) {
                count(); String name = in.readUTF();
                if (!names.add(name)) throw new IOException("Duplicate NBT compound key");
                if (scope == Scope.ROOT && name.equals("UUID")) {
                    if (type != 11 || in.readInt() != 4) throw new IOException("Native player UUID is not a four-int array");
                    long high = (long)in.readInt() << 32 | Integer.toUnsignedLong(in.readInt());
                    long low = (long)in.readInt() << 32 | Integer.toUnsignedLong(in.readInt()); uuid = new UUID(high, low);
                } else if (scope == Scope.ROOT && name.equals("playerGameType")) {
                    if (type != 3) throw new IOException("Native player game mode is not an integer"); gameMode = in.readInt();
                } else if (scope == Scope.ROOT && name.equals("BukkitValues")) {
                    if (type != 10) throw new IOException("Native PDC root is not a compound"); compound(depth + 1, Scope.PDC);
                } else if (scope == Scope.PDC && name.equals(markerKey)) {
                    markerPresent = true; markerString = type == 8;
                    if (markerString) marker = in.readUTF(); else payload(type, depth + 1);
                } else payload(type, depth + 1);
            }
        }
        void payload(int type, int depth) throws IOException {
            if (depth > MAX_DEPTH) throw new IOException("NBT nesting exceeds safety bound");
            switch (type) {
                case 1 -> in.readByte(); case 2 -> in.readShort(); case 3 -> in.readInt(); case 4 -> in.readLong(); case 5 -> in.readFloat(); case 6 -> in.readDouble();
                case 7 -> in.skipNBytes(length()); case 8 -> in.readUTF();
                case 9 -> { int elementType = in.readUnsignedByte(), length = length(); if (elementType > 12 || elementType == 0 && length != 0) throw new IOException("Invalid NBT list element type"); for (int i = 0; i < length; i++) { count(); payload(elementType, depth + 1); } }
                case 10 -> compound(depth, Scope.IGNORE);
                case 11 -> in.skipNBytes((long)length() * 4); case 12 -> in.skipNBytes((long)length() * 8);
                default -> throw new IOException("Unknown NBT tag type");
            }
        }
        int length() throws IOException { int count = in.readInt(); if (count < 0 || count > MAX_ELEMENTS) throw new IOException("NBT array/list length exceeds safety bound"); return count; }
        void count() throws IOException { if (++elements > MAX_ELEMENTS) throw new IOException("NBT element count exceeds safety bound"); }
    }
}
