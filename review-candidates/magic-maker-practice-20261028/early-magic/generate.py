"""OFFLINE review packet. No production I/O; no claim of runtime deployment readiness."""
import copy,hashlib,json,sys
from pathlib import Path
import yaml
from validate import validate
RANKS=('beginner','student','apprentice','master')
TERRAIN={'BreakBlock','Freeze','ModifyBreakable','Sphere'}
TERRAIN_PARAMS={'destructible','destructible_durability','break_durability','breakable','freeze_lava','ice'}
def merge(a,b):
 out=copy.deepcopy(a)
 for k,v in b.items():out[k]=merge(out[k],v) if isinstance(v,dict) and isinstance(out.get(k),dict) else copy.deepcopy(v)
 return out

def walk(node):
 if isinstance(node,dict):
  yield node
  for v in node.values():yield from walk(v)
 elif isinstance(node,list):
  for v in node:yield from walk(v)

def trim(nodes,keep=None):
 out=[]
 for n in nodes:
  if n.get('class') in TERRAIN:continue
  if n.get('class')=='Damage' and n is not keep:continue
  for k,v in list(n.items()):
   if isinstance(v,list) and all(isinstance(a,dict) for a in v):n[k]=trim(v,keep)
  out.append(n)
 return out

def definitions(config,reference):
 validate(config);out={}
 for key,profile in config['spells'].items():
  original={'stormshackle':'stormshackle','wrv_spark':'missile','wrv_frostbite':'frost'}[key]
  resolved={}
  for i,level in enumerate(profile['levels'],1):
   refkey=original if i==1 else original+'|'+str(i)
   if refkey not in reference:raise ValueError('missing reference '+refkey)
   resolved=merge(resolved,reference[refkey]);d=copy.deepcopy(resolved)
   # Expanded independent keys, never inherit another offensive spell family.
   for field in list(d):
    if field in ('upgrade_required_casts','inherit','headshot_parameters'):d.pop(field)
   d['name']=profile['display_name'];d['earns']=profile['earns'];d['earns_cooldown_scale']=1
   d['costs']['mana']=level['mana']
   pars=d['parameters']
   for field in TERRAIN_PARAMS:pars.pop(field,None)
   pars['target_breakables']=0
   pars.update(damage=level['damage_hp'],player_damage=level['damage_hp'],entity_damage=level['damage_hp'],cooldown=level['cooldown_ms'])
   projectiles=[n for n in d['actions']['cast'] if n.get('class')=='CustomProjectile']
   if len(projectiles)!=1:raise ValueError('unknown initial projectile shape '+refkey)
   first=projectiles[0]['actions'][0]
   if first.get('class')!='Damage':raise ValueError('first impact damage missing '+refkey)
   d['actions']['cast']=trim(d['actions']['cast'],first)
   first.update(damage=level['damage_hp'],player_damage=level['damage_hp'],entity_damage=level['damage_hp'])
   damages=[n for n in walk(d['actions']) if n.get('class')=='Damage']
   if len(damages)!=1:raise ValueError('damage graph invariant')
   out[key if i==1 else key+'|'+str(i)]=d
 return out

def compose(stage,files,defs):
 if type(stage)!=int or stage not in range(1,6):raise ValueError('stage')
 out=copy.deepcopy(files)
 new={'wrv_spark','wrv_frostbite'}
 if any(str(k).split('|')[0] in new for k in out['spells.yml']):raise ValueError('new family collision')
 allowed={'stormshackle'} | (new if stage>=2 else set())
 for key,d in defs.items():
  if key.split('|')[0] in allowed:out['spells.yml'][key]=copy.deepcopy(d)
 # Preserve exact existing Shackle transition path/tag/prerequisite policy for each stage.
 original_resolved={}
 for level in range(1,5):
  key='stormshackle'+('' if level==1 else '|'+str(level))
  if key not in files['spells.yml']:continue
  original_resolved=merge(original_resolved,files['spells.yml'][key])
  for field in ('upgrade_required_path','upgrade_required_tags','prerequisite_spells'):
   out['spells.yml'][key].pop(field,None)
   if field in original_resolved:out['spells.yml'][key][field]=copy.deepcopy(original_resolved[field])
 # Shackle family must not contain unknown higher levels that would be silently lost.
 if any(str(k).startswith('stormshackle|') and k not in defs for k in out['spells.yml']):raise ValueError('unknown Shackle level')
 for surface in ('paths.yml','wands.yml'):
  for rank in RANKS:
   row=out[surface].get(rank)
   if not isinstance(row,dict) or not isinstance(row.get('spells'),list):raise ValueError('missing rank '+surface+rank)
  for row in out[surface].values():
   if isinstance(row,dict):
    for field in ('spells','extra_spells','required_spells'):
     if isinstance(row.get(field),list) and any(str(s).split('|')[0] in new for s in row[field]):raise ValueError('prior new spell references')
  for rank in RANKS:
   for key in sorted(allowed):
    if key not in out[surface][rank]['spells']:out[surface][rank]['spells'].append(key)
 return out

def generate(config,baseline,dest):
 if dest.exists():raise ValueError('output must be new directory; preserve previous packet')
 refs=yaml.safe_load((baseline/'stage3-spells.yml').read_text());defs=definitions(config,refs)
 dest.mkdir(parents=True)
 (dest/'definitions.yml').write_text(yaml.safe_dump(defs,sort_keys=False))
 hashes={}
 for stage in range(1,6):
  files={}
  for kind in ('spells','paths','wands'):
   f=baseline/f'stage{stage}-{kind}.yml';raw=f.read_bytes();hashes[f.name]=hashlib.sha256(raw).hexdigest();files[kind+'.yml']=yaml.safe_load(raw)
  out=compose(stage,files,defs);d=dest/f'stage{stage}';d.mkdir()
  for name,data in out.items():(d/name).write_text(yaml.safe_dump(data,sort_keys=False))
 (dest/'INPUT-SHA256.json').write_text(json.dumps(hashes,indent=2)+'\n')
 (dest/'PRACTICE-POLICY.json').write_text(json.dumps({k:{'thresholds':config['practice_thresholds'][:len(p['levels'])-1],'max_level':len(p['levels'])} for k,p in config['spells'].items()},indent=2)+'\n')
if __name__=='__main__':
 generate(json.loads(Path(sys.argv[1]).read_text()),Path(sys.argv[2]),Path(sys.argv[3]))
