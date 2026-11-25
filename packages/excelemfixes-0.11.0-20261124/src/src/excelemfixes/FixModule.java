package excelemfixes;

/**
 * One EliteMobs workaround module. ExcelEMFixes is the standing
 * home for community fixes to EliteMobs defects (
 * Nov 22, 2026): when we find an EM bug we cannot repair inside
 * EM's own code, the workaround lands here as a module, and the
 * modules accumulate as we go.
 *
 * The framework is open: a module is a
 * self-contained package that implements this interface and is
 * added to the one-line list in {@link ModuleRegistry}. Nothing in
 * the core or in other modules changes when a module is added.
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
public interface FixModule {

    /** Stable module id, e.g. "quest-destinations". */
    String id();

    /** Short human name for logs, e.g. "Quest Destinations". */
    String displayName();

    /** Called once when the module is enabled (server start or an
     *  /emfixes reload that turns it on). */
    void onEnable(ModuleContext ctx);

    /** Called when the module is disabled. Must undo everything
     *  onEnable registered. */
    void onDisable();

    /** Called on /emfixes reload while the module stays enabled;
     *  reload module data here. */
    default void reload() {}
}
