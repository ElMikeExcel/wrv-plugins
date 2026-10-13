// SPDX-License-Identifier: MIT
package org.geysermc.geyser.api;
import org.geysermc.api.GeyserApiBase;
import org.geysermc.fixtures.GeyserFixture;
/** Test-only entrypoint. This must never be included in the production JAR. */
public interface GeyserApi extends GeyserApiBase {
    static GeyserApi api() { return GeyserFixture.api(); }
}
