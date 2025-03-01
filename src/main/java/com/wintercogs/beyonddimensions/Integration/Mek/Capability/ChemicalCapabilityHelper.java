package com.wintercogs.beyonddimensions.Integration.Mek.Capability;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import static com.wintercogs.beyonddimensions.BeyondDimensions.MekanismMODID;

public class ChemicalCapabilityHelper
{
    // 建立化学品能力实例 （能力是单例化实现，只需要提供的资源路径和类型一致，就能获取到同一个能力实例）
    // 但是此处仍然使用反射获取。因为注册能力会导致这个类被加载，并导致在未安装mek时无法找到化学品导入
    public static BlockCapability<Object, @Nullable Direction> CHEMICAL; //化学品能力
    public static Class<?> CHEMICAL_HANDLER_CLASS; // 化学品处理类

    static {
        try {
            // 动态加载 IChemicalHandler 类
            CHEMICAL_HANDLER_CLASS = Class.forName("mekanism.api.chemical.IChemicalHandler");

            // 通过反射调用 createSided
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(MekanismMODID, "chemical_handler");
            CHEMICAL = (BlockCapability<Object, Direction>) BlockCapability.class
                    .getMethod("createSided", ResourceLocation.class, Class.class)
                    .invoke(null, location, CHEMICAL_HANDLER_CLASS);
        } catch (ClassNotFoundException e) {
            // Mekanism 未加载
            CHEMICAL = null;
            CHEMICAL_HANDLER_CLASS = null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Mekanism capability", e);
        }
    }
}
