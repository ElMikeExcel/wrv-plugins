import com.winkreyes.excelwandbars.core.SpellEffects;
import java.io.FileInputStream;
import java.util.*;

/** Test helper: loads a Magic spells.yml and builds each template's merged config the way Magic 11.2.5 does. */
public final class SpellsYml {
    final Map<String, Object> raw;
    final Map<String, Map<String, Object>> merged = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    SpellsYml(String path) throws Exception {
        try (FileInputStream in = new FileInputStream(path)) {
            raw = (Map<String, Object>) new org.yaml.snakeyaml.Yaml().load(in);
        }
        for (String k : raw.keySet()) get(k);
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> get(String key) {
        if (merged.containsKey(key)) return merged.get(key);
        Map<String, Object> node = (Map<String, Object>) raw.get(key);
        if (node == null) return null;
        Map<String, Object> out = new LinkedHashMap<>(node);
        Object inh = node.get("inherit");
        if (inh != null && !"false".equalsIgnoreCase(inh.toString())) {
            Map<String, Object> parent = get(inh.toString());
            if (parent != null) out = SpellEffects.merge(out, parent);
        }
        int bar = key.indexOf('|');
        if (bar > 0) {
            int lvl = Integer.parseInt(key.substring(bar + 1));
            String up = key.substring(0, bar) + (lvl == 2 ? "" : "|" + (lvl - 1));
            Map<String, Object> parent = get(up);
            if (parent != null) out = SpellEffects.merge(out, parent);
        }
        merged.put(key, out);
        return out;
    }

    int maxLevel(String base) {
        int m = 1;
        while (raw.containsKey(base + "|" + (m + 1))) m++;
        return m;
    }

    String requiredPath(String base, int level) {
        Map<String, Object> c = get(level == 1 ? base : base + "|" + level);
        Object o = c == null ? null : c.get("upgrade_required_path");
        return o == null ? null : o.toString();
    }
}
