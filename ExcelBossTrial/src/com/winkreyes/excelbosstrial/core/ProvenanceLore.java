package com.winkreyes.excelbosstrial.core;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * PROVENANCE (ExcelBossTrial 0.2.5, spec 4.6): every copy of
 * named signature gear is stamped at grant time with who earned
 * it and when - a one-of-one should be able to prove it is the
 * one, and under the fairness rule each champion's copy is
 * personal: their name, not the party's.
 *
 * The text is plain player words (no internal terms, no codes):
 * the Bukkit adapter renders these lines in the vanilla lore
 * style (gray, italic). Pure strings + dates so the wording is
 * unit-testable; both loot paths (config-parsed and code
 * defaults) grant through the same adapter code, so the stamp
 * is identical whichever path built the item.
 */
public final class ProvenanceLore {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US);

    private ProvenanceLore() {
    }

    /**
     * The two lore lines for one champion's copy: the earner
     * line naming THAT champion, then the date line.
     */
    public static List<String> lines(String championName, LocalDate date) {
        String name = championName == null || championName.isBlank()
                ? "a champion" : championName;
        return List.of("Earned by " + name, formatDate(date));
    }

    /** The date line, e.g. "October 23, 2026". */
    public static String formatDate(LocalDate date) {
        return (date == null ? LocalDate.now() : date).format(DATE_FORMAT);
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
