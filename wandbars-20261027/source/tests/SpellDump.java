import com.winkreyes.excelwandbars.core.SpellEffects;
import java.util.*;
public final class SpellDump {
    public static void main(String[] a) throws Exception {
        SpellsYml y = new SpellsYml(a[0]);
        for (String k : y.merged.keySet()) {
            System.out.println("== " + k);
            for (String l : SpellEffects.lines(y.get(k))) System.out.println("   " + l);
        }
    }
}

