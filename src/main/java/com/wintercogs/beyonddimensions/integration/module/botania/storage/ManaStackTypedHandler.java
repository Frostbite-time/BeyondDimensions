package com.wintercogs.beyonddimensions.integration.module.botania.storage;

import com.google.common.base.Predicates;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import com.wintercogs.beyonddimensions.api.util.CapCtx;
import com.wintercogs.beyonddimensions.util.BDMath;
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

public class ManaStackTypedHandler implements ManaCollector, ManaPool, SparkAttachable
{
    private final StackHandler storageHandler;
    private final Level level;
    private final BlockPos pos;

    public ManaStackTypedHandler(StackHandler storageHandler, CapCtx ctx)
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
    public @NotNull BlockPos getManaReceiverPos()
    {
        return pos;
    }

    // 由于魔力池收发完全无回调的特殊性，getCurrentMana必须返回完全真实的量，不能仅仅是第一格
    @Override
    public int getCurrentMana()
    {
        return storageHandler.getBucket(ManaStackKey.ID)
                .map(bucket -> {
                    long sum = 0L;
                    for (int slot : bucket.snapshot())
                    {
                        if (slot < 0) continue;
                        long amt = storageHandler.getStackBySlot(slot).amount();
                        if (amt <= 0) continue;

                        // 保证sum <= Integer.MAX_VALUE，不会溢出
                        long remain = (long) Integer.MAX_VALUE - sum;
                        if (amt > remain)
                        {
                            return Integer.MAX_VALUE;
                        }
                        sum += amt;
                    }
                    return (int) sum; // 安全转换
                })
                .orElse(0);
    }

    // 必须返回真实可靠的情况
    @Override
    public boolean isFull()
    {
        for (KeyAmount keyAmount : storageHandler.getStorage())
        {
            if (keyAmount.isEmpty())
                return false;
            if (keyAmount.key() instanceof ManaStackKey && keyAmount.amount() < keyAmount.key().getVanillaMaxStackSize())
                return false;
        }
        return true;
    }

    @Override
    public void receiveMana(int mana)
    {
        if (mana > 0)
            storageHandler.insert(ManaStackKey.INSTANCE, mana, false);
        else
            storageHandler.extract(ManaStackKey.INSTANCE, -mana, false, false);
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

    // 非常难说，因为它是动态的容器
    // 因此，只能动态计算，好在容器也只有36格，比较轻量
    @Override
    public int getMaxMana()
    {
        int manaSlots = storageHandler.getBucket(ManaStackKey.ID)
                .map(StackHandler.SlotBucket::size)
                .orElse(0);
        int emptySlots = storageHandler.getBucket(EmptyStackKey.INSTANCE)
                .map(StackHandler.SlotBucket::size)
                .orElse(0);

        int eligibleSlots = manaSlots + emptySlots;
        if (eligibleSlots <= 0) return 0;

        long perSlot = Math.min(
                ManaStackKey.INSTANCE.getVanillaMaxStackSize(),
                storageHandler.getSlotCapacity(0) // 槽位容量对所有槽相同
        );

        long total = perSlot * (long) eligibleSlots;
        return BDMath.clampLongToInt(total);
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