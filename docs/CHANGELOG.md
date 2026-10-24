## English

0.8.1 rebuilds every screen on CompixelUI and adds custom category tabs, side configuration for machines, the Machine Configuration Talisman and the Stock Emitter. It requires CompixelUI and Kotlin; read the breaking changes at the end before updating.

### Interface

- Every screen is rebuilt on [CompixelUI](https://www.curseforge.com/projects/1715758): a window with a tab rail on the left and main and settings pages, in the Beyond Dimensions Casing style.
- Screens open and close with short animations.
- Four color schemes: Beyond Dimensions Casing (default), Glass (translucent), Vanilla and Brass. The palette button on each window opens an editor for every color, and resource packs can add schemes.
- Text uses CompixelUI's pixel font. Fluid and chemical amounts under one bucket show as `<1` instead of a decimal.
- The Energy Network screen shows the average flow and bar charts of the stored energy and of input and output over the last half minute. The server records them for every network, so they are complete as soon as the screen opens.

### Storage

- Create your own category tabs with an icon, a name, a search rule and hand-picked resources. Right-click a tab, or its row in the tab list, to edit, move or delete it, or drag it to reorder. Categories are saved per player and work in every network.
- New search syntax: `~` matches resource types (for example `~fluid`), `%` matches durability (for example `%<50`) and `&` matches extra data (for example `&enchant`). `*` matches the ID of every resource, not only items, and `\` makes any following character literal, including `@`, `#`, `-` and `|`.
- Right-click a stored resource with an empty hand to take one, take a chosen amount or add it to a category. Turn off "Right-click menu" on the settings page to take half instead, as in vanilla.
- Space + left-click moves everything at once: on your inventory or hotbar, every item in that part; on a stored resource, as much of it as possible; on a machine's slots, its whole storage. Shift + double-click moves every identical item from your inventory or the machine's slots.
- While holding a container (bucket, tank, battery, backpack and others), left-click a stored resource to fill the container and right-click to empty it into the network. Hold Shift to transfer as much as possible.
- While Shift is held the grid doesn't rearrange; resources you empty stay in place showing 0 until you release it.
- The grid can be wider than nine columns: set its columns and maximum rows on the settings page, and it narrows to fit the screen.
- Each player has their own crafting grid, saved on the server and shared by the network screen (O key), the Network Terminal and the Portable Network Terminal. With the new "Keep grid on close" setting its items stay for next time; otherwise they return to the storage or your inventory, as you choose.
- Space + left-click the crafting result to keep crafting until your inventory is full, or a crafting grid slot to empty the grid into the storage or your inventory, as you choose.
- Tab only moves the focus into and out of the search box.
- Switching between storage, crafting and the Primary Network Switcher no longer moves the mouse pointer to the center.

### Machines

- New **Machine Configuration Talisman**, crafted from paper, redstone and a Space-Time Ingot. Right-click a machine to copy its settings, Shift + right-click to paste, and Ctrl + right-click to paste to it and every connected machine of the same kind (up to 256). For item machines, hold the talisman in the off hand and the machine in the main hand.
- Side configuration for the Network Interface, the Network Furnace, Blast Furnace and Smoker, the Dimensional Network Energy Pathway and the Network Pump: set each face, relative to the machine's front, to input, output, both or disabled. The Network Pump's faces are input or disabled, and it only pulls through input faces.
- The Network Interface, Dimensional Network Energy Pathway and Network Pump face the player when placed.
- Fuzzy match switches for the filter slots of the Network Pump, Network Hopper and Network Magnet.
- Network Magnet: its settings are grouped into collecting, filtering and destination. Choose where picked-up items go (network only, network first, inventory first or inventory only), and mark exceptions that go to the other place instead.
- Network Feeder: new "No waste" mode, which feeds only when none of the food is wasted.
- New block: **Stock Emitter**. It emits a redstone signal while the network holds at least, or less than, a set amount of a marked resource, with optional fuzzy matching.
- A Network Terminal placed on a floor or ceiling faces the player.
- The Schematic Interface has a front, its blueprint face, which faces the player when placed.

### Compatibility

- Jade shows what Network Interfaces and Network Furnaces hold, one line per resource, instead of listing their empty slots and tanks. Dimensional Network Pathways no longer list the whole network's storage.
- KubeJS server scripts can add storage rules with `UnifiedStorageRules`, `InsertPlan` and `ExtractPlan`.

### Fixes

- Resources that can't be read for now are no longer discarded, for example items and fluids from a mod you've temporarily removed. They are kept as Error Data items and restored once the mod is back.
- Block updates send only the data the client needs.
- Deleted networks no longer accept resources. Storage screens close when their network is deleted or, for screens opened with a key, when you are no longer a member.
- Fixed duplicating items by filling or emptying containers on machine slots, and an exploit that took items out of virtual slots through vanilla click packets.
- When the network can't hold all the experience deposited with the XP Rod, the rest is refunded at the correct rate.
- Fixed Ars Nouveau setting a network's source to its full capacity instead of the requested amount.
- Taking resources out of the network checks what the destination accepts first, instead of taking them and putting the rest back.
- A recipe that would create an oversized Compressed Matter Orb is refused with a message, and orbs nest at most 16 levels deep.
- Fixed stretched textures on the edges of the Network Pump.
- The Unstable Space-Time Fragment uses the configured transfer time instead of always taking an hour.
- Translations completed and corrected for all ten languages.
- Fuzzy extraction is more efficient and takes the variant the network holds the most of, and networks no longer mark themselves for saving every tick.

### For modpack makers and developers

- New guides in English and Chinese, for modpacks and servers (KubeJS, storage rules, color schemes, commands and configuration) and for add-on developers (networks and storage, resource types, machine settings, screens). See the [README](https://github.com/Frostbite-time/BeyondDimensions/blob/1.20.1/README.md#guides).
- Published to the [wintercogs Maven repository](https://github.com/Frostbite-time/BeyondDimensions/blob/1.20.1/README.md#for-developers) together with its sources.
- New APIs: storage rules (`UnifiedStorageRules`, `InsertPlan`, `ExtractPlan`); data attached to networks, saved with them or kept in memory (`NetDataType`, `BDRegistries.NET_DATA_TYPE_KEY`); tasks run in every network's tick (`NetTickTasks`); machine settings (`MachineSetting`, `MachineType`, `Machine`, `MachineItem`, `SettingEditor`, `BDMachineSettings`); pages and server actions on Beyond Dimensions' screens (`BdPages`, `BdPage`, `BdMenuActions`); slot interactions (`SlotInteractions`, `SlotClick`); `UnifiedStorage.extractInto`; `IStackHandler.getFuzzyMatches`; `IStackKey.getSourceId`.

### Breaking changes

For players, modpacks and servers:

- CompixelUI and Kotlin are now required: install [Kotlin for Forge](https://www.curseforge.com/minecraft/mc-mods/kotlin-for-forge), or use the CompixelUI file whose name ends in `-with-kotlin`.
- Forge 47.2.18 or newer is required.
- Every screen is new. The built-in resource type tabs are gone; create category tabs instead.
- The configuration was reorganized, and moved or renamed options return to their defaults:
  - the storage screen settings moved from the common config to the client config, `beyonddimensions-client.toml`;
  - the Network Interface options moved from the common config to the server config, under `interface`, and `interfaceBlockedInputStackTypes` became `interface.blocked_input_types`;
  - `fragmentTransferTime` and `crystalGenerateTime` became `space_time.fragment_transfer_time` and `space_time.crystal_generate_time`.
- The Network Terminal no longer has one crafting grid shared by all players; each player has their own. Items left in an old Network Terminal or Portable Network Terminal go to the storage, then to the inventory of the player who opens it, and the rest drops at their feet. An old Network Terminal broken before being opened drops them.
- By default the crafting grid empties when you close the screen; turn on "Keep grid on close" to keep it.
- While holding a container, left-click now fills it and right-click empties it.
- Middle-click no longer stores or takes resources in Beyond Dimensions' screens. The XP Rod retrieves experience with a left-click on liquid experience instead.
- The Shift + Z key was removed; recipe viewer search sync is a setting on the storage screen.
- Network Interfaces, Dimensional Network Energy Pathways and Network Pumps placed before this version face north.
- Resource packs: full blocks now use one texture per face (`textures/block/<block>/<face>.png`, with `on` and `off` folders for lit or powered states), the Network Terminal uses a JSON model instead of OBJ, and the old screen textures are gone.
- KubeJS: `UnifiedStorageBeforeInsertHandler` and `UnifiedStorageBeforeExtractHandler` were removed; use `UnifiedStorageRules`, `InsertPlan` and `ExtractPlan` in server scripts.
- KubeJS: the bindings take the classes' own names. `IStackTypedHandler`, `StackTypedHandler`, `IStackType`, `ItemStackType`, `FluidStackType` and `EnergyStackType` are now `IStackHandler`, `StackHandler`, `IStackKey`, `ItemStackKey`, `FluidStackKey` and `EnergyStackKey`, and `KeyAmount` is new.

For developers:

- Removed `ButtonState`, `UnifiedStorageBeforeInsertHandler` and `UnifiedStorageBeforeExtractHandler`; storage hooks are now rules in `UnifiedStorageRules`.
- `IStackKey.deserializeNBTCommon` is lenient: keys that cannot be read become Error Data instead of failing. `IStackKey.deserializeNBTStrict` keeps the old behavior.
- `DimensionsNet.getNetFromPlayer` is deprecated for removal; use `getPrimaryNetFromPlayer`.
- `NetedBlockEntity` syncs only client data: `getUpdateTag`, `getUpdatePacket`, `handleUpdateTag` and `onDataPacket` are final. Override `saveClientData` and `loadClientData`, and call `syncToClient` when that data changes.
- Machines keep their settings as registered `MachineSetting`s instead of separate fields.
- The old screens, packets and widgets were removed. Screens use CompixelUI and menus sync through its menu sync; extend them with `BdPages`, `BdMenuActions` and `SlotInteractions`.
- `AbstractUnorderedStackHandler.unzipMatterBall` was removed; Compressed Matter Orbs unpack through the built-in storage rule `beyonddimensions:uncompress_matter_ball`.

## 中文

0.8.1 将所有界面改用 CompixelUI 重做，并新增自定义分类标签、机器面配置、机器配置符和存量发信器。本版需要 CompixelUI 和 Kotlin，更新前请先阅读文末的破坏性改动。

### 界面

- 所有界面改用 [CompixelUI](https://www.curseforge.com/projects/1715758) 重做：窗口左侧为页签栏，右侧为主页面和设置页，采用超维机壳风格。
- 界面打开和关闭时有简短的动画。
- 提供超维机壳（默认）、玻璃（半透明）、原版、黄铜四种配色。点击窗口上的调色板按钮可以修改任意颜色，资源包可以添加配色。
- 文字改用 CompixelUI 的像素字体。不足一桶的流体和化学品显示为 `<1`，不再显示小数。
- 能量网络界面以柱状图显示近半分钟的存量与收支，并显示平均收支。柱状图的数据由服务端为每个网络记录，打开界面时就是完整的。

### 存储界面

- 支持自定义分类标签，可设置图标、名称、搜索式和手动挑选的资源。右键标签或标签列表中对应的一行可以编辑、移动或删除，也可以拖动排序。分类按玩家保存，在所有网络中通用。
- 新增搜索写法：`~` 匹配资源类型（如 `~fluid`），`%` 匹配耐久（如 `%<50`），`&` 匹配额外数据（如 `&enchant`）。`*` 可以匹配所有资源的 ID，不再只限于物品；`\` 后的任意字符都按原样搜索，包括 `@`、`#`、`-` 和 `|`。
- 空手右键存储中的资源，可以取出一个、取出指定数量或加入分类。在设置页关闭“右键打开菜单”后，改为像原版一样取出一半。
- 空格 + 左键整批转移：点背包或快捷栏时转移该部分的全部物品，点存储中的资源时转移尽可能多的数量，点机器槽位时转移该机器存储的全部资源。Shift + 双击转移背包或机器槽位中所有完全相同的物品。
- 手持容器（桶、储罐、电池、背包等）时，左键存储中的资源装入容器，右键将容器中的内容排入网络；按住 Shift 尽可能多地转移。
- 按住 Shift 时存储格不重新排列，取空的资源留在原位显示为 0，松开后再整理。
- 存储格可以超过 9 列：在设置页设置列数和最多行数，屏幕较小时自动收窄。
- 合成格改为每个玩家各一份，保存在服务器上，由 O 键打开的网络界面、网络终端和便携网络终端共用。开启新增的“关闭后保留合成格”后物品会留到下次；未开启时按设置退回存储或背包。
- 空格 + 左键点合成结果连续合成，直到背包放满；点合成格则清空合成格，物品按设置退回存储或背包。
- Tab 键只用于进入和退出搜索框。
- 在存储、合成和主网络切换器之间切换时，鼠标指针不再跳到屏幕中央。

### 机器

- 新增**机器配置符**，由纸、红石和时空锭合成。右键机器复制设置，Shift + 右键粘贴，Ctrl + 右键粘贴到该机器及所有相连的同类机器（最多 256 台）。物品形式的机器需副手持配置符、主手持机器操作。
- 网络接口、网络熔炉、网络高炉、网络烟熏炉、维度网络能量通道和网络泵新增面配置：以机器正面为基准，每个面可设为输入、输出、双向或禁用。网络泵的面只能设为输入或禁用，且只从输入面抽取。
- 网络接口、维度网络能量通道和网络泵放置时正面朝向玩家。
- 网络泵、网络漏斗和网络磁铁的过滤槽新增模糊匹配开关。
- 网络磁铁的设置分为收集、过滤和去向三组；可以选择物品去向（仅网络、优先网络、优先背包、仅背包），并标记例外物品，改送另一处。
- 网络喂食器新增“不浪费”模式，食物恢复的饱食度能全部吃下时才喂食。
- 新增方块**存量发信器**：网络中标记资源的数量不少于或少于设定值时发出红石信号，可开启模糊匹配。
- 网络终端放在地面或天花板上时朝向玩家。
- 蓝图接口新增正面（图纸面），放置时朝向玩家。

### 模组联动

- Jade 显示网络接口和网络熔炉中存放的资源，同种资源合并为一项，不再列出空槽位和空储罐；维度网络通道不再列出整个网络的资源。
- KubeJS 服务端脚本可以通过 `UnifiedStorageRules`、`InsertPlan` 和 `ExtractPlan` 添加存储规则。

### 修复

- 现在不会丢弃暂时无法解析的资源，例如暂时移除的模组中的物品和流体。它们会保存为“错误数据”物品，模组装回后自动还原。
- 方块更新只发送客户端需要的数据。
- 已删除的网络不再接收资源。网络被删除时，或通过快捷键打开存储的玩家已不是成员时，存储界面会关闭。
- 修复在机器槽位上用容器装入或排出时复制物品的问题，以及通过原版点击数据包从虚拟槽位凭空取出物品的漏洞。
- 用经验棒存入经验而网络存不下时，剩余部分按正确的比例退还。
- 修复新生魔艺设置网络魔源时，数量被设为容量上限而非请求数量的问题。
- 从网络取出资源时先确认目标能接收的数量，不再取出后再把剩余部分放回。
- 合成会生成数据量过大的压缩物质球时拒绝并提示；压缩物质球最多嵌套 16 层。
- 修复网络泵边缘的贴图拉伸。
- 不稳定时空碎片改为按配置的转化时间计时，不再固定为一小时。
- 补全并修正十种语言的翻译。
- 模糊取出性能更佳，并优先取网络中数量最多的变体；网络不再每刻标记为需要保存。

### 整合包作者与开发者

- 新增中英文指南，面向整合包和服务器（KubeJS、存储规则、配色、命令与配置）和附属开发（网络与存储、资源类型、机器设置、界面），见 [README](https://github.com/Frostbite-time/BeyondDimensions/blob/1.20.1/README.zh-CN.md#%E6%8C%87%E5%8D%97)。
- 发布到 [wintercogs Maven 仓库](https://github.com/Frostbite-time/BeyondDimensions/blob/1.20.1/README.zh-CN.md#%E5%BC%80%E5%8F%91%E8%80%85)，附带源码包。
- 新增 API：存储规则（`UnifiedStorageRules`、`InsertPlan`、`ExtractPlan`）；网络挂接数据，可随网络保存或只在内存中（`NetDataType`、`BDRegistries.NET_DATA_TYPE_KEY`）；在每个网络每刻运行的任务（`NetTickTasks`）；机器设置（`MachineSetting`、`MachineType`、`Machine`、`MachineItem`、`SettingEditor`、`BDMachineSettings`）；超越维度界面的附属页面与服务器动作（`BdPages`、`BdPage`、`BdMenuActions`）；槽位交互（`SlotInteractions`、`SlotClick`）；`UnifiedStorage.extractInto`；`IStackHandler.getFuzzyMatches`；`IStackKey.getSourceId`。

### 破坏性改动

玩家、整合包与服务器：

- 现在需要 CompixelUI 和 Kotlin：安装 [Kotlin for Forge](https://www.curseforge.com/minecraft/mc-mods/kotlin-for-forge)，或使用文件名以 `-with-kotlin` 结尾的 CompixelUI。
- 需要 Forge 47.2.18 或更新版本。
- 所有界面均已重做。内置的资源类型标签已移除，可改用自定义分类标签。
- 配置已重新整理，移动或更名的选项会恢复默认值：
  - 存储界面的设置从通用配置移到客户端配置 `beyonddimensions-client.toml`；
  - 网络接口的选项从通用配置移到服务端配置的 `interface` 下，`interfaceBlockedInputStackTypes` 改为 `interface.blocked_input_types`；
  - `fragmentTransferTime` 和 `crystalGenerateTime` 改为 `space_time.fragment_transfer_time` 和 `space_time.crystal_generate_time`。
- 网络终端不再有所有玩家共用的合成格，改为每个玩家各一份。旧版网络终端和便携网络终端中留下的物品，在打开时依次送回存储和打开者的背包，放不下的掉落在其脚下；未打开就被拆除的旧网络终端会掉出这些物品。
- 合成格默认在关闭界面时清空，开启“关闭后保留合成格”即可保留。
- 手持容器时，改为左键装入、右键排出。
- 在超越维度的界面中，中键不再存入或取出资源；经验棒改为左键点击经验流体取出经验。
- 移除 Shift + Z 快捷键，配方查看器搜索同步改为存储界面中的设置。
- 此前放置的网络接口、维度网络能量通道和网络泵正面朝北。
- 资源包：完整方块的贴图改为每个面一个文件（`textures/block/<方块>/<面>.png`，点亮或通电的状态分 `on` 和 `off` 文件夹），网络终端改用 JSON 模型，旧的界面贴图已删除。
- KubeJS：移除 `UnifiedStorageBeforeInsertHandler` 和 `UnifiedStorageBeforeExtractHandler`，请在服务端脚本中改用 `UnifiedStorageRules`、`InsertPlan` 和 `ExtractPlan`。
- KubeJS：绑定改用类本身的名称，`IStackTypedHandler`、`StackTypedHandler`、`IStackType`、`ItemStackType`、`FluidStackType` 和 `EnergyStackType` 改为 `IStackHandler`、`StackHandler`、`IStackKey`、`ItemStackKey`、`FluidStackKey` 和 `EnergyStackKey`，并新增 `KeyAmount`。

开发者：

- 移除 `ButtonState`、`UnifiedStorageBeforeInsertHandler` 和 `UnifiedStorageBeforeExtractHandler`，存储钩子改为 `UnifiedStorageRules` 中的规则。
- `IStackKey.deserializeNBTCommon` 改为宽松解析：无法解析的资源键变为错误数据，不再报错；`IStackKey.deserializeNBTStrict` 保留原有行为。
- `DimensionsNet.getNetFromPlayer` 标记为将要移除，请改用 `getPrimaryNetFromPlayer`。
- `NetedBlockEntity` 只向客户端同步客户端数据：`getUpdateTag`、`getUpdatePacket`、`handleUpdateTag` 和 `onDataPacket` 改为 final。请重写 `saveClientData` 和 `loadClientData`，并在这些数据变化时调用 `syncToClient`。
- 机器设置改为注册的 `MachineSetting`，不再使用各自的字段。
- 移除旧的界面、数据包和控件。界面基于 CompixelUI，菜单通过它的菜单同步；扩展界面请使用 `BdPages`、`BdMenuActions` 和 `SlotInteractions`。
- 移除 `AbstractUnorderedStackHandler.unzipMatterBall`；压缩物质球改由内置存储规则 `beyonddimensions:uncompress_matter_ball` 拆开。
