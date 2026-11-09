/* Excel WRV maintenance; existing authorship retained. */ package wrv.rewards;
import java.nio.file.*;import java.util.*;
public class ComponentTest{
 static int n;static void check(boolean x){n++;if(!x)throw new AssertionError();}
 public static void main(String[]x)throws Exception{
 Path file=Files.createTempDirectory("component").resolve("ledger");UUID a=UUID.randomUUID(),b=UUID.randomUUID();
 try(var l=new TeamRewardLedger(file)){
 l.openEncounter("e1");l.qualify("e1",a);l.creditComponentInto("e1","trio");check(l.roster("trio").equals(Set.of(a)));check(l.outcome("e1")==TeamRewardLedger.EncounterOutcome.WON);
 l.creditComponentInto("e1","trio");check(l.claims(a).size()==1);
 l.openEncounter("denied");l.qualify("denied",b);l.closeWithoutWin("denied",TeamRewardLedger.EncounterOutcome.LOST);
 try{l.creditComponentInto("denied","trio");throw new AssertionError();}catch(IllegalStateException expected){n++;}check(!l.roster("trio").contains(b));
 l.openEncounter("e2");l.qualify("e2",b);l.creditComponentInto("e2","trio");check(l.roster("trio").equals(Set.of(a,b)));
 }
 try(var l=new TeamRewardLedger(file)){check(l.roster("trio").equals(Set.of(a,b)));l.creditComponentInto("e2","trio");check(l.claims(b).size()==1);}
 System.out.println("ComponentTest: "+n+" checks passed");
 }
}
