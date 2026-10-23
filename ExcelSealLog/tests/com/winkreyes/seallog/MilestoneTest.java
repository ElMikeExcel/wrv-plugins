package com.winkreyes.seallog;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class MilestoneTest {
    @Test void single() { List<Integer> o=new ArrayList<>(); assertEquals(1, ExcelSealLog.milestonesCrossed(0,74.9,o)); assertEquals(List.of(75),o); }
    @Test void multiAtOnce() { List<Integer> o=new ArrayList<>(); assertEquals(3, ExcelSealLog.milestonesCrossed(0,10,o)); assertEquals(List.of(75,50,25),o); }
    @Test void noRepeat() { List<Integer> o=new ArrayList<>(); assertEquals(1, ExcelSealLog.milestonesCrossed(1,60,o)); assertTrue(o.isEmpty()); }
    @Test void above() { List<Integer> o=new ArrayList<>(); assertEquals(0, ExcelSealLog.milestonesCrossed(0,90,o)); assertTrue(o.isEmpty()); }
}
