package com.winkreyes.excelguide;

import com.winkreyes.excelguide.core.FactsView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * Live fact sheet for the AI fallback (0.2.0), implementing the
 * privacy line structurally: {@link #askerFacts} takes ONLY the
 * asker's UUID and reads only that player's numbers; {@link
 * #publicFacts} reads only what is already public in the world (seal
 * progress, the Land Claimed board's leaders). There is no code path
 * that fetches a second player's private numbers.
 *
 * GriefPrevention is read through {@link GpClaims} - the shared
 * reflective read (extracted in 0.2.2, live-proven here since
 * 0.2.0) against the installed GriefPrevention 16.18.7 jar. Any
 * failure omits the claim lines instead of guessing. The land
 * leaders are computed from GP the same way the board computes
 * them (summed area of a player's own top-level claims in
 * claims-enabled worlds) and are cached for five minutes: a board
 * snapshot, not a live feed.
 */
public final class BukkitFacts implements FactsView {

    private final BukkitStats stats;
    private final GpClaims gp = new GpClaims();

    private List<String> landLeadersCache;
    private long landLeadersAt;

    public BukkitFacts(BukkitStats stats) {
        this.stats = stats;
    }

    // ---------- the asker's own facts ----------

    @Override
    public List<String> askerFacts(UUID asker) {
        List<String> out = new ArrayList<>();
        long[] claims = gp.claimBlocks(asker);
        if (claims != null) {
            out.add("Claim blocks: accrued " + claims[0] + ", bonus " + claims[1]
                    + ", used by claims " + claims[2] + ", remaining " + claims[3]);
        }
        addIfPresent(out, stats.mcMMOSummary(asker));
        addIfPresent(out, stats.jobsSummary(asker));
        addIfPresent(out, stats.moneySummary(asker));
        addIfPresent(out, stats.playtimeSummary(asker));
        if (stats.sealsBroken() >= 1) {
            String magic = stats.magicStanding(asker);
            if (magic != null && !magic.isBlank()) {
                out.add(magic.startsWith("Magic:") ? magic : "Magic: " + magic);
            }
        }
        return out;
    }

    // ---------- public facts ----------

    @Override
    public List<String> publicFacts() {
        List<String> out = new ArrayList<>();
        out.add("Guild seals broken: " + stats.sealsBroken() + " of " + stats.sealsTotal());
        out.addAll(landLeaders());
        return out;
    }

    /** Top of the Land Claimed board: name + blocks claimed, biggest first. Cached 5 min. */
    private List<String> landLeaders() {
        long now = System.currentTimeMillis();
        if (landLeadersCache != null && now - landLeadersAt < 300_000L) {
            return landLeadersCache;
        }
        List<String> out = new ArrayList<>();
        if (gp.available()) {
            try {
                List<String[]> rows = new ArrayList<>(); // name, claimed
                for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                    if (!op.hasPlayedBefore() || op.getName() == null) {
                        continue;
                    }
                    long[] c = gp.claimBlocks(op.getUniqueId());
                    if (c != null && c[2] > 0) {
                        rows.add(new String[]{op.getName(), String.valueOf(c[2])});
                    }
                }
                rows.sort(Comparator.comparingLong(r -> -Long.parseLong(r[1])));
                if (!rows.isEmpty()) {
                    StringBuilder line = new StringBuilder("Land Claimed board leaders: ");
                    for (int i = 0; i < Math.min(3, rows.size()); i++) {
                        if (i > 0) {
                            line.append("; ");
                        }
                        line.append(rows.get(i)[0]).append(' ').append(rows.get(i)[1])
                                .append(" blocks claimed");
                    }
                    out.add(line.toString());
                }
            } catch (Throwable ignored) {
                // The board line is a garnish; its absence never blocks an answer.
            }
        }
        landLeadersCache = out;
        landLeadersAt = now;
        return out;
    }

    private static void addIfPresent(List<String> out, String line) {
        if (line != null && !line.isBlank()) {
            out.add(line);
        }
    }
}
