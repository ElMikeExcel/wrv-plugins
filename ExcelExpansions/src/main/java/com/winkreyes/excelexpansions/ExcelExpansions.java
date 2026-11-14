package com.winkreyes.excelexpansions;
import com.winkreyes.excelexpansions.core.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit; import org.bukkit.OfflinePlayer;
import org.bukkit.command.*; import org.bukkit.entity.Player;
import org.bukkit.event.*; import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*; import java.nio.file.*; import java.time.*; import java.util.*;
/** ExcelExpansions 0.1.1 - WRV release watcher, TAP-CARD PROPOSER and RELEASER,
 *  plus the plugin-owned per-player LEVEL STORE (Mike, Nov 14 2026): EliteMobs
 *  stores no canonical combat level (LOCK-PREP #16), so this plugin persists
 *  the named EM signals itself - PlayerData.HighestLevelKilled +
 *  PlayerData.Score read read-only from plugins/EliteMobs/data/player_data.db -
 *  plus a last-active timestamp (Bukkit last-seen / join / quit) feeding the
 *  watcher's 14-day ACTIVE window. Refresh cadence: on enable, on join, on
 *  quit, and every level-store.refresh-minutes while online (async).
 *  The watcher reads the store via StoreCombatLevelSource; it never reads an
 *  invented or XP-converted number.
 *  0.1.1 adds the DESIGN section 3/4.2/5 flow on top of the 0.1.0 watcher:
 *  a proposer tick every watcher.cadence-hours (DESIGN 2.6) evaluates the
 *  manifest against the store roster; a pack that goes DUE produces ONE
 *  proposal per due episode, rendered to Mike in game as a chat card
 *  (Adventure click component for Java + the typed tap command spelled out
 *  for Bedrock). Untapped stays due silently; re-surfacing is the rundown
 *  command + delivery of the pending card on Mike's join. A tap is honoured
 *  only from Mike (console, a proposal.mike-uuids player, or op when
 *  proposal.allow-op) and only against the live proposal's token; the
 *  releaser then flips the anchored isEnabled flag in the row's
 *  content_package files (backup first, per file). There is NO auto-release
 *  path: no tap, no release, ever (DESIGN 3.2). */
public final class ExcelExpansions extends JavaPlugin implements CommandExecutor, Listener {
 private List<ManifestRow> rows=List.of(); private Watcher watcher;
 private LevelStore store=new LevelStore(); private Path storeFile;
 private CombatLevelSource levelSource; private volatile String refreshNote="not refreshed yet";
 // --- 0.1.1 proposer / releaser ---
 private Proposer proposer; private final ReleaseService releaseService=new ReleaseService();
 private ReleaseState releaseState=new ReleaseState(); private Path stateFile; private Path pluginsDir;
 private final Object tickLock=new Object();
 public void onEnable(){ saveDefaultConfig();
  try(InputStream in=getResource("releases.yml")){ rows=ManifestLoader.load(in); }catch(Exception e){ getLogger().severe("releases.yml load failed: "+e); }
  String f=getConfig().getString("watcher.dynamic-floor",""); Integer floor=(f==null||f.isBlank()||f.equals("null"))?null:Integer.valueOf(f.trim());
  watcher=new Watcher(floor);
  // --- level store: load persisted state, wire as the watcher's band source ---
  String storeName=getConfig().getString("level-store.file","level-store.txt");
  storeFile=getDataFolder().toPath().resolve(storeName);
  try{ store=LevelStore.load(storeFile); }catch(Exception e){ getLogger().warning("level store load failed ("+e+"); starting empty"); store=new LevelStore(); }
  levelSource=new StoreCombatLevelSource(store);
  // --- release state overlay (0.1.1): load persisted due/proposed/released state ---
  pluginsDir=getDataFolder().getParentFile().toPath();
  String stateName=getConfig().getString("release.state-file","release-state.txt");
  stateFile=getDataFolder().toPath().resolve(stateName);
  try{ releaseState=ReleaseState.load(stateFile); }catch(Exception e){ getLogger().warning("release state load failed ("+e+"); starting empty"); releaseState=new ReleaseState(); }
  proposer=new Proposer(watcher);
  long cadenceH=Math.max(1L,getConfig().getLong("watcher.cadence-hours",6L));
  getLogger().info("ExcelExpansions 0.1.1: "+rows.size()+" manifest rows loaded; IN-BAND source: "+levelSource.name()+"; level store: "+store.size()+" players; release state: "+releaseState.size()+" rows off initial; proposer cadence: "+cadenceH+"h; dynamic floor: "+(floor==null?"unset (TBD at build)":floor));
  if(getCommand("excelexpansions")!=null) getCommand("excelexpansions").setExecutor(this);
  getServer().getPluginManager().registerEvents(this,this);
  long mins=Math.max(1L,getConfig().getLong("level-store.refresh-minutes",15L));
  Bukkit.getScheduler().runTaskLaterAsynchronously(this, this::refreshStore, 40L);
  Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::refreshStore, mins*1200L, mins*1200L);
  Bukkit.getScheduler().runTaskLaterAsynchronously(this, this::proposerTick, 1200L);
  Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::proposerTick, cadenceH*72000L, cadenceH*72000L);
 }
 public void onDisable(){ try{ store.save(storeFile); }catch(Exception e){ getLogger().warning("level store save on disable failed: "+e); }
  try{ releaseState.save(stateFile); }catch(Exception e){ getLogger().warning("release state save on disable failed: "+e); } }
 @EventHandler public void onJoin(PlayerJoinEvent e){ touchOnline(e.getPlayer()); Bukkit.getScheduler().runTaskAsynchronously(this,this::refreshStore);
  // Delivery, not a new proposal (DESIGN 3.3/3.4): if Mike joins with a card
  // already in flight, hand him that same episode's card again.
  if(isMike(e.getPlayer())){ Player mike=e.getPlayer();
   Bukkit.getScheduler().runTaskLater(this, ()->{ for(ManifestRow r: rows) proposer.pendingCard(r,releaseState).ifPresent(p->sendCard(mike,p)); }, 60L); } }
 @EventHandler public void onQuit(PlayerQuitEvent e){ touchOnline(e.getPlayer()); try{ store.save(storeFile); }catch(Exception ex){ getLogger().warning("level store save on quit failed: "+ex); } }
 private void touchOnline(Player p){ store.touch(p.getUniqueId(), Instant.now(), "Bukkit last-seen / join-quit (ExcelExpansions store)"); }
 /** One refresh pass (async): Bukkit last-seen sweep for the ACTIVE window,
  *  then a read-only EM PlayerData read for the named signals. A failed or
  *  unavailable EM read retains last-known signals (noted, never wiped). */
 private void refreshStore(){
  for(OfflinePlayer op : Bukkit.getOfflinePlayers()){ long ls=op.getLastSeen(); if(ls>0L) store.touch(op.getUniqueId(), Instant.ofEpochMilli(ls), "Bukkit OfflinePlayer.getLastSeen"); }
  for(Player p : Bukkit.getOnlinePlayers()) touchOnline(p);
  File db=emDbFile();
  if(!EmDbReader.driverPresent()){ refreshNote="EM read skipped: SQLite JDBC driver absent at runtime - last-known signals retained"; getLogger().warning(refreshNote); persist(); return; }
  if(!db.isFile()){ refreshNote="EM read skipped: player_data.db not found at "+db.getPath()+" - last-known signals retained"; getLogger().warning(refreshNote); persist(); return; }
  try{ Map<UUID,EmDbReader.Signals> all=EmDbReader.readAll(db);
   int applied=0; for(Map.Entry<UUID,EmDbReader.Signals> e : all.entrySet()){ store.putSignals(e.getKey(), e.getValue().highestLevelKilled(), e.getValue().score(), "EliteMobs PlayerData (player_data.db): HighestLevelKilled/Score"); applied++; }
   refreshNote="EM PlayerData read OK: "+applied+" rows applied from "+db.getPath();
  }catch(Exception e){ refreshNote="EM read failed ("+e.getMessage()+") - last-known signals retained"; getLogger().warning(refreshNote); }
  persist();
 }
 private File emDbFile(){ String rel=getConfig().getString("level-store.em-db-path","EliteMobs/data/player_data.db");
  File pluginsDirF=getDataFolder().getParentFile(); return pluginsDirF==null? new File(rel) : new File(pluginsDirF, rel); }
 private void persist(){ try{ store.save(storeFile); }catch(Exception e){ getLogger().warning("level store save failed: "+e); } }
 private void persistState(){ try{ releaseState.save(stateFile); }catch(Exception e){ getLogger().warning("release state save failed: "+e); } }
 // ================= 0.1.1 proposer / releaser wiring =================
 private ZoneId zone(){ try{ return ZoneId.of(getConfig().getString("watcher.timezone","America/Chicago")); }catch(Exception e){ return ZoneId.of("America/Chicago"); } }
 /** The New Year turn (DESIGN 4, LOCKED occasion, no exact dates): the days
  *  straddling Dec 31 -> Jan 1 in the watcher timezone - the design's own
  *  wording. No wider exact-day window is invented anywhere. */
 static boolean newYearTurn(ZonedDateTime z){ return (z.getMonth()==Month.DECEMBER && z.getDayOfMonth()==31) || (z.getMonth()==Month.JANUARY && z.getDayOfMonth()==1); }
 /** One proposer tick (async): build the roster from the level store (the
  *  watcher's only level source), evaluate, persist state, and deliver any
  *  NEW proposal cards to Mike if he is online. Untapped proposals are NOT
  *  re-posted - delivery of a pending card happens on Mike's join and via
  *  the rundown command only (DESIGN 3.3/3.4). */
 private void proposerTick(){
  synchronized(tickLock){
   Instant now=Instant.now(); ZonedDateTime z=now.atZone(zone());
   List<Watcher.Player> all=new ArrayList<>();
   for(LevelStore.Entry e : store.all()) all.add(new Watcher.Player(e.uuid(), e.lastActive(), levelSource.combatLevel(e.uuid())));
   List<Watcher.Player> active=Watcher.activeRoster(all, now);
   List<Proposer.Proposal> fresh;
   try{ fresh=proposer.tick(rows, active, releaseState, now, z.getMonth(), newYearTurn(z)); }
   catch(Exception e){ getLogger().warning("proposer tick failed: "+e); return; }
   persistState();
   for(Proposer.Proposal p : fresh){
    getLogger().info("PROPOSAL "+p.kind()+" "+p.rowId()+" "+p.displayName()+" - "+p.reason()+" | tap: "+p.tapCommand());
    Bukkit.getScheduler().runTask(this, ()->deliverCard(p)); }
  }
 }
 private Set<UUID> mikeUuids(){ Set<UUID> out=new LinkedHashSet<>();
  for(String s : getConfig().getStringList("proposal.mike-uuids")){ try{ out.add(UUID.fromString(s.trim())); }catch(Exception e){ getLogger().warning("proposal.mike-uuids: bad UUID skipped: "+s); } }
  return out; }
 private boolean isMike(Player p){ return mikeUuids().contains(p.getUniqueId()) || (p.isOp() && getConfig().getBoolean("proposal.allow-op",true)); }
 private boolean isMikeSender(CommandSender s){ return !(s instanceof Player) || isMike((Player)s); }
 private void deliverCard(Proposer.Proposal p){
  boolean delivered=false;
  for(Player pl : Bukkit.getOnlinePlayers()) if(isMike(pl)){ sendCard(pl,p); delivered=true; }
  if(!delivered) getLogger().info("proposal "+p.rowId()+" posted; Mike not online - the same card delivers on his join (no re-post, DESIGN 3.3)"); }
 /** Render one proposal as an in-game chat card (DESIGN 3.1 contents): pack +
  *  phase, band + its contents-check source, the watcher reason (incl. the
  *  in-band count), any fixed-band urgency note and band note, then the tap
  *  affordance - a clickable component for Java and the typed command spelled
  *  out for Bedrock (Floodgate chat cannot be relied on for clicks). */
 private void sendCard(Player mike, Proposer.Proposal p){
  boolean retire=p.kind()==Proposer.Kind.RETIREMENT;
  mike.sendMessage(Component.text("----- ExcelExpansions -----", NamedTextColor.DARK_GRAY));
  mike.sendMessage(Component.text((retire? "Season over: " : "New content is DUE: ")+p.displayName(), retire? NamedTextColor.AQUA : NamedTextColor.GOLD, TextDecoration.BOLD));
  mike.sendMessage(Component.text("Phase "+p.phase()+"  ·  Band: "+p.bandLabel()+(p.bandText()==null||p.bandText().isBlank()? "" : " - "+p.bandText()), NamedTextColor.WHITE));
  if(p.bandSource()!=null && !p.bandSource().isBlank()) mike.sendMessage(Component.text("Band source: "+p.bandSource(), NamedTextColor.GRAY));
  mike.sendMessage(Component.text("Why now: "+p.reason(), NamedTextColor.WHITE));
  if(p.urgencyNote()!=null && !p.urgencyNote().isBlank()) mike.sendMessage(Component.text(p.urgencyNote(), NamedTextColor.YELLOW));
  if(p.bandNote()!=null && !p.bandNote().isBlank()) mike.sendMessage(Component.text("Note: "+p.bandNote(), NamedTextColor.GRAY));
  mike.sendMessage(Component.text(retire? "[ TAP TO RETIRE FOR THE SEASON ]" : "[ TAP TO RELEASE ]", NamedTextColor.GREEN, TextDecoration.BOLD)
    .clickEvent(ClickEvent.runCommand(p.tapCommand())).hoverEvent(HoverEvent.showText(Component.text("Runs: "+p.tapCommand()))));
  mike.sendMessage(Component.text("No click? Type:  "+p.tapCommand(), NamedTextColor.GRAY));
  mike.sendMessage(Component.text("No tap = no "+(retire? "retirement" : "release")+". It stays "+(retire? "open" : "due")+", silently.", NamedTextColor.DARK_GRAY));
 }
 private ManifestRow rowById(String id){ for(ManifestRow r: rows) if(r.id().equalsIgnoreCase(id)) return r; return null; }
 private boolean cmdTap(CommandSender s, String[] a){
  if(a.length<2){ s.sendMessage("Usage: /excelexpansions tap <row id> <token from the card>"); return true; }
  ManifestRow r=rowById(a[1]); if(r==null){ s.sendMessage("No manifest row "+a[1]); return true; }
  if(!isMikeSender(s)){ s.sendMessage("Only Mike can tap a release."); return true; }
  boolean console=!(s instanceof Player);
  String token=a.length>2? a[2] : null;
  ReleaseService.TapResult res=releaseService.tap(r, releaseState, token, console, console? "console" : s.getName(), pluginsDir, Instant.now());
  persistState();
  s.sendMessage(res.message());
  if(res.flip()!=null) for(Releaser.FileResult fr : res.flip().files()) s.sendMessage("  "+fr.relPath()+" - "+fr.status()+" ("+fr.detail()+")");
  if(res.outcome()==ReleaseService.Outcome.RELEASED || res.outcome()==ReleaseService.Outcome.RETIRED){
   getLogger().info("TAP "+res.outcome()+" "+r.id()+" "+r.displayName()+" by "+s.getName()+"; surfaces owed (supervisor, same pass) - see the release-state note");
   // DESIGN 3.6: once the in-flight proposal is tapped, the next due pack
   // may propose - run a tick shortly instead of waiting out the cadence.
   Bukkit.getScheduler().runTaskLaterAsynchronously(this, this::proposerTick, 100L);
  }
  return true; }
 private boolean cmdRundown(CommandSender s){
  // DESIGN 3.4: the rundown is the ONLY re-surfacing path for untapped due
  // packs. Lines only - plus, for Mike himself in game, the pending cards.
  int shown=0;
  for(ManifestRow r: rows){ String eff=releaseState.stateOf(r);
   if(ReleaseState.UNRELEASED.equals(eff)) continue;
   ReleaseState.Entry e=releaseState.get(r.id()).orElse(null);
   s.sendMessage(r.id()+" "+r.displayName()+" - "+eff+(e!=null && e.dueSince()!=null? " (due since "+e.dueSince()+")" : "")+(e!=null && e.note()!=null && !e.note().isBlank()? " - "+e.note() : ""));
   shown++; }
  if(shown==0) s.sendMessage("Rundown: nothing due, proposed, or released off its initial state.");
  if(s instanceof Player && isMike((Player)s)) for(ManifestRow r: rows) proposer.pendingCard(r,releaseState).ifPresent(p->sendCard((Player)s,p));
  return true; }
 public boolean onCommand(CommandSender s,Command c,String l,String[] a){ String sub=a.length>0?a[0]:"status";
  if(sub.equals("tap")) return cmdTap(s,a);
  if(sub.equals("rundown")) return cmdRundown(s);
  if(sub.equals("list")){ for(ManifestRow r: rows) s.sendMessage(r.id()+" "+r.slug()+" phase="+r.phase()+" band="+(r.bandMin()==null?"TBD/dynamic":r.bandMin()+"-"+r.bandMax())+" "+releaseState.stateOf(r)); return true; }
  if(sub.equals("store")){ s.sendMessage("Level store: "+store.size()+" players; IN-BAND source: "+levelSource.name()); s.sendMessage("Last refresh: "+refreshNote);
   for(LevelStore.Entry e : store.all()) s.sendMessage(" "+e.uuid()+" HLK="+(e.highestLevelKilled()==null?"?":e.highestLevelKilled())+" Score="+(e.score()==null?"?":e.score())+" lastActive="+(e.lastActive()==null?"?":e.lastActive())); return true; }
  long proposed=rows.stream().filter(r->ReleaseState.PROPOSED.equals(releaseState.stateOf(r))||ReleaseState.RETIREMENT_PROPOSED.equals(releaseState.stateOf(r))).count();
  long due=rows.stream().filter(r->ReleaseState.DUE.equals(releaseState.stateOf(r))).count();
  long released=rows.stream().filter(r->ReleaseState.RELEASED.equals(releaseState.stateOf(r))).count();
  s.sendMessage("ExcelExpansions 0.1.1 - rows: "+rows.size()+" (M-01..M-57); watcher + proposer + releaser live; NO auto-release (Mike tap only). States: "+proposed+" proposed, "+due+" due, "+released+" released. IN-BAND source: "+levelSource.name()+". Level store: "+store.size()+" players ("+refreshNote+")."); return true; }
 public List<ManifestRow> rows(){return rows;} public Watcher watcher(){return watcher;}
 public LevelStore store(){return store;} public CombatLevelSource levelSource(){return levelSource;}
 public ReleaseState releaseState(){return releaseState;} public Proposer proposer(){return proposer;}
}

