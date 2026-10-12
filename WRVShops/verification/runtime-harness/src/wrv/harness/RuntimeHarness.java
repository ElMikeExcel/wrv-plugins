package wrv.harness;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import com.winkreyes.shops.InventoryPlan;
import com.winkreyes.shops.ShopAdapter;
import com.winkreyes.shops.ShopUi;
import com.winkreyes.shops.ShopUi.ViewKind;
import com.winkreyes.shops.store.*;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Claim;
import com.winkreyes.shops.checkout.CheckoutService;
import com.winkreyes.shops.checkout.FileJournal;
import java.time.Clock;
import java.time.Duration;
import com.winkreyes.shops.checkout.CheckoutService.*;
import com.winkreyes.shops.checkout.Money;

/** Runtime-only fixture. Never deploy: creates fake players and shuts its server down. */
public final class RuntimeHarness extends JavaPlugin {
    private final List<String> results = new ArrayList<>();
    private final Deque<Case> cases = new ArrayDeque<>();
    private int assertions, passed, failed;
    private Plugin shops;
    private FakeVault vault;
    private World world;
    private FakePlayer owner, buyer, secondBuyer;
    private Physical uiFixture;
    private ItemStack uiItem;
    private InventoryView staleConfirmView;

    @Override public void onEnable() { Bukkit.getScheduler().runTaskLater(this, this::start, 20L); }
    private void start() {
        shops = Bukkit.getPluginManager().getPlugin(System.getProperty("wrv.test.plugin-name", "ExcelShops"));
        vault = (FakeVault) Bukkit.getPluginManager().getPlugin("Vault");
        world = Bukkit.getWorlds().getFirst();
        owner = fake(1, "TestOwner"); buyer = fake(2, "TestBuyer"); secondBuyer = fake(3, "TestBuyerTwo");
        if (System.getProperty("wrv.test.migration-mode") != null) {
            cases.add(new Case("legacy-migration-" + System.getProperty("wrv.test.migration-mode"), this::migrationSafety)); next(); return;
        }
        if (Boolean.getBoolean("wrv.test.no-economy")) {
            cases.add(new Case("provider-absent-startup-keeps-stock-protected", this::protectionOnlyStartup)); next(); return;
        }
        cases.add(new Case("exact-runtime-and-plugin-enable", () -> {
            check(Bukkit.getVersion().equals("26.2-123-5001879 (MC: 26.2)"), Bukkit.getVersion());
            check(Bukkit.getBukkitVersion().equals("26.2.build.123-stable"), Bukkit.getBukkitVersion());
            check(System.getProperty("java.runtime.version").equals("25.0.4+7-LTS"), System.getProperty("java.runtime.version"));
            check(shops != null && shops.isEnabled(), "shop plugin enabled with registered fake Economy");
            check(vault != null && vault.isEnabled(), "disposable fake Vault loaded");
            check(shops.getDataFolder().getName().equals(System.getProperty("wrv.test.plugin-name", "ExcelShops")), "data folder matches declared plugin identity");
            if (shops.getName().equals("ExcelShops")) {
                check(Bukkit.getPluginCommand("excelshop").getPlugin() == shops, "ExcelShops primary command belongs to renamed plugin");
                check(Bukkit.getPluginCommand("wrvshop").getPlugin() == shops, "legacy command alias remains available");
            }
        }));
        cases.add(new Case("real-item-metadata-and-storage-plan", this::metadataPlan));
        cases.add(new Case("install-synthetic-player-lookup", this::installLookup));
        addShopCases();
        next();
    }
    private void migrationSafety() throws Exception {
        String mode = System.getProperty("wrv.test.migration-mode");
        boolean startupBlocked = mode.equals("conflict") || mode.equals("missing-journal");
        check(shops != null && shops.isEnabled(), "renamed plugin stays enabled to protect stock");
        check(shops.getName().equals("ExcelShops"), "new runtime identity is ExcelShops");
        check(Bukkit.getPluginManager().getPlugin("WRVShops") == null, "legacy plugin is not loaded alongside the new plugin");
        if (startupBlocked) {
            check(checkout() == null && ui() == null, "unsafe migration source or target stops checkout and setup initialization");
        } else {
            check(checkout() != null && checkout().recoveryRequired(), "legacy dirty session requires recovery after migration and restart");
            check(Files.exists(shops.getDataFolder().toPath().resolve("checkout/active-session")), "recovery marker remains present");
            check(!registry().all().isEmpty(), "legacy registry remains loaded while recovery-locked");
        }
        loadFixtureChunks(80);
        Chest chest = (Chest) world.getBlockAt(80, 5, 0).getState();
        Block sign = world.getBlockAt(80, 5, 1);
        ItemStack[] before = chest.getBlockInventory().getContents();
        for (Block block : List.of(chest.getBlock(), sign)) {
            check(((TileState) block.getState()).getPersistentDataContainer().getKeys().stream().anyMatch(key -> key.getNamespace().equals("wrvshops")), "legacy wrvshops PDC tags remain present");
            BlockBreakEvent broken = new BlockBreakEvent(block, buyer.player); Bukkit.getPluginManager().callEvent(broken); check(broken.isCancelled(), "tagged legacy block break blocked while migration/recovery paused");
        }
        for (FakePlayer player : List.of(owner, buyer)) {
            InventoryOpenEvent open = new InventoryOpenEvent(player.view(chest.getBlockInventory())); Bukkit.getPluginManager().callEvent(open); check(open.isCancelled(), "owner and buyer physical access blocked while paused");
        }
        ItemStack sample = Arrays.stream(before).filter(Objects::nonNull).findFirst().orElseThrow();
        InventoryMoveItemEvent move = new InventoryMoveItemEvent(chest.getBlockInventory(), sample, Bukkit.createInventory(null, 9), true); Bukkit.getPluginManager().callEvent(move); check(move.isCancelled(), "hopper extraction blocked while paused");
        PlayerInteractEvent interact = new PlayerInteractEvent(buyer.player, Action.RIGHT_CLICK_BLOCK, null, chest.getBlock(), BlockFace.SOUTH, EquipmentSlot.HAND); Bukkit.getPluginManager().callEvent(interact); check(interact.isCancelled(), "interaction cannot expose paused physical stock");
        check(Bukkit.getPluginCommand("excelshop").execute(buyer.player, "excelshop", new String[0]), "renamed command remains registered");
        check(buyer.messages.stream().anyMatch(message -> message.contains("paused")), "command clearly reports paused migration/recovery state");
        check(vault.calls.isEmpty(), "migration/recovery probe makes no economy calls");
        check(Arrays.equals(before, chest.getBlockInventory().getContents()), "migration/recovery leaves original stock untouched");
        check(Arrays.stream(before).filter(Objects::nonNull).mapToInt(ItemStack::getAmount).sum() == 7, "all seven legacy fixture items retained");
        getLogger().info("WRV_SHOPS_HARNESS_MIGRATION_" + mode.toUpperCase(Locale.ROOT).replace('-', '_') + " verified");
    }
    private void protectionOnlyStartup() throws Exception {
        check(shops != null && shops.isEnabled(), "ExcelShops remains loaded to protect chests without economy service");
        check(checkout() == null && ui() == null, "checkout and setup remain unavailable");
        loadFixtureChunks(80);
        ShopRecord shop = registry().findByChest(world.getBlockAt(80, 5, 0)).orElseThrow();
        Chest chest = (Chest) shop.chest().resolve(Bukkit.getServer()).getState();
        InventoryOpenEvent open = new InventoryOpenEvent(buyer.view(chest.getBlockInventory())); Bukkit.getPluginManager().callEvent(open); check(open.isCancelled(), "existing physical chest cannot be looted without economy");
        BlockBreakEvent broken = new BlockBreakEvent(chest.getBlock(), buyer.player); Bukkit.getPluginManager().callEvent(broken); check(broken.isCancelled(), "existing chest break remains blocked without economy");
        InventoryMoveItemEvent move = new InventoryMoveItemEvent(chest.getBlockInventory(), shop.offers().getFirst().item(), Bukkit.createInventory(null, 9), true); Bukkit.getPluginManager().callEvent(move); check(move.isCancelled(), "hoppers remain blocked without economy");
        PlayerInteractEvent interact = new PlayerInteractEvent(buyer.player, Action.RIGHT_CLICK_BLOCK, null, chest.getBlock(), BlockFace.SOUTH, EquipmentSlot.HAND); Bukkit.getPluginManager().callEvent(interact); check(interact.isCancelled(), "right-click does not leak physical storage in protection-only mode");
        check(InventoryPlan.count(chest.getBlockInventory().getContents(), shop.offers().getFirst().item()) == 7, "protection-only boot leaves persisted stock untouched");
        getLogger().info("WRV_SHOPS_HARNESS_PROTECTION_ONLY verified");
    }
    private void addShopCases() {
        cases.add(new Case("registry-clean-restart-and-physical-persistence", this::restartRegistry));
        cases.add(new Case("registry-owner-claim-placement-and-review-snapshots", this::registryOwnership));
        cases.add(new Case("chest-final-confirm-stock-price-owner-and-concurrency", this::chestCheckout));
        cases.add(new Case("chest-payout-decline-rolls-back-and-refunds", this::chestRefund));
        cases.add(new Case("spawn-quote-cancel-exact-bundle-and-replay", this::spawnCheckout));
        cases.add(new Case("spawn-capacity-funds-permission-and-disconnect", this::spawnRejections));
        cases.add(new Case("spawn-reentrant-confirm-and-changed-inventory-refund", this::spawnReentrant));
        cases.add(new Case("uncertain-provider-and-refund-failures-persist-recovery", this::recoveryFailures));
        addUiCases();
    }
    private ShopUi ui() throws Exception { return (ShopUi) invoke(shops, "getUi"); }
    private void expectKind(FakePlayer player, ViewKind expected) throws Exception { check(ui().currentKind(player.uuid) == expected, "expected UI stage " + expected + " but was " + ui().currentKind(player.uuid)); }
    private InventoryClickEvent click(FakePlayer player, int slot) {
        if (player.view == null) throw new AssertionError("No open inventory for " + player.name);
        InventoryClickEvent event = new InventoryClickEvent(player.view, InventoryType.SlotType.CONTAINER, slot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        Bukkit.getPluginManager().callEvent(event); check(event.isCancelled(), "menu click never transfers icon or inventory items"); return event;
    }
    private void beforePublished() throws Exception { check(registry().findByChest(uiFixture.chest).isEmpty(), "shop remains ordinary storage before final publish"); check(vault.calls.isEmpty(), "setup never calls economy"); }
    private void beforePurchase() { check(vault.calls.isEmpty() && buyer.count(uiItem) == 0, "catalog/quantity/review/confirmation preview does not buy anything"); }
    private void signPlacementAndEdit() {
        BlockPlaceEvent placed = new BlockPlaceEvent(uiFixture.sign, uiFixture.sign.getState(), uiFixture.chest, new ItemStack(Material.OAK_SIGN), owner.player, true, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(placed);
        check(!placed.isCancelled(), "fresh attached sign placement allowed");
        SignChangeEvent edited = new SignChangeEvent(uiFixture.sign, owner.player, new String[]{"Any ordinary sign text", "", "", ""});
        Bukkit.getPluginManager().callEvent(edited); check(!edited.isCancelled(), "ordinary sign text requires no command syntax");
        Sign vanillaApplied = (Sign) uiFixture.sign.getState(); for (int i = 0; i < 4; i++) vanillaApplied.setLine(i, edited.getLine(i)); vanillaApplied.update(false, false);
    }
    private void addUiCases() {
        cases.add(new Case("sign-place-edit-triggers-onboarding", () -> {
            resetBalances(); uiFixture = physical(40); uiItem = named(Material.GOLD_INGOT, "UI listed gold");
            ItemMeta original = uiItem.getItemMeta(); original.lore(List.of(Component.text("Original item lore"))); original.addEnchant(Enchantment.UNBREAKING, 3, true); original.getPersistentDataContainer().set(new NamespacedKey(this, "ui_identity"), PersistentDataType.STRING, "exact"); uiItem.setItemMeta(original);
            stock(uiFixture, uiItem, 6); ItemStack variant = ItemStack.deserializeBytes(uiItem.serializeAsBytes()); variant.setAmount(6); chest(uiFixture).getBlockInventory().setItem(1, variant); nearby(owner, uiFixture); nearby(buyer, uiFixture);
            BlockPlaceEvent placed = new BlockPlaceEvent(uiFixture.chest, uiFixture.chest.getState(), uiFixture.chest.getRelative(BlockFace.DOWN), new ItemStack(Material.CHEST), owner.player, true, EquipmentSlot.HAND);
            Bukkit.getPluginManager().callEvent(placed); check(!placed.isCancelled(), "ordinary owned chest placement allowed");
            signPlacementAndEdit(); beforePublished();
        }));
        cases.add(new Case("onboarding-no-keeps-ordinary-storage", () -> { expectKind(owner, ViewKind.SETUP_PROMPT); beforePublished(); click(owner, 15); }));
        cases.add(new Case("onboarding-can-restart-with-fresh-sign", () -> { expectKind(owner, null); beforePublished(); signPlacementAndEdit(); }));
        cases.add(new Case("onboarding-yes-shows-real-chest-items", () -> { expectKind(owner, ViewKind.SETUP_PROMPT); click(owner, 11); beforePublished(); }));
        cases.add(new Case("setup-select-item-without-publishing", () -> { expectKind(owner, ViewKind.SETUP_ITEMS); check(owner.view.getTopInventory().getItem(0).getType() == Material.GOLD_INGOT, "actual chest contents become visual choices"); check(owner.view.getTopInventory().getItem(1) == null, "equivalent original and roundtripped metadata share one price entry"); click(owner, 0); beforePublished(); }));
        cases.add(new Case("setup-enter-unit-price-without-publishing", () -> { expectKind(owner, ViewKind.SETUP_PRICE); check(ui().enterNumber(owner.player, "2.50"), "price number input accepted"); expectKind(owner, ViewKind.SETUP_ITEMS); beforePublished(); click(owner, 31); }));
        cases.add(new Case("setup-review-shows-price-before-confirm", () -> { expectKind(owner, ViewKind.SETUP_REVIEW); beforePublished(); check(owner.view.getTopInventory().getItem(0).getItemMeta().getLore().stream().anyMatch(line -> line.contains("2.50")), "review displays selected exact unit price"); click(owner, 49); }));
        cases.add(new Case("setup-final-confirm-is-only-publish-action", () -> { expectKind(owner, ViewKind.SETUP_CONFIRM); beforePublished(); click(owner, 49); click(owner, 49); }));
        cases.add(new Case("setup-duplicate-publish-and-protection-listeners", () -> {
            ShopRecord shop = registry().findByChest(uiFixture.chest).orElseThrow();
            check(shop.revision() == 1 && shop.offers().size() == 1 && shop.offers().getFirst().unitPriceCents() == 250, "two final click events create exactly one listing");
            check(vault.calls.isEmpty(), "publishing uses no money"); protectionListeners(shop);
            ui().showManage(owner.player, shop); expectKind(owner, ViewKind.MANAGE); click(owner, 13); owner.x += 100;
            PlayerInteractEvent event = new PlayerInteractEvent(buyer.player, Action.RIGHT_CLICK_BLOCK, null, uiFixture.chest, BlockFace.SOUTH, EquipmentSlot.HAND);
            event.setCancelled(true); Bukkit.getPluginManager().callEvent(event);
            check(event.isCancelled() && event.useInteractedBlock() == Event.Result.DENY, "buyer interaction never opens physical chest even if another protection denied interaction");
        }));
        cases.add(new Case("buyer-catalog-read-only-drag-shift-numberkey", () -> {
            expectKind(owner, null); check(owner.view == null, "owner stock button rechecks proximity before opening physical inventory"); nearby(owner, uiFixture);
            expectKind(buyer, ViewKind.CATALOG); beforePurchase();
            check(buyer.view.getTopInventory().getItem(0).getItemMeta().getLore().stream().anyMatch(line -> line.contains("Original item lore")), "catalog preview preserves original item lore");
            for (ClickType type : List.of(ClickType.SHIFT_LEFT, ClickType.NUMBER_KEY, ClickType.DOUBLE_CLICK, ClickType.DROP)) {
                InventoryClickEvent event = new InventoryClickEvent(buyer.view, InventoryType.SlotType.CONTAINER, 0, type, InventoryAction.MOVE_TO_OTHER_INVENTORY, 1);
                Bukkit.getPluginManager().callEvent(event); check(event.isCancelled(), type + " cannot extract catalog icons");
            }
            InventoryDragEvent drag = new InventoryDragEvent(buyer.view, new ItemStack(Material.DIRT), new ItemStack(Material.DIRT, 2), false, Map.of(0, new ItemStack(Material.DIRT)));
            Bukkit.getPluginManager().callEvent(drag); check(drag.isCancelled(), "drag into menu blocked"); click(buyer, 0); beforePurchase();
        }));
        cases.add(new Case("buyer-quantity-preview-no-purchase", () -> { expectKind(buyer, ViewKind.QUANTITY); beforePurchase(); check(ui().enterNumber(buyer.player, "3"), "typed quantity accepted"); expectKind(buyer, ViewKind.BUY_REVIEW); beforePurchase(); click(buyer, 11); }));
        cases.add(new Case("buyer-final-screen-cancel-no-purchase", () -> { expectKind(buyer, ViewKind.BUY_CONFIRM); beforePurchase(); staleConfirmView = buyer.view; click(buyer, 15); }));
        cases.add(new Case("buyer-stale-confirm-after-cancel-cannot-buy", () -> {
            expectKind(buyer, null); beforePurchase(); InventoryClickEvent stale = new InventoryClickEvent(staleConfirmView, InventoryType.SlotType.CONTAINER, 11, ClickType.LEFT, InventoryAction.PICKUP_ALL);
            Bukkit.getPluginManager().callEvent(stale); check(stale.isCancelled(), "stale confirm event cancelled");
            ui().showShop(buyer.player, registry().findByChest(uiFixture.chest).orElseThrow()); click(buyer, 0);
        }));
        cases.add(new Case("buyer-review-second-attempt", () -> { expectKind(buyer, ViewKind.QUANTITY); beforePurchase(); ui().enterNumber(buyer.player, "3"); click(buyer, 11); }));
        cases.add(new Case("buyer-queued-back-invalidates-queued-confirm", () -> { expectKind(buyer, ViewKind.BUY_CONFIRM); beforePurchase(); click(buyer, 13); click(buyer, 11); }));
        cases.add(new Case("buyer-back-before-confirm-remains-unpaid", () -> { expectKind(buyer, ViewKind.QUANTITY); beforePurchase(); ui().enterNumber(buyer.player, "3"); click(buyer, 11); }));
        cases.add(new Case("buyer-close-before-queued-confirm-cancels", () -> { expectKind(buyer, ViewKind.BUY_CONFIRM); beforePurchase(); click(buyer, 11); buyer.player.closeInventory(); expectKind(buyer, null); }));
        cases.add(new Case("buyer-close-prevents-delayed-checkout", () -> { beforePurchase(); ui().showShop(buyer.player, registry().findByChest(uiFixture.chest).orElseThrow()); click(buyer, 0); }));
        cases.add(new Case("buyer-review-after-close", () -> { expectKind(buyer, ViewKind.QUANTITY); beforePurchase(); ui().enterNumber(buyer.player, "3"); click(buyer, 11); }));
        cases.add(new Case("buyer-explicit-final-confirm-duplicate-click", () -> { expectKind(buyer, ViewKind.BUY_CONFIRM); beforePurchase(); click(buyer, 11); click(buyer, 11); }));
        cases.add(new Case("buyer-exact-purchase-after-final-confirm-only", () -> {
            check(buyer.count(uiItem) == 3 && InventoryPlan.count(chest(uiFixture).getBlockInventory().getContents(), uiItem) == 9, "final confirmation delivered exactly three physical items");
            check(vault.balances.get(buyer.uuid) == 9992.5 && vault.balances.get(owner.uuid) == 10007.5 && vault.calls.size() == 2, "duplicate button events trigger one exact $7.50 transfer");
            ui().showShop(buyer.player, registry().findByChest(uiFixture.chest).orElseThrow()); click(buyer, 0);
        }));
        cases.add(new Case("buyer-prepare-disconnect-confirm", () -> { expectKind(buyer, ViewKind.QUANTITY); ui().enterNumber(buyer.player, "1"); click(buyer, 11); }));
        cases.add(new Case("buyer-disconnect-invalidates-current-review", () -> {
            expectKind(buyer, ViewKind.BUY_CONFIRM); InventoryView old = buyer.view; buyer.online = false;
            Bukkit.getPluginManager().callEvent(new PlayerQuitEvent(buyer.player, Component.empty()));
            InventoryClickEvent stale = new InventoryClickEvent(old, InventoryType.SlotType.CONTAINER, 11, ClickType.LEFT, InventoryAction.PICKUP_ALL); Bukkit.getPluginManager().callEvent(stale);
            check(stale.isCancelled() && ui().currentKind(buyer.uuid) == null, "disconnect removes page token");
        }));
        cases.add(new Case("buyer-disconnect-no-late-purchase-and-cleanup", () -> {
            check(vault.calls.size() == 2 && buyer.count(uiItem) == 3, "late disconnect click makes no extra purchase"); buyer.online = true;
            registry().remove(owner.player, registry().findByChest(uiFixture.chest).orElseThrow().id());
            check(InventoryPlan.count(chest(uiFixture).getBlockInventory().getContents(), uiItem) == 9, "closing shop retains remaining physical stock");
        }));
    }
    private void protectionListeners(ShopRecord shop) {
        Inventory real = chest(uiFixture).getBlockInventory();
        InventoryOpenEvent foreign = new InventoryOpenEvent(buyer.view(real)); Bukkit.getPluginManager().callEvent(foreign); check(foreign.isCancelled(), "non-owner physical open blocked");
        InventoryOpenEvent own = new InventoryOpenEvent(owner.view(real)); Bukkit.getPluginManager().callEvent(own); check(!own.isCancelled(), "validated owner may open own physical stock");
        Inventory other = Bukkit.createInventory(null, 9);
        InventoryMoveItemEvent take = new InventoryMoveItemEvent(real, uiItem, other, true); Bukkit.getPluginManager().callEvent(take); check(take.isCancelled(), "hopper extraction blocked");
        InventoryMoveItemEvent put = new InventoryMoveItemEvent(other, uiItem, real, true); Bukkit.getPluginManager().callEvent(put); check(put.isCancelled(), "hopper injection blocked");
        for (Block protectedBlock : List.of(uiFixture.chest, uiFixture.sign)) {
            BlockBreakEvent broken = new BlockBreakEvent(protectedBlock, buyer.player); Bukkit.getPluginManager().callEvent(broken); check(broken.isCancelled(), "foreign break blocked for " + protectedBlock.getType());
            BlockBreakEvent ownBreak = new BlockBreakEvent(protectedBlock, owner.player); Bukkit.getPluginManager().callEvent(ownBreak); check(ownBreak.isCancelled(), "owner must confirm shop closure before break");
            BlockBurnEvent burn = new BlockBurnEvent(protectedBlock); Bukkit.getPluginManager().callEvent(burn); check(burn.isCancelled(), "fire destruction blocked");
        }
        Block harmless = world.getBlockAt(41, 5, 2); harmless.setType(Material.DIRT, false);
        BlockExplodeEvent explosion = new BlockExplodeEvent(harmless, harmless.getState(), new ArrayList<>(List.of(uiFixture.chest, uiFixture.sign, harmless)), 1, ExplosionResult.DESTROY);
        Bukkit.getPluginManager().callEvent(explosion); check(!explosion.blockList().contains(uiFixture.chest) && !explosion.blockList().contains(uiFixture.sign) && explosion.blockList().contains(harmless), "explosion protects only registered shop blocks");
        BlockPistonExtendEvent piston = new BlockPistonExtendEvent(harmless, List.of(uiFixture.chest), BlockFace.NORTH); Bukkit.getPluginManager().callEvent(piston); check(piston.isCancelled(), "piston movement blocked");
        SignChangeEvent change = new SignChangeEvent(uiFixture.sign, buyer.player, new String[]{"stolen", "", "", ""}); Bukkit.getPluginManager().callEvent(change); check(change.isCancelled(), "protected shop sign cannot be edited");
        Block beside = uiFixture.chest.getRelative(BlockFace.EAST); beside.setType(Material.CHEST, false);
        BlockPlaceEvent secondChest = new BlockPlaceEvent(beside, beside.getState(), uiFixture.chest, new ItemStack(Material.CHEST), owner.player, true, EquipmentSlot.HAND); Bukkit.getPluginManager().callEvent(secondChest);
        check(secondChest.isCancelled(), "adjoining second chest cannot bypass protected single storage"); beside.setType(Material.AIR, false);
    }
    private ShopRegistry registry() throws Exception { return (ShopRegistry) invoke(shops, "getRegistry"); }
    private GriefPrevention gp() { return (GriefPrevention) Bukkit.getPluginManager().getPlugin("GriefPrevention"); }
    private record Physical(Block chest, Block sign) {}
    private void loadFixtureChunks(int x) { for (int dx : new int[]{-1, 0, 1}) for (int z : new int[]{-1, 0, 1}) { Chunk chunk = world.getChunkAt((x + dx) >> 4, z >> 4); chunk.load(true); chunk.addPluginChunkTicket(this); } }
    private Physical physical(int x) throws Exception {
        loadFixtureChunks(x);
        Block chest = world.getBlockAt(x, 5, 0), sign = world.getBlockAt(x, 5, 1);
        owner.x = x + .5; owner.y = 5; owner.z = 2.5;
        Optional<ShopRecord> previous = registry().findByChest(chest);
        if (previous.isPresent()) registry().remove(owner.player, previous.get().id());
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) world.getBlockAt(x + dx, 5, dz).setType(Material.AIR, false);
        chest.setType(Material.CHEST, false);
        sign.setType(Material.OAK_WALL_SIGN, false);
        org.bukkit.block.data.type.WallSign data = (org.bukkit.block.data.type.WallSign) sign.getBlockData(); data.setFacing(BlockFace.SOUTH); sign.setBlockData(data, false);
        return new Physical(chest, sign);
    }
    private Chest chest(Physical fixture) { return (Chest) fixture.chest.getState(); }
    private void stock(Physical fixture, ItemStack sample, int count) { ItemStack stack = sample.clone(); stack.setAmount(count); chest(fixture).getBlockInventory().clear(); chest(fixture).getBlockInventory().setItem(0, stack); }
    private ShopRecord create(Physical fixture, ItemStack sample, int count, long price) throws Exception {
        registry().recordPlaced(fixture.chest, owner.uuid); stock(fixture, sample, count);
        SetupProposal proposal = registry().propose(owner.player, fixture.chest, fixture.sign);
        return registry().commit(owner.player, proposal, List.of(new ShopOffer(UUID.randomUUID(), sample, price, count)));
    }
    private void nearby(FakePlayer fixture, Physical location) { fixture.x = location.chest.getX() + .5; fixture.y = 5; fixture.z = 2.5; }
    private void restartRegistry() throws Exception {
        Path file = getDataFolder().toPath().resolve("persistent-shop.properties");
        loadFixtureChunks(80);
        if (Files.exists(file)) {
            Properties state = new Properties(); try (var input = Files.newInputStream(file)) { state.load(input); }
            UUID id = UUID.fromString(state.getProperty("shop-id"));
            ShopRecord restored = registry().findById(id).orElseThrow();
            check(restored.chest().x() == 80, "shop registry restored before fixture writes");
            for (Block block : List.of(restored.chest().resolve(Bukkit.getServer()), restored.sign().resolve(Bukkit.getServer()))) {
                check(((TileState) block.getState()).getPersistentDataContainer().getKeys().stream().anyMatch(key -> key.getNamespace().equals("wrvshops")), "original wrvshops PDC namespace retained after rename/restart");
            }
            Chest persisted = registry().validate(restored);
            check(InventoryPlan.count(persisted.getBlockInventory().getContents(), restored.offers().getFirst().item()) == 7, "physical stock persists across full server restart");
            check(restored.offers().getFirst().item().isSimilar(named(Material.AMETHYST_SHARD, "Persisted shop sample")), "item metadata survives registry load");
            check(!checkout().recoveryRequired(), "clean lifecycle clears checkout dirty state");
            getLogger().info("WRV_SHOPS_HARNESS_RESTART verified shop=" + id);
        } else {
            Physical fixture = physical(80); ItemStack sample = named(Material.AMETHYST_SHARD, "Persisted shop sample");
            ShopRecord persisted = create(fixture, sample, 7, 123);
            Properties state = new Properties(); state.setProperty("shop-id", persisted.id().toString());
            Files.createDirectories(file.getParent()); try (var output = Files.newOutputStream(file)) { state.store(output, "Disposable exact-runtime persistence fixture"); }
            check(registry().validate(persisted) != null, "first boot prepares owned physical shop");
            getLogger().info("WRV_SHOPS_HARNESS_RESTART prepared shop=" + persisted.id());
        }
    }
    private void registryOwnership() throws Exception {
        resetBalances(); GriefPrevention.DataStore claims = gp().dataStore;
        claims.defaultClaim = new Claim(owner.uuid, 100); claims.claims.clear();
        Physical fixture = physical(5); ItemStack sample = named(Material.DIAMOND, "Owner stock"); stock(fixture, sample, 10);
        registry().recordPlaced(fixture.chest, owner.uuid);
        SetupProposal reviewed = registry().propose(owner.player, fixture.chest, fixture.sign);
        check(registry().findByChest(fixture.chest).isEmpty(), "onboarding proposal does not create a shop");
        check(!((Sign) fixture.sign.getState()).isWaxed(), "review does not relabel or wax sign");
        ItemStack[] copy = reviewed.contents(); copy[0].setAmount(1);
        check(reviewed.contents()[0].getAmount() == 10 && chest(fixture).getBlockInventory().getItem(0).getAmount() == 10, "setup snapshot is defensive and read-only");
        List<ShopOffer> offers = List.of(new ShopOffer(UUID.randomUUID(), sample, 123, 10));
        stock(fixture, sample, 9);
        rejects(() -> registry().commit(owner.player, reviewed, offers), "stock changed after review blocks creation");
        stock(fixture, sample, 10);
        SetupProposal changedSign = registry().propose(owner.player, fixture.chest, fixture.sign);
        Sign edited = (Sign) fixture.sign.getState(); edited.setLine(0, "Changed"); edited.update(false, false);
        rejects(() -> registry().commit(owner.player, changedSign, offers), "sign edit after review blocks creation");
        claims.defaultClaim.owner = secondBuyer.uuid;
        rejects(() -> registry().propose(owner.player, fixture.chest, fixture.sign), "container/build trust does not replace actual claim ownership");
        claims.defaultClaim.owner = null;
        rejects(() -> registry().propose(owner.player, fixture.chest, fixture.sign), "admin claim is not player ownership");
        claims.defaultClaim.owner = owner.uuid;
        rejects(() -> registry().propose(buyer.player, fixture.chest, fixture.sign), "recorded chest placer mismatch refused");
        claims.failLookup = true;
        rejects(() -> registry().propose(owner.player, fixture.chest, fixture.sign), "claim API failure fails closed"); claims.failLookup = false;
        Bukkit.getPluginManager().disablePlugin(gp());
        rejects(() -> registry().propose(owner.player, fixture.chest, fixture.sign), "required claim plugin unavailable fails closed");
        Bukkit.getPluginManager().enablePlugin(gp());
        SetupProposal revoked = registry().propose(owner.player, fixture.chest, fixture.sign);
        owner.denied.add("wrvshops.create"); rejects(() -> registry().commit(owner.player, revoked, offers), "create permission revoked after review blocks final publish"); owner.denied.clear();
        owner.x += 100; rejects(() -> registry().commit(owner.player, revoked, offers), "walking away after review blocks final publish"); nearby(owner, fixture);
        claims.claims.put("5,5,1", new Claim(secondBuyer.uuid, 200));
        rejects(() -> registry().propose(owner.player, fixture.chest, fixture.sign), "both sign and chest require actual claim ownership"); claims.claims.clear();
        ShopRecord created = registry().commit(owner.player, registry().propose(owner.player, fixture.chest, fixture.sign), offers);
        check(registry().validate(created) != null && ((Sign) fixture.sign.getState()).isWaxed(), "final creation activates protected shop and labels sign");
        check(InventoryPlan.count(chest(fixture).getBlockInventory().getContents(), sample) == 10, "creating shop leaves physical stock unchanged");
        rejects(() -> registry().remove(buyer.player, created.id()), "non-owner removal rejected");
        registry().remove(owner.player, created.id());
        check(!registry().isProtected(fixture.chest) && InventoryPlan.count(chest(fixture).getBlockInventory().getContents(), sample) == 10, "owner removal releases shop without taking items");
        Physical legacy = physical(10); stock(legacy, sample, 1);
        claims.defaultClaim = null;
        rejects(() -> registry().propose(owner.player, legacy.chest, legacy.sign), "unrecorded wilderness chest cannot be claimed");
        registry().recordPlaced(legacy.chest, owner.uuid);
        rejects(() -> registry().propose(owner.player, legacy.chest, legacy.sign), "production configuration requires owned claim even for recorded wilderness chest");
        claims.defaultClaim = new Claim(owner.uuid, 100);
        Physical other = physical(15); stock(other, sample, 1); registry().recordPlaced(other.chest, buyer.uuid);
        rejects(() -> registry().propose(owner.player, other.chest, other.sign), "claim owner cannot take another recorded player's chest");
        Physical doubleChest = physical(20); org.bukkit.block.data.type.Chest data = (org.bukkit.block.data.type.Chest) doubleChest.chest.getBlockData();
        data.setType(org.bukkit.block.data.type.Chest.Type.LEFT); doubleChest.chest.setBlockData(data, false);
        rejects(() -> registry().propose(owner.player, doubleChest.chest, doubleChest.sign), "double chest refused safely");
        doubleChest.chest.setType(Material.AIR, false); doubleChest.sign.setType(Material.AIR, false);
        Physical unattached = physical(50); stock(unattached, sample, 2); registry().recordPlaced(unattached.chest, owner.uuid);
        org.bukkit.block.data.type.WallSign reverse = (org.bukkit.block.data.type.WallSign) unattached.sign.getBlockData(); reverse.setFacing(BlockFace.NORTH); unattached.sign.setBlockData(reverse, false);
        rejects(() -> registry().propose(owner.player, unattached.chest, unattached.sign), "adjacent but unattached sign cannot claim chest");
        Physical legacyFree = physical(55); stock(legacyFree, sample, 2); registry().recordPlaced(legacyFree.chest, owner.uuid);
        Sign legacySign = (Sign) legacyFree.sign.getState(); legacySign.setLine(0, "ExistingOwner"); legacySign.setLine(1, "16"); legacySign.setLine(2, "B free"); legacySign.setLine(3, "DIAMOND"); legacySign.update(false, false);
        rejects(() -> registry().propose(owner.player, legacyFree.chest, legacyFree.sign), "legacy free-price ChestShop sign is never silently converted");
    }
    private void chestCheckout() throws Exception {
        resetBalances(); Physical fixture = physical(25); ItemStack item = named(Material.DIAMOND, "Precisely listed diamonds");
        ShopRecord shop = create(fixture, item, 8, 123); nearby(buyer, fixture); nearby(secondBuyer, fixture);
        ShopAdapter.Selection listing = adapter().chest(shop, shop.offers().getFirst()); Quote quote = quote(buyer, listing, 3);
        check(quote.amountMinorUnits() == 369 && vault.calls.isEmpty(), "review computes $1.23 times 3 exactly without charge");
        check(checkout().confirm(quote.id(), buyer.uuid).status() == Status.SUCCESS, "confirmed chest purchase succeeds");
        check(Math.abs(vault.balances.get(buyer.uuid) - 9996.31) < 0.000001 && Math.abs(vault.balances.get(owner.uuid) - 10003.69) < 0.000001, "exact total moves buyer to seller");
        check(buyer.count(item) == 3 && InventoryPlan.count(chest(fixture).getBlockInventory().getContents(), item) == 5, "physical stock decreases exactly as buyer inventory increases");
        check(buyer.storage.getItem(0).isSimilar(item), "purchase preserves exact item metadata");
        noCharge(quote, buyer, "duplicate chest confirmation rejected");
        Quote first = quote(buyer, adapter().chest(shop, shop.offers().getFirst()), 4);
        Quote second = quote(secondBuyer, adapter().chest(shop, shop.offers().getFirst()), 4);
        check(checkout().confirm(first.id(), buyer.uuid).status() == Status.SUCCESS, "first competing buyer gets available stock");
        noCharge(second, secondBuyer, "second competing review rejects changed stock");
        check(InventoryPlan.count(chest(fixture).getBlockInventory().getContents(), item) == 1, "competing reviews never oversell");
        Quote changedStock = quote(secondBuyer, adapter().chest(shop, shop.offers().getFirst()), 1);
        stock(fixture, item, 2); noCharge(changedStock, secondBuyer, "even added stock invalidates previous reviewed snapshot");
        Quote changedOwner = quote(secondBuyer, adapter().chest(shop, shop.offers().getFirst()), 1);
        gp().dataStore.defaultClaim.owner = buyer.uuid;
        noCharge(changedOwner, secondBuyer, "claim ownership transfer invalidates purchase"); gp().dataStore.defaultClaim.owner = owner.uuid;
        Quote changedPrice = quote(secondBuyer, adapter().chest(shop, shop.offers().getFirst()), 1);
        ShopOffer old = shop.offers().getFirst();
        ShopRecord updated = registry().commit(owner.player, registry().propose(owner.player, fixture.chest, fixture.sign), List.of(new ShopOffer(old.id(), item, 456, 8)));
        noCharge(changedPrice, secondBuyer, "price/revision change invalidates reviewed purchase");
        check(adapter().chest(updated, updated.offers().getFirst()).price() == 456, "fresh review sees new price");
        Quote away = quote(secondBuyer, adapter().chest(updated, updated.offers().getFirst()), 1); secondBuyer.x += 50;
        noCharge(away, secondBuyer, "buyer walking away invalidates purchase");
        registry().remove(owner.player, shop.id());
    }
    private void chestRefund() throws Exception {
        resetBalances(); Physical fixture = physical(30); ItemStack item = named(Material.EMERALD, "Refund fixture"); ShopRecord shop = create(fixture, item, 6, 250);
        nearby(buyer, fixture); Quote quote = quote(buyer, adapter().chest(shop, shop.offers().getFirst()), 2);
        vault.failDeposit = owner.uuid;
        check(checkout().confirm(quote.id(), buyer.uuid).status() == Status.REFUNDED, "definitive seller payout decline rolls back then refunds");
        check(vault.balances.get(buyer.uuid) == 10000 && vault.balances.get(owner.uuid) == 10000, "refund preserves both original balances");
        check(buyer.count(item) == 0 && InventoryPlan.count(chest(fixture).getBlockInventory().getContents(), item) == 6, "refund restores both inventory snapshots");
        check(vault.calls.size() == 3, "one debit, one failed payout and one refund");
        noCharge(quote, buyer, "refunded transaction cannot replay");
        vault.failDeposit = null; registry().remove(owner.player, shop.id());
    }
    private void installLookup() throws Exception {
        Server real = shops.getServer();
        List<FakePlayer> fixtures = List.of(owner, buyer, secondBuyer);
        Server proxy = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class}, (ignored, method, args) -> {
            if ((method.getName().equals("getPlayer") || method.getName().equals("getPlayerExact")) && args.length == 1) {
                for (FakePlayer fixture : fixtures) if (fixture.uuid.equals(args[0]) || fixture.name.equals(args[0])) return fixture.online ? fixture.player : null;
            }
            if (method.getName().equals("getOnlinePlayers")) return fixtures.stream().filter(f -> f.online).map(f -> f.player).toList();
            try { return method.invoke(real, args); } catch (InvocationTargetException exception) { throw exception.getCause(); }
        });
        Field server = JavaPlugin.class.getDeclaredField("server"); server.setAccessible(true); server.set(shops, proxy);
        check(shops.getServer().getPlayer(buyer.uuid) == buyer.player, "only fixture lookup is intercepted");
        check(shops.getServer().getWorld(world.getUID()) == world, "world access remains real Paper");
    }
    private CheckoutService checkout() throws Exception { return (CheckoutService) invoke(shops, "getCheckout"); }
    private ShopAdapter adapter() throws Exception { return (ShopAdapter) invoke(shops, "getAdapter"); }
    private Quote quote(FakePlayer customer, ShopAdapter.Selection selection, int quantity) throws Exception {
        return checkout().quote(customer.uuid, selection.seller(), selection.offerId(), selection.revision(), quantity,
            Money.multiply(selection.price(), quantity), selection.fingerprint());
    }
    private void resetBalances() throws Exception {
        checkout().invalidateAll(); vault.reset();
        for (FakePlayer fixture : List.of(owner, buyer, secondBuyer)) { fixture.online = true; fixture.allowed = true; fixture.denied.clear(); fixture.clear(); vault.balances.put(fixture.uuid, 10000.0); }
    }
    private void noCharge(Quote quote, FakePlayer customer, String reason) throws Exception {
        int before = vault.calls.size();
        check(checkout().confirm(quote.id(), customer.uuid).status() == Status.NO_CHARGE, reason);
        check(vault.calls.size() == before, reason + ": no economy calls");
    }
    private void spawnCheckout() throws Exception {
        resetBalances(); ShopAdapter.Selection bread = adapter().spawn("bread");
        check(bread.bundleSize() == 16 && bread.price() == 4000, "original bread bundle remains 16 for $40");
        Quote reviewed = quote(buyer, bread, 3);
        check(reviewed.amountMinorUnits() == 12000, "three bundles total exactly $120 in minor units");
        check(vault.calls.isEmpty() && buyer.count(bread.item()) == 0, "quote/review creates no charge or items");
        check(!checkout().cancel(reviewed.id(), secondBuyer.uuid), "another player cannot cancel quote");
        check(checkout().cancel(reviewed.id(), buyer.uuid), "owner cancels quote");
        noCharge(reviewed, buyer, "cancelled confirmation cannot buy");
        Quote confirmed = quote(buyer, bread, 3);
        check(checkout().confirm(confirmed.id(), buyer.uuid).status() == Status.SUCCESS, "final confirmation completes purchase");
        check(vault.balances.get(buyer.uuid) == 9880.0, "buyer charged exact $120 once");
        check(buyer.count(bread.item()) == 48, "three 16-item bundles delivered");
        check(vault.calls.size() == 1 && vault.calls.getFirst().startsWith("withdrawPlayer"), "spawn sale has one debit and no invented seller payout");
        noCharge(confirmed, buyer, "replayed final click cannot debit twice");
        Quote wrong = quote(buyer, bread, 1);
        noCharge(wrong, secondBuyer, "another player cannot use quote");
        check(checkout().confirm(wrong.id(), buyer.uuid).status() == Status.SUCCESS, "wrong-user attempt does not consume rightful buyer quote");
        Quote stale = quote(buyer, bread, 1); Quote fresh = quote(buyer, bread, 2);
        noCharge(stale, buyer, "new review invalidates old review");
        checkout().cancel(fresh.id(), buyer.uuid);
    }
    private void spawnRejections() throws Exception {
        resetBalances(); ShopAdapter.Selection bread = adapter().spawn("bread");
        for (int i = 0; i < 36; i++) buyer.storage.setItem(i, new ItemStack(Material.COBBLESTONE, 64));
        noCharge(quote(buyer, bread, 1), buyer, "full inventory rejected");
        buyer.clear(); vault.balances.put(buyer.uuid, 39.99);
        noCharge(quote(buyer, bread, 1), buyer, "insufficient balance rejected");
        vault.balances.put(buyer.uuid, 10000.0);
        Quote disconnected = quote(buyer, bread, 1); buyer.online = false;
        noCharge(disconnected, buyer, "disconnected buyer rejected"); buyer.online = true;
        Quote denied = quote(buyer, bread, 1); buyer.denied.add("wrvshops.use");
        noCharge(denied, buyer, "removed permission rejected"); buyer.denied.clear();
        Quote tampered = checkout().quote(buyer.uuid, null, bread.offerId(), bread.revision(), 1, 1, bread.fingerprint());
        noCharge(tampered, buyer, "underpriced forged quote rejected by final adapter");
        Quote changed = checkout().quote(buyer.uuid, null, bread.offerId(), bread.revision() + 1, 1, bread.price(), bread.fingerprint());
        noCharge(changed, buyer, "changed revision rejected");
        rejects(() -> quote(buyer, bread, 0), "zero quantity rejected");
        rejects(() -> quote(buyer, bread, -1), "negative quantity rejected");
        check(vault.calls.isEmpty(), "all preview and rejection paths avoid provider mutation");
    }
    private Quote localQuote(CheckoutService service, FakePlayer customer, ShopAdapter.Selection selection, int quantity) {
        return service.quote(customer.uuid, selection.seller(), selection.offerId(), selection.revision(), quantity, Money.multiply(selection.price(), quantity), selection.fingerprint());
    }
    private CheckoutService localCheckout(Path path) throws Exception { return new CheckoutService(adapter(), new FileJournal(path), Clock.systemUTC(), Duration.ofSeconds(60)); }
    private void recoveryFailures() throws Exception {
        resetBalances(); Files.createDirectories(getDataFolder().toPath());
        ShopAdapter.Selection bread = adapter().spawn("bread");
        Path uncertainPath = Files.createTempDirectory(getDataFolder().toPath(), "uncertain-provider-");
        CheckoutService uncertain = localCheckout(uncertainPath); Quote quote = localQuote(uncertain, buyer, bread, 1);
        vault.failWithdraw = buyer.uuid; vault.throwWithdraw = true;
        check(uncertain.confirm(quote.id(), buyer.uuid).status() == Status.RECOVERY_REQUIRED, "provider exception is uncertain, never assumed refunded");
        check(uncertain.recoveryRequired() && vault.calls.size() == 1, "uncertain debit pauses further checkouts without replay");
        check(uncertain.confirm(quote.id(), buyer.uuid).status() == Status.RECOVERY_REQUIRED && vault.calls.size() == 1, "uncertain transaction retry makes no further provider call");
        uncertain.closeUnclean();
        FileJournal reopened = new FileJournal(uncertainPath); check(reopened.requiresRecovery(), "uncertain transaction survives journal close/reopen as recovery-required"); reopened.closeUnclean();
        resetBalances();
        Path refundPath = Files.createTempDirectory(getDataFolder().toPath(), "failed-refund-");
        CheckoutService refund = localCheckout(refundPath); Quote refundQuote = localQuote(refund, buyer, bread, 1);
        vault.afterWithdraw = id -> buyer.online = false; vault.failDeposit = buyer.uuid;
        check(refund.confirm(refundQuote.id(), buyer.uuid).status() == Status.RECOVERY_REQUIRED, "disconnected delivery plus failed refund requires reconciliation");
        check(vault.calls.size() == 2 && vault.balances.get(buyer.uuid) == 9960 && buyer.count(bread.item()) == 0, "known debit/failed refund retain exact evidence without duplicate delivery");
        refund.closeUnclean();
        FileJournal refundReopened = new FileJournal(refundPath); check(refundReopened.requiresRecovery(), "failed refund remains halted after journal reopen"); refundReopened.closeUnclean();
        resetBalances();
        Physical fixture = physical(35); ItemStack item = named(Material.EMERALD, "Partial delivery fixture"); ShopRecord shop = create(fixture, item, 5, 100); nearby(buyer, fixture);
        Path partialPath = Files.createTempDirectory(getDataFolder().toPath(), "partial-transfer-"); CheckoutService partial = localCheckout(partialPath);
        Quote partialQuote = localQuote(partial, buyer, adapter().chest(shop, shop.offers().getFirst()), 1); buyer.failSetStorage = true;
        check(partial.confirm(partialQuote.id(), buyer.uuid).status() == Status.RECOVERY_REQUIRED, "inventory exception after stock write halts for reconciliation");
        buyer.failSetStorage = false;
        check(vault.calls.size() == 1 && buyer.count(item) == 0 && InventoryPlan.count(chest(fixture).getBlockInventory().getContents(), item) == 4, "partial outcome never triggers unsafe automatic refund or replay");
        partial.closeUnclean(); registry().remove(owner.player, shop.id());
        resetBalances();
    }
    private void spawnReentrant() throws Exception {
        resetBalances(); ShopAdapter.Selection bread = adapter().spawn("bread"); Quote outer = quote(buyer, bread, 1);
        List<Result> nested = new ArrayList<>();
        vault.afterWithdraw = id -> { try { nested.add(checkout().confirm(outer.id(), buyer.uuid)); } catch (Exception e) { throw new RuntimeException(e); } };
        check(checkout().confirm(outer.id(), buyer.uuid).status() == Status.SUCCESS, "outer final confirmation succeeds");
        check(nested.size() == 1 && nested.getFirst().status() == Status.NO_CHARGE, "reentrant confirmation rejected");
        check(vault.calls.size() == 1 && buyer.count(bread.item()) == 16, "reentry has one debit and one delivery");
        resetBalances(); Quote changed = quote(buyer, bread, 1);
        vault.afterWithdraw = id -> buyer.storage.setItem(0, new ItemStack(Material.DIRT, 1));
        check(checkout().confirm(changed.id(), buyer.uuid).status() == Status.REFUNDED, "post-debit changed destination returns known payment");
        check(vault.balances.get(buyer.uuid) == 10000.0, "refund restores buyer balance exactly");
        check(buyer.count(bread.item()) == 0 && buyer.storage.getItem(0).getType() == Material.DIRT, "no delivery overwrites changed destination");
        check(vault.calls.size() == 2, "one debit and one refund only");
        vault.afterWithdraw = null;
    }
    private FakePlayer fake(int suffix, String name) { return new FakePlayer(UUID.fromString(String.format("667cf2d7-0000-4000-8000-%012d", suffix)), name, world); }
    private void metadataPlan() {
        ItemStack sample = named(Material.DIAMOND_SWORD, "Shop fixture sword");
        ItemMeta meta = sample.getItemMeta();
        meta.lore(List.of(Component.text("Preserve this exact lore")));
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.getPersistentDataContainer().set(new NamespacedKey(this, "identity"), PersistentDataType.STRING, "fixture-identity");
        sample.setItemMeta(meta);
        ItemStack other = named(Material.DIAMOND_SWORD, "Different sword");
        ItemStack[] stock = {sample.clone(), other.clone(), sample.clone()};
        check(InventoryPlan.count(stock, sample) == 2, "only exact metadata counts as sellable stock");
        ItemStack[] after = InventoryPlan.remove(stock, sample, 1);
        check(InventoryPlan.count(after, sample) == 1, "exact selected sword removed");
        check(InventoryPlan.count(after, other) == 1, "different metadata remains");
        check(InventoryPlan.count(stock, sample) == 2, "planning leaves real source untouched");
        ItemStack[] delivered = InventoryPlan.add(new ItemStack[36], sample, 2, 64);
        check(delivered[0].getAmount() == 1 && delivered[1].getAmount() == 1, "unstackable items occupy distinct slots");
        check(delivered[0].isSimilar(sample), "lore, enchantment and PDC survive delivery plan");
        check(delivered[0].getItemMeta().getPersistentDataContainer().get(new NamespacedKey(this, "identity"), PersistentDataType.STRING).equals("fixture-identity"), "PDC value survives");
        ItemStack roundtrip = ItemStack.deserializeBytes(sample.serializeAsBytes());
        check(roundtrip.isSimilar(sample), "exact Paper metadata serialization roundtrip");
        Inventory nativeStorage = Bukkit.createInventory(null, 9);
        nativeStorage.setItem(0, sample); nativeStorage.setItem(1, roundtrip);
        check(nativeStorage.getItem(0).isSimilar(nativeStorage.getItem(1)), "both physical-storage variants preserve exact metadata");
        check(InventoryPlan.count(nativeStorage.getContents(), sample) == 2, "semantic metadata comparison counts both storage variants");
        ItemStack[] full = new ItemStack[36]; Arrays.fill(full, new ItemStack(Material.COBBLESTONE, 64));
        rejects(() -> InventoryPlan.add(full, sample, 1, 64), "full storage rejected before mutation");
        rejects(() -> InventoryPlan.remove(stock, sample, 3), "insufficient exact metadata stock rejected");
        rejects(() -> InventoryPlan.add(new ItemStack[36], sample, 0, 64), "zero quantity rejected");
        ItemStack merge = named(Material.DIAMOND, "Named diamonds");
        ItemStack[] partial = new ItemStack[36]; partial[0] = merge.clone(); partial[0].setAmount(63);
        ItemStack[] merged = InventoryPlan.add(partial, merge, 2, 64);
        check(merged[0].getAmount() == 64 && merged[1].getAmount() == 1, "stack merging obeys capacity");
        check(partial[0].getAmount() == 63, "merge plan does not mutate original");
    }
    private ItemStack named(Material material, String name) { ItemStack item = new ItemStack(material); ItemMeta meta = item.getItemMeta(); meta.displayName(Component.text(name)); item.setItemMeta(meta); return item; }
    private void next() {
        if (cases.isEmpty()) { finish(); return; }
        Case entry = cases.removeFirst();
        try { entry.action.run(); passed++; results.add("PASS " + entry.name); getLogger().info("WRV_SHOPS_HARNESS_PASS " + entry.name); }
        catch (Throwable exception) { failed++; results.add("FAIL " + entry.name + ": " + exception); getLogger().severe("WRV_SHOPS_HARNESS_FAIL " + entry.name + ": " + exception); exception.printStackTrace(); }
        Bukkit.getScheduler().runTaskLater(this, this::next, 3L);
    }
    private void finish() {
        try {
            Files.createDirectories(getDataFolder().toPath());
            Files.writeString(getDataFolder().toPath().resolve("runtime-results.txt"), String.join("\n", results) + "\nSUMMARY passed=" + passed + " failed=" + failed + " assertions=" + assertions + "\n");
        } catch (Exception exception) { failed++; getLogger().severe(exception.toString()); }
        getLogger().info("WRV_SHOPS_HARNESS_SUMMARY passed=" + passed + " failed=" + failed + " assertions=" + assertions);
        Bukkit.getScheduler().runTaskLater(this, Bukkit::shutdown, 10L);
    }
    private void check(boolean condition, String details) { assertions++; if (!condition) throw new AssertionError(details); }
    private void rejects(ThrowingRunnable action, String details) {
        boolean rejected = false; try { action.run(); } catch (Exception expected) { rejected = true; }
        check(rejected, details);
    }
    private static Object invoke(Object object, String name, Object... args) throws Exception {
        Method match = null;
        for (Method method : object.getClass().getMethods()) if (method.getName().equals(name) && method.getParameterCount() == args.length) {
            boolean fits = true;
            for (int i = 0; i < args.length; i++) if (args[i] != null && !wrap(method.getParameterTypes()[i]).isInstance(args[i])) fits = false;
            if (fits) { match = method; break; }
        }
        if (match == null) throw new NoSuchMethodException(object.getClass().getName() + "." + name + "/" + args.length);
        try { return match.invoke(object, args); } catch (InvocationTargetException exception) { if (exception.getCause() instanceof Exception cause) throw cause; throw exception; }
    }
    private static Class<?> wrap(Class<?> type) { if (type == int.class) return Integer.class; if (type == long.class) return Long.class; if (type == boolean.class) return Boolean.class; if (type == double.class) return Double.class; return type; }
    private record Case(String name, ThrowingRunnable action) {}
    @FunctionalInterface private interface ThrowingRunnable { void run() throws Exception; }
}
