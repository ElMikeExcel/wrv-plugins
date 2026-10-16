/* SPDX-License-Identifier: MIT */
package com.winkreyes.shops.store;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main-thread-only, fail-closed ownership registry. It never changes inventory contents.
 * The caller must cancel breaks, piston motion, explosions, foreign stock access and
 * attachment of second chests for protected positions. No block is force-loaded here.
 */
public final class ShopRegistry {
    public static final NamespacedKey CHEST_IDENTITY = new NamespacedKey("wrvshops", "chest_identity");
    public static final NamespacedKey SHOP_ID = new NamespacedKey("wrvshops", "shop_id");
    private static final int[][] SIDES = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private static final Pattern PRICE = Pattern.compile("(?i).*(?:[BS].*\\d|\\b[BS]\\b|\\bfree\\b).*");
    private final Server server;
    private final ClaimOwnership claims;
    private final boolean requireGriefPrevention;
    private final RegistryFile file;
    private RegistryFile.State state = new RegistryFile.State(Map.of(), Map.of());
    private final Map<BlockPos, UUID> chests = new HashMap<>(), signs = new HashMap<>();
    private final Map<BlockPos, Long> epochs = new HashMap<>();
    private long nextEpoch;
    private boolean ready;

    public ShopRegistry(JavaPlugin plugin, boolean requireGriefPrevention) {
        this(plugin.getServer(), plugin.getDataFolder().toPath().resolve("shops.dat"), requireGriefPrevention,
                new GriefPreventionOwnership(plugin.getServer()), RegistryFile.BUKKIT);
    }
    ShopRegistry(Server server, Path path, boolean requireGriefPrevention, ClaimOwnership claims, RegistryFile.ItemCodec codec) {
        this.server = server; this.claims = claims; this.requireGriefPrevention = requireGriefPrevention;
        this.file = new RegistryFile(path, codec);
    }
    public void load() throws IOException {
        ready = false;
        RegistryFile.State loaded = file.read();
        install(loaded); ready = true;
    }
    public void save() throws IOException {
        if (!ready) throw new IOException("Registry is not safely loaded");
        file.write(state);
    }
    public boolean healthy() { return ready; }
    public Collection<ShopRecord> all() { return List.copyOf(state.shops().values()); }
    public Optional<ShopRecord> findById(UUID id) { return id == null ? Optional.empty() : Optional.ofNullable(state.shops().get(id)); }
    public Optional<ShopRecord> findByChest(BlockPos position) { return findById(chests.get(position)); }
    public Optional<ShopRecord> findByChest(Block block) { return findByChest(BlockPos.of(block)); }
    public Optional<ShopRecord> findBySign(BlockPos position) { return findById(signs.get(position)); }
    public Optional<ShopRecord> findBySign(Block block) { return findBySign(BlockPos.of(block)); }
    public boolean isProtected(BlockPos position) { return chests.containsKey(position) || signs.containsKey(position); }
    public boolean isProtected(Block block) { return isProtected(BlockPos.of(block)); }
    public Set<BlockPos> protectedPositions() {
        Set<BlockPos> result = new HashSet<>(chests.keySet()); result.addAll(signs.keySet()); return Set.copyOf(result);
    }
    /** Conservative: prevent a second chest in any horizontal neighboring position. */
    public boolean wouldAttachToShop(Block block) {
        BlockPos pos = BlockPos.of(block);
        return chests.containsKey(pos.offset(1,0,0)) || chests.containsKey(pos.offset(-1,0,0))
                || chests.containsKey(pos.offset(0,0,1)) || chests.containsKey(pos.offset(0,0,-1));
    }
    /** Call on successful block placement/break to invalidate outstanding setup snapshots. */
    public void noteBlockChanged(Block block) { epochs.put(BlockPos.of(block), ++nextEpoch); }

    /** Call only after an uncancelled chest placement. No trust permission counts as ownership. */
    public void recordPlaced(Block block, UUID owner) throws RegistryException {
        checkReady(); Objects.requireNonNull(owner); BlockPos pos = BlockPos.of(block);
        if (isProtected(pos)) throw new RegistryException("This block already belongs to a shop.");
        Chest chest = singleChest(block);
        if (tag(chest, SHOP_ID) != null) throw new RegistryException("This chest has an existing shop identity. Ask an administrator to inspect it.");
        UUID identity = UUID.randomUUID();
        Map<BlockPos, RegistryFile.Placement> placements = new HashMap<>(state.placements());
        placements.put(pos, new RegistryFile.Placement(owner, identity));
        RegistryFile.State before = state, after = new RegistryFile.State(placements, state.shops());
        String oldIdentity = tag(chest, CHEST_IDENTITY);
        persist(after);
        try {
            chest.getPersistentDataContainer().set(CHEST_IDENTITY, PersistentDataType.STRING, identity.toString());
            if (!chest.update(false, false)) throw new RegistryException("Could not record chest ownership. Place a new chest and try again.");
            install(after); noteBlockChanged(block);
        } catch (RuntimeException | RegistryException failure) {
            boolean restored;
            try { restoreTag(chest, CHEST_IDENTITY, oldIdentity); restored = chest.update(false, false); }
            catch (RuntimeException ignored) { restored = false; }
            rollback(before, after, failure); if (!restored) ready = false;
            throw new RegistryException("Could not record chest ownership. Place a new chest and try again.", failure);
        }
    }
    /** Forget only unregistered chests; registered positions must be removed through confirmed UI. */
    public void forgetPlaced(Block block) throws RegistryException {
        checkReady(); BlockPos pos = BlockPos.of(block);
        if (isProtected(pos)) throw new RegistryException("Remove this shop through its owner menu first.");
        noteBlockChanged(block);
        if (!state.placements().containsKey(pos)) return;
        Map<BlockPos, RegistryFile.Placement> placements = new HashMap<>(state.placements()); placements.remove(pos);
        RegistryFile.State next = new RegistryFile.State(placements, state.shops()); persist(next); install(next);
    }

    /** Used after fresh placement or an explicit owner setup request on an attached ordinary sign. */
    public SetupProposal propose(Player owner, Block chestBlock, Block signBlock) throws RegistryException {
        checkReady(); checkOwnerAccess(owner, chestBlock);
        BlockPos chestPos = BlockPos.of(chestBlock), signPos = BlockPos.of(signBlock);
        ensureLoaded(chestPos, signPos);
        requireAttachedSign(chestPos, signBlock);
        Chest chest = singleChest(chestBlock); Sign sign = sign(signBlock);
        ShopRecord old = findByChest(chestPos).orElse(null);
        if (old != null && (!old.ownerId().equals(owner.getUniqueId()) || !old.sign().equals(signPos)))
            throw new RegistryException("Only this shop's owner can edit it using its existing sign.");
        if (old != null) validate(old);
        else if (isProtected(signPos) || tag(sign, SHOP_ID) != null || tag(chest, SHOP_ID) != null)
            throw new RegistryException("This sign is already registered to a shop.");
        if (old == null && looksLikeChestShop(sign))
            throw new RegistryException("This looks like an existing ChestShop sign. Place a new chest and sign instead.");
        rejectOtherSigns(chestPos, signPos);
        RegistryFile.Placement placement = state.placements().get(chestPos);
        ClaimPair claimPair = verifyOwner(owner.getUniqueId(), placement, chestBlock, signBlock);
        UUID identity = old == null ? identityForSetup(chest, placement) : old.proof().chestIdentity();
        OwnershipProof proof = new OwnershipProof(identity, placement == null ? null : placement.ownerId(),
                claimPair.chest.available(), claimPair.chest.claimId(), claimPair.sign.claimId());
        if (old != null && !proof.equals(old.proof())) throw new RegistryException("Shop ownership changed. Ask an administrator to review it.");
        return new SetupProposal(old == null ? UUID.randomUUID() : old.id(), owner.getUniqueId(), owner.getName(),
                chestPos, signPos, old == null ? 0 : old.revision(), epoch(chestPos, signPos), proof,
                chest.getBlockInventory().getContents(), signSnapshot(signBlock, sign), tag(chest, CHEST_IDENTITY));
    }

    /** Atomic reviewed change; stock changed since review always requires a new review. */
    public ShopRecord commit(Player owner, SetupProposal proposal, List<ShopOffer> offers) throws RegistryException {
        checkReady();
        if (!owner.getUniqueId().equals(proposal.ownerId())) throw new RegistryException("This review belongs to another player.");
        Block chestBlock = proposal.chest().resolve(server), signBlock = proposal.sign().resolve(server);
        SetupProposal fresh = propose(owner, chestBlock, signBlock);
        if (fresh.revision() != proposal.revision() || (proposal.revision() != 0 && !fresh.shopId().equals(proposal.shopId()))
                || fresh.epoch() != proposal.epoch() || !Objects.equals(fresh.chestTagSnapshot(), proposal.chestTagSnapshot())
                || !fresh.signSnapshot().equals(proposal.signSnapshot())
                || !sameProofBeforeCommit(fresh.proof(), proposal.proof()) || !Arrays.equals(fresh.contents(), proposal.contents()))
            throw new RegistryException("The chest, sign, stock, or ownership changed. Review the shop again before confirming.");
        validateOffers(offers, fresh.contents(), state.shops().get(proposal.shopId()));
        if (proposal.revision() == Long.MAX_VALUE) throw new RegistryException("This shop cannot be revised further.");
        ShopRecord shop = new ShopRecord(proposal.shopId(), owner.getUniqueId(), owner.getName(), proposal.chest(), proposal.sign(),
                proposal.revision() + 1, offers, proposal.proof());
        Map<UUID, ShopRecord> shops = new HashMap<>(state.shops()); shops.put(shop.id(), shop);
        RegistryFile.State before = state, after = new RegistryFile.State(state.placements(), shops);
        Chest chest = singleChest(chestBlock); Sign sign = sign(signBlock);
        SignBackup signBackup = new SignBackup(sign); String oldChestTag = tag(chest, CHEST_IDENTITY);
        String oldChestShopTag = tag(chest, SHOP_ID);
        persist(after); // No block label is touched until durable registry replacement succeeded.
        try {
            chest.getPersistentDataContainer().set(CHEST_IDENTITY, PersistentDataType.STRING, shop.proof().chestIdentity().toString());
            chest.getPersistentDataContainer().set(SHOP_ID, PersistentDataType.STRING, shop.id().toString());
            if (!chest.update(false, false)) throw new RegistryException("The chest changed before it could be registered.");
            sign.getPersistentDataContainer().set(SHOP_ID, PersistentDataType.STRING, shop.id().toString());
            for (Side side : Side.values()) {
                sign.getSide(side).setLine(0, "[Excel Shop]"); sign.getSide(side).setLine(1, shop.ownerName());
                sign.getSide(side).setLine(2, shop.offers().size() + " item type(s)"); sign.getSide(side).setLine(3, "Tap to shop");
            }
            sign.setWaxed(true);
            if (!sign.update(false, false)) throw new RegistryException("The sign changed before it could be registered.");
            install(after);
            // A container may already be open before registration; revoke foreign viewers immediately.
            for (var viewer : List.copyOf(chest.getBlockInventory().getViewers()))
                if (!viewer.getUniqueId().equals(shop.ownerId())) viewer.closeInventory();
            return shop;
        } catch (RuntimeException | RegistryException failure) {
            boolean restored = restoreBlocks(chest, oldChestTag, oldChestShopTag, sign, signBackup);
            rollback(before, after, failure);
            if (!restored) ready = false;
            throw new RegistryException("The shop was not changed because its sign or chest could not be updated. Ask an administrator to check it.", failure);
        }
    }

    /** Recheck immediately before a transaction or owner mutation. Returned inventory is physical. */
    public Chest validate(ShopRecord shop) throws RegistryException {
        checkReady(); ShopRecord current = state.shops().get(shop.id());
        if (current == null || current.revision() != shop.revision()) throw new RegistryException("The shop changed. Open it again.");
        Block chestBlock = current.chest().resolve(server), signBlock = current.sign().resolve(server);
        requireAttachedSign(current.chest(), signBlock);
        Chest chest = singleChest(chestBlock); Sign sign = sign(signBlock);
        if (!current.proof().chestIdentity().toString().equals(tag(chest, CHEST_IDENTITY))
                || !current.id().toString().equals(tag(sign, SHOP_ID))
                || !current.id().toString().equals(tag(chest, SHOP_ID)))
            throw new RegistryException("The shop's chest or sign identity changed. Ask its owner to review it.");
        RegistryFile.Placement placement = state.placements().get(current.chest());
        if (!Objects.equals(current.proof().recordedPlacer(), placement == null ? null : placement.ownerId())
                || (placement != null && !placement.identity().equals(current.proof().chestIdentity())))
            throw new RegistryException("The recorded chest ownership changed. Ask an administrator to review it.");
        ClaimPair pair = verifyOwner(current.ownerId(), placement, chestBlock, signBlock);
        if (pair.chest.available() != current.proof().griefPreventionPresent()
                || !Objects.equals(pair.chest.claimId(), current.proof().chestClaimId())
                || !Objects.equals(pair.sign.claimId(), current.proof().signClaimId()))
            throw new RegistryException("The claim protecting this shop changed. Ask its owner to review it.");
        rejectOtherSigns(current.chest(), current.sign());
        return chest;
    }

    /** Call only from an explicitly confirmed owner removal screen. No stock is moved. */
    public void remove(Player owner, UUID shopId) throws RegistryException {
        checkReady(); ShopRecord shop = state.shops().get(shopId);
        if (shop == null) throw new RegistryException("This shop no longer exists.");
        if (!shop.ownerId().equals(owner.getUniqueId())) throw new RegistryException("Only the shop owner can remove this shop.");
        Block chestBlock = shop.chest().resolve(server); checkOwnerAccess(owner, chestBlock);
        Chest chest = validate(shop); Sign sign = sign(shop.sign().resolve(server)); SignBackup backup = new SignBackup(sign);
        String identity = tag(chest, CHEST_IDENTITY), chestShopId = tag(chest, SHOP_ID);
        Map<UUID, ShopRecord> shops = new HashMap<>(state.shops()); shops.remove(shopId);
        RegistryFile.State before = state, after = new RegistryFile.State(state.placements(), shops); persist(after);
        try {
            chest.getPersistentDataContainer().remove(SHOP_ID);
            if (!chest.update(false, false)) throw new RegistryException("The chest could not be released.");
            sign.getPersistentDataContainer().remove(SHOP_ID); sign.setWaxed(false);
            for (Side side : Side.values()) for (int line = 0; line < 4; line++) sign.getSide(side).setLine(line, "");
            if (!sign.update(false, false)) throw new RegistryException("The sign could not be released.");
            install(after); noteBlockChanged(sign.getBlock());
        } catch (RuntimeException | RegistryException failure) {
            boolean restored = restoreBlocks(chest, identity, chestShopId, sign, backup);
            rollback(before, after, failure); if (!restored) ready = false;
            throw new RegistryException("The shop could not be removed safely. Ask an administrator to check its sign.", failure);
        }
    }

    public static int stock(ItemStack[] contents, ItemStack item) {
        int count = 0;
        for (ItemStack stack : contents) if (stack != null && stack.isSimilar(item)) count = Math.addExact(count, stack.getAmount());
        return count;
    }
    public static boolean looksLikeChestShop(Sign sign) {
        for (Side side : Side.values()) {
            String count = sign.getSide(side).getLine(1).trim(), price = sign.getSide(side).getLine(2).trim();
            if (count.matches("\\d+") && (PRICE.matcher(price).matches() || price.matches(".*\\d.*:.*\\d.*"))) return true;
        }
        return false;
    }
    private void validateOffers(List<ShopOffer> offers, ItemStack[] contents, ShopRecord previousShop) throws RegistryException {
        if (offers == null || offers.isEmpty() || offers.size() > 27) throw new RegistryException("Choose between 1 and 27 stocked item types.");
        Set<UUID> ids = new HashSet<>(); List<ItemStack> types = new ArrayList<>();
        for (ShopOffer offer : offers) {
            if (offer == null || !ids.add(offer.id())) throw new RegistryException("The review contains a duplicate or missing item.");
            ItemStack item = offer.item();
            for (ItemStack previous : types) if (previous.isSimilar(item)) throw new RegistryException("Each exact item type can only be listed once.");
            boolean existingExactOffer = previousShop != null && previousShop.offers().stream()
                    .anyMatch(previous -> previous.id().equals(offer.id()) && previous.item().isSimilar(item));
            if (stock(contents, item) < 1 && !existingExactOffer) throw new RegistryException("A new listed item is no longer in the chest. Review the stock again.");
            types.add(item);
        }
        for (ItemStack physical : contents) {
            if (physical != null && !ShopOffer.air(physical.getType()) && physical.getAmount() > 0
                    && types.stream().noneMatch(item -> item.isSimilar(physical)))
                throw new RegistryException("Set a price for every exact item type in the chest before confirming.");
        }
    }
    private record ClaimPair(ClaimOwnership.ClaimStatus chest, ClaimOwnership.ClaimStatus sign) {}
    private ClaimPair verifyOwner(UUID owner, RegistryFile.Placement placement, Block chest, Block sign) throws RegistryException {
        if (placement != null && !placement.ownerId().equals(owner))
            throw new RegistryException("This chest was placed by another player. Place a new chest you own.");
        ClaimOwnership.ClaimStatus chestClaim = claims.at(chest.getLocation()), signClaim = claims.at(sign.getLocation());
        if (chestClaim.available() != signClaim.available() || (requireGriefPrevention && !chestClaim.available()))
            throw new RegistryException("GriefPrevention is unavailable. Shop changes and purchases are paused until ownership can be checked.");
        if (requireGriefPrevention && (chestClaim.claimId() == null || signClaim.claimId() == null))
            throw new RegistryException("Create this shop inside your own GriefPrevention claim, with both its chest and sign protected.");
        if ((chestClaim.claimId() != null && !owner.equals(chestClaim.ownerId()))
                || (signClaim.claimId() != null && !owner.equals(signClaim.ownerId())))
            throw new RegistryException("You must own the actual claim at both the chest and sign. Build or container trust is not ownership.");
        if (placement == null && (chestClaim.claimId() == null || !owner.equals(chestClaim.ownerId())))
            throw new RegistryException("The owner of this chest is unknown. Place a new chest yourself, then attach a sign.");
        return new ClaimPair(chestClaim, signClaim);
    }
    private UUID identityForSetup(Chest chest, RegistryFile.Placement placement) throws RegistryException {
        String identity = tag(chest, CHEST_IDENTITY);
        if (placement != null) {
            if (!placement.identity().toString().equals(identity)) throw new RegistryException("This chest's placement record does not match. Place a new chest yourself.");
            return placement.identity();
        }
        if (identity == null) return UUID.randomUUID();
        try { return UUID.fromString(identity); }
        catch (IllegalArgumentException failure) { throw new RegistryException("The chest identity is invalid. Ask an administrator to inspect it."); }
    }
    private static boolean sameProofBeforeCommit(OwnershipProof a, OwnershipProof b) {
        // A claim-owned legacy chest has no tag until commit, so the fresh proposal issues another provisional UUID.
        return Objects.equals(a.recordedPlacer(), b.recordedPlacer()) && a.griefPreventionPresent() == b.griefPreventionPresent()
                && Objects.equals(a.chestClaimId(), b.chestClaimId()) && Objects.equals(a.signClaimId(), b.signClaimId())
                && (a.recordedPlacer() == null || a.chestIdentity().equals(b.chestIdentity()));
    }
    private void rejectOtherSigns(BlockPos chest, BlockPos target) throws RegistryException {
        for (int[] delta : SIDES) {
            BlockPos neighbor = chest.offset(delta[0], delta[1], delta[2]);
            if (neighbor.equals(target)) continue;
            Block block = neighbor.resolve(server);
            if (block.getState() instanceof Sign)
                throw new RegistryException("Another sign is attached or adjacent to this chest. Use a separate chest to avoid changing an existing shop.");
        }
    }
    private void ensureLoaded(BlockPos... positions) throws RegistryException { for (BlockPos pos : positions) pos.resolve(server); }
    private static Chest singleChest(Block block) throws RegistryException {
        if ((block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) || !(block.getState() instanceof Chest chest))
            throw new RegistryException("Use a normal or trapped single chest for this shop.");
        if (!(block.getBlockData() instanceof org.bukkit.block.data.type.Chest data)
                || data.getType() != org.bukkit.block.data.type.Chest.Type.SINGLE)
            throw new RegistryException("Double chests are not supported yet. Use a separate single chest.");
        return chest;
    }
    private static void requireAttachedSign(BlockPos chest, Block sign) throws RegistryException {
        BlockPos position = BlockPos.of(sign), support = null;
        if (sign.getBlockData() instanceof org.bukkit.block.data.type.WallSign wall) {
            var face = wall.getFacing().getOppositeFace();
            support = position.offset(face.getModX(), face.getModY(), face.getModZ());
        } else if (sign.getBlockData() instanceof org.bukkit.block.data.type.Sign) {
            support = position.offset(0, -1, 0);
        }
        if (!chest.equals(support))
            throw new RegistryException("Attach the wall sign directly to this chest, or place a standing sign on top of it.");
    }
    private static Sign sign(Block block) throws RegistryException {
        if (!(block.getState() instanceof Sign sign)) throw new RegistryException("The shop sign is missing. Place a sign directly on the chest.");
        return sign;
    }
    private long epoch(BlockPos chest, BlockPos sign) { return Math.max(epochs.getOrDefault(chest, 0L), epochs.getOrDefault(sign, 0L)); }
    private static String signSnapshot(Block block, Sign sign) {
        StringBuilder text = new StringBuilder(block.getBlockData().getAsString());
        text.append('\0').append(tag(sign, SHOP_ID)).append('\0').append(sign.isWaxed());
        for (Side side : Side.values()) for (String line : sign.getSide(side).getLines()) text.append('\0').append(line);
        return text.toString();
    }
    private void checkOwnerAccess(Player player, Block chest) throws RegistryException {
        if (!player.isOnline() || !player.hasPermission("wrvshops.create"))
            throw new RegistryException("You no longer have permission to create or manage shops.");
        if (!player.getWorld().getUID().equals(chest.getWorld().getUID())
                || player.getLocation().distanceSquared(chest.getLocation().add(0.5, 0.5, 0.5)) > 36.0)
            throw new RegistryException("Stand within 6 blocks of your shop to manage it.");
    }
    private void checkReady() throws RegistryException {
        if (!ready) throw new RegistryException("Shop storage is unavailable. Ask an administrator to check the server log.");
        if (!server.isPrimaryThread()) throw new RegistryException("Shop operations must run on the server thread.");
    }
    private void persist(RegistryFile.State next) throws RegistryException {
        try { file.write(next); }
        catch (IOException failure) { throw new RegistryException("Shop data could not be saved. Nothing was changed; ask an administrator to check storage.", failure); }
    }
    private void rollback(RegistryFile.State before, RegistryFile.State candidate, Exception failure) {
        try { file.write(before); install(before); }
        catch (IOException rollbackFailure) {
            failure.addSuppressed(rollbackFailure); ready = false;
            Map<UUID, ShopRecord> protectedShops = new HashMap<>(candidate.shops()); protectedShops.putAll(before.shops());
            install(new RegistryFile.State(before.placements(), protectedShops));
        }
    }
    private void install(RegistryFile.State state) {
        this.state = state; chests.clear(); signs.clear();
        for (ShopRecord shop : state.shops().values()) { chests.put(shop.chest(), shop.id()); signs.put(shop.sign(), shop.id()); }
    }
    private static String tag(org.bukkit.block.TileState state, NamespacedKey key) { return state.getPersistentDataContainer().get(key, PersistentDataType.STRING); }
    private static void restoreTag(org.bukkit.block.TileState state, NamespacedKey key, String previous) {
        if (previous == null) state.getPersistentDataContainer().remove(key);
        else state.getPersistentDataContainer().set(key, PersistentDataType.STRING, previous);
    }
    private static boolean restoreBlocks(Chest chest, String identity, String shopId, Sign sign, SignBackup backup) {
        boolean chestRestored, signRestored;
        try { restoreTag(chest, CHEST_IDENTITY, identity); restoreTag(chest, SHOP_ID, shopId); chestRestored = chest.update(false, false); } catch (RuntimeException failure) { chestRestored = false; }
        try { backup.restore(sign); signRestored = sign.update(false, false); } catch (RuntimeException failure) { signRestored = false; }
        return chestRestored && signRestored;
    }
    private static final class SignBackup {
        final String[][] lines; final String shopId; final boolean waxed;
        SignBackup(Sign sign) {
            lines = new String[Side.values().length][];
            for (Side side : Side.values()) lines[side.ordinal()] = sign.getSide(side).getLines().clone();
            shopId = tag(sign, SHOP_ID); waxed = sign.isWaxed();
        }
        void restore(Sign sign) {
            restoreTag(sign, SHOP_ID, shopId); sign.setWaxed(waxed);
            for (Side side : Side.values()) for (int i = 0; i < 4; i++) sign.getSide(side).setLine(i, lines[side.ordinal()][i]);
        }
    }
}
