/* SPDX-License-Identifier: MIT */
package com.winkreyes.shops.store;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.bukkit.inventory.ItemStack;

/** Bounded, checksummed file with atomic replacement. No Java object deserialization. */
final class RegistryFile {
    static final int MAGIC = 0x57525653, VERSION = 1, MAX_BYTES = 32 * 1024 * 1024;
    record Placement(UUID ownerId, UUID identity) {}
    record State(Map<BlockPos, Placement> placements, Map<UUID, ShopRecord> shops) {
        State { placements = Map.copyOf(placements); shops = Map.copyOf(shops); }
    }
    interface ItemCodec {
        byte[] encode(ItemStack item);
        ItemStack decode(byte[] bytes);
    }
    static final ItemCodec BUKKIT = new ItemCodec() {
        public byte[] encode(ItemStack item) { return item.serializeAsBytes(); }
        public ItemStack decode(byte[] bytes) { return ItemStack.deserializeBytes(bytes); }
    };
    private final Path path;
    private final ItemCodec codec;
    RegistryFile(Path path, ItemCodec codec) { this.path = path; this.codec = codec; }

    State read() throws IOException {
        if (!Files.exists(path)) return new State(Map.of(), Map.of());
        if (Files.size(path) > MAX_BYTES) throw new IOException("Registry exceeds maximum size");
        try (DataInputStream in = new DataInputStream(Files.newInputStream(path))) {
            if (in.readInt() != MAGIC || in.readInt() != VERSION) throw new IOException("Unsupported registry header");
            int length = bound(in.readInt(), MAX_BYTES, "payload length");
            byte[] digest = in.readNBytes(32), bytes = in.readNBytes(length);
            if (digest.length != 32 || bytes.length != length || in.read() != -1 || !MessageDigest.isEqual(digest, sha256(bytes)))
                throw new IOException("Registry checksum or length mismatch");
            return decode(bytes);
        } catch (RuntimeException failure) {
            throw new IOException("Invalid registry contents", failure);
        }
    }

    void write(State state) throws IOException {
        byte[] bytes;
        try { bytes = encode(state); }
        catch (RuntimeException failure) { throw new IOException("Could not encode shop items", failure); }
        if (bytes.length > MAX_BYTES) throw new IOException("Registry exceeds maximum size");
        Path directory = path.toAbsolutePath().getParent();
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, ".shops-", ".tmp");
        try {
            ByteArrayOutputStream payload = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(payload)) {
                out.writeInt(MAGIC); out.writeInt(VERSION); out.writeInt(bytes.length); out.write(sha256(bytes)); out.write(bytes);
            }
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(payload.toByteArray());
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            // Fail rather than silently fall back to a non-atomic overwrite.
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            // Linux supports directory fsync; some other providers do not.
            try (FileChannel directoryChannel = FileChannel.open(directory, StandardOpenOption.READ)) {
                directoryChannel.force(true);
            } catch (UnsupportedOperationException | IOException ignored) { /* file itself was already forced */ }
        } finally { Files.deleteIfExists(temporary); }
    }

    private byte[] encode(State state) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(state.placements.size());
            for (var entry : state.placements.entrySet()) {
                position(out, entry.getKey()); uuid(out, entry.getValue().ownerId); uuid(out, entry.getValue().identity);
            }
            out.writeInt(state.shops.size());
            for (ShopRecord shop : state.shops.values()) {
                uuid(out, shop.id()); uuid(out, shop.ownerId()); out.writeUTF(shop.ownerName());
                position(out, shop.chest()); position(out, shop.sign()); out.writeLong(shop.revision());
                OwnershipProof proof = shop.proof(); uuid(out, proof.chestIdentity()); nullableUuid(out, proof.recordedPlacer());
                out.writeBoolean(proof.griefPreventionPresent()); nullableString(out, proof.chestClaimId()); nullableString(out, proof.signClaimId());
                out.writeInt(shop.offers().size());
                for (ShopOffer offer : shop.offers()) {
                    uuid(out, offer.id()); out.writeLong(offer.unitPriceCents()); out.writeInt(offer.stockLimit());
                    byte[] item = codec.encode(offer.item());
                    if (item.length < 1 || item.length > 1024 * 1024) throw new IOException("Item is too large");
                    out.writeInt(item.length); out.write(item);
                }
            }
        }
        return bytes.toByteArray();
    }

    private State decode(byte[] bytes) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            Map<BlockPos, Placement> placements = new HashMap<>(); Map<UUID, ShopRecord> shops = new HashMap<>();
            int count = bound(in.readInt(), 1_000_000, "placements");
            for (int i = 0; i < count; i++) if (placements.put(position(in), new Placement(uuid(in), uuid(in))) != null)
                throw new IOException("Duplicate placement");
            count = bound(in.readInt(), 100_000, "shops");
            Set<BlockPos> positions = new HashSet<>();
            for (int i = 0; i < count; i++) {
                UUID id = uuid(in), owner = uuid(in); String name = in.readUTF();
                BlockPos chest = position(in), sign = position(in); long revision = in.readLong();
                OwnershipProof proof = new OwnershipProof(uuid(in), nullableUuid(in), in.readBoolean(), nullableString(in), nullableString(in));
                List<ShopOffer> offers = new ArrayList<>(); Set<UUID> offerIds = new HashSet<>();
                int offersCount = bound(in.readInt(), 27, "offers");
                for (int j = 0; j < offersCount; j++) {
                    UUID offerId = uuid(in); long price = in.readLong(); int cap = in.readInt();
                    int length = bound(in.readInt(), 1024 * 1024, "item length"); byte[] item = in.readNBytes(length);
                    if (item.length != length || !offerIds.add(offerId)) throw new IOException("Invalid offer");
                    ShopOffer offer = new ShopOffer(offerId, codec.decode(item), price, cap);
                    for (ShopOffer previous : offers) if (previous.item().isSimilar(offer.item())) throw new IOException("Duplicate item offer");
                    offers.add(offer);
                }
                ShopRecord shop = new ShopRecord(id, owner, name, chest, sign, revision, offers, proof);
                if (shops.put(id, shop) != null || !positions.add(chest) || !positions.add(sign)) throw new IOException("Duplicate shop position");
                Placement placement = placements.get(chest);
                if (proof.recordedPlacer() != null && (placement == null || !proof.recordedPlacer().equals(owner)
                        || !placement.ownerId.equals(owner) || !placement.identity.equals(proof.chestIdentity())))
                    throw new IOException("Inconsistent placement proof");
                if (proof.recordedPlacer() == null && (!proof.griefPreventionPresent() || proof.chestClaimId() == null))
                    throw new IOException("Shop has no ownership proof");
            }
            if (in.read() != -1) throw new IOException("Trailing registry data");
            return new State(placements, shops);
        }
    }
    private static int bound(int value, int max, String name) throws IOException {
        if (value < 0 || value > max) throw new IOException("Invalid " + name); return value;
    }
    private static byte[] sha256(byte[] bytes) {
        try { return MessageDigest.getInstance("SHA-256").digest(bytes); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static void uuid(DataOutputStream out, UUID id) throws IOException { out.writeLong(id.getMostSignificantBits()); out.writeLong(id.getLeastSignificantBits()); }
    private static UUID uuid(DataInputStream in) throws IOException { return new UUID(in.readLong(), in.readLong()); }
    private static void nullableUuid(DataOutputStream out, UUID id) throws IOException { out.writeBoolean(id != null); if (id != null) uuid(out, id); }
    private static UUID nullableUuid(DataInputStream in) throws IOException { return in.readBoolean() ? uuid(in) : null; }
    private static void nullableString(DataOutputStream out, String value) throws IOException { out.writeBoolean(value != null); if (value != null) out.writeUTF(value); }
    private static String nullableString(DataInputStream in) throws IOException { return in.readBoolean() ? in.readUTF() : null; }
    private static void position(DataOutputStream out, BlockPos pos) throws IOException { uuid(out, pos.worldId()); out.writeInt(pos.x()); out.writeInt(pos.y()); out.writeInt(pos.z()); }
    private static BlockPos position(DataInputStream in) throws IOException { return new BlockPos(uuid(in), in.readInt(), in.readInt(), in.readInt()); }
}
