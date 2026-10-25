package com.wintercogs.beyonddimensions.api.machine;

import com.wintercogs.beyonddimensions.api.registry.BDRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * 一类机器，注册到 {@link BDRegistries#MACHINE_TYPE_KEY}：列出它的设置，设置页按这个顺序生成，可以用 {@link Builder#section} 分组。
 * 配置卡记下复制时的机器类型，只能粘贴到同一类型的机器上
 * <pre>{@code
 * MachineType.builder(MyBlocks.PRESS)
 *         .setting(BDMachineSettings.REDSTONE)
 *         .setting(MySettings.SPEED, entry -> entry.hint("ui.myaddon.press.speed.hint"))
 *         .build();
 * }</pre>
 */
public final class MachineType
{
    private final Supplier<? extends ItemLike> icon;
    private final List<Entry<?>> entries;

    private MachineType(Supplier<? extends ItemLike> icon, List<Entry<?>> entries)
    {
        this.icon = icon;
        this.entries = List.copyOf(entries);
    }

    /**
     * @param icon 代表这类机器的物品，用于显示名称与图标
     */
    public static Builder builder(Supplier<? extends ItemLike> icon)
    {
        return new Builder(Objects.requireNonNull(icon));
    }

    /**
     * 这类机器的全部设置，按设置页的顺序
     */
    public List<Entry<?>> settings()
    {
        return entries;
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable Entry<T> entry(MachineSetting<T> setting)
    {
        for (Entry<?> entry : entries)
            if (entry.setting() == setting)
                return (Entry<T>) entry;
        return null;
    }

    public boolean has(MachineSetting<?> setting)
    {
        return entry(setting) != null;
    }

    public ItemLike icon()
    {
        return icon.get();
    }

    public Component displayName()
    {
        return new ItemStack(icon.get()).getHoverName();
    }

    @Override
    public String toString()
    {
        return "MachineType[" + BDRegistries.MACHINE_TYPES.get().getKey(this) + "]";
    }

    /**
     * 机器类型里的一项设置；可以替换设置自带的文字、编辑方式与默认值
     */
    public static final class Entry<T>
    {
        private final Supplier<? extends MachineSetting<T>> setting;
        private final @Nullable String labelKey;
        private final @Nullable String hintKey;
        private final @Nullable String lockedHintKey;
        private final @Nullable String sectionKey;
        private final @Nullable UnaryOperator<SettingEditor<T>> editorOverride;
        private final @Nullable T defaultValue;
        private volatile @Nullable SettingEditor<T> editor;

        private Entry(EntryBuilder<T> builder, @Nullable String sectionKey)
        {
            this.setting = builder.setting;
            this.labelKey = builder.labelKey;
            this.hintKey = builder.hintKey;
            this.lockedHintKey = builder.lockedHintKey;
            this.editorOverride = builder.editorOverride;
            this.defaultValue = builder.defaultValue;
            this.sectionKey = sectionKey;
        }

        public MachineSetting<T> setting()
        {
            return setting.get();
        }

        public String labelKey()
        {
            return labelKey != null ? labelKey : setting().labelKey();
        }

        public @Nullable String hintKey()
        {
            return hintKey != null ? hintKey : setting().hintKey();
        }

        /**
         * 这项设置在某台机器上不可修改时显示的说明，没有时沿用 {@link #hintKey()}
         */
        public @Nullable String lockedHintKey()
        {
            return lockedHintKey;
        }

        /**
         * 所在分组的标题，见 {@link Builder#section}；没有分组时为空
         */
        public @Nullable String sectionKey()
        {
            return sectionKey;
        }

        public SettingEditor<T> editor()
        {
            SettingEditor<T> resolved = editor;
            if (resolved == null)
            {
                resolved = editorOverride == null ? setting().editor() : editorOverride.apply(setting().editor());
                editor = resolved;
            }
            return resolved;
        }

        public boolean accepts(T value)
        {
            return editor().accepts(value);
        }

        /**
         * 这类机器上这项设置没有存过时的值
         */
        public T defaultValue()
        {
            return defaultValue != null ? defaultValue : setting().defaultValue();
        }

        /**
         * 这项设置在 settings 里的值，没有存过时为 {@link #defaultValue()}
         */
        public T valueIn(MachineSettings settings)
        {
            return settings.has(setting()) ? settings.get(setting()) : defaultValue();
        }
    }

    public static final class EntryBuilder<T>
    {
        private final Supplier<? extends MachineSetting<T>> setting;
        private @Nullable String labelKey;
        private @Nullable String hintKey;
        private @Nullable String lockedHintKey;
        private @Nullable UnaryOperator<SettingEditor<T>> editorOverride;
        private @Nullable T defaultValue;

        private EntryBuilder(Supplier<? extends MachineSetting<T>> setting)
        {
            this.setting = setting;
        }

        public EntryBuilder<T> label(String labelKey)
        {
            this.labelKey = labelKey;
            return this;
        }

        public EntryBuilder<T> hint(String hintKey)
        {
            this.hintKey = hintKey;
            return this;
        }

        public EntryBuilder<T> lockedHint(String lockedHintKey)
        {
            this.lockedHintKey = lockedHintKey;
            return this;
        }

        /**
         * 替换编辑方式，例如把开关换成短按钮
         */
        public EntryBuilder<T> editor(SettingEditor<T> editor)
        {
            this.editorOverride = base -> editor;
            return this;
        }

        /**
         * 在设置自带的编辑方式上修改，例如只开放部分选项；首次用到时才调用
         */
        public EntryBuilder<T> editor(UnaryOperator<SettingEditor<T>> override)
        {
            this.editorOverride = override;
            return this;
        }

        /**
         * 替换设置自带的默认值，只对这类机器生效
         */
        public EntryBuilder<T> defaultValue(T value)
        {
            this.defaultValue = Objects.requireNonNull(value);
            return this;
        }
    }

    public static final class Builder
    {
        private final Supplier<? extends ItemLike> icon;
        private final List<Entry<?>> entries = new ArrayList<>();
        private @Nullable String section;

        private Builder(Supplier<? extends ItemLike> icon)
        {
            this.icon = icon;
        }

        /**
         * 之后声明的设置归入一组：设置页在组前显示 labelKey 的标题，组与组之间有分隔线
         */
        public Builder section(String labelKey)
        {
            this.section = Objects.requireNonNull(labelKey);
            return this;
        }

        public <T> Builder setting(Supplier<? extends MachineSetting<T>> setting)
        {
            return setting(setting, UnaryOperator.identity());
        }

        public <T> Builder setting(Supplier<? extends MachineSetting<T>> setting, UnaryOperator<EntryBuilder<T>> customize)
        {
            entries.add(new Entry<>(customize.apply(new EntryBuilder<>(Objects.requireNonNull(setting))), section));
            return this;
        }

        public MachineType build()
        {
            return new MachineType(icon, entries);
        }
    }
}
