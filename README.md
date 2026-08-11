# Create: Labor Rush v2.0

Whip workers into overdrive, enchant leads and bells with Fire Aspect / Channeling, unleash worker rebellion, and earn 19 achievements.

---

## How to Use

| Action | Effect |
|--------|--------|
| **Lead** left-click a worker | 90s "Work!" effect |
| **Bell** right-click | Buff all workers within 16 blocks |
| **Shift+Left Click** | Lightning 5-Whip (5-block AOE) |

| Lead Enchant | Level | Batch Size |
|-------------|-------|-----------|
| Normal | Work I | Default |
| Fire Aspect I | Work II | 32 |
| Fire Aspect II | Work III | 64 |
| Channeling | Work III | 64 |

## v2.0 Highlights

- **Enchanted Bell**: Ring with Fire Aspect or Channeling, effects persist across restarts
- **Worker Rebellion** (disabled by default): Contagion, leader system, strength buff, emerald peace negotiation
- **Maid Uprising**: Auto-equips weapons, 22 Touhou-themed dialogues
- **19 Achievements**: From "First Whip" to "Minister of Punishment"
- **Lightning Effect**: Blue beam → burst → shockwave → arcing current

## How to Quell a Rebellion

| Method | Action | Note |
|--------|--------|------|
| Kill them | Kill all rebels | Workers go on strike |
| Wait it out | Wait for the timer | Rebellion auto-ends |
| Pay ransom | Right-click leader with **emeralds** | Pay enough to end peacefully |
| Ring a bell | Ring any bell | Peacemaker achievement |
| Channeling | Use Channeling tool | Stormlord achievement |

## Config

`config/createlaborrush-common.toml`

| Key | Default | Description |
|-----|---------|-------------|
| `destroyChance` | 0.15 | Item destruction chance |
| `destroyRatioMin` | 0.2 | Min batch loss ratio |
| `destroyRatioMax` | 0.5 | Max batch loss ratio |
| `enableRebellion` | false | Enable rebellion |
| `rebellionDuration` | 300 | Duration (seconds) |
| `canDestroyDevices` | true | Rebels destroy devices |
| `destroyIntensity` | 0.5 | Destruction intensity (0~1) |

## Compatibility

| Dependency | Version |
|-----------|---------|
| MC | 1.20.1 (Forge) / 1.21.1 (NeoForge) |
| Create | 6.0.0 – 6.1.0 |
| CVL | **1.4.0 – 1.5.0** |

## Requirements

- Forge 47.4.22+ / NeoForge 21.1.234+
- Create 6.0+
- Create: Villager Labor 1.4+

---

*An independent addon for Create: Villager Labor.*