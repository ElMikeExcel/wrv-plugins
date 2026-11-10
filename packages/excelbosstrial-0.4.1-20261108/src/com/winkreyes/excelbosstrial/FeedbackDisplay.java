/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial;

import com.winkreyes.excelbosstrial.core.BossKind;
import com.winkreyes.excelbosstrial.core.EncounterLedger;
import com.winkreyes.excelbosstrial.core.FeedbackOptOuts;
import com.winkreyes.excelbosstrial.core.FeedbackSettings;
import com.winkreyes.excelbosstrial.core.FeedbackTotals;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;

/**
 * The feel patch's Bukkit surface (ExcelBossTrial 0.3.6): owns
 * every numbered boss bar and every action-bar damage total.
 *
 * DISPLAY ONLY (DESIGN section 2): this class reads ledger
 * getters ({@code maxHealth}, {@code totalDamage},
 * {@code dealtBy}, {@code participants}) and the dragon
 * chapter's phase view, and writes NOTHING back - no event is
 * touched, no ledger mutator is called, no entity state changes
 * except the visibility/title of the display bars themselves
 * (and the wither/dragon vanilla-bar suppression below). A
 * failure here can never reach the fight: every entry point is
 * fail-soft (the FightTracker / ProtectEmit house pattern) -
 * the first display exception is logged once and the whole
 * display path disables itself for the boot.
 *
 * Bar ownership (DESIGN section 4): BossTrial owns one numbered
 * bar per encounter on EVERY chapter, so title, audience and
 * opt-out are uniform - including the warden, which vanilla
 * gives no bar at all. For the wither and the dragon, the
 * entity's vanilla bar is hidden for the encounter's life (its
 * progress fraction is the same number this bar shows, by
 * IntakeFactor design) and restored at close. Elder guardians
 * have no vanilla bar at all (finding R-1, TEST-REPORT: the
 * Paper 26.3 ElderGuardian class carries no ServerBossEvent
 * and CraftBukkit exposes no handle, because there is nothing
 * to handle) - the numbered bar is the only bar an elder gets.
 *
 * The dragon's title is priced PER PHASE from the host's phase
 * view (pool 4,000 then 16,000 at the settled slate): the main
 * ledger spans both phases, so the host supplies
 * {phasePool, phaseDealt} through the phase source; the title
 * total flips at the phase boundary on the next hit or sweep.
 */
final class FeedbackDisplay {

    /** One encounter's bar + the vanilla handle it suppressed. */
    private static final class BarState {
        final BossKind kind;
        final BossBar bar;
        final BossBar vanillaBar;
        String lastTitle = "";
        double lastProgress = -1.0;
        long lastReconcileMs;

        BarState(BossKind kind, BossBar bar, BossBar vanillaBar) {
            this.kind = kind;
            this.bar = bar;
            this.vanillaBar = vanillaBar;
        }
    }

    private final Logger log;
    private final FeedbackOptOuts optOuts;
    /** {phasePool, phaseDealt} for a dragon boss, or null when
     *  the chapter does not track it (ledger view is the fallback). */
    private final Function<UUID, double[]> dragonPhaseSource;
    /** 0.4.0 (PM-5): whether an encounter is verdict-pending -
     *  while it is, the bar's title renders its named FALLING
     *  state instead of a frozen 0-remaining frame: the bar is
     *  the boss, and a judged boss is falling, not stuck. */
    private final java.util.function.Predicate<UUID> verdictPendingSource;
    private final Map<UUID, BarState> bars = new HashMap<>();
    /** Per-player action-bar throttle stamps (across encounters). */
    private final Map<UUID, Long> lastActionBarMs = new HashMap<>();
    /** Per-player last action-bar line (diagnostic surface: the
     *  rig reads it to prove the total equals the fight table). */
    private final Map<UUID, String> lastActionBarLine = new HashMap<>();
    private FeedbackSettings global = FeedbackSettings.defaults();
    private final Map<BossKind, FeedbackSettings> chapters = new EnumMap<>(BossKind.class);
    private boolean disabled;
    private boolean failureLogged;

    FeedbackDisplay(Logger log, FeedbackOptOuts optOuts,
                    Function<UUID, double[]> dragonPhaseSource) {
        this(log, optOuts, dragonPhaseSource, null);
    }

    FeedbackDisplay(Logger log, FeedbackOptOuts optOuts,
                    Function<UUID, double[]> dragonPhaseSource,
                    java.util.function.Predicate<UUID> verdictPendingSource) {
        this.log = log;
        this.optOuts = optOuts;
        this.dragonPhaseSource = dragonPhaseSource;
        this.verdictPendingSource = verdictPendingSource;
    }

    /** The host's parsed settings (loadSettings; runs again on
     *  every /bosstrial reload). */
    void configure(FeedbackSettings globalSettings,
                   Map<BossKind, FeedbackSettings> chapterSettings) {
        if (globalSettings != null) {
            global = globalSettings;
        }
        chapters.clear();
        if (chapterSettings != null) {
            chapters.putAll(chapterSettings);
        }
    }

    private FeedbackSettings settings(BossKind kind) {
        FeedbackSettings s = chapters.get(kind);
        return s != null ? s : global;
    }

    // ------------------------------------------------------------------
    // Encounter lifecycle
    // ------------------------------------------------------------------

    /** The encounter's ledger exists (the host calls this on
     *  every encounter() touch; creation happens once). */
    void onEncounterOpen(LivingEntity boss, BossKind kind, EncounterLedger ledger) {
        if (disabled || boss == null || kind == null || ledger == null) {
            return;
        }
        try {
            if (bars.containsKey(boss.getUniqueId())) {
                return;
            }
            FeedbackSettings s = settings(kind);
            if (!s.poolNumbersOn()) {
                return;
            }
            BossBar vanilla = null;
            if (boss instanceof Wither wither) {
                vanilla = wither.getBossBar();
            } else if (boss instanceof EnderDragon dragon) {
                vanilla = dragon.getBossBar();
            }
            if (vanilla != null) {
                vanilla.setVisible(false);
            }
            BossBar bar = Bukkit.createBossBar("", barColor(kind), BarStyle.SEGMENTED_20);
            BarState state = new BarState(kind, bar, vanilla);
            bars.put(boss.getUniqueId(), state);
            refresh(boss, state, ledger, true);
        } catch (Throwable failure) {
            failSoft("encounter open", failure);
        }
    }

    /** A hit was just recorded (post-record, post-rally): move
     *  the title by the hit's true ledger amount and hand the
     *  damager their running total. {@code credited} is what the
     *  ledger credited the damager for this hit (0 for a
     *  band-excluded or unattributed blow). */
    void onRecordedHit(LivingEntity boss, BossKind kind, EncounterLedger ledger,
                       UUID dealt, double credited) {
        if (disabled || boss == null || kind == null || ledger == null) {
            return;
        }
        try {
            BarState state = bars.get(boss.getUniqueId());
            if (state != null) {
                refresh(boss, state, ledger, true);
            }
            FeedbackSettings s = settings(kind);
            if (dealt == null || credited <= 0 || !s.actionBarOn()
                    || optOuts.isOptedOut(dealt)) {
                return;
            }
            Player player = Bukkit.getPlayer(dealt);
            if (player == null || !player.isOnline()) {
                return;
            }
            long now = System.currentTimeMillis();
            long last = lastActionBarMs.getOrDefault(dealt, 0L);
            if (now - last < s.damageTotalMinIntervalMs()) {
                return;
            }
            lastActionBarMs.put(dealt, now);
            String line = FeedbackTotals.actionBarLine(
                    s.damageTotalFormat(), credited, ledger, dealt);
            lastActionBarLine.put(dealt, line);
            player.sendActionBar(LegacyComponentSerializer.legacySection()
                    .deserialize(color(line)));
        } catch (Throwable failure) {
            failSoft("recorded hit", failure);
        }
    }

    /** The sweep pass (the host's 1-second encounter loop):
     *  cadence-gated title refresh + audience reconciliation, so
     *  a player who walks up mid-fight gets the bar without
     *  hitting anything, and the dragon's title total flips at
     *  the phase boundary even between hits. */
    void reconcile(LivingEntity boss, BossKind kind, EncounterLedger ledger, long nowMs) {
        if (disabled || boss == null || kind == null || ledger == null) {
            return;
        }
        try {
            BarState state = bars.get(boss.getUniqueId());
            if (state == null) {
                // The encounter predates the bar (plugin reload
                // mid-fight, or pool-numbers was off at open and
                // a reload armed it): open it now.
                onEncounterOpen(boss, kind, ledger);
                return;
            }
            if (nowMs - state.lastReconcileMs < settings(kind).titleTickMs()) {
                return;
            }
            state.lastReconcileMs = nowMs;
            refresh(boss, state, ledger, true);
        } catch (Throwable failure) {
            failSoft("sweep reconcile", failure);
        }
    }

    /** The encounter ended (verdict, give-up, prune): the bar
     *  leaves every screen and the vanilla bar returns. */
    void onEncounterClose(UUID bossId) {
        if (bossId == null) {
            return;
        }
        try {
            BarState state = bars.remove(bossId);
            if (state != null) {
                teardown(state);
            }
        } catch (Throwable failure) {
            failSoft("encounter close", failure);
        }
    }

    /** Every bar down (disable / admin reset). */
    void closeAll() {
        try {
            for (BarState state : bars.values()) {
                teardown(state);
            }
            bars.clear();
        } catch (Throwable failure) {
            failSoft("close all", failure);
        }
    }

    private void teardown(BarState state) {
        state.bar.removeAll();
        if (state.vanillaBar != null) {
            try {
                state.vanillaBar.setVisible(true);
            } catch (Throwable ignored) {
                // The entity (and its bar) is gone; nothing to restore.
            }
        }
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    private void refresh(LivingEntity boss, BarState state, EncounterLedger ledger,
                         boolean reconcileAudience) {
        FeedbackTotals view = view(state.kind, boss, ledger);
        FeedbackSettings s = settings(state.kind);
        // 0.4.0 (PM-5): a verdict-pending boss renders its named
        // FALLING title - a distinct state, never a number on a
        // corpse-frame ("0 remaining" while still standing was
        // the Nov 7 bar lie's visible half).
        boolean falling = verdictPendingSource != null
                && verdictPendingSource.test(boss.getUniqueId());
        String title = falling
                ? com.winkreyes.excelbosstrial.core.FeedbackFormat.title(
                        s.exhaustedFormat(), state.kind, 0.0, 0.0)
                : view.title(s.titleFormat(), state.kind);
        if (!title.equals(state.lastTitle)) {
            state.bar.setTitle(color(title));
            state.lastTitle = title;
        }
        if (Math.abs(view.fraction() - state.lastProgress) > 0.0005) {
            state.bar.setProgress(view.fraction());
            state.lastProgress = view.fraction();
        }
        if (reconcileAudience) {
            reconcileAudience(boss, state, ledger, s);
        }
    }

    /** The numbers a title shows: the dragon prices per phase,
     *  every other chapter straight off the ledger. */
    private FeedbackTotals view(BossKind kind, LivingEntity boss, EncounterLedger ledger) {
        if (kind == BossKind.ENDER_DRAGON && dragonPhaseSource != null) {
            double[] phase = dragonPhaseSource.apply(boss.getUniqueId());
            if (phase != null && phase.length == 2 && phase[0] > 0) {
                return FeedbackTotals.forPhase(phase[0], phase[1]);
            }
        }
        return FeedbackTotals.fromLedger(ledger);
    }

    /** Audience = ledger participants + online players within
     *  the viewer range of the boss, minus the opted-out. */
    private void reconcileAudience(LivingEntity boss, BarState state,
                                   EncounterLedger ledger, FeedbackSettings s) {
        Set<UUID> want = new HashSet<>();
        want.addAll(ledger.participants());
        double rangeSq = (double) s.viewerRange() * s.viewerRange();
        for (Player p : boss.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(boss.getLocation()) <= rangeSq) {
                want.add(p.getUniqueId());
            }
        }
        want.removeIf(optOuts::isOptedOut);
        Set<UUID> have = new HashSet<>();
        for (Player p : state.bar.getPlayers()) {
            have.add(p.getUniqueId());
        }
        for (UUID id : want) {
            if (!have.contains(id)) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline()) {
                    state.bar.addPlayer(p);
                }
            }
        }
        for (UUID id : have) {
            if (!want.contains(id)) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    state.bar.removePlayer(p);
                }
            }
        }
    }

    /** Bar colors echo vanilla: wither red, elder purple,
     *  warden blue, dragon pink (DESIGN section 4; constants,
     *  not config - color is not balance). */
    private static BarColor barColor(BossKind kind) {
        return switch (kind) {
            case WITHER -> BarColor.RED;
            case ELDER_GUARDIAN -> BarColor.PURPLE;
            case WARDEN -> BarColor.BLUE;
            case ENDER_DRAGON -> BarColor.PINK;
        };
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    // ------------------------------------------------------------------
    // Diagnostic surface (rig + status): titles and lines as the
    // players last saw them.
    // ------------------------------------------------------------------

    /** The current title of one encounter's bar ("" when none). */
    String titleOf(UUID bossId) {
        BarState state = bars.get(bossId);
        return state == null ? "" : state.lastTitle;
    }

    /** How many encounter bars are up right now. */
    int openBars() {
        return bars.size();
    }

    /** One player's last action-bar damage line ("" when none). */
    String lastActionBarOf(UUID player) {
        return lastActionBarLine.getOrDefault(player, "");
    }

    /** True once a display failure has disabled the path (boot). */
    boolean isDisabled() {
        return disabled;
    }

    /** Fail-soft (DESIGN section 2.3): log once, disable for the
     *  boot, never throw into the fight. */
    private void failSoft(String where, Throwable failure) {
        disabled = true;
        try {
            for (BarState state : bars.values()) {
                teardown(state);
            }
        } catch (Throwable ignored) {
            // Already failing soft; a stuck bar must not chain.
        }
        bars.clear();
        if (!failureLogged) {
            failureLogged = true;
            log.warning("Damage feedback disabled for this boot (failure at "
                    + where + ": " + failure + ") - the fights themselves "
                    + "are unaffected; feedback returns next boot.");
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
