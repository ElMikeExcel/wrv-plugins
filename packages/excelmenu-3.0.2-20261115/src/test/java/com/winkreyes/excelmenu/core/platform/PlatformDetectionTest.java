package com.winkreyes.excelmenu.core.platform;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PlatformDetectionTest {

    private static final UUID ID = UUID.randomUUID();

    @Test
    public void bedrockTrueMapsToBedrockAndForm() {
        Detection.State s = Detection.detect(id -> Boolean.TRUE, ID);
        assertEquals(Detection.State.BEDROCK, s);
        assertEquals(Detection.RenderTarget.FORM, Detection.renderTarget(s));
    }

    @Test
    public void bedrockFalseMapsToJavaAndChest() {
        Detection.State s = Detection.detect(id -> Boolean.FALSE, ID);
        assertEquals(Detection.State.JAVA, s);
        assertEquals(Detection.RenderTarget.CHEST, Detection.renderTarget(s));
    }

    @Test
    public void nullProbeIsUnknownAndStillRendersChest() {
        Detection.State s = Detection.detect(id -> null, ID);
        assertEquals(Detection.State.UNKNOWN, s);
        // The fail-safe: a broken bridge degrades presentation, never strands.
        assertEquals(Detection.RenderTarget.CHEST, Detection.renderTarget(s));
    }

    @Test
    public void throwingProbeIsUnknownNeverAnException() {
        Detection.State s = Detection.detect(id -> {
            throw new IllegalStateException("Geyser moved its API");
        }, ID);
        assertEquals(Detection.State.UNKNOWN, s);
    }

    @Test
    public void linkageErrorIsUnknown() {
        Detection.State s = Detection.detect(id -> {
            throw new NoClassDefFoundError("org.geysermc.geyser.api.GeyserApi");
        }, ID);
        assertEquals(Detection.State.UNKNOWN, s);
    }

    @Test
    public void missingDetectorOrPlayerIsUnknown() {
        assertEquals(Detection.State.UNKNOWN, Detection.detect(null, ID));
        assertEquals(Detection.State.UNKNOWN, Detection.detect(id -> Boolean.TRUE, null));
    }

    @Test
    public void selfCheckLinesNameTheVerdict() {
        assertTrue(Detection.selfCheckLine(true).contains("PRESENT"));
        assertTrue(Detection.selfCheckLine(false).contains("ABSENT"));
        assertTrue(Detection.selfCheckLine(false).contains("chest GUIs"));
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
