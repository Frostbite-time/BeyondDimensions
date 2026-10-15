# 网络与存储

[English](../en/networks.md) · [全部指南](../../README.zh-CN.md#指南)

`com.wintercogs.beyonddimensions.api` 下的接口供其他模组访问网络：存取资源、监听网络事件、在网络上保存自定义数据。本页的接口均在服务端调用。

## 查找网络

| `DimensionsNet` 的方法 | 返回值 |
| --- | --- |
| `getPrimaryNetFromPlayer(player)` | 玩家的主网络；没有时返回 `null` |
| `getAllNetFromPlayer(player)` | 玩家所属的全部网络 |
| `getNetFromId(id)` | 指定 ID 的网络；不存在或已删除时返回 `null` |
| `hasAnyNet(player)`、`hasPrimaryNet(player)` | 玩家是否属于任一网络、是否有主网络 |
| `createNewNetForPlayer(player, capacity, kinds)` | 为玩家创建一个网络，可存 `kinds` 种资源，每种最多 `capacity` 个；玩家已有主网络时直接返回该网络 |

网络对象提供 ID（`getId()`）、名称（`getNetworkName()`，修改用 `setCustomName(name)`）、所有者（`getOwner()`）、管理员（`getManagers()`）和成员（`getPlayers()`）。成员管理使用 `addPlayer`、`addManager`、`removeManager`、`removePlayer` 和 `setOwner`，参数均为 UUID。

## 存取

`net.getUnifiedStorage()` 返回网络的统一存储。所有存取都会经过[存储规则](storage-rules.md)。

| 方法 | 作用 |
| --- | --- |
| `insert(key, amount, simulate)` | 存入至多 `amount` 个，返回未存入的部分（同一种资源） |
| `extract(key, amount, simulate)` | 取出至多 `amount` 个，返回实际取出的部分 |
| `extract(key, amount, simulate, true)` | 同上，但忽略组件：取出网络中数量最多的变体 |
| `extractInto(key, amount, fuzzy, accepts, place)` | 只取出目标能接收的数量，无需取出后再放回 |
| `getStackByKey(key)` | 网络中 `key` 的数量 |
| `getStorage()` | 网络中的全部资源 |
| `setSlotCapacity(amount)`、`setSlotMaxSize(kinds)` | 设置上限；当前值通过 `slotCapacity`、`slotMaxSize` 读取 |

资源以资源键表示，数量与资源键一起封装在 `KeyAmount(key, amount)` 中：

```java
DimensionsNet net = DimensionsNet.getPrimaryNetFromPlayer(player);
if (net != null)
{
    UnifiedStorage storage = net.getUnifiedStorage();
    KeyAmount rest = storage.insert(new ItemStackKey(new ItemStack(Items.DIAMOND)), 64, false);
    KeyAmount water = storage.extract(new FluidStackKey(new FluidStack(Fluids.WATER, 1)), 4000, false);
    KeyAmount energy = storage.extract(EnergyStackKey.INSTANCE, 100_000, false);
}
```

## 事件

以下事件在服务端发布到 `NeoForge.EVENT_BUS`：

| 事件 | 触发时机 |
| --- | --- |
| `DimensionsNetEvent.Created` | 网络创建完成；可在此设置上限或放入初始资源 |
| `DimensionsNetEvent.Destroyed` | 网络已销毁；携带其 ID、名称、内容、成员和挂接数据 |
| `DimensionsNetEvent.OwnerChanged` | 网络所有者变更 |
| `DimensionsNetEvent.MemberChanged` | 玩家加入、退出、被移出、升为管理员或降为成员（见 `getChangeState()`） |
| `NetedBlockEvent.Bound`、`Unbound` | 超越维度的方块绑定到网络或解除绑定 |
| `NetedItemEvent.Bound`、`Unbound` | 玩家将物品绑定到网络或解除绑定 |

合并两个网络时，被并入的网络会被销毁，因此同样会发布 `Destroyed`。

## 在网络上保存数据

`NetDataType` 为每个网络附加一份自定义数据：随网络一起保存，网络合并时按 `merge` 合并，网络销毁时通过 `Destroyed` 提供。注册到 `BDRegistries.NET_DATA_TYPE_KEY`：

```java
public static final DeferredRegister<NetDataType<?>> NET_DATA =
        DeferredRegister.create(BDRegistries.NET_DATA_TYPE_KEY, "examplemod");

public static final Supplier<NetDataType<Integer>> UPGRADES = NET_DATA.register("upgrades",
        () -> NetDataType.builder(() -> 0)
                .codec(Codec.INT)
                .merge(Integer::sum)
                .build());

int upgrades = net.getOrCreateData(UPGRADES.get());
net.updateData(UPGRADES.get(), count -> count + 1);
```

`getExistingData`、`setData` 和 `removeData` 的用法相同。数据应使用不可变值，并通过 `setData` 或 `updateData` 修改，这两个方法会将网络标记为待保存。数据只存在于服务端，客户端如需显示，需自行同步。模组卸载后，其数据原样保留在存档中，重新安装后恢复。
