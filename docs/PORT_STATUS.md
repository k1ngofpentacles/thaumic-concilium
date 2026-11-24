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

Every manual check is a QA quest in the FTB Quests book: the checks in **Foci ported 1:1** (`qa_foci_restored`, check "Foundation foci"). Nothing is listed here.

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

Every manual check is a QA quest in the FTB Quests book: **Foci ported 1:1** (`qa_foci_restored`: ring, Ethereal Manipulator, Scribing, Taint Animation, Warp Extraction). Nothing is listed here.

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
| Polishment (and the Polishment Device) | ✅ 0.7.0 | The gate on the infusion and the creative-only device: see "Phase 6b". Protobody waits for phase 8 |
| Coagulating Eye Drops, Astral Monitor | ✅ 0.7.0 | See "Phase 6b" |
| Thaumaturge and Warped Drums | ✅ 0.7.0, off hand 0.7.3 | See "Phase 6b". 0.7.3: the drum plays from either hand (main hand empty or a casting bracelet); the drum stance (main-hand bracelet + off-hand drum) casts with V on plain wand terms; you cannot aim while drumming, so only spells whose root delivery is an area around the caster or the caster are cast (`drum_castable`: aura, self, cloud; effect-only spells too), refused if an effect reads the look (`drum_aimed_effects`: alchemist_spree, ethereal_manipulator, vis_conductor, warp_extraction). While the drum is played the cast's area radius and vis cost are x1.5 (config `drum.drumRadiusMultiplier`, `drum.drumVisCostMultiplier`; `spell/DrumStance`, no mixin). Taint Animation is castable and unaimed in the stance; Positive Burst (0.7.4) releases its orbs straight up there. Ready-made foci: Reflection, Wrath Attraction, Taint Animation, Positive Burst. Left-handed mirroring untested |

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
- Shard Mill: the shard is chosen with sneak + mouse wheel (the original used a radial menu). The balanced shard is Thaumic
  Horizons' `thaumichorizons:balanced_shard` (tag `c:balanced_shards`), a Greatwood mill choice whose powder does nothing, as
  in the original (0.15.1); the gluttony shard (its powder did nothing) is left out. The Aura Core infusion takes four
  balanced shards (the tag), not essentia crystals (0.15.1). Pride powder keeps the
  original's intent, flinging creatures away; the original's formula aimed them at the world origin.
- Strong original numbers are kept for now and worth judging in play: Runic Windings' 200 charge each, entropy powder's
  Wither 51, wrath powder's 50000-second burn, pride and greed powders' speed of 3.

### Needs in-game testing

Every manual check is a QA quest in the FTB Quests book: **Powders and the Burdening Amulet** (`qa_phase3_restorations`: art, mill cranking, windings and the strong numbers, belt, bottles, jackboots, amulet, powders) and **Casting Bracelets** (`qa_bracelets`). Nothing is listed here.

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
| Rift recipes | ✅ | `recipe/RiftRecipe` (`thaumicconcilium:rift`). Astral Monitor and Warped Drum use it (6b); the pack test also has a test recipe |
| Vis Conductor | ✅ | `spell/effect/VisConductorEffect`: drain a crystal, burst it, Dematerialization (creatures, auram into rifts), Wisp Launcher -> `entity/ChargedWisp` |
| Destabilized Crystal, Vis Condenser | ✅ | `block/DestabilizedCrystal*`, `block/VisCondenserBlock` (original OBJ) |
| Primal orb | ✅ | The port's own (`entity/PrimalOrb`) for the rift's orb event |
| Research | ✅ | Vis Conductor and Destabilized Crystal (hint-gated), Rifts (scan a flux rift), Dematerialization, Vis Condenser, Hex; Cooling Focus and Dump Jackboots back under the crystal |

QA commands: `/thaumicconcilium qa hex <pos> full|chained|lesser`, `qa rift <pos> <size>`, `qa crystal <pos> <aspect> <amount>`.
QA chapter: **The rift workshop** (`qa_rift_workshop`). Deviations: the fidelity review, "Phase 6a".

## Phase 6b: monitors and machines (0.7.0)

| Area | Status | Notes |
|---|---|---|
| Polished infusions | ✅ | `polishment/Polishment` (synced datapack registry, twelve JSON files from `gen_phase6b_data.py`) and `PolishmentGate` on `InfusionCraftedEvent`: the crafter must be channelling the Vis Conductor at a crystal of the entry's aspect with the listed draw drawn, else the result is lost. Book line and item tooltip state the requirement |
| Polishment Device | ✅ | Creative-only; right-click a rift to remove it |
| Xylography | ✅ | A lone wand in the grid is signed with the crafter's name |
| Astral Monitor | ✅ | Rift recipe from a thaumometer; sneak-use opens the wand slot; constellation glyphs of the signed player's health, mana (Botania) and runes |
| Lithographer | ✅ | Built with a wand from the 3x3 pattern; four monitors, redstone pulse, Vis Condenser and a sensus crystal above, a sleeper in the bed; lithograph HUD |
| Coagulating Eye Drops | ✅ | Toggle the HUD; 1 damage |
| Quicksilver Elemental and Reactor | ✅ | Spawns in the overworld; burnt in a crucible it makes the reactor; the reactor merges a crucible's last two aspects |
| Thaumaturge's Drum, Warped Drum | ✅ | Wisps of random primals / harmless mind spiders while played; from the off hand too (0.7.3); drum stance casting (`bracelet_drum_stance`, `bracelet_drum_taint`, `drum_offhand`) |
| Research | ✅ | Thaumdrum, Warpdrum, Xylography, Quicksilver Crucible, Astral Monitor, Lithographer |

Deviations: the fidelity review, "Phase 6b". QA chapter: **Monitors and machines (6b)** (`qa_phase_6b`). Pack test:
`Phase6bTests` (lookup, gate, reactor merge, xylography, Lithographer build); the build, the research audit (0 errors)
and all 30 GameTests pass.

### Needs in-game testing

Every manual check is a QA quest in the FTB Quests book: **Monitors and machines (6b)** (`qa_phase_6b`: polished infusions, Xylography, the monitor, Lithographer and HUD, elemental, reactor, Polishment Device, the drums and the drum stance) and **The rift workshop** (`qa_rift_workshop`: primal orb). Nothing is listed here.

## Phase 6c: aura cores, gems, subspace and the Brightest One (0.8.0)

| Area | Status | Notes |
|---|---|---|
| Aura cores | ✅ | Gadomancy's cores: blank core by infusion from a primal charm; attuned by tossing it beside a primal crystal cluster and striking the cluster with a charged wand (scan of 9 x 9 x 9 around the core, particles and sound); six cleaning crucible recipes. Earth and entropy cores are usable |
| Gem of Rifts, Terra-cast Gem | ✅ | Held use opens an interdimensional rift (no rift within 16 blocks; perditio 10 + aer 10, terra 10 + aer 10 for the terra gem); the terra gem absorbs a block with terra and binds a dimension; crafting adds a cast to the Rift Gem (16 at most). Held rendering in first person (both hands), third person, GUI, ground and frame |
| Interdimensional rift | ✅ | Grows 5 to 50, teleports a player holding a gem, collapses by shrinking; no taint, no warp |
| Causal Bouillon, Void Reality | ✅ | Flat void dimension with a Void Reality floor, fixed noon, no beds; crossing keeps x and z in, scales x10 or /10 out, safe landing, Nether pocket exit |
| The Brightest One | ✅ | Summoned by a charged wisp (Wisp Launcher) from a researched player into a chained rift; immune to damage, short-circuited by charged wisps (1 of 15 health each); four attack sets by aspect; pink boss bar and darkened sky; despawns on peaceful |
| Primordial Life | ✅ | Now obtainable: the Brightest One drops it once |
| Solid Void | moved | To phase 8 (needs the Protobody and six protolimbs) |
| Burning Site | moved | To phase 10 (the Witchery re-imagining) |

Deviations: the fidelity review, "Phase 6c". Added a `wisp_launcher` focus preset (`/thaumicconcilium qa focus wisp_launcher`) for testing. QA chapter: **The Brightest One and subspace (6c)** (`qa_phase_6c`). Pack test: `Phase6cTests`, `Phase6cGemTests`, `Phase6cBossTests`.

### Needs in-game testing

Every manual check is a QA quest in the FTB Quests book: **The Brightest One and subspace (6c)** (`qa_phase_6c`). Nothing is listed here.

## Phase 5a: Crimson forces (0.9.0)

| Area | Status | Notes |
|---|---|---|
| Primal Arrows | ✅ | Six arrows (aer, ignis, aqua, terra, ordo, perditio) as real arrows: bows, crossbows, enchantments and dispensers fire them; per-type effects and multipliers from Thaumcraft's; arcane recipe (8 arrows around a primal essentia crystal, 8 vis); research `primal_arrows` |
| Crimson Paladin, Archer, Ranger | ✅ | Wear Thaumaturge's crimson armour plus our own overlays (Archer visor and quiver, Ranger spikes and cuffs); 50% of Paladins wear the **Gilded Crimson Helm** (our item, droppable, wearable). Crimson Orb, Ethereal Shackles (no visual yet), shard spray. 20% of Thaumaturge's Crimson Knight spawns become Paladins; Rangers spawn naturally. Archer damage as the original, scaled by `mobs.crimsonArcherDamageMultiplier` |
| Lesser Crimson Portal | ✅ | Staged cycle and minion waves; opens linearly over 50 ticks to 2.6 x 3 blocks; Thaumaturge's red portal texture (the original's was its host's, violet) |
| Crimson Pontifex | ✅ | Summoned by throwing Crimson Rites into a chained rift (with the research): the rift collapses into a boss portal. Boss bar and darkened sky, life drain, damage split by nearby cultists, enslave walk, loot bag and note drops |
| Crimson Annales | ✅ | Unlocks `crimson_annales` after Crimson Rites; in chests at the original's effective chances (11-44%) and the rare loot bag |
| Pontifex Hammer, Pontifex Robe | ✅ | Pulled forward from 5b. Hammer life steal and held render; dyeable four-piece robe with the original model and art, toggle L (minimal ethereal state), cultists ignore a full-set wearer; research for both and Crimson Initiation (text only) |
| Arcane Levitator | ✅ | Our own block (Thaumcraft's lifter mechanics, original code and art): lifts the column above it, 10 blocks per stacked levitator, redstone off, sneak damps falls; cached column |
| Cultist tower | ✅ | The Crimson Avanpost: original layout with mapped blocks, about 1 per 289 chunks on dirt, grass, sand or stone, armed archer guards, chest loot |
| Material Peeler, flesh economy, Crimson Raid, Shackles visual | 5b | 70% of Pontifex note drops wait for the Peeler |

Deviations: the fidelity review, "Phase 5a". Pack test: `Phase5a*Tests` (batches `phase5a_*`); the Brightest One tests now poll and run in their own batches.

### Needs in-game testing

Every manual check is a QA quest in the FTB Quests book: **Crimson Forces** (`qa_phase_5a`). Nothing is listed here.

## Phase 5b: Peeler, flesh and the Crimson Dagger (0.10.0)

| Area | Status | Notes |
|---|---|---|
| Player state, ethereal, flesh | built | One attachment `crimson_state`; no-clip, damage and healing cancelled (void too, by decision), flesh as reserved max health; replaces 5a's minimal state |
| Material Peeler | built | Entity summoned by Primordial Life thrown into a chained rift; bursts without a Hex below; touch flips ethereal; whiter rim (option B) over a vanilla end-portal core |
| Flesh Crucible | built | Sip every tick, smelt leaves a Quicksilver Reactor, burns others every 10 ticks; recipe needs 3 plating + 3 motes, so uncraftable until 5c. Open: nothing in play adds aspect to a fresh crucible (the smelt's +2 goes with the replaced block) |
| Crimson Initiation, Crimson Raid | built | Our Crimson Dagger (tag `initiation_tools`) converts a Mad Thaumaturge in the full robe; Raid spell part, complexity 22, 400-tick owned portal |
| Ethereal Shackles visual | built | Projectile and first-person chained hand from the original's art |
| Crimson Dagger | built | Infusion (Rites centre), 15-tick raise, stun by tier, Bleed (3 counters), self-cut with Neo Vitae, Enchanted sacrifice damage, Crimson Blood drop |
| Crimson Plating, Primordial Mote | built | Plating: the Gilded Cult Plate (2 blood + 4 gold nuggets around an iron block, 9 out). Mote is an item with no source until The Vessel (5c) |
| The Offertory (infusion minigame) | built as D1-c | Binding chain, flesh-offering demands, drain and snap; the ritual never kills the binder |
| Cult aspects | built | Real `base_aspects` for 8 Thaumaturge ids and our new items; Aspect Interpreter 1.8.0 (separate jar) |
| D1-c2: Yank, snap completes the rite and opens a Crimson Raid, Paladin champion, human-flesh offerings | pending merge | User's revision of the Offertory; replaces the offering list and the snap |
| D8: dagger animation | pending merge | Fable; option still to be chosen |

Deviations: the fidelity review, "Phase 5b". The decision record is `docs/design/5b-decision-tree.md`. Pack test: `Phase5bStateTests`, `Phase5bPeelerTests`, `Phase5bCrucibleTests`, `Phase5bCrimsonTests`, `Phase5bTrunkTests`, `Phase5bMinigameTests`. Known pre-existing gap: with Thaumaturge's aspect cache cleared, `items_have_aspects` fails on `polishment_device`.

### Needs in-game testing

Every manual check is a QA quest in the FTB Quests book: **Flesh, Peeler and the Crimson Dagger (5b)** (`qa_phase_5b`). Nothing is listed here.

## Phase 5c: The Vessel, pure half (0.11.0)

The Vessel is our own design (no original to port); the Brightest One is its sibling. Design canon: `docs/design/5c-design.md`; cycle notes in `docs/agents/retro/5c-cycle*-notes.md`.

| Area | Status | Notes |
|---|---|---|
| The sink | built | Only a **lesser hex** (Barrier Stone centre) holding a **tamed** rift feeds. Burden routes: F1 aura flux, F2 vitium through condensers, F3 goo and gas within 4 blocks, W1 sneak-use warp (temporary, then normal, never permanent), W2 thrown warp items (destroyed). Rift grows 25 -> 75 with burden and **eats no blocks**; three crack states on the hex, a hidden strain threshold (400, +-15%). Break on the next chunk load once strained, or leak (150% back as flux) and break when all players sleep. Chained rifts (the Brightest One's) and full hexes are exactly as before |
| Break cinematic | built | About 6.5 s: the hex becomes a **Sable sub-level** and rises, spins inside the Vessel, cracks with light, bursts; the four edge stones fly out as shards and land as anchors. Without Sable (or `vessel.break.sable = false`) a rendered stand-in plays the same timeline. Interrupted sequences finish at once on load; the Vessel cannot be woken until it ends. The hex is permanently broken (nothing restored) |
| Crater | built | Shallow bowl (about 4 deep, tapering), ragged rim, debris-block mound; carved from the centre outward by a queue over about 3 s; blast-resistant blocks stay, containers spill; the burst damages and throws players and mobs inside (explosion damage). Anchors land on the finished floor on radial spokes (roughly a circle, 8-12 out) |
| Anchor shards | built | Eight shapes, four picked per Vessel; textured from the hex's own stone, glyphs rise as particles (charge reads through them), a hovering shard bobs and turns; states `charges`, `tethered`, cooling. They crumble when the rite ends |
| The fight | built | Entity with a Mass bar; pull and horizon; draws at the anchors with cooldown; clots batted back as the stagger; Feeding, Spin-up and Collapse; dormancy when abandoned; weapons never hurt it. Mass 0 subdues. Darkness inside about 8 blocks of the hole, Blindness inside about 4 |
| Sable pull | built | Airships near the hole get impulses toward it (`compat/SablePull`, the only class naming Sable); capped impulse, approach speed and angular speed; one query per 20 ticks while active |
| The look | built | Black core, violet ring, haze, halo, clots, tether ribbons, vignette, sounds; harness-measured against the design numbers |
| The rite | built | Subdued Vessel, 20 charge units drained by held wand tethers at the anchors (resumable, per-anchor progress saved); 1 temporary warp per unit and one normal per 5 units; taint pulses on a widening ring; the Vessel is a living taint origin while draining |
| Taint Seed scar | built | Left at the end, sized by missed clots and spilled goo (Prime from score 12); the blight withers when it dies (Thaumaturge's rule, not tested) |
| Primordial Motes | built | To every participant at the subdue, second mote 10% each; absent contributors' share is paid on login. Makes the Flesh Crucible craftable |
| Research | built | Our tab: Rifts That Hold, What It Keeps, The Vessel, Drawing It Out (gated with `thaumaturge:unlock_eldritch`, permanent warp 3), Primordial Mote; notes granted by code |
| Absent-contributor debt | built | Level saved data keyed by UUID: temporary warp and a warp event (`rift/WarpKick`, by reflection, fails soft) on next login; teleport of contributors from any dimension |

Configurable numbers that are **first guesses** (all `TCConfig`, groups `vessel.*`): every burden rate and the threshold and its spread, the leak ratio and sleep threshold, growth 25 -> 75, the pull, horizon, Mass, clot and gulp numbers, the Sable caps (impulse 150, 4 blocks/s, angular 1.0 rad/s), Darkness and Blindness radii, crater size and blast damage and knockback (24, 1.6), the rite's charge (20 units), unit time (1200 ticks, about 20 min solo), pulse ring and goo rates, seed score and strength, and the second-mote chance. The QA chapter judges them.

Mixin `RiftSweepMixin` (`defaultRequire: 1`): one cancellable inject at the head of Thaumaturge's `RiftSegmentSweep.tick`, skipping the sweep for a tamed rift under a lesser hex. Justification: the sweep is called straight from the rift's own tick with no event, config or state to flip, and cancelling `EntityTickEvent.Pre` would also drop the collapse and outline refresh. Wild, chained, Gem and spatial rifts are unchanged (control test `wild_rift_still_eats`). Other mixins: `CrucibleAccessor`, `InfusionMatrixMixin`.

Deviations: the fidelity review, "Phase 5c". Pack test: `Phase5cSinkTests`, `Phase5cVesselTests`, `Phase5cSableTests`, `Phase5cShardTests`, `Phase5cBreakTests`, `Phase5cRiteTests` (214 GameTests in all, 0 failures; `pontifex_attack_rolls` is a known flake). Render harness: `phase5c.json`, `phase5c_break.json`.

### Needs in-game testing

Every manual check is a QA quest in the FTB Quests book: **The Vessel (5c)** (`qa_phase_5c`). Nothing is listed here. Not covered by any test or shot: the real player teleport and the break with real players, the feel of the pull, the 6.5 s sequence and the blast throw, bob and glyph motion, blight withering, the Sable twist and aboard rules, shaders, and multiplayer over LAN.

## Phase 5d: The Vessel, Botania half (0.12.0)

Our own design (no original to port); everything is gated on Botania (and Dynamic Trees for the trees), so without them the Vessel ends by the pure 5c rite. Design canon: `docs/design/5d-design.md` (with `5d-look.md`, `5d-pond-look.md`, `5d-finale-growth.md`, `5d-capital-breath.md`); cycle notes in `docs/agents/retro/5d-cycle*-notes.md`. Last full pack test: 301 of 301 GameTests, two runs. Phase 5d is **done and not tested in game**.

| Area | Status | Notes |
|---|---|---|
| Verdant Capital | built | Infused from terrasteel and a rune; placed on an anchor shard, a 10k Botania mana receiver and spark attachable. A capped anchor drains by mana alone (no drawer); starved, it pauses, then after a grace (6000 ticks) unwinds and refunds a unit. The capital count (0-4) sets the ending. The model is a star of two counter-rotating triforces (8 small tetrahedra) around a translucent sphere, with the mana beam through its centre; the whole look is `CapitalMotion`/`CapitalStar` (breath, anger, panic, relief, crossing choreography from a generated loop table) |
| Taint at every tier | built | The 5c pulses run in full whatever the capitals. Capitals instead raise the chance that strangling roots (variant C) kill a Taint Seed: 0/25/50/75/100% for 0-4 capitals, in the fight and at the ending |
| Endings | built | Tier 0 accelerated seed with children, tier 1 bound seed held at the rim, tier 2 starving seed, tiers 3-4 no seed at the centre (grove, pond, mound). Second mote chance by tier; tier 4 removes permanent warp (3 solo, 1 each with a Heartstone partner). Offline participants are paid on login |
| The finale | built | Every ending: the dying Vessel swells to fill the crater and blinds those inside while the painter paints it at 3x pace (tiers 0-2: 8 s dark, no light; tiers 3-4: dark until the crater is done, then a shadowy light stage with a wisp rising from the pond, a burst, and a white flash outside) |
| Concilium Grove | built | Biome `thaumicconcilium:concilium_grove` (magical forest plus ours), painted ring by ring from the hex by a queue on the server thread (4 ms budget, never force-loads); radius 24-48 by burden at tier 4, rim + 2 at tier 3. Two node mixins and a taint-restore mixin keep the grove from being tainted back by the vanilla rules |
| Grove life | built | Botania flowers, double flowers, shade mushrooms, meadow flowers, grass and fern; Thaumaturge's own flora and mana pods; nodes (15% a chunk, at most one); grove trees grown on screen: 8 greatwood and 10 silverwood landmark shapes (Dynamic Trees JoCodes) in stages, growth slowing exponentially from the centre, and Dynamic Trees saplings by accelerated pulses |
| Grove growth boost | built | +25% extra growth step for crops (`CropGrowEvent.Post`) and trees (two optional mixins), grove columns only |
| Mana pond | built | Our fluid, 13 still source blocks in a rough round pool that reads the crater floor, in a livingrock mound with mossy and cracked variants; animated chart texture on the standard fluid path. Botania pools and spreaders draw from it; a ledger (10k a painted chunk, 50k a node) then drains grove node vis, and at last breaks the weakest node into a rift of 25 |
| Research and lexicon | built | *The Green Translation* (hidden, Botania) and *The Glade* (granted by code to every tier 3-4 participant); one entry in Botania's lexicon pointing at our tab |

Mixins added: `NodeDriftMixin`, `NodePurifyMixin`, `TaintRestoreMixin` (all `defaultRequire: 1`, justified in their javadoc) and `SoilGrowthBoostMixin`, `SaplingGrowthBoostMixin` (`thaumicconcilium.growth.mixins.json`, optional). `heartstone` and `dynamictrees` are optional dependencies.

Configurable numbers that are **first guesses** (`TCConfig`, `vessel.rite.*`, `grove.*`, `growth.*`): mana per unit (250000), capital buffer (10000) and starve grace, the root chances, the grove radius and burden span, the flora densities, the pond ledger (10k, 50k, 1000 mana a vis, overdraw step 50k), the finale timings, the growth falloff and apex pulses (500, a choice not a measurement) and the boost (25%).

Known unverified: the sapling pulse count, Voxy LOD of grown leaves, Sable Trees, seasons, roots on a seed in a real fight (tested with fake seeds), several sequences overlapping, the violet rim of the black orb (very faint), the cinematic path against the final capital and landmark positions.

Deviations: the fidelity review, "Phase 5d". Pack test: `Phase5dCapitalTests`, `Phase5dEndingsTests`, `Phase5dPainterTests`, `Phase5dFloraTests`, `Phase5dLandmarkTests`, `Phase5dPondTests`, `Phase5dGladeTests`, `Phase5dLookTests`, `Phase5dFinaleTests`, `Phase5dGrowthTests`, `Phase5dRootsTests`. `5c` rite tests moved to template `big` and an order-dependent flake (`shade_mushrooms_under_a_roof_and_nodes_at_most_one_per_chunk`, `unloaded_chunks_wait_...`) is known (`docs/design/5d-rite-flake.md`). Render harness: `phase5d_look.json`, `phase5d_star.json`, `phase5d_pond2.json`, `phase5d_pond3.json`, `phase5d_landmarks.json`, `phase5d_pool.json`, `phase5d_growth.json`, `phase5d_lexicon.json`.

### Needs in-game testing

The QA chapter `qa_phase_5d` (`tools/qa-quests/specs/phase_5d.json`).

## 5d follow-up: the grove's own trees on a hill (0.12.1)

The Concilium Grove grows trees of its own (`docs/design/5d-grove-trees.md`, every user decision; notes
`docs/agents/retro/5d-trees-a-notes.md` and `5d-trees-c-notes.md`; Fable consult `docs/consults/5d-grove-options.md`):

- **Species:** our own Dynamic Trees families and six species (heroes `elder_`, the finale's lineage `grove_`, regular grove trees
  `glade_`, greatwood and silverwood), on our template shapes (heroes 38-40 tall, grove trees 24-26), with Thaumaturge's wood, bark,
  textures and seeds. Natural growth follows each tree's template to its cap (`TemplateGrowthLogic`): heroes 40; finale trees on the
  hill, 26 beside the heroes and never under 19; regular grove trees 19. Self-seeded saplings keep the lineage; a seed a player picks up
  is a plain Thaumaturge seed; a Thaumaturge sapling that becomes a tree in a grove becomes a regular grove tree.
- **The hill:** tier 4 groves are 70 to 120 blocks across (by burden; config keys renamed `grove.radius.min/max`); always three heroes a
  few blocks past the crater's lip; after the finale the forest slopes from them (heroes 30, neighbours 26) to saplings at the edge and
  down toward the pond.
- **Growth on screen:** small steps (no tree pops in whole); the meadow ripples outward at a ring every 4 s and the trees follow on one
  clock from sight returning: a radius-70 forest stands about 2 minutes after sight. Trees no player can see grow in one step.
- **No decay** in a grove that is still growing or whose pond still gives mana (two optional Dynamic Trees mixins): no leaf death,
  twig rot or disease; felled trees still clear.
- **The pond** at tier 4 starts with 1,000 mana per point of burden (about 0.34-0.6 M), sized when it is placed.
- **Painter:** waiting chunks rotate (an unloaded one no longer blocks the rest); decor stays within the step budget. A one-time
  warm-up hitch (first node or hero of a session, 30-40 ms) remains.
- Executive calls (mechanical): our own families (signal depth 64); our leaves `light_requirement` 8 and `smother` 6 (fuller crowns
  than Thaumaturge's own); `soil_longevity` 32/40/64; finale trees within 4 of the pond, and the edge band, are saplings.

Pack test: 320 of 320 (two full runs at the close). New tests `Phase5dTreeTemplateTests`, `Phase5dGroveSpeciesTests`,
`Phase5dFinaleTreesTests`, `a_living_grove_keeps_its_leaves_and_twigs`. Render harness: `tools/render-harness/scenes/phase5d_trees.json`
(needs render distance 12). Known flakes (pass alone): `shade_mushrooms_...`, `tier4_flora_counts_...`, `sable_break_holds_the_hex_...`.

### Needs in-game testing

The QA chapter `qa_phase_5d`: checks `grove_trees`, `grove_ripple`, `grove_natural_growth`, `grove_lineage`, `grove_no_decay`,
`grove_species_look`, and the updated `grove_biome`, `pond_mana_draw`, `teaser`.

## Shield foci (0.13.0)

New content (the original had no shield foci). Design `docs/design/shield-foci-design.md`, look `docs/design/shield-foci-look.md`
(accepted by the user after round 4: runic amber wards; the spell parts keep their own steel and blue on purpose).

**Built:**
- Guard stance: a worn bracelet with a shield in the off hand casts only spells whose first-order deliveries are in
  `thaumicconcilium:guard_castable` (self, aura, cloud, touch, wall, riposte); `DrumCastable` generalised over the tag.
- Riposte (delivery): arms the caster; a blocked hit within `riposte.parryTicks` (10) of raising releases the spell at x1.5 on
  the attacker (projectile owner within `riposte.range` 8); terrasteel volley gives 10 charges; window 5-20 s; vis lost on expiry.
- Aegis (effect + mob effect): all-round and magic shield blocking at double wear, never fall or fire; shares through an aura.
- One `LivingShieldBlockEvent` listener (`spell/GuardEvents`), sync payload (`GuardSync`, `client/GuardFx*`), art and data from
  `tools/gen_shield_foci_data.py`; research `riposte_focus`, `aegis_focus`.
- Pack test 331 of 331; render scenes `shield_foci.json` (`sf_*`). Bash (concept D) is later, with the animation agent.

**Known caveats:** the real hurt path is untested (the GameTests post the block event directly); the first-person off-hand
render takeover and the third-person batch end are untested with Iris and other off-hand mods; during a bracelet cast
`BraceletFirstPerson` owns the off hand (no ward for those ticks); a first-person forced block from behind only brightens the band.

### Needs in-game testing

The QA chapter `qa_shield_foci` (spec `tools/qa-quests/specs/shield_foci.json`).

## Phase 7: Valet seal and Vengeful Golem (0.14.0)

Designs: `docs/design/phase7-design.md`; decisions in the scope doc ("Settled 2026-11-23"). The Assistant core and Golem
Bydlo moved to after the Horizons port; the Burlak core is not ported (unfinished in the original).

**Built:**
- Valet seal `thaumicconcilium:valet` (a Thaumaturge `SealType`, placer `seal_valet`): filter slot 0 the tool, slot 1 a
  renamed phrase item; armed (transient, 200 ticks) by the owner's exact chat phrase within 32 blocks of the seal (every
  matching seal fires) or by a renamed golem bell rung in the air or at a seal-less block; one click task per arming;
  idle cost nothing. Infusion `seal_valet`, research `valet_seal`, the original `valet_core` art, `/thaumicconcilium qa valet`.
- Vengeful Golem (`thaumicconcilium:vengeful_golem`): attributes, goals and the enslave/release ranged attack as the
  original, golem bell in hand, gnaw sounds, loot, our own model port and brain/jar layer, spawn egg, bestiary scan and
  addendum page. Spawns in the Overworld, Nether and End through the `configured_spawns` biome modifier (config weight and
  biome blacklist); Enchanted's earmuffs protect. Added to the `enslavers` tag.
- Pack test: batches `phase7_valet` (5) and `phase7_vengeful` (7); render scenes `phase7.json`.

**Known caveats:** nothing is run in game (a real golem walking to a seal, a real chat message, real spawning, a real
enslave); the brain/jar colours are approximate (our own art, the original's is All Rights Reserved); our bell is
Thaumaturge's silver bell model; the Valet GUI shows only Thaumaturge's generic seal title.

### Needs in-game testing

The QA chapter `qa_phase_7` (spec `tools/qa-quests/specs/phase_7.json`).

## Crimson outfits (0.16.0)

Our crimson mobs (Paladin, Archer, Ranger; also the tower guards, Offertory and binding raids, the Lesser Crimson Portal and
the world's knight-to-Paladin replacement, which all go through `finalizeSpawn`) wear an outfit picked by weight from
`data/thaumicconcilium/crimson_outfits/<mob>.json` (generated by `tools/gen_crimson_outfits.py`, in `gen_all.py`; loaded by
`world/CrimsonOutfits`, a reload listener). Half of each table's weight is the host's (Thaumaturge) crimson armour, half
**Armor Series - Crimson Cult** (Enderghast, `armor_series__crimson_cult_`, now a **required** dependency, `[1.2.1,)`; items
referenced by id only, never shipped). Host-spawned cultists and the Pontifex are untouched (`outfitTable()` is null for the
Pontifex). Weapons are unchanged; armour drop chance stays the host cultist's 0.05 per slot. Unknown item ids are skipped with
one log line per table. The Gilded Crimson Helm is one weighted Paladin outfit (the old fifty-fifty roll is gone).

| Mob | Host (50) | Armor Series (50) |
|---|---|---|
| Paladin | gilded helm + plate chest + robe legs + boots 25; praetor 25 | paladin helm + knight chest/legs 20; knight 15; praetor 15 |
| Archer | cleric robes 50 | crossbowman 50 |
| Ranger | ranger set 50 | ranger 25; madcap 25 |
| Cultist (default table, no concrete mob uses it yet) | cleric 25, knight plate 25 | robes 25, knight 25 |

Armor Series sets without boots leave the feet slot bare. Skins: the Archer now uses the host cultist skin by reference
(`thaumaturge:textures/entity/cultist.png`, like the Ranger); the Thaumcraft 4 edit `crimson_archer.png` is deleted. The
Paladin keeps its own original WTFPL skin (not named in the brief: say if it should go too). Overlays: the Archer's visor and
the Ranger's spikes and cuffs are removed (they clashed with worn helmets and shoulders); the Archer's quiver stays.
Also (asset cleanup follow-up): the Nether Membrane is the vanilla phantom membrane tinted 0xC8405A (`TCClient.itemColors`),
its model written by `gen_art_redraw.py`; the redrawn PNG is deleted.

Tests: `CrimsonOutfitTests` (tables resolve, 50/50 split, 40 spawns per mob each a whole table outfit with both sources seen,
host cultists and Pontifex untouched); `Phase5aMobTests` armour asserts now defer to it. Harness: `scenes/crimson_outfits.json`
(`co_paladin`, `co_archer`, `co_ranger`, `co_ranger_series`, `co_nether_membrane`): Armor Series gear draws on our mobs through
GeckoLib, both sources show on the host skin.

### Needs in-game testing

QA chapter `crimson_outfits` (spec `tools/qa-quests/specs/crimson_outfits.json`).

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
| 5 | ✅ 5a, 5b, 5c, 5d done (0.9.0, 0.10.0, 0.11.0, 0.12.0): Crimson forces, flesh and Peeler, The Vessel pure half, the Botania half and the grove |
| 6 | ✅ 6a, 6b, 6c done (0.6.0, 0.7.0, 0.8.0): Brightest One, Gem of Rifts, Causal Bouillon dimension |
| 7 | ✅ (0.14.0) Valet seal and Vengeful Golem. The Assistant core and Golem Bydlo moved to after the Horizons port; Burlak not ported |
| 8 | Thaumic Horizons is now its own LGPL mod, ported whole; Gadomancy its own LGPL mod, with the aura cores moving there; then the Assistant core and Golem Bydlo, Thaumic Bases picks, Solid Void |
| 10 | Neo Vitae and Enchanted integrations, the Burning Site |

Phase 9 (a Tinkerer KAMI module) is dropped: Thaumaturge Tinker already ports KAMI.

- Wisp essence (0.15.2): Taint Animation, Ring of Blustering Light, Thaumdrum and Ethereal Manipulator infusions (the original wisp-essence recipes) take Thaumic Horizons wisp essence when it is loaded (`<id>`), else the crystal stand-in (`<id>_no_horizons`); `tools/gen_wisp_variants.py`, research pages list both. Wrath Attraction keeps its blank-focus catalyst. Protobody not built.
