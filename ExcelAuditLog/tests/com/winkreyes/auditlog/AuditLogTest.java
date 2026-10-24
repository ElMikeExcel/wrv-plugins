package com.winkreyes.auditlog;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditLogTest {
    @Test void csvRoundTrip() {
        String r = Csv.row("a,b", "q\"x", "plain", "line\nbreak");
        assertEquals(List.of("a,b", "q\"x", "plain", "line break"), Csv.parse(r));
    }

    @Test void diffShowsAddsAndRemoves() {
        String d = ItemDiff.diff(Map.of("DIAMOND", 5, "NETHERITE_SHOVEL {efficiency 5}", 1), Map.of("DIAMOND", 8));
        assertEquals("+3 DIAMOND; -1 NETHERITE_SHOVEL {efficiency 5}", d);
        assertEquals("", ItemDiff.diff(Map.of("DIRT", 2), Map.of("DIRT", 2)));
    }

    @Test void writesDailyFileAndQueries() throws Exception {
        File dir = Files.createTempDirectory("audit").toFile();
        AuditWriter w = new AuditWriter(dir);
        w.add("Jester_Turtle", "u1", "Java", "money", "+25.00 balance=100.00 recent=/pay x 25");
        w.add("Marvin", "u2", "Bedrock", "teleport", "cause=COMMAND from=world 1 2 3 to=world 4 5 6");
        w.add("Jester_Turtle", "u1", "Java", "teleport", "cause=PLUGIN");
        w.flush();
        File f = AuditWriter.fileFor(dir, ZonedDateTime.now(AuditWriter.ZONE));
        List<String> lines = Files.readAllLines(f.toPath());
        assertEquals(AuditWriter.HEADER, lines.get(0));
        assertEquals(4, lines.size());
        assertTrue(lines.get(1).contains("CDT") || lines.get(1).contains("CST"));
        assertEquals(2, w.query("jester_turtle", 1, null, 20).size());
        assertEquals(1, w.query("Jester_Turtle", 1, "tele", 20).size());
        assertEquals(1, w.query("Marvin", 1, null, 20).size());
        w.add("Marvin", "u2", "Bedrock", "xp_level", "1 -> 2"); w.flush();
        assertEquals(4 + 1, Files.readAllLines(f.toPath()).size()); // header written once
    }
}
