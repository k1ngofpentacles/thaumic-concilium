"""Generates phase-3 datapack JSON: gear (Runic Windings, Dump Jackboots, Elemental Surfer Shorts), the Burdening
Amulet and Tight Belt, the two thrown bottles and the two Shard Mills. Run from the repo root, after
gen_phase2_data.py (and import_phase3_assets.py for the art).

Translated from TCRecipeRegistry and Thaumonomicon (Concilium 1.1.1). As in phases 1-2: TC4 aspect amounts are kept,
instability is divided by 2.5 (capped at 6), wispy essences and TC4 crystal clusters become Thaumaturge crystals.
The one arcane recipe follows Forbidden Magic's rule: vis is the sum of the costs, crystals are each cost / 10.
"""
import json, math, os
NS = 'thaumicconcilium'
ROOT = 'src/main/resources'
T = 'thaumaturge'
FM = 'forbiddenmagic'
def w(path, obj):
    p = os.path.join(ROOT, path); os.makedirs(os.path.dirname(p), exist_ok=True)
    json.dump(obj, open(p, 'w'), indent=2); open(p, 'a').write('\n')
def it(i): return {'item': i if ':' in i else f'{T}:{i}'}
def asp(a): return a if ':' in a else f'{T}:{a}'
def crystal(a):
    return {'type': 'neoforge:components', 'items': 'thaumaturge:essentia_crystal',
            'components': {'thaumaturge:crystal_aspect': {'aspect': asp(a)}}}
def instab(tc4): return max(1, min(6, round(tc4 / 2.5)))
def gate(key, stage=1):
    g = {'entry': f'{NS}:{key}'}
    if stage is not None: g['stage'] = stage
    return g
def SHARD(vice): return it(f'{FM}:{vice}_shard')

# ---------------------------------------------------------------- recipes
def infusion(key, research, aspects, catalyst, components, tc4_instability, result):
    w(f'data/{NS}/recipe/infusion/{key}.json', {
        'type': 'thaumaturge:infusion', 'aspects': [{'aspect': asp(a), 'amount': n} for a, n in aspects],
        'catalyst': catalyst, 'components': components, 'instability': instab(tc4_instability),
        'research': gate(research), 'result': {'count': 1, 'id': f'{NS}:{result}'}})
def crucible(key, research, aspects, catalyst, result, stage=1):
    w(f'data/{NS}/recipe/crucible/{key}.json', {
        'type': 'thaumaturge:crucible', 'aspects': [{'aspect': asp(a), 'amount': n} for a, n in aspects],
        'catalyst': catalyst, 'research': gate(research, stage), 'result': {'count': 1, 'id': f'{NS}:{result}'}})

# Thaumic Concilium "ClearWater": a TC4 empty phial (itemEssence) + 1 aqua.
crucible('bottle_of_clear_water', 'bottle_of_clear_water', [('aqua', 1)], it('phial'), 'bottle_of_clear_water', stage=None)
# "BottleOfThickTaint": TC4 bottle of taint + taint 16, slime 16 (-> aqua), crystal 16.
crucible('bottle_of_thick_taint', 'bottle_of_thick_taint', [('vitium', 16), ('aqua', 16), ('vitreus', 16)],
         it('bottle_taint'), 'bottle_of_thick_taint')

# "ShardMill": GLG / WCW / GWG with gold ingot, lever, greatwood log, primal charm; entropy 50, order 25, air 10, earth 10.
costs = [('perditio', 50), ('ordo', 25), ('aer', 10), ('terra', 10)]
w(f'data/{NS}/recipe/arcane_workbench/shard_mill.json', {
    'type': 'thaumaturge:arcane_workbench_shaped', 'pattern': ['GLG', 'WCW', 'GWG'],
    'key': {'G': it('minecraft:gold_ingot'), 'L': it('minecraft:lever'), 'W': it('log_greatwood'), 'C': it('primal_charm')},
    'vis': sum(n for _, n in costs), 'crystals': [{'aspect': asp(a), 'amount': math.ceil(n / 10)} for a, n in costs],
    'research': gate('shard_mill'), 'result': {'count': 1, 'id': f'{NS}:shard_mill'}})

# "SilverwoodShardMill": silverwood log + silverwood rod, vis filter, thaumium, primal charm, thaumium cap (x2 each).
infusion('silverwood_shard_mill', 'silverwood_shard_mill',
         [('instrumentum', 64), ('victus', 64), ('herba', 128), ('ordo', 64), ('perditio', 64), ('machina', 32), ('praecantatio', 32)],
         it('log_silverwood'), [it('wand_rod_silverwood'), it('filter'), it('ingot_thaumium'), it('primal_charm'),
                                it('wand_cap_thaumium'), it('filter'), it('ingot_thaumium'), it('primal_charm')],
         5, 'silverwood_shard_mill')
# "DumpJackboots": traveller's boots + piston, thaumium shovels, sloth shards, air/earth crystals, item grate.
infusion('dump_jackboots', 'dump_jackboots', [('terra', 128), ('praemunio', 16), (f'{FM}:desidia', 64), ('machina', 32)],
         it('traveller_boots'), [it('minecraft:piston'), it('thaumium_shovel'), SHARD('sloth'), it('crystal_aer'), SHARD('sloth'),
                                 it('thaumium_shovel'), it('item_grate'), it('thaumium_shovel'), SHARD('sloth'), it('crystal_terra'),
                                 SHARD('sloth'), it('thaumium_shovel')], 5, 'dump_jackboots')
# "RunicBodyWindings"/"RunicLegsWindings": paper + primal charm, nitor, research notes, crystalwell, salis mundus, diamond,
# and a TC4 balanced crystal cluster (-> a second primal charm, Thaumaturge's balanced item).
WIND = [('alienis', 64), ('praemunio', 128), ('praecantatio', 32), ('potentia', 64), ('cognitio', 128)]
NOTE = it('research_note')
NITOR = {'tag': 'thaumaturge:nitors'}   # Thaumaturge's nitor comes in 16 colours; any will do
infusion('runic_body_windings', 'runic_windings', WIND, it('minecraft:paper'),
         [it('primal_charm'), NITOR, NOTE, NOTE, NOTE, it(f'{FM}:crystalwell'), it('salis_mundus'), it('minecraft:diamond'),
          NOTE, NOTE, NOTE, it('primal_charm')], 10, 'runic_body_windings')
infusion('runic_legs_windings', 'runic_windings', WIND, it('minecraft:paper'),
         [it('primal_charm'), NITOR, NOTE, NOTE, it(f'{FM}:crystalwell'), it('salis_mundus'), it('minecraft:diamond'),
          NOTE, NOTE, it('primal_charm')], 10, 'runic_legs_windings')
# "TightBelt": the TC4 girdle of hovering (-> Thaumaturge's thaumostatic harness, the "thaumostatic girdle" of the
# research text) + thick taint bottles, taint slime and tendrils, taint shards.
TT_ = it(f'{NS}:bottle_of_thick_taint')
infusion('tight_belt', 'tight_belt', [('vitium', 96), ('praemunio', 96), ('mortuus', 128), ('humanus', 16)],
         it('thaumostatic_harness'),
         [TT_, TT_] + [it('tainted_goo')] * 4 + [SHARD('taint')] * 4 + [TT_] * 3 + [SHARD('taint')] * 4
         + [it('taint_tendril')] * 4 + [TT_], 8, 'tight_belt')
# "BurdeningAmulet": vis amulet + sinister lodestone (-> lodestone), envy shards, gold block, eye of ender, obsidian totem,
# motion and flight essences, sugar, an air shard.
infusion('burdening_amulet', 'burdening_amulet', [('praemunio', 96), ('mortuus', 96), ('alienis', 128), (f'{FM}:invidia', 128)],
         it('amulet_vis'),
         [it('minecraft:lodestone'), SHARD('envy'), SHARD('envy'), it('minecraft:gold_block'), SHARD('envy'), SHARD('envy'),
          it('minecraft:ender_eye'), SHARD('envy'), SHARD('envy'), it('obsidian_totem'), SHARD('envy'), crystal('motus'),
          it('minecraft:sugar'), crystal('motus'), crystal('volatus'), it('crystal_aer'), crystal('volatus')], 8, 'burdening_amulet')

# ---------------------------------------------------------------- research
CAT = f'{NS}:concilium'
C = CAT
def entry(name, col, row, icon, parents, notes, complexity=2, recipe_stage=(), warp=0, stages=2, meta=None, autounlock=False):
    st = []
    for i in range(stages):
        s = {'text': f'research.{NS}.{name}.stage_{i}'}
        if i == 0 and not autounlock:
            s['required_knowledge'] = [{'type': 'theory', 'category': CAT, 'amount': 1 if complexity < 3 else 2}]
        st.append(s)
    if warp: st[0]['warp'] = warp
    if recipe_stage: st[-1]['recipes'] = list(recipe_stage)
    obj = {'category': CAT, 'name': f'research.{NS}.{name}.title', 'parents': list(parents), 'column': col, 'row': row,
           'icons': [icon], 'stages': st, 'note_aspects': [{'aspect': asp(a), 'amount': n} for a, n in notes],
           'complexity': complexity}
    m = list(meta or []) + (['autounlock'] if autounlock else [])
    if m: obj['meta'] = m
    w(f'data/{NS}/thaumaturge/research_entry/{name}.json', obj)

# Original positions. Parents that are later-phase entries (THAUMATURGES, DESTCRYSTAL) are replaced by the Concilium
# root; TC4 entries map to Thaumaturge's (BOOTSTRAVELLER -> boots_traveller, TCBOTTLETAINT -> bottle_taint,
# RUNICAUGMENTATION -> runic_shielding, HOVERGIRDLE -> thaumostatic_harness, ROD_* -> rod_*).
entry('bottle_of_clear_water', 0, -6, f'{NS}:bottle_of_clear_water', [C], [('aqua', 1)], 1,
      [f'{NS}:crucible/bottle_of_clear_water'], stages=1, meta=['round'], autounlock=True)
entry('bottle_of_thick_taint', 3, -5, f'{NS}:bottle_of_thick_taint', [C, f'{T}:bottle_taint'],
      [('vitium', 3), ('aqua', 4), ('vitreus', 2)], 2, [f'{NS}:crucible/bottle_of_thick_taint'], warp=2)
entry('shard_mill', 0, -3, f'{NS}:shard_mill', [C, f'~{T}:rod_greatwood'],
      [('instrumentum', 3), ('vitreus', 3), ('perditio', 1), ('praecantatio', 1)], 2, [f'{NS}:arcane_workbench/shard_mill'])
entry('silverwood_shard_mill', 1, -4, f'{NS}:silverwood_shard_mill', [f'{NS}:shard_mill', f'~{T}:rod_silverwood'],
      [('instrumentum', 3), ('vitreus', 3), ('ordo', 1), ('praecantatio', 1)], 2, [f'{NS}:infusion/silverwood_shard_mill'], warp=1)
entry('runic_windings', 3, -3, f'{NS}:runic_body_windings', [C, f'~{T}:runic_shielding', f'~{FM}:crystalwell'],
      [('praemunio', 2), ('alienis', 3), ('cognitio', 4), ('praecantatio', 4)], 1,
      [f'{NS}:infusion/runic_body_windings', f'{NS}:infusion/runic_legs_windings'], warp=3, meta=['hidden'])
entry('dump_jackboots', -2, 9, f'{NS}:dump_jackboots', [C, f'~{T}:boots_traveller'],
      [('praemunio', 4), ('fabrico', 2), ('terra', 2), ('machina', 1)], 2, [f'{NS}:infusion/dump_jackboots'])
entry('tight_belt', 5, -4, f'{NS}:tight_belt', [f'{NS}:bottle_of_thick_taint', f'~{T}:thaumostatic_harness'],
      [('praemunio', 4), ('vitium', 6), ('humanus', 1), ('mortuus', 6)], 2, [f'{NS}:infusion/tight_belt'], warp=3)
entry('burdening_amulet', 7, -5, f'{NS}:burdening_amulet', [f'{NS}:tight_belt', f'~{T}:vis_amulet'],
      [('alienis', 4), ('praemunio', 6), ('sensus', 6), ('mortuus', 6), (f'{FM}:invidia', 2)], 2,
      [f'{NS}:infusion/burdening_amulet'], warp=3)

# ---------------------------------------------------------------- curios, item models
w('data/curios/tags/item/necklace.json', {'replace': False, 'values': [f'{NS}:burdening_amulet']})
w('data/curios/tags/item/belt.json', {'replace': False, 'values': [f'{NS}:tight_belt']})
# Gaze veils: worn on the head, they blind Death Gaze (BurdeningAmuletEvents). Other mods' masks can join the tag.
w(f'data/{NS}/tags/item/gaze_veils.json', {'replace': False, 'values': ['minecraft:carved_pumpkin']})
for item in ('runic_body_windings', 'runic_legs_windings', 'dump_jackboots', 'elemental_surfer_shorts', 'burdening_amulet',
             'tight_belt', 'bottle_of_thick_taint', 'bottle_of_clear_water'):
    w(f'assets/{NS}/models/item/{item}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{item}'}})
MILL_DISPLAY = {
    # Third person: no rotation or offset, only scale. MillArmPose follows this chain to find the crank's grip
    # (its DISPLAY_SCALE must match), and MillRenderer turns the mill itself while grinding.
    'thirdperson_righthand': {'scale': [0.8, 0.8, 0.8]},
    'thirdperson_lefthand': {'scale': [0.8, 0.8, 0.8]},
    # No yaw in first person: MillRenderer turns the mill so its bottom (model -Y) faces away from you while grinding.
    'firstperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.8, 0.8, 0.8]},
    'firstperson_lefthand': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.8, 0.8, 0.8]},
    'gui': {'rotation': [30, 225, 0], 'scale': [1.1, 1.1, 1.1]},
    'ground': {'translation': [0, 2, 0], 'scale': [0.6, 0.6, 0.6]},
    'fixed': {'scale': [1.0, 1.0, 1.0]},
}
# Each mill item is drawn by MillRenderer (builtin/entity), from two side-loaded OBJ models per wood: the body (body +
# cap) and the crank handle, which spins while grinding. The renderer loads models/item/mill/<wood>_<part>.json.
for item, wood in (('shard_mill', 'greatwood'), ('silverwood_shard_mill', 'silverwood')):
    tex = f'{NS}:item/{wood}_shard_mill'
    w(f'assets/{NS}/models/item/{item}.json', {'parent': 'minecraft:builtin/entity', 'textures': {'particle': tex},
                                                'display': MILL_DISPLAY})
    for part in ('body', 'handle'):
        w(f'assets/{NS}/models/item/mill/{wood}_{part}.json', {
            'loader': 'neoforge:obj', 'model': f'{NS}:models/item/shard_mill_{part}.obj', 'flip_v': True,
            'automatic_culling': False, 'textures': {'mill': tex, 'particle': tex}})
print('phase 3 generated')
