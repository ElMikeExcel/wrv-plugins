package com.winkreyes.excelmobveil;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ResourceConfigurationTest {
    @Test void pluginDescriptorNamesCandidateAndRegistersVeilCommand() throws Exception {
        Map<String, Object> config = new Yaml().load(Files.readString(Path.of("plugin.yml")));
        assertEquals("ExcelMobVeil", config.get("name"));
        assertEquals("0.1.0", config.get("version"));
        assertEquals("com.winkreyes.excelmobveil.ExcelMobVeil", config.get("main"));
        assertTrue(config.get("description") instanceof String);
        assertEquals(List.of("Vault"), config.get("depend"));
        assertEquals(List.of("Essentials", "DeluxeMenus"), config.get("softdepend"));
        Map<?, ?> commands = (Map<?, ?>) config.get("commands");
        assertEquals(1, commands.size());
        Map<?, ?> command = (Map<?, ?>) commands.get("mobveil");
        assertEquals(List.of("veil"), command.get("aliases"));
        assertTrue(((String) command.get("usage")).contains("status|buy"));
        Map<?, ?> permission = (Map<?, ?>) ((Map<?, ?>) config.get("permissions")).get("excelmobveil.admin");
        assertEquals("op", permission.get("default"));
    }
    @Test void defaultsSetTwoThousandAndEightRealTimeMinutes() throws Exception {
        Map<String, Object> config = new Yaml().load(Files.readString(Path.of("config.yml")));
        assertEquals(2000.0, ((Number) config.get("price")).doubleValue());
        assertEquals(480, ((Number) config.get("duration-seconds")).intValue());
        assertEquals(2, config.size());
    }
    @Test void purchasePolicyHasOnlyVeilProduct() {
        assertArrayEquals(new PurchaseCoordinator.Product[]{PurchaseCoordinator.Product.VEIL}, PurchaseCoordinator.Product.values());
    }
}
