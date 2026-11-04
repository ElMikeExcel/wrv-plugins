import com.winkreyes.exceltrainingwheels.core.CopperDoors;

public class CopperDoorsTest {
    public static void run() {
        System.out.println("CopperDoorsTest");

        // The whole family: four oxidation levels, waxed or not.
        String[] family = {
                "COPPER_DOOR", "EXPOSED_COPPER_DOOR",
                "WEATHERED_COPPER_DOOR", "OXIDIZED_COPPER_DOOR",
                "WAXED_COPPER_DOOR", "WAXED_EXPOSED_COPPER_DOOR",
                "WAXED_WEATHERED_COPPER_DOOR", "WAXED_OXIDIZED_COPPER_DOOR"};
        for (String name : family) {
            Check.ok(CopperDoors.isCopperDoor(name), "copper family member tiered: " + name);
        }

        // Everything else is another tier (or no door at all).
        Check.ok(!CopperDoors.isCopperDoor("IRON_DOOR"), "iron door is the siege tier, not copper");
        Check.ok(!CopperDoors.isCopperDoor("OAK_DOOR"), "oak door is vanilla pace, not copper");
        Check.ok(!CopperDoors.isCopperDoor("SPRUCE_DOOR"), "spruce door is vanilla pace, not copper");
        Check.ok(!CopperDoors.isCopperDoor("COPPER_BLOCK"), "a copper block is not a copper door");
        Check.ok(!CopperDoors.isCopperDoor("WAXED_COPPER_TRAPDOOR"), "a copper trapdoor is not a copper door");
        Check.ok(!CopperDoors.isCopperDoor("copper_door"), "lowercase lookalike rejected (exact Material names only)");
        Check.ok(!CopperDoors.isCopperDoor(""), "empty name rejected");
        Check.ok(!CopperDoors.isCopperDoor(null), "null name rejected");
    }
}
