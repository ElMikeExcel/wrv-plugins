package com.winkreyes.excelbedrockmenu;
import java.nio.file.*;import java.util.*;
public final class BossRewardsMenuTest {
 static int n;static void check(boolean b,String s){if(!b)throw new AssertionError(s);n++;}
 public static void main(String[] args)throws Exception{
 String old=Files.readString(Path.of("../../menu-baseline/ExcelBedrockMenu-1.5.23.java"));
 String now=Files.readString(Path.of("src/com/winkreyes/excelbedrockmenu/ExcelBedrockMenu.java"));
 String[] buttons=MenuLogicTest.parseStringArray(now,"BUTTONS"), commands=MenuLogicTest.parseStringArray(now,"COMMANDS");
 check(buttons.length==23&&commands.length==23,"aligned23");
 check(Arrays.equals(Arrays.stream(buttons).filter(s->!s.equals("My Boss Rewards")).toArray(String[]::new),MenuLogicTest.parseStringArray(old.replace("Wand controls: Classic or Sneak bars","Wand controls"),"BUTTONS")),"all old button relative order preserved");
 check(Arrays.equals(Arrays.stream(commands).filter(s->!s.equals("bossrewards")).toArray(String[]::new),MenuLogicTest.parseStringArray(old,"COMMANDS")),"all old action relative order preserved");
 check(buttons[13].equals("My Boss Rewards")&&commands[13].equals("bossrewards"),"matching new index after trash");
 check(ExcelBedrockMenu.COLOR_ME.equals(ExcelBedrockMenu.groupColor("bossrewards")),"Me group");
 for(boolean locked:new boolean[]{true,false}){check(!ExcelBedrockMenu.requiresConfirm("bossrewards",locked),"not teleport");check(ExcelBedrockMenu.menuLabels(locked)[13].equals(ExcelBedrockMenu.COLOR_ME+"My Boss Rewards"),"colored label");}
 for(int bits=0;bits<8;bits++)check(ExcelBedrockMenu.bossRewardsAvailable((bits&1)!=0,(bits&2)!=0,(bits&4)!=0)==(bits==7),"availability truth table");
 check(!ExcelBedrockMenu.CATALOG_VIA_ACTION.contains("bossrewards"),"catalog unchanged");
 int hits=0;for(int g=0;g<ExcelBedrockMenu.catalogGroupCount();g++)for(int i=0;i<ExcelBedrockMenu.catalogEntryCount(g);i++)if("bossrewards".equals(ExcelBedrockMenu.catalogRun(g,i))){hits++;check(ExcelBedrockMenu.catalogGroupName(g).equals("Your Items & Help"),"catalog location");check(ExcelBedrockMenu.catalogDisplay(g,i).equals("/bossrewards"),"catalog display");}
 check(hits==0,"no unmatched catalogue addition");
 check(now.contains("cmd.getPlugin() == owner"),"command owner bound");
 check(!now.contains("excelbosstrial.rewards"),"no invented permission");
 check(now.contains("player.performCommand(\"bossrewards\")"),"runs as owner not console");
 check(now.contains("if (!player.isOnline()) return;"),"online check");
 System.out.println("PASS "+n+" differential and pure routing checks");
 }
}
