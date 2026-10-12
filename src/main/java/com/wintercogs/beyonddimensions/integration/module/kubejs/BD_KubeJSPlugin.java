package com.wintercogs.beyonddimensions.integration.module.kubejs;

import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.NetPermissionlevel;
import com.wintercogs.beyonddimensions.api.dimensionnet.PlayerPermissionInfo;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.dimensionnet.rule.ExtractPlan;
import com.wintercogs.beyonddimensions.api.dimensionnet.rule.InsertPlan;
import com.wintercogs.beyonddimensions.api.longtype.LongType;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptManager;
import dev.latvian.mods.kubejs.script.ScriptType;


public class BD_KubeJSPlugin implements KubeJSPlugin
{
    @Override
    public void registerBindings(BindingRegistry bindings)
    {
        bindings.add("UnifiedStorage", UnifiedStorage.class);
        // 统一存储规则只在服务端脚本里注册
        if (bindings.type().isServer())
        {
            bindings.add("UnifiedStorageRules", ScriptStorageRules.class);
            bindings.add("InsertPlan", InsertPlan.class);
            bindings.add("ExtractPlan", ExtractPlan.class);
        }
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

    @Override
    public void beforeScriptsLoaded(ScriptManager manager)
    {
        // 服务端脚本重新执行前移除上次注册的规则
        if (manager.scriptType == ScriptType.SERVER)
            ScriptStorageRules.clear();
    }
}
