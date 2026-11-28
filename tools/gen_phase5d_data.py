"""Phase 5d cycle 1: the Verdant Capital (placeholder block, item, loot, aspects), its infusion and research entry 5, The Green Translation.
Run after gen_phase5c_research.py, before gen_lang.py. Everything Botania-conditioned carries neoforge:mod_loaded botania.

The look is cycle 4: a plain cube in livingwood-like greens (own generated texture).
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from genlib import *

BOTANIA = [{'type': 'neoforge:mod_loaded', 'modid': 'botania'}]

# ---------------------------------------------------------------- the block (placeholder look)
w(f'assets/{NS}/blockstates/verdant_capital.json', {'variants': {'': {'model': f'{NS}:block/verdant_capital'}}})
# the block and item models are cycle 4b's (gen_phase5d_look.py)
drop_self_loot('verdant_capital')


class Lcg:
    def __init__(self, seed): self.s = seed
    def next(self, n):
        self.s = (self.s * 1103515245 + 12345) & 0x7fffffff
        return (self.s >> 8) % n


_rng = Lcg(5401)
_px = []
for _y in range(16):
    for _x in range(16):
        _v = _rng.next(26)
        _edge = _x in (0, 15) or _y in (0, 15)
        _ring = (abs(_x - 7.5) + abs(_y - 7.5)) % 5 < 1.2  # faint growth rings
        if _edge:
            _px.append((70 + _v // 2, 110 + _v, 60 + _v // 2, 255))
        elif _ring:
            _px.append((52 + _v // 2, 128 + _v, 58 + _v // 2, 255))
        else:
            _px.append((86 + _v, 146 + _v, 74 + _v // 2, 255))
write_png(f'{A}/textures/block/verdant_capital.png', 16, 16, _px)
add_item_aspects('verdant_capital', [('herba', 20), ('praecantatio', 10), ('ordo', 5)])

# ---------------------------------------------------------------- the infusion (user's decision, 2026-11-22)
# Revised by the user 2026-11-22 (replaces livingwood / terrasteel): a captured node (Thaumaturge's jarred node) at the centre;
# rune of spring, rune of mana, silverwood and greatwood saplings, a gold ingot, quicksilver, a mana diamond; instability 3
# (TC4-scale 7.5 / 2.5). Aspects as the user gave them. Tempestas -> thaumadditions:ventus 28 (user, 2026-11-27, replacing their
# earlier potentia 28); without Thaumic Additions a second recipe asks potentia 42 (add-on fallback rule: x1.5, rounded up).
#   verdant_capital.json                  ventus 28,   botania + mod_loaded thaumadditions
#   verdant_capital_no_thaumadditions.json potentia 42, botania + not mod_loaded thaumadditions
# Research lists both ids (the one that did not load is skipped, as with the wisp essence variants).
TA_LOADED = {'type': 'neoforge:mod_loaded', 'modid': 'thaumadditions'}
_VC_ASPECTS = lambda tempestas: [('victus', 28), ('permutatio', 28), ('lux', 40), ('tenebrae', 40), tempestas, ('motus', 28),
                                 ('herba', 40), ('praecantatio', 33)]
_VC_ARGS = (it('thaumaturge:jar_node'),
            [it('botania:rune_spring'), it('botania:rune_mana'), it('thaumaturge:sapling_silverwood'), it('thaumaturge:sapling_greatwood'),
             it('minecraft:gold_ingot'), it('thaumaturge:quicksilver'), it('botania:mana_diamond')],
            7.5, 'verdant_capital')
for _key, _tempestas, _cond in (('verdant_capital_no_thaumadditions', ('potentia', 42), {'type': 'neoforge:not', 'value': TA_LOADED}),
                                ('verdant_capital', ('thaumadditions:ventus', 28), TA_LOADED)):
    infusion(_key, 'the_green_translation', _VC_ASPECTS(_tempestas), *_VC_ARGS)
    _p = f'data/{NS}/recipe/infusion/{_key}.json'
    _o = r(_p)
    w(_p, {'neoforge:conditions': BOTANIA + [_cond]} | _o)

# ---------------------------------------------------------------- research 5: The Green Translation
# Hidden until The Vessel (entry 3) is known; stage 0 also asks for a jarred node and a rune (crafted, or held), then theory
# (the user replaced terrasteel with the jarred node, 2026-11-22).
entry('the_green_translation', -10, 2, f'{NS}:verdant_capital', [f'{NS}:the_vessel'],
      [('herba', 12), ('praecantatio', 10), ('ordo', 8)], recipes=[f'{NS}:infusion/verdant_capital', f'{NS}:infusion/verdant_capital_no_thaumadditions'], warp=1,
      meta=['hidden', 'spiky'], complexity=3)
_p = f'data/{NS}/thaumaturge/research_entry/the_green_translation.json'
_o = r(_p)
_o['stages'][0]['craft'] = [
    {'items': ['thaumaturge:jar_node'], 'amount': 1},
    {'items': ['botania:rune_spring', 'botania:rune_mana'], 'amount': 1},
]
w(_p, {'neoforge:conditions': BOTANIA} | _o)

# ---------------------------------------------------------------- research 7: The Glade (cycle 4a)
# A field note granted by code to every participant of a tier 3 or 4 ending (VesselEndings.grantGlade), Botania only, like entry 5.
entry('the_glade', -10, 3, f'{NS}:verdant_capital', [f'{NS}:the_green_translation'],
      [('herba', 8), ('victus', 8), ('auram', 8)], warp=0, meta=['hidden', 'round'], complexity=1, stages=2, autounlock=True)
_p = f'data/{NS}/thaumaturge/research_entry/the_glade.json'
_o = r(_p)
_o['meta'] = [m for m in _o.get('meta', []) if m != 'autounlock']
w(_p, {'neoforge:conditions': BOTANIA} | _o)

# ---------------------------------------------------------------- X2: one entry in Botania's lexicon (Patchouli book botania:lexicon, use_resource_pack)
# Our jar ships it under assets/botania/ (category botania:misc, where the other mod-compat entries sit). The Verdant Capital only exists
# with Botania, so no flag is needed. Text goes in assets/botania/lang/en_us.json (the book is i18n: true); resource packs merge lang files.
_LEX = f'assets/botania/patchouli_books/lexicon/en_us/entries/misc/{NS}_verdant_capital.json'
w(_LEX, {
    'name': f'{NS}.lexicon.verdant_capital.name',
    'category': 'botania:misc',
    'icon': f'{NS}:verdant_capital',
    'sortnum': 90,
    'pages': [
        {'type': 'text', 'text': f'{NS}.lexicon.verdant_capital.0'},
        {'type': 'text', 'text': f'{NS}.lexicon.verdant_capital.1'},
    ],
})
w('assets/botania/lang/en_us.json', {
    f'{NS}.lexicon.verdant_capital.name': 'The Verdant Capital',
    f'{NS}.lexicon.verdant_capital.0': 'Thaumic Concilium adds a block that speaks $(thing)mana$() to something that is not a pool.$(p)'
        'The $(item)Verdant Capital$() is infused about a captured node in its jar, with a rune of spring, a rune of mana, silverwood and '
        'greatwood saplings, gold, quicksilver and a mana diamond, and set on top of an anchor shard of a quiet Vessel. It takes mana from bursts and sparks as a pool does, '
        'and spends it on the rite in place of a hand.',
    f'{NS}.lexicon.verdant_capital.1': 'Its research, $(thing)The Green Translation$(), is in the Thaumic Concilium tab of the Thaumaturge '
        'research book, and is only revealed to one who has met the Vessel.$(p)What it leaves behind is a grove, with a still pond '
        'of mana at its centre that a spreader may draw from. The pond is not bottomless.$(p)Each capital in place also raises the chance, '
        'a quarter at a time, that roots rise and strangle a Taint Seed: the ones the Vessel\'s blight brings during the fight, and the ones '
        'left in the grove at the end. With four, none escape.',
})
