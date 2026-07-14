简介  
---
> 本项目当前支持 Minecraft 1.21.1；详细支持状态请参阅文末版本表。

超越维度是一个提供存储与实用工具的模组。它引入了一个“维度网络”———作为存储系统，其支持物品、流体、FE和通用机械的化学品。其兼具优秀的存储性能与简单易用的存储方式，可以轻松胜任大部分场景下的存储需求。

如何开始？  
---
你需要击败凋灵，或者用任何其他方法得到一个下界之星，用它合成一个不稳定时空碎片，放在背包中一个小时，你将获得一个稳态时空碎片。使用它合成一个维度网络发生器，使用以创建一个“维度网络”。随后只需按下快捷键（默认为“O”），即可打开存储界面。

存储系统  
---
- 容量：默认的存储空间即为最大，可存储21亿种不同的物品或其他资源，每种资源可存储2^63^-1单位，即java中long类型最大值。无论是存储的种类上限、单种的存储数量还是增加存储容量的方法，均可利用kubeJS进行魔改；
- 性能：存储系统经过了长时间的优化，作者自己对其进行了性能测试，无论是连接到AE还是仅使用本模组，性能表现都相当不错，具体测试表现可以查看[性能测试部分](#性能测试)；
- 联机：存储系统通过玩家权限信息来绑定玩家，同一存储可以共享给多个玩家。或者通过物品形式的终端进行共享。

UI界面  
---
易用的存储界面：模组提供的界面与AE和RS等主流模组的交互逻辑基本一致。

- 自带合成栏
- 支持多种搜索方式：直接输入文本可同时搜索名称、工具提示、模组id中任意一项匹配的物品，以‘#’强制要求工具提示匹配、以‘@’强制要求模组id匹配。
- 自带拼音搜索功能，且安装通用拼音搜索后会优先使用它的拼音搜索
- 显示行数可自定义
- 支持多种排序：按存入时间、按存储数量、按名称、按模组id

通用的槽位系统：模组提供的所有界面中，任何槽位都能够存储所有模组支持的资源类型，并支持快速交互。

快速操作容器：鼠标携带对应容器右键槽位，即可快速存入/取出容器内资源。如：用潜影盒、精妙背包或者任何容器右键槽位，可以快速把物品收入容器，右键空槽位，可以快速取出并放入槽位；对于流体、通用机械化学品甚至是FE能量都支持上述操作。

自动化支持与实用工具  
---
模组提供了一些用于自动化的方块和实用工具，这些方块和工具都需要绑定到网络才能使用，并且大都支持红石控制，存储可以通过它们与其他模组进行交互。

- 维度网络通道：暴露维度网络的整个存储空间，你可以通过管道、漏斗从网络中输入输出、通过AE、RS的存储总线来读取网络内容、通过精妙背包的卸货升级快速转移物品；
- 维度网络能量通道：与维度网络通道是几乎一样的，不过只用于FE能量的交互，从方块的UI中可以看到当前FE能量的变化情况。支持弹出模式，以21亿每tick的速率向周围输出FE能量；
- 网络接口：类似ME接口的功能，有标记槽和存储槽，会尝试将存储的资源放入网络，尝试将从网络中提取标记的资源，同时支持弹出模式；
- 网络泵：主动抽取周围容器中的资源，将其存入维度网络，有过滤槽，支持黑白名单模式；
- 网络漏斗：吸收周围的掉落物甚至是流体，直接存入维度网络。其范围可以调整，从2格到整个区块，工作范围越大，工作间隔越久。有过滤槽，支持黑白名单；
- 网络熔炉：可以标记原料和燃料，自动从网络中提取对应资源并冶炼，支持使用FE和熔岩流体作为燃料。输出的成品支持弹出到周围容器，也支持直接存入网络；
- 网络喂食器：当玩家的饥饿值掉到其设置的阈值后，会从网络取出标记的食物来喂食玩家。不会消耗无限火腿或永恒牛排等“无限”的食物。可用于Curios护符栏；
- 网络磁铁：与网络漏斗类似，但其可随身携带，仅吸收掉落物时的工作频率高于网络漏斗。可通过快捷键开关，可用于Curios护符栏；
- 主手物品快速转移：鼠标中键点击一个方块即可从网络中取出一组，蹲下中键可以将主手物品送回网络，该按键可以作为快捷键修改。

对其他模组的支持  
---
- 支持JEI和EMI的拖拽标记和快速配方转移；
- 使用JEI和EMI拖拽标记时，可以识别AE2的通用包裹堆栈内容物；
- 为AE2添加了专用的存储元件，用于读取绑定的网络内容。其性能比使用存储总线高10倍以上；
- Mek的化学品存储支持；
- 合成界面支持多态合成（Polymorph）；
- 物品终端可以放入Curios的护符槽中，且同时安装本模组和Curios后，将额外提供一个护符槽；
- KubeJS支持，可以调用模组API中的部分类，从而自定义如何创建网络、创建时的容量大小、如何扩容等；
- 模组的UI界面禁用了一键背包整理Next（IPN）的功能，模组的UI界面经过大量修改，无法与其一起使用。如需快速转移物品请使用鼠标手势。



附属制作以及kubeJS魔改帮助
---

### 附属制作  
可存储资源类型在代码上相当容易拓展，只需实现以下接口并注册资源类型和对应的操作方法：
- [[IStackType]](https://github.com/Frostbite-time/BeyondDimensions/blob/1.21.1/src/main/java/com/wintercogs/beyonddimensions/Api/DataBase/Stack/IStackType.java) 使模组可以识别和存储此资源类型；
- [[IStackHandlerWrapper]](https://github.com/Frostbite-time/BeyondDimensions/blob/1.21.1/src/main/java/com/wintercogs/beyonddimensions/Api/DataBase/StackHandlerWrapper/IStackHandlerWrapper.java) 使模组可以主动操作存储了此资源的其他模组容器；
- [[CapabilityHelper.BlockCapabilityMap]](https://github.com/Frostbite-time/BeyondDimensions/blob/1.21.1/src/main/java/com/wintercogs/beyonddimensions/Api/Registry/CapabilityHelper.java) 使对应资源种类能被其他模组的管道和存储总线获取；
- [[CapabilityHelper.ItemCapabilityMap]](https://github.com/Frostbite-time/BeyondDimensions/blob/1.21.1/src/main/java/com/wintercogs/beyonddimensions/Api/Registry/CapabilityHelper.java) 使UI右键快速可以容器中存取此资源；
- [[UnifiedStorage.typedHandlerMap]](https://github.com/Frostbite-time/BeyondDimensions/tree/1.21.1/src/main/java/com/wintercogs/beyonddimensions/Api/DataBase/Storage) 使得维度网络能操作对应资源；
- [[StackTypedHandler.typedHandlerMap]](https://github.com/Frostbite-time/BeyondDimensions/tree/1.21.1/src/main/java/com/wintercogs/beyonddimensions/Api/DataBase/Handler) 使得网络接口等方块所用的存储空间能操作对应资源。

以下两个为AE专用存储元件的兼容，即使不注册也能通过存储总线读取：
- [[AEHelper.ISTACK_TO_AEKEY_MAP]](https://github.com/Frostbite-time/BeyondDimensions/blob/1.21.1/src/main/java/com/wintercogs/beyonddimensions/Integration/AE/AEHelper.java) 使AE能通过专用存储元件读取模组资源
- [[AEHelper.AEKEY_TO_STACK_TYPE_MAP]](https://github.com/Frostbite-time/BeyondDimensions/blob/1.21.1/src/main/java/com/wintercogs/beyonddimensions/Integration/AE/AEHelper.java) 使AE能通过专用存储元件操作模组资源

注册方式可参考[此处](https://github.com/Frostbite-time/BeyondDimensions/blob/1.21.1/src/main/java/com/wintercogs/beyonddimensions/BeyondDimensions.java)，注册方式可能会在未来变动。

### 有关kubeJS魔改的帮助  
要对模组的容量进行修改，首先要禁用维度网络生成器的合成配方，随后根据下述函数提供自己的网络生成方法和扩容方法即可。


| 类名| 方法签名 | 返回值类型 | 静态方法 | 用途 |
| -------- | -------- | -------- | -------- | -------- |
| DimensionsNet | createNewNetForPlayer(Player player, long defaultSlotCapability, int defaultSlotMaxSize) | DimensionsNet | 是 |为指定玩家创建一个维度网络，并指定存储容量。defaultSlotCapability指单种物品最大容量、defaultSlotMaxSize指种类上限 |
| | getNetFromId(int id，MinecraftServer dataProvider) | DimensionsNet | 是 |根据数字id获取对应的维度网络 |
| | getNetFromPlayer(Player player) | DimensionsNet | 是 | 根据玩家获取对应的维度网络 |
| | getUnifiedStorage() | UnifiedStorage | 否 |获取当前网络的存储空间 |
| UnifiedStorage | setSlotCapacity(long capacity) | void | 否 | 设置单种物品的存储上限 |
| | setSlotMaxSize(int maxSize) | void | 否 | 设置可容纳的种类上限 |

此外，模组还为kubeJS暴露了一些其他的类，具体可以看[此处](https://github.com/Frostbite-time/BeyondDimensions/blob/1.21.1/src/main/java/com/wintercogs/beyonddimensions/Integration/KubeJS/BD_KubeJSPlugin.java)。
UnifiedStorage类中还有相当多的其他方法，可以让你直接修改玩家的存储内容。

模组的api有完整的中文注释，查看[此处](https://github.com/Frostbite-time/BeyondDimensions/tree/1.21.1/src/main/java/com/wintercogs/beyonddimensions/Api/DataBase)。

其他
----
### 性能测试
主要测试了三种情况，所有测试情况均在同一机器，相同后台环境下得出，利用Spark模组进行记录。测试时使用随机生成的1319种物品，三次测试使用的物品均完全相同。

1. 使用此模组存储物品并通过Mek管道进行输入输出，同时打开维度网络界面测试菜单同步性能；
2. 利用维度ME磁盘将模组连接到AE并通过Mek管道进行输入输出，同时打开无线终端，测试菜单同步性能；
3. 使用AE原生存储元件存储同样的物品（共使用21个64k存储元件）并通过Mek管道进行输入输出，同时打开无线终端，测试菜单同步性能。

第一种情况测试所用方块为模组自带的维度网络通道与网络接口，所得数据仅反映模组本身性能表现，不能与第二种、第三种测试环境对比。

第二种和第三种测试情况使用AE的输出总线和ME接口，均关闭了频道限制便于测试，两种测试情况仅AE所使用的存储来源不同，二者性能可以进行对比。


结果：

| 测试环境 | tps | mspt(中位数) | mspt(95%位数) | 区块数量 | 实体数量 |
| -------- | -------- | -------- | -------- | -------- | -------- |
| 仅超越维度 | 20 | 3.67 | 4.86 | 2601 | 13 |
| AE连接超越维度 | 20 | 2.97 | 3.37 | 2601 | 13 |
| 仅AE | 20 | 2.7 | 3.08 | 2601 | 13 |

spark概要和测试场景布置的具体信息请查看[此处](https://github.com/Frostbite-time/BeyondDimensions/tree/1.21.1/files/PerformanceTestReport)。



### 更新计划（大饼）  
- 将原版物品和流体存储最大上限更改为64位整型（已完成）；
- 经验存储及配套工具；
- 新生魔艺的魔源存储及配套工具；
- RS专用存储元件；
- 添加发展流程，而非一开始给予无上限的存储；
- 更多的原生自动化工具；
- 其他实用工具和装备。

### 支持版本
| MC版本 | 加载器 | 对应模组版本 | 状态 |
| -------- | -------- | -------- | -------- |
| 1.21.1 | neoforge | 0.3.0+ | 持续更新 |
| 1.20.1 | forge/neoforge | 0.3.0+ | 持续更新 |
| 1.12.2 | forge | 0.1.7.4 | 停更，仅修复重大bug |

在更新计划完成之前，暂时不会支持其他版本。