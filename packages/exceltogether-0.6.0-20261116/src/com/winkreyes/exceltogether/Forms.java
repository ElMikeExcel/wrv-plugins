package com.winkreyes.exceltogether;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Bedrock form sender. Every method returns false if the form could not be shown (caller falls back to chat). */
public interface Forms {
    /** cb gets the tapped button index, or -1 when the form is closed. */
    boolean simple(PlayerView p, String title, String content, List<String> buttons, IntConsumer cb);
    /** cb gets true for the first button, false for the second or when closed. */
    boolean modal(PlayerView p, String title, String content, String yes, String no, Consumer<Boolean> cb);
    /** Unused by ExcelTogether, kept so the shared GeyserForms code compiles unchanged. */
    boolean custom(PlayerView p, String title, List<Field> fields, Consumer<List<Object>> cb);

    final class Field {
        public enum Kind { INPUT, DROPDOWN, TOGGLE }
        public final Kind kind; public final String label; public final String hint; public final String def;
        public final List<String> options; public final int defIndex; public final boolean defBool;
        private Field(Kind k, String l, String h, String d, List<String> o, int di, boolean db) { kind = k; label = l; hint = h; def = d; options = o; defIndex = di; defBool = db; }
    }
}
