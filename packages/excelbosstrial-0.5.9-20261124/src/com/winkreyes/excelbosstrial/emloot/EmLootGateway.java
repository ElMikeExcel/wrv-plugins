/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.emloot;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * The EliteMobs gateway for the grantclaim emloot form
 * (ExcelBossTrial 0.5.8) - the same reflective-seam shape as
 * the sprinkle's EliteSprinkleGateway: this class touches
 * EliteMobs only through EM's own classloader, BossTrial
 * compiles and runs with EliteMobs absent, and every failure
 * degrades to a refusal status, never a throw into the
 * command handler.
 *
 * Construction is EM's own give path, bytecode-verified
 * against the shipped EliteMobs jar: {@code em loot give}
 * resolves the file through the static
 * {@code CustomItem.getCustomItem(filename)} (null = no such
 * loot file) and builds the stack with
 * {@code generateDefaultsItemStack(player, false, null)}.
 * This gateway makes exactly those two calls. The recipient
 * player is passed when they are online - the identical call
 * EM's command makes; for an OFFLINE recipient (the form's
 * primary use: an owed item returned to a player who is not
 * on) null is passed, which EM's constructor tolerates (its
 * one player read on this path is a permission check it
 * skips for a null player; the authored name, lore, enchants
 * and item identity all come from the item's own config).
 *
 * The built stack is a genuine EliteMobs item - the same
 * object EM would hand the player directly - and the caller
 * stores it through TeamClaims.grantClaim, so the claims
 * ledger stays single-writer and the credit presents and
 * delivers like any earned batch.
 */
public final class EmLootGateway {

    /** How a build attempt ended (the adapter words the refusal). */
    public enum Status { OK, EM_UNAVAILABLE, UNKNOWN_LOOT, BUILD_FAILED }

    /** Build outcome: the stack is non-null only on OK. */
    public record Build(Status status, ItemStack stack) {
    }

    private static final Build UNAVAILABLE = new Build(Status.EM_UNAVAILABLE, null);
    private static final Build UNKNOWN = new Build(Status.UNKNOWN_LOOT, null);
    private static final Build FAILED = new Build(Status.BUILD_FAILED, null);

    private final Logger log;
    private boolean seamDead;
    private boolean seamLogged;

    private Method getCustomItemMethod;
    private Method generateDefaultsMethod;

    public EmLootGateway(Logger log) {
        this.log = log;
    }

    public boolean seamDead() {
        return seamDead;
    }

    /**
     * Build one genuine stack of the named EliteMobs loot file
     * for the recipient (null when they are offline). Never
     * throws; a non-OK status means nothing was built and the
     * caller must store nothing.
     */
    public Build build(String lootFile, Player recipient) {
        if (seamDead) {
            return FAILED;
        }
        Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
        if (em == null || !em.isEnabled()) {
            return UNAVAILABLE;
        }
        try {
            if (!resolve(em)) {
                return FAILED;
            }
            Object customItem = getCustomItemMethod.invoke(null, lootFile);
            if (customItem == null) {
                return UNKNOWN;
            }
            Object built = generateDefaultsMethod.invoke(customItem, recipient, false, null);
            if (!(built instanceof ItemStack stack) || stack.getAmount() < 1
                    || stack.getType().isAir()) {
                markDead("EliteMobs built no usable stack for " + lootFile);
                return FAILED;
            }
            return new Build(Status.OK, stack);
        } catch (Throwable t) {
            markDead("EliteMobs item build failed for " + lootFile + ": " + t);
            return FAILED;
        }
    }

    /**
     * 0.5.9: ask whether a loot file exists in EM's registry
     * WITHOUT building it - the arm-time validation for
     * armreturn, where the whole point is that nothing is
     * built until the player joins. Same statuses as build():
     * OK only when the file resolves; EM_UNAVAILABLE when
     * EliteMobs is absent (an arm that cannot be validated is
     * refused, never stored on hope); UNKNOWN_LOOT when EM
     * has no such file.
     */
    public Status probe(String lootFile) {
        if (seamDead) {
            return Status.BUILD_FAILED;
        }
        Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
        if (em == null || !em.isEnabled()) {
            return Status.EM_UNAVAILABLE;
        }
        try {
            if (!resolve(em)) {
                return Status.BUILD_FAILED;
            }
            Object customItem = getCustomItemMethod.invoke(null, lootFile);
            return customItem == null ? Status.UNKNOWN_LOOT : Status.OK;
        } catch (Throwable t) {
            markDead("EliteMobs item lookup failed for " + lootFile + ": " + t);
            return Status.BUILD_FAILED;
        }
    }

    /** Resolve EM's construction surface once, through EM's
     *  own classloader. False (with the seam marked dead) when
     *  the surface is not where the shipped EM puts it. */
    private boolean resolve(Plugin em) {
        if (getCustomItemMethod != null && generateDefaultsMethod != null) {
            return true;
        }
        try {
            ClassLoader cl = em.getClass().getClassLoader();
            Class<?> customItem = Class.forName(
                    "com.magmaguy.elitemobs.items.customitems.CustomItem", true, cl);
            Class<?> eliteEntity = Class.forName(
                    "com.magmaguy.elitemobs.mobconstructor.EliteEntity", true, cl);
            getCustomItemMethod = customItem.getMethod("getCustomItem", String.class);
            generateDefaultsMethod = customItem.getMethod(
                    "generateDefaultsItemStack", Player.class, boolean.class, eliteEntity);
            return true;
        } catch (Throwable t) {
            markDead("EliteMobs item surface unresolved: " + t);
            return false;
        }
    }

    private void markDead(String why) {
        seamDead = true;
        if (!seamLogged) {
            seamLogged = true;
            log.warning("EmLootGateway seam dead for this boot - " + why
                    + " (grantclaim emloot will refuse; every other form is unaffected).");
        }
    }
}
