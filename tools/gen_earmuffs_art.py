"""Earmuffs (phase 10, R6): item icon and worn armour layer. Our own art, drawn with the art kit.

The object: a bent strip of tanned leather (the headband) with a padded wool cup at each end, seen three-quarter from
the wearer's left-front, so the near cup shows its leather back and wool rim and the far cup shows its wool pad.
    python -I mods/thaumic-concilium/tools/gen_earmuffs_art.py <workspace root>
"""
import os
import sys

ROOT = sys.argv[1]
sys.path.insert(0, os.path.join(ROOT, 'tools', 'art-style'))
from artkit import Sprite, ramp, tone, MATERIALS
from pngio import write_png

TEX = os.path.join(ROOT, 'mods', 'thaumic-concilium', 'src', 'main', 'resources', 'assets', 'thaumicconcilium', 'textures')
WOOL = ramp('e4dcc6', shift=14, spread=0.85)     # undyed wool: cream, cool shadows

icon = Sprite([
    '................',
    '.....llllll.....',
    '...llllllllll...',
    '..llll....llll..',
    '.lll........lll.',
    '.ll..........ll.',
    '.ll..........ll.',
    '.www.........lw.',
    'wwwww.......lllw',
    'wwwww.......lllw',
    'wwwww.......lllw',
    'wwwww.......lllw',
    '.www.........lw.',
    '................',
    '................',
    '................',
], {'l': 'leather', 'w': WOOL}, seed=4, grain=0.12)
icon.detail(2, 9, WOOL[1]).detail(2, 10, WOOL[2])            # the hollow of the far pad
icon.detail(13, 9, tone('leather', 4))                        # stitch highlight on the near cup
icon.save(os.path.join(TEX, 'item', 'earmuffs.png'))

# Worn layer: a standard 64x32 armour layer; only the head box (texOffs 0,0, 8x8x8) is painted.
# Head UV: top (8,0)-(16,8), right side (0,8)-(8,16), front (8,8)-(16,16), left side (16,8)-(24,16), back (24,8)-(32,16).
W, H = 64, 32
px = [(0, 0, 0, 0)] * (W * H)


def put(x, y, c):
    px[y * W + x] = c


L = MATERIALS['leather']
# band across the top face, two pixels deep (v 3-4), lit from the front edge
for x in range(8, 16):
    edge = x in (8, 15)
    put(x, 3, L[2 if edge else 4 if x in (10, 11) else 3])
    put(x, 4, L[1 if edge else 2])
# band coming down each side (u 3-4 of the face), then the cup
MUFF = [
    '.ww.',
    'wwww',
    'wwww',
    'wwww',
    '.ww.',
]
SHADE = [            # tone index per pixel: light top-left, dark bottom-right
    [0, 5, 4, 0],
    [5, 4, 3, 2],
    [4, 3, 3, 1],
    [3, 3, 2, 1],
    [0, 2, 1, 0],
]
for u0 in (0, 16):
    for v in (8, 9):
        put(u0 + 3, v, L[3 if v == 8 else 2])
        put(u0 + 4, v, L[2 if v == 8 else 1])
    for j, row in enumerate(MUFF):
        for i, ch in enumerate(row):
            if ch == 'w':
                put(u0 + 2 + i, 10 + j, WOOL[SHADE[j][i]])
    put(u0 + 3, 12, L[2]); put(u0 + 4, 12, L[1])      # the leather boss in the middle of the cup
    put(u0 + 3, 11, L[3])
write_png(os.path.join(TEX, 'models', 'armor', 'earmuffs_layer_1.png'), W, H, px)
print('wrote earmuffs.png, earmuffs_layer_1.png')
