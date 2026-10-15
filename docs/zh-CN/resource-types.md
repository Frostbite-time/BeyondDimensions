# 资源类型

[English](../en/resource-types.md) · [全部指南](../../README.zh-CN.md#指南)

网络可以存储任何具有资源键类型的资源。物品、流体和 FE 由超越维度提供，化学品、魔力、魔源和灵魂由联动模块以相同方式添加。其他模组添加新的资源类型分为以下四步。本页仅简要说明流程，具体请自行查看代码。

## 1. 资源键

资源键实现 `IStackKey<T>`，其中 `T` 是该资源自身的堆叠类。资源键需要提供唯一的类型 ID、用于保存和同步的编解码器、从资源堆叠转换的方法、用于显示的堆叠以及渲染器。

仅由数量构成的资源（如能量、魔力）实现起来更简单：用一个 `LongType` 表示数量，再配合一个单例 `LongStackKey`。超越维度的植物魔法联动（`integration/module/botania/storage`）是一个完整示例。

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

随后实现 `HeatStackKey extends LongStackKey<HeatType>`，提供类型 ID（`examplemod:stack_type/heat`）、无字段的 `MapCodec`、空的网络与 NBT 序列化，以及渲染器，写法可参考 `ManaStackKey`。

## 2. 渲染器

`getRender()` 返回一个 `IStackRender`，负责绘制 16×16 图标，并提供名称和提示文字。该方法仅在客户端调用。

## 3. 注册

在通用初始化阶段注册：

```java
StackKeyRegistry.registerType(HeatStackKey.INSTANCE);
```

## 4. 能力

管道和机器通过能力访问资源，因此需要将新资源接入其他模组所使用的能力：

| 接口 | 作用 |
| --- | --- |
| `CapabilityHelper.BlockCapabilityMap.put(id, capability)` | 指定该资源对应的方块能力 |
| `CapabilityHelper.ItemCapabilityMap.put(id, capability)` | 指定该资源对应的物品能力 |
| `CapabilityHelper.registerUSHandler(key, storage -> handler)` | 将网络包装为该能力 |
| `CapabilityHelper.registerStackTypedHandler(key, slots -> handler)` | 将超越维度的槽位存储（如网络接口内的存储）包装为该能力 |
| `StackHandlerWrapperHelper.stackWrappers.put(id, handler -> wrapper)` | 将其他模组的处理器包装为 `IStackHandlerWrapper`，供超越维度的机器操作 |

如需让该资源进入应用能源 2 或精致存储，将转换函数加入 `AEHelper.ISTACK_TO_AEKEY_MAP` 和 `AEKEY_TO_STACK_TYPE_MAP`，或 `RSHelper.ISTACK_TO_RSKEY_MAP` 和 `RSKEY_TO_STACK_TYPE_MAP`。
