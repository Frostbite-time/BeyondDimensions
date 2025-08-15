package com.wintercogs.beyonddimensions.Integration.RS.ExternalStorage;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.storage.AccessType;
import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorage;
import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorageContext;
import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorageProvider;
import com.refinedmods.refinedstorage.api.util.Action;
import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Integration.RS.Block.RSNetPathwayBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class BD_RS120ExternalStorageProviderItems implements IExternalStorageProvider<ItemStack>
{

    @Override
    public boolean canProvide(BlockEntity be, Direction direction) {
        // 具体逻辑下放
        return be instanceof RSNetPathwayBlockEntity;
    }

    @Nonnull
    @Override
    public IExternalStorage<ItemStack> provide(IExternalStorageContext ctx, BlockEntity externalBe, Direction direction) {
        if (!(externalBe.getLevel() instanceof ServerLevel serverLevel)) {
            // 客户端/容错：返回 No-Op 实现，避免 NPE
            return new NoOpExternalStorageItems(ctx);
        }

        if (externalBe instanceof RSNetPathwayBlockEntity rsBe) {
            DimensionsNet net = rsBe.getNet();
            UnifiedStorage us = net != null ? net.getUnifiedStorage() : UnifiedStorage.getEmpty();

            BD_RS120ExternalStorageItems storage =
                    new BD_RS120ExternalStorageItems(ctx, serverLevel, externalBe.getBlockPos(), us);

            // 无论初始是否有网，都挂监听；由 storage 内部负责激活/解绑/基线推送
            storage.attachTo(rsBe);
            return storage;
        }

        return new NoOpExternalStorageItems(ctx);
    }

    /** 简单的 No-Op 外部存储，避免在客户端创建真实实现导致 NPE */
    private static final class NoOpExternalStorageItems implements IExternalStorage<ItemStack> {
        private final IExternalStorageContext ctx;
        NoOpExternalStorageItems(IExternalStorageContext ctx) { this.ctx = ctx; }
        @Override public void update(INetwork network) {}
        @Override public long getCapacity() { return 0; }
        @Override public List<ItemStack> getStacks() { return Collections.emptyList(); }
        @Override public ItemStack insert(ItemStack prototype, int size, Action action) { return prototype.copy(); }
        @Override public ItemStack extract(ItemStack prototype, int size, int flags, Action action) { return ItemStack.EMPTY; }
        @Override public int getStored() { return 0; }
        @Override public int getPriority() { return ctx.getPriority(); }
        @Override public AccessType getAccessType() { return ctx.getAccessType(); }
        @Override public int getCacheDelta(int storedPreInsertion, int size, @Nullable ItemStack remainder) {
            int rem = remainder == null ? 0 : remainder.getCount();
            int delta = size - rem;
            return Math.max(0, Math.min(delta, Integer.MAX_VALUE));
        }
    }

    @Override
    public int getPriority() {
        // 固定高优先级，避免被 RS 内置 provider（0）或其他同优先级的 provider 覆盖
        return 15_624_380;
    }

    private static ServerLevel serverLevel(BlockEntity be) {
        return (ServerLevel) be.getLevel();
    }
}
