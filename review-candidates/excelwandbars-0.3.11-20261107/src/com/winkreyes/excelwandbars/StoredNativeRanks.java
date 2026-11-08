package com.winkreyes.excelwandbars;
import java.util.*;import java.util.function.Function;
/** Raw serialized rank extraction only. Never reads inherited templates, adds a spell or invents casts. */
final class StoredNativeRanks {
 record Parsed(Map<String,Integer> ranks,Set<String> possessed){Parsed{ranks=Map.copyOf(ranks);possessed=Set.copyOf(possessed);}}
 static Parsed parse(Object rawSpells,Map<String,Integer> rawLevels,Map<String,Integer> managedMax,Function<String,String> canonicalBase){
  Objects.requireNonNull(rawLevels);Objects.requireNonNull(managedMax);Objects.requireNonNull(canonicalBase);
  List<?> list;if(rawSpells==null)list=List.of();else if(rawSpells instanceof String s){if(s.length()>1000000)throw new IllegalArgumentException("spells size");list=Arrays.asList(s.replace("[","").replace("]","").split(","));}else if(rawSpells instanceof List<?> l)list=l;else throw new IllegalArgumentException("raw spells shape");
  if(list.size()>4096||rawLevels.size()>4096)throw new IllegalArgumentException("raw spell capacity");
  Map<String,Integer> ranks=new TreeMap<>();Set<String> possessed=new TreeSet<>();
  for(Object entry:list){if(!(entry instanceof String text))throw new IllegalArgumentException("raw spell value");text=text.trim();if(text.isEmpty()||text.equalsIgnoreCase("none")||text.equalsIgnoreCase("newpage"))continue;
   String key=text.split("@",2)[0].trim();String[] parts=key.split("\\|",-1);if(parts.length>2||!parts[0].matches("[a-z0-9_.-]{1,200}"))throw new IllegalArgumentException("noncanonical stored spell");
   int rank=parts.length==1?1:Integer.parseInt(parts[1]);String base=canonicalBase.apply(parts[0]);if(base==null)throw new IllegalArgumentException("unresolved stored spell");
   if(!managedMax.containsKey(base))continue;check(base,rank,managedMax);ranks.merge(base,rank,Math::max);possessed.add(base);
  }
  // Native loadSpellLevels runs after loadSpells and can override levels even without possession.
  for(var e:rawLevels.entrySet()){
   if(e.getKey()==null||!e.getKey().matches("[a-z0-9_.-]{1,200}")||e.getValue()==null)throw new IllegalArgumentException("raw level shape");
   String base=canonicalBase.apply(e.getKey());if(base==null)throw new IllegalArgumentException("unresolved stored level");
   if(!managedMax.containsKey(base))continue;check(base,e.getValue(),managedMax);ranks.put(base,e.getValue());
  }
  return new Parsed(ranks,possessed);
 }
 private static void check(String base,int rank,Map<String,Integer> maximum){Integer max=maximum.get(base);if(max==null||max<1||max>6||rank<1||rank>max)throw new IllegalArgumentException("stored rank outside reviewed native range");}
 private StoredNativeRanks(){}
}
