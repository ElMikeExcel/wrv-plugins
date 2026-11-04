// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.render;

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
 *       name.</li>
 *   <li><b>GROUPED group form</b>: the group's entries as buttons
 *       (plain stripped labels — the 1.5.27 catalogue shape), with a
 *       body listing every row as "label - first lore line" plus
 *       the layout's form note. Every entry is a button: a row whose
 *       action is a [message] hint speaks the hint when tapped (the
 *       Java behaviour), instead of having no button at all.</li>
 * </ul>
 */
public final class FormModel {

    /** Where a button tap leads: an entry's action, or a group form. */
    public sealed interface Target {
        record EntryTarget(String entryId) implements Target {}
        record GroupTarget(String group) implements Target {}
    }

    public record Button(String label, Target target) {}

    public record FormPage(String title, String content, List<Button> buttons) {
        public FormPage {
            buttons = buttons == null ? List.of() : List.copyOf(buttons);
            content = content == null ? "" : content;
        }
    }

    private FormModel() {}

    /** FLAT: the whole menu as one form (the native compass form). */
    public static FormPage flat(LayoutDef layout, List<ResolvedEntry> resolved,
                                RegistryConfig config) {
        List<Button> buttons = new ArrayList<>();
        for (ResolvedEntry r : resolved) {
            buttons.add(new Button(EntryResolver.bedrockButtonLabel(r, config),
                    new Target.EntryTarget(r.id())));
        }
        String title = layout.formTitle() != null ? layout.formTitle()
                : EntryResolver.stripLegacy(layout.title());
        return new FormPage(title, layout.formContent(), buttons);
    }

    /** GROUPED step 1: pick a group. Only groups with entries show. */
    public static FormPage picker(LayoutDef layout, List<ResolvedEntry> resolved) {
        Map<String, Boolean> nonEmpty = new LinkedHashMap<>();
        for (String g : orderedGroups(layout, resolved)) nonEmpty.put(g, Boolean.TRUE);
        List<Button> buttons = new ArrayList<>();
        for (String g : nonEmpty.keySet()) {
            buttons.add(new Button(layout.groups().getOrDefault(g, g),
                    new Target.GroupTarget(g)));
        }
        String title = layout.formTitle() != null ? layout.formTitle()
                : EntryResolver.stripLegacy(layout.title());
        return new FormPage(title, layout.formPickerContent(), buttons);
    }

    /** GROUPED step 2: one group's rows, body + buttons. */
    public static FormPage group(LayoutDef layout, List<ResolvedEntry> resolved,
                                 String group) {
        List<Button> buttons = new ArrayList<>();
        List<String> body = new ArrayList<>();
        for (ResolvedEntry r : resolved) {
            if (!r.entry().group().equals(group)) continue;
            String label = EntryResolver.stripLegacy(r.label());
            String desc = r.lore().isEmpty() ? ""
                    : EntryResolver.stripLegacy(r.lore().get(0));
            body.add(desc.isEmpty() ? label : label + " - " + desc);
            buttons.add(new Button(label, new Target.EntryTarget(r.id())));
        }
        if (layout.formNote() != null && !layout.formNote().isBlank()) {
            body.add("");
            body.add(layout.formNote());
        }
        String title = layout.formTitle() != null ? layout.formTitle()
                : EntryResolver.stripLegacy(layout.title());
        return new FormPage(title, String.join("\n", body), buttons);
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
