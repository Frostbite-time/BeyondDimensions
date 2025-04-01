package com.wintercogs.beyonddimensions.Registry;

import com.cleanroommc.modularui.factory.SimpleGuiFactory;
import com.wintercogs.beyonddimensions.Gui.DimensionsNetGUI;
import com.wintercogs.beyonddimensions.Gui.Factory.PosGuiFactory;
import com.wintercogs.beyonddimensions.Gui.NetControlGUI;
import com.wintercogs.beyonddimensions.Gui.NetEnergyGUI;
import com.wintercogs.beyonddimensions.Gui.NetInterfaceGUI;


public class UIRegister
{
    public static SimpleGuiFactory Factory_DimensionsNetGUI =  new SimpleGuiFactory("dimensions_net_gui",() ->{
        return new DimensionsNetGUI();
    });

    public static PosGuiFactory Factory_NetControlGUI =  new PosGuiFactory("net_control_gui",() ->{
        return new NetControlGUI();
    });

    public static PosGuiFactory Factory_NetEnergyGUI =  new PosGuiFactory("net_energy_gui",() ->{
        return new NetEnergyGUI();
    });

    public static PosGuiFactory Factory_NetInterfaceGUI =  new PosGuiFactory("net_interface_gui",() ->{
        return new NetInterfaceGUI();
    });

    // 引用实例以加载类，以进行注册
    public UIRegister()
    {}

}
