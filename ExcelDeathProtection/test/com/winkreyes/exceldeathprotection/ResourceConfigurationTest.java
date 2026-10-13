package com.winkreyes.exceldeathprotection;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ResourceConfigurationTest {
    @Test void pluginDescriptorParsesAndNamesTheCandidate() throws Exception {
        Map<String, Object> config = new Yaml().load(Files.readString(Path.of("plugin.yml")));
        assertEquals("ExcelDeathProtection", config.get("name"));
        assertEquals("0.1.3", config.get("version"));
        assertEquals("com.winkreyes.exceldeathprotection.ExcelDeathProtection", config.get("main"));
        assertTrue(config.get("description") instanceof String);
        assertTrue(((Map<?, ?>) config.get("commands")).containsKey("deathprotection"));
    }
    @Test void newPriceKeysAreExplicitAndLegacyPriceIsAbsent() throws Exception {
        Map<String, Object> config = new Yaml().load(Files.readString(Path.of("config.yml")));
        assertEquals(15000.0, ((Number) config.get("basic-price")).doubleValue());
        assertEquals(30000.0, ((Number) config.get("premium-price")).doubleValue());
        assertFalse(config.containsKey("price"));
    }
    @Test void menuIsAnUnassignedTemplateWithoutIndependentMoneyActions() throws Exception {
        String text = Files.readString(Path.of("integration/spawnshop-death-protection.template.yml"));
        assertNotNull(new Yaml().load(text));
        assertTrue(text.contains("deathprotection buy basic %player_name%"));
        assertTrue(text.contains("deathprotection buy premium %player_name%"));
        assertFalse(text.contains("[takemoney]"));
        assertFalse(text.contains("minecraft:give"));
    }
}
