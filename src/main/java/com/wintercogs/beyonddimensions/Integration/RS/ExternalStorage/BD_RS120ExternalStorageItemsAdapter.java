package com.wintercogs.beyonddimensions.Integration.RS.ExternalStorage;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.storage.AccessType;
import com.refinedmods.refinedstorage.api.storage.cache.IStorageCache;
import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorage;
import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorageContext;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.api.util.IComparer;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Integration.RS.Block.RSNetPathwayBlockEntity;
import com.wintercogs.beyonddimensions.Integration.RS.RSHelper;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public class BD_RS120ExternalStorageItemsAdapter implements IExternalStorage<ItemStack>
{
    private final IExternalStorageContext ctx;
    private final RSNetPathwayBlockEntity be;

    public BD_RS120ExternalStorageItemsAdapter(IExternalStorageContext ctx, RSNetPathwayBlockEntity be)
    {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        this.be = Objects.requireNonNull(be, "be");
    }

    @Override
    public void update(INetwork network)
    {
        IStorageCache<ItemStack> cache = network.getItemStorageCache();
        be.flushItemsToRsCache(cache, ctx);
    }

    @Override
    public long getCapacity()
    {
        return Long.MAX_VALUE;
    }

    @Override
    public List<ItemStack> getStacks()
    {
        return be.getItemsForContext(ctx);
    }

    @Override
    public @NotNull ItemStack insert(@NotNull ItemStack prototype, int size, Action action)
    {
        UnifiedStorage unified = be.getUnifiedStorageOrNull();
        if (unified == null) return prototype.copy();
        if (prototype.isEmpty() || size <= 0) return ItemStack.EMPTY;
        if (ctx.getAccessType() == AccessType.EXTRACT) return prototype.copy();
        if (!ctx.acceptsItem(prototype)) return prototype.copy();

        long before = size;
        long inserted = RSHelper.fromItemStackToIStack(prototype, size)
                .map(s -> size - unified.insert(s, action == Action.SIMULATE).getStackAmount())
                .orElse(0L);

        long remainder = before - inserted;
        if (remainder <= 0) return ItemStack.EMPTY;

        ItemStack rem = prototype.copy();
        rem.setCount((int) Math.min(remainder, Integer.MAX_VALUE));
        return rem;
    }

    @Override
    public @NotNull ItemStack extract(@NotNull ItemStack prototype, int size, int flags, Action action)
    {
        UnifiedStorage unified = be.getUnifiedStorageOrNull();
        if (unified == null) return ItemStack.EMPTY;
        if (prototype.isEmpty() || size <= 0) return ItemStack.EMPTY;
        if (ctx.getAccessType() == AccessType.INSERT) return ItemStack.EMPTY;

        var reqOpt = RSHelper.fromItemStackToIStack(prototype, size);
        if (reqOpt.isEmpty()) return ItemStack.EMPTY;

        IStackType<?> req = reqOpt.get();

        long can = unified.extract(req, true).getStackAmount();
        if (can <= 0) return ItemStack.EMPTY;

        boolean quantityStrict = (flags & IComparer.COMPARE_QUANTITY) == IComparer.COMPARE_QUANTITY;
        if (quantityStrict && can < size)
        {
            return ItemStack.EMPTY;
        }

        int want = (int) Math.min(can, size);

        if (action == Action.SIMULATE)
        {
            ItemStack out = prototype.copy();
            out.setCount(want);
            return out;
        }

        long took = unified.extract(req.copyWithCount(want), false).getStackAmount();
        if (took <= 0) return ItemStack.EMPTY;

        ItemStack out = prototype.copy();
        out.setCount((int) Math.min(took, Integer.MAX_VALUE));
        return out;
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