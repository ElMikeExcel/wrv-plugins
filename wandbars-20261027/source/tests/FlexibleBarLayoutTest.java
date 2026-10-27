import com.winkreyes.excelwandbars.core.*;
import java.util.*;

public final class FlexibleBarLayoutTest {
    private static int physical(int logical, int wand) { return logical >= wand ? logical + 1 : logical; }
    public static void run() {
        for (int gap = 0; gap < 8; gap++) {
            Map<String,Integer> desired = new LinkedHashMap<>(), actual = new LinkedHashMap<>();
            for (int n = 0; n < 7; n++) {
                desired.put("s"+n, n);
                actual.put("s"+n, n >= gap ? n + 1 : n);
            }
            desired.put("book", 8); actual.put("book", 0);
            Map<String,Integer> result = FlexibleBarLayout.preserve(desired, actual, 1);
            for (int n = 0; n < 7; n++) Check.eq(result.get("s"+n), actual.get("s"+n), "preserve chosen gap="+gap);
            Check.eq(result.get("book"), 8, "book position unchanged");
            Check.eq(FlexibleBarLayout.preserve(desired, result, 1), result, "reopen/reload stable");
            for (int wand = 0; wand < 9; wand++) {
                Set<Integer> occupied = new HashSet<>(); occupied.add(wand);
                for (var e : result.entrySet()) if (e.getValue() < 8)
                    Check.ok(occupied.add(physical(e.getValue(), wand)), "unique icons with wand="+wand);
                Check.eq(occupied.size(), 8, "exactly one physical empty slot regardless wand position");
            }
            Check.eq(FlexibleBarLayout.preserve(desired, null, 1), desired, "explicit rearrange uses desired choices");
        }
        Map<String,Integer> desired = new LinkedHashMap<>();
        for (int n = 0; n < 100; n++) desired.put("s"+n, n < 21 ? (n/7)*8+n%7 : 24+n-21);
        Map<String,Integer> old = new LinkedHashMap<>();
        for (int n = 0; n < 100; n++) old.put("s"+n, n); // Old layout could have eight filled columns.
        Map<String,Integer> repaired = FlexibleBarLayout.preserve(desired, old, 3);
        Check.eq(repaired.keySet(), desired.keySet(), "all known abilities retained after old full-page repair");
        Check.eq(new HashSet<>(repaired.values()).size(), repaired.size(), "no duplicate positions after migration");
        for (var e : repaired.entrySet()) {
            if (desired.get(e.getKey()) < 24)
                Check.eq(e.getValue()/8, desired.get(e.getKey())/8, "desired page assignment respected");
            else Check.eq(e.getValue(), desired.get(e.getKey()), "book overflow untouched in migration");
        }
        for (int page = 0; page < 3; page++) {
            int count=0;for(int pos:repaired.values())if(pos/8==page)count++;
            Check.eq(count,7,"one gap per repaired page");
        }
        Map<String,Integer> bad = new LinkedHashMap<>();for(int n=0;n<8;n++)bad.put("s"+n,n);
        boolean rejected=false;try{FlexibleBarLayout.preserve(bad,Map.of(),1);}catch(IllegalArgumentException ok){rejected=true;}
        Check.ok(rejected,"reject overfull desired layout rather than delete an ability");
        Check.eq(FlexibleBarLayout.preserve(Map.of(),Map.of(),1),Map.of(),"empty wand safe");
        Map<String,Integer> malformed=Map.of("a",0,"b",1,"c",2);
        Map<String,Integer> result=FlexibleBarLayout.preserve(malformed,Map.of("a",0,"b",0,"c",-1),1);
        Check.eq(result.size(),3,"invalid old duplicate/negative positions repaired without lost spell");
        Check.eq(new HashSet<>(result.values()).size(),3,"repaired positions unique");
    }
}
