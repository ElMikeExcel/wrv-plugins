package com.winkreyes.excelguide.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * The Guide's knowledge: the website's ask chunks, loaded from the
 * generated knowledge.yml (site-ai/make-guide-knowledge.py). The YAML
 * parse lives here (SnakeYAML is a plain library dependency, present in
 * Paper at runtime and in the test classpath); everything downstream
 * works on plain records.
 */
public final class KnowledgeBase {

    /** One website knowledge chunk. key is unique; id is the site's id. */
    public record Chunk(String key, String id, String title, String text) {}

    private final List<Chunk> chunks;
    private final String sourceSha256;

    public KnowledgeBase(List<Chunk> chunks, String sourceSha256) {
        this.chunks = List.copyOf(chunks);
        this.sourceSha256 = sourceSha256 == null ? "" : sourceSha256;
    }

    public List<Chunk> chunks() {
        return chunks;
    }

    public int size() {
        return chunks.size();
    }

    public String sourceSha256() {
        return sourceSha256;
    }

    /** Knowledge version stamp: first 8 of the source hash (SPEC 8). */
    public String version() {
        return sourceSha256.length() >= 8 ? sourceSha256.substring(0, 8) : "unknown";
    }

    /** Parse the generated knowledge.yml text. Throws on malformed YAML. */
    @SuppressWarnings("unchecked")
    public static KnowledgeBase fromYaml(String yamlText) {
        Object root = new Yaml().load(yamlText);
        if (!(root instanceof Map)) {
            throw new IllegalArgumentException("knowledge.yml: root is not a map");
        }
        Map<String, Object> map = (Map<String, Object>) root;
        String sha = map.get("source_sha256") == null ? "" : String.valueOf(map.get("source_sha256"));
        Object rawChunks = map.get("chunks");
        if (!(rawChunks instanceof List)) {
            throw new IllegalArgumentException("knowledge.yml: no chunks list");
        }
        List<Chunk> out = new ArrayList<>();
        for (Object o : (List<Object>) rawChunks) {
            Map<String, Object> c = (Map<String, Object>) o;
            out.add(new Chunk(
                    str(c.get("key")), str(c.get("id")), str(c.get("title")), str(c.get("text"))));
        }
        return new KnowledgeBase(out, sha);
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
