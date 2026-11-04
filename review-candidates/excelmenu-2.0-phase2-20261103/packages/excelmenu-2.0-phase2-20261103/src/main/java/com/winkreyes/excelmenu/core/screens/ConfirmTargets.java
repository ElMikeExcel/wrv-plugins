// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.screens;

import java.util.List;

/**
 * The four teleport-confirmation targets (DESIGN-2.0 §2.4 forced
 * change 2): the doors that used to open the teleport_confirm_*
 * definition menus now route to the confirmation-gate service. Each
 * target carries BOTH old columns' presentation verbatim — the Java
 * definition's chest (title, icon, info name/lore, Yes/Cancel lore)
 * and the Bedrock modal's title/content (ExcelBedrockMenu's
 * teleportConfirmTitle/Content) — and the Yes action its columns ran:
 *
 *   spawn     the compass Spawn door. Java: [console] spawn (the
 *             EssentialsX spawn, per the definition's own 2026-10-24
 *             note). Bedrock: the village-fallback path (console
 *             spawn, then the configured village point if the player
 *             is still in the guild world). Union: the fallback path
 *             — it differs from the plain command only where that
 *             command would strand the player in the guild world.
 *   spawncmd  the catalogue /spawn row. Both columns: [player] spawn.
 *   guild     the Adventurers Guild doors. Both columns: [player] ag.
 *   home      Both columns: [player] home. (The Java definition
 *             existed; the Bedrock gate carried the same target; no
 *             registry entry dispatches here — the target completes
 *             the retired set.)
 */
public final class ConfirmTargets {

    public enum Kind { SPAWN_FALLBACK, PLAYER_COMMAND }

    public record Target(String definitionId,
                         String modalTitle, String modalContent,
                         String chestTitle, String icon, String infoName,
                         List<String> infoLore, String yesLore,
                         Kind kind, String command) {}

    public static final String YES_LABEL = "Yes, teleport";
    public static final String NO_LABEL = "Cancel";
    public static final String CANCEL_LORE = "&7Stay where I am";

    private static final Target SPAWN = new Target(
            "teleport_confirm_spawn",
            "Teleport to spawn?",
            "This takes you back to the village spawn right away.",
            "&6&lTeleport to Spawn?", "COMPASS", "&e&lSpawn",
            List.of("&7Teleport back to the village"),
            "&7Take me to spawn",
            Kind.SPAWN_FALLBACK, null);

    private static final Target SPAWNCMD = new Target(
            "teleport_confirm_spawncmd",
            "Teleport to spawn?",
            "This takes you back to the village spawn right away.",
            "&6&lTeleport to Spawn?", "LODESTONE", "&e&lSpawn",
            List.of("&7Teleport back to the village spawn"),
            "&7Take me to spawn",
            Kind.PLAYER_COMMAND, "spawn");

    private static final Target GUILD = new Target(
            "teleport_confirm_guild",
            "Teleport to the Adventurers Guild?",
            "This takes you to the Adventurers Guild right away.",
            "&6&lTeleport to the Guild?", "BELL", "&e&lAdventurers Guild",
            List.of("&7Teleport to the EliteMobs", "&7guild hall"),
            "&7Take me to the guild hall",
            Kind.PLAYER_COMMAND, "ag");

    private static final Target HOME = new Target(
            "teleport_confirm_home",
            "Teleport home?",
            "This takes you home right away.",
            "&6&lTeleport Home?", "RED_BED", "&e&lHome",
            List.of("&7Teleport to your home"),
            "&7Take me home",
            Kind.PLAYER_COMMAND, "home");

    private static final List<Target> ALL = List.of(SPAWN, SPAWNCMD, GUILD, HOME);

    /** The target a retired definition id routes to, or null. */
    public static Target byDefinitionId(String menuId) {
        if (menuId == null) return null;
        for (Target t : ALL) {
            if (t.definitionId().equals(menuId)) return t;
        }
        return null;
    }

    public static boolean isConfirmation(String menuId) {
        return byDefinitionId(menuId) != null;
    }

    public static List<Target> all() {
        return ALL;
    }

    private ConfirmTargets() {}
}
