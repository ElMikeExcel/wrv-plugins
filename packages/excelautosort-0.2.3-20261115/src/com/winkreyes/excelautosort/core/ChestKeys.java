package com.winkreyes.excelautosort.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Block-key handling for chest records (SPEC §5.1, §9). A key is the
 * string {@code world:x:y:z}. Pure JDK so the format, the parsing and
 * the canonical ordering are unit-tested off-server.
 */
public final class ChestKeys {

    private ChestKeys() {}

    public record BlockKey(String world, int x, int y, int z) {
        public String format() {
            return world + ":" + x + ":" + y + ":" + z;
        }
    }

    public static String key(String world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }

    /** Parse a key; null when malformed (world names may not contain ':'). */
    public static BlockKey parse(String key) {
        if (key == null) return null;
        String[] parts = key.split(":");
        if (parts.length != 4) return null;
        try {
            return new BlockKey(parts[0],
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean isKey(String s) {
        return parse(s) != null;
    }

    /**
     * Canonical stored order for a record's keys: sorted ascending, so a
     * record means the same thing no matter which half was tapped first.
     */
    public static List<String> canonical(List<String> keys) {
        List<String> out = new ArrayList<>(keys);
        out.sort(String::compareTo);
        return out;
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
