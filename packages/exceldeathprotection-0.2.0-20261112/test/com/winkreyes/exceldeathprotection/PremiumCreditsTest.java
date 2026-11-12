package com.winkreyes.exceldeathprotection;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Banked premium-credit semantics (0.2.0, decision D9). */
class PremiumCreditsTest {

    private static PremiumCredits.CreditResult credit(boolean active, int banked, int amount) {
        return PremiumCredits.applyCredit(active, banked, amount, true, 5, 0);
    }

    @Test void creditWhileUnprotectedActivatesAndBanksNothingForOne() {
        PremiumCredits.CreditResult r = credit(false, 0, 1);
        assertTrue(r.credited());
        assertTrue(r.activateNow());
        assertEquals(1, r.effectiveAmount());
        assertEquals(0, r.newBanked());
    }

    @Test void creditWhileProtectedBanksInFull() {
        PremiumCredits.CreditResult r = credit(true, 0, 1);
        assertTrue(r.credited());
        assertFalse(r.activateNow());
        assertEquals(1, r.newBanked());
    }

    @Test void creditWhileBasicActiveAlsoBanksRatherThanOverwriting() {
        // The plugin passes active=true for either tier; the basic
        // protection is never overwritten by a premium credit.
        PremiumCredits.CreditResult r = credit(true, 2, 1);
        assertTrue(r.credited());
        assertFalse(r.activateNow());
        assertEquals(3, r.newBanked());
    }

    @Test void creditThreeWhileProtectedBanksThree() {
        assertEquals(3, credit(true, 0, 3).newBanked());
    }

    @Test void creditThreeWhileUnprotectedActivatesOneAndBanksTwo() {
        PremiumCredits.CreditResult r = credit(false, 0, 3);
        assertTrue(r.activateNow());
        assertEquals(2, r.newBanked());
    }

    @Test void rearmAfterConsumedDeathSpendsOneBankedCredit() {
        PremiumCredits.RearmResult r = PremiumCredits.rearm(2, true, true);
        assertTrue(r.rearm());
        assertEquals(1, r.newBanked());
    }

    @Test void rearmWithEmptyBankDoesNothing() {
        PremiumCredits.RearmResult r = PremiumCredits.rearm(0, true, true);
        assertFalse(r.rearm());
        assertEquals(0, r.newBanked());
    }

    @Test void noRearmWhenProtectionWasNotUsed() {
        PremiumCredits.RearmResult r = PremiumCredits.rearm(3, false, true);
        assertFalse(r.rearm());
        assertEquals(3, r.newBanked());
    }

    @Test void noRearmWhenAutoRearmDisabled() {
        PremiumCredits.RearmResult r = PremiumCredits.rearm(3, true, false);
        assertFalse(r.rearm());
        assertEquals(3, r.newBanked());
    }

    @Test void disabledSeamRejectsCreditWithoutStateChange() {
        PremiumCredits.CreditResult r = PremiumCredits.applyCredit(false, 4, 1, false, 5, 0);
        assertFalse(r.credited());
        assertFalse(r.activateNow());
        assertEquals(4, r.newBanked());
    }

    @Test void zeroAndNegativeAmountsAreRejected() {
        assertFalse(credit(false, 0, 0).credited());
        assertFalse(credit(true, 1, -2).credited());
        assertEquals(1, credit(true, 1, -2).newBanked());
    }

    @Test void amountIsClampedToMaxPerCall() {
        PremiumCredits.CreditResult r = credit(true, 0, 99);
        assertTrue(r.credited());
        assertEquals(5, r.effectiveAmount());
        assertEquals(5, r.newBanked());
    }

    @Test void bankCapIsRespectedAndFullBankRejectsFurtherCredit() {
        PremiumCredits.CreditResult r = PremiumCredits.applyCredit(true, 4, 3, true, 5, 5);
        assertTrue(r.credited());
        assertEquals(1, r.effectiveAmount());
        assertEquals(5, r.newBanked());
        assertFalse(PremiumCredits.applyCredit(true, 5, 1, true, 5, 5).credited());
    }

    @Test void revokeRemovesUpToTheBankAndNeverGoesNegative() {
        assertEquals(1, PremiumCredits.revoke(3, 2));
        assertEquals(0, PremiumCredits.revoke(3, 99));
        assertEquals(0, PremiumCredits.revoke(0, 1));
        assertEquals(3, PremiumCredits.revoke(3, 0));
    }

    @Test void fullCycleCreditDeathRearmDeathLeavesPlayerUnprotected() {
        // Credit while protected -> bank 1; death consumes protection;
        // respawn re-arms from bank; second death with empty bank ends it.
        PremiumCredits.CreditResult r = credit(true, 0, 1);
        assertEquals(1, r.newBanked());
        PremiumCredits.RearmResult rearm = PremiumCredits.rearm(r.newBanked(), true, true);
        assertTrue(rearm.rearm());
        assertEquals(0, rearm.newBanked());
        assertFalse(PremiumCredits.rearm(rearm.newBanked(), true, true).rearm());
    }

    @Test void bundledConfigCarriesThePremiumDefaults() throws Exception {
        Map<String, Object> config = new Yaml().load(Files.readString(Path.of("config.yml")));
        assertTrue(config.get("premium") instanceof Map);
        Map<?, ?> premium = (Map<?, ?>) config.get("premium");
        assertEquals(Boolean.TRUE, premium.get("enabled"));
        assertEquals(Boolean.TRUE, premium.get("auto-rearm"));
        assertEquals(5, ((Number) premium.get("max-per-call")).intValue());
        assertEquals(0, ((Number) premium.get("max-banked")).intValue());
        // Prices untouched by the 0.2.0 delta.
        assertEquals(15000.0, ((Number) config.get("basic-price")).doubleValue());
        assertEquals(30000.0, ((Number) config.get("premium-price")).doubleValue());
    }
}
