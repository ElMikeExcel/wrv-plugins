import yaml,csv,collections,hashlib
B='/workspace/wb027-rig/plugins/ExcelMagicGate/stages'
ranks=['beginner','student','apprentice','master']
W={s:yaml.safe_load(open(f'{B}/stage{s}/wands.yml')) for s in range(6)}
msg=yaml.safe_load(open('/workspace/ship1025-rig/plugins/Magic/defaults/messages.defaults.yml'))['spells']
ov=yaml.safe_load(open(f'{B}/stage5/messages.yml'))['spells']
def name(k):
    d=ov.get(k) or msg.get(k) or {}
    n=d.get('name')
    return n or {'throwmagictorch':'Throw Magic Torch','hulkleap':'Hulk Leap','skating':'Skating','wearskates':'Wear Skates','tamepet':'Tame Pet','localrain':'Local Rain','restorelocalweather':'Restore Local Weather','tiny_house':'Tiny House','giant_house':'Giant House'}.get(k,k)
master5=W[5]['master']['spells']
def band(k):
    for r in ranks:
        if k in (W[5][r].get('spells') or []): return r
def cur_seal(k):
    for s in range(1,6):
        if k in (W[s]['master'].get('spells') or []): return s
masons=['platform','walls','sponge','tent','tiny_house','giant_house','harden','rubberize']
P2=['eat_food','rolldice','skating','wearskates','admission','anvil','tamepet','healing','music']
P3=['reflect','boomerang','throwsword','whip','holylight']
P4=masons+['cloak','earth','critical','counterspell','arrowrain','sweep','powerstone','fury','tendril','shrink','force','invincible','masterattack','day','night','weather','time','localrain','restorelocalweather','layonhands']
assert len(P2)+len(P3)+len(P4)==42 and len(set(P2+P3+P4))==42
REMOVED=['disposal']  # Mike, Oct 26 2026 2:13 PM CDT: off every wand (destroys items, no undo)
def new_seal(k):
    if k in REMOVED: return 99
    c=cur_seal(k)
    if c<5: return c
    return 2 if k in P2 else 3 if k in P3 else 4
CAT={}
for c,ks in {
 'Healing':'heal mend flashheal greaterheal superheal cure feed healing holylight layonhands',
 'Defense':'shell ward boon blessing bubble reflect cloak invincible',
 'Damage':'stormshackle missile frost arrow flare stun stream arrow_ice arrow_light arrow_shock arrow_regular wave smite lightning chainlightning boomerang throwsword whip critical counterspell arrowrain sweep powerstone fury tendril shrink masterattack push pull force',
 'Movement':'recall recall_death fling dash evade hulkleap backflip glide blink skating wearskates gather piggyback lift',
 'Summon':'familiar wolf cat tame tamepet',
 'Building':'platform walls sponge tent tiny_house giant_house harden rubberize earth',
 'World':'day night weather time localrain restorelocalweather',
 'Utility':'extinguish aqualung gills gasp torch magictorch throwmagictorch peek workbench repairitem repairequipment stash disposal anvil eat_food rolldice music hat growth pollinate grow sparkle admission',
}.items():
    for k in ks.split():
        assert k not in CAT,k; CAT[k]=c
missing=[k for k in master5 if k not in CAT]; extra=[k for k in CAT if k not in master5]
assert not missing and not extra,(missing,extra)
rows=[]
for k in master5:
    rows.append(dict(spell_key=k,name=name(k),rank_band=band(k),current_seal=cur_seal(k),proposed_seal=('removed' if k in REMOVED else new_seal(k)),moved=('removed' if k in REMOVED else 'yes' if cur_seal(k)!=new_seal(k) else ''),category=CAT[k],builder_gated_now='yes (Seal 4 builder group)' if k in masons else ''))
with open('spells-by-seal.csv','w',newline='') as f:
    w=csv.DictWriter(f,fieldnames=list(rows[0])); w.writeheader(); w.writerows(rows)
# per stage table
out=[]
def counts(fn):
    res={}
    for s in range(6):
        res[s]={r:sum(1 for x in rows if x['rank_band'] in ranks[:ranks.index(r)+1] and fn(x)<=s) for r in ranks}
    return res
cur=counts(lambda x:x['current_seal']); new=counts(lambda x:new_seal(x['spell_key']))
# sanity vs files
for s in range(6):
    for r in ranks:
        assert cur[s][r]==len(W[s][r].get('spells') or []) if W[s].get(r) else cur[s][r]==0,(s,r,cur[s][r])
with open('per-stage-counts.csv','w',newline='') as f:
    w=csv.writer(f); w.writerow(['plan','stage','new_on_master_wand','cumulative_beginner','cumulative_student','cumulative_apprentice','cumulative_master','new_spell_keys'])
    for lab,c,fld in [('current',cur,'current_seal'),('proposed',new,'proposed_seal')]:
        for s in range(6):
            nk=[x['spell_key'] for x in rows if (x[fld] if fld=='current_seal' else new_seal(x['spell_key']))==s]
            w.writerow([lab,s,len(nk),c[s]['beginner'],c[s]['student'],c[s]['apprentice'],c[s]['master'],' '.join(nk)])
cc=collections.Counter(CAT[k] for k in master5 if k not in REMOVED)
print(cc)
for lab,c in [('cur',cur),('new',new)]:
    print(lab,[ (s,c[s]) for s in range(6)])
for s in (1,2,3,4):
    print('newcat',s,collections.Counter(x['category'] for x in rows if new_seal(x['spell_key'])<=s))
# sanity vs the staged seal4-full packet (new/stageN/wands.yml)
SN='staged/seal4-full/new'
for st in (2,3,4,5):
    Wn=yaml.safe_load(open(f'{SN}/stage{st}/wands.yml'))
    for r in ranks:
        assert new[st][r]==len(Wn[r]['spells']),(st,r,new[st][r],len(Wn[r]['spells']))
print('staged packet counts match the plan')
