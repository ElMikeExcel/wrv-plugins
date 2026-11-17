package com.winkreyes.auditlog;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * Read-only parsers for every file-based audit source. Each takes the plugins/ folder (or server root),
 * a player name and a lower time bound, and returns matching entries. A missing file or folder yields
 * an empty list; a malformed line is skipped. Formats are taken from each plugin's source.
 */
public final class Sources {
    private Sources() {}

    public static final List<String> CP_TYPES = List.of("block", "container", "item", "chat", "command");
    public static final List<String> FILE_TYPES = List.of("money", "teleport", "enderchest", "xp", "enchant", "command",
            "shop", "menu", "trash", "death", "burn", "seal");

    static boolean same(String a, String b) {
        if (a == null || b == null) return false;
        String x = a.startsWith(".") ? a.substring(1) : a, y = b.startsWith(".") ? b.substring(1) : b;
        return x.equalsIgnoreCase(y);
    }

    static List<String> lines(Path f) {
        try { return Files.exists(f) ? Files.readAllLines(f, StandardCharsets.UTF_8) : List.of(); }
        catch (IOException e) { return List.of(); }
    }

    /** ExcelAuditLog's own audit-YYYY-MM-DD.csv: time,player,uuid,platform,event,details */
    public static List<Entry> own(Path dir, String player, long since) {
        List<Entry> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) return out;
        try (Stream<Path> s = Files.list(dir)) {
            for (Path f : s.filter(p -> p.getFileName().toString().matches("audit-\\d{4}-\\d{2}-\\d{2}\\.csv")).sorted().toList())
                for (String line : lines(f)) {
                    if (line.startsWith("time,")) continue;
                    List<String> r = Csv.parse(line);
                    if (r.size() < 6 || !same(r.get(1), player)) continue;
                    try {
                        long t = ZonedDateTime.parse(r.get(0), AuditWriter.TS).toInstant().toEpochMilli();
                        if (t >= since) out.add(new Entry(t, ownType(r.get(4)), r.get(1), r.get(4) + " " + r.get(5)));
                    } catch (RuntimeException bad) { }
                }
        } catch (IOException ignored) { }
        return out;
    }
    static String ownType(String event) {
        if (event.startsWith("enderchest")) return "enderchest";
        if (event.startsWith("xp")) return "xp";
        return event;
    }

    /** ExcelShops 1.3.5 purchases.csv: timestamp,player,uuid,platform,shop,item,qty,bundle_size,unit_price,total,balance_after,offer_id */
    public static List<Entry> shop(Path plugins, String player, long since) {
        List<Entry> out = new ArrayList<>();
        for (String line : lines(plugins.resolve("ExcelShops/purchases.csv"))) {
            if (line.startsWith("timestamp,")) continue;
            List<String> f = Csv.parse(line);
            if (f.size() < 12 || !same(unq(f.get(1)), player)) continue;
            try {
                long t = OffsetDateTime.parse(f.get(0)).toInstant().toEpochMilli();
                if (t < since) continue;
                out.add(new Entry(t, "shop", unq(f.get(1)), "bought " + f.get(6) + "x" + f.get(7) + " " + f.get(5) + " for $" + f.get(9)
                        + " (" + f.get(4) + ")" + (f.get(10).isEmpty() ? "" : " bal $" + f.get(10))));
            } catch (RuntimeException bad) { }
        }
        return out;
    }
    /** Undo the leading ' that ExcelShops/ExcelMenuStats add to formula-looking names. */
    static String unq(String s) { return s.startsWith("'") && s.length() > 1 && "=+-@".indexOf(s.charAt(1)) >= 0 ? s.substring(1) : s; }

    /** ExcelMenuStats clicks.csv: timestamp,player,uuid,platform,menu,button,slot,click */
    public static List<Entry> menu(Path plugins, String player, long since) {
        List<Entry> out = new ArrayList<>();
        for (String line : lines(plugins.resolve("ExcelMenuStats/clicks.csv"))) {
            if (line.startsWith("timestamp,")) continue;
            List<String> f = Csv.parse(line);
            if (f.size() < 8 || !same(unq(f.get(1)), player)) continue;
            try {
                long t = OffsetDateTime.parse(f.get(0)).toInstant().toEpochMilli();
                if (t >= since) out.add(new Entry(t, "menu", unq(f.get(1)), f.get(4) + " > " + f.get(5) + " (" + f.get(3) + " " + f.get(7) + ")"));
            } catch (RuntimeException bad) { }
        }
        return out;
    }

    /** ExcelTrash trash.log: "<ZonedDateTime.toString()> TRASH|UNDO player=X uuid=U stacks=N items=N [detail]" */
    public static List<Entry> trash(Path plugins, String player, long since) {
        List<Entry> out = new ArrayList<>();
        for (String line : lines(plugins.resolve("ExcelTrash/trash.log"))) {
            String[] p = line.split(" ", 4);
            if (p.length < 4 || !p[2].startsWith("player=") || !same(p[2].substring(7), player)) continue;
            try {
                long t = ZonedDateTime.parse(p[0]).toInstant().toEpochMilli();
                if (t < since) continue;
                String rest = p[3].replaceFirst("^uuid=\\S+ ?", "");
                out.add(new Entry(t, "trash", p[2].substring(7), p[1].toLowerCase(Locale.ROOT) + " " + rest));
            } catch (RuntimeException bad) { }
        }
        return out;
    }

    /** ExcelDeathRecord snapshot/burn-ledger v1 files: top-level "key: value" lines, values quoted with \" escapes. */
    static Map<String, String> topLevel(Path f) {
        Map<String, String> m = new LinkedHashMap<>(); String section = null; int items = 0;
        for (String line : lines(f)) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (Character.isWhitespace(line.charAt(0))) { if (line.trim().startsWith("id:") && section != null) m.merge(section + "#", "1", (a, b) -> Integer.toString(Integer.parseInt(a) + 1)); continue; }
            int c = line.indexOf(':'); if (c < 0) continue;
            String k = line.substring(0, c).trim(), v = line.substring(c + 1).trim();
            if (v.isEmpty()) { section = k; continue; }
            section = null;
            if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
            m.put(k, v);
        }
        return m;
    }
    static List<Path> ymls(Path root) {
        if (!Files.isDirectory(root)) return List.of();
        try (Stream<Path> s = Files.walk(root, 2)) { return s.filter(p -> p.toString().endsWith(".yml")).sorted().toList(); }
        catch (IOException e) { return List.of(); }
    }
    static String loc(Map<String, String> m) {
        return m.getOrDefault("world", "?") + " " + num(m.get("x")) + " " + num(m.get("y")) + " " + num(m.get("z"));
    }
    static String num(String s) { try { return Long.toString((long) Math.floor(Double.parseDouble(s))); } catch (RuntimeException e) { return "?"; } }

    public static List<Entry> death(Path plugins, String player, long since) {
        List<Entry> out = new ArrayList<>();
        for (Path f : ymls(plugins.resolve("ExcelDeathRecord/deaths"))) {
            Map<String, String> m = topLevel(f);
            if (!same(m.get("player"), player)) continue;
            try {
                long t = Long.parseLong(m.get("epoch-millis"));
                if (t < since) continue;
                out.add(new Entry(t, "death", m.get("player"), m.getOrDefault("death-message", "died") + " | killer " + m.getOrDefault("killer", "none")
                        + " | " + m.getOrDefault("damage-cause", "?") + " at " + loc(m) + " | drops " + m.getOrDefault("drops#", "0")
                        + " | lvl " + m.getOrDefault("xp-level", "?") + " | file " + plugins.relativize(f)));
            } catch (RuntimeException bad) { }
        }
        return out;
    }

    public static List<Entry> burn(Path plugins, String player, long since) {
        List<Entry> out = new ArrayList<>();
        for (Path f : ymls(plugins.resolve("ExcelDeathRecord/burned"))) {
            Map<String, String> m = topLevel(f);
            if (!same(m.get("owner"), player)) continue;
            try {
                long t = Long.parseLong(m.get("epoch-millis"));
                if (t < since) continue;
                String id = "?"; for (String l : lines(f)) if (l.trim().startsWith("id:")) { id = l.trim().substring(3).trim().replace("\"", ""); break; }
                String count = "1"; for (String l : lines(f)) if (l.trim().startsWith("count:")) { count = l.trim().substring(6).trim(); break; }
                out.add(new Entry(t, "burn", m.get("owner"), count + "x " + id + " destroyed by " + m.getOrDefault("cause", "?") + " at " + loc(m)
                        + " | file " + plugins.relativize(f)));
            } catch (RuntimeException bad) { }
        }
        return out;
    }

    static final Pattern LOG = Pattern.compile("^\\[(\\d{2}):(\\d{2}):(\\d{2})(?: [A-Z]+)?\\]:? ?(?:\\[[^\\]]*\\]:? )?\\[SealLog\\] (.*)$");
    static final Pattern LOG_ANY_TIME = Pattern.compile("^\\[(\\d{2}):(\\d{2}):(\\d{2})");
    static final Pattern GZ = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})-\\d+\\.log\\.gz");

    /**
     * ExcelSealLog lines ([SealLog] KIND key=value ...) from logs/latest.log and logs/YYYY-MM-DD-N.log.gz.
     * Log line times have no date: the date comes from the file name (gz) or the file's modified date
     * (latest.log), stepping a day at each midnight wrap. zone = the console log's clock (UTC on Shockbyte).
     */
    public static List<Entry> seal(Path serverRoot, String player, long since, ZoneId zone) {
        List<Entry> out = new ArrayList<>();
        Path logs = serverRoot.resolve("logs");
        if (!Files.isDirectory(logs)) return out;
        LocalDate sinceDay = Instant.ofEpochMilli(since).atZone(zone).toLocalDate().minusDays(1);
        try (Stream<Path> s = Files.list(logs)) {
            for (Path f : s.sorted().toList()) {
                String n = f.getFileName().toString(); Matcher g = GZ.matcher(n);
                List<String> ls; LocalDate start;
                if (g.matches()) {
                    start = LocalDate.parse(g.group(1)); if (start.isBefore(sinceDay)) continue;
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(f)), StandardCharsets.UTF_8))) { ls = r.lines().toList(); }
                    catch (IOException e) { continue; }
                } else if (n.equals("latest.log")) {
                    ls = lines(f);
                    LocalDate end; try { end = Files.getLastModifiedTime(f).toInstant().atZone(zone).toLocalDate(); } catch (IOException e) { continue; }
                    start = end.minusDays(wraps(ls));
                } else continue;
                parseSeal(ls, start, zone, player, since, out);
            }
        } catch (IOException ignored) { }
        return out;
    }
    static int wraps(List<String> ls) {
        int w = 0, last = -1;
        for (String l : ls) { Matcher m = LOG_ANY_TIME.matcher(l); if (!m.find()) continue; int sec = secs(m); if (sec < last) w++; last = sec; }
        return w;
    }
    static int secs(Matcher m) { return Integer.parseInt(m.group(1)) * 3600 + Integer.parseInt(m.group(2)) * 60 + Integer.parseInt(m.group(3)); }
    static void parseSeal(List<String> ls, LocalDate start, ZoneId zone, String player, long since, List<Entry> out) {
        LocalDate day = start; int last = -1;
        for (String l : ls) {
            Matcher any = LOG_ANY_TIME.matcher(l); if (!any.find()) continue;
            int sec = secs(any); if (sec < last) day = day.plusDays(1); last = sec;
            Matcher m = LOG.matcher(l); if (!m.matches()) continue;
            String body = m.group(4);
            if (!mentions(body, player)) continue;
            long t = day.atStartOfDay(zone).plusSeconds(sec).toInstant().toEpochMilli();
            if (t >= since) out.add(new Entry(t, "seal", player, body));
        }
    }
    /** True when the player appears as a value of player=, players=, killer=, winners= (comma lists). */
    static boolean mentions(String body, String player) {
        for (String tok : body.split(" ")) {
            int e = tok.indexOf('='); if (e < 0) continue;
            String k = tok.substring(0, e);
            if (!(k.equals("player") || k.equals("players") || k.equals("killer") || k.equals("winners") || k.equals("nearby"))) continue;
            for (String v : tok.substring(e + 1).split(",")) if (same(v, player)) return true;
        }
        return false;
    }
}
