package com.wintercogs.beyonddimensions.Integration.AE_Ars;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.SourceStackType;
import com.wintercogs.beyonddimensions.Integration.AE.AEHelper;
import gripe._90.arseng.me.key.SourceKey;
import gripe._90.arseng.me.key.SourceKeyType;

import java.util.Optional;

public class BD_AE_ArsPlugin
{
    public static void register()
    {
        AEHelper.ISTACK_TO_AEKEY_MAP.put(SourceStackType.ID, stackType -> Optional.ofNullable(SourceKey.KEY));
        AEHelper.AEKEY_TO_STACK_TYPE_MAP.put(SourceKeyType.TYPE, (key, amount) -> Optional.of(new SourceStackType(amount)));
    }
}
