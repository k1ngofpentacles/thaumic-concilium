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
w(f'assets/{NS}/models/block/verdant_capital.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/verdant_capital'}})
w(f'assets/{NS}/models/item/verdant_capital.json', {'parent': f'{NS}:block/verdant_capital'})
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
# catalyst livingwood; rune of spring, rune of mana, terrasteel, a silverwood sapling; herba 48, praecantatio 32, ordo 16; instability 4
# (TC4-scale 10 / 2.5 = 4 through genlib's instab).
infusion('verdant_capital', 'the_green_translation', [('herba', 48), ('praecantatio', 32), ('ordo', 16)],
         it('botania:livingwood'),
         [it('botania:rune_spring'), it('botania:rune_mana'), it('botania:terrasteel_ingot'), it('thaumaturge:sapling_silverwood')],
         10, 'verdant_capital')
_p = f'data/{NS}/recipe/infusion/verdant_capital.json'
_o = r(_p)
w(_p, {'neoforge:conditions': BOTANIA} | _o)

# ---------------------------------------------------------------- research 5: The Green Translation
# Hidden until The Vessel (entry 3) is known; stage 0 also asks for a terrasteel ingot and a rune (crafted, or held), then theory.
entry('the_green_translation', -10, 2, f'{NS}:verdant_capital', [f'{NS}:the_vessel'],
      [('herba', 12), ('praecantatio', 10), ('ordo', 8)], recipes=[f'{NS}:infusion/verdant_capital'], warp=1,
      meta=['hidden', 'spiky'], complexity=3)
_p = f'data/{NS}/thaumaturge/research_entry/the_green_translation.json'
_o = r(_p)
_o['stages'][0]['craft'] = [
    {'items': ['botania:terrasteel_ingot'], 'amount': 1},
    {'items': ['botania:rune_spring', 'botania:rune_mana'], 'amount': 1},
]
w(_p, {'neoforge:conditions': BOTANIA} | _o)
