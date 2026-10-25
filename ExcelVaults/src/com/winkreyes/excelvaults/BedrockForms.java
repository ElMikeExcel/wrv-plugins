package com.winkreyes.excelvaults;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

/** Only loaded when Floodgate is present. Bedrock gets tap forms, no typing. */
final class BedrockForms {
    private BedrockForms() {}

    static boolean isBedrock(Player p) {
        try { return FloodgateApi.getInstance().isFloodgatePlayer(p.getUniqueId()); }
        catch (Throwable t) { return false; }
    }

    /** Two-button confirm. onYes runs on the main thread through sync. */
    static boolean confirm(Player p, String title, String body, String yes, String no, Runnable onYes, Consumer<Runnable> sync) {
        ModalForm form = ModalForm.builder().title(title).content(body).button1(yes).button2(no)
                .validResultHandler(r -> { if (r.clickedFirst()) sync.accept(onYes); })
                .build();
        return FloodgateApi.getInstance().sendForm(p.getUniqueId(), form);
    }

    static boolean info(Player p, String title, String body) {
        ModalForm form = ModalForm.builder().title(title).content(body).button1("OK").button2("Close").build();
        return FloodgateApi.getInstance().sendForm(p.getUniqueId(), form);
    }

    /** One button per entry. onPick gets the button index on the main thread. */
    static boolean list(Player p, String title, String body, List<String> buttons, IntConsumer onPick, Consumer<Runnable> sync) {
        SimpleForm.Builder b = SimpleForm.builder().title(title).content(body);
        for (String s : buttons) b.button(s);
        b.validResultHandler(r -> { int i = r.clickedButtonId(); sync.accept(() -> onPick.accept(i)); });
        return FloodgateApi.getInstance().sendForm(p.getUniqueId(), b.build());
    }
}
