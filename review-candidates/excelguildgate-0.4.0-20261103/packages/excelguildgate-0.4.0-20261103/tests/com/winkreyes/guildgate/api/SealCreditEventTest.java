package com.winkreyes.guildgate.api;

/**
 * The 0.3.1 credit-veto event: payload round-trip and the
 * cancellable contract GuildGate's death handler relies on.
 * (The veto integration itself - callEvent from the death handler
 * and the cancelled path skipping creditSeal - is proven on the
 * BossTrial scratch rig, where both plugins run together.)
 */
public final class SealCreditEventTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    public static void main(String[] args) {
        SealCreditEvent e = new SealCreditEvent("wither", null, "GumGumJetPistol");
        check("wither".equals(e.getSealId()), "seal id round-trips");
        check("GumGumJetPistol".equals(e.getKillerName()), "killer name round-trips");
        check(e.getEntity() == null, "entity payload carried as given");
        check(!e.isCancelled(), "a fresh proposal is not cancelled");
        e.setCancelled(true);
        check(e.isCancelled(), "cancellation sticks (the veto)");
        e.setCancelled(false);
        check(!e.isCancelled(), "cancellation can be lifted again (Bukkit contract)");

        SealCreditEvent anon = new SealCreditEvent("ender_dragon", null, null);
        check(anon.getKillerName() == null, "the forgiving-rule credit carries a null killer");

        check(SealCreditEvent.getHandlerList() != null, "static handler list exists");
        check(e.getHandlers() == SealCreditEvent.getHandlerList(),
                "instance handlers are the static list (Bukkit registration contract)");

        System.out.println("SealCreditEventTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
