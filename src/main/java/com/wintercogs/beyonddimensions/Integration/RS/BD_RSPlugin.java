package com.wintercogs.beyonddimensions.Integration.RS;


import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.wintercogs.beyonddimensions.Integration.RS.ExternalStorage.BD_RSExternalStorageProviderFactory;

public class BD_RSPlugin
{
    private static final RefinedStorageApi api = RefinedStorageApi.INSTANCE;
    public static void register()
    {
        api.addExternalStorageProviderFactory(new BD_RSExternalStorageProviderFactory());
    }
}
