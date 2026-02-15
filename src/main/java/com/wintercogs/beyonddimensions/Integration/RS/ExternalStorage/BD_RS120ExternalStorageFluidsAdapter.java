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
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public class BD_RS120ExternalStorageFluidsAdapter implements IExternalStorage<FluidStack>
{
    private final IExternalStorageContext ctx;
    private final RSNetPathwayBlockEntity be;

    public BD_RS120ExternalStorageFluidsAdapter(IExternalStorageContext ctx, RSNetPathwayBlockEntity be)
    {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        this.be = Objects.requireNonNull(be, "be");
    }

    @Override
    public void update(INetwork network)
    {
        IStorageCache<FluidStack> cache = network.getFluidStorageCache();
        be.flushFluidsToRsCache(cache, ctx);
    }

    @Override
    public long getCapacity()
    {
        return Long.MAX_VALUE;
    }

    @Override
    public List<FluidStack> getStacks()
    {
        return be.getFluidsForContext();
    }

    @Override
    public @NotNull FluidStack insert(@NotNull FluidStack prototype, int size, Action action)
    {
        UnifiedStorage unified = be.getUnifiedStorageOrNull();
        if (unified == null) return prototype.copy();
        if (prototype.isEmpty() || size <= 0) return FluidStack.EMPTY;
        if (ctx.getAccessType() == AccessType.EXTRACT) return prototype.copy();
        if (!ctx.acceptsFluid(prototype)) return prototype.copy();

        long before = size;

        long inserted = RSHelper.fromFluidStackToIStack(prototype, size)
                .map(s -> before - unified.insert(s, action == Action.SIMULATE).getStackAmount())
                .orElse(0L);

        long remainder = before - inserted;
        if (remainder <= 0) return FluidStack.EMPTY;

        FluidStack rem = prototype.copy();
        rem.setAmount((int) Math.min(remainder, Integer.MAX_VALUE));
        return rem;
    }

    @Override
    public @NotNull FluidStack extract(@NotNull FluidStack prototype, int size, int flags, Action action)
    {
        UnifiedStorage unified = be.getUnifiedStorageOrNull();
        if (unified == null) return FluidStack.EMPTY;
        if (prototype.isEmpty() || size <= 0) return FluidStack.EMPTY;
        if (ctx.getAccessType() == AccessType.INSERT) return FluidStack.EMPTY;

        var reqOpt = RSHelper.fromFluidStackToIStack(prototype, size);
        if (reqOpt.isEmpty()) return FluidStack.EMPTY;

        IStackType<?> req = reqOpt.get();

        long can = unified.extract(req, true).getStackAmount();
        if (can <= 0) return FluidStack.EMPTY;

        boolean quantityStrict = (flags & IComparer.COMPARE_QUANTITY) == IComparer.COMPARE_QUANTITY;
        if (quantityStrict && can < size)
        {
            return FluidStack.EMPTY;
        }

        int want = (int) Math.min(can, size);

        if (action == Action.SIMULATE)
        {
            FluidStack out = prototype.copy();
            out.setAmount(want);
            return out;
        }

        long took = unified.extract(req.copyWithCount(want), false).getStackAmount();
        if (took <= 0) return FluidStack.EMPTY;

        FluidStack out = prototype.copy();
        out.setAmount((int) Math.min(took, Integer.MAX_VALUE));
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
    public int getCacheDelta(int storedPreInsertion, int size, @Nullable FluidStack remainder)
    {
        int rem = remainder == null ? 0 : remainder.getAmount();
        int delta = size - rem;
        return Math.max(0, delta);
    }
}