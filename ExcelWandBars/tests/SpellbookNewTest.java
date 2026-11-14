package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.SpellbookGroups;
import com.winkreyes.excelwandbars.core.SpellbookText;
import com.winkreyes.excelwandbars.core.SpellbookText.Entry;
import java.util.ArrayList;
import java.util.List;
/** 0.3.5 design 5.3: the Spell Book's New group (a view, never a stored collection). */
public class SpellbookNewTest {
 static int n; static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 static Entry e(String key,String name,int band,boolean locked,boolean isNew){
  Entry x=new Entry(key,name,band,"Rank",locked); x.isNew=isNew; return x; }
 public static void main(String[]args){
  List<Entry> es=new ArrayList<>(List.of(
   e("mend","Mend",0,false,true),
   e("boon","Boon",1,false,true),
   e("heal","Heal",0,false,false),
   e("tame","Tame",2,true,true),      // locked: never in the New tab even if flagged
   e("shackle","Storm Shackle",1,false,true)));
  // Filtering: unlocked + flagged only, rank band then name.
  List<Entry> nl=SpellbookText.newList(es);
  ok(nl.size()==3);
  ok(nl.get(0).key.equals("mend")&&nl.get(1).key.equals("boon")&&nl.get(2).key.equals("shackle"));
  ok(SpellbookText.newCount(es)==3);
  ok(SpellbookText.newCount(List.of())==0);
  // The generic list() routes the pseudo-group to the same view.
  SpellbookGroups g=new SpellbookGroups();
  ok(SpellbookText.list(g,es,SpellbookText.NEW,false).equals(nl));
  ok(SpellbookText.list(g,es,SpellbookText.NEW,true).equals(nl));
  // The badge button shows the count and fits the form limit.
  ok(SpellbookText.newButton(2).equals("New spells (2)"));
  ok(SpellbookText.visible(SpellbookText.newButton(12))<=SpellbookText.MAX);
  ok(SpellbookText.visible(SpellbookText.MARK_ALL)<=SpellbookText.MAX);
  ok(SpellbookText.visible(SpellbookText.GOT_IT)<=SpellbookText.MAX);
  ok(SpellbookText.visible(SpellbookText.groupTitle(SpellbookText.NEW_NAME,3,0,1))<=SpellbookText.MAX);
  // An entry stops being new the moment its flag clears (the model acked it).
  es.get(0).isNew=false;
  ok(SpellbookText.newCount(es)==2);
  ok(SpellbookText.newList(es).get(0).key.equals("boon"));
  System.out.println("PASS "+n+" spellbook new-group checks");
 }
}
