package com.winkreyes.exceldeathprotection;

import org.bukkit.entity.Player;

/**
 * Premium-credit seam for ExcelBossTrial's repeat / first-break
 * war-chest row (decision D9). BossTrial probes this class
 * reflectively for a public static creditPremium(Player[, int])
 * and invokes it with a null target; keep the name, package,
 * static modifier and the int parameter EXACTLY as they are.
 *
 * Semantics (D9): one credit is one banked premium protection.
 * A credit arriving while no protection is active activates a
 * premium protection immediately and banks any remainder; a credit
 * arriving while a protection is active banks in full. Banked
 * credits re-arm premium protection automatically at respawn after
 * a death consumes the active protection. A credit is a service
 * balance, never an inventory item: it cannot be lost, dropped,
 * traded, or despawned, and it stacks as a count in the player's
 * persistent data.
 */
public final class ExcelDeathProtectionAPI {

    private ExcelDeathProtectionAPI() {}

    /** One premium credit (the shape BossTrial uses when only the
     *  1-parameter form is found; here it delegates). */
    public static boolean creditPremium(Player player) {
        return creditPremium(player, 1);
    }

    /** Credit {@code amount} premium protections to {@code player}.
     *  Returns false (and credits nothing) when the plugin is not
     *  enabled, the player is null, the amount is under 1, or the
     *  premium seam is disabled in config. Never throws. */
    public static boolean creditPremium(Player player, int amount) {
        try {
            ExcelDeathProtection plugin = ExcelDeathProtection.instance();
            if (plugin == null || player == null) {
                return false;
            }
            return plugin.creditPremiumInternal(player, amount, "boss-trial");
        } catch (Throwable t) {
            return false;
        }
    }

    /** Current banked premium-credit count for {@code player}
     *  (0 when the plugin is disabled or the player is null). */
    public static int bankedPremiumCredits(Player player) {
        try {
            ExcelDeathProtection plugin = ExcelDeathProtection.instance();
            if (plugin == null || player == null) {
                return 0;
            }
            return plugin.getBankedPremiumCredits(player);
        } catch (Throwable t) {
            return 0;
        }
    }
}
