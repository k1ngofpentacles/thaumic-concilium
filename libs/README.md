Put the jars this mod compiles against here. They are compile-only and gitignored; never commit them.

Required:
- `thaumaturge-1.21.1-NeoForge-1.1.0.jar` (All Rights Reserved)
- `curios-neoforge-9.5.1+1.21.1.jar`

Optional integrations (compiled against, soft-loaded at runtime):
- Neo Vitae (`neovitae-1.21.1-1.1.54.jar`: the Crimson Dagger's self-cut) and, to compile against its altar class, `geckolib-neoforge-1.21.1-4.9.3.jar`
- Enchanted: Witchcraft (1.21.1)
- Thaumic Additions CE (only for its `infernum` aspect; we fall back to our own when it is absent)
- Sable (phase 5c, the Vessel's pull on ships; PolyForm Shield, compiled against, never shipped, soft-loaded through `compat/SablePull`):
  `sable-neoforge-1.21.1-2.0.6.jar` from the pack, and `sable-companion-common-1.21.1-1.6.0.jar` (its `Pose3d` and `BoundingBox3d`), which is
  inside Sable's jar at `META-INF/jarjar/`: `unzip -j sable-neoforge-1.21.1-2.0.6.jar "META-INF/jarjar/sable-companion-common-*.jar"`. The pack
  test's build also reads the companion jar from here.
- Botania Revived 2.0.4 (`Botania-Revived-1.21.1-2.0.4-NeoForge.jar`: mana API for the Botania bracelets)

All of them are already in the moving castle pack's `mods` folder: copy them across.
