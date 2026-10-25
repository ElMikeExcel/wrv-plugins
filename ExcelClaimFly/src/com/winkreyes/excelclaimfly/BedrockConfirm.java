package com.winkreyes.excelclaimfly;

import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.floodgate.api.FloodgateApi;

/** Only loaded when Floodgate is present. Bedrock gets a two-button tap form. */
final class BedrockConfirm {
    private BedrockConfirm() {}

    static boolean isBedrock(Player p) {
        try { return FloodgateApi.getInstance().isFloodgatePlayer(p.getUniqueId()); }
        catch (Throwable t) { return false; }
    }

    /** onBuy runs on the main thread via the supplied runner. */
    static boolean send(Player p, String title, String body, String buyLabel, String cancelLabel,
                        Runnable onBuy, java.util.function.Consumer<Runnable> sync) {
        ModalForm form = ModalForm.builder()
                .title(title).content(body).button1(buyLabel).button2(cancelLabel)
                .validResultHandler(r -> { if (r.clickedFirst()) sync.accept(onBuy); })
                .build();
        return FloodgateApi.getInstance().sendForm(p.getUniqueId(), form);
    }

    static boolean info(Player p, String title, String body, String ok) {
        ModalForm form = ModalForm.builder().title(title).content(body).button1(ok).button2("Close").build();
        return FloodgateApi.getInstance().sendForm(p.getUniqueId(), form);
    }
}
