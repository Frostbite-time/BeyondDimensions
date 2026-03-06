package com.wintercogs.beyonddimensions.integration.module.rs.storage;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.storage.AccessType;
import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorage;
import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorageContext;
import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorageProvider;
import com.refinedmods.refinedstorage.api.util.Action;
import com.wintercogs.beyonddimensions.integration.module.rs.block.RSNetPathwayBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class BD_RS120ExternalStorageProviderItems implements IExternalStorageProvider<ItemStack>
{
    @Override
    public boolean canProvide(BlockEntity be, Direction direction)
    {
        return be instanceof RSNetPathwayBlockEntity;
    }

    @Nonnull
    @Override
    public IExternalStorage<ItemStack> provide(IExternalStorageContext ctx, BlockEntity externalBe, Direction direction)
    {
        if (!(externalBe.getLevel() instanceof ServerLevel))
        {
            return new NoOpExternalStorageItems(ctx);
        }

        if (externalBe instanceof RSNetPathwayBlockEntity rsBe)
        {
            // BE 负责订阅/解绑与视图增量；adapter 仅负责 RS 接口
            return new BD_RS120ExternalStorageItemsAdapter(ctx, rsBe);
        }

        return new NoOpExternalStorageItems(ctx);
    }

    /**
     * 简单的 No-Op 外部存储，避免在客户端创建真实实现导致 NPE
     */
    private static final class NoOpExternalStorageItems implements IExternalStorage<ItemStack>
    {
        private final IExternalStorageContext ctx;

        NoOpExternalStorageItems(IExternalStorageContext ctx)
        {
            this.ctx = ctx;
        }

        @Override
        public void update(INetwork network)
        {
        }

        @Override
        public long getCapacity()
        {
            return 0;
        }

        @Override
        public List<ItemStack> getStacks()
        {
            return Collections.emptyList();
        }

        @Override
        public @NotNull ItemStack insert(ItemStack prototype, int size, Action action)
        {
            return prototype.copy();
        }

        @Override
        public @NotNull ItemStack extract(@NotNull ItemStack prototype, int size, int flags, Action action)
        {
            return ItemStack.EMPTY;
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
        public AccessType getAccessType()
        {
            return ctx.getAccessType();
        }

        @Override
        public int getCacheDelta(int storedPreInsertion, int size, @Nullable ItemStack remainder)
        {
            int rem = remainder == null ? 0 : remainder.getCount();
            int delta = size - rem;
            return Math.max(0, delta);
        }
    }

    @Override
    public int getPriority()
    {
        // 固定高优先级，避免被 RS 内置 provider（0）或其他同优先级的 provider 覆盖
        return 15_624_380;
    }
}