package com.winkreyes.excelemcompanion.shared;

import java.util.List;

/**
 * The shared menu view model. Every module builds its screens as
 * these views; the single MenuRenderer turns them into a Java chest
 * GUI or a Bedrock Cumulus form with identical content, so the two
 * platforms can never drift apart and a new module needs no renderer
 * code of its own.
 *
 * Navigation is stack-based: a button's action is routed to the
 * module that owns its prefix (the part before the first ':'), and
 * the renderer keeps each player's view history so the core actions
 * below work everywhere without module code:
 *   core:back   — return to the previous view
 *   core:close  — close the menu
 *   core:noop   — display only
 * Standing law: every menu and submenu carries a Back button (nav
 * buttons render in the Java GUI's bottom row and at the end of the
 * Bedrock form); root menus carry Close instead.
 */
public final class Views {

    private Views() {}

    /**
     * One menu entry. {@code detail} is the optional text screen the
     * entry opens when its action is "core:detail" — used where the
     * Java hover lore is not available (Bedrock) or where a longer
     * explanation belongs behind one tap (the expertise legend).
     * {@code nav} marks chrome buttons (Back / pages / Close) that
     * render in the GUI's bottom row instead of the content grid.
     */
    public record Button(String action, String name, String icon,
                         List<String> lore, Detail detail, boolean nav) {
        public Button {
            lore = lore == null ? List.of() : List.copyOf(lore);
        }

        public Button(String action, String name, String icon,
                      List<String> lore) {
            this(action, name, icon, lore, null, false);
        }

        public static Button nav(String action, String name, String icon) {
            return new Button(action, name, icon, List.of(), null, true);
        }

        public static Button content(String action, String name, String icon,
                                     List<String> lore) {
            return new Button(action, name, icon, lore, null, false);
        }
    }

    /**
     * One menu screen: title plus content buttons, then nav buttons.
     * {@code intro} is the short body text a Bedrock form shows above
     * its buttons (the Java GUI has no body area; it stays empty
     * there and the same text rides in button lore where needed).
     */
    public record View(String title, List<Button> buttons, String intro) {
        public View {
            buttons = List.copyOf(buttons);
            intro = intro == null ? "" : intro;
        }

        public View(String title, List<Button> buttons) {
            this(title, buttons, "");
        }
    }

    /** A text screen: title plus body lines; Back is renderer chrome. */
    public record Detail(String title, String icon, List<String> lines) {
        public Detail {
            lines = List.copyOf(lines);
            icon = icon == null || icon.isBlank() ? "PAPER" : icon;
        }

        public Detail(String title, List<String> lines) {
            this(title, "PAPER", lines);
        }
    }
}
