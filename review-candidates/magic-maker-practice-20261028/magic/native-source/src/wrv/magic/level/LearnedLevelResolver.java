package wrv.magic.level;

import java.util.*;
/** Read-only, opt-in decision boundary. Never grants a spell, writes a rank, or chooses an ownership policy. */
public final class LearnedLevelResolver {
 public record View(String ownerScope,String spellKey,int learnedLevel,int maxLevel,long usefulCount,Long nextCumulativeThreshold,int migrationFloor,String policyId){}
 public record Request(UUID player,String wandId,String spell,int storedNativeLevel){}
 public record Scope(Request subject,String ownerScope){}
 public record Binding(String policyId,int maxLevel){public Binding{if(policyId==null||policyId.isBlank()||maxLevel<1||maxLevel>64)throw new IllegalArgumentException("explicit policy");}}
 public record Availability(Request subject,String ownerScope,String policyId,String stageRevision,boolean possessesSpell,Set<Integer> availableLevels){public Availability{availableLevels=Set.copyOf(availableLevels);}}
 @FunctionalInterface public interface ScopeResolver {Scope resolve(Request request)throws Exception;}
 @FunctionalInterface public interface ViewProvider {View read(Scope scope)throws Exception;}
 @FunctionalInterface public interface AvailabilityProvider {Availability read(Scope scope)throws Exception;}
 public enum Status { UNMANAGED,DENIED,RESOLVED }
 public record Decision(Status status,int storedNativeLevel,Integer effectiveLearnedLevel,String reason){}
 private final Map<String,Binding> managed;private final ScopeResolver scopes;private final ViewProvider views;private final AvailabilityProvider availability;private final Thread owner=Thread.currentThread();
 public LearnedLevelResolver(Map<String,Binding> managed,ScopeResolver scopes,ViewProvider views,AvailabilityProvider availability){
  for(String k:managed.keySet())if(k==null||!k.matches("[a-z0-9_.-]+"))throw new IllegalArgumentException("canonical base keys");
  this.managed=Map.copyOf(managed);this.scopes=scopes;this.views=views;this.availability=availability;
 }
 public static LearnedLevelResolver disabled(){return new LearnedLevelResolver(Map.of(),null,null,null);}
 public Set<String> managedKeys(){return Set.copyOf(managed.keySet());}
 public boolean manages(String key){return key!=null&&managed.containsKey(key);}
 private static Decision deny(Request r,String reason){return new Decision(Status.DENIED,r==null?0:r.storedNativeLevel(),null,reason);}
 public Decision resolve(Request r){
  if(r==null)return deny(null,"missing request");
  if(r.spell()==null||!r.spell().matches("[a-z0-9_.-]+"))return deny(r,"canonical base key required");
  Binding binding=managed.get(r.spell());
  if(binding==null)return new Decision(Status.UNMANAGED,r.storedNativeLevel(),null,"native path unchanged");
  if(Thread.currentThread()!=owner)return deny(r,"wrong thread");
  if(r.player()==null||r.wandId()==null||r.wandId().isBlank()||r.storedNativeLevel()<1||scopes==null||views==null||availability==null)return deny(r,"missing authority");
  try{
   Scope scope=scopes.resolve(r);
   if(scope==null||!r.equals(scope.subject())||scope.ownerScope()==null||scope.ownerScope().isBlank())return deny(r,"unknown scope");
   View v=views.read(scope);
   if(v==null||!scope.ownerScope().equals(v.ownerScope())||!r.spell().equals(v.spellKey())||!binding.policyId().equals(v.policyId())||binding.maxLevel()!=v.maxLevel())return deny(r,"view identity/policy mismatch");
   if(v.learnedLevel()<1||v.learnedLevel()>v.maxLevel()||v.migrationFloor()<1||v.migrationFloor()>v.learnedLevel()||v.usefulCount()<0|| (v.learnedLevel()==v.maxLevel()?v.nextCumulativeThreshold()!=null:v.nextCumulativeThreshold()==null||v.nextCumulativeThreshold()<=v.usefulCount()))return deny(r,"invalid coherent view");
   // A higher saved level requires reviewed floor migration. Never silently downgrade it or invent a floor here.
   if(r.storedNativeLevel()>v.learnedLevel())return deny(r,"higher native rank needs reviewed migration");
   Availability a=availability.read(scope);
   if(a==null||!r.equals(a.subject())||!scope.ownerScope().equals(a.ownerScope())||!binding.policyId().equals(a.policyId())||a.stageRevision()==null||a.stageRevision().isBlank()||!a.possessesSpell())return deny(r,"spell or learned template unavailable at current seal");
   // Authenticated adapters must report current state, not cached/stale stage evidence.
   int effective=0;
   for(Integer level:a.availableLevels()){
    if(level==null||level<1||level>binding.maxLevel())return deny(r,"invalid current availability");
    if(level<=v.learnedLevel())effective=Math.max(effective,level);
   }
   if(effective<1)return deny(r,"no currently eligible native level");
   // Practice remains banked in its ledger. A later path/seal unlock exposes it without rewriting counts.
   return new Decision(Status.RESOLVED,r.storedNativeLevel(),effective,
       effective<v.learnedLevel()?"practice banked; higher rank requires current path/seal":"read-only learned rank");
  }catch(Exception unavailable){return deny(r,"authority unavailable");}
 }
}
