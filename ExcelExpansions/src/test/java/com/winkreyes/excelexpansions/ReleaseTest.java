package com.winkreyes.excelexpansions;
import com.winkreyes.excelexpansions.core.*;
import java.io.InputStream; import java.nio.charset.StandardCharsets; import java.nio.file.*;
import java.time.*; import java.util.*;
/** Desk harness for the 0.1.1 proposer / releaser / tap service / release
 *  state (DESIGN 3, 4.2, 5, 7.12). Same PASS/FAIL + RESULT contract as
 *  WatcherTest; exits non-zero on any failure. */
public class ReleaseTest {
 static int pass=0, fail=0;
 static void check(String name, boolean cond){ if(cond){ pass++; System.out.println("PASS "+name); } else { fail++; System.out.println("FAIL "+name); } }
 static ManifestRow row(String id,String slug,String unit,String phase,int order,Integer bmin,Integer bmax,String kind,String src,boolean seasonal,String state,List<String> files){
  return new ManifestRow(id,slug,id+" display",unit,phase,order,bmin,bmax,kind,src,seasonal,null,null,null,List.of(),state,"parked","","band text "+id,files); }
 static Watcher.Player pl(int level){ return new Watcher.Player(UUID.randomUUID(), Instant.now(), OptionalInt.of(level)); }
 static Path writePkg(Path plugins, String rel, String text) throws Exception {
  Path f=plugins.resolve(rel); Files.createDirectories(f.getParent()); Files.writeString(f,text,StandardCharsets.UTF_8); return f; }
 public static void main(String[] args) throws Exception {
  Watcher w=new Watcher(null); Proposer prop=new Proposer(w); ReleaseService svc=new ReleaseService();
  Instant t0=Instant.now();
  ManifestRow A=row("M-T1","test-alpha","headline","1",1,10,20,"fixed","pack-read: test",false,"unreleased",List.of("EliteMobs/content_packages/test_alpha.yml"));
  List<Watcher.Player> due3=List.of(pl(15),pl(12),pl(3)); // 2 of 3 in band (floor 10)
  // --- Proposer: DUE -> one proposal; second tick no re-spam ---
  ReleaseState st=new ReleaseState();
  List<Proposer.Proposal> p1=prop.tick(List.of(A),due3,st,t0,Month.JUNE,false);
  check("proposer: DUE row produces exactly one RELEASE proposal", p1.size()==1 && p1.get(0).kind()==Proposer.Kind.RELEASE && p1.get(0).rowId().equals("M-T1"));
  check("proposer: band label + tap command carry the episode token", p1.get(0).bandLabel().equals("10-20") && p1.get(0).tapCommand().equals("/excelexpansions tap M-T1 "+p1.get(0).token()));
  check("proposer: state advanced to proposed with token stored", ReleaseState.PROPOSED.equals(st.stateOf(A)) && p1.get(0).token().equals(st.get("M-T1").orElseThrow().token()));
  List<Proposer.Proposal> p2=prop.tick(List.of(A),due3,st,t0.plusSeconds(3600),Month.JUNE,false);
  check("proposer: untapped stays silent - no re-spam, proposed_at unchanged", p2.isEmpty() && st.get("M-T1").orElseThrow().proposedAt().equals(t0));
  // --- Proposer: majority lapse after proposal - card stands, note only ---
  prop.tick(List.of(A),List.of(pl(15),pl(3),pl(2)),st,t0.plusSeconds(7200),Month.JUNE,false);
  check("proposer: lapsed majority keeps the proposal, lapse noted (DESIGN 2.3)", ReleaseState.PROPOSED.equals(st.stateOf(A)) && st.get("M-T1").orElseThrow().note().contains("majority lapsed"));
  // --- Proposer: floor of 3 ---
  ReleaseState stF=new ReleaseState();
  List<Proposer.Proposal> pF=prop.tick(List.of(A),List.of(pl(15),pl(12)),stF,t0,Month.JUNE,false);
  check("proposer: fewer than 3 active - no proposal, row untouched", pF.isEmpty() && stF.get("M-T1").isEmpty());
  // --- Proposer: one headline in flight; recorded DUE proposes after the tap ---
  ManifestRow B=row("M-T0","test-beta","headline","1",0,10,20,"fixed","pack-read: test",false,"unreleased",List.of("EliteMobs/content_packages/test_beta.yml"));
  ReleaseState stH=new ReleaseState();
  stH.put(new ReleaseState.Entry("M-T1",ReleaseState.PROPOSED,t0,t0,null,null,"tokA","proposed: test setup"));
  List<Proposer.Proposal> pH=prop.tick(List.of(B,A),due3,stH,t0.plusSeconds(60),Month.JUNE,false);
  check("proposer: second DUE row recorded due behind the in-flight headline, no card", pH.isEmpty() && ReleaseState.DUE.equals(stH.stateOf(B)));
  stH.put(new ReleaseState.Entry("M-T1",ReleaseState.RELEASED,t0,t0,t0,"Mike tap test",null,"released: test setup"));
  List<Proposer.Proposal> pH2=prop.tick(List.of(B,A),due3,stH,t0.plusSeconds(120),Month.JUNE,false);
  check("proposer: after the headline tap lands, the recorded-due row proposes", pH2.size()==1 && pH2.get(0).rowId().equals("M-T0") && ReleaseState.PROPOSED.equals(stH.stateOf(B)));
  // --- Proposer: released stays released ---
  List<Proposer.Proposal> pR=prop.tick(List.of(A),due3,stH,t0.plusSeconds(180),Month.JUNE,false);
  check("proposer: released rows are never re-proposed", pR.stream().noneMatch(p->p.rowId().equals("M-T1")));
  check("proposer: pendingCard rebuilds the live episode only", prop.pendingCard(B,stH).isPresent() && prop.pendingCard(B,stH).orElseThrow().token().equals(stH.get("M-T0").orElseThrow().token()) && prop.pendingCard(A,stH).isEmpty());
  // --- Releaser.flipText ---
  check("releaser: anchored flag flips, other lines untouched", Releaser.flipText("isEnabled: false\nfoo: bar\n",true).equals("isEnabled: true\nfoo: bar\n"));
  check("releaser: comment lookalikes never touched", Releaser.flipText("# header\nisEnabled: false\n# isEnabled: true\n",true).equals("# header\nisEnabled: true\n# isEnabled: true\n"));
  check("releaser: comment-only file has no flag", Releaser.flipText("# isEnabled: false\nfoo: bar\n",true)==null);
  check("releaser: already at target returns the same text", Releaser.flipText("isEnabled: true\n",true).equals("isEnabled: true\n"));
  check("releaser: non-boolean flag value rejected", Releaser.flipText("isEnabled: maybe\n",true)==null);
  check("releaser: CRLF line endings preserved", Releaser.flipText("isEnabled: false\r\nfoo: bar\r\n",true).equals("isEnabled: true\r\nfoo: bar\r\n"));
  // --- Releaser.flip on disk ---
  Path dir=Files.createTempDirectory("ee011-rel");
  Path fa=writePkg(dir,"EliteMobs/content_packages/test_alpha.yml","isEnabled: false\nx: 1\n");
  Releaser.Result f1=new Releaser().flip(dir,List.of("EliteMobs/content_packages/test_alpha.yml"),true);
  check("releaser: flip succeeds, file enabled, backup holds the original",
    f1.success() && f1.files().get(0).status()==Releaser.Status.FLIPPED
    && Files.readString(fa).startsWith("isEnabled: true")
    && Files.readString(dir.resolve("EliteMobs/content_packages/test_alpha.yml"+Releaser.BACKUP_SUFFIX)).equals("isEnabled: false\nx: 1\n"));
  Releaser.Result f2=new Releaser().flip(dir,List.of("EliteMobs/content_packages/test_alpha.yml"),true);
  check("releaser: re-flip converges as ALREADY_SET (idempotent)", f2.success() && f2.files().get(0).status()==Releaser.Status.ALREADY_SET);
  Releaser.Result f3=new Releaser().flip(dir,List.of("EliteMobs/content_packages/nope.yml"),true);
  check("releaser: missing file fails the flip as a whole", !f3.success() && f3.files().get(0).status()==Releaser.Status.MISSING);
  // --- Seasonal full cycle: propose (no roster floor) -> tap -> retire proposal -> tap -> next window proposes again ---
  Path sdir=Files.createTempDirectory("ee011-season");
  Path npf=writePkg(sdir,"EliteMobs/content_packages/north_pole.yml","isEnabled: false\nname: np\n");
  ManifestRow NP=row("M-T3","north-pole","seasonal","seasonal",1,null,null,null,"pack-read: test",true,"unreleased",List.of("EliteMobs/content_packages/north_pole.yml"));
  ReleaseState stS=new ReleaseState();
  List<Proposer.Proposal> s1=prop.tick(List.of(NP),List.of(),stS,t0,Month.DECEMBER,false);
  check("seasonal: window-open proposal fires with an EMPTY roster (no floor, no band test)", s1.size()==1 && s1.get(0).kind()==Proposer.Kind.RELEASE);
  String tokS=stS.get("M-T3").orElseThrow().token();
  ReleaseService.TapResult tS=svc.tap(NP,stS,tokS,false,"MikeExcel",sdir,Instant.now());
  check("seasonal: tap releases and flips the pack on", tS.outcome()==ReleaseService.Outcome.RELEASED && Files.readString(npf).startsWith("isEnabled: true") && ReleaseState.RELEASED.equals(stS.stateOf(NP)));
  List<Proposer.Proposal> s2=prop.tick(List.of(NP),List.of(),stS,t0.plusSeconds(3600),Month.JANUARY,false);
  check("seasonal: window close proposes retirement exactly once", s2.size()==1 && s2.get(0).kind()==Proposer.Kind.RETIREMENT && ReleaseState.RETIREMENT_PROPOSED.equals(stS.stateOf(NP))
    && prop.tick(List.of(NP),List.of(),stS,t0.plusSeconds(7200),Month.JANUARY,false).isEmpty());
  String tokR=stS.get("M-T3").orElseThrow().token();
  ReleaseService.TapResult tR=svc.tap(NP,stS,tokR,false,"MikeExcel",sdir,Instant.now());
  check("seasonal: retirement tap flips the pack off and retires it", tR.outcome()==ReleaseService.Outcome.RETIRED && Files.readString(npf).startsWith("isEnabled: false") && ReleaseState.RETIRED.equals(stS.stateOf(NP)));
  List<Proposer.Proposal> s3=prop.tick(List.of(NP),List.of(),stS,t0.plusSeconds(10800),Month.DECEMBER,false);
  check("seasonal: retired pack proposes again at its next window (DESIGN 4.2)", s3.size()==1 && s3.get(0).kind()==Proposer.Kind.RELEASE && ReleaseState.PROPOSED.equals(stS.stateOf(NP)));
  // --- ReleaseService token / state guards ---
  Path gdir=Files.createTempDirectory("ee011-guard");
  Path gf=writePkg(gdir,"EliteMobs/content_packages/test_alpha.yml","isEnabled: false\n");
  ReleaseState stG=new ReleaseState();
  stG.put(new ReleaseState.Entry("M-T1",ReleaseState.PROPOSED,t0,t0,null,null,"tok123","proposed: test"));
  ReleaseService.TapResult g1=svc.tap(A,stG,"wrong-token",false,"MikeExcel",gdir,Instant.now());
  check("tap: wrong token rejected, nothing flips, row stays proposed", g1.outcome()==ReleaseService.Outcome.REJECTED_TOKEN && Files.readString(gf).startsWith("isEnabled: false") && ReleaseState.PROPOSED.equals(stG.stateOf(A)));
  ReleaseService.TapResult g2=svc.tap(A,stG,null,false,"MikeExcel",gdir,Instant.now());
  check("tap: in-game tap without a token rejected", g2.outcome()==ReleaseService.Outcome.REJECTED_TOKEN && ReleaseState.PROPOSED.equals(stG.stateOf(A)));
  ReleaseService.TapResult g3=svc.tap(A,stG,null,true,"console",gdir,Instant.now());
  check("tap: console may tap without a token (trusted surface)", g3.outcome()==ReleaseService.Outcome.RELEASED && Files.readString(gf).startsWith("isEnabled: true"));
  ReleaseService.TapResult g4=svc.tap(A,stG,"tok123",false,"MikeExcel",gdir,Instant.now());
  check("tap: double-tap returns ALREADY_RELEASED and flips nothing", g4.outcome()==ReleaseService.Outcome.ALREADY_RELEASED && g4.flip()==null);
  ReleaseService.TapResult g5=svc.tap(B,new ReleaseState(),null,true,"console",gdir,Instant.now());
  check("tap: no live proposal - rejected", g5.outcome()==ReleaseService.Outcome.REJECTED_NOT_PROPOSED);
  ManifestRow NC=row("M-T9","test-nocontent","headline","1",1,10,20,"fixed","pack-read: test",false,"unreleased",List.of());
  ReleaseState stN=new ReleaseState(); stN.put(new ReleaseState.Entry("M-T9",ReleaseState.PROPOSED,t0,t0,null,null,"tokN","proposed: test"));
  ReleaseService.TapResult g6=svc.tap(NC,stN,"tokN",false,"MikeExcel",gdir,Instant.now());
  check("tap: row with no content files - NO_CONTENT, stays proposed", g6.outcome()==ReleaseService.Outcome.NO_CONTENT && ReleaseState.PROPOSED.equals(stN.stateOf(NC)));
  ManifestRow MF=row("M-T8","test-mixed","headline","1",1,10,20,"fixed","pack-read: test",false,"unreleased",List.of("EliteMobs/content_packages/test_alpha.yml","EliteMobs/content_packages/missing.yml"));
  ReleaseState stM=new ReleaseState(); stM.put(new ReleaseState.Entry("M-T8",ReleaseState.PROPOSED,t0,t0,null,null,"tokM","proposed: test"));
  ReleaseService.TapResult g7=svc.tap(MF,stM,"tokM",false,"MikeExcel",gdir,Instant.now());
  check("tap: a missing file fails the flip - row stays proposed for retry", g7.outcome()==ReleaseService.Outcome.FLIP_FAILED && ReleaseState.PROPOSED.equals(stM.stateOf(MF)));
  // --- ReleaseState persistence ---
  ReleaseState stP=new ReleaseState();
  Instant t0m=Instant.ofEpochMilli(t0.toEpochMilli()); // the store persists epoch millis
  stP.put(new ReleaseState.Entry("M-02",ReleaseState.PROPOSED,t0m,t0m,null,null,"tokXYZ","note with a | pipe and\na newline"));
  Path sp=Files.createTempDirectory("ee011-state").resolve("release-state.txt");
  stP.save(sp); ReleaseState stL=ReleaseState.load(sp);
  ReleaseState.Entry le=stL.get("M-02").orElseThrow();
  check("state: save/load round-trip preserves the entry", le.state().equals(ReleaseState.PROPOSED) && le.token().equals("tokXYZ") && le.proposedAt().equals(t0m));
  check("state: notes are pipe/newline-sanitised on save", !le.note().contains("|") && !le.note().contains("\n"));
  check("state: stateOf falls back to the row's bundled initial state", new ReleaseState().stateOf(row("M-01","x","utility","0",1,null,null,"hub","none",false,"released",List.of())).equals("released"));
  // --- Real 0.1.1 manifest smoke (FINAL manifest data + content_files) ---
  List<ManifestRow> rows;
  try(InputStream in=ReleaseTest.class.getResourceAsStream("/releases.yml")){ rows=ManifestLoader.load(in); }
  check("manifest: 57 rows load from the built resources", rows.size()==57);
  Map<String,ManifestRow> byId=new LinkedHashMap<>(); for(ManifestRow r: rows) byId.put(r.id(),r);
  check("manifest: M-02 Primis carries its 4 content files", byId.get("M-02").contentFiles().size()==4);
  check("manifest: M-06 Story carries meta + 10 dungeon/sanctum files", byId.get("M-06").contentFiles().size()==11);
  check("manifest: M-25 Enchantment carries meta + 20 sanctum files", byId.get("M-25").contentFiles().size()==21);
  long withFiles=rows.stream().filter(r->!r.contentFiles().isEmpty()).count();
  check("manifest: 47 rows carry content files; the 10 empty rows can never be proposed (unassigned / support / the unassigned-by-verdict Harvester)", withFiles==47
    && rows.stream().filter(r->r.contentFiles().isEmpty()).allMatch(r->r.unassigned() || r.support() || r.slug().equals("craftenmines-harvester-event")));
  check("manifest: every content path is a content_packages path under a known host plugin",
    rows.stream().flatMap(r->r.contentFiles().stream()).allMatch(p->p.contains("/content_packages/") && (p.startsWith("EliteMobs/")||p.startsWith("BetterStructures/")||p.startsWith("FreeMinecraftModels/")||p.startsWith("CannonRTP/"))));
  System.out.println("RESULT pass="+pass+" fail="+fail);
  if(fail>0) System.exit(1);
 }
}
