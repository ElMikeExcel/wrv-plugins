package com.winkreyes.excelsummons;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Read-only ExcelTogether adapter: finds Together's party manager on
 * the live plugin instance by structural probe (a field whose type
 * offers partyOf(UUID)), reads membership, and never writes anything.
 * Target-sharing failure answers "no party" — sharing degrades to the
 * caster's own fights. Party-protection lookups (membershipOf) fail
 * the other way: any doubt answers UNKNOWN, which protects the player
 * (0.1.1 fail-soft rule). One quiet log line records whether the probe
 * found its manager; no error storms.
 *
 * A clean phase-2 improvement exists and is noted in the build record:
 * a public getter on ExcelTogether would replace this probe. Until
 * then, this is the house-style adapter (cf. Together's own reflective
 * mcMMO link).
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class TogetherPartyProvider implements PartyProvider {

    private final Logger log;
    private Object manager;          // probed PartyManager instance
    private Method partyOfMethod;    // partyOf(UUID) on the manager
    private boolean probed;
    private long nextProbeAt;

    public TogetherPartyProvider(Logger log) {
        this.log = log;
    }

    @Override
    public Set<UUID> partyMembersOf(UUID playerId) {
        Object mgr = manager();
        if (mgr == null || playerId == null) return Collections.emptySet();
        try {
            Object party = partyOfMethod.invoke(mgr, playerId);
            if (party == null) return Collections.emptySet();
            Set<UUID> out = new LinkedHashSet<>();
            for (Object member : membersOf(party)) {
                UUID id = uuidOf(member);
                if (id != null && !id.equals(playerId)) out.add(id);
            }
            return out;
        } catch (Throwable t) {
            return Collections.emptySet(); // fail-soft, quiet by design
        }
    }

    /**
     * Per-event membership verdict for the 0.1.1 party-protection
     * rules. PARTY / NOT_PARTY are answered only when the lookup is
     * actually determinable: the adapter linked, the owner's party
     * read cleanly, and its member list extracted non-empty (a party
     * object whose members cannot be read is doubt, not an empty
     * party). An owner in no party at all makes every other player a
     * known NOT_PARTY. Everything else is UNKNOWN.
     */
    @Override
    public Membership membershipOf(UUID ownerId, UUID playerId) {
        if (ownerId == null || playerId == null) return Membership.UNKNOWN;
        if (ownerId.equals(playerId)) return Membership.PARTY;
        Object mgr = manager();
        if (mgr == null) return Membership.UNKNOWN;
        try {
            Object party = partyOfMethod.invoke(mgr, ownerId);
            if (party == null) return Membership.NOT_PARTY; // owner has no party
            java.util.Collection<?> members = membersOf(party);
            if (members.isEmpty()) return Membership.UNKNOWN; // unreadable shape
            for (Object member : members) {
                if (playerId.equals(uuidOf(member))) return Membership.PARTY;
            }
            return Membership.NOT_PARTY;
        } catch (Throwable t) {
            return Membership.UNKNOWN; // fail-soft, quiet by design
        }
    }

    private synchronized Object manager() {
        long now = System.currentTimeMillis();
        if (probed && manager == null && now < nextProbeAt) return null;
        if (manager != null) return manager;
        probed = true;
        nextProbeAt = now + 300_000L; // re-probe at most every 5 minutes
        try {
            Plugin together = Bukkit.getPluginManager().getPlugin("ExcelTogether");
            if (together == null) {
                log.info("ExcelSummons: ExcelTogether not present; party target-sharing off.");
                return null;
            }
            for (Field f : allFields(together.getClass())) {
                f.setAccessible(true);
                Object value = f.get(together);
                if (value == null) continue;
                Method m = findPartyOf(value.getClass());
                if (m != null) {
                    manager = value;
                    partyOfMethod = m;
                    log.info("ExcelSummons: ExcelTogether party adapter linked ("
                            + value.getClass().getSimpleName() + ").");
                    return manager;
                }
            }
            log.info("ExcelSummons: ExcelTogether found but no party manager shape matched; "
                    + "party target-sharing off (a Together getter is the phase-2 fix).");
        } catch (Throwable t) {
            log.info("ExcelSummons: party adapter probe failed quietly; target-sharing off.");
        }
        return null;
    }

    private static Method findPartyOf(Class<?> type) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals("partyOf") && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == UUID.class) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        return null;
    }

    private static java.util.List<Field> allFields(Class<?> type) {
        java.util.List<Field> out = new java.util.ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            Collections.addAll(out, c.getDeclaredFields());
        }
        return out;
    }

    /** Pull the member collection out of a Party object by shape. */
    private static Collection<?> membersOf(Object party) {
        for (String name : new String[]{"getMembers", "members", "getPlayers", "players",
                                        "getMemberIds", "memberIds"}) {
            try {
                Method m = party.getClass().getMethod(name);
                Object v = m.invoke(party);
                if (v instanceof Collection) return (Collection<?>) v;
            } catch (Throwable ignored) {
                // try the next shape
            }
        }
        return Collections.emptyList();
    }

    /** A member may be a UUID, an OfflinePlayer, or a house wrapper. */
    private static UUID uuidOf(Object member) {
        if (member == null) return null;
        if (member instanceof UUID) return (UUID) member;
        if (member instanceof org.bukkit.OfflinePlayer) {
            return ((org.bukkit.OfflinePlayer) member).getUniqueId();
        }
        for (String name : new String[]{"getUniqueId", "id", "getId", "uuid"}) {
            try {
                Object v = member.getClass().getMethod(name).invoke(member);
                if (v instanceof UUID) return (UUID) v;
            } catch (Throwable ignored) {
                // try the next shape
            }
        }
        return null;
    }
}
