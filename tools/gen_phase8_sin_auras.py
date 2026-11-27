"""Generates phase 8-5 data: the seven sin pylon effects bound to aspects in Gadomancy's aura effect data map, and their hidden research
entries (the keys a pylon unlocks). Original (Concilium, WTFPL): GadomancyIntegration.java:56-198. Brief docs/agents/briefs/phase8-5-sin-auras.md.
The terminus meteor shower type is registered but unbound (user decision g1-design 6).

Notes: thaumadditions:infernum always exists (Forbidden Magic's fallback pack defines it without Thaumic Additions), so neither the binding
nor the research entry is conditioned; the entry's icon is Forbidden Magic's infernus texture, which is always present.
Run from the module root before gen_lang.py.
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genlib import *

# aspect id -> (effect type, interval, range, research icon, grid cell). Cells sorted by entry name, below Gadomancy's row 12.
SINS = [
    ('forbiddenmagic:desidia', 'prolong_effects', 4, 8.0, 'forbiddenmagic:textures/aspects/desidia.png', (6, 13)),
    ('forbiddenmagic:gula', 'feed_players', 20, 8.0, 'forbiddenmagic:textures/aspects/gula.png', (7, 13)),
    ('thaumadditions:infernum', 'smelt_items', 5, 8.0, 'forbiddenmagic:textures/aspects/infernus.png', (8, 13)),
    ('forbiddenmagic:invidia', 'disarm', 80, 8.0, 'forbiddenmagic:textures/aspects/invidia.png', (3, 14)),
    ('forbiddenmagic:ira', 'provoke', 80, 8.0, 'forbiddenmagic:textures/aspects/ira.png', (4, 14)),
    ('forbiddenmagic:luxuria', 'reset_breeding', 40, 8.0, 'forbiddenmagic:textures/aspects/luxuria.png', (5, 14)),
    ('forbiddenmagic:superbia', 'repel', 8, 32.0, 'forbiddenmagic:textures/aspects/superbia.png', (6, 14)),
]

values = {}
for aspect, kind, interval, rng, icon, (col, row) in SINS:
    values[aspect] = {'type': f'{NS}:{kind}', 'interval': interval, 'range': rng}
    ns, path = aspect.split(':')
    key = f'research.gadomancy.aura/{ns}/{path}'
    w(f'data/gadomancy/thaumaturge/research_entry/aura/{ns}/{path}.json', {
        'category': 'gadomancy:gadomancy',
        'name': f'{key}.title',
        'parents': ['gadomancy:aura_effects'],
        'column': col,
        'row': row,
        'icons': [icon],
        'stages': [{'text': f'{key}.stage_0'}],
        'note_aspects': [],
        'complexity': 1,
        'meta': ['hidden', 'round'],
    })
w('data/gadomancy/data_maps/thaumaturge/aspect/aura_effect.json', {'values': values})
