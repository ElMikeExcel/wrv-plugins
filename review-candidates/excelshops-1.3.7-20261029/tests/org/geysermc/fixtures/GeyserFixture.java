// Excel signature - WRV maintained source; original authorship and license notices preserved.
// SPDX-License-Identifier: MIT
package org.geysermc.fixtures;

import java.util.*;
import java.util.function.Consumer;
import org.geysermc.cumulus.form.Form;
import org.geysermc.geyser.api.GeyserApi;

/** Original deterministic fixture, not an upstream implementation or compatibility guarantee. */
public final class GeyserFixture {
    public static boolean online, sendAccepted, throwOnSend, apiAvailable;
    public static UUID lastPlayer;
    public static Consumer<Form> duringSend;
    public static final List<Form> sentForms = new ArrayList<>();
    private static final GeyserApi API = new HiddenApi();
    private GeyserFixture() { }
    public static GeyserApi api() { return apiAvailable ? API : null; }
    public static void reset() {
        online = true; sendAccepted = true; apiAvailable = true; throwOnSend = false;
        lastPlayer = null; duringSend = form -> { }; sentForms.clear();
    }
    private static final class HiddenApi implements GeyserApi {
        public Object connectionByUuid(UUID id) { return online ? new HiddenConnection() : null; }
        public boolean sendForm(UUID id, Form form) {
            lastPlayer = id; sentForms.add(form); duringSend.accept(form);
            if (throwOnSend) throw new IllegalStateException("Fixture send failure after receiving form");
            return sendAccepted;
        }
    }
    private static final class HiddenConnection { }
}
