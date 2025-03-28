package com.wintercogs.beyonddimensions.Block;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Block.Custom.NetControlBlock;
import com.wintercogs.beyonddimensions.Block.Custom.NetEnergyPathwayBlock;
import com.wintercogs.beyonddimensions.Block.Custom.NetInterfaceBlock;
import com.wintercogs.beyonddimensions.Block.Custom.NetPathwayBlock;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetEnergyPathwayBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetInterfaceBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetPathwayBlockEntity;
import com.wintercogs.beyonddimensions.Item.ModCreativeModeTabs;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod.EventBusSubscriber(modid = BeyondDimensions.MODID)
public class ModBlocks
{
    // 声明方块实例
    public static Block NET_CONTROL = new NetControlBlock(Material.IRON)
            .setHardness(4.0F)
            .setRegistryName(BeyondDimensions.MODID, "net_control")
            .setTranslationKey("net_control")
            .setCreativeTab(ModCreativeModeTabs.BLOCKS_TAB);
    public static Block NET_INTERFACE = new NetInterfaceBlock(Material.IRON)
            .setHardness(2.0F)
            .setRegistryName(BeyondDimensions.MODID, "net_interface")
            .setTranslationKey("net_interface")
            .setCreativeTab(ModCreativeModeTabs.BLOCKS_TAB);
    public static Block NET_PATHWAY = new NetPathwayBlock(Material.IRON)
            .setHardness(2.0F)
            .setRegistryName(BeyondDimensions.MODID, "net_pathway")
            .setTranslationKey("net_pathway")
            .setCreativeTab(ModCreativeModeTabs.BLOCKS_TAB);
    public static Block NET_ENERGY_PATHWAY = new NetEnergyPathwayBlock(Material.IRON)
            .setHardness(2.0F)
            .setRegistryName(BeyondDimensions.MODID, "net_energy_pathway")
            .setTranslationKey("net_energy_pathway")
            .setCreativeTab(ModCreativeModeTabs.BLOCKS_TAB);


    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event)
    {
        event.getRegistry().registerAll(
                NET_CONTROL,
                NET_INTERFACE,
                NET_PATHWAY,
                NET_ENERGY_PATHWAY
        );
        // 注册 TileEntity
        GameRegistry.registerTileEntity(
                NetPathwayBlockEntity.class,
                new ResourceLocation(BeyondDimensions.MODID, "net_pathway_block_entity")
        );
        GameRegistry.registerTileEntity(
                NetInterfaceBlockEntity.class,
                new ResourceLocation(BeyondDimensions.MODID, "net_interface_block_entity")
        );
        GameRegistry.registerTileEntity(
                NetEnergyPathwayBlockEntity.class,
                new ResourceLocation(BeyondDimensions.MODID, "net_energy_pathway_block_entity")
        );
    }

    public static Item NET_CONTROL_BLOCK_ITEM = new ItemBlock(NET_CONTROL).setRegistryName(NET_CONTROL.getRegistryName());
    public static Item NET_INTERFACE_BLOCK_ITEM = new ItemBlock(NET_INTERFACE).setRegistryName(NET_INTERFACE.getRegistryName());
    public static Item NET_PATHWAY_BLOCK_ITEM = new ItemBlock(NET_PATHWAY).setRegistryName(NET_PATHWAY.getRegistryName());
    public static Item NET_ENERGY_PATHWAY_BLOCK_ITEM = new ItemBlock(NET_ENERGY_PATHWAY).setRegistryName(NET_ENERGY_PATHWAY.getRegistryName());

    // 注册方块的 ItemBlock
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
    {
        event.getRegistry().registerAll(
                NET_CONTROL_BLOCK_ITEM,
                NET_INTERFACE_BLOCK_ITEM,
                NET_PATHWAY_BLOCK_ITEM,
                NET_ENERGY_PATHWAY_BLOCK_ITEM
        );
    }

}
