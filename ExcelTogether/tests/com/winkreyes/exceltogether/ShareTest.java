package com.winkreyes.exceltogether;

import com.gamingmesh.jobs.api.JobsPrePaymentEvent;
import com.gamingmesh.jobs.container.Job;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.Proxy;
import java.util.*;

/** Bonus indicator + Jobs sharing rules. Dependency-free runner. */
public class ShareTest {
    static int fail = 0, pass = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }
    static boolean eq(double a, double b) { return Math.abs(a - b) < 1e-9; }

    static OfflinePlayer pl(UUID id, String name) {
        return (OfflinePlayer) Proxy.newProxyInstance(ShareTest.class.getClassLoader(), new Class<?>[]{OfflinePlayer.class}, (p, m, a) -> {
            if (m.getName().equals("getUniqueId")) return id;
            if (m.getName().equals("getName")) return name;
            if (m.getName().equals("hashCode")) return id.hashCode();
            if (m.getName().equals("equals")) return p == a[0];
            Class<?> r = m.getReturnType();
            if (r == boolean.class) return false; if (r == int.class) return 0; if (r == long.class) return 0L; if (r == double.class) return 0.0;
            return null;
        });
    }
    static Job job() throws Exception {
        java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
        return (Job) ((sun.misc.Unsafe) f.get(null)).allocateInstance(Job.class);
    }
    static JobsPrePaymentEvent ev(OfflinePlayer p, Job j, double money, double exp) {
        return new JobsPrePaymentEvent(p, j, money, exp, 0.0, null, null, null, null);
    }
    static final class Fin implements PartyFinder {
        boolean partner = true; String name = "Fern"; List<OfflinePlayer> sh = new ArrayList<>();
        public boolean hasPartner(OfflinePlayer e) { return partner; }
        public String partnerName(OfflinePlayer e) { return partner ? name : null; }
        public List<OfflinePlayer> sharers(OfflinePlayer e) { return sh; }
    }
    static final class Rec { OfflinePlayer to; double money, xp; String from; }

    public static void main(String[] args) throws Exception {
        PartyBonus b = new PartyBonus(1.25, 50);
        UUID ua = UUID.randomUUID(), ub = UUID.randomUUID(), uc = UUID.randomUUID();
        OfflinePlayer A = pl(ua, "Mike"), B = pl(ub, "Fern"), C = pl(uc, "Third");
        Job j = job();
        List<Rec> got = new ArrayList<>(); List<String> bars = new ArrayList<>(), chats = new ArrayList<>();
        long[] now = {1_000_000L};
        Fin f = new Fin(); f.sh.add(B); f.sh.add(C);
        PartyListener[] holder = new PartyListener[1];
        ShareSink sink = (r, job, m, x, from) -> {
            Rec rc = new Rec(); rc.to = r; rc.money = m; rc.xp = x; rc.from = from; got.add(rc);
            // a buggy payout path that fires another gain must not cause more sharing
            holder[0].onPay(ev(r, job, 5, 5));
            return !r.getUniqueId().equals(uc) || true;
        };
        PartyListener l = new PartyListener(b, f, sink, 100, (p, t) -> bars.add(p.getName() + ": " + t), (p, t) -> chats.add(p.getName() + ": " + t), () -> now[0]);
        holder[0] = l;

        JobsPrePaymentEvent e = ev(A, j, 8, 4);
        l.onPay(e);
        check("earner x1.25 money", eq(e.getAmount(), 10)); check("earner x1.25 xp", eq(e.getExp(), 5));
        check("two receivers", got.size() == 2);
        check("receiver B gets 100% of boosted (X*1.25)", eq(got.get(0).money, 10) && eq(got.get(0).xp, 5));
        check("receiver C same", eq(got.get(1).money, 10) && eq(got.get(1).xp, 5));
        check("names", got.get(0).from.equals("Mike"));
        check("no re-sharing from shared gain", l.shared == 2 && l.applied == 1);
        check("indicator text", bars.get(0).equals("Mike: +25% party bonus (with Fern)"));
        check("receiver indicator", bars.stream().anyMatch(s -> s.startsWith("Fern: +25% party bonus: shared")));
        check("one chat line explains", chats.size() == 1 && chats.get(0).contains("/excelparty status"));
        // throttle
        got.clear(); bars.clear(); chats.clear();
        now[0] += 1000; l.onPay(ev(A, j, 1, 1));
        check("bar throttled within 3s", bars.stream().noneMatch(s -> s.startsWith("Mike:")));
        check("chat throttled", chats.isEmpty());
        now[0] += 3000; l.onPay(ev(A, j, 1, 1));
        check("bar again after 3s", bars.stream().anyMatch(s -> s.startsWith("Mike:")));
        now[0] += 120_000; l.onPay(ev(A, j, 1, 1));
        check("chat again after 2 min", chats.size() == 1);
        // no partner: nothing
        f.partner = false; got.clear(); bars.clear();
        JobsPrePaymentEvent n = ev(A, j, 8, 4); l.onPay(n);
        check("no partner unchanged", eq(n.getAmount(), 8) && eq(n.getExp(), 4) && got.isEmpty() && bars.isEmpty());
        f.partner = true;
        // no sharers (all far/idle)
        f.sh = new ArrayList<>(); got.clear();
        l.onPay(ev(A, j, 8, 4)); check("no sharers no copies", got.isEmpty());
        f.sh = new ArrayList<>(List.of(A, B)); got.clear();   // earner in own list is skipped
        l.onPay(ev(A, j, 8, 4)); check("earner never self-shared", got.size() == 1 && got.get(0).to == B);
        // penalty and zero never shared
        got.clear(); JobsPrePaymentEvent pen = ev(A, j, -5, 0); l.onPay(pen);
        check("penalty not shared or boosted", got.isEmpty() && eq(pen.getAmount(), -5));
        // percent 50 and off
        PartyListener half = new PartyListener(b, f, sink, 50, null, null, () -> now[0]); holder[0] = half;
        got.clear(); half.onPay(ev(A, j, 8, 4));
        check("50% share", got.size() == 1 && eq(got.get(0).money, 5) && eq(got.get(0).xp, 2.5));
        PartyListener off = new PartyListener(b, f, sink, 0, null, null, () -> now[0]); holder[0] = off;
        got.clear(); JobsPrePaymentEvent o = ev(A, j, 8, 4); off.onPay(o);
        check("share off still boosts earner", eq(o.getAmount(), 10) && got.isEmpty());
        // null job: bonus ok, no share
        holder[0] = l; got.clear(); JobsPrePaymentEvent nj = ev(A, null, 8, 4); l.onPay(nj);
        check("null job: boost, no share", eq(nj.getAmount(), 10) && got.isEmpty());
        // sink throws: earner still paid, others continue
        f.sh = new ArrayList<>(List.of(B, C)); int[] calls = {0};
        PartyListener thr = new PartyListener(b, f, (r, jb, m, x, fr) -> { calls[0]++; if (calls[0] == 1) throw new RuntimeException("x"); return true; }, 100, null, null, () -> now[0]);
        JobsPrePaymentEvent te = ev(A, j, 8, 4); thr.onPay(te);
        check("throwing sink: earner fine, next receiver still tried", eq(te.getAmount(), 10) && calls[0] == 2 && thr.shared == 1);
        // bad share percent
        PartyListener nan = new PartyListener(b, f, sink, Double.NaN, null, null, () -> now[0]); got.clear(); nan.onPay(ev(A, j, 8, 4));
        check("NaN percent = no share", got.isEmpty());
        PartyListener big = new PartyListener(b, f, sink, 900, null, null, () -> now[0]); holder[0] = big; got.clear(); big.onPay(ev(A, j, 8, 4));
        check("percent capped at 100", eq(got.get(0).money, 10));

        // Jobs: receiver's own boosts on top of the party x1.25, earner's boost not counted
        {
            com.gamingmesh.jobs.container.Boost wk = new com.gamingmesh.jobs.container.Boost();   // receiver with a +20% weekend boost
            wk.add(com.gamingmesh.jobs.PlayerManager.BoostOf.Global, new com.gamingmesh.jobs.container.BoostMultiplier()
                    .add(com.gamingmesh.jobs.container.CurrencyType.MONEY, 0.2).add(com.gamingmesh.jobs.container.CurrencyType.EXP, 0.2));
            double[] r = JobsShareSink.withReceiverBoosts(wk, 10.0, 5.0);       // 10 / 5 = earner's party-boosted, pre-Jobs-boost amounts
            check("weekend ON: money 10 -> 12", eq(r[0], 12.0)); check("weekend ON: xp 5 -> 6", eq(r[1], 6.0));
            com.gamingmesh.jobs.container.Boost none = new com.gamingmesh.jobs.container.Boost();
            double[] q = JobsShareSink.withReceiverBoosts(none, 10.0, 5.0);
            check("weekend OFF: unchanged", eq(q[0], 10.0) && eq(q[1], 5.0));
            double[] z = JobsShareSink.withReceiverBoosts(null, 10.0, 5.0);
            check("no boost info: unchanged", eq(z[0], 10.0) && eq(z[1], 5.0));
            // full chain: base 8 money, party x1.25 -> 10 to listener; earner (boost 0.2) gets 12 from Jobs; receiver gets 10*1.2 = 12 too, no extra
            check("earner and receiver end equal with equal boosts", eq(10.0 * 1.2, JobsShareSink.withReceiverBoosts(wk, 10.0, 0)[0]));
            // the listener hands the sink the PRE-boost party amounts: receiver boost is applied once, not twice
            List<double[]> seen = new ArrayList<>();
            PartyListener pl2 = new PartyListener(b, f, (rc, jb, m, x, fr) -> { seen.add(new double[]{m, x}); return true; }, 100, null, null, () -> now[0]);
            f.sh = new ArrayList<>(List.of(B)); pl2.onPay(ev(A, j, 8, 4));
            check("sink gets pre-Jobs-boost party amounts (10, 5)", seen.size() == 1 && eq(seen.get(0)[0], 10) && eq(seen.get(0)[1], 5));
        }
        System.out.println(pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
