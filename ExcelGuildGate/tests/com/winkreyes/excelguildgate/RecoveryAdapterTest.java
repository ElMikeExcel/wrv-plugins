package com.winkreyes.excelguildgate;
import java.io.*;import java.nio.file.*;import java.lang.reflect.*;import java.util.*;import java.util.logging.Logger;
import org.bukkit.*;import org.bukkit.plugin.java.JavaPlugin;import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.*;
/** Actual GuildGate adapter/private persistence methods, proxy server, real temporary files; not a live server. */
public final class RecoveryAdapterTest{
 static int n;static boolean permission=true,main=true;static List<String> messages=new ArrayList<>();
 static void c(boolean x,String s){if(!x)throw new AssertionError(s);n++;}
 static void set(Object o,Class<?> t,String k,Object v)throws Exception{Field f=t.getDeclaredField(k);f.setAccessible(true);f.set(o,v);}
 static Object get(Object o,String k)throws Exception{Field f=ExcelGuildGate.class.getDeclaredField(k);f.setAccessible(true);return f.get(o);}
 static Object call(Object o,String k)throws Exception{Method m=ExcelGuildGate.class.getDeclaredMethod(k);m.setAccessible(true);return m.invoke(o);}
 static ExcelGuildGate plugin(Path p)throws Exception{
  Class<?> u=Class.forName("sun.misc.Unsafe");Field uf=u.getDeclaredField("theUnsafe");uf.setAccessible(true);Object us=uf.get(null);
  ExcelGuildGate g=(ExcelGuildGate)u.getMethod("allocateInstance",Class.class).invoke(us,ExcelGuildGate.class);
  set(g,JavaPlugin.class,"isEnabled",true);set(g,JavaPlugin.class,"dataFolder",p.toFile());set(g,JavaPlugin.class,"logger",Logger.getLogger("RecoveryAdapterTest"));
  Map<String,Object> defs=new LinkedHashMap<>();for(String id:ExcelGuildGate.SEAL_ORDER)defs.put(id,null);
  set(g,ExcelGuildGate.class,"defs",defs);set(g,ExcelGuildGate.class,"catchUp",new CatchUpTracker());
  Set<String> ids=new LinkedHashSet<>(ExcelGuildGate.SEAL_ORDER);set(g,ExcelGuildGate.class,"state",new SealState(ids,ids));
  set(g,ExcelGuildGate.class,"recoveryDiskDigest","");set(g,ExcelGuildGate.class,"recoveryLastGlobalDigest","");
  set(g,ExcelGuildGate.class,"recoveryJournal",new LinkedHashMap<>());set(g,ExcelGuildGate.class,"recoveryWriteProbe",(RecoveryAtomicFile.Fault)phase->{});
  call(g,"loadState");call(g,"ensureRecoveryEngine");return g;
 }
 static RecoveryEngine engine(ExcelGuildGate g)throws Exception{return(RecoveryEngine)get(g,"recovery");}
 static String token(RecoveryEngine e,String id){return(String)e.preview("CONSOLE",true,"raid",id,"reviewed raid victory",10).get("token");}
 static String confirm(RecoveryEngine e,String t){return String.valueOf(e.confirm("CONSOLE",true,t,20).get("status"));}
 public static void main(String[] a)throws Exception{
  Server server=(Server)Proxy.newProxyInstance(Server.class.getClassLoader(),new Class[]{Server.class},(p,m,x)->{
   if(m.getName().equals("isPrimaryThread"))return main;if(m.getName().equals("getLogger"))return Logger.getLogger("mock");
   if(m.getReturnType()==boolean.class)return false;if(m.getReturnType()==int.class)return 0;if(m.getReturnType()==String.class)return "offline";return null;});
  Field sf=Bukkit.class.getDeclaredField("server");sf.setAccessible(true);sf.set(null,server);
  Path dir=Files.createTempDirectory("gg-adapter-");ExcelGuildGate g=plugin(dir);RecoveryEngine e=engine(g);
  String t=token(e,"raid-recovery");c(t!=null&&!Files.exists(dir.resolve("state.yml")),"real adapter preview no state write");
  c(confirm(e,t).equals("COMPLETED"),"real adapter raid recovery");
  SealState state=(SealState)get(g,"state");c(state.isBroken("raid")&&state.brokenCount()==1,"only raid granted");
  YamlConfiguration y=new YamlConfiguration();y.load(dir.resolve("state.yml").toFile());
  c(y.getBoolean("seals.raid.broken")&&y.getMapList("admin-recovery.receipts").size()==1,"same file receipt/global state");
  long rev=y.getLong("admin-recovery.global-revision");c(rev==1,"global revision increments once on actual grant");
  call(g,"saveState");y.load(dir.resolve("state.yml").toFile());c(y.getMapList("admin-recovery.receipts").size()==1&&y.getLong("admin-recovery.global-revision")==rev,"normal save preserves receipt/revision");
  ExcelGuildGate restarted=plugin(dir);c(engine(restarted).preview("CONSOLE",true,"raid","raid-recovery","reviewed raid victory",30).get("status").equals("COMPLETED_HISTORY"),"real reload validates receipt digest");
  // Pending raid intent with completion-before-move failure; later normal grant+revoke must invalidate old CAS.
  Path d2=Files.createTempDirectory("gg-adapter-pending-");ExcelGuildGate q=plugin(d2);final int[] moves={0};
  set(q,ExcelGuildGate.class,"recoveryWriteProbe",(RecoveryAtomicFile.Fault)phase->{if(phase.equals("before-move")&&++moves[0]==2)throw new IOException("fail completion");});
  c(confirm(engine(q),token(engine(q),"pending")).equals("IO_FAILURE"),"real completion pre-move failure");
  ExcelGuildGate r=plugin(d2);SealState rs=(SealState)get(r,"state");rs.grant("raid",50);call(r,"saveState");rs.revoke("raid");call(r,"saveState");
  c(engine(r).preview("CONSOLE",true,"raid","pending","reviewed raid victory",60).get("status").equals("STALE"),"grant then revoke cannot revive pending old snapshot");
  // Completion rename succeeds, acknowledgement fails. Normal save is locked and cannot erase receipt.
  Path d3=Files.createTempDirectory("gg-adapter-ack-");ExcelGuildGate k=plugin(d3);final int[] renames={0};
  set(k,ExcelGuildGate.class,"recoveryWriteProbe",(RecoveryAtomicFile.Fault)phase->{if(phase.equals("after-move")&&++renames[0]==2)throw new IOException("lost ack");});
  c(confirm(engine(k),token(engine(k),"ack")).equals("IO_FAILURE"),"real lost completion ack");
  String committed=Files.readString(d3.resolve("state.yml"));call(k,"saveState");c(committed.equals(Files.readString(d3.resolve("state.yml"))),"real normal save locked after committed-but-unacked replacement");
  c(!((SealState)get(k,"state")).isBroken("raid"),"no premature memory publication");
  ExcelGuildGate kr=plugin(d3);c(((SealState)get(kr,"state")).isBroken("raid"),"restart reconciles committed global grant");
  String[] corrupt={"admin-recovery:\n  protocol: 1.5\n  receipts: []\n","admin-recovery:\n  protocol: \"1\"\n  receipts: []\n","admin-recovery:\n  protocol: 2\n  receipts: []\n","admin-recovery:\n  protocol: 1\n  receipts: wrong\n","admin-recovery:\n  protocol: 1\n  receipts: [wrong]\n","admin-recovery: wrong\n","bad: [unterminated"};
  for(String bad:corrupt){Path d=Files.createTempDirectory("gg-adapter-invalid-");Files.writeString(d.resolve("state.yml"),bad);ExcelGuildGate b=plugin(d);c(Boolean.TRUE.equals(get(b,"recoveryWriteBlocked")),"malformed journal/YAML locks");call(b,"saveState");c(bad.equals(Files.readString(d.resolve("state.yml"))),"malformed evidence preserved");}
  // Command adapter permission and main-thread checks execute before previews.
  ConsoleCommandSender console=(ConsoleCommandSender)Proxy.newProxyInstance(ConsoleCommandSender.class.getClassLoader(),new Class[]{ConsoleCommandSender.class},(p,m,x)->{
   if(m.getName().equals("hasPermission"))return permission;if(m.getName().equals("sendMessage")){if(x!=null)messages.add(String.valueOf(x[x.length-1]));return null;}
   if(m.getName().equals("getName"))return "CONSOLE";if(m.getReturnType()==boolean.class)return true;return null;});
  Method cmd=ExcelGuildGate.class.getDeclaredMethod("recoveryCommand",CommandSender.class,String[].class);cmd.setAccessible(true);
  permission=false;messages.clear();cmd.invoke(g,console,new String[]{"recovery","preview","raid","console-no-perm","why"});
  c(messages.stream().anyMatch(m->m.contains("ALREADY_BROKEN"))&&!messages.stream().anyMatch(m->m.contains("permission")),"actual console identity allowed despite default-false permission");
  org.bukkit.entity.Player player=(org.bukkit.entity.Player)Proxy.newProxyInstance(org.bukkit.entity.Player.class.getClassLoader(),new Class[]{org.bukkit.entity.Player.class},(p,m,x)->{
   if(m.getName().equals("hasPermission"))return permission;if(m.getName().equals("getUniqueId"))return new UUID(1,1);
   if(m.getName().equals("sendMessage")){messages.add(String.valueOf(x[x.length-1]));return null;}
   if(m.getReturnType()==boolean.class)return false;if(m.getReturnType()==int.class)return 0;return null;});
  messages.clear();cmd.invoke(g,player,new String[]{"recovery","preview","raid","player-no-perm","why"});c(messages.stream().anyMatch(m->m.contains("permission")),"player still needs dedicated permission plus admin");
  CommandSender generic=(CommandSender)Proxy.newProxyInstance(CommandSender.class.getClassLoader(),new Class[]{CommandSender.class},(p,m,x)->{
   if(m.getName().equals("hasPermission"))return true;if(m.getName().equals("sendMessage")){messages.add(String.valueOf(x[x.length-1]));return null;}if(m.getName().equals("getName"))return "CONSOLE";if(m.getReturnType()==boolean.class)return true;return null;});
  messages.clear();cmd.invoke(g,generic,new String[]{"recovery","preview","raid","fake-console","why"});c(messages.stream().anyMatch(m->m.contains("permission")),"generic sender named CONSOLE never elevated");
  for(String granted:List.of("excelguildgate.admin","excelguildgate.recovery")){
   org.bukkit.entity.Player partial=(org.bukkit.entity.Player)Proxy.newProxyInstance(org.bukkit.entity.Player.class.getClassLoader(),new Class[]{org.bukkit.entity.Player.class},(p,m,x)->{
    if(m.getName().equals("hasPermission"))return granted.equals(String.valueOf(x[0]));if(m.getName().equals("getUniqueId"))return new UUID(2,2);
    if(m.getName().equals("sendMessage")){messages.add(String.valueOf(x[x.length-1]));return null;}if(m.getReturnType()==boolean.class)return false;if(m.getReturnType()==int.class)return 0;return null;});
   messages.clear();cmd.invoke(g,partial,new String[]{"recovery","preview","raid","partial","why"});c(messages.stream().anyMatch(m->m.contains("permission")),"player single permission insufficient: "+granted);
  }
  for(Class<?> type:List.of(BlockCommandSender.class,RemoteConsoleCommandSender.class)){
   CommandSender other=(CommandSender)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},(p,m,x)->{
    if(m.getName().equals("hasPermission"))return true;if(m.getName().equals("sendMessage")){messages.add(String.valueOf(x[x.length-1]));return null;}if(m.getReturnType()==boolean.class)return true;return null;});
   messages.clear();cmd.invoke(g,other,new String[]{"recovery","preview","raid","not-console","why"});c(messages.stream().anyMatch(m->m.contains("permission")),"no console elevation for "+type.getSimpleName());
  }
  permission=true;main=false;messages.clear();cmd.invoke(g,console,new String[]{"recovery","preview","wither","no","why"});c(messages.stream().anyMatch(m->m.contains("main thread")),"adapter thread gate");main=true;
  System.out.println("RecoveryAdapterTest: "+n+" passed; actual adapter, proxy server, temporary files; not live gameplay.");
 }
}
