package com.winkreyes.exceltogether;

/**
 * ExcelTogether 0.6.0 (P-PARTY-REBUILD-1, SCOPING.md §2.1): where a roster
 * mutation came from. Under the one-door design the roster of record may only
 * change through the door layer (the basic commands and the EM invite popups
 * answering a hub invite), the login rebuild (the persisted roster being
 * restored, plus the one-shot first-boot seed), or an admin action. Every
 * PartyManager mutation carries one of these tags, the tag is persisted with
 * the party in roster.yml (RosterStore), and the admin status surface and the
 * regression tests read it back - so "no roster change from a non-door
 * surface" is provable from data, not argued from code reading.
 * Excel - Wink Reyes Valley.
 */
public enum RosterOrigin {
    /** The door layer: /party + /excelparty commands, compass, Bedrock forms, EM popup answers. */
    DOOR,
    /** Login rebuild: restore from roster.yml, or the one-shot first-boot EM-first seed (Q6). */
    LOGIN_REBUILD,
    /** The /excelparty admin surface (console or excelparty.admin). */
    ADMIN;

    /** Parse a persisted tag; null for anything unrecognised (the store logs and falls back). */
    public static RosterOrigin parse(String s) {
        if (s == null) return null;
        try { return valueOf(s.trim().toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException bad) { return null; }
    }
}
