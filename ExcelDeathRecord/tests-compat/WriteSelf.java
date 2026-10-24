import com.winkreyes.exceldeathrecord.core.*;
import java.nio.file.*;
/** 0.3.0 side of the rollback check: a Burn Ledger record before and after a Lost & Found recovery. */
public class WriteSelf {
    public static void main(String[] a) throws Exception {
        DestructionRecord d = new DestructionRecord();
        d.ownerUuid = new java.util.UUID(0L, 10L).toString(); d.ownerName = "Sam";
        d.epochMillis = 1_800_000_000_000L; d.timeText = "2027-01-15 02:00:00"; d.cause = "DESPAWN";
        d.world = "minecraft:overworld"; d.x = 1; d.y = 64; d.z = -2; d.ageSeconds = 300;
        d.item.slot = "destroyed"; d.item.id = "minecraft:diamond_sword"; d.item.count = 1; d.item.damage = 0;
        d.item.bytesB64 = "U1dPUkQ=";
        Files.writeString(Path.of(a[0], "self-plain.yml"), BurnFormat.write(d));
        d.item.restoredTimes = 1; d.item.restoredTo = "Sam"; d.item.restoredToUuid = d.ownerUuid;
        d.item.restoredBy = "SELF"; d.item.restoredEpoch = 1_800_000_100_000L; d.item.restoredAt = RestoreCsv.markTime(1_800_000_100_000L);
        Files.writeString(Path.of(a[0], "self-marked.yml"), BurnFormat.write(d));
        LostFoundState st = new LostFoundState(); st.freeUsed = true; st.recoveries = 1;
        Files.writeString(Path.of(a[0], "state.yml"), LostFoundState.write(st));
    }
}
