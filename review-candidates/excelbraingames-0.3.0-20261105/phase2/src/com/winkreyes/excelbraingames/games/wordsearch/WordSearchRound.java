package com.winkreyes.excelbraingames.games.wordsearch;

import com.winkreyes.excelbraingames.core.Dir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * One Word Search round: generation, the two-tap checker, and save/resume
 * serialization. Pure logic (SPEC §3) — the Bukkit layer renders it.
 *
 * The checker validates taps against the stored PLACEMENTS only (§3.1):
 * accidental words formed by filler letters neither count nor break anything
 * — tapping one is a plain NO_MATCH. Accepted behavior, covered by the
 * property tests.
 *
 * Cells are indexed row-major (idx = row * cols + col). Cells whose GUI slot
 * carries a control button are VOID: words never cross them and they hold no
 * letter. The Bukkit layer computes the mask from the slot layout; the logic
 * here just honors it.
 */
public final class WordSearchRound {

    /** Tier shape the generator needs (values come from config). */
    public record Spec(int cols, int rows, int wordCount, int minLen, int maxLen, Set<Dir> directions) {}

    public static final class Placement {
        public final String word;
        public final int r, c, dr, dc;

        public Placement(String word, int r, int c, int dr, int dc) {
            this.word = word; this.r = r; this.c = c; this.dr = dr; this.dc = dc;
        }

        public int len() { return word.length(); }
        public int endR() { return r + dr * (len() - 1); }
        public int endC() { return c + dc * (len() - 1); }
        public int startIdx(int cols) { return r * cols + c; }
        public int endIdx(int cols) { return endR() * cols + endC(); }
        public int idxAt(int cols, int i) { return (r + dr * i) * cols + (c + dc * i); }
    }

    public enum TapResult { PENDING_SET, CANCELLED, FOUND, ALREADY_FOUND, NO_MATCH, NOT_A_LINE, IGNORED }

    private final String tierId, themeId;
    private final int cols, rows;
    private final char[] grid;
    private final boolean[] voids;
    private final List<Placement> placements;
    private final Set<Integer> found = new HashSet<>();
    private int pending = -1;
    private final long startedMillis;
    private boolean paid;

    private WordSearchRound(String tierId, String themeId, int cols, int rows, char[] grid,
                            boolean[] voids, List<Placement> placements, long startedMillis) {
        this.tierId = tierId; this.themeId = themeId;
        this.cols = cols; this.rows = rows;
        this.grid = grid; this.voids = voids;
        this.placements = placements;
        this.startedMillis = startedMillis;
    }

    // ---------------------------------------------------------- generation

    /**
     * Deal a fresh round. Words are drawn from the pool (filtered to the
     * tier's length band); each word gets a bounded number of placement
     * tries, and a word that won't fit is swapped for the next pool word, so
     * a round always starts on time (SPEC §3.1). If the pool runs dry the
     * round simply has fewer words — callers log it; shipped pools are sized
     * so this never happens.
     */
    public static WordSearchRound generate(String tierId, String themeId, Spec spec,
                                           List<String> pool, boolean[] voids, Random rng,
                                           long startedMillis) {
        int cols = spec.cols(), rows = spec.rows();
        char[] grid = new char[cols * rows];
        List<String> candidates = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String w : pool) {
            if (w == null) continue;
            String word = w.trim().toUpperCase();
            if (word.length() < spec.minLen() || word.length() > spec.maxLen()) continue;
            if (!word.chars().allMatch(ch -> ch >= 'A' && ch <= 'Z')) continue;
            if (seen.add(word)) candidates.add(word);
        }
        List<Dir> dirs = new ArrayList<>(spec.directions());
        // Whole-board deals (subset biased toward shorter words, jittered so
        // long words rotate in across rounds). A deal that fits every word
        // stops the loop; the best deal is kept. A deal landing within two
        // words of full gets completion attacks from its own knot before
        // being discarded, and the best knot overall gets a final round of
        // attacks at the end.
        List<Placement> bestPlaced = List.of();
        int boardAttempts = 600;
        for (int attempt = 0; attempt < boardAttempts && bestPlaced.size() < spec.wordCount(); attempt++) {
            char[] trial = new char[cols * rows];
            List<Placement> placed = placeAll(trial, voids, cols, rows,
                    dealSubsetBudget(candidates, spec.wordCount(), voids, rng), dirs, spec.wordCount(),
                    new ArrayList<>(), rng);
            if (placed.size() >= spec.wordCount() - 2 && placed.size() < spec.wordCount()) {
                placed = completionAttacks(placed, candidates, voids, cols, rows, dirs, spec.wordCount(), rng, 40);
            }
            if (placed.size() > bestPlaced.size()) bestPlaced = placed;
        }
        if (bestPlaced.size() == spec.wordCount() - 1) {
            bestPlaced = swapAttacks(bestPlaced, candidates, voids, cols, rows, dirs, spec.wordCount(), rng, 300);
        }
        if (bestPlaced.size() < spec.wordCount()) {
            bestPlaced = completionAttacks(bestPlaced, candidates, voids, cols, rows, dirs, spec.wordCount(), rng, 300);
        }
        grid = new char[cols * rows];
        for (Placement p : bestPlaced) write(grid, cols, p.word, p.r, p.c, Dir.ofDelta(p.dr, p.dc));
        List<Placement> placed = bestPlaced;
        for (int i = 0; i < grid.length; i++) {
            if (grid[i] == 0 && !voids[i]) grid[i] = (char) ('A' + rng.nextInt(26));
        }
        return new WordSearchRound(tierId, themeId, cols, rows, grid, voids, placed, startedMillis);
    }

    /**
     * Finisher for a knot sitting at wordCount - 1: evict one placed word,
     * thread the missing words in, then try to re-place the evicted word.
     * Rotating the eviction over the knot breaks the geometry that blocked
     * the last word. Returns the best result seen (never worse than input).
     */
    private static List<Placement> swapAttacks(List<Placement> knot, List<String> candidates,
                                               boolean[] voids, int cols, int rows,
                                               List<Dir> dirs, int wordCount, Random rng, int attacks) {
        List<Placement> best = knot;
        for (int a = 0; a < attacks && best.size() < wordCount; a++) {
            List<Placement> base = new ArrayList<>(best);
            Placement evicted = base.remove(rng.nextInt(base.size()));
            char[] trial = new char[cols * rows];
            for (Placement p : base) write(trial, cols, p.word, p.r, p.c, Dir.ofDelta(p.dr, p.dc));
            Set<String> used = new HashSet<>();
            for (Placement p : base) used.add(p.word);
            List<String> rest = new ArrayList<>();
            for (String w : candidates) if (!used.contains(w)) rest.add(w);
            Collections.shuffle(rest, rng);
            rest.sort((x, y) -> Integer.compare(y.length(), x.length()));
            List<Placement> placed = new ArrayList<>(base);
            for (String word : rest) {
                if (placed.size() >= wordCount) break;
                int[] spot = scanSpot(trial, voids, cols, rows, word, dirs, false, rng);
                if (spot != null) {
                    Dir d = dirs.get(spot[2]);
                    write(trial, cols, word, spot[0], spot[1], d);
                    placed.add(new Placement(word, spot[0], spot[1], d.dr, d.dc));
                }
            }
            if (placed.size() > best.size()) best = placed;
        }
        return best;
    }

    /**
     * Re-attack the missing words against an existing knot: rebuild the
     * knot's grid, then scan-place the remaining candidates with the
     * crossing rule relaxed — the last word often threads an empty line
     * through the knot's gaps. Returns the best result seen (never worse
     * than the knot it was given).
     */
    private static List<Placement> completionAttacks(List<Placement> knot, List<String> candidates,
                                                     boolean[] voids, int cols, int rows,
                                                     List<Dir> dirs, int wordCount, Random rng, int attacks) {
        List<Placement> best = knot;
        for (int a = 0; a < attacks && best.size() < wordCount; a++) {
            char[] trial = new char[cols * rows];
            for (Placement p : best) write(trial, cols, p.word, p.r, p.c, Dir.ofDelta(p.dr, p.dc));
            Set<String> used = new HashSet<>();
            for (Placement p : best) used.add(p.word);
            List<String> rest = new ArrayList<>();
            for (String w : candidates) if (!used.contains(w)) rest.add(w);
            Collections.shuffle(rest, rng);
            List<Placement> placed = new ArrayList<>(best);
            for (String word : rest) {
                if (placed.size() >= wordCount) break;
                int[] spot = scanSpot(trial, voids, cols, rows, word, dirs, false, rng);
                if (spot != null) {
                    Dir d = dirs.get(spot[2]);
                    write(trial, cols, word, spot[0], spot[1], d);
                    placed.add(new Placement(word, spot[0], spot[1], d.dr, d.dc));
                }
            }
            if (placed.size() > best.size()) best = placed;
        }
        return best;
    }

    /**
     * A deal's working set under a LETTER BUDGET: the board's usable cells
     * times ~1.2. A deal whose words total far more letters than that cannot
     * physically fit no matter how the words cross (measured: a 51-letter
     * knot on 48 cells leaves no line for the 10th word), so the subset is
     * built shortest-first (jittered) and capped by the budget once the
     * tier's word count is reached; swap candidates beyond the count are
     * added only while the budget allows.
     */
    private static List<String> dealSubsetBudget(List<String> candidates, int wordCount,
                                                 boolean[] voids, Random rng) {
        int usable = 0;
        for (boolean v : voids) if (!v) usable++;
        int budget = usable * 6 / 5;
        List<String> sorted = new ArrayList<>(candidates);
        // Jitter keys are drawn ONCE per word before sorting. A comparator
        // that re-rolls rng per comparison violates the sort contract, and
        // TimSort throws on candidate lists of 32+ — latent until the
        // Nov 4, 2026 list expansion pushed pools past that size.
        Map<String, Integer> jitter = new LinkedHashMap<>();
        for (String w : sorted) jitter.put(w, w.length() + rng.nextInt(3));
        sorted.sort(Comparator.comparingInt(jitter::get));
        List<String> out = new ArrayList<>();
        int total = 0;
        for (String w : sorted) {
            if (out.size() >= wordCount + 3) break;
            if (out.size() >= wordCount && total + w.length() > budget) continue;
            out.add(w);
            total += w.length();
        }
        out.sort((a, b) -> Integer.compare(b.length(), a.length())); // placement order: longest first
        return out;
    }

    /** A deal's working set: length-biased with jitter, capped at wordCount + slack. */
    private static List<String> dealSubset(List<String> candidates, int wordCount, Random rng) {
        List<String> subset = new ArrayList<>(candidates);
        Map<String, Integer> jitter = new LinkedHashMap<>();
        for (String w : subset) jitter.put(w, w.length() + rng.nextInt(3));
        subset.sort(Comparator.comparingInt(jitter::get));
        subset = new ArrayList<>(subset.subList(0, Math.min(subset.size(), wordCount + 3)));
        subset.sort((a, b) -> Integer.compare(b.length(), a.length())); // stable: jittered ties stay random
        return subset;
    }

    /**
     * One board deal. Words are placed by ATTACHMENT (the crossword trick):
     * each new word anchors onto a letter of an already-placed word, so the
     * board grows as a crossing knot by construction instead of tiling
     * space. If no attachment fits, a full scan takes the max-overlap spot;
     * from the third word on, a scan spot must cross (score >= 1) or the
     * word is skipped for this deal — sprawl eats the empty straight lines
     * every later word needs (measured repeatedly during development:
     * tiling/sprawl strategies saturate a 9x6 board at 8-9 words, the
     * attachment knot reaches the dense tier's 10).
     */
    private static List<Placement> placeAll(char[] grid, boolean[] voids, int cols, int rows,
                                            List<String> order, List<Dir> dirs, int wordCount,
                                            List<Placement> placed, Random rng) {
        for (String word : order) {
            if (placed.size() >= wordCount) break;
            int[] spot = attachSpot(grid, voids, cols, rows, word, dirs, placed, rng);
            if (spot == null) {
                spot = scanSpot(grid, voids, cols, rows, word, dirs, placed.size() >= 2, rng);
            }
            if (spot == null) continue; // cannot fit this deal; swap to the next word
            Dir d = dirs.get(spot[2]);
            write(grid, cols, word, spot[0], spot[1], d);
            placed.add(new Placement(word, spot[0], spot[1], d.dr, d.dc));
        }
        return placed;
    }

    /**
     * Find a spot for the word anchored on an existing placement's letter:
     * pick a placed word, one of its cells, and a matching letter index in
     * the new word, then a direction — the new word passes through that
     * cell. Best overlap over the sampled anchors wins. Null when the board
     * is empty or no sampled anchor produces a legal spot.
     */
    private static int[] attachSpot(char[] grid, boolean[] voids, int cols, int rows,
                                    String word, List<Dir> dirs, List<Placement> placed, Random rng) {
        if (placed.isEmpty()) return null;
        int[] best = null;
        int bestScore = 0;
        int samples = 400;
        for (int t = 0; t < samples; t++) {
            Placement p = placed.get(rng.nextInt(placed.size()));
            int k = rng.nextInt(p.len());
            char letter = p.word.charAt(k);
            int cellR = p.r + p.dr * k, cellC = p.c + p.dc * k;
            // candidate indexes of that letter in the new word
            int matches = 0;
            for (int i = 0; i < word.length(); i++) if (word.charAt(i) == letter) matches++;
            if (matches == 0) continue;
            int pick = rng.nextInt(matches), idx = -1;
            for (int i = 0; i < word.length(); i++) {
                if (word.charAt(i) == letter && pick-- == 0) { idx = i; break; }
            }
            Dir d = dirs.get(rng.nextInt(dirs.size()));
            int r0 = cellR - d.dr * idx, c0 = cellC - d.dc * idx;
            int score = overlapScore(grid, voids, cols, rows, word, r0, c0, d);
            if (score > bestScore) {
                bestScore = score;
                best = new int[]{r0, c0, dirs.indexOf(d)};
                if (score >= 3) break; // a triple crossing is plenty; stop sampling
            }
        }
        return best;
    }

    /**
     * Full scan for the word's best spot: max letters shared with the board,
     * ties broken randomly. When mustCross is set, spots sharing nothing are
     * not eligible. Returns {r, c, dirIndex} or null.
     */
    private static int[] scanSpot(char[] grid, boolean[] voids, int cols, int rows,
                                  String word, List<Dir> dirs, boolean mustCross, Random rng) {
        int bestScore = -1;
        List<int[]> best = new ArrayList<>();
        for (Dir d : dirs) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int score = overlapScore(grid, voids, cols, rows, word, r, c, d);
                    if (score < 0) continue;
                    if (mustCross && score == 0) continue;
                    if (score > bestScore) { bestScore = score; best.clear(); }
                    if (score == bestScore) best.add(new int[]{r, c, dirs.indexOf(d)});
                }
            }
        }
        if (best.isEmpty()) return null;
        return best.get(rng.nextInt(best.size()));
    }

    /** Letters shared with the current board if the word goes here, or -1 if it cannot go here. */
    private static int overlapScore(char[] grid, boolean[] voids, int cols, int rows,
                                    String word, int r, int c, Dir d) {
        int score = 0;
        for (int i = 0; i < word.length(); i++) {
            int rr = r + d.dr * i, cc = c + d.dc * i;
            if (rr < 0 || rr >= rows || cc < 0 || cc >= cols) return -1;
            int idx = rr * cols + cc;
            if (voids[idx]) return -1;
            char existing = grid[idx];
            if (existing != 0) {
                if (existing != word.charAt(i)) return -1;
                score++;
            }
        }
        return score;
    }

    private static boolean canPlace(char[] grid, boolean[] voids, int cols, int rows,
                                    String word, int r, int c, Dir d) {
        for (int i = 0; i < word.length(); i++) {
            int rr = r + d.dr * i, cc = c + d.dc * i;
            if (rr < 0 || rr >= rows || cc < 0 || cc >= cols) return false;
            int idx = rr * cols + cc;
            if (voids[idx]) return false;
            char existing = grid[idx];
            if (existing != 0 && existing != word.charAt(i)) return false;
        }
        return true;
    }

    private static void write(char[] grid, int cols, String word, int r, int c, Dir d) {
        for (int i = 0; i < word.length(); i++) {
            grid[(r + d.dr * i) * cols + (c + d.dc * i)] = word.charAt(i);
        }
    }

    // ---------------------------------------------------------- the checker

    /** One tap of the two-tap "circling" interaction (SPEC §3.3). */
    public TapResult tap(int idx) {
        if (idx < 0 || idx >= grid.length || voids[idx]) return TapResult.IGNORED;
        if (pending == idx) { pending = -1; return TapResult.CANCELLED; }
        if (pending < 0) { pending = idx; return TapResult.PENDING_SET; }
        int r1 = pending / cols, c1 = pending % cols;
        int r2 = idx / cols, c2 = idx % cols;
        pending = -1;
        int dr = r2 - r1, dc = c2 - c1;
        boolean straight = dr == 0 || dc == 0 || Math.abs(dr) == Math.abs(dc);
        if (!straight) return TapResult.NOT_A_LINE;
        for (int i = 0; i < placements.size(); i++) {
            Placement p = placements.get(i);
            boolean fwd = p.startIdx(cols) == r1 * cols + c1 && p.endIdx(cols) == idx;
            boolean rev = p.endIdx(cols) == r1 * cols + c1 && p.startIdx(cols) == idx;
            if (fwd || rev) {
                return found.add(i) ? TapResult.FOUND : TapResult.ALREADY_FOUND;
            }
        }
        return TapResult.NO_MATCH;
    }

    // ---------------------------------------------------------- accessors

    public String tierId() { return tierId; }
    public String themeId() { return themeId; }
    public int cols() { return cols; }
    public int rows() { return rows; }
    public char letterAt(int idx) { return grid[idx]; }
    public boolean isVoid(int idx) { return voids[idx]; }
    public List<Placement> placements() { return Collections.unmodifiableList(placements); }
    public Set<Integer> found() { return Collections.unmodifiableSet(found); }
    public int pending() { return pending; }
    public long startedMillis() { return startedMillis; }
    public boolean paid() { return paid; }
    public void markPaid() { this.paid = true; }
    public boolean isComplete() { return !placements.isEmpty() && found.size() == placements.size(); }

    public boolean cellFound(int idx) {
        for (int fi : found) {
            Placement p = placements.get(fi);
            for (int i = 0; i < p.len(); i++) if (p.idxAt(cols, i) == idx) return true;
        }
        return false;
    }

    /** The letters along a placement, read off the grid (property-test proof of findability). */
    public String readPlacement(int i) {
        Placement p = placements.get(i);
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < p.len(); k++) sb.append(grid[p.idxAt(cols, k)]);
        return sb.toString();
    }

    // ---------------------------------------------------------- save / resume

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tier", tierId);
        m.put("theme", themeId);
        m.put("cols", cols);
        m.put("rows", rows);
        StringBuilder g = new StringBuilder();
        for (char ch : grid) g.append(ch == 0 ? '.' : ch);
        m.put("grid", g.toString());
        StringBuilder v = new StringBuilder();
        for (boolean b : voids) v.append(b ? '1' : '0');
        m.put("voids", v.toString());
        List<String> ps = new ArrayList<>();
        for (Placement p : placements) ps.add(p.word + ":" + p.r + "," + p.c + "," + p.dr + "," + p.dc);
        m.put("placements", ps);
        List<Integer> f = new ArrayList<>(found);
        Collections.sort(f);
        m.put("found", f);
        m.put("started", startedMillis);
        m.put("paid", paid);
        return m;
    }

    public static WordSearchRound fromMap(Map<String, Object> m) {
        String tier = (String) m.get("tier");
        String theme = (String) m.get("theme");
        int cols = ((Number) m.get("cols")).intValue();
        int rows = ((Number) m.get("rows")).intValue();
        String g = (String) m.get("grid");
        char[] grid = new char[cols * rows];
        for (int i = 0; i < grid.length && i < g.length(); i++) grid[i] = g.charAt(i) == '.' ? 0 : g.charAt(i);
        String v = (String) m.getOrDefault("voids", "");
        boolean[] voids = new boolean[cols * rows];
        for (int i = 0; i < voids.length && i < v.length(); i++) voids[i] = v.charAt(i) == '1';
        List<Placement> ps = new ArrayList<>();
        Object raw = m.get("placements");
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                String[] a = String.valueOf(o).split(":");
                String[] b = a[1].split(",");
                ps.add(new Placement(a[0], Integer.parseInt(b[0]), Integer.parseInt(b[1]),
                        Integer.parseInt(b[2]), Integer.parseInt(b[3])));
            }
        }
        WordSearchRound round = new WordSearchRound(tier, theme, cols, rows, grid, voids, ps,
                ((Number) m.getOrDefault("started", 0L)).longValue());
        Object f = m.get("found");
        if (f instanceof List<?> list) for (Object o : list) round.found.add(((Number) o).intValue());
        round.paid = Boolean.TRUE.equals(m.get("paid"));
        return round;
    }
}
