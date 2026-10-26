"""WRV spell level fixes (Mike, Oct 26 2026 3:05 PM CDT: "Yes, use all the level fixes").
Source: batch-next/spell-level-audit-20261026/AUDIT.md, proposed fixes table (incl. optional Lightning 2).
Rank gates (upgrade_required_*) untouched; Missile damage cap untouched. Text edit, so comments and order stay."""
import re, yaml
NOTE='WRV level fix Oct 26 2026 (Mike 3:05 PM CDT)'
FIX=[('mend|2',     [('costs','mana',6),('parameters','amount',1.75)]),
     ('flashheal|2',[('costs','mana',19),('parameters','amount',5.5)]),
     ('greaterheal|2',[('costs','mana',9),('parameters','amount',7)]),
     ('stormshackle|2',[('costs','mana',16),('parameters','cooldown',7000)]),
     ('missile|5',  [('parameters','cooldown',850),('parameters','range',72)]),
     ('flare|2',    [('parameters','range',24),('parameters','cooldown',1500)]),
     ('flare|3',    [('parameters','range',32),('parameters','cooldown',1000)]),
     ('platform|4', [('parameters','radius',2.5)]),
     ('lightning|2',[('parameters','cooldown',40000)])]
def _blk(text,key):
    m=re.search(r'(?m)^'+re.escape(key)+r':[^\n]*\n',text)
    if not m: return None
    n=re.search(r'(?m)^[^\s#\n]',text[m.end():])
    e=m.end()+(n.start() if n else len(text)-m.end())
    return m.start(),e
def fmt(v): return str(v)
def apply(text):
    done=[]
    for key,sets in FIX:
        b=_blk(text,key)
        if not b: continue
        s,e=b; seg=text[s:e]
        body=seg.rstrip('\n'); tail=seg[len(body):]
        for sec,p,v in sets:
            sm=re.search(r'(?m)^  '+sec+r':\n',body)
            line=f'    {p}: {fmt(v)}  # {NOTE}'
            if sm:
                sub_end=re.search(r'(?m)^  \S',body[sm.end():])
                se=sm.end()+(sub_end.start() if sub_end else len(body)-sm.end())
                sub=body[sm.end():se]
                pm=re.search(r'(?m)^    '+p+r':.*$',sub)
                if pm: sub=sub[:pm.start()]+line+f' (was {pm.group(0).split(":",1)[1].strip()})'+sub[pm.end():]
                else: sub=line+'\n'+sub
                body=body[:sm.end()]+sub+body[se:]
            else:
                body=body+f'\n  {sec}:\n{line}'
        text=text[:s]+body+tail+text[e:]
        done.append(key)
    y=yaml.safe_load(text)
    for key,sets in FIX:
        if key in done:
            for sec,p,v in sets: assert y[key][sec][p]==v,(key,sec,p)
    return text,done
