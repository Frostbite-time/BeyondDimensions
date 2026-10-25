# KubeJS

[English](../en/kubejs.md) · [全部指南](../../README.zh-CN.md#指南)

使用 [KubeJS](https://kubejs.com) 可以通过脚本限制网络存储量、添加初始物资、制作升级道具，以及自定义[存储规则](storage-rules.md)。这些脚本通常放在 `kubejs/server_scripts`。

## 常见类

| 类 | 用途 |
| --- | --- |
| `DimensionsNet` | 维度网络：查询玩家的网络、创建网络，见[网络与存储](networks.md) |
| `UnifiedStorage` | 网络的统一存储 |
| `ItemStackKey`、`FluidStackKey`、`EnergyStackKey` | 资源键 |
| `KeyAmount` | 资源及其数量，`insert` 和 `extract` 的返回值 |
| `UnifiedStorageRules`、`InsertPlan`、`ExtractPlan` | [存储规则](storage-rules.md) |

资源键表示一种资源（包含 NBT），不含数量：

```js
new ItemStackKey(Item.of('minecraft:diamond'))
new FluidStackKey(Fluid.of('minecraft:water'))
EnergyStackKey.INSTANCE
```

## 限制新网络并放入初始物资

新网络最多可存 2,147,483,647 种资源，每种最多 9,223,372,036,854,775,807 个，可以通过脚本限制。网络创建完成后会触发 `DimensionsNetEvent$Created` 事件，无论网络由维度网络发生器、命令还是其他模组创建。KubeJS 在启动脚本中处理 Forge 事件，因此这段脚本放在 `kubejs/startup_scripts`：

```js
// 新网络最多存 64 种资源，每种最多 10 万个，并放入 32 个面包
ForgeEvents.onEvent('com.wintercogs.beyonddimensions.api.event.dimensionnet.DimensionsNetEvent$Created', event => {
  const storage = event.getNet().getUnifiedStorage()
  storage.setSlotMaxSize(64)
  storage.setSlotCapacity(100000)
  storage.insert(new ItemStackKey(Item.of('minecraft:bread')), 32, false)
})
```

## 升级

`slotMaxSize` 和 `slotCapacity` 为网络当前的上限：

```js
// 右键下界之星：玩家主网络的种类上限增加 64，单种资源的上限乘以 10
ItemEvents.rightClicked('minecraft:nether_star', event => {
  const net = DimensionsNet.getPrimaryNetFromPlayer(event.player)
  if (net == null) return
  const storage = net.getUnifiedStorage()
  storage.setSlotMaxSize(storage.slotMaxSize + 64)
  storage.setSlotCapacity(storage.slotCapacity * 10)
  event.item.shrink(1)
})
```

## 存储规则

`UnifiedStorageRules` 的用法见[存储规则](storage-rules.md)：

```js
// 每存入一颗钻石消耗 1000 FE
UnifiedStorageRules.insert('kubejs:diamond_fee', new ItemStackKey(Item.of('minecraft:diamond')),
  request => InsertPlan.store().take(EnergyStackKey.INSTANCE, 1000))
```

脚本无法移除其他模组的规则。如需覆盖某条规则，将自己的规则注册在它之前：第四个参数传入 `UnifiedStorageRules.before('<规则 ID>')`。`UnifiedStorageRules.insertIds()` 和 `extractIds()` 按顺序列出当前的规则。
