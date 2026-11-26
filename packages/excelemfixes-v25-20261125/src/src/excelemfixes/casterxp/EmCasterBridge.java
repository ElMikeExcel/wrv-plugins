package excelemfixes.casterxp;

import com.magmaguy.elitemobs.advancedcombat.AdvancedCombatModule;
import com.magmaguy.elitemobs.advancedcombat.progression.ProfileSnapshot;
import com.magmaguy.elitemobs.playerdata.database.PlayerData;
import com.magmaguy.elitemobs.skills.SkillType;
import com.magmaguy.elitemobs.skills.SkillXPCalculator;

import java.util.Optional;
import java.util.UUID;

/**
 * The ONLY class-xp-sharing class that touches EliteMobs
 * types. EliteMobs is a compile-time-only dependency (the
 * pack's libs jar); the module checks that EliteMobs is
 * present and enabled before calling in, and catches linkage
 * failures, so a future EliteMobs that moves these classes
 * degrades the module to idle instead of breaking — the same
 * isolation as the skills menu's EmSkillsBridge.
 *
 * THE GRANT ROUTE (and why): everything goes through
 * EliteMobs' own public statics on PlayerData —
 * {@code getSkillXP} to read a track total and
 * {@code addSkillXP} to grant. addSkillXP is the very call
 * EliteMobs' own SkillXPHandler makes after firing its
 * EliteSkillXpGainEvent (bytecode-verified against the
 * 10.9.10 jar, Nov 25, 2026): a plain read-modify-write of
 * the player's in-memory data that EliteMobs persists
 * through its own database layer. It fires no event, so
 * this module's grants can never re-enter its own listener,
 * and no console command, level recomputation or direct
 * database write is involved anywhere. Skill levels are
 * derived from totals by EliteMobs' own SkillXPCalculator,
 * the same derivation EliteMobs' screens use, so every
 * EliteMobs surface (its menus, the class cap, our skills
 * menu) agrees with what this module grants.
 *
 * THE FORM ROUTE (0.21.0): the player's ACTIVE FORM id is
 * read from EliteMobs' own Advanced Combat profile —
 * AdvancedCombatModule's per-player ProfileSnapshot, whose
 * activeFormId accounts for a class-locked dungeon run's
 * selection and falls back here to the plain selected form.
 * The form's foundation pair is NOT read from EliteMobs at
 * runtime: it comes from this module's probed catalog
 * table (ClassFormTable), the audit-verified source of
 * truth, so a form id the table does not know resolves to
 * no pair and the module stands down for that player.
 */
public final class EmCasterBridge {

    private EmCasterBridge() {}

    /** Linkage probe: true when EliteMobs' skill-data
     *  machinery answers. Call only with EliteMobs enabled. */
    public static boolean probe() {
        try {
            SkillXPCalculator.levelFromTotalXP(0);
            PlayerData.getSkillXP(new UUID(0, 0), SkillType.WANDS);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * The player's active class form id (e.g. "paladin"),
     * or null when EliteMobs' Advanced Combat System is
     * not initialized, the player's profile is not loaded,
     * or no form is selected. A null here always means
     * "stand down": the module never intervenes for a
     * player whose form it cannot name.
     */
    public static String activeFormId(UUID uuid) {
        try {
            if (!AdvancedCombatModule.isInitialized()) return null;
            AdvancedCombatModule module = AdvancedCombatModule.get();
            if (module == null) return null;
            Optional<ProfileSnapshot> snapshot = module.profile(uuid);
            if (snapshot.isEmpty()) return null;
            ProfileSnapshot profile = snapshot.get();
            Optional<String> active = profile.activeFormId();
            if (active.isPresent() && !active.get().isBlank()) {
                return active.get();
            }
            String selected = profile.selectedFormId();
            return selected == null || selected.isBlank() ? null : selected;
        } catch (Throwable t) {
            return null;
        }
    }

    /** One track's total XP (any EliteMobs SkillType name). */
    public static long readXp(UUID uuid, String typeName) {
        return PlayerData.getSkillXP(uuid, SkillType.valueOf(typeName));
    }

    /**
     * Grants XP into one track through EliteMobs' own add
     * call; returns the track's new total (0 when EliteMobs
     * has no loaded data for the player, in which case the
     * grant did not land — callers treat that as "not
     * applied", never as success).
     */
    public static long addXp(UUID uuid, String typeName, long amount) {
        if (amount <= 0) return readXp(uuid, typeName);
        return PlayerData.addSkillXP(uuid, SkillType.valueOf(typeName),
                amount);
    }

    /** EliteMobs' level for a total-XP value. */
    public static int levelOf(long totalXp) {
        return SkillXPCalculator.levelFromTotalXP(totalXp);
    }
}
