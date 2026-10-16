package com.winkreyes.excelwaypoints;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Moves form replies onto the main server thread before the menu logic runs. */
public final class SyncForms implements Forms {
    private final Forms in; private final Executor main;
    public SyncForms(Forms in, Executor main) { this.in = in; this.main = main; }
    @Override public boolean simple(PlayerView p, String t, String c, List<String> b, IntConsumer cb) {
        return in.simple(p, t, c, b, i -> main.execute(() -> cb.accept(i)));
    }
    @Override public boolean modal(PlayerView p, String t, String c, String y, String n, Consumer<Boolean> cb) {
        return in.modal(p, t, c, y, n, r -> main.execute(() -> cb.accept(r)));
    }
    @Override public boolean custom(PlayerView p, String t, List<Field> f, Consumer<List<Object>> cb) {
        return in.custom(p, t, f, r -> main.execute(() -> cb.accept(r)));
    }
}
