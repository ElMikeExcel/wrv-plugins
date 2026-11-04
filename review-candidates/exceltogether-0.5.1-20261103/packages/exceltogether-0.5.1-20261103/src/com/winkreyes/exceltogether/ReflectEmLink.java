package com.winkreyes.exceltogether;

import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * EliteMobs mesh leg by reflection (no compile-time or class-load-time reference to EliteMobs types
 * anywhere else, so a missing or shifted EliteMobs can never break plugin enable). Uses only
 * {@code com.magmaguy.elitemobs.parties.PartyManager}'s public statics - no EliteMobs fork, no
 * config edits, EM stays stock. Every method is looked up once at start; if any is missing the link
 * turns itself off and ExcelTogether runs its pre-0.5.0 two-way behaviour.
 *
 * R-1 (desk-proven from the EM 10.9.12 bytecode, see SOURCE.md): the boolean parameter of EM's
 * create/invite/accept/leave is a NOTIFY flag - EM sends its own result message only when it is
 * true. Adapter-driven calls pass {@link #EM_QUIET} (false): Together already told the player in
 * its own words. Actions a player takes at the EM door never pass through this adapter at all, so
 * EM's own messages there are untouched.
 */
public final class ReflectEmLink implements EmLink {
    /** Passed as the notify flag on every adapter-driven EM call: EM stays quiet, Together speaks. */
    static final boolean EM_QUIET = false;
    /** Driven invites live exactly as long as EM's own (Party.yml inviteTimeoutSeconds: 60). */
    static final long INVITE_LEDGER_MS = 60_000L;
    /** After this many reflective failures in a row the leg latches off for the session (one warning). */
    static final int FAILURE_LATCH = 5;

    enum Op { CREATE, INVITE, ACCEPT, LEAVE }

    private final Function<UUID, Player> online;
    private final Logger log;
    private final List<String> missing = new ArrayList<>();

    private Class<?> cPartyMgr, cParty;
    private Method mGetParties, mGetParty, mCreate, mInvite, mAccept, mLeave;
    private Method pGetId, pGetLeader, pDisplayOrder;
    private Method mPartiesEnabled;                       // optional probe (PartyConfig.isEnabled)
    private final Map<Class<?>, Boolean> dungeonEvents = new LinkedHashMap<>();  // event class -> isJoin

    private volatile boolean sessionDisabled;             // EM said DISABLED once: parties off in Party.yml
    private volatile boolean latchedOff;                  // reflective failures piled up
    private int failures;
    private volatile boolean lastReadOk = true;

    /** Players currently inside an EM dungeon/arena instance (freeze F1), fed by the api events. */
    private final Set<UUID> inInstance = new HashSet<>();
    /** Adapter-driven invites: target -> who invited them and when (DESIGN §5 ledger). */
    private final Map<UUID, DrivenInvite> drivenInvites = new HashMap<>();

    static final class DrivenInvite {
        final UUID inviter; final long at;
        DrivenInvite(UUID inviter, long at) { this.inviter = inviter; this.at = at; }
    }

    /** Ledger liveness as a pure function of time, so it unit-tests without a server. */
    static boolean inviteLive(DrivenInvite d, UUID inviter, long now) {
        return d != null && d.inviter.equals(inviter) && now - d.at >= 0 && now - d.at < INVITE_LEDGER_MS;
    }

    public ReflectEmLink(ClassLoader cl, Function<UUID, Player> online, Logger log) {
        this.online = online; this.log = log;
        resolve(cl);
    }

    /** Names of EliteMobs classes or methods this link needs but cannot find. Empty = the link can work. */
    public static List<String> probe(ClassLoader cl) {
        return new ReflectEmLink(cl, id -> null, Logger.getLogger("probe")).missing;
    }
    public List<String> missing() { return Collections.unmodifiableList(missing); }
    /** Whether EM parties are enabled in its Party.yml, when the probe resolved; null = unknown. */
    public Boolean partiesEnabled() {
        if (mPartiesEnabled == null) return null;
        try { return (Boolean) mPartiesEnabled.invoke(null); } catch (Throwable t) { return null; }
    }
    /** Dungeon/arena event classes for the plugin to register (class -> true = join event). Empty = F1 unavailable. */
    public Map<Class<?>, Boolean> dungeonEvents() { return Collections.unmodifiableMap(dungeonEvents); }
    /** True when the last allParties() read actually read EM (false = the empty list is a failure, not a fact). */
    public boolean lastReadOk() { return lastReadOk; }

    private Class<?> cls(ClassLoader cl, String n, boolean required) {
        try { return Class.forName(n, false, cl); }
        catch (Throwable t) { if (required) missing.add("class " + n); return null; }
    }
    private Method m(Class<?> c, String n, Class<?>... args) {
        if (c == null) return null;
        try { return c.getMethod(n, args); } catch (Throwable t) { missing.add(c.getSimpleName() + "." + n); return null; }
    }

    private void resolve(ClassLoader cl) {
        cPartyMgr = cls(cl, "com.magmaguy.elitemobs.parties.PartyManager", true);
        cParty = cls(cl, "com.magmaguy.elitemobs.parties.Party", true);
        cls(cl, "com.magmaguy.elitemobs.parties.PartyOperationResult", true);
        mGetParties = m(cPartyMgr, "getParties");
        mGetParty = m(cPartyMgr, "getParty", UUID.class);
        mCreate = m(cPartyMgr, "create", Player.class, boolean.class);
        mInvite = m(cPartyMgr, "invite", Player.class, String.class, boolean.class);
        mAccept = m(cPartyMgr, "accept", Player.class, boolean.class);
        mLeave = m(cPartyMgr, "leave", Player.class, boolean.class);
        pGetId = m(cParty, "getId");
        pGetLeader = m(cParty, "getLeader");
        pDisplayOrder = m(cParty, "getMembersInDisplayOrder");
        // optional: the parties-enabled probe (its absence never disables the link by itself)
        Class<?> cCfg = cls(cl, "com.magmaguy.elitemobs.config.PartyConfig", false);
        if (cCfg != null) { try { mPartiesEnabled = cCfg.getMethod("isEnabled"); } catch (Throwable t) { mPartiesEnabled = null; } }
        // optional: the dungeon-instance tracker events (freeze F1); absence leaves F2 to carry it
        for (String[] e : new String[][]{
                {"com.magmaguy.elitemobs.api.PlayerJoinDungeonEvent", "true"},
                {"com.magmaguy.elitemobs.api.PlayerJoinArenaEvent", "true"},
                {"com.magmaguy.elitemobs.api.PlayerLeaveDungeonEvent", "false"},
                {"com.magmaguy.elitemobs.api.PlayerLeaveArenaEvent", "false"}}) {
            Class<?> ec = cls(cl, e[0], false);
            if (ec != null) dungeonEvents.put(ec, Boolean.valueOf(e[1]));
        }
    }

    @Override public boolean available() { return missing.isEmpty() && !sessionDisabled && !latchedOff; }

    /** Stands the link down for the rest of the session, with one log line (idempotent). */
    public void disableSession(String why) {
        if (sessionDisabled) return;
        sessionDisabled = true;
        log.info("ExcelTogether mesh off for this session: " + why + ". ExcelTogether and the mcMMO link are unaffected.");
    }

    private void failed(String what) {
        if (++failures >= FAILURE_LATCH && !latchedOff) {
            latchedOff = true;
            log.warning("ExcelTogether mesh: EliteMobs link switched off for this session after repeated errors (last: " + what + "). The mcMMO party link is unaffected.");
        }
    }
    private void succeeded() { failures = 0; }

    private EmParty picture(Object party) throws Exception {
        if (party == null) return EmParty.NONE;
        Object key = pGetId.invoke(party);
        if (key == null) return EmParty.UNKNOWN;
        UUID leader = (UUID) pGetLeader.invoke(party);
        List<UUID> members = new ArrayList<>();
        Object order = pDisplayOrder.invoke(party);
        if (order instanceof List<?> list) for (Object o : list) if (o instanceof UUID u) members.add(u);
        return new EmParty(leader, members, key);
    }

    @Override public EmParty partyOf(UUID id) {
        if (!available() || id == null) return EmParty.UNKNOWN;
        try {
            EmParty p = picture(mGetParty.invoke(null, id));
            succeeded();
            return p;
        } catch (Throwable t) { failed("partyOf"); return EmParty.UNKNOWN; }
    }

    @Override public List<EmParty> allParties() {
        if (!available()) { lastReadOk = false; return List.of(); }
        try {
            Object map = mGetParties.invoke(null);
            List<EmParty> out = new ArrayList<>();
            if (map instanceof Map<?, ?> mp) {
                for (Object v : mp.values()) {
                    EmParty p = picture(v);
                    if (p.real()) out.add(p);
                }
            }
            lastReadOk = true;
            succeeded();
            return out;
        } catch (Throwable t) { failed("allParties"); lastReadOk = false; return List.of(); }
    }

    /** PartyOperationResult name -> EmMirror, context-sensitive per DESIGN §1.4. Unlisted combinations map conservatively. */
    static EmMirror map(String result, Op op) {
        if (result == null) return EmMirror.ERROR;
        switch (result) {
            case "SUCCESS": return EmMirror.OK;
            case "ALREADY_IN_PARTY": return (op == Op.CREATE || op == Op.ACCEPT) ? EmMirror.OK : EmMirror.NOT_READY;
            case "INVITE_ALREADY_PENDING": return op == Op.INVITE ? EmMirror.OK : EmMirror.NOT_READY;
            case "NOT_IN_PARTY": return op == Op.LEAVE ? EmMirror.OK : EmMirror.NOT_READY;
            case "PARTY_FULL": return EmMirror.FULL;
            case "TARGET_ALREADY_IN_PARTY": return EmMirror.TARGET_IN_OTHER_PARTY;
            case "DISABLED": return EmMirror.DISABLED;
            case "NO_PERMISSION": return EmMirror.NO_PERMISSION;
            case "TARGET_NO_PERMISSION": return EmMirror.TARGET_NO_PERMISSION;
            case "PLAYER_UNAVAILABLE": case "NO_PENDING_INVITE": case "INVITE_EXPIRED": return EmMirror.NOT_READY;
            case "SELF_INVITE": return EmMirror.ERROR;   // unreachable by construction; the adapter guards self
            default: return EmMirror.ERROR;
        }
    }

    private EmMirror drive(Op op, Method method, Object... args) {
        if (!available()) return EmMirror.ABSENT;
        try {
            Object r = method.invoke(null, args);
            succeeded();
            EmMirror mapped = map(r instanceof Enum<?> e ? e.name() : null, op);
            if (mapped == EmMirror.DISABLED) disableSession("EliteMobs parties are disabled in Party.yml");
            return mapped;
        } catch (Throwable t) { failed(op.name()); return EmMirror.ERROR; }
    }

    @Override public EmMirror createParty(UUID leader) {
        if (leader == null) return EmMirror.ERROR;
        Player p = online.apply(leader);
        if (p == null) return EmMirror.NOT_READY;
        return drive(Op.CREATE, mCreate, p, EM_QUIET);
    }

    @Override public EmMirror invite(UUID inviter, UUID target) {
        if (inviter == null || target == null) return EmMirror.ERROR;
        if (inviter.equals(target)) return EmMirror.ERROR;      // SELF_INVITE, guarded here per §1.4
        Player from = online.apply(inviter);
        Player to = online.apply(target);
        if (from == null || to == null) return EmMirror.NOT_READY;
        EmMirror r = drive(Op.INVITE, mInvite, from, to.getName(), EM_QUIET);
        if (r == EmMirror.OK) drivenInvites.put(target, new DrivenInvite(inviter, System.currentTimeMillis()));
        return r;
    }

    @Override public EmMirror accept(UUID target) {
        if (target == null) return EmMirror.ERROR;
        Player p = online.apply(target);
        if (p == null) return EmMirror.NOT_READY;
        return drive(Op.ACCEPT, mAccept, p, EM_QUIET);
    }

    @Override public EmMirror leave(UUID id) {
        if (id == null) return EmMirror.ERROR;
        Player p = online.apply(id);
        if (p == null) return EmMirror.OK;   // EM holds online players only; an offline player is already out
        return drive(Op.LEAVE, mLeave, p, EM_QUIET);
    }

    @Override public boolean inviteDriven(UUID inviter, UUID target) {
        return inviteLive(drivenInvites.get(target), inviter, System.currentTimeMillis());
    }

    // ---- dungeon-instance tracker (freeze F1) ----
    /** Feed one dungeon/arena event (registered by the plugin through EM's classloader). Extracts the player reflectively. */
    public void trackInstanceEvent(Object event, boolean joined) {
        UUID id = eventPlayer(event);
        if (id == null) return;
        if (joined) inInstance.add(id); else inInstance.remove(id);
    }
    static UUID eventPlayer(Object event) {
        if (event == null) return null;
        try {
            Method g = event.getClass().getMethod("getPlayer");
            Object p = g.invoke(event);
            if (p instanceof Player pl) return pl.getUniqueId();
        } catch (Throwable ignored) { }
        try {
            Field f = event.getClass().getDeclaredField("player");
            f.setAccessible(true);
            Object p = f.get(event);
            if (p instanceof Player pl) return pl.getUniqueId();
            if (p instanceof UUID u) return u;
        } catch (Throwable ignored) { }
        return null;
    }
    @Override public boolean dungeonBusy(Collection<UUID> members) {
        if (members == null || inInstance.isEmpty()) return false;
        for (UUID id : members) if (inInstance.contains(id)) return true;
        return false;
    }
    /** Test hook: the tracked instance set right now. */
    Set<UUID> inInstance() { return Collections.unmodifiableSet(inInstance); }
}
