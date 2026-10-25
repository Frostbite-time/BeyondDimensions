# KubeJS

[简体中文](../zh-CN/kubejs.md) · [All guides](../../README.md#guides)

With [KubeJS](https://kubejs.com), scripts can limit how much a network holds, add starting contents, make upgrade items
and customize [storage rules](storage-rules.md). These scripts usually go in `kubejs/server_scripts`.

## Common classes

| Class | Use |
| --- | --- |
| `DimensionsNet` | Networks: find a player's network, create one. See [Networks and storage](networks.md) |
| `UnifiedStorage` | The storage of a network |
| `ItemStackKey`, `FluidStackKey`, `EnergyStackKey` | Resource keys |
| `KeyAmount` | A key with an amount, returned by `insert` and `extract` |
| `UnifiedStorageRules`, `InsertPlan`, `ExtractPlan` | [Storage rules](storage-rules.md) |

A key names one kind of resource, NBT included, without an amount:

```js
new ItemStackKey(Item.of('minecraft:diamond'))
new FluidStackKey(Fluid.of('minecraft:water'))
EnergyStackKey.INSTANCE
```

## Limit and fill new networks

A new network holds up to 2,147,483,647 kinds of resources and up to 9,223,372,036,854,775,807 of each, and scripts can
limit both. The `DimensionsNetEvent$Created` event fires after a network is created, whether by the Dimensional Network
Generator, a command or another mod. KubeJS handles Forge events in startup scripts, so this one goes in
`kubejs/startup_scripts`:

```js
// New networks hold 64 kinds of resources, 100,000 of each, and start with some bread
ForgeEvents.onEvent('com.wintercogs.beyonddimensions.api.event.dimensionnet.DimensionsNetEvent$Created', event => {
  const storage = event.getNet().getUnifiedStorage()
  storage.setSlotMaxSize(64)
  storage.setSlotCapacity(100000)
  storage.insert(new ItemStackKey(Item.of('minecraft:bread')), 32, false)
})
```

## Upgrades

`slotMaxSize` and `slotCapacity` read the current limits:

```js
// A Nether Star adds 64 kinds and ten times the amount to the player's primary network
ItemEvents.rightClicked('minecraft:nether_star', event => {
  const net = DimensionsNet.getPrimaryNetFromPlayer(event.player)
  if (net == null) return
  const storage = net.getUnifiedStorage()
  storage.setSlotMaxSize(storage.slotMaxSize + 64)
  storage.setSlotCapacity(storage.slotCapacity * 10)
  event.item.shrink(1)
})
```

## Storage rules

`UnifiedStorageRules` works as described in [Storage rules](storage-rules.md):

```js
// Storing a diamond costs 1,000 FE
UnifiedStorageRules.insert('kubejs:diamond_fee', new ItemStackKey(Item.of('minecraft:diamond')),
  request => InsertPlan.store().take(EnergyStackKey.INSTANCE, 1000))
```

Scripts can't remove other mods' rules. To override one, register yours before it by passing
`UnifiedStorageRules.before('<its id>')` as a fourth argument; `UnifiedStorageRules.insertIds()` and `extractIds()` list
the rules in order.
