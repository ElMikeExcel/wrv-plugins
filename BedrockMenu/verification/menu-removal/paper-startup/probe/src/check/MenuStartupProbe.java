package check;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Test-only plugin. Never place on a live server: it shuts down this test runtime. */
public final class MenuStartupProbe extends JavaPlugin {
    private final List<String> results = new ArrayList<>();
    private void check(boolean condition, String label) {
        results.add((condition ? "PASS " : "FAIL ") + label);
        if (!condition) throw new AssertionError(label);
    }
    private String[] readArray(Plugin target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (String[]) field.get(null);
    }
    @Override public void onEnable() {
        // A real main-thread server tick, after plugin startup, runs all checks.
        getServer().getScheduler().runTaskLater(this, this::verify, 20L);
    }
    private void verify() {
        try {
            check(Bukkit.isPrimaryThread(), "probe is on actual Paper main thread");
            check(Bukkit.getMinecraftVersion().equals("26.2"), "Minecraft 26.2 runtime");
            check(Bukkit.getVersion().contains("123"), "Paper build 123 runtime");
            Plugin target = Bukkit.getPluginManager().getPlugin("BedrockMenu");
            check(target != null, "original BedrockMenu plugin identity resolved");
            check(target.isEnabled(), "actual candidate plugin enabled");
            check(target.getDescription().getVersion().equals("1.4.2-menu.1"), "exact candidate descriptor version");
            check(target.getClass().getName().equals("com.winkreyes.bedrockmenu.BedrockMenu"), "original entry point loaded by Paper");
            check(Bukkit.getPluginManager().getPlugin("ExcelBedrockMenu") == null, "no renamed plugin loaded");
            String[] buttons = readArray(target, "BUTTONS");
            String[] commands = readArray(target, "COMMANDS");
            check(Arrays.equals(buttons, new String[]{"Spawn", "Teleport to a player", "Spawn Shop", "mcMMO Stats", "Leaderboard", "Server Info", "Auto-open on join"}), "seven ordered runtime button labels");
            check(Arrays.equals(commands, new String[]{"spawn", "tpmenu", "shop", "mcstats", "leaderboard", "motd", "menujoin"}), "seven correctly paired runtime commands");
            check(buttons.length == commands.length, "button/command lengths agree");
            check(!Arrays.asList(buttons).contains("Jobs & Money"), "removed label absent");
            check(!Arrays.asList(commands).contains("jobs browse"), "removed command route absent");
            for (String command : new String[]{"menu", "tpmenu"}) {
                PluginCommand actual = Bukkit.getPluginCommand(command);
                check(actual != null, command + " registered in actual Paper command map");
                check(actual.getPlugin() == target, command + " remains owned by BedrockMenu");
                check(actual.getExecutor() != null, command + " executor assigned");
                check(Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command), command + " console rejection path executes without linkage failure");
            }
            var listeners = HandlerList.getRegisteredListeners(target);
            check(!listeners.isEmpty(), "actual Paper listener registration succeeded");
            check(listeners.stream().allMatch(r -> r.getListener().getClass().getName().equals("com.winkreyes.bedrockmenu.TeleportMenu")), "original teleport listener registered");
            for (String simple : new String[]{"GeyserForms", "TeleportMenu", "TeleportMenu$PickHolder"}) {
                Class<?> type = Class.forName("com.winkreyes.bedrockmenu." + simple, false, target.getClass().getClassLoader());
                check(type.getClassLoader() == target.getClass().getClassLoader(), simple + " resolves from actual plugin classloader");
            }
            check(Bukkit.getPluginManager().getPlugin("Geyser-Spigot") == null, "test explicitly has no Geyser integration");
            check(Bukkit.getPluginManager().getPlugin("Essentials") == null, "test explicitly has no Essentials integration");
            results.add("RESULT PASS " + results.size() + " checks");
        } catch (Throwable failure) {
            results.add("RESULT FAIL " + failure);
            failure.printStackTrace();
        } finally {
            try {
                Files.write(Path.of("bedrock-startup-results.txt"), results);
                for (String line : results) getLogger().info(line);
            } catch (Exception failure) { failure.printStackTrace(); }
            Bukkit.shutdown();
        }
    }
}
