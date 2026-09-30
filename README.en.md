# Create: Labor Rush

[![Modrinth](https://img.shields.io/badge/Modrinth-Create_Labor_Rush-00AF5C?logo=modrinth&logoColor=white)](https://modrinth.com/project/iD2FJnQa)

[简体中文](README.md) | **English** | [Русский](README.ru.md)

Whip workers into overdrive with a lead, enchant leads and bells with Fire Aspect / Channeling, rally everyone by ringing a bell, push them too far and trigger a worker rebellion — and slap struggle-session tall hats, capitalist top hats and hand-written placards on your workers. 19 achievements to unlock.

---

## How to Use

| Action | Effect |
|--------|--------|
| **Lead** left-click a worker | 90s "Work!" effect |
| **Bell** right-click | Buff all workers within 16 blocks |
| **Shift+Left Click** | Lightning 5-Whip (5-block AOE) |

| Lead Enchant | Level | Batch Size |
|--------------|-------|------------|
| Normal | Work I | Default |
| Fire Aspect I | Work II | 32 |
| Fire Aspect II | Work III | 64 |
| Channeling | Work III | 64 |

## Highlights

- **Enchanted Bell**: ring with Fire Aspect or Channeling, effects persist across restarts
- **Worker Rebellion** (disabled by default): contagion, leader system, strength buff, emerald peace negotiation
- **Maid Uprising**: auto-equips weapons, 22 Touhou-themed dialogues
- **19 Achievements**: from "First Whip" to "Minister of Punishment"
- **Lightning Effect**: blue beam → burst → shockwave → arcing current
- **Graphical Config**: edit in-game via Mods → Config; changes apply instantly
- **Multiplayer**: server config is authoritative; only admins can change it and it syncs to everyone
- **Struggle Tall Hat / Capitalist Hat**: custom-model helmets (3 armor / 1 toughness), maids can wear them too
- **Placard** (currently disabled): vanilla hanging signs can go into your chest slot or onto a worker, with a 4-line writing screen
- **Slogan Painting**: a 4×3 "Time is money, efficiency is life" painting
- **Localisation**: Simplified Chinese / English / Russian

## How to Quell a Rebellion

| Method | Action | Note |
|--------|--------|------|
| Kill them | Kill all rebels | Long strike cooldown in the area |
| Wait it out | Wait for the timer | Rebellion auto-ends (shorter cooldown) |
| Pay ransom | Right-click the rebel leader with **emeralds** | Pay enough to end peacefully |
| Ring a bell | Ring a bell **inside the rebellion area** | Ends peacefully; Peacemaker achievement |
| Channeling | Channeling lead + **Shift+Left Click** (Lightning 5-Whip) hitting rebels | Stormlord achievement |

> `enableRebellion` only controls whether **new** rebellions can start. An ongoing rebellion keeps running and all five methods above still work.

## Hats & Placards

| Item | Recipe | Note |
|------|--------|------|
| **Struggle Tall Hat** | Create Cardboard ×5 | Helmet slot, 3 armor / 1 toughness, maids can wear it too |
| **Capitalist Hat** | Black Wool ×4 + Red Dye ×1 | Same stats, a black felt top hat |
| **Slogan Painting** | vanilla painting flow | 4×3, reads "Time is money / Efficiency is life" |

> On 1.20.1 the painting needs a wall at least **4 wide and 3 tall** before it can be picked at random.

The placard is just a **vanilla hanging sign** — no new item. The text lives on the sign, so it survives being placed as a block.

> **The placard is currently disabled** (its text rendering still has unsolved issues). The table below describes the intended design; right now hanging signs behave exactly like vanilla.

| Action | Effect |
|--------|--------|
| Hanging sign + **right-click air** | Open the writing screen (4 lines) |
| Hanging sign + **sneak + right-click air** | Swap it into your chest slot (old chest item goes to your hand) |
| Hanging sign + **right-click a worker** | Hang it on the worker (consumes one sign) |
| **Sneak + empty hand + right-click a worker** | Take the placard back |

## Config

Edit it in-game via **Mods → Create: Labor Rush → Config** (applies instantly, no restart needed), or directly in `config/createlaborrush-common.toml`.

| Key | Default | Range | Description |
|-----|---------|-------|-------------|
| `destroyChance` | 0.15 | 0.0–1.0 | Item destruction chance |
| `destroyRatioMin` | 0.2 | 0.0–1.0 | Min batch loss ratio |
| `destroyRatioMax` | 0.5 | 0.0–1.0 | Max batch loss ratio |
| `enableRebellion` | false | bool | Enable rebellion (only gates *new* rebellions) |
| `rebellionTriggerTime` | 300 | 10–3600 | Seconds of accumulated work before a rebellion can start |
| `rebellionChance` | 0.05 | 0.0–1.0 | Base chance per 10-second check |
| `rebellionDuration` | 300 | 5–600 | Duration (seconds) |
| `rebellionRadius` | 5 | 1–32 | Detection radius (blocks) |
| `canDestroyDevices` | true | bool | Rebels destroy devices |
| `destroyCooldown` | 10 | 1–60 | Cooldown between demolitions, per rebel (seconds) |
| `destroyIntensity` | 0.5 | 0.0–1.0 | Destruction intensity |

On multiplayer the server's config is authoritative: only admins (OP, or the singleplayer/LAN host) can change it, and their changes are synced to everyone else. Rejected changes are reported in chat. Server values are never written into a client's local config file.

## Compatibility

| Dependency | Version |
|-----------|---------|
| MC | 1.20.1 (Forge) / 1.21.1 (NeoForge) |
| Create | 6.0.0 – 6.1.0 |
| CVL | **1.4.0 – 2.0.0** (exclusive) |
| Touhou Little Maid (optional) | 1.5.x — maid hats / maid uprising need it |

## Requirements

- NeoForge 21.1.234+
- Create 6.0+
- Create: Villager Labor 1.4+

---

*An independent addon for Create: Villager Labor.*
