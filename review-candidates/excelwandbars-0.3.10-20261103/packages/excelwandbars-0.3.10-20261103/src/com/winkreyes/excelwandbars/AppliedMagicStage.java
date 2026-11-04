package com.winkreyes.excelwandbars;
import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.util.*;import org.bukkit.Bukkit;import org.bukkit.plugin.Plugin;import org.bukkit.configuration.file.YamlConfiguration;
/** Read-only exact MagicGate persisted stage. Does not reinterpret GuildGate revokes or grant definitions. */
final class AppliedMagicStage {
 static boolean dependencyEnabled(){Plugin gate=Bukkit.getPluginManager().getPlugin("ExcelMagicGate");return gate!=null&&gate.isEnabled();}
 static int read(){Plugin gate=Bukkit.getPluginManager().getPlugin("ExcelMagicGate");if(gate==null||!gate.isEnabled())throw new IllegalStateException("ExcelMagicGate unavailable");return readFile(gate.getDataFolder().toPath().resolve("state.yml"));}
 static int readFile(Path path){try{if(!Files.isRegularFile(path)||Files.size(path)>65536)throw new IllegalStateException("MagicGate stage state unavailable");byte[] bytes=Files.readAllBytes(path);if(bytes.length>65536)throw new IllegalStateException("stage state too large");var yaml=new YamlConfiguration();yaml.loadFromString(new String(bytes,StandardCharsets.UTF_8));Object value=yaml.get("appliedStage");if(!(value instanceof Integer n)||n<0||n>5)throw new IllegalStateException("invalid appliedStage");if(!Arrays.equals(bytes,Files.readAllBytes(path)))throw new IllegalStateException("stage changed during read");return n;}catch(Exception failure){throw new IllegalStateException("MagicGate applied-stage read failed",failure);}}
 static Set<String> active(Map<String,Integer> manifest,int stage){if(stage<0||stage>5)throw new IllegalArgumentException("stage");Set<String> result=new TreeSet<>();manifest.forEach((spell,min)->{if(min==null||min<0||min>5)throw new IllegalArgumentException("minimum seal");if(min<=stage)result.add(spell);});return Set.copyOf(result);}
 private AppliedMagicStage(){}
}
