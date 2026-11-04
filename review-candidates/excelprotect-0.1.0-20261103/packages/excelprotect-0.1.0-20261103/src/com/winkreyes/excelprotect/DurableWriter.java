package com.winkreyes.excelprotect;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Excel — durable daily-file writer (0.3.0). Fixes the confirmed 0.2.0 defect (Moon audit,
 * AUDITLOG-020-DISPOSITION): the old writer drained its queue into a throwaway buffer before
 * appending, so one IOException silently discarded the whole batch.
 *
 * Here, queued lines are only removed after they are durably written. Each line is flushed
 * individually so a mid-batch failure commits an exact prefix and retains the unwritten
 * remainder, in original order, for the next flush. Failures are counted and the last error is
 * kept for /auditlog status. When the queue exceeds its cap, incoming lines are dropped and
 * counted loudly (never silently).
 */
public final class DurableWriter {
    private record Queued(String day, String line) {}

    private final File dir;
    private final String prefix, suffix, header;
    private final int cap;
    private final Deque<Queued> q = new ArrayDeque<>();
    private long dropped, failedBatches, retainedAfterFailure;
    private String lastError;

    public DurableWriter(File dir, String prefix, String suffix, String header, int cap) {
        this.dir = dir; this.prefix = prefix; this.suffix = suffix; this.header = header;
        this.cap = Math.max(100, cap);
    }

    public synchronized void add(String day, String line) {
        if (q.size() >= cap) { dropped++; return; }
        q.addLast(new Queued(day, line));
    }

    public synchronized int pending() { return q.size(); }
    public synchronized long dropped() { return dropped; }
    public synchronized long failedBatches() { return failedBatches; }
    public synchronized long retainedAfterFailure() { return retainedAfterFailure; }
    public synchronized String lastError() { return lastError; }

    public File fileForDay(String day) { return new File(dir, prefix + day + suffix); }

    /**
     * Write every queued line, grouped into per-day files in queue order. On failure, the
     * unwritten remainder (including the failed line) stays queued and an IOException is thrown
     * after state is recorded; already-written lines are never re-queued (no duplicates).
     */
    public synchronized void flush() throws IOException {
        if (q.isEmpty()) return;
        dir.mkdirs();
        List<Queued> batch = new ArrayList<>(q);
        int committed = 0;
        try {
            int i = 0;
            while (i < batch.size()) {
                String day = batch.get(i).day();
                List<String> lines = new ArrayList<>();
                int j = i;
                while (j < batch.size() && batch.get(j).day().equals(day)) { lines.add(batch.get(j).line()); j++; }
                writeGroup(fileForDay(day), lines);
                committed = j;
                i = j;
            }
        } catch (IOException e) {
            for (int k = 0; k < committed; k++) q.pollFirst();
            failedBatches++;
            retainedAfterFailure = q.size();
            lastError = String.valueOf(e);
            throw e;
        }
        q.clear();
    }

    private void writeGroup(File f, List<String> lines) throws IOException {
        boolean fresh = !f.exists() || f.length() == 0;
        try (BufferedWriter w = Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (fresh && header != null) { w.write(header); w.newLine(); w.flush(); }
            for (String l : lines) { w.write(l); w.newLine(); w.flush(); }
        }
    }
}
