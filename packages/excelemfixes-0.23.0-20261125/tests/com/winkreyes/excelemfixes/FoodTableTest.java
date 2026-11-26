import excelemfixes.foodheal.FoodTable;
import org.bukkit.Material;

/** Budget math: vanilla saturation values + the D162 formula bands. */
public final class FoodTableTest {

    private static double total(Material m) {
        Double sat = FoodTable.vanillaSaturation(m);
        if (sat == null) return Double.NaN;
        return FoodTable.totalPercent(sat, 0.75, 1.0, 10.0);
    }

    public static void run() {
        System.out.println("FoodTableTest");
        // Saturation spot values (vanilla).
        Check.close(12.8, FoodTable.vanillaSaturation(Material.COOKED_BEEF), "steak saturation 12.8");
        Check.close(6.0, FoodTable.vanillaSaturation(Material.BREAD), "bread saturation 6.0");
        Check.close(14.4, FoodTable.vanillaSaturation(Material.GOLDEN_CARROT), "golden carrot saturation 14.4");
        Check.close(0.4, FoodTable.vanillaSaturation(Material.COOKIE), "cookie saturation 0.4");
        Check.eq(null, FoodTable.vanillaSaturation(Material.CAKE), "cake has no table entry (never fires consume)");
        Check.eq(null, FoodTable.vanillaSaturation(Material.MILK_BUCKET), "milk has no table entry");
        Check.eq(null, FoodTable.vanillaSaturation(Material.STONE), "non-food has no table entry");

        // D162 class bands.
        Check.band(8.0, 10.0, total(Material.COOKED_BEEF), "steak-class total in 8-10% band");
        Check.close(9.6, total(Material.COOKED_BEEF), "steak total exactly 9.6%");
        Check.close(9.6, total(Material.COOKED_PORKCHOP), "porkchop total exactly 9.6%");
        Check.band(4.0, 5.0, total(Material.BREAD), "bread-class total in 4-5% band");
        Check.close(4.5, total(Material.BREAD), "bread total exactly 4.5%");
        Check.close(4.5, total(Material.BAKED_POTATO), "baked potato total exactly 4.5%");
        Check.band(2.0, 3.0, total(Material.CARROT), "snack-class (carrot) total in 2-3% band");
        Check.close(2.7, total(Material.CARROT), "carrot total exactly 2.7%");

        // Clamps.
        Check.close(10.0, total(Material.GOLDEN_CARROT), "golden carrot clamps to the 10% ceiling");
        Check.close(10.0, FoodTable.totalPercent(100.0, 0.75, 1.0, 10.0), "huge saturation clamps to ceiling");
        Check.close(1.0, total(Material.COOKIE), "cookie floors to the 1% minimum (instant only)");
        Check.close(1.0, total(Material.MELON_SLICE), "melon slice floors to the 1% minimum");
        Check.close(1.0, FoodTable.totalPercent(0.0, 0.75, 1.0, 10.0), "zero saturation floors to minimum");

        // Mid-table exactness.
        Check.close(7.2, total(Material.GOLDEN_APPLE), "golden apple total 7.2%");
        Check.close(7.2, total(Material.COOKED_SALMON), "cooked salmon total 7.2%");
        Check.close(5.4, total(Material.COOKED_CHICKEN), "cooked chicken total 5.4%");
        Check.close(1.8, total(Material.APPLE), "apple total 1.8%");
        Check.close(9.0, total(Material.RABBIT_STEW), "rabbit stew total 9.0%");
    }
}
