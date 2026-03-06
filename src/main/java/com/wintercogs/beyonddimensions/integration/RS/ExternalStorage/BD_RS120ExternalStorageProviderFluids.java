package com.wintercogs.beyonddimensions.integration.RS.ExternalStorage;

import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorage;
import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorageContext;
import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorageProvider;
import com.wintercogs.beyonddimensions.integration.RS.Block.RSNetPathwayBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

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
        if (!(externalBe.getLevel() instanceof ServerLevel))
        {
            // 客户端容错：返回一个空实现
            return new NoOpExternalStorageFluids(ctx);
        }

        if (externalBe instanceof RSNetPathwayBlockEntity rsBe)
        {
            // BE 负责订阅/解绑/视图增量；adapter 仅实现 RS 接口
            return new BD_RS120ExternalStorageFluidsAdapter(ctx, rsBe);
        }

        return new NoOpExternalStorageFluids(ctx);
    }

    @Override
    public int getPriority()
    {
        return 27_543_908;
    }

    private static final class NoOpExternalStorageFluids implements IExternalStorage<FluidStack>
    {
        private final IExternalStorageContext ctx;

        private NoOpExternalStorageFluids(IExternalStorageContext ctx)
        {
            this.ctx = ctx;
        }

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
        public List<FluidStack> getStacks()
        {
            return Collections.emptyList();
        }

        @Override
        public @NotNull FluidStack insert(FluidStack prototype, int size, com.refinedmods.refinedstorage.api.util.Action action)
        {
            return prototype.copy();
        }

        @Override
        public @NotNull FluidStack extract(@NotNull FluidStack prototype, int size, int flags, com.refinedmods.refinedstorage.api.util.Action action)
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
            return Math.max(0, delta);
        }
    }
}