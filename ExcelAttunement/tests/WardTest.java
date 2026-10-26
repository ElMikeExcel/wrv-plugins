import com.winkreyes.excelattunement.core.AttunementState;
import com.winkreyes.excelattunement.core.SpellClass;
import com.winkreyes.excelattunement.core.Stance;
import com.winkreyes.excelattunement.core.WardNotice;
import com.winkreyes.excelattunement.core.WardRules;
import com.winkreyes.excelattunement.core.WardWorlds;
import com.winkreyes.excelattunement.core.WardWorlds.Reason;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** 0.1.5 guild ward: world matching, buff strip, teleport/cast rules, notice throttle, config defaults. */
public class WardTest {
    static final long T0 = 9_000_000L;

    static void run() throws Exception {
        System.out.println("== WardTest ==");
        worlds();
        buffs();
        rules();
        notice();
        clearOnEntry();
        configDefaults();
    }

    static void worlds() {
        WardWorlds w = new WardWorlds(List.of("em_"), List.of("GoblinKingdom_vf"));
        for (String n : new String[]{"em_adventurers_guild", "em_steamworks_lair", "em_north_pole", "em_oasis_wormhole",
                "em_id_binder_of_worlds", "em_id_binder_of_worlds_3", "em_id_enchantment_challenge_12_7", "EM_Adventurers_Guild"}) {
            Check.eq(Reason.PREFIX, w.reason(n, null), "prefix wards " + n + " (no registry)");
        }
        Check.eq(Reason.NAME, w.reason("GoblinKingdom_vf", null), "exact name wards GoblinKingdom_vf");
        Check.eq(Reason.NAME, w.reason("goblinkingdom_vf", null), "exact name is case-insensitive");
        for (String n : new String[]{"world", "world_nether", "world_the_end", "wrv_spawn", "items", "system", "GoblinKingdom", "goblinkingdom_vf2", "my_em_world"}) {
            Check.eq(Reason.NONE, w.reason(n, null), "normal world not warded: " + n);
            Check.eq(Reason.NONE, w.reason(n, Boolean.FALSE), "registry FALSE keeps " + n + " unwarded");
        }
        Check.eq(Reason.ELITEMOBS_REGISTRY, w.reason("custom_pack_world", Boolean.TRUE), "registry adds an unprefixed pack world");
        Check.eq(Reason.PREFIX, w.reason("em_adventurers_guild", Boolean.FALSE), "registry FALSE never overrides a config match");
        Check.eq(Reason.NONE, w.reason(null, null), "null world not warded");
        Check.ok(w.isWarded("em_x", null) && !w.isWarded("world", null), "isWarded mirrors reason");
        WardWorlds none = new WardWorlds(List.of(), List.of());
        Check.eq(Reason.NONE, none.reason("em_adventurers_guild", null), "empty config + no registry = nothing warded");
        Check.eq(Reason.ELITEMOBS_REGISTRY, none.reason("em_adventurers_guild", true), "empty config still uses the registry");
        WardWorlds blanks = new WardWorlds(java.util.Arrays.asList("", " ", null), java.util.Arrays.asList("", null));
        Check.eq(Reason.NONE, blanks.reason("world", null), "blank prefix never matches every world");
    }

    static void buffs() {
        Set<String> t = WardRules.normalizeAll(List.of("resistance", "absorption", "regeneration"));
        Check.eq(Set.of("resistance", "absorption", "regeneration"), t, "strip types normalized");
        Check.eq("resistance", WardRules.normalizeEffect("minecraft:resistance"), "namespaced key");
        Check.eq("resistance", WardRules.normalizeEffect("DAMAGE_RESISTANCE"), "legacy Magic name damage_resistance");
        Check.eq("absorption", WardRules.normalizeEffect(" ABSORPTION "), "upper case + spaces");
        // Magic's buffs (staged kit): Blessing 10s resistance+absorption, Ward 15s absorption, Superheal regen 5s
        Check.ok(WardRules.shouldStrip("minecraft:resistance", 200, t, 15), "Blessing resistance 10s stripped");
        Check.ok(WardRules.shouldStrip("minecraft:absorption", 300, t, 15), "Ward absorption 15s stripped (boundary)");
        Check.ok(WardRules.shouldStrip("minecraft:regeneration", 100, t, 15), "Superheal regen 5s stripped");
        Check.ok(WardRules.shouldStrip("minecraft:resistance", 1, t, 15), "1 tick left stripped");
        Check.ok(!WardRules.shouldStrip("minecraft:absorption", 301, t, 15), "15.05s left kept");
        Check.ok(!WardRules.shouldStrip("minecraft:absorption", 2400, t, 15), "golden apple absorption 2:00 kept");
        Check.ok(!WardRules.shouldStrip("minecraft:regeneration", 900, t, 15), "brewed regen 0:45 kept");
        Check.ok(!WardRules.shouldStrip("minecraft:resistance", -1, t, 15), "infinite resistance kept");
        Check.ok(!WardRules.shouldStrip("minecraft:fire_resistance", 200, t, 15), "fire resistance not in list kept");
        Check.ok(!WardRules.shouldStrip("minecraft:speed", 100, t, 15), "speed kept");
        Check.ok(!WardRules.shouldStrip("minecraft:resistance", 200, Set.of(), 15), "empty list strips nothing");
        Check.ok(WardRules.shouldStrip("minecraft:resistance", 600, t, 30), "max-seconds knob raises the cap");
        Check.ok(!WardRules.shouldStrip("minecraft:resistance", 200, t, 0), "max-seconds 0 strips nothing that lasts");
    }

    static void rules() {
        Check.ok(WardRules.blockCast(true, true, false), "cast in warded world blocked");
        Check.ok(!WardRules.blockCast(true, true, true), "bypass casts in warded world");
        Check.ok(!WardRules.blockCast(true, false, false), "normal world cast untouched");
        Check.ok(!WardRules.blockCast(false, true, false), "ward disabled = cast untouched");
        Check.ok(WardRules.blockTeleport(true, true, true, true, false), "Magic teleport into warded world blocked");
        Check.ok(!WardRules.blockTeleport(true, true, true, false, false), "non-Magic teleport (/ag, EliteMobs) allowed");
        Check.ok(!WardRules.blockTeleport(true, true, false, true, false), "Magic teleport to a normal world allowed");
        Check.ok(!WardRules.blockTeleport(true, true, true, true, true), "bypass may Recall in");
        Check.ok(!WardRules.blockTeleport(true, false, true, true, false), "teleport knob off");
        Check.ok(!WardRules.blockTeleport(false, true, true, true, false), "ward off");
        List<String> magicStack = List.of("java.lang.Thread", "org.bukkit.craftbukkit.entity.CraftPlayer",
                "com.elmakers.mine.bukkit.action.CastContext", "com.elmakers.mine.bukkit.action.builtin.RecallAction");
        List<String> agStack = List.of("java.lang.Thread", "org.bukkit.craftbukkit.entity.CraftPlayer",
                "com.magmaguy.elitemobs.commands.AdventurersGuildCommand", "com.winkreyes.excelguildgate.Gate");
        List<String> prefixes = List.of("com.elmakers.mine.bukkit.");
        Check.ok(WardRules.anyFrameMatches(magicStack, prefixes), "Recall stack is Magic-driven");
        Check.ok(!WardRules.anyFrameMatches(agStack, prefixes), "/ag stack is not Magic-driven");
        Check.ok(!WardRules.anyFrameMatches(magicStack, List.of("")), "blank prefix never matches");
        Check.ok(!WardRules.anyFrameMatches(java.util.Arrays.asList((String) null), prefixes), "null frame safe");
    }

    static void notice() {
        WardNotice n = new WardNotice(10);
        Check.ok(n.take(T0), "first blocked-cast line shows");
        Check.ok(!n.take(T0 + 9_999), "line throttled inside 10s");
        Check.ok(n.take(T0 + 10_000), "line again after 10s");
        n.force(T0 + 12_000);
        Check.ok(!n.take(T0 + 15_000), "entry line restarts the cooldown");
        WardNotice z = new WardNotice(0);
        Check.ok(z.take(T0) && z.take(T0), "cooldown 0 = every time");
    }

    static void clearOnEntry() {
        // The glue calls AttunementState.clear() on arrival: weapon lock is
        // driven by activeStance, so clear() frees the weapon at once.
        AttunementState s = new AttunementState(30, 10);
        s.onCast(SpellClass.COMBAT, T0);
        Check.eq(Stance.CASTER, s.activeStance(T0 + 1_000), "caster stance before entry");
        s.clear();
        Check.eq(Stance.NONE, s.activeStance(T0 + 1_000), "entry clear: no stance, weapons free");
        Check.ok(!s.inCombat(T0 + 1_000), "entry clear: combat context reset");
        s.onHostileDamageTaken(T0 + 2_000);
        Check.eq(Stance.NONE, s.activeStance(T0 + 2_000), "hostile damage after clear cannot revive the lock");
        Check.ok(!s.pollExpired(T0 + 40_000), "no stray 'stance fades' line after an entry clear");
    }

    static void configDefaults() throws Exception {
        String cfg = Files.readString(Path.of("config.yml"));
        Check.ok(cfg.contains("magic-ward:"), "config has magic-ward section");
        Check.ok(cfg.contains("  enabled: true"), "ward on by default");
        Check.ok(cfg.contains("    - em_"), "default prefix em_");
        Check.ok(cfg.contains("    - GoblinKingdom_vf"), "default exact name GoblinKingdom_vf");
        Check.ok(cfg.contains("strip-max-seconds: 15"), "strip cap 15s");
        Check.ok(cfg.contains("notice: \"The guild's halls are warded. Magic falls silent inside.\""), "notice text");
        Check.ok(cfg.contains("window-seconds: 30") && cfg.contains("combat-context-seconds: 10"), "attunement design knobs unchanged");
        String yml = Files.readString(Path.of("plugin.yml"));
        Check.ok(yml.contains("version: 0.1.6"), "plugin.yml 0.1.6");
        Check.ok(yml.contains("excelattunement.ward.bypass:") && yml.contains("default: false"), "bypass permission, not default for ops");
        Check.ok(yml.contains("softdepend: [Magic, EliteMobs]"), "soft-depends Magic and EliteMobs only (arena locks need no ExcelGuildGate)");
    }
}
