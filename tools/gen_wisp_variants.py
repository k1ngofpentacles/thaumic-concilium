"""Wisp essence variants (Concilium 0.15.2, user decision 2026-11-23). The original used Thaumcraft 4 wisp essence in these infusions;
Thaumaturge has none, so every other generator writes an essentia crystal of the aspect (the stand-in). With Thaumic Horizons loaded, its
wisp essence (item thaumichorizons:wisp_essence, component thaumichorizons:wisp_aspect) replaces the crystals:
  <key>.json             wisp essence, condition mod_loaded thaumichorizons
  <key>_no_horizons.json the crystal stand-in, condition not mod_loaded thaumichorizons
Research pages list both ids (the one that did not load is skipped, as with FM's Balanced Shard variants).
Runs after every recipe generator (see gen_all.py); idempotent because the generators rewrite <key>.json first. Run from the module root."""
import json
import os

NS = 'thaumicconcilium'
ROOT = 'src/main/resources'
HORIZONS = 'thaumichorizons'
# Original recipes that took wisp essence (TC4 TCRecipeRegistry): Taint Animation (taint x4), Ring of Bluestering Light (pride x4),
# Thaumdrum (life, aura, magic), Ethereal Manipulator (aura, air, motion, flight). Wrath Attraction's essence catalyst is not ported.
KEYS = ['taint_animation_focus', 'ring_of_blustering_light', 'thaumaturge_drum', 'ethereal_manipulator_focus']
LOADED = {'type': 'neoforge:mod_loaded', 'modid': HORIZONS}


def load(path):
    with open(os.path.join(ROOT, path), encoding='utf-8') as f:
        return json.load(f)


def save(path, obj):
    with open(os.path.join(ROOT, path), 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def is_crystal(ing):
    return isinstance(ing, dict) and ing.get('items') == 'thaumaturge:essentia_crystal' and 'thaumaturge:crystal_aspect' in ing.get('components', {})


def to_wisp(ing):
    return {'type': 'neoforge:components', 'items': f'{HORIZONS}:wisp_essence',
            'components': {f'{HORIZONS}:wisp_aspect': {'aspect': ing['components']['thaumaturge:crystal_aspect']['aspect']}}}


for key in KEYS:
    path = f'data/{NS}/recipe/infusion/{key}.json'
    base = load(path)
    if any(c.get('items') == f'{HORIZONS}:wisp_essence' for c in base.get('components', []) if isinstance(c, dict)):
        raise SystemExit(f'{path} was already converted: run the recipe generators first (gen_all.py)')
    n = sum(1 for c in base['components'] if is_crystal(c)) + (1 if is_crystal(base['catalyst']) else 0)
    if n == 0:
        raise SystemExit(f'{key}: no essentia crystal ingredient to turn into wisp essence')
    fallback = dict(base)
    fallback['neoforge:conditions'] = [{'type': 'neoforge:not', 'value': LOADED}]
    wisp = dict(base)
    wisp['components'] = [to_wisp(c) if is_crystal(c) else c for c in base['components']]
    if is_crystal(base['catalyst']):
        wisp['catalyst'] = to_wisp(base['catalyst'])
    wisp['neoforge:conditions'] = [LOADED]
    save(path, wisp)
    save(f'data/{NS}/recipe/infusion/{key}_no_horizons.json', fallback)

    recipe_id = f'{NS}:infusion/{key}'
    rdir = f'data/{NS}/thaumaturge/research_entry'
    for fn in sorted(os.listdir(os.path.join(ROOT, rdir))):
        if not fn.endswith('.json'):
            continue
        entry = load(f'{rdir}/{fn}')
        changed = False
        for stage in entry.get('stages', []):
            recipes = stage.get('recipes')
            if recipes and recipe_id in recipes and recipe_id + '_no_horizons' not in recipes:
                recipes.insert(recipes.index(recipe_id) + 1, recipe_id + '_no_horizons')
                changed = True
        if changed:
            save(f'{rdir}/{fn}', entry)
print('wisp essence variants done:', ', '.join(KEYS))
