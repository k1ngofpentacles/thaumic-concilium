"""Generates phase-5a cycle 5b data: the Crimson Avanpost (cultist tower), after the original's CultistTowerWorldGen (WTFPL).
Replays the original's placement calls (x, y, z = its anchor) into a block list, writes the structure template, the structure
and structure set JSON, the chest loot table and the biome tag. Run after gen_phase5a_levitator.py and before gen_lang.py.

Block mapping (1.7.10 -> 1.21): magical log -> thaumaturge:log_greatwood (meta 0 axis y, 4 axis x, 8 axis z); cosmetic solid 12 ->
thaumaturge:stone_arcane_brick; stained clay 14 -> red_terracotta; greatwood stairs -> thaumaturge:stairs_greatwood (meta&3: 0 east,
1 west, 2 south, 3 north; meta&4 upside down); wooden device 8 + TileBanner -> thaumaturge:wall_banner_crimson_cult (rotation
0 south, 4 west, 8 north, 12 east); lifter -> thaumicconcilium:arcane_levitator; chest -> minecraft:chest with the loot table.
Template origin = original (x-2, y, z-7), size 11 x 12 x 11.
"""
import sys, os
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, '..', '..', '..', 'tools', 'structures'))
from genlib import *
import nbt

LOG, BRICK, CLAY = 'thaumaturge:log_greatwood', 'thaumaturge:stone_arcane_brick', 'minecraft:red_terracotta'
STAIRS = 'thaumaturge:stairs_greatwood'
OX, OZ = 2, 7            # local = original relative + (OX, 0, OZ)
SIZE = (11, 12, 11)
B = {}


def put(x, y, z, state):
    B[(x + OX, y, z + OZ)] = state


def column(x, y, z, n, state):
    for i in range(y, y + n):
        put(x, i, z, state)


def row_x(x, y, z, n, state):
    for i in range(x, x + n):
        put(i, y, z, state)


def row_z(x, y, z, n, state):
    for i in range(z - n, z):
        put(x, y, i, state)


def log(axis):
    return f'{LOG}[axis={axis}]'


FACE = {0: 'east', 1: 'west', 2: 'south', 3: 'north'}


def stair(x, y, z, meta):
    put(x, y, z, f'{STAIRS}[facing={FACE[meta & 3]},half={"top" if meta & 4 else "bottom"},shape=straight]')


LY, LX, LZ = log('y'), log('x'), log('z')
for dx, dz in [(0, 0), (1, 1), (3, 2), (5, 1), (6, 0), (7, -2), (6, -4), (5, -5), (3, -6), (1, -5), (0, -4), (-1, -2)]:
    column(dx, 0, dz, 4, LY)
for dx, dz in [(2, 1), (4, 1), (6, -1), (6, -3), (4, -5), (2, -5), (0, -1), (0, -3)]:
    column(dx, 0, dz, 3, BRICK)
for dx, dz in [(1, 0), (5, 0), (1, -4), (5, -4), (3, 1), (6, -2), (0, -2), (3, -5)]:
    column(dx, 4, dz, 6, LY)
for dx, dz in [(2, 0), (4, 0), (5, -1), (5, -3), (2, -4), (4, -4), (1, -1), (1, -3)]:
    column(dx, 5, dz, 5, CLAY)
row_x(2, 3, 1, 3, LX); row_x(2, 3, -5, 3, LX)
row_z(0, 3, 0, 3, LZ); row_z(6, 3, 0, 3, LZ)
row_x(2, 4, 0, 3, LX); row_x(2, 4, -4, 3, LX)
row_z(1, 4, 0, 3, LZ); row_z(5, 4, 0, 3, LZ)
for dx, dy, dz, m in [(3, 6, 1, 3), (3, 7, 1, 7), (0, 6, -2, 0), (0, 7, -2, 4), (3, 6, -5, 2), (3, 7, -5, 6), (6, 6, -2, 1), (6, 7, -2, 5),
                      (1, 4, 1, 3), (3, 4, 2, 3), (5, 4, 1, 3), (1, 4, -5, 2), (3, 4, -6, 2), (5, 4, -5, 2),
                      (0, 4, 0, 0), (-1, 4, -2, 0), (0, 4, -4, 0), (6, 4, 0, 1), (7, 4, -2, 1), (6, 4, -4, 1)]:
    stair(dx, dy, dz, m)
for dx, dz in [(1, 1), (0, 0), (3, 2), (5, 1), (6, 0), (7, -2), (6, -4), (5, -5), (3, -6), (1, -5), (0, -4), (-1, -2)]:
    column(dx, 10, dz, 2, LY)
for dx, dy, dz, m in [(1, 9, 1, 7), (3, 9, 2, 7), (5, 9, 1, 7), (1, 9, -5, 6), (3, 9, -6, 6), (5, 9, -5, 6),
                      (0, 9, 0, 4), (-1, 9, -2, 4), (0, 9, -4, 4), (6, 9, 0, 5), (7, 9, -2, 5), (6, 9, -4, 5)]:
    stair(dx, dy, dz, m)
row_z(0, 10, 0, 3, LZ); row_z(6, 10, 0, 3, LZ)
row_x(2, 10, 1, 3, LX); row_x(2, 10, -5, 3, LX)
# wall banners: (dx, dz, rotation) -> FACING (0 south, 4 west, 8 north, 12 east)
for dx, dz, facing in [(3, 3, 'south'), (3, -7, 'north'), (-2, -2, 'west'), (8, -2, 'east')]:
    put(dx, 8, dz, f'thaumaturge:wall_banner_crimson_cult[facing={facing}]')
for xx in (-1, 0, 1):
    for zz in (-1, 0, 1):
        put(3 + xx, 9, -2 + zz, CLAY)
del B[(3 + OX, 9, -2 + OZ)]          # the original sets this cell back to air
for dx, dz in [(5, -2), (1, -2), (3, -4), (3, 0)]:
    put(dx, 9, dz, CLAY)
put(3, 0, -2, 'thaumicconcilium:arcane_levitator')
put(5, 10, -2, 'minecraft:chest[facing=south,type=single,waterlogged=false]')
assert all(0 <= x < SIZE[0] and 0 <= y < SIZE[1] and 0 <= z < SIZE[2] for x, y, z in B), 'block outside the template box'

tpl = nbt.structure(SIZE, B)
chest_local = (5 + OX, 10, -2 + OZ)
for blk in tpl['blocks']:
    if tuple(int(c) for c in blk['pos']) == chest_local:
        blk['nbt'] = {'id': 'minecraft:chest', 'LootTable': f'{NS}:chests/cultist_tower'}
# the 3 fixed guards, persistent (finalised with their equipment at placement by the structure piece: see CrimsonAvanpostPiece)
GUARDS = [(3, 11, -3), (5, 11, -3), (3, 11, 0)]
ents = []
for dx, dy, dz in GUARDS:
    lx, lz = dx + OX, dz + OZ
    ents.append({'pos': nbt.List(6, [nbt.Double(lx + 0.5), nbt.Double(dy), nbt.Double(lz + 0.5)]),
                 'blockPos': nbt.List(3, [nbt.Int(lx), nbt.Int(dy), nbt.Int(lz)]),
                 'nbt': {'id': f'{NS}:crimson_archer', 'PersistenceRequired': nbt.Byte(1)}})
tpl['entities'] = nbt.List(10, ents)
out = os.path.join(ROOT, f'data/{NS}/structure/cultist_tower.nbt')
os.makedirs(os.path.dirname(out), exist_ok=True)
nbt.write(out, tpl)

# ---------------------------------------------------------------- worldgen data
w(f'data/{NS}/worldgen/structure/crimson_avanpost.json', {
    'type': f'{NS}:crimson_avanpost', 'biomes': '#minecraft:is_overworld', 'step': 'surface_structures',
    'terrain_adaptation': 'none', 'spawn_overrides': {}})
# One attempt per spacing^2 chunks (17 -> 1 in 289, the original's 1 in 300 attempts); the ground rule refuses some.
w(f'data/{NS}/worldgen/structure_set/crimson_avanposts.json', {
    'structures': [{'structure': f'{NS}:crimson_avanpost', 'weight': 1}],
    'placement': {'type': 'minecraft:random_spread', 'spacing': 17, 'separation': 8, 'salt': 1550370113}})

# Chest loot: ChestGenHooks min 2, max 8 -> 2-8 rolls; weights and counts as the original's CommonProxy.
def _e(item, wt, lo, hi=None):
    e = {'type': 'minecraft:item', 'name': item, 'weight': wt}
    if hi:
        e['functions'] = [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]
    return e


w(f'data/{NS}/loot_table/chests/cultist_tower.json', {'type': 'minecraft:chest', 'pools': [{
    'rolls': {'type': 'minecraft:uniform', 'min': 2, 'max': 8}, 'bonus_rolls': 0.0,
    'entries': [_e('thaumaturge:nugget_void', 10, 1, 3), _e('thaumaturge:curio_rites', 6, 1), _e('minecraft:bow', 8, 1),
                _e('minecraft:arrow', 15, 1, 16), _e('thaumaturge:triple_meat_treat', 10, 1, 3), _e('thaumaturge:curio_knowledge', 12, 1, 3)]}]})
print('cultist tower: %d blocks' % len(B))
