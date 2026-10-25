# Resource types

[简体中文](../zh-CN/resource-types.md) · [All guides](../../README.md#guides)

Networks store any resource that has a key type. Items, fluids and FE come with Beyond Dimensions (BD), and its
integrations add chemicals, mana and source the same way. Another mod can add its own type in four steps. This
page only outlines them; see the code for the details.

## 1. The key

A key implements `IStackKey<T>`, where `T` is the resource's own stack class. It has a unique type ID, NBT and
network serialization for saving and syncing, conversions from the resource's stacks, a display stack and a renderer.

A resource that is only an amount, like energy or mana, needs much less: a `LongType` for the amount and a single
`LongStackKey`. BD's Botania integration, in `integration/module/botania/storage`, is a complete example of one.

```java
public final class HeatType extends LongType<HeatType>
{
    public HeatType(long amount)
    {
        this.stackCount = amount;
    }

    @Override
    public Component getName()
    {
        return Component.translatable("types.examplemod.heat");
    }

    @Override
    public HeatType getEmpty()
    {
        return new HeatType(0);
    }

    @Override
    public HeatType copy()
    {
        return new HeatType(stackCount);
    }

    @Override
    public HeatType copyWithAmount(long amount)
    {
        return new HeatType(amount);
    }
}
```

`HeatStackKey extends LongStackKey<HeatType>` then gives the type ID (`examplemod:stack_type/heat`), empty
network and NBT serialization, and its renderer; `ManaStackKey` shows each of these.

## 2. The renderer

`getRender()` returns an `IStackRender`, which draws the 16×16 icon and gives the name and tooltip. It is called on the
client only.

## 3. Registration

In common setup:

```java
StackKeyRegistry.registerType(HeatStackKey.INSTANCE);
```

## 4. Capabilities

For pipes and machines to reach the resource, connect it to the capability that other mods use for it:

| Hook | Connects |
| --- | --- |
| `CapabilityHelper.BlockCapabilityMap.put(id, capability)` | The block capability for this type |
| `CapabilityHelper.ItemCapabilityMap.put(id, capability)` | The item capability for this type |
| `CapabilityHelper.registerUSHandler(key, storage -> handler)` | A network, seen through that capability |
| `CapabilityHelper.registerStackTypedHandler(key, slots -> handler)` | BD's slot storages, such as a Network Interface's, seen through it |
| `StackHandlerWrapperHelper.stackWrappers.put(id, handler -> wrapper)` | Another mod's handler, seen as an `IStackHandlerWrapper` that BD's machines can work with |

To carry the type into Applied Energistics 2, add its conversions to `AEHelper.ISTACK_TO_AEKEY_MAP` and
`AEKEY_TO_STACK_TYPE_MAP`.
