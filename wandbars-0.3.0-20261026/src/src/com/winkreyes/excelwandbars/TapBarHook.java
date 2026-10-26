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
    /** 0.2.5 counters: cancelled slot changes ignored (bar down), holds read from a drop, casts blocked during a hold. */
    private long heldCancelledIgnored, dropReads, dropCloses, holdCastsBlocked, pageLines;
    /** 0.2.5: right clicks with the Tap bar wand (Magic's right_click is "none" there): Bedrock reads, Java opens or puts items back. */
    private long rightReads, rightOpens, rightCloses, rightRepeats;
    private final Map<UUID, Long> lastRight = new HashMap<>();
    /** 0.2.6 Tap to pick (original): right clicks are ours (Magic's right_click is "none" there). */
    private long origRightOpens, origRightCloses, origHoldCasts, origHoldSwingCast, origRightRepeats;
    /** 0.2.6: when this player's last cast started (PreCastEvent), so one press and hold casts once. */
    private final Map<UUID, Long> lastCastStart = new HashMap<>();
    /** 0.2.5: a press and hold within this long after tapping an icon reads THAT spell (the finger is still on it). */
    private long holdReadsTapMs = 1500;
    /** 0.2.5: Tap bar stays up while the wand is held (no idle close); items back = the empty slot, Q or the wand slot hold. */
    private boolean tapBarStaysOpen = true;
    private final Map<UUID, Long> lastHoldSignal = new HashMap<>();
    private final Map<UUID, Long> lastUse = new HashMap<>();
    private final Map<UUID, String> lastCast = new HashMap<>();
    private final Map<UUID, Integer> casts = new HashMap<>();
    private long opened, closedIdle, closedEmpty, closedOther;
    private BukkitTask ticker;
    /** 0.2.7: tapping the wand itself (the slot already selected). toggle = items back when the bar is up, bar up when it is down. */
    private String tapWand = "toggle";
    private long tapWandGuardMs = 700;
    private final Map<UUID, Long> lastServerChange = new HashMap<>();
    private long wandTapCloses, wandTapOpens, wandTapGuarded, wandTapIgnored, wandTapSeen;
    private String wandTapSource = "off";

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
        holdReadsTapMs = Math.max(0, c == null ? 1500 : c.getLong("hold-reads-tap-ms", 1500));
        tapBarStaysOpen = c == null || c.getBoolean("tap-bar-stays-open", true);
        Object tw = c == null ? null : c.get("tap-wand");
        String tws = tw == null ? "toggle" : String.valueOf(tw).trim().toLowerCase(java.util.Locale.ROOT);
        tapWand = tws.equals("false") || tws.equals("off") || tws.equals("none") ? "off" : tws.equals("close") ? "close" : "toggle";
        tapWandGuardMs = Math.max(0, c == null ? 700 : c.getLong("tap-wand-guard-ms", 700));
    }

    boolean tapWandWanted() { return !"off".equals(tapWand); }
    void setTapWandSource(String s) { wandTapSource = s; }

    /** 0.2.7: WandSlotHook moved the wand and selected it: count it as the player's pick (the Tap bar opener needs one). */
    void notePick(Player p) { lastSlotPick.put(p.getUniqueId(), System.currentTimeMillis()); noteServerChange(p); }

    /** 0.2.7: the server just changed this player's hotbar or selection; a re-sent slot right after is not a tap. */
    void noteServerChange(Player p) { lastServerChange.put(p.getUniqueId(), System.currentTimeMillis()); }

    /**
     * 0.2.7 Tap bar: a Bedrock tap on the slot that is already selected, read from Geyser
     * (Geyser does not forward it; Java sends nothing at all for it). Main thread.
     */
    String onWandTap(Player p, int slot) {
        wandTapSeen++;
        if (p == null || !p.isOnline()) return "offline";
        if ("off".equals(tapWand)) { wandTapIgnored++; return "off"; }
        Wand w = tapWand(p);
        long now = System.currentTimeMillis();
        Long last = lastServerChange.get(p.getUniqueId());
        boolean wandHeld = w != null && p.getInventory().getHeldItemSlot() == slot;
        if (!wandHeld) { wandTapIgnored++; return "not the wand"; }
        if (!com.winkreyes.excelwandbars.core.WandSlotPlan.isWandTap(true, true, slot, p.getInventory().getHeldItemSlot(), now, last == null ? 0L : last, tapWandGuardMs)) {
            wandTapGuarded++; return "guard";
        }
        noteServerChange(p);
        lastUse.put(p.getUniqueId(), now);
        if (w.isInventoryOpen()) {
            w.closeInventory();
            wandTapCloses++;
            lastHold.put(p.getUniqueId(), "wand tap close");
            return "closed";
        }
        if (!"toggle".equals(tapWand)) { wandTapIgnored++; return "bar down"; }
        String r = tryOpen(p, "wand tap");
        wandTapOpens++;
        lastHold.put(p.getUniqueId(), "wand tap open " + r);
        if ("opened".equals(r)) sendPageLine(p, 3L);
        return "open " + r;
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
                + ", idle close " + (idleCloseMs == 0 ? "OFF" : (idleCloseMs / 1000) + "s" + (tapBarStaysOpen ? " (original only, Tap bar stays up)" : ""))
                + ", close before other inventory " + (closeOnOtherInventory ? "ON" : "OFF")
                + ", Tap bar hold shows spell info " + (holdShowsInfo ? "ON (never casts; hold after a tap reads that spell within " + holdReadsTapMs + " ms)" : "OFF") + ", icon lore " + (!iconLore ? "OFF" : loreMagicStyle ? "Magic's text + next rank" : "Spell Book text")
                + ", Tap to pick: tap picks (quick_cast manual), right click handled (Java open / items back, Bedrock hold casts once)"
                + ", Tap bar tap the wand: " + tapWand + " (guard " + tapWandGuardMs + " ms, source " + wandTapSource + ")";
    }
    String counters() { return "opened " + opened + " idle-closed " + closedIdle + " empty-closed " + closedEmpty + " other-closed " + closedOther
            + " hold-info " + holdsInfo + " hold-open " + holdsOpen + " hold-merged " + holdsMerged + " hold-skipped " + holdsSkipped + " lore " + loreSet
            + " held-cancelled-ignored " + heldCancelledIgnored + " drop-reads " + dropReads + " drop-closes " + dropCloses
            + " hold-casts-blocked " + holdCastsBlocked + " page-lines " + pageLines
            + " right-reads " + rightReads + " right-opens " + rightOpens + " right-closes " + rightCloses + " right-repeats " + rightRepeats
            + " pick-right-opens " + origRightOpens + " pick-right-closes " + origRightCloses + " pick-hold-casts " + origHoldCasts
            + " pick-hold-swing-cast " + origHoldSwingCast + " pick-right-repeats " + origRightRepeats
            + " wand-tap seen " + wandTapSeen + " closes " + wandTapCloses + " opens " + wandTapOpens + " guarded " + wandTapGuarded + " ignored " + wandTapIgnored; }
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
        noteServerChange(p);
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
            try { if ("opened".equals(tryOpen(p, "hold"))) sendPageLine(p, 3L); } catch (Throwable t) { plugin.getLogger().warning("Tap bar open error for " + p.getName() + ": " + t); }
        }, 4L);
    }

    /**
     * 0.2.5: MONITOR (was LOWEST), after ExcelAttunement (LOW) and Magic (NORMAL) have decided.
     * Bar down: a switch someone cancelled (ExcelAttunement's stance lock refusing the wand)
     * is ignored completely, so it can never count as "picked the wand" and open the bar.
     * Bar up: Magic itself cancels every slot change (quick cast / pick keeps the wand
     * selected), so a cancelled event is normal there; note the tapped icon, and an empty
     * slot means "give me my items back".
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onHeld(PlayerItemHeldEvent event) {
        Player p = event.getPlayer();
        if (!uses(p)) return;
        noteServerChange(p);
        Wand w = openWand(p);
        if (w == null) {
            if (event.isCancelled()) { heldCancelledIgnored++; return; }
            lastSlotPick.put(p.getUniqueId(), System.currentTimeMillis());
            return;
        }
        lastSlotPick.put(p.getUniqueId(), System.currentTimeMillis());
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
        lastCastStart.put(id, System.currentTimeMillis());
        noteServerChange(p);
        lastCast.put(id, event.getSpell() == null ? "?" : event.getSpell().getKey() + " STARTED");
        casts.merge(id, 1, Integer::sum);
        if (openWand(p) != null) {
            lastUse.put(id, System.currentTimeMillis()); scheduleLore(p, 3L);
            // 0.2.5: a tap shows what you cast (name, mana, cooldown, rank) on the action bar.
            if (tapBarStyle(p) && event.getSpell() != null) {
                final String key = event.getSpell().getKey();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!p.isOnline() || openWand(p) == null) return;
                    MagicHook mh = plugin.magicHook();
                    SpellFacts f = mh == null ? null : mh.factsFor(p, key);
                    if (f != null) p.sendActionBar(Component.text(SpellInfoText.summary(f) + " | " + SpellInfoText.rankText(f), NamedTextColor.AQUA));
                }, 2L);
            }
        }
    }

    /**
     * 0.2.5 belt and braces: in Tap bar, pressing and holding never casts. left_click is
     * "none" on the wand, but if anything still starts a cast within 300 ms of a hold
     * (swing / left click / Q) and NOT right after a slot tap, it is stopped here, before
     * Magic spends mana or starts the cooldown.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPreCastGuard(com.elmakers.mine.bukkit.api.event.PreCastEvent event) {
        Mage m = event.getMage();
        Player p = m == null || !m.isPlayer() ? null : m.getPlayer();
        if (p == null || !tapBarStyle(p)) return;
        long now = System.currentTimeMillis();
        Long hold = lastHoldSignal.get(p.getUniqueId());
        if (hold == null || now - hold > 300) return;
        Long tap = lastSlotPick.get(p.getUniqueId());
        if (tap != null && now - tap <= 300) return;
        event.setCancelled(true);
        holdCastsBlocked++;
        if (plugin.debug()) plugin.getLogger().info("Tap bar: blocked a cast during press and hold for " + p.getName());
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
        return holdShowsInfo ? tapWand(p) : null;
    }

    /** 0.2.5: the held Tap bar wand (any use, not only hold-to-read), else null. */
    private Wand tapWand(Player p) {
        if (!tapBarStyle(p)) return null;
        return inventoryRankWand(p);
    }

    /** 0.2.6: the held Tap to pick (original) wand, else null. */
    private Wand pickWand(Player p) {
        StyleHook s = plugin.styleHook();
        if (s == null || !s.enabled() || s.styleOf(p) != WandStyle.ORIGINAL) return null;
        return inventoryRankWand(p);
    }

    /** The held rank wand in inventory mode, in a normal world, no other screen open; else null. */
    private Wand inventoryRankWand(Player p) {
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
        lastHoldSignal.put(p.getUniqueId(), System.currentTimeMillis());
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
        lastHoldSignal.put(p.getUniqueId(), System.currentTimeMillis());
        if (a == Action.LEFT_CLICK_AIR) event.setCancelled(true);
        hold(p, w);
    }

    /**
     * 0.2.5: Q, or on a phone pressing and holding a hotbar slot, is a drop. Tap bar sets the
     * wand's drop action to "none" (Magic keeps the wand: it is undroppable, and spell icons
     * are never dropped either), and this decides what it means:
     *  - right after tapping a spell icon (the finger is still on it): read THAT spell,
     *  - a spell icon itself: read it,
     *  - otherwise with the bar up: put the items back (what drop did in 0.2.4),
     *  - bar down: open it.
     * Never casts. Observes only (Magic cancels the drop itself).
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        Player p = event.getPlayer();
        if (!holdShowsInfo || !tapBarStyle(p)) return;
        Wand w = holdWand(p);
        if (w == null) return;
        UUID id = p.getUniqueId();
        long now = System.currentTimeMillis();
        lastHoldSignal.put(id, now);
        MagicHook mh = plugin.magicHook();
        ItemStack dropped = event.getItemDrop() == null ? null : event.getItemDrop().getItemStack();
        String iconKey = mh == null ? null : mh.spellKeyOf(dropped);
        String tapKey = lastTapKey.get(id);
        Long at = lastTapAt.get(id);
        final String key = iconKey != null ? iconKey : (tapKey != null && at != null && now - at <= holdReadsTapMs ? tapKey : null);
        final boolean wasOpen = w.isInventoryOpen();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            Wand now2 = holdWand(p);
            if (now2 == null) return;
            if (key != null && mh != null) {
                lastSwing.put(id, System.currentTimeMillis()); // the swings of the same held finger merge into this read
                String r = mh.showSpellInfo(p, key);
                dropReads++; holdsInfo++;
                lastHold.put(id, "drop " + r);
                lastUse.put(id, System.currentTimeMillis());
                return;
            }
            if (wasOpen && now2.isInventoryOpen()) { now2.closeInventory(); noteServerChange(p); dropCloses++; lastHold.put(id, "drop close"); return; }
            if (!now2.isInventoryOpen()) { String r = tryOpen(p, "drop"); holdsOpen++; lastHold.put(id, "drop open " + r); }
        });
    }

    private final Map<UUID, Long> lastUseClick = new HashMap<>();

    /** A right click / tap on a block or entity also swings the arm: that swing is not a hold. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRightClick(PlayerInteractEvent event) {
        Action a = event.getAction();
        // 0.2.5: only a click that really uses something (chest, door, button, villager) makes the
        // swing that comes with it "not a hold". A right click on air or a plain block does not:
        // a phone's press and hold could otherwise be skipped and show nothing.
        if (a == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null && event.getClickedBlock().getType().isInteractable())
            lastUseClick.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    /**
     * 0.2.5: right click with the Tap bar wand. A phone's press and hold on the screen arrives as a
     * right click (item use), and Magic's own right_click (toggle) cannot be cancelled by a plugin,
     * so 0.2.4 closed the bar and showed nothing. Tap bar now sets Magic's right_click to "none" and
     * handles it here. Bedrock: press and hold = read (same as the swing). Java: right click opens the
     * bar or puts your items back (what Magic's toggle did). Crouch + right click stays Magic's page
     * flip (right_click_sneak). A click that uses a block (chest, door) is left alone.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onRightClickWand(PlayerInteractEvent event) {
        Action a = event.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != null && event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        Player p = event.getPlayer();
        if (p.isSneaking()) return;
        if (a == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null && event.getClickedBlock().getType().isInteractable()) return;
        Wand w = tapWand(p);
        if (w == null) return;
        UUID id = p.getUniqueId();
        long now = System.currentTimeMillis();
        Long prevRight = lastRight.put(id, now);
        boolean repeat = prevRight != null && now - prevRight < 350; // a held mouse button / finger repeats every 4 ticks
        if (plugin.isBedrock(p) && holdShowsInfo) {
            lastHoldSignal.put(id, now);
            rightReads++;
            hold(p, w); // merges with the swing of the same finger
            return;
        }
        if (repeat) { rightRepeats++; return; }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            Wand n = tapWand(p);
            if (n == null) return;
            if (n.isInventoryOpen()) { n.closeInventory(); noteServerChange(p); rightCloses++; lastHold.put(id, "right close"); }
            else { String r = tryOpen(p, "right click"); rightOpens++; lastHold.put(id, "right open " + r); if ("opened".equals(r)) sendPageLine(p, 3L); }
        });
    }

    /**
     * 0.2.6: right click with the Tap to pick (original) wand. Magic's right_click is "none" for this
     * style (its toggle closed the bar when a phone's press and hold arrived as a right click, so the
     * picked spell never cast). Here:
     *  - bar down (Java and Bedrock): open the bar, as Magic's toggle did,
     *  - Java, bar up: put the items back, as Magic's toggle did,
     *  - Bedrock, bar up: press and hold casts the picked spell ONCE. If the swing of the same hold
     *    already started a cast (Magic's left_click cast), nothing more happens.
     * A held finger or mouse button repeats every 4 ticks; repeats are ignored. Crouch + right click
     * stays Magic's page flip (right_click_sneak). A click that uses a block (chest, door) is left alone.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onRightClickPick(PlayerInteractEvent event) {
        Action a = event.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != null && event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        Player p = event.getPlayer();
        if (p.isSneaking()) return;
        if (a == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null && event.getClickedBlock().getType().isInteractable()) return;
        Wand w = pickWand(p);
        if (w == null) return;
        UUID id = p.getUniqueId();
        long now = System.currentTimeMillis();
        Long prevRight = lastRight.put(id, now);
        if (prevRight != null && now - prevRight < 350) { origRightRepeats++; return; }
        final boolean bedrock = plugin.isBedrock(p);
        final boolean wasOpen = w.isInventoryOpen();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            Wand n = pickWand(p);
            if (n == null) return;
            if (!n.isInventoryOpen()) {
                if (wasOpen) return; // something else closed it in between (empty slot, Q): leave it
                String r = tryOpen(p, "right click");
                origRightOpens++;
                lastHold.put(id, "pick right open " + r);
                if ("opened".equals(r)) sendPageLine(p, 3L);
                return;
            }
            if (!wasOpen) return; // opened in between by the hotbar opener: leave it up
            if (!bedrock) { n.closeInventory(); origRightCloses++; lastHold.put(id, "pick right close"); return; }
            Long c = lastCastStart.get(id);
            if (c != null && c >= now - 400) { origHoldSwingCast++; lastHold.put(id, "pick hold (swing cast)"); return; }
            boolean ok = n.cast();
            origHoldCasts++;
            lastHold.put(id, "pick hold cast " + n.getActiveSpellKey() + (ok ? "" : " (not cast)"));
        }, bedrock ? 3L : 1L);
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
            if ("opened".equals(r)) sendPageLine(p, 3L);
            return;
        }
        MagicHook mh = plugin.magicHook();
        if (mh == null) return;
        // 0.2.5: the finger still on the icon just tapped = read that spell; otherwise the list
        // of all your spells (Bedrock form: tap one for the full page; Java: chat with hover).
        String key = lastTapKey.get(id);
        Long at = lastTapAt.get(id);
        if (key != null && (at == null || now - at > Math.min(rememberTapMs, holdReadsTapMs))) key = null;
        String r = mh.showSpellInfo(p, key);
        holdsInfo++;
        lastHold.put(id, r);
        if (plugin.debug()) plugin.getLogger().info("Tap bar hold for " + p.getName() + ": " + r);
    }

    /** 0.2.5: "Page 1/2: Heal, Boon, ..." once when the bar opens with more than one page. */
    private void sendPageLine(Player p, long ticks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            MagicHook mh = plugin.magicHook();
            String line = mh == null ? null : mh.pageLine(p);
            if (line == null) return;
            p.sendActionBar(Component.text(line, NamedTextColor.AQUA));
            pageLines++;
        }, ticks);
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
        if (n > 0) noteServerChange(p);
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

    /** 0.2.7: a crouch can flip the page (the hotbar icons change): not a wand tap right after. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSneakStamp(org.bukkit.event.player.PlayerToggleSneakEvent event) {
        if (event.isSneaking() && uses(event.getPlayer())) noteServerChange(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lastHoldSignal.remove(id); lastRight.remove(id); lastUse.remove(id); lastSwing.remove(id); lastTapKey.remove(id); lastTapAt.remove(id); lastHold.remove(id); lastUseClick.remove(id); lastSlotPick.remove(id); lastCastStart.remove(id); lastServerChange.remove(id);
    }

    private void tick() {
        if (idleCloseMs <= 0) return;
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!uses(p)) continue;
            if (tapBarStaysOpen && tapBarStyle(p)) continue; // 0.2.5: Tap bar stays up while the wand is held
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
