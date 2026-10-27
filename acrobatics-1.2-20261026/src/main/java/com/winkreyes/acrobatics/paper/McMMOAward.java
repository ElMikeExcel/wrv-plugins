package com.winkreyes.acrobatics.paper;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/** Awards Acrobatics XP through mcMMO's public API, reached reflectively
 * so the exact installed build decides: the first signature that verifies
 * is used, and if none does, awards stay disabled (logged once) instead of
 * failing. Candidates, in order:
 *   ExperienceAPI.addExperience(Player, String, int)
 *   ExperienceAPI.addExperience(Player, String, float)
 *   ExperienceAPI.addExperience(Player, PrimarySkillType, int)
 *   ExperienceAPI.addExperience(Player, PrimarySkillType, float) */
public final class McMMOAward {

    private final Logger log;
    private boolean probed;
    private Method method;
    private Object skillArg; // "Acrobatics" String or the PrimarySkillType enum constant
    private boolean floatArg;

    public McMMOAward(Logger log) {
        this.log = log;
    }

    private void probe() {
        if (probed) {
            return;
        }
        probed = true;
        if (Bukkit.getPluginManager().getPlugin("mcMMO") == null) {
            log.info("ExcelAcrobatics: mcMMO not found; water-fall awards disabled.");
            return;
        }
        try {
            Class<?> api = Class.forName("com.gmail.nossr50.api.ExperienceAPI");
            // 1.1: the real API names first. 1.0 only tried "addExperience",
            // which does not exist in mcMMO, so awards never fired.
            Method real = null;
            try {
                real = api.getMethod("addModifiedXP", Player.class, String.class, int.class);
                floatArg = false;
            } catch (NoSuchMethodException r1) {
                try {
                    real = api.getMethod("addRawXP", Player.class, String.class, float.class);
                    floatArg = true;
                } catch (NoSuchMethodException r2) {
                    real = null;
                }
            }
            if (real != null) {
                method = real;
                skillArg = "Acrobatics";
                log.info("ExcelAcrobatics: mcMMO award API verified (" + method + ").");
                return;
            }
            try {
                method = api.getMethod("addExperience", Player.class, String.class, int.class);
                skillArg = "Acrobatics";
                floatArg = false;
            } catch (NoSuchMethodException e1) {
                try {
                    method = api.getMethod("addExperience", Player.class, String.class, float.class);
                    skillArg = "Acrobatics";
                    floatArg = true;
                } catch (NoSuchMethodException e2) {
                    Class<?> skillType = Class.forName("com.gmail.nossr50.datatypes.skills.PrimarySkillType");
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Object acrobatics = Enum.valueOf((Class<Enum>) skillType, "ACROBATICS");
                    try {
                        method = api.getMethod("addExperience", Player.class, skillType, int.class);
                        skillArg = acrobatics;
                        floatArg = false;
                    } catch (NoSuchMethodException e3) {
                        method = api.getMethod("addExperience", Player.class, skillType, float.class);
                        skillArg = acrobatics;
                        floatArg = true;
                    }
                }
            }
            log.info("ExcelAcrobatics: mcMMO award API verified (" + method + ").");
        } catch (ReflectiveOperationException | RuntimeException e) {
            method = null;
            log.warning("ExcelAcrobatics: mcMMO present but its award API did not verify; awards disabled.");
        }
    }

    public boolean available() {
        probe();
        return method != null;
    }

    public boolean award(Player player, int xp) {
        probe();
        if (method == null || xp <= 0) {
            return false;
        }
        try {
            if (floatArg) {
                method.invoke(null, player, skillArg, (float) xp);
            } else {
                method.invoke(null, player, skillArg, xp);
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            log.warning("ExcelAcrobatics: mcMMO award call threw: " + e);
            return false;
        }
    }
}

