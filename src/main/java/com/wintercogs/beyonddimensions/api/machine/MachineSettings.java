package com.wintercogs.beyonddimensions.api.machine;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.*;
import com.wintercogs.beyonddimensions.api.registry.BDRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 一台机器的设置值，不可变。没有存过的设置取默认值；认不出或解析不了的设置原样保留并写回
 */
public final class MachineSettings
{
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final MachineSettings EMPTY = new MachineSettings(Map.of(), Map.of());

    /**
     * 保存为 {设置 id: 值}
     */
    public static final Codec<MachineSettings> CODEC = new Codec<>()
    {
        @Override
        public <T> DataResult<Pair<MachineSettings, T>> decode(DynamicOps<T> ops, T input)
        {
            DataResult<MapLike<T>> map = ops.getMap(input);
            if (map.result().isEmpty())
                return DataResult.error(() -> "Machine settings are not a map: " + input);
            Map<MachineSetting<?>, Object> values = new LinkedHashMap<>();
            Map<String, Tag> unresolved = new LinkedHashMap<>();
            map.result().get().entries().forEach(entry -> {
                String key = ops.getStringValue(entry.getFirst()).result().orElse(null);
                if (key == null)
                    return;
                ResourceLocation id = ResourceLocation.tryParse(key);
                MachineSetting<?> setting = id == null ? null : BDRegistries.MACHINE_SETTINGS.get(id);
                if (setting == null || !decodeInto(setting, ops, entry.getSecond(), values))
                    unresolved.put(key, ops.convertTo(NbtOps.INSTANCE, entry.getSecond()));
            });
            return DataResult.success(Pair.of(new MachineSettings(values, unresolved), input));
        }

        @Override
        public <T> DataResult<T> encode(MachineSettings input, DynamicOps<T> ops, T prefix)
        {
            RecordBuilder<T> builder = ops.mapBuilder();
            input.unresolved.forEach((key, raw) -> builder.add(key, NbtOps.INSTANCE.convertTo(ops, raw)));
            input.values.forEach((setting, value) -> {
                ResourceLocation id = BDRegistries.MACHINE_SETTINGS.getKey(setting);
                if (id == null)
                {
                    LOGGER.error("Machine setting {} is not registered in {} and cannot be saved", setting, BDRegistries.MACHINE_SETTING_KEY.location());
                    return;
                }
                encodeInto(setting, value, ops).ifPresent(encoded -> builder.add(id.toString(), encoded));
            });
            return builder.build(prefix);
        }
    };

    private final Map<MachineSetting<?>, Object> values;
    private final Map<String, Tag> unresolved;

    private MachineSettings(Map<MachineSetting<?>, Object> values, Map<String, Tag> unresolved)
    {
        this.values = Collections.unmodifiableMap(values);
        this.unresolved = Collections.unmodifiableMap(unresolved);
    }

    /**
     * 这项设置的值，没有存过时为默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(MachineSetting<T> setting)
    {
        Object value = values.get(setting);
        return value != null ? (T) value : setting.defaultValue();
    }

    /**
     * 是否明确存过这项设置
     */
    public boolean has(MachineSetting<?> setting)
    {
        return values.containsKey(setting);
    }

    /**
     * 设成 value 后的设置；同名的原样保留数据随之作废
     */
    public <T> MachineSettings with(MachineSetting<T> setting, T value)
    {
        Objects.requireNonNull(value);
        if (Objects.equals(values.get(setting), value))
            return this;
        Map<MachineSetting<?>, Object> next = new LinkedHashMap<>(values);
        next.put(setting, value);
        Map<String, Tag> nextUnresolved = unresolved;
        ResourceLocation id = BDRegistries.MACHINE_SETTINGS.getKey(setting);
        if (id != null && unresolved.containsKey(id.toString()))
        {
            nextUnresolved = new LinkedHashMap<>(unresolved);
            nextUnresolved.remove(id.toString());
        }
        return new MachineSettings(next, nextUnresolved);
    }

    /**
     * 以 NBT 写出；用于方块实体存档、菜单同步与配置卡
     */
    public CompoundTag toTag()
    {
        return CODEC.encodeStart(NbtOps.INSTANCE, this).result()
                .filter(CompoundTag.class::isInstance).map(CompoundTag.class::cast)
                .orElseGet(CompoundTag::new);
    }

    public static MachineSettings fromTag(CompoundTag tag)
    {
        return CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(EMPTY);
    }

    @SuppressWarnings("unchecked")
    private static <V, T> boolean decodeInto(MachineSetting<V> setting, DynamicOps<T> ops, T raw, Map<MachineSetting<?>, Object> into)
    {
        try
        {
            var value = setting.codec().parse(ops, raw).result();
            if (value.isEmpty())
                return false;
            into.put(setting, value.get());
            return true;
        }
        catch (RuntimeException e)
        {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static <V, T> java.util.Optional<T> encodeInto(MachineSetting<V> setting, Object value, DynamicOps<T> ops)
    {
        try
        {
            return setting.codec().encodeStart(ops, (V) value).result();
        }
        catch (RuntimeException e)
        {
            LOGGER.error("Failed to encode machine setting {}: {}", setting, value, e);
            return java.util.Optional.empty();
        }
    }

    @Override
    public boolean equals(Object other)
    {
        return this == other || other instanceof MachineSettings settings
                && values.equals(settings.values) && unresolved.equals(settings.unresolved);
    }

    @Override
    public int hashCode()
    {
        return 31 * values.hashCode() + unresolved.hashCode();
    }

    @Override
    public String toString()
    {
        return "MachineSettings" + toTag();
    }
}
