package com.winkreyes.excelmagicgate;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The wand-lore surface of the earning system (0.3.0; DESIGN.md §5) —
 * the locked spec surface: rank, credited casts, next rank +
 * remaining, and the world ceiling, on the wand item's own lore,
 * updating as the player casts. 0.3.1 adds one lore line only
 * (Mike's ruling, Nov 10, 2026): the milestone numbers themselves
 * — Student 100 · Apprentice 400 · Master 2,000 credited casts —
 * listed alongside the credited count and the next-rank gap, so a
 * player reading the wand sees what each rank takes, not only how
 * far away the next one is.
 *
 * Magic OWNS wand lore: Wand.getLore() rebuilds it on every wand
 * update (messages lore, else the wand's `lore` property, else the
 * generated description), so lines appended straight to the
 * ItemStack are dropped the next time Magic regenerates the item.
 * Two seams are therefore implemented, rig-arbitrated (DESIGN.md §5):
 *
 * - SEAM 1 (property, preferred): set the wand's `lore` property
 *   reflectively and let Magic render our block itself on every
 *   regeneration — durable by construction. Verified per refresh by
 *   reading the live item back after Magic's own updateLore().
 * - SEAM 2 (append, fallback): strip our prior sentinel-marked block
 *   from the active wand item's lore and append the fresh block.
 *   Certain to display immediately; may vanish between one of
 *   Magic's rewrites and our next refresh trigger (activation,
 *   credited cast, reconcile — all frequent while playing).
 *
 * Mode "auto" tries seam 1 until it first verifies, then sticks to
 * it; a failed verification permanently selects seam 2 for the
 * session and the failure is logged once, loudly (the §5 fallback
 * beyond this — action-bar + /magicgate rank — triggers only if
 * BOTH seams fail, and is a packet-level report to Mike, never a
 * silent substitution; /magicgate rank carries the same four facts
 * in every mode regardless).
 */
public final class WandLoreHook {

    /** Invisible sentinel prefix on the block's first line (§0§r). */
    static final String SENTINEL = "§0§r";
    private static final List<String> BLOCK_PREFIXES = List.of(
            "§6Rank:", "§7Credited casts:", "§7Next:", "§7Earned", "§7Top rank",
            "§7Milestones:", "§7· §fMaster");

    private final ExcelMagicGate plugin;
    private volatile Boolean propertySeamWorks; // null = untried
    private volatile boolean seamLogged;

    public WandLoreHook(ExcelMagicGate plugin) {
        this.plugin = plugin;
    }

    /** The seam in use, for /magicgate status. */
    public String seamName() {
        String mode = plugin.loreMode();
        if ("off".equals(mode)) {
            return "off";
        }
        if ("append".equals(mode)) {
            return "append (config)";
        }
        if ("property".equals(mode)) {
            return "property (config)";
        }
        if (propertySeamWorks == null) {
            return "auto (undecided — no live wand refreshed yet)";
        }
        return propertySeamWorks ? "property (auto, verified)" : "append (auto, property seam failed)";
    }

    /**
     * The milestone lines (0.3.1): every rank's threshold, read from
     * the same thresholds the earning logic uses, so the numbers on
     * the wand can never drift from the numbers that earn the ranks.
     * With the locked thresholds they render exactly:
     *   "§7Milestones: §fStudent 100 §7· §fApprentice 400"
     *   "§7· §fMaster 2,000"
     * Two lines on purpose: the rig (boot-031-2.log) showed Magic
     * wrapping the single-line form mid-phrase ("…· Master" /
     * "2,000" split across lines), and Mike's ruling is that the
     * numbers show CLEARLY — each milestone phrase stays whole.
     */
    public static List<String> milestoneLines(EarnedRank.Thresholds t) {
        NumberFormat fmt = NumberFormat.getInstance(Locale.US);
        return List.of(
                "§7Milestones: §fStudent " + fmt.format(t.studentAt())
                        + " §7· §fApprentice " + fmt.format(t.apprenticeAt()),
                "§7· §fMaster " + fmt.format(t.masterAt()));
    }

    /** The block's facts as display lines (DESIGN.md §5 shape, plus
     *  the 0.3.1 milestone line). Pure form, unit-testable on a
     *  plain JVM: the credited count, the next-rank gap (or its
     *  ceiling / top-rank stand-ins) and all three milestone
     *  numbers are all present in the returned lines. */
    public static List<String> blockLines(long credited, String currentRankKey,
            RankFloor.Rank ceiling, EarnedRank.Thresholds t) {
        RankFloor.Rank earned = EarnedRank.rankFor(credited, t);
        String number = NumberFormat.getInstance(Locale.US).format(credited);
        List<String> lines = new ArrayList<>();
        lines.add(SENTINEL + "§6Rank: " + MagicFloorHook.title(currentRankKey)
                + " §7(ceiling: " + MagicFloorHook.title(ceiling.key()) + ")");
        lines.add("§7Credited casts: §f" + number);
        if (earned.ordinal() > ceiling.ordinal()) {
            lines.add("§7Earned " + MagicFloorHook.title(earned.key())
                    + " — held by the ceiling until the next seal falls");
        } else if (earned.next() == null) {
            lines.add("§7Top rank earned — Master of the valley's magic");
        } else {
            lines.add("§7Next: " + MagicFloorHook.title(earned.next().key()) + " — "
                    + NumberFormat.getInstance(Locale.US).format(
                            EarnedRank.remainingToNext(credited, t)) + " to go");
        }
        lines.addAll(milestoneLines(t));
        return lines;
    }

    /** The block's facts as display lines (DESIGN.md §5 shape). */
    public List<String> blockLines(Player player, String currentRankKey) {
        long credited = plugin.earnedStore().creditedTotal(player.getUniqueId().toString());
        EarnedRank.Thresholds t = plugin.thresholds();
        RankFloor.Rank ceiling = RankFloor.floorForStage(plugin.appliedStage());
        return blockLines(credited, currentRankKey, ceiling, t);
    }

    /**
     * Refresh the player's active/offhand wand lore. Safe to call on
     * any trigger; silently no-ops when there is no live wand (the
     * block lands on the next refresh after the wand is live).
     */
    public void refresh(Player player) {
        if ("off".equals(plugin.loreMode()) || player == null || !player.isOnline()) {
            return;
        }
        try {
            Object[] found = discover(player);
            if (found == null) {
                return;
            }
            Object mage = found[0];
            Object wand = found[1];
            String rankKey = plugin.floorHook().rankKeyOf(mage, wand);
            if (rankKey == null) {
                return;
            }
            List<String> block = blockLines(player, rankKey);
            String mode = plugin.loreMode();
            boolean wantProperty = "property".equals(mode)
                    || ("auto".equals(mode) && !Boolean.FALSE.equals(propertySeamWorks));
            if (wantProperty && tryPropertySeam(wand, block)) {
                if (!Boolean.TRUE.equals(propertySeamWorks)) {
                    propertySeamWorks = true;
                    logSeam("property seam VERIFIED — Magic renders the rank block "
                            + "from the wand lore property on every regeneration.");
                }
                return;
            }
            if (wantProperty && "auto".equals(mode)) {
                propertySeamWorks = false;
                logSeam("property seam FAILED verification — falling back to the "
                        + "append seam for this session (DESIGN.md S5 seam 2).");
            }
            appendSeam(wand, block);
        } catch (Exception e) {
            plugin.getLogger().warning("Wand lore refresh failed for "
                    + player.getName() + ": " + e);
        }
    }

    private void logSeam(String line) {
        if (!seamLogged) {
            seamLogged = true;
            plugin.getLogger().info("Wand lore: " + line);
        }
    }

    /** Seam 1: set the wand's lore property; verify on the live item
     *  after Magic's own lore update. */
    private boolean tryPropertySeam(Object wand, List<String> block) {
        try {
            Method setProperty = findMethod(wand.getClass(), "setProperty",
                    String.class, Object.class);
            if (setProperty == null) {
                return false;
            }
            setProperty.setAccessible(true);
            setProperty.invoke(wand, "lore", new ArrayList<>(block));
            Method updateLore = findMethod(wand.getClass(), "updateLore");
            if (updateLore != null) {
                updateLore.setAccessible(true);
                updateLore.invoke(wand);
            }
            ItemStack item = itemOf(wand);
            return item != null && loreContains(item, block.get(0));
        } catch (Exception e) {
            return false;
        }
    }

    /** Seam 2: strip our old block from the item's lore, append fresh. */
    private void appendSeam(Object wand, List<String> block) {
        try {
            ItemStack item = itemOf(wand);
            if (item == null) {
                return;
            }
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return;
            }
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore())
                    : new ArrayList<>();
            int cut = -1;
            for (int i = 0; i < lore.size(); i++) {
                if (lore.get(i) != null && lore.get(i).contains(SENTINEL)) {
                    cut = i;
                    break;
                }
            }
            if (cut >= 0) {
                lore = new ArrayList<>(lore.subList(0, cut));
            } else {
                // Defensive: also strip an orphaned tail block (sentinel
                // lost but our line shapes remain at the end).
                while (!lore.isEmpty()) {
                    String last = lore.get(lore.size() - 1);
                    boolean ours = last != null && BLOCK_PREFIXES.stream()
                            .anyMatch(last::startsWith);
                    if (!ours) {
                        break;
                    }
                    lore.remove(lore.size() - 1);
                }
            }
            lore.addAll(block);
            meta.setLore(lore);
            item.setItemMeta(meta);
        } catch (Exception e) {
            plugin.getLogger().warning("Wand lore append seam failed: " + e);
        }
    }

    private boolean loreContains(ItemStack item, String needle) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore() || meta.getLore() == null) {
            return false;
        }
        String strippedNeedle = needle.replace(SENTINEL, "");
        for (String line : meta.getLore()) {
            if (line != null && line.contains(strippedNeedle.replace("§6", ""))) {
                return true;
            }
        }
        return false;
    }

    private ItemStack itemOf(Object wand) {
        try {
            Method m = wand.getClass().getMethod("getItem");
            m.setAccessible(true);
            Object item = m.invoke(wand);
            return item instanceof ItemStack stack ? stack : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Minimal wand discovery for lore: active wand, else offhand. */
    private Object[] discover(Player player) throws Exception {
        Plugin magic = Bukkit.getPluginManager().getPlugin("Magic");
        if (magic == null || !magic.isEnabled()) {
            return null;
        }
        Method getController = magic.getClass().getMethod("getController");
        Object controller = getController.invoke(magic);
        if (controller == null) {
            return null;
        }
        Method getMage = controller.getClass().getMethod("getMage", Player.class);
        Object mage = getMage.invoke(controller, player);
        if (mage == null) {
            return null;
        }
        Object wand = invoke(mage, "getActiveWand");
        if (wand == null) {
            wand = invoke(mage, "getOffhandWand");
        }
        return wand == null ? null : new Object[]{mage, wand};
    }

    private static Object invoke(Object target, String name) throws Exception {
        Method m = target.getClass().getMethod(name);
        m.setAccessible(true);
        return m.invoke(target);
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... params) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getMethod(name, params);
            } catch (NoSuchMethodException ignored) {
                // keep walking up
            }
            try {
                return c.getDeclaredMethod(name, params);
            } catch (NoSuchMethodException ignored) {
                // keep walking up
            }
        }
        return null;
    }
}
