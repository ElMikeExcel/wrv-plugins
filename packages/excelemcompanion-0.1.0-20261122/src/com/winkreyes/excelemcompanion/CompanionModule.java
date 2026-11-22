package com.winkreyes.excelemcompanion;

/**
 * A companion module. ExcelEMCompanion is an open module framework
 * (Mike's direction, Nov 21, 2026: "We'll probably keep adding
 * things to this plugin so make sure that's open"): a module is a
 * self-contained package that implements this interface and is added
 * to the one-line list in {@link ModuleRegistry}. Nothing in the
 * core or in other modules changes when a module is added.
 *
 * Contract:
 * - {@link #id()} is the module's stable id; its config toggle is
 *   {@code modules.<id>.enabled} in config.yml.
 * - A module registers its commands and its menu-action handler
 *   only inside {@link #onEnable(ModuleContext)}, and unregisters
 *   them in {@link #onDisable()} — disabling a module in config
 *   removes its commands and menus completely and leaves every
 *   other module untouched.
 * - Menus are built as shared Views and opened through the
 *   ModuleContext's MenuRenderer; modules never render inventories
 *   or forms themselves.
 */
public interface CompanionModule {

    /** Stable module id, e.g. "drop-guide". */
    String id();

    /** Short human name for logs, e.g. "Drop Guide". */
    String displayName();

    /** Called once when the module is enabled (server start or a
     *  /companion reload that turns it on). */
    void onEnable(ModuleContext ctx);

    /** Called when the module is disabled. Must undo everything
     *  onEnable registered. */
    void onDisable();

    /** Called on /companion reload while the module stays enabled;
     *  reload module data (indexes, catalogs) here. */
    default void reload() {}
}
