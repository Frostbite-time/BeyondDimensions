package com.wintercogs.beyonddimensions.integration.module.rs.storage;

import com.refinedmods.refinedstorage.api.storage.external.ExternalStorageProvider;
import com.refinedmods.refinedstorage.common.api.storage.externalstorage.ExternalStorageProviderFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

public class BD_RSExternalStorageProviderFactory implements ExternalStorageProviderFactory
{

    @Override
    public @NotNull ExternalStorageProvider create(@NotNull ServerLevel serverLevel, @NotNull BlockPos blockPos, @NotNull Direction direction)
    {
        return new BD_RSExternalStorageProvider(serverLevel, blockPos);
    }
}
