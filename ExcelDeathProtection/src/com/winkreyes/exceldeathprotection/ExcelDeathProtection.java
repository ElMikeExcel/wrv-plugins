package com.winkreyes.exceldeathprotection;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.*;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;

import java.io.IOException;
import java.util.*;

/** One shared, persistent entitlement. Presentation never determines protection. */
public class ExcelDeathProtection extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    public enum Tier { NONE, BASIC, PREMIUM }
    private NamespacedKey potionKey, activeKey, tierKey, loreKey;
    private Economy economy;
    private PurchaseCoordinator purchases;
    private final Map<UUID, Player> purchasePlayers = new HashMap<>();
    private final Map<UUID, BossBar> basicBars = new HashMap<>();
    private final Set<UUID> usedProtection = new HashSet<>();
    private final Set<PlayerDeathEvent> pendingDeaths = Collections.newSetFromMap(new IdentityHashMap<>());
    // Pending consume is finalized after every cancellable listener has run.
    private final Map<UUID, PlayerItemConsumeEvent> pendingConsumes = new HashMap<>();
    private static final int LORE_VERSION = 2;
    private static final String BASIC_BAR = "Inventory protected: 1 death";

    @Override public void onEnable() {
        saveDefaultConfig();
        potionKey = new NamespacedKey(this, "potion");
        activeKey = new NamespacedKey(this, "active");
        tierKey = new NamespacedKey(this, "tier");
        loreKey = new NamespacedKey(this, "lore_version");
        RegisteredServiceProvider<Economy> provider = getServer().getServicesManager().getRegistration(Economy.class);
        economy = provider == null ? null : provider.getProvider();
        if (economy == null) getLogger().warning("Vault economy unavailable; purchases disabled.");
        if (getConfig().contains("price")) getLogger().warning("Legacy price is ignored. Use basic-price and premium-price; defaults are 15000 and 30000.");
        initializePurchases();
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("deathprotection")).setExecutor(this);
        getCommand("deathprotection").setTabCompleter(this);
        for (Player player : Bukkit.getOnlinePlayers()) refreshPlayer(player);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) refreshPlayer(player);
        }, 100L, 100L);
        getLogger().info("ExcelDeathProtection 0.1.2: basic $" + money(price(PurchaseCoordinator.Product.BASIC))
                + ", premium $" + money(price(PurchaseCoordinator.Product.PREMIUM)));
    }

    @Override public void onDisable() {
        basicBars.values().forEach(BossBar::removeAll);
        basicBars.clear();
        pendingConsumes.clear();
        usedProtection.clear();
        pendingDeaths.clear();
        // Do not strip effects belonging to a natural conduit or another plugin.
        // Premium protection remains persisted; normal death removes vanilla effects.
    }

    public Tier getTier(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        if (!data.has(activeKey, PersistentDataType.BYTE)
                || !Byte.valueOf((byte) 1).equals(data.get(activeKey, PersistentDataType.BYTE))) return Tier.NONE;
        // Every historical active=1 purchase was premium. Unknown future/malformed tier
        // also preserves the purchase's premium entitlement rather than downgrading it.
        return data.has(tierKey, PersistentDataType.STRING)
                && "BASIC".equals(data.get(tierKey, PersistentDataType.STRING)) ? Tier.BASIC : Tier.PREMIUM;
    }
    public boolean isProtected(Player player) { return getTier(player) != Tier.NONE; }

    private void setTier(Player player, Tier tier) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        if (tier == Tier.NONE) {
            data.remove(activeKey); data.remove(tierKey);
        } else {
            data.set(activeKey, PersistentDataType.BYTE, (byte) 1);
            data.set(tierKey, PersistentDataType.STRING, tier.name());
        }
    }
    private void refreshPlayer(Player player) {
        if (!player.isOnline()) return;
        refreshPresentation(player);
        refreshPotionLore(player);
    }
    private void refreshPresentation(Player player) {
        Tier tier = getTier(player);
        if (tier == Tier.BASIC) {
            BossBar bar = basicBars.computeIfAbsent(player.getUniqueId(), id ->
                    Bukkit.createBossBar(BASIC_BAR, BarColor.GREEN, BarStyle.SOLID));
            if (!bar.getPlayers().contains(player)) bar.addPlayer(player);
            bar.setVisible(true);
        } else hideBar(player);
        if (tier == Tier.PREMIUM && !player.hasPotionEffect(PotionEffectType.CONDUIT_POWER)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER,
                    PotionEffect.INFINITE_DURATION, 0, true, false, true));
        }
        // No unconditional removePotionEffect: the old version recorded no ownership,
        // and Bukkit cannot reliably distinguish another plugin's identical effect.
    }
    private void hideBar(Player player) {
        BossBar bar = basicBars.remove(player.getUniqueId());
        if (bar != null) bar.removeAll();
    }

    public ItemStack createPotion(int amount) {
        if (amount < 1 || amount > 64) throw new IllegalArgumentException("amount must be 1..64");
        ItemStack item = new ItemStack(Material.POTION, amount);
        ItemMeta meta = Objects.requireNonNull(item.getItemMeta());
        meta.getPersistentDataContainer().set(potionKey, PersistentDataType.BYTE, (byte) 1);
        meta.setEnchantmentGlintOverride(true);
        if (meta instanceof PotionMeta potion) potion.setColor(Color.fromRGB(0x7D, 0x3C, 0x98));
        updatePotionText(meta);
        item.setItemMeta(meta);
        return item;
    }
    public boolean isProtectionPotion(ItemStack item) {
        return item != null && item.getType() == Material.POTION && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(potionKey, PersistentDataType.BYTE);
    }
    private void updatePotionText(ItemMeta meta) {
        String oldName = meta.hasDisplayName() ? ChatColor.stripColor(meta.getDisplayName()) : "";
        if (oldName.isEmpty() || oldName.equals("Death Protection Potion") || oldName.equals("+Death Protection Potion"))
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "+Death Protection Potion");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Drink to protect your items for one death.",
                ChatColor.GRAY + "You still die and respawn normally.",
                ChatColor.RED + "Does NOT save your XP levels.",
                ChatColor.AQUA + "While active: Conduit Power gives",
                ChatColor.AQUA + "underwater breathing and clearer vision,",
                ChatColor.AQUA + "plus faster mining (including on land).",
                ChatColor.DARK_GRAY + "One active protection. A second drink is blocked.",
                ChatColor.DARK_GRAY + "Existing potions keep their premium benefits."));
        meta.getPersistentDataContainer().set(loreKey, PersistentDataType.INTEGER, LORE_VERSION);
    }
    private void refreshPotionLore(Player player) {
        PlayerInventory inv = player.getInventory();
        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack item = inv.getItem(slot);
            if (!isProtectionPotion(item)) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta.getPersistentDataContainer().has(loreKey, PersistentDataType.INTEGER)
                    && Integer.valueOf(LORE_VERSION).equals(meta.getPersistentDataContainer().get(loreKey, PersistentDataType.INTEGER))) continue;
            ItemStack updated = item.clone();
            updatePotionText(meta); updated.setItemMeta(meta); inv.setItem(slot, updated);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!isProtectionPotion(event.getItem())) return;
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (isProtected(player) || pendingConsumes.containsKey(id)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.YELLOW + "Protection is already active. This potion was not used.");
            return;
        }
        // Activate during this event so lethal damage immediately after drinking is
        // protected. At MONITOR, reverse the tentative change if another listener
        // cancelled the drink. Only finalized activation gets an effect or save.
        pendingConsumes.put(id, event);
        setTier(player, Tier.PREMIUM);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void finishConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (pendingConsumes.get(player.getUniqueId()) != event) return;
        pendingConsumes.remove(player.getUniqueId());
        if (event.isCancelled() || !isProtectionPotion(event.getItem())) { setTier(player, Tier.NONE); return; }
        // Vanilla decrements the consumed bottle after this event returns. Saving
        // inside the event would persist both the bottle and active protection.
        Bukkit.getScheduler().runTask(this, () -> {
            try { player.saveData(); }
            catch (RuntimeException ex) {
                getLogger().severe("Premium activation persistence uncertain for " + player.getUniqueId() + ": " + ex);
                player.sendMessage(ChatColor.RED + "Saving protection failed. Tell an admin before logging out.");
            }
        });
        refreshPresentation(player);
        player.sendMessage(ChatColor.GREEN + "+Death Protection is ACTIVE. Your next death keeps your items once.");
        player.sendMessage(ChatColor.RED + "XP is not saved. " + ChatColor.AQUA + "Conduit: underwater breathing/vision and faster mining, including on land.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!isProtected(player)) return;
        event.setKeepInventory(true);
        event.getDrops().clear();
        pendingDeaths.add(event);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void finishDeath(PlayerDeathEvent event) {
        if (!pendingDeaths.remove(event) || event.isCancelled()) return;
        Player player = event.getEntity();
        // Do not alter keepLevel, droppedExp, newExp/newLevel or actual death/respawn.
        setTier(player, Tier.NONE);
        hideBar(player);
        pendingConsumes.remove(player.getUniqueId());
        usedProtection.add(player.getUniqueId());
        // Player data is saved by the normal death/respawn flow. Saving an alive
        // pre-death snapshot here would create a misleading rollback boundary.
    }
    @EventHandler public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        hideBar(player);
        if (usedProtection.remove(player.getUniqueId())) {
            player.sendMessage(ChatColor.GOLD + "Death protection used: your items were kept. Protection is now off.");
            player.sendMessage(ChatColor.GRAY + "XP follows the server's normal death rules.");
        }
    }
    @EventHandler public void onJoin(PlayerJoinEvent event) { refreshPlayer(event.getPlayer()); }
    @EventHandler public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        hideBar(player); pendingConsumes.remove(player.getUniqueId()); usedProtection.remove(player.getUniqueId());
        if (purchases != null) purchases.clearConfirmation(player.getUniqueId());
    }

    private double price(PurchaseCoordinator.Product product) {
        return getConfig().getDouble(product == PurchaseCoordinator.Product.BASIC ? "basic-price" : "premium-price",
                product == PurchaseCoordinator.Product.BASIC ? 15000.0 : 30000.0);
    }
    private static String money(double amount) { return String.format(Locale.US, "%,.2f", amount); }
    private static String productName(PurchaseCoordinator.Product product) {
        return product == PurchaseCoordinator.Product.BASIC ? "Death Protection" : "+Death Protection Potion";
    }
    private void message(Player p, ChatColor color, String title, String detail) {
        p.sendMessage(color + title + ". " + ChatColor.WHITE + detail);
        p.sendTitle(color + title, ChatColor.WHITE + detail, 5, 50, 10);
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!Bukkit.isPrimaryThread()) { sender.sendMessage("This command must run on the server thread."); return true; }
        if (args.length == 0) { usage(sender); return true; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status":
                if (sender instanceof Player player) {
                    Tier tier = getTier(player);
                    player.sendMessage(ChatColor.GOLD + "Death Protection: " + tier + ". " +
                            (tier == Tier.NONE ? "Basic $" + money(price(PurchaseCoordinator.Product.BASIC)) + "; premium $" + money(price(PurchaseCoordinator.Product.PREMIUM))
                                    : "Your next death keeps your items once; XP is not saved."));
                } else sender.sendMessage("Only players have a protection status.");
                return true;
            case "buy": return handleBuy(sender, args);
            case "give": return handleGive(sender, args);
            default: usage(sender); return true;
        }
    }
    private void usage(CommandSender sender) {
        sender.sendMessage("/dp status | /dp buy basic | /dp buy premium" +
                (sender.hasPermission("exceldeathprotection.admin") ? " | /dp give <player> [amount]" : ""));
    }
    private boolean handleBuy(CommandSender sender, String[] args) {
        PurchaseCoordinator.Product product = PurchaseCoordinator.Product.PREMIUM;
        Player buyer;
        if (sender instanceof Player player) {
            buyer = player;
            if (args.length > 2) { usage(sender); return true; }
            if (args.length == 2) {
                try { product = PurchaseCoordinator.Product.valueOf(args[1].toUpperCase(Locale.ROOT)); }
                catch (IllegalArgumentException ex) { usage(sender); return true; }
            }
        } else {
            if (!sender.hasPermission("exceldeathprotection.admin")) { sender.sendMessage("Permission denied."); return true; }
            String name;
            if (args.length == 2) name = args[1]; // historical menu command means premium
            else if (args.length == 3) {
                try { product = PurchaseCoordinator.Product.valueOf(args[1].toUpperCase(Locale.ROOT)); }
                catch (IllegalArgumentException ex) { usage(sender); return true; }
                name = args[2];
            } else { sender.sendMessage("Console: deathprotection buy basic|premium <player>"); return true; }
            buyer = Bukkit.getPlayerExact(name);
            if (buyer == null) { sender.sendMessage("Player is not online: " + name); return true; }
        }
        performPurchase(buyer, product);
        return true;
    }
    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("exceldeathprotection.admin")) { sender.sendMessage("Permission denied."); return true; }
        if (args.length < 2 || args.length > 3) { usage(sender); return true; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage("Player not online."); return true; }
        int amount = 1;
        try { if (args.length == 3) amount = Integer.parseInt(args[2]); }
        catch (NumberFormatException ex) { sender.sendMessage("Amount must be 1..64."); return true; }
        if (amount < 1 || amount > 64) { sender.sendMessage("Amount must be 1..64."); return true; }
        int given = 0;
        for (int i = 0; i < amount && target.getInventory().firstEmpty() >= 0; i++) {
            if (!target.getInventory().addItem(createPotion(1)).isEmpty()) break;
            given++;
        }
        target.saveData();
        sender.sendMessage("Gave " + given + " premium potion(s) to " + target.getName() + "; " + (amount - given) + " not delivered (no space).");
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return sender.hasPermission("exceldeathprotection.admin") ? List.of("status", "buy", "give") : List.of("status", "buy");
        if (args.length == 2 && args[0].equalsIgnoreCase("buy")) return List.of("basic", "premium");
        return List.of();
    }

    private void initializePurchases() {
        try {
            PurchaseJournal journal = new PurchaseJournal(getDataFolder().toPath().resolve("purchases.journal"));
            purchases = new PurchaseCoordinator(new PurchaseCoordinator.Ports() {
                @Override public PurchaseCoordinator.Eligibility check(UUID id, PurchaseCoordinator.Product product) {
                    Player p = purchasePlayers.get(id);
                    if (economy == null) return PurchaseCoordinator.Eligibility.UNAVAILABLE;
                    if (p == null || !p.isOnline() || p.isDead()) return PurchaseCoordinator.Eligibility.OFFLINE;
                    if (product == PurchaseCoordinator.Product.BASIC && isProtected(p)) return PurchaseCoordinator.Eligibility.ALREADY_ACTIVE;
                    if (product == PurchaseCoordinator.Product.PREMIUM && !hasPotionSpace(p)) return PurchaseCoordinator.Eligibility.NO_SPACE;
                    return PurchaseCoordinator.Eligibility.ELIGIBLE;
                }
                @Override public double balance(UUID id) { return economy.getBalance(purchasePlayers.get(id)); }
                @Override public PurchaseCoordinator.MoneyResult withdraw(UUID id, double amount) {
                    EconomyResponse r = economy.withdrawPlayer(purchasePlayers.get(id), amount);
                    return r == null ? null : new PurchaseCoordinator.MoneyResult(r.transactionSuccess(), r.amount, r.balance);
                }
                @Override public PurchaseCoordinator.MoneyResult deposit(UUID id, double amount) {
                    EconomyResponse r = economy.depositPlayer(purchasePlayers.get(id), amount);
                    return r == null ? null : new PurchaseCoordinator.MoneyResult(r.transactionSuccess(), r.amount, r.balance);
                }
                @Override public PurchaseCoordinator.DeliveryOutcome deliver(UUID id, PurchaseCoordinator.Product product) {
                    Player p = purchasePlayers.get(id);
                    if (p == null || !p.isOnline() || p.isDead()) return PurchaseCoordinator.DeliveryOutcome.FAILED_ROLLED_BACK;
                    return product == PurchaseCoordinator.Product.BASIC ? deliverBasic(p) : deliverPotion(p);
                }
            }, journal, System::currentTimeMillis, 20_000L, 1_000L);
            if (!journal.unresolved().isEmpty()) getLogger().severe("Unresolved purchase journal entries: " + journal.unresolved().size()
                    + ". Affected players are locked from purchases until manual reconciliation; do not automatically retry payment/refund.");
        } catch (IOException | RuntimeException ex) {
            purchases = null;
            getLogger().severe("Purchase journal unavailable. Purchases disabled: " + ex);
        }
    }
    private boolean hasPotionSpace(Player player) {
        for (ItemStack item : player.getInventory().getStorageContents())
            if (item == null || item.getType().isAir()) return true;
        return false;
    }
    // SUCCESS below means the grant was observed in memory and saveData requested.
    // Paper can swallow disk-save errors; its void API does not prove durability.
    // Retain the purchase audit and use manual reconciliation after storage failure.
    private PurchaseCoordinator.DeliveryOutcome deliverBasic(Player player) {
        if (isProtected(player)) return PurchaseCoordinator.DeliveryOutcome.FAILED_ROLLED_BACK;
        PersistentDataContainer data = player.getPersistentDataContainer();
        byte[] before;
        try { before = data.serializeToBytes(); }
        catch (IOException | RuntimeException ex) { return PurchaseCoordinator.DeliveryOutcome.FAILED_ROLLED_BACK; }
        try {
            setTier(player, Tier.BASIC);
            player.saveData();
            if (getTier(player) != Tier.BASIC) throw new IllegalStateException("Basic entitlement verification failed");
            return PurchaseCoordinator.DeliveryOutcome.SUCCESS;
        } catch (RuntimeException ex) {
            getLogger().warning("Basic grant failed for " + player.getUniqueId() + "; attempting rollback: " + ex);
            try {
                data.readFromBytes(before, true);
                player.saveData();
                if (getTier(player) == Tier.NONE) return PurchaseCoordinator.DeliveryOutcome.FAILED_ROLLED_BACK;
            } catch (IOException | RuntimeException rollback) {
                getLogger().severe("Basic rollback uncertain for " + player.getUniqueId() + ": " + rollback);
            }
            return PurchaseCoordinator.DeliveryOutcome.UNKNOWN;
        }
    }
    private PurchaseCoordinator.DeliveryOutcome deliverPotion(Player player) {
        if (!hasPotionSpace(player)) return PurchaseCoordinator.DeliveryOutcome.FAILED_ROLLED_BACK;
        PlayerInventory inv = player.getInventory();
        ItemStack[] before;
        ItemStack potion;
        int count;
        try {
            before = cloneItems(inv.getStorageContents());
            potion = createPotion(1);
            count = countSimilar(before, potion);
        } catch (RuntimeException ex) { return PurchaseCoordinator.DeliveryOutcome.FAILED_ROLLED_BACK; }
        try {
            Map<Integer, ItemStack> leftover = inv.addItem(potion.clone());
            if (leftover == null || !leftover.isEmpty() || countSimilar(inv.getStorageContents(), potion) != count + 1)
                throw new IllegalStateException("Potion delivery verification failed");
            player.saveData();
            return PurchaseCoordinator.DeliveryOutcome.SUCCESS;
        } catch (RuntimeException ex) {
            getLogger().warning("Potion grant failed for " + player.getUniqueId() + "; attempting rollback: " + ex);
            try {
                inv.setStorageContents(cloneItems(before));
                player.saveData();
                if (Arrays.equals(before, inv.getStorageContents())) return PurchaseCoordinator.DeliveryOutcome.FAILED_ROLLED_BACK;
            } catch (RuntimeException rollback) {
                getLogger().severe("Potion rollback uncertain for " + player.getUniqueId() + ": " + rollback);
            }
            return PurchaseCoordinator.DeliveryOutcome.UNKNOWN;
        }
    }
    private static ItemStack[] cloneItems(ItemStack[] source) {
        ItemStack[] clone = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) clone[i] = source[i] == null ? null : source[i].clone();
        return clone;
    }
    private static int countSimilar(ItemStack[] contents, ItemStack wanted) {
        int count = 0;
        for (ItemStack item : contents) if (item != null && item.isSimilar(wanted)) count += item.getAmount();
        return count;
    }
    private void performPurchase(Player buyer, PurchaseCoordinator.Product product) {
        if (purchases == null || economy == null) {
            message(buyer, ChatColor.RED, "Shop unavailable", "No purchase was started. Please try later.");
            return;
        }
        UUID id = buyer.getUniqueId();
        // Retain only the current synchronous caller, never an offline Player object.
        if (purchasePlayers.putIfAbsent(id, buyer) != null) {
            message(buyer, ChatColor.YELLOW, "Purchase in progress", "Wait for its result before trying again.");
            return;
        }
        PurchaseCoordinator.Result result;
        try { result = purchases.click(id, product, price(product)); }
        finally { purchasePlayers.remove(id); }
        switch (result.status()) {
            case ARMED -> message(buyer, ChatColor.YELLOW, productName(product) + " $" + money(result.price()),
                    "Tap again within 20s. Balance $" + money(result.balance()) + ".");
            case PURCHASED -> {
                try { refreshPresentation(buyer); }
                catch (RuntimeException ex) { getLogger().warning("Presentation refresh failed after successful purchase: " + ex); }
                message(buyer, ChatColor.GREEN, product == PurchaseCoordinator.Product.BASIC ? "Death Protection active" : "Premium potion bought",
                        product == PurchaseCoordinator.Product.BASIC ? "Inventory protected for 1 death. XP not saved." : "Drink it to activate. XP not saved.");
                buyer.sendMessage(ChatColor.GRAY + "Paid $" + money(result.price()) + "; balance $" + money(result.balance()) + ".");
            }
            case INELIGIBLE -> {
                String detail = result.detail();
                if (isProtected(buyer) && product == PurchaseCoordinator.Product.BASIC) detail = "Protection is already active; no charge.";
                else if (product == PurchaseCoordinator.Product.PREMIUM && !hasPotionSpace(buyer)) detail = "Make an empty inventory slot first; no charge.";
                else detail = "This purchase is unavailable right now; no charge.";
                message(buyer, ChatColor.YELLOW, "Cannot buy", detail);
            }
            case INSUFFICIENT_FUNDS -> message(buyer, ChatColor.RED, "Not enough money",
                    "Costs $" + money(result.price()) + "; short $" + money(Math.max(0, result.price() - result.balance())) + ".");
            case DECLINED -> message(buyer, ChatColor.RED, "Payment declined", "The economy provider declined this purchase.");
            case REFUNDED -> message(buyer, ChatColor.YELLOW, "Purchase refunded", "Delivery failed; $" + money(result.price()) + " was returned.");
            case COOLDOWN, BUSY -> message(buyer, ChatColor.YELLOW, "Please wait", "Your last purchase is already being handled.");
            case LOCKED -> {
                getLogger().severe("Purchase needs manual reconciliation for " + id + ", transaction " + result.transactionId() + ": " + result.detail());
                message(buyer, ChatColor.RED, "Purchase needs admin review", "Payment or delivery is uncertain. Do not retry; tell an admin.");
            }
            case UNAVAILABLE, INVALID_PRICE -> message(buyer, ChatColor.RED, "Shop unavailable", "No purchase can be completed right now; tell an admin.");
        }
    }
}
