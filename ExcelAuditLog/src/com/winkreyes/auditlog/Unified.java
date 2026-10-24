package com.winkreyes.auditlog;

import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

/** Merges every source for /audit. Pure file logic here; CoreProtect rows are passed in. */
public final class Unified {
    public static final int PAGE = 10;
    static final DateTimeFormatter SHOW = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");
    public static final List<String> ALL_TYPES;
    static { List<String> t = new ArrayList<>(Sources.FILE_TYPES); for (String c : Sources.CP_TYPES) if (!t.contains(c)) t.add(c); ALL_TYPES = List.copyOf(t); }

    public record Query(String player, int hours, String type, int page) { }

    /** /audit <player> [hours] [type|all] [page N]; returns null with an error in err[0] when bad. */
    public static Query parse(String[] a, String[] err) {
        if (a.length < 1) { err[0] = "Usage: /audit <player> [hours] [type|all] [page N]  |  /audit types"; return null; }
        int hours = 24, page = 1; String type = null;
        for (int i = 1; i < a.length; i++) {
            String x = a[i].toLowerCase(Locale.ROOT);
            if (x.equals("page") && i + 1 < a.length) { try { page = Math.max(1, Integer.parseInt(a[++i])); } catch (NumberFormatException e) { err[0] = "Bad page: " + a[i]; return null; } }
            else if (x.matches("\\d+")) hours = Math.max(1, Math.min(720, Integer.parseInt(x)));
            else if (x.equals("all")) type = null;
            else if (ALL_TYPES.contains(x)) type = x;
            else { err[0] = "Unknown type '" + a[i] + "'. Try /audit types"; return null; }
        }
        return new Query(a[0], hours, type, page);
    }

    /** Which file sources exist right now (for the boot line and /audit types). */
    public static List<String> available(Path plugins, Path root) {
        List<String> s = new ArrayList<>();
        s.add("auditlog");
        if (Files.exists(plugins.resolve("ExcelShops"))) s.add("shop");
        if (Files.exists(plugins.resolve("ExcelMenuStats"))) s.add("menu");
        if (Files.exists(plugins.resolve("ExcelTrash"))) s.add("trash");
        if (Files.isDirectory(plugins.resolve("ExcelDeathRecord/deaths"))) s.add("death");
        if (Files.isDirectory(plugins.resolve("ExcelDeathRecord/burned"))) s.add("burn");
        if (Files.isDirectory(root.resolve("logs"))) s.add("seal");
        return s;
    }

    public static List<Entry> collect(Query q, Path plugins, Path root, ZoneId logZone, long nowMs, Supplier<List<Entry>> coreProtect) {
        long since = nowMs - q.hours() * 3600_000L; String t = q.type();
        List<Entry> all = new ArrayList<>();
        boolean any = t == null;
        if (any || List.of("money", "teleport", "enderchest", "xp", "enchant", "command").contains(t)) all.addAll(Sources.own(plugins.resolve("ExcelAuditLog"), q.player(), since));
        if (any || t.equals("shop")) all.addAll(Sources.shop(plugins, q.player(), since));
        if (any || t.equals("menu")) all.addAll(Sources.menu(plugins, q.player(), since));
        if (any || t.equals("trash")) all.addAll(Sources.trash(plugins, q.player(), since));
        if (any || t.equals("death")) all.addAll(Sources.death(plugins, q.player(), since));
        if (any || t.equals("burn")) all.addAll(Sources.burn(plugins, q.player(), since));
        if (any || t.equals("seal")) all.addAll(Sources.seal(root, q.player(), since, logZone));
        if (coreProtect != null && (any || Sources.CP_TYPES.contains(t))) try { all.addAll(coreProtect.get()); } catch (Throwable ignored) { }
        all.removeIf(e -> e.epochMs() < since || e.epochMs() > nowMs + 60_000 || (t != null && !e.type().equals(t)));
        all.sort(Comparator.comparingLong(Entry::epochMs).reversed());
        return all;
    }

    public static List<String> render(Query q, List<Entry> rows) {
        int pages = Math.max(1, (rows.size() + PAGE - 1) / PAGE), p = Math.min(q.page(), pages);
        List<String> out = new ArrayList<>();
        out.add("Audit " + q.player() + " last " + q.hours() + "h type=" + (q.type() == null ? "all" : q.type()) + ": " + rows.size()
                + " entries, page " + p + "/" + pages + " (newest first, " + AuditWriter.ZONE.getId() + ")");
        for (Entry e : rows.subList((p - 1) * PAGE, Math.min(rows.size(), p * PAGE)))
            out.add(Instant.ofEpochMilli(e.epochMs()).atZone(AuditWriter.ZONE).format(SHOW) + " [" + e.type() + "] " + e.details());
        if (p < pages) out.add("Next: /audit " + q.player() + " " + q.hours() + " " + (q.type() == null ? "all" : q.type()) + " page " + (p + 1));
        return out;
    }
}
