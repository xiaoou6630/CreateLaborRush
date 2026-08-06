# Create: Labor Rush v2.0

一个 **Create: Villager Labor** 的附属模组，通过施加"干活！"效果来加速工人，并新增工人起义彩蛋、女仆武装起义、引雷特效、成就系统等丰富功能。

## 概述

想让你的村民工作得更快？这个模组可以做到——用**拴绳**左键点击工人，或者敲响**钟**来给附近所有工人加速。

处于"干活！"效果下的工人几乎瞬间完成加工：

| 阶段 | 正常 | 有"干活！"效果 |
|-------|--------|-------------|
| 加工中 | 5–20 tick | **1 tick** |
| 冷却 | 20 tick | **5 tick** |

## 使用方法

### 拴绳命令
主手持有**拴绳**，左键点击坐在座椅上的工人（村民、玩家、女仆等）。他们会获得 90 秒的"干活！"效果。

- **普通拴绳** → Work I（amp 0，按座椅材质批量）
- **火焰附加 I 拴绳** → Work II（amp 1，批量 32）
- **火焰附加 II 拴绳** → Work III（amp 2，批量 64）
- **引雷拴绳** → Work III（amp 2，批量 64，落雷特效）

### 钟声
右键点击**钟**或**Desk Bell**，给 16 格半径内的所有工人施加 Work I（amp 0）效果。

### 闪电五连鞭
**Shift + 左键**点击工人，打击 5 格半径内的所有工人（5 秒冷却）：
- 根据拴绳附魔，给予对应等级的 Work 效果
- 每个工人头顶触发引雷特效（纯粒子，不造成伤害）
- 播放雷鸣音效

### 附魔互斥
- **火焰附加**和**引雷**不能同时存在于同一拴绳或钟上

## v2.0 新增功能

### 工人类型检测
- 原版村民：直接 instanceof 检测
- 玩家：直接 instanceof 检测
- CVL 标签工人：`create_labor:workers` 标签检测
- 车万女仆（Touhou Little Maid）：**反射检测**（`Class.forName`），不依赖前置模组
- 千年村庄村民（Millénaire 1.21.1）：**反射检测**，不依赖前置模组
- 所有反射代码包在 try-catch 中，类不存在时静默降级

### 引雷特效（天劫屠龙炮版）
每个工人触发一套完整特效序列（纯粒子组合，不生成实体闪电）：
- 第 0 tick：蓝色光柱（END_ROD 垂直下落）
- 第 2 tick：闪电炸裂（200 个 ELECTRIC_SPARK 球形扩散）
- 第 4 tick：冲击波扩散（白色电弧环，逐圈淡出）
- 第 6 tick：电流缠绕（ELECTRIC_SPARK 跳跃，持续 1 秒）
- 第 8 tick：地面余波（ELECTRIC_SPARK + FLAME 混合，持续 2 秒）
- 屏幕震动 + 雷鸣音效

### 村民起义彩蛋
**默认关闭**，可在配置文件中开启。

| 触发条件 | 间隔 | 概率 | 触发时间 |
|---------|------|------|---------|
| 无 Work 效果 | 120s | 2% | 600s |
| Work I | 60s | 5% | 300s |
| Work II | 30s | 8% | 150s |
| Work III | 15s | 15% | 60s |

**特色机制：**
- **智能AI分工**：攻击者攻击玩家，破坏者拆除设备
- **力量Buff**：根据人数给予力量 I-V
- **传染机制**：每 10 秒判定，3 格半径，30% 概率传染，上限 10 人
- **首领系统**：随机选首领，👑 标识，发光，Boss 条显示，血量翻倍
- **起义特效**：爆炸、愤怒、火焰粒子 + 灾厄号角声
- **聊天栏口号**：起义触发、传染、首领登场时随机发送

### 女仆武装起义（反射调用，软前置）
当女仆起义时，按以下流程执行：
1. **搜索背包**：按优先级搜索武器
   - 第一优先级：枪械（检测弹药，有弹药才装备）
   - 第二优先级：远程武器（弓、弩、三叉戟）
   - 第三优先级：近战武器（剑、斧，按攻击力排序）
   - 第四优先级：空手攻击
2. **切换攻击模式**：近战/远程
3. **发起攻击**：设置目标并攻击

**女仆起义对话（气泡特效）：**
- 22 句东方梗对话池，随机选取
- 反射获取主人名字，显示在对话中
- 反射失败时使用头顶文字降级

### 成就系统（19个成就）
按层级解锁，从"第一鞭"到"刑部尚书"：

| 层级 | 成就 | 类型 | 前置 |
|------|------|------|------|
| 1 | 第一鞭 | TASK | - |
| 1 | 可持续发展 | TASK | - |
| 2 | 周扒皮 | TASK | 第一鞭 |
| 2 | 火焰使者 | GOAL | 第一鞭 |
| 2 | 交响乐 | GOAL | 可持续发展 |
| 3 | 地狱火 | GOAL | 火焰使者 |
| 3 | 群起而攻之 | GOAL | 第一鞭 |
| 3 | 和平大使 | TASK | - |
| 4 | 天罚 | GOAL | 地狱火 |
| 4 | 天籁之音 | GOAL | 交响乐 |
| 4 | 第一次起义 | 隐藏 | 周扒皮 |
| 5 | 闪电五连鞭 | GOAL | 天罚 |
| 5 | 镇压者 | GOAL | 第一次起义 |
| 5 | 反抗军领袖 | 隐藏 | 第一次起义 |
| 5 | 和平使者 | 隐藏 | 第一次起义 |
| 5 | 拆迁大队 | 隐藏 | 第一次起义 |
| 6 | 灭村者 | CHALLENGE | 镇压者 |
| 6 | 雷电法王 | CHALLENGE | 天籁之音+镇压者 |
| 7 | 刑部尚书 | CHALLENGE | 所有其他成就 |

## 功能

- **支持所有工位**：锯木机、压板机、搅拌机、磨石、机械手等
- **支持所有工人类型**：原版村民、玩家、CVL 标签工人、女仆、千年村庄村民
- **可配置的物品销毁**：加工完成时物品可能被销毁（0–100% 概率，默认 15%）
- **引雷特效**：纯粒子组合，不生成实体闪电
- **纯 Mixin 附属**：无需修改 Create 或 Create: Villager Labor 的源代码
- **反射软前置**：女仆和千年村庄均为反射检测，不添加硬依赖

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
3. 将 `createlaborrush-2.0.0.jar` 放入 `mods` 文件夹
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

[rebellion]
    # 启用工人起义系统
    enableRebellion = false
    # 基础触发时间（秒）
    rebellionTriggerTime = 300
    # 基础触发概率
    rebellionChance = 0.05
    # 起义持续时间（秒）
    rebellionDuration = 30
    # 检测半径
    rebellionRadius = 5
    # 是否允许破坏设备
    canDestroyDevices = false
    # 破坏冷却（秒）
    destroyCooldown = 10
    # 攻击者比例（剩余为破坏者）
    attackPlayerRatio = 0.5
```

## 技术细节

### 架构

本模组使用 **Mixin** 注入到 `WorkerSeatBlockEntity` 中。两个注入点：

| 目标 | 注入 | 效果 |
|--------|-----------|--------|
| `processWork()` | `@At("HEAD")` | 缩短加工计时器和冷却 |
| `finishProcessing()` | `@At("HEAD")` | 缩短冷却时间；处理物品销毁 |
| `tryTakeFromBelt/Depot/Basin` | `@Redirect` | 根据 amplifier 等级覆盖批量大小 |

### 工人检测架构

```
WorkerUtil.isWorkerEntity()  (CVL 提供，标签检测)
  ├── instanceof Villager (原版)
  ├── instanceof Player (玩家)
  ├── create_labor:workers 标签 (CVL)
  └── WorkerTypeDetector (反射检测)
        ├── isMaid() → 车万女仆
        └── isMillVillager() → 千年村庄
```

### 安全

- `require = 0` 确保 Mixin 注入失败时不会崩溃
- `@Inject` 替代 `@Overwrite`，兼容其他 Mixin
- `@Unique` 方法以 `laborrush$` 为前缀，避免命名冲突
- 所有反射代码包在 try-catch 中，静默降级

### 效果注册

"干活！"效果（`createlaborrush:work`）通过 Forge 的 `DeferredRegister` 注册，是一个带有红色粒子的正面效果。效果本身是纯视觉的——所有逻辑都在 Mixin 中实现。

## 许可证

MIT — 参见 [LICENSE](LICENSE)。

## 鸣谢

- **xiaoou6630** — 作者
- **Create: Villager Labor** — 本模组扩展的基模组
- **Create** — 机械自动化框架
- **Touhou Little Maid** — 女仆模组（反射兼容）
- **Millénaire** — 千年村庄模组（反射兼容，仅 1.21.1）