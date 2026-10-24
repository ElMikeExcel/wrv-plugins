package rig.probe;
import org.bukkit.*;import org.bukkit.block.*;import org.bukkit.command.*;import org.bukkit.entity.Player;import org.bukkit.plugin.java.JavaPlugin;
/** Test-rig only: lets console make a player open or break a block (fires the normal Bukkit events). */
public class Probe extends JavaPlugin {
  public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
    Player p = Bukkit.getPlayerExact(a[1]); Block b = p.getWorld().getBlockAt(Integer.parseInt(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4]));
    switch (a[0]) {
      case "open" -> { if (b.getState() instanceof Container ct) { var v = p.openInventory(ct.getInventory()); s.sendMessage("probe open " + (v != null && p.getOpenInventory().getTopInventory().equals(ct.getInventory()) ? "OPENED" : "DENIED")); } }
      case "break" -> { boolean ok = p.breakBlock(b); s.sendMessage("probe break " + (ok ? "BROKEN" : "DENIED") + " now=" + b.getType()); }
    }
    return true;
  }
}
