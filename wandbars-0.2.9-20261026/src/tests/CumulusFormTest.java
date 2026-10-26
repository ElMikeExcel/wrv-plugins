import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/** Builds a real Cumulus SimpleForm with the same reflection calls BedrockForms uses (Cumulus from the floodgate jar). */
public final class CumulusFormTest {
    public static void run(String fg) throws Exception {
        System.out.println("CumulusFormTest");
        if (fg == null || fg.isEmpty() || !new File(fg).exists()) { System.out.println("  SKIP (no floodgate jar)"); return; }
        try (URLClassLoader l = new URLClassLoader(new URL[]{new File(fg).toURI().toURL()}, CumulusFormTest.class.getClassLoader())) {
            Object b = Class.forName("org.geysermc.cumulus.form.SimpleForm", true, l).getMethod("builder").invoke(null);
            Class<?> bt = Class.forName("org.geysermc.cumulus.form.SimpleForm$Builder", true, l);
            Class<?> fb = Class.forName("org.geysermc.cumulus.form.util.FormBuilder", true, l);
            Class.forName("org.geysermc.cumulus.response.SimpleFormResponse", true, l).getMethod("clickedButtonId");
            Class.forName("org.geysermc.cumulus.response.result.FormResponseResult", true, l).getMethod("isValid");
            Class.forName("org.geysermc.cumulus.response.result.ValidFormResponseResult", true, l).getMethod("response");
            fb.getMethod("title", String.class).invoke(b, "Spell Book");
            bt.getMethod("content", String.class).invoke(b, "Tap a spell");
            for (String s : List.of("Bar 1: Mend (Rank 1)", "Bar 2: Boon (Rank 1)")) bt.getMethod("button", String.class).invoke(b, s);
            fb.getMethod("resultHandler", java.util.function.BiConsumer.class).invoke(b, (java.util.function.BiConsumer<Object, Object>) (x, y) -> {});
            Object form = fb.getMethod("build").invoke(b);
            Method buttons = form.getClass().getMethod("buttons");
            Check.eq(2, ((List<?>) buttons.invoke(form)).size(), "real Cumulus SimpleForm built with 2 buttons");
            Check.eq("Spell Book", form.getClass().getMethod("title").invoke(form), "form title");
        }
    }
}
