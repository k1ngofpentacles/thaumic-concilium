"""Generates phase-6a datapack JSON and assets: the rift workshop (Vis Conductor, Destabilized Crystal, Vis Condenser,
Hex of Predictability, Rifts and Dematerialization research). Run from the repo root after gen_phase4_data.py (it
restores the original parents of entries the earlier scripts wrote, and gates two more entries on hint notes).

Translated from Thaumonomicon / TCRecipeRegistry (Concilium 1.1.1): TC4 aspect amounts kept, instability / 2.5 capped
at 6, arcane vis = sum of costs with crystals cost / 10, research notes = TC4 amount / 2.5. Models and textures are the
original's (WTFPL): the Hex's OBJ rings (their rune texture split into stone and an ink layer, which the renderer tints
as the original's shader did) and the Vis Condenser's OBJ. The destabilized crystal's art is drawn here (the original
drew Thaumcraft 4's crystal, all rights reserved).
"""
import json, math, os, struct, zipfile, zlib
NS = 'thaumicconcilium'
ROOT = 'src/main/resources'
T = 'thaumaturge'
JAR = os.environ.get('TC_ORIGINAL_JAR', '../../reference/jars/legacy-1.7.10/ThaumicConcilium-1.1.1.jar')
PRIMALS = ['aer', 'terra', 'ignis', 'aqua', 'ordo', 'perditio']


def w(path, obj):
    p = os.path.join(ROOT, path); os.makedirs(os.path.dirname(p), exist_ok=True)
    json.dump(obj, open(p, 'w'), indent=2); open(p, 'a').write('\n')
def r(path): return json.load(open(os.path.join(ROOT, path)))
def asp(a): return a if ':' in a else f'{T}:{a}'
def note(n): return max(1, round(n / 2.5))
def instab(tc4): return max(1, min(6, round(tc4 / 2.5)))
def it(i): return {'item': i if ':' in i else f'{T}:{i}'}


# ---------------------------------------------------------------- spell part
def setting(key, lo, hi, default=0, complexity=0.0, vis=0.0, labels=None):
    s = {'key': key, 'min': lo, 'max': hi, 'name': f'spell.{NS}.setting.{key}', 'default': default}
    if complexity: s['complexity'] = complexity
    if vis: s['vis'] = vis
    if labels: s['labels'] = [f'spell.{NS}.value.vis_conductor.{key}.{v}' for v in labels]
    return s
# Original: 5 of each primal every 3 ticks while drawing (TC4 centivis); per Thaumaturge pulse about 0.5 vis.
w(f'data/{NS}/thaumaturge/spell_part/vis_conductor.json', {
    'aspect': f'{T}:auram', 'behavior': {'type': f'{NS}:vis_conductor'}, 'color': 0xAAAAAA, 'complexity': 6,
    'icon': f'{NS}:textures/foci/vis_conductor.png', 'research': {'entry': f'{NS}:vis_conductor_focus'}, 'vis': 0.5,
    'max_per_spell': 1,
    'settings': [setting('potency', 0, 3, complexity=2.0, vis=0.2),
                 setting('upgrade', 0, 2, complexity=3.0, labels=['none', 'dematerialization', 'wisp_launcher']),
                 setting('extend', 0, 2, complexity=1.0)]})

# ---------------------------------------------------------------- research
CAT = f'{NS}:concilium'
def entry(name, col, row, icon, parents, notes, recipes=(), warp=0, meta=None, complexity=2):
    st = [{'text': f'research.{NS}.{name}.stage_0',
           'required_knowledge': [{'type': 'theory', 'category': CAT, 'amount': 1}]},
          {'text': f'research.{NS}.{name}.stage_1'}]
    if recipes: st[1]['recipes'] = list(recipes)
    if warp: st[0]['warp'] = warp
    obj = {'category': CAT, 'name': f'research.{NS}.{name}.title', 'parents': parents, 'column': col, 'row': row,
           'icons': [icon], 'stages': st,
           'note_aspects': [{'aspect': asp(a), 'amount': note(n)} for a, n in notes], 'complexity': complexity}
    if meta: obj['meta'] = meta
    w(f'data/{NS}/thaumaturge/research_entry/{name}.json', obj)

# VISCONDUCTOR (-4,5): lost (a traded hint), hidden parents THAUMATURGES, INFUSION, VISPOWER, WANDPEDFOC.
entry('vis_conductor_focus', -4, 5, f'focus:{NS}:vis_conductor',
      [f'~{NS}:concilium', f'~{NS}:thaumaturges', f'~{T}:infusion', f'~{T}:recharge_pedestal'],
      [('permutatio', 10), ('instrumentum', 10), ('humanus', 10), ('potentia', 10), ('praecantatio', 10)],
      [f'{NS}:infusion/vis_conductor_focus'], meta=['spiky'], complexity=3)
# DESTCRYSTAL (-2,5): lost (a Mad Thaumaturge's hint), after the Vis Conductor and crystallized essence.
entry('destabilized_crystal', -2, 5, f'{NS}:destabilized_crystal',
      [f'{NS}:vis_conductor_focus', f'~{T}:essentia_crystalizer'],
      [('vitreus', 30), ('potentia', 20), ('praecantatio', 20), ('auram', 30)], [f'{NS}:arcane_workbench/destabilized_crystal'])
# RIFTS (-9,-3): lost, found by scanning a rift (Thaumaturge's flux rifts, which Concilium's now are); 3 warp.
entry('rifts', -9, -3, f'{NS}:textures/research/rift_res.png', [f'{T}:scanned/fluxrift'],
      [('vacuos', 4), ('praecantatio', 4), ('alienis', 4), ('perditio', 8)], warp=3, meta=['hidden', 'round'], complexity=1)
# DEMATUPGRADE (-11,-1): after Rifts; hidden Vis Conductor (and focal manipulation, Thaumaturge's base).
entry('dematerialization', -11, -1, f'{NS}:textures/research/dematerialization.png',
      [f'{NS}:rifts', f'~{NS}:vis_conductor_focus'],
      [('praecantatio', 4), ('potentia', 4), ('auram', 8), ('instrumentum', 4)], complexity=1)
# VISCONDENSER (-13,-1): after Dematerialization; hidden the advanced node stabilizer.
entry('vis_condenser', -13, -1, f'{NS}:vis_condenser', [f'{NS}:dematerialization', f'~{T}:node_stabilizer_advanced'],
      [('praecantatio', 4), ('potentia', 4), ('auram', 8), ('machina', 4)], [f'{NS}:arcane_workbench/vis_condenser'], complexity=1)
# HEXOFPREDICTABILITY (-12,1): after the Vis Condenser; hidden Outer Revelations; 2 warp; special.
entry('hex_of_predictability', -12, 1, f'{NS}:textures/research/hex.png', [f'{NS}:vis_condenser', f'~{T}:outer_revelations'],
      [('ordo', 15), ('terra', 30), ('vinculum', 15), ('praecantatio', 30)], warp=2, meta=['spiky'], complexity=3)

# Original parents restored now that the Destabilized Crystal exists: Cooling Focus and Dump Jackboots hang off it.
for key in ['cooling_focus', 'dump_jackboots']:
    path = f'data/{NS}/thaumaturge/research_entry/{key}.json'
    obj = r(path)
    obj['parents'] = [f'{NS}:destabilized_crystal'] + [p for p in obj['parents'] if p.startswith('~') and p != f'~{NS}:destabilized_crystal']
    w(path, obj)
# The two hints that waited for this phase (agreed 2026-11-21: hint notes gate their entries).
for key in ['vis_conductor_focus', 'destabilized_crystal']:
    path = f'data/{NS}/thaumaturge/research_entry/{key}.json'
    obj = r(path)
    obj['stages'][0]['obtain'] = [{'items': f'{NS}:research_hint', 'amount': 1, 'components': {f'{NS}:hint': f'{NS}:{key}'}}]
    w(path, obj)

# ---------------------------------------------------------------- recipes
def spell(style, part, settings=None):
    node = {'part': f'{NS}:{part}'}
    if settings: node['settings'] = settings
    return {'style': style, 'root': {'part': f'{T}:origin', 'children': [node]}}
def focus(key, sp):
    return {'count': 1, 'id': f'{T}:focus_1', 'components': {
        f'{T}:spell': sp,
        'minecraft:custom_name': json.dumps({'translate': f'{NS}.focus.preset', 'with': [{'translate': f'research.{NS}.{key}.title'}]})}}
VC = spell('channeled', 'vis_conductor')
DEMAT = spell('channeled', 'vis_conductor', {'upgrade': 1})

# "VisConductor": infusion, instability 20; catalyst node transducer; runic matrices, quartz, primal charms, compound
# recharge foci; permutatio 128, potentia 64, praecantatio 128, humanus 256, instrumentum 32.
w(f'data/{NS}/recipe/infusion/vis_conductor_focus.json', {
    'type': f'{T}:infusion', 'catalyst': it('node_transducer'),
    'components': [it('infusion_matrix'), it('minecraft:quartz'), it('primal_charm'), it('recharge_pedestal')] * 2,
    'aspects': [{'aspect': asp(a), 'amount': n} for a, n in [('permutatio', 128), ('potentia', 64), ('praecantatio', 128),
                                                             ('humanus', 256), ('instrumentum', 32)]],
    'instability': instab(20), 'research': {'entry': f'{NS}:vis_conductor_focus', 'stage': 1},
    'result': focus('vis_conductor_focus', VC)})

def arcane_costs(costs):
    return {'vis': sum(n for _, n in costs), 'crystals': [{'aspect': asp(a), 'amount': math.ceil(n / 10)} for a, n in costs]}
# "DestCrystal": shapeless, nine crystallized essences (any aspect), 50 of every primal.
w(f'data/{NS}/recipe/arcane_workbench/destabilized_crystal.json', {
    'type': f'{T}:arcane_workbench_shapeless', 'ingredients': [it('essentia_crystal')] * 9,
    **arcane_costs([(a, 50) for a in PRIMALS]),
    'research': {'entry': f'{NS}:destabilized_crystal', 'stage': 1}, 'result': {'count': 1, 'id': f'{NS}:destabilized_crystal'}})
# "VisCondenser": GSG / BFB / BAB: gold, arcane stone, obsidian, a Vis Conductor with Dematerialization, an advanced
# node stabilizer; perditio 50, ordo 50, aer 10, ignis 10.
demat_ing = {'type': 'neoforge:components', 'items': f'{T}:focus_1', 'components': {f'{T}:spell': DEMAT}}
w(f'data/{NS}/recipe/arcane_workbench/vis_condenser.json', {
    'type': f'{T}:arcane_workbench_shaped', 'pattern': ['GSG', 'BFB', 'BAB'],
    'key': {'G': it('minecraft:gold_ingot'), 'S': it('stone_arcane'), 'B': it('minecraft:obsidian'), 'F': demat_ing,
            'A': it('node_stabilizer_advanced')},
    **arcane_costs([('perditio', 50), ('ordo', 50), ('aer', 10), ('ignis', 10)]),
    'research': {'entry': f'{NS}:vis_condenser', 'stage': 1}, 'result': {'count': 1, 'id': f'{NS}:vis_condenser'}})

# ---------------------------------------------------------------- blocks, items, models
w(f'assets/{NS}/blockstates/hex_of_predictability.json', {'variants': {'': {'model': f'{NS}:block/hex_of_predictability'}}})
w(f'assets/{NS}/models/block/hex_of_predictability.json', {'textures': {'particle': f'{NS}:block/hex/caps'}})
w(f'assets/{NS}/blockstates/vis_condenser.json', {'variants': {'': {'model': f'{NS}:block/vis_condenser'}}})
w(f'assets/{NS}/models/block/vis_condenser.json', {
    'loader': 'neoforge:obj', 'model': f'{NS}:models/block/vis_condenser.obj', 'flip_v': True, 'automatic_culling': False,
    'textures': {'tex': f'{NS}:block/vis_condenser', 'particle': f'{NS}:block/vis_condenser'}})
w(f'assets/{NS}/models/item/vis_condenser.json', {'parent': f'{NS}:block/vis_condenser'})
# Destabilized crystal: three spikes off a base, drawn grey and tinted by the aspect; facing = the side it grows to.
SPIKES = [((6, 0, 6), (10, 12, 10), 0), ((3, 0, 7), (6, 8, 10), 22.5), ((10, 0, 5), (13, 9, 8), -22.5)]
elements = []
for (f, t, angle) in SPIKES:
    faces = {d: {'texture': '#crystal', 'tintindex': 0, 'uv': [0, 0, t[0] - f[0] if d in ('north', 'south') else t[2] - f[2], t[1] - f[1]]}
             for d in ('north', 'south', 'east', 'west')}
    faces['up'] = {'texture': '#crystal', 'tintindex': 0, 'uv': [0, 0, t[0] - f[0], t[2] - f[2]]}
    el = {'from': list(f), 'to': list(t), 'faces': faces}
    if angle:
        el['rotation'] = {'origin': [8, 0, 8], 'axis': 'z' if angle > 0 else 'x', 'angle': angle}
    elements.append(el)
w(f'assets/{NS}/models/block/destabilized_crystal.json', {
    'render_type': 'minecraft:translucent', 'ambientocclusion': False,
    'textures': {'crystal': f'{NS}:block/destabilized_crystal', 'particle': f'{NS}:block/destabilized_crystal'}, 'elements': elements})
ROT = {'up': (0, 0), 'down': (180, 0), 'north': (90, 0), 'south': (90, 180), 'west': (90, 270), 'east': (90, 90)}
w(f'assets/{NS}/blockstates/destabilized_crystal.json', {'variants': {
    f'facing={d}': {'model': f'{NS}:block/destabilized_crystal', **({'x': x} if x else {}), **({'y': y} if y else {})}
    for d, (x, y) in ROT.items()}})
w(f'assets/{NS}/models/item/destabilized_crystal.json', {'parent': f'{NS}:block/destabilized_crystal'})
w(f'assets/{NS}/models/item/void_slag.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/void_slag'}})
# The Hex's OBJ meshes are drawn directly by HexRenderer (their rune texture repeats, which a baked model can't do).


# ---------------------------------------------------------------- art
def png_rgba(b):
    pos, idat, plte, trns = 8, b'', None, None
    while pos < len(b):
        n = struct.unpack('>I', b[pos:pos + 4])[0]; t = b[pos + 4:pos + 8]; d = b[pos + 8:pos + 8 + n]; pos += 12 + n
        if t == b'IHDR': wid, hei, _, ct = struct.unpack('>IIBB', d[:10])
        elif t == b'PLTE': plte = d
        elif t == b'tRNS': trns = d
        elif t == b'IDAT': idat += d
    bpp = {6: 4, 2: 3, 3: 1, 0: 1, 4: 2}[ct]; stride = wid * bpp; raw = zlib.decompress(idat); prev = bytearray(stride); i = 0; px = []
    for _ in range(hei):
        f = raw[i]; i += 1; line = bytearray(raw[i:i + stride]); i += stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0; up = prev[x]; c = prev[x - bpp] if x >= bpp else 0
            if f == 1: line[x] = (line[x] + a) & 255
            elif f == 2: line[x] = (line[x] + up) & 255
            elif f == 3: line[x] = (line[x] + (a + up) // 2) & 255
            elif f == 4:
                p = a + up - c; pa, pb, pc = abs(p - a), abs(p - up), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else up if pb <= pc else c)) & 255
        for x in range(wid):
            q = line[x * bpp:(x + 1) * bpp]
            px.append(tuple(q) if ct == 6 else tuple(q) + (255,) if ct == 2 else
                      tuple(plte[3 * q[0]:3 * q[0] + 3]) + ((trns[q[0]] if trns and q[0] < len(trns) else 255),) if ct == 3 else
                      (q[0],) * 3 + (255,) if ct == 0 else (q[0],) * 3 + (q[1],))
        prev = line
    return wid, hei, px

def write_png(path, wid, hei, px):
    raw = b''.join(b'\x00' + b''.join(struct.pack('BBBB', *px[y * wid + x]) for x in range(wid)) for y in range(hei))
    def chunk(t, d): return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'wb').write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', wid, hei, 8, 6, 0, 0, 0))
                           + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))

A = os.path.join(ROOT, f'assets/{NS}')
if os.path.exists(JAR):
    z = zipfile.ZipFile(JAR)
    def obj(src, dst):
        # Each OBJ becomes one material, "tex", whose texture the model JSON names.
        lines = []
        for line in z.read(f'assets/{NS}/models/{src}.obj').decode('utf-8').splitlines():
            if line.startswith('mtllib'):
                lines.append(f'mtllib {os.path.basename(dst)}.mtl')
            elif line.startswith('usemtl'):
                lines.append('usemtl tex')
            else:
                lines.append(line)
        path = f'{A}/models/{dst}.obj'
        os.makedirs(os.path.dirname(path), exist_ok=True)
        open(path, 'w', newline='\n').write('\n'.join(lines) + '\n')
        open(f'{A}/models/{dst}.mtl', 'w', newline='\n').write('newmtl tex\nmap_Kd #tex\n')
    for n in ['hex1', 'hex2', 'hex3', 'hex_caps']:
        obj(n, f'block/hex/{n}')
    obj('vis_condenser', 'block/vis_condenser')
    for src, dst in [('models/hex_caps.png', 'block/hex/caps.png'), ('models/vis_condenser.png', 'block/vis_condenser.png'),
                     ('items/void_slag.png', 'item/void_slag.png'), ('misc/rift_res.png', 'research/rift_res.png'),
                     ('misc/dematerialization.png', 'research/dematerialization.png'), ('misc/hex.png', 'research/hex.png')]:
        out = f'{A}/textures/{dst}'; os.makedirs(os.path.dirname(out), exist_ok=True)
        open(out, 'wb').write(z.read(f'assets/{NS}/textures/{src}'))
    # The runes: the original shader recoloured every pixel with no blue (the ink); split them into their own layer.
    wid, hei, px = png_rgba(z.read(f'assets/{NS}/textures/models/hex_runes.png'))
    ink = [(255, 255, 255, 255) if p[2] == 0 else (0, 0, 0, 0) for p in px]
    stone = [(0, 0, 0, 0) if p[2] == 0 else p for p in px]
    write_png(f'{A}/textures/block/hex/runes.png', wid, hei, stone)
    write_png(f'{A}/textures/block/hex/runes_ink.png', wid, hei, ink)
else:
    print('original jar not found, art not refreshed:', JAR)

# Destabilized crystal (original art): a pale, faceted crystal texture for the tint to colour.
px = []
for y in range(16):
    for x in range(16):
        band = (x + y) % 6
        v = 235 - band * 12 - (y * 3)
        edge = x in (0, 15)
        px.append((v - 40 if edge else v, v - 40 if edge else v, min(255, v + 10), 200))
write_png(f'{A}/textures/block/destabilized_crystal.png', 16, 16, px)

print('phase 6a generated')
