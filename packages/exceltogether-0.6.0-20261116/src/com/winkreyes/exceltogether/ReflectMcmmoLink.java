package com.winkreyes.exceltogether;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * mcMMO link by reflection (no compile-time or hard dependency). Every mcMMO method used is looked up once
 * at start; if any is missing the link turns itself off and ExcelTogether runs alone.
 *
 * 0.6.0 (P-PARTY-REBUILD-1 unit 2 / U2-D1): the link is a one-way mirror writer. The pre-0.6.0
 * invite/accept/cancel surface is deleted - invites live hub-side only, so there is no mcMMO
 * invite state to dangle (audit D5). What remains: party reads (drift detection) and the
 * reconcile primitives, bound to the same mcMMO PartyManager calls PartyAPI delegates to in
 * the fork source (PartyAPI.addToParty -> PartyManager.addToParty(McMMOPlayer, Party);
 * PartyAPI.setPartyLeader -> PartyManager.setPartyLeader(UUID, Party);
 * PartyAPI.removeFromParty -> PartyManager.removeFromParty). Party creation reuses the
 * createParty binding the old link already proved. Whether these bindings resolve against
 * the LIVE jar is a rig question (U2-R1); here they fail soft and self-report through
 * {@link #missing()} exactly as the old link did.
 */
public final class ReflectMcmmoLink implements McmmoLink {
    private final Function<UUID, Player> online;
    private final Function<UUID, OfflinePlayer> offline;
    private final Logger log;
    private final List<String> missing = new ArrayList<>();

    private Class<?> cMcMMO, cUser, cMmoPlayer, cParty, cPartyMgr;
    private Method mPartyEnabled, mPartyCfg, mPartyMgr, mUserGet, mGenCfg;
    private Method mpGetParty;
    private Method pName, pMembers, pLeader, leaderId;
    private Method mgrGetByName, mgrGetByPlayer, mgrGetPlayerParty, mgrCreate, mgrAddToParty, mgrSetLeader, mgrRemoveMmo, mgrRemoveOff;

    /** players we mirror in an mcMMO party: id -> mcMMO party name. Only these are ever removed by us, and only from that party. */
    private final Map<UUID, String> linked = new HashMap<>();
    /** mirror parties we created: leader id -> party name (Q5 naming bookkeeping, kept from the old link). */
    private final Map<UUID, String> created = new HashMap<>();

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
        mUserGet = m(cUser, "getPlayer", Player.class);
        mpGetParty = m(cMmoPlayer, "getParty");
        pName = m(cParty, "getName");
        pMembers = m(cParty, "getMembers");
        pLeader = m(cParty, "getLeader");
        leaderId = m(cLeader, "getUniqueId");
        mgrGetByName = m(cPartyMgr, "getParty", String.class);
        mgrGetByPlayer = m(cPartyMgr, "getParty", Player.class);
        mgrGetPlayerParty = m(cPartyMgr, "getPlayerParty", String.class, UUID.class);
        mgrCreate = m(cPartyMgr, "createParty", cMmoPlayer, String.class, String.class);
        mgrAddToParty = m(cPartyMgr, "addToParty", cMmoPlayer, cParty);
        mgrSetLeader = m(cPartyMgr, "setPartyLeader", UUID.class, cParty);
        mgrRemoveMmo = m(cPartyMgr, "removeFromParty", cMmoPlayer);
        mgrRemoveOff = m(cPartyMgr, "removeFromParty", OfflinePlayer.class, cParty);
    }

    @Override public boolean available() { return missing.isEmpty(); }

    @Override public McParty partyOf(UUID id) {
        if (!available() || id == null) return McParty.UNKNOWN;
        try {
            if (!partyEnabled()) return McParty.UNKNOWN;
            Player p = online.apply(id);
            Object party;
            if (p != null) {
                Object mmo = mmo(p);
                if (mmo == null) return McParty.UNKNOWN;         // profile not loaded yet: do not guess
                party = partyOfMmo(mmo);
            } else {
                OfflinePlayer op = offline.apply(id);
                if (op == null || op.getName() == null) return McParty.UNKNOWN;
                party = mgrGetPlayerParty.invoke(mgr(), op.getName(), id);
            }
            if (party == null) return McParty.NONE;
            return new McParty(nameOf(party), leaderOf(party), new LinkedHashMap<>(membersOf(party)));
        } catch (Throwable t) { warn("read party", t); return McParty.UNKNOWN; }
    }
    @Override public McParty partyByName(String name) {
        if (!available() || name == null) return McParty.UNKNOWN;
        try {
            Object party = mgrGetByName.invoke(mgr(), name);
            return party == null ? McParty.NONE : new McParty(nameOf(party), leaderOf(party), new LinkedHashMap<>(membersOf(party)));
        } catch (Throwable t) { warn("read party by name", t); return McParty.UNKNOWN; }
    }

    // ---------- one-way reconcile primitives ----------
    @Override public String createMirrorParty(UUID leader, String hint, String fallbackHint) {
        if (!available() || leader == null) return null;
        try {
            if (!partyEnabled()) return null;
            Player p = online.apply(leader);
            Object mmo = mmo(p);
            if (mmo == null) return null;                        // profile not loaded: caller retries on a later poll
            Object mgr = mgr();
            Object existing = partyOfMmo(mmo);
            if (existing != null) mgrRemoveMmo.invoke(mgr, mmo); // the mirror is a fresh party; the leader leaves whatever mcMMO held
            String want = clean(hint);
            String name;
            if (mgrGetByName.invoke(mgr, want) == null) name = want;
            else {
                String fb = clean(fallbackHint);                 // Q5: hub-id fallback name on collision
                name = mgrGetByName.invoke(mgr, fb) == null ? fb : freeName(mgr, fb);
            }
            mgrCreate.invoke(mgr, mmo, name, null);
            Object party = partyOfMmo(mmo);
            if (party == null || !nameOf(party).equals(name)) return null;
            created.put(leader, name);
            linked.put(leader, name);
            return name;
        } catch (Throwable t) { warn("create mirror party", t); return null; }
    }

    @Override public boolean addToParty(UUID id, String partyName) {
        if (!available() || id == null || partyName == null) return false;
        try {
            if (!partyEnabled()) return false;
            Player p = online.apply(id);
            Object mmo = mmo(p);
            if (mmo == null) return false;                       // offline or profile not loaded: a later poll retries
            Object mgr = mgr();
            Object party = mgrGetByName.invoke(mgr, partyName);
            if (party == null) return false;
            Object cur = partyOfMmo(mmo);
            if (cur != null && nameOf(cur).equals(partyName)) { linked.put(id, partyName); return true; }
            if (cur != null) mgrRemoveMmo.invoke(mgr, mmo);      // drift: they were somewhere else; the roster wins
            mgrAddToParty.invoke(mgr, mmo, party);
            Object now = partyOfMmo(mmo);
            if (now == null || !nameOf(now).equals(partyName)) return false;
            linked.put(id, partyName);
            return true;
        } catch (Throwable t) { warn("add to mirror party", t); return false; }
    }

    @Override public void removeFromParty(UUID id, String partyName) {
        if (!available() || id == null || partyName == null) return;
        try {
            Object mgr = mgr();
            Player p = online.apply(id);
            Object mmo = mmo(p);
            if (mmo != null) {
                Object party = partyOfMmo(mmo);
                if (party != null && nameOf(party).equals(partyName)) mgrRemoveMmo.invoke(mgr, mmo);
            } else {
                OfflinePlayer op = offline.apply(id);
                Object party = mgrGetByName.invoke(mgr, partyName);
                if (op != null && party != null && membersOf(party).containsKey(id)) mgrRemoveOff.invoke(mgr, op, party);
            }
            if (partyName.equals(linked.get(id))) linked.remove(id);
            Object party = mgrGetByName.invoke(mgr, partyName);
            if (party == null) created.values().removeIf(partyName::equals);   // the mirror party is gone; forget we made it
        } catch (Throwable t) { warn("remove from mirror party", t); }
    }

    @Override public boolean setPartyLeader(String partyName, UUID leader, String leaderName) {
        if (!available() || partyName == null || leader == null) return false;
        try {
            if (!partyEnabled()) return false;
            Object mgr = mgr();
            Object party = mgrGetByName.invoke(mgr, partyName);
            if (party == null) return false;
            if (leader.equals(leaderOf(party))) return true;
            if (!membersOf(party).containsKey(leader)) return false;
            mgrSetLeader.invoke(mgr, leader, party);
            return leader.equals(leaderOf(party));
        } catch (Throwable t) { warn("set mirror party leader", t); return false; }
    }

    // ---------- raw mcMMO access ----------
    private Object mcmmo() throws Exception { return cMcMMO.getField("p").get(null); }
    private Object mgr() throws Exception { return mPartyMgr.invoke(mcmmo()); }
    private boolean partyEnabled() throws Exception { return (Boolean) mPartyEnabled.invoke(mPartyCfg.invoke(mcmmo())); }
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

    private void warn(String what, Throwable t) {
        Throwable c = t instanceof java.lang.reflect.InvocationTargetException && t.getCause() != null ? t.getCause() : t;
        log.warning("mcMMO link " + what + " failed (ExcelTogether party is unaffected): " + c);
    }
}
