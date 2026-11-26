package excelemfixes.questxp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The quest-XP table: quest filename -&gt; EliteMobs XP (D136, sizing rule
 * D-W3-2: XP = K x tier^2). Pure logic — no Bukkit calls — so the plain-JVM
 * test suite can exercise it without a server.
 *
 * <p>Lookup order for a completed quest, because the exact form of
 * {@code CustomQuest.getConfigurationFilename()} is not statically provable
 * from the EliteMobs bytecode (it returns a stored field, set at config
 * load): (1) the raw filename string as returned, (2) its basename,
 * (3) {@code "primis/"} + basename, (4) the color-stripped quest display
 * name. Step 4 only ever matches names that map to exactly one XP value —
 * ambiguous names are excluded from the table at generation time
 * ("Make them go ouch" maps to both 3630 and 1815 and is never guessed).
 */
public final class QuestXpTable {

    /** One lookup outcome: the XP amount and which key form matched. */
    public record Result(long xp, String matchedBy) {}

    private final Map<String, Long> byFile;
    private final Map<String, Long> byName;

    public QuestXpTable(Map<String, Long> byFile, Map<String, Long> byName) {
        this.byFile = new HashMap<>(byFile);
        this.byName = new HashMap<>(byName);
    }

    /** Number of filename rows (112 for the Nov 19, 2026 table). */
    public int size() {
        return byFile.size();
    }

    /** Sum of all filename rows (130,355 for the Nov 19, 2026 table). */
    public long totalXp() {
        long total = 0;
        for (long xp : byFile.values()) {
            total += xp;
        }
        return total;
    }

    /** Quests whose configured XP is exactly 0 (excluded/held on purpose). */
    public Set<String> zeroFiles() {
        Set<String> out = new java.util.HashSet<>();
        for (Map.Entry<String, Long> e : byFile.entrySet()) {
            if (e.getValue() == 0) {
                out.add(e.getKey());
            }
        }
        return Collections.unmodifiableSet(out);
    }

    /** True when the table holds a row for this exact filename key. */
    public boolean hasFileKey(String key) {
        return key != null && byFile.containsKey(key);
    }

    public Result lookup(String rawFilename, String questName) {
        if (rawFilename != null) {
            String raw = rawFilename.trim();
            if (!raw.isEmpty()) {
                if (byFile.containsKey(raw)) {
                    return new Result(byFile.get(raw), "filename:" + raw);
                }
                String base = basename(raw);
                if (!base.equals(raw) && byFile.containsKey(base)) {
                    return new Result(byFile.get(base), "basename:" + base);
                }
                String primis = "primis/" + base;
                if (byFile.containsKey(primis)) {
                    return new Result(byFile.get(primis), "primis-prefixed:" + primis);
                }
            }
        }
        if (questName != null) {
            String stripped = stripColorCodes(questName).trim();
            if (!stripped.isEmpty() && byName.containsKey(stripped)) {
                return new Result(byName.get(stripped), "name:" + stripped);
            }
        }
        return new Result(0, "none");
    }

    /** Long XP for a table key, or 0 when the key is absent. */
    public long xpForFile(String key) {
        Long xp = byFile.get(key);
        return xp == null ? 0 : xp;
    }

    static String basename(String path) {
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    /** Strip Bukkit '&amp;' codes and section-sign codes from a display name. */
    static String stripColorCodes(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == '&' || c == '\u00a7') && i + 1 < text.length()) {
                i++; // skip the code character too
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }
}
