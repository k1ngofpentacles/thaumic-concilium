"""Generates the shield foci data (design: docs/design/shield-foci-design.md): the Riposte and Aegis spell parts, the
guard_castable and shield_foci tags, their two research entries, and the look's art (docs/design/shield-foci-look.md):
the two part icons, the Aegis effect icon, the Riposte halo, the ward both cues share and the Aegis rune strip. All of it is drawn here.
Run from the module root after gen_phase2_data.py (research parents) and before gen_lang.py (it writes the text).
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genlib import *


def setting(part, key, lo, hi, default, complexity, labels):
    return {'key': key, 'min': lo, 'max': hi, 'name': f'spell.{NS}.setting.{key}', 'default': default,
            'complexity': complexity, 'labels': [f'spell.{NS}.value.{part}.{key}.{v}' for v in labels]}


STEPS = ['s5', 's10', 's15', 's20']   # seconds per step of the window and duration settings (5, 10, 15, 20)

# ---------------------------------------------------------------- spell parts
w(f'data/{NS}/thaumaturge/spell_part/riposte.json', {
    'aspect': f'{T}:aversio', 'behavior': {'type': f'{NS}:riposte'}, 'color': 0xC9D6E8, 'complexity': 5,
    'icon': f'{NS}:textures/foci/riposte.png', 'max_per_spell': 1,
    'research': {'entry': f'{NS}:riposte_focus'},
    'settings': [setting('riposte', 'window', 1, 4, 2, 1.0, STEPS)]})
w(f'data/{NS}/thaumaturge/spell_part/aegis.json', {
    'aspect': f'{T}:praemunio', 'behavior': {'type': f'{NS}:aegis'}, 'color': 0x7FB2FF, 'complexity': 6, 'vis': 3.0,
    'cooldown_multiplier': 2.0,
    'icon': f'{NS}:textures/foci/aegis.png',
    'research': {'entry': f'{NS}:aegis_focus'},
    'settings': [setting('aegis', 'duration', 1, 4, 2, 1.0, STEPS)]})

# ---------------------------------------------------------------- tags
# Deliveries a worn bracelet may cast behind a raised shield: those that act on or around the caster, plus riposte
# (ShieldFoci.GUARD_CASTABLE, walked with DrumCastable). touch and wall are not aimed past the shield: touch reaches what the
# arm reaches, wall rises where the caster stands. Everything beneath such a delivery is fine.
w(f'data/{NS}/tags/thaumaturge/spell_part/guard_castable.json', {'replace': False, 'values': [
    f'{T}:self', f'{T}:aura', f'{T}:cloud', f'{T}:touch', f'{T}:wall', f'{NS}:riposte']})
# Marks our shield parts, for tooltips and research text (it is not the gate any more).
w(f'data/{NS}/tags/thaumaturge/spell_part/shield_foci.json', {'replace': False, 'values': [f'{NS}:riposte', f'{NS}:aegis']})

# ---------------------------------------------------------------- research
# Free cells beside the bracelet fan (casting_bracelets is at 5,1; scribing_focus at 3,2). Bash (later) takes 4,0.
entry('riposte_focus', 4, 1, f'focus:{NS}:riposte',
      [f'{NS}:casting_bracelets', f'~{T}:thaumium_shield', f'{T}:focus_mine'],
      [('aversio', 10), ('praemunio', 5), ('motus', 5)], complexity=2)
entry('aegis_focus', 3, 1, f'focus:{NS}:aegis',
      [f'{NS}:riposte_focus', f'{T}:runic_shielding'],
      [('praemunio', 15), ('praecantatio', 10), ('aversio', 5)], complexity=2)

# ---------------------------------------------------------------- art (original; shapes rasterised with 4 x 4 supersampling)
SS = 4


def raster(wid, hei, inside, colour=(255, 255, 255), hard=False):
    """Pixels of a shape: inside(x, y) -> bool (or an (r, g, b) colour) in pixel units; alpha is the covered share of each pixel
    (hard: 0 or 255 at half cover, for cutout textures)."""
    px = []
    for y in range(hei):
        for x in range(wid):
            hits, col = 0, colour
            for j in range(SS):
                for i in range(SS):
                    got = inside(x + (i + 0.5) / SS, y + (j + 0.5) / SS)
                    if got:
                        hits += 1
                        if got is not True:
                            col = got
            a = hits * 255 // (SS * SS)
            if hard:
                a = 255 if a >= 128 else 0
            px.append((col[0], col[1], col[2], a) if a else (0, 0, 0, 0))
    return px


def heater(cx, top, wid, hei, inset=0.0):
    """A heater shield: flat top, straight sides for the upper 45 percent, then a rounded taper to the point."""
    half, mid, bottom = wid / 2.0 - inset, top + hei * 0.45, top + hei - inset * 1.6
    def inside(x, y):
        if y < top + inset or y > bottom:
            return False
        if y <= mid:
            return abs(x - cx) <= half
        return abs(x - cx) <= half * (1.0 - ((y - mid) / (bottom - mid)) ** 1.7)
    return inside


def segment(x0, y0, x1, y1, thick):
    """A line from (x0, y0) to (x1, y1) with round ends."""
    dx, dy = x1 - x0, y1 - y0
    ll = dx * dx + dy * dy
    def inside(x, y):
        t = max(0.0, min(1.0, ((x - x0) * dx + (y - y0) * dy) / ll))
        return (x - x0 - t * dx) ** 2 + (y - y0 - t * dy) ** 2 <= (thick / 2.0) ** 2
    return inside


def triangle(a, b, c):
    def side(p, q, x, y): return (q[0] - p[0]) * (y - p[1]) - (q[1] - p[1]) * (x - p[0])
    def inside(x, y):
        d = (side(a, b, x, y), side(b, c, x, y), side(c, a, x, y))
        return all(v >= 0 for v in d) or all(v <= 0 for v in d)
    return inside


def riposte_icon():
    """32 x 32, white: a heater shield's outline with an arrow leaving it up and to the right (the blow given back)."""
    outer, inner = heater(13.0, 6.0, 20.0, 25.0), heater(13.0, 6.0, 20.0, 25.0, 2.6)
    shaft, gap = segment(10.0, 21.0, 24.0, 7.0, 3.2), segment(10.0, 21.0, 26.0, 5.0, 7.0)
    head = triangle((30.5, 1.5), (20.0, 4.0), (28.0, 12.0))
    return raster(32, 32, lambda x, y: shaft(x, y) or head(x, y) or (outer(x, y) and not inner(x, y) and not gap(x, y)))


def aegis_shape(cx, cy, k):
    """The aegis motif about (cx, cy) at scale k (1 = the 32 px icon): a ring with four ticks, a solid heater shield inside.
    Returns two tests: the ring with its ticks, and the shield."""
    shield = heater(cx, cy - 7.5 * k, 11.0 * k, 15.0 * k)
    def ring(x, y):
        dx, dy = x - cx, y - cy
        r2 = dx * dx + dy * dy
        if (12.2 * k) ** 2 <= r2 <= (14.6 * k) ** 2:
            return True
        tick = abs(dx) <= 1.2 * k or abs(dy) <= 1.2 * k
        return tick and (9.2 * k) ** 2 <= r2 <= (12.2 * k) ** 2
    return ring, shield


def aegis_icon():
    ring, shield = aegis_shape(16.0, 16.0, 1.0)
    return raster(32, 32, lambda x, y: ring(x, y) or shield(x, y))


def aegis_effect_icon():
    """18 x 18 in colour, 1 px margin: the ring in the effect's blue, the shield paler."""
    ring, shield = aegis_shape(9.0, 9.0, 8.0 / 14.6)
    return raster(18, 18, lambda x, y: (220, 235, 255) if shield(x, y) else (127, 178, 255) if ring(x, y) else False)


def halo():
    """40 x 60 (2 px per model px): full at the 12 x 22 plate's edge, squared falloff to nothing 4 model px out."""
    px = []
    for y in range(60):
        for x in range(40):
            dx, dy = max(0.0, abs(x + 0.5 - 20.0) - 12.0), max(0.0, abs(y + 0.5 - 30.0) - 22.0)
            d = min(1.0, (dx * dx + dy * dy) ** 0.5 / 8.0)
            px.append((255, 255, 255, int(255 * (1.0 - d) ** 2)))
    return px


def ward():
    """32 x 32, tiling: a crackle of cell walls (the edges between random cells on a torus), bright where two cells meet and
    brighter where three do, over a faint haze. Two layers of it drifting against each other are the ward's shimmer."""
    import random
    rnd = random.Random(7)
    n, size = 14, 32
    seeds = [(rnd.uniform(0, size), rnd.uniform(0, size)) for _ in range(n)]
    px = []
    for y in range(size):
        for x in range(size):
            ds = []
            for sx, sy in seeds:
                dx, dy = abs(x + 0.5 - sx), abs(y + 0.5 - sy)
                dx, dy = min(dx, size - dx), min(dy, size - dy)
                ds.append((dx * dx + dy * dy) ** 0.5)
            ds.sort()
            wall = max(0.0, 1.0 - (ds[1] - ds[0]) / 1.6)
            knot = max(0.0, 1.0 - (ds[2] - ds[0]) / 2.2)
            px.append((255, 255, 255, int(255 * min(1.0, 0.10 + 0.75 * wall * wall + 0.35 * knot))))
    return px


# Eight runes of our own (strokes on a 16 px cell, 2 px margin), then a white cell for the ring's bands.
RUNES = [
    [(4, 2, 4, 14), (4, 2, 11, 6), (11, 6, 4, 9)],
    [(8, 2, 8, 14), (4, 6, 8, 2), (12, 10, 8, 14)],
    [(3, 3, 13, 3), (8, 3, 8, 13), (4, 13, 12, 13), (8, 8, 12, 8)],
    [(4, 2, 12, 8), (12, 8, 4, 14), (4, 8, 8, 8)],
    [(3, 13, 8, 2), (8, 2, 13, 13), (5, 9, 11, 9), (8, 9, 8, 14)],
    [(4, 2, 4, 14), (12, 2, 12, 14), (4, 5, 12, 11)],
    [(8, 2, 3, 8), (3, 8, 8, 14), (8, 14, 13, 8), (13, 8, 8, 2), (8, 6, 8, 10)],
    [(3, 4, 13, 4), (5, 4, 5, 13), (11, 4, 11, 10), (5, 13, 13, 13)],
]


def rune_strip():
    strokes = [segment(16 * i + a, b, 16 * i + c, d, 1.7) for i, rune in enumerate(RUNES) for a, b, c, d in rune]
    return raster(16 * 9, 16, lambda x, y: x >= 16 * 8 or any(s(x, y) for s in strokes))


write_png(os.path.join(A, 'textures/foci/riposte.png'), 32, 32, riposte_icon())
write_png(os.path.join(A, 'textures/foci/aegis.png'), 32, 32, aegis_icon())
write_png(os.path.join(A, 'textures/mob_effect/aegis.png'), 18, 18, aegis_effect_icon())
write_png(os.path.join(A, 'textures/misc/guard_halo.png'), 40, 60, halo())
write_png(os.path.join(A, 'textures/misc/guard_ward.png'), 32, 32, ward())
write_png(os.path.join(A, 'textures/misc/aegis_runes.png'), 16 * 9, 16, rune_strip())
print('shield foci data written')
