// Excel signature - WRV maintained source; original authorship and license notices preserved.
// SPDX-License-Identifier: MIT
package com.winkreyes.shops.log;

import java.lang.reflect.Method;
import java.util.UUID;

/** Java/Bedrock via the Floodgate API when present (reflection: no compile dependency). */
public final class Platform {
    private final Object api; private final Method check;
    private Platform(Object api, Method check) { this.api = api; this.check = check; }
    /** @param floodgateLoader the enabled floodgate plugin's class loader, or null when absent. */
    public static Platform detect(ClassLoader floodgateLoader) {
        if (floodgateLoader == null) return new Platform(null, null);
        try {
            Class<?> c = Class.forName("org.geysermc.floodgate.api.FloodgateApi", true, floodgateLoader);
            Object api = c.getMethod("getInstance").invoke(null);
            if (api == null) return new Platform(null, null);
            return new Platform(api, c.getMethod("isFloodgatePlayer", UUID.class));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException absent) { return new Platform(null, null); }
    }
    public String of(UUID id) {
        if (api == null) return "java";
        try { return Boolean.TRUE.equals(check.invoke(api, id)) ? "bedrock" : "java"; }
        catch (ReflectiveOperationException | RuntimeException failure) { return "unknown"; }
    }
}
