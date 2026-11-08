package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.games.text.RiddleText;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Word content lives in data files, never in code (SPEC §9.4):
 *   plugins/ExcelBrainGames/wordlists/<file>.yml
 *     theme: animals
 *     kid: [CAT, DOG, ...]        (sections by tier band)
 *     standard: [...]
 *     challenge: [...]
 * Several files may contribute sections to the same theme (that is how the
 * hand-checked kid lists join a theme when they move out of draft/).
 * Files under wordlists/draft/ are NEVER loaded — they are starting material
 * for Mike's hand check, behind the kid-tier pre-release gate.
 */
public final class WordLists {

    private final Map<String, Map<String, List<String>>> byTheme = new HashMap<>();
    private final List<RiddleText.Riddle> riddles = new ArrayList<>();
    private int fileCount;

    public void load(File wordlistDir, Logger log) {
        byTheme.clear();
        riddles.clear();
        fileCount = 0;
        File[] files = wordlistDir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            if (f.getName().equals("riddles.yml")) {
                for (Map<?, ?> e : y.getMapList("riddles")) {
                    Object q = e.get("question");
                    if (q == null) continue;
                    List<String> answers = new ArrayList<>();
                    Object a = e.get("answers");
                    if (a instanceof List<?> list) for (Object o : list) answers.add(String.valueOf(o));
                    if (answers.isEmpty()) continue;
                    riddles.add(new RiddleText.Riddle(String.valueOf(q), answers,
                            e.get("hint") == null ? "" : String.valueOf(e.get("hint")),
                            Boolean.TRUE.equals(e.get("kid"))));
                }
                fileCount++;
                continue;
            }
            String theme = y.getString("theme");
            if (theme == null || theme.isBlank()) {
                log.warning("ExcelBrainGames: wordlist " + f.getName() + " has no theme: key — skipped");
                continue;
            }
            Map<String, List<String>> sections = byTheme.computeIfAbsent(theme, k -> new HashMap<>());
            for (String section : List.of("kid", "standard", "challenge")) {
                List<String> words = y.getStringList(section);
                if (!words.isEmpty()) {
                    sections.computeIfAbsent(section, k -> new ArrayList<>()).addAll(words);
                }
            }
            fileCount++;
        }
    }

    /** The generator pool for a tier band of a theme. */
    public List<String> pool(String theme, String band) {
        Map<String, List<String>> sections = byTheme.get(theme);
        if (sections == null) return List.of();
        List<String> out = new ArrayList<>();
        switch (band) {
            case "kid" -> out.addAll(sections.getOrDefault("kid", List.of()));
            case "standard" -> out.addAll(sections.getOrDefault("standard", List.of()));
            case "challenge" -> {
                out.addAll(sections.getOrDefault("challenge", List.of()));
                out.addAll(sections.getOrDefault("standard", List.of()));
            }
            default -> { /* unknown band: empty pool */ }
        }
        return out;
    }

    public boolean hasPool(String theme, String band) {
        return !pool(theme, band).isEmpty();
    }

    /** Every theme's pool for a band, merged (Word Scramble draws server-wide, SPEC §7.1). */
    public List<String> poolAll(String band) {
        List<String> out = new ArrayList<>();
        for (String theme : byTheme.keySet()) {
            for (String w : pool(theme, band)) if (!out.contains(w)) out.add(w);
        }
        return out;
    }

    /** The Riddle of the Day list (wordlists/riddles.yml). */
    public List<RiddleText.Riddle> riddles() { return List.copyOf(riddles); }

    public int fileCount() { return fileCount; }
    public int themeCount() { return byTheme.size(); }
}
