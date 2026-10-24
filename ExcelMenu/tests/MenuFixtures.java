import com.winkreyes.excelmenu.core.DefinitionLoader;
import com.winkreyes.excelmenu.core.MenuDef;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared fixtures: the shipped definitions, a fake viewer context. */
final class MenuFixtures {

    static final String[] EXPECTED_MENUS = {
            "basics_menu", "commands_menu", "server_menu", "skills_perks",
            "spawnshop", "spawnshop_building", "spawnshop_enchanting",
            "spawnshop_food", "spawnshop_potions_protection", "spawnshop_safe_blocks",
            "spawnshop_travel_animals", "spawnshop_cosmetics_colors",
            "spawnshop_cosmetics_sets", "teleport_confirm_guild",
            "teleport_confirm_home", "teleport_confirm_spawn",
            "teleport_confirm_spawncmd",
    };

    static List<MenuDef> loadShipped() {
        List<MenuDef> defs = new ArrayList<>();
        for (String name : EXPECTED_MENUS) {
            Path file = Path.of("menus", name + ".yml");
            DefinitionLoader.Result r = DefinitionLoader.load(file);
            if (!r.ok()) {
                throw new IllegalStateException("shipped definition " + name
                        + " failed to load: " + r.errors());
            }
            defs.add(r.def());
        }
        return defs;
    }

    static MenuDef byId(List<MenuDef> defs, String id) {
        for (MenuDef d : defs) if (d.id.equals(id)) return d;
        throw new IllegalArgumentException("no menu " + id);
    }

    static DefinitionLoader.Result loadString(String id, String yaml) {
        return DefinitionLoader.load(id,
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    }

    static Set<String> materialNames() {
        Set<String> names = new HashSet<>();
        for (org.bukkit.Material m : org.bukkit.Material.values()) names.add(m.name());
        return names;
    }

    /** A RequirementEngine.Context driven by a placeholder map. */
    static final class FakeContext implements com.winkreyes.excelmenu.core.RequirementEngine.Context {
        final Map<String, String> placeholders = new HashMap<>();
        final Set<String> permissions = new HashSet<>();
        double balance = 0;

        FakeContext() {
            placeholders.put("%player_name%", "RigJava");
        }

        @Override
        public String resolve(String text) {
            String out = text;
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                out = out.replace(e.getKey(), e.getValue());
            }
            return out;
        }

        @Override
        public boolean hasPermission(String permission) {
            return permissions.contains(permission);
        }

        @Override
        public double balance() { return balance; }

        @Override
        public boolean hasItem(String material, int amount) { return false; }
    }

    static String readMenuFile(String name) throws Exception {
        return Files.readString(Path.of("menus", name + ".yml"));
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
