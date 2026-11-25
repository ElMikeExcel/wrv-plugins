package excelemfixes.skillsmenu;

import com.magmaguy.elitemobs.config.menus.premade.SkillBonusMenuConfig;
import com.magmaguy.elitemobs.config.skillbonuses.SkillBonusConfigFields;
import com.magmaguy.elitemobs.playerdata.database.PlayerData;
import com.magmaguy.elitemobs.skills.SkillType;
import com.magmaguy.elitemobs.skills.SkillXPCalculator;
import com.magmaguy.elitemobs.skills.bonuses.PlayerSkillSelection;
import com.magmaguy.elitemobs.skills.bonuses.SkillBonus;
import com.magmaguy.elitemobs.skills.bonuses.SkillBonusRegistry;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The ONLY skills-menu class that touches EliteMobs types.
 * EliteMobs is a compile-time-only dependency (the pack's
 * libs jar); the module checks that EliteMobs is present and
 * enabled before calling in, and catches linkage failures, so
 * a future EliteMobs that moves these classes degrades the
 * menu to the fallback path instead of breaking — the same
 * isolation as the clarity module's EmMenuBridge.
 *
 * Reads use the same public statics EliteMobs' own screens
 * use. The toggle performs EliteMobs' own menu click
 * sequence, read from the live jar's bytecode (SkillBonusMenu
 * events handler, Nov 24, 2026):
 *   level = SkillXPCalculator.levelFromTotalXP(
 *               PlayerData.getSkillXP(uuid, type))
 *   locked  -> send SkillBonusMenuConfig's requires-level
 *              message (%level% substituted), stop
 *   active  -> PlayerSkillSelection.removeActiveSkill,
 *              send the deactivated message (%skill%),
 *              SkillBonus.onDeactivate(player)
 *   full    -> send the max-active message, stop
 *   else    -> PlayerSkillSelection.addActiveSkill,
 *              send the activated message (%skill%),
 *              SkillBonus.onActivate(player)
 * Selection state is EliteMobs' own (it persists through
 * PlayerSkillSelection's database save) — this bridge forges
 * nothing; it drives the same calls from a different screen.
 */
public final class EmSkillsBridge {

    private EmSkillsBridge() {}

    /** Linkage probe: true when EliteMobs' skill-bonus
     *  machinery answers. Call only with EliteMobs enabled. */
    public static boolean probe() {
        try {
            SkillBonusRegistry.getAllEnabledBonuses();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Every skill with the player's state, in EliteMobs'
     *  own type order (Armor first). */
    public static List<SkillsMenuModel.SkillState> readAll(Player player) {
        List<SkillsMenuModel.SkillState> out = new ArrayList<>();
        for (SkillType type : SkillType.values()) {
            out.add(readSkill(player, type));
        }
        return out;
    }

    /** One skill's state, or null for an unknown type name. */
    public static SkillsMenuModel.SkillState readSkill(Player player,
                                                       String typeName) {
        SkillType type;
        try {
            type = SkillType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return readSkill(player, type);
    }

    private static SkillsMenuModel.SkillState readSkill(Player player,
                                                        SkillType type) {
        UUID uuid = player.getUniqueId();
        int level = PlayerData.getSkillLevel(uuid, type);
        long xp = PlayerData.getSkillXP(uuid, type);
        Set<String> active = Set.copyOf(
                PlayerSkillSelection.getActiveSkills(uuid, type));
        List<SkillsMenuModel.BonusState> bonuses = new ArrayList<>();
        for (SkillBonus bonus : SkillBonusRegistry.getEnabledBonuses(type)) {
            bonuses.add(toState(bonus, level, active));
        }
        return new SkillsMenuModel.SkillState(type.name(),
                type.getDisplayName(), level, xp,
                PlayerSkillSelection.MAX_ACTIVE_SKILLS, bonuses);
    }

    private static SkillsMenuModel.BonusState toState(SkillBonus bonus,
                                                      int level,
                                                      Set<String> active) {
        SkillBonusConfigFields fields = bonus.getConfigFields();
        String name = fields != null && fields.getName() != null
                ? fields.getName() : bonus.getBonusName();
        int required = fields != null
                ? fields.getRequiredLevel() : bonus.getRequiredLevel();
        List<String> description;
        if (fields != null && fields.getDescription() != null
                && !fields.getDescription().isEmpty()) {
            description = fields.getDescription();
        } else if (bonus.getDescription() != null
                && !bonus.getDescription().isBlank()) {
            description = List.of(bonus.getDescription());
        } else {
            description = List.of();
        }
        String effect;
        try {
            effect = bonus.getFormattedBonus(level);
        } catch (Throwable t) {
            effect = "";
        }
        return new SkillsMenuModel.BonusState(bonus.getSkillId(), name,
                description, effect == null ? "" : effect, required,
                bonus.getUnlockTier(), active.contains(bonus.getSkillId()));
    }

    /**
     * Switches one bonus on or off, exactly as EliteMobs' own
     * bonus screen does (see the class comment for the
     * sequence). State is re-read fresh here — the view the
     * player clicked may be a few seconds old.
     */
    public static void toggle(Player player, String typeName,
                              String bonusId) {
        SkillType type = SkillType.valueOf(typeName);
        SkillBonus bonus = SkillBonusRegistry.getSkillById(bonusId);
        if (bonus == null || bonus.getSkillType() != type) return;
        UUID uuid = player.getUniqueId();
        SkillBonusConfigFields fields = bonus.getConfigFields();
        String displayName = fields != null && fields.getName() != null
                ? fields.getName() : bonus.getBonusName();
        int required = fields != null
                ? fields.getRequiredLevel() : bonus.getRequiredLevel();
        // EliteMobs' handler derives the level from XP itself.
        int level = SkillXPCalculator.levelFromTotalXP(
                PlayerData.getSkillXP(uuid, type));
        List<String> active =
                PlayerSkillSelection.getActiveSkills(uuid, type);
        SkillsMenuModel.BonusState state = new SkillsMenuModel.BonusState(
                bonusId, displayName, List.of(), "", required,
                bonus.getUnlockTier(), active.contains(bonusId));
        switch (SkillsMenuModel.planToggle(level, state, active.size(),
                PlayerSkillSelection.MAX_ACTIVE_SKILLS)) {
            case LOCKED -> player.sendMessage(
                    SkillBonusMenuConfig.getSkillRequiresLevelMessage()
                            .replace("%level%",
                                    String.valueOf(required)));
            case CAPPED -> player.sendMessage(
                    SkillBonusMenuConfig.getMaxActiveSkillsReached());
            case DEACTIVATE -> {
                PlayerSkillSelection.removeActiveSkill(uuid, type, bonusId);
                player.sendMessage(
                        SkillBonusMenuConfig.getSkillDeactivatedMessage()
                                .replace("%skill%", displayName));
                bonus.onDeactivate(player);
            }
            case ACTIVATE -> {
                PlayerSkillSelection.addActiveSkill(uuid, type, bonusId);
                player.sendMessage(
                        SkillBonusMenuConfig.getSkillActivatedMessage()
                                .replace("%skill%", displayName));
                bonus.onActivate(player);
            }
        }
    }
}
