/* Excel WRV maintained source; original attribution and license preserved. */ package com.winkreyes.makersmark;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;import java.util.*;
class AccomplishmentsTest {
 @Test void arbitraryNamedMobHasObservedProvenance(){var k=Accomplishments.observed("ZOMBIE","Jester's Guardian",List.of("elitemobs"));assertEquals("Jester's Guardian",k.name());assertEquals("NAMED:ZOMBIE;observed-source=elitemobs",k.kind());}
 @Test void healthOnlyIsNotAName(){for(String text:List.of("100/100 ❤","HP: 12/24","Health: 100","████ 50%","Level 5"))assertNull(Accomplishments.observed("ZOMBIE",text,List.of()),text);}
 @Test void vanillaBossRetainsTypeWhenStatusOnly(){var k=Accomplishments.observed("WARDEN","100/500 HP",List.of("custom"));assertEquals("Warden",k.name());assertEquals("BOSS:WARDEN;observed-source=custom",k.kind());}
 @Test void existingVanillaKeysDoNotChange(){assertEquals("BOSS:WITHER",Accomplishments.observed("WITHER",null,List.of()).kind());assertEquals("Wither",Accomplishments.observed("WITHER",null,List.of()).name());}
}
