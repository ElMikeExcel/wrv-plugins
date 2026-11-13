package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.BarPosition;
import com.winkreyes.excelwandbars.core.LoadoutText;
import com.winkreyes.excelwandbars.core.BarLoadout;
import com.winkreyes.excelwandbars.core.SpellbookText;
import java.util.Map;
/**
 * 0.3.7 position labels (Mike, Nov 1, 2026): every hotbar row is a BAR and
 * every position in it is a SLOT. Pins the formatter on the boundary cases
 * and the screenshot decodes, plus the label surfaces that print positions.
 */
public class BarPositionTest {
 static int n; static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 static void eq(String got, String want){ ok(want.equals(got)); }
 public static void main(String[]args){
  BarPosition.setTemplate(null); // settled default
  // First slot of bar 1 (Mend in Mike's screenshot: loadout slot 0).
  eq(BarPosition.format(0,7),"Bar 1 · Slot 1");
  // Heal in the screenshot: loadout slot 1.
  eq(BarPosition.format(1,7),"Bar 1 · Slot 2");
  // Last slot of a bar.
  eq(BarPosition.format(6,7),"Bar 1 · Slot 7");
  // First slot of bar 2.
  eq(BarPosition.format(7,7),"Bar 2 · Slot 1");
  // The Cure case: the old label said "bar 17" for loadout slot 16.
  eq(BarPosition.format(16,7),"Bar 3 · Slot 3");
  // Last slot of the 21-slot bar.
  eq(BarPosition.format(20,7),"Bar 3 · Slot 7");
  // The width is honored (Sneak bars run 8 wide).
  eq(BarPosition.format(16,8),"Bar 3 · Slot 1");
  eq(BarPosition.format(7,8),"Bar 1 · Slot 8");
  eq(BarPosition.format(8,8),"Bar 2 · Slot 1");
  // barOf/slotOf agree with the format.
  ok(BarPosition.barOf(16,7)==3 && BarPosition.slotOf(16,7)==3);
  ok(BarPosition.barOf(0,7)==1 && BarPosition.slotOf(0,7)==1);
  // Degenerate inputs clamp instead of throwing.
  eq(BarPosition.format(0,0),"Bar 1 · Slot 1");
  eq(BarPosition.format(-3,7),"Bar 1 · Slot 1");
  // The format string is a config key: a custom template is honored, and a
  // blank one restores the settled default.
  BarPosition.setTemplate("B{bar}S{slot}");
  eq(BarPosition.format(16,7),"B3S3");
  BarPosition.setTemplate("  ");
  eq(BarPosition.format(16,7),"Bar 3 · Slot 3");
  BarPosition.setTemplate(null);
  eq(BarPosition.template(),BarPosition.DEFAULT_TEMPLATE);

  // ---- the label surfaces print bar AND slot, never the flattened index
  SpellbookText.Entry cure=new SpellbookText.Entry("cure","Cure",0,"Beginner",false);
  cure.barSlot=16; cure.perPage=7;
  String btn=SpellbookText.bedrockButton(cure);
  ok(btn.contains("Bar 3 · Slot 3"));
  ok(!btn.contains("bar 17"));
  String line=SpellbookText.javaLine(cure);
  ok(line.contains("Bar 3 · Slot 3"));
  SpellbookText.Entry mend=new SpellbookText.Entry("mend","Mend",0,"Beginner",false);
  mend.barSlot=0; mend.perPage=7; mend.active=true;
  ok(SpellbookText.bedrockButton(mend).contains("Bar 1 · Slot 1"));
  ok(SpellbookText.bedrockButton(mend).contains("(in use)")); // the suffix stays
  // Legacy fallback untouched: no own-bar slot, a Magic page still prints.
  SpellbookText.Entry legacy=new SpellbookText.Entry("cure","Cure",0,"Beginner",false);
  legacy.page=2;
  ok(SpellbookText.bedrockButton(legacy).contains("page 2"));

  // LoadoutText surfaces.
  eq(LoadoutText.slotButton(16,"Cure",7),"Bar 3 · Slot 3: Cure");
  eq(LoadoutText.slotTitle(0,"Mend",7),"Bar 1 · Slot 1: Mend");
  eq(LoadoutText.pickSpellButton("Cure",16,7),"Cure (Bar 3 · Slot 3)");
  eq(LoadoutText.pickSpellButton("Cure",-1,7),"Cure");
  ok(LoadoutText.pickSpellContent(16,7).startsWith("Pick the spell for Bar 3 · Slot 3."));
  ok(SpellbookText.pickGroupContent(16,7).startsWith("Pick the spell for Bar 3 · Slot 3."));
  ok(SpellbookText.pickListTitle(16,"Healing",0,1,7).startsWith("Bar 3 · Slot 3: Healing"));
  ok(LoadoutText.pagesLine(7,21).contains("bar 2"));
  ok(!LoadoutText.pagesLine(7,21).contains("page"));
  // Result lines name both positions of a swap (real Changes from a real loadout).
  BarLoadout l=BarLoadout.seed(java.util.List.of("mend","heal"),21);
  BarLoadout.Change swap=l.put("heal",0);
  String s=LoadoutText.changed(swap,Map.of("mend","Mend","heal","Heal"),7);
  ok(s.contains("Bar 1 · Slot 1") && s.contains("Bar 1 · Slot 2"));
  BarLoadout.Change put=l.put("cure",16);
  ok(LoadoutText.changed(put,Map.of("cure","Cure"),7).contains("Bar 3 · Slot 3"));
  // The drag-write confirmations.
  eq(LoadoutText.dragPutLine("Cure",16,7),"Bar 3 · Slot 3: Cure");
  eq(LoadoutText.dragOffLine("Mend"),"Mend off your bar");
  System.out.println("BarPositionTest: "+n+" checks passed");
 }
}
