package com.winkreyes.exceltelemetry;

import com.winkreyes.exceltelemetry.core.Json;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JSON writer: escaping and number shaping the Python tooling relies on. */
public class JsonTest {
    static int fails = 0;
    static void check(boolean c, String m) {
        if (!c) { fails++; System.out.println("FAIL " + m); } else System.out.println("ok   " + m);
    }

    public static void main(String[] args) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("plain", "zombie");
        m.put("quoted", "say \"hi\" \\ now");
        m.put("newline", "a\nb");
        m.put("whole", 40.0);
        m.put("frac", 12.5);
        m.put("ints", 7);
        m.put("bool", true);
        m.put("nothing", null);
        m.put("list", List.of("x", 1.5));
        String out = Json.write(m);
        check(out.contains("\"plain\":\"zombie\""), "plain string");
        check(out.contains("say \\\"hi\\\" \\\\ now"), "quotes and backslashes escaped");
        check(out.contains("a\\nb"), "newline escaped, never literal");
        check(out.contains("\"whole\":40"), "40.0 trims to 40");
        check(out.contains("\"frac\":12.5"), "fractions preserved");
        check(out.contains("\"nothing\":null"), "null literal");
        check(out.contains("\"list\":[\"x\",1.5]"), "nested list");
        check(!out.contains("\n"), "a line is a single physical line (JSONL contract)");

        check(Json.write(Double.NaN).equals("0"), "NaN can never poison a line");
        check(Json.round1(2.25) == 2.3 || Json.round1(2.25) == 2.2, "round1 sane");

        System.out.println(fails == 0 ? "ALL PASS" : "FAILED " + fails);
        if (fails > 0) System.exit(1);
    }
}
