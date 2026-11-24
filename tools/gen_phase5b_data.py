"""Generates phase-5b data. Cycle 2: the Peeler of Matter (research entry, hint-note gate, entity aspects, research icon).
Run after gen_phase5a_*.py (reads the hint-note pattern they use) and before gen_lang.py.

TC4 -> Thaumaturge: man -> humanus, void -> vacuos, flesh -> forbiddenmagic:corpus, exchange -> permutatio, order -> ordo.
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genlib import *

# ================================================================ cycle 2: the Peeler of Matter
copy_from_jar({'textures/misc/peeler.png': 'research/peeler.png'})

# MATERIALPEELER (Thaumonomicon.java:1017-1033): man, void, flesh, exchange, order 8; (-8, 10); complexity 1; warp 4; lost (found by the
# Pontifex's research note) and round; parent CRIMSONPONTIFEX (the hidden parent CREATION has no counterpart here).
entry('material_peeler', -8, 10, f'{NS}:textures/research/peeler.png', [f'{NS}:crimson_pontifex'],
      [('humanus', 8), ('vacuos', 8), ('forbiddenmagic:corpus', 8), ('permutatio', 8), ('ordo', 8)], warp=4, meta=['hidden', 'round'], complexity=1)
_path = f'data/{NS}/thaumaturge/research_entry/material_peeler.json'
_obj = r(_path)
_obj['stages'][0]['obtain'] = [{'items': f'{NS}:research_hint', 'amount': 1, 'components': {f'{NS}:hint': f'{NS}:material_peeler'}}]
w(_path, _obj)

# Entity tags (TCEntityRegistry:232-236, x2.5): eldritch 16, void 16, magic 16, flesh 32.
add_entity_aspects('material_peeler', [('alienis', 40), ('vacuos', 40), ('praecantatio', 40), ('forbiddenmagic:corpus', 80)])

# ================================================================ cycle 3: the Flesh Crucible
# PROVISIONAL (user decision pending): FLESH_ASPECT is the aspect the crucible stores (TC4 FLESH is Forbidden Magic's corpus; the
# Java side, FleshCrucibleBlockEntity, is aspect-free: it holds one number, and this only names it in the recipe and notes).
# PLATING and MOTE are our Crimson Plating and Primordial Mote (trunk T1; Tainted Magic's plating was ItemMaterial meta 7).
FLESH_ASPECT = 'forbiddenmagic:corpus'
PLATING = f'{NS}:crimson_plating'      # T1: ours now (was the plate_void stand-in)
MOTE = f'{NS}:primordial_mote'          # T1: ours now, no source until 5c
copy_from_jar({f'textures/blocks/flesh_crucible{i}.png': f'block/flesh_crucible{i}.png' for i in range(1, 7)}
              | {'textures/blocks/liquid_flesh.png': 'block/liquid_flesh.png'})
# The original drew a full cube (top 1, bottom 2, sides 3) and added inner faces by code (inner side 5, inner floor 6); this is the
# same basin as the Quicksilver Reactor's (floor 4 px, walls 2 px).
def _fbox(f, t, **faces): return {'from': f, 'to': t, 'faces': {d: {'texture': x} for d, x in faces.items()}}
_S, _I, _T, _B = '#side', '#inner', '#top', '#bottom'
w(f'assets/{NS}/models/block/flesh_crucible.json', {
    'parent': 'minecraft:block/block', 'ambientocclusion': False,
    'textures': {'top': f'{NS}:block/flesh_crucible1', 'bottom': f'{NS}:block/flesh_crucible2', 'side': f'{NS}:block/flesh_crucible3',
                 'inner': f'{NS}:block/flesh_crucible5', 'floor': f'{NS}:block/flesh_crucible6', 'particle': f'{NS}:block/flesh_crucible3'},
    'elements': [
        _fbox([0, 0, 0], [16, 4, 16], north=_S, south=_S, east=_S, west=_S, up='#floor', down=_B),
        _fbox([0, 4, 0], [16, 16, 2], north=_S, south=_I, east=_S, west=_S, up=_T),
        _fbox([0, 4, 14], [16, 16, 16], north=_I, south=_S, east=_S, west=_S, up=_T),
        _fbox([0, 4, 2], [2, 16, 14], north=_I, south=_I, east=_I, west=_S, up=_T),
        _fbox([14, 4, 2], [16, 16, 14], north=_I, south=_I, east=_S, west=_I, up=_T)]})
w(f'assets/{NS}/blockstates/flesh_crucible.json', {'variants': {'': {'model': f'{NS}:block/flesh_crucible'}}})
w(f'assets/{NS}/models/item/flesh_crucible.json', {'parent': f'{NS}:block/flesh_crucible'})
w(f'data/{NS}/loot_table/blocks/flesh_crucible.json', {
    'type': 'minecraft:block', 'random_sequence': f'{NS}:blocks/flesh_crucible',
    'pools': [{'rolls': 1.0, 'bonus_rolls': 0.0, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
               'entries': [{'type': 'minecraft:item', 'name': f'{NS}:flesh_crucible'}]}]})

# FLESHCRUCIBLE (Thaumonomicon.java:1096-1107): man, void, flesh, exchange, fire 8; (-8, 12); complexity 3; concealed; warp 4;
# parent MATERIALPEELER, hidden parent QUICKSILVERCRUCIBLE; the peeler lists it as its sibling (:1028).
entry('flesh_crucible', -8, 12, f'{NS}:flesh_crucible', [f'{NS}:material_peeler', f'~{NS}:quicksilver_crucible'],
      [('humanus', 8), ('vacuos', 8), (FLESH_ASPECT, 8), ('permutatio', 8), ('ignis', 8)], [f'{NS}:infusion/flesh_crucible'],
      warp=4, meta=['hidden'], complexity=3)
_p = r(f'data/{NS}/thaumaturge/research_entry/material_peeler.json')
_p['siblings'] = [f'{NS}:flesh_crucible']
w(f'data/{NS}/thaumaturge/research_entry/material_peeler.json', _p)

# Recipe (TCRecipeRegistry.java:858-883): instability 10; flesh 128 + 128, fire 128, gluttony 64, nether 128 (infernum), hunger 64
# (desiderium); catalyst the quicksilver crucible (our reactor); components: nether shard, 3 crimson plating, bright nitor,
# 3 primordial motes (ours; no source until 5c, so the recipe stays uncraftable: user decision).
infusion('flesh_crucible', 'flesh_crucible',
         [(FLESH_ASPECT, 256), ('ignis', 128), ('forbiddenmagic:gula', 64), ('thaumadditions:infernum', 128), ('desiderium', 64)],
         it(f'{NS}:quicksilver_reactor'),
         [it('thaumaturgetinker:nether_shard')] + [it(PLATING)] * 3 + [it('thaumaturgetinker:bright_nitor')]
         + [it(MOTE)] * 3, 10, 'flesh_crucible')
_path = f'data/{NS}/recipe/infusion/flesh_crucible.json'
_o = r(_path)
_o['neoforge:conditions'] = [{'type': 'neoforge:item_exists', 'item': 'thaumaturgetinker:ichor'}]
w(_path, _o)

# ================================================================ cycle 4: the Crimson Raid spell part
# Crimson Initiation's tool: any item in this tag (user decision); our Crimson Dagger is in it (the pack test adds crimson_blade too).
w(f'data/{NS}/tags/item/initiation_tools.json', {'values': [f'{NS}:crimson_dagger']})
# RAID_COMPLEXITY is the part's price (Thaumaturge prices a part by complexity: hellbat with two bats 16, primal 20). The original was an Ars Magica 2 component (mana 3000, burnout 300); no AM2 here.
RAID_COMPLEXITY = 22   # user decision: above a lesser focus's 20, so it needs an advanced focus or better
_z = jar()
if _z is not None:
    # The original's 64 x 64 spell icon, halved to the 32 x 32 of the other part icons.
    _w, _h, _px = read_png(_z.read(f'assets/{NS}/textures/items/spells/Crimson_Raid.png'))
    write_png(os.path.join(ROOT, f'assets/{NS}/textures/foci/crimson_raid.png'), 32, 32,
              [_px[(y * _h // 32) * _w + (x * _w // 32)] for y in range(32) for x in range(32)])
w(f'data/{NS}/thaumaturge/spell_part/crimson_raid.json', {
    'aspect': f'{T}:alienis', 'behavior': {'type': f'{NS}:crimson_raid'}, 'color': 0xA01020, 'complexity': RAID_COMPLEXITY,
    'icon': f'{NS}:textures/foci/crimson_raid.png', 'research': {'entry': f'{NS}:crimson_raid'}})
# CRIMSONRAID (Thaumonomicon.java:1069-1082): man, weapon, eldritch, travel 8; (-5, 13); complexity 1; round, concealed; parent
# CRIMSONINITIATION; warp 3. Weapon -> aversio, eldritch -> alienis, travel -> motus.
entry('crimson_raid', -5, 13, f'{NS}:textures/foci/crimson_raid.png', [f'{NS}:crimson_initiation'],
      [('humanus', 8), ('aversio', 8), ('alienis', 8), ('motus', 8)], warp=3, meta=['hidden', 'round'], complexity=1)

# ================================================================ cycle 5: Ethereal Shackles meshes and textures (original, WTFPL)
# Drawn by client/EtherealShacklesRenderer (thrown) and client/ShacklesFirstPerson (chained hand): ObjMesh.getFlat on both.
for _mesh in ('shackles', 'chain'):
    _out = f'{A}/models/item/{_mesh}.obj'
    os.makedirs(os.path.dirname(_out), exist_ok=True)
    if jar() is not None:
        with open(_out, 'wb') as _f:
            _f.write(jar_read(f'models/{_mesh}.obj'))
copy_from_jar({'textures/models/shackles.png': 'entity/shackles/shackles.png', 'textures/models/Chain.png': 'entity/shackles/chain.png'})

# ================================================================ phase 5b D6: base_aspects for the crimson cult's gear (option A)
# Thaumaturge ships none for these ids (the interpreter guessed). Real entries for crimson_plate_*, praetor, robe and boots stay.
_ba = r(f'data/{T}/data_maps/item/base_aspects.json')
_ba['values']['thaumaturge:crimson_blade'] = aspects([('metallum', 12), ('aversio', 16), ('humanus', 6), ('alienis', 6)])
_ba['values']['thaumaturge:curio_rites'] = aspects([('cognitio', 10), ('alienis', 10), ('humanus', 5)])
_ba['values']['thaumaturge:crimson_captain_helm'] = aspects([('praemunio', 8), ('metallum', 8), ('humanus', 3), ('alienis', 3)])
_ba['values']['thaumaturge:crimson_captain_chest'] = aspects([('praemunio', 24), ('metallum', 20), ('humanus', 5), ('alienis', 5)])
_ba['values']['thaumaturge:crimson_ranger_helm'] = aspects([('praemunio', 6), ('humanus', 3), ('alienis', 3)])
_ba['values']['thaumaturge:crimson_ranger_chest'] = aspects([('praemunio', 16), ('humanus', 5), ('alienis', 5)])
_ba['values']['thaumaturge:crimson_ranger_legs'] = aspects([('praemunio', 12), ('humanus', 4), ('alienis', 4)])
w(f'data/{T}/data_maps/item/base_aspects.json', _ba)
