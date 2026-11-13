package com.winkreyes.excelskillspanel.bukkit;

import com.winkreyes.excelskillspanel.ExcelSkillsPanel;
import com.winkreyes.excelskillspanel.core.PanelModel;
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

/**
 * The Skills panel's two presentations over the one pure
 * PanelModel (DESIGN §7.3, Summons pattern).
 *
 * JAVA — a 54-slot chest: header blocks across the top row (one
 * item per block, its lines + bar as lore), the skill rows in the
 * middle rows (name + level as the item name, the bar + XP
 * numbers as lore), Close in the bottom row. Tapping a skill
 * opens its 27-slot detail view; tapping the Magic header item
 * opens the Magic detail view (the canonical explainer + boosts
 * + mana + casts). Back returns to the panel.
 *
 * BEDROCK — step 1: the panel form (header block as body text,
 * one compact line per skill, one button per skill + a Magic
 * details button when the Magic block is present + Close).
 * Step 2: the tapped skill's detail form (or the Magic detail
 * form), with a Back button. Forms are the live-proven Cumulus
 * shape (BedrockForms); a failed send falls back to the chat
 * lines — a form failure never leaves the player with nothing,
 * and handler failures log at WARNING, never silently.
 *
 * All content decisions live in PanelModel / the providers; this
 * class is Bukkit mechanics only.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class PanelMenus {
    private PanelMenus() {}

    static final int SLOT_SKILLS_FIRST = 9;
    static final int SLOT_CLOSE = 49;
    static final int SLOT_DETAIL_ITEM = 13;
    static final int SLOT_BACK = 18;
    static final int SLOT_DETAIL_CLOSE = 22;

    // ============================== holders ==============================

    public static final class MainMenu implements InventoryHolder {
        private Inventory inventory;
        @Override public Inventory getInventory() { return inventory; }
        void set(Inventory inv) { this.inventory = inv; }
    }

    public static final class SkillDetailMenu implements InventoryHolder {
        public final String rowId;
        private Inventory inventory;
        public SkillDetailMenu(String rowId) { this.rowId = rowId; }
        @Override public Inventory getInventory() { return inventory; }
        void set(Inventory inv) { this.inventory = inv; }
    }

    public static final class MagicDetailMenu implements InventoryHolder {
        private Inventory inventory;
        @Override public Inventory getInventory() { return inventory; }
        void set(Inventory inv) { this.inventory = inv; }
    }

    // ============================== entry point ==============================

    public static void open(ExcelSkillsPanel plugin, Player player) {
        PanelModel.Panel panel = plugin.panelFor(player);
        if (PlatformDetect.isBedrock(player, plugin.getLogger())) {
            if (sendMainForm(plugin, player, panel)) return;
            sendChatFallback(player, panel);
            return;
        }
        openMain(plugin, player, panel);
    }

    // ============================== Java menus ==============================

    public static void openMain(ExcelSkillsPanel plugin, Player player, PanelModel.Panel panel) {
        MainMenu holder = new MainMenu();
        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text("Skills", NamedTextColor.DARK_PURPLE));
        holder.set(inv);
        int slot = 0;
        for (PanelModel.HeaderBlock h : panel.headers()) {
            if (slot > 8) break;
            inv.setItem(slot++, item(iconFor(h.iconKey()), h.title(), h.allLines()));
        }
        List<PanelModel.SkillRow> rows = panel.rows();
        for (int i = 0; i < rows.size() && SLOT_SKILLS_FIRST + i < 45; i++) {
            PanelModel.SkillRow r = rows.get(i);
            List<String> lore = new ArrayList<>();
            lore.add(r.bar() == null
                    ? "Child skill \u2014 level from its parent skills"
                    : r.bar().line());
            if (r.rankLabel() != null && !r.rankLabel().isBlank()) {
                lore.add("Rank: " + r.rankLabel());
            }
            lore.add("Tap for details");
            inv.setItem(SLOT_SKILLS_FIRST + i,
                    item(iconFor(r.iconKey()), r.name() + " \u2014 Lv " + r.level(), lore));
        }
        if (rows.isEmpty() && panel.headers().isEmpty()) {
            inv.setItem(22, item(Material.BOOK, "No skills data",
                    List.of("No skills provider answered \u2014 is mcMMO enabled?")));
        }
        inv.setItem(SLOT_CLOSE, item(Material.BARRIER, "Close", List.of()));
        player.openInventory(inv);
    }

    public static void openSkillDetail(ExcelSkillsPanel plugin, Player player,
                                       PanelModel.SkillRow row) {
        SkillDetailMenu holder = new SkillDetailMenu(row.id());
        Inventory inv = Bukkit.createInventory(holder, 27,
                Component.text(row.name(), NamedTextColor.DARK_PURPLE));
        holder.set(inv);
        inv.setItem(SLOT_DETAIL_ITEM, item(iconFor(row.iconKey()),
                row.name() + " \u2014 Lv " + row.level(), row.detailLines()));
        inv.setItem(SLOT_BACK, item(Material.ARROW, "Back to Skills", List.of()));
        inv.setItem(SLOT_DETAIL_CLOSE, item(Material.BARRIER, "Close", List.of()));
        player.openInventory(inv);
    }

    public static void openMagicDetail(ExcelSkillsPanel plugin, Player player,
                                       PanelModel.Panel panel) {
        PanelModel.HeaderBlock magic = panel.header("magic");
        MagicDetailMenu holder = new MagicDetailMenu();
        Inventory inv = Bukkit.createInventory(holder, 27,
                Component.text("Magic", NamedTextColor.DARK_PURPLE));
        holder.set(inv);
        inv.setItem(SLOT_DETAIL_ITEM, item(
                iconFor(magic != null ? magic.iconKey() : "BLAZE_ROD"),
                "Magic", panel.magicDetailLines()));
        inv.setItem(SLOT_BACK, item(Material.ARROW, "Back to Skills", List.of()));
        inv.setItem(SLOT_DETAIL_CLOSE, item(Material.BARRIER, "Close", List.of()));
        player.openInventory(inv);
    }

    // ============================== click handling ==============================

    /** Returns true when the click was handled (the listener has
     *  already cancelled the event). */
    public static boolean handleClick(ExcelSkillsPanel plugin, Player player,
                                      InventoryHolder holder, int slot) {
        if (holder instanceof MainMenu) {
            PanelModel.Panel panel = plugin.panelFor(player);
            int headerIndex = slot;
            if (headerIndex >= 0 && headerIndex < panel.headers().size()) {
                PanelModel.HeaderBlock h = panel.headers().get(headerIndex);
                if (h.hasDetail()) {
                    openMagicDetail(plugin, player, panel);
                }
                return true;
            }
            int rowIndex = slot - SLOT_SKILLS_FIRST;
            if (rowIndex >= 0 && rowIndex < panel.rows().size()) {
                openSkillDetail(plugin, player, panel.rows().get(rowIndex));
                return true;
            }
            if (slot == SLOT_CLOSE) {
                player.closeInventory();
            }
            return true;
        }
        if (holder instanceof SkillDetailMenu || holder instanceof MagicDetailMenu) {
            if (slot == SLOT_BACK) {
                open(plugin, player);
            } else if (slot == SLOT_DETAIL_CLOSE) {
                player.closeInventory();
            }
            return true;
        }
        return false;
    }

    // ============================== Bedrock forms ==============================

    private static boolean sendMainForm(ExcelSkillsPanel plugin, Player player,
                                        PanelModel.Panel panel) {
        List<String> buttons = new ArrayList<>();
        boolean magicButton = panel.hasMagicDetail();
        if (magicButton) buttons.add("Magic \u2014 details");
        for (PanelModel.SkillRow r : panel.rows()) buttons.add(r.buttonLabel());
        buttons.add("Close");
        int closeIndex = buttons.size() - 1;
        return BedrockForms.sendSimpleForm(player, "Skills",
                PanelModel.bedrockSafe(panel.bedrockBody()), buttons,
                idx -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline() || idx < 0 || idx == closeIndex) return;
                    PanelModel.Panel fresh = plugin.panelFor(player);
                    if (magicButton && idx == 0) {
                        if (!sendMagicForm(plugin, player, fresh)) sendChatFallback(player, fresh);
                        return;
                    }
                    int rowIndex = idx - (magicButton ? 1 : 0);
                    if (rowIndex >= 0 && rowIndex < fresh.rows().size()) {
                        if (!sendSkillForm(plugin, player, fresh, fresh.rows().get(rowIndex))) {
                            sendChatFallback(player, fresh);
                        }
                    }
                }), plugin.getLogger());
    }

    /** A detail view's lines for a Bedrock form (0.2.1): each
     *  line's bar is gold-filled via PanelModel.colorizeBar,
     *  then the whole body passes bedrockSafe as before. */
    private static String bedrockDetailBody(List<String> lines) {
        List<String> colored = new ArrayList<>();
        for (String l : lines) colored.add(PanelModel.colorizeBar(l));
        return PanelModel.bedrockSafe(String.join("\n", colored));
    }

    private static boolean sendSkillForm(ExcelSkillsPanel plugin, Player player,
                                         PanelModel.Panel panel, PanelModel.SkillRow row) {
        return BedrockForms.sendSimpleForm(player, row.name(),
                bedrockDetailBody(row.detailLines()),
                List.of("Back to Skills"),
                idx -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;
                    if (idx == 0 && !sendMainForm(plugin, player, plugin.panelFor(player))) {
                        sendChatFallback(player, panel);
                    }
                }), plugin.getLogger());
    }

    private static boolean sendMagicForm(ExcelSkillsPanel plugin, Player player,
                                         PanelModel.Panel panel) {
        return BedrockForms.sendSimpleForm(player, "Magic",
                bedrockDetailBody(panel.magicDetailLines()),
                List.of("Back to Skills"),
                idx -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;
                    if (idx == 0 && !sendMainForm(plugin, player, plugin.panelFor(player))) {
                        sendChatFallback(player, panel);
                    }
                }), plugin.getLogger());
    }

    /** Form failure fallback: the panel's chat lines. */
    private static void sendChatFallback(Player player, PanelModel.Panel panel) {
        player.sendMessage("The skills form didn't open \u2014 here are your skills:");
        for (String line : panel.chatLines()) player.sendMessage(line);
    }

    // ============================== helpers ==============================

    static Material iconFor(String iconKey) {
        if (iconKey != null) {
            Material m = Material.matchMaterial(iconKey);
            if (m != null) return m;
        }
        return Material.NAME_TAG;
    }

    static ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.WHITE));
            List<Component> loreC = new ArrayList<>();
            for (String l : lore) loreC.add(loreLine(l));
            meta.lore(loreC);
            it.setItemMeta(meta);
        }
        return it;
    }

    /** One lore line as components (0.2.1, Item 9b): the FILLED
     *  portion (\u25b0) of any progress bar in the line renders
     *  NamedTextColor.GOLD; empty segments (\u25b1) and all other
     *  text keep the standing GRAY. This is the Java-chest half
     *  of the gold-bars change \u2014 the Bedrock forms carry the
     *  same split as legacy \u00a76/\u00a77 codes via
     *  PanelModel's colored bar variants. */
    static Component loreLine(String line) {
        if (line == null || (!line.contains(PanelModel.BAR_FILLED)
                && !line.contains(PanelModel.BAR_EMPTY))) {
            return Component.text(line == null ? "" : line, NamedTextColor.GRAY);
        }
        Component out = Component.empty();
        StringBuilder run = new StringBuilder();
        boolean runFilled = false;
        boolean inRun = false;
        for (int i = 0; i <= line.length(); i++) {
            boolean filled = i < line.length() && line.charAt(i) == '\u25b0';
            boolean barChar = i < line.length()
                    && (line.charAt(i) == '\u25b0' || line.charAt(i) == '\u25b1');
            if (barChar && (!inRun || filled == runFilled)) {
                if (!inRun) { inRun = true; runFilled = filled; }
                run.append(line.charAt(i));
                continue;
            }
            if (inRun) {
                out = out.append(Component.text(run.toString(),
                        runFilled ? NamedTextColor.GOLD : NamedTextColor.GRAY));
                run.setLength(0);
                inRun = false;
            }
            if (i < line.length()) {
                // plain text segment: consume until the next bar char
                int j = i;
                while (j < line.length() && line.charAt(j) != '\u25b0'
                        && line.charAt(j) != '\u25b1') j++;
                out = out.append(Component.text(line.substring(i, j), NamedTextColor.GRAY));
                i = j - 1;
            }
        }
        if (inRun) {
            out = out.append(Component.text(run.toString(),
                    runFilled ? NamedTextColor.GOLD : NamedTextColor.GRAY));
        }
        return out;
    }
}
