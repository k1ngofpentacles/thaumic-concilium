"""Generates phase 10-4 data: the Burning Site's two structure templates and its test pool (only with Just Another Witchery Remake).
Original (Concilium, WTFPL): common/world/village/BurningSite.java (the whole piece, 90 lines), BurningSiteHandler.java (weight 10, limit 1).
Facts: docs/design/phase10-original-facts.md section 10. The village pool injection and the piece's code are Java (compat/witchery/BurningSites,
world/BurningSiteElement); this file writes only what is data.

Layout (template 9 x 6 x 9, x and z 0..8; the original's local y 0 is template y 1):
  y0 dirt (original y -1)            y1 gravel floor (original y 0; desert: sandstone) with the pyre's placeholders at x 3..5, z 3..5
  y2 cobblestone wall ring on the four edges (original y 1, closed) and the stake at (4, y2..y5, 4) (original y 1..4)
  every other cell of y2..y5 is explicit air (the original clears its box).
The pyre's placeholders are re-rolled by BurningSiteElement.place (rand.nextInt(3): coal block, hay bale, vertical oak log; desert: sandstone for
the log), so the placeholders are only what a plain /place template shows. One jigsaw in the ring at (0, y2, 4) facing west, joined like a vanilla
rigid house with its entrance one above the floor (plains_fletcher_house_1: floor y0, jigsaw y1): the gravel ends level with the street's path.

Needs tools/structures/nbt.py (read as a library). Run from the module root after gen_phase10_data.py (python -I tools/gen_phase10_site.py).
"""
import sys, os
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, '..', '..', '..', 'tools', 'structures'))
from genlib import *
import nbt

# R12 C, then the user's second decision (2026-11-27): the stockade is a palisade block of its own; the site's stake is the separate
# thaumicconcilium:burning_stake, four high, parts foot / shackle / shaft / top stored here (partFor in BurningStakeBlock).
# Both blocks exist only with the remake: unknown to the game otherwise, so the stake reads as air.
STAKE_PARTS = {2: 'foot', 3: 'shackle', 4: 'shaft', 5: 'top'}
def stake(y):
    return f'thaumicconcilium:burning_stake[part={STAKE_PARTS[y]}]'
WITCHERY = [{'type': 'neoforge:mod_loaded', 'modid': 'witchery'}]
SIZE = (9, 6, 9)


def wall(x, z):
    """A cobblestone wall of the ring, connected to its ring neighbours (template blocks are placed as stored, no neighbour update)."""
    def ring(a, b):
        return 0 <= a <= 8 and 0 <= b <= 8 and (a in (0, 8) or b in (0, 8))
    n, s, e, wst = ring(x, z - 1), ring(x, z + 1), ring(x + 1, z), ring(x - 1, z)
    straight = (n and s and not e and not wst) or (e and wst and not n and not s)
    low = lambda c: 'low' if c else 'none'
    return (f'minecraft:cobblestone_wall[east={low(e)},north={low(n)},south={low(s)},up={"false" if straight else "true"},'
            f'waterlogged=false,west={low(wst)}]')


def build(desert):
    floor = 'minecraft:sandstone' if desert else 'minecraft:gravel'
    log = 'minecraft:sandstone' if desert else 'minecraft:oak_log[axis=y]'
    blocks = {}
    for x in range(9):
        for z in range(9):
            blocks[(x, 0, z)] = 'minecraft:dirt'
            blocks[(x, 1, z)] = floor
            for y in range(2, 6):
                blocks[(x, y, z)] = 'minecraft:air'
    for x in range(3, 6):
        for z in range(3, 6):
            blocks[(x, 1, z)] = ('minecraft:coal_block', 'minecraft:hay_block[axis=y]', log)[(x + z) % 3]
    for y in range(2, 6):
        blocks[(4, y, 4)] = stake(y)
    for i in range(9):
        for x, z in ((i, 0), (i, 8), (0, i), (8, i)):
            blocks[(x, 2, z)] = wall(x, z)
    blocks[(0, 2, 4)] = 'minecraft:jigsaw[orientation=west_up]'
    st = nbt.structure(SIZE, blocks)
    for b in st['blocks']:
        if tuple(int(c) for c in b['pos']) == (0, 2, 4):
            b['nbt'] = {'id': 'minecraft:jigsaw', 'name': 'minecraft:building_entrance', 'target': 'minecraft:building_entrance',
                        'pool': 'minecraft:empty', 'joint': 'aligned',
                        'final_state': 'minecraft:cobblestone_wall'}  # re-set with its true connections by BurningSiteElement.place
    return st


for name, desert in (('burning_site', False), ('burning_site_desert', True)):
    p = os.path.join(ROOT, f'data/{NS}/structure/village/{name}.nbt')
    os.makedirs(os.path.dirname(p), exist_ok=True)
    nbt.write(p, build(desert))

# The test pool: /place jigsaw thaumicconcilium:village/burning_site, and what the pack test builds from.
w(f'data/{NS}/worldgen/template_pool/village/burning_site.json', {
    'neoforge:conditions': WITCHERY,
    'fallback': 'minecraft:empty',
    'elements': [{'weight': 1, 'element': {
        'element_type': f'{NS}:burning_site', 'location': f'{NS}:village/burning_site',
        'processors': 'minecraft:empty', 'projection': 'rigid'}}]})
# ---------------------------------------------------------------- the two blocks' data. Every blockstate, model and texture of `stockade` and
# `burning_stake` belongs to the Fable look agent: nothing here writes them.
def block_data(name, aspect_pairs):
    full = f'{NS}:{name}'
    w(f'data/{NS}/loot_table/blocks/{name}.json', {
        'neoforge:conditions': WITCHERY, 'type': 'minecraft:block', 'random_sequence': f'{NS}:blocks/{name}',
        'pools': [{'rolls': 1.0, 'bonus_rolls': 0.0, 'conditions': [{'condition': 'minecraft:survives_explosion'}],
                   'entries': [{'type': 'minecraft:item', 'name': full}]}]})
    # shared tag and data map: read-modify-write (our entries optional / conditioned, harmless without the remake)
    axe = r('data/minecraft/tags/block/mineable/axe.json')
    axe['values'] = [v for v in axe['values'] if not (isinstance(v, dict) and v.get('id') == full) and v != full] + [{'id': full, 'required': False}]
    w('data/minecraft/tags/block/mineable/axe.json', axe)
    p = f'data/{T}/data_maps/item/base_aspects.json'
    m = r(p)
    m['values'][full] = {'neoforge:conditions': WITCHERY, 'value': aspects(aspect_pairs)}
    w(p, m)

# wooden things are herba in the host; spikes a little aversio; the stake's shackle chain is vinculum, its pyre ignis
block_data('stockade', [('herba', 5), ('aversio', 2)])
block_data('burning_stake', [('herba', 5), ('ignis', 2), ('vinculum', 2)])
print('phase 10-4: burning site templates, test pool and the blocks\' data written')
