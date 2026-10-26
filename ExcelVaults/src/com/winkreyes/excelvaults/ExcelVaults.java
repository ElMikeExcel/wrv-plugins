package com.winkreyes.excelvaults;

import com.winkreyes.excelvaults.core.Pricing;
import com.winkreyes.excelvaults.core.Rules;
import com.winkreyes.excelvaults.core.Store;
import com.winkreyes.excelvaults.core.Texts;
import com.winkreyes.excelvaults.core.TimeFmt;
import com.winkreyes.excelvaults.core.VaultKey;
import io.papermc.paper.event.block.VaultChangeStateEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * ExcelVaults: trial chamber vaults reopen per player after a real-time cooldown.
 * Uses only the Paper Vault block API (hasRewardedPlayer / removeRewardedPlayer): no NBT edits,
 * nothing is unlocked that a player has not opened (or clicked) since install.
 * Ominous vaults wait for the GuildGate seal gate. Players can pay (Vault economy) to skip a cooldown.
 */
public final class ExcelVaults extends JavaPlugin implements Listener {
    private static final LegacyComponentSerializer AMP = LegacyComponentSerializer.legacyAmpersand();

    private Economy econ;
    private boolean floodgate;
    private final AuditHook audit = new AuditHook();
    private final Store store = new Store();
    private File dataFile, journalFile;

    // config
    private boolean enabled, notifyReady;
    private final TypeCfg normal = new TypeCfg(), ominous = new TypeCfg();
    private int minSeals, formDelay;
    private boolean tapForm; private long tapFormCooldownMs;
    private final Texts.TapGate tapGate = new Texts.TapGate();
    private double priceStep;
    private long hintCooldownMs;
    private ZoneId zone = ZoneId.of("America/Chicago");

    static final class TypeCfg {
        long cooldownMs; boolean payEnabled, scale; double full, min; int dailyLimit;
    }

    // runtime
    private final Map<UUID, Long> lastHint = new HashMap<>();
    private final Set<String> notified = new HashSet<>();
    private final Set<UUID> buying = new HashSet<>();
    private Method sealsMethod;
    private Integer sealsCache; private long sealsAt;
    private String sealWarn;

    @Override public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        RegisteredServiceProvider<Economy> rsp = getServer().getPluginManager().getPlugin("Vault") == null ? null
                : getServer().getServicesManager().getRegistration(Economy.class);
        econ = rsp == null ? null : rsp.getProvider();
        if (econ == null) getLogger().warning("No Vault economy provider: pay-to-reopen is disabled (time reopening still works).");
        floodgate = getServer().getPluginManager().getPlugin("floodgate") != null;
        dataFile = new File(getDataFolder(), "data.yml");
        journalFile = new File(getDataFolder(), "purchases.csv");
        loadData();
        getServer().getPluginManager().registerEvents(this, this);
        long sweep = Math.max(1, getConfig().getInt("sweep-seconds", 5)) * 20L;
        getServer().getScheduler().runTaskTimer(this, this::sweep, 40L, sweep);
        getServer().getScheduler().runTaskTimer(this, this::saveIfDirty, 100L, 100L);
        getLogger().info("ExcelVaults enabled: " + summary());
    }

    @Override public void onDisable() { saveData(); }

    String summary() {
        Integer s = seals();
        return "normal " + hours(normal.cooldownMs) + "h, ominous " + hours(ominous.cooldownMs) + "h after seal " + minSeals
                + " (seals " + (s == null ? "unknown" : s + " of 5") + ", ominous " + (Rules.gateOpen(s, minSeals) ? "OPEN" : "waiting") + ")"
                + ", reopen-now $" + money(normal.full) + (normal.scale ? " scaling to min $" + money(normal.min) : " flat") + " / $" + money(ominous.full) + (ominous.scale ? " scaling to min $" + money(ominous.min) : " flat")
                + (econ == null ? " (no economy)" : "") + ", " + store.recordCount() + " cooldowns on " + store.vaultCount() + " vaults"
                + ", Bedrock forms " + (floodgate ? "on" + (tapForm ? " (tap opens reopen window)" : "") : "off") + ", audit " + (audit.available() ? "on" : "off") + (enabled ? "" : ", DISABLED") + ".";
    }

    private static long hours(long ms) { return ms / 3_600_000L; }

    private void loadSettings() {
        reloadConfig();
        FileConfiguration c = getConfig();
        enabled = c.getBoolean("enabled", true);
        readType(c.getConfigurationSection("normal"), normal, 24, 750, 50, 0);
        readType(c.getConfigurationSection("ominous"), ominous, 72, 3000, 200, 2);
        minSeals = c.getInt("ominous.min-seals", 2);
        priceStep = Math.max(0, c.getDouble("price-step", 5));
        notifyReady = c.getBoolean("notify-ready", true);
        hintCooldownMs = Math.max(0, c.getLong("hint-cooldown-seconds", 3)) * 1000L;
        formDelay = Math.max(0, c.getInt("bedrock-form-delay-ticks", 6));
        tapForm = c.getBoolean("bedrock-tap-opens-form", true);
        tapFormCooldownMs = Math.max(0, c.getLong("bedrock-tap-form-cooldown-seconds", 30)) * 1000L;
        try { zone = ZoneId.of(c.getString("timezone", "America/Chicago")); } catch (RuntimeException e) { zone = ZoneId.of("America/Chicago"); }
    }

    private static void readType(ConfigurationSection s, TypeCfg t, int defH, double defFull, double defMin, int defLimit) {
        double h = s == null ? defH : s.getDouble("cooldown-hours", defH);
        t.cooldownMs = (long) (Math.max(0.0167, h) * 3_600_000L);
        ConfigurationSection r = s == null ? null : s.getConfigurationSection("reopen-now");
        t.payEnabled = r == null || r.getBoolean("enabled", true);
        t.full = Math.max(0, r == null ? defFull : r.getDouble("full-price", defFull));
        t.min = Math.max(0, Math.min(t.full, r == null ? defMin : r.getDouble("min-price", defMin)));
        t.scale = r == null || r.getBoolean("scale-with-time-left", true);
        t.dailyLimit = Math.max(0, r == null ? defLimit : r.getInt("daily-limit", defLimit));
    }

    private TypeCfg cfg(boolean om) { return om ? ominous : normal; }

    // ------------------------------------------------------------ seals (ExcelGuildGate)
    Integer seals() {
        long now = System.currentTimeMillis();
        if (now - sealsAt < 5000) return sealsCache;
        sealsAt = now;
        sealsCache = readSeals();
        return sealsCache;
    }
    private Integer readSeals() {
        Plugin gg = Bukkit.getPluginManager().getPlugin("ExcelGuildGate");
        if (gg == null || !gg.isEnabled()) { warnOnce("ExcelGuildGate not found or not enabled: ominous vaults stay vanilla."); return null; }
        try {
            if (sealsMethod == null) {
                Class<?> c = Class.forName("com.winkreyes.guildgate.api.GuildGateAPI", true, gg.getClass().getClassLoader());
                sealsMethod = c.getMethod("sealsBrokenCount");
            }
            return (Integer) sealsMethod.invoke(null);
        } catch (Exception ex) {
            sealsMethod = null;
            warnOnce("Could not read GuildGateAPI.sealsBrokenCount(): " + ex);
            return null;
        }
    }
    private void warnOnce(String m) { if (!m.equals(sealWarn)) { sealWarn = m; getLogger().warning(m); } }
    boolean gate(boolean om) { return !om || Rules.gateOpen(seals(), minSeals); }

    // ------------------------------------------------------------ vault access (Paper API only)
    static VaultKey key(Block b) { return new VaultKey(b.getWorld().getName(), b.getX(), b.getY(), b.getZ()); }

    private static org.bukkit.block.Vault vaultState(Block b) {
        if (b == null || b.getType() != Material.VAULT) return null;
        BlockState s = b.getState();
        return s instanceof org.bukkit.block.Vault v ? v : null;
    }
    private static boolean isOminous(Block b) {
        return b.getBlockData() instanceof org.bukkit.block.data.type.Vault d && d.isOminous();
    }
    private Block block(VaultKey k) {
        World w = Bukkit.getWorld(k.world());
        if (w == null || !w.isChunkLoaded(k.x() >> 4, k.z() >> 4)) return null;
        return w.getBlockAt(k.x(), k.y(), k.z());
    }

    /** Remove the player from the vault's rewarded list (vanilla then lets them use a key again). */
    private static boolean reallow(Block b, UUID p) {
        org.bukkit.block.Vault v = vaultState(b);
        if (v == null) return false;
        if (!v.hasRewardedPlayer(p)) return true;
        v.removeRewardedPlayer(p);
        v.update(true, false);
        org.bukkit.block.Vault check = vaultState(b);
        return check != null && !check.hasRewardedPlayer(p);
    }

    // ------------------------------------------------------------ events
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e) {
        if (!enabled || e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND) return;
        Block b = e.getClickedBlock();
        if (b == null || b.getType() != Material.VAULT) return;
        if (e.useInteractedBlock() == Event.Result.DENY) return;
        org.bukkit.block.Vault v = vaultState(b);
        if (v == null) return;
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        VaultKey k = key(b);
        boolean om = isOminous(b);
        long now = System.currentTimeMillis();
        boolean rewarded = v.hasRewardedPlayer(id);
        Long opened = store.get(k, id);
        Rules.Decision d = Rules.decide(rewarded, opened, om, gate(om), p.isSneaking(), now, cfg(om).cooldownMs);
        if (d.startTimer()) { store.put(k, id, now, om); getLogger().info("Cooldown started on first click after install: " + p.getName() + " at " + k.pretty() + (om ? " (ominous)" : "")); }
        switch (d.action()) {
            case VANILLA -> watchOpen(p, b, k, om);
            case GATED -> { if (hintOk(id)) say(p, "gated", vars(om, 0, 0, k, p)); }
            case REALLOW -> {
                if (reallow(b, id)) { store.remove(k, id); notified.remove(k + "|" + id); getLogger().info("Vault re-allowed on click: " + p.getName() + " at " + k.pretty()); }
                watchOpen(p, b, k, om);
            }
            case HINT -> hint(p, k, om, now);
            case PROMPT -> { e.setCancelled(true); openConfirm(p, k); }
        }
    }

    /** Vanilla adds the player to the rewarded list the moment a key goes in. Check a tick later. */
    private void watchOpen(Player p, Block b, VaultKey k, boolean om) {
        UUID id = p.getUniqueId();
        org.bukkit.block.Vault before = vaultState(b);
        if (before == null || before.hasRewardedPlayer(id)) return;
        Bukkit.getScheduler().runTaskLater(this, () -> {
            org.bukkit.block.Vault after = vaultState(b);
            if (after != null && after.hasRewardedPlayer(id)) recordOpen(p, k, om);
        }, 1L);
    }

    /** Backup signal: Paper fires this when a key unlocks a vault. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVaultState(VaultChangeStateEvent e) {
        if (!enabled || e.getPlayer() == null) return;
        if (e.getNewState() != org.bukkit.block.data.type.Vault.State.UNLOCKING) return;
        Player p = e.getPlayer();
        Block b = e.getBlock();
        Bukkit.getScheduler().runTask(this, () -> {
            org.bukkit.block.Vault v = vaultState(b);
            if (v != null && v.hasRewardedPlayer(p.getUniqueId())) recordOpen(p, key(b), isOminous(b));
        });
    }

    private void recordOpen(Player p, VaultKey k, boolean om) {
        Long had = store.get(k, p.getUniqueId());
        long now = System.currentTimeMillis();
        if (had != null && had > 0 && now - had < 10_000) return; // already recorded by the other signal
        store.put(k, p.getUniqueId(), now, om);
        notified.remove(k + "|" + p.getUniqueId());
        getLogger().info("Vault opened: " + p.getName() + " at " + k.pretty() + (om ? " (ominous)" : "") + ", reopens in " + TimeFmt.hm(cfg(om).cooldownMs)
                + (om && !gate(true) ? " once seal " + minSeals + " is broken" : ""));
    }

    private boolean hintOk(UUID id) {
        long now = System.currentTimeMillis();
        Long last = lastHint.get(id);
        if (last != null && now - last < hintCooldownMs) return false;
        lastHint.put(id, now);
        return true;
    }

    private void hint(Player p, VaultKey k, boolean om, long now) {
        if (!hintOk(p.getUniqueId())) return;
        Long opened = store.get(k, p.getUniqueId());
        long left = opened == null ? cfg(om).cooldownMs : Rules.left(opened, now, cfg(om).cooldownMs);
        double price = price(om, left);
        Map<String, String> v = vars(om, left, price, k, p);
        say(p, "hint", v);
        boolean pay = canPay(om);
        if (!pay) { say(p, "hint-pay-off", v); return; }
        boolean bedrock = bedrock(p);
        say(p, "hint-pay", v);
        if (bedrock) {
            // 0.1.1: chat buttons cannot be clicked on Bedrock. A plain tap opens the same reopen window
            // (two buttons, nothing charged until "Reopen" is tapped), at most once per vault per cooldown.
            if (tapForm && gate(om) && tapGate.allow(p.getUniqueId() + "|" + k, now, tapFormCooldownMs)) {
                say(p, "bedrock-form-note", v);
                openConfirm(p, k);
            }
            return;
        }
        {
            String cmd = "/vaults reopen " + k.world() + " " + k.x() + " " + k.y() + " " + k.z();
            p.sendMessage(AMP.deserialize(fill(msg("hint-button"), v))
                    .clickEvent(ClickEvent.runCommand(cmd))
                    .hoverEvent(HoverEvent.showText(AMP.deserialize(fill(msg("hint-button-hover"), v)))));
        }
    }

    private boolean canPay(boolean om) { return econ != null && cfg(om).payEnabled; }

    double price(boolean om, long left) {
        TypeCfg t = cfg(om);
        return Pricing.price(left, t.cooldownMs, t.full, t.min, t.scale, priceStep);
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) { lastHint.remove(e.getPlayer().getUniqueId()); buying.remove(e.getPlayer().getUniqueId()); tapGate.forgetPlayer(e.getPlayer().getUniqueId() + "|"); }

    // ------------------------------------------------------------ sweep: re-allow ready vaults
    private void sweep() {
        if (!enabled) return;
        long now = System.currentTimeMillis();
        boolean omGate = gate(true);
        for (Store.Entry en : store.all()) {
            if (en.ominous() && !omGate) continue;
            long left = Rules.left(en.openedAt(), now, cfg(en.ominous()).cooldownMs);
            if (left > 0) continue;
            String nk = en.key() + "|" + en.player();
            Block b = block(en.key());
            if (b != null) {
                if (b.getType() != Material.VAULT) { store.removeVault(en.key()); getLogger().warning("Vault at " + en.key().pretty() + " is gone; dropped its cooldowns."); continue; }
                if (reallow(b, en.player())) {
                    store.remove(en.key(), en.player());
                    notified.remove(nk);
                    Player p = Bukkit.getPlayer(en.player());
                    if (notifyReady && p != null && en.openedAt() > 0) say(p, "ready", vars(en.ominous(), 0, 0, en.key(), p));
                    continue;
                }
            }
            // not loaded: tell an online player once; it re-allows the next time the chunk is loaded
            if (notifyReady && en.openedAt() > 0 && notified.add(nk)) {
                Player p = Bukkit.getPlayer(en.player());
                if (p != null) say(p, "ready", vars(en.ominous(), 0, 0, en.key(), p));
                else notified.remove(nk);
            }
        }
    }

    // ------------------------------------------------------------ pay to reopen
    record Quote(boolean ok, String reasonKey, long left, double price, boolean ominous) {}

    Quote quote(Player p, VaultKey k) {
        UUID id = p.getUniqueId();
        Long opened = store.get(k, id);
        boolean om;
        Block b = block(k);
        if (b != null && b.getType() == Material.VAULT) {
            om = isOminous(b);
            org.bukkit.block.Vault v = vaultState(b);
            if (v != null && !v.hasRewardedPlayer(id)) return new Quote(false, "not-locked", 0, 0, om);
            if (opened == null) { // locked before install: start its cooldown now
                opened = System.currentTimeMillis();
                store.put(k, id, opened, om);
            }
        } else {
            if (opened == null) return new Quote(false, "not-locked", 0, 0, false);
            om = store.isOminous(k);
        }
        if (!enabled || !canPay(om)) return new Quote(false, "not-available", 0, 0, om);
        if (!gate(om)) return new Quote(false, "gated", 0, 0, om);
        long left = Rules.left(opened, System.currentTimeMillis(), cfg(om).cooldownMs);
        if (left <= 0) return new Quote(false, "not-locked", 0, 0, om);
        double price = price(om, left);
        int lim = cfg(om).dailyLimit;
        if (lim > 0 && store.paidToday(id, today(), om) >= lim) return new Quote(false, "limit", left, price, om);
        return new Quote(true, null, left, price, om);
    }

    private String today() { return LocalDate.now(zone).toString(); }

    void openConfirm(Player p, VaultKey k) {
        Quote q = quote(p, k);
        Map<String, String> v = vars(q.ominous(), q.left(), q.price(), k, p);
        if (!q.ok()) { say(p, q.reasonKey(), v); return; }
        String body = String.join("\n", lines("confirm-body").stream().map(s -> plain(fill(s, v))).toList())
                + (cfg(q.ominous()).scale && q.price() > cfg(q.ominous()).min ? "\n" + plain(fill(msg("confirm-scaled"), v)) : "")
                + "\n\n" + plain(fill(msg("confirm-balance"), v));
        if (floodgate && BedrockForms.isBedrock(p)) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (!p.isOnline()) return;
                BedrockForms.confirm(p, msg("confirm-title"), body, plain(fill(msg("confirm-buy"), v)), msg("confirm-cancel"),
                        () -> buy(p, k, q.price()), r -> Bukkit.getScheduler().runTask(this, r));
            }, formDelay);
            return;
        }
        ConfirmHolder h = new ConfirmHolder(p.getUniqueId(), k, q.price());
        Inventory inv = Bukkit.createInventory(h, 27, AMP.deserialize("&8" + msg("confirm-title")));
        h.inv = inv;
        List<String> lore = new ArrayList<>();
        for (String s : body.split("\n")) lore.add("&7" + s);
        inv.setItem(13, item(Material.VAULT, "&6&l" + msg("confirm-title"), lore));
        inv.setItem(11, item(Material.LIME_CONCRETE, "&a&l" + fill(msg("confirm-buy"), v), List.of("&7" + k.pretty())));
        inv.setItem(15, item(Material.BARRIER, "&c&l" + msg("confirm-cancel"), List.of("&7Close. No charge.")));
        p.openInventory(inv);
    }

    static final class ConfirmHolder implements InventoryHolder {
        final UUID owner; final VaultKey key; final double shownPrice; Inventory inv; boolean used;
        ConfirmHolder(UUID owner, VaultKey key, double shownPrice) { this.owner = owner; this.key = key; this.shownPrice = shownPrice; }
        @Override public Inventory getInventory() { return inv; }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onConfirmClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof ConfirmHolder h)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(h.owner) || h.used) return;
        int slot = e.getRawSlot();
        if (slot == 11) { h.used = true; Bukkit.getScheduler().runTask(this, () -> { p.closeInventory(); buy(p, h.key, h.shownPrice); }); }
        else if (slot == 15) { h.used = true; Bukkit.getScheduler().runTask(this, () -> p.closeInventory()); }
    }
    @EventHandler public void onConfirmDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof ConfirmHolder) e.setCancelled(true);
    }

    /** Re-quote, charge (never more than the price shown), then reopen. A vault that vanished refunds. */
    void buy(Player p, VaultKey k, double shownPrice) {
        UUID id = p.getUniqueId();
        if (!p.isOnline() || !buying.add(id)) return;
        try {
            Quote q = quote(p, k);
            if (!q.ok()) { say(p, q.reasonKey(), vars(q.ominous(), q.left(), q.price(), k, p)); return; }
            double price = Math.min(q.price(), shownPrice > 0 ? shownPrice : q.price());
            Map<String, String> v = vars(q.ominous(), q.left(), price, k, p);
            if (!econ.has(p, price)) { say(p, "no-money", v); return; }
            EconomyResponse w = econ.withdrawPlayer(p, price);
            if (w == null || !w.transactionSuccess()) { say(p, "no-money", v); return; }
            Block b = block(k);
            String result;
            if (b != null && b.getType() != Material.VAULT) {
                econ.depositPlayer(p, price);
                store.removeVault(k);
                journal(p, k, q, price, "REFUNDED");
                say(p, "refunded", v);
                return;
            }
            store.addPaid(id, today(), q.ominous());
            if (b != null && reallow(b, id)) { store.remove(k, id); result = "REOPENED"; say(p, "reopened", vars(q.ominous(), q.left(), price, k, p)); }
            else { store.put(k, id, 0L, q.ominous()); result = "PENDING"; say(p, "reopened-later", vars(q.ominous(), q.left(), price, k, p)); }
            notified.add(k + "|" + id); // they know already
            journal(p, k, q, price, result);
            saveData();
        } finally { buying.remove(id); }
    }

    private void journal(Player p, VaultKey k, Quote q, double price, String result) {
        String bal = econ == null ? "" : String.format(Locale.US, "%.2f", econ.getBalance(p));
        String line = String.join(",", OffsetDateTime.now(zone).toString(), csv(p.getName()), p.getUniqueId().toString(),
                floodgate && BedrockForms.isBedrock(p) ? "bedrock" : "java", q.ominous() ? "ominous" : "normal", csv(k.pretty()),
                String.format(Locale.US, "%.2f", price), Long.toString(q.left() / 60_000L), bal, result) + "\n";
        try {
            getDataFolder().mkdirs();
            boolean head = !journalFile.exists();
            Files.writeString(journalFile.toPath(), (head ? "timestamp,player,uuid,platform,type,vault,price,minutes_skipped,balance_after,result\n" : "") + line,
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) { getLogger().warning("purchases.csv write failed: " + ex.getMessage()); }
        audit.log(p, "reopen-now " + result.toLowerCase(Locale.ROOT) + " " + (q.ominous() ? "ominous" : "normal") + " vault " + k.pretty()
                + " for $" + String.format(Locale.US, "%.2f", price) + " skipped=" + TimeFmt.hm(q.left()), getLogger());
        getLogger().info("Reopen-now " + result + ": " + p.getName() + " paid $" + money(price) + " for " + (q.ominous() ? "ominous" : "normal") + " vault at " + k.pretty()
                + " (skipped " + TimeFmt.hm(q.left()) + ")");
    }
    static String csv(String s) {
        if (s == null) return "";
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        return s.contains(",") || s.contains("\"") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    // ------------------------------------------------------------ /vaults list
    private void list(Player p) {
        long now = System.currentTimeMillis();
        List<Store.Entry> es = new ArrayList<>(store.forPlayer(p.getUniqueId()));
        es.removeIf(en -> Rules.left(en.openedAt(), now, cfg(en.ominous()).cooldownMs) <= 0 && gate(en.ominous()));
        es.sort(Comparator.comparingLong(en -> Rules.left(en.openedAt(), now, cfg(en.ominous()).cooldownMs)));
        if (es.isEmpty()) { say(p, "list-empty", vars(false, 0, 0, null, p)); return; }
        if (floodgate && BedrockForms.isBedrock(p)) {
            List<String> buttons = new ArrayList<>();
            List<VaultKey> keys = new ArrayList<>();
            for (Store.Entry en : es) {
                long left = Rules.left(en.openedAt(), now, cfg(en.ominous()).cooldownMs);
                boolean g = gate(en.ominous());
                buttons.add(en.key().pretty() + "\n" + (g ? TimeFmt.hm(left) + (canPay(en.ominous()) ? " | $" + money(price(en.ominous(), left)) : "") : "after seal " + minSeals));
                keys.add(en.key());
            }
            Bukkit.getScheduler().runTaskLater(this, () -> BedrockForms.list(p, plain(msg("list-header")), plain(msgFor(p, "list-footer")), buttons,
                    i -> { if (i >= 0 && i < keys.size()) openConfirm(p, keys.get(i)); }, r -> Bukkit.getScheduler().runTask(this, r)), 1L);
            return;
        }
        say(p, "list-header", vars(false, 0, 0, null, p));
        for (Store.Entry en : es) {
            long left = Rules.left(en.openedAt(), now, cfg(en.ominous()).cooldownMs);
            boolean g = gate(en.ominous());
            Map<String, String> v = vars(en.ominous(), left, price(en.ominous(), left), en.key(), p);
            Component line = AMP.deserialize(fill(msg(g ? "list-line" : "list-line-gated"), v));
            if (g && canPay(en.ominous())) {
                String cmd = "/vaults reopen " + en.key().world() + " " + en.key().x() + " " + en.key().y() + " " + en.key().z();
                line = line.append(Component.text(" ")).append(AMP.deserialize(fill(msg("hint-button"), v))
                        .clickEvent(ClickEvent.runCommand(cmd)).hoverEvent(HoverEvent.showText(AMP.deserialize(fill(msg("hint-button-hover"), v)))));
            }
            p.sendMessage(line);
        }
        say(p, "list-footer", vars(false, 0, 0, null, p));
    }

    // ------------------------------------------------------------ commands
    @Override public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        String sub = a.length == 0 ? "list" : a[0].toLowerCase(Locale.ROOT);
        boolean admin = s.hasPermission("excelvaults.admin");
        switch (sub) {
            case "list" -> {
                if (!(s instanceof Player p)) { s.sendMessage("ExcelVaults: " + summary()); return true; }
                list(p); return true;
            }
            case "help" -> {
                Map<String, String> v = new HashMap<>();
                v.put("normal-hours", Long.toString(hours(normal.cooldownMs))); v.put("ominous-hours", Long.toString(hours(ominous.cooldownMs)));
                List<String> help = s instanceof Player hp ? Texts.pickLines("help", bedrock(hp), this::cfgList) : lines("help");
                for (String l : help) s.sendMessage(AMP.deserialize(fill(l, v)));
                return true;
            }
            case "reopen" -> {
                if (!(s instanceof Player p)) { s.sendMessage("Players only."); return true; }
                if (a.length < 5) { s.sendMessage("Usage: /vaults reopen <world> <x> <y> <z>"); return true; }
                VaultKey k;
                try { k = new VaultKey(a[1], Integer.parseInt(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4])); }
                catch (RuntimeException ex) { s.sendMessage("Usage: /vaults reopen <world> <x> <y> <z>"); return true; }
                openConfirm(p, k); return true;
            }
            case "status" -> {
                if (!admin) { s.sendMessage("No permission."); return true; }
                if (a.length < 2) { s.sendMessage("ExcelVaults: " + summary()); return true; }
                OfflinePlayer t = offline(a[1]);
                if (t == null) { s.sendMessage("Unknown player " + a[1]); return true; }
                long now = System.currentTimeMillis();
                List<Store.Entry> es = store.forPlayer(t.getUniqueId());
                s.sendMessage("ExcelVaults " + a[1] + ": " + es.size() + " cooldowns, paid today normal " + store.paidToday(t.getUniqueId(), today(), false)
                        + " ominous " + store.paidToday(t.getUniqueId(), today(), true));
                for (Store.Entry en : es) {
                    long left = Rules.left(en.openedAt(), now, cfg(en.ominous()).cooldownMs);
                    s.sendMessage(" - " + en.key().pretty() + (en.ominous() ? " ominous" : " normal") + (en.openedAt() == 0 ? " PENDING-REOPEN" : left <= 0 ? " READY" : " left=" + TimeFmt.hm(left))
                            + (gate(en.ominous()) ? "" : " (seal gate closed)"));
                }
                return true;
            }
            case "reset" -> {
                if (!admin) { s.sendMessage("No permission."); return true; }
                if (a.length < 2) { s.sendMessage("Usage: vaults reset <player> [here]"); return true; }
                OfflinePlayer t = offline(a[1]);
                if (t == null) { s.sendMessage("Unknown player " + a[1]); return true; }
                if (a.length > 2 && a[2].equalsIgnoreCase("here")) {
                    if (!(s instanceof Player sp)) { s.sendMessage("'here' needs you in game, looking at a vault."); return true; }
                    Block b = sp.getTargetBlockExact(6);
                    if (b == null || b.getType() != Material.VAULT) { s.sendMessage("Look at a vault (within 6 blocks)."); return true; }
                    boolean okR = reallow(b, t.getUniqueId());
                    store.remove(key(b), t.getUniqueId());
                    s.sendMessage(okR ? "Reopened " + key(b).pretty() + " for " + a[1] + " (ignores the seal gate)." : "Could not reopen that vault.");
                    getLogger().info("Admin reset here by " + s.getName() + ": " + a[1] + " at " + key(b).pretty() + " ok=" + okR);
                    return true;
                }
                int n = store.resetPlayer(t.getUniqueId());
                store.clearPaid(t.getUniqueId());
                saveData();
                s.sendMessage("Marked " + n + " recorded vaults of " + a[1] + " to reopen now (seal gate still applies to ominous) and cleared today's paid-reopen count. Unloaded ones reopen when their area loads.");
                getLogger().info("Admin reset by " + s.getName() + ": " + a[1] + ", " + n + " vaults");
                sweep();
                return true;
            }
            case "age" -> { // support/test: pretend this player's vaults were opened N minutes earlier
                if (!admin) { s.sendMessage("No permission."); return true; }
                if (a.length < 3) { s.sendMessage("Usage: vaults age <player> <minutes>"); return true; }
                OfflinePlayer t = offline(a[1]);
                if (t == null) { s.sendMessage("Unknown player " + a[1]); return true; }
                long min;
                try { min = Long.parseLong(a[2]); } catch (NumberFormatException ex) { s.sendMessage("minutes must be a number"); return true; }
                int n = store.age(t.getUniqueId(), min * 60_000L);
                s.sendMessage("Moved " + n + " cooldowns of " + a[1] + " back " + min + " min.");
                getLogger().info("Admin age by " + s.getName() + ": " + a[1] + " -" + min + " min on " + n + " cooldowns");
                return true;
            }
            case "reload" -> {
                if (!admin) { s.sendMessage("No permission."); return true; }
                loadSettings();
                sealsAt = 0;
                s.sendMessage("ExcelVaults reloaded: " + summary());
                return true;
            }
            default -> { s.sendMessage("Usage: /vaults [list|help]"); return true; }
        }
    }

    @SuppressWarnings("deprecation")
    private static OfflinePlayer offline(String name) {
        Player on = Bukkit.getPlayerExact(name);
        if (on != null) return on;
        OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(name);
        return op;
    }

    @Override public List<String> onTabComplete(CommandSender s, Command c, String alias, String[] a) {
        if (a.length != 1) return List.of();
        List<String> out = new ArrayList<>(List.of("list", "help"));
        if (s.hasPermission("excelvaults.admin")) out.addAll(List.of("status", "reset", "age", "reload"));
        out.removeIf(x -> !x.startsWith(a[0].toLowerCase(Locale.ROOT)));
        return out;
    }

    // ------------------------------------------------------------ data
    private void loadData() {
        if (!dataFile.exists()) return;
        YamlConfiguration y = new YamlConfiguration();
        y.options().pathSeparator('\u0001'); // world names may contain '.'
        try { y.loadFromString(Files.readString(dataFile.toPath(), StandardCharsets.UTF_8)); }
        catch (Exception ex) {
            File keep = new File(getDataFolder(), "data.yml.unreadable-" + System.currentTimeMillis());
            dataFile.renameTo(keep);
            getLogger().severe("data.yml could not be read (" + ex.getMessage() + "); kept as " + keep.getName() + ", starting with no cooldowns.");
            return;
        }
        int skipped = store.fromMap(toPlain(y.getValues(false)));
        if (skipped > 0) getLogger().warning("data.yml: skipped " + skipped + " bad entries.");
    }
    private static Map<String, Object> toPlain(Map<String, Object> m) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : m.entrySet()) {
            Object v = e.getValue();
            out.put(e.getKey(), v instanceof ConfigurationSection cs ? toPlain(cs.getValues(false)) : v);
        }
        return out;
    }
    private void saveIfDirty() { if (store.dirty()) saveData(); }
    /** Atomic: write data.yml.tmp then move over data.yml. */
    void saveData() {
        YamlConfiguration y = new YamlConfiguration();
        y.options().pathSeparator('\u0001');
        for (Map.Entry<String, Object> e : store.toMap().entrySet()) y.set(e.getKey(), e.getValue());
        try {
            getDataFolder().mkdirs();
            File tmp = new File(getDataFolder(), "data.yml.tmp");
            Files.writeString(tmp.toPath(), "# ExcelVaults cooldowns. openedAt is epoch ms; 0 = reopen pending.\n" + y.saveToString(), StandardCharsets.UTF_8);
            try { Files.move(tmp.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (IOException atomicFail) { Files.move(tmp.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING); }
            store.clean();
        } catch (IOException ex) { getLogger().warning("data.yml save failed: " + ex.getMessage()); }
    }

    // ------------------------------------------------------------ text helpers
    private String msg(String k) { return getConfig().getString("messages." + k, k); }
    private List<String> lines(String k) {
        List<String> l = getConfig().getStringList("messages." + k);
        return l.isEmpty() ? List.of(msg(k)) : l;
    }
    private Map<String, String> vars(boolean om, long left, double price, VaultKey k, Player p) {
        Map<String, String> v = new HashMap<>();
        v.put("left", TimeFmt.hm(left));
        v.put("price", money(price));
        v.put("min", money(cfg(om).min));
        v.put("key", msg(om ? "key-ominous" : "key-normal"));
        v.put("type", msg(om ? "type-ominous" : "type-normal"));
        v.put("where", k == null ? "" : k.pretty());
        v.put("balance", econ == null || p == null ? "?" : money(econ.getBalance(p)));
        v.put("limit", Integer.toString(cfg(om).dailyLimit));
        v.put("count", p == null ? "0" : Integer.toString(store.paidToday(p.getUniqueId(), today(), om)));
        return v;
    }
    static String fill(String s, Map<String, String> v) {
        for (Map.Entry<String, String> e : v.entrySet()) s = s.replace("{" + e.getKey() + "}", e.getValue());
        return s;
    }
    private void say(Player p, String key, Map<String, String> v) { p.sendMessage(AMP.deserialize(fill(msgFor(p, key), v))); }
    private boolean bedrock(Player p) { return floodgate && BedrockForms.isBedrock(p); }
    /** 0.1.1: Bedrock version of hint-pay / list-footer (config messages.<key>-bedrock, else the code default). */
    private String msgFor(Player p, String key) {
        if (key.equals("bedrock-form-note")) { String o = getConfig().getString("messages.bedrock-form-note"); return o != null ? o : Texts.BEDROCK_FORM_NOTE; }
        return Texts.pick(key, p != null && bedrock(p), k -> getConfig().getString("messages." + k));
    }
    private List<String> cfgList(String k) { return getConfig().getStringList("messages." + k); }
    static String plain(String s) { return s.replaceAll("&[0-9a-fk-orA-FK-OR]", ""); }
    static String money(double d) {
        if (Math.abs(d - Math.rint(d)) < 0.005) return String.format(Locale.US, "%,d", (long) Math.rint(d));
        return String.format(Locale.US, "%,.2f", d);
    }
    private static ItemStack item(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(AMP.deserialize("&r" + name));
        List<Component> l = new ArrayList<>();
        for (String s : lore) l.add(AMP.deserialize(s));
        meta.lore(l);
        it.setItemMeta(meta);
        return it;
    }
}
