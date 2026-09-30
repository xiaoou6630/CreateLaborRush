# Create: Labor Rush

[![Modrinth](https://img.shields.io/badge/Modrinth-Create_Labor_Rush-00AF5C?logo=modrinth&logoColor=white)](https://modrinth.com/project/iD2FJnQa)

[简体中文](README.md) | **English** | [Русский](README.ru.md)

### Boost with a Lead or Bell - now with enchanted bells, worker rebellions and 19 achievements

An addon for **Create: Villager Labor** that lets you motivate villagers to work at incredible speeds. Use a Lead on a single worker, or ring a Bell to boost everyone nearby.

**New in v2.0.1:** in-game graphical config with server/admin sync, custom-model hats (struggle tall hat and capitalist hat), a 4x3 slogan painting, and Russian localisation.

---

## Version Compatibility

| Your Mod Version | CVL Version | Status |
|------------------|-------------|--------|
| **v2.0.1** | **1.4.0 - 1.5.3** | Current |
| v2.0.0 | 1.4.0 - 1.5.0 | Archived |
| v1.1.0 | 1.4.0 - 1.5.0 | Archived |
| v1.0.0 | 1.3.1 | Archived |

> Requires Create: Villager Labor 1.4.0 or newer. Available for **Forge 1.20.1** and **NeoForge 1.21.1**.

---

## How It Works

- **Lead (Normal)** - left-click a seated worker with a Lead to give them a "Work" effect for **90 seconds**.
- **Lead + Fire Aspect I** - left-click with a Fire Aspect I Lead to activate **Work II**, processing **32 items per batch**.
- **Lead + Fire Aspect II** - left-click with a Fire Aspect II Lead to activate **Work III**, processing **64 items per batch**.
- **Lead + Channeling** - left-click a worker, then **Shift + Left-click** to unleash the **Lightning 5-Whip**: a 5-block AoE chain strike with a brand-new lightning effect (beam, burst, shockwave, arcing current).
- **See it in action:** [Lightning 5-Whip (bilibili)](https://www.bilibili.com/video/BV1Y24y1g759/?share_source=copy_web&vd_source=a4a9ee56bb8c9d1119a8298c1a1d3b2a&t=25)
- **Bell / Desk Bell** - right-click a Bell or Desk Bell to motivate **all seated workers** within **16 blocks** at once. Enchanted bells work too.
- **Enchanted Bells** - enchant a Bell with **Fire Aspect** or **Channeling** on an anvil, place it, and every ring triggers the enchantment. Effects are saved to the block and **persist across world restarts**; middle-click to copy an enchanted bell instantly.

> Tip: leads can be enchanted using an anvil and an enchanted book.

---

## What It Does

Workers under the "Work" effect blaze through recipes at maximum speed:

| Phase | Normal | With Work |
|-------|--------|-----------|
| Processing Time | 10-20 ticks | 1 tick |
| Cooldown (between items) | 20 ticks | 1 tick |

### Batch Processing

| Trigger | Mode | Batch Size |
|---------|------|------------|
| Bell / Normal Lead | Work I | Seat material default (1/2/4) |
| Fire Aspect I Lead | Work II | 32 |
| Fire Aspect II Lead | Work III | 64 |

> Total cycle time drops from roughly 1-2 seconds to just **2 ticks (0.1s)** - ideal for high-throughput production lines.

---

## Worker Rebellion (new in v2.0)

Push your workers too hard and they might just snap. This optional system is **disabled by default** - enable it in the config if you want chaos.

- **Contagion** - one angry worker can spread the rebellion to the whole workshop
- **Rebel leader** - rebellions have a leader (tracked with a boss bar), and rebels buff up while rebelling
- **Property damage** - rebels smash nearby machines (configurable)
- **Emerald negotiation** - right-click the leader and pay up to end the strike peacefully
- **Maid Uprising** - Touhou Little Maid maids auto-equip weapons and shout 22 Touhou-themed battle cries (gracefully skipped if the maid mod is not installed)

### How to Quell a Rebellion

| Method | How | Note |
|--------|-----|------|
| Kill them | Kill every rebel | Workers go on strike afterwards |
| Wait it out | Let the timer run out | Rebellion ends on its own |
| Pay ransom | Right-click the leader with emeralds | Ends it peacefully |
| Ring a bell | Ring any (enchanted) bell inside the area | Unlocks the Peacemaker achievement |
| Channeling | Strike the rebels with a Channeling lead | Unlocks the Stormlord achievement |

> `enableRebellion` only controls whether new rebellions can start. An ongoing rebellion keeps running.

---

## Balancing - Haste Makes Waste

Working at breakneck speed has its risks: items processed under "Work" have a **configurable chance** of being destroyed on completion.

When destruction triggers, **only a portion of the batch is lost** (default 20%-50%), not the entire batch.

| Setting | Default | Effect |
|---------|---------|--------|
| `destroyChance` | `0.15` | Chance of destruction (0.0-1.0) |
| `destroyRatioMin` | `0.2` | Minimum lost when destroyed (0.0-1.0) |
| `destroyRatioMax` | `0.5` | Maximum lost when destroyed (0.0-1.0) |
| `enableRebellion` | `false` | Toggle the rebellion system (new rebellions only) |
| `rebellionTriggerTime` | `300` | Seconds of accumulated work before a rebellion can start |
| `rebellionDuration` | `300` | Rebellion duration (seconds) |
| `canDestroyDevices` | `true` | Let rebels smash machines |
| `destroyCooldown` | `10` | Cooldown between demolitions, per rebel (seconds) |
| `destroyIntensity` | `0.5` | Destruction intensity (0.0-1.0) |

**Example:** processing 64 items with Work III gives a 15% chance to trigger; if triggered, 13-32 items are lost (random roll) and the rest are produced normally.

Config is editable in-game via **Mods -> Create: Labor Rush -> Config** (applies instantly), or directly in `config/createlaborrush-common.toml`.

On multiplayer the server's config is authoritative: only admins (OP, or the singleplayer/LAN host) can change it, and their changes are synced to everyone else. Server values are never written into a client's local config file.

---

## Features

- Works with **all worker stations**: Saw, Press, Mixer, Millstone, Deployer
- **Enchanted Bells** with Fire Aspect / Channeling, persisted across restarts
- **Worker Rebellion** with contagion, leaders, negotiation and 5 ways to quell it
- **19 achievements** - from "First Whip" to "Minister of Punishment"
- **Brand-new lightning effects** for Channeling strikes
- **Custom-model hats**: struggle tall hat and capitalist hat (helmet slot, 3 armor / 1 toughness, maids can wear them too)
- **Slogan painting**: a 4x3 "Time is money, efficiency is life" canvas
- Compatible with **Touhou Little Maid** (maids can be motivated and revolt)
- Compatible with **Millenaire** villagers (reflection, optional)
- **Fire Aspect** supercharge - 32/64 items per batch
- **Workers protected from fire damage** - they will not burn
- Fully configurable item loss chance and loss ratio
- Zero coremod changes - pure Mixin addon

---

## Requirements

| Dependency | Version |
|------------|---------|
| **Forge** | 47.4.22+ |
| **NeoForge** | 21.1.234+ |
| **Minecraft** | 1.20.1 / 1.21.1 |
| **Create** | 6.0.10 - 6.1.0 |
| **Create: Villager Labor** | 1.4.0 - 1.5.3 |
| Touhou Little Maid (optional) | 1.5.x |

---

## Notes

- The "Work" effect has a custom status icon (red)
- Fire Aspect leads produce flame / soul flame particles on workers (visual only, no damage)
- Use the Bell to motivate your entire workshop at once
- Effect duration: **90 seconds (1800 ticks)**
- Rebellion is **off by default** - flip `enableRebellion` in the config to try it

---

## Changelog

### v2.0.1
- In-game graphical config, with admin-only changes synced from the server
- New items: struggle tall hat, capitalist hat (custom models, maids can wear them) and a 4x3 slogan painting
- Russian localisation (zh_cn / en_us / ru_ru)
- Fixed the rebellion boss bar jumping when the duration setting was changed mid-rebellion
- Placard (hanging-sign-on-body) feature is disabled in this build

### v2.0.0
- Enchanted Bells (Fire Aspect / Channeling), saved to the block and persisted across restarts
- Worker Rebellion system (opt-in), Maid Uprising, 19 achievements
- Overhauled lightning effects, Desk Bell support, numerous fixes

### v1.1.0
- Fire Aspect I / II leads (32 / 64 items per batch), Channeling Lightning 5-Whip
- Workers protected from fire damage, configurable partial batch destruction

### v1.0.0
- Initial release

---

*This mod is an independent addon and is not affiliated with Create: Villager Labor or the Create mod team.*
