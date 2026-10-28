package com.winkreyes.teamrewards.marker;
import java.util.*;
/** Exact loaded-world selection only. Resolving a name does not itself attest protection coverage. */
public final class ReviewedWorlds {
 public record Loaded(String name, UUID id) {
  public Loaded { Objects.requireNonNull(name); Objects.requireNonNull(id); }
 }
 public record Resolution(Set<UUID> ids, List<Loaded> worlds) {
  public Resolution { ids=Set.copyOf(ids); worlds=List.copyOf(worlds); }
 }
 public static Resolution resolve(Object uuidSetting,Object nameSetting,List<Loaded> loaded) {
  List<String> ids=strings(uuidSetting),names=strings(nameSetting);
  Map<String,Loaded> byName=new HashMap<>();Map<UUID,Loaded> byId=new HashMap<>();
  for(Loaded world:loaded) {
   if(byName.putIfAbsent(world.name(),world)!=null || byId.putIfAbsent(world.id(),world)!=null)
    throw new IllegalArgumentException("ambiguous loaded world registry");
  }
  LinkedHashMap<UUID,Loaded> selected=new LinkedHashMap<>();
  for(String value:ids) {
   UUID id=UUID.fromString(value);
   if(!id.toString().equals(value))throw new IllegalArgumentException("noncanonical reviewed world UUID");
   select(byId.get(id),selected);
  }
  for(String name:names)select(byName.get(name),selected);
  return new Resolution(selected.keySet(),new ArrayList<>(selected.values()));
 }
 private static void select(Loaded world,Map<UUID,Loaded> selected) {
  if(world==null)throw new IllegalArgumentException("reviewed world is unknown or unloaded");
  if(Set.of("guild","em_adventurers_guild","minecraft:em_adventurers_guild").contains(world.name().toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("guild is excluded from personal markers");
  if(selected.putIfAbsent(world.id(),world)!=null)throw new IllegalArgumentException("duplicate reviewed world selection");
 }
 private static List<String> strings(Object value) {
  if(value==null)return List.of();
  if(!(value instanceof List<?> list))throw new IllegalArgumentException("reviewed world policy must be a list");
  List<String> out=new ArrayList<>();
  for(Object entry:list) {
   if(!(entry instanceof String s)||s.isEmpty()||!s.equals(s.trim()))throw new IllegalArgumentException("invalid reviewed world policy entry");
   out.add(s);
  }
  return out;
 }
 private ReviewedWorlds(){}
}
