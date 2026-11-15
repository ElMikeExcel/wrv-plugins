package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.gui.Views;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 0.3.4 (BB-02) Back-navigation contract. The Bukkit GUIs cannot be
 * instantiated off-server, so — like the box view's display-shape gate
 * in SudokuBoxViewTest — this suite pins the contract where it lives:
 * the renderers' slot->action registrations in Views.java and the
 * dispatch branches in GuiListener.java, read from the build tree
 * (-Dbuild=<path>). Every fixed screen from the audit's BB-02 rows is
 * asserted: the control exists, at the decided slot, with the decided
 * action; the handler saves the in-progress round and lands on the
 * named parent; and no Back path discards or banks a round.
 */
public final class BackNavTest {

    private static int count(String haystack, String needle) {
        int n = 0, i = 0;
        while ((i = haystack.indexOf(needle, i)) >= 0) { n++; i += needle.length(); }
        return n;
    }

    /** The switch-branch body for a case label: from the label to the next case. */
    private static String branch(String src, String label) {
        int start = src.indexOf(label);
        if (start < 0) return "";
        int next = src.indexOf("case \"", start + label.length());
        return next < 0 ? src.substring(start) : src.substring(start, next);
    }

    public static void main(String[] args) throws Exception {
        Check c = new Check("BackNavTest");
        String build = System.getProperty("build");
        c.that(build != null && !build.isBlank(), "-Dbuild=<build tree> is set");
        if (build == null || build.isBlank()) { c.finish(); return; }
        Path root = Path.of(build, "src", "com", "winkreyes", "excelbraingames", "gui");
        String views = Files.readString(root.resolve("Views.java"));
        String listener = Files.readString(root.resolve("GuiListener.java"));

        // ---- shared board slots (the house control row is unchanged)
        c.eq(51, Views.SLOT_WORDS, "SLOT_WORDS stays 51");
        c.eq(52, Views.SLOT_NEW, "SLOT_NEW stays 52");
        c.eq(53, Views.SLOT_CLOSE, "SLOT_CLOSE (the Back slot on boards) stays 53");

        // ---- FIX-2: game picker -> Server Menu (Java + Bedrock share this GUI)
        c.eq(1, count(views, "\"back:servermenu\""),
                "picker registers exactly one back:servermenu action");
        c.that(views.contains("h.actions.put(26, \"back:servermenu\")"),
                "picker Back sits at slot 26");
        c.that(views.contains("Back to Server Menu"),
                "picker Back is labelled Back to Server Menu");
        String backBranch = branch(listener, "case \"back\"");
        c.that(backBranch.contains("performCommand(\"menu\")"),
                "servermenu branch runs the ExcelMenu /menu opener");
        c.that(backBranch.contains("closeInventory()"),
                "servermenu branch closes the picker before opening the Server Menu");

        // ---- FIX-1 boards: slot 53 is Back (save + picker), Close action gone
        c.eq(5, count(views, "boardBackItem()"),
                "boardBackItem defined once and placed on WS, Memory, Sudoku controls, Sudoku 9x9 palette");
        c.eq(3, count(views, "h.actions.put(SLOT_CLOSE, \"back:picker\")"),
                "WS board, Memory board and the shared Sudoku controls put Back at slot 53");
        c.eq(0, count(views, "\"ctl:close\""),
                "no renderer still emits the old close-only board action");
        c.eq(8, count(views, "\"back:picker\""),
                "back:picker registrations: theme, tier, resume, riddle (carried) + WS, Memory, Sudoku controls, Sudoku 9x9 (new)");

        // ---- Sudoku 9x9 slot decision: slot 51 Back, Eraser on the high bank
        c.that(views.contains("h.actions.put(51, \"back:picker\")"),
                "Sudoku 9x9 Back sits at slot 51 (the old standalone Eraser slot)");
        c.that(views.contains("h.actions.put(49, \"spal:erase\")"),
                "Sudoku 9x9 Eraser rides the high palette bank at slot 49");
        c.that(views.contains("\"spal:bank\"") && views.contains("\"spal:check\"")
                && views.contains("\"spal:page\"") && views.contains("\"spal:show\""),
                "Sudoku bank switch, check, page toggle and show-board controls all survive");

        // ---- the back:picker handler saves before it navigates
        c.that(backBranch.contains("svc.store().save(pd)"),
                "back:picker handler saves the player data explicitly");
        c.that(backBranch.indexOf("svc.store().save(pd)") < backBranch.indexOf("views.picker")
                && backBranch.indexOf("views.picker") >= 0,
                "back:picker handler saves BEFORE reopening the picker");
        c.that(!backBranch.contains("wordSearch = null") && !backBranch.contains("sudoku = null")
                && !backBranch.contains("memory = null") && !backBranch.contains("quickMath = null")
                && !backBranch.contains("scramble = null"),
                "back handler never discards a round (no round field is nulled)");

        // ---- FIX-1 question cards (Quick Math + Word Scramble share the card)
        c.that(views.contains("h.actions.put(18, \"qback\")"),
                "question card Back sits at slot 18");
        c.eq(1, count(views, "\"qback\""), "question card registers exactly one qback action");
        c.that(views.contains("\"qfinish\"") && views.contains("\"qstop\"") && views.contains("\"qclose\""),
                "Finish now / Save & stop / Close controls all survive beside the new Back");
        String qback = branch(listener, "case \"qback\"");
        c.that(qback.contains("svc.store().save(pd)"), "qback handler saves the round");
        c.that(qback.contains("clearInput"), "qback handler clears typed-answer mode like Close");
        c.that(qback.contains("views.picker"), "qback handler reopens the game picker");
        c.that(!qback.contains("completeScramble") && !qback.contains("completeQuickMath")
                && !qback.contains("= null"),
                "qback never banks, completes or discards the round");

        c.finish();
    }
}
