package com.wintercogs.beyonddimensions.Datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Fluid.ModFluids;
import com.wintercogs.beyonddimensions.Item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider
{

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper)
    {
        super(output, BeyondDimensions.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels()
    {
        basicItem(ModItems.NET_CREATER.get());
        basicItem(ModItems.NET_MEMBER_INVITER.get());
        basicItem(ModItems.NET_MANAGER_INVITER.get());
        basicItem(ModItems.UNSTABLE_SPACE_TIME_FRAGMENT.get());
        basicItem(ModItems.STABLE_SPACE_TIME_FRAGMENT.get());
        basicItem(ModItems.SPACE_TIME_STABLE_FRAME.get());
        basicItem(ModItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION.get());
        basicItem(ModItems.SPACE_TIME_BAR.get());
        basicItem(ModItems.NET_TERMINAL_ITEM.get());
        basicItem(ModItems.NET_GIFTER.get());
        basicItem(ModItems.NET_DESTROYER.get());
        basicItem(ModItems.NET_AE_STORAGE_CELL.get());
        basicItem(ModItems.MATTER_COMPRESS_BALL.get());
        basicItem(ModItems.NET_MAGNET_ITEM.get());
        basicItem(ModItems.NET_FEEDER_ITEM.get());
        basicItem(ModItems.XP_EXCHANGE_ITEM.get());

        generateFluidBucketModels();
    }

    /**
     * 为所有流体生成桶模型（基于我们封装的 ModFluids.ALL）
     */
    private void generateFluidBucketModels()
    {
        for (ModFluids.FluidEntry e : ModFluids.ALL)
        {
            fluidBucketModel(e);
        }
    }

    /**
     * 生成单个流体桶模型：
     * {
     * "parent": "neoforge:item/bucket",
     * "loader": "neoforge:fluid_container",
     * "fluid": "<modid>:<fluidName>"
     * }
     */
    private void fluidBucketModel(ModFluids.FluidEntry e)
    {
        // 模型文件名建议与桶物品注册名一致，避免资源定位搞混
        final String modelName = e.bucket().getId().getPath(); // 例如 "<fluid>_bucket"

        ItemModelBuilder builder = getBuilder(modelName)
                .parent(new ModelFile.UncheckedModelFile(ResourceLocation.tryBuild("forge", "item/bucket")));

        // 挂上自定义加载器并指定对应的“静止”流体（Source）
        builder.customLoader(DynamicFluidContainerModelBuilder::begin)
                .fluid((Fluid) e.source().get())               // 会在 JSON 中写出 "fluid": "<modid>:<fluidName>"
                // 可选项（按需开启）：
                // .flipGas(true)                       // 气体翻转
                // .coverIsMask(false)                  // 盖层是否作遮罩
                // .applyFluidLuminosity(true)          // 是否应用流体发光
                .end();
    }
}
