import com.winkreyes.excelitems.core.BuiltinItems;
import com.winkreyes.excelitems.core.ItemDefinition;
import com.winkreyes.excelitems.core.ItemRegistry;
import excelemfixes.fieldmedic.BandageItem;
import excelemfixes.fieldmedic.FieldMedicEngine;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Field Medic suites: the bandage state machine (schedule,
 * refusals, cooldown stores) and the registry contract (the
 * definition, its registration into the real ExcelItems registry
 * classes, and the 0.1.0 config-validation trap that forbids an
 * ExcelItems config line for ID 6).
 */
public class FieldMedicTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-00000000f1e1");
    private static final long T0 = 1_700_000_000_000L;

    private static FieldMedicEngine engine() {
        return new FieldMedicEngine(new FieldMedicEngine.Settings(35.0, 5, 60_000L));
    }

    private static boolean throwsIAE(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        }
    }

    public static void run() {
        System.out.println("FieldMedicTest:");
        engineSchedule();
        engineRefusals();
        engineStores();
        engineValidation();
        definition();
        registryContract();
    }

    // ------------------------------------------------------------ schedule

    private static void engineSchedule() {
        FieldMedicEngine e = engine();
        Check.eq(FieldMedicEngine.UseResult.STARTED,
                e.tryUse(PLAYER, T0).result(), "first use starts");
        Check.close(0.0, e.tick(PLAYER, T0 + 500), "nothing due before 1 s");
        double total = 0.0;
        for (int s = 1; s <= 5; s++) {
            double offer = e.tick(PLAYER, T0 + s * 1000L);
            Check.close(7.0, offer, "second " + s + " offers 7%");
            total += offer;
        }
        Check.close(35.0, total, "five seconds offer exactly 35%");
        Check.ok(!e.isActive(PLAYER, T0 + 5000), "heal complete at 5 s");
        Check.close(0.0, e.tick(PLAYER, T0 + 6000), "no offers after completion");
        Check.ok(e.bandagingPlayers().isEmpty(), "work set empty after completion");

        // A lagged tick catches up instead of losing healing.
        FieldMedicEngine lag = engine();
        lag.tryUse(PLAYER, T0);
        Check.close(21.0, lag.tick(PLAYER, T0 + 3500), "lagged tick at 3.5 s offers 21%");
        Check.close(14.0, lag.tick(PLAYER, T0 + 5000), "final tick offers the remaining 14%");

        // A non-divisible window still totals exactly the heal percent.
        FieldMedicEngine six = new FieldMedicEngine(new FieldMedicEngine.Settings(35.0, 6, 0L));
        six.tryUse(PLAYER, T0);
        double sum = 0.0;
        for (int s = 1; s <= 6; s++) sum += six.tick(PLAYER, T0 + s * 1000L);
        Check.close(35.0, sum, "35% over 6 s totals exactly 35%");
        Check.ok(!six.isActive(PLAYER, T0 + 6000), "6 s heal complete at 6 s");
    }

    // ------------------------------------------------------------ refusals

    private static void engineRefusals() {
        FieldMedicEngine e = engine();
        e.tryUse(PLAYER, T0);
        Check.eq(FieldMedicEngine.UseResult.REFUSED_ACTIVE,
                e.tryUse(PLAYER, T0 + 2000).result(), "second use while bandaging refused");
        for (int s = 1; s <= 5; s++) e.tick(PLAYER, T0 + s * 1000L);
        FieldMedicEngine.UseOutcome duringCooldown = e.tryUse(PLAYER, T0 + 30_000L);
        Check.eq(FieldMedicEngine.UseResult.REFUSED_COOLDOWN,
                duringCooldown.result(), "use during cooldown refused");
        Check.close(30_000.0, duringCooldown.cooldownRemainingMillis(),
                "cooldown refusal reports the remaining 30 s");
        Check.eq(FieldMedicEngine.UseResult.STARTED,
                e.tryUse(PLAYER, T0 + 60_000L).result(), "use at cooldown end starts");
    }

    // ------------------------------------------------------------ the two stores

    private static void engineStores() {
        // Death clears both stores: a fresh life gets a fresh decision.
        FieldMedicEngine death = engine();
        death.tryUse(PLAYER, T0);
        death.forget(PLAYER);
        Check.eq(FieldMedicEngine.UseResult.STARTED,
                death.tryUse(PLAYER, T0 + 1000).result(), "death clears heal and cooldown");

        // Quit ends the heal (benefit forfeited) but the cooldown stands.
        FieldMedicEngine quit = engine();
        quit.tryUse(PLAYER, T0);
        quit.endHeal(PLAYER);
        Check.ok(!quit.isActive(PLAYER, T0 + 1000), "quit ends the heal");
        Check.eq(FieldMedicEngine.UseResult.REFUSED_COOLDOWN,
                quit.tryUse(PLAYER, T0 + 1000).result(), "quit keeps the cooldown");

        // A cooldown read back from persistent data (the relog path)
        // refuses exactly like a live one.
        FieldMedicEngine relog = engine();
        relog.restoreCooldown(PLAYER, T0 + 45_000L);
        FieldMedicEngine.UseOutcome outcome = relog.tryUse(PLAYER, T0);
        Check.eq(FieldMedicEngine.UseResult.REFUSED_COOLDOWN,
                outcome.result(), "restored cooldown refuses");
        Check.close(45_000.0, outcome.cooldownRemainingMillis(),
                "restored cooldown reports its remaining 45 s");
        Check.eq(FieldMedicEngine.UseResult.STARTED,
                relog.tryUse(PLAYER, T0 + 45_000L).result(), "restored cooldown expires on time");

        // Restoring a shorter stored value never shortens a live cooldown.
        FieldMedicEngine keep = engine();
        keep.tryUse(PLAYER, T0);
        keep.restoreCooldown(PLAYER, T0 + 10_000L);
        Check.close(60_000.0, keep.cooldownUntilMillis(PLAYER) - T0,
                "shorter stored cooldown does not shorten the live one");
    }

    private static void engineValidation() {
        Check.ok(throwsIAE(() -> new FieldMedicEngine.Settings(0.0, 5, 60_000L)),
                "heal percent 0 rejected");
        Check.ok(throwsIAE(() -> new FieldMedicEngine.Settings(100.1, 5, 60_000L)),
                "heal percent over 100 rejected");
        Check.ok(throwsIAE(() -> new FieldMedicEngine.Settings(35.0, 0, 60_000L)),
                "duration 0 rejected");
        Check.ok(throwsIAE(() -> new FieldMedicEngine.Settings(35.0, 5, -1L)),
                "negative cooldown rejected");
    }

    // ------------------------------------------------------------ definition + registry

    private static void definition() {
        ItemDefinition def = BandageItem.DEFINITION;
        Check.eq(6, def.id(), "bandage registry id is 6");
        Check.eq("field_bandage", def.key(), "bandage registry key");
        Check.eq(ItemDefinition.Kind.ITEM, def.kind(), "bandage is a physical item");
        Check.eq("PAPER", def.material(), "bandage material is PAPER");
        Check.eq("excelemfixes:field-medic", def.handlerId(), "bandage handler label");
        Check.ok(def.lore().get(def.lore().size() - 1).contains("$150"),
                "bandage lore carries the $150 shop line");
        Check.ok(!def.hasLegacyMark(), "bandage has no legacy mark (born in the registry era)");
    }

    private static void registryContract() {
        // The builtins hold 1-4; the bandage registers cleanly at 6.
        ItemRegistry registry = BuiltinItems.registry();
        Check.eq(4, registry.size(), "builtins registry holds the four first items");
        registry.register(BandageItem.DEFINITION);
        Check.eq(5, registry.size(), "bandage registers into the registry");
        Check.eq("field_bandage", registry.requireId(6).key(), "id 6 resolves to the bandage");
        Check.ok(throwsIAE(() -> registry.register(new ItemDefinition(
                        6, "other_item", "&fOther", "PAPER", null, null, false,
                        List.of(), ItemDefinition.Kind.ITEM, null, null, null, 0, "x:y"))),
                "a second claim on id 6 is rejected");
        Check.ok(throwsIAE(() -> registry.register(new ItemDefinition(
                        9, "field_bandage", "&fClone", "PAPER", null, null, false,
                        List.of(), ItemDefinition.Kind.ITEM, null, null, null, 0, "x:y"))),
                "a second claim on the bandage key is rejected");

        // THE 0.1.0 TRAP, pinned: ExcelItems validates its config
        // assignments at ITS enable, when the registry holds only
        // the builtins — so a "6: field_bandage" config line reads
        // as an unknown ID and would disable ExcelItems. This is
        // why the packet ships NO ExcelItems config line.
        ItemRegistry builtinsOnly = BuiltinItems.registry();
        List<String> problems = builtinsOnly.validateAssignments(Map.of(
                "1", "veil_potion",
                "2", "deathprotection_basic",
                "3", "deathpotion",
                "4", "revive_potion",
                "6", "field_bandage"));
        Check.eq(1, problems.size(), "a config line for id 6 produces exactly one problem");
        Check.ok(problems.get(0).contains("unknown id 6"),
                "the problem is the unknown-id trap: " + problems.get(0));
        Check.ok(builtinsOnly.validateAssignments(Map.of(
                "1", "veil_potion",
                "2", "deathprotection_basic",
                "3", "deathpotion",
                "4", "revive_potion")).isEmpty(),
                "the shipped builtins assignments validate clean");
    }
}
