package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.init.BDFluids;
import com.wintercogs.beyonddimensions.common.machine.XpTransferSpeedMode;
import com.wintercogs.beyonddimensions.common.menu.XpExchangeMenu;
import com.wintercogs.beyonddimensions.util.BDMath;
import com.wintercogs.beyonddimensions.util.XpUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;

// 经验交换棒
public class XpExchangeItem extends NetedItem
{
    public static List<Fluid> xpFluids = new ArrayList<>();

    public XpExchangeItem(Properties properties)
    {
        super(properties.stacksTo(1)
                .component(BDDataComponents.XP_TRANSFER_SPEED_MODE, XpTransferSpeedMode.SLOW)
                .component(BDDataComponents.XP_TARGET_LEVEL, XpExchangeSettings.DEFAULT_TARGET_LEVEL)
                .component(BDDataComponents.XP_NET_KEEP_MODE, false));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot)
    {
        super.inventoryTick(stack, level, entity, slot);
        XpExchangeSettings.ensureComponents(stack);
        if (xpFluids.isEmpty())
            xpFluids = getExperienceFluids(level);
        if (entity instanceof Player player && !level.isClientSide() && stack.getOrDefault(BDDataComponents.XP_NET_KEEP_MODE, false))
            keepXpLevel(stack, player, level);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag)
    {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
        String[] tooltipLines = Component.translatable("tooltip.beyonddimensions.item.xp_exchange").getString().split("\\n");
        for (String tooltipLine : tooltipLines)
        {
            tooltipAdder.accept(Component.literal(tooltipLine));
        }
    }

    // 每点经验能转为多少mb经验流体？
    public static int getConversionRate()
    {
        return 20;
    }

    /**
     * 按整点 XP 提取经验流体。slot 为 -1 时按 key 提取，否则从指定槽位提取。
     * maxXp 必须已按玩家剩余容量限制；返回值不会超过该预算。
     */
    public static int extractExperience(IStackHandler storage, FluidStackKey key, int slot, int maxXp)
    {
        if (maxXp <= 0)
            return 0;

        int conversionRate = getConversionRate();
        KeyAmount available = slot >= 0 ? storage.getStackBySlot(slot) : storage.getStackByKey(key);
        if (available.isEmpty())
            return 0;

        // 先留下库存中不足 1 XP 的零头，避免正常路径依赖回插成功。
        long requestedXp = Math.min((long) maxXp, available.amount() / conversionRate);
        if (requestedXp <= 0)
            return 0;

        long requestedUnits = requestedXp * conversionRate;
        KeyAmount extracted = slot >= 0
                ? storage.extract(slot, requestedUnits, false)
                : storage.extract(key, requestedUnits, false, false);
        if (extracted.isEmpty())
            return 0;

        int gainedXp = (int) Math.min(requestedXp, extracted.amount() / conversionRate);
        long remainder = extracted.amount() - (long) gainedXp * conversionRate;
        // 提取处理器改变实际数量时，退回不足 1 XP 或超出预算的部分。
        if (remainder > 0)
        {
            if (slot >= 0)
                storage.insert(slot, extracted.key(), remainder, false);
            else
                storage.insert(extracted.key(), remainder, false);
        }
        return gainedXp;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand usedHand)
    {
        super.use(level, player, usedHand);
        ItemStack itemstack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND || player.isShiftKeyDown())
        {
            return InteractionResult.FAIL;
        }

        XpExchangeSettings.ensureComponents(itemstack);
        if (!level.isClientSide())
        {
            player.openMenu(new SimpleMenuProvider((containerId, inv, serverPlayer) ->
                            new XpExchangeMenu(containerId, inv, itemstack),
                            Component.translatable("menu.title.beyonddimensions.xp_exchange_menu")),
                    buf -> buf.writeEnum(usedHand));
        }

        return InteractionResult.SUCCESS;
    }

    private void keepXpLevel(ItemStack stack, Player player, Level level)
    {
        if (level.isClientSide()) return;

        DimensionsNet net = NetedItem.getNet(stack);
        if (net == null) return;

        final int conversionRate = XpExchangeItem.getConversionRate();
        final double currentLevel = XpUtil.levelAsDouble(player);
        final int targetLevel = getXpLevelPerAction(stack);
        final UnifiedStorage storage = net.getUnifiedStorage();

        // 经验流体候选列表：优先自家 XP 流体，其次为所有带 C_EXPERIENCE 标签的其它流体
        final Fluid canonicalXp = BDFluids.XP_FLUID.source().get();

        if (currentLevel > targetLevel)
        {
            // 把多余的 XP 存成 XP流体
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
                // 有剩余，把这部分折算回 XP，不再扣玩家
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
            int remainingXp = XpUtil.clampXpToGive(player, needAddXp);
            int gainedXpTotal = 0;

            for (Fluid f : xpFluids)
            {
                if (remainingXp <= 0) break;

                int gainedXp = extractExperience(storage, new FluidStackKey(new FluidStack(f, 1)), -1, remainingXp);

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
        final Registry<Fluid> reg = level.registryAccess().lookupOrThrow(Registries.FLUID);
        final LinkedHashSet<Fluid> set = new LinkedHashSet<>();
        // 追加所有带 C_EXPERIENCE 标签的流体
        reg.getTagOrEmpty(Tags.Fluids.EXPERIENCE).forEach(fluidHolder -> {
            set.add(fluidHolder.value());
        });

        return new ArrayList<>(set);
    }

    // 获取本次操作时最大操作的经验等级
    public static int getXpLevelPerAction(ItemStack stack)
    {
        if (stack.getItem() instanceof XpExchangeItem)
        {
            XpExchangeSettings.ensureComponents(stack);
            return XpExchangeSettings.getTargetLevel(stack);
        }
        return 0;
    }
}
