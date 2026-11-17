package com.winkreyes.auditlog;

import java.util.*;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import net.coreprotect.api.LookupOptions;
import net.coreprotect.api.result.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * CoreProtect via its official API (v10+ LookupOptions, chat/command lookups). Only loaded when
 * CoreProtect is enabled, so the class never resolves without it. Call off the main thread.
 */
final class CoreProtectSource {
    static final List<String> TYPES = Sources.CP_TYPES;
    private CoreProtectSource() {}

    static CoreProtectAPI api() {
        Plugin p = Bukkit.getPluginManager().getPlugin("CoreProtect");
        if (!(p instanceof CoreProtect cp) || !p.isEnabled()) return null;
        CoreProtectAPI a = cp.getAPI();
        return a != null && a.isEnabled() && a.APIVersion() >= 10 ? a : null;
    }
    static String status() {
        Plugin p = Bukkit.getPluginManager().getPlugin("CoreProtect");
        if (p == null || !p.isEnabled()) return null;
        try { CoreProtectAPI a = api(); return a == null ? null : "coreprotect(api " + a.APIVersion() + ")"; } catch (Throwable t) { return null; }
    }

    static List<Entry> lookup(String player, int seconds, Set<String> want, int cap) {
        List<Entry> out = new ArrayList<>();
        CoreProtectAPI a = api(); if (a == null) return out;
        LookupOptions o = LookupOptions.builder().user(player).time(seconds).limit(0, cap).build();
        if (want.contains("block")) add(out, a.blockLookup(o), "block", r -> ((BlockResult) r).getType() == null ? "entity" : ((BlockResult) r).getType().name());
        if (want.contains("container")) add(out, a.containerLookup(o), "container", r -> ((ContainerResult) r).getAmount() + "x " + ((ContainerResult) r).getType());
        if (want.contains("item")) add(out, a.itemLookup(o), "item", r -> ((ItemResult) r).getAmount() + "x " + ((ItemResult) r).getType());
        if (want.contains("chat")) add(out, a.chatLookup(player, seconds), "chat", r -> ((MessageResult) r).getMessage());
        if (want.contains("command")) add(out, a.commandLookup(player, seconds), "command", r -> ((MessageResult) r).getMessage());
        return out;
    }
    private static void add(List<Entry> out, List<? extends CoreProtectResult> rs, String type, java.util.function.Function<CoreProtectResult, String> what) {
        if (rs == null) return;
        for (CoreProtectResult r : rs) {
            String w; try { w = what.apply(r); } catch (RuntimeException e) { w = "?"; }
            out.add(new Entry(r.getTimestamp(), type, r.getPlayer(), r.getActionString() + " " + w + " at " + r.worldName() + " " + r.getX() + " " + r.getY() + " " + r.getZ() + " [CoreProtect]"));
        }
    }
}
