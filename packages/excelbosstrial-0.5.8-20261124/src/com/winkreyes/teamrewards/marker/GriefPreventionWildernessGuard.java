/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.teamrewards.marker;
import java.util.*;import org.bukkit.*;import org.bukkit.block.Block;import org.bukkit.plugin.Plugin;
/** Conservative GP16.18.7 public API adapter. Only explicitly reviewed worlds; NO claim-owner exceptions.
 * reviewedWorlds is an operator attestation that this guard covers the world's installed protection stack.
 * Never auto-populate it from loaded worlds: missing policy, GP, API or visibility denies placement/removal.
 */
public final class GriefPreventionWildernessGuard implements PersonalChestMarkers.Authorization {
 private final Server server;private final Set<UUID> reviewedWorlds;
 public GriefPreventionWildernessGuard(Server server,Set<UUID> reviewedWorlds){this.server=Objects.requireNonNull(server);this.reviewedWorlds=Set.copyOf(reviewedWorlds);}
 @Override public boolean mayPlace(UUID owner,Block block){return permitted(block);}
 @Override public boolean mayRemove(UUID owner,Block block){return permitted(block);}
 private boolean permitted(Block block){
  if(!Bukkit.isPrimaryThread())return false;
  World world=block.getWorld();if(!reviewedWorlds.contains(world.getUID())||!world.isChunkLoaded(block.getX()>>4,block.getZ()>>4))return false;
  Location spawn=world.getSpawnLocation();int radius=Math.max(0,server.getSpawnRadius())+1;
  if(Math.abs((long)block.getX()-spawn.getBlockX())<=radius&&Math.abs((long)block.getZ()-spawn.getBlockZ())<=radius)return false;
  for(double dx:new double[]{0.01,0.99})for(double dz:new double[]{0.01,0.99})if(!world.getWorldBorder().isInside(new Location(world,block.getX()+dx,block.getY(),block.getZ()+dz)))return false;
  Plugin gp=server.getPluginManager().getPlugin("GriefPrevention");if(gp==null||!gp.isEnabled())return false;
  try {
   Class<?> claim=Class.forName("me.ryanhamshire.GriefPrevention.Claim",false,gp.getClass().getClassLoader());
   Object store=gp.getClass().getField("dataStore").get(gp);
   // Ignore height, matching established local ownership adapter: no vertical boundary bypass.
   return store.getClass().getMethod("getClaimAt",Location.class,boolean.class,claim).invoke(store,block.getLocation(),true,null)==null;
  }catch(ReflectiveOperationException|RuntimeException|LinkageError unknown){return false;}
 }
}
