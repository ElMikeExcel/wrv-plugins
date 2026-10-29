import copy,json,unittest
from pathlib import Path
from validate import validate,preview
BASE=json.loads(Path(__file__).with_name('balance.json').read_text())
class Tests(unittest.TestCase):
 def reject(self, mutate):
  c=copy.deepcopy(BASE);mutate(c)
  with self.assertRaises((ValueError,TypeError)):validate(c)
 def test_shackle_earns_locked(self):self.reject(lambda c:c['spells']['stormshackle'].__setitem__('earns',7))
 def test_new_spell_earns_adjustable(self):
  c=copy.deepcopy(BASE);c['spells']['wrv_spark']['earns']=4;validate(c)
 def test_new_spell_earns_bounded(self):self.reject(lambda c:c['spells']['wrv_spark'].__setitem__('earns',20))
 def test_earns_integer(self):self.reject(lambda c:c['spells']['wrv_frostbite'].__setitem__('earns',6.5))
 def test_earns_not_bool(self):self.reject(lambda c:c['spells']['wrv_frostbite'].__setitem__('earns',True))
 def test_candidate(self):self.assertEqual(len(preview(BASE)['stormshackle']),4)
 def test_adjustment(self):
  c=copy.deepcopy(BASE);c['spells']['stormshackle']['levels'][0]['mana']=14;validate(c)
 def test_display_only_identity(self):
  c=copy.deepcopy(BASE);c['spells']['wrv_spark']['display_name']='Minor Spark';validate(c)
 def test_curve(self):self.reject(lambda c:c['practice_thresholds'].__setitem__(4,999))
 def test_unknown_root(self):self.reject(lambda c:c.update(sp_bonus=2))
 def test_identity(self):self.reject(lambda c:c['spells'].__setitem__('missile',c['spells'].pop('wrv_spark')))
 def test_unlock(self):self.reject(lambda c:c['spells']['wrv_spark'].__setitem__('unlock_seal',1))
 def test_rank(self):self.reject(lambda c:c['spells']['stormshackle'].__setitem__('entry_rank','student'))
 def test_count(self):self.reject(lambda c:c['spells']['stormshackle']['levels'].pop())
 def test_damage_ceiling(self):self.reject(lambda c:c['spells']['stormshackle']['levels'][-1].__setitem__('damage_hp',1.5))
 def test_nan(self):self.reject(lambda c:c['spells']['wrv_spark']['levels'][0].__setitem__('damage_hp',float('nan')))
 def test_infinite_mana(self):self.reject(lambda c:c['spells']['wrv_spark']['levels'][0].__setitem__('mana',float('inf')))
 def test_bool(self):self.reject(lambda c:c['spells']['wrv_spark']['levels'][0].__setitem__('mana',True))
 def test_zero_cost(self):self.reject(lambda c:c['spells']['wrv_spark']['levels'][0].__setitem__('mana',0))
 def test_cooldown(self):self.reject(lambda c:c['spells']['wrv_spark']['levels'][0].__setitem__('cooldown_ms',0))
 def test_fractional_ms(self):self.reject(lambda c:c['spells']['wrv_spark']['levels'][0].__setitem__('cooldown_ms',1000.5))
 def test_secondary(self):self.reject(lambda c:c['spells']['stormshackle'].__setitem__('secondary_damage',True))
 def test_terrain(self):self.reject(lambda c:c['spells']['wrv_frostbite'].__setitem__('terrain_changes',True))
 def test_unknown_level_field(self):self.reject(lambda c:c['spells']['wrv_spark']['levels'][0].update(earns=20))
 def test_decreasing_damage(self):self.reject(lambda c:c['spells']['wrv_spark']['levels'][-1].__setitem__('damage_hp',0.1))
 def test_preview_thresholds(self):self.assertEqual([v['cumulative_use_threshold'] for v in preview(BASE)['wrv_spark']],[0,40,120])
 def test_no_mutation(self):
  c=copy.deepcopy(BASE);preview(c);self.assertEqual(c,BASE)
if __name__=='__main__': unittest.main()
