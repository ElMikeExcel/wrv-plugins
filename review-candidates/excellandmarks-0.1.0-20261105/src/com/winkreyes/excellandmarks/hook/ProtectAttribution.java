package com.winkreyes.excellandmarks.hook;

import com.winkreyes.excellandmarks.core.Attribution;
import com.winkreyes.excellandmarks.core.Spot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * Read-only ExcelProtect blocks-stream reader (SPEC §5). Protect keeps
 * per-day files blocks-YYYY-MM-DD.jsonl (+ .gz after roll) in its data
 * folder. Missing folder/files = attribution unavailable = empty tally;
 * the sweep logs that and proceeds (fail-soft, the accepted limit).
 */
public final class ProtectAttribution {

    private final File logsDir;
    private final Logger log;

    public ProtectAttribution(File baseDir, String configuredDir, Logger log) {
        // configuredDir is relative to baseDir — the caller passes this
        // plugin's own data folder, so "../ExcelProtect" names the sibling.
        this.logsDir = new File(baseDir, configuredDir);
        this.log = log;
    }

    public boolean available() {
        return logsDir.isDirectory();
    }

    public File logsDir() { return logsDir; }

    /** Latest-action-per-position map across every blocks day file, oldest first. */
    public Map<String, Attribution.Entry> latest() {
        List<Attribution.Entry> all = new ArrayList<>();
        File[] files = logsDir.listFiles((d, n) -> n.startsWith("blocks-") && (n.endsWith(".jsonl") || n.endsWith(".jsonl.gz")));
        if (files == null || files.length == 0) return Map.of();
        java.util.Arrays.sort(files, (a, b) -> a.getName().compareTo(b.getName()));
        for (File f : files) {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(
                    f.getName().endsWith(".gz")
                            ? new GZIPInputStream(new FileInputStream(f))
                            : new FileInputStream(f), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    Attribution.Entry e = Attribution.parseLine(line);
                    if (e != null) all.add(e);
                }
            } catch (Throwable t) {
                log.warning("ExcelLandmarks: could not read Protect blocks file " + f.getName() + " (" + t + ")");
            }
        }
        return Attribution.latestPerPosition(all);
    }

    public Attribution.Tally tallyFor(Spot spot, UUID renter, int yBelow, int yAbove) {
        if (!available()) {
            log.info("ExcelLandmarks: ExcelProtect attribution unavailable — materials tally empty for spot " + spot.id);
            return new Attribution.Tally();
        }
        return Attribution.tally(latest(), spot, renter, yBelow, yAbove);
    }
}
