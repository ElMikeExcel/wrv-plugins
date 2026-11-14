// SPDX-License-Identifier: MIT
package org.geysermc.api;
import java.util.UUID;
import org.geysermc.cumulus.form.Form;
/** Tests inherited public API method lookup, without a Geyser runtime. */
public interface GeyserApiBase {
    Object connectionByUuid(UUID id);
    boolean sendForm(UUID id, Form form);
}
