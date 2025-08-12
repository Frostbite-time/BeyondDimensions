package com.wintercogs.beyonddimensions.Api.DataBase.Storage;

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

// 默认实现作为魔力收集器-可以从产能花收集魔力
public class ManaUnifiedStorageHandler implements ManaCollector, ManaPool, SparkAttachable
{
    private UnifiedStorage storage;
    private Level level;
    private BlockPos pos;

    public ManaUnifiedStorageHandler(UnifiedStorage storage, CapCtx ctx)
    {
        this.storage = storage;
        this.level = ctx.level();
        this.pos = ctx.pos();
    }

    public UnifiedStorage getStorage()
    {
        return storage;
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

    public long getActualCurrentMana()
    {
        return storage.getTypeIdIndexList(ManaStackType.ID)
                .map(slots -> slots.get(0))
                .filter(actualIndex -> actualIndex>=0)
                .map(actualIndex -> (ManaStackType)storage.getStackBySlot(actualIndex))
                .map(stack -> stack.getStackAmount())
                .orElse(0L);
    }

    public long getActualMaxMana()
    {
        return storage.getSlotCapacity(0);
    }

    @Override
    public int getCurrentMana()
    {
        return storage.getTypeIdIndexList(ManaStackType.ID)
                .map(slots -> slots.get(0))
                .filter(actualIndex -> actualIndex>=0)
                .map(actualIndex -> (ManaStackType)storage.getStackBySlot(actualIndex))
                .map(stack -> BDMath.clampLongToInt(stack.getStackAmount()))
                .orElse(0);
    }

    @Override
    public boolean isFull()
    {
        // 尽可能轻量的方式来检查
        IStackType stack = storage.getStackByStack(new ManaStackType(0));
        long currentMana = stack == null ? 0 : stack.getStackAmount();
        return currentMana >= storage.getSlotCapacity(0) || (currentMana < storage.getSlotCapacity(0) && storage.isFullSlotsSize());
    }

    @Override
    public void receiveMana(int mana)
    {
        if(mana > 0)
            storage.insert(new ManaStackType(mana), false);
        else
            storage.extract(new ManaStackType(-mana), false);
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

    @Override
    public int getMaxMana()
    {
        return BDMath.clampLongToInt(storage.getSlotCapacity(0));
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
