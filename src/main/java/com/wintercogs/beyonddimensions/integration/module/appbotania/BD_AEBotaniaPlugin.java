package com.wintercogs.beyonddimensions.integration.module.appbotania;

import appbot.ae2.ManaKey;
import appbot.ae2.ManaKeyType;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ManaStackKey;
import com.wintercogs.beyonddimensions.integration.module.ae2.AEHelper;

import java.util.Optional;

public class BD_AEBotaniaPlugin
{
    private BD_AEBotaniaPlugin()
    {
    }

    public static void register()
    {
        AEHelper.ISTACK_TO_AEKEY_MAP.put(ManaKey.KEY.getId(), stackType -> Optional.of(ManaKey.KEY));
        AEHelper.AEKEY_TO_STACK_TYPE_MAP.put(ManaKeyType.TYPE, key -> Optional.of(ManaStackKey.INSTANCE));
    }
}
