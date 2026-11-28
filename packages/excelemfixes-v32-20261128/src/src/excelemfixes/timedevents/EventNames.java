package excelemfixes.timedevents;

/**
 * Plain-language names for timed events, for the broadcast.
 * Pure text work — no Bukkit, no EliteMobs.
 *
 * The name a player should see is the boss's own display name
 * ("Bone Seeker Goblin"), cleaned of the markup EliteMobs
 * config files carry (MiniMessage tags like
 * {@code <gradient:#76EFC4:#00C96E>Bone Seeker Goblin</gradient>},
 * legacy section-sign codes, and the {@code $eventBossLevel}
 * placeholder EliteMobs swaps for a level badge at spawn). When
 * no display name can be read at all, the boss filename is
 * prettified as a last resort.
 */
public final class EventNames {

    private EventNames() {}

    /**
     * Clean a raw EliteMobs display name for chat: strip
     * MiniMessage-style {@code <...>} tags, strip legacy
     * section-sign color/format codes, drop the
     * {@code $eventBossLevel} placeholder token, collapse
     * whitespace. Returns "" when nothing readable remains.
     */
    public static String cleanDisplayName(String raw) {
        if (raw == null) return "";
        String s = raw.replaceAll("<[^>]*>", " ");
        s = s.replaceAll("§.", "");
        s = s.replace("$eventBossLevel", " ");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    /**
     * Last-resort name from a config filename:
     * "em_free_goblins_event_pack_bone_seeker_goblin.yml" ->
     * "Free Goblins Event Pack Bone Seeker Goblin". Strips any
     * directory part and the .yml extension, drops a trailing
     * "_event" (event files name the event after their boss),
     * drops a leading "em" token (EliteMobs' file prefix
     * convention), title-cases the rest.
     */
    public static String prettifyFilename(String filename) {
        if (filename == null) return "";
        String s = filename;
        int slash = Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\'));
        if (slash >= 0) s = s.substring(slash + 1);
        if (s.toLowerCase().endsWith(".yml")) s = s.substring(0, s.length() - 4);
        if (s.toLowerCase().endsWith("_event")) s = s.substring(0, s.length() - 6);
        StringBuilder out = new StringBuilder();
        boolean first = true;
        for (String token : s.split("_")) {
            if (token.isEmpty()) continue;
            if (first && token.equalsIgnoreCase("em")) {
                first = false;
                continue;
            }
            first = false;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) out.append(token.substring(1).toLowerCase());
        }
        return out.toString();
    }

    /**
     * The name to broadcast: the cleaned CONFIGURED name when
     * one reads (the pack author's canonical name — it cleans
     * deterministically), else the cleaned live display name,
     * else the prettified boss filename, else the prettified
     * event filename, else "A timed event". Configured-first
     * is rig-earned (Nov 26, 2026): the live entity name came
     * back badge-prefixed and truncated ("「☠」 Bone Seeker
     * Gobl") where the configured name cleaned to exactly
     * "Bone Seeker Goblin".
     */
    public static String displayName(String configuredName, String liveName,
                                     String bossFilename, String eventFilename) {
        String name = cleanDisplayName(configuredName);
        if (!name.isEmpty()) return name;
        name = cleanDisplayName(liveName);
        if (!name.isEmpty()) return name;
        name = prettifyFilename(bossFilename);
        if (!name.isEmpty()) return name;
        name = prettifyFilename(eventFilename);
        if (!name.isEmpty()) return name;
        return "A timed event";
    }
}
