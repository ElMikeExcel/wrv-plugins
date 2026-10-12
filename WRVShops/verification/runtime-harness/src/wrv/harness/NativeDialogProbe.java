package wrv.harness;

import java.lang.reflect.Method;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.ConfirmationType;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;

/**
 * Exact-Paper test fixture only. Inspects the real serialized dialog and dispatches
 * synthetic responses through Paper's real registered callback manager. This uses
 * private server inspection exclusively in the disposable harness, never production.
 * Capturing showDialog proves server acceptance; it does not prove client rendering.
 */
public final class NativeDialogProbe {
    public final Object dialog;
    public final DialogBase base;
    public final TextDialogInput input;
    public final ConfirmationType buttons;

    public NativeDialogProbe(Object dialog) throws Exception {
        if (dialog == null || !dialog.getClass().getName().equals("io.papermc.paper.dialog.PaperDialog"))
            throw new AssertionError("Expected a real constructed PaperDialog, got " + dialog);
        this.dialog = dialog;
        Object handle = dialog.getClass().getMethod("getHandle").invoke(dialog);
        Class<?> conversions = Class.forName("io.papermc.paper.registry.data.util.Conversions");
        Object global = conversions.getMethod("global").invoke(null);
        Class<?> minecraftDialog = Class.forName("net.minecraft.server.dialog.Dialog");
        DialogRegistryEntry entry = (DialogRegistryEntry) Class.forName("io.papermc.paper.registry.data.dialog.PaperDialogRegistryEntry")
                .getConstructor(conversions, minecraftDialog).newInstance(global, handle);
        base = entry.base();
        if (base.inputs().size() != 1 || !(base.inputs().getFirst() instanceof TextDialogInput text))
            throw new AssertionError("Native amount entry must have exactly one actual text input");
        input = text;
        if (!(entry.type() instanceof ConfirmationType confirmation))
            throw new AssertionError("Native amount entry requires Use amount and Cancel actions");
        buttons = confirmation;
    }

    public void submit(Player sender, String text) throws Exception { reply(sender, true, text); }
    public void cancel(Player sender) throws Exception { reply(sender, false, null); }

    private void reply(Player sender, boolean use, String text) throws Exception {
        DialogAction action = (use ? buttons.yesButton() : buttons.noButton()).action();
        if (!(action instanceof DialogAction.CustomClickAction custom) || custom.additions() == null)
            throw new AssertionError("Native amount button must use a registered server callback");
        Object payload = Class.forName("net.minecraft.nbt.TagParser")
                .getMethod("parseCompoundFully", String.class).invoke(null, custom.additions().string());
        if (text != null) payload.getClass().getMethod("putString", String.class, String.class)
                .invoke(payload, "amount", text);
        Object manager = Class.forName("io.papermc.paper.adventure.providers.ClickCallbackProviderImpl")
                .getField("DIALOG_CLICK_MANAGER").get(null);
        // Match Paper's tick processing so even a same-tick test can find a just-registered callback.
        manager.getClass().getMethod("handleQueue", int.class).invoke(manager, 1);
        Method response = manager.getClass().getDeclaredMethod("doRunCallback", Audience.class, Key.class,
                Class.forName("net.minecraft.nbt.Tag"));
        response.setAccessible(true);
        response.invoke(manager, sender, custom.id(), payload);
    }
}
