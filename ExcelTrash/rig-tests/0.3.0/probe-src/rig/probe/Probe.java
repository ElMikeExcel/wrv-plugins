package rig.probe;
import org.bukkit.*;import org.bukkit.block.*;import org.bukkit.command.*;import org.bukkit.entity.Player;import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Event;import org.bukkit.event.block.Action;import org.bukkit.event.player.PlayerInteractEvent;import org.bukkit.inventory.EquipmentSlot;
/** Test-rig only. open/break as before; tap = a real right-click on the block: fires PlayerInteractEvent (HAND), then opens it like vanilla unless denied. */
public class Probe extends JavaPlugin implements org.bukkit.event.Listener {
  boolean protect;
  public void onEnable() { getServer().getPluginManager().registerEvents(this, this); }
  /** Same as a keep-inventory plugin onDeath. */
  @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
  public void onDeath(org.bukkit.event.entity.PlayerDeathEvent e) { if (protect) { e.setKeepInventory(true); e.getDrops().clear(); } }
  public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
    if (a[0].equals("protect")) { protect = a[1].equals("on"); s.sendMessage("probe protect " + protect); return true; }
    Player p = Bukkit.getPlayerExact(a[1]); Block b = p.getWorld().getBlockAt(Integer.parseInt(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4]));
    switch (a[0]) {
      case "open" -> { if (b.getState() instanceof Container ct) { var v = p.openInventory(ct.getInventory()); s.sendMessage("probe open " + (v != null && p.getOpenInventory().getTopInventory().equals(ct.getInventory()) ? "OPENED" : "DENIED")); } }
      case "break" -> { boolean ok = p.breakBlock(b); s.sendMessage("probe break " + (ok ? "BROKEN" : "DENIED") + " now=" + b.getType()); }
      case "tap" -> {
        PlayerInteractEvent ev = new PlayerInteractEvent(p, Action.RIGHT_CLICK_BLOCK, p.getInventory().getItemInMainHand(), b, BlockFace.NORTH, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(ev);
        boolean denied = ev.useInteractedBlock() == Event.Result.DENY;
        if (!denied && b.getState() instanceof Container ct) p.openInventory(ct.getInventory());
        s.sendMessage("probe tap " + (denied ? "VANILLA-DENIED" : "VANILLA-OPENED"));
      }
    }
    return true;
  }
}
