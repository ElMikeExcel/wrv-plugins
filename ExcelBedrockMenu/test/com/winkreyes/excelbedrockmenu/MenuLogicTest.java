package com.winkreyes.excelbedrockmenu;
public class MenuLogicTest {
    static int fails=0;
    static void check(boolean c,String m){ if(!c){fails++;System.out.println("FAIL "+m);} else System.out.println("ok   "+m);}
    public static void main(String[] a){
        try{
            String src=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.java")));
            String yml=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("commands_menu.yml")));
            check(src.contains("\"Party\", \"Excel Shop\", \"Adventurers Guild\""),"button array has Party before Excel Shop");
            check(src.contains("\"spawn\", \"tpmenu\", \"excelparty\", \"shop\""),"command array runs excelparty at the same index as Party");
            check(!src.contains("performCommand(\"party\")")&&!src.contains("\"spawn\", \"tpmenu\", \"party\""),"never runs bare /party (mcMMO)");
            check(!src.contains("\"Spawn Shop\"")||src.indexOf("\"Spawn Shop\"")<src.indexOf("class ExcelBedrockMenu"),"no Spawn Shop label in code");
            check(src.contains("{\"/shop\", \"shop\", \"Open the Excel Shop\"}"),"/shop kept, Excel Shop description");
            check(yml.contains("Open the Excel Shop")&&!yml.contains("Open the Spawn Shop"),"commands_menu.yml renamed");
        }catch(Exception e){check(false,"file read "+e);}
        String l=ExcelBedrockMenu.lockedLabel("Adventurers Guild");
        check(l.equals("Adventurers Guild"),"locked label text");
        boolean ascii=l.chars().allMatch(c->c<128);
        check(ascii,"locked label is plain ASCII");
        check(ExcelBedrockMenu.isSpawnWorld(true,"world"),"overworld accepted");
        check(!ExcelBedrockMenu.isSpawnWorld(true,"em_adventurers_guild"),"guild world rejected");
        check(!ExcelBedrockMenu.isSpawnWorld(false,"world_nether"),"nether rejected");
        check(!ExcelBedrockMenu.isSpawnWorld(false,"world_the_end"),"end rejected");
        check(!ExcelBedrockMenu.needsSpawnFallback(true,"world"),"no fallback when console spawn worked");
        check(ExcelBedrockMenu.needsSpawnFallback(true,"em_adventurers_guild"),"fallback when still in guild");
        check(ExcelBedrockMenu.needsSpawnFallback(false,"world"),"fallback when command unavailable");
        // v1.5.8: teleport confirm routing (Mike's standing menu standard)
        check(ExcelBedrockMenu.requiresConfirm("spawn",false),"spawn tap opens confirm (no instant teleport)");
        check(ExcelBedrockMenu.requiresConfirm("spawn",true),"spawn confirm does not depend on guild state");
        check(ExcelBedrockMenu.requiresConfirm("ag",false),"guild button confirms while unlocked (it teleports)");
        check(!ExcelBedrockMenu.requiresConfirm("ag",true),"no guild confirm while sealed (/ag opens the quest page, not a teleport)");
        check(ExcelBedrockMenu.requiresConfirm("home",false),"catalogue /home confirms");
        String[] menuOthers={"tpmenu","excelparty","shop","jobs browse","mcstats","leaderboard","motd","servercommands","serverwebsite","menujoin"};
        boolean menuOk=true;
        for(String c:menuOthers){ if(ExcelBedrockMenu.requiresConfirm(c,false)||ExcelBedrockMenu.requiresConfirm(c,true)) menuOk=false; }
        check(menuOk,"non-teleport menu buttons never confirm");
        String[] catalogOthers={"menu","tpmenu","tpaccept","trapped","sethome","warps","balance","balancetop","sell hand","jobs stats","mcstats","mctop","party","leaderboard","excelshop setup","excelshop manage","deathprotection status","mobveil status","claim","claimslist","seals","em","motd","list","playermap","compass","menujoin","servercommands","serverwebsite"};
        boolean catOk=true;
        for(String c:catalogOthers){ if(ExcelBedrockMenu.requiresConfirm(c,false)) catOk=false; }
        check(catOk,"catalogue non-teleports never confirm (tpaccept + trapped deliberately excluded)");
        check(ExcelBedrockMenu.teleportConfirmTitle("spawn",false).equals("Teleport to spawn?"),"spawn confirm title");
        check(ExcelBedrockMenu.teleportConfirmTitle("ag",false).equals("Teleport to the Adventurers Guild?"),"guild confirm title");
        check(ExcelBedrockMenu.teleportConfirmTitle("home",false).equals("Teleport home?"),"home confirm title");
        check(ExcelBedrockMenu.teleportConfirmTitle("ag",true)==null,"no title while sealed");
        check(ExcelBedrockMenu.teleportConfirmTitle("shop",false)==null,"no title for non-teleports");
        check(ExcelBedrockMenu.confirmChoiceTeleports(Boolean.TRUE),"confirm Yes teleports");
        check(!ExcelBedrockMenu.confirmChoiceTeleports(Boolean.FALSE),"confirm Cancel does nothing");
        check(!ExcelBedrockMenu.confirmChoiceTeleports(null),"form closed without choosing does nothing");
        String allConfirmText=ExcelBedrockMenu.teleportConfirmTitle("spawn",false)
                +ExcelBedrockMenu.teleportConfirmTitle("ag",false)+ExcelBedrockMenu.teleportConfirmTitle("home",false)
                +ExcelBedrockMenu.teleportConfirmContent("spawn")+ExcelBedrockMenu.teleportConfirmContent("ag")
                +ExcelBedrockMenu.teleportConfirmContent("home")+ExcelBedrockMenu.CONFIRM_YES+ExcelBedrockMenu.CONFIRM_NO;
        check(allConfirmText.chars().allMatch(c->c<128),"all confirm texts are plain ASCII");
        check(ExcelBedrockMenu.CONFIRM_YES.equals("Yes, teleport"),"confirm Yes label text");
        check(ExcelBedrockMenu.CONFIRM_NO.equals("Cancel"),"confirm Cancel label text");
        try{
            String src2=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.java")));
            check(src2.contains("GeyserForms.sendModalForm"),"teleport confirm uses the existing Cumulus ModalForm helper");
            check(src2.contains("confirmTeleport(player, command"),"main-menu routing calls the confirm step");
            check(src2.contains("runCatalogEntry(player, run)"),"catalogue routing calls the confirm gate");
            check(src2.contains("if (requiresConfirm(command, isGuildLocked()))"),"action routing is gated by the confirm decision");
            String yml2=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("plugin.yml")));
            check(yml2.contains("version: 1.5.8"),"plugin.yml is 1.5.8");
        }catch(Exception e){check(false,"1.5.8 source checks "+e);}
        System.out.println(fails==0?"ALL PASS":"FAILED "+fails);
        if(fails>0)System.exit(1);
    }
}
