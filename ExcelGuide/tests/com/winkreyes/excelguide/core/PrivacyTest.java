package com.winkreyes.excelguide.core;

import java.util.List;

/**
 * Privacy and routing rules through the engine (SPEC 3 + 7): personal
 * stats are always private, the wand answer is always private, seals
 * stay public, asking about another player is refused, addressed
 * questions in ambient chat are ignored, /guide off is honored (but
 * /ask still works), and ambient stays quiet right after damage.
 */
public final class PrivacyTest {

    public static void main(String[] args) {
        TestSupport.T t = new TestSupport.T();
        TestSupport.FakeStats stats = new TestSupport.FakeStats();
        TestSupport.FakeClock clock = new TestSupport.FakeClock();
        GuideEngine engine = TestSupport.engine(stats, clock);
        GuideEngine.PlayerRef mike = TestSupport.MIKE;
        GuideEngine.PlayerRef avery = TestSupport.AVERY;

        // Personal stats never leave the asker's ears.
        clock.advanceSeconds(65);
        var money = engine.handle(mike, "what is my balance", GuideEngine.Door.COMMAND,
                TestSupport.ONLINE, false);
        t.check("money answered", !money.isEmpty());
        t.eq("money private", GuideEngine.Channel.PRIVATE, money.get(0).channel());

        clock.advanceSeconds(65);
        var jobs = engine.handle(mike, "what is my job level", GuideEngine.Door.COMMAND,
                TestSupport.ONLINE, false);
        t.check("jobs answered", !jobs.isEmpty());
        t.eq("jobs private", GuideEngine.Channel.PRIVATE, jobs.get(0).channel());

        // The ambient door never answers personal questions at all.
        clock.advanceSeconds(65);
        var ambientMoney = engine.handle(mike, "what is my balance", GuideEngine.Door.AMBIENT,
                TestSupport.ONLINE, false);
        t.check("ambient money silent", ambientMoney.isEmpty());
        var ambientLevel = engine.handle(mike, "whats my level", GuideEngine.Door.AMBIENT,
                TestSupport.ONLINE, false);
        t.check("ambient clarify silent", ambientLevel.isEmpty());

        // Wand help is private even when the whole server sees the ask.
        clock.advanceSeconds(65);
        var wand = engine.handle(mike, "i lost my wand", GuideEngine.Door.AMBIENT,
                TestSupport.ONLINE, false);
        t.check("wand answered in ambient", !wand.isEmpty());
        t.eq("wand answer private", GuideEngine.Channel.PRIVATE, wand.get(0).channel());

        // Seals are public knowledge.
        clock.advanceSeconds(65);
        var seals = engine.handle(mike, "how many seals are left", GuideEngine.Door.AMBIENT,
                TestSupport.ONLINE, false);
        t.check("seals answered", !seals.isEmpty());
        t.eq("seals answer public", GuideEngine.Channel.PUBLIC, seals.get(0).channel());
        t.check("seals count in text", seals.get(0).text().contains("0 of 5"));

        // Asking about another player's stats: polite refusal, private.
        clock.advanceSeconds(65);
        var other = engine.handle(mike, "what is testkite's level", GuideEngine.Door.COMMAND,
                TestSupport.ONLINE, false);
        t.check("other-player stats refused", !other.isEmpty());
        t.eq("refusal private", GuideEngine.Channel.PRIVATE, other.get(0).channel());
        t.check("refusal wording", other.get(0).text().contains("theirs to share"));
        t.check("refusal leaks no numbers", !other.get(0).text().matches(".*\\d{2,}.*"));

        clock.advanceSeconds(65);
        var other2 = engine.handle(mike, "what is ExamplePlayer balance", GuideEngine.Door.SUMMON,
                TestSupport.ONLINE, false);
        t.check("other-player money refused on summon", !other2.isEmpty()
                && other2.get(0).channel() == GuideEngine.Channel.PRIVATE);

        // Questions addressed to another player in ambient chat are ignored.
        clock.advanceSeconds(65);
        var addressed = engine.handle(mike, "testkite what is your level",
                GuideEngine.Door.AMBIENT, TestSupport.ONLINE, false);
        t.check("addressed-to-player ignored", addressed.isEmpty());
        clock.advanceSeconds(65);
        var addressed2 = engine.handle(mike, ".OwnerTest can you tp me",
                GuideEngine.Door.AMBIENT, TestSupport.ONLINE, false);
        t.check("addressed-to-asker-name ignored too", addressed2.isEmpty());

        // /guide off silences ambient + summon for that player only.
        engine.setPlayerOff(avery.id(), true);
        clock.advanceSeconds(65);
        var kAmbient = engine.handle(avery, "how many seals are left", GuideEngine.Door.AMBIENT,
                TestSupport.ONLINE, false);
        t.check("toggled-off ambient silent", kAmbient.isEmpty());
        var kSummon = engine.handle(avery, "how many seals are left", GuideEngine.Door.SUMMON,
                TestSupport.ONLINE, false);
        t.check("toggled-off summon silent", kSummon.isEmpty());
        var kCommand = engine.handle(avery, "how many seals are left", GuideEngine.Door.COMMAND,
                TestSupport.ONLINE, false);
        t.check("/ask still works when off", !kCommand.isEmpty());
        var mAmbient = engine.handle(mike, "the guild is locked", GuideEngine.Door.AMBIENT,
                TestSupport.ONLINE, false);
        t.check("toggle is per player (Mike still answered)", !mAmbient.isEmpty()
                && mAmbient.get(0).channel() == GuideEngine.Channel.PUBLIC);
        engine.setPlayerOff(avery.id(), false);
        t.eq("toggle round trips through text", false, engine.isPlayerOff(avery.id()));

        // Combat quiet: ambient stays silent right after damage.
        clock.advanceSeconds(65);
        var hurt = engine.handle(mike, "i lost my wand", GuideEngine.Door.AMBIENT,
                TestSupport.ONLINE, true);
        t.check("ambient silent after damage", hurt.isEmpty());
        var fine = engine.handle(mike, "i lost my wand", GuideEngine.Door.AMBIENT,
                TestSupport.ONLINE, false);
        t.check("ambient answers when calm", !fine.isEmpty());

        // Unavailable stats never guess.
        stats.nulls = true;
        clock.advanceSeconds(65);
        var cant = engine.handle(mike, "what is my balance", GuideEngine.Door.COMMAND,
                TestSupport.ONLINE, false);
        t.check("unavailable stat admits it", !cant.isEmpty()
                && cant.get(0).text().contains("can't see"));
        stats.nulls = false;

        // Wand flag switches the wording (SPEC 4).
        clock.advanceSeconds(65);
        var fallback = engine.handle(mike, "i lost my wand", GuideEngine.Door.COMMAND,
                TestSupport.ONLINE, false);
        t.check("flag-off wording is the fallback", !fallback.isEmpty()
                && fallback.get(0).text().contains("Mike or Argus"));
        // Flip the flag on a fresh engine built from a modified config.
        GuideConfig on = GuideConfig.fromYaml(TestSupport.readProj("config.yml")
                .replace("wand-self-restore-enabled: false", "wand-self-restore-enabled: true"));
        GuideEngine engine2 = new GuideEngine(on, TestSupport.realIntents(),
                TestSupport.realKnowledge(), stats, clock, null);
        var primary = engine2.handle(mike, "i lost my wand", GuideEngine.Door.COMMAND,
                TestSupport.ONLINE, false);
        t.check("flag-on wording is self-serve", !primary.isEmpty()
                && primary.get(0).text().contains("/wand restore"));

        // wrap(): long answers split to at most 2 lines of at most 200 chars.
        List<GuideEngine.Reply> wrapped = GuideEngine.wrap(GuideEngine.Channel.PUBLIC,
                "x".repeat(500));
        t.eq("long answer is 2 lines", 2, wrapped.size());
        t.check("line length cap", wrapped.stream().allMatch(r -> r.text().length() <= 200));
        t.check("overflow pointer", wrapped.get(1).text().contains("Ask page"));

        t.finish("PrivacyTest");
    }
}
