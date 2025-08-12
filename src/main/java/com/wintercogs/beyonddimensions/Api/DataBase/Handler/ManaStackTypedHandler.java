package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ManaStackType;
import com.wintercogs.beyonddimensions.Api.Util.CapCtx;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.UnknownNullability;
import vazkii.botania.api.internal.ManaBurst;
import vazkii.botania.api.mana.ManaCollector;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.api.mana.spark.SparkAttachable;

import java.util.Optional;

public class ManaStackTypedHandler implements ManaCollector, ManaPool, SparkAttachable
{
    private StackTypedHandler storageHandler;
    private Level level;
    private BlockPos pos;

    public ManaStackTypedHandler(StackTypedHandler storageHandler, CapCtx ctx)
    {
        this.storageHandler = storageHandler;
        this.level = ctx.level();
        this.pos = ctx.pos();
    }

    @Override
    public @UnknownNullability Level getManaReceiverLevel()
    {
        return level;
    }

    @Override
    public BlockPos getManaReceiverPos()
    {
        return pos;
    }

    // 由于魔力池收发完全无回调的特殊性，getCurrentMana必须返回完全真实的量，不能仅仅是第一格
    @Override
    public int getCurrentMana()
    {
        return storageHandler.getTypeIdIndexList(ManaStackType.ID)
                .map(list -> list.stream()
                        .filter(idx -> idx >= 0)
                        .map(storageHandler::getStackBySlot)
                        .filter(ManaStackType.class::isInstance)
                        .map(ManaStackType.class::cast)
                        .mapToLong(ManaStackType::getStackAmount)
                        .sum())
                .map(BDMath::clampLongToInt)
                .orElse(0);
    }

    // 必须返回真实可靠的情况
    @Override
    public boolean isFull()
    {
        for(IStackType stack : storageHandler.getStorage())
        {
            if(stack.isEmpty())
                return false;
            if(stack instanceof ManaStackType && stack.getStackAmount() < stack.getVanillaMaxStackSize())
                return false;
        }
        return true;
    }

    @Override
    public void receiveMana(int mana)
    {
        if(mana > 0)
            storageHandler.insert(new ManaStackType(mana), false);
        else
            storageHandler.extract(new ManaStackType(-mana), false);
    }

    @Override
    public boolean canReceiveManaFromBursts()
    {
        return true;
    }

    @Override
    public void onClientDisplayTick()
    {
        // 不改变渲染
    }

    @Override
    public float getManaYieldMultiplier(ManaBurst burst)
    {
        return 1f; // 始终完整接收所有脉冲
    }

    @Override
    public boolean isOutputtingPower()
    {
        return true;
    }

    // 非常难说，因为它是动态的容器
    // 因此，只能动态计算，好在容器也只有36格，比较轻量
    @Override
    public int getMaxMana()
    {
        long maxMana = 0;
        ManaStackType stackType = new ManaStackType();
        for(IStackType stack : storageHandler.getStorage())
        {
            if(stack.isEmpty())
                maxMana += stackType.getVanillaMaxStackSize();
            if(stack instanceof ManaStackType && stack.getStackAmount() < stack.getVanillaMaxStackSize())
                maxMana += stack.getVanillaMaxStackSize() - stack.getStackAmount();
        }
        return BDMath.clampLongToInt(maxMana);
    }

    @Override
    public Optional<DyeColor> getColor()
    {
        return Optional.empty();
    }

    @Override
    public void setColor(Optional<DyeColor> color)
    {

    }

    @Override
    public boolean canAttachSpark(ItemStack stack)
    {
        return true;
    }

    @Override
    public int getAvailableSpaceForMana()
    {
        return getMaxMana() - getCurrentMana();
    }

    @Override
    public boolean areIncomingTransfersDone()
    {
        return isFull();
    }

    @Override
    public BlockPos getBlockPos()
    {
        return pos;
    }

    @Override
    public @UnknownNullability Level getLevel()
    {
        return level;
    }
}
