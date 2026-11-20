package com.winkreyes.excelweekendxp;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VanillaXpMultiplierTest {
    private static PlayerExpChangeEvent event(int amount) {
        return new PlayerExpChangeEvent(mock(Player.class), amount);
    }

    @Test
    void weekendMultiplierScalesVanillaXpGains() {
        VanillaXpMultiplier multiplier = new VanillaXpMultiplier();
        assertTrue(multiplier.applyMultiplier(1.2));
        assertEquals(1.2, multiplier.getMultiplier());
        PlayerExpChangeEvent ten = event(10);
        multiplier.onPlayerExpChange(ten);
        assertEquals(12, ten.getAmount());
        PlayerExpChangeEvent five = event(5);
        multiplier.onPlayerExpChange(five);
        assertEquals(6, five.getAmount());
        // 3 x 1.2 = 3.6 rounds to 4: the bonus is never rounded away entirely.
        PlayerExpChangeEvent three = event(3);
        multiplier.onPlayerExpChange(three);
        assertEquals(4, three.getAmount());
    }

    @Test
    void weekendMultiplierNeverReducesAnAmount() {
        VanillaXpMultiplier multiplier = new VanillaXpMultiplier();
        multiplier.applyMultiplier(1.2);
        // 1 x 1.2 = 1.2 rounds to 1: unchanged, never reduced.
        PlayerExpChangeEvent one = event(1);
        multiplier.onPlayerExpChange(one);
        assertEquals(1, one.getAmount());
        // Zero stays zero: no XP is created from nothing.
        PlayerExpChangeEvent zero = event(0);
        multiplier.onPlayerExpChange(zero);
        assertEquals(0, zero.getAmount());
    }

    @Test
    void weekdayMultiplierLeavesAmountsExactlyUntouched() {
        VanillaXpMultiplier multiplier = new VanillaXpMultiplier();
        assertEquals(1.0, multiplier.getMultiplier(), "Default state is the weekday 1.0x");
        for (int amount : new int[] { 1, 3, 7, 10, 100 }) {
            PlayerExpChangeEvent e = event(amount);
            multiplier.onPlayerExpChange(e);
            assertEquals(amount, e.getAmount());
        }
        // Explicit weekday reset after a weekend: back to exact no-op.
        multiplier.applyMultiplier(1.2);
        assertTrue(multiplier.applyMultiplier(1.0));
        assertEquals(1.0, multiplier.getMultiplier());
        PlayerExpChangeEvent e = event(7);
        multiplier.onPlayerExpChange(e);
        assertEquals(7, e.getAmount());
    }

    @Test
    void guardRejectsAnythingBelowOnePointZero() {
        VanillaXpMultiplier multiplier = new VanillaXpMultiplier();
        multiplier.applyMultiplier(1.2);
        for (double invalid : new double[] { 0.0, 0.99, -1.0, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }) {
            assertThrows(IllegalArgumentException.class, () -> multiplier.applyMultiplier(invalid));
        }
        assertEquals(1.2, multiplier.getMultiplier(), "Rejected values must not change the live rate");
    }

    @Test
    void listenerOnlyScalesPlayerExpChangeEvents() {
        // No-double-count (design): this class handles exactly one event type,
        // PlayerExpChangeEvent. mcMMO skill XP flows through McMMOPlayer.addXp
        // (internal profile math) and the patched mcMMO source contains zero
        // references to PlayerExpChangeEvent, so a skill-XP gain never reaches
        // this listener. The WeekendSchedule tests separately prove the
        // mcmmo:xprate command leg still dispatches on the same transitions.
        long handlers = java.util.Arrays.stream(VanillaXpMultiplier.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(org.bukkit.event.EventHandler.class))
                .peek(m -> assertEquals(PlayerExpChangeEvent.class, m.getParameterTypes()[0],
                        "The only handler must take PlayerExpChangeEvent"))
                .count();
        assertEquals(1, handlers, "Exactly one event handler may exist");
    }
}
