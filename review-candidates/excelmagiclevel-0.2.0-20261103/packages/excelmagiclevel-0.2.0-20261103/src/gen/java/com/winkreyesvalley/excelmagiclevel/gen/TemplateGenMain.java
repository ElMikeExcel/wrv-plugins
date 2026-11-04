package com.winkreyesvalley.excelmagiclevel.gen;

import com.winkreyesvalley.excelmagiclevel.core.Boost;
import com.winkreyesvalley.excelmagiclevel.core.Curve;
import com.winkreyesvalley.excelmagiclevel.core.ModifierTemplates;
import com.winkreyesvalley.excelmagiclevel.core.RewardTable;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ExcelMagicLevel — build-time generator entry point (NOT shipped in the
 * jar). Parses the plugin's real config.yml and writes the Magic modifiers
 * fragment from its curve + rewards tables — config data in, templates
 * out, no hand-written per-level table anywhere. If the level count or
 * the reward slate is ever rescaled, this regenerates from the edited
 * config; nothing here hardcodes the 20-level shape. Excel — Wink Reyes
 * Valley.
 *
 * Usage: TemplateGenMain &lt;config.yml&gt; &lt;output-file&gt;
 */
public final class TemplateGenMain {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(Path.of(args[0]))) {
            root = new Yaml().load(in);
        }
        Map<String, Object> curveSection = (Map<String, Object>) root.get("curve");
        Map<Integer, Long> overrides = new HashMap<>();
        Object rawOverrides = curveSection.get("overrides");
        if (rawOverrides instanceof Map<?, ?> map) {
            map.forEach((k, v) -> overrides.put(Integer.parseInt(String.valueOf(k)), ((Number) v).longValue()));
        }
        Curve curve = new Curve(
                ((Number) curveSection.get("max-level")).intValue(),
                ((Number) curveSection.get("base")).doubleValue(),
                ((Number) curveSection.get("exponent")).doubleValue(),
                ((Number) curveSection.get("round-to")).longValue(),
                overrides);

        Map<String, Object> rewardsSection = (Map<String, Object>) root.get("rewards");
        Map<String, Object> perLevel = (Map<String, Object>) rewardsSection.get("per-level");
        Boost per = new Boost(
                ((Number) perLevel.get("mana-max-boost")).doubleValue(),
                ((Number) perLevel.get("mana-regeneration-boost")).doubleValue(),
                ((Number) perLevel.get("cost-reduction")).doubleValue());
        Set<Integer> milestones = new HashSet<>();
        for (Object m : (List<Object>) rewardsSection.get("milestones")) {
            milestones.add(((Number) m).intValue());
        }
        RewardTable rewards = new RewardTable(per, milestones);

        Map<String, Object> modifiers = (Map<String, Object>) root.get("modifiers");
        String prefix = String.valueOf(modifiers.get("key-prefix"));

        String fragment = ModifierTemplates.generateFragment(prefix, curve, rewards);
        Path out = Path.of(args[1]);
        Files.writeString(out, fragment, StandardCharsets.UTF_8);
        System.out.println("wrote " + out + " (" + fragment.length() + " chars, levels 2.." + curve.maxLevel()
                + ", prefix " + prefix + ")");
    }
}
