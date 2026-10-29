import copy,json,unittest,os
from pathlib import Path
import yaml
from generate import definitions,compose,walk
ROOT=Path(__file__).parent;BASE=Path(os.environ['MAGIC_STAGE_REFERENCE_DIR'])
C=json.loads((ROOT/'balance.json').read_text());R=yaml.safe_load((BASE/'stage3-spells.yml').read_text())
class GenerateTests(unittest.TestCase):
 def test_definitions(self):
  d=definitions(C,R);self.assertEqual(len(d),10)
  for key,v in d.items():
   with self.subTest(key=key):
    ds=[n for n in walk(v['actions']) if n.get('class')=='Damage'];self.assertEqual(len(ds),1)
    self.assertEqual(v['earns'],6);self.assertEqual(v['earns_cooldown_scale'],1)
    self.assertNotIn('headshot_parameters',v);self.assertNotIn('inherit',v)
    self.assertNotIn('upgrade_required_casts',v)
    self.assertFalse(any(n.get('class') in ('BreakBlock','Freeze','Sphere','ModifyBreakable') for n in walk(v['actions'])))
    self.assertEqual(ds[0]['damage'],v['parameters']['damage'])
 def test_control_preserved(self):
  d=definitions(C,R)
  for level in range(1,5):
   v=d['stormshackle'+('' if level==1 else '|'+str(level))]
   counts=lambda x,cls:sum(n.get('class')==cls for n in walk(x['actions']))
   self.assertEqual(counts(v,'Repeat'),2);self.assertEqual(counts(v,'AreaOfEffect'),2)
   self.assertGreaterEqual(counts(v,'PotionEffect'),5)
 def test_all_stages_preserve_unrelated(self):
  d=definitions(C,R)
  for stage in range(1,6):
   with self.subTest(stage=stage):
    files={k+'.yml':yaml.safe_load((BASE/f'stage{stage}-{k}.yml').read_text()) for k in ('spells','paths','wands')};before=copy.deepcopy(files);out=compose(stage,files,d)
    self.assertEqual(files,before)
    for key,v in files['spells.yml'].items():
     if key.split('|')[0]!='stormshackle':self.assertEqual(v,out['spells.yml'][key])
    for kind in ('paths.yml','wands.yml'):
     for rank,row in files[kind].items():
      for field,value in row.items():
       if field!='spells' or rank not in ('beginner','student','apprentice','master'):self.assertEqual(value,out[kind][rank][field])
     for rank in ('beginner','student','apprentice','master'):
      self.assertIn('stormshackle',out[kind][rank]['spells'])
      self.assertEqual('wrv_spark' in out[kind][rank]['spells'],stage>=2)
    self.assertEqual('wrv_spark' in out['spells.yml'],stage>=2)
 def test_transition_gates_retained(self):
  d=definitions(C,R)
  self.assertEqual(d['stormshackle']['upgrade_required_path'],R['stormshackle']['upgrade_required_path'])
  f={k+'.yml':yaml.safe_load((BASE/f'stage2-{k}.yml').read_text()) for k in ('spells','paths','wands')};o=compose(2,f,d)
  self.assertNotIn('upgrade_required_path',o['spells.yml']['stormshackle'])
  self.assertEqual(o['spells.yml']['stormshackle|3']['upgrade_required_path'],'master')
 def test_collision(self):
  f={k+'.yml':yaml.safe_load((BASE/f'stage2-{k}.yml').read_text()) for k in ('spells','paths','wands')};f['spells.yml']['wrv_spark|8']={}
  with self.assertRaises(ValueError):compose(2,f,definitions(C,R))
 def test_reference_not_mutated(self):
  before=copy.deepcopy(R);definitions(C,R);self.assertEqual(R,before)
 def test_tuning_reaches_native(self):
  c=copy.deepcopy(C);c['spells']['wrv_spark']['levels'][0].update(damage_hp=.8,mana=5,cooldown_ms=4000);d=definitions(c,R)['wrv_spark']
  self.assertEqual(d['parameters']['damage'],.8);self.assertEqual(d['costs']['mana'],5);self.assertEqual(d['parameters']['cooldown'],4000)
if __name__=='__main__':unittest.main()
