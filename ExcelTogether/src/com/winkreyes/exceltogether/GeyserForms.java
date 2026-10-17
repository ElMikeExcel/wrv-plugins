package com.winkreyes.exceltogether;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

/**
 * Bedrock forms through Geyser's own class loader (same approach as ExcelBedrockMenu), so no compile-time
 * Geyser or Cumulus dependency. Building is separate from sending so tests can build real Cumulus forms.
 */
public final class GeyserForms implements Forms {
    private final Logger log;
    public GeyserForms(Logger log) { this.log = log; }

    static ClassLoader geyserLoader() {
        Plugin g = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
        return g != null && g.isEnabled() ? g.getClass().getClassLoader() : null;
    }

    /** True if Geyser says this UUID is a Bedrock player. */
    public static boolean isBedrock(Player pl) {
        try {
            ClassLoader l = geyserLoader();
            if (l == null) return false;
            Class<?> api = Class.forName("org.geysermc.geyser.api.GeyserApi", true, l);
            Object a = api.getMethod("api").invoke(null);
            return Boolean.TRUE.equals(api.getMethod("isBedrockPlayer", UUID.class).invoke(a, pl.getUniqueId()));
        } catch (Throwable t) { return false; }
    }

    // ---- builders (public static for tests) ----
    static Object newBuilder(ClassLoader l, String form) throws Exception {
        return Class.forName("org.geysermc.cumulus.form." + form, true, l).getMethod("builder").invoke(null);
    }
    static Class<?> iface(ClassLoader l, String name) throws Exception { return Class.forName(name, true, l); }
    static void call(Object b, Class<?> type, String m, Class<?>[] sig, Object... args) throws Exception {
        type.getMethod(m, sig).invoke(b, args);
    }


    /** One handler for every outcome: valid reply goes to "valid", closed or invalid goes to "other". */
    static void attach(Object b, Class<?> fb, ClassLoader l, Consumer<Object> valid, Runnable other) throws Exception {
        Class<?> res = iface(l, "org.geysermc.cumulus.response.result.FormResponseResult");
        Class<?> ok = iface(l, "org.geysermc.cumulus.response.result.ValidFormResponseResult");
        Method isValid = res.getMethod("isValid"), response = ok.getMethod("response");
        java.util.function.BiConsumer<Object, Object> h = (form, r) -> {
            try {
                if (Boolean.TRUE.equals(isValid.invoke(r))) { valid.accept(response.invoke(r)); return; }
            } catch (Exception e) { /* fall through to other */ }
            other.run();
        };
        call(b, fb, "resultHandler", new Class<?>[]{java.util.function.BiConsumer.class}, h);
    }

    public static Object buildSimple(ClassLoader l, String title, String content, List<String> buttons, IntConsumer cb) throws Exception {
        Object b = newBuilder(l, "SimpleForm");
        Class<?> bt = iface(l, "org.geysermc.cumulus.form.SimpleForm$Builder");
        Class<?> fb = iface(l, "org.geysermc.cumulus.form.util.FormBuilder");
        Class<?> resp = iface(l, "org.geysermc.cumulus.response.SimpleFormResponse");
        call(b, fb, "title", new Class<?>[]{String.class}, title);
        call(b, bt, "content", new Class<?>[]{String.class}, content);
        for (String s : buttons) call(b, bt, "button", new Class<?>[]{String.class}, s);
        Method id = resp.getMethod("clickedButtonId");
        Consumer<Object> valid = r -> { try { cb.accept((Integer) id.invoke(r)); } catch (Exception e) { cb.accept(-1); } };
        attach(b, fb, l, valid, () -> cb.accept(-1));
        return fb.getMethod("build").invoke(b);
    }

    public static Object buildModal(ClassLoader l, String title, String content, String yes, String no, Consumer<Boolean> cb) throws Exception {
        Object b = newBuilder(l, "ModalForm");
        Class<?> bt = iface(l, "org.geysermc.cumulus.form.ModalForm$Builder");
        Class<?> fb = iface(l, "org.geysermc.cumulus.form.util.FormBuilder");
        Class<?> resp = iface(l, "org.geysermc.cumulus.response.ModalFormResponse");
        call(b, fb, "title", new Class<?>[]{String.class}, title);
        call(b, bt, "content", new Class<?>[]{String.class}, content);
        call(b, bt, "button1", new Class<?>[]{String.class}, yes);
        call(b, bt, "button2", new Class<?>[]{String.class}, no);
        Method res = resp.getMethod("clickedFirst");
        Consumer<Object> valid = r -> { try { cb.accept(Boolean.TRUE.equals(res.invoke(r))); } catch (Exception e) { cb.accept(false); } };
        attach(b, fb, l, valid, () -> cb.accept(false));
        return fb.getMethod("build").invoke(b);
    }

    public static Object buildCustom(ClassLoader l, String title, List<Forms.Field> fields, Consumer<List<Object>> cb) throws Exception {
        Object b = newBuilder(l, "CustomForm");
        Class<?> bt = iface(l, "org.geysermc.cumulus.form.CustomForm$Builder");
        Class<?> fb = iface(l, "org.geysermc.cumulus.form.util.FormBuilder");
        Class<?> resp = iface(l, "org.geysermc.cumulus.response.CustomFormResponse");
        call(b, fb, "title", new Class<?>[]{String.class}, title);
        for (Forms.Field f : fields) {
            switch (f.kind) {
                case INPUT -> call(b, bt, "input", new Class<?>[]{String.class, String.class, String.class}, f.label, f.hint == null ? "" : f.hint, f.def == null ? "" : f.def);
                case DROPDOWN -> call(b, bt, "dropdown", new Class<?>[]{String.class, List.class, int.class}, f.label, f.options, f.defIndex);
                case TOGGLE -> call(b, bt, "toggle", new Class<?>[]{String.class, boolean.class}, f.label, f.defBool);
            }
        }
        Method in = resp.getMethod("asInput", int.class), dd = resp.getMethod("asDropdown", int.class), tg = resp.getMethod("asToggle", int.class);
        Consumer<Object> valid = r -> {
            List<Object> out = new ArrayList<>();
            try {
                for (int i = 0; i < fields.size(); i++) {
                    switch (fields.get(i).kind) {
                        case INPUT -> { Object v = in.invoke(r, i); out.add(v == null ? "" : v); }
                        case DROPDOWN -> out.add(dd.invoke(r, i));
                        case TOGGLE -> out.add(tg.invoke(r, i));
                    }
                }
            } catch (Exception e) { cb.accept(null); return; }
            cb.accept(out);
        };
        attach(b, fb, l, valid, () -> cb.accept(null));
        return fb.getMethod("build").invoke(b);
    }

    // ---- sending ----
    private boolean send(PlayerView p, java.util.concurrent.Callable<Object> builder) {
        try {
            ClassLoader l = geyserLoader();
            if (l == null) return false;
            Class<?> api = Class.forName("org.geysermc.geyser.api.GeyserApi", true, l);
            Object a = api.getMethod("api").invoke(null);
            Object conn = api.getMethod("connectionByUuid", UUID.class).invoke(a, p.id());
            if (conn == null) return false;
            Object form = builder.call();
            for (Method m : conn.getClass().getMethods()) {
                if (m.getName().equals("sendForm") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(form.getClass())) {
                    return Boolean.TRUE.equals(m.invoke(conn, form));
                }
            }
            log.warning("No compatible sendForm on the Geyser connection");
            return false;
        } catch (Throwable t) {
            log.warning("Bedrock form failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return false;
        }
    }

    @Override public boolean simple(PlayerView p, String title, String content, List<String> buttons, IntConsumer cb) {
        ClassLoader l = geyserLoader();
        return l != null && send(p, () -> buildSimple(l, title, content, buttons, cb));
    }
    @Override public boolean modal(PlayerView p, String title, String content, String yes, String no, Consumer<Boolean> cb) {
        ClassLoader l = geyserLoader();
        return l != null && send(p, () -> buildModal(l, title, content, yes, no, cb));
    }
    @Override public boolean custom(PlayerView p, String title, List<Forms.Field> fields, Consumer<List<Object>> cb) {
        ClassLoader l = geyserLoader();
        return l != null && send(p, () -> buildCustom(l, title, fields, cb));
    }
}
