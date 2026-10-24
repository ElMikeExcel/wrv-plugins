import com.winkreyes.exceldeathrecord.core.RecordName;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

public class RecordNameTest {
    public static void run() {
        System.out.println("RecordNameTest");
        ZoneId chicago = ZoneId.of("America/Chicago");
        long epoch = LocalDateTime.of(2026, 10, 22, 21, 24, 4).atZone(chicago).toInstant().toEpochMilli();

        Check.eq("2026-10-22_212404", RecordName.baseName(epoch, chicago), "base name formats the local second");
        Check.eq("2026-10-22 21:24:04", RecordName.timeText(epoch, chicago), "time text formats for humans");
        Check.eq("2026-10-23_022404", RecordName.baseName(epoch, ZoneId.of("UTC")), "same instant, UTC zone");

        Check.eq("2026-10-22_212404.yml", RecordName.unique("2026-10-22_212404", Set.of()), "no collision: plain name");
        Check.eq("2026-10-22_212404-2.yml",
            RecordName.unique("2026-10-22_212404", Set.of("2026-10-22_212404.yml")),
            "one collision: -2 suffix");
        Check.eq("2026-10-22_212404-3.yml",
            RecordName.unique("2026-10-22_212404", Set.of("2026-10-22_212404.yml", "2026-10-22_212404-2.yml")),
            "two collisions: -3 suffix");
        Check.eq("2026-10-22_212404.yml",
            RecordName.unique("2026-10-22_212404", Set.of("2026-10-22_212405.yml", "other.yml")),
            "unrelated names do not collide");
    }
}
