package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.FormContrast;
import com.winkreyes.excelwandbars.core.SpellbookText;
import com.winkreyes.excelwandbars.core.SpellbookText.Entry;
/** 0.3.14: Bedrock form contrast. Locked rows dark on both fills; the renderer rule rewrites every colour code in a button label to black and nothing else. */
public class FormContrastTest {
 static int n; static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 public static void main(String[]args){
  // The photographed defect, rebuilt exactly: Summon group, locked rows.
  Entry tamePet=new Entry("tamepet","Tame Pet",1,"Apprentice",true);
  String locked=SpellbookText.bedrockButton(tamePet);
  ok(locked.equals("\u00a70Tame Pet - unlocks at Apprentice"));       // dark ink, same words
  ok(!locked.contains("\u00a77"));                                   // the pale grey is gone
  ok(SpellbookText.visible(locked)==32);                             // visible text unchanged
  ok(SpellbookText.visible(locked)<=SpellbookText.MAX);
  // The long-name fallback path is dark too.
  Entry longLocked=new Entry("x","A Very Long Locked Spell Name Indeed",2,"Master",true);
  String ll=SpellbookText.bedrockButton(longLocked);
  ok(ll.startsWith(FormContrast.INK)&&!ll.contains("\u00a77"));
  ok(SpellbookText.visible(ll)<=SpellbookText.MAX);
  // Unlocked rows carry no ink code at all and are byte-identical to 0.3.13.
  Entry open=new Entry("wolf","Wolf",0,"Beginner",false);
  ok(SpellbookText.bedrockButton(open).equals("Wolf"));
  // The source constant itself is the safe ink now.
  ok(SpellbookText.GREY.equals(FormContrast.INK));
  // The hint no longer describes a grey rendering that no longer exists.
  ok(!SpellbookText.LOCKED_HINT.contains("Gray"));
  ok(SpellbookText.groupContent(true,true).contains("Locked spells unlock at a higher rank."));
  // Renderer rule: every legacy colour code becomes black, in place.
  String codes="0123456789abcdefABCDEF";
  for(int i=0;i<codes.length();i++){
   ok(FormContrast.buttonLabel("\u00a7"+codes.charAt(i)+"Row").equals("\u00a70Row"));
   ok(FormContrast.buttonLabel("&"+codes.charAt(i)+"Row").equals("\u00a70Row"));
  }
  // Formatting codes survive untouched (bold locked names stay bold).
  ok(FormContrast.buttonLabel("\u00a7lRow").equals("\u00a7lRow"));
  ok(FormContrast.buttonLabel("\u00a7oRow").equals("\u00a7oRow"));
  ok(FormContrast.buttonLabel("\u00a7rRow").equals("\u00a7rRow"));
  ok(FormContrast.buttonLabel("\u00a7nRow").equals("\u00a7nRow"));
  ok(FormContrast.buttonLabel("\u00a7mRow").equals("\u00a7mRow"));
  ok(FormContrast.buttonLabel("\u00a7kRow").equals("\u00a7kRow"));
  // Mixed label: colour rewritten, formatting kept, words untouched.
  ok(FormContrast.buttonLabel("\u00a77Tame \u00a7lPet").equals("\u00a70Tame \u00a7lPet"));
  ok(FormContrast.buttonLabel("\u00a7a\u00a7bTwo").equals("\u00a70\u00a70Two"));
  // Plain labels (every other WandBars button today) pass through identical.
  ok(FormContrast.buttonLabel("Summon (4)").equals("Summon (4)"));
  ok(FormContrast.buttonLabel("Close").equals("Close"));
  ok(FormContrast.buttonLabel("").equals(""));
  ok(FormContrast.buttonLabel(null)==null);
  // A trailing or lone sign is not a code and is left alone.
  ok(FormContrast.buttonLabel("Row\u00a7").equals("Row\u00a7"));
  ok(FormContrast.buttonLabel("Row\u00a7z").equals("Row\u00a7z"));
  // Idempotent: normalizing a normalized label changes nothing.
  String once=FormContrast.buttonLabel("\u00a77Tame Pet - unlocks at Apprentice");
  ok(FormContrast.buttonLabel(once).equals(once));
  // The source-level locked label is already renderer-clean (belt and braces).
  ok(FormContrast.buttonLabel(locked).equals(locked));
  System.out.println("PASS "+n+" form contrast checks");
 }
}
