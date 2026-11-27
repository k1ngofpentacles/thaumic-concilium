"""Generates phase-7 data, cycle 1: the Valet seal (design: docs/design/phase7-design.md sections 1-2).
Original (Concilium, WTFPL): TCRecipeRegistry 1193-1207 (infusion), Thaumonomicon 814-823 (VALETCORE), art textures/items/valet_core.png.
TC4 -> Thaumaturge: Use core -> seal_use, Arcane Ear -> arcane_ear, Knowledge Fragment -> curio_knowledge, amulet blank -> amulet_mundane,
tool -> instrumentum, mechanism -> machina, senses -> sensus, man -> humanus, pride -> forbiddenmagic:superbia. Instability 8 / 2.5 = 3.
Run from the module root after gen_phase6c_data.py and before gen_lang.py.
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genlib import *

FM = 'forbiddenmagic'

# ---------------------------------------------------------------- recipe
infusion('seal_valet', 'valet_seal',
         [('instrumentum', 32), ('machina', 16), ('sensus', 32)],
         it('seal_use'), [it('arcane_ear'), it('curio_knowledge'), it('amulet_mundane'), it('curio_knowledge')], 8, 'seal_valet')

# ---------------------------------------------------------------- research
# Concilium tab (-7, -1), complexity 2, concealed; parent gadomancy:aura_core (TCGADOMANCY), hidden parents seal_use and arcane_ear (COREUSE, ARCANEEAR).
entry('valet_seal', -7, -1, f'{NS}:seal_valet',
      ['gadomancy:aura_core', f'~{T}:seal_use', f'~{T}:arcane_ear'],
      [('humanus', 8), (f'{FM}:superbia', 8), ('machina', 8)],
      [f'{NS}:infusion/seal_valet'], meta=['hidden'])

add_item_aspects('seal_valet', [('instrumentum', 10), ('sensus', 10), ('machina', 5)])

# ---------------------------------------------------------------- item model and art
# The original valet_core.png (animated) serves the placer item and the seal's in-world icon (Thaumaturge draws
# textures/item/seal_<type path>.png from the block atlas).
gen('seal_valet')
copy_from_jar({'textures/items/valet_core.png': 'item/seal_valet.png',
               'textures/items/valet_core.png.mcmeta': 'item/seal_valet.png.mcmeta'})

# ================================================================ cycle 2: the Vengeful Golem
# Original (VengefulGolem.func_70628_a): nextInt(2) + looting Knowledge Fragments. Looting adds one per level.
w(f'data/{NS}/loot_table/entities/vengeful_golem.json', {'type': 'minecraft:entity', 'random_sequence': f'{NS}:entities/vengeful_golem', 'pools': [
    {'rolls': 1.0, 'bonus_rolls': 0.0, 'entries': [{'type': 'minecraft:item', 'name': f'{T}:curio_knowledge', 'functions': [
        {'function': 'minecraft:set_count', 'add': False, 'count': {'type': 'minecraft:uniform', 'min': 0.0, 'max': 1.0}},
        {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting', 'count': 1.0}]}]}]})

# Head items that muffle the bell. PROVISIONAL(R6): our own Earmuffs item waits on the user, so the tag stays empty (nothing muffles).
w(f'data/{NS}/tags/item/muffles_bells.json', {'replace': False, 'values': []})

# Entity tag MAN 4, CRAFT 4, PRIDE 4, MOTION 4 (x2.5 = 10 each; the 5a convention).
add_entity_aspects('vengeful_golem', [('humanus', 10), ('fabrico', 10), (f'{FM}:superbia', 10), ('motus', 10)])
spawn_egg_model('vengeful_golem')

# Natural spawn (original: monster, weight vengefulGolemSpawnChance 5, groups of 1-2, every biome minus a blacklist). Our biome
# modifier takes the weight and the biome blacklist from TCConfig (vengefulGolem.spawnWeight / .biomeBlacklist); the biomes are the
# three dimensions' tags (user: every biome, overworld, Nether and End).
w(f'data/{NS}/neoforge/biome_modifier/vengeful_golem.json', {
    'type': f'{NS}:configured_spawns',
    'biomes': {'type': 'neoforge:or', 'values': ['#minecraft:is_overworld', '#minecraft:is_nether', '#minecraft:is_end']},
    'spawners': [{'type': f'{NS}:vengeful_golem', 'weight': 5, 'minCount': 1, 'maxCount': 2}]})

# Body texture: the original (WTFPL, 128x64 over 64x32 UVs). The brain and jar sat on Thaumcraft 4's golem_decoration.png (all rights
# reserved), so vengeful_golem_decoration.png is our own drawing of the same 128x128 layout: jar (96,56, 8x4x8), brain (96,70, 7x3x7).
copy_from_jar({'textures/entity/vengeful_golem.png': 'entity/vengeful_golem.png'})
_px = [(0, 0, 0, 0)] * (128 * 128)
def _rect(x0, y0, x1, y1, c):
    for y in range(y0, y1):
        for x in range(x0, x1):
            _px[y * 128 + x] = c
_rect(96, 56, 128, 68, (190, 225, 235, 110))          # jar glass, translucent
_rect(104, 56, 120, 64, (215, 240, 245, 150))         # top and bottom faces a little denser
_rect(96, 66, 128, 68, (150, 190, 205, 170))          # the base rim
for _x in range(97, 128, 8):
    _rect(_x, 64, _x + 1, 66, (235, 250, 255, 190))   # a glint on each side
_rect(96, 70, 124, 80, (214, 128, 150, 255))          # brain
for _y in range(70, 80):
    for _x in range(96, 124):
        if (_x * 3 + _y * 5) % 7 in (0, 1) and (_x + _y) % 2 == 0:
            _px[_y * 128 + _x] = (176, 88, 116, 255)  # folds
        elif (_x * 5 + _y * 3) % 11 == 0:
            _px[_y * 128 + _x] = (236, 160, 178, 255) # highlights
write_png(f'{ROOT}/assets/{NS}/textures/entity/vengeful_golem_decoration.png', 128, 128, _px)
