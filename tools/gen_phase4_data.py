"""Generates phase-4 datapack JSON and assets: Thaumaturges, Mad Thaumaturges, the corpses and the Nether Membrane.
Run from the repo root, after gen_phase3b_data.py (it adds the hint-note gate to entries the earlier scripts wrote).

Translated from Thaumonomicon / TCEntityRegistry / TCConfig (Concilium 1.1.1). Spawn weights are the original's
addSpawn calls. Art (entity skins, research illustrations, icons, the membrane) is copied from the original jar
(WTFPL); set TC_ORIGINAL_JAR if it isn't at the workspace's reference/jars path. The research hint's icon is drawn here.
"""
import json, os, struct, zipfile, zlib
NS = 'thaumicconcilium'
ROOT = 'src/main/resources'
T = 'thaumaturge'
INFERNUM = 'thaumadditions:infernum'
JAR = os.environ.get('TC_ORIGINAL_JAR', '../../reference/jars/legacy-1.7.10/ThaumicConcilium-1.1.1.jar')
BOTANIA = [{'type': 'neoforge:mod_loaded', 'modid': 'botania'}]


def w(path, obj):
    p = os.path.join(ROOT, path); os.makedirs(os.path.dirname(p), exist_ok=True)
    json.dump(obj, open(p, 'w'), indent=2); open(p, 'a').write('\n')
def r(path): return json.load(open(os.path.join(ROOT, path)))
def asp(a): return a if ':' in a else f'{T}:{a}'
def note(n): return max(1, round(n / 2.5))
def aspects(pairs): return [{'aspect': asp(a), 'amount': n} for a, n in pairs]


# ---------------------------------------------------------------- research
CAT = f'{NS}:concilium'
def entry(name, col, row, icon, parents, notes, warp=0, meta=None, addenda=None, complexity=2):
    st = [{'text': f'research.{NS}.{name}.stage_0',
           'required_knowledge': [{'type': 'theory', 'category': CAT, 'amount': 1}]},
          {'text': f'research.{NS}.{name}.stage_1'}]
    if warp: st[0]['warp'] = warp
    obj = {'category': CAT, 'name': f'research.{NS}.{name}.title', 'parents': parents, 'column': col, 'row': row,
           'icons': [icon], 'stages': st,
           'note_aspects': [{'aspect': asp(a), 'amount': note(n)} for a, n in notes], 'complexity': complexity}
    if meta: obj['meta'] = meta
    if addenda: obj['addenda'] = addenda
    w(f'data/{NS}/thaumaturge/research_entry/{name}.json', obj)

def scanned(kind, ident): ns, path = ident.split(':'); return f'{T}:scanned/{kind}/{ns}/{path}'

# THAUMATURGES: concealed, bought with points, off the root (original parent: research duplication). MAN 8, EXCHANGE 8.
entry('thaumaturges', -3, -1, f'{NS}:textures/item/icon_thaumaturge.png', [CAT],
      [('humanus', 8), ('permutatio', 8)], complexity=1)
# MADTHAUMATURGES: lost, found by scanning one. MIND 5, ELDRITCH 10, MAN 15; 1 warp.
entry('mad_thaumaturges', -5, -1, f'{NS}:textures/item/icon_mad_thaumaturge.png',
      [f'{NS}:thaumaturges', scanned('entity', f'{NS}:mad_thaumaturge')],
      [('cognitio', 5), ('alienis', 10), ('humanus', 15)], warp=1, meta=['hidden', 'round'])
# THAUMICCONCILIUM (the bestiary): lost, found by scanning any of the fallen. Their pages join as their phases land;
# the Withered Botanist's shows once one has been scanned (it only exists with Botania).
entry('fallen', -3, -3, f'{NS}:textures/category.png', [f'{NS}:thaumaturges', f'{NS}:scanned/fallen'],
      [('cognitio', 5), ('alienis', 10), ('humanus', 15)], warp=1, meta=['hidden', 'round'],
      addenda=[{'text': f'research.{NS}.fallen.botanist',
                'required_research': [scanned('entity', f'{NS}:withered_botanist')]},
               {'text': f'research.{NS}.fallen.vengeful',
                'required_research': [scanned('entity', f'{NS}:vengeful_golem')]}])
w(f'data/{NS}/thaumaturge/scan/fallen.json', {'entities': [f'{NS}:nether_explorer', f'{NS}:withered_botanist', f'{NS}:vengeful_golem'],
                                              'key': f'{NS}:scanned/fallen'})
# NETHER_MEMBRANE: secondary, lost, triggered by the membrane; a Nether Explorer grants it outright.
# FLESH, NETHER, ARMOR, CLOTH 4 each (flesh -> Forbidden Magic's corpus; no cloth aspect in Thaumaturge: fabrico).
entry('nether_membrane', 3, -2, f'{NS}:nether_membrane', [scanned('item', f'{NS}:nether_membrane')],
      [('forbiddenmagic:corpus', 4), (INFERNUM, 4), ('praemunio', 4), ('fabrico', 4)], meta=['hidden'], complexity=1)

# Hint notes gate their entries (agreed 2026-11-21): the note is an obtain requirement on the first stage, consumed when
# that stage is bought. The Vis Conductor and Destabilized Crystal hints wait for phase 6.
for key in ['reflection_focus', 'bottle_of_thick_taint', 'runic_windings', 'positive_burst_focus', 'impulse_focus']:
    path = f'data/{NS}/thaumaturge/research_entry/{key}.json'
    obj = r(path)
    obj['stages'][0]['obtain'] = [{'items': f'{NS}:research_hint', 'amount': 1,
                                   'components': {f'{NS}:hint': f'{NS}:{key}'}}]
    w(path, obj)

# ---------------------------------------------------------------- aspects (scanning)
# Original entity tags x 2.5.
w(f'data/{T}/data_maps/entity_type/entity_aspects.json', {'values': {
    f'{NS}:thaumaturge': aspects([('humanus', 10), ('praecantatio', 10), ('auram', 10), ('ordo', 10)]),
    f'{NS}:mad_thaumaturge': aspects([('humanus', 10), ('cognitio', 10), ('alienis', 10), ('vacuos', 10)]),
    f'{NS}:nether_explorer': aspects([('humanus', 10), (INFERNUM, 10), ('mortuus', 10), ('praecantatio', 10)]),
    f'{NS}:withered_botanist': aspects([('humanus', 10), ('herba', 10), ('mortuus', 10), ('permutatio', 10)]),
}})
w(f'data/{T}/data_maps/item/base_aspects.json', {'values': {
    f'{NS}:nether_membrane': aspects([('forbiddenmagic:corpus', 5), (INFERNUM, 5), ('praemunio', 5), ('fabrico', 5)]),
    f'{NS}:research_hint': aspects([('cognitio', 5), ('humanus', 3)]),
}})

# ---------------------------------------------------------------- spawns
ANY = {'type': 'neoforge:not', 'value': f'{NS}:causal_bouillon'}   # every biome except the Causal Bouillon (6c cycle 2)
def add_spawns(name, entity, weight, lo, hi, biomes='#minecraft:is_overworld', cond=None):
    w(f'data/{NS}/neoforge/biome_modifier/{name}.json', ({'neoforge:conditions': cond} if cond else {}) | {
        'type': 'neoforge:add_spawns', 'biomes': biomes,
        'spawners': [{'type': f'{NS}:{entity}', 'weight': weight, 'minCount': lo, 'maxCount': hi}]})
# Original: every biome (minus a config blacklist). Mad Thaumaturges and Botanists keep that (the Botanist's own rule
# still limits it to the overworld). Thaumaturges: overworld, and only as a haunt's chunk generates (TCSpawns).
add_spawns('thaumaturge', 'thaumaturge', 5, 1, 3)
add_spawns('mad_thaumaturge', 'mad_thaumaturge', 5, 1, 3, biomes=ANY)
add_spawns('withered_botanist', 'withered_botanist', 5, 1, 1, biomes=ANY, cond=BOTANIA)

# Surface structures near which Thaumaturges spawn naturally (agreed with the user, 2026-11-21).
w(f'data/{NS}/tags/worldgen/structure/thaumaturge_haunts.json', {'replace': False, 'values': [
    '#minecraft:village', 'minecraft:desert_pyramid', 'minecraft:jungle_pyramid', 'minecraft:swamp_hut',
    'minecraft:igloo', 'minecraft:ruined_portal',
    {'id': 'thaumaturge:mound', 'required': False},
    {'id': 'thaumaturge:eldritch_obelisk', 'required': False},
    {'id': 'villagesandpillages:village_witch', 'required': False},
]})

# ---------------------------------------------------------------- recipes
w(f'data/{NS}/recipe/nether_membrane.json', {'type': f'{NS}:nether_membrane', 'category': 'misc'})

# ---------------------------------------------------------------- item models, art
def gen(name, tex=None):
    w(f'assets/{NS}/models/item/{name}.json', {'parent': 'minecraft:item/generated',
                                              'textures': {'layer0': f'{NS}:item/{tex or name}'}})
gen('research_hint')  # nether_membrane's model is written by gen_art_redraw.py
for e in ['thaumaturge', 'mad_thaumaturge', 'nether_explorer', 'withered_botanist']:
    w(f'assets/{NS}/models/item/{e}_spawn_egg.json', {'parent': 'minecraft:item/template_spawn_egg'})

COPY = {
    'textures/entity/thaumaturge.png': 'textures/entity/thaumaturge.png',
    'textures/entity/mad_thaumaturge.png': 'textures/entity/mad_thaumaturge.png',
    'textures/entity/nether_explorer.png': 'textures/entity/nether_explorer.png',
    'textures/entity/withered_botanist.png': 'textures/entity/withered_botanist.png',
    'textures/gui/thaumaturge.png': 'textures/gui/thaumaturge.png',
    'textures/gui/mad_thaumaturge.png': 'textures/gui/mad_thaumaturge.png',
    'textures/gui/nether_explorer.png': 'textures/gui/nether_explorer.png',
    'textures/gui/withered_botanist.png': 'textures/gui/withered_botanist.png',
    'textures/items/iconThaumaturge.png': 'textures/item/icon_thaumaturge.png',
    'textures/items/iconMadThaumaturge.png': 'textures/item/icon_mad_thaumaturge.png',
}
if os.path.exists(JAR):
    z = zipfile.ZipFile(JAR)
    for src, dst in COPY.items():
        out = os.path.join(ROOT, f'assets/{NS}', dst); os.makedirs(os.path.dirname(out), exist_ok=True)
        open(out, 'wb').write(z.read(f'assets/{NS}/{src}'))
else:
    print('original jar not found, art not refreshed:', JAR)


def png(path, size, pixels):
    raw = b''.join(b'\x00' + b''.join(struct.pack('BBBB', *pixels.get((x, y), (0, 0, 0, 0))) for x in range(size))
                   for y in range(size))
    def chunk(t, d): return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0)) \
        + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'wb').write(data)

# item/research_hint.png: drawn with the art kit in gen_art_redraw.py (art pass 2026-11-27).

print('phase 4 generated')
