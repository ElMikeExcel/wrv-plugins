package com.winkreyes.exceltogether;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

/**
 * 0.6.0 unit 1 (P-PARTY-REBUILD-1, SCOPING.md §2.1/§2.5 + §3 RT-5 store half):
 * the roster-of-record persistence layer. Covers:
 *  - round-trip: a live PartyManager roster (two parties, names, origin tags,
 *    next-id) survives save -> load -> restore exactly;
 *  - restart survival: a party formed before a simulated restart is the
 *    roster after it, a leave taken after the restart persists (no
 *    resurrection on the next cycle), and new parties never reuse a
 *    restored party id;
 *  - origin-tag recording: DOOR accepts, LOGIN_REBUILD adoptions, ADMIN
 *    removals and the write-through change listener all land in the file;
 *  - corrupt / partial recovery: garbage files, a bad party among good ones,
 *    undersized parties, duplicate members, unknown origin tags, truncated
 *    files - load never throws and recovers what still parses;
 *  - the first-boot distinction: missing file (filePresent=false, the Q6
 *    seed case) vs an existing empty roster (filePresent=true).
 * Dependency-free runner like the rest of the suite. Exit code 1 on failure.
 */
public class RosterStoreTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }
    static final Logger LOG = Logger.getLogger("RosterStoreTest");
    static UUID u() { return UUID.randomUUID(); }

    static PartyManager mgr(Set<UUID> on) { return new PartyManager(5, 60_000, 300_000, on::contains); }

    /** Form a party through the door path (invite + accept) and return its id. */
    static long doorParty(PartyManager m, UUID leader, UUID... rest) {
        long t = 1_000L;
        for (UUID member : rest) {
            check("door invite ok", m.invite(leader, member, t) == PartyManager.InviteResult.OK);
            PartyManager.AcceptOutcome o = m.accept(member, null, t + 1);
            check("door accept ok", o.result == PartyManager.AcceptResult.OK);
        }
        PartyManager.Party p = m.partyOf(leader);
        check("door party exists", p != null);
        return p == null ? -1 : p.id;
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("rosterstore-test");
        roundTrip(dir);
        restartSurvival(dir);
        originTags(dir);
        writeThrough(dir);
        corruptRecovery(dir);
        firstBoot(dir);
        System.out.println("RosterStoreTest: " + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }

    // ---------- round-trip ----------
    static void roundTrip(Path dir) throws Exception {
        Set<UUID> on = new HashSet<>();
        UUID a = u(), b = u(), c = u(), d = u(), e = u(), f = u();
        on.addAll(List.of(a, b, c, d, e, f));
        PartyManager m = mgr(on);
        m.rememberName(a, "Alice"); m.rememberName(b, "Bob"); m.rememberName(c, "Carol");
        m.rememberName(d, "Dan"); m.rememberName(e, "Erin"); m.rememberName(f, "Fred");
        long p1 = doorParty(m, a, b, c);                       // DOOR party of 3
        PartyManager.AdoptOutcome ad = m.adopt(f, d);          // LOGIN_REBUILD party of 2 (d leads)
        check("adopt created", ad.result == PartyManager.AdoptResult.CREATED);
        long p2 = m.partyOf(d).id;
        check("two parties live", m.partyCount() == 2 && p1 != p2);

        File file = dir.resolve("roundtrip.yml").toFile();
        RosterStore store = new RosterStore();
        store.save(file, m, LOG);
        check("roster file written", file.isFile() && file.length() > 0);
        String text = Files.readString(file.toPath());
        check("file carries origin DOOR", text.contains("DOOR"));
        check("file carries origin LOGIN_REBUILD", text.contains("LOGIN_REBUILD"));
        check("file carries version", text.contains("version: 1"));

        RosterStore.Loaded loaded = store.load(file, LOG);
        check("load present + parseOk", loaded.filePresent && loaded.parseOk);
        check("load skipped none", loaded.skipped == 0);
        check("load found both parties", loaded.parties.size() == 2);

        PartyManager m2 = mgr(on);
        int restored = m2.restore(loaded.parties, loaded.names, loaded.nextId);
        check("restore count", restored == 2);
        check("restored roster matches (a,b,c)", m2.sameParty(a, b) && m2.sameParty(a, c));
        check("restored roster matches (d,f)", m2.sameParty(d, f));
        check("restored parties stay separate", !m2.sameParty(a, d));
        check("restored leader p1", m2.partyOf(a).leader.equals(a) && m2.partyOf(a).id == p1);
        check("restored leader p2", m2.partyOf(d).leader.equals(d) && m2.partyOf(d).id == p2);
        check("restored member order p1", m2.partyOf(a).members.equals(List.of(a, b, c)));
        check("restored origin p1 is DOOR", m2.originOf(p1) == RosterOrigin.DOOR);
        check("restored origin p2 is LOGIN_REBUILD", m2.originOf(p2) == RosterOrigin.LOGIN_REBUILD);
        check("restored originOfPlayer", m2.originOfPlayer(b) == RosterOrigin.DOOR && m2.originOfPlayer(f) == RosterOrigin.LOGIN_REBUILD);
        check("restored names survive", m2.nameOf(a).equals("Alice") && m2.nameOf(f).equals("Fred"));
        check("restored nextId preserved", m2.nextId() == m.nextId());
    }

    // ---------- restart survival (RT-5, store half) ----------
    static void restartSurvival(Path dir) throws Exception {
        Set<UUID> on = new HashSet<>();
        UUID a = u(), b = u(), c = u(), d = u(), e = u();
        on.addAll(List.of(a, b, c, d, e));
        RosterStore store = new RosterStore();
        File file = dir.resolve("restart.yml").toFile();

        PartyManager before = mgr(on);
        long pid = doorParty(before, a, b, c);
        store.save(file, before, LOG);

        // --- simulated restart: a brand-new manager whose ONLY input is the file ---
        PartyManager after = mgr(on);
        RosterStore.Loaded l1 = store.load(file, LOG);
        after.restore(l1.parties, l1.names, l1.nextId);
        check("RT-5 roster of record = pre-restart roster", after.sameParty(a, b) && after.sameParty(a, c)
                && after.partyOf(a).leader.equals(a) && after.partyOf(a).id == pid);

        // a leave after the restart persists too: no resurrection on the next cycle
        PartyManager.Change ch = new PartyManager.Change();
        check("post-restart leave works", after.leave(c, ch) == PartyManager.LeaveResult.LEFT);
        store.save(file, after, LOG);
        PartyManager after2 = mgr(on);
        RosterStore.Loaded l2 = store.load(file, LOG);
        after2.restore(l2.parties, l2.names, l2.nextId);
        check("RT-5 leave sticks across another restart", !after2.inParty(c) && after2.sameParty(a, b));

        // a disband after the restart persists: the party is gone for good
        after2.leave(a, new PartyManager.Change());
        store.save(file, after2, LOG);
        PartyManager after3 = mgr(on);
        RosterStore.Loaded l3 = store.load(file, LOG);
        after3.restore(l3.parties, l3.names, l3.nextId);
        check("RT-5 disband sticks across another restart", after3.partyCount() == 0 && !after3.inParty(b));

        // new parties formed after a restore never reuse a restored (or retired) id
        PartyManager m = mgr(on);
        RosterStore.Loaded l4 = store.load(dir.resolve("roundtrip.yml").toFile(), LOG);
        m.restore(l4.parties, l4.names, l4.nextId);
        long maxRestored = l4.parties.stream().mapToLong(p -> p.id).max().orElse(0);
        long fresh = doorParty(m, d, e);
        check("post-restore party id is fresh", fresh > maxRestored);
    }

    // ---------- origin-tag recording ----------
    static void originTags(Path dir) throws Exception {
        Set<UUID> on = new HashSet<>();
        UUID a = u(), b = u(), c = u(), d = u();
        on.addAll(List.of(a, b, c, d));
        PartyManager m = mgr(on);
        check("no origin before any mutation", m.lastOrigin() == null);
        long p1 = doorParty(m, a, b, c);
        check("door accept tags party DOOR", m.originOf(p1) == RosterOrigin.DOOR);
        check("lastOrigin DOOR after door accept", m.lastOrigin() == RosterOrigin.DOOR);

        // an ADMIN removal re-tags the surviving party and the global last origin
        check("admin kick ok", m.kick(a, c, new PartyManager.Change(), RosterOrigin.ADMIN) == PartyManager.KickResult.OK);
        check("surviving party re-tagged ADMIN", m.originOf(p1) == RosterOrigin.ADMIN);
        check("lastOrigin ADMIN after admin kick", m.lastOrigin() == RosterOrigin.ADMIN);

        // an ADMIN disband leaves no party but the last origin still records it
        PartyManager m2 = mgr(on);
        long q = doorParty(m2, a, b);
        m2.leave(a, new PartyManager.Change(), RosterOrigin.ADMIN);
        check("admin disband removes party", m2.partyCount() == 0 && m2.originOf(q) == null);
        check("lastOrigin ADMIN after admin disband", m2.lastOrigin() == RosterOrigin.ADMIN);

        // setLeader carries its origin too
        PartyManager m3 = mgr(on);
        long r = doorParty(m3, a, b, c);
        check("setLeader works", m3.setLeader(b, RosterOrigin.ADMIN));
        check("setLeader re-tags ADMIN", m3.originOf(r) == RosterOrigin.ADMIN && m3.partyOf(a).leader.equals(b));

        // tags persist through the file
        RosterStore store = new RosterStore();
        File file = dir.resolve("origins.yml").toFile();
        store.save(file, m, LOG);
        RosterStore.Loaded loaded = store.load(file, LOG);
        PartyManager m4 = mgr(on);
        m4.restore(loaded.parties, loaded.names, loaded.nextId);
        check("ADMIN tag survives the file", m4.originOf(p1) == RosterOrigin.ADMIN);
        check("parse: valid tags", RosterOrigin.parse("door") == RosterOrigin.DOOR && RosterOrigin.parse(" LOGIN_REBUILD ") == RosterOrigin.LOGIN_REBUILD);
        check("parse: unknown is null", RosterOrigin.parse("SYNC") == null && RosterOrigin.parse(null) == null);
        check("d unused sanity", d != null);
    }

    // ---------- write-through listener (the plugin's save hook) ----------
    static void writeThrough(Path dir) throws Exception {
        Set<UUID> on = new HashSet<>();
        UUID a = u(), b = u();
        on.addAll(List.of(a, b));
        PartyManager m = mgr(on);
        RosterStore store = new RosterStore();
        File file = dir.resolve("writethrough.yml").toFile();
        int[] saves = {0};
        m.setChangeListener(() -> { saves[0]++; store.save(file, m, LOG); });
        check("no save before any mutation", saves[0] == 0 && !file.exists());
        m.invite(a, b, 1_000L);
        check("an invite alone is not roster state (no save)", saves[0] == 0);
        m.accept(b, null, 1_001L);
        check("accept writes through", saves[0] == 1 && file.isFile());
        RosterStore.Loaded l = store.load(file, LOG);
        check("written-through roster loads back", l.parties.size() == 1 && l.parties.get(0).members.containsAll(List.of(a, b)));
        m.leave(b, new PartyManager.Change());
        check("leave writes through (disband persisted)", saves[0] == 2);
        RosterStore.Loaded l2 = store.load(file, LOG);
        check("disband is in the file, not resurrected", l2.filePresent && l2.parties.isEmpty());
        // a throwing listener never breaks party logic
        PartyManager m2 = mgr(on);
        m2.setChangeListener(() -> { throw new RuntimeException("boom"); });
        long p = doorParty(m2, a, b);
        check("throwing listener leaves the roster working", p > 0 && m2.sameParty(a, b));
    }

    // ---------- corrupt / partial recovery ----------
    static void corruptRecovery(Path dir) throws Exception {
        RosterStore store = new RosterStore();
        UUID a = u(), b = u(), c = u(), d = u(), e = u();

        // (a) pure garbage: no throw, present-but-unparsed, empty roster
        File garbage = dir.resolve("garbage.yml").toFile();
        Files.write(garbage.toPath(), new byte[]{0x00, 0x01, (byte) 0xFF, 'x', ':', '[', '{'});
        RosterStore.Loaded g = store.load(garbage, LOG);
        check("garbage: filePresent, not parseOk, empty", g.filePresent && !g.parseOk && g.parties.isEmpty());

        // (b) one good party + one with an unparseable leader + one undersized party
        File mixed = dir.resolve("mixed.yml").toFile();
        Files.writeString(mixed.toPath(),
                "version: 1\n"
                + "next-id: 9\n"
                + "names:\n  '" + a + "': Alice\n"
                + "parties:\n"
                + "  '1':\n    leader: '" + a + "'\n    origin: DOOR\n    members: ['" + a + "', '" + b + "']\n"
                + "  '2':\n    leader: 'not-a-uuid'\n    origin: DOOR\n    members: ['" + c + "', '" + d + "']\n"
                + "  '3':\n    leader: '" + e + "'\n    origin: DOOR\n    members: ['" + e + "']\n");
        RosterStore.Loaded mx = store.load(mixed, LOG);
        check("mixed: parseOk with skips", mx.parseOk && mx.skipped == 2);
        check("mixed: the good party survives", mx.parties.size() == 1 && mx.parties.get(0).id == 1
                && mx.parties.get(0).leader.equals(a) && mx.parties.get(0).members.equals(List.of(a, b)));
        check("mixed: names + next-id survive", mx.names.get(a).equals("Alice") && mx.nextId == 9);

        // (c) duplicate member across parties: first party keeps them, second drops below size and is skipped
        File dup = dir.resolve("dup.yml").toFile();
        Files.writeString(dup.toPath(),
                "version: 1\nparties:\n"
                + "  '1':\n    leader: '" + a + "'\n    origin: DOOR\n    members: ['" + a + "', '" + b + "']\n"
                + "  '2':\n    leader: '" + b + "'\n    origin: DOOR\n    members: ['" + b + "', '" + c + "']\n");
        RosterStore.Loaded dp = store.load(dup, LOG);
        check("duplicate member: first party wins", dp.parties.size() == 1 && dp.parties.get(0).members.equals(List.of(a, b)));

        // (d) unknown origin tag: party loads, tag falls back to LOGIN_REBUILD
        File unk = dir.resolve("unknown-origin.yml").toFile();
        Files.writeString(unk.toPath(),
                "version: 1\nparties:\n"
                + "  '5':\n    leader: '" + a + "'\n    origin: SYNC_ENGINE\n    members: ['" + a + "', '" + b + "']\n");
        RosterStore.Loaded un = store.load(unk, LOG);
        check("unknown origin: party survives with fallback tag", un.parties.size() == 1
                && un.parties.get(0).origin == RosterOrigin.LOGIN_REBUILD);

        // (e) leader not among members: longest-standing member leads instead of dropping the party
        File lead = dir.resolve("leader-missing.yml").toFile();
        Files.writeString(lead.toPath(),
                "version: 1\nparties:\n"
                + "  '7':\n    leader: '" + e + "'\n    origin: ADMIN\n    members: ['" + a + "', '" + b + "']\n");
        RosterStore.Loaded ld = store.load(lead, LOG);
        check("missing leader: party survives, first member leads", ld.parties.size() == 1 && ld.parties.get(0).leader.equals(a));

        // (f) truncated mid-party: no throw (whatever YAML still maps is recovered, or empty)
        File trunc = dir.resolve("truncated.yml").toFile();
        Files.writeString(trunc.toPath(),
                "version: 1\nparties:\n  '1':\n    leader: '" + a + "'\n    members: ['" + a + "', '");
        RosterStore.Loaded tr = null;
        try { tr = store.load(trunc, LOG); } catch (Throwable t) { check("truncated: load never throws", false); }
        check("truncated: load returned a result", tr != null && tr.filePresent);

        // (g) a failed save is soft: saving under a path whose parent is a regular file must not throw
        File blocker = dir.resolve("blocker").toFile();
        Files.writeString(blocker.toPath(), "not a directory");
        PartyManager m = mgr(new HashSet<>(List.of(a, b)));
        doorParty(m, a, b);
        boolean threw = false;
        try { store.save(new File(blocker, "roster.yml"), m, LOG); } catch (Throwable t) { threw = true; }
        check("unwritable save never throws", !threw);
        check("in-memory roster unaffected by failed save", m.sameParty(a, b));
    }

    // ---------- first boot vs stored-empty ----------
    static void firstBoot(Path dir) throws Exception {
        RosterStore store = new RosterStore();
        File missing = dir.resolve("never-written.yml").toFile();
        RosterStore.Loaded absent = store.load(missing, LOG);
        check("missing file = first boot (filePresent false)", !absent.filePresent && absent.parseOk && absent.parties.isEmpty());

        // an existing file holding an empty roster is a stored state, NOT a seed trigger
        PartyManager empty = mgr(new HashSet<>());
        File emptyFile = dir.resolve("empty.yml").toFile();
        store.save(emptyFile, empty, LOG);
        RosterStore.Loaded el = store.load(emptyFile, LOG);
        check("stored empty roster: filePresent true, zero parties", el.filePresent && el.parseOk && el.parties.isEmpty());
        PartyManager m = mgr(new HashSet<>());
        check("restore of empty snapshot restores zero", m.restore(el.parties, el.names, el.nextId) == 0 && m.isEmpty());
    }
}
