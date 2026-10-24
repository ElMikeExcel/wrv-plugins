package com.winkreyes.exceldeathrecord;

import com.winkreyes.exceldeathrecord.core.BurnIndex;
import com.winkreyes.exceldeathrecord.core.DestructionRecord;
import com.winkreyes.exceldeathrecord.core.LostFoundRules;
import com.winkreyes.exceldeathrecord.core.LostFoundState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 0.3.0 /lostfound: the player screens. Java gets chest menus,
 * Bedrock gets Geyser forms (the same reflection helpers the restore
 * menu uses). Every open shows the first-use tip until the player
 * taps OK. The screens only build a choice; the confirm itself is
 * LostFoundService.confirm.
 */
final class LostFoundMenu implements Listener, CommandExecutor {

    private enum Step { TIP, LIST, CONFIRM }

    private static final class Holder implements InventoryHolder {
        final Step step;
        Inventory inv;
        List<BurnIndex.Entry> entries = new ArrayList<>();
        long offerId = -1;
        Holder(Step step) { this.step = step; }
        @Override public Inventory getInventory() { return inv; }
    }

    static final String TITLE = "Lost & Found";
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("MMM d h:mm a z", Locale.US);

    private final ExcelDeathRecord plugin;
    private final LostFoundService service;
    private final ZoneId zone;
    final TipsBridge tips;

    LostFoundMenu(ExcelDeathRecord plugin, LostFoundService service, ZoneId zone) {
        this.plugin = plugin;
        this.service = service;
        this.zone = zone;
        this.tips = new TipsBridge(plugin);
    }

    // ---- ExcelTips hand-over (optional) ----

    /** Called once everything has loaded, and again whenever ExcelTips (re)enables. */
    void hookTips() {
        tips.hook(List.of(tipText()), List.of(tipText()), (id, source) -> service.tipOk(id));
    }

    @EventHandler
    public void onPluginEnable(org.bukkit.event.server.PluginEnableEvent event) {
        if (event.getPlugin().getName().equals("ExcelTips")) Bukkit.getScheduler().runTask(plugin, this::hookTips);
    }

    @EventHandler
    public void onPluginDisable(org.bukkit.event.server.PluginDisableEvent event) {
        if (event.getPlugin().getName().equals("ExcelTips")) tips.unhook();
    }

    String tipText() {
        LostFoundRules.Settings s = service.settings;
        return "Lost something to lava, fire, the void, or despawning? If it was yours, it shows here for "
                + LostFoundRules.daysText(s.windowMillis) + ". "
                + (s.freeEnabled ? "Your first one is free if you have less than " + LostFoundRules.money(s.freeThreshold) + ". " : "")
                + "Each one costs " + LostFoundRules.money(s.price) + ", and you can bring back one item every "
                + LostFoundRules.durationText(s.cooldownMillis) + ". "
                + "Death Protection from the Spawn Shop keeps your whole kit safe, so it's the better deal.";
    }

    // ---- command ----

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Lost & Found is for players in the game."); return true; }
        if (!p.hasPermission(LostFoundService.PERM)) { p.sendMessage("\u00a7cLost & Found isn't available for you."); return true; }
        if (!service.enabled) { p.sendMessage("\u00a7eLost & Found is closed right now."); return true; }
        if (args.length >= 1 && args[0].equalsIgnoreCase("ok")) {
            service.tipOk(p.getUniqueId());
            if (tips.active()) tips.acknowledge(p);
            p.sendMessage("\u00a7aGot it! The Lost & Found tip won't show again.");
            return true;
        }
        open(p);
        return true;
    }

    void open(Player p) {
        LostFoundState st = service.state(p.getUniqueId());
        if (st == null) { p.sendMessage("\u00a7cLost & Found couldn't load your info. Ask an adult to check the server."); return; }
        if (tips.active()) {
            // ExcelTips shows the tip (chat line with [OK]; Bedrock gets its OK
            // pop-up when the player is free). One tip, never two.
            if (st.tipOk) tips.acknowledge(p);
            else if (tips.isAcknowledged(p.getUniqueId())) service.tipOk(p.getUniqueId());
            else tips.tip(p);
            showList(p);
            return;
        }
        if (!st.tipOk) showTip(p); else showList(p);
    }

    private boolean bedrock(Player p) { return GeyserForms.isBedrockPlayer(p, plugin.getLogger()); }

    // ---- tip ----

    private void showTip(Player p) {
        if (bedrock(p)) {
            boolean sent = GeyserForms.sendModalForm(p, TITLE + " tip", tipText(), "OK, got it!", "Remind me later",
                    ok -> { if (ok) { service.tipOk(p.getUniqueId()); p.sendMessage("\u00a7aGot it! The Lost & Found tip won't show again."); } showList(p); },
                    plugin.getLogger());
            if (sent) return;
            p.sendMessage("\u00a7e[Tip] " + tipText() + " \u00a77Type /lostfound ok to hide this tip.");
            showList(p);
            return;
        }
        Holder h = new Holder(Step.TIP);
        Inventory inv = Bukkit.createInventory(h, 27, Component.text(TITLE + " tip"));
        h.inv = inv;
        inv.setItem(13, icon(Material.BOOK, "How Lost & Found works", NamedTextColor.GOLD, wrap(tipText(), 38)));
        inv.setItem(11, icon(Material.LIME_CONCRETE, "OK, got it!", NamedTextColor.GREEN, List.of("Don't show this tip again")));
        inv.setItem(15, icon(Material.GRAY_CONCRETE, "Remind me later", NamedTextColor.GRAY, List.of("Show this tip next time too")));
        p.openInventory(inv);
    }

    // ---- list ----

    void showList(Player p) {
        LostFoundState st = service.state(p.getUniqueId());
        if (st == null) { p.sendMessage("\u00a7cLost & Found couldn't load your info."); return; }
        List<BurnIndex.Entry> entries = service.eligible(p);
        boolean moneyOk = service.money().ready();
        LostFoundRules.Price price = service.quote(p, st);
        long cd = service.cooldownLeft(st);
        List<String> info = new ArrayList<>();
        info.add("Price: " + LostFoundRules.priceText(price));
        if (moneyOk) info.add("Your money: " + LostFoundRules.money(service.money().balance(p)));
        else info.add("The money system is offline right now.");
        if (cd > 0) info.add("Next recovery in " + LostFoundRules.durationText(cd));
        info.add("Items stay here for " + LostFoundRules.daysText(service.settings.windowMillis) + ".");

        if (bedrock(p)) {
            List<String> buttons = new ArrayList<>();
            for (BurnIndex.Entry e : entries) buttons.add(buttonText(e.record));
            buttons.add("Close");
            String content = String.join("\n", info) + "\n\n" + (entries.isEmpty()
                    ? "Nothing to bring back. Only your own items lost to lava, fire, the void, or despawning in the last "
                      + LostFoundRules.daysText(service.settings.windowMillis) + " show here."
                    : "Tap an item to bring it back.");
            final List<BurnIndex.Entry> snap = entries;
            boolean sent = GeyserForms.sendSimpleForm(p, TITLE, content, buttons.toArray(new String[0]), id -> {
                if (id >= 0 && id < snap.size()) pick(p, snap.get(id));
            }, plugin.getLogger());
            if (!sent) p.sendMessage("\u00a7cThe Lost & Found screen couldn't open. Ask an adult to check the server.");
            return;
        }
        Holder h = new Holder(Step.LIST);
        h.entries = entries;
        Inventory inv = Bukkit.createInventory(h, 54, Component.text(TITLE));
        h.inv = inv;
        for (int i = 0; i < entries.size() && i < 45; i++) inv.setItem(i, entryIcon(entries.get(i).record, "Click to bring it back"));
        if (entries.isEmpty()) {
            inv.setItem(22, icon(Material.BARRIER, "Nothing to bring back", NamedTextColor.GRAY,
                    wrap("Only your own items lost to lava, fire, the void, or despawning in the last "
                            + LostFoundRules.daysText(service.settings.windowMillis) + " show here.", 38)));
        }
        inv.setItem(49, icon(price.free ? Material.EMERALD : Material.GOLD_INGOT, "Price: " + LostFoundRules.priceText(price),
                NamedTextColor.GOLD, info.subList(1, info.size())));
        inv.setItem(53, icon(Material.OAK_DOOR, "Close", NamedTextColor.RED, List.of()));
        p.openInventory(inv);
    }

    private void pick(Player p, BurnIndex.Entry e) {
        LostFoundState st = service.state(p.getUniqueId());
        if (st == null) return;
        long cd = service.cooldownLeft(st);
        if (cd > 0) {
            p.sendMessage("\u00a7eYou can bring back one item every " + LostFoundRules.durationText(service.settings.cooldownMillis)
                    + ". Your next one is ready in " + LostFoundRules.durationText(cd) + ".");
            return;
        }
        if (!service.money().ready()) { p.sendMessage("\u00a7eThe money system is offline right now, so Lost & Found can't run."); return; }
        LostFoundRules.Price price = service.quote(p, st);
        showConfirm(p, service.offer(p, e, price));
    }

    // ---- confirm ----

    private void showConfirm(Player p, LostFoundService.Offer o) {
        String item = RestoreService.pretty(o.record.item);
        String ptext = LostFoundRules.priceText(o.price);
        if (bedrock(p)) {
            String content = "Bring back your " + item + "?\n" + LostFoundRules.causeWords(o.record.cause) + " " + when(o.record)
                    + "\n\nPrice: " + ptext;
            boolean sent = GeyserForms.sendModalForm(p, TITLE, content, "Bring it back (" + ptext + ")", "Cancel",
                    yes -> { if (yes) doConfirm(p, o.id); else service.dropOffer(p); }, plugin.getLogger());
            if (!sent) p.sendMessage("\u00a7cThe Lost & Found screen couldn't open.");
            return;
        }
        Holder h = new Holder(Step.CONFIRM);
        h.offerId = o.id;
        Inventory inv = Bukkit.createInventory(h, 27, Component.text("Bring it back?"));
        h.inv = inv;
        inv.setItem(13, entryIcon(o.record, null));
        inv.setItem(11, icon(Material.LIME_CONCRETE, "Bring it back: " + ptext, NamedTextColor.GREEN, List.of(item)));
        inv.setItem(15, icon(Material.RED_CONCRETE, "Cancel", NamedTextColor.RED, List.of("Nothing is charged")));
        p.openInventory(inv);
    }

    private void doConfirm(Player p, long offerId) {
        LostFoundService.Result r = service.confirm(p, offerId);
        if (r.silent) return;
        for (String line : r.lines) p.sendMessage((r.ok ? "\u00a7a" : "\u00a7e") + line);
        if (r.newOffer != null) showConfirm(p, r.newOffer);
    }

    // ---- clicks ----

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder h)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) return;
        int slot = event.getRawSlot();
        Bukkit.getScheduler().runTask(plugin, () -> handle(p, h, slot));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { service.dropOffer(event.getPlayer()); }

    private void handle(Player p, Holder h, int slot) {
        switch (h.step) {
            case TIP -> {
                if (slot == 11) { service.tipOk(p.getUniqueId()); p.sendMessage("\u00a7aGot it! The Lost & Found tip won't show again."); showList(p); }
                else if (slot == 15) showList(p);
            }
            case LIST -> {
                if (slot >= 0 && slot < 45 && slot < h.entries.size()) pick(p, h.entries.get(slot));
                else if (slot == 53) p.closeInventory();
            }
            case CONFIRM -> {
                if (slot == 11) { p.closeInventory(); doConfirm(p, h.offerId); }
                else if (slot == 15) { service.dropOffer(p); showList(p); }
            }
        }
    }

    // ---- icons ----

    private String when(DestructionRecord r) {
        return WHEN.format(Instant.ofEpochMilli(r.epochMillis).atZone(zone));
    }

    private String buttonText(DestructionRecord r) {
        return RestoreService.pretty(r.item) + "\n" + LostFoundRules.causeWords(r.cause) + ", " + when(r);
    }

    private ItemStack entryIcon(DestructionRecord r, String action) {
        ItemStack s = RestoreService.rebuild(r.item);
        if (s == null || s.getType().isAir()) s = new ItemStack(Material.PAPER);
        s = s.clone();
        s.setAmount(Math.max(1, Math.min(s.getAmount(), s.getMaxStackSize())));
        ItemMeta m = s.getItemMeta();
        if (m != null) {
            List<Component> lore = new ArrayList<>();
            List<Component> old = m.lore();
            if (old != null) lore.addAll(old);
            lore.add(line("Lost " + when(r), NamedTextColor.GRAY));
            lore.add(line("How: " + LostFoundRules.causeWords(r.cause), NamedTextColor.GRAY));
            lore.add(line("Where: " + (int) Math.floor(r.x) + ", " + (int) Math.floor(r.y) + ", " + (int) Math.floor(r.z)
                    + " (" + worldName(r.world) + ")", NamedTextColor.GRAY));
            if (action != null) lore.add(line(action, NamedTextColor.YELLOW));
            m.lore(lore);
            s.setItemMeta(m);
        }
        return s;
    }

    private static String worldName(String key) {
        return switch (key) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "End";
            default -> key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
        };
    }

    private static ItemStack icon(Material mat, String name, NamedTextColor color, List<String> lore) {
        ItemStack s = new ItemStack(mat);
        ItemMeta m = s.getItemMeta();
        m.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
        List<Component> l = new ArrayList<>();
        for (String x : lore) l.add(line(x, NamedTextColor.GRAY));
        m.lore(l);
        s.setItemMeta(m);
        return s;
    }

    private static Component line(String s, NamedTextColor c) {
        return Component.text(s, c).decoration(TextDecoration.ITALIC, false);
    }

    static List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : text.split(" ")) {
            if (cur.length() > 0 && cur.length() + 1 + w.length() > width) { out.add(cur.toString()); cur.setLength(0); }
            if (cur.length() > 0) cur.append(' ');
            cur.append(w);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }
}
