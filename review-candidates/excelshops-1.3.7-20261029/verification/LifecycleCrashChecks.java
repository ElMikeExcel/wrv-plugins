// Excel: preserved recurrence verification harness, synthetic files only.
import com.winkreyes.shops.checkout.*;
import java.nio.file.*;import java.util.*;import java.time.*;

/** Executes exact candidate FileJournal + GlobalSessionGuard in killed JVMs, no Bukkit server. */
public class LifecycleCrashChecks {
 static int checks;
 static Map<String,GlobalSessionGuard.Fingerprint> proofs(FileJournal a,FileJournal b)throws Exception{return Map.of("checkout",a.proof(),"legacy-checkout",b.proof());}
 static void crash(){Runtime.getRuntime().halt(73);}
 static void child(Path root,String phase)throws Exception{
  var g=GlobalSessionGuard.open(root,true);if(phase.equals("after-global-lock"))crash();
  var a=new FileJournal(root.resolve("checkout"));if(phase.equals("after-native-marker"))crash();
  var b=new FileJournal(root.resolve("legacy-checkout"));if(phase.equals("after-legacy-marker"))crash();
  a.attachGuard(g);b.attachGuard(g);var p=proofs(a,b);g.assess(p,a.markerPresentOnOpen()||b.markerPresentOnOpen());g.recordReady(p);if(phase.equals("after-ready"))crash();
  if(phase.equals("external-uncertainty")){a.markUncertain("external ambiguous result");crash();}
  if(phase.equals("after-fence")){g.beforeMutation("registry-edit");crash();}
  if(phase.startsWith("native-")||phase.equals("legacy-intent")){
   var q=new CheckoutService.Quote(UUID.randomUUID(),UUID.randomUUID(),null,"spawn:test",0,1,100,"fingerprint",Instant.now().plusSeconds(30));
   if(phase.equals("legacy-intent")){b.record(q,CheckoutService.Stage.LEGACY_INTENT,"before external effect");crash();}
   var stages=new CheckoutService.Stage[]{CheckoutService.Stage.WITHDRAW_INTENT,CheckoutService.Stage.WITHDRAWN,CheckoutService.Stage.TRANSFER_INTENT,CheckoutService.Stage.TRANSFER_APPLIED,CheckoutService.Stage.COMMITTED};
   for(var s:stages){a.record(q,s,"test evidence");if(phase.equals("native-"+s))crash();}
  }
  a.close();if(phase.equals("after-native-close"))crash();b.close();if(phase.equals("after-both-close"))crash();g.closeClean(true);crash();
 }
 static GlobalSessionGuard.Decision reopen(Path root,boolean recover)throws Exception{
  try(var g=GlobalSessionGuard.open(root,true);var a=new FileJournal(root.resolve("checkout"));var b=new FileJournal(root.resolve("legacy-checkout"))){
   var p=proofs(a,b);GlobalSessionGuard.Decision d;
   try{d=g.assess(p,a.markerPresentOnOpen()||b.markerPresentOnOpen());}catch(java.io.IOException bad){return GlobalSessionGuard.Decision.BLOCKED;}
   if(recover&&d==GlobalSessionGuard.Decision.NO_OP_RECOVERY_ELIGIBLE){g.recordReady(p,true);if(a.markerOnlyRecovery())a.acceptNoActivityRecovery();if(b.markerOnlyRecovery())b.acceptNoActivityRecovery();if(a.requiresRecovery()||b.requiresRecovery())throw new AssertionError("partial unlock");a.close();b.close();g.closeClean(true);}
   return d;
  }
 }
 public static void main(String[]args)throws Exception{
  if(args.length==2){child(Path.of(args[0]),args[1]);return;}
  Path root=Files.createTempDirectory("shops-real-lifecycle");
  var expect=new LinkedHashMap<String,GlobalSessionGuard.Decision>();
  expect.put("after-global-lock",GlobalSessionGuard.Decision.CLEAN_START);
  for(String s:List.of("after-native-marker","after-legacy-marker","after-native-close","after-both-close","external-uncertainty","after-fence","native-WITHDRAW_INTENT","native-WITHDRAWN","native-TRANSFER_INTENT","native-TRANSFER_APPLIED","native-COMMITTED","legacy-intent"))expect.put(s,GlobalSessionGuard.Decision.BLOCKED);
  for(String s:List.of("after-ready"))expect.put(s,GlobalSessionGuard.Decision.NO_OP_RECOVERY_ELIGIBLE);
  expect.put("after-clean",GlobalSessionGuard.Decision.CLEAN_START);
  for(var e:expect.entrySet()){
   Path dir=Files.createDirectory(root.resolve(e.getKey()));var process=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-cp",System.getProperty("java.class.path"),LifecycleCrashChecks.class.getName(),dir.toString(),e.getKey()).inheritIO().start();if(process.waitFor()!=73)throw new AssertionError("child failed "+e.getKey());checks++;
   var d=reopen(dir,true);if(d!=e.getValue())throw new AssertionError(e.getKey()+" got "+d);checks++;
   if(d==GlobalSessionGuard.Decision.NO_OP_RECOVERY_ELIGIBLE){if(Files.exists(dir.resolve("global-session-v2"))||Files.exists(dir.resolve("checkout/active-session"))||Files.exists(dir.resolve("legacy-checkout/active-session")))throw new AssertionError("not cleanly recovered");checks++;}
  }
  System.out.println(checks+" actual two-journal/coordinator subprocess-crash checks PASS; no Bukkit integration claimed.");
 }
}
