/* Excel WRV maintained source; original attribution and license preserved. */ package com.winkreyes.makersmark.core;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransferEvidenceTest {
    private static List<String> slots(String... items) { return Arrays.asList(items); }
    @Test void simpleCursorPickup() {
        assertEquals(3,TransferEvidence.uniqueNewSlot(slots("old",null,null,null),
            slots("old",null,null,"mace"),"mace",1,0,false,true).orElse(-1));
    }
    @Test void shiftClickAndHotbarAndOffhandUseActualDestination() {
        for(int i=0;i<42;i++) {
            String[] b=new String[42],a=new String[42];a[i]="mace";
            assertEquals(i,TransferEvidence.uniqueNewSlot(slots(b),slots(a),"mace",1,0,false,true).orElse(-1));
        }
    }
    @Test void cancelledOrInvalidContextCannotClaim() {
        assertTrue(TransferEvidence.uniqueNewSlot(slots((String)null),slots("mace"),"mace",1,0,true,true).isEmpty());
        assertTrue(TransferEvidence.uniqueNewSlot(slots((String)null),slots("mace"),"mace",1,0,false,false).isEmpty());
    }
    @Test void sourceStillThereOrMultipleMatchingSourcesCannotClaim() {
        for(int before:new int[]{0,2,3})
            assertTrue(TransferEvidence.uniqueNewSlot(slots((String)null),slots("mace"),"mace",before,0,false,true).isEmpty());
        assertTrue(TransferEvidence.uniqueNewSlot(slots((String)null),slots("mace"),"mace",1,1,false,true).isEmpty());
    }
    @Test void stationaryIdenticalItemDoesNotBlockDistinctNewSlot() {
        assertEquals(1,TransferEvidence.uniqueNewSlot(slots("mace",null),slots("mace","mace"),"mace",1,0,false,true).orElse(-1));
        assertTrue(TransferEvidence.uniqueNewSlot(slots("mace",null),slots(null,"mace"),"mace",1,0,false,true).isEmpty());
        assertTrue(TransferEvidence.uniqueNewSlot(slots("mace",null,null),slots(null,"mace","mace"),"mace",1,0,false,true).isEmpty());
    }
    @Test void multipleNewCopiesOrNoDestinationCannotClaim() {
        assertTrue(TransferEvidence.uniqueNewSlot(slots(null,null),slots("mace","mace"),"mace",1,0,false,true).isEmpty());
        assertTrue(TransferEvidence.uniqueNewSlot(slots(null,null),slots(null,null),"mace",1,0,false,true).isEmpty());
    }
    @Test void changedMetadataDoesNotMatchOriginalSnapshot() {
        assertTrue(TransferEvidence.uniqueNewSlot(slots((String)null),slots("mace:enchantschanged"),"mace",1,0,false,true).isEmpty());
    }
    @Test void unrelatedPlayerItemsCanRemainAndSwapIntoChest() {
        assertEquals(0,TransferEvidence.uniqueNewSlot(slots("sword","pick"),slots("mace","pick"),"mace",1,0,false,true).orElse(-1));
    }
    @Test void nullWantedCannotBecomeClaim() {
        assertTrue(TransferEvidence.uniqueNewSlot(slots((String)null),slots((String)null),null,1,0,false,true).isEmpty());
    }
}
