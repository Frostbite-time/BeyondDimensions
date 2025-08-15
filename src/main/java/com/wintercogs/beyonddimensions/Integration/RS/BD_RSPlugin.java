package com.wintercogs.beyonddimensions.Integration.RS;


import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.wintercogs.beyonddimensions.Integration.RS.ExternalStorage.BD_RSExternalStorageProviderFactory;

public class BD_RSPlugin
{
    private static final RefinedStorageApi api = RefinedStorageApi.INSTANCE;
    public static void register()
    {
        // 对于RS的存储元件而言，目前没有看到除修改模组本体以外的方式能将非RS主动操作导致的变化推送回RS
        // 所以，只能选用外部存储兼容，RS会对外部存储保持定期遍历来更新物品
        // 这里通过内部缓存和增量更新机制尽可能降低键转换开销
        // 由于RS的定期扫描机制，仍然会存在一个额外开销
        // 但是比起直接扫描普通容器，这里的性能还是高了很多很多倍
        api.addExternalStorageProviderFactory(new BD_RSExternalStorageProviderFactory());
    }
}
