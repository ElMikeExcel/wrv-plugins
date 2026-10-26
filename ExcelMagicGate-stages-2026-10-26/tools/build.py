#!/usr/bin/env python3
"""Build the Seal-4 redistribution stage packet (stages 2-5) with Disposal removed.
Approved by Mike Oct 26 2026 2:13-2:17 PM CDT. Staging only."""
import re, os, shutil, yaml, hashlib, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__))); import levelfix
LEVELFIX={}
H='/workspace/batch-next/spells-by-seal4-20261026/staged/seal4-full'
RIG='/workspace/wb027-rig/plugins/ExcelMagicGate/stages'
LIVE=H+'/live-20261026-1457'  # live stage2-5 files downloaded by Mike 2:57 PM CDT Oct 26 (source of truth; wands carry the mana boss_bar B1)
MW='/workspace/batch-next/masterwand-20261026/new/plugins/ExcelMagicGate/stages'  # live messages after Master Wand rename
NOTE='Oct 26 2026 (Mike, 2:13-2:17 PM CDT)'
P2='eat_food rolldice skating wearskates admission tamepet anvil healing music'.split()
P3='reflect boomerang throwsword whip holylight'.split()
MASONS='platform walls sponge tent tiny_house giant_house harden rubberize'.split()
P4=MASONS+'cloak earth critical counterspell arrowrain sweep powerstone force fury invincible tendril shrink masterattack day night weather time localrain restorelocalweather layonhands'.split()
MOVE={2:P2,3:P3,4:P4}
REMOVED={'disposal'}
# cooldown tuning (ms). Magic 11.2.5 cooldowns are per caster (BaseSpell 'cooldown'); no server-wide scope exists.
TUNE={'day':{'cooldown':(1800000,1800000)},'night':{'cooldown':(1800000,1800000)},
      'weather':{'cooldown':(900000,1200000)},'time':{'cooldown':(10000,1200000)},
      'invincible':{'cooldown':(180000,600000),'duration':(5000,5000)}}
def seal_of(k, cur):
    for s,l in MOVE.items():
        if k in l: return s
    return cur
def load(p): return open(p).read()
def sha(p): return hashlib.sha256(open(p,'rb').read()).hexdigest()
def blocks(text):
    """top-level key -> (start,end) char span, incl. preceding nothing"""
    out={}; ms=list(re.finditer(r'(?m)^([A-Za-z0-9_\'"|]+):', text))
    for i,m in enumerate(ms):
        e=ms[i+1].start() if i+1<len(ms) else len(text)
        out[m.group(1).strip('\'"')]=(m.start(),e)
    return out
def replace_list(text, top, sub, items):
    b=blocks(text); s,e=b[top]; seg=text[s:e]
    m=re.search(r'(?m)^(  '+sub+r':\n)((?:  - .*\n)+)', seg)
    assert m,(top,sub)
    new=m.group(1)+''.join(f'  - {x}\n' for x in items)
    seg=seg[:m.start()]+new+seg[m.end():]
    return text[:s]+seg+text[e:]
def get_list(text, top, sub):
    b=blocks(text); s,e=b[top]; seg=text[s:e]
    m=re.search(r'(?m)^  '+sub+r':\n((?:  - .*\n)+)', seg)
    return [l[4:] for l in m.group(1).splitlines()] if m else None
W5=load(f'{LIVE}/stage5/wands.yml'); PA5=load(f'{RIG}/stage5/paths.yml'); SP5=load(f'{RIG}/stage5/spells.yml')
Y5=yaml.safe_load(W5)
# current seal of each master-wand spell
cur={}
for s in range(1,6):
    for k in yaml.safe_load(load(f'{RIG}/stage{s}/wands.yml'))['master']['spells']:
        cur.setdefault(k,s)
newseal={k:seal_of(k,cur[k]) for k in cur}
def allowed(s): return [k for k in cur if newseal[k]<=s and k not in REMOVED]
def tune(text):
    b=blocks(text)
    for k,params in TUNE.items():
        if k not in b: continue
        s,e=b[k]; seg=text[s:e]
        for p,(old,new) in params.items():
            pat=f'\n    {p}: {old}\n'
            assert seg.count(pat)==1,(k,p,old)
            if old!=new:
                seg=seg.replace(pat,f'\n    {p}: {new}  # WRV {NOTE}: long cooldown (was {old})\n')
        text=text[:s]+seg+text[e:]
    return text
def disable_disposal(text):
    b=blocks(text)
    if 'disposal' not in b: return text
    s,e=b['disposal']
    stub=(f'# WRV {NOTE}: Disposal spell removed (destroys items, no undo).\n'
          '# enabled: false makes Magic 11.2.5 skip this key at load (ConfigurationUtils.isEnabled).\n'
          'disposal:\n  enabled: false\n\n')
    return text[:s]+stub+text[e:]
counts={}
for s in (2,3,4,5):
    os.makedirs(f'{H}/base/stage{s}',exist_ok=True); os.makedirs(f'{H}/new/stage{s}',exist_ok=True)
    for f in ('wands.yml','paths.yml','spells.yml','grants.txt','config.yml','classes.yml'):
        src=f'{LIVE}/stage{s}/{f}' if os.path.exists(f'{LIVE}/stage{s}/{f}') else f'{RIG}/stage{s}/{f}'
        shutil.copy(src,f'{H}/base/stage{s}/{f}')
    shutil.copy(f'{LIVE}/stage{s}/messages.yml',f'{H}/base/stage{s}/messages.yml')
    for f in ('paths.yml','spells.yml','grants.txt','messages.yml')+(('config.yml',) if s==4 else ()):
        assert sha(f'{LIVE}/stage{s}/{f}')==sha((f'{MW}/stage{s}/' if f=='messages.yml' else f'{RIG}/stage{s}/')+f),(s,f)  # rig source = live
    A=set(allowed(s))
    # wands: rank lists = stage5 order filtered
    W=load(f'{LIVE}/stage{s}/wands.yml')  # live copy incl. mana boss_bar
    for r in ('beginner','student','apprentice','master'):
        W=replace_list(W,r,'spells',[k for k in Y5[r]['spells'] if k in A])
    if s==4:
        pass  # masons wand template already present (hidden, harmless)
    W=W.replace('\n','\n# WRV '+NOTE+': spells re-spread so everything opens by Seal 4; Disposal removed.\n',1) if not W.startswith('#') else W.split('\n',1)[0]+'\n# WRV '+NOTE+': spells re-spread so everything opens by Seal 4; Disposal removed.\n'+W.split('\n',1)[1]
    open(f'{H}/new/stage{s}/wands.yml','w').write(W)
    # paths
    PA=load(f'{RIG}/stage{s}/paths.yml'); Y5p=yaml.safe_load(PA5)
    for r in ('beginner','student','apprentice','master'):
        PA=replace_list(PA,r,'spells',[k for k in Y5p[r]['spells'] if k in A])
    PA=PA.split('\n',1)[0]+'\n# WRV '+NOTE+': path lists re-spread (all spells open by Seal 4); Disposal removed.\n'+PA.split('\n',1)[1]
    open(f'{H}/new/stage{s}/paths.yml','w').write(PA)
    # spells: keep stage s text, append moved blocks from stage5
    SP=load(f'{RIG}/stage{s}/spells.yml'); b5=blocks(SP5); bs=blocks(SP)
    need=[k for k in b5 if k.split('|')[0] in A and k not in bs]
    if s==5: assert not need
    if need:
        SP=SP.rstrip('\n')+f'\n\n# ===== WRV {NOTE}: spells moved here from Seal 5 (copied verbatim from stage5/spells.yml) =====\n'
        for k in need:
            st,en=b5[k]; SP+=SP5[st:en].rstrip('\n')+'\n\n'
    SP=disable_disposal(tune(SP))
    SP,_lf=levelfix.apply(SP); LEVELFIX[s]=_lf  # Mike 3:05 PM CDT: all level fixes (levelfix.py)
    open(f'{H}/new/stage{s}/spells.yml','w').write(SP)
    # grants
    G=load(f'{RIG}/stage{s}/grants.txt')
    G=G.replace('lp group default permission set magic.cast.disposal true\n',f'# (removed {NOTE}: magic.cast.disposal, Disposal spell taken off all wands)\n')
    if s==4:
        G=re.sub(r'(?m)^lp group builder permission set magic\.cast\.(\w+) true\n', r'# (moved to default group below: \1)\n', G)
        G=G.replace('# Create the group first if it does not exist: lp creategroup builder\n',
                    f'# WRV {NOTE}: Mike decided the Masons spells go to EVERYONE at Seal 4.\n# No builder group is created any more (ExcelMagicGate only runs lp creategroup builder\n# when a line starts with "lp group builder"; none remain).\n')
    have=set(re.findall(r'(?m)^lp group default permission set magic\.cast\.(\w+) true$',G))
    add=[k for k in allowed(s) if k not in have]
    if add:
        G=G.rstrip('\n')+f'\n\n# WRV {NOTE}: default-group cast nodes for spells that now open at this seal\n'+''.join(f'lp group default permission set magic.cast.{k} true\n' for k in add)
    if s==5:
        G=G.replace('# Note: if the Seal 4 builder group was created, its 8 masons grants\n',f'# Note ({NOTE}): the builder group is no longer created at Seal 4 (Masons go to everyone).\n# Old note: if the Seal 4 builder group was created, its 8 masons grants\n')
    open(f'{H}/new/stage{s}/grants.txt','w').write(G)
    # messages: stage4 gets Lay on Hands strings = live stage5 messages
    M=load(f'{MW}/stage{5 if s==4 else s}/messages.yml')
    # WRV Oct 26 2026 2:46 PM CDT: friendly names for wand spells that had none (players saw raw keys).
    # Only spells on this stage's wands. Every line <=38 chars for Bedrock forms, no em dashes.
    NAMES=[('throwmagictorch','Throw Magic Torch','Toss a magic light at a far spot'),
           ('hulkleap','Hulk Leap','Leap high and far, with a safe landing'),
           ('tamepet','Tame Pet','Tame animals and care for your pets'),
           ('localrain','Local Rain','Rain that only you can see'),
           ('restorelocalweather','Restore Local Weather','Bring back the real weather for you')]
    onw=set(get_list(load(f'{H}/new/stage{s}/wands.yml'),'master','spells'))
    add=''.join(f'  {k}:\n    name: {n}\n    description: {d}\n' for k,n,d in NAMES if k in onw)
    for k,n,d in NAMES: assert len(n)<=38 and len(d)<=38 and '\u2014' not in n+d
    assert M.count('\nspells:\n')==1
    M=M.replace('\nspells:\n','\nspells:\n  # WRV Oct 26 2026: names for spells that showed raw keys\n'+add,1)
    open(f'{H}/new/stage{s}/messages.yml','w').write(M)
    # config: stage4 gets stage5's automata comment block (comment only)
    C=load(f'{RIG}/stage{s}/config.yml')
    if s==4:
        C5=load(f'{RIG}/stage5/config.yml'); C=C5.replace(C5.split('\n',1)[0], C.split('\n',1)[0],1)
        assert yaml.safe_load(C)==yaml.safe_load(load(f'{RIG}/stage4/config.yml'))
    open(f'{H}/new/stage{s}/config.yml','w').write(C)
    shutil.copy(f'{RIG}/stage{s}/classes.yml',f'{H}/new/stage{s}/classes.yml')
    # verify
    yw=yaml.safe_load(open(f'{H}/new/stage{s}/wands.yml')); yp=yaml.safe_load(open(f'{H}/new/stage{s}/paths.yml')); ys=yaml.safe_load(open(f'{H}/new/stage{s}/spells.yml'))
    for r in ('beginner','student','apprentice','master'):
        for k in yw[r]['spells']: assert k in ys and ys[k].get('enabled',True),(s,r,k)
    pathset=set(sum([yp[r]['spells'] for r in ('beginner','student','apprentice','master')],[]))|set(yp['beginner'].get('extra_spells',[]))
    assert pathset==set(yw['master']['spells']),(s,pathset^set(yw['master']['spells']))
    assert 'disposal' not in pathset
    counts[s]={r:len(yw[r]['spells']) for r in ('beginner','student','apprentice','master')}
counts[1]={r:len(yaml.safe_load(load(f'{RIG}/stage1/wands.yml'))[r]['spells']) for r in ('beginner','student','apprentice','master')}
for s in sorted(counts): print('seal',s,counts[s])
for s in sorted(LEVELFIX): print('levelfix stage',s,LEVELFIX[s])
