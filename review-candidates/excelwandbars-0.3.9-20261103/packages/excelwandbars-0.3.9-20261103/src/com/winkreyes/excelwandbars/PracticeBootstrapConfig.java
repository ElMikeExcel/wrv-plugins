package com.winkreyes.excelwandbars;
import java.util.*;import org.bukkit.configuration.ConfigurationSection;import wrv.magic.retention.BoundedPractice;import wrv.sp.route.SingleSpRoute;
/** Explicit validated activation inputs. Missing section is OFF, never an inferred rollout. */
final class PracticeBootstrapConfig {
 record Loaded(PreparedPracticeFactory.Settings settings,FirstObservationFloor.Policy migration,String practiceTargets,String spRouting,Map<String,Integer> minimumSeals){Loaded{minimumSeals=Map.copyOf(minimumSeals);}}
 static Optional<Loaded> read(ConfigurationSection c){
  if(c==null)return Optional.empty();Object on=c.get("enabled");if(on==null||Boolean.FALSE.equals(on))return Optional.empty();if(!Boolean.TRUE.equals(on))throw new IllegalArgumentException("spell-practice.enabled must be boolean");
  String practice=choice(c,"practice-targets",Set.of("players-hostiles","players-and-pets-hostiles"));
  String sp=c.contains("sp-routing")?choice(c,"sp-routing",Set.of("legacy")):"legacy";
  String epoch=required(c,"migration-epoch");int migrationCapacity=integer(c,"migration-capacity",1,10000000);
  ConfigurationSection spells=c.getConfigurationSection("spells");if(spells==null||spells.getKeys(false).isEmpty())throw new IllegalArgumentException("explicit spells required");
  Map<String,PreparedPracticeFactory.SpellPolicy> policies=new TreeMap<>();Map<String,Integer> minimumSeals=new TreeMap<>();
  for(String key:spells.getKeys(false)){
   ConfigurationSection s=spells.getConfigurationSection(key);if(s==null)throw new IllegalArgumentException("spell section required");
   minimumSeals.put(key,integer(s,"minimum-seal",0,5));
   Object raw=s.get("accepted-kinds");if(!(raw instanceof List<?> list)||list.isEmpty())throw new IllegalArgumentException("accepted-kinds required");
   Set<BoundedPractice.EffectiveKind> kinds=EnumSet.noneOf(BoundedPractice.EffectiveKind.class);for(Object kind:list){if(!(kind instanceof String value))throw new IllegalArgumentException("kind must be string");kinds.add(BoundedPractice.EffectiveKind.valueOf(value));}
   policies.put(key,new PreparedPracticeFactory.SpellPolicy(required(s,"policy-id"),integer(s,"native-levels",2,6),kinds,integer(s,"minimum-interval-ms",0,86400000),integer(s,"daily-cap",0,1000000),integer(s,"receipt-capacity",1,10000000)));
  }
  var settings=new PreparedPracticeFactory.Settings(policies,integer(c,"issuer-capacity",1,10000000),1,integer(c,"receipt-capacity",1,10000000),integer(c,"player-capacity",1,1000000),SingleSpRoute.LateProof.REQUIRE_BEFORE_EVENT);
  return Optional.of(new Loaded(settings,new FirstObservationFloor.Policy(epoch,true,migrationCapacity),practice,sp,minimumSeals));
 }
 static String required(ConfigurationSection c,String key){Object v=c.get(key);if(!(v instanceof String s)||s.isBlank())throw new IllegalArgumentException("explicit "+key+" required");return s;}
 static int integer(ConfigurationSection c,String key,int min,int max){Object v=c.get(key);if(!(v instanceof Integer)||((int)v)<min||((int)v)>max)throw new IllegalArgumentException("invalid integer "+key);return (int)v;}
 static String choice(ConfigurationSection c,String key,Set<String> choices){String v=required(c,key);if(!choices.contains(v))throw new IllegalArgumentException("unknown "+key);return v;}
 private PracticeBootstrapConfig(){}
}
