// Excel signature - WRV maintained source; original authorship and license notices preserved.
// SPDX-License-Identifier: MIT
package org.geysermc.cumulus.form.util;
import java.util.function.BiConsumer;
import org.geysermc.cumulus.form.Form;
/** Original test double with the public method signatures used by the bridge. */
public interface FormBuilder<B extends FormBuilder<B,F,R>,F extends Form,R> {
    B title(String title);
    B validResultHandler(BiConsumer<F,R> callback);
    B closedOrInvalidResultHandler(Runnable callback);
    F build();
}
