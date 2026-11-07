package com.winkreyes.excelautosort;

/** Player-friendly material names: COBBLESTONE → "Cobblestone", TNT stays "TNT". */
public final class Names {

    private Names() {}

    public static String friendly(String materialName) {
        if (materialName == null || materialName.isEmpty()) return "";
        String name = materialName;
        int colon = name.indexOf(':');
        if (colon >= 0) name = name.substring(colon + 1); // strip namespace for display
        if (name.length() <= 3 && name.equals(name.toUpperCase())) return name; // TNT etc.
        String[] parts = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!sb.isEmpty()) sb.append(' ');
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
