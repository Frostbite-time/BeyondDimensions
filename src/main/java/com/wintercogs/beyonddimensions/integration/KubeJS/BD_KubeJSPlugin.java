package com.wintercogs.beyonddimensions.integration.KubeJS;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.IStackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.LongType.LongType;
import com.wintercogs.beyonddimensions.Api.DataBase.NetPermissionlevel;
import com.wintercogs.beyonddimensions.Api.DataBase.PlayerPermissionInfo;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.*;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Api.Registry.UnifiedStorageBeforeInsertHandler;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;


public class BD_KubeJSPlugin implements KubeJSPlugin
{
    @Override
    public void registerBindings(BindingRegistry bindings)
    {
        bindings.add("UnifiedStorage", UnifiedStorage.class);
        bindings.add("UnifiedStorageBeforeInsertHandler", UnifiedStorageBeforeInsertHandler.class);
        bindings.add("IStackHandler", IStackHandler.class);
        bindings.add("StackHandler", StackHandler.class);
        bindings.add("LongType", LongType.class);
        bindings.add("IStackKey", IStackKey.class);
        bindings.add("KeyAmount", KeyAmount.class);
        bindings.add("ItemStackKey", ItemStackKey.class);
        bindings.add("FluidStackKey", FluidStackKey.class);
        bindings.add("EnergyStackKey", EnergyStackKey.class);
        bindings.add("IStackHandlerWrapper", IStackHandlerWrapper.class);
        bindings.add("DimensionsNet", DimensionsNet.class);
        bindings.add("PlayerPermissionInfo", PlayerPermissionInfo.class);
        bindings.add("NetPermissionlevel", NetPermissionlevel.class);
    }
}
