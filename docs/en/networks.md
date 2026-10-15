# Networks and storage

[简体中文](../zh-CN/networks.md) · [All guides](../../README.md#guides)

The API under `com.wintercogs.beyonddimensions.api` lets other mods reach networks, store and take resources, follow
network events and keep their own data on a network. Everything on this page runs on the server.

## Find a network

| Method on `DimensionsNet` | Returns |
| --- | --- |
| `getPrimaryNetFromPlayer(player)` | The player's primary network, or `null` |
| `getAllNetFromPlayer(player)` | Every network the player belongs to |
| `getNetFromId(id)` | The network with this ID, or `null` if there is none or it was deleted |
| `hasAnyNet(player)`, `hasPrimaryNet(player)` | Whether the player belongs to a network, or has a primary one |
| `createNewNetForPlayer(player, capacity, kinds)` | A new network owned by the player that holds `kinds` kinds of resources and up to `capacity` of each; the player's primary network if they already have one |

A network knows its ID (`getId()`), name (`getNetworkName()`, `setCustomName(name)`), owner (`getOwner()`), managers
(`getManagers()`) and members (`getPlayers()`), and changes membership with `addPlayer`, `addManager`, `removeManager`,
`removePlayer` and `setOwner`, all by UUID.

## Store and take

`net.getUnifiedStorage()` is the network's storage. Inserts and extracts pass through the
[storage rules](storage-rules.md).

| Method | Does |
| --- | --- |
| `insert(key, amount, simulate)` | Stores up to `amount`; returns the rest, as the same resource |
| `extract(key, amount, simulate)` | Takes up to `amount`; returns what was taken |
| `extract(key, amount, simulate, true)` | The same, ignoring components: takes the variant the network holds most of |
| `extractInto(key, amount, fuzzy, accepts, place)` | Takes only what the destination accepts, without putting anything back |
| `getStackByKey(key)` | How much of `key` the network holds |
| `getStorage()` | Everything the network holds |
| `setSlotCapacity(amount)`, `setSlotMaxSize(kinds)` | The limits; `slotCapacity` and `slotMaxSize` read them |

Resources are named by keys, and amounts travel with them as `KeyAmount(key, amount)`:

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

## Events

Posted on `NeoForge.EVENT_BUS`, on the server:

| Event | When |
| --- | --- |
| `DimensionsNetEvent.Created` | A network was created; set its limits or fill it here |
| `DimensionsNetEvent.Destroyed` | A network was destroyed; carries its ID, name, contents, members and data |
| `DimensionsNetEvent.OwnerChanged` | A network got a new owner |
| `DimensionsNetEvent.MemberChanged` | A player joined, left, was removed, promoted or demoted (`getChangeState()`) |
| `NetedBlockEvent.Bound`, `Unbound` | A Beyond Dimensions block was bound to or unbound from a network |
| `NetedItemEvent.Bound`, `Unbound` | A player bound an item to a network, or unbound it |

Merging two networks destroys the one merged in, so it posts `Destroyed`.

## Keep data on a network

A `NetDataType` stores a value of your own with each network: saved with it, merged when networks merge and passed to
`Destroyed`. Register it in `BDRegistries.NET_DATA_TYPE_KEY`:

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

`getExistingData`, `setData` and `removeData` work the same way. Keep values immutable and change them through
`setData` or `updateData`, which mark the network for saving. Data stays on the server; send what the client needs
yourself. When your mod is removed, its data stays in the save untouched and comes back with the mod.
