package rig;
import org.bukkit.*;import org.bukkit.command.*;import org.bukkit.entity.Player;import org.bukkit.plugin.java.JavaPlugin;
/** RIG-ONLY test aid for ExcelMakersMark (never shipped). Bot block clicks don't reach Paper 26.3 through
 *  ViaVersion on the rigs, so this opens the real block menus server-side (same menus a right-click opens,
 *  bound to the block location so Prepare*Event and result pickup run the normal vanilla path). */
@SuppressWarnings("deprecation")
public final class MMRigAid extends JavaPlugin {
  @Override public void onEnable(){ // EliteMobs-style dungeon world name that EliteMobs itself does not register (rig only)
    if(Bukkit.getWorld("em_id_test_dungeon")==null){WorldCreator c=new WorldCreator("em_id_test_dungeon");c.type(WorldType.FLAT);c.generateStructures(false);World w=c.createWorld();getLogger().info("rig world em_id_test_dungeon "+(w!=null));}}
  @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a){
    if(a.length<2){s.sendMessage("mmrig open <player> craft|anvil|smith|grind <x> <y> <z> | use <player> [releaseTicks] | attack <player> <target>");return true;}
    Player p=Bukkit.getPlayerExact(a[1]);if(p==null){s.sendMessage("mmrig: no player");return true;}
    if(a[0].equals("use")){int rel=a.length>2?Integer.parseInt(a[2]):0;try{useItem(p);s.sendMessage("mmrig: used "+p.getInventory().getItemInMainHand().getType()+" active="+p.hasActiveItem());
        if(rel>0)Bukkit.getScheduler().runTaskLater(this,()->{try{Object h=handle(p);call(h,"releaseUsingItem");getLogger().info("mmrig: released for "+p.getName());}catch(Throwable t){getLogger().warning("release: "+t);}},rel);}
      catch(Throwable t){s.sendMessage("mmrig: use failed "+t);}return true;}
    if(a[0].equals("attack")){org.bukkit.entity.Entity t=null;Player tp=Bukkit.getPlayerExact(a[2]);if(tp!=null)t=tp;else{double best=99;for(org.bukkit.entity.Entity e:p.getNearbyEntities(8,4,8)){if(e.getType().name().equalsIgnoreCase(a[2])&&e.getLocation().distance(p.getLocation())<best){best=e.getLocation().distance(p.getLocation());t=e;}}}
      if(t==null){s.sendMessage("mmrig: no target "+a[2]);return true;}p.attack(t);s.sendMessage("mmrig: "+p.getName()+" attacked "+t.getType()+" with "+p.getInventory().getItemInMainHand().getType());return true;}
    Location loc=new Location(p.getWorld(),Integer.parseInt(a[3]),Integer.parseInt(a[4]),Integer.parseInt(a[5]));
    switch(a[2]){
      case "craft"->p.openWorkbench(loc,false);
      case "anvil"->p.openAnvil(loc,false);
      case "smith"->p.openSmithingTable(loc,false);
      case "grind"->p.openGrindstone(loc,false);
      case "enchant"->p.openEnchanting(loc,true);
      default->{s.sendMessage("mmrig: unknown "+a[2]);return true;}}
    s.sendMessage("mmrig: opened "+a[2]+" ("+loc.getBlock().getType()+") for "+p.getName()+" view="+p.getOpenInventory().getType());return true;}

  static Object handle(Player p)throws Exception{return p.getClass().getMethod("getHandle").invoke(p);}
  static Object call(Object o,String n,Object...args)throws Exception{for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())for(java.lang.reflect.Method m:c.getDeclaredMethods())if(m.getName().equals(n)&&m.getParameterCount()==args.length){m.setAccessible(true);return m.invoke(o,args);}throw new NoSuchMethodException(n);}
  /** The same server path a use_item packet takes: ServerPlayerGameMode.useItem(player, level, stack, MAIN_HAND). */
  static void useItem(Player p)throws Exception{Object h=handle(p);Object gm=null;for(Class<?> c=h.getClass();c!=null&&gm==null;c=c.getSuperclass())for(java.lang.reflect.Field f:c.getDeclaredFields())if(f.getType().getSimpleName().equals("ServerPlayerGameMode")){f.setAccessible(true);gm=f.get(h);break;}
    Object level=call(h,"level");Class<?> handCls=Class.forName("net.minecraft.world.InteractionHand");Object main=handCls.getField("MAIN_HAND").get(null);Object stack=call(h,"getItemInHand",main);
    for(java.lang.reflect.Method m:gm.getClass().getDeclaredMethods())if(m.getName().equals("useItem")&&m.getParameterCount()==4){m.setAccessible(true);m.invoke(gm,h,level,stack,main);return;}throw new NoSuchMethodException("useItem");}
}
