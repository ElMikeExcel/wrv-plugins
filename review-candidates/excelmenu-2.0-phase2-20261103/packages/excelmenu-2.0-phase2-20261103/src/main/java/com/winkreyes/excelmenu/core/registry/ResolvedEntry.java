// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.registry;

import java.util.List;

/**
 * One entry as resolved for one viewer at one moment — the exact
 * input both renderers consume (Java chest: icon + label + lore;
 * Bedrock form: label; both: action on tap). Resolution happens
 * per open, never cached (DESIGN-2.0 §2.2's per-open rule).
 *
 * @param guardFailure null when every guard passed; otherwise the
 *                     Amendment A reason ("plugin ExcelSilkWatch
 *                     absent", "permission excelsilkwatch.toggle
 *                     missing", "state 'bogus' unknown for
 *                     %silkwarnings_status%"). When set, the
 *                     presentation is the entry's default variant
 *                     (or base) and {@code action} is the standard
 *                     unavailable message — the door is shown, the
 *                     tap explains itself, nothing dispatches.
 * @param stateValue   the resolved placeholder value that selected
 *                     the variant, or null for a stateless entry.
 */
public record ResolvedEntry(
        EntryDef entry,
        String icon,
        String label,
        List<String> lore,
        List<String> action,
        String stateValue,
        String guardFailure) {

    public ResolvedEntry {
        lore = lore == null ? List.of() : List.copyOf(lore);
        action = action == null ? List.of() : List.copyOf(action);
    }

    public String id() { return entry.id(); }

    public boolean guarded() { return guardFailure != null; }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
