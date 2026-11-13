// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.registry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One player's stored menu arrangement (ExcelMenu 3.0, DESIGN §3.3):
 * ids only — never weights, labels, or copies of entry data. A group
 * absent from {@code groups} means "default order for that group";
 * a null/empty {@code groupOrder} means "default picker order".
 * Reset = delete the record; there is no snapshot of the default
 * anywhere (DESIGN §3.2, by-construction default).
 */
public record PlayerMenuPrefs(
        int version,
        String menu,
        List<String> groupOrder,
        Map<String, List<String>> groups) {

    public static final int CURRENT_VERSION = 1;
    public static final String MENU = "server_menu";

    public PlayerMenuPrefs {
        groupOrder = groupOrder == null ? List.of() : List.copyOf(groupOrder);
        Map<String, List<String>> copy = new LinkedHashMap<>();
        if (groups != null) {
            for (Map.Entry<String, List<String>> e : groups.entrySet()) {
                copy.put(e.getKey(), e.getValue() == null ? List.of() : List.copyOf(e.getValue()));
            }
        }
        groups = java.util.Collections.unmodifiableMap(copy);
    }

    public static PlayerMenuPrefs empty(String menu) {
        return new PlayerMenuPrefs(CURRENT_VERSION, menu, List.of(), Map.of());
    }

    public boolean isEmpty() {
        return groupOrder.isEmpty() && groups.values().stream().allMatch(List::isEmpty)
                && groups.isEmpty();
    }

    public List<String> groupList(String group) {
        return groups.getOrDefault(group, List.of());
    }

    public boolean hasGroup(String group) {
        return groups.containsKey(group);
    }

    public PlayerMenuPrefs withGroup(String group, List<String> ids) {
        Map<String, List<String>> copy = new LinkedHashMap<>(groups);
        copy.put(group, List.copyOf(ids));
        return new PlayerMenuPrefs(version, menu, groupOrder, copy);
    }

    public PlayerMenuPrefs withoutGroup(String group) {
        Map<String, List<String>> copy = new LinkedHashMap<>(groups);
        copy.remove(group);
        return new PlayerMenuPrefs(version, menu, groupOrder, copy);
    }

    public PlayerMenuPrefs withGroupOrder(List<String> order) {
        return new PlayerMenuPrefs(version, menu, order, groups);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
