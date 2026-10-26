import com.winkreyes.excelwandbars.core.PageLayout;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 0.2.5: every unlocked spell on the hotbar, in pages (pack, page count, page line). */
public final class PageTest {
    public static void run() {
        Check.eq(1, PageLayout.pages(0, 7), "pages 0 spells");
        Check.eq(1, PageLayout.pages(6, 7), "pages 6 @7");
        Check.eq(1, PageLayout.pages(7, 7), "pages 7 @7");
        Check.eq(2, PageLayout.pages(8, 7), "pages 8 @7");
        Check.eq(2, PageLayout.pages(12, 7), "pages 12 @7 (student)");
        Check.eq(3, PageLayout.pages(15, 7), "pages 15 @7 (apprentice)");
        Check.eq(2, PageLayout.pages(15, 8), "pages 15 @8 (sneak bars)");
        Check.eq(1, PageLayout.clampPerPage(0), "clamp 0");
        Check.eq(8, PageLayout.clampPerPage(99), "clamp 99");

        // A wand upgraded over time: gaps and one spell parked past the last bar.
        Map<String, Integer> cur = new HashMap<>();
        cur.put("heal", 0); cur.put("boon", 3); cur.put("mend", 1); cur.put("feed", 17);
        List<String> spells = Arrays.asList("feed", "Heal", "boon", "mend", "light", "heal");
        Check.eq(Arrays.asList("heal", "mend", "boon", "feed", "light"), PageLayout.order(cur, spells), "order by slot then loose by name");
        Map<String, Integer> p7 = PageLayout.pack(cur, spells, 7);
        Check.eq(0, p7.get("heal"), "packed heal");
        Check.eq(4, p7.get("light"), "packed light");
        Check.eq(5, p7.size(), "packed size (no spell hidden or doubled)");
        Check.ok(!PageLayout.same(cur, p7), "not same before");
        Check.ok(PageLayout.same(new HashMap<>(p7), p7), "same after");

        // 9 spells at 7 per page: 7 on page 1 (slots 0-6, slot 7 empty), 2 on page 2 (slots 8, 9).
        List<String> nine = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i");
        Map<String, Integer> p = PageLayout.pack(null, nine, 7);
        Check.eq(6, p.get("g"), "g on page 1 slot 7");
        Check.eq(8, p.get("h"), "h first on page 2");
        Check.eq(9, p.get("i"), "i page 2");
        Check.ok(!p.containsValue(7), "slot 7 (the 8th) left empty");
        Map<String, Integer> p8 = PageLayout.pack(null, nine, 8);
        Check.eq(7, p8.get("h"), "8 per page: h in slot 8th");
        Check.eq(8, p8.get("i"), "8 per page: i page 2");

        String line = PageLayout.pageLine(0, 2, Arrays.asList("Heal", "Mend"));
        Check.ok(line.startsWith("Page 1/2"), "page line starts Page 1/2: " + line);
        Check.ok(!line.contains("\u2014"), "page line no em dash");
    }
}
