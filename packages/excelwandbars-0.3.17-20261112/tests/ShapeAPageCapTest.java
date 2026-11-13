package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.BarLoadout;
import com.winkreyes.excelwandbars.core.PageLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/** 0.3.17 (Shape A): layouts fit the wand's EFFECTIVE page count.
 *  The bar packs against a page budget (student 2, master 3, beginner 1);
 *  loadout picks past the budget park in the book region in slot order,
 *  ahead of never-barred spells; nothing is lost; a budget at or past the
 *  loadout's own demand reproduces the pre-0.3.17 layout exactly. */
public class ShapeAPageCapTest {
 static int n; static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 static final List<String> MIKE_BAR = List.of("mend","heal","boon","feed","flashheal","ward","missile",
   "peek","workbench","bubble","cat","familiar","wolf","throwmagictorch","repairequipment",
   "wrv_spark","cure","shell","repairitem","cloak","earth");
 static List<String> wandOrder() {
  List<String> out = new ArrayList<>(MIKE_BAR);
  for (int i = 0; i < 59; i++) out.add("extra" + i);
  return out; // 80 known spells, Mike's fixture shape
 }
 static BarLoadout mikeLoadout() { return BarLoadout.restore(MIKE_BAR, wandOrder(), 21); }
 public static void main(String[]args){
  List<String> order = wandOrder();

  // --- Student budget (cap 2, tap perPage 7): 14 on the bar, the 7 page-3
  // picks park in the book in slot order, then the 59 never-barred spells.
  BarLoadout.Layout s = mikeLoadout().layout(order, 7, 2);
  ok(s.pages == 2);
  ok(s.onBar == 14);
  ok(s.inBook == 66);
  ok(s.slots.size() == 80);
  ok(new java.util.HashSet<>(s.slots.values()).size() == 80); // no position collisions, nothing lost
  ok(s.slots.get("mend") == 0 && s.slots.get("missile") == 6);
  ok(s.slots.get("peek") == 8 && s.slots.get("throwmagictorch") == 14); // page 2, exit column 7/15 skipped
  ok(s.slots.get("repairequipment") == 16 && s.slots.get("wrv_spark") == 17 && s.slots.get("earth") == 22); // parked picks first
  ok(s.slots.get("extra0") == 23 && s.slots.get("extra58") == 81); // then the rest, in wand order
  boolean allUnderCap = s.slots.entrySet().stream().filter(e -> MIKE_BAR.subList(0, 14).contains(e.getKey())).allMatch(e -> e.getValue() < 16);
  ok(allUnderCap);

  // --- Master budget (cap 3) is exactly the pre-0.3.17 uncapped layout.
  BarLoadout.Layout m = mikeLoadout().layout(order, 7, 3);
  BarLoadout.Layout legacy = mikeLoadout().layout(order, 7);
  ok(m.pages == 3 && m.onBar == 21 && m.inBook == 59);
  ok(m.slots.equals(legacy.slots));
  ok(m.slots.get("repairequipment") == 16 && m.slots.get("earth") == 22); // page 3 on the bar
  ok(m.slots.get("extra0") == 24);

  // --- Beginner budget (cap 1): one page of 7, everything else parked.
  BarLoadout.Layout b = mikeLoadout().layout(order, 7, 1);
  ok(b.pages == 1 && b.onBar == 7 && b.inBook == 73 && b.slots.size() == 80);
  ok(b.slots.get("missile") == 6 && b.slots.get("peek") == 8 && b.slots.get("earth") == 21 && b.slots.get("extra0") == 22);

  // --- A budget past the demand changes nothing (short loadout).
  BarLoadout short21 = BarLoadout.restore(List.of("mend","heal","boon","",""), List.of("mend","heal","boon","cure"), 21);
  BarLoadout.Layout capped = short21.layout(List.of("mend","heal","boon","cure"), 7, 2);
  BarLoadout.Layout plain = short21.layout(List.of("mend","heal","boon","cure"), 7);
  ok(capped.slots.equals(plain.slots) && capped.pages == 1 && capped.onBar == 3 && capped.inBook == 1);

  // --- Sneak bars (perPage 8) under a student budget: 16 on the bar.
  BarLoadout.Layout sneak = mikeLoadout().layout(order, 8, 2);
  ok(sneak.pages == 2 && sneak.onBar == 16 && sneak.slots.get("throwmagictorch") == 13 && sneak.slots.get("repairequipment") == 14 && sneak.slots.get("cure") == 16);

  // --- Fixed point: a wand already carrying the capped layout verifies
  // (PageLayout.same over the packed map) and re-laying-out is stable.
  ok(PageLayout.same(new LinkedHashMap<>(s.slots), s.slots));
  BarLoadout.Layout s2 = mikeLoadout().layout(order, 7, 2);
  ok(s2.slots.equals(s.slots));

  // --- PageLayout.packCapped (the no-loadout path): same budget rules.
  Map<String,Integer> cur = new LinkedHashMap<>();
  List<String> spells = new ArrayList<>();
  for (int i = 0; i < 80; i++) { spells.add("sp" + i); cur.put("sp" + i, i); }
  Map<String,Integer> pc = PageLayout.packCapped(cur, spells, 7, 2);
  ok(pc.size() == 80 && new java.util.HashSet<>(pc.values()).size() == 80);
  ok(pc.get("sp0") == 0 && pc.get("sp13") == 14);          // 14 on the bar
  ok(pc.get("sp14") == 16 && pc.get("sp79") == 81);        // 66 parked from slot 16
  Map<String,Integer> pc3 = PageLayout.packCapped(cur, spells.subList(0, 21), 7, 3);
  ok(pc3.equals(PageLayout.pack(cur, spells.subList(0, 21), 7))); // budget >= demand == pack()
  Map<String,Integer> pc1 = PageLayout.packCapped(cur, spells, 7, 1);
  ok(pc1.get("sp6") == 6 && pc1.get("sp7") == 8 && pc1.get("sp79") == 79 + 1);
  Map<String,Integer> pc8 = PageLayout.packCapped(cur, spells, 8, 2);
  ok(pc8.get("sp15") == 15 && pc8.get("sp16") == 16);      // sneak budget: 16 on the bar, book from 16

  System.out.println("PASS "+n+" shape-A page-cap checks");
 }
}
