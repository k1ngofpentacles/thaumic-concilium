"""Phase 5d cycles 3 and 4d: the mana pond fluid's look. Run after gen_phase5d_data.py, before gen_lang.py.

Render path (user, 2026-11-22): the standard fluid path only, so it works under Iris/Sodium shader packs: a FluidType with
IClientFluidTypeExtensions (a still texture from the block atlas), translucent render type, no shader, no BER, no custom GL.

Cycle 4d (docs/design/5d-pond-look.md, option 1, picked by the user): THE CHART. One animated picture of 128 x 128 pixels
(8 x 8 blocks) that wraps on 128, cut into 64 tiles of 16 pixels; the block at (x, z) shows tile (x & 7, z & 7)
(ManaPondClient.getStillTexture with a position). Neighbouring blocks continue each other and the period (8 blocks) is wider
than a pond, so nothing repeats per block. Layers, all moving by phase rotation between two independent noise fields
(a cos wt + b sin wt: features swell, wander and dissolve, nothing travels, and the loop closes exactly):
  depth zones  value noise, cell 32 px   +-ZONE      1 cycle per loop
  veins        the zero lines of a cell-16 noise, masked by a slow field   0..VEIN, alpha +VEIN_ALPHA   1 cycle
  shimmer      noise, cell 4 px          +-SHIMMER   5 cycles
  grain        per pixel, static         +-GRAIN
and GLINTS texture glints over the whole chart, each on a vein maximum, never two lit at once.
The script checks its own output (tiles differ, the chart wraps, the loop closes, vein area, glints) and fails loudly.
"""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genlib import *

SIZE = 128         # the chart, pixels (8 blocks)
TILES = 8
FRAMES = 64
FRAMETIME = 5      # loop = 320 ticks (16 s); interpolation fades each frame into the next
ZONE = 9.0         # depth zones, +- of 255
VEIN = 16.0        # veins at their brightest
VEIN_W = 0.13      # half width of a vein in noise units (about 1-2 px)
VEIN_ALPHA = 15    # added to alpha 200 on a full vein
SHIMMER = 3.0
GRAIN = 2
GLINTS = 6
GLINT_PEAK = 55
GLINT_ARM = 18
GLINT_APART = 12   # pixels, on the wrapping chart
GLINT_ENV = (0.4, 1.0, 0.8, 0.3)   # its four lit frames
BASE = (155, 215, 245, 200)        # cycle 3's pale blue


class Lcg:
    def __init__(self, seed): self.s = seed
    def next(self, n):
        self.s = (self.s * 1103515245 + 12345) & 0x7fffffff
        return (self.s >> 8) % n
    def unit(self):
        return self.next(20001) / 10000.0 - 1.0


_rng = Lcg(5404)


def field(cell):
    """Value noise in -1..1 on the wrapping chart: a random lattice every `cell` pixels, smoothstep between."""
    n = SIZE // cell
    lat = [[_rng.unit() for _ in range(n)] for _ in range(n)]
    out = [0.0] * (SIZE * SIZE)
    for y in range(SIZE):
        gy, fy = divmod(y / cell, 1.0)
        gy = int(gy); sy = fy * fy * (3 - 2 * fy)
        for x in range(SIZE):
            gx, fx = divmod(x / cell, 1.0)
            gx = int(gx); sx = fx * fx * (3 - 2 * fx)
            a = lat[gy % n][gx % n]; b = lat[gy % n][(gx + 1) % n]
            c = lat[(gy + 1) % n][gx % n]; d = lat[(gy + 1) % n][(gx + 1) % n]
            out[y * SIZE + x] = (a + (b - a) * sx) * (1 - sy) + (c + (d - c) * sx) * sy
    mean = sum(out) / len(out)      # no mean: a rotating field with one would make the whole surface pulse
    return [v - mean for v in out]


_zone = (field(32), field(32))
_zone2 = (field(64), field(64))       # a broader swell under the zones (features about 4 blocks)
_vein = (field(16), field(16))
_mask = (field(32), field(32))        # where veins show at all: they come and go by region
_shim = (field(4), field(4))
_grain = [_rng.next(2 * GRAIN + 1) - GRAIN for _ in range(SIZE * SIZE)]
N = SIZE * SIZE


def rot(pair, cycles, f):
    t = 2 * math.pi * cycles * f / FRAMES
    c, s = math.cos(t), math.sin(t)
    a, b = pair
    return [a[i] * c + b[i] * s for i in range(N)]


def vein_frame(f):
    v = rot(_vein, 1, f)                    # zero lines repeat every half turn: 8 s, lit in different regions each half
    m = rot(_mask, 1, f + FRAMES // 5)
    out = [0.0] * N
    for i in range(N):
        d = abs(v[i])
        if d < VEIN_W:
            k = (m[i] + 0.30) / 0.40            # the mask: nothing below -0.3, full above 0.1
            if k > 0:
                out[i] = (1.0 - d / VEIN_W) * (1.0 if k > 1 else k)
    return out


def wrapd(a, b):
    d = abs(a - b) % SIZE
    return min(d, SIZE - d)


# the glints: each on the brightest vein pixel of its own frame that is far enough from the others; their lit times never overlap
_slots = list(range(0, FRAMES, 4))
_sites = []
for _g in range(GLINTS):
    _f0 = _slots.pop(_rng.next(len(_slots)))
    _vf = vein_frame((_f0 + 1) % FRAMES)
    _best = None
    _start = _rng.next(N)                       # ties are broken by a random scan start, not by the top-left corner
    for _k in range(N):
        _i = (_start + _k) % N
        _x, _y = _i % SIZE, _i // SIZE
        if _vf[_i] > 0.9 and all(max(wrapd(_x, _s[0]), wrapd(_y, _s[1])) >= GLINT_APART for _s in _sites):
            _best = (_x, _y, _f0)
            break
    assert _best, 'no vein pixel left for a glint'
    _sites.append(_best)

_frames = []
_lit_share = []
for _f in range(FRAMES):
    _z = rot(_zone, 1, _f)
    _z2 = rot(_zone2, 1, _f + FRAMES // 8)
    _s = rot(_shim, 5, _f)
    _v = vein_frame(_f)
    _lit_share.append(sum(1 for _q in _v if _q > 0.25) / N)
    _img = []
    for _i in range(N):
        _l = ZONE * (0.65 * _z[_i] + 0.55 * _z2[_i]) + SHIMMER * _s[_i] + _grain[_i]
        _q = VEIN * _v[_i]
        _img.append([BASE[0] + 1.4 * _l + 1.2 * _q, BASE[1] + _l + _q, BASE[2] + 0.4 * _l + 0.6 * _q, BASE[3] + VEIN_ALPHA * _v[_i]])
    for _x, _y, _f0 in _sites:
        _step = (_f - _f0) % FRAMES
        if _step < len(GLINT_ENV):
            _e = GLINT_ENV[_step]
            _p = _img[_y * SIZE + _x]
            for _c in range(3):
                _p[_c] += GLINT_PEAK * _e
            _p[3] += 20 * _e
            for _dx, _dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                _p = _img[((_y + _dy) % SIZE) * SIZE + (_x + _dx) % SIZE]
                for _c in range(3):
                    _p[_c] += GLINT_ARM * _e
    _frames.append([tuple(max(0, min(255, int(_c))) for _c in _p) for _p in _img])

# ---------------------------------------------------------------- self-checks
def _diff(p, q):
    return max(abs(p[0] - q[0]), abs(p[1] - q[1]), abs(p[2] - q[2]))


for _f in (0, 21, 42):
    _img = _frames[_f]
    _inner = max(_diff(_img[_y * SIZE + _x], _img[_y * SIZE + _x + 1]) for _y in range(SIZE) for _x in range(SIZE - 1))
    _seam = max(_diff(_img[_y * SIZE + SIZE - 1], _img[_y * SIZE]) for _y in range(SIZE))
    _seam = max(_seam, max(_diff(_img[(SIZE - 1) * SIZE + _x], _img[_x]) for _x in range(SIZE)))
    assert _seam <= _inner, f'the chart does not wrap in frame {_f}: seam {_seam}, inside {_inner}'
_step_max = max(max(_diff(_frames[_f][_i], _frames[_f + 1][_i]) for _i in range(0, N, 7)) for _f in range(FRAMES - 1))
_close = max(_diff(_frames[FRAMES - 1][_i], _frames[0][_i]) for _i in range(0, N, 7))
assert _close <= _step_max, f'the loop does not close: {_close} against {_step_max} between frames'
assert all(200 <= _p[3] <= 235 for _img in _frames for _p in _img), 'alpha outside 200..235 (Sodium needs translucent pixels)'
assert 0.04 <= min(_lit_share) and max(_lit_share) <= 0.16, f'vein area {min(_lit_share):.3f}..{max(_lit_share):.3f}'
_lum = [sum(_p[1] for _p in _img) / N for _img in _frames]
assert max(_lum) - min(_lum) < 3, 'the whole surface pulses'

# ---------------------------------------------------------------- the tiles
_old = [f'{A}/textures/block/mana_pond_still.png', f'{ROOT}/assets/{NS}/textures/block/mana_pond_still.png.mcmeta']
for _o in _old:
    if os.path.exists(_o):
        os.remove(_o)
_seen = set()
for _j in range(TILES):
    for _i in range(TILES):
        _px = []
        for _img in _frames:
            for _y in range(16):
                _row = (_j * 16 + _y) * SIZE + _i * 16
                _px.extend(_img[_row:_row + 16])
        _seen.add(hash(tuple(_px[:256])))
        write_png(f'{A}/textures/block/mana_pond/t_{_i}_{_j}.png', 16, 16 * FRAMES, _px)
        w(f'assets/{NS}/textures/block/mana_pond/t_{_i}_{_j}.png.mcmeta', {'animation': {'frametime': FRAMETIME, 'interpolate': True}})
assert len(_seen) == TILES * TILES, 'two tiles are alike'

# a liquid block's blockstate has the level property; one unconditional part covers every value. The model is only the break/step particle.
w(f'assets/{NS}/blockstates/mana_pond.json', {'multipart': [{'apply': {'model': f'{NS}:block/mana_pond'}}]})
w(f'assets/{NS}/models/block/mana_pond.json', {'textures': {'particle': f'{NS}:block/mana_pond/t_0_0'}})

# a look at the chart for the notes (not shipped): frames 0, 16, 32 and 48 side by side, three times life size
_shots = os.environ.get('TC_POND_PREVIEW')
if _shots:
    _k = 3
    _wid = (SIZE * _k + 6) * 4
    _px = []
    for _y in range(SIZE * _k):
        for _n, _f in enumerate((0, 16, 32, 48)):
            _img = _frames[_f]
            for _x in range(SIZE * _k):
                _p = _img[(_y // _k) * SIZE + _x // _k]
                _px.append((_p[0], _p[1], _p[2], 255))
            _px.extend([(0, 0, 0, 255)] * 6)
    write_png(_shots, _wid, SIZE * _k, _px)
print(f'pond chart: {TILES * TILES} tiles, {FRAMES} frames, vein area {min(_lit_share):.3f}..{max(_lit_share):.3f}, '
      f'largest step {_step_max}, loop closure {_close}, glints at {[(s[0], s[1], s[2]) for s in _sites]}')
