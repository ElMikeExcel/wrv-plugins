package com.winkreyes.excelemcompanion;

import com.winkreyes.excelemcompanion.dropguide.DropGuideModule;
import com.winkreyes.excelemcompanion.tracker.TrackerModule;

import java.util.List;

/**
 * THE module list. Adding a module to ExcelEMCompanion = write its
 * package, implement CompanionModule, and add one line here. The
 * core enables each listed module whose config toggle
 * (modules.<id>.enabled) is on; everything else about a module
 * lives inside its own package.
 */
public final class ModuleRegistry {

    private ModuleRegistry() {}

    public static List<CompanionModule> all() {
        return List.of(
                new DropGuideModule(),
                new TrackerModule()
        );
    }
}
