import com.winkreyes.excelwandbars.core.*;
import java.util.*;
public class ExitSlotTest {
 public static void run() {
  Check.eq(ExitSlotPolicy.perPage(WandStyle.TAPBAR,8,8),7,"Tap bar cannot configure away exit");
  Check.eq(ExitSlotPolicy.perPage(WandStyle.BARS,8,8),8,"other style unchanged");
  Check.eq(ExitSlotPolicy.perPage(WandStyle.ORIGINAL,8,8),8,"original unchanged");
  Check.ok(ExitSlotPolicy.pagesRequired(WandStyle.TAPBAR,false),"Tap bar retains pages even disabled setting");
  for(int n=0;n<=100;n++) {
   List<String> spells=new ArrayList<>(); for(int i=0;i<n;i++)spells.add("spell"+i);
   Map<String,Integer> packed=PageLayout.pack(null,spells,ExitSlotPolicy.perPage(WandStyle.TAPBAR,8,8));
   Check.eq(packed.size(),n,"all known spells preserved "+n);
   for(int pos:packed.values())Check.ok(pos%8!=7,"reserved column never filled "+n+":"+pos);
   BarLoadout l=BarLoadout.seed(spells,21);
   for(int i=0;i<21;i++) l.put("spell"+i,i);
   BarLoadout.Layout a=l.layout(spells,7);
   for(int pos:a.slots.values()) if(pos<a.pages*8)Check.ok(pos%8!=7,"manual loadout exit reserved");
   Check.eq(a.slots.size(),n,"book overflow retained");
   l=BarLoadout.restore(l.storedSlots(),l.seen(),21);
   Check.eq(l.layout(spells,7).slots,a.slots,"reload keeps mapping");
  }
  BarLoadout l=new BarLoadout(21);l.put("mend",7);
  Check.eq(l.layout(List.of("mend"),7).slots.get("mend"),8,"logical slot8 becomes page2slot1 not forbidden");
 }
}
