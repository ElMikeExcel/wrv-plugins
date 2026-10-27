package com.winkreyes.exceltelemetry;
import com.winkreyes.exceltelemetry.integration.TrialCompatibility;
import com.winkreyes.exceltelemetry.core.Encounter;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.entity.EntityRemoveEvent;
public class TrialCompatibilityTest {
 static int count;
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);count++;System.out.println("ok "+m);}
 public static void main(String[] args)throws Exception {
  check(TrialCompatibility.provider(false,false)==null,"no providers safely absent");
  check("ExcelBossTrial".equals(TrialCompatibility.provider(true,true)),"BossTrial precedence");
  check("ExcelDragonTrial".equals(TrialCompatibility.provider(false,true)),"legacy fallback");
  check("dragon/state.yml".equals(TrialCompatibility.statePath("ExcelBossTrial")),"BossTrial chapter path");
  check("state.yml".equals(TrialCompatibility.statePath("ExcelDragonTrial")),"legacy root path");
  check("phase1".equals(TrialCompatibility.phase(400,400,900)),"current phase1");
  check("phase2".equals(TrialCompatibility.phase(900,400,900)),"current phase2");
  check("unknown".equals(TrialCompatibility.phase(400,250,300)),"stale config not pretend phase");
  check("unknown".equals(TrialCompatibility.phase(400,400,410)),"ambiguous config not phase1");
  check("unknown".equals(TrialCompatibility.phase(Double.NaN,400,900)),"NaN safe");
  check("phase1".equals(TrialCompatibility.phase(250,250,300)),"legacy phase1");
  check(!TrialCompatibility.chapterActive(false,true),"disabled root excluded");
  check(!TrialCompatibility.chapterActive(true,false),"disabled chapter excluded");
  check(TrialCompatibility.chapterActive(true,true),"enabled chapter accepted");
  UUID id=UUID.randomUUID(); var yaml=new YamlConfiguration();
  yaml.loadFromString("worlds:\n  world_the_end:\n    phase: PHASE2\n    dragon-uuid: "+id+"\n");
  check("PHASE2".equals(TrialCompatibility.savedPhase(yaml,"world_the_end",id)),"matching current dragon phase");
  check(TrialCompatibility.savedPhase(yaml,"world_the_end",UUID.randomUUID())==null,"stale dragon rejected");
  check(TrialCompatibility.savedPhase(yaml,"other",id)==null,"wrong world rejected");
  check(TrialCompatibility.savedPhase(null,"world_the_end",id)==null,"missing state safe");
  yaml.set("worlds.world_the_end.phase","TRANSITION");
  check("TRANSITION".equals(TrialCompatibility.savedPhase(yaml,"world_the_end",id)),"transition preserved");
  yaml.set("worlds.world_the_end.phase","DONE");
  check(TrialCompatibility.savedPhase(yaml,"world_the_end",id)==null,"completed stale state rejected");
  yaml.set("worlds.world_the_end.phase","PHASE1");yaml.set("worlds.world_the_end.dragon-uuid",null);
  check(TrialCompatibility.savedPhase(yaml,"world_the_end",id)==null,"missing uuid rejected");
  check(TrialCompatibility.isUnload(EntityRemoveEvent.Cause.UNLOAD.name()),"actual Paper26.2 UNLOAD enum");
  check(!TrialCompatibility.isUnload(EntityRemoveEvent.Cause.DEATH.name()),"death separate");
  check(!TrialCompatibility.isUnload("DISCARD"),"other remove separate");
  var e=new Encounter("boss:x","ender_dragon","x","world_the_end",1000);
  e.recordDamageToBoss("player:x","x","Player","ENTITY_ATTACK",10,"phase1",2000);
  e.addMarker("boss_unloaded",5000,"UNLOAD");
  check(e.isOpen(),"unload marker doesn't complete fight");
  check(e.lastActivityMs()==2000,"unload doesn't extend idle retention");
  e.recordDamageToBoss("player:x","x","Player","ENTITY_ATTACK",20,"phase2",6000);
  check(e.totalDamageToBoss()==30,"resume accumulates same encounter");
  check(606001-e.lastActivityMs()>600000,"idle age remains bounded");
  e.close(Encounter.Outcome.ABANDONED);
  check(!e.success(),"idle abandonment never success");
  System.out.println("ALL PASS "+count);
 }
}
