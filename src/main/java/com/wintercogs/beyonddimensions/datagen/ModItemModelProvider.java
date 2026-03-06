package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.common.init.BDFluids;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.datagen.util.BDItemModelProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

public class ModItemModelProvider extends BDItemModelProvider
{

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper)
    {
        super(output, existingFileHelper);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions ItemTag Provider";
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

}
