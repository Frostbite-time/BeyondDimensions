package com.wintercogs.beyonddimensions.integration.module.rs.ExternalStorage;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.external.ExternalStorageProvider;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.integration.module.rs.Block.RSNetPathwayBlockEntity;
import com.wintercogs.beyonddimensions.integration.module.rs.RSHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

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

    // ================= ExternalStorageProvider =================

    @Override
    public @NotNull Iterator<ResourceAmount> iterator()
    {
        RSNetPathwayBlockEntity rsBe = getBe();
        if (rsBe == null)
        {
            return Collections.emptyIterator();
        }
        return rsBe.rsExternalSnapshotIterator();
    }

    @Override
    public long extract(@NotNull ResourceKey resourceKey, long amount, @NotNull Action action, @NotNull Actor actor)
    {
        RSNetPathwayBlockEntity rsBe = getBe();
        if (rsBe == null || amount <= 0) return 0L;

        UnifiedStorage us = rsBe.getUnifiedStorageForRsOrNull();
        if (us == null) return 0L;

        return RSHelper.fromRSKeyToIStack(resourceKey)
                .map(s -> us.extract(s, amount, action == Action.SIMULATE, false).amount())
                .orElse(0L);
    }

    @Override
    public long insert(@NotNull ResourceKey resourceKey, long amount, @NotNull Action action, @NotNull Actor actor)
    {
        RSNetPathwayBlockEntity rsBe = getBe();
        if (rsBe == null || amount <= 0) return 0L;

        UnifiedStorage us = rsBe.getUnifiedStorageForRsOrNull();
        if (us == null) return 0L;

        return RSHelper.fromRSKeyToIStack(resourceKey)
                .map(s -> amount - us.insert(s, amount, action == Action.SIMULATE).amount())
                .orElse(0L);
    }

    // ================= helper =================

    private @Nullable RSNetPathwayBlockEntity getBe()
    {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RSNetPathwayBlockEntity rsBe)
        {
            return rsBe;
        }
        return null;
    }
}