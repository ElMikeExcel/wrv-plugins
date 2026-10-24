import com.winkreyes.exceldeathrecord.core.*;
import java.nio.file.*;
public class WriteMarked {
    public static void main(String[] a) throws Exception {
        DeathRecord r = RecordFormatTest.deathWithCause("LAVA", "x", "none");
        Files.writeString(Path.of(a[0], "plain.yml"), RecordFormat.write(r));
        r.drops.get(0).restoredTimes = 1; r.drops.get(0).restoredTo = "Sam"; r.drops.get(0).restoredAt = "2026-10-24 00:00:00 UTC";
        r.inventory.get(0).restoredTimes = 1; r.inventory.get(0).restoredTo = "Sam";
        r.xpRestoredTimes = 1; r.xpRestoredTo = "Alex";
        Files.writeString(Path.of(a[0], "marked.yml"), RecordFormat.write(r));
        DestructionRecord d = new DestructionRecord(); d.cause = "DESPAWN"; d.item.id = "minecraft:netherite_shovel"; d.item.count = 1; d.item.bytesB64 = "U0hPVg==";
        Files.writeString(Path.of(a[0], "burnplain.yml"), BurnFormat.write(d));
        d.item.restoredTimes = 1; d.item.restoredTo = "Sam";
        Files.writeString(Path.of(a[0], "burnmarked.yml"), BurnFormat.write(d));
    }
}
