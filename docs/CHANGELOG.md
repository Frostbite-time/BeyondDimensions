## English

- The storage screen now has a work area below the grid that shows one workbench at a time: Crafting Table, Anvil, Smithing Table or Stonecutter, chosen from the dropdown at its top left. When the grid is wide enough, the work area sits beside your inventory so the grid keeps more rows. The rail button expands or collapses it, and both choices are remembered.
- The Anvil, Smithing Table and Stonecutter follow vanilla's rules. The Smithing Table shows vanilla's outlines in its empty slots, and the Stonecutter's list has a scrollbar and scrolls one row per wheel notch.
- JEI and EMI fill stonecutting, smithing and anvil recipes into their own workbench and switch the work area to it; the Stonecutter also selects the recipe.
- "Keep items on close", in the new "Work area" section of the settings page, applies to every workbench. The buttons at the top right of the work area return a workbench's items to the storage or your inventory, and the arrow beside them chooses where items go first when a workbench is cleared.
- With Polymorph installed, the Crafting Table's recipe choice is at the top right of the work area.
- The sections of the settings page are separated by lines.

### For developers

- Add-ons can add workbenches: register the menu side with `StorageWorkbenchTypes`, including an availability check that runs on the server when the storage opens, and the screen with `StorageWorkbenches`. `ClearableWidget` and `RecipeFillWidget` let a workbench be cleared and filled from recipe viewers, and `PlayerWorkbenches` keeps each player's workbench items.
- Add-on pages can set their own size, and the window animates to it when the page opens.

### Breaking changes

For developers:

- The storage menus include the work area, so `DimensionsCraftMenu`, `DimensionsCraftMenuTerminal`, `CraftableMenu` and `PlayerCraftingGrid` were removed. Use `DimensionsNetMenu` or `DimensionsNetTerminalMenu`, `menu.crafting()` and `PlayerWorkbenches`. The menu types `dimensions_craft_menu` and `dimensions_craft_menu_terminal` were replaced by `dimensions_net_terminal_menu`, and `NetMenuType` now has `NET_MENU` and `NET_TERMINAL`.
- Open storage menus with `DimensionsNetMenu.open(player, provider)`. A storage menu opened another way offers every workbench without checking availability.
- `BDMenuCommands.recipe` takes the target workbench and an optional recipe ID.

## 中文

- 存储界面在存储格下方新增工作区，一次显示一个工作台：合成台、铁砧、锻造台或切石机，在工作区左上角的下拉框中选择。存储格足够宽时，工作区与背包并排，存储格能多显示几行。侧栏按钮可以展开或收起工作区，选择与展开状态都会记住。
- 铁砧、锻造台与切石机的规则与原版相同。锻造台的空槽像原版一样显示底纹；切石机的产物列表带滚动条，滚轮一格滚动一行。
- JEI 与 EMI 会把切石、锻造与铁砧配方填进对应的工作台，并把工作区切换到该工作台；切石机同时选中该配方。
- 设置页新增“工作区”分组，其中的“关闭后保留工作台里的物品”对所有工作台生效。工作区右上角的按钮可以把工作台里的物品退回存储或背包，旁边的箭头决定工作台清空时物品优先退回哪里。
- 装有 Polymorph 时，合成台的配方选择位于工作区右上角。
- 设置页的各个分组之间以细线分隔。

### 开发者

- 附属模组可以添加工作台：用 `StorageWorkbenchTypes` 注册菜单一侧，可以附带在打开存储界面时于服务端判断玩家能否使用；用 `StorageWorkbenches` 注册界面。`ClearableWidget` 与 `RecipeFillWidget` 让工作台可以被清空、由配方查看器填充，`PlayerWorkbenches` 保存每个玩家留在工作台里的物品。
- 附属模组的页面可以设定自己的大小，切换到该页时窗口以动画变到这一大小。

### 破坏性改动

开发者：

- 存储菜单包含了工作区，因此移除了 `DimensionsCraftMenu`、`DimensionsCraftMenuTerminal`、`CraftableMenu` 与 `PlayerCraftingGrid`，请改用 `DimensionsNetMenu` 或 `DimensionsNetTerminalMenu`、`menu.crafting()` 与 `PlayerWorkbenches`。菜单类型 `dimensions_craft_menu` 与 `dimensions_craft_menu_terminal` 由 `dimensions_net_terminal_menu` 取代，`NetMenuType` 现在只有 `NET_MENU` 与 `NET_TERMINAL`。
- 存储菜单需要用 `DimensionsNetMenu.open(player, provider)` 打开；以其他方式打开时不判断工作台能否使用，所有工作台都会出现。
- `BDMenuCommands.recipe` 需要指定目标工作台，并可附带配方 ID。
