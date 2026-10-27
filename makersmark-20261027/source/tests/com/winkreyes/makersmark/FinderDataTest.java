package com.winkreyes.makersmark;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FinderDataTest {
    private Marks.Data found(String id) {
        Marks.Data d=new Marks.Data();d.foundBy=id;d.foundName="Finder "+id;d.foundDate="2026-10-27";return d;
    }
    private Marks.Data crafted(String id) {
        Marks.Data d=new Marks.Data();d.by=id;d.name="Crafter "+id;d.date="2026-10-26";return d;
    }
    @Test void finderIsNotCrafterButNotEmpty() {
        Marks.Data d=found("a");assertTrue(d.found());assertTrue(d.hasProvenance());assertFalse(d.marked());assertFalse(d.empty());
    }
    @Test void statsOnlyWeaponRemainsClaimable() {
        Marks.Data d=new Marks.Data();d.id="existing-item";d.mob=123;d.pvp=4;
        assertFalse(d.hasProvenance());assertFalse(d.empty());
    }
    @Test void firstFinderSurvivesLaterFinderAndCountersKeepMaximum() {
        Marks.Data a=found("a"),b=found("b");a.mob=20;a.pvp=4;a.hits=1;a.id="item-a";b.mob=3;b.pvp=6;b.hits=10;b.id="item-b";
        Marks.Data out=Marks.merge(a,b);
        assertEquals("a",out.foundBy);assertEquals(a.foundName,out.foundName);assertEquals(a.foundDate,out.foundDate);
        assertNull(out.by);assertEquals("item-a",out.id);assertEquals(20,out.mob);assertEquals(6,out.pvp);assertEquals(10,out.hits);
    }
    @Test void craftedThenFoundDoesNotInventBothOrigins() {
        Marks.Data out=Marks.merge(crafted("a"),found("b"));assertEquals("a",out.by);assertNull(out.foundBy);
    }
    @Test void foundThenCraftedKeepsFirstProvenanceAtomically() {
        Marks.Data out=Marks.merge(found("a"),crafted("b"));assertEquals("a",out.foundBy);assertNull(out.by);
    }
    @Test void unsignedFirstInputCanInheritExistingSecondOrigin() {
        Marks.Data out=Marks.merge(new Marks.Data(),found("a"));assertEquals("a",out.foundBy);assertFalse(out.marked());
    }
    @Test void partialOriginIsNotOverwritten() {
        Marks.Data a=new Marks.Data();a.by="existing-uuid";assertTrue(a.hasProvenance());
        Marks.Data out=Marks.merge(a,found("b"));assertEquals(a.by,out.by);assertNull(out.foundBy);
    }
    @Test void repairCarriesOnlyRecordedOwnedLineEvidence() {
        Marks.Data a=found("a"),b=new Marks.Data();a.ownedLines.add("Found by Finder a");b.ownedLines.add("Kills: 5");
        Marks.Data out=Marks.merge(a,b);assertEquals(java.util.Set.of("Found by Finder a","Kills: 5"),out.ownedLines);
        assertFalse(out.ownedLines.contains("Found by unrelated story"));
    }
    @Test void trophiesPreservedAtMaximumWithoutAdding() {
        Marks.Data a=found("a"),b=new Marks.Data();a.slain.put("WITHER",3);b.slain.put("WITHER",2);b.slain.put("WARDEN",1);
        Marks.Data out=Marks.merge(a,b);assertEquals(3,out.slain.get("WITHER"));assertEquals(1,out.slain.get("WARDEN"));
    }
}
