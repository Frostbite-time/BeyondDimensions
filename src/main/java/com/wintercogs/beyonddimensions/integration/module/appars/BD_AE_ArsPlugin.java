package com.wintercogs.beyonddimensions.integration.module.appars;

import com.wintercogs.beyonddimensions.api.storage.key.impl.SourceStackKey;
import com.wintercogs.beyonddimensions.integration.module.ae2.AEHelper;
import gripe._90.arseng.me.key.SourceKey;
import gripe._90.arseng.me.key.SourceKeyType;

import java.util.Optional;

public class BD_AE_ArsPlugin
{
    public static void register()
    {
        AEHelper.ISTACK_TO_AEKEY_MAP.put(SourceStackKey.ID, stackType -> Optional.of(SourceKey.KEY));
        AEHelper.AEKEY_TO_STACK_TYPE_MAP.put(SourceKeyType.TYPE, key -> Optional.of(SourceStackKey.INSTANCE));
    }
}
