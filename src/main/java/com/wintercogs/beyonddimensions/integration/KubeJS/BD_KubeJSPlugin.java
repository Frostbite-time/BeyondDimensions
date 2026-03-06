package com.wintercogs.beyonddimensions.integration.KubeJS;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.longtype.LongType;
import com.wintercogs.beyonddimensions.api.dimensionnet.NetPermissionlevel;
import com.wintercogs.beyonddimensions.api.dimensionnet.PlayerPermissionInfo;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.dimensionnet.helper.UnifiedStorageBeforeInsertHandler;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;

public class BD_KubeJSPlugin extends KubeJSPlugin
{

    @Override
    public void registerBindings(BindingsEvent bindings)
    {
        bindings.add("UnifiedStorage", UnifiedStorage.class);
        bindings.add("UnifiedStorageBeforeInsertHandler", UnifiedStorageBeforeInsertHandler.class);
        bindings.add("IStackTypedHandler", IStackHandler.class);
        bindings.add("StackTypedHandler", StackHandler.class);
        bindings.add("LongType", LongType.class);
        bindings.add("IStackType", IStackKey.class);
        bindings.add("ItemStackType", ItemStackKey.class);
        bindings.add("FluidStackType", FluidStackKey.class);
        bindings.add("EnergyStackType", EnergyStackKey.class);
        bindings.add("IStackHandlerWrapper", IStackHandlerWrapper.class);
        bindings.add("DimensionsNet", DimensionsNet.class);
        bindings.add("PlayerPermissionInfo", PlayerPermissionInfo.class);
        bindings.add("NetPermissionlevel", NetPermissionlevel.class);
    }
}
