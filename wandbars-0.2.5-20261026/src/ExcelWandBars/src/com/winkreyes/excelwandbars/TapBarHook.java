package com.winkreyes.excelwandbars;

import com.elmakers.mine.bukkit.api.event.CastEvent;
import com.elmakers.mine.bukkit.api.event.WandActivatedEvent;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.elmakers.mine.bukkit.api.wand.Wand;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.winkreyes.excelwandbars.core.SpellFacts;
import com.winkreyes.excelwandbars.core.SpellInfoText;
import com.winkreyes.excelwandbars.core.WandStyle;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 0.2.4: the touch opener for the two hotbar-icon styles (Tap bar and Tap to
 * cycle (original)). Magic (elmakers) already does the hard part: in inventory
 * mode it stores the player's real hotbar, fills it with spell icons, casts
 * (quick cast) or picks (no quick cast) on a slot change and cancels that
 * change so the wand stays selected, and puts the real items back on close,
 * death, quit and world change. On Bedrock touch nothing reaches the server
 * for a tap on air, so Magic's "right click opens the bar" never fires.
 * This hook adds:
 *  - picking the wand in the hotbar opens the bar (the wand becomes active),
 *  - tapping an EMPTY slot while the bar is open puts the items back and
 *    selects that slot (so a phone player is never stuck on the wand),
 *  - the bar closes by itself after close-after-idle-seconds with no spell,
 *  - the bar closes before another inventory (chest, furnace...) opens and
 *    when this plugin stops (belt and braces: Magic also closes it).
 * Closing always uses Magic's own Wand.closeInventory(), which restores the
 * stored items. This class never creates, moves or deletes an item itself.
 *
 * 0.2.4 hold to read (Tap bar only):
 *  - On touch, Geyser turns a press and hold into an arm swing (left click).
 *    In Tap bar that swing no longer casts (casting is the one tap on an icon):
 *    with the bar down it opens the bar, with the bar up it shows the Spell
 *    Book detail page (the same text as /spellinfo: what it does, cost,
 *    cooldown, range, rank and the live "Next rank: N of M casts") for the
 *    last spell icon tapped, or the whole Spell Book when none was tapped yet.
 *    A held finger sends several swings; they count as one hold (hold-gap-ms).
 *  - The icons' name and lore get the same lines (icon-lore), refreshed when
 *    the bar opens and after each cast. Only the display name and lore of an
 *    item Magic itself marks as a spell icon change; nothing is created or
 *    moved, and Magic's own tags stay, so Magic still restores the real items.
 */
final class TapBarHook implements Listener {
    private final ExcelWandBars plugin;
    private final MagicAPI api;
    private boolean openOnHold = true;
    private boolean emptySlotCloses = true;
    private long idleCloseMs = 15_000;
    private boolean closeOnOtherInventory = true;
    private boolean holdShowsInfo = true;
    private long holdGapMs = 700;
    private long rememberTapMs = 30_000;
    private boolean iconLore = true;
    /** spellbook = name + lore are the Spell Book lines; magic = Magic's own name/lore kept, the next-rank line added. */
    private boolean loreMagicStyle = false;
    private final Map<UUID, Long> lastSwing = new HashMap<>();
    private final Map<UUID, Long> lastSlotPick = new HashMap<>();
    private final Map<UUID, String> lastTapKey = new HashMap<>();
    private final Map<UUID, Long> lastTapAt = new HashMap<>();
    private final Map<UUID, String> lastHold = new HashMap<>();
    private long holdsInfo, holdsOpen, holdsMerged, holdsSkipped, loreSet;
    private final Map<UUID, Long> lastUse = new HashMap<>();
    private final Map<UUID, String> lastCast = new HashMap<>();
    private final Map<UUID, Integer> casts = new HashMap<>();
    private long opened, closedIdle, closedEmpty, closedOther;
    private BukkitTask ticker;

    TapBarHook(ExcelWandBars plugin, MagicAPI api) { this.plugin = plugin; this.api = api; }

    void loadSettings(ConfigurationSection c) {
        openOnHold = c == null || c.getBoolean("open-on-hold", true);
        emptySlotCloses = c == null || c.getBoolean("empty-slot-closes", true);
        idleCloseMs = Math.max(0, (c == null ? 15 : c.getLong("close-after-idle-seconds", 15)) * 1000L);
        closeOnOtherInventory = c == null || c.getBoolean("close-before-other-inventory", true);
        holdShowsInfo = c == null || c.getBoolean("hold-shows-info", true);
        holdGapMs = Math.max(0, c == null ? 700 : c.getLong("hold-gap-ms", 700));
        rememberTapMs = Math.max(0, (c == null ? 30 : c.getLong("info-remembers-tap-seconds", 30)) * 1000L);
        Object il = c == null ? null : c.get("icon-lore");
        String ils = il == null ? "spellbook" : String.valueOf(il).trim().toLowerCase(java.util.Locale.ROOT);
        iconLore = !(ils.equals("false") || ils.equals("off") || ils.equals("none"));
        loreMagicStyle = ils.equals("magic") || ils.equals("magic-plus-rank");
    }

    void start() {
        if (ticker != null) ticker.cancel();
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
    }

    /** Plugin stop: put every open hotbar-icon bar back (Magic's own close, real items restored). */
    void stop() {
        if (ticker != null) { ticker.cancel(); ticker = null; }
        for (Player p : Bukkit.getOnlinePlayers()) {
            try { Wand w = openWand(p); if (w != null && uses(p)) w.closeInventory(); } catch (Throwable ignored) { }
        }
    }

    String summary() {
        return "open on hold " + (openOnHold ? "ON" : "OFF") + ", empty slot closes " + (emptySlotCloses ? "ON" : "OFF")
                + ", idle close " + (idleCloseMs == 0 ? "OFF" : (idleCloseMs / 1000) + "s")
                + ", close before other inventory " + (closeOnOtherInventory ? "ON" : "OFF")
                + ", Tap bar hold shows spell info " + (holdShowsInfo ? "ON" : "OFF") + ", icon lore " + (!iconLore ? "OFF" : loreMagicStyle ? "Magic's text + next rank" : "Spell Book text");
    }
    String counters() { return "opened " + opened + " idle-closed " + closedIdle + " empty-closed " + closedEmpty + " other-closed " + closedOther
            + " hold-info " + holdsInfo + " hold-open " + holdsOpen + " hold-merged " + holdsMerged + " hold-skipped " + holdsSkipped + " lore " + loreSet; }
    String lastHold(Player p) { return lastHold.getOrDefault(p.getUniqueId(), "-"); }
    String lastCast(Player p) { return lastCast.getOrDefault(p.getUniqueId(), "-") + " x" + casts.getOrDefault(p.getUniqueId(), 0); }

    private boolean uses(Player p) {
        StyleHook s = plugin.styleHook();
        return s != null && s.enabled() && s.styleOf(p).opensOnHold();
    }

    private Wand activeWand(Player p) {
        Mage m = api.getMage(p);
        return m == null ? null : m.getActiveWand();
    }

    private Wand openWand(Player p) {
        Wand w = activeWand(p);
        return w != null && w.isInventoryOpen() ? w : null;
    }

    private static boolean empty(ItemStack it) { return it == null || it.getType().isAir(); }

    /** Open the held rank wand's spell bar if this player's style wants it and nothing blocks it. */
    String tryOpen(Player p, String why) {
        if (p == null || !p.isOnline()) return "offline";
        if (!uses(p)) return "style";
        StyleHook s = plugin.styleHook();
        Wand w = activeWand(p);
        if (w == null) return "no wand";
        if (w.isInventoryOpen()) return "already open";
        if (!s.isRankWand(w) || !w.canUse(p)) return "not rank wand";
        Object mode = w.getProperty("mode");
        if (mode == null || !String.valueOf(mode).equalsIgnoreCase("inventory")) return "not inventory mode";
        if (plugin.skipWorld(p.getWorld())) return "skip world";
        if (p.getGameMode() == GameMode.SPECTATOR || p.isDead()) return "spectator/dead";
        InventoryType top = p.getOpenInventory().getTopInventory().getType();
        if (top != InventoryType.CRAFTING && top != InventoryType.PLAYER) return "other inventory open (" + top + ")";
        w.openInventory();
        opened++;
        scheduleLore(p, 2L);
        lastUse.put(p.getUniqueId(), System.currentTimeMillis());
        if (plugin.debug()) plugin.getLogger().info("Tap bar opened for " + p.getName() + " (" + why + ")");
        return w.isInventoryOpen() ? "opened" : "open failed";
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWandActivated(WandActivatedEvent event) {
        if (!openOnHold) return;
        Mage mage = event.getMage();
        Player p = mage == null || !mage.isPlayer() ? null : mage.getPlayer();
        if (p == null || !uses(p)) return;
        // Only a real pick of the wand slot opens the bar (not a respawn, join, world change or /give).
        Long picked = lastSlotPick.get(p.getUniqueId());
        if (picked == null || System.currentTimeMillis() - picked > 2000) return;
        // After StyleHook's next-tick apply (which re-activates the wand once when it changes controls).
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try { tryOpen(p, "hold"); } catch (Throwable t) { plugin.getLogger().warning("Tap bar open error for " + p.getName() + ": " + t); }
        }, 4L);
    }

    /** Before Magic's handler: note the tap, and an empty slot means "give me my items back". */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onHeld(PlayerItemHeldEvent event) {
        Player p = event.getPlayer();
        if (!uses(p)) return;
        lastSlotPick.put(p.getUniqueId(), System.currentTimeMillis());
        Wand w = openWand(p);
        if (w == null) return;
        lastUse.put(p.getUniqueId(), System.currentTimeMillis());
        int slot = event.getNewSlot();
        MagicHook mh = plugin.magicHook();
        String key = mh == null ? null : mh.spellKeyOf(p.getInventory().getItem(slot));
        if (key != null) { lastTapKey.put(p.getUniqueId(), key); lastTapAt.put(p.getUniqueId(), System.currentTimeMillis()); }
        if (!emptySlotCloses) return;
        if (!empty(p.getInventory().getItem(slot))) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            Wand now = openWand(p);
            if (now == null) return;
            now.closeInventory();
            closedEmpty++;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!p.isOnline()) return;
                Wand again = openWand(p);
                if (again != null) return; // something re-opened it; leave the selection alone
                p.getInventory().setHeldItemSlot(slot);
                Mage m = api.getMage(p);
                if (m != null) m.checkWand();
            });
        });
    }

    /** A cast starts (Magic fires CastEvent only when a spell with a duration ends, so count here). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPreCast(com.elmakers.mine.bukkit.api.event.PreCastEvent event) {
        Mage m = event.getMage();
        Player p = m == null || !m.isPlayer() ? null : m.getPlayer();
        if (p == null) return;
        UUID id = p.getUniqueId();
        lastCast.put(id, event.getSpell() == null ? "?" : event.getSpell().getKey() + " STARTED");
        casts.merge(id, 1, Integer::sum);
        if (openWand(p) != null) { lastUse.put(id, System.currentTimeMillis()); scheduleLore(p, 3L); }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCast(CastEvent event) {
        Mage m = event.getMage();
        Player p = m == null || !m.isPlayer() ? null : m.getPlayer();
        if (p == null) return;
        UUID id = p.getUniqueId();
        lastCast.put(id, event.getSpell() == null ? "?" : event.getSpell().getKey() + " " + event.getSpellResult());
        if (openWand(p) != null) { lastUse.put(id, System.currentTimeMillis()); scheduleLore(p, 3L); }
    }

    // ------------------------------------------------------------------ hold to read (Tap bar)

    private boolean tapBarStyle(Player p) {
        StyleHook s = plugin.styleHook();
        return s != null && s.enabled() && s.styleOf(p) == WandStyle.TAPBAR;
    }

    /** The held wand when this swing belongs to Tap bar's hold-to-read, else null. */
    private Wand holdWand(Player p) {
        if (!holdShowsInfo || !tapBarStyle(p)) return null;
        if (plugin.skipWorld(p.getWorld()) || p.getGameMode() == GameMode.SPECTATOR || p.isDead()) return null;
        InventoryType top = p.getOpenInventory().getType();
        if (top != InventoryType.CRAFTING && top != InventoryType.CREATIVE) return null;
        Wand w = activeWand(p);
        if (w == null || !plugin.styleHook().isRankWand(w)) return null;
        Object mode = w.getProperty("mode");
        if (mode == null || !String.valueOf(mode).equalsIgnoreCase("inventory")) return null;
        return w;
    }

    /** Before Magic (NORMAL, ignoreCancelled): in Tap bar a swing is "press and hold", never a cast. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        Player p = event.getPlayer();
        Wand w = holdWand(p);
        if (w == null) return;
        event.setCancelled(true);
        hold(p, w);
    }

    /** Magic only casts from this event when animation events are off; cancel the left click in Tap bar too. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onLeftClick(PlayerInteractEvent event) {
        Action a = event.getAction();
        if (a != Action.LEFT_CLICK_AIR && a != Action.LEFT_CLICK_BLOCK) return;
        if (event.getHand() != null && event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        Player p = event.getPlayer();
        Wand w = holdWand(p);
        if (w == null) return;
        if (a == Action.LEFT_CLICK_AIR) event.setCancelled(true);
        hold(p, w);
    }

    private final Map<UUID, Long> lastUseClick = new HashMap<>();

    /** A right click / tap on a block or entity also swings the arm: that swing is not a hold. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRightClick(PlayerInteractEvent event) {
        Action a = event.getAction();
        if (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK) lastUseClick.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRightClickEntity(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        lastUseClick.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    private boolean nearUseClick(UUID id) {
        Long t = lastUseClick.get(id);
        return t != null && Math.abs(System.currentTimeMillis() - t) < 350;
    }

    private void hold(Player p, Wand w0) {
        UUID id = p.getUniqueId();
        // Decide 3 ticks later: the right click that came with this swing (chest, door, villager) arrives
        // in the same burst, before or after it. Then it was a use, not a hold.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            if (nearUseClick(id)) { holdsSkipped++; return; }
            Wand w = holdWand(p);
            if (w == null) { holdsSkipped++; return; }
            holdNow(p, w);
        }, 3L);
    }

    private void holdNow(Player p, Wand w) {
        UUID id = p.getUniqueId();
        long now = System.currentTimeMillis();
        Long prev = lastSwing.put(id, now);
        if (prev != null && now - prev < holdGapMs) { holdsMerged++; return; } // the same finger still down
        lastUse.put(id, now);
        if (!w.isInventoryOpen()) {
            String r = tryOpen(p, "press and hold");
            holdsOpen++;
            lastHold.put(id, "open " + r);
            return;
        }
        MagicHook mh = plugin.magicHook();
        if (mh == null) return;
        String key = lastTapKey.get(id);
        Long at = lastTapAt.get(id);
        if (key != null && (at == null || now - at > rememberTapMs)) key = null;
        String r = mh.showSpellInfo(p, key);
        holdsInfo++;
        lastHold.put(id, r);
        if (plugin.debug()) plugin.getLogger().info("Tap bar hold for " + p.getName() + ": " + r);
    }

    // ------------------------------------------------------------------ icon name / lore = the Spell Book lines

    private void scheduleLore(Player p, long ticks) {
        if (!iconLore || !tapBarStyle(p)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> { try { applyLore(p); } catch (Throwable t) {
            if (plugin.debug()) plugin.getLogger().warning("Tap bar lore for " + p.getName() + ": " + t); } }, ticks);
    }

    /** Hotbar spell icons only, bar open only: set name + lore to the Spell Book lines. Returns icons changed. */
    int applyLore(Player p) {
        if (!p.isOnline() || openWand(p) == null) return 0;
        MagicHook mh = plugin.magicHook();
        if (mh == null) return 0;
        int n = 0;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack it = p.getInventory().getItem(slot);
            String key = mh.spellKeyOf(it);
            if (key == null) continue;
            SpellFacts f = mh.factsFor(p, key);
            if (f == null) continue;
            List<String> lines = SpellInfoText.lines(f);
            ItemMeta meta = it.getItemMeta();
            if (meta == null) continue;
            if (loreMagicStyle) {
                // Keep Magic's own lore; replace only our rank line at the end.
                List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
                while (!lore.isEmpty()) {
                    String last = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(lore.get(lore.size() - 1));
                    if (last.startsWith("Next rank") || last.startsWith("Top rank")) lore.remove(lore.size() - 1); else break;
                }
                String rankLine = null;
                for (String l : lines) if (l.startsWith("Next rank") || l.startsWith("Top rank")) rankLine = l;
                if (rankLine != null) lore.add(Component.text(rankLine, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                if (lore.equals(meta.lore())) continue;
                meta.lore(lore);
                it.setItemMeta(meta);
                n++;
                continue;
            }
            Component name = Component.text(lines.get(0), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false);
            List<Component> lore = new ArrayList<>();
            for (String l : lines.subList(1, lines.size())) lore.add(Component.text(l, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            if (name.equals(meta.displayName()) && lore.equals(meta.lore())) continue;
            meta.displayName(name);
            meta.lore(lore);
            it.setItemMeta(meta);
            // Still Magic's spell icon after the edit? If not, it is a bug: say so (the bar still closes and restores).
            if (mh.spellKeyOf(it) == null) plugin.getLogger().warning("Tap bar lore: icon in slot " + slot + " no longer reads as a spell");
            n++;
        }
        loreSet += n;
        return n;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onOtherInventory(InventoryOpenEvent event) {
        if (!closeOnOtherInventory || !(event.getPlayer() instanceof Player p) || !uses(p)) return;
        InventoryType t = event.getInventory().getType();
        if (t == InventoryType.CRAFTING || t == InventoryType.PLAYER) return;
        Wand w = openWand(p);
        if (w == null) return;
        w.closeInventory();
        closedOther++;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lastUse.remove(id); lastSwing.remove(id); lastTapKey.remove(id); lastTapAt.remove(id); lastHold.remove(id); lastUseClick.remove(id); lastSlotPick.remove(id);
    }

    private void tick() {
        if (idleCloseMs <= 0) return;
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!uses(p)) continue;
            Wand w = openWand(p);
            if (w == null) continue;
            Long last = lastUse.get(p.getUniqueId());
            if (last == null) { lastUse.put(p.getUniqueId(), now); continue; }
            if (now - last >= idleCloseMs) {
                w.closeInventory();
                closedIdle++;
                lastUse.remove(p.getUniqueId());
            }
        }
    }
}
