package com.winkreyes.excelwandbars.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/**
 * Spell info 2026-10-26: what a spell really does, in plain words, worked out
 * from the spell's own Magic config (the merged spells.yml block for one rank:
 * "parameters", "actions", "triggers"). Nothing is typed in by hand, so the
 * numbers follow spells.yml at every seal stage and never go stale.
 *
 * Magic 11.2.5 rules used here (read from its source):
 *  - action parameters: the spell's parameters, overridden by keys on the action
 *    itself, inherited by the actions inside it.
 *  - Heal "amount" is health points (2 = 1 heart). Repeat "repeat" (default 2)
 *    runs its actions that many times; a Delay inside waits "delay" ms first.
 *  - effect_X: N gives potion level N+1 for "duration" ms.
 *  - Feed "feed" is food points (2 = 1 drumstick).
 *  - target any_entity with nothing aimed at targets the caster (Targeting.java).
 *  - cancel_on_damage: a hit of MORE than that many health points cancels.
 * Pure Java (maps and lists only), unit-tested against the live spells.yml.
 * No em dashes. Lines are kept short for Bedrock forms and item lore.
 */
public final class SpellEffects {
    private SpellEffects() {}

    // ------------------------------------------------------------------ number words

    /** Health points as hearts: 1.5 -> "0.75 heart", 2 -> "1 heart", 10 -> "5 hearts". */
    public static String hearts(double hp) {
        double h = hp / 2.0;
        String n = num(h);
        return n + (Math.abs(h - 1.0) < 1e-9 || h < 1.0 ? " heart" : " hearts");
    }

    /** Food points as drumsticks: 6 -> "3 drumsticks". */
    public static String drumsticks(double points) {
        double d = points / 2.0;
        String n = num(d);
        return n + (Math.abs(d - 1.0) < 1e-9 || d < 1.0 ? " drumstick" : " drumsticks");
    }

    /** Up to 2 decimals, trailing zeros trimmed. */
    public static String num(double d) {
        if (Math.abs(d - Math.rint(d)) < 1e-9) return Long.toString((long) Math.rint(d));
        String s = String.format(Locale.ROOT, "%.2f", d);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }

    static String roman(int level) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return level >= 1 && level < r.length ? r[level] : Integer.toString(level);
    }

    // ------------------------------------------------------------------ Magic config helpers

    /**
     * Magic's ConfigurationUtils.addConfigurations(child, parent, false), the way
     * spell levels and "inherit" are built: keys in child win, nested maps merge,
     * lists and plain values from child replace the parent's.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> merge(Map<String, ?> child, Map<String, ?> parent) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (parent != null) out.putAll(parent);
        if (child == null) return out;
        for (Map.Entry<String, ?> e : child.entrySet()) {
            Object c = e.getValue(), p = out.get(e.getKey());
            if (c instanceof Map && p instanceof Map) out.put(e.getKey(), merge((Map<String, ?>) c, (Map<String, ?>) p));
            else out.put(e.getKey(), c);
        }
        return out;
    }

    /**
     * Magic Wand.levelSpells with level_spells_to_path (Wand.java, 11.2.5): the spell
     * rank a wand of a given rank gets. requiredPath(level) is the upgrade_required_path
     * of that rank's template (null if none); holds(path) is the wand rank's hasPath.
     */
    public static int levelAt(int maxLevel, IntFunction<String> requiredPath, Predicate<String> holds) {
        int target = maxLevel;
        String need = requiredPath.apply(target);
        while (target > 1 && need != null && !holds.test(need)) {
            target--;
            need = requiredPath.apply(target);
        }
        return Math.min(target + 1, maxLevel);
    }

    // ------------------------------------------------------------------ walk the actions

    private static final class Report {
        String who;                       // first line
        long castMs;                      // Delay before the main effect (top level, not in a Repeat)
        final List<String> castSelf = new ArrayList<>();    // potions on the caster during the cast
        double healNow;                   // instant heal (HP) per target
        double hotAmount; int hotTimes; long hotEvery;      // heal over time
        final List<String> gives = new ArrayList<>();
        final List<String> cures = new ArrayList<>();
        double feed; double saturation; int feedTimes = 1; long feedEvery;
        long focusMs;                     // wrv_focus on the caster
        String lockoutMod; long lockoutMs;
        boolean whileOn;                  // infinite repeat (toggle)
        String bolt;                      // damage chain description lines
        final List<String> extra = new ArrayList<>();
        final List<String> unknown = new ArrayList<>();
    }

    private static final java.util.Set<String> QUIET = java.util.Set.of(
            "message", "playeffects", "removeentity", "spawnentity", "asynchronous", "serial", "parallel");

    @SuppressWarnings("unchecked")
    private static Map<String, Object> local(Map<String, Object> inherited, Map<String, ?> action) {
        Map<String, Object> p = new LinkedHashMap<>(inherited);
        for (Map.Entry<String, ?> e : action.entrySet()) {
            String k = e.getKey();
            if (k.equals("class") || k.equals("actions") || k.equals("fail")) continue;
            p.put(k, e.getValue());
        }
        return p;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Object o) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (o instanceof List<?> l) for (Object x : l) if (x instanceof Map) out.add((Map<String, Object>) x);
        return out;
    }

    private static String cls(Map<String, ?> a) {
        Object c = a.get("class");
        String s = c == null ? "" : c.toString();
        if (s.endsWith("Action")) s = s.substring(0, s.length() - 6);
        return s.toLowerCase(Locale.ROOT);
    }

    static double d(Map<String, ?> p, String k, double def) {
        Object o = p.get(k);
        if (o instanceof Number n) return n.doubleValue();
        if (o != null) try { return Double.parseDouble(o.toString().trim()); } catch (NumberFormatException ignored) {}
        return def;
    }

    static boolean b(Map<String, ?> p, String k, boolean def) {
        Object o = p.get(k);
        if (o instanceof Boolean x) return x;
        if (o != null) return o.toString().trim().equalsIgnoreCase("true");
        return def;
    }

    static String s(Map<String, ?> p, String k) {
        Object o = p.get(k);
        return o == null ? null : o.toString();
    }

    private static List<String> strings(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List<?> l) { for (Object x : l) { if (x != null) out.add(x.toString()); } }
        else if (o != null) out.add(o.toString());
        return out;
    }

    private static boolean contains(List<Map<String, Object>> actions, String cls) {
        for (Map<String, Object> a : actions) {
            if (cls(a).equals(cls)) return true;
            if (contains(list(a.get("actions")), cls)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ potion words

    private static final Map<String, String> EFFECT = new LinkedHashMap<>();
    static {
        EFFECT.put("speed", "Speed"); EFFECT.put("jump", "Jump Boost"); EFFECT.put("slow", "Slowness");
        EFFECT.put("damage_resistance", "Resistance"); EFFECT.put("fire_resistance", "Fire Resistance");
        EFFECT.put("absorption", "Absorption"); EFFECT.put("heal", "Instant Health");
        EFFECT.put("regeneration", "Regeneration"); EFFECT.put("night_vision", "Night Vision");
        EFFECT.put("water_breathing", "Water Breathing"); EFFECT.put("confusion", "Nausea");
        EFFECT.put("poison", "Poison"); EFFECT.put("slow_digging", "Mining Fatigue");
        EFFECT.put("weakness", "Weakness"); EFFECT.put("wither", "Wither"); EFFECT.put("blindness", "Blindness");
        EFFECT.put("hunger", "Hunger"); EFFECT.put("increase_damage", "Strength"); EFFECT.put("fast_digging", "Haste");
        EFFECT.put("invisibility", "Invisibility"); EFFECT.put("levitation", "Levitation"); EFFECT.put("slow_falling", "Slow Falling");
    }
    /** Effects whose level changes nothing a player can see: shown without a number. */
    private static final java.util.Set<String> NO_LEVEL = java.util.Set.of("night_vision", "water_breathing", "fire_resistance", "invisibility");

    public static String effectName(String key) {
        String k = key.toLowerCase(Locale.ROOT);
        String n = EFFECT.get(k);
        if (n != null) return n;
        StringBuilder sb = new StringBuilder();
        for (String w : k.split("_")) if (!w.isEmpty()) sb.append(sb.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return sb.toString();
    }

    /** "effect_speed: 1" style keys in p, as (key, amplifier). Magic reads "a,b" as ticks,power. */
    private static Map<String, Integer> potions(Map<String, Object> p) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : p.entrySet()) {
            if (!e.getKey().startsWith("effect_") || e.getKey().equals("effect_only")) continue;
            String v = String.valueOf(e.getValue());
            try {
                int amp = v.contains(",") ? (int) Float.parseFloat(v.split(",")[1].trim()) : (int) Float.parseFloat(v.trim());
                out.put(e.getKey().substring(7), amp);
            } catch (NumberFormatException ignored) {}
        }
        return out;
    }

    /** One potion as words. durationMs <= 0 means "while on". */
    static String potionLine(String key, int amp, long durationMs, boolean whileOn) {
        String name = effectName(key) + (NO_LEVEL.contains(key) ? "" : " " + roman(amp + 1));
        String when = whileOn ? " while on" : (durationMs > 0 ? " for " + Durations.ms(durationMs) : "");
        switch (key) {
            case "heal": // vanilla: Instant Health heals 4 << amplifier health
                return "Heals " + hearts(4 << Math.max(0, amp)) + " at once (" + name + ")";
            case "absorption": // vanilla: 4 extra health per level
                return name + when + " (" + hearts(4.0 * (amp + 1)).replace("heart", "extra heart") + ")";
            case "regeneration": { // vanilla: 1 health every 50 >> amplifier ticks
                if (whileOn || durationMs <= 0) return name + when;
                long every = Math.max(1, 50 >> Math.max(0, amp));
                long hp = (durationMs / 50) / every;
                return name + when + (hp > 0 ? " (about " + hearts(hp) + ")" : "");
            }
            case "damage_resistance":
                return name + when + " (" + Math.min(100, 20 * (amp + 1)) + "% less damage)";
            case "speed":
                return name + when + " (" + (20 * (amp + 1)) + "% faster)";
            default:
                return name + when;
        }
    }

    // ------------------------------------------------------------------ main entry

    /** Plain effect lines for one spell rank. cfg = the merged spell config for that rank. */
    @SuppressWarnings("unchecked")
    public static List<String> lines(Map<String, ?> cfg) {
        List<String> out = new ArrayList<>();
        if (cfg == null) return out;
        Map<String, Object> params = cfg.get("parameters") instanceof Map ? new LinkedHashMap<>((Map<String, Object>) cfg.get("parameters")) : new LinkedHashMap<>();
        Map<String, Object> acts = cfg.get("actions") instanceof Map ? (Map<String, Object>) cfg.get("actions") : Map.of();
        List<Map<String, Object>> cast = list(acts.get("cast"));
        Report r = new Report();
        r.whileOn = "infinite".equalsIgnoreCase(String.valueOf(params.get("repeat"))) || "cancel".equalsIgnoreCase(String.valueOf(cfg.get("toggle")));
        r.who = who(cfg, params, cast);
        walk(cast, params, r, Scope.TARGET, false, 0, 0, 0);
        render(cfg, params, cast, r, out);
        List<Map<String, Object>> sneak = list(acts.get("alternate_sneak"));
        for (Map<String, Object> a : sneak) {
            if (cls(a).equals("recall") && "place".equalsIgnoreCase(s(a, "type"))) {
                int markers = (int) d(params, "marker_count", 1);
                out.add("Crouch and cast to place a marker");
                out.add("You can keep " + markers + " marker" + (markers == 1 ? "" : "s"));
            }
        }
        return out;
    }

    private enum Scope { TARGET, CASTER, AREA }

    @SuppressWarnings("unchecked")
    private static void walk(List<Map<String, Object>> actions, Map<String, Object> p, Report r, Scope scope,
                             boolean inRepeat, int times, long every, int projectile) {
        for (Map<String, Object> a : actions) {
            String c = cls(a);
            Map<String, Object> lp = local(p, a);
            List<Map<String, Object>> kids = list(a.get("actions"));
            switch (c) {
                case "changecontext":
                    walk(kids, lp, r, b(lp, "target_caster", false) ? Scope.CASTER : scope, inRepeat, times, every, projectile);
                    break;
                case "modifier": {
                    String m = s(lp, "add_modifiers");
                    long dur = (long) d(lp, "duration", 0);
                    if (scope == Scope.CASTER && "wrv_focus".equalsIgnoreCase(m)) r.focusMs = dur;
                    else if (m != null && m.equalsIgnoreCase(r.lockoutMod)) r.lockoutMs = dur;
                    break;
                }
                case "checkmodifiers":
                    r.lockoutMod = s(lp, "blocked");
                    walk(kids, lp, r, scope, inRepeat, times, every, projectile);
                    break;
                case "repeat": {
                    String rs = String.valueOf(lp.getOrDefault("repeat", "2"));
                    boolean inf = rs.equalsIgnoreCase("infinite");
                    int n = inf ? -1 : (int) d(lp, "repeat", 2);
                    long dl = 0;
                    for (Map<String, Object> k : kids) if (cls(k).equals("delay")) dl = (long) d(local(lp, k), "delay", d(local(lp, k), "warmup", 1));
                    if (projectile > 0) break; // counted in bolt()
                    walk(kids, lp, r, scope, true, n, dl, projectile);
                    break;
                }
                case "delay":
                    if (!inRepeat && projectile == 0 && scope == Scope.TARGET) r.castMs = (long) d(lp, "delay", d(lp, "warmup", 1));
                    break;
                case "heal": {
                    double amt = d(lp, "amount", 20);
                    if (d(lp, "percentage", 0) > 0) { r.extra.add("Heals " + num(d(lp, "percentage", 0) * 100) + "% of max health"); break; }
                    if (inRepeat && times > 0) { r.hotAmount = amt; r.hotTimes = times; r.hotEvery = every; }
                    else r.healNow += amt;
                    break;
                }
                case "feed":
                    r.feed = d(lp, "feed", 20); r.saturation = d(lp, "saturation", 20);
                    if (inRepeat && times > 0) { r.feedTimes = times; r.feedEvery = every; }
                    break;
                case "potioneffect": {
                    long dur = (long) d(lp, "duration", 0);
                    Map<String, Integer> pot = potions(lp);
                    if (scope == Scope.CASTER) {
                        for (Map.Entry<String, Integer> e : pot.entrySet()) r.castSelf.add(effectName(e.getKey()) + (NO_LEVEL.contains(e.getKey()) ? "" : " " + roman(e.getValue() + 1)));
                        break;
                    }
                    if (projectile > 0) break; // counted in bolt()
                    boolean inf = inRepeat && times < 0;
                    for (Map.Entry<String, Integer> e : pot.entrySet()) {
                        String line = potionLine(e.getKey(), e.getValue(), dur, inf);
                        if (!r.gives.contains(line)) r.gives.add(line);
                    }
                    for (String x : strings(lp.get("remove_effects"))) {
                        String n = effectName(x);
                        if (!r.cures.contains(n)) r.cures.add(n);
                    }
                    break;
                }
                case "areaofeffect":
                    walk(kids, lp, r, Scope.AREA, inRepeat, times, every, projectile);
                    break;
                case "customprojectile":
                    if (contains(kids, "damage")) { r.bolt = "x"; boltLines(kids, lp, r, 0); break; }
                    walk(kids, lp, r, scope, inRepeat, times, every, projectile);
                    break;
                case "sphere": {
                    double rad = d(lp, "radius", 1);
                    String brush = String.valueOf(lp.get("brush"));
                    if (brush.equalsIgnoreCase("erase") && String.valueOf(lp.get("destructible")).contains("fire")) {
                        r.extra.add("Puts out fire blocks within " + num(rad) + " blocks");
                    } else if (contains(kids, "modifyblock")) {
                        long undo = (long) d(lp, "undo", 0);
                        r.extra.add("Makes a " + blockName(brush) + " dome where it lands");
                        r.extra.add("Dome size: " + num(rad) + " blocks from middle to edge");
                        if (undo > 0) r.extra.add("The dome lasts " + Durations.ms(undo));
                        double refl = d(lp, "reflect_chance", 0);
                        if (refl > 0) r.extra.add("Spells that hit it bounce back " + num(refl * 100) + "% of the time");
                    }
                    break;
                }
                case "ignite":
                    if (d(lp, "duration", 5000) <= 0) r.extra.add("Puts out burning players and mobs nearby");
                    break;
                case "airsupply":
                    r.extra.add("max".equalsIgnoreCase(s(lp, "air")) ? "Fills your air bar all the way" : "Gives you air");
                    break;
                case "hat": {
                    long undo = (long) d(lp, "undo", 0);
                    r.extra.add("Puts a " + blockName(String.valueOf(lp.get("brush"))) + " helmet on you" + (undo > 0 ? " for " + Durations.ms(undo) : ""));
                    List<String> en = new ArrayList<>();
                    if (lp.get("enchantments") instanceof Map<?, ?> m) for (Map.Entry<?, ?> e : m.entrySet()) en.add(enchantName(String.valueOf(e.getKey()), (int) d(Map.of("v", e.getValue()), "v", 1)));
                    if (!en.isEmpty()) r.extra.add("Helmet has " + String.join(" and ", en));
                    break;
                }
                case "recall":
                    recall(lp, r);
                    break;
                case "checkentity":
                    if (String.valueOf(lp.get("allowed_entities")).contains("zombie_villager")) r.extra.add("Turns zombie villagers back into villagers");
                    break;
                default:
                    if (!QUIET.contains(c)) { if (!r.unknown.contains(c)) r.unknown.add(c); }
                    walk(kids, lp, r, scope, inRepeat, times, every, projectile);
            }
        }
    }

    /** Damage carried by a bolt (CustomProjectile), with the targets it jumps to. */
    private static void boltLines(List<Map<String, Object>> kids, Map<String, Object> p, Report r, int depth) {
        double dmgPlayer = 0; int hits = 0; String slow = null; long slowMs = 0;
        Map<String, Object> chain = null; List<Map<String, Object>> chainKids = null;
        for (Map<String, Object> a : kids) {
            String c = cls(a); Map<String, Object> lp = local(p, a);
            if (c.equals("damage")) { hits++; dmgPlayer = d(lp, "player_damage", d(lp, "damage", 1)); }
            else if (c.equals("potioneffect")) {
                for (Map.Entry<String, Integer> e : potions(lp).entrySet()) { slow = effectName(e.getKey()) + " " + roman(e.getValue() + 1); slowMs = (long) d(lp, "duration", 0); }
            } else if (c.equals("repeat")) {
                int n = (int) d(lp, "repeat", 2);
                for (Map<String, Object> k : list(a.get("actions"))) if (cls(k).equals("damage")) hits += n;
            } else if (c.equals("areaofeffect")) {
                chain = lp;
                for (Map<String, Object> k : list(a.get("actions"))) {
                    if (cls(k).equals("asynchronous")) for (Map<String, Object> k2 : list(k.get("actions"))) { if (cls(k2).equals("customprojectile")) { chainKids = list(k2.get("actions")); chain = local(local(lp, k), k2); } }
                    if (cls(k).equals("customprojectile")) { chainKids = list(k.get("actions")); chain = local(lp, k); }
                }
            }
        }
        List<String> l = r.extra;
        String each = hearts(dmgPlayer).replace(" heart", " heart");
        if (depth == 0) {
            l.add("Each hit: " + each + " damage");
            if (slow != null) l.add("Each hit: " + slow + " for " + Durations.ms(slowMs));
            l.add("First target is hit " + hits + " time" + (hits == 1 ? "" : "s"));
        } else {
            l.add((depth == 1 ? "Each of those is hit " : "That one is hit ") + hits + " time" + (hits == 1 ? "" : "s"));
        }
        if (chain != null && chainKids != null) {
            int count = (int) d(chain, "target_count", -1);
            double rad = d(chain, "radius", 8);
            l.add((depth == 0 ? "Then jumps to " + (count > 0 ? "up to " + count + " more" : "everyone near") : "Each then jumps to " + (count > 0 ? (count == 1 ? "1 more" : "up to " + count + " more") : "everyone near")) + " within " + num(rad) + " blocks");
            boltLines(chainKids, chain, r, depth + 1);
        }
    }

    private static String blockName(String brush) {
        String b = brush == null ? "" : brush.toLowerCase(Locale.ROOT);
        if (b.contains("glass")) return "glass";
        return b.replace('_', ' ');
    }

    private static String enchantName(String key, int level) {
        String n;
        switch (key.toLowerCase(Locale.ROOT)) {
            case "oxygen": case "respiration": n = "Respiration"; break;
            case "water_worker": case "aqua_affinity": n = "Aqua Affinity"; return n;
            default: n = effectName(key);
        }
        return n + " " + roman(level);
    }

    private static void recall(Map<String, Object> p, Report r) {
        if ("death".equalsIgnoreCase(s(p, "type"))) { r.extra.add("Takes you back to where you last died"); return; }
        List<String> places = new ArrayList<>();
        if (b(p, "allow_home", false)) places.add("your bed");
        if (b(p, "allow_town", false)) places.add("your town");
        if (b(p, "allow_wand", false)) places.add("a lost wand");
        if (b(p, "allow_death", false)) places.add("where you died");
        if (b(p, "allow_spawn", false)) places.add("spawn");
        if (b(p, "allow_marker", false)) places.add("your markers");
        if (places.isEmpty()) return;
        r.extra.add("Lets you pick: " + joinAnd(places));
    }

    // ------------------------------------------------------------------ who

    @SuppressWarnings("unchecked")
    private static String who(Map<String, ?> cfg, Map<String, Object> p, List<Map<String, Object>> cast) {
        String target = s(p, "target");
        String type = String.valueOf(p.getOrDefault("target_type", "")).toLowerCase(Locale.ROOT);
        String kinds = type.equals("player") ? "players" : "players and mobs";
        boolean passive = b(cfg, "passive", false) || cfg.get("triggers") != null;
        if (passive) {
            for (Object t : (cfg.get("triggers") instanceof List<?> l ? l : List.of())) {
                if (t instanceof Map<?, ?> m && "drowning".equalsIgnoreCase(String.valueOf(m.get("damage_type")))) return "Works by itself when you start to drown";
            }
            return "Works by itself";
        }
        boolean topArea = false, projArea = false, projectile = false;
        for (Map<String, Object> a : cast) {
            String c = cls(a);
            if (c.equals("areaofeffect")) topArea = true;
            if (c.equals("customprojectile")) { projectile = true; if (contains(list(a.get("actions")), "areaofeffect") && !contains(list(a.get("actions")), "damage")) projArea = true; }
        }
        double rad = d(p, "radius", 8);
        if (target != null && target.equalsIgnoreCase("self") && topArea)
            return "Who: you and friendly " + kinds + "|within " + num(rad) + " blocks of you";
        if (target != null && target.equalsIgnoreCase("self")) return "Who: you";
        if (projArea) return "Who: friendly " + kinds + " within|" + num(rad) + " blocks of where your shot lands";
        if (target != null && target.equalsIgnoreCase("any_entity")) {
            String one = type.equals("player") ? "player" : "player or mob";
            String w = b(p, "only_friendly", false) ? "Who: a friendly " + one + " you aim at" : "Who: the " + one + " you aim at";
            return w + "|Aim at nothing to use it on yourself";
        }
        if (projectile && contains(cast, "damage")) return "Shoots a bolt at what you aim at";
        if (projectile) return "Shoots at what you aim at";
        if (contains(cast, "sphere")) return "Works where you aim";
        return null;
    }

    // ------------------------------------------------------------------ render

    private static void render(Map<String, ?> cfg, Map<String, Object> p, List<Map<String, Object>> cast, Report r, List<String> out) {
        if (r.who != null) for (String w : r.who.split("\\|")) out.add(w);
        boolean area = r.who != null && r.who.contains("within");
        long warm = (long) d(p, "warmup", 0);
        double cancelDmg = p.get("cancel_on_damage") instanceof Boolean bb ? (bb ? 0.00001 : 0) : d(p, "cancel_on_damage", 0);
        if (r.castMs > 0) {
            out.add("Takes " + Durations.ms(r.castMs) + " to cast");
            if (!r.castSelf.isEmpty()) out.add("You get " + String.join(" and ", r.castSelf) + " while casting");
        } else if (warm > 0 && contains(cast, "recall")) {
            out.add("You wait " + Durations.ms(warm) + ", then you go");
        }
        if (r.castMs > 0 || warm > 0) {
            if (cancelDmg > 0) out.add(cancelDmg < 0.01 ? "Getting hurt stops it" : "A hit over " + hearts(cancelDmg) + " stops it");
            if (b(p, "cancel_on_cast_other", false)) out.add("Casting another spell stops it");
        }
        String eachSuffix = area ? " each" : "";
        if (r.healNow > 0 && r.hotTimes > 0) {
            out.add("Heals " + hearts(r.healNow) + " right away");
            out.add("Then " + hearts(r.hotAmount) + " every " + Durations.ms(r.hotEvery) + " for " + Durations.ms(r.hotEvery * r.hotTimes));
            out.add(hearts(r.healNow + r.hotAmount * r.hotTimes) + " in all");
        } else if (r.hotTimes > 0) {
            out.add("Heals " + hearts(r.hotAmount) + " every " + Durations.ms(r.hotEvery) + " for " + Durations.ms(r.hotEvery * r.hotTimes));
            out.add(hearts(r.hotAmount * r.hotTimes) + " in all");
        } else if (r.healNow > 0) {
            out.add((r.castMs > 0 ? "Then heals " : "Heals ") + hearts(r.healNow) + eachSuffix);
        }
        for (String g : r.gives) out.add(g.startsWith("Heals ") ? g : (r.whileOn ? "Gives " : "Gives ") + g);
        if (!r.cures.isEmpty()) out.add("Cures " + joinAnd(r.cures));
        if (r.feed > 0) {
            if (r.feedTimes > 1) out.add("Fills " + drumsticks(r.feed * r.feedTimes) + " of food over " + Durations.ms(r.feedEvery * r.feedTimes));
            else out.add("Fills " + drumsticks(r.feed) + " of food" + eachSuffix);
        }
        if (r.lockoutMod != null && r.lockoutMs > 0) out.add("Same target can get it again after " + Durations.ms(r.lockoutMs));
        out.addAll(r.extra);
        if (r.focusMs > 0) out.add("Your mana refills slower for " + Durations.ms(r.focusMs) + " after");
    }

    /** "A, B and C". */
    public static String joinAnd(List<String> items) {
        if (items.isEmpty()) return "";
        if (items.size() == 1) return items.get(0);
        return String.join(", ", items.subList(0, items.size() - 1)) + " and " + items.get(items.size() - 1);
    }

    /** Short lines for Bedrock forms and item lore: wraps at a space, max chars per line (a longer single word stays whole). */
    public static List<String> wrap(String line, int max) {
        List<String> out = new ArrayList<>();
        if (line == null) return out;
        String rest = line.trim();
        while (rest.length() > max) {
            int cut = rest.lastIndexOf(' ', max);
            if (cut <= 0) cut = rest.indexOf(' ');
            if (cut <= 0) break;
            out.add(rest.substring(0, cut).trim());
            rest = rest.substring(cut + 1).trim();
        }
        if (!rest.isEmpty()) out.add(rest);
        return out;
    }

    /** Lines in next that are not in now, in next's order (what a rank adds or changes). */
    public static List<String> changes(List<String> now, List<String> next) {
        List<String> out = new ArrayList<>();
        for (String n : next) if (!now.contains(n)) out.add(n);
        return out;
    }
}
