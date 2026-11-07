package com.winkreyes.excelautosort.bedrock;

import com.winkreyes.excelautosort.ExcelAutoSort;
import com.winkreyes.excelautosort.Names;
import com.winkreyes.excelautosort.core.BufferCore;
import com.winkreyes.excelautosort.core.FilterCore;
import com.winkreyes.excelautosort.core.Mode;
import com.winkreyes.excelautosort.core.SelectorModel;
import com.winkreyes.excelautosort.engine.SelectorFlow;
import com.winkreyes.excelautosort.gui.SelectorSession;
import com.winkreyes.excelautosort.store.BufferEntry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Bedrock selector (SPEC §7.5) — the ONLY class in ExcelAutoSort
 * that touches Floodgate/Cumulus, loaded only when Floodgate is
 * present. Same flow, same words, same safety semantics as the Java
 * GUI: it renders {@link SelectorFlow}'s model as forms — mode as a
 * dropdown with the current mode pre-selected and named, the
 * contents-seeded list as toggles starting at the current state, and
 * the arm/confirm as a modal carrying the FULL itemized reject list
 * in its text (never behind a tap).
 *
 * Form responses arrive off the main thread; every state change is
 * re-dispatched onto it before touching the flow.
 */
public final class BedrockFormsHook {

    private final ExcelAutoSort plugin;

    public BedrockFormsHook(ExcelAutoSort plugin) {
        this.plugin = plugin;
    }

    public boolean isBedrock(Player p) {
        try {
            return FloodgateApi.getInstance().getPlayer(p.getUniqueId()) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private FloodgatePlayer floodgate(Player p) {
        try {
            return FloodgateApi.getInstance().getPlayer(p.getUniqueId());
        } catch (Throwable t) {
            return null;
        }
    }

    private void onMain(Runnable r) {
        Bukkit.getScheduler().runTask(plugin, r);
    }

    /** Bedrock forms take plain text: strip the '&' legacy codes the chat/GUI strings carry. */
    private static String plain(String s) {
        return s == null ? "" : s.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
    }

    // ------------------------------------------------------------ main form

    public void openSelector(Player player, List<String> targetKeys) {
        SelectorSession s = plugin.flow().open(player, targetKeys);
        if (s == null) { // §6.4 refusal: plain line, no form, no settings leak.
            plugin.messages().send(player, "no-permission", "&cYou don't have permission for that.");
            return;
        }
        openMain(player, s);
    }

    private void openMain(Player player, SelectorSession s) {
        FloodgatePlayer fp = floodgate(player);
        if (fp == null) return;
        ChestStateText state = stateText(s);
        SimpleForm form = SimpleForm.builder()
                .title(plain(plugin.messages().raw("gui-selector-title", "Chest filter")))
                .content(state.text())
                .button(plain(plugin.messages().raw("form-list-button", "Filter list & mode")))
                .button(handButtonText(player))
                .button(plain(plugin.messages().raw("done", "&aDone")))
                .validResultHandler(r -> onMain(() -> {
                    switch (r.clickedButtonId()) {
                        case 0 -> openListForm(player, s);
                        case 1 -> {
                            ItemStack hand = player.getInventory().getItemInMainHand();
                            if (hand == null || hand.getType() == Material.AIR) {
                                plugin.messages().send(player, "hand-empty",
                                        "&eYour hand is empty — hold the item you want to add.");
                                openMain(player, s);
                            } else {
                                SelectorFlow.Result res = plugin.flow().addItem(s, hand.getType().name());
                                if (res == SelectorFlow.Result.NEEDS_CONFIRM) openConfirm(player, s);
                                else openMain(player, s);
                            }
                        }
                        default -> { /* Done — the form simply closes. */ }
                    }
                }))
                .build();
        fp.sendForm(form);
    }

    private String handButtonText(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            return plain(plugin.messages().raw("add-from-hand", "Add from my hand")) + " (hand empty)";
        }
        return plain(plugin.messages().raw("add-from-hand", "Add from my hand"))
                + ": " + Names.friendly(hand.getType().name());
    }

    private record ChestStateText(String text) {}

    private ChestStateText stateText(SelectorSession s) {
        StringBuilder sb = new StringBuilder();
        sb.append("Filter mode now: ").append(plain(modeName(s.workMode))).append("\n");
        sb.append("On the list: ").append(s.workItems.size()).append(" kinds\n");
        if (plugin.flow().isSuspended(s)) {
            sb.append("Paused after a claim change — arm it again to restart it.\n");
        }
        return new ChestStateText(sb.toString().trim());
    }

    private String modeName(Mode mode) {
        return plugin.messages().raw(switch (mode) {
            case OFF -> "mode-off";
            case WHITELIST -> "mode-whitelist";
            case BLACKLIST -> "mode-blacklist";
        }, mode.id());
    }

    // ------------------------------------------------------------ list form

    private void openListForm(Player player, SelectorSession s) {
        FloodgatePlayer fp = floodgate(player);
        if (fp == null) return;
        List<SelectorModel.Entry> entries = plugin.flow().entriesOf(s);
        CustomForm.Builder b = CustomForm.builder()
                .title(plain(plugin.messages().raw("gui-selector-title", "Chest filter")))
                .dropdown("Filter mode (now: " + plain(modeName(s.workMode)) + ")",
                        List.of(plain(modeName(Mode.OFF)), plain(modeName(Mode.WHITELIST)),
                                plain(modeName(Mode.BLACKLIST))),
                        SelectorModel.modeIndex(s.workMode));
        for (SelectorModel.Entry e : entries) {
            String label = Names.friendly(e.material())
                    + (e.inChest() > 0 ? " ×" + e.inChest() + " in the chest" : " — not in this chest right now")
                    + (e.onList() ? " — on the list" : "");
            b.toggle(label, e.onList());
        }
        CustomForm form = b
                .validResultHandler(r -> onMain(() -> {
                    int modeIdx = 0;
                    Set<String> newItems = new LinkedHashSet<>();
                    try {
                        Object mv = r.valueAt(0);
                        if (mv instanceof Number n) modeIdx = n.intValue();
                        for (int i = 0; i < entries.size(); i++) {
                            Object tv = r.valueAt(i + 1);
                            if (tv instanceof Boolean on && on) newItems.add(entries.get(i).material());
                        }
                    } catch (RuntimeException ex) {
                        return; // A malformed response changes nothing.
                    }
                    Mode newMode = SelectorModel.modeAt(modeIdx);
                    s.workItems.clear();
                    s.workItems.addAll(newItems);
                    if (newMode != s.workMode || plugin.flow().isSuspended(s)) {
                        SelectorFlow.Result res = plugin.flow().requestMode(s, newMode);
                        if (res == SelectorFlow.Result.NEEDS_CONFIRM) { openConfirm(player, s); return; }
                        openMain(player, s);
                    } else {
                        SelectorFlow.Result res = plugin.flow().applyListSet(s, newItems);
                        if (res == SelectorFlow.Result.NEEDS_CONFIRM) { openConfirm(player, s); return; }
                        openMain(player, s);
                    }
                }))
                .closedOrInvalidResultHandler(() -> onMain(() -> openMain(player, s)))
                .build();
        fp.sendForm(form);
    }

    // ------------------------------------------------------------ confirm modal

    private void openConfirm(Player player, SelectorSession s) {
        FloodgatePlayer fp = floodgate(player);
        if (fp == null) return;
        List<FilterCore.RejectLine> rejects = plugin.flow().rejectsOf(s, s.pendingMode, s.pendingItems);
        StringBuilder content = new StringBuilder();
        content.append(plain(plugin.gui().modeWords(s.pendingMode,
                s.pendingItems == null ? 0 : s.pendingItems.size())));
        if (!rejects.isEmpty()) {
            content.append("\n\nThese are in the chest and don't pass:\n");
            for (FilterCore.RejectLine l : rejects) {
                content.append("- ").append(Names.friendly(l.material()))
                        .append(" ×").append(l.totalCount()).append("\n");
            }
            content.append("Total: ").append(FilterCore.totalItems(rejects))
                    .append(" items in ").append(FilterCore.totalStacks(rejects)).append(" stacks");
        }
        content.append("\n\nThey wait in your recovery box for ")
                .append(BufferCore.timeLeft(System.currentTimeMillis()
                                + plugin.getConfig().getLong("recovery.hours", 168) * 3_600_000L,
                        System.currentTimeMillis()))
                .append(".");
        ModalForm form = ModalForm.builder()
                .title(plain(plugin.messages().raw("gui-confirm-title", "Turn the filter on?")))
                .content(content.toString())
                .button1(plain(rejects.isEmpty()
                        ? plugin.messages().raw("confirm-on", "&aTurn the filter on")
                        : plugin.messages().raw("confirm-sweep",
                                "&aMove those to my recovery box and turn the filter on")))
                .button2(plain(plugin.messages().raw("confirm-not-yet", "&cNot yet")))
                .validResultHandler(r -> onMain(() -> {
                    if (r.clickedFirst()) {
                        int[] swept = plugin.flow().confirm(player, s);
                        plugin.messages().send(player, "armed", "&aFilter on: {mode}",
                                Map.of("mode", Names.friendly(s.workMode.id())));
                        if (swept[0] > 0) {
                            plugin.messages().send(player, "swept", "&eMoved {count} items to your recovery box.",
                                    Map.of("count", String.valueOf(swept[1])));
                        }
                    } else {
                        plugin.flow().cancel(s);
                    }
                    openMain(player, s);
                }))
                .closedOrInvalidResultHandler(() -> onMain(() -> {
                    plugin.flow().cancel(s);
                    openMain(player, s);
                }))
                .build();
        fp.sendForm(form);
    }

    // ------------------------------------------------------------ recovery form

    public void openRecovery(Player player) {
        FloodgatePlayer fp = floodgate(player);
        if (fp == null) return;
        List<BufferEntry> entries = plugin.buffers().entriesOf(player.getUniqueId());
        SimpleForm.Builder b = SimpleForm.builder()
                .title(plain(plugin.messages().raw("gui-recovery-title", "Filter recovery box")))
                .content(entries.isEmpty()
                        ? plain(plugin.messages().raw("recovery-empty", "Nothing is waiting in your recovery box."))
                        : "Tap an item to claim it back.")
                .button(plain(plugin.messages().raw("claim-all", "Claim everything")));
        long now = System.currentTimeMillis();
        for (BufferEntry e : entries) {
            b.button(Names.friendly(displayName(e.materialKey)) + " ×" + e.count
                    + " — " + BufferCore.timeLeft(e.expiresAtMs, now) + " left");
        }
        b.button(plain(plugin.messages().raw("done", "Done")));
        SimpleForm form = b
                .validResultHandler(r -> onMain(() -> {
                    int id = r.clickedButtonId();
                    if (id == 0) {
                        int n = plugin.buffers().claimAll(player);
                        plugin.messages().send(player, "claimed", "&aClaimed {count} stacks from your recovery box.",
                                Map.of("count", String.valueOf(n)));
                    } else if (id >= 1 && id <= entries.size()) {
                        plugin.buffers().claim(player, entries.get(id - 1).id);
                        openRecovery(player);
                    }
                }))
                .build();
        fp.sendForm(form);
    }

    private static String displayName(String materialKey) {
        // BufferEntry stores the namespaced key; Names.friendly strips it.
        return materialKey;
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
