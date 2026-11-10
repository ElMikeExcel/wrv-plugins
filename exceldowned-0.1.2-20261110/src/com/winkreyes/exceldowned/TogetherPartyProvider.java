package com.winkreyes.exceldowned;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
 * Read-only ExcelTogether adapter (the ExcelSummons pattern): structural
 * probe for a party manager offering partyOf(UUID), membership reads
 * only, never writes. Lookup failure answers UNKNOWN — never party.
 *
 * Rig/dev fallback: when ExcelTogether is absent AND the config opts in
 * (party.fallback-all-online, default false), every online player counts
 * as party. Production config never enables it; the desk rig does,
 * because the rig has no Together.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class TogetherPartyProvider implements PartyProvider {

    private final Logger log;
    private final boolean fallbackAllOnline;
    private Object manager;
    private Method partyOfMethod;
    private boolean probed;
    private long nextProbeAt;

    public TogetherPartyProvider(Logger log, boolean fallbackAllOnline) {
        this.log = log;
        this.fallbackAllOnline = fallbackAllOnline;
    }

    @Override
    public Set<UUID> partyMembersOf(UUID playerId) {
        if (playerId == null) return Collections.emptySet();
        Object mgr = manager();
        if (mgr == null) {
            if (!fallbackAllOnline) return Collections.emptySet();
            Set<UUID> out = new LinkedHashSet<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getUniqueId().equals(playerId)) out.add(p.getUniqueId());
            }
            return out;
        }
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
            return Collections.emptySet();
        }
    }

    @Override
    public Membership membershipOf(UUID ownerId, UUID playerId) {
        if (ownerId == null || playerId == null) return Membership.UNKNOWN;
        if (ownerId.equals(playerId)) return Membership.PARTY;
        Object mgr = manager();
        if (mgr == null) {
            if (fallbackAllOnline) {
                boolean bothOnline = Bukkit.getPlayer(ownerId) != null && Bukkit.getPlayer(playerId) != null;
                return bothOnline ? Membership.PARTY : Membership.UNKNOWN;
            }
            return Membership.UNKNOWN;
        }
        try {
            Object party = partyOfMethod.invoke(mgr, ownerId);
            if (party == null) return Membership.NOT_PARTY;
            Collection<?> members = membersOf(party);
            if (members.isEmpty()) return Membership.UNKNOWN;
            for (Object member : members) {
                if (playerId.equals(uuidOf(member))) return Membership.PARTY;
            }
            return Membership.NOT_PARTY;
        } catch (Throwable t) {
            return Membership.UNKNOWN;
        }
    }

    private synchronized Object manager() {
        long now = System.currentTimeMillis();
        if (probed && manager == null && now < nextProbeAt) return null;
        if (manager != null) return manager;
        probed = true;
        nextProbeAt = now + 300_000L;
        try {
            Plugin together = Bukkit.getPluginManager().getPlugin("ExcelTogether");
            if (together == null) {
                log.info("ExcelDowned: ExcelTogether not present; party targeting "
                        + (fallbackAllOnline ? "uses the all-online FALLBACK (rig/dev only)."
                                             : "answers UNKNOWN (no revives) until Together is present."));
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
                    log.info("ExcelDowned: ExcelTogether party adapter linked ("
                            + value.getClass().getSimpleName() + ").");
                    return manager;
                }
            }
            log.info("ExcelDowned: ExcelTogether found but no party manager shape matched; party targeting UNKNOWN.");
        } catch (Throwable t) {
            log.info("ExcelDowned: party adapter probe failed quietly; party targeting UNKNOWN.");
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
