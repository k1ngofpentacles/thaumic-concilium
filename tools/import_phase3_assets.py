"""Imports phase-3 art. Run from the repo root.

Original textures and models (Thaumic Concilium 1.1.1, WTFPL) are copied from the unpacked original jar
(the workspace's reference/unpacked/ThaumicConcilium-1.1.1, or TC_ORIGINAL_ASSETS). The three Shard Mill OBJ parts
(body, cap, handle) are merged into one OBJ, scaled to fit a block, with a single '#mill' texture slot so both mills
share it. The Elemental Surfer Shorts never had art in the original, so their icon and armour layer are drawn here
(original art, MIT like the rest of the port's code).
"""
import os, shutil, struct, zlib

NS = 'thaumicconcilium'
SRC = os.environ.get('TC_ORIGINAL_ASSETS', '../../reference/unpacked/ThaumicConcilium-1.1.1/assets/thaumicconcilium')
OUT = f'src/main/resources/assets/{NS}'


def copy(src, dst):
    s, d = os.path.join(SRC, src), os.path.join(OUT, dst)
    os.makedirs(os.path.dirname(d), exist_ok=True)
    shutil.copyfile(s, d)
    if os.path.exists(s + '.mcmeta'):
        shutil.copyfile(s + '.mcmeta', d + '.mcmeta')


ITEMS = {
    'burdening_amulet': 'burdening_amulet', 'tight_belt': 'tight_belt', 'dump_jackboots': 'dump_jackboots',
    'chest_windings_icon': 'runic_body_windings', 'legs_windings_icon': 'runic_legs_windings',
    'thicc_bottle': 'bottle_of_thick_taint',  # bottle_of_clear_water is our own redraw (gen_art_redraw.py)
}
for src, dst in ITEMS.items():
    copy(f'textures/items/{src}.png', f'textures/item/{dst}.png')
# Armour: layer_1 is helmet/chest/boots, layer_2 is legs (same 64x32 layout as 1.7.10).
copy('textures/models/armor/chest_windings.png', 'textures/models/armor/windings_layer_1.png')
copy('textures/models/armor/legs_windings.png', 'textures/models/armor/windings_layer_2.png')
copy('textures/models/armor/dump_jackboots.png', 'textures/models/armor/dump_jackboots_layer_1.png')
copy('textures/models/greatwood_shard_mill.png', 'textures/item/greatwood_shard_mill.png')
copy('textures/models/silverwood_shard_mill.png', 'textures/item/silverwood_shard_mill.png')

# ---------------------------------------------------------------- shard mill OBJ
# Two models, both scaled to fit a block around the same centre: the body (body + cap) and the crank handle, which the
# renderer spins about its shaft while grinding. The shaft is the X axis through y = 1.8, z = 0 in the original units
# (the original renderer's pivot): MillRenderer.CRANK_PIVOT_Y/Z hold the scaled position.
SCALE = 0.225
CENTRE = (-0.94, 1.30, 0.0)   # middle of the three parts' bounding box
print('crank pivot (scaled): y=%.4f z=%.4f' % ((1.8 - CENTRE[1]) * SCALE + 0.5, (0.0 - CENTRE[2]) * SCALE + 0.5))
def merge(parts, name):
    verts, uvs, norms, faces = [], [], [], []
    for part in parts:
        base = (len(verts), len(uvs), len(norms))
        for line in open(os.path.join(SRC, f'models/{part}_shard_mill.obj'), encoding='utf-8'):
            p = line.split()
            if not p:
                continue
            if p[0] == 'v':
                x, y, z = (float(c) for c in p[1:4])
                verts.append(((x - CENTRE[0]) * SCALE + 0.5, (y - CENTRE[1]) * SCALE + 0.5, (z - CENTRE[2]) * SCALE + 0.5))
            elif p[0] == 'vt':
                uvs.append(tuple(float(c) for c in p[1:3]))
            elif p[0] == 'vn':
                norms.append(tuple(float(c) for c in p[1:4]))
            elif p[0] == 'f':
                face = []
                for corner in p[1:]:
                    face.append('/'.join(str(int(v) + base[k]) if v else '' for k, v in enumerate(corner.split('/'))))
                faces.append(face)
    obj = [f'# Thaumic Concilium Shard Mill: {" + ".join(parts)} (WTFPL original model, rescaled)', 'mtllib shard_mill.mtl',
           f'o {name}', 'usemtl mill']
    obj += ['v %.6f %.6f %.6f' % v for v in verts]
    obj += ['vt %.6f %.6f' % t for t in uvs]
    obj += ['vn %.6f %.6f %.6f' % n for n in norms]
    obj += ['f ' + ' '.join(f) for f in faces]
    open(f'{OUT}/models/item/{name}.obj', 'w', newline='\n').write('\n'.join(obj) + '\n')
os.makedirs(f'{OUT}/models/item', exist_ok=True)
merge(('body', 'cap'), 'shard_mill_body')
merge(('handle',), 'shard_mill_handle')
open(f'{OUT}/models/item/shard_mill.mtl', 'w', newline='\n').write('newmtl mill\nmap_Kd #mill\n')
old = f'{OUT}/models/item/shard_mill.obj'
if os.path.exists(old):
    os.remove(old)


# ---------------------------------------------------------------- surfer shorts (original art)
def png(path, w, h, pixels):
    raw = b''.join(b'\x00' + b''.join(struct.pack('BBBB', *pixels.get((x, y), (0, 0, 0, 0))) for x in range(w)) for y in range(h))
    def chunk(t, d): return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)) \
        + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'wb').write(data)


CLOTH, SHADE, SEAM = (40, 92, 140, 255), (28, 64, 104, 255), (18, 40, 66, 255)
PRIMAL = [(255, 221, 64, 255), (232, 82, 30, 255), (64, 196, 240, 255), (86, 176, 40, 255)]   # aer, ignis, aqua, terra
BAND = (214, 205, 178, 255)

icon = {}
for y in range(3, 14):
    for x in range(2, 14):
        leg_gap = y >= 8 and 7 <= x <= 8
        if leg_gap:
            continue
        c = CLOTH if (x + y) % 5 else SHADE
        if y == 3:
            c = BAND
        elif x in (2, 13) or y == 13 or (y >= 8 and x in (6, 9)):
            c = SEAM
        icon[(x, y)] = c
for i, col in enumerate(PRIMAL):   # four elemental stripes down the legs
    x = (3, 4, 11, 12)[i]
    for y in range(5, 13):
        icon[(x, y)] = col
png(f'{OUT}/textures/item/elemental_surfer_shorts.png', 16, 16, icon)

layer = {}
def fill(x0, y0, x1, y1, f):
    for y in range(y0, y1):
        for x in range(x0, x1):
            layer[(x, y)] = f(x, y)
cloth = lambda x, y: CLOTH if (x + y) % 5 else SHADE
fill(0, 20, 16, 26, cloth)            # leg sides, upper half only: shorts
fill(4, 16, 8, 20, cloth)             # leg top
fill(16, 28, 40, 32, cloth)           # waist (bottom of the body box)
fill(16, 28, 40, 29, lambda x, y: BAND)
for i, col in enumerate(PRIMAL):
    for y in range(20, 26):
        layer[(1 + 4 * i, y)] = col
png(f'{OUT}/textures/models/armor/elemental_surfer_shorts_layer_2.png', 64, 32, layer)
print('phase 3 assets imported')
