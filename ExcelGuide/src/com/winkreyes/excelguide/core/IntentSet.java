package com.winkreyes.excelguide.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * The curated intent definitions (intents.yml) and the matcher.
 * A phrase group matches when every `all` entry matches and, if the
 * group carries an `any` list, at least one `any` entry matches. Any
 * forbidden entry present in the message vetoes the intent outright.
 * The matcher's confidence for an intent is the best matching group's
 * authored weight; ties break toward more matched entries, then toward
 * the intent defined earlier in the file (write specific intents first).
 */
public final class IntentSet {

    /** One phrase group: required entries + optional any-of entries. */
    public record PhraseGroup(double weight, boolean ambient,
                              List<String> all, List<String> any) {}

    /** One curated intent. type: text | retrieval | wand | seals | clarify | stat | spells. */
    public record IntentDef(String id, String channel, boolean ambient, String type,
                            String stat, boolean requiresSkill, boolean vetoOnSkill,
                            List<PhraseGroup> phrases, List<String> forbidden,
                            String text, String textPrimary, String textFallback,
                            List<String> hints) {
        public boolean isPrivate() {
            return "private".equals(channel);
        }
    }

    /** A successful match: the intent + the confidence its group confers. */
    public record Match(IntentDef intent, double score, int matchedEntries) {}

    private final List<IntentDef> intents;

    public IntentSet(List<IntentDef> intents) {
        this.intents = List.copyOf(intents);
    }

    public List<IntentDef> intents() {
        return intents;
    }

    public IntentDef byId(String id) {
        for (IntentDef i : intents) {
            if (i.id().equals(id)) {
                return i;
            }
        }
        return null;
    }

    /**
     * Match a normalized message. For the AMBIENT door only groups
     * flagged ambient (on ambient intents) are eligible; other doors use
     * every group. Returns null when nothing matches.
     */
    public Match match(String normalized, java.util.Set<String> tokens,
                       boolean ambientDoor, boolean hasSkillToken) {
        Match best = null;
        for (IntentDef intent : intents) {
            if (intent.requiresSkill() && !hasSkillToken) {
                continue;
            }
            if (intent.vetoOnSkill() && hasSkillToken) {
                continue;
            }
            boolean vetoed = false;
            for (String f : intent.forbidden()) {
                if (TextNorm.entryMatches(f, normalized, tokens)) {
                    vetoed = true;
                    break;
                }
            }
            if (vetoed) {
                continue;
            }
            for (PhraseGroup g : intent.phrases()) {
                if (ambientDoor && !(intent.ambient() && g.ambient())) {
                    continue;
                }
                if (!groupMatches(g, normalized, tokens)) {
                    continue;
                }
                int entries = g.all().size() + (g.any().isEmpty() ? 0 : 1);
                Match cand = new Match(intent, g.weight(), entries);
                if (best == null
                        || cand.score() > best.score()
                        || (cand.score() == best.score() && cand.matchedEntries() > best.matchedEntries())) {
                    best = cand;
                }
            }
        }
        return best;
    }

    private static boolean groupMatches(PhraseGroup g, String normalized, java.util.Set<String> tokens) {
        for (String e : g.all()) {
            if (!TextNorm.entryMatches(e, normalized, tokens)) {
                return false;
            }
        }
        if (!g.any().isEmpty()) {
            boolean anyHit = false;
            for (String e : g.any()) {
                if (TextNorm.entryMatches(e, normalized, tokens)) {
                    anyHit = true;
                    break;
                }
            }
            if (!anyHit) {
                return false;
            }
        }
        return true;
    }

    /** Parse intents.yml text. Throws on malformed YAML. */
    @SuppressWarnings("unchecked")
    public static IntentSet fromYaml(String yamlText) {
        Object root = new Yaml().load(yamlText);
        if (!(root instanceof Map)) {
            throw new IllegalArgumentException("intents.yml: root is not a map");
        }
        Object raw = ((Map<String, Object>) root).get("intents");
        if (!(raw instanceof List)) {
            throw new IllegalArgumentException("intents.yml: no intents list");
        }
        List<IntentDef> out = new ArrayList<>();
        for (Object o : (List<Object>) raw) {
            Map<String, Object> m = (Map<String, Object>) o;
            List<PhraseGroup> groups = new ArrayList<>();
            Object rawGroups = m.get("phrases");
            if (rawGroups instanceof List) {
                for (Object go : (List<Object>) rawGroups) {
                    Map<String, Object> g = (Map<String, Object>) go;
                    double weight = g.get("weight") instanceof Number n ? n.doubleValue() : 0.0;
                    boolean amb = Boolean.TRUE.equals(g.get("ambient"));
                    groups.add(new PhraseGroup(weight, amb,
                            strList(g.get("all")), strList(g.get("any"))));
                }
            }
            out.add(new IntentDef(
                    str(m.get("id")),
                    m.get("channel") == null ? "public" : str(m.get("channel")),
                    Boolean.TRUE.equals(m.get("ambient")),
                    m.get("type") == null ? "text" : str(m.get("type")),
                    m.get("stat") == null ? "" : str(m.get("stat")),
                    Boolean.TRUE.equals(m.get("requires-skill")),
                    Boolean.TRUE.equals(m.get("veto-on-skill")),
                    List.copyOf(groups),
                    strList(m.get("forbidden")),
                    strOrNull(m.get("text")),
                    strOrNull(m.get("text-primary")),
                    strOrNull(m.get("text-fallback")),
                    strList(m.get("hints"))));
        }
        return new IntentSet(out);
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static String strOrNull(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static List<String> strList(Object o) {
        if (!(o instanceof List)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Object e : (List<?>) o) {
            out.add(String.valueOf(e));
        }
        return List.copyOf(out);
    }
}
