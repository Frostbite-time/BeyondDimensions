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

public class ModBlocks
{
    // 声明方块实例
    public static Block NET_CONTROL;
    public static Block NET_INTERFACE;
    public static Block NET_PATHWAY;
    public static Block NET_ENERGY_PATHWAY;
    // 初始化方块
    public static void init() {
        NET_CONTROL = new NetControlBlock(Material.IRON)
                .setHardness(4.0F)
                .setRegistryName(BeyondDimensions.MODID, "net_control")
                .setTranslationKey("net_control");
        NET_INTERFACE = new NetInterfaceBlock(Material.IRON)
                .setHardness(2.0F)
                .setRegistryName(BeyondDimensions.MODID, "net_interface");
        NET_PATHWAY = new NetPathwayBlock(Material.IRON)
                .setHardness(2.0F)
                .setRegistryName(BeyondDimensions.MODID, "net_pathway");
        NET_ENERGY_PATHWAY = new NetEnergyPathwayBlock(Material.IRON)
                .setHardness(2.0F)
                .setRegistryName(BeyondDimensions.MODID, "net_energy_pathway");
    }
    // 注册方块
    @Mod.EventBusSubscriber(modid = BeyondDimensions.MODID)
    public static class Registration {
        @SubscribeEvent
        public static void registerBlocks(RegistryEvent.Register<Block> event) {
            init(); // 初始化方块实例
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
                    new ResourceLocation(BeyondDimensions.MODID, "net_pathway_block_entity")
            );
            GameRegistry.registerTileEntity(
                    NetEnergyPathwayBlockEntity.class,
                    new ResourceLocation(BeyondDimensions.MODID, "net_energy_pathway_block_entity")
            );
        }
        // 注册方块的 ItemBlock
        @SubscribeEvent
        public static void registerItems(RegistryEvent.Register<Item> event) {
            event.getRegistry().registerAll(
                    new ItemBlock(NET_CONTROL)
                            .setRegistryName(NET_CONTROL.getRegistryName())
                            .setCreativeTab(ModCreativeModeTabs.BLOCKS_TAB),
                    new ItemBlock(NET_INTERFACE)
                            .setRegistryName(NET_INTERFACE.getRegistryName())
                            .setCreativeTab(ModCreativeModeTabs.BLOCKS_TAB),
                    new ItemBlock(NET_PATHWAY)
                            .setRegistryName(NET_PATHWAY.getRegistryName())
                            .setCreativeTab(ModCreativeModeTabs.BLOCKS_TAB),
                    new ItemBlock(NET_ENERGY_PATHWAY)
                            .setRegistryName(NET_ENERGY_PATHWAY.getRegistryName())
                            .setCreativeTab(ModCreativeModeTabs.BLOCKS_TAB)
            );
        }
    }
}
