"""Generates phase-8 data, cycle 1 (design: docs/design/phase8-golems-design.md; facts: phase8-golems-facts.md):
the Assistant seal, the research of the Bydlo chain (TCHORIZONS, BRAINLITTERING, BUFFGOLEM) and the Golem Bydlo's texture.
Original (Concilium, WTFPL): TCRecipeRegistry 1208-1233 (ASSISTANCECORE infusion), Thaumonomicon 824-845, 862-912, art
textures/items/assistance_core.png, textures/misc/brain_littering.png, textures/items/iconGolem.png.
TC4 -> Thaumaturge: Butcher core -> seal_butcher (centre), Alchemy core -> seal_provider (stand-in: the seal that supplies jars; no
host twin), focus of Containment -> the ready-inscribed Containment focus of Thaumic Horizons, syringeBlood -> blood_syringe,
tool -> instrumentum, beast -> bestia, trap -> vinculum. Instability 8 / 2.5 = 3.
The species recipes (Thaumaturge -> Mad Thaumaturge -> Bydlo) are in the chain block below (cycle 2).
Run from the module root after gen_phase7_data.py and before gen_lang.py.
"""
import sys, os, zlib
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genlib import *

TH = 'thaumichorizons'

# ---------------------------------------------------------------- the Assistant seal: recipe
# Containment focus as an ingredient: the ready-inscribed focus Horizons' focus_containment recipe makes (Beam range 12 -> Containment).
containment = {'type': 'neoforge:components', 'items': f'{T}:focus_1', 'components': {
    f'{T}:spell': {'style': 'channeled', 'root': {'part': f'{T}:origin', 'children': [
        {'part': f'{T}:beam', 'settings': {'range': 12}, 'children': [{'part': f'{TH}:containment'}]}]}}}}
infusion('seal_assistant', 'assistant_seal',
         [('instrumentum', 32), ('bestia', 16), ('vinculum', 32)],
         it('seal_butcher'), [containment, it(f'{TH}:blood_syringe'), it('seal_provider'), it(f'{TH}:blood_syringe')], 8, 'seal_assistant')

# ---------------------------------------------------------------- research
# ASSISTANCECORE (-7, 3) cx2, concealed, warp 1; parent TCGADOMANCY (= gadomancy:aura_core), hidden COREALCHEMY and COREBUTCHER (their seals),
# plus the Containment focus (the original did not require it; the recipe does).
entry('assistant_seal', -7, 3, f'{NS}:seal_assistant',
      ['gadomancy:aura_core', f'~{T}:seal_butcher', f'~{T}:seal_provide', f'~{TH}:focus_containment'],
      [('humanus', 8), ('vinculum', 8), ('bestia', 8)],
      [f'{NS}:infusion/seal_assistant'], warp=1, meta=['hidden'])
add_item_aspects('seal_assistant', [('instrumentum', 10), ('bestia', 5), ('vinculum', 10)])

# ---------------------------------------------------------------- the species chain (cycle 2): Thaumaturge -> Mad Thaumaturge -> Golem Bydlo
# Original: HorizonsIntegration 86-123 (the real recipes: instability 4, raw as the H3 recipes) and TCRecipeRegistry 1235-1287 (the
# display recipes, which showed 10). TC4 -> Thaumaturge: Knowledge Fragment -> Horizons' fragment; Forbidden Magic Taint Shard ->
# forbiddenmagic:taint_shard; Zombie Brain -> brain; Taint Shard -> a vitium essentia crystal (decision J8); Thaumium Golem placer ->
# the Golem item with a thaumium golem build; Animation Powder -> Horizons' golem powder; Thaumium Block, Tallow Block, Golem Fetter
# -> their Thaumaturge blocks. Aspects: entropy perditio, eldritch alienis, mind cognitio, energy potentia, metal metallum,
# pride superbia. The vat refuses golems, merchants, Npcs: both Thaumaturge entities are Monsters, so they pass.
FM = 'forbiddenmagic'
thaumium_golem = {'type': 'neoforge:components', 'items': f'{T}:golem', 'components': {f'{T}:golem_properties': {
    'material': f'{T}:thaumium', 'head': f'{T}:basic', 'arms': f'{T}:basic', 'legs': f'{T}:walker', 'addon': f'{T}:none'}}}
CHAIN = {
    'brain_littering': ('brain_littering', f'{NS}:thaumaturge', f'{NS}:mad_thaumaturge', f'{NS}:mad_thaumaturge_spawn_egg', 4,
                        [('perditio', 64), ('alienis', 64), ('cognitio', 64)],
                        [it(f'{TH}:knowledge_fragment'), it(f'{FM}:taint_shard'), it('brain'), crystal('vitium')]),
    'buff_golem': ('buff_golem', f'{NS}:mad_thaumaturge', f'{NS}:golem_bydlo', f'{NS}:golem_bydlo_spawn_egg', 4,
                   [('potentia', 64), ('metallum', 128), (f'{FM}:superbia', 32), ('perditio', 128)],
                   [thaumium_golem] * 3 + [it(f'{TH}:golem_powder')] * 2 + [it('metal_thaumium')] * 4 + [it('tallow_block')] * 2
                   + [it('golem_fetter')]),
}
for name, (research, inp, out, egg, inst, asp_pairs, comps) in CHAIN.items():
    w(f'data/{NS}/recipe/creature_infusion/{name}.json', {
        'type': f'{TH}:creature_infusion', 'species': {'input': inp, 'output': out}, 'components': comps,
        'aspects': aspects(asp_pairs), 'instability': inst, 'requires': gate(research, 1)})
    w(f'data/{NS}/recipe/infusion/display_{name}.json', {
        'type': f'{T}:infusion', 'catalyst': it(f'{TH}:vat_dummy'), 'components': comps, 'aspects': aspects(asp_pairs),
        'instability': instab(inst), 'research': gate(research, 1), 'result': {'count': 1, 'id': egg}})

# TCHORIZONS (7, -1) cx1 round: the Concilium tab's door to Thaumic Horizons; parent THAUMATURGES (our thaumaturges), hidden healing vat.
entry('thaumic_horizons', 7, -1, f'{TH}:textures/misc/vat.png', [f'{NS}:thaumaturges', f'~{TH}:healing_vat'],
      [('praecantatio', 8), ('humanus', 8)], stages=1, complexity=1, meta=['round'])
# BRAINLITTERING (8, 2) cx2, concealed, warp 5; parent TCHORIZONS, hidden infusion vat. Recipe page: cycle 2.
entry('brain_littering', 8, 2, f'{NS}:textures/research/brain_littering.png', [f'{NS}:thaumic_horizons', f'~{TH}:infusion_vat'],
      [('cognitio', 8), ('perditio', 8), ('alienis', 8), ('humanus', 8)], [f'{NS}:infusion/display_brain_littering'], warp=5, meta=['hidden'])
# BUFFGOLEM (7, 5) cx1, concealed, warp 5; parent BRAINLITTERING, hidden golem powder, thaumium golem and the golem fetter.
entry('buff_golem', 7, 5, f'{NS}:textures/research/golem_bydlo.png',
      [f'{NS}:brain_littering', f'~{TH}:golem_powder', f'~{T}:golem_direct', f'~{T}:golem_fetter'],
      [('humanus', 8), ('potentia', 8), ('metallum', 8), ('alienis', 8)], [f'{NS}:infusion/display_buff_golem'], warp=5, complexity=1, meta=['hidden'],
      addenda=[{'text': f'research.{NS}.buff_golem.addendum_0', 'required_research': [f'{T}:focus_break']},
               {'text': f'research.{NS}.buff_golem.addendum_1', 'required_research': [f'{NS}:wrath_attraction_focus']}])

copy_from_jar({'textures/items/assistance_core.png': 'item/seal_assistant.png',
               'textures/items/assistance_core.png.mcmeta': 'item/seal_assistant.png.mcmeta',
               'textures/misc/brain_littering.png': 'research/brain_littering.png',
               'textures/items/iconGolem.png': 'research/golem_bydlo.png'})
gen('seal_assistant')
spawn_egg_model('golem_bydlo')

# ---------------------------------------------------------------- the Golem Bydlo's texture (128x64, GolemBydloModel's box layout)
# Original: Thaumcraft 4's golem_thaumium.png (all rights reserved): not used. This is drawn here: riveted grey plates, a tarnished
# brass head with two lamp eyes and a grille, a black seam between plates, brass fists. u,v per box = GolemBydloModel.create().
W, H = 128, 64
px = [(0, 0, 0, 0)] * (W * H)


def put(x, y, c):
    if 0 <= x < W and 0 <= y < H:
        px[y * W + x] = c + (255,) if len(c) == 3 else c


def hsh(x, y, s):
    n = (x * 73856093) ^ (y * 19349663) ^ (s * 83492791)
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFF) / 255.0


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def face(x0, y0, wid, hei, base, seed, rivets=True, plates=True):
    for j in range(hei):
        for i in range(wid):
            k = 0.88 + 0.22 * hsh(i, j, seed)
            if hsh(i // 2, j // 2, seed + 7) > 0.8:
                k *= 0.82          # tarnish patches
            c = shade(base, k)
            edge = i in (0, wid - 1) or j in (0, hei - 1)
            if edge:
                c = shade(base, 0.55)           # seam between plates
            elif plates and j == hei // 2 and wid > 4:
                c = shade(base, 0.7)            # a weld line across the plate
            if rivets and wid >= 4 and hei >= 4 and (i, j) in ((1, 1), (wid - 2, 1), (1, hei - 2), (wid - 2, hei - 2)):
                c = shade(base, 1.45)           # rivet highlight
            put(x0 + i, y0 + j, c)


def box(u, v, w, h, d, base, seed, **kw):
    face(u + d, v, w, d, shade(base, 1.1), seed + 1, **kw)             # top
    face(u + d + w, v, w, d, shade(base, 0.7), seed + 2, **kw)         # bottom
    face(u, v + d, d, h, shade(base, 0.85), seed + 3, **kw)            # right
    face(u + d, v + d, w, h, base, seed + 4, **kw)                     # front
    face(u + d + w, v + d, d, h, shade(base, 0.85), seed + 5, **kw)    # left
    face(u + 2 * d + w, v + d, w, h, shade(base, 0.9), seed + 6, **kw) # back


IRON = (96, 104, 112)
BRASS = (150, 118, 62)
DARK = (58, 62, 70)
box(0, 0, 8, 9, 8, BRASS, 10)                   # head
box(32, 0, 6, 3, 1, DARK, 15, rivets=False, plates=False)   # jaw grille
box(48, 0, 7, 3, 2, IRON, 20, rivets=False, plates=False)   # toe caps
box(72, 0, 10, 7, 2, BRASS, 35)                 # chest plate
box(100, 0, 10, 9, 3, DARK, 36)                 # boiler hump
box(0, 17, 14, 15, 10, IRON, 30)                # torso
box(48, 17, 12, 5, 8, DARK, 32)                 # pelvis
box(88, 17, 6, 5, 8, BRASS, 38)                 # shoulder plate
box(0, 42, 5, 9, 6, DARK, 40)                   # upper arm
box(22, 42, 6, 12, 8, IRON, 50)                 # gauntlet
box(50, 42, 7, 14, 7, IRON, 60)                 # leg
# the face: head front is (8, 8) 8 wide x 9 tall: a dark visor slot with two lamp eyes
for gx in range(9, 15):
    for gy in (10, 11):
        put(gx, gy, shade(DARK, 0.6))
for ex in (9, 13):
    for ey in (10, 11):
        put(ex, ey, (255, 214, 96))
        put(ex + 1, ey, (255, 170, 48))
put(9, 10, (255, 244, 190)); put(13, 10, (255, 244, 190))
# jaw grille front (33, 1) 6 x 3: brass bars
for gx in range(33, 39, 2):
    for gy in range(1, 4):
        put(gx, gy, shade(BRASS, 0.8))
# chest plate front (74, 2) 10 x 7: a furnace slit glowing behind bars
for gx in range(76, 82):
    for gy in (4, 5, 6):
        put(gx, gy, (255, 150, 40) if gx % 2 == 0 else shade(DARK, 0.5))
    if gx % 2 == 0:
        put(gx, 4, (255, 214, 96))
# a brass belt round the torso (side faces start at v 27, 15 tall; the strip is 48 wide), brass knees and gauntlet cuffs
BAND = (170, 134, 72)
for i in range(48):
    put(i, 27 + 12, BAND); put(i, 27 + 13, shade(BAND, 0.75))
for i in range(28):
    put(50 + i, 49 + 6, BAND); put(50 + i, 49 + 7, shade(BAND, 0.75))      # knee band
    put(22 + i, 50 + 1, BAND); put(22 + i, 50 + 2, shade(BAND, 0.75))      # gauntlet cuff
    put(22 + i, 50 + 8, shade(IRON, 0.6))                                   # knuckle line
write_png(f'{ROOT}/assets/{NS}/textures/entity/golem_bydlo.png', W, H, px)
