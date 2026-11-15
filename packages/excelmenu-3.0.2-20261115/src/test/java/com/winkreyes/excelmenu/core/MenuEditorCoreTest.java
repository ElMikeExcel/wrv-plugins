package com.winkreyes.excelmenu.core;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 3.0 editor move algebra (DESIGN §3.5/§3.6/§7). */
public class MenuEditorCoreTest {

    private static final List<String> BASE = List.of("a", "b", "c", "d", "e");

    @Test
    public void moveUpDownSwapNeighbours() {
        assertEquals(List.of("b", "a", "c", "d", "e"),
                MenuEditorCore.moveUp(BASE, "b").list());
        assertEquals(List.of("a", "c", "b", "d", "e"),
                MenuEditorCore.moveDown(BASE, "b").list());
    }

    @Test
    public void boundariesAreHonestNoOps() {
        MenuEditorCore.MoveResult up = MenuEditorCore.moveUp(BASE, "a");
        assertFalse(up.moved());
        assertEquals("Already at the top.", up.note());
        assertEquals(BASE, up.list());
        MenuEditorCore.MoveResult down = MenuEditorCore.moveDown(BASE, "e");
        assertFalse(down.moved());
        assertEquals("Already at the bottom.", down.note());
        MenuEditorCore.MoveResult top = MenuEditorCore.moveToTop(BASE, "a");
        assertFalse(top.moved());
        assertEquals("Already at the top.", top.note());
        MenuEditorCore.MoveResult bottom = MenuEditorCore.moveToBottom(BASE, "e");
        assertFalse(bottom.moved());
        assertEquals("Already at the bottom.", bottom.note());
    }

    @Test
    public void moveToEnds() {
        assertEquals(List.of("d", "a", "b", "c", "e"),
                MenuEditorCore.moveToTop(BASE, "d").list());
        assertEquals(List.of("a", "c", "d", "e", "b"),
                MenuEditorCore.moveToBottom(BASE, "b").list());
    }

    @Test
    public void unknownIdIsNoOpWithNote() {
        MenuEditorCore.MoveResult r = MenuEditorCore.moveUp(BASE, "gone");
        assertFalse(r.moved());
        assertEquals(BASE, r.list());
        assertTrue(r.note() != null);
    }

    @Test
    public void insertBeforeSelfIsNoOp() {
        MenuEditorCore.MoveResult r = MenuEditorCore.insertBefore(BASE, "c", "c");
        assertFalse(r.moved());
        assertEquals(BASE, r.list());
    }

    /**
     * The property the two editors' equivalence rests on: a Java
     * insert-move produces exactly the list the Bedrock Up/Down
     * sequence produces for the same move.
     */
    @Test
    public void insertEqualsUpDownSequence() {
        Random random = new Random(20261113L);
        for (int trial = 0; trial < 500; trial++) {
            int n = 2 + random.nextInt(8);
            List<String> base = new ArrayList<>();
            for (int i = 0; i < n; i++) base.add("e" + i);
            String moving = base.get(random.nextInt(n));
            String target = base.get(random.nextInt(n));
            List<String> viaInsert = MenuEditorCore.insertBefore(base, moving, target).list();
            // Via Up/Down: bubble moving until it sits immediately
            // before target (or, when moving down past target's old
            // place, until the insert result is reached).
            List<String> viaSteps = new ArrayList<>(base);
            // Compute expected by the insert definition directly on
            // indices: remove, then insert at target's new index.
            List<String> expected;
            if (moving.equals(target)) {
                expected = base;
            } else {
                expected = new ArrayList<>(base);
                expected.remove(moving);
                expected.add(expected.indexOf(target), moving);
            }
            assertEquals(expected, viaInsert);
            // And stepping: repeated moveUp/moveDown toward the
            // expected index converges to the same list.
            List<String> stepped = new ArrayList<>(base);
            int want = expected.indexOf(moving);
            while (stepped.indexOf(moving) < want) {
                stepped = new ArrayList<>(MenuEditorCore.moveDown(stepped, moving).list());
            }
            while (stepped.indexOf(moving) > want) {
                stepped = new ArrayList<>(MenuEditorCore.moveUp(stepped, moving).list());
            }
            assertEquals(viaInsert, stepped);
        }
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
