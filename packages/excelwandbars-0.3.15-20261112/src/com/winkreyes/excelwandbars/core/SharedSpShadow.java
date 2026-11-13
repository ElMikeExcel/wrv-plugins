package com.winkreyes.excelwandbars.core;
import java.time.*;import java.util.*;

/** Observation only. No currency API, scheduler, file mutation or live-award activation switch exists here.
 * Caller must capture original rank-wand identity/earns and recipient eligibility authoritatively.
 * Receipt delivery time/current held wand must never substitute for missing cast-origin evidence.
 */
public final class SharedSpShadow {
 public enum Source { LEGACY_CAST, VERIFIED_EFFECT }
 public enum Status { WOULD_AWARD, DUPLICATE, WRONG_SOURCE, UNKNOWN_CAST, INELIGIBLE, INVALID, CLOCK_OR_DAY_CHANGED }
 public record Origin(UUID cast,UUID caster,String spell,String sourceWandIdentity,boolean rankWand,
  int earns,long castAtMs,Long lastEarnAtMs,long cooldownMs,double bonus,double earnedToday,double carry,
  double fullPerDay,double maxPerDay,double slowRate,LocalDate accountingDay) {
  public Origin {Objects.requireNonNull(cast);Objects.requireNonNull(caster);Objects.requireNonNull(spell);Objects.requireNonNull(sourceWandIdentity);Objects.requireNonNull(accountingDay);}
 }
 public record Evidence(UUID cast,UUID caster,String spell,UUID recipient,double effectiveAmount) {}
 public record Observation(Status status,Source source,UUID cast,SharedSpAward.Result calculation,String limitation) {}
 private static final ZoneId CHICAGO=ZoneId.of("America/Chicago");
 private final Set<String> migrated;private final int capacity;
 private final Map<UUID,Origin> origins=new HashMap<>();private final Set<UUID> used=new HashSet<>();
 public SharedSpShadow(Set<String> migratedSpells,int capacity){
  if(capacity<=0)throw new IllegalArgumentException("capacity");this.capacity=capacity;
  Set<String> normalized=new HashSet<>();for(String spell:migratedSpells){if(spell==null||spell.isBlank()||!spell.equals(spell.toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("canonical spell required");normalized.add(spell);}migrated=Set.copyOf(normalized);
 }
 public synchronized boolean capture(Origin origin){
  if(!origin.rankWand||origin.sourceWandIdentity.isBlank()||origin.earns<=0||origin.spell.isBlank()||origin.cooldownMs<0||!origin.spell.equals(origin.spell.toLowerCase(Locale.ROOT)))return false;
  if(!Double.isFinite(origin.bonus)||!Double.isFinite(origin.earnedToday)||!Double.isFinite(origin.carry)||origin.bonus<0||origin.earnedToday<0||origin.carry<0||origin.carry>=1)return false;
  if(!Double.isFinite(origin.fullPerDay)||!Double.isFinite(origin.maxPerDay)||!Double.isFinite(origin.slowRate))return false;
  if(used.contains(origin.cast))return false;
  if(origins.containsKey(origin.cast))return origins.get(origin.cast).equals(origin);
  if(origins.size()>=capacity)return false; // Never evict dedup evidence and silently re-admit a cast.
  origins.put(origin.cast,origin);return true;
 }
 public synchronized Observation legacy(UUID cast,boolean legacyQualified,long eventAt){return evaluate(cast,null,null,null,legacyQualified,Source.LEGACY_CAST,eventAt);}
 public synchronized Observation verified(Evidence effect,boolean recipientEligible,long eventAt){
  if(effect==null||effect.recipient==null||!Double.isFinite(effect.effectiveAmount)||effect.effectiveAmount<=0)
   return new Observation(Status.INVALID,Source.VERIFIED_EFFECT,effect==null?null:effect.cast,null,"No positive verified effect");
  return evaluate(effect.cast,effect.caster,effect.spell,effect.recipient,recipientEligible,Source.VERIFIED_EFFECT,eventAt);
 }
 private Observation evaluate(UUID cast,UUID caster,String spell,UUID recipient,boolean eligible,Source source,long eventAt){
  Origin o=origins.get(cast);if(o==null)return result(Status.UNKNOWN_CAST,source,cast,null);
  if(source==Source.VERIFIED_EFFECT&&(!o.caster.equals(caster)||!o.spell.equals(spell)))return result(Status.INVALID,source,cast,null);
  if(migrated.contains(o.spell)!=(source==Source.VERIFIED_EFFECT))return result(Status.WRONG_SOURCE,source,cast,null);
  if(!eligible)return result(Status.INELIGIBLE,source,cast,null);
  if(used.contains(cast))return result(Status.DUPLICATE,source,cast,null);
  LocalDate day=Instant.ofEpochMilli(eventAt).atZone(CHICAGO).toLocalDate();
  if(eventAt<o.castAtMs||!day.equals(o.accountingDay))return result(Status.CLOCK_OR_DAY_CHANGED,source,cast,null);
  int base=EarnMath.throttled(o.earns,o.lastEarnAtMs==null?Long.MAX_VALUE:o.castAtMs-o.lastEarnAtMs,o.cooldownMs);
  var calc=SharedSpAward.calculate(base,o.bonus,o.earnedToday,o.carry,o.fullPerDay,o.maxPerDay,o.slowRate);
  used.add(cast);return result(Status.WOULD_AWARD,source,cast,calc);
 }
 private Observation result(Status status,Source source,UUID cast,SharedSpAward.Result calc){return new Observation(status,source,cast,calc,"SHADOW ONLY: immutable snapshot estimate, not settlement or a payout promise");}
}
