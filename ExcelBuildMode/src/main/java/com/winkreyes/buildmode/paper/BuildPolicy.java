package com.winkreyes.buildmode.paper;

import java.util.*;
import org.bukkit.Material;

/** Deliberately fixed list. No wildcard, other-player mode command, scripting, reload or bypass grant. */
public final class BuildPolicy {
    private BuildPolicy() { }
    public static final Set<String> WORLD_EDIT_PERMISSIONS = Set.of(
        "worldedit.wand", "worldedit.selection.pos", "worldedit.selection.hpos",
        "worldedit.selection.expand", "worldedit.selection.contract", "worldedit.selection.shift",
        "worldedit.region.set", "worldedit.region.replace", "worldedit.region.walls", "worldedit.region.faces",
        "worldedit.clipboard.copy", "worldedit.clipboard.paste", "worldedit.clipboard.rotate", "worldedit.clipboard.flip",
        "worldedit.history.undo.self", "worldedit.history.redo.self",
        // Brushes (added in 1.2, Mike's decision Oct 14, 2026 after Marvin's acceptance run):
        // every construction brush family plus the brush option commands. Deliberately NOT
        // included: worldedit.brush.butcher, which kills entities in a radius; that is combat,
        // not construction, and fights the pilot's no-combat design.
        "worldedit.brush.apply", "worldedit.brush.biome", "worldedit.brush.clipboard",
        "worldedit.brush.cylinder", "worldedit.brush.deform", "worldedit.brush.ex",
        "worldedit.brush.feature", "worldedit.brush.forest", "worldedit.brush.gravity",
        "worldedit.brush.heightmap", "worldedit.brush.item", "worldedit.brush.lower",
        "worldedit.brush.morph", "worldedit.brush.paint", "worldedit.brush.raise",
        "worldedit.brush.set", "worldedit.brush.smooth", "worldedit.brush.snow",
        "worldedit.brush.snowsmooth", "worldedit.brush.sphere", "worldedit.brush.splatter",
        "worldedit.brush.options.material", "worldedit.brush.options.range",
        "worldedit.brush.options.size", "worldedit.brush.options.mask");
    /** Blocks whose right-click use is a harvesting/collection or cooking start, never construction. */
    public static final Set<Material> HARVEST_BLOCKS = Set.of(
        Material.COMPOSTER, Material.SWEET_BERRY_BUSH, Material.BEEHIVE, Material.BEE_NEST,
        Material.CAMPFIRE, Material.SOUL_CAMPFIRE);
    /**
     * Pilot session permissions (ExcelBuildMode 1.1). jobs.use is denied for the session so Jobs
     * pays nothing for new ordinary actions while building; the fixed WorldEdit set is granted
     * only when temporary WorldEdit is enabled. Nothing else is ever attached.
     */
    public static Map<String, Boolean> sessionPermissions(boolean worldEdit) {
        Map<String, Boolean> nodes = new LinkedHashMap<>();
        nodes.put("jobs.use", false);
        if (worldEdit) for (String node : WORLD_EDIT_PERMISSIONS) nodes.put(node, true);
        return Collections.unmodifiableMap(nodes);
    }
    /**
     * True when a right-click block interaction is harvesting, collection or interactive
     * stripping rather than construction: a harvest block with anything in hand, or an axe
     * used on a strippable log, stem, wood or bamboo block.
     */
    public static boolean blocksHarvestInteraction(Material block, Material heldItem) {
        if (block == null) return false;
        if (HARVEST_BLOCKS.contains(block)) return true;
        boolean axe = heldItem != null && heldItem.name().endsWith("_AXE");
        return axe && isStrippable(block);
    }
    /** Axe-strippable blocks, matched by material name so the check needs no live tag registry. */
    private static boolean isStrippable(Material block) {
        String name = block.name();
        return name.endsWith("_LOG") || name.endsWith("_STEM") || name.endsWith("_WOOD")
            || name.endsWith("_HYPHAE") || block == Material.BAMBOO_BLOCK;
    }
    private static final Set<String> EDIT_COMMANDS = Set.of(
        "/wand", "/pos1", "/pos2", "/hpos1", "/hpos2", "/expand", "/contract", "/shift",
        "/set", "/replace", "/walls", "/faces", "/copy", "/paste", "/rotate", "/flip", "undo", "redo", "/undo", "/redo");
    private static final Set<String> CHAT_HELP = Set.of("msg", "tell", "w", "m", "message", "r", "reply", "help", "?", "list");
    public static boolean allowsCommand(String text, boolean worldEdit) { return allowsCommand(text, worldEdit, true); }
    public static boolean allowsCommand(String text, boolean worldEdit, boolean chatHelp) {
        if (!text.startsWith("/")) return false;
        String root = text.substring(1).split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        if (root.equals("buildmode") || root.equals("excelbuildmode:buildmode")) return true;
        String ordinary = root;
        for (String namespace : List.of("essentials:", "minecraft:", "bukkit:"))
            if (ordinary.startsWith(namespace)) { ordinary = ordinary.substring(namespace.length()); break; }
        if (chatHelp && CHAT_HELP.contains(ordinary)) return true;
        if (!worldEdit) return false;
        if (root.startsWith("worldedit:")) root = root.substring("worldedit:".length());
        return EDIT_COMMANDS.contains(root);
    }
}
