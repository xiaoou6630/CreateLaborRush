# Create: Labor Rush v2.0

An addon for **Create: Villager Labor** that speeds up seated workers by applying the "Work!" effect, with new features including worker rebellion system, maid uprising, lightning effects, and achievement system.

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

- **Normal Lead** → Work I (amp 0, batch size based on seat material)
- **Fire Aspect I Lead** → Work II (amp 1, fixed batch size 32)
- **Fire Aspect II Lead** → Work III (amp 2, fixed batch size 64)
- **Channeling Lead** → Work III (amp 2, fixed batch size 64, lightning visual)

### Bell ring
Right-click a **Bell** or **Desk Bell** to apply Work I (amp 0) to all seated workers within a **16-block radius**.

### Lightning Five-Whip (Shift + Left Click)
**Shift + Left Click** a worker to strike all workers within a 5-block radius (5-second cooldown):
- Applies the appropriate Work level based on the Lead's enchantments
- Triggers lightning effect on each worker (particle-based, no damage)
- Plays thunder sound effect

### Enchantment Mutual Exclusion
- **Fire Aspect** and **Channeling** cannot coexist on the same Lead or Bell

## v2.0 New Features

### Worker Type Detection
- Vanilla Villagers: direct instanceof
- Players: direct instanceof
- CVL tagged workers: `create_labor:workers` tag
- Touhou Little Maid: **reflection** (`Class.forName`), soft dependency
- Millénaire villagers: **reflection** (`Class.forName`), soft dependency
- All reflection code wrapped in try-catch, silently degrades when class not found

### Lightning Effect
Complete particle sequence per worker (no entity lightning):
- Tick 0: Blue light column (END_ROD falling vertically)
- Tick 2: Lightning burst (200 ELECTRIC_SPARK spherical spread)
- Tick 4: Shockwave diffusion (white arc rings, fading outward)
- Tick 6: Electric current缠绕 (ELECTRIC_SPARK jumping, 1 second)
- Tick 8: Ground aftermath (ELECTRIC_SPARK + FLAME, 2 seconds)
- Screen shake + thunder sound effects

### Worker Rebellion System
**Disabled by default**, configurable in the mod config file.

| Trigger Condition | Interval | Chance | Trigger Time |
|---------|------|------|---------|
| No Work effect | 120s | 2% | 600s |
| Work I | 60s | 5% | 300s |
| Work II | 30s | 8% | 150s |
| Work III | 15s | 15% | 60s |

**Key Mechanics:**
- **Smart AI Division**: Attackers attack players, destroyers dismantle devices
- **Strength Buff**: Strength I-V based on rebel count
- **Contagion**: Every 10 seconds, 3-block radius, 30% chance, max 10 rebels
- **Leader System**: Random leader, 👑 tag, glowing, Boss bar, doubled HP
- **Rebellion Effects**: Explosion, angry villager, flame particles + raid horn
- **Chat Slogans**: Random messages on rebellion trigger, contagion, leader appearance

### Maid Uprising (Reflection, Soft Dependency)
When a maid rebels, the following process is executed:
1. **Search Inventory**: Priority-based weapon search
   - Priority 1: Guns (check ammo, equip only if ammo available)
   - Priority 2: Ranged weapons (bow, crossbow, trident)
   - Priority 3: Melee weapons (sword, axe, sorted by damage)
   - Priority 4: Unarmed attack
2. **Switch Attack Mode**: Melee/Ranged
3. **Initiate Attack**: Set target and attack

**Maid Rebellion Dialogue (Bubble Chat):**
- 22 Touhou-themed dialogue pool, randomly selected
- Reflection-based owner name retrieval
- Falls back to name tag when reflection fails

### Achievement System (19 Achievements)
Unlockable by tier, from "First Whip" to "Minister of Justice":

| Tier | Achievement | Type | Prerequisite |
|------|------|------|------|
| 1 | First Whip | TASK | - |
| 1 | Sustainable | TASK | - |
| 2 | Whip 10 Times | TASK | First Whip |
| 2 | Fire Master | GOAL | First Whip |
| 2 | Symphony | GOAL | Sustainable |
| 3 | Hellfire | GOAL | Fire Master |
| 3 | Group Attack | GOAL | First Whip |
| 3 | Peace Ambassador | TASK | - |
| 4 | Heavenly Punishment | GOAL | Hellfire |
| 4 | Heavenly Sound | GOAL | Symphony |
| 4 | First Rebellion | Hidden | Whip 10 Times |
| 5 | Lightning Five-Whip | GOAL | Heavenly Punishment |
| 5 | Suppressor | GOAL | First Rebellion |
| 5 | Rebellion Leader | Hidden | First Rebellion |
| 5 | Peace Bell | Hidden | First Rebellion |
| 5 | Demolition Squad | Hidden | First Rebellion |
| 6 | Village Destroyer | CHALLENGE | Suppressor |
| 6 | Lightning King | CHALLENGE | Heavenly Sound + Suppressor |
| 7 | Minister of Justice | CHALLENGE | All other achievements |

## Features

- **All stations supported**: Saw, Press, Mixer, Millstone, Deployer — any block extending `WorkerSeatBlockEntity`
- **All worker types supported**: Villagers, Players, CVL tagged workers, Touhou Little Maids, Millénaire villagers
- **Touhou Little Maid compatibility**: Maids can receive the "Work!" effect too (reflection, optional dependency)
- **Configurable item destruction**: Items may be destroyed on completion (0–100% chance, default 15%)
- **Lightning effects**: Particle-based, no entity lightning spawned
- **Amplifier-based Work levels**: Work I/II/III distinguished by amplifier (0/1/2)
- **Enchantment mutual exclusion**: Fire Aspect and Channeling are mutually exclusive
- **Pure Mixin addon**: No source code changes to Create or Create: Villager Labor
- **Reflection-based soft dependencies**: Touhou Little Maid and Millénaire are optional

## Requirements

| Dependency | Version |
|-----------|---------|
| NeoForge | 21.1.234+ |
| Minecraft | 1.21.1 |
| Create | 6.0.0 – 6.1.0 |
| Create: Villager Labor | 1.4.0+ |

## Installation

1. Install NeoForge 21.1.234+ for Minecraft 1.21.1
2. Install Create 6.0+ and Create: Villager Labor 1.4+
3. Drop `createlaborrush-2.0.0.jar` into your `mods` folder
4. Launch the game

## Configuration

The mod config file is located at `config/createlaborrush-common.toml`:

```toml
[work]
    # Probability (0.0–1.0) that an item is destroyed during Work processing
    destroyChance = 0.15
    # When destroy triggers, minimum percentage (0.0–1.0) of the batch that will be destroyed
    destroyRatioMin = 0.2
    # When destroy triggers, maximum percentage (0.0–1.0) of the batch that will be destroyed
    destroyRatioMax = 0.5

[rebellion]
    # Enable worker rebellion system
    enableRebellion = false
    # Base trigger time (seconds)
    rebellionTriggerTime = 300
    # Base trigger chance
    rebellionChance = 0.05
    # Rebellion duration (seconds)
    rebellionDuration = 30
    # Detection radius
    rebellionRadius = 5
    # Allow device destruction
    canDestroyDevices = false
    # Destruction cooldown (seconds)
    destroyCooldown = 10
    # Attacker ratio (remaining are destroyers)
    attackPlayerRatio = 0.5
```

## Technical Details

### Architecture

This mod uses **Mixin** to inject into `WorkerSeatBlockEntity` (the base class of all worker stations in Create: Villager Labor). Two injection points:

| Target | Injection | Effect |
|--------|-----------|--------|
| `processWork()` | `@At("HEAD")` | Shortens processing timer and cooldown |
| `finishProcessing()` | `@At("HEAD")` | Shortens cooldown duration; handles item destruction |
| `tryTakeFromBelt/Depot/Basin` | `@Redirect` | Overrides batch size based on amplifier level |

All subclasses (`SawSeatBlockEntity`, `PressSeatBlockEntity`, `MixerSeatBlockEntity`, `MillstoneSeatBlockEntity`, `DeployerSeatBlockEntity`) are automatically covered.

### Worker Detection Architecture

```
WorkerUtil.isWorkerEntity()  (CVL-provided, tag-based)
  ├── instanceof Villager (vanilla)
  ├── instanceof Player (vanilla)
  ├── create_labor:workers tag (CVL)
  └── WorkerTypeDetector (reflection)
        ├── isMaid() → Touhou Little Maid
        └── isMillVillager() → Millénaire
```

### Safety

- `require = 0` on all injections — if Create: Villager Labor updates and changes method names, the mod won't crash; it'll just log a warning
- `@Inject` instead of `@Overwrite` — compatible with other mixins
- `@Unique` methods prefixed with `laborrush$` — avoids naming conflicts
- All reflection code wrapped in try-catch, silently degrades on failure

### Effect Registration

The "Work!" effect (`createlaborrush:work`) is registered via NeoForge's `DeferredRegister`. It's a beneficial effect with red particles. The effect itself is purely visual — all logic is in the mixins.

## License

MIT — see [LICENSE](LICENSE).

## Credits

- **xiaoou6630** — author
- **Create: Villager Labor** — the base mod this addon extends
- **Create** — the mechanical automation framework
- **Touhou Little Maid** — maid mod (reflection-compatible)
- **Millénaire** — medieval village mod (reflection-compatible, 1.21.1 only)