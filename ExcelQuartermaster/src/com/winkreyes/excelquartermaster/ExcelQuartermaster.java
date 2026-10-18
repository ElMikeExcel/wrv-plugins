package com.winkreyes.excelquartermaster;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ExcelQuartermaster - the Old Quartermaster's voice.
 *
 * The village quartermaster is a Shopkeepers admin-shop villager: he can
 * hold trades but he cannot speak. This plugin gives him his tales. When a
 * player right-clicks the villager whose custom name matches the
 * configured NPC name (optionally also anchored near a configured spot),
 * the interaction is cancelled - so his deliberately empty trade window
 * never opens - and the player instead hears the next tale from the
 * rotation, in chat.
 *
 * All decision rules live in the Bukkit-free classes (TaleRotation,
 * NpcMatcher, TaleText, TaleStore) and are unit-tested on a plain JVM.
 * This class is only the wiring: config in, events in, chat out, and the
 * per-player rotation state saved to player-tales.txt in the plugin
 * folder.
 *
 * Sneaking is sacred: a sneak-click is the Shopkeepers editor gesture, so
 * it is passed through untouched, always.
 */
public final class ExcelQuartermaster extends JavaPlugin implements Listener {

    private String npcName;
    private String header;
    private long cooldownMillis;
    private boolean anchorEnabled;
    private String anchorWorld;
    private double anchorX;
    private double anchorY;
    private double anchorZ;
    private double anchorRadius;
    private List<String> tales = List.of();

    private final TaleRotation rotation = new TaleRotation();
    private File dataFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadSettings();
        dataFile = new File(getDataFolder(), "player-tales.txt");
        try {
            rotation.restore(TaleStore.load(dataFile.toPath()));
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Could not read " + dataFile.getName()
                    + "; starting the tale rotation fresh.", e);
        }
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("ExcelQuartermaster enabled: '" + npcName + "' will tell his tales ("
                + tales.size() + " tales, anchor " + (anchorEnabled
                        ? "on: " + anchorWorld + " " + fmt(anchorX) + "," + fmt(anchorY) + ","
                                + fmt(anchorZ) + " r" + fmt(anchorRadius)
                        : "off")
                + ", cooldown " + (cooldownMillis / 1000) + "s, "
                + rotation.trackedCount() + " players with saved progress).");
        if (tales.isEmpty()) {
            getLogger().warning("No tales configured: clicks on '" + npcName
                    + "' will be quietly cancelled with nothing to say.");
        }
    }

    @Override
    public void onDisable() {
        saveRotation();
    }

    private void reloadSettings() {
        npcName = getConfig().getString("npc-name", "Old Quartermaster");
        header = getConfig().getString("header", "&6{name} &7says:");
        cooldownMillis = Math.max(0L, getConfig().getLong("cooldown-seconds", 8L)) * 1000L;
        anchorEnabled = getConfig().getBoolean("anchor.enabled", true);
        anchorWorld = getConfig().getString("anchor.world", "world");
        anchorX = getConfig().getDouble("anchor.x", -783.0);
        anchorY = getConfig().getDouble("anchor.y", 81.0);
        anchorZ = getConfig().getDouble("anchor.z", -299.0);
        anchorRadius = getConfig().getDouble("anchor.radius", 10.0);
        tales = getConfig().getStringList("tales");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEntityEvent event) {
        // The event fires once per hand; only the main hand acts, so one
        // click can never tell two tales.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Entity entity = event.getRightClicked();
        if (entity.getType() != EntityType.VILLAGER) {
            return;
        }
        Location loc = entity.getLocation();
        boolean matched = NpcMatcher.matches(
                entity.getCustomName(), npcName,
                anchorEnabled, anchorWorld, anchorX, anchorY, anchorZ, anchorRadius,
                loc.getWorld() == null ? null : loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ());
        if (!matched) {
            return;
        }
        Player player = event.getPlayer();
        TaleRotation.Outcome outcome = rotation.decide(
                player.getUniqueId(), player.isSneaking(),
                System.currentTimeMillis(), tales.size(), cooldownMillis);
        switch (outcome.kind()) {
            case PASSTHROUGH -> {
                // Sneak-click: the Shopkeepers editor path. Never interfere.
            }
            case CANCEL_SILENT -> event.setCancelled(true);
            case TELL -> {
                event.setCancelled(true);
                player.sendMessage(color(header.replace("{name}", npcName)));
                for (String line : TaleText.splitForChat(tales.get(outcome.taleIndex()))) {
                    player.sendMessage(color(line));
                }
                if (outcome.changed()) {
                    saveRotation();
                }
            }
        }
    }

    private void saveRotation() {
        if (dataFile == null) {
            return;
        }
        try {
            TaleStore.save(dataFile.toPath(), rotation.snapshot());
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Could not save tale rotation to "
                    + dataFile.getName() + ".", e);
        }
    }

    private static String fmt(double value) {
        return value == Math.rint(value) ? Long.toString(Math.round(value)) : Double.toString(value);
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
