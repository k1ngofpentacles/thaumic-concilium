"""Generates phase-3b datapack JSON and art: the casting bracelets (recipes, research, Curios slot and tags, item models)
and their item icons, drawn here (original art). Run from the repo root, after gen_phase3_data.py.

Translated from TCRecipeRegistry/Thaumonomicon (Concilium 1.1.1) and Thaumic Bases' bracelet recipes, re-flavoured
onto Thaumaturge materials (see docs/PORT_STATUS.md, phase 3b). The livingwood, dreamwood and terrasteel bracelets
carry a botania mod_loaded condition, as their items only exist with Botania. Same rules as phase 3: TC4 aspect amounts kept,
instability / 2.5, arcane vis = sum of costs with crystals cost / 10, research notes = TC4 amount / 2.5.
"""
import json, math, os, struct, zlib
NS = 'thaumicconcilium'
ROOT = 'src/main/resources'
T = 'thaumaturge'
FM = 'forbiddenmagic'
INFERNUM = 'thaumadditions:infernum'
PRIMALS = ['aer', 'terra', 'ignis', 'aqua', 'ordo', 'perditio']


def w(path, obj):
    p = os.path.join(ROOT, path); os.makedirs(os.path.dirname(p), exist_ok=True)
    json.dump(obj, open(p, 'w'), indent=2); open(p, 'a').write('\n')
def it(i): return {'item': i if ':' in i else f'{T}:{i}'}
def asp(a): return a if ':' in a else f'{T}:{a}'
def crystal(a):
    return {'type': 'neoforge:components', 'items': 'thaumaturge:essentia_crystal',
            'components': {'thaumaturge:crystal_aspect': {'aspect': asp(a)}}}
def instab(tc4): return max(1, min(6, round(tc4 / 2.5)))
def gate(key): return {'entry': f'{NS}:{key}', 'stage': 1}
def note(n): return max(1, round(n / 2.5))


BOTANIA = [{'type': 'neoforge:mod_loaded', 'modid': 'botania'}]
def BOT(i): return it(f'botania:{i}')


def infusion(key, aspects, catalyst, components, tc4_instability, cond=None):
    w(f'data/{NS}/recipe/infusion/{key}.json', ({'neoforge:conditions': cond} if cond else {}) | {
        'type': 'thaumaturge:infusion', 'aspects': [{'aspect': asp(a), 'amount': n} for a, n in aspects],
        'catalyst': catalyst, 'components': components, 'instability': instab(tc4_instability),
        'research': gate(key), 'result': {'count': 1, 'id': f'{NS}:{key}'}})


def arcane(key, research, pattern, keys, primal_cost):
    costs = [(a, primal_cost) for a in PRIMALS]
    w(f'data/{NS}/recipe/arcane_workbench/{key}.json', {
        'type': 'thaumaturge:arcane_workbench_shaped', 'pattern': pattern, 'key': keys,
        'vis': sum(n for _, n in costs), 'crystals': [{'aspect': asp(a), 'amount': math.ceil(n / 10)} for a, n in costs],
        'research': gate(research), 'result': {'count': 1, 'id': f'{NS}:{key}'}})


# ---------------------------------------------------------------- recipes
# Thaumic Bases "TB.Bracelet.Iron": CIC / N N, iron cap, iron ingot, iron nuggets; 1 of each primal.
arcane('iron_bracelet', 'casting_bracelets', ['CIC', 'N N'],
       {'C': it('wand_cap_iron'), 'I': it('minecraft:iron_ingot'), 'N': it('minecraft:iron_nugget')}, 1)
# "TB.Bracelet.Thaumium": CIC / INI / N N, thauminite caps (-> thaumium caps), thaumium, salis mundus; 25 each.
arcane('thaumium_bracelet', 'casting_bracelets', ['CIC', 'INI', 'N N'],
       {'C': it('wand_cap_thaumium'), 'I': it('ingot_thaumium'), 'N': it('salis_mundus')}, 25)
# "TB.Bracelet.Void": void ingot + 2 void caps, void dust blocks, void metal, crystal blocks; eldritch/void/magic 64,
# aura/energy 24, instability 8.
infusion('void_bracelet', [('alienis', 64), ('vacuos', 64), ('praecantatio', 64), ('auram', 24), ('potentia', 24)],
         it('ingot_void'), [it('wand_cap_void'), it('salis_mundus'), it('nugget_void'), crystal('vacuos'),
                            it('wand_cap_void'), it('salis_mundus'), it('nugget_void'), crystal('vacuos')], 8)
# Concilium's Botania bracelets, only with Botania (Botania Revived), on Forbidden Magic's Botania rods and caps.
# "LivingwoodBracelet": livingwood rod + manasteel caps, salis, mana diamonds. tool 32, tree 128, life 64, magic 64.
infusion('livingwood_bracelet', [('instrumentum', 32), ('herba', 128), ('victus', 64), ('praecantatio', 64)],
         it(f'{FM}:livingwood_rod'), [it(f'{FM}:manasteel_cap'), it('salis_mundus'), BOT('mana_diamond'),
                                      it(f'{FM}:manasteel_cap'), it('salis_mundus'), BOT('mana_diamond')], 5, BOTANIA)
# "DreamwoodBracelet": dreamwood staff core + elementium caps, salis, dragonstones. tool 32, tree 128, soul 64, magic 64.
infusion('dreamwood_bracelet', [('instrumentum', 32), ('herba', 128), ('spiritus', 64), ('praecantatio', 64)],
         it(f'{FM}:dreamwood_staff_rod'), [it(f'{FM}:elementium_cap'), it('salis_mundus'), BOT('dragonstone'),
                                           it(f'{FM}:elementium_cap'), it('salis_mundus'), BOT('dragonstone')], 5, BOTANIA)
# "TerrasteelBracelet": terrasteel block + terrasteel caps, salis, gaia spirit ingots.
# tool 32, metal 128, earth 64, energy 64, magic 64, instability 7.
infusion('terrasteel_bracelet', [('instrumentum', 32), ('metallum', 128), ('terra', 64), ('potentia', 64), ('praecantatio', 64)],
         BOT('terrasteel_block'), [it(f'{FM}:terrasteel_cap'), it('salis_mundus'), BOT('gaia_ingot'),
                                   it(f'{FM}:terrasteel_cap'), it('salis_mundus'), BOT('gaia_ingot')], 7, BOTANIA)
# "InfernalBracelet": infernal core + thaumium caps, salis, pride shards. tool 32, nether 128, pride 64, wrath 64.
infusion('infernal_bracelet', [('instrumentum', 32), (INFERNUM, 128), (f'{FM}:superbia', 64), (f'{FM}:ira', 64)],
         it(f'{FM}:infernal_rod'), [it('wand_cap_thaumium'), it('salis_mundus'), it(f'{FM}:pride_shard'),
                                    it('wand_cap_thaumium'), it('salis_mundus'), it(f'{FM}:pride_shard')], 5)
# "TaintedBracelet": tainted core + thaumium caps, salis, taint shards. tool 32, taint 128, entropy 64, magic 64.
infusion('tainted_bracelet', [('instrumentum', 32), ('vitium', 128), ('perditio', 64), ('praecantatio', 64)],
         it(f'{FM}:tainted_rod'), [it('wand_cap_thaumium'), it('salis_mundus'), it(f'{FM}:taint_shard'),
                                   it('wand_cap_thaumium'), it('salis_mundus'), it(f'{FM}:taint_shard')], 5)
# "WarpwoodBracelet" -> eldritch: warpwood core + shadowmetal caps, salis, shadowmetal and crimson cloth.
# tool 32, eldritch 128, mind 64, magic 64.
infusion('eldritch_bracelet', [('instrumentum', 32), ('alienis', 128), ('cognitio', 64), ('praecantatio', 64)],
         it('void_seed'), [it('wand_cap_void'), it('salis_mundus'), it('ingot_void'),
                           it('wand_cap_void'), it('salis_mundus'), it('minecraft:ender_eye')], 5)

# ---------------------------------------------------------------- research
CAT = f'{NS}:concilium'
C = CAT
def entry(name, col, row, icon, parents, notes, recipes, warp=0, meta=None, cond=None):
    st = [{'text': f'research.{NS}.{name}.stage_0',
           'required_knowledge': [{'type': 'theory', 'category': CAT, 'amount': 1}]},
          {'text': f'research.{NS}.{name}.stage_1', 'recipes': recipes}]
    if warp: st[0]['warp'] = warp
    obj = ({'neoforge:conditions': cond} if cond else {}) | {
           'category': CAT, 'name': f'research.{NS}.{name}.title', 'parents': parents, 'column': col, 'row': row,
           'icons': [icon], 'stages': st,
           'note_aspects': [{'aspect': asp(a), 'amount': note(n)} for a, n in notes], 'complexity': 2}
    if meta: obj['meta'] = meta
    w(f'data/{NS}/thaumaturge/research_entry/{name}.json', obj)

# Original grid: livingwood 1,2  dreamwood 2,2  terrasteel 3,2  tainted 3,3  infernal 2,4  warpwood 3,4 (row, col).
# Those cells are taken here, so the family sits to the right of the phase 3 gear.
def B(k): return f'{NS}:{k}_bracelet'
entry('casting_bracelets', 5, 1, B('iron'), [C, f'~{T}:cap_thaumium'], [('instrumentum', 10), ('praecantatio', 10)],
      [f'{NS}:arcane_workbench/iron_bracelet', f'{NS}:arcane_workbench/thaumium_bracelet'], meta=['round'])
entry('void_bracelet', 5, 3, B('void'), [f'{NS}:casting_bracelets', f'~{T}:cap_void'],
      [('instrumentum', 10), ('alienis', 15), ('vacuos', 10)], [f'{NS}:infusion/void_bracelet'])
entry('livingwood_bracelet', 6, 0, B('livingwood'), [f'{NS}:casting_bracelets', f'~{FM}:rod_livingwood', f'~{FM}:cap_manasteel'],
      [('instrumentum', 10), ('herba', 15), ('victus', 10)], [f'{NS}:infusion/livingwood_bracelet'], cond=BOTANIA)
entry('dreamwood_bracelet', 7, 0, B('dreamwood'), [f'{NS}:livingwood_bracelet', f'~{FM}:rod_dreamwood', f'~{FM}:cap_elementium'],
      [('instrumentum', 10), ('herba', 15), ('auram', 10)], [f'{NS}:infusion/dreamwood_bracelet'], cond=BOTANIA)
entry('terrasteel_bracelet', 8, 1, B('terrasteel'), [f'{NS}:casting_bracelets', f'~{FM}:cap_terrasteel'],
      [('instrumentum', 10), ('metallum', 15), ('auram', 10)], [f'{NS}:infusion/terrasteel_bracelet'], meta=['spiky'], cond=BOTANIA)
entry('infernal_bracelet', 6, 2, B('infernal'), [f'{NS}:casting_bracelets', f'~{FM}:rod_infernal'],
      [('instrumentum', 10), (INFERNUM, 15), (f'{FM}:superbia', 10)], [f'{NS}:infusion/infernal_bracelet'])
entry('tainted_bracelet', 7, 2, B('tainted'), [f'{NS}:casting_bracelets', f'~{FM}:rod_tainted'],
      [('instrumentum', 10), ('vitium', 15), ('praecantatio', 10)], [f'{NS}:infusion/tainted_bracelet'])
entry('eldritch_bracelet', 6, 4, B('eldritch'), [f'{NS}:void_bracelet'],
      [('instrumentum', 10), ('herba', 15), ('alienis', 10), ('tenebrae', 10), ('cognitio', 10)],
      [f'{NS}:infusion/eldritch_bracelet'], warp=3)

# ---------------------------------------------------------------- shield foci
# The shield_foci and guard_castable spell-part tags are written by gen_shield_foci_data.py.

# ---------------------------------------------------------------- curios, item models
KINDS = ['iron', 'thaumium', 'void', 'livingwood', 'dreamwood', 'terrasteel', 'infernal', 'tainted', 'eldritch']
BOTANICAL = {'livingwood', 'dreamwood', 'terrasteel'}
w('data/curios/tags/item/bracelet.json', {'replace': False, 'values': [
    {'id': B(k), 'required': False} if k in BOTANICAL else B(k) for k in KINDS]})
# The Aura Core attunement (Gadomancy mod) ignores these wands, as before the move (the old AuraCores excluded CastingBracelet).
w('data/gadomancy/tags/item/no_core_attunement.json', {'replace': False, 'values': [
    {'id': B(k), 'required': False} if k in BOTANICAL else B(k) for k in KINDS]})
# Give players the bracelet slot (Curios defines it; Neo Vitae also enables it, but we must not depend on that).
w(f'data/{NS}/curios/entities/bracelet.json', {'entities': ['player'], 'slots': ['bracelet']})
# In the hands a bracelet is drawn on the wrist (BraceletArmLayer, BraceletFirstPerson), so the item model vanishes there.
GONE = {'scale': [0, 0, 0]}
HANDS = {c: GONE for c in ('thirdperson_righthand', 'thirdperson_lefthand', 'firstperson_righthand', 'firstperson_lefthand')}
for k in KINDS:
    w(f'assets/{NS}/models/item/{k}_bracelet.json',
      {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{k}_bracelet'}, 'display': HANDS})


# ---------------------------------------------------------------- icons (original art)
def png(path, size, pixels):
    raw = b''.join(b'\x00' + b''.join(struct.pack('BBBB', *pixels.get((x, y), (0, 0, 0, 0))) for x in range(size))
                   for y in range(size))
    def chunk(t, d): return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0)) \
        + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'wb').write(data)

def hexc(h): return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)
def shade(c, f): return tuple(max(0, min(255, round(v * f))) for v in c[:3]) + (255,)
# band colour, rim (cap metal) colour, gem colour or None
PALETTE = {
    'iron': ('b8b8b8', 'e2e2e2', None),
    'thaumium': ('5c4a8c', 'e8c860', None),
    'void': ('2a1c3a', '7a5ab0', None),
    'livingwood': ('4a3020', '8ab8e8', '58d070'),
    'dreamwood': ('d8dcd0', 'f080c8', 'a0e8ff'),
    'terrasteel': ('38a838', 'b0f060', 'ffffff'),
    'infernal': ('7a2418', '5c4a8c', 'ff7020'),
    'tainted': ('5c2c6c', '5c4a8c', 'c050e0'),
    'eldritch': ('1c1c2c', '3a2a5a', '60f0c0'),
}
# A bracelet seen from above at an angle: an elliptical ring, lit from the top left.
CX, CY = 7.5, 8.0
for k, (band, rim, gem) in PALETTE.items():
    band, rim = hexc(band), hexc(rim)
    px = {}
    for y in range(16):
        for x in range(16):
            dx, dy = (x - CX) / 7.0, (y - CY) / 4.6
            r = math.hypot(dx, dy)
            if r > 1.0:
                continue
            inner = math.hypot((x - CX) / 4.4, (y - CY + 0.9) / 2.3)
            if inner < 1.0:
                continue   # the hole, with the far wall showing below
            lit = 1.15 - 0.35 * ((y - 3) / 10.0) - 0.1 * ((x - 1) / 14.0)
            if r > 0.86 or inner < 1.12:
                px[(x, y)] = shade(rim, lit)
            else:
                px[(x, y)] = shade(band, lit)
    # dark outline
    out = dict(px)
    for (x, y) in px:
        if any((nx, ny) not in px for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1))):
            out[(x, y)] = shade(px[(x, y)], 0.55)
    if gem:
        g = hexc(gem)
        for (x, y), f in (((7, 12), 1.0), ((8, 12), 0.85), ((7, 13), 0.7), ((8, 13), 0.6)):
            out[(x, y)] = shade(g, f)
        out[(7, 12)] = shade(g, 1.35)
    png(os.path.join(ROOT, f'assets/{NS}/textures/item/{k}_bracelet.png'), 16, out)

print('phase 3b generated')
