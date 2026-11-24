"""Generates phase-2 datapack JSON: the remaining foci (minus Vis Conductor, deferred to phase 6), the Ring of
Blustering Light and Primordial Life. Run from the repo root, after gen_phase1_data.py."""
import json, os
NS = 'thaumicconcilium'
ROOT = 'src/main/resources'
T = 'thaumaturge'
FM = 'forbiddenmagic'
def w(path, obj):
    p = os.path.join(ROOT, path); os.makedirs(os.path.dirname(p), exist_ok=True)
    json.dump(obj, open(p, 'w'), indent=2); open(p, 'a').write('\n')
def label(part, key, vals): return [f'spell.{NS}.value.{part}.{key}.{v}' for v in vals]
def setting(part, key, lo, hi, default=None, complexity=0.0, vis=0.0, labels=None, step=None):
    s = {'key': key, 'min': lo, 'max': hi, 'name': f'spell.{NS}.setting.{key}'}
    if default is not None: s['default'] = default
    if step: s['step'] = step
    if complexity: s['complexity'] = complexity
    if vis: s['vis'] = vis
    if labels:
        assert len(labels) == hi - lo + 1, (part, key)
        s['labels'] = label(part, key, labels)
    return s

# ---------------------------------------------------------------- spell parts
# vis: the originals were priced per tick in TC4 centivis; these are per Thaumaturge pulse (about 5 ticks).
parts = {
  'reflection': dict(aspect=f'{T}:cognitio', color=0x7E4BA8, complexity=6, vis=0.06, research='reflection_focus', max_per_spell=1,
      # The original cost no vis (only experience); Thaumaturge refuses free casts, so the minimum.
      settings=[setting('reflection','potency',0,3,0,complexity=2.0,vis=0.0),
                setting('reflection','mode',0,1,0,complexity=4.0,labels=['experience','oblivion'])]),
  'scribing': dict(aspect=f'{T}:praemunio', color=0x8AA8FF, complexity=4, vis=2.0, research='scribing_focus',
      settings=[setting('scribing','potency',0,3,0,complexity=2.0,vis=0.5)]),
  'wrath_attraction': dict(aspect=f'{FM}:ira', color=0xFF4400, complexity=6, vis=6.0, research='wrath_attraction_focus',
      sound=f'{NS}:ira', cooldown_multiplier=2.5,
      settings=[setting('wrath_attraction','reach',0,3,0,complexity=1.5),
                setting('wrath_attraction','burnout',0,1,0,complexity=4.0,vis=2.0,labels=['off','on'])]),
  'taint_animation': dict(aspect=f'{T}:vitium', color=0x9C4CD6, complexity=7, vis=2.5, research='taint_animation_focus',
      settings=[setting('taint_animation','mode',0,2,0,labels=['original','animate','command']),
                setting('taint_animation','reach',0,3,0,complexity=1.0),
                setting('taint_animation','potency',0,3,0,complexity=2.0,vis=0.5),
                setting('taint_animation','vaporization',0,1,0,complexity=3.0,vis=0.5,labels=['off','on']),
                setting('taint_animation','manifestation',0,1,0,complexity=3.0,vis=0.3,labels=['off','on'])]),
  'ethereal_manipulator': dict(aspect=f'{T}:motus', color=0xFF00FF, complexity=3, vis=0.4, research='ethereal_manipulator_focus', max_per_spell=1,
      settings=[setting('ethereal_manipulator','distance',1,16,5)]),
  'spotlight': dict(aspect=f'{T}:lux', color=0xFFFF00, complexity=8, vis=6.0, research='spotlight_focus',
      settings=[setting('spotlight','potency',0,3,0,complexity=3.0,vis=1.0),
                setting('spotlight','mode',0,2,0,complexity=3.0,labels=['searing','warming','purification'])]),
  'alchemist_spree': dict(aspect=f'{T}:permutatio', color=0x66CCAA, complexity=6, vis=3.0, research='alchemist_spree_focus', max_per_spell=1,
      settings=[setting('alchemist_spree','potency',0,3,0,complexity=2.0,vis=0.5),
                setting('alchemist_spree','primal',0,1,0,complexity=4.0,vis=1.0,labels=['off','on'])]),
  'warp_extraction': dict(aspect=f'{T}:alienis', color=0x5D016B, complexity=7, vis=1.6, research='warp_extraction_focus', max_per_spell=1,
      settings=[setting('warp_extraction','potency',0,3,0,complexity=3.0,vis=0.5),
                setting('warp_extraction','reach',0,3,0,complexity=1.0),
                setting('warp_extraction','mode',0,2,0,complexity=4.0,labels=['none','mass_hysteria','self_flagellation'])]),
}
for name, p in parts.items():
    obj = {'aspect': p['aspect'], 'behavior': {'type': f'{NS}:{name}'}, 'color': p['color'],
           'complexity': p['complexity'], 'icon': f'{NS}:textures/foci/{name}.png',
           'research': {'entry': f"{NS}:{p['research']}"}}
    for k in ('vis', 'sound', 'max_per_spell', 'cooldown_multiplier'):
        if p.get(k): obj[k] = p[k]
    if p.get('settings'): obj['settings'] = p['settings']
    w(f'data/{NS}/thaumaturge/spell_part/{name}.json', obj)

# ---------------------------------------------------------------- research
CAT = f'{NS}:concilium'
def entry(name, col, row, icon, parents, notes, complexity=2, recipes=(), warp=0, stages=2, obtain=None, meta=None):
    st = [{'text': f'research.{NS}.{name}.stage_0',
           'required_knowledge': [{'type': 'theory', 'category': CAT, 'amount': 1}]}]
    if warp: st[0]['warp'] = warp
    if obtain: st[0]['obtain'] = obtain
    for i in range(1, stages): st.append({'text': f'research.{NS}.{name}.stage_{i}'})
    if recipes: st[-1]['recipes'] = list(recipes)
    obj = {'category': CAT, 'name': f'research.{NS}.{name}.title', 'parents': list(parents),
           'column': col, 'row': row, 'icons': [icon], 'stages': st,
           'note_aspects': [{'aspect': a, 'amount': n} for a, n in notes], 'complexity': complexity}
    if meta: obj['meta'] = meta
    w(f'data/{NS}/thaumaturge/research_entry/{name}.json', obj)
C = f'{NS}:concilium'
def rec(key): return [f'{NS}:infusion/{key}']
entry('scribing_focus', 3, 2, f'focus:{NS}:scribing', [C, f'{T}:runic_shielding', f'{T}:focus_ward', '~thaumaturgetinker:bright_nitor'],
      [(f'{T}:praemunio', 3), (f'{T}:potentia', 2), (f'{T}:praecantatio', 3), (f'{T}:humanus', 2)], 3, rec('scribing_focus'))
entry('ethereal_manipulator_focus', -3, 2, f'focus:{NS}:ethereal_manipulator', [C, f'{T}:focus_exchange', '~thaumaturgetinker:focus_telekinesis'],
      [(f'{T}:instrumentum', 2), (f'{T}:auram', 2), (f'{T}:cognitio', 2), (f'{T}:motus', 2)], 2, rec('ethereal_manipulator_focus'))
entry('wrath_attraction_focus', 2, 6, f'focus:{NS}:wrath_attraction', [C, f'{T}:focus_curse'],
      [(f'{FM}:ira', 3), (f'{T}:bestia', 2), (f'{T}:sensus', 2), (f'{T}:desiderium', 3)], 3, rec('wrath_attraction_focus'), warp=3)
entry('taint_animation_focus', 4, 6, f'focus:{NS}:taint_animation', [C, f'{T}:flux', f'{T}:focus_hellbat'],
      [(f'{T}:vitium', 3), (f'{T}:victus', 3), ('forbiddenmagic:corpus', 3), (f'{T}:aversio', 2)], 3, rec('taint_animation_focus'), warp=4)
entry('reflection_focus', -2, 6, f'focus:{NS}:reflection', [C, f'{T}:warp', f'{T}:sane_soap', '~thaumaturgetinker:focus_xp_drain'],
      [(f'{T}:alienis', 2), (f'{T}:cognitio', 3), (f'{T}:instrumentum', 2), (f'{T}:vitium', 2), (f'{T}:permutatio', 3)], 3,
      rec('reflection_focus'), warp=5)
entry('primordial_life', -6, 4, f'{NS}:primordial_life', [f'{NS}:brightest_one'],
      [(f'{T}:potentia', 2), (f'{T}:auram', 3), (f'{T}:victus', 2), (f'{FM}:superbia', 2)], 2, stages=1, warp=3,
      obtain=[{'items': [f'{NS}:primordial_life'], 'amount': 1}], meta=['hidden', 'round'])
entry('ring_of_blustering_light', -6, 6, f'{NS}:ring_of_blustering_light', [f'{NS}:primordial_life', f'~{T}:runic_shielding', f'~{T}:primordial_pearl', '~thaumaturgetinker:ichor'],
      [(f'{T}:potentia', 3), (f'{T}:praemunio', 3), (f'{T}:lux', 3), (f'{T}:praecantatio', 2), (f'{FM}:superbia', 2)], 3,
      rec('ring_of_blustering_light'), warp=3)
entry('spotlight_focus', -8, 8, f'focus:{NS}:spotlight', [f'{NS}:ring_of_blustering_light', f'{T}:focus_beam'],
      [(f'{T}:lux', 2), (f'{T}:aversio', 3), (f'{T}:ordo', 1), (f'{T}:potentia', 2)], 2, rec('spotlight_focus'))
entry('alchemist_spree_focus', -6, 8, f'focus:{NS}:alchemist_spree', [f'{NS}:ring_of_blustering_light', f'{T}:thaumatorium'],
      [(f'{T}:permutatio', 3), (f'{T}:fabrico', 1), (f'{T}:praecantatio', 2), (f'{T}:aversio', 1)], 3, rec('alchemist_spree_focus'))
entry('warp_extraction_focus', -4, 8, f'focus:{NS}:warp_extraction', [f'{NS}:ring_of_blustering_light', f'{NS}:reflection_focus'],
      [(f'{T}:alienis', 2), (f'{T}:permutatio', 3), (f'{T}:potentia', 1), (f'{T}:instrumentum', 2), (f'{T}:cognitio', 2)], 3,
      rec('warp_extraction_focus'), warp=5)

# ---------------------------------------------------------------- infusion recipes
# Translated from TCRecipeRegistry. Forbidden Magic shards and crystalwells come from Forbidden Magic (Thaumaturge), a
# required dependency; TC4 wispy essences are essentia crystals of their aspect; TC4 foci used as components -> crystals
# of their element or blank foci.
# Thaumic Tinkerer items come from Thaumaturge Tinker (a required dependency): ichor, bright nitor, smokey quartz,
# gaseous illuminae and its foci, as in the original.
# TC4 instability /2.5, capped at Thaumaturge's 6.
def n(part, settings=None, children=(), ns=NS):
    d = {'part': f'{ns}:{part}'}
    if settings: d['settings'] = settings
    if children: d['children'] = list(children)
    return d
def spell(style, *children): return {'style': style, 'root': {'part': f'{T}:origin', 'children': list(children)}}
PRESETS = {
  'reflection_focus': (spell('channeled', n('reflection')), 4),
  'scribing_focus': (spell('channeled', n('beam', {'range': 8}, [n('scribing')], ns=T)), 0),
  'wrath_attraction_focus': (spell('instant', n('wrath_attraction')), 3),
  'taint_animation_focus': (spell('channeled', n('taint_animation')), 4),
  'ethereal_manipulator_focus': (spell('channeled', n('ethereal_manipulator')), 0),
  'spotlight_focus': (spell('channeled', n('beam', {'range': 24}, [n('spotlight')], ns=T)), 0),
  'alchemist_spree_focus': (spell('channeled', n('alchemist_spree')), 0),
  'warp_extraction_focus': (spell('channeled', n('warp_extraction')), 5),
}
def focus_result(key):
    sp, warp = PRESETS[key]
    comps = {'thaumaturge:spell': sp,
             'minecraft:custom_name': json.dumps({'translate': f'{NS}.focus.preset', 'with': [{'translate': f'research.{NS}.{key}.title'}]})}
    if warp: comps['thaumaturge:warp'] = warp
    return {'count': 1, 'id': 'thaumaturge:focus_1', 'components': comps}
def crystal(a):
    aid = a if ':' in a else f'{T}:{a}'
    return {'type': 'neoforge:components', 'items': 'thaumaturge:essentia_crystal',
            'components': {'thaumaturge:crystal_aspect': {'aspect': aid}}}
def it(i): return {'item': i if ':' in i else f'{T}:{i}'}
def times(x, k): return [x] * k
def infusion(key, aspects, catalyst, components, instability, result, conditions=None):
    obj = {}
    if conditions: obj['neoforge:conditions'] = conditions
    w(f'data/{NS}/recipe/infusion/{key}.json', obj | {
      'type': 'thaumaturge:infusion', 'aspects': [{'aspect': a if ':' in a else f'{T}:{a}', 'amount': v} for a, v in aspects],
      'catalyst': catalyst, 'components': components, 'instability': instability,
      'research': {'entry': f'{NS}:{key}', 'stage': 1}, 'result': result})
BLANK = it('focus_1')
TT = 'thaumaturgetinker'
def tt(i): return {'item': f'{TT}:{i}'}
ICHOR = tt('ichor')
NITOR = tt('bright_nitor')
DARK_QUARTZ = tt('dark_quartz')
# Recipes that need Tinker's KAMI items load only while KAMI is enabled in its startup config.
KAMI = [{'type': 'neoforge:item_exists', 'item': f'{TT}:ichor'}]
PL = it(f'{NS}:primordial_life')
# Forbidden Magic (Thaumaturge) items. Original deadlyShards metadata: 0 wrath, 1 envy, 2 taint, 3 pride, 4 lust, 5 sloth, 6 greed.
def SHARD(vice): return it(f'{FM}:{vice}_shard')
CRYSTALWELL = it(f'{FM}:crystalwell')

infusion('scribing_focus', [('praemunio', 256), ('potentia', 128), ('praecantatio', 192), ('humanus', 32), ('instrumentum', 64), ('permutatio', 128)],
         BLANK, [it('primal_charm'), NITOR, CRYSTALWELL, crystal('ordo'), it('minecraft:glowstone'),
                 it('minecraft:quartz'), it('curio_knowledge'), DARK_QUARTZ], 6, focus_result('scribing_focus'))
infusion('wrath_attraction_focus', [(f'{FM}:ira', 128), ('bestia', 64), ('sensus', 64), ('desiderium', 96), ('thaumadditions:infernum', 128)],
         BLANK, times(it('minecraft:diamond'), 3) + times(SHARD('wrath'), 3) + times(DARK_QUARTZ, 3) + times(SHARD('wrath'), 3),
         6, focus_result('wrath_attraction_focus'))
infusion('taint_animation_focus', [('vitium', 256), ('aversio', 64), ('victus', 96), ('forbiddenmagic:corpus', 64), ('instrumentum', 32)],
         BLANK, [SHARD('taint'), SHARD('taint'), crystal('vitium'), it('primal_charm'), it('taint_tendril'), it('taint_tendril'),
                 it('taint_tendril'), it('primal_charm'), crystal('vitium'), SHARD('taint'), SHARD('taint'), SHARD('taint'),
                 crystal('vitium'), it('primal_charm'), it('tainted_goo'), it('tainted_goo'), it('tainted_goo'), it('primal_charm'), crystal('vitium'),
                 SHARD('taint')], 6, focus_result('taint_animation_focus'))
infusion('reflection_focus', [('vitium', 256), ('cognitio', 256), ('instrumentum', 64), ('permutatio', 128), ('alienis', 128), (f'{FM}:superbia', 64)],
         tt('focus_xp_drain'), times(it('thaumonomicon'), 2) + times(it('scribing_tools'), 3) + times(it('sanity_soap'), 3) + times(it('mirrored_glass'), 3)
                + times(CRYSTALWELL, 3) + times(it('purifying_bucket'), 3) + [it('thaumonomicon')], 6, focus_result('reflection_focus'))
infusion('ethereal_manipulator_focus', [('instrumentum', 64), ('motus', 64), ('auram', 64), ('volatus', 64)],
         tt('focus_telekinesis'), [crystal('auram'), it('minecraft:sugar'), crystal('auram'), crystal('aer'), it('alumentum'), crystal('aer'),
                 crystal('motus'), it('minecraft:sugar'), crystal('motus'), crystal('volatus'), crystal('aer'), crystal('volatus')],
         4, focus_result('ethereal_manipulator_focus'))
infusion('ring_of_blustering_light', [('potentia', 512), ('lux', 512), ('praemunio', 64), ('praecantatio', 256), ('permutatio', 32), ('ordo', 96), (f'{FM}:superbia', 96)],
         it('ring_fancy'), [it('primordial_pearl'), crystal(f'{FM}:superbia'), ICHOR, NITOR, NITOR, NITOR,
                            it('minecraft:diamond_block'), it('minecraft:diamond_block'), it('minecraft:diamond_block'), ICHOR,
                            crystal(f'{FM}:superbia'), PL, crystal(f'{FM}:superbia'), ICHOR, tt('gaseous_light_bottle'),
                            tt('gaseous_light_bottle'), tt('gaseous_light_bottle'), SHARD('wrath'), SHARD('pride'), SHARD('greed'),
                            ICHOR, crystal(f'{FM}:superbia')],
         6, {'count': 1, 'id': f'{NS}:ring_of_blustering_light'}, KAMI)
infusion('spotlight_focus', [('lux', 512), ('potentia', 512), ('praecantatio', 64), ('ordo', 128), ('aversio', 96)],
         tt('focus_shadowbeam'), [it('primordial_pearl'), ICHOR] + times(NITOR, 5) + [ICHOR, PL, ICHOR]
                + times(crystal('ignis'), 5) + [ICHOR], 6, focus_result('spotlight_focus'), KAMI)
infusion('alchemist_spree_focus', [('permutatio', 512), ('fabrico', 128), ('praecantatio', 512), ('aqua', 64), ('ignis', 64), ('alienis', 128), ('aversio', 128)],
         tt('focus_deflect'), [PL, it('quicksilver'), it('crucible'), it('alembic'), crystal('ordo'), it('alembic'), it('crucible'), it('advanced_alchemical_construct'),
                 it('primordial_pearl'), it('quicksilver'), it('crucible'), it('alembic'), crystal('ordo'), it('alembic'), it('crucible'),
                 it('advanced_alchemical_construct')], 6, focus_result('alchemist_spree_focus'))
infusion('warp_extraction_focus', [('cognitio', 256), ('alienis', 256), ('instrumentum', 64), ('permutatio', 128), ('potentia', 256), ('perditio', 64), ('spiritus', 32)],
         it('primordial_pearl'), [it('eldritch_eye'), it('curio_knowledge')] + times(tt('focus_telekinesis'), 3) + times(CRYSTALWELL, 3)
                + [ICHOR, PL, ICHOR] + times(it('minecraft:experience_bottle'), 3) + times(it('primal_charm'), 3) + [it('curio_knowledge')],
         6, focus_result('warp_extraction_focus'), KAMI)

# ---------------------------------------------------------------- curios, item models
w('data/curios/tags/item/ring.json', {'replace': False, 'values': [f'{NS}:ring_of_blustering_light']})
for item in ('ring_of_blustering_light', 'primordial_life'):
    w(f'assets/{NS}/models/item/{item}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{item}'}})
# ---------------------------------------------------------------- spell part icons (original art, WTFPL)
# Each focus's original item sprite is an animated strip; Thaumaturge draws a spell part icon as one 32x32 picture, so
# the icon is the strip's first frame at double size. The compressed blast has no original focus (it was split out of
# Signal and Impulse), so its icon is the port's own and is left alone. Phase 1 and 2 parts alike are refreshed here.
import struct, zipfile, zlib
JAR = os.environ.get('TC_ORIGINAL_JAR', '../../reference/jars/legacy-1.7.10/ThaumicConcilium-1.1.1.jar')
FOCUS_ICONS = {'signal': 'signal', 'impulse': 'impulse', 'positive_burst': 'positive_burst', 'flurry': 'cooling',
               'reflection': 'reflection', 'scribing': 'scribing', 'wrath_attraction': 'wrath_attraction',
               'taint_animation': 'taint_animation', 'ethereal_manipulator': 'ethereal_manipulator',
               'spotlight': 'spotlight', 'alchemist_spree': 'alchemist_spree', 'warp_extraction': 'warp_extraction',
               'vis_conductor': 'vis_conductor'}


def _png_rgba(b):
    pos, idat, plte, trns = 8, b'', None, None
    while pos < len(b):
        n = struct.unpack('>I', b[pos:pos + 4])[0]; t = b[pos + 4:pos + 8]; d = b[pos + 8:pos + 8 + n]; pos += 12 + n
        if t == b'IHDR': wid, hei, _, ct = struct.unpack('>IIBB', d[:10])
        elif t == b'PLTE': plte = d
        elif t == b'tRNS': trns = d
        elif t == b'IDAT': idat += d
    bpp = {6: 4, 2: 3, 3: 1, 0: 1, 4: 2}[ct]; stride = wid * bpp; raw = zlib.decompress(idat)
    prev = bytearray(stride); i = 0; px = []
    for _ in range(hei):
        f = raw[i]; i += 1; line = bytearray(raw[i:i + stride]); i += stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0; up = prev[x]; c = prev[x - bpp] if x >= bpp else 0
            if f == 1: line[x] = (line[x] + a) & 255
            elif f == 2: line[x] = (line[x] + up) & 255
            elif f == 3: line[x] = (line[x] + (a + up) // 2) & 255
            elif f == 4:
                p = a + up - c; pa, pb, pc = abs(p - a), abs(p - up), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else up if pb <= pc else c)) & 255
        for x in range(wid):
            q = line[x * bpp:(x + 1) * bpp]
            px.append(tuple(q) if ct == 6 else tuple(q) + (255,) if ct == 2 else
                      tuple(plte[3 * q[0]:3 * q[0] + 3]) + ((trns[q[0]] if trns and q[0] < len(trns) else 255),) if ct == 3 else
                      (q[0],) * 3 + (255,) if ct == 0 else (q[0],) * 3 + (q[1],))
        prev = line
    return wid, hei, px


def _write_png(path, size, px):
    raw = b''.join(b'\x00' + b''.join(struct.pack('BBBB', *px[y * size + x]) for x in range(size)) for y in range(size))
    def chunk(t, d): return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    open(path, 'wb').write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0))
                           + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


if os.path.exists(JAR):
    jar = zipfile.ZipFile(JAR)
    for part, orig in FOCUS_ICONS.items():
        wid, _, px = _png_rgba(jar.read(f'assets/{NS}/textures/items/{orig}_foci.png'))
        icon = [px[(y // 2) * wid + x // 2] for y in range(32) for x in range(32)]
        out = os.path.join(ROOT, f'assets/{NS}/textures/foci/{part}.png')
        os.makedirs(os.path.dirname(out), exist_ok=True)
        _write_png(out, 32, icon)
else:
    print('original jar not found, focus icons not refreshed:', JAR)
print('phase 2 generated')
