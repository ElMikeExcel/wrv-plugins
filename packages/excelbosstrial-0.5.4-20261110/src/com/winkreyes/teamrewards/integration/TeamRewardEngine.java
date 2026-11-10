/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.teamrewards.integration;

import com.winkreyes.teamrewards.encounter.TeamAdmission;
import wrv.rewards.TeamRewardLedger;
import java.io.IOException;
import java.util.*;

/** Integration facade: owning boss/raid plugin supplies authoritative encounter and qualified verdict.
 * Personal gear replaces, never supplements, its matching direct payout. Shared materials stay external.
 */
public final class TeamRewardEngine {
 public record PersonalRecipe(String recipeId,String opaqueItemPayload) {
  public PersonalRecipe { if(recipeId==null||!recipeId.matches("[A-Za-z0-9._:-]{1,64}"))throw new IllegalArgumentException("recipe ID"); Objects.requireNonNull(opaqueItemPayload); }
 }
 private final TeamRewardLedger ledger;
 private final TeamAdmission admission;
 public TeamRewardEngine(TeamRewardLedger ledger){this.ledger=Objects.requireNonNull(ledger);this.admission=new TeamAdmission(ledger::qualify);}
 /** Import original signature-paid IDs before enabling replacement payout; no retroactive claims. */
 public void seedAlreadyPaid(Set<String> originalPaidSeals)throws IOException{
  for(String seal:originalPaidSeals)ledger.importConsumedScope(scope(seal));
 }
 public int observe(TeamAdmission.Encounter encounter,Collection<TeamAdmission.PlayerView> players)throws IOException{
  if(encounter.kind()==TeamAdmission.Kind.RAID)throw new IllegalArgumentException("raid rewards out of scope");
  if(!encounter.active())return 0;
  ledger.openEncounter(encounter.runId());
  return admission.observe(encounter,players);
 }
 /** No live-health or damage-share recomputation: only the owning plugin can establish qualifying victory. */
 public void awardPersonalSignature(String runId,String sealId,String immutablePlanId,boolean ownerVerdictCredit,
                                    boolean ownerSignatureEligible,List<PersonalRecipe> recipes)throws IOException{
  if(!ownerVerdictCredit||!ownerSignatureEligible)throw new IllegalStateException("owning plugin has not authorized signature payout");
  Objects.requireNonNull(recipes);if(recipes.isEmpty())throw new IllegalArgumentException("no personal signature recipe");
  Set<String> names=new HashSet<>();for(var recipe:recipes)if(!names.add(recipe.recipeId()))throw new IllegalArgumentException("duplicate recipe");
  Map<UUID,List<TeamRewardLedger.RewardUnit>> distribution=new TreeMap<>();
  for(UUID player:ledger.roster(runId)){
   List<TeamRewardLedger.RewardUnit> units=new ArrayList<>();
   for(var recipe:recipes) units.add(new TeamRewardLedger.RewardUnit("personal:"+player+":"+recipe.recipeId(),recipe.opaqueItemPayload()));
   distribution.put(player,List.copyOf(units));
  }
  ledger.winOnceScope(runId,scope(sealId),immutablePlanId,distribution);
 }
 public void finishWithoutPersonalRecipe(String runId,String planId,boolean ownerVerdictCredit)throws IOException{
  if(!ownerVerdictCredit)throw new IllegalStateException("not a qualifying win");
  Map<UUID,List<TeamRewardLedger.RewardUnit>> empty=new TreeMap<>();
  for(UUID player:ledger.roster(runId))empty.put(player,List.of());
  ledger.win(runId,planId,empty); // Participation recorded, no invented raid items or duplicated shared material.
 }
 private static String scope(String seal){
  if(!Set.of("wither","elder_guardian","warden","ender_dragon").contains(seal))throw new IllegalArgumentException("unknown seal");
  return "signature:"+seal;
 }
}
