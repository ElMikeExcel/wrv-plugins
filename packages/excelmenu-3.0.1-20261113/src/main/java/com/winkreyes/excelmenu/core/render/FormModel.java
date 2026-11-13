// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.render;

import com.winkreyes.excelmenu.core.BedrockDetail;
import com.winkreyes.excelmenu.core.registry.EntryResolver;
import com.winkreyes.excelmenu.core.registry.RegistryConfig;
import com.winkreyes.excelmenu.core.registry.ResolvedEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Bedrock form renderer's brain — pure, Bukkit-free,
 * unit-tested. It consumes the resolver's output (already resolved
 * and Bedrock-ordered) plus the layout, and produces form pages the
 * Bukkit layer sends through the unified platform bridge.
 *
 * Three shapes, all layout-driven (no per-menu code):
 * <ul>
 *   <li><b>FLAT</b> (the native compass form): one form; every entry
 *       is a button labelled with its group colour + label (the
 *       resolver's {@code bedrockButtonLabel} rule, 1.5.27's
 *       colouring).</li>
 *   <li><b>GROUPED picker</b>: one button per non-empty group, in
 *       layout group order, labelled with the group's display
 *       name — prefixed with the group's colour from the config
 *       (3.0.1: the section colours Mike approved Nov 13, 2026;
 *       a group with no configured colour stays plain, so the
 *       commands catalogue's picker is unchanged).</li>
 *   <li><b>GROUPED group form</b>: the group's entries as buttons
 *       (3.0.1: each button carries the section colour, the same
 *       FLAT rule as the compass form — previously plain stripped
 *       labels), with a body listing every row as "label - first
 *       lore line" plus the layout's form note. Every entry is a
 *       button: a row whose action is a [message] hint speaks the
 *       hint when tapped (the Java behaviour), instead of having
 *       no button at all. One Back button is appended LAST (2.0.1):
 *       a group form is one step deep, and every menu/submenu
 *       always carries a way back to its picker (Mike's standing
 *       rule, Nov 13, 2026). The picker itself is the root and
 *       carries no Back.</li>
 * </ul>
 */
public final class FormModel {

    /** The group form's Back label — single wording source, shared
     *  with the definition-menu detail forms (BedrockDetail). */
    public static final String BACK_LABEL = BedrockDetail.BACK_LABEL;

    /** Where a button tap leads: an entry's action, a group form,
     *  or back to the grouped picker. */
    public sealed interface Target {
        record EntryTarget(String entryId) implements Target {}
        record GroupTarget(String group) implements Target {}
        record PickerTarget() implements Target {}
        /** The rearrange editor's chrome door (3.0, DESIGN §3.4):
         *  not an entry — no stats tap. */
        record EditorTarget() implements Target {}
    }

    /** The picker chrome button that opens the rearrange editor. */
    public static final String REARRANGE_LABEL = "Rearrange Menu";

    public record Button(String label, Target target) {}

    public record FormPage(String title, String content, List<Button> buttons) {
        public FormPage {
            buttons = buttons == null ? List.of() : List.copyOf(buttons);
            content = content == null ? "" : content;
        }
    }

    private FormModel() {}

    /** FLAT: the whole menu as one form (the native compass form).
     *  Pins render first, in pinned order, and are NOT duplicated
     *  in the remaining list (3.0, DESIGN §2.3). */
    public static FormPage flat(LayoutDef layout, List<ResolvedEntry> resolved,
                                RegistryConfig config) {
        List<Button> buttons = new ArrayList<>();
        java.util.Set<String> pinnedIds = new java.util.HashSet<>(layout.pinned());
        for (String pin : layout.pinned()) {
            for (ResolvedEntry r : resolved) {
                if (r.id().equals(pin)) {
                    buttons.add(new Button(pinLabel(r, config),
                            new Target.EntryTarget(r.id())));
                }
            }
        }
        for (ResolvedEntry r : resolved) {
            if (pinnedIds.contains(r.id())) continue;
            buttons.add(new Button(EntryResolver.bedrockButtonLabel(r, config),
                    new Target.EntryTarget(r.id())));
        }
        String title = layout.formTitle() != null ? layout.formTitle()
                : EntryResolver.stripLegacy(layout.title());
        return new FormPage(title, layout.formContent(), buttons);
    }

    /** GROUPED step 1: pick a group. Only groups with entries show. */
    public static FormPage picker(LayoutDef layout, List<ResolvedEntry> resolved) {
        return picker(layout, resolved, null, false, null);
    }

    /** GROUPED step 1 with config (pin labels carry group colour). */
    public static FormPage picker(LayoutDef layout, List<ResolvedEntry> resolved,
                                  RegistryConfig config) {
        return picker(layout, resolved, config, false, null);
    }

    /** GROUPED step 1 with the Rearrange chrome button (3.0). */
    public static FormPage picker(LayoutDef layout, List<ResolvedEntry> resolved,
                                  RegistryConfig config, boolean rearrangeEnabled) {
        return picker(layout, resolved, config, rearrangeEnabled, null);
    }

    /**
     * GROUPED step 1 (3.0 full form, DESIGN §2.3/§3.4): pin buttons
     * first (in {@code pinned} order, labels by the PIN colour rule,
     * 3.0.1 — dark blue, owned by the pins), then one button per
     * group that still has non-pinned entries —
     * in {@code groupOrderOverride} order when a player preference
     * supplies one, else layout order — then the Rearrange button
     * LAST when the layout is rearrange-enabled. The picker is the
     * root: it carries no Back button (2.0.1 rule, unchanged).
     */
    public static FormPage picker(LayoutDef layout, List<ResolvedEntry> resolved,
                                  RegistryConfig config, boolean rearrangeEnabled,
                                  List<String> groupOrderOverride) {
        List<Button> buttons = new ArrayList<>();
        java.util.Set<String> pinnedIds = new java.util.HashSet<>(layout.pinned());
        for (String pin : layout.pinned()) {
            for (ResolvedEntry r : resolved) {
                if (r.id().equals(pin)) {
                    String label = config != null
                            ? pinLabel(r, config)
                            : EntryResolver.stripLegacy(r.label());
                    buttons.add(new Button(label, new Target.EntryTarget(r.id())));
                }
            }
        }
        List<String> groups = groupOrderOverride != null && !groupOrderOverride.isEmpty()
                ? orderedGroups(layout, resolved, groupOrderOverride)
                : orderedGroups(layout, resolved);
        for (String g : groups) {
            boolean hasNonPinned = resolved.stream().anyMatch(
                    r -> r.entry().group().equals(g) && !pinnedIds.contains(r.id()));
            if (!hasNonPinned) continue;
            // 3.0.1: the section button carries its section colour
            // (config group-colors, '&' -> '§'); a null config or an
            // uncoloured group renders the plain display name, so
            // the commands catalogue's picker is unchanged.
            buttons.add(new Button(groupPrefix(config, g)
                    + layout.groups().getOrDefault(g, g),
                    new Target.GroupTarget(g)));
        }
        if (rearrangeEnabled) {
            buttons.add(new Button(REARRANGE_LABEL, new Target.EditorTarget()));
        }
        String title = layout.formTitle() != null ? layout.formTitle()
                : EntryResolver.stripLegacy(layout.title());
        return new FormPage(title, layout.formPickerContent(), buttons);
    }

    /** Groups in an explicit (player-preferred) order first, then
     *  layout order, then resolved order for undeclared groups. */
    static List<String> orderedGroups(LayoutDef layout, List<ResolvedEntry> resolved,
                                      List<String> preferred) {
        List<String> out = new ArrayList<>();
        for (String g : preferred) {
            if (!out.contains(g) && resolved.stream()
                    .anyMatch(r -> r.entry().group().equals(g))) out.add(g);
        }
        for (String g : orderedGroups(layout, resolved)) {
            if (!out.contains(g)) out.add(g);
        }
        return out;
    }

    /** GROUPED step 2: one group's rows, body + buttons. */
    public static FormPage group(LayoutDef layout, List<ResolvedEntry> resolved,
                                 String group) {
        return group(layout, resolved, group, null);
    }

    /**
     * GROUPED step 2 with config (3.0.1): entry buttons carry the
     * section colour — the same FLAT rule
     * ({@code EntryResolver.bedrockButtonLabel}) — instead of plain
     * stripped labels. A null config keeps the pre-3.0.1 plain
     * shape (the commands catalogue's groups have no colour
     * configured, so its group forms are unchanged either way).
     * The Back button stays plain: navigation chrome, not a door.
     */
    public static FormPage group(LayoutDef layout, List<ResolvedEntry> resolved,
                                 String group, RegistryConfig config) {
        List<Button> buttons = new ArrayList<>();
        List<String> body = new ArrayList<>();
        for (ResolvedEntry r : resolved) {
            if (!r.entry().group().equals(group)) continue;
            // Root-only pins (3.0, DESIGN §2.3): a pinned entry does
            // not also list in its home group's form.
            if (layout.pinned().contains(r.id())) continue;
            String label = config != null
                    ? EntryResolver.bedrockButtonLabel(r, config)
                    : EntryResolver.stripLegacy(r.label());
            String desc = r.lore().isEmpty() ? ""
                    : EntryResolver.stripLegacy(r.lore().get(0));
            // The body listing stays plain: only the BUTTON carries
            // the section colour (3.0.1).
            String plain = EntryResolver.stripLegacy(r.label());
            body.add(desc.isEmpty() ? plain : plain + " - " + desc);
            buttons.add(new Button(label, new Target.EntryTarget(r.id())));
        }
        // Back always LAST (2.0.1), mirroring BedrockDetail's
        // convention: returns to the picker (FormService re-resolves
        // fresh and re-opens it). Navigation chrome, not a door —
        // no entry id, no stats tap.
        buttons.add(new Button(BACK_LABEL, new Target.PickerTarget()));
        if (layout.formNote() != null && !layout.formNote().isBlank()) {
            body.add("");
            body.add(layout.formNote());
        }
        String title = layout.formTitle() != null ? layout.formTitle()
                : EntryResolver.stripLegacy(layout.title());
        return new FormPage(title, String.join("\n", body), buttons);
    }

    /** The group's colour prefix as a '§' code, or "" when the
     *  config is null or the group has no colour configured. */
    private static String groupPrefix(RegistryConfig config, String group) {
        if (config == null) return "";
        return config.groupColor(group).replace('&', '§');
    }

    /** A root pin's label (3.0.1): the PIN colour from the config
     *  (dark blue by default — blue is owned by the Spawn +
     *  Teleport pins) + the stripped label. Pins never take their
     *  home group's colour, so Travel going green leaves the pins
     *  exactly as Mike sees them today. */
    private static String pinLabel(ResolvedEntry r, RegistryConfig config) {
        return config.pinColor().replace('&', '§')
                + EntryResolver.stripLegacy(r.label());
    }

    /** Groups in layout declaration order, then any undeclared group
     *  in resolved order (a layout that forgot a group still serves
     *  it — declared in /excelmenu list, never silently dropped). */
    static List<String> orderedGroups(LayoutDef layout, List<ResolvedEntry> resolved) {
        List<String> out = new ArrayList<>();
        for (String g : layout.groups().keySet()) {
            if (resolved.stream().anyMatch(r -> r.entry().group().equals(g))) out.add(g);
        }
        for (ResolvedEntry r : resolved) {
            if (!out.contains(r.entry().group())) out.add(r.entry().group());
        }
        return out;
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
