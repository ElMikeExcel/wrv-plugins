/* Excel WRV maintained source; original attribution and license preserved. */ package com.winkreyes.makersmark;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.winkreyes.makersmark.core.*;
import net.kyori.adventure.text.format.NamedTextColor;
class ColorCreditTest {
 @Test void boundariesAndFractions() {
  long[] cuts={100,500,1000,5000};
  NamedTextColor[] colors={NamedTextColor.GRAY,NamedTextColor.GREEN,NamedTextColor.BLUE,NamedTextColor.DARK_PURPLE,NamedTextColor.GOLD};
  for(int i=0;i<cuts.length;i++) {
   assertEquals(colors[i],MilestoneColors.weapon(cuts[i]*5-1,0));
   assertEquals(colors[i+1],MilestoneColors.weapon(cuts[i]*5,0));
   assertEquals(colors[i+1],MilestoneColors.weapon(cuts[i]*5+1,0));
   assertEquals(colors[i],MilestoneColors.armor((int)cuts[i]-1));
   assertEquals(colors[i+1],MilestoneColors.armor((int)cuts[i]));
  }
 }
 @Test void actualKillsDoNotBecomeCredit() {
  Marks.Data d=new Marks.Data();d.mob=99;d.mobKill(true);
  assertEquals(100,d.mob); assertEquals(496,d.creditUnits());
  assertEquals(NamedTextColor.GRAY,MilestoneColors.weapon(d.creditUnits(),0));
  for(int i=0;i<4;i++)d.mobKill(true);
  assertEquals(104,d.mob); assertEquals(500,d.creditUnits());
  assertEquals(NamedTextColor.GREEN,MilestoneColors.weapon(d.creditUnits(),0));
  assertTrue(LoreModel.killsLine(d.mob,0).contains("104"));
 }
 @Test void legacyGrandfatherAndPvP() {
  for(int old:new int[]{0,99,100,500,1000,5000,Integer.MAX_VALUE}) {
   Marks.Data d=new Marks.Data();d.mob=old;
   assertEquals((long)old*5,d.creditUnits());
   assertEquals(MilestoneColors.armor(old),MilestoneColors.weapon(d.creditUnits(),0));
  }
  assertEquals(NamedTextColor.GREEN,MilestoneColors.weapon(495,1));
  assertEquals(NamedTextColor.GOLD,MilestoneColors.weapon(Long.MAX_VALUE,Integer.MAX_VALUE));
 }
 @Test void fractionMergeAndPreview() {
  Marks.Data a=new Marks.Data(),b=new Marks.Data();a.mob=100;a.mobCredit=51;b.mob=99;b.mobCredit=49;
  assertEquals(51,Marks.merge(a,b).creditUnits());
  Marks.Data preview=new Marks.Data();preview.mob=100;
  assertEquals(51,Marks.mergeResult(a,preview).creditUnits());
  preview.mobCredit=99999; assertEquals(51,Marks.mergeResult(a,preview).creditUnits());
  b.mobCredit=-1; assertEquals(495,Marks.merge(a,b).creditUnits());
 }
 @Test void fifthsAndOverflow() {
  long n=0;for(int i=0;i<5;i++)n=KillCredit.add(n,true);assertEquals(5,n);
  assertEquals(10,KillCredit.add(n,false));assertEquals(Long.MAX_VALUE,KillCredit.add(Long.MAX_VALUE-1,false));
  Marks.Data d=new Marks.Data();d.mob=Integer.MAX_VALUE;d.mobKill(true);assertEquals(Integer.MAX_VALUE,d.mob);
  assertTrue(LoreModel.killsLine(Integer.MAX_VALUE,Integer.MAX_VALUE).contains("4294967294"));
 }
 @Test void narrowOriginsAndOverlap() {
  assertTrue(KillCredit.reduced("SPAWNER",null,null,false));
  assertTrue(KillCredit.reduced("CUSTOM",null,"spawn_origin_spawner",false));
  for(String reason:new String[]{"TRIAL_SPAWNER","SPAWNER_EGG","NETHER_PORTAL","DEFAULT","CUSTOM"}) {
   assertFalse(KillCredit.reduced(reason,reason,"classification_unavailable",false));
   assertTrue(KillCredit.reduced(reason,reason,null,true));
  }
  assertEquals(1,KillCredit.add(0,KillCredit.reduced("SPAWNER",null,null,true)));
 }
 @Test void armorAndOtherLoreIndependent() {
  assertEquals(NamedTextColor.GOLD,MilestoneColors.forCreditLine("Hits blocked: 5000",0,0,5000));
  assertEquals(NamedTextColor.GRAY,MilestoneColors.forCreditLine("Found by Mike",Long.MAX_VALUE,5000,5000));
  assertEquals(NamedTextColor.GRAY,MilestoneColors.forCreditLine("Crafted by Mike",Long.MAX_VALUE,5000,5000));
  assertEquals(NamedTextColor.GRAY,MilestoneColors.forCreditLine("Slain: Wither",Long.MAX_VALUE,5000,5000));
 }
 @Test void historiesAndFractionOnlySurviveMerge() {
  Marks.Data a=new Marks.Data(),b=new Marks.Data();a.by="maker";a.name="Name";a.mobCredit=1;a.pvp=4;a.hits=6;a.slain.put("WITHER",1);
  assertTrue(a.hasStats());assertFalse(a.empty());Marks.Data d=Marks.merge(a,b);
  assertEquals(1,d.creditUnits());assertEquals(4,d.pvp);assertEquals(6,d.hits);assertEquals("maker",d.by);assertEquals(1,d.slain.get("WITHER"));
 }
}
