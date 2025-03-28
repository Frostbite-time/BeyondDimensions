package com.wintercogs.beyonddimensions.Integration.Mek.Capability;

import net.minecraftforge.common.capabilities.Capability;

import java.lang.reflect.Field;


public class ChemicalCapabilityHelper
{
    // 建立化学品能力实例 （能力是单例化实现，只需要提供的资源路径和类型一致，就能获取到同一个能力实例）
    // 但是此处仍然使用反射获取。因为注册能力会导致这个类被加载，并导致在未安装mek时无法找到化学品导入
    public static Capability<Object> CHEMICAL; //化学品能力
    //public static Class<?> CHEMICAL_HANDLER_CLASS; // 化学品处理类

    static {
        try {
            // 动态加载 IChemicalHandler 类

            // 获取目标类
            Class<?> capabilitiesClass = Class.forName("mekanism.common.capabilities.Capabilities");

            // 获取 public static final 字段
            Field gasHandlerField = capabilitiesClass.getField("GAS_HANDLER_CAPABILITY");

            // 通过反射调用 createSided
            CHEMICAL = (Capability<Object>) gasHandlerField.get(null);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to initialize Mekanism capability", e);
        }
    }
}
