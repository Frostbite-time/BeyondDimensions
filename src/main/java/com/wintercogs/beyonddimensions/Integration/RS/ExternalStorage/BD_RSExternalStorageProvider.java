package com.wintercogs.beyonddimensions.Integration.RS.ExternalStorage;

import com.google.common.collect.AbstractIterator;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.external.ExternalStorageProvider;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Integration.RS.Block.RSNetPathwayBlockEntity;
import com.wintercogs.beyonddimensions.Integration.RS.RSHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;

public class BD_RSExternalStorageProvider implements ExternalStorageProvider
{

    private final ServerLevel level;
    private final BlockPos pos;


    public BD_RSExternalStorageProvider(ServerLevel level, BlockPos pos)
    {
        this.level = level;
        this.pos = pos;
    }

    @Override
    public Iterator<ResourceAmount> iterator()
    {

        UnifiedStorage storage = null;
        BlockEntity be = level.getBlockEntity(pos);
        if(be instanceof RSNetPathwayBlockEntity rsBe && rsBe.getNet() != null)
            storage = rsBe.getNet().getUnifiedStorage();

        final UnifiedStorage finalStorage = storage;

        if(finalStorage != null)
        {
            return new AbstractIterator<ResourceAmount>() {
                private int index;

                @Nullable
                protected ResourceAmount computeNext() {
                    if (this.index > finalStorage.getSlots()) {
                        return (ResourceAmount)this.endOfData();
                    } else {
                        while(this.index < finalStorage.getSlots()) {
                            IStackType<?> stack = finalStorage.getStackBySlot(this.index);
                            if (!stack.isEmpty()) {
                                ++this.index;
                                ResourceKey key = RSHelper.fromIStackToRSKey(stack).orElse(null);
                                if(key != null)
                                {
                                    return new ResourceAmount(key, stack.getStackAmount());
                                }
                            }

                            ++this.index;
                        }

                        return (ResourceAmount)this.endOfData();
                    }
                }
            };
        }
        else
        {
            return Collections.emptyListIterator();
        }
    }

    // 返回导出量
    @Override
    public long extract(ResourceKey resourceKey, long amount, Action action, Actor actor)
    {
        UnifiedStorage storage = null;
        BlockEntity be = level.getBlockEntity(pos);
        if(be instanceof RSNetPathwayBlockEntity rsBe && rsBe.getNet() != null)
            storage = rsBe.getNet().getUnifiedStorage();

        final UnifiedStorage finalStorage = storage;

        if(finalStorage != null)
        {
            return RSHelper.fromRSKeyToIStack(resourceKey,amount)
                    .map(stack -> finalStorage.extract(stack, action == Action.SIMULATE).getStackAmount())
                    .orElse(0L);
        }
        else
        {
            return 0L;
        }


    }

    // 返回插入量
    @Override
    public long insert(ResourceKey resourceKey, long amount, Action action, Actor actor)
    {
        UnifiedStorage storage = null;
        BlockEntity be = level.getBlockEntity(pos);
        if(be instanceof RSNetPathwayBlockEntity rsBe && rsBe.getNet() != null)
            storage = rsBe.getNet().getUnifiedStorage();

        final UnifiedStorage finalStorage = storage;

        if(finalStorage != null)
        {
            return RSHelper.fromRSKeyToIStack(resourceKey,amount)
                    .map(stack -> amount - finalStorage.insert(stack, action == Action.SIMULATE).getStackAmount())
                    .orElse(0L);
        }
        else
        {
            return 0L;
        }
    }
}
