package com.wintercogs.beyonddimensions.Item.Custom;


import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NetCreater extends Item
{

    public NetCreater(Properties properties)
    {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand)
    {
        ItemStack itemstack = player.getItemInHand(usedHand);
        if(usedHand != InteractionHand.MAIN_HAND)
        {
            return InteractionResultHolder.fail(itemstack);
        }

        if(!level.isClientSide())
        {
            DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
            if (net != null)
            {
                return InteractionResultHolder.fail(itemstack);
            }

            DimensionsNet newNet = DimensionsNet.createNewNetForPlayer(player,Long.MAX_VALUE, Integer.MAX_VALUE);

            itemstack.shrink(1);

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
            player.sendSystemMessage(Component.translatable("msg.beyonddimensions.network_created"));

            // 为新网络添加一些时空碎片
            ItemStack timeCrystal = new ItemStack(ModItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION.get(), 64);
            newNet.getUnifiedStorage().insert(new ItemStackType(timeCrystal), false);
        }

        return InteractionResultHolder.sidedSuccess(itemstack,level.isClientSide());
    }

}
