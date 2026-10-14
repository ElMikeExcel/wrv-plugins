import java.io.File;
import java.nio.file.*;
import java.util.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/** Offline parser check using the exact Paper 26.2 build 123 API and SnakeYAML. */
public class VerifyPaperYaml {
  static int checks;
  static void check(boolean ok, String message) {
    checks++;
    if (!ok) throw new AssertionError(message);
  }
  static YamlConfiguration load(Path path) throws Exception {
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.load(path.toFile()); // Unlike loadConfiguration, this throws on invalid YAML.
    return yaml;
  }
  public static void main(String[] args) throws Exception {
    Path root = Path.of(args[0]);
    YamlConfiguration original = load(root.resolve("baseline/spawnshop.yml"));
    YamlConfiguration config = load(root.resolve("candidate/config.yml"));
    Set<String> products = new HashSet<>();
    for (String key : original.getConfigurationSection("items").getKeys(false))
      if (!Set.of("balance", "back").contains(key)) products.add(key);
    Set<String> seen = new HashSet<>();
    int menus = 0;
    try (var paths = Files.list(root.resolve("candidate/gui_menus"))) {
      for (Path path : paths.sorted().toList()) {
        String name = path.getFileName().toString().replace(".yml", "");
        YamlConfiguration yaml = load(path);
        check(yaml.getInt("size") == 9, name + " size");
        String type = yaml.getString("inventory_type", "CHEST");
        check(Set.of("CHEST", "DISPENSER").contains(type), name + " supported type");
        check(config.getString("gui_menus." + name + ".file").equals(name + ".yml"), name + " registration");
        if (name.equals("spawnshop")) {
          check(yaml.getString("open_command").equals("shop"), "shop route");
          check(yaml.getBoolean("register_command"), "registered command");
        } else {
          check(yaml.isList("open_command") && yaml.getStringList("open_command").isEmpty(), name + " no new commands");
        }
        var items = yaml.getConfigurationSection("items");
        Set<Integer> slots = new HashSet<>();
        for (String key : items.getKeys(false)) {
          var item = items.getConfigurationSection(key);
          int slot = item.getInt("slot", -1);
          check(slot >= 0 && slot < 9 && slots.add(slot), name + "/" + key + " unique valid slot");
          check(original.getConfigurationSection("items").getKeys(false).stream().anyMatch(k -> item.getString("material").equals(original.getString("items." + k + ".material"))), name + "/" + key + " existing verified material");
          if (products.contains(key)) {
            check(seen.add(key), key + " appears once");
            var before = original.getConfigurationSection("items." + key).getValues(true);
            var after = item.getValues(true);
            before.remove("slot"); after.remove("slot");
            check(before.equals(after), key + " exact product fields");
          }
          for (String side : List.of("left_click_commands", "right_click_commands")) {
            if (item.contains(side)) {
              var commands = item.getStringList(side);
              check(commands.size() == 1, name + "/" + key + " one action per click");
              if (commands.getFirst().startsWith("[openguimenu] "))
                check(config.isConfigurationSection("gui_menus." + commands.getFirst().substring(14)), name + "/" + key + " known navigation target");
            }
          }
        }
        for (int s = 0; s < slots.size(); s++) check(slots.contains(s), name + " no leading/internal gaps");
        check(yaml.getBoolean("items.balance.update") && yaml.getInt("update_interval") == 1, name + " balance refresh");
        System.out.println("PASS " + name + ": " + slots.size() + " buttons, " + type);
        menus++;
      }
    }
    check(menus == 6, "six menus");
    check(seen.equals(products) && seen.size() == 21, "all 21 products preserved exactly once");
    System.out.println("PASS: " + checks + " assertions; exact Paper YAML API accepts all seven install files.");
    System.out.println("Scope: offline YAML/API checks only; not a live DeluxeMenus/Geyser/client test.");
  }
}
