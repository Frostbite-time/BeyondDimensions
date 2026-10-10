package com.wintercogs.beyonddimensions.api.machine;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 一项设置的编辑声明：设置页怎样编辑它、哪些值合法。机器的设置页按它统一生成，服务端也按它校验客户端提交的值
 */
public sealed interface SettingEditor<T> permits SettingEditor.Choice, SettingEditor.Toggle
{
    /**
     * 这个值能否被设置页选中；服务端拒绝不合法的值
     */
    boolean accepts(T value);

    /**
     * 从若干选项中选一个
     *
     * @param optionLabel 选项名的语言键
     * @param optionHint  选中项下方说明的语言键，没有时为 null
     * @param cycle       为真时显示成点一下换到下一项的单个按钮，适合选项多的设置
     */
    record Choice<T>(List<T> options, Function<T, String> optionLabel, @Nullable Function<T, String> optionHint,
                     boolean cycle) implements SettingEditor<T>
    {
        public Choice
        {
            options = List.copyOf(options);
            Objects.requireNonNull(optionLabel);
        }

        @Override
        public boolean accepts(T value)
        {
            return options.contains(value);
        }

        public Choice<T> withHints(Function<T, String> optionHint)
        {
            return new Choice<>(options, optionLabel, optionHint, cycle);
        }

        public Choice<T> cycling()
        {
            return new Choice<>(options, optionLabel, optionHint, true);
        }
    }

    /**
     * 开关：打开时为 on，关闭时为 off
     */
    record Toggle<T>(T on, T off) implements SettingEditor<T>
    {
        public Toggle
        {
            Objects.requireNonNull(on);
            Objects.requireNonNull(off);
        }

        @Override
        public boolean accepts(T value)
        {
            return on.equals(value) || off.equals(value);
        }
    }

    static <T> Choice<T> choice(List<T> options, Function<T, String> optionLabel)
    {
        return new Choice<>(options, optionLabel, null, false);
    }

    static <T> Toggle<T> toggle(T on, T off)
    {
        return new Toggle<>(on, off);
    }
}
