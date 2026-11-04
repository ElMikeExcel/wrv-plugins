package com.winkreyesvalley.excelmagiclevel.core;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** ExcelMagicLevel — one place that formats a fraction as a percent string. */
public final class Percents {
    private Percents() {
    }

    /** 0.0125 → "1.25"; 0.095 → "9.5"; 0.2375 → "23.75"; 0 → "0". */
    public static String format(double fraction) {
        BigDecimal pct = BigDecimal.valueOf(fraction)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        return pct.toPlainString();
    }
}
