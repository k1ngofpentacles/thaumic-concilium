"""Generates phase-1 datapack JSON (spell parts, aspects, research). Run from the repo root."""
import json, os
NS = 'thaumicconcilium'
ROOT = 'src/main/resources'
def w(path, obj):
    p = os.path.join(ROOT, path); os.makedirs(os.path.dirname(p), exist_ok=True)
    json.dump(obj, open(p, 'w'), indent=2); open(p, 'a').write('\n')
def label(part, key, vals): return [f'spell.{NS}.value.{part}.{key}.{v}' for v in vals]
def setting(part, key, lo, hi, default=None, complexity=0.0, vis=0.0, labels=None):
    s = {'key': key, 'min': lo, 'max': hi, 'name': f'spell.{NS}.setting.{key}'}
    if default is not None: s['default'] = default
    if complexity: s['complexity'] = complexity
    if vis: s['vis'] = vis
    if labels: s['labels'] = label(part, key, labels)
    return s

# ---------------------------------------------------------------- spell parts
SIGNAL_COLORS = ['black','red','green','brown','blue','purple','cyan','light_gray','gray','pink','lime','yellow','light_blue','magenta','orange','white','chaos']
parts = {
  'compressed_blast': dict(aspect='thaumaturge:potentia', color=0xFFF2C0, complexity=4,
      research='signal_focus', sound='thaumicconcilium:thump',
      settings=[setting('compressed_blast','fuse',0,3,0,complexity=1.0),
                setting('compressed_blast','draw',0,1,0,complexity=6.0,vis=1.0,labels=['none','vacuum'])]),
  'impulse': dict(aspect='thaumaturge:perditio', color=0xFF7A1C, complexity=8, vis=4.0,
      research='impulse_focus',
      settings=[setting('impulse','potency',0,3,0,complexity=3.0,vis=1.0),
                setting('impulse','mode',0,1,0,complexity=2.0,labels=['blast','pneumatic'])]),
  'signal': dict(aspect='thaumaturge:ignis', color=0xFFD34F, complexity=2,
      research='signal_focus',
      settings=[setting('signal','color',0,16,1,labels=SIGNAL_COLORS),
                setting('signal','shape',0,5,0,complexity=1.0,labels=['small_ball','large_ball','star','creeper','burst','random']),
                setting('signal','flashbang',0,1,0,complexity=3.0,labels=['off','on'])]),
  'positive_burst': dict(aspect='thaumaturge:victus', color=0x22EE44, complexity=6, vis=2.0,
      research='positive_burst_focus',
      settings=[setting('positive_burst','potency',0,3,0,complexity=2.0,vis=0.5),
                setting('positive_burst','mode',0,2,0,complexity=3.0,labels=['heal','fulfillment','vitaminize'])]),
  'flurry': dict(aspect='thaumaturge:gelum', color=0xBFEFFF, complexity=3,
      research='cooling_focus',
      settings=[setting('flurry','shot',0,1,0,complexity=3.0,labels=['snowball','frost_shard'])]),
}
for name, p in parts.items():
    obj = {'aspect': p['aspect'], 'behavior': {'type': f'{NS}:{name}'}, 'color': p['color'],
           'complexity': p['complexity'], 'icon': f'{NS}:textures/foci/{name}.png',
           'research': {'entry': f"{NS}:{p['research']}"}}
    if p.get('vis'): obj['vis'] = p['vis']
    if p.get('sound'): obj['sound'] = p['sound']
    if p.get('settings'): obj['settings'] = p['settings']
    w(f'data/{NS}/thaumaturge/spell_part/{name}.json', obj)

# Sin aspects (ira, gula, invidia, superbia, desidia, luxuria) and the infernum fallback come from Forbidden Magic
# (Thaumaturge), a required dependency, under the forbiddenmagic namespace.
FM = 'forbiddenmagic'

# ---------------------------------------------------------------- research
w(f'data/{NS}/thaumaturge/research_category/concilium.json', {
  'background': f'{NS}:textures/researchback.png',
  'overlay_background': 'thaumaturge:textures/gui/gui_research_back_over.png',
  'icon': f'{NS}:textures/category.png', 'index': 20,
  'required_research': 'thaumaturge:base_auromancy',
  'formula': [{'aspect': 'thaumaturge:humanus', 'amount': 20}, {'aspect': 'thaumaturge:cognitio', 'amount': 10},
              {'aspect': 'thaumaturge:praecantatio', 'amount': 10}, {'aspect': 'thaumaturge:desiderium', 'amount': 5},
              {'aspect': f'{FM}:ira', 'amount': 5}, {'aspect': 'thaumaturge:instrumentum', 'amount': 5}]})
CAT = f'{NS}:concilium'
def entry(name, col, row, icon, parents, notes, stages=2, extra_req=None, complexity=2, hidden_parents=(), recipes=()):
    st = [{'text': f'research.{NS}.{name}.stage_0',
           'required_knowledge': [{'type': 'theory', 'category': CAT, 'amount': 1}]}]
    if extra_req: st[0]['required_research'] = extra_req
    for i in range(1, stages): st.append({'text': f'research.{NS}.{name}.stage_{i}'})
    if recipes: st[-1]['recipes'] = list(recipes)
    w(f'data/{NS}/thaumaturge/research_entry/{name}.json', {
      'category': CAT, 'name': f'research.{NS}.{name}.title', 'parents': list(parents) + list(hidden_parents),
      'column': col, 'row': row, 'icons': [icon], 'stages': st,
      'note_aspects': [{'aspect': a, 'amount': n} for a, n in notes], 'complexity': complexity})
entry('concilium', 0, 0, f'{NS}:textures/category.png', ['thaumaturge:base_auromancy'],
      [('thaumaturge:humanus', 3), ('thaumaturge:cognitio', 2)], complexity=1)
entry('signal_focus', 0, 2, f'focus:{NS}:signal', [f'{NS}:concilium', 'thaumaturge:focus_projectile'],
      [('thaumaturge:instrumentum', 2), ('thaumaturge:potentia', 3), ('thaumaturge:sensus', 3)], complexity=1, recipes=[f'{NS}:infusion/signal_focus'])
entry('impulse_focus', 2, 4, f'focus:{NS}:impulse', [f'{NS}:signal_focus'],
      [('thaumaturge:potentia', 3), (f'{FM}:ira', 2), ('thaumaturge:ignis', 3)], complexity=2, recipes=[f'{NS}:infusion/impulse_focus'])
entry('positive_burst_focus', 0, 4, f'focus:{NS}:positive_burst', [f'{NS}:signal_focus', 'thaumaturge:focus_heal'],
      [('thaumaturge:victus', 3), ('thaumaturge:auram', 2), ('thaumaturge:humanus', 2)], complexity=3, recipes=[f'{NS}:infusion/positive_burst_focus'])
entry('cooling_focus', -3, 4, f'focus:{NS}:flurry', [f'{NS}:concilium', 'thaumaturge:focus_elemental'],
      [('thaumaturge:gelum', 3), ('thaumaturge:aer', 2), ('thaumaturge:instrumentum', 2)], complexity=2, recipes=[f'{NS}:infusion/cooling_focus'])
print('generated')

# ---------------------------------------------------------------- pre-inscribed foci (infusion)
# Original infusion recipes, translated: TC4 items -> Thaumaturge equivalents, TC4 instability /2.5 (Thaumaturge's
# scale tops out near 6), aspect names TC4 -> Thaumaturge (sano -> victus, telum -> aversio, wrath -> our ira).
def n(part, settings=None, children=()):
    d = {'part': f'{NS}:{part}'}
    if settings: d['settings'] = settings
    if children: d['children'] = list(children)
    return d
def spell(style, *children): return {'style': style, 'root': {'part': 'thaumaturge:origin', 'children': list(children)}}
PRESETS = {
  'signal_focus': spell('instant', n('compressed_blast', {'fuse': 1}, [n('signal', {'color': 1})])),
  'impulse_focus': spell('instant', n('compressed_blast', None, [n('impulse')])),
  'positive_burst_focus': spell('instant', n('positive_burst')),
  'cooling_focus': spell('channeled', n('flurry')),
}
def focus_result(key):
    return {'count': 1, 'id': 'thaumaturge:focus_1', 'components': {
        'thaumaturge:spell': PRESETS[key],
        'minecraft:custom_name': json.dumps({'translate': f'{NS}.focus.preset', 'with': [{'translate': f'research.{NS}.{key}.title'}]})}}
def preset_ingredient(key):
    return {'type': 'neoforge:components', 'items': 'thaumaturge:focus_1', 'components': {'thaumaturge:spell': PRESETS[key]}}
def crystal(a): return {'type': 'neoforge:components', 'items': 'thaumaturge:essentia_crystal',
                        'components': {'thaumaturge:crystal_aspect': {'aspect': f'thaumaturge:{a}'}}}
def infusion(key, aspects, catalyst, components, instability):
    w(f'data/{NS}/recipe/infusion/{key}.json', {
      'type': 'thaumaturge:infusion', 'aspects': [{'aspect': a, 'amount': v} for a, v in aspects],
      'catalyst': catalyst, 'components': components, 'instability': instability,
      'research': {'entry': f'{NS}:{key}', 'stage': 1}, 'result': focus_result(key)})
BLANK = {'item': 'thaumaturge:focus_1'}
DYES = ['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
infusion('signal_focus', [('thaumaturge:potentia', 32), ('thaumaturge:instrumentum', 16), ('thaumaturge:sensus', 64), ('thaumaturge:lux', 16)],
         BLANK, [{'tag': f'c:dyes/{d}'} for d in DYES], 2)
infusion('impulse_focus', [('thaumaturge:perditio', 128), ('thaumaturge:ignis', 128), (f'{FM}:ira', 96), ('thaumaturge:vinculum', 32)],
         preset_ingredient('signal_focus'),
         [crystal('ignis'), {'item': 'thaumaturge:obsidian_totem'}, {'item': 'thaumaturge:primal_charm'}, {'item': 'thaumaturge:obsidian_totem'},
          crystal('ignis'), {'item': 'thaumaturge:obsidian_totem'}, {'item': 'thaumaturge:primal_charm'}, {'item': 'thaumaturge:obsidian_totem'}], 6)
infusion('positive_burst_focus', [('thaumaturge:victus', 256), ('thaumaturge:humanus', 128), ('thaumaturge:instrumentum', 64),
                                  ('thaumaturge:auram', 64), (f'{FM}:desidia', 32)],
         {'item': 'thaumaturgetinker:focus_heal'},
         [{'item': 'thaumaturge:tube_oneway'}, {'item': 'minecraft:enchanted_golden_apple'}, {'item': 'thaumaturge:primal_charm'},
          {'item': 'thaumaturge:primal_charm'}, {'item': 'thaumaturge:primal_charm'}, {'item': 'minecraft:enchanted_golden_apple'},
          {'item': 'minecraft:experience_bottle'}, {'item': 'minecraft:enchanted_golden_apple'}, {'item': 'thaumaturge:primal_charm'},
          {'item': 'thaumaturge:primal_charm'}, {'item': 'thaumaturge:primal_charm'}, {'item': 'minecraft:enchanted_golden_apple'}], 6)
infusion('cooling_focus', [('thaumaturge:ignis', 16), ('thaumaturge:aer', 16), ('thaumaturge:instrumentum', 16)],
         BLANK, [{'item': 'minecraft:torch'}, {'item': 'minecraft:torch'}], 2)
print('recipes generated')
