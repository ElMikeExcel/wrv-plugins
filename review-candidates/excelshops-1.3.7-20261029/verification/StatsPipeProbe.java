// Excel signature - WRV maintained audit fixture; original authorship and license notices preserved.
import com.winkreyes.shops.log.PurchaseLog;
import java.nio.file.*;import java.nio.charset.*;import java.time.*;import java.lang.reflect.*;import java.util.concurrent.*;import java.util.*;
/** Synthetic FIFO is only a deterministic blocked-read seam, not a production file assumption. */
public final class StatsPipeProbe {
 public static void main(String[]args)throws Exception{
  Path p=Files.createTempDirectory("shops-stats-pipe-synthetic-");Path csv=p.resolve(PurchaseLog.FILE);
  if(new ProcessBuilder("mkfifo",csv.toString()).start().waitFor()!=0)throw new AssertionError("mkfifo");
  var warn=new CopyOnWriteArrayList<String>();var log=new PurchaseLog(p,ZoneOffset.UTC,Clock.systemUTC(),warn::add);
  Field f=PurchaseLog.class.getDeclaredField("writer");f.setAccessible(true);ExecutorService e=(ExecutorService)f.get(log);
  var stats=log.stats(1,10);var behind=e.submit(()->{});
  long start=System.nanoTime();log.close();System.out.println("close_ms="+(System.nanoTime()-start)/1_000_000.0);
  if(stats.isDone()||behind.isDone()||e.isTerminated())throw new AssertionError("expected stats worker blocked");
  if(log.closeResult()!=PurchaseLog.CloseResult.TIMED_OUT)throw new AssertionError("timeout state");
  if(warn.stream().anyMatch(x->x.contains("remaining lines dropped")))throw new AssertionError("false drop claim");
  try(var contender=new PurchaseLog(p,ZoneOffset.UTC,Clock.systemUTC(),m->{})){
   try{contender.stats(1,10).get(2,TimeUnit.SECONDS);throw new AssertionError("ownership overlap");}catch(ExecutionException expected){}
  }
  System.out.println("PASS real blocked stats retains exclusive ownership and truthful timeout");
  Files.writeString(csv,PurchaseLog.HEADER+"\n",StandardCharsets.UTF_8,StandardOpenOption.WRITE);
  if(stats.get(3,TimeUnit.SECONDS).purchases()!=0)throw new AssertionError("stats");behind.get(3,TimeUnit.SECONDS);
  if(!e.awaitTermination(3,TimeUnit.SECONDS))throw new AssertionError("termination");
  System.out.println("PASS release synthetic FIFO lets stats and queued task complete after close");
  System.out.println("warnings="+warn);
 }
}
