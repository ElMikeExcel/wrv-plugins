package rig;
import org.bukkit.Bukkit;import org.bukkit.block.Block;import org.bukkit.block.Container;import org.bukkit.command.*;import org.bukkit.entity.Player;import org.bukkit.plugin.java.JavaPlugin;
/** RIG-ONLY test aid (never shipped): opens a block container for a player server-side, the same
 *  Player.openInventory path a real click ends in, so InventoryOpenEvent/InventoryCloseEvent fire.
 *  Needed because bot block clicks don't reach Paper 26.3 through ViaVersion on the rig. */
public final class RigOpen extends JavaPlugin {
  @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a){
    if(a.length<4){s.sendMessage("rigopen <player> <x> <y> <z> | rigopen <player> ec");return true;}
    Player p=Bukkit.getPlayerExact(a[0]);if(p==null){s.sendMessage("rigopen: no player");return true;}
    Block b=p.getWorld().getBlockAt(Integer.parseInt(a[1]),Integer.parseInt(a[2]),Integer.parseInt(a[3]));
    if(b.getType()==org.bukkit.Material.ENDER_CHEST){p.openInventory(p.getEnderChest());s.sendMessage("rigopen: opened ender chest for "+p.getName());return true;}
    if(!(b.getState() instanceof Container ct)){s.sendMessage("rigopen: not a container "+b.getType());return true;}
    p.openInventory(ct.getInventory());s.sendMessage("rigopen: opened "+b.getType()+" for "+p.getName());return true;}
}
