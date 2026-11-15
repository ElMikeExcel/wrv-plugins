// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses one click-action line, e.g.
 *   "[console] excelcosmetics buy %player_name% color pink"
 *   "[message] &7Done<delay=100>"
 * The bracket vocabulary matches the DeluxeMenus lines the live
 * menus were written in, so migrated definitions kept every click
 * string byte-for-byte and this parser is the compatibility layer.
 * An unknown tag is a parse failure — never a silent no-op.
 */
public final class ActionParser {

    public enum Kind {
        CONSOLE, PLAYER, COMMANDEVENT, MESSAGE, OPEN_MENU, CLOSE,
        REFRESH, JSON, SOUND, BROADCAST_SOUND, CONNECT
    }

    public record Action(Kind kind, String body, int delayTicks) {}

    public static final class ActionParseException extends Exception {
        public ActionParseException(String message) { super(message); }
    }

    private static final Map<String, Kind> TAGS = Map.ofEntries(
            Map.entry("console", Kind.CONSOLE),
            Map.entry("player", Kind.PLAYER),
            Map.entry("commandevent", Kind.COMMANDEVENT),
            Map.entry("message", Kind.MESSAGE),
            Map.entry("openguimenu", Kind.OPEN_MENU),
            Map.entry("close", Kind.CLOSE),
            Map.entry("refresh", Kind.REFRESH),
            Map.entry("json", Kind.JSON),
            Map.entry("sound", Kind.SOUND),
            Map.entry("broadcastsound", Kind.BROADCAST_SOUND),
            Map.entry("connect", Kind.CONNECT));

    private static final Pattern DELAY = Pattern.compile("<delay=(\\d+)>\\s*$");

    private ActionParser() {}

    public static boolean isKnownTag(String tag) {
        return TAGS.containsKey(tag.toLowerCase(Locale.ROOT));
    }

    public static Action parse(String line) throws ActionParseException {
        if (line == null) throw new ActionParseException("null action line");
        String s = line.trim();
        int delay = 0;
        Matcher m = DELAY.matcher(s);
        if (m.find()) {
            delay = Integer.parseInt(m.group(1));
            s = s.substring(0, m.start()).trim();
        }
        if (!s.startsWith("[")) {
            throw new ActionParseException("action line has no [tag]: '" + line + "'");
        }
        int close = s.indexOf(']');
        if (close < 0) {
            throw new ActionParseException("action line has an unclosed [tag: '" + line + "'");
        }
        String tag = s.substring(1, close).toLowerCase(Locale.ROOT);
        Kind kind = TAGS.get(tag);
        if (kind == null) {
            throw new ActionParseException("unknown action tag [" + tag + "] in '" + line + "'");
        }
        String body = s.substring(close + 1).trim();
        if (kind != Kind.CLOSE && kind != Kind.REFRESH && body.isEmpty()) {
            throw new ActionParseException("action [" + tag + "] needs an argument: '" + line + "'");
        }
        return new Action(kind, body, delay);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
