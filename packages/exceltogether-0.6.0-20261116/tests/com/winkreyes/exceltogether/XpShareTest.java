package com.winkreyes.exceltogether;

import java.util.*;

public class XpShareTest {
    static int fail = 0, pass = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }
    public static void main(String[] a) throws Exception {
        UUID e = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        List<String> log = new ArrayList<>();
        XpShareCore[] h = new XpShareCore[1];
        XpShareCore core = new XpShareCore((r, s, xp, rs) -> {
            log.add(r.equals(b) ? "B" + xp : "C" + xp);
            h[0].onGain(r, s, xp, rs, false, () -> List.of(e, b, c));    // payout fires another gain event: must not re-share
            return !r.equals(c) || true;
        }, 100);
        h[0] = core;
        int n = core.onGain(e, "MINING", 40f, "PVE", false, () -> List.of(e, b, c));
        check("two copies, full amount", n == 2 && log.equals(List.of("B40.0", "C40.0")));
        check("no re-share loop", core.shared == 2);
        log.clear();
        check("mcMMO EQUAL share: stay out", core.onGain(e, "MINING", 40f, "PVE", true, () -> List.of(b)) == 0 && log.isEmpty());
        check("zero xp", core.onGain(e, "MINING", 0f, "PVE", false, () -> List.of(b)) == 0);
        check("NaN xp", core.onGain(e, "MINING", Float.NaN, "PVE", false, () -> List.of(b)) == 0);
        check("null skill", core.onGain(e, null, 5f, "PVE", false, () -> List.of(b)) == 0);
        check("nobody in range", core.onGain(e, "MINING", 5f, "PVE", false, List::of) == 0);
        check("earner skipped", core.onGain(e, "MINING", 5f, "PVE", false, () -> List.of(e)) == 0);
        XpShareCore half = new XpShareCore((r, s, xp, rs) -> { log.add("H" + xp); return true; }, 50);
        log.clear(); half.onGain(e, "MINING", 10f, "PVE", false, () -> List.of(b));
        check("50 percent", log.equals(List.of("H5.0")));
        check("off at 0", new XpShareCore((r, s, xp, rs) -> true, 0).onGain(e, "S", 5f, "R", false, () -> List.of(b)) == 0);
        XpShareCore thr = new XpShareCore((r, s, xp, rs) -> { if (r.equals(b)) throw new RuntimeException(); return true; }, 100);
        check("one failing receiver does not stop the next", thr.onGain(e, "S", 5f, "R", false, () -> List.of(b, c)) == 1);
        // Boost stacking math (mcMMO side). Weekend ExcelWeekendXP = global rate 1.2 for everybody.
        float finalA = 100f * 1.2f * 1.25f;                                     // earner: base 100, weekend 1.2, party 1.25 (Together bonus)
        float base = XpShareCore.baseOf(finalA, 1.2, 1.25);
        check("base recovered, earner boost not double counted", Math.abs(base - 100f) < 1e-3);
        check("receiver weekend: base*1.2*1.25 (same as earner)", Math.abs(XpShareCore.receiverAmount(base, 1.2, 1.25) - 150f) < 1e-3);
        float baseWd = XpShareCore.baseOf(100f * 1.0f * 1.25f, 1.0, 1.25);
        check("weekday: base*1.0*1.25", Math.abs(XpShareCore.receiverAmount(baseWd, 1.0, 1.25) - 125f) < 1e-3);
        check("receiver with own +10% perk on weekend", Math.abs(XpShareCore.receiverAmount(base, 1.2 * 1.1, 1.25) - 165f) < 1e-3);
        check("earner with perk, plain receiver: earner perk removed", Math.abs(XpShareCore.receiverAmount(XpShareCore.baseOf(100f * 1.5f * 1.25f, 1.5, 1.25), 1.0, 1.25) - 125f) < 1e-3);
        check("earner at skill cap (mod 0): plain copy fallback", Float.isNaN(XpShareCore.baseOf(50f, 0, 1.25)));
        check("together bonus absent (upstream): divide by 1", Math.abs(XpShareCore.baseOf(120f, 1.2, 1.0) - 100f) < 1e-3);
        check("NaN mod unusable", Float.isNaN(XpShareCore.baseOf(50f, Double.NaN, 1.0)));
        check("receiver at cap gets 0", XpShareCore.receiverAmount(100f, 0, 1.25) == 0f);
        // real mcMMO jar: every reflected piece exists
        String jar = System.getProperty("mcmmojar");
        if (jar != null) {
            try (java.net.URLClassLoader cl = new java.net.URLClassLoader(new java.net.URL[]{new java.io.File(jar).toURI().toURL()}, XpShareTest.class.getClassLoader()) {
                @Override protected Class<?> loadClass(String n, boolean r) throws ClassNotFoundException {   // real mcMMO classes win over test stubs
                    if (n.startsWith("com.gmail.nossr50.")) { synchronized (getClassLoadingLock(n)) { Class<?> c = findLoadedClass(n); if (c == null) c = findClass(n); return c; } }
                    return super.loadClass(n, r);
                } }) {
                McmmoXpShare x = new McmmoXpShare(cl, new PartyFinder() { public boolean hasPartner(org.bukkit.OfflinePlayer p) { return false; } }, 100, id -> null, null, "t", 1.25);
                check("real mcMMO exposes every method sharing needs: " + x.missing, x.available());
                check("real mcMMO: receiver-boost path (modifyXpGain) resolves", x.receiverBoosts);
            }
        }
        System.out.println(pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
