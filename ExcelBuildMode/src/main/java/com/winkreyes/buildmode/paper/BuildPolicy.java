package com.winkreyes.buildmode.paper;

import java.util.*;
import org.bukkit.Material;

/**
 * Fixed policy lists. Through 1.2 the WorldEdit list below WAS the session grant.
 * From 1.3 (Mike's direction, Oct 20, 2026) the session grant is the full
 * WorldEdit ability set as the single worldedit.* node — held only while Build
 * Mode is active, attached on entry and on relog-restore, removed on every
 * exit — and this list is the VERIFICATION PROBE set: representative nodes
 * (selection, region, clipboard, history, brushes, brush options) sampled to
 * prove the grant is effective or fully gone. No other-player mode command,
 * scripting, reload or bypass grant appears anywhere in this policy.
 */
public final class BuildPolicy {
    private BuildPolicy() { }
    /** The single session grant node for the full WorldEdit ability set (1.3). */
    public static final String WORLD_EDIT_GRANT = "worldedit.*";
    /**
     * Reviewed builder node (October 1.3.3 "reviewed" build, live Oct 25,
     * 2026; source recovered Nov 10, 2026 from the preserved live jar —
     * see REVIEWED-DELTA-RECOVERY.md in the 1.3.4 build folder). A holder
     * has been administrator-reviewed to keep standing WorldEdit
     * permissions outside Build Mode: the outside-WorldEdit entry refusal
     * and the exit residual-permission probe are skipped for them. The
     * node is ours, NOT a WorldEdit node, so holding it permanently trips
     * neither probe itself. plugin.yml default false.
     */
    public static final String REVIEWED_PERMISSION = "excelbuildmode.reviewed";
    /**
     * Entry decision (recovered): entry is refused for outside WorldEdit
     * only when temporary WorldEdit is enabled, the builder is NOT a
     * reviewed holder, and the builder already holds a WorldEdit probe
     * node outside Build Mode.
     */
    public static boolean refusesOutsideWorldEdit(boolean worldEdit, boolean reviewed, boolean hasOutsideWorldEdit) {
        return worldEdit && !reviewed && hasOutsideWorldEdit;
    }
    /**
     * Exit-probe decision (recovered): after the session attachment is
     * removed, residual WorldEdit probe nodes are treated as a failure
     * only when temporary WorldEdit is enabled and the builder is NOT a
     * reviewed holder (a reviewed builder's standing WorldEdit is
     * expected to remain).
     */
    public static boolean probesResidualWorldEdit(boolean worldEdit, boolean reviewed) {
        return worldEdit && !reviewed;
    }
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
     * Session permissions (ExcelBuildMode 1.1; WorldEdit shape changed in 1.3).
     * jobs.use is denied for the session so Jobs pays nothing for new ordinary
     * actions while building. When temporary WorldEdit is enabled, the full
     * WorldEdit ability set is granted (Mike, Oct 20, 2026: full set incl.
     * brushes, controlled by Build Mode — never an always-on grant) as the
     * worldedit.* wildcard PLUS every probe node granted explicitly: Bukkit
     * wildcard expansion only covers permission children REGISTERED with the
     * server, and WorldEdit 7.4.5 registers none in its plugin.yml — the 1.3
     * runtime probe caught worldedit.brush.smooth resolving false under a
     * wildcard-only grant even though 1.2's explicit grant covered it. The
     * explicit list guarantees the known toolset; the wildcard covers the
     * registered long tail. With worldEdit=false this is also the
     * landing/fallback shape: Jobs denied, no WorldEdit. Nothing else is
     * ever attached.
     */
    public static Map<String, Boolean> sessionPermissions(boolean worldEdit) {
        Map<String, Boolean> nodes = new LinkedHashMap<>();
        nodes.put("jobs.use", false);
        if (worldEdit) {
            nodes.put(WORLD_EDIT_GRANT, true);
            for (String node : WORLD_EDIT_PERMISSIONS) nodes.put(node, true);
        }
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
    /**
     * 1.3.1 hotfix: the schematic family is gated by SUBCOMMAND, not added to
     * EDIT_COMMANDS. The village paste workflow runs //schem load + //schem
     * save from inside Build Mode; 1.3's root-only allowlist missed the
     * family entirely, so the guard cancelled Marvin's live
     * "//schem load medieval-village-130red" with "Switch to Play Normally"
     * (confirmed Oct 20, 2026). Session-needs subcommands (load/save) plus
     * read-only helpers (list/formats) are allowed with session WorldEdit;
     * delete/rename (file-destructive, never needed in session) and any
     * other or missing subcommand stay denied and fall through to the same
     * guard message.
     */
    private static final Set<String> SCHEMATIC_SUBCOMMANDS = Set.of("load", "save", "list", "formats");
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
        // 1.3.1: schematic family, subcommand-gated (see SCHEMATIC_SUBCOMMANDS).
        // The bare name covers every typed form after the normalization above:
        // //schem, //schematic, /schem, /worldedit:schem, /worldedit:/schem.
        String bare = root.replaceFirst("^/+", "");
        if (bare.equals("schem") || bare.equals("schematic")) {
            String[] parts = text.trim().split("\\s+");
            String sub = parts.length > 1 ? parts[1].toLowerCase(Locale.ROOT) : "";
            return SCHEMATIC_SUBCOMMANDS.contains(sub);
        }
        return EDIT_COMMANDS.contains(root);
    }
}
