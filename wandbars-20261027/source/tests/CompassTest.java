import com.winkreyes.excelwandbars.core.CompassDedupe;
import java.util.Arrays;
public class CompassTest {
    public static void run() {
        Check.ok(CompassDedupe.plan(new int[]{0,0,1,0}) == null, "one compass: no change");
        Check.ok(CompassDedupe.plan(new int[]{0,0,0}) == null, "no compass: no change (guard never gives)");
        Check.eq("[0, 1, 0]", Arrays.toString(CompassDedupe.plan(new int[]{0,2,0})), "stack of 2 -> 1");
        Check.eq("[1, 0, 0, 0]", Arrays.toString(CompassDedupe.plan(new int[]{1,0,1,3})), "spread copies -> keep first slot only");
        Check.eq(4, CompassDedupe.removed(new int[]{1,0,1,3}), "removed count");
        Check.eq(0, CompassDedupe.removed(new int[]{0,1}), "removed none");
        Check.ok(CompassDedupe.plan(null) == null, "null safe");
    }
}

