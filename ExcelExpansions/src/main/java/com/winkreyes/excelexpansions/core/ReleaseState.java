package com.winkreyes.excelexpansions.core;
import java.io.*; import java.nio.charset.StandardCharsets; import java.nio.file.*; import java.time.Instant; import java.util.*;
/** Persistent release-state overlay for ExcelExpansions 0.1.1 (DESIGN 1.2 state machine).
 *  releases.yml carries each row's INITIAL state; this store carries the live overlay -
 *  due / proposed / released / retirement-proposed / retired-seasonal plus the audit fields
 *  (due_since, proposed_at, released_at, released_by, per-episode tap token, note).
 *  Persisted at plugins/ExcelExpansions/release-state.txt (pipe-delimited, one line per row
 *  that has left its initial state). A restart reloads it, so a released pack is never
 *  re-proposed or re-flipped and an untapped proposal is never re-posted (DESIGN 3.3).
 *  Writes go through a temp file + move so a crash mid-write cannot corrupt the store. */
public final class ReleaseState {
 public static final String UNRELEASED="unreleased", DUE="due", PROPOSED="proposed", RELEASED="released",
   RETIREMENT_PROPOSED="retirement-proposed", RETIRED="retired-seasonal";
 public record Entry(String rowId, String state, Instant dueSince, Instant proposedAt, Instant releasedAt,
   String releasedBy, String token, String note){}
 private final Map<String,Entry> map=new LinkedHashMap<>();
 public synchronized Optional<Entry> get(String rowId){ return Optional.ofNullable(map.get(rowId)); }
 public synchronized void put(Entry e){ map.put(e.rowId(),e); }
 public synchronized int size(){ return map.size(); }
 public synchronized Collection<Entry> all(){ return List.copyOf(map.values()); }
 /** Effective state of a row: the overlay when present, else the row's bundled initial state. */
 public synchronized String stateOf(ManifestRow r){ Entry e=map.get(r.id()); return e!=null? e.state() : r.releasedState(); }
 private static String clean(String s){ return s==null? "" : s.replace('|','/').replace('\n',' ').replace('\r',' '); }
 private static String ms(Instant i){ return i==null? "" : Long.toString(i.toEpochMilli()); }
 private static Instant ms(String s){ return s==null||s.isBlank()? null : Instant.ofEpochMilli(Long.parseLong(s.trim())); }
 public synchronized void save(Path f) throws IOException {
  List<String> ls=new ArrayList<>(); ls.add("# ExcelExpansions release state - fields: rowId, state, dueSinceMs, proposedAtMs, releasedAtMs, releasedBy, token, note");
  for(Entry e: map.values()) ls.add(String.join("|", e.rowId(), e.state(), ms(e.dueSince()), ms(e.proposedAt()), ms(e.releasedAt()), clean(e.releasedBy()), clean(e.token()), clean(e.note())));
  Path tmp=f.resolveSibling(f.getFileName()+".tmp"); Files.write(tmp,ls,StandardCharsets.UTF_8);
  try{ Files.move(tmp,f,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
  catch(AtomicMoveNotSupportedException x){ Files.move(tmp,f,StandardCopyOption.REPLACE_EXISTING); } }
 public static ReleaseState load(Path f) throws IOException { ReleaseState s=new ReleaseState(); if(!Files.exists(f)) return s;
  for(String l: Files.readAllLines(f,StandardCharsets.UTF_8)){ if(l.isBlank()||l.startsWith("#")) continue; String[] p=l.split("\\|",-1); if(p.length<8) continue;
   s.put(new Entry(p[0],p[1],ms(p[2]),ms(p[3]),ms(p[4]),p[5].isBlank()?null:p[5],p[6].isBlank()?null:p[6],p[7].isBlank()?null:p[7])); } return s; }
}
