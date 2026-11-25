package com.winkreyes.excelmagicgate;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Wand-owner decontamination (0.3.4; the 0.2.1 repair re-based
 * onto the 0.3.x rank system — logic unchanged).
 *
 * The defect (proven live Nov 23, 2026; wand-restore record has the
 * full evidence chain): a player who logs out while DOWNED
 * (ExcelDowned) and returns after the bleed-out window loses their
 * Magic wand permanently and silently. ExcelDowned marks a downed
 * player's display name "✝ DOWNED <name>"; a display-name-bound
 * wand takes its owner string from that display name when Magic
 * saves it; at the next login the carried wand fails Magic's owner
 * validation during login processing and is deleted — no drop, no
 * lost-wand registration, no log line, no recovery path.
 *
 * This repair runs at the login seam, BEFORE Magic's own join
 * processing validates wands (ExcelMagicGate's join handler at
 * EventPriority.LOWEST, plus a re-run ahead of the delayed floor
 * reconcile for wands Magic surfaces late). For every wand the
 * joining player carries, it reads the owner through Magic's own
 * API; when — and only when — the owner is the downed-marked form
 * of THIS player's name (DownedMarkOwner), it calls the wand's own
 * takeOwnership(player), so Magic itself re-stamps owner and owner
 * id from the player's clean identity. No NBT is edited by hand,
 * no foreign wand is ever re-stamped, and nothing about the wand's
 * spells, rank or identity changes — only the poisoned owner
 * string is healed.
 *
 * Everything Magic-facing is reflective, this plugin's house
 * pattern; wand discovery mirrors MagicFloorHook (controller
 * getIfWand only — never getWand, which wand-ifies any item — plus
 * the same real-wand sanity check).
 *
 * ExcelDowned 1.0.4 separately stops new contaminations at the
 * source (it hands the real name back at logout, before Magic's
 * save). Either half alone closes the observed path; this half
 * also heals wands contaminated before either fix existed.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class WandOwnerRepair {

    private static final Class<?>[] NOARGS = new Class<?>[0];

    private final ExcelMagicGate plugin;

    public WandOwnerRepair(ExcelMagicGate plugin) {
        this.plugin = plugin;
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("wand-owner-repair.enabled", true);
    }

    /**
     * Scan one player's carried wands and repair any whose owner
     * carries the downed mark for this player. Returns the number
     * of wands repaired. Fail-open throughout: any reflection or
     * Magic hiccup leaves the wand exactly as found and is logged
     * once at warning level.
     */
    public int repairPlayer(Player player, String source) {
        if (!enabled() || player == null) {
            return 0;
        }
        Object controller;
        try {
            controller = controller();
        } catch (Exception e) {
            plugin.getLogger().warning("Wand-owner repair (" + source + "): Magic controller "
                    + "unavailable for " + player.getName() + ": " + e.getMessage());
            return 0;
        }
        if (controller == null) {
            return 0; // Magic absent or disabled: nothing to repair against
        }
        int repaired = 0;
        try {
            Method getIfWand = controller.getClass().getMethod("getIfWand", ItemStack.class);
            List<ItemStack> stacks = new ArrayList<>();
            for (ItemStack s : player.getInventory().getContents()) {
                if (s != null) stacks.add(s);
            }
            for (ItemStack s : player.getEnderChest().getContents()) {
                if (s != null) stacks.add(s);
            }
            for (ItemStack stack : stacks) {
                // Per-wand isolation: one wand Magic refuses to load
                // or re-stamp must not block healing the next one.
                try {
                    Object wand = getIfWand.invoke(controller, stack);
                    if (wand == null || !looksLikeRealWand(wand)) {
                        continue;
                    }
                    Object ownerObj = invoke(wand, "getOwner", NOARGS);
                    String owner = ownerObj instanceof String ? (String) ownerObj : null;
                    if (!DownedMarkOwner.isContaminated(owner, player.getName())) {
                        continue;
                    }
                    // Magic's own rebind: re-stamps owner + owner id from
                    // the player's current (clean) identity.
                    Method takeOwnership = wand.getClass().getMethod("takeOwnership", Player.class);
                    takeOwnership.setAccessible(true);
                    takeOwnership.invoke(wand, player);
                    Object afterObj = invoke(wand, "getOwner", NOARGS);
                    String after = afterObj instanceof String ? (String) afterObj : null;
                    if (DownedMarkOwner.isContaminated(after, player.getName())) {
                        plugin.getLogger().warning("Wand-owner repair (" + source + "): wand carried by "
                                + player.getName() + " STILL carries the downed-marked owner '"
                                + after + "' after takeOwnership — left as found, needs a human look.");
                    } else {
                        repaired++;
                        plugin.getLogger().info("Wand-owner repair (" + source + "): healed a wand "
                                + "carried by " + player.getName() + " — owner was '" + owner
                                + "' (ExcelDowned's downed mark), re-stamped to '" + after
                                + "' before Magic's owner check could delete it.");
                    }
                } catch (Exception wandFailure) {
                    plugin.getLogger().warning("Wand-owner repair (" + source + "): one wand carried by "
                            + player.getName() + " could not be checked and was left as found: "
                            + describe(wandFailure));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Wand-owner repair (" + source + ") failed for "
                    + player.getName() + ": " + describe(e));
            return repaired;
        }
        return repaired;
    }

    /** Unwrap reflective invocation layers down to the root cause,
     *  so a log line names the real failure (e.g. a Magic-side
     *  IllegalStateException), not the reflection wrapper. */
    private static String describe(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur.toString();
    }

    /** Magic's controller, the MagicFloorHook route. */
    private Object controller() throws Exception {
        Plugin magic = Bukkit.getPluginManager().getPlugin("Magic");
        if (magic == null || !magic.isEnabled()) {
            return null;
        }
        Method m = magic.getClass().getMethod("getController");
        return m.invoke(magic);
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... args)
            throws Exception {
        Method m = target.getClass().getMethod(name, types);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    /** Same sanity bar as MagicFloorHook: a real wand is bound,
     *  templated, or holding spells; a phantom materialization is
     *  none of the three. */
    private boolean looksLikeRealWand(Object wand) {
        try {
            Object bound = invoke(wand, "isBound", NOARGS);
            if (Boolean.TRUE.equals(bound)) {
                return true;
            }
            Object template = invoke(wand, "getTemplateKey", NOARGS);
            if (template != null) {
                return true;
            }
            Object spells = invoke(wand, "getSpells", NOARGS);
            return spells instanceof Collection<?> c && !c.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
