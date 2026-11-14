# Think

[![Minecraft](https://img.shields.io/badge/Minecraft-26.3+-brightgreen?style=flat-square)](https://minecraft.net)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-blue?style=flat-square)](https://fabricmc.net)
[![Client-side](https://img.shields.io/badge/Side-Client-orange?style=flat-square)](https://modrinth.com)

**Think** 是一款专为速通（Speedrunning）、生电（Technical Minecraft）和日常生存设计的**纯客户端**辅助模组。它将复杂的要塞定位、地狱交通计算与 FPS 级别的敌情预警，封装在了一个拥有现代交互动画的环形菜单中。

**Think** is a lightweight, **client-side** utility mod designed for speedrunning, technical survival, and daily gameplay. It packs advanced stronghold triangulation, Nether transit calculation, and FPS-style threat alerts into a modern, radial-menu-driven interface.

---

## 🌟 核心特色 / Key Features

### 🎯 1. 科学且精准的末影之眼三角定位 (Precision Eye Triangulation)
不再盲目寻找要塞。按下轮盘菜单开始定位，系统会通过 HUD 逐步引导你完成“投掷-移动-再投掷”的标准流程。
*   **底层算法**：精准采样末影之眼实体的初始速度向量，配合克莱默法则（Cramer's Rule）求解交点。
*   **智能引导**：在聊天栏和 HUD 顶部实时提示当前进度、还需移动的距离以及是否满足计算夹角。
*   **纯视觉反馈**：计算完成后，HUD 会出现一个平滑旋转的**白色箭头**，带你直奔要塞。
> *No more blind searching. Initiate the locator from the radial menu, and let the HUD guide you through the standard "throw-move-throw" process. Utilizes precise velocity sampling and Cramer's Rule for intersection calculation, pointing you to the stronghold with a smoothly rotating white arrow.*

### 🧭 2. 动态双模式 HUD 导航 (Dual-Mode HUD Navigation)
*   **白色箭头**：指向末影之眼计算出的要塞。
*   **蓝色箭头**：指向你自定义的传送门目标。
*   **Y 轴高度提示**：目标在上方或下方时，箭头下方会贴心地显示 `↑ 15` 或 `↓ 8`。
*   **坐标死区防抖**：采用高频滤波与 lerp 平滑插值，告别低帧率下箭头“一抖一抖”的糟糕体验。
> *White arrow for stronghold, Blue arrow for custom portal targets. Features Y-axis height indicators (e.g., `↑ 15`) and deadzone anti-shake technology to ensure smooth camera tracking.*

### 💣 3. FPS 级别的实体威胁预警 (FPS-Style Threat Alerts)
仿照现代射击游戏的手榴弹警告系统，专为应对苦力怕等危险实体设计。
*   **视野内**：目标被**红色动态瞄框**锁定，距离越近（<4格），闪烁越快，压迫感拉满。
*   **视野外**：在屏幕边缘生成**红色感叹号与指向箭头**，同方向多个敌人会自动合并为 `×3` 指示器。
*   **高度自定义**：支持通过指令添加任何实体作为警告目标（如 `player` 或其他怪物），自定义探测与危险半径。
*   **动态音效**：极度危险时播放可自定义的电子警报音。
> *Modern shooter-style grenade alerts for Creepers and beyond. In-view targets are locked with a pulsing red box; off-screen threats appear as edge indicators with merged counts (`×3`). Fully customizable entities, ranges, and alert sounds.*

### 🌌 4. 地狱交通与传送门助手 (Nether Transit Assistant)
搭建地狱交通的利器。
*   **一键跨维设置**：站在主世界，呼出轮盘设置目标。进入下界后，蓝色箭头会自动进行 `8:1` 的坐标缩放，精准指引对应的下界坐标。
> *The ultimate tool for Nether hubs. Set a target in the Overworld, and the blue arrow will automatically scale the coordinates (8:1) to guide you accurately in the Nether.*

### 🎨 5. 现代环形菜单与 UI (Modern Radial Menu & UI)
*   按 **`V`** 键唤出带有 Q 弹动画（EaseOutCubic）的 8 扇区环形菜单。
*   支持鼠标点击或键盘 **`1~8`** 数字键快速操作。
*   原生支持 **Mod Menu**，所有功能开关、音效类型、检测范围均可通过图形化界面（GUI）修改。
> *Press `V` to open a buttery-smooth radial menu. Supports mouse clicks or number keys `1-8`. Full GUI configuration via Mod Menu.*

---

## ⌨️ 快捷键与指令 / Controls & Commands

| 操作 (Action) | 快捷键 (Key) | 说明 (Description) |
| :--- | :--- | :--- |
| **打开轮盘菜单** (Open Radial Menu) | `V` | 核心交互入口，包含所有功能 (Core interactive hub) |
| **选择功能** (Select Action) | 鼠标 / `1~8` | 滑动选择或直接按数字键 (Click or use num keys) |
| **退出菜单** (Close Menu) | `ESC` 或 `V` | 平滑收起菜单 (Smoothly close) |

**高级指令 (Advanced Commands):**
*   `/think status` - 查看当前三角定位状态 (Check locator status)
*   `/think warn add <entity>` - 添加警示实体，如 `/think warn add zombie` (Add warning entities)
*   `/think warn range <detect> <warn> <danger>` - 调整警示距离 (Adjust warning ranges)
*   `/think portal where` - 快速获取当前坐标的异世界对应位置 (Get translated coords)

---

## ⚙️ 性能与安全性 / Performance & Safety

*   **纯客户端运行 (Client-Side Only)**：不发送任何自定义数据包，在所有反作弊服务器下绝对安全。
*   **极致性能 (Optimized)**：实体制导采用 10Hz 扫描与哈希缓存；UI 渲染采用逐行极简绘制，拒绝掉帧。
*   **数据本地化 (Local Config)**：所有设置自动保存至 `.minecraft/config/think.json`，随时备份，永不丢失。

---

## 🛠️ 安装要求 / Requirements

*   **Minecraft**: 26.3 (1.21.6+)
*   **Loader**: Fabric Loader
*   **依赖**: Fabric API
*   **可选**: Mod Menu (用于图形化配置)

---

<details>
<summary>📜 语言支持 / Language Support</summary>

*   English (US)
*   简体中文 (Simplified Chinese)
*   文言 (华夏) (Classical Chinese)
</details>

> 💡 **Tip**: 模组内置了详细的防误触机制，当你的末影之眼计算夹角过小时，系统会拒绝记录并提示你换个方向投掷，确保每一次导航都精确无误。