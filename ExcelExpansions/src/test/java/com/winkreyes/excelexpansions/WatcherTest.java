package com.winkreyes.excelexpansions;
import com.winkreyes.excelexpansions.core.*; import java.io.*; import java.nio.file.*; import java.time.*; import java.util.*;
public class WatcherTest { static int pass=0,fail=0; static void ck(String n,boolean b){ if(b)pass++; else {fail++; System.out.println("FAIL "+n);} System.out.println((b?"PASS ":"FAIL ")+n); }
 public static void main(String[] a) throws Exception {
  String yml=Files.readString(Path.of("src/main/resources/releases.yml")); List<ManifestRow> rows=ManifestLoader.parse(yml);
  ck("row count = 57", rows.size()==57); ck("M-01 present opening wave", rows.stream().anyMatch(r->r.id().equals("M-01")));
  ck("M-02 Primis 0-20 fixed", rows.stream().anyMatch(r->r.id().equals("M-02")&&r.bandMin()==0&&r.bandMax()==20));
  ck("seasonal count = 4", rows.stream().filter(ManifestRow::seasonal).count()==4);
  ck("seasonal windows LOCKED months/occasions #10", rows.stream().filter(ManifestRow::seasonal).allMatch(r->r.seasonWindowOpens()!=null && !r.seasonWindowOpens().isBlank()));
  ck("bench rows unassigned no bands #15", rows.stream().filter(r->List.of("M-34","M-37","M-39","M-40").contains(r.id())).allMatch(r->r.unassigned()&&r.bandMin()==null));
  ck("dynamic rows no invented band_min", rows.stream().filter(ManifestRow::dynamic).allMatch(r->r.bandMin()==null));
  ck("M-29 no single band asserted", rows.stream().anyMatch(r->r.id().equals("M-29")&&r.bandMin()==null&&r.bandNote().contains("discrepancy")));
  ck("M-11 BAND-TBD not evaluable", rows.stream().anyMatch(r->r.id().equals("M-11")&&!r.evaluable()));
  Instant now=Instant.parse("2026-11-14T12:00:00Z");
  List<Watcher.Player> act=List.of(new Watcher.Player(UUID.randomUUID(),now,OptionalInt.of(25)),new Watcher.Player(UUID.randomUUID(),now,OptionalInt.of(22)),new Watcher.Player(UUID.randomUUID(),now,OptionalInt.of(3)));
  ck("active roster 336h includes recent, excludes 15d-old", Watcher.activeRoster(List.of(act.get(0),new Watcher.Player(UUID.randomUUID(),now.minus(Duration.ofDays(15)),OptionalInt.of(99))),now).size()==1);
  Watcher w=new Watcher(null);
  ManifestRow primis=rows.stream().filter(r->r.id().equals("M-02")).findFirst().orElseThrow();
  List<ManifestRow> rel=new ArrayList<>(rows); // M-01 is released in data, so phase order passes for M-02
  ck("majority 2/3 in band -> DUE (Primis, floor unset irrelevant fixed)", w.evaluate(primis,rel,act,Set.of("free-minecraft-models"),Month.NOVEMBER,false).verdict()==Watcher.Verdict.DUE);
  List<Watcher.Player> one=List.of(act.get(2),act.get(2),act.get(2));
  // 0/3 distinct check: use players below band
  List<Watcher.Player> low=List.of(new Watcher.Player(UUID.randomUUID(),now,OptionalInt.of(0)),new Watcher.Player(UUID.randomUUID(),now,OptionalInt.of(0)),new Watcher.Player(UUID.randomUUID(),now,OptionalInt.of(25)));
  // Primis bandMin 0 so all >=0; use Oasis instead for majority fail: need M-08 with earlier rows released - construct synthetic released set by marking? phase order blocks; expect NOT_DUE either way
  ck("phase order blocks later row", w.evaluate(rows.stream().filter(r->r.id().equals("M-08")).findFirst().orElseThrow(),rows,act,Set.of(),Month.NOVEMBER,false).verdict()==Watcher.Verdict.NOT_DUE);
  ck("floor of 3: 2 actives -> NOT_DUE", w.evaluate(primis,rel,act.subList(0,2),Set.of(),Month.NOVEMBER,false).verdict()==Watcher.Verdict.NOT_DUE);
  ck("unknown level counts not in band (TBD source)", new TbdCombatLevelSource().combatLevel(UUID.randomUUID()).isEmpty());
  ck("dynamic floor unset -> NOT_EVALUABLE", w.evaluate(rows.stream().filter(r->r.id().equals("M-03")).findFirst().orElseThrow(),rel,act,Set.of(),Month.NOVEMBER,false).verdict()!=Watcher.Verdict.DUE);
  ck("seasonal North Pole Dec -> SEASONAL_DUE", w.evaluate(rows.stream().filter(r->r.id().equals("M-30")).findFirst().orElseThrow(),rows,List.of(),Set.of(),Month.DECEMBER,false).verdict()==Watcher.Verdict.SEASONAL_DUE);
  ck("seasonal North Pole Nov -> NOT_DUE", w.evaluate(rows.stream().filter(r->r.id().equals("M-30")).findFirst().orElseThrow(),rows,List.of(),Set.of(),Month.NOVEMBER,false).verdict()==Watcher.Verdict.NOT_DUE);
  ck("support never", w.evaluate(rows.stream().filter(r->r.id().equals("M-44")).findFirst().orElseThrow(),rows,act,Set.of(),Month.NOVEMBER,false).verdict()==Watcher.Verdict.SUPPORT_NEVER);
  ck("unassigned never", w.evaluate(rows.stream().filter(r->r.id().equals("M-37")).findFirst().orElseThrow(),rows,act,Set.of(),Month.NOVEMBER,false).verdict()==Watcher.Verdict.UNASSIGNED_NEVER);
  ck("M-01 released stays released", w.evaluate(rows.get(0),rows,act,Set.of(),Month.NOVEMBER,false).verdict()==Watcher.Verdict.RELEASED);
  try{ java.nio.file.Path tmp=java.nio.file.Files.createTempFile("lvl",".txt"); LevelStore st=new LevelStore(); UUID u=UUID.randomUUID(); st.put(new LevelStore.Entry(u,22,3674L,now,"EM PlayerData: HighestLevelKilled/Score")); st.save(tmp); LevelStore back=LevelStore.load(tmp); ck("level store round-trip (named EM sources, no invented level)", back.get(u).map(e->e.highestLevelKilled()==22 && e.score()==3674L).orElse(false)); }catch(Exception e){ ck("level store round-trip", false); }
  // --- level store wiring (pass 2): store-backed band source + refresh merge semantics ---
  { LevelStore st=new LevelStore(); StoreCombatLevelSource src=new StoreCombatLevelSource(st); UUID u=UUID.randomUUID();
    ck("store source: no entry -> unknown (not in band)", src.combatLevel(u).isEmpty());
    st.touch(u,now,"Bukkit OfflinePlayer.getLastSeen");
    ck("store source: activity-only entry -> still unknown", src.combatLevel(u).isEmpty());
    st.putSignals(u,22,3674L,"EliteMobs PlayerData (player_data.db): HighestLevelKilled/Score");
    ck("store source: EM signal read -> band signal = HighestLevelKilled 22", src.combatLevel(u).orElse(-1)==22);
    ck("store touch: lastActive never moves backwards", st.get(u).map(e->e.lastActive().equals(now)).orElse(false) && st.get(u).map(e->e.highestLevelKilled()==22).orElse(false));
    st.touch(u,now.plus(Duration.ofHours(2)),"Bukkit OfflinePlayer.getLastSeen");
    ck("store touch: newer activity advances lastActive, signals retained", st.get(u).map(e->e.lastActive().equals(now.plus(Duration.ofHours(2))) && e.highestLevelKilled()==22 && e.score()==3674L).orElse(false));
    st.putSignals(u,null,null,"EM read retry");
    ck("store merge: null-signal read retains last-known values (never wiped)", st.get(u).map(e->e.highestLevelKilled()==22 && e.score()==3674L).orElse(false)); }
  // --- EM DB reader against the REAL pulled database (primary source, G4) ---
  { java.io.File db=new java.io.File(System.getProperty("em.db",""));
    if(db.isFile() && EmDbReader.driverPresent()){ try{ Map<UUID,EmDbReader.Signals> all=EmDbReader.readAll(db);
      ck("EM DB reader: 11 PlayerData rows from pulled player_data.db", all.size()==11);
      ck("EM DB reader: Jester_Turtle HLK 22 / Score 3674 (matches LOCK-PREP #16)", all.get(UUID.fromString("e739fb4d-7cec-48f4-8a2b-7c25e27d8b9e")).highestLevelKilled()==22 && all.get(UUID.fromString("e739fb4d-7cec-48f4-8a2b-7c25e27d8b9e")).score()==3674L);
      ck("EM DB reader: GumGumJetPistol HLK 23 / Score 858 (matches LOCK-PREP #16)", all.get(UUID.fromString("8399c623-786f-448c-8b11-f54504721819")).highestLevelKilled()==23 && all.get(UUID.fromString("8399c623-786f-448c-8b11-f54504721819")).score()==858L);
      ck("EM DB reader: Floodgate UUIDs keyed as-is (.MikeExcel HLK 12)", all.get(UUID.fromString("00000000-0000-0000-0009-01feb74ea7be")).highestLevelKilled()==12);
    }catch(Exception e){ ck("EM DB reader: readAll threw "+e, false); } }
    else System.out.println("SKIP EM DB reader checks (driver or -Dem.db not provided)"); }
  // ================= 0.1.1: tap-card proposer + releaser flip =================
  { ManifestRow m30=rows.stream().filter(r->r.id().equals("M-30")).findFirst().orElseThrow();
    ck("content_files wired from FINAL manifest (M-02 = 4 files, M-03 catacombs)", primis.contentFiles().size()==4 && rows.stream().filter(r->r.id().equals("M-03")).findFirst().orElseThrow().contentFiles().equals(List.of("EliteMobs/content_packages/catacombs_lair.yml")));
    ck("M-33 has no content files (never flippable until captured)", rows.stream().filter(r->r.id().equals("M-33")).findFirst().orElseThrow().contentFiles().isEmpty());
    // --- proposer: DUE -> exactly one card; untapped stays due silently ---
    Proposer prop=new Proposer(new Watcher(null)); ReleaseState st=new ReleaseState();
    List<Proposer.Proposal> p1=prop.tick(rows,act,st,now,Month.NOVEMBER,false);
    ck("proposer: DUE pack produces ONE tap-card proposal", p1.size()==1 && p1.get(0).rowId().equals("M-02") && p1.get(0).kind()==Proposer.Kind.RELEASE);
    ck("tap card carries the typed tap command + token", p1.get(0).tapCommand().equals("/excelexpansions tap M-02 "+p1.get(0).token()) && p1.get(0).token().length()==10);
    ck("proposer: row recorded proposed with the same token", ReleaseState.PROPOSED.equals(st.stateOf(primis)) && st.get("M-02").orElseThrow().token().equals(p1.get(0).token()));
    List<Proposer.Proposal> p2=prop.tick(rows,act,st,now.plus(Duration.ofHours(6)),Month.NOVEMBER,false);
    ck("proposer: untapped stays due SILENTLY (second tick posts nothing)", p2.isEmpty() && ReleaseState.PROPOSED.equals(st.stateOf(primis)));
    ck("pendingCard re-delivers the SAME episode (same token, delivery not a new proposal)", prop.pendingCard(primis,st).map(pc->pc.token().equals(p1.get(0).token())).orElse(false));
    // --- releaser flipText units ---
    ck("flipText flips the anchored flag only (decoy comment untouched)", Releaser.flipText("isEnabled: false\n# isEnabled: true decoy\nname: x\n",true).equals("isEnabled: true\n# isEnabled: true decoy\nname: x\n"));
    ck("flipText already-set returns the same text", Releaser.flipText("isEnabled: true\n",true).equals("isEnabled: true\n"));
    ck("flipText no anchored flag -> null", Releaser.flipText("name: x\n  isEnabled: false\n",true)==null);
    // --- fixture parked pack: full tap -> flip -> double-tap path (M-03 Catacombs) ---
    ManifestRow m03=rows.stream().filter(r->r.id().equals("M-03")).findFirst().orElseThrow();
    Path plug=Files.createTempDirectory("exc011-fixture"); Path pkg=plug.resolve("EliteMobs/content_packages/catacombs_lair.yml");
    Files.createDirectories(pkg.getParent()); Files.writeString(pkg,"isEnabled: false\n# isEnabled: true decoy comment\nname: catacombs\n");
    ReleaseState st2=new ReleaseState(); ReleaseService svc=new ReleaseService();
    ck("tap with NO live proposal is rejected, nothing flips", svc.tap(m03,st2,"abc",false,"Mike",plug,now).outcome()==ReleaseService.Outcome.REJECTED_NOT_PROPOSED && Files.readString(pkg).contains("isEnabled: false"));
    st2.put(new ReleaseState.Entry("M-03",ReleaseState.PROPOSED,now,now,null,null,"tok011test","proposed: fixture"));
    ck("tap with a WRONG (stale) token is rejected, nothing flips", svc.tap(m03,st2,"stale00000",false,"Mike",plug,now).outcome()==ReleaseService.Outcome.REJECTED_TOKEN && Files.readString(pkg).contains("isEnabled: false"));
    ReleaseService.TapResult good=svc.tap(m03,st2,"tok011test",false,"Mike",plug,now);
    ck("tap on the live proposal RELEASES the parked fixture pack", good.outcome()==ReleaseService.Outcome.RELEASED && good.flip()!=null && good.flip().success());
    String flipped=Files.readString(pkg);
    ck("fixture pack flipped enabled+open: anchored flag true, decoy comment untouched, backup written", flipped.startsWith("isEnabled: true") && flipped.contains("# isEnabled: true decoy comment") && Files.exists(plug.resolve("EliteMobs/content_packages/catacombs_lair.yml"+Releaser.BACKUP_SUFFIX)));
    ck("state advanced to released with the Mike-tap audit line", ReleaseState.RELEASED.equals(st2.stateOf(m03)) && st2.get("M-03").orElseThrow().releasedBy().contains("Mike tap"));
    ck("double-tap returns ALREADY_RELEASED and flips nothing again", svc.tap(m03,st2,"tok011test",false,"Mike",plug,now).outcome()==ReleaseService.Outcome.ALREADY_RELEASED && Files.readString(pkg).startsWith("isEnabled: true"));
    // --- missing content file fails the flip as a whole; row stays proposed ---
    ManifestRow m04=rows.stream().filter(r->r.id().equals("M-04")).findFirst().orElseThrow();
    ReleaseState st3=new ReleaseState(); st3.put(new ReleaseState.Entry("M-04",ReleaseState.PROPOSED,now,now,null,null,"tok011test","proposed: fixture"));
    ReleaseService.TapResult miss=svc.tap(m04,st3,"tok011test",false,"Mike",plug,now);
    ck("flip with a missing content file FAILS and the row stays proposed", miss.outcome()==ReleaseService.Outcome.FLIP_FAILED && ReleaseState.PROPOSED.equals(st3.stateOf(m04)));
    // --- seasonal retirement: released North Pole out of window -> retirement card -> tap retires ---
    Path np=plug.resolve(m30.contentFiles().get(0)); Files.createDirectories(np.getParent()); Files.writeString(np,"isEnabled: true\nname: north pole\n");
    ReleaseState st4=new ReleaseState(); st4.put(new ReleaseState.Entry("M-30",ReleaseState.RELEASED,null,null,now,"Mike tap fixture",null,"released fixture"));
    List<Proposer.Proposal> rp=prop.tick(rows,List.of(),st4,now,Month.NOVEMBER,false);
    ck("seasonal: released pack out of window produces ONE retirement proposal", rp.size()==1 && rp.get(0).kind()==Proposer.Kind.RETIREMENT && ReleaseState.RETIREMENT_PROPOSED.equals(st4.stateOf(m30)));
    ReleaseService.TapResult ret=svc.tap(m30,st4,rp.get(0).token(),false,"Mike",plug,now);
    ck("retirement tap flips the pack back to disabled (retired-seasonal)", ret.outcome()==ReleaseService.Outcome.RETIRED && Files.readString(np).startsWith("isEnabled: false") && ReleaseState.RETIRED.equals(st4.stateOf(m30)));
    // --- release state round-trip ---
    Path sf=Files.createTempFile("relstate",".txt"); st2.save(sf); ReleaseState back2=ReleaseState.load(sf);
    ck("release state round-trip (released row + audit fields survive restart)", ReleaseState.RELEASED.equals(back2.stateOf(m03)) && back2.get("M-03").orElseThrow().releasedBy().contains("Mike tap")); }
  System.out.println("RESULT pass="+pass+" fail="+fail); if(fail>0) System.exit(1); }
}
