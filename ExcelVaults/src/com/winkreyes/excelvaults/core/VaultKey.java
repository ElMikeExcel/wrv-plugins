package com.winkreyes.excelvaults.core;

import java.util.Locale;

/** A vault's block position. String form "world;x;y;z" is the data-file key. */
public record VaultKey(String world, int x, int y, int z) {
    public VaultKey {
        if (world == null || world.isEmpty() || world.indexOf(';') >= 0) throw new IllegalArgumentException("bad world: " + world);
    }
    @Override public String toString() { return world + ";" + x + ";" + y + ";" + z; }

    /** Returns null when the text is not a valid key. */
    public static VaultKey parse(String s) {
        if (s == null) return null;
        String[] p = s.split(";");
        if (p.length != 4) return null;
        try { return new VaultKey(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])); }
        catch (RuntimeException e) { return null; }
    }

    /** "world 10 -20 30", used in chat and forms. */
    public String pretty() { return String.format(Locale.ROOT, "%s %d %d %d", world, x, y, z); }
}
