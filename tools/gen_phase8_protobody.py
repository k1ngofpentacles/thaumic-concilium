"""Generates phase 8-4 data: the Protobody (infusion, polishment, research), the Solid Void block (model, loot, research, aspects),
and the creature-infusion ban for Protolimbs. Design: docs/design/phase8-golems-facts.md section 6 and B3; brief
docs/agents/briefs/phase8-4-protobody.md.
Original (Concilium, WTFPL): TCRecipeRegistry 885-912 (infusion) and 1460-1462 (polishment), Thaumonomicon 1123-1146 (research),
textures/blocks/cosmos.png. TC4 -> Thaumaturge: man -> humanus, void -> vacuos, eldritch -> alienis, aura -> auram, flesh -> forbiddenmagic:corpus,
craft -> fabrico, crystal -> vitreus, wispy essence -> Thaumic Horizons' wisp essence (Horizons is required, so no fallback variant).
Instability 10 / 2.5 = 4. Run from the module root after gen_phase8_data.py and before gen_lang.py.
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genlib import *

TH = 'thaumichorizons'


def wisp(aspect):
    """Horizons' wisp essence of one aspect (the format of recipe/infusion/ethereal_manipulator_focus.json)."""
    return {'type': 'neoforge:components', 'items': f'{TH}:wisp_essence', 'components': {f'{TH}:wisp_aspect': {'aspect': asp(aspect)}}}


putty = it(f'{TH}:void_putty')
# Centre the Corpse Effigy; around it, alternately, four Void Putty and the essences of void, flesh (-> corpus), aura and man (-> humanus).
infusion('protobody', 'protobody', [('humanus', 128), ('vacuos', 256), ('alienis', 64), ('auram', 128)],
         it(f'{TH}:corpse_effigy'),
         [putty, wisp('vacuos'), putty, wisp('forbiddenmagic:corpus'), putty, wisp('auram'), putty, wisp('humanus')], 10, 'protobody')

# Polished infusion: corpus 64 (original FLESH 64), named after the research entry.
w(f'data/{NS}/{NS}/polishment/protobody.json', {'result': {'item': f'{NS}:protobody'}, 'aspect': asp('forbiddenmagic:corpus'), 'amount': 64})

# PROTOBODY (-8, 13) cx3, concealed, warp 4; parent FLESHCRUCIBLE, hidden parent cloneVillager.
entry('protobody', -8, 13, f'{NS}:protobody', [f'{NS}:flesh_crucible', f'~{TH}:clone_villager'],
      [('humanus', 8), ('vacuos', 8), ('forbiddenmagic:corpus', 8), ('alienis', 8), ('fabrico', 8)],
      [f'{NS}:infusion/protobody'], warp=4, complexity=3, meta=['hidden'])
# SOLIDVOID (-8, 14) cx3, round, LOST: granted by code (the sixth absorbed Protolimb), never by the book.
entry('solid_void', -8, 14, f'{NS}:solid_void', [f'{NS}:protobody'],
      [('vacuos', 8), ('vitreus', 8), ('alienis', 8), ('fabrico', 8)], stages=1, complexity=3, autounlock=True, meta=['hidden', 'round'])
_p = f'data/{NS}/thaumaturge/research_entry/solid_void.json'
_o = r(_p)
_o['meta'] = [m for m in _o.get('meta', []) if m != 'autounlock']
w(_p, _o)

add_item_aspects('protobody', [('humanus', 4), ('vacuos', 4), ('alienis', 4), ('perditio', 4)])
add_item_aspects('solid_void', [('vacuos', 2)])

# Protolimbs cannot be creature-infused (original: the class was in TileVat's ban list). Horizons' tag, extended from here.
w(f'data/{TH}/tags/entity_type/creature_infusion_banned.json', {'replace': False, 'values': [f'{NS}:protolimb']})

# ---------------------------------------------------------------- models
# Break particles and the item's particle sprite: vanilla obsidian by reference (the original's cosmos.png equals vanilla's end portal texture, which
# is not in the block atlas, so it cannot be a particle; nothing is copied).
PARTICLE = 'minecraft:block/obsidian'
DISPLAY_FIGURE = {
    'gui': {'rotation': [20, 30, 0], 'scale': [0.7, 0.7, 0.7]},
    'ground': {'translation': [0, 2, 0], 'scale': [0.65, 0.65, 0.65]},
    'fixed': {'scale': [0.6, 0.6, 0.6]},
    # the hand prefix lays a model on its back (its up along the arm); +90 about X stands the figure up again
    'thirdperson_righthand': {'rotation': [90, 0, 0], 'translation': [0, 1, 1], 'scale': [0.4, 0.4, 0.4]},
    'thirdperson_lefthand': {'rotation': [90, 0, 0], 'translation': [0, 1, 1], 'scale': [0.4, 0.4, 0.4]},
    'firstperson_righthand': {'rotation': [0, -20, 0], 'translation': [0, 3, 0], 'scale': [0.3, 0.3, 0.3]},
    'firstperson_lefthand': {'rotation': [0, -20, 0], 'translation': [0, 3, 0], 'scale': [0.3, 0.3, 0.3]}}
DISPLAY_CUBE = {
    'gui': {'rotation': [30, 225, 0], 'scale': [0.625, 0.625, 0.625]},
    'ground': {'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
    'fixed': {'scale': [0.5, 0.5, 0.5]},
    'thirdperson_righthand': {'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
    'thirdperson_lefthand': {'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.4, 0.4, 0.4]},
    'firstperson_lefthand': {'rotation': [0, 225, 0], 'scale': [0.4, 0.4, 0.4]}}
for name, display in (('protobody', DISPLAY_FIGURE), ('solid_void', DISPLAY_CUBE)):
    w(f'assets/{NS}/models/item/{name}.json', {'parent': 'minecraft:builtin/entity', 'gui_light': 'side',
                                               'textures': {'particle': PARTICLE}, 'display': display})
# The block is drawn by its block entity renderer (the end portal render type); the model only carries the break particles.
w(f'assets/{NS}/models/block/solid_void.json', {'textures': {'particle': PARTICLE}})
block_state_simple('solid_void')
drop_self_loot('solid_void')
