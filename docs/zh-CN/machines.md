# 机器设置

[English](../en/machines.md) · [全部指南](../../README.zh-CN.md#指南)

机器的各项设置（如红石控制、过滤模式）只需声明一次，保存、同步和校验均依据该声明进行。实现了 `Machine` 的方块实体支持机器配置卡：复制其设置，并粘贴到所有相连的同类机器上。

| 类型 | 作用 |
| --- | --- |
| `MachineSetting<T>` | 一项设置：编解码器、默认值、名称和编辑方式。注册到 `BDRegistries.MACHINE_SETTING_KEY` |
| `SettingEditor<T>` | 编辑设置用的控件，同时决定合法取值：`buttons`（分段按钮）、`cycle`（循环按钮）、`dropdown`（下拉菜单）从选项中选一个；`toggle`（开关）、`chip`（短按钮，同组相邻的排成一行）切换开启与关闭；`number`（数值框）输入范围内的整数。服务端拒绝不合法的值 |
| `MachineType` | 一类机器及其设置，按显示顺序排列。注册到 `BDRegistries.MACHINE_TYPE_KEY` |
| `MachineSettings` | 一台机器的设置值，不可变。无法识别的设置原样保留，保存时写回 |
| `Machine` | 由机器的方块实体实现 |
| `MachineItem` | 由本身作为机器的物品实现，如网络磁铁；设置保存在物品堆叠上 |

`BDMachineSettings` 包含超越维度自身的设置，其他机器也可以直接使用：`REDSTONE`、`WORKING`、`POP`、`FUZZY`、`FILTER_FUZZY`、`FILTER`、`RECEIVE`、`SORT`、`COLLECT_ITEMS`、`COLLECT_EXPERIENCE`、`COLLECT_FLUIDS`、`COLLECT_COMPONENTS`、`RANGE` 等。`BDMachineSettings.side(face)` 是面配置的六个面；机器类型列出全部六个面时，设置页将它们显示为一张展开图。

## 声明设置和机器类型

```java
public static final DeferredRegister<MachineSetting<?>> SETTINGS =
        DeferredRegister.create(BDRegistries.MACHINE_SETTING_KEY, "examplemod");
public static final DeferredRegister<MachineType> TYPES =
        DeferredRegister.create(BDRegistries.MACHINE_TYPE_KEY, "examplemod");

public enum Speed { SLOW, NORMAL, FAST }

public static final RegistryObject<MachineSetting<Speed>> SPEED = SETTINGS.register("speed",
        () -> MachineSetting.enumeration(Speed.class, Speed.NORMAL)
                .label("ui.examplemod.speed")
                .editor(SettingEditor.buttons(List.of(Speed.values()),
                        speed -> "ui.examplemod.speed." + MachineSetting.lowercase(speed)))
                .build());

public static final RegistryObject<MachineType> PRESS = TYPES.register("press",
        () -> MachineType.builder(ExampleBlocks.PRESS)
                .setting(BDMachineSettings.REDSTONE)
                .setting(SPEED, entry -> entry.hint("ui.examplemod.press.speed.hint"))
                .build());
```

机器类型可以通过 `entry` 覆盖某项设置在该类机器上的名称、说明、编辑方式或默认值，例如用 `withOptions` 只开放部分选项，或把开关换成短按钮。`section(label)` 将其后声明的设置归入一组，设置页在组前显示标题。

## 实现 Machine

```java
public class PressBlockEntity extends BlockEntity implements Machine
{
    private MachineSettings settings = MachineSettings.EMPTY;

    @Override
    public MachineType machineType()
    {
        return ExampleMachines.PRESS.get();
    }

    @Override
    public MachineSettings settings()
    {
        return settings;
    }

    @Override
    public void setSettings(MachineSettings settings)
    {
        this.settings = settings;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.put(SETTINGS, settings.toTag());
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        settings = MachineSettings.fromTag(tag.getCompound(SETTINGS));
    }
}
```

读取设置使用 `setting(ExampleMachines.SPEED.get())`，未保存过时返回机器类型给出的默认值；修改设置使用 `setSetting`。某项设置暂时不可修改时，重写 `isSettingAvailable`，配置卡也会跳过该设置。如果机器除设置外还有其他需要复制的内容（如过滤槽），重写 `exportConfig` 和 `applyConfig`，并先调用默认实现。
