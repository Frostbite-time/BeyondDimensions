package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.common.init.BDFluidTags;
import com.wintercogs.beyonddimensions.common.init.BDFluids;
import com.wintercogs.beyonddimensions.common.machine.XpTransferSpeedMode;
import com.wintercogs.beyonddimensions.util.BDMath;
import com.wintercogs.beyonddimensions.util.XpUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class XpExchangeItem extends Item
{
    public static List<Fluid> xpFluids = new ArrayList<>();

    public XpExchangeItem(Properties properties)
    {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected)
    {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        checkComponents(stack);
        if (xpFluids.isEmpty())
            xpFluids = getExperienceFluids(level);
        if (entity instanceof Player player && !level.isClientSide() && getOrDefaultXpNetKeepMode(stack, false))
            keepXpLevel(stack, player, level);
    }

    private void checkComponents(ItemStack stack)
    {
        if (!hasXpTransferSpeedMode(stack))
            setXpTransferSpeedMode(stack, XpTransferSpeedMode.SLOW);
        if (!hasXpNetKeepMode(stack))
            setXpNetKeepMode(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced)
    {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
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
        if (usedHand != InteractionHand.MAIN_HAND)
            return InteractionResultHolder.fail(itemstack); // 非主手使用默认fail

        if (level.isClientSide()) // 客户端shift使用时播放失败动画，否则播放pass动画（直接步进到最终回退）
        {
            if (player.isShiftKeyDown())
            {
                return InteractionResultHolder.fail(itemstack);
            }
        }
        if (!level.isClientSide()) // 服务端实际处理两个不同操作
        {
            if (player.isShiftKeyDown())
            {
                cycleMode(itemstack, player, level);
            }
            else
            {
                boolean current = getOrDefaultXpNetKeepMode(itemstack, false);
                setXpNetKeepMode(itemstack, !current);
                if (getOrDefaultXpNetKeepMode(itemstack, false))
                    player.sendSystemMessage(Component.translatable("msg.beyonddimensions.item.xp_exchange.open"));
                else
                    player.sendSystemMessage(Component.translatable("msg.beyonddimensions.item.xp_exchange.close"));
            }
        }

        // 最终回退
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    private void keepXpLevel(ItemStack stack, Player player, Level level)
    {
        if (level.isClientSide()) return;

        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net == null) return;

        final int conversionRate = XpExchangeItem.getConversionRate();
        final double currentLevel = XpUtil.levelAsDouble(player);
        final int targetLevel = getXpLevelPerAction(stack);
        final UnifiedStorage storage = net.getUnifiedStorage();

        // 经验流体候选列表：优先自家 XP 流体，其次为所有带 C_EXPERIENCE 标签的其它流体
        final Fluid canonicalXp = BDFluids.XP_FLUID.source().get();

        if (currentLevel > targetLevel)
        {
            // 把多余的 XP 存成“自家 XP 流体”
            long needRemoveXp = XpUtil.xpExcessAbove(currentLevel, targetLevel);
            int toRemoveXp = BDMath.clampLongToInt(needRemoveXp);

            long toInsertUnits = (long) toRemoveXp * conversionRate;
            KeyAmount remaining = storage.insert(
                    new FluidStackKey(new FluidStack(canonicalXp, 1)),
                    toInsertUnits,
                    false
            );

            if (!remaining.isEmpty())
            {
                // 有剩余（仓库放不下），把这部分折算回 XP，不再扣玩家
                int overflowXp = BDMath.clampLongToInt(remaining.amount() / conversionRate);
                toRemoveXp -= overflowXp;
            }

            if (toRemoveXp != 0)
            {
                player.giveExperiencePoints(-toRemoveXp);
            }

        }
        else if (currentLevel < targetLevel)
        {
            // 从任意“经验流体”里提取，尽量把玩家补到目标等级
            long needAddXp = XpUtil.xpToReachAtLeast(currentLevel, targetLevel);
            int remainingXp = BDMath.clampLongToInt(needAddXp);
            int gainedXpTotal = 0;

            for (Fluid f : xpFluids)
            {
                if (remainingXp <= 0) break;

                long wantUnits = (long) remainingXp * conversionRate;
                if (wantUnits <= 0) break;

                KeyAmount extracted = storage.extract(
                        new FluidStackKey(new FluidStack(f, 1)),
                        wantUnits,
                        false,
                        false
                );

                if (extracted.isEmpty()) continue;

                long units = extracted.amount();
                int gainedXp = BDMath.clampLongToInt(units / conversionRate);
                if (gainedXp <= 0)
                {
                    // 抽到了不足 1 XP 的零头，原样放回，继续尝试其它流体
                    storage.insert(new FluidStackKey(new FluidStack(f, 1)), units, false);
                    continue;
                }

                long consumedUnits = (long) gainedXp * conversionRate;
                long remainderUnits = units - consumedUnits;

                // 多抽出来但不足 1 XP 的部分回滚
                if (remainderUnits > 0)
                {
                    storage.insert(new FluidStackKey(new FluidStack(f, 1)), remainderUnits, false);
                }

                gainedXpTotal += gainedXp;
                remainingXp -= gainedXp;
            }

            if (gainedXpTotal > 0)
            {
                player.giveExperiencePoints(gainedXpTotal);
            }
            // 如果仓库里经验流体不足，玩家会被尽量接近目标等级，等待下次再补。
        }
    }

    /**
     * 获取“经验流体”候选列表：先放 canonical，再放其它带标签的（去重）。
     */
    private List<Fluid> getExperienceFluids(Level level)
    {
        final Registry<Fluid> reg = level.registryAccess().registryOrThrow(Registries.FLUID);
        final LinkedHashSet<Fluid> set = new LinkedHashSet<>();
        // 追加所有带 C_EXPERIENCE 标签的流体
        reg.getTag(BDFluidTags.C_EXPERIENCE).ifPresent((HolderSet<Fluid> holders) -> {
            for (Holder<Fluid> h : holders)
            {
                set.add(h.value());
            }
        });

        return new ArrayList<>(set);
    }

    // 获取本次操作时最大操作的经验等级
    public static int getXpLevelPerAction(ItemStack stack)
    {
        if (stack.getItem() instanceof XpExchangeItem)
        {
            XpTransferSpeedMode xpMode = getOrDefaultXpTransferSpeedMode(stack, XpTransferSpeedMode.SLOW);

            return switch (xpMode)
            {
                case SLOW -> 1;
                case MID -> 10;
                case HIGH -> 30;
                case HIGHEST -> 100;
                case OVER_HIGHEST -> 150;
            };
        }
        return 0; // 最终回退
    }

    // 将经验模式切换到下一级
    private static void cycleMode(ItemStack stack, Player player, Level level)
    {
        XpTransferSpeedMode cur = getOrDefaultXpTransferSpeedMode(stack, XpTransferSpeedMode.SLOW);
        XpTransferSpeedMode next = cur.next();
        setXpTransferSpeedMode(stack, next);

        // 提示切换
        // 键为 beyonddimensions.xp_mode.switch.<xpmode>
        player.sendSystemMessage(Component.translatable("msg.beyonddimensions.xp_mode.switch." + next.name().toLowerCase(Locale.ENGLISH)));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    public static XpTransferSpeedMode getOrDefaultXpTransferSpeedMode(ItemStack stack, XpTransferSpeedMode defaultValue)
    {
        if (stack.hasTag() && stack.getTag().contains("xp_transfer_speed_mode"))
        {
            return XpTransferSpeedMode.valueOf(stack.getTag().getString("xp_transfer_speed_mode"));
        }
        return defaultValue; //未命中
    }

    public static boolean hasXpTransferSpeedMode(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().contains("xp_transfer_speed_mode");
    }

    public static void setXpTransferSpeedMode(ItemStack stack, XpTransferSpeedMode newMode)
    {
        stack.getOrCreateTag().putString("xp_transfer_speed_mode", newMode.name());
    }

    public static boolean getOrDefaultXpNetKeepMode(ItemStack stack, boolean defaultValue)
    {
        if (stack.hasTag() && stack.getTag().contains("xp_net_keep_mode"))
        {
            return stack.getTag().getBoolean("xp_net_keep_mode");
        }
        return defaultValue; //未命中
    }

    public static boolean hasXpNetKeepMode(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().contains("xp_net_keep_mode");
    }

    public static void setXpNetKeepMode(ItemStack stack, boolean newMode)
    {
        stack.getOrCreateTag().putBoolean("xp_net_keep_mode", newMode);
    }
}
