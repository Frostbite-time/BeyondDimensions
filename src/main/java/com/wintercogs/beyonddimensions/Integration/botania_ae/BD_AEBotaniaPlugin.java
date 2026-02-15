package com.wintercogs.beyonddimensions.Integration.botania_ae;

import appbot.ae2.ManaKey;
import appbot.ae2.ManaKeyType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ManaStackKey;
import com.wintercogs.beyonddimensions.Integration.AE.AEHelper;

import java.util.Optional;

public class BD_AEBotaniaPlugin
{
    private BD_AEBotaniaPlugin() {}

    public static void register()
    {
        AEHelper.ISTACK_TO_AEKEY_MAP.put(ManaKey.KEY.getId(), stackType -> Optional.of(ManaKey.KEY));
        AEHelper.AEKEY_TO_STACK_TYPE_MAP.put(ManaKeyType.TYPE, key -> Optional.of(ManaStackKey.INSTANCE));
    }
}
