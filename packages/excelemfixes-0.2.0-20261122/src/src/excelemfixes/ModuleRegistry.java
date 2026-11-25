package excelemfixes;

import excelemfixes.arcane.ArcaneBoltModule;
import excelemfixes.dropguide.DropGuideModule;
import excelemfixes.foodheal.FoodHealModule;
import excelemfixes.jobsdummy.JobsDummyGateModule;
import excelemfixes.leash.LeashModule;
import excelemfixes.seal.SealRestoreModule;
import excelemfixes.questdest.QuestDestinationModule;
import excelemfixes.tracker.TrackerModule;

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
                new JobsDummyGateModule()
        );
    }
}
