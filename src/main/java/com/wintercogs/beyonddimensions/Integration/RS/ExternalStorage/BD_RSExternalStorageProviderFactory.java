package com.wintercogs.beyonddimensions.Integration.RS.ExternalStorage;

import com.refinedmods.refinedstorage.api.storage.external.ExternalStorageProvider;
import com.refinedmods.refinedstorage.common.api.storage.externalstorage.ExternalStorageProviderFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

public class BD_RSExternalStorageProviderFactory implements ExternalStorageProviderFactory
{

    @Override
    public ExternalStorageProvider create(ServerLevel serverLevel, BlockPos blockPos, Direction direction)
    {
        return new BD_RSExternalStorageProvider(serverLevel, blockPos);
    }
}
