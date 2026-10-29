// Excel: preserved recurrence verification harness, synthetic files only.
import com.winkreyes.shops.checkout.GlobalSessionGuard;
import com.winkreyes.shops.checkout.DataFileLock;
import java.nio.file.*; import java.util.*; import java.io.*;
public class GuardChecks {
 static int checks;
 static Map<String,GlobalSessionGuard.Fingerprint> proofs(String h){return Map.of("checkout",new GlobalSessionGuard.Fingerprint(0,h,true,"c".repeat(64)),"legacy-checkout",new GlobalSessionGuard.Fingerprint(0,h,true,"c".repeat(64)));}
 static final Map<String,GlobalSessionGuard.Fingerprint>P=proofs("a".repeat(64));
 static void ok(boolean x){checks++;if(!x)throw new AssertionError("check"+checks);}
 interface Op{void run()throws Exception;}
 static void fails(Op x)throws Exception{boolean failed=false;try{x.run();}catch(IOException e){failed=true;}ok(failed);}
 static GlobalSessionGuard ready(Path p)throws Exception{var g=GlobalSessionGuard.open(p,true);ok(g.assess(P,false)==GlobalSessionGuard.Decision.CLEAN_START);g.recordReady(P);return g;}
 static int child(String mode,Path p)throws Exception{return new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-cp",System.getProperty("java.class.path"),"GuardChecks",mode,p.toString()).inheritIO().start().waitFor();}
 public static void main(String[]a)throws Exception{
  if(a.length>0){Path p=Path.of(a[1]);if(a[0].equals("lock")){try(var g=GlobalSessionGuard.open(p,true)){System.exit(1);}catch(IOException e){System.exit(0);}}var g=ready(p);if(a[0].equals("crash-fence"))g.beforeMutation("test intent");Runtime.getRuntime().halt(0);}
  Path root=Files.createTempDirectory("guard-checks-");
  Path p=root.resolve("noop");try(var g=ready(p)){}try(var g=GlobalSessionGuard.open(p,true)){ok(g.assess(P,true)==GlobalSessionGuard.Decision.NO_OP_RECOVERY_ELIGIBLE);fails(()->g.recordReady(P));g.recordReady(P,true);g.closeClean(true);}ok(!Files.exists(p.resolve("global-session-v2")));
  p=root.resolve("v1");try(var g=GlobalSessionGuard.open(p,true)){ok(g.assess(P,true)==GlobalSessionGuard.Decision.BLOCKED);fails(()->g.recordReady(P,true));}
  p=root.resolve("changed");try(var g=ready(p)){}try(var g=GlobalSessionGuard.open(p,true)){ok(g.assess(proofs("b".repeat(64)),true)==GlobalSessionGuard.Decision.BLOCKED);}
  p=root.resolve("mutated");try(var g=ready(p)){g.beforeMutation("test");g.beforeMutation("again");}try(var g=GlobalSessionGuard.open(p,true)){ok(g.assess(P,true)==GlobalSessionGuard.Decision.BLOCKED);}
  p=root.resolve("uncertain");try(var g=ready(p)){g.markUncertain("external error");fails(()->g.beforeMutation("no"));fails(()->g.closeClean(true));}try(var g=GlobalSessionGuard.open(p,true)){ok(g.assess(P,true)==GlobalSessionGuard.Decision.BLOCKED);}
  p=root.resolve("pre-ready");try(var g=GlobalSessionGuard.open(p,true)){fails(()->g.beforeMutation("startup"));ok(g.assess(P,false)==GlobalSessionGuard.Decision.BLOCKED);}
  p=root.resolve("bad");try(var g=ready(p)){}Files.writeString(p.resolve("global-session-v2"),"partial");try(var g=GlobalSessionGuard.open(p,true)){ok(g.assess(P,true)==GlobalSessionGuard.Decision.BLOCKED);}
  p=root.resolve("concurrent");try(var g=ready(p)){final Path q=p;fails(()->GlobalSessionGuard.open(q,true));ok(child("lock",p)==0);}
  p=root.resolve("diskfailure");try(var g=ready(p)){Files.createDirectory(p.resolve("global-session-v2.fence"));fails(()->g.beforeMutation("no write"));fails(()->g.closeClean(true));}
  p=root.resolve("crashready");ok(child("crash-ready",p)==0);try(var g=GlobalSessionGuard.open(p,true)){ok(g.assess(P,true)==GlobalSessionGuard.Decision.NO_OP_RECOVERY_ELIGIBLE);}
  p=root.resolve("crashfence");ok(child("crash-fence",p)==0);try(var g=GlobalSessionGuard.open(p,true)){ok(g.assess(P,true)==GlobalSessionGuard.Decision.BLOCKED);}
  p=root.resolve("relocated");Path old=root.resolve("old-location");try(var g=ready(old)){}Files.createDirectories(p);Files.copy(old.resolve("global-session-v2"),p.resolve("global-session-v2"));try(var g=GlobalSessionGuard.open(p,true)){ok(g.assess(P,true)==GlobalSessionGuard.Decision.BLOCKED);}
  p=root.resolve("clock");try(var g=ready(p)){}Files.setLastModifiedTime(p.resolve("global-session-v2"),java.nio.file.attribute.FileTime.fromMillis(0));try(var g=GlobalSessionGuard.open(p,true)){ok(g.assess(P,true)==GlobalSessionGuard.Decision.NO_OP_RECOVERY_ELIGIBLE);}
  p=root.resolve("longuncertainty");try(var g=ready(p)){g.markUncertain("x".repeat(10000));}try(var g=GlobalSessionGuard.open(p,true)){ok(g.assess(P,true)==GlobalSessionGuard.Decision.BLOCKED);}
  p=root.resolve("unknown-marker");var unknown=Map.of("checkout",new GlobalSessionGuard.Fingerprint(0,"a".repeat(64),true),"legacy-checkout",new GlobalSessionGuard.Fingerprint(0,"a".repeat(64),true));try(var g=GlobalSessionGuard.open(p,true)){ok(g.assess(unknown,false)==GlobalSessionGuard.Decision.CLEAN_START);g.recordReady(unknown);}try(var g=GlobalSessionGuard.open(p,true)){ok(g.assess(unknown,true)==GlobalSessionGuard.Decision.BLOCKED);}
  p=root.resolve("changed-marker");try(var g=ready(p)){}var changedMarker=new HashMap<>(P);changedMarker.put("legacy-checkout",new GlobalSessionGuard.Fingerprint(0,"a".repeat(64),true,"d".repeat(64)));try(var g=GlobalSessionGuard.open(p,true)){ok(g.assess(changedMarker,true)==GlobalSessionGuard.Decision.BLOCKED);}
  p=root.resolve("missing-marker");try(var g=ready(p)){}var missingMarker=new HashMap<>(P);missingMarker.put("checkout",new GlobalSessionGuard.Fingerprint(0,"a".repeat(64),true));try(var g=GlobalSessionGuard.open(p,true)){ok(g.assess(missingMarker,true)==GlobalSessionGuard.Decision.BLOCKED);}
  p=root.resolve("migration-scan");try(var g=ready(p)){g.beforeMutation("scan fixture");ok(!Files.exists(p.resolve("global-session-v2.lock")));ok(Files.exists(p.resolveSibling("."+p.getFileName()+"-global-session-v2.lock")));try(var paths=Files.walk(p)){for(Path file:paths.filter(Files::isRegularFile).toList()){try(var access=DataFileLock.openReadOnly(file)){ok(access.channel().size()>0);}}}ok(child("lock",p)==0);}
  p=root.resolve("distinct-plugin-root");try(var a1=ready(p);var a2=ready(root.resolve("another-plugin-root"))){ok(true);}
  p=root.resolve("audit-retained");try(var g=ready(p)){}byte[] first=Files.readAllBytes(p.resolve("global-session-v2"));try(var g=GlobalSessionGuard.open(p,true)){ok(g.assess(P,true)==GlobalSessionGuard.Decision.NO_OP_RECOVERY_ELIGIBLE);g.recordReady(P,true);}byte[] second=Files.readAllBytes(p.resolve("global-session-v2"));try(var g=GlobalSessionGuard.open(p,true)){ok(g.assess(P,true)==GlobalSessionGuard.Decision.NO_OP_RECOVERY_ELIGIBLE);g.recordReady(P,true);}try(var entries=Files.list(p.resolve("global-session-v2-audit"))){var receipts=entries.toList();ok(receipts.size()==2);int firstFound=0,secondFound=0;for(Path f:receipts){byte[] v=Files.readAllBytes(f);if(Arrays.equals(first,v))firstFound++;if(Arrays.equals(second,v))secondFound++;}ok(firstFound==1&&secondFound==1);}
  p=root.resolve("audit-failure");try(var g=ready(p)){}byte[] oldEvidence=Files.readAllBytes(p.resolve("global-session-v2"));Files.writeString(p.resolve("global-session-v2-audit"),"blocked destination");try(var g=GlobalSessionGuard.open(p,true)){ok(g.assess(P,true)==GlobalSessionGuard.Decision.NO_OP_RECOVERY_ELIGIBLE);fails(()->g.recordReady(P,true));fails(()->g.beforeMutation("must not execute"));}ok(Arrays.equals(oldEvidence,Files.readAllBytes(p.resolve("global-session-v2"))));
  p=root.resolve("invalidproof");try(var g=GlobalSessionGuard.open(p,true)){fails(()->g.assess(Map.of(),false));var bad=new HashMap<>(P);bad.put("checkout",new GlobalSessionGuard.Fingerprint(0,"a".repeat(64),false));fails(()->g.assess(bad,false));}
  p=root.resolve("disabled");try(var g=GlobalSessionGuard.open(p,false)){g.beforeMutation("old semantics");ok(g.assess(P,true)==GlobalSessionGuard.Decision.BLOCKED);}ok(!Files.exists(p));
  p=root.resolve("falseclean");try(var g=ready(p)){fails(()->g.closeClean(false));}ok(Files.exists(p.resolve("global-session-v2")));
  System.out.println("PASS "+checks+" checks; real subprocess crash and lock scenarios; no Bukkit integration asserted");
 }
}
