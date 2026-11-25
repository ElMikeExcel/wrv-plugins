/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The Bedrock form-fallback policy (0.4.0, PM-11): the form is
 * offered only when all three facts hold - the player is a
 * Floodgate (Bedrock) player, the chest menu's retry window
 * closed unpresented, and a batch is actually waiting.
 */
public final class PresentFallbackTest {

    private static int passed;
    private static int failed;

    private static void check(boolean ok, String label) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    public static void main(String[] args) {
        check(PresentFallback.offerForm(true, true, true), "all three facts: offer the form");
        check(!PresentFallback.offerForm(false, true, true), "a Java player never gets the form");
        check(!PresentFallback.offerForm(true, false, true), "window still open: no form (the menu may yet present)");
        check(!PresentFallback.offerForm(true, true, false), "nothing waiting: no form");
        check(!PresentFallback.offerForm(false, false, false), "no facts: no form");

        System.out.println("PresentFallbackTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
