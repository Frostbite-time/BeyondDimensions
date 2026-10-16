package com.wintercogs.beyonddimensions.api.machine;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;

/**
 * 一项设置的编辑方式：设置页上用哪种控件编辑它，以及哪些值合法，服务端也按它校验。每种控件有自己的工厂方法：
 * <ul>
 *     <li>{@link #buttons}：分段按钮，全部选项并排，适合两到四个短选项</li>
 *     <li>{@link #cycle}：循环按钮，一次显示一项，用两侧的箭头切换，适合有先后顺序或较多的选项</li>
 *     <li>{@link #dropdown}：下拉菜单，点开后列出全部选项，适合选项很多、没有先后顺序的设置</li>
 *     <li>{@link #toggle}：开关，在设置名一行的右侧</li>
 *     <li>{@link #chip}：短按钮，同一组里相邻的几个并排成一行，悬停时显示设置名与说明</li>
 *     <li>{@link #number}：数值框，减号、可输入的数字与加号</li>
 * </ul>
 * 选择类的控件可以用 {@link Choice#withHints} 给每个选项加上说明，设置页显示选中项的说明
 */
public sealed interface SettingEditor<T> permits SettingEditor.Choice, SettingEditor.OnOff, SettingEditor.NumberField
{
    /**
     * 这个值能否被设置页选中；服务端拒绝不合法的值
     */
    boolean accepts(T value);

    /**
     * 从若干选项中选一个：分段按钮、循环按钮或下拉菜单
     */
    sealed interface Choice<T> extends SettingEditor<T> permits Buttons, Cycle, Dropdown
    {
        List<T> options();

        /**
         * 选项名的语言键
         */
        Function<T, String> optionLabel();

        /**
         * 选中项下方说明的语言键；为 null 时显示设置本身的说明
         */
        @Nullable Function<T, String> optionHint();

        /**
         * 同一种控件，每个选项另有说明
         */
        Choice<T> withHints(Function<T, String> optionHint);

        /**
         * 同一种控件，只开放其中的一部分选项
         */
        Choice<T> withOptions(List<T> options);

        @Override
        default boolean accepts(T value)
        {
            return options().contains(value);
        }
    }

    /**
     * 分段按钮
     */
    record Buttons<T>(List<T> options, Function<T, String> optionLabel,
                      @Nullable Function<T, String> optionHint) implements Choice<T>
    {
        public Buttons
        {
            options = List.copyOf(options);
            Objects.requireNonNull(optionLabel);
        }

        @Override
        public Buttons<T> withHints(Function<T, String> optionHint)
        {
            return new Buttons<>(options, optionLabel, Objects.requireNonNull(optionHint));
        }

        @Override
        public Buttons<T> withOptions(List<T> options)
        {
            return new Buttons<>(options, optionLabel, optionHint);
        }
    }

    /**
     * 循环按钮
     */
    record Cycle<T>(List<T> options, Function<T, String> optionLabel,
                    @Nullable Function<T, String> optionHint) implements Choice<T>
    {
        public Cycle
        {
            options = List.copyOf(options);
            Objects.requireNonNull(optionLabel);
        }

        @Override
        public Cycle<T> withHints(Function<T, String> optionHint)
        {
            return new Cycle<>(options, optionLabel, Objects.requireNonNull(optionHint));
        }

        @Override
        public Cycle<T> withOptions(List<T> options)
        {
            return new Cycle<>(options, optionLabel, optionHint);
        }
    }

    /**
     * 下拉菜单
     */
    record Dropdown<T>(List<T> options, Function<T, String> optionLabel,
                       @Nullable Function<T, String> optionHint) implements Choice<T>
    {
        public Dropdown
        {
            options = List.copyOf(options);
            Objects.requireNonNull(optionLabel);
        }

        @Override
        public Dropdown<T> withHints(Function<T, String> optionHint)
        {
            return new Dropdown<>(options, optionLabel, Objects.requireNonNull(optionHint));
        }

        @Override
        public Dropdown<T> withOptions(List<T> options)
        {
            return new Dropdown<>(options, optionLabel, optionHint);
        }
    }

    /**
     * 打开或关闭：开关或短按钮
     */
    sealed interface OnOff<T> extends SettingEditor<T> permits Toggle, Chip
    {
        T on();

        T off();

        @Override
        default boolean accepts(T value)
        {
            return on().equals(value) || off().equals(value);
        }
    }

    /**
     * 开关
     */
    record Toggle<T>(T on, T off) implements OnOff<T>
    {
        public Toggle
        {
            Objects.requireNonNull(on);
            Objects.requireNonNull(off);
        }
    }

    /**
     * 短按钮
     *
     * @param labelKey 按钮上文字的语言键，应当很短；完整的设置名在悬停提示里
     */
    record Chip<T>(T on, T off, String labelKey) implements OnOff<T>
    {
        public Chip
        {
            Objects.requireNonNull(on);
            Objects.requireNonNull(off);
            Objects.requireNonNull(labelKey);
        }
    }

    /**
     * 数值框：min 到 max 之间的整数
     *
     * @param toLong   设置值换成框里的数
     * @param fromLong 框里的数换成设置值
     */
    record NumberField<T>(long min, long max, ToLongFunction<T> toLong,
                          LongFunction<T> fromLong) implements SettingEditor<T>
    {
        public NumberField
        {
            if (min > max)
                throw new IllegalArgumentException("Number field minimum " + min + " is above its maximum " + max);
            Objects.requireNonNull(toLong);
            Objects.requireNonNull(fromLong);
        }

        @Override
        public boolean accepts(T value)
        {
            long number = toLong.applyAsLong(value);
            return number >= min && number <= max;
        }
    }

    static <T> Buttons<T> buttons(List<T> options, Function<T, String> optionLabel)
    {
        return new Buttons<>(options, optionLabel, null);
    }

    static <T> Cycle<T> cycle(List<T> options, Function<T, String> optionLabel)
    {
        return new Cycle<>(options, optionLabel, null);
    }

    static <T> Dropdown<T> dropdown(List<T> options, Function<T, String> optionLabel)
    {
        return new Dropdown<>(options, optionLabel, null);
    }

    /**
     * @param on  打开时的值
     * @param off 关闭时的值
     */
    static <T> Toggle<T> toggle(T on, T off)
    {
        return new Toggle<>(on, off);
    }

    /**
     * @param labelKey 按钮上文字的语言键，应当很短
     */
    static <T> Chip<T> chip(T on, T off, String labelKey)
    {
        return new Chip<>(on, off, labelKey);
    }

    /**
     * 以 int 保存的数值
     */
    static NumberField<Integer> number(int min, int max)
    {
        return new NumberField<>(min, max, Integer::longValue, number -> (int) number);
    }

    /**
     * 以 long 保存的数值
     */
    static NumberField<Long> number(long min, long max)
    {
        return new NumberField<>(min, max, Long::longValue, number -> number);
    }
}
