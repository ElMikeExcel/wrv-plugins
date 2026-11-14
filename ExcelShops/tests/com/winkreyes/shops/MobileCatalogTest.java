// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.File;
import java.util.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

/** Authored for later JVM execution. No claim that these tests have run. */
class MobileCatalogTest {
    private MobileCatalog catalog(){return new MobileCatalog(name->YamlConfiguration.loadConfiguration(new File("mobile-menus/"+name+".yml")));}
    @Test void preservesFiveCategoriesAndTwentyOneExactProductIds(){
        MobileCatalog catalog=catalog();
        assertEquals(List.of("food","building","travel_animals","enchanting","potions_protection"),catalog.categories().stream().map(MobileCatalog.Category::id).toList());
        assertEquals(21,catalog.categories().stream().mapToInt(c->c.products().size()).sum());
        assertEquals(18,catalog.categories().stream().flatMap(c->c.products().stream()).filter(p->p.route()==MobileCatalog.Route.ORDINARY).count());
        assertEquals(4000,catalog.product("bread").price());assertEquals(16,catalog.product("bread").bundleSize());
        assertEquals(1500000,catalog.product("deathprotection_basic").price());
        assertEquals("[console] mobveil buy %player_name%",catalog.product("veil_potion").originalCommand());
    }
    @Test void alteredOrInjectedCommandIsRejectedWithoutExecution(){
        assertThrows(IllegalArgumentException.class,()->new MobileCatalog(name->{
            YamlConfiguration yaml=YamlConfiguration.loadConfiguration(new File("mobile-menus/"+name+".yml"));
            if(name.equals("spawnshop_food"))yaml.set("items.bread.left_click_commands",List.of("[console] op %player_name%"));
            return yaml;
        }));
    }
    @Test void addedOrMissingProductsAreRejected(){
        assertThrows(IllegalArgumentException.class,()->new MobileCatalog(name->{
            YamlConfiguration yaml=YamlConfiguration.loadConfiguration(new File("mobile-menus/"+name+".yml"));
            if(name.equals("spawnshop_food"))yaml.set("items.bread",null);
            return yaml;
        }));
    }
    @Test void newCatalogPriceOrBundleMustMatchDisplayedContract(){
        MobileCatalog catalog=catalog();ItemStack item=mock(ItemStack.class);when(item.clone()).thenReturn(item);
        ShopAdapter.Selection correct=new ShopAdapter.Selection("spawn:bread",1,null,"Bread",item,16,4000,144,"fixture",null);
        catalog.validate(catalog.product("bread"),correct);assertEquals(144,MobileCatalog.maximum(correct));
        assertThrows(IllegalArgumentException.class,()->catalog.validate(catalog.product("bread"),new ShopAdapter.Selection("spawn:bread",1,null,"Bread",item,16,4001,144,"fixture",null)));
        assertThrows(IllegalArgumentException.class,()->catalog.validate(catalog.product("steak"),correct));
    }
    @Test void exactTextRequiresBoundedWholeNumbers(){
        assertEquals(19,MobileCatalog.quantity(" 19 ",144));
        for(String value:Arrays.asList(null,"","0","-1","1.5","1e2","145","10000","19; op user"))
            assertThrows(IllegalArgumentException.class,()->MobileCatalog.quantity(value,144),String.valueOf(value));
    }
    @Test void resourceDescriptionsRemainImmutable(){
        MobileCatalog.Product potion=catalog().product("deathpotion");
        assertTrue(potion.lore().stream().anyMatch(l->l.contains("Does NOT save your XP")));
        assertThrows(UnsupportedOperationException.class,()->potion.lore().add("changed"));
    }
}
