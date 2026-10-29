package com.winkreyes.makersmark;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.magic.MagicPlugin;
import com.elmakers.mine.bukkit.wand.Wand;
/** Native cache synchronization for one freshly revalidated physical inventory slot. Never opens/creates a Mage or mints an identity. */
final class MagicPhysicalItems {
 private final java.util.function.Supplier<? extends Collection<? extends Player>> players;
 private final java.util.function.Supplier<? extends Collection<? extends Mage>> mages;
 MagicPhysicalItems(MagicPlugin magic){this(()->Bukkit.getOnlinePlayers(),()->magic.getAPI().getMages());}
 MagicPhysicalItems(java.util.function.Supplier<? extends Collection<? extends Player>> players,java.util.function.Supplier<? extends Collection<? extends Mage>> mages){this.players=Objects.requireNonNull(players);this.mages=Objects.requireNonNull(mages);}
 static boolean wand(ItemStack item){return item!=null&&item.getAmount()==1&&Wand.isWand(item)&&!Wand.isUpgrade(item)&&!Wand.isSpell(item);}
 private record Binding(ItemStack snapshot,Wand cached){}
 private Binding bind(Player owner,int slot,ItemStack expected){return bind(owner,slot,expected,false);}
 private Binding bind(Player owner,int slot,ItemStack expected,boolean loreOnly){
  if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Physical wand access requires server thread");
  if(!wand(expected)||!expected.equals(owner.getInventory().getItem(slot)))throw new IllegalStateException("Wand slot changed");
  String nativeId=Wand.getWandId(expected);if(nativeId==null||nativeId.isBlank())throw new IllegalStateException("Wand has no persisted native identity");
  int copies=0;
  for(Player p:players.get()){
   List<ItemStack> observed=new ArrayList<>(Arrays.asList(p.getInventory().getContents()));observed.add(p.getItemOnCursor());observed.addAll(Arrays.asList(p.getEnderChest().getContents()));
   for(ItemStack item:observed)if(item!=null&&nativeId.equals(Wand.getWandId(item)))copies+=item.getAmount();
  }
  if(copies!=1)throw new IllegalStateException("Native wand identity not unique in observable inventories");
  Wand cached=null;
  for(Mage mage:mages.get()){
   Player player=mage.getPlayer();if(player==null)continue;
   if(player.getUniqueId().equals(owner.getUniqueId())&&mage.hasStoredInventory())throw new IllegalStateException("Wand virtual inventory is open");
   for(var candidate:Arrays.asList(mage.getActiveWand(),mage.getOffhandWand())){
    if(candidate==null)continue;
    int declaredSlot=candidate==mage.getOffhandWand()?40:owner.getInventory().getHeldItemSlot();
    if(player.getUniqueId().equals(owner.getUniqueId())&&declaredSlot==slot&&!nativeId.equals(candidate.getId()))throw new IllegalStateException("Occupied native slot has a different cached wand");
    if(!nativeId.equals(candidate.getId()))continue;
    if(!(candidate instanceof Wand actual)||!player.getUniqueId().equals(owner.getUniqueId())||actual.isInventoryOpen()||actual.hasStoredInventory()||!(loreOnly?sameOutsideLore(expected,actual.getItem()):expected.equals(actual.getItem())))throw new IllegalStateException("Native wand binding is ambiguous or stale");
    int actualSlot=candidate==mage.getOffhandWand()?40:owner.getInventory().getHeldItemSlot();
    if(actualSlot!=slot||cached!=null&&cached!=actual)throw new IllegalStateException("Native wand is bound to a different slot");cached=actual;
   }
  }
  return new Binding(expected.clone(),cached);
 }
 boolean detached(ItemStack item){
  if(!Bukkit.isPrimaryThread()||!wand(item))return false;String id=Wand.getWandId(item);if(id==null||id.isBlank())return false;
  for(Mage mage:mages.get())for(var cached:Arrays.asList(mage.getActiveWand(),mage.getOffhandWand()))if(cached!=null&&id.equals(cached.getId()))return false;
  return true;
 }
 boolean supported(Player owner,int slot,ItemStack expected){try{bind(owner,slot,expected);return true;}catch(RuntimeException e){return false;}}
 void replace(Player owner,int slot,ItemStack expected,ItemStack replacement){
  Binding binding=bind(owner,slot,expected);
  if(!sameOutsideMaker(expected,replacement))throw new IllegalArgumentException("Wand write would alter native data");
  // Same main-thread operation: publish the fresh clone to both physical inventory and native cache.
  try{owner.getInventory().setItem(slot,replacement);}catch(RuntimeException failure){
   ItemStack observed=owner.getInventory().getItem(slot);
   if(replacement.equals(observed)&&binding.cached!=null)binding.cached.setItem(observed);
   throw failure; // durable intent remains pending; never reconstruct an unknown slot.
  }
  ItemStack placed=owner.getInventory().getItem(slot);
  if(!replacement.equals(placed))throw new IllegalStateException("Wand physical write readback failed; durable receipt remains pending");
  if(binding.cached!=null)binding.cached.setItem(placed);
  if(binding.cached!=null&&!placed.equals(binding.cached.getItem()))throw new IllegalStateException("Native cache write readback failed");
 }
 void redraw(Player owner,int slot,ItemStack expected){
  Binding b=bind(owner,slot,expected);if(b.cached==null)return;
  b.cached.updateLore();ItemStack refreshed=b.cached.getItem(),current=owner.getInventory().getItem(slot);
  if(!sameOutsideLore(expected,current)||!sameOutsideLore(expected,refreshed))throw new IllegalStateException("Native lore callback changed physical item context");
  if(bind(owner,slot,current,true).cached!=b.cached)throw new IllegalStateException("Native cached wand changed during lore callback");
  owner.getInventory().setItem(slot,refreshed);
  if(!refreshed.equals(owner.getInventory().getItem(slot)))throw new IllegalStateException("Native lore physical readback failed");
 }
 private static boolean sameOutsideLore(ItemStack a,ItemStack b){if(a==null||b==null)return false;ItemStack x=a.clone(),y=b.clone();for(ItemStack item:List.of(x,y)){var meta=item.getItemMeta();meta.lore(null);item.setItemMeta(meta);}return x.equals(y);}

 static boolean sameOutsideMaker(ItemStack a,ItemStack b){
  if(a==null||b==null)return false;ItemStack x=a.clone(),y=b.clone();
  for(ItemStack i:List.of(x,y)){var meta=i.getItemMeta();for(var key:new HashSet<>(meta.getPersistentDataContainer().getKeys()))if(key.getNamespace().equals("excelmakersmark"))meta.getPersistentDataContainer().remove(key);i.setItemMeta(meta);}return x.equals(y);
 }
}
