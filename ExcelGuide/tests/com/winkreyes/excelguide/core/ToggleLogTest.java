package com.winkreyes.excelguide.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Player-toggle persistence and the unanswered log (SPEC 6.3 + 9):
 * forgiving parse, stable sorted serialization, append format, cap
 * trimming, and the engine writing entries for questions it missed.
 */
public final class ToggleLogTest {

    private static final UUID A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    public static void main(String[] args) throws Exception {
        TestSupport.T t = new TestSupport.T();

        // PlayerToggles round trip.
        Map<UUID, Boolean> states = Map.of(A, true, B, false);
        String text = PlayerToggles.serialize(states);
        t.eq("serialize sorts by uuid", A + " off\n" + B + " on\n", text);
        t.eq("parse inverts serialize", states, PlayerToggles.parse(text));
        t.eq("empty serializes empty", "", PlayerToggles.serialize(Map.of()));
        Map<UUID, Boolean> messy = PlayerToggles.parse(
                "# comment\n\nnot-a-uuid off\n" + A + " off\n" + B + " banana\n" + A + " extra words here\n");
        t.eq("forgiving parse keeps the good line", Map.of(A, true), messy);

        // UnansweredLog file behavior.
        Path dir = Files.createTempDirectory("excelguide-test");
        Path logFile = dir.resolve("unanswered.log");
        UnansweredLog log = new UnansweredLog(logFile, 10);
        t.eq("fresh log empty", 0, log.count());
        log.append(new UnansweredLog.Entry(Instant.parse("2026-10-18T18:00:00Z"),
                ".OwnerTest", A.toString(), "COMMAND", "how do i tame a fox?", "guide", 3.5));
        t.eq("one entry counted", 1, log.count());
        String line = log.readAll().get(0);
        t.check("line carries the fields", line.contains(".OwnerTest") && line.contains("COMMAND")
                && line.contains("how do i tame a fox?") && line.contains("best=guide"));
        for (int i = 0; i < 14; i++) {
            log.append(new UnansweredLog.Entry(Instant.parse("2026-10-18T18:01:00Z"),
                    "p", B.toString(), "COMMAND", "question " + i, null, 0));
        }
        t.eq("cap trims to max", 10, log.count());
        t.check("cap keeps the newest", log.readAll().get(9).contains("question 13"));
        t.check("cap dropped the oldest", !log.readAll().get(0).contains("tame a fox"));

        // Engine integration: a missed command question is logged.
        TestSupport.FakeStats stats = new TestSupport.FakeStats();
        TestSupport.FakeClock clock = new TestSupport.FakeClock();
        UnansweredLog engineLog = new UnansweredLog(dir.resolve("engine.log"), 100);
        GuideEngine engine = TestSupport.engine(stats, clock, engineLog);
        var r1 = engine.handle(TestSupport.MIKE, "how do i tame a fox",
                GuideEngine.Door.COMMAND, TestSupport.ONLINE, false);
        t.check("miss gets the non-answer", !r1.isEmpty()
                && r1.get(0).text().contains("don't know that one yet"));
        t.eq("non-answer is private", GuideEngine.Channel.PRIVATE, r1.get(0).channel());
        t.eq("miss logged once", 1, engineLog.count());
        t.eq("engine counts unanswered", 1L, engine.unansweredCount());
        t.check("log names the door", engineLog.readAll().get(0).contains("COMMAND"));

        // A plausible ambient question is logged silently.
        clock.advanceSeconds(65);
        var r2 = engine.handle(TestSupport.MIKE, "how do i use the player map",
                GuideEngine.Door.AMBIENT, TestSupport.ONLINE, false);
        t.check("ambient miss stays silent", r2.isEmpty());
        t.eq("ambient plausible question logged", 2, engineLog.count());
        t.check("ambient door recorded", engineLog.readAll().get(1).contains("AMBIENT"));

        // Pure chatter is not logged.
        clock.advanceSeconds(65);
        var before = engineLog.count();
        engine.handle(TestSupport.MIKE, "dinner was great tonight lol",
                GuideEngine.Door.AMBIENT, TestSupport.ONLINE, false);
        t.eq("chatter not logged", before, engineLog.count());

        t.finish("ToggleLogTest");
    }
}
