"""Verdant Capital: how far the star can compact before its pieces touch, and the twist that lets it (phase 5d cycle 4b).

Run from anywhere: python -I tools/capital_clearance.py. It writes nothing; it prints the numbers the renderer's constants come from
(CapitalStar / VerdantCapitalRenderer; recorded in docs/agents/retro/5d-cycle4b-notes.md). About two minutes.

The star is two level-1 Sierpinski tetrahedra ("3D triforces", whole edge 1 here), one apex up and one apex down, on one vertical
axis. Each is 4 small solid tetrahedra (half-size copies drawn in to the corners). Two numbers pose it:
  spread e   every small tetrahedron stands e further out from the common centre along its own direction (its centre is then at
             R/2 + e, the radius of the capital's sphere);
  twist psi  every small tetrahedron is turned by psi about that same direction (its own radial axis, a three-fold axis of the
             piece, so 120 degrees is a whole turn and 60 the most it can differ from untwisted).
The two triforces turn opposite ways about the vertical axis, so every relative angle occurs.

Two independent tests:
  rings      an upper and a lower piece collide at some moment exactly when their volumes of revolution about the axis overlap,
             that is when at some height the ring [r_min, r_max] swept by one piece's cross-section meets the other's. Slices every
             piece at many heights with its exact cross-section polygon. Fast: used to search.
  rotation   turns the lower triforce against the upper through the relative turn in small steps and applies the separating-axis
             test to every upper-lower pair of tetrahedra (face normals of both and the cross products of their edges). The gap
             is the least separation found: positive = apart by at least that, zero = touching, negative = overlapping. The proof.
Pieces of one triforce never move against each other, but a twist can push them into each other, so they are tested too (once).
"""
import math

A = 1.0
H = A * math.sqrt(2.0 / 3.0)       # height of the whole tetrahedron
RHO = A / math.sqrt(3.0)           # radius of its base corners
R = A * math.sqrt(6.0) / 4.0       # distance of its corners from its centre
SLICES = 2000


def _sub(a, b): return (a[0] - b[0], a[1] - b[1], a[2] - b[2])
def _cross(a, b): return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])
def _dot(a, b): return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def corners():
    c = [(0.0, 0.75 * H, 0.0)]
    for k in range(3):
        a = k * 2.0 * math.pi / 3.0
        c.append((RHO * math.cos(a), -0.25 * H, RHO * math.sin(a)))
    return c


def _about(v, k, ang):
    """v turned by ang about the unit axis k through the origin (Rodrigues)."""
    c, s = math.cos(ang), math.sin(ang)
    kv = _cross(k, v); kd = _dot(k, v)
    return tuple(v[i] * c + kv[i] * s + k[i] * kd * (1.0 - c) for i in range(3))


def pieces(e, twist_deg=0.0):
    """(upper, lower): each 4 tetrahedra as 4 vertices. The lower triforce is the upper one mirrored top to bottom, twist and all."""
    cs = corners()
    up, down = [], []
    for i in range(4):
        n = math.sqrt(_dot(cs[i], cs[i]))
        d = tuple(v / n for v in cs[i])
        tet = []
        for j in range(4):
            v = _about(tuple((cs[i][k] + cs[j][k]) / 2.0 for k in range(3)), d, math.radians(twist_deg))
            tet.append(tuple(v[k] + d[k] * e for k in range(3)))
        up.append(tet)
        down.append([(v[0], -v[1], v[2]) for v in tet])
    return up, down


# ---------------------------------------------------------------- rings
def seg_axis_distance(p, q):
    """Nearest distance from the vertical axis to the segment p-q (both at one height): a 2D point-to-segment distance from the origin."""
    dx, dz = q[0] - p[0], q[2] - p[2]
    L = dx * dx + dz * dz
    t = 0.0 if L == 0.0 else max(0.0, min(1.0, -(p[0] * dx + p[2] * dz) / L))
    return math.hypot(p[0] + t * dx, p[2] + t * dz)


def ring(tet, y):
    """The ring (r_min, r_max) swept by the tetrahedron's cross-section at height y, or None above or below it."""
    pts = []
    for i in range(4):
        for j in range(i + 1, 4):
            ya, yb = tet[i][1], tet[j][1]
            if (ya - y) * (yb - y) < 0.0:
                t = (y - ya) / (yb - ya)
                pts.append(tuple(tet[i][d] + (tet[j][d] - tet[i][d]) * t for d in range(3)))
    if not pts:
        return None
    rmax = max(math.hypot(p[0], p[2]) for p in pts)
    # the polygon is convex: order its points round their middle; the axis is inside it when it is on one side of every edge
    cx = sum(p[0] for p in pts) / len(pts); cz = sum(p[2] for p in pts) / len(pts)
    pts.sort(key=lambda p: math.atan2(p[2] - cz, p[0] - cx))
    inside = len(pts) >= 3
    rmin = 1e9
    for k in range(len(pts)):
        p, q = pts[k], pts[(k + 1) % len(pts)]
        if (q[0] - p[0]) * (0.0 - p[2]) - (q[2] - p[2]) * (0.0 - p[0]) < 0.0:
            inside = False
        rmin = min(rmin, seg_axis_distance(p, q))
    return (0.0 if inside else rmin, rmax)


def rings_collide(e, twist_deg=0.0, slices=None):
    up, down = pieces(e, twist_deg)
    n = slices or SLICES
    lo = min(v[1] for t in up + down for v in t); hi = max(v[1] for t in up + down for v in t)
    for s in range(n + 1):
        y = lo + (hi - lo) * (s + 0.5) / (n + 1)
        ru = [r for r in (ring(t, y) for t in up) if r]
        rd = [r for r in (ring(t, y) for t in down) if r]
        for a in ru:
            for b in rd:
                if a[0] < b[1] - 1e-9 and b[0] < a[1] - 1e-9:
                    return True
    return False


# ---------------------------------------------------------------- rotation (separating axes)
def _axes(t):
    edges = [_sub(t[j], t[i]) for i in range(4) for j in range(i + 1, 4)]
    faces = [_cross(_sub(t[(f + 2) % 4], t[(f + 1) % 4]), _sub(t[(f + 3) % 4], t[(f + 1) % 4])) for f in range(4)]
    return edges, faces


def gap(t1, t2):
    e1, f1 = _axes(t1); e2, f2 = _axes(t2)
    best = -1e9
    for ax in f1 + f2 + [_cross(a, b) for a in e1 for b in e2]:
        n = math.sqrt(_dot(ax, ax))
        if n < 1e-12:
            continue
        p1 = [_dot(v, ax) / n for v in t1]; p2 = [_dot(v, ax) / n for v in t2]
        best = max(best, min(p2) - max(p1), min(p1) - max(p2))
    return best


def turned(tet, deg):
    c, s_ = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    return [(v[0] * c - v[2] * s_, v[1], v[0] * s_ + v[2] * c) for v in tet]


def same_side_gap(e, twist_deg):
    """The smallest gap between two pieces of one triforce (they are rigid to each other, so one look is enough)."""
    up, _ = pieces(e, twist_deg)
    return min(gap(up[i], up[j]) for i in range(4) for j in range(i + 1, 4))


def rotation_gap(e, twist_deg=0.0, step=0.25, span=360.0):
    """The smallest gap between any upper and any lower piece over the relative turn, and the angle it happens at."""
    up, down = pieces(e, twist_deg)
    worst, at = 1e9, 0.0
    for k in range(int(round(span / step))):
        ang = k * step
        low = [turned(t, ang) for t in down]
        for a in up:
            for b in low:
                g = gap(a, b)
                if g < worst:
                    worst, at = g, ang
    return worst, at


def e_min(twist_deg, slices=600):
    """The smallest spread at which nothing collides for this twist (rings between the triforces, a static test within one)."""
    def bad(e):
        return same_side_gap(e, twist_deg) < -1e-9 or rings_collide(e, twist_deg, slices)
    lo, hi = 0.0, 1.0
    assert not bad(hi)
    for _ in range(24):
        mid = (lo + hi) / 2.0
        if bad(mid):
            lo = mid
        else:
            hi = mid
    return hi


# ---------------------------------------------------------------- the wobble (round 5: instability)
# At the tightest every piece also rocks about its radial axis, each with its own phase, so an upper piece may be anywhere in
# [twist - w, twist + w] while the lower piece it passes is anywhere in the same range. Collisions are between pairs, so it is
# enough to test every pair of twists (upper, lower) from the range.
def pieces_two(e, twist_up, twist_low):
    up, _ = pieces(e, twist_up)
    _, down = pieces(e, twist_low)
    return up, down


def wobble_rings_collide(e, twist_deg, w, slices=400, samples=5):
    """Rings with each piece's ring widened to cover its whole wobble (the union over sampled twists): conservative, and fast."""
    twists = [twist_deg - w + 2.0 * w * k / (samples - 1) for k in range(samples)]
    sets = [pieces(e, t) for t in twists]
    allv = [v for up, down in sets for t in up + down for v in t]
    lo = min(v[1] for v in allv); hi = max(v[1] for v in allv)
    for s_ in range(slices + 1):
        y = lo + (hi - lo) * (s_ + 0.5) / (slices + 1)
        ru, rd = [], []
        for i in range(4):
            for side, out in ((0, ru), (1, rd)):
                rs = [r for r in (ring(sets[k][side][i], y) for k in range(samples)) if r]
                if rs:
                    out.append((min(r[0] for r in rs), max(r[1] for r in rs)))
        for x in ru:
            for z in rd:
                if x[0] < z[1] - 1e-9 and z[0] < x[1] - 1e-9:
                    return True
    return False


def wobble_gap(e, twist_deg, w, step=0.5, span=120.0, samples=5):
    """The smallest gap over the relative turn for every pair of twists in the wobble range, upper-lower and within one triforce."""
    twists = [twist_deg - w + 2.0 * w * k / (samples - 1) for k in range(samples)] if w > 0 else [twist_deg]
    worst = 1e9
    for tu in twists:
        for tl in twists:
            up, down = pieces_two(e, tu, tl)
            up2, _ = pieces(e, tl)
            for i in range(4):          # two pieces of one triforce with different twists
                for j in range(4):
                    if i != j:
                        worst = min(worst, gap(up[i], up2[j]))
            for k in range(int(round(span / step))):
                low = [turned(t, k * step) for t in down]
                for x in up:
                    for z in low:
                        worst = min(worst, gap(x, z))
    return worst


def e_min_wobble(twist_deg, w):
    lo, hi = 0.0, 1.0
    for _ in range(22):
        mid = (lo + hi) / 2.0
        if wobble_rings_collide(mid, twist_deg, w):
            lo = mid
        else:
            hi = mid
    return hi


# ---------------------------------------------------------------- the crossing choreography (round 5: hemisphere crossing)
# At full compaction the six waist pieces stop circling with their triforces and each loops about its own place on the sphere,
# crossing the equator and reversing its direction of travel as it crosses; the two pole pieces stay at the poles and keep turning
# opposite ways. Positions are given on the sphere (longitude, latitude), the piece's radial axis always pointing outward.
LAT0 = math.asin(1.0 / 3.0)        # the waist pieces' own latitude in a triforce (19.47 degrees below or above the equator)
LOOP_TICKS = 200                   # the choreography's loop: 10 s
CROSSINGS = 3                      # crossing cycles per loop (3.33 s each)
ORBIT_DEG = 6.0                    # how far a piece travels along the equator each way at the widest
ORBIT_SWELL = (0.7, 0.3)           # the orbit's width over the loop: 0.7 + 0.3 cos(2 pi t / loop), so the loop does not repeat inside 10 s
PIROUETTE_DEG = 10.0               # each way: free up to 6, then about 0.02 px of spread a degree (part 5 prints the scan)
PIROUETTE_MIRRORED = True
LOCAL_TWIST_DEG = 30.0             # a waist piece's twist in its local east-north frame: +30 upper triforce, -30 lower (= the 60 degree twist)


def sphere_piece(rs, lon, lat, twist):
    """A small tetrahedron (whole edge 1) with its centre on the sphere of radius rs at (lon, lat), turned twist about its radial axis."""
    cl, sl, cp, sp = math.cos(lat), math.sin(lat), math.cos(lon), math.sin(lon)
    u = (cl * cp, sl, cl * sp)
    east = (-sp, 0.0, cp)
    north = (-sl * cp, cl, -sl * sp)
    out = [tuple(u[i] * (rs + R / 2.0) for i in range(3))]
    for j in range(3):
        ang = twist + j * 2.0 * math.pi / 3.0
        ca, sa = math.cos(ang), math.sin(ang)
        out.append(tuple(u[i] * (rs - R / 6.0) + (RHO / 2.0) * (ca * east[i] + sa * north[i]) for i in range(3)))
    return out


def waist_pose(k, t, h):
    """(lon, lat, twist) of waist piece k (0..5; even = upper triforce) at tick t; h = 0 its place in the triforce, 1 the full choreography."""
    typ = 1.0 if k % 2 == 0 else -1.0
    x = 2.0 * math.pi * CROSSINGS * t / LOOP_TICKS + k * math.pi / 3.0      # a wave travelling round the waist
    swell = ORBIT_SWELL[0] + ORBIT_SWELL[1] * math.cos(2.0 * math.pi * t / LOOP_TICKS)
    lat = LAT0 * ((1.0 - h) * -typ + h * math.cos(x))
    lon = math.radians(60.0 * k) - h * math.radians(ORBIT_DEG) * swell * math.sin(x)
    # the pirouette: a partial turn about its own axis, one way above the equator and back below it (so it reverses at the equator,
    # where the twist is at its furthest); upper and lower pieces turn in mirror image, like everything else about them
    pirouette = math.radians(PIROUETTE_DEG) * h * (typ if PIROUETTE_MIRRORED else 1.0) * math.sin(x)
    return lon, lat, typ * math.radians(LOCAL_TWIST_DEG) + pirouette


def choreo_gap(e, h, wobble_deg, frames=400, pole_step_deg=10.0, wobble_samples=3, stop_below=None, pose=None):
    """
    The smallest gap anywhere in the loop: every pair of waist pieces, and every waist piece against both pole pieces at every
    pole angle (the poles keep turning, so any angle can meet any frame). Every piece may be anywhere in +-wobble_deg of twist.
    """
    rs = R / 2.0 + e
    offs = [0.0] if wobble_deg == 0 else [math.radians(-wobble_deg + 2.0 * wobble_deg * i / (wobble_samples - 1)) for i in range(wobble_samples)]
    poles = [sphere_piece(rs, 0.0, s_ * math.pi / 2.0, math.radians(q * pole_step_deg))
             for s_ in (1, -1) for q in range(int(round(120.0 / pole_step_deg)))]
    worst = 1e9
    for f in range(frames):
        t = LOOP_TICKS * f / frames
        ws = []
        for k in range(6):
            lon, lat, tw = (pose or waist_pose)(k, t, h)
            ws.append([sphere_piece(rs, lon, lat, tw + o) for o in offs])
        for a_ in range(6):
            for b_ in range(a_ + 1, 6):
                for x in ws[a_]:
                    for z in ws[b_]:
                        worst = min(worst, gap(x, z))
            for x in ws[a_]:
                for pl in poles:
                    worst = min(worst, gap(x, pl))
        if stop_below is not None and worst < stop_below:
            return worst
    return worst


def choreo_e_min(wobble_deg):
    lo, hi = 0.1, 0.3
    for _ in range(9):
        mid = (lo + hi) / 2.0
        # the full set of pairs, pole angles and twists at every tick (a coarser search misses the worst moment by 0.1 px)
        if choreo_gap(mid, 1.0, wobble_deg, frames=200, stop_below=1e-9) > 1e-9:
            hi = mid
        else:
            lo = mid
    return hi


# ---------------------------------------------------------------- the stored loop (what the renderer plays back)
# The renderer does not evaluate the formula above: it plays one waist piece's loop from a table of keyframes (its longitude
# offset, latitude and pirouette, in degrees, every KEY_TICKS ticks), smoothed between keys with a periodic Catmull-Rom curve.
# Waist piece k reads the same table a sixth of a crossing cycle further on per place round the waist, so the wave still
# travels; its pirouette is mirrored for the lower triforce. The proof (part 7) runs on this playback, not on the formula.
KEYS = 40
KEY_TICKS = LOOP_TICKS / KEYS
PIECE_SHIFT_TICKS = LOOP_TICKS / CROSSINGS / 6.0
LOOP_JAVA = 'src/main/java/dev/movingcastle/thaumicconcilium/client/CapitalLoop.java'


def loop_table():
    """The keyframes of waist piece 0 at full choreography: (longitude offset, latitude, pirouette) in degrees, rounded as stored."""
    rows = []
    for i in range(KEYS):
        lon, lat, tw = waist_pose(0, i * KEY_TICKS, 1.0)
        rows.append((round(math.degrees(lon), 4), round(math.degrees(lat), 4), round(math.degrees(tw) - LOCAL_TWIST_DEG, 4)))
    return rows


def loop_sample(rows, t):
    """The table at tick t (any real number): periodic Catmull-Rom through the keys. The renderer's CapitalMotion.sample is the same."""
    x = (t / KEY_TICKS) % KEYS
    i = int(math.floor(x)) % KEYS
    f = x - math.floor(x)
    p0, p1, p2, p3 = rows[(i - 1) % KEYS], rows[i], rows[(i + 1) % KEYS], rows[(i + 2) % KEYS]
    return tuple(0.5 * (2.0 * p1[c] + (p2[c] - p0[c]) * f + (2.0 * p0[c] - 5.0 * p1[c] + 4.0 * p2[c] - p3[c]) * f * f
                        + (3.0 * p1[c] - p0[c] - 3.0 * p2[c] + p3[c]) * f * f * f) for c in range(3))


_ROWS = None


def played_pose(k, t, h):
    """(lon, lat, twist) of waist piece k as the renderer plays it: its place in the triforce blended by h into the stored loop."""
    global _ROWS
    if _ROWS is None:
        _ROWS = loop_table()
    typ = 1.0 if k % 2 == 0 else -1.0
    lon, lat, pir = loop_sample(_ROWS, t + k * PIECE_SHIFT_TICKS)
    return (math.radians(60.0 * k + h * lon), math.radians((1.0 - h) * -typ * math.degrees(LAT0) + h * lat),
            typ * math.radians(LOCAL_TWIST_DEG + h * pir))


def loop_java():
    rows = loop_table()
    def arr(name, c):
        vals = ', '.join(f'{r[c]:.4f}F' for r in rows)
        return f'    static final float[] {name} = {{{vals}}};\n'
    return ('package dev.movingcastle.thaumicconcilium.client;\n\n'
            '/**\n'
            ' * GENERATED by tools/capital_clearance.py --table; do not edit. The Verdant Capital\'s hemisphere-crossing loop (docs/design/5d-look.md,\n'
            ' * addendum A5): one waist piece\'s longitude offset, latitude and pirouette in degrees at ' + str(KEYS) + ' keyframes, ' + f'{KEY_TICKS:g}' + ' ticks apart. The same\n'
            ' * script proves the loop contact-free as CapitalMotion plays it back (periodic Catmull-Rom between the keys).\n'
            ' */\n'
            'final class CapitalLoop {\n'
            f'    static final int KEYS = {KEYS};\n'
            f'    static final float KEY_TICKS = {KEY_TICKS:g}F;\n'
            f'    static final float LOOP_TICKS = {LOOP_TICKS:g}F;\n'
            '    /** Waist piece k (its place round the waist, in sixths of a turn) reads the table this many ticks further on per place. */\n'
            f'    static final float PIECE_SHIFT_TICKS = {PIECE_SHIFT_TICKS:.6f}F;\n'
            + arr('LON', 0) + arr('LAT', 1) + arr('PIROUETTE', 2) +
            '\n    private CapitalLoop() {\n    }\n}\n')


# ---------------------------------------------------------------- round 5 as built (CapitalMotion; part 7)
RING_PX = 2.035                    # the tightest spread while the triforces still circle (wobble +-4 degrees)
TIGHT_PX = 1.75                    # full compaction on the crossing loop (A5)
IDLE_PX = 2.60
FED_PX = 2.95                      # fed's base; its inhale tops out at 3.202 px
TWIST_PER_PX_5 = (60.0 - 45.0) / (3.202 - RING_PX)
H_OPEN, H_TIGHT = 0.25, 0.12       # the breath's headroom above the base: open states, full compaction


def openness(base_px):
    """0 at the ring's tightest spread, 1 at idle's and wider: how much of the open states' behaviour applies at a base spread."""
    return max(0.0, min(1.0, (base_px - RING_PX) / (IDLE_PX - RING_PX)))


def twist_built(base_px, breath_px):
    """CapitalMotion.twist: the schedule read at the breathing spread in the open states, at the base itself by the ring's tightest."""
    return max(0.0, min(60.0, 60.0 - TWIST_PER_PX_5 * (base_px + breath_px * openness(base_px) - RING_PX)))


def headroom_built(base_px):
    return H_TIGHT + (H_OPEN - H_TIGHT) * openness(base_px)


def wobble_built(base_px):
    """The most wobble the renderer allows at a base spread (it takes the smaller of this and the arousal curve's peak)."""
    return 4.0 - 2.5 * openness(base_px)


# ---------------------------------------------------------------- the built numbers (CapitalStar / VerdantCapitalRenderer)
EDGE_PX = 7.75                     # the whole triforce's edge; a piece is half of it
TWIST_MAX = 60.0                   # degrees, at the tightest
TWIST_PER_PX = 9.375               # degrees of twist given up per pixel of spread beyond the tightest
TOUCH_MARGIN_PX = 0.02             # what turns "touching" into "apart": the only margin on the tightest state
OPEN_IDLE_PX = 0.8                 # idle and starved stand this far out from the tightest
OPEN_FED_PX = 1.6                  # fed


def schedule(e_px, tight_px):
    """The twist for a spread: full at the tightest, eased off in a straight line as the star opens."""
    return max(0.0, min(TWIST_MAX, TWIST_MAX - TWIST_PER_PX * (e_px - tight_px)))


if __name__ == '__main__':
    import os, sys
    _root = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
    if '--table' in sys.argv:
        with open(os.path.join(_root, LOOP_JAVA), 'w', encoding='utf-8', newline=chr(10)) as _f:
            _f.write(loop_java())
        print('wrote ' + LOOP_JAVA)
        sys.exit(0)
    print('1. the smallest spread per twist (whole edge 1; rings, bisected):')
    table = {}
    for deg in range(0, 61, 5):
        table[deg] = e_min(deg)
        print(f'   twist {deg:2d} deg: e_min = {table[deg]:.4f}' + ('   (= R, the untwisted star of the earlier rounds)' if deg == 0 else ''))
    print(f'   (65..120 mirror 55..0; R = {R:.4f}, R/3 = {R / 3:.4f})')
    best = table[60]
    print(f'   best: twist 60 deg, e_min = {best:.4f} = {best / R:.3f} R: the star compacts to a third of the untwisted minimum')
    print()
    tight = best * EDGE_PX + TOUCH_MARGIN_PX
    print(f'2. the build: whole edge {EDGE_PX} px, a piece {EDGE_PX / 2} px; e_min at 60 deg = {best * EDGE_PX:.3f} px')
    print('   relative turn in 0.25 degree steps, all 16 upper-lower pairs, separating-axis test:')
    states = (('just under e_min', best * EDGE_PX - 0.02), ('at e_min', best * EDGE_PX), ('unwinding', tight),
              ('idle and starved', tight + OPEN_IDLE_PX), ('fed', tight + OPEN_FED_PX))
    for name, e_px in states:
        tw = schedule(e_px, tight)
        g, at = rotation_gap(e_px / EDGE_PX, tw)
        side = same_side_gap(e_px / EDGE_PX, tw)
        state = 'apart' if g > 1e-9 else 'touching' if g > -1e-9 else 'OVERLAP'
        tip = (R + e_px / EDGE_PX) * EDGE_PX
        print(f'   {name:17s} e = {e_px:6.3f} px, twist {tw:5.2f} deg: smallest gap {g * EDGE_PX:+.4f} px at {at:6.2f} deg ({state}); '
              f'within one triforce {side * EDGE_PX:+.3f} px; sphere radius {(R / 2) * EDGE_PX + e_px:.2f} px, '
              f'star {tip * RHO / R * 2:.2f} px across, {tip * 2:.2f} px tall')
    print()
    print('3. the twist schedule between the states (relative turn 0..120 degrees, which the three-fold symmetry repeats; 0.5 degree steps):')
    worst = 1e9
    for k in range(0, 17):
        e_px = tight + OPEN_FED_PX * k / 16.0
        tw = schedule(e_px, tight)
        g, _ = rotation_gap(e_px / EDGE_PX, tw, step=0.5, span=120.0)
        side = same_side_gap(e_px / EDGE_PX, tw)
        worst = min(worst, g, side)
        print(f'   e = {e_px:.3f} px, twist {tw:5.2f} deg: gap {g * EDGE_PX:+.4f} px, within one triforce {side * EDGE_PX:+.3f} px')
    print(f'   smallest anywhere on the path: {worst * EDGE_PX:+.4f} px ' + ('(no contact)' if worst > 1e-9 else '(CONTACT)'))

    # ------------------------------------------------------------ round 5 (proposed; the renderer still has the numbers above)
    print()
    print('4. round 5, instability: the tightest state backed off so every piece can rock about its radial axis, each on its own')
    W = 4.0                       # degrees each way
    MARGIN = 1.04                 # the few-percent margin, on the spread
    for w in (2.0, 3.0, 4.0, 5.0, 6.0):
        print(f'   wobble +-{w:.0f} deg about 60: smallest spread {e_min_wobble(60.0, w) * EDGE_PX:.3f} px')
    need = e_min_wobble(60.0, W) * EDGE_PX
    tight5 = round(need * MARGIN, 3)
    idle5, fed5 = 2.60, 3.202
    per_px = (60.0 - 45.0) / (fed5 - tight5)
    print(f'   chosen: wobble +-{W:.0f} deg, smallest spread {need:.3f} px, x {MARGIN} = unwinding {tight5:.3f} px; idle {idle5} px; fed {fed5} px (unchanged)')
    print(f'   schedule: twist = 60 - {per_px:.3f} x (spread - {tight5:.3f} px); the wobble fades in a straight line from full at unwinding to nothing at idle')
    for name, e_px in (('unwinding', tight5), ('half way', (tight5 + idle5) / 2.0), ('idle and starved', idle5), ('fed', fed5)):
        tw = max(0.0, 60.0 - per_px * (e_px - tight5))
        w = W * max(0.0, min(1.0, (idle5 - e_px) / (idle5 - tight5)))
        g = wobble_gap(e_px / EDGE_PX, tw, w)
        tip = (R + e_px / EDGE_PX) * EDGE_PX
        print(f'   {name:17s} e = {e_px:.3f} px, twist {tw:5.2f} +-{w:.1f} deg: smallest gap {g * EDGE_PX:+.4f} px (every pair of twists, whole relative turn); '
              f'sphere radius {(R / 2) * EDGE_PX + e_px:.2f} px, star {tip * RHO / R * 2:.2f} x {tip * 2:.2f} px')

    # ------------------------------------------------------------ round 5b (proposed): the crossing choreography and the breath
    print()
    print('5. round 5, hemisphere crossing: the six waist pieces loop across the equator, the poles stay (a 10 s loop)')
    up60, down60 = pieces(0.3, 60.0)
    mine = sphere_piece(R / 2.0 + 0.3, math.radians(120.0), -LAT0, math.radians(LOCAL_TWIST_DEG))
    err = max(min(math.dist(v, w) for w in up60[2]) for v in mine)
    print(f'   check: the h = 0 pose is the 60-degree-twisted triforce piece (largest vertex mismatch {err:.1e})')
    chosen_pirouette = PIROUETTE_DEG
    print('   the pirouette (a partial turn of each crossing piece about its own axis, reversing at the equator), with the +-4 degree wobble:')
    for amp in (0.0, 6.0, 8.0, 10.0, 12.0, 15.0, 20.0):
        globals()['PIROUETTE_DEG'] = amp
        print(f'     +-{amp:2.0f} degrees: smallest clear spread {choreo_e_min(4.0) * EDGE_PX:.3f} px')
    globals()['PIROUETTE_DEG'] = chosen_pirouette
    C_WOBBLE = 4.0
    need_c = choreo_e_min(C_WOBBLE) * EDGE_PX
    tight_c = round(need_c * 1.04, 3)
    print(f'   chosen: pirouette +-{PIROUETTE_DEG:.0f} deg, wobble +-{C_WOBBLE:.0f} deg, {need_c:.3f} px x 1.04 = full compaction {tight_c:.3f} px (the ring could only reach {tight5:.3f} px)')
    g = choreo_gap(tight_c / EDGE_PX, 1.0, C_WOBBLE)
    print(f'   proof, whole loop: 400 frames (every half tick), 15 waist pairs x 9 twist pairs, 12 pole pairs x 12 pole angles x 3 twists:')
    print(f'     smallest gap {g * EDGE_PX:+.4f} px; sphere radius {(R / 2) * EDGE_PX + tight_c:.2f} px, star {(R * EDGE_PX + tight_c) * 2:.2f} px tall')
    g2 = choreo_gap((tight_c + 0.12) / EDGE_PX, 1.0, C_WOBBLE, frames=100)
    print(f'     at the top of a breath (+0.12 px): {g2 * EDGE_PX:+.4f} px')
    print('   blending in (h from 0 to 1: the pieces take up their loops at the ring\'s tightest spread first, and the star compacts')
    print('   only over the last 40%, eased; wobble +-4; 100 frames):')
    for k in range(0, 11):
        h = k / 10.0
        q = max(0.0, (h - 0.6) / 0.4)
        e_px = tight5 - (tight5 - tight_c) * q * q * (3.0 - 2.0 * q)
        print(f'     h = {h:.1f}, spread {e_px:.3f} px: smallest gap {choreo_gap(e_px / EDGE_PX, h, C_WOBBLE, frames=100) * EDGE_PX:+.4f} px')
    print()
    print('6. the breath and the faint tremble in the open states (the breath only ever expands the sphere from these bases):')
    fed_base = 2.95
    for name, e_px, w in (('idle, exhaled', idle5, 0.75), ('starved, exhaled', idle5, 1.5), ('fed, exhaled', fed_base, 0.75), ('fed, inhaled', fed5, 0.75)):
        tw = max(0.0, 60.0 - per_px * (e_px - tight5))
        print(f'   {name:17s} spread {e_px:.3f} px, twist {tw:5.2f} +-{w} deg: smallest gap {wobble_gap(e_px / EDGE_PX, tw, w, samples=3) * EDGE_PX:+.4f} px; '
              f'star {(R * EDGE_PX + e_px) * 2:.2f} px tall')


    # ------------------------------------------------------------ round 5 as built (CapitalMotion, CapitalLoop)
    print()
    print('7. round 5 as built: the stored loop as the renderer plays it, the breath, the anger period and the relief sigh')
    ok = True

    def verdict(g_px):
        global_ok = g_px > 1e-9
        return '' if global_ok else '   <-- CONTACT'

    try:
        with open(os.path.join(_root, LOOP_JAVA), encoding='utf-8') as _f:
            same = _f.read() == loop_java()
    except OSError:
        same = False
    ok = ok and same
    print(f'   {LOOP_JAVA}: ' + ('matches this script' if same else 'MISSING OR STALE (run with --table)'))
    dev = [0.0, 0.0, 0.0]
    for k in range(6):
        for f in range(800):
            t = LOOP_TICKS * f / 800
            a_ = waist_pose(k, t, 1.0); b_ = played_pose(k, t, 1.0)
            for c in range(3):
                dev[c] = max(dev[c], abs(math.degrees(a_[c] - b_[c])))
    print(f'   playback against the A5 formula, at most: longitude {dev[0]:.3f}, latitude {dev[1]:.3f}, twist {dev[2]:.3f} degrees')
    print(f'   (each piece carries the swell of the loop with it in the table, where the formula swells all six together)')
    print(f'   full compaction {TIGHT_PX} px on the played loop, wobble +-4, 400 frames, all waist pairs and both poles at every angle:')
    for extra in (0.0, 0.06, H_TIGHT):
        g = choreo_gap((TIGHT_PX + extra) / EDGE_PX, 1.0, 4.0, pose=played_pose) * EDGE_PX
        ok = ok and g > 1e-9
        print(f'     breath +{extra:.2f} px: smallest gap {g:+.4f} px' + verdict(g))
    print('   blending in and out on the played loop (the poles free at every step; wobble +-4; 100 frames), exhaled and at the top of a breath:')
    for k in range(0, 11):
        h = k / 10.0
        q = max(0.0, (h - 0.6) / 0.4)
        e_px = RING_PX - (RING_PX - TIGHT_PX) * q * q * (3.0 - 2.0 * q)
        g0 = choreo_gap(e_px / EDGE_PX, h, 4.0, frames=100, pose=played_pose) * EDGE_PX
        g1 = choreo_gap((e_px + H_TIGHT) / EDGE_PX, h, 4.0, frames=100, pose=played_pose) * EDGE_PX
        ok = ok and g0 > 1e-9 and g1 > 1e-9
        print(f'     h = {h:.1f}, spread {e_px:.3f} px: {g0:+.4f} px; +{H_TIGHT} px: {g1:+.4f} px' + verdict(min(g0, g1)))
    print('   circling, between the states (the twist as CapitalMotion.twist, the wobble the most it allows at that base, every pair of')
    print('   twists over the whole relative turn), exhaled and at the top of the deepest breath allowed there:')
    for i in range(0, 5):
        base = RING_PX + (IDLE_PX - RING_PX) * i / 4.0
        w = wobble_built(base)
        for b in (0.0, headroom_built(base)):
            tw = twist_built(base, b)
            g = wobble_gap((base + b) / EDGE_PX, tw, w, samples=3) * EDGE_PX
            ok = ok and g > 1e-9
            print(f'     base {base:.3f} px + {b:.3f}: twist {tw:5.2f} +-{w:.2f} deg: smallest gap {g:+.4f} px' + verdict(g))
    print('   the open states at the most wobble any of them has (+-1.5, starved), so idle, starved, fed and every passage between them:')
    for base in (IDLE_PX, (IDLE_PX + FED_PX) / 2.0, FED_PX):
        for b in (0.0, H_OPEN):
            tw = twist_built(base, b)
            g = wobble_gap((base + b) / EDGE_PX, tw, 1.5, samples=3) * EDGE_PX
            ok = ok and g > 1e-9
            tall = (R * EDGE_PX + base + b) * 2.0
            ok = ok and tall <= 16.0
            print(f'     base {base:.3f} px + {b:.3f}: twist {tw:5.2f} +-1.50 deg: smallest gap {g:+.4f} px; star {tall:.2f} px tall' + verdict(g))
    print(f'   the anger period: starved base {IDLE_PX} px, breath to the whole headroom (+{H_OPEN} px), wobble +-1.5: the rows "base 2.600" above.')
    print('   the relief sigh: a breath to the whole headroom of wherever the base stands, wobble no more than allowed there: every row above.')
    print(f'   the jiggle moves the whole capital rigidly; it is held to 8 - {R * EDGE_PX:.3f} - spread px, so the star never leaves its block.')
    print()
    print('RESULT: ' + ('every state and transition contact-free' if ok else 'CONTACT OR STALE TABLE: see the rows marked above'))
