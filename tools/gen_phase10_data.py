"""Generates phase 10-1 data: the Chort and the Sloppy Alchemist (only with Just Another Witchery Remake, `witchery`).
Original (Concilium, WTFPL): Chort.java (drops 70-77), SloppyAlchemist.java, TCEntityRegistry.java 95-116 (aspects), 425-447 (spawn),
Thaumonomicon.java 225-226 (page text). Facts: docs/design/phase10-original-facts.md 4.1, 4.2.
TC4 -> Thaumaturge aspects (x2.5 of the original 4): beast -> bestia, greed -> desiderium, magic -> praecantatio, nether -> infernum
(thaumadditions:infernum, as the Nether Explorer), man -> humanus, trap -> vinculum, exchange -> permutatio.
Every data file that can carries neoforge:mod_loaded witchery; the shared data maps cannot (one file per registry), and an entry for an
entity type that is not registered is skipped by the game, as the Withered Botanist's is without Botania.
Run from the module root after gen_phase4_data.py (it rewrites the 'fallen' entry this one extends) and before gen_lang.py.
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genlib import *

WITCHERY = [{'type': 'neoforge:mod_loaded', 'modid': 'witchery'}]
INFERNUM = 'thaumadditions:infernum'

# ---------------------------------------------------------------- aspects (scanning)
# Each entry carries its own mod_loaded condition (NeoForge skips a data map entry whose conditions fail).
_p = f'data/{T}/data_maps/entity_type/entity_aspects.json'
_m = r(_p)
for _mob, _pairs in (('chort', [('bestia', 10), ('desiderium', 10), ('praecantatio', 10), (INFERNUM, 10)]),
                     ('sloppy_alchemist', [('humanus', 10), ('vinculum', 10), ('permutatio', 10), (INFERNUM, 10)])):
    _m['values'][f'{NS}:{_mob}'] = {'neoforge:conditions': WITCHERY, 'value': aspects(_pairs)}
w(_p, _m)

# ---------------------------------------------------------------- drops (R8): original r = nextInt(10): r<3 foul fume x0-3,
# r<6 exhale of the horned one x0-3, else one moonshine (the remake has no moonshine: its mandrake root, the brew's ingredient).
def count(lo, hi):
    return [{'function': 'minecraft:set_count', 'add': False, 'count': {'type': 'minecraft:uniform', 'min': float(lo), 'max': float(hi)}}]

w(f'data/{NS}/loot_table/entities/chort.json', {
    'neoforge:conditions': WITCHERY,
    'type': 'minecraft:entity', 'random_sequence': f'{NS}:entities/chort',
    'pools': [{'rolls': 1.0, 'bonus_rolls': 0.0, 'entries': [
        {'type': 'minecraft:item', 'name': 'witchery:foul_fume', 'weight': 3, 'functions': count(0, 3)},
        {'type': 'minecraft:item', 'name': 'witchery:exhale_of_the_horned_one', 'weight': 3, 'functions': count(0, 3)},
        {'type': 'minecraft:item', 'name': 'witchery:mandrake_root', 'weight': 4},
    ]}]})
# The Sloppy Alchemist drops nothing: his reward is given by his right-click (SloppyAlchemist.mobInteract).
w(f'data/{NS}/loot_table/entities/sloppy_alchemist.json', {
    'neoforge:conditions': WITCHERY, 'type': 'minecraft:entity', 'random_sequence': f'{NS}:entities/sloppy_alchemist', 'pools': []})

# ---------------------------------------------------------------- spawn (original: monster, weight 5, group 1-1, Nether biomes minus a
# blacklist). The weight and blacklist come from the config (ConfiguredSpawns). The Chort never spawns alone: he comes on the alchemist.
w(f'data/{NS}/neoforge/biome_modifier/sloppy_alchemist.json', {
    'neoforge:conditions': WITCHERY,
    'type': f'{NS}:configured_spawns', 'biomes': '#minecraft:is_nether',
    'spawners': [{'type': f'{NS}:sloppy_alchemist', 'weight': 5, 'minCount': 1, 'maxCount': 1}]})

# ---------------------------------------------------------------- models, art
spawn_egg_model('chort')
spawn_egg_model('sloppy_alchemist')
# Placeholder textures (WTFPL originals: ChortRenderer's chort.png, SloppyAlchemistRenderer's excsi.png). Not overwritten when the
# look agent's art is already there.
for src, dst in {'textures/entity/chort.png': 'entity/chort.png', 'textures/entity/excsi.png': 'entity/sloppy_alchemist.png'}.items():
    if not os.path.exists(os.path.join(A, 'textures', dst)):
        copy_from_jar({src: dst})

# ---------------------------------------------------------------- research
# 10-1b (lane decision): the original's page (sloppyalchemist1, the virtual research TCWITCHERYBM) is its own hidden entry, conditioned on
# witchery (the 'fallen' entry is not conditional and a scan key of an unregistered entity fails the datapack check), parent 'fallen',
# unlocked by scanning the alchemist. Icon: the original research icon gui/sloppy_alchemist.png (WTFPL).
copy_from_jar({'textures/gui/sloppy_alchemist.png': 'gui/sloppy_alchemist.png'})
entry('sloppy_alchemist', -4, -4, f'{NS}:textures/gui/sloppy_alchemist.png',
      [f'{NS}:fallen', f'{T}:scanned/entity/{NS}/sloppy_alchemist'],
      [('humanus', 8), ('permutatio', 8)], stages=1, autounlock=True, meta=['hidden'], complexity=1)
_p = f'data/{NS}/thaumaturge/research_entry/sloppy_alchemist.json'
_e = r(_p)
w(_p, {'neoforge:conditions': WITCHERY, **_e})
# Scanning either opens the same 'fallen' key as the others, from a scan file of their own that is skipped without Witchery
# (the shared scan/fallen.json fails to load with an entity type that is not registered).
w(f'data/{NS}/thaumaturge/scan/witchery_fallen.json', {
    'neoforge:conditions': WITCHERY, 'entities': [f'{NS}:chort', f'{NS}:sloppy_alchemist', f'{NS}:burned_witch'], 'key': f'{NS}:scanned/fallen'})

# ================================================================ phase 10-2: the Burned Witch (original BurnedWitch.java, TCEntityRegistry.java 90-94,
# 448-462, Thaumonomicon.java 227-228). Same gate as the pair.
# Aspects (original MAN 4, MAGIC 4, DEATH 4, NETHER 4 -> x2.5): humanus, praecantatio, mortuus, infernum (as the Nether Explorer).
_p = f'data/{T}/data_maps/entity_type/entity_aspects.json'
_m = r(_p)
_m['values'][f'{NS}:burned_witch'] = {'neoforge:conditions': WITCHERY,
    'value': aspects([('humanus', 10), ('praecantatio', 10), ('mortuus', 10), (INFERNUM, 10)])}
w(_p, _m)

# Drops (R8, original func_70628_a): j = nextInt(3) + 1 stacks; each stack picks one of five items with equal chance and drops
# nextInt(3) copies (+ nextInt(looting + 1)). Pool: Items.field_151065_br = blaze powder, field_151073_bk = ghast tear, and the Witchery
# GENERIC metas 28 foulFume, 30 oilOfVitriol, 35 reekOfMisfortune -> the remake's items of the same names. Looting is exact at every
# level (user, 2026-11-27): our thaumicconcilium:uniform_enchant_bonus adds nextInt(L + 1) (registry/TCLootFunctions); vanilla's
# enchanted_count_increase (round(L * uniform(0, 1))) matched only at 0 and I.
_looting = {'function': f'{NS}:uniform_enchant_bonus', 'enchantment': 'minecraft:looting'}
_witch_drops = ['minecraft:blaze_powder', 'minecraft:ghast_tear', 'witchery:foul_fume', 'witchery:oil_of_vitriol', 'witchery:reek_of_misfortune']
w(f'data/{NS}/loot_table/entities/burned_witch.json', {
    'neoforge:conditions': WITCHERY,
    'type': 'minecraft:entity', 'random_sequence': f'{NS}:entities/burned_witch',
    'pools': [{'rolls': {'type': 'minecraft:uniform', 'min': 1.0, 'max': 3.0}, 'bonus_rolls': 0.0, 'entries': [
        {'type': 'minecraft:item', 'name': _n, 'weight': 1, 'functions': count(0, 2) + [_looting]} for _n in _witch_drops]}]})

# Spawn: monster, weight 5 (config), group 1-1, every Nether biome minus the config blacklist.
w(f'data/{NS}/neoforge/biome_modifier/burned_witch.json', {
    'neoforge:conditions': WITCHERY,
    'type': f'{NS}:configured_spawns', 'biomes': '#minecraft:is_nether',
    'spawners': [{'type': f'{NS}:burned_witch', 'weight': 5, 'minCount': 1, 'maxCount': 1}]})

spawn_egg_model('burned_witch')
if not os.path.exists(os.path.join(A, 'textures', 'entity/burned_witch.png')):
    copy_from_jar({'textures/entity/burned_witch.png': 'entity/burned_witch.png'})

# Research: the original's TCWITCHERY pages burnedwitch1/2 as a hidden entry beside the alchemist's (like 10-1b), child of 'fallen',
# unlocked by scanning her. Icon: the original's page picture gui/burned_witch.png (WTFPL), also shown on the page.
copy_from_jar({'textures/gui/burned_witch.png': 'gui/burned_witch.png'})
entry('burned_witch', -3, -4, f'{NS}:textures/gui/burned_witch.png',
      [f'{NS}:fallen', f'{T}:scanned/entity/{NS}/burned_witch'],
      [('humanus', 8), ('praecantatio', 8)], stages=1, autounlock=True, meta=['hidden'], complexity=1)
_p = f'data/{NS}/thaumaturge/research_entry/burned_witch.json'
_e = r(_p)
w(_p, {'neoforge:conditions': WITCHERY, **_e})

# ================================================================ phase 10-3: the Crimson Spells (original CRIMSONSPELLS, Thaumonomicon.java:1108-1121).
# MAN, LIFE, MIND, MAGIC, WEAPON 8 -> humanus, victus, cognitio, praecantatio, aversio; warp 1; "lost" (found through the Pontifex's note) and
# round, parent the Pontifex. Original (-5, 9); ours (-9, 9), a free cell beside the Pontifex's others. Icon: the original used TC4's jar label;
# Thaumaturge has none, so the page picture (WTFPL) serves, as for the other Witchery entries. Only with witchery.
copy_from_jar({'textures/gui/crimson_spells.png': 'gui/crimson_spells.png'})
entry('crimson_spells', -9, 9, f'{NS}:textures/gui/crimson_spells.png', [f'{NS}:crimson_pontifex'],
      [('humanus', 8), ('victus', 8), ('cognitio', 8), ('praecantatio', 8), ('aversio', 8)], warp=1, meta=['hidden', 'round'], complexity=1)
_p = f'data/{NS}/thaumaturge/research_entry/crimson_spells.json'
_e = r(_p)
_e['stages'][0]['obtain'] = [{'items': f'{NS}:research_hint', 'amount': 1, 'components': {f'{NS}:hint': f'{NS}:crimson_spells'}}]
w(_p, {'neoforge:conditions': WITCHERY, **_e})
