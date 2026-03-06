package com.wintercogs.beyonddimensions.integration.module.rs;

import com.refinedmods.refinedstorage.api.IRSAPI;
import com.refinedmods.refinedstorage.api.storage.StorageType;
import com.refinedmods.refinedstorage.apiimpl.API;
import com.wintercogs.beyonddimensions.integration.module.rs.storage.BD_RS120ExternalStorageProviderFluids;
import com.wintercogs.beyonddimensions.integration.module.rs.storage.BD_RS120ExternalStorageProviderItems;

public class BD_RSPlugin
{
    public static IRSAPI api = API.instance();

    public static void register()
    {
        api.addExternalStorageProvider(StorageType.ITEM, new BD_RS120ExternalStorageProviderItems());
        api.addExternalStorageProvider(StorageType.FLUID, new BD_RS120ExternalStorageProviderFluids());
    }
}
