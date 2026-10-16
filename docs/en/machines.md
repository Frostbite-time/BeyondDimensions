# Machine settings

[简体中文](../zh-CN/machines.md) · [All guides](../../README.md#guides)

A machine's settings, such as redstone control or a filter mode, are declared once and saved, synchronized and checked
from that declaration. A block entity that implements `Machine` works with the Machine Configuration Card, which copies
its settings and pastes them onto every connected machine of the same type.

| Type | Role |
| --- | --- |
| `MachineSetting<T>` | One setting: its codec, default, label and editor. Registered in `BDRegistries.MACHINE_SETTING_KEY` |
| `SettingEditor<T>` | The control a setting is edited with, which also decides its valid values: `buttons`, `cycle` or `dropdown` to pick one of the options, `toggle` or `chip` (a short button; adjacent ones share a row) to switch it on and off, `number` for a whole number in a range. The server refuses other values |
| `MachineType` | A kind of machine and its settings, in display order. Registered in `BDRegistries.MACHINE_TYPE_KEY` |
| `MachineSettings` | A machine's values, immutable. Settings it doesn't recognize are kept and saved back |
| `Machine` | Implemented by a machine's block entity |
| `MachineItem` | Implemented by an item that is a machine, such as the Network Magnet; its settings live on the stack |

`BDMachineSettings` holds Beyond Dimensions' own settings, which other machines can use too: `REDSTONE`, `WORKING`,
`POP`, `FUZZY`, `FILTER_FUZZY`, `FILTER`, `RECEIVE`, `SORT`, `COLLECT_ITEMS`, `COLLECT_EXPERIENCE`, `COLLECT_FLUIDS`,
`COLLECT_COMPONENTS`, `RANGE` and others. `BDMachineSettings.side(face)` are the six faces of a side configuration; a
machine type that lists all six shows them as one net on the settings page.

## Declare settings and a machine type

```java
public static final DeferredRegister<MachineSetting<?>> SETTINGS =
        DeferredRegister.create(BDRegistries.MACHINE_SETTING_KEY, "examplemod");
public static final DeferredRegister<MachineType> TYPES =
        DeferredRegister.create(BDRegistries.MACHINE_TYPE_KEY, "examplemod");

public enum Speed { SLOW, NORMAL, FAST }

public static final DeferredHolder<MachineSetting<?>, MachineSetting<Speed>> SPEED = SETTINGS.register("speed",
        () -> MachineSetting.enumeration(Speed.class, Speed.NORMAL)
                .label("ui.examplemod.speed")
                .editor(SettingEditor.buttons(List.of(Speed.values()),
                        speed -> "ui.examplemod.speed." + MachineSetting.lowercase(speed)))
                .build());

public static final DeferredHolder<MachineType, MachineType> PRESS = TYPES.register("press",
        () -> MachineType.builder(ExampleBlocks.PRESS)
                .setting(BDMachineSettings.REDSTONE)
                .setting(SPEED, entry -> entry.hint("ui.examplemod.press.speed.hint"))
                .build());
```

A machine type can change a setting's label, hint, editor or default for its own machines through the `entry` builder,
for example to offer only some of the options with `withOptions` or to show a toggle as a chip. `section(label)` puts
the settings declared after it in a titled group on the settings page.

## Implement Machine

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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.put(SETTINGS, settings.toTag());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        settings = MachineSettings.fromTag(tag.getCompound(SETTINGS));
    }
}
```

`setting(ExampleMachines.SPEED.get())` reads a value, falling back to the type's default, and `setSetting` changes one.
Override `isSettingAvailable` to lock a setting for now; the configuration card then skips it. A machine with more to
copy than its settings, such as filter slots, overrides `exportConfig` and `applyConfig` and calls the default
implementations first.
