package com.winkreyes.exceltogether;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * ExcelTogether 0.2.0: the Jobs party bonus (+25% money and XP) plus the party itself: invite, accept, leave.
 * The command is /excelparty (aliases /eparty, /together) because mcMMO owns /party on this server.
 */
public final class ExcelTogether extends JavaPlugin implements Listener, Env {
    private PartyManager mgr;
    private Controller ctl;
    private boolean compassClick;

    @Override public void onEnable() {
        saveDefaultConfig();
        PartyBonus bonus = new PartyBonus(getConfig().getDouble("multiplier", PartyBonus.DEFAULT_MULTIPLIER),
                getConfig().getDouble("radius", PartyBonus.DEFAULT_RADIUS));
        BonusMode mode = BonusMode.parse(getConfig().getString("bonus-mode", "party"));
        mgr = new PartyManager(getConfig().getInt("max-party-size", PartyManager.DEFAULT_SIZE),
                getConfig().getLong("invite-expiry-seconds", 60) * 1000L,
                getConfig().getLong("offline-grace-seconds", 300) * 1000L,
                id -> Bukkit.getPlayer(id) != null);
        compassClick = getConfig().getBoolean("compass-click-invite", true);
        int pct = (int) Math.round((bonus.multiplier() - 1.0) * 100.0);
        String note = "Party bonus: +" + pct + "% Jobs money and XP when a party member is within " + (int) bonus.radius() + " blocks.";
        Forms forms = new SyncForms(new GeyserForms(getLogger()), r -> Bukkit.getScheduler().runTask(this, r));
        ctl = new Controller(mgr, this, forms, note);
        String linkNote = setupMcmmoLink();
        getServer().getPluginManager().registerEvents(new PartyListener(bonus, new PartyMemberFinder(bonus, mgr, mode)), this);
        getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, () -> ctl.tick(), 20L, 20L);
        getLogger().info("ExcelTogether 0.2.0 enabled: x" + bonus.multiplier() + " money and XP, bonus-mode " + mode
                + ", radius " + bonus.radius() + " blocks, parties up to " + mgr.maxSize() + ", invites last "
                + (mgr.ttlMs() / 1000) + "s, compass click invite " + (compassClick ? "ON" : "off") + ". Bedrock forms: "
                + (Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null ? "Geyser found" : "no Geyser, chat menu only")
                + ". mcMMO party link: " + linkNote);
    }

    /** Soft link to mcMMO parties. Any problem just leaves it off; ExcelTogether works alone. */
    private String setupMcmmoLink() {
        if (!getConfig().getBoolean("mcmmo-party-link", true)) return "OFF (config)";
        org.bukkit.plugin.Plugin mm = Bukkit.getPluginManager().getPlugin("mcMMO");
        if (mm == null || !mm.isEnabled()) return "OFF (mcMMO not installed)";
        try {
            ReflectMcmmoLink l = new ReflectMcmmoLink(mm.getClass().getClassLoader(), Bukkit::getPlayer, Bukkit::getOfflinePlayer, getLogger());
            if (!l.available()) { getLogger().warning("mcMMO found but these parts are missing, party link OFF: " + l.missing()); return "OFF (mcMMO API differs: " + l.missing().size() + " missing)"; }
            ctl.setLink(l);
            return "ON (mcMMO " + mm.getDescription().getVersion() + ")";
        } catch (Throwable t) { getLogger().warning("mcMMO party link OFF: " + t); return "OFF (error)"; }
    }

    // ---- Env ----
    @Override public PlayerView online(UUID id) { Player p = Bukkit.getPlayer(id); return p == null ? null : new BukkitPlayer(p); }
    @Override public List<PlayerView> visibleOthers(PlayerView viewer) {
        List<PlayerView> out = new ArrayList<>();
        Player v = Bukkit.getPlayer(viewer.id());
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (v != null && (o.getUniqueId().equals(v.getUniqueId()) || !v.canSee(o))) continue;
            out.add(new BukkitPlayer(o));
        }
        return out;
    }
    @Override public long now() { return System.currentTimeMillis(); }

    // ---- events ----
    @EventHandler public void onQuit(PlayerQuitEvent e) { ctl.playerQuit(e.getPlayer().getUniqueId()); }
    @EventHandler public void onJoin(PlayerJoinEvent e) { ctl.playerJoin(new BukkitPlayer(e.getPlayer())); }

    /** Right-click another player while holding a compass: ask to invite them (Yes/Cancel first). They must still accept. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCompassClick(PlayerInteractEntityEvent e) {
        if (!compassClick || e.getHand() != EquipmentSlot.HAND) return;
        if (!(e.getRightClicked() instanceof Player target)) return;
        Player me = e.getPlayer();
        if (me.getInventory().getItemInMainHand().getType() != Material.COMPASS) return;
        if (!me.hasPermission("excelparty.use") || target.hasMetadata("NPC")) return;
        ctl.compassClick(new BukkitPlayer(me), new BukkitPlayer(target));
    }

    // ---- command ----
    @Override public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (!(s instanceof Player pl)) { s.sendMessage("Players only."); return true; }
        if (!pl.hasPermission("excelparty.use")) { pl.sendMessage("You can't use parties."); return true; }
        ctl.command(new BukkitPlayer(pl), a);
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 1) {
            List<String> o = new ArrayList<>(List.of("invite", "accept", "decline", "confirm", "cancel", "leave", "list", "kick", "invites", "help"));
            o.removeIf(x -> !x.startsWith(a[0].toLowerCase(Locale.ROOT)));
            return o;
        }
        if (a.length == 2 && a[0].equalsIgnoreCase("invite")) {
            List<String> o = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase(Locale.ROOT).startsWith(a[1].toLowerCase(Locale.ROOT))) o.add(p.getName());
            return o;
        }
        return List.of();
    }
}
