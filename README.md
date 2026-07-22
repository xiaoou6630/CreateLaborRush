# ⚡ Create: Labor Rush

**An addon for Create: Villager Labor that speeds up seated workers.**

[![Modrinth](https://img.shields.io/badge/Modrinth-1.21.1-green)](https://modrinth.com/mod/createlaborrush)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 📖 Overview

Ever wished your villagers would work faster? This mod lets you give them a speed boost — use a **Lead** on a single worker, or ring a **Bell** to boost everyone nearby.

**New in v1.1.0:** Use **Flame Aspect** enchantment on a Lead to unlock **批量超频** — workers process up to 64 items per batch! 🔥

---

## 🚀 How It Works

### 🔔 Bell Ring
Right-click a **Bell** or **Desk Bell** to apply the **"Work!"** effect to **all seated workers** within a **16-block radius**.

### 🪢 Lead Command
Hold a **Lead** in your main hand and **left-click** a seated worker:
- **Normal Lead** → Work I (batch size depends on seat material)
- **Flame Aspect I Lead** → Work II (**32 items/batch**) 🔥
- **Flame Aspect II Lead** → Work III (**64 items/batch**) 🔥🔥

> 💡 **Tip:** Leads can now be enchanted with Flame Aspect using an anvil + enchanted book!

---

## ⚡ What It Does

| Phase | Normal | With "Work!" |
|-------|--------|--------------|
| Processing Time | 10–20 ticks | **1 tick** |
| Cooldown (between items) | 20 ticks | **1 tick** |

### 📊 Batch Processing (v1.1.0)

| Trigger | Mode | Batch Size |
|---------|------|------------|
| Bell / Normal Lead | **Work I** | Seat material default (1/2/4) |
| **Flame Aspect I Lead** | **Work II** | **32** 🔥 |
| **Flame Aspect II Lead** | **Work III** | **64** 🔥🔥 |

> 💡 **Creative seats** always process 64 items/batch and are unaffected by Flame Aspect.

---

## 🎲 Balancing – Haste Makes Waste

Working at breakneck speed has its risks. Items processed under "Work!" have a **configurable chance** of being destroyed on completion.

**When destruction triggers, only a portion of the batch is lost** (default: 20%–50%), not the entire batch.

This is fully configurable in `config/createlaborrush-common.toml`:

| Setting | Default | Effect |
|---------|---------|--------|
| `destroyChance` | `0.15` | Chance of destruction (0.0–1.0) |
| `destroyRatioMin` | `0.2` | Minimum items lost when destroyed (0.0–1.0) |
| `destroyRatioMax` | `0.5` | Maximum items lost when destroyed (0.0–1.0) |

**Example:**
- You process **64 items** with Work III
- 15% chance to trigger destruction
- If triggered, **13–32 items** are lost (random roll between 20%–50%)
- The rest are produced normally ✅

---

## 🔥 Flame Aspect Supercharge (v1.1.0)

| Enchantment Level | Mode | Batch Size | Visual |
|-------------------|------|------------|--------|
| None | Work I | Seat default | — |
| **Flame Aspect I** | Work II | **32** | 🔥 Flame particles |
| **Flame Aspect II** | Work III | **64** | 🔥 Soul flame particles |

### 💡 How to Enchant a Lead

1. Obtain a **Flame Aspect** enchanted book (from villager trading, loot, or fishing)
2. Use an **Anvil** + the book on a **Lead**
3. Now your Lead is supercharged!

### 🛡️ Safety

Workers under the "Work!" effect **will not take fire damage** from Flame Aspect — they're protected! The fire particles are purely visual.

---

## ✨ Features

- ✅ Works with **all worker stations** – Saw, Press, Mixer, Millstone, Deployer
- ✅ Compatible with **Touhou Little Maid** (maids can be motivated too)
- ✅ Compatible with **Millénaire** villagers (optional detection via reflection)
- ✅ Fully configurable item loss chance and loss ratio
- ✅ Flame Aspect enchanted Leads unlock **32/64 batch supercharge**
- ✅ Zero coremod changes – pure Mixin addon
- ✅ Workers protected from fire damage

---

## 📦 Requirements

| Dependency | Version |
|------------|---------|
| **NeoForge** | 21.1.234+ |
| **Minecraft** | 1.21.1 |
| **Create** | 6.0.10 – 6.1.0 |
| **Create: Villager Labor** | 1.4.0 – 1.5.0 |

> ⚠️ **v1.1.0 requires Create Villager Labor 1.4.0 or newer.**  
> For CVL 1.3.1, use [v1.0.0](https://github.com/yourusername/createlaborrush/releases/tag/v1.0.0).

---

## 📥 Installation

1. Install **NeoForge 21.1.234+** for Minecraft 1.21.1
2. Install **Create 6.0+** and **Create: Villager Labor 1.4.0+**
3. Drop `createlaborrush-1.1.0.jar` into your `mods` folder
4. Launch the game

---

## 🛠️ Configuration

File: `config/createlaborrush-common.toml`

```toml
[work]
# 触发销毁的总概率（0.0–1.0）
destroyChance = 0.15

# 触发销毁后，销毁比例下限（0.0–1.0）
destroyRatioMin = 0.2

# 触发销毁后，销毁比例上限（0.0–1.0）
destroyRatioMax = 0.5
