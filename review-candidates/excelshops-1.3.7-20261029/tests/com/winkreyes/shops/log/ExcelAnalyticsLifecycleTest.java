// Excel: synthetic analytics lifecycle and exclusive-ownership regression tests.
package com.winkreyes.shops.log;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class ExcelAnalyticsLifecycleTest {
 @TempDir Path dir;
 PurchaseLog log() { return new PurchaseLog(dir, ZoneOffset.UTC, Clock.systemUTC(), s -> {}); }
 PurchaseLog.Purchase row() {return new PurchaseLog.Purchase(Instant.now(),"Synthetic",new UUID(0,1),"java","spawn","stone",1,1,1,1,1L,"synthetic");}
 ExecutorService executor(PurchaseLog log) throws Exception {var f=PurchaseLog.class.getDeclaredField("writer");f.setAccessible(true);return (ExecutorService)f.get(log);}
 @Test void extraStatsExplicitlyBusyAndPurchaseOrderingUnchanged() throws Exception {
  try(var log=log()) {
   var gate=new CountDownLatch(1);var begun=new CountDownLatch(1);
   executor(log).execute(()->{begun.countDown();try{gate.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}});begun.await();
   try {
    var before=log.record(row());var stats=log.stats(7,10);var after=log.record(row());
    for(int i=0;i<100;i++)assertThrows(ExecutionException.class,()->log.stats(30,1).get());
    gate.countDown();assertEquals(1,stats.get(3,TimeUnit.SECONDS).purchases());before.get();after.get();
    assertEquals(2,log.stats(7,10).get().purchases());
   } finally {gate.countDown();}
  }
 }
 @Test void timeoutRetainsOwnerAndEventuallyDrainsWithoutDropping() throws Exception {
  var log=log();log.record(row()).get();
  var gate=new CountDownLatch(1);var begun=new CountDownLatch(1);
  executor(log).execute(()->{begun.countDown();try{gate.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}});begun.await();
  var pending=log.record(row());
  try {
   log.close();assertEquals(PurchaseLog.CloseResult.TIMED_OUT,log.closeResult());
   assertEquals(1,log.status().pending());assertFalse(pending.isDone());
   try(var contender=log()) {assertThrows(ExecutionException.class,()->contender.record(row()).get());}
   // Closing the failed contender must not release the original process's OS lock.
   String java=Path.of(System.getProperty("java.home"),"bin","java").toString();
   var process=new ProcessBuilder(java,"-cp",System.getProperty("java.class.path"),LockProbe.class.getName(),dir.resolve("purchases.csv.analytics.lock").toString()).inheritIO().start();
   assertEquals(0,process.waitFor());
  } finally {gate.countDown();pending.get(3,TimeUnit.SECONDS);log.close();}
  assertEquals(PurchaseLog.CloseResult.TERMINATED,log.closeResult());
  try(var next=log()){next.record(row()).get();assertEquals(3,next.stats(7,10).get().purchases());}
 }
 public static class LockProbe { public static void main(String[] args)throws Exception {try(var c=java.nio.channels.FileChannel.open(Path.of(args[0]),StandardOpenOption.WRITE)){var lock=c.tryLock();if(lock!=null){lock.release();System.exit(4);}}} }
 @Test void interruptedCloseRetainsOwnershipAndInterruptFlag() throws Exception {
  var log=log();log.record(row()).get();var gate=new CountDownLatch(1);
  executor(log).execute(()->{try{gate.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}});
  try{Thread.currentThread().interrupt();log.close();assertTrue(Thread.interrupted());assertEquals(PurchaseLog.CloseResult.INTERRUPTED,log.closeResult());}
  finally{Thread.interrupted();gate.countDown();log.close();}
 }
 @Test void warningFailureCannotEscapeAndFuturesTruthful() throws Exception {
  var log=new PurchaseLog(dir,ZoneOffset.UTC,Clock.systemUTC(),s->{throw new IllegalStateException("synthetic warning failure");});
  Files.createDirectory(dir.resolve("purchases.csv"));
  assertThrows(ExecutionException.class,()->log.record(row()).get());assertEquals(1,log.status().failed());
  log.close();assertThrows(ExecutionException.class,()->log.record(row()).get());assertEquals(1,log.status().rejected());
 }
 @Test void cancellationDoesNotSilentlyDiscardAcceptedRow() throws Exception {
  var log=log();var gate=new CountDownLatch(1);
  executor(log).execute(()->{try{gate.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}});
  var result=log.record(row());result.cancel(false);gate.countDown();log.close();
  assertEquals(1,log.status().completed());assertEquals(2,Files.readAllLines(dir.resolve("purchases.csv")).size());
 }
 @Test void separateClassloaderCannotWriteWhileOwnerAlive() throws Exception {
  try(var first=log()) {
   first.record(row()).get();
   var url=PurchaseLog.class.getProtectionDomain().getCodeSource().getLocation();
   try(var loader=new java.net.URLClassLoader(new java.net.URL[]{url},ClassLoader.getPlatformClassLoader())){
    var type=loader.loadClass(PurchaseLog.class.getName());var other=type.getConstructor(Path.class,ZoneId.class,Clock.class,java.util.function.Consumer.class).newInstance(dir,ZoneOffset.UTC,Clock.systemUTC(),(java.util.function.Consumer<String>)(s->{}));
    try{var future=(CompletableFuture<?>)type.getMethod("stats",int.class,int.class).invoke(other,7,10);assertThrows(ExecutionException.class,()->future.get());}
    finally{type.getMethod("close").invoke(other);}
   }
   first.record(row()).get();assertEquals(2,first.stats(7,10).get().purchases());
  }
 }
}
