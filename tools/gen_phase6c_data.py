"""Generates phase-6c data: cycle 1, Gadomancy's Aura Cores (infusion for the blank core, six crucible washes, the research entry,
item models, art). Run from the module root after gen_phase6b_data.py (it extends the item aspect data map) and before gen_lang.py.
Cycle 2 (Gem of Rifts, Terra-cast Gem, Causal Bouillon) is the second half of this file; later cycles extend it.

Gadomancy 1.7.10 is LGPL (credit Makeo and the Gadomancy team, never monetise). Its art is copied from the extracted reference
folder (reference/src/gadomancy-1.7.10-1.0.7.3, never committed except these three textures, credited in CREDITS.md).
TC4 -> Thaumaturge: aura -> auram, magic -> praecantatio, eldritch -> alienis, void -> vacuos, water -> aqua, heal -> victus,
balanced shard -> tag c:balanced_shards (Thaumic Horizons' balanced shard, phase 8-3), thaumium ingot -> ingot_thaumium, primal charm -> primal_charm. Instability 6 / 2.5.
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genlib import *

GAD_ART = os.environ.get('TC_REFERENCE', '../../reference') + '/src/gadomancy-1.7.10-1.0.7.3/assets/gadomancy/textures/items'
TYPES = {'air': 'aer', 'fire': 'ignis', 'water': 'aqua', 'earth': 'terra', 'order': 'ordo', 'entropy': 'perditio'}

# ---------------------------------------------------------------- recipes
# RegisteredRecipes.recipeAuraCore: catalyst primal charm; 4 balanced shards and 8 thaumium ingots; aura 9, magic 19, eldritch 17, void 34.
infusion('aura_core', 'aura_core',
         [('auram', 9), ('praecantatio', 19), ('alienis', 17), ('vacuos', 34)],
         it('primal_charm'), [{'tag': 'c:balanced_shards'}] * 4 + [it('ingot_thaumium')] * 8, 6, 'aura_core')

# recipesWashAuraCore: any attuned core + magic 12, water 12, heal 18 -> blank core (one recipe per type, as the original).
for t in TYPES:
    w(f'data/{NS}/recipe/crucible/aura_core_clean_{t}.json', {
        'type': f'{T}:crucible', 'aspects': aspects([('praecantatio', 12), ('aqua', 12), ('victus', 18)]),
        'catalyst': {'type': 'neoforge:components', 'items': f'{NS}:aura_core', 'components': {f'{NS}:core_type': t}},
        'research': gate('aura_core'), 'result': {'count': 1, 'id': f'{NS}:aura_core'}})

# ---------------------------------------------------------------- research
# AURA_CORE (original: Gadomancy tab (-1,6), complexity 2, aura 22, magic 10, each primal 8); ours at (-5,1) off the Thaumaturges.
entry('aura_core', -5, 1, f'{NS}:aura_core', [f'{NS}:thaumaturges'],
      [('auram', 22), ('praecantatio', 10)] + [(p, 8) for p in PRIMALS],
      [f'{NS}:infusion/aura_core'] + [f'{NS}:crucible/aura_core_clean_{t}' for t in TYPES], meta=['round'])

# ---------------------------------------------------------------- aspects (scanning)
add_item_aspects('aura_core', [('auram', 10), ('praecantatio', 10), ('vacuos', 5)])

# ---------------------------------------------------------------- models and art
w(f'assets/{NS}/models/item/aura_core.json', {
    'parent': 'minecraft:item/generated',
    'textures': {'layer0': f'{NS}:item/aura_core_core', 'layer1': f'{NS}:item/aura_core_border'},
    'overrides': [{'predicate': {f'{NS}:blank': 1}, 'model': f'{NS}:item/aura_core_blank'}]})
w(f'assets/{NS}/models/item/aura_core_blank.json', {
    'parent': 'minecraft:item/generated',
    'textures': {'layer0': f'{NS}:item/aura_core_core_blank', 'layer1': f'{NS}:item/aura_core_border'}})
ART = {'core_border.png': 'aura_core_border.png', 'core_core.png': 'aura_core_core.png',
       'core_core.png.mcmeta': 'aura_core_core.png.mcmeta', 'core_core_blank.png': 'aura_core_core_blank.png',
       'core_core_blank.png.mcmeta': 'aura_core_core_blank.png.mcmeta'}
if os.path.isdir(GAD_ART):
    for src, dst in ART.items():
        out = os.path.join(A, 'textures/item', dst); os.makedirs(os.path.dirname(out), exist_ok=True)
        with open(os.path.join(GAD_ART, src), 'rb') as f, open(out, 'wb') as g:
            g.write(f.read())
else:
    print('Gadomancy art not found, textures not refreshed:', GAD_ART)



# ================================================================ cycle 2: Gem of Rifts, Terra-cast Gem, Causal Bouillon
# Original (Concilium, WTFPL): TCRecipeRegistry 1121-1192 and 1395-1415, Thaumonomicon 765-812, items/RiftGem, items/TerraCastGem,
# dim/*, blocks/VoidRealityBlock. TC4 -> Thaumaturge: crystal -> vitreus, void -> vacuos, travel (iter) -> motus, aura -> auram,
# mind -> cognitio, entropy -> perditio. Instability 10 / 2.5 = 4.
# Components (TC4 resource meta -> ours): 15 primal charm; Gadomancy's Element of Darkness -> essentia crystal of vacuos (deviation);
# 1 nitor -> yellow nitor; 0 alumentum; 17 void seed; cosmetic opaque 2 warded glass; custom ore 0 / 7 cinnabar / amber ore,
# 1 / 4 / 6 infused air / earth / entropy stone -> placed crystal of the aspect; nugget 16 / 31 native iron / cinnabar cluster.
def core(kind):
    return {'type': 'neoforge:components', 'items': f'{NS}:aura_core', 'components': {f'{NS}:core_type': kind}}

def many(item, n):
    return [it(item)] * n

RIFT_COMPONENTS = (many('primal_charm', 2) + [crystal('vacuos')] * 2 + many('crystal_perditio', 2) + many('nitor_yellow', 2)
                   + many('warded_glass', 2) + many('void_seed', 2) + many('crystal_aer', 2) + many('alumentum', 2))
TERRA_COMPONENTS = (many('primal_charm', 2) + many('cluster_iron', 1) + many('crystal_terra', 2) + many('nitor_yellow', 2)
                    + many('warded_glass', 2) + many('ore_cinnabar', 1) + many('ore_amber', 1) + many('crystal_aer', 2)
                    + many('alumentum', 2) + many('cluster_cinnabar', 1))
assert len(RIFT_COMPONENTS) == 16 and len(TERRA_COMPONENTS) == 16
infusion('rift_gem', 'rift_gem', [('vitreus', 64), ('vacuos', 64), ('motus', 96), ('auram', 32)],
         core('entropy'), RIFT_COMPONENTS, 10, 'inert_rift_gem')
infusion('terra_cast_gem', 'terra_cast_gem', [('vitreus', 64), ('terra', 64), ('motus', 96), ('auram', 32)],
         core('earth'), TERRA_COMPONENTS, 10, 'inert_terra_cast_gem')
# Chained-rift recipes: the inert gem thrown on a hex whose rift holds the essentia becomes the active gem.
rift_recipe('rift_gem', it(f'{NS}:inert_rift_gem'),
            [('motus', 128), ('perditio', 128), ('vitreus', 128), ('vacuos', 128), ('auram', 64)], 'rift_gem', 'rift_gem')
rift_recipe('terra_cast_gem', it(f'{NS}:inert_terra_cast_gem'),
            [('motus', 128), ('perditio', 128), ('vitreus', 128), ('cognitio', 128), ('auram', 64)], 'terra_cast_gem', 'terra_cast_gem')
# Gem of Rifts + Terra-cast Gem with a cast -> the rift gem with the cast added (special crafting recipe, TerraGemRecipe).
w(f'data/{NS}/recipe/terra_gem_cast.json', {'type': f'{NS}:terra_gem_cast', 'category': 'misc'})

# Research (original: RIFTGEM (-9,2) and TERRACAST (-9,0), both concealed; parents TCGADOMANCY = our aura_core, and the hex).
entry('rift_gem', -9, 2, f'{NS}:rift_gem', [f'{NS}:aura_core', f'{NS}:hex_of_predictability'],
      [('vitreus', 8), ('auram', 8), ('vacuos', 8), ('motus', 8)], [f'{NS}:infusion/rift_gem'], meta=['hidden'])
entry('terra_cast_gem', -9, 0, f'{NS}:terra_cast_gem', [f'{NS}:aura_core', f'{NS}:hex_of_predictability', f'{NS}:rift_gem'],
      [('vitreus', 8), ('auram', 8), ('terra', 8), ('motus', 8)], [f'{NS}:infusion/terra_cast_gem'], meta=['hidden'])

# Items: aspects for scanning (the original gems had none of their own)
add_item_aspects('inert_rift_gem', [('vitreus', 20), ('vacuos', 20), ('motus', 20)])
add_item_aspects('rift_gem', [('vitreus', 20), ('vacuos', 20), ('motus', 20), ('perditio', 10)])
add_item_aspects('inert_terra_cast_gem', [('vitreus', 20), ('terra', 20), ('motus', 20)])
add_item_aspects('terra_cast_gem', [('vitreus', 20), ('terra', 20), ('motus', 20)])
add_item_aspects('void_reality', [('vacuos', 20), ('alienis', 10)])

# Models: built-in entity models, drawn by client/GemRenderer (BEWLR) and client/GemFirstPerson (spec: docs/design/6c-gem-held-render.md).
# The placeholder flat icon below survives only as the break-particle texture. Original meshes and textures (Concilium, WTFPL).
for gem_name in ('inert_rift_gem', 'rift_gem', 'inert_terra_cast_gem', 'terra_cast_gem'):
    w(f'assets/{NS}/models/item/{gem_name}.json', {
        'parent': 'minecraft:builtin/entity', 'gui_light': 'side', 'textures': {'particle': f'{NS}:item/{gem_name}'},
        'display': {'gui': {'rotation': [30, 225, 0], 'scale': [0.625, 0.625, 0.625]}, 'ground': {'scale': [0.5, 0.5, 0.5]}}})
for mesh_name in ('gem_base', 'gem_top'):
    out = f'{A}/models/item/{mesh_name}.obj'
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, 'wb') as f:
        f.write(jar_read(f'models/{mesh_name}.obj'))
copy_from_jar({'textures/models/gem_base.png': 'entity/gem/gem_base.png', 'textures/models/gem_top.png': 'entity/gem/gem_top.png'})

def gem_png(name, body, edge, shine):
    px = [(0, 0, 0, 0)] * 256
    def put(x, y, c):
        px[y * 16 + x] = c + (255,)
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5) + abs(y - 7.5)
            if d <= 6.5:
                put(x, y, edge if d > 5.0 else body)
    for x, y in ((6, 4), (7, 3), (5, 5), (6, 5), (5, 6)):
        put(x, y, shine)
    write_png(f'{A}/textures/item/{name}.png', 16, 16, px)

gem_png('inert_rift_gem', (96, 96, 128), (60, 60, 90), (170, 170, 200))
gem_png('rift_gem', (150, 70, 200), (90, 30, 130), (230, 190, 255))
gem_png('inert_terra_cast_gem', (96, 120, 96), (60, 80, 60), (170, 200, 170))
gem_png('terra_cast_gem', (70, 170, 80), (30, 110, 40), (190, 255, 190))

# Void Reality: the Causal Bouillon's floor. Original art (textures/blocks/void.png, WTFPL).
block_state_simple('void_reality')
w(f'assets/{NS}/models/block/void_reality.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/void_reality'}})
w(f'assets/{NS}/models/item/void_reality.json', {'parent': f'{NS}:block/void_reality'})
copy_from_jar({'textures/blocks/void.png': 'block/void_reality.png'})

# The dimension (data-driven). Original: flat, Void Reality over an air layer at the bottom; no weather, noon forever, no respawn,
# fog 0xA080A0 x 0.15 (= 0x181318). Floor: y 1-3 (three layers), so the surface is y 4.
w(f'data/{NS}/dimension_type/causal_bouillon.json', {
    'ultrawarm': False, 'natural': False, 'coordinate_scale': 1.0, 'has_skylight': True, 'has_ceiling': False,
    'ambient_light': 0.0, 'fixed_time': 6000, 'piglin_safe': False, 'bed_works': False, 'respawn_anchor_works': False,
    'has_raids': False, 'logical_height': 256, 'min_y': 0, 'height': 256, 'infiniburn': '#minecraft:infiniburn_overworld',
    'effects': f'{NS}:causal_bouillon', 'monster_spawn_light_level': 0, 'monster_spawn_block_light_limit': 0})
w(f'data/{NS}/worldgen/biome/causal_bouillon.json', {
    'has_precipitation': False, 'temperature': 0.5, 'downfall': 0.0,
    'effects': {'sky_color': 0x181318, 'fog_color': 0x181318, 'water_color': 0x3F76E4, 'water_fog_color': 0x050533,
                'particle': {'options': {'type': 'minecraft:ash'}, 'probability': 0.004}},
    'spawners': {}, 'spawn_costs': {}, 'carvers': {}, 'features': []})
w(f'data/{NS}/dimension/causal_bouillon.json', {
    'type': f'{NS}:causal_bouillon',
    'generator': {'type': 'minecraft:flat', 'settings': {
        'biome': f'{NS}:causal_bouillon', 'features': False, 'lakes': False, 'structure_overrides': [],
        'layers': [{'block': 'minecraft:air', 'height': 1}, {'block': f'{NS}:void_reality', 'height': 3}]}}})


# ================================================================ cycle 3: the Brightest One
# Original BRIGHTESTONE (-10,4): energy 10, aura 20, life 10, pride 10; round, concealed; parent the hex, hidden parent the Thaumaturge drum.
# Primordial Life (phase 2) now hangs off it (gen_phase2_data.py). Icon: the original's textures/misc/brightest.png (WTFPL).
entry('brightest_one', -10, 4, f'{NS}:textures/research/brightest.png', [f'{NS}:hex_of_predictability', f'~{NS}:thaumdrum'],
      [('potentia', 10), ('auram', 20), ('victus', 10), ('forbiddenmagic:superbia', 10)], meta=['hidden', 'round'])
copy_from_jar({'textures/misc/brightest.png': 'research/brightest.png'})

# Brightest One halo: a white radial falloff with soft angular wisps, alpha 255 (additive: the renderer tints it through the
# vertex colour at full strength, as the original did). Our own art, MIT. The radial profile (r in units of the quad half-size)
# is the brightness curve measured from the original's sprite sheet averaged over its 16 frames (numbers only, no pixels
# copied): full to r 0.25, half at 0.35, gone by 0.6. So the visible disc is ~2.4 blocks across on the 3.5 half-size quad.
import math
HALO_PROFILE = [(0.00, 1.00), (0.15, 0.95), (0.20, 0.92), (0.25, 0.88), (0.30, 0.78), (0.35, 0.54), (0.40, 0.33),
                (0.45, 0.19), (0.50, 0.095), (0.55, 0.048), (0.60, 0.023), (0.70, 0.006), (0.80, 0.0)]


def halo_profile(r):
    for (r0, v0), (r1, v1) in zip(HALO_PROFILE, HALO_PROFILE[1:]):
        if r < r1:
            return v0 + (v1 - v0) * (r - r0) / (r1 - r0)
    return 0.0


HALO = 128
halo_px = []
for hy in range(HALO):
    for hx in range(HALO):
        dx, dy = (hx + 0.5) / HALO * 2 - 1, (hy + 0.5) / HALO * 2 - 1
        rr = math.hypot(dx, dy)
        ang = math.atan2(dy, dx)
        wisp = 1.0 + 0.12 * math.sin(3 * ang + 5 * rr) + 0.08 * math.sin(5 * ang - 7 * rr)
        v = halo_profile(rr) * (wisp if rr < 0.3 else 1.0)
        v = max(0.0, min(1.0, v))
        c = int(round(v * 255))
        halo_px.append((c, c, c, 255))
os.makedirs(f'{A}/textures/misc', exist_ok=True)
write_png(f'{A}/textures/misc/brightest_halo.png', HALO, HALO, halo_px)
