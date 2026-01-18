package com.wintercogs.beyonddimensions.Integration.RS.ExternalStorage;

import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorage;
import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorageContext;
import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorageProvider;
import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Integration.RS.Block.RSNetPathwayBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;

public class BD_RS120ExternalStorageProviderFluids implements IExternalStorageProvider<FluidStack>
{
    @Override
    public boolean canProvide(BlockEntity be, Direction direction)
    {
        return be instanceof RSNetPathwayBlockEntity;
    }

    @Nonnull
    @Override
    public IExternalStorage<FluidStack> provide(IExternalStorageContext ctx, BlockEntity externalBe, Direction direction)
    {
        if (!(externalBe.getLevel() instanceof ServerLevel serverLevel))
        {
            // 客户端容错：返回一个“空实现”，避免 NPE
            return new IExternalStorage<>()
            {
                @Override
                public void update(com.refinedmods.refinedstorage.api.network.INetwork network)
                {
                }

                @Override
                public long getCapacity()
                {
                    return 0;
                }

                @Override
                public java.util.List<FluidStack> getStacks()
                {
                    return java.util.Collections.emptyList();
                }

                @Override
                public FluidStack insert(FluidStack prototype, int size, com.refinedmods.refinedstorage.api.util.Action action)
                {
                    return prototype.copy();
                }

                @Override
                public FluidStack extract(FluidStack prototype, int size, int flags, com.refinedmods.refinedstorage.api.util.Action action)
                {
                    return FluidStack.EMPTY;
                }

                @Override
                public int getStored()
                {
                    return 0;
                }

                @Override
                public int getPriority()
                {
                    return ctx.getPriority();
                }

                @Override
                public com.refinedmods.refinedstorage.api.storage.AccessType getAccessType()
                {
                    return ctx.getAccessType();
                }

                @Override
                public int getCacheDelta(int storedPreInsertion, int size, FluidStack remainder)
                {
                    int rem = remainder == null ? 0 : remainder.getAmount();
                    int delta = size - rem;
                    return Math.max(0, Math.min(delta, Integer.MAX_VALUE));
                }
            };
        }

        if (externalBe instanceof RSNetPathwayBlockEntity rsBe)
        {
            DimensionsNet net = rsBe.getNet();
            UnifiedStorage us = (net != null) ? net.getUnifiedStorage() : UnifiedStorage.getEmpty();

            BD_RS120ExternalStorageFluids storage =
                    new BD_RS120ExternalStorageFluids(ctx, serverLevel, externalBe.getBlockPos(), us);

            storage.attachTo(rsBe);
            return storage;
        }

        // 理论上不会到这里：仍然返回一个空实现兜底
        return new IExternalStorage<>()
        {
            @Override
            public void update(com.refinedmods.refinedstorage.api.network.INetwork network)
            {
            }

            @Override
            public long getCapacity()
            {
                return 0;
            }

            @Override
            public java.util.List<FluidStack> getStacks()
            {
                return java.util.Collections.emptyList();
            }

            @Override
            public FluidStack insert(FluidStack prototype, int size, com.refinedmods.refinedstorage.api.util.Action action)
            {
                return prototype.copy();
            }

            @Override
            public FluidStack extract(FluidStack prototype, int size, int flags, com.refinedmods.refinedstorage.api.util.Action action)
            {
                return FluidStack.EMPTY;
            }

            @Override
            public int getStored()
            {
                return 0;
            }

            @Override
            public int getPriority()
            {
                return ctx.getPriority();
            }

            @Override
            public com.refinedmods.refinedstorage.api.storage.AccessType getAccessType()
            {
                return ctx.getAccessType();
            }

            @Override
            public int getCacheDelta(int storedPreInsertion, int size, FluidStack remainder)
            {
                int rem = remainder == null ? 0 : remainder.getAmount();
                int delta = size - rem;
                return Math.max(0, Math.min(delta, Integer.MAX_VALUE));
            }
        };
    }

    @Override
    public int getPriority()
    {
        // 同物品版，给一个很高的固定优先级
        return 27_543_908;
    }
}
