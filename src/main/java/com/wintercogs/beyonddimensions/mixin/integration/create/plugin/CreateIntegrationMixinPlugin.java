package com.wintercogs.beyonddimensions.mixin.integration.create.plugin;

import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class CreateIntegrationMixinPlugin implements IMixinConfigPlugin
{
    private static final String CREATE_MIXIN_PACKAGE = "com.wintercogs.beyonddimensions.mixin.integration.create.target.";
    // 只用于机械动力 0.5.1：6.0 起镜像由 Create6Module 注册，两处都生效会镜像两次
    private static final String STRUCTURE_TRANSFORM_MIXIN = CREATE_MIXIN_PACKAGE + "StructureTransformMixin";

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
            if (!isModLoaded("create"))
            {
                return false;
            }
            if (mixinClassName.equals(STRUCTURE_TRANSFORM_MIXIN))
            {
                return !isCreate6();
            }
            return true;
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

    // 已加载的机械动力是否为 6.0 及以上
    private static boolean isCreate6()
    {
        ModFileInfo file = LoadingModList.get().getModFileById("create");
        return file != null && file.getMods().stream()
                .anyMatch(mod -> mod.getModId().equals("create")
                        && mod.getVersion().compareTo(new DefaultArtifactVersion("6.0.0")) >= 0);
    }
}
