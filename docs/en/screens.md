# Screens

[简体中文](../zh-CN/screens.md) · [All guides](../../README.md#guides)

Other mods can add pages to the screens of Beyond Dimensions (BD), register expandable side panels on the storage
screen, send actions to the server and change what clicks on slots do. BD's screens are built with
[CompixelUI](https://github.com/Frostbite-time/CompixelUI) and Compose, and pages and side panels can use BD's own
controls.

## Add a page

A page gets a tab in the screen's tab rail, after the screen's own tabs and before Settings. It is registered for a
menu class and appears on every screen of that menu or its subclasses:

| Member of `BdPage<M>` | Does |
| --- | --- |
| `icon` | The tab's 10×10 pixel icon, such as `BdIcons.Info`, or your own `BdPixelIcon` |
| `title(context)` | The tab's tooltip, read once when the screen opens |
| `appliesTo(context)` | Whether this screen shows the page; `true` by default |
| `size(context)` | The page's size; the main page's size by default, see [Page size](#page-size) |
| `Content(context, scope)` | Draws the page on the game thread |

Read changing game data inside `Content` with `gameState { ... }` (returning immutable values, without menus, stacks or
other game objects), and post mutations or server requests with `scope.gameThread.post { ... }`. In `Content`,
`scope.open(BdPages.SETTINGS)` switches pages, `scope.shown` tells whether the page is showing and `scope.slots` draws
the menu's slots with `BdSlot`.

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

Register pages on the client, for example in `FMLClientSetupEvent`:

```kotlin
BdPages.register<DimensionsNetMenu>(ResourceLocation.fromNamespaceAndPath("examplemod", "network_info"), NetworkInfoPage)
```

A third argument places the page among other mods' pages: `BdPages.first()`, `last()`, `before(id)` or `after(id)`.

### Page size

`size(context)` returns a `BdPageSize` and is read once when the screen opens. When the page opens, the window
animates to its size:

| `BdPageSize` | Size |
| --- | --- |
| `Main` | The main page's size; the default |
| `Fixed(width, height)` | A fixed size in GUI pixels, including the page's padding. On a smaller screen it shrinks to fit and the content scrolls |
| `Content(maxWidth, maxHeight)` | Follows the content, up to the optional limits. The outermost content needs a definite size, so no `fillMaxSize` or `weight` |

A page shorter than the tab rail still leaves room for every tab, but never more than the main page's height.

## Storage side panels

The expandable area on the right side of the storage screen (shared with the built-in category editor) accepts panels
registered by other mods, allowing add-ons to display substantial custom content alongside the storage grid without
opening a separate page. Add-ons decide when and how to open or close their panels. Only one side panel is open at a
time; opening one expands the window to the right, narrows the storage grid columns to fit, and hosts the panel inside a
vertically scrollable `Column`:

| Member of `StorageSidePanel` | Does |
| --- | --- |
| `appliesTo(context)` | Whether this storage screen mounts the panel; `true` by default, checked once when the screen opens |
| `width(context)` | Content width in dp (excluding the divider and spacing); defaults to `127` and is clamped to `80..220` |
| `onOpen(context)` | Called on the game thread when the panel opens |
| `onClose(context)` | Called on the game thread when the panel closes, is replaced by another panel, or the screen closes |
| `Content(context, scope)` | Draws the panel on the game thread |

Inside `Content`, read changing game data with `gameState { ... }` and post mutations or server requests with
`scope.gameThread.post { ... }`. `scope.openPanel(id)` switches to another side panel, `scope.closePanel()` closes the
current panel, and `scope.shown` reports whether the panel is currently shown on the main page.
`context.remember(key) { ... }` keeps per-screen state for the panel, and `context.request(action, value)` sends a menu
action to the server.

```kotlin
val DETAILS_PANEL = ResourceLocation.fromNamespaceAndPath("examplemod", "details")

object DetailsSidePanel : StorageSidePanel {
    override fun width(context: StorageSidePanelContext): Int = 150

    @Composable
    override fun ColumnScope.Content(context: StorageSidePanelContext, scope: StorageSidePanelScope) {
        val name by gameState { context.menu.networkName().string }
        BdSectionLabel("Details")
        OreText(name, color = Bd.colors[BdColors.text])
        BdChip({ scope.closePanel() }, enabled = scope.shown) {
            OreText("Close", color = Bd.colors[BdColors.text])
        }
    }
}
```

Register side panels on the client (for example in your client mod constructor or `FMLClientSetupEvent`):

```kotlin
StorageSidePanels.register(DETAILS_PANEL, DetailsSidePanel)
```

Control the panel from your own buttons or callbacks on the client thread (for example inside `scope.gameThread.post`):

- `StorageSidePanels.open(screen, id)`: opens the panel; if another tab such as Settings or an injected page is open, the screen switches back to the main page automatically.
- `StorageSidePanels.close(screen)`: closes the currently open side panel.
- `StorageSidePanels.toggle(screen, id)`: toggles the panel and returns whether it is now open.
- `StorageSidePanels.current(screen)`: returns the currently open panel ID or `null` (the built-in category editor is `StorageSidePanels.CATEGORY_EDITOR`).

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
