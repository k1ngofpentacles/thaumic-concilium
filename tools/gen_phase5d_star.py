"""Phase 5d cycle 4b, the Verdant Capital's redesign (user, 2026-11-22; docs/design/5d-look.md, "Addendum" and its correction).
Run after gen_phase5d_look.py, before gen_lang.py.

The capital is a star of two 3D triforces (level-1 Sierpinski tetrahedra), one pointing up and one down: eight small solid
tetrahedra of cyan-gilded gold with a thin lichen along their edges. The shape is built in code by the renderer (a tetrahedron's
faces are at angles a block model cannot hold), so this script writes only art and two stub models: the skin of a triangular face,
its labPBR specular map for shader packs, the beam's tile and the sphere's plain texture.
All of it is drawn here (our own); nothing is copied from Botania or Thaumaturge.
"""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genlib import *


class Lcg:
    def __init__(self, seed): self.s = seed
    def next(self, n):
        self.s = (self.s * 1103515245 + 12345) & 0x7fffffff
        return (self.s >> 8) % n


# ---------------------------------------------------------------- the skin, 32x32: four triangular faces, one per face of a small tetrahedron
# Each 16x16 cell holds one face: corners at the cell's bottom left, bottom right and top middle (the renderer's UVs sit half a
# texel inside). Lichen grows only along the triangle's edges; the rest is gold with the cyan gilding in short runs.
GOLD = (236, 190, 70)
GOLD_SHADE = (196, 140, 40)
GOLD_LIGHT = (255, 236, 140)
CYAN = (70, 220, 205)
CYAN_LIGHT = (190, 255, 245)
LICHEN = (132, 158, 84)
LICHEN_LIGHT = (176, 190, 120)
GILD_MIX = 0.65       # how far a gilded texel goes from gold to cyan
GILD_SHARE = 30       # % of a face's inner texels, in runs of 2 to 4
LICHEN_EDGE = 1.3     # texels from an edge: the lichen's band
LICHEN_SHARE = 45     # % of the texels in that band
LICHEN_CREEP = 12     # % of the texels in the next texel inward

_rng = Lcg(5406)
TRI = ((0.0, 16.0), (16.0, 16.0), (8.0, 0.0))


def _jit(c, n):
    v = _rng.next(2 * n + 1) - n
    return tuple(max(0, min(255, k + v)) for k in c)


def _mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def _edge_distance(x, y):
    """Distance from a texel centre to the nearest edge of the cell's triangle (texels; texels outside count by their own distance)."""
    best = 99.0
    for i in range(3):
        (x0, y0), (x1, y1) = TRI[i], TRI[(i + 1) % 3]
        dx, dy = x1 - x0, y1 - y0
        t = max(0.0, min(1.0, ((x - x0) * dx + (y - y0) * dy) / (dx * dx + dy * dy)))
        best = min(best, math.hypot(x - x0 - t * dx, y - y0 - t * dy))
    return best


_kind = [['gold'] * 32 for _ in range(32)]
for _cell in range(4):
    _ox, _oy = (_cell % 2) * 16, (_cell // 2) * 16
    _inner = []
    for _y in range(16):
        for _x in range(16):
            _d = _edge_distance(_x + 0.5, _y + 0.5)
            if _d < LICHEN_EDGE:
                if _rng.next(100) < LICHEN_SHARE:
                    _kind[_oy + _y][_ox + _x] = 'lichen'
            elif _d < LICHEN_EDGE + 1.0:
                if _rng.next(100) < LICHEN_CREEP:
                    _kind[_oy + _y][_ox + _x] = 'lichen'
            else:
                _inner.append((_x, _y))
    _done = 0
    while _done < len(_inner) * GILD_SHARE // 100:   # the gilding: short horizontal runs inside the face
        _x0, _y0 = _inner[_rng.next(len(_inner))]
        for _x in range(_x0, min(16, _x0 + 2 + _rng.next(3))):
            if (_x, _y0) in _inner and _kind[_oy + _y0][_ox + _x] == 'gold':
                _kind[_oy + _y0][_ox + _x] = 'gild'; _done += 1

_px = []
_spec = []
for _y in range(32):
    for _x in range(32):
        _k = _kind[_y][_x]
        _roll = _rng.next(100)
        if _k == 'lichen':
            _px.append(_jit(LICHEN_LIGHT if _roll < 22 else LICHEN, 12) + (255,))
            _spec.append((30, 10, 0, 255))           # rough, not a metal
        elif _k == 'gild':
            _px.append((CYAN_LIGHT if _roll < 14 else _jit(_mix(GOLD, CYAN, GILD_MIX), 8)) + (255,))
            _spec.append((235, 231, 0, 255))         # labPBR 1.3: smoothness, F0 231 = gold
        else:
            _c = GOLD_SHADE if _roll < 8 else GOLD_LIGHT if _roll < 20 else GOLD
            _px.append(_jit(_c, 6) + (255,))
            _spec.append((215, 231, 0, 255))
write_png(f'{A}/textures/entity/verdant_capital.png', 32, 32, _px)
# Iris reads <name>_s.png beside an entity texture: the gold shines under packs with labPBR; without one the file is ignored
write_png(f'{A}/textures/entity/verdant_capital_s.png', 32, 32, _spec)

# ---------------------------------------------------------------- the ray's tile, 16x32: white with alpha, coloured by the renderer
# One tile along the beam (V): a long bright pulse centred on V = 0 (it wraps) and a faint pip half way between. The beam passes
# through the sphere's centre and drives nothing; the pulses only show which way the mana flows.
_px = []
for _y in range(32):
    _dv = min(_y, 32 - _y)
    _cleat = max(0.0, 1.0 - _dv / 5.0)
    _pip = 0.35 * max(0.0, 1.0 - abs(_y - 16) / 2.0)
    for _x in range(16):
        _across = max(0.0, 1.0 - abs(_x - 7.5) / 8.0)
        _i = (_across ** 1.4) * (0.28 + 0.72 * max(_cleat, _pip))
        _px.append((255, 255, 255, int(255 * min(1.0, _i))))
write_png(f'{A}/textures/misc/capital_ray.png', 16, 32, _px)

# ---------------------------------------------------------------- the sphere and the beam's pulses
# The sphere is a mesh whose colour and translucency are in its vertices (faint facing the viewer, strong at the rim), so its
# texture is plain white. The rim disc of the first build is gone.
write_png(f'{A}/textures/misc/capital_sphere.png', 4, 4, [(255, 255, 255, 255)] * 16)
_old = os.path.join(A, 'textures/misc/capital_sphere_rim.png')
if os.path.exists(_old):
    os.remove(_old)

# ---------------------------------------------------------------- the models
# nothing in the chunk mesh: the star is the renderer's. The block model is only the break particle.
w(f'assets/{NS}/models/block/verdant_capital.json', {'textures': {'particle': f'{NS}:block/verdant_capital'}})
# the item: drawn by the capital's item renderer (the closed star), with the usual block display transforms
w(f'assets/{NS}/models/item/verdant_capital.json', {
    'parent': 'minecraft:builtin/entity', 'gui_light': 'side',
    'display': {
        'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
        'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
        'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
        'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
        'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
        'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]}}})
