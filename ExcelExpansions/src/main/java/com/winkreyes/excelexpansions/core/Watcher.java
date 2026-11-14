package com.winkreyes.excelexpansions.core;
import java.time.*; import java.util.*;
public final class Watcher {
 public record Player(UUID uuid, Instant lastPlayed, OptionalInt combatLevel){}
 public enum Verdict{ DUE, NOT_DUE, NOT_EVALUABLE, SUPPORT_NEVER, UNASSIGNED_NEVER, SEASONAL_DUE, RELEASED }
 public record Result(Verdict verdict,String reason){}
 private final Integer dynamicFloor; // config watcher.dynamic-floor; null = unset (default) - never invented
 public Watcher(Integer dynamicFloor){this.dynamicFloor=dynamicFloor;}
 public Integer dynamicFloor(){return dynamicFloor;}
 public static List<Player> activeRoster(List<Player> all, Instant now){ // ACTIVE = played within rolling 336h (DESIGN 2.1)
   return all.stream().filter(p->p.lastPlayed()!=null && !p.lastPlayed().isBefore(now.minus(Duration.ofHours(336)))).toList(); }
 public static boolean seasonalInWindow(ManifestRow r, Month currentMonth, boolean newYearTurn){ // LOCKED windows (Mike, Nov 14 2026): months/occasions only, no exact dates
   return switch(r.slug()){ case "north-pole"->currentMonth==Month.DECEMBER; case "fireworks"->newYearTurn; case "the-hallowed-haunt","craftenmines-harvester-event"->currentMonth==Month.OCTOBER; default->false; }; }
 public Result evaluate(ManifestRow r, List<ManifestRow> all, List<Player> active, Set<String> releasedSlugs, Month currentMonth, boolean newYearTurn){
  if("released".equals(r.releasedState())) return new Result(Verdict.RELEASED,"already released - stays released");
  if(r.support()) return new Result(Verdict.SUPPORT_NEVER,"support rows never propose");
  if(r.unassigned()) return new Result(Verdict.UNASSIGNED_NEVER,"phase: unassigned - never DUE until Mike assigns a phase (#15)");
  if(r.seasonal()){ boolean in = seasonalInWindow(r,currentMonth,newYearTurn);
    if("craftenmines-harvester-event".equals(r.slug())) return new Result(Verdict.UNASSIGNED_NEVER,"seasonal story placement but phase: unassigned / no content captured - cannot fire until captured and phased (#15)");
    return in? new Result(Verdict.SEASONAL_DUE,"season window open - Mike tap still gates") : new Result(Verdict.NOT_DUE,"outside season window"); }
  if(active.size()<3) return new Result(Verdict.NOT_DUE,"ACTIVE floor of 3 not met: "+active.size());
  // phase order: every earlier non-seasonal row must be released
  for(ManifestRow o: all){ if(o.seasonal()||o.support()||o.unassigned()||o==r) continue;
    if(phaseKey(o)<phaseKey(r) || (phaseKey(o)==phaseKey(r)&&o.phaseOrder()<r.phaseOrder())) if(!"released".equals(o.releasedState()) && !releasedSlugs.contains(o.slug())) return new Result(Verdict.NOT_DUE,"phase order blocked by "+o.id()); }
  for(String req: r.requires()) if(!releasedSlugs.contains(req) && !"free-minecraft-models".equals(req)) return new Result(Verdict.NOT_DUE,"requires not released: "+req);
  // FMM prerequisite is VERIFIED live per LOCK-PREP #17, treated as released for requires purposes by caller passing it in releasedSlugs
  if(!r.evaluable()) return new Result(Verdict.NOT_EVALUABLE,"band_source pending contents check");
  Integer floor;
  if(r.dynamic()){ if(dynamicFloor==null) return new Result(Verdict.NOT_EVALUABLE,"dynamic row: watcher floor TBD at build (config watcher.dynamic-floor unset) - no floor invented"); floor=dynamicFloor; }
  else { if(r.bandMin()==null) return new Result(Verdict.NOT_EVALUABLE,"no band_min asserted for this row (see band_note)"); floor=r.bandMin(); }
  long inBand=active.stream().filter(p->p.combatLevel().isPresent() && p.combatLevel().getAsInt()>=floor).count();
  return (inBand*2>active.size())? new Result(Verdict.DUE,inBand+" of "+active.size()+" active in band") : new Result(Verdict.NOT_DUE,inBand+" of "+active.size()+" active in band - no strict majority"); }
 private static int phaseKey(ManifestRow r){ try{return Integer.parseInt(r.phase());}catch(Exception e){return 99;} }
}
