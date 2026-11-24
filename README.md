# Thaumic Concilium (Thaumaturge port)

An unofficial port of Ilya3000's **Thaumic Concilium** (Forge 1.7.10, WTFPL) to **NeoForge 1.21.1**, rebuilt on
**Thaumaturge** 1.1 instead of Thaumcraft 4. It also re-homes content from the mods Concilium leaned on
(Forbidden Magic, Thaumic Horizons, Gadomancy and others); see `docs/PORT_STATUS.md`.

## Requirements

Thaumaturge 1.1+, Curios 9.5+ and **Thaumaturge Tinker** 1.1.3+ (the 1.21.1 counterpart of Thaumic Tinkerer, which
the original also required), **Forbidden Magic (Thaumaturge)**, Thaumic Horizons and **Armor Series - Crimson Cult** 1.2.1+
(Enderghast; our crimson mobs wear its armour, with GeckoLib). Thaumic Additions CE is optional.

## Building

1. Copy `thaumaturge-1.21.1-NeoForge-1.1.0.jar` and `curios-neoforge-9.5.1+1.21.1.jar` into `libs/`
   (see `libs/README.md`). They are compile-only and must never be committed.
2. Run `gradle build` (or open the folder in IntelliJ and run the `build` task). The jar lands in `build/libs/`.

Requires Java 21. The Gradle setup matches Aspect Interpreter's (ModDevGradle 2.0.136, NeoForge 21.1.265).

## How the port maps onto Thaumaturge

- **Foci become spell parts.** Each original focus is a signature part (an effect or a delivery), and its upgrades
  become settings on that part. TC4's generic Frugal/Potency/Enlarge/Extend upgrades become Thaumaturge's own
  Efficient/Amplify/Widen/Extend modifiers. Research unlocks the parts. An infusion recipe (the original's, translated)
  also makes a ready-inscribed focus that behaves like the original out of the box.
- **Sin aspects, shards and the crystalwell** come from **Forbidden Magic (Thaumaturge)**, a required dependency:
  `forbiddenmagic:ira`, `gula`, `invidia`, `superbia`, `desidia`, `luxuria`. Greed is Thaumaturge's desiderium.
  Infernum comes from Thaumic Additions CE when installed; otherwise Forbidden Magic supplies a fallback.
- Baubles become Curios, golem cores become seals, and MineTweaker support becomes datapacks plus KubeJS.

## Content

Foci (signal, impulse, positive burst, cooling, reflection, scribing, wrath attraction, taint animation, ethereal
manipulator, spotlight, alchemist's spree, warp extraction, Vis Conductor), the Ring of Blustering Light, gear and
baubles, casting bracelets, the Thaumaturges and other mobs, the rift workshop (spatial rifts, Hex of Predictability,
Destabilized Crystal, Vis Condenser) and, new in 0.7.0: the Astral Monitor and Lithographer (with Xylography and Coagulating
Eye Drops), the Quicksilver Elemental and Reactor, Thaumaturge's and the Warped Drum, and the **polished infusions**: twelve
items (Cooling, Reflection, Scribing, Wrath Attraction, Taint Animation, Ethereal Manipulator, Spotlight and Warp Extraction
foci, the Ring of Blustering Light, Dump Jackboots, Tight Belt, Burdening Amulet) only come out of an infusion if you draw
the listed aspect from a Destabilized Crystal with the Vis Conductor as it completes. The list is a datapack registry
(`data/<namespace>/thaumicconcilium/polishment/*.json`).

New in 0.17.0: the **Protobody** (infused from a Thaumic Horizons Corpse Effigy), which shatters into six **Protolimbs** when
dropped far below the world floor; absorbing all six teaches the **Solid Void** research, and a wand raised in the void
gathers the block (the end portal's starfield, drawn with the vanilla end portal render type).

## Licence

Port code: MIT. Original Concilium and Forbidden Magic material: WTFPL. Credits are in `CREDITS.md`.
