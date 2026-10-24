import com.winkreyes.exceldeathrecord.core.*;
import java.util.ArrayList;
import java.util.List;

/** 0.3.0 Lost & Found rules: eligibility, cooldown, price + free threshold, globs, state, CSV. */
public class LostFoundTest {
    static final String ME = new java.util.UUID(0L, 10L).toString(); // synthetic test id
    static final String OTHER = new java.util.UUID(0L, 11L).toString();
    static final long DAY = 24L * 3600 * 1000;
    static final long NOW = 1_800_000_000_000L;

    static DestructionRecord rec(String owner, long epoch, String id) {
        DestructionRecord r = new DestructionRecord();
        r.ownerUuid = owner; r.ownerName = owner.isEmpty() ? "" : "Sam";
        r.epochMillis = epoch; r.cause = "LAVA"; r.world = "minecraft:overworld";
        r.item.id = id; r.item.count = 1; r.item.bytesB64 = "QUJD";
        return r;
    }

    public static void run() {
        System.out.println("LostFoundTest");
        LostFoundRules.Settings s = new LostFoundRules.Settings();

        // eligibility
        Check.eq(null, LostFoundRules.ineligible(rec(ME, NOW - DAY, "minecraft:diamond_sword"), ME, ME, NOW, s), "own entry inside 7 days is eligible");
        Check.ok(LostFoundRules.ineligible(rec(OTHER, NOW - DAY, "x"), OTHER, ME, NOW, s).contains("someone else"), "another player's entry is refused");
        Check.ok(LostFoundRules.ineligible(rec(ME, NOW - DAY, "x"), OTHER, ME, NOW, s) != null, "entry in another player's folder is refused");
        Check.ok(LostFoundRules.ineligible(rec("", NOW - DAY, "x"), "unknown", ME, NOW, s).contains("no known owner"), "unknown owner stays admin-only");
        Check.eq(null, LostFoundRules.ineligible(rec(ME, NOW - 7 * DAY, "x"), ME, ME, NOW, s), "exactly 7 days is still inside the window");
        Check.ok(LostFoundRules.ineligible(rec(ME, NOW - 7 * DAY - 1, "x"), ME, ME, NOW, s).contains("more than 7 days"), "7 days + 1 ms is outside the window");
        DestructionRecord marked = rec(ME, NOW - DAY, "x"); marked.item.restoredTimes = 1;
        Check.ok(LostFoundRules.ineligible(marked, ME, ME, NOW, s).contains("already"), "already brought back (once-only mark) is refused");
        DestructionRecord nobytes = rec(ME, NOW - DAY, "x"); nobytes.item.bytesB64 = "";
        Check.ok(LostFoundRules.ineligible(nobytes, ME, ME, NOW, s).contains("no saved exact copy"), "entry without bytes is refused");
        Check.ok(LostFoundRules.ineligible(rec(ME, NOW + 10 * 60_000, "x"), ME, ME, NOW, s) != null, "future-dated entry is refused");

        // cooldown
        LostFoundState st = new LostFoundState();
        Check.eq(0L, LostFoundRules.cooldownLeft(st, NOW, s), "no recovery yet: no cooldown");
        st.lastRecoveryEpoch = NOW - DAY + 60_000;
        Check.eq(60_000L, LostFoundRules.cooldownLeft(st, NOW, s), "1 minute left of 24 h");
        st.lastRecoveryEpoch = NOW - DAY;
        Check.eq(0L, LostFoundRules.cooldownLeft(st, NOW, s), "exactly 24 h later: ready");
        Check.eq("23h 59m", LostFoundRules.durationText(DAY - 60_000), "duration text");
        Check.eq("24h", LostFoundRules.durationText(DAY), "duration 24h");

        // price + free threshold edge
        LostFoundState fresh = new LostFoundState();
        Check.ok(LostFoundRules.decide(fresh, 9999.99, s).free, "$9,999.99 with no recovery yet: free");
        Check.ok(!LostFoundRules.decide(fresh, 10000.00, s).free, "$10,000.00 exactly: not free (must be UNDER)");
        Check.ok(!LostFoundRules.decide(fresh, 40000, s).free, "rich player: not free");
        Check.close(16000, LostFoundRules.decide(fresh, 40000, s).amount, "price is 16000");
        Check.ok(LostFoundRules.decide(fresh, 0, s).free, "broke player first time: free");
        LostFoundState usedFree = new LostFoundState(); usedFree.freeUsed = true; usedFree.recoveries = 1;
        Check.ok(!LostFoundRules.decide(usedFree, 5, s).free, "free used: pays next time");
        LostFoundState paidBefore = new LostFoundState(); paidBefore.recoveries = 1;
        Check.ok(!LostFoundRules.decide(paidBefore, 5, s).free, "paid one before: no free later (default)");
        s.freeOnlyOnFirst = false;
        Check.ok(LostFoundRules.decide(paidBefore, 5, s).free, "free-only-if-no-earlier-recovery false: free later when poor");
        s.freeOnlyOnFirst = true;
        s.freeEnabled = false;
        Check.ok(!LostFoundRules.decide(fresh, 0, s).free, "free switched off in config");
        s.freeEnabled = true;
        Check.eq("Free (first recovery)", LostFoundRules.priceText(LostFoundRules.decide(fresh, 1, s)), "free text");
        Check.eq("$16,000", LostFoundRules.priceText(LostFoundRules.decide(fresh, 20000, s)), "paid text");
        Check.eq("$9,999.99", LostFoundRules.money(9999.99), "money with cents");
        Check.eq("$1,326", LostFoundRules.money(1326.0), "money whole");
        Check.ok(LostFoundRules.decide(fresh, 1, s).equals(LostFoundRules.decide(fresh, 2, s)), "same price compares equal (offer check)");
        Check.ok(!LostFoundRules.decide(fresh, 1, s).equals(LostFoundRules.decide(fresh, 20000, s)), "free vs paid differ (price-changed path)");

        // globs
        TypeGlob g = new TypeGlob(List.of("diamond_*", "netherite_*", "elytra", "*shulker_box", "minecraft:trident", "  "));
        Check.ok(g.matches("minecraft:diamond_sword"), "diamond_* matches diamond_sword");
        Check.ok(g.matches("minecraft:netherite_ingot"), "netherite_* matches ingot");
        Check.ok(g.matches("minecraft:shulker_box") && g.matches("minecraft:red_shulker_box"), "*shulker_box matches plain and colored");
        Check.ok(g.matches("minecraft:trident") && g.matches("TRIDENT"), "minecraft: prefix + case-insensitive");
        Check.ok(!g.matches("minecraft:diamond"), "diamond_* needs the underscore part");
        Check.ok(!g.matches("minecraft:iron_sword"), "iron_sword not listed");
        Check.ok(!g.matches("minecraft:elytra_x"), "exact name does not prefix-match");
        Check.eq(5, g.size(), "blank pattern ignored");

        // menu order: valuables first, newest first
        List<BurnIndex.Entry> es = new ArrayList<>();
        es.add(new BurnIndex.Entry("a.yml", rec(ME, NOW - 1000, "minecraft:cobblestone")));
        es.add(new BurnIndex.Entry("b.yml", rec(ME, NOW - 5000, "minecraft:diamond_sword")));
        es.add(new BurnIndex.Entry("c.yml", rec(ME, NOW - 2000, "minecraft:dirt")));
        List<BurnIndex.Entry> ord = LostFoundRules.menuOrder(es, g, 45);
        Check.eq("b.yml,a.yml,c.yml", ord.get(0).fileName + "," + ord.get(1).fileName + "," + ord.get(2).fileName, "valuable first, then newest");
        Check.eq(2, LostFoundRules.menuOrder(es, g, 2).size(), "menu capped");

        // state round trip
        LostFoundState w = new LostFoundState(); w.freeUsed = true; w.freeUsedEpoch = 5; w.recoveries = 3; w.lastRecoveryEpoch = 7; w.tipOk = true;
        LostFoundState rd = LostFoundState.read(LostFoundState.write(w) + "future-key: 1\n");
        Check.ok(rd.freeUsed && rd.freeUsedEpoch == 5 && rd.recoveries == 3 && rd.lastRecoveryEpoch == 7 && rd.tipOk, "state round trip (unknown key ignored)");
        Check.ok(!LostFoundState.read("recoveries: nope\n").freeUsed && LostFoundState.read("recoveries: nope\n").recoveries == 0, "bad number keeps default");

        // CSV
        String row = LostFoundCsv.row(0, "=Sam", "minecraft:diamond_sword [sharpness 5]", "burned/x/1.yml", "$16,000", 16000, 40135.5, "ok");
        Check.eq("1970-01-01T00:00:00Z,'=Sam,minecraft:diamond_sword [sharpness 5],burned/x/1.yml,\"$16,000\",16000.00,40135.50,ok", row, "lostfound.csv row (formula guard + quoting)");
        Check.eq(RestoreCsv.HEADER, "time_utc,staff,source_player,death_file,item,target,item_no,count,source,forced", "restores.csv header unchanged");

        // attribution order
        Attribution.DropInfo t = new Attribution.DropInfo(); t.ownerUuid = "T"; t.ownerName = "Tracked";
        Check.eq("E", Attribution.resolve("E", "Ent", "S", "Stamp", "H", "Thr", t).uuid, "entity owner first");
        Check.eq("S", Attribution.resolve("", "", "S", "Stamp", "H", "Thr", t).uuid, "PDC stamp second (survives restart)");
        Check.eq("H", Attribution.resolve("", "", "", "", "H", "Thr", t).uuid, "thrower third");
        Check.eq("T", Attribution.resolve("", "", "", "", "", "", t).uuid, "tracker last");
        Check.eq("", Attribution.resolve("", "", "", "", "", "", null).uuid, "unknown when nobody knows");

        Check.eq("lava", LostFoundRules.causeWords("LAVA").replace("burned in ", ""), "cause words");
        Check.eq("7 days", LostFoundRules.daysText(7 * DAY), "days text");
    }
}
