// SPDX-License-Identifier: MIT
package org.geysermc.fixtures;
import java.util.UUID;
import org.geysermc.floodgate.api.FloodgateApi;
/** Original deterministic fixture, not a real-client acceptance test. */
public final class FloodgateFixture {
    public static boolean bedrock, apiAvailable, throwOnLookup;
    public static UUID lastPlayer;
    private static final FloodgateApi API = new HiddenApi();
    private FloodgateFixture() { }
    public static FloodgateApi api() { return apiAvailable ? API : null; }
    public static void reset() { bedrock = true; apiAvailable = true; throwOnLookup = false; lastPlayer = null; }
    private static final class HiddenApi implements FloodgateApi {
        public boolean isFloodgatePlayer(UUID id) {
            lastPlayer = id;
            if (throwOnLookup) throw new IllegalStateException("Fixture detection failure");
            return bedrock;
        }
    }
}
