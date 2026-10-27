import com.winkreyes.excelwandbars.core.EarnedBarGate;
import java.util.UUID;
import static com.winkreyes.excelwandbars.core.EarnedBarGate.Result.*;
public final class EarnedBarGateTest {
 private static void clearHold(EarnedBarGate g,UUID a,long t,long at){
  for(int i=0;i<5;i++) Check.eq(WAIT_CLEAR,g.decide(a,t,at+i*500,false,false),"clear grace poll "+i);
 }
 public static void run() {
  UUID a=UUID.randomUUID(), b=UUID.randomUUID(); EarnedBarGate g=new EarnedBarGate();
  long t=g.offer(a,0); clearHold(g,a,t,600);
  Check.eq(SEND,g.decide(a,t,3100,false,false),"no competing UI sends once after grace");
  Check.eq(STALE,g.decide(a,t,3101,false,false),"no duplicate");
  t=g.offer(a,10000); Check.eq(WAIT_STANCE,g.decide(a,t,10600,true,false),"stance protected");
  clearHold(g,a,t,11000); Check.eq(SEND,g.decide(a,t,13500,false,false),"after clear hold sends");
  t=g.offer(a,20000); g.decide(a,t,20600,true,false); g.decide(a,t,21000,false,false);
  Check.eq(WAIT_STANCE,g.decide(a,t,21500,true,false),"new stance interrupts clear hold");
  clearHold(g,a,t,22000);
  long newer=g.offer(a,25000); Check.ok(!g.isCurrent(a,t),"old callback cannot invalidate current token");
  Check.ok(g.isCurrent(a,newer),"new token current");
  Check.eq(STALE,g.decide(a,t,25600,false,false),"latest text wins");
  clearHold(g,a,newer,25600); Check.eq(SEND,g.decide(a,newer,28100,false,false),"replacement sends once");
  t=g.offer(a,30000); clearHold(g,a,t,30600);
  for(int i=0;i<3;i++) Check.eq(WAIT_PARTY,g.decide(a,t,33100+i*2500,false,true),"Together hold "+i);
  Check.eq(PARTY_SKIP,g.decide(a,t,40600,false,true),"Together never overridden after retry limit");
  t=g.offer(a,50000); Check.eq(EXPIRED,g.decide(a,t,110000,true,false),"bounded stale notification expiry");
  t=g.offer(a,120000); long tb=g.offer(b,120000); g.forget(a);
  Check.eq(STALE,g.decide(a,t,120100,false,false),"quit clears player");
  clearHold(g,b,tb,120100); Check.eq(SEND,g.decide(b,tb,122600,false,false),"other player isolated");
  t=g.offer(a,130000); g.clear(); Check.eq(STALE,g.decide(a,t,130100,false,false),"reload clears");
  t=g.offer(a,140000); for(int i=0;i<100;i++) Check.eq(WAIT_STANCE,g.decide(a,t,140001+i*500,true,false),"continuous stance does not write "+i);
  // Wall-time lag does not consume clear grace; only adapter's ten-tick callbacks do.
  t=g.offer(a,200000); Check.eq(WAIT_CLEAR,g.decide(a,t,210000,false,false),"lag first clear poll");
  Check.eq(WAIT_CLEAR,g.decide(a,t,220000,false,false),"lag second clear poll");
  Check.eq(WAIT_CLEAR,g.decide(a,t,230000,false,false),"lag third clear poll");
  Check.eq(WAIT_CLEAR,g.decide(a,t,240000,false,false),"lag fourth clear poll");
  Check.eq(WAIT_CLEAR,g.decide(a,t,241000,false,false),"lag fifth clear poll");
  Check.eq(SEND,g.decide(a,t,242000,false,false),"lag sixth clear poll");
 }
}
