package com.wintercogs.beyonddimensions.Integration.KubeJS;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.LongType.LongType;
import com.wintercogs.beyonddimensions.Api.DataBase.NetPermissionlevel;
import com.wintercogs.beyonddimensions.Api.DataBase.PlayerPermissionInfo;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EnergyStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Api.Registry.UnifiedStorageBeforeInsertHandler;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;

public class BD_KubeJSPlugin extends KubeJSPlugin
{

    @Override
    public void registerBindings(BindingsEvent bindings)
    {
        bindings.add("UnifiedStorage", UnifiedStorage.class);
        bindings.add("UnifiedStorageBeforeInsertHandler", UnifiedStorageBeforeInsertHandler.class);
        bindings.add("IStackTypedHandler", IStackTypedHandler.class);
        bindings.add("StackTypedHandler", StackTypedHandler.class);
        bindings.add("LongType", LongType.class);
        bindings.add("IStackType", IStackType.class);
        bindings.add("ItemStackType", ItemStackType.class);
        bindings.add("FluidStackType", FluidStackType.class);
        bindings.add("EnergyStackType", EnergyStackType.class);
        bindings.add("IStackHandlerWrapper", IStackHandlerWrapper.class);
        bindings.add("DimensionsNet", DimensionsNet.class);
        bindings.add("PlayerPermissionInfo", PlayerPermissionInfo.class);
        bindings.add("NetPermissionlevel", NetPermissionlevel.class);
    }
}
