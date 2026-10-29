package com.winkreyes.makersmark.history;
import java.io.IOException;import java.util.function.LongSupplier;
/** Alternative policy: durable acknowledgement before acceptance. A slow/failed call cannot be timed away. */
public final class StrictIngress {
 public enum Status { ACCEPTED, ACCEPTED_AND_PAUSED, REFUSED_PAUSED, UNCERTAIN_AND_PAUSED }
 public record Result(Status status,long elapsedNanos,String reason){public boolean accepted(){return status==Status.ACCEPTED||status==Status.ACCEPTED_AND_PAUSED;}}
 private final HistoryLedger ledger;private final long pauseAfterNanos;private final LongSupplier clock;private boolean paused;
 public StrictIngress(HistoryLedger ledger,long pauseAfterNanos){this(ledger,pauseAfterNanos,System::nanoTime);}
 public StrictIngress(HistoryLedger ledger,long pauseAfterNanos,LongSupplier clock){if(pauseAfterNanos<1)throw new IllegalArgumentException("Positive pause threshold required");this.ledger=ledger;this.pauseAfterNanos=pauseAfterNanos;this.clock=clock;}
 public synchronized Result accept(HistoryLedger.Award award){
  if(paused)return new Result(Status.REFUSED_PAUSED,0,"Strict history ingress is paused for administrator review");
  long start=clock.getAsLong();try{
   ledger.acceptAward(award);long elapsed=Math.max(0,clock.getAsLong()-start);
   if(elapsed>pauseAfterNanos){paused=true;return new Result(Status.ACCEPTED_AND_PAUSED,elapsed,"Current award is durable; later admissions paused after slow storage");}
   return new Result(Status.ACCEPTED,elapsed,"Award durably acknowledged");
  }catch(IOException|RuntimeException failure){paused=true;return new Result(Status.UNCERTAIN_AND_PAUSED,Math.max(0,clock.getAsLong()-start),"Storage outcome uncertain; inspect/reopen archive before retry: "+failure.getMessage());}
 }
 public synchronized boolean paused(){return paused;}
}
