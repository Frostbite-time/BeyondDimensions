package com.wintercogs.beyonddimensions.integration.AE_IFS;

import com.buuz135.soulplied_energistics.applied.SoulAEKeyType;
import com.buuz135.soulplied_energistics.applied.SoulKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.WardenSoulStackKey;
import com.wintercogs.beyonddimensions.integration.ae2.AEHelper;

import java.util.Optional;

// 对工业先锋附属——灵魂涌动的应用能源附属的兼容
public class BD_AE_IFS_Plugin
{
    public static void register()
    {
        AEHelper.ISTACK_TO_AEKEY_MAP.put(WardenSoulStackKey.ID, stackType -> Optional.of(SoulKey.INSTANCE));

        AEHelper.AEKEY_TO_STACK_TYPE_MAP.put(SoulAEKeyType.TYPE, key -> Optional.of(WardenSoulStackKey.INSTANCE));
    }
}
