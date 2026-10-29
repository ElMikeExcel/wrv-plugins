package com.winkreyes.excelwandbars.core;
import java.util.*;
/** Read-only display snapshot. Null in SpellFacts means unmanaged legacy display. */
public record PracticeDisplay(boolean available,int activeLevel,int learnedLevel,int maxLevel,long usefulCount,Long nextThreshold,String requirement){
 public PracticeDisplay{requirement=requirement==null?"":requirement;if(available&&(activeLevel<1||learnedLevel<activeLevel||maxLevel<learnedLevel||usefulCount<0||nextThreshold!=null&&nextThreshold<=usefulCount))throw new IllegalArgumentException("incoherent practice display");}
 public static PracticeDisplay unavailable(){return new PracticeDisplay(false,0,0,0,0,null,"");}
 public List<String> lines(){
  if(!available)return List.of("Spell practice unavailable right now.","This view does not change saved progress.");
  var out=new ArrayList<String>();out.add("Active rank: "+activeLevel+" of "+maxLevel);
  if(learnedLevel>activeLevel)out.add("Learned rank "+learnedLevel+" saved; currently gated.");
  if(nextThreshold!=null)out.add("Useful casts: "+usefulCount+" / "+nextThreshold+" for learned rank "+(learnedLevel+1));
  else out.add("Useful casts: "+usefulCount+". Top learned rank reached.");
  if(!requirement.isBlank())out.add("Requirement: "+requirement);
  return List.copyOf(out);
 }
 public String compact(){if(!available)return "Spell practice unavailable";return "Active "+activeLevel+", learned "+learnedLevel+(nextThreshold==null?" | top learned rank":" | "+usefulCount+"/"+nextThreshold+" useful casts");}
}
