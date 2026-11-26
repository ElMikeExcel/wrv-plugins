package excelemfixes.bedrockabilities;

import java.util.List;

/**
 * Plain-JVM suite for ExcelEliteMobsAbilityBedrockBridge's bridge-side logic. Runs in
 * two modes (see run-tests.sh):
 *  -DprobeExpect=UNAVAILABLE : EliteMobs jar deliberately ABSENT from
 *      the runtime classpath - AcsProbe must report UNAVAILABLE and
 *      must not throw (the fail-soft contract).
 *  -DprobeExpect=NOT_READY   : EliteMobs jar present but no server -
 *      AcsProbe must not claim READY and must not throw.
 * Exit code 0 = all checks passed.
 */
public final class BridgeTestMain {

    static int checks = 0;
    static int failures = 0;

    static void check(String name, boolean ok, String detail) {
        checks++;
        if (ok) {
            System.out.println("PASS " + name);
        } else {
            failures++;
            System.out.println("FAIL " + name + " :: " + detail);
        }
    }

    static void eq(String name, Object expected, Object actual) {
        check(name, expected == null ? actual == null : expected.equals(actual),
                "expected <" + expected + "> but was <" + actual + ">");
    }

    public static void main(String[] args) {
        labels();
        failureMessages();
        noClassModel();
        fullClassModel();
        emptySlotModel();
        noResourceModel();
        classViewHelpers();
        tapPipeline();
        probe();

        System.out.println("----");
        System.out.println(checks + " checks, " + failures + " failures");
        System.exit(failures == 0 ? 0 : 1);
    }

    // ----------------------------------------------------------------

    static void labels() {
        eq("number whole", "100", Labels.number(100.0));
        eq("number cost", "40", Labels.number(40.0));
        eq("number zero", "0", Labels.number(0.0));
        eq("number negative", "-3", Labels.number(-3.0));
        eq("number decimal", "65.5", Labels.number(65.5));
        eq("number null", "?", Labels.number(null));

        eq("prettify snake", "Divine Steed", Labels.prettifyId("divine_steed"));
        eq("prettify dotted slot tail", "Paladin Signature", Labels.prettifyId("paladin.signature"));
        eq("prettify plain", "Provoke", Labels.prettifyId("provoke"));
        eq("prettify null", null, Labels.prettifyId(null));
        eq("prettify blank", null, Labels.prettifyId("  "));

        eq("label live name wins", "Provoke",
                Labels.resolveAbilityLabel("Provoke", "paladin.signature"));
        eq("label falls back to id", "Divine Steed",
                Labels.resolveAbilityLabel("  ", "divine_steed"));
        eq("label both blank", null, Labels.resolveAbilityLabel(null, null));
    }

    static void failureMessages() {
        for (BridgeFailure f : BridgeFailure.values()) {
            String line = FailureMessages.line(f);
            check("failure line non-blank " + f, line != null && !line.isBlank(),
                    "line was <" + line + ">");
        }
        check("invalid player wording",
                FailureMessages.line(BridgeFailure.INVALID_PLAYER).contains("isn't available"),
                FailureMessages.line(BridgeFailure.INVALID_PLAYER));
        check("no corpse wording",
                FailureMessages.line(BridgeFailure.NO_CORPSE).contains("corpse"),
                FailureMessages.line(BridgeFailure.NO_CORPSE));
        check("no valid target wording",
                FailureMessages.line(BridgeFailure.NO_VALID_TARGET).contains("target"),
                FailureMessages.line(BridgeFailure.NO_VALID_TARGET));
        eq("null failure maps safe", FailureMessages.line(BridgeFailure.UNKNOWN),
                FailureMessages.line(null));
    }

    static ClassView paladin(boolean utilityPresent, boolean utilityAffordable) {
        List<SlotView> slots = List.of(
                new SlotView(BridgeSlot.MOBILITY, true, "Divine Steed", 55.0, true, "Resolve"),
                new SlotView(BridgeSlot.SIGNATURE, true, "Provoke", 40.0, true, "Resolve"),
                utilityPresent
                        ? new SlotView(BridgeSlot.UTILITY, true, "Repulse", 30.0, utilityAffordable, "Resolve")
                        : SlotView.absent(BridgeSlot.UTILITY));
        return new ClassView(true, "Paladin", "Resolve", 65.0, 100.0, slots);
    }

    static void noClassModel() {
        FormModel model = FormModelBuilder.build(ClassView.noClass());
        eq("noclass title", "Class Abilities", model.title());
        check("noclass content plain",
                model.content().contains("don't have a class yet")
                        && model.content().contains("/em class"),
                model.content());
        eq("noclass buttons", List.of("Close"), model.buttons());
        eq("noclass close maps null", null, model.slotForButton(0));
        eq("noclass out of range", null, model.slotForButton(5));
        eq("noclass negative", null, model.slotForButton(-1));
        FormModel nullView = FormModelBuilder.build(null);
        eq("null view behaves as noclass", List.of("Close"), nullView.buttons());
    }

    static void fullClassModel() {
        FormModel model = FormModelBuilder.build(paladin(true, false));
        check("full header line",
                model.content().contains("Paladin - Resolve 65 / 100"),
                model.content());
        check("full signature line",
                model.content().contains("Signature: Provoke - 40 Resolve"),
                model.content());
        check("full unaffordable marked",
                model.content().contains("Utility: Repulse - 30 Resolve (not enough yet)"),
                model.content());
        eq("full buttons",
                List.of("Divine Steed (Mobility)", "Provoke (Signature)",
                        "Repulse (Utility)", "Close"),
                model.buttons());
        eq("full map 0", BridgeSlot.MOBILITY, model.slotForButton(0));
        eq("full map 1", BridgeSlot.SIGNATURE, model.slotForButton(1));
        eq("full map 2", BridgeSlot.UTILITY, model.slotForButton(2));
        eq("full map close", null, model.slotForButton(3));
    }

    static void emptySlotModel() {
        FormModel model = FormModelBuilder.build(paladin(false, false));
        check("empty slot stated plainly",
                model.content().contains("Utility: nothing learned yet"),
                model.content());
        eq("empty slot gets no button",
                List.of("Divine Steed (Mobility)", "Provoke (Signature)", "Close"),
                model.buttons());
        eq("empty map 1", BridgeSlot.SIGNATURE, model.slotForButton(1));
        eq("empty map close", null, model.slotForButton(2));
    }

    static void noResourceModel() {
        List<SlotView> slots = List.of(
                new SlotView(BridgeSlot.MOBILITY, false, null, null, false, null),
                new SlotView(BridgeSlot.SIGNATURE, true, "Provoke", null, true, null),
                SlotView.absent(BridgeSlot.UTILITY));
        ClassView view = new ClassView(true, "Paladin", null, null, null, slots);
        FormModel model = FormModelBuilder.build(view);
        check("noresource class named", model.content().contains("Paladin"),
                model.content());
        check("noresource no null leak", !model.content().contains("null"),
                model.content());
        check("noresource costless line",
                model.content().contains("Signature: Provoke"),
                model.content());
        eq("noresource buttons", List.of("Provoke (Signature)", "Close"), model.buttons());
    }

    static void classViewHelpers() {
        ClassView none = ClassView.noClass();
        check("noClass anyPresent false", !none.anyPresent(), "");
        eq("noClass slot absent", false, none.slot(BridgeSlot.SIGNATURE).present());
        ClassView full = paladin(true, true);
        check("full anyPresent true", full.anyPresent(), "");
        eq("full slot lookup", "Provoke", full.slot(BridgeSlot.SIGNATURE).abilityName());
    }

    static void tapPipeline() {
        BridgeStubGateway stub = new BridgeStubGateway();
        stub.view = paladin(true, true);
        // The exact pipeline AbilitiesCommand runs: view -> model -> tap -> fire.
        FormModel model = FormModelBuilder.build(stub.classView(null));
        BridgeSlot tapped = model.slotForButton(1);
        eq("tap resolves signature", BridgeSlot.SIGNATURE, tapped);
        FireOutcome outcome = stub.fire(null, tapped);
        check("tap fire succeeded", outcome.successful(), outcome.toString());
        eq("tap fired once", 1, stub.firedSlots.size());
        eq("tap fired signature", BridgeSlot.SIGNATURE, stub.firedSlots.get(0));

        stub.nextOutcome = FireOutcome.failure("paladin.signature", BridgeFailure.NO_VALID_TARGET);
        FireOutcome refused = stub.fire(null, model.slotForButton(1));
        check("refusal carries reason", !refused.successful()
                        && refused.failure() == BridgeFailure.NO_VALID_TARGET,
                refused.toString());
        check("refusal line plain",
                FailureMessages.line(refused.failure()).contains("target"),
                FailureMessages.line(refused.failure()));

        stub.ready = false;
        check("stub not-ready gate", !stub.moduleReady(), "");
    }

    static void probe() {
        String expect = System.getProperty("probeExpect", "");
        AcsProbe.Result result;
        try {
            result = AcsProbe.probe();
        } catch (Throwable t) {
            check("probe never throws", false, "threw " + t);
            return;
        }
        check("probe never throws", true, "");
        if (expect.equals("UNAVAILABLE")) {
            eq("probe absent EM -> UNAVAILABLE", AcsProbe.Result.UNAVAILABLE, result);
        } else if (expect.equals("NOT_READY")) {
            check("probe with EM jar, no server -> not READY",
                    result != AcsProbe.Result.READY, "was " + result);
            System.out.println("INFO probe with EM present but unbooted server: " + result);
        }
    }
}
