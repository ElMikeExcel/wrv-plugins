package com.winkreyes.exceldowned;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Revive Potion confirm UI + prompt + interact (ExcelDowned
 * 1.0.0, spec A sections 3–5) — the GiveUpUi pattern carried to the
 * carrier side: one state (a ticket bound to carrier + target),
 * three doors chosen by GiveUpRoute — Java inventory menu, Bedrock
 * Cumulus ModalForm via BedrockForms, chat arm/confirm fallback
 * (S-5: the typed path never breaks).
 *
 * Discipline (spec 3.3–3.5): one open UI per carrier; opening a new
 * one replaces the old; every answer re-validates through
 * RevivePotionPolicy at click time; the potion is consumed ONLY
 * after the revive lands (shouldConsume); a successful use stamps
 * the carrier's 10-minute potion cooldown (Mike, Nov 17, 2026 —
 * config revive-potion.use-cooldown-seconds; SEPARATE from the
 * Cleric ability cooldown). Every session exit invalidates UIs
 * bound to the target (ExcelDowned calls invalidateForTarget
 * wherever it calls GiveUpUi.invalidate).
 *
 * This class also owns the drink-block (spec 2: the potion is
 * passive, never drunk) and the 1-second prompt rendering, driven
 * from ExcelDowned's existing state task.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class RevivePotionUi implements Listener {

    static final int SLOT_CONFIRM = 2;
    static final int SLOT_PICTURE = 4;
    static final int SLOT_BACK = 6;
    private static final long CHAT_ARM_MS = 10_000L;

    /** One presented UI: the carrier, the target, and its arm time. */
    private static final class Ticket {
        final UUID carrierId;
        final UUID targetId;
        final long armedAtMs;
        Ticket(UUID carrierId, UUID targetId, long armedAtMs) {
            this.carrierId = carrierId;
            this.targetId = targetId;
            this.armedAtMs = armedAtMs;
        }
    }

    private final ExcelDowned plugin;
    private final Map<UUID, Ticket> tickets = new ConcurrentHashMap<>();
    private final Set<UUID> javaOpen = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastUseMs = new ConcurrentHashMap<>();
    private final Map<String, Long> lastHintMs = new ConcurrentHashMap<>();

    public RevivePotionUi(ExcelDowned plugin) { this.plugin = plugin; }

    // ---------- cooldown (per carrier, success-only) ----------

    public boolean onCooldown(UUID carrierId, long nowMs) {
        Long last = lastUseMs.get(carrierId);
        return last != null && RevivePotionPolicy.cooldownActive(
                nowMs, last, plugin.revivePotionUseCooldownMs);
    }

    public long cooldownRemainingMs(UUID carrierId, long nowMs) {
        Long last = lastUseMs.get(carrierId);
        if (last == null) return 0;
        return Math.max(0, plugin.revivePotionUseCooldownMs - (nowMs - last));
    }

    // ---------- targeting helpers ----------

    private static double distSq(Player a, Player b) {
        return a.getLocation().distanceSquared(b.getLocation());
    }

    // ---------- the prompt tick (driven by ExcelDowned's 1 s task) ----------

    public void promptTick() {
        if (!plugin.revivePotionEnabled || !plugin.revivePotionPromptEnabled) return;
        long now = System.currentTimeMillis();
        for (Player carrier : Bukkit.getOnlinePlayers()) {
            if (plugin.isDowned(carrier.getUniqueId())) continue;
            if (tickets.containsKey(carrier.getUniqueId())) continue; // UI open: it speaks
            int count = RevivePotionItem.countPotions(plugin, carrier.getInventory());
            if (count < 1) continue;
            boolean cooling = onCooldown(carrier.getUniqueId(), now);
            Player best = null;
            double bestSq = Double.MAX_VALUE;
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (target == carrier) continue;
                if (!plugin.isDowned(target.getUniqueId())) continue;
                if (target.getWorld() != carrier.getWorld()) continue;
                double sq = distSq(carrier, target);
                if (!RevivePotionPolicy.promptEligible(true, false, false, true,
                        true, sq,
                        plugin.revivePotionPromptRange, count, cooling)) continue;
                if (sq < bestSq) { bestSq = sq; best = target; }
            }
            if (best == null) continue;
            DownedSession s = plugin.sessionOf(best.getUniqueId());
            String clock = s == null ? "??:??"
                    : ExcelDowned.formatClock(s.remainingMs(now));
            carrier.sendActionBar(ChatColor.GREEN + "✝ " + best.getName()
                    + " is DOWN (bleed out " + clock + ") — right-click them to revive"
                    + " · you hold " + count + " Revive Potion(s)");
        }
    }

    // ---------- the interact (spec 3.3) ----------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!plugin.revivePotionEnabled) return;
        if (event.getHand() != EquipmentSlot.HAND) return; // one event per click
        if (!(event.getRightClicked() instanceof Player)) return;
        Player carrier = event.getPlayer();
        Player target = (Player) event.getRightClicked();
        if (carrier == target) return;
        if (plugin.isDowned(carrier.getUniqueId())) return; // lockdown owns them
        if (!plugin.isDowned(target.getUniqueId())) return;
        if (target.getWorld() != carrier.getWorld()) return;
        // 1.0.2: no party gate on the interact (open targeting —
        // any carrier in range of any downed player may revive).
        if (!FixPolicy.withinReviveRange(true, distSq(carrier, target),
                plugin.revivePotionConfirmRange)) return;
        long now = System.currentTimeMillis();
        int count = RevivePotionItem.countPotions(plugin, carrier.getInventory());
        if (count < 1) {
            // zero-potion teaching hint, throttled per pair (spec 5.4)
            String pair = carrier.getUniqueId() + ">" + target.getUniqueId();
            Long last = lastHintMs.get(pair);
            if (RevivePotionPolicy.hintAllowed(now, last == null ? 0 : last,
                    plugin.revivePotionZeroHintCooldownMs)) {
                lastHintMs.put(pair, now);
                carrier.sendMessage(ChatColor.GOLD + "✝ You need a Revive Potion to revive "
                        + target.getName() + " — Excel Shop, $"
                        + String.format("%,d", plugin.revivePotionPrice) + ".");
            }
            return; // NOT cancelled: nothing was claimed
        }
        if (onCooldown(carrier.getUniqueId(), now)) {
            String pair = carrier.getUniqueId() + ">cooldown";
            Long last = lastHintMs.get(pair);
            if (RevivePotionPolicy.hintAllowed(now, last == null ? 0 : last,
                    plugin.revivePotionZeroHintCooldownMs)) {
                lastHintMs.put(pair, now);
                carrier.sendMessage(ChatColor.GOLD + "✝ Your Revive Potion is resting — "
                        + ExcelDowned.formatClock(cooldownRemainingMs(carrier.getUniqueId(), now))
                        + " left before you can use another.");
            }
            return;
        }
        event.setCancelled(true); // this click is spoken for
        openConfirm(carrier, target);
    }

    // ---------- the drink-block (spec 2: passive, never drunk) ----------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!plugin.revivePotionEnabled) return;
        if (!RevivePotionItem.isRevivePotion(plugin, event.getItem())) return;
        event.setCancelled(true);
        Player p = event.getPlayer();
        if (!plugin.isDowned(p.getUniqueId())) {
            p.sendMessage(ChatColor.GOLD + "✝ The Revive Potion isn't for drinking"
                    + " — carry it to a downed teammate.");
        }
    }

    // ---------- presentation (three doors, GiveUpUi shape) ----------

    /**
     * Present the confirm UI for (carrier, target). Replaces any UI
     * the carrier already has open (ticket invalidation). Returns
     * true when some door presented (menu, form, or chat arm).
     */
    public boolean openConfirm(Player carrier, Player target) {
        if (!plugin.isDowned(target.getUniqueId())) return false;
        Ticket ticket = new Ticket(carrier.getUniqueId(), target.getUniqueId(),
                System.currentTimeMillis());
        tickets.put(carrier.getUniqueId(), ticket);
        int count = RevivePotionItem.countPotions(plugin, carrier.getInventory());
        DownedSession s = plugin.sessionOf(target.getUniqueId());
        String clock = s == null ? "??:??"
                : ExcelDowned.formatClock(s.remainingMs(System.currentTimeMillis()));
        boolean bedrock = BedrockForms.isBedrockPlayer(carrier, plugin.getLogger());
        switch (GiveUpRoute.route(bedrock, BedrockForms.isFormApiAvailable())) {
            case BEDROCK_FORM: {
                String body = "Revive — use 1/" + count + " revive potions in your inventory\n\n"
                        + target.getName() + " bleeds out in " + clock + ".\n"
                        + "They come back at half health.\n"
                        + "The potion is used only if the revive lands.";
                boolean sent = BedrockForms.sendModalForm(carrier,
                        "✝ Revive " + target.getName() + "?", body,
                        "✚ Revive — use 1 of " + count, "Back",
                        first -> Bukkit.getScheduler().runTask(plugin,
                                () -> onFormAnswer(carrier, ticket, first)),
                        () -> Bukkit.getScheduler().runTask(plugin,
                                () -> onFormClosed(carrier, ticket)),
                        plugin.getLogger());
                if (sent) return true;
                tickets.remove(carrier.getUniqueId(), ticket);
                return armChatFallback(carrier, target, ticket);
            }
            case JAVA_MENU: {
                Inventory inv = buildMenu(carrier, target, count, clock);
                javaOpen.add(carrier.getUniqueId());
                carrier.openInventory(inv);
                return true;
            }
            default:
                return armChatFallback(carrier, target, ticket);
        }
    }

    private boolean armChatFallback(Player carrier, Player target, Ticket ticket) {
        // the ticket is already in the map; the typed confirm shares it
        carrier.sendMessage(ChatColor.GREEN + "✝ Type " + ChatColor.BOLD
                + "/downed revivepotion confirm" + ChatColor.RESET + ChatColor.GREEN
                + " within 10s to revive " + target.getName()
                + " — or click them again when the menu works.");
        return true;
    }

    private Inventory buildMenu(Player carrier, Player target, int count, String clock) {
        Map<Integer, String> actions = new LinkedHashMap<>();
        actions.put(SLOT_CONFIRM, RevivePotionHolder.ACTION_CONFIRM);
        actions.put(SLOT_BACK, RevivePotionHolder.ACTION_BACK);
        RevivePotionHolder holder = new RevivePotionHolder(actions);
        Inventory inv = Bukkit.createInventory(holder, 9,
                ChatColor.DARK_GREEN + "✝ Revive " + target.getName() + "?");
        holder.bind(inv);
        ItemStack filler = named(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, filler);
        inv.setItem(SLOT_CONFIRM, named(Material.GREEN_CONCRETE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "✚ REVIVE " + target.getName()
                        + " — use 1 potion",
                ChatColor.GRAY + "Revive — use 1/" + count + " revive potions in your inventory",
                ChatColor.GRAY + target.getName() + " bleeds out in " + clock + ".",
                ChatColor.GRAY + "They come back at half health.",
                ChatColor.GRAY + "The potion is used only if the revive lands."));
        ItemStack picture = RevivePotionItem.create(plugin);
        inv.setItem(SLOT_PICTURE, picture);
        inv.setItem(SLOT_BACK, named(Material.ARROW,
                ChatColor.RED + "" + ChatColor.BOLD + "Back",
                ChatColor.GRAY + "Close — nothing is spent."));
        return inv;
    }

    private static ItemStack named(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(Arrays.asList(lore));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    // ---------- Bedrock form answers ----------

    private void onFormAnswer(Player carrier, Ticket ticket, boolean revive) {
        if (tickets.get(carrier.getUniqueId()) != ticket) return; // stale form
        if (!carrier.isOnline()) { tickets.remove(carrier.getUniqueId(), ticket); return; }
        if (revive) confirm(carrier, ticket);
        else tickets.remove(carrier.getUniqueId(), ticket); // Back: nothing spent
    }

    private void onFormClosed(Player carrier, Ticket ticket) {
        if (tickets.get(carrier.getUniqueId()) != ticket) return;
        tickets.remove(carrier.getUniqueId(), ticket); // dismissed = Back
    }

    // ---------- Java menu events ----------

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RevivePotionHolder)) return;
        event.setCancelled(true); // a menu is not a container
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player carrier = (Player) event.getWhoClicked();
        if (event.getClickedInventory() != event.getInventory()) return;
        RevivePotionHolder holder = (RevivePotionHolder) event.getInventory().getHolder();
        String action = holder.actionAt(event.getSlot());
        if (action == null) return;
        Ticket ticket = tickets.get(carrier.getUniqueId());
        if (RevivePotionHolder.ACTION_CONFIRM.equals(action)) {
            if (ticket != null) confirm(carrier, ticket);
        } else if (RevivePotionHolder.ACTION_BACK.equals(action)) {
            tickets.remove(carrier.getUniqueId());
            closeJavaMenu(carrier); // Back: nothing spent
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof RevivePotionHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof RevivePotionHolder)) return;
        if (!(event.getPlayer() instanceof Player)) return;
        Player carrier = (Player) event.getPlayer();
        javaOpen.remove(carrier.getUniqueId());
        // Closing without confirming (ESC) is the Back path: the
        // ticket dies, nothing is spent. After a confirm the ticket
        // is already gone, so this is a no-op on that path.
        tickets.remove(carrier.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        invalidateCarrier(event.getPlayer().getUniqueId());
    }

    // ---------- the chat-fallback confirm (spec 7) ----------

    /** /downed revivepotion confirm — confirms the armed ticket. */
    public boolean confirmFromChat(Player carrier) {
        Ticket ticket = tickets.get(carrier.getUniqueId());
        if (ticket == null) return false;
        if (System.currentTimeMillis() - ticket.armedAtMs > CHAT_ARM_MS
                && !javaOpen.contains(carrier.getUniqueId())) {
            // a menu ticket has no separate timer (spec 4); only the
            // chat arm expires — javaOpen marks the menu door
            tickets.remove(carrier.getUniqueId(), ticket);
            return false;
        }
        confirm(carrier, ticket);
        return true;
    }

    // ---------- confirm: re-validate, revive FIRST, consume after ----------

    private void confirm(Player carrier, Ticket ticket) {
        Player target = Bukkit.getPlayer(ticket.targetId);
        long now = System.currentTimeMillis();
        int count = RevivePotionItem.countPotions(plugin, carrier.getInventory());
        RevivePotionPolicy.ConfirmVerdict verdict = RevivePotionPolicy.confirmVerdict(
                plugin.revivePotionEnabled,
                tickets.get(carrier.getUniqueId()) == ticket,
                ticket.targetId.equals(carrier.getUniqueId()),
                plugin.isDowned(carrier.getUniqueId()),
                onCooldown(carrier.getUniqueId(), now),
                target != null && plugin.isDowned(target.getUniqueId()),
                target != null && target.getWorld() == carrier.getWorld(),
                target == null ? Double.MAX_VALUE : distSq(carrier, target),
                plugin.revivePotionConfirmRange,
                count);
        if (verdict != RevivePotionPolicy.ConfirmVerdict.OK) {
            tickets.remove(carrier.getUniqueId(), ticket);
            closeJavaMenu(carrier);
            switch (verdict) {
                case STALE_TICKET -> { /* silent no-op (spec 3.5) */ }
                case TARGET_NOT_DOWNED -> carrier.sendMessage(ChatColor.GOLD
                        + "✝ " + (target == null ? "They are" : target.getName() + " is")
                        + " already back up — the potion stays in your bag.");
                case NO_POTION -> carrier.sendMessage(ChatColor.GOLD
                        + "✝ No Revive Potion left — nothing was used.");
                case CARRIER_ON_COOLDOWN -> carrier.sendMessage(ChatColor.GOLD
                        + "✝ Your Revive Potion is resting — "
                        + ExcelDowned.formatClock(cooldownRemainingMs(carrier.getUniqueId(), now))
                        + " left.");
                case OUT_OF_RANGE, WRONG_WORLD -> carrier.sendMessage(ChatColor.GOLD
                        + "✝ Too far — walk up to them and try again. Nothing was used.");
                default -> carrier.sendMessage(ChatColor.GOLD
                        + "✝ That revive can't land right now. Nothing was used.");
            }
            return;
        }
        // Revive FIRST (the session remove inside is the exactly-once
        // gate); consume only if it landed — RevivePotionPolicy
        // .shouldConsume is the ordering invariant, applied literally.
        boolean landed = plugin.reviveByPotion(carrier, target);
        tickets.remove(carrier.getUniqueId(), ticket);
        closeJavaMenu(carrier);
        if (!RevivePotionPolicy.shouldConsume(landed)) {
            carrier.sendMessage(ChatColor.GOLD + "✝ " + target.getName()
                    + " is already back up — the potion stays in your bag.");
            return;
        }
        boolean consumed = RevivePotionItem.consumeOne(plugin, carrier.getInventory());
        if (!consumed) {
            // Defensive only: the count was re-validated above on the
            // same tick; if this ever fires the revive still stands
            // and the anomaly is logged, never silently swallowed.
            plugin.getLogger().warning("ExcelDowned: potion revive landed for "
                    + carrier.getName() + " -> " + target.getName()
                    + " but no tagged potion was found to consume.");
        }
        lastUseMs.put(carrier.getUniqueId(), System.currentTimeMillis());
        int left = RevivePotionItem.countPotions(plugin, carrier.getInventory());
        carrier.sendMessage(ChatColor.GREEN + "✝ You revived " + target.getName()
                + " — " + left + " Revive Potion(s) left.");
    }

    // ---------- lifecycle ----------

    /** Drop a carrier's UI (quit, carrier downed, replacement). */
    public void invalidateCarrier(UUID carrierId) {
        tickets.remove(carrierId);
        javaOpen.remove(carrierId);
        Player p = Bukkit.getPlayer(carrierId);
        if (p != null) closeJavaMenu(p);
    }

    /**
     * Invalidate every UI bound to a target whose session just
     * exited (revive / death / give-up / clear / quit): the ticket
     * dies, so a late confirm is a no-spend no-op (spec 3.5, R3).
     */
    public void invalidateForTarget(UUID targetId) {
        for (Map.Entry<UUID, Ticket> e : Map.copyOf(tickets).entrySet()) {
            if (e.getValue().targetId.equals(targetId)) {
                tickets.remove(e.getKey(), e.getValue());
                javaOpen.remove(e.getKey());
                Player carrier = Bukkit.getPlayer(e.getKey());
                if (carrier != null) closeJavaMenu(carrier);
            }
        }
    }

    public void shutdown() {
        for (UUID id : Set.copyOf(javaOpen)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) closeJavaMenu(p);
        }
        javaOpen.clear();
        tickets.clear();
    }

    private void closeJavaMenu(Player player) {
        if (player.getOpenInventory() != null
                && player.getOpenInventory().getTopInventory().getHolder()
                        instanceof RevivePotionHolder) {
            player.closeInventory();
        }
        javaOpen.remove(player.getUniqueId());
    }
}
