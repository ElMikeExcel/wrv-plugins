package com.winkreyes.excelexpansions.core;
import java.nio.file.Path; import java.time.*; import java.time.format.DateTimeFormatter; import java.util.*;
/** Tap handling for ExcelExpansions 0.1.1 - DESIGN 3.2 + 5 + 7.12. The host authenticates
 *  the sender as Mike (config proposal.mike-uuids, or op/console per config) BEFORE calling;
 *  this service enforces the episode rules:
 *  - a tap is honoured only against a live proposal (state proposed / retirement-proposed);
 *    a tap with no proposal in flight is rejected, so a stale card can never release twice;
 *  - a presented token must match the proposal's token. Console may tap without a token
 *    (the console is already Mike's trusted surface); a WRONG token is rejected from anyone -
 *    that is the stale-card guard. A tap on a proposed row whose majority has since lapsed is
 *    still honoured (DESIGN 2.3: the card in front of Mike is not withdrawn);
 *  - double-tap: a row already released / retired returns ALREADY_* and flips nothing;
 *  - the flip runs through Releaser; only a fully successful flip advances the state to
 *    released / retired-seasonal (persisted by the caller). On FLIP_FAILED or NO_CONTENT the
 *    state is untouched, so the row stays proposed and the failure can be reported + retried.
 *  Surfaces after a release (Guide, What's New both, website, menus - DESIGN 5 step 4) are
 *  not plugin-writable; the released entry's note names them as owed so the supervisor's
 *  same-pass surfaces leg picks them up. World setup, where a row's standby state still owes
 *  it (FINAL manifest), is likewise named in the note - the flip is the enable leg. */
public final class ReleaseService {
 public enum Outcome{ RELEASED, RETIRED, ALREADY_RELEASED, ALREADY_RETIRED, REJECTED_TOKEN, REJECTED_NOT_PROPOSED, NO_CONTENT, FLIP_FAILED }
 public record TapResult(Outcome outcome, Releaser.Result flip, String message){}
 private final Releaser releaser=new Releaser();
 public Releaser releaser(){ return releaser; }
 public TapResult tap(ManifestRow row, ReleaseState state, String presentedToken, boolean console, String senderLabel, Path pluginsDir, Instant now){
  String eff=state.stateOf(row);
  if(ReleaseState.RELEASED.equals(eff)) return new TapResult(Outcome.ALREADY_RELEASED,null,row.id()+" "+row.displayName()+" is already released - double-tap ignored, nothing re-flipped");
  if(ReleaseState.RETIRED.equals(eff)) return new TapResult(Outcome.ALREADY_RETIRED,null,row.id()+" "+row.displayName()+" is already retired for the season - nothing re-flipped");
  boolean retirement=ReleaseState.RETIREMENT_PROPOSED.equals(eff);
  if(!ReleaseState.PROPOSED.equals(eff) && !retirement)
   return new TapResult(Outcome.REJECTED_NOT_PROPOSED,null,"no live proposal for "+row.id()+" (state: "+eff+") - tap rejected; only a posted proposal can be tapped");
  ReleaseState.Entry cur=state.get(row.id()).orElse(null);
  String want=cur!=null? cur.token() : null;
  if(presentedToken!=null && !presentedToken.isBlank()){
   if(want==null || !want.equals(presentedToken.trim()))
    return new TapResult(Outcome.REJECTED_TOKEN,null,"tap token does not match the live proposal for "+row.id()+" - stale card rejected, nothing flipped");
  } else if(!console){
   return new TapResult(Outcome.REJECTED_TOKEN,null,"tap for "+row.id()+" presented no token - use the card or the typed command it shows"); }
  if(row.contentFiles().isEmpty())
   return new TapResult(Outcome.NO_CONTENT,null,row.id()+" "+row.displayName()+" has no content_package files on record - nothing to flip; row stays proposed");
  Releaser.Result flip=releaser.flip(pluginsDir,row.contentFiles(),!retirement);
  if(!flip.success())
   return new TapResult(Outcome.FLIP_FAILED,flip,"flip failed for "+row.id()+" - row stays proposed; per-file results follow");
  String date=DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.of("America/Chicago")).format(now);
  String by="Mike tap "+date+" ("+senderLabel+")";
  if(retirement){
   state.put(new ReleaseState.Entry(row.id(),ReleaseState.RETIRED, null, cur!=null?cur.proposedAt():null,
     cur!=null?cur.releasedAt():null, cur!=null?cur.releasedBy():null, null,
     "retired by "+by+"; surfaces owed (supervisor, same pass): Guide + What's New (both) note the seasonal close; menus checked"));
   return new TapResult(Outcome.RETIRED,flip,row.displayName()+" retired for the season by "+by); }
  state.put(new ReleaseState.Entry(row.id(),ReleaseState.RELEASED, cur!=null?cur.dueSince():null, cur!=null?cur.proposedAt():null,
    now, by, null,
    "released by tap; surfaces owed (supervisor, same pass): Guide + What's New (both surfaces) + website where the surface exists + menus checked; world setup owed where the row's standby state names it (FINAL manifest)"));
  return new TapResult(Outcome.RELEASED,flip,row.displayName()+" released by "+by); }
}
