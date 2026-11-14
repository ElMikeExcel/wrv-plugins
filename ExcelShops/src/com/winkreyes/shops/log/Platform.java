// SPDX-License-Identifier: MIT
package com.winkreyes.shops.log;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Floodgate platform lookup (ExcelShops 1.3.7, reconstructed from the
 * live 1.3.7-analytics-OFFLINE jar — see the 1.3.8 build record).
 * Reflection only, against the floodgate plugin's own class loader,
 * so ExcelShops keeps no hard dependency on Geyser/floodgate:
 * detect() resolves FloodgateApi.isFloodgatePlayer once; of()
 * answers "bedrock", "java", or "unknown" (lookup unavailable or
 * failed) for the purchase log's platform column.
 */
public final class Platform {
    private final Object api;
    private final Method check;

    private Platform(Object api, Method check) {
        this.api = api;
        this.check = check;
    }

    public static Platform detect(ClassLoader loader) {
        if (loader == null) return new Platform(null, null);
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi", true, loader);
            Object api = apiClass.getMethod("getInstance").invoke(null);
            if (api == null) return new Platform(null, null);
            return new Platform(api, apiClass.getMethod("isFloodgatePlayer", UUID.class));
        } catch (LinkageError | RuntimeException | ReflectiveOperationException failure) {
            return new Platform(null, null);
        }
    }

    public String of(UUID playerId) {
        if (api == null) return "java";
        try {
            return Boolean.TRUE.equals(check.invoke(api, playerId)) ? "bedrock" : "java";
        } catch (RuntimeException | ReflectiveOperationException failure) {
            return "unknown";
        }
    }
}
