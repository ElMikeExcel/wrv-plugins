/* Excel WRV maintenance; existing authorship retained. */ package wrv.rewards;
import java.nio.file.*;import java.util.*;import java.io.*;
import static wrv.rewards.TeamRewardLedger.*;
public class TeamRewardLedgerTest{
 static int checks;static void ok(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);} interface Run{void run()throws Exception;}static void fails(Run r){checks++;try{r.run();}catch(Exception expected){return;}throw new AssertionError("expected failure "+checks);}
 public static void main(String[]args)throws Exception{
 if(args.length==2&&args[0].equals("crash")){var l=new TeamRewardLedger(Path.of(args[1]));UUID p=UUID.fromString("00000000-0000-0000-0000-000000000001");l.openEncounter("crash");l.qualify("crash",p);l.win("crash","p",Map.of(p,List.of(new RewardUnit("u","opaque"))));l.beginDelivery(p,"crash/"+p);Runtime.getRuntime().halt(0);}
 Path dir=Files.createTempDirectory("team-reward-test-");Path p=dir.resolve("ledger");UUID a=UUID.randomUUID(),b=UUID.randomUUID();String ca="dragon-final/"+a,cb="dragon-final/"+b;
 var reward=new RewardUnit("shared-gem-1","opaque:item:gem:1");Map<UUID,List<RewardUnit>> plan=Map.of(a,List.of(reward),b,List.of());String pending;
 try(var l=new TeamRewardLedger(p)){
 l.openEncounter("dragon-final");l.openEncounter("dragon-final");l.qualify("dragon-final",a);l.qualify("dragon-final",a);l.qualify("dragon-final",b);ok(l.roster("dragon-final").size()==2); // late participant is retained without percent condition
 fails(()->new TeamRewardLedger(p));fails(()->l.win("dragon-final","p1",Map.of(a,List.of(reward))));fails(()->l.win("dragon-final","p1",Map.of(a,List.of(reward),b,List.of(reward))));
 l.win("dragon-final","p1",plan);l.win("dragon-final","p1",plan);ok(l.claims(a).size()==1);fails(()->l.qualify("dragon-final",UUID.randomUUID()));fails(()->l.win("dragon-final","different",plan));fails(()->l.win("dragon-final","p1",Map.of(a,List.of(),b,List.of())));fails(()->l.beginDelivery(b,ca));
 Claim c=l.beginDelivery(a,ca);pending=c.attempt();ok(c.state()==State.PENDING);fails(()->l.beginDelivery(a,ca));fails(()->l.finishDelivery(a,ca,UUID.randomUUID().toString(),Outcome.DELIVERED));
 }
 try(var l=new TeamRewardLedger(p)){ok(l.claims(a).get(0).state()==State.REVIEW);fails(()->l.beginDelivery(a,ca));fails(()->l.finishDelivery(a,ca,pending,Outcome.DELIVERED));ok(l.roster("dragon-final").containsAll(List.of(a,b)));l.win("dragon-final","p1",plan); // reconnect/restart does not erase or replay
 Claim c=l.beginDelivery(b,cb);l.finishDelivery(b,cb,c.attempt(),Outcome.NOT_APPLIED);ok(l.claims(b).get(0).state()==State.OWED);c=l.beginDelivery(b,cb);l.finishDelivery(b,cb,c.attempt(),Outcome.DELIVERED);l.finishDelivery(b,cb,c.attempt(),Outcome.DELIVERED);ok(l.claims(b).get(0).state()==State.DELIVERED);fails(()->l.beginDelivery(b,cb));
 for(String encounter:List.of("raid-attempt","elder-trio-final","wither-attempt","warden-attempt")){l.openEncounter(encounter);l.qualify(encounter,a);l.win(encounter,"plan",Map.of(a,List.of(new RewardUnit("unit", "opaque"))));}
 ok(l.claims(a).size()==5);
 }
 try(var l=new TeamRewardLedger(p)){ok(l.claims(b).get(0).state()==State.DELIVERED);Claim c=l.beginDelivery(a,"wither-attempt/"+a);l.finishDelivery(a,c.id(),c.attempt(),Outcome.UNCERTAIN);ok(l.claims(a).stream().anyMatch(x->x.id().equals(c.id())&&x.state()==State.REVIEW));}
 try(var l=new TeamRewardLedger(p)){
 l.openEncounter("lost");l.qualify("lost",a);l.closeWithoutWin("lost",EncounterOutcome.LOST);l.closeWithoutWin("lost",EncounterOutcome.LOST);l.openEncounter("lost");ok(l.outcome("lost")==EncounterOutcome.LOST);fails(()->l.qualify("lost",b));fails(()->l.win("lost","p",Map.of(a,List.of())));fails(()->l.closeWithoutWin("lost",EncounterOutcome.ABORTED));
 l.openEncounter("aborted");l.closeWithoutWin("aborted",EncounterOutcome.ABORTED);fails(()->l.qualify("aborted",a));l.openEncounter("empty");fails(()->l.win("empty","p",Map.of()));
 l.openEncounter("immutable");l.qualify("immutable",a);List<RewardUnit> mutable=new ArrayList<>(List.of(new RewardUnit("u","payload")));Map<UUID,List<RewardUnit>> m=new HashMap<>();m.put(a,mutable);l.win("immutable","p",m);mutable.clear();m.clear();Claim c=l.claims(a).stream().filter(x->x.encounterId().equals("immutable")).findFirst().orElseThrow();ok(c.units().size()==1);fails(()->c.units().clear());fails(()->l.closeWithoutWin("immutable",EncounterOutcome.LOST));
 }
 try(var l=new TeamRewardLedger(p)){ok(l.outcome("lost")==EncounterOutcome.LOST);ok(l.outcome("aborted")==EncounterOutcome.ABORTED);fails(()->l.win("lost","p",Map.of(a,List.of())));}
 try(var l=new TeamRewardLedger(p)){
 fails(()->l.openEncounter("!IMPORTED"));l.importConsumedScope("signature:elder");l.importConsumedScope("signature:elder");l.openEncounter("elder-new");l.qualify("elder-new",a);fails(()->l.winOnceScope("elder-new","signature:elder","p",Map.of(a,List.of(new RewardUnit("e","opaque")))));ok(l.outcome("elder-new")==EncounterOutcome.ACTIVE);
 l.openEncounter("signature-w1");l.qualify("signature-w1",a);var once=Map.of(a,List.of(new RewardUnit("signature:wither:item1","opaque")));l.winOnceScope("signature-w1","signature:wither","p",once);l.winOnceScope("signature-w1","signature:wither","p",once);ok(l.consumedScopes().get("signature:wither").equals("signature-w1"));
 l.openEncounter("signature-w2");l.qualify("signature-w2",a);fails(()->l.winOnceScope("signature-w2","signature:wither","p",once));ok(l.claims(a).stream().noneMatch(c->c.encounterId().equals("signature-w2")));
 }
 try(var l=new TeamRewardLedger(p)){ok(l.consumedScopes().containsKey("signature:elder"));fails(()->l.winOnceScope("signature-w2","signature:wither","p",Map.of(a,List.of())));}
 // external mutation blocks subsequent persistence, and instance becomes unusable.
 try(var l=new TeamRewardLedger(p)){byte[] original=Files.readAllBytes(p);Files.write(p,new byte[]{1,2});fails(()->l.openEncounter("new"));fails(()->l.claims(a));Files.write(p,original);}
 byte[] bytes=Files.readAllBytes(p);bytes[20]^=1;Files.write(p,bytes);fails(()->new TeamRewardLedger(p));Files.write(p,new byte[]{1});fails(()->new TeamRewardLedger(p));
 Path crash=dir.resolve("crash-ledger");Process child=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-cp",System.getProperty("java.class.path"),TeamRewardLedgerTest.class.getName(),"crash",crash.toString()).inheritIO().start();ok(child.waitFor()==0);try(var l=new TeamRewardLedger(crash)){ok(l.claims(UUID.fromString("00000000-0000-0000-0000-000000000001")).get(0).state()==State.REVIEW);}
 System.out.println("PASS "+checks+" checks; actual temp files: "+dir);
 }
}
