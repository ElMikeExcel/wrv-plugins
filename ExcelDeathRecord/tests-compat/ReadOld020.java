import com.winkreyes.exceldeathrecord.core.*;
import java.nio.file.*;
/** Compiled against the 0.2.0 core: does 0.2.0 read a 0.3.0 Lost & Found marked record, and keep it marked? */
public class ReadOld020 {
    public static void main(String[] a) throws Exception {
        DestructionRecord plain = BurnFormat.read(Files.readString(Path.of(a[0], "self-plain.yml")));
        DestructionRecord marked = BurnFormat.read(Files.readString(Path.of(a[0], "self-marked.yml")));
        boolean same = plain.ownerUuid.equals(marked.ownerUuid) && plain.item.id.equals(marked.item.id)
                && plain.item.bytesB64.equals(marked.item.bytesB64) && plain.epochMillis == marked.epochMillis;
        boolean isMarked = marked.item.isRestored() && "SELF".equals(marked.item.restoredBy) && "Sam".equals(marked.item.restoredTo);
        boolean plainOpen = !plain.item.isRestored();
        String round = BurnFormat.write(marked);
        boolean roundTrip = BurnFormat.read(round).item.isRestored() && "SELF".equals(BurnFormat.read(round).item.restoredBy);
        System.out.println("0.2.0 reads 0.3.0 burn record (same item): " + (same ? "PASS" : "FAIL"));
        System.out.println("0.2.0 sees the Lost & Found mark (restored, by SELF), so an op restore is refused without force: " + (isMarked ? "PASS" : "FAIL"));
        System.out.println("0.2.0 sees the unrecovered record as open: " + (plainOpen ? "PASS" : "FAIL"));
        System.out.println("0.2.0 rewrite keeps the SELF mark: " + (roundTrip ? "PASS" : "FAIL"));
        System.out.println("0.2.0 summary line: " + BurnIndex.summaryLine(marked));
        if (!same || !isMarked || !plainOpen || !roundTrip) System.exit(1);
    }
}
