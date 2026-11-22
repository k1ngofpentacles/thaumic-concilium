"""Generates phase-5a cycle 5 data: the Arcane Levitator block (Thaumcraft 4's blockLifter, mechanics only: original art and code).
Run after gen_phase5a_data.py and before gen_lang.py.

Recipe (TC4 WEW/BNB/WAW): W greatwood planks, E terra shard -> essentia crystal of terra, A aer shard -> crystal of aer, N nitor (Thaumaturge's
yellow nitor, the base one), B iron ingot; vis aer 10, terra 5. Research: motion 3, flight 3, air 3 -> motus, volatus, aer; complexity 1.
"""
import sys, os, random
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genlib import *

arcane_shaped('arcane_levitator', 'arcane_levitator', ['WEW', 'BNB', 'WAW'],
              {'W': it('thaumaturge:plank_greatwood'), 'E': crystal('terra'), 'A': crystal('aer'), 'N': it('thaumaturge:nitor_yellow'),
               'B': it('minecraft:iron_ingot')}, [('aer', 10), ('terra', 5)], 'arcane_levitator')

# LEVITATOR (Artifice tab, -3,-3 there): concealed, parent NITOR. Ours: the tab root is the parent (no nitor entry exists here), at (-1,-2).
entry('arcane_levitator', -1, -2, f'{NS}:arcane_levitator', [CAT], [('motus', 3), ('volatus', 3), ('aer', 3)],
      [f'{NS}:arcane_workbench/arcane_levitator'], complexity=1, meta=['hidden'])

add_item_aspects('arcane_levitator', [('motus', 10), ('volatus', 10), ('aer', 10), ('machina', 5)])

# ---------------------------------------------------------------- block, item, art
w(f'assets/{NS}/blockstates/arcane_levitator.json', {'variants': {'': {'model': f'{NS}:block/arcane_levitator'}}})
w(f'assets/{NS}/models/block/arcane_levitator.json', {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
    'top': f'{NS}:block/arcane_levitator_top', 'bottom': f'{NS}:block/arcane_levitator_bottom', 'side': f'{NS}:block/arcane_levitator_side',
    'particle': f'{NS}:block/arcane_levitator_side'}})
w(f'assets/{NS}/models/item/arcane_levitator.json', {'parent': f'{NS}:block/arcane_levitator'})
drop_self_loot('arcane_levitator')
w('data/minecraft/tags/block/mineable/axe.json', {'replace': False, 'values': [f'{NS}:arcane_levitator']})

rng = random.Random(5)
PLANK = [(150, 104, 54), (139, 95, 47), (160, 114, 62), (128, 86, 42)]
BRASS = [(214, 170, 62), (186, 142, 44), (240, 200, 96), (120, 88, 30)]
AER = (255, 240, 110)


def planks(x, y):
    """Greatwood planks: 4-pixel boards with a darker seam and grain."""
    c = PLANK[(y // 4 + (x // 8)) % 4]
    if y % 4 == 3:
        c = tuple(int(v * 0.72) for v in c)
    elif rng.random() < 0.18:
        c = tuple(int(v * 0.92) for v in c)
    return c


def mix(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


side, top, bottom = [], [], []
for y in range(16):
    for x in range(16):
        c = planks(x, y)
        # brass frame at the corners and a row of vents in the middle
        if (x in (0, 15) or y in (0, 15)):
            c = BRASS[(x + y) % 3] if (x + y) % 2 == 0 else BRASS[1]
        elif 5 <= y <= 10 and 3 <= x <= 12:
            c = (40, 30, 22) if y % 2 == 1 else mix((40, 30, 22), BRASS[3], 0.6)
        side.append(c + (255,))
        t = planks(x, y)
        d = max(abs(x - 7.5), abs(y - 7.5))
        if d < 5.5:   # a ring of brass around an aer-tinted glow
            t = mix((60, 60, 40), AER, max(0.0, 1.0 - d / 4.0) * 0.9) if d < 3.5 else BRASS[1]
        elif d < 7.5 and (x in (0, 15) or y in (0, 15)):
            t = BRASS[0]
        top.append(t + (255,))
        bottom.append(planks(x, y) + (255,))
write_png(f'{A}/textures/block/arcane_levitator_side.png', 16, 16, side)
write_png(f'{A}/textures/block/arcane_levitator_top.png', 16, 16, top)
write_png(f'{A}/textures/block/arcane_levitator_bottom.png', 16, 16, bottom)
