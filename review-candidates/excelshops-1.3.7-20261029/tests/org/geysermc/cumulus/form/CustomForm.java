// Excel signature - WRV maintained source; original authorship and license notices preserved.
// SPDX-License-Identifier: MIT
package org.geysermc.cumulus.form;
import java.util.List;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.fixtures.FormFixtures;
/** Test-only signature fixture; no native client or actual Geyser is exercised. */
public interface CustomForm extends Form {
    static Builder builder() { return FormFixtures.customBuilder(); }
    interface Builder extends FormBuilder<Builder,CustomForm,CustomFormResponse> {
        Builder input(String label, String placeholder, String initial);
        Builder dropdown(String label, List<String> options, int defaultIndex);
    }
}
