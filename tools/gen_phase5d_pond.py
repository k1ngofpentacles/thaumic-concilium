"""Phase 5d cycle 3: the mana pond fluid's placeholder look. Run after gen_phase5d_data.py, before gen_lang.py.

Render path (user, 2026-11-22): the standard fluid path only, so it works under Iris/Sodium shader packs: a FluidType with
IClientFluidTypeExtensions (still texture from the block atlas), translucent render type, no shader, no BER, no custom GL.
The still texture is an animation strip (FRAMES frames, frametime FRAMETIME) with a .mcmeta, so cycle 4 can add a sparkly sheen in the
texture itself. This is a plain still, glowing, pale blue placeholder; the final art is cycle 4's.
"""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genlib import *

FRAMES = 8
FRAMETIME = 6

# a liquid block's blockstate has the level property; one unconditional part covers every value. The model is only the break/step particle.
w(f'assets/{NS}/blockstates/mana_pond.json', {'multipart': [{'apply': {'model': f'{NS}:block/mana_pond'}}]})
w(f'assets/{NS}/models/block/mana_pond.json', {'textures': {'particle': f'{NS}:block/mana_pond_still'}})


class Lcg:
    def __init__(self, seed): self.s = seed
    def next(self, n):
        self.s = (self.s * 1103515245 + 12345) & 0x7fffffff
        return (self.s >> 8) % n


# a seamless pale-blue field: a sum of two sine waves that wrap on 16 pixels, drifting a little per frame, plus a little fixed grain
_rng = Lcg(5403)
_grain = [[_rng.next(10) for _x in range(16)] for _y in range(16)]
_px = []
for _f in range(FRAMES):
    _phase = _f / FRAMES * 2 * math.pi
    for _y in range(16):
        for _x in range(16):
            _wave = 0.5 + 0.25 * math.sin(2 * math.pi * _x / 16 + _phase) + 0.25 * math.sin(2 * math.pi * (_x + 2 * _y) / 16 - _phase)
            _g = _grain[_y][_x]
            _r = int(120 + 70 * _wave + _g)
            _g2 = int(190 + 50 * _wave + _g)
            _b = int(235 + 20 * _wave)
            _px.append((min(255, _r), min(255, _g2), min(255, _b), 200))
write_png(f'{A}/textures/block/mana_pond_still.png', 16, 16 * FRAMES, _px)
w(f'assets/{NS}/textures/block/mana_pond_still.png.mcmeta', {'animation': {'frametime': FRAMETIME, 'interpolate': True}})
