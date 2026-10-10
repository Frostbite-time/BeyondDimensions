package com.wintercogs.beyonddimensions.common.menu.machine;

import com.wintercogs.beyonddimensions.api.machine.Machine;
import com.wintercogs.beyonddimensions.api.machine.MachineSetting;
import com.wintercogs.beyonddimensions.api.machine.MachineSettings;
import com.wintercogs.beyonddimensions.api.machine.MachineType;
import com.wintercogs.beyonddimensions.api.machine.SettingEditor;
import com.wintercogs.beyonddimensions.api.registry.BDRegistries;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;
import dev.compixel.forge.sync.MenuAction;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.sync.state.SyncCodec;
import dev.compixel.sync.state.SyncSchema;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 机器设置在菜单里的同步与修改：一个同步字段带上机器类型、设置值与此刻不可修改的设置，
 * 一个动作提交单项修改，服务端按机器类型里的编辑声明校验。各机器菜单不再逐项声明
 */
public final class MachineSettingsSync
{
    private static final int MAX_NBT_BYTES = 1 << 16;
    private static final int MAX_SETTINGS = 256;
    private static final ResourceLocation NO_MACHINE = ResourceLocation.fromNamespaceAndPath("beyonddimensions", "none");

    /**
     * 菜单里同步的设置
     *
     * @param locked 此刻不可修改的设置
     */
    public record Snapshot(ResourceLocation type, MachineSettings settings, Set<ResourceLocation> locked)
    {
        public Snapshot
        {
            locked = Set.copyOf(locked);
        }

        public @Nullable MachineType machineType()
        {
            return BDRegistries.MACHINE_TYPES.get(type);
        }

        public boolean isLocked(MachineSetting<?> setting)
        {
            ResourceLocation id = BDRegistries.MACHINE_SETTINGS.getKey(setting);
            return id == null || locked.contains(id);
        }
    }

    public static final Snapshot EMPTY = new Snapshot(NO_MACHINE, MachineSettings.EMPTY, Set.of());

    /**
     * 客户端提交的单项修改
     */
    public record Change(ResourceLocation setting, Tag value)
    {
    }

    public static final SyncCodec<Snapshot> SNAPSHOT_CODEC = SyncCodec.of("beyonddimensions:machine_settings/1",
            (out, value) -> {
                out.writeUTF(value.type().toString());
                NbtIo.write(value.settings().toTag(), out);
                out.writeInt(value.locked().size());
                for (ResourceLocation id : value.locked())
                    out.writeUTF(id.toString());
            },
            in -> {
                ResourceLocation type = parse(in.readUTF());
                MachineSettings settings = MachineSettings.fromTag(NbtIo.read(in, NbtAccounter.create(MAX_NBT_BYTES)));
                int count = in.readInt();
                if (count < 0 || count > MAX_SETTINGS)
                    throw new IOException("Too many locked machine settings: " + count);
                Set<ResourceLocation> locked = new LinkedHashSet<>();
                for (int i = 0; i < count; i++)
                    locked.add(parse(in.readUTF()));
                return new Snapshot(type, settings, locked);
            });

    public static final SyncCodec<Change> CHANGE_CODEC = SyncCodec.of("beyonddimensions:machine_setting_change/1",
            (out, value) -> {
                out.writeUTF(value.setting().toString());
                CompoundTag holder = new CompoundTag();
                holder.put("value", value.value());
                NbtIo.write(holder, out);
            },
            in -> {
                ResourceLocation setting = parse(in.readUTF());
                Tag value = NbtIo.read(in, NbtAccounter.create(MAX_NBT_BYTES)).get("value");
                if (value == null)
                    throw new IOException("Machine setting change without a value");
                return new Change(setting, value);
            });

    private MachineSettingsSync()
    {
    }

    private static ResourceLocation parse(String text) throws IOException
    {
        ResourceLocation id = ResourceLocation.tryParse(text);
        if (id == null)
            throw new IOException("Invalid id: " + text);
        return id;
    }

    /**
     * 给菜单的同步 schema 加上设置字段
     */
    public static <M extends BDBaseMenu & MachineMenu> SyncSchema.Builder<M> field(SyncSchema.Builder<M> builder)
    {
        return builder.field("machine.settings", SNAPSHOT_CODEC,
                m -> m.player.level().isClientSide() ? m.machineSettings().snapshot() : snapshot(m.machine(), m.player.isSpectator()),
                (m, value) -> m.machineSettings().set(value));
    }

    /**
     * 修改单项设置的动作；每个机器菜单各声明一个并挂到自己的同步上
     */
    public static <M extends BDBaseMenu & MachineMenu> MenuAction<M, Change> action()
    {
        return MenuAction.of("machine.setting", CHANGE_CODEC, (menu, player, change) -> apply(menu.machine(), player, change));
    }

    /**
     * 服务端：一台机器此刻的设置；旁观者不能修改任何设置
     */
    public static Snapshot snapshot(@Nullable Machine machine, boolean spectator)
    {
        if (machine == null)
            return EMPTY;
        MachineType type = machine.machineType();
        ResourceLocation typeId = BDRegistries.MACHINE_TYPES.getKey(type);
        if (typeId == null)
            return EMPTY;
        Set<ResourceLocation> locked = new LinkedHashSet<>();
        for (MachineType.Entry<?> entry : type.settings())
        {
            MachineSetting<?> setting = entry.setting();
            if (spectator || !machine.isSettingAvailable(setting))
                locked.add(BDRegistries.MACHINE_SETTINGS.getKey(setting));
        }
        return new Snapshot(typeId, machine.settings(), locked);
    }

    private static boolean apply(@Nullable Machine machine, ServerPlayer player, Change change)
    {
        if (machine == null || player.isSpectator())
            return false;
        MachineSetting<?> setting = BDRegistries.MACHINE_SETTINGS.get(change.setting());
        MachineType.Entry<?> entry = setting == null ? null : machine.machineType().entry(setting);
        if (entry == null || !machine.isSettingAvailable(setting))
            return false;
        return applyEntry(entry, machine, change.value());
    }

    private static <T> boolean applyEntry(MachineType.Entry<T> entry, Machine machine, Tag raw)
    {
        Optional<T> value = entry.setting().codec().parse(NbtOps.INSTANCE, raw).result();
        if (value.isEmpty() || !entry.accepts(value.get()))
            return false;
        machine.setSetting(entry.setting(), value.get());
        return true;
    }

    /**
     * 客户端：把设置改成编辑声明里的第 index 个值（开关的 0 为打开、1 为关闭）
     */
    public static <M extends BDBaseMenu & MachineMenu> boolean request(M menu, MenuSync<M> sync, MenuAction<M, Change> action,
                                                                       ResourceLocation settingId, int index)
    {
        Snapshot snapshot = menu.machineSettings().snapshot();
        MachineType type = snapshot.machineType();
        MachineSetting<?> setting = BDRegistries.MACHINE_SETTINGS.get(settingId);
        MachineType.Entry<?> entry = type == null || setting == null ? null : type.entry(setting);
        if (entry == null || snapshot.isLocked(setting))
            return false;
        Tag value = encodeChoice(entry, index);
        return value != null && sync.request(action, new Change(settingId, value)).queued();
    }

    private static <T> @Nullable Tag encodeChoice(MachineType.Entry<T> entry, int index)
    {
        T value = valueAt(entry.editor(), index);
        return value == null ? null : entry.setting().codec().encodeStart(NbtOps.INSTANCE, value).result().orElse(null);
    }

    /**
     * 编辑声明里的第 index 个值：选择的第 index 项，开关的 0 为打开、1 为关闭
     */
    public static <T> @Nullable T valueAt(SettingEditor<T> editor, int index)
    {
        return switch (editor)
        {
            case SettingEditor.Choice<T> choice -> index >= 0 && index < choice.options().size() ? choice.options().get(index) : null;
            case SettingEditor.Toggle<T> toggle -> index == 0 ? toggle.on() : index == 1 ? toggle.off() : null;
        };
    }

    /**
     * 客户端的设置镜像，由同步写入
     */
    public static final class Mirror
    {
        private volatile Snapshot snapshot = EMPTY;

        public Snapshot snapshot()
        {
            return snapshot;
        }

        void set(Snapshot snapshot)
        {
            this.snapshot = snapshot;
        }
    }
}
