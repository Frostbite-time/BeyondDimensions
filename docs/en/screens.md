# Screens

[简体中文](../zh-CN/screens.md) · [All guides](../../README.md#guides)

Other mods can add pages to the screens of Beyond Dimensions (BD), send actions from them to the server and change
what clicks on slots do. BD's screens are built with [CompixelUI](https://github.com/Frostbite-time/CompixelUI) and Compose, and a
page can use BD's own controls.

## Add a page

A page gets a tab in the screen's tab rail, after the screen's own tabs and before Settings. It is registered for a
menu class and appears on every screen of that menu or its subclasses:

| Member of `BdPage<M, S, A>` | Runs on | Does |
| --- | --- | --- |
| `icon` | | The tab's 10×10 pixel icon, such as `BdIcons.Info`, or your own `BdPixelIcon` |
| `title(context)` | Game thread | The tab's tooltip |
| `appliesTo(context)` | Game thread | Whether this screen shows the page; `true` by default |
| `snapshot(context)` | Game thread | The page's state `S`; only reads, and may run several times a tick |
| `handle(context, action)` | Game thread | Handles an action `A` sent by the page; a new snapshot follows |
| `Content(state, scope)` | Compose thread | Draws the page from the state |

Keep the state an immutable value, without menus, stacks or other game objects. In `Content`, `scope.send(action)` passes
an action to `handle`, `scope.open(BdPages.SETTINGS)` switches pages, `scope.shown` tells whether the page is showing and
`scope.slots` draws the menu's slots with `BdSlot`.

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

Register pages on the client, for example in `FMLClientSetupEvent`:

```kotlin
BdPages.register<DimensionsNetMenu>(ResourceLocation.fromNamespaceAndPath("examplemod", "network_info"), NetworkInfoPage)
```

A third argument places the page among other mods' pages: `BdPages.first()`, `last()`, `before(id)` or `after(id)`.

## Send an action to the server

`context.request(action, value)` sends a [CompixelUI menu action](https://github.com/Frostbite-time/CompixelUI/blob/main/docs/en/menu-sync.md)
to the server. Register the action for the menu class with `BdMenuActions`, in your mod's constructor on both client and
server. The handler runs on the server thread with the menu and the player, checks the value itself and returns whether
it applied the action:

```kotlin
val PING: MenuAction<DimensionsNetMenu, Int> =
    MenuAction.of("examplemod.ping", SyncCodecs.INT) { _, player, times ->
        val valid = times in 1..10
        if (valid) player.sendSystemMessage(Component.literal("Ping x$times"))
        valid
    }

BdMenuActions.register(DimensionsNetMenu::class.java, PING)
```

## Change what a click does

A click on a slot in a BD screen goes to the server, where the slot interactions are tried in order; the first one that
applies and performs the click handles it. `SlotInteractions.ids()` lists them in that order, BD's own included, such as
`beyonddimensions:insert`, `beyonddimensions:take` and `beyonddimensions:quick_move`. Register yours relative to one of
them:

```java
// Shift-clicking an upgrade slot moves its item into the player's inventory
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

`applies` only reads; `perform` returns `false` to leave the click to the next interaction. `SlotClick` describes the
click:

| Method | Returns |
| --- | --- |
| `menu()`, `player()`, `slot()` | Where the click happened |
| `kind()` | `STORAGE` (the network grid), `ORDERED` (a slot of a machine's storage), `FLAG` (a filter slot) or `VANILLA` |
| `isLeft()`, `isRight()`, `isMiddle()`, `shift()`, `ctrl()`, `alt()`, `space()`, `doubleClick()` | The button and the keys held |
| `clicked()` | The clicked resource and the amount the server holds |
| `carried()`, `setCarried(stack)` | The stack on the cursor |
| `insert(key, amount, simulate)`, `extract(key, amount, simulate)` | Store in or take from the clicked storage |
