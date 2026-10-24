import com.winkreyes.exceldeathrecord.core.DestructionGuard;

import java.util.UUID;

public class DestructionGuardTest {
    public static void run() {
        System.out.println("DestructionGuardTest");
        DestructionGuard g = new DestructionGuard(60_000);
        UUID a = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID b = UUID.fromString("22222222-2222-2222-2222-222222222222");

        Check.ok(g.claim(a, 1000), "first claim on an entity succeeds");
        Check.ok(!g.claim(a, 1001), "second claim on the same entity is refused (exactly once)");
        Check.ok(g.isClaimed(a, 1002), "isClaimed sees the claim");
        Check.ok(g.claim(b, 1003), "a different entity claims independently");
        Check.eq(2, g.size(), "two live claims held");

        // After the TTL lapses the entry is evicted (an item UUID
        // is never reused while it lives, so a late re-claim can
        // only be a stale path re-firing on a corpse — harmless).
        Check.ok(g.isClaimed(a, 61_000), "claim still held at exactly the TTL edge");
        Check.ok(g.claim(a, 61_001), "claim after the TTL succeeds again (entry evicted)");
        Check.eq(2, g.size(), "expired entry evicted; the fresher claim (b) still held");
    }
}
