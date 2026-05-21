package com.wintercogs.beyonddimensions.mixin.integration.create.plugin;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class CreateIntegrationMixinPlugin implements IMixinConfigPlugin
{
    private static final String CREATE_MIXIN_PACKAGE = "com.wintercogs.beyonddimensions.mixin.integration.create.target.";

    @Override
    public void onLoad(String mixinPackage)
    {
    }

    @Override
    public String getRefMapperConfig()
    {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
    {
        if (mixinClassName.startsWith(CREATE_MIXIN_PACKAGE))
        {
            return isModLoaded("create");
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets)
    {
    }

    @Override
    public List<String> getMixins()
    {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
    {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
    {
    }

    private static boolean isModLoaded(String modId)
    {
        try
        {
            LoadingModList loadingModList = LoadingModList.get();
            return loadingModList != null && loadingModList.getModFileById(modId) != null;
        }
        catch (Throwable ignored)
        {
            return false;
        }
    }
}
