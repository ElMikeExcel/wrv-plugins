import com.winkreyesvalley.excelmenujoin.CompassTap;

/** 1.4.2: a tap in the air (block pre-denied, item allowed) must open the menu. */
public final class CompassTapTest {
    public static void run() {
        System.out.println("CompassTapTest");
        Check.ok(CompassTap.shouldOpen(false), "air tap (item use allowed) opens the menu");
        Check.ok(!CompassTap.shouldOpen(true), "another plugin denied the item use (shop sign): menu stays shut");
    }
}
