/* SPDX-License-Identifier: MIT */
package com.winkreyes.shops.store;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.persistence.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class ShopRegistryTest {
    @TempDir Path directory;
    Server server; World world; Player owner; UUID ownerId, worldId;
    Block chestBlock, signBlock; Chest chest; Sign sign; Inventory inventory;
    AtomicReference<ItemStack[]> contents;
    AtomicReference<ClaimOwnership.ClaimStatus> claim;
    Map<String, Block> blocks;
    ShopRegistry registry;
    Path file;
    final RegistryFile.ItemCodec codec = new RegistryFile.ItemCodec() {
        public byte[] encode(ItemStack item) {
            TestItem value = (TestItem) item;
            return (value.type.name() + "|" + value.metadata + "|" + value.amount).getBytes(StandardCharsets.UTF_8);
        }
        public ItemStack decode(byte[] bytes) {
            String[] parts = new String(bytes, StandardCharsets.UTF_8).split("\\|");
            return new TestItem(Material.valueOf(parts[0]), parts[1], Integer.parseInt(parts[2]));
        }
    };
    @BeforeEach void setUp() throws Exception {
        server = mock(Server.class); world = mock(World.class); owner = mock(Player.class);
        ownerId = UUID.randomUUID(); worldId = UUID.randomUUID(); blocks = new HashMap<>();
        when(server.isPrimaryThread()).thenReturn(true); when(server.getWorld(worldId)).thenReturn(world);
        when(world.getUID()).thenReturn(worldId); when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(world.getMinHeight()).thenReturn(-64); when(world.getMaxHeight()).thenReturn(320);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(a -> block(a.getArgument(0), a.getArgument(1), a.getArgument(2)));
        when(owner.getUniqueId()).thenReturn(ownerId); when(owner.getName()).thenReturn("Owner");
        when(owner.isOnline()).thenReturn(true); when(owner.hasPermission("wrvshops.create")).thenReturn(true);
        when(owner.getWorld()).thenReturn(world); when(owner.getLocation()).thenReturn(new Location(world,5,64,5));
        chestBlock = block(5,64,5); signBlock = block(5,64,4); chest = mock(Chest.class); sign = mock(Sign.class);
        when(chestBlock.getType()).thenReturn(Material.CHEST); when(chestBlock.getState()).thenReturn(chest);
        org.bukkit.block.data.type.Chest chestData = mock(org.bukkit.block.data.type.Chest.class);
        when(chestData.getType()).thenReturn(org.bukkit.block.data.type.Chest.Type.SINGLE); when(chestData.getAsString()).thenReturn("single");
        when(chestBlock.getBlockData()).thenReturn(chestData); when(chest.getBlock()).thenReturn(chestBlock);
        org.bukkit.block.data.type.WallSign wall = mock(org.bukkit.block.data.type.WallSign.class);
        when(wall.getFacing()).thenReturn(BlockFace.NORTH); when(wall.getAsString()).thenReturn("oak_wall_sign[north]");
        when(signBlock.getBlockData()).thenReturn(wall);
        when(signBlock.getType()).thenReturn(Material.OAK_WALL_SIGN); when(signBlock.getState()).thenReturn(sign); when(sign.getBlock()).thenReturn(signBlock);
        PersistentDataContainer chestPdc = pdc(), signPdc = pdc();
        when(chest.getPersistentDataContainer()).thenReturn(chestPdc); when(sign.getPersistentDataContainer()).thenReturn(signPdc);
        when(chest.update(false,false)).thenReturn(true); when(sign.update(false,false)).thenReturn(true);
        for (Side side : Side.values()) { SignSide signSide = side(); when(sign.getSide(side)).thenReturn(signSide); }
        inventory = mock(Inventory.class); contents = new AtomicReference<>(new ItemStack[] {new TestItem(Material.DIAMOND,"ordinary",10), null});
        when(chest.getBlockInventory()).thenReturn(inventory); when(inventory.getContents()).thenAnswer(a -> SetupProposal.copy(contents.get()));
        claim = new AtomicReference<>(ClaimOwnership.ClaimStatus.unavailable());
        file = directory.resolve("shops.dat"); registry = registry(false); registry.load();
    }
    ShopRegistry registry(boolean required) { return new ShopRegistry(server, file, required, location -> claim.get(), codec); }
    Block block(int x, int y, int z) {
        return blocks.computeIfAbsent(x + "," + y + "," + z, key -> {
            Block block = mock(Block.class); when(block.getWorld()).thenReturn(world);
            when(block.getX()).thenReturn(x); when(block.getY()).thenReturn(y); when(block.getZ()).thenReturn(z);
            when(block.getLocation()).thenAnswer(a -> new Location(world,x,y,z)); when(block.getType()).thenReturn(Material.AIR);
            when(block.getState()).thenReturn(mock(BlockState.class)); BlockData data = mock(BlockData.class);
            when(data.getAsString()).thenReturn("minecraft:oak_wall_sign[facing=north]"); when(block.getBlockData()).thenReturn(data);
            return block;
        });
    }
    PersistentDataContainer pdc() {
        PersistentDataContainer container = mock(PersistentDataContainer.class); Map<NamespacedKey,String> values = new HashMap<>();
        when(container.get(any(NamespacedKey.class), eq(PersistentDataType.STRING))).thenAnswer(a -> values.get(a.getArgument(0)));
        doAnswer(a -> { values.put(a.getArgument(0), a.getArgument(2)); return null; }).when(container).set(any(),eq(PersistentDataType.STRING),anyString());
        doAnswer(a -> { values.remove(a.getArgument(0)); return null; }).when(container).remove(any()); return container;
    }
    SignSide side() {
        SignSide side = mock(SignSide.class); String[] lines = {"My supplies", "", "", ""};
        when(side.getLine(anyInt())).thenAnswer(a -> lines[a.getArgument(0)]);
        when(side.getLines()).thenAnswer(a -> lines.clone());
        doAnswer(a -> { lines[a.getArgument(0)] = a.getArgument(1); return null; }).when(side).setLine(anyInt(),anyString());
        return side;
    }
    ShopOffer offer() { return new ShopOffer(UUID.randomUUID(), contents.get()[0], 123, 2304); }
    ShopRecord create() throws Exception {
        registry.recordPlaced(chestBlock, ownerId);
        return registry.commit(owner, registry.propose(owner,chestBlock,signBlock), List.of(offer()));
    }
    void ownsClaim() { claim.set(new ClaimOwnership.ClaimStatus(true,"42",ownerId)); }

    @Test void createdShopRoundTripsExactMetadataAndNeverMutatesStock() throws Exception {
        ShopRecord created = create(); registry.save();
        ShopRegistry loaded = registry(false); loaded.load(); ShopRecord record = loaded.findById(created.id()).orElseThrow();
        assertEquals(created.id(),record.id()); assertEquals(123,record.offers().getFirst().unitPriceCents());
        assertEquals("ordinary",((TestItem)record.offers().getFirst().item()).metadata); assertEquals(chest,loaded.validate(record));
        assertEquals(10,contents.get()[0].getAmount()); assertTrue(loaded.isProtected(signBlock));
        assertTrue(loaded.wouldAttachToShop(block(6,64,5))); assertFalse(loaded.wouldAttachToShop(block(8,64,5)));
        verify(inventory,never()).setContents(any()); verify(inventory,never()).setItem(anyInt(),any());
    }
    @Test void unknownUnclaimedChestRefusesAndSuggestsNewChest() {
        claim.set(ClaimOwnership.ClaimStatus.wilderness());
        assertTrue(assertThrows(RegistryException.class,()->registry.propose(owner,chestBlock,signBlock)).getMessage().contains("Place a new chest"));
    }
    @Test void actualClaimOwnerMayRegisterPreexistingChest() throws Exception {
        ownsClaim(); SetupProposal proposal = registry.propose(owner,chestBlock,signBlock);
        assertFalse(Files.exists(file)); assertNull(chest.getPersistentDataContainer().get(ShopRegistry.CHEST_IDENTITY,PersistentDataType.STRING));
        ShopRecord shop = registry.commit(owner,proposal,List.of(offer())); assertNull(shop.proof().recordedPlacer());
        assertEquals(chest,registry.validate(shop));
    }
    @Test void recordedOtherPlacerTakesPriorityOverClaimOwnership() throws Exception {
        registry.recordPlaced(chestBlock,UUID.randomUUID()); ownsClaim();
        assertTrue(assertThrows(RegistryException.class,()->registry.propose(owner,chestBlock,signBlock)).getMessage().contains("another player"));
    }
    @Test void containerTrustAndBuildTrustCannotClaimOtherOwnersChest() throws Exception {
        registry.recordPlaced(chestBlock,ownerId); claim.set(new ClaimOwnership.ClaimStatus(true,"42",UUID.randomUUID()));
        assertTrue(assertThrows(RegistryException.class,()->registry.propose(owner,chestBlock,signBlock)).getMessage().contains("trust is not ownership"));
    }
    @Test void administrativeClaimHasNoPlayerOwner() throws Exception {
        registry.recordPlaced(chestBlock,ownerId); claim.set(new ClaimOwnership.ClaimStatus(true,"42",null));
        assertThrows(RegistryException.class,()->registry.propose(owner,chestBlock,signBlock));
    }
    @Test void missingRequiredGriefPreventionFailsClosedEvenForRecordedPlacer() throws Exception {
        registry.recordPlaced(chestBlock,ownerId); ShopRegistry required = registry(true); required.load();
        assertTrue(assertThrows(RegistryException.class,()->required.propose(owner,chestBlock,signBlock)).getMessage().contains("unavailable"));
    }
    @Test void griefPreventionApiFailureNeverFallsBackToPlacement() throws Exception {
        registry.recordPlaced(chestBlock,ownerId);
        ShopRegistry failure = new ShopRegistry(server,file,false, location -> { throw new RegistryException("GP failed"); },codec); failure.load();
        assertEquals("GP failed",assertThrows(RegistryException.class,()->failure.propose(owner,chestBlock,signBlock)).getMessage());
    }
    @Test void claimTransferOrClaimReplacementDisablesTransactions() throws Exception {
        ownsClaim(); ShopRecord shop = create();
        claim.set(new ClaimOwnership.ClaimStatus(true,"42",UUID.randomUUID())); assertThrows(RegistryException.class,()->registry.validate(shop));
        claim.set(new ClaimOwnership.ClaimStatus(true,"43",ownerId)); assertThrows(RegistryException.class,()->registry.validate(shop));
        claim.set(ClaimOwnership.ClaimStatus.unavailable()); assertThrows(RegistryException.class,()->registry.validate(shop));
    }
    @Test void claimAtSignMustAlsoBelongToOwner() throws Exception {
        registry.recordPlaced(chestBlock,ownerId);
        ShopRegistry boundary = new ShopRegistry(server,file,false, location -> new ClaimOwnership.ClaimStatus(true,"42",location.getBlockZ()==4?UUID.randomUUID():ownerId),codec);
        boundary.load(); assertThrows(RegistryException.class,()->boundary.propose(owner,chestBlock,signBlock));
    }
    @Test void doubleChestRefused() {
        when(((org.bukkit.block.data.type.Chest)chestBlock.getBlockData()).getType()).thenReturn(org.bukkit.block.data.type.Chest.Type.LEFT);
        assertTrue(assertThrows(RegistryException.class,()->registry.recordPlaced(chestBlock,ownerId)).getMessage().contains("Double chests"));
    }
    @Test void adjacentOrdinarySignConservativelyRefusesChestShopConflict() throws Exception {
        registry.recordPlaced(chestBlock,ownerId); when(block(6,64,5).getState()).thenReturn(mock(Sign.class));
        assertThrows(RegistryException.class,()->registry.propose(owner,chestBlock,signBlock));
    }
    @Test void targetChestShopSignIsNeverAdopted() throws Exception {
        registry.recordPlaced(chestBlock,ownerId); sign.getSide(Side.FRONT).setLine(1,"64"); sign.getSide(Side.FRONT).setLine(2,"B 10 : S 5");
        assertTrue(assertThrows(RegistryException.class,()->registry.propose(owner,chestBlock,signBlock)).getMessage().contains("ChestShop"));
        assertNull(sign.getPersistentDataContainer().get(ShopRegistry.SHOP_ID,PersistentDataType.STRING));
    }
    @Test void changedStockCountOrMetadataRequiresNewReview() throws Exception {
        registry.recordPlaced(chestBlock,ownerId); SetupProposal proposal = registry.propose(owner,chestBlock,signBlock); ShopOffer offer = offer();
        contents.set(new ItemStack[]{new TestItem(Material.DIAMOND,"ordinary",9),null});
        assertThrows(RegistryException.class,()->registry.commit(owner,proposal,List.of(offer)));
        contents.set(new ItemStack[]{new TestItem(Material.DIAMOND,"renamed",10),null});
        assertThrows(RegistryException.class,()->registry.commit(owner,proposal,List.of(offer))); assertTrue(registry.all().isEmpty());
    }
    @Test void replacedBlockOrEditedSignRequiresNewReview() throws Exception {
        registry.recordPlaced(chestBlock,ownerId); SetupProposal proposal = registry.propose(owner,chestBlock,signBlock);
        registry.noteBlockChanged(signBlock); assertThrows(RegistryException.class,()->registry.commit(owner,proposal,List.of(offer())));
        SetupProposal next = registry.propose(owner,chestBlock,signBlock); sign.getSide(Side.BACK).setLine(0,"edited");
        assertThrows(RegistryException.class,()->registry.commit(owner,next,List.of(offer())));
    }
    @Test void changedPdcIdentityDisablesTransactions() throws Exception {
        ShopRecord shop = create(); chest.getPersistentDataContainer().set(ShopRegistry.CHEST_IDENTITY,PersistentDataType.STRING,UUID.randomUUID().toString());
        assertThrows(RegistryException.class,()->registry.validate(shop));
    }
    @Test void ownerEditIncrementsRevisionAndOldReviewIsStale() throws Exception {
        ShopRecord shop = create(); SetupProposal proposal = registry.propose(owner,chestBlock,signBlock);
        ShopRecord updated = registry.commit(owner,proposal,List.of(new ShopOffer(shop.offers().getFirst().id(),contents.get()[0],200,2304)));
        assertEquals(2,updated.revision()); assertEquals(shop.id(),updated.id()); assertThrows(RegistryException.class,()->registry.validate(shop));
        assertThrows(RegistryException.class,()->registry.commit(owner,proposal,List.of(offer())));
    }
    @Test void cannotListUnknownOrDuplicateExactMetadata() throws Exception {
        registry.recordPlaced(chestBlock,ownerId); SetupProposal proposal = registry.propose(owner,chestBlock,signBlock);
        ShopOffer absent = new ShopOffer(UUID.randomUUID(),new TestItem(Material.DIAMOND,"different",1),10,2304);
        assertThrows(RegistryException.class,()->registry.commit(owner,proposal,List.of(absent)));
        assertThrows(RegistryException.class,()->registry.commit(owner,proposal,List.of(offer(),offer())));
    }
    @Test void signFailureRollsBackDurableRegistrationAndOriginalText() throws Exception {
        registry.recordPlaced(chestBlock,ownerId); SetupProposal proposal = registry.propose(owner,chestBlock,signBlock);
        when(sign.update(false,false)).thenReturn(false,true);
        assertThrows(RegistryException.class,()->registry.commit(owner,proposal,List.of(offer())));
        assertEquals("My supplies",sign.getSide(Side.FRONT).getLine(0)); assertTrue(registry.all().isEmpty()); assertTrue(registry.healthy());
        ShopRegistry reload = registry(false); reload.load(); assertTrue(reload.all().isEmpty());
    }
    @Test void saveFailureDoesNotTouchLabelOrInventory() throws Exception {
        ownsClaim(); SetupProposal proposal = registry.propose(owner,chestBlock,signBlock); Files.createDirectory(file);
        assertThrows(RegistryException.class,()->registry.commit(owner,proposal,List.of(offer())));
        assertEquals("My supplies",sign.getSide(Side.FRONT).getLine(0)); verify(sign,never()).update(anyBoolean(),anyBoolean());
    }
    @Test void corruptFileIsNeverSilentlyReplacedWithEmptyRegistry() throws Exception {
        create(); byte[] bytes = Files.readAllBytes(file); bytes[bytes.length-1] ^= 1; Files.write(file,bytes);
        ShopRegistry broken = registry(false); assertThrows(IOException.class,broken::load); assertFalse(broken.healthy());
        assertThrows(IOException.class,broken::save); assertArrayEquals(bytes,Files.readAllBytes(file));
    }
    @Test void unloadedChunkIsRefusedWithoutForceLoading() throws Exception {
        ShopRecord shop = create(); when(world.isChunkLoaded(anyInt(),anyInt())).thenReturn(false);
        assertThrows(RegistryException.class,()->registry.validate(shop)); verify(world,never()).loadChunk(anyInt(),anyInt());
    }
    @Test void removalIsOwnerOnlyAndRestoresOrdinarySignWithoutTouchingStock() throws Exception {
        ShopRecord shop = create(); Player other = mock(Player.class); when(other.getUniqueId()).thenReturn(UUID.randomUUID());
        assertThrows(RegistryException.class,()->registry.remove(other,shop.id())); registry.remove(owner,shop.id());
        assertTrue(registry.all().isEmpty()); assertFalse(registry.isProtected(chestBlock));
        assertNull(sign.getPersistentDataContainer().get(ShopRegistry.SHOP_ID,PersistentDataType.STRING));
        assertEquals("",sign.getSide(Side.FRONT).getLine(0)); assertEquals(10,contents.get()[0].getAmount());
        ShopRegistry reload = registry(false); reload.load(); assertTrue(reload.all().isEmpty());
    }
    @Test void inputAndReturnedItemSnapshotsAreDefensiveCopies() throws Exception {
        registry.recordPlaced(chestBlock,ownerId); SetupProposal proposal = registry.propose(owner,chestBlock,signBlock);
        proposal.contents()[0].setAmount(99); assertEquals(10,proposal.contents()[0].getAmount());
        ShopOffer offer = offer(); offer.item().setAmount(99); assertEquals(1,offer.item().getAmount());
    }
    @Test void emptyRegistryLookupIsSafe() { assertTrue(registry.findByChest(chestBlock).isEmpty()); assertTrue(registry.findBySign(signBlock).isEmpty()); }
    @Test void registryRejectsOffThreadMutation() throws Exception {
        when(server.isPrimaryThread()).thenReturn(false); assertThrows(RegistryException.class,()->registry.recordPlaced(chestBlock,ownerId));
    }
    @Test void requiredProtectionRefusesWildernessEvenForRecordedOwner() throws Exception {
        registry.recordPlaced(chestBlock,ownerId); claim.set(ClaimOwnership.ClaimStatus.wilderness());
        ShopRegistry required = registry(true); required.load();
        assertTrue(assertThrows(RegistryException.class,()->required.propose(owner,chestBlock,signBlock)).getMessage().contains("inside your own"));
    }
    @Test void everyStockedMetadataVariantMustHavePrice() throws Exception {
        contents.set(new ItemStack[]{new TestItem(Material.DIAMOND,"ordinary",10),new TestItem(Material.DIAMOND,"renamed",5)});
        registry.recordPlaced(chestBlock,ownerId); SetupProposal proposal = registry.propose(owner,chestBlock,signBlock);
        assertTrue(assertThrows(RegistryException.class,()->registry.commit(owner,proposal,List.of(offer()))).getMessage().contains("every exact item type"));
    }
    @Test void preservedSoldOutOfferCanBeEditedButNotReplacedWithNewId() throws Exception {
        ShopRecord shop = create(); ShopOffer old = shop.offers().getFirst(); contents.set(new ItemStack[]{null,null});
        SetupProposal proposal = registry.propose(owner,chestBlock,signBlock);
        ShopOffer repriced = new ShopOffer(old.id(),old.item(),234,2304);
        ShopRecord updated = registry.commit(owner,proposal,List.of(repriced)); assertEquals(234,updated.offers().getFirst().unitPriceCents());
        SetupProposal next = registry.propose(owner,chestBlock,signBlock);
        assertThrows(RegistryException.class,()->registry.commit(owner,next,List.of(new ShopOffer(UUID.randomUUID(),old.item(),234,2304))));
    }
    @Test void registrationClosesForeignExistingChestViewers() throws Exception {
        org.bukkit.entity.HumanEntity foreign = mock(org.bukkit.entity.HumanEntity.class);
        when(foreign.getUniqueId()).thenReturn(UUID.randomUUID()); when(inventory.getViewers()).thenReturn(List.of(owner,foreign));
        create(); verify(foreign).closeInventory(); verify(owner,never()).closeInventory();
    }
    @Test void permissionRevocationAfterReviewRefusesCommit() throws Exception {
        registry.recordPlaced(chestBlock,ownerId); SetupProposal proposal = registry.propose(owner,chestBlock,signBlock);
        when(owner.hasPermission("wrvshops.create")).thenReturn(false);
        assertThrows(RegistryException.class,()->registry.commit(owner,proposal,List.of(offer()))); assertTrue(registry.all().isEmpty());
    }
    @Test void offlineOrDistantOwnerCannotCreateEditOrRemove() throws Exception {
        ShopRecord shop = create(); when(owner.isOnline()).thenReturn(false);
        assertThrows(RegistryException.class,()->registry.propose(owner,chestBlock,signBlock));
        when(owner.isOnline()).thenReturn(true); when(owner.getLocation()).thenReturn(new Location(world,100,64,100));
        assertThrows(RegistryException.class,()->registry.propose(owner,chestBlock,signBlock));
        assertThrows(RegistryException.class,()->registry.remove(owner,shop.id()));
    }
    @Test void freeChestShopPriceIsNeverAdopted() throws Exception {
        registry.recordPlaced(chestBlock,ownerId); sign.getSide(Side.FRONT).setLine(1,"64");
        for (String price : List.of("B free", "S free", "B free : S free", "free")) {
            sign.getSide(Side.FRONT).setLine(2,price);
            assertTrue(assertThrows(RegistryException.class,()->registry.propose(owner,chestBlock,signBlock)).getMessage().contains("ChestShop"));
        }
        assertNull(sign.getPersistentDataContainer().get(ShopRegistry.SHOP_ID,PersistentDataType.STRING));
    }
    @Test void adjacentWallSignFacingOtherSupportIsRefused() throws Exception {
        registry.recordPlaced(chestBlock,ownerId);
        when(((org.bukkit.block.data.type.WallSign)signBlock.getBlockData()).getFacing()).thenReturn(BlockFace.SOUTH);
        assertTrue(assertThrows(RegistryException.class,()->registry.propose(owner,chestBlock,signBlock)).getMessage().contains("Attach the wall sign"));
    }
    @Test void standingSignMustBeDirectlyAboveChest() throws Exception {
        registry.recordPlaced(chestBlock,ownerId);
        org.bukkit.block.data.type.Sign standing = mock(org.bukkit.block.data.type.Sign.class);
        when(standing.getAsString()).thenReturn("oak_sign[rotation=0]"); when(signBlock.getBlockData()).thenReturn(standing);
        assertThrows(RegistryException.class,()->registry.propose(owner,chestBlock,signBlock));
        Block above = block(5,65,5); when(above.getBlockData()).thenReturn(standing); when(above.getState()).thenReturn(sign);
        when(signBlock.getState()).thenReturn(mock(BlockState.class));
        assertEquals(BlockPos.of(above),registry.propose(owner,chestBlock,above).sign());
    }
    @Test void rotatingRegisteredSignDisablesPurchaseAndCommit() throws Exception {
        ShopRecord shop = create(); SetupProposal review = registry.propose(owner,chestBlock,signBlock);
        when(((org.bukkit.block.data.type.WallSign)signBlock.getBlockData()).getFacing()).thenReturn(BlockFace.SOUTH);
        assertThrows(RegistryException.class,()->registry.validate(shop));
        assertThrows(RegistryException.class,()->registry.commit(owner,review,List.of(offer())));
    }
    /** Avoids global Bukkit server state; integration harness separately exercises real CraftItemStack metadata. */
    static final class TestItem extends ItemStack {
        final Material type; final String metadata; int amount;
        TestItem(Material type,String metadata,int amount) { super(); this.type=type;this.metadata=metadata;this.amount=amount; }
        @Override public Material getType() { return type; }
        @Override public int getAmount() { return amount; }
        @Override public void setAmount(int amount) { this.amount=amount; }
        @Override public TestItem clone() { return new TestItem(type,metadata,amount); }
        @Override public boolean isSimilar(ItemStack item) { return item instanceof TestItem other && type==other.type && metadata.equals(other.metadata); }
        @Override public boolean equals(Object item) { return item instanceof TestItem other && amount==other.amount && isSimilar(other); }
        @Override public int hashCode() { return Objects.hash(type,metadata,amount); }
        @Override public String toString() { return type+":"+metadata+":"+amount; }
    }
}
