// Excel signature - WRV maintained source; original authorship and license notices preserved.
// SPDX-License-Identifier: MIT
package org.geysermc.cumulus.response;
/** Minimal public test API; fixture implementations are deliberately nonpublic. */
public interface CustomFormResponse { String asInput(int index); int asDropdown(int index); }
