package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.api.storage.key.ErrorData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 错误数据：存档里解析不了的资源，原样封装在 {@link com.wintercogs.beyonddimensions.common.init.BDDataComponents#ERROR_DATA} 里，
 * 见 {@link ErrorData}
 */
public class ErrorDataItem extends Item
{
    public ErrorDataItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag)
    {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.beyonddimensions.error_data").withStyle(ChatFormatting.GRAY));
        Tag raw = ErrorData.raw(stack);
        if (raw != null)
        {
            String type = ErrorData.type(raw);
            String content = ErrorData.content(raw);
            if (type != null)
                tooltip.add(Component.translatable("tooltip.beyonddimensions.error_data.type", type).withStyle(ChatFormatting.DARK_GRAY));
            if (content != null)
                tooltip.add(Component.translatable("tooltip.beyonddimensions.error_data.content", content).withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable("tooltip.beyonddimensions.error_data.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
