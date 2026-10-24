package com.winkreyes.excelmenu.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Semantic validation of a whole menu set. The loader guarantees
 * shape; the validator guarantees MEANING: real materials, in-range
 * slots, no ambiguous slot collisions, known requirement types with
 * their required options, parseable click actions, unique open
 * commands, and — the check whose absence took the cosmetics store
 * down under DeluxeMenus — every [openguimenu] target must be a
 * menu that actually loaded. Any failure is a load error that
 * names the menu, the item, and the problem.
 */
public final class MenuValidator {

    public static final Set<String> TYPES = Set.of("chest", "dispenser");
    /** Legal bedrock-render values (0.1.3): "form" = Cumulus form
     *  (the default), "chest" = serve Bedrock the chest GUI. */
    public static final Set<String> BEDROCK_RENDERERS = Set.of("form", "chest");
    public static final Set<String> CLICK_KINDS =
            Set.of("left", "right", "shift-left", "shift-right", "middle", "any");
    /** Commands ExcelMenu itself owns; a menu may not claim them. */
    public static final Set<String> RESERVED_COMMANDS = Set.of("excelmenu", "dm");

    private MenuValidator() {}

    public static List<String> validate(List<MenuDef> defs, Set<String> validMaterials) {
        List<String> errors = new ArrayList<>();
        Map<String, MenuDef> byId = new HashMap<>();
        for (MenuDef d : defs) {
            if (byId.put(d.id, d) != null) {
                errors.add("duplicate menu id '" + d.id + "'");
            }
        }
        Map<String, String> commandOwners = new HashMap<>();
        for (MenuDef d : defs) {
            validateMenu(d, validMaterials, errors);
            if (d.command != null) {
                if (RESERVED_COMMANDS.contains(d.command)) {
                    errors.add(d.id + ": command '" + d.command + "' is reserved by ExcelMenu");
                }
                String prev = commandOwners.put(d.command, d.id);
                if (prev != null) {
                    errors.add(d.id + ": command '" + d.command
                            + "' is already claimed by menu '" + prev + "'");
                }
            }
            // Navigation targets across the whole set.
            for (MenuDef.ItemDef it : d.items) {
                for (List<String> lines : it.clicks().values()) {
                    checkActions(d, it, lines, byId.keySet(), errors);
                }
            }
            for (String line : d.openDeny) {
                checkAction(d, null, line, byId.keySet(), errors);
            }
        }
        return errors;
    }

    private static void validateMenu(MenuDef d, Set<String> validMaterials,
                                     List<String> errors) {
        String w = d.id;
        if (d.title == null || d.title.isBlank()) {
            errors.add(w + ": title is missing or blank");
        }
        if (!TYPES.contains(d.type)) {
            errors.add(w + ": type '" + d.type + "' is not supported (chest, dispenser)");
        }
        if (!BEDROCK_RENDERERS.contains(d.bedrockRender)) {
            errors.add(w + ": bedrock-render '" + d.bedrockRender
                    + "' is not supported (form, chest)");
        }
        if ("chest".equals(d.type) && (d.size < 9 || d.size > 54 || d.size % 9 != 0)) {
            errors.add(w + ": chest size " + d.size + " is not a multiple of 9 in 9..54");
        }
        if ("dispenser".equals(d.type) && d.size != 9) {
            errors.add(w + ": dispenser size must be 9, not " + d.size);
        }
        checkRequirements(w + " open-requirement", d.openRequirements, errors);

        Map<Integer, Set<Integer>> slotPriorities = new HashMap<>();
        Map<Integer, Integer> slotBare = new HashMap<>();
        boolean anyUpdate = false;
        for (MenuDef.ItemDef it : d.items) {
            String iw = w + ":" + it.id();
            if (!validMaterials.contains(it.material())) {
                errors.add(iw + ": unknown material '" + it.material() + "'");
            }
            if (it.slot() < 0 || it.slot() >= d.size) {
                errors.add(iw + ": slot " + it.slot() + " is outside menu size " + d.size);
            }
            if (it.amount() < 1) {
                errors.add(iw + ": amount " + it.amount() + " is below 1");
            }
            if (it.update()) anyUpdate = true;
            if (it.priority() == null) {
                if (slotBare.put(it.slot(), 1) != null || slotPriorities.containsKey(it.slot())) {
                    errors.add(iw + ": slot " + it.slot()
                            + " is shared with another item but this item has no priority");
                }
            } else {
                if (slotBare.containsKey(it.slot())) {
                    errors.add(iw + ": slot " + it.slot()
                            + " is shared with an item that has no priority");
                }
                if (!slotPriorities.computeIfAbsent(it.slot(), k -> new HashSet<>())
                        .add(it.priority())) {
                    errors.add(iw + ": slot " + it.slot() + " priority " + it.priority()
                            + " is already used by another item in that slot");
                }
            }
            for (String kind : it.clicks().keySet()) {
                if (!CLICK_KINDS.contains(kind)) {
                    errors.add(iw + ": unknown click kind '" + kind + "'");
                }
            }
            checkRequirements(iw + " view-requirement", it.viewRequirements(), errors);
            if (it.bedrock() != null) {
                if (!"modal".equals(it.bedrock().mode())) {
                    errors.add(iw + ": bedrock mode '" + it.bedrock().mode() + "' is not 'modal'");
                } else if (blank(it.bedrock().leftLabel()) || blank(it.bedrock().rightLabel())) {
                    errors.add(iw + ": bedrock modal needs both left-label and right-label");
                } else {
                    // The Bedrock four button mirrors the Java
                    // shift-left click: label and lines are a pair.
                    // Lines without a label would leave Bedrock
                    // players no way to reach the action (a parity
                    // gap); a label without lines would render a
                    // button that dispatches nothing (a dead
                    // button). Both are load errors, never silent.
                    List<String> shiftLeft = it.clicks().get("shift-left");
                    boolean hasShiftLeft = shiftLeft != null && !shiftLeft.isEmpty();
                    boolean hasFour = !blank(it.bedrock().fourLabel());
                    if (hasShiftLeft && !hasFour) {
                        errors.add(iw + ": bedrock modal mirrors shift-left clicks"
                                + " but sets no four-label — Bedrock players would"
                                + " have no 4th button for them");
                    } else if (hasFour && !hasShiftLeft) {
                        errors.add(iw + ": bedrock four-label '" + it.bedrock().fourLabel()
                                + "' has no shift-left click lines to dispatch");
                    }
                }
            }
        }
        if (anyUpdate && d.updateInterval <= 0) {
            errors.add(w + ": items use update: true but the menu sets no update-interval");
        }
    }

    private static void checkRequirements(String where,
                                          List<MenuDef.Requirement> reqs,
                                          List<String> errors) {
        for (MenuDef.Requirement r : reqs) {
            if (!RequirementEngine.TYPES.contains(r.type())) {
                errors.add(where + ": unknown requirement type '" + r.type()
                        + "' (id '" + r.id() + "')");
                continue;
            }
            switch (r.type()) {
                case "has-permission" -> need(where, r, "permission", errors);
                case "has-money" -> need(where, r, "amount", errors);
                case "has-item" -> need(where, r, "material", errors);
                case "regex-matches" -> { need(where, r, "input", errors); need(where, r, "regex", errors); }
                default -> { need(where, r, "input", errors); need(where, r, "output", errors); }
            }
            for (String line : r.deny()) {
                try {
                    ActionParser.parse(line);
                } catch (ActionParser.ActionParseException e) {
                    errors.add(where + ": deny line invalid — " + e.getMessage());
                }
            }
        }
    }

    private static void need(String where, MenuDef.Requirement r, String key,
                             List<String> errors) {
        if (r.options().get(key) == null) {
            errors.add(where + ": requirement '" + r.id() + "' (" + r.type()
                    + ") is missing option '" + key + "'");
        }
    }

    private static void checkActions(MenuDef d, MenuDef.ItemDef it, List<String> lines,
                                     Set<String> menuIds, List<String> errors) {
        for (String line : lines) checkAction(d, it, line, menuIds, errors);
    }

    private static void checkAction(MenuDef d, MenuDef.ItemDef it, String line,
                                    Set<String> menuIds, List<String> errors) {
        String where = it == null ? d.id : d.id + ":" + it.id();
        try {
            ActionParser.Action a = ActionParser.parse(line);
            if (a.kind() == ActionParser.Kind.OPEN_MENU && !menuIds.contains(a.body())) {
                errors.add(where + ": [openguimenu] target '" + a.body()
                        + "' is not a loaded menu — the click would die silently");
            }
        } catch (ActionParser.ActionParseException e) {
            errors.add(where + ": " + e.getMessage());
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
