"""Generates phase-5a data. Cycle 1: Thaumcraft 4's primal arrows (six arrow items, the arcane recipes, the research entry, the item
icons and the glow sprite, the damage types and the arrow tags). Run after gen_phase6c_data.py (extends the item aspect data map)
and before gen_lang.py.

Thaumcraft 4 is All Rights Reserved: mechanics only. The icons and the glow are drawn here, original pixels.
TC4 -> Thaumaturge: primal shard -> essentia crystal of that primal, telum (weapon) -> aversio, vis 8 of the primal.
"""
import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genlib import *

KINDS = ['aer', 'ignis', 'aqua', 'terra', 'ordo', 'perditio']
# Primal colours as Thaumaturge shows them (used only for the icon art; the entity reads the live aspect colour).
COLOR = {'aer': (255, 240, 110), 'ignis': (255, 100, 20), 'aqua': (60, 200, 250), 'terra': (90, 190, 40),
         'ordo': (205, 205, 235), 'perditio': (70, 60, 90)}

# ---------------------------------------------------------------- recipes
# ConfigRecipes: AAA/ASA/AAA, A = arrow, S = the primal's shard -> 8 arrows, vis 8 of that primal.
for k in KINDS:
    arcane_shaped(f'primal_arrow_{k}', 'primal_arrows', ['AAA', 'ASA', 'AAA'],
                  {'A': it('minecraft:arrow'), 'S': crystal(k)}, [(k, 8)], f'primal_arrow_{k}', count=8)

# ---------------------------------------------------------------- research
# PRIMALARROW (Artifice tab): weapon 3 and each primal 3. Ours hangs off the tab root, right of it.
entry('primal_arrows', 2, 0, f'{NS}:primal_arrow_aer', [CAT],
      [('aversio', 3)] + [(k, 3) for k in KINDS], [f'{NS}:arcane_workbench/primal_arrow_{k}' for k in KINDS], complexity=2)

# ---------------------------------------------------------------- aspects (scanning)
for k in KINDS:
    add_item_aspects(f'primal_arrow_{k}', [('aversio', 1)])

# ---------------------------------------------------------------- damage types and tags
def dtype(name, extra=None):
    w(f'data/{NS}/damage_type/{name}.json', {'message_id': f'{NS}.{name}', 'scaling': 'when_caused_by_living_non_player',
                                            'exhaustion': 0.1, **(extra or {})})
dtype('air_arrow'); dtype('order_arrow'); dtype('fire_arrow', {'effects': 'burning'})
def dtag(ns, tag, values):
    w(f'data/{ns}/tags/damage_type/{tag}.json', {'replace': False, 'values': values})
projectiles = [f'{NS}:air_arrow', f'{NS}:fire_arrow', f'{NS}:order_arrow']
dtag('minecraft', 'is_projectile', projectiles)
dtag('minecraft', 'bypasses_armor', [f'{NS}:air_arrow', f'{NS}:order_arrow'])
dtag('minecraft', 'is_fire', [f'{NS}:fire_arrow'])
dtag('neoforge', 'is_magic', [f'{NS}:air_arrow', f'{NS}:order_arrow'])
items = [f'{NS}:primal_arrow_{k}' for k in KINDS]
w('data/minecraft/tags/item/arrows.json', {'replace': False, 'values': items})
w(f'data/{NS}/tags/item/primal_arrows.json', {'replace': False, 'values': items})

# ---------------------------------------------------------------- models and art
def icon(name, col):
    """16x16 arrow, tail bottom-left, point top-right: brown shaft, pale fletching, a crystal head in the primal's colour."""
    px = [(0, 0, 0, 0)] * 256
    def put(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            px[y * 16 + x] = tuple(c) + (255,)
    def shade(c, f):
        return tuple(max(0, min(255, int(v * f))) for v in c)
    for x in range(2, 12):                       # shaft: two pixels thick along the diagonal
        put(x, 15 - x, (150, 108, 62)); put(x, 16 - x, (104, 72, 40))
    for x in range(2, 6):                        # fletching: a pale vane each side of the shaft, a primal-coloured tail tip
        put(x, 14 - x, (236, 236, 240)); put(x, 17 - x, (200, 200, 208))
    put(1, 13, col); put(2, 15, shade(col, 0.7))
    for dy in range(-2, 3):                      # head: a diamond crystal
        for dx in range(-2, 3):
            d = abs(dx) + abs(dy)
            if d <= 2:
                put(12 + dx, 3 + dy, shade(col, 0.55) if d == 2 else shade(col, 1.0))
    put(11, 2, shade(col, 1.0) if col == COLOR['perditio'] else (255, 255, 255))
    put(12, 2, tuple(min(255, v + 70) for v in col))
    write_png(f'{ROOT}/assets/{NS}/textures/item/{name}.png', 16, 16, px)
    w(f'assets/{NS}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{name}'}})

for k in KINDS:
    icon(f'primal_arrow_{k}', COLOR[k])

# Glow sprite: white radial falloff (tinted and blended by the renderer).
size = 32
glow = []
for y in range(size):
    for x in range(size):
        d = math.hypot(x - 15.5, y - 15.5) / 16.0
        a = max(0.0, 1.0 - d)
        glow.append((255, 255, 255, int(255 * a * a)))
write_png(f'{ROOT}/assets/{NS}/textures/entity/primal_arrow_glow.png', size, size, glow)


# ================================================================ cycle 2: the Crimson Paladin, Archer and Ranger
# Original facts: docs/design/5-original-facts.md. Mobs are the code; this section is their data: drops, equipment, spawn, art.
# TC4 -> Thaumaturge: Knowledge Fragment (resource 9) -> curio_knowledge, Void Seed (17) -> void_seed, Gold Coin (18) -> gold_nugget,
# itemShard -> an essentia crystal of a random primal, itemPrimalArrow -> our primal arrows (random kind).
MOBS = ['crimson_paladin', 'crimson_archer', 'crimson_ranger']

def _count(lo, hi):
    return [{'function': 'minecraft:set_count', 'add': False, 'count': {'type': 'minecraft:uniform', 'min': float(lo), 'max': float(hi)}},
            {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
             'count': {'type': 'minecraft:uniform', 'min': 0.0, 'max': 1.0}}]

def _entry(name, weight=1, funcs=None, components=None):
    e = {'type': 'minecraft:item', 'name': name, 'weight': weight}
    f = list(funcs or [])
    if components:
        f.append({'function': 'minecraft:set_components', 'components': components})
    if f:
        e['functions'] = f
    return e

def _pool(entries, conditions=None):
    p = {'rolls': 1.0, 'bonus_rolls': 0.0, 'entries': entries}
    if conditions:
        p['conditions'] = conditions
    return p

def _loot(name, pools):
    w(f'data/{NS}/loot_table/entities/{name}.json', {'type': 'minecraft:entity', 'pools': pools,
                                                     'random_sequence': f'{NS}:entities/{name}'})

def _equip(name, items):
    w(f'data/{NS}/loot_table/equipment/{name}.json', {'type': 'minecraft:equipment', 'random_sequence': f'{NS}:equipment/{name}',
        'pools': [_pool([{'type': 'minecraft:item', 'name': i}]) for i in items]})

# EntityCultist.dropFewItems: nextInt(10): 0 fragment, 1 void seed, 2..3 a gold coin (1-3 with looting); six in ten nothing.
def base_pool():
    return _pool([_entry('thaumaturge:curio_knowledge'), _entry('thaumaturge:void_seed'),
                  _entry('minecraft:gold_nugget', 2, _count(1, 3)), {'type': 'minecraft:empty', 'weight': 6}])

# Paladin: its own roll and then the base roll too (two identical pools).
_loot('crimson_paladin', [base_pool(), base_pool()])
# Archer: the base roll; and on the coin outcome (one in five) half the time 1-3 primal arrows of a random kind.
_loot('crimson_archer', [base_pool(), _pool([_entry(f'{NS}:primal_arrow_{k}', 1, _count(1, 3)) for k in KINDS],
                                            [{'condition': 'minecraft:random_chance', 'chance': 0.1}])])
# Ranger: on the coin outcome half a coin and half a crystal of a random primal. Weights x6 so each primal is a whole weight.
_loot('crimson_ranger', [_pool([_entry('thaumaturge:curio_knowledge', 6), _entry('thaumaturge:void_seed', 6),
                                _entry('minecraft:gold_nugget', 6, _count(1, 3))]
                               + [_entry('thaumaturge:essentia_crystal', 1, _count(1, 3),
                                         {'thaumaturge:crystal_aspect': {'aspect': asp(k)}}) for k in KINDS]
                               + [{'type': 'minecraft:empty', 'weight': 36}])])

# Equipment (Thaumaturge's own cultist gear; the Thaumic Dyes pieces of the original have no equivalent). The Paladin's staff is set in code.
_equip('crimson_paladin', ['thaumaturge:crimson_robe_helm', 'thaumaturge:crimson_plate_chest', 'thaumaturge:crimson_robe_legs', 'thaumaturge:crimson_boots'])
_equip('crimson_archer', ['minecraft:bow', 'thaumaturge:crimson_ranger_helm', 'thaumaturge:crimson_robe_chest', 'thaumaturge:crimson_plate_legs',
                          'thaumaturge:crimson_boots'])
_equip('crimson_ranger', ['thaumaturge:crimson_ranger_helm', 'thaumaturge:crimson_ranger_chest', 'thaumaturge:crimson_ranger_legs',
                          'thaumaturge:crimson_boots'])

# Natural spawn: only the Ranger. Overworld, monster category, weight 5, groups of 1-2 (original: all biomes minus a blacklist).
add_spawns('crimson_ranger', 'crimson_ranger', 5, 1, 2)

# Scanning (original entity tags x2.5): Paladin man, life, eldritch, magic; Archer man, weapon, life, eldritch; Ranger man, travel, life, eldritch.
add_entity_aspects('crimson_paladin', [('humanus', 10), ('victus', 10), ('alienis', 10), ('praecantatio', 10)])
add_entity_aspects('crimson_archer', [('humanus', 10), ('aversio', 10), ('victus', 10), ('alienis', 10)])
add_entity_aspects('crimson_ranger', [('humanus', 10), ('motus', 10), ('victus', 10), ('alienis', 10)])

for m in MOBS:
    spawn_egg_model(m)
# Original body textures (WTFPL); the Ranger uses Thaumaturge's cultist texture by reference, not a copy.
copy_from_jar({'textures/entity/crimson_paladin.png': 'entity/crimson_paladin.png'})  # archer and ranger: the host cultist skin by reference (0.16.0)

# ================================================================ cycle 3: Lesser Crimson Portal and Crimson Pontifex
# Loot (CrimsonPontifex.dropFewItems): a Thaumcraft loot bag, meta 2 (the rare one), guaranteed. The research notes, the Pontifex hammer
# and the Pontifex robe the original also drops belong to the Annales / gear cycles (the items do not exist yet).
_loot('crimson_pontifex', [_pool([_entry('thaumaturge:loot_bag_rare')])])
# (cycle 4b: he holds his hammer and wears his own robe, as the original's func_82164_bB)
_equip('crimson_pontifex', [f'{NS}:pontifex_hammer', f'{NS}:pontifex_helm', f'{NS}:pontifex_chest', f'{NS}:pontifex_legs', f'{NS}:pontifex_boots'])
# Entity tags (x2.5): Portal eldritch, void, magic; Pontifex soul, life, magic.
add_entity_aspects('lesser_crimson_portal', [('alienis', 40), ('vacuos', 40), ('praecantatio', 40)])
add_entity_aspects('crimson_pontifex', [('spiritus', 40), ('victus', 40), ('praecantatio', 40)])
for m in ('lesser_crimson_portal', 'crimson_pontifex'):
    spawn_egg_model(m)
# Entities that enslave a player who is near (client walks the player toward them): the Pontifex now, the Vengeful Golem later.
w(f'data/{NS}/tags/entity_type/enslavers.json', {'replace': False, 'values': [f'{NS}:crimson_pontifex', f'{NS}:vengeful_golem']})

# ================================================================ cycle 4a: Crimson Annales, research, loot
# Annales item art (original, WTFPL) and the Pontifex research icon.
gen('crimson_annales')
copy_from_jar({'textures/items/crimson_annales.png': 'item/crimson_annales.png', 'textures/misc/pontifex.png': 'research/pontifex.png'})
add_item_aspects('crimson_annales', [('cognitio', 10), ('alienis', 10), ('humanus', 5)])

# CRIMSONANNALES (-8, 6): man 8, life 8, warp 3; parent TCTAINTEDMAGIC (Tainted Magic is not in this port), hidden parent CRIMSON =
# Thaumaturge's crimson_rites. Found by reading the book itself (CrimsonAnnales.use completes it), so it is hidden and round.
entry('crimson_annales', -8, 6, f'{NS}:crimson_annales', [CAT, '~thaumaturge:crimson_rites'],
      [('humanus', 8), ('victus', 8)], warp=3, meta=['hidden', 'round'])
# CRIMSONPONTIFEX (original -8, 8; spotlight_focus holds that cell here, so -8, 7): man 8, life 8, pride 8, void 8, eldritch 8, warp 2; parents the Annales, hidden the Brightest One.
# One page in the original; ours adds a closing paragraph saying what the knowledge unlocks.
entry('crimson_pontifex', -8, 7, f'{NS}:textures/research/pontifex.png', [f'{NS}:crimson_annales', f'~{NS}:brightest_one'],
      [('humanus', 8), ('victus', 8), ('forbiddenmagic:superbia', 8), ('vacuos', 8), ('alienis', 8)], warp=2, meta=['hidden', 'round'], stages=1)

# Chest loot (TaintedMagicIntegration.java:19-27). The original adds the Annales as a ChestGenHooks entry of weight w; a 1.7.10 chest
# picks n entries (with replacement) from its table of total weight T (vanilla entries, Forge's added enchanted book weight 1 or 2, and
# the Annales itself), so the chance of at least one Annales in a chest is 1 - (1 - w/T)^n, averaged over n = min..max.
# Our global loot modifier (neoforge:add_table) appends one pool that yields an Annales with exactly that per-chest chance.
# The Pontifex's rare loot bag (what the Pontifex drops): Thaumcraft's bag gives 8 + nextInt(5) rolls, each a weighted pick from the
# rare list (T 2547 = Config.java:526-558 sum, estimated potions ~100), but 2.5% x rarity 2 = 5% of the rolls are gear instead.
def effective_chance(w, T, nmin, nmax, skip=0.0):
    q = (1.0 - skip) * w / (T + w)
    ns = range(nmin, nmax + 1)
    return sum(1.0 - (1.0 - q) ** n for n in ns) / len(ns)

# key: (loot table, w, T before the Annales, n min, n max, share of rolls that are gear)
CHESTS = {'dungeon': ('minecraft:chests/simple_dungeon', 8, 120, 8, 8, 0.0),
          'jungle_temple': ('minecraft:chests/jungle_temple', 8, 73, 2, 7, 0.0),
          'desert_pyramid': ('minecraft:chests/desert_pyramid', 8, 73, 2, 7, 0.0),
          'mineshaft': ('minecraft:chests/abandoned_mineshaft', 4, 80, 3, 7, 0.0),
          'stronghold_corridor': ('minecraft:chests/stronghold_corridor', 4, 99, 2, 4, 0.0),
          'stronghold_crossing': ('minecraft:chests/stronghold_crossing', 4, 62, 1, 5, 0.0),
          'stronghold_library': ('minecraft:chests/stronghold_library', 10, 44, 1, 5, 0.0),
          'loot_bag_rare': ('thaumaturge:gameplay/loot_bag_rare', 30, 2547, 8, 12, 0.05)}
CHANCE = {k: round(effective_chance(v[1], v[2], v[3], v[4], v[5]), 4) for k, v in CHESTS.items()}
mods = []
for key, (table_id, *_rest) in CHESTS.items():
    weight = round(CHANCE[key] * 10000)
    w(f'data/{NS}/loot_table/chests/annales_{key}.json', {'type': 'minecraft:chest', 'pools': [_pool(
        [_entry(f'{NS}:crimson_annales', weight), {'type': 'minecraft:empty', 'weight': 10000 - weight}])]})
    w(f'data/{NS}/loot_modifiers/annales_{key}.json', {'type': 'neoforge:add_table', 'table': f'{NS}:chests/annales_{key}',
        'conditions': [{'condition': 'neoforge:loot_table_id', 'loot_table_id': table_id}]})
    mods.append(f'{NS}:annales_{key}')
w('data/neoforge/loot_modifiers/global_loot_modifiers.json', {'replace': False, 'entries': mods})

# ================================================================ cycle 4b: the Pontifex's hammer and robe
# Original art (WTFPL): the robe's armour layers (one texture serves the outer and inner layer names), the icon layers, the hammer OBJ and texture.
copy_from_jar({'textures/models/armor/pontifex_robe.png': 'models/armor/pontifex_robe_layer_1.png'})
copy_from_jar({'textures/models/armor/pontifex_robe.png': 'models/armor/pontifex_robe_layer_2.png'})
copy_from_jar({'textures/models/armor/pontifex_robe_overlay.png': 'models/armor/pontifex_robe_layer_1_overlay.png'})
copy_from_jar({'textures/models/armor/pontifex_robe_overlay.png': 'models/armor/pontifex_robe_layer_2_overlay.png'})
PIECES = ['helm', 'chest', 'legs', 'boots']
for i, piece in enumerate(PIECES):
    copy_from_jar({f'textures/items/pontifex_base{i}.png': f'item/pontifex_{piece}.png',
                   f'textures/items/pontifex_overlay{i}.png': f'item/pontifex_{piece}_overlay.png'})
    # two layers: the base takes the dye colour (tint index 0), the overlay stays as drawn (a client item colour handler)
    w(f'assets/{NS}/models/item/pontifex_{piece}.json', {'parent': 'minecraft:item/generated', 'textures': {
        'layer0': f'{NS}:item/pontifex_{piece}', 'layer1': f'{NS}:item/pontifex_{piece}_overlay'}})
w('data/minecraft/tags/item/dyeable.json', {'replace': False, 'values': [f'{NS}:pontifex_{p}' for p in PIECES]})

# The hammer: the original's Molot OBJ. Display transforms derive from PaladinHammerRenderer: first person (0.5, 1.0, 0.3) and 50 degrees about y,
# third person 45 degrees about y with (0, 1.3, 0.7), inventory scale 0.5 with -65 about x, ground (0, 1.5, 0); the OBJ is centred on the origin.
copy_from_jar({'textures/models/Molot.png': 'item/pontifex_hammer.png'})
obj_recentered('Molot', 'pontifex_hammer')
# Cycle 6b: the numbers of docs/design/5a-portal-hammer-render.md section 1 (the original's whole 1.7.10 GL chain multiplied out; measured
# against the reference shots, first person within 2 px). Left-hand blocks carry the same numbers (ItemTransform.apply mirrors them).
_FP = {'rotation': [0, 95, 0], 'translation': [-0.905, 3.2, -0.905], 'scale': [0.4, 0.4, 0.4]}
_TP = {'rotation': [0, -90, 10], 'translation': [0.071, 4.878, -1.389], 'scale': [0.625, 0.625, 0.625]}
HAMMER_DISPLAY = {
    'thirdperson_righthand': _TP,
    'thirdperson_lefthand': _TP,
    'firstperson_righthand': _FP,
    'firstperson_lefthand': _FP,
    'gui': {'rotation': [-41.75, -17.39, -42.19], 'translation': [0, 0, 0], 'scale': [0.3125, 0.3125, 0.3125]},
    'ground': {'translation': [0, 12, 0], 'scale': [0.5, 0.5, 0.5]},
    'fixed': {'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
}
obj_item_model('pontifex_hammer', display=HAMMER_DISPLAY)

# Recipes. The hammer: infusion (instability 7 in TC4), catalyst Thaumaturge's Crimson Blade (TC4 itemSwordCrimson). Aspects: weapon 64 -> aversio,
# life 64, metal 128, pride 32, exchange 16, hunger 16 -> desiderium. Tainted Magic's mace focus and materials do not exist here: the ten
# components keep the original's pairs (two gold blocks) and use Thaumaturge's void metal, thaumium and void seeds for the rest.
infusion('pontifex_hammer', 'pontifex_hammer',
         [('aversio', 64), ('victus', 64), ('metallum', 128), ('forbiddenmagic:superbia', 32), ('permutatio', 16), ('desiderium', 16)],
         it('thaumaturge:crimson_blade'),
         [it('thaumaturge:void_seed'), it('minecraft:gold_block'), it('thaumaturge:ingot_void'), it('thaumaturge:ingot_void'),
          it('thaumaturge:ingot_thaumium'), it('thaumaturge:void_seed'), it('thaumaturge:ingot_thaumium'), it('thaumaturge:ingot_void'),
          it('thaumaturge:ingot_void'), it('minecraft:gold_block')], 7, 'pontifex_hammer')
# The robe: a rift recipe per piece (original: addChainedRiftRecipe, catalyst Thaumcraft's cultist robe piece): life 128, void 128, armor 256,
# exchange 64, flesh 256 (no flesh aspect: victus).
for piece, cat in zip(PIECES, ['crimson_robe_helm', 'crimson_robe_chest', 'crimson_robe_legs', 'crimson_boots']):
    rift_recipe(f'pontifex_{piece}', it(f'thaumaturge:{cat}'),
                [('victus', 384), ('vacuos', 128), ('praemunio', 256), ('permutatio', 64)], 'pontifex_robe', f'pontifex_{piece}')

# Research (-6,10), (-7,11), (-6,12). Lost entries: the Pontifex's research note (a hint note, the first stage's obtain item) opens them.
# PONTIFEXHAMMER: man, life, pride, weapon, hunger 8, warp 2. PONTIFEXROBE: man, life, pride, armor, craft 8, warp 2. CRIMSONINITIATION: man, life,
# exchange, mind 8, warp 3. The originals' other parents (Material Peeler, Praetor Armor, Crimson Blade) do not exist in this pack.
entry('pontifex_hammer', -6, 10, f'{NS}:pontifex_hammer', [f'{NS}:crimson_pontifex'],
      [('humanus', 8), ('victus', 8), ('forbiddenmagic:superbia', 8), ('aversio', 8), ('desiderium', 8)], [f'{NS}:infusion/pontifex_hammer'],
      warp=2, meta=['hidden'])
entry('pontifex_robe', -7, 11, f'{NS}:pontifex_helm', [f'{NS}:crimson_pontifex'],
      [('humanus', 8), ('victus', 8), ('forbiddenmagic:superbia', 8), ('praemunio', 8), ('fabrico', 8)], warp=2, meta=['hidden'])
entry('crimson_initiation', -6, 12, f'{NS}:pontifex_chest', [f'{NS}:pontifex_robe'],
      [('humanus', 8), ('victus', 8), ('permutatio', 8), ('cognitio', 8)], warp=3, meta=['hidden', 'round'], stages=1)
for key in ('pontifex_hammer', 'pontifex_robe'):
    path = f'data/{NS}/thaumaturge/research_entry/{key}.json'
    obj = r(path)
    obj['stages'][0]['obtain'] = [{'items': f'{NS}:research_hint', 'amount': 1, 'components': {f'{NS}:hint': f'{NS}:{key}'}}]
    w(path, obj)

for piece in PIECES:   # original registerObjectTag: mind, cloth, armor, hunger, pride 16 each
    add_item_aspects(f'pontifex_{piece}', [('cognitio', 16), ('fabrico', 16), ('praemunio', 16), ('desiderium', 16), ('forbiddenmagic:superbia', 16)])
add_item_aspects('pontifex_hammer', [('aversio', 20), ('victus', 10), ('metallum', 20)])

# ================================================================ cycle 6: Crimson mob art (overlays + the Gilded Crimson Helm)
# Fable memo docs/design/5a-mob-overlay-art.md. All pixels are ours (Thaumaturge's art is never copied). 64x32 textures, box unwrap as vanilla.
STEEL, DARK, LIGHT, SLIT = (48, 48, 50, 255), (30, 30, 32, 255), (72, 72, 78, 255), (8, 8, 8, 255)
GOLD, GMID, GSHADE, GGLINT = (200, 145, 5, 255), (170, 122, 2, 255), (120, 82, 0, 255), (225, 175, 40, 255)
LEATHER, LDARK, LLIGHT = (58, 37, 31, 255), (40, 24, 20, 255), (85, 59, 52, 255)
CLEAR = (0, 0, 0, 0)

class Tex:
    def __init__(self, wid=64, hei=32):
        self.w, self.h, self.px = wid, hei, [CLEAR] * (wid * hei)
    def put(self, x, y, c):
        self.px[y * self.w + x] = c
    def rect(self, x0, y0, x1, y1, c):          # [x0, x1) x [y0, y1)
        for y in range(y0, y1):
            for x in range(x0, x1):
                self.put(x, y, c)
    def pattern(self, x0, y0, rows, key):
        for dy, row in enumerate(rows):
            for dx, ch in enumerate(row):
                if ch in key:
                    self.put(x0 + dx, y0 + dy, key[ch])
    def save(self, path):
        write_png(f'{ROOT}/{path}', self.w, self.h, self.px)

# --- Archer: visor (head, texOffs 0,0, 10x6x1), quiver (body, 0,8, 4x11x3), arrows (quiver, 16,8, 3x3x1)
t = Tex()
t.rect(0, 0, 22, 7, DARK)
t.pattern(1, 1, ['GGGGGGGGGG', 'GKKKKKKKKG', 'SSSGKKGSSS', 'ShSGKKGShS', 'SSSGKKGSSS', 'ShSGGGGShS'],
          {'G': GOLD, 'K': SLIT, 'S': STEEL, 'h': DARK})
t.rect(0, 8, 14, 22, LEATHER)                   # quiver: all faces leather
t.rect(3, 8, 11, 11, LLIGHT)                    # top and bottom (open end and base)
t.rect(10, 11, 14, 22, LDARK)                   # outer face (+z): dark rim, lighter sigil, gold buckle glint
t.rect(11, 12, 13, 21, LEATHER)
t.pattern(11, 13, ['.L', 'LL', 'L.', 'LL', '.L'], {'L': LLIGHT})
t.put(12, 19, GGLINT)
SHAFT, FLETCH, RED = (110, 80, 45, 255), (200, 200, 200, 255), (150, 25, 25, 255)
t.rect(16, 8, 24, 12, SHAFT)
for fx in (17, 21):                              # front u17-20 v9-12 and back u21-24 v9-12: fletching rows over a red band
    for k in range(3):
        t.put(fx + k, 9, FLETCH); t.put(fx + k, 10, FLETCH); t.put(fx + k, 11, RED)
t.save('assets/thaumicconcilium/textures/entity/overlay/crimson_archer.png')

# (0.16.0: the Ranger overlay of spikes and cuffs is gone; it clashed with the worn armour sets)

# --- Gilded helm armour layer (shell 0,0 8x8x8; fin 0,16 1x3x6; fin tip 16,16 1x2x4). Layer file name follows the armour material.
t = Tex()
t.rect(0, 0, 32, 16, DARK)
for fx in (8, 16):                               # top and bottom faces: a light seam along the top edge
    t.rect(fx, 0, fx + 8, 1, LIGHT)
for fx in (0, 8, 16, 24):                        # side faces: light 1 px seams along the top and left edge
    t.rect(fx, 8, fx + 8, 9, LIGHT); t.rect(fx, 8, fx + 1, 16, LIGHT)
t.rect(0, 11, 8, 12, GOLD); t.rect(16, 11, 24, 12, GOLD)      # the gold band on both sides, to the fin root
t.pattern(8, 8, ['ssssssss', 'sGGssGGs', 'sGssssGs', 'GGGGGGGG', 'KKKKKKKK', 'sGGKKGGs', 'GsGKKGsG', 'sGGKKGGs'],
          {'s': DARK, 'G': GOLD, 'K': SLIT})
t.rect(0, 16, 14, 25, GMID)
t.rect(0, 22, 6, 24, GOLD); t.rect(7, 22, 13, 24, GOLD)       # lit outer faces
t.rect(0, 24, 14, 25, GSHADE)                                 # shadow lower edge
t.put(2, 22, GGLINT)
t.rect(16, 16, 26, 22, GOLD); t.rect(16, 21, 26, 22, GSHADE); t.put(17, 18, GGLINT)
t.save('assets/thaumicconcilium/textures/models/armor/gilded_crimson_layer_1.png')

# --- the item icon
ICON = ['................', '..G..........G..', '..GG..DDDD..GG..', '..GM.DLLLLD.MG..', '...GDLLDDLLDG...', '...DLDDDDDDLD...',
        '...DGGGGGGGGD...', '...DKKKKKKKKD...', '...DDKKGGKKDD...', '...DDGKKKKGDD...', '...DLDKKKKDLD...', '...DL.KKKK.LD...',
        '...DD..KK..DD...', '....D......D....', '................', '................']
assert all(len(row) == 16 for row in ICON)
t = Tex(16, 16)
t.pattern(0, 0, ICON, {'D': DARK, 'L': LIGHT, 'G': GOLD, 'M': GMID, 'K': SLIT})
t.put(2, 3, GGLINT)
t.save('assets/thaumicconcilium/textures/item/gilded_crimson_helm.png')
w(f'assets/{NS}/models/item/gilded_crimson_helm.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/gilded_crimson_helm'}})
w('data/minecraft/tags/item/head_armor.json', {'replace': False, 'values': [f'{NS}:gilded_crimson_helm']})
add_item_aspects('gilded_crimson_helm', [('praemunio', 16), ('metallum', 8)])
