# Port status

Legend: ✅ ported · 🟡 partial · ⬜ not started

## Phase 1: foundation and first foci (0.1.0)

| Area | Status | Notes |
|---|---|---|
| Project, registries, config, networking | ✅ | Compiles against NeoForge 21.1.265 and Thaumaturge 1.1.0 |
| Sin aspects | ✅ | Moved to Forbidden Magic (Thaumaturge) in 0.2.1 (`forbiddenmagic:*`); the infernum fallback moved with them |
| Research tab "Thaumic Concilium" | ✅ | Original icon and background. Root entry plus 4 focus entries |
| Signal focus | ✅ | `compressed_blast` delivery + `signal` effect. Colour (16 + chaos), shape (5 + random), flashbang |
| Impulse focus | ✅ | `impulse` effect: blast or pneumatic strike, potency 0-3. Vacuum lives on `compressed_blast` |
| Positive Burst focus | ✅ | `positive_burst` effect: heal, fulfillment, vitaminize; homing orbs skip the caster |
| Cooling focus | ✅ | `flurry` delivery, channelled; snowball or frost shard (Refreeze) |
| Ready-inscribed foci | ✅ | Creative tab, plus infusion recipes translated from the originals |

### Mapping decisions (phase 1)

- The compressed blast is a **delivery**, so any effect can ride it; Impulse and Signal are **effects**.
  The original's two foci are reproduced by the presets `blast → signal` and `blast → impulse`.
- Vacuum moved from Impulse to the blast delivery, because it acts during flight.
- TC4 focus ranks are gone. Every setting is available at once and costs complexity/vis instead.
- Recipe translation:
  - Fire focus / frost focus / Thaumic Tinkerer heal focus → blank Thaumaturge focus
  - Primal charm → `primal_charm`; obsidian totem → `obsidian_totem`; one-way tube → `tube_oneway`
  - Sano → victus, telum → aversio
- The billboard sprites use Thaumaturge's `sparkle` strip, because TC4's particle atlas is All Rights Reserved.

### Needs in-game testing

- Each preset casts and draws vis sensibly. Tune `complexity`/`vis` in the spell-part JSON if costs feel off.
- The flurry's fire rate (Thaumaturge's pulse interval: `4 + complexity/8` ticks).
- Research entries appear in the new tab and gate the parts.

## Phase 2: remaining foci, the Ring of Blustering Light (0.2.0)

| Area | Status | Notes |
|---|---|---|
| Reflection focus | ✅ | `reflection` effect: experience → temporary warp, or Oblivion (forget a research aspect point) |
| Scribing focus | ✅ | `scribing` effect: refills another player's Thaumaturge runic shield; preset is beam → scribing |
| Wrath Attraction focus | ✅ | `wrath_attraction` effect: hostiles target the caster (or whoever a delivery hits); Burnout; Ira sigil |
| Taint Animation focus | ✅ | `taint_animation` effect: fibre → taintacle, flux gas → swarm, goo → gas; command tainted mobs (sneak, as the original) |
| Ethereal Manipulator focus | ✅ | `ethereal_manipulator` effect: telekinesis for items and projectiles; mouse wheel sets distance 1-16 |
| Spotlight focus | ✅ | `spotlight` effect on a beam: ramping armour-piercing damage, Warming Up, Taint Purification; overheat cooldown |
| Alchemist's Spree focus | ✅ | `alchemist_spree` effect: snowball → frost shard, egg → taint spider, small → explosive fireball, eldritch orb → ray |
| Warp Extraction focus | ✅ | `warp_extraction` effect + Concentrated Warp Charge entity; Mass Hysteria, Self-flagellation |
| Ring of Blustering Light | ✅ | Curios ring: runic charge 50, primal vis trickle into a hotbar wand, 3 warp. Gates the three parts above |
| Primordial Life | 🟡 | Item and research only; it drops from the Brightest One, which arrives in phase 6 |
| Vis Conductor focus | ⬜ | Deferred to phase 6 with the Destabilized Crystal, rifts and the Material Peeler |

### Mapping decisions (phase 2)

- The ring requirement is checked on Thaumaturge's `SpellCastEvent.Pre`, before vis is paid. A refused cast costs nothing.
- Warp: TC4's temporary warp is Thaumaturge's `TEMPORARY`. Warp Extraction's damage uses permanent + normal warp
  (TC4 had permanent and sticky). A charge only burns warp on a successful hit (the original drained it every tick of contact).
- Per-tick TC4 effects are re-tuned per Thaumaturge channel pulse (about 5 ticks): Reflection drains 50 XP for
  `(1 + potency) × 5` temporary warp per pulse by default (JSON-tunable).
- Taint Animation's command strength is capped at amplifier 9 (original cap 200). Thought Manifestation makes a tainted
  spider; Concilium's "smart" taint spider arrives with the phase-4 mobs.
- Spotlight purification cures trait-tainted animals (Thaumaturge's tainted cows, sheep and so on are vanilla mobs with the
  `thaumaturge:tainted` trait), doubles damage on taint and eldritch creatures, and scrubs ecological pressure and
  fibres where the beam lands (TC4 changed the biome instead).
- Alchemist's Spree's fireball is always incendiary (vanilla large fireball). Bottles of clear water → thick taint
  join with the bottles in a later phase. TC4's primal orb → pech blast has no Thaumaturge counterpart and is dropped.
- Thaumic Tinkerer ingredients (ichor, bright nitor, smokey quartz, gaseous illuminae, the telekinesis, experience
  drain, shadowbeam, distortion and mending foci) come from **Thaumaturge Tinker**, a required dependency since 0.2.0.
  Recipes needing ichor carry a `neoforge:item_exists` condition, so they drop out if Tinker's KAMI is disabled.
- Since 0.2.1 the recipes use real Forbidden Magic shards (wrath, taint, pride, greed) and crystalwells where the
  original did; wispy essences stay as essentia crystals of their aspect. The original's shard metadata order was
  wrath, envy, taint, pride, lust, sloth, greed, so Taint Animation now takes taint shards (the stand-ins were lust).
- Research parents that are later-phase entries (Destabilized Crystal, Hex of Predictability, Brightest One) are replaced
  with Concilium and Thaumaturge entries for now. The original's hidden parents become Thaumaturge `~` parents,
  including Tinker's Bright Nitor, Telekinesis, Experience Drain and Ichor entries.
- Phase-1 research entries now list their infusion recipes on the last stage, so the book shows them.

### Needs in-game testing

- The ring shows in a Curios ring slot, shields, and fills a wand; the three ring parts refuse without it.
- Ethereal Manipulator's wheel capture while channelling, and Warp Extraction's release-to-throw.
- Taint Animation in a taint biome; Scribing on a second player with runic gear.

## Phase 3: gear, warp baubles, bottles, shard mills (0.3.0)

| Area | Status | Notes |
|---|---|---|
| Runic Body and Legs Windings | ✅ | Armour: runic charge 200 each, +5 shield a second, 3% vis discount, 3 warp each. Research hidden until Runic Shielding and the Crystalwell are known |
| Dump Jackboots | ✅ | Boots: sneak with a block in hand to rise steadily, laying it beneath you |
| Elemental Surfer Shorts | ✅ | Creative-only, as in the original (no recipe). While sneaking, vis-paid embers, frost shards, golem orbs and projectile deflection. Art is original (the 1.7.10 jar had none) |
| Burdening Amulet | ✅ | Curios necklace: your pets attack what you look at (32 blocks); temporary warp → Death Gaze, drained ~1 in 10 per tick |
| Tight Belt | ✅ | Curios belt: temporary warp → Thaumarhia (by total warp), drained ~1 in 25 per tick, leaking flux gas |
| Bottle of Clear Water | ✅ | Crucible (phial + aqua). Thrown: puts out fire and burning creatures, scalds blazes and magma cubes |
| Bottle of Thick Taint | ✅ | Crucible (bottle of taint + vitium, aqua, vitreus). Thrown: Flux Taint around, fibres, flux gas, ecological pressure |
| Shard Mill, Silverwood Shard Mill | 🟡 | Arcane / infusion recipes; all thirteen powders. Static OBJ model: the first-person cranking animation isn't ported yet |
| Casting bracelets | ✅ 0.4.1 | Worn (Curios bracelet slot) or in the shield slot, V to cast, F radial. Livingwood, dreamwood and terrasteel need Botania (Revived). Band model on the wrist in first and third person; the casting arm rises (synced) with particles. Checked in the render harness and by `BraceletTests`; feel untested in game |
| Polishment (and the Polishment Device) | ⬜ | Phase 6, with the machines |
| Coagulating Eye Drops, Astral Monitor | ⬜ | Phase 6, with the Hex of Predictability |
| Thaumaturge and Warped Drums | ⬜ | Phase 4, with the Thaumaturges |

### Fixed in 0.3.1 (after the first in-game test)

- Greatwood Shard Mill: Thaumaturge's primal shards are essentia crystals (what crystal clusters drop); the mill only
  looked for the cluster block items, so it never loaded. It now takes both.
- Shard powder: each mote trails dust in its shard's colour. Powders that act on a creature act when they touch one
  (the original waited until the mote settled), and never act on the player who sprayed them.
- Shard Mill motion (0.3.2): tips towards you while loading. While grinding it is held out bottom-forward like a pepper
  grinder, the crank handle (a separate OBJ part, as in the original) spins about its shaft, and the other hand works
  the crank: drawn in first person, and in third person through a custom arm pose (`THAUMICCONCILIUM_MILL_CRANK`,
  added with NeoForge's enum extension) that other players see too. Since 0.3.3 the hand grips the crank: in first
  person the arm is drawn by the mill's renderer with the fist pinned to the grip; in third person the arm is aimed at
  the grip every frame. Since 0.3.4 third person is computed rather than tuned: the holding arm is lowered to chest
  height, the mill's bottom points forward and slightly up with the crank inward, and the free arm is aimed at the
  grip's exact position (found by following the game's held-item transform chain in `MillArmPose`).
  Since 0.3.5 the mill sits on the inside of the holding hand, and loading has a third-person pose too: the mill is
  held upright, tipping its top towards the chest, while the free hand lowers into its mouth.
- Dump Jackboots (0.3.9): a steady lift. Sneaking with a block in hand raises you 0.3 blocks a tick (six blocks a
  second) and lays the block under your feet as each space clears. The rise is kept below the point where letting go
  coasts you past a block (0.7 blocks of coast at 0.3, 1.7 at the earlier 0.5), so pillars can't be built in floating
  segments. (0.3.7-0.3.8 tried a placement-driven design; rolled back.)
- Bottles: glass, a coloured cloud and the potion-break sound on impact (water splash for Clear Water, witch sparks
  for Thick Taint).
- Burdening Amulet: Thaumaturge's Death Gaze withers every creature in sight, pets included. While the amulet is worn,
  the wearer's own pets are spared its Wither (exactly Wither I for 80 ticks; other sources still apply) and are never
  turned against their owner.

### Mapping decisions (phase 3)

- TC4 aspect amounts are kept and instability is divided by 2.5, as in phases 1-2. The one arcane recipe (Shard Mill)
  uses Forbidden Magic's rule: vis is the sum of the costs, crystals are each cost / 10.
- Ingredient stand-ins: TC4 balanced crystal cluster → primal charm; sinister lodestone → lodestone; girdle of hovering →
  thaumostatic harness (the research calls it the "thaumostatic girdle"); TC4 research notes → research note;
  nitor → any colour of Thaumaturge nitor (`#thaumaturge:nitors`); slime aspect → aqua, poison → mortuus.
- Research parents that are later-phase entries (Thaumaturges, Destabilized Crystal) are replaced by the Concilium root.
  Entries keep their original positions, which don't collide with phases 1-2.
- Thick Taint: Thaumaturge has no taint biome, so the original's biome change becomes 0.04 ecological pressure per spot
  (a flux lightning strike's worth) plus an attempt at fibre growth.
- Shard Mill: the shard is chosen with sneak + mouse wheel (the original used a radial menu). The balanced shard (no
  Thaumaturge counterpart) and the gluttony shard (its powder did nothing) are left out. Pride powder keeps the
  original's intent, flinging creatures away; the original's formula aimed them at the world origin.
- Strong original numbers are kept for now and worth judging in play: Runic Windings' 200 charge each, entropy powder's
  Wither 51, wrath powder's 50000-second burn, pride and greed powders' speed of 3.

### Needs in-game testing

- **Art:** the Shard Mills' OBJ model in hand, in the GUI and on the ground (display transforms are a first guess);
  armour layers for the windings, jackboots and shorts; item icons.
- Runic Windings: shield size with both pieces, the faster refill, the vis discount in the arcane workbench.
- Burdening Amulet with a tamed wolf; Death Gaze and the warp drain. Tight Belt: Thaumarhia, warp drain, flux gas.
- Both bottles thrown: fire put out; taint splash, fibres and flux gas.
- Shard Mill (0.3.2): loading essentia crystals into the Greatwood mill; the dust trails; contact effects; the
  cranking hand and spinning crank in first person, in third person (F5) and as seen by another player; positions
  are first guesses, so describe or screenshot what's off.
- Dump Jackboots: climb speed, and that releasing sneak mid-climb never leaves a gap; also on a LAN client; Burdening Amulet with a pet under Death Gaze; bottle splash particles.

See `../../../docs/design/concilium-fidelity-review.md` for every deviation from the original awaiting a decision.

## Phase 4: Thaumaturges, Mad Thaumaturges, the corpses (0.5.0)

| Area | Status | Notes |
|---|---|---|
| Thaumaturge | ✅ | `entity/mob/ThaumaturgeEntity`: 20 HP, 6 attack, 0.4 speed, +2 armour, heals 1 per 2 s. Fire wand (five fire bolts through Thaumaturge's own `projectile → fire` spell); hunts cultists, zombies, skeletons, eldritch and tainted mobs. Hit one: every Thaumaturge within 32 is angry for 20-40 s. Taunts until the Thaumaturges research is done; says it has nothing left to surprise you with once all three traded hints are known |
| Trading | ✅ | `ThaumaturgeMenu` + `ThaumaturgeScreen` (Pech layout; the background is Thaumaturge's `gui_pech.png`, used from its jar). `ThaumaturgeTrades`: the original values and weighted table, 1:1 |
| Mad Thaumaturge | ✅ | Void hoe or shovel; hunts players, Thaumaturges, cultists; leaps onto whoever hurt it and claws; drops hint notes (20%) or Curios of Knowledge. Replaces 20% of Brainy Zombies (config) |
| Research hints | ✅ | `research_hint` with a `thaumicconcilium:hint` component; an `obtain` requirement on the first stage of Reflection, Thick Taint, Runic Windings, Positive Burst and Impulse (consumed). Original tooltips, including "these lunatics" once all Mad hints are known |
| Nether Explorer | ✅ | Floats on lava and water (the original's boat physics); touch or hit: Nether Membrane + the Nether Membrane research. Spawner: near a player in the Nether, on the lava sea, while fewer than 4 are loaded |
| Nether Membrane | ✅ | Special crafting recipe: membrane + one item → that item with vanilla `fire_resistant` (plus a "Nether Protection" tooltip) |
| Withered Botanist | ✅ | Only with Botania. Touch or hit: 3-4 random mystical flowers bloom; it blows away |
| Smart taint spider | ✅ | `SmartTaintSpiders`: a tainted spider without self-targeting or drops; Taint Animation's command mode and Thought Manifestation make them (phase 2 used a taint crawler) |
| Research | ✅ | Thaumaturges (bought), Mad Thaumaturges (scan one), The Price of Scientific Advances (the bestiary: scan a Nether Explorer or Botanist; the Botanist page appears once one is scanned), Nether Membrane |
| Drums | ⬜ | Moved to phase 6: the Thaumaturge's Drum hangs off the Vis Conductor entry and the Warped Drum is a rift recipe |

### Mapping decisions (phase 4)

- **Spawning** (agreed 2026-11-21: Thaumaturges a welcome, uncommon sight, not a constant presence): the original's
  creature spawn (weight 5, groups of 1-3) only takes as the chunk of an overworld surface structure in
  `#thaumicconcilium:thaumaturge_haunts` generates (villages, temples, witch huts, igloos, ruined portals, Thaumaturge's
  mounds and obelisks, Villages & Pillages' witch village), and those Thaumaturges stay. The periodic creature
  respawns never bring them. On top of that, rare wanderers: checked once a minute, 1.5% (config), one or two near a
  random player, gone after one or two days.
- Mad Thaumaturges and Withered Botanists spawn in every biome, as in the original (the Botanist's own rule keeps it in
  the overworld). Mad Thaumaturges carry a void hoe or shovel, as in the original code (not an axe).
- Knowledge fragment → Thaumaturge's Curio of Knowledge; TC4 shards → essentia crystals; crystal clusters → the six
  primal clusters (Thaumaturge has no balanced cluster); the fire focus → a focus inscribed `projectile → fire`.
- Hints for entries that don't exist yet (Vis Conductor, Destabilized Crystal: phase 6) are skipped: the Mad roll drops
  nothing, the trade falls back to Positive Burst or Impulse.
- Not ported: the Mad Thaumaturge's crimson-dagger conversion (needs Tainted Magic, not in the pack).

### Needs in-game testing

The FTB Quests chapter **Thaumaturges and the fallen** (`qa_thaumaturges`) walks through each check. Not covered by the
pack test: the trade screen itself, the fire bolts and anger in play, the Mad Thaumaturge leaping onto a player, lava
floating, flowers blooming, natural spawns near structures, wanderers, and the bestiary appearing on scans.

## Phase 6a: the rift workshop (0.6.0)

| Area | Status | Notes |
|---|---|---|
| Spatial rifts | ✅ | `rift/ConciliumRifts`: Thaumaturge flux rifts tagged by Concilium. Breaking an aura node opens one (config `chanceOfRiftOpening`, 100); the original's growth, event table and collapse taint, on top of Thaumaturge's sweep |
| Taming native rifts | ✅ | Port addition (to be revisited, see the scope doc): a lesser hex (ancient stone around a Barrier Stone) pins a native flux rift very stable (no events, growth or taint), reins it to size 25, transmutes at double essentia |
| Hex of Predictability | ✅ | `rift/Hexes`, `block/Hex*`: bedrock + 8 ancient stone, wand trigger; jarred node -> chained rift; heat, void slag, transmutation; OBJ rings with a tinted ink layer (`client/HexRenderer`) |
| Rift recipes | ✅ | `recipe/RiftRecipe` (`thaumicconcilium:rift`). None yet: the first (Astral Monitor) arrives in 6b; the pack test uses a test recipe |
| Vis Conductor | ✅ | `spell/effect/VisConductorEffect`: drain a crystal, burst it, Dematerialization (creatures, auram into rifts), Wisp Launcher -> `entity/ChargedWisp` |
| Destabilized Crystal, Vis Condenser | ✅ | `block/DestabilizedCrystal*`, `block/VisCondenserBlock` (original OBJ) |
| Primal orb | ✅ | The port's own (`entity/PrimalOrb`) for the rift's orb event |
| Research | ✅ | Vis Conductor and Destabilized Crystal (hint-gated), Rifts (scan a flux rift), Dematerialization, Vis Condenser, Hex; Cooling Focus and Dump Jackboots back under the crystal |

QA commands: `/thaumicconcilium qa hex <pos> full|chained|lesser`, `qa rift <pos> <size>`, `qa crystal <pos> <aspect> <amount>`.
QA chapter: **The rift workshop** (`qa_rift_workshop`). Deviations: the fidelity review, "Phase 6a".

## Remaining phases (planned)

Dependencies mirror the original's (`required-after: Thaumcraft, ForbiddenMagic, ThaumicTinkerer`):
Thaumaturge, **Thaumaturge Tinker** (replaces our planned Tinkerer module) and a **Forbidden Magic** mod built from
Forbidden Magic Reforged (MIT) with a new Thaumaturge host adapter, which now holds the sin aspects in its
`forbiddenmagic` namespace.

| Phase | Content |
|---|---|
| FM | ✅ (0.2.1) Forbidden Magic (Thaumaturge) built; sin aspects moved; shard stand-ins swapped for real shards |
| 3 | ✅ (0.3.0) Gear, warp baubles, bottles, shard mills. 3b: casting bracelets |
| 4 | ✅ (0.5.0) Thaumaturges, Mad Thaumaturges, Nether Explorer, Withered Botanist, hint notes, Nether Membrane |
| 5 | Crimson forces: paladin, archer, ranger, lesser portal, Pontifex boss |
| 6 | Rifts, Hex of Predictability, machines (Destabilized Crystal), Vis Conductor focus, Thaumaturge's and Warped Drums, Brightest One, Gem of Rifts, Causal Bouillon dimension |
| 7 | Golem cores → seals, Golem Bydlo, Vengeful Golem |
| 8 | Horizons flesh alchemy, Gadomancy gadgets, Thaumic Bases picks |
| 10 | Neo Vitae and Enchanted integrations |

Phase 9 (a Tinkerer KAMI module) is dropped: Thaumaturge Tinker already ports KAMI.
