import excelemfixes.meleemitigation.IndicatorText;
import excelemfixes.meleemitigation.MeleeWeaponClassifier;
import excelemfixes.meleemitigation.MitigationMath;
import excelemfixes.meleemitigation.OffHandClassifier;

import java.util.List;
import java.util.Set;

/**
 * Plain-JVM suite for the melee-mitigation module's pure
 * classes: MitigationMath (percent sanitize, totals, the
 * TrainingWheels-shaped scaling), MeleeWeaponClassifier
 * (the audit's melee families + the staff trap),
 * OffHandClassifier (the split bonus) and IndicatorText
 * (the on-item line + lore reconcile contract).
 */
public final class MeleeMitigationTest {

    public static void run() {
        System.out.println("MeleeMitigationTest:");

        // ---- math: shipped defaults are the D206 numbers
        Check.close(0.15, MitigationMath.totalPercent(0.15, 0.0, 0.90), "mit base alone = 15%");
        Check.close(0.17, MitigationMath.totalPercent(0.15, 0.02, 0.90), "mit sword+totem = 17%");
        Check.close(0.22, MitigationMath.totalPercent(0.15, 0.07, 0.90), "mit sword+shield = 22%");
        Check.close(0.90, MitigationMath.totalPercent(0.85, 0.20, 0.90), "mit total capped at cap");
        Check.close(85.0, MitigationMath.mitigatedDamage(100.0, 0.15), "mit 100 at 15% -> 85");
        Check.close(78.0, MitigationMath.mitigatedDamage(100.0, 0.22), "mit 100 at 22% -> 78");
        Check.close(100.0, MitigationMath.mitigatedDamage(100.0, 0.0), "mit 0% is a no-op");
        Check.close(0.0, MitigationMath.mitigatedDamage(0.0, 0.15), "mit zero damage untouched");
        Check.close(-5.0, MitigationMath.mitigatedDamage(-5.0, 0.15), "mit negative damage untouched");
        Check.close(0.15, MitigationMath.sanitizePercent(0.15, 0.15), "mit percent in range kept");
        Check.close(0.0, MitigationMath.sanitizePercent(0.0, 0.15), "mit percent 0 kept (deliberate off)");
        Check.close(0.15, MitigationMath.sanitizePercent(-0.1, 0.15), "mit negative percent -> fallback");
        Check.close(0.15, MitigationMath.sanitizePercent(0.99, 0.15), "mit >0.90 percent -> fallback");
        Check.close(0.15, MitigationMath.sanitizePercent(Double.NaN, 0.15), "mit NaN percent -> fallback");

        // ---- melee classifier: the audit families
        Check.ok(MeleeWeaponClassifier.isMeleeWeapon("DIAMOND_SWORD", false), "cls diamond sword is melee");
        Check.ok(MeleeWeaponClassifier.isMeleeWeapon("NETHERITE_AXE", false), "cls netherite axe is melee");
        Check.ok(MeleeWeaponClassifier.isMeleeWeapon("MACE", false), "cls mace is melee");
        Check.ok(MeleeWeaponClassifier.isMeleeWeapon("DIAMOND_SPEAR", false), "cls spear is melee");
        Check.ok(MeleeWeaponClassifier.isMeleeWeapon("DIAMOND_HOE", false), "cls hoe (EM scythe) is melee");
        Check.ok(MeleeWeaponClassifier.isMeleeWeapon("TRIDENT", false), "cls held trident is melee");
        Check.ok(!MeleeWeaponClassifier.isMeleeWeapon("BOW", false), "cls bow is not melee");
        Check.ok(!MeleeWeaponClassifier.isMeleeWeapon("CROSSBOW", false), "cls crossbow is not melee");
        Check.ok(!MeleeWeaponClassifier.isMeleeWeapon("BLAZE_ROD", false), "cls blaze rod (wand) is not melee");
        Check.ok(!MeleeWeaponClassifier.isMeleeWeapon("STICK", false), "cls stick is not melee");
        Check.ok(!MeleeWeaponClassifier.isMeleeWeapon("AIR", false), "cls empty hand is not melee");
        Check.ok(!MeleeWeaponClassifier.isMeleeWeapon(null, false), "cls null is not melee");
        Check.ok(!MeleeWeaponClassifier.isMeleeWeapon("DIAMOND_PICKAXE", false), "cls pickaxe is not melee");
        // THE STAFF TRAP: WOODEN_SPEAR material + EM magic identity = staff, never melee
        Check.ok(!MeleeWeaponClassifier.isMeleeWeapon("WOODEN_SPEAR", true), "cls staff (wooden spear, magic) is NOT melee");
        Check.ok(!MeleeWeaponClassifier.isMeleeWeapon("DIAMOND_SWORD", true), "cls any magic-flagged item is NOT melee");
        // config exclusion wins; config addition works (data-driven)
        Check.ok(!MeleeWeaponClassifier.isMeleeWeapon("TRIDENT", false,
                MeleeWeaponClassifier.DEFAULT_MELEE_MATERIALS,
                MeleeWeaponClassifier.DEFAULT_MELEE_SUFFIXES, Set.of("TRIDENT")),
                "cls config-excluded trident is not melee");
        Check.ok(MeleeWeaponClassifier.isMeleeWeapon("COPPER_SHORTSWORD", false,
                Set.of("MACE"), Set.of("_SHORTSWORD"), Set.of()),
                "cls config-added suffix counts as melee");

        // ---- off-hand classifier: the split bonus
        Check.ok(OffHandClassifier.kindOf("TOTEM_OF_UNDYING",
                OffHandClassifier.DEFAULT_TOTEM_MATERIALS,
                OffHandClassifier.DEFAULT_SHIELD_LIKE_MATERIALS) == OffHandClassifier.Kind.TOTEM,
                "off totem kind");
        Check.ok(OffHandClassifier.kindOf("SHIELD",
                OffHandClassifier.DEFAULT_TOTEM_MATERIALS,
                OffHandClassifier.DEFAULT_SHIELD_LIKE_MATERIALS) == OffHandClassifier.Kind.SHIELD_LIKE,
                "off shield kind");
        Check.ok(OffHandClassifier.kindOf("TORCH",
                OffHandClassifier.DEFAULT_TOTEM_MATERIALS,
                OffHandClassifier.DEFAULT_SHIELD_LIKE_MATERIALS) == OffHandClassifier.Kind.NONE,
                "off torch kind none");
        Check.ok(OffHandClassifier.kindOf(null,
                OffHandClassifier.DEFAULT_TOTEM_MATERIALS,
                OffHandClassifier.DEFAULT_SHIELD_LIKE_MATERIALS) == OffHandClassifier.Kind.NONE,
                "off null kind none");
        Check.close(0.02, OffHandClassifier.bonusFor(OffHandClassifier.Kind.TOTEM, 0.02, 0.07), "off totem pays 2%");
        Check.close(0.07, OffHandClassifier.bonusFor(OffHandClassifier.Kind.SHIELD_LIKE, 0.02, 0.07), "off shield pays 7%");
        Check.close(0.0, OffHandClassifier.bonusFor(OffHandClassifier.Kind.NONE, 0.02, 0.07), "off none pays 0");

        // ---- indicator text: its own percent, nothing when inactive
        Check.ok("7% mitigation activated".equals(
                IndicatorText.lineFor(0.07).replace(IndicatorText.LINE_COLOR, "")),
                "ind shield line reads 7% mitigation activated");
        Check.ok("2% mitigation activated".equals(
                IndicatorText.lineFor(0.02).replace(IndicatorText.LINE_COLOR, "")),
                "ind totem line reads 2% mitigation activated");
        Check.ok("2.5".equals(IndicatorText.formatPercent(0.025)), "ind percent 2.5 formats clean");
        List<String> lore = List.of("§7An old shield.", "§8From Primis.");
        List<String> added = IndicatorText.reconcile(lore, IndicatorText.lineFor(0.07));
        Check.ok(added != null && added.size() == 3
                && added.get(0).equals(lore.get(0)) && added.get(1).equals(lore.get(1)),
                "ind reconcile appends, original lore untouched");
        Check.ok(IndicatorText.reconcile(added, IndicatorText.lineFor(0.07)) == null,
                "ind reconcile no-change returns null");
        List<String> removed = IndicatorText.reconcile(added, null);
        Check.ok(removed != null && removed.equals(lore),
                "ind reconcile removal restores original lore exactly");
        List<String> swapped = IndicatorText.reconcile(added, IndicatorText.lineFor(0.02));
        Check.ok(swapped != null && swapped.size() == 3
                && swapped.get(2).contains("2% mitigation activated"),
                "ind reconcile swaps percent when item kind changes");
        Check.ok(IndicatorText.reconcile(lore, null) == null,
                "ind reconcile inactive + no line = no change");
    }
}
