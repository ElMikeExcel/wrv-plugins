package com.winkreyes.excelexpansions.core;
import java.nio.charset.StandardCharsets; import java.security.MessageDigest; import java.time.*; import java.util.*;
/** Proposal engine for ExcelExpansions 0.1.1 - DESIGN section 3 (proposal flow) and 4.2 (seasonal flow).
 *  Pure logic, Bukkit-free: the host renders each returned Proposal as an in-game chat card
 *  (Adventure click component for Java + a typed-command line for Bedrock) and persists the
 *  ReleaseState this engine mutates. Rules implemented exactly:
 *  - a pack that goes DUE (Watcher verdict) produces ONE proposal per due episode;
 *  - untapped stays due SILENTLY: a row already in state "proposed" never produces another
 *    proposal (no re-spam); re-surfacing is the host's rundown command only (DESIGN 3.4);
 *  - a majority that lapses after proposal does NOT withdraw the card (DESIGN 2.3): the row
 *    stays "proposed" and the lapse is only noted on its state entry for the rundown;
 *  - one non-seasonal headline in flight at a time (DESIGN 3.6): a second DUE row is recorded
 *    as "due" (not proposed) until the in-flight proposal is tapped;
 *  - seasonal proposals are independent of the headline slot and fire on their LOCKED window
 *    only; a released seasonal pack whose window has closed produces ONE retirement proposal;
 *    a retired-seasonal pack proposes again at its next window (DESIGN 4.2);
 *  - released stays released: released rows are never re-evaluated for proposals. */
public final class Proposer {
 public enum Kind{ RELEASE, RETIREMENT }
 public record Proposal(String rowId, String slug, String displayName, Kind kind, String phase,
   String bandLabel, String bandText, String bandSource, String reason, String urgencyNote,
   String bandNote, String token, String tapCommand){}
 private final Watcher watcher;
 public Proposer(Watcher watcher){ this.watcher=watcher; }
 /** Per-episode tap token: deterministic in (row, episode start) so a stale card from an
  *  earlier episode can never match the live proposal (DESIGN 7.12 stale-tap handling). */
 public static String tokenFor(String rowId, Instant episodeStart){
  try{ byte[] h=MessageDigest.getInstance("SHA-256").digest((rowId+"|"+episodeStart.toEpochMilli()).getBytes(StandardCharsets.UTF_8));
   StringBuilder sb=new StringBuilder(); for(int i=0;i<5;i++) sb.append(String.format("%02x",h[i])); return sb.toString(); }
  catch(Exception e){ return Integer.toHexString((rowId+episodeStart.toEpochMilli()).hashCode()); } }
 public static String bandLabel(ManifestRow r){
  if(r.seasonal()) return r.seasonWindowLabel()!=null? r.seasonWindowLabel() : "seasonal";
  if(r.bandMin()!=null) return r.bandMin()+"-"+r.bandMax();
  if(r.dynamic()) return "Dynamic (chosen at entry)";
  return "no band asserted - see band note"; }
 private String urgency(ManifestRow r, List<Watcher.Player> active){
  if(r.bandMax()==null||r.bandKind()==null) return "";
  if(!("fixed".equals(r.bandKind())||"fixed-band".equals(r.bandKind()))) return "";
  int top=active.stream().filter(p->p.combatLevel().isPresent()).mapToInt(p->p.combatLevel().getAsInt()).max().orElse(-1);
  if(top>=r.bandMax()) return "Fixed band: an active player is already at/above this band's top ("+r.bandMax()+") - fixed content goes flat once out-levelled, so this one should not wait.";
  if(top>=Math.ceil(r.bandMax()*0.8)) return "Fixed band: actives are approaching this band's top ("+r.bandMax()+") - fixed content goes flat once out-levelled.";
  return ""; }
 private Proposal proposal(ManifestRow r, Kind kind, String reason, List<Watcher.Player> active, String token){
  return new Proposal(r.id(), r.slug(), r.displayName(), kind, r.phase(), bandLabel(r),
   kind==Kind.RETIREMENT? "Seasonal - window closed" : (r.bandText()!=null? r.bandText() : ""), r.bandSource(), reason,
   kind==Kind.RELEASE? urgency(r,active) : "", r.bandNote()==null? "" : r.bandNote(), token,
   "/excelexpansions tap "+r.id()+" "+token); }
 /** One watcher tick. Mutates state (due/proposed transitions + lapse notes); returns the
  *  proposals to post this tick (usually empty). Caller persists state afterwards. */
 public List<Proposal> tick(List<ManifestRow> rows, List<Watcher.Player> active, ReleaseState state, Instant now, Month month, boolean newYearTurn){
  Set<String> releasedSlugs=new LinkedHashSet<>();
  for(ManifestRow r: rows) if(ReleaseState.RELEASED.equals(state.stateOf(r))) releasedSlugs.add(r.slug());
  boolean headlineInFlight=rows.stream().anyMatch(r->!r.seasonal() && ReleaseState.PROPOSED.equals(state.stateOf(r)));
  List<Proposal> out=new ArrayList<>();
  for(ManifestRow r: rows){
   String eff=state.stateOf(r);
   ReleaseState.Entry cur=state.get(r.id()).orElse(null);
   if(ReleaseState.RELEASED.equals(eff)){
    if(r.seasonal() && !Watcher.seasonalInWindow(r,month,newYearTurn)){
     String tok=tokenFor(r.id(),now);
     state.put(new ReleaseState.Entry(r.id(),ReleaseState.RETIREMENT_PROPOSED, null, now,
       cur!=null?cur.releasedAt():null, cur!=null?cur.releasedBy():null, tok, "season window closed - retirement proposed (DESIGN 4.2)"));
     out.add(proposal(r,Kind.RETIREMENT,"season window for "+r.displayName()+" has closed",active,tok)); }
    continue; }
   if(ReleaseState.RETIREMENT_PROPOSED.equals(eff)) continue; // retirement card in flight - silent
   if(ReleaseState.PROPOSED.equals(eff)){
    // Untapped: stays due silently, no re-spam (DESIGN 3.3). A lapsed majority is noted
    // for the rundown only; the card in front of Mike is never withdrawn (DESIGN 2.3).
    Watcher.Result res=watcher.evaluate(r,rows,active,releasedSlugs,month,newYearTurn);
    if(res.verdict()==Watcher.Verdict.NOT_DUE && (cur==null||cur.note()==null||!cur.note().contains("majority lapsed"))){
     state.put(new ReleaseState.Entry(r.id(),ReleaseState.PROPOSED, cur!=null?cur.dueSince():null, cur!=null?cur.proposedAt():null, null, null,
       cur!=null?cur.token():null, "majority lapsed after proposal ("+res.reason()+") - card stands per DESIGN 2.3; rundown notes it")); }
    continue; }
   // unreleased / due / retired-seasonal: evaluate fresh
   Watcher.Result res=watcher.evaluate(r,rows,active,releasedSlugs,month,newYearTurn);
   boolean due=res.verdict()==Watcher.Verdict.DUE || res.verdict()==Watcher.Verdict.SEASONAL_DUE;
   if(!due){
    if(ReleaseState.DUE.equals(eff)) state.put(new ReleaseState.Entry(r.id(),ReleaseState.UNRELEASED,null,null,null,null,null,
      "was due (blocked by an in-flight proposal); verdict lapsed before proposal ("+res.reason()+") - back to unreleased (DESIGN 2.3)"));
    continue; }
   if(!r.seasonal() && headlineInFlight){
    if(!ReleaseState.DUE.equals(eff)) state.put(new ReleaseState.Entry(r.id(),ReleaseState.DUE,now,null,null,null,null,
      "DUE ("+res.reason()+") but a headline proposal is already in flight - recorded due, proposed when that tap lands (DESIGN 3.6)"));
    continue; }
   String tok=tokenFor(r.id(),now);
   Instant dueSince=ReleaseState.DUE.equals(eff)&&cur!=null&&cur.dueSince()!=null? cur.dueSince() : now;
   state.put(new ReleaseState.Entry(r.id(),ReleaseState.PROPOSED,dueSince,now,null,null,tok,"proposed: "+res.reason()));
   if(!r.seasonal()) headlineInFlight=true;
   out.add(proposal(r,Kind.RELEASE,res.reason(),active,tok)); }
  return out; }
 /** Rebuild the pending card for a row already in proposed / retirement-proposed state -
  *  used ONLY to deliver the same episode on Mike's join (delivery, not a new proposal:
  *  the token and proposed_at are the stored ones; no state changes). */
 public Optional<Proposal> pendingCard(ManifestRow r, ReleaseState state){
  ReleaseState.Entry e=state.get(r.id()).orElse(null); if(e==null||e.token()==null) return Optional.empty();
  if(ReleaseState.PROPOSED.equals(e.state())) return Optional.of(proposal(r,Kind.RELEASE,
    e.note()!=null&&e.note().startsWith("proposed: ")? e.note().substring(10) : "awaiting Mike tap", List.of(), e.token()));
  if(ReleaseState.RETIREMENT_PROPOSED.equals(e.state())) return Optional.of(proposal(r,Kind.RETIREMENT,
    "season window for "+r.displayName()+" has closed", List.of(), e.token()));
  return Optional.empty(); }
}
