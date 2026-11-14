package com.winkreyes.exceleitemobsabilitybedrockbridge;

import com.magmaguy.elitemobs.advancedcombat.AdvancedCombatModule;
import com.magmaguy.elitemobs.advancedcombat.abilities.AbilityFailureReason;
import com.magmaguy.elitemobs.advancedcombat.abilities.AbilityResult;
import com.magmaguy.elitemobs.advancedcombat.classes.AbilitySlot;
import com.magmaguy.elitemobs.advancedcombat.classes.ClassLineage;
import com.magmaguy.elitemobs.advancedcombat.resources.ClassResourceController;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * The production gateway: every EliteMobs call the bridge makes, and
 * the only place (besides AcsProbe) that names EliteMobs types.
 *
 * Compiled against the pinned EliteMobs 10.9.14 jar. The surface used
 * is AdvancedCombatModule's public surface - the same useAbility entry
 * point EliteMobs' own F-key gesture router dispatches to. No
 * reflection, no package-private state, no open-world opt-in writes:
 * the bridge fires only what EliteMobs itself would accept.
 *
 * Nothing here throws: failures become ClassView.noClass(),
 * FireOutcome.unreachable(), or moduleReady() == false.
 */
public final class EmAcsGateway implements AcsGateway {

    private final Logger log;

    public EmAcsGateway(Logger log) {
        this.log = log;
    }

    @Override
    public boolean moduleReady() {
        return AcsProbe.probe() == AcsProbe.Result.READY;
    }

    @Override
    public ClassView classView(Player player) {
        try {
            if (!moduleReady()) {
                return ClassView.noClass();
            }
            AdvancedCombatModule module = AdvancedCombatModule.get();
            if (!module.hasActiveClass(player)) {
                return ClassView.noClass();
            }

            String className = null;
            String lineageResource = null;
            Optional<ClassLineage> lineage =
                    AdvancedCombatModule.activeClassLineageSnapshot(player.getUniqueId());
            if (lineage.isPresent()) {
                if (lineage.get().activeForm() != null) {
                    className = lineage.get().activeForm().displayName();
                }
                if (lineage.get().resourceType() != null) {
                    lineageResource = lineage.get().resourceType().displayName();
                }
            }

            String resourceName = lineageResource;
            Double amount = null;
            Double max = null;
            Optional<ClassResourceController.Snapshot> snapshot =
                    AdvancedCombatModule.resourceSnapshot(player.getUniqueId());
            if (snapshot.isPresent()) {
                ClassResourceController.Snapshot s = snapshot.get();
                amount = s.amount();
                max = s.maximum();
                if (s.type() != null) {
                    resourceName = s.type().displayName();
                }
            }

            List<SlotView> slots = new ArrayList<>(3);
            for (BridgeSlot slot : BridgeSlot.values()) {
                slots.add(readSlot(module, player, slot, resourceName));
            }
            return new ClassView(true, className, resourceName, amount, max, slots);
        } catch (Throwable t) {
            log.warning("ExcelEliteMobsAbilityBedrockBridge: reading class state failed: "
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
            return ClassView.noClass();
        }
    }

    private SlotView readSlot(AdvancedCombatModule module, Player player,
                              BridgeSlot slot, String resourceName) {
        AbilitySlot emSlot = toEmSlot(slot);
        Optional<AdvancedCombatModule.AbilityResourceSnapshot> snap =
                AdvancedCombatModule.abilityResourceSnapshot(player, emSlot);
        if (snap.isEmpty()) {
            return SlotView.absent(slot);
        }
        AdvancedCombatModule.AbilityResourceSnapshot s = snap.get();
        String liveName = null;
        try {
            liveName = module.abilityName(player, emSlot);
        } catch (Throwable ignored) {
            // Label falls back to the snapshot's ability id.
        }
        String label = Labels.resolveAbilityLabel(liveName, s.abilityId());
        return new SlotView(slot, true, label, s.cost(), s.affordable(), resourceName);
    }

    @Override
    public FireOutcome fire(Player player, BridgeSlot slot) {
        try {
            if (!moduleReady()) {
                return FireOutcome.failure(null, BridgeFailure.ENGINE_CLOSED);
            }
            AbilityResult result = AdvancedCombatModule.get().useAbility(player, toEmSlot(slot));
            if (result == null) {
                return FireOutcome.unreachable();
            }
            if (result.successful()) {
                return FireOutcome.success(result.abilityId());
            }
            return FireOutcome.failure(result.abilityId(), mapReason(result.failureReason()));
        } catch (Throwable t) {
            // Includes the module's own IllegalStateException when its
            // resource state moved mid-cast. A bridge fire must never
            // blow up inside a form callback.
            log.warning("ExcelEliteMobsAbilityBedrockBridge: fire failed for " + player.getName()
                    + " slot " + slot + ": " + t.getClass().getSimpleName()
                    + ": " + t.getMessage());
            return FireOutcome.unreachable();
        }
    }

    /** BridgeSlot -> EliteMobs AbilitySlot, name-for-name. Pinned by test. */
    static AbilitySlot toEmSlot(BridgeSlot slot) {
        return switch (slot) {
            case MOBILITY -> AbilitySlot.MOBILITY;
            case SIGNATURE -> AbilitySlot.SIGNATURE;
            case UTILITY -> AbilitySlot.UTILITY;
        };
    }

    /** EliteMobs failure reason -> bridge failure, one-for-one. Pinned by test. */
    static BridgeFailure mapReason(AbilityFailureReason reason) {
        if (reason == null) {
            return BridgeFailure.UNKNOWN;
        }
        return switch (reason) {
            case NONE -> BridgeFailure.NONE;
            case WRONG_THREAD -> BridgeFailure.WRONG_THREAD;
            case INVALID_PLAYER -> BridgeFailure.INVALID_PLAYER;
            case INVALID_LEVEL -> BridgeFailure.INVALID_LEVEL;
            case ABILITY_NOT_REGISTERED -> BridgeFailure.ABILITY_NOT_REGISTERED;
            case NO_VALID_TARGET -> BridgeFailure.NO_VALID_TARGET;
            case NO_CORPSE -> BridgeFailure.NO_CORPSE;
            case PATH_BLOCKED -> BridgeFailure.PATH_BLOCKED;
            case UNSAFE_DESTINATION -> BridgeFailure.UNSAFE_DESTINATION;
            case ENGINE_CLOSED -> BridgeFailure.ENGINE_CLOSED;
            case NOT_MOVING -> BridgeFailure.NOT_MOVING;
            case NO_LOW_HEALTH_ALLY -> BridgeFailure.NO_LOW_HEALTH_ALLY;
            case NO_WOUNDED_TARGET -> BridgeFailure.NO_WOUNDED_TARGET;
            case NO_ELITE_TARGET -> BridgeFailure.NO_ELITE_TARGET;
            case NO_LARGE_TARGET -> BridgeFailure.NO_LARGE_TARGET;
            case NO_BOSS_TARGET -> BridgeFailure.NO_BOSS_TARGET;
        };
    }
}
