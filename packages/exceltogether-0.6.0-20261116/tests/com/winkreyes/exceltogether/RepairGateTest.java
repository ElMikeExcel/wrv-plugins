package com.winkreyes.exceltogether;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelTogether 0.5.2 unit tests for the Repair Equipment confirmation gate:
 * RepairGate's arm / confirm / expire / lockout state machine, its prompt
 * texts, and RepairCooldownStore's persistence round-trip (the lockout must
 * survive a restart - proven here at the file level, and on the rig at the
 * server level). Pure logic plus a temp file; no server needed.
 * Run: java -cp <classes> com.winkreyes.exceltogether.RepairGateTest
 */
public class RepairGateTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }

    static final long WINDOW = 10_000L;
    static final long COOLDOWN = 14_400_000L;   // 4 hours

    public static void main(String[] args) throws Exception {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        long t0 = 1_000_000_000L;

        // ---- the state machine -------------------------------------------
        RepairGate gate = new RepairGate(WINDOW, COOLDOWN, RepairGate.Texts.defaults());
        check("fresh caster arms", gate.verdict(a, t0).decision == RepairGate.Decision.ARM);
        check("verdict alone never arms or locks", gate.verdict(a, t0).decision == RepairGate.Decision.ARM
                && gate.remainingMs(a, t0) == 0);

        gate.arm(a, t0);
        check("cast inside the window confirms", gate.verdict(a, t0 + 1).decision == RepairGate.Decision.CONFIRM);
        check("verdict is read-only: still confirmable", gate.verdict(a, t0 + 2).decision == RepairGate.Decision.CONFIRM);
        check("an armed caster is not locked", gate.remainingMs(a, t0 + 2) == 0);

        gate.confirm(a, t0 + 3);
        RepairGate.Verdict locked = gate.verdict(a, t0 + 4);
        check("confirmed cast locks", locked.decision == RepairGate.Decision.LOCKED);
        check("lockout remaining is the full cooldown, minus a breath",
                locked.remainingMs > COOLDOWN - 1000 && locked.remainingMs <= COOLDOWN);
        check("remaining counts down", gate.remainingMs(a, t0 + 3 + 3_600_000L) == COOLDOWN - 3_600_000L);
        check("lockout is per-caster", gate.verdict(b, t0 + 4).decision == RepairGate.Decision.ARM);
        check("lockout ends at the cooldown boundary",
                gate.verdict(a, t0 + 3 + COOLDOWN).decision == RepairGate.Decision.ARM);

        // ---- a lapsed arm costs nothing ----------------------------------
        RepairGate gate2 = new RepairGate(WINDOW, COOLDOWN, RepairGate.Texts.defaults());
        gate2.arm(a, t0);
        check("window edge still confirms", gate2.verdict(a, t0 + WINDOW - 1).decision == RepairGate.Decision.CONFIRM);
        check("lapsed arm re-arms, never locks", gate2.verdict(a, t0 + WINDOW).decision == RepairGate.Decision.ARM
                && gate2.remainingMs(a, t0 + WINDOW) == 0);
        gate2.arm(a, t0 + WINDOW);
        gate2.confirm(a, t0 + WINDOW + 1);
        check("re-arm then confirm still works after a lapse",
                gate2.verdict(a, t0 + WINDOW + 2).decision == RepairGate.Decision.LOCKED);

        // ---- persistence: snapshot / restore ------------------------------
        RepairGate gate3 = new RepairGate(WINDOW, COOLDOWN, RepairGate.Texts.defaults());
        gate3.confirm(a, t0);
        gate3.confirm(b, t0 - COOLDOWN);   // already expired at snapshot time
        Map<UUID, Long> snap = gate3.snapshotConfirmed(t0 + 1000);
        check("snapshot keeps the live lockout", snap.containsKey(a) && snap.get(a) == t0);
        check("snapshot prunes expired lockouts", !snap.containsKey(b));
        RepairGate restored = new RepairGate(WINDOW, COOLDOWN, RepairGate.Texts.defaults());
        restored.restoreConfirmed(snap);
        check("restored gate still locks the caster", restored.verdict(a, t0 + 1000).decision == RepairGate.Decision.LOCKED);
        check("restored remaining matches", restored.remainingMs(a, t0 + 1000) == COOLDOWN - 1000);

        // ---- persistence: the store's file round-trip ---------------------
        RepairCooldownStore store = new RepairCooldownStore();
        File tmp = File.createTempFile("repair-cooldowns", ".yml");
        tmp.deleteOnExit();
        Map<UUID, Long> data = new HashMap<>();
        data.put(a, t0);
        data.put(b, t0 + 5);
        store.save(tmp, data, null);
        check("store round-trips the confirmed map", store.load(tmp, null).equals(data));
        File missing = new File(tmp.getParent(), "no-such-cooldowns-file.yml");
        check("missing file loads as a clean slate", store.load(missing, null).isEmpty());
        Files.writeString(tmp.toPath(), "cooldowns:\n  not-a-uuid: 123\n  " + a + ": " + t0 + "\n");
        Map<UUID, Long> partial = store.load(tmp, null);
        check("malformed keys are skipped, good ones kept", partial.size() == 1 && partial.get(a) == t0);
        Files.writeString(tmp.toPath(), "::: not yaml at all :::\n\t- broken");
        check("unreadable file loads as a clean slate, no throw", store.load(tmp, null) != null);

        // ---- the texts -----------------------------------------------------
        RepairGate text = new RepairGate(WINDOW, COOLDOWN, RepairGate.Texts.defaults());
        String prompt = text.promptText(7, 2, 3);
        check("prompt carries the live count", prompt.contains("7 damaged item(s)") && prompt.contains("2 party member(s)"));
        check("prompt names the window and the cooldown", prompt.contains("10 seconds") && prompt.contains("4h 0m"));
        check("prompt has no placeholders left", !prompt.contains("{"));
        String none = text.promptText(0, 0, 3);
        check("zero-count prompt is honest about the burn", none.contains("no damaged gear") && none.contains("3 party member(s)")
                && none.contains("still starts your 4h 0m cooldown"));
        String refused = text.refusedText(11_340_000L);
        check("refusal carries the remaining time", refused.contains("3h 9m remaining"));
        RepairGate custom = new RepairGate(30_000L, 60_000L,
                new RepairGate.Texts("P {items}/{members}/{seconds}/{cooldown}", "N {members}", "R {remaining}"));
        check("custom texts and values flow through", custom.promptText(1, 1, 1).equals("P 1/1/30/1m")
                && custom.promptText(0, 0, 4).equals("N 4") && custom.refusedText(30_000L).equals("R 30s"));
        check("blank texts fall back to the defaults",
                new RepairGate.Texts("", null, " ").prompt.equals(RepairGate.Texts.DEFAULT_PROMPT));

        // ---- duration formatting -------------------------------------------
        check("formatDuration zero", RepairGate.formatDuration(0).equals("0s"));
        check("formatDuration seconds", RepairGate.formatDuration(59_000L).equals("59s"));
        check("formatDuration sub-minute seconds round up", RepairGate.formatDuration(59_500L).equals("60s"));
        check("formatDuration one minute", RepairGate.formatDuration(60_000L).equals("1m"));
        check("formatDuration minutes", RepairGate.formatDuration(600_000L).equals("10m"));
        check("formatDuration hours", RepairGate.formatDuration(COOLDOWN).equals("4h 0m"));
        check("formatDuration hours+minutes", RepairGate.formatDuration(11_520_000L).equals("3h 12m"));

        System.out.println("RepairGateTest: " + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
