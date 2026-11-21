package com.winkreyes.excelquestxp;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;

/**
 * Run B test suite (EliteMobs jar on the classpath — 10.9.14 in a file named 10.9.10): reflection pins
 * on every EliteMobs API surface the bridge touches, plus exact checks on
 * the pure static helpers (armor XP = xp/3, level curve spot values).
 * No server is started; nothing here calls into live game state.
 * Exits non-zero on any failure. Run via run-tests.sh.
 */
public final class EmApiTestMain {

    private static int pass;
    private static int fail;

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            pass++;
            System.out.println("PASS " + name + " — " + detail);
        } else {
            fail++;
            System.out.println("FAIL " + name + " — " + detail);
        }
    }

    private static Method method(Class<?> c, String name, Class<?>... params) throws Exception {
        Method m = c.getMethod(name, params);
        check("method-" + c.getSimpleName() + "." + name,
                Modifier.isPublic(m.getModifiers()), "public, returns " + m.getReturnType().getSimpleName());
        return m;
    }

    public static void main(String[] args) throws Exception {
        Class<?> playerData = Class.forName("com.magmaguy.elitemobs.playerdata.database.PlayerData");
        Class<?> skillType = Class.forName("com.magmaguy.elitemobs.skills.SkillType");
        Class<?> questEvent = Class.forName("com.magmaguy.elitemobs.api.QuestCompleteEvent");
        Class<?> customQuest = Class.forName("com.magmaguy.elitemobs.quests.CustomQuest");
        Class<?> quest = Class.forName("com.magmaguy.elitemobs.quests.Quest");
        Class<?> calculator = Class.forName("com.magmaguy.elitemobs.skills.SkillXPCalculator");
        Class<?> display = Class.forName("com.magmaguy.elitemobs.skills.CombatLevelDisplay");
        Class<?> health = Class.forName("com.magmaguy.elitemobs.skills.ArmorSkillHealthBonus");
        Class<?> xpGainEvent = Class.forName("com.magmaguy.elitemobs.api.EliteSkillXpGainEvent");
        check("classes-present", true, "all 9 EM classes load");

        // --- PlayerData grant/read surface (W3 §1c route C) ---
        Method add = method(playerData, "addSkillXP", UUID.class, skillType, long.class);
        check("addSkillXP-returns-long", add.getReturnType() == long.class, "returns new total");
        check("addSkillXP-is-static", Modifier.isStatic(add.getModifiers()), "static");
        method(playerData, "getSkillXP", UUID.class, skillType);
        method(playerData, "getSkillLevel", UUID.class, skillType);
        method(playerData, "getAllSkillXP", UUID.class);
        method(playerData, "setSkillXP", UUID.class, skillType, long.class);
        method(playerData, "getPlayerData", UUID.class);
        method(playerData, "isDataLoaded", UUID.class);

        // --- Backpay guard surface (1.0.1): live QuestsCompleted counter ---
        // getQuestsCompleted reads the database directly when the player is
        // not in memory, so the pre-pay guard works even before the player
        // is online; addSkillXP, by contrast, silently returns 0 when data
        // is not loaded (bytecode-verified) — hence backpay requires online.
        Method completed = method(playerData, "getQuestsCompleted", UUID.class);
        check("getQuestsCompleted-returns-int", completed.getReturnType() == int.class,
                "returns int");
        check("getQuestsCompleted-is-static", Modifier.isStatic(completed.getModifiers()),
                "static");
        method(playerData, "getPlayerQuestCooldowns", UUID.class);

        // --- SkillType: 11 skills, 10 weapons + Armor ---
        Object[] values = (Object[]) skillType.getMethod("values").invoke(null);
        check("skilltype-count", values.length == 11, "11 skills");
        Object[] weapons = (Object[]) skillType.getMethod("getWeaponSkills").invoke(null);
        check("weapons-count", weapons.length == 10, "10 weapon skills");
        Object armor = Enum.valueOf((Class<Enum>) skillType, "ARMOR");
        Object swords = Enum.valueOf((Class<Enum>) skillType, "SWORDS");
        check("armor-not-weapon",
                !(boolean) skillType.getMethod("isWeaponSkill").invoke(armor), "ARMOR is not a weapon skill");
        check("swords-is-weapon",
                (boolean) skillType.getMethod("isWeaponSkill").invoke(swords), "SWORDS is a weapon skill");
        String displayName = (String) skillType.getMethod("getDisplayName").invoke(swords);
        check("swords-display", displayName != null && !displayName.isBlank(), "display=" + displayName);

        // --- D159 (1.1.0): the live weapon table feeds TargetPolicy ---
        // Pin the weapon order (index 0 = the fresh-player tie-break) and
        // resolve typed input against the REAL enum + display names.
        String[] enumNames = new String[weapons.length];
        String[] displayNames = new String[weapons.length];
        boolean displaysOk = true;
        for (int i = 0; i < weapons.length; i++) {
            enumNames[i] = weapons[i].toString();
            displayNames[i] = (String) skillType.getMethod("getDisplayName").invoke(weapons[i]);
            displaysOk &= displayNames[i] != null && !displayNames[i].isBlank();
        }
        check("weapons-order-first", "SWORDS".equals(enumNames[0]),
                "weapon order[0]=" + enumNames[0] + " (tie-break target)");
        check("weapons-displays", displaysOk,
                "all 10 display names present: " + java.util.Arrays.toString(displayNames));
        check("target-resolve-wand", "WANDS".equals(
                TargetPolicy.resolveSkill("wand", enumNames, displayNames)), "wand -> WANDS (live table)");
        check("target-resolve-sword", "SWORDS".equals(
                TargetPolicy.resolveSkill("sword", enumNames, displayNames)), "sword -> SWORDS (live table)");
        check("target-resolve-display", enumNames[9].equals(
                TargetPolicy.resolveSkill(displayNames[9], enumNames, displayNames)),
                "display name '" + displayNames[9] + "' resolves to " + enumNames[9]);
        check("target-resolve-armor-null",
                TargetPolicy.resolveSkill("armor", enumNames, displayNames) == null,
                "armor never resolves to a weapon target");

        // --- QuestCompleteEvent + CustomQuest identity ---
        method(questEvent, "getPlayer");
        method(questEvent, "getQuest");
        method(questEvent, "isCancelled");
        method(questEvent, "setCancelled", boolean.class);
        check("quest-is-superclass", quest.isAssignableFrom(customQuest), "CustomQuest extends Quest");
        Method filename = method(customQuest, "getConfigurationFilename");
        check("filename-returns-string", filename.getReturnType() == String.class, "String");
        method(quest, "getQuestName");

        // --- Refresh path (/em skill set does these two after setSkillXP) ---
        Method updateDisplay = method(display, "updateDisplay", org.bukkit.entity.Player.class);
        check("updateDisplay-static", Modifier.isStatic(updateDisplay.getModifiers()), "static");
        method(display, "updateDisplay", UUID.class);
        Method updateHealth = method(health, "updateHealthBonus", org.bukkit.entity.Player.class);
        check("updateHealthBonus-static", Modifier.isStatic(updateHealth.getModifiers()), "static");

        // --- SkillXPCalculator: exact, bytecode-verified helpers ---
        Method armorXp = calculator.getMethod("calculateArmorXP", long.class);
        check("calc-armor-1815", (long) armorXp.invoke(null, 1815L) == 605L, "1815/3");
        check("calc-armor-5", (long) armorXp.invoke(null, 5L) == 1L, "5/3 floors");
        Method levelFrom = calculator.getMethod("levelFromTotalXP", long.class);
        check("level-from-0", (int) levelFrom.invoke(null, 0L) == 1, "0 XP is level 1");
        int low = (int) levelFrom.invoke(null, 0L);
        int high = (int) levelFrom.invoke(null, 100000L);
        check("level-monotonic", high > low, "level(100000)=" + high + " > level(0)=" + low);
        Method xpNext = calculator.getMethod("xpToNextLevel", int.class);
        check("xp-next-10", (long) xpNext.invoke(null, 10) == 8500L, "8.5 x 10^3 = 8500");
        Method totalFor = calculator.getMethod("totalXPForLevel", int.class);
        check("total-for-level-2", (long) totalFor.invoke(null, 2) == 8L, "8.5 x 1^3 floors to 8");

        // --- EliteSkillXpGainEvent: COMBAT is the only reason (W3 §1c) ---
        Class<?> reason = Class.forName("com.magmaguy.elitemobs.api.EliteSkillXpGainEvent$Reason");
        Object[] reasons = (Object[]) reason.getMethod("values").invoke(null);
        check("xp-gain-reason-only-combat", reasons.length == 1
                && reasons[0].toString().equals("COMBAT"), "Reason=" + java.util.Arrays.toString(reasons));

        System.out.println("EmApiTestMain: " + pass + " passed, " + fail + " failed.");
        if (fail > 0) {
            System.exit(1);
        }
    }
}
