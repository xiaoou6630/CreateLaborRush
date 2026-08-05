# Create: Labor Rush

一个 **Create: Villager Labor** 的附属模组，通过施加"干活！"效果来加速工人。

## 概述

想让你的村民工作得更快？这个模组可以做到——用**拴绳**左键点击工人，或者敲响**钟**来给附近所有工人加速。

处于"干活！"效果下的工人几乎瞬间完成加工：

| 阶段 | 正常 | 有"干活！"效果 |
|-------|--------|-------------|
| 加工中 | 5–20 tick | **1 tick** |
| 冷却 | 20 tick | **5 tick** |

## 使用方法

### 拴绳命令
主手持有**拴绳**，左键点击坐在座椅上的工人（村民、玩家或东方人形）。他们会获得 90 秒的"干活！"效果。

- **普通拴绳** → Work I（amp 0，按座椅材质批量）
- **火焰附加 I 拴绳** → Work II（amp 1，批量 32）
- **火焰附加 II 拴绳** → Work III（amp 2，批量 64）
- **引雷拴绳** → Work III（amp 2，批量 64，落雷特效）

### 钟声
右键点击**钟**或**Desk Bell**，给 16 格半径内的所有工人施加 Work I（amp 0）效果。

### 闪电五连鞭
**Shift + 左键**点击工人，打击 5 格半径内的所有工人（5 秒冷却）：
- 根据拴绳附魔，给予对应等级的 Work 效果
- 每个工人头顶落下闪电（视觉效果，不造成伤害）
- 播放雷鸣音效

### 附魔互斥
- **火焰附加**和**引雷**不能同时存在于同一拴绳上

## 功能

- **支持所有工位**：锯木机、压板机、搅拌机、磨石、机械手等
- **可配置的物品销毁**：加工完成时物品可能被销毁（0–100% 概率，默认 15%）
- **纯 Mixin 附属**：无需修改 Create 或 Create: Villager Labor 的源代码

## 需求

| 依赖 | 版本 |
|-----------|---------|
| Forge | 47.4.22+ |
| Minecraft | 1.20.1 |
| Create | 6.0.0 – 6.1.0 |
| Create: Villager Labor | 1.4.0+ |

## 安装

1. 安装 Forge 47.4.22+ for Minecraft 1.20.1
2. 安装 Create 6.0+ 和 Create: Villager Labor 1.4+
3. 将 `createlaborrush-1.0.0.jar` 放入 `mods` 文件夹
4. 启动游戏

## 配置

配置文件位于 `config/createlaborrush-common.toml`：

```toml
[work]
    # 加工时物品被销毁的概率 (0.0–1.0)
    destroyChance = 0.15
    # 触发销毁时，销毁比例下限 (0.0–1.0)
    destroyRatioMin = 0.2
    # 触发销毁时，销毁比例上限 (0.0–1.0)
    destroyRatioMax = 0.5
```

## 技术细节

### 架构

本模组使用 **Mixin** 注入到 `WorkerSeatBlockEntity` 中。两个注入点：

| 目标 | 注入 | 效果 |
|--------|-----------|--------|
| `processWork()` | `@At("HEAD")` | 缩短加工计时器和冷却 |
| `finishProcessing()` | `@At("HEAD")` | 缩短冷却时间；处理物品销毁 |
| `tryTakeFromBelt/Depot/Basin` | `@Redirect` | 根据 amplifier 等级覆盖批量大小 |

### 安全

- `require = 1` 确保 Mixin 正确注入
- `@Inject` 替代 `@Overwrite`，兼容其他 Mixin
- `@Unique` 方法以 `laborrush$` 为前缀，避免命名冲突

### 效果注册

"干活！"效果（`createlaborrush:work`）通过 Forge 的 `DeferredRegister` 注册，是一个带有红色粒子的正面效果。效果本身是纯视觉的——所有逻辑都在 Mixin 中实现。

## 许可证

MIT — 参见 [LICENSE](LICENSE)。

## 鸣谢

- **xiaoou6630** — 作者
- **Create: Villager Labor** — 本模组扩展的基模组
- **Create** — 机械自动化框架