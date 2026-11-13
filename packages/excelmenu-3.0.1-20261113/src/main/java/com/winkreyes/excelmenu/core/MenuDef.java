package com.winkreyes.excelmenu.core;

import java.util.List;
import java.util.Map;

/**
 * The ExcelMenu menu definition model (format 1). One definition is
 * the single source for BOTH renderers: the Java chest GUI and the
 * Bedrock form. Definitions are data, loaded from
 * plugins/ExcelMenu/menus/<id>.yml; see FORMAT.md for the schema.
 *
 * ExcelMenu exists because DeluxeMenus silently skipped two menu
 * files that were never registered, and a store button died without
 * a word. Every class in this core package is built so that class of
 * failure is a loud load error instead: unknown keys, unknown
 * requirement types, unknown click actions, and navigation targets
 * that do not exist are all rejected before a menu can open.
 */
public final class MenuDef {

    /** One requirement: a typed check with free-form options and an
     *  optional deny action list (open requirements only). */
    public record Requirement(String id, String type,
                              Map<String, Object> options,
                              List<String> deny) {}

    /** Bedrock rendering hints. mode "modal": the item opens a
     *  detail form whose buttons run the left / right click
     *  actions; labels describe those actions to a player who has
     *  no left/right click. fourLabel (0.1.2, nullable) names a
     *  third action button, rendered between the right button
     *  and Back, which runs the item's shift-left click actions —
     *  the Bedrock mirror of the Java shift-left click. The
     *  validator pairs it with the shift-left lines: one without
     *  the other is a load error (see BedrockDetail for the
     *  button/index mapping both render paths share). */
    public record BedrockDef(String mode, String leftLabel, String rightLabel,
                             String fourLabel) {}

    public record ItemDef(String id, String material, int slot, int amount,
                          Integer data, Integer priority, boolean update,
                          String name, List<String> lore,
                          List<Requirement> viewRequirements,
                          Map<String, List<String>> clicks,
                          BedrockDef bedrock) {}

    public final String id;
    public final String title;
    public final int size;
    /** chest | dispenser (lower-case; see MenuValidator for the set). */
    public final String type;
    /** How a Bedrock (Geyser/Floodgate) viewer is rendered this
     *  menu: "form" (the default — a Cumulus SimpleForm) or "chest"
     *  (0.1.3 — skip the form and serve the chest GUI, which Geyser
     *  translates with real item icons; for shelves whose whole
     *  point is seeing the items, e.g. cosmetic blocks). Java
     *  viewers always get the chest GUI. Lower-case; see
     *  MenuValidator for the set. */
    public final String bedrockRender;
    /** Optional command that opens this menu (e.g. "shop"). */
    public final String command;
    /** Seconds between placeholder refreshes of update:true items; 0 = off. */
    public final int updateInterval;
    public final List<Requirement> openRequirements;
    public final List<String> openDeny;
    public final List<ItemDef> items;

    public MenuDef(String id, String title, int size, String type, String bedrockRender,
                   String command,
                   int updateInterval, List<Requirement> openRequirements,
                   List<String> openDeny, List<ItemDef> items) {
        this.id = id;
        this.title = title;
        this.size = size;
        this.type = type;
        this.bedrockRender = bedrockRender;
        this.command = command;
        this.updateInterval = updateInterval;
        this.openRequirements = openRequirements;
        this.openDeny = openDeny;
        this.items = items;
    }

    /** True when Bedrock viewers get the chest GUI for this menu
     *  instead of a form (see {@link #bedrockRender}). */
    public boolean bedrockChest() {
        return "chest".equals(bedrockRender);
    }

    public ItemDef item(String itemId) {
        for (ItemDef it : items) {
            if (it.id().equals(itemId)) return it;
        }
        return null;
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
