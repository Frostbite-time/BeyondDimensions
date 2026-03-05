package com.wintercogs.beyonddimensions.Datagen.helpers;

import com.wintercogs.beyonddimensions.Fluid.ModFluids;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

public abstract class BaseItemModelProvider extends ItemModelProvider
{
    public BaseItemModelProvider(PackOutput output, String modid, ExistingFileHelper existingFileHelper)
    {
        super(output, modid, existingFileHelper);
    }

    /**
     * 生成单个流体桶模型：
     * {
     * "parent": "neoforge:item/bucket",
     * "loader": "neoforge:fluid_container",
     * "fluid": "<modid>:<fluidName>"
     * }
     */
    protected void fluidBucketModel(ModFluids.FluidEntry e)
    {
        final String modelName = e.bucket().getId().getPath();

        ItemModelBuilder builder = getBuilder(modelName)
                .parent(new ModelFile.UncheckedModelFile(ResourceLocation.fromNamespaceAndPath("neoforge", "item/bucket")));

        // 挂上自定义加载器并指定对应的“静止”流体（Source）
        builder.customLoader(DynamicFluidContainerModelBuilder::begin)
                .fluid(e.source().get())
                // 可选项：
                // .flipGas(true)                       // 气体翻转
                // .coverIsMask(false)                  // 盖层是否作遮罩
                // .applyFluidLuminosity(true)          // 是否应用流体发光
                .end();
    }
}
