# 界面

[English](../en/screens.md) · [全部指南](../../README.zh-CN.md#指南)

其他模组可以向超越维度的界面添加页面、从页面向服务器发送动作，以及修改点击槽位的行为。超越维度的界面基于 [CompixelUI](https://github.com/Frostbite-time/CompixelUI) 和 Compose 实现，页面中可以直接使用超越维度的控件。

## 添加页面

每个页面在界面左侧的页签栏中占一个页签，位于界面自带的页签之后、设置页签之前。页面按菜单类注册，会显示在该菜单（包括其子类）的所有界面上：

| `BdPage<M, S, A>` 的成员 | 运行线程 | 作用 |
| --- | --- | --- |
| `icon` | | 页签的 10×10 像素图标，如 `BdIcons.Info`，也可以是自定义的 `BdPixelIcon` |
| `title(context)` | 游戏线程 | 页签的悬停提示 |
| `appliesTo(context)` | 游戏线程 | 当前界面是否显示该页面，默认显示 |
| `snapshot(context)` | 游戏线程 | 生成页面状态 `S`；只读，每刻可能调用多次 |
| `handle(context, action)` | 游戏线程 | 处理页面发送的动作 `A`，处理后重新生成状态 |
| `Content(state, scope)` | Compose 线程 | 根据状态绘制页面 |

状态应为不可变值，不应包含菜单、物品堆叠等游戏对象。在 `Content` 中，`scope.send(action)` 将动作交给 `handle`，`scope.open(BdPages.SETTINGS)` 切换页面，`scope.shown` 表示该页面是否正在显示，`scope.slots` 配合 `BdSlot` 绘制菜单的槽位。

```kotlin
object NetworkInfoPage : BdPage<DimensionsNetMenu, NetworkInfoPage.State, NetworkInfoPage.Ping> {
    data class State(val name: String, val id: Int)
    data object Ping

    override val icon = BdIcons.Info

    override fun title(context: BdPageContext<DimensionsNetMenu>) = Component.literal("Network info")

    override fun snapshot(context: BdPageContext<DimensionsNetMenu>) =
        State(context.menu.networkName().string, context.menu.networkId())

    override fun handle(context: BdPageContext<DimensionsNetMenu>, action: Ping) {
        context.request(PING, 3)
    }

    @Composable
    override fun ColumnScope.Content(state: State, scope: BdPageScope<Ping>) {
        BdSectionLabel("Network")
        OreText("${state.name} #${state.id}", color = Bd.colors[BdColors.text])
        BdChip({ scope.send(Ping) }, enabled = scope.shown) {
            OreText("Ping", color = Bd.colors[BdColors.text])
        }
    }
}
```

页面仅在客户端注册，例如在 `FMLClientSetupEvent` 中：

```kotlin
BdPages.register<DimensionsNetMenu>(ResourceLocation.fromNamespaceAndPath("examplemod", "network_info"), NetworkInfoPage)
```

第三个参数指定页面在其他模组页面中的位置：`BdPages.first()`、`last()`、`before(id)` 或 `after(id)`。

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
SlotInteractions.register(ResourceLocation.fromNamespaceAndPath("examplemod", "upgrade_quick_move"),
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
        SlotInteractions.before(ResourceLocation.fromNamespaceAndPath("beyonddimensions", "quick_move_vanilla")));
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
