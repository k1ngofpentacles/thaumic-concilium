"""Phase 10-4 look: blockstates, models and textures of the Palisade (thaumicconcilium:stockade) and the Burning Stake
(thaumicconcilium:burning_stake). Owned by the look agent; gen_phase10_site.py writes the data only.
Run from the module root:  python -I tools/gen_phase10_site_look.py

Palisade (user decision 2026-11-27, a deliberate improvement on the original's stepped point): bark-on oak logs (vanilla oak log by
reference), each axe-hewn to a four-facet point of pale cut wood with a fire-hardened tip. The facets are four flat planes leaning
22.5 degrees inward, each showing a triangle (cutout texture), so the point is a true pyramid, not steps. Point heights differ from
log to log (weighted random models, seeded by position). Joined logs carry a rope lashing under the points.
Burning Stake: one squared, seasoned timber with a blunt top, charred at the foot, an iron hoop and a short chain on the shackle part.
All new textures are drawn here with the art kit's ramps (original art; nothing sampled from Witchery, which is tier C)."""
import json, math, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, '..', '..', '..', 'tools', 'art-style'))
from artkit import tone, _noise
from pngio import write_png

NS = 'thaumicconcilium'
A = os.path.join('src', 'main', 'resources', 'assets', NS)
CLEAR = (0, 0, 0, 0)


def r3(v):
    return round(v, 3)


def wj(path, obj):
    p = os.path.join(A, path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w', newline='\n') as f:
        json.dump(obj, f, indent=1)
        f.write('\n')


def rm(path):
    p = os.path.join(A, path)
    if os.path.exists(p):
        os.remove(p)


def png(name, px):
    write_png(os.path.join(A, 'textures', 'block', name + '.png'), 16, 16, px)


# ================================================================ textures
W = 7.0                                   # a log is 7 px across
LEAN = math.radians(22.5)
RISE = (W / 2) / math.tan(LEAN)           # 8.45: height of the point
SLANT = (W / 2) / math.sin(LEAN)          # 9.146: length of a facet along its slope
V0 = 16 - SLANT                           # top of the facet in the texture


def point_texture():
    """Two facet triangles (columns 0..6 and 8..14), base on the bottom row. Long vertical axe strokes in pale wood; the top third
    darkens through scorched wood into char."""
    px = [CLEAR] * 256
    widths = [7, 7, 5, 5, 5, 3, 3, 1, 1, 1]            # per row from the base up
    for k, x0 in enumerate((0, 8)):
        for row, wid in enumerate(widths):
            y = 15 - row
            for c in range(7):
                if abs(c - 3) > wid // 2:
                    continue
                n = _noise(c + k * 31, row // 4 + k * 5, 11)          # a stroke keeps its tone for about four rows
                t = 3 + (1 if n > 0.8 else -1 if n < 0.12 else 0)
                if c == 3 - wid // 2 and wid > 1:
                    t = min(t + 1, 5)                                  # the lit edge of the cut
                if row == 0 and _noise(c, k, 5) > 0.5:
                    t = 2                                              # ragged shadow where the bark stops
                col = tone('hewn_oak', t)
                if row >= 9:
                    col = tone('char', 2)
                elif row >= 7:
                    col = tone('char', 3)
                elif row >= 5:
                    col = tone('hewn_oak', 1 if c == 3 - wid // 2 and wid > 1 else 0)   # fire-hardened: dark brown, not black
                elif row == 4:
                    col = tone('hewn_oak', 1) if _noise(c, k, 3) > 0.35 else tone('char', 4)
                elif row == 3 and _noise(c, k, 17) > 0.55:
                    col = tone('hewn_oak', 2)                          # scorch creeping down
                px[y * 16 + x0 + c] = col
    png('stockade_point', px)


def rope_texture():
    """Twisted two-strand rope, tiles in both directions (the band uses any two rows)."""
    seq = (3, 2, 1, 2)
    px = []
    for y in range(16):
        for x in range(16):
            t = seq[(x + y * 3) % 4]
            if t == 3 and _noise(x, y, 2) > 0.8:
                t = 4
            px.append(tone('hemp', t))
    png('stockade_rope', px)


def timber_tone(x, y, seed):
    """Seasoned, squared timber: grain runs up the beam in long streaks, with a few checks (drying cracks)."""
    off = int(_noise(x, 0, seed + 5) * 12)                             # streaks start at a different height in every column
    n = _noise(x, (y + off) // 7 + x * 7, seed)
    t = 3 + (1 if n > 0.7 else -1 if n < 0.25 else 0)
    if _noise(x, (y + off) // 4, seed + 1) > 0.9:
        t = 1                                                          # a check, four rows long
    if _noise(x, y, seed + 2) > 0.93:
        t = max(1, t - 1)
    return t


def stake_textures():
    px = [tone('seasoned_timber', timber_tone(x, y, 21)) for y in range(16) for x in range(16)]
    png('burning_stake_side', px)

    # the foot: the same timber, soot-dark from a third of the way down, charcoal at the bottom (blocky, cracked, a few ash flecks)
    foot = []
    for y in range(16):
        for x in range(16):
            edge = 9 + int((_noise(x, 0, 33) - 0.5) * 5)               # where char starts in this column
            t = timber_tone(x, y, 21)
            if y >= edge:
                crack = (x + (y // 3)) % 4 == 0 or (y % 3 == 0 and _noise(x // 2, y, 35) > 0.45)
                c = tone('char', 0 if crack else (3 if _noise(x, y, 36) > 0.8 else 2 if _noise(x // 2, y // 2, 37) > 0.5 else 1))
                if not crack and _noise(x, y, 38) > 0.95:
                    c = tone('stone', 2)                               # ash
            elif y >= edge - 3:
                c = tone('char', 3) if _noise(x, y, 39) > 0.5 else tone('seasoned_timber', 1)
            elif y >= 4:
                c = tone('seasoned_timber', max(1, t - 1))             # soot
            else:
                c = tone('seasoned_timber', t)
            foot.append(c)
    png('burning_stake_foot', foot)

    # end grain: square growth rings around the heart, one check to the edge
    end = []
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            t = (4, 3, 4, 2)[int(d) % 4]
            if t > 2 and _noise(x, y, 41) > 0.85:
                t -= 1
            if d < 1:
                t = 1
            if y == 8 and x > 8:
                t = 1
            end.append(tone('seasoned_timber', t))
    png('burning_stake_end', end)

    # iron: rows 0-1 the hoop (riveted every four pixels), rows 2-3 its shadowed edge, two chain strips, a plain patch for the staple
    iron = [CLEAR] * 256
    for x in range(16):
        iron[x] = tone('wrought_iron', 5 if x % 4 == 1 else 4 if _noise(x, 0, 51) > 0.5 else 3)
        iron[16 + x] = tone('wrought_iron', 1 if x % 4 == 1 else 2)
        iron[32 + x] = iron[48 + x] = tone('wrought_iron', 1)
    front = ['.#.', '###', '#.#', '###', '.#.', '.#.', '###', '#.#', '###']
    side = ['.#.', '.#.', '.#.', '###', '#.#', '#.#', '###', '.#.', '.#.']
    for x0, rows in ((0, front), (4, side)):
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch == '#':
                    iron[(5 + j) * 16 + x0 + i] = tone('wrought_iron', 4 if i == 0 else 2 if i == 2 else 3)
    for y in range(5, 9):
        for x in range(8, 12):
            iron[y * 16 + x] = tone('wrought_iron', 3 if (x + y) % 3 else 2)
    png('burning_stake_iron', iron)


# ================================================================ palisade models
BARK = 'minecraft:block/oak_log'
RINGS = 'minecraft:block/oak_log_top'
PAL_TEX = {'bark': BARK, 'rings': RINGS, 'point': f'{NS}:block/stockade_point', 'rope': f'{NS}:block/stockade_rope', 'particle': BARK}
HEIGHTS = {'a': 9.5, 'b': 8.5, 'c': 7.5}       # where the bark ends on a pointed log; the point adds 8.45 (the tallest tip stands 1.95 px over the block)


def bark(x0, z0, x1, z1, h, u0, top=True):
    side = lambda wid: {'texture': '#bark', 'uv': [r3(u0), 0, r3(u0 + wid), r3(h)]}
    f = {'north': side(x1 - x0), 'south': side(x1 - x0), 'east': side(z1 - z0), 'west': side(z1 - z0),
         'down': {'texture': '#rings', 'uv': [0, 0, 16, 16]}}
    if top:
        f['up'] = {'texture': '#rings', 'uv': [r3(8 - (x1 - x0) / 2), r3(8 - (z1 - z0) / 2), r3(8 + (x1 - x0) / 2), r3(8 + (z1 - z0) / 2)]}
    return {'from': [x0, 0, z0], 'to': [x1, h, z1], 'faces': f}


def point(x0, z0, x1, z1, h):
    """Four facets on a 7 x 7 log top at height h, each a flat plane leaning 22.5 degrees toward the middle."""
    top = r3(h + SLANT)
    uva, uvb = [0, r3(V0), 7, 16], [8, r3(V0), 15, 16]
    def plane(frm, to, face, axis, angle, uv):
        return {'from': frm, 'to': to, 'rotation': {'origin': [frm[0], h, frm[2]], 'axis': axis, 'angle': angle},
                'faces': {face: {'texture': '#point', 'uv': uv}}}
    return [plane([x0, h, z0], [x1, top, z0], 'north', 'x', 22.5, uva),
            plane([x0, h, z1], [x1, top, z1], 'south', 'x', -22.5, uvb),
            plane([x1, h, z0], [x1, top, z1], 'east', 'z', 22.5, uvb),
            plane([x0, h, z0], [x0, top, z1], 'west', 'z', -22.5, uva)]


def band(x0, z0, x1, z1):
    """Rope lashing, 2 px high under the points, 0.4 proud of the logs."""
    y0, y1 = 4.0, 6.0
    f = {}
    for d, wid, v in (('north', x1 - x0, 0), ('south', x1 - x0, 4), ('east', z1 - z0, 8), ('west', z1 - z0, 12)):
        f[d] = {'texture': '#rope', 'uv': [0, v, r3(min(16, wid)), v + 2]}
    f['up'] = f['down'] = {'texture': '#rope', 'uv': [0, 0, r3(min(16, x1 - x0)), r3(min(16, z1 - z0))]}
    return {'from': [x0, y0, z0], 'to': [x1, y1, z1], 'faces': f}


def pal(name, elements, parent=None, cutout=False):
    m = {'textures': PAL_TEX, 'elements': elements}
    if parent:
        m = {'parent': parent, **m}
    if cutout:
        m['render_type'] = 'minecraft:cutout'
    wj(f'models/block/{name}.json', m)


C = (4.5, 4.5, 11.5, 11.5)                # the lone log, and the corner post
P1 = (4.5, 0.5, 11.5, 7.5)                # a joined block: two logs along z (the blockstate turns them for east-west)
P2 = (4.5, 8.5, 11.5, 15.5)
STUB = (4.5, 0.5, 11.5, 4.0)              # at a corner: a split log filling toward a joined side


def palisade_models():
    pal('stockade_log', [bark(*C, 16, 4.5)], parent='minecraft:block/block')
    pal('stockade_pair', [bark(*P1, 16, 0), bark(*P2, 16, 9)])
    pal('stockade_stub', [bark(*STUB, 16, 2)])
    order = 'abc'
    for i, v in enumerate(order):
        h = HEIGHTS[v]
        pal(f'stockade_log_tip_{v}', [bark(*C, h, 4.5, top=False)] + point(*C, h), parent='minecraft:block/block', cutout=True)
        h2 = HEIGHTS[order[(i + 2) % 3]]                                # the pair's second log takes another height
        pal(f'stockade_pair_tip_{v}', [bark(*P1, h, 0, top=False)] + point(*P1, h) + [bark(*P2, h2, 9, top=False)] + point(*P2, h2)
            + [band(4.1, 0, 11.9, 16)], cutout=True)
    pal('stockade_stub_tip', [bark(*STUB, 13, 2), band(4.1, 0, 11.9, 4.1)])
    pal('stockade_band', [band(4.1, 4.1, 11.9, 11.9)])
    wj('models/item/stockade.json', {'parent': f'{NS}:block/stockade_log_tip_a'})
    for old in ('stockade_post', 'stockade_post_tip', 'stockade_tip', 'stockade_side', 'stockade_pair_tip'):
        rm(f'models/block/{old}.json')

    m = lambda n, **kw: {'model': f'{NS}:block/{n}', **kw}
    Z = {'OR': [{'north': 'true'}, {'south': 'true'}]}
    X = {'OR': [{'east': 'true'}, {'west': 'true'}]}
    lone = {'north': 'false', 'east': 'false', 'south': 'false', 'west': 'false'}
    centre = {'OR': [lone, {'AND': [Z, X]}]}                           # one log in the middle: alone, or as a corner post
    z_only = {'AND': [Z, {'east': 'false', 'west': 'false'}]}
    x_only = {'AND': [X, {'north': 'false', 'south': 'false'}]}
    tip = lambda cond, t: {'AND': [cond, {'tip': t}]}
    parts = [
        {'when': tip(centre, 'false'), 'apply': m('stockade_log')},
        {'when': tip(centre, 'true'), 'apply': [m(f'stockade_log_tip_{v}') for v in order]},
        {'when': tip(z_only, 'false'), 'apply': m('stockade_pair')},
        {'when': tip(z_only, 'true'), 'apply': [m(f'stockade_pair_tip_{v}', y=y) for v in order for y in (0, 180)]},
        {'when': tip(x_only, 'false'), 'apply': m('stockade_pair', y=90)},
        {'when': tip(x_only, 'true'), 'apply': [m(f'stockade_pair_tip_{v}', y=y) for v in order for y in (90, 270)]},
        {'when': {'AND': [Z, X, {'tip': 'true'}]}, 'apply': m('stockade_band')},
    ]
    for side, cross, y in (('north', X, 0), ('east', Z, 90), ('south', X, 180), ('west', Z, 270)):
        for t, model in (('false', 'stockade_stub'), ('true', 'stockade_stub_tip')):
            parts.append({'when': {'AND': [{side: 'true'}, cross, {'tip': t}]}, 'apply': m(model, y=y)})
    wj('blockstates/stockade.json', {'multipart': parts})


# ================================================================ burning stake models
ST_TEX = {'side': f'{NS}:block/burning_stake_side', 'foot': f'{NS}:block/burning_stake_foot', 'end': f'{NS}:block/burning_stake_end',
          'iron': f'{NS}:block/burning_stake_iron', 'particle': f'{NS}:block/burning_stake_side'}


def timber(y0, y1, tex='#side', inset=0.0, ends=('up', 'down')):
    a, b = 5 + inset, 11 - inset
    f = {}
    for d, u in (('north', 5), ('east', 0), ('south', 10), ('west', 3)):          # a different strip of grain on each face
        f[d] = {'texture': tex, 'uv': [u, r3(16 - y1), r3(u + (b - a)), r3(16 - y0)]}
    for d in ends:
        f[d] = {'texture': '#end', 'uv': [r3(a), r3(a), r3(b), r3(b)]}
    return {'from': [a, y0, a], 'to': [b, y1, b], 'faces': f}


def shackle():
    hoop = {'from': [4.5, 11, 4.5], 'to': [11.5, 13, 11.5], 'faces': {
        'north': {'texture': '#iron', 'uv': [0, 0, 7, 2]}, 'south': {'texture': '#iron', 'uv': [8, 0, 15, 2]},
        'east': {'texture': '#iron', 'uv': [4, 0, 11, 2]}, 'west': {'texture': '#iron', 'uv': [9, 0, 16, 2]},
        'up': {'texture': '#iron', 'uv': [0, 2, 7, 3]}, 'down': {'texture': '#iron', 'uv': [0, 3, 7, 4]}}}
    patch = lambda: {'texture': '#iron', 'uv': [8, 5, 10, 6]}
    staple = {'from': [7, 11.5, 3.25], 'to': [9, 12.5, 4.5], 'faces': {d: patch() for d in ('north', 'east', 'west', 'up', 'down')}}
    # the chain hangs 9 px from the staple: two crossed planes like a vanilla chain
    a = {'from': [6.5, 3, 3.25], 'to': [9.5, 12, 3.25], 'shade': False, 'faces': {
        'north': {'texture': '#iron', 'uv': [3, 5, 0, 14]}, 'south': {'texture': '#iron', 'uv': [0, 5, 3, 14]}}}
    b = {'from': [8, 3, 1.75], 'to': [8, 12, 4.75], 'shade': False, 'faces': {
        'west': {'texture': '#iron', 'uv': [4, 5, 7, 14]}, 'east': {'texture': '#iron', 'uv': [7, 5, 4, 14]}}}
    return [hoop, staple, a, b]


def stake_models():
    def st(name, elements, cutout=False):
        m = {'parent': 'minecraft:block/block', 'textures': ST_TEX, 'elements': elements}
        if cutout:
            m['render_type'] = 'minecraft:cutout'
        wj(f'models/block/{name}.json', m)
    st('burning_stake_shaft', [timber(0, 16)])
    st('burning_stake_foot', [timber(0, 16, tex='#foot')])
    st('burning_stake_shackle', [timber(0, 16)] + shackle(), cutout=True)
    # blunt top: the last pixel is trimmed half a pixel all round (a worn, chamfered end)
    st('burning_stake_top', [timber(0, 15, ends=('up', 'down')), timber(15, 16, inset=0.5, ends=('up',))])
    wj('models/item/burning_stake.json', {'parent': f'{NS}:block/burning_stake_shackle'})
    wj('blockstates/burning_stake.json', {'variants': {f'part={p}': {'model': f'{NS}:block/burning_stake_{p}'}
                                                       for p in ('foot', 'shackle', 'shaft', 'top')}})


point_texture()
rope_texture()
stake_textures()
palisade_models()
stake_models()
print('phase 10-4 look: palisade and burning stake blockstates, models and textures written')
