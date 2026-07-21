# Create: Labor Rush

An addon for **Create: Villager Labor** that speeds up seated workers by applying the "Work!" effect.

## Overview

Ever wished your villagers would work faster? This mod lets you give them a speed boost — use a **Lead** on a single worker, or ring a **Bell** to boost everyone nearby.

Workers under the "Work!" effect process items almost instantly:

| Phase | Normal | With "Work!" |
|-------|--------|-------------|
| Processing | 5–20 ticks | **1 tick** |
| Cooldown | 20 ticks | **5 ticks** |

## How To Use

### Lead command
Hold a **Lead** in your main hand and left-click a seated worker (villager, player, or Touhou Little Maid). They'll receive the "Work!" effect for 90 seconds.

### Bell ring
Right-click a **Bell** or **Desk Bell** to apply the "Work!" effect to all seated workers within a **16-block radius**.

## Features

- **All stations supported**: Saw, Press, Mixer, Millstone, Deployer — any block extending `WorkerSeatBlockEntity`
- **Touhou Little Maid compatibility**: Maids can receive the "Work!" effect too (optional dependency, uses reflection)
- **Configurable item destruction**: Items may be destroyed on completion (0–100% chance, default 15%), configurable in the mod config file
- **Pure Mixin addon**: No source code changes to Create or Create: Villager Labor — just drop it in

## Requirements

| Dependency | Version |
|-----------|---------|
| NeoForge | 21.1.234+ |
| Minecraft | 1.21.1 |
| Create | 6.0.0 – 6.1.0 |
| Create: Villager Labor | 1.0.0+ |

## Installation

1. Install NeoForge 21.1.234+ for Minecraft 1.21.1
2. Install Create 6.0+ and Create: Villager Labor 1.0+
3. Drop `createlaborrush-1.0.0.jar` into your `mods` folder
4. Launch the game

## Configuration

The mod config file is located at `config/createlaborrush-common.toml`:

```toml
[work]
    # Probability (0.0–1.0) that an item is destroyed during Work processing
    destroyChance = 0.15
```

## Technical Details

### Architecture

This mod uses **Mixin** to inject into `WorkerSeatBlockEntity` (the base class of all worker stations in Create: Villager Labor). Two injection points:

| Target | Injection | Effect |
|--------|-----------|--------|
| `processWork()` | `@At("HEAD")` | Shortens processing timer and cooldown |
| `finishProcessing()` | `@At("HEAD")` | Shortens cooldown duration; handles item destruction |

All subclasses (`SawSeatBlockEntity`, `PressSeatBlockEntity`, `MixerSeatBlockEntity`, `MillstoneSeatBlockEntity`, `DeployerSeatBlockEntity`) are automatically covered.

### Safety

- `require = 0` on all injections — if Create: Villager Labor updates and changes method names, the mod won't crash; it'll just log a warning
- `@Inject` instead of `@Overwrite` — compatible with other mixins
- `@Unique` methods prefixed with `laborrush$` — avoids naming conflicts

### Effect Registration

The "Work!" effect (`createlaborrush:work`) is registered via NeoForge's `DeferredRegister`. It's a beneficial effect with red particles. The effect itself is purely visual — all logic is in the mixins.

## License

MIT — see [LICENSE](LICENSE).

## Credits

- **xiaoou6630** — author
- **Create: Villager Labor** — the base mod this addon extends
- **Create** — the mechanical automation framework