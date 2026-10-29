"""Offline balance validation only. No server I/O or runtime activation."""
import json, math, sys
SPECS = {'stormshackle': (1, 4, 'first_impact_only', 1.5),
         'wrv_spark': (2, 3, 'single_target', 2.01),
         'wrv_frostbite': (2, 3, 'first_impact_only', 1.0)}

def require(ok, message):
    if not ok: raise ValueError(message)

def number(n):
    return type(n) in (int, float) and math.isfinite(n)

def validate(config):
    require(set(config) == {'schema_version','status','practice_thresholds','spells'}, 'unknown root field')
    require(type(config['schema_version']) is int and config['schema_version'] == 1, 'schema')
    require(config['status'] == 'offline_candidate_not_installed', 'status')
    # Balance tuning deliberately cannot alter earned-practice policy.
    require(config['practice_thresholds'] == [40,120,300,600,1000] and
            all(type(n) is int for n in config['practice_thresholds']), 'approved practice curve')
    require(set(config['spells']) == set(SPECS), 'stable spell IDs')
    for key, (seal, count, delivery, ceiling) in SPECS.items():
        spell = config['spells'][key]
        require(set(spell) == {'display_name','unlock_seal','entry_rank','delivery','secondary_damage','terrain_changes','levels','earns'}, key + ' fields')
        require(isinstance(spell['display_name'], str) and 0 < len(spell['display_name']) <= 64, 'display name')
        require(type(spell['unlock_seal']) is int and spell['unlock_seal'] == seal and spell['entry_rank'] == 'beginner', 'unlock policy')
        require(spell['delivery'] == delivery and spell['secondary_damage'] is False and spell['terrain_changes'] is False, 'effect topology')
        require(type(spell['earns']) is int and (spell['earns'] == 6 if key == 'stormshackle' else 1 <= spell['earns'] <= 8), 'SP earns: preserve Shackle6; minor spells 1..8')
        require(isinstance(spell['levels'],list) and len(spell['levels']) == count, 'native level count')
        previous = -1
        for level in spell['levels']:
            require(set(level) == {'damage_hp','mana','cooldown_ms'}, 'level fields')
            d,m,c = (level[k] for k in ('damage_hp','mana','cooldown_ms'))
            require(number(d) and 0 <= d < ceiling and d >= previous, key + ' modest nondecreasing damage')
            require(number(m) and 0 < m <= 100, 'finite positive mana <=100')
            require(type(c) is int and 1000 <= c <= 60000, 'cooldown 1000..60000 ms')
            previous = d
    return config

def preview(config):
    validate(config)
    return {key: [{'level': i+1, 'earns': spell['earns'], 'cumulative_use_threshold': 0 if i==0 else config['practice_thresholds'][i-1], **level}
                  for i, level in enumerate(spell['levels'])] for key,spell in config['spells'].items()}

if __name__ == '__main__':
    with open(sys.argv[1], encoding='utf8') as f: config=json.load(f)
    print(json.dumps(preview(config), indent=2, allow_nan=False))
