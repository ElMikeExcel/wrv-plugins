package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.BarLoadout;
import java.util.List;
/** 0.3.5 design 5.1: the unacknowledged-new set's transition rules. */
public class BarLoadoutNewTest {
 static int n; static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 public static void main(String[]args){
  // Seed is a baseline, never an unlock: no badges for day-one players.
  BarLoadout s=BarLoadout.seed(List.of("mend","heal"),3);
  ok(s.newCount()==0);
  ok(s.seen().size()==2);
  // One free slot: the first new spell is placed AND badged, the second parked AND badged.
  BarLoadout.Sync r1=s.sync(List.of("mend","heal","boon","shackle"));
  ok(r1.placed.containsKey("boon"));
  ok(r1.toBook.equals(List.of("shackle")));
  ok(r1.newKeys.equals(List.of("boon","shackle")));
  ok(s.isNew("boon")&&s.isNew("shackle"));
  ok(s.newSpells().equals(List.of("boon","shackle")));
  // Re-sync never re-badges (seen is seen).
  BarLoadout.Sync r2=s.sync(List.of("mend","heal","boon","shackle"));
  ok(r2.newKeys.isEmpty());
  ok(s.newCount()==2);
  // Viewing is not an ack: nothing above touched the set. Got it clears exactly one.
  ok(s.ackNew("shackle"));
  ok(s.newCount()==1);
  ok(!s.ackNew("shackle")); // already cleared
  // Full bar: parked in the book AND badged.
  BarLoadout f=BarLoadout.seed(List.of("mend","heal","boon"),3);
  BarLoadout.Sync rf=f.sync(List.of("mend","heal","boon","frost"));
  ok(rf.toBook.equals(List.of("frost")));
  ok(f.isNew("frost"));
  ok(f.indexOf("frost")<0);
  // Placing it (a swap-in put) clears the badge.
  f.put("frost",0);
  ok(!f.isNew("frost"));
  ok(f.indexOf("frost")==0);
  // Put of an on-bar new spell (a move) also clears it.
  BarLoadout m=BarLoadout.seed(List.of("mend","heal"),3);
  m.sync(List.of("mend","heal","boon")); // boon placed at 2, badged
  ok(m.isNew("boon"));
  m.put("boon",0);
  ok(!m.isNew("boon"));
  // Swap does not ack either party.
  BarLoadout w=BarLoadout.seed(List.of("mend","heal"),3);
  w.sync(List.of("mend","heal","boon"));
  w.swap(0,2);
  ok(w.isNew("boon"));
  // Removing a badged spell keeps the badge (it is still unplaced knowledge).
  w.remove(w.indexOf("boon"));
  ok(w.indexOf("boon")<0);
  ok(w.isNew("boon"));
  // Removing a spell that was acked does not re-badge it, and re-learning is impossible (seen).
  ok(w.ackAllNew()==1);
  ok(w.newCount()==0);
  BarLoadout.Sync rw=w.sync(List.of("mend","heal","boon"));
  ok(rw.newKeys.isEmpty());
  ok(w.newCount()==0);
  // Mark all as seen counts and clears.
  BarLoadout all=BarLoadout.seed(List.of("mend"),3);
  all.sync(List.of("mend","heal","boon"));
  ok(all.newCount()==2);
  ok(all.ackAllNew()==2);
  ok(all.newCount()==0);
  ok(all.ackAllNew()==0);
  // Persistence round trip: unacked survives restore, filtered to seen keys.
  BarLoadout src=BarLoadout.seed(List.of("mend","heal"),3);
  src.sync(List.of("mend","heal","boon"));
  BarLoadout back=BarLoadout.restore(src.storedSlots(),src.seen(),src.newSpells(),3);
  ok(back.isNew("boon"));
  ok(back.storedSlots().equals(src.storedSlots()));
  BarLoadout dirty=BarLoadout.restore(src.storedSlots(),src.seen(),List.of("boon","ghost"),3);
  ok(dirty.newCount()==1); // "ghost" was never seen: dropped
  // The pre-0.3.5 three-arg restore (old loadouts.yml) yields zero badges: the migration rule.
  BarLoadout old=BarLoadout.restore(src.storedSlots(),src.seen(),3);
  ok(old.newCount()==0);
  // Multiple unlocks keep learn order.
  BarLoadout ord=BarLoadout.seed(List.of("mend"),5);
  ord.sync(List.of("mend","zeta","alpha","mid"));
  ok(ord.newSpells().equals(List.of("zeta","alpha","mid")));
  System.out.println("PASS "+n+" bar loadout new-set checks");
 }
}
