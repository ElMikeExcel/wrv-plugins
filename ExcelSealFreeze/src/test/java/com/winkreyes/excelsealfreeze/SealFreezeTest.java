package com.winkreyes.excelsealfreeze;

import com.winkreyes.excelsealfreeze.core.BaselineStore;
import com.winkreyes.excelsealfreeze.core.ClampEngine;
import com.winkreyes.excelsealfreeze.core.ClampPolicy;
import com.winkreyes.excelsealfreeze.core.Progression;
import com.winkreyes.excelsealfreeze.core.SealFlag;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Desk harness for ExcelSealFreeze (house pattern: plain main, PASS/FAIL
 * lines, non-zero exit on any failure). The database section runs against a
 * THROWAWAY COPY of the real pulled EM database (-Dem.db=...); the pull
 * itself is opened read-only by the copy step and never written.
 */
public final class SealFreezeTest {

    private static int failures = 0;

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failures++;
    }

    private static Progression.ProgRow row(String uuid, long hlk, long score, long kills,
                                           double cur, long cents, long xp0) {
        long[] xp = new long[Progression.SKILL_COLUMNS.length];
        xp[0] = xp0;
        return new Progression.ProgRow(uuid, hlk, score, kills, cur, cents, xp);
    }

    public static void main(String[] args) throws Exception {
        // ---------- ClampPolicy ----------
        Progression.ProgRow base = row("u1", 0, 0, 0, 0.0, 0, 0);
        Progression.ProgRow gained = row("u1", 30, 500, 12, 42.5, 4250, 900);
        List<ClampPolicy.Clamp> clamps = ClampPolicy.diff(base, gained);
        check("policy: all-zero baseline clamps every gained field", clamps.size() == 6);
        check("policy: update SQL shape",
                "UPDATE PlayerData SET \"HighestLevelKilled\" = ?, \"Score\" = ?, \"Kills\" = ?, "
                        .concat("\"CurrencyV2\" = ?, \"CurrencyCents\" = ?, \"SkillXP_ARMOR\" = ? WHERE PlayerUUID = ?")
                        .equals(ClampPolicy.updateSql(clamps)));

        Progression.ProgRow belowBase = row("u1", 0, -250, 0, 0.0, 0, 0);
        check("policy: value below baseline (negative score from deaths) is untouched",
                ClampPolicy.diff(base, belowBase).isEmpty());
        check("policy: current == baseline is untouched",
                ClampPolicy.diff(base, base).isEmpty());
        check("policy: null baseline means zero baseline (freeze-era joiner)",
                ClampPolicy.diff(null, gained).size() == 6);
        check("policy: empty diff produces no SQL", ClampPolicy.updateSql(List.of()) == null);

        Progression.ProgRow partial = row("u1", 5, 0, 3, 0.0, 0, 0);
        Progression.ProgRow partialBase = row("u1", 5, 0, 0, 0.0, 0, 0);
        List<ClampPolicy.Clamp> pc = ClampPolicy.diff(partialBase, partial);
        check("policy: per-column independence (only Kills clamps)", pc.size() == 1
                && pc.get(0).column.equals("Kills") && pc.get(0).longValue == 0L);

        Progression.ProgRow centsOnly = row("u1", 0, 0, 0, 0.0, 700, 0);
        List<ClampPolicy.Clamp> cc = ClampPolicy.diff(base, centsOnly);
        check("policy: currency cents clamp independently of the double",
                cc.size() == 1 && cc.get(0).column.equals("CurrencyCents"));

        // ---------- SealFlag ----------
        String lockedYml = "unlocked: false\nunlockTimestamp: 0\nseals:\n  elder:\n    broken: true\n"
                + "  dragon:\n    broken: true\n  raid:\n    broken: true\n  wither:\n    broken: true\n"
                + "  warden:\n    broken: false\n";
        check("sealflag: locked state reads locked", !SealFlag.isUnlocked(lockedYml));
        check("sealflag: broken count reads 4 of 5", SealFlag.brokenCount(lockedYml) == 4);
        String openYml = lockedYml.replace("unlocked: false", "unlocked: true")
                .replace("broken: false", "broken: true");
        check("sealflag: unlocked state reads unlocked", SealFlag.isUnlocked(openYml));
        check("sealflag: broken count reads 5 of 5", SealFlag.brokenCount(openYml) == 5);
        check("sealflag: nested lookalike 'unlocked' does not lift the freeze",
                !SealFlag.isUnlocked("seals:\n  unlocked: true\n"));
        check("sealflag: unlockTimestamp alone does not lift the freeze",
                !SealFlag.isUnlocked("unlockTimestamp: 1763000000000\n"));
        check("sealflag: null/empty text reads locked (fail-safe)",
                !SealFlag.isUnlocked(null) && !SealFlag.isUnlocked(""));

        // ---------- BaselineStore ----------
        Map<String, Progression.ProgRow> snap = new LinkedHashMapForTest();
        snap.put("u1", row("u1", 22, 3674, 220, 7.0, 700, 3550));
        snap.put("u2", row("u2", 0, -988, 10, 0.0, 0, 0));
        String text = BaselineStore.serialize(snap, 1763000000000L);
        Map<String, Progression.ProgRow> back = BaselineStore.parse(text);
        check("baseline: round-trip preserves rows", back.size() == 2
                && back.get("u1").highestLevelKilled == 22
                && back.get("u1").currencyV2 == 7.0
                && back.get("u1").skillXp[0] == 3550
                && back.get("u2").score == -988);
        Map<String, Progression.ProgRow> withJunk = BaselineStore.parse(text + "garbage line\nx|1|2\n");
        check("baseline: malformed lines are skipped, good rows survive", withJunk.size() == 2);

        // ---------- ClampEngine against a throwaway copy of the real EM DB ----------
        String dbPath = System.getProperty("em.db");
        if (dbPath == null) {
            check("engine: -Dem.db not set (database section skipped is a FAILURE in this harness)", false);
        } else {
            engineSection(Path.of(dbPath));
        }

        System.out.println(failures == 0 ? "ALL CHECKS PASSED" : failures + " CHECK(S) FAILED");
        if (failures > 0) System.exit(1);
    }

    private static void engineSection(Path sourceDb) throws Exception {
        Path tmp = Files.createTempFile("sealfreeze-test", ".db");
        Files.copy(sourceDb, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Class.forName("org.sqlite.JDBC");
        String url = "jdbc:sqlite:" + tmp.toAbsolutePath();
        Map<String, Long> deathsBefore = new HashMap<>();
        Map<String, Progression.ProgRow> baseline;
        try (Connection c = DriverManager.getConnection(url)) {
            baseline = ClampEngine.readAll(c);
            check("engine: reads all 11 real rows", baseline.size() == 11);
            check("engine: real values match the G4 record (Jester 22 / Marvin 23)",
                    baseline.values().stream().anyMatch(r -> r.highestLevelKilled == 22)
                            && baseline.values().stream().anyMatch(r -> r.highestLevelKilled == 23));
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT PlayerUUID, Deaths FROM PlayerData")) {
                while (rs.next()) deathsBefore.put(rs.getString(1), rs.getLong(2));
            }
            // Simulate freeze-era gains: a level-30 elite kill spree by the HLK-23 player,
            // plus a skill-XP gain, a currency gain, and a death-driven score drop elsewhere.
            String top = baseline.entrySet().stream()
                    .max(Map.Entry.comparingByValue((a, b) -> Long.compare(a.highestLevelKilled, b.highestLevelKilled)))
                    .orElseThrow().getKey();
            try (Statement st = c.createStatement()) {
                st.executeUpdate("UPDATE PlayerData SET HighestLevelKilled = 30, Score = Score + 400, "
                        + "Kills = Kills + 9, SkillXP_SWORDS = SkillXP_SWORDS + 250, "
                        + "CurrencyV2 = 42.5, CurrencyCents = 4250 WHERE PlayerUUID = '" + top + "'");
                st.executeUpdate("UPDATE PlayerData SET Score = Score - 100 WHERE PlayerUUID != '" + top + "'");
            }
            ClampEngine.Report report = ClampEngine.clampAll(c, baseline);
            check("engine: clamp cycle clamps exactly the gaining row", report.rowsClamped == 1);
            check("engine: clamp cycle clamps exactly the 6 gained columns", report.columnsClamped == 6);
            Map<String, Progression.ProgRow> after = ClampEngine.readAll(c);
            Progression.ProgRow topAfter = after.get(top);
            Progression.ProgRow topBase = baseline.get(top);
            check("engine: gains fully returned to baseline", topAfter.highestLevelKilled == topBase.highestLevelKilled
                    && topAfter.score == topBase.score && topAfter.kills == topBase.kills
                    && topAfter.currencyV2 == topBase.currencyV2 && topAfter.currencyCents == topBase.currencyCents
                    && topAfter.skillXp[1] == topBase.skillXp[1]);
            boolean negativesKept = after.entrySet().stream()
                    .filter(e -> !e.getKey().equals(top))
                    .allMatch(e -> e.getValue().score == baseline.get(e.getKey()).score - 100);
            check("engine: score losses (deaths) are NOT clamped back up", negativesKept);
            boolean deathsSame = true;
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT PlayerUUID, Deaths FROM PlayerData")) {
                while (rs.next()) {
                    if (deathsBefore.getOrDefault(rs.getString(1), -1L) != rs.getLong(2)) deathsSame = false;
                }
            }
            check("engine: Deaths column untouched on every row", deathsSame);

            // Single-row clamp path (join/quit enforcement).
            try (Statement st = c.createStatement()) {
                st.executeUpdate("UPDATE PlayerData SET Kills = Kills + 3 WHERE PlayerUUID = '" + top + "'");
            }
            ClampEngine.Report one = ClampEngine.clampOne(c, baseline, top);
            check("engine: single-row clamp restores the one row", one.rowsClamped == 1
                    && ClampEngine.readOne(c, top).kills == topBase.kills);
        } finally {
            Files.deleteIfExists(tmp);
            File wal = new File(tmp + "-wal");
            if (wal.exists()) Files.deleteIfExists(wal.toPath());
            File shm = new File(tmp + "-shm");
            if (shm.exists()) Files.deleteIfExists(shm.toPath());
        }
    }

    /** LinkedHashMap with a tiny literal name so imports stay obvious in the harness. */
    private static final class LinkedHashMapForTest extends java.util.LinkedHashMap<String, Progression.ProgRow> {
    }
}
