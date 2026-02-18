package com.wintercogs.beyonddimensions.Api.DataBase.Storage;

import com.google.common.base.Predicates;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ManaStackKey;
import com.wintercogs.beyonddimensions.Api.Util.CapCtx;
import com.wintercogs.beyonddimensions.Util.BDMath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;
import vazkii.botania.api.internal.ManaBurst;
import vazkii.botania.api.mana.ManaCollector;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.api.mana.spark.ManaSpark;
import vazkii.botania.api.mana.spark.SparkAttachable;

import java.util.List;
import java.util.Optional;

// 默认实现作为魔力收集器-可以从产能花收集魔力
public class ManaUnifiedStorageHandler implements ManaCollector, ManaPool, SparkAttachable
{
    private final UnifiedStorage storage;
    private final Level level;
    private final BlockPos pos;

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
    public @NotNull BlockPos getManaReceiverPos()
    {
        return pos;
    }

    public long getActualCurrentMana()
    {
        return storage.getStackByKey(ManaStackKey.INSTANCE).amount();
    }

    public long getActualMaxMana()
    {
        return storage.getSlotCapacity(0);
    }

    @Override
    public int getCurrentMana()
    {
        return BDMath.clampLongToInt(getActualCurrentMana());
    }

    @Override
    public boolean isFull()
    {
        // 尽可能轻量的方式来检查
        long currentMana = getActualCurrentMana();
        return currentMana >= storage.getSlotCapacity(0) || (currentMana < storage.getSlotCapacity(0) && storage.isFullSlotsSize());
    }

    @Override
    public void receiveMana(int mana)
    {
        if (mana > 0)
            storage.insert(ManaStackKey.INSTANCE, mana, false);
        else
            storage.extract(ManaStackKey.INSTANCE, -mana, false, false);
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
    public float getManaYieldMultiplier(@NotNull ManaBurst burst)
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
    public boolean canAttachSpark(@NotNull ItemStack stack)
    {
        return true;
    }

    @Override
    public int getAvailableSpaceForMana()
    {
        return getMaxMana() - getCurrentMana();
    }

    @Override
    public ManaSpark getAttachedSpark()
    {
        List<Entity> sparks = level.getEntitiesOfClass(Entity.class, new AABB(pos.above()), Predicates.instanceOf(ManaSpark.class));
        if (sparks.size() == 1)
        {
            Entity e = sparks.get(0);
            return (ManaSpark) e;
        }

        return null;
    }

    @Override
    public boolean areIncomingTranfersDone()
    {
        return isFull();
    }
}
