/* Excel WRV maintained source; original attribution and license preserved. */ // Excel — WRV runtime-only diagnostic qualification; synthetic fixtures only.
package com.winkreyes.sealalerts;
import org.junit.jupiter.api.Test;
import org.bukkit.command.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;
import static org.junit.jupiter.api.Assertions.*;
class RuntimeDiagnosticsTest {
 WebhookSender sender() throws Exception {
  return new WebhookSender(new WebhookSender.Settings(), Files.createTempDirectory("excel-runtime").resolve("outbox").toFile(), Logger.getLogger("excel-runtime-test"));
 }
 ExcelSealAlerts plugin(WebhookSender w) throws Exception {
  Class<?> u=Class.forName("sun.misc.Unsafe"); Field f=u.getDeclaredField("theUnsafe"); f.setAccessible(true);
  Object unsafe=f.get(null); ExcelSealAlerts p=(ExcelSealAlerts)u.getMethod("allocateInstance",Class.class).invoke(unsafe,ExcelSealAlerts.class);
  Field s=ExcelSealAlerts.class.getDeclaredField("sender");s.setAccessible(true);s.set(p,w);return p;
 }
 CommandSender cs(Class<?> type,List<String> messages) {
  return (CommandSender)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},(p,m,a)-> {
   if(m.getName().equals("sendMessage")) { for(Object x:a) if(x instanceof String s) messages.add(s);return null;}
   if(m.getReturnType()==boolean.class)return true;
   return null;
  });
 }
 @Test void defaultOffToggleDoesNotEnqueueFlushOrPersist() throws Exception {
  WebhookSender w=sender(); assertFalse(w.diagnosticsEnabled());
  Field f=WebhookSender.class.getDeclaredField("flushRequested");f.setAccessible(true);
  w.diagnostics(true);assertTrue(w.diagnosticsEnabled());w.diagnostics(false);assertFalse(w.diagnosticsEnabled());
  assertEquals(0,w.pendingCount());assertEquals(0,w.heldCount());assertFalse(f.getBoolean(w));assertNull(w.lastError);
 }
 @Test void consoleCommandsStatusAndInvalidInputDoNotMutate() throws Exception {
  WebhookSender w=sender(); ExcelSealAlerts p=plugin(w); List<String> msgs=new ArrayList<>();CommandSender c=cs(ConsoleCommandSender.class,msgs);
  p.onCommand(c,null,"sealalerts",new String[]{"diagnostics","status"});assertFalse(w.diagnosticsEnabled());
  p.onCommand(c,null,"sealalerts",new String[]{"diagnostics","enable"});assertTrue(w.diagnosticsEnabled());
  p.onCommand(c,null,"sealalerts",new String[]{"diagnostics","bad"});assertTrue(w.diagnosticsEnabled());
  p.onCommand(c,null,"sealalerts",new String[]{"diagnostics","disable"});assertFalse(w.diagnosticsEnabled());
  assertEquals(0,w.pendingCount()); assertTrue(msgs.stream().anyMatch(x->x.contains("ON")));
 }
 @Test void playerLikeAndRemoteConsoleCannotEnable() throws Exception {
  WebhookSender w=sender();ExcelSealAlerts p=plugin(w);
  for(Class<?> type:List.of(CommandSender.class,RemoteConsoleCommandSender.class,BlockCommandSender.class)) {
   List<String> msgs=new ArrayList<>(); p.onCommand(cs(type,msgs),null,"sealalerts",new String[]{"diagnostics","enable"});
   assertFalse(w.diagnosticsEnabled());assertTrue(msgs.get(0).contains("console-only"));
  }
 }
 @Test void disableInvalidatesInflightTicketEvenAfterReenable() throws Exception {
  WebhookSender w=sender(); List<String> logs=new ArrayList<>();Logger l=Logger.getLogger("excel-runtime-test");
  Handler h=new Handler(){public void publish(LogRecord r){logs.add(r.getMessage());}public void flush(){}public void close(){}};
  l.addHandler(h);
  try {
   Method ticket=WebhookSender.class.getDeclaredMethod("diagnosticTicket");ticket.setAccessible(true);
   Method log=WebhookSender.class.getDeclaredMethod("logDiagnostic",long.class,int.class,String.class);log.setAccessible(true);
   w.diagnostics(true); long old=(long)ticket.invoke(w);w.diagnostics(false);w.diagnostics(true);
   log.invoke(w,old,400,"{\"code\":\"INVALID_JSON\"}");assertTrue(logs.isEmpty());
   log.invoke(w,(long)ticket.invoke(w),400,"{\"code\":\"INVALID_JSON\",\"message\":\"secret player\"}");
   assertEquals(List.of("ExcelSealAlerts rejection hint HTTP 400 code=INVALID_JSON"),logs);assertNull(w.lastError);
  } finally {l.removeHandler(h);}
 }
}
