// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.registry;

import com.winkreyes.excelmenu.core.platform.Platform;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turns the registry into the visible entry list for ONE viewer at
 * ONE moment — the seam both renderers consume, and the home of the
 * Amendment A guarded-entry contract.
 *
 * Resolution order (per entry, per open — never cached):
 * <ol>
 *   <li><b>Menu + platform filter.</b> Wrong menu, or the viewer's
 *       platform is not in the entry's {@code platforms:} — not
 *       rendered. (Targeting, not failure.)</li>
 *   <li><b>Date window.</b> {@code visible-from}/{@code visible-until}
 *       are evaluated HERE, at render time, in the configured zone,
 *       both days inclusive. Outside the window the entry simply is
 *       not rendered — no reload, no file edit, ever.</li>
 *   <li><b>Guards (Amendment A), applied uniformly:</b>
 *       {@code requires-plugin} absent, {@code requires-permission}
 *       not held, or the {@code status-placeholder} unresolvable /
 *       resolving to an unlisted state — ANY of these resolves the
 *       entry to its {@code default} variant (or its base
 *       presentation when it has no states) with the action REPLACED
 *       by the standard unavailable message. The door stays visible
 *       and explains itself; nothing dispatches; and there is zero
 *       per-plugin renderer code — this method is the only place
 *       guards exist.</li>
 *   <li><b>State selection.</b> All guards green: the variant named
 *       by the placeholder's value renders (its null components
 *       inherit the base entry).</li>
 * </ol>
 *
 * Ordering: Java packs by (group sequence from the menu layout,
 * {@code order}, id). Bedrock sorts by the FLAT {@code order-bedrock:}
 * weight (falling back to {@code order}) because the 1.5.27 list it
 * must reproduce interleaves groups.
 */
public final class EntryResolver {

    private EntryResolver() {}

    public static List<ResolvedEntry> resolve(String menuId, Iterable<EntryDef> all,
                                              ViewerContext viewer, Clock clock,
                                              RegistryConfig config) {
        LocalDate today = LocalDate.now(clock.withZone(config.zone()));
        List<ResolvedEntry> out = new ArrayList<>();
        for (EntryDef e : all) {
            if (!e.menu().equals(menuId)) continue;
            if (!e.platforms().contains(viewer.platform())) continue;
            if (e.visibleFrom() != null && today.isBefore(e.visibleFrom())) continue;
            if (e.visibleUntil() != null && today.isAfter(e.visibleUntil())) continue;
            out.add(resolveOne(e, viewer, config));
        }
        out.sort(comparator(menuId, viewer.platform(), config));
        return List.copyOf(out);
    }

    private static ResolvedEntry resolveOne(EntryDef e, ViewerContext viewer,
                                            RegistryConfig config) {
        String failure = null;
        String stateValue = null;
        EntryDef.Variant variant = null;

        if (e.requiresPlugin() != null && !viewer.pluginPresent(e.requiresPlugin())) {
            failure = "plugin " + e.requiresPlugin() + " absent";
        } else if (e.requiresPermission() != null
                && !viewer.hasPermission(e.requiresPermission())) {
            failure = "permission " + e.requiresPermission() + " missing";
        } else if (e.statusPlaceholder() != null) {
            stateValue = viewer.resolvePlaceholder(e.statusPlaceholder());
            if (stateValue == null) {
                failure = "state unresolvable for " + e.statusPlaceholder();
            } else {
                variant = e.variant(stateValue);
                if (variant == null) {
                    failure = "state '" + stateValue + "' unknown for "
                            + e.statusPlaceholder();
                }
            }
        }

        if (failure != null) {
            EntryDef.Variant dv = e.defaultVariant();
            String icon = dv != null && dv.icon() != null ? dv.icon() : e.icon();
            String label = dv != null && dv.label() != null ? dv.label() : e.label();
            List<String> lore = dv != null && dv.lore() != null ? dv.lore() : e.lore();
            return new ResolvedEntry(e, icon, label, lore,
                    config.unavailableAction(), stateValue, failure);
        }
        if (variant != null) {
            String icon = variant.icon() != null ? variant.icon() : e.icon();
            String label = variant.label() != null ? variant.label() : e.label();
            List<String> lore = variant.lore() != null ? variant.lore() : e.lore();
            List<String> action = variant.action() != null ? variant.action() : e.action();
            return new ResolvedEntry(e, icon, label, lore, action, stateValue, null);
        }
        return new ResolvedEntry(e, e.icon(), e.label(), e.lore(), e.action(), null, null);
    }

    private static Comparator<ResolvedEntry> comparator(String menuId, Platform platform,
                                                        RegistryConfig config) {
        if (platform == Platform.BEDROCK) {
            return Comparator.comparingInt((ResolvedEntry r) -> r.entry().bedrockOrder())
                    .thenComparing(ResolvedEntry::id);
        }
        List<String> groups = config.groupsFor(menuId);
        return Comparator
                .comparingInt((ResolvedEntry r) -> {
                    int i = groups.indexOf(r.entry().group());
                    return i >= 0 ? i : groups.size();
                })
                .thenComparing(r -> r.entry().group())
                .thenComparingInt(r -> r.entry().order())
                .thenComparing(ResolvedEntry::id);
    }

    /**
     * The label a Bedrock form button carries for a resolved entry:
     * the group's colour prefix + the label with every legacy colour
     * code stripped (BedrockMenu 1.5.21's rule — buttons are light
     * grey; group colour is the only colour a button gets).
     */
    public static String bedrockButtonLabel(ResolvedEntry r, RegistryConfig config) {
        return toSection(config.groupColor(r.entry().group())) + stripLegacy(r.label());
    }

    /** Remove &amp;x and §x legacy codes (code characters only — a
     *  bare '&' in prose, as in "Jobs & Money", is not a code). */
    public static String stripLegacy(String s) {
        if (s == null) return "";
        return s.replaceAll("[&§][0-9a-fA-Fk-oK-OrRxX]", "");
    }

    private static String toSection(String s) {
        return s == null ? "" : s.replace('&', '§');
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
