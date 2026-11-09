/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.recovery;
import com.winkreyes.excelbosstrial.ExcelBossTrial;
import com.winkreyes.excelbosstrial.core.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

/** Real API methods and real temp files; mocked server/thread and no plugin lifecycle/fights. */
public final class RecoveryApiTest {
 static int n;static boolean main=true;static String DIGEST="a".repeat(64);
 static void c(boolean b,String s){if(!b)throw new AssertionError(s);n++;System.out.println("PASS "+s);}
 static void set(Object o,Class<?> type,String name,Object value)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
 static Object get(Object o,String name)throws Exception{Field f=ExcelBossTrial.class.getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 static void call(Object o,String name)throws Exception{Method m=ExcelBossTrial.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(o);}
 static ExcelBossTrial plugin(Path dir)throws Exception{
  Class<?> u=Class.forName("sun.misc.Unsafe");Field uf=u.getDeclaredField("theUnsafe");uf.setAccessible(true);Object unsafe=uf.get(null);
  ExcelBossTrial p=(ExcelBossTrial)u.getMethod("allocateInstance",Class.class).invoke(unsafe,ExcelBossTrial.class);
  set(p,JavaPlugin.class,"isEnabled",true);set(p,JavaPlugin.class,"newConfig",new YamlConfiguration());set(p,JavaPlugin.class,"dataFolder",dir.toFile());set(p,JavaPlugin.class,"logger",Logger.getLogger("RecoveryApiTest"));
  set(p,ExcelBossTrial.class,"cfg",BossTrialConfig.defaults());
  set(p,ExcelBossTrial.class,"encounters",new HashMap<UUID,EncounterLedger>());
  set(p,ExcelBossTrial.class,"cooldowns",new CooldownBook());set(p,ExcelBossTrial.class,"monuments",new MonumentTracker());
  set(p,ExcelBossTrial.class,"signaturePaid",new HashSet<String>());set(p,ExcelBossTrial.class,"recoveryReceipts",new LinkedHashMap<String,Map<String,String>>());
  set(p,ExcelBossTrial.class,"recoveryGeneration",UUID.randomUUID().toString());
  set(p,ExcelBossTrial.class,"recoveryDiskToken","MISSING");
  set(p,ExcelBossTrial.class,"stateWriteProbe",(AtomicStateFile.Probe)phase->{});
  return p;
 }
 static String apply(ExcelBossTrial p,String id,String seal,Map<String,Object> expected){return String.valueOf(p.recoveryApply(id,DIGEST,seal,expected).get("status"));}
 public static void main(String[]args)throws Exception{
  Server server=(Server)Proxy.newProxyInstance(Server.class.getClassLoader(),new Class[]{Server.class},(p,m,a)->{
   if(m.getName().equals("isPrimaryThread"))return main;
   if(m.getName().equals("getLogger"))return Logger.getLogger("mock");
   if(m.getReturnType()==String.class)return "offline";if(m.getReturnType()==boolean.class)return false;if(m.getReturnType()==int.class)return 0;return null;
  });
  Field sf=Bukkit.class.getDeclaredField("server");sf.setAccessible(true);sf.set(null,server);
  Path dir=Files.createTempDirectory("bosstrial-recovery-api-");ExcelBossTrial p=plugin(dir);
  CooldownBook book=(CooldownBook)get(p,"cooldowns");book.start("wither",100);book.start("warden",200);
  var snap=p.recoverySnapshot("wither");c(snap.get("protocol").equals(1)&&Boolean.TRUE.equals(snap.get("enabled")),"snapshot protocol/enabled");
  String id=UUID.randomUUID().toString();c(apply(p,id,"wither",snap).equals("APPLIED"),"real API apply");
  c(book.untilMs("wither")==0&&book.untilMs("warden")>0,"only selected cooldown cleared");
  String disk=Files.readString(dir.resolve("state.yml"));c(disk.contains(id)&&disk.contains("warden"),"receipt and unrelated deadline committed together");
  c(apply(p,id,"wither",snap).equals("REPLAYED"),"same receipt replay");
  @SuppressWarnings("unchecked") Map<UUID,EncounterLedger> replayEncounters=(Map<UUID,EncounterLedger>)get(p,"encounters");
  UUID replayBoss=UUID.randomUUID();replayEncounters.put(replayBoss,new EncounterLedger(BossKind.WITHER,replayBoss,1500,1));
  c(apply(p,id,"wither",snap).equals("ACTIVE"),"receipt replay blocks coordinator completion during a newer selected fight");
  replayEncounters.clear();replayEncounters.put(replayBoss,new EncounterLedger(BossKind.WARDEN,replayBoss,1300,1));
  c(apply(p,id,"wither",snap).equals("REPLAYED"),"unrelated encounter does not block receipt replay");replayEncounters.clear();
  set(p,JavaPlugin.class,"isEnabled",false);c(apply(p,id,"wither",snap).equals("BLOCKED"),"disabled plugin blocks receipt replay completion");set(p,JavaPlugin.class,"isEnabled",true);
  call(p,"saveState");c(Files.readString(dir.resolve("state.yml")).contains(id),"normal save preserves receipt");
  ExcelBossTrial restored=plugin(dir);call(restored,"loadState");c(apply(restored,id,"wither",snap).equals("REPLAYED"),"restart receipt replay across generation");
  ((CooldownBook)get(restored,"cooldowns")).start("wither",999);c(apply(restored,id,"wither",snap).equals("DIVERGED"),"replay does not clear newer cooldown");
  c(apply(restored,UUID.randomUUID().toString(),"wither",snap).equals("STALE"),"old generation preview refused");
  c(apply(p,"not-uuid","wither",snap).equals("INVALID"),"invalid operation refused");
  c(String.valueOf(p.recoveryApply(id,"b".repeat(64),"wither",snap).get("status")).equals("INVALID"),"conflicting digest refused");
  c(apply(p,UUID.randomUUID().toString(),"raid",snap).equals("UNAVAILABLE"),"raid not owned here");
  set(p,ExcelBossTrial.class,"cfg",BossTrialConfig.fromMap(Map.of("wither.enabled",false)));
  c(apply(p,id,"wither",snap).equals("BLOCKED"),"disabled selected chapter blocks receipt replay");set(p,ExcelBossTrial.class,"cfg",BossTrialConfig.defaults());
  var ds=p.recoverySnapshot("ender_dragon");c(Boolean.TRUE.equals(ds.get("enabled"))&&apply(p,UUID.randomUUID().toString(),"ender_dragon",ds).equals("APPLIED"),"dragon bridge supported without altering battle phase");
  main=false;c(apply(p,id,"wither",snap).equals("BLOCKED"),"off-main API refused");main=true;
  var ep=p.recoverySnapshot("elder_guardian");MonumentTracker mt=(MonumentTracker)get(p,"monuments");UUID elder=UUID.randomUUID();mt.register(elder,"world",0,40,0);mt.markPendingRespawn(elder,1000);
  c(!Boolean.TRUE.equals(p.recoverySnapshot("elder_guardian").get("enabled")),"pending elder blocks recovery");
  c(apply(p,UUID.randomUUID().toString(),"elder_guardian",ep).equals("BLOCKED")&&mt.record(elder).status==MonumentTracker.ElderStatus.PENDING_RESPAWN,"elder timer/state preserved");
  Map<String,Object> beforeMon=mt.snapshot();
  @SuppressWarnings("unchecked") Set<String> paid=(Set<String>)get(p,"signaturePaid");paid.add("ender_dragon");
  var pw=p.recoverySnapshot("warden");c(apply(p,UUID.randomUUID().toString(),"warden",pw).equals("APPLIED"),"other seal recovery works with unrelated pending elders");
  c(mt.snapshot().equals(beforeMon)&&paid.equals(Set.of("ender_dragon")),"unrelated monuments and signature rewards unchanged in memory");
  YamlConfiguration y=YamlConfiguration.loadConfiguration(dir.resolve("state.yml").toFile());c(y.getBoolean("signature-paid.ender_dragon")&&y.getMapList("monuments.elders").get(0).get("status").equals("PENDING_RESPAWN"),"unrelated pending elder and signature persisted unchanged");
  Path dir2=Files.createTempDirectory("bosstrial-recovery-fault-");ExcelBossTrial q=plugin(dir2);var qs=q.recoverySnapshot("wither");String qid=UUID.randomUUID().toString();
  set(q,ExcelBossTrial.class,"stateWriteProbe",(AtomicStateFile.Probe)phase->{if(phase.equals("after-move"))throw new IOException("injected ambiguous acknowledgement");});
  c(apply(q,qid,"wither",qs).equals("IO_FAILURE"),"after-rename failure reported");
  String committed=Files.readString(dir2.resolve("state.yml"));c(committed.contains(qid),"receipt actually reached disk before injected error");
  call(q,"saveState");c(Files.readString(dir2.resolve("state.yml")).equals(committed),"normal save cannot erase uncertain committed receipt");
  c(apply(q,qid,"wither",qs).equals("IO_FAILURE"),"uncertain live instance fails closed");
  ExcelBossTrial r=plugin(dir2);call(r,"loadState");c(apply(r,qid,"wither",qs).equals("REPLAYED"),"restart reconciles committed receipt");
  Path d3=Files.createTempDirectory("bosstrial-recovery-target-");ExcelBossTrial a=plugin(d3);
  @SuppressWarnings("unchecked") Map<UUID,EncounterLedger> es=(Map<UUID,EncounterLedger>)get(a,"encounters");
  UUID wid=UUID.randomUUID();es.put(wid,new EncounterLedger(BossKind.WITHER,wid,1500,1));
  var as=a.recoverySnapshot("wither");c(Boolean.TRUE.equals(as.get("active")),"retained selected encounter marked active without entity lookup");
  c(apply(a,UUID.randomUUID().toString(),"wither",as).equals("ACTIVE"),"selected active encounter refuses apply");
  var ward=a.recoverySnapshot("warden");c(apply(a,UUID.randomUUID().toString(),"warden",ward).equals("APPLIED")&&es.containsKey(wid),"unrelated active wither preserved during warden recovery");
  es.clear();var ae=a.recoverySnapshot("elder_guardian");String eid=UUID.randomUUID().toString();
  c(apply(a,eid,"elder_guardian",ae).equals("APPLIED")&&Boolean.TRUE.equals(get(a,"elderSealApproved")),"reviewed full elder recovery establishes latch without monument reset");
  set(a,ExcelBossTrial.class,"elderSealApproved",false);c(apply(a,eid,"elder_guardian",ae).equals("DIVERGED"),"later elder latch revoke not overwritten by receipt replay");
  Path d4=Files.createTempDirectory("bosstrial-recovery-prewrite-");ExcelBossTrial f=plugin(d4);call(f,"saveState");String old=Files.readString(d4.resolve("state.yml"));var fs=f.recoverySnapshot("wither");
  set(f,ExcelBossTrial.class,"stateWriteProbe",(AtomicStateFile.Probe)phase->{if(phase.equals("before-move"))throw new IOException("injected before rename");});
  c(apply(f,UUID.randomUUID().toString(),"wither",fs).equals("IO_FAILURE"),"before-rename failure reported");c(Files.readString(d4.resolve("state.yml")).equals(old),"before-rename keeps old disk state intact");
  Path d5=Files.createTempDirectory("bosstrial-recovery-corrupt-");Files.writeString(d5.resolve("state.yml"),"recovery:\n  protocol: 1\n  receipts:\n    - id: broken\n");ExcelBossTrial broken=plugin(d5);call(broken,"loadState");String corrupt=Files.readString(d5.resolve("state.yml"));
  c(!Boolean.TRUE.equals(broken.recoverySnapshot("wither").get("enabled")),"malformed receipt locks recovery");call(broken,"saveState");c(Files.readString(d5.resolve("state.yml")).equals(corrupt),"normal save preserves corrupt evidence rather than erasing it");
  Path d6=Files.createTempDirectory("bosstrial-recovery-yaml-");Files.writeString(d6.resolve("state.yml"),"bad: [unterminated");ExcelBossTrial badYaml=plugin(d6);call(badYaml,"loadState");c(!Boolean.TRUE.equals(badYaml.recoverySnapshot("wither").get("enabled")),"malformed YAML fails closed");
  Path d7=Files.createTempDirectory("bosstrial-recovery-external-");ExcelBossTrial external=plugin(d7);call(external,"saveState");var xs=external.recoverySnapshot("wither");
  String edited=Files.readString(d7.resolve("state.yml"))+"# externally edited after preview\n";Files.writeString(d7.resolve("state.yml"),edited);
  c(apply(external,UUID.randomUUID().toString(),"wither",xs).equals("IO_FAILURE"),"external disk edit after preview refuses owner write");
  c(Files.readString(d7.resolve("state.yml")).equals(edited),"external edited state left untouched");call(external,"saveState");c(Files.readString(d7.resolve("state.yml")).equals(edited),"normal save cannot overwrite external edit after lock");
  Path d8=Files.createTempDirectory("bosstrial-normal-external-");ExcelBossTrial normal=plugin(d8);call(normal,"saveState");String altered=Files.readString(d8.resolve("state.yml"))+"# normal-save external edit\n";Files.writeString(d8.resolve("state.yml"),altered);call(normal,"saveState");
  c(Files.readString(d8.resolve("state.yml")).equals(altered)&&!Boolean.TRUE.equals(normal.recoverySnapshot("wither").get("enabled")),"normal save independently checks disk fingerprint and locks");
  for (String protocol : List.of("1.5", "'1'", ".NaN", ".inf", "2")) {
   Path dp=Files.createTempDirectory("bosstrial-invalid-protocol-");String invalid="recovery:\n  protocol: "+protocol+"\n  receipts: []\n";
   Files.writeString(dp.resolve("state.yml"),invalid);ExcelBossTrial ip=plugin(dp);call(ip,"loadState");
   c(!Boolean.TRUE.equals(ip.recoverySnapshot("wither").get("enabled")),"invalid raw protocol locks: "+protocol);
   call(ip,"saveState");c(Files.readString(dp.resolve("state.yml")).equals(invalid),"invalid protocol evidence preserved: "+protocol);
  }
  System.out.println("RecoveryApiTest: "+n+" passed, 0 failed; proxy server + real temp filesystem, not live gameplay.");
 }
}
