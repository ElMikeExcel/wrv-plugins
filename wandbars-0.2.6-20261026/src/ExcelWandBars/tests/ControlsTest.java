import com.winkreyes.excelwandbars.core.*;
import java.util.List;

public final class ControlsTest {
    public static void run() {
        System.out.println("ControlsTest");
        for (WandStyle s : WandStyle.values()) {
            String b = String.join(" ", ControlsText.howTo(s, true));
            Check.ok(!b.toLowerCase().contains("click"), "Bedrock " + s + " how-to never says click: " + b);
            Check.ok(b.contains("tap") || b.contains("Tap"), "Bedrock " + s + " how-to says tap");
            Check.ok(b.contains("press and hold") || b.contains("Press and hold"), "Bedrock " + s + " how-to says press and hold");
            Check.ok(!b.contains("\u2014"), "no long dash " + s);
        }
        Check.ok(String.join(" ", ControlsText.howTo(WandStyle.CLASSIC, false)).contains("Left click casts."), "Java Classic wording unchanged from 0.2.1");
        Check.ok(!ControlsText.bookTitle(true).toLowerCase().contains("click"), "Bedrock chat book title: no click");
        Check.ok(ControlsText.bookTitle(false).contains("click"), "Java chat book title keeps click");
        Check.ok(!ControlsText.controlsButton(false, true).toLowerCase().contains("click"), "Bedrock controls button: no click");
        Check.ok(!ControlsText.chooserHeading("Classic", true).toLowerCase().contains("click"), "Bedrock chooser heading: no click");
        String tipB = String.join(" ", TipGate.lines(true)) + " " + TipGate.formContent();
        Check.ok(!tipB.toLowerCase().contains("click"), "Bedrock wand tip (chat + pop-up): no click: " + tipB);
        Check.ok(String.join(" ", TipGate.lines(false)).contains("right-click"), "Java wand tip keeps right-click / left-click");
    }
}
