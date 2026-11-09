package com.winkreyes.exceldetails;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Writes meter lines onto scoreboards under the three placement
 * constraints from DETAILS-PLACEMENT-DESK-VERIFICATION.md:
 *  (1) never setScoreboard a private board while a foreign (EM)
 *      managed board with a sidebar objective is in place — piggyback
 *      writes go into that board's objective;
 *  (2) piggyback lines take NEGATIVE scores (EM owns 0..n-1);
 *  (3) the combined box self-caps: meter lines <= 15 - EM lines.
 * Outside that context the meter owns a per-player board alone.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class SidebarRenderer {

    private static final String OWN_OBJECTIVE = "exceldetails";
    private static final String HEX = "0123456789abcdef";

    private record Applied(Scoreboard board, int lineCount) {}

    private final SegmentTracker tracker;
    private final PlayerPrefs prefs;
    private final int maxRows;
    private final Map<UUID, Scoreboard> ownBoards = new HashMap<>();
    private final Map<UUID, Applied> applied = new HashMap<>();

    public SidebarRenderer(SegmentTracker tracker, PlayerPrefs prefs, int maxRows) {
        this.tracker = tracker;
        this.prefs = prefs;
        this.maxRows = maxRows;
    }

    /** Unique, EM-dialect-distinct entry string for meter line i. */
    static String entryFor(int i) {
        return "§7§d§" + HEX.charAt((i >> 4) & 0xF) + "§" + HEX.charAt(i & 0xF);
    }

    static boolean isMeterEntry(String entry) {
        return entry != null && entry.startsWith("§7§d§");
    }

    public void render(Player player, long nowMs) {
        UUID id = player.getUniqueId();
        // 0.2.0: meter off OR every section unticked = no box at all
        // (segments keep tracking; only the display hands back).
        if (!prefs.isEnabled(id) || prefs.shownWindows(id).isEmpty()) {
            cleanup(player);
            return;
        }
        Scoreboard board = player.getScoreboard();
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        Scoreboard own = ownBoards.get(id);
        boolean foreign = board != main && board != own;

        if (foreign) {
            Objective objective = board.getObjective(DisplaySlot.SIDEBAR);
            if (objective != null) {
                renderPiggyback(player, board, objective, nowMs);
                return;
            }
            // Foreign board without a sidebar objective is not EM's
            // party box; fall through to the standalone board.
        }
        renderStandalone(player, nowMs);
    }

    private void renderPiggyback(Player player, Scoreboard board, Objective objective, long nowMs) {
        UUID id = player.getUniqueId();
        detachIfBoardChanged(id, board);
        int foreignLines = 0;
        for (String entry : board.getEntries()) {
            if (isMeterEntry(entry)) continue;
            try {
                if (objective.getScore(entry).getScore() >= 0) foreignLines++;
            } catch (IllegalArgumentException ignored) {
                // entry belongs to another objective on this board
            }
        }
        int budget = MeterMath.meterBudget(foreignLines);
        List<String> lines = linesFor(player, budget, nowMs);
        apply(board, objective, lines, true);
        applied.put(id, new Applied(board, lines.size()));
    }

    private void renderStandalone(Player player, long nowMs) {
        UUID id = player.getUniqueId();
        // No segment data yet (or the segment just emptied): no box —
        // the meter auto-opens with the first tracked event instead of
        // idling as an empty frame on a board we assigned.
        List<String> lines = linesFor(player, MeterMath.SIDEBAR_CEILING, nowMs);
        if (lines.isEmpty()) {
            cleanup(player);
            return;
        }
        Scoreboard own = ownBoards.computeIfAbsent(id, k -> {
            Scoreboard b = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective o = b.registerNewObjective(OWN_OBJECTIVE, "dummy", "§6§lDetails!");
            o.setDisplaySlot(DisplaySlot.SIDEBAR);
            return b;
        });
        detachIfBoardChanged(id, own);
        if (player.getScoreboard() != own) {
            player.setScoreboard(own);
        }
        Objective objective = own.getObjective(OWN_OBJECTIVE);
        apply(own, objective, lines, false);
        applied.put(id, new Applied(own, lines.size()));
    }

    /**
     * 0.2.0 mix: one section per ticked metric, canonical order,
     * composed under the caller's budget (piggyback: 15 − EM lines;
     * standalone: the 15-line ceiling). maxRows caps each section.
     */
    private List<String> linesFor(Player player, int budget, long nowMs) {
        UUID id = player.getUniqueId();
        List<LineBuilder.Section> sections = new java.util.ArrayList<>();
        for (CombatStats.Window w : CombatStats.Window.values()) {
            if (!prefs.shows(id, w)) continue;
            List<SegmentTracker.Row> rows = tracker.ranked(w);
            double total = rows.stream().mapToDouble(SegmentTracker.Row::amount).sum();
            sections.add(new LineBuilder.Section(w, rows, total));
        }
        return LineBuilder.buildSections(sections, id, tracker.elapsedMs(nowMs), maxRows, budget);
    }

    private void apply(Scoreboard board, Objective objective, List<String> lines, boolean negative) {
        Applied prev = null; // line-count bookkeeping handled by caller map via detach/apply below
        for (int i = 0; i < lines.size(); i++) {
            String entry = entryFor(i);
            Team team = board.getTeam("edt" + i);
            if (team == null) team = board.registerNewTeam("edt" + i);
            if (!team.hasEntry(entry)) team.addEntry(entry);
            if (!lines.get(i).equals(team.getPrefix())) team.setPrefix(lines.get(i));
            int want = negative ? -1 - i : lines.size() - i;
            if (objective.getScore(entry).getScore() != want) {
                objective.getScore(entry).setScore(want);
            }
        }
        // Remove lines beyond the new count (shrink case).
        for (int i = lines.size(); i < MAX_TRACKED; i++) {
            String entry = entryFor(i);
            Team team = board.getTeam("edt" + i);
            if (team == null && !board.getEntries().contains(entry)) break;
            board.resetScores(entry);
            if (team != null) team.unregister();
        }
    }

    private static final int MAX_TRACKED = 15;

    /** If our lines live on a different board than the current one, strip them there first. */
    private void detachIfBoardChanged(UUID id, Scoreboard current) {
        Applied prev = applied.get(id);
        if (prev != null && prev.board() != current) {
            strip(prev.board());
            applied.remove(id);
        }
    }

    private void strip(Scoreboard board) {
        for (int i = 0; i < MAX_TRACKED; i++) {
            String entry = entryFor(i);
            Team team = board.getTeam("edt" + i);
            if (team == null && !board.getEntries().contains(entry)) continue;
            board.resetScores(entry);
            if (team != null) team.unregister();
        }
    }

    /** Remove every meter line the player was shown; restore the main board if we own theirs. */
    public void cleanup(Player player) {
        UUID id = player.getUniqueId();
        Applied prev = applied.remove(id);
        if (prev != null) strip(prev.board());
        Scoreboard own = ownBoards.get(id);
        if (own != null && player.getScoreboard() == own) {
            strip(own);
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public void forget(Player player) {
        cleanup(player);
        ownBoards.remove(player.getUniqueId());
        applied.remove(player.getUniqueId());
    }
}
