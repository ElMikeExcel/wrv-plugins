package com.winkreyes.exceltogether;

import java.util.Locale;

/** Who counts as the partner for the +25%. PARTY is the default (Mike: party is the same as the bonus group). */
public enum BonusMode {
    PARTY,      // a party member within the radius
    NEARBY,     // 0.1.0 behavior: any other player within the radius
    EITHER;     // either of the two

    public static BonusMode parse(String s) {
        if (s == null) return PARTY;
        try { return valueOf(s.trim().toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException e) { return PARTY; }
    }
    /** Pure decision so it can be tested without a server. */
    public boolean counts(boolean partyMemberNear, boolean anyoneNear) {
        return switch (this) { case PARTY -> partyMemberNear; case NEARBY -> anyoneNear; case EITHER -> partyMemberNear || anyoneNear; };
    }
}
