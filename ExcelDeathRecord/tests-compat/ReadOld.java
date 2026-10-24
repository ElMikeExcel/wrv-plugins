import com.winkreyes.exceldeathrecord.core.*;
import java.nio.file.*;
/** Compiled against the 0.1.0 core: a 0.2.0 marked record must read exactly like the unmarked one. */
public class ReadOld {
    public static void main(String[] a) throws Exception {
        boolean d = RecordFormat.read(Files.readString(Path.of(a[0], "plain.yml"))).equals(RecordFormat.read(Files.readString(Path.of(a[0], "marked.yml"))));
        boolean b = BurnFormat.read(Files.readString(Path.of(a[0], "burnplain.yml"))).equals(BurnFormat.read(Files.readString(Path.of(a[0], "burnmarked.yml"))));
        System.out.println("0.1.0 reads 0.2.0 death record (marks ignored): " + (d ? "PASS" : "FAIL"));
        System.out.println("0.1.0 reads 0.2.0 burn record (marks ignored): " + (b ? "PASS" : "FAIL"));
        if (!d || !b) System.exit(1);
    }
}
