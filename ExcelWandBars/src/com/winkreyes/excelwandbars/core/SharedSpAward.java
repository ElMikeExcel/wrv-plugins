package com.winkreyes.excelwandbars.core;

/** Existing shared-SP daily/carry arithmetic, extracted without changing thresholds or rounding. */
public final class SharedSpAward {
 private SharedSpAward(){}
 public record Result(double qualifiedSp,int currencyWhole,double carryAfter) {}
 public static Result calculate(int throttledBase,double multiplier,double earnedToday,double carry,
                                double fullPerDay,double maxPerDay,double slowRate){
  double got=EarnMath.daily(throttledBase*multiplier,earnedToday,fullPerDay,maxPerDay,slowRate);
  double total=carry+got;
  int whole=(int)Math.floor(total+1e-9);
  return new Result(got,whole,total-whole);
 }
}
