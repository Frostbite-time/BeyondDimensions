package com.wintercogs.beyonddimensions.api.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.wintercogs.beyonddimensions.api.registry.BDRegistries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 机器的一项设置，注册到 {@link BDRegistries#MACHINE_SETTING_KEY}：值的类型与编解码器、默认值、显示文字与编辑声明，
 * 以及旧版本存档里的写法。机器类型（{@link MachineType}）列出自己用到的设置，设置页、同步与配置卡都由此生成
 * <p>
 * 值应为不可变对象，编解码器不应依赖注册表上下文：设置还会随菜单同步与配置卡传输
 */
public final class MachineSetting<T>
{
    private final Codec<T> codec;
    private final T defaultValue;
    private final String labelKey;
    private final @Nullable String hintKey;
    private final SettingEditor<T> editor;
    private final List<Function<CompoundTag, Optional<T>>> legacyTags;
    private final List<Supplier<? extends DataComponentType<T>>> legacyComponents;

    private MachineSetting(Builder<T> builder)
    {
        this.codec = builder.codec;
        this.defaultValue = builder.defaultValue;
        this.labelKey = builder.labelKey;
        this.hintKey = builder.hintKey;
        this.editor = builder.editor;
        this.legacyTags = List.copyOf(builder.legacyTags);
        this.legacyComponents = List.copyOf(builder.legacyComponents);
    }

    public static <T> Builder<T> builder(Codec<T> codec, T defaultValue)
    {
        return new Builder<>(codec, defaultValue);
    }

    /**
     * 以枚举名保存的设置
     */
    public static <E extends Enum<E>> Builder<E> enumeration(Class<E> type, E defaultValue)
    {
        Codec<E> codec = Codec.STRING.comapFlatMap(name -> {
            try
            {
                return DataResult.success(Enum.valueOf(type, name));
            }
            catch (IllegalArgumentException e)
            {
                return DataResult.error(() -> "Unknown " + type.getSimpleName() + ": " + name);
            }
        }, Enum::name);
        return new Builder<>(codec, defaultValue);
    }

    /**
     * 枚举值在语言键里的写法：小写的枚举名
     */
    public static String lowercase(Enum<?> value)
    {
        return value.name().toLowerCase(Locale.ROOT);
    }

    public Codec<T> codec()
    {
        return codec;
    }

    public T defaultValue()
    {
        return defaultValue;
    }

    /**
     * 设置名的语言键；机器类型可以替换它
     */
    public String labelKey()
    {
        return labelKey;
    }

    /**
     * 设置名下方说明的语言键，没有时为 null；机器类型可以替换它
     */
    public @Nullable String hintKey()
    {
        return hintKey;
    }

    public SettingEditor<T> editor()
    {
        return editor;
    }

    /**
     * 旧版本方块实体存档里的值：按登记顺序取第一个读得出的写法
     */
    public Optional<T> readLegacy(CompoundTag tag)
    {
        for (Function<CompoundTag, Optional<T>> reader : legacyTags)
        {
            Optional<T> value = reader.apply(tag);
            if (value.isPresent())
                return value;
        }
        return Optional.empty();
    }

    /**
     * 旧版本物品上的值：各项设置曾是独立的数据组件
     */
    public Optional<T> readLegacy(ItemStack stack)
    {
        for (Supplier<? extends DataComponentType<T>> component : legacyComponents)
        {
            T value = stack.get(component.get());
            if (value != null)
                return Optional.of(value);
        }
        return Optional.empty();
    }

    /**
     * 旧版本物品上保存这项设置的数据组件，迁移到新格式后移除
     */
    public List<DataComponentType<T>> legacyComponents()
    {
        List<DataComponentType<T>> out = new ArrayList<>(legacyComponents.size());
        for (Supplier<? extends DataComponentType<T>> component : legacyComponents)
            out.add(component.get());
        return out;
    }

    @Override
    public String toString()
    {
        return "MachineSetting[" + BDRegistries.MACHINE_SETTINGS.getKey(this) + "]";
    }

    public static final class Builder<T>
    {
        private final Codec<T> codec;
        private final T defaultValue;
        private String labelKey;
        private @Nullable String hintKey;
        private SettingEditor<T> editor;
        private final List<Function<CompoundTag, Optional<T>>> legacyTags = new ArrayList<>();
        private final List<Supplier<? extends DataComponentType<T>>> legacyComponents = new ArrayList<>();

        private Builder(Codec<T> codec, T defaultValue)
        {
            this.codec = Objects.requireNonNull(codec);
            this.defaultValue = Objects.requireNonNull(defaultValue);
        }

        public Builder<T> label(String labelKey)
        {
            this.labelKey = Objects.requireNonNull(labelKey);
            return this;
        }

        public Builder<T> hint(String hintKey)
        {
            this.hintKey = hintKey;
            return this;
        }

        public Builder<T> editor(SettingEditor<T> editor)
        {
            this.editor = Objects.requireNonNull(editor);
            return this;
        }

        /**
         * 旧版本把这项设置存在方块实体 NBT 的 key 下，写法与当前编解码器相同
         */
        public Builder<T> legacyKey(String key)
        {
            return legacy(tag -> {
                Tag raw = tag.get(key);
                if (raw == null)
                    return Optional.empty();
                return codec.parse(NbtOps.INSTANCE, raw).result();
            });
        }

        /**
         * 旧版本的其他写法；读不出时返回空
         */
        public Builder<T> legacy(Function<CompoundTag, Optional<T>> reader)
        {
            legacyTags.add(Objects.requireNonNull(reader));
            return this;
        }

        /**
         * 旧版本物品把这项设置存在一个独立的数据组件里
         */
        public Builder<T> legacyComponent(Supplier<? extends DataComponentType<T>> component)
        {
            legacyComponents.add(Objects.requireNonNull(component));
            return this;
        }

        /**
         * @throws IllegalStateException 没有设置名或编辑声明
         */
        public MachineSetting<T> build()
        {
            if (labelKey == null)
                throw new IllegalStateException("MachineSetting needs a label");
            if (editor == null)
                throw new IllegalStateException("MachineSetting needs an editor");
            if (!editor.accepts(defaultValue))
                throw new IllegalStateException("MachineSetting default value is not accepted by its editor: " + defaultValue);
            return new MachineSetting<>(this);
        }
    }
}
