"""Phase 5c cycles 3b and 3c: the Vessel's anchors as broken pieces of the hex (memo docs/design/5c-anchor-shard.md; the 3c rework
in docs/design/5c-design.md "Shard rework"). Executed from gen_phase5c_look.py (same globals: A, NS, ROOT, w, write_png, read_png).

8 shapes x 2 looks (charged, cooling) = 16 block models, the blockstate (48 variants onto them), the item model and the textures.
The stone IS the hex's: every carved face is one 16x16 rune tile of hex/runes.png at its own resolution, with the rune left as an
empty dark groove (the hex's unlit ink colour); fracture faces are a rune-free patch of hex/caps.png. No painted glow: the glyphs
leave the stone as particles (client/GlyphParticle, which draws hex/runes_ink.png itself).
"""
import math, os

_HEX = os.path.join(ROOT, f'assets/{NS}/textures/block/hex')
_BLK = f'{A}/textures/block'


def _load(name):
    return read_png(open(os.path.join(_HEX, name), 'rb').read())


# ---------------------------------------------------------------- textures
_rw, _rh, _runes = _load('runes.png')
_, _, _ink = _load('runes_ink.png')
_cw, _ch, _caps = _load('caps.png')

GROOVE = (0x15, 0x15, 0x15)   # HexRenderer's ink colour while the hex holds no rift
CRACK_RGB = (14, 8, 12)       # the hex's own crack colour (gen_phase5c_data.py)
TILES = [0, 1, 2, 5, 6, 9, 12, 15]  # which of the sheet's 16 rune tiles the eight carved textures show (index = row * 4 + column)


def _line(points):
    out = set()
    for (x0, y0), (x1, y1) in zip(points, points[1:]):
        n = max(abs(x1 - x0), abs(y1 - y0))
        for i in range(n + 1):
            out.add((round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n)))
    return out


CRACK = _line([(0, 9), (3, 8), (5, 10), (8, 9), (10, 6), (13, 7), (15, 5)])


def _tile(t, crack=True):
    """One rune tile of the hex at its own 16x16: the hex's stone, the rune an empty groove, the break's crack across it."""
    ox, oy = (t % 4) * 16, (t // 4) * 16
    out = []
    for y in range(16):
        for x in range(16):
            i = (oy + y) * _rw + ox + x
            if crack and (x, y) in CRACK:
                out.append(CRACK_RGB + (255,))
            elif _ink[i][3] > 0 or _runes[i][3] == 0:
                out.append(GROOVE + (255,))
            else:
                out.append(tuple(_runes[i][:3]) + (255,))
    return out


for _k, _t in enumerate(TILES):
    write_png(f'{_BLK}/vessel_anchor_r{_k}.png', 16, 16, _tile(_t))
write_png(f'{_BLK}/vessel_anchor.png', 16, 16, _tile(TILES[0], crack=False))  # the disc's rubble cubes and the hovering shard


def _caps_patch():
    """The 16x16 patch of the hex's cap stone with the least colour (no orange rune) nearest the middle of the sheet."""
    best = None
    for oy in range(40, _ch - 56, 4):
        for ox in range(40, _cw - 56, 4):
            px = [_caps[(oy + y) * _cw + ox + x] for y in range(16) for x in range(16)]
            sat = max(max(p[:3]) - min(p[:3]) for p in px)
            score = sat * 100 + abs(ox + 8 - _cw // 2) + abs(oy + 8 - _ch // 2)
            if best is None or score < best[0]:
                best = (score, px)
    return [tuple(p[:3]) + (255,) for p in best[1]]


write_png(f'{_BLK}/vessel_anchor_break.png', 16, 16, _caps_patch())
# cooling: the crack alone holds a dull ember (the orange of the hex caps' own runes, dimmed)
write_png(f'{_BLK}/vessel_anchor_ember.png', 16, 16, [
    (((0xC8, 0x58, 0x18, 255) if (x + y) % 3 == 0 else (0x7A, 0x2C, 0x10, 255)) if (x, y) in CRACK else (0, 0, 0, 0))
    for y in range(16) for x in range(16)])

# ---------------------------------------------------------------- geometry
# part: (kind, from, to, (axis, angle, origin) or None, group)   kind: bed | slab | spike | chip   group: A | B | None
def P(kind, f, t, rot=None, group=None):
    return {'kind': kind, 'from': list(f), 'to': list(t), 'rot': rot, 'group': group}


def _ctr(f, t):
    return [(f[i] + t[i]) / 2.0 for i in range(3)]


def _yrot(f, t, a):
    return ('y', a, _ctr(f, t))


SHAPES = {
    0: [P('slab', (2, 0, 3), (14, 6, 13), ('z', 22.5, (8, 0, 8)), 'A'), P('spike', (3, 0, 11), (6, 10, 13), ('x', -22.5, (4.5, 0, 12)), 'B'),
        P('chip', (11, 0, 1), (14, 3, 4), _yrot((11, 0, 1), (14, 3, 4), 45))],
    1: [P('slab', (3, 0, 2), (13, 6, 14), ('x', -22.5, (8, 0, 8)), 'A'), P('spike', (11, 0, 2), (13, 11, 5), ('z', 22.5, (12, 0, 3.5)), 'B'),
        P('chip', (1, 0, 10), (4, 2, 13), _yrot((1, 0, 10), (4, 2, 13), 22.5))],
    2: [P('slab', (2, 0, 3), (8, 6, 13), ('z', -22.5, (8, 0, 8)), 'A'), P('slab', (8, 0, 3), (14, 5, 13), ('z', 22.5, (8, 0, 8)), 'B'),
        P('chip', (1, 0, 1), (4, 3, 4), _yrot((1, 0, 1), (4, 3, 4), 45)), P('chip', (12, 0, 12), (15, 2, 15), _yrot((12, 0, 12), (15, 2, 15), -22.5))],
    3: [P('slab', (2, 0, 4), (12, 6, 14), ('x', 22.5, (8, 0, 8)), 'A'), P('spike', (10, 0, 1), (14, 9, 4), ('z', 22.5, (12, 0, 2.5)), 'B'),
        P('chip', (12, 0, 11), (15, 4, 14), _yrot((12, 0, 11), (15, 4, 14), -22.5))],
    4: [P('slab', (3, 0, 3), (13, 5, 13), None, 'A'), P('spike', (2, 0, 2), (5, 11, 4), ('x', 22.5, (3.5, 0, 3)), 'B'),
        P('spike', (11, 0, 12), (14, 8, 14), ('z', 22.5, (12.5, 0, 13)))],
    5: [P('slab', (2, 0, 5), (14, 6, 11), ('z', -22.5, (8, 0, 8)), 'A'), P('spike', (6, 0, 12), (10, 9, 14), ('x', -22.5, (8, 0, 13)), 'B'),
        P('chip', (3, 0, 1), (6, 3, 4), _yrot((3, 0, 1), (6, 3, 4), 22.5))],
    6: [P('slab', (3, 0, 3), (13, 5, 13), ('y', 45, (8, 0, 8)), 'A'), P('spike', (6.5, 4, 6.5), (9.5, 11, 9.5), _yrot((6.5, 4, 6.5), (9.5, 11, 9.5), 22.5), 'B')],
    7: [P('slab', (2, 0, 2), (14, 3, 14), None), P('slab', (4, 3, 5), (13, 6, 12), ('y', 22.5, (8, 3, 8)), 'A'),
        P('spike', (1, 0, 12), (4, 9, 15), ('z', -22.5, (2.5, 0, 13.5)), 'B')],
}
BED = P('bed', (3, 0, 3), (13, 2, 13))
_YMAX = {'bed': 2.5, 'slab': 8.0, 'chip': 8.0, 'spike': 11.5}


def _corners(p):
    f, t = p['from'], p['to']
    pts = [(x, y, z) for x in (f[0], t[0]) for y in (f[1], t[1]) for z in (f[2], t[2])]
    if p['rot']:
        ax, ang, o = p['rot']
        c, s = math.cos(math.radians(ang)), math.sin(math.radians(ang))
        out = []
        for (x, y, z) in pts:
            x, y, z = x - o[0], y - o[1], z - o[2]
            if ax == 'x':
                y, z = y * c - z * s, y * s + z * c
            elif ax == 'y':
                x, z = x * c + z * s, -x * s + z * c
            else:
                x, y = x * c - y * s, x * s + y * c
            out.append((x + o[0], y + o[1], z + o[2]))
        pts = out
    return pts


def _fits(p):
    pts = _corners(p)
    return (all(-1e-6 <= q[0] <= 16 + 1e-6 and -1e-6 <= q[2] <= 16 + 1e-6 for q in pts) and max(q[1] for q in pts) <= _YMAX[p['kind']] + 1e-6)


def _fit(p, shape):
    """Shrinks a part until its rotated corners sit inside the block (x, z in 0..16, tops under the kind's limit)."""
    n = 0
    while not _fits(p):
        f, t = p['from'], p['to']
        cx, cz = (f[0] + t[0]) / 2.0, (f[2] + t[2]) / 2.0
        for i, c in ((0, cx), (2, cz)):
            f[i] = c + (f[i] - c) * 0.97
            t[i] = c + (t[i] - c) * 0.97
        t[1] = f[1] + (t[1] - f[1]) * 0.97
        if p['rot'] and p['rot'][0] == 'y':
            p['rot'] = ('y', p['rot'][1], _ctr(f, t))
        n += 1
        assert n < 80, f'shape {shape}: {p["kind"]} cannot be fitted'
    if n:
        print(f'  anchor shape {shape}: {p["kind"]} {p["from"]}-{p["to"]} shrunk x{0.97 ** n:.2f} to fit')


for _s, _parts in SHAPES.items():
    for _p in _parts:
        _fit(_p, _s)
assert _fits(BED)


def _rot(p):
    if not p['rot']:
        return None
    ax, ang, o = p['rot']
    return {'origin': [round(v, 4) for v in o], 'axis': ax, 'angle': ang}


def _r(v):
    return [round(x, 4) for x in v]


def _broad(p):
    """The face of a spike with the biggest area (the side the carving is shown on): normal along the thinner horizontal axis."""
    dx, dz = p['to'][0] - p['from'][0], p['to'][2] - p['from'][2]
    return 'east' if dx < dz else 'south'


FACES = ('north', 'south', 'east', 'west', 'up', 'down')


def _body(p, carved_face, carved='#carved'):
    faces = {}
    for d in FACES:
        face = {'texture': carved if d == carved_face else '#break'}
        if d == 'down' and p['kind'] == 'bed':
            face['cullface'] = 'down'
        faces[d] = face
    e = {'from': _r(p['from']), 'to': _r(p['to']), 'faces': faces}
    if p['rot']:
        e['rotation'] = _rot(p)
    return e


def _overlay(p, face, tex, light):
    e = {'from': _r([v - 0.02 for v in p['from']]), 'to': _r([v + 0.02 for v in p['to']]), 'shade': False,
         'neoforge_data': {'block_light': light}, 'faces': {face: {'texture': tex}}}
    if p['rot']:
        e['rotation'] = _rot(p)
    return e


def elements(shape, cooling):
    """Group A faces show this shape's rune tile, group B (and other spikes) a second one; cooling adds the ember in the crack."""
    parts = SHAPES[shape]
    els = [_body(BED, None)]
    for p in parts:
        face = 'up' if p['kind'] == 'slab' else (_broad(p) if p['kind'] == 'spike' else None)
        els.append(_body(p, face, '#carved' if p['group'] != 'B' else '#carved2'))
    if cooling:
        for p in parts:
            if p['kind'] == 'slab':
                els.append(_overlay(p, 'up', '#ember', 7))
    return els


def textures(shape):
    return {'carved': f'{NS}:block/vessel_anchor_r{shape}', 'carved2': f'{NS}:block/vessel_anchor_r{(shape + 3) % 8}',
            'break': f'{NS}:block/vessel_anchor_break', 'ember': f'{NS}:block/vessel_anchor_ember', 'particle': f'{NS}:block/vessel_anchor_break'}


# remove what earlier cycles wrote: the band models, the painted rune overlays and the 48 per-state models
for _old in ('bands1', 'bands2', 'cooling', 'bands1_lit', 'bands2_lit', 'cooling_lit', 'runes_ready', 'runes_lit', 'runes_ember', 'runes_mask'):
    for _f in (f'{_BLK}/vessel_anchor_{_old}.png', os.path.join(ROOT, f'assets/{NS}/models/block/vessel_anchor_{_old}.json')):
        if os.path.exists(_f):
            os.remove(_f)
for _s in range(8):
    for _c in (0, 1, 2):
        for _suffix in ('', '_lit'):
            _f = os.path.join(ROOT, f'assets/{NS}/models/block/vessel_anchor_s{_s}_c{_c}{_suffix}.json')
            if os.path.exists(_f):
                os.remove(_f)

_variants = {}
for _s in range(8):
    for _cool in (False, True):
        _name = f'vessel_anchor_s{_s}' + ('_cool' if _cool else '')
        w(f'assets/{NS}/models/block/{_name}.json', {
            'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': textures(_s), 'elements': elements(_s, _cool)})
    for _c in (2, 1, 0):
        for _t in (False, True):
            _variants[f'charges={_c},shape={_s},tethered={str(_t).lower()}'] = {
                'model': f'{NS}:block/vessel_anchor_s{_s}' + ('_cool' if _c == 0 else '')}
w(f'assets/{NS}/blockstates/vessel_anchor.json', {'variants': _variants})

# item: shape 0 plus the hovering shard, shown small in the slot
_shard = {'from': [7, 18, 7.5], 'to': [10, 23.5, 8.5], 'rotation': {'origin': [8.5, 20.5, 8], 'axis': 'z', 'angle': 22.5},
          'faces': {d: {'texture': '#carved' if d in ('north', 'south') else '#break', 'uv': [5, 3, 11, 13]} for d in FACES}}
w(f'assets/{NS}/models/item/vessel_anchor.json', {
    'parent': 'minecraft:block/block', 'textures': textures(0), 'elements': elements(0, False) + [_shard],
    'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, -2, 0], 'scale': [0.55, 0.55, 0.55]}}})
