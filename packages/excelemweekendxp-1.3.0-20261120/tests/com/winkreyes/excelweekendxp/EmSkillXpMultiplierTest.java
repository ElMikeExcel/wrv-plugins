package com.winkreyes.excelweekendxp;

import com.magmaguy.elitemobs.api.EliteSkillXpGainEvent;
import com.magmaguy.elitemobs.mobconstructor.EliteEntity;
import com.magmaguy.elitemobs.skills.SkillType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmSkillXpMultiplierTest {
    private static EliteSkillXpGainEvent event(long xp) {
        return new EliteSkillXpGainEvent(mock(Player.class), SkillType.SWORDS,
                mock(EliteEntity.class), xp);
    }

    @Test
    void weekendMultiplierScalesEmSkillXpGains() {
        EmSkillXpMultiplier multiplier = new EmSkillXpMultiplier();
        assertTrue(multiplier.applyMultiplier(1.2));
        assertEquals(1.2, multiplier.getMultiplier());
        EliteSkillXpGainEvent ten = event(10);
        multiplier.onEliteSkillXpGain(ten);
        assertEquals(12, ten.getXp());
        assertEquals(10, ten.getOriginalXp(), "Scaling must not rewrite the original amount");
        EliteSkillXpGainEvent five = event(5);
        multiplier.onEliteSkillXpGain(five);
        assertEquals(6, five.getXp());
        // 3 x 1.2 = 3.6 rounds to 4: the bonus is never rounded away entirely.
        EliteSkillXpGainEvent three = event(3);
        multiplier.onEliteSkillXpGain(three);
        assertEquals(4, three.getXp());
        // Larger combat gains scale by the same rate.
        EliteSkillXpGainEvent hundred = event(100);
        multiplier.onEliteSkillXpGain(hundred);
        assertEquals(120, hundred.getXp());
    }

    @Test
    void weekendMultiplierNeverReducesAnAmount() {
        EmSkillXpMultiplier multiplier = new EmSkillXpMultiplier();
        multiplier.applyMultiplier(1.2);
        // 1 x 1.2 = 1.2 rounds to 1: unchanged, never reduced.
        EliteSkillXpGainEvent one = event(1);
        multiplier.onEliteSkillXpGain(one);
        assertEquals(1, one.getXp());
        // Zero stays zero: no XP is created from nothing.
        EliteSkillXpGainEvent zero = event(0);
        multiplier.onEliteSkillXpGain(zero);
        assertEquals(0, zero.getXp());
        // Negative gains cannot exist in this event at all: the EliteMobs
        // constructor itself rejects them (IllegalArgumentException: "Skill
        // XP must be non-negative", thrown by setXp from the constructor —
        // verified against the live 10.9.14 jar). The handler's xp <= 0
        // guard therefore only ever has to cover the zero case above.
        assertThrows(IllegalArgumentException.class, () -> event(-5));
    }

    @Test
    void weekdayMultiplierLeavesAmountsExactlyUntouched() {
        EmSkillXpMultiplier multiplier = new EmSkillXpMultiplier();
        assertEquals(1.0, multiplier.getMultiplier(), "Default state is the weekday 1.0x");
        for (long amount : new long[] { 1, 3, 7, 10, 100, 1000 }) {
            EliteSkillXpGainEvent e = event(amount);
            multiplier.onEliteSkillXpGain(e);
            assertEquals(amount, e.getXp());
        }
        // Explicit weekday reset after a weekend: back to exact no-op.
        multiplier.applyMultiplier(1.2);
        assertTrue(multiplier.applyMultiplier(1.0));
        assertEquals(1.0, multiplier.getMultiplier());
        EliteSkillXpGainEvent e = event(7);
        multiplier.onEliteSkillXpGain(e);
        assertEquals(7, e.getXp());
    }

    @Test
    void guardRejectsAnythingBelowOnePointZero() {
        EmSkillXpMultiplier multiplier = new EmSkillXpMultiplier();
        multiplier.applyMultiplier(1.2);
        for (double invalid : new double[] { 0.0, 0.99, -1.0, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }) {
            assertThrows(IllegalArgumentException.class, () -> multiplier.applyMultiplier(invalid));
        }
        assertEquals(1.2, multiplier.getMultiplier(), "Rejected values must not change the live rate");
    }

    @Test
    void listenerOnlyScalesEliteSkillXpGainEvents() {
        // Scope (D155): this class handles exactly one event type,
        // EliteSkillXpGainEvent, at HIGHEST with ignoreCancelled so a gain
        // cancelled elsewhere is never scaled. Programmatic grants through
        // PlayerData.addSkillXP (ExcelQuestXP payouts, quest backpay) never
        // fire this event, so exact-amount payouts never reach this listener.
        long handlers = java.util.Arrays.stream(EmSkillXpMultiplier.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(org.bukkit.event.EventHandler.class))
                .peek(m -> {
                    assertEquals(EliteSkillXpGainEvent.class, m.getParameterTypes()[0],
                            "The only handler must take EliteSkillXpGainEvent");
                    org.bukkit.event.EventHandler handler =
                            m.getAnnotation(org.bukkit.event.EventHandler.class);
                    assertEquals(EventPriority.HIGHEST, handler.priority());
                    assertTrue(handler.ignoreCancelled(), "Cancelled gains must stay cancelled");
                })
                .count();
        assertEquals(1, handlers, "Exactly one event handler may exist");
    }
}
