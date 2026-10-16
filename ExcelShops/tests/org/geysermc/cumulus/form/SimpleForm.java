// SPDX-License-Identifier: MIT
package org.geysermc.cumulus.form;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.fixtures.FormFixtures;
/** Test-only signature fixture; no native client or actual Geyser is exercised. */
public interface SimpleForm extends Form {
    static Builder builder() { return FormFixtures.simpleBuilder(); }
    interface Builder extends FormBuilder<Builder,SimpleForm,SimpleFormResponse> {
        Builder content(String content);
        Builder button(String label);
    }
}
