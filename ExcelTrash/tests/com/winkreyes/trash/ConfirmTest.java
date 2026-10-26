package com.winkreyes.trash;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.winkreyes.trash.Confirm.Result.*;

class ConfirmTest {
    @Test void firstTapArmsSecondDeletes() {
        Confirm c = new Confirm();
        assertEquals(ARMED, c.press(1000, 12));
        assertTrue(c.armed(1000)); assertEquals(12, c.count());
        assertEquals(DELETE, c.press(2000, 12));
        assertFalse(c.armed(2000));
    }
    @Test void timesOutAfterFiveSeconds() {
        Confirm c = new Confirm();
        c.press(0, 5);
        assertTrue(c.armed(Confirm.WINDOW_MS - 1));
        assertFalse(c.armed(Confirm.WINDOW_MS));
        assertEquals(ARMED, c.press(Confirm.WINDOW_MS, 5), "late second tap only re-arms");
    }
    @Test void emptyNeverArms() {
        Confirm c = new Confirm();
        assertEquals(EMPTY, c.press(0, 0));
        assertFalse(c.armed(0));
        c.press(0, 3);
        assertEquals(EMPTY, c.press(10, 0), "emptied while armed: nothing deleted, disarmed");
        assertFalse(c.armed(10));
    }
    @Test void changedCountReArms() {
        Confirm c = new Confirm();
        c.press(0, 3);
        assertEquals(ARMED, c.press(100, 7), "count changed: show the new number first");
        assertEquals(7, c.count());
        assertEquals(DELETE, c.press(200, 7));
    }
    @Test void tokenChangesSoStaleRevertIsSkipped() {
        Confirm c = new Confirm();
        c.press(0, 1); int t = c.token();
        c.press(100, 1); // delete resets
        assertNotEquals(t, c.token());
    }
}
