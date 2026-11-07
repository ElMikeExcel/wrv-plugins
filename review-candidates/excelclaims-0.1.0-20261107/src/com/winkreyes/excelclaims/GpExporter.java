package com.winkreyes.excelclaims;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;

/**
 * ExcelClaims — the rollback exporter: writes the current store back
 * out in the old flat-file shapes, so returning to GriefPrevention is
 * a file copy, not a rescue operation. Writes exactly the keys the
 * old store writes (nothing foreign), into a target folder the
 * operator chooses. Two documented degradations on this path, both
 * bounded and named in BUILD-RECORD: view-only (Inventory) trust
 * returns as ordinary Container trust (the old model has no claim
 * field for it in these files), and ExcelClaims bookkeeping keys
 * (creation/modification times, chest-claim flag) are simply not
 * written. Land, owners, subdivisions, and the four trust lists are
 * lossless. Excel — Iris / Mike.
 */
public final class GpExporter {

    public static final class ExportResult {
        public int claims;
        public int players;
    }

    public ExportResult exportTo(Collection<ClaimRecord> claims, Collection<PlayerRecord> players,
                                 long nextClaimId, File targetDir) throws IOException {
        ExportResult result = new ExportResult();
        File claimDir = new File(targetDir, "ClaimData");
        File playerDir = new File(targetDir, "PlayerData");
        claimDir.mkdirs();
        playerDir.mkdirs();
        for (ClaimRecord r : claims) {
            YamlConfiguration y = new YamlConfiguration();
            y.set("Lesser Boundary Corner", r.world + ";" + r.lx + ";" + r.ly + ";" + r.lz);
            y.set("Greater Boundary Corner", r.world + ";" + r.gx + ";" + r.gy + ";" + r.gz);
            y.set("Owner", r.owner == null ? "" : r.owner.toString());
            y.set("Builders", new ArrayList<>(r.builders));
            y.set("Containers", new ArrayList<>(r.containers));
            y.set("Accessors", new ArrayList<>(r.accessors));
            y.set("Managers", new ArrayList<>(r.managers));
            y.set("Parent Claim ID", r.parentId);
            y.set("inheritNothing", r.inheritNothing);
            y.save(new File(claimDir, r.id + ".yml"));
            result.claims++;
        }
        Files.writeString(new File(claimDir, "_nextClaimID").toPath(),
                Long.toString(nextClaimId), StandardCharsets.UTF_8);
        for (PlayerRecord pr : players) {
            Files.writeString(new File(playerDir, pr.id.toString()).toPath(),
                    "\n" + pr.accrued + "\n" + pr.bonus + "\n", StandardCharsets.UTF_8);
            result.players++;
        }
        return result;
    }
}

// ExcelClaims — Iris / Mike, for Wink Reyes Valley
