package com.winkreyes.excelautosort.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Parsing of the incumbent AutoSort's {@code active.txt} for the
 * one-time import (SPEC §10): one UUID per line; blank and malformed
 * lines are skipped, never fatal — the same tolerance the incumbent's
 * own loader and the retired bridge's reader both showed.
 */
public final class ActivesParse {

    private ActivesParse() {}

    public static Set<UUID> parse(List<String> lines) {
        Set<UUID> out = new HashSet<>();
        if (lines == null) return out;
        for (String line : lines) {
            if (line == null) continue;
            String t = line.trim();
            if (t.isEmpty()) continue;
            try {
                out.add(UUID.fromString(t));
            } catch (IllegalArgumentException skipped) {
                // One bad line must not void the whole file.
            }
        }
        return out;
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
