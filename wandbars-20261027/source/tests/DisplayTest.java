import com.winkreyes.excelwandbars.core.*;
import java.util.*;
public class DisplayTest {
 public static void run() {
  System.out.println("DisplayTest"); StyleProfile profile=new StyleProfile();
  for(WandStyle style:WandStyle.values()){
   Map<String,String> target=profile.wanted(style);
   Check.eq("none",target.get("xp_display"),style+" restores vanilla XP fraction");
   Check.eq("none",target.get("level_display"),style+" restores vanilla XP level");
   Check.ok(!target.containsKey("boss_bar"),style+" retains existing top mana bar");
   Map<String,String> existing=new HashMap<>(target);existing.put("xp_display","mana");existing.put("level_display","sp");
   Map<String,String> delta=profile.changes(existing,style);
   Check.eq(Map.of("xp_display","none","level_display","none"),delta,style+" only display overrides change on matching old wand");
   existing.putAll(delta);Check.ok(profile.changes(existing,style).isEmpty(),style+" converges once without repeated rewrites");
   for(String k:List.of("mana","mana_max","mana_regeneration","sp","xp","level","boss_bar")) Check.ok(!delta.containsKey(k),style+" leaves resource value "+k+" alone");
  }
 }
}
