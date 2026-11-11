import com.winkreyesvalley.excelmenujoin.AdminCommands;

import java.util.UUID;

/**
 * ExcelMenuJoin 1.4.4 console-admin surface (/excelmenujoin
 * admin status|toggle|compass <player>), in the house Check
 * style. What is pinned here: the gate decision (console
 * allowed, permission otherwise, denial for a non-admin), the
 * exact status replies for every state shape the real plugin
 * can present (auto-open ON/OFF x compass held/not held), the
 * toggle confirmation in both directions (the was-state is the
 * pre-flip state), the compass given / not-given replies (the
 * give path's single refusal shape: already held, inventory or
 * Utility Pouch), the not-found rejections for all three forms
 * (each naming what did not happen, and the online-only limit
 * spelled out), and the usage / denial texts. The plugin.yml
 * wiring itself is pinned by PluginYmlGuardTest, which reads
 * the shipped file.
 */
public class AdminCommandsTest {
    private static final UUID A = UUID.fromString("11111111-1111-1111-1111-111111111111");

    static void run() {
        System.out.println("AdminCommandsTest:");

        // ---- the gate: console allowed, permission otherwise ----
        Check.ok(AdminCommands.allowed(true, false), "gate: console allowed without the node (explicit console path)");
        Check.ok(AdminCommands.allowed(true, true), "gate: console allowed with the node");
        Check.ok(AdminCommands.allowed(false, true), "gate: non-console sender WITH excelmenujoin.admin allowed");
        Check.ok(!AdminCommands.allowed(false, false), "gate: non-console sender WITHOUT the node denied");
        Check.eq("excelmenujoin.admin", AdminCommands.PERMISSION, "gate: permission node name");

        // ---- status: exact replies for every real state shape ----
        Check.eq("MenuJoin status for Marvin (" + A + "): join auto-open ON; menu compass held.",
                AdminCommands.statusMessage("Marvin", A, true, true), "status: auto-open ON + compass held");
        Check.eq("MenuJoin status for Marvin (" + A + "): join auto-open ON; menu compass not held.",
                AdminCommands.statusMessage("Marvin", A, true, false), "status: auto-open ON + no compass");
        Check.eq("MenuJoin status for Marvin (" + A + "): join auto-open OFF; menu compass held.",
                AdminCommands.statusMessage("Marvin", A, false, true), "status: auto-open OFF + compass held");
        Check.eq("MenuJoin status for Marvin (" + A + "): join auto-open OFF; menu compass not held.",
                AdminCommands.statusMessage("Marvin", A, false, false), "status: auto-open OFF + no compass");
        Check.eq("MenuJoin status for that player (" + A + "): join auto-open ON; menu compass held.",
                AdminCommands.statusMessage("  ", A, true, true), "status: blank name falls back gracefully");
        Check.eq("MenuJoin status for Marvin (unknown uuid): join auto-open OFF; menu compass not held.",
                AdminCommands.statusMessage("Marvin", null, false, false), "status: null uuid renders as unknown uuid, never throws");

        // ---- toggle: confirmation names the flip truthfully ----
        Check.eq("MenuJoin auto-open for Marvin (" + A + ") set to OFF (was ON).",
                AdminCommands.toggleMessage("Marvin", A, true), "toggle: was ON -> now OFF");
        Check.eq("MenuJoin auto-open for Marvin (" + A + ") set to ON (was OFF).",
                AdminCommands.toggleMessage("Marvin", A, false), "toggle: was OFF -> now ON");
        Check.eq("MenuJoin auto-open for that player (" + A + ") set to ON (was OFF).",
                AdminCommands.toggleMessage(null, A, false), "toggle: null name falls back gracefully");

        // ---- compass: given / not-given ----
        Check.eq("Gave a Server Menu compass to Marvin (" + A + ").",
                AdminCommands.compassGivenMessage("Marvin", A), "compass: given reply");
        Check.eq("No compass given to Marvin (" + A + "): they already hold one (inventory or Utility Pouch).",
                AdminCommands.compassNotGivenMessage("Marvin", A), "compass: not-given reply names the already-held rule");

        // ---- not-found rejections: online-only, nothing happened ----
        Check.eq("No online player named 'NobodyHereXYZ'. MenuJoin state lives on the player, so admin commands need the player online. Nothing was looked up.",
                AdminCommands.notFoundStatus("NobodyHereXYZ"), "not-found: status rejection (nothing looked up)");
        Check.eq("No online player named 'NobodyHereXYZ'. MenuJoin state lives on the player, so admin commands need the player online. Nothing was changed.",
                AdminCommands.notFoundToggle("NobodyHereXYZ"), "not-found: toggle rejection (nothing changed)");
        Check.eq("No online player named 'NobodyHereXYZ'. A compass can only be given to a player who is online. Nothing was given.",
                AdminCommands.notFoundCompass("NobodyHereXYZ"), "not-found: compass rejection (nothing given)");

        // ---- malformed usage + denial texts ----
        Check.eq("Usage: /excelmenujoin admin status <player>", AdminCommands.USAGE_STATUS, "usage: status form");
        Check.eq("Usage: /excelmenujoin admin toggle <player>", AdminCommands.USAGE_TOGGLE, "usage: toggle form");
        Check.eq("Usage: /excelmenujoin admin compass <player>", AdminCommands.USAGE_COMPASS, "usage: compass form");
        Check.eq("You do not have permission to do that. (excelmenujoin.admin required)",
                AdminCommands.DENIED, "denied: text names the required node");
    }
}
