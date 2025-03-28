package com.wintercogs.beyonddimensions.Registry;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Mod;



@Mod.EventBusSubscriber(modid = BeyondDimensions.MODID)
public class UIRegister
{

    // 1.12.2 使用静态常量定义 GUI ID
    public static final int DIMENSIONS_NET_GUI = 0;
    public static final int NET_CONTROL_GUI = 1;
    public static final int NET_INTERFACE_GUI = 2;
    public static final int NET_ENERGY_GUI = 3;

//    // 注册 GUI Handler
//    @SubscribeEvent
//    public static void registerGuis(FMLInitializationEvent event) {
//        NetworkRegistry.INSTANCE.registerGuiHandler(BeyondDimensions.instance, new GuiHandler());
//    }

//    // GUI Handler 实现类
//    public static class GuiHandler implements IGuiHandler
//    {
//        @Override
//        public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
//            switch (ID) {
//                case DIMENSIONS_NET_GUI:
//                    return new DimensionsNetMenu(player.inventory);
//                case NET_CONTROL_GUI:
//                    return new NetControlMenu(player.inventory);
//                case NET_INTERFACE_GUI:
//                    return new NetInterfaceBaseMenu(player.inventory);
//                case NET_ENERGY_GUI:
//                    return new NetEnergyMenu(player.inventory);
//            }
//            return null;
//        }
//        @Override
//        public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
//            switch (ID) {
//                case DIMENSIONS_NET_GUI:
//                    return new DimensionsNetGUI(new DimensionsNetMenu(player.inventory));
//                case NET_CONTROL_GUI:
//                    return new NetControlGUI(new NetControlMenu(player.inventory));
//                case NET_INTERFACE_GUI:
//                    return new NetInterfaceBaseGUI(new NetInterfaceBaseMenu(player.inventory));
//                case NET_ENERGY_GUI:
//                    return new NetEnergyGUI(new NetEnergyMenu(player.inventory));
//            }
//            return null;
//        }
//    }

    // 打开 GUI 的方法
    public static void openGui(EntityPlayer player, int guiId) {
        player.openGui(BeyondDimensions.instance, guiId, player.world,
                (int) player.posX, (int) player.posY, (int) player.posZ);
    }


}
