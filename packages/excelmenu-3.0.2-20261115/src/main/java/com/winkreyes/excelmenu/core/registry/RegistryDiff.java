// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * The reload diff summary (menu-edit census P4): what changed between
 * the registry set that was live and the set a reload just produced —
 * added / removed / changed entry ids. J7/J10/J11 in the census share
 * a shape: a bad menu edit was discovered by a player or a later audit
 * instead of at apply time. This summary is printed on every reload
 * (and at boot) so a regression is visible in the log the minute it
 * lands.
 */
public final class RegistryDiff {

    public record Diff(List<String> added, List<String> removed, List<String> changed,
                       int unchanged) {
        public boolean empty() {
            return added.isEmpty() && removed.isEmpty() && changed.isEmpty();
        }

        public String summary() {
            StringBuilder sb = new StringBuilder("registry diff:");
            sb.append(" +").append(added.size()).append(" added");
            if (!added.isEmpty()) sb.append(" (").append(String.join(", ", added)).append(")");
            sb.append(", -").append(removed.size()).append(" removed");
            if (!removed.isEmpty()) {
                sb.append(" (").append(String.join(", ", removed)).append(")");
            }
            sb.append(", ~").append(changed.size()).append(" changed");
            if (!changed.isEmpty()) {
                sb.append(" (").append(String.join(", ", changed)).append(")");
            }
            sb.append(", ").append(unchanged).append(" unchanged");
            return sb.toString();
        }
    }

    private RegistryDiff() {}

    public static Diff between(Map<String, EntryDef> before, Map<String, EntryDef> after) {
        TreeSet<String> added = new TreeSet<>(after.keySet());
        added.removeAll(before.keySet());
        TreeSet<String> removed = new TreeSet<>(before.keySet());
        removed.removeAll(after.keySet());
        List<String> changed = new ArrayList<>();
        int unchanged = 0;
        for (Map.Entry<String, EntryDef> e : after.entrySet()) {
            EntryDef old = before.get(e.getKey());
            if (old == null) continue;
            if (old.equals(e.getValue())) unchanged++;
            else changed.add(e.getKey());
        }
        changed.sort(String::compareTo);
        return new Diff(List.copyOf(added), List.copyOf(removed), changed, unchanged);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
