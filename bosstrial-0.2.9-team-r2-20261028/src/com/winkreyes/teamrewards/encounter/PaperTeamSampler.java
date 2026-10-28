package com.winkreyes.teamrewards.encounter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
/** Called synchronously by owning encounter plugin. Never loads chunks or discovers fights by nearby names. */
public final class PaperTeamSampler {
 private final TeamAdmission admission;
 private final Supplier<Collection<TeamAdmission.Encounter>> activeEncounters;
 public PaperTeamSampler(TeamAdmission admission,Supplier<Collection<TeamAdmission.Encounter>> activeEncounters){this.admission=Objects.requireNonNull(admission);this.activeEncounters=Objects.requireNonNull(activeEncounters);}
 public int sample()throws IOException{
  if(!Bukkit.isPrimaryThread())throw new IllegalStateException("main thread required");
  int count=0;
  for(var encounter:List.copyOf(activeEncounters.get())){
   if(!encounter.active())continue;
   World world=Bukkit.getWorld(encounter.center().world());
   if(world==null)continue;
   List<TeamAdmission.PlayerView> views=new ArrayList<>();
   for(Player player:world.getPlayers()){
    var loc=player.getLocation();
    var mode=switch(player.getGameMode()){
     case SURVIVAL->TeamAdmission.Mode.SURVIVAL;
     case ADVENTURE->TeamAdmission.Mode.ADVENTURE;
     case CREATIVE->TeamAdmission.Mode.CREATIVE;
     case SPECTATOR->TeamAdmission.Mode.SPECTATOR;
    };
    views.add(new TeamAdmission.PlayerView(player.getUniqueId(),new TeamAdmission.Point(world.getUID(),loc.getX(),loc.getY(),loc.getZ()),mode,player.isOnline(),!player.isDead()));
   }
   count+=admission.observe(encounter,views);
  }
  return count;
 }
}
