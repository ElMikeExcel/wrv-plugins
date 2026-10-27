package com.winkreyes.makersmark.core;

import java.util.List;
import org.junit.jupiter.api.Test;
import net.kyori.adventure.text.format.NamedTextColor;
import static org.junit.jupiter.api.Assertions.*;

class MilestoneColorsTest {
    // Synthetic test boundaries, NOT proposed production progression.
    private static final List<MilestoneColors.Tier> T=List.of(
        new MilestoneColors.Tier(17,NamedTextColor.GOLD),
        new MilestoneColors.Tier(3,NamedTextColor.GREEN),
        new MilestoneColors.Tier(8,NamedTextColor.BLUE));
    @Test void boundariesAndUnorderedInput() {
        long[] values={-1,0,2,3,7,8,16,17,Long.MAX_VALUE};
        NamedTextColor[] expected={NamedTextColor.GRAY,NamedTextColor.GRAY,NamedTextColor.GRAY,
            NamedTextColor.GREEN,NamedTextColor.GREEN,NamedTextColor.BLUE,NamedTextColor.BLUE,NamedTextColor.GOLD,NamedTextColor.GOLD};
        for(int i=0;i<values.length;i++) assertEquals(expected[i],MilestoneColors.tier(values[i],T));
    }
    @Test void absentConfigurationLeavesGray() {
        assertEquals(NamedTextColor.GRAY,MilestoneColors.tier(500000,List.of()));
    }
    @Test void rejectInvalidOrAmbiguousTiers() {
        assertThrows(IllegalArgumentException.class,()->new MilestoneColors.Tier(-1,NamedTextColor.BLUE));
        assertThrows(IllegalArgumentException.class,()->new MilestoneColors.Tier(1,null));
        assertThrows(IllegalArgumentException.class,()->MilestoneColors.tier(0,List.of(T.get(0),T.get(0))));
    }
    @Test void weaponExistingCombinedCountAndArmorSeparate() {
        assertEquals(NamedTextColor.BLUE,MilestoneColors.forLine("Kills: 8 (Players: 1)",7,1,99,T,List.of()));
        assertEquals(NamedTextColor.GRAY,MilestoneColors.forLine("Hits blocked: 99",7,1,99,T,List.of()));
        assertEquals(NamedTextColor.GOLD,MilestoneColors.forLine("Hits blocked: 99",0,0,99,List.of(),T));
        assertEquals(NamedTextColor.GOLD,MilestoneColors.forLine("Kills: maximum",Integer.MAX_VALUE,Integer.MAX_VALUE,0,T,List.of()));
    }
    @Test void otherLinesStayGray() {
        for(String s:new String[]{"Crafted by Tester","Slain: Wither x2","Custom text"})
            assertEquals(NamedTextColor.GRAY,MilestoneColors.forLine(s,999,999,999,T,T));
    }
}
