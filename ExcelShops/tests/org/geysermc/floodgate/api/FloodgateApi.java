// SPDX-License-Identifier: MIT
package org.geysermc.floodgate.api;
import java.util.UUID;
import org.geysermc.fixtures.FloodgateFixture;
/** Original test-only public API fixture; never included in the production JAR. */
public interface FloodgateApi {
    static FloodgateApi getInstance() { return FloodgateFixture.api(); }
    boolean isFloodgatePlayer(UUID id);
}
