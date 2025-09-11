package com.wintercogs.beyonddimensions.Item.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.DataComponents.ModDataComponents;
import com.wintercogs.beyonddimensions.Fluid.ModFluids;
import com.wintercogs.beyonddimensions.Machine.XpTransferSpeedMode;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import com.wintercogs.beyonddimensions.Unit.XpUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Locale;

// 经验交换棒
public class XpExchangeItem extends Item
{
    public XpExchangeItem(Properties properties)
    {
        super(properties.stacksTo(1).component(ModDataComponents.XP_TRANSFER_SPEED_MODE, XpTransferSpeedMode.SLOW));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag)
    {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.beyonddimensions.item.xp_exchange"));
    }

    // 每点经验能转为多少mb经验流体？
    public static int getConversionRate()
    {
        return 20;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand)
    {
        super.use(level, player, usedHand);
        ItemStack itemstack = player.getItemInHand(usedHand);
        if(usedHand != InteractionHand.MAIN_HAND)
            return InteractionResultHolder.fail(itemstack); // 非主手使用默认fail

        if(level.isClientSide()) // 客户端shift使用时播放失败动画，否则播放pass动画（直接步进到最终回退）
        {
            if(player.isShiftKeyDown())
            {
                return InteractionResultHolder.fail(itemstack);
            }
        }
        if(!level.isClientSide()) // 服务端实际处理两个不同操作
        {
            if(player.isShiftKeyDown())
            {
                cycleMode(itemstack,player,level);
            }
            else
            {
                DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
                if(net != null)
                {
                    int conversionRate = XpExchangeItem.getConversionRate();
                    double currentLevel = XpUtil.levelAsDouble(player);
                    int wantConversionLevel = getXpLevelPerAction(itemstack);
                    UnifiedStorage storage = net.getUnifiedStorage();

                    long needRemovePlayerXp = XpUtil.xpBetweenLevels(Math.max(currentLevel-wantConversionLevel,0),currentLevel);
                    int actualRemovePlayerXp = BDMath.clampLongToInt(needRemovePlayerXp);
                    long actualInsertFluid = (long) actualRemovePlayerXp * conversionRate;

                    // 插入当前经验流体
                    KeyAmount remaining = storage.insert(new FluidStackKey(new FluidStack(ModFluids.XP_FLUID.source(),1)),actualInsertFluid,false);
                    if(!remaining.isEmpty())
                    {
                        int needReturnXp = BDMath.clampLongToInt(remaining.amount()/20); // 由于前面从int*20，这里除回去
                        actualRemovePlayerXp = actualRemovePlayerXp - needReturnXp;
                    }
                    player.giveExperiencePoints(-actualRemovePlayerXp); // 根据插入的流体给玩家减去经验值
                }
            }
        }

        // 最终回退
        return InteractionResultHolder.sidedSuccess(itemstack,level.isClientSide());
    }

    // 获取本次操作时最大操作的经验等级
    public static int getXpLevelPerAction(ItemStack stack)
    {
        if(stack.getItem() instanceof XpExchangeItem)
        {
            XpTransferSpeedMode xpMode = stack.getOrDefault(ModDataComponents.XP_TRANSFER_SPEED_MODE, XpTransferSpeedMode.SLOW);

            return switch(xpMode)
            {
                case SLOW -> 1;
                case MID -> 10;
                case HIGH -> 30;
                case HIGHEST -> 100;
            };
        }
        return 0; // 最终回退
    }

    // 将经验模式切换到下一级
    private static void cycleMode(ItemStack stack, Player player, Level level)
    {
        XpTransferSpeedMode cur  = stack.getOrDefault(ModDataComponents.XP_TRANSFER_SPEED_MODE, XpTransferSpeedMode.SLOW);
        XpTransferSpeedMode next = cur.next();
        stack.set(ModDataComponents.XP_TRANSFER_SPEED_MODE, next); // 写回到该物品栈

        // 提示切换
        // 键为 beyonddimensions.xp_mode.switch.<xpmode>
        player.sendSystemMessage(Component.translatable("msg.beyonddimensions.xp_mode.switch." + next.name().toLowerCase(Locale.ENGLISH)));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS,0.8F,1.0F);
    }
}
