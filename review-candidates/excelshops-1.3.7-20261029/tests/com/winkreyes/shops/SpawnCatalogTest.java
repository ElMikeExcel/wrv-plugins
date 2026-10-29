// Excel signature - WRV maintained source; original authorship and license notices preserved.
// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Cap and config rules for the spawn catalog. Full catalog parsing resolves
 * Materials against the server registry, so it is proven on the Paper rig;
 * the size/config rules extracted here are pure and run headless.
 */
class SpawnCatalogTest {
    @Test void legacyCapBoundaryAtFortyFive() {
        SpawnCatalog.requireValidSize(1, 45);
        SpawnCatalog.requireValidSize(45, 45);
        IllegalArgumentException over = assertThrows(IllegalArgumentException.class,
            () -> SpawnCatalog.requireValidSize(46, 45));
        assertEquals("Catalog must contain 1..45 entries", over.getMessage());
    }
    @Test void raisedCapAdmitsTheWholeRecordedWave() {
        SpawnCatalog.requireValidSize(46, SpawnCatalog.DEFAULT_MAX_ENTRIES);
        SpawnCatalog.requireValidSize(200, SpawnCatalog.DEFAULT_MAX_ENTRIES);
        SpawnCatalog.requireValidSize(SpawnCatalog.DEFAULT_MAX_ENTRIES, SpawnCatalog.DEFAULT_MAX_ENTRIES);
        assertThrows(IllegalArgumentException.class,
            () -> SpawnCatalog.requireValidSize(SpawnCatalog.DEFAULT_MAX_ENTRIES + 1, SpawnCatalog.DEFAULT_MAX_ENTRIES));
        assertEquals(250, SpawnCatalog.DEFAULT_MAX_ENTRIES);
    }
    @Test void emptyCatalogIsRejectedAtAnyCap() {
        assertThrows(IllegalArgumentException.class, () -> SpawnCatalog.requireValidSize(0, 45));
        assertThrows(IllegalArgumentException.class, () -> SpawnCatalog.requireValidSize(0, SpawnCatalog.DEFAULT_MAX_ENTRIES));
    }
    @Test void configuredCapMustStayInsideTheSanityBound() {
        assertEquals(1, SpawnCatalog.requireMaxEntries(1));
        assertEquals(250, SpawnCatalog.requireMaxEntries(250));
        assertEquals(SpawnCatalog.ABSOLUTE_MAX_ENTRIES, SpawnCatalog.requireMaxEntries(SpawnCatalog.ABSOLUTE_MAX_ENTRIES));
        assertThrows(IllegalArgumentException.class, () -> SpawnCatalog.requireMaxEntries(0));
        assertThrows(IllegalArgumentException.class, () -> SpawnCatalog.requireMaxEntries(-1));
        assertThrows(IllegalArgumentException.class, () -> SpawnCatalog.requireMaxEntries(SpawnCatalog.ABSOLUTE_MAX_ENTRIES + 1));
    }
    @Test void offerDataCarriesTheMobileDerivationFields() {
        SpawnCatalog.OfferData offer = new SpawnCatalog.OfferData("disc_pigstep", "Music Disc Pigstep", 1, 120000, null);
        assertEquals("disc_pigstep", offer.id());
        assertEquals(1, offer.bundleSize());
        assertEquals(120000, offer.bundlePrice());
        assertNull(offer.category());
    }
}
