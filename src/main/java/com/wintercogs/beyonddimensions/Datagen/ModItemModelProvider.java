package com.wintercogs.beyonddimensions.Datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Datagen.helpers.BaseItemModelProvider;
import com.wintercogs.beyonddimensions.Fluid.ModFluids;
import com.wintercogs.beyonddimensions.Item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

public class ModItemModelProvider extends BaseItemModelProvider
{

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper)
    {
        super(output, BeyondDimensions.MODID, existingFileHelper);
    }

    @Override
    public @NotNull String getName()
    {
        return "Beyond Dimensions ItemModels";
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
        basicItem(ModItems.MATTER_COMPRESS_BALL.get());
        basicItem(ModItems.NET_MAGNET_ITEM.get());
        basicItem(ModItems.NET_FEEDER_ITEM.get());
        basicItem(ModItems.NET_RESTOCKER_ITEM.get());
        basicItem(ModItems.XP_EXCHANGE_ITEM.get());
        basicItem(ModItems.WARDEN_SOUL_TAG_ITEM.get());

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
}
