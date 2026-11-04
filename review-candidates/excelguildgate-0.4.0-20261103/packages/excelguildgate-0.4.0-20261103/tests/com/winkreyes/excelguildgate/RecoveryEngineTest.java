package com.winkreyes.excelguildgate;
import java.io.*;import java.nio.file.*;import java.util.*;
import org.bukkit.configuration.file.YamlConfiguration;
public final class RecoveryEngineTest {
 static int n;static void check(boolean x,String label){if(!x)throw new AssertionError(label);n++;}
 static class Host implements RecoveryEngine.Host{
  Path file;Map<String,Object> s=new LinkedHashMap<>();Map<String,Map<String,Object>> j=new LinkedHashMap<>();boolean blocked=false;int calls=0,writes=0;int failWrite=0;String failPhase="before-move";
  String gen="boot1",token=RecoveryEngine.digest("before");long deadline=1000;boolean active=false;Map<String,String> applied=new HashMap<>();
  Host()throws Exception{file=Files.createTempDirectory("gg-recovery-test-").resolve("state.yml");}
  public Map<String,Object> state(){return new LinkedHashMap<>(s);}
  @SuppressWarnings("unchecked")public Map<String,Map<String,Object>> journal(){return(Map<String,Map<String,Object>>)RecoveryEngine.copy(j);}
  public boolean available(){return !blocked;}
  public boolean knownEnabled(String seal){return Set.of("raid","wither","elder_guardian","warden","ender_dragon").contains(seal);}
  public boolean broken(String seal){return Boolean.TRUE.equals(s.get(seal));}
  public Map<String,Object> bossSnapshot(String seal){return new LinkedHashMap<>(Map.of("protocol",1,"seal",seal,"enabled",true,"active",active,"deadline",deadline,"generation",gen,"stateToken",token,"reason",""));}
  public Map<String,Object> bossApply(String op,String digest,String seal,Map<String,Object> expected){
   calls++;String status;
   if(applied.containsKey(op))status=applied.get(op).equals(digest)&&token.equals(RecoveryEngine.digest("after"))?"REPLAYED":"DIVERGED";
   else if(active)status="ACTIVE";
   else if(!RecoveryEngine.digest(bossSnapshot(seal)).equals(RecoveryEngine.digest(expected)))status="STALE";
   else{status="APPLIED";applied.put(op,digest);token=RecoveryEngine.digest("after");deadline=0;}
   Map<String,Object> r=bossSnapshot(seal);r.put("status",status);r.put("operationId",op);return r;
  }
  public void commit(Map<String,Map<String,Object>> journal,String grant,long now)throws IOException{
   if(blocked)throw new IOException("locked");writes++;
   Map<String,Object> projected=new LinkedHashMap<>(s);if(grant!=null)projected.put(grant,true);
   YamlConfiguration y=new YamlConfiguration();y.set("seals",projected);y.set("journal",new ArrayList<>(journal.values()));
   try{RecoveryAtomicFile.write(file,y.saveToString().getBytes(java.nio.charset.StandardCharsets.UTF_8),phase->{if(writes==failWrite&&phase.equals(failPhase))throw new IOException("injected");});}
   catch(IOException e){blocked=true;throw e;}
   s=projected;j=journal;
  }
  @SuppressWarnings("unchecked")void restart()throws Exception{
   if(Files.exists(file)){YamlConfiguration y=new YamlConfiguration();y.load(file.toFile());s=new LinkedHashMap<>(y.getConfigurationSection("seals").getValues(false));j=new LinkedHashMap<>();for(Map<?,?> r:y.getMapList("journal")){Map<String,Object> c=(Map<String,Object>)RecoveryEngine.copy(r);j.put((String)c.get("caseId"),c);}}
   blocked=false;
  }
 }
 static Map<String,Object> preview(RecoveryEngine e,String seal,String id){return e.preview("admin1",true,seal,id,"reviewed encounter evidence",10);}
 static String tok(Map<String,Object> r){return(String)r.get("token");}
 static String status(Map<String,Object> r){return(String)r.get("status");}
 public static void main(String[]z)throws Exception{
  Host h=new Host();RecoveryEngine e=new RecoveryEngine(h);Map<String,Object> p=preview(e,"wither","case1");
  check("PREVIEW".equals(status(p))&&h.writes==0&&h.calls==0,"preview read only");String t=tok(p);
  check("DENIED".equals(status(e.confirm("admin2",true,t,20))),"actor binding");
  check("DENIED".equals(status(e.confirm("admin1",false,t,20))),"permission recheck");
  check("COMPLETED".equals(status(e.confirm("admin1",true,t,20)))&&h.broken("wither"),"confirm grant");
  check(h.calls==1&&h.writes==2,"intent before boss and completion atomic");
  check("EXPIRED".equals(status(e.confirm("admin1",true,t,21)))&&h.calls==1,"consumed token no retry effects");
  check("COMPLETED_HISTORY".equals(status(preview(e,"wither","case1"))),"completed no regrant");
  h.s.clear();check("COMPLETED_HISTORY".equals(status(preview(e,"wither","case1"))),"later revoke does not reopen receipt");
  check("CONFLICT".equals(status(preview(e,"warden","case1"))),"case bound to seal");
  check("CONFLICT".equals(status(e.preview("admin1",true,"wither","case1","changed reason",20))),"case bound to reason");
  Host raid=new Host();RecoveryEngine re=new RecoveryEngine(raid);String rt=tok(preview(re,"raid","raid1"));
  check("COMPLETED".equals(status(re.confirm("admin1",true,rt,20)))&&raid.calls==0,"raid no boss dependency");
  Host stale=new Host();RecoveryEngine se=new RecoveryEngine(stale);String st=tok(preview(se,"wither","s"));stale.s.put("raid",true);
  check("STALE".equals(status(se.confirm("admin1",true,st,20)))&&stale.calls==0&&stale.writes==0,"global CAS");
  Host bs=new Host();RecoveryEngine be=new RecoveryEngine(bs);String bt=tok(preview(be,"wither","b"));bs.deadline=2000;
  check("STALE".equals(status(be.confirm("admin1",true,bt,20)))&&bs.calls==0,"new cooldown CAS");
  Host ex=new Host();RecoveryEngine ee=new RecoveryEngine(ex);String et=tok(preview(ee,"wither","x"));
  check("EXPIRED".equals(status(ee.confirm("admin1",true,et,300010))),"expiry boundary");
  check("EXPIRED".equals(status(new RecoveryEngine(ex).confirm("admin1",true,et,20))),"boot token invalidation");
  Host fi=new Host();fi.failWrite=1;RecoveryEngine fe=new RecoveryEngine(fi);String ft=tok(preview(fe,"wither","f"));
  check("IO_FAILURE".equals(status(fe.confirm("admin1",true,ft,20)))&&fi.calls==0&&!fi.broken("wither"),"intent write failure no side effects");
  Host after=new Host();after.failWrite=2;after.failPhase="after-move";RecoveryEngine ae=new RecoveryEngine(after);String at=tok(preview(ae,"wither","a"));
  check("IO_FAILURE".equals(status(ae.confirm("admin1",true,at,20)))&&!after.broken("wither")&&after.blocked,"rename ack failure no memory publish");
  String disk=Files.readString(after.file);try{after.commit(after.j,null,30);throw new AssertionError("write should lock");}catch(IOException ok){}
  check(disk.equals(Files.readString(after.file)),"normal stale save cannot overwrite committed receipt");
  after.restart();check(after.broken("wither")&&"COMPLETED".equals(after.j.get("a").get("status")),"restart reads committed receipt and grant together");
  Host pending=new Host();pending.failWrite=2;RecoveryEngine pe=new RecoveryEngine(pending);String pt=tok(preview(pe,"wither","p"));pe.confirm("admin1",true,pt,20);
  check(!pending.broken("wither")&&pending.applied.size()==1,"partial boss-success global-write fail");
  pending.restart();pending.gen="boot2";pending.failWrite=0;pe=new RecoveryEngine(pending);String resume=tok(preview(pe,"wither","p"));
  check("COMPLETED".equals(status(pe.confirm("admin1",true,resume,30)))&&pending.applied.size()==1,"explicit retry receipt replay after restart");
  Host unapplied=new Host();unapplied.failWrite=1;unapplied.failPhase="after-move";RecoveryEngine ue=new RecoveryEngine(unapplied);String ut=tok(preview(ue,"wither","u"));ue.confirm("admin1",true,ut,20);
  unapplied.restart();unapplied.gen="boot2";unapplied.failWrite=0;ue=new RecoveryEngine(unapplied);String ur=tok(preview(ue,"wither","u"));
  check("PENDING".equals(status(ue.confirm("admin1",true,ur,30)))&&!unapplied.broken("wither"),"restart-before-boss stale pending not rebased");
  Host active=new Host();active.active=true;check("BLOCKED".equals(status(preview(new RecoveryEngine(active),"wither","active"))),"active encounter blocks preview");
  check("INVALID".equals(status(e.preview("admin1",true,"raid","bad.case","reason",10))),"safe case ID");
  check("INVALID".equals(status(e.preview("admin1",true,"raid","empty"," ",10))),"explicit reason");
  check(RecoveryEngine.validReceipt("p",pending.j.get("p")),"YAML roundtrip receipt digest");
  Host prior=new Host();prior.s.put("wither",true);RecoveryEngine pri=new RecoveryEngine(prior);Map<String,Object> pp=preview(pri,"wither","prior-manual");
  check("PRESERVE".equals(pp.get("globalAction")),"already-broken previews no global regrant");
  check("COMPLETED".equals(status(pri.confirm("admin1",true,tok(pp),30)))&&prior.broken("wither")&&prior.deadline==0,"already-broken reviewed cooldown repair");
  Host rr=new Host();rr.s.put("raid",true);check("ALREADY_BROKEN".equals(status(preview(new RecoveryEngine(rr),"raid","prior-raid")))&&rr.writes==0,"already-broken raid pure no-op");
  Host malformed=new Host(){public Map<String,Object> bossApply(String op,String dig,String seal,Map<String,Object> expected){Map<String,Object> r=super.bossApply(op,dig,seal,expected);r.put("protocol",2);return r;}};
  RecoveryEngine me=new RecoveryEngine(malformed);String mtoken=tok(preview(me,"wither","malformed"));
  check("PENDING".equals(status(me.confirm("admin1",true,mtoken,30)))&&!malformed.broken("wither"),"malformed success response does not grant");
  System.out.println("RecoveryEngineTest: "+n+" passed");
 }
}
