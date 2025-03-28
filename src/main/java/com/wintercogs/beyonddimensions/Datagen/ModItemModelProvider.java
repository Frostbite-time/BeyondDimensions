//package com.wintercogs.beyonddimensions.Datagen;
//
//import com.wintercogs.beyonddimensions.BeyondDimensions;
//import com.wintercogs.beyonddimensions.Item.ModItems;
//import net.minecraft.data.PackOutput;
//import net.minecraftforge.client.model.generators.ItemModelProvider;
//import net.minecraftforge.common.data.ExistingFileHelper;
//
//public class ModItemModelProvider extends ItemModelProvider
//{
//
//    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper)
//    {
//        super(output, BeyondDimensions.MODID, existingFileHelper);
//    }
//
//    @Override
//    protected void registerModels()
//    {
//        basicItem(ModItems.NET_CREATER.get());
//        basicItem(ModItems.NET_MEMBER_INVITER.get());
//        basicItem(ModItems.NET_MANAGER_INVITER.get());
//        basicItem(ModItems.UNSTABLE_SPACE_TIME_FRAGMENT.get());
//        basicItem(ModItems.STABLE_SPACE_TIME_FRAGMENT.get());
//        basicItem(ModItems.SPACE_TIME_STABLE_FRAME.get());
//        basicItem(ModItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION.get());
//        basicItem(ModItems.SPACE_TIME_BAR.get());
//    }
//}
