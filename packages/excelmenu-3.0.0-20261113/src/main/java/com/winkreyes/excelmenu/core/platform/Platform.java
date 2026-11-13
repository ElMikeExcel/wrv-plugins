// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.platform;

import java.util.Locale;

/**
 * The two render targets of ExcelMenu 2.0. An entry's {@code platforms:}
 * flag names which of these may render it (default: both). This enum is
 * about the RENDERER, not the device: a player arriving through Geyser
 * is a BEDROCK-rendered player whatever hardware sits behind the
 * connection (DESIGN-2.0 §2.2).
 */
public enum Platform {
    JAVA, BEDROCK;

    /** Parse a schema token ("java" / "bedrock", any case); null when unknown. */
    public static Platform parse(String token) {
        if (token == null) return null;
        return switch (token.trim().toLowerCase(Locale.ROOT)) {
            case "java" -> JAVA;
            case "bedrock" -> BEDROCK;
            default -> null;
        };
    }

    public String token() {
        return name().toLowerCase(Locale.ROOT);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
