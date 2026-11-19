package com.winkreyes.exceldowned;

import com.magmaguy.elitemobs.advancedcombat.AdvancedCombatModule;
import com.magmaguy.elitemobs.advancedcombat.classes.ClassLineage;
import com.magmaguy.elitemobs.advancedcombat.resources.ClassResourceController;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * The production Cleric gate: every EliteMobs call the Raise Ally
 * ability makes, and the ONLY place in ExcelDowned that names an
 * EliteMobs type (design B section 6 step 2 — the bridge's seam
 * discipline). Compiled against the pinned EliteMobs 10.9.14 jar;
 * local-build.sh refuses any other EM hash.
 *
 * Reads are EM's public statics — the proven bridge surface:
 * hasActiveClass, activeClassLineageSnapshot (root id + resource
 * type), resourceSnapshot (GRACE amount/max), activeClassIdentity
 * (level() == the active lineage's effective level, bytecode-verified
 * against ActiveLineageSnapshot.activeEffectiveLevel).
 *
 * The spend is D107's route: ClassResourceController.trySpend(Player,
 * double) is public and is EM's own charge call (AdvancedCombatModule
 * .useAbility invokes canAfford then trySpend on the same controller),
 * but no public accessor hands out the controller instance — it lives
 * in the module's private final "resources" field. Reaching it takes
 * ONE reflective field read off the public AdvancedCombatModule.get()
 * singleton. The read sits behind the probe: if the field is ever
 * moved/renamed, controller() answers null, trySpend returns false,
 * and the ability stands down — Downed is never affected. The section
 * 5.3 affordability-read fallback is NOT engaged by this build (D107);
 * a spend that fails after a landed revive is logged by the caller,
 * never silently retried.
 *
 * Nothing here throws: every EM call is Throwable-guarded.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class EmClericGate implements ClericGate {

    private final Logger log;
    private boolean spendUnavailableLogged;

    public EmClericGate(Logger log) {
        this.log = log;
    }

    @Override
    public Probe probe() {
        try {
            return AdvancedCombatModule.isInitialized()
                    ? Probe.READY : Probe.NOT_INITIALIZED;
        } catch (Throwable t) {
            return Probe.UNAVAILABLE;
        }
    }

    @Override
    public ClericView view(Player player) {
        try {
            if (player == null || probe() != Probe.READY) {
                return ClericView.noClass();
            }
            AdvancedCombatModule module = AdvancedCombatModule.get();
            if (module == null || !module.hasActiveClass(player)) {
                return ClericView.noClass();
            }
            String rootId = null;
            String activeFormId = null;
            String resourceType = null;
            Optional<ClassLineage> lineage =
                    AdvancedCombatModule.activeClassLineageSnapshot(player.getUniqueId());
            if (lineage.isPresent()) {
                ClassLineage lin = lineage.get();
                if (lin.root() != null) rootId = lin.root().id();
                if (lin.activeForm() != null) activeFormId = lin.activeForm().id();
                if (lin.resourceType() != null) resourceType = lin.resourceType().name();
            }
            Double amount = null;
            Double max = null;
            Optional<ClassResourceController.Snapshot> snapshot =
                    AdvancedCombatModule.resourceSnapshot(player.getUniqueId());
            if (snapshot.isPresent()) {
                ClassResourceController.Snapshot s = snapshot.get();
                amount = s.amount();
                max = s.maximum();
                if (s.type() != null) resourceType = s.type().name();
            }
            int emLevel = -1;
            AdvancedCombatModule.ClassIdentity identity =
                    AdvancedCombatModule.activeClassIdentity(player.getUniqueId());
            if (identity != null) emLevel = identity.level();
            return new ClericView(true, rootId, activeFormId, resourceType,
                    amount, max, emLevel);
        } catch (Throwable t) {
            log.warning("ExcelDowned: Raise Ally class read failed: "
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
            return ClericView.noClass();
        }
    }

    @Override
    public boolean trySpend(Player player, double amount) {
        try {
            if (player == null || !(amount > 0) || probe() != Probe.READY) {
                return false;
            }
            ClassResourceController controller = controller();
            if (controller == null) return false;
            return controller.trySpend(player, amount);
        } catch (Throwable t) {
            log.warning("ExcelDowned: Raise Ally GRACE spend failed for "
                    + (player == null ? "?" : player.getName()) + ": "
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
            return false;
        }
    }

    /**
     * The D107 reflective instance read: AdvancedCombatModule.get()
     * (public static) -> private final "resources" field -> the
     * controller. Null on any failure (module gone, field moved,
     * access refused) — the caller stands the spend down. A failure
     * is logged once per enable, not per cast.
     */
    private ClassResourceController controller() {
        try {
            AdvancedCombatModule module = AdvancedCombatModule.get();
            if (module == null) return null;
            Field f = AdvancedCombatModule.class.getDeclaredField("resources");
            f.setAccessible(true);
            Object value = f.get(module);
            return value instanceof ClassResourceController
                    ? (ClassResourceController) value : null;
        } catch (Throwable t) {
            if (!spendUnavailableLogged) {
                spendUnavailableLogged = true;
                log.warning("ExcelDowned: Raise Ally GRACE spend unavailable "
                        + "(EliteMobs resource controller could not be obtained: "
                        + t.getClass().getSimpleName() + ": " + t.getMessage()
                        + ") — the ability stands down until this resolves.");
            }
            return null;
        }
    }
}
