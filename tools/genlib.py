"""Shared helpers for the Concilium data generators (tools/gen_phase*.py). New phase generators import this instead of
redefining the helpers; the existing generators still carry their own copies (unchanged).

    import sys, os; sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    from genlib import *

Run generators from the module root (mods/thaumic-concilium): ROOT is the relative 'src/main/resources'.
Importing has no side effects (no files read or written, the original jar is opened lazily by jar()).

Constants: NS, T, ROOT, A (assets dir), CAT (research tab), PRIMALS, JAR_PATH.

JSON and ids:  w, r, asp, it, aspects, crystal, gate
Translation:   note, instab, arcane_costs
Research:      entry
Recipes:       infusion, crucible, arcane_shaped, arcane_shapeless, rift_recipe, spell, focus_result
Data maps:     add_entity_aspects, add_item_aspects
Spawns:        add_spawns
Models:        gen, spawn_egg_model, obj_item_model, disp, box, block_state_simple, drop_self_loot
Art:           write_png, read_png, jar, jar_read, copy_from_jar, obj_recentered
"""
import json, math, os, struct, zipfile, zlib

NS = 'thaumicconcilium'
T = 'thaumaturge'
ROOT = 'src/main/resources'
A = os.path.join(ROOT, f'assets/{NS}')
CAT = f'{NS}:concilium'
PRIMALS = ['aer', 'terra', 'ignis', 'aqua', 'ordo', 'perditio']
JAR_PATH = os.environ.get('TC_ORIGINAL_JAR', '../../reference/jars/legacy-1.7.10/ThaumicConcilium-1.1.1.jar')


# ---------------------------------------------------------------- json and ids
def w(path, obj):
    """Write obj as indented JSON (trailing newline) under ROOT, creating folders. w('data/thaumicconcilium/recipe/x.json', {...})"""
    p = os.path.join(ROOT, path); os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w') as f:
        json.dump(obj, f, indent=2); f.write('\n')

def r(path):
    """Read a JSON file under ROOT (for read-modify-write of shared maps). ba = r('data/thaumaturge/data_maps/item/base_aspects.json')"""
    with open(os.path.join(ROOT, path)) as f:
        return json.load(f)

def asp(a):
    """Aspect id: bare names get the thaumaturge namespace. asp('sensus') -> 'thaumaturge:sensus'; asp('forbiddenmagic:ira') unchanged."""
    return a if ':' in a else f'{T}:{a}'

def it(i):
    """Item ingredient. it('ingot_thaumium') -> {'item': 'thaumaturge:ingot_thaumium'}; it('minecraft:string') keeps its namespace."""
    return {'item': i if ':' in i else f'{T}:{i}'}

def aspects(pairs):
    """Aspect list for recipes and data maps. aspects([('sensus', 10), ('auram', 5)]) -> [{'aspect': 'thaumaturge:sensus', 'amount': 10}, ...]"""
    return [{'aspect': asp(a), 'amount': n} for a, n in pairs]

def crystal(a):
    """Essentia crystal ingredient of an aspect (TC4 'wispy essence'). crystal('victus')"""
    return {'type': 'neoforge:components', 'items': f'{T}:essentia_crystal',
            'components': {f'{T}:crystal_aspect': {'aspect': asp(a)}}}

def gate(key, stage=1):
    """Research gate of a recipe: gate('thaumdrum') -> {'entry': 'thaumicconcilium:thaumdrum', 'stage': 1}; stage=None omits the stage."""
    g = {'entry': f'{NS}:{key}'}
    if stage is not None:
        g['stage'] = stage
    return g


# ---------------------------------------------------------------- translation rules from TC4
def note(n):
    """Research note amount from a TC4 aspect amount (divide by 2.5, at least 1). note(10) -> 4"""
    return max(1, round(n / 2.5))

def instab(tc4):
    """Infusion instability from TC4 (divide by 2.5, between 1 and 6). instab(10) -> 4"""
    return max(1, min(6, round(tc4 / 2.5)))

def arcane_costs(costs):
    """Arcane workbench cost fields: vis = sum of the costs, one crystal per 10 of each aspect.
    **arcane_costs([('perditio', 50), ('ordo', 50)]) -> {'vis': 100, 'crystals': [{'aspect': ..., 'amount': 5}, ...]}"""
    return {'vis': sum(n for _, n in costs), 'crystals': [{'aspect': asp(a), 'amount': math.ceil(n / 10)} for a, n in costs]}


# ---------------------------------------------------------------- research
def entry(name, col, row, icon, parents, notes, recipes=(), warp=0, meta=None, complexity=2, stages=2, autounlock=False,
          addenda=None):
    """Write data/thaumicconcilium/thaumaturge/research_entry/<name>.json (lang keys research.thaumicconcilium.<name>.title / .stage_N
    go in gen_lang.py). parents: ids, '~id' = hidden parent, a bare CAT for roots. notes: [(aspect, TC4 amount)] (converted by note()).
    recipes: ids like 'thaumicconcilium:infusion/x' shown on the last stage. meta: ['hidden', 'round', 'spiky'].
    entry('thaumdrum', -6, 7, 'thaumicconcilium:thaumaturge_drum', ['thaumicconcilium:vis_conductor_focus'],
          [('sensus', 10), ('auram', 20)], ['thaumicconcilium:infusion/thaumaturge_drum'], warp=3, meta=['hidden'])"""
    st = []
    for i in range(stages):
        s = {'text': f'research.{NS}.{name}.stage_{i}'}
        if i == 0 and not autounlock:
            s['required_knowledge'] = [{'type': 'theory', 'category': CAT, 'amount': 1}]
        st.append(s)
    if warp:
        st[0]['warp'] = warp
    if recipes:
        st[-1]['recipes'] = list(recipes)
    obj = {'category': CAT, 'name': f'research.{NS}.{name}.title', 'parents': list(parents), 'column': col, 'row': row,
           'icons': [icon], 'stages': st,
           'note_aspects': [{'aspect': asp(a), 'amount': note(n)} for a, n in notes], 'complexity': complexity}
    m = list(meta or []) + (['autounlock'] if autounlock else [])
    if m:
        obj['meta'] = m
    if addenda:
        obj['addenda'] = addenda
    w(f'data/{NS}/thaumaturge/research_entry/{name}.json', obj)


# ---------------------------------------------------------------- recipes
def infusion(key, research, aspect_pairs, catalyst, components, tc4_instability, result, stage=1, result_obj=None):
    """data/.../recipe/infusion/<key>.json gated on research entry <research>. result is an item name in NS, or pass result_obj
    (a full result dict, e.g. focus_result(...)). infusion('x', 'x', [('sensus', 128)], it('minecraft:note_block'), [it('fabric')], 10, 'x')"""
    w(f'data/{NS}/recipe/infusion/{key}.json', {
        'type': f'{T}:infusion', 'catalyst': catalyst, 'components': components, 'aspects': aspects(aspect_pairs),
        'instability': instab(tc4_instability), 'research': gate(research, stage),
        'result': result_obj or {'count': 1, 'id': f'{NS}:{result}'}})

def crucible(key, research, aspect_pairs, catalyst, result, stage=1):
    """data/.../recipe/crucible/<key>.json. stage=None leaves the recipe ungated by stage.
    crucible('bottle_of_clear_water', 'bottle_of_clear_water', [('aqua', 1)], it('phial'), 'bottle_of_clear_water', stage=None)"""
    w(f'data/{NS}/recipe/crucible/{key}.json', {
        'type': f'{T}:crucible', 'aspects': aspects(aspect_pairs), 'catalyst': catalyst,
        'research': gate(research, stage), 'result': {'count': 1, 'id': f'{NS}:{result}'}})

def arcane_shaped(key, research, pattern, keys, costs, result, count=1):
    """Shaped arcane workbench recipe; costs [(aspect, vis)] via arcane_costs.
    arcane_shaped('shard_mill', 'shard_mill', ['GLG', 'WCW', 'GWG'], {'G': it('minecraft:gold_ingot')}, [('perditio', 50)], 'shard_mill')"""
    w(f'data/{NS}/recipe/arcane_workbench/{key}.json', {
        'type': f'{T}:arcane_workbench_shaped', 'pattern': pattern, 'key': keys, **arcane_costs(costs),
        'research': gate(research), 'result': {'count': count, 'id': f'{NS}:{result}'}})

def arcane_shapeless(key, research, ingredients, costs, result, count=1):
    """Shapeless arcane workbench recipe. arcane_shapeless('destabilized_crystal', 'destabilized_crystal', [it('essentia_crystal')] * 9,
    [(a, 50) for a in PRIMALS], 'destabilized_crystal')"""
    w(f'data/{NS}/recipe/arcane_workbench/{key}.json', {
        'type': f'{T}:arcane_workbench_shapeless', 'ingredients': ingredients, **arcane_costs(costs),
        'research': gate(research), 'result': {'count': count, 'id': f'{NS}:{result}'}})

def rift_recipe(key, catalyst, aspect_pairs, research, result):
    """Rift recipe (an item thrown on a hex whose chained rift holds the essentia); research is a bare entry id string, not a gate.
    rift_recipe('astral_monitor', it('thaumometer'), [('sensus', 128), ('vacuos', 128)], 'astral_monitor', 'astral_monitor')"""
    w(f'data/{NS}/recipe/rift/{key}.json', {
        'type': f'{NS}:rift', 'catalyst': catalyst, 'aspects': aspects(aspect_pairs),
        'research': f'{NS}:{research}', 'result': {'count': 1, 'id': f'{NS}:{result}'}})

def spell(style, part, settings=None):
    """Spell tree of one part under the origin. spell('channeled', 'vis_conductor', {'upgrade': 1})"""
    node = {'part': f'{NS}:{part}'}
    if settings:
        node['settings'] = settings
    return {'style': style, 'root': {'part': f'{T}:origin', 'children': [node]}}

def focus_result(key, sp):
    """Result dict of a ready-inscribed focus named after research entry <key>. focus_result('vis_conductor_focus', spell('channeled', 'vis_conductor'))"""
    return {'count': 1, 'id': f'{T}:focus_1', 'components': {
        f'{T}:spell': sp,
        'minecraft:custom_name': json.dumps({'translate': f'{NS}.focus.preset', 'with': [{'translate': f'research.{NS}.{key}.title'}]})}}


# ---------------------------------------------------------------- data maps (read-modify-write; run after gen_phase4_data.py)
def add_entity_aspects(entity, pairs):
    """Add scan aspects for an entity type. add_entity_aspects('quicksilver_elemental', [('humanus', 10), ('metallum', 10)])"""
    p = f'data/{T}/data_maps/entity_type/entity_aspects.json'
    m = r(p); m['values'][f'{NS}:{entity}'] = aspects(pairs); w(p, m)

def add_item_aspects(item, pairs):
    """Add base aspects for one of our items. add_item_aspects('thaumaturge_drum', [('sensus', 10), ('instrumentum', 10)])"""
    p = f'data/{T}/data_maps/item/base_aspects.json'
    m = r(p); m['values'][f'{NS}:{item}'] = aspects(pairs); w(p, m)


# ---------------------------------------------------------------- spawns
def add_spawns(name, entity, weight, lo, hi, biomes='#minecraft:is_overworld', cond=None):
    """Biome modifier adding a natural spawn. cond = a list of neoforge conditions. Also needs a spawn placement (TCSpawns.placements).
    add_spawns('quicksilver_elemental', 'quicksilver_elemental', 5, 1, 2)"""
    w(f'data/{NS}/neoforge/biome_modifier/{name}.json', ({'neoforge:conditions': cond} if cond else {}) | {
        'type': 'neoforge:add_spawns', 'biomes': biomes,
        'spawners': [{'type': f'{NS}:{entity}', 'weight': weight, 'minCount': lo, 'maxCount': hi}]})


# ---------------------------------------------------------------- models and blockstates
def gen(name, tex=None):
    """Flat item model (item/generated). gen('coagulating_eye_drops'); gen('x', tex='other_texture')"""
    w(f'assets/{NS}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{tex or name}'}})

def spawn_egg_model(entity):
    """Spawn egg model for <entity>_spawn_egg. spawn_egg_model('quicksilver_elemental')"""
    w(f'assets/{NS}/models/item/{entity}_spawn_egg.json', {'parent': 'minecraft:item/template_spawn_egg'})

def disp(tr, rot, sc=None):
    """One display transform. disp([0, 2.5, 0], [75, 45, 0], 0.3). sc is a uniform scale or None."""
    d = {'translation': tr, 'rotation': rot}
    if sc:
        d['scale'] = [sc] * 3
    return d

def obj_item_model(name, texture_item=None, display=None, block_texture=False):
    """Item model from an OBJ at models/item/<name>.obj (one material 'tex'). display = dict of transforms (see disp).
    obj_item_model('thaumaturge_drum', display={'gui': disp([0, -4, 0], [30, 225, 0], 0.35)})"""
    tex = f'{NS}:{"block" if block_texture else "item"}/{texture_item or name}'
    w(f'assets/{NS}/models/item/{name}.json', {
        'loader': 'neoforge:obj', 'model': f'{NS}:models/item/{name}.obj', 'flip_v': True, 'automatic_culling': False,
        'textures': {'tex': tex, 'particle': tex}, **({'display': display} if display else {})})

def box(f, t, tex='#side'):
    """One cuboid element with the same texture on all faces (pixels 0-16). box([0, 0, 0], [16, 4, 16])"""
    return {'from': f, 'to': t, 'faces': {d: {'texture': tex} for d in ('north', 'south', 'east', 'west', 'up', 'down')}}

def block_state_simple(name, model=None):
    """Blockstate with one model. block_state_simple('vis_condenser')"""
    w(f'assets/{NS}/blockstates/{name}.json', {'variants': {'': {'model': model or f'{NS}:block/{name}'}}})

def drop_self_loot(name):
    """Loot table that drops the block itself (survives_explosion). drop_self_loot('lithographer')"""
    w(f'data/{NS}/loot_table/blocks/{name}.json', {
        'type': 'minecraft:block', 'random_sequence': f'{NS}:blocks/{name}',
        'pools': [{'rolls': 1.0, 'bonus_rolls': 0.0, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
                   'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{name}'}]}]})


# ---------------------------------------------------------------- art: PNG
def write_png(path, wid, hei, px):
    """Write an RGBA PNG; px is a flat list of (r, g, b, a) tuples, row by row. write_png('x.png', 16, 16, [(0, 0, 0, 0)] * 256)"""
    raw = b''.join(b'\x00' + b''.join(struct.pack('BBBB', *px[y * wid + x]) for x in range(wid)) for y in range(hei))
    def chunk(t, d): return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', wid, hei, 8, 6, 0, 0, 0))
                + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))

def read_png(data):
    """Decode PNG bytes (8-bit, non-interlaced, any colour type) -> (width, height, [(r, g, b, a), ...]). w_, h_, px = read_png(jar_read('assets/..png'))"""
    assert data[:8] == b'\x89PNG\r\n\x1a\n'
    pos = 8; idat = b''; pal = trns = b''
    while pos < len(data):
        n, = struct.unpack('>I', data[pos:pos + 4]); t = data[pos + 4:pos + 8]; d = data[pos + 8:pos + 8 + n]; pos += 12 + n
        if t == b'IHDR': wid, hei, bd, ct, _, _, il = struct.unpack('>IIBBBBB', d)
        elif t == b'PLTE': pal = d
        elif t == b'tRNS': trns = d
        elif t == b'IDAT': idat += d
    ch = {6: 4, 2: 3, 0: 1, 4: 2, 3: 1}[ct]; assert bd == 8 and il == 0
    raw = zlib.decompress(idat); stride = wid * ch; rows = []; prev = bytearray(stride); p = 0
    for y in range(hei):
        f = raw[p]; line = bytearray(raw[p + 1:p + 1 + stride]); p += 1 + stride
        for i in range(stride):
            a = line[i - ch] if i >= ch else 0; b = prev[i]; c = prev[i - ch] if i >= ch else 0
            if f == 1: line[i] = (line[i] + a) & 255
            elif f == 2: line[i] = (line[i] + b) & 255
            elif f == 3: line[i] = (line[i] + (a + b) // 2) & 255
            elif f == 4:
                pa = abs(b - c); pb = abs(a - c); pc = abs(a + b - 2 * c)
                line[i] = (line[i] + (a if pa <= pb and pa <= pc else (b if pb <= pc else c))) & 255
        rows.append(line); prev = line
    px = []
    for line in rows:
        for x in range(wid):
            if ct == 6: px.append(tuple(line[x * 4:x * 4 + 4]))
            elif ct == 2: px.append(tuple(line[x * 3:x * 3 + 3]) + (255,))
            elif ct == 3:
                i = line[x]; px.append(tuple(pal[i * 3:i * 3 + 3]) + ((trns[i] if i < len(trns) else 255),))
            elif ct == 0: px.append((line[x],) * 3 + (255,))
            else: px.append((line[x * 2],) * 3 + (line[x * 2 + 1],))
    return wid, hei, px


# ---------------------------------------------------------------- art: the original 1.7.10 jar (WTFPL), opened lazily
_jar = None

def jar():
    """The original ThaumicConcilium-1.1.1.jar as a ZipFile, or None when it is not there (art is then not refreshed)."""
    global _jar
    if _jar is None and os.path.exists(JAR_PATH):
        _jar = zipfile.ZipFile(JAR_PATH)
    return _jar

def jar_read(name):
    """Bytes of assets/thaumicconcilium/<name> from the original jar. jar_read('textures/entity/quicksilver_elemental.png')"""
    return jar().read(f'assets/{NS}/{name}')

def copy_from_jar(mapping):
    """Copy original files into our assets: {'textures/blocks/quicksilver.png': 'block/quicksilver.png'} maps jar path (under
    assets/thaumicconcilium) to path under textures/. Does nothing, with a message, when the jar is missing."""
    z = jar()
    if z is None:
        print('original jar not found, art not refreshed:', JAR_PATH); return
    for src, dst in mapping.items():
        out = os.path.join(A, 'textures', dst); os.makedirs(os.path.dirname(out), exist_ok=True)
        with open(out, 'wb') as f:
            f.write(z.read(f'assets/{NS}/{src}'))

def obj_recentered(src, dst_name, folder='item', shift=0.5):
    """Copy the original models/<src>.obj to assets/models/<folder>/<dst_name>.obj + .mtl with one material 'tex' and every vertex
    shifted by +shift (the original's models are centred on the origin, an item model's origin is the block corner).
    obj_recentered('auram_thaum_drum', 'thaumaturge_drum')"""
    lines = []
    for line in jar().read(f'assets/{NS}/models/{src}.obj').decode('utf-8').splitlines():
        if line.startswith('mtllib'): lines.append(f'mtllib {dst_name}.mtl')
        elif line.startswith('usemtl'): lines.append('usemtl tex')
        elif line.startswith('v '):
            x, y, z = (float(v) + shift for v in line.split()[1:4])
            lines.append(f'v {x:.6f} {y:.6f} {z:.6f}')
        else: lines.append(line)
    base = os.path.join(A, 'models', folder); os.makedirs(base, exist_ok=True)
    with open(os.path.join(base, f'{dst_name}.obj'), 'w', newline='\n') as f:
        f.write('\n'.join(lines) + '\n')
    with open(os.path.join(base, f'{dst_name}.mtl'), 'w', newline='\n') as f:
        f.write('newmtl tex\nmap_Kd #tex\n')
