"""Phase 5c cycle 3 (the Vessel's look): the textures (appendix R1-R4 plus the ribbon), the anchor block's models and the sounds.
Executed at the end of gen_phase5c_data.py (exec), so gen_all.py needs no new entry. All art is original (generated here).
"""
import math, os, json

TEX = f'{A}/textures/misc'


def lerp_table(table, x):
    for (x0, v0), (x1, v1) in zip(table, table[1:]):
        if x <= x1:
            return v0 + (v1 - v0) * (x - x0) / (x1 - x0) if x1 > x0 else v1
    return table[-1][1]


def px_square(size, fn):
    out = []
    for y in range(size):
        for x in range(size):
            out.append(fn((x + 0.5) / size * 2 - 1, (y + 0.5) / size * 2 - 1))
    return out


# R1: the dark halo, black with alpha by radius (reaches 0 at rr 1 and stays 0 in the corners)
_dark = [(0, 1.0), (0.3125, 1.0), (0.40, 0.72), (0.50, 0.45), (0.65, 0.20), (0.80, 0.07), (1.0, 0.0)]
def dark_px(dx, dy):
    rr = math.hypot(dx, dy)
    return (0, 0, 0, int(round(255 * lerp_table(_dark, rr)))) if rr < 1.0 else (0, 0, 0, 0)


write_png(f'{TEX}/vessel_dark.png', 128, 128, px_square(128, dark_px))

# R2: the horizon ring, greyscale, alpha 255 (additive: black adds nothing). Peak at rr 0.625.
_ring = [(0.0, 0.0), (0.57, 0.0), (0.60, 0.55), (0.625, 1.0), (0.66, 0.60), (0.72, 0.25), (0.85, 0.05), (1.0, 0.0)]


def ring_px(dx, dy):
    rr = math.hypot(dx, dy)
    ang = math.atan2(dy, dx)
    v = lerp_table(_ring, min(rr, 1.0)) * (0.775 + 0.225 * math.cos(ang)) * (1 + 0.10 * math.sin(9 * ang + 14 * rr))
    c = int(round(255 * max(0.0, min(1.0, v))))
    return (c, c, c, 255)


write_png(f'{TEX}/vessel_ring.png', 256, 256, px_square(256, ring_px))

# R3: the disc haze, 256 x 32; u runs round the annulus (tiles), v from the inner edge to the outer
_dpx = []
for y in range(32):
    v = (y + 0.5) / 32
    edge = 0.0 if v <= 0 or v >= 1 else min(1.0, v / 0.15, (1 - v) / 0.20)
    for x in range(256):
        u = (x + 0.5) / 256
        n = (0.5 * math.sin(2 * math.pi * (3 * u + 1.5 * v)) + 0.3 * math.sin(2 * math.pi * (7 * u - 2 * v))
             + 0.2 * math.sin(2 * math.pi * (13 * u + 4 * v)))
        n = (n + 1.0) / 2.0
        a = int(round(255 * max(0.0, min(1.0, edge * (0.55 + 0.45 * n)))))
        _dpx.append((255, 255, 255, a))
write_png(f'{TEX}/vessel_disc.png', 256, 32, _dpx)


# R4: the clot (dark core, violet edge), and a white variant to tint toward cyan when batted
def clot_px(hot):
    def f(dx, dy):
        rr = math.hypot(dx, dy)
        ang = math.atan2(dy, dx)
        rr = rr / (1 + 0.12 * math.sin(3 * ang) + 0.07 * math.sin(5 * ang + 1))
        a = 1.0 if rr <= 0.6 else max(0.0, 1.0 - (rr - 0.6) / 0.4)
        if hot:
            c = (255, 255, 255)
        else:
            t = 0.0 if rr <= 0.45 else min(1.0, (rr - 0.45) / 0.35)
            c = tuple(int(round(p + (q - p) * t)) for p, q in zip((0x2A, 0x0F, 0x3A), (0xC2, 0x3B, 0xD6)))
        return (c[0], c[1], c[2], int(round(255 * a)))
    return f


write_png(f'{TEX}/vessel_clot.png', 32, 32, px_square(32, clot_px(False)))
write_png(f'{TEX}/vessel_clot_hot.png', 32, 32, px_square(32, clot_px(True)))

# The core: a plain black texture for entityCutoutNoCull
write_png(f'{TEX}/vessel_black.png', 2, 2, [(0, 0, 0, 255)] * 4)

# The ribbon (tether and pins): u across (soft edges), v along with pulses; greyscale, alpha 255, for the additive type
_rpx = []
for y in range(64):
    v = y / 64
    pulse = 0.5 + 0.5 * math.sin(2 * math.pi * v * 2)
    for x in range(16):
        e = abs((x + 0.5) / 16 * 2 - 1)
        c = int(round(255 * max(0.0, (1 - e * e) ** 1.5 * (0.30 + 0.70 * pulse ** 2))))
        _rpx.append((c, c, c, 255))
write_png(f'{TEX}/vessel_ribbon.png', 16, 64, _rpx)

# The ribbon body (C1): white, translucent, under the thin additive line so the ribbon stays violet over a bright sky
_bpx = []
for y in range(64):
    pulse = 0.5 + 0.5 * math.sin(2 * math.pi * (y / 64) * 2)
    for x in range(16):
        e = abs((x + 0.5) / 16 * 2 - 1)
        _bpx.append((255, 255, 255, int(round(255 * max(0.0, 1 - e * e) ** 0.75 * (0.70 + 0.30 * pulse ** 2)))))
write_png(f'{TEX}/vessel_ribbon_body.png', 16, 64, _bpx)

# ---------------------------------------------------------------- anchor block: broken hex shards (cycle 3b)
exec(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "gen_phase5c_anchor.py")).read())

# ---------------------------------------------------------------- sounds: our events, pointing at vanilla / Thaumaturge events
# (name, event, volume, pitch). The secondary layers (Java: VesselSounds) are played beside the primary one.
VESSEL_SOUNDS = [
    ('hum', 'minecraft:block.beacon.ambient', 0.8, 1.0), ('wake', 'minecraft:block.respawn_anchor.charge', 1.0, 0.5),
    ('swell', 'minecraft:entity.warden.sonic_charge', 1.0, 0.6), ('gulp', 'minecraft:entity.warden.sonic_boom', 0.9, 0.5),
    ('clot_windup', 'minecraft:entity.ghast.warn', 0.8, 1.4), ('clot_launch', 'minecraft:entity.ghast.shoot', 0.8, 0.7),
    ('bat', 'minecraft:entity.breeze.deflect', 1.0, 0.9), ('stagger', 'minecraft:block.beacon.deactivate', 1.0, 0.6),
    ('recover', 'minecraft:block.beacon.activate', 0.8, 0.6), ('tether_form', 'thaumaturge:wand', 0.8, 1.0),
    ('tether_break', 'thaumaturge:craftfail', 0.7, 1.0), ('mass_drop', 'minecraft:block.respawn_anchor.deplete', 1.0, 0.8),
    ('horizon_throw', 'minecraft:entity.enderman.teleport', 1.0, 0.5), ('eat', 'minecraft:entity.item.pickup', 0.5, 0.4),
    ('anchor_cool', 'minecraft:block.fire.extinguish', 0.6, 0.6), ('anchor_ready', 'minecraft:block.amethyst_block.chime', 1.0, 0.8),
    ('subdued', 'minecraft:block.conduit.deactivate', 1.0, 0.6),
]
_sp = os.path.join(ROOT, f'assets/{NS}/sounds.json')
_sj = json.load(open(_sp))
for _n, _ev, _v, _p in VESSEL_SOUNDS:
    _sj[f'vessel.{_n}'] = {'subtitle': f'subtitles.{NS}.vessel.{_n}', 'sounds': [{'name': _ev, 'type': 'event', 'volume': _v, 'pitch': _p}]}
with open(_sp, 'w') as _f:
    json.dump(_sj, _f, indent=2)
    _f.write('\n')
