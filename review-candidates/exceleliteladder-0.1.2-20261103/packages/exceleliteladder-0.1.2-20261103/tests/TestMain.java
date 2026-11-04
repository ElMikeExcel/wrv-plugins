import com.winkreyes.exceleliteladder.core.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Plain-JVM suite for the ExcelEliteLadder core (house pattern, no framework). */
public final class TestMain {
    static final Path FX = Paths.get("tests/fixtures");

    static String fx(String n) throws IOException {
        return new String(Files.readAllBytes(FX.resolve(n)), StandardCharsets.UTF_8);
    }

    /** What EliteMobs writes back on load: plain Double.toString number text. */
    static String canonical(String text) {
        StringBuilder out = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            String l = line;
            for (String k : MobCombatFile.KEYS) {
                if (line.startsWith(k + ":")) {
                    String v = line.substring(k.length() + 1).trim();
                    l = k + ": " + MobCombatFile.format(k, Double.parseDouble(v));
                }
            }
            out.append(l).append('\n');
        }
        return out.substring(0, out.length() - 1);
    }

    static final java.util.function.Function<String[], String> YAML_OK = ba -> null;

    public static void main(String[] a) throws Exception {
        System.out.println("Ladder table + fixtures");
        for (int i = 1; i <= 4; i++) {
            Map<String, Double> v = MobCombatFile.read(fx("S" + i + "-norm-MobCombatSettings.yml"));
            Check.eq(i, LadderTable.matchStep(v), "staged S" + i + "-norm file matches table row " + i);
        }
        // 0.1.2: the S5 row is identical to S4 by design, so the S5 fixture matches
        // row 5's values and matchStep resolves the peak file to the first peak row (4).
        Map<String, Double> v5 = MobCombatFile.read(fx("S5-norm-MobCombatSettings.yml"));
        Check.ok(LadderTable.matches(v5, LadderTable.step(5)), "staged S5-norm file matches table row 5 values");
        Check.eq(4, LadderTable.matchStep(v5), "peak file resolves to S4 (S5 row identical, first match wins)");
        Check.eq(0, LadderTable.matchStep(MobCombatFile.read(fx("old-S0-MobCombatSettings.yml"))), "old S0 is off ladder");
        Check.eq(0, LadderTable.matchStep(MobCombatFile.read(fx("old-S1-MobCombatSettings.yml"))), "old S1 (no norm) is off ladder");
        Check.ok(LadderTable.describeOffLadder(MobCombatFile.read(fx("old-S0-MobCombatSettings.yml")), LadderTable.step(1)).contains("OLD pre-ladder S0"), "old S0 refusal names it");
        Check.ok(LadderTable.describeOffLadder(MobCombatFile.read(fx("old-S1-MobCombatSettings.yml")), LadderTable.step(1)).contains("OLD S1 file without the dungeon lines"), "old S1 refusal names it");

        System.out.println("0.1.2: ladder tops out at Seal 4, compressed (Mike, Nov 1, 2026, 2:30 PM CST)");
        Check.close(0.11, LadderTable.step(4).values().get("eliteMobsSpawnPercentage"), "S4 spawn = 0.11 (compressed peak)");
        Check.close(150.0, LadderTable.step(4).values().get("naturalEliteMobsLevelCap"), "S4 cap = 150");
        Check.close(2.35, LadderTable.step(4).values().get("scaledDamageToPlayerMultiplier"), "S4 toPlayer = 2.35");
        Check.close(0.40, LadderTable.step(4).values().get("scaledDamageToEliteMultiplier"), "S4 toElite = 0.40");
        for (Map.Entry<String, Double> e : LadderTable.step(4).values().entrySet()) {
            Check.close(e.getValue(), LadderTable.step(5).values().get(e.getKey()), "S5 " + e.getKey() + " = S4 (no-op row)");
        }
        Check.close(120.0, LadderTable.step(3).values().get("naturalEliteMobsLevelCap"), "S3 cap stays 120");
        Check.close(2.05, LadderTable.step(3).values().get("scaledDamageToPlayerMultiplier"), "S3 toPlayer stays 2.05");
        String s4old = fx("S4-norm-MobCombatSettings.yml")
                .replace("eliteMobsSpawnPercentage: 0.11", "eliteMobsSpawnPercentage: 0.10")
                .replace("naturalEliteMobsLevelCap: 150", "naturalEliteMobsLevelCap: 120")
                .replace("scaledDamageToEliteMultiplier: 0.40", "scaledDamageToEliteMultiplier: 0.45")
                .replace("scaledDamageToPlayerMultiplier: 2.35", "scaledDamageToPlayerMultiplier: 2.20");
        Check.eq(0, LadderTable.matchStep(MobCombatFile.read(s4old)), "0.1.1-era S4 file (0.10/120/0.45/2.20) is off ladder in 0.1.2");
        String d4 = LadderTable.differences(MobCombatFile.read(s4old), LadderTable.step(4));
        Check.ok(d4.contains("eliteMobsSpawnPercentage=0.1 (want 0.11)") && d4.contains("naturalEliteMobsLevelCap=120 (want 150)")
                && d4.contains("scaledDamageToEliteMultiplier=0.45 (want 0.4)") && d4.contains("scaledDamageToPlayerMultiplier=2.2 (want 2.35)"),
                "and the refusal names exactly the four peak lines");
        String s5old = fx("S5-norm-MobCombatSettings.yml")
                .replace("eliteMobsSpawnPercentage: 0.11", "eliteMobsSpawnPercentage: 0.12")
                .replace("naturalEliteMobsLevelCap: 150", "naturalEliteMobsLevelCap: 120")
                .replace("scaledDamageToEliteMultiplier: 0.40", "scaledDamageToEliteMultiplier: 0.45")
                .replace("scaledDamageToPlayerMultiplier: 2.35", "scaledDamageToPlayerMultiplier: 2.20");
        Check.eq(0, LadderTable.matchStep(MobCombatFile.read(s5old)), "0.1.1-era S5 file (0.12/120/0.45/2.20) is off ladder in 0.1.2");

        System.out.println("Patch: each step from the previous, bytes = staged file in EliteMobs' number form");
        String cur = fx("S1-norm-MobCombatSettings.yml");
        Check.eq(cur, canonical(cur), "S1-norm is already in EliteMobs number form");
        for (int i = 2; i <= 5; i++) {
            String next = MobCombatFile.patch(cur, LadderTable.step(i));
            String staged = fx("S" + i + "-norm-MobCombatSettings.yml");
            Check.eq(canonical(staged), next, "S" + (i - 1) + " -> S" + i + " equals staged S" + i + "-norm (EM form)");
            Check.eq(null, MobCombatFile.verify(cur, next, LadderTable.step(i)), "verify S" + i + " ok");
            Check.eq(MobCombatFile.read(staged), MobCombatFile.read(next), "S" + i + " values equal staged values");
            cur = next;
        }
        String s1 = fx("S1-norm-MobCombatSettings.yml");
        Check.eq(s1, MobCombatFile.patch(s1, LadderTable.step(1)), "patching S1 onto S1 is a no-op (idempotent)");
        String crlf = s1.replace("\n", "\r\n");
        String crlf2 = MobCombatFile.patch(crlf, LadderTable.step(2));
        Check.eq(canonical(fx("S2-norm-MobCombatSettings.yml")).replace("\n", "\r\n"), crlf2, "CRLF file keeps CRLF");

        System.out.println("Reader refusals");
        expectThrow(() -> MobCombatFile.read(s1.replace("eliteMobsSpawnPercentage: 0.07", "x: 1")), "missing key");
        expectThrow(() -> MobCombatFile.read(s1 + "\nscaledDamageToPlayerMultiplier: 9.0"), "duplicate key");
        expectThrow(() -> MobCombatFile.read(s1.replace("naturalEliteMobsLevelCap: 120", "naturalEliteMobsLevelCap: lots")), "non-number");
        Check.ok(MobCombatFile.verify(s1, s1.replace("doWeakEffect: true", "doWeakEffect: false"), LadderTable.step(1)) != null, "verify flags a non-ladder line change");
        Check.ok(MobCombatFile.verify(s1, s1.substring(0, s1.length() / 2), LadderTable.step(1)) != null, "verify flags a truncated file");

        System.out.println("StepPlan");
        Check.eq(StepPlan.Action.APPLY, StepPlan.decide(true, 2, 1, 0, false).action(), "seal 2, empty -> apply");
        Check.eq(2, StepPlan.decide(true, 2, 1, 0, false).target(), "target S2");
        Check.eq(StepPlan.Action.WAIT_FOR_EMPTY, StepPlan.decide(true, 2, 1, 3, false).action(), "players on -> wait");
        Check.eq(StepPlan.Action.APPLY, StepPlan.decide(true, 2, 1, 3, true).action(), "players on + flag -> apply");
        Check.eq(StepPlan.Action.WARN_DOWNGRADE, StepPlan.decide(true, 0, 3, 0, false).action(), "reset -> warn, no step-down");
        Check.eq(StepPlan.Action.UP_TO_DATE, StepPlan.decide(true, 3, 3, 0, false).action(), "same -> nothing");
        Check.eq(5, StepPlan.decide(true, 7, 4, 0, false).target(), "seals clamp to 5");
        Check.eq(4, StepPlan.decide(true, 4, 1, 0, false).target(), "jump 1 -> 4 goes straight to S4");
        Check.eq(StepPlan.Action.NO_BASELINE, StepPlan.decide(true, 2, 0, 0, false).action(), "no baseline -> nothing");
        Check.eq(StepPlan.Action.NO_SEAL_SOURCE, StepPlan.decide(true, null, 1, 0, false).action(), "no GuildGate -> nothing");
        Check.eq(StepPlan.Action.DISABLED, StepPlan.decide(false, 2, 1, 0, false).action(), "disabled -> nothing");

        System.out.println("FileApplier (temp dirs)");
        Path dir = Files.createTempDirectory("ell");
        Path f = dir.resolve("MobCombatSettings.yml");
        Path bk = dir.resolve("backups");
        Files.writeString(f, s1);
        FileApplier.Result r = FileApplier.apply(f, bk, 1, LadderTable.step(2), "20261025-140000", YAML_OK, FileApplier.NONE);
        Check.ok(r.ok() && r.changed(), "S1 -> S2 applied");
        Check.eq(FileApplier.sha256(canonical(fx("S2-norm-MobCombatSettings.yml")).getBytes(StandardCharsets.UTF_8)), FileApplier.shaOf(f), "file sha = S2-norm (EM form)");
        Check.eq(s1, Files.readString(bk.resolve("MobCombatSettings.yml.20261025-140000-from-S1.bak")), "dated backup holds S1 bytes");
        Check.ok(!Files.exists(dir.resolve("MobCombatSettings.yml.eliteladder-tmp")), "no temp left");
        FileApplier.Result again = FileApplier.apply(f, bk, 1, LadderTable.step(2), "20261025-140000", YAML_OK, FileApplier.NONE);
        Check.ok(again.ok() && !again.changed(), "re-run with file already at target is a no-op");

        System.out.println("FileApplier (0.1.2 peak + Seal 5 no-op)");
        Path f2 = dir.resolve("MobCombatSettings-peak.yml");
        Files.writeString(f2, fx("S3-norm-MobCombatSettings.yml"));
        FileApplier.Result rPeak = FileApplier.apply(f2, bk, 3, LadderTable.step(4), "20261101-143000", YAML_OK, FileApplier.NONE);
        Check.ok(rPeak.ok() && rPeak.changed(), "S3 -> S4 compressed peak applied");
        Check.eq(FileApplier.sha256(canonical(fx("S4-norm-MobCombatSettings.yml")).getBytes(StandardCharsets.UTF_8)), FileApplier.shaOf(f2), "file sha = S4-peak (EM form)");
        String peakSha = FileApplier.shaOf(f2);
        FileApplier.Result rNoop = FileApplier.apply(f2, bk, 4, LadderTable.step(5), "20261101-143100", YAML_OK, FileApplier.NONE);
        Check.ok(rNoop.ok() && !rNoop.changed(), "Seal 5 step from the peak is a safe no-op");
        Check.eq(peakSha, FileApplier.shaOf(f2), "no-op left the peak file byte-identical");

        String sha2 = FileApplier.shaOf(f);
        FileApplier.Result bad = FileApplier.apply(f, bk, 3, LadderTable.step(4), "20261025-140100", YAML_OK, FileApplier.NONE);
        Check.ok(!bad.ok() && bad.error().startsWith("REFUSED"), "applied S3 but file is S2 -> refused");
        Check.eq(sha2, FileApplier.shaOf(f), "refusal changed nothing");

        Files.writeString(f, fx("old-S0-MobCombatSettings.yml"));
        String sha0 = FileApplier.shaOf(f);
        FileApplier.Result r0 = FileApplier.apply(f, bk, 1, LadderTable.step(2), "20261025-140200", YAML_OK, FileApplier.NONE);
        Check.ok(!r0.ok() && r0.error().contains("OLD pre-ladder S0"), "old S0 live -> refused with reason");
        Check.eq(sha0, FileApplier.shaOf(f), "old S0 untouched");
        Files.writeString(f, fx("old-S1-MobCombatSettings.yml"));
        FileApplier.Result r1 = FileApplier.apply(f, bk, 1, LadderTable.step(2), "20261025-140300", YAML_OK, FileApplier.NONE);
        Check.ok(!r1.ok() && r1.error().contains("OLD S1"), "old S1 live -> refused with reason");

        Files.writeString(f, s1);
        FileApplier.Result fb = FileApplier.apply(f, bk, 1, LadderTable.step(2), "20261025-140400", YAML_OK, new FileApplier.Faults() {
            @Override public void beforeMove(Path t) throws IOException { throw new IOException("disk full"); }
        });
        Check.ok(!fb.ok() && fb.error().contains("disk full") && fb.restored(), "write failure -> restored");
        Check.eq(s1, Files.readString(f), "file is S1 again after write failure");
        FileApplier.Result fa = FileApplier.apply(f, bk, 1, LadderTable.step(2), "20261025-140500", YAML_OK, new FileApplier.Faults() {
            @Override public void afterMove(Path t) throws IOException { byte[] b = Files.readAllBytes(t); Files.write(t, Arrays.copyOf(b, b.length / 2)); }
        });
        Check.ok(!fa.ok() && fa.error().contains("verify failed") && fa.restored(), "corrupt write -> verify fails -> restored");
        Check.eq(s1, Files.readString(f), "file is S1 again after corrupt write");
        FileApplier.Result fy = FileApplier.apply(f, bk, 1, LadderTable.step(2), "20261025-140600", ba -> "YAML parse failed: test", FileApplier.NONE);
        Check.ok(!fy.ok() && fy.restored(), "YAML check failure -> restored");
        Check.eq(s1, Files.readString(f), "file is S1 again after YAML failure");
        Check.ok(Files.exists(bk.resolve("MobCombatSettings.yml.20261025-140000-from-S1.bak")), "earlier backup never overwritten");

        System.out.println();
        System.out.println(Check.passed + " passed, " + Check.failed + " failed");
        if (Check.failed > 0) System.exit(1);
    }

    static void expectThrow(Runnable r, String name) {
        try { r.run(); Check.ok(false, name + " refused"); }
        catch (LadderException e) { Check.ok(true, name + " refused (" + e.getMessage() + ")"); }
    }
}
