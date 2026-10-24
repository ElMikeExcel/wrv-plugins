package com.winkreyes.exceldeathrecord;

import com.winkreyes.exceldeathrecord.core.DeathRecord;
import com.winkreyes.exceldeathrecord.core.RecordIndex;
import com.winkreyes.exceldeathrecord.core.Selection;
import com.winkreyes.exceldeathrecord.core.SlotPlanner;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/**
 * The tap menu for /deathrecord restore <player>:
 *   1 pick a death  ->  2 pick items (multi-select or Restore all)
 *   -> 3 pick who gets them (the dead player first)  ->  4 confirm.
 * Java gets chest GUIs; Bedrock gets Cumulus forms (the
 * ExcelBedrockMenu GeyserForms pattern). Every screen shows the
 * typed equivalent. The menu only builds a choice: the actual
 * restore is RestoreService.restore (all-or-nothing), the same as
 * the typed command, and permission is re-checked at Yes.
 */
final class RestoreMenu implements Listener {

    static final String PERM = "exceldeathrecord.restore";
    private static final int PAGE = 45;

    private enum Step { DEATHS, ITEMS, TARGET, CONFIRM }

    /** Per-staff state. */
    private static final class Session {
        UUID srcUuid;
        String srcName = "";
        List<RecordIndex.Entry> deaths = new ArrayList<>();
        int page = 1;
        int deathNumber = -1;             // 1-based in deaths
        RecordIndex.Entry death;
        TreeSet<Integer> selected = new TreeSet<>();
        UUID targetUuid;
        String targetName = "";
        boolean xp = false;
    }

    private static final class Holder implements InventoryHolder {
        final Step step;
        Inventory inv;
        Holder(Step step) { this.step = step; }
        @Override public Inventory getInventory() { return inv; }
    }

    private final ExcelDeathRecord plugin;
    private final RecordStore store;
    private final RestoreService service;
    private final ZoneId displayZone;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("MMM d h:mm a z", Locale.US);

    RestoreMenu(ExcelDeathRecord plugin, RecordStore store, RestoreService service, ZoneId displayZone) {
        this.plugin = plugin; this.store = store; this.service = service; this.displayZone = displayZone;
    }

    String when(long epochMillis) {
        return WHEN.format(Instant.ofEpochMilli(epochMillis).atZone(displayZone));
    }

    // ---------- entry ----------

    /** Open at step 1 (deathNumber <= 0) or straight at a death's items. */
    void open(Player staff, UUID srcUuid, List<RecordIndex.Entry> newestFirst, int deathNumber) {
        Session s = new Session();
        s.srcUuid = srcUuid;
        s.deaths = newestFirst;
        s.srcName = newestFirst.get(0).record.playerName;
        sessions.put(staff.getUniqueId(), s);
        if (deathNumber > 0 && deathNumber <= newestFirst.size()) pickDeath(staff, s, deathNumber);
        else showDeaths(staff, s);
    }

    private boolean bedrock(Player p) { return GeyserForms.isBedrockPlayer(p, plugin.getLogger()); }

    private static String deathLine(RecordIndex.Entry e) {
        DeathRecord r = e.record;
        String cause = r.damageCause.isEmpty() ? "unknown cause" : r.damageCause.toLowerCase(Locale.ROOT).replace('_', ' ');
        String killer = "none".equals(r.killer) ? "" : " by " + r.killer.replaceAll(" \\(.*\\)$", "");
        return cause + killer;
    }

    private static int restoredCount(List<DeathRecord.ItemEntry> list) {
        int n = 0;
        for (DeathRecord.ItemEntry e : list) if (e.isRestored()) n++;
        return n;
    }

    private String hint(Session s) {
        String items = s.selected.isEmpty() ? (s.death == null ? "" : "<items>")
                : Selection.format(new ArrayList<>(s.selected), RecordIndex.restorable(s.death.record).size());
        if (s.death == null) return "Typed: /deathrecord restore " + s.srcName + " <n> <items> [to <player>] (1 = newest death)";
        return RecordIndex.typedHint(s.srcName, s.deathNumber, items, s.targetName);
    }

    // ---------- step 1: deaths ----------

    private void showDeaths(Player staff, Session s) {
        s.death = null; s.deathNumber = -1; s.selected.clear(); s.targetUuid = null; s.targetName = ""; s.xp = false;
        if (bedrock(staff)) {
            int show = Math.min(30, s.deaths.size());
            String[] buttons = new String[show];
            for (int i = 0; i < show; i++) {
                RecordIndex.Entry e = s.deaths.get(i);
                List<DeathRecord.ItemEntry> list = RecordIndex.restorable(e.record);
                int rc = restoredCount(list);
                buttons[i] = "#" + (i + 1) + " " + when(e.record.epochMillis) + "\n" + deathLine(e) + ", " + list.size() + " items"
                        + (rc > 0 ? " (" + rc + " restored)" : "");
            }
            String content = hint(s) + "\n\nPick a death (newest first)." + (s.deaths.size() > show ? " Older ones: type the number." : "");
            if (!GeyserForms.sendSimpleForm(staff, "Restore: " + s.srcName, content, buttons,
                    id -> { if (id >= 0 && id < show) pickDeath(staff, s, id + 1); }, plugin.getLogger())) {
                staff.sendMessage("Could not open the Bedrock form. " + hint(s));
            }
            return;
        }
        Holder h = new Holder(Step.DEATHS);
        Inventory inv = Bukkit.createInventory(h, 54, Component.text("Restore: " + s.srcName + " - pick a death"));
        h.inv = inv;
        List<RecordIndex.Entry> page = RecordIndex.page(s.deaths, s.page, PAGE);
        for (int i = 0; i < page.size(); i++) {
            int number = (s.page - 1) * PAGE + i + 1;
            RecordIndex.Entry e = page.get(i);
            List<DeathRecord.ItemEntry> list = RecordIndex.restorable(e.record);
            int rc = restoredCount(list);
            List<String> lore = new ArrayList<>();
            lore.add(deathLine(e));
            lore.add(list.size() + " items" + (rc > 0 ? ", " + rc + " already restored" : ""));
            lore.add("Record " + e.fileName.replace(".yml", ""));
            lore.add("Click to see the items");
            inv.setItem(i, icon(Material.SKELETON_SKULL, "#" + number + "  " + when(e.record.epochMillis), lore, false));
        }
        int pages = RecordIndex.pageCount(s.deaths.size(), PAGE);
        if (s.page > 1) inv.setItem(45, icon(Material.ARROW, "Newer deaths", List.of("Page " + (s.page - 1)), false));
        inv.setItem(49, icon(Material.PAPER, "How to type it", List.of(hint(s)), false));
        inv.setItem(52, icon(Material.BARRIER, "Close", List.of(), false));
        if (s.page < pages) inv.setItem(53, icon(Material.ARROW, "Older deaths", List.of("Page " + (s.page + 1)), false));
        staff.openInventory(inv);
    }

    private void pickDeath(Player staff, Session s, int number) {
        s.deathNumber = number;
        s.death = reload(s, s.deaths.get(number - 1));
        s.selected.clear();
        showItems(staff, s);
    }

    /** Fresh copy from disk, so restore marks made meanwhile show. */
    private RecordIndex.Entry reload(Session s, RecordIndex.Entry e) {
        try {
            return new RecordIndex.Entry(e.fileName, store.readDeathFile(store.deathFile(s.srcUuid, e.fileName)));
        } catch (Exception ex) {
            return e;
        }
    }

    // ---------- step 2: items ----------

    private void showItems(Player staff, Session s) {
        s.death = reload(s, s.death);
        List<DeathRecord.ItemEntry> list = RecordIndex.restorable(s.death.record);
        if (bedrock(staff)) {
            StringBuilder content = new StringBuilder(hint(s)).append("\n\n")
                    .append(when(s.death.record.epochMillis)).append(", ").append(deathLine(s.death)).append("\n");
            if (RecordIndex.restorableIsCarried(s.death.record)) content.append("Nothing dropped on this death (kept inventory). Restore needs the typed command with force.\n");
            for (int i = 0; i < list.size(); i++) content.append("#").append(i + 1).append(" ").append(RestoreService.pretty(list.get(i)))
                    .append(list.get(i).isRestored() ? " (restored to " + list.get(i).restoredTo + ")" : "").append("\n");
            int open = list.size() - restoredCount(list);
            String[] buttons = { "Restore all (" + open + ")", "Pick items", "Back" };
            GeyserForms.sendSimpleForm(staff, "Death #" + s.deathNumber + " items", content.toString(), buttons, id -> {
                if (id == 0) { selectAllOpen(s); if (s.selected.isEmpty()) { staff.sendMessage("Every item on this death is already restored."); showItems(staff, s); } else showTargets(staff, s); }
                else if (id == 1) pickItemsBedrock(staff, s);
                else if (id == 2) showDeaths(staff, s);
            }, plugin.getLogger());
            return;
        }
        Holder h = new Holder(Step.ITEMS);
        Inventory inv = Bukkit.createInventory(h, 54, Component.text("Death #" + s.deathNumber + " - tap items to select"));
        h.inv = inv;
        for (int i = 0; i < Math.min(PAGE, list.size()); i++) {
            DeathRecord.ItemEntry e = list.get(i);
            ItemStack shown = RestoreService.rebuild(e);
            if (shown == null) shown = new ItemStack(Material.PAPER);
            shown = shown.clone();
            ItemMeta m = shown.getItemMeta();
            List<Component> lore = new ArrayList<>();
            if (m != null && m.lore() != null) lore.addAll(m.lore());
            lore.add(plain("Item #" + (i + 1), NamedTextColor.GRAY));
            if (e.isRestored()) {
                lore.add(plain("Already restored to " + e.restoredTo, NamedTextColor.RED));
                lore.add(plain(e.restoredAt + (e.restoredBy.isEmpty() ? "" : " by " + e.restoredBy), NamedTextColor.RED));
                lore.add(plain("Can't pick it again here (typed: add force)", NamedTextColor.DARK_GRAY));
            } else if (s.selected.contains(i + 1)) {
                lore.add(plain("Selected", NamedTextColor.GREEN));
                lore.add(plain("Click to unselect", NamedTextColor.GRAY));
                if (m != null) m.setEnchantmentGlintOverride(true);
            } else {
                lore.add(plain("Click to select", NamedTextColor.YELLOW));
            }
            if (m != null) { m.lore(lore); shown.setItemMeta(m); }
            inv.setItem(i, shown);
        }
        int open = list.size() - restoredCount(list);
        inv.setItem(45, icon(Material.ARROW, "Back to deaths", List.of(), false));
        inv.setItem(47, icon(Material.CHEST, "Restore all (" + open + ")", List.of("Every item not yet restored"), false));
        inv.setItem(49, icon(Material.PAPER, "How to type it", List.of(hint(s)), false));
        inv.setItem(51, icon(s.selected.isEmpty() ? Material.GRAY_DYE : Material.LIME_DYE, "Give selected (" + s.selected.size() + ")",
                List.of(s.selected.isEmpty() ? "Tap items above first" : "Next: pick who gets them"), false));
        inv.setItem(53, icon(Material.BARRIER, "Close", List.of(), false));
        staff.openInventory(inv);
    }

    private void selectAllOpen(Session s) {
        s.selected.clear();
        List<DeathRecord.ItemEntry> list = RecordIndex.restorable(s.death.record);
        for (int i = 0; i < list.size(); i++) if (!list.get(i).isRestored()) s.selected.add(i + 1);
    }

    private void pickItemsBedrock(Player staff, Session s) {
        List<DeathRecord.ItemEntry> list = RecordIndex.restorable(s.death.record);
        List<String> toggles = new ArrayList<>();
        List<Integer> map = new ArrayList<>();
        StringBuilder restored = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            DeathRecord.ItemEntry e = list.get(i);
            if (e.isRestored()) { restored.append("#").append(i + 1).append(" ").append(RestoreService.pretty(e)).append(" - restored to ").append(e.restoredTo).append("\n"); continue; }
            toggles.add("#" + (i + 1) + " " + RestoreService.pretty(e));
            map.add(i + 1);
        }
        if (toggles.isEmpty()) { staff.sendMessage("Every item on this death is already restored."); return; }
        String content = hint(s) + "\nTurn on the items to give, then Submit." + (restored.length() > 0 ? "\n\nAlready restored:\n" + restored : "");
        GeyserForms.sendToggleForm(staff, "Pick items", content, toggles, vals -> {
            s.selected.clear();
            for (int i = 0; i < vals.size() && i < map.size(); i++) if (vals.get(i)) s.selected.add(map.get(i));
            if (s.selected.isEmpty()) { staff.sendMessage("No items picked."); showItems(staff, s); return; }
            showTargets(staff, s);
        }, plugin.getLogger());
    }

    // ---------- step 3: who gets it ----------

    private List<Player> targetChoices(Session s) {
        List<Player> out = new ArrayList<>();
        Player dead = Bukkit.getPlayer(s.srcUuid);
        if (dead != null) out.add(dead);
        for (Player p : Bukkit.getOnlinePlayers()) if (dead == null || !p.getUniqueId().equals(dead.getUniqueId())) out.add(p);
        return out;
    }

    private void showTargets(Player staff, Session s) {
        List<Player> choices = targetChoices(s);
        boolean deadOnline = Bukkit.getPlayer(s.srcUuid) != null;
        if (bedrock(staff)) {
            int show = Math.min(40, choices.size());
            String[] buttons = new String[show + 1];
            for (int i = 0; i < show; i++) buttons[i] = choices.get(i).getName() + (i == 0 && deadOnline ? " (default, died)" : "");
            buttons[show] = "Back";
            String content = hint(s) + "\n\nWho gets " + s.selected.size() + " item(s)?" + (deadOnline ? "" : "\n" + s.srcName + " is offline.");
            GeyserForms.sendSimpleForm(staff, "Give to", content, buttons, id -> {
                if (id >= 0 && id < show) { pickTarget(staff, s, choices.get(id)); }
                else if (id == show) showItems(staff, s);
            }, plugin.getLogger());
            return;
        }
        Holder h = new Holder(Step.TARGET);
        Inventory inv = Bukkit.createInventory(h, 54, Component.text("Who gets " + s.selected.size() + " item(s)?"));
        h.inv = inv;
        int slot = 0;
        if (!deadOnline) inv.setItem(slot++, icon(Material.BARRIER, s.srcName + " is offline", List.of("Pick someone online, or wait for them"), false));
        for (int i = 0; i < choices.size() && slot < PAGE; i++, slot++) {
            Player p = choices.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) head.getItemMeta();
            sm.setOwningPlayer(p);
            sm.displayName(plain(p.getName() + (i == 0 && deadOnline ? " (default)" : ""), NamedTextColor.YELLOW));
            sm.lore(List.of(plain(i == 0 && deadOnline ? "The player who died" : "Click to give to " + p.getName(), NamedTextColor.GRAY)));
            head.setItemMeta(sm);
            inv.setItem(slot, head);
        }
        inv.setItem(45, icon(Material.ARROW, "Back to items", List.of(), false));
        inv.setItem(49, icon(Material.PAPER, "How to type it", List.of(hint(s)), false));
        inv.setItem(53, icon(Material.BARRIER, "Close", List.of(), false));
        staff.openInventory(inv);
    }

    private void pickTarget(Player staff, Session s, Player target) {
        s.targetUuid = target.getUniqueId();
        s.targetName = target.getName();
        s.xp = false;
        showConfirm(staff, s);
    }

    // ---------- step 4: confirm ----------

    private RestoreService.Source source(Session s) {
        RestoreService.Source src = new RestoreService.Source();
        src.burned = false;
        src.file = store.deathFile(s.srcUuid, s.death.fileName);
        src.sourceName = s.srcName;
        src.sourceUuid = s.srcUuid;
        return src;
    }

    private void showConfirm(Player staff, Session s) {
        Player target = Bukkit.getPlayer(s.targetUuid);
        if (target == null) { staff.sendMessage(s.targetName + " went offline."); showTargets(staff, s); return; }
        RestoreService.Prepared p = service.prepare(source(s), null, new ArrayList<>(s.selected), target, false, s.xp);
        if (!p.ok()) { staff.sendMessage(p.error); showItems(staff, s); return; }
        List<DeathRecord.ItemEntry> list = p.list;
        String what = p.numbers.size() == 1 ? RestoreService.pretty(list.get(p.numbers.get(0) - 1)) : p.numbers.size() + " items";
        String question = "Give " + what + " to " + target.getName() + "?";
        List<String> lines = new ArrayList<>();
        for (int n : p.numbers) lines.add("#" + n + " " + RestoreService.pretty(list.get(n - 1)));
        String slots = SlotPlanner.summary(target.getName(), p.plan);
        if (bedrock(staff)) {
            StringBuilder c = new StringBuilder(question).append("\n\n");
            for (int i = 0; i < Math.min(25, lines.size()); i++) c.append(lines.get(i)).append("\n");
            if (lines.size() > 25) c.append("... and ").append(lines.size() - 25).append(" more\n");
            c.append("\n").append(slots).append("\n\n").append(hint(s));
            List<String> b = new ArrayList<>();
            b.add("Yes, give");
            if (p.xpOffered) b.add("Yes, give + XP (" + p.xpPoints + " points)");
            b.add("Cancel");
            String[] buttons = b.toArray(new String[0]);
            GeyserForms.sendSimpleForm(staff, "Confirm restore", c.toString(), buttons, id -> {
                if (id == 0) { s.xp = false; doRestore(staff, s); }
                else if (p.xpOffered && id == 1) { s.xp = true; doRestore(staff, s); }
                else staff.sendMessage("Cancelled. Nothing was given.");
            }, plugin.getLogger());
            return;
        }
        Holder h = new Holder(Step.CONFIRM);
        Inventory inv = Bukkit.createInventory(h, 27, Component.text("Confirm restore"));
        h.inv = inv;
        List<String> lore = new ArrayList<>(lines.subList(0, Math.min(20, lines.size())));
        if (lines.size() > 20) lore.add("... and " + (lines.size() - 20) + " more");
        lore.add("");
        lore.add(slots);
        inv.setItem(4, icon(Material.PAPER, "How to type it", List.of(hint(s)), false));
        inv.setItem(13, icon(Material.CHEST, question, lore, false));
        inv.setItem(11, p.plan.fits()
                ? icon(Material.LIME_WOOL, "Yes, give", List.of(slots), false)
                : icon(Material.ORANGE_WOOL, "Won't fit yet", List.of(SlotPlanner.refusal(target.getName(), p.plan), "Click to check again"), false));
        inv.setItem(15, icon(Material.RED_WOOL, "Cancel", List.of("Nothing is given"), false));
        if (p.xpOffered) {
            inv.setItem(22, icon(Material.EXPERIENCE_BOTTLE, "Also restore XP: " + (s.xp ? "ON" : "OFF"),
                    List.of(p.xpPoints + " XP points (level " + p.death.xpLevel + " at death)", "Click to switch"), s.xp));
        }
        inv.setItem(18, icon(Material.ARROW, "Back to who gets it", List.of(), false));
        staff.openInventory(inv);
    }

    private void doRestore(Player staff, Session s) {
        if (!staff.hasPermission(PERM)) { staff.sendMessage("ExcelDeathRecord: restore is for ops."); return; }
        Player target = Bukkit.getPlayer(s.targetUuid);
        if (target == null) { staff.sendMessage(s.targetName + " went offline. Nothing was given."); return; }
        List<String> out = service.restore(staff.getName(), source(s), null, new ArrayList<>(s.selected), target, false, s.xp);
        for (String line : out) staff.sendMessage(line);
        staff.sendMessage(hint(s) + (s.xp ? " xp" : ""));
        sessions.remove(staff.getUniqueId());
    }

    // ---------- Java clicks ----------

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder h)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player staff)) return;
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) return;
        Session s = sessions.get(staff.getUniqueId());
        if (s == null || !staff.hasPermission(PERM)) { staff.closeInventory(); return; }
        int slot = event.getRawSlot();
        Bukkit.getScheduler().runTask(plugin, () -> handle(staff, s, h.step, slot));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { sessions.remove(event.getPlayer().getUniqueId()); }

    private void handle(Player staff, Session s, Step step, int slot) {
        switch (step) {
            case DEATHS -> {
                if (slot < PAGE) {
                    int number = (s.page - 1) * PAGE + slot + 1;
                    if (number <= s.deaths.size()) pickDeath(staff, s, number);
                } else if (slot == 45 && s.page > 1) { s.page--; showDeaths(staff, s); }
                else if (slot == 53 && s.page < RecordIndex.pageCount(s.deaths.size(), PAGE)) { s.page++; showDeaths(staff, s); }
                else if (slot == 52) staff.closeInventory();
            }
            case ITEMS -> {
                List<DeathRecord.ItemEntry> list = RecordIndex.restorable(s.death.record);
                if (slot < PAGE) {
                    if (slot >= list.size()) return;
                    if (list.get(slot).isRestored()) { staff.sendMessage("Item #" + (slot + 1) + " was already restored" + RecordIndex.restoredNote(list.get(slot)) + "."); return; }
                    if (!s.selected.remove(slot + 1)) s.selected.add(slot + 1);
                    showItems(staff, s);
                } else if (slot == 45) showDeaths(staff, s);
                else if (slot == 47) {
                    selectAllOpen(s);
                    if (s.selected.isEmpty()) staff.sendMessage("Every item on this death is already restored.");
                    else showTargets(staff, s);
                } else if (slot == 51) {
                    if (s.selected.isEmpty()) staff.sendMessage("Tap items to select them first, or use Restore all.");
                    else showTargets(staff, s);
                } else if (slot == 53) staff.closeInventory();
            }
            case TARGET -> {
                if (slot == 45) { showItems(staff, s); return; }
                if (slot == 53) { staff.closeInventory(); return; }
                if (slot >= PAGE) return;
                boolean deadOnline = Bukkit.getPlayer(s.srcUuid) != null;
                int idx = deadOnline ? slot : slot - 1;
                List<Player> choices = targetChoices(s);
                if (idx >= 0 && idx < choices.size()) pickTarget(staff, s, choices.get(idx));
            }
            case CONFIRM -> {
                if (slot == 11) {
                    Player target = Bukkit.getPlayer(s.targetUuid);
                    RestoreService.Prepared p = target == null ? null : service.prepare(source(s), null, new ArrayList<>(s.selected), target, false, s.xp);
                    if (p != null && p.ok() && !p.plan.fits()) {
                        staff.sendMessage(SlotPlanner.refusal(target.getName(), p.plan));
                        showConfirm(staff, s);
                        return;
                    }
                    staff.closeInventory();
                    doRestore(staff, s);
                } else if (slot == 15) { staff.closeInventory(); staff.sendMessage("Cancelled. Nothing was given."); }
                else if (slot == 22) { s.xp = !s.xp; showConfirm(staff, s); }
                else if (slot == 18) showTargets(staff, s);
            }
        }
    }

    // ---------- helpers ----------

    private static Component plain(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private static ItemStack icon(Material mat, String name, List<String> lore, boolean glow) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.displayName(plain(name, NamedTextColor.YELLOW));
            List<Component> l = new ArrayList<>();
            for (String s : lore) l.add(plain(s, NamedTextColor.GRAY));
            m.lore(l);
            if (glow) m.setEnchantmentGlintOverride(true);
            it.setItemMeta(m);
        }
        return it;
    }
}
