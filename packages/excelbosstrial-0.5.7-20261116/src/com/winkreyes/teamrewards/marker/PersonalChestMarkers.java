// Excel WRV: personal reward marker protection; original project attribution preserved.
package com.winkreyes.teamrewards.marker;
import java.util.*;import java.util.function.Consumer;import org.bukkit.*;import org.bukkit.block.*;import org.bukkit.entity.Player;import org.bukkit.event.*;import org.bukkit.event.block.*;import org.bukkit.event.entity.EntityExplodeEvent;import org.bukkit.event.inventory.*;import org.bukkit.event.player.PlayerInteractEvent;import org.bukkit.inventory.*;import org.bukkit.persistence.PersistentDataType;import org.bukkit.plugin.Plugin;

/** Optional empty chest markers. Claims remain exclusively in durable reward ledger/menu. */
public final class PersonalChestMarkers implements Listener {
 public interface Authorization { boolean mayPlace(UUID owner,Block emptyTarget); boolean mayRemove(UUID owner,Block marker); }
 public enum Placement { PLACED, MENU_FALLBACK }
 private final Plugin plugin;private final MarkerRegistry registry;private final Authorization authorization;private final Consumer<Player> openClaims;
 private final NamespacedKey markerKey,ownerKey;
 public PersonalChestMarkers(Plugin plugin,MarkerRegistry registry,Authorization authorization,Consumer<Player> opener){this.plugin=Objects.requireNonNull(plugin);this.registry=Objects.requireNonNull(registry);this.authorization=Objects.requireNonNull(authorization);openClaims=Objects.requireNonNull(opener);markerKey=new NamespacedKey(plugin,"personal_reward_marker");ownerKey=new NamespacedKey(plugin,"personal_reward_owner");}
 /** No chunk loads/teleports: bounded search at supplied surface height, dry supporting ground required. */
 public Placement placeNear(UUID owner,Location anchor){
  main();Objects.requireNonNull(owner);if(!registry.writable())return Placement.MENU_FALLBACK;if(registry.all().stream().anyMatch(m->m.owner().equals(owner)))return Placement.MENU_FALLBACK;World world=anchor.getWorld();if(world==null)return Placement.MENU_FALLBACK;
  for(int radius=1;radius<=3;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
   if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;int x=anchor.getBlockX()+dx,y=anchor.getBlockY(),z=anchor.getBlockZ()+dz;
   if(y<=world.getMinHeight()||y+1>=world.getMaxHeight()||!loadedArea(world,x,z))continue;
   Block b=world.getBlockAt(x,y,z);if(!safe(b))continue;
   try{if(!authorization.mayPlace(owner,b))continue;}catch(RuntimeException unavailable){return Placement.MENU_FALLBACK;}
   var marker=new MarkerRegistry.Marker(UUID.randomUUID(),owner,world.getUID(),x,y,z);
   try{registry.add(marker);}catch(Exception persistence){return Placement.MENU_FALLBACK;}
   // Recheck after durable intent. No overwriting if authorization or surrounding world changed.
   boolean mutationStarted=false;
   try{if(!safe(b)||!authorization.mayPlace(owner,b)){
     try{registry.remove(marker.id());}catch(Exception failedCleanup){plugin.getLogger().warning("Unapplied marker intent needs inspection: "+marker.id());}
     return Placement.MENU_FALLBACK;
    }
    registry.verifyCurrent();
    mutationStarted=true;b.setType(Material.CHEST,false);Chest chest=(Chest)b.getState();chest.getPersistentDataContainer().set(markerKey,PersistentDataType.STRING,marker.id().toString());chest.getPersistentDataContainer().set(ownerKey,PersistentDataType.STRING,owner.toString());chest.setLock("reward-marker-"+marker.id());
    if(!chest.update(false,false)||proof(b)==null){plugin.getLogger().warning("Unproven physical marker requires manual review, not automatic block removal: "+marker.id());return Placement.MENU_FALLBACK;}
    return Placement.PLACED;
   }catch(Exception uncertain){if(!mutationStarted){try{registry.remove(marker.id());}catch(Exception failedCleanup){plugin.getLogger().warning("Unapplied marker intent needs inspection: "+marker.id());}return Placement.MENU_FALLBACK;}plugin.getLogger().warning("Marker placement uncertain; retain intent and inspect block manually: "+marker.id());return Placement.MENU_FALLBACK;}
  }
  return Placement.MENU_FALLBACK;
 }
 private boolean safe(Block b){
  if(b.getType()!=Material.AIR||b.getRelative(0,1,0).getType()!=Material.AIR)return false;
  if(!b.getWorld().getNearbyEntities(new org.bukkit.util.BoundingBox(b.getX(),b.getY(),b.getZ(),b.getX()+1,b.getY()+1,b.getZ()+1)).isEmpty())return false;
  Block floor=b.getRelative(0,-1,0);if(!floor.getType().isSolid()||floor.isLiquid())return false;
  for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
   Block near=b.getRelative(dx,0,dz);if(near.isLiquid()||near.getType()==Material.CHEST||near.getType()==Material.TRAPPED_CHEST)return false;
  }
  return true;
 }
 private static boolean loadedArea(World world,int x,int z){for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)if(!world.isChunkLoaded((x+dx)>>4,(z+dz)>>4))return false;return true;}
 private MarkerRegistry.Marker proof(Block b){
  if(!b.getWorld().isChunkLoaded(b.getX()>>4,b.getZ()>>4)||!(b.getState() instanceof Chest chest))return null;
  String id=chest.getPersistentDataContainer().get(markerKey,PersistentDataType.STRING),owner=chest.getPersistentDataContainer().get(ownerKey,PersistentDataType.STRING);
  try{var m=registry.get(UUID.fromString(id));return m!=null&&m.owner().toString().equals(owner)&&m.world().equals(b.getWorld().getUID())&&m.x()==b.getX()&&m.y()==b.getY()&&m.z()==b.getZ()?m:null;}catch(RuntimeException bad){return null;}
 }
 private boolean tagged(Inventory inventory){
  if(inventory==null)return false;
  InventoryHolder holder=inventory.getHolder();
  // Excel: inspect each physical chest directly. Chest#getInventory can return
  // the combined double chest again, so never recurse through side inventories.
  if(holder instanceof DoubleChest dc)return taggedChest(dc.getLeftSide())||taggedChest(dc.getRightSide());
  return taggedChest(holder);
 }
 private boolean taggedChest(InventoryHolder holder){return holder instanceof Chest chest&&chest.getPersistentDataContainer().has(markerKey,PersistentDataType.STRING);}
 public boolean cleanup(UUID id){main();if(!registry.writable())return false;var m=registry.get(id);if(m==null)return false;World w=Bukkit.getWorld(m.world());if(w==null||!w.isChunkLoaded(m.x()>>4,m.z()>>4))return false;Block b=w.getBlockAt(m.x(),m.y(),m.z());if(!m.equals(proof(b)))return false;
  try{if(!authorization.mayRemove(m.owner(),b))return false;}catch(RuntimeException unknown){return false;}
  Chest chest=(Chest)b.getState();if(!chest.getBlockInventory().isEmpty())return false; // Never destroy externally inserted property.
  try{registry.verifyCurrent();}catch(Exception externalChange){return false;}
  b.setType(Material.AIR,false);try{registry.remove(id);}catch(Exception uncertain){return false;}return true;
 }
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)public void interact(PlayerInteractEvent e){Block b=e.getClickedBlock();if(b==null||!(b.getState() instanceof Chest chest)||!chest.getPersistentDataContainer().has(markerKey,PersistentDataType.STRING))return;boolean prior=e.isCancelled();e.setCancelled(true);if(prior||e.getAction()!=Action.RIGHT_CLICK_BLOCK||e.getHand()!=EquipmentSlot.HAND)return;var m=proof(b);if(m!=null&&m.owner().equals(e.getPlayer().getUniqueId()))Bukkit.getScheduler().runTask(plugin,()->{if(e.getPlayer().isOnline())openClaims.accept(e.getPlayer());});}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)public void inventoryOpen(InventoryOpenEvent e){if(tagged(e.getInventory()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)public void click(InventoryClickEvent e){if(tagged(e.getView().getTopInventory()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)public void drag(InventoryDragEvent e){if(tagged(e.getView().getTopInventory()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)public void move(InventoryMoveItemEvent e){if(tagged(e.getSource())||tagged(e.getDestination()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)public void pickup(InventoryPickupItemEvent e){if(tagged(e.getInventory()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)public void breakBlock(BlockBreakEvent e){if(e.getBlock().getState() instanceof Chest chest&&chest.getPersistentDataContainer().has(markerKey,PersistentDataType.STRING))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)public void piston(BlockPistonExtendEvent e){if(e.getBlocks().stream().anyMatch(b->proof(b)!=null))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)public void piston(BlockPistonRetractEvent e){if(e.getBlocks().stream().anyMatch(b->proof(b)!=null))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)public void explode(EntityExplodeEvent e){e.blockList().removeIf(b->proof(b)!=null);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)public void explode(BlockExplodeEvent e){e.blockList().removeIf(b->proof(b)!=null);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)public void adjacentChest(BlockPlaceEvent e){if(e.getBlock().getType()!=Material.CHEST&&e.getBlock().getType()!=Material.TRAPPED_CHEST)return;for(var face:List.of(org.bukkit.block.BlockFace.NORTH,org.bukkit.block.BlockFace.SOUTH,org.bukkit.block.BlockFace.EAST,org.bukkit.block.BlockFace.WEST))if(proof(e.getBlock().getRelative(face))!=null){e.setCancelled(true);return;}}
 private static void main(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Main thread required");}
}
