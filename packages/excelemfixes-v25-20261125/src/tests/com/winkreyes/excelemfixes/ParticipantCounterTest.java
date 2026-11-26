import excelemfixes.partydrops.ParticipantCounter;
import excelemfixes.partydrops.ParticipantCounter.Candidate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Participant filter (spec §2, §9): alive, online, same world, in range, not spectator; distinct UUIDs; cap. */
public class ParticipantCounterTest {

    static final UUID A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    // A Floodgate Bedrock UUID shape (00000000-0000-0000-0009-...) — a real player, counts normally.
    static final UUID BEDROCK = UUID.fromString("00000000-0000-0000-0009-01f05d0d739d");

    static Candidate at(UUID id, double dx) {
        return new Candidate(id, true, true, false, true, dx, 0, 0);
    }

    public static void run() {
        System.out.println("ParticipantCounterTest");
        double r = 50.0;

        Check.eq(0, ParticipantCounter.count(List.of(), r, 5), "nobody online -> 0");
        Check.eq(0, ParticipantCounter.count(null, r, 5), "null list -> 0");
        Check.eq(1, ParticipantCounter.count(List.of(at(A, 0)), r, 5), "killer alone at the corpse -> 1");
        Check.eq(2, ParticipantCounter.count(List.of(at(A, 0), at(B, 49.9)), r, 5), "two in range -> 2");
        Check.eq(1, ParticipantCounter.count(List.of(at(A, 0), at(B, 50.0001)), r, 5), "second a hair past 50 -> 1");
        Check.eq(2, ParticipantCounter.count(List.of(at(A, 0), at(B, 50.0)), r, 5), "second at exactly 50 -> 2");

        // exclusions, one at a time
        Check.eq(1, ParticipantCounter.count(List.of(at(A, 0),
                new Candidate(B, true, false, false, true, 0, 0, 0)), r, 5), "dead player excluded");
        Check.eq(1, ParticipantCounter.count(List.of(at(A, 0),
                new Candidate(B, true, true, true, true, 0, 0, 0)), r, 5), "spectator excluded");
        Check.eq(1, ParticipantCounter.count(List.of(at(A, 0),
                new Candidate(B, false, true, false, true, 0, 0, 0)), r, 5), "offline snapshot excluded");
        Check.eq(1, ParticipantCounter.count(List.of(at(A, 0),
                new Candidate(B, true, true, false, false, 0, 0, 0)), r, 5), "other world excluded");

        // Bedrock counts; duplicates do not
        Check.eq(2, ParticipantCounter.count(List.of(at(A, 0), at(BEDROCK, 10)), r, 5),
                "Bedrock (Floodgate) player counts as a normal participant");
        Check.eq(1, ParticipantCounter.count(List.of(at(A, 0), at(A, 5)), r, 5),
                "same UUID twice counts once");

        // cap: seven distinct in-range players count as 5
        List<Candidate> crowd = new ArrayList<>();
        for (int i = 0; i < 7; i++) crowd.add(at(UUID.randomUUID(), i));
        Check.eq(5, ParticipantCounter.count(crowd, r, 5), "7 in range, cap 5 -> 5");
        Check.eq(7, ParticipantCounter.count(crowd, r, 20), "cap 20 lets all 7 count (config plumbing)");

        // eligible() returns the actual UUID set for the hint pass, in order, capped
        List<UUID> elig = ParticipantCounter.eligible(List.of(at(B, 10), at(A, 0), at(BEDROCK, 20)), r, 5);
        Check.eq(List.of(B, A, BEDROCK), elig, "eligible returns distinct UUIDs in snapshot order");
    }
}
