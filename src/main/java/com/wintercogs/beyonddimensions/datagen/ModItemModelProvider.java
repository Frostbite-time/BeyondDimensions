package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.common.init.BDFluids;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider
{

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper)
    {
        super(output, BeyondDimensions.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels()
    {
        basicItem(BDItems.NET_CREATER.get());
        basicItem(BDItems.NET_MEMBER_INVITER.get());
        basicItem(BDItems.NET_MANAGER_INVITER.get());
        basicItem(BDItems.UNSTABLE_SPACE_TIME_FRAGMENT.get());
        basicItem(BDItems.STABLE_SPACE_TIME_FRAGMENT.get());
        basicItem(BDItems.SPACE_TIME_STABLE_FRAME.get());
        basicItem(BDItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION.get());
        basicItem(BDItems.SPACE_TIME_BAR.get());
        basicItem(BDItems.NET_TERMINAL_ITEM.get());
        basicItem(BDItems.NET_GIFTER.get());
        basicItem(BDItems.NET_DESTROYER.get());
        basicItem(BDItems.NET_AE_STORAGE_CELL.get());
        basicItem(BDItems.MATTER_COMPRESS_BALL.get());
        basicItem(BDItems.NET_MAGNET_ITEM.get());
        basicItem(BDItems.NET_FEEDER_ITEM.get());
        basicItem(BDItems.NET_RESTOCKER_ITEM.get());
        basicItem(BDItems.XP_EXCHANGE_ITEM.get());
        basicItem(BDItems.WARDEN_SOUL_TAG_ITEM.get());

        generateFluidBucketModels();
    }

    /**
     * 为所有流体生成桶模型（基于我们封装的 ModFluids.ALL）
     */
    private void generateFluidBucketModels()
    {
        for (BDFluids.FluidEntry e : BDFluids.ALL)
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
    private void fluidBucketModel(BDFluids.FluidEntry e)
    {
        // 模型文件名建议与桶物品注册名一致，避免资源定位搞混
        final String modelName = e.bucket().getId().getPath(); // 例如 "<fluid>_bucket"

        ItemModelBuilder builder = getBuilder(modelName)
                .parent(new ModelFile.UncheckedModelFile(ResourceLocation.tryBuild("neoforge", "item/bucket")));

        // 挂上自定义加载器并指定对应的“静止”流体（Source）
        builder.customLoader(DynamicFluidContainerModelBuilder::begin)
                .fluid(e.source().get())               // 会在 JSON 中写出 "fluid": "<modid>:<fluidName>"
                // 可选项（按需开启）：
                // .flipGas(true)                       // 气体翻转
                // .coverIsMask(false)                  // 盖层是否作遮罩
                // .applyFluidLuminosity(true)          // 是否应用流体发光
                .end();
    }

}
