package com.wintercogs.beyonddimensions.common.item;


import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.client.init.BDShortKeys;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class NetCreater extends Item
{

    public NetCreater(Properties properties)
    {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack,
                                @NotNull TooltipContext context,
                                @NotNull TooltipDisplay tooltipDisplay,
                                @NotNull Consumer<Component> tooltipAdder,
                                @NotNull TooltipFlag flag)
    {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
        tooltipAdder.accept(
                Component.translatable("tooltip.beyonddimensions.network_open_key",
                                BDShortKeys.OPEN_GUI_KEY.getKey().getDisplayName())
                        .withStyle(ChatFormatting.DARK_GRAY)
        );
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand usedHand)
    {
        ItemStack itemstack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND)
        {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide())
        {
            DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
            if (net != null)
            {
                return InteractionResult.FAIL;
            }

            DimensionsNet newNet = DimensionsNet.createNewNetForPlayer(player, Long.MAX_VALUE, Integer.MAX_VALUE);

            itemstack.consume(1, player);

            // 在成功创建网络后添加
            level.playSound(null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, // 信标音效
                    SoundSource.PLAYERS,
                    0.8F,
                    1.0F);
            // 发送文字提示
            player.displayClientMessage(Component.translatable("msg.beyonddimensions.network_created"), false);

            // 为新网络添加一些时空碎片
            if (newNet != null)
            {
                ItemStack timeCrystal = new ItemStack(BDItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION.get());
                newNet.getUnifiedStorage().insert(new ItemStackKey(timeCrystal), 64, false);
            }
        }

        return InteractionResult.SUCCESS;
    }

}
