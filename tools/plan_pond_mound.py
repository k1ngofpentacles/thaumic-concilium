"""Phase 5d, the livingrock mound round the mana pond: a PLAN in Python (docs/design/5d-pond-look.md, "Livingrock mound").

Not a generator and not in gen_all.py: it ports world/PondShape.java, adds the proposed mound rule, and prints both on the
same simulated craters the pond shape was proved on, so the rule can be judged before any Java is written.
Run: python -I tools/plan_pond_mound.py

ROUND 2 (user, 2026-11-23, after the pond3_ shots): a rounder 13-source pool (still level, still reading the crater floor); the
mound stands 1 above the pool's floor block (round 1: 2); a wider, uneven rim (mostly 2-3); a weathered contour of livingrock
slabs and stairs; moss and cracks creeping up from the grass (mossy / cracked livingrock bricks, moss blocks, moss carpet and
grass tongues densest where stone meets grass, thinning toward the water, none within reach of the water); flora on the lower
slopes. Round 1 is kept here untouched (mound_r1 and the R1 pool settings) so the preview page can show the two together.
tools/preview/export_mound.py imports this file for the preview page: nothing is retyped there.

Block ids (verified in reference/src/botania-revived, BotaniaBlocks.java registry and LibBlockNames.java):
  botania:livingrock  botania:livingrock_slab (SlabBlock: type, waterlogged)  botania:livingrock_stairs (StairBlock: facing, half,
  shape, waterlogged)  botania:mossy_livingrock_bricks  botania:cracked_livingrock_bricks (no stairs or slab for the cracked brick)
  minecraft:moss_block  minecraft:moss_carpet.
In this plan a stair's `low` is the direction its low step points (outward, away from the pool); Minecraft's `facing` property
points toward the HIGH side, so facing = the opposite of low.
"""
import math, sys

M64 = (1 << 64) - 1


def _mul(a, b): return (a * b) & M64
def _j(v): return v & M64          # a Java long as an unsigned 64-bit pattern


def crater_hash(dx, dz, seed):     # the stand-in for VesselCrater.hash used in the cycle 4d shape samples
    v = _j(dx * 73856093) ^ _j(dz * 19349663) ^ 83492791 ^ _j(seed)
    v = _mul(v ^ (v >> 13), 1274126177)
    return ((v ^ (v >> 16)) & 0xFFFF) / 65535.0


def unit(dx, dz, seed, salt=0):    # PondShape.unit, with a salt for the mound's own rolls
    v = _j(dx * 73856093) ^ _j(dz * 19349663) ^ 0x9E3779B97F4A7C15 ^ _j(seed) ^ _j(salt * 83492791)
    v = _mul(v ^ (v >> 13), 1274126177)
    v = _mul(v ^ (v >> 29), 0xBF58476D1CE4E5B9)
    return ((v ^ (v >> 32)) & 0xFFFF) / 65535.0


def make_cut(R, seed):
    rim = R * (1.0 + 0.25 * crater_hash(7, 7, seed))
    def cut(dx, dz):
        r = math.hypot(dx, dz)
        if r >= rim:
            return 0
        d = 4.0 * (1.0 - (r / rim) ** 2)
        return int(math.floor(max(0.0, min(4.0, d + (0.0 if r < 1.5 else (crater_hash(dx, dz, seed) - 0.5) * 0.8))) + 0.5))
    return cut


# ---------------------------------------------------------------- world/PondShape.java, as built (round 1), and round 2
SIZE, SHALLOW, COVE, LONG, CORNER, REACH = 13, 1.25, 10.0, 100.0, 3.0, 4
ROUND_R1, ROUGH_R1 = 0.5, 1.0     # round 1, as built: ROUND pulls cells toward the pool's middle, ROUGH is the random wobble
ROUND_R2, ROUGH_R2 = 1.6, 0.35    # round 2: pulled in harder and wobbled less, so the outline is closer to a circle (SHALLOW kept: the floor still decides)


def pond_with(cut, seed, rnd, rough):
    sx = sz = 0
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if cut(dx, dz) > cut(sx, sz):
                sx, sz = dx, dz
    deep = cut(sx, sz)
    cells = [(sx, sz)]
    while len(cells) < SIZE:
        xs = [c[0] for c in cells]; zs = [c[1] for c in cells]
        mx, mz = sum(xs) / len(cells), sum(zs) / len(cells)
        best, best_cost = None, 1e18
        for dx in range(sx - REACH, sx + REACH + 1):
            for dz in range(sz - REACH, sz + REACH + 1):
                if (dx, dz) in cells:
                    continue
                touching = sum(1 for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)) if (dx + a, dz + b) in cells)
                if not touching:
                    continue
                wide = max(max(xs), dx) - min(min(xs), dx) + 1
                long_ = max(max(zs), dz) - min(min(zs), dz) + 1
                cost = (SHALLOW * max(0, deep - cut(dx, dz)) + rnd * ((dx - mx) ** 2 + (dz - mz) ** 2) + rough * unit(dx, dz, seed)
                        - (COVE if touching >= 3 else 0) + (LONG if abs(wide - long_) > 1 else 0)
                        + (CORNER if wide >= 3 and long_ >= 3 and (dx <= min(xs) or dx >= max(xs)) and (dz <= min(zs) or dz >= max(zs)) else 0))
                if cost < best_cost:
                    best, best_cost = (dx, dz), cost
        cells.append(best)
    level, most = deep, 0
    for c in range(deep, -1, -1):
        n = sum(1 for cell in cells if cut(*cell) == c)
        if n > most:
            most, level = n, c
    return level, cells


# round 2 picks, per crater seed, among varied shapes weighted toward the round one (user, 2026-11-23): the roundest settings with
# this chance, otherwise a middling or a round-1-style (varied) shape. Chosen so the roundest shape is the most common but stays under 40%.
VARIETY = ((0.33, ROUND_R2, ROUGH_R2), (0.34, 1.0, 0.8), (0.33, ROUND_R1, ROUGH_R1))   # (chance, ROUND, ROUGH)


def pond(cut, seed):
    roll, acc = unit(9, 9, seed, 21), 0.0
    for p, rnd, rough in VARIETY:
        acc += p
        if roll < acc:
            break
    return pond_with(cut, seed, rnd, rough)


def pond_r1(cut, seed):
    return pond_with(cut, seed, ROUND_R1, ROUGH_R1)


# ---------------------------------------------------------------- ROUND 1 mound rule (unchanged, kept for the comparison)
HEIGHT = 2
WIDTH_MIN = 1.4
WIDTH_MAX = 3.4
EDGE_JITTER = 0.45
STEP_BACK = 0.9
EDGE_BAND = 0.9
P_GRASS = 0.30
P_SLAB = 0.25
P_MOSSY = 0.22
P_CRACKED = 0.12


def mound_r1(cut, seed, level, cells):
    """-> {(dx, dz): (top, kind, added)}: top = blocks above the pool's floor level (1 or 2), kind in R m c s g; only columns that get a block."""
    pool = set(cells)
    mx = sum(c[0] for c in cells) / len(cells)
    mz = sum(c[1] for c in cells) / len(cells)
    p1, p2, p3 = (unit(1, 0, seed, 11) * 6.283, unit(2, 0, seed, 11) * 6.283, unit(3, 0, seed, 11) * 6.283)
    def width(angle):
        n = 0.5 + 0.5 * (0.5 * math.sin(2 * angle + p1) + 0.3 * math.sin(3 * angle + p2) + 0.2 * math.sin(5 * angle + p3))
        return WIDTH_MIN + (WIDTH_MAX - WIDTH_MIN) * n
    out = {}
    for dx in range(-9, 10):
        for dz in range(-9, 10):
            if (dx, dz) in pool:
                continue
            d = min(math.hypot(dx - a, dz - b) for a, b in pool)
            w = width(math.atan2(dz - mz, dx - mx))
            touches = d < 1.5
            edge = w + (unit(dx, dz, seed, 12) * 2 - 1) * EDGE_JITTER
            if not touches and d > edge:
                continue
            top = HEIGHT if touches or d <= w - STEP_BACK else HEIGHT - 1
            added = top - (level - cut(dx, dz))
            if added < 0:
                continue
            kind = 'R'
            if not touches:
                is_edge = d > edge - EDGE_BAND
                roll = unit(dx, dz, seed, 13)
                if is_edge and roll < P_GRASS:
                    kind = 'g'
                elif is_edge and top == HEIGHT - 1 and added > 0 and roll < P_GRASS + P_SLAB:
                    kind = 's'
            if kind == 'g' and added == 0:
                continue
            if kind == 'R' and not touches and d > edge - EDGE_BAND:
                roll = unit(dx, dz, seed, 14)
                kind = 'm' if roll < P_MOSSY else 'c' if roll < P_MOSSY + P_CRACKED else 'R'
            out[(dx, dz)] = (top, kind, added)
    return out


# ---------------------------------------------------------------- ROUND 2 mound rule
TOP = 1              # the mound's top stands this many blocks above the floor block at the pool; the water lies in this layer, the bed is the floor block
W2_MIN = 2.0         # rim width, blocks, by direction round the pool's middle: mostly 2-3, a few narrow and wide parts
W2_MAX = 3.9
JITTER2 = 0.55       # per-column wobble of the edge, blocks
LUMP2 = 0.5          # slow lumps of the edge along its length (the weathered, irregular contour)
T_LOW = 1.4          # t = (edge - distance to the water): 0 < t <= T_LOW is the weathered lower slope (tongues, stairs, slabs, bricks)
APRON = 1.6          # outside the edge, this far, moss blocks and flora creep off the stone onto the grass
FAR = 2.0            # moss, carpet and bricks stay at least this far from every water cell (the 8 neighbours are plain rock, so none touch the water)
P_TURF = 0.30        # a lower-slope column is a grass tongue (a grass block at the mound's height), fading toward the water
P_STAIR = 0.28       # ... a livingrock stair, low side outward
P_SLAB2 = 0.28       # ... a livingrock slab
P_BRICK = 0.62       # a full rock column at t = 0 is a mossy or cracked brick; falls as exp(-t / BRICK_DECAY)
BRICK_DECAY = 1.0
P_MOSSY_OF_BRICK = 0.62   # of those bricks, mossy; the rest cracked
P_CARPET = 0.45      # moss carpet on a full rock top, at t = 0; falls as exp(-t / CARPET_DECAY)
CARPET_DECAY = 1.5
P_APRON_MOSS = 0.34  # a moss block replaces the grass block at the edge (t = 0), fading to nothing at t = -APRON
P_APRON_FLORA = 0.30 # a flora marker on the grass beside the stone, likewise
P_TURF_FLORA = 0.75  # a flora marker on a grass tongue
SYM = {'e': (1, 0), 'w': (-1, 0), 's': (0, 1), 'n': (0, -1)}   # +x east, +z south, as in Minecraft
OPP = {'e': 'w', 'w': 'e', 's': 'n', 'n': 's'}


def mound(cut, seed, level, cells):
    """-> {(dx, dz): {'top', 'kind', 'added', 'carpet', 'flora', 'low', 'd', 't'}}; only columns that change.
    top = blocks above the floor block at the pool (1 here), added = blocks put on that column's own ground (0 = its top block is replaced),
    kind: R livingrock, m mossy brick, c cracked brick, s slab, t stair (low side `low`), g grass tongue, o moss block (replaces the
    ground's top block), '-' flora only; carpet = moss carpet on top; flora = a small plant on top (short grass, fern or flower)."""
    pool = set(cells)
    mx = sum(c[0] for c in cells) / len(cells)
    mz = sum(c[1] for c in cells) / len(cells)
    p1, p2, p3 = (unit(1, 0, seed, 11) * 6.283, unit(2, 0, seed, 11) * 6.283, unit(3, 0, seed, 11) * 6.283)
    p4, p5 = unit(4, 0, seed, 11) * 6.283, unit(5, 0, seed, 11) * 6.283
    def width(angle):
        n = 0.5 + 0.5 * (0.5 * math.sin(2 * angle + p1) + 0.3 * math.sin(3 * angle + p2) + 0.2 * math.sin(5 * angle + p3))
        return W2_MIN + (W2_MAX - W2_MIN) * n
    out = {}
    for dx in range(-10, 11):
        for dz in range(-10, 11):
            if (dx, dz) in pool:
                continue
            d = min(math.hypot(dx - a, dz - b) for a, b in pool)
            if d > W2_MAX + LUMP2 + JITTER2 + APRON + 0.1:
                continue
            w = width(math.atan2(dz - mz, dx - mx))
            edge = w + LUMP2 * math.sin(0.85 * dx + p4) * math.sin(0.75 * dz + p5) + (unit(dx, dz, seed, 12) * 2 - 1) * JITTER2
            touches = d < 1.5                                        # the eight neighbours of the water: plain full rock, always, so it holds the water in
            t = edge - d
            ground = level - cut(dx, dz)                             # this column's ground top, in blocks above the floor block at the pool
            added = TOP - ground
            near_water = d < FAR
            if touches:
                if added >= 0:
                    out[(dx, dz)] = dict(top=TOP, kind='R', added=added, carpet=False, flora=False, low=None, d=d, t=max(t, 2.0))
                continue
            if t <= 0:                                               # beyond the stone: the apron, where moss and flora creep onto the grass
                if t < -APRON or near_water:
                    continue
                f = 1 + t / APRON                                    # 1 at the edge, 0 at the apron's outer limit
                if unit(dx, dz, seed, 16) < P_APRON_MOSS * f:
                    out[(dx, dz)] = dict(top=ground, kind='o', added=0, carpet=False, flora=unit(dx, dz, seed, 18) < 0.25, low=None, d=d, t=t)
                elif unit(dx, dz, seed, 17) < P_APRON_FLORA * f:
                    out[(dx, dz)] = dict(top=ground, kind='-', added=0, carpet=False, flora=True, low=None, d=d, t=t)
                continue
            if added < 0:
                continue                                             # the ground is already higher: the mound runs into the slope
            kind, low, flora, carpet = 'R', None, False, False
            if t <= T_LOW:                                           # the weathered lower slope
                u = unit(dx, dz, seed, 13)
                pt = P_TURF * (1 - 0.5 * t / T_LOW)
                if u < pt:
                    kind = 'g'
                    flora = unit(dx, dz, seed, 19) < P_TURF_FLORA
                elif u < pt + P_STAIR:
                    kind = 't'
                elif u < pt + P_STAIR + P_SLAB2:
                    kind = 's'
            if kind == 'g' and added == 0:
                continue                                             # turf where the ground is: nothing changes
            if kind == 't':
                vx, vz = dx - mx, dz - mz
                low = ('e' if vx > 0 else 'w') if abs(vx) >= abs(vz) else ('s' if vz > 0 else 'n')
            if kind == 'R' and not near_water and unit(dx, dz, seed, 14) < P_BRICK * math.exp(-max(t, 0) / BRICK_DECAY):
                kind = 'm' if unit(dx, dz, seed, 15) < P_MOSSY_OF_BRICK else 'c'
            if kind in 'Rmc' and not near_water and unit(dx, dz, seed, 20) < P_CARPET * math.exp(-max(t, 0) / CARPET_DECAY):
                carpet = True
            out[(dx, dz)] = dict(top=TOP, kind=kind, added=added, carpet=carpet, flora=flora, low=low, d=d, t=t)
    return out


STRUCT = 'Rmcstg'      # kinds that are a block of the mound itself (not an apron block or a flora-only column)


def counts(m):
    c = {k: 0 for k in 'Rmcstgo-'}
    for v in m.values():
        c[v['kind']] += 1
    return dict(columns=sum(1 for v in m.values() if v['kind'] in STRUCT), blocks=sum(v['added'] for v in m.values()),
                rock=c['R'], mossy=c['m'], cracked=c['c'], slabs=c['s'], stairs=c['t'], turf=c['g'], moss_blocks=c['o'],
                carpets=sum(1 for v in m.values() if v['carpet']), flora=sum(1 for v in m.values() if v['flora']), flora_only=c['-'])


def counts_r1(m):
    return dict(columns=len(m), blocks=sum(v[2] for v in m.values()), rock=sum(1 for v in m.values() if v[1] == 'R'),
                mossy=sum(1 for v in m.values() if v[1] == 'm'), cracked=sum(1 for v in m.values() if v[1] == 'c'),
                slabs=sum(1 for v in m.values() if v[1] == 's'), turf=sum(1 for v in m.values() if v[1] == 'g'))


def violations(m, cells):
    """Moss, carpet, bricks and moss blocks anywhere within FAR of the water, or a hole in the ring of plain rock: each is a bug."""
    bad = 0
    pool = set(cells)
    for (dx, dz), v in m.items():
        d = min(math.hypot(dx - a, dz - b) for a, b in pool)
        if d < FAR and (v['kind'] in 'mco-' or v['carpet'] or v['flora']):
            bad += 1
    for a, b in pool:
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                n = (a + dx, b + dz)
                if n not in pool and n in m and m[n]['kind'] != 'R':
                    bad += 1
    return bad


def pool_shape(cells):
    """Roundness of the pool: mean deviation of the cells' distances from the middle, relative to their mean (0 = a perfect ring), the bounding box, and the exposed edges."""
    mx = sum(c[0] for c in cells) / len(cells); mz = sum(c[1] for c in cells) / len(cells)
    ds = [math.hypot(c[0] - mx, c[1] - mz) for c in cells]
    mean = sum(ds) / len(ds)
    perim = sum(1 for c in cells for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)) if (c[0] + a, c[1] + b) not in cells)
    xs = [c[0] for c in cells]; zs = [c[1] for c in cells]
    return dict(round=sum(abs(x - mean) for x in ds) / len(ds) / mean, w=max(xs) - min(xs) + 1, h=max(zs) - min(zs) + 1, perimeter=perim)


def at_level(cut, cells, level):
    return sum(1 for c in cells if cut(*c) == level)


def show(R, seed, quiet=False):
    cut = make_cut(R, seed)
    level, cells = pond(cut, seed)
    m = mound(cut, seed, level, cells)
    pool = set(cells)
    c = counts(m)
    sh = pool_shape(cells)
    if quiet:
        return m, cells
    print(f'crater radius {R}, seed {seed}: pool level = cut {level} ({at_level(cut, cells, level)} of 13 on it), pool {sh["w"]}x{sh["h"]}, roundness {sh["round"]:.3f}; mound {c["columns"]} columns, '
          f'{c["blocks"]} blocks added: {c["rock"]} rock, {c["mossy"]} mossy, {c["cracked"]} cracked, {c["slabs"]} slabs, {c["stairs"]} stairs, {c["turf"]} turf; '
          f'on it {c["moss_blocks"]} moss blocks, {c["carpets"]} carpets, {c["flora"]} flora; violations {violations(m, cells)}')
    arrow = {'e': '>', 'w': '<', 's': 'v', 'n': '^'}
    for dz in range(-7, 8):
        a, b, g = [], [], []
        for dx in range(-7, 8):
            if (dx, dz) in pool:
                a.append('~'); b.append('~'); g.append('~')
            elif (dx, dz) in m:
                v = m[(dx, dz)]
                k = v['kind']
                a.append({'R': '#', 'm': 'M', 'c': 'C', 's': '_', 't': arrow.get(v['low'], '?'), 'g': '"', 'o': ',', '-': '.'}[k])
                b.append('%' if v['carpet'] else '*' if v['flora'] else '.')
                g.append('.')
            else:
                a.append('.'); b.append('.'); g.append('.')
        print('   ' + ' '.join(a) + '      ' + ' '.join(b))
    return m, cells


def show_r1(R, seed):
    cut = make_cut(R, seed)
    level, cells = pond_r1(cut, seed)
    m = mound_r1(cut, seed, level, cells)
    pool = set(cells)
    c = counts_r1(m)
    print(f'ROUND 1: crater radius {R}, seed {seed}: pool level = cut {level}, roundness {pool_shape(cells)["round"]:.3f}; mound {c["columns"]} columns, {c["blocks"]} blocks, {c["turf"]} turf, {c["slabs"]} slabs, {c["mossy"]} mossy, {c["cracked"]} cracked')
    for dz in range(-7, 8):
        a = []
        for dx in range(-7, 8):
            if (dx, dz) in pool: a.append('~')
            elif (dx, dz) in m:
                top, kind, added = m[(dx, dz)]
                a.append({'R': '#' if top == HEIGHT else '=' if added == 0 else '+', 'm': 'M' if top == HEIGHT else 'm', 'c': 'C' if top == HEIGHT else 'c', 's': '_', 'g': '"'}[kind])
            else: a.append('.')
        print('   ' + ' '.join(a))


def stats(R, n=400):
    out = {}
    for tag in ('r1', 'r2'):
        cols = blocks = turf = bricks = 0
        rnd = lvl = 0.0
        perim = viol = 0
        hist = {}
        sub = [0] * 5
        for k in range(1, n + 1):
            seed = k * 7919
            cut = make_cut(R, seed)
            if tag == 'r1':
                level, cells = pond_r1(cut, seed); m = mound_r1(cut, seed, level, cells)
                cols += len(m); blocks += sum(v[2] for v in m.values()); turf += sum(1 for v in m.values() if v[1] == 'g')
                bricks += sum(1 for v in m.values() if v[1] in 'mc')
                present = lambda p: p in m
            else:
                level, cells = pond(cut, seed); m = mound(cut, seed, level, cells)
                c = counts(m)
                cols += c['columns']; blocks += c['blocks']; turf += c['turf']; bricks += c['mossy'] + c['cracked']
                sub[0] += c['slabs']; sub[1] += c['stairs']; sub[2] += c['moss_blocks']; sub[3] += c['carpets']; sub[4] += c['flora']
                viol += violations(m, cells)
                present = lambda p: p in m and m[p]['kind'] in STRUCT
            sh = pool_shape(cells)
            rnd += sh['round']; perim += sh['perimeter']; lvl += at_level(cut, cells, level)
            xs = [c[0] for c in cells]; zs = [c[1] for c in cells]
            my, mz = round(sum(zs) / 13), round(sum(xs) / 13)
            for ddx, ddz, x0, z0 in ((1, 0, max(xs), my), (-1, 0, min(xs), my), (0, 1, mz, max(zs)), (0, -1, mz, min(zs))):
                wdt = 0
                while present((x0 + ddx * (wdt + 1), z0 + ddz * (wdt + 1))):
                    wdt += 1
                hist[wdt] = hist.get(wdt, 0) + 1
        tot = sum(hist.values())
        out[tag] = dict(columns=cols / n, blocks=blocks / n, turf=turf / n, bricks=bricks / n, roundness=rnd / n, perimeter=perim / n, level_cells=lvl / n,
                        widths={w: round(100.0 * v / tot, 1) for w, v in sorted(hist.items())}, violations=viol,
                        slabs=sub[0] / n, stairs=sub[1] / n, moss_blocks=sub[2] / n, carpets=sub[3] / n, flora=sub[4] / n)
    return out


def sample(R, seed):
    """Everything the preview page draws for one crater, from this script's own functions: -> dict (JSON-friendly)."""
    cut = make_cut(R, seed)
    ground = [[-cut(dx, dz) for dx in range(-10, 11)] for dz in range(-10, 11)]        # absolute y of each column's top block, the crater lip = 0; row = dz, column = dx
    out = dict(R=R, seed=seed, ground=ground)
    level, cells = pond_r1(cut, seed)
    m = mound_r1(cut, seed, level, cells)
    out['r1'] = dict(floor_y=-level, pool=[list(c) for c in cells], mound=[[dx, dz, v[0], v[1], v[2]] for (dx, dz), v in sorted(m.items())], counts=counts_r1(m),
                     roundness=pool_shape(cells)['round'])
    level, cells = pond(cut, seed)
    m = mound(cut, seed, level, cells)
    out['r2'] = dict(floor_y=-level, pool=[list(c) for c in cells],
                     mound=[[dx, dz, v['top'], v['kind'], v['added'], 1 if v['carpet'] else 0, 1 if v['flora'] else 0, v['low'] or ''] for (dx, dz), v in sorted(m.items())],
                     counts=counts(m), violations=violations(m, cells), roundness=pool_shape(cells)['round'])
    return out


# the six craters of the preview and the ASCII samples: the same radius-10 and radius-5 craters as the round 1 proof
SAMPLES = [(10, 1 * 7919), (10, 2 * 7919), (10, 3 * 7919), (10, 4 * 7919), (5, 1 * 7919), (5, 2 * 7919)]

if __name__ == '__main__':
    print('ROUND 2.  Left: what stands there.  ~ pool   # livingrock   M mossy bricks   C cracked bricks   _ slab   ^ > v < stair (arrow = its low side, outward)')
    print('          " grass tongue (grass block raised to the mound height)   , moss block replacing the grass at the edge   . untouched')
    print('Right: on top of it.  % moss carpet   * flora (short grass, fern or flower)\n')
    for R, seed in SAMPLES:
        show(R, seed)
        print()
    print('ROUND 1 (kept for comparison; # full height  + one lower  = flush with the ground)')
    for R, seed in SAMPLES[:2]:
        show_r1(R, seed)
        print()
    for R in (5, 7, 10, 14):
        s = stats(R)
        a, b = s['r1'], s['r2']
        print(f'radius {R}, 400 craters:')
        print(f'   round 1: pool roundness {a["roundness"]:.3f}, perimeter {a["perimeter"]:.1f}, {a["level_cells"]:.1f} of 13 on the level; mound columns {a["columns"]:.1f}, blocks {a["blocks"]:.1f}, turf {a["turf"]:.1f}, bricks {a["bricks"]:.1f}; rim width along the axes (% of 4 axes) {a["widths"]}')
        print(f'   round 2: pool roundness {b["roundness"]:.3f}, perimeter {b["perimeter"]:.1f}, {b["level_cells"]:.1f} of 13 on the level; mound columns {b["columns"]:.1f}, blocks {b["blocks"]:.1f}, turf {b["turf"]:.1f}, bricks {b["bricks"]:.1f}, '
              f'slabs {b["slabs"]:.1f}, stairs {b["stairs"]:.1f}, moss blocks {b["moss_blocks"]:.1f}, carpets {b["carpets"]:.1f}, flora {b["flora"]:.1f}; violations {b["violations"]}; rim width (% of 4 axes) {b["widths"]}')
