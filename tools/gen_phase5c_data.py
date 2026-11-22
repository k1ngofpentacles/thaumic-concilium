"""Phase 5c cycle 1 (the sink): the Hex's crack blockstates and retextured runes, the Vessel anchor block (placeholder art) and aspects.
Run after gen_phase6a_data.py (it reads the runes texture that script writes and replaces the hex blockstate), before gen_lang.py.
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genlib import *

# ---------------------------------------------------------------- the hex: four crack states (the renderer draws the rings)
w(f'assets/{NS}/blockstates/hex_of_predictability.json', {'variants': {
    f'crack={n}': {'model': f'{NS}:block/hex_of_predictability'} for n in range(4)}})


class Lcg:
    def __init__(self, seed): self.s = seed
    def next(self, n):
        self.s = (self.s * 1103515245 + 12345) & 0x7fffffff
        return (self.s >> 8) % n


_runes = f'{A}/textures/block/hex/runes.png'
if os.path.exists(_runes):
    wid, hei, px = read_png(open(_runes, 'rb').read())
    for stage in (1, 2, 3):
        rng = Lcg(5300 + stage)
        out = [(int(p[0] * (1 - 0.12 * stage)), int(p[1] * (1 - 0.12 * stage)), int(p[2] * (1 - 0.1 * stage)), p[3]) for p in px]
        for _ in range(5 * stage):
            x, y = rng.next(wid), rng.next(hei)
            dx, dy = rng.next(3) - 1, rng.next(3) - 1
            for _step in range(6 + 5 * stage):
                if 0 <= x < wid and 0 <= y < hei and out[y * wid + x][3] > 0:
                    out[y * wid + x] = (14, 8, 12, out[y * wid + x][3])
                x += dx if rng.next(4) else rng.next(3) - 1
                y += dy if rng.next(4) else rng.next(3) - 1
        write_png(f'{A}/textures/block/hex/runes_crack{stage}.png', wid, hei, out)
else:
    print('runes.png not found (run gen_phase6a_data.py first): crack textures not written')

# ---------------------------------------------------------------- the anchor block (placeholder art, cycle 4 gives it a look)
w(f'assets/{NS}/blockstates/vessel_anchor.json', {'variants': {
    f'charges={c},tethered={str(t).lower()}': {'model': f'{NS}:block/vessel_anchor'} for c in range(3) for t in (False, True)}})
w(f'assets/{NS}/models/block/vessel_anchor.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/vessel_anchor'}})
w(f'assets/{NS}/models/item/vessel_anchor.json', {'parent': f'{NS}:block/vessel_anchor'})
_rng = Lcg(5301)
_px = []
for y in range(16):
    for x in range(16):
        v = 40 + _rng.next(22)
        rim = x in (0, 15) or y in (0, 15)
        _px.append((v + 30, v - 10, v + 50, 255) if rim else (v, v - 8, v + 8, 255))
write_png(f'{A}/textures/block/vessel_anchor.png', 16, 16, _px)
add_item_aspects('vessel_anchor', [('terra', 20), ('vacuos', 10), ('alienis', 10)])

# ---------------------------------------------------------------- cycle 2: the fight's tags and effect icons
w(f'data/{NS}/tags/entity_type/vessel_pull_immune.json', {'replace': False, 'values': [
    f'{NS}:vessel', f'{NS}:vessel_clot', f'{NS}:brightest_one', 'minecraft:ender_dragon', 'minecraft:wither']})
w(f'data/{NS}/tags/block/vessel_edible.json', {'replace': False, 'values': [
    '#thaumaturge:taint_blocks', '#thaumaturge:physical_flux', '#minecraft:flowers', '#minecraft:saplings', '#minecraft:replaceable',
    'minecraft:short_grass', 'minecraft:tall_grass', 'minecraft:fern', 'minecraft:large_fern']})
for _name, _c in (('tethered', (155, 92, 255)), ('flung', (176, 240, 255))):
    _px = []
    for y in range(18):
        for x in range(18):
            d = max(abs(x - 8.5), abs(y - 8.5))
            _px.append((_c[0], _c[1], _c[2], 255) if d < 6 else ((_c[0] // 3, _c[1] // 3, _c[2] // 3, 255) if d < 8 else (0, 0, 0, 0)))
    write_png(f'{A}/textures/mob_effect/{_name}.png', 18, 18, _px)

# ---------------------------------------------------------------- cycle 3: the Vessel look (textures, anchor models, sounds)
exec(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "gen_phase5c_look.py")).read())
