package rig;
import org.bukkit.*;import org.bukkit.block.Block;import org.bukkit.command.*;import org.bukkit.entity.*;import org.bukkit.plugin.java.JavaPlugin;import org.bukkit.event.*;import org.bukkit.event.player.PlayerTeleportEvent;
import java.lang.reflect.Method;
/** RIG ONLY. Never install live. Creates an EliteMobs-style dungeon world name, reports where a player
 * stands, logs cancelled teleports, and calls ExcelBedrockMenu's own private spawn methods by reflection
 * (the exact code the Bedrock Spawn button runs). */
public class RigEmWorlds extends JavaPlugin implements Listener {
  public void onEnable(){ getServer().getPluginManager().registerEvents(this,this);
    for(String n: new String[]{"em_id_test_dungeon"}){ if(Bukkit.getWorld(n)==null){ WorldCreator c=new WorldCreator(n); c.type(WorldType.FLAT); c.generateStructures(false); World w=c.createWorld(); getLogger().info("rig world "+n+" "+(w!=null)); } } }
  @EventHandler(priority=EventPriority.MONITOR) public void tp(PlayerTeleportEvent e){ getLogger().info("RIGTP "+e.getPlayer().getName()+" cause="+e.getCause()+" cancelled="+e.isCancelled()+" from="+e.getFrom().getWorld().getName()+" to="+(e.getTo()==null?"null":e.getTo().getWorld().getName()+" "+e.getTo().getBlockX()+","+e.getTo().getBlockY()+","+e.getTo().getBlockZ())); }
  public boolean onCommand(CommandSender s, Command c, String l, String[] a){
    try{
    if(a.length>=2&&a[0].equals("where")){ Player p=Bukkit.getPlayerExact(a[1]); if(p==null){s.sendMessage("RIGWHERE none");return true;} Location L=p.getLocation(); Block f=L.getBlock(), h=f.getRelative(0,1,0), u=f.getRelative(0,-1,0);
      s.sendMessage("RIGWHERE "+p.getName()+" world="+L.getWorld().getName()+" env="+L.getWorld().getEnvironment()+" pos="+L.getBlockX()+","+L.getBlockY()+","+L.getBlockZ()+" feet="+f.getType()+" head="+h.getType()+" below="+u.getType()+" feetPassable="+f.isPassable()+" headPassable="+h.isPassable()); return true; }
    if(a.length>=2&&(a[0].equals("ebmspawn")||a[0].equals("ebmfallback"))){ Player p=Bukkit.getPlayerExact(a[1]); Object ebm=Bukkit.getPluginManager().getPlugin("ExcelBedrockMenu");
      Method m=ebm.getClass().getDeclaredMethod(a[0].equals("ebmspawn")?"teleportToSpawn":"villageFallback",Player.class); m.setAccessible(true); m.invoke(ebm,p); s.sendMessage("RIGEBM invoked "+m.getName()+" for "+p.getName()); return true; }
    if(a.length>=1&&a[0].equals("dragon")){ World e=Bukkit.getWorlds().stream().filter(w->w.getEnvironment()==World.Environment.THE_END).findFirst().orElse(null); if(e==null){s.sendMessage("RIGDRAGON noend");return true;}
      org.bukkit.boss.DragonBattle b=e.getEnderDragonBattle(); s.sendMessage("RIGDRAGON world="+e.getName()+" battle="+(b!=null)+" dragon="+(b!=null&&b.getEnderDragon()!=null&&!b.getEnderDragon().isDead())+" killedBefore="+(b!=null&&b.hasBeenPreviouslyKilled())); return true; }
    if(a.length>=1&&a[0].equals("worlds")){ StringBuilder sb=new StringBuilder("RIGWORLDS"); for(World w:Bukkit.getWorlds()) sb.append(" ").append(w.getName()).append("/").append(w.getEnvironment()); s.sendMessage(sb.toString()); return true; }
    }catch(Throwable t){ s.sendMessage("RIGERR "+t); getLogger().warning("RIGERR "+t); return true; }
    s.sendMessage("rigem where|ebmspawn|ebmfallback <p> | dragon | worlds"); return true; }
}
