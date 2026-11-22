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
 'burdening_amulet': ('BURDENINGAMULET', ['entry.burdeningamulet.first'], ['entry.burdeningamulet.second'],
    "My gaze turns on everything I look at, my own creatures too. A carved pumpkin over my face blinds it: the amulet "
    "still drinks my warp and my creatures still go where I look, but they do the killing."),
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
L[f"item.{NS}.burdening_amulet.desc"] = "Your creatures attack what you look at. Feeds on temporary warp; a carved pumpkin blinds its gaze"
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

# ---------------------------------------------------------------- phase 3b
BR = {
 'iron': 'Iron Casting Bracelet', 'thaumium': 'Thaumium Casting Bracelet', 'void': 'Void Casting Bracelet',
 'livingwood': o('item.CastingBracelet.livingwood'), 'dreamwood': o('item.CastingBracelet.dreamwood'),
 'terrasteel': o('item.CastingBracelet.terrasteel'), 'infernal': o('item.CastingBracelet.infernal'),
 'tainted': o('item.CastingBracelet.tainted'), 'eldritch': 'Void Attuned Eldritch Bracelet',
}
for k, v in BR.items():
    L[f'item.{NS}.{k}_bracelet'] = v
L[f'item.{NS}.casting_bracelet.desc'] = 'Worn or in the shield slot: cast key (V) casts. In hand: a wand'
L[f'item.{NS}.casting_bracelet.volley'] = 'Casts instant spells ten at once, at triple cooldown'
L[f'message.{NS}.bracelet_no_focus'] = 'Your bracelet holds no focus'
L[f'message.{NS}.bracelet_drum_undirected'] = "You cannot aim while drumming"
L[f'message.{NS}.bracelet_shield_busy'] = 'Your shield arm is busy: only shield foci cast past a shield'
L[f'key.{NS}.bracelet_cast'] = 'Cast from bracelet'
L[f'key.categories.{NS}'] = 'Thaumic Concilium'
HOW = ("Worn on the wrist, it leaves both hands free: press the cast key (V unless I change it) to cast, hold it to "
       "channel or charge, and use the change-focus key with no wand in hand to choose its focus. Its casts cost two "
       "thirds of a wand's and cool down in half the time, but it holds far less vis.")
P3B = {
 'casting_bracelets': ('Casting Bracelets', 'A wand you wear',
    "A wand need not be a rod in the hand. Bent into a band about the wrist, a capped core still holds and channels vis, "
    "if rather less of it.", HOW + " The iron bracelet holds 10 vis; the thaumium one, 20."),
 'void_bracelet': ('Void Bracelet', 'Nothing, worn close',
    "Void metal drinks vis less greedily than thaumium. A band of it holds 27 vis.", None),
 'livingwood_bracelet': (o('tc.research_name.LIVINGWOODBRACELET'), o('tc.research_text.LIVINGWOODBRACELET'),
    o('entry.livingwoodbracelet.first'), "It holds 30 vis, and while I wear it, it refills from the mana tablets and rings "
    "I carry, six mana for every hundredth of a vis."),
 'dreamwood_bracelet': (o('tc.research_name.DREAMWOODBRACELET'), o('tc.research_text.DREAMWOODBRACELET'),
    o('entry.dreamwoodbracelet.first'), "It holds 50 vis and refills from my mana items as the livingwood one does."),
 'terrasteel_bracelet': (o('tc.research_name.TERRASTEELBRACELET'), o('tc.research_text.TERRASTEELBRACELET'),
    o('entry.terrasteelbracelet.first'), "It holds 50 vis and refills from mana at twenty mana for every hundredth of a "
    "vis. Every instant spell it casts fires ten times over, each at three times a plain wand's price, and the "
    "cooldown triples."),
 'infernal_bracelet': (o('tc.research_name.INFERNALBRACELET'), o('tc.research_text.INFERNALBRACELET'),
    o('entry.infernalbracelet.first'), "It holds 35 vis, and draws fire from the Nether as an infernal wand does."),
 'tainted_bracelet': (o('tc.research_name.TAINTEDBRACELET'), o('tc.research_text.TAINTEDBRACELET'),
    o('entry.taintedbracelet.first'), "It holds 35 vis, and feeds on taint as a tainted wand does."),
 'eldritch_bracelet': ('Eldritch Bracelet', 'It knows what I have seen',
    "The more permanently the Outer Lands have marked me, the faster this bracelet refills, a little of every primal at a "
    "time, unless I am warded against warp. It holds 90 vis.", None),
}
for key, (title, sub, first, second) in P3B.items():
    L[f'research.{NS}.{key}.title'] = title
    L[f'research.{NS}.{key}.stage_0'] = join(sub, first)
    L[f'research.{NS}.{key}.stage_1'] = second or HOW

L[f'{NS}.focus.preset'] = 'Pre-inscribed: %s'

# ---------------------------------------------------------------- phase 4
for i in range(4):
    L[f'{NS}.thaumaturge.taunt.{i}'] = o(f'tc.thaumaturge.taunt.{i}')
L[f'{NS}.thaumaturge.no_more'] = o('tc.thaumaturge.no_more')
for i in range(1, 4):
    L[f'{NS}.thaumaturge.trade.{i}'] = o(f'tc.thaumaturge.trade.{i}')
for k in ['mad_complete', 'reflection', 'thicktaint', 'windings', 'destcrystal', 'visconductor', 'positiveburst', 'impulse']:
    L[f'{NS}.tooltip.hint.{k}'] = o(f'tc.tooltip.hint.{k}').replace('§5', '')
L[f'{NS}.tooltip.nether_membrane'] = o('tc.tooltip.nether_membrane').replace('§c', '')
for e, orig in [('thaumaturge', 'Thaumaturge'), ('mad_thaumaturge', 'MadThaumaturge'), ('nether_explorer', 'NetherExplorer'),
                ('withered_botanist', 'WitheredBotanist')]:
    L[f'entity.{NS}.{e}'] = o(f'entity.ThaumicConcilium.{orig}.name')
    L[f'item.{NS}.{e}_spawn_egg'] = o(f'entity.ThaumicConcilium.{orig}.name') + ' Spawn Egg'
L[f'container.{NS}.thaumaturge'] = o('entity.ThaumicConcilium.Thaumaturge.name')
L[f'item.{NS}.nether_membrane'] = o('item.NetherMembrane.name')
L[f'item.{NS}.research_hint'] = 'Scribbled Research Notes'
L[f'research.{NS}.thaumaturges.title'] = o('tc.research_name.THAUMATURGES')
L[f'research.{NS}.thaumaturges.stage_0'] = join(o('tc.research_text.THAUMATURGES'), o('entry.thaumaturges.first'))
L[f'research.{NS}.thaumaturges.stage_1'] = join(o('entry.thaumaturges.second'),
    "Now they will talk to me. Offered something they value at the trade table (mana beans, skulls, wands, bottles o' "
    "enchanting, enchanted books) they lay out what they feel it is worth. A finished research note interests them most: "
    "in exchange they share an idea of their own.")
L[f'research.{NS}.mad_thaumaturges.title'] = o('tc.research_name.MADTHAUMATURGES')
L[f'research.{NS}.mad_thaumaturges.stage_0'] = join(o('tc.research_text.MADTHAUMATURGES'), o('entry.madthaumaturges.first'))
L[f'research.{NS}.mad_thaumaturges.stage_1'] = o('entry.madthaumaturges.second')
# The original's bestiary entry was also called "Thaumic Concilium", like the tab's root here; it takes its subtitle.
L[f'research.{NS}.fallen.title'] = o('tc.research_text.THAUMICCONCILIUM')
L[f'research.{NS}.fallen.stage_0'] = o('entry.thaumicconcilium.first')
L[f'research.{NS}.fallen.stage_1'] = join(o('entry.thaumicconcilium.second'), o('entry.thaumicconcilium.netherexplorer1'),
                                          o('entry.thaumicconcilium.netherexplorer2'))
L[f'research.{NS}.fallen.botanist'] = join(o('entry.thaumicconcilium.witheredbotanist1'), o('entry.thaumicconcilium.witheredbotanist2'))
L[f'research.{NS}.nether_membrane.title'] = o('tc.research_name.NETHER_MEMBRANE')
L[f'research.{NS}.nether_membrane.stage_0'] = join(o('tc.research_text.NETHER_MEMBRANE'), o('entry.nether_membrane.first'))
L[f'research.{NS}.nether_membrane.stage_1'] = ("Wrapping anything in the membrane at a crafting table, one item at a time, leaves it "
    "proof against fire and lava.")

# ---------------------------------------------------------------- phase 6a
L[f'block.{NS}.hex_of_predictability'] = o('tc.research_name.HEXOFPREDICTABILITY')
L[f'block.{NS}.destabilized_crystal'] = o('tile.DestabilizedCrystalBlock.name')
L[f'block.{NS}.vis_condenser'] = o('tile.VisCondenserBlock.name')
L[f'item.{NS}.void_slag'] = o('item.VoidSlag.name')
L[f'entity.{NS}.charged_wisp'] = o('entity.ThaumicConcilium.ChargedWispEntity.name')
L[f'entity.{NS}.primal_orb'] = 'Primal Orb'
L[f'message.{NS}.hex_no_vis'] = 'The hex needs %1$s ordo and %1$s terra vis in your wand'
L[f'spell.{NS}.vis_conductor.name'] = 'Vis Conductor'
L[f'spell.{NS}.vis_conductor.desc'] = ("Channel at a destabilized crystal cluster to draw its charge, then let go at a target. Letting go at "
    "nothing bursts the cluster. Dematerialization turns the charge on creatures, and an auram charge on rifts.")
L[f'spell.{NS}.setting.upgrade'] = 'Upgrade'
L[f'spell.{NS}.setting.extend'] = 'Extend'
for v, txt in [('none', 'None'), ('dematerialization', o('focus.upgrade.dematerialization.name')), ('wisp_launcher', o('focus.upgrade.wispLauncher.name'))]:
    L[f'spell.{NS}.value.vis_conductor.upgrade.{v}'] = txt
CLUSTER = ("Feed a cluster crystallized essentia by hand, one at a time: the first sets its aspect, each of that aspect "
           "adds one. It holds what it is given; break it and it is lost.")
P6A = {
 'vis_conductor_focus': (o('tc.research_name.VISCONDUCTOR'), o('tc.research_text.VISCONDUCTOR'), o('entry.visconductor.first'),
    join(o('entry.visconductor.second'), "Channelled at a destabilized crystal cluster, it draws the cluster's charge into "
    "itself; what happens when I let go depends on where I point it, and on the upgrade I set in the focal manipulator: "
    "Dematerialization, once I understand rifts, or the Wisp Launcher, which reels in a wisp and flings it, charged.")),
 'destabilized_crystal': (o('tc.research_name.DESTCRYSTAL'), o('tc.research_text.DESTCRYSTAL'), o('entry.destcrystal.first'), CLUSTER),
 'rifts': (o('tc.research_name.RIFTS'), o('tc.research_text.RIFTS'), o('entry.rifts.first'), join(o('entry.rifts.second'),
    "A rift torn from a node grows, lashes out at whatever lives near it, and taints the ground as it closes. The rifts "
    "that flux tears open are kin to it, if calmer.")),
 'dematerialization': (o('tc.research_name.DEMATUPGRADE'), o('tc.research_text.DEMATUPGRADE'), o('entry.dematupgrade.first'),
    o('focus.upgrade.dematerialization.text')),
 'vis_condenser': (o('tc.research_name.VISCONDENSER'), o('tc.research_text.VISCONDENSER'), o('entry.viscondenser.first'),
    join(o('entry.viscondenser.second'), "A signal from below sends the whole charge into a Hex of Predictability holding a "
    "rift within eight blocks.")),
 'hex_of_predictability': (o('tc.research_name.HEXOFPREDICTABILITY'), o('tc.research_text.HEXOFPREDICTABILITY'), o('entry.hex.first'),
    join(o('entry.hex.second'), "Eight ancient stone around a bedrock block, struck with a wand (100 ordo and 100 terra): "
    "the Hex. A jarred node set on its centre and struck again unseals into a young rift, chained above it. Condensers send "
    "it essentia; throw one item onto the hex and wait while the runes burn: the rift remakes it, if I know how. Void slag "
    "keeps it hot; too hot and the rift collapses.<BR>The same ring around a Barrier Stone makes a lesser hex (50 and 50). "
    "It cannot open a rift, but it calms a flux rift hanging above it: no more growth, no more taint when it closes, and it "
    "will remake things too, for twice the essentia. Keep the rift three blocks or more above the ring, or it eats what I "
    "throw.")),
}
for key, (title, sub, first, second) in P6A.items():
    L[f'research.{NS}.{key}.title'] = title
    L[f'research.{NS}.{key}.stage_0'] = join(sub, first)
    L[f'research.{NS}.{key}.stage_1'] = second

# ---------------------------------------------------------------- phase 6b (first cycle)
L[f'block.{NS}.quicksilver_reactor'] = o('tile.QuicksilverCrucible.name')
L[f'item.{NS}.thaumaturge_drum'] = o('item.ThaumaturgeDrum.name')
L[f'item.{NS}.warped_drum'] = o('item.WarpedDrum.name')
L[f'item.{NS}.polishment_device'] = o('item.PolishmentDevice.name')
L[f'item.{NS}.coagulating_eye_drops'] = o('item.CoagulatingEyeDrops.name')
L[f'entity.{NS}.quicksilver_elemental'] = o('entity.ThaumicConcilium.QuicksilverElemental.name')
L[f'item.{NS}.quicksilver_elemental_spawn_egg'] = o('entity.ThaumicConcilium.QuicksilverElemental.name') + ' Spawn Egg'
L[f'{NS}.tooltip.xylography'] = o('tc.tooltip.xylography').replace('§6§n', '')
L[f'{NS}.tooltip.polishment'] = o('tc.tooltip.polishment')
L[f'{NS}.tooltip.creative'] = o('tc.tooltip.creative').replace('§6', '')
P6B = {
 'thaumdrum': (o('tc.research_name.THAUMDRUM'), o('tc.research_text.THAUMDRUM'), o('entry.thaumdrum.first'),
    "Hold the use key to play it: a bass beat every second of a second and a half, and every one and a half seconds a wisp "
    "of a random primal aspect answers the call, until sixteen roam within sixty-four blocks. They are not grateful."),
 'warpdrum': (o('tc.research_name.WARPDRUM'), o('tc.research_text.WARPDRUM'), o('entry.warpdrum.first'),
    "Throw a Thaumaturge's Drum onto a Hex of Predictability whose rift holds 128 sensus, 128 perditio, 64 cognitio, 64 bestia "
    "and 64 vitium. Played, the Warped Drum calls a harmless mind spider every half second, up to sixteen within thirty-two blocks."),
 'xylography': (o('tc.research_name.XYLOGRAPHY'), o('tc.research_text.XYLOGRAPHY'), o('entry.xylography.first'), None),
 'quicksilver_crucible': (o('tc.research_name.QUICKSILVERCRUCIBLE'), o('tc.research_text.QUICKSILVERCRUCIBLE'),
    o('entry.quicksilvercrucible.first'),
    join(o('entry.quicksilvercrucible.second').replace('no more than two blocks away', 'within eight blocks'),
         "Throw quicksilver into the reactor to feed it (each fraction is two points of permutatio, up to sixty-four); a redstone "
         "signal pauses it, and a comparator reads how full it is. Creatures standing in the mercury are burned.")),
}
for key, (title, sub, first, second) in P6B.items():
    L[f'research.{NS}.{key}.title'] = title
    L[f'research.{NS}.{key}.stage_0'] = join(sub, first)
    if second:
        L[f'research.{NS}.{key}.stage_1'] = second

# ---------------------------------------------------------------- phase 6b (second cycle)
L[f'item.{NS}.astral_monitor'] = o('item.AstralMonitor.name')
L[f'block.{NS}.lithographer'] = o('tile.Lithographer.name')
L[f'container.{NS}.astral_monitor'] = o('item.AstralMonitor.name')
L[f'container.{NS}.lithographer'] = o('tile.Lithographer.name')
L[f'{NS}.tooltip.monitor'] = o('tc.tooltip.monitor').replace('§9', '')
L[f'message.{NS}.lithographer_no_vis'] = 'The Lithographer needs 100 ignis, 100 ordo, 50 aqua and 100 aer vis in your wand'
P6B2 = {
 'astral_monitor': (o('tc.research_name.ASTRALMONITOR'), o('tc.research_text.ASTRALMONITOR'), o('entry.astralmonitor.first'),
    join(o('entry.astralmonitor.second'), o('entry.astralmonitor.third'),
         "Throw a Thaumometer onto a Hex of Predictability whose rift holds 128 sensus, 128 vacuos and 64 cognitio. Sneak and "
         "use the monitor to open its slot, which takes a xylographed wand; held in the main hand it reads that player's "
         "health, mana (with Botania) and runic shielding every half second and shows them as constellation glyphs over its screen.")),
 'lithographer': (o('tc.research_name.LITHOGRAPHER'), o('tc.research_text.LITHOGRAPHER'), o('entry.lithographer.first'),
    join(o('entry.lithographer.second'),
         "Build it with a wand (100 ignis, 100 ordo, 50 aqua, 100 aer): over the head of a bed set a thaumium block, with a block "
         "of gold either side of it and either side of the bed, in a line along one axis; stand an Alchemical Construct on the "
         "thaumium block and strike it with the wand. The Lithographer takes the thaumium block's place. Four Astral Monitors "
         "that are watching someone go in its slots. A redstone signal starts it: it needs a Vis Condenser directly above it, "
         "a destabilized crystal of sensus holding at least 32 above that, and someone asleep in the bed.",
         "Coagulating Eye Drops (a phial of sensus, 4 perditio, 2 sensus, 2 victus in a crucible) switch the sight off and on again.")),
}
for key, (title, sub, first, second) in P6B2.items():
    L[f'research.{NS}.{key}.title'] = title
    L[f'research.{NS}.{key}.stage_0'] = join(sub, first)
    L[f'research.{NS}.{key}.stage_1'] = second

# ---------------------------------------------------------------- phase 6c (first cycle): Gadomancy's Aura Cores
# Text adapted from Gadomancy 1.7.10 (LGPL; Makeo and the Gadomancy team): item.ItemAuraCore.name, gadomancy.auracore.blank,
# tc.research_*.GADOMANCY.AURA_CORE and gadomancy.research_page.AURA_CORE.1 / .3 / CLEAN_AURA_CORE.1 (spelling corrected).
L[f'item.{NS}.aura_core'] = 'Aura Core'
L[f'{NS}.auracore.blank'] = 'Blank'
L[f'message.{NS}.aura_core_no_vis'] = 'The cluster needs %s vis of its primal in your wand'
L[f'research.{NS}.aura_core.title'] = 'Aura Core'
L[f'research.{NS}.aura_core.stage_0'] = join('Your surroundings matter!',
    "Many times, the strange properties of the primal charm have been puzzling. Constantly spitting out small sparks of primal "
    "energy and changing colors in a completely unstable fashion, the charm's behavior is unlike anything else, making the primal "
    "charm most difficult to use. By encasing the charm in a layer of crystallized thaumium it is possible to calm these bizarre "
    "tendencies, resulting in a much more usable object known as an Aura Core.")
L[f'research.{NS}.aura_core.stage_1'] = join(
    "Unlike the charm that it was constructed from, an Aura Core has had most of the primal energy removed in the crafting process. "
    "This leaves Aura Cores heavily susceptible to the presence of auras in the environment. Tossing one on the ground near a "
    "primal crystal cluster and then energizing the cluster with a wand will cause the core to coat itself in an extra layer of "
    "crystal, forcing the core to tune to one primal aspect. Six primal cores are possible to make.",
    "Here: the wand pays 84 vis of the cluster's primal and the cluster is consumed (without breaking blocks around it). The core "
    "then gathers for twenty-five seconds, reading the aspects of every block in the nine-by-nine cube around it, so surround it "
    "with the element you want: at least 109 blocks with aspects are needed, and the winner is drawn by weight from the primals "
    "above the average. Too bare a place and the core comes back blank.",
    "Returning an Aura Core to its original state is very simple. Tossing an attuned core into a bath of essentia (a crucible "
    "holding 12 praecantatio, 12 aqua and 18 victus) removes the extra crystal coat without damaging the core.",
    "(Adapted from Gadomancy by Makeo, LGPL.)")

# ---------------------------------------------------------------- phase 6c (second cycle): Gem of Rifts, Terra-cast Gem, Causal Bouillon
L[f'item.{NS}.inert_rift_gem'] = o('item.RiftGemInert.name')
L[f'item.{NS}.rift_gem'] = o('item.RiftGem.name')
L[f'item.{NS}.inert_terra_cast_gem'] = o('item.TerraGemInert.name')
L[f'item.{NS}.terra_cast_gem'] = o('item.TerraGem.name')
L[f'block.{NS}.void_reality'] = o('tile.VoidRealityBlock.name')
L[f'{NS}.tooltip.gem_cost'] = 'Vis cost:'
L[f'{NS}.tooltip.riftgem'] = o('tc.tooltip.riftgem').replace('§9', '')
L[f'{NS}.tooltip.riftgem.inflate'] = o('tc.tooltip.riftgem.inflate').replace('§e', '')
L[f'{NS}.tooltip.riftgem.deflate'] = o('tc.tooltip.riftgem.deflate').replace('§e', '')
L[f'{NS}.tooltip.terragem'] = o('tc.tooltip.terragem').replace('§9', '')
L['death.attack.%s.void_reality' % NS] = '%1$s was lost to the Void Reality'
L[f'research.{NS}.rift_gem.title'] = o('tc.research_name.RIFTGEM')
L[f'research.{NS}.rift_gem.stage_0'] = join(o('tc.research_text.RIFTGEM'), o('entry.riftgem.first'), o('entry.riftgem.second'))
L[f'research.{NS}.rift_gem.stage_1'] = join(
    o('entry.riftgem.third'), o('entry.riftgem.fourth'),
    "Here: hold the use key with the gem in your hand and a wand with 10 perditio and 10 aer in your hotbar; every four seconds, "
    "where no other rift is within sixteen blocks, it opens a rift that lives about ten seconds. Step into it with the gem in your "
    "main hand to enter the Causal Bouillon at the same x and z; step into another rift there to leave, at ten times (or, with the "
    "space deflated, a tenth of) your x and z, to a safe surface. Shift + use cycles the casts. The gem is made by infusion from an "
    "entropy aura core, and woken by throwing it onto a Hex of Predictability whose rift holds 128 motus, 128 perditio, 128 vitreus, "
    "128 vacuos and 64 auram. Crafting it with a Terra-cast Gem that holds a cast adds the cast (sixteen at most).")
L[f'research.{NS}.terra_cast_gem.title'] = o('tc.research_name.TERRACAST')
L[f'research.{NS}.terra_cast_gem.stage_0'] = join(o('tc.research_text.TERRACAST'), o('entry.terracast.first'), o('entry.terracast.second'))
L[f'research.{NS}.terra_cast_gem.stage_1'] = join(
    "Here: hold the use key on a solid block that holds terra (10 terra and 10 aer from a wand in your hotbar). The block "
    "vanishes without a drop and the gem remembers it and your dimension. The gem is made by infusion from an earth aura core, and "
    "woken by throwing it onto a Hex of Predictability whose rift holds 128 motus, 128 perditio, 128 vitreus, 128 cognitio and 64 auram.")

# ---------------------------------------------------------------- phase 6c (third cycle): the Brightest One
L[f'entity.{NS}.brightest_one'] = o('entity.ThaumicConcilium.BrightestOne.name')
L[f'research.{NS}.brightest_one.title'] = o('tc.research_name.BRIGHTESTONE')
L[f'research.{NS}.brightest_one.stage_0'] = join(o('tc.research_text.BRIGHTESTONE'), o('entry.brightestone.first'))
L[f'research.{NS}.brightest_one.stage_1'] = join(o('entry.brightestone.second'),
    "Here: fling a charged wisp (Vis Conductor with the wisp launcher) into a chained rift on a Hex of Predictability. The rift "
    "turns interdimensional and collapses, and the Brightest One is born from it. It cannot be hurt: every charged wisp that "
    "reaches it changes its aspect and costs it one of its fifteen health.")

# ---------------------------------------------------------------- phase 5a (first cycle): primal arrows (Thaumcraft 4 mechanics, port text)
PA = {'aer': 'Aer', 'ignis': 'Ignis', 'aqua': 'Aqua', 'terra': 'Terra', 'ordo': 'Ordo', 'perditio': 'Perditio'}
PA_TIP = {
    'aer': 'Magic damage that ignores armour',
    'ignis': 'Sets the target alight for five more seconds',
    'aqua': 'Slows the target (V, 10 s) and never ignites',
    'terra': 'Hits half as hard again and knocks back further',
    'ordo': 'Weakens the target (V, 10 s); magic damage that ignores armour; hits softer',
    'perditio': 'Withers the target (5 s); hits softer',
}
for k, n in PA.items():
    L[f'item.{NS}.primal_arrow_{k}'] = f'{n} Arrow'
    L[f'{NS}.tooltip.primal_arrow.{k}'] = PA_TIP[k]
L[f'entity.{NS}.primal_arrow'] = 'Primal Arrow'
L[f'death.attack.{NS}.air_arrow'] = '%1$s was torn apart by a gale arrow'
L[f'death.attack.{NS}.air_arrow.player'] = '%1$s was torn apart by a gale arrow from %2$s'
L[f'death.attack.{NS}.fire_arrow'] = '%1$s was burnt by a fire arrow'
L[f'death.attack.{NS}.fire_arrow.player'] = '%1$s was burnt by a fire arrow from %2$s'
L[f'death.attack.{NS}.order_arrow'] = '%1$s was unravelled by an arrow of order'
L[f'death.attack.{NS}.order_arrow.player'] = '%1$s was unravelled by an arrow of order from %2$s'
L[f'research.{NS}.primal_arrows.title'] = 'Primal Arrows'
L[f'research.{NS}.primal_arrows.stage_0'] = join('Arrows with a crystal heart',
    "A crystal of a primal, set in the head of an ordinary arrow, makes the arrow carry that primal with it. Six kinds are possible, "
    "one for each primal. They fly half as fast again as a plain arrow drawn the same, and any bow or crossbow will take them.")
L[f'research.{NS}.primal_arrows.stage_1'] = join(
    "Aer and Ordo arrows strike as magic and pass through armour; Ordo also saps the target's strength for ten seconds, but hits "
    "a fifth softer than a plain arrow. Ignis arrows burn, adding five seconds of fire to anything they hit (but never endermen), "
    "and Aqua arrows chill the target to a crawl for ten seconds and will not set it alight.",
    "Terra arrows hit half as hard again, and when a player shot them they knock the target back like an extra level of Punch. "
    "Perditio arrows wither what they hit for five seconds and also hit a fifth softer than a plain arrow.",
    "Here: eight arrows of one kind are made at an arcane workbench from an arrow in every slot but the middle, which takes an "
    "essentia crystal of that primal, and cost 8 vis of the primal. Infinity does not make them free. A player's own arrows can be picked up again.")

# ---------------------------------------------------------------- phase 5a (second cycle): the Crimson Paladin, Archer and Ranger
for e, orig in (('crimson_paladin', 'CrimsonPaladin'), ('crimson_archer', 'CrimsonArcher'), ('crimson_ranger', 'CrimsonRanger')):
    L[f'entity.{NS}.{e}'] = o(f'entity.ThaumicConcilium.{orig}.name')
    L[f'item.{NS}.{e}_spawn_egg'] = o(f'entity.ThaumicConcilium.{orig}.name') + ' Spawn Egg'
L[f'entity.{NS}.crimson_orb'] = o('entity.ThaumicConcilium.CrimsonOrbEntity.name')
L[f'entity.{NS}.ethereal_shackles'] = 'Ethereal Shackles'

# cycle 3: the portal and the Pontifex (the original's name is a format string "Pontifex %s the %d"; ours drops the champion modifier)
L[f'entity.{NS}.lesser_crimson_portal'] = o('entity.ThaumicConcilium.LesserPortal.name')
L[f'item.{NS}.lesser_crimson_portal_spawn_egg'] = o('entity.ThaumicConcilium.LesserPortal.name') + ' Spawn Egg'
L[f'entity.{NS}.crimson_pontifex'] = 'Crimson Pontifex'
L[f'item.{NS}.crimson_pontifex_spawn_egg'] = 'Crimson Pontifex Spawn Egg'
L[f'message.{NS}.pontifex_enrage'] = '%s is enraged!'

# cycle 4a: Crimson Annales and the two research entries
L[f'item.{NS}.crimson_annales'] = o('item.CrimsonAnnales.name')
L[f'message.{NS}.annales_unclear'] = o('tc.tooltip.book.2')
L[f'research.{NS}.crimson_annales.title'] = o('tc.research_name.CRIMSONANNALES')
L[f'research.{NS}.crimson_annales.stage_0'] = join(o('tc.research_text.CRIMSONANNALES'), o('entry.crimsonannales.first'))
L[f'research.{NS}.crimson_annales.stage_1'] = o('entry.crimsonannales.second')
L[f'research.{NS}.crimson_pontifex.title'] = o('tc.research_name.CRIMSONPONTIFEX')
L[f'research.{NS}.crimson_pontifex.stage_0'] = join(o('tc.research_text.CRIMSONPONTIFEX'), o('entry.crimsonpontifex.first'),
    "Here: with this knowledge, a Crimson Rites thrown into a chained rift is not wasted. The rift swallows it, turns on itself and "
    "collapses, and a Lesser Crimson Portal that will not stop until it has brought the Pontifex through opens in its place.")

# cycle 4b: the Pontifex's hammer and robe
L[f'item.{NS}.gilded_crimson_helm'] = 'Gilded Crimson Helm'
L[f'item.{NS}.pontifex_hammer'] = o('item.PontifexHammer.name')
L[f'item.{NS}.pontifex_helm'] = o('item.ItemPontifexRobeHead.name')
L[f'item.{NS}.pontifex_chest'] = o('item.ItemPontifexRobeChest.name')
L[f'item.{NS}.pontifex_legs'] = o('item.ItemPontifexRobeLegs.name')
L[f'item.{NS}.pontifex_boots'] = o('item.ItemPontifexRobeFeet.name')
L[f'item.{NS}.pontifex_hammer.desc'] = 'Hold use to drain the life of everything within six blocks'
L[f'item.{NS}.pontifex_robe.desc'] = 'Full set: press the toggle key to let go of your body. Cultists will not hunt you. Can be dyed'
L[f'key.{NS}.pontifex_toggle'] = "Toggle Pontifex's Robe"
L[f'research.{NS}.pontifex_hammer.title'] = o('tc.research_name.PONTIFEXHAMMER')
L[f'research.{NS}.pontifex_hammer.stage_0'] = join(o('tc.research_text.PONTIFEXHAMMER'), o('entry.pontifexhammer.first'))
L[f'research.{NS}.pontifex_hammer.stage_1'] = "Infused on a Crimson Blade with gold, void metal, thaumium and void seeds. Hold the use button: everything living within six blocks loses health and you gain it."
L[f'research.{NS}.pontifex_robe.title'] = o('tc.research_name.PONTIFEXROBE')
L[f'research.{NS}.pontifex_robe.stage_0'] = join(o('tc.research_text.PONTIFEXROBE'), o('entry.pontifexrobe.first'))
L[f'research.{NS}.pontifex_robe.stage_1'] = join(o('entry.pontifexrobe.second'),
    "Here: each piece is made by throwing a Crimson Robe piece (helm, robe, legs, boots) onto a hex whose chained rift holds the essentia.")
L[f'research.{NS}.crimson_initiation.title'] = o('tc.research_name.CRIMSONINITIATION')
L[f'research.{NS}.crimson_initiation.stage_0'] = join(o('tc.research_text.CRIMSONINITIATION'), o('entry.crimsonimitiation.first'),
    "Here: the dagger of the Cult is not in this pack, so nothing opens from this knowledge yet.")
L[f'{NS}.tooltip.hint.pontifexhammer'] = 'The weapon of the Pontifex: a hammer that drinks life. Its making is set down here...'
L[f'{NS}.tooltip.hint.pontifexrobe'] = 'The vestments of the Pontifex, and how the Cult came to fear them. The method is set down here...'
L[f'{NS}.tooltip.hint.materialpeeler'] = 'Where did the Pontifex go? These notes say how to follow...'

# cycle 5: the Arcane Levitator (our own text; Thaumcraft 4's levitator is mechanics only)
L[f'block.{NS}.arcane_levitator'] = 'Arcane Levitator'
L[f'block.{NS}.arcane_levitator.desc'] = 'Lifts what stands above it. Sneak to settle. Stack them for height; redstone switches it off'
L[f'research.{NS}.arcane_levitator.title'] = 'Arcane Levitator'
L[f'research.{NS}.arcane_levitator.stage_0'] = join('Rising on a breath',
    'A chamber of greatwood and brass that holds a pocket of bound air, fed from a nitor. Anything above it, a dropped item, a beast or yourself, is lifted up the column of air.')
L[f'research.{NS}.arcane_levitator.stage_1'] = join(
    'The column is ten blocks tall and ends at the first solid block. Stacking levitators adds ten blocks each. Sneaking settles you gently; redstone on the levitator, or on the block above it, switches it off.')

# ---------------------------------------------------------------- phase 6b (third cycle): polished infusions
# Thaumaturge's book has no page type for the original's "Polished Infusion" page, so the recipe stage says it in words.
# Polishment files are named after their research entry (gen_phase6b_data.py).
import glob
def aspect_name(a):
    return a.split(':')[1].capitalize()
L['message.%s.polishment_failed' % NS] = 'The infusion unravels: it needed a polished draw from a crystal'
L['tooltip.%s.polished_infusion' % NS] = 'Polished Infusion: %s %s'
for f in sorted(glob.glob(f'src/main/resources/data/{NS}/{NS}/polishment/*.json')):
    pol = json.load(open(f, encoding='utf-8'))
    key = os.path.basename(f)[:-5]
    L[f'research.{NS}.{key}.stage_1'] = join(L[f'research.{NS}.{key}.stage_1'],
        f"Polished Infusion: needs {pol['amount']} {aspect_name(pol['aspect'])} drawn from a Destabilized Crystal with a Vis Conductor, "
        "held as the infusion completes; without it the result is lost.")

out = f'src/main/resources/assets/{NS}/lang/en_us.json'
os.makedirs(os.path.dirname(out), exist_ok=True)
json.dump(dict(sorted(L.items())), open(out, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
print(len(L), 'keys')
