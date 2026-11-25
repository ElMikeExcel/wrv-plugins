package excelemfixes;

import excelemfixes.arcane.ArcaneBoltModule;
import excelemfixes.enderman.EndermanResistanceModule;
import excelemfixes.dropguide.DropGuideModule;
import excelemfixes.foodheal.FoodHealModule;
import excelemfixes.jobsdummy.JobsDummyGateModule;
import excelemfixes.leash.LeashModule;
import excelemfixes.seal.SealRestoreModule;
import excelemfixes.questdest.QuestDestinationModule;
import excelemfixes.tracker.TrackerModule;
import excelemfixes.fieldmedic.FieldMedicModule;
import excelemfixes.meleepremium.MeleePremiumModule;
import excelemfixes.meleemitigation.MeleeMitigationModule;
import excelemfixes.skillnudge.SkillNudgeModule;
import excelemfixes.respawngrace.RespawnGraceModule;
import excelemfixes.mount.SummonMountModule;
import excelemfixes.mcmmo.McMMOXpConnectorModule;
import excelemfixes.questbeacon.QuestBeaconModule;

import java.util.List;

/**
 * THE module list. Adding a fix to ExcelEMFixes = write its
 * package, implement FixModule, and add one line here. The core
 * enables each listed module whose config toggle
 * (modules.<id>.enabled) is on; everything else about a module
 * lives inside its own package.
 */
public final class ModuleRegistry {

    private ModuleRegistry() {}

    public static List<FixModule> all() {
        return List.of(
                new QuestDestinationModule(),
                new ArcaneBoltModule(),
                new DropGuideModule(),
                new TrackerModule(),
                new FoodHealModule(),
                new LeashModule(),
                new SealRestoreModule(),
                new JobsDummyGateModule(),
                new EndermanResistanceModule(),
                new FieldMedicModule(),
                new MeleePremiumModule(),
                new MeleeMitigationModule(),
                new SkillNudgeModule(),
                new RespawnGraceModule(),
                new SummonMountModule(),
                new McMMOXpConnectorModule(),
                new QuestBeaconModule()
        );
    }
}
