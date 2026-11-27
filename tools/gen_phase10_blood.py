"""Generates phase 10-5 data: Earmuffs (R6 A), the Blood Bracelet (Neo Vitae) and the Drainage Syringe (Horizons + Botania + Neo Vitae, Q6 B).
Original (Concilium 1.1.1, WTFPL): TCRecipeRegistry 1026-1055 (bracelet), 1343-1361 (syringe), Thaumonomicon 668-684, 914-930.
Facts: docs/design/phase10-original-facts.md 5.1, 5.2. TC4 -> Thaumaturge: tool -> instrumentum, hunger -> desiderium, life -> victus,
magic -> praecantatio, void -> vacuos, slime -> aqua (no Limus). Components: wandCap 0 -> wand_cap_iron, itemResource 14 -> salis_mundus
(as phase 3b), blockEssentiaReservoir -> essentia_reservoir, blockTube 6 (one-way valve) -> tube_oneway, itemResource 15 (primal charm) ->
primal_charm, itemResource 16 (void metal ingot) -> ingot_void.
Run after gen_phase3b_data.py (the bracelet's item model, curio tag and icon are there) and before gen_lang.py.
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genlib import *

NEOVITAE = [{'type': 'neoforge:mod_loaded', 'modid': 'neovitae'}]
SYRINGE_MODS = [{'type': 'neoforge:mod_loaded', 'modid': m} for m in ('thaumichorizons', 'botania', 'neovitae')]


def conditioned(path, cond):
    """Adds neoforge:conditions to a JSON file genlib just wrote."""
    obj = r(path)
    w(path, {'neoforge:conditions': cond} | obj)


# ---------------------------------------------------------------- Earmuffs (R6 A)
# A leather band over two wool pads. The tag muffles_bells is written by gen_phase7_data.py.
w(f'data/{NS}/recipe/earmuffs.json', {
    'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': ['LLL', 'W W'],
    'key': {'L': {'item': 'minecraft:leather'}, 'W': {'tag': 'minecraft:wool'}},
    'result': {'count': 1, 'id': f'{NS}:earmuffs'}})
gen('earmuffs')

# ---------------------------------------------------------------- Blood Bracelet (original BLOODBRACELET)
# Infusion, instability 5: tool 32, hunger 128, life 64, magic 64. Centre: Forbidden Magic's blood staff rod (wandCore meta 9).
# Components, two sets of: iron cap (wandCap 0), salis mundus (itemResource 14), the bloodstone brick (large brick in Blood Magic).
brick = it('neovitae:bloodstone_brick')
infusion('blood_bracelet', 'blood_bracelet',
         [('instrumentum', 32), ('desiderium', 128), ('victus', 64), ('praecantatio', 64)],
         it('forbiddenmagic:blood_staff_rod'),
         [it('wand_cap_iron'), it('salis_mundus'), brick, it('wand_cap_iron'), it('salis_mundus'), brick], 5, 'blood_bracelet')
conditioned(f'data/{NS}/recipe/infusion/blood_bracelet.json', NEOVITAE)
# Original grid 3,1, parent TCBASES (the bracelet base entry), hidden parents the iron bracelet (the same entry), ROD_blood, CAP_alchemical. Warp 1.
entry('blood_bracelet', 8, 3, f'{NS}:blood_bracelet',
      [f'{NS}:casting_bracelets', '~forbiddenmagic:rod_blood_staff', '~forbiddenmagic:cap_alchemical'],
      [('instrumentum', 10), ('desiderium', 15), ('victus', 10)], [f'{NS}:infusion/blood_bracelet'], warp=1)
conditioned(f'data/{NS}/thaumaturge/research_entry/blood_bracelet.json', NEOVITAE)

# ---------------------------------------------------------------- Drainage Syringe (original DRAINAGESYRINGE)
# Infusion, instability 10: tool 64, life 128, hunger 64, void 128, slime 32. Centre: Horizons' injector.
infusion('drainage_syringe', 'drainage_syringe',
         [('instrumentum', 64), ('victus', 128), ('desiderium', 64), ('vacuos', 128), ('aqua', 32)],
         it('thaumichorizons:injector'),
         [it('essentia_reservoir'), it('tube_oneway'), it('thaumichorizons:blood_syringe'), it('primal_charm'),
          it('minecraft:lever'), it('ingot_void')], 10, 'drainage_syringe')
conditioned(f'data/{NS}/recipe/infusion/drainage_syringe.json', SYRINGE_MODS)
# Original parent TCHORIZONS (our Horizons entry), hidden parents the injector and the essentia reservoir. Warp 3 (the item has 3 too).
entry('drainage_syringe', 8, -1, f'{NS}:drainage_syringe',
      [f'{NS}:thaumic_horizons', '~thaumichorizons:injector', f'~{T}:essentia_reservoir'],
      [('instrumentum', 8), ('vacuos', 8), ('aqua', 8), ('desiderium', 8)], [f'{NS}:infusion/drainage_syringe'], warp=3)
conditioned(f'data/{NS}/thaumaturge/research_entry/drainage_syringe.json', SYRINGE_MODS)

# The held model is the chief's (docs/consults/concilium-drainage-syringe.md): a BEWLR over builtin/entity. Written once, never over the chief's.
_model = os.path.join(ROOT, f'assets/{NS}/models/item/drainage_syringe.json')
if not os.path.exists(_model):
    w(f'assets/{NS}/models/item/drainage_syringe.json', {'parent': 'minecraft:builtin/entity'})
print('phase 10-5 blood generated')
