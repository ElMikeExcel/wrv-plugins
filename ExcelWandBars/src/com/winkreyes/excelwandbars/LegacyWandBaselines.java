package com.winkreyes.excelwandbars;
import java.util.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import wrv.magic.level.RawWandLoadBridge;
/** Excel: migration floors, NOT earned casts. Native old-JAR + verified pre-update config fixture.
 * wands SHA256 4f8ee7bbfb7693e8fe33155012435eac19acaac72bb2e6ad1d8a1ee7213aeb73;
 * spells 229f8c614d141c85a20b6893de24978965e7e1ab7bb62c41dc1401ba19f21c89;
 * paths 95d9f0a783d96b0a383c48f1ae6b5371020b70caacc318780e18c3e5e7368fbd.
 * Slot values never represent levels. Only historical possession keys support an inherited floor. */
final class LegacyWandBaselines {
 static final String PROVENANCE="excel-legacy-v7-preupdate-20261029-4f8ee7bb-229f8c61-95d9f0a7";
 private static final Map<String,Map<String,Integer>> OLD=Map.of(
  "beginner",Map.of("heal",2,"mend",2),
  "student",Map.of("heal",2,"mend",3,"cure",2,"flashheal",2,"stormshackle",1),
  "apprentice",Map.of("heal",3,"mend",4,"cure",3,"flashheal",3,"greaterheal",2,"stormshackle",1),
  "master",Map.of("heal",3,"mend",5,"cure",5,"flashheal",4,"greaterheal",3,"stormshackle",1));
 static Map<String,Integer> resolve(RawWandLoadBridge.Snapshot raw,String text,Map<String,Integer> maximum,
  Map<String,Integer> explicit)throws Exception {
  var cfg=new YamlConfiguration();cfg.loadFromString(text);
  if(cfg.getInt("version",-1)!=7||!OLD.containsKey(raw.template()))throw new IllegalArgumentException("unreviewed legacy wand schema/template");
  Set<String> possessed=new HashSet<>();Object slots=cfg.get("spell_inventory");
  Map<?,?> map=slots instanceof ConfigurationSection c?c.getValues(false):slots instanceof Map<?,?> m?m:slots==null?Map.of():null;
  if(map==null||map.size()>4096)throw new IllegalArgumentException("unknown inventory shape");
  for(var e:map.entrySet()){
   if(!(e.getKey() instanceof String key)||!key.matches("[a-z0-9_.-]+")||!(e.getValue() instanceof Integer position)||position<0||position>4095)throw new IllegalArgumentException("unknown slot shape");
   possessed.add(key);
  }
  var result=new TreeMap<String,Integer>();
  for(var policy:maximum.entrySet()){
   int level=1;
   if(raw.ownerId()!=null&&!raw.ownerId().isBlank()&&raw.spells()==null&&possessed.contains(policy.getKey()))level=OLD.get(raw.template()).getOrDefault(policy.getKey(),1);
   if(explicit.containsKey(policy.getKey())){level=explicit.get(policy.getKey());if((raw.ownerId()==null||raw.ownerId().isBlank())&&level>1)throw new IllegalArgumentException("unowned explicit rank requires reviewed provisioning");}
   if(level>policy.getValue())throw new IllegalArgumentException("historical level exceeds reviewed maximum; no clamp");
   result.put(policy.getKey(),level);
  }
  return Map.copyOf(result);
 }
 private LegacyWandBaselines(){}
}
