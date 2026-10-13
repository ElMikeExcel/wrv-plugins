// SPDX-License-Identifier: MIT
package com.winkreyes.shops.checkout;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MoneyTest {
    @Test void decimalAmountsStayExact() {
        assertEquals(1, Money.parse("0.01"));
        assertEquals(110, Money.parse("1.1"));
        assertEquals(12345678, Money.parse("123456.78"));
        assertEquals(Money.MAX_MINOR_UNITS, Money.parse("100000000.00"));
        assertEquals(999, Money.multiply(333, 3));
        assertEquals(300, Money.add(100, 200));
        assertEquals("0.01", Money.format(1));
        assertEquals("123456.78", Money.format(12345678));
    }

    @ParameterizedTest @ValueSource(strings = {"", " ", "0", "0.00", "-1", "+1", "01", ".50", "1.",
            "1.001", "1e3", "NaN", "Infinity", "1,000", "100000000.01", "9999999999999999999", " 1", "1 "})
    void rejectsInvalidPrices(String value) { assertThrows(IllegalArgumentException.class, () -> Money.parse(value)); }

    @Test void rejectsNullAndOverflowAndBadQuantities() {
        assertThrows(IllegalArgumentException.class, () -> Money.parse(null));
        for (long value : new long[] {Long.MIN_VALUE, -1, 0, Long.MAX_VALUE, Money.MAX_MINOR_UNITS + 1}) {
            assertThrows(IllegalArgumentException.class, () -> Money.requirePositive(value));
            assertThrows(IllegalArgumentException.class, () -> Money.toVault(value));
            assertThrows(IllegalArgumentException.class, () -> Money.format(value));
        }
        assertThrows(IllegalArgumentException.class, () -> Money.multiply(Long.MAX_VALUE, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> Money.multiply(Money.MAX_MINOR_UNITS, 2));
        assertThrows(IllegalArgumentException.class, () -> Money.add(Money.MAX_MINOR_UNITS, 1));
        assertThrows(IllegalArgumentException.class, () -> Money.add(1, -1));
        for (int quantity : new int[] {Integer.MIN_VALUE, -1, 0, 2305, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> Money.multiply(100, quantity));
        }
        assertEquals(2304, Money.multiply(1, 2304));
    }

    @Test void vaultBoundaryRoundTripsAllRelevantMagnitudeBands() {
        for (long cents : new long[] {1, 2, 3, 7, 29, 99, 101, 10001, 999999999, Money.MAX_MINOR_UNITS - 1, Money.MAX_MINOR_UNITS}) {
            assertEquals(cents, BigDecimal.valueOf(Money.toVault(cents)).movePointRight(2).longValueExact());
        }
    }
}
