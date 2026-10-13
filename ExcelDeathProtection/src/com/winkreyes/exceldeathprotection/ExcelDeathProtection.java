package com.winkreyes.exceldeathprotection;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelDeathProtection
 *
 * The Spawn Shop sells a Death Protection Potion (through the DeluxeMenus
 * console command hook, because ExcelShops delivers plain material items and
 * cannot hand out a custom potion). Drinking the potion activates a one-use
 * buff: the next time the player dies, they keep ALL of their items and the
 * buff is consumed. The death itself is NOT prevented and XP is NOT saved;
 * XP drops exactly as a normal death.
 *
 * While the buff is active it is shown as the vanilla Conduit Power status
 * effect (small HUD icon + listed in the inventory screen). Conduit Power is
 * used because it exists on BOTH Java and Bedrock: the original Luck icon
 * (0.1.0) is a Java-only effect that Bedrock cannot display at all, which
 * Mike caught in the Oct 13, 2026 phone test. On land Conduit Power does
 * nothing; underwater it grants breathing, clearer vision and faster
 * digging, and that benefit is disclosed on the potion and in the guide.
 * The buff state itself is stored in the player's PersistentDataContainer,
 * so it survives relogs and restarts, and is re-applied by a watchdog.
 */
public class ExcelDeathProtection extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private NamespacedKey potionKey;
    private NamespacedKey activeKey;
    private Economy economy;
    private final Map<UUID, Long> pendingBuys = new HashMap<>();
    private final Map<UUID, Boolean> usedProtection = new HashMap<>();

    private static final long CONFIRM_WINDOW_MS = 20_000L;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        potionKey = new NamespacedKey(this, "potion");
        activeKey = new NamespacedKey(this, "active");

        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found. The buy command is disabled; give/status still work.");
        }

        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("deathprotection") != null) {
            getCommand("deathprotection").setExecutor(this);
            getCommand("deathprotection").setTabCompleter(this);
        }

        // Watchdog: keep the Conduit Power indicator present while the buff is active
        // (milk, /effect clear and relogs all remove effects; the buff flag in
        // the player's data is the source of truth).
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (isProtected(player) && !player.hasPotionEffect(PotionEffectType.CONDUIT_POWER)) {
                        applyIndicator(player);
                    }
                }
            }
        }.runTaskTimer(this, 100L, 100L);

        getLogger().info("ExcelDeathProtection enabled. Potion price: $" + money(price()));
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private double price() {
        return getConfig().getDouble("price", 15000.0);
    }

    private String money(double amount) {
        return String.format("%,.2f", amount);
    }

    // ------------------------------------------------------------------
    // Buff state
    // ------------------------------------------------------------------

    public boolean isProtected(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        return data.has(activeKey, PersistentDataType.BYTE)
                && data.get(activeKey, PersistentDataType.BYTE) == (byte) 1;
    }

    private void setProtected(Player player, boolean active) {
        if (active) {
            player.getPersistentDataContainer().set(activeKey, PersistentDataType.BYTE, (byte) 1);
            applyIndicator(player);
        } else {
            player.getPersistentDataContainer().remove(activeKey);
            player.removePotionEffect(PotionEffectType.CONDUIT_POWER);
            player.removePotionEffect(PotionEffectType.LUCK);
        }
    }

    private void applyIndicator(Player player) {
        // Conduit Power: visible on Java and Bedrock, inert on land.
        player.removePotionEffect(PotionEffectType.LUCK);
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.CONDUIT_POWER,
                PotionEffect.INFINITE_DURATION,
                0,      // amplifier
                true,   // ambient
                false,  // no particles
                true)); // show icon
    }

    // ------------------------------------------------------------------
    // The potion item
    // ------------------------------------------------------------------

    public ItemStack createPotion(int amount) {
        ItemStack potion = new ItemStack(Material.POTION, amount);
        ItemMeta meta = potion.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Death Protection Potion");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Drink to activate " + ChatColor.WHITE + "\u2620\u2714 Items protected" + ChatColor.GRAY + ".",
                    ChatColor.GRAY + "If you die while it is active, you",
                    ChatColor.GRAY + "keep all your items " + ChatColor.WHITE + "once" + ChatColor.GRAY + ".",
                    ChatColor.RED + "Does NOT save your XP levels.",
                    ChatColor.AQUA + "While active: Conduit Power. Breathe,",
                    ChatColor.AQUA + "see and dig faster underwater.",
                    ChatColor.DARK_GRAY + "One use. Stays active until it saves you."));
            meta.setEnchantmentGlintOverride(true);
            meta.getPersistentDataContainer().set(potionKey, PersistentDataType.BYTE, (byte) 1);
            if (meta instanceof PotionMeta) {
                ((PotionMeta) meta).setColor(Color.fromRGB(0x7D, 0x3C, 0x98));
            }
            potion.setItemMeta(meta);
        }
        return potion;
    }

    private boolean isProtectionPotion(ItemStack item) {
        if (item == null || item.getType() != Material.POTION || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null
                && meta.getPersistentDataContainer().has(potionKey, PersistentDataType.BYTE);
    }

    // ------------------------------------------------------------------
    // Events
    // ------------------------------------------------------------------

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!isProtectionPotion(event.getItem())) {
            return;
        }
        Player player = event.getPlayer();
        if (isProtected(player)) {
            // Never waste a potion: protection is already active.
            event.setCancelled(true);
            player.sendMessage(ChatColor.YELLOW + "\u2620\u2714 Items protected is already active. This potion was not used.");
            return;
        }
        setProtected(player, true);
        player.sendMessage(ChatColor.GREEN + "\u2620\u2714 Items protected is now ACTIVE.");
        player.sendMessage(ChatColor.GRAY + "If you die, you keep your items once. " + ChatColor.RED + "Your XP is not saved.");
        player.sendMessage(ChatColor.DARK_GRAY + "Look for the blue shell (Conduit Power) icon: while you see it, you are protected.");
        player.sendMessage(ChatColor.DARK_GRAY + "While it is on, you also breathe, see and dig faster underwater.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!isProtected(player)) {
            return;
        }
        // Save the items only. XP handling is left exactly as vanilla:
        // keepLevel stays false and dropped XP is untouched, so levels are
        // lost and XP orbs drop like a normal death.
        event.setKeepInventory(true);
        event.getDrops().clear();
        setProtected(player, false);
        usedProtection.put(player.getUniqueId(), Boolean.TRUE);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (usedProtection.remove(event.getPlayer().getUniqueId()) != null) {
            Player player = event.getPlayer();
            player.sendMessage(ChatColor.GOLD + "\u2620\u2714 Death protection used: your items were kept.");
            player.sendMessage(ChatColor.GRAY + "Your XP dropped as normal. Protection is now off.");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isProtected(player) && !player.hasPotionEffect(PotionEffectType.CONDUIT_POWER)) {
            applyIndicator(player);
        }
    }

    // ------------------------------------------------------------------
    // Commands
    // ------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "status":
                return handleStatus(sender);
            case "give":
                return handleGive(sender, args);
            case "buy":
                return handleBuy(sender, args);
            default:
                sendUsage(sender);
                return true;
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Death Protection: " + ChatColor.WHITE
                + "/deathprotection status | buy" 
                + (sender.hasPermission("exceldeathprotection.admin") ? " | give <player> [amount]" : ""));
    }

    private boolean handleStatus(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players have a protection status.");
            return true;
        }
        Player player = (Player) sender;
        if (isProtected(player)) {
            player.sendMessage(ChatColor.GREEN + "\u2620\u2714 Items protected: ACTIVE. Your next death keeps your items (XP not saved).");
        } else {
            player.sendMessage(ChatColor.GRAY + "\u2620\u2714 Items protected: not active. The Spawn Shop sells the potion for $" + money(price()) + ".");
        }
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("exceldeathprotection.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to do that.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /deathprotection give <player> [amount]");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found or not online: " + args[1]);
            return true;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[2])));
            } catch (NumberFormatException ignored) {
                sender.sendMessage(ChatColor.RED + "Amount must be a number.");
                return true;
            }
        }
        target.getInventory().addItem(createPotion(amount));
        sender.sendMessage(ChatColor.GREEN + "Gave " + amount + " Death Protection Potion(s) to " + target.getName() + ".");
        target.sendMessage(ChatColor.GREEN + "You received a Death Protection Potion. Drink it to activate \u2620\u2714 Items protected.");
        return true;
    }

    /**
     * Buy flow used by the Spawn Shop menu (console: /deathprotection buy <player>)
     * and by players directly (/deathprotection buy). Two-step confirm:
     * the first run arms the purchase, a second run within 20 seconds completes it.
     */
    /**
     * Big on-screen message for the buy flow. Chat alone is easy to miss
     * behind the shop window on Bedrock (Mike's Oct 13 test), so every buy
     * outcome also gets a title that renders over the menu.
     */
    private void showBuyTitle(Player player, String title, String subtitle) {
        player.sendTitle(title, subtitle, 5, 50, 10);
    }

    private boolean handleBuy(CommandSender sender, String[] args) {
        Player buyer;
        if (sender instanceof Player) {
            buyer = (Player) sender;
        } else {
            if (args.length < 2) {
                sender.sendMessage("Console usage: /deathprotection buy <player>");
                return true;
            }
            buyer = Bukkit.getPlayerExact(args[1]);
            if (buyer == null) {
                sender.sendMessage("Player not found or not online: " + args[1]);
                return true;
            }
        }

        if (economy == null) {
            buyer.sendMessage(ChatColor.RED + "The shop money system is not available right now.");
            return true;
        }

        double cost = price();
        UUID id = buyer.getUniqueId();
        long now = System.currentTimeMillis();
        Long armedAt = pendingBuys.get(id);
        if (armedAt == null || now - armedAt > CONFIRM_WINDOW_MS) {
            pendingBuys.put(id, now);
            buyer.sendMessage(ChatColor.YELLOW + "Death Protection Potion: " + ChatColor.WHITE + "$" + money(cost) + ChatColor.YELLOW + ".");
            buyer.sendMessage(ChatColor.YELLOW + "Tap Buy again within 20 seconds to confirm. " 
                    + ChatColor.GRAY + "(Balance: $" + money(economy.getBalance(buyer)) + ")");
            showBuyTitle(buyer,
                    ChatColor.YELLOW + "Death Protection Potion",
                    ChatColor.WHITE + "Tap Buy again within 20 seconds to confirm ($" + money(cost) + ")");
            return true;
        }
        pendingBuys.remove(id);

        double balance = economy.getBalance(buyer);
        if (balance < cost) {
            buyer.sendMessage(ChatColor.RED + "Not enough money. The potion costs $" + money(cost)
                    + ", you have $" + money(balance) + " (short $" + money(cost - balance) + ").");
            showBuyTitle(buyer,
                    ChatColor.RED + "Not enough money",
                    ChatColor.WHITE + "Potion costs $" + money(cost) + " (short $" + money(cost - balance) + ")");
            return true;
        }
        if (!economy.withdrawPlayer(buyer, cost).transactionSuccess()) {
            buyer.sendMessage(ChatColor.RED + "The purchase failed. You were not charged.");
            return true;
        }
        buyer.getInventory().addItem(createPotion(1));
        showBuyTitle(buyer,
                ChatColor.GREEN + "Potion bought",
                ChatColor.WHITE + "Drink it to activate \u2620\u2714 Items protected");
        buyer.sendMessage(ChatColor.GREEN + "You bought a Death Protection Potion for $" + money(cost) + ".");
        buyer.sendMessage(ChatColor.GRAY + "Balance left: $" + money(economy.getBalance(buyer)) + ". Drink the potion to activate \u2620\u2714 Items protected.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("status");
            options.add("buy");
            if (sender.hasPermission("exceldeathprotection.admin")) {
                options.add("give");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                options.add(player.getName());
            }
        }
        return options;
    }
}
