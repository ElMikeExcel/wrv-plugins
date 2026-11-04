package com.winkreyes.excelmenu.core.registry;

import com.winkreyes.excelmenu.core.platform.Platform;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Test viewer: every answer is a map lookup the test controls. */
final class FakeViewer implements ViewerContext {

    Platform platform = Platform.JAVA;
    final Set<String> permissions = new HashSet<>();
    final Set<String> plugins = new HashSet<>();
    final Map<String, String> placeholders = new HashMap<>();

    FakeViewer platform(Platform p) { this.platform = p; return this; }

    FakeViewer grant(String permission) { permissions.add(permission); return this; }

    FakeViewer withPlugin(String name) { plugins.add(name); return this; }

    FakeViewer placeholder(String token, String value) {
        placeholders.put(token, value);
        return this;
    }

    @Override public Platform platform() { return platform; }

    @Override public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    @Override public boolean pluginPresent(String pluginName) {
        return plugins.contains(pluginName);
    }

    @Override public String resolvePlaceholder(String token) {
        return placeholders.get(token);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
