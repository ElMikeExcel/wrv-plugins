import com.winkreyes.teamrewards.marker.ReviewedWorlds;import java.util.*;
public class ReviewedWorldsTest {
 static int count;static void yes(boolean x){if(!x)throw new AssertionError();count++;}
 static void no(Runnable r){try{r.run();throw new AssertionError("accepted invalid policy");}catch(IllegalArgumentException expected){count++;}}
 public static void main(String[] args){
 UUID a=UUID.randomUUID(),b=UUID.randomUUID(),c=UUID.randomUUID();
 var worlds=List.of(new ReviewedWorlds.Loaded("world",a),new ReviewedWorlds.Loaded("world_nether",b),new ReviewedWorlds.Loaded("world_the_end",c));
 yes(ReviewedWorlds.resolve(null,null,worlds).ids().isEmpty());
 yes(ReviewedWorlds.resolve(List.of(a.toString()),null,worlds).ids().equals(Set.of(a)));
 yes(ReviewedWorlds.resolve(null,List.of("world","world_nether","world_the_end"),worlds).ids().equals(Set.of(a,b,c)));
 yes(ReviewedWorlds.resolve(List.of(a.toString()),List.of("world_the_end"),worlds).ids().equals(Set.of(a,c)));
 no(()->ReviewedWorlds.resolve(null,List.of("World"),worlds));no(()->ReviewedWorlds.resolve(null,List.of("nether"),worlds));
 no(()->ReviewedWorlds.resolve(null,List.of("world"),List.of()));
 no(()->ReviewedWorlds.resolve(List.of(UUID.randomUUID().toString()),null,worlds));
 no(()->ReviewedWorlds.resolve(List.of(a.toString()),List.of("world"),worlds));
 no(()->ReviewedWorlds.resolve(null,List.of("world","world"),worlds));
 no(()->ReviewedWorlds.resolve(List.of(a.toString(),a.toString()),null,worlds));
 no(()->ReviewedWorlds.resolve(null,"world",worlds));no(()->ReviewedWorlds.resolve(null,List.of(1),worlds));
 no(()->ReviewedWorlds.resolve(null,List.of(" world"),worlds));no(()->ReviewedWorlds.resolve(List.of("bad"),null,worlds));
 no(()->ReviewedWorlds.resolve(null,List.of("world"),List.of(worlds.get(0),new ReviewedWorlds.Loaded("world",b))));
 no(()->ReviewedWorlds.resolve(null,List.of("world"),List.of(worlds.get(0),new ReviewedWorlds.Loaded("other",a))));
 no(()->ReviewedWorlds.resolve(null,List.of("guild"),List.of(new ReviewedWorlds.Loaded("guild",a))));
 for(String excluded:List.of("em_adventurers_guild","minecraft:em_adventurers_guild")){
 no(()->ReviewedWorlds.resolve(null,List.of(excluded),List.of(new ReviewedWorlds.Loaded(excluded,a))));
 no(()->ReviewedWorlds.resolve(List.of(a.toString()),null,List.of(new ReviewedWorlds.Loaded(excluded,a))));
 }
 var result=ReviewedWorlds.resolve(null,List.of("world"),worlds);try{result.ids().clear();throw new AssertionError();}catch(UnsupportedOperationException e){count++;}
 System.out.println(count+" reviewed-world resolver checks pass");
 }
}
