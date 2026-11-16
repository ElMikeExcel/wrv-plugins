package com.winkreyes.exceltogether;

import com.winkreyesvalley.excelmagiclevel.adapter.together.PartyLookup;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * DESIGN §11B case B13 (the unit half): the PartyLookup provider over PartyManager — solo players
 * get {self}, a party returns its full member set including offline-grace members, and the result
 * is a copy the caller cannot mutate the roster through. (Service registration itself needs a
 * running server and is proven on the rig / at install boot-verify.)
 * Run: java -cp <classes> com.winkreyes.exceltogether.PartyLookupTest
 */
public class PartyLookupTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }

    public static void main(String[] args) {
        PartyFeatureTest.Rig r = new PartyFeatureTest.Rig(5);
        PartyLookup lookup = new MagicLevelPartyLookup(r.mgr);
        var a = r.java("Look1"); var b = r.java("Look2"); var c = r.java("Look3");
        UUID stranger = UUID.randomUUID();

        check("solo player gets {self}", lookup.partyMembers(a.id).equals(Set.of(a.id)));
        check("unknown player gets {self}", lookup.partyMembers(stranger).equals(Set.of(stranger)));

        r.mgr.adopt(b.id, a.id); r.mgr.adopt(c.id, a.id);
        Set<UUID> want = new HashSet<>(Set.of(a.id, b.id, c.id));
        check("in-party returns the full set", lookup.partyMembers(a.id).equals(want));
        check("same answer from any member", lookup.partyMembers(c.id).equals(want));

        // an offline-grace member is still a member (consumers gate on online themselves)
        r.env.on.remove(c.id); r.mgr.onQuit(c.id, r.env.t);
        check("offline-grace member is included", lookup.partyMembers(a.id).equals(want));

        // the returned set is a copy: clearing it does not touch the roster
        Set<UUID> got = lookup.partyMembers(a.id);
        got.clear();
        check("result is a defensive copy", lookup.partyMembers(a.id).equals(want));

        System.out.println("PartyLookupTest: " + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
