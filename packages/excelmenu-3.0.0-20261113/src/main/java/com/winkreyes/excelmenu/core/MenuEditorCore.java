// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core;

import java.util.ArrayList;
import java.util.List;

/**
 * The move algebra behind both rearrange editors (ExcelMenu 3.0,
 * DESIGN §3.5/§3.6) — pure, so the Bedrock Up/Down buttons and the
 * Java select-and-insert mechanic are proven to be the SAME algebra:
 * an insert-move equals the Up/Down sequence that bubbles the entry
 * to the same place (property-pinned in MenuEditorCoreTest).
 * Boundary moves are honest no-ops carrying a note, never fake
 * affordances.
 */
public final class MenuEditorCore {

    public record MoveResult(List<String> list, boolean moved, String note) {
        public MoveResult { list = List.copyOf(list); }
    }

    private MenuEditorCore() {}

    public static MoveResult moveUp(List<String> list, String id) {
        int i = list.indexOf(id);
        if (i < 0) return new MoveResult(list, false, "That entry is no longer in this section.");
        if (i == 0) return new MoveResult(list, false, "Already at the top.");
        List<String> out = new ArrayList<>(list);
        out.remove(i);
        out.add(i - 1, id);
        return new MoveResult(out, true, null);
    }

    public static MoveResult moveDown(List<String> list, String id) {
        int i = list.indexOf(id);
        if (i < 0) return new MoveResult(list, false, "That entry is no longer in this section.");
        if (i == list.size() - 1) return new MoveResult(list, false, "Already at the bottom.");
        List<String> out = new ArrayList<>(list);
        out.remove(i);
        out.add(i + 1, id);
        return new MoveResult(out, true, null);
    }

    public static MoveResult moveToTop(List<String> list, String id) {
        int i = list.indexOf(id);
        if (i < 0) return new MoveResult(list, false, "That entry is no longer in this section.");
        if (i == 0) return new MoveResult(list, false, "Already at the top.");
        List<String> out = new ArrayList<>(list);
        out.remove(i);
        out.add(0, id);
        return new MoveResult(out, true, null);
    }

    public static MoveResult moveToBottom(List<String> list, String id) {
        int i = list.indexOf(id);
        if (i < 0) return new MoveResult(list, false, "That entry is no longer in this section.");
        if (i == list.size() - 1) return new MoveResult(list, false, "Already at the bottom.");
        List<String> out = new ArrayList<>(list);
        out.remove(i);
        out.add(id);
        return new MoveResult(out, true, null);
    }

    /** The Java editor's move: take {@code movingId} out and insert
     *  it immediately BEFORE {@code targetId}. Inserting before the
     *  entry right after its current place (or onto itself) is a
     *  no-op. */
    public static MoveResult insertBefore(List<String> list, String movingId, String targetId) {
        int from = list.indexOf(movingId);
        int to = list.indexOf(targetId);
        if (from < 0 || to < 0) {
            return new MoveResult(list, false, "That entry is no longer in this section.");
        }
        if (from == to) return new MoveResult(list, false, null);
        List<String> out = new ArrayList<>(list);
        out.remove(from);
        int insertAt = out.indexOf(targetId);
        out.add(insertAt, movingId);
        return new MoveResult(out, true, null);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
