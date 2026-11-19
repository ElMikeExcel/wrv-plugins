import com.winkreyes.excelitems.core.BuiltinItems;
import com.winkreyes.excelitems.core.ItemRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * The config ID-assignment shape: config.yml pins which key owns
 * which permanent ID, validated at enable. Drift must surface as a
 * problem list, never as a silently re-pointed ID.
 */
public final class AssignmentsTest {

    private static Map<String, String> good() {
        Map<String, String> m = new HashMap<>();
        m.put("1", "veil_potion");
        m.put("2", "deathprotection_basic");
        m.put("3", "deathpotion");
        m.put("4", "revive_potion");
        return m;
    }

    public static void run() {
        System.out.println("AssignmentsTest:");
        ItemRegistry reg = BuiltinItems.registry();

        Check.ok(reg.validateAssignments(good()).isEmpty(), "the shipped assignments validate clean");

        Map<String, String> swapped = good();
        swapped.put("1", "revive_potion");
        Check.eq(1, reg.validateAssignments(swapped).size(), "a swapped key is exactly one problem");

        Map<String, String> unknown = good();
        unknown.put("9", "future_item");
        Check.eq(1, reg.validateAssignments(unknown).size(), "an unknown id is exactly one problem");

        Map<String, String> missing = good();
        missing.remove("3");
        Check.eq(1, reg.validateAssignments(missing).size(), "a missing registered id is exactly one problem");

        Map<String, String> junk = good();
        junk.put("x", "veil_potion");
        Check.ok(reg.validateAssignments(junk).stream().anyMatch(p -> p.contains("not a number")),
                "a non-numeric assignment key is flagged");

        Check.eq(4, reg.validateAssignments(Map.of()).size(), "an empty map flags all four missing ids");
        Check.eq(4, reg.validateAssignments(null).size(), "a null map flags all four missing ids");
    }
}
