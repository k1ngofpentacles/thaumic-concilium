#!/usr/bin/env python3
"""Landmark tree shapes for the Concilium Grove (phase 5d cycle 2d): veteran oak greatwoods and ghost gum silverwoods.

Writes src/main/resources/data/thaumicconcilium/grove_trees/landmarks.json:
  {"greatwood": [{"code", "cells", "tips", ...}, ...], "silverwood": [...]}
Each "code" is a Dynamic Trees JoCode (3-bit instructions, 0 down 1 up 2 north 3 south 4 west 5 east 6 fork 7 return, two per base-64
character; the walk starts ON the soil block, so the first instruction is 1). Both species are built at 1 block = 1 m. See
docs/design/5d-landmark-trees.md (and its "User answers"): the real trees inspire the branching structure.

Run from the module root (python -I tools/gen_landmark_trees.py, or via gen_all.py). Pure stdlib, fixed seeds: the output never changes
unless this file does. Every variant is decoded again and compared with the cells it was built from, the Dynamic Trees inflator and leaf
map are simulated, and the run fails when a number leaves its range.
"""
import json
import math
import os
import random
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', 'src', 'main', 'resources', 'data', 'thaumicconcilium', 'grove_trees', 'landmarks.json')

ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'
# Direction.values() order: down, up, north (-z), south (+z), west (-x), east (+x)
D = [(0, -1, 0), (0, 1, 0), (0, 0, -1), (0, 0, 1), (-1, 0, 0), (1, 0, 0)]

# dynamictrees:dark_oak leaf cluster, 7 x 5 x 7, centre (3, 1, 3), index y * 49 + z * 7 + x (LeafClusters.DARK_OAK, verified in reference/src)

def load_cluster():
    # the 245 hydration values of LeafClusters.DARK_OAK (extracted from the decompiled source once, kept here so the generator needs no reference/)
    return [int(v) for v in CLUSTER_TEXT.split()]


CLUSTER_TEXT = "0 0 0 0 0 0 0 0 0 1 1 1 0 0 0 1 0 0 0 1 0 0 1 0 0 0 1 0 0 1 0 0 0 1 0 0 0 1 1 1 0 0 0 0 0 0 0 0 0 0 0 1 1 1 0 0 0 1 2 2 2 1 0 1 2 3 4 3 2 1 1 2 4 0 4 2 1 1 2 3 4 3 2 1 0 1 2 2 2 1 0 0 0 1 1 1 0 0 0 0 0 0 0 0 0 0 0 1 1 1 0 0 0 1 2 2 2 1 0 0 1 2 4 2 1 0 0 1 2 2 2 1 0 0 0 1 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 1 1 0 0 0 0 1 2 1 0 0 0 0 1 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0"


def add(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


class Tree:
    def __init__(self):
        self.root = (0, 0, 0)
        self.parent = {}
        self.kids = {self.root: []}

    def attach_path(self, par, cells):
        """Add a path under par; an existing cell is shared only when it already hangs under the previous one. False on a loop."""
        prev = par
        for c in cells:
            if c in self.parent:
                if self.parent[c] != prev:
                    return False
                prev = c
                continue
            if c == self.root or c[1] < 1:
                return False
            self.parent[c] = prev
            self.kids.setdefault(prev, []).append(c)
            self.kids.setdefault(c, [])
            prev = c
        return True

    def cells(self):
        return list(self.parent)

    def tips(self):
        return [c for c in self.parent if not self.kids[c]]

    def clear(self):
        """No two cells touch unless one is the other's parent."""
        for c, p in self.parent.items():
            for d in D:
                n = add(c, d)
                if n in self.parent and n != p and self.parent[n] != c:
                    return False
        return True

    def depth(self):
        best = 0
        for c in self.parent:
            n = 0
            x = c
            while x in self.parent:
                x = self.parent[x]
                n += 1
            best = max(best, n)
        return best


def step_prefix(start, dx, dz, n):
    """n unit moves along the dominant horizontal axis of (dx, dz); returns the cells."""
    cells = []
    cur = start
    if abs(dx) >= abs(dz):
        d = (1 if dx >= 0 else -1, 0, 0)
    else:
        d = (0, 0, 1 if dz >= 0 else -1)
    for _ in range(n):
        cur = add(cur, d)
        cells.append(cur)
    return cells


def walk(a, b):
    """Monotone path a -> b (a excluded), one axis move at a time, the axes interleaved evenly."""
    tot = [abs(b[i] - a[i]) for i in range(3)]
    rem = tot[:]
    done = [0, 0, 0]
    sgn = [(b[i] > a[i]) - (b[i] < a[i]) for i in range(3)]
    cur = list(a)
    out = []
    while sum(rem):
        best = None
        for i in (1, 0, 2):
            if rem[i]:
                f = (done[i] + 0.5) / tot[i]
                if best is None or f < best[0] - 1e-9:
                    best = (f, i)
        i = best[1]
        cur[i] += sgn[i]
        done[i] += 1
        rem[i] -= 1
        out.append(tuple(cur))
    return out


def lead(a, b):
    """A side branch from a to b: two moves away along its dominant horizontal axis first (so it never hugs its parent), then walk."""
    dx, dz = b[0] - a[0], b[2] - a[2]
    n = min(2, max(abs(dx), abs(dz)))
    pre = step_prefix(a, dx, dz, n)
    start = pre[-1] if pre else a
    return pre + walk(start, b)


def encode(tree):
    out = []

    def emit(node):
        kids = tree.kids[node]
        for i, k in enumerate(kids):
            d = D.index((k[0] - node[0], k[1] - node[1], k[2] - node[2]))
            if i < len(kids) - 1:
                out.append(6)
                out.append(d)
                emit(k)
                out.append(7)
            else:
                out.append(d)
                emit(k)

    emit(tree.root)
    if len(out) % 2:
        out.append(7)
    return ''.join(ALPHABET[out[i] << 3 | out[i + 1]] for i in range(0, len(out), 2))


def decode(code):
    ins = []
    for ch in code:
        v = ALPHABET.index(ch)
        ins.append(v >> 3)
        ins.append(v & 7)
    cells = set()

    def fork(i, pos):
        while i < len(ins):
            c = ins[i]
            if c == 6:
                i = fork(i + 1, pos)
            elif c == 7:
                return i + 1
            else:
                pos = add(pos, D[c])
                cells.add(pos)
                i += 1
        return i

    fork(0, (0, 0, 0))
    return cells


# ---------------------------------------------------------------- simulations of what Dynamic Trees does with a code
def inflate(tree, tapering, max_radius=24):
    """InflatorNode: radius per cell (int, as stored), and the leaf map voxels."""
    cluster = load_cluster()
    stored = {}
    st = {'radius': 1.0, 'last': None, 'highest': None}
    leaf = {}
    cells = tree.parent

    def blit(pos):
        for y in range(5):
            for z in range(7):
                for x in range(7):
                    v = cluster[y * 49 + z * 7 + x]
                    if v:
                        q = (pos[0] + x - 3, pos[1] + y - 1, pos[2] + z - 3)
                        if v > leaf.get(q, 0):
                            leaf[q] = v

    def visit(cell, parent):
        st['radius'] = 1.0
        if st['highest'] is None and add(cell, D[1]) not in cells:
            st['highest'] = cell
        for d in range(6):
            n = add(cell, D[d])
            if n == parent or n not in cells:
                continue
            visit(n, cell)
        area = st['radius'] ** 2
        twig = True
        for d in range(6):
            n = add(cell, D[d])
            if n == parent:
                continue
            if n == st['last']:
                twig = False
            elif n in cells:
                area += stored[n] ** 2
        if twig:
            leaf[cell] = 16
            blit(cell)
            stored[cell] = 1
        else:
            r = math.sqrt(area) + tapering * 1.5
            r = min(r, max_radius)
            h = st['highest']
            in_trunk = h is not None and cell[0] == h[0] and cell[2] == h[2] and cell[1] <= h[1]
            if r > 8 and not in_trunk:
                r = 8.0
            r = max(r, 2.0)
            st['radius'] = r
            stored[cell] = int(math.floor(r))
            leaf[cell] = 32
        st['last'] = cell

    first = add(tree.root, D[1])
    visit(first, tree.root)
    return stored, leaf


def slim(tree, step=0.5, column_cap=14):
    """The gum trunk (DynamicTreesLandmarks.slim): r = min(cap, sqrt(sum child r^2) + step), cap 16 on the straight column from the root and 8 off it."""
    stored = {}
    st = {'radius': 1.0, 'last': None}
    cells = tree.parent
    topy = 0
    while (0, topy + 1, 0) in cells:
        topy += 1

    def visit(cell, parent):
        st['radius'] = 1.0
        for d in range(6):
            n = add(cell, D[d])
            if n == parent or n not in cells:
                continue
            visit(n, cell)
        area = st['radius'] ** 2
        twig = True
        for d in range(6):
            n = add(cell, D[d])
            if n == parent:
                continue
            if n == st['last']:
                twig = False
            elif n in cells:
                area += stored[n] ** 2
        if twig:
            stored[cell] = 1
        else:
            cap = column_cap if (cell[0] == 0 and cell[2] == 0 and cell[1] <= topy) else 8.0
            r = min(cap, math.sqrt(area) + step)
            st['radius'] = r
            stored[cell] = int(math.floor(r))
        st['last'] = cell

    visit(add(tree.root, D[1]), tree.root)
    return stored


def leaves_of(leaf, strict=False):
    """JoCode.smother (max 3) and the leaf cells (value & 15 != 0), clipped to the 23 x 32 x 23 leaf map."""
    m = smothered(leaf)
    return {k for k, v in m.items() if (v & 15) > (1 if strict else 0) and abs(k[0]) <= 11 and abs(k[2]) <= 11 and 1 <= k[1] <= 32}


def smothered(leaf):
    m = dict(leaf)
    xs = [k[0] for k in m]
    zs = [k[2] for k in m]
    ys = [k[1] for k in m]
    top = max(ys)
    for x in range(min(xs), max(xs) + 1):
        for z in range(min(zs), max(zs) + 1):
            count = 0
            for y in range(top, min(ys) - 1, -1):
                v = m.get((x, y, z), 0)
                if v == 0:
                    count = 0
                elif v & 15:
                    count += 1
                    if count > 3:
                        m[(x, y, z)] = 0
                elif v & 16:
                    count += 1
                    m[(x, y + 1, z)] = 4
    return m


def sky_fraction(leaves, branches, axis):
    """Share of the crown's side-view box (leaves and branches projected along `axis`) that shows sky."""
    pts = set()
    for p in leaves | branches:
        pts.add((p[0] if axis == 'z' else p[2], p[1]))
    lp = {(p[0] if axis == 'z' else p[2], p[1]) for p in leaves}
    x0 = min(a for a, _ in lp)
    x1 = max(a for a, _ in lp)
    y0 = min(b for _, b in lp)
    y1 = max(b for _, b in lp)
    total = (x1 - x0 + 1) * (y1 - y0 + 1)
    return 1.0 - len([1 for a, b in pts if x0 <= a <= x1 and y0 <= b <= y1]) / total


def tip_stats(tips):
    mind = 99.0
    nn = []
    for i, a in enumerate(tips):
        best = 99.0
        for j, b in enumerate(tips):
            if i != j:
                d = math.dist(a, b)
                best = min(best, d)
                if i < j:
                    mind = min(mind, d)
        nn.append(best)
    return mind, (sum(nn) / len(nn) if nn else 0.0)


# ---------------------------------------------------------------- oak
def build_oak(rng):
    t = Tree()
    n = rng.choice([5, 6])
    ys = []
    y = rng.choice([4, 5])
    for _ in range(n):
        ys.append(y)
        y += rng.choice([1, 1, 1, 1, 2])
    H = max(8, ys[-1] + rng.choice([0, 1]))
    if H > 10:
        return None
    prev = t.root
    for y in range(1, H + 1):
        c = (0, y, 0)
        t.attach_path(prev, [c])
        prev = c
    axes_order = rng.sample(range(4), 4)
    forks3 = set(rng.sample(range(n), 2 if rng.random() < 0.5 else 1))
    used_axes_high = set()
    for i in range(n):
        ya = ys[i]
        th = axes_order[i % 4] * math.pi / 2 + rng.uniform(-0.45, 0.45)
        if ya >= H - 1:
            used_axes_high.add(axes_order[i % 4])
        R = rng.uniform(7.2, 8.4)
        rise = rng.choice([0, 1]) if i < 2 else rng.choice([1, 2, 3])
        frac = rng.uniform(0.5, 0.6)
        A = (0, ya, 0)
        fx, fz = round(frac * R * math.cos(th)), round(frac * R * math.sin(th))
        if max(abs(fx), abs(fz)) < 3:
            return None
        pre = step_prefix(A, fx, fz, 2)
        F = (fx, ya + (rise // 2), fz)
        path = pre + walk(pre[-1], F) if pre[-1] != F else pre
        if not t.attach_path(A, path):
            return None
        Fcell = path[-1]
        spread = math.radians(rng.uniform(40, 70))
        offs = [-spread / 2, spread / 2] if i not in forks3 else [-spread / 2, 0.0, spread / 2]
        for off in offs:
            Rj = R * rng.uniform(0.95, 1.05)
            ex = max(-8, min(8, round(Rj * math.cos(th + off))))
            ez = max(-8, min(8, round(Rj * math.sin(th + off))))
            ey = ya + rise + rng.choice([0, 0, 1]) if i >= 2 else ya + rise
            ey = max(ey, Fcell[1])
            if (ex, ez) == (Fcell[0], Fcell[2]):
                return None
            sub = lead(Fcell, (ex, ey, ez))
            if not t.attach_path(Fcell, sub):
                return None
    # leaders: 2 or 3, leaving the column top sideways, rising to y 16-18
    top = (0, H, 0)
    nl = rng.choice([2, 3])
    # axis index 0..3 of a limb = east, south, west, north = D index 5, 3, 4, 2
    blocked = {{0: 5, 1: 3, 2: 4, 3: 2}[a] for a in used_axes_high}
    free = [a for a in range(2, 6) if a not in blocked]
    if len(free) < nl:
        return None
    axes = rng.sample(free, nl)
    tops = sorted(rng.sample([16, 17, 18], nl), reverse=True)
    if 18 not in tops and 17 not in tops:
        tops[0] = 17
    for a, ty in zip(axes, tops):
        d = D[a]
        first = add(top, d)
        lat = rng.randint(-2, 2)
        h = rng.randint(3, 5)
        perp = (d[2], 0, d[0])
        end = (d[0] * h + perp[0] * lat, ty, d[2] * h + perp[2] * lat)
        path = [first] + walk(first, end)
        if not t.attach_path(top, path):
            return None
        k = max(2, int(len(path) * 0.62))
        F = path[k]
        side = rng.choice([-1, 1])
        q = (F[0] + perp[0] * side * 3 + d[0], F[1] + 2, F[2] + perp[2] * side * 3 + d[2])
        sub = lead(F, q)
        if not t.attach_path(F, sub):
            return None
    return t


def check_oak(t, info, after=False):
    tips = t.tips()
    mind, nn = tip_stats(tips)
    stored, leaf = inflate(t, 0.55)
    lv = leaves_of(leaf)
    br = set(t.parent)
    w = [max(p[a] for p in lv) - min(p[a] for p in lv) + 1 for a in (0, 2)]
    topy = max(p[1] for p in lv)
    low = min(p[1] for p in lv)
    lvs = leaves_of(leaf, True)
    info['rowSkyX'] = round(row_sky(lvs, br, 'z'), 2)
    info['rowSkyZ'] = round(row_sky(lvs, br, 'x'), 2)
    if after:
        # after the twig fill: more tips, closer together; the shape limits stay
        ok = (t.depth() <= 30 and all(abs(c[0]) <= 8 and abs(c[2]) <= 8 and c[1] <= 26 for c in tips)
              and 19 <= topy <= 22 and min(w) >= 19 and max(w) <= 23 and low >= 3
              and stored[(0, 1, 0)] >= 20 and stored[(0, 4, 0)] >= 12)
    else:
        ok = _check_oak_base(t, tips, mind, nn, stored, topy, w, low)
    info.update(tips=len(tips), cells=len(br), depth=t.depth(), minTip=round(mind, 1), meanNN=round(nn, 1), widthX=w[0], widthZ=w[1], top=topy,
                lowLeaf=low, r1=stored[(0, 1, 0)], r4=stored[(0, 4, 0)], rMid=stored.get((0, 6, 0)), leaves=len(lv),
                skyX=round(sky_fraction(lv, br, 'z'), 2), skyZ=round(sky_fraction(lv, br, 'x'), 2),
                firstLimb=min(c[1] for c in br if (c[0], c[2]) != (0, 0)), trunkTop=max(c[1] for c in br if (c[0], c[2]) == (0, 0)))
    return ok


def _check_oak_base(t, tips, mind, nn, stored, topy, w, low):
    ok = (14 <= len(tips) <= 20 and mind >= 3.0 and 3.0 <= nn <= 5.5 and t.depth() <= 30
          and all(abs(c[0]) <= 8 and abs(c[2]) <= 8 and c[1] <= 26 for c in tips)
          and 19 <= topy <= 22 and min(w) >= 19 and max(w) <= 23 and low >= 3
          and stored[(0, 1, 0)] >= 20 and stored[(0, 4, 0)] >= 12)
    return ok


# ---------------------------------------------------------------- gum
HDIRS = [(0, 0, -1), (0, 0, 1), (-1, 0, 0), (1, 0, 0)]  # north, south, west, east
UPV = (0, 1, 0)
DOWNV = (0, -1, 0)


def _interleave(xs, zs):
    out = []
    i = j = 0
    while i < len(xs) or j < len(zs):
        if i < len(xs):
            out.append(xs[i])
            i += 1
        if j < len(zs):
            out.append(zs[j])
            j += 1
    return out


def stair(a, b, rng, first=None, vmax=2, clip=False):
    """Gum path a -> b (a excluded): every horizontal move is followed by 1..vmax vertical moves, so no two moves in a row go the same
    horizontal way and no vertical run is longer than vmax. Needs dy >= number of horizontal moves. None when it cannot."""
    dx, dy, dz = b[0] - a[0], b[1] - a[1], b[2] - a[2]
    hm = []
    if first is not None:
        hm.append(first)
        dx -= first[0]
        dz -= first[2]
    sx = 1 if dx > 0 else -1
    sz = 1 if dz > 0 else -1
    hm += _interleave([(sx, 0, 0)] * abs(dx), [(0, 0, sz)] * abs(dz))
    n = len(hm)
    if n == 0 or dy < n:
        return None
    gaps = [1] * n
    extra = dy - n
    while extra > 0:
        open_ = [i for i in range(n) if gaps[i] < vmax]
        if not open_:
            if clip:
                break
            return None
        gaps[rng.choice(open_)] += 1
        extra -= 1
    out = []
    cur = a
    for i, h in enumerate(hm):
        cur = add(cur, h)
        out.append(cur)
        for _ in range(gaps[i]):
            cur = add(cur, UPV)
            out.append(cur)
    return out


def _face_towards(a, b):
    dx, dz = b[0] - a[0], b[2] - a[2]
    if abs(dx) >= abs(dz) and dx:
        return (1 if dx > 0 else -1, 0, 0)
    if dz:
        return (0, 0, 1 if dz > 0 else -1)
    return None


def _snapshot(t):
    return (dict(t.parent), {k: list(v) for k, v in t.kids.items()})


def _restore(t, snap):
    t.parent = dict(snap[0])
    t.kids = {k: list(v) for k, v in snap[1].items()}


def _fork(t, rng, par, path, depth, vmax):
    """Attach path up to a fork cell F (about 40-55% along) and two arms that leave F on two different horizontal faces (Y fork). An arm that
    cannot be built is left out and a fork that would loop is undone, so this always attaches at least the plain path (False only if even that loops)."""
    snap = _snapshot(t)
    if len(path) >= 6:
        k = int(len(path) * rng.uniform(0.38, 0.55))
        while k > 2 and path[k - 1][1] == path[k - 2][1]:
            k -= 1
        F = path[k - 1]
        T = path[-1]
        fa = _face_towards(F, T)
        if fa is not None and t.attach_path(par, path[:k]):
            fb = rng.choice([h for h in HDIRS if h != fa and h != (-fa[0], 0, -fa[2])] + [(-fa[0], 0, -fa[2])])
            ob = rng.choice([2, 3])
            tb = (F[0] + fb[0] * ob, F[1] + ob + rng.choice([1, 2, 3]), F[2] + fb[2] * ob)
            built = 0
            for target, face in ((T, fa), (tb, fb)):
                sub = stair(F, target, rng, first=face, vmax=vmax, clip=True)
                if sub is None:
                    continue
                s2 = _snapshot(t)
                if depth > 0 and rng.random() < 0.75:
                    ok = _fork(t, rng, F, sub, depth - 1, vmax)
                else:
                    ok = t.attach_path(F, sub)
                if ok and t.clear():
                    built += 1
                else:
                    _restore(t, s2)
            if built == 2:
                return True
        _restore(t, snap)
    return t.attach_path(par, path)



def build_gum(rng):
    t = Tree()
    Hc = rng.choice([5, 6, 7])
    prev = t.root
    for y in range(1, Hc + 1):
        c = (0, y, 0)
        t.attach_path(prev, [c])
        prev = c
    top = (0, Hc, 0)
    nl = rng.choice([2, 3])
    faces = rng.sample(HDIRS, nl)
    t.mids = []
    t.Hc = Hc
    for i, f in enumerate(faces):
        if i == 0:
            # the dominant leader: steep, with one S-curve (the sideways direction reverses once), to y 17-19
            ty = rng.choice([17, 18, 19])
            o1 = rng.choice([2, 3])
            dy1 = o1 * rng.choice([2, 3])
            w1 = (top[0] + f[0] * o1, top[1] + dy1, top[2] + f[2] * o1)
            leg1 = stair(top, w1, rng, first=f, vmax=3)
            if leg1 is None:
                return None
            side = rng.choice([-1, 1])
            lat = (f[2] * side, 0, f[0] * side)
            back = rng.choice([1, 2])
            tx = w1[0] - f[0] * back + lat[0] * rng.choice([1, 2])
            tz = w1[2] - f[2] * back + lat[2] * rng.choice([1, 2])
            tgt = (tx, ty, tz)
            leg2 = stair(w1, tgt, rng, vmax=3)
            if leg2 is None:
                return None
            path = leg1 + leg2
            vmax = 3
        else:
            ty = rng.choice([11, 12, 13, 14, 15, 16])
            dy = ty - Hc
            o = rng.randint(max(3, -(-dy // 2)), min(dy, 6))
            lat = rng.choice([-1, 0, 0, 1])
            tgt = (top[0] + f[0] * o + (f[2] * lat), ty, top[2] + f[2] * o + (f[0] * lat))
            path = stair(top, tgt, rng, first=f, vmax=2)
            if path is None:
                return None
            vmax = 2
        t.mids.append(path[len(path) // 2])
        if not _fork(t, rng, top, path, 2, vmax):
            return None
    # up to two low outer tips droop: out, sideways, down (never straight down: that would touch the limb)
    low = sorted([c for c in t.tips() if c[1] <= 15 and max(abs(c[0]), abs(c[2])) >= 3], key=lambda c: c[1])[:2]
    for c in low:
        if rng.random() < 0.6:
            fdir = _face_towards((0, 0, 0), c) or HDIRS[0]
            g = rng.choice([h for h in HDIRS if h != fdir and h != (-fdir[0], 0, -fdir[2])])
            c1 = add(c, fdir)
            c2 = add(c1, g)
            c3 = add(c2, DOWNV)
            if not t.attach_path(c, [c1, c2, c3]):
                return None
    return t


def runs_ok(t):
    """No two moves in a row the same horizontal way; no vertical run over 3 off the column."""
    for c in t.tips():
        chain = []
        x = c
        while x in t.parent:
            chain.append(x)
            x = t.parent[x]
        chain.append(x)
        chain.reverse()
        moves = [(chain[i + 1][0] - chain[i][0], chain[i + 1][1] - chain[i][1], chain[i + 1][2] - chain[i][2]) for i in range(len(chain) - 1)]
        vrun = 0
        for i, m in enumerate(moves):
            if m[1] != 0:
                on_col = chain[i + 1][0] == 0 and chain[i + 1][2] == 0 and chain[i + 1][1] <= t.Hc
                vrun = 0 if on_col else (vrun + 1 if m[1] > 0 else 0)
                if vrun > 3:
                    return False
            else:
                vrun = 0
                if i and moves[i - 1] == m:
                    return False
    return True


def tip_kinds(tips):
    """(pairs, singles) when every tip is paired (one partner within 4.5, all others 5.5+) or alone (6+ from all), else None."""
    kind = []
    for i, a in enumerate(tips):
        ds = sorted(math.dist(a, b) for j, b in enumerate(tips) if j != i)
        near = [d for d in ds if d <= 4.6]
        if len(near) == 0 and ds[0] >= 5.0:
            kind.append('s')
        elif len(near) == 1 and ds[0] >= 2.5 and (len(ds) < 2 or ds[1] >= 4.6):
            kind.append('p')
        else:
            return None
    p = kind.count('p')
    return p // 2, kind.count('s')


def ascii_view(lv, br, axis):
    ys = [p[1] for p in lv | br]
    xs = [p[0 if axis == 'z' else 2] for p in lv | br]
    rows = []
    for y in range(max(ys), 0, -1):
        row = ''
        for x in range(min(xs), max(xs) + 1):
            bc = any((p[0 if axis == 'z' else 2], p[1]) == (x, y) for p in br)
            lc = any((p[0 if axis == 'z' else 2], p[1]) == (x, y) for p in lv)
            row += '#' if bc else ('o' if lc else '.')
        rows.append('%2d %s' % (y, row))
    return '\n'.join(rows)


def check_gum(t, info, after=False):
    tips = t.tips()
    stored, leaf = inflate(t, 0.8)
    slim_r = slim(t, 0.5, 14)
    lv = leaves_of(leaf)
    br = set(t.parent)
    if not lv:
        return False
    w = [max(p[a] for p in lv) - min(p[a] for p in lv) + 1 for a in (0, 2)]
    topy = max(p[1] for p in lv)
    low = min(p[1] for p in lv)
    kinds = tip_kinds(tips)
    tys = sorted(c[1] for c in tips)
    cx = sum(p[0] for p in lv) / len(lv)
    cz = sum(p[2] for p in lv) / len(lv)
    skyx = sky_fraction(lv, br, 'z')
    skyz = sky_fraction(lv, br, 'x')
    off_max = max((r for c, r in slim_r.items() if not (c[0] == 0 and c[2] == 0 and c[1] <= t.Hc)), default=0)
    mid_r = min((slim_r[m] for m in t.mids if m in slim_r), default=0)
    conds = {
        "tips": 7 <= len(tips) <= 10,
        "kinds": kinds is not None and 1 <= kinds[0] <= 3 and 2 <= kinds[1] <= 5,
        "tipy": tys[0] >= 11 and tys[-1] <= 19 and len(set(tys)) >= 3 and tys[-1] - tys[0] >= 7,
        "top": 20 <= topy <= 23,
        "width": 15 <= max(w) <= 19 and min(w) >= 12,
        "low": 8 <= low <= 11,
        "reach": all(max(abs(c[0]), abs(c[2])) <= 8 for c in tips) and t.depth() <= 30,
        "centroid": 1.5 <= math.hypot(cx, cz) <= 4.5,
        "sky": skyx >= 0.35 and skyz >= 0.35,
        "depth": (topy - low + 1) >= 0.5 * topy,
        "radius": mid_r >= 4 and 13 <= slim_r[(0, 1, 0)] <= 16 and off_max <= 8,
        "runs": runs_ok(t),
    }
    if after:
        for k in ("tips", "kinds", "tipy", "centroid"):
            conds[k] = True
    info["fails"] = [k for k, v in conds.items() if not v]
    lvs = leaves_of(leaf, True)
    info["rowSkyX"] = round(row_sky(lvs, br, "z"), 2)
    info["rowSkyZ"] = round(row_sky(lvs, br, "x"), 2)
    ok = not info["fails"]
    info.update(tips=len(tips), pairs=kinds[0] if kinds else -1, singles=kinds[1] if kinds else -1, cells=len(br), depth=t.depth(), widthX=w[0], widthZ=w[1], top=topy,
                lowLeaf=low, tipYs=tys, centroid=round(math.hypot(cx, cz), 1), nativeR1=stored[(0, 1, 0)], slimR1=slim_r[(0, 1, 0)], colTopR=slim_r[(0, t.Hc, 0)],
                midLeaderR=mid_r, leaves=len(lv), skyX=round(skyx, 2), skyZ=round(skyz, 2), firstLimb=t.Hc, trunkTop=t.Hc)
    if ok:
        info['_view'] = ascii_view(lv, br, 'z') + '\n' + ascii_view(lv, br, 'x')
    return ok


# gum attempts found by the full search (seed 23), tried first so a run takes seconds; the search still runs if one stops passing
KNOWN_GUM = [37540, 86768, 116232]


def _spans(leaves, idx):
    """(rows, cols): per y the span of leaf positions along the view axis, and per view position the span of y."""
    rows = {}
    cols = {}
    for p in leaves:
        lo, hi = rows.get(p[1], (p[idx], p[idx]))
        rows[p[1]] = (min(lo, p[idx]), max(hi, p[idx]))
        lo, hi = cols.get(p[idx], (p[1], p[1]))
        cols[p[idx]] = (min(lo, p[1]), max(hi, p[1]))
    return rows, cols


def _inside(sp, a, y):
    rows, cols = sp
    return y in rows and a in cols and rows[y][0] <= a <= rows[y][1] and cols[a][0] <= y <= cols[a][1]


def row_sky(leaves, branches, axis):
    """Sky inside the crown silhouette: the cells of a side view that lie within the leaves' span on their row AND on their column (so the empty
    corners of a dome are outside), not filled by a leaf or a branch, as a share of those cells. (sky_fraction counts the whole box.)"""
    idx = 0 if axis == 'z' else 2
    pts = {(p[idx], p[1]) for p in leaves | branches}
    sp = _spans(leaves, idx)
    total = empty = 0
    for y, (lo, hi) in sp[0].items():
        for a in range(lo, hi + 1):
            if _inside(sp, a, y):
                total += 1
                empty += (a, y) not in pts
    return empty / total if total else 0.0


def cluster_offsets():
    cl = load_cluster()
    return [(x - 3, y - 1, z - 3) for y in range(5) for z in range(7) for x in range(7) if cl[y * 49 + z * 7 + x]]


def _inbox(sp, a, y):
    rows, cols = sp
    return bool(rows) and min(rows) <= y <= max(rows) and min(cols) <= a <= max(cols)


def fill(t, rng, tapering, goal, limit, min_gain=3, mode='box'):
    """Twig fill: attach one-block twigs to interior branch cells where their leaf cluster closes the most gaps inside the crown silhouette, until
    the silhouette sky share is <= goal in both side views or `limit` twigs are added. A twig is an ordinary Dynamic Trees branch tip, so its leaves
    are the species' own cluster and Dynamic Trees sustains them like any other (nothing is placed that is not carried by a branch)."""
    offs = cluster_offsets()
    added = 0
    banned = set()
    while added < limit:
        stored, leaf = inflate(t, tapering)
        lv = leaves_of(leaf, True)
        br = set(t.parent)
        measure = (lambda ax: sky_fraction(lv, br, ax)) if mode == 'box' else (lambda ax: row_sky(lv, br, ax))
        inside = _inbox if mode == 'box' else _inside
        if max(measure('z'), measure('x')) <= goal:
            break
        spans = {'x': _spans(lv, 0), 'z': _spans(lv, 2)}
        full = lv | br
        px = {(p[0], p[1]) for p in full}
        pz = {(p[2], p[1]) for p in full}
        tips = t.tips()
        best = None
        for c in sorted(t.parent):
            if not t.kids[c] or (c[0] == 0 and c[2] == 0):
                continue
            for d in D[1:]:
                q = add(c, d)
                if q in t.parent or q in banned or q[1] < 4 or q[1] > 26 or max(abs(q[0]), abs(q[2])) > 8:
                    continue
                if any(add(q, e) in t.parent and add(q, e) != c for e in D):
                    continue
                if min(math.dist(q, tp) for tp in tips) < 2.0:
                    continue
                gain = 0
                sx = set()
                sz = set()
                for o in offs:
                    a = (q[0] + o[0], q[1] + o[1])
                    b = (q[2] + o[2], q[1] + o[1])
                    if a not in px and a not in sx and inside(spans['x'], a[0], a[1]):
                        gain += 1
                        sx.add(a)
                    if b not in pz and b not in sz and inside(spans['z'], b[0], b[1]):
                        gain += 1
                        sz.add(b)
                score = gain + rng.random() * 0.5
                if best is None or score > best[0]:
                    best = (score, q, c, gain)
        if best is None or best[3] < min_gain:
            break
        t.attach_path(best[2], [best[1]])
        # a twig must keep leaves (strict model: the thin outer fringe may age away) or Dynamic Trees rots it at once
        _, leaf2 = inflate(t, tapering)
        m2 = smothered(leaf2)
        q = best[1]
        # Dynamic Trees keeps a tip only if its face neighbours hold leaves worth at least 2 (BasicBranchBlock.checkForRot); keep a margin
        support = sum(m2.get(add(q, d), 0) & 15 for d in D)
        near = {q} if support >= 8 else set()
        if not near:
            del t.parent[q]
            del t.kids[q]
            t.kids[best[2]].remove(q)
            banned.add(q)
            continue
        added += 1
    return added


def make(name, build, check, count, seed, post=None, known=None):
    out = []
    attempt = 0
    todo = list(known or [])
    while len(out) < count:
        if todo:
            attempt = todo.pop(0)
        else:
            attempt = attempt + 1
            if attempt > 200000:
                sys.exit(f'{name}: no {count} variants in 200000 attempts (found {len(out)})')
        rng = random.Random(seed * 1000003 + attempt)
        t = build(rng)
        if t is None or not t.clear() or not t.tips():
            continue
        info = {}
        if not check(t, info):
            continue
        if post is not None:
            info['twigs'] = post(t, random.Random(seed * 7919 + attempt), info)
            info.pop('fails', None)
            if not t.clear() or not check(t, info, True):
                print(name, 'attempt', attempt, 'fails after the twig fill', info.get('fails'))
                continue
        code = encode(t)
        if decode(code) != set(t.parent):
            sys.exit(f'{name} attempt {attempt}: decode(encode) differs')
        info['code'] = code
        info['attempt'] = attempt
        out.append(info)
    return out


def oak_post(t, rng, info):
    return fill(t, rng, 0.55, 0.15, 36, mode='box')


def gum_post(t, rng, info):
    return fill(t, rng, 0.8, 0.04, 4, min_gain=6, mode='sil')


def main():
    oaks = make('greatwood', build_oak, check_oak, 4, 11, oak_post)
    gums = make('silverwood', build_gum, check_gum, 3, 23, gum_post, known=KNOWN_GUM)
    data = {'greatwood': [], 'silverwood': []}
    for key, lst in (('greatwood', oaks), ('silverwood', gums)):
        for info in lst:
            view = info.pop("_view", None)
            print(key, {k: v for k, v in info.items() if k != "code"})
            if view and "--view" in sys.argv:
                print(view)
            info = dict(info)
            info.pop("attempt")
            data[key].append(info)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=1)
        f.write('\n')
    print('wrote', os.path.relpath(OUT))


if __name__ == '__main__':
    main()
