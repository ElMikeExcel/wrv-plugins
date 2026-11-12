package com.winkreyes.exceltrainingdummy;

import com.winkreyes.exceltrainingdummy.core.Hazard;
import com.winkreyes.exceltrainingdummy.core.ReportText;
import com.winkreyes.exceltrainingdummy.core.TestSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The two doors' controls (DESIGN §1 + two-doors section):
 *  - the SPAWN MENU (/dummy, and the compass menu's "Training Dummy"
 *    entry calls the same command): spawn 1 / 3 / 5 with the dialed-in
 *    tier + presets, your active dummies, remove-all;
 *  - the DUMMY SETTINGS menu (tap your dummy with a compass): tier,
 *    hazardous level, lethal, boss-armor, execute reset, checkpoint
 *    prints, remove — for THAT dummy.
 * Bedrock players get the same picks as Cumulus simple forms, and
 * cycling a setting re-sends the form so the form itself is the menu.
 * All chat reports print identically on both platforms (DESIGN §8).
 */
public final class DummyMenus {
    private DummyMenus() {}

    // ============================== holders ==============================

    public static final class SpawnMenu implements InventoryHolder {
        private Inventory inventory;
        @Override public Inventory getInventory() { return inventory; }
        void set(Inventory inv) { this.inventory = inv; }
    }

    public static final class DummySettingsMenu implements InventoryHolder {
        public final UUID dummyEntityId;
        private Inventory inventory;
        public DummySettingsMenu(UUID dummyEntityId) { this.dummyEntityId = dummyEntityId; }
        @Override public Inventory getInventory() { return inventory; }
        void set(Inventory inv) { this.inventory = inv; }
    }

    // ============================== entry points ==============================

    public static void openSpawnMenu(ExcelTrainingDummy plugin, Player player) {
        if (PlatformDetect.isBedrock(player, plugin.getLogger())) {
            if (sendSpawnForm(plugin, player)) return;
            // form failed: chat fallback below
            player.sendMessage(Component.text("[Training Dummy] The form didn't open — "
                    + "use /dummy spawn 1|3|5 [tier], /dummy removeall, /dummy status.", NamedTextColor.YELLOW));
            return;
        }
        SpawnMenu holder = new SpawnMenu();
        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text("Training Dummy", NamedTextColor.DARK_AQUA));
        holder.set(inv);
        fillSpawnMenu(plugin, player, inv);
        player.openInventory(inv);
    }

    public static void openDummySettings(ExcelTrainingDummy plugin, Player player, Dummy d) {
        if (PlatformDetect.isBedrock(player, plugin.getLogger())) {
            if (sendDummyForm(plugin, player, d)) return;
            player.sendMessage(Component.text("[Training Dummy] The form didn't open — "
                    + "your dummy's settings are unchanged.", NamedTextColor.YELLOW));
            return;
        }
        DummySettingsMenu holder = new DummySettingsMenu(d.entityId);
        Inventory inv = Bukkit.createInventory(holder, 27,
                Component.text("Dummy Settings", NamedTextColor.DARK_AQUA));
        holder.set(inv);
        fillDummyMenu(plugin, d, inv);
        player.openInventory(inv);
    }

    // ============================== Java menus ==============================

    private static void fillSpawnMenu(ExcelTrainingDummy plugin, Player player, Inventory inv) {
        PlayerSetup s = plugin.setup(player.getUniqueId());
        inv.setItem(4, item(Material.BOOK, "Training Dummy — spawn menu",
                List.of("Dial in a tier and presets, then pick a count.",
                        "Dummies are yours: they leave when you do,",
                        "after 10 idle minutes, or on Remove all.")));
        inv.setItem(10, item(Material.ZOMBIE_HEAD, "Spawn 1 dummy",
                List.of("A single target — rotation practice.")));
        inv.setItem(11, item(Material.ZOMBIE_HEAD, "Spawn 3 dummies",
                List.of("The cleave cluster — AoE tests.")));
        inv.setItem(12, item(Material.ZOMBIE_HEAD, "Spawn 5 dummies",
                List.of("The full cluster — big AoE tests.")));
        inv.setItem(14, item(Material.IRON_SWORD, "Tier: " + s.tier.displayName
                        + " (" + ReportText.num(plugin.pool(s.tier)) + " HP)",
                List.of("Click to change tier.")));
        inv.setItem(15, item(s.endless ? Material.LIME_DYE : Material.GRAY_DYE,
                "Endless: " + onOff(s.endless),
                List.of("Cannot die: at the end of a lap it prints",
                        "the report and resets to full.")));
        inv.setItem(16, item(s.bossArmor ? Material.LIME_DYE : Material.GRAY_DYE,
                "Boss-armor preset: " + onOff(s.bossArmor),
                List.of("Numbers read at boss display scale (x"
                        + ReportText.num(plugin.scaleFactor()) + ").")));
        inv.setItem(19, item(Material.BLAZE_POWDER, "Hazardous: " + s.hazard.displayName,
                List.of("The dummy pulses damage at you while you test.",
                        "Stops at 1 HP unless Lethal is on.")));
        inv.setItem(20, item(s.lethal ? Material.LIME_DYE : Material.GRAY_DYE,
                "Lethal: " + onOff(s.lethal),
                List.of("Hazard may kill you — Downed/rescue drills",
                        "with a volunteer. Off = stops at 1 HP.")));
        inv.setItem(21, item(s.executeStart ? Material.LIME_DYE : Material.GRAY_DYE,
                "Execute start (25% HP): " + onOff(s.executeStart),
                List.of("Spawn already in execute range.")));
        inv.setItem(22, item(s.checkpoints ? Material.LIME_DYE : Material.GRAY_DYE,
                "Checkpoint prints (25% steps): " + onOff(s.checkpoints),
                List.of("Running DPS printed at 75 / 50 / 25% HP left.")));
        List<Dummy> mine = plugin.dummiesOf(player.getUniqueId());
        for (int i = 0; i < Math.min(5, mine.size()); i++) {
            Dummy d = mine.get(i);
            inv.setItem(28 + i, item(Material.TARGET, "Your dummy #" + (i + 1)
                            + " — " + d.session.tier().displayName,
                    List.of(ReportText.num(d.session.remaining()) + " of "
                                    + ReportText.num(d.session.pool()) + " HP left",
                            "Click to open its settings.")));
        }
        inv.setItem(49, item(Material.REDSTONE_BLOCK, "Remove all my dummies",
                List.of(mine.size() + " active right now.")));
        inv.setItem(53, item(Material.BARRIER, "Close", List.of()));
    }

    private static void fillDummyMenu(ExcelTrainingDummy plugin, Dummy d, Inventory inv) {
        TestSession s = d.session;
        inv.setItem(4, item(Material.BOOK, "This dummy — "
                        + ReportText.num(s.remaining()) + " / " + ReportText.num(s.pool()) + " HP",
                List.of("Changes restart this dummy's test.",
                        "Damage so far: " + ReportText.num(s.totalDamage()))));
        inv.setItem(10, item(Material.IRON_SWORD, "Tier: " + s.tier().displayName,
                List.of("Click to change tier (restarts the test).")));
        inv.setItem(11, item(Material.BLAZE_POWDER, "Hazardous: " + d.hazard.displayName,
                List.of("Click to change the pulse level.")));
        inv.setItem(12, item(d.lethal ? Material.LIME_DYE : Material.GRAY_DYE,
                "Lethal: " + onOff(d.lethal), List.of("On = the hazard can kill you.")));
        inv.setItem(14, item(d.bossArmor ? Material.LIME_DYE : Material.GRAY_DYE,
                "Boss-armor preset: " + onOff(d.bossArmor),
                List.of("Numbers at boss display scale.")));
        inv.setItem(15, item(s.checkpointsEnabled() ? Material.LIME_DYE : Material.GRAY_DYE,
                "Checkpoint prints: " + onOff(s.checkpointsEnabled()), List.of()));
        inv.setItem(16, item(Material.CLOCK, "Execute reset — restart at 25% HP",
                List.of("Clears the current test.")));
        inv.setItem(22, item(Material.RED_DYE, "Remove this dummy", List.of()));
        inv.setItem(26, item(Material.BARRIER, "Close", List.of()));
    }

    /** Re-renders an already-open Java menu after a click changed state. */
    public static void refresh(ExcelTrainingDummy plugin, Player player, InventoryHolder holder) {
        if (holder instanceof SpawnMenu) fillSpawnMenu(plugin, player, holder.getInventory());
        if (holder instanceof DummySettingsMenu m) {
            Dummy d = plugin.dummyOf(plugin.getServer().getEntity(m.dummyEntityId));
            if (d != null) fillDummyMenu(plugin, d, holder.getInventory());
        }
    }

    // ============================== click handling ==============================

    /** Returns true when the click was handled (event already cancelled by caller). */
    public static boolean handleClick(ExcelTrainingDummy plugin, Player player,
                                      InventoryHolder holder, int slot) {
        if (holder instanceof SpawnMenu) {
            PlayerSetup s = plugin.setup(player.getUniqueId());
            switch (slot) {
                case 10 -> { player.closeInventory(); plugin.spawn(player, 1); }
                case 11 -> { player.closeInventory(); plugin.spawn(player, 3); }
                case 12 -> { player.closeInventory(); plugin.spawn(player, 5); }
                case 14 -> { s.tier = s.tier.next(); refresh(plugin, player, holder); }
                case 15 -> { s.endless = !s.endless; refresh(plugin, player, holder); }
                case 16 -> { s.bossArmor = !s.bossArmor; refresh(plugin, player, holder); }
                case 19 -> { s.hazard = s.hazard.next(); refresh(plugin, player, holder); }
                case 20 -> { s.lethal = !s.lethal; refresh(plugin, player, holder); }
                case 21 -> { s.executeStart = !s.executeStart; refresh(plugin, player, holder); }
                case 22 -> { s.checkpoints = !s.checkpoints; refresh(plugin, player, holder); }
                case 49 -> { player.closeInventory(); plugin.removeAllOf(player.getUniqueId(), true); }
                case 53 -> player.closeInventory();
                default -> {
                    if (slot >= 28 && slot <= 32) {
                        List<Dummy> mine = plugin.dummiesOf(player.getUniqueId());
                        int idx = slot - 28;
                        if (idx < mine.size()) {
                            player.closeInventory();
                            openDummySettings(plugin, player, mine.get(idx));
                        }
                    }
                }
            }
            return true;
        }
        if (holder instanceof DummySettingsMenu m) {
            Dummy d = plugin.dummyOf(plugin.getServer().getEntity(m.dummyEntityId));
            if (d == null) { player.closeInventory(); return true; }
            switch (slot) {
                case 10 -> {
                    // Tier change rebuilds the session on the same dummy.
                    com.winkreyes.exceltrainingdummy.core.Tier next = d.session.tier().next();
                    d.session = new TestSession(next, plugin.pool(next), d.endless,
                            d.session.executeStart(), d.session.checkpointsEnabled());
                    d.lastActivityMillis = System.currentTimeMillis();
                    plugin.refreshNameplate(d);
                    refresh(plugin, player, holder);
                }
                case 11 -> { d.hazard = d.hazard.next(); refresh(plugin, player, holder); }
                case 12 -> { d.lethal = !d.lethal; refresh(plugin, player, holder); }
                case 14 -> { d.bossArmor = !d.bossArmor; refresh(plugin, player, holder); }
                case 15 -> {
                    d.session.setCheckpointsEnabled(!d.session.checkpointsEnabled());
                    refresh(plugin, player, holder);
                }
                case 16 -> {
                    d.session.restartAtExecute(System.currentTimeMillis());
                    d.lastActivityMillis = System.currentTimeMillis();
                    plugin.refreshNameplate(d);
                    refresh(plugin, player, holder);
                }
                case 22 -> { player.closeInventory(); plugin.removeDummy(d, true); }
                case 26 -> player.closeInventory();
                default -> { }
            }
            return true;
        }
        return false;
    }

    // ============================== Bedrock forms ==============================

    private static boolean sendSpawnForm(ExcelTrainingDummy plugin, Player player) {
        PlayerSetup s = plugin.setup(player.getUniqueId());
        List<Dummy> mine = plugin.dummiesOf(player.getUniqueId());
        List<String> buttons = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        buttons.add("Spawn 1 dummy"); actions.add(() -> plugin.spawn(player, 1));
        buttons.add("Spawn 3 dummies (cleave)"); actions.add(() -> plugin.spawn(player, 3));
        buttons.add("Spawn 5 dummies (cleave)"); actions.add(() -> plugin.spawn(player, 5));
        buttons.add("Tier: " + s.tier.displayName + " (" + ReportText.num(plugin.pool(s.tier)) + " HP)");
        actions.add(() -> { s.tier = s.tier.next(); sendSpawnForm(plugin, player); });
        buttons.add("Endless: " + onOff(s.endless));
        actions.add(() -> { s.endless = !s.endless; sendSpawnForm(plugin, player); });
        buttons.add("Boss-armor preset: " + onOff(s.bossArmor));
        actions.add(() -> { s.bossArmor = !s.bossArmor; sendSpawnForm(plugin, player); });
        buttons.add("Hazardous: " + s.hazard.displayName);
        actions.add(() -> { s.hazard = s.hazard.next(); sendSpawnForm(plugin, player); });
        buttons.add("Lethal: " + onOff(s.lethal));
        actions.add(() -> { s.lethal = !s.lethal; sendSpawnForm(plugin, player); });
        buttons.add("Execute start (25 percent HP): " + onOff(s.executeStart));
        actions.add(() -> { s.executeStart = !s.executeStart; sendSpawnForm(plugin, player); });
        buttons.add("Checkpoint prints: " + onOff(s.checkpoints));
        actions.add(() -> { s.checkpoints = !s.checkpoints; sendSpawnForm(plugin, player); });
        for (int i = 0; i < mine.size(); i++) {
            Dummy d = mine.get(i);
            buttons.add("Dummy #" + (i + 1) + " settings — " + d.session.tier().displayName
                    + ", " + ReportText.num(d.session.remaining()) + " HP left");
            actions.add(() -> sendDummyForm(plugin, player, d));
        }
        buttons.add("Remove all my dummies");
        actions.add(() -> plugin.removeAllOf(player.getUniqueId(), true));
        String body = "Dial in a tier and presets, then pick a count. You have "
                + mine.size() + " dummy(s) up.";
        return BedrockForms.sendSimpleForm(player, "Training Dummy", body, buttons,
                idx -> { if (idx >= 0 && idx < actions.size()) actions.get(idx).run(); },
                plugin.getLogger());
    }

    private static boolean sendDummyForm(ExcelTrainingDummy plugin, Player player, Dummy d) {
        TestSession s = d.session;
        List<String> buttons = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        buttons.add("Tier: " + s.tier().displayName + " (tap to change, restarts test)");
        actions.add(() -> {
            com.winkreyes.exceltrainingdummy.core.Tier next = d.session.tier().next();
            d.session = new TestSession(next, plugin.pool(next), d.endless,
                    d.session.executeStart(), d.session.checkpointsEnabled());
            d.lastActivityMillis = System.currentTimeMillis();
            plugin.refreshNameplate(d);
            sendDummyForm(plugin, player, d);
        });
        buttons.add("Hazardous: " + d.hazard.displayName);
        actions.add(() -> { d.hazard = d.hazard.next(); sendDummyForm(plugin, player, d); });
        buttons.add("Lethal: " + onOff(d.lethal));
        actions.add(() -> { d.lethal = !d.lethal; sendDummyForm(plugin, player, d); });
        buttons.add("Boss-armor preset: " + onOff(d.bossArmor));
        actions.add(() -> { d.bossArmor = !d.bossArmor; sendDummyForm(plugin, player, d); });
        buttons.add("Checkpoint prints: " + onOff(s.checkpointsEnabled()));
        actions.add(() -> {
            d.session.setCheckpointsEnabled(!d.session.checkpointsEnabled());
            sendDummyForm(plugin, player, d);
        });
        buttons.add("Execute reset — restart at 25 percent HP");
        actions.add(() -> {
            d.session.restartAtExecute(System.currentTimeMillis());
            d.lastActivityMillis = System.currentTimeMillis();
            plugin.refreshNameplate(d);
            sendDummyForm(plugin, player, d);
        });
        buttons.add("Remove this dummy");
        actions.add(() -> plugin.removeDummy(d, true));
        buttons.add("Back to spawn menu");
        actions.add(() -> sendSpawnForm(plugin, player));
        String body = "This dummy: " + ReportText.num(s.remaining()) + " of "
                + ReportText.num(s.pool()) + " HP left. Damage so far: "
                + ReportText.num(s.totalDamage()) + ".";
        return BedrockForms.sendSimpleForm(player, "Dummy Settings", body, buttons,
                idx -> { if (idx >= 0 && idx < actions.size()) actions.get(idx).run(); },
                plugin.getLogger());
    }

    // ============================== helpers ==============================

    private static String onOff(boolean b) { return b ? "ON" : "off"; }

    static ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.WHITE));
            List<Component> loreC = new ArrayList<>();
            for (String l : lore) loreC.add(Component.text(l, NamedTextColor.GRAY));
            meta.lore(loreC);
            it.setItemMeta(meta);
        }
        return it;
    }
}
