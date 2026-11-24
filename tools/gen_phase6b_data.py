"""Generates phase-6b (first cycle) datapack JSON and assets: Xylography, Thaumaturge's and the Warped Drum, the
Polishment Device, Coagulating Eye Drops, the Quicksilver Elemental and the Quicksilver Reactor. Run from the repo root
after gen_phase6a_data.py and before gen_lang.py (it extends the entity and item aspect data maps phase 4 writes, so
it must come after gen_phase4_data.py).

Translated from Thaumonomicon / TCRecipeRegistry (Concilium 1.1.1): TC4 aspect amounts kept, instability / 2.5 capped
at 6, research notes = TC4 amount / 2.5. Wispy essences are essentia crystals of their aspect. Art is the original's
(WTFPL): the drums' OBJ models and textures, the reactor's side and mercury textures, the elemental's skin, the eye
drops' animated strip. The Polishment Device's icon is drawn here (the original borrowed Thaumcraft 4's).
Cycle 2 adds the Astral Monitor (rift recipe, research, item model referencing Thaumaturge's scanner mesh) and the
Lithographer (research, block, models, loot). Cycle 3 writes the twelve polishment entries (data/thaumicconcilium/thaumicconcilium/polishment).
"""
import json, os, struct, zipfile, zlib
NS = 'thaumicconcilium'
ROOT = 'src/main/resources'
T = 'thaumaturge'
JAR = os.environ.get('TC_ORIGINAL_JAR', '../../reference/jars/legacy-1.7.10/ThaumicConcilium-1.1.1.jar')


def w(path, obj):
    p = os.path.join(ROOT, path); os.makedirs(os.path.dirname(p), exist_ok=True)
    json.dump(obj, open(p, 'w'), indent=2); open(p, 'a').write('\n')
def r(path): return json.load(open(os.path.join(ROOT, path)))
def asp(a): return a if ':' in a else f'{T}:{a}'
def note(n): return max(1, round(n / 2.5))
def instab(tc4): return max(1, min(6, round(tc4 / 2.5)))
def it(i): return {'item': i if ':' in i else f'{T}:{i}'}
def aspects(pairs): return [{'aspect': asp(a), 'amount': n} for a, n in pairs]
def crystal(a):
    return {'type': 'neoforge:components', 'items': f'{T}:essentia_crystal',
            'components': {f'{T}:crystal_aspect': {'aspect': asp(a)}}}

# ---------------------------------------------------------------- recipes
# WandXylography: a special crafting recipe (item/XylographyRecipe), nothing to configure.
w(f'data/{NS}/recipe/xylography.json', {'type': f'{NS}:xylography', 'category': 'misc'})

# "ThaumDrum": note block + primal charm, leather, fabric, vitium crystal, life essence, thaumium, aura essence, fabric,
# string, arcane stone, string, fabric, magic essence, thaumium; sensus 128, victus 64, auram 96, instrumentum 32; instability 10.
w(f'data/{NS}/recipe/infusion/thaumaturge_drum.json', {
    'type': f'{T}:infusion', 'catalyst': it('minecraft:note_block'),
    'components': [it('primal_charm'), it('minecraft:leather'), it('fabric'), it('crystal_vitium'), crystal('victus'),
                   it('ingot_thaumium'), crystal('auram'), it('fabric'), it('minecraft:string'), it('stone_arcane'),
                   it('minecraft:string'), it('fabric'), crystal('praecantatio'), it('ingot_thaumium')],
    'aspects': aspects([('sensus', 128), ('victus', 64), ('auram', 96), ('instrumentum', 32)]),
    'instability': instab(10), 'research': {'entry': f'{NS}:thaumdrum', 'stage': 1},
    'result': {'count': 1, 'id': f'{NS}:thaumaturge_drum'}})
# "WarpDrum": a chained rift recipe, the drum thrown on a hex whose rift holds the essentia.
w(f'data/{NS}/recipe/rift/warped_drum.json', {
    'type': f'{NS}:rift', 'catalyst': it(f'{NS}:thaumaturge_drum'),
    'aspects': [{'aspect': asp(a), 'amount': n} for a, n in
                [('sensus', 128), ('perditio', 128), ('cognitio', 64), ('bestia', 64), ('vitium', 64)]],
    'research': f'{NS}:warpdrum', 'result': {'count': 1, 'id': f'{NS}:warped_drum'}})
# "EyeDrops": a phial of sensus (TC4: itemEssence meta 1 holding sensus 8; a Thaumaturge phial holds 10) + perditio 4,
# sensus 2, victus 2. The research (the Lithographer's) is written in the next cycle.
w(f'data/{NS}/recipe/crucible/coagulating_eye_drops.json', {
    'type': f'{T}:crucible',
    'catalyst': {'type': 'neoforge:components', 'items': f'{T}:phial',
                 'components': {f'{T}:aspects': [{'aspect': asp('sensus'), 'amount': 10}]}},
    'aspects': aspects([('perditio', 4), ('sensus', 2), ('victus', 2)]),
    'research': {'entry': f'{NS}:lithographer', 'stage': 1},
    'result': {'count': 1, 'id': f'{NS}:coagulating_eye_drops'}})

# ---------------------------------------------------------------- research
CAT = f'{NS}:concilium'
def entry(name, col, row, icon, parents, notes, recipes=(), warp=0, meta=None, complexity=2, stages=2, autounlock=False):
    st = []
    for i in range(stages):
        s = {'text': f'research.{NS}.{name}.stage_{i}'}
        if i == 0 and not autounlock:
            s['required_knowledge'] = [{'type': 'theory', 'category': CAT, 'amount': 1}]
        st.append(s)
    if warp: st[0]['warp'] = warp
    if recipes: st[-1]['recipes'] = list(recipes)
    obj = {'category': CAT, 'name': f'research.{NS}.{name}.title', 'parents': list(parents), 'column': col, 'row': row,
           'icons': [icon], 'stages': st,
           'note_aspects': [{'aspect': asp(a), 'amount': note(n)} for a, n in notes], 'complexity': complexity}
    m = list(meta or []) + (['autounlock'] if autounlock else [])
    if m: obj['meta'] = m
    w(f'data/{NS}/thaumaturge/research_entry/{name}.json', obj)

# THAUMDRUM (-6,7): after the Vis Conductor; hidden until a note block or a wisp has been scanned (original item and
# entity triggers: a scan entry gathers both, any one completes it).
w(f'data/{NS}/thaumaturge/scan/drum.json', {
    'key': f'{NS}:scanned/drum', 'blocks': ['minecraft:note_block'], 'items': ['minecraft:note_block'],
    'entities': [f'{T}:wisp']})
entry('thaumdrum', -6, 7, f'{NS}:thaumaturge_drum', [f'{NS}:vis_conductor_focus', f'{NS}:scanned/drum'],
      [('sensus', 10), ('auram', 20), ('victus', 10)], [f'{NS}:infusion/thaumaturge_drum'], meta=['hidden'])
# WARPDRUM (-5,7): after the drum; hidden the Hex of Predictability; 3 warp.
entry('warpdrum', -5, 7, f'{NS}:warped_drum', [f'{NS}:thaumdrum', f'~{NS}:hex_of_predictability'],
      [('sensus', 10), ('cognitio', 20), ('victus', 10), ('perditio', 10), ('bestia', 10)], warp=3)
# XYLOGRAPHY (-2,-3): auto-unlocked, round, one page.
entry('xylography', -2, -3, f'{T}:wand', [CAT], [('instrumentum', 1), ('cognitio', 1), ('herba', 1)],
      meta=['round'], complexity=2, stages=1, autounlock=True)
# QUICKSILVERCRUCIBLE (-14,3): hidden until the elemental is seen (scanned); completed by one dying in a crucible; 3 warp.
entry('quicksilver_crucible', -14, 3, f'{NS}:quicksilver_reactor',
      [f'{T}:scanned/entity/{NS}/quicksilver_elemental'],
      [('humanus', 10), ('permutatio', 10), ('metallum', 10), ('praecantatio', 10), ('forbiddenmagic:corpus', 10)], warp=3, meta=['hidden'])

# ---------------------------------------------------------------- aspects (scanning)
ea = r(f'data/{T}/data_maps/entity_type/entity_aspects.json')
ea['values'][f'{NS}:quicksilver_elemental'] = aspects([('humanus', 10), ('permutatio', 10), ('metallum', 10), ('praecantatio', 10)])
w(f'data/{T}/data_maps/entity_type/entity_aspects.json', ea)
ba = r(f'data/{T}/data_maps/item/base_aspects.json')
ba['values'][f'{NS}:thaumaturge_drum'] = aspects([('sensus', 10), ('instrumentum', 10), ('auram', 10)])
ba['values'][f'{NS}:warped_drum'] = aspects([('sensus', 10), ('cognitio', 10), ('bestia', 10)])
ba['values'][f'{NS}:coagulating_eye_drops'] = aspects([('sensus', 5), ('victus', 5)])
ba['values'][f'{NS}:polishment_device'] = aspects([('ordo', 10), ('fabrico', 8), ('alienis', 5), ('praecantatio', 3)])   # user 2026-11-22: split of the two proposals
w(f'data/{T}/data_maps/item/base_aspects.json', ba)

# ---------------------------------------------------------------- spawn (original: monster, weight 5, groups 1-2)
w(f'data/{NS}/neoforge/biome_modifier/quicksilver_elemental.json', {
    'type': 'neoforge:add_spawns', 'biomes': '#minecraft:is_overworld',
    'spawners': [{'type': f'{NS}:quicksilver_elemental', 'weight': 5, 'minCount': 1, 'maxCount': 2}]})

# ---------------------------------------------------------------- models
def gen(name, tex=None):
    w(f'assets/{NS}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{tex or name}'}})
gen('coagulating_eye_drops'); gen('polishment_device')
w(f'assets/{NS}/models/item/quicksilver_elemental_spawn_egg.json', {'parent': 'minecraft:item/template_spawn_egg'})
DRUM_DISPLAY = {  # the OBJ is 1 x 2 x 1 blocks; tuned in the render harness (phase6b.json)
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.3, 0.3, 0.3]},
    'thirdperson_lefthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.3, 0.3, 0.3]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [1, -1, 0], 'scale': [0.3, 0.3, 0.3]},
    'firstperson_lefthand': {'rotation': [0, 45, 0], 'translation': [1, -1, 0], 'scale': [0.3, 0.3, 0.3]},
    'gui': {'rotation': [30, 225, 0], 'translation': [0, -4, 0], 'scale': [0.35, 0.35, 0.35]},
    'ground': {'translation': [0, 2, 0], 'scale': [0.25, 0.25, 0.25]},
    'fixed': {'translation': [0, -4, 0], 'scale': [0.4, 0.4, 0.4]},
}
for item in ('thaumaturge_drum', 'warped_drum'):
    w(f'assets/{NS}/models/item/{item}.json', {
        'loader': 'neoforge:obj', 'model': f'{NS}:models/item/{item}.obj', 'flip_v': True, 'automatic_culling': False,
        'textures': {'tex': f'{NS}:item/{item}', 'particle': f'{NS}:item/{item}'}, 'display': DRUM_DISPLAY})

# The reactor: a crucible-shaped basin (floor 4 px, walls 2 px) in the original's side texture; the mercury surface is
# drawn by QuicksilverReactorRenderer.
def box(f, t): return {'from': f, 'to': t, 'faces': {d: {'texture': '#side'} for d in ('north', 'south', 'east', 'west', 'up', 'down')}}
w(f'assets/{NS}/models/block/quicksilver_reactor.json', {
    'parent': 'minecraft:block/block', 'ambientocclusion': False,
    'textures': {'side': f'{NS}:block/quicksilver_crucible', 'particle': f'{NS}:block/quicksilver_crucible'},
    'elements': [box([0, 0, 0], [16, 4, 16]), box([0, 4, 0], [16, 16, 2]), box([0, 4, 14], [16, 16, 16]),
                 box([0, 4, 2], [2, 16, 14]), box([14, 4, 2], [16, 16, 14])]})
w(f'assets/{NS}/blockstates/quicksilver_reactor.json', {'variants': {'': {'model': f'{NS}:block/quicksilver_reactor'}}})
w(f'assets/{NS}/models/item/quicksilver_reactor.json', {'parent': f'{NS}:block/quicksilver_reactor'})

# ---------------------------------------------------------------- cycle 2: Astral Monitor and Lithographer
# "AstralMonitor": a thaumometer thrown on a hex whose chained rift holds sensus 128, vacuos 128, cognitio 64.
w(f'data/{NS}/recipe/rift/astral_monitor.json', {
    'type': f'{NS}:rift', 'catalyst': it('thaumometer'),
    'aspects': [{'aspect': asp(a), 'amount': n} for a, n in [('sensus', 128), ('vacuos', 128), ('cognitio', 64)]],
    'research': f'{NS}:astral_monitor', 'result': {'count': 1, 'id': f'{NS}:astral_monitor'}})
# ASTRALMONITOR (-12,3): after the Hex of Predictability. LITHOGRAPHER (-13,5): after the monitor, hidden the advanced
# alchemical furnace; 2 warp. Both complexity 3 as in the original.
entry('astral_monitor', -12, 3, f'{NS}:astral_monitor', [f'{NS}:hex_of_predictability'],
      [('sensus', 15), ('vitreus', 30), ('instrumentum', 15), ('spiritus', 30), ('vacuos', 10)], complexity=3)
entry('lithographer', -13, 5, f'{NS}:textures/research/lithographer.png',
      [f'{NS}:astral_monitor', f'~{T}:advanced_alchemical_furnace'],
      [('sensus', 15), ('machina', 10), ('vacuos', 10), ('alienis', 10)], [f'{NS}:crucible/coagulating_eye_drops'],
      warp=2, complexity=3)

ba = r(f'data/{T}/data_maps/item/base_aspects.json')
ba['values'][f'{NS}:astral_monitor'] = aspects([('sensus', 10), ('instrumentum', 10), ('spiritus', 10)])
ba['values'][f'{NS}:lithographer'] = aspects([('sensus', 10), ('machina', 20), ('metallum', 20)])
w(f'data/{T}/data_maps/item/base_aspects.json', ba)

# Loot: the cycle-1 and 6a machines had none, so breaking them dropped nothing. Each drops itself.
for name in ('lithographer', 'vis_condenser', 'quicksilver_reactor'):
    w(f'data/{NS}/loot_table/blocks/{name}.json', {
        'type': 'minecraft:block', 'random_sequence': f'{NS}:blocks/{name}',
        'pools': [{'rolls': 1.0, 'bonus_rolls': 0.0, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
                   'entries': [{'type': 'minecraft:item', 'name': f'{NS}:{name}'}]}]})

# Astral Monitor model: drawn by AstralMonitorRenderer (builtin/entity, Thaumaturge thaumometer display transforms).
def disp(tr, rot, sc=None):
    d = {'translation': tr, 'rotation': rot}
    if sc: d['scale'] = [sc] * 3
    return d
MON_DISPLAY = {
    # Thaumaturge draws its own thaumometer in first person with a hand renderer; ours goes through these, so they
    # are our own (the scanner mesh is 2 blocks across). Tuned in the render harness (phase6b.json).
    'firstperson_righthand': disp([-1.5, 2.5, -2], [70, 0, 0], 0.3),
    'firstperson_lefthand': disp([1.5, 2.5, -2], [70, 0, 0], 0.3),
    'thirdperson_righthand': disp([-1.6, 1.6, 0], [90, 0, -90], 0.2),
    'thirdperson_lefthand': disp([-1.6, 1.6, 0], [90, 0, -90], 0.2),
    'gui': disp([0, 0, -2.8], [90, 0, 0], 0.35),
    'ground': disp([0, 3.2, 0], [90, 0, 0], 0.2),
    'fixed': disp([0, 0, 0.48], [-90, 0, 0], 0.4),
    'head': disp([0, 0, 0], [90, 0, 0], 0.45),
}
w(f'assets/{NS}/models/item/astral_monitor.json', {'parent': 'minecraft:builtin/entity', 'display': MON_DISPLAY})
# The body is Thaumaturge's scanner mesh (referenced, not copied) with the original's frame texture.
w(f'assets/{NS}/models/item/astral_monitor_frame.json', {
    'loader': f'{T}:mesh', 'model': f'{T}:models/mesh/scanner.ttmesh', 'flip_v': True,
    'textures': {'body': f'{NS}:item/astral_frame', 'pane': f'{NS}:item/astral_screen', 'particle': f'{NS}:item/astral_frame'}})

# Lithographer: a BlockEntityRenderer draws the block; the item is the idle machine as one static OBJ.
w(f'assets/{NS}/blockstates/lithographer.json', {'variants': {
    'axis=x': {'model': f'{NS}:block/lithographer'}, 'axis=z': {'model': f'{NS}:block/lithographer'}}})
w(f'assets/{NS}/models/block/lithographer.json', {'textures': {'particle': f'{NS}:block/lithographer'}})
w(f'assets/{NS}/models/item/lithographer.json', {
    'loader': 'neoforge:obj', 'model': f'{NS}:models/item/lithographer.obj', 'flip_v': True, 'automatic_culling': False,
    'textures': {'tex': f'{NS}:block/lithographer', 'particle': f'{NS}:block/lithographer'},
    'display': {
        'gui': {'rotation': [30, 225, 0], 'scale': [0.4, 0.4, 0.4]},
        'ground': {'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
        'fixed': {'scale': [0.4, 0.4, 0.4]},
        'thirdperson_righthand': {'scale': [0.3, 0.3, 0.3]}, 'thirdperson_lefthand': {'scale': [0.3, 0.3, 0.3]},
        'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.3, 0.3, 0.3]},
        'firstperson_lefthand': {'rotation': [0, 45, 0], 'scale': [0.3, 0.3, 0.3]}}})

# ---------------------------------------------------------------- cycle 3: polished infusions (the gate)
# TCRecipeRegistry 1364-1375, translated like the recipes (telum -> aversio, sano -> victus, iter -> motus, ...).
# Named after the research entry; gen_lang.py appends the requirement to that entry's recipe page. Protobody: phase 8.
FOCUS = f'{T}:focus_1'
POLISH = [
    ('cooling_focus', FOCUS, 'flurry', 'aversio', 64),
    ('dump_jackboots', f'{NS}:dump_jackboots', None, 'motus', 128),
    ('wrath_attraction_focus', FOCUS, 'wrath_attraction', 'cognitio', 128),
    ('taint_animation_focus', FOCUS, 'taint_animation', 'thaumadditions:infernum', 256),
    ('reflection_focus', FOCUS, 'reflection', 'vinculum', 256),
    ('tight_belt', f'{NS}:tight_belt', None, 'volatus', 128),
    ('burdening_amulet', f'{NS}:burdening_amulet', None, 'sensus', 64),
    ('warp_extraction_focus', FOCUS, 'warp_extraction', 'victus', 512),
    ('ring_of_blustering_light', f'{NS}:ring_of_blustering_light', None, 'tenebrae', 512),
    ('spotlight_focus', FOCUS, 'spotlight', 'perditio', 1024),
    ('scribing_focus', FOCUS, 'scribing', 'fabrico', 128),
    ('ethereal_manipulator_focus', FOCUS, 'ethereal_manipulator', 'forbiddenmagic:desidia', 128),
]
for name, item, part, aspect, amount in POLISH:
    res = {'item': item}
    if part: res['spell_part'] = f'{NS}:{part}'
    w(f'data/{NS}/{NS}/polishment/{name}.json', {'result': res, 'aspect': asp(aspect), 'amount': amount})

# ---------------------------------------------------------------- art
def write_png(path, wid, hei, px):
    raw = b''.join(b'\x00' + b''.join(struct.pack('BBBB', *px[y * wid + x]) for x in range(wid)) for y in range(hei))
    def chunk(t, d): return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'wb').write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', wid, hei, 8, 6, 0, 0, 0))
                           + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))

# Minimal PNG reader (8-bit, non-interlaced), for recolouring the original's art.
def read_png(data):
    assert data[:8]==b'\x89PNG\r\n\x1a\n'
    pos=8; idat=b''; 
    while pos<len(data):
        n,=struct.unpack('>I',data[pos:pos+4]); t=data[pos+4:pos+8]; d=data[pos+8:pos+8+n]; pos+=12+n
        if t==b'IHDR': w,h,bd,ct,_,_,il=struct.unpack('>IIBBBBB',d)
        elif t==b'PLTE': pal=d
        elif t==b'tRNS': trns=d
        elif t==b'IDAT': idat+=d
    ch={6:4,2:3,0:1,4:2,3:1}[ct]; assert bd==8 and il==0
    raw=zlib.decompress(idat); stride=w*ch; rows=[]; prev=bytearray(stride); p=0
    for y in range(h):
        f=raw[p]; line=bytearray(raw[p+1:p+1+stride]); p+=1+stride
        for i in range(stride):
            a=line[i-ch] if i>=ch else 0; b=prev[i]; c=prev[i-ch] if i>=ch else 0
            if f==1: line[i]=(line[i]+a)&255
            elif f==2: line[i]=(line[i]+b)&255
            elif f==3: line[i]=(line[i]+(a+b)//2)&255
            elif f==4:
                pa=abs(b-c); pb=abs(a-c); pc=abs(a+b-2*c)
                pr=a if pa<=pb and pa<=pc else (b if pb<=pc else c)
                line[i]=(line[i]+pr)&255
        rows.append(line); prev=line
    px=[]
    for line in rows:
        for x in range(w):
            if ct==6: px.append(tuple(line[x*4:x*4+4]))
            elif ct==2: px.append(tuple(line[x*3:x*3+3])+(255,))
            elif ct==3:
                i=line[x]; px.append(tuple(pal[i*3:i*3+3])+((trns[i] if i<len(trns) else 255),))
            elif ct==0: px.append((line[x],)*3+(255,))
            else: px.append((line[x*2],)*3+(line[x*2+1],))
    return w,h,px

A = os.path.join(ROOT, f'assets/{NS}')
if os.path.exists(JAR):
    z = zipfile.ZipFile(JAR)
    def drum_obj(src, dst):
        # One material "tex"; vertices shifted +0.5 (the original's models are centred on the origin, an item model's
        # origin is the block corner, which the display transforms pivot around).
        lines = []
        for line in z.read(f'assets/{NS}/models/{src}.obj').decode('utf-8').splitlines():
            if line.startswith('mtllib'):
                lines.append(f'mtllib {os.path.basename(dst)}.mtl')
            elif line.startswith('usemtl'):
                lines.append('usemtl tex')
            elif line.startswith('v '):
                x, y, z_ = (float(v) + 0.5 for v in line.split()[1:4])
                lines.append(f'v {x:.6f} {y:.6f} {z_:.6f}')
            else:
                lines.append(line)
        path = f'{A}/models/item/{dst}.obj'
        os.makedirs(os.path.dirname(path), exist_ok=True)
        open(path, 'w', newline='\n').write('\n'.join(lines) + '\n')
        open(f'{A}/models/item/{dst}.mtl', 'w', newline='\n').write('newmtl tex\nmap_Kd #tex\n')
    drum_obj('auram_thaum_drum', 'thaumaturge_drum')
    drum_obj('warped_thaum_drum', 'warped_drum')
    COPY = {
        'textures/models/auram_thaum_drum.png': 'item/thaumaturge_drum.png',
        'textures/models/warped_thaum_drum.png': 'item/warped_drum.png',
        'textures/items/coagulating_eye_drops.png': 'item/coagulating_eye_drops.png',
        'textures/items/coagulating_eye_drops.png.mcmeta': 'item/coagulating_eye_drops.png.mcmeta',
        'textures/blocks/quicksilver_crucible.png': 'block/quicksilver_crucible.png',
        'textures/blocks/quicksilver.png': 'block/quicksilver.png',
        'textures/entity/quicksilver_elemental.png': 'entity/quicksilver_elemental.png',
    }
    for src, dst in COPY.items():
        out = f'{A}/textures/{dst}'; os.makedirs(os.path.dirname(out), exist_ok=True)
        data = z.read(f'assets/{NS}/{src}')
        open(out, 'wb').write(data)

    # Cycle 2 art (original, WTFPL): lithographer OBJs, their texture, the GUIs and the astral monitor frame.
    def obj_text(name): return z.read(f'assets/{NS}/models/{name}.obj').decode('utf-8')
    LD = f'{A}/models/block/lithographer'; os.makedirs(LD, exist_ok=True)
    for src, dst in [('lithographer', 'base'), ('lithographer1', 'sector1'), ('lithographer2', 'sector2'),
                     ('lithographer3', 'sector3'), ('lithographer4', 'sector4')]:
        open(f'{LD}/{dst}.obj', 'w', newline='\n').write(obj_text(src))
    # The item model: base plus the first sector in one OBJ, one material, moved up half a block and across to the
    # block middle (an item model origin is the block corner).
    vs, vts, vns, fs = [], [], [], []
    for src in ('lithographer', 'lithographer1'):
        v0, t0, n0 = len(vs), len(vts), len(vns)
        for line in obj_text(src).splitlines():
            p = line.split()
            if not p: continue
            if p[0] == 'v': vs.append((float(p[1]) + 0.5, float(p[2]) + 0.5, float(p[3]) + 0.5))
            elif p[0] == 'vt': vts.append(line)
            elif p[0] == 'vn': vns.append(line)
            elif p[0] == 'f':
                parts = []
                for c in p[1:]:
                    i = c.split('/')
                    i[0] = str(int(i[0]) + v0)
                    if len(i) > 1 and i[1]: i[1] = str(int(i[1]) + t0)
                    if len(i) > 2 and i[2]: i[2] = str(int(i[2]) + n0)
                    parts.append('/'.join(i))
                fs.append('f ' + ' '.join(parts))
    os.makedirs(f'{A}/models/item', exist_ok=True)
    open(f'{A}/models/item/lithographer.obj', 'w', newline='\n').write(
        'mtllib lithographer.mtl\no lithographer\n' + '\n'.join(f'v {x:.6f} {y:.6f} {zz:.6f}' for x, y, zz in vs) + '\n'
        + '\n'.join(vts) + '\n' + '\n'.join(vns) + '\nusemtl tex\n' + '\n'.join(fs) + '\n')
    open(f'{A}/models/item/lithographer.mtl', 'w', newline='\n').write('newmtl tex\nmap_Kd #tex\n')
    for src, dst in {
        'textures/models/lithographer.png': 'block/lithographer.png',
        'textures/models/atsral_frame.png': 'item/astral_frame.png',
        'textures/misc/lithographer.png': 'research/lithographer.png',
        'textures/gui/astral_monitor.png': 'gui/astral_monitor.png',
        'textures/gui/lithographer.png': 'gui/lithographer.png',
        'textures/gui/astral_hole.png': 'gui/astral_hole.png',
        'textures/gui/star.png': 'gui/star.png',
        'textures/gui/constellations.png': 'gui/constellations.png',
    }.items():
        out = f'{A}/textures/{dst}'; os.makedirs(os.path.dirname(out), exist_ok=True)
        data = z.read(f'assets/{NS}/{src}')
        if dst == 'item/astral_frame.png':
            # The original's frame shader swapped the pure-blue gem texels (blue 255..250 for gems 0..5, a 7x7 box each)
            # for one flat colour per gem; the outline and highlight around/inside them are plain texture. Here the frame
            # keeps a dark socket under each gem, and every gem is cut out as a 9x9 sprite (box plus one texel): body
            # texels white (astral_gems_body.png, tinted by the renderer), the rest in its original colours
            # (astral_gems_trim.png). astral_gems.json gives the renderer each gem's box centre in the frame texture.
            fw, fh, fpx = read_png(data)
            is_gem = lambda q: q[0] == 0 and q[1] == 0 and q[2] >= 250 and q[3] > 0
            CELL = 9
            body = [(0, 0, 0, 0)] * (CELL * 6 * CELL)
            trim = [(0, 0, 0, 0)] * (CELL * 6 * CELL)
            centres = []
            for g in range(6):
                pts = [(i % fw, i // fw) for i, q in enumerate(fpx) if is_gem(q) and q[2] == 255 - g]
                x0, x1 = min(p[0] for p in pts), max(p[0] for p in pts)
                y0, y1 = min(p[1] for p in pts), max(p[1] for p in pts)
                assert x1 - x0 == 6 and y1 - y0 == 6
                centres.append([x0 + 3.5, y0 + 3.5])
                for cy in range(CELL):
                    for cx in range(CELL):
                        q = fpx[(y0 - 1 + cy) * fw + (x0 - 1 + cx)]
                        i = cy * CELL * 6 + g * CELL + cx
                        if is_gem(q):
                            body[i] = (255, 255, 255, 255)
                        else:
                            trim[i] = q
            fpx = [(33, 18, 52, q[3]) if is_gem(q) else q for q in fpx]
            write_png(out, fw, fh, fpx)
            if os.path.exists(f'{A}/textures/item/astral_gems.png'): os.remove(f'{A}/textures/item/astral_gems.png')
            write_png(f'{A}/textures/item/astral_gems_body.png', CELL * 6, CELL, body)
            write_png(f'{A}/textures/item/astral_gems_trim.png', CELL * 6, CELL, trim)
            import json as _json
            open(f'{A}/astral_gems.json', 'w', newline='\n').write(_json.dumps(
                {'texture_size': fw, 'cell': CELL, 'centres': centres}) + '\n')
        else:
            open(out, 'wb').write(data)
else:
    print('original jar not found, art not refreshed:', JAR)

# Polishment Device (original art, drawn here): a brass-ringed lens on a short handle, violet glass.
px = [(0, 0, 0, 0)] * 256
def put(x, y, c): px[y * 16 + x] = c + (255,)
brass, dark, glass, shine = (200, 160, 70), (120, 90, 40), (110, 60, 170), (220, 190, 255)
for i in range(8):                      # handle, bottom-left to the lens
    put(2 + i, 13 - i, dark); put(3 + i, 13 - i, brass)
cx, cy = 9, 6
for y in range(16):
    for x in range(16):
        d2 = (x - cx) ** 2 + (y - cy) ** 2
        if d2 <= 20: put(x, y, brass if d2 > 12 else glass)
put(8, 5, shine); put(9, 4, shine); put(8, 4, shine)
write_png(f'{A}/textures/item/polishment_device.png', 16, 16, px)

# Astral Monitor screen: a dark field with sparse blue stars (the original's end-sky shader, as a flat texture).
import random
rnd = random.Random(1721)
sky = [(11, 9, 16, 255)] * (128 * 128)
for _ in range(900):
    x, y = rnd.randrange(128), rnd.randrange(128)
    k = rnd.random()
    c = (round(25 + 50 * k), round(60 + 110 * k), round(110 + 130 * k))
    sky[y * 128 + x] = c + (255,)
    if k > 0.8:
        for dx, dy in ((1, 0), (0, 1)):
            sky[((y + dy) % 128) * 128 + (x + dx) % 128] = tuple(v // 2 for v in c) + (255,)
write_png(f'{A}/textures/item/astral_screen.png', 128, 128, sky)

print('phase 6b generated')


# ---------------------------------------------------------------- drum stance: no aiming while drumming
# A casting bracelet in the main hand with a drum in the off hand casts only spells whose first-order delivery (the one
# at the root of the tree, which leaves the caster) acts on an area centred on the caster in all directions, or on the
# caster (DrumCastable). The area chooses the targets, so any effects beneath it are fine. Decided by each delivery's
# behaviour class in Thaumaturge, not its name; effects with no delivery act on the caster's origin target.
w(f'data/{NS}/tags/thaumaturge/spell_part/drum_castable.json', {'replace': False, 'values': [
    'thaumaturge:aura',    # AuraDelivery: everything within a radius of the caster, every direction
    'thaumaturge:self',    # SelfDelivery: the caster
    'thaumaturge:cloud',   # CloudDelivery: a lingering area at the caster's position (SpellCloud.spawn at the origin)
]})
# Left out (aim, or a target the caster picks): ray (beam, bolt, touch), projectile (projectile, lob), spray, chain,
# wall, architect (plan), hierophant (eldritch_*), and ours compressed_blast and flurry. Also left out although they
# don't read the look: mine and spellbat (placed at the caster, but triggered by contact, not an area around them) and
# sprite (an autonomous summon).
# Effects that read the caster's look direction themselves, whatever delivers them, so cannot be unaimed: refused.
w(f'data/{NS}/tags/thaumaturge/spell_part/drum_aimed_effects.json', {'replace': False, 'values': [
    f'{NS}:alchemist_spree',       # throws bottles from the caster's rotation and clips a ray along the look
    f'{NS}:ethereal_manipulator',  # holds what the caster looks at, and a point along the look
    f'{NS}:vis_conductor',         # clips and sweeps along the look
    f'{NS}:warp_extraction',       # hurls along the look
]})
