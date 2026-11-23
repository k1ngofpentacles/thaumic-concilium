#!/usr/bin/env python3
"""The grove's own Dynamic Trees families, leaves and species (phase 5d trees A). Run from the module root (python -I tools/gen_grove_species.py).

Dynamic Trees loads a mod's tree pack from the `trees/` folder at the root of the mod's jar (Resources.addModTreePack: findResource("trees")), so
everything under src/main/resources/trees/thaumicconcilium/ is loaded exactly when Dynamic Trees is. The pack's blocks register as
dynamictrees:<name>_branch / _leaves (the family's and the leaves properties' registry path), their loot tables are looked up under our namespace
(trees/branches/<name>_branch, trees/leaves/<name>) and their client models are ours too, under assets/dynamictrees (as Thaumaturge ships its own).

Why families and leaves of our own (not Thaumaturge's): the families' max_signal_depth is 32 (too shallow for a 40-block tree), and a family's leaves
must belong to it (DynamicLeavesBlock.branchSupport needs branch family == the leaves' family), while Family.setCommonLeaves would re-own
Thaumaturge's own leaves properties and change its trees. So grove_<wood> leaves are a second block of the same primitive leaves, the same cell
kit and smother, and the same texture. Wood, bark, rings, sticks and seeds are Thaumaturge's: the primitive logs, its texture overrides, its seed items.

Seeds: every species names Thaumaturge's own seed (dynamictrees:<wood>_seed), which also stops Dynamic Trees generating a seed for it; generate_sapling gives each species its own sapling block (dynamictrees:<species>_sapling). A
seed a player picks up therefore plants Thaumaturge's own species.
"""
import json
import os

ROOT = 'src/main/resources'
NS = 'thaumicconcilium'
WOODS = ['greatwood', 'silverwood']

# measured by Phase5dTreeTemplateTests (see docs/agents/retro/5d-trees-a-notes.md): signal energy per class and wood
ENERGY = {
    'elder': {'greatwood': 44.0, 'silverwood': 42.0},
    'grove': {'greatwood': 30.0, 'silverwood': 28.0},
    'glade': {'greatwood': 23.0, 'silverwood': 21.0},
}
# how many successful growth signals a tree's soil feeds per point of fertility (DT default 8: about 120 cells in a tree's life, too few for
# templates of 170-270 cells; Species.grow spends a point at 1 in soil_longevity successes)
LONGEVITY = {'elder': 64, 'grove': 40, 'glade': 32}  # measured: a glade greatwood spent its last point at 169 of 175 cells with 24
# Thaumaturge's own species numbers
THAUM = {
    'greatwood': dict(tapering=0.55, lowest_branch_height=6, growth_rate=0.7, climate_tolerance=0.5),
    'silverwood': dict(tapering=0.8, lowest_branch_height=5, growth_rate=0.65, climate_tolerance=0.45),
}


def w(path, obj):
    p = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def loot_branch(item, seq):
    def pool(fn, name):
        return {'bonus_rolls': 0.0, 'rolls': 1.0, 'entries': [{'type': 'minecraft:item', 'name': name,
                'functions': [{'function': fn}, {'function': 'minecraft:explosion_decay'}]}]}
    return {'neoforge:conditions': [{'type': 'neoforge:mod_loaded', 'modid': 'dynamictrees'}], 'type': 'minecraft:block',
            'pools': [pool('dynamictrees:multiply_logs_count', item), pool('dynamictrees:multiply_sticks_count', 'minecraft:stick')],
            'random_sequence': seq}


def loot_leaves(seq):
    return {'neoforge:conditions': [{'type': 'neoforge:mod_loaded', 'modid': 'dynamictrees'}], 'type': 'minecraft:block', 'pools': [
        {'bonus_rolls': 0.0, 'rolls': 1.0, 'entries': [{'type': 'dynamictrees:seed_item', 'conditions': [
            {'condition': 'minecraft:survives_explosion'},
            {'condition': 'minecraft:table_bonus', 'enchantment': 'minecraft:fortune', 'chances': [0.015625, 0.03125, 0.046875, 0.0625]},
            {'condition': 'dynamictrees:seasonal_seed_drop_chance'}]}]},
        {'bonus_rolls': 0.0, 'rolls': 1.0, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:stick', 'conditions': [
            {'condition': 'minecraft:table_bonus', 'enchantment': 'minecraft:fortune', 'chances': [0.02, 0.022222223, 0.025, 0.033333335, 0.1]}],
            'functions': [{'function': 'minecraft:set_count', 'add': False, 'count': {'type': 'minecraft:uniform', 'min': 1.0, 'max': 2.0}},
                          {'function': 'minecraft:explosion_decay'}]}]}],
            'random_sequence': seq}


def main():
    tags = {'block/branches_that_burn': [], 'block/stripped_branches_that_burn': [], 'block/leaves': [], 'item/branches_that_burn': [], 'block/saplings': []}
    for wood in WOODS:
        fam = f'grove_{wood}'
        # family: Thaumaturge's own (logs, textures, thickness cap), a deeper signal, our leaves
        w(f'trees/{NS}/families/{fam}.json', {
            'common_leaves': f'{NS}:{fam}',
            'common_species': f'{NS}:glade_{wood}',
            'primitive_log': f'thaumaturge:log_{wood}',
            'primitive_stripped_log': f'thaumaturge:stripped_log_{wood}',
            'max_branch_radius': 24,
            'max_signal_depth': 64,
            'texture_overrides': {
                'branch': f'thaumaturge:block/log_{wood}',
                'branch_top': f'thaumaturge:block/log_{wood}_top',
                'stripped_branch': f'thaumaturge:block/stripped_log_{wood}',
                'stripped_branch_top': f'thaumaturge:block/stripped_log_{wood}_top'}})
        w(f'trees/{NS}/leaves_properties/{fam}.json', {'primitive_leaves': f'thaumaturge:leaves_{wood}', 'cell_kit': 'dynamictrees:dark_oak',
          # DT's smother 3 refuses a leaf (and so the wood that must follow it) under 3 stacked tree blocks: a dense oak's inner limbs never grew
          'smother': 6,
          # DT's default 13 keeps a dense crown's inner leaves (and so its inner wood) from ever growing: the oak's low limbs sat at sky light 9-12
          'light_requirement': 8})
        for cls in ('elder', 'grove', 'glade'):
            t = THAUM[wood]
            w(f'trees/{NS}/species/{cls}_{wood}.json', {
                'family': f'{NS}:{fam}',
                'tapering': t['tapering'],
                'signal_energy': ENERGY[cls][wood],
                'max_branch_radius': 24,
                'lowest_branch_height': t['lowest_branch_height'],
                'growth_rate': t['growth_rate'],
                'leaves_properties': f'{NS}:{fam}',
                'world_gen_leaf_map_height': 48,
                'preferred_climate': 'temperate',
                'climate_tolerance': t['climate_tolerance'],
                'growth_logic_kit': 'thaumicconcilium:template',
                'seed': f'dynamictrees:{wood}_seed',
                'generate_sapling': True,
                'soil_longevity': LONGEVITY[cls]})
            # its sapling (DT names it dynamictrees:<species>_sapling): Thaumaturge's textures by id, DT's sapling model
            w(f'assets/dynamictrees/blockstates/{cls}_{wood}_sapling.json', {'variants': {'': {'model': f'dynamictrees:block/saplings/{cls}_{wood}'}}})
            w(f'assets/dynamictrees/models/block/saplings/{cls}_{wood}.json', {'parent': 'dynamictrees:block/smartmodel/sapling', 'render_type': 'minecraft:cutout_mipped',
              'textures': {'leaves': f'thaumaturge:block/leaves_{wood}', 'log': f'thaumaturge:block/log_{wood}'}})
            tags['block/saplings'].append(f'dynamictrees:{cls}_{wood}_sapling')
        # loot (data), looked up under our namespace
        w(f'data/{NS}/loot_table/trees/branches/{fam}_branch.json', loot_branch(f'thaumaturge:log_{wood}', f'{NS}:trees/branches/{fam}_branch'))
        w(f'data/{NS}/loot_table/trees/branches/stripped_{fam}_branch.json',
          loot_branch(f'thaumaturge:stripped_log_{wood}', f'{NS}:trees/branches/stripped_{fam}_branch'))
        w(f'data/{NS}/loot_table/trees/leaves/{fam}.json', loot_leaves(f'{NS}:trees/leaves/{fam}'))
        # client models (Thaumaturge's own, for our block ids)
        w(f'assets/dynamictrees/blockstates/{fam}_branch.json', {'variants': {'': {'model': f'dynamictrees:block/{fam}_branch'}}})
        w(f'assets/dynamictrees/blockstates/stripped_{fam}_branch.json', {'variants': {'': {'model': f'dynamictrees:block/stripped_{fam}_branch'}}})
        w(f'assets/dynamictrees/blockstates/{fam}_leaves.json', {'variants': {'': {'model': f'thaumaturge:block/leaves_{wood}'}}})
        w(f'assets/dynamictrees/models/block/{fam}_branch.json', {'loader': 'dynamictrees:thick_branch', 'family': f'{NS}:{fam}',
          'textures': {'bark': f'thaumaturge:block/log_{wood}', 'rings': f'thaumaturge:block/log_{wood}_top'}})
        w(f'assets/dynamictrees/models/block/stripped_{fam}_branch.json', {'loader': 'dynamictrees:thick_branch', 'family': f'{NS}:{fam}',
          'textures': {'bark': f'thaumaturge:block/stripped_log_{wood}', 'rings': f'thaumaturge:block/stripped_log_{wood}_top'}})
        w(f'assets/dynamictrees/models/item/{fam}_branch.json', {'parent': 'dynamictrees:item/branch',
          'textures': {'bark': f'thaumaturge:block/log_{wood}', 'rings': f'thaumaturge:block/log_{wood}_top'}})
        tags['block/branches_that_burn'].append(f'dynamictrees:{fam}_branch')
        tags['block/stripped_branches_that_burn'].append(f'dynamictrees:stripped_{fam}_branch')
        tags['block/leaves'].append(f'dynamictrees:{fam}_leaves')
        tags['item/branches_that_burn'].append(f'dynamictrees:{fam}_branch')
    for name, ids in tags.items():
        w(f'data/dynamictrees/tags/{name}.json', {'values': [{'id': i, 'required': False} for i in ids]})
    print('grove species written')


if __name__ == '__main__':
    main()
