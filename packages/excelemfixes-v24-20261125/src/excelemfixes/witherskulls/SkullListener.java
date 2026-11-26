package excelemfixes.witherskulls;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * The death hook, ported verbatim in behavior from the standalone
 * ExcelWitherSkulls 0.1.0 SkullListener. It runs at HIGHEST — after
 * vanilla has populated the drops list and after EliteMobs' own loot
 * handling (which for natural elites never touches the vanilla list:
 * its clear-the-drops path is gated on isVanillaLoot() == false, a
 * custom-boss-only state — verified in the 10.9.10 bytecode during
 * the Oct 21, 2026 rewards audit). The only write is appending at
 * most one skull. It never fires for a non-player kill: getKiller()
 * is Bukkit's own player-kill attribution, the same attribution
 * vanilla's killed-by-player skull gate uses.
 */
final class SkullListener implements Listener {

    private final Supplier<SkullSettings> settings;
    private final EmGateway gateway;
    private final java.util.logging.Logger log;

    SkullListener(Supplier<SkullSettings> settings, EmGateway gateway,
                  java.util.logging.Logger log) {
        this.settings = settings;
        this.gateway = gateway;
        this.log = log;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.getType() != EntityType.WITHER_SKELETON) return;

        SkullSettings s = settings.get();
        Player killer = dead.getKiller();
        boolean playerKill = killer != null;
        int looting = 0;
        if (playerKill) {
            ItemStack hand = killer.getInventory().getItemInMainHand();
            if (hand != null) looting = hand.getEnchantmentLevel(Enchantment.LOOTING);
        }
        boolean skullPresent = false;
        for (ItemStack drop : event.getDrops()) {
            if (drop != null && drop.getType() == Material.WITHER_SKELETON_SKULL
                    && drop.getAmount() > 0) {
                skullPresent = true;
                break;
            }
        }
        double roll = ThreadLocalRandom.current().nextDouble(100.0);
        boolean elite = gateway.isElite(dead);

        TopUpDecider.Verdict verdict = TopUpDecider.decide(
                s.enabled(), true, elite, playerKill, skullPresent,
                s.targetChancePercent(), looting, roll);

        switch (verdict) {
            case ADD_SKULL -> {
                event.getDrops().add(new ItemStack(Material.WITHER_SKELETON_SKULL, 1));
                debug(s, killer, dead, looting, roll, "ADDED 1 skull");
            }
            case ROLL_MISS, TARGET_NOT_ABOVE_VANILLA, SKULL_ALREADY_PRESENT ->
                    debug(s, killer, dead, looting, roll, verdict.name());
            case NOT_PLAYER_KILL -> {
                if (elite) debug(s, killer, dead, looting, roll, verdict.name());
            }
            default -> {
            }
        }
    }

    private void debug(SkullSettings s, Player killer, LivingEntity dead,
                       int looting, double roll, String outcome) {
        if (!s.debugLog()) return;
        double vanilla = SkullMath.vanillaChancePercent(looting);
        double topUp = SkullMath.topUpChancePercent(s.targetChancePercent(), looting);
        log.info(String.format(
                "ExcelEMFixes wither-skulls: elite wither skeleton (level %d) killed by %s; "
                        + "skull target %.1f%%, vanilla chance %.1f%% (Looting %d), "
                        + "top-up chance %.2f%%, roll %.2f -> %s",
                gateway.eliteLevelOf(dead),
                killer == null ? "?" : killer.getName(),
                s.targetChancePercent(), vanilla, looting, topUp, roll, outcome));
    }
}
