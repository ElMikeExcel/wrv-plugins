package com.winkreyes.excelmobveil;

import com.destroystokyo.paper.event.entity.EndermanAttackPlayerEvent;
import com.destroystokyo.paper.event.entity.SlimeTargetLivingEntityEvent;
import io.papermc.paper.event.entity.WardenAngerChangeEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.*;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import java.io.IOException;
import java.util.*;
import java.util.function.LongSupplier;

/** Stored potions add eight real-time minutes of standard mob targeting suppression. */
public final class ExcelMobVeil extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private NamespacedKey potionKey, startedKey, expiresKey;
    private Economy economy;
    private PurchaseCoordinator purchases;
    private PurchaseReviewMenu reviewMenu;
    private final Map<UUID, Player> purchasePlayers = new HashMap<>();
    private final Map<UUID, PlayerItemConsumeEvent> pendingConsumes = new HashMap<>();
    private final Set<UUID> ownEffectChanges = new HashSet<>();
    private LongSupplier clock = System::currentTimeMillis;

    @Override public void onEnable() {
        saveDefaultConfig();
        potionKey = new NamespacedKey(this, "potion");
        startedKey = new NamespacedKey(this, "started_at");
        expiresKey = new NamespacedKey(this, "expires_at");
        if (getConfig().getInt("duration-seconds", 480) != 480)
            getLogger().warning("Each potion adds a fixed eight minutes; duration-seconds must remain 480.");
        RegisteredServiceProvider<Economy> provider = getServer().getServicesManager().getRegistration(Economy.class);
        economy = provider == null ? null : provider.getProvider();
        if (economy == null) getLogger().warning("No Vault economy provider; purchases disabled.");
        initializePurchases();
        initializeReviewMenu();
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("mobveil")).setExecutor(this);
        getCommand("mobveil").setTabCompleter(this);
        for (Player player : Bukkit.getOnlinePlayers()) refreshPlayer(player, true);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            List<Player> active = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                refreshPlayer(player, false);
                if (isVeiled(player)) active.add(player);
            }
            if (!active.isEmpty()) clearTargets(active);
        }, 1L, 10L);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) reviewMenu.refresh(player);
        }, 20L, 20L);
        getLogger().info("ExcelMobVeil 0.1.0: $" + money(price(PurchaseCoordinator.Product.VEIL))
                + "; each drink adds eight real-time minutes. Attacking ends all time. Scripted bosses and area damage remain possible.");
    }
    @Override public void onDisable() {
        if (reviewMenu != null) reviewMenu.shutdown();
        pendingConsumes.clear();
        for (Player player : Bukkit.getOnlinePlayers()) if (hasRecord(player)) {
            removeOwnedEffect(player);
            savePlayer(player);
        }
    }
    public ItemStack createPotion(int amount) {
        if (amount < 1 || amount > 64) throw new IllegalArgumentException("amount must be 1..64");
        ItemStack item = new ItemStack(Material.POTION, amount);
        PotionMeta meta = (PotionMeta) Objects.requireNonNull(item.getItemMeta());
        meta.setBasePotionType(PotionType.WATER);
        meta.setColor(Color.fromRGB(0x63, 0xA8, 0xAA));
        meta.setEnchantmentGlintOverride(true);
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Veil Potion");
        meta.setLore(List.of(ChatColor.GRAY + "Drink: invisible body + standard mobs ignore you.",
                ChatColor.GRAY + "Adds 8 real-time minutes, even while offline.",
                ChatColor.AQUA + "Drink more to stack time; attacking ends it all.",
                ChatColor.YELLOW + "Works in armor. Armor stays visible to players.",
                ChatColor.GRAY + "Farming is safe. Milk and death end all time.",
                ChatColor.DARK_GRAY + "Boss scripts and area attacks can still hurt you."));
        meta.getPersistentDataContainer().set(potionKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }
    public boolean isVeilPotion(ItemStack item) {
        return item != null && item.getType() == Material.POTION && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(potionKey, PersistentDataType.BYTE)
                && Byte.valueOf((byte) 1).equals(item.getItemMeta().getPersistentDataContainer().get(potionKey, PersistentDataType.BYTE));
    }
    private boolean hasRecord(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        return data.has(startedKey) || data.has(expiresKey);
    }
    public long remainingMillis(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        if (!data.has(startedKey, PersistentDataType.LONG) || !data.has(expiresKey, PersistentDataType.LONG)) return 0;
        return VeilTiming.remaining(data.get(startedKey, PersistentDataType.LONG),
                data.get(expiresKey, PersistentDataType.LONG), clock.getAsLong());
    }
    public boolean isVeiled(Player player) { return remainingMillis(player) > 0 && !player.isDead(); }
    private boolean savePlayer(Player player) {
        try { player.saveData(); return true; }
        catch (RuntimeException failure) {
            getLogger().severe("Player save uncertain for " + player.getUniqueId() + ": " + failure);
            player.sendMessage(ChatColor.RED + "Saving Veil Potion state failed. Please tell an admin.");
            return false;
        }
    }
    private boolean veilEffect(PotionEffect effect) {
        return effect != null && effect.getType() == PotionEffectType.INVISIBILITY && effect.getAmplifier() == 0
                && !effect.isAmbient() && !effect.hasParticles() && effect.hasIcon();
    }
    private void removeOwnedEffect(Player player) {
        if (!veilEffect(player.getPotionEffect(PotionEffectType.INVISIBILITY))) return;
        UUID id = player.getUniqueId(); ownEffectChanges.add(id);
        try { player.removePotionEffect(PotionEffectType.INVISIBILITY); }
        finally { ownEffectChanges.remove(id); }
    }
    private boolean applyEffect(Player player, long remaining) {
        UUID id = player.getUniqueId(); ownEffectChanges.add(id);
        try {
            return player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,
                    VeilTiming.ticks(remaining), 0, false, false, true));
        } finally { ownEffectChanges.remove(id); }
    }
    private void end(Player player, String reason, boolean removeEffect, boolean save) {
        pendingConsumes.remove(player.getUniqueId());
        boolean existed = hasRecord(player);
        player.getPersistentDataContainer().remove(startedKey);
        player.getPersistentDataContainer().remove(expiresKey);
        if (existed && removeEffect) removeOwnedEffect(player);
        if (existed && save) savePlayer(player);
        if (existed && reason != null) player.sendMessage(ChatColor.YELLOW + reason);
    }
    private void refreshPlayer(Player player, boolean restore) {
        if (!hasRecord(player)) return;
        long left = remainingMillis(player);
        if (left <= 0 || player.isDead()) { end(player, "Your veil has ended.", true, true); return; }
        PotionEffect effect = player.getPotionEffect(PotionEffectType.INVISIBILITY);
        if (effect != null && !veilEffect(effect)) {
            end(player, "Your veil ended when invisibility changed.", false, true); return;
        }
        if (restore) {
            removeOwnedEffect(player);
            if (!applyEffect(player, left)) end(player, "Veil ended because invisibility could not be applied.", false, true);
        } else if (effect == null) {
            end(player, "Your veil has ended.", false, true);
        } else if ((long) effect.getDuration() > (long) VeilTiming.ticks(left) + 10L
                || VeilTiming.ticks(left) == Integer.MAX_VALUE && effect.getDuration() < Integer.MAX_VALUE - 1_200) {
            removeOwnedEffect(player);
            if (!applyEffect(player, left)) end(player, "Your veil has ended.", false, true);
        }
    }
    private record ConsumedSlot(int slot, ItemStack before) {}
    private ConsumedSlot consumedSlot(Player player, PlayerItemConsumeEvent event) {
        PlayerInventory inventory = player.getInventory();
        int slot = event.getHand() == EquipmentSlot.OFF_HAND ? 40 : inventory.getHeldItemSlot();
        ItemStack before = inventory.getItem(slot);
        return before != null && before.isSimilar(event.getItem()) ? new ConsumedSlot(slot, before.clone()) : null;
    }
    private boolean restoreConsumedBottle(Player player, ConsumedSlot slot) {
        if (slot != null) {
            ItemStack current = player.getInventory().getItem(slot.slot());
            if (Objects.equals(current, slot.before())) { savePlayer(player); return true; }
            if (slot.before().getAmount() == 1 && current != null && current.getType() == Material.GLASS_BOTTLE
                    && current.getAmount() == 1) {
                player.getInventory().setItem(slot.slot(), slot.before().clone());
                if (Objects.equals(player.getInventory().getItem(slot.slot()), slot.before())) {
                    savePlayer(player); return true;
                }
            }
        }
        savePlayer(player);
        getLogger().severe("Veil drink rollback inventory uncertain for " + player.getUniqueId()
                + "; reconcile bottle and timer manually. Do not blindly grant another bottle.");
        player.sendMessage(ChatColor.RED + "Veil could not activate. Ask an admin to check the bottle and timer.");
        return false;
    }
    private void activateOrExtend(Player player, ConsumedSlot slot) {
        long now = clock.getAsLong();
        PersistentDataContainer data = player.getPersistentDataContainer();
        Long previousStart = data.has(startedKey, PersistentDataType.LONG) ? data.get(startedKey, PersistentDataType.LONG) : null;
        Long previousExpiry = data.has(expiresKey, PersistentDataType.LONG) ? data.get(expiresKey, PersistentDataType.LONG) : null;
        boolean extending = VeilTiming.remaining(previousStart, previousExpiry, now) > 0;
        long deadline;
        try { deadline = VeilTiming.extension(extending ? previousExpiry : null, now); }
        catch (ArithmeticException | IllegalArgumentException failure) { restoreConsumedBottle(player, slot); return; }
        data.set(startedKey, PersistentDataType.LONG, extending ? previousStart : now);
        data.set(expiresKey, PersistentDataType.LONG, deadline);
        removeOwnedEffect(player);
        if (!applyEffect(player, deadline - now) || !savePlayer(player)) {
            getLogger().warning("Veil drink did not commit for " + player.getUniqueId() + "; previous expiry="
                    + previousExpiry + ", attempted expiry=" + deadline + ". Attempting rollback; durability is not guaranteed.");
            removeOwnedEffect(player);
            data.remove(startedKey); data.remove(expiresKey);
            if (extending) {
                data.set(startedKey, PersistentDataType.LONG, previousStart);
                data.set(expiresKey, PersistentDataType.LONG, previousExpiry);
                long previousLeft = VeilTiming.remaining(previousStart, previousExpiry, clock.getAsLong());
                if (previousLeft <= 0 || !applyEffect(player, previousLeft)) {
                    data.remove(startedKey); data.remove(expiresKey);
                }
            }
            if (restoreConsumedBottle(player, slot)) player.sendMessage(ChatColor.YELLOW
                    + "This drink could not complete. Your potion was restored; check /veil status.");
            return;
        }
        clearTargets(List.of(player));
        long seconds = (deadline - now) / 1000 + ((deadline - now) % 1000 == 0 ? 0 : 1);
        player.sendMessage(ChatColor.AQUA + "Veil " + (extending ? "extended" : "active")
                + ": added 8 minutes; " + seconds + " seconds left. Attacking ends it all.");
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void consume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (event.getItem().getType() == Material.MILK_BUCKET) {
            Bukkit.getScheduler().runTask(this, () -> {
                if (!event.isCancelled() && event.getItem().getType() == Material.MILK_BUCKET)
                    end(player, "Milk removed your veil.", true, true);
            });
            return;
        }
        if (!isVeilPotion(event.getItem())) return;
        refreshPlayer(player, false);
        UUID id = player.getUniqueId();
        if (pendingConsumes.containsKey(id) || player.hasPotionEffect(PotionEffectType.INVISIBILITY) && !isVeiled(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.YELLOW + "A drink is pending or another invisibility effect is active. This potion was not used.");
            return;
        }
        try {
            Long currentExpiry = isVeiled(player) ? player.getPersistentDataContainer().get(expiresKey, PersistentDataType.LONG) : null;
            VeilTiming.extension(currentExpiry, clock.getAsLong());
        } catch (ArithmeticException | IllegalArgumentException overflow) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Your veil timer cannot safely hold more time. This potion was not used."); return;
        }
        // A thrown save error cancels before native consumption; a void save is not fsync proof.
        if (!savePlayer(player)) { event.setCancelled(true); return; }
        ConsumedSlot slot = consumedSlot(player, event);
        pendingConsumes.put(id, event);
        // Recheck final cancellation after vanilla has consumed the bottle.
        Bukkit.getScheduler().runTask(this, () -> {
            if (pendingConsumes.get(id) != event) return;
            pendingConsumes.remove(id);
            if (event.isCancelled() || !isVeilPotion(event.getItem()) || !player.isOnline() || player.isDead()) return;
            if (player.hasPotionEffect(PotionEffectType.INVISIBILITY) && !isVeiled(player)) {
                restoreConsumedBottle(player, slot); return;
            }
            activateOrExtend(player, slot);
        });
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void died(PlayerDeathEvent event) { end(event.getEntity(), null, true, false); }
    @EventHandler public void joined(PlayerJoinEvent event) {
        refreshPlayer(event.getPlayer(), true);
        if (isVeiled(event.getPlayer())) clearTargets(List.of(event.getPlayer()));
    }
    @EventHandler public void changedWorld(PlayerChangedWorldEvent event) {
        refreshPlayer(event.getPlayer(), true);
        if (isVeiled(event.getPlayer())) clearTargets(List.of(event.getPlayer()));
    }
    @EventHandler public void quit(PlayerQuitEvent event) {
        pendingConsumes.remove(event.getPlayer().getUniqueId());
        if (purchases != null) purchases.cancel(event.getPlayer().getUniqueId());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void effectChanged(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player) || ownEffectChanges.contains(player.getUniqueId())
                || event.getModifiedType() != PotionEffectType.INVISIBILITY || !hasRecord(player)) return;
        if (event.getAction() == EntityPotionEffectEvent.Action.CHANGED && !event.isOverride()) return;
        // Retire ownership synchronously so subsequent attacks cannot remove an external replacement.
        end(player, "Your veil ended when invisibility changed.", false, false);
        Bukkit.getScheduler().runTask(this, () -> savePlayer(player));
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void target(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player player && isVeiled(player)) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void enderman(EndermanAttackPlayerEvent event) { if (isVeiled(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void slime(SlimeTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player player && isVeiled(player)) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void anger(WardenAngerChangeEvent event) {
        if (event.getTarget() instanceof Player player && isVeiled(player)) event.setNewAnger(0);
    }
    private void clearTargets(Collection<Player> players) {
        Map<UUID, Player> active = new HashMap<>();
        for (Player player : players) if (isVeiled(player)) active.put(player.getUniqueId(), player);
        if (active.isEmpty()) return;
        for (World world : Bukkit.getWorlds()) for (LivingEntity entity : world.getLivingEntities()) {
            if (!(entity instanceof Mob mob)) continue;
            LivingEntity victim = mob.getTarget();
            if (victim != null && active.containsKey(victim.getUniqueId())) {
                mob.setTarget(null); mob.getPathfinder().stopPathfinding();
            }
            if (mob instanceof Warden warden) for (Player player : active.values()) warden.clearAnger(player);
            if (mob instanceof Wither wither) for (Wither.Head head : Wither.Head.values()) {
                LivingEntity target = wither.getTarget(head);
                if (target != null && active.containsKey(target.getUniqueId())) wither.setTarget(head, null);
            }
            UUID angry = mob.getMemory(MemoryKey.ANGRY_AT);
            if (angry != null && active.containsKey(angry)) mob.setMemory(MemoryKey.ANGRY_AT, null);
        }
    }
    private void attack(Player player) { end(player, "Attacking revealed you. Your veil has ended.", true, true); }
    @EventHandler(priority = EventPriority.LOWEST)
    public void melee(PrePlayerAttackEntityEvent event) { attack(event.getPlayer()); }
    @EventHandler(priority = EventPriority.LOWEST)
    public void outgoing(EntityDamageByEntityEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.THORNS) return;
        Player attacker = responsiblePlayer(event.getDamager());
        if (attacker != null) attack(attacker);
        // Incoming damage is never cancelled or changed.
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void bow(EntityShootBowEvent event) { if (event.getEntity() instanceof Player player) attack(player); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void projectile(ProjectileLaunchEvent event) {
        Projectile shot = event.getEntity();
        if (shot instanceof EnderPearl || shot instanceof ThrownExpBottle || shot instanceof FishHook) return;
        if (shot.getShooter() instanceof Player player) attack(player);
    }
    private Player responsiblePlayer(Entity source) {
        if (source instanceof Player player) return player;
        if (source instanceof Projectile shot && shot.getShooter() instanceof Player player) return player;
        if (source instanceof AreaEffectCloud cloud && cloud.getSource() instanceof Player player) return player;
        if (source instanceof TNTPrimed tnt && tnt.getSource() instanceof Player player) return player;
        if (source instanceof EvokerFangs fangs && fangs.getOwner() instanceof Player player) return player;
        return null;
    }
    private double price(PurchaseCoordinator.Product ignored) { return getConfig().getDouble("price", 2000.0); }
    private static String money(double value) { return String.format(Locale.US, "%,.2f", value); }
    private void message(Player player, ChatColor color, String title, String detail) {
        player.sendMessage(color + title + ". " + ChatColor.WHITE + detail);
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!Bukkit.isPrimaryThread()) { sender.sendMessage("Run this command on the server thread."); return true; }
        if (args.length == 0) { usage(sender); return true; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status" -> {
                if (sender instanceof Player player) {
                    refreshPlayer(player, false);
                    long left = remainingMillis(player), seconds = left / 1000 + (left % 1000 == 0 ? 0 : 1);
                    player.sendMessage(seconds > 0 ? ChatColor.AQUA + "Veil active: " + seconds + " seconds left. Attacking reveals you."
                            : ChatColor.GRAY + "Veil inactive. /veil buy opens the $" + money(price(PurchaseCoordinator.Product.VEIL)) + " review.");
                } else sender.sendMessage("Only players have a veil status.");
            }
            case "buy" -> {
                Player buyer;
                if (sender instanceof Player player) {
                    if (args.length != 1) { usage(sender); return true; } buyer = player;
                } else {
                    if (!sender.hasPermission("excelmobveil.admin")) { sender.sendMessage("Permission denied."); return true; }
                    if (args.length != 2) { sender.sendMessage("Console: mobveil buy <player>"); return true; }
                    buyer = Bukkit.getPlayerExact(args[1]);
                    if (buyer == null) { sender.sendMessage("Player is not online."); return true; }
                }
                reviewMenu.open(buyer, PurchaseCoordinator.Product.VEIL);
            }
            case "give" -> {
                if (!sender.hasPermission("excelmobveil.admin")) { sender.sendMessage("Permission denied."); return true; }
                if (args.length < 2 || args.length > 3) { usage(sender); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage("Player is not online."); return true; }
                int count;
                try { count = args.length == 3 ? Integer.parseInt(args[2]) : 1; }
                catch (NumberFormatException failure) { sender.sendMessage("Amount must be 1..64."); return true; }
                if (count < 1 || count > 64) { sender.sendMessage("Amount must be 1..64."); return true; }
                int delivered = 0;
                for (int i = 0; i < count; i++) {
                    if (deliverPotion(target) != PurchaseCoordinator.DeliveryOutcome.SUCCESS) break;
                    delivered++;
                }
                sender.sendMessage("Gave " + delivered + " Veil Potion(s); " + (count - delivered) + " not delivered.");
            }
            default -> usage(sender);
        }
        return true;
    }
    private void usage(CommandSender sender) {
        sender.sendMessage("/veil buy | /veil status" + (sender.hasPermission("excelmobveil.admin") ? " | /veil give <player> [amount]" : ""));
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return sender.hasPermission("excelmobveil.admin") ? List.of("buy", "status", "give") : List.of("buy", "status");
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
                    if (!hasPotionSpace(p)) return PurchaseCoordinator.Eligibility.NO_SPACE;
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
                    return deliverPotion(p);
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
    private enum PurchaseMode { QUOTE, PREVIEW, CONFIRM }
    private void initializeReviewMenu() {
        reviewMenu = new PurchaseReviewMenu(new PurchaseReviewMenu.Gateway() {
            @Override public PurchaseCoordinator.Result quote(Player p, PurchaseCoordinator.Product product) {
                return purchaseResult(p, product, PurchaseMode.QUOTE);
            }
            @Override public PurchaseCoordinator.Result preview(Player p, PurchaseCoordinator.Product product) {
                return purchaseResult(p, product, PurchaseMode.PREVIEW);
            }
            @Override public PurchaseCoordinator.Result confirm(Player p, PurchaseCoordinator.Product product) {
                return purchaseResult(p, product, PurchaseMode.CONFIRM);
            }
            @Override public void cancel(UUID id) { if (purchases != null) purchases.cancel(id); }
            @Override public void report(Player p, PurchaseCoordinator.Result result) { reportPurchase(p, result); }
        }, new PurchaseReviewMenu.Host() {
            @Override public Inventory inventory(InventoryHolder holder, String title) {
                return Bukkit.createInventory(holder, 27, title);
            }
            @Override public ItemStack item(Material material, String name, List<String> lore) {
                ItemStack item = new ItemStack(material, 1);
                ItemMeta meta = Objects.requireNonNull(item.getItemMeta());
                meta.setDisplayName(name); meta.setLore(lore); item.setItemMeta(meta); return item;
            }
            @Override public void later(Runnable action) { Bukkit.getScheduler().runTask(ExcelMobVeil.this, action); }
            @Override public void back(Player player) { player.performCommand("shop"); }
        }, System::currentTimeMillis);
        getServer().getPluginManager().registerEvents(reviewMenu, this);
    }
    private PurchaseCoordinator.Result purchaseResult(Player buyer, PurchaseCoordinator.Product product, PurchaseMode mode) {
        double currentPrice = price(product);
        if (purchases == null || economy == null) {
            return new PurchaseCoordinator.Result(PurchaseCoordinator.Status.UNAVAILABLE, product, currentPrice,
                    Double.NaN, null, "Economy or purchase journal unavailable");
        }
        UUID id = buyer.getUniqueId();
        if (purchasePlayers.putIfAbsent(id, buyer) != null) {
            return new PurchaseCoordinator.Result(PurchaseCoordinator.Status.BUSY, product, currentPrice,
                    Double.NaN, null, "Purchase already in progress");
        }
        try {
            return switch (mode) {
                case QUOTE -> purchases.quote(id, product, currentPrice);
                case PREVIEW -> purchases.preview(id, product, currentPrice);
                case CONFIRM -> purchases.confirm(id, product, currentPrice);
            };
        } finally { purchasePlayers.remove(id); }
    }
    private void reportPurchase(Player buyer, PurchaseCoordinator.Result result) {
        switch (result.status()) {
            case PURCHASED -> message(buyer, ChatColor.GREEN, "Veil Potion bought", "Paid $" + money(result.price())
                    + "; balance $" + money(result.balance()) + ". Drink it to activate.");
            case REFUNDED -> message(buyer, ChatColor.YELLOW, "Purchase refunded", "Delivery failed; $" + money(result.price()) + " returned.");
            case LOCKED -> message(buyer, ChatColor.RED, "Purchase needs an admin", "Payment or delivery is uncertain. Do not pay again.");
            case DECLINED -> message(buyer, ChatColor.RED, "Payment declined", "No potion delivered.");
            case INSUFFICIENT_FUNDS -> message(buyer, ChatColor.RED, "Not enough money", "Price $" + money(result.price()) + ".");
            case ARMED -> message(buyer, ChatColor.YELLOW, "Review purchase", "Select Confirm. No charge yet.");
            case INELIGIBLE -> message(buyer, ChatColor.YELLOW, "Cannot buy", "Make an empty inventory slot and try again.");
            default -> message(buyer, ChatColor.YELLOW, "Purchase unavailable", "Try later, or ask an admin.");
        }
    }
}
