package com.winkreyes.excelquestxp;

import com.magmaguy.elitemobs.playerdata.database.PlayerData;
import com.magmaguy.elitemobs.skills.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The quest-XP target popup (D159) — one choice, two doors, chosen by
 * platform like every Excel menu: Bedrock players (Geyser/Floodgate) get
 * a Cumulus SimpleForm via {@link BedrockForms}; Java players get a
 * chest inventory GUI. The Java side follows the ExcelDowned holder
 * pattern (an InventoryHolder carrying a slot→action map; clicks and
 * drags always cancelled; the menu is stateless — picking a skill
 * applies it through {@link ExcelQuestXP#applyTargetChoice} and closes).
 *
 * <p>Contents (both doors, same order): "Highest weapon skill (auto)",
 * then one entry per EliteMobs weapon skill showing the player's current
 * level and XP with the active choice marked, then Close.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class TargetMenu implements Listener {

    static final String ACTION_HIGHEST = "highest";
    static final String ACTION_CLOSE = "close";
    static final String ACTION_SKILL_PREFIX = "skill:";

    private static final int SLOT_HIGHEST = 4;
    private static final int[] SKILL_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};
    private static final int SLOT_CLOSE = 26;
    private static final int SIZE = 27;

    /** Slot→action map carrier for the Java menu (Downed holder pattern). */
    static final class Holder implements InventoryHolder {
        private final Map<Integer, String> slotActions;
        private Inventory inventory;

        Holder(Map<Integer, String> slotActions) {
            this.slotActions = slotActions;
        }

        String actionAt(int slot) {
            return slotActions.get(slot);
        }

        void bind(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private final ExcelQuestXP plugin;

    public TargetMenu(ExcelQuestXP plugin) {
        this.plugin = plugin;
    }

    /** Open the popup for this player, on their platform's door. */
    public void open(Player player) {
        if (BedrockForms.isBedrockPlayer(player, plugin.getLogger())) {
            if (openForm(player)) {
                return;
            }
            // The form could not be sent — the typed path never breaks.
            player.sendMessage(ChatColor.GOLD + "Quest XP target menu could not open. "
                    + "Use " + ChatColor.BOLD + "/questxp target <skill>" + ChatColor.RESET
                    + ChatColor.GOLD + " or " + ChatColor.BOLD + "/questxp target highest"
                    + ChatColor.RESET + ChatColor.GOLD + " instead.");
            return;
        }
        player.openInventory(buildMenu(player));
    }

    // ---------- Bedrock door (Cumulus SimpleForm) ----------

    private boolean openForm(Player player) {
        SkillType[] weapons = SkillType.getWeaponSkills();
        SkillType pinned = plugin.pinnedSkill(player.getUniqueId());
        String[] labels = new String[weapons.length + 2];
        labels[0] = "Highest weapon skill (auto)" + (pinned == null ? "  (current)" : "");
        for (int i = 0; i < weapons.length; i++) {
            labels[i + 1] = weapons[i].getDisplayName()
                    + " — Lv " + skillLevel(player.getUniqueId(), weapons[i])
                    + (weapons[i] == pinned ? "  (current)" : "");
        }
        labels[labels.length - 1] = "Close";
        return BedrockForms.sendChoices(plugin, player,
                "Quest XP Target",
                "Where should your quest XP go?\nArmor always gains its 1/3 share on top.",
                labels,
                buttonId -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (buttonId == 0) {
                        plugin.applyTargetChoice(player, null);
                    } else if (buttonId >= 1 && buttonId <= weapons.length) {
                        plugin.applyTargetChoice(player, weapons[buttonId - 1]);
                    }
                    // last button = Close: nothing to do, the form is gone.
                },
                () -> { /* dismissed = no change */ });
    }

    // ---------- Java door (chest GUI) ----------

    private Inventory buildMenu(Player player) {
        UUID uuid = player.getUniqueId();
        SkillType[] weapons = SkillType.getWeaponSkills();
        SkillType pinned = plugin.pinnedSkill(uuid);

        Map<Integer, String> actions = new LinkedHashMap<>();
        actions.put(SLOT_HIGHEST, ACTION_HIGHEST);
        for (int i = 0; i < weapons.length && i < SKILL_SLOTS.length; i++) {
            actions.put(SKILL_SLOTS[i], ACTION_SKILL_PREFIX + weapons[i].name());
        }
        actions.put(SLOT_CLOSE, ACTION_CLOSE);

        Holder holder = new Holder(actions);
        Inventory inv = Bukkit.createInventory(holder, SIZE,
                ChatColor.DARK_GREEN + "Quest XP Target");
        holder.bind(inv);
        ItemStack filler = named(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, filler);
        }

        inv.setItem(SLOT_HIGHEST, choiceItem(Material.COMPASS,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Highest weapon skill (auto)",
                pinned == null,
                "Quest XP pays your current highest",
                "weapon skill. Plus Armor at 1/3, always."));
        for (int i = 0; i < weapons.length && i < SKILL_SLOTS.length; i++) {
            SkillType skill = weapons[i];
            inv.setItem(SKILL_SLOTS[i], choiceItem(iconFor(skill),
                    ChatColor.WHITE + skill.getDisplayName(),
                    skill == pinned,
                    "Level " + skillLevel(uuid, skill) + " — "
                            + formatXp(skillXp(uuid, skill)) + " XP"));
        }
        inv.setItem(SLOT_CLOSE, named(Material.ARROW,
                ChatColor.RED + "" + ChatColor.BOLD + "Close",
                ChatColor.GRAY + "Nothing changes until you pick a skill."));
        return inv;
    }

    private ItemStack choiceItem(Material material, String name, boolean active, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(active ? ChatColor.GREEN + "✔ " + name : name);
            java.util.List<String> lines = new java.util.ArrayList<>(Arrays.asList(lore));
            for (int i = 0; i < lines.size(); i++) {
                lines.set(i, ChatColor.GRAY + lines.get(i));
            }
            if (active) {
                lines.add(ChatColor.GREEN + "► Currently selected");
                meta.setEnchantmentGlintOverride(true);
            } else {
                lines.add(ChatColor.YELLOW + "Click to choose");
            }
            meta.setLore(lines);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack named(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static Material iconFor(SkillType skill) {
        return switch (skill) {
            case SWORDS -> Material.IRON_SWORD;
            case AXES -> Material.IRON_AXE;
            case BOWS -> Material.BOW;
            case CROSSBOWS -> Material.CROSSBOW;
            case TRIDENTS -> Material.TRIDENT;
            case HOES -> Material.IRON_HOE;
            case MACES -> Material.MACE;
            // SPEAR exists on the live version (the SPEARS skill exists
            // because the weapon does); matchMaterial keeps older APIs safe.
            case SPEARS -> {
                Material spear = Material.matchMaterial("SPEAR");
                yield spear != null ? spear : Material.IRON_SWORD;
            }
            case STAVES -> Material.STICK;
            case WANDS -> Material.BLAZE_ROD;
            default -> Material.PAPER;
        };
    }

    private static int skillLevel(UUID uuid, SkillType skill) {
        try {
            return PlayerData.getSkillLevel(uuid, skill);
        } catch (Throwable t) {
            return 0;
        }
    }

    private static long skillXp(UUID uuid, SkillType skill) {
        try {
            return PlayerData.getSkillXP(uuid, skill);
        } catch (Throwable t) {
            return 0;
        }
    }

    private static String formatXp(long xp) {
        return java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(xp);
    }

    // ---------- Java menu events ----------

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder)) {
            return;
        }
        event.setCancelled(true); // a menu is not a container
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() != event.getInventory()) {
            return;
        }
        Holder holder = (Holder) event.getInventory().getHolder();
        String action = holder.actionAt(event.getSlot());
        if (action == null) {
            return;
        }
        if (ACTION_CLOSE.equals(action)) {
            player.closeInventory();
        } else if (ACTION_HIGHEST.equals(action)) {
            plugin.applyTargetChoice(player, null);
            player.closeInventory();
        } else if (action.startsWith(ACTION_SKILL_PREFIX)) {
            try {
                plugin.applyTargetChoice(player,
                        SkillType.valueOf(action.substring(ACTION_SKILL_PREFIX.length())));
            } catch (IllegalArgumentException staleAction) {
                plugin.getLogger().warning("TargetMenu: stale skill action " + action);
            }
            player.closeInventory();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }
}
