"""Phase 5b trunk: the Crimson Dagger, Crimson Blood, Crimson Plating and the Primordial Mote (items, art, tags, recipes, aspects).
Run after gen_phase5a_data.py and gen_phase5b_data.py (read-modify-writes crimson_initiation and the aspect map), before gen_lang.py.

Every PROVISIONAL value below is a branch decision (docs/agents/retro/5b-trunk-notes.md lists them); change it here and re-run.
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genlib import *

# ================================================================ PROVISIONAL (branch decisions): the dagger's recipe
# Ritual reading (docs/design/5b-design.md): Crimson Rites (the contract, spent in the signing) at the centre; the ring is A's
# (brain, tallow x2, phial, bone) plus Thaumaturge's void sword; essentia humanus, victus, ordo and C's vinculum.
DAGGER_CENTRE = 'curio_rites'          # thaumaturge:curio_rites, "Crimson Rites"
DAGGER_RING = ['brain', 'tallow', 'tallow', 'phial', 'minecraft:bone', 'void_sword']
DAGGER_ESSENTIA = [('humanus', 48), ('ordo', 24), ('vinculum', 24)]   # D1 option C, The Offertory: 96, no victus (the binder's own)
DAGGER_INSTABILITY = 6                  # option C keeps native instability at a moderate 6

# ================================================================ D1-C The Offertory: the binding (damage type, offerings)
# world/BindingSessions.java: the chain drinks from the binder (damage type `binding`) unless fed one of these on a spare pedestal.
# D1-c2 (user decision 2026-11-22): strictly human-related flesh. Our Crimson Blood, Thaumaturge's zombie brain (also a ring item: a ring
# brain the matrix has not pulled is never eaten), rotten flesh, and its compact derivative the flesh block (nine rotten flesh).
# Left out: raw beef, pork, spider eye (animal), tallow (rendered fat), Neo Vitae tainted flesh (demon meat), Alex's Mobs blood sac
# (a mosquito's), Enchanted demonic blood, Madman's Dreams distorted meat (dream creatures), Thaumaturge Tinkers' mob aspect items.
OFFERINGS = [f'{NS}:crimson_blood', 'thaumaturge:brain', 'minecraft:rotten_flesh', 'thaumaturge:flesh_block']
w(f'data/{NS}/damage_type/binding.json', {'message_id': f'{NS}.binding', 'scaling': 'never', 'exhaustion': 0.0})
_p = 'data/minecraft/tags/damage_type/bypasses_armor.json'
_o = r(_p)
if f'{NS}:binding' not in _o['values']:
    _o['values'].append(f'{NS}:binding')
w(_p, _o)
w(f'data/{NS}/tags/item/binding_offerings.json', {'replace': False, 'values': OFFERINGS})

# ================================================================ PROVISIONAL: the Crimson Plating recipe
# The user's sketch (Tainted Magic's idea, ours in detail): infusion, iron block at the centre, Crimson Blood, gold nuggets, 9 out.
# D5 option A (The Gilded Cult Plate): iron block, 2 blood, 4 gold nuggets; metallum 64, victus 32, praemunio 32; TC4 5 -> 2
PLATING_CENTRE = 'minecraft:iron_block'
PLATING_RING = [f'{NS}:crimson_blood'] * 2 + ['minecraft:gold_nugget'] * 4
PLATING_ESSENTIA = [('metallum', 64), ('victus', 32), ('praemunio', 32)]
PLATING_INSTABILITY = 2                 # final value
PLATING_COUNT = 9

# ================================================================ entity tiers (user decision): data, so branches edit the tags
STUN = [f'{T}:cultist_knight', f'{T}:cultist_cleric', f'{NS}:crimson_archer']                                 # full stun
SLOW = [f'{NS}:crimson_paladin', f'{T}:cultist_ranger', f'{T}:cultist_ranger_captain', f'{NS}:crimson_ranger']  # Slowness
BOSSES = [f'{NS}:crimson_pontifex', f'{T}:cultist_leader']                                                     # nothing
w(f'data/{NS}/tags/entity_type/dagger_stun.json', {'replace': False, 'values': STUN})
w(f'data/{NS}/tags/entity_type/dagger_slow.json', {'replace': False, 'values': SLOW})
w(f'data/{NS}/tags/entity_type/crimson_cultists.json',
  {'replace': False, 'values': [f'#{NS}:dagger_stun', f'#{NS}:dagger_slow'] + BOSSES})

# ================================================================ recipes
infusion('crimson_dagger', 'crimson_initiation', DAGGER_ESSENTIA, it(DAGGER_CENTRE), [it(i) for i in DAGGER_RING], 15, 'crimson_dagger',
         stage=None)
_p = f'data/{NS}/recipe/infusion/crimson_dagger.json'
_o = r(_p); _o['instability'] = DAGGER_INSTABILITY; w(_p, _o)
infusion('crimson_plating', 'crimson_initiation', PLATING_ESSENTIA, it(PLATING_CENTRE),
         [it(i) for i in PLATING_RING], 8, 'crimson_plating',
         stage=None, result_obj={'count': PLATING_COUNT, 'id': f'{NS}:crimson_plating'})
_p = f'data/{NS}/recipe/infusion/crimson_plating.json'
_o = r(_p); _o['instability'] = PLATING_INSTABILITY; w(_p, _o)

# The research page lists both recipes (the entry has one stage: crimson_initiation, phase 5a).
_p = f'data/{NS}/thaumaturge/research_entry/crimson_initiation.json'
_o = r(_p); _o['stages'][-1]['recipes'] = [f'{NS}:infusion/crimson_dagger', f'{NS}:infusion/crimson_plating']; w(_p, _o)

# ================================================================ aspects (scanning)
add_item_aspects('crimson_dagger', [('aversio', 10), ('metallum', 6), ('victus', 5), ('humanus', 5)])   # D6 option A
add_item_aspects('crimson_blood', [('victus', 12), ('humanus', 6), ('alienis', 4)])   # D6 option A
add_item_aspects('crimson_plating', [('metallum', 6), ('praemunio', 4), ('victus', 2), ('alienis', 2)])   # D6 option A
add_item_aspects('primordial_mote', [('praecantatio', 10), ('alienis', 5), ('vacuos', 5)])

# ================================================================ models and art (original, ours)
# D8 (dagger animation): the item property `thaumicconcilium:grip` (client/DaggerClient) picks the grip model while the
# dagger is raised, stabbing or cutting: 0 forward (the plain handheld model), 0.25 cut, 0.5 inline (blade continues the
# forearm line), 0.75 plunge, 1 reverse (blade out of the bottom of the fist, icepick). Shared by the three D8 options.
#
# Third person, the hand frame (ItemInHandLayer): item +y is forward when the arm hangs, +z runs up the arm, the origin is
# the front bottom edge of the arm; the blade lies on the texture's diagonal, so with rotation [0, -90, rz] it points along
# (0, sin(45 + rz), cos(45 + rz)) and the plate stays edge-on to the front. Vanilla's handheld is rz 55 (forward, 10
# degrees down the arm). The middle of the grip is 6.6 px behind the item's centre (scale 0.85); the translation puts it
# in the fist, at (0, -2, 2).
#   reverse: the blade out of the little-finger side, tilted 25 degrees up the arm, so that with the arm raised it hangs
#            point down and forward (rz = 25 - 135).
#   plunge:  the same grip with the wrist turned 90 degrees down the arm (rz = -65 - 135): with the arm fallen to 35 degrees
#            below level the point goes down and forward into the target, not back at the holder's own legs.
#   cut:     the blade continues the forearm (rz 135), the grip inside the fist; first person keeps the handheld display.
# First person is the view frame; reverse and plunge share one display there, so the wrist turn does not pop in the hand.
# item/handheld has its own left-hand entries, so each grip model needs them too or a left-handed holder falls back to
# the forward grip (found by D8-b). The game negates y and z of a left-hand entry and mirrors its translation, so the entry
# that mirrors a right-hand [x, y, z] is [x, y - 180, -z] (the plate turned over): vanilla's handheld is [0, -90, 55] and
# [0, 90, -55]. For y = -90 that equals "negate y and z"; for heart's first person (y = 0) only the general form is right.
#   heart (0.875, option C, Heartguard): the true reverse grip, the blade straight out of the little-finger side
#            (rz = -135: backwards when the arm hangs, down and in with the fist at the chest). First person: the blade
#            along -x with the grip at the model's origin, so DaggerPoseC places the fist and turns the blade about it.
_dag_tex = {'layer0': f'{NS}:item/crimson_dagger'}
w(f'assets/{NS}/models/item/crimson_dagger.json', {'parent': 'minecraft:item/handheld', 'textures': _dag_tex, 'overrides': [
    {'predicate': {f'{NS}:grip': 0.25}, 'model': f'{NS}:item/crimson_dagger_cut'},
    {'predicate': {f'{NS}:grip': 0.5}, 'model': f'{NS}:item/crimson_dagger_inline'},
    {'predicate': {f'{NS}:grip': 0.75}, 'model': f'{NS}:item/crimson_dagger_plunge'},
    {'predicate': {f'{NS}:grip': 0.875}, 'model': f'{NS}:item/crimson_dagger_heart'},
    {'predicate': {f'{NS}:grip': 1.0}, 'model': f'{NS}:item/crimson_dagger_reverse'}]})
_FP_HANDHELD = ([0, -90, 25], [1.13, 3.2, 1.13], 0.68)
_FP_REVERSE = ([0, -90, 200], [1.13, -3.1, -1.4], 0.68)
for _grip, _tp, _fp in (
        ('cut', ([0, -90, 135], [0, -1, -5.6], 0.85), _FP_HANDHELD),
        ('inline', ([0, -90, 135], [0, -2, -7], 0.85), ([0, -90, 135], [1.13, 0, -3.4], 0.68)),
        ('plunge', ([0, -90, 160], [0, -4.79, -3.98], 0.85), _FP_REVERSE),
        ('heart', ([0, -90, -135], [0, -8.1, 1.5], 0.85), ([0, 0, 135], [-4.9, 0, 0], 0.68)),
        ('reverse', ([0, -90, -110], [0, -7.98, 4.79], 0.85), _FP_REVERSE)):
    _disp = {}
    for _k, _v in (('thirdperson', _tp), ('firstperson', _fp)):
        _disp[_k + '_righthand'] = {'rotation': _v[0], 'translation': _v[1], 'scale': [_v[2]] * 3}
        _disp[_k + '_lefthand'] = {'rotation': [_v[0][0], (_v[0][1] + 360) % 360 - 180, -_v[0][2]], 'translation': _v[1], 'scale': [_v[2]] * 3}
    w(f'assets/{NS}/models/item/crimson_dagger_{_grip}.json', {'parent': 'minecraft:item/handheld', 'textures': _dag_tex, 'display': _disp})
for _n in ('crimson_blood', 'primordial_mote'):   # crimson_plating's model is written by gen_art_redraw.py (host plate, tinted)
    gen(_n)


def _canvas(n):
    return [(0, 0, 0, 0)] * (n * n)

def _put(px, n, x, y, c):
    if 0 <= x < n and 0 <= y < n:
        px[y * n + x] = c if len(c) == 4 else c + (255,)

def _save(path, n, px):
    write_png(os.path.join(ROOT, path), n, n, px)

# Crimson Dagger: a short blade on the diagonal, a crossguard, a wrapped grip.
_px = _canvas(16)
for _i in range(5, 14):                       # blade, tip at the top right
    _put(_px, 16, _i, 15 - _i, (200, 30, 40)); _put(_px, 16, _i + 1, 15 - _i, (120, 10, 25)); _put(_px, 16, _i, 14 - _i, (235, 90, 90))
_put(_px, 16, 14, 1, (250, 200, 200))
for _d in (-2, -1, 0, 1, 2):                  # crossguard across the diagonal
    _put(_px, 16, 4 + _d, 11 - _d, (200, 160, 50)); _put(_px, 16, 5 + _d, 11 - _d, (140, 110, 30))
for _i in (1, 2, 3):                          # grip
    _put(_px, 16, _i, 15 - _i, (70, 35, 30)); _put(_px, 16, _i + 1, 15 - _i, (45, 20, 20))
_put(_px, 16, 0, 15, (200, 160, 50))
_save(f'assets/{NS}/textures/item/crimson_dagger.png', 16, _px)

# Crimson Blood and Primordial Mote icons: drawn with the art kit in gen_art_redraw.py (art pass 2026-11-27).
# Crimson Plating is the host iron plate, referenced and tinted (gen_art_redraw.py, TCClient.itemColors).

# Mob effect icons (18 x 18): Bleed is a red drop, Stunned a violet ring with a bar.
_px = _canvas(18)
for _y in range(18):
    for _x in range(18):
        _dx, _dy = _x - 8.5, _y - 10.0
        if _dx * _dx + _dy * _dy <= 20 or (_y < 10 and _y >= 2 and abs(_dx) <= (_y - 2) * 0.55):
            _put(_px, 18, _x, _y, (170, 15, 30))
_save(f'assets/{NS}/textures/mob_effect/bleed.png', 18, _px)
_px = _canvas(18)
for _y in range(18):
    for _x in range(18):
        _r = ((_x - 8.5) ** 2 + (_y - 8.5) ** 2) ** 0.5
        if 5.0 <= _r <= 7.5 or (7 <= _x <= 10 and 4 <= _y <= 13):
            _put(_px, 18, _x, _y, (150, 90, 210))
_save(f'assets/{NS}/textures/mob_effect/stunned.png', 18, _px)

# Peeler halo shell (D4-b): own 16 x 16 pale white-blue noise, drawn additively (ONE, ONE) by client/TCRenderTypes.PEELER_HALO.
# Mean about (15, 12, 5) a layer, so three layers over a (168, 200, 255) sky add up to the original's white-blue rim.
_seed = 12345
_px = []
for _i in range(256):
    _seed = (_seed * 1103515245 + 12345) & 0x7fffffff
    _k = 0.55 + 0.9 * ((_seed >> 8) & 255) / 255.0
    _px.append((min(255, int(15 * _k + 0.5)), min(255, int(12 * _k + 0.5)), min(255, int(5 * _k + 0.5)), 255))
_save(f'assets/{NS}/textures/misc/peeler_halo.png', 16, _px)
