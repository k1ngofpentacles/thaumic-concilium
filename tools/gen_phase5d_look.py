"""Phase 5d cycle 4b: the look (memo docs/design/5d-look.md). Run after gen_phase5d_pond.py, before gen_lang.py.

Writes the textures of the crackle, the glint star and the grove bind sheet (bark and leaves). The Verdant Capital's own art and
models are in gen_phase5d_star.py (the user's redesign replaced the cube with a rotor that this script first wrote).
All art is drawn here (our own); nothing is copied from Botania or Thaumaturge.
"""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genlib import *


class Lcg:
    def __init__(self, seed): self.s = seed
    def next(self, n):
        self.s = (self.s * 1103515245 + 12345) & 0x7fffffff
        return (self.s >> 8) % n


# ---------------------------------------------------------------- the crackle strips, 32x4: white with alpha, used across a ribbon's width
# Left half: alpha falling from the middle to both edges (the violet fringe, the white core). Right half: full across the middle
# ten texels with a short fall at each edge (the black silhouette: an even shade with a firm outline, not a stepped glow).
_px = []
for _y in range(4):
    for _x in range(32):
        if _x < 16:
            _a = max(0.0, 1.0 - abs(_x - 7.5) / 8.0) ** 0.8
        else:
            _a = min(1.0, max(0.0, (8.0 - abs(_x - 23.5)) / 3.0))
        _px.append((255, 255, 255, int(255 * _a)))
write_png(f'{A}/textures/particle/capital_crackle.png', 32, 4, _px)

# ---------------------------------------------------------------- the glint, 8x32: a four-point star in four sizes (pond sparkles, the seed's motes)
_px = []
for _f in range(4):
    _arm = _f + 1  # arm length in pixels
    for _y in range(8):
        for _x in range(8):
            _dx = min(abs(_x - 3), abs(_x - 4)); _dy = min(abs(_y - 3), abs(_y - 4))
            _a = 0
            if _dx == 0 and _dy == 0:
                _a = 255
            elif _dx == 0 and _dy <= _arm:
                _a = int(255 * (1.0 - _dy / (_arm + 1.0)))
            elif _dy == 0 and _dx <= _arm:
                _a = int(255 * (1.0 - _dx / (_arm + 1.0)))
            elif _dx == 1 and _dy == 1 and _arm >= 3:
                _a = 90
            _px.append((255, 255, 255, _a))
write_png(f'{A}/textures/particle/mana_glint.png', 8, 32, _px)

# ---------------------------------------------------------------- the grove bind sheet, 32x32: root bark and a leaf cluster in the top half (cutout); the rest is empty
_rng = Lcg(5405)
_sheet = [(0, 0, 0, 0)] * (32 * 32)


def _put(x, y, c): _sheet[y * 32 + x] = c


for _y in range(16):  # bark: brown, darker streaks along the root, a little moss
    for _x in range(16):
        _v = _rng.next(20)
        if _rng.next(13) == 0:
            _put(_x, _y, (70 + _v, 112 + _v, 48 + _v // 2, 255))
        elif (_x + (_y // 5)) % 4 == 0:
            _put(_x, _y, (58 + _v // 2, 40 + _v // 2, 24 + _v // 2, 255))
        else:
            _put(_x, _y, (98 + _v, 70 + _v, 42 + _v // 2, 255))
for _n in range(11):  # leaf cluster: overlapping small discs in three greens
    _cx = 2 + _rng.next(12); _cy = 2 + _rng.next(12); _rad = 2 + _rng.next(2)
    _g = [(62, 132, 48), (88, 164, 60), (120, 190, 74)][_rng.next(3)]
    for _y in range(16):
        for _x in range(16):
            if math.hypot(_x - _cx, _y - _cy) <= _rad:
                _v = _rng.next(18)
                _edge = math.hypot(_x - _cx, _y - _cy) > _rad - 1
                _k = 0.78 if _edge else 1.0
                _put(16 + _x, _y, (int((_g[0] + _v) * _k), int((_g[1] + _v) * _k), int((_g[2] + _v // 2) * _k), 255))
write_png(f'{A}/textures/entity/grove_bind.png', 32, 32, _sheet)

# Phase 5d cycle 4, the finale's white light (VesselFinaleDraw): a soft white disc, alpha by radius. The preview's exporter reads LIGHT_PROFILE.
LIGHT_PROFILE = [(0.0, 1.0), (0.7, 1.0), (0.88, 0.45), (1.0, 0.0)]


def _light_alpha(rr):
    for (r0, v0), (r1, v1) in zip(LIGHT_PROFILE, LIGHT_PROFILE[1:]):
        if rr <= r1:
            return v0 + (v1 - v0) * (rr - r0) / (r1 - r0)
    return 0.0


_lpx = []
for _y in range(64):
    for _x in range(64):
        _lpx.append((255, 255, 255, int(round(255 * _light_alpha(math.hypot((_x + 0.5) / 32 - 1, (_y + 0.5) / 32 - 1))))))
write_png(f'{A}/textures/misc/vessel_light.png', 64, 64, _lpx)
