package com.winkreyes.exceldetails;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 0.2.1 party scoping — the seam half. Asks the two party systems
 * who is in the viewer's party, by reflection, so ExcelDetails keeps
 * NO compile or load-order dependency on either plugin:
 *
 *   ExcelTogether 0.5.3 — plugin instance field `mgr`
 *     (com.winkreyes.exceltogether.PartyManager, source in the
 *     records package exceltogether-0.5.3-20261104): partyOf(UUID)
 *     returns a Party whose public `members` field is the roster.
 *   EliteMobs 10.9.12 — com.magmaguy.elitemobs.parties.PartyManager
 *     (verified by javap against the actual jar): static
 *     getParty(UUID) returns a Party; Party.getMembers() is the
 *     roster (Set<UUID>).
 *
 * SOFT SEAMS, house pattern: each seam resolves lazily at render
 * time and caches its handles; the bind outcome is logged ONCE
 * (BOUND / UNBOUND); any error or absence fails soft to an empty
 * member set for that system — the scope then just misses those
 * members, it never widens (PartyScope's floor is the viewer alone).
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class PartyLookup {

    private final Logger log;

    // EliteMobs seam handles (null until bound).
    private Method emGetParty;
    private Method emGetMembers;
    private boolean emBound;
    private boolean emLogged;

    // ExcelTogether seam handles (null until bound).
    private Object togetherManager;
    private Method togetherPartyOf;
    private Field togetherMembers;
    private boolean togetherBound;
    private boolean togetherLogged;

    private boolean callWarned;

    public PartyLookup(Logger log) { this.log = log; }

    /** {viewer} ∪ Together party ∪ EliteMobs party. Never throws. */
    public Set<UUID> scopeFor(Player viewer) {
        UUID id = viewer.getUniqueId();
        return PartyScope.scopeOf(id, togetherMembers(id), eliteMobsMembers(id));
    }

    // ----- EliteMobs -----

    private Set<UUID> eliteMobsMembers(UUID id) {
        try {
            if (!emBound && !bindEliteMobs()) return Set.of();
            Object party = emGetParty.invoke(null, id);
            if (party == null) return Set.of();
            Object members = emGetMembers.invoke(party);
            return toUuidSet(members);
        } catch (Exception e) {
            warnOnce("EliteMobs", e);
            return Set.of();
        }
    }

    private boolean bindEliteMobs() {
        Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
        if (em == null) { logOnce(true, false, "plugin absent"); return false; }
        try {
            Class<?> mgr = Class.forName("com.magmaguy.elitemobs.parties.PartyManager",
                    true, em.getClass().getClassLoader());
            emGetParty = mgr.getMethod("getParty", UUID.class);
            Class<?> partyClass = emGetParty.getReturnType();
            emGetMembers = partyClass.getMethod("getMembers");
            emBound = true;
            logOnce(true, true, "PartyManager.getParty + Party.getMembers");
            return true;
        } catch (Exception e) {
            logOnce(true, false, e.toString());
            return false;
        }
    }

    // ----- ExcelTogether -----

    private Set<UUID> togetherMembers(UUID id) {
        try {
            if (!togetherBound && !bindTogether()) return Set.of();
            if (togetherManager == null) return Set.of();
            Object party = togetherPartyOf.invoke(togetherManager, id);
            if (party == null) return Set.of();
            Object members = togetherMembers.get(party);
            return toUuidSet(members);
        } catch (Exception e) {
            warnOnce("ExcelTogether", e);
            return Set.of();
        }
    }

    private boolean bindTogether() {
        Plugin tg = Bukkit.getPluginManager().getPlugin("ExcelTogether");
        if (tg == null) { logOnce(false, false, "plugin absent"); return false; }
        try {
            Field mgrField = tg.getClass().getDeclaredField("mgr");
            mgrField.setAccessible(true);
            togetherManager = mgrField.get(tg);
            // Manager not set yet = Together still enabling: retry
            // silently on the next render, no UNBOUND line for a
            // transient state.
            if (togetherManager == null) return false;
            togetherPartyOf = togetherManager.getClass().getMethod("partyOf", UUID.class);
            Class<?> partyClass = togetherPartyOf.getReturnType();
            togetherMembers = partyClass.getField("members");
            togetherBound = true;
            logOnce(false, true, "PartyManager.partyOf + Party.members");
            return true;
        } catch (Exception e) {
            logOnce(false, false, e.toString());
            return false;
        }
    }

    // ----- helpers -----

    private static Set<UUID> toUuidSet(Object members) {
        Set<UUID> out = new HashSet<>();
        if (members instanceof Collection<?> c) {
            for (Object o : c) if (o instanceof UUID u) out.add(u);
        }
        return out;
    }

    /** One line per seam outcome, ever (BOUND on success, UNBOUND on first failure). */
    private void logOnce(boolean em, boolean bound, String detail) {
        if (em) {
            if (emLogged) return;
            emLogged = true;
            log.info(bound
                    ? "ExcelDetails: EliteMobs party seam BOUND (" + detail + ") — display scoping counts EM parties."
                    : "ExcelDetails: EliteMobs party seam UNBOUND (" + detail + ") — display scoping uses ExcelTogether + own rows only.");
        } else {
            if (togetherLogged) return;
            togetherLogged = true;
            log.info(bound
                    ? "ExcelDetails: ExcelTogether party seam BOUND (" + detail + ") — display scoping counts Together parties."
                    : "ExcelDetails: ExcelTogether party seam UNBOUND (" + detail + ") — display scoping uses EliteMobs + own rows only.");
        }
    }

    private void warnOnce(String seam, Exception e) {
        if (callWarned) return;
        callWarned = true;
        log.warning("ExcelDetails: " + seam + " party lookup failed once (" + e
                + ") — that seam reads empty until restart; scoping never widens on error.");
    }
}
