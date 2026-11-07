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

    // ------------------------------------------------------------ Max Organization (§15.6)

    /**
     * The Bedrock Max Organization flow — the same feature, tap-only:
     * an unlock form (state + pitch up front, rent/buy behind a modal
     * confirm, exactly like the Java screens) and the network as a
     * button list. Every action routes through MaxOrgService, so the
     * charge path, the rights rule, and the fail-closed moves are the
     * Java ones, unchanged.
     */
    public void openMaxOrg(Player player) {
        if (plugin.maxOrg().entitled(player.getUniqueId())) openNetworkForm(player, 0);
        else openUnlockForm(player);
    }

    private void openUnlockForm(Player player) {
        FloodgatePlayer fp = floodgate(player);
        if (fp == null) return;
        var mo = plugin.maxOrg();
        StringBuilder content = new StringBuilder();
        content.append(plain(plugin.messages().raw(mo.stateKey(player.getUniqueId()),
                "Max Organization isn't active for you right now.", mo.stateVars(player.getUniqueId()))));
        content.append("\n\n");
        for (String line : mo.pitchLines()) content.append(plain(line)).append("\n");
        List<String> labels = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        com.winkreyes.excelautosort.core.MaxOrgCore.Access access = mo.access(player.getUniqueId());
        if (access != com.winkreyes.excelautosort.core.MaxOrgCore.Access.NONE) {
            labels.add(plain(plugin.messages().raw("maxorg-open-network", "Open my chests")));
            actions.add("open");
        }
        if (access != com.winkreyes.excelautosort.core.MaxOrgCore.Access.OWNED) {
            labels.add(plain(plugin.messages().raw("maxorg-rent-button", "Rent — {rent} for {days} days", mo.pitchVars())));
            actions.add("rent");
            labels.add(plain(plugin.messages().raw("maxorg-buy-button", "Buy — {buy}, yours forever", mo.pitchVars())));
            actions.add("buy");
        }
        labels.add(plain(plugin.messages().raw("done", "Done")));
        actions.add("done");
        SimpleForm.Builder b = SimpleForm.builder()
                .title(plain(plugin.messages().raw("gui-maxorg-title", "Max Organization")))
                .content(content.toString().trim());
        for (String label : labels) b.button(label);
        SimpleForm form = b
                .validResultHandler(r -> onMain(() -> {
                    int id = r.clickedButtonId();
                    if (id < 0 || id >= actions.size()) return;
                    switch (actions.get(id)) {
                        case "open" -> openNetworkForm(player, 0);
                        case "rent" -> openPurchaseConfirm(player, false);
                        case "buy" -> openPurchaseConfirm(player, true);
                        default -> { /* Done — the form simply closes. */ }
                    }
                }))
                .build();
        fp.sendForm(form);
    }

    private void openPurchaseConfirm(Player player, boolean buy) {
        FloodgatePlayer fp = floodgate(player);
        if (fp == null) return;
        var mo = plugin.maxOrg();
        StringBuilder content = new StringBuilder();
        for (String line : mo.confirmLines(buy)) content.append(plain(line)).append("\n");
        double bal = plugin.economy().available() ? plugin.economy().balance(player.getUniqueId()) : -1;
        if (bal >= 0) {
            content.append("\nYour balance: ").append(com.winkreyes.excelautosort.core.MaxOrgCore.money(bal));
        }
        ModalForm form = ModalForm.builder()
                .title(plain(plugin.messages().raw("gui-maxorg-confirm-title", "Unlock Max Organization?")))
                .content(content.toString().trim())
                .button1(plain(plugin.messages().raw("maxorg-confirm-charge", "Charge me {price}",
                        Map.of("price", com.winkreyes.excelautosort.core.MaxOrgCore.money(
                                buy ? mo.buyPrice() : mo.rentPrice())))))
                .button2(plain(plugin.messages().raw("confirm-not-yet", "Not yet")))
                .validResultHandler(r -> onMain(() -> {
                    if (r.clickedFirst()) {
                        var outcome = mo.purchase(player, buy);
                        mo.tellOutcome(player, outcome, buy);
                        if (outcome == com.winkreyes.excelautosort.engine.MaxOrgService.PurchaseOutcome.RENTED
                                || outcome == com.winkreyes.excelautosort.engine.MaxOrgService.PurchaseOutcome.BOUGHT) {
                            openNetworkForm(player, 0);
                            return;
                        }
                    }
                    openUnlockForm(player);
                }))
                .closedOrInvalidResultHandler(() -> onMain(() -> openUnlockForm(player)))
                .build();
        fp.sendForm(form);
    }

    private void openNetworkForm(Player player, int page) {
        FloodgatePlayer fp = floodgate(player);
        if (fp == null) return;
        var mo = plugin.maxOrg();
        List<com.winkreyes.excelautosort.engine.MaxOrgService.NetChest> network = mo.resolveNetwork(player);
        List<com.winkreyes.excelautosort.core.MaxOrgCore.ViewEntry> entries = mo.viewOf(network);
        int pageSize = mo.pageSize();
        int pages = Math.max(1, (entries.size() + pageSize - 1) / pageSize);
        int pg = Math.min(Math.max(0, page), pages - 1);
        List<String> labels = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        List<com.winkreyes.excelautosort.core.MaxOrgCore.ViewEntry> shown = new ArrayList<>();
        String content;
        if (network.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String line : plugin.messages().rawList("maxorg-network-empty", List.of(
                    "None of your chests are within reach here.",
                    "Stand closer to your storage and try again."), Map.of())) {
                sb.append(plain(line)).append("\n");
            }
            content = sb.toString().trim();
        } else if (entries.isEmpty()) {
            content = "Your chests in reach are all empty right now.";
        } else {
            content = network.size() + (network.size() == 1 ? " chest" : " chests") + " in reach — tap a stack to take it.";
            int from = pg * pageSize;
            for (int i = from; i < Math.min(entries.size(), from + pageSize); i++) {
                var e = entries.get(i);
                shown.add(e);
                labels.add(Names.friendly(e.stack().material()) + " ×" + e.stack().count()
                        + " — chest at " + e.x() + ", " + e.y() + ", " + e.z());
                actions.add("take");
            }
        }
        labels.add(plain(plugin.messages().raw("maxorg-deposit-button", "Put my things away")));
        actions.add("deposit");
        labels.add(plain(plugin.messages().raw("gui-maxorg-title", "Max Organization")));
        actions.add("unlock");
        if (pages > 1 && pg > 0) { labels.add("Previous page"); actions.add("prev"); }
        if (pages > 1 && pg < pages - 1) { labels.add("Next page"); actions.add("next"); }
        labels.add(plain(plugin.messages().raw("done", "Done")));
        actions.add("done");
        SimpleForm.Builder b = SimpleForm.builder()
                .title(plain(plugin.messages().raw("gui-network-title", "My chests around me")))
                .content(content);
        for (String label : labels) b.button(label);
        int currentPage = pg;
        SimpleForm form = b
                .validResultHandler(r -> onMain(() -> {
                    int id = r.clickedButtonId();
                    if (id < 0 || id >= actions.size()) return;
                    switch (actions.get(id)) {
                        case "take" -> {
                            var e = shown.get(id);
                            com.winkreyes.excelautosort.engine.MaxOrgService.NetChest nc = null;
                            for (var c : network) if (c.key().equals(e.chestKey())) nc = c;
                            if (nc == null) { openNetworkForm(player, currentPage); return; }
                            int moved = mo.take(player, nc, e.slot());
                            if (moved < 0) {
                                plugin.messages().send(player, "maxorg-chest-gone",
                                        "&eThat chest isn't in your network anymore — the view has been refreshed.");
                            } else if (moved == 0) {
                                plugin.messages().send(player, "maxorg-no-room",
                                        "&eNo room in your inventory for that right now.");
                            }
                            openNetworkForm(player, currentPage);
                        }
                        case "deposit" -> {
                            var report = mo.smartDeposit(player);
                            plugin.maxOrgGui().tellDeposit(player, report);
                            openNetworkForm(player, currentPage);
                        }
                        case "unlock" -> openUnlockForm(player);
                        case "prev" -> openNetworkForm(player, currentPage - 1);
                        case "next" -> openNetworkForm(player, currentPage + 1);
                        default -> { /* Done — the form simply closes. */ }
                    }
                }))
                .build();
        fp.sendForm(form);
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
