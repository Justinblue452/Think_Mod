# Think

> 适用于 Minecraft 26.3 (1.21.6+) 的 Fabric 客户端辅助模组。

Think 是一个纯客户端的辅助模组，主要面向速通和生电玩家。它把要塞定位、下界坐标换算、实体威胁预警这几个常用功能整合进了一个轮盘菜单，按 V 键即可唤出，不需要记快捷键。

A client-side utility mod for Minecraft 26.3 (1.21.6+) on Fabric. It is aimed at speedrunners and technical players, bundling stronghold triangulation, Nether coordinate conversion, and entity threat alerts into a single radial menu bound to the V key.

---

## 功能 / Features

### 末影之眼三角定位 / Eye Triangulation

从轮盘菜单启动定位后，模组会在 HUD 顶部显示当前进度。投掷第一颗末影之眼会记录方向，随后提示你移动到合适位置再投掷第二颗，最终由两条射线求交点得出要塞坐标。

- 采样末影之眼生成时的速度向量作为方向依据
- 会拒绝间距过近或夹角过小的投掷，避免误差过大
- 定位完成后 HUD 底部会出现白色箭头指向要塞

> After starting the locator from the radial menu, the HUD shows progress at the top of the screen. Throwing the first eye records its direction; you'll be prompted to move to a suitable position before throwing the second one. The stronghold position is then calculated as the intersection of two rays. Invalid throws (too close or nearly parallel) are rejected to keep the result accurate.

### HUD 导航 / HUD Navigation

屏幕底部有两个箭头共用同一位置：

- 白色箭头：指向末影之眼计算出的要塞
- 蓝色箭头：指向你手动设置的传送门目标

设置跨维度目标时（比如在主世界设置、跑到下界），箭头会自动按 8:1 换算坐标。目标高度差超过 2 格时，箭头下方会显示 `↑ 12` 或 `↓ 6` 之类的提示。

移动和转向都加了平滑插值，正常游玩不会出现箭头抖动的情况。

> Two arrows share the same position at the bottom of the screen: white for strongholds, blue for custom portal targets. Cross-dimension targets are automatically scaled 8:1. When the target's Y level differs by more than 2 blocks, a height indicator like `↑ 12` or `↓ 6` appears below the arrow. Position and rotation are smoothly interpolated so the arrow doesn't jitter during normal play.

### 实体威胁预警 / Entity Threat Alerts

默认只对苦力怕生效，但可以通过指令添加任意实体作为预警目标。

- 在视野内：目标被红色方框锁定，距离越近方框越亮
- 在视野外：在离目标最近的屏幕边缘生成红色警示图标，同方向多个敌人会合并显示为 ×3
- 4 格内会播放警告音效（可在设置中更换或关闭）

> Creepers are warned by default, but you can add any entity via command. In view: targets are marked with a red box that brightens as they approach. Out of view: a red alert icon appears on the nearest screen edge, and multiple threats in the same direction are merged into a ×3 indicator. A warning sound plays within 4 blocks (configurable).

### 下界坐标换算 / Nether Coordinate Conversion

设置传送门目标后，主世界和下界的坐标会自动换算。也可以直接用指令查询当前位置对应的另一维度坐标。

> Portal targets are auto-converted between Overworld and Nether coordinates. You can also query the corresponding coordinates in the other dimension directly via command.

### 轮盘菜单 / Radial Menu

按 V 唤出，8 个功能按顺序排列在圆环上。支持鼠标点击，也可以按数字键 1~8 直接选择。再按 V 或 ESC 关闭。

如果安装了 Mod Menu，可以在模组列表中直接打开设置界面调整各项参数。

> Press V to open. Eight functions are arranged in a ring. Supports mouse clicks or number keys 1-8. Press V or ESC again to close. If Mod Menu is installed, the settings screen can be opened directly from the mod list.

---

## 快捷键 / Controls

| 按键 | 作用 |
| :--- | :--- |
| V | 打开 / 关闭轮盘菜单 |
| 鼠标左键 / 右键 | 点击轮盘按钮 |
| 1 ~ 8 | 快速选择对应功能 |
| ESC | 关闭菜单 |

---

## 指令 / Commands
