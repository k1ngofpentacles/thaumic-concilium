"""Builds assets/thaumicconcilium/lang/en_us.json. Original text (WTFPL) is pulled from the 1.7.10 lang file;
port-specific strings are written here. Re-run whenever new keys are added."""
import json, os
NS = 'thaumicconcilium'
ORIG = os.environ.get('TC_ORIGINAL_LANG', 'tools/original/en_US.lang')  # the 1.7.10 jar's lang file (WTFPL)
old = {}
for line in open(ORIG, encoding='utf-8'):
    line = line.rstrip('\n')
    if '=' in line and not line.startswith('#'):
        k, v = line.split('=', 1); old[k.strip()] = v
L = {}
def o(k): return old[k]
L['research_category.%s.concilium' % NS] = o('tc.research_category.CONCILIUM')
L['itemGroup.%s' % NS] = 'Thaumic Concilium'
# Sin aspect and infernum names come from Forbidden Magic (Thaumaturge).
# research
L[f'research.{NS}.concilium.title'] = o('tc.research_name.THAUMICCONCILIUM')
L[f'research.{NS}.concilium.stage_0'] = ("Thaumaturgy does not stay in the laboratory. It changes the people who practise it, the people who fear it, "
    "and the people who would profit from it. Some of my colleagues have formed a concilium of sorts: they trade knowledge, "
    "signal one another across the wilds, and watch for those who have gone too far.<BR>I should learn their methods.")
L[f'research.{NS}.concilium.stage_1'] = ("The concilium's foci are refinements of tools I already know, rebuilt to work alongside the parts I inscribe in my "
    "focal manipulator. Each can also be bound, ready-made, onto a blank focus.")
for key, orig in [('signal_focus','SIGNALFOCI'),('impulse_focus','IMPULSEFOCI'),('positive_burst_focus','POSITIVEBURSTFOCI'),('cooling_focus','COOLINGFOCI')]:
    L[f'research.{NS}.{key}.title'] = o(f'tc.research_name.{orig}')
    L[f'research.{NS}.{key}.stage_0'] = o(f'tc.research_text.{orig}')
    L[f'research.{NS}.{key}.stage_1'] = o(f'entry.{orig.lower()}.first')
# spell parts
parts = {
 'compressed_blast': ('Compressed Blast', 'Launches a clot of compressed energy that flies straight and bursts in mid-air when its fuse runs out.'),
 'impulse': ('Impulse', 'Detonates where it lands. Can instead be tuned into a pneumatic strike that spares blocks and hurls creatures away.'),
 'signal': ('Signal', 'A harmless, colourful firework burst, so your allies can find you. Can be made dazzling.'),
 'positive_burst': ('Positive Burst', 'Materialises motes of life energy that seek out nearby players other than you and heal, fortify or invigorate them.'),
 'flurry': ('Flurry', 'Throws snowballs, or fragile frost shards. Best channelled.'),
}
for p, (n, d) in parts.items():
    L[f'spell.{NS}.{p}.name'] = n; L[f'spell.{NS}.{p}.desc'] = d
settings = {'fuse':'Fuse','draw':'Draw','potency':'Potency','mode':'Mode','color':'Colour','shape':'Shape','flashbang':'Flashbang','shot':'Shot'}
for k, v in settings.items(): L[f'spell.{NS}.setting.{k}'] = v
values = {
 'compressed_blast.draw': {'none':'None','vacuum':o('focus.upgrade.vacuum.name')},
 'impulse.mode': {'blast':'Blast','pneumatic':o('focus.upgrade.pneumoStrike.name')},
 'signal.color': {c: c.replace('_',' ').title() for c in ['black','red','green','brown','blue','purple','cyan','light_gray','gray','pink','lime','yellow','light_blue','magenta','orange','white']},
 'signal.shape': {'small_ball':'Small Ball','large_ball':o('focus.upgrade.largeBall.name'),'star':o('focus.upgrade.star.name'),
                  'creeper':o('focus.upgrade.creeper.name'),'burst':'Burst','random':o('focus.upgrade.randomBang.name')},
 'signal.flashbang': {'off':'Off','on':'On'},
 'positive_burst.mode': {'heal':'Heal','fulfillment':o('focus.upgrade.fulfillment.name'),'vitaminize':o('focus.upgrade.vitaminize.name')},
 'flurry.shot': {'snowball':'Snowball','frost_shard':'Frost Shard'},
}
values['signal.color']['chaos'] = 'Chaos'
for path, vals in values.items():
    for v, txt in vals.items(): L[f'spell.{NS}.value.{path}.{v}'] = txt
# entities
for e, orig in [('compressed_blast',None),('positive_burst_orb','PositiveBurstOrbEntity'),('flurry_shot',None)]:
    L[f'entity.{NS}.{e}'] = o(f'entity.ThaumicConcilium.{orig}.name') if orig else {'compressed_blast':'Compressed Blast','flurry_shot':'Flurry Shot'}[e]
# sounds
for s in json.load(open(f'src/main/resources/assets/{NS}/sounds.json')):
    L[f'subtitles.{NS}.{s}'] = s.replace('_', ' ').capitalize()

# ---------------------------------------------------------------- phase 2
def join(*parts): return '<BR><BR>'.join(x for x in parts if x)
P2 = {
 'scribing_focus': ('SCRIBINGFOCI', 'entry.scribingfoci.first', None,
    "Channel a beam onto an ally to restore their runic shielding, two points (plus potency) each pulse, never beyond what their runic gear can hold. The runes cannot be written on yourself."),
 'ethereal_manipulator_focus': ('ETHEREALMANIPULATOR', 'entry.ethman.first', None,
    "While channelling, the dropped item or projectile you look at is held in the air in front of you. Turn the mouse wheel to draw it nearer or push it further, between one and sixteen blocks."),
 'wrath_attraction_focus': ('WRATHATTRACTION', 'entry.wrathattraction.first', None,
    "Every hostile creature nearby turns its anger on the target: on yourself when cast directly, or on whoever a delivery strikes. Reach widens the call; Burnout leaves the embittered creatures weakened."),
 'taint_animation_focus': ('TAINTANIMATION', 'entry.taintanimation.first', 'entry.taintanimation.second',
    "In the focal manipulator the part can be fixed to animating or commanding alone. Flux Vaporization boils flux goo into gas; Thought Manifestation twists mind spiders into taint spiders."),
 'reflection_focus': ('REFLECTIONFOCI', 'entry.reflectionfoci.first', 'entry.reflectionfoci.second',
    "While channelled, the focus drains experience and turns it into temporary warp. Tuned to Oblivion it takes research instead: a point of some compound aspect you know, forgotten for a larger dose of warp."),
 'ring_of_blustering_light': ('RINGOFBLUSTERINGLIGHT', 'entry.ringofblusteringlight.first', None,
    "Worn as a ring, it carries a deep runic shield and feeds raw primal vis into the wand on my belt. Only with it on can I command the Spotlight, the Alchemist's Spree and Warp Extraction."),
 'spotlight_focus': ('SPOTLIGHTFOCI', 'entry.spotlightfoci.first', None,
    "The beam's heat grows the longer it is held, and ignores armour. Letting go leaves the focus too hot to use for a while, longer the longer it burned. Warming Up turns it into a healing ray; Taint Purification cures tainted creatures, scorches the corrupted and scrubs taint from the ground. Requires the Ring of Blustering Light."),
 'alchemist_spree_focus': ('ALCHEMISTSPREE', 'entry.alchemistspreefoci.first', None,
    "While channelled, projectiles that reach me are re-forged and sent out along my aim: snowballs become frost shards, eggs hatch taint spiders, small fireballs become explosive ones and eldritch orbs become an eldritch ray. Primal Essence strengthens them further. Requires the Ring of Blustering Light."),
 'warp_extraction_focus': ('WARPEXTRACTIONFOCI', 'entry.warpextractionfoci.first', None,
    "Channel to draw out the charge; it needs temporary warp, which Reflection can supply. Keep channelling to hold it before you and release to hurl it. Start channelling while sneaking to dismiss it. Its touch hurts in proportion to my lasting warp, burning a point of temporary warp each time. Requires the Ring of Blustering Light."),
}
for key, (orig, first, second, how) in P2.items():
    L[f'research.{NS}.{key}.title'] = o(f'tc.research_name.{orig}')
    L[f'research.{NS}.{key}.stage_0'] = join(o(first))
    L[f'research.{NS}.{key}.stage_1'] = join(o(second) if second else None, how)
L[f'research.{NS}.primordial_life.title'] = o('tc.research_name.PRIMORDIALLIFE')
L[f'research.{NS}.primordial_life.stage_0'] = o('entry.primordiallife.first')
parts2 = {
 'reflection': ('Reflection', 'Trades experience (or forgotten research) for temporary warp.'),
 'scribing': ('Scribing', "Restores an ally's runic shielding."),
 'wrath_attraction': ('Wrath Attraction', 'Turns every nearby hostile creature against the target.'),
 'taint_animation': ('Taint Animation', 'Raises taintacles and swarms from fibres and flux gas, or sets tainted creatures on a victim.'),
 'ethereal_manipulator': ('Ethereal Manipulator', 'Holds the item or projectile you look at in mid-air. Mouse wheel adjusts the distance.'),
 'spotlight': ('Spotlight', 'A searing, armour-piercing heat that builds while held. Needs the Ring of Blustering Light.'),
 'alchemist_spree': ("Alchemist's Spree", 'Re-forges projectiles that reach you and fires them along your aim. Needs the Ring of Blustering Light.'),
 'warp_extraction': ('Warp Extraction', 'Draws a harmful charge of warp out of your mind. Needs the Ring of Blustering Light.'),
}
for p_, (n_, d_) in parts2.items():
    L[f'spell.{NS}.{p_}.name'] = n_; L[f'spell.{NS}.{p_}.desc'] = d_
for k, v in {'reach': 'Reach', 'burnout': o('focus.upgrade.burnout.name'), 'vaporization': o('focus.upgrade.fluxVaporization.name'),
             'manifestation': o('focus.upgrade.thoughtManifestation.name'), 'distance': 'Distance', 'primal': o('focus.upgrade.primalEssence.name')}.items():
    L[f'spell.{NS}.setting.{k}'] = v
values2 = {
 'reflection.mode': {'experience': 'Experience', 'oblivion': o('focus.upgrade.oblivion.name')},
 'wrath_attraction.burnout': {'off': 'Off', 'on': 'On'},
 'taint_animation.mode': {'original': 'Sneak to Command', 'animate': 'Animate', 'command': 'Command'},
 'taint_animation.vaporization': {'off': 'Off', 'on': 'On'},
 'taint_animation.manifestation': {'off': 'Off', 'on': 'On'},
 'spotlight.mode': {'searing': 'Searing', 'warming': o('focus.upgrade.warmingUp.name'), 'purification': o('focus.upgrade.taintPurification.name')},
 'alchemist_spree.primal': {'off': 'Off', 'on': 'On'},
 'warp_extraction.mode': {'none': 'None', 'mass_hysteria': o('focus.upgrade.massHysteria.name'), 'self_flagellation': o('focus.upgrade.selfFlagellation.name')},
}
for path, vals in values2.items():
    for v, txt in vals.items(): L[f'spell.{NS}.value.{path}.{v}'] = txt
L[f'item.{NS}.ring_of_blustering_light'] = o('item.RingOfBlusteringLight.name')
L[f'item.{NS}.ring_of_blustering_light.desc'] = 'Lets you command the Spotlight, Alchemist\'s Spree and Warp Extraction.'
L[f'item.{NS}.primordial_life'] = o('item.PrimordialLife.name')
L[f'item.{NS}.primordial_life.desc'] = o('tc.tooltip.life.0') + ' ' + o('tc.tooltip.life.1')
L[f'entity.{NS}.concentrated_warp_charge'] = 'Concentrated Warp Charge'
L[f'message.{NS}.no_ring'] = o('TC.no_ring')
L[f'message.{NS}.no_warp'] = o('TC.no_warp')
L[f'message.{NS}.oblivion_response'] = o('TC.oblivion_response')

# ---------------------------------------------------------------- phase 3
P3 = {
 'bottle_of_clear_water': ('BOTTLEWATER', ['entry.clearwater.first'], None, None),
 'bottle_of_thick_taint': ('BOTTLEOFTHICKTAINT', ['entry.bottleofthicktaint.first'], None,
    "Thrown, it bursts into a cloud that gives Flux Taint to every living, untainted creature within five blocks, and "
    "seeds the ground around with taint fibres, flux gas and ecological pressure."),
 'shard_mill': ('SHARDMILL', ['entry.shardmill.first'], ['entry.shardmill.second', 'entry.shardmill.third'],
    "Sneak and use the mill to open or close its lid. With the lid open, hold use to tip in a shard of the selected kind; "
    "with it closed, hold use to crank out the powder. Sneak and turn the mouse wheel to choose the shard."),
 'silverwood_shard_mill': ('SILVERWOODSHARDMILL', ['entry.silverwoodshardmill.first'],
    ['entry.silverwoodshardmill.second', 'entry.silverwoodshardmill.third', 'entry.silverwoodshardmill.fourth'], None),
 'runic_windings': ('RUNICWINDINGS', ['entry.runicwindings.first'], None,
    "Each winding holds a great runic charge, refills the shield faster than any rune I know and eases the cost of vis by "
    "three parts in a hundred."),
 'dump_jackboots': ('DUMPJACKBOOTS', ['entry.dumpjackboots.first'], None,
    "Sneak with a block in hand and the boots lift me steadily upward, laying the block beneath my feet as I rise."),
 'tight_belt': ('TIGHTBELT', ['entry.tightbelt.first'], None,
    "While I carry temporary warp it squeezes me with Thaumarhia, and slowly wrings the warp out of me, leaking flux gas "
    "as it does."),
 'burdening_amulet': ('BURDENINGAMULET', ['entry.burdeningamulet.first'], ['entry.burdeningamulet.second'], None),
}
for key, (orig, first, second, how) in P3.items():
    L[f'research.{NS}.{key}.title'] = o(f'tc.research_name.{orig}')
    L[f'research.{NS}.{key}.stage_0'] = join(*(o(k) for k in first))
    if second or how:
        L[f'research.{NS}.{key}.stage_1'] = join(*([o(k) for k in (second or [])] + [how]))
items3 = {
 'runic_body_windings': o('item.RunicBodyWindings.name'), 'runic_legs_windings': o('item.RunicLegsWindings.name'),
 'dump_jackboots': o('item.DumpJackboots.name'), 'elemental_surfer_shorts': 'Elemental Surfer Shorts',
 'burdening_amulet': o('item.BurdeningAmulet.name'), 'tight_belt': o('item.TightBelt.name'),
 'bottle_of_thick_taint': o('item.BottleOfThickTaint.name'), 'bottle_of_clear_water': o('item.BottleOfClearWater.name'),
 'shard_mill': o('item.ShardMill.name'), 'silverwood_shard_mill': o('item.SilverShardMill.name'),
}
for k, v in items3.items():
    L[f'item.{NS}.{k}'] = v
L[f'item.{NS}.runic_windings.desc'] = 'Refills runic shielding by 5 a second. Vis discount 3%'
L[f'item.{NS}.dump_jackboots.desc'] = 'Sneak with a block in hand to tower upwards'
L[f'item.{NS}.elemental_surfer_shorts.desc'] = 'While sneaking, unleashes the elements, paid in vis from a wand on the hotbar'
L[f'item.{NS}.burdening_amulet.desc'] = 'Your creatures attack what you look at. Feeds on temporary warp'
L[f'item.{NS}.tight_belt.desc'] = 'Wrings temporary warp out of you, leaking flux gas'
L[f'item.{NS}.shard_mill.selected'] = 'Shard: %s'
L[f'item.{NS}.shard_mill.loaded'] = 'Loaded: %s (%s)'
L[f'item.{NS}.shard_mill.open'] = 'Lid open'
L[f'item.{NS}.shard_mill.closed'] = 'Lid closed'
L[f'item.{NS}.shard_mill.desc'] = 'Sneak + use: lid. Sneak + mouse wheel: choose shard'
L[f'message.{NS}.mill_selected'] = 'Shard Mill: %s'
for e, n in {'bottle_of_thick_taint': o('item.BottleOfThickTaint.name'), 'bottle_of_clear_water': o('item.BottleOfClearWater.name'),
             'shard_powder': 'Shard Powder'}.items():
    L[f'entity.{NS}.{e}'] = n

L[f'{NS}.focus.preset'] = 'Pre-inscribed: %s'
out = f'src/main/resources/assets/{NS}/lang/en_us.json'
os.makedirs(os.path.dirname(out), exist_ok=True)
json.dump(dict(sorted(L.items())), open(out, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
print(len(L), 'keys')
