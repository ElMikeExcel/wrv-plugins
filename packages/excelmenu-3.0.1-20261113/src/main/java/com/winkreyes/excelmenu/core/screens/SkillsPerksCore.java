// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.screens;

/**
 * The Skills & Perks screen copy and the Server Website copy, ported
 * verbatim from ExcelBedrockMenu 1.5.28. The Skills & Perks body is
 * the skills-perks MENU-SPEC section 2 copy; only the seal count is
 * live data (read reflectively from ExcelGuildGate by the Bukkit
 * screen at open time). The numbers in the copy (19 skills, 83
 * perks, levels 1-1000) are copy, not a live mcMMO integration.
 */
public final class SkillsPerksCore {

    public static final String TITLE = "Skills & Perks";
    public static final String[] BUTTONS =
            {"My mcMMO stats", "Skill Trees on the Website", "Close"};
    public static final int MCSTATS = 0;
    public static final int WEBSITE = 1;
    public static final String WEBSITE_FORM_TITLE = "Skill Trees on the Website";

    /** Skills & Perks form body, with the live broken-seal count in
     *  the Magic line (never a hard-coded number). */
    public static String skillsPerksContent(int sealsBroken) {
        return "mcMMO skills are live now: 19 skills and 83 perks, and levels run from "
                + "1 to 1,000. The game announces each new perk in chat the moment you "
                + "unlock it.\n\nCheck yours any time:\n/mcstats shows all your levels.\n"
                + "Typing a skill's name (like /smelting) shows that skill's own page.\n"
                + "/mctop shows the leaderboards.\n\n" + skillsPerksMagicLine(sealsBroken);
    }

    /**
     * The Magic line. "Sealed" only while no seal has broken (the
     * spec copy, unchanged); from Seal 1 on Magic is live, so the
     * line says so, with the live count and where the first wand
     * comes from.
     */
    public static String skillsPerksMagicLine(int sealsBroken) {
        if (sealsBroken <= 0) {
            return "Magic is sealed for now. It arrives as the Adventurers Guild seals fall (0 of 5 today).";
        }
        int n = Math.min(sealsBroken, 5);
        return "Magic is awake: " + n + " of 5 seals broken. Get your first wand from the Old "
                + "Quartermaster in the spawn village. More spells open as each seal falls. "
                + "Tap Spellbook in the menu to see your wand's spells.";
    }

    /** Follow-on form body behind "Skill Trees on the Website": the
     *  address as plain text (Bedrock cannot open links). */
    public static String skillsPerksWebsiteContent() {
        return WebsiteCore.WEBSITE_DISPLAY
                + "\n\nOpen it and tap Skill Trees — every ladder is there.";
    }

    private SkillsPerksCore() {}
}
