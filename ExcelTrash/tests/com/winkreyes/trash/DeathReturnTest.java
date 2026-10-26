package com.winkreyes.trash;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DeathReturnTest {
    static final class S { final String n; S(String n) { this.n = n; } @Override public boolean equals(Object o) { return o instanceof S s && s.n.equals(n); } @Override public int hashCode() { return n.hashCode(); } }

    @Test void keepInventoryClearedDrops_givesItemsBack() {
        S a = new S("dirt"), b = new S("stone");
        List<S> drops = new ArrayList<>(); // a keep-inventory plugin cleared them at HIGHEST
        List<S> back = DeathReturn.settle(true, true, drops, List.of(a, b));
        assertEquals(List.of(a, b), back);
        assertTrue(drops.isEmpty());
    }
    @Test void keptButDropsNotCleared_removedFromDropsNoDupe() {
        S a = new S("dirt"); S other = new S("sword");
        List<S> drops = new ArrayList<>(List.of(other, a));
        List<S> back = DeathReturn.settle(true, true, drops, List.of(a));
        assertEquals(List.of(a), back);
        assertEquals(List.of(other), drops);
    }
    @Test void identityOnly_playersOwnEqualItemStays() {
        S mine = new S("dirt"), playersOwn = new S("dirt");
        List<S> drops = new ArrayList<>(List.of(playersOwn, mine));
        DeathReturn.settle(true, true, drops, List.of(mine));
        assertEquals(1, drops.size());
        assertSame(playersOwn, drops.get(0));
    }
    @Test void normalDeath_itemsStayInDrops() {
        S a = new S("dirt");
        List<S> drops = new ArrayList<>(List.of(a));
        assertTrue(DeathReturn.settle(false, true, drops, List.of(a)).isEmpty());
        assertEquals(List.of(a), drops);
    }
    @Test void keepTurnedOffLater_addedToDropsNow() {
        S a = new S("dirt");
        List<S> drops = new ArrayList<>();
        assertTrue(DeathReturn.settle(false, false, drops, List.of(a)).isEmpty());
        assertEquals(List.of(a), drops);
    }
    @Test void nothingInTrash_nothingHappens() {
        List<S> drops = new ArrayList<>();
        assertTrue(DeathReturn.settle(true, true, drops, List.of()).isEmpty());
        assertTrue(DeathReturn.settle(true, true, drops, null).isEmpty());
    }
}
