package com.winkreyes.exceltogether;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * mcMMO link by reflection (no compile-time or hard dependency). Every mcMMO method used is looked up once
 * at start; if any is missing the link turns itself off and ExcelTogether runs alone. Uses mcMMO's own
 * PartyManager so mcMMO's size limit, party list and messages stay consistent.
 */
public final class ReflectMcmmoLink implements McmmoLink {
    private final Function<UUID, Player> online;
    private final Function<UUID, OfflinePlayer> offline;
    private final Logger log;
    private final List<String> missing = new ArrayList<>();

    private Class<?> cMcMMO, cUser, cMmoPlayer, cParty, cPartyMgr;
    private Method mPartyEnabled, mPartyCfg, mPartyMgr, mUserGet, mMaxSize, mGenCfg;
    private Method mpGetParty, mpSetInvite, mpGetInvite, mpRemoveInvite;
    private Method pName, pMembers, pLeader, pLocked, leaderId;
    private Method mgrGetByName, mgrGetByPlayer, mgrGetPlayerParty, mgrCreate, mgrJoin, mgrFull, mgrCanInvite, mgrRemoveMmo, mgrRemoveOff;

    /** players we put in an mcMMO party: id -> mcMMO party name. Only these are ever removed by us. */
    private final Map<UUID, String> linked = new HashMap<>();
    /** inviters whose mcMMO party we created: id -> party name. */
    private final Map<UUID, String> created = new HashMap<>();
    private final Map<UUID, Set<UUID>> pending = new HashMap<>();

    public ReflectMcmmoLink(ClassLoader cl, Function<UUID, Player> online, Function<UUID, OfflinePlayer> offline, Logger log) {
        this.online = online; this.offline = offline; this.log = log;
        resolve(cl);
    }

    /** Names of mcMMO classes or methods this link needs but cannot find. Empty = the link can work. */
    public static List<String> probe(ClassLoader cl) {
        return new ReflectMcmmoLink(cl, id -> null, id -> null, Logger.getLogger("probe")).missing;
    }
    public List<String> missing() { return Collections.unmodifiableList(missing); }

    private Class<?> cls(ClassLoader cl, String n) {
        try { return Class.forName(n, false, cl); } catch (Throwable t) { missing.add("class " + n); return null; }
    }
    private Method m(Class<?> c, String n, Class<?>... args) {
        if (c == null) return null;
        try { return c.getMethod(n, args); } catch (Throwable t) { missing.add(c.getSimpleName() + "." + n); return null; }
    }
    private void resolve(ClassLoader cl) {
        cMcMMO = cls(cl, "com.gmail.nossr50.mcMMO");
        cUser = cls(cl, "com.gmail.nossr50.util.player.UserManager");
        cMmoPlayer = cls(cl, "com.gmail.nossr50.datatypes.player.McMMOPlayer");
        cParty = cls(cl, "com.gmail.nossr50.datatypes.party.Party");
        cPartyMgr = cls(cl, "com.gmail.nossr50.party.PartyManager");
        Class<?> cLeader = cls(cl, "com.gmail.nossr50.datatypes.party.PartyLeader");
        mPartyCfg = m(cMcMMO, "getPartyConfig");
        mPartyMgr = m(cMcMMO, "getPartyManager");
        mGenCfg = m(cMcMMO, "getGeneralConfig");
        if (cMcMMO != null) { try { cMcMMO.getField("p"); } catch (Throwable t) { missing.add("mcMMO.p"); } }
        if (mPartyCfg != null) mPartyEnabled = m(mPartyCfg.getReturnType(), "isPartyEnabled");
        if (mGenCfg != null) mMaxSize = m(mGenCfg.getReturnType(), "getPartyMaxSize");
        mUserGet = m(cUser, "getPlayer", Player.class);
        mpGetParty = m(cMmoPlayer, "getParty");
        mpSetInvite = m(cMmoPlayer, "setPartyInvite", cParty);
        mpGetInvite = m(cMmoPlayer, "getPartyInvite");
        mpRemoveInvite = m(cMmoPlayer, "removePartyInvite");
        pName = m(cParty, "getName");
        pMembers = m(cParty, "getMembers");
        pLeader = m(cParty, "getLeader");
        pLocked = m(cParty, "isLocked");
        leaderId = m(cLeader, "getUniqueId");
        mgrGetByName = m(cPartyMgr, "getParty", String.class);
        mgrGetByPlayer = m(cPartyMgr, "getParty", Player.class);
        mgrGetPlayerParty = m(cPartyMgr, "getPlayerParty", String.class, UUID.class);
        mgrCreate = m(cPartyMgr, "createParty", cMmoPlayer, String.class, String.class);
        mgrJoin = m(cPartyMgr, "joinInvitedParty", cMmoPlayer);
        mgrFull = m(cPartyMgr, "isPartyFull", Player.class, cParty);
        mgrCanInvite = m(cPartyMgr, "canInvite", cMmoPlayer);
        mgrRemoveMmo = m(cPartyMgr, "removeFromParty", cMmoPlayer);
        mgrRemoveOff = m(cPartyMgr, "removeFromParty", OfflinePlayer.class, cParty);
    }

    @Override public boolean available() { return missing.isEmpty(); }

    // ---------- raw mcMMO access ----------
    private Object mcmmo() throws Exception { return cMcMMO.getField("p").get(null); }
    private Object mgr() throws Exception { return mPartyMgr.invoke(mcmmo()); }
    private boolean partyEnabled() throws Exception { return (Boolean) mPartyEnabled.invoke(mPartyCfg.invoke(mcmmo())); }
    private int maxSize() throws Exception { return (Integer) mMaxSize.invoke(mGenCfg.invoke(mcmmo())); }
    private Object mmo(Player p) throws Exception { return p == null ? null : mUserGet.invoke(null, p); }
    private Object partyOfMmo(Object mmo) throws Exception { return mpGetParty.invoke(mmo); }
    private String nameOf(Object party) throws Exception { return (String) pName.invoke(party); }
    @SuppressWarnings("unchecked") private Map<UUID, String> membersOf(Object party) throws Exception { return (Map<UUID, String>) pMembers.invoke(party); }
    private UUID leaderOf(Object party) throws Exception { return (UUID) leaderId.invoke(pLeader.invoke(party)); }

    private static String clean(String hint) {
        StringBuilder b = new StringBuilder();
        if (hint != null) for (char c : hint.toCharArray()) if (Character.isLetterOrDigit(c) || c == '_') b.append(c);
        return b.length() == 0 ? "Family" : b.length() > 16 ? b.substring(0, 16) : b.toString();
    }
    private String freeName(Object mgr, String hint) throws Exception {
        String base = clean(hint); String n = base; int k = 2;
        while (mgrGetByName.invoke(mgr, n) != null && k < 100) n = base + k++;
        return n;
    }

    /** The inviter's mcMMO party, creating one if they have none. Returns null with why[0] set if it cannot be done. */
    private Object ensureParty(Object mgr, Object mmoInviter, Player inviter, String hint, Mirror[] why) throws Exception {
        Object party = partyOfMmo(mmoInviter);
        if (party != null) {
            if (!(Boolean) mgrCanInvite.invoke(mgr, mmoInviter)) { why[0] = Mirror.LOCKED; return null; }
            return party;
        }
        mgrCreate.invoke(mgr, mmoInviter, freeName(mgr, hint), null);
        party = partyOfMmo(mmoInviter);
        if (party == null) { why[0] = Mirror.ERROR; return null; }
        created.put(inviter.getUniqueId(), nameOf(party));
        linked.put(inviter.getUniqueId(), nameOf(party));
        return party;
    }

    @Override public Mirror invite(UUID inviterId, UUID targetId, String hint) {
        if (!available()) return Mirror.ABSENT;
        try {
            if (!partyEnabled()) return Mirror.ABSENT;
            Player inv = online.apply(inviterId), tgt = online.apply(targetId);
            Object mi = mmo(inv), mt = mmo(tgt);
            if (mi == null || mt == null) return Mirror.NOT_READY;
            Object mgr = mgr();
            Object theirs = partyOfMmo(mt), mine = partyOfMmo(mi);
            if (theirs != null) return mine != null && nameOf(mine).equals(nameOf(theirs)) ? Mirror.OK : Mirror.TARGET_IN_OTHER_PARTY;
            Mirror[] why = {Mirror.ERROR};
            boolean hadParty = mine != null;
            Object party = ensureParty(mgr, mi, inv, hint, why);
            if (party == null) return why[0];
            if ((Boolean) mgrFull.invoke(mgr, tgt, party)) { if (!hadParty) cleanupSolo(inviterId); return Mirror.FULL; }
            mpSetInvite.invoke(mt, party);
            pending.computeIfAbsent(inviterId, k -> new HashSet<>()).add(targetId);
            return Mirror.OK;
        } catch (Throwable t) { warn("invite", t); return Mirror.ERROR; }
    }

    @Override public Mirror accept(UUID targetId, UUID inviterId, String hint) {
        if (!available()) return Mirror.ABSENT;
        try {
            if (!partyEnabled()) return Mirror.ABSENT;
            Player inv = online.apply(inviterId), tgt = online.apply(targetId);
            Object mi = mmo(inv), mt = mmo(tgt);
            if (mi == null || mt == null) return Mirror.NOT_READY;
            Object mgr = mgr();
            Set<UUID> pend = pending.get(inviterId); if (pend != null) pend.remove(targetId);
            Object theirs = partyOfMmo(mt);
            Object mine = partyOfMmo(mi);
            if (theirs != null) return mine != null && nameOf(mine).equals(nameOf(theirs)) ? Mirror.OK : Mirror.TARGET_IN_OTHER_PARTY;
            Mirror[] why = {Mirror.ERROR};
            Object party = ensureParty(mgr, mi, inv, hint, why);     // normally exists already; recreated if someone disbanded it
            if (party == null) return why[0];
            int max = maxSize();
            if ((Boolean) mgrFull.invoke(mgr, tgt, party) || (max > 0 && membersOf(party).size() >= max && !bypass(tgt))) { dropInviteTo(mt, party); return Mirror.FULL; }
            mpSetInvite.invoke(mt, party);
            mgrJoin.invoke(mgr, mt);
            Object now = partyOfMmo(mt);
            if (now == null || !nameOf(now).equals(nameOf(party))) { mpRemoveInvite.invoke(mt); return Mirror.FULL; }
            linked.put(targetId, nameOf(party));
            return Mirror.OK;
        } catch (Throwable t) { warn("accept", t); return Mirror.ERROR; }
    }
    /** Remove the invite we set earlier when it can no longer be used. */
    private void dropInviteTo(Object mt, Object party) throws Exception {
        Object inv = mpGetInvite.invoke(mt);
        if (inv != null && nameOf(inv).equals(nameOf(party))) mpRemoveInvite.invoke(mt);
    }
    private boolean bypass(Player p) { try { return p.hasPermission("mcmmo.bypass.partylimit"); } catch (Throwable t) { return false; } }

    @Override public void cancel(UUID targetId, UUID inviterId) {
        if (!available()) return;
        try {
            Set<UUID> pend = pending.get(inviterId);
            if (pend != null) { pend.remove(targetId); if (pend.isEmpty()) pending.remove(inviterId); }
            Player tgt = online.apply(targetId);
            Object mt = mmo(tgt);
            if (mt != null) {
                Object inv = mpGetInvite.invoke(mt);
                Object mi = mmo(online.apply(inviterId));
                Object theirParty = mi == null ? null : partyOfMmo(mi);
                boolean fromInviter = inv != null && ((theirParty != null && nameOf(theirParty).equals(nameOf(inv))) || inviterId.equals(createdLeader(inv)));
                if (fromInviter) mpRemoveInvite.invoke(mt);
            }
        } catch (Throwable t) { warn("cancel", t); }
    }
    private UUID createdLeader(Object party) { try { return leaderOf(party); } catch (Throwable t) { return null; } }

    @Override public void leave(UUID id) {
        if (!available()) return;
        String name = linked.remove(id);
        if (name == null) return;
        try {
            Object mgr = mgr();
            Player p = online.apply(id);
            Object mmo = mmo(p);
            if (mmo != null) {
                Object party = partyOfMmo(mmo);
                if (party != null && nameOf(party).equals(name)) mgrRemoveMmo.invoke(mgr, mmo);
            } else {
                OfflinePlayer op = offline.apply(id);
                Object party = mgrGetByName.invoke(mgr, name);
                if (op != null && party != null && membersOf(party).containsKey(id)) mgrRemoveOff.invoke(mgr, op, party);
            }
            created.remove(id);
        } catch (Throwable t) { warn("leave", t); }
    }

    @Override public void cleanup(Predicate<UUID> hasOpenInvites) {
        if (!available() || created.isEmpty()) return;
        for (UUID id : new ArrayList<>(created.keySet())) {
            try { if (!hasOpenInvites.test(id)) cleanupSolo(id); } catch (Throwable t) { warn("cleanup", t); }
        }
    }

    /** Remove a creator from the mcMMO party we made for them if nobody else is in it. */
    private void cleanupSolo(UUID id) throws Exception {
        String name = created.get(id);
        if (name == null) return;
        Object mgr = mgr();
        Object party = mgrGetByName.invoke(mgr, name);
        if (party == null) { created.remove(id); linked.remove(id); return; }
        Map<UUID, String> mem = membersOf(party);
        if (mem.size() == 1 && mem.containsKey(id)) { leave(id); }
    }

    private void warn(String what, Throwable t) {
        Throwable c = t instanceof java.lang.reflect.InvocationTargetException && t.getCause() != null ? t.getCause() : t;
        log.warning("mcMMO link " + what + " failed (ExcelTogether party is unaffected): " + c);
    }
}
