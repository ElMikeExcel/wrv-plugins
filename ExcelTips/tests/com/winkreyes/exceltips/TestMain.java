package com.winkreyes.exceltips;

import com.winkreyes.exceltips.core.TipBook;
import com.winkreyes.exceltips.core.TipBook.Result;
import com.winkreyes.exceltips.core.Words;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TestMain {
    static int pass = 0, fail = 0;
    static void check(boolean ok, String what) { if (ok) pass++; else { fail++; System.out.println("FAIL: " + what); } }

    public static void main(String[] a) throws Exception {
        book();
        words();
        tipsFile(a.length > 0 ? a[0] : "tips.yml", a.length > 1 ? a[1] : null);
        if (a.length > 2) form(a[2]);
        System.out.println(pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }

    static void book() {
        TipBook b = new TipBook();
        UUID p = UUID.randomUUID(), q = UUID.randomUUID();
        long t = 1_000_000;
        // shows on first 3 uses, then DONE
        check(b.onUse(p, "jobs", t, true, 3, 3000, 10000, 10000) == Result.SHOW, "first use shows");
        check(b.onUse(p, "jobs", t + 1000, true, 3, 3000, 10000, 10000) == Result.GAP, "burst = one use");
        check(b.onUse(p, "jobs", t + 20000, true, 3, 3000, 10000, 10000) == Result.SHOW, "second use shows");
        check(b.onUse(p, "jobs", t + 40000, true, 3, 3000, 10000, 10000) == Result.SHOW, "third use shows");
        check(b.onUse(p, "jobs", t + 60000, true, 3, 3000, 10000, 10000) == Result.DONE, "fourth is done");
        check(b.shows(p, "jobs") == 3, "count 3");
        check(b.finished(p, "jobs", 3), "finished at cap");
        // ack stops it, per feature only
        check(b.onUse(p, "trash", t + 80000, true, 3, 3000, 10000, 10000) == Result.SHOW, "trash shows");
        check(b.lastShown(p).equals("trash"), "last shown trash");
        check(b.ack(p, "TRASH"), "ack is case-insensitive and new");
        check(!b.ack(p, "trash"), "second ack not new");
        check(b.onUse(p, "trash", t + 200000, true, 3, 3000, 10000, 10000) == Result.ACKED, "acked stops it");
        check(b.onUse(p, "spawn", t + 200000, true, 3, 3000, 10000, 10000) == Result.SHOW, "other feature still shows");
        // min gap between different tips
        check(b.onUse(p, "beds", t + 201000, true, 3, 3000, 10000, 10000) == Result.BUSY, "two tips at once never stack");
        check(b.shows(p, "beds") == 0, "busy not counted");
        // combat
        b.combat(q, t);
        check(b.onUse(q, "jobs", t + 5000, true, 3, 3000, 10000, 10000) == Result.COMBAT, "no tips in combat");
        check(b.shows(q, "jobs") == 0, "combat not counted");
        check(b.onUse(q, "jobs", t + 11000, true, 3, 3000, 10000, 10000) == Result.SHOW, "after combat quiet it shows");
        b.combat(q, t + 50000); b.clearCombat(q);
        check(b.onUse(q, "after", t + 51000, true, 3, 3000, 10000, 10000) == Result.SHOW, "respawn clears combat");
        // max 0 = until OK
        UUID r = UUID.randomUUID();
        for (int i = 0; i < 30; i++) b.onUse(r, "x", t + i * 20000L, true, 0, 3000, 10000, 10000);
        check(b.shows(r, "x") == 30, "max 0 keeps showing");
        check(!b.finished(r, "x", 0), "max 0 not finished until OK");
        // wand style: 15
        UUID w = UUID.randomUUID(); int shown = 0;
        for (int i = 0; i < 40; i++) if (b.onUse(w, "wandstyle", t + i * 2500L, true, 15, 2000, 0, 10000) == Result.SHOW) shown++;
        check(shown == 15, "wandstyle shows exactly 15, got " + shown);
        // disabled
        check(b.onUse(w, "off", t, false, 3, 0, 0, 0) == Result.DISABLED, "disabled");
        // form gate
        UUID f = UUID.randomUUID();
        check(b.takeForm(f, t, 300000, 10000), "first pop-up ok");
        check(!b.takeForm(f, t + 1000, 300000, 10000), "pop-up cooldown");
        check(b.takeForm(f, t + 301000, 300000, 10000), "pop-up after cooldown");
        b.combat(f, t + 700000);
        check(!b.takeForm(f, t + 705000, 300000, 10000), "no pop-up in combat");
        // reset
        check(b.reset(p, "jobs") == 1, "reset one");
        check(b.shows(p, "jobs") == 0 && b.acked(p, "trash"), "reset one keeps others");
        check(b.reset(p, null) >= 2, "reset all");
        check(b.state(p, "trash") == null, "reset all clears");
        // persistence round trip shape
        TipBook c = new TipBook(); c.put(p, "a", 2, false); c.put(p, "b", 1, true); c.put(p, "z", 0, false);
        check(c.shows(p, "a") == 2 && c.acked(p, "b") && c.state(p, "z") == null, "put");
        // session end keeps stored data
        c.endSession(p); check(c.shows(p, "a") == 2 && c.lastShown(p) == null, "endSession keeps data");
    }

    static void words() {
        check(Words.label("/Essentials:TPA bob").equals("tpa"), "label namespace");
        check(Words.label("//jobs browse").equals("jobs"), "label slashes");
        check(Words.label("").isEmpty(), "label empty");
        for (String s : List.of("ok", "OK!", "okay", "Okay.", "got it", "k", "ok got it!")) check(Words.isOk(s), "ok reply " + s);
        for (String s : List.of("ok lets go mining", "not ok", "okk", "", "book")) check(!Words.isOk(s), "not ok reply " + s);
        check(Words.versionAtLeast("0.2.0", "0.2.0") && Words.versionAtLeast("0.2.1-SNAPSHOT", "0.2.0") && Words.versionAtLeast("1.0", "0.9.9"), "version at least");
        check(!Words.versionAtLeast("0.1.0", "0.2.0") && !Words.versionAtLeast("0.1.17+26.3", "0.2") && Words.versionAtLeast("0.1.17+26.3", "0.1.17"), "version below");
        check(Words.versionAtLeast("2.12.0-b1270", "") && !Words.versionAtLeast(null, "1"), "blank want / null have");
        check(Arrays.equals(Words.pluginSpec("ExcelTrash>=0.2.0"), new String[]{"ExcelTrash", "0.2.0"}) && Arrays.equals(Words.pluginSpec(" AutoSort "), new String[]{"AutoSort", ""}), "plugin spec");
        check(Words.hasDash("a \u2014 b") && Words.hasDash("1\u20132") && !Words.hasDash("a - b"), "dash check");
    }

    @SuppressWarnings("unchecked")
    static void tipsFile(String path, String bannedPath) throws Exception {
        Map<String, Object> y = new Yaml().load(new FileInputStream(path));
        Map<String, Object> tips = (Map<String, Object>) y.get("tips");
        check(tips != null && tips.size() >= 20, "tips.yml has 20+ tips");
        String raw = Files.readString(new File(path).toPath());
        check(raw.contains("SEAL AND BOSS RULE") && raw.contains("learn-by-dying"), "seal rule comment present");
        check(!Words.hasDash(raw), "no long dashes anywhere in tips.yml");
        List<String> banned = new ArrayList<>();
        if (bannedPath != null && new File(bannedPath).exists()) for (String l : Files.readAllLines(new File(bannedPath).toPath())) if (!l.isBlank()) banned.add(l.trim().toLowerCase());
        for (String b : banned) check(!raw.toLowerCase().contains(b), "no private name/tag in tips.yml (" + banned.indexOf(b) + ")");
        for (String dup : List.of("wandstyle", "magic-wand", "spellscreen", "lostfound", "lost-and-found"))
            check(!tips.containsKey(dup), "no duplicate tip entry for a plugin that ships its own (" + dup + ")");
        List<String> sealWords = List.of("wave", "phase", "boss", "wither", "warden", "dragon", "elder", "guardian", "raid", "omen", "health", "damage", "counter", "cheese", "hit", "attack", "armor", "potion", "kill", "minion", "beat", "win", "strong", "hard", "expect", "trick");
        for (var e : tips.entrySet()) {
            Map<String, Object> t = (Map<String, Object>) e.getValue();
            List<String> text = new ArrayList<>();
            for (String k : List.of("text", "java", "bedrock")) if (t.get(k) instanceof List<?> l) for (Object o : l) text.add(String.valueOf(o));
            check(!text.isEmpty(), e.getKey() + " has text");
            check(t.get("triggers") instanceof List<?> tl && !tl.isEmpty(), e.getKey() + " has triggers");
            for (String s : text) check(s.length() <= 140, e.getKey() + " line short enough: " + s.length());
            for (String k : List.of("text", "java", "bedrock")) if (t.get(k) instanceof List<?> l) check(l.size() <= 4, e.getKey() + " " + k + " at most 4 lines");
            if (e.getKey().equals("seals") || e.getKey().equals("guild-gate")) {
                for (String s : text) for (String w : sealWords) check(!(" " + s.toLowerCase().replaceAll("[^a-z ]", " ") + " ").contains(" " + w + " ") && !s.toLowerCase().contains(w + "s "), e.getKey() + " says nothing about how the fight plays (" + w + ")");
            }
            if (t.get("bedrock") != null) for (Object o : (List<?>) t.get("bedrock")) check(!String.valueOf(o).toLowerCase().contains("click"), e.getKey() + " bedrock text says tap, not click");
            if (t.get("text") != null) for (Object o : (List<?>) t.get("text")) { String s2 = String.valueOf(o).toLowerCase(); check(!s2.contains("click") && !s2.contains(" tap "), e.getKey() + " shared text has no click/tap (use java:/bedrock:)"); }
            if (e.getKey().contains("seal") || String.join(" ", text).toLowerCase().contains("seal")) check(e.getKey().equals("seals") || e.getKey().equals("guild-gate"), e.getKey() + " mentions seals: must go through the seal word check");
            if (t.get("java") != null) for (Object o : (List<?>) t.get("java")) check(!String.valueOf(o).toLowerCase().contains(" tap "), e.getKey() + " java text says click, not tap");
        }
    }

    static void form(String floodgateJar) throws Exception {
        URLClassLoader l = new URLClassLoader(new URL[]{new File(floodgateJar).toURI().toURL()}, TestMain.class.getClassLoader());
        AtomicInteger got = new AtomicInteger(-9);
        Object f = Bedrock.buildSimple(l, "Wand tip", "line one\n\nline two", List.of("OK, got it!", "Remind me later"), got::set);
        check(f != null && f.getClass().getName().contains("SimpleForm"), "Cumulus SimpleForm builds from floodgate.jar: " + (f == null ? null : f.getClass().getName()));
        check(String.valueOf(f.getClass().getMethod("title").invoke(f)).equals("Wand tip"), "form title");
        check(((List<?>) f.getClass().getMethod("buttons").invoke(f)).size() == 2, "two buttons");
    }
}
