## English

- Migrated all screens and UI extension APIs (`BdPage`, `StorageWorkbench`) to **CompixelUI 0.2.0**. Requires CompixelUI `0.2.0` (`[0.2.0, 0.3)`) together with a compatible Kotlin runtime mod (such as Kotlin for Forge). Add-ons using BD's UI extension APIs need to update to CompixelUI 0.2.0's unified `gameState` and `gameThread` model.
- Added independent integer UI scaling (`Auto`, `Follow game`, or fixed `1x`–`16x`) and a minimum visible rows setting for `Auto` mode in the storage screen settings, keeping the storage grid spacious and sharp under Auto GUI scale or smaller windows.
- Added an injectable side panel API (`StorageSidePanel` / `StorageSidePanels`) for the right-side expandable area of the storage screen, allowing add-ons to open custom panels alongside the storage grid. The built-in category editor now uses the same system.

## 中文

- 将全部界面与界面扩展 API（`BdPage`、`StorageWorkbench`）迁移至 **CompixelUI 0.2.0**。需要安装 CompixelUI `0.2.0`（`[0.2.0, 0.3)`）及兼容的 Kotlin 运行时模组（如 Kotlin for Forge）；使用 BD 界面扩展接口的附属模组需要同步适配 CompixelUI 0.2.0 的 `gameState` 与 `gameThread`。
- 存储界面设置新增独立的整数「界面缩放」（自动 / 跟随游戏 / `1x`–`16x`）以及自动模式下的「最少显示行数」选项，在原版自动缩放或较小窗口下也能保持清晰并留出足够空间。
- 为存储界面右侧可展开区域新增注入式侧边面板接口（`StorageSidePanel` / `StorageSidePanels`），方便附属模组在不新开整页的前提下展示自定义内容；内置分类编辑区已改为通过同一机制注册。
