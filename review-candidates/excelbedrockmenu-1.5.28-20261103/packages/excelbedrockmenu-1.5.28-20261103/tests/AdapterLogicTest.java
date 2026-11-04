// ExcelBedrockMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
package com.winkreyes.excelbedrockmenu;
public final class AdapterLogicTest {
    private static int checks;
    private static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); checks++; }
    public static void main(String[] args) {
        check(ExcelBedrockMenu.silkWarningLabel("ON").equals("Silk Touch Warnings: ON"),"ON label");
        check(ExcelBedrockMenu.silkWarningLabel("OFF").equals("Silk Touch Warnings: OFF"),"OFF label");
        for (String bad : new String[]{null,"", "UNKNOWN", "UNAVAILABLE", "on"})
            check(ExcelBedrockMenu.silkWarningLabel(bad).equals("Silk Touch Warnings: unavailable"),"never fabricate ON");
        for (int flags=0;flags<16;flags++) {
            boolean online=(flags&1)!=0,enabled=(flags&2)!=0,owner=(flags&4)!=0,permission=(flags&8)!=0;
            for (String state : new String[]{"ON","OFF","UNAVAILABLE",null})
                check(ExcelBedrockMenu.silkWarningActionAllowed(online,enabled,owner,permission,state)==
                      (flags==15 && ("ON".equals(state)||"OFF".equals(state))),"guard matrix");
        }
        check(ExcelBedrockMenu.groupColor("silkwarnings").equals("\u00a78"),"Settings group");
        check(!ExcelBedrockMenu.requiresConfirm("silkwarnings",false),"no confirmation unlocked");
        check(!ExcelBedrockMenu.requiresConfirm("silkwarnings",true),"no confirmation locked");
        // v1.5.27: Aim Assist adapter, the exact mirror of the Silk Touch checks above.
        check(ExcelBedrockMenu.aimAssistLabel("ON").equals("Aim Assist: ON"),"aim ON label");
        check(ExcelBedrockMenu.aimAssistLabel("OFF").equals("Aim Assist: OFF"),"aim OFF label");
        for (String bad : new String[]{null,"", "UNKNOWN", "UNAVAILABLE", "on"})
            check(ExcelBedrockMenu.aimAssistLabel(bad).equals("Aim Assist: unavailable"),"aim never fabricate ON");
        for (int flags=0;flags<16;flags++) {
            boolean online=(flags&1)!=0,enabled=(flags&2)!=0,owner=(flags&4)!=0,permission=(flags&8)!=0;
            for (String state : new String[]{"ON","OFF","UNAVAILABLE",null})
                check(ExcelBedrockMenu.aimAssistActionAllowed(online,enabled,owner,permission,state)==
                      (flags==15 && ("ON".equals(state)||"OFF".equals(state))),"aim guard matrix");
        }
        check(ExcelBedrockMenu.groupColor("aimassist").equals("\u00a78"),"aim Settings group");
        check(!ExcelBedrockMenu.requiresConfirm("aimassist",false),"aim no confirmation unlocked");
        check(!ExcelBedrockMenu.requiresConfirm("aimassist",true),"aim no confirmation locked");
        // v1.5.28: Halloween Scares button — Help group, no confirm, and the
        // full main-menu label sequence differs from 1.5.27 by exactly this
        // one insertion (after "Ask the Guide", before "Wand controls").
        check(ExcelBedrockMenu.groupColor("scares").equals("\u00a7n"),"scares Help group");
        check(!ExcelBedrockMenu.requiresConfirm("scares",false),"scares no confirmation unlocked");
        check(!ExcelBedrockMenu.requiresConfirm("scares",true),"scares no confirmation locked");
        String[] expectedLabels = {
            "Spawn", "Teleport to a player", "Party", "Utility Pouch", "Spellbook", "Excel Shop", "My Cosmetics",
            "Adventurers Guild", "Jobs & Money", "Skills & Perks", "Leaderboard", "Lost & Found", "Trash Bin", "My Boss Rewards",
            "Server Info", "Server Commands", "Server Website", "What's New", "Ask the Guide",
            "Halloween Scares \u2014 your switches",
            "Wand controls", "Auto-sort my chests: on/off", "Maker's mark and Gear stats",
            "Auto-open on join", "Silk Touch Warnings", "Aim Assist"
        };
        String[] labels = ExcelBedrockMenu.menuLabels(false);
        check(labels.length == 26,"menu has 26 buttons");
        check(labels.length == expectedLabels.length,"expected sequence length");
        for (int i = 0; i < expectedLabels.length; i++)
            check(ExcelBedrockMenu.stripCodes(labels[i]).equals(expectedLabels[i]),"label sequence "+i);
        check(labels[19].equals("\u00a7nHalloween Scares \u2014 your switches"),"scares colored label exact");
        check(ExcelBedrockMenu.stripCodes(labels[18]).equals("Ask the Guide"),"scares after Ask the Guide");
        check(ExcelBedrockMenu.stripCodes(labels[20]).equals("Wand controls"),"scares before Wand controls");
        check(labels[0].startsWith("\u00a71"),"Spawn keeps Travel color");
        check(labels[5].startsWith("\u00a72"),"Excel Shop keeps Shops color");
        check(labels[25].startsWith("\u00a78"),"Aim Assist keeps Settings color");
        System.out.println("ADAPTER_LOGIC_PASS checks="+checks);
    }
}
