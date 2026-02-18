package com.wintercogs.beyonddimensions.Integration.AE_Botania;

import appbot.ae2.ManaKey;
import appbot.ae2.ManaKeyType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ManaStackKey;
import com.wintercogs.beyonddimensions.Integration.AE.AEHelper;

import java.util.Optional;

public class BD_AE_BotaniaPlugin
{
    public static void register()
    {
        AEHelper.ISTACK_TO_AEKEY_MAP.put(ManaStackKey.ID, stackType -> Optional.of(ManaKey.KEY));
        AEHelper.AEKEY_TO_STACK_TYPE_MAP.put(ManaKeyType.TYPE, (key, amount) -> Optional.of(new ManaStackKey(amount)));
    }
}
