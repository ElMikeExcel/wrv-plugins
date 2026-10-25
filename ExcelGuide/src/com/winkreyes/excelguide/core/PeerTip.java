package com.winkreyes.excelguide.core;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.yaml.snakeyaml.Yaml;

/**
 * 0.3.0 first-use tip for player-taught answers (Mike's rule: every
 * new player-facing piece ships a tip that repeats until the player
 * taps OK). Pure logic + file, no Bukkit types, so it is unit-tested.
 *
 * Shown each time a player is served an answer starting with the
 * serve prefix ("A player said: "), until they say OK: the Java [OK]
 * click, the Bedrock pop-up OK button, or /guide ok. The Bedrock
 * pop-up goes out at most once per session; the chat lines repeat on
 * every player answer until OK. Stored per player: only the OK flag
 * (data-folder peer-tip.yml, never in the repo).
 *
 * The file loads fail-soft: missing, empty, "[]", "{}" or a broken
 * file all mean "nobody has said OK yet", with no crash.
 */
public final class PeerTip {

    public static final String TITLE = "Guide tip";
    public static final String L_WHAT = "Tip: answers that start with \"A player said\" were taught by other players and checked by the server owner.";
    public static final String L_CHECK = "They are usually right, but the website's Ask page is the official word.";
    public static final String L_OK_JAVA = "Click [OK] to hide this tip.";
    public static final String L_OK_BEDROCK = "Tap OK on the pop-up, or type /guide ok, to hide this tip.";
    public static final String OK_BUTTON = "OK, got it!";
    public static final String DONE_ACK = "Got it! That tip won't show again.";
    public static final String ALREADY = "You already said OK to that tip.";

    public boolean enabled = true;

    private final Path file;
    private final Set<UUID> acked = new HashSet<>();
    private final Set<UUID> formThisSession = new HashSet<>();
    private String loadWarning;
    private long shown;

    public PeerTip(Path file) {
        this.file = file;
        this.loadWarning = load();
    }

    public String loadWarning() { return loadWarning; }
    public long shownCount() { return shown; }
    public int ackedCount() { return acked.size(); }
    public boolean acked(UUID id) { return id != null && acked.contains(id); }

    /** Does this delivered reply text carry a player-taught answer? */
    public static boolean isPeerAnswer(String replyText, String servePrefix) {
        return replyText != null && servePrefix != null && !servePrefix.isBlank()
                && replyText.startsWith(servePrefix);
    }

    /** A player answer was just delivered to this player: show the tip now? */
    public boolean shouldShow(UUID id) {
        if (!enabled || id == null || acked.contains(id)) {
            return false;
        }
        shown++;
        return true;
    }

    /** May the Bedrock pop-up go out now? Once per session. */
    public boolean takeForm(UUID id) {
        return id != null && !acked.contains(id) && formThisSession.add(id);
    }

    public void endSession(UUID id) { formThisSession.remove(id); }

    /** OK. True when new (and saved). */
    public boolean ack(UUID id) {
        if (id == null || !acked.add(id)) {
            return false;
        }
        save();
        return true;
    }

    /** Admin: show the tip to this player again. */
    public boolean reset(UUID id) {
        formThisSession.remove(id);
        if (id == null || !acked.remove(id)) {
            return false;
        }
        save();
        return true;
    }

    /** Chat lines for one showing (the OK line last). */
    public static List<String> lines(boolean bedrock) {
        List<String> out = new ArrayList<>();
        out.add(L_WHAT);
        out.add(L_CHECK);
        out.add(bedrock ? L_OK_BEDROCK : L_OK_JAVA);
        return out;
    }

    public static String formContent() {
        return L_WHAT + "\n\n" + L_CHECK;
    }

    // ---------- file ----------

    private String load() {
        acked.clear();
        if (file == null || !Files.exists(file)) {
            return null;
        }
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            if (text.isBlank()) {
                return null;
            }
            Object root = new Yaml().load(text);
            if (root == null || (root instanceof List<?> l && l.isEmpty())
                    || (root instanceof Map<?, ?> m0 && m0.isEmpty())) {
                return null;
            }
            if (!(root instanceof Map<?, ?> m)) {
                return "tip file root is not a map; nobody has said OK yet";
            }
            Object raw = m.get("acknowledged");
            if (raw == null) {
                return null;
            }
            if (!(raw instanceof List<?> list)) {
                return "tip file 'acknowledged' is not a list; nobody has said OK yet";
            }
            int bad = 0;
            for (Object o : list) {
                try {
                    acked.add(UUID.fromString(String.valueOf(o)));
                } catch (IllegalArgumentException e) {
                    bad++;
                }
            }
            return bad == 0 ? null : "skipped " + bad + " unreadable tip lines";
        } catch (Exception e) {
            acked.clear();
            return "tip file unreadable (" + e.getMessage() + "); nobody has said OK yet";
        }
    }

    private void save() {
        if (file == null) {
            return;
        }
        try {
            Map<String, Object> root = new LinkedHashMap<>();
            List<String> ids = new ArrayList<>();
            for (UUID id : new TreeSet<>(acked)) {
                ids.add(id.toString());
            }
            root.put("acknowledged", ids);
            String body = "# ExcelGuide: players who said OK to the player-answer tip (written by the plugin).\n"
                    + new Yaml().dump(root);
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, body, StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            loadWarning = "could not save tip file: " + e.getMessage();
        }
    }
}
