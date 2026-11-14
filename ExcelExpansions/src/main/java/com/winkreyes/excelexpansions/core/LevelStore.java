package com.winkreyes.excelexpansions.core;
import java.io.*; import java.nio.charset.StandardCharsets; import java.nio.file.*; import java.time.Instant; import java.util.*;
/** ExcelExpansions' own persistent per-player level store (Mike, Nov 14 2026): EliteMobs stores no canonical
 *  combat level (em_player_data.db has Score / HighestLevelKilled / Kills, no level column - proven, #16).
 *  Band-signal fields carry NAMED EM sources only; last-active feeds the 14-day ACTIVE window.
 *  Refreshed on join/quit + periodically while online (host wiring). The watcher never reads an invented
 *  or XP-converted number. Which field is canonical for IN-BAND is the rig-owed #16 decision. */
public final class LevelStore {
 public record Entry(UUID uuid, Integer highestLevelKilled, Long score, Instant lastActive, String sourceNote){}
 private final Map<UUID,Entry> map=new LinkedHashMap<>();
 public synchronized void put(Entry e){map.put(e.uuid(),e);} public synchronized Optional<Entry> get(UUID u){return Optional.ofNullable(map.get(u));}
 public synchronized Collection<Entry> all(){return List.copyOf(map.values());} public synchronized int size(){return map.size();}
 /** Stamp activity (Bukkit last-seen / join / quit - host-supplied, named at the call site).
  *  Creates a signal-less entry when none exists; NEVER moves lastActive backwards and
  *  NEVER touches stored signals - a refresh that cannot read EM retains last-known values. */
 public synchronized void touch(UUID u, Instant when, String note){
  if(when==null) return; Entry old=map.get(u);
  if(old==null){ map.put(u,new Entry(u,null,null,when,note)); return; }
  Instant la = (old.lastActive()==null || when.isAfter(old.lastActive())) ? when : old.lastActive();
  map.put(u,new Entry(u,old.highestLevelKilled(),old.score(),la,old.sourceNote())); }
 /** Apply one successful EM read (EmDbReader signals). Signals only - lastActive is
  *  activity-sourced and is not moved by a data read. A null signal in the read
  *  retains the previously stored value rather than erasing it. */
 public synchronized void putSignals(UUID u, Integer highestLevelKilled, Long score, String note){
  Entry old=map.get(u);
  Integer h = highestLevelKilled!=null ? highestLevelKilled : (old==null?null:old.highestLevelKilled());
  Long s = score!=null ? score : (old==null?null:old.score());
  map.put(u,new Entry(u,h,s,old==null?null:old.lastActive(),note)); }
 public synchronized void save(Path f) throws IOException { List<String> ls=new ArrayList<>(); ls.add("# ExcelExpansions level store - fields: uuid, highestLevelKilled (EM source), score (EM source), lastActiveEpochMs, sourceNote");
  for(Entry e: map.values()) ls.add(e.uuid()+"|"+(e.highestLevelKilled()==null?"":e.highestLevelKilled())+"|"+(e.score()==null?"":e.score())+"|"+(e.lastActive()==null?"":e.lastActive().toEpochMilli())+"|"+(e.sourceNote()==null?"":e.sourceNote()));
  Files.write(f,ls,StandardCharsets.UTF_8); }
 public static LevelStore load(Path f) throws IOException { LevelStore s=new LevelStore(); if(!Files.exists(f)) return s;
  for(String l: Files.readAllLines(f,StandardCharsets.UTF_8)){ if(l.isBlank()||l.startsWith("#")) continue; String[] p=l.split("\\|",-1); if(p.length<5) continue;
   s.put(new Entry(UUID.fromString(p[0]), p[1].isBlank()?null:Integer.valueOf(p[1]), p[2].isBlank()?null:Long.valueOf(p[2]), p[3].isBlank()?null:Instant.ofEpochMilli(Long.parseLong(p[3])), p[4])); } return s; }
}
