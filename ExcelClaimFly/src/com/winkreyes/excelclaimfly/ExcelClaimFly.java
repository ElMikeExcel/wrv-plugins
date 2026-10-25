package com.winkreyes.excelclaimfly;

import com.winkreyes.excelclaimfly.core.CombatClock;
import com.winkreyes.excelclaimfly.core.FlightRules;
import com.winkreyes.excelclaimfly.core.FlightRules.Block;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * ExcelClaimFly: the Build Flight pass. Fly only inside your own
 * GriefPrevention claim while a LuckPerms temporary node is active.
 * Leaving the claim, a fight (hit by a mob, projectile or player, or
 * attacking) or the pass running out turns flight off and the player
 * lands with no fall damage. Java and Bedrock (Geyser) behave the same:
 * the server only toggles allow-flight; the client's own double-tap
 * jump starts flying.
 */
public final class ExcelClaimFly extends JavaPlugin implements Listener {
    private static final LegacyComponentSerializer AMP = LegacyComponentSerializer.legacyAmpersand();

    private Economy econ;
    private LuckPermsPasses passes;
    private final CombatClock combat = new CombatClock(10_000);
    private boolean floodgate;

    // config
    private boolean enabled, trustedBuilders, combatRule, actionbar;
    private double price;
    private int passMinutes, maxTotalMinutes, formDelay;
    private long safeMaxMs, hintCooldownMs;
    private int[] warnMinutes = {5, 1};
    private Set<String> blockedWorlds = new HashSet<>();

    // state
    private final Set<UUID> managed = new HashSet<>();          // we turned allow-flight on
    private final Set<UUID> landSafeOnJoin = new HashSet<>();   // were flying when they left / plugin stopped
    private final Map<UUID, Long> safeUntil = new HashMap<>();
    private final Map<UUID, Integer> groundRuns = new HashMap<>();
    private final Map<UUID, Long> lastRemaining = new HashMap<>();
    private final Map<UUID, Long> warnedFor = new HashMap<>();   // pass end (ms, rounded) the warnings belong to
    private final Map<UUID, Integer> warnedMin = new HashMap<>();
    private final Map<UUID, Block> lastBlock = new HashMap<>();
    private final Map<UUID, Long> lastHint = new HashMap<>();
    private final Set<UUID> pausedWhileFlying = new HashSet<>();
    private final Map<UUID, Claim> claimCache = new HashMap<>();
    private final Set<UUID> buying = new HashSet<>();
    private File stateFile, journalFile;

    @Override public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        econ = rsp == null ? null : rsp.getProvider();
        if (econ == null) getLogger().warning("No Vault economy provider: purchases are disabled.");
        passes = new LuckPermsPasses(getConfig().getString("pass-node", "excelclaimfly.pass"));
        floodgate = getServer().getPluginManager().getPlugin("floodgate") != null;
        stateFile = new File(getDataFolder(), "state.yml");
        journalFile = new File(getDataFolder(), "purchases.log");
        loadState();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, this::tick, 20L, 5L);
        for (Player p : Bukkit.getOnlinePlayers()) if (landSafeOnJoin.remove(p.getUniqueId())) safeLanding(p);
        getLogger().info("ExcelClaimFly enabled: price $" + money(price) + ", " + passMinutes + " min pass, cap "
                + maxTotalMinutes + " min, combat pause " + (combatRule ? "ON" : "OFF") + ", owner"
                + (trustedBuilders ? "+trusted builders" : " only") + ", Bedrock forms " + (floodgate ? "on" : "off") + ".");
    }

    @Override public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            if (!managed.contains(id)) continue;
            if (p.isFlying()) landSafeOnJoin.add(id);
            p.setFlying(false); p.setAllowFlight(false);
            managed.remove(id);
        }
        saveState();
    }

    private void loadSettings() {
        reloadConfig();
        FileConfiguration c = getConfig();
        enabled = c.getBoolean("enabled", true);
        price = c.getDouble("price", 1000);
        passMinutes = Math.max(1, c.getInt("pass-minutes", 30));
        maxTotalMinutes = Math.max(passMinutes, c.getInt("max-total-minutes", 120));
        trustedBuilders = c.getBoolean("trusted-builders-can-fly", false);
        blockedWorlds = new HashSet<>();
        for (String w : c.getStringList("blocked-worlds")) blockedWorlds.add(w.toLowerCase(Locale.ROOT));
        combatRule = c.getBoolean("combat.pause-flight", true);
        combat.setQuietMs((long) (c.getDouble("combat.tag-seconds", 10) * 1000));
        safeMaxMs = (long) (c.getDouble("safe-landing-max-seconds", 30) * 1000);
        List<Integer> w = c.getIntegerList("warn-minutes");
        warnMinutes = w.isEmpty() ? new int[]{5, 1} : w.stream().mapToInt(Integer::intValue).toArray();
        actionbar = c.getBoolean("actionbar", true);
        hintCooldownMs = (long) (c.getDouble("reentry-hint-cooldown-seconds", 60) * 1000);
        formDelay = Math.max(1, c.getInt("bedrock-form-delay-ticks", 10));
    }

    // ---------------------------------------------------------------- text
    private String fill(String s, Player p, long remaining) {
        return s.replace("{price}", money(price)).replace("{minutes}", String.valueOf(passMinutes))
                .replace("{left}", FlightRules.words(remaining)).replace("{clock}", FlightRules.clock(remaining))
                .replace("{cap}", FlightRules.words(maxTotalMinutes * 60_000L))
                .replace("{balance}", econ == null || p == null ? "?" : money(econ.getBalance(p)));
    }
    private String msg(String key) { return getConfig().getString("messages." + key, ""); }
    private List<String> lines(String key) { return getConfig().getStringList("messages." + key); }
    private void say(Player p, String key, long rem) { String s = msg(key); if (!s.isEmpty()) p.sendMessage(AMP.deserialize(fill(s, p, rem))); }
    private void sayLines(Player p, String key, long rem) { for (String s : lines(key)) p.sendMessage(AMP.deserialize(fill(s, p, rem))); }
    private void title(Player p, String tKey, String sKey, long rem) {
        p.showTitle(Title.title(AMP.deserialize(fill(msg(tKey), p, rem)), AMP.deserialize(fill(msg(sKey), p, rem)),
                Title.Times.times(Duration.ofMillis(250), Duration.ofMillis(3500), Duration.ofMillis(750))));
    }
    static String money(double v) {
        return v == Math.rint(v) ? String.format(Locale.US, "%,d", (long) v) : String.format(Locale.US, "%,.2f", v);
    }
    private static String plain(String s) { return s.replaceAll("&[0-9a-fk-orA-FK-OR]", ""); }

    // ---------------------------------------------------------------- claims
    boolean inOwnClaim(Player p) {
        GriefPrevention gp = GriefPrevention.instance;
        if (gp == null || gp.dataStore == null) return false;
        Claim c = gp.dataStore.getClaimAt(p.getLocation(), true, claimCache.get(p.getUniqueId()));
        if (c == null) { claimCache.remove(p.getUniqueId()); return false; }
        claimCache.put(p.getUniqueId(), c);
        Claim top = c; while (top.parent != null) top = top.parent;
        if (top.isAdminClaim()) return false;
        if (p.getUniqueId().equals(top.getOwnerID())) return true;
        return trustedBuilders && c.checkPermission(p, ClaimPermission.Build, null) == null;
    }
    boolean ownsAnyClaim(Player p) {
        GriefPrevention gp = GriefPrevention.instance;
        if (gp == null || gp.dataStore == null) return false;
        for (Claim c : gp.dataStore.getPlayerData(p.getUniqueId()).getClaims()) if (!c.isAdminClaim()) return true;
        return false;
    }

    // ---------------------------------------------------------------- core loop (every 5 ticks)
    private void tick() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            landingCheck(p, now);
            boolean wasManaged = managed.contains(id);
            long rem = passes.remainingMs(id);
            long before = lastRemaining.getOrDefault(id, 0L);
            if (rem > 0) lastRemaining.put(id, rem); else lastRemaining.remove(id);
            if (rem <= 0 && !wasManaged) {
                if (before > 0) { // ran out while not flying (e.g. outside claim)
                    say(p, "expired-chat", 0);
                }
                lastBlock.remove(id); pausedWhileFlying.remove(id);
                continue;
            }
            GameMode gm = p.getGameMode();
            boolean survival = gm == GameMode.SURVIVAL || gm == GameMode.ADVENTURE;
            boolean worldBlocked = blockedWorlds.contains(p.getWorld().getName().toLowerCase(Locale.ROOT));
            boolean inClaim = rem > 0 && survival && !worldBlocked && inOwnClaim(p);
            Block b = FlightRules.decide(new FlightRules.Facts(rem > 0 && enabled, survival, worldBlocked, inClaim,
                    combat.tagged(id, now), combatRule));
            Block prev = lastBlock.put(id, b);

            if (b == Block.OTHER_MODE) { managed.remove(id); continue; } // creative/spectator own their flight
            if (b == Block.NONE) {
                if (!wasManaged) {
                    if (!p.getAllowFlight()) {
                        p.setAllowFlight(true); managed.add(id);
                        if (pausedWhileFlying.remove(id)) say(p, "combat-resumed", rem);
                        else if (prev != null && prev != Block.NONE && prev != Block.NO_PASS && hintDue(id, now)) say(p, "reentered-claim", rem);
                    } // else someone else (an admin /fly) gave flight: leave it alone
                } else if (!p.getAllowFlight()) p.setAllowFlight(true);
            } else if (wasManaged) {
                boolean flying = p.isFlying();
                stopFlight(p, flying);
                if (b == Block.NO_PASS && rem > 0) say(p, "not-available", rem); // switched off by config
                else if (b == Block.NO_PASS) { title(p, "expired-title", "expired-subtitle", 0); say(p, "expired-chat", 0); }
                else if (flying) switch (b) {
                    case OUTSIDE_CLAIM -> say(p, "left-claim", rem);
                    case BLOCKED_WORLD -> say(p, "world-blocked", rem);
                    case COMBAT -> { pausedWhileFlying.add(id); say(p, "attack-paused", rem); }
                    default -> {}
                }
            }
            if (rem > 0 && rem != Long.MAX_VALUE) warnings(p, id, rem, now);
            if (actionbar && rem > 0 && rem != Long.MAX_VALUE) {
                if (p.isFlying() && managed.contains(id)) p.sendActionBar(AMP.deserialize(fill(msg("actionbar"), p, rem)));
                else if (b == Block.COMBAT && inClaim && pausedWhileFlying.contains(id)) p.sendActionBar(AMP.deserialize(fill(msg("actionbar-paused"), p, rem)));
            }
        }
    }

    private boolean hintDue(UUID id, long now) {
        Long t = lastHint.get(id);
        if (t != null && now - t < hintCooldownMs) return false;
        lastHint.put(id, now); return true;
    }

    private void warnings(Player p, UUID id, long rem, long now) {
        long end = (now + rem) / 30_000L; // 30 s buckets: an extension moves the end and re-arms warnings
        Long had = warnedFor.get(id);
        // A new end time (purchase, extension, rejoin, admin grant) re-arms the warnings. If the pass already has less
        // than a threshold left, warningDue() picks only the smallest passed threshold, so the player gets one warning now.
        if (had == null || Math.abs(had - end) > 1) { warnedFor.put(id, end); warnedMin.put(id, Integer.MAX_VALUE); }
        int due = FlightRules.warningDue(rem, warnedMin.getOrDefault(id, Integer.MAX_VALUE), warnMinutes);
        if (due > 0) { warnedMin.put(id, due); title(p, "warn-title", "warn-subtitle", rem); say(p, "warn-chat", rem); }
    }

    private void stopFlight(Player p, boolean landSafe) {
        UUID id = p.getUniqueId();
        managed.remove(id);
        p.setFlying(false);
        p.setAllowFlight(false);
        if (landSafe || p.getFallDistance() > 3) safeLanding(p);
    }

    private void safeLanding(Player p) {
        safeUntil.put(p.getUniqueId(), System.currentTimeMillis() + safeMaxMs);
        groundRuns.remove(p.getUniqueId());
    }

    @SuppressWarnings("deprecation")
    private void landingCheck(Player p, long now) {
        UUID id = p.getUniqueId();
        Long until = safeUntil.get(id);
        if (until == null) return;
        if (now > until) { safeUntil.remove(id); groundRuns.remove(id); return; }
        if (p.isOnGround() || p.isInWater() || p.isFlying()) {
            int n = groundRuns.merge(id, 1, Integer::sum);
            if (n >= 3) { safeUntil.remove(id); groundRuns.remove(id); }
        } else groundRuns.remove(id);
    }

    // ---------------------------------------------------------------- events
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFall(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL || !(e.getEntity() instanceof Player p)) return;
        if (safeUntil.remove(p.getUniqueId()) != null) { e.setCancelled(true); groundRuns.remove(p.getUniqueId()); }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        Entity source = damager;
        if (damager instanceof Projectile proj) {
            ProjectileSource s = proj.getShooter();
            source = s instanceof Entity ent ? ent : null;
        }
        long now = System.currentTimeMillis();
        if (e.getEntity() instanceof Player victim && (damager instanceof Projectile || damager instanceof LivingEntity)
                && (source == null || !source.getUniqueId().equals(victim.getUniqueId()))) {
            combat.tag(victim.getUniqueId(), now);
            if (combatRule && managed.contains(victim.getUniqueId())) {
                boolean flying = victim.isFlying();
                stopFlight(victim, true);
                lastBlock.put(victim.getUniqueId(), Block.COMBAT);
                if (flying) { pausedWhileFlying.add(victim.getUniqueId()); say(victim, "hit-paused", 0); }
            }
        }
        if (source instanceof Player attacker && e.getEntity() instanceof LivingEntity
                && !e.getEntity().getUniqueId().equals(attacker.getUniqueId())) {
            combat.tag(attacker.getUniqueId(), now);
            if (combatRule && managed.contains(attacker.getUniqueId())) {
                boolean flying = attacker.isFlying();
                stopFlight(attacker, true);
                lastBlock.put(attacker.getUniqueId(), Block.COMBAT);
                if (flying) { pausedWhileFlying.add(attacker.getUniqueId()); say(attacker, "attack-paused", 0); }
            }
        }
    }

    @EventHandler public void onDeath(PlayerDeathEvent e) {
        UUID id = e.getEntity().getUniqueId();
        combat.clear(id); safeUntil.remove(id); pausedWhileFlying.remove(id);
    }

    @EventHandler public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer(); UUID id = p.getUniqueId();
        if (landSafeOnJoin.remove(id)) safeLanding(p);
        lastRemaining.remove(id); warnedFor.remove(id);
        // A stale allow-flight from before a restart is cleared by the next tick (managed set is persisted).
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer(); UUID id = p.getUniqueId();
        if (managed.contains(id) && (p.isFlying() || safeUntil.containsKey(id))) landSafeOnJoin.add(id);
        claimCache.remove(id); safeUntil.remove(id); groundRuns.remove(id); lastBlock.remove(id); buying.remove(id);
        saveState();
    }

    // ---------------------------------------------------------------- purchase
    private record Quote(boolean ok, String reasonKey, long remaining, double balance) {}

    private Quote quote(Player p) {
        long rem = passes.remainingMs(p.getUniqueId());
        double bal = econ == null ? 0 : econ.getBalance(p);
        if (!enabled || econ == null) return new Quote(false, "not-available", rem, bal);
        if (!trustedBuilders && !ownsAnyClaim(p)) return new Quote(false, "no-claim", rem, bal);
        if (rem == Long.MAX_VALUE || !FlightRules.canExtend(rem, passMinutes * 60_000L, maxTotalMinutes * 60_000L))
            return new Quote(false, "at-cap", rem, bal);
        if (bal < price) return new Quote(false, "no-money", rem, bal);
        return new Quote(true, null, rem, bal);
    }

    private String confirmBody(Player p, Quote q) {
        StringBuilder sb = new StringBuilder();
        for (String s : lines("confirm-body")) sb.append(plain(fill(s, p, q.remaining()))).append('\n');
        if (q.remaining() > 0) sb.append(plain(fill(msg("confirm-body-active"), p, q.remaining()))).append('\n');
        sb.append('\n').append(plain(fill(msg("confirm-balance"), p, q.remaining())));
        if (!q.ok()) sb.append("\n\n").append(plain(fill(msg(q.reasonKey()), p, q.remaining())));
        return sb.toString();
    }

    void openConfirm(Player p) {
        Quote q = quote(p);
        if (floodgate && BedrockConfirm.isBedrock(p)) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (!p.isOnline()) return;
                String body = confirmBody(p, q);
                if (q.ok()) BedrockConfirm.send(p, msg("confirm-title"), body, plain(fill(msg("confirm-buy"), p, q.remaining())),
                        msg("confirm-cancel"), () -> buy(p), r -> Bukkit.getScheduler().runTask(this, r));
                else BedrockConfirm.info(p, msg("confirm-title"), body, "OK");
            }, formDelay);
            return;
        }
        Bukkit.getScheduler().runTask(this, () -> {
            if (!p.isOnline()) return;
            ConfirmHolder h = new ConfirmHolder(p.getUniqueId());
            Inventory inv = Bukkit.createInventory(h, 27, AMP.deserialize("&8" + msg("confirm-title")));
            h.inv = inv;
            List<String> lore = new ArrayList<>();
            for (String s : confirmBody(p, q).split("\n")) lore.add("&7" + s);
            inv.setItem(13, item(Material.FEATHER, "&b&l" + msg("confirm-title"), lore));
            if (q.ok()) inv.setItem(11, item(Material.LIME_CONCRETE, "&a&l" + fill(msg("confirm-buy"), p, q.remaining()),
                    List.of("&7Charges only when you click this.")));
            else inv.setItem(11, item(Material.RED_CONCRETE, "&c" + plain(fill(msg(q.reasonKey()), p, q.remaining())), List.of("&7No money taken.")));
            inv.setItem(15, item(Material.BARRIER, "&c&l" + msg("confirm-cancel"), List.of("&7Close. No charge.")));
            p.openInventory(inv);
        });
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

    static final class ConfirmHolder implements InventoryHolder {
        final UUID owner; Inventory inv; boolean used;
        ConfirmHolder(UUID owner) { this.owner = owner; }
        @Override public Inventory getInventory() { return inv; }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onConfirmClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof ConfirmHolder h)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(h.owner) || h.used) return;
        int slot = e.getRawSlot();
        if (slot == 11 && e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.LIME_CONCRETE) {
            h.used = true;
            Bukkit.getScheduler().runTask(this, () -> { p.closeInventory(); buy(p); });
        } else if (slot == 15 || slot == 11) {
            h.used = true;
            Bukkit.getScheduler().runTask(this, () -> p.closeInventory());
        }
    }
    @EventHandler public void onConfirmDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof ConfirmHolder) e.setCancelled(true);
    }

    /** Charge then grant, as one guarded step. A failed grant refunds. */
    void buy(Player p) {
        UUID id = p.getUniqueId();
        if (!p.isOnline() || !buying.add(id)) return;
        try {
            Quote q = quote(p);
            if (!q.ok()) { say(p, q.reasonKey(), q.remaining()); return; }
            EconomyResponse w = econ.withdrawPlayer(p, price);
            if (w == null || !w.transactionSuccess()) { say(p, "no-money", q.remaining()); return; }
            long now = passes.add(id, Duration.ofMinutes(passMinutes));
            if (now < 0) {
                econ.depositPlayer(p, price);
                say(p, "refunded", 0);
                journal(p, "REFUNDED", 0);
                return;
            }
            journal(p, q.remaining() > 0 ? "EXTEND" : "BUY", now);
            lastRemaining.put(id, now);
            if (q.remaining() > 0) { title(p, "extended-title", "extended-subtitle", now); sayLines(p, "extended-chat", now); }
            else { title(p, "bought-title", "bought-subtitle", now); sayLines(p, "bought-chat", now); }
        } finally { buying.remove(id); }
    }

    private void journal(Player p, String what, long remaining) {
        try (FileWriter w = new FileWriter(journalFile, true)) {
            w.write(OffsetDateTime.now() + "\t" + what + "\t" + p.getUniqueId() + "\t" + p.getName() + "\t$" + money(price)
                    + "\tleft=" + FlightRules.clock(remaining) + "\tbalance=$" + (econ == null ? "?" : money(econ.getBalance(p))) + "\n");
        } catch (IOException ex) { getLogger().warning("purchase journal write failed: " + ex.getMessage()); }
    }

    // ---------------------------------------------------------------- state
    private void loadState() {
        if (!stateFile.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(stateFile);
        for (String s : y.getStringList("managed")) try { managed.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        for (String s : y.getStringList("land-safe-on-join")) try { landSafeOnJoin.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
    }
    private void saveState() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("managed", managed.stream().map(UUID::toString).sorted().toList());
        y.set("land-safe-on-join", landSafeOnJoin.stream().map(UUID::toString).sorted().toList());
        try { getDataFolder().mkdirs(); y.save(stateFile); } catch (IOException ex) { getLogger().warning("state save failed: " + ex.getMessage()); }
    }

    // ---------------------------------------------------------------- command
    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {
        String sub = a.length == 0 ? "status" : a[0].toLowerCase(Locale.ROOT);
        boolean admin = sender.hasPermission("excelclaimfly.admin");
        switch (sub) {
            case "status", "time", "left" -> {
                Player t = a.length > 1 && admin ? Bukkit.getPlayerExact(a[1]) : sender instanceof Player sp ? sp : null;
                if (t == null) { sender.sendMessage("Usage: buildfly status <player>"); return true; }
                long rem = passes.remainingMs(t.getUniqueId());
                if (rem <= 0) { sender.sendMessage(AMP.deserialize(fill(msg("status-none"), t, 0))); return true; }
                Block b = lastBlock.getOrDefault(t.getUniqueId(), Block.NONE);
                String where = switch (b) {
                    case NONE -> msg("status-where-on"); case COMBAT -> msg("status-where-combat");
                    case BLOCKED_WORLD -> msg("status-where-world"); case OTHER_MODE -> msg("status-where-mode");
                    default -> msg("status-where-outside");
                };
                sender.sendMessage(AMP.deserialize(fill(msg("status-active"), t, rem).replace("{where}", where)));
                return true;
            }
            case "help", "how" -> {
                if (sender instanceof Player p) sayLines(p, "bought-chat", passMinutes * 60_000L);
                return true;
            }
            case "buy" -> {
                Player t;
                if (a.length > 1) { if (!admin) { sender.sendMessage("No permission."); return true; } t = Bukkit.getPlayerExact(a[1]); }
                else t = sender instanceof Player sp ? sp : null;
                if (t == null) { sender.sendMessage("Player not online."); return true; }
                openConfirm(t);
                return true;
            }
            case "confirm" -> { // admin/test hook: run the purchase without the screen
                if (!admin || a.length < 2) { sender.sendMessage("No permission."); return true; }
                Player t = Bukkit.getPlayerExact(a[1]);
                if (t == null) { sender.sendMessage("Player not online."); return true; }
                buy(t); sender.sendMessage("buy attempted for " + t.getName() + "; left now " + FlightRules.clock(passes.remainingMs(t.getUniqueId())));
                return true;
            }
            case "give" -> {
                if (!admin || a.length < 3) { sender.sendMessage("Usage: buildfly give <player> <minutes>"); return true; }
                Player t = Bukkit.getPlayerExact(a[1]);
                if (t == null) { sender.sendMessage("Player not online."); return true; }
                long r = passes.add(t.getUniqueId(), Duration.ofMinutes(Long.parseLong(a[2])));
                sender.sendMessage("Gave " + a[2] + " min Build Flight to " + t.getName() + ", left now " + FlightRules.clock(r));
                return true;
            }
            case "end" -> {
                if (!admin || a.length < 2) { sender.sendMessage("Usage: buildfly end <player>"); return true; }
                Player t = Bukkit.getPlayerExact(a[1]);
                if (t == null) { sender.sendMessage("Player not online."); return true; }
                sender.sendMessage(passes.end(t.getUniqueId()) ? "Ended Build Flight for " + t.getName() : "No temporary pass on " + t.getName());
                return true;
            }
            case "info" -> {
                if (!admin || a.length < 2) { sender.sendMessage("Usage: buildfly info <player>"); return true; }
                Player t = Bukkit.getPlayerExact(a[1]);
                if (t == null) { sender.sendMessage("Player not online."); return true; }
                UUID id = t.getUniqueId(); long now = System.currentTimeMillis();
                sender.sendMessage("ClaimFly info " + t.getName() + ": left=" + FlightRules.clock(passes.remainingMs(id))
                        + " block=" + lastBlock.get(id) + " managed=" + managed.contains(id) + " allowFlight=" + t.getAllowFlight()
                        + " flying=" + t.isFlying() + " inOwnClaim=" + inOwnClaim(t) + " combat=" + combat.tagged(id, now)
                        + " safeLanding=" + safeUntil.containsKey(id) + " bedrock=" + (floodgate && BedrockConfirm.isBedrock(t)));
                return true;
            }
            case "reload" -> {
                if (!admin) { sender.sendMessage("No permission."); return true; }
                loadSettings();
                sender.sendMessage("ExcelClaimFly reloaded: price $" + money(price) + ", " + passMinutes + " min, cap " + maxTotalMinutes
                        + " min, combat pause " + (combatRule ? "ON" : "OFF") + ", owner" + (trustedBuilders ? "+trusted builders" : " only") + ".");
                return true;
            }
            default -> { sender.sendMessage("Usage: /buildfly [status|help|buy]"); return true; }
        }
    }

    @Override public List<String> onTabComplete(CommandSender s, Command c, String alias, String[] a) {
        if (a.length != 1) return List.of();
        List<String> out = new ArrayList<>(List.of("status", "help", "buy"));
        if (s.hasPermission("excelclaimfly.admin")) out.addAll(List.of("give", "end", "info", "confirm", "reload"));
        out.removeIf(x -> !x.startsWith(a[0].toLowerCase(Locale.ROOT)));
        return out;
    }

    // exposed for OfflinePlayer-safe money formatting in tests
    static String moneyFor(OfflinePlayer p, Economy e) { return e == null ? "?" : money(e.getBalance(p)); }
}
