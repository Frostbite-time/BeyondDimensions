# 界面

[English](../en/screens.md) · [全部指南](../../README.zh-CN.md#指南)

其他模组可以向超越维度的界面添加页面、从页面向服务器发送动作，以及修改点击槽位的行为。超越维度的界面基于 [CompixelUI](https://github.com/Frostbite-time/CompixelUI) 和 Compose 实现，页面中可以直接使用超越维度的控件。

## 添加页面

每个页面在界面左侧的页签栏中占一个页签，位于界面自带的页签之后、设置页签之前。页面按菜单类注册，会显示在该菜单（包括其子类）的所有界面上：

| `BdPage<M>` 的成员 | 作用 |
| --- | --- |
| `icon` | 页签的 10×10 像素图标，如 `BdIcons.Info`，也可以是自定义的 `BdPixelIcon` |
| `title(context)` | 页签的悬停提示，打开界面时读取一次 |
| `appliesTo(context)` | 当前界面是否显示该页面，默认显示 |
| `size(context)` | 页面的大小，默认与主页面一样大，见[页面大小](#页面大小) |
| `Content(context, scope)` | 在游戏线程绘制页面 |

在 `Content` 中通过 `gameState { ... }` 读取会变化的游戏数据（应返回不可变值，不要包含菜单、物品堆叠等游戏对象），通过 `scope.gameThread.post { ... }` 提交修改或服务端请求。`scope.open(BdPages.SETTINGS)` 切换页面，`scope.shown` 表示该页面是否正在显示，`scope.slots` 配合 `BdSlot` 绘制菜单的槽位。

```kotlin
object NetworkInfoPage : BdPage<DimensionsNetMenu> {
    data class Info(val name: String, val id: Int)

    override val icon = BdIcons.Info

    override fun title(context: BdPageContext<DimensionsNetMenu>) = Component.literal("Network info")

    @Composable
    override fun ColumnScope.Content(context: BdPageContext<DimensionsNetMenu>, scope: BdPageScope) {
        val info by gameState { Info(context.menu.networkName().string, context.menu.networkId()) }
        BdSectionLabel("Network")
        OreText("${info.name} #${info.id}", color = Bd.colors[BdColors.text])
        BdChip({ scope.gameThread.post { context.request(PING, 3) } }, enabled = scope.shown) {
            OreText("Ping", color = Bd.colors[BdColors.text])
        }
    }
}
```

页面仅在客户端注册，例如在 `FMLClientSetupEvent` 中：

```kotlin
BdPages.register<DimensionsNetMenu>(ResourceLocation("examplemod", "network_info"), NetworkInfoPage)
```

第三个参数指定页面在其他模组页面中的位置：`BdPages.first()`、`last()`、`before(id)` 或 `after(id)`。

### 页面大小

`size(context)` 返回 `BdPageSize`，打开界面时取一次。切换到这一页时，窗口以动画变到它的大小：

| `BdPageSize` | 页面大小 |
| --- | --- |
| `Main` | 与主页面一样大，为默认值 |
| `Fixed(width, height)` | 固定宽高，单位为界面像素，包括页面四周的留白；屏幕放不下时缩到屏幕内，内容在页面中滚动 |
| `Content(maxWidth, maxHeight)` | 随内容变化，可以设上限；内容的最外层需要有确定的大小，不能使用 `fillMaxSize` 或 `weight` |

页面比左侧页签栏所需的高度矮时，窗口仍会留出放下所有页签的高度，但不会超过主页面的高度。

## 向服务器发送动作

`context.request(action, value)` 将一个 [CompixelUI 菜单动作](https://github.com/Frostbite-time/CompixelUI/blob/main/docs/zh-CN/menu-sync.md)发送到服务器。动作需通过 `BdMenuActions` 注册到菜单类，并在模组构造函数中于客户端和服务端两侧注册。处理函数在服务器线程运行，接收菜单和玩家，需自行校验收到的值，并返回是否执行了该动作：

```kotlin
val PING: MenuAction<DimensionsNetMenu, Int> =
    MenuAction.of("examplemod.ping", SyncCodecs.INT) { _, player, times ->
        val valid = times in 1..10
        if (valid) player.sendSystemMessage(Component.literal("Ping x$times"))
        valid
    }

BdMenuActions.register(DimensionsNetMenu::class.java, PING)
```

## 修改点击行为

在超越维度的界面中点击槽位时，点击会发送到服务器，按顺序尝试已注册的槽位交互，由第一个适用并处理了该点击的交互生效。`SlotInteractions.ids()` 按该顺序列出全部交互，包括超越维度自身的交互，如 `beyonddimensions:insert`、`beyonddimensions:take` 和 `beyonddimensions:quick_move`。注册交互时，需指定其相对于已有交互的位置：

```java
// 按住 Shift 点击升级槽时，将其中的物品移入玩家背包
SlotInteractions.register(new ResourceLocation("examplemod", "upgrade_quick_move"),
        new SlotInteractions.Interaction()
        {
            @Override
            public boolean applies(SlotClick click)
            {
                return click.shift() && click.slot() instanceof UpgradeSlot;
            }

            @Override
            public boolean perform(SlotClick click)
            {
                ItemStack moving = click.slot().getItem().copy();
                click.player().getInventory().add(moving);
                click.slot().set(moving);
                return true;
            }
        },
        SlotInteractions.before(new ResourceLocation("beyonddimensions", "quick_move_vanilla")));
```

`applies` 只能读取状态；`perform` 返回 `false` 表示不处理，交给下一个交互。`SlotClick` 描述本次点击：

| 方法 | 返回值 |
| --- | --- |
| `menu()`、`player()`、`slot()` | 点击所在的菜单、玩家和槽位 |
| `kind()` | `STORAGE`（网络存储格）、`ORDERED`（机器存储中的一格）、`FLAG`（过滤槽）或 `VANILLA` |
| `isLeft()`、`isRight()`、`isMiddle()`、`shift()`、`ctrl()`、`alt()`、`space()`、`doubleClick()` | 按下的鼠标键及按住的修饰键 |
| `clicked()` | 被点击的资源及其在服务端的实际数量 |
| `carried()`、`setCarried(stack)` | 鼠标上持有的物品 |
| `insert(key, amount, simulate)`、`extract(key, amount, simulate)` | 向被点击的存储存入，或从中取出 |
